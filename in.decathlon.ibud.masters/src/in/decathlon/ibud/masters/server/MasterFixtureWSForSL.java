package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.commons.JSONHelper;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;

public class MasterFixtureWSForSL implements WebService {
  private static final Logger log = Logger.getLogger(MasterFixtureWSForSL.class);
  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    JSONObject jsonObj = processRequest(path, request, response);
    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonObj.toString());
    w.close();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  private JSONObject processRequest(String path, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    JSONObject jsonDataObject = new JSONObject();

    try {
      String updated = request.getParameter("updated");
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
      Date updatedDate = format.parse(updated);

      log.info("Inside Pull Master Webservice fetching data from " + updatedDate);

      List<Object[]> subDeptList = getNewEntityData(updatedDate, "cl_subdepartment");
      log.info("subDeptList->" + subDeptList.size());
      jsonDataObject.put("cl_subdepartment",// generateJsonWS(CLSubdepartment.class, updated, ""));
          getNewEntityJsonFromList(subDeptList, "cl_subdepartment"));

      List<Object[]> deptList = getNewEntityData(updatedDate, "cl_department");
      log.info("deptList->" + deptList.size());
      jsonDataObject.put("cl_department", getNewEntityJsonFromList(deptList, "cl_department"));

      List<Object[]> sportList = getNewEntityData(updatedDate, "cl_sport");
      log.info("sportList->" + sportList.size());
      jsonDataObject.put("cl_sport", getNewEntityJsonFromList(sportList, "cl_sport"));

      List<Object[]> brandList = getNewEntityData(updatedDate, "cl_brand");
      log.info("brandList->" + brandList.size());
      jsonDataObject.put("cl_brand", getNewEntityJsonFromList(brandList, "cl_brand"));

      List<Object[]> nopList = getNewEntityData(updatedDate, "cl_natureofproduct");
      log.info("nopList->" + nopList.size());
      jsonDataObject.put("cl_natureofproduct",
          getNewEntityJsonFromList(nopList, "cl_natureofproduct"));

      List<Object[]> componentBrandList = getNewEntityData(updatedDate, "cl_component_brand");
      log.info("componentBrandList->" + componentBrandList.size());
      jsonDataObject.put("cl_component_brand",
          getNewEntityJsonFromList(componentBrandList, "cl_component_brand"));

      List<Object[]> storeDeptList = getNewEntityData(updatedDate, "cl_storedept");
      log.info("storeDeptList->" + storeDeptList.size());
      jsonDataObject.put("cl_storedept", getNewEntityJsonFromList(storeDeptList, "cl_storedept"));

      List<Object[]> brandDeptList = getNewEntityData(updatedDate, "cl_branddepartment");
      log.info("brandDeptList->" + brandDeptList.size());
      jsonDataObject.put("cl_branddepartment",
          getNewEntityJsonFromList(brandDeptList, "cl_branddepartment"));

      List<Object[]> universeList = getNewEntityData(updatedDate, "cl_universe");
      log.info("universeList->" + universeList.size());
      jsonDataObject.put("cl_universe", getNewEntityJsonFromList(universeList, "cl_universe"));

      List<Object[]> modelList = getModelData(updatedDate);
      log.info("modelList->" + modelList.size());
      jsonDataObject.put("model", getModelJsonFromList(modelList));

      /*
       * List<Object[]> taxcategoryList = getNewEntityData(updatedDate, "c_taxcategory");
       * log.info("taxcategoryList->" + taxcategoryList.size()); jsonDataObject.put("c_taxcategory",
       * getNewEntityJsonFromList(taxcategoryList, "c_taxcategory"));
       * 
       * List<Object[]> GSTProductCodeList = getGstProductCodeData(updatedDate);
       * log.info("GSTProductCodeList->" + GSTProductCodeList.size());
       * jsonDataObject.put("ingst_gstproductcode",
       * getGSTProdctCodeJsonFromList(GSTProductCodeList));
       */
      List<Object[]> productCategoryList = getProductCategoryData(updatedDate);
      log.info("productCategoryList->" + productCategoryList.size());
      jsonDataObject.put("m_product_category", getProdctCategoryJsonFromList(productCategoryList));

      List<Object[]> clColorList = getNewEntityData(updatedDate, "cl_color");
      log.info("CLColorList->" + clColorList.size());
      jsonDataObject.put("cl_color", getNewEntityJsonFromList(clColorList, "cl_color"));

      List<Object[]> prdList = getProductData(updatedDate);
      log.info("prdList->" + prdList.size());
      jsonDataObject.put("product", getProductJsonFromList(prdList));

      List<Object[]> priceList = getPricelistData(updatedDate);
      log.info("priceList->" + priceList.size());
      jsonDataObject.put("m_pricelist", getPriceListJsonFromList(priceList));

      List<Object[]> discountSchemaList = getDiscountSchemaData(updatedDate);
      log.info("discountSchemaList->" + discountSchemaList.size());
      jsonDataObject.put("m_discountschema", getDiscountSchemaJsonFromList(discountSchemaList));

      List<Object[]> pricelistVersionList = getPricelistVersionData(updatedDate);
      log.info("pricelistVersionList->" + pricelistVersionList.size());
      jsonDataObject.put("m_pricelist_version",
          getPriceListVersionJsonFromList(pricelistVersionList));

      List<Object[]> productPriceList = getProductPriceData(updatedDate);
      log.info("productpriceList->" + productPriceList.size());
      jsonDataObject.put("price", getPriceJsonFromList(productPriceList));

      jsonDataObject.put("status", "success");
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Calling Pull Master Process and Error is: " + e);
    }
    return jsonDataObject;
  }

  private JSONArray getModelJsonFromList(List<Object[]> modelList) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] m : modelList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("cl_model_id", m[0]);
        jsonObject.put("ad_client_id", m[1]);
        jsonObject.put("ad_org_id", m[2]);
        jsonObject.put("isactive", m[3]);
        jsonObject.put("created", m[4]);
        jsonObject.put("createdby", m[5]);
        jsonObject.put("updated", df.format(new Date()));
        jsonObject.put("updatedby", m[7]);
        jsonObject.put("value", m[8]);
        jsonObject.put("name", m[9]);
        if (m[10] != null)
          jsonObject.put("cl_subdepartment_id", m[10]);
        else
          jsonObject.put("cl_subdepartment_id", "");
        if (m[11] != null)
          jsonObject.put("cl_department_id", m[11]);
        else
          jsonObject.put("cl_department_id", "");
        if (m[12] != null)
          jsonObject.put("cl_sport_id", m[12]);
        else
          jsonObject.put("cl_sport_id", "");
        if (m[13] != null)
          jsonObject.put("merchandise_category", m[13]);
        else
          jsonObject.put("merchandise_category", "");
        if (m[14] != null)
          jsonObject.put("cl_brand_id", m[14]);
        else
          jsonObject.put("cl_brand_id", "");
        if (m[15] != null)
          jsonObject.put("typology", m[15]);
        else
          jsonObject.put("typology", "");
        if (m[16] != null)
          jsonObject.put("cl_natureofproduct_id", m[16]);
        else
          jsonObject.put("cl_natureofproduct_id", "");
        if (m[17] != null)
          jsonObject.put("cl_component_brand_id", m[17]);
        else
          jsonObject.put("cl_component_brand_id", "");
        if (m[18] != null)
          jsonObject.put("blueproduct", m[18]);
        else
          jsonObject.put("blueproduct", "");
        if (m[19] != null)
          jsonObject.put("cl_storedept_id", m[19]);
        else
          jsonObject.put("cl_storedept_id", "");
        if (m[20] != null)
          jsonObject.put("cl_universe_id", m[20]);
        else
          jsonObject.put("cl_universe_id", "");
        if (m[21] != null)
          jsonObject.put("cl_branddepartment_id", m[21]);
        else
          jsonObject.put("cl_branddepartment_id", "");
        if (m[22] != null)
          jsonObject.put("imancode", m[22]);
        else
          jsonObject.put("imancode", "0");

        jsonArray.put(jsonObject);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for Model data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private JSONArray getProductJsonFromList(List<Object[]> prdList) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] m : prdList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("m_product_id", m[0]);
        jsonObject.put("ad_client_id", m[1]);
        jsonObject.put("ad_org_id", m[2]);
        jsonObject.put("isactive", m[3]);
        jsonObject.put("created", m[4]);
        jsonObject.put("createdby", m[5]);
        jsonObject.put("updated", df.format(new Date()));
        jsonObject.put("updatedby", m[7]);
        jsonObject.put("value", m[8]);
        jsonObject.put("name", m[9]);
        if (m[10] != null)
          jsonObject.put("upc", m[10]);
        else
          jsonObject.put("upc", "");
        if (m[11] != null)
          jsonObject.put("c_uom_id", m[11]);
        else
          jsonObject.put("c_uom_id", "");
        if (m[12] != null)
          jsonObject.put("salesrep_id", m[12]);
        else
          jsonObject.put("salesrep_id", "");
        if (m[13] != null)
          jsonObject.put("issummary", m[13]);
        else
          jsonObject.put("issummary", "N");
        if (m[14] != null)
          jsonObject.put("isstocked", m[14]);
        else
          jsonObject.put("isstocked", "N");

        if (m[15] != null)
          jsonObject.put("ispurchased", m[15]);
        else
          jsonObject.put("ispurchased", "N");
        if (m[16] != null)
          jsonObject.put("m_product_category_id", m[16]);
        else
          jsonObject.put("m_product_category_id", "");
        if (m[16] != null)

          jsonObject.put("volume", m[17]);
        if (m[16] != null)

          jsonObject.put("weight", m[18]);
        jsonObject.put("c_taxcategory_id", m[19]);
        jsonObject.put("producttype", m[20]);
        jsonObject.put("m_attributeset_id", m[21]);
        if (m[22] != null)
          jsonObject.put("em_cl_log_rec", m[22]);
        else
          jsonObject.put("em_cl_log_rec", "Non Standard 54");
        jsonObject.put("em_cl_modelname", m[23]);
        jsonObject.put("em_cl_modelcode", m[24]);
        if (m[25] != null)
          jsonObject.put("em_cl_size", m[25]);
        else
          jsonObject.put("em_cl_size", "");

        if (m[26] != null)
          jsonObject.put("em_cl_pcb_qty", m[26]);
        else
          jsonObject.put("em_cl_pcb_qty", "1");
        if (m[27] != null)
          jsonObject.put("em_cl_ue_qty", m[27]);
        else
          jsonObject.put("em_cl_ue_qty", "1");
        if (m[28] != null)
          jsonObject.put("em_cl_grosswt_pcb", m[28]);
        else
          jsonObject.put("em_cl_grosswt_pcb", "0");
        if (m[29] != null)
          jsonObject.put("em_cl_volume_pcb", m[29]);
        else
          jsonObject.put("em_cl_volume_pcb", "0");
        if (m[30] != null)
          jsonObject.put("em_cl_color_id", m[30]);
        else
          jsonObject.put("em_cl_color_id", "");

        if (m[31] != null)
          jsonObject.put("em_cl_model_id", m[31]);
        else
          jsonObject.put("em_cl_model_id", "");

        if (m[32] != null)
          jsonObject.put("em_cl_age", m[32]);
        else
          jsonObject.put("em_cl_age", "");

        if (m[33] != null)
          jsonObject.put("em_cl_gender", m[33]);
        else
          jsonObject.put("em_cl_gender", "");

        if (m[34] != null)
          jsonObject.put("em_cl_lifestage", m[34]);
        else
          jsonObject.put("em_cl_lifestage", "New");

        if (m[35] != null)
          jsonObject.put("em_cl_typea", m[35]);
        else
          jsonObject.put("em_cl_typea", "N");

        if (m[36] != null)
          jsonObject.put("em_cl_typeb", m[36]);
        else
          jsonObject.put("em_cl_typeb", "N");

        if (m[37] != null)
          jsonObject.put("em_cl_typec", m[37]);
        else
          jsonObject.put("em_cl_typec", "N");

        if (m[38] != null)
          jsonObject.put("em_cl_ismii", m[38]);
        else
          jsonObject.put("em_cl_ismii", "");

        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for Product data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private JSONArray getPriceJsonFromList(List<Object[]> priceList) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] m : priceList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("m_productprice_id", m[0]);
        jsonObject.put("m_pricelist_version_id", m[1]);
        jsonObject.put("m_product_id", m[2]);
        jsonObject.put("ad_client_id", m[3]);
        jsonObject.put("ad_org_id", m[4]);
        jsonObject.put("isactive", m[5]);
        jsonObject.put("created", m[6]);
        jsonObject.put("createdby", m[7]);
        jsonObject.put("updated", df.format(new Date()));
        jsonObject.put("updatedby", m[9]);
        jsonObject.put("pricelist", m[10]);
        jsonObject.put("pricestd", m[11]);
        jsonObject.put("pricelimit", m[12]);
        jsonObject.put("cost", m[13]);
        if (m[14] != null)
          jsonObject.put("algorithm", m[14]);
        else
          jsonObject.put("algorithm", "");

        jsonObject.put("em_cl_fobprice", m[15]);
        jsonObject.put("em_cl_mrpprice", m[16]);
        jsonObject.put("em_cl_cessionprice", m[17]);
        jsonObject.put("em_cl_ccunitprice", m[18]);
        jsonObject.put("em_cl_ccueprice", m[19]);
        jsonObject.put("em_cl_ccpcbprice", m[20]);
        jsonObject.put("em_cl_unitmarginamount", m[21]);
        jsonObject.put("em_cl_unitmarginpercentage", m[22]);
        jsonObject.put("em_cl_uemarginamount", m[23]);
        jsonObject.put("em_cl_uemarginpercentage", m[24]);
        jsonObject.put("em_cl_pcbmarginamount", m[25]);
        jsonObject.put("em_cl_pcbmarginpercentage", m[26]);
        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for Product Price list data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private List<Object[]> getProductPriceData(Date updatedTime) {
    List<Object[]> priceData = new ArrayList<Object[]>();
    try {
      String priceSQLQuery = "SELECT DISTINCT pp.m_productprice_id, pp.m_pricelist_version_id, pp.m_product_id, pp.ad_client_id, "
          + " pp.ad_org_id, pp.isactive, pp.created, pp.createdby, pp.updated, pp.updatedby, "
          + "pp.pricelist, pp.pricestd, pp.pricelimit, pp.cost, pp.algorithm, pp.em_cl_fobprice, "
          + "pp.em_cl_mrpprice, pp.em_cl_cessionprice, pp.em_cl_ccunitprice, pp.em_cl_ccueprice, "
          + "pp.em_cl_ccpcbprice, pp.em_cl_unitmarginamount, round(pp.em_cl_unitmarginpercentage,2) as em_cl_unitmarginpercentage, "
          + " pp.em_cl_uemarginamount, round(pp.em_cl_uemarginpercentage,2) as em_cl_uemarginpercentage, pp.em_cl_pcbmarginamount, "
          + " round(pp.em_cl_pcbmarginpercentage,2) as em_cl_pcbmarginpercentage "
          + "FROM m_productprice pp   join m_product p on p.m_product_id=pp.m_product_id "
          + "join cl_model ml on p.em_cl_model_id=ml.cl_model_id   join cl_brand b on b.cl_brand_id=ml.cl_brand_id "
          + "where b.name in ('FIXTURES','Events','UNKNOWN') "
          + "and pp.m_pricelist_version_id in (SELECT DISTINCT m_pricelist_version_id from m_pricelist_version where name = 'DMI CATALOGUE') and pp.updated >= ? ";
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(priceSQLQuery);
      query.setDate(0, updatedTime);
      priceData = query.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Price Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return priceData;
  }

  private List<Object[]> getModelData(Date updatedTime) {
    List<Object[]> modelData = new ArrayList<Object[]>();
    try {
      String query = "SELECT DISTINCT ml.cl_model_id, ml.ad_client_id, ml.ad_org_id, ml.isactive, ml.created, ml.createdby,"
          + " ml.updated, ml.updatedby, ml.value, ml.name, ml.cl_subdepartment_id, ml.cl_department_id,"
          + " ml.cl_sport_id, ml.merchandise_category, ml.cl_brand_id, "
          + " ml.typology, ml.cl_natureofproduct_id, ml.cl_component_brand_id, "
          + " ml.blueproduct, ml.cl_storedept_id, ml.cl_universe_id,  ml.cl_branddepartment_id, imancode "
          + " FROM cl_model ml join m_product p on p.em_cl_model_id=ml.cl_model_id"
          + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id"
          + " where b.name in ('FIXTURES','Events','UNKNOWN') and ml.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      modelData = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Model Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return modelData;
  }

  public List<Object[]> getProductData(Date updatedTime) throws Exception {
    List<Object[]> prdData = new ArrayList<Object[]>();
    try {
      String productSQLQuery = "SELECT DISTINCT p.m_product_id, p.ad_client_id, p.ad_org_id, p.isactive, p.created, p.createdby, p.updated, p.updatedby, p.value, p.name, "
          + " p.upc, p.c_uom_id, p.salesrep_id, p.issummary, p.isstocked, p.ispurchased, p.m_product_category_id, p.volume, p.weight, "
          + " p.c_taxcategory_id, p.producttype, p.m_attributeset_id, p.em_cl_log_rec, p.em_cl_modelname, p.em_cl_modelcode, p.em_cl_size, p.em_cl_pcb_qty, p.em_cl_ue_qty, p.em_cl_grosswt_pcb,"
          + " p.em_cl_volume_pcb, p.em_cl_color_id, p.em_cl_model_id, p.em_cl_age, p.em_cl_gender, p.em_cl_lifestage, p.em_cl_typea, p.em_cl_typeb,"
          + " p.em_cl_typec, p.em_cl_ismii "
          + " FROM m_product p join cl_model ml on p.em_cl_model_id=ml.cl_model_id"
          + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id where b.name in ('FIXTURES','Events','UNKNOWN') and ml.updated >= ?";
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(productSQLQuery);
      query.setDate(0, updatedTime);
      prdData = query.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Product Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return prdData;

  }

  private List<Object[]> getProductCategoryData(Date updatedTime) {
    List<Object[]> EntityDataList = new ArrayList<Object[]>();
    try {
      String query = " SELECT DISTINCT e.m_product_category_id ,  e.ad_client_id ,  e.ad_org_id  , e. isactive  ,  e.created ,  e.createdby  ,  "
          + " e.updated  ,  e.updatedby  ,  e.value  ,  e.name  ,  e.description ,  "
          + "e.isdefault  ,e.plannedmargin , e.a_asset_group_id  , e. ad_image_id  , e. issummary  , e. em_ingst_gstproductcode_id    "
          + "FROM m_product_category e   "
          + "join m_product p on p.m_product_category_id=e.m_product_category_id  "
          + " join cl_model ml on p.em_cl_model_id=ml.cl_model_id  "
          + "join cl_brand b on b.cl_brand_id=ml.cl_brand_id   "
          + " where b.name in ('FIXTURES','Events','UNKNOWN') and ml.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      EntityDataList = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Product Category Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return EntityDataList;

  }

  private List<Object[]> getDiscountSchemaData(Date updatedTime) {
    List<Object[]> EntityDataList = new ArrayList<Object[]>();
    try {
      String query = "  SELECT DISTINCT e.m_discountschema_id, e.ad_client_id, e.ad_org_id, e.isactive, e.created, "
          + "   e.createdby, e.updated, e.updatedby, e.name, e.description, e.validfrom,  "
          + " e.discounttype, e.script, e.flatdiscount, e.isquantitybased, e.cumulativelevel,  "
          + " e.processing  "
          + "   FROM m_discountschema e  "
          + "  join m_pricelist_version mpv on   mpv.m_discountschema_id=e.m_discountschema_id "
          + "  join  m_productprice pp  on mpv.m_pricelist_version_id=pp.m_pricelist_version_id "
          + "   join m_product p on p.m_product_id=pp.m_product_id "
          + "    join cl_model ml on p.em_cl_model_id=ml.cl_model_id "
          + "  join cl_brand b on b.cl_brand_id=ml.cl_brand_id "
          + " where b.name in ('FIXTURES','Events','UNKNOWN') and pp.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      EntityDataList = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Discount Schema Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return EntityDataList;

  }

  private List<Object[]> getPricelistVersionData(Date updatedTime) {
    List<Object[]> EntityDataList = new ArrayList<Object[]>();
    try {
      String query = "   SELECT DISTINCT e.m_pricelist_version_id, e.ad_client_id, e.ad_org_id, e.isactive, e.created,   "
          + "     e.createdby, e.updated, e.updatedby, e.name, e.description, e.m_pricelist_id,   "
          + " e.m_discountschema_id, e.validfrom, e.proccreate, e.m_pricelist_version_base_id,   "
          + " e.m_pricelist_version_generate  "
          + "   FROM   m_pricelist_version e  "
          + "   join  m_productprice pp  on e.m_pricelist_version_id=pp.m_pricelist_version_id  "
          + "  join m_product p on p.m_product_id=pp.m_product_id  "
          + " join cl_model ml on p.em_cl_model_id=ml.cl_model_id  "
          + "   join cl_brand b on b.cl_brand_id=ml.cl_brand_id  "
          + " where b.name in ('FIXTURES','Events','UNKNOWN') and pp.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      EntityDataList = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Price List Version Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return EntityDataList;

  }

  private List<Object[]> getGstProductCodeData(Date updatedTime) {
    List<Object[]> EntityDataList = new ArrayList<Object[]>();
    try {
      String query = " SELECT DISTINCT e.ingst_gstproductcode_id, e.ad_client_id, e.ad_org_id, e.isactive, e.created,  "
          + "       e.createdby, e.updated, e.updatedby, e.value, e.name, e.type, e.description,  "
          + "       e.c_taxcategory_id, e.display, e.isexception   "
          + "    FROM ingst_gstproductcode e    "
          + "     join m_product p on p.em_ingst_gstproductcode_id=e.ingst_gstproductcode_id "
          + " join cl_model ml on p.em_cl_model_id=ml.cl_model_id "
          + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id "
          + " where b.name in ('FIXTURES','Events','UNKNOWN') and e.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      EntityDataList = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the GST Product Code Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return EntityDataList;

  }

  private List<Object[]> getNewEntityData(Date updatedTime, String Key) {
    List<Object[]> listDataObj = new ArrayList<Object[]>();
    try {
      String keyId = Key + "_id";
      String query = "SELECT DISTINCT  e." + keyId
          + ", e.ad_client_id, e.ad_org_id, e.isactive, e.created, e.createdby,  "
          + "     e.updated, e.updatedby, e.name, e.description ";
      if (!(Key.equalsIgnoreCase("cl_storedept") || Key.equalsIgnoreCase("cl_universe"))) {
        query = query + ", e.isdefault  ";
      }
      if (Key.equalsIgnoreCase("c_taxcategory")) {
        query = query + " ,  e.em_ingst_gstproductcode_id, e.asbom  ";
      }

      query = query + "   FROM " + Key + " e     ";

      if (Key.equalsIgnoreCase("c_taxcategory")) {
        query = query
            + "  join m_product p on p.c_taxcategory_id=e.c_taxcategory_id join cl_model ml on p.em_cl_model_id=ml.cl_model_id ";

      } else if (Key.equalsIgnoreCase("cl_color")) {

        query = query
            + " join m_product p on p.em_cl_color_id=e.cl_color_id              join cl_model ml on p.em_cl_model_id=ml.cl_model_id ";

      } else {
        query = query + " join cl_model ml on e." + keyId + "=ml." + keyId + " "
            + " join m_product p on p.em_cl_model_id=ml.cl_model_id ";
      }
      query = query + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id "
          + " where b.name in ('FIXTURES','Events','UNKNOWN')  ";

      // query = query + " and e.updated >= ? ";
      query = query + " and ml.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      listDataObj = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the " + Key + " Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return listDataObj;

  }

  private JSONArray getNewEntityJsonFromList(List<Object[]> ListObj, String Entity)
      throws JSONException {
    JSONArray jsonArray = new JSONArray();
    String EntityId = Entity + "_id";
    try {
      for (Object[] Obj : ListObj) {
        JSONObject jsonObject = getbasicJson(Obj, EntityId);

        if (Obj[8] != null) {
          jsonObject.put("name", Obj[8]);
        } else {
          jsonObject.put("name", "");
        }
        if (Obj[9] != null) {
          jsonObject.put("description", Obj[9].toString());
        } else {
          jsonObject.put("description", "");
        }
        if (!(Entity.equalsIgnoreCase("cl_storedept") || Entity.equalsIgnoreCase("cl_universe"))) {
          if (Obj[10] != null) {
            jsonObject.put("isdefault", Obj[10].toString());
          } else {
            jsonObject.put("isdefault", "");
          }
        }

        if (Entity.equalsIgnoreCase("c_taxcategory")) {

          if (Obj[11] != null) {
            jsonObject.put("em_ingst_gstproductcode_id", Obj[11].toString());
          } else {
            jsonObject.put("em_ingst_gstproductcode_id", "");
          }
          if (Obj[12] != null) {
            jsonObject.put("asbom", Obj[12].toString());
          } else {
            jsonObject.put("asbom", "");
          }
        }
        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for " + Entity
          + " data using pull master process and Error is: " + e);
    }
    return jsonArray;
  }

  private JSONArray getGSTProdctCodeJsonFromList(List<Object[]> ListObj) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] Obj : ListObj) {

        JSONObject jsonObject = getbasicJson(Obj, "ingst_gstproductcode_id");

        if (Obj[8] != null) {
          jsonObject.put("value", Obj[8]);
        } else {
          jsonObject.put("value", "");
        }
        if (Obj[9] != null) {
          jsonObject.put("name", Obj[9].toString());
        } else {
          jsonObject.put("name", "");
        }

        if (Obj[10] != null) {
          jsonObject.put("type", Obj[10]);
        } else {
          jsonObject.put("type", "");
        }
        if (Obj[11] != null) {
          jsonObject.put("description", Obj[11].toString());
        } else {
          jsonObject.put("description", "");
        }
        if (Obj[12] != null) {
          jsonObject.put("c_taxcategory_id", Obj[12]);
        } else {
          jsonObject.put("c_taxcategory_id", "");
        }
        if (Obj[13] != null) {
          jsonObject.put("display", Obj[13].toString());
        } else {
          jsonObject.put("display", "");
        }
        if (Obj[14] != null) {
          jsonObject.put("isexception", Obj[14].toString());
        } else {
          jsonObject.put("isexception", "");
        }

        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for GST Product Code data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private JSONArray getProdctCategoryJsonFromList(List<Object[]> ListObj) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] Obj : ListObj) {
        JSONObject jsonObject = getbasicJson(Obj, "m_product_category_id");
        if (Obj[8] != null) {
          jsonObject.put("value", Obj[8]);
        } else {
          jsonObject.put("value", "");
        }
        if (Obj[9] != null) {
          jsonObject.put("name", Obj[9].toString());
        } else {
          jsonObject.put("name", "");
        }

        if (Obj[10] != null) {
          jsonObject.put("description", Obj[10]);
        } else {
          jsonObject.put("description", "");
        }
        if (Obj[11] != null) {
          jsonObject.put("isdefault", Obj[11].toString());
        } else {
          jsonObject.put("isdefault", "");
        }
        if (Obj[12] != null) {
          jsonObject.put("plannedmargin", Obj[12]);
        } else {
          jsonObject.put("plannedmargin", "");
        }
        if (Obj[13] != null) {
          jsonObject.put("a_asset_group_id", Obj[13].toString());
        } else {
          jsonObject.put("a_asset_group_id", "");
        }
        if (Obj[14] != null) {
          jsonObject.put("ad_image_id", Obj[14].toString());
        } else {
          jsonObject.put("ad_image_id", "");
        }
        if (Obj[15] != null) {
          jsonObject.put("issummary", Obj[15].toString());
        } else {
          jsonObject.put("issummary", "");
        }
        if (Obj[16] != null) {
          jsonObject.put("em_ingst_gstproductcode_id", Obj[16].toString());
        } else {
          jsonObject.put("em_ingst_gstproductcode_id", "");
        }
        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for Product Category data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  public JSONObject getbasicJson(Object[] Obj, String key) throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(key, Obj[0].toString());
    jsonObject.put("ad_client_id", Obj[1].toString());
    jsonObject.put("ad_org_id", Obj[2].toString());
    jsonObject.put("isactive", Obj[3].toString());
    jsonObject.put("created", Obj[4].toString());
    jsonObject.put("createdby", Obj[5].toString());
    jsonObject.put("updated", df.format(new Date()));
    jsonObject.put("updatedby", Obj[7].toString());
    return jsonObject;

  }

  private JSONArray getPriceListJsonFromList(List<Object[]> ListObj) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] Obj : ListObj) {

        JSONObject jsonObject = getbasicJson(Obj, "m_pricelist_id");

        if (Obj[8] != null) {
          jsonObject.put("name", Obj[8]);
        } else {
          jsonObject.put("name", "");
        }
        if (Obj[9] != null) {
          jsonObject.put("description", Obj[9].toString());
        } else {
          jsonObject.put("description", "");
        }

        if (Obj[10] != null) {
          jsonObject.put("basepricelist_id", Obj[10]);
        } else {
          jsonObject.put("basepricelist_id", "");
        }
        if (Obj[11] != null) {
          jsonObject.put("istaxincluded", Obj[11].toString());
        } else {
          jsonObject.put("istaxincluded", "");
        }
        if (Obj[12] != null) {
          jsonObject.put("issopricelist", Obj[12]);
        } else {
          jsonObject.put("issopricelist", "");
        }
        if (Obj[13] != null) {
          jsonObject.put("isdefault", Obj[13].toString());
        } else {
          jsonObject.put("isdefault", "");
        }
        if (Obj[14] != null) {
          jsonObject.put("c_currency_id", Obj[14].toString());
        } else {
          jsonObject.put("c_currency_id", "");
        }
        if (Obj[15] != null) {
          jsonObject.put("enforcepricelimit", Obj[15].toString());
        } else {
          jsonObject.put("enforcepricelimit", "");
        }
        if (Obj[16] != null) {
          jsonObject.put("costbased", Obj[16].toString());
        } else {
          jsonObject.put("costbased", "");
        }

        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for Price list data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private JSONArray getDiscountSchemaJsonFromList(List<Object[]> ListObj) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] Obj : ListObj) {

        JSONObject jsonObject = getbasicJson(Obj, "m_discountschema_id");

        if (Obj[8] != null) {
          jsonObject.put("name", Obj[8]);
        } else {
          jsonObject.put("name", "");
        }
        if (Obj[9] != null) {
          jsonObject.put("description", Obj[9].toString());
        } else {
          jsonObject.put("description", "");
        }
        if (Obj[10] != null) {
          jsonObject.put("validfrom", Obj[10]);
        } else {
          jsonObject.put("validfrom", "");
        }
        if (Obj[11] != null) {
          jsonObject.put("discounttype", Obj[11].toString());
        } else {
          jsonObject.put("discounttype", "");
        }
        if (Obj[12] != null) {
          jsonObject.put("script", Obj[12]);
        } else {
          jsonObject.put("script", "");
        }
        if (Obj[13] != null) {
          jsonObject.put("flatdiscount", Obj[13].toString());
        } else {
          jsonObject.put("flatdiscount", "");
        }
        if (Obj[14] != null) {
          jsonObject.put("isquantitybased", Obj[14].toString());
        } else {
          jsonObject.put("isquantitybased", "");
        }
        if (Obj[15] != null) {
          jsonObject.put("cumulativelevel", Obj[15].toString());
        } else {
          jsonObject.put("cumulativelevel", "");
        }
        if (Obj[16] != null) {
          jsonObject.put("processing", Obj[16].toString());
        } else {
          jsonObject.put("processing", "");
        }

        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for Discount Schema data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private JSONArray getPriceListVersionJsonFromList(List<Object[]> ListObj) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    try {
      for (Object[] Obj : ListObj) {

        JSONObject jsonObject = getbasicJson(Obj, "m_pricelist_version_id");

        if (Obj[8] != null) {
          jsonObject.put("name", Obj[8]);
        } else {
          jsonObject.put("name", "");
        }
        if (Obj[9] != null) {
          jsonObject.put("description", Obj[9].toString());
        } else {
          jsonObject.put("description", "");
        }
        if (Obj[10] != null) {
          jsonObject.put("m_pricelist_id", Obj[10]);
        } else {
          jsonObject.put("m_pricelist_id", "");
        }
        if (Obj[11] != null) {
          jsonObject.put("m_discountschema_id", Obj[11].toString());
        } else {
          jsonObject.put("m_discountschema_id", "");
        }
        if (Obj[12] != null) {
          jsonObject.put("validfrom", Obj[12]);
        } else {
          jsonObject.put("validfrom", "");
        }
        if (Obj[13] != null) {
          jsonObject.put("proccreate", Obj[13].toString());
        } else {
          jsonObject.put("proccreate", "");
        }
        if (Obj[14] != null) {
          jsonObject.put("m_pricelist_version_base_id", Obj[14].toString());
        } else {
          jsonObject.put("m_pricelist_version_base_id", "");
        }
        if (Obj[15] != null) {
          jsonObject.put("m_pricelist_version_generate", Obj[15].toString());
        } else {
          jsonObject.put("m_pricelist_version_generate", "");
        }

        jsonArray.put(jsonObject);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Creating Json for price list version data using pull master process and Error is: "
          + e);
    }
    return jsonArray;
  }

  private List<Object[]> getPricelistData(Date updatedTime) {
    List<Object[]> EntityDataList = new ArrayList<Object[]>();
    try {
      String query = "   SELECT DISTINCT e.m_pricelist_id, e.ad_client_id, e.ad_org_id, e.isactive, e.created, e.createdby,  "
          + "     e.updated, e.updatedby, e.name, e.description, e.basepricelist_id, e.istaxincluded,  "
          + "       e.issopricelist, e.isdefault, e.c_currency_id, e.enforcepricelimit, e.costbased   "
          + "   FROM m_pricelist e  "
          + "   join m_pricelist_version mpv on   mpv.m_pricelist_id=e.m_pricelist_id "
          + " join  m_productprice pp  on mpv.m_pricelist_version_id=pp.m_pricelist_version_id "
          + " join m_product p on p.m_product_id=pp.m_product_id "
          + " join cl_model ml on p.em_cl_model_id=ml.cl_model_id "
          + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id  "
          + " where b.name in ('FIXTURES','Events','UNKNOWN') and pp.updated >= ? ";

      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setDate(0, updatedTime);
      EntityDataList = sqlQuery.list();
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error whlie Getting the Price list Data for pull master process with date: "
          + updatedTime + " and Error is: " + e);
    }
    return EntityDataList;

  }

  public JSONArray generateJsonWS(Class<? extends BaseOBObject> bob, String updatedTime,
      String master) throws Exception {
    JSONArray jsonArray = new JSONArray();

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    OBCriteria<? extends BaseOBObject> genClassCriteria = OBDal.getInstance().createCriteria(bob);
    if (!master.equals("DocSequence") && !master.equals("User")) {
      genClassCriteria.add(Restrictions.ge(Product.PROPERTY_UPDATED, newDate));
    } else {
      genClassCriteria.add(Restrictions.ge(Sequence.PROPERTY_CREATIONDATE, newDate));
    }
    genClassCriteria.setFilterOnActive(false);
    genClassCriteria.setFilterOnReadableOrganization(false);
    genClassCriteria.setFetchSize(100);
    genClassCriteria.addOrderBy(Product.PROPERTY_UPDATED, true);
    ScrollableResults genMasterScrollar = genClassCriteria.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;

    List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
    while (genMasterScrollar.next()) {

      bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
      jsonArray.put(JSONHelper.convetBobToJson((BaseOBObject) genMasterScrollar.get()[0]));

      if (i % 100 == 0) {
        bobList.clear();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
    return jsonArray;
  }

}
