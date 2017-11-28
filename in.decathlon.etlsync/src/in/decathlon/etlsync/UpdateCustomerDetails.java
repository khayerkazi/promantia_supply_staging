package in.decathlon.etlsync;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

public class UpdateCustomerDetails implements WebService {

  private static final Logger log = Logger.getLogger(UpdateCustomerDetails.class);
  private static int existingCustomers = 0;
  private static int newCustomers = 0;
  private static int junkData = 0;
  private static int total = 0;

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String result = "";
    // fetch docType from config table
    // String docTypeId = getOrderDocTypeID();
    // System.out.println("Inside doPost");
    // fetch orders without oxylaneNos
    log.info("Inside method Update Customer Details");
    final Session session = OBDal.getInstance().getSession();
    String hql = "from Order o where (o.rCOxylaneno is null or o.rCOxylaneno='') and o.processed=true and o.documentType in (select id from DocumentType where name like '%POS %' and  sOSubType = 'WR')";
    Query query = session.createQuery(hql).setMaxResults(500);
    List<Order> ord = query.list();
    // System.out.println("ord->" + ord.size());

    // validate the customer
    result = validateCustomers(ord);
    System.out.println("junkdata->" + junkData);
    // System.out.println("total->" + total);

    // doPost(request, response);
    JSONObject json = null;
    PrintWriter out = null;
    if (result.equals("success")) {
      try {
        json = new JSONObject("{'response':{'Orders without customers':'" + total
            + "','New Customers Created':'" + newCustomers + "','Updated customers':'"
            + existingCustomers + "'},'status':'200'}");
        log.info("Success : " + json);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    } else {
      try {
        json = new JSONObject("{'response': " + result + ",'status':'201'}");
        log.info("Failure : " + json);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    // refresh the static variables
    junkData = 0;
    total = 0;
    existingCustomers = 0;
    newCustomers = 0;
    response.setContentType("application/json");
    out = response.getWriter();
    out.print(json);
    out.flush();
    out.close();
  }

  private String validateCustomers(final List<Order> ord) {
    // System.out.println("Inside Validate Customer");

    String result = "success";
    boolean update = false;
    for (Order o : ord) {
      if ((null == o.getSyncEmail() || o.getSyncEmail().equals(""))
          && (null == o.getSyncLandline() || o.getSyncLandline().equals(""))
          && (null == o.getRCMobileNo() || o.getRCMobileNo().equals(""))) {
        // all columns null
        junkData = junkData + 1;
      } else {
        total = total + 1;
        HttpURLConnection hc;
        String oxylaneNo = "";
        try {
          hc = createConnection(o, "old");
          hc.connect();
          // System.out.println("Inside Connection hc" + hc);
          // Getting the Response from the Web service
          BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
          String inputLine;
          StringBuffer resp = new StringBuffer();
          while ((inputLine = in.readLine()) != null) {
            resp.append(inputLine);
          }
          String secondResponse = resp.toString();
          final JSONObject respJsonObject = new JSONObject(secondResponse);
          final JSONObject responseJsonObject = (JSONObject) respJsonObject.get("response");
          final JSONArray dataJsonArray = (JSONArray) responseJsonObject.get("data");
          if (dataJsonArray.length() > 0) {
            final JSONObject dataJsonObject = dataJsonArray.getJSONObject(0);
            // log.info("JSON Response : " + dataJsonObject);
            in.close();
            if (null != dataJsonObject.get("rCOxylane") || dataJsonObject.has("rCOxylane")
                || !"".equals(dataJsonObject.has("rCOxylane"))) {
              oxylaneNo = (dataJsonObject.get("rCOxylane").toString());
              // System.out.println("Inside old: oxylaneNo->" + oxylaneNo);
              if ((oxylaneNo.length() == 13) && (oxylaneNo.matches("[0-9]+"))) {
                updateOxylaneForOrder(o, oxylaneNo, "old");
                // OBDal.getInstance().commitAndClose();
                SessionHandler.getInstance().commitAndStart();
              }
            }
          } else {// else 1
            HttpURLConnection hc1;
            // System.out.println("Inside new");
            try {
              hc1 = createConnection(o, "new");
              hc1.connect();
              // System.out.println("new's connection->" + hc1);

              // Getting the Response from the Web service
              BufferedReader in1 = new BufferedReader(new InputStreamReader(hc1.getInputStream()));
              String inputLine1;
              StringBuffer resp1 = new StringBuffer();
              while ((inputLine1 = in1.readLine()) != null) {
                resp1.append(inputLine1);
              }
              String secondResponse1 = resp1.toString();
              final JSONObject respJsonObject1 = new JSONObject(secondResponse1);
              final JSONObject responseJsonObject1 = (JSONObject) respJsonObject1.get("response");
              final JSONArray dataJsonArray1 = (JSONArray) responseJsonObject1.get("data");
              final JSONObject dataJsonObject1 = dataJsonArray1.getJSONObject(0);
              // log.info("JSON Response : " + dataJsonObject1);
              in1.close();

              oxylaneNo = (dataJsonObject1.get("rCOxylane").toString());
              updateOxylaneForOrder(o, oxylaneNo, "new");
              SessionHandler.getInstance().commitAndStart();
            } catch (Exception e1) {
              log.error(e1);
              result = e1.toString();
            }

          }// end of else 1
        } catch (Exception e1) {
          log.error(e1);
          result = e1.toString();
        }
      }
    }
    // System.out.println("Returning result");

    return result;
  }

  private void updateOxylaneForOrder(Order o, String oxylaneNo, String key) {
    // log.info("Order->" + o.getId());
    o.setRCOxylaneno(oxylaneNo);
    o.setUpdated(new Date());
    OBDal.getInstance().save(o);
    OBDal.getInstance().flush();
    if (key.equals("old")) {
      // System.out.println("Inside old updateoxylane");
      existingCustomers = existingCustomers + 1;
    } else {
      // System.out.println("Inside new updateoxylane");
      newCustomers = newCustomers + 1;
    }
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  /**
   * Creates a HTTP connection.
   * 
   * @param wsPart
   * @param method
   *          POST, PUT, GET or DELETE
   * @return the created connection
   * @throws Exception
   */

  protected HttpURLConnection createConnection(Order o, String key) throws Exception {

    String customerDBURL = "";
    String customerDBUName = "";
    String customerDBPwd = "";
    OBContext.setAdminMode();
    final Map<String, String> custmerDBConfig = new HashMap<String, String>();
    OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(
        DSIDEFModuleConfig.class);
    configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME,
        "in.decathlon.customerdb"));
    if (configInfoObCriteria.count() > 0) {
      for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
        custmerDBConfig.put(config.getKey(), config.getSearchKey());
      }
      customerDBURL = custmerDBConfig.get("customerdbWSURL");
      customerDBUName = custmerDBConfig.get("custSearchId");
      customerDBPwd = custmerDBConfig.get("custSearchPwd");
    }
    // System.out.println("customerdbWSURL=>" + customerDBURL);
    // System.out.println("customerDBUName->" + customerDBUName);
    // System.out.println("customerDBPwd->" + customerDBPwd);

    // System.out.println("Inside createconnection");

    OBContext.restorePreviousMode();
    URL url = null;
    if (key.equals("old")) {
      // System.out.println("Inside old createconnection");

      if (o.getRCMobileNo() != null && !o.getRCMobileNo().equals("")) {
        url = new URL(customerDBURL + "/dsiCustomerdbGet?mobileno=" + o.getRCMobileNo()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
        log.debug(customerDBURL + "/dsiCustomerdbGet?mobileno=" + o.getRCMobileNo() + "&username="
            + customerDBUName + "&pwd=" + customerDBPwd + "");
      } else if (o.getSyncEmail() != null && !o.getSyncEmail().equals("")) {
        url = new URL(customerDBURL + "/dsiCustomerdbGet?email=" + o.getSyncEmail() + "&username="
            + customerDBUName + "&pwd=" + customerDBPwd + "");
        log.debug(customerDBURL + "/dsiCustomerdbGet?email=" + o.getSyncEmail() + "&username="
            + customerDBUName + "&pwd=" + customerDBPwd + "");
      } else if (o.getSyncLandline() != null && !o.getSyncLandline().equals("")) {
        url = new URL(customerDBURL + "/dsiCustomerdbGet?landline=" + o.getSyncLandline()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
        log.debug(customerDBURL + "/dsiCustomerdbGet?landline=" + o.getSyncLandline()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
      }
    } else {
      // System.out.println("Inside new createconnection");

      url = new URL(customerDBURL
          + "/dsiCustomerdbPost?mobileno="
          + getNotNull(o.getRCMobileNo())
          + "&landline="
          + getNotNull(o.getSyncLandline())
          + "&email="
          + getNotNull(o.getSyncEmail())
          + "&name=Anonymous&name2=Customer&em_sms_alert=N&em_email_alert=N&store="
          + getNotNull(URLEncoder.encode(o.getOrganization().getDescription(), "UTF-8"))
          + "&profile_created_by="
          + (getNotNull(o.getUserContact().getId()).equals("") ? (getNotNull(o.getCreatedBy()
              .getId())) : getNotNull(o.getUserContact().getId())) + "&creation_source="
          + getNotNull(o.getOrganization().getName()) + "&username=" + customerDBUName + "&pwd="
          + customerDBPwd + "");
      log.debug(customerDBURL
          + "/dsiCustomerdbPost?mobileno="
          + getNotNull(o.getRCMobileNo())
          + "&landline="
          + getNotNull(o.getSyncLandline())
          + "&email="
          + getNotNull(o.getSyncEmail())
          + "&name=Anonymous&name2=Customer&em_sms_alert=N&em_email_alert=N&store="
          + getNotNull(URLEncoder.encode(o.getOrganization().getDescription(), "UTF-8"))
          + "&profile_created_by="
          + (getNotNull(o.getUserContact().getId()).equals("") ? (getNotNull(o.getCreatedBy()
              .getId())) : getNotNull(o.getUserContact().getId())) + "&creation_source="
          + getNotNull(o.getOrganization().getName()) + "&username=" + customerDBUName + "&pwd="
          + customerDBPwd + "");
    }
    // System.out.println("end of createconnection");

    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    if (key.equals("old"))
      hc.setRequestMethod("GET");
    if (key.equals("new"))
      hc.setRequestMethod("POST");
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    return hc;
  }

  /**
   * Method to get Order DocType ID
   */
  private String getOrderDocTypeID() {
    DSIDEFModuleConfig bp = null;
    OBContext.setAdminMode();
    OBCriteria<DSIDEFModuleConfig> obCriteria = OBDal.getInstance().createCriteria(
        DSIDEFModuleConfig.class);
    obCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_KEY, "orderdoctype"));
    obCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.etlsync"));
    final List<DSIDEFModuleConfig> cBPList = obCriteria.list();
    if (!cBPList.isEmpty())
      bp = cBPList.get(0);
    else
      throw new OBException("orderdoctype not found in dsidef_module_config table");
    OBContext.restorePreviousMode();
    return bp.getSearchKey();
  }

  private String getNotNull(String string) {
    if (null == string || string.equals("null")) {
      return "";
    } else
      return string;
  }
}
