package in.decathlon.ibud.picklistext;

import in.decathlon.ibud.orders.client.SOConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.PickingList;

public class ShipmentActionHandler extends BaseActionHandler {

  Logger log = Logger.getLogger(CompleteActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    System.out.println("Inside CompleteActionHAndler");
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      final String strAction = jsonRequest.getString("action");
      if (strAction.equals("ship")) {
        JSONObject jsonMsg = processShipment(jsonRequest);
        jsonResponse.put("message", jsonMsg);
        return jsonResponse;
      }

    } catch (Exception e) {
      log.error("Error in AssignActionHandler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", "".equals(message) ? e : message);
        jsonResponse.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error generating the error message", e2);
      }
    }
    return jsonResponse;
  }

  private JSONObject processShipment(JSONObject jsonRequest) throws JSONException,
      PropertyException {
    final JSONObject jsonMsg = new JSONObject();
    jsonMsg.put("severity", "success");
    jsonMsg.put("text", OBMessageUtils.messageBD("Success"));
    final JSONArray pickingIds = jsonRequest.getJSONArray("pickings");
    for (int i = 0; i < pickingIds.length(); i++) {
      final String strPickingId = pickingIds.getString(i);
      final PickingList picking = OBDal.getInstance().get(PickingList.class, strPickingId);
      if (!picking.getPickliststatus().equals("CL")) {
        throw new OBException(OBMessageUtils.messageBD("IBUDPK_ProcessShipmentStatusError"));
      } else {
        executeProcess(picking);
      }
      picking.setPickliststatus(SOConstants.pickListShippedStatus);
      OBDal.getInstance().save(picking);

    }
    return jsonMsg;
  }

  public void executeProcess(final PickingList picking) {
    List<String> documentNos = getDoucumentNos(picking);
    for (String docNo : documentNos) {
      Order ord = getSalesOrder(docNo);
      if (ord != null && !(ord.getDocumentStatus().equals(SOConstants.closed))) {
        ord.setDocumentStatus(SOConstants.shipped);
      }
      else if(ord == null){
        throw new OBException("picklist description should have salesOrder document no. " + docNo);
      }
    }
  }

  public void executeProcessForZeroLines(final PickingList picking) {
	  boolean dontCloseSO=false;
    List<String> documentNos = getDoucumentNos(picking);
    for (String docNo : documentNos) {
      Order ord = getSalesOrder(docNo);
      if (ord != null) {
    	  dontCloseSO = getPickListLines(ord,picking);
    	  if(!dontCloseSO){
    		  ord.setDocumentStatus(SOConstants.closed);
    		  ord.setObwplIsinpickinglist(false);
    		  OBDal.getInstance().save(ord);
    		  dontCloseSO=false;
    	  }
      } else {
        throw new OBException("picklist description should have salesOrder document no. " + docNo);
      }

    }
  }

  private boolean getPickListLines(Order ord, PickingList picking) {
	  String qry = " id in (select id from MaterialMgmtInternalMovementLine ml where ml.oBWPLWarehousePickingList.id= '"+picking.getId()+"' and" 
                    +" ml.stockReservation.salesOrderLine.salesOrder.id= '"+ord.getId()+"' )";
	  OBQuery<InternalMovementLine> query = OBDal.getInstance().createQuery(InternalMovementLine.class, qry);
	  List<InternalMovementLine> mlList = query.list();
	  if(mlList != null && mlList.size()>0)
		  return true;
	  return false;
}

private List<String> getDoucumentNos(PickingList picking) {
    List<String> documentNos = new ArrayList<String>();
    String[] descriptions = picking.getDescription().split(",");
    for (String des : descriptions) {
      String temp = des.trim();
      String[] tokens = temp.split(" ");
      documentNos.add(tokens[3]);
    }
    return documentNos;
  }

  private Order getSalesOrder(String docNo) {
    OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
    ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, docNo));
    ordCrit.setMaxResults(1);
    if (ordCrit.count() > 0)
      return ordCrit.list().get(0);
    else
      return null;
  }

}
