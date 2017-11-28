package in.decathlon.ibud.transfer.ad_process;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;
import in.decathlon.ibud.shipment.store.UpdateStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.decathlon.warehouse.truckreception.DTRTruckReception;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/*
 * Completes Goods receipt and closes PO, Sends request to supply to complete shipment and close
 * sales order
 */

public class EnhancedProcessGoods extends DalBaseProcess {

  public static final Logger log = Logger.getLogger(EnhancedProcessGoods.class);

  public static final String TransitBin = "Transit Bin";
  public static final String shuttleBin = "Shuttel Bin";
  public static String processid = "";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    try {
      OBContext.setAdminMode();
      
      processid = bundle.getProcessId();

      final String recordID = (String) bundle.getParams().get("M_InOut_ID");
      ShipmentInOut shipmentInOut = OBDal.getInstance().get(ShipmentInOut.class, recordID);

      assert shipmentInOut.isSalesTransaction() : "This is not called for sales transactions";

      processReceipt(shipmentInOut);

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

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /*
   * Completes the receipt and closes the Po and processes the supply flow
   */
  public void processReceipt(ShipmentInOut shipmentInOut) throws Exception {
    JSONObject soDetails = new JSONObject();
    try {
      String shipmentDoc = shipmentInOut.getDocumentNo();
      if (shipmentDoc.length() > 0) {
        log.debug("Call to Web Service to close SO and complete Shipment");
        String wsName = "in.decathlon.ibud.shipment.CompleteShipmentWS";
        soDetails = JSONWebServiceInvocationHelper.sendPostrequestToClose(wsName,
            "shipmentIdfromGRN=" + shipmentDoc, "{}");
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error from Supply " + e);
      throw new OBException("Error from Supply " + e);
    }

    try {
      if (shipmentInOut.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
        throw new OBException("GRN is already closed so not proceeding to CL the PO ");
      }
      BusinessEntityMapper.executeProcess(shipmentInOut.getId(), "104",
          "SELECT * FROM M_InOut_Post0(?)");

      completeTruck(shipmentInOut);

      OBDal.getInstance().refresh(shipmentInOut);
      shipmentInOut.setIbodtrCompletedTime(new Date());
      OBDal.getInstance().save(shipmentInOut);
      List<ShipmentInOutLine> grLineList = shipmentInOut.getMaterialMgmtShipmentInOutLineList();

      // Update custom movement type field that is set when M_Inout_Post is called

      List<MaterialTransaction> txnList = null;
      for (ShipmentInOutLine grnLine : grLineList) {
        txnList = grnLine.getMaterialMgmtMaterialTransactionList();
        for (MaterialTransaction txn : txnList) {
          txn.setSwMovementtype(shipmentInOut.getSWMovement());
          OBDal.getInstance().save(txn);
        }
      }

      // SessionHandler.getInstance().commitAndStart();
      // OBDal.getInstance().refresh(shipmentInOut);
      updateRecQty(shipmentInOut, soDetails);
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error in retail " + e);
      throw new OBException("Error in retail " + e);
    }
  }

  public static void updateRecQty(ShipmentInOut goodShipment, JSONObject soDetails) {

    try {
        Map<String, BigDecimal> inoutlineMap = new HashMap<String, BigDecimal>();
      HashMap<String, String> respHashMap = new HashMap<String, String>();
      JSONObject respObj = new JSONObject();
      if (soDetails.isNull("data")) {
        log.error("error in retrieval");
        throw new OBException("error in retrieval of json object");
      }
      // HashMap<String, String> map = new HashMap<String, String>();
      respObj = soDetails.getJSONObject("data");
      Iterator<?> keys = respObj.keys();

      while (keys.hasNext()) {
        String key = (String) keys.next();
        String value = respObj.getString(key);
        respHashMap.put(key, value);

      }

      for (ShipmentInOutLine inoutLine : goodShipment.getMaterialMgmtShipmentInOutLineList()) {
        OrderLine ordLine = inoutLine.getSalesOrderLine();
        if (ordLine != null) {
          Order ord = ordLine.getSalesOrder();
          if (ord != null) {
            if (ord.getDocumentStatus().equals(SOConstants.DraftDocumentStatus)) {
              ImmediateSOonPO poBook = new ImmediateSOonPO();
              poBook.processRequest(ord);
              if (ord.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
                ord.setIbdoCreateso(true);
                ord.setIbdoCreatingso(true);
                OBDal.getInstance().save(ord);
              }
            }
            String docNo = ord.getDocumentNo();
            if (docNo != null) {
              try {
                if (respHashMap.containsKey(docNo)) {
                  String soDocStatus = respHashMap.get(docNo);
                  if (soDocStatus != null && !(soDocStatus.equals("NA"))) {
                    ord.setDocumentStatus(soDocStatus);
                    if (soDocStatus.equals(SOConstants.partialRecieved))
                      ord.setDocumentAction(SOConstants.closed);
                    else if (soDocStatus.equals(SOConstants.closed)){
                      ord.setDocumentAction("--");
                      if (ord.isSwIsimplantation()) {
                          UpdateStatus.setBlockedQtyForImpl(docNo, ord, inoutlineMap);
                    
                  }

                      
                    }
                    ord.setProcessed(true);
                    OBDal.getInstance().save(ord);
                    OBDal.getInstance().flush();
                  }
                } else {
                  log.debug("no details to update doc status ");
                }
              } catch (Exception e) {
                e.printStackTrace();
                log.error("no details to update doc status ");
              }
            } else {
              throw new OBException(" no document no for the order " + ord);
            }
          } else {
            throw new OBException(" no order reference for the orderline " + ordLine);

          }

          // set received qty in order line
          String curReceivedQtyStr = ordLine.getSWEMSwRecqty();
          BigDecimal currentRecQtyDec = new BigDecimal(curReceivedQtyStr);
          BigDecimal newRecQty = BigDecimal.ZERO;
          // recheck for null values
          // newRecQty = newRecQty.add(currentRecQtyDec.add(inoutLine.getMovementQuantity()));
          newRecQty = inoutLine.getMovementQuantity();
          newRecQty = newRecQty.add(currentRecQtyDec);
          String strRecQty = newRecQty.toString();
          if (strRecQty.contains(".")) {
            strRecQty = strRecQty.substring(0, strRecQty.indexOf('.'));
          }
          
          BigDecimal confirmqty = new BigDecimal(ordLine.getSwConfirmedqty());
          
            try {
          	  if(confirmqty.compareTo(newRecQty)==-1){
         		  throw new OBException(" Received Quantity is more than Confirmed quantity " + ordLine); 
          	  } else {
          		ordLine.setSWEMSwRecqty(strRecQty);
                OBDal.getInstance().save(ordLine);
          	  }
            } catch (Exception e){
          	  JSONObject errorObject = new JSONObject();
              errorObject.put(SOConstants.RECORD_ID, ordLine.getId());
              errorObject.put(SOConstants.recordIdentifier, ordLine.getSalesOrder().getDocumentNo());
          	  BusinessEntityMapper.createErrorLogRecord(e, processid, errorObject);
            }

        } else
          throw new OBException(" no orderline reference for the inoutline " + inoutLine);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new OBException("error in updating of orderline with  " + e);
    }

  }

  private void completeTruck(ShipmentInOut shipmentInOut) {
    String qry = "select det.goodsShipment from DTR_Truck_Reception dtr join dtr.dTRTruckDetailsList det "
        + " where dtr.id = (select dtr.id from DTR_Truck_Reception dtr join dtr.dTRTruckDetailsList det "
        + " where det.goodsShipment.id ='"
        + shipmentInOut.getId()
        + "')  and det.goodsShipment.id <>'" + shipmentInOut.getId() + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    List<ShipmentInOut> queryList = query.list();
    Boolean allRecClosed = true;
    for (ShipmentInOut shipOut : queryList) {
      if (shipOut.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
        allRecClosed = true;
      } else {
        allRecClosed = false;
        break;
      }
    }
    if (allRecClosed.equals(true)) {
      DTRTruckReception truck;
      String truckQry = "select dtr from DTR_Truck_Reception dtr join dtr.dTRTruckDetailsList det "
          + "where det.goodsShipment.id =:shipmentId";
      Query truckQuery = OBDal.getInstance().getSession().createQuery(truckQry);
      truckQuery.setParameter("shipmentId", shipmentInOut.getId());
      List<DTRTruckReception> truckList = truckQuery.list();
      if (truckList != null & truckList.size() > 0) {
        truck = truckList.get(0);
        truck.setDocumentStatus(SOConstants.CompleteDocumentStatus);
        OBDal.getInstance().save(truck);
      }
    }
  }

  // this method closes the PO in store side, SO in supply side and goods shipment in supply side
  public void closePOinStore(ShipmentInOut shipmentInOut) throws Exception {
    try {
      String shipmentDoc = shipmentInOut.getDocumentNo();
      int indexOfLast = shipmentDoc.lastIndexOf("*");
      String newShipmentDoc = shipmentDoc;
      String poDocType = BusinessEntityMapper.getTrasactionDocumentType(
          shipmentInOut.getOrganization()).getName();
      boolean isSalesTransaction = shipmentInOut.isSalesTransaction();
      String grnDocStatus = "";

      if (indexOfLast >= 0)
        newShipmentDoc = shipmentDoc.substring(0, indexOfLast);
      String ordQry;
      if (isSalesTransaction)
        ordQry = "id in (select ord.id from Order ord where ord.documentNo = '" + shipmentDoc
            + "' and ord.salesTransaction = :isSalesTransaction)";
      else
        ordQry = "id in (select ord.id from Order ord where ord.documentNo = '" + newShipmentDoc
            + "' and ord.salesTransaction = :isSalesTransaction "
            + " and ord.documentType.name like '" + poDocType + "')";

      OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class, ordQry);
      orderQuery.setNamedParameter("isSalesTransaction", isSalesTransaction);
      List<Order> orderList = orderQuery.list();
      int grnDocCount = 0;
      int noOfDoc = 0;
      int closedSoRefCount = 0;
      if (orderList != null && orderList.size() > 0) {
        for (Order ord : orderList) {
          if (ord.getDocumentStatus().equals(SOConstants.DraftDocumentStatus)) {
            ImmediateSOonPO poBook = new ImmediateSOonPO();
            poBook.processRequest(ord);
            if (ord.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
              ord.setIbdoCreateso(true);
              ord.setIbdoCreatingso(true);
              OBDal.getInstance().save(ord);
            }
          }
          OBDal.getInstance().refresh(ord);
          String salesOrderRef = ord.getSwPoReference();
          String closedSoRef = ord.getIbdoGrReference();
          if (closedSoRef != null) {
            String[] closedSOList = closedSoRef.split("/");
            closedSoRefCount = closedSOList.length;
          }
          if (salesOrderRef == null) {
            throw new OBException("No so reference set in the PO " + ord);
          }
          String[] salesOrderList = salesOrderRef.split("/");
          int salesOrderListCount = salesOrderList.length;
          if (salesOrderList != null) {

            if (closedSoRef != null) {
              for (String salesOrder : salesOrderList) {
                if (!closedSoRef.toString().contains(salesOrder)) {
                  grnDocStatus = getdocStatusOfGR(salesOrder);
                  if (grnDocStatus.equals(SOConstants.CompleteDocumentStatus)) {
                    grnDocCount++;
                  }
                }
              }
            } else {
              for (String salesOrder : salesOrderList) {
                grnDocStatus = getdocStatusOfGR(salesOrder);
                if (grnDocStatus.equals(SOConstants.CompleteDocumentStatus)) {
                  grnDocCount++;
                }

              }
            }
          }
          noOfDoc = salesOrderListCount - closedSoRefCount;
          log.debug("update po line received qty after completing grn and before closing po");
          // update po line received quantity after completing grn and before closing po
          for (OrderLine orderLine : ord.getOrderLineList()) {
            String prevRecvdQty = orderLine.getSWEMSwRecqty();
            if (prevRecvdQty == null)
              prevRecvdQty = "0";
            double currentRecivedQty = 0;

            currentRecivedQty = Double.parseDouble(prevRecvdQty);
            BigDecimal gRNReceivedQty = new BigDecimal(0);
            for (ShipmentInOutLine inOutLine : orderLine.getMaterialMgmtShipmentInOutLineList()) {
              if (inOutLine.getShipmentReceipt().getId().equals(shipmentInOut.getId()))
                gRNReceivedQty = gRNReceivedQty.add(inOutLine.getMovementQuantity());
            }

            double newReserveQty = gRNReceivedQty.doubleValue();
            String strReserveQty = String.valueOf(currentRecivedQty + newReserveQty);
            if (strReserveQty.contains("."))
              strReserveQty = strReserveQty.substring(0, strReserveQty.indexOf('.'));
            orderLine.setSWEMSwRecqty(strReserveQty);

            /*
             * if (orderLine.getSalesOrder().isSwIsimplantation()) { // Setting blocked qty if PO is
             * of type implantation BigDecimal blockedQty = new BigDecimal(strReserveQty);
             * CLImplantation implRecord = BusinessEntityMapper.getImplantationOrg(orderLine
             * .getOrganization(), orderLine.getProduct());
             * log.debug("Setting blocked qty to the org " + implRecord.getStoreImplanted() +
             * "with product" + implRecord.getProduct()); implRecord.setBLOCKEDQTY(blockedQty); if
             * (blockedQty == BigDecimal.valueOf(implRecord.getImplantationQty())) {
             * implRecord.setImplanted(true); }
             * log.debug("blocked qty has been set to the received qty " + blockedQty);
             * OBDal.getInstance().save(implRecord); }
             */
            OBDal.getInstance().save(orderLine);
          }
          // OBDal.getInstance().flush();
          // Review this code
          if (grnDocCount == noOfDoc) {
            ord.setDocumentStatus("CL");
            ord.setDocumentAction("--");
            ord.setProcessed(true);
          } else {
            ord.setDocumentStatus("IBDO_PR");
            ord.setDocumentAction("CL");
            ord.setProcessed(true);
          }
          OBDal.getInstance().save(ord);
        }
        // SessionHandler.getInstance().commitAndStart();
        log.debug("Purchase Order Closed");
        /*
         * if (shipmentDoc.length() > 0) {
         * log.debug("Call to Web Service to close SO and complete Shipment"); String wsName =
         * "in.decathlon.ibud.shipment.CompleteShipmentWS";
         * JSONWebServiceInvocationHelper.sendPostrequest(wsName, "shipmentIdfromGRN=" +
         * shipmentDoc, "{}"); }
         */
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      throw e;
    }
  }

  public static String getdocStatusOfGR(String grn1) {
    OBCriteria<ShipmentInOut> grnCrit = OBDal.getInstance().createCriteria(ShipmentInOut.class);
    grnCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, grn1));
    grnCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, false));
    grnCrit.setMaxResults(1);
    if (grnCrit.count() == 1) {
      return grnCrit.list().get(0).getDocumentStatus();
    } else
      return "";
  }
}
