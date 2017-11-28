package com.sysfore.decathlonimport.ad_process;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.UtilSql;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonDefaultData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ImportDCServlet extends HttpSecureAppServlet {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String process = "ImportDCServlet";
    if (vars.commandIn("DEFAULT")) {
      System.out.println("Inside the Default ImportDCServlet");
      String strTabId = vars.getGlobalVariable("inpTabId", "ImportDCServlet|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId", "ImportDCServlet|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
    } else if (vars.commandIn("SAVE")) {
      System.out.println("Inside the ImportDCServlet Save");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "ImportDCServlet|tabId");

      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

      OBError myError = new OBError();

      String result = "";
      String client = vars.getClient();
      String organistation = vars.getOrg();
      String user = vars.getUser();
      result = importDCrecords(client, organistation, user);
      myError.setType("Success");
      myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      myError.setMessage(Utility.messageBD(this, result, vars.getLanguage()));
      vars.setMessage(strTabId, myError);
      System.out.println("Result is " + result);
      printPageClosePopUp(response, vars, strWindowPath);
    } else
      pageErrorPopUp(response);
  }

  private String importDCrecords(String ad_client_id, String ad_org_id, String ad_user_id)
      throws ServletException {
    String strSql = "";
     strSql = strSql + "SELECT im_import_dc(?,?,?) from Dual";
    //strSql = strSql + "SELECT im_impdc(?,?,?) from Dual";
    //strSql = strSql + "SELECT im_importdc(?,?,?) from Dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = this.getPreparedStatement(strSql);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_client_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_org_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_user_id);

      result = st.executeQuery();
      if (result.next()) {
        //strReturn = UtilSql.getValue(result, "im_impdc");
    	 // strReturn = UtilSql.getValue(result, "im_importdc");
    	  strReturn = UtilSql.getValue(result, "im_import_dc");
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        this.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);

  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strProcessId, String strWindowId, String strTabId, String strDeleteOld)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: process ImportDCServlet");
    ActionButtonDefaultData[] data = null;
    String strHelp = "", strDescription = "";
    String[] discard = { "" };
    if (strHelp.equals(""))
      discard[0] = new String("helpDiscard");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/decathlonimport/ad_process/ImportDCServlet").createXmlDocument();
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("question",
        Utility.messageBD(this, "StartProcess?", vars.getLanguage()));
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
    return "Servlet ImportDCServlet";
  } // end of getServletInfo() method

}
