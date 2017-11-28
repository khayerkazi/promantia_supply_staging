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
 * All portions are Copyright (C) 2001-2009 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.storewarehouse.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.data.FieldProvider;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.erpCommon.utility.OBError;

public class SL_Req_Line extends HttpSecureAppServlet {
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
      //String strProduct = vars.getStringParameter("inpmProductId");
      String strProduct = vars.getStringParameter("inpitemcode");
      System.out.println("1.strProduct: " + strProduct);
      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strProduct, strTabId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strProduct, String strTabId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    SLReqLineData[] data = SLReqLineData.select(this, strProduct);
    StringBuffer resultado = new StringBuffer();
    StringBuffer message = new StringBuffer();

    resultado.append("var calloutName='SL_Req_Line';\n\n");
    resultado.append("var respuesta = new Array(");

    System.out.println("2.strProduct: " + strProduct);
    System.out.println("****************************************");
 	if (data != null && data.length > 0)
        {
         resultado.append("new Array(\"inpmProductId\", \"" + FormatUtilities.replaceJS(data[0].product) + "\"),\n");
         System.out.println("Inside If : Product: " + data[0].product);

         //resultado.append("new Array(\"inpmodelname\", \"" + data[0].model + "\"),\n");
         resultado.append("new Array(\"inpmodelname\", \"" + FormatUtilities.replaceJS(data[0].model) + "\"),\n");
         System.out.println("Inside If : model: " + data[0].model);
         //resultado.append("new Array(\"inpsize\", \"" + data[0].size + "\"),\n");
         resultado.append("new Array(\"inpsize\", \"" + FormatUtilities.replaceJS(data[0].size) + "\"),\n");
         System.out.println("Inside If : size: " + data[0].size);
         //resultado.append("new Array(\"inpcolor\", \"" + data[0].color + "\")\n");
         resultado.append("new Array(\"inpcolor\", \"" + FormatUtilities.replaceJS(data[0].color) + "\")\n");
         System.out.println("Inside If : color: " + data[0].color);
        }
else{
         System.out.println("Inside Else Starts ");
         resultado.append("new Array(\"inpmProductId\", \"" + FormatUtilities.replaceJS("") + "\"),\n");

         resultado.append("new Array(\"inpmodelname\", \"" + FormatUtilities.replaceJS("") + "\"),\n");

         resultado.append("new Array(\"inpsize\", \"" + FormatUtilities.replaceJS("") + "\"),\n");

         resultado.append("new Array(\"inpcolor\", \"" + FormatUtilities.replaceJS("") + "\")\n");

        /*message.setTitle("Error");
        message.setType("ERROR");*/
      message.append(Utility.messageBD(this, "SW_NoProduct", vars.getLanguage()));
         System.out.println("Inside message: ");
         System.out.println("Inside Else Ends ");
    }

     if (message != null) {
      resultado.append(", new Array('MESSAGE', \"" + message + "\")");
    }

    resultado.append(");\n");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
