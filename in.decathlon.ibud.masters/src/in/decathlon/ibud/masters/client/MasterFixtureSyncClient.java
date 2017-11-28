package in.decathlon.ibud.masters.client;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLModel;

public class MasterFixtureSyncClient extends DalBaseProcess {

  private static final Logger log = Logger.getLogger(MasterSyncClient.class);
  private static ProcessLogger logger;

  protected void doExecute(ProcessBundle bundle) throws Exception {
    String processid = bundle.getProcessId();
    try {
      boolean status = false;
      logger = bundle.getLogger();
      log.info("Inside MasterFixtureSync class to GET master data");
      logger.log("Request to Supply");

      HttpURLConnection hc;

      // calling Master WS
      try {
        hc = createConnection();
        hc.connect();
        // Getting the Response from the Web service
        BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
        String inputLine;
        StringBuffer resp = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          resp.append(inputLine);
        }

        String response = resp.toString();
        final JSONObject responseJsonObject = new JSONObject(response);
        // System.out.println("JSonObject Response->" + responseJsonObject);
        logger.log("\nReceived Supply data");
        logger.log("Copying Supply data to Retail");

        status = copyMaster(responseJsonObject);
        if (!status) {
          logger.log("\nCopying Supply Master data to Retail failed");
        } else {
          logger.log("\nSuccessfully copied Supply Master Data to Retail");
          DSIDEFModuleConfig config = null;
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
          Date today = Calendar.getInstance().getTime();
          String updatedTime = df.format(today);
          OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(
              DSIDEFModuleConfig.class);
          configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME,
              "in.decathlon.ibud.masters"));
          configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_KEY, "updatedDate"));
          final List<DSIDEFModuleConfig> configList = configInfoObCriteria.list();

          if (!configList.isEmpty()) {
            config = configList.get(0);
            config.setSearchKey(updatedTime);
            OBDal.getInstance().save(config);
            // System.out.println("UpdatedDate After Success->" + updatedTime);

          }
        }
        in.close();
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
    }
  }

  private boolean copyMaster(JSONObject responseJsonObject) throws Exception {

    boolean flag = false;
    boolean modelStatus = false;
    boolean productStatus = false;
    boolean priceStatus = false;

    final JSONArray modelArray = (JSONArray) getOtherMasters(responseJsonObject.toString(), "model");
    logger.log("\n Total Models-> " + modelArray.length());

    final JSONArray productArray = (JSONArray) getOtherMasters(responseJsonObject.toString(),
        "product");
    logger.log("\n Total Products-> " + productArray.length());

    final JSONArray priceArray = (JSONArray) getOtherMasters(responseJsonObject.toString(), "price");
    logger.log("\n Total ProductPrices-> " + priceArray.length());
    // System.out.println("JSonObject for Model->" + modelArray);
    // System.out.println("JSonObject for Product->" + productArray);
    // System.out.println("JSonObject for Price->" + priceArray);

    if (modelArray.length() > 0) {
      modelStatus = createModelData(modelArray, responseJsonObject);
    } else
      modelStatus = true;
    if (productArray.length() > 0) {
      productStatus = createProductData(productArray);
    } else
      productStatus = true;
    if (priceArray.length() > 0) {
      priceStatus = createPriceData(priceArray);
    } else
      priceStatus = true;
    if (modelStatus == true && productStatus == true && priceStatus == true) {
      flag = true;
    }
    return flag;
  }

  private boolean createModelData(JSONArray modelArray, JSONObject responseJsonObject)
      throws Exception {
    boolean flag = false;
    for (int i = 0; i < modelArray.length(); i++) {
      JSONObject modelObj = (JSONObject) modelArray.get(i);
      // System.out.println("ModelObj->" + modelObj);
      String id = modelObj.get("cl_model_id").toString();

      CLModel model = OBDal.getInstance().get(CLModel.class, id);
      if (model == null)
        flag = insertModel(modelObj);
      else
        flag = updateModel(model, modelObj);
    }

    return flag;
  }

  private boolean updateModel(CLModel model, JSONObject modelObj) throws JSONException {

    model.setMerchandiseCategory(modelObj.get("merchandise_category").toString());
    OBDal.getInstance().save(model);

    return true;
  }

  private boolean insertModel(JSONObject modelObj) throws JSONException, ParseException {
    String insertQuery = "INSERT INTO cl_model(cl_model_id, ad_client_id, ad_org_id, isactive, created, createdby,"
        + " updated, updatedby, value, name, cl_subdepartment_id, cl_department_id, "
        + " cl_sport_id, merchandise_category, cl_brand_id, typology, cl_natureofproduct_id, cl_component_brand_id, "
        + " blueproduct, cl_storedept_id, cl_universe_id, cl_branddepartment_id, imancode) "
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(insertQuery);
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    query.setString(0, modelObj.get("cl_model_id").toString());
    query.setString(1, modelObj.get("ad_client_id").toString());
    query.setString(2, modelObj.get("ad_org_id").toString());
    query.setString(3, modelObj.get("isactive").toString());
    query.setDate(4, format.parse(modelObj.get("created").toString()));
    query.setString(5, modelObj.get("createdby").toString());
    query.setDate(6, format.parse(modelObj.get("updated").toString()));
    query.setString(7, modelObj.get("updatedby").toString());
    query.setString(8, modelObj.get("value").toString());
    query.setString(9, modelObj.get("name").toString());
    query.setString(10, modelObj.get("cl_subdepartment_id").toString());
    query.setString(11, modelObj.get("cl_department_id").toString());
    if ((modelObj.has("cl_sport_id")) && (!modelObj.get("cl_sport_id").toString().equals("")))
      query.setString(12, modelObj.get("cl_sport_id").toString());
    else
      query.setString(12, null);

    query.setString(13, modelObj.get("merchandise_category").toString());
    query.setString(14, modelObj.get("cl_brand_id").toString());
    query.setString(15, modelObj.get("typology").toString());
    query.setString(16, modelObj.get("cl_natureofproduct_id").toString());
    if ((modelObj.has("cl_component_brand_id"))
        && (!modelObj.get("cl_component_brand_id").toString().equals("")))
      query.setString(17, modelObj.get("cl_component_brand_id").toString());
    else
      query.setString(17, null);

    if (modelObj.has("blueproduct"))
      query.setString(18, modelObj.get("blueproduct").toString());
    else
      query.setString(18, "N");

    query.setString(19, modelObj.get("cl_storedept_id").toString());
    if ((modelObj.has("cl_universe_id")) && (!modelObj.get("cl_universe_id").toString().equals("")))
      query.setString(20, modelObj.get("cl_universe_id").toString());
    else
      query.setString(20, null);

    if ((modelObj.has("cl_branddepartment_id"))
        && (!modelObj.get("cl_branddepartment_id").toString().equals("")))
      query.setString(21, modelObj.get("cl_branddepartment_id").toString());
    else
      query.setString(21, null);

    query.setString(22, modelObj.get("imancode").toString());

    int result = query.executeUpdate();
    // System.out.println("Model Result" + result);
    if (result == 1)
      return true;
    else
      return false;
  }

  private boolean createPriceData(JSONArray priceArray) throws JSONException, ParseException {
    boolean flag = false;
    for (int i = 0; i < priceArray.length(); i++) {
      JSONObject priceObj = (JSONObject) priceArray.get(i);
      // System.out.println("ProductObj->" + prdObj);
      String id = priceObj.get("m_productprice_id").toString();

      ProductPrice pp = OBDal.getInstance().get(ProductPrice.class, id);
      if (pp == null)
        flag = insertPrice(priceObj);
      else
        flag = updatePrice(pp, priceObj);
    }
    return flag;
  }

  private boolean updatePrice(ProductPrice pp, JSONObject priceObj) throws JSONException {
    pp.setClMrpprice(new BigDecimal(priceObj.get("em_cl_mrpprice").toString()));
    pp.setClCessionprice(new BigDecimal(priceObj.get("em_cl_cessionprice").toString()));
    pp.setClCcunitprice(new BigDecimal(priceObj.get("em_cl_ccunitprice").toString()));
    pp.setClCcueprice(new BigDecimal(priceObj.get("em_cl_ccueprice").toString()));
    pp.setClCcpcbprice(new BigDecimal(priceObj.get("em_cl_ccpcbprice").toString()));
    pp.setClUnitmarginamount(new BigDecimal(priceObj.get("em_cl_unitmarginamount").toString()));
    pp.setClUnitmarginpercentage(new BigDecimal(priceObj.get("em_cl_unitmarginpercentage")
        .toString()));
    pp.setClUemarginamount(new BigDecimal(priceObj.get("em_cl_uemarginamount").toString()));
    pp.setClUemarginpercentage(new BigDecimal(priceObj.get("em_cl_uemarginpercentage").toString()));
    pp.setClPcbmarginamount(new BigDecimal(priceObj.get("em_cl_pcbmarginamount").toString()));
    pp.setClPcbmarginpercentage(new BigDecimal(priceObj.get("em_cl_pcbmarginpercentage").toString()));
    OBDal.getInstance().save(pp);

    return true;
  }

  private boolean insertPrice(JSONObject priceObj) throws JSONException, ParseException {
    String insertQuery = "INSERT INTO m_productprice(m_productprice_id, m_pricelist_version_id, "
        + "m_product_id, ad_client_id, "
        + "  ad_org_id, isactive, created, createdby, updated, updatedby, pricelist, "
        + "  pricestd, pricelimit, cost, algorithm, em_cl_fobprice, em_cl_mrpprice, "
        + "  em_cl_cessionprice, em_cl_ccunitprice, em_cl_ccueprice, em_cl_ccpcbprice, "
        + " em_cl_unitmarginamount, em_cl_unitmarginpercentage, em_cl_uemarginamount, "
        + " em_cl_uemarginpercentage, em_cl_pcbmarginamount, em_cl_pcbmarginpercentage, em_cl_followcatalog) "
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(insertQuery);
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    query.setString(0, priceObj.get("m_productprice_id").toString());
    query.setString(1, priceObj.get("m_pricelist_version_id").toString());
    query.setString(2, priceObj.get("m_product_id").toString());
    query.setString(3, priceObj.get("ad_client_id").toString());
    query.setString(4, priceObj.get("ad_org_id").toString());
    query.setString(5, priceObj.get("isactive").toString());
    query.setDate(6, format.parse(priceObj.get("created").toString()));
    query.setString(7, priceObj.get("createdby").toString());
    query.setDate(8, format.parse(priceObj.get("updated").toString()));
    query.setString(9, priceObj.get("updatedby").toString());
    query.setDouble(10, Double.parseDouble(priceObj.get("pricelist").toString()));
    query.setDouble(11, Double.parseDouble(priceObj.get("pricestd").toString()));
    query.setDouble(12, Double.parseDouble(priceObj.get("pricelimit").toString()));
    query.setDouble(13, Double.parseDouble(priceObj.get("cost").toString()));
    query.setString(14, priceObj.get("algorithm").toString());
    query.setDouble(15, Double.parseDouble(priceObj.get("em_cl_fobprice").toString()));
    query.setDouble(16, Double.parseDouble(priceObj.get("em_cl_mrpprice").toString()));
    query.setDouble(17, Double.parseDouble(priceObj.get("em_cl_cessionprice").toString()));
    query.setDouble(18, Double.parseDouble(priceObj.get("em_cl_ccunitprice").toString()));
    query.setDouble(19, Double.parseDouble(priceObj.get("em_cl_ccueprice").toString()));
    query.setDouble(20, Double.parseDouble(priceObj.get("em_cl_ccpcbprice").toString()));
    query.setDouble(21, Double.parseDouble(priceObj.get("em_cl_unitmarginamount").toString()));
    query.setDouble(22, Double.parseDouble(priceObj.get("em_cl_unitmarginpercentage").toString()));
    query.setDouble(23, Double.parseDouble(priceObj.get("em_cl_uemarginamount").toString()));
    query.setDouble(24, Double.parseDouble(priceObj.get("em_cl_uemarginpercentage").toString()));
    query.setDouble(25, Double.parseDouble(priceObj.get("em_cl_pcbmarginamount").toString()));
    query.setDouble(26, Double.parseDouble(priceObj.get("em_cl_pcbmarginpercentage").toString()));
    query.setString(27, "Y");

    int result = query.executeUpdate();
    if (result == 1)
      return true;
    else
      return false;
    // System.out.println("Price Result" + result);
  }

  private boolean createProductData(JSONArray productArray) throws JSONException, ParseException {
    boolean flag = false;
    for (int i = 0; i < productArray.length(); i++) {
      JSONObject prdObj = (JSONObject) productArray.get(i);
      // System.out.println("ProductObj->" + prdObj);
      String id = prdObj.get("m_product_id").toString();

      Product p = OBDal.getInstance().get(Product.class, id);
      if (p == null)
        flag = insertProduct(prdObj);
      else
        flag = updatePrd(p, prdObj);
    }

    return flag;
  }

  private boolean updatePrd(Product p, JSONObject prdObj) throws JSONException {
    TaxCategory tax = OBDal.getInstance().get(TaxCategory.class,
        prdObj.get("c_taxcategory_id").toString());
    if (tax != null)
      p.setTaxCategory(tax);
    p.setClLifestage(prdObj.get("em_cl_lifestage").toString());
    p.setClPcbQty(new BigDecimal(prdObj.get("em_cl_pcb_qty").toString()));
    p.setClUeQty(new BigDecimal(prdObj.get("em_cl_ue_qty").toString()));
    OBDal.getInstance().save(p);
    return true;
  }

  private boolean insertProduct(JSONObject prdObj) throws JSONException, ParseException {
    String insertQuery = "INSERT INTO m_product(m_product_id, ad_client_id, ad_org_id, isactive, created, createdby,"
        + "updated, updatedby, value, name, "
        + " upc, c_uom_id, salesrep_id, issummary, isstocked, ispurchased, m_product_category_id, volume, weight, "
        + " c_taxcategory_id, "
        + " producttype, m_attributeset_id, em_cl_log_rec, em_cl_modelname, em_cl_modelcode, em_cl_size, em_cl_pcb_qty, "
        + " em_cl_ue_qty, em_cl_grosswt_pcb, em_cl_volume_pcb, em_cl_color_id, em_cl_model_id, em_cl_age, "
        + " em_cl_gender, em_cl_lifestage, em_cl_typea, em_cl_typeb, em_cl_typec, em_cl_ismii) "
        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        + " ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(insertQuery);
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    query.setString(0, prdObj.get("m_product_id").toString());
    query.setString(1, prdObj.get("ad_client_id").toString());
    query.setString(2, prdObj.get("ad_org_id").toString());
    query.setString(3, prdObj.get("isactive").toString());
    query.setDate(4, format.parse(prdObj.get("created").toString()));
    query.setString(5, prdObj.get("createdby").toString());
    query.setDate(6, format.parse(prdObj.get("updated").toString()));
    query.setString(7, prdObj.get("updatedby").toString());
    query.setString(8, prdObj.get("value").toString());
    query.setString(9, prdObj.get("name").toString());
    query.setString(10, prdObj.get("upc").toString());
    query.setString(11, prdObj.get("c_uom_id").toString());
    if ((prdObj.has("salesrep_id")) && (!prdObj.get("salesrep_id").toString().equals("")))
      query.setString(12, prdObj.get("salesrep_id").toString());
    else
      query.setString(12, null);

    query.setString(13, prdObj.get("issummary").toString());
    query.setString(14, prdObj.get("isstocked").toString());
    query.setString(15, prdObj.get("ispurchased").toString());
    query.setString(16, prdObj.get("m_product_category_id").toString());
    query.setInteger(17, Integer.parseInt(prdObj.get("volume").toString()));
    query.setInteger(18, Integer.parseInt(prdObj.get("weight").toString()));
    query.setString(19, prdObj.get("c_taxcategory_id").toString());
    query.setString(20, prdObj.get("producttype").toString());
    query.setString(21, prdObj.get("m_attributeset_id").toString());
    if (prdObj.has("em_cl_log_rec"))
      query.setString(22, prdObj.get("em_cl_log_rec").toString());
    else
      query.setString(22, "");

    query.setString(23, prdObj.get("em_cl_modelname").toString());
    query.setString(24, prdObj.get("em_cl_modelcode").toString());
    query.setString(25, prdObj.get("em_cl_size").toString());
    query.setInteger(26, Integer.parseInt(prdObj.get("em_cl_pcb_qty").toString()));
    query.setInteger(27, Integer.parseInt(prdObj.get("em_cl_ue_qty").toString()));
    query.setInteger(28, Integer.parseInt(prdObj.get("em_cl_grosswt_pcb").toString()));
    query.setInteger(29, Integer.parseInt(prdObj.get("em_cl_volume_pcb").toString()));
    query.setString(30, prdObj.get("em_cl_color_id").toString());
    query.setString(31, prdObj.get("em_cl_model_id").toString());
    query.setString(32, prdObj.get("em_cl_age").toString());
    query.setString(33, prdObj.get("em_cl_gender").toString());
    query.setString(34, prdObj.get("em_cl_lifestage").toString());
    query.setString(35, prdObj.get("em_cl_typea").toString());
    query.setString(36, prdObj.get("em_cl_typeb").toString());
    query.setString(37, prdObj.get("em_cl_typec").toString());
    query.setString(38, prdObj.get("em_cl_ismii").toString());

    int result = query.executeUpdate();
    // System.out.println("Product Result" + result);
    if (result == 1)
      return true;
    else
      return false;
  }

  protected HttpURLConnection createConnection() throws Exception {

    String host = "";
    String port = "";
    URL url = null;
    String context = "";
    String updated = "";

    OBContext.setAdminMode();
    final Map<String, String> masterDBConfig = new HashMap<String, String>();
    OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(
        DSIDEFModuleConfig.class);
    configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME,
        "in.decathlon.ibud.masters"));
    if (configInfoObCriteria.count() > 0) {
      for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
        masterDBConfig.put(config.getKey(), config.getSearchKey());
      }
      host = masterDBConfig.get("host");
      port = masterDBConfig.get("port");
      context = masterDBConfig.get("context");
      updated = masterDBConfig.get("updatedDate");
      final String userName = masterDBConfig.get("userName");
      final String pwd = masterDBConfig.get("pwd");

      if ((updated != null || updated != "") || (host != null || host != "")
          || (port != null || port != "") || (context != null || context != "")) {
        url = new URL("http://" + host + ":" + port + "/" + context + "?updated=" + updated);
      } else {
        log.error("Master ERP Configuration for Supply Connection is not set");
        return null;
      }

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, pwd.toCharArray());
        }
      });
    }
    OBContext.restorePreviousMode();

    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    hc.setRequestMethod("GET");
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    return hc;
  }

  private Object getOtherMasters(String content, String master) throws JSONException {
    Check.isNotNull(content, "Content must be set");
    Object jsonMasterList = null;
    JSONObject jsonObj = new JSONObject(content);
    jsonMasterList = jsonObj.get(master);
    return jsonMasterList;
  }

}
