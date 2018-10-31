package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class BusinessPartnerSync extends DalBaseProcess {

  private static final Logger log = Logger.getLogger(MasterSyncClient.class);
  static JSONWebServiceInvocationHelper masterHandler = new JSONWebServiceInvocationHelper();
  private static ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try {
      log.debug("Inside MasterSyncClient class to GET master data");
      JSONObject jsonObj = masterHandler.sendGetrequest(true, "Partner",
          "in.decathlon.ibud.masters.PartnerWS", processid, logger);

      boolean result = processServerData(jsonObj);
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
    }
  }

  private boolean processServerData(JSONObject jsonObj) {
    try {
      JSONArray partnerJsonArray = (JSONArray) JSONHelper.getContentAsJSON(jsonObj.toString());
      log.debug("Products in json  " + partnerJsonArray);

    } catch (JSONException e) {
      e.printStackTrace();
    }

    return false;
  }
}
