package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class OrganizationSyncMaster extends DalBaseProcess {

  private static final Logger log = Logger.getLogger(MasterSyncClient.class);
  static JSONWebServiceInvocationHelper masterHandler = new JSONWebServiceInvocationHelper();
  private static ProcessLogger logger;
  String tableDetails = "";

  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      log.debug("Inside MasterSyncClient class to GET master data");
      logger.log("Requesting Supply to get data");
      masterHandler.sendGetrequest(true, "Organization", "in.decathlon.ibud.masters.OrgSyncWS",
          processid, logger);
      BusinessEntityMapper.setLastUpdatedTime(updated, "Organization");
    }

    catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
    }

    // processServerData(jsonObj);

  }

  @SuppressWarnings("unused")
  public void processServerData(JSONObject json) throws Exception {
    boolean organizationInfo = false;

    try {
      // Getting Organization details from supply's response
      final JSONArray organizationJsonArray = (JSONArray) JSONHelper.getContentAsJSON(json
          .toString());
      log.debug("Organizations in json  " + organizationJsonArray);
      logger.log(" Total Organizations " + organizationJsonArray.length());

      /*
       * final JSONArray orgTypeJsonArray = (JSONArray) getOtherMasters(json.toString(),
       * "OrganizationType"); log.debug("Organization type in json " + orgTypeJsonArray);
       * logger.log(" Total OrganizationType " + orgTypeJsonArray.length());
       */

      final JSONArray clientJsonArray = (JSONArray) getOtherMasters(json.toString(), "Client");
      log.debug("Client in json " + clientJsonArray);
      logger.log(" Total Clients " + clientJsonArray.length());

      final JSONArray generalLedgerJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "GeneralLedger");
      log.debug("general ledger in json " + generalLedgerJsonArray);
      logger.log(" Total general ledger " + generalLedgerJsonArray.length());

      final JSONArray organizationInfoJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "OrganizationInformation");
      log.debug("Organization Info in json " + organizationInfoJsonArray);
      logger.log(" Total Organization Info in json " + organizationInfoJsonArray.length());

      final JSONArray bpLocationJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "bpLocation");
      log.debug("Business partner location in json " + bpLocationJsonArray);
      logger.log("Total bpLocation in json " + bpLocationJsonArray.length());

      final JSONArray bPartnerCatJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "BPCategory");
      log.debug(" BPCategory in json " + bPartnerCatJsonArray);
      logger.log(" Total bpCategory in json " + bPartnerCatJsonArray);

      final JSONArray bPartnerJsonArray = (JSONArray) getOtherMasters(json.toString(), "BPartner");
      log.debug(" bPartner in json " + bPartnerJsonArray);
      logger.log(" Total bPartner in json" + bPartnerJsonArray.length());

      final JSONArray locationJsonArray = (JSONArray) getOtherMasters(json.toString(), "Location");
      log.debug(" Location in json " + locationJsonArray);
      logger.log(" Total location in json" + locationJsonArray.length());

      final JSONArray contactJsonArray = (JSONArray) getOtherMasters(json.toString(), "Contact");
      log.debug(" Contact in json " + contactJsonArray);
      logger.log(" Total no of contacts in json" + contactJsonArray);

      final JSONArray companyImageJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "CompanyImage");
      log.debug(" Company Image in json " + companyImageJsonArray);
      logger.log(" Total no of companyImage in json" + companyImageJsonArray.length());

      JSONHelper.saveJSONObject(clientJsonArray, logger);
      JSONHelper.saveJSONObject(generalLedgerJsonArray, logger);
      OBContext.setAdminMode(true);
      JSONHelper.saveJSONObject(organizationJsonArray, logger);
      OBContext.restorePreviousMode();

      JSONHelper.saveJSONObject(locationJsonArray, logger);
      JSONHelper.saveJSONObject(contactJsonArray, logger);
      JSONHelper.saveJSONObject(bPartnerCatJsonArray, logger);
      JSONHelper.saveJSONObject(bPartnerJsonArray, logger);

      OBContext.setAdminMode(true);
      JSONHelper.saveJSONObject(organizationInfoJsonArray, logger);
      OBContext.restorePreviousMode();

      JSONObject lastOrganizationInfo = organizationInfoJsonArray
          .getJSONObject(organizationInfoJsonArray.length() - 1);

      String LastUpdatedTime = lastOrganizationInfo.getString("updatedTime");

      SimpleDateFormat readFormat = new SimpleDateFormat("EE MMM dd hh:mm:ss z yyyy");

      Date date = null;

      date = readFormat.parse(LastUpdatedTime);

      SimpleDateFormat writeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      String formattedDate = "";
      if (date != null) {
        formattedDate = writeFormat.format(date);
      }

      short updatedRow = (short) BusinessEntityMapper.setLastUpdatedTime(formattedDate,
          "Organization");

    } catch (Exception e) {
      log.error(e);
      if (e.getMessage().contains("JSONObject[\"data\"] not found"))
        logger.log("Supply failed to respond");
      logger.log(e.getMessage());
      e.printStackTrace();

    }
    log.info("Final result " + organizationInfo);
    logger.log("Final result" + organizationInfo);

  }

  private Object getOtherMasters(String content, String master) throws JSONException {
    Check.isNotNull(content, "Content must be set");
    Object jsonMasterList = null;
    JSONObject jsonObj = new JSONObject(content);
    jsonMasterList = jsonObj.get(master);
    return jsonMasterList;
  }

}