// Decompiled by DJ v3.12.12.96 Copyright 2011 Atanas Neshkov  Date: 10/18/2011 4:32:27 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   CreditNote.java

package com.sysfore.sankalpcrm.ad_forms;

import java.io.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.servlet.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.*;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.*;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.*;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;

import com.sysfore.decathlonsales.ad_reports.*;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;



import java.io.IOException;

import javax.servlet.ServletConfig;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;


// Referenced classes of package com.sysfore.sankalpcrm.ad_forms:
//            CreditNote1Data

public class CreditNoteHistory extends HttpSecureAppServlet
{
	private static final long serialVersionUID = 1L;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
		{		
		VariablesSecureApp vars = new VariablesSecureApp(request);
	  if (vars.commandIn("DEFAULT")) {
      String strOrderId = vars.getSessionValue("CreditNoteHistory.inprcCreditnoteId");
	        System.out.println("strOrderId_R------------>"+strOrderId);
      if (strOrderId.equals(""))
        strOrderId = vars.getSessionValue("CreditNoteHistory.inprcCreditnoteId");
      if (log4j.isDebugEnabled())
        log4j.debug("+***********************: " +strOrderId);
       // System.out.println("--- --------->"+strOrderId);
        printPageXls(response, vars, strOrderId);
    }else
      pageError(response);
    }
void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strmInoutId) throws IOException, ServletException {
    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
	String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_forms/Report_credit.jrxml";
    //System.out.println("strmInoutId**********" + strmInoutId);
    String strOutput = "pdf";
    String strTitle = classInfo.name;
    //System.out.println("strTitle value is "+strTitle);
    //
    response.setHeader("Content-disposition","inline;filename=CreditNote.pdf");
	HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_TITLE", strTitle);
	
    // Added for displaying lines in excel file
    strmInoutId = strmInoutId.replaceAll("\\(|\\)|'", "");
    System.out.println("strmInoutId**************** << " +strmInoutId);
    parameters.put("rcCreditnoteId", strmInoutId);
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    renderJR(vars, response, strReportName, strOutput, parameters, null, null);
	
  }
    public String getServletInfo()
    {
        return "CreditNoteHistory controller servlet made specifically for CBD Training";
    }

    
}
