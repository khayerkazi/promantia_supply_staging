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
//import java.math.int;
import java.lang.Integer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;
//import org.openbravo.utils.FormatUtilities;

public class SL_UE_Req extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  //private static final int ZERO = new int(0.0);
  //private static final int ONE = new int(1.0);

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

      /*String strFreeConcUser = vars.getNumericParameter("inpfreeconcuser");
      String strProfConcUser = vars.getNumericParameter("inpprofconcuser");*/
      String strReqQty = vars.getStringParameter("inpqtyreq");
      String strProduct = vars.getStringParameter("inpmProductId");
      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strChanged, strReqQty, strProduct, strTabId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strReqQty, String strProduct, String strTabId) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

   String strUEqty = SLUEReqData.select(this, strProduct);
    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_UE_Req';\n\n");
    resultado.append("var respuesta = new Array(");


    int reqQty, total, modvalue, subvalue;
    int ueQty = 0;

    reqQty = Integer.valueOf(strReqQty);
    ueQty = Integer.valueOf(strUEqty);

    modvalue = reqQty % ueQty;
  if(strProduct.equals(""))
     resultado.append("new Array(\"inpqtyreq\", " + ueQty + ")");
     //resultado.append("new Array(\"inpqtyreq\", " + ueQty + ")");

  else {   
    if(reqQty == 0){
       resultado.append("new Array(\"inpqtyreq\", " + ueQty + ")");
       //resultado.append("new Array(\"inpqtyreq\", " + ueQty + ")");
     }
    else if(modvalue == 0){
      resultado.append("new Array(\"inpqtyreq\", " + reqQty + ")");
      //resultado.append("new Array(\"inpqtyreq\", " + reqQty + ")");
      }
    else {
           subvalue = ueQty - modvalue;
           total = reqQty + subvalue;
      resultado.append("new Array(\"inpqtyreq\", " + total + ")");
      //resultado.append("new Array(\"inpqtyreq\", " + total + ")");
    }
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
