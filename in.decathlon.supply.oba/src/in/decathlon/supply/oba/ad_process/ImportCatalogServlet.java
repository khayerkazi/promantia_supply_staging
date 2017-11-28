package in.decathlon.supply.oba.ad_process;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.UtilSql;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ImportCatalogServlet extends HttpSecureAppServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger LOG=Logger.getLogger(ImportCatalogServlet.class);
	public void init(ServletConfig config) {
		    super.init(config);
		    boolHist = false;
		  }

		  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
		      ServletException {
		    VariablesSecureApp vars = new VariablesSecureApp(request);

		    String process = "ImportCatalogServlet";
		    if (vars.commandIn("DEFAULT")) {
		      LOG.debug("vars.commandIn : DEFAULT");
		      String strTabId = vars.getGlobalVariable("inpTabId", "ImportCatalogServlet|tabId");
		      String strWindowId = vars.getGlobalVariable("inpwindowId", "ImportCatalogServlet|windowId");
		      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
		      printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
		    } else if (vars.commandIn("SAVE")) {
		      LOG.debug("vars.commandIn : SAVE");
		      String strTabId = vars.getRequestGlobalVariable("inpTabId", "ImportCatalogServlet|tabId");

		      @SuppressWarnings("deprecation")
		      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
		      if (strWindowPath.equals(""))
		        strWindowPath = strDefaultServlet;

		      OBError myError = new OBError();

		      String result = "";
		      String client = vars.getClient();
		      String organistation = vars.getOrg();
		      String user = vars.getUser();
		      result = importCatalogRecords(client, organistation, user);
		      if(result.equals("Success - Items:Inserted-0, updated-0 , Models : Inserted -0, updated - 0")) {
		    	  myError.setType("Error");
		    	  myError.setMessage(Utility.messageBD(this, "No records are in validated state!", vars.getLanguage()));
		    	  myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
		      } else {
		    	  myError.setType("Success");
		    	  myError.setMessage(Utility.messageBD(this, result, vars.getLanguage()));
		    	  myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
		      }
//		      myError.setType("Success");
//		      myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
//		      myError.setMessage(Utility.messageBD(this, result, vars.getLanguage()));
		      vars.setMessage(strTabId, myError);
		      LOG.debug("Result is " + result);
		      printPageClosePopUp(response, vars, strWindowPath);
		    } else
		      pageErrorPopUp(response);
		  }

		  private String importCatalogRecords(String ad_client_id, String ad_org_id, String ad_user_id)
		      throws ServletException {
		    String strSql = "";
            strSql = strSql + "SELECT oba_importcatalog(?,?,?) from Dual";
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
		    	  strReturn = UtilSql.getValue(result, "oba_importcatalog");
		      }
		      result.close();
		    } catch (SQLException e) {
		      LOG.error("SQL error in query: " + strSql + "Exception:" + e);
		      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
		          + e.getMessage());
		    } catch (Exception ex) {
		      LOG.error("Exception in query: " + strSql + "Exception:" + ex);
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
		    if (LOG.isDebugEnabled())
		      LOG.debug("Output: process ImportCatalogServlet");
		    String strHelp = "", strDescription = "";
		    String[] discard = { "" };
		    if (strHelp.equals(""))
		      discard[0] = new String("helpDiscard");
		    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
		        "in/decathlon/supply/oba/ad_process/ImportCatalogServlet").createXmlDocument();
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
		    return "Servlet ImportCatalogServlet";
		  } // end of getServletInfo() method


}
