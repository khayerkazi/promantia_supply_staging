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
import com.sysfore.sankalpcrm.ad_combos.StateData;

public class ReportBestCustomer extends HttpSecureAppServlet 
{

  public void doPost (HttpServletRequest request, HttpServletResponse response)throws IOException,ServletException
 {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) 
    {
      				// BEGIN Parse parameters passed by the form

        String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportBestCustomer|dateFrom");
        String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportBestCustomer|dateTo");
     	String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportBestCustomer|org");
        String strState = vars.getGlobalVariable("inpState", "ReportBestCustomer|state","");
	String strTopxrecords =vars.getRequestGlobalVariable("inpTopxrecords","ReportBestCustomer|topxrecords");
        
        printPageDataSheet(response, vars, strDateFrom, strDateTo, strOrg, strState, strTopxrecords);

        vars.setSessionValue("ReportBestCustomer|state", "");
	
	

    }
    else if (vars.commandIn("FIND"))
    {
 	 String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportBestCustomer|dateFrom");
         String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportBestCustomer|dateTo");
     	 String strOrg = vars.getRequestGlobalVariable("inpOrg","ReportBestCustomer|org");
         String strState = vars.getGlobalVariable("inpState", "ReportBestCustomer|state","");
	 String strTopxrecords = vars.getRequestGlobalVariable("inpTopxrecords","ReportBestCustomer|topxrecords");
         
         printPageHtml(request, response, vars, strDateFrom, strDateTo, strOrg, strState, strTopxrecords);
	vars.setSessionValue("ReportBestCustomer|state", "");
         
    } 
   else if (vars.commandIn("XLS"))
   {	
	 String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportBestCustomer|dateFrom");
         String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportBestCustomer|dateTo");
   	 String strOrg = vars.getRequestGlobalVariable("inpOrg","ReportBestCustomer|org");
         String strState = vars.getGlobalVariable("inpState", "ReportBestCustomer|state","");
	 String strTopxrecords = vars.getRequestGlobalVariable("inpTopxrecords","ReportBestCustomer|topxrecords");
         System.out.println("In XLS");
        
         printPageXls( response, vars, strDateFrom, strDateTo, strOrg, strState, strTopxrecords);
        vars.setSessionValue("ReportBestCustomer|state", "");
       	
    } 
   else pageError(response);
    			// END Controller of actions
}
  

 void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom, String strDateTo, String strOrg, String strState, String strTopxrecords) throws IOException, ServletException
 {

    if (log4j.isDebugEnabled()) log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();


   if (strOrg.equals("0"))
   {
   ReportBestCustomerData[] data = ReportBestCustomerData.select1(this, strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strState, strTopxrecords);

   }
   else
   {
   ReportBestCustomerData[] data = ReportBestCustomerData.select(this, strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strOrg, strState, strTopxrecords);
   }



        
                     // BEGIN Retrieve invoice data for the selected timespan

 //ReportBestCustomerData[] data = ReportBestCustomerData.select(this, strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strOrg, strState, strTopxrecords);

    		    // END Retrieve invoice data for the selected timespan

    		   // BEGIN Initialize the UI template

    XmlDocument xmlDocument=null;
    xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/sankalpcrm/ad_reports/ReportBestCustomer").createXmlDocument();

    		  // END Initialize the UI template

    log4j.debug("XML LOADED");

   		 // BEGIN Set up and pass core UI parameters to the template
    try 
    {
      WindowTabs tabs = new WindowTabs(this, vars, "com.sysfore.sankalpcrm.ad_reports.ReportBestCustomer");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportBestCustomer.html", classInfo.id,    classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportBestCustomer.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } 
    catch (Exception ex) 
    {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0,2));
    xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");

    		// END Set up and pass core UI parameters to the template

    log4j.debug("UI parameters set");

    		// BEGIN Prepare and set empty toolbar

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportBestCustomer", false, "", "", "",false, "ad_reports",  strReplaceWith, false,  true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString()); 

   	 	// END Prepare empty toolbar

    log4j.debug("TOOLBAR SET");
   
    OBError myMessage = vars.getMessage("ReportBestCustomer");
    vars.removeMessage("ReportBestCustomer");
    if (myMessage!=null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
   	 	// END Check, show & remove message

    log4j.debug("MESSAGE SET");

   	 	// BEGIN Set parameters that need to be redisplayed on refresh
  
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("org", strOrg);
    System.out.println("org");
    xmlDocument.setParameter("state", strState);
    System.out.println("state");
    xmlDocument.setParameter("topxrecords", strTopxrecords);
    System.out.println("topxrecords");

    //ReportBestCustomerData[] state = ReportBestCustomerData.selectState(this, "0");
    //System.out.println("state1 :"+ state[0].state);
    //System.out.println("state2 :"+ state[1].state);

    log4j.info("Sample Display");
    System.out.println("Try Block1 Starts");

    
            try
	    {
	      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "ad_org_id",
	          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportBestCustomer"),
	          Utility.getContext(this, vars, "#User_Client", "ReportBestCustomer"), 0);
	      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportBestCustomer", strOrg);
	      xmlDocument.setData("reportad_orgid", "liststructure", comboTableData.select(false));
	      comboTableData = null;
	    } 
            catch (Exception ex) 
            {
     		 throw new ServletException(ex);
            }
     

    xmlDocument.setData("reportc_regionid", "liststructure", StateData.selectCombo(this, "0"));
    StateData [] data1 = StateData.selectCombo(this, "0");
    System.out.println("state.name : " + data1[0].name);
   
/*try 
 
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportBestCustomer"),
          Utility.getContext(this, vars, "#User_Client", "ReportBestCustomer"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportBestCustomer", state);
      xmlDocument.setData("reportc_regionid", "liststructure", comboTableData.select(false));
      comboTableData = null;
    }
    catch (Exception ex) 
    {
      throw new ServletException(ex);
    }*/

	System.out.println("Try Block2 Starts");
	String strClient = "0";
	System.out.println("Try Block2 Ends");   

	    	// END Set parameters that need to be redisplayed on refresh

       log4j.debug("FILTER PARAMETERS SET");
       log4j.debug("STRUCTURE SET");
       out.println(xmlDocument.print());
       out.close();
  }
