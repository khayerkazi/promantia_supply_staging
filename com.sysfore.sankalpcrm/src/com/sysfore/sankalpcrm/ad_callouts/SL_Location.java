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
package com.sysfore.sankalpcrm.ad_callouts;

 import java.io.IOException;
 import java.io.PrintWriter;
  
 import javax.servlet.ServletConfig;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
  
 import org.openbravo.base.secureApp.HttpSecureAppServlet;
 import org.openbravo.base.secureApp.VariablesSecureApp;
 import org.openbravo.utils.FormatUtilities;
 import org.openbravo.xmlEngine.XmlDocument;
 
 public class SL_Location extends HttpSecureAppServlet {
    private static final long serialVersionUID = 1L;
 
    public void init(ServletConfig config) {
        super.init(config);
        boolHist = false;
    }
  
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        VariablesSecureApp vars = new VariablesSecureApp(request);
 
        if (vars.commandIn("DEFAULT")) {
            // parse input parameters here; the names derive from the column
            // names of the table prepended by inp and stripped of all
            // underscore characters; letters following the underscore character
            // are capitalized; this way a database column named
            // M_PRODUCT_CATEGORY_ID that is shown on a tab will become
            // inpmLocationId html field
            //String strProductName = vars.getStringParameter("inpname");
            String strLocationId = vars.getStringParameter("inpcLocationId");
            try {
                if (strLocationId != null)
                    printPage(response, vars, strLocationId);
            } catch (ServletException ex) {
                pageErrorCallOut(response);
            }
        } else
            pageError(response);
    }
 
    private void printPage(HttpServletResponse response,
            VariablesSecureApp vars, String strLocationId) throws IOException, ServletException {
        log4j.debug("Output: dataSheet");
 
        XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut")
                .createXmlDocument();
 
        // retrieve the actual product category name
        String strCompanyAddress = SLLocationData.select(this, strLocationId);
        // construct the Search Key
        //String generatedSearchKey = FormatUtilities.replaceJS(strProductName
                //.replaceAll(" ", ""))
                //+ "_" + strProductCategoryName.replaceAll(" ", "");
 
        StringBuffer result = new StringBuffer();
        result.append("var calloutName='SL_Location';\n\n");
 
        result.append("var respuesta = new Array(");
        // construct the array, where the first dimension contains the name
        // of the field to be changed and the second one our newly generated
        // value
        result.append("new Array(\"inpcompanyaddress\", \"" + strCompanyAddress + "\")");
        result.append(");");
 
        // inject the generated code
        xmlDocument.setParameter("array", result.toString());
 
        xmlDocument.setParameter("frameName", "appFrame");
 
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println(xmlDocument.print());
        out.close();
    }
 }
