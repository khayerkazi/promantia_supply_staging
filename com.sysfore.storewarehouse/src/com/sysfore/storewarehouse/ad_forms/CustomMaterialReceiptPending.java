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
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.businessUtility.WindowTabsData;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;



public class CustomMaterialReceiptPending extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
          System.out.println("I am in defult==========");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "MaterialReceiptPending|DateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "MaterialReceiptPending|DateTo", "");
       String strCDD = vars.getGlobalVariable("inpCDD", "MaterialReceiptPending|DateCDD",
          "");
      String strEDD = vars.getGlobalVariable("inpEDD", "MaterialReceiptPending|DateEDD", "");
      String strDocumentNo = vars.getGlobalVariable("inpDocumentNo",
          "MaterialReceiptPending|DocumentNo", "");
      String strC_BPartner_ID = vars.getGlobalVariable("inpcBpartnerId",
          "MaterialReceiptPending|C_BPartner_ID", "");
      String strAD_Org_ID = vars.getGlobalVariable("inpadOrgId",
          "MaterialReceiptPending|AD_Org_ID", vars.getOrg());
       String strmWarehouseId = vars.getStringParameter("inpWarehouse", "");
      vars.setSessionValue("MaterialReceiptPending|isSOTrx", "Y");
      if (strDocumentNo.equals(""))
        strDocumentNo += "%";
      printPageDataSheet(response, vars, strC_BPartner_ID, strAD_Org_ID, strDateFrom, strDateTo, strCDD, strEDD, strmWarehouseId,
          strDocumentNo, "DEFAULT");
      System.out.println("I am in defult1==========");
    } else if (vars.commandIn("FIND")) {
          System.out.println("I am in find==========");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MaterialReceiptPending|DateFrom");
      String strDateTo = vars
          .getRequestGlobalVariable("inpDateTo", "MaterialReceiptPending|DateTo");
      String strCDD = vars.getGlobalVariable("inpCDD", "MaterialReceiptPending|DateCDD",
          "");
      String strEDD = vars.getGlobalVariable("inpEDD", "MaterialReceiptPending|DateEDD", "");
      String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
          "MaterialReceiptPending|DocumentNo");
      String strC_BPartner_ID = vars.getRequestGlobalVariable("inpcBpartnerId",
          "MaterialReceiptPending|C_BPartner_ID");
      String strAD_Org_ID = vars
          .getGlobalVariable("inpadOrgId", "MaterialReceiptPending|AD_Org_ID");
      String strmWarehouseId = vars.getStringParameter("inpWarehouse", "");
      printPageDataSheet(response, vars, strC_BPartner_ID, strAD_Org_ID, strDateFrom, strDateTo, strCDD, strEDD, strmWarehouseId,
          strDocumentNo, "FIND");
        System.out.println("I am in find1==========");
    } else if (vars.commandIn("GENERATE")) {
          System.out.println("I am in generate==========");
      String strcOrderLineId = vars.getRequiredInStringParameter("inpOrder", IsIDFilter.instance);
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MaterialReceiptPending|DateFrom");
      String strDateTo = vars
          .getRequestGlobalVariable("inpDateTo", "MaterialReceiptPending|DateTo");
      String strCDD = vars.getGlobalVariable("inpCDD", "MaterialReceiptPending|DateCDD",
          "");
      String strEDD = vars.getGlobalVariable("inpEDD", "MaterialReceiptPending|DateEDD", "");
      String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
          "MaterialReceiptPending|DocumentNo");
      String strC_BPartner_ID = vars.getRequestGlobalVariable("inpcBpartnerId",
          "MaterialReceiptPending|C_BPartner_ID");
      String strAD_Org_ID = vars
          .getGlobalVariable("inpadOrgId", "MaterialReceiptPending|AD_Org_ID");
      String strmWarehouseId = vars.getStringParameter("inpWarehouse", "");
      OBError myMessage = processPurchaseOrder(vars, strcOrderLineId);
      vars.setMessage("MaterialReceiptPending", myMessage);
      printPageDataSheet(response, vars, strC_BPartner_ID, strAD_Org_ID, strDateFrom, strDateTo, strCDD, strEDD, strmWarehouseId,
          strDocumentNo, "GENERATE");
        System.out.println("I am in generate1==========");
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strC_BPartner_ID, String strAD_Org_ID, String strDateFrom, String strDateTo, String strCDD, String strEDD, String strmWarehouseId,
      String strDocumentNo, String commandIn) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    String discard[] = { "sectionDetail" };
    XmlDocument xmlDocument = null;

    // String strMessage =
    // vars.getSessionValue("MaterialReceiptPending|message");
    // vars.removeSessionValue("MaterialReceiptPending|message");

    MaterialReceiptPending3Data[] data = null;
    String strTreeOrg = MaterialReceiptPending3Data.treeOrg(this, vars.getClient());
    if (strC_BPartner_ID.equals("") && strAD_Org_ID.equals("")) {
      xmlDocument = xmlEngine.readXmlTemplate(
          "com/sysfore/storewarehouse/ad_forms/MaterialReceiptPending", discard).createXmlDocument();
      data = MaterialReceiptPending3Data.set();
    } else {
      xmlDocument = xmlEngine.readXmlTemplate(
          "com/sysfore/storewarehouse/ad_forms/MaterialReceiptPending").createXmlDocument();
      String strDateFormat = vars.getSessionValue("#AD_SqlDateFormat");
      System.out.println("strCDD========" +strCDD);
      System.out.println("strEDD======" +strEDD);
      //MaterialReceiptPending3Data[] data;
      data = MaterialReceiptPending3Data.selectLines(this, strDateFormat, vars.getLanguage(),
          Utility.getContext(this, vars, "#User_Client", "MaterialReceiptPending"), Tree
              .getMembers(this, strTreeOrg, strAD_Org_ID), strDateFrom, DateTimeData.nDaysAfter(
              this, strDateTo, "1"), strCDD, strEDD, strC_BPartner_ID, strDocumentNo);
    }

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "MaterialReceiptPending", false, "",
        "", "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    OBError myMessage = null;
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_forms.MaterialReceiptPending");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "MaterialReceiptPending.html", classInfo.id, classInfo.type, strReplaceWith, tabs
              .breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "MaterialReceiptPending.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      myMessage = vars.getMessage("MaterialReceiptPending");
      vars.removeMessage("MaterialReceiptPending");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("paramBPartnerId", strC_BPartner_ID);
    xmlDocument.setParameter("paramAdOrgId", strAD_Org_ID);
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateCDD", strCDD);
    xmlDocument.setParameter("dateEDD", strEDD);
    xmlDocument.setParameter("paramDocumentNo", strDocumentNo);
    xmlDocument.setParameter("paramBPartnerDescription", MaterialReceiptPending3Data
        .bPartnerDescription(this, strC_BPartner_ID));
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org Security validation", Utility.getContext(this, vars, "#User_Org",
              "MaterialReceiptPending"), Utility.getContext(this, vars, "#User_Client",
              "MaterialReceiptPending"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "MaterialReceiptPending",
          strAD_Org_ID);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // xmlDocument.setParameter("paramMessage",
    // (strMessage.equals("")?"":"alert('" + strMessage + "');"));
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));

    String orgId = vars.getOrg();
    System.out.println("orgId : " + orgId);
    String warehouse = MaterialReceiptPending6Data.select(myPool);
    System.out.println("warehouse : " + warehouse);
    int whlength = warehouse.length();
    System.out.println("whlength : " + whlength);

     xmlDocument.setParameter("mWarehouseId", warehouse);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_Warehouse_ID",
          "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "MaterialReceiptPending"), Utility.getContext(this, vars, "#User_Client",
              "MaterialReceiptPending"), 0);

      Utility.fillSQLParameters(this, vars, null, comboTableData, "MaterialReceiptPending", MaterialReceiptPending6Data.select(this));
      xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (commandIn.equals("GENERATE") && myMessage != null && !myMessage.getType().equals("Success")) {

      String strcOrderLineId = vars.getRequiredInStringParameter("inpOrder", IsIDFilter.instance);
      StringBuffer html = new StringBuffer();
      if (strcOrderLineId.startsWith("("))
        strcOrderLineId = strcOrderLineId.substring(1, strcOrderLineId.length() - 1);
      if (!strcOrderLineId.equals("")) {
        strcOrderLineId = Replace.replace(strcOrderLineId, "'", "");
        StringTokenizer st = new StringTokenizer(strcOrderLineId, ",", false);
        html.append("\nfunction insertData() {\n");
        while (st.hasMoreTokens()) {
          String strOrderlineId = st.nextToken().trim();
          int i = 0;
          for (i = 0; i < data.length; i++) {
            if (data[i].id.equals(strOrderlineId)) {
              String strLocator = vars.getStringParameter("inpmLocatorId" + strOrderlineId);
              //System.out.println("strmWarehouseId=============" +strmWarehouseId);

              //String strmWarehouseId1 = vars.getStringParameter("inpWarehouse", "");
               // System.out.println("strmWarehouseId1=============" +strmWarehouseId1);

                 //String strmWarehouseId1 = vars.getStringParameter("inpWarehouse" + strOrderlineId);
                  //System.out.println("strmWarehouseId=============" +strmWarehouseId1);
              String strDateReceipt = vars.getStringParameter("inpDateReceipt"
                  + data[0].cBpartnerId);
              html.append("document.getElementsByName(\"" + "inpQtyordered" + strOrderlineId + "\""
                  + ")[0].value = " + "'"
                  + vars.getStringParameter("inpQtyordered" + strOrderlineId) + "';\n");
              html.append("document.getElementsByName(\"" + "inpmLocatorId" + strOrderlineId + "\""
                  + ")[0].value = " + "'" + strLocator + "';\n");
              html.append("document.getElementsByName(\"" + "inpmLocatorId_D" + strOrderlineId
                  + "\"" + ")[0].value = '"
                  + MaterialReceiptPending3Data.selectLocator(this, strLocator) + "';\n");
              html.append("document.getElementsByName(\"" + "inpDateReceipt" + data[0].cBpartnerId
                  + "\"" + ")[0].value = '" + strDateReceipt + "';\n");
              html
                  .append("setCheckedValue(document.frmMain.inpOrder, '" + strOrderlineId + "');\n");
              break;
            }
          }
        }
        html.append("}\n");
      }
      xmlDocument.setParameter("script", html.toString());
    }
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processPurchaseOrder(VariablesSecureApp vars, String strcOrderLineId)
      throws IOException, ServletException {
    //System.out.println("I am in processPurchaseOrder function============");
    String strMessageResult = "";
    String strMessageType = "Success";
    String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "184");
    OBError myMessage = null;
    OBError myMessageAux = new OBError();
    myMessage = new OBError();
    myMessage.setTitle("");
    if (strcOrderLineId.equals("")) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
      return myMessage;
    }

    Connection conn = null;
    try {
      conn = this.getTransactionConnection();
      if (strcOrderLineId.startsWith("("))
        strcOrderLineId = strcOrderLineId.substring(1, strcOrderLineId.length() - 1);
      if (!strcOrderLineId.equals("")) {
        strcOrderLineId = Replace.replace(strcOrderLineId, "'", "");
        StringTokenizer st = new StringTokenizer(strcOrderLineId, ",", false);
        //System.out.println("I am after StringTokenizer======= ");
        String strmInoutId = "";
        String docTargetType = "";
        String docType = "";
        String strDocumentno = "";
        String strDateReceipt = "";
        String strLastBpartnerId = "";
        String strLastOrgId = "";
        int line = 0;

        while (st.hasMoreTokens()) {
          String strOrderlineId = st.nextToken().trim();
          // System.out.println("I am after StringTokenizer======= "+strOrderlineId);
          //Selecting from Orderline
          MaterialReceiptPending3Data[] data = MaterialReceiptPending3Data.select(this,
              strOrderlineId);
          if (!strLastBpartnerId.equals(data[0].cBpartnerId)
              || !strLastOrgId.equals(data[0].adOrgId)) {
            if (!strmInoutId.equals("")) {
              myMessageAux = mInoutPost(conn, vars, strmInoutId);
              strMessageResult += strWindowName + " " + strDocumentno + ": "
                  + myMessageAux.getMessage() + "<br>";
              if (strMessageType.equals("Success"))
                strMessageType = myMessageAux.getType();
              else if (strMessageType.equals("Warning") && myMessageAux.getType().equals("Error"))
                strMessageType = "Error";
            }
            line = 10;
            strmInoutId = SequenceIdData.getUUID();
            docTargetType = MaterialReceiptPending3Data.cDoctypeTarget(this, vars.getClient(),
                data[0].adOrgId);
            docType = MaterialReceiptPending3Data.cDoctypeId(this, docTargetType);
            strDocumentno = Utility.getDocumentNo(this, vars, "", "M_InOut", docTargetType,
                docType, false, true);
            strDateReceipt = vars.getStringParameter("inpDateReceipt" + data[0].cBpartnerId);

            if (strDateReceipt.equals("")) {
              myMessage.setType("Error");
              myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
              myMessage.setMessage(Utility.messageBD(this, "DateReceipt", vars.getLanguage()) + " "
                  + MaterialReceiptPending3Data.bPartnerDescription(this, data[0].cBpartnerId));
              return myMessage;
            }
            try {
              MaterialReceiptPending3Data.insert(conn, this, strmInoutId, vars.getClient(),
                  data[0].adOrgId, "Y", vars.getUser(), vars.getUser(), "N", strDocumentno, "CO",
                  "DR", "N", "N", "N", docTargetType, data[0].description, data[0].cOrderId,
                  data[0].dateordered, "N", "V+", strDateReceipt, strDateReceipt,
                  data[0].cBpartnerId, data[0].cBpartnerLocationId, data[0].mWarehouseId,
                  data[0].poreference, data[0].deliveryrule, data[0].freightcostrule,
                  data[0].freightamt, data[0].deliveryviarule, data[0].mShipperId,
                  data[0].cChargeId, data[0].chargeamt, data[0].priorityrule, "N", "N",
                  data[0].adUserId, data[0].salesrepId, data[0].adOrgtrxId, data[0].cProjectId,
                  data[0].cCampaignId, data[0].cActivityId, data[0].user1Id, data[0].user2Id, "N",
                  "N", "N");
            } catch (ServletException ex) {
              myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
              releaseRollbackConnection(conn);
              return myMessage;
            }
          }
          strLastBpartnerId = data[0].cBpartnerId;
          strLastOrgId = data[0].adOrgId;
          String strQtyordered = vars.getNumericParameter("inpQtyordered" + strOrderlineId);
          String strLocator = vars.getStringParameter("inpmLocatorId" + strOrderlineId);
          String strmWarehouseId = vars.getStringParameter("inpWarehouse" + strOrderlineId);
          System.out.println("strmWarehouseId=============" +strmWarehouseId);
          String strLocatorId = MaterialReceiptPending2Data.select(myPool, strmWarehouseId);
          System.out.println("strLocatorId=============" +strLocatorId);
          String strSequenceLine = SequenceIdData.getUUID();
          MaterialReceiptPendingLinesData[] dataLine = MaterialReceiptPendingLinesData.select(this,
              strOrderlineId);

          try {
            MaterialReceiptPendingLinesData.insert(conn, this, strSequenceLine, vars.getClient(),
                data[0].adOrgId, "Y", vars.getUser(), vars.getUser(), String.valueOf(line),
                dataLine[0].description, strmInoutId, strOrderlineId, strLocatorId,
                dataLine[0].mProductId, dataLine[0].cUomId, strQtyordered, "N",
                dataLine[0].mAttributesetinstanceId, "N", dataLine[0].quantityorder,
                dataLine[0].mProductUomId);
          }
            catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
          line += 10;
           System.out.println("I am before Update1===========");
           System.out.println("strcOrderLineId===========" +strOrderlineId);
           System.out.println("strQtyordered==========" +strQtyordered+"hh");
           /*try{MaterialReceiptPending1Data.update1(this, strQtyordered, strOrderlineId);
           }catch(Exception e) {
               e.printStackTrace();
           }*/
           System.out.println("I am passing Update1===========");

           MaterialReceiptPending1Data[] pendingdata = MaterialReceiptPending1Data.select(myPool, strOrderlineId);
           System.out.println("pendingdata.length=========" + pendingdata.length);
           String strcOrderId = pendingdata[0].cOrderId;
           System.out.println("strcOrderId=======" + strcOrderId);
           //MaterialReceiptPending1Data.update(myPool, strcOrderId);
           System.out.println("I am passing update======");
        }
        myMessageAux = mInoutPost(conn, vars, strmInoutId);
        strMessageResult += strWindowName + " " + strDocumentno + ": " + myMessageAux.getMessage();
        if (strMessageType.equals("Success"))
          strMessageType = myMessageAux.getType();
        else if (strMessageType.equals("Warning") && myMessageAux.getType().equals("Error"))
          strMessageType = "Error";
      }
      releaseCommitConnection(conn);
      myMessage.setType(strMessageType);
      myMessage.setTitle(myMessageAux.getTitle());
      myMessage.setMessage(strMessageResult);
    } catch (Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    }
    return myMessage;
  }

  private OBError mInoutPost(Connection conn, VariablesSecureApp vars, String strmInoutId)
      throws IOException, ServletException, SQLException {
    String pinstance = SequenceIdData.getUUID();

    OBError myMessage = null;
    myMessage = new OBError();
    try {
      PInstanceProcessData.insertPInstance(this, pinstance, "109", strmInoutId, "N",
          vars.getUser(), vars.getClient(), vars.getOrg());
    } catch (ServletException ex) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
      releaseRollbackConnection(conn);
      return myMessage;
    }
    MaterialReceiptPending3Data.mInoutPost0(conn, this, pinstance);

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(conn, this,
        pinstance);
    Pattern patern = Pattern.compile("^@ERROR=@Inline@\\s+[0-9]+\\s+@productWithoutAttributeSet@");
    for (PInstanceProcessData pinstanceD : pinstanceData) {
      if (patern.matcher(pinstanceD.errormsg).matches()) {
        pinstanceD.errormsg = "@productWithoutSomeAttributeSet@";
        pinstanceD.result = "2";
      }
    }
    myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
    return myMessage;
  }

  public String getServletInfo() {
    return "Servlet MaterialReceiptPending. This Servlet was made by Jon Alegría";
  } // end of getServletInfo() method
}
