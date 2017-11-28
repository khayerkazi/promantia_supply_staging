/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sysfore.storewarehouse.ad_callouts;

/**
 *
 * @author subrata
 */


import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_ActShipmentDt extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
    //  System.out.println("I am in defult===");
     // String strCProjectID = vars.getStringParameter("inpemErCProjectId");
      //System.out.println("strCProjectID===" +strCProjectID);
      String strCOrderId = vars.getStringParameter("inpcOrderId");
      String strEstShipmtDt = vars.getStringParameter("inpemSwEstshipdate");
      String strWindowId = vars.getStringParameter("inpwindowId");
      String strIsSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strChanged, strCOrderId, strTabId, strEstShipmtDt);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strCOrderId, String strTabId, String strEstShipmtDt) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_Exptd_DeliveryDt';\n\n");
    resultado.append("var respuesta = new Array(");

   /* SLRequisitionSkipapp1Data[] data;
    data = SLRequisitionSkipapp1Data.select(myPool, strCProjectID);*/
    System.out.println("strEstShipmtDt=======" +strEstShipmtDt);
    SLPriceListVersionData[] data = SLPriceListVersionData.select(myPool, strEstShipmtDt);
    resultado.append("new Array(\"inpemSwActshipdate\", \"" + strEstShipmtDt + "\"),\n");
    resultado.append("new Array(\"inpemSwPricelistver\", \"" + data[0].mPricelistVersionId + "\")\n");

    //resultado.append("new Array(\"inpemErSkipapp1\", \"" + data[0].emErSkipapp + "\")\n");
    //resultado.append("new Array(\"inpemSwActshipdate\", \"" + strEstShipmtDt + "\")");
    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
