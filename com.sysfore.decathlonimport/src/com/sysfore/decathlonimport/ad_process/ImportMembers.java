/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

public class ImportMembers extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String process = "ImportMembers";// ImportData.processId(this, "ImportProduct");
    if (vars.commandIn("DEFAULT")) {
      System.out.println("Inside the Import members");
      String strTabId = vars.getGlobalVariable("inpTabId", "ImportMembers|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId",
          "ImportMembers|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
    } else if (vars.commandIn("SAVE")) {
      System.out.println("Inside the ImportMembers Save");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
      String strTabId = vars.getRequestGlobalVariable("inpTabId",
          "ImportMembers|tabId");

      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

      OBError myError = new OBError();

      // vars.setMessage(strTabId, myError);

      String result = "";
      String client = vars.getClient();
      String organistation = vars.getOrg();
      String user = vars.getUser();
      result = ImportMembersData.impmembers(myPool, client, organistation, user);
      myError.setType("Success");
      myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      myError.setMessage(Utility.messageBD(this, result, vars.getLanguage()));
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
      log4j.debug("Output: process ImportFobPriceServlet");
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
        "com/sysfore/decathlonimport/ad_process/ImportMembers").createXmlDocument();
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
    return "Servlet ImportFobPriceServlet";
  } // end of getServletInfo() method
}
