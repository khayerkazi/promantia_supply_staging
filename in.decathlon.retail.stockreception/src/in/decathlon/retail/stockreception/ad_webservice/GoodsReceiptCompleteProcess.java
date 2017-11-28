package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class GoodsReceiptCompleteProcess extends DalBaseProcess {

  public static final Logger log = Logger.getLogger(EnhancedProcessGoods.class);

  public static final String TransitBin = "Transit Bin";
  public static final String shuttleBin = "Shuttel Bin";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    try {
      OBContext.setAdminMode();

      final String recordID = (String) bundle.getParams().get("M_InOut_ID");
      ShipmentInOut shipmentInOut = OBDal.getInstance().get(ShipmentInOut.class, recordID);

      assert shipmentInOut.isSalesTransaction() : "This is not called for sales transactions";

      processReceipt(shipmentInOut,"");

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
  public void processReceipt(ShipmentInOut shipmentInOut,String email) throws Exception {

    BusinessEntityMapper.executeProcess(shipmentInOut.getId(), "104",
        "SELECT * FROM M_InOut_Post0(?)");
    
    OBDal.getInstance().refresh(shipmentInOut);
    shipmentInOut.setIbodtrCompletedTime(new Date());
    shipmentInOut.setUpdatedBy(getUser(email));
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

    SessionHandler.getInstance().commitAndStart();
    OBDal.getInstance().refresh(shipmentInOut);
    boolean closePo = closePOinStore(shipmentInOut,email);
    log.debug("Close PO: " + closePo);
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
  // this method closes the PO in store side, SO in supply side and goods shipment in supply side
  public boolean closePOinStore(ShipmentInOut shipmentInOut,String email) {

    try {
      boolean flag = false;

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
          String salesOrderRef = ord.getSwPoReference();
          String closedSoRef = ord.getIbdoGrReference();
          if (closedSoRef != null) {
            String[] closedSOList = closedSoRef.split("/");
            closedSoRefCount = closedSOList.length;
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

            OBDal.getInstance().save(orderLine);
          }
          OBDal.getInstance().flush();
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
          ord.setUpdatedBy(getUser(email));
          OBDal.getInstance().save(ord);
        }
        SessionHandler.getInstance().commitAndStart();
        log.debug("Purchase Order Closed");
        if (shipmentDoc.length() > 0) {
          log.debug("Call to Web Service to close SO and complete Shipment");
          String wsName = "in.decathlon.supply.stockreception.CompleteShipmentWS";
          JSONWebServiceInvocationHelper.sendPostrequest(wsName,
              "shipmentIdfromGRN=" + shipmentDoc, "{}");
        }
      }
      return flag;
    } catch (Exception e) {
      log.debug(e.getMessage());
      e.printStackTrace();
    }
    return false;
  }

  private String getdocStatusOfGR(String grn1) {
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
