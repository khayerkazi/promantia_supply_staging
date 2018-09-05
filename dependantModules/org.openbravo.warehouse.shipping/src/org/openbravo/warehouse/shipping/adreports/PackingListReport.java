package org.openbravo.warehouse.shipping.adreports;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;
import org.openbravo.warehouse.shipping.OBWSHIPShippingDetails;

public class PackingListReport extends BaseProcessActionHandler {

  private static String SDate = "2018-08-01";
  private static String EDate = "2018-08-29";
  private static String FILE_NAME = ""; // GSTIN_TAXPERIOD_DateTime.xls
  private static String IType = "shipping";
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  static JSONObject result = new JSONObject();
  static JSONObject output = new JSONObject();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    // select value into v_hsncode from ingst_gstproductcode where ingst_gstproductcode_id =(select
    // em_ingst_gstproductcode_id from m_product where m_product_id=new.m_product_id);
    JSONObject jsonRequest = null;
    // String message = new String();
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      String strShippingId = jsonRequest.getString("Obwship_Shipping_ID");

      OBWSHIPShipping shippingObj = OBDal.getInstance().get(OBWSHIPShipping.class, strShippingId);
      String packingInvoiceNo = "";
      if (shippingObj.getPackinginvoiceno() != null) {
        packingInvoiceNo = shippingObj.getPackinginvoiceno();
      } else {
        packingInvoiceNo = shippingObj.getGsUniqueno();

      }
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Date date = new Date();

