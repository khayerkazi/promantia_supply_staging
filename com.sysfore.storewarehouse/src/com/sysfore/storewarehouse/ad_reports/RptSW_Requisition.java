/*
 *
 * The contents of this file are subject to the Openbravo Public License Version
 * 1.0 (the "License"), being the Mozilla Public License Version 1.1 with a
 * permitted attribution clause; you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.openbravo.com/legal/license.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. The Original Code is
 * Openbravo ERP. The Initial Developer of the Original Code is Openbravo SLU All
 * portions are Copyright (C) 2001-2009 Openbravo SLU All Rights Reserved.
 * Contributor(s): ______________________________________.
 */
package com.sysfore.storewarehouse.ad_reports;

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

public class RptSW_Requisition extends HttpSecureAppServlet {
private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
    ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strswSrequisitionId = vars.getSessionValue("RptSW_Requisition.inpswSrequisitionId_R");
     // String strswSrequisitionId1 = vars.getSessionValue("RptSW_Requisition.inpswSrequisitionId");
      System.out.println("strswSrequisitionId: in DEFAULT " + strswSrequisitionId);
      if (strswSrequisitionId.equals(""))
       strswSrequisitionId = vars.getSessionValue("RptSW_Requisition.inpswSrequisitionId");
       System.out.println("strswSrequisitionId:" + strswSrequisitionId);
      if (strswSrequisitionId.equals(""))
       strswSrequisitionId = vars.getSessionValue("RptSW_Requisition.inpswSrequisitionId");
       System.out.println("strswSrequisitionId--" +strswSrequisitionId);
       String strmovemenType = vars.getStringParameter("inpmovementtype", "");
       //System.out.println("strmovemenType========" +strmovemenType);
      if (log4j.isDebugEnabled())
        log4j.debug("strswrequisitionId: " + strswSrequisitionId);
      printPagePartePDF(response, vars, strswSrequisitionId);
    } else
      pageError(response);
  }

  private void printPagePartePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strswSrequisitionId) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("Output: RptSW_Srequisition - xls");
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    JasperReport jasperReportLines;
    String strLanguage = vars.getLanguage();
    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    //System.out.println("strBaseDesign***********" +strBaseDesign);

    try
    {
     strswSrequisitionId = strswSrequisitionId.replaceAll("\\(|\\)|'", "");
     RptSWRequisitionData[] data = RptSWRequisitionData.select(this, strswSrequisitionId);
     System.out.println("strmovemenType========" + data[0].movementtype);
     //System.out.println("strswSrequisitionId: after RptSWRequisitionData" + data[0].movementtype + data.length);
   
     if(data[0].movementtype.equals("SRN"))
      {
         System.out.println("I am inside SRN =========");
         JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
          + "/com/sysfore/storewarehouse/ad_reports/RptSW_Requisition.jrxml");
          jasperReportLines = JasperCompileManager.compileReport(jasperDesignLines);
         // System.out.println("I am inside SRN =========");
          strswSrequisitionId = strswSrequisitionId.replaceAll("\\(|\\)|'", "");
          String strReportName = "@basedesign@/com/sysfore/storewarehouse/ad_reports/RptSW_Requisitions.jrxml";
          response.setHeader("Content-disposition", "inline; filename=StoreRequisition.xls");
          parameters.put("SR_LINES", jasperReportLines);
          parameters.put("SW_Srequisition_Id", strswSrequisitionId);
          renderJR(vars, response, strReportName, "xls", parameters, null, null);
          System.out.println("I am passing SRN");
      }
      else 
      {
       System.out.println("I am entering SRQ.....");
       JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign + "/com/sysfore/storewarehouse/ad_reports/RptSW_RequisitionSRQ.jrxml");
       jasperReportLines = JasperCompileManager.compileReport(jasperDesignLines);
       strswSrequisitionId = strswSrequisitionId.replaceAll("\\(|\\)|'", "");
       parameters.put("SR_LINES1", jasperReportLines);
       parameters.put("SW_Srequisition_Id", strswSrequisitionId);
       renderJR(vars, response, null, "xls", parameters, null, null);
       System.out.println("Hi It is ok SRQ....");
       //if (log4j.isDebugEnabled())
      
      }
    } catch (JRException e) {
      e.printStackTrace();
     throw new ServletException(e.getMessage());
    }


  }

  public String getServletInfo() {
    return "Servlet that presents the RptSWrequisition seeker";
  } // End of getServletInfo() method
}
