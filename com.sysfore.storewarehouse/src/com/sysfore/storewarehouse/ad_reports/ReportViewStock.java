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
//import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;
import com.sysfore.fastnslowmovingrpt.report.SelectorUtilityData;
//import com.sysfore.fastnslowmovingrpt.report.TestData;

public class ReportViewStock extends HttpSecureAppServlet {

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      // BEGIN Parse parameters passed by the form
         String strDate = vars.getGlobalVariable("inpDate", "ReportStockValuation|date", DateTimeData
          .today(this));
         System.out.println("strDate: in Default " + strDate);

      String strProduct = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportViewStock|product", "", IsIDFilter.instance);
      vars.setSessionValue("ReportViewStock|product", "");
      System.out.println("strProduct: in XLS " + strProduct);

      String strBrand = vars.getInGlobalVariable("inpclBrand_IN", "ReportViewStock|clBrand",
          "", IsIDFilter.instance);
      vars.setSessionValue("ReportViewStock|clBrand", "");
      System.out.println("strBrand: in XLS " + strBrand);

      // String strmWarehouseId = vars.getStringParameter("inpmWarehouseId", "");
     /* String strmWarehouseId = vars.getGlobalVariable("inpmWarehouseId",
          "ReportViewStock|Warehouse", "");
      System.out.println("strmWarehouseId: in Default " + strmWarehouseId);

      String strMovementtype = vars.getGlobalVariable("inpemswMovementtypegm",
          "ReportViewStock|Movementtype", "");
      System.out.println("strMovementtype: in Default " + strMovementtype);

      String strBrand = vars.getGlobalVariable("inpclBrandId", "ReportViewStock|brand", "");
      System.out.println("strBrand: in default " + strBrand);*/

      printPageDataSheet(response, vars, strDate, strProduct, strBrand);

    } else if (vars.commandIn("XLS")) {
         String strDate = vars.getGlobalVariable("inpDate", "ReportStockValuation|date", DateTimeData
          .today(this));
         System.out.println("strDate: in Default " + strDate);

      String strProduct = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportViewStock|product", "", IsIDFilter.instance);
      vars.setSessionValue("ReportViewStock|product", "");
      System.out.println("strProduct: in XLS " + strProduct);

      String strBrand = vars.getInGlobalVariable("inpclBrand_IN", "ReportViewStock|clBrand",
          "", IsIDFilter.instance);
      vars.setSessionValue("ReportViewStock|clBrand", "");
      System.out.println("strBrand: in XLS " + strBrand);
      printPageXls(response, vars, strDate, strProduct, strBrand);

    } else
      pageError(response);
    // END Controller of actions
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDate, String strProduct, String strBrand) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    // BEGIN Retrieve invoice data for the selected timespan
    /*ReportViewStockData[] data = ReportViewStockData.select(this, strDateFrom, DateTimeData.nDaysAfter(this,        strDateTo, "1"), strProduct, strBrand);*/
      /*ReportViewStockData[] data = ReportViewStockData.select(this, strDate, strDate, strDate, strProduct, strBrand);
      System.out.println("data.length:  " + data.length);*/
    // END Retrieve invoice data for the selected timespan
    // BEGIN Initialize the UI template
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/storewarehouse/ad_reports/ReportViewStock").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.storewarehouse.ad_reports.ReportViewStock");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportViewStock.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportViewStock.html",
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
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportViewStock", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportViewStock");
    vars.removeMessage("ReportViewStock");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    // END Check, show & remove message
    log4j.debug("MESSAGE SET");

    // BEGIN Set parameters that need to be redisplayed on refresh

      xmlDocument.setParameter("date", strDate);
      xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
//    xmlDocument.setParameter("dateFrom", strDateFrom);
//    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
//    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
//    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));

    xmlDocument.setParameter("Product", strProduct);
    xmlDocument.setParameter("Brand", strBrand);

    xmlDocument.setData("reportMProductId_IN", "liststructure", SelectorUtilityData.selectMproduct(
        this, Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility.getContext(this,
            vars, "#User_Client", ""), strProduct));

    xmlDocument.setData("reportClBrandId_IN", "liststructure", SelectorUtilityData.selectBrand(
        this, vars.getOrg(), vars.getClient(), strBrand));

    log4j.info("Sample Display");
    System.out.println("in xml1:");

    System.out.println("in xml");
    // xmlDocument.setParameter("productDescription", ReportViewStockData.selectMproduct(this,
    // strProduct));

    /*
     * try { ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
     * "m_product_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
     * "ReportViewStock"), Utility.getContext(this, vars, "#User_Client",
     * "ReportViewStock"), 0); Utility.fillSQLParameters(this, vars, null, comboTableData,
     * "ReportViewStock", strProduct); xmlDocument.setData("reportm_Product_Id",
     * "liststructure", comboTableData.select(false)); comboTableData = null; } catch (Exception ex)
     * { throw new ServletException(ex); }
     */

   /* try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "m_warehouse_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportViewStock"),
          Utility.getContext(this, vars, "#User_Client", "ReportViewStock"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportViewStock",
          strmWarehouseId);
      xmlDocument.setData("reportM_WAREHOUSEID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "MovementType",
          "63C70C976A804530BF81BCEDEDBE586E", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "ReportViewStock"), Utility.getContext(this, vars,
              "#User_Client", "ReportViewStock"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportViewStock",
          strMovementtype);
      xmlDocument.setData("reportsw_Movementtype", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }*/// "SW_AllMovementType"

    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    log4j.debug("STRUCTURE SET");
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strDate, String strProduct, String strBrand, String strOutput)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportViewStockData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();
    try {
      System.out.println("Calling select inside printPageHtml");
      data = ReportViewStockData.select(this, strDate, strDate, strDate, strProduct, strBrand);
    } catch (ServletException ex) {
      ex.printStackTrace();
      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
    strConvRateErrorMsg = myMessage.getMessage();

    // String strOutput = "html";
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportViewStock.jrxml";

    String strTitle = classInfo.name;

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_SUBTITLE", strTitle);

    renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    // }
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDate, String strProduct, String strBrand) throws IOException, ServletException {

    System.out.println("Calling select inside printPageXls");
    System.out.println("strProduct" + strProduct);

    ReportViewStockData[] data = ReportViewStockData.select(this, strDate, strDate, strDate, strProduct, strBrand);

    String strOutput = "xls";
    System.out.println("after fetching data" + data);
    System.out.println("after fetching data:length" + data.length);
    String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportViewStock.jrxml";
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

  }

  public String getServletInfo() {
    return "ReportViewStock for displaying in excel format";
  } // end of getServletInfo() method
}
