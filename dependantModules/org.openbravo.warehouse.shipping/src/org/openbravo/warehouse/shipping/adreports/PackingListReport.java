package org.openbravo.warehouse.shipping.adreports;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.RegionUtil;
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

    List<Object[]> queryList = getPackingReportProductDetailList(shippingObj);

    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet("Style example");

    HSSFFont boldFont = workbook.createFont();
    boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
    boldFont.setFontName(HSSFFont.FONT_ARIAL);
    // this is for row 0
    Row row = sheet.createRow(1);
    Cell cell = row.createCell(0);
    /*
     * for (int j = 1; j <= 17; j++) { row = sheet.createRow(j); for (int i = 0; i <= 9; i++) {
     * 
     * CellRangeAddress mergedCell = new CellRangeAddress(j, // first row (0-based) 17, // last row
     * (0-based) i, // first column (0-based) 9 // last column (0-based) ); //
     * sheet.addMergedRegion(mergedCell);
     * 
     * setRegionBorderWithMedium(mergedCell, sheet); // setCellBolder(workbook, boldFont, row, i); }
     * }
     */
    String[] cellData = { "Model code", "Item", "Nature of Product", "Size", "Criterion Code",
        "Country of Origin", "Net Weight (KG)", "Gross Weight", "Quantity", "Box Numbers" };

    int colNum = 0;
    row = sheet.createRow(18);
    for (Object field : cellData) {
      setCellvalueWithAlignment(workbook, boldFont, row, (String) field, true, colNum, true, "");
      sheet.autoSizeColumn(colNum);
      colNum++;
    }

    row = sheet.createRow(16);
    setCellvalueWithAlignment(workbook, boldFont, row, "SHIPPED BY", true, 0, false, "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "EQUIPMENT NB", true, 2, false, "");
    sheet.autoSizeColumn(2);
    setCellvalueWithAlignment(workbook, boldFont, row, "NOTIFY PARTY", true, 8, false, "");
    sheet.autoSizeColumn(8);
    setCellvalueWithAlignment(workbook, boldFont, row, "SAME AS CONSIGNEE", false, 9, false, "");
    sheet.autoSizeColumn(9);

    row = sheet.createRow(2);

    setCellvalueWithAlignment(workbook, boldFont, row, "Invoice No", true, 0, false, "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "123", false, 1, false, "");
    sheet.autoSizeColumn(1);

    setCellvalueWithAlignment(workbook, boldFont, row, "SHIPPER", true, 8, false, "");
    sheet.autoSizeColumn(8);

    setCellvalueWithAlignment(workbook, boldFont, row, "DECATHLON SPORTS INDIA PVT LTD", false, 9,
        false, "");

    row = sheet.createRow(15);
    setCellvalueWithAlignment(workbook, boldFont, row, "PLACE OF DISCHARGE", true, 0, false, "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "Sri Lanka", false, 1, false, "");
    sheet.autoSizeColumn(1);

    setCellvalueWithAlignment(workbook, boldFont, row, "IN.TRANSPORT REF.", true, 2, false, "");
    sheet.autoSizeColumn(2);

    // this is for row 2
    row = sheet.createRow(3);
    setCellvalueWithAlignment(workbook, boldFont, row, "SURVEY NO 78/10", false, 9, false, "");

    // this is for row 3
    row = sheet.createRow(4);
    setCellvalueWithAlignment(workbook, boldFont, row, "A2 0-CHIKKAJALA VILLAGE BELLARY", false, 9,
        false, "");

    row = sheet.createRow(5);
    setCellvalueWithAlignment(workbook, boldFont, row, "Date", true, 0, false, "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "12-08-2018", false, 1, false, "");
    sheet.autoSizeColumn(1);

    setCellvalueWithAlignment(workbook, boldFont, row, "562157 BANGALORE", false, 9, false, "");

    row = sheet.createRow(6);
    setCellvalueWithAlignment(workbook, boldFont, row, "INDIA", false, 9, false, "");

    row = sheet.createRow(7);
    setCellvalueWithAlignment(workbook, boldFont, row, "FINAL  DELIVERY ADDRESS", true, 0, false,
        "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "DECATHLON LANKA SPORT ACCESS (PVT) LTD.",
        false, 1, false, "");

    setCellvalueWithAlignment(workbook, boldFont, row, "CONSIGNEE", true, 8, false, "");
    sheet.autoSizeColumn(8);

    setCellvalueWithAlignment(workbook, boldFont, row, "DECATHLON LANKA SPORT ACCESS (PVT) LTD.",
        false, 9, false, "");

    row = sheet.createRow(8);
    setCellvalueWithAlignment(workbook, boldFont, row, "NO. 249, STANLEY THILAKARATHNE MAWATHA,",
        false, 1, false, "");

    setCellvalueWithAlignment(workbook, boldFont, row, "NO. 249, STANLEY THILAKARATHNE MAWATHA,",
        false, 9, false, "");

    row = sheet.createRow(9);
    setCellvalueWithAlignment(workbook, boldFont, row, "NUGEGODA, SRI LANKA.", false, 1, false, "");
    setCellvalueWithAlignment(workbook, boldFont, row, "NUGEGODA, SRI LANKA.", false, 9, false, "");

    row = sheet.createRow(10);
    setCellvalueWithAlignment(workbook, boldFont, row, "TEL:  +94 112818345", false, 1, false, "");
    setCellvalueWithAlignment(workbook, boldFont, row, "TEL:  +94 112818345", false, 9, false, "");

    row = sheet.createRow(11);
    setCellvalueWithAlignment(workbook, boldFont, row, "MOBILE:  +94 77 9446832", false, 1, false,
        "");
    setCellvalueWithAlignment(workbook, boldFont, row, "MOBILE:  +94 77 9446832", false, 9, false,
        "");

    row = sheet.createRow(12);
    setCellvalueWithAlignment(workbook, boldFont, row, "ETD", true, 0, false, "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "ATD", true, 2, false, "");
    sheet.autoSizeColumn(2);

    setCellvalueWithAlignment(workbook, boldFont, row, "COST CENTER", true, 8, false, "");
    sheet.autoSizeColumn(8);

    row = sheet.createRow(13);
    setCellvalueWithAlignment(workbook, boldFont, row, "ATD", true, 0, false, "");
    sheet.autoSizeColumn(0);

    setCellvalueWithAlignment(workbook, boldFont, row, "PLACE OF LOADING", true, 2, false, "");
    sheet.autoSizeColumn(2);

    setCellvalueWithAlignment(workbook, boldFont, row, "INCOTERM", true, 8, false, "");
    sheet.autoSizeColumn(8);

    row = sheet.createRow(14);
    setCellvalueWithAlignment(workbook, boldFont, row, "TERMS OF PAYMENT", true, 8, false, "");
    sheet.autoSizeColumn(8);

    row = sheet.createRow(1);
    cell = row.createCell(0);
    cell.setCellValue("PACKING LIST");
    cell.setCellStyle(getAlignStyle(workbook, boldFont, true, true));
    sheet.addMergedRegion(CellRangeAddress.valueOf("A2:J2"));

    /*
     * for (int i = 19; i < 25; i++) { row = sheet.createRow(i);
     * 
     * for (int j = 0; j < colNum; j++) {
     * 
     * setCellvalueWithAlignment(workbook, boldFont, row, String.valueOf(j), false, j, true, ""); }
     * }
     */
    int rowNum = 19;
    Set<String> boxList = new HashSet<String>();
    int totalQty = 0;

    for (Object[] queryListObj : queryList) {
      if (queryListObj[6] != null) {

        int colNum1 = 0;
        row = sheet.createRow(rowNum++);

        if (queryListObj[0] != null)
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[0].toString(), false,
              colNum1++, true, "");
        else
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");
        if (queryListObj[1] != null)
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[1].toString(), false,
              colNum1++, true, "");
        else
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");

        if (queryListObj[2] != null)
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[2].toString(), false,
              colNum1++, true, "");
        else
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");
        if (queryListObj[3] != null)
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[3].toString(), false,
              colNum1++, true, "");
        else
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");
        if (queryListObj[4] != null)
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[4].toString(), false,
              colNum1++, true, "");
        else
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");

        setCellvalueWithAlignment(workbook, boldFont, row, "N/A", false, colNum1++, true, "");
        setCellvalueWithAlignment(workbook, boldFont, row, "N/A", false, colNum1++, true, "");
        setCellvalueWithAlignment(workbook, boldFont, row, "N/A", false, colNum1++, true, "");
        if (queryListObj[5] != null) {
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[5].toString(), false,
              colNum1++, true, "");
          totalQty = totalQty + Integer.parseInt(queryListObj[5].toString());
        } else {
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");
        }
        if (queryListObj[6] != null) {
          setCellvalueWithAlignment(workbook, boldFont, row, queryListObj[6].toString(), false,
              colNum1++, true, "");
          boxList.add(queryListObj[6].toString());

        } else {
          setCellvalueWithAlignment(workbook, boldFont, row, "", false, colNum1++, true, "");
        }
      }

    }
    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "Total", false, 0, true, "");

    setCellvalueWithAlignment(workbook, boldFont, row, "", true, 5, true, "0");
    // sheet.addMergedRegion(CellRangeAddress.valueOf("A2:E2"));
    setCellvalueWithAlignment(workbook, boldFont, row, "0", true, 6, true, "");
    setCellvalueWithAlignment(workbook, boldFont, row, "0", true, 7, true, "");
    /*
     * String cellsFrom_A1_to_A3 = "H20:H" + rowNum;
     * 
     * setCellvalueWithAlignment(workbook, boldFont, row, "", true, 11, true, ("SUM(" +
     * cellsFrom_A1_to_A3 + ")"));
     */
    setCellvalueWithAlignment(workbook, boldFont, row, String.valueOf(totalQty), true, 8, true, "");

    setCellvalueWithAlignment(workbook, boldFont, row, String.valueOf(boxList.size()), true, 9,
        true, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "LUT ARN No", true, 0, true, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "TOTAL VOLUME (CBM)", true, 0, true, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "TOTAL NET WEIGHT (Kg)", true, 0, true, "");
    setCellvalueWithAlignment(workbook, boldFont, row, "0", true, 1, null, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "TOTAL GROSS WEIGHT (Kg)", true, 0, true, "");
    setCellvalueWithAlignment(workbook, boldFont, row, "0", true, 1, null, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "TOTAL NUMBER OF PACKAGES", true, 0, true,
        "");
    setCellvalueWithAlignment(workbook, boldFont, row, String.valueOf(boxList.size()), true, 1,
        null, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "TOTAL NUMBER OF PALLETS", true, 0, true, "");
    setCellvalueWithAlignment(workbook, boldFont, row, "0", true, 1, null, "");

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(workbook, boldFont, row, "TOTAL PIECES", true, 0, true, "");
    setCellvalueWithAlignment(workbook, boldFont, row, String.valueOf(totalQty), true, 1, null, "");

    FILE_NAME = fileName + ".xlsx";

    DownloadFile(workbook);

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

  private static void DownloadFile(HSSFWorkbook workbook) throws FileNotFoundException,
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

  public static List<Object[]> getPackingReportProductDetailList(OBWSHIPShipping shippingObj) {
    String sql = "select  mp.em_cl_modelcode as ModelCode,  "
        + " mp.name as ProductName,  "
        + " cln.name as natualofproduct,"
        + " mp.em_cl_size as size,"
        + " COALESCE(ml.em_obwship_hsncode,gst.value) as ProductHSNCode, "
        + " ml.movementqty as ShipmentQty, "
        + "  mi.documentno "
        + " from obwship_shipping os "
        + " left join obwship_shipping_details osd on os.obwship_shipping_id = osd.obwship_shipping_id  "
        + " left join m_inout mi on mi.m_inout_id = osd.m_inout_id  "
        + " left join m_inoutline ml on ml.m_inout_id = mi.m_inout_id "
        + " left join m_product mp on mp.m_product_id = ml.m_product_id "
        + " left join INGST_GSTProductCode gst on gst.INGST_GSTProductCode_id = mp.EM_Ingst_Gstproductcode_ID "
        + " left join m_productprice mpp on mpp.m_product_id = mp.m_product_id   "
        + " left join cl_model cl on cl.cl_model_id=mp.em_cl_model_id "
        + " left join cl_natureofproduct cln on cln.cl_natureofproduct_id=cl.cl_natureofproduct_id "
        + " where   os.obwship_shipping_id= '" + shippingObj.getId() + "' "
        + " group by   mp.em_cl_modelcode ,  mp.name , " + "  mp.em_cl_size ," + " cln.name ,"
        + " COALESCE(ml.em_obwship_hsncode,gst.value) , " + " ml.movementqty ,  mi.documentno"
        + " order by  mi.documentno,mp.name ";

    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
    @SuppressWarnings("unchecked")
    List<Object[]> queryList = query.list();
    return queryList;
  }

  public static void setCellvalueWithAlignment(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      String message, Boolean isBold, int cellCount, Boolean isCenterAllignment, String formula) {
    Cell cell;
    cell = row.createCell(cellCount);
    if (formula.equalsIgnoreCase("") || formula == null) {
      cell.setCellValue(message);

    } else {
      cell.setCellFormula(formula);
    }

    cell.setCellStyle(getAlignStyle(workbook, boldFont, isBold, isCenterAllignment));

  }

  public static void setCellBolder(HSSFWorkbook workbook, HSSFFont boldFont, Row row, int cellCount) {

    Cell cell;
    cell = row.createCell(cellCount);
    HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

    centerAlignStyleWithBold.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
    centerAlignStyleWithBold.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
    centerAlignStyleWithBold.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

    cell.setCellStyle(centerAlignStyleWithBold);

  }

  private static void setRegionBorderWithMedium(
      org.apache.poi.ss.util.CellRangeAddress cellRangeAddress, Sheet sheet) {
    Workbook wb = sheet.getWorkbook();
    RegionUtil.setBorderBottom(CellStyle.BORDER_MEDIUM, cellRangeAddress, sheet, wb);
    RegionUtil.setBorderLeft(CellStyle.BORDER_MEDIUM, cellRangeAddress, sheet, wb);
    RegionUtil.setBorderRight(CellStyle.BORDER_MEDIUM, cellRangeAddress, sheet, wb);
    RegionUtil.setBorderTop(CellStyle.BORDER_MEDIUM, cellRangeAddress, sheet, wb);
  }

  public static HSSFCellStyle getAlignStyle(HSSFWorkbook workbook, HSSFFont boldFont,
      Boolean isBoldText, Boolean isCenterAllignment) {
    HSSFCellStyle leftAlignStyleWithBold = workbook.createCellStyle();
    if (isBoldText) {
      leftAlignStyleWithBold.setFont(boldFont);
    }
    if (isCenterAllignment == null) {
      leftAlignStyleWithBold.setAlignment(leftAlignStyleWithBold.ALIGN_RIGHT);

    } else if (isCenterAllignment) {
      leftAlignStyleWithBold.setAlignment(leftAlignStyleWithBold.ALIGN_CENTER);
    } else {
      leftAlignStyleWithBold.setAlignment(leftAlignStyleWithBold.ALIGN_LEFT);

    }

    leftAlignStyleWithBold.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    leftAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
    leftAlignStyleWithBold.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
    leftAlignStyleWithBold.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

    return leftAlignStyleWithBold;
  }
}
