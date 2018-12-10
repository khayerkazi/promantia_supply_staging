package in.decathlon.ibud.masters.server;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;
//import org.apache.log4j.Logger;



public class MasterFixtureWS implements WebService {

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

    String updated = request.getParameter("updated");
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    Date updatedDate = format.parse(updated);

    System.out.println("Inside Pull Master Webservice fetching data from " + updatedDate);
    JSONObject jsonDataObject = new JSONObject();

    List<Object[]> modelList = getModelData(updatedDate);
    System.out.println("modelList->" + modelList.size());

    List<Object[]> prdList = getProductData(updatedDate);
    System.out.println("prdList->" + prdList.size());

    List<Object[]> priceList = getProductPriceData(updatedDate);
    System.out.println("priceList->" + priceList.size());

    jsonDataObject.put("model", getModelJsonFromList(modelList));
    jsonDataObject.put("product", getProductJsonFromList(prdList));
    jsonDataObject.put("price", getPriceJsonFromList(priceList));
    jsonDataObject.put("status", "success");

    return jsonDataObject;
  }

  private JSONArray getModelJsonFromList(List<Object[]> modelList) throws JSONException {
    JSONArray jsonArray = new JSONArray();

    for (Object[] m : modelList) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("cl_model_id", m[0]);
      jsonObject.put("ad_client_id", m[1]);
      jsonObject.put("ad_org_id", m[2]);
      jsonObject.put("isactive", m[3]);
      jsonObject.put("created", m[4]);
      jsonObject.put("createdby", m[5]);
      jsonObject.put("updated", m[6]);
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
    return jsonArray;
  }

  private JSONArray getProductJsonFromList(List<Object[]> prdList) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    for (Object[] m : prdList) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("m_product_id", m[0]);
      jsonObject.put("ad_client_id", m[1]);
      jsonObject.put("ad_org_id", m[2]);
      jsonObject.put("isactive", m[3]);
      jsonObject.put("created", m[4]);
      jsonObject.put("createdby", m[5]);
      jsonObject.put("updated", m[6]);
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
    return jsonArray;
  }

  private JSONArray getPriceJsonFromList(List<Object[]> priceList) throws JSONException {
    JSONArray jsonArray = new JSONArray();
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
      jsonObject.put("updated", m[8]);
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
    return jsonArray;
  }

  private List<Object[]> getProductPriceData(Date updatedTime) {
    List<Object[]> priceData = null;
    String priceSQLQuery = "SELECT pp.m_productprice_id, pp.m_pricelist_version_id, pp.m_product_id, pp.ad_client_id, "
        + " pp.ad_org_id, pp.isactive, pp.created, pp.createdby, pp.updated, pp.updatedby, "
        + "pp.pricelist, pp.pricestd, pp.pricelimit, pp.cost, pp.algorithm, pp.em_cl_fobprice, "
        + "pp.em_cl_mrpprice, pp.em_cl_cessionprice, pp.em_cl_ccunitprice, pp.em_cl_ccueprice, "
        + "pp.em_cl_ccpcbprice, pp.em_cl_unitmarginamount, round(pp.em_cl_unitmarginpercentage,2) as em_cl_unitmarginpercentage, "
        + " pp.em_cl_uemarginamount, round(pp.em_cl_uemarginpercentage,2) as em_cl_uemarginpercentage, pp.em_cl_pcbmarginamount, "
        + " round(pp.em_cl_pcbmarginpercentage,2) as em_cl_pcbmarginpercentage "
        + "FROM m_productprice pp   join m_product p on p.m_product_id=pp.m_product_id "
        + "join cl_model ml on p.em_cl_model_id=ml.cl_model_id   join cl_brand b on b.cl_brand_id=ml.cl_brand_id "
        + "where b.name in ('FIXTURES','Events','UNKNOWN') "
        + "and pp.m_pricelist_version_id in (select m_pricelist_version_id from m_pricelist_version where name = 'DMI CATALOGUE') and pp.updated >= ? ";
    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(priceSQLQuery);
    query.setDate(0, updatedTime);
    priceData = query.list();
    return priceData;
  }

  private List<Object[]> getModelData(Date updatedTime) {
    List<Object[]> modelData = null;
    String modelSQLQuery = "SELECT ml.cl_model_id, ml.ad_client_id, ml.ad_org_id, ml.isactive, ml.created, ml.createdby,"
        + " ml.updated, ml.updatedby, ml.value, ml.name, ml.cl_subdepartment_id, ml.cl_department_id,"
        + " ml.cl_sport_id, ml.merchandise_category, ml.cl_brand_id, "
        + " ml.typology, ml.cl_natureofproduct_id, ml.cl_component_brand_id, "
        + " ml.blueproduct, ml.cl_storedept_id, ml.cl_universe_id,  ml.cl_branddepartment_id, imancode "
        + " FROM cl_model ml join m_product p on p.em_cl_model_id=ml.cl_model_id"
        + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id"
        + " where b.name in ('FIXTURES','Events','UNKNOWN') and ml.updated >= ? ";

    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(modelSQLQuery);
    query.setDate(0, updatedTime);
    modelData = query.list();
    return modelData;
  }

  public List<Object[]> getProductData(Date updatedTime) throws Exception {
    List<Object[]> prdData = null;
    String productSQLQuery = "SELECT p.m_product_id, p.ad_client_id, p.ad_org_id, p.isactive, p.created, p.createdby, p.updated, p.updatedby, p.value, p.name, "
        + " p.upc, p.c_uom_id, p.salesrep_id, p.issummary, p.isstocked, p.ispurchased, p.m_product_category_id, p.volume, p.weight, "
        + " p.c_taxcategory_id, p.producttype, p.m_attributeset_id, p.em_cl_log_rec, p.em_cl_modelname, p.em_cl_modelcode, p.em_cl_size, p.em_cl_pcb_qty, p.em_cl_ue_qty, p.em_cl_grosswt_pcb,"
        + " p.em_cl_volume_pcb, p.em_cl_color_id, p.em_cl_model_id, p.em_cl_age, p.em_cl_gender, p.em_cl_lifestage, p.em_cl_typea, p.em_cl_typeb,"
        + " p.em_cl_typec, p.em_cl_ismii "
        + " FROM m_product p join cl_model ml on p.em_cl_model_id=ml.cl_model_id"
        + " join cl_brand b on b.cl_brand_id=ml.cl_brand_id where b.name in ('FIXTURES','Events','UNKNOWN') and ml.updated >= ?";
    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(productSQLQuery);
    query.setDate(0, updatedTime);
    prdData = query.list();
    return prdData;

  }
}
