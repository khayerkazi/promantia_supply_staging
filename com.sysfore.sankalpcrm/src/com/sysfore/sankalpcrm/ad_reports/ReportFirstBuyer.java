package com.sysfore.sankalpcrm.ad_reports;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportFirstBuyer extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportFirstBuyer|dateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportFirstBuyer|dateTo", "");
      String strAdOrgId = vars.getOrg();

      printPageDataSheet(response, vars, strDateFrom, strDateTo, strAdOrgId);
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportFirstBuyer|dateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportFirstBuyer|dateTo", "");
      String strAdOrgId = vars.getOrg();

      printPageHTML(response, vars, strDateFrom, strDateTo, strAdOrgId);
    } else if (vars.commandIn("XLS")) {

      String strDateFrom = vars
          .getRequestGlobalVariable("inpDateFrom", "ReportFirstBuyer|dateFrom");
      System.out.println("strDateFrom: in XLS " + strDateFrom);
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportStockValuation|dateTo");
      System.out.println("strDateTo: in XLS " + strDateTo);
      String strAdOrgId = vars.getOrg();
      System.out.println("strAdOrgId: in XLS " + strAdOrgId);

      printPageXls(response, vars, strDateFrom, strDateTo, strAdOrgId);// strDateFrom,

    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strAdOrgId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = null;

    xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/sankalpcrm/ad_reports/ReportFirstBuyer")
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportFirstBuyer", false, "", "", "",
        false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.sankalpcrm.ad_reports.ReportFirstBuyer");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportFirstBuyer.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportFirstBuyer.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportFirstBuyer");
      vars.removeMessage("ReportFirstBuyer");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("today", DateTimeData.today(this));
    xmlDocument.setParameter("USER_ORG", strAdOrgId);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom,
      String strDateTo, String strAdOrgId) throws IOException, ServletException {

    System.out.println("strAdOrgId: " + strAdOrgId);
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    if (strAdOrgId.equals("0")) {
      try {
        Calendar cal = Calendar.getInstance();
        cal.setTime(df.parse(strDateTo));
        cal.add(Calendar.DATE, 1);
        String convertedDate = df.format(cal.getTime());
        strDateTo = convertedDate;
        System.out.println("todate after converting is: " + strDateTo);
      } catch (ParseException ex) {
        Logger.getLogger(ReportFirstBuyer.class.getName()).log(Level.SEVERE, null, ex);
      }

      ReportFirstBuyerData[] data = ReportFirstBuyerData.select(this, strDateFrom, strDateTo);

      String strBaseDesign = getBaseDesignPath(vars.getLanguage());

      String strOutput = "xls";

      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportFirstBuyer.jrxml";
      System.out.println("after fetching strReportName" + strReportName);
      String strTitle = classInfo.name;
      Date fromdate = null;
      Date todate = null;
      // DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      try {
        fromdate = df.parse(strDateFrom);
        todate = df.parse(strDateTo);
      } catch (ParseException ex) {
        Logger.getLogger(ReportFirstBuyer.class.getName()).log(Level.SEVERE, null, ex);
      }

      response.setHeader("Content-disposition", "inline;filename=ReportFirstBuyer.xls");
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_TITLE", strTitle);
      parameters.put("datefrom", fromdate);
      parameters.put("dateto", todate);
      parameters.put("USER_ORG", strAdOrgId);

      // Added for displaying lines in excel file
      HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
      String strSubTitle = "";
      strSubTitle = Utility.messageBD(this, "From", vars.getLanguage()) + " " + strDateFrom + " "
          + Utility.messageBD(this, "To", vars.getLanguage()) + " " + strDateTo;
      parameters.put("REPORT_SUBTITLE", strSubTitle);
      parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

      for (int i = 0; i <= data.length - 1; i++) {
       // System.out.println("decathlon ID " + data[i].decathlonid);

        // Sending HTTP POST request
        HttpURLConnection hc;
        try {
          hc = createConnection(data[i].decathlonid);
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
          final JSONObject dataJsonObject = dataJsonArray.getJSONObject(0);
          //System.out.println("JSON Response : " + dataJsonObject);
          in.close();
          data[i].membername = dataJsonObject.get("name").toString();
          data[i].lastname = dataJsonObject.get("name2").toString();
          data[i].postalcode = dataJsonObject.get("rCZipcode").toString();
          data[i].mobile = dataJsonObject.get("rCMobile").toString();
          data[i].landline = dataJsonObject.get("rCLandline").toString();
          data[i].email = dataJsonObject.getString("rCEmail").toString();
        } catch (Exception e) {
          e.printStackTrace();
        }

      }

      renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
    } else {
      try {
        Calendar cal = Calendar.getInstance();
        cal.setTime(df.parse(strDateTo));
        cal.add(Calendar.DATE, 1);
        String convertedDate = df.format(cal.getTime());
        strDateTo = convertedDate;
        System.out.println("todate after converting is: " + strDateTo);
      } catch (ParseException ex) {
        Logger.getLogger(ReportFirstBuyer.class.getName()).log(Level.SEVERE, null, ex);
      }

      ReportFirstBuyerData[] data = ReportFirstBuyerData.selectOrg(this, strAdOrgId, strDateFrom,
          strDateTo);

      String strBaseDesign = getBaseDesignPath(vars.getLanguage());

      String strOutput = "xls";

      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportFirstBuyer.jrxml";
      System.out.println("after fetching strReportName" + strReportName);
      String strTitle = classInfo.name;
      Date fromdate = null;
      Date todate = null;
      // DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      try {
        fromdate = df.parse(strDateFrom);
        todate = df.parse(strDateTo);
      } catch (ParseException ex) {
        Logger.getLogger(ReportFirstBuyer.class.getName()).log(Level.SEVERE, null, ex);
      }

      response.setHeader("Content-disposition", "inline;filename=ReportFirstBuyer.xls");
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_TITLE", strTitle);
      parameters.put("datefrom", fromdate);
      parameters.put("dateto", todate);
      parameters.put("USER_ORG", strAdOrgId);

      // Added for displaying lines in excel file
      HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
      String strSubTitle = "";
      strSubTitle = Utility.messageBD(this, "From", vars.getLanguage()) + " " + strDateFrom + " "
          + Utility.messageBD(this, "To", vars.getLanguage()) + " " + strDateTo;
      parameters.put("REPORT_SUBTITLE", strSubTitle);
      parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

      for (int i = 0; i <= data.length - 1; i++) {
       // System.out.println("decathlon ID " + data[i].decathlonid);

        // Sending HTTP POST request
        HttpURLConnection hc;
        try {
          hc = createConnection(data[i].decathlonid);
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
          final JSONObject dataJsonObject = dataJsonArray.getJSONObject(0);
         // System.out.println("JSON Response : " + dataJsonObject);
          in.close();
          data[i].membername = dataJsonObject.get("name").toString();
          data[i].lastname = dataJsonObject.get("name2").toString();
          data[i].postalcode = dataJsonObject.get("rCZipcode").toString();
          data[i].mobile = dataJsonObject.get("rCMobile").toString();
          data[i].landline = dataJsonObject.get("rCLandline").toString();
          data[i].email = dataJsonObject.getString("rCEmail").toString();

        } catch (Exception e) {
          e.printStackTrace();
        }

      }

      renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
    }

  }

  private void printPageHTML(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strAdOrgId) throws IOException, ServletException {
    /*
     * if (log4j.isDebugEnabled()) log4j.debug("Output: dataSheet"); XmlDocument xmlDocument = null;
     * 
     * System.out.println("strAdOrgId: " + strAdOrgId); ReportFirstBuyerData[] data =
     * ReportFirstBuyerData.selectOrg(this,strAdOrgId, strDateFrom, strDateTo);
     * 
     * 
     * xmlDocument = xmlEngine.readXmlTemplate(
     * "com/sysfore/sankalpcrm/ad_reports/ReportFirstBuyer").createXmlDocument(); ToolBar toolbar =
     * new ToolBar(this, vars.getLanguage(), "ReportFirstBuyer", false, "", "", "", false,
     * "ad_reports", strReplaceWith, false, true); toolbar.prepareSimpleToolBarTemplate();
     * xmlDocument.setParameter("toolbar", toolbar.toString()); try { WindowTabs tabs = new
     * WindowTabs(this, vars, "com.sysfore.sankalpcrm.ad_reports.ReportFirstBuyer");
     * xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
     * xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
     * xmlDocument.setParameter("childTabContainer", tabs.childTabs());
     * xmlDocument.setParameter("theme", vars.getTheme()); NavigationBar nav = new
     * NavigationBar(this, vars.getLanguage(), "ReportFirstBuyer.html", classInfo.id,
     * classInfo.type, strReplaceWith, tabs.breadcrumb()); xmlDocument.setParameter("navigationBar",
     * nav.toString()); LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(),
     * "ReportFirstBuyer.html", strReplaceWith); xmlDocument.setParameter("leftTabs",
     * lBar.manualTemplate()); } catch (Exception ex) { throw new ServletException(ex); } { OBError
     * myMessage = vars.getMessage("ReportFirstBuyer"); vars.removeMessage("ReportFirstBuyer"); if
     * (myMessage != null) { xmlDocument.setParameter("messageType", myMessage.getType());
     * xmlDocument.setParameter("messageTitle", myMessage.getTitle());
     * xmlDocument.setParameter("messageMessage", myMessage.getMessage()); } }
     * 
     * xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
     * xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
     * xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
     * xmlDocument.setParameter("dateFrom", strDateFrom);
     * xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
     * xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
     * xmlDocument.setParameter("dateTo", strDateTo);
     * xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
     * xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
     * xmlDocument.setParameter("today", DateTimeData.today(this));
     * xmlDocument.setParameter("USER_ORG", strAdOrgId);
     * 
     * 
     * response.setContentType("text/html; charset=UTF-8"); PrintWriter out = response.getWriter();
     * // out.println(xmlDocument.print()); // out.close(); log4j.debug("STRUCTURE SET");
     * xmlDocument.setData("structure1", data); out.println(xmlDocument.print()); out.close();
     */
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
  protected HttpURLConnection createConnection(String oxylaneId) throws Exception {

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
    OBContext.restorePreviousMode();

    final URL url = new URL(customerDBURL + "/dsiCustomerdbGet?decathlonId=" + oxylaneId
        + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
    //System.out.println(customerDBURL + "/dsiCustomerdbGet?decathlonId=" + oxylaneId + "&username="
        //+ customerDBUName + "&pwd=" + customerDBPwd + "");
    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();

    hc.setRequestMethod("GET");
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    return hc;
  }

  public String getServletInfo() {
    return "Servlet ReportFirstBuyer. This Servlet was made by Pablo Sarobe";
  } // end of the getServletInfo() method
}
