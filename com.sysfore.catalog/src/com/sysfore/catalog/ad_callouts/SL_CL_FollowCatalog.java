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
import javax.xml.crypto.Data;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_CL_FollowCatalog extends HttpSecureAppServlet {
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

      //String cessionPrice = vars.getStringParameter("inpemClCcunitprice");
      String unitPrice = vars.getStringParameter("inpemClCcunitprice");
      String uePrice = vars.getStringParameter("inpemClCcueprice");
	  String pcbPrice = vars.getStringParameter("inpemClCcpcbprice");	  
      String strProduct = vars.getStringParameter("inpmProductId");
      String strTabId = vars.getStringParameter("inpTabId");
      String strPricelistVersionId = vars.getStringParameter("inpmPricelistVersionId");
      String strFollowCatalog = vars.getStringParameter("inpemClFollowcatalog");
      

      try {
        printPage(response, vars, strChanged, unitPrice,uePrice, pcbPrice,strProduct, strTabId,strPricelistVersionId,strFollowCatalog);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String unitPrice,String uePrice,String pcbPrice, String strProduct, String strTabId,String strPricelistVersionId,String strFollowCatalog) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
       
        String basePricelistId = "";
        //System.out.println("strFollowCatalog:"+strFollowCatalog);
        if (log4j.isDebugEnabled())
      log4j.debug("strFollowCatalog:"+strFollowCatalog);
        basePricelistId = CLFollowCatalogData.basepricelistversion(this,strPricelistVersionId);
        	//SLCLPriceMarginData.select(this, strProduct);
        
        	//if(basePricelistId != null && !basePricelistId.equals("")){        		
        		CLFollowCatalogData[] data =CLFollowCatalogData.price(this, basePricelistId, strProduct);
        	//}
        	//CLFollowCatalogData[] data1 = CLFollowCatalogData.price(this, basePricelistId, strProduct);
      	
    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_CL_FollowCatalog';\n\n");
    resultado.append("var respuesta = new Array(");
	
   if(data.length > 0 && strFollowCatalog.equalsIgnoreCase("Y")){
		resultado.append("new Array(\"inpemClCcunitprice\", \"" + ""+data[0].emClCcunitprice + "\"),\n");
		resultado.append("new Array(\"inpemClCcueprice\", \"" + data[0].emClCcueprice + "\"),\n");	
		resultado.append("new Array(\"inpemClCcpcbprice\", \"" + data[0].emClCcpcbprice + "\")\n");
   }
/*else{
	   resultado.append("new Array(\"inpemClCcunitprice\", \"" + ""+"0.00"+ "\"),\n");
		resultado.append("new Array(\"inpemClCcueprice\", \"" + "0.00" + "\"),\n");	
		resultado.append("new Array(\"inpemClCcpcbprice\", \"" + "0.00" + "\")\n");
   }*/
	  

    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
