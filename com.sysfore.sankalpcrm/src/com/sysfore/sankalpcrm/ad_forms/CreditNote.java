// Decompiled by DJ v3.12.12.96 Copyright 2011 Atanas Neshkov  Date: 10/18/2011 4:32:27 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   CreditNote.java

package com.sysfore.sankalpcrm.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

// Referenced classes of package com.sysfore.sankalpcrm.ad_forms:
//            CreditNote1Data

public class CreditNote extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String M_id;
  private String Sw_type;
  private String sw_Acc_type;
  private String CNoteId;
  private String creditnoteid;
  private String orgId;
  private String clientId;
  private String userId;
  private String strCreditnoteType;
  private String strCreditnoteAccountType;
  private String Member;
  private String strswSreqLineId;
  private String strDocumentno;
  private int crin;
  public final String CRN_TYPE_EXCHANGE = "Exchange";

  // Connection conn = null;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    // Collecting all variables
    // String strDocumentno;
    VariablesSecureApp vars = new VariablesSecureApp(request);
    orgId = vars.getOrg();
    clientId = vars.getClient();
    userId = vars.getUser();
    strCreditnoteType = vars.getStringParameter("inpemcrCreditnotetype", "");
    strCreditnoteAccountType = vars.getStringParameter("inpemcrCreditNoteAccountType", "");
    Member = vars.getStringParameter("inpmProductId", "");
    creditnoteid = vars.getStringParameter("inpCreditNoteId", "");
    String DateFrom = vars.getStringParameter("inpDateFrom", "");
    String Dateto = vars.getStringParameter("inpDateTo", "");
    String ItemCode = vars.getStringParameter("inpItemCode", "");
    String DocumentNo = vars.getStringParameter("inpDocumentNo", "");

    if (vars.commandIn("DEFAULT")) {
      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "DEFAULT");
    } else if (vars.commandIn("FIND")) {

      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "FIND");
    } else if (vars.commandIn("GENERATE")) {
      discardAllOpenCRNbyUser(userId);
      // Generating UUID for Header
      creditnoteid = SequenceIdData.getUUID();
      OBError myMessage = null;
      // Call method to insert Header and Lines
      if (!Member.equals("")) {
        myMessage = processPurchaseOrder(vars, null, orgId, clientId, userId, Member, DateFrom,
            strCreditnoteType, strCreditnoteAccountType, creditnoteid);
      }
      vars.setMessage("CreditNote", myMessage);
      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "GENERATE");

    } else if (vars.commandIn("PRINT")) {

      printPageXls(response, vars, creditnoteid, strCreditnoteAccountType, strCreditnoteType);

    } else if (vars.commandIn("COMPLETE")) {

      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "COMPLETE");
    } else if (vars.commandIn("DELETE")) {

      String strrcCreditNoteLineId = vars.getRequiredInStringParameter("inpswCreditNoteLineId",
          IsIDFilter.instance);

      OBError myMessage = deleteCreditNoteLine(vars, strrcCreditNoteLineId);
      vars.setMessage("CreditNote", myMessage);
      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "DELETE");
    } else if (vars.commandIn("DISCARD")) {

      discardAllOpenCRNbyUser(userId);

      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "DISCARD");
    }

    else if (vars.commandIn("BILL")) {
      // String strrcCreditNoteLineId =
      // vars.getRequiredInStringParameter("inprcCreditNoteLineId",
      // IsIDFilter.instance);
      // " strrcCreditNoteLineId");
      printPagePartePDF(request, response, vars);

      // OBError myMessage = deleteCreditNoteLine(vars,
      // strrcCreditNoteLineId);
      // vars.setMessage("CreditNote", myMessage);
      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "BILL");
    } else if (vars.commandIn("ADDLINES")) {
      if (!creditnoteid.equals("")) {
        insertLine(vars, creditnoteid, orgId, clientId, userId);
      }
      printPageDataSheet(response, vars, DateFrom, Dateto, Member, ItemCode, DocumentNo,
          strCreditnoteType, strCreditnoteAccountType, "ADDLINES");
    } else {
      pageError(response);
    }
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String DateFrom,
      String DateTo, String Member, String ItemCode, String DocumentNo, String strCreditnoteType,
      String strCreditnoteAccountType, String commandIn) throws IOException, ServletException {
    String MemId = "", CBillNo = "";

    if (log4j.isDebugEnabled())
      log4j.debug("BEGIN PRINTPAGEDATASHEET");
    response.setContentType("text/html; charset=UTF-8");
    log4j.info("Before print writer");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    CreditNote1Data data[] = null;

    Date fromdate = null;
    Date todate = null;
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");

    try {
      fromdate = df.parse(DateFrom);
      todate = df.parse(DateTo);
    } catch (ParseException ex) {
      // x Logger.getLogger(CreditNote.class.getName()).log(Level.SEVERE,
      // null, ex);
    }
    xmlDocument = xmlEngine.readXmlTemplate("com/sysfore/sankalpcrm/ad_forms/CreditNote")
        .createXmlDocument();

    if ((ItemCode.equals("")) && (DocumentNo.equals("")) && (DateFrom.equals(""))
        && (DateTo.equals(""))) {
      // xmlDocument =
      // xmlEngine.readXmlTemplate("com/sysfore/sankalpcrm/ad_forms/CreditNote").createXmlDocument();
      data = CreditNote1Data.set();
    } else {

      // xmlDocument =
      // xmlEngine.readXmlTemplate("com/sysfore/sankalpcrm/ad_forms/CreditNote").createXmlDocument();

      if ((!DocumentNo.equals("")) && (ItemCode.equals("")) && (DateFrom.equals(""))
          && (DateTo.equals(""))) {
        data = CreditNote1Data.selectresult(this, DocumentNo, Member);
      }
      if ((!ItemCode.equals("")) && (DocumentNo.equals("")) && (DateFrom.equals(""))
          && (DateTo.equals("")))
      // if("".equals(DocumentNo))
      {
        data = CreditNote1Data.selectresult2(this, ItemCode, Member);
      }
      if ((ItemCode.equals("")) && (DocumentNo.equals("")) && (!DateFrom.equals(""))
          && (!DateTo.equals(""))) {
        data = CreditNote1Data.selectresult4(this, Member, DateFrom, DateTo);
      }
      if ((!ItemCode.equals("")) && (!DocumentNo.equals("")) && (DateFrom.equals(""))
          && (DateTo.equals(""))) {
        data = CreditNote1Data.selectresult3(this, ItemCode, DocumentNo, Member);
      }
      // else if(!"".equals(DateFrom) && !"".equals(DateTo))

    }

    log4j.debug("XML LOADED");
    try {
      String strDateFrom = vars.getStringParameter("inpDateFrom", "");
      String strDateTo = vars.getStringParameter("inpDateTo", "");
      WindowTabs tabs = new WindowTabs(this, vars, "com.sysfore.sankalpcrm.ad_forms.CreditNote");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      xmlDocument.setParameter("dateFrom", strDateFrom);
      xmlDocument.setParameter("dateTo", strDateTo);
      xmlDocument.setParameter("parameterListSelected", strCreditnoteType);
      xmlDocument.setParameter("parameterListSelected", strCreditnoteAccountType);
      if (commandIn.equals("DISCARD")) {
        xmlDocument.setParameter("inpCreditNoteId", "");
        xmlDocument.setParameter("inpmProductId", "");
        xmlDocument.setParameter("dateFrom", "");
        xmlDocument.setParameter("dateTo", "");

      } else if (commandIn.equals("COMPLETE")) {
        xmlDocument.setParameter("inpCreditNoteId", "");
        xmlDocument.setParameter("inpmProductId", "");
        xmlDocument.setParameter("dateFrom", "");
        xmlDocument.setParameter("dateTo", "");
      } else {
        xmlDocument.setParameter("inpCreditNoteId", creditnoteid);
        xmlDocument.setParameter("inpmProductId", Member);
      }
      xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "CreditNote.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "CreditNote.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());

      CreditNote5Data activemember[] = CreditNote5Data.activemember(this, Member);
      xmlDocument.setData("structure3", activemember);

    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    log4j.debug("UI parameters set");
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "CreditNote", false, "", "", "", false,
        "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    log4j.debug("TOOLBAR SET");
    log4j.debug("MESSAGE SET");
    xmlDocument.setParameter("Creditnotetype", strCreditnoteType);
    xmlDocument.setParameter("Creditnoteaccounttype", strCreditnoteAccountType);
    try {
      // E6AED79D81CC48BD830546BA5DCCF6E7 staging
      // 6EAEF1A8ACCC48D3B9D6C08F83CB8CC7 local
      // 8AE7F5AD1E8C4535A973A669DE1F2CD7
      // 641CAB81DE55470790D1BC34ECCD96CB production
      // "67278183C8724A2F896E9F2442CEC826" my local
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "Creditnotetype",
          "67278183C8724A2F896E9F2442CEC826", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "CreditNote"), Utility.getContext(this, vars, "#User_Client",
              "CreditNote"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "CreditNote", strCreditnoteType);
      xmlDocument.setData("reportcr_Creditnotetype", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      // 302FD229312C42D0990ACC016FE51A92 staging
      // 63C70C976A804530BF81BCEDEDBE586E local
      // C9D1034699324D0DB00CE39DE6785A5C // live production
      // "C0DAB873BC1C4B81A3AEF320071F1CE3" //new movement type for refund
      // exchange

      ComboTableData comboTableData3 = new ComboTableData(vars, this, "17", "Creditnotetype",
          "302FD229312C42D0990ACC016FE51A92", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "CreditNote"), Utility.getContext(this, vars, "#User_Client",
              "CreditNote"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData3, "CreditNote", "");
      xmlDocument.setData("reportcr_CreditNoteAccountType", "liststructure",
          comboTableData3.select(false));
      comboTableData3 = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      // E6AED79D81CC48BD830546BA5DCCF6E7 staging
      // 1496882686F44637B292A033D4E4287F locAL
      // "BCE5760E6BDF409D9E8CF0120963F7D7" // local current
      // 6A016B357CC1415586D5200054D036D0 /// live production
      ComboTableData comboTableData4 = new ComboTableData(vars, this, "17",
          "Exchangerefundresaone", "E6AED79D81CC48BD830546BA5DCCF6E7", "", Utility.getContext(this,
              vars, "#AccessibleOrgTree", "CreditNote"), Utility.getContext(this, vars,
              "#User_Client", "CreditNote"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData4, "CreditNote", "");
      xmlDocument.setData("reportcr_ExchangeRefundResaone", "liststructure",
          comboTableData4.select(false));
      comboTableData4 = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    /*
     * try { conn = getTransactionConnection(); } catch (Exception ex) { try {
     * releaseRollbackConnection(conn); } catch (Exception ignored) { } ex.printStackTrace();
     * log4j.warn("Rollback in transaction");
     * 
     * }
     */

    if (commandIn.equals("GENERATE")) {

      /*
       * strswSreqLineId = vars.getRequiredInStringParameter("inpswSrequisitionId",
       * IsIDFilter.instance); StringBuffer html = new StringBuffer(); if
       * (strswSreqLineId.startsWith("(")) strswSreqLineId = strswSreqLineId.substring(1,
       * strswSreqLineId.length() - 1); if (!strswSreqLineId.equals("")) { strswSreqLineId =
       * Replace.replace(strswSreqLineId, "'", ""); StringTokenizer st = new
       * StringTokenizer(strswSreqLineId, ",", false);
       * 
       * System.out.println("CRN ID "+strswSreqLineId); html.append("\nfunction insertData() {\n");
       * while (st.hasMoreTokens()) { String strexlineId = st.nextToken().trim(); int i = 0; while
       * (i < data.length) { html.append((new StringBuilder())
       * .append("document.getElementsByName(\"inpqtyneword") .append(strexlineId) .append("\"")
       * .append(")[0].value = ") .append("'") .append( vars.getStringParameter((new
       * StringBuilder()).append("inpqtyneword")
       * .append(strexlineId).toString())).append("';\n").toString()); html.append((new
       * StringBuilder()) .append("document.getElementsByName(\"inppricenew") .append(strexlineId)
       * .append("\"") .append(")[0].value = ") .append("'") .append( vars.getStringParameter((new
       * StringBuilder()).append("inppricenew")
       * .append(strexlineId).toString())).append("';\n").toString()); html.append((new
       * StringBuilder()) .append("document.getElementsByName(\"inpemcrExchangeRefundResaone")
       * .append(strexlineId) .append("\"") .append(")[0].value = ") .append("'") .append(
       * vars.getStringParameter((new StringBuilder()).append("inpqtyneword")
       * .append(strexlineId).toString())).append("';\n").toString()); html.append((new
       * StringBuilder()) .append("document.getElementsByName(\"inpcomment") .append(strexlineId)
       * .append("\"") .append(")[0].value = ") .append("'") .append( vars.getStringParameter((new
       * StringBuilder()).append("inppricenew")
       * .append(strexlineId).toString())).append("';\n").toString()); html.append((new
       * StringBuilder()) .append("setCheckedValue(document.frmMain.inpswSrequisitionId, '")
       * .append(strexlineId).append("');\n").toString()); i++; } } }
       */

    } else if (commandIn.equals("DELETE")) {

      String strrcCreditNoteLineId = vars.getRequiredInStringParameter("inpswCreditNoteLineId",
          IsIDFilter.instance);

      StringBuffer html = new StringBuffer();
      if (strrcCreditNoteLineId.startsWith("("))
        strrcCreditNoteLineId = strrcCreditNoteLineId.substring(1,
            strrcCreditNoteLineId.length() - 1);
      if (!strrcCreditNoteLineId.equals("")) {

        strrcCreditNoteLineId = Replace.replace(strrcCreditNoteLineId, "'", "");

        StringTokenizer st = new StringTokenizer(strrcCreditNoteLineId, ",", false);
        html.append("\nfunction insertData() {\n");
        while (st.hasMoreTokens()) {
          String strexlineId = st.nextToken().trim();
          int i = 0;
          i = 0;
          while (i < data.length) {
            html.append((new StringBuilder())
                .append("setCheckedValue(document.frmMain.inpswCreditNoteLineId, '")
                .append(strexlineId).append("');\n").toString());
            i++;
          }
        }
      }
    } else if (commandIn.equals("BILL")) {

    } else if (commandIn.equals("COMPLETE")) {
      String sumamt = CreditNote2Data.selecttotal(this, creditnoteid);

      int sts = CreditNote2Data.updatenote(this, creditnoteid);
      if (sts > 0) {
      }
    }
    // /code will execult while page load and check if user has active
    // credit note ot not if not then create new else display line

    /*
     * CreditNote2Data activerecord[] = CreditNote2Data.selectactivecreditnoteHead(this,
     * vars.getUser()); if (activerecord.length > 0) { crin = 1; creditnoteid =
     * activerecord[0].scno; String CurrentM_id = activerecord[0].memid;
     * System.out.println(" Active new member " + CurrentM_id); System.out.println(" new member " +
     * CurrentM_id); if (CurrentM_id.equals(M_id)) {
     * 
     * // System.out.println(" M_id " + M_id ); int acm = CreditNote3Data.updateactivemember(this,
     * Member, creditnoteid); // System.out.println(" creditnoteid" + creditnoteid ); } else { M_id
     * = CurrentM_id; }
     */
    CreditNote3Data activerecordLine[] = CreditNote3Data.selectactivecreditnoteline(this,
        creditnoteid);
    xmlDocument.setData("structure1", activerecordLine);

    // xmlDocument.setData("structure3", activemember);

    log4j.debug("STRUCTURE SET");
    xmlDocument.setData("structure2", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError deleteCreditNoteLine(VariablesSecureApp vars, String strrcCreditNoteLineId)
      throws IOException, ServletException {
    int line = 0;
    OBError myMessage = null;
    OBError myMessageAux = new OBError();
    myMessage = new OBError();
    myMessage.setTitle("Information");
    myMessage.setType("INFO");
    if (strrcCreditNoteLineId.equals("")) {

      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "RC_ldelete");
      // return myMessage;
    }
    try {
      if (strrcCreditNoteLineId.startsWith("("))
        strrcCreditNoteLineId = strrcCreditNoteLineId.substring(1,
            strrcCreditNoteLineId.length() - 1);
      if (!strrcCreditNoteLineId.equals("")) {
        strrcCreditNoteLineId = Replace.replace(strrcCreditNoteLineId, "'", "");
        StringTokenizer st = new StringTokenizer(strrcCreditNoteLineId, ",", false);
        for (; st.hasMoreTokens(); line += 10) {

          String strCnlineId = st.nextToken().trim();
          int dro = CreditNote2Data.deleteselectedline(this, strCnlineId);
          myMessage.setMessage(Utility.messageBD(this, "RC_ldelete", vars.getLanguage()));
        }
      }
    } catch (Exception e) {
      try {
        // releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "RC_ldelete");
    }

    return myMessage;
  }

  private OBError processPurchaseOrder(VariablesSecureApp vars, String strswSreqLineId,
      String orgId, String clientId, String userId, String memID, String reexdate, String crtype,
      String crAcctype, String creditnoteid) throws IOException, ServletException {
    String strMessageResult = "";
    int sts = 0;
    String strMessageType = "Success";

    OBError myMessage = null;
    OBError myMessageAux = new OBError();
    myMessage = new OBError();
    myMessage.setTitle("");
    XmlDocument xmlDocument = null;

    /*
     * if (Member != null) { xmlDocument.setParameter("messageType", myMessage.getType());
     * xmlDocument.setParameter("messageTitle", myMessage.getTitle());
     * xmlDocument.setParameter("messageMessage", myMessage.getMessage()); }
     */

    try {
      // String strDocumentno;
      int line = 0;
      double ltotal = 0;
      double emdsgt = 0;
      String Strlinetotal = "";
      String StrGrandTotal = "";

      // Get Max of Document no
      String maxdocno = CreditNote3Data.selectmaxodcno(this);

      int newdon = 0;
      if ("".equals(maxdocno)) {
        newdon = 10000;
      } else {
        newdon = Integer.parseInt(maxdocno) + 1;
      }

      // stores the value of documentno in creditnote header
      strDocumentno = Integer.toString(newdon);

      // Inserting Header in here
      // crin = CreditNote3Data.insert(conn, this, creditnoteid, clientId, orgId, userId, userId,
      // memID, crtype, crAcctype, strDocumentno);
      crin = CreditNote3Data.insert(this, creditnoteid, clientId, orgId, userId, userId, memID,
          crtype, crAcctype, strDocumentno);

      // releaseCommitConnection(conn);
      myMessage.setType(strMessageType);
      myMessage.setTitle(myMessageAux.getTitle());
      myMessage.setMessage(strMessageResult);

    } catch (Exception e) {
      try {
        // releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    }

    return myMessage;
  }

  private OBError insertLine(VariablesSecureApp vars, String strCreditNoteId, String orgId,
      String clientId, String userId) throws IOException, ServletException {
    // If header is inserted, Insert Line

    String strMessageResult = "";
    int sts = 0;
    String strMessageType = "Success";

    OBError myMessage = null;
    OBError myMessageAux = new OBError();
    myMessage = new OBError();
    myMessage.setTitle("");

    if (strCreditNoteId.equals("")) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
      return myMessage;
    }
    int line = 0;

    double ltotal = 0;
    double emdsgt = 0;
    String Strlinetotal = "";
    String StrGrandTotal = "";

    // C_ORDERLINE_ID
    strswSreqLineId = vars.getRequiredInStringParameter("inpswSrequisitionId", IsIDFilter.instance);
    /*
     * StringBuffer html = new StringBuffer(); if (strswSreqLineId.startsWith("(")) strswSreqLineId
     * = strswSreqLineId.substring(1, strswSreqLineId.length() - 1); if
     * (!strswSreqLineId.equals("")) { strswSreqLineId = Replace.replace(strswSreqLineId, "'", "");
     * StringTokenizer st = new StringTokenizer(strswSreqLineId, ",", false); //
     */
    try {
      if (strswSreqLineId.startsWith("("))
        strswSreqLineId = strswSreqLineId.substring(1, strswSreqLineId.length() - 1);

      if (!strswSreqLineId.equals("")) {
        strswSreqLineId = Replace.replace(strswSreqLineId, "'", "");
        StringTokenizer st = new StringTokenizer(strswSreqLineId, ",", false);
        String strmInoutId = "";

        sts = 1;

        for (; st.hasMoreTokens(); line += 10) {

          String strOrderlineId = st.nextToken().trim();
          String strnewQty = vars.getStringParameter((new StringBuilder()).append("inpqtyneword")
              .append(strOrderlineId).toString());
          String strnewprice = vars.getStringParameter((new StringBuilder()).append("inppricenew")
              .append(strOrderlineId).toString());

          String Resone = vars.getStringParameter((new StringBuilder())
              .append("inpemcrExchangeRefundResaone").append(strOrderlineId).toString());

          // Getting Bill Number from c_order by passing c_orderline_id
          String BillNoin = CreditNote3Data.selectdocno(this, strOrderlineId);

          // Getting product infomration from c_orderline
          CreditNote1Data itde[] = CreditNote1Data.selectitem(this, strOrderlineId);

          for (int s = 0; s < itde.length; s++) {
            String itemcode = itde[s].itemcode;
            String pl1 = itde[s].l1;
            String pl2 = itde[s].l2;
            String pl3 = itde[s].l3;
            String qtyinv = itde[s].qtyord;
            if ((Double.parseDouble(strnewQty) == 0)) {
              ltotal = Double.parseDouble(strnewprice) * 1;
            } else if ((Double.parseDouble(strnewprice) == 0)) {
              ltotal = 1 * Double.parseDouble(strnewQty);
            } else {
              ltotal = Double.parseDouble(strnewprice) * Double.parseDouble(strnewQty);
            }
            Strlinetotal = Double.toString(ltotal);

            // Inserting Line in the DB
            try {
              /*
               * int cnl = CreditNote1Data.insertLine(conn, this, clientId, orgId, userId, userId,
               * strCreditNoteId, itde[s].itemcode, itde[s].qtyord, strnewQty, itde[s].l1,
               * itde[s].l2, itde[s].l3, strnewprice, Resone, Strlinetotal, BillNoin);
               */
              int cnl = CreditNote1Data.insertLine(this, clientId, orgId, userId, userId,
                  strCreditNoteId, itde[s].itemcode, itde[s].qtyord, strnewQty, itde[s].l1,
                  itde[s].l2, itde[s].l3, strnewprice, Resone, Strlinetotal, BillNoin);
            } catch (Exception e) {
              e.printStackTrace();
            }

          }// inner for

        }// outer for

        if (strMessageType.equals("Success")) {
          strMessageType = myMessageAux.getType();
        } else if (strMessageType.equals("Warning") && myMessageAux.getType().equals("Error")) {
          strMessageType = "Error";
        }

        // Else insert the new line in the same header
      } else {
        // to handle if no check box selected
      }

      /*
       * if (strMessageType.equals("Success")) strMessageType = myMessageAux.getType(); else if
       * (strMessageType.equals("Warning") && myMessageAux.getType().equals("Error")) strMessageType
       * = "Error";
       */
      // releaseCommitConnection(conn);
      myMessage.setType(strMessageType);
      myMessage.setTitle(myMessageAux.getTitle());
      myMessage.setMessage(strMessageResult);
    } catch (Exception e) {
      try {
        // releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    }

    return myMessage;

  }// end method

  private void discardAllOpenCRNbyUser(String adUserID) {

    try {
      CreditNote6Data.deleteAllCRNlinesbyUser(this, adUserID);
      CreditNote6Data.deleteAllCRNbyUser(this, adUserID);
    } catch (Exception e) {
      try {
        // releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction deleting failed");
    }

  }

  private void printPagePartePDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars) throws IOException, ServletException {

    String strOrderId = "";
    strswSreqLineId = vars.getRequiredInStringParameter("inpswSrequisitionId", IsIDFilter.instance);
    StringBuffer html = new StringBuffer();
    if (strswSreqLineId.startsWith("("))
      strswSreqLineId = strswSreqLineId.substring(1, strswSreqLineId.length() - 1);
    if (!strswSreqLineId.equals("")) {
      strswSreqLineId = Replace.replace(strswSreqLineId, "'", "");
      StringTokenizer st = new StringTokenizer(strswSreqLineId, ",", false);
      html.append("\nfunction insertData() {\n");
      String strexlineId = st.nextToken().trim();
      strOrderId = CreditNote1Data.selectcorderID(this, strexlineId);
    }

    // ////from here print bill
    if (log4j.isDebugEnabled())

      log4j.debug("Output: pdf");

    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    JasperReport jasperReportLines1;
    try {
      JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
          + "/com/sysfore/sankalpcrm/ad_forms/DuplicateBill_subreport0.jrxml");
      jasperReportLines1 = JasperCompileManager.compileReport(jasperDesignLines);
    } catch (JRException e) {
      e.printStackTrace();
      throw new ServletException(e.getMessage());
    }

    JasperReport jasperReportLines2;
    try {
      JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
          + "/com/sysfore/sankalpcrm/ad_forms/DuplicateBill_subreport2.jrxml");
      jasperReportLines2 = JasperCompileManager.compileReport(jasperDesignLines);
    } catch (JRException e) {
      e.printStackTrace();
      throw new ServletException(e.getMessage());
    }

    JasperReport jasperReportLines3;
    try {
      JasperDesign jasperDesignLines = JRXmlLoader.load(strBaseDesign
          + "/com/sysfore/sankalpcrm/ad_forms/DuplicateBill_subreport1.jrxml");
      jasperReportLines3 = JasperCompileManager.compileReport(jasperDesignLines);
    } catch (JRException e) {
      e.printStackTrace();
      throw new ServletException(e.getMessage());
    }

    strOrderId = strOrderId.replaceAll("\\(|\\)|'", "");

    parameters.put("SR_LINES1", jasperReportLines1);
    parameters.put("SR_LINES2", jasperReportLines2);
    parameters.put("SR_LINES3", jasperReportLines3);
    parameters.put("ORDERID", strOrderId);
    String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_forms/DuplicateBill.jrxml";
    String strOutput = "html";
    renderJR(vars, response, strReportName, strOutput, parameters, null, null);

  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strmInoutId,
      String AccountType, String CRNType) throws IOException, ServletException {
    String locatoreto = "", locatorefrom = "", locaterid = "02AA5CD37B824B13981EA21D3B178807";
    String strmWarehouseId = vars.getWarehouse();
    orgId = CreditNote2Data.selectorg(this, strmInoutId);
    CreditNote6Data locator[] = CreditNote6Data.selectlocatores(this, orgId,
        CreditNote6Data.selectacctype(this, strmInoutId));
    if (locator.length > 0) {
      for (int dt = 0; dt < locator.length; dt++) {
        locatoreto = locator[dt].locatoreto;
        locatorefrom = locator[dt].locatorefrom;
      }
    }

    String sumamt = CreditNote2Data.selecttotal(this, strmInoutId);
    // added incase of concurrency, documentno gets overriden
    String thisDCNo = CreditNote2Data.selectdocno(this, strmInoutId);
    // orgId = CreditNote2Data.selectorg(this, strmInoutId);
    String itemCode = "", m_product_id = "", qtyext = "", documnetnois = "", crAcctype = "";
    int firstTransactionCount = 0, swtran = 0, secondTransactionCount = 0;
    double afterqty = 0.0, newafter = 0.0;
    int updategt = CreditNote1Data.updateheader(this, sumamt, creditnoteid);
    int sts = CreditNote2Data.updatenote(this, strmInoutId);
    if (CRNType.equals(CRN_TYPE_EXCHANGE)) {
      CreditNote4Data nameselect[] = CreditNote4Data.selectname(this, strmInoutId);
      if (nameselect.length > 0) {
        for (int nt = 0; nt < nameselect.length; nt++) {
          itemCode = nameselect[nt].pname; // / item in credt note
          qtyext = nameselect[nt].qtyext;
          swtran = Integer.parseInt(qtyext) * -1;
          crAcctype = nameselect[nt].accounttype;

          m_product_id = CreditNote3Data.selectproductid(this, itemCode);
          log4j.debug("document no in m_transaction" + strDocumentno);
          // documnetnois = "CRN-" + strDocumentno;
          documnetnois = "CRN-" + thisDCNo;
          try {

            firstTransactionCount = CreditNote4Data.inserttransection(this, clientId, orgId,
                userId, userId, locatorefrom, m_product_id, Integer.toString(swtran), locatoreto,
                crAcctype, documnetnois);

            if (firstTransactionCount > 0) {
              // added incase of concurrency
              orgId = CreditNote2Data.selectorg(this, strmInoutId);
              secondTransactionCount = CreditNote4Data.inserttransection(this, clientId, orgId,
                  userId, userId, locatoreto, m_product_id, qtyext, locatorefrom, crAcctype,
                  documnetnois);
            }

          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

    } else {

    }

    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    String strReportName = "@basedesign@/com/sysfore/sankalpcrm/ad_forms/Report_credit.jrxml";

    String strOutput = "pdf";
    String strTitle = classInfo.name;

    response.setHeader("Content-disposition", "inline;filename=CreditNote.pdf");
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("REPORT_TITLE", strTitle);

    strmInoutId = strmInoutId.replaceAll("\\(|\\)|'", "");
    parameters.put("rcCreditnoteId", strmInoutId);
    HashMap<Object, Object> parametersexport = new HashMap<Object, Object>();
    M_id = "";
    renderJR(vars, response, strReportName, strOutput, parameters, null, null);
    // }

  }

  public String getServletInfo() {
    return "CreditNote controller servlet made specifically for CBD Training";
  }

}
