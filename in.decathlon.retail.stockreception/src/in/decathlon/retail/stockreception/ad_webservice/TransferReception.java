package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class TransferReception extends DalBaseProcess {

  final private static Logger log = Logger.getLogger(TransferReception.class);
  final String disputeWarehouse = SOConstants.disputeWarehouse;
  final String disputeBin = SOConstants.disputeBin;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    String recordID = null;
    String email;
    try {
      OBContext.setAdminMode();
      recordID = (String) bundle.getParams().get("M_InOut_ID");
      ShipmentInOut shipmentInOut = OBDal.getInstance().get(ShipmentInOut.class, recordID);
      processValidation(shipmentInOut,email="");
      final OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle(OBMessageUtils.messageBD("Success"));
      bundle.setResult(msg);
    } catch (Exception e) {
      log.error("Error", e);
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("Error occurred");
      bundle.setResult(msg);
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void processValidation(ShipmentInOut shipmentInOut,String email) throws Exception, JSONException {
    BigDecimal movementQty = null;
    BigDecimal actualMovementQty = null;
    JSONObject wsObj = new JSONObject();
    JSONArray wsArray = new JSONArray();
    if (isValidationTimeExceeded(shipmentInOut)) {
      throw new OBException("Validation time 24 hours exceeded Completed time ("
          + shipmentInOut.getIbodtrCompletedTime() + ")");
    }

    List<ShipmentInOutLine> listShipmentInOutLine = shipmentInOut
        .getMaterialMgmtShipmentInOutLineList();

    DocumentType rtvsDocType = BusinessEntityMapper.getDocType("MMR", true);
    DocumentType returnGRN = BusinessEntityMapper.getDocType("MMR", false);

    Order returnToVendorHeader = createRTVHeader(shipmentInOut,email);
    ShipmentInOut returnMaterialShipmentHeader = createRMShipmentHeader(shipmentInOut, rtvsDocType,email);
    ShipmentInOut goodsReceipt = createRMShipmentHeader(shipmentInOut, returnGRN,email);

    for (ShipmentInOutLine ioLine : listShipmentInOutLine) {
      wsArray = new JSONArray();
      movementQty = ioLine.getMovementQuantity();
      double mQty = movementQty.doubleValue();
      actualMovementQty = ioLine.getIbodtrActmovementqty();
      String strActualMovementQty = actualMovementQty.toString();
      double aMqty = actualMovementQty.doubleValue();
      double returnqty = aMqty - mQty;
      double initialRecQty;

      if (mQty < aMqty) {
    	  List<ShipmentInOut> shipmentList=null;
	    	OBCriteria<ShipmentInOut> criteriaorder = OBDal.getInstance().createCriteria(
	    			ShipmentInOut.class);
			criteriaorder.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, shipmentInOut.getDocumentNo()+"*IRC"));
			
			shipmentList = criteriaorder.list();

		        Integer inoutCount1 = shipmentList.size();
		        if (inoutCount1 >0)
		        {
		        	log.info("Duplicate IRC Avoided");
		        }else
		        {
        OBDal.getInstance().save(goodsReceipt); // Goods receipt header created
        long line = (goodsReceipt.getMaterialMgmtShipmentInOutLineList().size() + 1) * 10;
        List<ShipmentInOutLine> grnLineList = createGrnLine(goodsReceipt, ioLine, returnqty, true,
            line,email);
        goodsReceipt.getMaterialMgmtShipmentInOutLineList().addAll(grnLineList);
        OBDal.getInstance().save(goodsReceipt);
		        }
      } else if (mQty > aMqty) {

        List<OrderLine> newOrderLines = createRTVOrderLine(returnToVendorHeader, ioLine, mQty
            - aMqty,email);
        returnToVendorHeader.getOrderLineList().addAll(newOrderLines);

        if (returnToVendorHeader.getOrderLineList().size() > 0) {
          SaveRTVOrder(returnToVendorHeader); // RTV header created
          long line = (returnMaterialShipmentHeader.getMaterialMgmtShipmentInOutLineList().size() + 1) * 10;

          OBDal.getInstance().save(returnMaterialShipmentHeader); // return Material Ship created
          List<ShipmentInOutLine> aShipmentLine = createGrnLine(returnMaterialShipmentHeader,
              ioLine, mQty - aMqty, false, line,email);
          returnMaterialShipmentHeader.getMaterialMgmtShipmentInOutLineList().addAll(aShipmentLine);
          OBDal.getInstance().save(returnMaterialShipmentHeader);
        }
      }

      // Received quantity update in purchase order
      try {
        String shipmentDoc = shipmentInOut.getDocumentNo();

        int indexOfLast = shipmentDoc.lastIndexOf("*");
        String orderDoc = null;
        if (indexOfLast >= 0)
          orderDoc = shipmentDoc.substring(0, indexOfLast);

        String ordlineqry = "id in (select ol.id from OrderLine ol where ol.salesOrder.documentNo = '"
            + orderDoc + "' and ol.product.id= '" + ioLine.getProduct().getId() + "')";

        OBQuery<OrderLine> ordLineQuery = OBDal.getInstance().createQuery(OrderLine.class,
            ordlineqry);

        List<OrderLine> ordLineList = ordLineQuery.list();
        for (OrderLine ordLine : ordLineList) {

          initialRecQty = Double.parseDouble(ordLine.getSWEMSwRecqty());

          initialRecQty += returnqty;

          strActualMovementQty = String.valueOf(initialRecQty);

          if (strActualMovementQty.contains("."))
            strActualMovementQty = strActualMovementQty.substring(0, strActualMovementQty
                .indexOf('.'));

          ordLine.setSWEMSwRecqty(strActualMovementQty);

          OBDal.getInstance().save(ordLine);
        }

      } catch (Exception e) {
        log.error(e.getMessage(), e);
        throw e;
      }
    }

    shipmentInOut.setIbodtrVaidate(true);
    shipmentInOut.setUpdatedBy(getUser(email));
    OBDal.getInstance().save(shipmentInOut);
    OBDal.getInstance().flush();
    JSONObject shipmentObj = new JSONObject();
    if (goodsReceipt.getMaterialMgmtShipmentInOutLineList().size() > 0) {
      shipmentObj = createShipmentJSONObj(goodsReceipt);
      wsArray.put(0, shipmentObj);
      wsObj.put("dataGRN", wsArray);
    }

    wsArray = new JSONArray();
    if (returnToVendorHeader.getOrderLineList().size() > 0) {
      JSONObject orderObj = createOrderJsonObj(returnToVendorHeader);
      wsArray.put(0, orderObj);
      wsObj.put("dataRTV", wsArray);
    }

    wsArray = new JSONArray();
    if (returnMaterialShipmentHeader.getMaterialMgmtShipmentInOutLineList().size() > 0) {
      shipmentObj = createShipmentJSONObj(returnMaterialShipmentHeader);
      wsArray.put(0, shipmentObj);
      wsObj.put("dataRMS", wsArray);
    }

    SessionHandler.getInstance().commitAndStart();
    String content = wsObj.toString();
    log.error(content);
    if (content.length() > 0) {
      String wsName = "in.decathlon.ibud.transfer.TransferWS";
      JSONWebServiceInvocationHelper.sendPostrequest(wsName, "", content);
      wsObj = new JSONObject();
      OBDal.getInstance().flush();
      if (goodsReceipt.getMaterialMgmtShipmentInOutLineList().size() > 0) {
        BusinessEntityMapper.executeProcess(goodsReceipt.getId(), "109",
            "SELECT * FROM M_InOut_Post0(?)");
        //BusinessEntityMapper.txnSWMovementType(goodsReceipt);
      }

      if (returnToVendorHeader.getOrderLineList().size() > 0) {
        BusinessEntityMapper.executeProcess(returnToVendorHeader.getId(), "104",
            "SELECT * FROM C_Order_post(?)");
      }
      if (returnMaterialShipmentHeader.getMaterialMgmtShipmentInOutLineList().size() > 0) {
        BusinessEntityMapper.executeProcess(returnMaterialShipmentHeader.getId(), "109",
            "SELECT * FROM M_InOut_Post0(?)");
        //BusinessEntityMapper.txnSWMovementType(returnMaterialShipmentHeader);
      }
    }

    SessionHandler.getInstance().commitAndStart();
  }

  private boolean isValidationTimeExceeded(ShipmentInOut shipmentInOut) {
    Date completedTime = shipmentInOut.getIbodtrCompletedTime();
    int validateTime = 0;
    String strValidateTime = IbudConfig.getGrnValidateTime();
    if (completedTime == null || strValidateTime == null)
      return true;
    validateTime = Integer.parseInt(strValidateTime);
    Calendar cal = Calendar.getInstance();
    cal.setTime(completedTime);
    cal.add(Calendar.HOUR_OF_DAY, validateTime);
    Date validationExceedTime = cal.getTime();
    if (validationExceedTime.after(new Date()))
      return false;
    return true;
  }

  private JSONObject createOrderJsonObj(Order newOrder) {
    JSONObject orderObj = new JSONObject();
    JSONObject orderHeader = new JSONObject();
    JSONArray orderLines = new JSONArray();
    try {
      orderHeader = JSONHelper.convetBobToJson(newOrder);

      List<OrderLine> orderLineList = newOrder.getOrderLineList();
      if (orderLineList != null && orderLineList.size() > 0) {
        for (OrderLine ol : orderLineList) {
          orderLines.put(JSONHelper.convetBobToJson(ol));
        }
        orderObj.put("OrderHeader", orderHeader);
        orderObj.put("OrderLines", orderLines);
      }

    } catch (Exception e) {
      log.error("Error calculating promotions", e);
      e.printStackTrace();

    }
    return orderObj;
  }

  private JSONObject createShipmentJSONObj(ShipmentInOut aShipment) throws Exception {

    JSONObject shipmentObj = new JSONObject();
    JSONObject shipmentHeader = new JSONObject();
    JSONArray shipmentLines = new JSONArray();
    try {

      shipmentHeader = JSONHelper.convetBobToJson(aShipment);
      for (ShipmentInOutLine sl : aShipment.getMaterialMgmtShipmentInOutLineList()) {
        shipmentLines.put(JSONHelper.convetBobToJson(sl));
      }

      shipmentObj.put("ReceiptHeader", shipmentHeader);
      shipmentObj.put("ReceiptLines", shipmentLines);
    } catch (Exception e) {
      log.error("Problem Creating Shipment Json Data ", e);
      e.printStackTrace();
      throw e;

    }
    return shipmentObj;
  }

  private ShipmentInOut createRMShipmentHeader(ShipmentInOut io, DocumentType docType,String email)
      throws Exception {
    ShipmentInOut shipmentHeader = OBProvider.getInstance().get(ShipmentInOut.class);
    try {
      shipmentHeader.setClient(io.getClient());
      shipmentHeader.setOrganization(io.getOrganization());
      shipmentHeader.setActive(true);
      shipmentHeader.setCreationDate(new Date());
      shipmentHeader.setCreatedBy(getUser(email));
      shipmentHeader.setUpdatedBy(getUser(email));
      shipmentHeader.setUpdated(new Date());
      shipmentHeader.setBusinessPartner(io.getBusinessPartner());
      shipmentHeader.setPartnerAddress(io.getBusinessPartner().getBusinessPartnerLocationList()
          .get(0));
      shipmentHeader.setMovementDate(new Date());
      shipmentHeader.setDocumentStatus(SOConstants.DraftDocumentStatus);
      shipmentHeader.setProcessed(false);
      shipmentHeader.setIbodtrVaidate(true);
      shipmentHeader.setSWMovement(SOConstants.SWMovement);
      shipmentHeader.setDocumentType(docType);
      shipmentHeader.setDocumentNo(io.getDocumentNo() + "*IRC");
      shipmentHeader.setWarehouse(io.getWarehouse());
      shipmentHeader.setProcessGoodsJava(SOConstants.CompleteDocumentStatus);
      shipmentHeader.setAccountingDate(new Date());
      shipmentHeader.setIbodtrVaidate(true);
      shipmentHeader.setSalesTransaction(false);
      shipmentHeader.setNewOBObject(true);

    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
    return shipmentHeader;
  }

  private List<ShipmentInOutLine> createGrnLine(ShipmentInOut rMShipment, ShipmentInOutLine ioLine,
      double qty, boolean flag, long line,String email) throws Exception {
    List<ShipmentInOutLine> shipmentInOutLine = new ArrayList<ShipmentInOutLine>();

    try {
      double quantity = qty;
      if (!flag)
        quantity = quantity * -1;

      BigDecimal qtyToBeOrdered = BigDecimal.valueOf(quantity);

      Product pr = ioLine.getProduct();
      ShipmentInOutLine newShipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
      newShipmentLine.setClient(rMShipment.getClient());
      newShipmentLine.setOrganization(rMShipment.getOrganization());
      newShipmentLine.setActive(true);
      newShipmentLine.setCreationDate(new Date());
      newShipmentLine.setCreatedBy(getUser(email));
      newShipmentLine.setUpdatedBy(getUser(email	));
      newShipmentLine.setUpdated(new Date());
      newShipmentLine.setMovementQuantity(qtyToBeOrdered);
      newShipmentLine.setShipmentReceipt(rMShipment);
      newShipmentLine.setProduct(pr);
      newShipmentLine.setUOM(pr.getUOM());

      newShipmentLine.setIbodtrActmovementqty(qtyToBeOrdered);
      if (ioLine.getShipmentReceipt().getWarehouse().getReturnlocator() == null) {
        throw new OBException("No Return Bin defined for warehouse");
      } else
        newShipmentLine
            .setStorageBin(ioLine.getShipmentReceipt().getWarehouse().getReturnlocator());
      newShipmentLine.setLineNo(line);
      shipmentInOutLine.add(newShipmentLine);

    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
    return shipmentInOutLine;
  }

  private void SaveRTVOrder(Order orderHeader) {
    try {
      OBDal.getInstance().save(orderHeader);
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
    }
  }


 
  private Order createRTVHeader(ShipmentInOut io,String email) throws Exception {
    Order newOrder = OBProvider.getInstance().get(Order.class);

    try {

      newOrder.setSwIsautoOrder(true);
      newOrder.setClient(io.getClient());
      newOrder.setOrganization(io.getOrganization());
      newOrder.setActive(true);
      newOrder.setCreationDate(new Date());
      newOrder.setCreatedBy(getUser(email));
      newOrder.setUpdatedBy(getUser(email));
      newOrder.setUpdated(new Date());
      newOrder.setBusinessPartner(io.getBusinessPartner());
      newOrder.setPartnerAddress(io.getBusinessPartner().getBusinessPartnerLocationList().get(0));
      FIN_PaymentMethod paymentMethod = getPaymentMethod(SOConstants.PaymentMethod);
      newOrder.setPaymentMethod(paymentMethod);
      PaymentTerm paymentTerm = getPaymentTerm(SOConstants.PaymentTerm);
      newOrder.setPaymentTerms(paymentTerm);
      newOrder.setOrderDate(new Date());
      newOrder.setDocumentStatus(SOConstants.DraftDocumentStatus);

      newOrder.setProcessed(false);
      DocumentType transactionDocumentType = BusinessEntityMapper.getDocType("POO", true);
      newOrder.setTransactionDocument(transactionDocumentType);
      newOrder.setScheduledDeliveryDate(new Date());
      PriceList pricelist = getPriceList(SOConstants.POPriceList);
      newOrder.setPriceList(pricelist);
      newOrder.setDocumentType(transactionDocumentType);
      newOrder.setDocumentNo(io.getDocumentNo() + "*IRC");
      newOrder.setAccountingDate(new Date());
      newOrder.setCurrency(io.getClient().getCurrency());
      newOrder.setWarehouse(io.getWarehouse());
      newOrder.setSalesTransaction(false);

    } catch (Exception e) {
      log.error("Error calculating promotions", e);
      e.printStackTrace();
      throw e;
    }
    return newOrder;
  }

  public List<OrderLine> createRTVOrderLine(Order newOrder, ShipmentInOutLine ioLine, double qty,String email) {

    long line = 10;
    List<OrderLine> orderlines = new ArrayList<OrderLine>();

    BigDecimal qtyToBeOrdered = BigDecimal.valueOf(qty);
    Product pr = ioLine.getProduct();
    OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
    newOrderLine.setLineNo(line);
    newOrderLine.setClient(newOrder.getClient());
    newOrderLine.setOrganization(newOrder.getOrganization());
    newOrderLine.setActive(true);
    newOrderLine.setCreationDate(new Date());
    newOrderLine.setCreatedBy(getUser(email));
    newOrderLine.setUpdatedBy(getUser(email));
    newOrderLine.setUpdated(new Date());
    newOrderLine.setOrderDate(new Date());
    newOrderLine.setSalesOrder(newOrder);
    newOrderLine.setProduct(pr);
    newOrderLine.setUOM(pr.getUOM());
    newOrderLine.setCurrency(newOrder.getClient().getCurrency());
    newOrderLine.setOrderedQuantity(qtyToBeOrdered.multiply(new BigDecimal("-1.0")));
    newOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
    newOrderLine.setReservedQuantity(BigDecimal.ZERO);
    newOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
    newOrderLine.setListPrice(pr.getPricingProductPriceList().get(0).getListPrice());
    newOrderLine.setUnitPrice(pr.getPricingProductPriceList().get(0).getStandardPrice());
    newOrderLine.setTax(getTaxRate(pr));
    newOrderLine.setDirectShipment(false);
    newOrderLine.setFreightAmount(BigDecimal.ZERO);
    newOrderLine.setLineNetAmount(qtyToBeOrdered.multiply(pr.getPricingProductPriceList().get(0)
        .getListPrice()));
    newOrderLine.setPriceLimit(qtyToBeOrdered.multiply(pr.getPricingProductPriceList().get(0)
        .getListPrice()));
    newOrderLine.setStandardPrice(qtyToBeOrdered.multiply(pr.getPricingProductPriceList().get(0)
        .getListPrice()));
    newOrderLine.setDescriptionOnly(false);
    newOrderLine.setGrossUnitPrice(qtyToBeOrdered.multiply(pr.getPricingProductPriceList().get(0)
        .getListPrice()));
    // newOrderLine.setWarehouse(getWarehouse(ioLine.getOrganization(), disputeWarehouse));
    newOrderLine.setWarehouse(ioLine.getShipmentReceipt().getWarehouse());

    orderlines.add(newOrderLine);

    return orderlines;

  }

  private PriceList getPriceList(String name) {
    OBCriteria<PriceList> priceListCrit = OBDal.getInstance().createCriteria(PriceList.class);
    priceListCrit.add(Restrictions.eq(PriceList.PROPERTY_NAME, name));
    List<PriceList> finPayTermCritList = priceListCrit.list();
    if (finPayTermCritList != null && finPayTermCritList.size() > 0) {
      return finPayTermCritList.get(0);
    } else {
      throw new OBException("PriceList not found");
    }
  }

  private PaymentTerm getPaymentTerm(String name) {
    OBCriteria<PaymentTerm> paymentTermCrit = OBDal.getInstance().createCriteria(PaymentTerm.class);
    paymentTermCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
    List<PaymentTerm> finPayTermCritList = paymentTermCrit.list();
    if (finPayTermCritList != null && finPayTermCritList.size() > 0) {
      return finPayTermCritList.get(0);
    } else {
      throw new OBException("payment term not found");
    }
  }

  private FIN_PaymentMethod getPaymentMethod(String name) {
    OBCriteria<FIN_PaymentMethod> paymentCrit = OBDal.getInstance().createCriteria(
        FIN_PaymentMethod.class);
    paymentCrit.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_NAME, name));
    List<FIN_PaymentMethod> finPayCritList = paymentCrit.list();
    if (finPayCritList != null && finPayCritList.size() > 0) {
      return finPayCritList.get(0);
    } else {
      throw new OBException("payment method not found");
    }
  }

  private TaxRate getTaxRate(Product product) {

    List<TaxRate> taxRateList = product.getTaxCategory().getFinancialMgmtTaxRateList();
    if (taxRateList == null || taxRateList.size() <= 0) {
      throw new OBException("specify tax rate to product " + product);
    }

    else
      return taxRateList.get(0);
  }

  private User getUser(String email) {
	   
	    OBCriteria<User> userCrit = OBDal.getInstance().createCriteria(User.class);
	    userCrit.add(Restrictions.eq(User.PROPERTY_EMAIL, email));
	    List<User> userCritList = userCrit.list();
	    if (userCritList != null && userCritList.size() > 0) {
	      return userCritList.get(0);
	    } else {
	      throw new OBException("user not found");
	    }
	  }

  private Warehouse getWarehouse(String name) {
    OBCriteria<Warehouse> warehouseCrit = OBDal.getInstance().createCriteria(Warehouse.class);
    warehouseCrit.add(Restrictions.eq(Warehouse.PROPERTY_NAME, name));
    List<Warehouse> warehouseCritList = warehouseCrit.list();
    if (warehouseCritList != null && warehouseCritList.size() > 0) {
      return warehouseCritList.get(0);
    } else {
      throw new OBException("warehouse not found");
    }
  }

}