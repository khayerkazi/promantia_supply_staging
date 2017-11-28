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

package in.nous.creditnote.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportCreditNote extends HttpSecureAppServlet {

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      // BEGIN Parse parameters passed by the form
      String strDateFrom = vars
          .getRequestGlobalVariable("inpDateFrom", "ReportCreditNote|dateFrom");
      System.out.println("strDateFrom: in Default " + strDateFrom);

      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportCreditNote|dateTo");
      System.out.println("strDateTo: in Default " + strDateTo);

      printPageDataSheet(response, vars, strDateFrom, strDateTo);

    } else if (vars.commandIn("find")) {
      String strDateFrom = vars
          .getRequestGlobalVariable("inpDateFrom", "ReportCreditNote|dateFrom");
      System.out.println("strDateFrom: in Find " + strDateFrom);

      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportCreditNote|dateTo");
      System.out.println("strDateTo: in Find " + strDateTo);

      setHistoryCommand(request, "FIND");
      printPageHtml(request, response, vars, strDateFrom, strDateTo, "html");
    } else if (vars.commandIn("XLS")) {

      String strDateFrom = vars
          .getRequestGlobalVariable("inpDateFrom", "ReportCreditNote|dateFrom");
      System.out.println("strDateFrom: in XLS " + strDateFrom);

      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportCreditNote|dateTo");
      System.out.println("strDateTo: in XLS " + strDateTo);

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

    // BEGIN Initialize the UI template
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate("in/nous/creditnote/ad_reports/ReportCreditNote")
        .createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "in.nous.creditnote.ad_reports.ReportCreditNote");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportCreditNote.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportCreditNote.html",
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
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportCreditNote", false, "", "", "",
        false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportCreditNote");
    vars.removeMessage("ReportCreditNote");
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
    System.out.println("in xml1:");

    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    log4j.debug("STRUCTURE SET");
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strDateFrom, String strDateTo, String strOutput)
      throws IOException, ServletException {

    System.out.println(strDateTo + "  << date to is here");
    System.out.println(strDateFrom + "  << date from is here");
    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEHTMLSHEET");
    // Main Copy ReportPickListData[] data = ReportPickListData.select(this,strDateFrom,strDateTo,
    // strmWarehouseId, strmDocumentNo);
    String strClient = vars.getClient();
    ReportCreditNoteData[] data = ReportCreditNoteData.select(this, strDateFrom,
        DateTimeData.nDaysAfter(this, strDateTo, "1"), strClient);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate("in/nous/creditnote/ad_reports/ReportCreditNote")
        .createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");
    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "in.nous.creditnote.ad_reports.ReportCreditNote");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportCreditNote.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportCreditNote.html",
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
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportCreditNote", false, "", "", "",
        false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("ReportCreditNote");
    vars.removeMessage("ReportCreditNote");
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
    System.out.println("in xml1:");

    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");
    log4j.debug("STRUCTURE SET");
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();

  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom,
      String strDateTo) throws IOException, ServletException {

    String dateFrom = strDateFrom;
    String dateTo = strDateTo;
    System.out.println("Inside Nous module");
    System.out.println("Calling select inside printPageXls");
    System.out.println("From Date : " + dateFrom);
    System.out.println("To Date : " + dateTo);
    String strClient = vars.getClient();
    ReportCreditNoteData[] data = ReportCreditNoteData.select(this, strDateFrom,
        DateTimeData.nDaysAfter(this, strDateTo, "1"), strClient);
    String strOutput = "xls";
    System.out.println("after fetching data" + data);
    System.out.println("after fetching data" + data.length);
    String strReportName = "@basedesign@/in/nous/creditnote/ad_reports/ReportCreditNote.jrxml";
    System.out.println("after fetching strReportName" + strReportName);
    String strTitle = classInfo.name;
    response.setHeader("Content-disposition", "inline;filename=ReportCreditNote.xls");
    dateFrom = null;
    dateTo = null;
    System.out.println("From Date : " + dateFrom);
    System.out.println("To Date : " + dateTo);

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("REPORT_TITLE", strTitle);

    // Added for displaying lines in excel file
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

    renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);

  }

  public String getServletInfo() {
    return "ReportCreditNote for displaying in excel format";
  } // end of getServletInfo() method
}
