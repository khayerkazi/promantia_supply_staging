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

import org.apache.commons.lang.ArrayUtils;

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

public class DCReport extends HttpSecureAppServlet {

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
	ServletException {

		VariablesSecureApp vars = new VariablesSecureApp(request);

		if (vars.commandIn("DEFAULT")) {
			// BEGIN Parse parameters passed by the form
			String strmDocumentNo = vars.getGlobalVariable("inDocumentNo", "DCReport|documentNo", "");
			System.out.println("DEFAULT-strmDocumentNo:- " + strmDocumentNo);
			printPageDataSheet(response, vars, strmDocumentNo);

		} 
		else if (vars.commandIn("find")) {
			String strmDocumentNo = vars.getGlobalVariable("inDocumentNo", "DCReport|documentNo", "");
			System.out.println("strmDocumentNo: in Find " + strmDocumentNo);
			setHistoryCommand(request, "FIND");
			printPageHtml(request, response, vars, strmDocumentNo, "html");

		} 
		else if (vars.commandIn("XLS")) {
			String strmDocumentNo = vars.getGlobalVariable("inDocumentNo", "DCReport|documentNo", "");
			System.out.println("strmDocumentNo: in XLS " + strmDocumentNo);
			printPageXls(response, vars, strmDocumentNo);

		} 
		else
			pageError(response);
		// END Controller of actions
  	}

