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

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_combos.OrganizationComboData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;
//Added for displaying lines in excel file
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

public class ReportStockValuation extends HttpSecureAppServlet {

  public void doPost (HttpServletRequest request, HttpServletResponse response)
        throws IOException,ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {

      // BEGIN Parse parameters passed by the form inpDate

        String strDate = vars.getGlobalVariable("inpDate", "ReportStockValuation|date", DateTimeData.today(this));
        System.out.println("strDate: in Default " + strDate);   
        
	String strProduct = vars.getGlobalVariable("inpmProductId","ReportStockValuation|product", "");
 	System.out.println("strProduct: in default " + strProduct);

	String strModel = vars.getGlobalVariable("inpclModelId","ReportStockValuation|model", "");
	System.out.println("strModel: in Default " + strModel);

	String strBrand = vars.getGlobalVariable("inpclBrandId","ReportStockValuation|brand", "");
	System.out.println("strBrand: in default " + strBrand);

	String strWarehouse = vars.getGlobalVariable("inpmWarehouseId","ReportStockValuation|warehouse", "");
	System.out.println("strWarehouse: in default " + strWarehouse);

 	String strLocatorType = vars.getGlobalVariable("inpemObwhsType", "ReportStockValuation|locatortype","");
	System.out.println("strLocatorType in default" + strLocatorType);

	printPageDataSheet(response, vars, strDate, strProduct, strModel, strBrand, strWarehouse, strLocatorType);

    } else if (vars.commandIn("FIND")){

        String strDate = vars.getGlobalVariable("inpDate", "ReportStockValuation|date", DateTimeData.today(this));
        System.out.println("strDate: in PRINT_HTML " + strDate);

 	String strProduct = vars.getRequestGlobalVariable("inpmProductId","ReportStockValuation|product");
	System.out.println("strProduct: in Find " + strProduct);
	
	String strBrand = vars.getRequestGlobalVariable("inpclBrandId","ReportStockValuation|brand");
	System.out.println("strBrand: in Find " + strBrand);

	String strModel = vars.getRequestGlobalVariable("inpclModelId","ReportStockValuation|model");
	System.out.println("strModel: in FIND " + strModel);

	String strWarehouse = vars.getGlobalVariable("inpmWarehouseId","ReportStockValuation|warehouse", "");
	System.out.println("strWarehouse: in FIND " + strWarehouse);
 
 	String strLocatorType = vars.getGlobalVariable("inpemObwhsType", "ReportStockValuation|locatortype","");
 	System.out.println("strLocatorType in FIND" + strLocatorType);

	printPageDataSheet(response, vars, strDate, strProduct, strModel, strBrand, strWarehouse, strLocatorType);
	}

else if (vars.commandIn("XLS")){

    	String strDate = vars.getGlobalVariable("inpDate", "ReportStockValuation|date", DateTimeData.today(this));
	System.out.println("strDate: in XLS " + strDate);

	String strProduct = vars.getRequestGlobalVariable("inpmProductId","ReportStockValuation|product");
	System.out.println("strProduct: in XLS " + strProduct);

	String strBrand = vars.getRequestGlobalVariable("inpclBrandId","ReportStockValuation|brand");
	System.out.println("strBrand: in FiXLSnd " + strBrand);
      
	String strModel = vars.getRequestGlobalVariable("inpclModelId","ReportStockValuation|model");
	System.out.println("strModel: in XLS " + strModel);

	String strWarehouse = vars.getGlobalVariable("inpmWarehouseId","ReportStockValuation|warehouse", "");
 	System.out.println("strWarehouse: in XLS " + strWarehouse);
 
 	String strLocatorType = vars.getGlobalVariable("inpemObwhsType", "ReportStockValuation|locatortype","");
 	System.out.println("strLocatorType in XLS" + strLocatorType);
 
	printPageXls( response, vars, strDate, strProduct, strModel, strBrand, strWarehouse, strLocatorType);

    }  else pageError(response);
    // END Controller of actions
}


  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strDate, String strProduct, String strModel, String strBrand, String strWarehouse, String strLocatorType)
    throws IOException, ServletException {

    if (log4j.isDebugEnabled()) log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    
	OBError myMessage = vars.getMessage("ReportStockValuation");
	ReportStockValuationData[] data = null;
	myMessage = new OBError();

  if (vars.commandIn("FIND")){
      try {
        String currentWareHouse = vars.getWarehouse();
        System.out.println("Warehouse ID Passed "+vars.getWarehouse());
        System.out.println("Calling select inside printPageDatasheet");

	data = ReportStockValuationData.select(this, DateTimeData.nDaysAfter(this, strDate, "1"), strWarehouse, strLocatorType, strProduct, strModel, strBrand);

	System.out.println("data of find" + data.length);
	System.out.println("data of item" + data[0].itemcode);

      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
      }
    }

	XmlDocument xmlDocument=null;
	xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/storewarehouse/ad_reports/ReportStockValuation").createXmlDocument();
    // END Initialize the UI template
    
	log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars, "org.openbravo.erpCommon.ad_reports.ReportStockValuation");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());

      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportStockValuation.html", classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());

      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportStockValuation.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());

    } catch (Exception ex) {
      throw new ServletException(ex);
    }
	xmlDocument.setParameter("calendar", vars.getLanguage().substring(0,2));
	xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
	xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");
    // END Set up and pass core UI parameters to the template
   
	 log4j.debug("UI parameters set");

    // BEGIN Prepare and set empty toolbar
   	 ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportStockValuation", false, "", "", "",false, "ad_reports",  strReplaceWith, false,  true);
    	toolbar.prepareSimpleToolBarTemplate();
    	xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    	log4j.debug("TOOLBAR SET");


    vars.removeMessage("ReportStockValuation");
    if (myMessage!=null) {
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
      xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("Brand", strBrand);
      xmlDocument.setParameter("Warehouse", strWarehouse);
      xmlDocument.setParameter("Locatortype", strLocatorType);

      xmlDocument.setParameter("paramProductId", strProduct);
      xmlDocument.setParameter("paramModelId", strModel);

    log4j.info("Sample Display");

try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_brand_id","", "", Utility.getContext(this, vars,"#AccessibleOrgTree", "ReportStockValuation"),Utility.getContext(this, vars, "#User_Client", "ReportStockValuation"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportStockValuation",strBrand);
      xmlDocument.setData("reportcl_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "m_warehouse_id","", "", Utility.getContext(this, vars,"#AccessibleOrgTree", "ReportStockValuation"),Utility.getContext(this, vars, "#User_Client", "ReportStockValuation"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportStockValuation",strWarehouse);
      xmlDocument.setData("reportm_Warehouse_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

try {
    ComboTableData comboTableData = new ComboTableData(vars, this, "17", "Em_Obwhs_Type","20F76540A20B4E21AF30C3709329B00B", "", Utility.getContext(this, vars,"#AccessibleOrgTree", "ReportStockValuation"), Utility.getContext(this, vars,"#User_Client", "ReportStockValuation"), 0);
    Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportStockValuation", strLocatorType);
    xmlDocument.setData("reportm_locatortype", "liststructure", comboTableData.select(false));
    comboTableData = null;
  } catch (Exception ex) {
    throw new ServletException(ex);
  }

    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

void printPageHtml(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars, String strDate,String strProduct, String strModel, String strBrand, String strWarehouse, String strLocatorType, String strOutput) throws IOException, ServletException {

  if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportStockValuationData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();

    try {
        System.out.println("Calling select inside printPageHtml");
        System.out.println("Warehouse ID Passed "+vars.getWarehouse());
  	
	data = ReportStockValuationData.select(this, DateTimeData.nDaysAfter(this, strDate, "1"), strWarehouse, strLocatorType, strProduct, strModel, strBrand);

  	System.out.println("data of find" + data.length);
  
   } catch (ServletException ex) {
      ex.printStackTrace();
      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
	strConvRateErrorMsg = myMessage.getMessage();
	String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportStockValuation.jrxml";
	String strTitle =classInfo.name;

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_SUBTITLE", strTitle);
      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
}


void printPageXls( HttpServletResponse response, VariablesSecureApp vars,String  strDate, String strProduct, String strModel, String strBrand, String strWarehouse, String strLocatorType) throws IOException, ServletException {

	System.out.println("Warehouse ID Passed "+vars.getWarehouse());
	System.out.println("Calling select inside printPageXls");

	ReportStockValuationData[] data = ReportStockValuationData.select(this, DateTimeData.nDaysAfter(this, strDate, "1"), strWarehouse, strLocatorType, strProduct, strModel, strBrand);

	System.out.println("data of find" + data.length);
	String strOutput = "xls";

      	System.out.println("after fetching data" + data);
      	String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/ReportStockValuation.jrxml";

	System.out.println("after fetching strReportName" + strReportName);
      	String strTitle =classInfo.name;

     	response.setHeader("Content-disposition","inline;filename=ReportStockValuation.xls");
      	HashMap<String, Object> parameters = new HashMap<String, Object>();
      	parameters.put("REPORT_TITLE", strTitle);

	//Added for displaying lines in excel file
	HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
	parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
	renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);

  }

  public String getServletInfo() {
    return "ReportStockValuation for displaying in excel format";
  } 
}
