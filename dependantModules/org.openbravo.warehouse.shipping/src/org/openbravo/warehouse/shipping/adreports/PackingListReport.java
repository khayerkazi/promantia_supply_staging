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

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellUtil;
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

  private static String FILE_NAME = ""; // GSTIN_TAXPERIOD_DateTime.xls
  static SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    String strShippingId = "";
    try {
      jsonRequest = new JSONObject(content);
      if (jsonRequest.has("Obwship_Shipping_ID")) {
        strShippingId = jsonRequest.getString("Obwship_Shipping_ID");
      }
      OBWSHIPShipping shippingObj = OBDal.getInstance().get(OBWSHIPShipping.class, strShippingId);
      if (shippingObj != null) {

        String shippingInvoiceNo = "";
        if (shippingObj.getGsUniqueno() != null) {
          shippingInvoiceNo = shippingObj.getGsUniqueno();
        } else {
          shippingInvoiceNo = shippingObj.getDocumentNo();

        }
        /* } */

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

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();

        if (parameters.containsKey("_action")) {

          String fileName = "PackingSalesReport_For-" + shippingInvoiceNo + "_on_"
              + dateFormat.format(date);
          String linkdocument = extractForShipping(shippingObj, fileName, shippingInvoiceNo);

          JSONObject msgTotal = new JSONObject();
          msgTotal.put("msgType", "info");
          msgTotal.put("msgTitle", "Report Generated!!" + " Click " + linkdocument
              + " to download ");
          JSONObject msgTotalAction = new JSONObject();
          msgTotalAction.put("showMsgInProcessView", msgTotal);
          JSONArray actions = new JSONArray();

          actions.put(msgTotalAction);
          jsonRequest.put("messege", msgTotal);
          jsonRequest.put("responseActions", actions);

          return jsonRequest;

        }
      }

    } catch (Exception e) {

      // TODO Auto-generated catch block
      e.printStackTrace();

    } finally {
      // log.info("CSV Export Process Finished...!!");
      OBContext.restorePreviousMode();
    }

    return jsonRequest;

  }

  @SuppressWarnings("hiding")
  public static String extractForShipping(OBWSHIPShipping shippingObj, String fileName,
      String shippingInvoiceNo) throws FileNotFoundException, IOException, JSONException,
      ParseException {

    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet("Packing Report");

    HSSFFont boldFont = workbook.createFont();
    boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
    boldFont.setFontName(HSSFFont.FONT_ARIAL);
    workbook.setSheetName(0, "Packing Report");

    Row row = null;
    Cell cell = null;

    setProductDetailsHeader(workbook, sheet, boldFont, 17);

    row = sheet.createRow(14);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row,
        "Place Of Discharge", true, cell, false);
    // sheet.autoSizeColumn(0);

    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Sri Lanka",
        false, cell, false);

    cell = row.createCell(2);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "In.transport Ref.", true, cell, false);

    row = sheet.createRow(15);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "Shipped by",
        true, cell, false);
    sheet.autoSizeColumn(0);

    cell = row.createCell(2);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Equipment NB",
        true, cell, false);
    sheet.autoSizeColumn(2);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Notify Party",
        true, cell, false);
    sheet.autoSizeColumn(5);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Same as Consignee", false, cell, false);
    // sheet.autoSizeColumn(7);
    row = sheet.createRow(16);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);

    row = sheet.createRow(1);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "Invoice No",
        true, cell, false);
    sheet.autoSizeColumn(0);

    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        shippingInvoiceNo, false, cell, false);
    sheet.autoSizeColumn(1);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Shipper", true,
        cell, false);
    sheet.autoSizeColumn(5);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Decathlon Sports India Pvt Ltd", false, cell, false);

    // sheet.autoSizeColumn(2);

    row = sheet.createRow(2);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Survey No - 78/10", false, cell, false);

    row = sheet.createRow(3);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "A2 - Chikkajala Village", false, cell, false);

    row = sheet.createRow(4);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "Date", true,
        cell, false);
    sheet.autoSizeColumn(0);

    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        formatter.format(shippingObj.getShipmentDate()), false, cell, false);
    sheet.autoSizeColumn(1);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "562157 Bangalore", false, cell, false);
    row = sheet.createRow(5);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "India", false,
        cell, false);

    row = sheet.createRow(7);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "No. 249, Stanley Thilakarathne Mawatha,", false, cell, false);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "No. 249, Stanley Thilakarathne Mawatha,", false, cell, false);

    row = sheet.createRow(8);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Nugegoda, Sri Lanka.", false, cell, false);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Nugegoda, Sri Lanka.", false, cell, false);

    row = sheet.createRow(9);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "TEL:  +94 112818345", false, cell, false);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "TEL:  +94 112818345", false, cell, false);

    row = sheet.createRow(10);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);
    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Mobile:  +94 77 9446832", false, cell, false);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Mobile:  +94 77 9446832", false, cell, false);
    setCellBolderleft(workbook, boldFont, row, 10);

    row = sheet.createRow(11);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "ETD", true,
        cell, false);
    sheet.autoSizeColumn(0);

    cell = row.createCell(2);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "ATD", true,
        cell, false);
    sheet.autoSizeColumn(2);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Cost Center",
        true, cell, false);
    sheet.autoSizeColumn(5);

    row = sheet.createRow(12);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "ATD", true,
        cell, false);
    sheet.autoSizeColumn(0);

    cell = row.createCell(2);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Place Of Loading", true, cell, false);
    // sheet.autoSizeColumn(2);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Incoterm",
        true, cell, false);
    sheet.autoSizeColumn(5);

    // sheet.autoSizeColumn(5);

    row = sheet.createRow(0);
    setCellBolderleft(workbook, boldFont, row, 10);
    cell = row.createCell(0);
    cell.setCellValue("PACKING LIST");
    cell.setCellStyle(getAlignStyle("Packing List", true, workbook, boldFont, true, true));
    sheet.addMergedRegion(CellRangeAddress.valueOf("A1:J1"));
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    row = sheet.createRow(6);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row,
        "Final Delivery Address", true, cell, false);
    // sheet.autoSizeColumn(0);

    cell = row.createCell(1);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Decathlon Lanka Sport Access (Pvt) Ltd.", false, cell, false);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Consignee",
        true, cell, false);
    sheet.autoSizeColumn(5);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Decathlon Lanka Sport Access (Pvt) Ltd.", false, cell, false);

    int rowNum = 18;
    Set<String> boxList = new HashSet<String>();
    int totalQty = 0;
    List<Object[]> queryList = getPackingReportProductDetailList(shippingObj);

    for (Object[] queryListObj : queryList) {

      int colNum1 = 0;
      row = sheet.createRow(rowNum++);

      cell = row.createCell(0);
      if (queryListObj[0] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[0].toString(), false, cell, true);
      } else
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            row.createCell(0), true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(1);
      if (queryListObj[1] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[1].toString(), false, cell, true);
      } else
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            row.createCell(1), true);

      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(2);
      if (queryListObj[2] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[2].toString(), false, cell, true);
      } else
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            row.createCell(2), true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(3);
      if (queryListObj[3] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[3].toString(), false, cell, true);
        sheet.autoSizeColumn(3);
      } else
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            row.createCell(3), true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(4);
      if (queryListObj[4] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[4].toString(), false, cell, true);
      } else
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            cell, true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(5);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false, cell,
          true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(6);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false, cell,
          true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(7);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false, cell,
          true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(8);
      if (queryListObj[5] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[5].toString(), false, cell, true);
        totalQty = totalQty + Integer.parseInt(queryListObj[5].toString());
      } else {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            cell, true);
      }
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(9);
      if (queryListObj[6] != null) {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
            queryListObj[6].toString().trim(), false, cell, true);
        boxList.add(queryListObj[6].toString());
        sheet.autoSizeColumn(9);
      } else {
        setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
            cell, true);
      }
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    }

    row = sheet.createRow(rowNum++);

    cell = row.createCell(0);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "Total", true,
        cell, true);
    sheet.addMergedRegion(CellRangeAddress.valueOf("A" + rowNum + ":F" + rowNum));
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_RIGHT);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "0", true, cell,
        true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    cell = row.createCell(6);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "0", true, cell,
        true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    cell = row.createCell(7);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "0", true, cell,
        true);

    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    cell = row.createCell(8);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
        String.valueOf(totalQty), true, cell, true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    cell = row.createCell(9);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
        String.valueOf(boxList.size()), true, cell, true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    row = sheet.createRow(rowNum++);

    cell = row.createCell(0);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "LUT ARN No",
        true, cell, false);
    // setCellBolder(workbook, boldFont, row, 1);

    row = sheet.createRow(rowNum++);
    cell = row.createCell(0);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
        "Total Volume (CBM)", true, cell, false);

    cell = row.createCell(1);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", true, cell,
        false);

    cell = row.createCell(2);
    cell.setCellValue("Shipping Remarks:");
    cell.setCellStyle(getAlignStyle("Shipping Remarks", true, workbook, boldFont, true, false));
    int nextFour = rowNum + 5;
    sheet.addMergedRegion(CellRangeAddress.valueOf("C" + rowNum + ":F" + nextFour + ""));
    CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,
        CellStyle.VERTICAL_TOP);

    cell = row.createCell(6);
    cell.setCellValue("Signed by");
    cell.setCellStyle(getAlignStyle("Signed by", true, workbook, boldFont, true, true));
    CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,
        CellStyle.VERTICAL_TOP);
    sheet.addMergedRegion(CellRangeAddress.valueOf("G" + rowNum + ":J" + nextFour + ""));

    setCellBolderleft(workbook, boldFont, row, 10);

    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    row = sheet.createRow(rowNum++);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
        "Total Net Weight (KG)", true, cell, false);
    cell = row.createCell(1);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true, cell,
        null);

    row = sheet.createRow(rowNum++);
    setCellBolderleft(workbook, boldFont, row, 10);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
        "Total Gross Weight (KG)", true, row.createCell(0), false);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true,
        row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellBolderleft(workbook, boldFont, row, 10);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
        "Total No. of Packages", true, row.createCell(0), false);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
        String.valueOf(boxList.size()), true, row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellBolderleft(workbook, boldFont, row, 10);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
        "Total No. of Pallets", true, row.createCell(0), false);
    setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true,
        row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellBolderleft(workbook, boldFont, row, 10);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "Total Pieces",
        true, row.createCell(0), false);
    setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
        String.valueOf(totalQty), true, row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    // setCellBolderleft(workbook, boldFont, row, 10);

    setCellBolderTop(workbook, boldFont, row, 2);
    setCellBolderTop(workbook, boldFont, row, 3);
    setCellBolderTop(workbook, boldFont, row, 4);
    setCellBolderTop(workbook, boldFont, row, 5);
    setCellBolderTop(workbook, boldFont, row, 6);
    setCellBolderTop(workbook, boldFont, row, 7);
    setCellBolderTop(workbook, boldFont, row, 8);
    setCellBolderTop(workbook, boldFont, row, 9);

    row = sheet.createRow(13);
    setCellBolderleft(workbook, boldFont, row, 10);

    cell = row.createCell(0);
    setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
        false);

    cell = row.createCell(5);
    setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
        "Terms Of Payment", true, cell, false);

    FILE_NAME = fileName + ".xls";

    return DownloadFile(workbook);

  }

  public static void setProductDetailsHeader(HSSFWorkbook workbook, HSSFSheet sheet,
      HSSFFont boldFont, int rowCount) {
    HSSFRow row;
    String[] cellData = { "Model code", "Item", "Nature of Product", "Size", "Criterion Code",
        "Country of Origin", "Net Weight", "Gross Weight", "Quantity", "Box Numbers" };

    int colNum = 0;
    row = sheet.createRow(rowCount);
    // setCellBolderRight(workbook, boldFont, row, 9);

    for (Object field : cellData) {

      final HSSFCell cell = row.createCell(colNum);
      row.setHeight((short) 0);
      HSSFCellStyle cellStyle = workbook.createCellStyle();
      // int
      cell.setCellValue((String) field);
      cellStyle.setFont(boldFont);
      cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
      cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
      cellStyle.setAlignment(cellStyle.ALIGN_CENTER);
      cellStyle.setVerticalAlignment(CellStyle.ALIGN_FILL);
      cellStyle.setWrapText(true);
      // cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      if (field.equals("Box Numbers")) {
        cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
      }
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
      cell.setCellStyle(cellStyle);
      System.out.println("field==" + field);

      sheet.autoSizeColumn(colNum);
      colNum++;
    }
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

  private static String DownloadFile(HSSFWorkbook workbook) throws FileNotFoundException,
      IOException, JSONException {

    String attachpath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");

    String FILeName = attachpath.trim() + "/" + FILE_NAME;

    FileOutputStream outputStream = new FileOutputStream(FILeName);
    workbook.write(outputStream);
    String linkdocument = getDownloadUrl(FILE_NAME);

    return linkdocument;
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

  public static void setCellvalueWithAlignment(Boolean withLeftBorder, Boolean withBorder,
      Boolean withBottomBorder, Boolean isRightBorder, HSSFWorkbook workbook, HSSFFont boldFont,
      Row row, String message, Boolean isBold, Cell cell, Boolean isCenterAllignment) {
    cell.setCellValue(message);
    HSSFCellStyle cellStyle = workbook.createCellStyle();
    if (message.equals("Final Delivery Address")
        || message.equalsIgnoreCase("Total Net Weight (KG)")
        || message.equalsIgnoreCase("Total Gross Weight (KG)")
        || message.equalsIgnoreCase("Total No. of Packages")) {
      row.setHeight((short) 0);
      cellStyle.setVerticalAlignment(CellStyle.ALIGN_FILL);
      cellStyle.setWrapText(true);
    }

    if (isBold) {
      cellStyle.setFont(boldFont);
    }
    if (isCenterAllignment == null) {
      cellStyle.setAlignment(cellStyle.ALIGN_RIGHT);

    } else if (isCenterAllignment) {
      cellStyle.setAlignment(cellStyle.ALIGN_CENTER);
      // cellStyle.setVerticalAlignment(CellStyle.ALIGN_FILL);

    } else {
      cellStyle.setAlignment(cellStyle.ALIGN_LEFT);
    }

    if (isRightBorder) {

      cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
    }
    if (withBorder) {
      cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
    }
    if (withLeftBorder) {
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

    }
    if (withBottomBorder) {
      cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    }
    cell.setCellStyle(cellStyle);

  }

  public static void setCellBolder(HSSFWorkbook workbook, HSSFFont boldFont, Row row, int cellCount) {

    Cell cell;
    cell = row.createCell(cellCount);
    HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

    /*
     * centerAlignStyleWithBold.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
     * centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
     * centerAlignStyleWithBold.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
     * centerAlignStyleWithBold.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
     */

    cell.setCellStyle(centerAlignStyleWithBold);

  }

  public static void setCellBolderTop(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      int cellCount) {

    Cell cell;
    cell = row.createCell(cellCount);
    HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();
    centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
    cell.setCellStyle(centerAlignStyleWithBold);

  }

  public static void setCellBolderleft(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      int cellCount) {

    Cell cell;
    cell = row.createCell(cellCount);
    HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

    centerAlignStyleWithBold.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

    cell.setCellStyle(centerAlignStyleWithBold);

  }

  public static HSSFCellStyle getAlignStyle(String Text, Boolean isRightBorder,
      HSSFWorkbook workbook, HSSFFont boldFont, Boolean isBoldText, Boolean isCenterAllignment) {
    HSSFCellStyle cellStyle = workbook.createCellStyle();

    if (isBoldText) {
      cellStyle.setFont(boldFont);
    }
    if (isCenterAllignment == null) {
      cellStyle.setAlignment(cellStyle.ALIGN_RIGHT);

    } else if (isCenterAllignment) {
      cellStyle.setAlignment(cellStyle.ALIGN_CENTER);
      // cellStyle.setVerticalAlignment(CellStyle.ALIGN_FILL);
    } else {
      cellStyle.setAlignment(cellStyle.ALIGN_LEFT);
    }
    // cellStyle.setRotation((short) 90);
    if (Text.equalsIgnoreCase("Signed by")) {
      cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    } else {
      cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    }
    if (isRightBorder) {

      // cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
    }
    return cellStyle;
  }
}
