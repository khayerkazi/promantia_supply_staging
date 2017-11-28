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
 * All portions are Copyright (C) 2001-2009 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.decathlon.production.modelitem.searchItem;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ProductionSearchItem extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String strBrand = vars.getRequestGlobalVariable("inpclBrandId", "ProductionSearchItem|brand");

    String strModelName = vars.getRequestGlobalVariable("inpModelName",
        "ProductionSearchItem|modelname");

    String strModelCode = vars.getRequestGlobalVariable("inpModelCode",
        "ProductionSearchItem|modelcode");

    String strpdtName = vars.getRequestGlobalVariable("inppdtName", "ProductionSearchItem|pdtname"+"");

    String[] arrSupplier = vars.getMultiValueStringParameter("inpcBPartnerId_IN", false, null);

    String strProcess = vars.getRequestGlobalVariable("inpprmiProcessId",
        "ProductionSearchItem|Process");

    String strComponentLabel = vars.getRequestGlobalVariable("prmicomponentlabelId",
        "ProductionSearchItem|ComponentLabel");

    String strLifestage = vars.getRequestGlobalVariable("inpemclLifestage",
        "ProductionSearchItem|lifestage");

    if (vars.commandIn("DEFAULT", "DIRECT")) {

      setHistoryCommand(request, "DIRECT");
      printPageDataSheet(response, vars, strBrand, strModelName, strModelCode, strpdtName,
          arrSupplier, strProcess, strComponentLabel, strLifestage, "DEFAULT");
    } else if (vars.commandIn("FIND")) {

      vars.setMessage("ProductionSearchItem", processSearchItem(response, vars, strBrand,
          strpdtName, strModelName, strModelCode, arrSupplier, strProcess, strComponentLabel,
          strLifestage));

      printPageDataSheet(response, vars, strBrand, strModelName, strModelCode, strpdtName,
          arrSupplier, strProcess, strComponentLabel, strLifestage, "FIND");

    } else if (vars.commandIn("PRINT_XLS")) {

      setHistoryCommand(request, "DIRECT");
      printPageDataXls(response, vars, strBrand, strModelName, strModelCode, strpdtName,
          arrSupplier, strProcess, strComponentLabel, strLifestage);
    } else
      pageError(response);
  }

  void printPageDataXls(HttpServletResponse response, VariablesSecureApp vars, String strBrand,
      String strModelName, String strModelCode, String strName, String[] arrSupplier,
      String strProcess, String strComponentLabel, String strLifestage) throws IOException,
      ServletException {

    String strReportName = null;
    ProductionSearchItemData[] data = null;

    data = productionSearch(vars, strBrand, strModelName, strModelCode, strName, arrSupplier,
        strProcess, strComponentLabel, strLifestage);
    String strOutput = "Xls";

    strReportName = "@basedesign@/in/decathlon/production/modelitem/searchItem/reportSearchProductionItems.jrxml";
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    renderJR(vars, response, strReportName, "xls", parameters, data, null);

  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strBrand,
      String strModelName, String strModelCode, String strName, String[] arrSupplier,
      String strProcess, String strComponentLabel, String strLifestage, String command)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()) {
    }
    XmlDocument xmlDocument;

    ProductionSearchItemData[] data = null;

    if (data == null || data.length == 0) {
      data = ProductionSearchItemData.set();

    }
    if (vars.commandIn("DEFAULT")) {

      xmlDocument = xmlEngine.readXmlTemplate(
          "in/decathlon/production/modelitem/searchItem/ProductionSearchItem").createXmlDocument();
      data = ProductionSearchItemData.set();
    } else {
      data = productionSearch(vars, strBrand, strModelName, strModelCode, strName, arrSupplier,
          strProcess, strComponentLabel, strLifestage);
      xmlDocument = xmlEngine.readXmlTemplate(
          "in/decathlon/production/modelitem/searchItem/ProductionSearchItem").createXmlDocument();
    }
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ProductionSearchItem", false, "", "",
        "", false, "searchItem", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "in/decathlon/production/modelitem/searchItem/ProductionSearchItem");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ProductionSearchItem.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ProductionSearchItem.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    /*
     * { OBError myMessage = vars.getMessage("ReportSearchItem");
     * vars.removeMessage("ReportSearchItem"); if (myMessage != null) {
     * xmlDocument.setParameter("messageType", myMessage.getType());
     * xmlDocument.setParameter("messageTitle", myMessage.getTitle());
     * xmlDocument.setParameter("messageMessage", myMessage.getMessage()); } }
     */

    // xmlDocument.setParameter("calendar", vars.getLanguage().substring(0,
    // 2));
    // xmlDocument.setParameter("directory", "var baseDirectory = \"" +
    // strReplaceWith + "/\";\n");
    // xmlDocument.setParameter("paramLanguage", "defaultLang=\"" +
    // vars.getLanguage() + "\";");
    xmlDocument.setParameter("ModelCode", strModelCode);
    xmlDocument.setParameter("ModelName", strModelName);
    xmlDocument.setParameter("pdtname", strName);
    // xmlDocument.setParameter("Supplier", arrSupplier);
    // TODO: Return array100000009
    xmlDocument.setParameter("Process", strProcess);
    xmlDocument.setParameter("Brand", strBrand);
    xmlDocument.setParameter("Lifestage", strLifestage);
    xmlDocument.setParameter("ComponentLabel", strComponentLabel);

    // BEGIN check for any pending message to be shown and clear it from the
    // session
    OBError myMessage = vars.getMessage("ProductionSearchItem");
    vars.removeMessage("ProductionSearchItem");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    // END check for any pending message to be shown and clear it from the
    // session

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "prmi_process_id",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ProductionSearchItem"),
          Utility.getContext(this, vars, "#User_Client", "ProductionSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductionSearchItem",
          strProcess);
      xmlDocument.setData("reportprmi_process_id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_brand_id", "",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ProductionSearchItem"), Utility
              .getContext(this, vars, "#User_Client", "ProductionSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductionSearchItem", strBrand);
      xmlDocument.setData("reportcl_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "prmi_componentlabel_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ProductionSearchItem"), Utility.getContext(this, vars, "#User_Client",
              "ProductionSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductionSearchItem",
          strComponentLabel);
      xmlDocument.setData("reportprmi_componentlabel_id", "liststructure", comboTableData
          .select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strLifestage);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "EM_Cl_Lifestage",
          "8E405997F12A4DB79252DDF3CAC421D6", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "ProductionSearchItem"), Utility.getContext(this, vars,
              "#User_Client", "ProductionSearchItem"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ProductionSearchItem",
          strLifestage);
      xmlDocument.setData("reportEM_cl_LIFESTAGE", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (arrSupplier.length != 0)
      try {
        String strsupplier;
        strsupplier = "('" + StringUtils.join(arrSupplier, "','") + "')";

        xmlDocument.setData("reportCBPartnerId_IN", "liststructure", SelectorUtilityData
            .selectBpartner(this, Utility.getContext(this, vars, "#AccessibleOrgTree", ""), Utility
                .getContext(this, vars, "#User_Client", ""), strsupplier));

      } catch (Exception ex) {
        throw new ServletException(ex);
      }

    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processSearchItem(HttpServletResponse response, VariablesSecureApp vars,
      String strBrand, String strName, String strModelName, String strModelCode,
      String[] arrSupplier, String strProcess, String strComponentLabel, String strLifestage) {
    ProductionSearchItemData[] data1 = null;

    OBError myMessage = new OBError();
    try {
      data1 = productionSearch(vars, strBrand, strModelName, strModelCode, strName, arrSupplier,
          strProcess, strComponentLabel, strLifestage);

      int datalength = data1.length;

      if (datalength == 0) {
        myMessage.setTitle("Information");
        myMessage.setType("INFO");
      }
      myMessage.setMessage(Utility.messageBD(this, "CL_NoData", vars.getLanguage()));

    } catch (Exception e) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "CL_NoData");
    }
    return myMessage;
  }

  private ProductionSearchItemData[] productionSearch(VariablesSecureApp vars, String strBrand,
      String strModelName, String strModelCode, String strName, String[] arrSupplier,
      String strProcess, String strComponentLabel, String strLifestage) throws ServletException {
    ProductionSearchItemData[] data;
    String strSupplier;
    if (arrSupplier.length == 0)
      strSupplier = "";
    else

      strSupplier = " AND exists ( select 1 from prmi_subcontractor sc  where sc.c_bpartner_id in ('"
          + StringUtils.join(arrSupplier, "','") + "') and sc.cl_model_id = m.cl_model_id) ";
    data = ProductionSearchItemData.select(this, Utility.getContext(this, vars, "#User_Client",
        "ProductionSearchItem"), Utility.getContext(this, vars, "#AccessibleOrgTree",
        "ProductionSearchItem"), strBrand, strName, strModelName, strModelCode, strProcess,
        strComponentLabel, strLifestage, strSupplier);

    return data;

  }

  public String getServletInfo() {
    return "Servlet ProductionSearchItem. This Servlet was made by Pablo Sarobe";
  }
}
