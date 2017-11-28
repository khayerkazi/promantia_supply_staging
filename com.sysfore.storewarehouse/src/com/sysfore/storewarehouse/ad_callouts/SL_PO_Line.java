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
import java.math.BigDecimal;

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

public class SL_PO_Line extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  private static final BigDecimal ZERO = new BigDecimal(0.0);

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
      String strProduct = vars.getStringParameter("inpemSwItemcode");
      System.out.println("1.strProduct: " + strProduct);
      String strTabId = vars.getStringParameter("inpTabId");
      String strOrderqty = vars.getStringParameter("inpemSwOrderqty");
     String strCOrderId = vars.getStringParameter("inpcOrderId");

      try {
        printPage(response, vars, strProduct, strTabId, strOrderqty, strCOrderId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strProduct, String strTabId, String strOrderqty, String strCOrderId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    SLPOLineData[] data = SLPOLineData.select(this, strProduct);
    StringBuffer resultado = new StringBuffer();
    StringBuffer message = new StringBuffer();

    resultado.append("var calloutName='SL_PO_Line';\n\n");
    resultado.append("var respuesta = new Array(");

    System.out.println("2.strProduct: " + strProduct);
    BigDecimal volumepcb, grosswtpcb, ntwtpcb, value;
    //Double  orderqty, pcbqty, noofparcels;
    String strvolumepcb, strgrosswtpcb, strntwtpcb, strValue;
    strValue = "0.00";

    System.out.println("data.length****************************************" +data.length);
    SLOrderLineModelData[] data2 = SLOrderLineModelData.select(myPool, strCOrderId);
    System.out.println("data2.length======" +data2.length);
    System.out.println("data2.model======" +data2[0].model);
   // System.out.println("data0.model======" +data[0].model);
    if(data.length==0 )
    {
     message.append(Utility.messageBD(this, "SW_NoProduct", vars.getLanguage()));
    System.out.println("I am in First message block======");
    } else if(!data2[0].model.equals(data[0].model))
    {
      message.append(Utility.messageBD(this, "SW_NoModelMatching", vars.getLanguage()));
      System.out.println("I am in second message block======");
    }
 	if (data != null && data.length > 0)
        {
         strvolumepcb = data[0].volumepcb;
         volumepcb = new BigDecimal(strvolumepcb);
         System.out.println("strvolumepcb: " + strvolumepcb);
         strgrosswtpcb = data[0].grosswtpcb;
         grosswtpcb = new BigDecimal(strgrosswtpcb);
         System.out.println("strgrosswtpcb: " + strgrosswtpcb);
         strntwtpcb = data[0].ntwtpcb;
         ntwtpcb = new BigDecimal(strntwtpcb);
         System.out.println("strntwtpcb: " + strntwtpcb);

         String strcession = data[0].cession;
         BigDecimal cession = new BigDecimal(strcession);
         System.out.println("strcession: " + strcession);

         String strmrp = data[0].mrp;
         BigDecimal mrp = new BigDecimal(strmrp);
         System.out.println("strmrp: " + strmrp);

         String strccunitprice = data[0].ccunitprice;
         BigDecimal ccunitprice = new BigDecimal(strccunitprice);
         System.out.println("strccunitprice: " + strccunitprice);

        // String strpcbqty = data[0].pcbqty;
         //orderqty =  Double.parseDouble(strOrderqty);
        // System.out.println("orderqty====" +orderqty);
         //pcbqty =  Double.parseDouble(strpcbqty);
        // System.out.println("pcbqty====" +pcbqty);
         //noofparcels = orderqty/pcbqty;
        // String strnoofparcels = Double.toString(noofparcels);
         //System.out.println("strnoofparcels=========" +strnoofparcels);
  
         String strfobprice;
         System.out.println("data[0].product=======" +data[0].product);
          System.out.println("strCOrderId=======" +strCOrderId);
         SLPOLinePriceData[] data1 = SLPOLinePriceData.select(myPool, strCOrderId, data[0].product);
          System.out.println("data1.length=======" +data1.length);
         if(data1 != null && data1.length >0)
         {
            strfobprice = data1[0].fobprice;
         } else
         {
             strfobprice = "0";
             System.out.println("strfobprice======" +strfobprice);
         }
         resultado.append("new Array(\"inpmProductId\", \"" + FormatUtilities.replaceJS(data[0].product) + "\"),\n");
         System.out.println("Inside If : Product: " + data[0].product);
         resultado.append("new Array(\"inpemSwVolpcb\", \"" + volumepcb.toString() + "\"),\n");
         System.out.println("Inside If : strvolumepcb: " + strvolumepcb);
         resultado.append("new Array(\"inpemSwGrwtpcb\", \"" + grosswtpcb.toString() + "\"),\n");
         System.out.println("Inside If : strgrosswtpcb: " + strgrosswtpcb);
         resultado.append("new Array(\"inpemSwNtwtpcb\", \"" + ntwtpcb.toString() + "\"),\n");
         System.out.println("Inside If : strntwtpcb: " + strntwtpcb);

         resultado.append("new Array(\"inpemDsCcunitprice\", \"" + ccunitprice.toString() + "\"),\n");
         resultado.append("new Array(\"inpemDsCessionprice\", \"" + cession.toString() + "\"),\n");
         resultado.append("new Array(\"inpemDsMrpprice\", \"" + mrp.toString() + "\"),\n");

         resultado.append("new Array(\"inpemSwModelname\", \"" + FormatUtilities.replaceJS(data[0].model) + "\"),\n");
         System.out.println("Inside If : model: " + data[0].model);
         //resultado.append("new Array(\"inpsize\", \"" + data[0].size + "\"),\n");
         resultado.append("new Array(\"inpemSwSize\", \"" + FormatUtilities.replaceJS(data[0].size) + "\"),\n");
         System.out.println("Inside If : size: " + data[0].size);
         //resultado.append("new Array(\"inpcolor\", \"" + data[0].color + "\")\n");
         resultado.append("new Array(\"inpemSwColor\", \"" + FormatUtilities.replaceJS(data[0].color) + "\"),\n");
         System.out.println("Inside If : color: " + data[0].color);
        // resultado.append("new Array(\"inpemSwSize\", \"" + FormatUtilities.replaceJS(data[0].color) + "\")\n");
         //System.out.println("data1[0].fobprice " + data1[0].fobprice);
         resultado.append("new Array(\"inppriceactual\", \"" + FormatUtilities.replaceJS(strfobprice) + "\")\n");
        }
else{
         System.out.println("Inside Else Starts ");
         value = new BigDecimal(strValue);
         resultado.append("new Array(\"inpmProductId\", \"" + FormatUtilities.replaceJS("") + "\"),\n");

         resultado.append("new Array(\"inpemSwVolpcb\", \"" + value.toString() + "\"),\n");
         resultado.append("new Array(\"inpemSwGrwtpcb\", \"" + value.toString() + "\"),\n");
         resultado.append("new Array(\"inpemSwNtwtpcb\", \"" + value.toString() + "\"),\n");
         resultado.append("new Array(\"inpemDsCcunitprice\", \"" + value.toString() + "\"),\n");
         resultado.append("new Array(\"inpemDsCessionprice\", \"" + value.toString() + "\"),\n");
         resultado.append("new Array(\"inpemDsMrpprice\", \"" + value.toString() + "\"),\n");

         resultado.append("new Array(\"inpemSwModelname\", \"" + FormatUtilities.replaceJS("") + "\"),\n");

         resultado.append("new Array(\"inpemSwSize\", \"" + FormatUtilities.replaceJS("") + "\"),\n");

         resultado.append("new Array(\"inpemSwColor\", \"" + FormatUtilities.replaceJS("") + "\")\n");

         //message.append(Utility.messageBD(this, "SW_NoProduct", vars.getLanguage()));
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