      if (shippingObj != null) {
        if (parameters.containsKey("_action")) {
          if (parameters.get("_action").equals(
              "org.openbravo.warehouse.shipping.adreports.PackingListReport")) {
            String fileName = "PackingSalesReport_For-" + packingInvoiceNo + "_on_"
                + dateFormat.format(date);
            result = PackingListReport.extractForShipping(shippingObj, fileName);
          } else {
            if (parameters.get("_action").equals(
                "org.openbravo.warehouse.shipping.adreports.ShippingListReport")) {
              String fileName = "ShippingSalesReport_For-" + packingInvoiceNo + "_on_"
                  + dateFormat.format(date);
            }
          }

        }
      }
      for (OBWSHIPShippingDetails shippinglineObj : shippingObj.getOBWSHIPShippingDetailsList()) {
        if (shippinglineObj.getGoodsShipment() != null) {
          for (ShipmentInOutLine inoutLineObj : shippinglineObj.getGoodsShipment()
              .getMaterialMgmtShipmentInOutLineList()) {
            if (inoutLineObj.getObwshipHsncode() == null) {
              if (inoutLineObj.getProduct() != null) {
                if (inoutLineObj.getProduct().getIngstGstproductcode() != null) {
                  if (inoutLineObj.getProduct().getIngstGstproductcode().getValue() != null) {
                    inoutLineObj.setObwshipHsncode(inoutLineObj.getProduct()
                        .getIngstGstproductcode().getValue());
                    OBDal.getInstance().save(inoutLineObj);
                    OBDal.getInstance().flush();
                  }
                }
              }
            }
          }
        }
      }

    } catch (Exception e) {
      try {
        result = generateJSONMessage("error", "Error in Excel Export Transaction ", e.getMessage());
        // log.error("Error in Excel Export Transaction" + e.getMessage());
      } catch (JSONException e1) {
        result = new JSONObject();
      }
      // TODO Auto-generated catch block
      e.printStackTrace();

    }

    finally {
      // log.info("CSV Export Process Finished...!!");
      OBContext.restorePreviousMode();
    }

    return result;
  }

  @SuppressWarnings("hiding")
  public static JSONObject extractForShipping(OBWSHIPShipping shippingObj, String fileName)
      throws FileNotFoundException, IOException, JSONException, ParseException {

    String sql = "select "
        + "mw.name as WHName,mw.value as WHCode,mw.em_gs_gstin as WHGSTINNo, "
        + "org.name as OrgName,org.value as OrgCode, "
        + "ingst.uidno as StoreGSTINUniqueID,os.EM_Gs_Uniqueno as INVOICENUMBER,  "
        + "to_char(os.shipment_date,'YYYY-mm-dd') as DateofInvoice, "
        + "mp.name as ProductName,  "
        + "COALESCE(ml.em_obwship_hsncode,gst.value) as ProductHSNCode, "
        + "mp.em_cl_modelname as ModelName,  "
        + "ml.movementqty as ShipmentQty, "
        + "ml.em_obwship_cessionprice as CessionPrice, "
        + "(ml.movementqty*ml.em_obwship_cessionprice) as TaxableValue, "
        + " ml.em_obwship_taxrate as INGSTRATE, "
        + " ml.em_obwship_taxamount as TotalTaxValue,  (ml.em_obwship_taxableamount+ml.em_obwship_taxamount) as Total "
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
        + "where to_char(os.shipment_date,'YYYY-mm-dd')<= '"
        + EDate
        + "' and  to_char(os.shipment_date,'YYYY-mm-dd')>= '"
        + SDate
        + "' and "
        + "mpp.m_pricelist_version_id ='0F39C05C15EE4E5BB50BD5FEC1645DA1' "
        + "group by ml.em_obwship_hsncode, mw.name  ,mw.value ,mw.em_gs_gstin  , org.name  ,org.value  ,ingst.uidno  ,os.EM_Gs_Uniqueno , "
        + " os.shipment_date,  mp.name , gst.value ,mp.em_cl_modelname , ct.rate, ml.movementqty , "
        + "ml.em_obwship_cessionprice ,ml.movementqty,ml.em_obwship_cessionprice, ml.em_obwship_taxableamount, ml.em_obwship_taxamount, ml.em_obwship_taxrate "
        + "";

    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
    @SuppressWarnings("unchecked")
    List<Object[]> queryList = query.list();

    if (queryList.size() == 0) {
      try {
        result = generateJSONMessage("warning", "No Invoice to Export", "");
        // log.info("No Invoice to Export.");
        return result;
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    else {

      XSSFWorkbook workbook = new XSSFWorkbook();
      CreationHelper createHelper = workbook.getCreationHelper();
      CellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MMM-yyyy"));
      // this is for sheet name
      XSSFSheet sheet = workbook.createSheet("Shipping");
      XSSFFont cellFont = workbook.createFont();
      Object[] cellData = { "Name of the Store/Warehouse from where goods are transferred",
          "Store/Warehouse Code", "Store/Warehouse GSTIN No.",
          "Name of the Store/Warehouse to where goods are transferred (Receipient)",
          "Store/Warehouse Code (Receipient)", "Store/Warehouse GSTIN No. (Receipient)",
          "Invoice No.", "Invoice date", "Item Code", "HSN", "Nature of Product", "Qty", "Rate",
          "Qty*Rate", "IGST - Rate", "IGST - Amount", "CGST - Rate", "CGST - Amount",
          "SGST - Rate", "SGST - Amount", "VALUE INCLUDING TAX", "CC", "ICA", "Purchase Sub ICA",
          "Sale Sub ICA", "Debit", "Amount", "Credit ICA", "Amount" };

      int rowNum = 1;
      int colNum = 0;

      // log.info("Creating Excel.");
      Row row = sheet.createRow(rowNum++);
      for (Object field : cellData) {
        Cell cell = row.createCell(colNum++);
        cell.setCellValue((String) field);
      }
      Row row3 = sheet.createRow(0);
      row3.createCell(0).setCellValue("PACKING LIST");
      cellStyle.setAlignment(cellStyle.ALIGN_CENTER);
      cellFont.setBold(true);
      sheet.addMergedRegion(CellRangeAddress.valueOf("A1:J1"));

      for (Object[] queryListObj : queryList) {

        if (queryListObj[6] != null) {

          int colNum1 = 0;
          Row row1 = sheet.createRow(rowNum++);

          if (queryListObj[0] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[0].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[1] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[1].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[2] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[2].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[3] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[3].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[4] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[4].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[5] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[5].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[6] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[6].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[7] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[7].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[8] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[8].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[9] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[9].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[10] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[10].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[11] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[11].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[12] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[12].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[13] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[13].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[14] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[14].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[15] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[15].toString());
          else
            row1.createCell(colNum1++).setCellValue("");

          row1.createCell(colNum1++).setCellValue("");
          row1.createCell(colNum1++).setCellValue("");
          row1.createCell(colNum1++).setCellValue("");
          row1.createCell(colNum1++).setCellValue("");

          if (queryListObj[16] != null)
            row1.createCell(colNum1++).setCellValue(queryListObj[16].toString());
          else
            row1.createCell(colNum1++).setCellValue(0.00);

        }
      }
      FILE_NAME = fileName + ".xlsx";

      DownloadFile(workbook);

    }
    return result;
  }

  private static JSONObject generateJSONMessage(String msgType, String title, String text)
      throws JSONException {

    @SuppressWarnings("hiding")
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

  private static void DownloadFile(XSSFWorkbook workbook) throws FileNotFoundException,
      IOException, JSONException {

    String attachpath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");

    String FILeName = attachpath.trim() + "/" + FILE_NAME;

    FileOutputStream outputStream = new FileOutputStream(FILeName);
    workbook.write(outputStream);
    JSONArray actions = new JSONArray();
    String linkdocument = getDownloadUrl(FILE_NAME);

    JSONObject msgTotal = new JSONObject();
    msgTotal.put("msgType", "info");
    msgTotal.put("msgTitle", "Excel Report Generated!!" + " Click " + linkdocument
        + " to download ");
    JSONObject msgTotalAction = new JSONObject();
    msgTotalAction.put("showMsgInProcessView", msgTotal);

    actions.put(msgTotalAction);
    result.put("responseActions", actions);
    result.put("retryExecution", true);

    // log.info("Excel Report '" + FILE_NAME + "' Generated...!!");
  }

  private static String getDownloadUrl(String fullFileName) {
    String contextName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("context.name");
    String linkdocument = "/" + contextName
        + "/org.openbravo.warehouse.shipping.print/DownloadExcelFile.html?param=" + fullFileName;
    // // log.info("The final URL to download the Export Sheet: " + linkdocument);

    return "<a href='" + linkdocument + "'target='_blank' >here</a>";
  }

}
