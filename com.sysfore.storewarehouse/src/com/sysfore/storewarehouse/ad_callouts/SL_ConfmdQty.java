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
import java.lang.Math;

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

public class SL_ConfmdQty extends HttpSecureAppServlet {
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
   
      String strCOrderId = vars.getStringParameter("inpcOrderId");
      String strOrderqty = vars.getStringParameter("inpemSwOrderqty");
      String strWindowId = vars.getStringParameter("inpwindowId");
      String strIsSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
      String strTabId = vars.getStringParameter("inpTabId");
      String strProduct = vars.getStringParameter("inpemSwItemcode");

      try {
        printPage(response, vars, strChanged, strCOrderId, strTabId, strOrderqty, strProduct);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strCOrderId, String strTabId, String strOrderqty, String strProduct) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    StringBuffer resultado = new StringBuffer();
    StringBuffer message = new StringBuffer();
    resultado.append("var calloutName='SL_Exptd_DeliveryDt';\n\n");
    resultado.append("var respuesta = new Array(");
    Double  orderqty, pcbqty, noofparcels, multiplechecking;
    SLPOLineData[] data = SLPOLineData.select(this, strProduct);
    String strpcbqty = data[0].pcbqty;
    orderqty =  Double.parseDouble(strOrderqty);
    System.out.println("orderqty====" +orderqty);
    pcbqty =  Double.parseDouble(strpcbqty);
    System.out.println("pcbqty====" +pcbqty);  //first % second == 0
    multiplechecking = orderqty % pcbqty;
    System.out.println("multiplechecking====" +multiplechecking);
     if(multiplechecking == 0.00)
     {
          System.out.println("I am ok===");
     } else 
     {
    
     message.append(Utility.messageBD(this, "SW_Orderqty", vars.getLanguage()));
    System.out.println("I am in First message block======");
    }
    if(pcbqty==0.0)
    {
      noofparcels=0.0;
    } else
    {
    noofparcels = orderqty/pcbqty;
    noofparcels = Math.ceil(noofparcels);
   System.out.println("noofparcels=========" +noofparcels);

    }
    String strnoofparcels = Double.toString(noofparcels);
    System.out.println("strnoofparcels=========" +strnoofparcels);

   /* SLRequisitionSkipapp1Data[] data;
    data = SLRequisitionSkipapp1Data.select(myPool, strCProjectID);*/
    System.out.println("strOrderqty=======" +strOrderqty);

    resultado.append("new Array(\"inpqtyordered\", \"" + strOrderqty + "\"),\n");
    resultado.append("new Array(\"inpemSwNoofparcel\", \"" + strnoofparcels + "\")\n");
    //resultado.append("new Array(\"inpqtyordered\", \"" + strOrderqty + "\")");
      if (message != null) {
      resultado.append(", new Array('MESSAGE', \"" + message + "\")");
    }

    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
