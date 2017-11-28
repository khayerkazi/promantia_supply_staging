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

public class GetUserStorelist implements WebService {

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(GetUserStorelist.class);

  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    Connection conn = null;
    conn = OBDal.getInstance().getConnection();
    PreparedStatement pst = null;
    ResultSet rs = null;
    // do some checking of parameters
    String user_id = request.getParameter("user_id");
    if (user_id == null) {
      throw new IllegalArgumentException("The user_id parameter is mandatory");
    }
    // and get the result
    StringWriter sw = new StringWriter();
    JSONObject jsonDataObject = getUserStorelist(user_id);

    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());
    w.close();
  }

  private JSONObject getUserStorelist(String user_id) throws JSONException {
    String hql = "";
    Query query = null;
    String role_id = "";
    JSONObject jsonDataObject = new JSONObject();
    try {
      JSONArray jsonArray = new JSONArray();
      JSONArray storeArray = new JSONArray();
      JSONObject jsonObject = new JSONObject();
      hql = "select u.defaultOrganization.name , u.defaultRole.id from ADUser u where u.id='"
          + user_id + "'";
      query = OBDal.getInstance().getSession().createQuery(hql);
      query.setMaxResults(1);
      List list = query.list();
      if (!list.isEmpty()) {
        for (Object rows : list) {
          Object[] row = (Object[]) rows;
          // System.out.println(row[0].toString());
          if (row[0].toString().isEmpty()) {
            jsonObject.put("default_org", "");
          } else {
            jsonObject.put("default_org", row[0].toString());
          }
          // System.out.println(row[1].toString());
          role_id = row[1].toString();
        }
        hql = "select rog.organization.searchKey , rog.organization.description from ADRoleOrganization rog where rog.role ='"
            + role_id
            + "' and rog.organization.id in (select org.id from Organization org where sWIsstore='Y')";
        query = OBDal.getInstance().getSession().createQuery(hql);
        List orgList = query.list();
        if (!orgList.isEmpty()) {
          List<Object[]> storeList = orgList;
          storeArray = getJsonFromList(storeList);
        }
        // System.out.println("orgList->" + orgList);
        jsonObject.put("stores", storeArray);
        jsonDataObject.put("status", "Success");
      } else {
        jsonDataObject.put("status", "No data found");
        jsonObject.put("default_org", "");
        jsonObject.put("stores", "");
      }
      jsonArray.put(jsonObject);
      jsonDataObject.put("data", jsonArray);
    } catch (Exception exp) {
      log.error("Exception while getting User Storelist data:", exp);
      jsonDataObject.put("status", "Failure");
      jsonDataObject.put("data", exp);
      // exp.printStackTrace();
      return jsonDataObject;
    }

    return jsonDataObject;
  }

  private JSONArray getJsonFromList(List<Object[]> storeList) throws Exception {
    JSONArray jsonArray = new JSONArray();
    for (Object[] objects : storeList) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("storevalue", objects[0].toString());
      jsonObject.put("storedesp", objects[1].toString());
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