	void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strmDocumentNo) 
					throws IOException, ServletException {

		if (log4j.isDebugEnabled())
			log4j.debug("BEGIN PRINTPAGEDATASHEET");
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();

		// BEGIN Initialize the UI template
		XmlDocument xmlDocument = null;
		xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/storewarehouse/ad_reports/DCReport").createXmlDocument();
		// END Initialize the UI template
		log4j.debug("XML LOADED");

		// BEGIN Set up and pass core UI parameters to the template
		try {
			WindowTabs tabs = new WindowTabs(this, vars, "com.sysfore.storewarehouse.ad_reports.DCReport");
			xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
			xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
			xmlDocument.setParameter("childTabContainer", tabs.childTabs());
			xmlDocument.setParameter("theme", vars.getTheme());
			NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "DCReport.html",
			classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
			xmlDocument.setParameter("navigationBar", nav.toString());
			LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "DCReport.html", strReplaceWith);
			xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
		} 
		catch (Exception ex) {
			throw new ServletException(ex);
		}
		xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
		xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
		xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");

		// END Set up and pass core UI parameters to the template
		log4j.debug("UI parameters set");

		// BEGIN Prepare and set empty toolbar
		ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "DCReport", false, "", "",
		"", false, "ad_reports", strReplaceWith, false, true);
		toolbar.prepareSimpleToolBarTemplate();
		xmlDocument.setParameter("toolbar", toolbar.toString());

		// END Prepare empty toolbar
		log4j.debug("TOOLBAR SET");

		OBError myMessage = vars.getMessage("DCReport");
		vars.removeMessage("DCReport");
		if (myMessage != null) {
			xmlDocument.setParameter("messageType", myMessage.getType());
			xmlDocument.setParameter("messageTitle", myMessage.getTitle());
			xmlDocument.setParameter("messageMessage", myMessage.getMessage());
		}
		// END Check, show & remove message
		log4j.debug("MESSAGE SET");

		// BEGIN Set parameters that need to be redisplayed on refresh
		xmlDocument.setParameter("documentNo", strmDocumentNo);
		log4j.info("Sample Display");
		System.out.println("Inside XML");

		// END Set parameters that need to be redisplayed on refresh
		log4j.debug("FILTER PARAMETERS SET");

		log4j.debug("STRUCTURE SET");
		out.println(xmlDocument.print());
		out.close();
	}

	void printPageHtml(HttpServletRequest request, HttpServletResponse response,
	VariablesSecureApp vars, String strmDocumentNo, String strOutput)
	throws IOException, ServletException {

		if (log4j.isDebugEnabled())
			log4j.debug("BEGIN PRINTPAGEHTMLSHEET");

		DCReportData[] data = DCReportData.select(this,strmDocumentNo);

		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();
		XmlDocument xmlDocument = null;
		xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/storewarehouse/ad_reports/DCReport").createXmlDocument();
		// END Initialize the UI template
		log4j.debug("XML LOADED");
		// BEGIN Set up and pass core UI parameters to the template
		try {
			WindowTabs tabs = new WindowTabs(this, vars,
			"com.sysfore.storewarehouse.ad_reports.DCReport");
			xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
			xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
			xmlDocument.setParameter("childTabContainer", tabs.childTabs());
			xmlDocument.setParameter("theme", vars.getTheme());
			NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "DCReport.html",
			classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
			xmlDocument.setParameter("navigationBar", nav.toString());
			LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "DCReport.html",
			strReplaceWith);
			xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
		} 
		catch (Exception ex) {
			throw new ServletException(ex);
		}
		xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
		xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
		xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");
		// END Set up and pass core UI parameters to the template
		log4j.debug("UI parameters set");

		// BEGIN Prepare and set empty toolbar
		ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "DCReport", false, "", "", "", false, "ad_reports", strReplaceWith, false, true);
		toolbar.prepareSimpleToolBarTemplate();
		xmlDocument.setParameter("toolbar", toolbar.toString());
		// END Prepare empty toolbar
		log4j.debug("TOOLBAR SET");

		OBError myMessage = vars.getMessage("DCReport");
		vars.removeMessage("DCReport");
		if (myMessage != null) {
			xmlDocument.setParameter("messageType", myMessage.getType());
			xmlDocument.setParameter("messageTitle", myMessage.getTitle());
			xmlDocument.setParameter("messageMessage", myMessage.getMessage());
		}
		// END Check, show & remove message
		log4j.debug("MESSAGE SET");

		// BEGIN Set parameters that need to be redisplayed on refresh

		// xmlDocument.setParameter("Jvacancy",strJvacancy);

		xmlDocument.setParameter("documentNo", strmDocumentNo);

		log4j.info("Sample Display");
		System.out.println("in xml1:");


		// END Set parameters that need to be redisplayed on refresh
		log4j.debug("FILTER PARAMETERS SET");
		log4j.debug("STRUCTURE SET");
		xmlDocument.setData("structure1", data);
		out.println(xmlDocument.print());
		out.close();
	}

	void printPageXls(HttpServletResponse response, VariablesSecureApp vars,  String strmDocumentNo) throws IOException, ServletException {

		System.out.println("Inside printPageXls()");
		String documentNo = strmDocumentNo;
		String finalDocumentNo = "";
		if(documentNo.contains(",")) {
			String docArr[] = documentNo.split(",");
			for(int i=0;i<docArr.length;i++) {
				finalDocumentNo = finalDocumentNo + "'" + docArr[i] + "'";
				if(i < 	docArr.length-1) {
					finalDocumentNo = finalDocumentNo + ", ";
				}
			}
		}
		else {
			finalDocumentNo = "'" + documentNo + "'";
		}
		/*DCReportData[] data=null; 
		if(documentNo.contains(",")) {
			String docArr[] = documentNo.split(",");
			data = new DCReportData[docArr.length];
			for(int i=0;i<docArr.length;i++) {
				DCReportData[] tempData = DCReportData.select(this,docArr[i]);
				data = (DCReportData[]) ArrayUtils.addAll(data, tempData);
			}
		}
		else {
			data = DCReportData.select(this,documentNo);
			System.out.println("Document No:" + documentNo);
		}*/
		DCReportData[] data = DCReportData.select(this,finalDocumentNo);
		//System.out.println("Document No:" + documentNo);
		String strOutput = "xls";
		System.out.println("After fetching data:" + data);
		System.out.println("Data Length:" + data.length);
		String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/DCReport.jrxml";
		String strTitle = classInfo.name;
		response.setHeader("Content-disposition","inline;filename=DCReport_new.xls");
		documentNo = null;

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("REPORT_TITLE", strTitle);

		// Added for displaying lines in excel file
		HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
		parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
		try {
			renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String getServletInfo() {
		return "DCReport for displaying in excel format";
		// end of getServletInfo() method
	} 
}
