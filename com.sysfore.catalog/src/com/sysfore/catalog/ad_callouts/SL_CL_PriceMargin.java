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
package com.sysfore.catalog.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_CL_PriceMargin extends HttpSecureAppServlet {
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

      String cessionPrice = vars.getStringParameter("inpemClCessionprice");
      String unitPrice = vars.getStringParameter("inpemClCcunitprice");
      String uePrice = vars.getStringParameter("inpemClCcueprice");
      String pcbPrice = vars.getStringParameter("inpemClCcpcbprice");
      String strProduct = vars.getStringParameter("inpmProductId");
      String strOrg = vars.getStringParameter("inpadOrgId");

      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strChanged, cessionPrice, unitPrice, uePrice, pcbPrice,
            strProduct, strTabId, strOrg);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String cessionPrice, String unitPrice, String uePrice, String pcbPrice, String strProduct,
      String strTabId, String strOrg) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    String taxRate = SLCLPriceMarginData.select(this, strProduct, strOrg);

    String octroi = SLCLPriceMarginData.selectOctroi(this, strProduct);

    String region = SLCLPriceMarginData.selectRegion(this, strOrg);

    double cession, unit, ue, pcb, tax, taxAmount, unitMargin, unitMarginPer, ueMargin, ueMarginPer, pcbMargin, pcbMarginPer;

    if (cessionPrice == null || cessionPrice.equals("")) {
      cession = 0;
    } else {
      cession = Double.valueOf(cessionPrice);
    }
    if (unitPrice == null || unitPrice.equals("")) {
      unit = 0;
    } else {
      unit = Double.valueOf(unitPrice);
    }
    if (uePrice == null || uePrice.equals("")) {
      ue = 0;
    } else {
      ue = Double.valueOf(uePrice);
    }
    if (pcbPrice == null || pcbPrice.equals("")) {
      pcb = 0;
    } else {
      pcb = Double.valueOf(pcbPrice);
    }
    if (taxRate == null || taxRate.equals("")) {
      tax = 0;
    } else {
      // taxRate = taxRate/100;
      tax = Double.valueOf(taxRate);
    }

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_CL_PriceMargin';\n\n");
    resultado.append("var respuesta = new Array(");

    if (cession == 0.0 || unit == 0.0) {
      resultado.append("new Array(\"inpemClUnitmarginamount\", \"" + "" + "" + "\"),\n");
      resultado.append("new Array(\"inpemClUnitmarginpercentage\", \"" + "" + "\"),\n");

    } else {
      if (region.equals("MH")) {
        double tempOctCalc = (Double.valueOf(octroi) / 100) * cession;

        taxAmount = (unit / (100 + tax)) * tax;
        unitMargin = unit - taxAmount - cession - tempOctCalc;
        unitMarginPer = (unitMargin / unit) * 100;

      } else {
        taxAmount = (unit / (100 + tax)) * tax;
        unitMargin = unit - taxAmount - cession;
        unitMarginPer = (unitMargin / unit) * 100;
      }

      resultado.append("new Array(\"inpemClUnitmarginamount\", \"" + "" + unitMargin + "\"),\n");
      resultado.append("new Array(\"inpemClUnitmarginpercentage\", \"" + unitMarginPer + "\"),\n");

    }
    if (cession == 0.0 || ue == 0.0) {
      resultado.append("new Array(\"inpemClUemarginamount\", \"" + "" + "\"),\n");
      resultado.append("new Array(\"inpemClUemarginpercentage\", \"" + "" + "\"),\n");

    } else {
      if (region.equals("MH")) {
        double tempOctCalc = (Double.valueOf(octroi) / 100) * cession;
        taxAmount = (ue / (100 + tax)) * tax;
        ueMargin = ue - taxAmount - cession - tempOctCalc;
        ueMarginPer = (ueMargin / ue) * 100;
      } else {
        taxAmount = (ue / (100 + tax)) * tax;
        ueMargin = ue - taxAmount - cession;
        ueMarginPer = (ueMargin / ue) * 100;
      }
      resultado.append("new Array(\"inpemClUemarginamount\", \"" + ueMargin + "\"),\n");
      resultado.append("new Array(\"inpemClUemarginpercentage\", \"" + ueMarginPer + "\"),\n");

    }
    if (cession == 0.0 || pcb == 0.0) {
      resultado.append("new Array(\"inpemClPcbmarginamount\", \"" + "" + "\"),\n");
      resultado.append("new Array(\"inpemClPcbmarginpercentage\", \"" + "" + "\")\n");

    } else {
      if (region.equals("MH")) {
        double tempOctCalc = (Double.valueOf(octroi) / 100) * cession;
        taxAmount = (pcb / (100 + tax)) * tax;
        pcbMargin = pcb - taxAmount - cession - tempOctCalc;
        pcbMarginPer = (pcbMargin / pcb) * 100;
      } else {
        taxAmount = (pcb / (100 + tax)) * tax;
        pcbMargin = pcb - taxAmount - cession;
        pcbMarginPer = (pcbMargin / pcb) * 100;
      }
      resultado.append("new Array(\"inpemClPcbmarginamount\", \"" + pcbMargin + "\"),\n");
      resultado.append("new Array(\"inpemClPcbmarginpercentage\", \"" + pcbMarginPer + "\")\n");

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
