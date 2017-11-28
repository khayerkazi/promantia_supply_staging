package in.nous.picklist.reports.ad_reports;

import in.nous.picklist.reports.info.SelectorUtilityForOrgData;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportStockFile extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportStockFile|dateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportStockFile|dateTo", "");

      String strOrg = vars.getInGlobalVariable("inpadOrg_IN", "ReportStockFile|adOrg", "",
          IsIDFilter.instance);

      printPageDataSheet(response, vars, strDateFrom, strDateTo, strOrg);
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportStockFile|dateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportStockFile|dateTo", "");
      String strOrg = vars.getInGlobalVariable("inpadOrg_IN", "ReportStockFile|adOrg", "",
          IsIDFilter.instance);
      printPageDataSheet(response, vars, strDateFrom, strDateTo, strOrg);
    } else if (vars.commandIn("XLS")) {

      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportStockFile|dateFrom");
      System.out.println("strDateFrom: in XLS " + strDateFrom);
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportStockFile|dateTo");
      System.out.println("strDateTo: in XLS " + strDateTo);
      String strOrg = vars.getInGlobalVariable("inpadOrg_IN", "ReportStockFile|adOrg", "",
          IsIDFilter.instance);
      vars.setSessionValue("ReportStockFile|adOrg", "");
      printPageXls(response, vars, strDateFrom, strDateTo, strOrg);

    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strOrg) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = null;

    xmlDocument = xmlEngine.readXmlTemplate("in/nous/picklist/reports/ad_reports/ReportStockFile")
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportStockFile", false, "", "", "",
        false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "in.nous.picklist.reports.ad_reports.ReportStockFile");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportStockFile.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportStockFile.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportStockFile");
      vars.removeMessage("ReportStockFile");
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

    xmlDocument.setData("reportadorgId_IN", "liststructure",
        SelectorUtilityForOrgData.selectOrg(this, vars.getClient(), strOrg));

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom,
      String strDateTo, String strOrg) throws IOException, ServletException {

    String strDateTo1 = DateTimeData.nDaysAfter(this, strDateTo, "1");
    System.out.println("strDateTo1======" + strDateTo1);

    ReportStockFileData[] data = ReportStockFileData.select(this, strDateFrom, strDateTo1, strOrg);

    String strBaseDesign = getBaseDesignPath(vars.getLanguage());

    String strOutput = "xls";
    // System.out.println("after fetching data" + data.length);
    String strReportName = "@basedesign@/in/nous/picklist/reports/ad_reports/ReportStockFile.jrxml";
    System.out.println("after fetching strReportName" + strReportName);
    String strTitle = classInfo.name;
    Date fromdate = null;
    Date todate = null;
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    try {
      fromdate = df.parse(strDateFrom);
      todate = df.parse(strDateTo);
    } catch (ParseException ex) {
      Logger.getLogger(ReportStockFile.class.getName()).log(Level.SEVERE, null, ex);
    }

    response.setHeader("Content-disposition", "inline;filename=ReportStockFile.xls");
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_TITLE", strTitle);
    parameters.put("datefrom", fromdate);
    parameters.put("dateto", todate);
    parameters.put("adOrg", strOrg);

    // Added for displaying lines in excel file
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();

    parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
    renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
  }

  public String getServletInfo() {
    return "Servlet ReportStockFile.";
  } // end of the getServletInfo() method
}
