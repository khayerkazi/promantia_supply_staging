package in.decathlon.supply.barcode.ad_form;

import in.decathlon.supply.barcode.data.SBCBarcodeSequence;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.xmlEngine.XmlDocument;

import com.sysfore.catalog.CLBrand;

public class ReportGenerateBarcode extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  List<String> fpList = new ArrayList<String>();
  FieldProvider[] data = null;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT", "DIRECT")) {
      data = null;
      fpList.clear();

      String strStore = vars.getGlobalVariable("inpadOrgId", "ReportGenerateBarcode|store", "");

      String strBrand = vars.getGlobalVariable("inpclBrandId", "ReportGenerateBarcode|brand", "");
      String strQty = vars.getGlobalVariable("inpQty", "ReportGenerateBarcode|qty", "");

      setHistoryCommand(request, "DIRECT");
      // System.out.println("Store->" + strStore + "->Brand->" + strBrand);
      printPageDataSheet(response, vars, strStore, strBrand, strQty, "DEFAULT");

    } else if (vars.commandIn("UPDATE")) {

      String strStore = vars.getRequestGlobalVariable("inpadOrgId", "ReportGenerateBarcode|store");

      String strBrand = vars
          .getRequestGlobalVariable("inpclBrandId", "ReportGenerateBarcode|brand");

      String strQty = vars.getRequestGlobalVariable("inpQty", "ReportGenerateBarcode|qty");
      // System.out.println("Store->" + strStore + "->Brand->" + strBrand);
      printPageDataSheet(response, vars, strStore, strBrand, strQty, "DEFAULT");
    }

    else if (vars.commandIn("FIND")) {

      String strStore = vars.getRequestGlobalVariable("inpadOrgId", "ReportGenerateBarcode|store");

      String strBrand = vars
          .getRequestGlobalVariable("inpclBrandId", "ReportGenerateBarcode|brand");

      String strQty = vars.getRequestGlobalVariable("inpQty", "ReportGenerateBarcode|qty");

      vars.setMessage("ReportGenerateBarcode",
          processSearchItem(response, vars, strStore, strBrand, strQty));
      // System.out.println("Store->" + strStore + "->Brand->" + strBrand);
      printPageDataSheet(response, vars, strStore, strBrand, strQty, "FIND");

    } else if (vars.commandIn("PRINT_XLS")) {

      String strStore = vars.getRequestGlobalVariable("inpadOrgId", "ReportGenerateBarcode|store");

      String strBrand = vars
          .getRequestGlobalVariable("inpclBrandId", "ReportGenerateBarcode|brand");

      String strQty = vars.getRequestGlobalVariable("inpQty", "ReportGenerateBarcode|qty");

      setHistoryCommand(request, "DIRECT");
      // System.out.println("Store->" + strStore + "->Brand->" + strBrand);
      printPageDataXls(response, vars, strStore, strBrand, strQty);

    } else
      pageError(response);
  }

  void printPageDataXls(HttpServletResponse response, VariablesSecureApp vars, String strStore,
      String strBrand, String strQty) throws IOException, ServletException {

    String strReportName = null;
    // System.out.println("after fetching data for xls->" + data.length);
    // System.out.println("Data->" + data.toString());
    /*
     * for (int d = 0; d < data.length; d++) { System.out.println(d + "->" +
     * data[d].getField("slno")); System.out.println(d + "->" + data[d].getField("barcode")); }
     */
    strReportName = "@basedesign@/in/decathlon/supply/barcode/ad_form/ReportGenerateBarcode.jrxml";
    response.setHeader("Content-disposition", "inline;filename=GenerateBarcode.xls");
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    // parameters.put("ID", strBrand);
    renderJR(vars, response, strReportName, "xls", parameters, data, null);
    data = null;
    fpList.clear();
  }

  void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars, String strStore,
      String strBrand, String strQty, String command) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()) {
    }
    XmlDocument xmlDocument;

    if (command.equals("DEFAULT")) {

      data = null;
      fpList.clear();

    } else {
      data = getData(strBrand, strQty, strStore);
      vars.setMessage("ReportGenerateBarcode",
          processSearchItem(response, vars, strStore, strBrand, strQty));
    }

    if (data == null || data.length == 0) {
      data = ReportGenerateBarcodeData.set();
    }
    if (vars.commandIn("DEFAULT")) {

      xmlDocument = xmlEngine.readXmlTemplate(
          "in/decathlon/supply/barcode/ad_form/ReportGenerateBarcode").createXmlDocument();
      data = ReportGenerateBarcodeData.set();
      xmlDocument.setData("structure1", data);

    } else {
      xmlDocument = xmlEngine.readXmlTemplate(
          "in/decathlon/supply/barcode/ad_form/ReportGenerateBarcode").createXmlDocument();
    }

    // System.out.println("after sheet fetching data->" + data.length);

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportGenerateBarcode", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "in.decathlon.supply.barcode.ad_form.ReportGenerateBarcode");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportGenerateBarcode.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportGenerateBarcode.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("Store", strStore);
    xmlDocument.setParameter("Brand", strBrand);
    xmlDocument.setParameter("Qty", strQty);

    // BEGIN check for any pending message to be shown and clear it from the session
    OBError myMessage = vars.getMessage("ReportGenerateBarcode");
    vars.removeMessage("ReportGenerateBarcode");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }
    // END check for any pending message to be shown and clear it from the session

    // for store
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "LIST", "",
          "06E16C5996434BA19B60F6B6F839E26D", "", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "ReportGenerateBarcode"), Utility.getContext(this, vars,
              "#User_Client", "ReportGenerateBarcode"), 0);
      Utility
          .fillSQLParameters(this, vars, null, comboTableData, "ReportGenerateBarcode", strStore);
      xmlDocument.setData("reportad_org_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // for brand
    /*
     * try { ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
     * "sbc_barcode_seq_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
     * "ReportGenerateBarcode"), Utility.getContext(this, vars, "#User_Client",
     * "ReportGenerateBarcode"), 0); Utility .fillSQLParameters(this, vars, null, comboTableData,
     * "ReportGenerateBarcode", strBrand); xmlDocument.setData("reportcl_Brand_Id", "liststructure",
     * comboTableData.select(false)); comboTableData = null; } catch (Exception ex) { throw new
     * ServletException(ex); }
     */
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "Cl_Brand_ID", "",
          "", Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGenerateBarcode"),
          Utility.getContext(this, vars, "#User_Client", "ReportGenerateBarcode"), 0);
      Utility
          .fillSQLParameters(this, vars, null, comboTableData, "ReportGenerateBarcode", strBrand);
      xmlDocument.setData("reportcl_Brand_Id", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processSearchItem(HttpServletResponse response, VariablesSecureApp vars,
      String strStore, String strBrand, String strQty) {
    OBError myMessage = new OBError();
    try {

      int datalength = data.length;

      if (datalength == 0) {
        myMessage.setTitle("Information");
        myMessage.setType("INFO");
      }
      myMessage.setMessage(Utility.messageBD(this,
          "No configuration found selected Store and Brand", vars.getLanguage()));

    } catch (Exception e) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(),
          "No configuration found selected Store and Brand");
    }
    return myMessage;
  }

  // get data to print

  private FieldProvider[] getData(String strBrand, String strQty, String strStore) {
    List<Object> param = new ArrayList<Object>();
    data = null;
    fpList.clear();
    // Organization org = OBDal.getInstance().get(Organization.class, strStore);
    // System.out.println("strBrand->" + strBrand);
    CLBrand brandId = OBDal.getInstance().get(CLBrand.class, strBrand);
    OBCriteria<SBCBarcodeSequence> bcCriteria = OBDal.getInstance().createCriteria(
        SBCBarcodeSequence.class);
    bcCriteria.add(Restrictions.eq(SBCBarcodeSequence.PROPERTY_BRAND, brandId));
    bcCriteria.add(Restrictions.eq(SBCBarcodeSequence.PROPERTY_STORENAME, strStore));

    List<SBCBarcodeSequence> bcList = bcCriteria.list();
    if (!bcList.isEmpty()) {
      param.add(bcList.get(0).getId());
      if (strQty.equals("") || strQty == null) {
        strQty = "0";
      }
      String documentNo = null;
      for (int i = 0; i < Integer.parseInt(strQty); i++) {
        documentNo = (String) CallStoredProcedure.getInstance().call("sbc_barcode_sequence", param,
            null, true, true);
        // System.out.println(i + "->" + documentNo);
        fpList.add(i, documentNo);
      }
      data = FieldProviderFactory.getFieldProviderArray(fpList);

      for (int j = 0; j < data.length; j++) {
        FieldProviderFactory.setField(data[j], "slno", fpList.get(j));
        FieldProviderFactory.setField(data[j], "barcode", fpList.get(j));
      }
    }
    return data;
  }

  public String getServletInfo() {
    return "Servlet ReportGenerateBarcode.";
  }
}
