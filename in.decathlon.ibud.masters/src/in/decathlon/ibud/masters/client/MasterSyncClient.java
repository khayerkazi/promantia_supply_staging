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
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class MasterSyncClient extends DalBaseProcess {

  private static final Logger log = Logger.getLogger(MasterSyncClient.class);
  static JSONWebServiceInvocationHelper masterHandler = new JSONWebServiceInvocationHelper();
  private static ProcessLogger logger;

  protected void doExecute(ProcessBundle bundle) throws Exception {
    String processid = bundle.getProcessId();
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      logger = bundle.getLogger();
      log.debug("Inside MasterSyncClient class to GET master data");
      logger.log("Requesting Supply to get data");
      JSONObject jsonObj = masterHandler.sendGetrequest(true, "Product",
          "in.decathlon.ibud.masters.MasterWS", processid, logger);
      BusinessEntityMapper.setLastUpdatedTime(updated, "Product");
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
    }
  }

  // processServerData(jsonObj);

  @SuppressWarnings("unused")
  public void processServerData(JSONObject json) throws Exception {

    boolean finalResult = false;
    try {

      final JSONArray productJsonArray = (JSONArray) JSONHelper.getContentAsJSON(json.toString());
      log.debug("Products in json  " + productJsonArray);
      logger.log(" Total Products " + productJsonArray.length());

      final JSONArray modelJsonArray = (JSONArray) getOtherMasters(json.toString(), "CLModel");
      log.debug("model in json  " + modelJsonArray);
      logger.log(" Total model " + modelJsonArray.length());

      final JSONArray productCatJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "ProductCategory");
      log.debug("Product Categories in json  " + productCatJsonArray);
      logger.log(" Total ProductCategories " + productCatJsonArray.length());

      final JSONArray taxCatJsonArray = (JSONArray) getOtherMasters(json.toString(), "TaxCategory");
      log.debug("Tax Category in json  " + taxCatJsonArray);
      logger.log(" Total TaxCategories " + taxCatJsonArray.length());

      final JSONArray uomJsonArray = (JSONArray) getOtherMasters(json.toString(), "UOM");
      log.debug("Uom in json  " + uomJsonArray);
      logger.log(" Total UOM's " + uomJsonArray.length());

      final JSONArray colorJsonArray = (JSONArray) getOtherMasters(json.toString(), "Color");
      log.debug("Color in json " + colorJsonArray);
      logger.log(" Total Colors " + colorJsonArray.length());

      final JSONArray attributeArray = (JSONArray) getOtherMasters(json.toString(), "Attribute");
      log.debug("Attribute in json " + attributeArray);
      logger.log(" Total Attributes " + attributeArray.length());

      final JSONArray attributeSetInsArray = (JSONArray) getOtherMasters(json.toString(),
          "AttributeSetInstance");
      log.debug("AttributeSet in json  " + attributeSetInsArray);
      logger.log(" Total AttributeSetInstances " + attributeSetInsArray.length());

      final JSONArray attributeInsArray = (JSONArray) getOtherMasters(json.toString(),
          "AttributeInstance");
      log.debug("AttributeInstance  " + attributeInsArray);
      logger.log(" Total AtrributeInstances " + attributeInsArray.length());

      final JSONArray attributeSetArray = (JSONArray) getOtherMasters(json.toString(),
          "AttributeSet");
      log.debug("AttributeSet in json  " + attributeSetArray);
      logger.log(" Total AttributeSets " + attributeSetArray.length());

      final JSONArray attributeUseArray = (JSONArray) getOtherMasters(json.toString(),
          "AttributeUse");
      log.debug("AttributeUse in json  " + attributeUseArray);
      logger.log(" Total AttributeUses " + attributeUseArray.length());

      final JSONArray attributeValueArray = (JSONArray) getOtherMasters(json.toString(),
          "AttributeValue");
      log.debug("Attribute Value in json  " + attributeValueArray);
      logger.log(" Total Attribute Values " + attributeValueArray.length());

      final JSONArray brandArray = (JSONArray) getOtherMasters(json.toString(), "CLBrand");
      log.debug("Brands in json  " + brandArray);
      logger.log(" Total brands " + brandArray.length());

      final JSONArray universeArray = (JSONArray) getOtherMasters(json.toString(), "CLUniverse");
      log.debug("universes in json  " + universeArray);
      logger.log(" Total universes " + universeArray.length());

      final JSONArray storeDepArray = (JSONArray) getOtherMasters(json.toString(), "CLStoreDept");
      log.debug("StoreDepts in json  " + storeDepArray);
      logger.log(" Total StoreDepts " + storeDepArray.length());

      final JSONArray depArray = (JSONArray) getOtherMasters(json.toString(), "CLDepartment");
      log.debug("Departments in json  " + depArray);
      logger.log(" Total Departments " + depArray.length());

      final JSONArray natureOfPrArray = (JSONArray) getOtherMasters(json.toString(),
          "CLNatureOfProduct");
      log.debug("NatureOfProduct in json  " + natureOfPrArray);
      logger.log(" Total NatureOfProducts " + natureOfPrArray.length());

      final JSONArray componentArray = (JSONArray) getOtherMasters(json.toString(),
          "CLCOMPONENTBRAND");
      log.debug("NatureOfProduct in json  " + componentArray);
      logger.log(" Total NatureOfProducts " + componentArray.length());

      final JSONArray taxRateArray = (JSONArray) getOtherMasters(json.toString(), "TaxRate");
      log.debug("TaxRates in json  " + taxRateArray);
      logger.log(" Total TaxRates " + taxRateArray.length());

      final JSONArray taxZoneArray = (JSONArray) getOtherMasters(json.toString(), "TaxZone");
      log.debug("TaxZones in json  " + taxZoneArray);
      logger.log(" Total TaxZones " + taxZoneArray.length());

      final JSONArray taxRateAccArray = (JSONArray) getOtherMasters(json.toString(),
          "TaxRateAccounts");
      log.debug("TaxRateAccArray in json  " + taxRateAccArray);
      logger.log(" Total TaxRateAccArray " + taxRateAccArray.length());

      boolean taxCatAddResult = JSONHelper.saveJSONObject(taxCatJsonArray, logger);
      boolean taxRateAddResult = JSONHelper.saveJSONObject(taxRateArray, logger);
      boolean taxZoneAddResult = JSONHelper.saveJSONObject(taxZoneArray, logger);
      // boolean taxRateAccAddResult = JSONHelper.saveJSONObject(taxRateAccArray);
      boolean brandAddResult = JSONHelper.saveJSONObject(brandArray, logger);
      boolean universeAddResult = JSONHelper.saveJSONObject(universeArray, logger);
      boolean storeDeptAddResult = JSONHelper.saveJSONObject(storeDepArray, logger);
      boolean deptAddResult = JSONHelper.saveJSONObject(depArray, logger);
      boolean componentAddResult = JSONHelper.saveJSONObject(componentArray, logger);
      boolean natOfPrAddResult = JSONHelper.saveJSONObject(natureOfPrArray, logger);
      boolean colorAddresult = JSONHelper.saveJSONObject(colorJsonArray, logger);
      boolean modelAddResult = JSONHelper.saveJSONObject(modelJsonArray, logger);
      boolean productCatAddResult = JSONHelper.saveJSONObject(productCatJsonArray, logger);
      boolean uomAddResult = JSONHelper.saveJSONObject(uomJsonArray, logger);
      boolean attrAddResult = JSONHelper.saveJSONObject(attributeArray, logger);
      boolean attrSetInsAddResult = JSONHelper.saveJSONObject(attributeSetInsArray, logger);
      boolean attrInsAddResult = JSONHelper.saveJSONObject(attributeInsArray, logger);
      boolean attrSetAddResult = JSONHelper.saveJSONObject(attributeSetArray, logger);
      boolean attrUseAddResult = JSONHelper.saveJSONObject(attributeUseArray, logger);
      boolean attrValueAddResult = JSONHelper.saveJSONObject(attributeValueArray, logger);

      finalResult = JSONHelper.saveJSONObject(productJsonArray, logger);
      JSONObject lastProduct = productJsonArray.getJSONObject(productJsonArray.length() - 1);

      String LastUpdatedTime = lastProduct.getString("updatedTime");
      SimpleDateFormat readFormat = new SimpleDateFormat("EE MMM dd hh:mm:ss z yyyy");

      Date date = null;

      date = readFormat.parse(LastUpdatedTime);

      SimpleDateFormat writeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      String formattedDate = "";
      if (date != null) {
        formattedDate = writeFormat.format(date);
      }

      short updatedRow = (short) BusinessEntityMapper.setLastUpdatedTime(formattedDate, "Product");

    } catch (Exception e) {
      log.error(e);
      if (e.getMessage().contains("JSONObject[\"data\"] not found"))
        logger.log("Supply failed to respond");
      logger.log(e.getMessage());
      e.printStackTrace();

    }
    log.info("Final result " + finalResult);

  }

  private Object getOtherMasters(String content, String master) throws JSONException {
    Check.isNotNull(content, "Content must be set");
    Object jsonMasterList = null;
    JSONObject jsonObj = new JSONObject(content);
    jsonMasterList = jsonObj.get(master);
    return jsonMasterList;
  }

}
