package in.decathlon.ibud.transfer.ad_process;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.warehouse.pickinglist.PickingList;

public class CompleteGSGRProcess {

  public static final Logger log = Logger.getLogger(CompleteGSGRProcess.class);

  private static final String DOCUMENT_NO_M_MOVEMENT = "DocumentNo_M_Movement";
  public static final String TransitBin = "Transit Bin";
  public static final String shuttleBin = "Shuttel Bin";
  // public static final String TransitWarehouse = "InTransit Warehouse";
  ShipmentInOutLine inoutLine;

  public void doExecute(String recordID) {

    OBContext.setAdminMode();
    InternalMovement movement = null;
    try {

      String qry = "id in (select id from MaterialMgmtShipmentInOut io where io.id = '" + recordID
          + "')";
      OBQuery<ShipmentInOut> shipmentQuery = OBDal.getInstance().createQuery(ShipmentInOut.class,
          qry);
      List<ShipmentInOut> shipmentList = shipmentQuery.list();

      if (shipmentList != null && shipmentList.size() > 0) {
        for (ShipmentInOut io : shipmentList) {

          if (io.isSalesTransaction()) {
            List<ShipmentInOutLine> lstIOLine = io.getMaterialMgmtShipmentInOutLineList();

            if (!(io.getProcessGoodsJava().equals("IBUDS_SH"))) {

              log.debug(" Enter if condition");

              // Closing Goods Shipment at supply
              final Process process = OBDal.getInstance().get(Process.class, "109");
              final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
              pInstance.setProcess(process);
              pInstance.setRecordID(recordID);
              OBDal.getInstance().save(pInstance);
              try {
                final Connection connection = OBDal.getInstance().getConnection();
                final PreparedStatement ps = connection
                    .prepareStatement("SELECT * FROM M_InOut_Post0(?)");
                ps.setString(1, pInstance.getId());

                ps.execute();
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            }

            else {

              releaseStockReservation(io);
              List<ShipmentInOutLine> shpmentLineList = io.getMaterialMgmtShipmentInOutLineList();
              List<Product> ShipLineProducts = new ArrayList<Product>();
              HashMap<Product, BigDecimal> shutlRecords = new HashMap<Product, BigDecimal>();
              int i = 1;
              for (ShipmentInOutLine shpmentLine : shpmentLineList) {
                log.debug("Enetred for loop" + i);
                log.debug(" ShipLineProducts.size() = " + ShipLineProducts.size());
                if (ShipLineProducts.size() > 0) {
                  log.debug("size is greater than 0");
                  if (ShipLineProducts.contains(shpmentLine.getProduct())) {
                    log.debug("ShipLineProducts contains product");
                    createTransactions(shpmentLine, shpmentLine.getStorageBin(), "M-");
                    shutlRecords.put(shpmentLine.getProduct(), shutlRecords.get(
                        shpmentLine.getProduct()).add(shpmentLine.getMovementQuantity()));
                  } else {
                    log.debug("ShipLineProducts does not contains product");
                    createTransactions(shpmentLine, shpmentLine.getStorageBin(), "M-");
                    shutlRecords.put(shpmentLine.getProduct(), shpmentLine.getMovementQuantity());
                  }
                }

                else {
                  log.debug("ShipLineProducts size is 0");
                  createTransactions(shpmentLine, shpmentLine.getStorageBin(), "M-");
                  shutlRecords.put(shpmentLine.getProduct(), shpmentLine.getMovementQuantity());

                }
                // add shipment line product to ShipLineProducts
                ShipLineProducts.add(shpmentLine.getProduct());

                shpmentLine.setStorageBin(getSBin());
                shpmentLine.setAttributeSetValue(null);

              }
              updateTransactionRecord(io, shutlRecords, getSBin(), "M+");
              ShipLineProducts.clear();
              io.setProcessGoodsJava("CO");
              io.setDocumentStatus("IBUDS_SH");
              io.setDocumentAction("CO");
              OBDal.getInstance().save(io);

              ArrayList<ShipmentInOutLine> iolines = clubShipmentlinesForSamePrd(io);
              
              SessionHandler.getInstance().commitAndStart();
              
              io.getMaterialMgmtShipmentInOutLineList().removeAll(iolines);
              OBDal.getInstance().save(io);
              System.out.println("remove small list");


            }
            // InternalMovement movement = createMovementHeader(io);
          } else {
            boolean flag = BusinessEntityMapper.executeProcess(recordID, "104",
                "SELECT * FROM M_InOut_Post0(?)");

            String shipmentDoc = io.getIbudsShipmentreference();

            int indexOfLast = shipmentDoc.lastIndexOf("*");
            String newshipmentDoc = shipmentDoc;
            if (indexOfLast >= 0)
              newshipmentDoc = shipmentDoc.substring(0, indexOfLast);

            String ordqry = "id in (select ord.id from Order ord where ord.documentNo = '"
                + newshipmentDoc + "' and ord.salesTransaction = false )";
            OBQuery<org.openbravo.model.common.order.Order> orderQuery = OBDal.getInstance()
                .createQuery(org.openbravo.model.common.order.Order.class, ordqry);

            List<org.openbravo.model.common.order.Order> orderList = orderQuery.list();

            if (orderList != null && orderList.size() > 0) {
              for (Order ord : orderList) {
                String orderId = ord.getId();
                // ord.setDocumentStatus("CL");
                // OBDal.getInstance().save(ord);

                // closing SO in supply
                BusinessEntityMapper
                    .executeProcess(orderId, "109", "SELECT * FROM C_Order_post(?)");
              }
              if (shipmentDoc.length() > 0) {
                String wsName = "in.decathlon.ibud.shipment.CompleteShipmentWS";
                JSONWebServiceInvocationHelper.sendPostrequest(wsName, "shipmentIdfromGRN="
                    + shipmentDoc, "{}");

              }
            }
          }
        }
      }
      final OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle(OBMessageUtils.messageBD("Success"));
      // bundle.setResult(msg);
    } catch (Exception e) {

      log.error("Error", e);

      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("Error occurred");
      // bundle.setResult(msg);

    }

  }

  // This method clubs multiple shipment lines having same Product
  private ArrayList<ShipmentInOutLine> clubShipmentlinesForSamePrd(ShipmentInOut io) {
    List<ShipmentInOutLine> shipLinList = io.getMaterialMgmtShipmentInOutLineList();
    HashMap<Product, BigDecimal> shipPrdNQty = new HashMap<Product, BigDecimal>();
    ArrayList<ShipmentInOutLine> iolines = new ArrayList<ShipmentInOutLine>();
    PickingList pkList = null;
    long lineNo = 10;

    for (ShipmentInOutLine shipLine : shipLinList) {
      pkList = shipLine.getObwplPickinglist();
      if (shipPrdNQty.size() > 0) {
        if (shipPrdNQty.containsKey(shipLine.getProduct())) {
          BigDecimal initQty = shipPrdNQty.get(shipLine.getProduct());
          shipPrdNQty.put(shipLine.getProduct(), initQty.add(shipLine.getMovementQuantity()));
        } else {

          shipPrdNQty.put(shipLine.getProduct(), shipLine.getMovementQuantity());
        }
      } else {
        shipPrdNQty.put(shipLine.getProduct(), shipLine.getMovementQuantity());
      }
      // shipLine.setActive(false);
      // io.getMaterialMgmtShipmentInOutLineList().remove(shipLine);
      // OBDal.getInstance().remove(shipLine);
      iolines.add(shipLine);
    }
    // io.getMaterialMgmtShipmentInOutLineList().clear();

    /*while (io.getMaterialMgmtShipmentInOutLineList().size() > 0) {
      ShipmentInOutLine shipmentLine = io.getMaterialMgmtShipmentInOutLineList().get(0);
      System.out.println("removing "+io.getMaterialMgmtShipmentInOutLineList().get(0));
      io.getMaterialMgmtShipmentInOutLineList().remove(shipmentLine);
      OBDal.getInstance().remove(shipmentLine);
      // OBDal.getInstance().refresh(io.getMaterialMgmtShipmentInOutLineList());
      System.out.println("removed");
    }*/

    Set<Product> prdSet = shipPrdNQty.keySet();
    // SessionHandler.getInstance().commitAndStart();
    for (Product prd : prdSet) {
      createShiplines(io, prd, shipPrdNQty.get(prd), lineNo, pkList);
      lineNo += 10;
    }
    return iolines;
  }

  private void createShiplines(ShipmentInOut io, Product prd, BigDecimal qty, long lineNo,
      PickingList pkList) {
    ShipmentInOutLine newShipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
    newShipmentLine.setLineNo(lineNo);
    newShipmentLine.setClient(io.getClient());
    newShipmentLine.setOrganization(io.getOrganization());
    newShipmentLine.setActive(true);
    newShipmentLine.setCreationDate(new Date());
    newShipmentLine.setCreatedBy(io.getCreatedBy());
    newShipmentLine.setUpdatedBy(io.getUpdatedBy());
    newShipmentLine.setUpdated(new Date());
    newShipmentLine.setMovementQuantity(qty);
    newShipmentLine.setShipmentReceipt(io);
    newShipmentLine.setProduct(prd);
    newShipmentLine.setUOM(prd.getUOM());

    newShipmentLine.setIbodtrActmovementqty(qty);
    newShipmentLine.setObwplPickinglist(pkList);
    newShipmentLine.setStorageBin(getSBin());
    io.getMaterialMgmtShipmentInOutLineList().add(newShipmentLine);
    OBDal.getInstance().save(io);

  }

  private boolean closePo(ShipmentInOut io, boolean isSalesTransaction) {
    try {
      boolean flag = false;
      String shipmentDoc = io.getDocumentNo();

      int indexOfLast = shipmentDoc.lastIndexOf("*");
      String newshipmentDoc = shipmentDoc;
      if (indexOfLast >= 0)
        newshipmentDoc = shipmentDoc.substring(0, indexOfLast);
      String ordqry;
      if (isSalesTransaction)
        ordqry = "id in (select ord.id from Order ord where ord.documentNo = '" + shipmentDoc
            + "' and ord.salesTransaction = " + isSalesTransaction + ")";
      else
        ordqry = "id in (select ord.id from Order ord where ord.documentNo = '" + newshipmentDoc
            + "' and ord.salesTransaction = " + isSalesTransaction + ")";

      OBQuery<org.openbravo.model.common.order.Order> orderQuery = OBDal.getInstance().createQuery(
          org.openbravo.model.common.order.Order.class, ordqry);

      List<org.openbravo.model.common.order.Order> orderList = orderQuery.list();

      if (orderList != null && orderList.size() > 0) {
        for (Order ord : orderList) {

          String orderId = ord.getId();
          ord.setDocumentStatus("CL");
          OBDal.getInstance().save(ord);
          OBDal.getInstance().flush();

          flag = BusinessEntityMapper.executeProcess(orderId, "109",
              "SELECT * FROM C_Order_post(?)");
        }
        if (shipmentDoc.length() > 0) {
          String wsName = "in.decathlon.ibud.shipment.CompleteShipmentWS";
          JSONWebServiceInvocationHelper.sendPostrequest(wsName,
              "shipmentIdfromGRN=" + shipmentDoc, "{}");

        }
      }
      return flag;
    } catch (Exception e) {
      log.debug("Error closing PO/SO" + e);
    }
    return false;
  }

  private boolean releaseStockReservation(ShipmentInOut io) {
    log.debug("Call M_Reservation_Consumtion");
    try {
      List<OrderLine> salesOrderLine = io.getSalesOrder().getOrderLineList();
      for (OrderLine ordLine : salesOrderLine) {

        List<org.openbravo.model.materialmgmt.onhandquantity.Reservation> resvList = ordLine
            .getMaterialMgmtReservationList();
        for (org.openbravo.model.materialmgmt.onhandquantity.Reservation reserv : resvList) {
          OBCriteria<ReservationStock> resvStockCriteria = OBDal.getInstance().createCriteria(
              ReservationStock.class);
          resvStockCriteria.add(Restrictions.eq(ReservationStock.PROPERTY_RESERVATION, reserv));
          List<ReservationStock> resrvLineList = resvStockCriteria.list();
          for (ReservationStock resevStock : resrvLineList) {
            String pReservationId = reserv.getId();
            String pLocatorId = resevStock.getStorageBin().getId();
            String pAttributesetinstanceId = resevStock.getAttributeSetValue().getId();
            BigDecimal pQty = resevStock.getQuantity();
            String pUserId = resevStock.getCreatedBy().getId();

            try {
              final Connection connection = OBDal.getInstance().getConnection();
              final PreparedStatement ps = connection
                  .prepareStatement("SELECT * FROM m_reservation_consumption(?,?,?,?,?)");
              ps.setString(1, pReservationId);
              ps.setString(2, pLocatorId);
              ps.setString(3, pAttributesetinstanceId);
              ps.setBigDecimal(4, pQty);
              ps.setString(5, pUserId);

              ps.execute();
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }

          }

        }

      }
      OBDal.getInstance().flush();
      return true;
    } catch (Exception e) {
      log.debug("Error Removing reservations", e);
    }
    return false;
  }

  public void createTransactions(ShipmentInOutLine shpmentLine, Locator bin, String movementType) {

    MaterialTransaction mtrTranx = OBProvider.getInstance().get(MaterialTransaction.class);
    try {
      mtrTranx.setClient(shpmentLine.getClient());
      mtrTranx.setOrganization(shpmentLine.getOrganization());
      mtrTranx.setActive(true);
      mtrTranx.setCreatedBy(OBContext.getOBContext().getUser());
      mtrTranx.setUpdatedBy(OBContext.getOBContext().getUser());
      mtrTranx.setCreationDate(new Date());
      mtrTranx.setUpdated(new Date());
      mtrTranx.setMovementDate(new Date());
      mtrTranx.setMovementType(movementType);
      mtrTranx.setStorageBin(bin);
      mtrTranx.setProduct(shpmentLine.getProduct());
      mtrTranx.setAttributeSetValue(shpmentLine.getAttributeSetValue());
      if (movementType.equals("M-"))
        mtrTranx.setMovementQuantity(shpmentLine.getMovementQuantity().negate());
      else {
        mtrTranx.setMovementQuantity(shpmentLine.getMovementQuantity());
        mtrTranx.setAttributeSetValue(null);
      }
      mtrTranx.setUOM(shpmentLine.getUOM());
      // mtrTranx.setGoodsShipmentLine(shpmentLine);
      mtrTranx.setSwMovementtype("SRQ");
      mtrTranx.setSwDocumentno(shpmentLine.getShipmentReceipt().getDocumentNo());
      OBDal.getInstance().save(mtrTranx);

    } catch (Exception e) {
      log.debug(e);
    }
  }

  // This methods clubs shuttle records if prdct is same bcoz earlier it was seperate shtle recrds
  // for each shpline
  private void updateTransactionRecord(ShipmentInOut io, HashMap<Product, BigDecimal> shutlRecords,
      Locator sBin, String movementType) {

    Set<Product> prdcts = shutlRecords.keySet();
    for (Product pr : prdcts) {
      MaterialTransaction mtrTranx = OBProvider.getInstance().get(MaterialTransaction.class);
      try {
        mtrTranx.setClient(io.getClient());
        mtrTranx.setOrganization(io.getOrganization());
        mtrTranx.setActive(true);
        mtrTranx.setCreatedBy(OBContext.getOBContext().getUser());
        mtrTranx.setUpdatedBy(OBContext.getOBContext().getUser());
        mtrTranx.setCreationDate(new Date());
        mtrTranx.setUpdated(new Date());
        mtrTranx.setMovementDate(new Date());
        mtrTranx.setMovementType(movementType);
        mtrTranx.setStorageBin(sBin);
        mtrTranx.setProduct(pr);
        // mtrTranx.setAttributeSetValue(shpmentLine.getAttributeSetValue()); // cmntd bcoz trxs
        // recds need nt hav attrbtsetval
        if (movementType.equals("M-"))
          mtrTranx.setMovementQuantity(shutlRecords.get(pr).negate());
        else
          mtrTranx.setMovementQuantity(shutlRecords.get(pr));

        mtrTranx.setUOM(pr.getUOM());
        // mtrTranx.setGoodsShipmentLine(shpmentLine); commented because shuttle records need not
        // have shpmntlin reference
        mtrTranx.setSwMovementtype("SRQ");
        mtrTranx.setSwDocumentno(io.getDocumentNo());
        OBDal.getInstance().save(mtrTranx);

      } catch (Exception e) {
        log.debug(e);
      }

    }

  }

  private Warehouse getWarehouse(Organization org, String name) {
    try {
      String qry = "wh where wh.name='" + name + "' and wh.organization.id='" + org.getId() + "'";
      OBQuery<Warehouse> strQry = OBDal.getInstance().createQuery(Warehouse.class, qry);
      strQry.setMaxResult(1);
      List<Warehouse> WarehouseList = strQry.list();

      return WarehouseList.get(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static Locator getSBin(String warehouse) {
    String qry = "sb where sb.warehouse.id ='" + warehouse + "' and sb.searchKey='" + TransitBin
        + "'";
    OBQuery<Locator> storageqry = OBDal.getInstance().createQuery(Locator.class, qry);

    List<Locator> storageList = storageqry.list();
    if (storageList != null && storageList.size() > 0) {

      return storageList.get(0);
    } else {
      return null;
    }

  }

  public static Locator getSBin() {
    String qry = " sb where  sb.searchKey='" + shuttleBin + "'";
    OBQuery<Locator> storageqry = OBDal.getInstance().createQuery(Locator.class, qry);

    List<Locator> storageList = storageqry.list();
    if (storageList != null && storageList.size() > 0) {

      return storageList.get(0);
    } else {
      return null;
    }

  }

}