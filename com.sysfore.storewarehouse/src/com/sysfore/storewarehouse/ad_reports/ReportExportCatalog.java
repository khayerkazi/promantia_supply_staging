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

package com.sysfore.storewarehouse.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportExportCatalog extends HttpSecureAppServlet {

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      // BEGIN Parse parameters passed by the form
      String strModelName = vars.getStringParameter("inpmodelName", "");
      System.out.println("strModelName: in default " + strModelName);
      String strModelCode = vars.getStringParameter("inpmodelCode", "");
      System.out.println("strModelCode: in default " + strModelCode);

      String strBrand = vars.getStringParameter("inpclBrandId", "");
      System.out.println("strBrand: in default " + strBrand);
      String strLifestage = vars.getStringParameter("inpemclLifestage", "");
      System.out.println("strLifestage: in default " + strLifestage);

      printPageDataSheet(response, vars, strModelName, strModelCode, strBrand, strLifestage);

    } else if (vars.commandIn("XLS")) {

      String strModelName = vars.getStringParameter("inpmodelName", "");
      System.out.println("strModelName: in default " + strModelName);
      String strModelCode = vars.getStringParameter("inpmodelCode", "");
      System.out.println("strModelCode: in default " + strModelCode);

      String strBrand = vars.getStringParameter("inpclBrandId", "");
      System.out.println("strBrand: in default " + strBrand);
      String strLifestage = vars.getStringParameter("inpemclLifestage", "");
      System.out.println("strLifestage: in default " + strLifestage);

      printPageXls(response, vars, strModelName, strModelCode, strBrand, strLifestage);

    } else
      pageError(response);
    // END Controller of actions
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strModelName, String strModelCode, String strBrand, String strLifestage) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    // BEGIN Retrieve invoice data for the selected timespan
    /*ReportExportCatalogData[] data = ReportExportCatalogData.select(this, strDateFrom, DateTimeData.nDaysAfter(this,        strDateTo, "1"), strProduct, strBrand);*/
      /*ReportExportCatalogData[] data = ReportExportCatalogData.select(this, strModelName, strModelCode, strBrand, strLifestage);
      System.out.println("data.length:  " + data.length);

      ReportExportCatalogData[] data1 = ReportExportCatalogData.selectOrg(this, strModelName, strModelCode, strBrand, strLifestage, vars.getOrg());
      System.out.println("data1.length:  " + data1.length);*/
    // END Retrieve invoice data for the selected timespan
    // BEGIN Initialize the UI template
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/storewarehouse/ad_reports/ReportExportCatalog").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.storewarehouse.ad_reports.ReportExportCatalog");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportExportCatalog.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportExportCatalog.html",
          strReplaceWith);
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
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportExportCatalog", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportExportCatalog");
    vars.removeMessage("ReportExportCatalog");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    // END Check, show & remove message
    log4j.debug("MESSAGE SET");

    // BEGIN Set parameters that need to be redisplayed on refresh

    xmlDocument.setParameter("ModelName", strBrand);
    xmlDocument.setParameter("ModelCode", strLifestage);
    xmlDocument.setParameter("Brand", strBrand);
    xmlDocument.setParameter("Lifestage", strLifestage);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_brand_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportExportCatalog"),
          Utility.getContext(this, vars, "#User_Client", "ReportExportCatalog"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportExportCatalog",
          strBrand);
      xmlDocument.setData("reportcl_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    };

    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strLifestage);
      try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "EM_Cl_Lifestage",
          "8E405997F12A4DB79252DDF3CAC421D6", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "ReportExportCatalog"), Utility.getContext(this, vars,
              "#User_Client", "ReportExportCatalog"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportExportCatalog", strLifestage);
      xmlDocument.setData("reportEM_cl_LIFESTAGE", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    log4j.info("Sample Display");
    System.out.println("in xml:");
    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    log4j.debug("STRUCTURE SET");
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strModelName, String strModelCode, String strBrand, String strLifestage, 
String strOutput)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportExportCatalogData[] data = null;
    ReportExportCatalogData[] data1 = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();
    String strOrg = vars.getOrg();
    System.out.println("strOrg:  " + strOrg);
    
    String strClient = vars.getClient();
    System.out.println("strClient:  " + strClient);
    
    if (strOrg.equals(0)){
    try {
      System.out.println("Calling select inside printPageHtml");
      data = ReportExportCatalogData.select(this, strModelName, strModelCode, strBrand,  strLifestage, strClient);
    } catch (ServletException ex) {
      ex.printStackTrace();
      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
   }else{
    try {
      System.out.println("Calling select inside printPageHtml");
      data1 = ReportExportCatalogData.selectOrg(this, strModelName, strModelCode, strBrand,  strLifestage, strOrg, strClient);
    } catch (ServletException ex) {
      ex.printStackTrace();
      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
   }

    strConvRateErrorMsg = myMessage.getMessage();

    // String strOutput = "html";
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportExportCatalog.jrxml";

    String strTitle = classInfo.name;

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_SUBTITLE", strTitle);

    renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    // }
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strModelName, String strModelCode, String strBrand, String strLifestage) throws IOException, ServletException {

    System.out.println("Calling select inside printPageXls");
    String strOrg = vars.getOrg();
    System.out.println("strOrg:  " + strOrg); 
    
    String strClient = vars.getClient();
    System.out.println("strClient:  " + strClient);
    
    if (strOrg.equals(0)){
    ReportExportCatalogData[] data = ReportExportCatalogData.select(this, strModelName, strModelCode, strBrand, strLifestage, strClient);
    String strOutput = "xls";
    System.out.println("after fetching data" + data);
    System.out.println("after fetching data:length" + data.length);
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportExportCatalog.jrxml";
    System.out.println("after fetching strReportName" + strReportName);
    String strTitle = classInfo.name;
    // response.setHeader("Content-disposition","inline;filename=RecuitmentJR.pdf");
    response.setHeader("Content-disposition", "inline;filename=StockMpvement_new.xls");

    // response.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("REPORT_TITLE", strTitle);

    // Added for displaying lines in excel file
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
   }else{
    ReportExportCatalogData[] data1 = ReportExportCatalogData.selectOrg(this, strModelName, strModelCode, strBrand, strLifestage, strOrg, strClient);
    String strOutput = "xls";
    System.out.println("after fetching data1" + data1);
    System.out.println("after fetching data1:length" + data1.length);
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportExportCatalog.jrxml";
    System.out.println("after fetching strReportName" + strReportName);
    String strTitle = classInfo.name;
    // response.setHeader("Content-disposition","inline;filename=RecuitmentJR.pdf");
    response.setHeader("Content-disposition", "inline;filename=StockMpvement_new.xls");

    // response.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("REPORT_TITLE", strTitle);

    // Added for displaying lines in excel file
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    renderJR(vars, response, strReportName, strOutput, parameters, data1, parametersexport);
}
  }

  public String getServletInfo() {
    return "ReportExportCatalog for displaying in excel format";
  } // end of getServletInfo() method
}
