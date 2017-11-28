package in.nous.searchitem.ad_webservice;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;
import java.util.StringTokenizer;

public class SignagePriceDetails implements WebService {

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(SignagePriceDetails.class);

  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    Connection conn = null;
    conn = OBDal.getInstance().getConnection();
    PreparedStatement pst = null;
    ResultSet rs = null;
    // do some checking of parameters
    String modelcodes = request.getParameter("modelcodes");
    String orgid = request.getParameter("orgid");
    if (modelcodes == null) {
      throw new IllegalArgumentException("The modelcode parameter is mandatory");
    }
    if (orgid == null) {
      throw new IllegalArgumentException("The orgid parameter is mandatory");
    }
    // and get the result
    StringWriter sw = new StringWriter();
   // modelcodes="8806968,8806967";
    StringTokenizer st2 = new StringTokenizer(modelcodes, ",");
    int count=0;
    JSONObject jsonDataObject = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    while (st2.hasMoreElements()) {
           // System.out.println(st2.nextElement());
      JSONArray jsonArray1 = new JSONArray();
      JSONObject jsonDataObject1 = new JSONObject();
            String modelcode=st2.nextToken().toString();
            //System.out.println("modelcodess "+modelcode);
    
            jsonArray1 = getPricelist(modelcode,orgid);
  
            jsonDataObject1.put("Result", jsonArray1);
            jsonArray.put(jsonDataObject1);
            
    } // end of while loop
    jsonDataObject.put("Success", jsonArray);
    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());
    w.close();
  }

  private JSONArray getPricelist(String modelcode,String orgid) throws JSONException {
    String hql = "";
    Query query = null;
   
    
    JSONObject jsonDataObject = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    try {
      
      hql = "select pp.product.clModel.merchandiseCategory,pp.product.clModelcode,pp.product.name,pp.clMrpprice,pp.clCcunitprice,pp.clCcueprice,pp.cLEMClSuqty from PricingProductPrice pp where pp.product.clModelcode='"
          + modelcode + "' and  pp.organization.id='"
          + orgid + "'";
      query = OBDal.getInstance().getSession().createQuery(hql);
     // query.setMaxResults(1);
     
     
      List<Object[]> reportList = query.list();
      //System.out.println("sqlQuery " + sqlQuery);

      jsonArray = getJsonFromList(reportList);
    
    } catch (Exception exp) {
      log.error("Exception while fetching data. ", exp);
      //jsonDataObject.put("status", "failure");
    }
 
    return jsonArray;
  }

  private JSONArray getJsonFromList(List<Object[]> reportList) throws Exception {
    JSONArray jsonArray = new JSONArray();
    for (Object[] objects : reportList) {
      JSONObject jsonObject = new JSONObject();
     
      
      jsonObject.put("ProductFamily", objects[0].toString());
      jsonObject.put("Modelcode", objects[1].toString());
      jsonObject.put("Itemcode", objects[2].toString());
      jsonObject.put("MRP", objects[3].toString());
      jsonObject.put("CCUnitPrice", objects[4].toString());
      jsonObject.put("CCUeprice", objects[5].toString());
      jsonObject.put("UEQTY", objects[6].toString());
     
      
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
}
