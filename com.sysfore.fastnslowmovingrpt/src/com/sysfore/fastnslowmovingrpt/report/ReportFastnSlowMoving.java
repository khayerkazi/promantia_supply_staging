package com.sysfore.fastnslowmovingrpt.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportFastnSlowMoving extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String[] fieldNames = { "RANK", "org", "brand", "storedept", "dept",
      "merchandisecat", "natofpdt", "modelname", "size", "color", "modelcode", "itemcode",
      "turnover", "qty", "marginamt", "marginper", "taxcat", "taxamt", "curstock" };
  private static final String[] fieldTitles = { "Rank", "Store", "Brand", "Store Department",
      "DMI Department", "Family", "Nature of Product", "Model Name", "Size", "Color", "Model Code",
      "Item Code", "Turnover", "Quantities", "Margin", "Margin%", "Tax Category", "Tax Amount",
      "Current Stock" };
  private static final boolean[] fieldIsNumeric = { true, false, false, false, false, false, false,
      false, false, false, false, false, false, true, true, true, true, false, true, true };

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportFastnSlowMoving|dateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportFastnSlowMoving|dateTo", "");
      String strClnatureofproductid = vars.getStringParameter("inpclNatureOfProductId", "");
      String strLifestage = vars.getGlobalVariable("inpemclLifestage",
          "ReportFastnSlowMoving|lifestage", "");
      String strBluePdt = vars.getGlobalVariable("inpBlueproduct",
          "ReportFastnSlowMoving|BlueProduct", "");
      String strBrand = vars.getInGlobalVariable("inpclBrand_IN", "ReportFastnSlowMoving|clBrand",
          "", IsIDFilter.instance);
      System.out.println("strBrand: in FiXLSnd " + strBrand);
      // String strDepartment = vars.getRequestGlobalVariable("inpclDepartmentId",
      // "ReportFastnSlowMoving|department");
      String strDepartment = vars.getInGlobalVariable("inpclDepartmentId_IN",
          "ReportFastnSlowMoving|clDepartment", "", IsIDFilter.instance);
      // String strSport = vars.getRequestGlobalVariable("inpclSportId",
      // "ReportFastnSlowMoving|sport");
      String strSport = vars.getInGlobalVariable("inpclSportId_IN",
          "ReportFastnSlowMoving|clSport", "", IsIDFilter.instance);
      String strFamily = vars.getRequestGlobalVariable("inpmerchandiseCategory",
          "ReportFastnSlowMoving|parammerchandiseCategory");
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportFastnSlowMoving|mProductId", "", IsIDFilter.instance);
      String strstoreDepartment = vars.getInGlobalVariable("inpclStoreDeptId_IN",
          "ReportFastnSlowMoving|clStoredept", "", IsIDFilter.instance);
      System.out.println("strstoreDepartment: in FiXLSnd " + strstoreDepartment);
      String strUniverse = vars.getInGlobalVariable("inpclUniverseId_IN",
          "ReportFastnSlowMoving|clUniverse", "", IsIDFilter.instance);
      System.out.println("strUniverse: in FiXLSnd " + strUniverse);
      printPageDataSheet(response, vars, strDateFrom, strDateTo, strClnatureofproductid,
          strLifestage, strBluePdt, strBrand, strDepartment, strSport, strFamily, strmProductId,
          strstoreDepartment, strUniverse);
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportFastnSlowMoving|dateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportFastnSlowMoving|dateTo", "");
      String strClnatureofproductid = vars.getStringParameter("inpclNatureOfProductId", "");
      String strLifestage = vars.getGlobalVariable("inpemclLifestage",
          "ReportSearchItem|lifestage", "");
      String strBluePdt = vars.getGlobalVariable("inpBlueproduct",
          "ReportFastnSlowMoving|BlueProduct", "");
      String strBrand = vars.getInGlobalVariable("inpclBrand_IN", "ReportFastnSlowMoving|clBrand",
          "", IsIDFilter.instance);
      String strDepartment = vars.getInGlobalVariable("inpclDepartmentId_IN",
          "ReportFastnSlowMoving|clDepartment", "", IsIDFilter.instance);
      // String strSport = vars.getRequestGlobalVariable("inpclSportId",
      // "ReportFastnSlowMoving|sport");
      String strSport = vars.getInGlobalVariable("inpclSportId_IN",
          "ReportFastnSlowMoving|clSport", "", IsIDFilter.instance);
      String strFamily = vars.getRequestGlobalVariable("inpmerchandiseCategory",
          "ReportFastnSlowMoving|parammerchandiseCategory");
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportFastnSlowMoving|mProductId", "", IsIDFilter.instance);
      String strstoreDepartment = vars.getInGlobalVariable("inpclStoreDeptId_IN",
          "ReportFastnSlowMoving|clStoredept", "", IsIDFilter.instance);
      System.out.println("strstoreDepartment: in FiXLSnd " + strstoreDepartment);
      String strUniverse = vars.getInGlobalVariable("inpclUniverseId_IN",
          "ReportFastnSlowMoving|clUniverse", "", IsIDFilter.instance);
      System.out.println("strUniverse: in FiXLSnd " + strUniverse);
      printPageHTML(response, vars, strDateFrom, strDateTo, strClnatureofproductid, strLifestage,
          strBluePdt, strBrand, strDepartment, strSport, strFamily, strmProductId,
          strstoreDepartment, strUniverse);
    } else if (vars.commandIn("XLS")) {

      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportFastnSlowMoving|dateFrom");
      System.out.println("strDateFrom: in XLS " + strDateFrom);
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportStockValuation|dateTo");
      System.out.println("strDateTo: in XLS " + strDateTo);
      String strBrand = vars.getInGlobalVariable("inpclBrand_IN", "ReportFastnSlowMoving|clBrand",
          "", IsIDFilter.instance);
      vars.setSessionValue("ReportFastnSlowMoving|clBrand", "");
      String strDepartment = vars.getInGlobalVariable("inpclDepartmentId_IN",
          "ReportFastnSlowMoving|clDepartment", "", IsIDFilter.instance);
      vars.setSessionValue("ReportFastnSlowMoving|clDepartment", "");
      String strSport = vars.getInGlobalVariable("inpclSportId_IN",
          "ReportFastnSlowMoving|clSport", "", IsIDFilter.instance);
      vars.setSessionValue("ReportFastnSlowMoving|clSport", "");
      String strFamily = vars.getRequestGlobalVariable("inpmerchandiseCategory",
          "ReportFastnSlowMoving|parammerchandiseCategory");
      String strLifestage = vars.getGlobalVariable("inpemclLifestage",
          "ReportSearchItem|lifestage", "");
      // System.out.println("strLifestage: in XLS " + strLifestage);
      String strBluePdt = vars.getGlobalVariable("inpBlueproduct",
          "ReportFastnSlowMoving|BlueProduct", "");
      // System.out.println("strBluePdt: in XLS " + strBluePdt);
      String strClnatureofproductid = vars.getStringParameter("inpclNatureOfProductId", "");
      // System.out.println("strClnatureofproductid: in XLS " + strClnatureofproductid);
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportFastnSlowMoving|mProductId", "", IsIDFilter.instance);
      // System.out.println("strmProductId: in XLS " + strmProductId);
      String strstoreDepartment = vars.getInGlobalVariable("inpclStoreDeptId_IN",
          "ReportFastnSlowMoving|clStoredept", "", IsIDFilter.instance);
      vars.setSessionValue("ReportFastnSlowMoving|clStoredept", "");
      System.out.println("strstoreDepartment: in FiXLSnd " + strstoreDepartment);
      String strUniverse = vars.getInGlobalVariable("inpclUniverseId_IN",
          "ReportFastnSlowMoving|clUniverse", "", IsIDFilter.instance);
      // System.out.println("strUniverse: in FiXLSnd " + strUniverse);
      printPageXls(response, vars, strDateFrom, strDateTo, strBrand, strDepartment, strSport,
          strFamily, strLifestage, strBluePdt, strClnatureofproductid, strmProductId,
          strstoreDepartment, strUniverse);// strDateFrom,

    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strClnatureofproductid, String strLifestage,
      String strBluePdt, String strBrand, String strDepartment, String strSport, String strFamily,
      String strmProductId, String strstoreDepartment, String strUniverse) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = null;

    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/fastnslowmovingrpt/report/ReportFastnSlowMoving").createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportFastnSlowMoving", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.fastnslowmovingrpt.report.ReportFastnSlowMoving");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportFastnSlowMoving.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportFastnSlowMoving.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportFastnSlowMoving");
      vars.removeMessage("ReportFastnSlowMoving");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("today", DateTimeData.today(this));
    xmlDocument.setParameter("NatureofProduct", strClnatureofproductid);
    xmlDocument.setParameter("Lifestage", strLifestage);
    xmlDocument.setData("reportClBrandId_IN", "liststructure", SelectorUtilityData.selectBrand(
        this, vars.getOrg(), vars.getClient(), strBrand));
    xmlDocument.setData("reportClDepartmentId_IN", "liststructure", SelectorUtilityData
        .selectDepartment(this, vars.getOrg(), vars.getClient(), strDepartment));
    xmlDocument.setData("reportClSportId_IN", "liststructure", SelectorUtilityData.selectSports(
        this, vars.getOrg(), vars.getClient(), strSport));
    xmlDocument.setData("reportClStoreDeptId_IN", "liststructure", SelectorUtilityData
        .selectStoreDepartment(this, vars.getOrg(), vars.getClient(), strstoreDepartment));
    xmlDocument.setData("reportClUniverseId_IN", "liststructure", SelectorUtilityData
        .selectUniverse(this, vars.getOrg(), vars.getClient(), strUniverse));
    xmlDocument.setParameter("merchandiseCategory", strFamily);

    try {

      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "cl_natureofproduct_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportFastnSlowMoving"), Utility.getContext(this, vars, "#User_Client",
              "ReportFastnSlowMoving"), 0);
      // System.out.println("I am passing combo table data....");
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportFastnSlowMoving",
          strClnatureofproductid);
      xmlDocument.setData("reportcl_NatureOfProduct_Id", "liststructure", comboTableData
          .select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    LifestageData[] data2 = LifestageData.select(myPool);
    // System.out.println("Lifesatge Ref====" +data2[0].id);
    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strLifestage);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "EM_Cl_Lifestage",
          data2[0].id, "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportFastnSlowMoving"), Utility.getContext(this, vars, "#User_Client",
              "ReportFastnSlowMoving"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportFastnSlowMoving",
          strLifestage);
      xmlDocument.setData("reportEM_cl_LIFESTAGE", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    BlueProductData[] data3 = BlueProductData.select(myPool);
    // System.out.println("BlueProduct Ref====" +data3[0].id);
    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strBluePdt);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "Blueproduct",
          data3[0].id, "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportFastnSlowMoving"), Utility.getContext(this, vars, "#User_Client",
              "ReportFastnSlowMoving"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportFastnSlowMoving",
          strBluePdt);
      xmlDocument.setData("reportBlueProduct", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageXls(HttpServletResponse response, VariablesSecureApp vars, String strDateFrom,
      String strDateTo, String strBrand, String strDepartment, String strSport, String strFamily,
      String strLifestage, String strBluePdt, String strClnatureofproductid, String strmProductId,
      String strstoreDepartment, String strUniverse) throws IOException, ServletException {

    String strDateTo1 = DateTimeData.nDaysAfter(this, strDateTo, "1");
    System.out.println("strDateTo1======" + strDateTo1);
    String strClient = vars.getClient();
    String strOrg = vars.getOrg();
    String locOrg = strOrg;
    System.out.println("locOrg: " + locOrg);
    System.out.println("strOrg: " + strOrg);

    dateDiffValidation(strDateFrom, strDateTo);

    long s1 = System.currentTimeMillis();
    ReportFastnSlowMovingData[] data = null;
    if (strOrg.equals("0")) {

      data = ReportFastnSlowMovingData.select(this, strDateFrom, strDateTo1, strDateFrom,
          strDateTo1, strBrand, strDepartment, strSport, strstoreDepartment, strFamily,
          strClnatureofproductid, strLifestage, strBluePdt, strClient);
    } else {
      data = ReportFastnSlowMovingData.selectOrg(this, locOrg, strDateFrom, strDateTo1,
          strDateFrom, strDateTo1, strBrand, strDepartment, strSport, strstoreDepartment,
          strFamily, strClnatureofproductid, strLifestage, strBluePdt, strOrg);
    }

    // calculate sums for a few fields
    ReportFastnSlowMovingData sumRow = new ReportFastnSlowMovingData();
    BigDecimal sumturnover = BigDecimal.ZERO;
    BigDecimal sumqty = BigDecimal.ZERO;
    BigDecimal summarginamt = BigDecimal.ZERO;
    BigDecimal marginpercentage = BigDecimal.ZERO;
    BigDecimal sumtaxamt = BigDecimal.ZERO;
    for (int i = 0; i < data.length; i++) {
      ReportFastnSlowMovingData oneRow = data[i];
      sumturnover = sumturnover.add(new BigDecimal(oneRow.turnover));
      sumqty = sumqty.add(new BigDecimal(oneRow.qty));
      summarginamt = summarginamt.add(new BigDecimal(oneRow.marginamt));
      sumtaxamt = sumtaxamt.add(new BigDecimal(oneRow.taxamt));
    }
    if (summarginamt.compareTo(BigDecimal.ZERO) == 0) {
      marginpercentage = BigDecimal.ZERO;
    } else {
      // round with 4 decimals so we get 2 decimals after the *100
      marginpercentage = summarginamt.divide(sumturnover, 4, BigDecimal.ROUND_HALF_EVEN).multiply(
          new BigDecimal(100));
    }
    sumRow.turnover = sumturnover.toPlainString();
    sumRow.qty = sumqty.toPlainString();
    sumRow.marginamt = summarginamt.toPlainString();
    sumRow.marginper = marginpercentage.toPlainString();
    sumRow.taxamt = sumtaxamt.toPlainString();

    long s2 = System.currentTimeMillis();
    log4j.debug("query for #rows: " + data.length + " took: " + (s2 - s1));

    String filename = "ReportFastnSlowMoving-" + strDateFrom + " to " + strDateTo + ".csv";
    response.setContentType("text/csv; charset=iso-8859-1");
    response.setHeader("Content-Disposition", "attachment; filename=" + filename);
    PrintWriter writer = response.getWriter();

    // Write header line
    for (int i = 0; i < fieldTitles.length; i++) {
      if (i > 0) {
        writer.print(",");
      }
      writer.print(fieldTitles[i]);
    }
    writer.println();
    // now write all rows
    for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
      writeOneCsvRow(data[rowIdx], writer, rowIdx);
    }
    // now write summary row
    writeOneCsvRow(sumRow, writer, -1);
    long s3 = System.currentTimeMillis();
    log4j.debug("write CSV took: " + (s3 - s2));

  }

  private void writeOneCsvRow(ReportFastnSlowMovingData row, PrintWriter writer, int rowIdx) {
    for (int i = 0; i < fieldNames.length; i++) {
      if (i > 0) {
        writer.print(",");
      }
      String fieldName = fieldNames[i];
      String fieldValue = row.getField(fieldName);
      if (fieldValue == null) {
        fieldValue = "";
      }
      if (fieldName.equals("RANK")) {
        if (rowIdx >= 0) {
          writer.print(rowIdx + 1);
        }
      } else {

        if (fieldIsNumeric[i]) {
          writer.print(fieldValue);
        } else {
          fieldValue = Replace.replace(fieldValue, "\"", "\"\"");
          writer.print("\"");
          writer.print(fieldValue);
          writer.print("\"");
        }
      }
    }
    writer.println();
  }

  private void printPageHTML(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strClnatureofproductid, String strLifestage,
      String strBluePdt, String strBrand, String strDepartment, String strSport, String strFamily,
      String strmProductId, String strstoreDepartment, String strUniverse) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = null;
    String strDateTo1 = DateTimeData.nDaysAfter(this, strDateTo, "1");
    dateDiffValidation(strDateFrom, strDateTo);
    String strOrg = vars.getOrg();
    String strClient = vars.getClient();
    String locOrg = strOrg;
    System.out.println("strOrg: " + strOrg);
    ReportFastnSlowMovingData[] data = ReportFastnSlowMovingData.selectOrg(this, locOrg,
        strDateFrom, strDateTo1, strDateFrom, strDateTo1, strBrand, strDepartment, strSport,
        strstoreDepartment, strFamily, strClnatureofproductid, strLifestage, strBluePdt, strOrg);
    if (strOrg.equals("0")) {
      // System.out.println("strDateTo1======" + strDateTo1);
      data = ReportFastnSlowMovingData.select(this, strDateFrom, strDateTo1, strDateFrom,
          strDateTo1, strBrand, strDepartment, strSport, strstoreDepartment, strFamily,
          strClnatureofproductid, strLifestage, strBluePdt, strClient);
    }

    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/fastnslowmovingrpt/report/ReportFastnSlowMoving").createXmlDocument();
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportFastnSlowMoving", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sysfore.fastnslowmovingrpt.report.ReportFastnSlowMoving");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportFastnSlowMoving.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportFastnSlowMoving.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportFastnSlowMoving");
      vars.removeMessage("ReportFastnSlowMoving");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("today", DateTimeData.today(this));
    xmlDocument.setParameter("NatureofProduct", strClnatureofproductid);
    xmlDocument.setParameter("Lifestage", strLifestage);
    xmlDocument.setData("reportClBrandId_IN", "liststructure", SelectorUtilityData.selectBrand(
        this, vars.getOrg(), vars.getClient(), strBrand));
    xmlDocument.setData("reportClDepartmentId_IN", "liststructure", SelectorUtilityData
        .selectDepartment(this, vars.getOrg(), vars.getClient(), strDepartment));
    xmlDocument.setData("reportClSportId_IN", "liststructure", SelectorUtilityData.selectSports(
        this, vars.getOrg(), vars.getClient(), strSport));
    xmlDocument.setData("reportClStoreDeptId_IN", "liststructure", SelectorUtilityData
        .selectStoreDepartment(this, vars.getOrg(), vars.getClient(), strstoreDepartment));
    xmlDocument.setData("reportClUniverseId_IN", "liststructure", SelectorUtilityData
        .selectUniverse(this, vars.getOrg(), vars.getClient(), strUniverse));
    xmlDocument.setParameter("merchandiseCategory", strFamily);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR",
          "cl_natureofproduct_id", "", "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportFastnSlowMoving"), Utility.getContext(this, vars, "#User_Client",
              "ReportFastnSlowMoving"), 0);
      // System.out.println("I am passing combo table data....");
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportFastnSlowMoving",
          strClnatureofproductid);
      xmlDocument.setData("reportcl_NatureOfProduct_Id", "liststructure", comboTableData
          .select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    LifestageData[] data2 = LifestageData.select(myPool);
    // System.out.println("Lifesatge Ref====" +data2[0].id);
    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strLifestage);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "EM_Cl_Lifestage",
          data2[0].id, "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportFastnSlowMoving"), Utility.getContext(this, vars, "#User_Client",
              "ReportFastnSlowMoving"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportFastnSlowMoving",
          strLifestage);
      xmlDocument.setData("reportEM_cl_LIFESTAGE", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    BlueProductData[] data3 = BlueProductData.select(myPool);
    // System.out.println("BlueProduct Ref====" +data3[0].id);
    if (log4j.isDebugEnabled())
      log4j.debug("ListData.select lifestage:" + strBluePdt);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "17", "Blueproduct",
          data3[0].id, "", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "ReportFastnSlowMoving"), Utility.getContext(this, vars, "#User_Client",
              "ReportFastnSlowMoving"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportFastnSlowMoving",
          strBluePdt);
      xmlDocument.setData("reportBlueProduct", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    // out.println(xmlDocument.print());
    // out.close();
    log4j.debug("STRUCTURE SET");
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private void dateDiffValidation(String from_date1, String to_date2) {
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    try {

      Date from_date = formatter.parse(from_date1);
      Date to_date = formatter.parse(to_date2);
      Days days = Days.daysBetween(LocalDate.fromDateFields(from_date), LocalDate
          .fromDateFields(to_date));
      if (days.getDays() > 32)
        throw new OBException(
            "Difference between From Date and To Date should not be greater than 32 days");

    } catch (ParseException e) {
      System.out.println("Date validation Error");
      e.printStackTrace();
    }
    return;
  }

}
