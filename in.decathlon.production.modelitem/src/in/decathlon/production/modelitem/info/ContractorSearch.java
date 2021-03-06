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
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2009 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.decathlon.production.modelitem.info;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.info.SelectorUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SQLReturnObject;
import org.openbravo.erpCommon.utility.TableSQLData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ContractorSearch extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  private static final String[] colNames = { "Value", "Name", "C_Bpartner_ID", "RowKey" };
  private static final RequestFilter columnFilter = new ValueListFilter(colNames);
  private static final RequestFilter directionFilter = new ValueListFilter("asc", "desc");

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strName = vars.getGlobalVariable("inpName", "ContractSearch.name", "");
      String strValue = vars.getGlobalVariable("inpSearchKey", "ContractSearch.searchKey", "");
      printPage(response, vars, strValue, strName);
    } else if (vars.commandIn("STRUCTURE")) {
      printGridStructure(response, vars);
    } else if (vars.commandIn("DATA")) {
      String action = vars.getStringParameter("action");
      log4j.debug("command DATA - action: " + action);

      if (vars.getStringParameter("clear").equals("true")) {
        vars.removeSessionValue("ContractSearch.name");
        vars.removeSessionValue("ContractSearch.value");
        vars.removeSessionValue("ContractSearch.adorgid");
      }
      String strName = vars.getGlobalVariable("inpName", "ContractSearch.name", "");
      String strValue = vars.getGlobalVariable("inpSearchKey", "ContractSearch.searchKey", "");
      String strOrg = vars.getGlobalVariable("inpAD_Org_ID", "ContractSearch.adorgid", "");
      String strNewFilter = vars.getStringParameter("newFilter");
      String strOffset = vars.getStringParameter("offset");
      String strPageSize = vars.getStringParameter("page_size");
      String strSortCols = vars.getInStringParameter("sort_cols", columnFilter);
      String strSortDirs = vars.getInStringParameter("sort_dirs", directionFilter);

      if (action.equalsIgnoreCase("getRows")) { // Asking for data rows
        printGridData(response, vars, strValue, strName, strOrg, strSortCols, strSortDirs,
            strOffset, strPageSize, strNewFilter);
      } else if (action.equalsIgnoreCase("getIdsInRange")) {
        // asking for selected rows
        printGridDataSelectedRows(response, vars, strValue, strName, strOrg, strSortCols,
            strSortDirs);
      } else {
        throw new ServletException("Unimplemented action in DATA request: " + action);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strValue,
      String strName) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Multiple projects seeker Frame Set");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "in/decathlon/production/modelitem/info/ContractorSearch").createXmlDocument();
    if (strValue.equals("")) {
      xmlDocument.setParameter("searchKey", "%");
    } else {
      xmlDocument.setParameter("searchKey", strValue);
    }
    if (strName.equals("")) {
      xmlDocument.setParameter("name", "%");
    } else {
      xmlDocument.setParameter("name", strName);
    }

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("searchKey", strValue);
    xmlDocument.setParameter("name", strName);
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printGridStructure(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print page structure");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/utility/DataGridStructure").createXmlDocument();

    SQLReturnObject[] data = getHeaders(vars);
    String type = "Hidden";
    String title = "";
    String description = "";

    xmlDocument.setParameter("type", type);
    xmlDocument.setParameter("title", title);
    xmlDocument.setParameter("description", description);
    xmlDocument.setData("structure1", data);
    xmlDocument.setParameter("backendPageSize", String.valueOf(TableSQLData.maxRowsPerGridPage));
    response.setContentType("text/xml; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled())
      log4j.debug(xmlDocument.print());
    out.println(xmlDocument.print());
    out.close();
  }

  private SQLReturnObject[] getHeaders(VariablesSecureApp vars) {
    SQLReturnObject[] data = null;
    Vector<SQLReturnObject> vAux = new Vector<SQLReturnObject>();
    // String[] colWidths = { "180", "0", "0" };
    String[] colWidths = { "275", "400", "0", "0" };
    for (int i = 0; i < colNames.length; i++) {
      SQLReturnObject dataAux = new SQLReturnObject();
      dataAux.setData("columnname", colNames[i]);
      dataAux.setData("gridcolumnname", colNames[i]);
      dataAux.setData("adReferenceId", "AD_Reference_ID");
      dataAux.setData("adReferenceValueId", "AD_ReferenceValue_ID");
      dataAux.setData("isidentifier", (colNames[i].equals("rowkey") ? "true" : "false"));
      dataAux.setData("iskey", (colNames[i].equals("RowKey") ? "true" : "false"));
      dataAux.setData("isvisible",
          (colNames[i].equals("C_Project_ID") || colNames[i].equalsIgnoreCase("RowKey") ? "false"
              : "true"));
      String name = Utility.messageBD(this, "MPS_" + colNames[i].toUpperCase(), vars.getLanguage());
      dataAux.setData("name", (name.startsWith("MPS_") ? colNames[i] : name));
      dataAux.setData("type", "string");
      dataAux.setData("width", colWidths[i]);
      vAux.addElement(dataAux);
    }
    data = new SQLReturnObject[vAux.size()];
    vAux.copyInto(data);
    return data;
  }

  private void printGridData(HttpServletResponse response, VariablesSecureApp vars,
      String strValue, String strName, String strOrg, String strOrderCols, String strOrderDirs,
      String strOffset, String strPageSize, String strNewFilter) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: print page rows");
    int page = 0;
    SQLReturnObject[] headers = getHeaders(vars);
    FieldProvider[] data = null;
    String type = "Hidden";
    String title = "";
    String description = "";
    String strNumRows = "0";
    int offset = Integer.valueOf(strOffset).intValue();
    int pageSize = Integer.valueOf(strPageSize).intValue();

    if (headers != null) {
      try {
        // build sql orderBy clause
        String strOrderBy = SelectorUtility.buildOrderByClause(strOrderCols, strOrderDirs);
        page = TableSQLData.calcAndGetBackendPage(vars, "ContractorSearch.currentPage");
        if (vars.getStringParameter("movePage", "").length() > 0) {
          // on movePage action force executing countRows again
          strNewFilter = "";
        }
        int oldOffset = offset;
        offset = (page * TableSQLData.maxRowsPerGridPage) + offset;
        log4j.debug("relativeOffset: " + oldOffset + " absoluteOffset: " + offset);
        if (strNewFilter.equals("1") || strNewFilter.equals("")) { // New
          // filter
          // or
          // first
          // load
          String rownum = "0", oraLimit1 = null, oraLimit2 = null, pgLimit = null;
          pgLimit = TableSQLData.maxRowsPerGridPage + " OFFSET " + offset;

          strNumRows = ContractorSearchData.countRows(this, rownum, strValue, strName,
              Utility.getContext(this, vars, "#User_Client", "ContractorSearch"),
              Utility.getSelectorOrgs(this, vars, strOrg), pgLimit, oraLimit1, oraLimit2);
          vars.setSessionValue("ContractorSearch.numrows", strNumRows);
        } else {
          strNumRows = vars.getSessionValue("ContractorSearch.numrows");
        }

        // Filtering result
        String pgLimit = pageSize + " OFFSET " + offset;
        data = ContractorSearchData.select(this, "1", strValue, strName,
            Utility.getContext(this, vars, "#User_Client", "ContractorSearch"),
            Utility.getSelectorOrgs(this, vars, strOrg), strOrderBy, pgLimit, "", "");

      } catch (ServletException e) {
        log4j.error("Error in print page data: " + e);
        e.printStackTrace();
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(), e.getMessage());
        if (!myError.isConnectionAvailable()) {
          bdErrorAjax(response, "Error", "Connection Error", "No database connection");
          return;
        } else {
          type = myError.getType();
          title = myError.getTitle();
          if (!myError.getMessage().startsWith("<![CDATA["))
            description = "<![CDATA[" + myError.getMessage() + "]]>";
          else
            description = myError.getMessage();
        }
      } catch (Exception e) {
        if (log4j.isDebugEnabled())
          log4j.debug("Error obtaining rows data");
        type = "Error";
        title = "Error";
        if (e.getMessage().startsWith("<![CDATA["))
          description = "<![CDATA[" + e.getMessage() + "]]>";
        else
          description = e.getMessage();
        e.printStackTrace();
      }
    }

    if (!type.startsWith("<![CDATA["))
      type = "<![CDATA[" + type + "]]>";
    if (!title.startsWith("<![CDATA["))
      title = "<![CDATA[" + title + "]]>";
    if (!description.startsWith("<![CDATA["))
      description = "<![CDATA[" + description + "]]>";
    StringBuffer strRowsData = new StringBuffer();
    strRowsData.append("<xml-data>\n");
    strRowsData.append("  <status>\n");
    strRowsData.append("    <type>").append(type).append("</type>\n");
    strRowsData.append("    <title>").append(title).append("</title>\n");
    strRowsData.append("    <description>").append(description).append("</description>\n");
    strRowsData.append("  </status>\n");
    strRowsData.append("  <rows numRows=\"").append(strNumRows)
        .append("\" backendPage=\"" + page + "\">\n");
    if (data != null && data.length > 0) {
      for (int j = 0; j < data.length; j++) {
        strRowsData.append("    <tr>\n");
        for (int k = 0; k < headers.length; k++) {
          strRowsData.append("      <td><![CDATA[");
          String columnname = headers[k].getField("columnname");

          if ((data[j].getField(columnname)) != null) {
            if (headers[k].getField("adReferenceId").equals("32"))
              strRowsData.append(strReplaceWith).append("/images/");
            strRowsData.append(data[j].getField(columnname).replaceAll("<b>", "")
                .replaceAll("<B>", "").replaceAll("</b>", "").replaceAll("</B>", "")
                .replaceAll("<i>", "").replaceAll("<I>", "").replaceAll("</i>", "")
                .replaceAll("</I>", "").replaceAll("<p>", "&nbsp;").replaceAll("<P>", "&nbsp;")
                .replaceAll("<br>", "&nbsp;").replaceAll("<BR>", "&nbsp;"));
          } else {
            if (headers[k].getField("adReferenceId").equals("32")) {
              strRowsData.append(strReplaceWith).append("/images/blank.gif");
            } else
              strRowsData.append("&nbsp;");
          }
          strRowsData.append("]]></td>\n");
        }
        strRowsData.append("    </tr>\n");
      }
    }
    strRowsData.append("  </rows>\n");
    strRowsData.append("</xml-data>\n");

    response.setContentType("text/xml; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled())
      log4j.debug(strRowsData.toString());
    out.print(strRowsData.toString());
    out.close();
  }

  /**
   * Prints the response for the getRowsIds action. It returns the rowkey for the identifier column
   * for the list of selected rows [minOffset..maxOffset]
   * 
   */
  private void printGridDataSelectedRows(HttpServletResponse response, VariablesSecureApp vars,
      String strValue, String strName, String strOrg, String strOrderCols, String strOrderDirs)
      throws IOException, ServletException {
    int minOffset = new Integer(vars.getStringParameter("minOffset")).intValue();
    int maxOffset = new Integer(vars.getStringParameter("maxOffset")).intValue();
    log4j.debug("Output: print page ids, minOffset: " + minOffset + ", maxOffset: " + maxOffset);
    String type = "Hidden";
    String title = "";
    String description = "";
    FieldProvider[] data = null;
    FieldProvider[] res = null;
    try {
      // build sql orderBy clause
      String strOrderBy = SelectorUtility.buildOrderByClause(strOrderCols, strOrderDirs);
      String strPage = vars.getSessionValue("ProductMultiple|currentPage", "0");
      int page = Integer.valueOf(strPage);

      int oldMinOffset = minOffset;
      int oldMaxOffset = maxOffset;
      minOffset = (page * TableSQLData.maxRowsPerGridPage) + minOffset;
      maxOffset = (page * TableSQLData.maxRowsPerGridPage) + maxOffset;
      log4j.debug("relativeMinOffset: " + oldMinOffset + " absoluteMinOffset: " + minOffset);
      log4j.debug("relativeMaxOffset: " + oldMaxOffset + " absoluteMaxOffset: " + maxOffset);
      // Filtering result
      // minOffset and maxOffset are zero based so pageSize is difference +1
      int pageSize = maxOffset - minOffset + 1;
      String pgLimit = pageSize + " OFFSET " + minOffset;
      data = ContractorSearchData.select(this, "1", strValue, strName,
          Utility.getContext(this, vars, "#User_Client", "ContractorSearch"),
          Utility.getSelectorOrgs(this, vars, strOrg), strOrderBy, pgLimit, "", "");

      // result field has to be named id -> rename by copy the list
      res = new FieldProvider[data.length];
      for (int i = 0; i < data.length; i++) {
        SQLReturnObject sqlReturnObject = new SQLReturnObject();
        sqlReturnObject.setData("id", data[i].getField("rowkey"));
        res[i] = sqlReturnObject;
      }
    } catch (Exception e) {
      log4j.error("Error obtaining id-list for getIdsInRange", e);
      type = "Error";
      title = "Error";
      if (!e.getMessage().startsWith("<![CDATA["))
        description = "<![CDATA[" + e.getMessage() + "]]>";
    }

    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/utility/DataGridID").createXmlDocument();
    xmlDocument.setParameter("type", type);
    xmlDocument.setParameter("title", title);
    xmlDocument.setParameter("description", description);
    xmlDocument.setData("structure1", res);
    response.setContentType("text/xml; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();
    log4j.debug(xmlDocument.print());
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet that presents the multiple projects seeker";
  } // end of getServletInfo() method
}
