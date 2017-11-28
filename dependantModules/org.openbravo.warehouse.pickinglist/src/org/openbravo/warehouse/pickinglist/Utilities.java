package org.openbravo.warehouse.pickinglist;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.ad.process.Parameter;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.warehouse.pickinglist.hooks.ProcessPLOrderHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {
  final private static Logger log = LoggerFactory.getLogger(CreateActionHandler.class);

  static long lineNo;

  @Inject
  @Any
  private static Instance<ProcessPLOrderHook> processPLOrderHook;

  public static PickingList createPL(Order order, Locator locator) {

    if (order.isObwplIsinpickinglist()) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_IsInPL", false) + order.getDocumentNo());
    }

    final Date now = DateUtils.truncate(new Date(), Calendar.DATE);
    long startCreatePL = System.currentTimeMillis();
    PickingList pickingList = OBProvider.getInstance().get(PickingList.class);
    pickingList.setOrganization(order.getOrganization());
    pickingList.setDocumentdate(now);
    boolean useOutbound = locator != null;
    DocumentType plDocType = OBWPL_Utils.getDocumentType(order.getOrganization(), "OBWPL_doctype",
        useOutbound);
    if (plDocType == null) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_DoctypeMissing", false));
    }
    BusinessPartner bPartner = order.getBusinessPartner();
    if (bPartner != null && !bPartner.isActive())
      throw new OBException(OBMessageUtils.messageBD("InActiveBusinessPartner"));
    pickingList.setDocumentType(plDocType);
    // Setting dummy document number. Final value set at the end to avoid contention
    pickingList.setDocumentNo(SequenceIdData.getUUID().substring(2));
    pickingList.setPickliststatus("DR");
    if(order.isFacstIsDirectDelivery()){
    	pickingList.setPickliststatus("AS");
    }
    pickingList.setOutboundStorageBin(locator);

    try {
      DocumentType docType = pickingList.getDocumentType();
      if (docType.isOBWPLUseOutbound() && !docType.isOBWPLIsGroup()
          && docType.isObwplPickusingboxes()) {
        pickingList.setUsePickingBoxes(true);
      }
    } catch (Exception ex) {
      pickingList.setUsePickingBoxes(false);
    }
    OBDal.getInstance().save(pickingList);
    long elapsedCreatePl = (System.currentTimeMillis() - startCreatePL);
    log.debug("total time create PL: " + elapsedCreatePl);
    return pickingList;
  }

  public static void processOrder(Order order, PickingList pickingList,
      HashSet<String> notCompletedPL) {
    if (order.isObwplReadypl()) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_IsExcluded", false)
          + order.getDocumentNo());
    }
    order.setObwplIsinpickinglist(true);

    // Create In Out
    ShipmentInOut shipment = createInOut(order);
    lineNo = 10L;
    final StringBuilder hqlString = new StringBuilder();
    hqlString.append(" select e.id ");
    hqlString.append(" from OrderLine as e");
    hqlString.append(" where e.salesOrder.id = :order");
    Query query = OBDal.getInstance().getSession().createQuery(hqlString.toString());
    query.setParameter("order", order.getId());
    List<String> orderLineIds = query.list();
    int counter = 0;
    long processInit = System.currentTimeMillis();
    for (String orderLineID : orderLineIds) {
      long init = System.currentTimeMillis();
      long initProcessOrderLine = System.currentTimeMillis();
      OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, orderLineID);
      processOrderLine(orderLine, shipment, pickingList, notCompletedPL);
      long elapsedProcessOrderLine = (System.currentTimeMillis() - initProcessOrderLine);
      log.debug("Total time to process order line (" + lineNo + "): " + elapsedProcessOrderLine);

      counter++;
      log.debug("Time for line " + counter + " " + (System.currentTimeMillis() - init) + " milis");
      if (counter % 20 == 0) {
        init = System.currentTimeMillis();
        OBDal.getInstance().getSession().clear();
        order = OBDal.getInstance().get(Order.class, order.getId());
        shipment = OBDal.getInstance().get(ShipmentInOut.class, shipment.getId());
        pickingList = OBDal.getInstance().get(PickingList.class, pickingList.getId());
        log.debug("Time for line flush " + (System.currentTimeMillis() - init) + " milis");
      }
    }

    log.debug("Time for process " + (System.currentTimeMillis() - processInit) + " milis");
    OBDal.getInstance().getSession().clear();
    order = OBDal.getInstance().get(Order.class, order.getId());
    shipment = OBDal.getInstance().get(ShipmentInOut.class, shipment.getId());
    pickingList = OBDal.getInstance().get(PickingList.class, pickingList.getId());
    // Setting sequence at the end of picking creation once lines have been processed to avoid
    // contention
    shipment.setDocumentNo(OBWPL_Utils.getDocumentNo(shipment.getDocumentType(), "M_InOut"));
    pickingList.setDocumentNo(OBWPL_Utils.getDocumentNo(pickingList.getDocumentType(),
        "OBWPL_PickingList"));
    OBDal.getInstance().save(shipment);
    OBDal.getInstance().save(pickingList);
    try {
      executeProcessPLOrderHook(order, shipment, pickingList);
    } catch (Exception e) {
      log.error("An error happened when processPLOrderHook was executed.", e.getMessage(),
          e.getStackTrace());
    }

    if (shipment.getMaterialMgmtShipmentInOutLineList().size() == 0) {
      throw new OBException(OBMessageUtils.messageBD("NotEnoughAvailableStock", false));
    }

  }

  public static ShipmentInOut createInOut(Order order) {
    final Date now = DateUtils.truncate(new Date(), Calendar.DATE);
    ShipmentInOut shipment = OBProvider.getInstance().get(ShipmentInOut.class);
    shipment.setOrganization(order.getOrganization());
    shipment.setSalesTransaction(true);
    shipment.setMovementType("C-");
    DocumentType shipDocType = OBWPL_Utils.getDocumentType(order.getOrganization(), "MMS", false,
        false);
    if ("".equals(shipDocType) || shipDocType == null) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_DocType_Shipment", false));
    }
    shipment.setDocumentType(shipDocType);
    // Setting dummy document number. Final value set at the end to avoid contention
    shipment.setDocumentNo(SequenceIdData.getUUID().substring(2));
    shipment.setWarehouse(order.getWarehouse());
    shipment.setBusinessPartner(order.getBusinessPartner());
    shipment.setPartnerAddress(order.getPartnerAddress());
    shipment.setDeliveryLocation(order.getDeliveryLocation());
    shipment.setDeliveryMethod(order.getDeliveryMethod());
    shipment.setDeliveryTerms(order.getDeliveryTerms());

    shipment.setMovementDate(now);
    shipment.setAccountingDate(now);
    shipment.setSalesOrder(order);
    shipment.setUserContact(order.getUserContact());
    shipment.setOrderReference(order.getOrderReference());
    shipment.setFreightCostRule(order.getFreightCostRule());
    shipment.setFreightAmount(order.getFreightAmount());
    shipment.setShippingCompany(order.getShippingCompany());
    shipment.setPriority(order.getPriority());

    shipment.setProject(order.getProject());
    shipment.setActivity(order.getActivity());
    shipment.setSalesCampaign(order.getSalesCampaign());
    shipment.setStDimension(order.getStDimension());
    shipment.setNdDimension(order.getNdDimension());
    shipment.setTrxOrganization(order.getTrxOrganization());

    shipment.setDocumentStatus("DR");
    shipment.setDocumentAction("CO");
    shipment.setProcessNow(false);
    OBDal.getInstance().save(shipment);

    return shipment;

  }

  public static void processOrderLine(OrderLine orderLine, ShipmentInOut shipment,
      PickingList pickingList, HashSet<String> notCompletedPL) {

    // Only consider pending to deliver lines of stocked item products.
    if (orderLine.getProduct() == null || !orderLine.getProduct().getProductType().equals("I")
        || orderLine.getOrderedQuantity().signum() == 0
        || orderLine.getOrderedQuantity().compareTo(orderLine.getDeliveredQuantity()) <= 0
        || (orderLine.isObwplReadypl() != null && orderLine.isObwplReadypl())) {
      return;
    }
    if (orderLine.getOrderedQuantity().signum() < 0) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_OrderedQtyMustBePositive", false));
    }
    if (orderLine.getProduct().isStocked()) {

      // Reserve Order Line
      boolean existsReservation = !orderLine.getMaterialMgmtReservationList().isEmpty();
      Reservation res = ReservationUtils.getReservationFromOrder(orderLine);
      if (res.getRESStatus().equals("DR")) {
        ReservationUtils.processReserve(res, "PR");
      } else if (res.getQuantity().compareTo(res.getReservedQty()) != 0) {
        ReservationUtils.reserveStockAuto(res);
      }
      // refresh
      res = OBDal.getInstance().get(Reservation.class, res.getId());
      OBDal.getInstance().refresh(res);

      List<ReservationStock> listResStock = new ArrayList<ReservationStock>();
      for (ReservationStock resStock : res.getMaterialMgmtReservationStockList()) {
        if (!resStock.isAllocated()) {
          if (resStock.getStorageBin() != null) {
            OBCriteria<StorageDetail> critSD = OBDal.getInstance().createCriteria(
                StorageDetail.class);
            critSD.add(Restrictions.eq(StorageDetail.PROPERTY_UOM, res.getUOM()));
            critSD.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, res.getProduct()));
            critSD
                .add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, resStock.getStorageBin()));
            critSD.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE,
                resStock.getAttributeSetValue()));
            critSD.add(Restrictions.isNull(StorageDetail.PROPERTY_ORDERUOM));
            critSD.setMaxResults(1);

            StorageDetail sd = (StorageDetail) critSD.uniqueResult();
            listResStock.add(ReservationUtils.reserveStockManual(res, sd, resStock.getQuantity()
                .negate(), "N"));
            ReservationUtils.reserveStockManual(res, sd, resStock.getQuantity(), "Y");
          } else {
            listResStock.add(ReservationUtils.reserveStockManual(res, resStock.getSalesOrderLine(),
                resStock.getQuantity().negate(), "N"));
            ReservationUtils.reserveStockManual(res, resStock.getSalesOrderLine(),
                resStock.getQuantity(), "Y");
          }
        }
      }
      // refresh
      res = OBDal.getInstance().get(Reservation.class, res.getId());
      OBDal.getInstance().refresh(res);

      if (!listResStock.isEmpty()) {
        for (ReservationStock resStock : listResStock) {
          if (resStock.getQuantity().equals(BigDecimal.ZERO) && !resStock.isAllocated()) {
            res.getMaterialMgmtReservationStockList().remove(resStock);
            OBDal.getInstance().remove(resStock);
          }
        }
      }
      OBDal.getInstance().flush();

      if (!existsReservation) {
        res.setOBWPLGeneratedByPickingList(true);
        OBDal.getInstance().save(res);
      }
      OBDal.getInstance().flush();
      if (res.getQuantity().compareTo(res.getReservedQty()) != 0) {
        notCompletedPL.add(pickingList.getDocumentNo());
      }

      for (ReservationStock resStock : res.getMaterialMgmtReservationStockList()) {
        if (resStock.getStorageBin() == null) {
          // If pre-reserve is not yet reserve
          continue;
        }
        BigDecimal releasedQty = resStock.getReleased() == null ? BigDecimal.ZERO : resStock
            .getReleased();
        if (resStock.getQuantity().compareTo(releasedQty) <= 0) {
          // Ignore released stock
          continue;
        }
        BigDecimal quantity = resStock.getQuantity().subtract(releasedQty);
        // Create InOut line.
        createShipmentLine(resStock.getAttributeSetValue(), resStock.getStorageBin(), quantity,
            orderLine, shipment, pickingList);
      }
      OBDal.getInstance().flush();

    } else {
      // Create Shipment Line for non stocked products.
      BigDecimal qty = orderLine.getOrderedQuantity().subtract(orderLine.getDeliveredQuantity());
      createShipmentLine(orderLine.getAttributeSetValue(), null, qty, orderLine, shipment,
          pickingList);
      OBDal.getInstance().flush();
    }
  }

  private static void createShipmentLine(AttributeSetInstance asi, Locator sb, BigDecimal qty,
      OrderLine orderLine, ShipmentInOut shipment, PickingList pickingList) {
    ShipmentInOutLine line = OBProvider.getInstance().get(ShipmentInOutLine.class);
    line.setOrganization(shipment.getOrganization());
    line.setShipmentReceipt(shipment);
    line.setSalesOrderLine(orderLine);
    line.setObwplPickinglist(pickingList);
    line.setLineNo(lineNo);
    lineNo += 10L;
    line.setProduct(orderLine.getProduct());
    line.setUOM(orderLine.getUOM());
    line.setAttributeSetValue(asi);
    line.setStorageBin(sb);
    line.setMovementQuantity(qty);
    line.setDescription(orderLine.getDescription());
    line.setExplode(orderLine.isExplode());

    if (orderLine.getBOMParent() != null) {
      OBCriteria<ShipmentInOutLine> obc = OBDal.getInstance().createCriteria(
          ShipmentInOutLine.class);
      obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipment));
      obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine.getBOMParent()));
      obc.setMaxResults(1);
      line.setBOMParent((ShipmentInOutLine) obc.uniqueResult());
    }

    shipment.getMaterialMgmtShipmentInOutLineList().add(line);
    OBDal.getInstance().save(line);
    OBDal.getInstance().save(shipment);
  }

  public static String processOrderOutbound(PickingList picking, Order order,
      HashSet<String> notCompletedPL) {

    StringBuffer message = new StringBuffer();

    OBCriteria<OrderLine> qLines = OBDal.getInstance().createCriteria(OrderLine.class);
    qLines.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order));
    qLines.addOrderBy(OrderLine.PROPERTY_LINENO, true);
    ScrollableResults scrollLines = qLines.scroll(ScrollMode.FORWARD_ONLY);

    int zeroStockLines = 0;
    int stockedProductCount = 0;
    while (scrollLines.next()) {
      final OrderLine line = (OrderLine) scrollLines.get()[0];

      BigDecimal pendingQty = processOrderLineOutbound(picking, line);
      if (pendingQty == null)
        continue;

      if (pendingQty.signum() > 0) {
        // Not enough stock
        message.append("</br>");
        message.append(line.getLineNo());
        message.append(": ");
        message.append(OBMessageUtils.messageBD("OBWPL_PartiallyReserved", false));
      }

      // clear session after each line iteration because the number of objects read in memory is
      // big
      OBDal.getInstance().getSession().clear();
      if (line.getProduct().isStocked()) {
        stockedProductCount++;
      }
      if (line.getOrderedQuantity().compareTo(pendingQty) == 0) {
        zeroStockLines++;
      }
    }
    // Setting sequence at the end of picking creation once lines have been processed to avoid
    // contention
    String pickingProvisionalDocNo = picking.getDocumentNo();
    picking
        .setDocumentNo(OBWPL_Utils.getDocumentNo(picking.getDocumentType(), "OBWPL_PickingList"));
    OBDal.getInstance().save(picking);
    if (notCompletedPL.contains(pickingProvisionalDocNo)) {
      notCompletedPL.remove(pickingProvisionalDocNo);
      notCompletedPL.add(picking.getDocumentNo());
    }

    // Doing this instead of commented code because I'm getting the following warning:
    // org.openbravo.dal.core.OBInterceptor - The object Order(xxxxx) is detected as not new (is its
    // id != null?) but it does not have a current state in the database. This can happen when the
    // id is set but not setNewObject(true); has been called.
    Order myOrder = OBDal.getInstance().get(Order.class, order.getId());
    if (zeroStockLines == stockedProductCount) {
      throw new OBException(OBMessageUtils.messageBD("NotEnoughAvailableStock", false));
    }
    myOrder.setObwplIsinpickinglist(true);
    OBDal.getInstance().save(myOrder);
    // order.setObwplIsinpickinglist(true);
    // OBDal.getInstance().save(order);
    if (message.length() == 0) {
      message.append(OBMessageUtils.messageBD("Success", false));
    }
    return message.toString();
  }

  public static BigDecimal processOrderLineOutbound(PickingList picking, OrderLine line) {

    BigDecimal pendingQty = line.getOrderedQuantity().subtract(line.getDeliveredQuantity());
    if (line.getProduct() == null || !line.getProduct().isStocked()
        || !"I".equals(line.getProduct().getProductType())
        || line.getOrderedQuantity().signum() <= 0 || pendingQty.signum() <= 0) {
      return null;
    }
    boolean hasReserve = !line.getMaterialMgmtReservationList().isEmpty();
    Reservation res = ReservationUtils.getReservationFromOrder(line);
    if ("DR".equals(res.getRESStatus())) {
      List<Object> params = new ArrayList<Object>();
      params.add(null);
      params.add(res.getId());
      params.add("PR");
      params.add(OBContext.getOBContext().getUser().getId());
      long initProcessMReservPOST = System.currentTimeMillis();
      CallStoredProcedure.getInstance().call("M_RESERVATION_POST", params, null, true, false);
      long elapsedProcessMReservPOST = System.currentTimeMillis() - initProcessMReservPOST;
      log.debug("Total time to execute M_RESERVATION_POST (" + line.getSalesOrder().getDocumentNo()
          + " - " + line.getLineNo() + ") : " + elapsedProcessMReservPOST);
    }
    // Refresh
    OBDal.getInstance().refresh(res);
    if (!hasReserve) {
      res.setOBWPLGeneratedByPickingList(true);
      OBDal.getInstance().save(res);
    }

    // In case the same storage detail is proposed multiple times all the quantities need to be
    // summed up. This can happen when the reserve has some stock allocated and it is proposed
    // to retrieve not allocated stock from the same storage detail.
    long initGetStock = System.currentTimeMillis();
    List<StockProposed> stocksProposed = callGetStock(line, res, true);
    long elapsedGetStock = (System.currentTimeMillis() - initGetStock);
    log.debug("Total time to execute callGetStock (" + line.getSalesOrder().getDocumentNo() + " - "
        + line.getLineNo() + ") : " + elapsedGetStock);
    Map<StorageDetail, BigDecimal> allocatedQty = new HashMap<StorageDetail, BigDecimal>();
    for (StockProposed stockProposed : stocksProposed) {
      BigDecimal qtyToMove = stockProposed.getQuantity();
      if (qtyToMove.compareTo(pendingQty) == 1) {
        qtyToMove = pendingQty;
      }
      long initGetStorageDetail = System.currentTimeMillis();
      StorageDetail sd = stockProposed.getStorageDetail();
      long elapsedGetStoragedetail = (System.currentTimeMillis() - initGetStorageDetail);
      log.debug("Total time to execute getStorageDetail (" + line.getSalesOrder().getDocumentNo()
          + " - " + line.getLineNo() + ") : " + elapsedGetStoragedetail);

      BigDecimal qty = allocatedQty.get(sd);
      if (qty == null) {
        qty = BigDecimal.ZERO;
      }
      qty = qty.add(qtyToMove);
      long initReallocteStock = System.currentTimeMillis();
      OBError result = ReservationUtils.reallocateStock(res, sd.getStorageBin(),
          sd.getAttributeSetValue(), qty);
      long elapsedReallocateStock = (System.currentTimeMillis() - initReallocteStock);
      log.debug("Total time to execute reallocateStock (" + line.getSalesOrder().getDocumentNo()
          + " - " + line.getLineNo() + ") : " + elapsedReallocateStock);
      if (!"Success".equals(result.getType())) {
        throw new OBException(result.getMessage());
      }
      // Refresh after call to reallocate stock procedure
      OBDal.getInstance().getSession().evict(res);
      res = OBDal.getInstance().get(Reservation.class, res.getId());

      // All reserved stock must be allocated. If the allocated quantity is not at least the
      // quantity reserved in this picking move the difference from the no allocated to
      // allocated.
      ReservationStock allocRS = ReservationUtils.reserveStockManual(res, sd, BigDecimal.ZERO, "Y");
      BigDecimal allocRSReleased = allocRS.getReleased() == null ? BigDecimal.ZERO : allocRS
          .getReleased();
      BigDecimal pendingToAllocate = qty.subtract(allocRS.getQuantity().subtract(allocRSReleased));
      if (pendingToAllocate.signum() == 1) {
        ReservationStock noAllocRS = ReservationUtils.reserveStockManual(res, sd, BigDecimal.ZERO,
            "N");
        allocRS.setQuantity(allocRS.getQuantity().add(pendingToAllocate));
        noAllocRS.setQuantity(noAllocRS.getQuantity().subtract(pendingToAllocate));
        OBDal.getInstance().save(allocRS);
        OBDal.getInstance().save(noAllocRS);
        // TODO:Review this flush
        OBDal.getInstance().flush();
      }
      OBDal.getInstance().refresh(res);

      allocatedQty.put(sd, qty);
      pendingQty = pendingQty.subtract(qtyToMove);
      if (pendingQty.signum() <= 0) {
        break;
      }
    }
    // TODO:Review this flush
    OBDal.getInstance().flush();

    List<ReservationStock> rsToRemove = new ArrayList<ReservationStock>();
    for (ReservationStock resStock : res.getMaterialMgmtReservationStockList()) {
      BigDecimal resStockReleasedQty = resStock.getReleased() == null ? BigDecimal.ZERO : resStock
          .getReleased();
      BigDecimal qty = resStock.getQuantity().subtract(resStockReleasedQty);
      if (qty.signum() > 0) {
        OBWPL_Utils.createGoodMovement(resStock, picking, null, null, line);
      } else if (qty.signum() == 0) {
        rsToRemove.add(resStock);
      }
    }
    res.getMaterialMgmtReservationStockList().removeAll(rsToRemove);
    OBDal.getInstance().save(res);
    // TODO:Review this flush
    OBDal.getInstance().flush();

    return pendingQty;
  }

  private static List<StockProposed> callGetStock(OrderLine line, Reservation res,
      boolean filterByWarehouse) {
    Process procGetStock = OBDal.getInstance().get(Process.class,
        "FF80818132C964E30132C9747257002E");

    long initGetStock = System.currentTimeMillis();
    Warehouse warehouse = line.getWarehouse();
    AttributeSetInstance attributeSetInstance = line.getAttributeSetValue();
    if (warehouse == null)
      warehouse = line.getSalesOrder().getWarehouse();

    String id = callProcessGetStock(procGetStock, line.getId(), (String) DalUtil.getId(line
        .getClient()), (String) DalUtil.getId(line.getOrganization()), (String) DalUtil.getId(line
        .getProduct()), (String) DalUtil.getId(line.getUOM()), (String) DalUtil.getId(warehouse),
        attributeSetInstance != null ? (String) DalUtil.getId(attributeSetInstance) : null, line
            .getOrderedQuantity().subtract(line.getDeliveredQuantity()),
        line.getWarehouseRule() != null ? (String) DalUtil.getId(line.getWarehouseRule()) : null,
        res != null ? res.getId() : null);
    long elapsedGetStock = (System.currentTimeMillis() - initGetStock);
    log.debug("Partial time to execute callGetStock (" + line.getSalesOrder().getDocumentNo()
        + " - " + line.getLineNo() + ") : " + elapsedGetStock);

    OBCriteria<StockProposed> critProposed = OBDal.getInstance()
        .createCriteria(StockProposed.class);
    critProposed.add(Restrictions.eq(StockProposed.PROPERTY_PROCESSINSTANCE, id));
    critProposed.addOrderBy(StockProposed.PROPERTY_PRIORITY, true);

    return critProposed.list();
  }

  private static ProcessInstance callProcess(org.openbravo.model.ad.ui.Process process,
      String recordID, Map<String, ?> parameters) {
    OBContext.setAdminMode();
    try {
      // Create the pInstance
      final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
      pInstance.setProcess(process);
      pInstance.setActive(true);
      pInstance.setAllowRead(true);
      pInstance.setRecordID(recordID);
      pInstance.setUserContact(OBContext.getOBContext().getUser());

      // now create the parameters and set their values
      int index = 0;
      for (String key : parameters.keySet()) {
        index++;
        final Object value = parameters.get(key);
        final Parameter parameter = OBProvider.getInstance().get(Parameter.class);
        parameter.setSequenceNumber(index + "");
        parameter.setParameterName(key);
        if (value instanceof String) {
          parameter.setString((String) value);
        } else if (value instanceof Date) {
          parameter.setProcessDate((Date) value);
        } else if (value instanceof BigDecimal) {
          parameter.setProcessNumber((BigDecimal) value);
        }

        // set both sides of the bidirectional association
        pInstance.getADParameterList().add(parameter);
        parameter.setProcessInstance(pInstance);
      }
      OBDal.getInstance().save(pInstance);
      OBDal.getInstance().flush();

      List<Object> params = new ArrayList<Object>();
      params.add(pInstance.getId());
      params.add("N");
      long initGetStockProcedureCall = System.currentTimeMillis();
      CallStoredProcedure.getInstance().call("M_GET_STOCK", params, null, true, false);
      long elapsedGetStockProcedureCall = (System.currentTimeMillis() - initGetStockProcedureCall);
      log.debug("Partial time to execute callGetStock Procedure Call() : "
          + elapsedGetStockProcedureCall);
      // refresh the pInstance as the SP has changed it
      OBDal.getInstance().getSession().refresh(pInstance);
      return pInstance;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static String callProcessGetStock(org.openbravo.model.ad.ui.Process process,
      String recordID, String clientId, String orgId, String productId, String uomId,
      String warehouseId, String attributesetinstanceId, BigDecimal quantity,
      String warehouseRuleId, String reservationId) {

    String processId = SequenceIdData.getUUID();
    OBContext.setAdminMode();
    List<Object> params = new ArrayList<Object>();
    // p_uuid
    params.add(processId);
    // p_RecordId
    params.add(recordID);
    // p_quantity
    params.add(quantity);
    // p_ProductId
    params.add(productId);
    // p_LocatorId
    params.add(null);
    // p_warehouseId
    params.add(warehouseId);
    // p_PriorityWarehouseId
    params.add(null);
    // p_OrgId
    params.add(orgId);
    // p_Attributesetinstanceid
    params.add(attributesetinstanceId);
    // p_userId
    params.add(OBContext.getOBContext().getUser().getId());
    // p_client
    params.add(clientId);
    // p_WarehouseruleId
    log.debug("warehouseRuleId: " + warehouseRuleId);
    params.add(warehouseRuleId);
    // p_uomId
    params.add(uomId);
    // p_ProductUomId
    params.add(null);
    // p_tableId
    params.add(null);
    // p_AuxId
    params.add(null);
    // p_lineNo
    params.add(null);
    // p_ProcessId
    params.add(null);
    // p_reservationId
    params.add(reservationId);
    // p_calledFromApp
    params.add("N");
    try {
      log.debug("Parameters : '" + processId + "', '" + recordID + "', " + quantity + ", '"
          + productId + "', null, '" + warehouseId + "', null, '" + orgId + "', '"
          + attributesetinstanceId + "', '" + OBContext.getOBContext().getUser().getId() + "', '"
          + clientId + "', '" + warehouseRuleId + "', '" + uomId
          + "', null, null, null, null, null, '" + reservationId + "', 'N'");
      long initGetStockProcedureCall = System.currentTimeMillis();
      CallStoredProcedure.getInstance().call("M_GET_STOCK_PARAM", params, null, true, true);
      long elapsedGetStockProcedureCall = (System.currentTimeMillis() - initGetStockProcedureCall);
      log.debug("Partial time to execute callGetStock Procedure Call() : "
          + elapsedGetStockProcedureCall);
      return processId;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected static void executeProcessPLOrderHook(Order order, ShipmentInOut shipment,
      PickingList pickingList) throws Exception {
    Set<Bean<?>> beansSet = WeldUtils.getStaticInstanceBeanManager().getBeans(
        ProcessPLOrderHook.class);
    List<Bean<?>> beansList = new ArrayList<Bean<?>>();
    beansList.addAll(beansSet);
    for (Bean<?> abstractBean : beansList) {
      ProcessPLOrderHook hook = (ProcessPLOrderHook) WeldUtils.getStaticInstanceBeanManager()
          .getReference(abstractBean, ProcessPLOrderHook.class,
              WeldUtils.getStaticInstanceBeanManager().createCreationalContext(abstractBean));
      hook.exec(order, shipment, pickingList);
    }
  }

  public static void updatePickingListDescription(String pickingListId) {
    String strDesc = "";
    PickingList pickingList = OBDal.getInstance().get(PickingList.class, pickingListId);
    // Refresh pickingList object to get the recent line information to get description
    OBDal.getInstance().save(pickingList);
    OBDal.getInstance().flush();
    final StringBuilder hsqlScript = new StringBuilder();
    hsqlScript.append("select distinct e.stockReservation.salesOrderLine.salesOrder as o ");
    hsqlScript
        .append("from MaterialMgmtInternalMovementLine e where e.oBWPLWarehousePickingList.id  = :pickingListId");
    final Session session = OBDal.getInstance().getSession();
    final Query query = session.createQuery(hsqlScript.toString());
    query.setParameter("pickingListId", pickingList.getId());
    for (Object o : query.list()) {
      Order order = (Order) o;
      if (strDesc.length() > 1) {
        strDesc += ", \n";
      }
      strDesc += OBMessageUtils.messageBD("OBWPL_OrderNo") + " " + order.getDocumentNo() + " "
          + OBMessageUtils.messageBD("OBWPL_BPartner") + order.getBusinessPartner().getName();
    }
    if (strDesc.length() > 2000) {
      strDesc = strDesc.substring(0, 1997) + "...";
    }
    pickingList.setDescription(strDesc);
    OBDal.getInstance().save(pickingList);
    OBDal.getInstance().flush();
  }
}
