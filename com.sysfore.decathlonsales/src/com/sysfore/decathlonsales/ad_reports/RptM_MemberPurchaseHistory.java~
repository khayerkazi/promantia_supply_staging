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
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.decathlonsales.ad_reports;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class RptM_MemberPurchaseHistory extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strOrderId = vars.getSessionValue("RptM_MemberPurchaseHistory.inpcOrderId_R");
        System.out.println("strOrderId_R------------>"+strOrderId);
      if (strOrderId.equals(""))
        strOrderId = vars.getSessionValue("RptM_MemberPurchaseHistory.inpcOrderId");
      if (log4j.isDebugEnabled())
        log4j.debug("+***********************: " +strOrderId);
        System.out.println("------------>"+strOrderId);
        printPagePartePDF(response, vars, strOrderId);
    } else
      pageError(response);
  }

  private void printPagePartePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strOrderId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: pdf");
    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
      System.out.println("-------------base design"+strBaseDesign);
    HashMap<String, Object> parameters = new HashMap<String, Object>();


    JasperReport jasperReportLines1;
   try {
     JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
        + "/com/sysfore/decathlonsales/ad_reports/DuplicateBill_subreport0.jrxml");
    jasperReportLines1 = JasperCompileManager.compileReport(jasperDesignLines);
    } catch (JRException e) {
    e.printStackTrace();
     throw new ServletException(e.getMessage());
   }


     JasperReport jasperReportLines2;
   try {
     JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
        + "/com/sysfore/decathlonsales/ad_reports/DuplicateBill_subreport2.jrxml");
    jasperReportLines2 = JasperCompileManager.compileReport(jasperDesignLines);
  } catch (JRException e) {
    e.printStackTrace();
     throw new ServletException(e.getMessage());
   }


    JasperReport jasperReportLines3;
   try {
     JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
        + "/com/sysfore/decathlonsales/ad_reports/DuplicateBill_subreport1.jrxml");
    jasperReportLines3 = JasperCompileManager.compileReport(jasperDesignLines);
  } catch (JRException e) {
    e.printStackTrace();
     throw new ServletException(e.getMessage());
   }

    strOrderId = strOrderId.replaceAll("\\(|\\)|'", "");

    System.out.println("-------------->passing"+strOrderId);
    
    parameters.put("SR_LINES1", jasperReportLines1);
    parameters.put("SR_LINES2", jasperReportLines2);
    parameters.put("SR_LINES3", jasperReportLines3);
    parameters.put("ORDERID", strOrderId);
    renderJR(vars, response, null, "pdf", parameters, null, null);
  }

  public String getServletInfo() {
    return "Servlet that presents the RptMRequisitions seeker";
  } // End of getServletInfo() method
}
