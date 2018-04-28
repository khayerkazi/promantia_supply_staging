package org.openbravo.warehouse.shipping.print;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportFixturesInvoiceReport extends BaseProcessActionHandler {
  private static final Logger log = LoggerFactory.getLogger(ExportFixturesInvoiceReport.class);

  private static String SDate = "DateFrom";
  private static String EDate = "DateTo";
  private static String FILE_NAME = ""; // GSTIN_TAXPERIOD_DateTime.xls
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  static JSONObject result = new JSONObject();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub

    JSONObject jsonRequest = null;
    // String message = new String();
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");
      // log.debug("{}", jsonRequest);
      SDate = params.getString("DateFrom");
      EDate = params.getString("DateTo");

      Date StartDate = sdf.parse(SDate);
      Date EndDate = sdf.parse(EDate);

      if (EndDate.before(StartDate)) {
        try {
          result = generateJSONMessage("error", "End Date must be greater than Start Date", "");
          return result;
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      long difference = EndDate.getTime() - StartDate.getTime();
      float daysBetween = (difference / (1000 * 60 * 60 * 24));

      if (daysBetween >= 31) {
        try {
          result = generateJSONMessage("error",
              "The difference between Start Date and End Date should not exceed 31", "");
          return result;
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

      ExportFixturesInvoiceReport.extract(StartDate, EndDate);

    } catch (JSONException e) {
      try {
        result = generateJSONMessage("error", "Error in Excel Export Transaction ", e.getMessage());
      } catch (JSONException e1) {
        result = new JSONObject();
      }
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      OBDal.getInstance().rollbackAndClose();
      try {
        result = generateJSONMessage("error", "Error in Transaction Generation", e.getMessage());
      } catch (JSONException e1) {
        result = new JSONObject();
      }
      // TODO Auto-generated catch block
    } catch (ParseException e) {
      result = new JSONObject();
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    finally {
      OBContext.restorePreviousMode();
    }
    return result;

    // return jsonRequest;
  }

  public static JSONObject extract(Date StartDate, Date EndDate)
      throws FileNotFoundException, IOException, JSONException, ParseException {

    String sql = "select " + "mw.name as WHName,mw.value as WHCode,mw.em_gs_gstin as WHGSTINNo, "
        + "org.name as OrgName,org.value as OrgCode, "
        + "ingst.uidno as StoreGSTINUniqueID,os.documentno as INVOICENUMBER,  "
        + "to_char(os.shipment_date,'YYYY-mm-dd') as DateofInvoice, " + "mp.name as ProductName,  "
        + "gst.value as ProductHSNCode, " + "mp.em_cl_modelname as ModelName,  "
        + "ml.movementqty as ShipmentQty, " + "ml.em_obwship_cessionprice as CessionPrice, "
        + "(ml.movementqty*ml.em_obwship_cessionprice) as TaxableValue, "
        + "(case when mw.em_gs_gstin=ingst.uidno then 0 else(case when ct.rate !=0 then ct.rate else 0 end)end) as INGSTRATE, "
        + "(case when mw.em_gs_gstin=ingst.uidno then 0 else(case when ct.rate !='0' then "
        + "round((((ml.movementqty*ml.em_obwship_cessionprice)*ct.rate)/100),2) else 0 end)end) "
        + "as TotalTaxValue,(case when mw.em_gs_gstin=ingst.uidno then ml.movementqty*ml.em_obwship_cessionprice else (case when ct.rate !='0' then "
        + "(ml.movementqty*ml.em_obwship_cessionprice)+(round((((ml.movementqty*ml.em_obwship_cessionprice)*ct.rate)/100),2)) "
        + "else (ml.movementqty*ml.em_obwship_cessionprice) end)end) as Total "
        + "from obwship_shipping os "
        + "left join obwship_shipping_details osd on os.obwship_shipping_id = osd.obwship_shipping_id "
        + "left join m_inout mi on mi.m_inout_id = osd.m_inout_id "
        + "left join m_warehouse mw on mw.m_warehouse_id = mi.m_warehouse_id "
        + "left join c_location loc on loc.c_location_id = mw.c_location_id "
        + "left join c_region reg1 on reg1.c_region_id = loc.c_region_id "
        + "left join c_bpartner cb on cb.c_bpartner_id = os.c_bpartner_id "
        + "left join ad_org org on org.name = cb.name "
        + "left join ad_orginfo info on info.ad_org_id = org.ad_org_id "
        + "left join c_location cloc on cloc.c_location_id = info.c_location_id "
        + "left join c_region reg on reg.c_region_id = cloc.c_region_id "
        + "left join ingst_gstidentifiermaster ingst on ingst.ingst_gstidentifiermaster_id = info.em_ingst_gstidentifirmaster_id "
        + "left join m_inoutline ml on ml.m_inout_id = mi.m_inout_id "
        + "left join m_product mp on mp.m_product_id = ml.m_product_id "
        + "left join INGST_GSTProductCode gst on gst.INGST_GSTProductCode_id = mp.EM_Ingst_Gstproductcode_ID "
        + "left join m_productprice mpp on mpp.m_product_id = mp.m_product_id "
        + "left join c_orderline orl on orl.c_orderline_id = ml.c_orderline_id "
        + "left join c_orderlinetax olt on olt.c_orderline_id = orl.c_orderline_id "
        + "left join c_tax ct on ct.c_tax_id = olt.c_tax_id "
        + "where to_char(os.shipment_date,'YYYY-mm-dd')<='2018-02-01' and  to_char(os.shipment_date,'YYYY-mm-dd')>='2018-02-01'  "
        + " and mpp.m_pricelist_version_id ='0F39C05C15EE4E5BB50BD5FEC1645DA1' "
        + "group by  mw.name  ,mw.value ,mw.em_gs_gstin  , org.name  ,org.value  ,ingst.uidno  ,os.documentno  ,"
        + " os.shipment_date,  mp.name , gst.value ,mp.em_cl_modelname , ct.rate, ml.movementqty ,"
        + "ml.em_obwship_cessionprice ,ml.movementqty,ml.em_obwship_cessionprice" + "";
    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
    List<Object[]> queryList = query.list();

    if (queryList.size() == 0) {
      try {
        result = generateJSONMessage("warning", "No Invoice to Export", "");
        return result;
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    else {

      List<Order> OrderList = null;
      XSSFWorkbook workbook = new XSSFWorkbook();

      CreationHelper createHelper = workbook.getCreationHelper();

      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MMM-yyyy"));

      XSSFSheet sheet = workbook.createSheet("sheet1");

      Object[] cellData = { "Name of the Store/Warehouse from where goods are transferred",
          "Store/Warehouse Code", "Store/Warehouse GSTIN No.",
          "Name of the Store/Warehouse to where goods are transferred (Receipient)",
          "Store/Warehouse Code (Receipient)", "Store/Warehouse GSTIN No. (Receipient)",
          "Invoice No.", "Invoice date", "Item Code", "HSN", "Nature of Product", "Qty", "Rate",
          "Qty*Rate", "IGST - Rate", "IGST - Amount", "CGST - Rate", "CGST - Amount", "SGST - Rate",
          "SGST - Amount", "VALUE INCLUDING TAX", "CC", "ICA", "Purchase Sub ICA", "Sale Sub ICA",
          "Debit", "Amount", "Credit ICA", "Amount" };

      int rowNum = 2;
      int colNum = 0;
      System.out.println("Creating excel");

      Row row = sheet.createRow(rowNum++);
      for (Object field : cellData) {
        Cell cell = row.createCell(colNum++);
        cell.setCellValue((String) field);
      }
      // for (Object[] queryListObj : queryList) {
      // paymentMethodId = queryListObj[1].toString();
      // paymentName = queryListObj[0].toString();
      // }
      // put the record in the sheet
      // modify the following logic according to the requirement........

      int colNum1 = 0;
      Row row1 = sheet.createRow(rowNum++);

      row1.createCell(colNum1++).setCellValue(StartDate);
      row1.createCell(colNum1++).setCellValue(EndDate);
      // Cell cell = row1.createCell(colNum1++);

      FILE_NAME = new Date() + ".xlsx";

      DownloadFile(workbook);

    }
    return result;
  }

  private static JSONObject generateJSONMessage(String msgType, String title, String text)
      throws JSONException {

    JSONObject result = new JSONObject();
    JSONObject returnMessage = new JSONObject();
    JSONArray actions = new JSONArray();

    returnMessage.put("msgType", msgType);
    returnMessage.put("msgTitle", title);
    returnMessage.put("msgText", text);
    JSONObject msgInAction = new JSONObject();
    msgInAction.put("showMsgInProcessView", returnMessage);
    actions.put(msgInAction);
    result.put("responseActions", actions);
    result.put("retryExecution", true);
    return result;
  }

  private static void DownloadFile(XSSFWorkbook workbook)
      throws FileNotFoundException, IOException, JSONException {
    String contextURL = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("context.url");
    String attachpath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");

    String FILeName = attachpath.trim() + "/" + FILE_NAME;

    FileOutputStream outputStream = new FileOutputStream(FILeName);
    workbook.write(outputStream);
    JSONArray actions = new JSONArray();
    String linkdocument = getDownloadUrl(FILE_NAME);

    JSONObject msgTotal = new JSONObject();
    msgTotal.put("msgType", "info");
    msgTotal.put("msgTitle",
        "Excel Report Generated!!" + " Click " + linkdocument + " to download ");
    JSONObject msgTotalAction = new JSONObject();
    msgTotalAction.put("showMsgInProcessView", msgTotal);

    actions.put(msgTotalAction);
    result.put("responseActions", actions);
    result.put("retryExecution", true);
  }

  private static String getDownloadUrl(String fullFileName) {
    String contextName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("context.name");
    String linkdocument = "/" + contextName
        + "/org.openbravo.warehouse.shipping.print/DownloadExcelFile.html?param=" + fullFileName;

    return "<a href='" + linkdocument + "'target='_blank' >here</a>";
  }

}
