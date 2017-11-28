/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2001-2006 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.sysfore.sankalpcrm.ad_reports;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

//Added for displaying lines in excel file

public class ReportCustomerSatisfaction extends HttpSecureAppServlet {

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      // BEGIN Parse parameters passed by the form
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportCustomerSatisfaction|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportCustomerSatisfaction|dateTo");
      printPageDataSheet(response, vars, strDateFrom, strDateTo);

    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportCustomerSatisfaction|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportCustomerSatisfaction|dateTo");

      printPageHtml(request, response, vars, strDateFrom, strDateTo);
    } else if (vars.commandIn("XLS")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportCustomerSatisfaction|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportCustomerSatisfaction|dateTo");

      printPageXls(response, vars, strDateFrom, strDateTo);

    } else
      pageError(response);
    // END Controller of actions
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    // BEGIN Retrieve invoice data for the selected timespan
    /*
     * ReportCustomerSatisfactionData[] data = ReportCustomerSatisfactionData.select(this,
     * Utility.getContext(this, vars, "#User_Client", "ReportCustomerSatisfaction"),
     * Utility.getContext(this, vars, "#AccessibleOrgTree",
     * "ReportCustomerSatisfaction"),strDateFrom, DateTimenameData .nDaysAfter(this, strDateTo,
     * "1"));
     */
    // END Retrieve invoice data for the selected timespan

    // BEGIN Initialize the UI template
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/sankalpcrm/ad_reports/ReportCustomerSatisfaction").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_reports.ReportCustomerSatisfaction");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "ReportCustomerSatisfaction.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(),
          "ReportCustomerSatisfaction.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");
    // END Set up and pass core UI parameters to the template
    log4j.debug("UI parameters set");

    // BEGIN Prepare and set empty toolbar
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportCustomerSatisfaction", false,
        "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportCustomerSatisfaction");
    vars.removeMessage("ReportCustomerSatisfaction");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    // END Check, show & remove message
    log4j.debug("MESSAGE SET");

    // BEGIN Set parameters that need to be redisplayed on refresh

    // xmlDocument.setParameter("Jvacancy",strJvacancy);
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    log4j.info("Sample Display");
    // try {
    // ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "er_jvacancy_ID",
    // "", "er_jvacancy name", Utility.getContext(this, vars, "#AccessibleOrgTree",
    // "SampleExcel"), Utility.getContext(this, vars, "#User_Client",
    // "SampleExcel"), 0);
    // Utility.fillSQLParameters(this, vars, null, comboTableData,
    // "RecuitmentJR", strJvacancy);
    // xmlDocument.setData("reportER_jvacancy_ID", "liststructure", comboTableData.select(false));
    // comboTableData = null;
    // } catch (Exception ex) {
    // throw new ServletException(ex);
    // }

    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    log4j.debug("STRUCTURE SET");
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strDateFrom, String strDateTo) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportCustomerSatisfactionData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();
    try {
      System.out.println("Calling select inside printPageHtml");
      data = ReportCustomerSatisfactionData.select(this,
          Utility.getContext(this, vars, "#User_Client", "ReportCustomerSatisfaction"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportCustomerSatisfaction"),
          strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"));
    } catch (ServletException ex) {
      ex.printStackTrace();
      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
    strConvRateErrorMsg = myMessage.getMessage();
    // If a conversion rate is missing for a certain transaction, an error
    // message window pops-up.
    // if (!strConvRateErrorMsg.equals("") && strConvRateErrorMsg != null) {
    // advisePopUp(request, response, "ERROR", Utility.messageBD(this, "NoConversionRateHeader",
    // vars.getLanguage()), strConvRateErrorMsg);
    // } //else { // Launch the report as usual, calling the JRXML file
    String strOutput = "html";
    String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportCustomerSatisfaction.jrxml";

    String strTitle = classInfo.name;

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_SUBTITLE", strTitle);

    for (int i = 0; i <= data.length - 1; i++) {
      //System.out.println("decathlon ID " + data[i].emRcOxylane);

      // Sending HTTP POST request
      HttpURLConnection hc;
      try {
        hc = createConnection(data[i].emRcOxylane);
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
        data[i].name2 = dataJsonObject.get("name2").toString();
        data[i].postalcode = dataJsonObject.get("rCZipcode").toString();
        data[i].mobileno = dataJsonObject.get("rCMobile").toString();
        data[i].landline = dataJsonObject.get("rCLandline").toString();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    // }
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom,
      String strDateTo) throws IOException, ServletException {

    System.out.println("Calling select inside printPageHtml");
    ReportCustomerSatisfactionData[] data = ReportCustomerSatisfactionData.select(this,
        Utility.getContext(this, vars, "#User_Client", "ReportCustomerSatisfaction"),
        Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportCustomerSatisfaction"),
        strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"));
    String strOutput = "xls";
    String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportCustomerSatisfaction.jrxml";

    String strTitle = classInfo.name;
    // response.setHeader("Content-disposition","inline;filename=RecuitmentJR.pdf");
    response.setHeader("Content-disposition", "inline;filename=ReportCustomerSatisfaction.xls");

    // response.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("REPORT_TITLE", strTitle);

    // Added for displaying lines in excel file
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    for (int i = 0; i <= data.length - 1; i++) {
      //System.out.println("decathlon ID " + data[i].emRcOxylane);

      // Sending HTTP POST request
      HttpURLConnection hc;
      try {
        hc = createConnection(data[i].emRcOxylane);
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
        data[i].name2 = dataJsonObject.get("name2").toString();
        data[i].postalcode = dataJsonObject.get("rCZipcode").toString();
        data[i].mobileno = dataJsonObject.get("rCMobile").toString();
        data[i].landline = dataJsonObject.get("rCLandline").toString();
        data[i].emailaddress = dataJsonObject.getString("rCEmail").toString();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);

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
       // + customerDBUName + "&pwd=" + customerDBPwd + "");
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
    return "ReportCustomerSatisfaction for displaying in excel format";
  } // end of getServletInfo() method
}