void printPageHtml(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars,String strDateFrom, String strDateTo, String strOrg, String strState, String strTopxrecords) throws IOException, ServletException 
{
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print html");

    		// Checks if there is a conversion rate for each of the transactions of the report

    ReportBestCustomerData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();

    try 
    {
        System.out.println("Calling select inside printPageHtml");
       data = ReportBestCustomerData.select(this,  strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strOrg, strState, strTopxrecords);
    } 
    catch (ServletException ex) 
    {
        ex.printStackTrace();
        System.out.println(" Exception " + ex);
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
    }
    strConvRateErrorMsg = myMessage.getMessage();
    
      String strOutput = "html";
      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportBestCustomer.jrxml";

      String strTitle = "";
      strTitle = Utility.messageBD(this, "From", vars.getLanguage()) + " " + strDateFrom + " "
          + Utility.messageBD(this, "To", vars.getLanguage()) + " " + strDateTo;


      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_SUBTITLE", strTitle);
    
      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
   
}
void printPageXls( HttpServletResponse response, VariablesSecureApp vars,String strDateFrom, String strDateTo, String strOrg, String strState, String strTopxrecords) throws IOException, ServletException 
{

      if (strOrg.equals("0"))
      {

      System.out.println("Calling select inside printPageHtml");
      ReportBestCustomerData[] data = ReportBestCustomerData.select1(this, strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strState, strTopxrecords);
      System.out.println("data.length********************"  +data.length);
         System.out.println("strState=========="  +strState);
      String strOutput = "xls";
      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportBestCustomer1.jrxml";
      String strTitle = "";
      String  strSubTitle = "";
      strSubTitle = Utility.messageBD(this, "From Date ", vars.getLanguage()) + " " + strDateFrom + " "
          + Utility.messageBD(this, "To Date ", vars.getLanguage()) + " " + strDateTo;
      response.setHeader("Content-disposition","inline;filename=ReportBestCustomer1.xls");
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_TITLE", strTitle);
      parameters.put("REPORT_SUBTITLE", strSubTitle);

		//Added for displaying lines in excel file

      HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();      
      parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
      renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
    
    }
    else
    {
   
     System.out.println("Calling select inside printPageHtml");
      ReportBestCustomerData[] data = ReportBestCustomerData.select(this, strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strOrg, strState, strTopxrecords);
      System.out.println("strState=========="  +strState);
      System.out.println("data.length=========="  +data.length);
      String strOutput = "xls";
      String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_reports/ReportBestCustomer.jrxml";
     String strTitle = "";
      String  strSubTitle = "";
      strSubTitle = Utility.messageBD(this, "From Date ", vars.getLanguage()) + " " + strDateFrom + " "
          + Utility.messageBD(this, "To Date ", vars.getLanguage()) + " " + strDateTo;
      response.setHeader("Content-disposition","inline;filename=ReportBestCustomer1.xls");
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_TITLE", strTitle);
      parameters.put("REPORT_SUBTITLE", strSubTitle);

		//Added for displaying lines in excel file

      HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();      
      parametersexport.put(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
      renderJR(vars, response, strReportName, strOutput, parameters, data, parametersexport);
     }
}

  public String getServletInfo() 
{
    return "ReportBestCustomer for displaying excel format";

}
}


