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

package com.sysfore.storewarehouse.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;
import com.sysfore.storewarehouse.ad_combos.OrgData;

public class StoreReceiptPending extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // BEGIN Verify privileges to access this report - deprecated in 2.40
    // if (!Utility.hasProcessAccess(this, vars, "", "StoreReceiptPending")) {
    // bdError(response, "AccessTableNoView", vars.getLanguage());
    // return;
    // }
    // END Verify privileges to access this report

    // BEGIN Controller of actions
    if (vars.commandIn("DEFAULT")) {
      // BEGIN Parse parameters passed by the form
      String strclBrandId = vars.getStringParameter("inpBrand", "");
      String stradOrgId = vars.getStringParameter("inpadOrgId", "");
      String strmWarehouseId = vars.getStringParameter("inpWarehouse", "");
      String strDateFrom = vars.getStringParameter("inpDateFrom", "");
      String strDateTo = vars.getStringParameter("inpDateTo", "");
      String strDocumentNo = vars.getStringParameter("inpDocumentNo", "");
      // END Parse parameters passed by the form
      // Hand over the response generation to a subroutine
      printPageDataSheet(response, vars, strclBrandId, stradOrgId, strmWarehouseId, strDateFrom,
          strDateTo, strDocumentNo, "DEFAULT");

    } else if (vars.commandIn("FIND")) {
      // BEGIN Parse parameters passed by the form
      String strclBrandId = vars.getStringParameter("inpBrand", "");
      String stradOrgId = vars.getStringParameter("inpadOrgId", "");
      String strmWarehouseId = vars.getStringParameter("inpWarehouse", "");
      String strDateFrom = vars.getStringParameter("inpDateFrom", "");
      String strDateTo = vars.getStringParameter("inpDateTo", "");
      String strDocumentNo = vars.getStringParameter("inpDocumentNo", "");
      // END Parse parameters passed by the form
      // Hand over the response generation to a subroutine
      printPageDataSheet(response, vars, strclBrandId, stradOrgId, strmWarehouseId, strDateFrom,
          strDateTo, strDocumentNo, "FIND");
    } else if (vars.commandIn("GENERATE")) {
      System.out.println("Enter the above Generate");
      // BEGIN Parse parameters passed by the form
      String strswSrequisitionId = vars.getRequiredInStringParameter("inpswSrequisitionId",
          IsIDFilter.instance);
      String strRequisitionLine = vars.getRequiredInStringParameter("inpRequisitionLine",
          IsIDFilter.instance);
      String strclBrandId = vars.getStringParameter("inpBrand", "");
      String stradOrgId = vars.getStringParameter("inpadOrgId", "");
      String strmWarehouseId = vars.getStringParameter("inpWarehouse", "");
      String strDateFrom = vars.getStringParameter("inpDateFrom", "");
      String strDateTo = vars.getStringParameter("inpDateTo", "");
      String strDocumentNo = vars.getStringParameter("inpDocumentNo", "");

      String result = "";
      if (strswSrequisitionId.startsWith("("))
        strswSrequisitionId = strswSrequisitionId.substring(1, strswSrequisitionId.length() - 1);
      if (!strswSrequisitionId.equals(""))
        strswSrequisitionId = Replace.replace(strswSrequisitionId, "'", "");

      System.out.println("Requsition ID Before Processing " + strswSrequisitionId);
      StringTokenizer st = new StringTokenizer(strswSrequisitionId, ",", false);

      while (st.hasMoreTokens()) {

        String myToken = st.nextToken().trim();

    StoreReceiptPending1Data[] data = StoreReceiptPending1Data.selectline1(this, myToken);
        String strDateReceipt = "";
        strDateReceipt = vars.getStringParameter("inpDateReceipt" + data[0].adOrgId);
        System.out.println("IN GENERATE: DateReceipt : " + strDateReceipt);

        StoreReceiptPending1Data.processRequisition(this, myToken);
        StoreReceiptPending1Data[] lines = null;
        lines = StoreReceiptPending1Data.selectReqLine(this, myToken);
        if (lines != null) {
          for (int i = 0; i < lines.length; i++) {
            String warehousefrom = lines[i].wh;
            System.out.println("warehousefrom : " + warehousefrom);      
            String enteredQty = vars.getNumericParameter("inpQtyphysical" + lines[i].id);
            String warehouse = vars.getStringParameter("inpWarehouse" + lines[i].id);
            String user = vars.getUser();
            System.out.println("user : " + user);

           StoreReceiptPending1Data.updateLines(this, enteredQty.trim(), warehouse.trim(), strDateReceipt.trim(),
                lines[i].id.trim(), user.trim());
            System.out.println("start " + enteredQty.trim()+" "+warehouse.trim()+" "+strDateReceipt.trim()+
" "+lines[i].id.trim()+"end");
          }
        }

      }

      printPageDataSheet(response, vars, strclBrandId, stradOrgId, strmWarehouseId, strDateFrom,
          strDateTo, strDocumentNo, "GENERATE");
    } else
      pageError(response);
    // END Controller of actions
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strclBrandId, String stradOrgId, String strmWarehouseId, String strDateFrom,
      String strDateTo, String strDocumentNo, String commandIn) throws IOException,
      ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    String orgid=vars.getOrg();
    // BEGIN Retrieve invoice data for the selected timespan
    StoreReceiptPending1Data[] data = StoreReceiptPending1Data.selectline(this, orgid, strDateFrom, strDateTo);
    // END Retrieve invoice data for the selected timespan
    // BEGIN Initialize the UI template
    String clientId = vars.getClient();
    System.out.println("clientId : " + clientId);
    
    //String warehouse = StoreReceiptPending1Data.selectLocator(this, clientId);
    //System.out.println("warehouse : " + warehouse);
    //int whlength = warehouse.length();
    //System.out.println("whlength : " + whlength);    
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/storewarehouse/ad_forms/StoreReceiptPending").createXmlDocument();
    // END Initialize the UI template
    log4j.debug("XML LOADED");

    // BEGIN Set up and pass core UI parameters to the template
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.storewarehouse.ad_forms.StoreReceiptPending");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "StoreReceiptPending.html", classInfo.id, classInfo.type, strReplaceWith, tabs
              .breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "StoreReceiptPending.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");
    // END Set up and pass core UI parameters to the template
    log4j.debug("UI parameters set");

    // BEGIN Prepare and set empty toolbar
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "StoreReceiptPending", false, "",
        "", "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    // END Prepare empty toolbar
    log4j.debug("TOOLBAR SET");

    // BEGIN Check, show & remove message
    /*
     * OBError myMessage = vars.getMessage("StoreReceiptPending");
     * vars.removeMessage("StoreReceiptPending"); if (myMessage!=null) {
     * xmlDocument.setParameter("messageType", myMessage.getType());
     * xmlDocument.setParameter("messageTitle", myMessage.getTitle());
     * xmlDocument.setParameter("messageMessage", myMessage.getMessage()); }
     */
    // END Check, show & remove message
    log4j.debug("MESSAGE SET");

    // BEGIN Set parameters that need to be redisplayed on refresh

    xmlDocument.setParameter("Brand", strclBrandId);
    xmlDocument.setParameter("Org", stradOrgId);
    //xmlDocument.setParameter("Warehouse", strmWarehouseId);
    //xmlDocument.setParameter("mWarehouseId", warehouse); 
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("DocumentNo", strDocumentNo);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("date", StoreReceiptPending1Data.selectDate(this, clientId));
    xmlDocument.setData("reportAD_Org_ID", "liststructure", OrgData.selectCombo(this, vars.getOrg()));

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "cl_Brand_ID", "",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "StoreReceiptPending"), Utility.getContext(this, vars, "#User_Client",
              "StoreReceiptPending"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "StoreReceiptPending",
          strclBrandId);
      xmlDocument.setData("reportCL_Brand_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    
 /* try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "ad_Org_ID", "",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "StoreReceiptPending"), Utility.getContext(this, vars, "#User_Client",
              "StoreReceiptPending"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData,"StoreReceiptPending", "");
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }*/

    System.out.println("****************************************************************************\n");
    //int k = 0;
    //for (k = 0; k < data.length; k++) {
     //String org = data[k].org;
     //System.out.println("org : " + org);
    //String warehouse = StoreReceiptPending1Data.selectLocator(this, org);
    String orgId = vars.getOrg();
    System.out.println("orgId : " + orgId);
    String warehouse = StoreReceiptPending1Data.selectLocator(this, orgId);
    System.out.println("warehouse : " + warehouse);
    int whlength = warehouse.length();
    System.out.println("whlength : " + whlength); 

    xmlDocument.setParameter("mWarehouseId", warehouse);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_Warehouse_ID",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "StoreReceiptPending"), Utility.getContext(this, vars, "#User_Client",
              "StoreReceiptPending"), 0);

      Utility.fillSQLParameters(this, vars, null, comboTableData, "StoreReceiptPending", StoreReceiptPending1Data.selectLocator(this, orgId));
      xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    } 
 // }
    System.out.println("****************************************************************************\n");

