/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2009 Openbravo S.L.U.
 ******************************************************************************
 */
package com.sysfore.decathlonimport.ad_process;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonDefaultData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ValidateFobPriceServlet extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String process = "ValidateFobPrice";// ImportData.processId(this, "ImportProduct");
    if (vars.commandIn("DEFAULT")) {
      System.out.println("Inside the Default Product");
      String strTabId = vars.getGlobalVariable("inpTabId", "ValidateFobPriceServlet|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId",
          "ValidateFobPriceServlet|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
    } else if (vars.commandIn("SAVE")) {
      System.out.println("Inside the FobItem Save");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
      String strTabId = vars.getRequestGlobalVariable("inpTabId",
          "ValidateFobPriceServlet|tabId");

      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

      OBError myError = new OBError();

      // vars.setMessage(strTabId, myError);

      String result = "";
      String client = vars.getClient();
      // String organistation = vars.getOrg();
      // String user = vars.getUser();
      result = ValidateFobPriceData.validateFobPricelist(this, client);
      if (result != null && result.equalsIgnoreCase("SUCCESS")) {
        myError.setType("Success");
        myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
        myError.setMessage(Utility.messageBD(this, "Validation Successful", vars.getLanguage()));
      } else {
        myError.setType("Error");
        myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
        myError.setMessage(Utility.messageBD(this,
            "Validation Failure ! Please Check Error Messages.", vars.getLanguage()));

      }

      vars.setSessionValue(strTabId + "|ImportFOBPricelistD287B07B03D4487789D99AF213C59C5D.view", "");
      vars.setMessage(strTabId, myError);
      System.out.println("Result is " + result);
      printPageClosePopUp(response, vars, strWindowPath);
    } else
      pageErrorPopUp(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strProcessId, String strWindowId, String strTabId, String strDeleteOld)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: process ValidateFobPriceServlet");
    ActionButtonDefaultData[] data = null;
    String strHelp = "", strDescription = "";
    /*
     * if (vars.getLanguage().equals("en_US")) { // data = ActionButtonDefaultData.select(this,
     * strProcessId); System.out.println("Inside English " + data.length); } else data =
     * ActionButtonDefaultData.selectLanguage(this, vars.getLanguage(), strProcessId); /* if (data
     * != null && data.length != 0) { strDescription = data[0].description; strHelp = data[0].help;
     * System.out.println(" StrDescription " + strDescription); System.out.println(" Strhelp " +
     * strHelp); }
     */
    String[] discard = { "" };
    if (strHelp.equals(""))
      discard[0] = new String("helpDiscard");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/decathlonimport/ad_process/ValidateFobPriceServlet").createXmlDocument();
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("question", Utility.messageBD(this, "StartProcess?", vars
        .getLanguage()));
    xmlDocument.setParameter("description", strDescription);
    xmlDocument.setParameter("help", strHelp);
    xmlDocument.setParameter("windowId", strWindowId);
    xmlDocument.setParameter("tabId", strTabId);
    xmlDocument.setParameter("deleteOld", strDeleteOld);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet ValidateFobPriceServlet";
  } // end of getServletInfo() method
}
