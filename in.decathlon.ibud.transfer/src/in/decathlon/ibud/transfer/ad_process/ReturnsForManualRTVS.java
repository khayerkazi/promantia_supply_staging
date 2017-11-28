package in.decathlon.ibud.transfer.ad_process;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;

import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.json.JsonConstants;

public class ReturnsForManualRTVS extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    String m_inout_id = (String) bundle.getParams().get("M_InOut_ID"); // get shipment_id

    processVendorShipment(m_inout_id);

  }

  public void processVendorShipment(String m_inout_id) throws Exception, JSONException {
    String wsname = "in.decathlon.ibud.transfer.ReturnsTransferWS";

    ShipmentInOut reciept = OBDal.getInstance().get(ShipmentInOut.class, m_inout_id); // get RTVS

    if (reciept.getDocumentStatus().equals("DR")) {
      // call the process to complete shipment
      BusinessEntityMapper.executeProcess(m_inout_id, "109", "SELECT * FROM M_InOut_Post0(?)");
    }
    /*if (reciept.getMaterialMgmtShipmentInOutLineList() != null
        && reciept.getMaterialMgmtShipmentInOutLineList().size() > 0) {
      BusinessEntityMapper.txnSWMovementType(reciept);
    }*/
    JSONObject rtvHeaderJson = JSONHelper.convetBobToJson(reciept); // converts RTV header to Json

    JSONArray rtvLinesJson = createRtvLines(reciept); // convrts RTV lines to JsonArr

    rtvHeaderJson.put("ibodtrIsCreatedbyidl", (reciept.getSalesOrder() != null && reciept
        .getSalesOrder().isIbodtrIsCreatedbyidl()) ? "Y" : "N");
    JSONObject rtvs = new JSONObject();
    rtvs.put("Header", rtvHeaderJson);
    rtvs.put("Lines", rtvLinesJson);

    JSONObject rtvsToBeSent = new JSONObject();
    rtvsToBeSent.put(JsonConstants.DATA, rtvs);
    SessionHandler.getInstance().commitAndStart();
    JSONWebServiceInvocationHelper.sendPostrequest(wsname, "", rtvsToBeSent.toString());
  }

  private JSONArray createRtvLines(ShipmentInOut reciept) throws JSONException {
    JSONArray arr = new JSONArray();
    List<ShipmentInOutLine> rtvLines = reciept.getMaterialMgmtShipmentInOutLineList();
    for (ShipmentInOutLine iol : rtvLines) {
      arr.put(JSONHelper.convetBobToJson(iol));
    }
    return arr;

  }
}
