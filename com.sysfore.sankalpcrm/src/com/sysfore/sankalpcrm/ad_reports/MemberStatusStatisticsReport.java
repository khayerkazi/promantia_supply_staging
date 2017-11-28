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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

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

public class MemberStatusStatisticsReport extends HttpSecureAppServlet {

  public void doPost (HttpServletRequest request, HttpServletResponse response)
        throws IOException,ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

       boolean bs=vars.commandIn("DEFAULT");
       System.out.println("===================  1"+bs);
       bs=vars.commandIn("FIND");
       System.out.println("===================  2"+bs);
       bs=vars.commandIn("XLS");
       System.out.println("===================  3"+bs);

      if (vars.commandIn("DEFAULT")) {

     
      // BEGIN Parse parameters passed by the form
        //String strOrg = vars.getRequestGlobalVariable("inpOrg", "MemberStatusStatisticsReport|inpOrg");
        /*String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MemberStatusStatisticsReport|dateFrom");
        String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "MemberStatusStatisticsReport|dateTo");*/
        String client=vars.getClient();

        printPageDataSheet(response, vars, client);


    } else if (vars.commandIn("FIND"))
  	{
       // String strOrg = vars.getRequestGlobalVariable("inpOrg", "MemberStatusStatisticsReport|inpOrg");
 	/* String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MemberStatusStatisticsReport|dateFrom");
        String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "MemberStatusStatisticsReport|dateTo");*/
         String client=vars.getClient();
         printPageHtml(request, response, vars, client);
	}
     else if (vars.commandIn("XLS"))
	{
         //String strOrg = vars.getRequestGlobalVariable("inpOrg", "MemberStatusStatisticsReport|inpOrg");
	 /*String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MemberStatusStatisticsReport|dateFrom");
          String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "MemberStatusStatisticsReport");*/
          String client=vars.getClient();
            System.out.println("inside--------------XLS" +client);
            try {
                printPageXls(response, vars, client);
            } catch (ParseException ex) {
                Logger.getLogger(MemberStatusStatisticsReport.class.getName()).log(Level.SEVERE, null, ex);
            }

   }  else pageError(response);
    // END Controller of actions
}


  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String client)
    throws IOException, ServletException {

    if (log4j.isDebugEnabled()) log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    // BEGIN Retrieve invoice data for the selected timespan
   MemberStatusStatisticsReportData[] data = MemberStatusStatisticsReportData.select(this,client);
    // END Retrieve invoice data for the selected timespan
         System.out.println("----------------length"+data.length);
         System.out.println("----------------length"+data[0].disabledcustomers);
    // BEGIN Initialize the UI template
    XmlDocument xmlDocument=null;
    xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/sankalpcrm/ad_reports/MemberStatusStatisticsReport").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars, "org.openbravo.erpCommon.ad_reports.MemberStatusStatisticsReport");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "MemberStatusStatisticsReport.html", classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "MemberStatusStatisticsReport.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0,2));
    xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");
    // END Set up and pass core UI parameters to the template
    //xmlDocument.setData("reportAD_ORGID", "liststructure", OrganizationComboData.selectCombo(this,
   //     vars.getRole()));
    log4j.debug("UI parameters set");

    // BEGIN Prepare and set empty toolbar
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "MemberStatusStatisticsReport", false, "", "", "",false, "ad_reports",  strReplaceWith, false,  true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    OBError myMessage = vars.getMessage("MemberStatusStatisticsReport");
    vars.removeMessage("MemberStatusStatisticsReport");
    if (myMessage!=null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    // END Check, show & remove message
    log4j.debug("MESSAGE SET");

    // BEGIN Set parameters that need to be redisplayed on refresh

   // xmlDocument.setParameter("Jvacancy",strJvacancy);
    //xmlDocument.setParameter("adOrgId",strOrg);
    /*xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));*/
    log4j.info("Sample Display");
 //try {
   //     ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "er_jvacancy_ID",
   //         "", "er_jvacancy name", Utility.getContext(this, vars, "#AccessibleOrgTree",
   //             "SampleExcel"), Utility.getContext(this, vars, "#User_Client",
   //             "SampleExcel"), 0);
    //    Utility.fillSQLParameters(this, vars, null, comboTableData,
    //        "RecuitmentJR", strJvacancy);
       // xmlDocument.setData("reportER_jvacancy_ID", "liststructure", comboTableData.select(false));
     //   comboTableData = null;
    //  } catch (Exception ex) {
    //    throw new ServletException(ex);
    //  }


    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    
    log4j.debug("STRUCTURE SET");
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }
void printPageHtml(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars, String client) throws IOException, ServletException {
  if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    MemberStatusStatisticsReportData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();
    try {
        System.out.println("HTML");
      data = MemberStatusStatisticsReportData.select(this, client);
   } catch (Exception ex) {

      System.out.println(" Exception " + ex);
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
    strConvRateErrorMsg = myMessage.getMessage();
    // If a conversion rate is missing for a certain transaction, an error
    // message window pops-up.
    //if (!strConvRateErrorMsg.equals("") && strConvRateErrorMsg != null) {
    //  advisePopUp(request, response, "ERROR", Utility.messageBD(this, "NoConversionRateHeader",
        //  vars.getLanguage()), strConvRateErrorMsg);
   // } //else { // Launch the report as usual, calling the JRXML file
      String strOutput = "html";
      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/MemberStatusStatisticsReport.jrxml";

       String strTitle =classInfo.name;

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_SUBTITLE", strTitle);

      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
   // }
}
void printPageXls( HttpServletResponse response, VariablesSecureApp vars, String client) throws IOException, ServletException, ParseException {

      System.out.println("------------------------------------------------------------------------------------XLS");
      MemberStatusStatisticsReportData[] data = MemberStatusStatisticsReportData.select(this, client);
     // System.out.println("active customers-------------->"+data[0].activecustomers);
      String strOutput = "xls";
      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/MemberStatusStatisticsReport.jrxml";
      System.out.println("inside--------------XLS" +strReportName);
      String strTitle =classInfo.name;
     //response.setHeader("Content-disposition","inline;filename=RecuitmentJR.pdf");
      response.setHeader("Content-disposition","inline;filename=MemberStatusStatisticsReport.xls");

     //response.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);

      SimpleDateFormat formatter = new SimpleDateFormat("DD-MM-yyyy");

      HashMap<String, Object> parameters = new HashMap<String, Object>();

      parameters.put("REPORT_TITLE", strTitle);
      /*parameters.put("DateFrom",formatter.parse(strDateFrom));
      parameters.put("DateTo",formatter.parse(strDateTo));*/
      //parameters.put("USER_ORG",strOrg);
      parameters.put("USER_ORG",client);

      HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();

      parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
      renderJR(vars, response, strReportName, strOutput, parameters,data, parametersexport);

  }

  public String getServletInfo() {
    return "MemberStatusStatisticsReport for displaying in excel format";
  } // end of getServletInfo() method
}

