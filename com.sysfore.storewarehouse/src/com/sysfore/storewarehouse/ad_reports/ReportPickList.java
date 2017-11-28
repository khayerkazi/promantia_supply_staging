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
 * Contributor(s):  __Johnson____________________________________.
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
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportPickList extends HttpSecureAppServlet {

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      // BEGIN Parse parameters passed by the form
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportPickList|dateFrom");
      System.out.println("strDateFrom: in Default " + strDateFrom);

      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportPickList|dateTo");
      System.out.println("strDateTo: in Default " + strDateTo);

      String strmWarehouseId = vars.getGlobalVariable("inpmWarehouseId", "ReportPickList|Warehouse", "");
      System.out.println("strmWarehouseId: in Default " + strmWarehouseId);
      
      String strmDocumentNo = vars.getGlobalVariable("inDocumentNo", "ReportPickList|documentNo", "");
      System.out.println("strmDocumentNo: in Default " + strmDocumentNo);

	String warehouse=vars.getWarehouse();    

      printPageDataSheet(response, vars, strDateFrom, strDateTo, warehouse,strmDocumentNo );

    } else if (vars.commandIn("find")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportPickList|dateFrom");
      System.out.println("strDateFrom: in Find " + strDateFrom);

      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportPickList|dateTo");
      System.out.println("strDateTo: in Find " + strDateTo);

      
      // String strmWarehouseId = vars.getStringParameter("inpmWarehouseId", "");
      String strmWarehouseId = vars.getRequestGlobalVariable("inpmWarehouseId", "ReportPickList|Warehouse");
      System.out.println("strmWarehouseId: in Find " + strmWarehouseId);

     String strmDocumentNo = vars.getGlobalVariable("inDocumentNo", "ReportPickList|documentNo", "");
      System.out.println("strmDocumentNo: in Find " + strmDocumentNo);

      setHistoryCommand(request, "FIND");
      printPageHtml(request, response, vars, strDateFrom, strDateTo, strmWarehouseId, strmDocumentNo, "html");
    } else if (vars.commandIn("XLS")) {

      String warehouse=vars.getWarehouse();    	
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportPickList|dateFrom");
      System.out.println("strDateFrom: in XLS " + strDateFrom);

      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportPickList|dateTo");
      System.out.println("strDateTo: in XLS " + strDateTo);

      // String strmWarehouseId = vars.getStringParameter("inpmWarehouseId", "");
      String strmWarehouseId = vars.getRequestGlobalVariable("inpmWarehouseId", "ReportPickList|Warehouse");
      System.out.println("strmWarehouseId: in XLS " + strmWarehouseId);

      String strmDocumentNo = vars.getGlobalVariable("inDocumentNo", "ReportPickList|documentNo", "");
      System.out.println("strmDocumentNo: in XLS " + strmDocumentNo);

      printPageXls(response, vars, strDateFrom, strDateTo, warehouse, strmDocumentNo);

    } else
      pageError(response);
    // END Controller of actions
  }

 void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strmWarehouseId, String strmDocumentNo) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    // BEGIN Retrieve invoice data for the selected timespan
    /*ReportStockMovementData[] data = ReportStockMovementData.select(this, Utility.getContext(this,
        vars, "#User_Client", "ReportStockMovement"), Utility.getContext(this, vars,
        "#AccessibleOrgTree", "ReportStockMovement"), strDateFrom, DateTimeData.nDaysAfter(this,
        strDateTo, "1"), strProduct, strmWarehouseId, strMovementtype, strBrand);*/
    // END Retrieve invoice data for the selected timespan

    // BEGIN Initialize the UI template
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/storewarehouse/ad_reports/ReportPickList").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.storewarehouse.ad_reports.ReportPickList");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportPickList.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportPickList.html",
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
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportPickList", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportPickList");
    vars.removeMessage("ReportPickList");
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

    // xmlDocument.setParameter("Product", strProduct);
    xmlDocument.setParameter("Warehouse", strmWarehouseId);
    xmlDocument.setParameter("documentNo", strmDocumentNo);
    log4j.info("Sample Display");
    System.out.println("in xml1:");

   
    // xmlDocument.setParameter("productDescription", ReportStockMovementData.selectMproduct(this,
    // strProduct));

    /*
     * try { ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
     * "m_product_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
     * "ReportStockMovement"), Utility.getContext(this, vars, "#User_Client",
     * "ReportStockMovement"), 0); Utility.fillSQLParameters(this, vars, null, comboTableData,
     * "ReportStockMovement", strProduct); xmlDocument.setData("reportm_Product_Id",
     * "liststructure", comboTableData.select(false)); comboTableData = null; } catch (Exception ex)
     * { throw new ServletException(ex); }
     */

   
    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    log4j.debug("STRUCTURE SET");
    out.println(xmlDocument.print());
    out.close();
  }

 void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strDateFrom, String strDateTo,
      String strmWarehouseId, String strmDocumentNo, String strOutput)
      throws IOException, ServletException {

System.out.println(strDateTo + "  << date to is here");    
System.out.println(strDateFrom + "  << date from is here");
    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEHTMLSHEET");
   //Main Copy ReportPickListData[] data = ReportPickListData.select(this,strDateFrom,strDateTo, strmWarehouseId, strmDocumentNo);

     ReportPickListData[] data = ReportPickListData.select(this,strmWarehouseId,strDateFrom,DateTimeData.nDaysAfter(this,
        strDateTo, "1"),strmDocumentNo);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/storewarehouse/ad_reports/ReportPickList").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");
    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.storewarehouse.ad_reports.ReportPickList");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportPickList.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportPickList.html",
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
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportPickList", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportPickList");
    vars.removeMessage("ReportPickList");
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
    // xmlDocument.setParameter("Product", strProduct);

    xmlDocument.setParameter("Warehouse", strmWarehouseId);
    xmlDocument.setParameter("documentNo", strmDocumentNo);
   
    log4j.info("Sample Display");
    System.out.println("in xml1:");
    
   /* try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "m_locator_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportPickList"),
          Utility.getContext(this, vars, "#User_Client", "ReportPickList"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportPickList",
          strmWarehouseId);
      xmlDocument.setData("reportM_LOCATORID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
   */
    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");
    log4j.debug("STRUCTURE SET");
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
    /*  if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportStockMovementData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();
    try {
      System.out.println("Calling select inside printPageHtml");
      data = ReportStockMovementData.select(this, Utility.getContext(this, vars, "#User_Client",
          "ReportStockMovement"), Utility.getContext(this, vars, "#AccessibleOrgTree",
          "ReportStockMovement"), strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"),
          strProduct, strmWarehouseId, strMovementtype, strBrand);
    } catch (ServletException ex) {
      ex.printStackTrace();
      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
    strConvRateErrorMsg = myMessage.getMessage();

    // String strOutput = "html";
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportStockMovement.jrxml";

    String strTitle = classInfo.name;

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_SUBTITLE", strTitle);

    renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    */ 
    
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom,
      String strDateTo, String strmWarehouseId, String strmDocumentNo) throws IOException, ServletException {
    
    
    String dateFrom = strDateFrom;
    String dateTo = strDateTo;
    String documentNo = strmDocumentNo;
    System.out.println("Calling select inside printPageXls");
	System.out.println("From Date : " + dateFrom);
    System.out.println("To Date : " + dateTo);
    System.out.println("Document No : " + documentNo);    
	    System.out.println("strmWarehouseId : " + strmWarehouseId);

   //  System.out.println("strProduct" + strProduct);
   // Main Copy ReportPickListData[] data = ReportPickListData.select(this,strDateFrom,strDateTo,strmWarehouseId, strmDocumentNo);
		    ReportPickListData[] data = ReportPickListData.select(this,strmWarehouseId ,dateFrom,DateTimeData.nDaysAfter(this,dateTo, "1"),documentNo);
    String strOutput = "xls";
    System.out.println("after fetching data" + data);
    System.out.println("after fetching data" + data.length);
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportPickList.jrxml";
    System.out.println("after fetching strReportName" + strReportName);
    String strTitle = classInfo.name;
    // response.setHeader("Content-disposition","inline;filename=RecuitmentJR.pdf");
    response.setHeader("Content-disposition", "inline;filename=ReportPickList_new.xls");
    dateFrom = null;
    dateTo = null;
    documentNo = null;
    System.out.println("From Date : " + dateFrom);
    System.out.println("To Date : " + dateTo);
    System.out.println("Document No : " + documentNo);
    // response.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("REPORT_TITLE", strTitle);

    // Added for displaying lines in excel file
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);

  }

  public String getServletInfo() {
    return "ReportPickList for displaying in excel format";
  } // end of getServletInfo() method
}