//xmlDocument.setData("reportM_Warehouse_ID","liststructure",StoreReceiptPending1Data.selectLocator(this,data[0].adOrgId));


    // If command is "GENERATE" i.e process button STARTS
    if (commandIn.equals("GENERATE")) {

      // ///////////////////////////////////////////////////////
      Connection con = null;
      try {
        con = this.getTransactionConnection();
      } catch (Exception e) {

      }
      System.out.println("<---------Generate Starts Here-------->");
      String strswSreqLineId = vars.getRequiredInStringParameter("inpRequisitionLine",
          IsIDFilter.instance);
      String strswSreqHeaderId = vars.getRequiredInStringParameter("inpswSrequisitionId",
          IsIDFilter.instance);

      // result = StoreReceiptPending1Data.processRequisition(this, strswSrequisitionId);

      // /////////////////////////////////////////////////////////

      StringBuffer html = new StringBuffer();
      if (strswSreqLineId.startsWith("("))
        strswSreqLineId = strswSreqLineId.substring(1, strswSreqLineId.length() - 1);
      if (!strswSreqLineId.equals("")) {
        strswSreqLineId = Replace.replace(strswSreqLineId, "'", "");

        if (strswSreqHeaderId.startsWith("("))
          strswSreqHeaderId = strswSreqHeaderId.substring(1, strswSreqHeaderId.length() - 1);

        int length = strswSreqHeaderId.length();
        // System.out.println("length of strswSreqHeaderId" + strswSreqHeaderId);

        // System.out.println(length);
        if (!strswSreqHeaderId.equals("")) {
          String strswSrequsisitionHeaderId = Replace.replace(strswSreqHeaderId, "'", "");
          String strswSreqHeader = Replace.replace(strswSrequsisitionHeaderId, ",", "");
          strswSreqHeaderId = Replace.replace(strswSreqHeader, " ", "");

          int length1 = strswSreqHeaderId.length();

          OBError myError = new OBError();

          // vars.setMessage(strTabId, myError);

          myError.setType("Success");
          myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
          myError.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
          // vars.setMessage(strTabId, myError);
        }

        StringTokenizer st1 = new StringTokenizer(strswSreqLineId, ",", false);
        html.append("\nfunction insertData() {\n");
        while (st1.hasMoreTokens()) {
          String strRequisitionlineId = st1.nextToken().trim();
          int i = 0;
          for (i = 0; i < data.length; i++) {
            if (data[i].id.equals(strRequisitionlineId)) {
              String strLocator = vars.getStringParameter("inpmLocatorId" + strRequisitionlineId);
              String strDateShipped = vars.getStringParameter("inpDateReceipt" + data[0].adOrgId);
              html.append("document.getElementsByName(\"" + "inpQtyphysical" + strRequisitionlineId
                  + "\"" + ")[0].value = " + "'"
                  + vars.getStringParameter("inpQtyphysical" + strRequisitionlineId) + "';\n");
              html.append("document.getElementsByName(\"" + "inpmLocatorId" + strRequisitionlineId
                  + "\"" + ")[0].value = " + "'" + strLocator + "';\n");
              html.append("document.getElementsByName(\"" + "inpmLocatorId_D"
                  + strRequisitionlineId + "\"" + ")[0].value = '"
                  + StoreReceiptPending1Data.selectLocator(this, strLocator) + "';\n");
              html.append("document.getElementsByName(\"" + "inpDateShipped" + data[0].adOrgId
                  + "\"" + ")[0].value = '" + strDateShipped + "';\n");
              html.append("setCheckedValue(document.frmMain.inpRequisitionLine, '"
                  + strRequisitionlineId + "');\n");
              break;
            }
          }
        }
        html.append("}\n");

      }
      xmlDocument.setParameter("script", html.toString());
    }
    // If command is "GENERATE" i.e process button ENDS

    // END Set parameters that need to be redisplayed on refresh
    log4j.debug("FILTER PARAMETERS SET");

    xmlDocument.setData("structure1", data);
    log4j.debug("STRUCTURE SET");
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "StoreReceiptPending controller servlet made specifically for CBD Training";
  } // end of getServletInfo() method
}
