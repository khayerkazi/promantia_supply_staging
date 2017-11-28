package com.sysfore.decathlonecom.ad_webservices;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * The Web Service is for Ecommerce Customer Resigeration.
 * 
 */

public class EcomCustomerRegister extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(EcomCustomerRegister.class);

  // private static Map<String, PosSyncProcess> posProcesses = null;
  protected static ConnectionProvider pool;
  Connection conn = null;
  // ResourceBundle properties = null;
  boolean setERP = false;
  String memebrcatagories = "6EEA0C26F3834AA18292321C842BEA6E";

  public EcomCustomerRegister() throws Exception {
    // initPool();
    // System.out.println("I'm setting connection.");
    try {
      conn = OBDal.getInstance().getConnection();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (conn != null) {
      // System.out.println("You made it, take control your database now!");
      log.info("Connected to postgresql database!");
    } else {
      // System.out.println("Failed to make connection!");
      log.error("Failed to make postgresql db connection!");
    }
  }

  /**
   * Performs the GET REST operation. This service handles the request for the XML Schema of list of
   * Business Objects.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
    /*
     * System.out.println("Inside the get"); response.setContentType("text/xml");
     * response.setCharacterEncoding("utf-8"); final Writer w = response.getWriter();
     * w.write("inside the get"); w.close();
     */
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    // System.out.println("Inside the post");

    String xml = request.getParameter("ecomUser");

    // Following code converting the String into XML format

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);

    String result = "";
    // System.out.println("xml->" + xml);
    // System.out.println("document->" + document);

    result = parseEcomCustomerRegistryXML(document);
    // System.out.println("result->" + result);

    // doPost(request, response);
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    xmlBuilder.append("<root>");
    if (null != result || (!result.equals(""))) {
      // System.out.println("inside if result->" + result);
      if ((result.length() == 13) && (result.matches("[0-9]+"))) {
        xmlBuilder.append("<oxylane>").append(result).append("</oxylane>");
        xmlBuilder.append("<message>").append("0").append("</message>");
      } else {
        xmlBuilder.append("<oxylane>").append("0").append("</oxylane>");
        xmlBuilder.append("<message>").append(result).append("</message>");
      }
    } else {
      result = "failure";
      xmlBuilder.append("<message>").append(result).append("</message>");
    }
    xmlBuilder.append("</root>");
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(xmlBuilder.toString());
    w.close();

  }

  private String parseEcomCustomerRegistryXML(Document document) throws Exception {
    String msg = "0";
    try { // main try block

      EcomCustomerRegisterDTO eComCustomer = EcomSyncUtil.parseEcomCustomerXML(document);
      eComCustomer.setGreetingId(selectGreetings(eComCustomer.getGreeting()));
      LinkedList<String> companyDetails = selectCompanyDetails(eComCustomer.getCompany());
      if (companyDetails != null && companyDetails.size() > 0) {
        eComCustomer.setCompanyId(companyDetails.get(0));
        eComCustomer.setLicenseId(companyDetails.get(1));
        eComCustomer.setLicenseNo(companyDetails.get(2));
        eComCustomer.setCompanyAddress(companyDetails.get(3));
        List<EcomAddressDTO> address = eComCustomer.getEcomAddress();
        /*
         * // to check for mobile and email in erp if (null != eComCustomer.getEmail() && null !=
         * eComCustomer.getMemberType() && !eComCustomer.getEmail().equals("") &&
         * !eComCustomer.getMobile().equals("")) { msg = checkERP(eComCustomer); if (!msg.equals("")
         * || ((msg.length() == 13) && (msg.matches("[0-9]+")))) { return msg; } }
         */
        HttpURLConnection hc;
        try {
          hc = createCustomerConnection(eComCustomer, "old");
          hc.connect();

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
            // System.out.println("Inside old");
            final JSONObject dataJsonObject = dataJsonArray.getJSONObject(0);
            log.info("JSON Response : " + dataJsonObject);
            in.close();
            if (null != dataJsonObject.get("rCOxylane") || dataJsonObject.has("rCOxylane")
                || !"".equals(dataJsonObject.has("rCOxylane"))) {
              msg = (dataJsonObject.get("rCOxylane").toString());
              if ((msg.length() == 13) && (msg.matches("[0-9]+"))) {
                eComCustomer.setOxylane(msg);
              }
              // System.out.println("dataJsonObject->" + msg);
            }
            // to check for mobile and email in erp
            // System.out.println("Inside !setErp");
            if (null != eComCustomer.getEmail() && null != eComCustomer.getMemberType()
                && !eComCustomer.getEmail().equals("") && !eComCustomer.getMobile().equals("")) {
              String output = checkERP(eComCustomer);
              // System.out.println("output->" + output);
              if (null != output && !"".equals(output)) {
                if ((output.length() == 13) && (output.matches("[0-9]+"))) {
                  msg = output;
                }
              } else {
                setERP = true;
              }
            }

          } else {// else 1
            HttpURLConnection hc1;
            // System.out.println("Inside new");
            try {
              // System.out.println("inside createCustConn:new");
              hc1 = createCustomerConnection(eComCustomer, "new");
              hc1.connect();

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
              log.info("JSON Response : " + dataJsonObject1);
              in1.close();

              msg = (dataJsonObject1.get("rCOxylane").toString());
              eComCustomer.setOxylane(msg);
              setERP = true;
              // System.out.println("dataJsonObject1->" + msg);
            } catch (Exception e1) {
              e1.printStackTrace();
            }

          }// end of else 1
        } catch (Exception e1) {
          e1.printStackTrace();
        }

        if (setERP) {
          msg = createEcomCustomer(eComCustomer);
          // System.out.println("CreateEcomCust->" + msg);
        }

      } else {
        msg = "Company not defined";
      }

    } catch (Exception exp) {
      log.error("Exception while Customer Registration. ", exp);
      exp.printStackTrace();
    }// end of main try block
     // System.out.println("final msg->" + msg);
    return msg;
  }

  private String checkERP(EcomCustomerRegisterDTO eComCustomer) {
    String hql = "";
    String msg = "";
    Query query = null;
    // System.out.println("Inside checkERp");

    try {
      /*
       * hql = "select bp.rCOxylane from BusinessPartner bp where bp.rCMobile ='" +
       * eComCustomer.getMobile() + "' and bp.rCEmail='" + eComCustomer.getEmail() + "'"; query =
       * OBDal.getInstance().getSession().createQuery(hql); query.setMaxResults(1); List bpList =
       * query.list(); if (!bpList.isEmpty()) { msg = bpList.get(0).toString(); }
       */
      // new code
      PreparedStatement pst = null;
      ResultSet rs = null;
      String oxylaneNo = "";

      pst = conn.prepareStatement("select em_rc_oxylane from c_bpartner where em_rc_mobile ='"
          + eComCustomer.getMobile() + "' and em_rc_email='" + eComCustomer.getEmail() + "'");
      rs = pst.executeQuery();
      while (rs.next()) {
        oxylaneNo = rs.getString("em_rc_oxylane");
      }
      rs.close();
      pst.close();
      pst = null;
      rs = null;
      if (null != oxylaneNo || !oxylaneNo.equals("")) {
        msg = oxylaneNo;
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error occured due to: " + e);
      msg = "Error occurred->" + e;
    }
    return msg;
  }

  public String createEcomCustomer(EcomCustomerRegisterDTO eComCustomer) {
    // System.out.println("InsideCreateEComCustomer");
    String sql = "INSERT INTO c_bpartner (c_bpartner_id,ad_client_id,ad_org_id,value,createdby,updatedby,em_rc_source,em_rc_membertype,"
        + "c_greeting_id,name,name2,isactive,c_bp_group_id,em_rc_optin,em_rc_email,em_rc_mobile,em_rc_status,em_rc_company_id,em_rc_location,"
        + "em_rc_license_id,em_rc_licenseno,em_rc_comments,em_rc_conditions,em_rc_oxylane) "
        + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    String sqlPhone = "select c_bpartner_id from c_bpartner where em_rc_mobile = ?";
    String rcOxylane = "failure";
    PreparedStatement pst0 = null;
    ResultSet rs0 = null;
    ResultSet rs3 = null;
    ResultSet rs4 = null;
    PreparedStatement pst = null;
    PreparedStatement pst1 = null;
    PreparedStatement pst2 = null;
    PreparedStatement pst3 = null;
    String uuidCustomer = "";
    String uuidLocation = "";

    int result = 0;
    try {
      pst0 = conn.prepareStatement(sqlPhone);
      pst0.setString(1, eComCustomer.getMobile());
      rs0 = pst0.executeQuery();

      if (rs0.next()) {
        rs0.close();
        pst0.close();
      }

      pst = conn.prepareStatement(sql);

      uuidCustomer = SequenceIdData.getUUID();

      pst.setString(1, uuidCustomer);
      pst.setString(2, OBContext.getOBContext().getCurrentClient().getId());
      pst.setString(3, OBContext.getOBContext().getCurrentOrganization().getId());
      pst.setString(4, eComCustomer.getMobile());
      pst.setString(5, OBContext.getOBContext().getUser().getId());
      pst.setString(6, OBContext.getOBContext().getUser().getId());
      pst.setString(7, eComCustomer.getSource());
      pst.setString(8, eComCustomer.getMemberType());
      pst.setString(9, eComCustomer.getGreetingId());
      pst.setString(10, eComCustomer.getFirstName());
      pst.setString(11, eComCustomer.getLastName());
      pst.setString(12, "Y");
      pst.setString(13, memebrcatagories);
      pst.setString(14, eComCustomer.getOptIn());
      pst.setString(15, eComCustomer.getEmail());
      pst.setString(16, eComCustomer.getMobile());
      pst.setString(17, eComCustomer.getStatus());
      pst.setString(18, eComCustomer.getCompanyId());
      pst.setString(19, eComCustomer.getCompanyAddress());
      pst.setString(20, eComCustomer.getLicenseId());
      pst.setString(21, eComCustomer.getLicenseNo());
      pst.setString(22, eComCustomer.getComments());
      pst.setString(23, "Member has accepted Decathlon Terms and Conditions");
      pst.setString(24, eComCustomer.getOxylane());
      result = pst.executeUpdate();
      if (result == 0) {
        pst.close();
        conn.rollback();
        rcOxylane = "failure";
        return rcOxylane;
      } else {
        uuidLocation = SequenceIdData.getUUID();
        pst1 = conn
            .prepareStatement("insert into c_location (c_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,address1,address2,em_rc_address3,em_rc_address4,city,postal,c_country_id,c_region_id)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        pst1.setString(1, uuidLocation);
        pst1.setString(2, OBContext.getOBContext().getCurrentClient().getId());
        pst1.setString(3, OBContext.getOBContext().getCurrentOrganization().getId());
        pst1.setString(4, "Y");
        pst1.setString(5, OBContext.getOBContext().getUser().getId());
        pst1.setString(6, OBContext.getOBContext().getUser().getId());
        pst1.setString(7, ((eComCustomer.getEcomAddress()).get(0)).getAddress1());
        pst1.setString(8, ((eComCustomer.getEcomAddress()).get(0)).getAddress2());
        pst1.setString(9, ((eComCustomer.getEcomAddress()).get(0)).getAddress3());
        pst1.setString(10, ((eComCustomer.getEcomAddress()).get(0)).getAddress4());
        pst1.setString(11, ((eComCustomer.getEcomAddress()).get(0)).getCity());
        pst1.setString(12, ((eComCustomer.getEcomAddress()).get(0)).getPostalCode());
        // System.out.println("Country " + ((ecomCustomer.getEcomAddress()).get(0)).getCountry());
        pst1.setString(13, selectCountry(((eComCustomer.getEcomAddress()).get(0)).getCountry()));
        pst1.setString(14, selectState(((eComCustomer.getEcomAddress()).get(0)).getState()));
        result = pst1.executeUpdate();

        if (result == 0) {
          pst1.close();
          conn.rollback();
          rcOxylane = "failure";
          return rcOxylane;
        }
      }

      pst2 = conn
          .prepareStatement("insert into c_bpartner_location (c_bpartner_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,name,c_location_id,c_bpartner_id) values (?,?,?,?,?,?,?,?,?)");
      pst2.setString(1, uuidLocation);
      pst2.setString(2, OBContext.getOBContext().getCurrentClient().getId());
      pst2.setString(3, OBContext.getOBContext().getCurrentOrganization().getId());
      pst2.setString(4, "Y");
      pst2.setString(5, OBContext.getOBContext().getUser().getId());
      pst2.setString(6, OBContext.getOBContext().getUser().getId());
      pst2.setString(7, "." + eComCustomer.getFirstName() + "," + eComCustomer.getLastName());
      pst2.setString(8, uuidLocation);
      pst2.setString(9, uuidCustomer);

      result = pst2.executeUpdate();

      rs0.close();
      pst0.close();
      pst.close();
      pst1.close();
      pst2.close();

      conn.commit();

      pst3 = conn.prepareStatement("select em_rc_oxylane from c_bpartner where em_rc_mobile =?");
      pst3.setString(1, eComCustomer.getMobile());
      rs3 = pst3.executeQuery();

      if (rs3.next()) {
        // System.out.println("rs3 : rcOxylane->" + rs3.getString("em_rc_oxylane"));
        rcOxylane = rs3.getString("em_rc_oxylane");
      }

    } catch (Exception e) {
      try {
        conn.rollback();

      } catch (SQLException sqe) {

      }
      e.printStackTrace();
    } finally {
      pst = null;

    }
    // System.out.println("rcOxylane->" + rcOxylane);
    return rcOxylane;
  }

  public String selectGreetings(String value) {
    String sql = "select c_greeting_id from c_greeting where name = (select name from rc_greeting where value=?)";
    String rcGreeting = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcGreeting = rs.getString("c_greeting_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcGreeting;
  }

  public LinkedList<String> selectCompanyDetails(String value) {
    String sql = "select rc_company_id,rc_license_id,licenseno,companyaddress from rc_company where lower(companyname) = lower(?) LIMIT 1";

    LinkedList<String> rcCompany = new LinkedList<String>();
    PreparedStatement pst = null;
    ResultSet rs = null;
    PreparedStatement pst1 = null;
    ResultSet rs1 = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcCompany.add(0, rs.getString("rc_company_id"));
        rcCompany.add(1, rs.getString("rc_license_id"));
        rcCompany.add(2, rs.getString("licenseno"));
        rcCompany.add(3, rs.getString("companyaddress"));
      } else {

        pst1 = conn.prepareStatement(sql);
        pst1.setString(1, "Decathlon Default");
        rs1 = pst1.executeQuery();

        if (rs1.next()) {
          rcCompany.add(0, rs1.getString("rc_company_id"));
          rcCompany.add(1, rs1.getString("rc_license_id"));
          rcCompany.add(2, rs1.getString("licenseno"));
          rcCompany.add(3, rs1.getString("companyaddress"));
        }

      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcCompany;
  }

  public String selectCountry(String value) {

    String sql = "select c_country_id from c_country where name = ?";
    String rcCountry = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcCountry = rs.getString("c_country_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcCountry;
  }

  public String selectState(String value) {

    String sql = "select c_region_id from c_region where value = ?";
    String rcCity = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcCity = rs.getString("c_region_id");
      }
      // System.out.println("City is " + rcCity);
      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcCity;
  }

  protected HttpURLConnection createCustomerConnection(EcomCustomerRegisterDTO ecomCustomer,
      String strKey) throws Exception {

    //
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
    //
    /*
     * OBContext.setAdminMode(); OBCriteria<DSIDEFModuleConfig> configInfoObCriteria =
     * OBDal.getInstance().createCriteria( DSIDEFModuleConfig.class);
     * configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME,
     * "in.decathlon.etlsync"));
     * configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_KEY,
     * "customerdbWSURL")); if (configInfoObCriteria.count() > 0) { customerDBURL =
     * configInfoObCriteria.list().get(0).getSearchKey(); }
     */
    // System.out.println("customerDBURL->" + customerDBURL);
    OBContext.restorePreviousMode();
    URL url = null;
    if (strKey.equals("old")) {
      if (ecomCustomer.getMobile() != null) {
        url = new URL(customerDBURL + "/dsiCustomerdbGet?mobileno=" + ecomCustomer.getMobile()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
        log.info(customerDBURL + "/dsiCustomerdbGet?mobileno=" + ecomCustomer.getMobile()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
      } else if (ecomCustomer.getEmail() != null) {
        url = new URL(customerDBURL + "/dsiCustomerdbGet?email=" + ecomCustomer.getEmail()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
        log.info(customerDBURL + "/dsiCustomerdbGet?email=" + ecomCustomer.getEmail()
            + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
      }
    } else {

      url = new URL(customerDBURL + "/dsiCustomerdbPost?mobileno="
          + getNotNull(ecomCustomer.getMobile()) + "&landline=" + "" + "&email="
          + getNotNull(ecomCustomer.getEmail()) + "&name="
          + getNotNull(URLEncoder.encode(ecomCustomer.getFirstName(), "UTF-8")) + "&name2="
          + getNotNull(URLEncoder.encode(ecomCustomer.getLastName(), "UTF-8"))
          + "&em_sms_alert=N&em_email_alert=N&store=" + "" + "&username=" + customerDBUName
          + "&pwd=" + customerDBPwd + "");
      log.info(customerDBURL
          + "/dsiCustomerdbPost?mobileno="
          + getNotNull(ecomCustomer.getMobile())
          + // "&landline=" + "" +
          "&email=" + getNotNull(ecomCustomer.getEmail()) + "&name="
          + getNotNull(ecomCustomer.getFirstName()) + "&name2="
          + getNotNull(ecomCustomer.getLastName()) + "&em_sms_alert=N&em_email_alert=N&store=" + ""
          + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");

    }
    // System.out.println("url->" + url);

    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    if (strKey.equals("old"))
      hc.setRequestMethod("GET");
    if (strKey.equals("new"))
      hc.setRequestMethod("POST");
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    return hc;

  }

  private String getNotNull(String string) {
    if (null == string || string.equals("null")) {
      return "";
    } else
      return string;
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  // currently not used, but may be used if required in future
  public String updateEcomCustomer(EcomCustomerRegisterDTO eComCustomer) {
    String sql = "UPDATE c_bpartner set ad_client_id=?,ad_org_id=?,value=?,updatedby=?,em_rc_source=?,em_rc_membertype=?,c_greeting_id=?,"
        + "name=?,name2=?,isactive=?,c_bp_group_id=?,em_rc_optin=?,em_rc_email=?,em_rc_mobile=?,em_rc_status=?,em_rc_company_id=?,"
        + "em_rc_location=?,em_rc_license_id=?,em_rc_licenseno=?,em_rc_comments=? where em_rc_oxylane = ? ";

    String sqlOxylane = "select c_bpartner_id from c_bpartner where em_rc_oxylane = ?";
    String sqlPhone = "select c_bpartner_id from c_bpartner where em_rc_mobile = ?";
    String sqlLocation = "select c_bpartner.c_bpartner_id, c_location_id from c_bpartner_location,c_bpartner where c_bpartner.c_bpartner_id=c_bpartner_location.c_bpartner_id and em_rc_oxylane =? ";
    PreparedStatement pst0 = null;
    ResultSet rs0 = null;
    ResultSet rs3 = null;
    ResultSet rs4 = null;
    PreparedStatement pst = null;
    PreparedStatement pst1 = null;
    PreparedStatement pst2 = null;
    PreparedStatement pst3 = null;
    PreparedStatement pst4 = null;
    PreparedStatement pst10 = null;
    PreparedStatement pst11 = null;
    String uuidCustomer = "";
    String uuidLocation = "";
    String bPartnerId = "";

    int result = 0;
    try {
      pst0 = conn.prepareStatement(sqlOxylane);
      pst0.setString(1, eComCustomer.getOxylane());
      rs0 = pst0.executeQuery();

      if (rs0.next()) {
        bPartnerId = rs0.getString("c_bpartner_id");
      } else {
        rs0.close();
        pst0.close();
        return "Oxylane not existed";
      }

      pst3 = conn.prepareStatement(sqlPhone);
      pst3.setString(1, eComCustomer.getMobile());
      rs3 = pst3.executeQuery();

      if (rs3.getFetchSize() > 1) {
        rs3.close();
        pst3.close();

        return "duplicate mobile";
      }
      pst = conn.prepareStatement(sql);

      uuidCustomer = SequenceIdData.getUUID();

      pst.setString(1, OBContext.getOBContext().getCurrentClient().getId());
      pst.setString(2, OBContext.getOBContext().getCurrentOrganization().getId());
      pst.setString(3, eComCustomer.getMobile());
      pst.setString(4, OBContext.getOBContext().getUser().getId());
      pst.setString(5, eComCustomer.getSource());
      pst.setString(6, eComCustomer.getMemberType());
      pst.setString(7, eComCustomer.getGreetingId());
      pst.setString(8, eComCustomer.getFirstName());
      pst.setString(9, eComCustomer.getLastName());
      pst.setString(10, "Y");
      pst.setString(11, memebrcatagories);
      pst.setString(12, eComCustomer.getOptIn());
      pst.setString(13, eComCustomer.getEmail());
      pst.setString(14, eComCustomer.getMobile());
      pst.setString(15, eComCustomer.getStatus());
      pst.setString(16, eComCustomer.getCompanyId());
      pst.setString(17, eComCustomer.getCompanyAddress());
      pst.setString(18, eComCustomer.getLicenseId());
      pst.setString(19, eComCustomer.getLicenseNo());
      pst.setString(20, eComCustomer.getComments());
      pst.setString(21, eComCustomer.getOxylane());
      result = pst.executeUpdate();
      // System.out.println("Result  " + result);
      if (result == 0) {
        pst.close();
        conn.rollback();
        return "failure";
      } else {

        pst4 = conn.prepareStatement(sqlLocation);
        pst4.setString(1, eComCustomer.getOxylane());
        rs4 = pst4.executeQuery();

        while (rs4.next()) {
          String location_id = rs4.getString("c_location_id");
          pst10 = conn.prepareStatement("delete from c_bpartner_location where c_location_id = ?");
          pst10.setString(1, location_id);
          result = pst10.executeUpdate();

          pst11 = conn.prepareStatement("delete from c_location where c_location_id = ?");
          pst11.setString(1, location_id);
          result = pst11.executeUpdate();
        }

        rs4.close();
        pst4.close();

        List<EcomAddressDTO> address = eComCustomer.getEcomAddress();
        for (int i = 0; i < address.size(); i++) {
          uuidLocation = SequenceIdData.getUUID();
          pst1 = conn
              .prepareStatement("insert into c_location (c_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,address1,address2,em_rc_address3,em_rc_address4,city,postal,c_country_id,c_region_id)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
          pst1.setString(1, uuidLocation);
          pst1.setString(2, OBContext.getOBContext().getCurrentClient().getId());
          pst1.setString(3, OBContext.getOBContext().getCurrentOrganization().getId());
          pst1.setString(4, "Y");
          pst1.setString(5, OBContext.getOBContext().getUser().getId());
          pst1.setString(6, OBContext.getOBContext().getUser().getId());
          pst1.setString(7, address.get(i).getAddress1());
          pst1.setString(8, address.get(i).getAddress2());
          pst1.setString(9, address.get(i).getAddress3());
          pst1.setString(10, address.get(i).getAddress4());
          pst1.setString(11, address.get(i).getCity());
          pst1.setString(12, address.get(i).getPostalCode());
          pst1.setString(13, selectCountry(address.get(i).getCountry()));
          pst1.setString(14, selectState(address.get(i).getState()));
          result = pst1.executeUpdate();

          if (!uuidLocation.equals("")) {
            pst2 = conn
                .prepareStatement("insert into c_bpartner_location (c_bpartner_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,name,c_location_id,c_bpartner_id) values (?,?,?,?,?,?,?,?,?)");
            pst2.setString(1, uuidLocation);
            pst2.setString(2, OBContext.getOBContext().getCurrentClient().getId());
            pst2.setString(3, OBContext.getOBContext().getCurrentOrganization().getId());
            pst2.setString(4, "Y");
            pst2.setString(5, OBContext.getOBContext().getUser().getId());
            pst2.setString(6, OBContext.getOBContext().getUser().getId());
            pst2.setString(7, "." + eComCustomer.getFirstName() + "," + eComCustomer.getLastName());
            pst2.setString(8, uuidLocation);
            pst2.setString(9, bPartnerId);

            result = pst2.executeUpdate();
            pst2.close();
          }

        }

      }
      rs0.close();
      pst0.close();
      pst.close();
      pst1.close();

      conn.commit();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        conn.rollback();
        return "failure";
      } catch (SQLException sqe) {

      }

      return "failure";
    } finally {
      pst = null;

    }

    return "Updated Details";

  }
}
