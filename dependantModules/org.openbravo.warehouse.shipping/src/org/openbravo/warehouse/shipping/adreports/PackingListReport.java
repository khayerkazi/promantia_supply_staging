package org.openbravo.warehouse.shipping.adreports;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
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
  public static final Logger log = Logger.getLogger(PackingListReport.class);

  private static String FILE_NAME = ""; // GSTIN_TAXPERIOD_DateTime.xls
  static SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");
  static HSSFCellStyle cellStyle = null;
  static HSSFSheet sheet = null;

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
          shippingInvoiceNo = shippingObj.getGsUniqueno().replace("/", "_");
        } else {
          shippingInvoiceNo = shippingObj.getDocumentNo().replace("/", "_");

        }
        log.error("Packing Invoice Report calling for " + shippingInvoiceNo);

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
              + dateFormat.format(shippingObj.getShipmentDate());
          String linkdocument = extractForShipping(shippingObj, fileName,
              shippingObj.getGsUniqueno(), true);
          log.error("Packing Invoice Report Processed Successfully for " + shippingInvoiceNo
              + " and doc link is: " + linkdocument);

          JSONObject msgTotal = new JSONObject();
          msgTotal.put("msgType", "info");
          msgTotal.put("msgTitle", "Packing Report Generated!!" + " Click " + linkdocument
              + " to download ");
          JSONObject msgTotalAction = new JSONObject();
          msgTotalAction.put("showMsgInProcessView", msgTotal);
          JSONArray actions = new JSONArray();

          actions.put(msgTotalAction);
          jsonRequest.put("messege", msgTotal);
          jsonRequest.put("responseActions", actions);
          sheet = null;

          return jsonRequest;

        }
      }

    } catch (Exception e) {
      log.error(e);

      e.printStackTrace();

    } finally {
      OBContext.restorePreviousMode();

    }
    return jsonRequest;

  }

  @SuppressWarnings("hiding")
  public static String extractForShipping(OBWSHIPShipping shippingObj, String fileName,
      String shippingInvoiceNo, boolean isPackingReport) throws Exception {

    HSSFWorkbook workbook = new HSSFWorkbook();
    int TotalRowCount = 0;
    String strFormula = "";
    int MaxRowCount = 0;
    try {
      if (isPackingReport) {
        sheet = workbook.createSheet("Packing Report");
        workbook.setSheetName(0, "Packing Report");
        MaxRowCount = 10;
      } else {
        sheet = workbook.createSheet("Shipping Report");
        workbook.setSheetName(0, "Shipping Report");
        MaxRowCount = 9;

      }
      HSSFFont boldFont = workbook.createFont();
      boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
      boldFont.setFontName(HSSFFont.FONT_ARIAL);
      Row row = null;
      Cell cell = null;

      setProductDetailsHeader(workbook, sheet, boldFont, 17, isPackingReport);

      row = sheet.createRow(14);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row,
          "Place Of Discharge", true, cell, false);
      cell = row.createCell(1);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Sri Lanka",
          false, cell, false);
      cell = row.createCell(2);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "In.transport Ref.", true, cell, false);

      cell = row.createCell(MaxRowCount);
      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(15);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "Shipped by",
          true, cell, false);
      sheet.autoSizeColumn(0);
      cell = row.createCell(2);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Equipment NB", true, cell, false);
      sheet.autoSizeColumn(2);
      cell = row.createCell(5);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Notify Party", true, cell, false);
      sheet.autoSizeColumn(5);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Same as Consignee", false, cell, false);
      // sheet.autoSizeColumn(7);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(16);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(1);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "Invoice No",
          true, cell, false);
      sheet.autoSizeColumn(0);
      cell = row.createCell(1);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          shippingInvoiceNo, false, cell, false);
      sheet.autoSizeColumn(1);
      cell = row.createCell(5);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "Shipper",
          true, cell, false);
      sheet.autoSizeColumn(5);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Decathlon Sports India Pvt Ltd", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);
      // sheet.autoSizeColumn(2);

      row = sheet.createRow(2);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Survey No - 78/10", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(3);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "A2 - Chikkajala Village", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(4);
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

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(5);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row, "India",
          false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(7);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(1);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "No. 249, Stanley Thilakarathne Mawatha,", false, cell, false);

      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "No. 249, Stanley Thilakarathne Mawatha,", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(8);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(1);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Nugegoda, Sri Lanka.", false, cell, false);

      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Nugegoda, Sri Lanka.", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", false,
          cell, false);

      row = sheet.createRow(9);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(1);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "TEL:  +94 112818345", false, cell, false);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "TEL:  +94 112818345", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", false,
          cell, false);

      row = sheet.createRow(10);
      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(1);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Mobile:  +94 77 9446832", false, cell, false);
      cell = row.createCell(6);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Mobile:  +94 77 9446832", false, cell, false);

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(11);
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

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(12);
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

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);
      // sheet.autoSizeColumn(5);

      row = sheet.createRow(0);
      cell = row.createCell(0);
      if (isPackingReport) {
        cell.setCellValue("PACKING LIST");
        cell.setCellStyle(getAlignStyle("Packing List", true, workbook, boldFont, true, true));
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:J1"));

      } else {
        cell.setCellValue("COMMERCIAL INVOICE");
        cell.setCellStyle(getAlignStyle("Commercial invoice", true, workbook, boldFont, true, true));
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:I1"));

      }

      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      for (int i = 1; i <= MaxRowCount; i++) {
        cell = row.createCell(i);
        setCellBolderTopAndBottom(workbook, boldFont, row, cell);
      }

      cell = row.createCell(MaxRowCount);
      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(6);
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

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      int rowNum = 18;
      Set<String> boxList = new HashSet<String>();
      int totalQty = 0;
      BigDecimal totalAmt = BigDecimal.ZERO;
      List<Object[]> queryList = getInvoiceDetails(shippingObj);

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

        if (isPackingReport) {

          cell = row.createCell(5);
          setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
              cell, true);
          CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

          cell = row.createCell(6);
          setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
              cell, true);
          CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

          cell = row.createCell(7);
          setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
              cell, true);
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

        } else {

          cell = row.createCell(5);
          setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
              cell, true);
          CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

          cell = row.createCell(6);
          if (queryListObj[5] != null) {
            setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
                queryListObj[5].toString(), false, cell, true);
            totalQty = totalQty + Integer.parseInt(queryListObj[5].toString());
          } else {
            setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
                cell, true);
          }
          CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
          if (queryListObj[6] != null) {
            boxList.add(queryListObj[6].toString());
          }
          cell = row.createCell(7);
          if (queryListObj[7] != null) {
            setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
                queryListObj[7].toString().trim(), false, cell, true);
            sheet.autoSizeColumn(9);
          } else {
            setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
                cell, true);
          }
          CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

          cell = row.createCell(8);
          if (queryListObj[8] != null) {
            setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
                queryListObj[8].toString().trim(), false, cell, true);
            totalAmt = totalAmt.add(new BigDecimal(queryListObj[8].toString()));

            sheet.autoSizeColumn(10);
          } else {
            setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "", false,
                cell, true);
          }
          CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

        }
      }

      row = sheet.createRow(rowNum++);

      cell = row.createCell(0);
      setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "Total", true,
          cell, true);
      sheet.addMergedRegion(CellRangeAddress.valueOf("A" + rowNum + ":F" + rowNum));
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_RIGHT);

      cell = row.createCell(1);
      setCellBolderTopAndBottom(workbook, boldFont, row, cell);

      cell = row.createCell(2);
      setCellBolderTopAndBottom(workbook, boldFont, row, cell);
      cell = row.createCell(3);
      setCellBolderTopAndBottom(workbook, boldFont, row, cell);

      cell = row.createCell(4);
      setCellBolderTopAndBottom(workbook, boldFont, row, cell);

      cell = row.createCell(5);
      setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "0", true, cell,
          true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      if (isPackingReport) {
        /*
         * cell = row.createCell(6); setCellvalueWithAlignment(false, true, true, true, workbook,
         * boldFont, row, "0", true, cell, true); CellUtil.setAlignment(cell, workbook,
         * CellStyle.ALIGN_CENTER);
         */
        TotalRowCount = rowNum - 1;
        cell = row.createCell(6);
        if (isPackingReport) {
          strFormula = "SUM(G19:G" + TotalRowCount + ")";
          cell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
          cell.setCellFormula(strFormula);
        }
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
        setCellvalueWithAlignment(true, true, true, true, workbook, boldFont, row, "0", true, cell,
            true);

        cell = row.createCell(7);
        /*
         * setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "0", true,
         * cell, true);
         * 
         * CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
         */
        if (isPackingReport) {
          strFormula = "SUM(H19:H" + TotalRowCount + ")";
          cell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
          cell.setCellFormula(strFormula);
        }
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
        setCellvalueWithAlignment(true, true, true, true, workbook, boldFont, row, "0", true, cell,
            true);

        cell = row.createCell(8);
        setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
            String.valueOf(totalQty), true, cell, true);
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

        cell = row.createCell(9);
        setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
            String.valueOf(boxList.size()), true, cell, true);
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      } else {
        cell = row.createCell(6);
        setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
            String.valueOf(totalQty), true, cell, true);
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

        cell = row.createCell(7);
        setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "", true, cell,
            true);

        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

        cell = row.createCell(8);
        setCellvalueWithAlignment(true, true, true, true, workbook, boldFont, row,
            String.valueOf(totalAmt), true, cell, true);
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      }
      row = sheet.createRow(rowNum++);

      cell = row.createCell(0);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "LUT ARN No",
          true, cell, false);
      // setCellBolder(workbook, boldFont, row, 1);
      cell = row.createCell(2);
      setCellvalueWithAlignment(false, false, true, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(3);
      setCellvalueWithAlignment(false, false, true, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(4);
      setCellvalueWithAlignment(false, false, true, false, workbook, boldFont, row, "", true, cell,
          false);
      cell = row.createCell(5);
      setCellvalueWithAlignment(false, false, true, false, workbook, boldFont, row, "", true, cell,
          false);

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
      if (isPackingReport) {
        sheet.addMergedRegion(CellRangeAddress.valueOf("C" + rowNum + ":F" + nextFour + ""));
      } else {
        sheet.addMergedRegion(CellRangeAddress.valueOf("C" + rowNum + ":E" + nextFour + ""));

      }
      CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,

      CellStyle.VERTICAL_TOP);

      if (isPackingReport) {
        cell = row.createCell(6);
        cell.setCellValue("Signed by");
        cell.setCellStyle(getAlignStyle("Signed by", true, workbook, boldFont, true, true));
        CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,
            CellStyle.VERTICAL_TOP);
        sheet.addMergedRegion(CellRangeAddress.valueOf("G" + rowNum + ":J" + nextFour + ""));
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
        for (int i = 7; i <= MaxRowCount; i++) {
          setCellBolderTop(workbook, boldFont, row, i);
        }
      } else {
        cell = row.createCell(5);
        cell.setCellValue("Signed by");
        cell.setCellStyle(getAlignStyle("Signed by", true, workbook, boldFont, true, true));
        CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,
            CellStyle.VERTICAL_TOP);
        sheet.addMergedRegion(CellRangeAddress.valueOf("F" + rowNum + ":I" + nextFour + ""));
        CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

        for (int i = 6; i <= MaxRowCount; i++) {
          setCellBolderTop(workbook, boldFont, row, i);
        }
      }

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);
      row = sheet.createRow(rowNum++);
      cell = row.createCell(0);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
          "Total Net Weight (KG)", true, cell, false);
      cell = row.createCell(1);
      /*
       * setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true,
       * cell, null);
       */
      if (isPackingReport) {
        strFormula = "SUM(G19:G" + TotalRowCount + ")";
        cell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
        cell.setCellFormula(strFormula);
      }
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true, cell,
          null);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      if (isPackingReport) {
        cell = row.createCell(5);
        setCellBolderright(workbook, boldFont, row, cell);
      } else {
        cell = row.createCell(4);
        setCellBolderright(workbook, boldFont, row, cell);
      }

      cell = row.createCell(MaxRowCount);
      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(rowNum++);
      cell = row.createCell(0);

      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
          "Total Gross Weight (KG)", true, cell, false);
      cell = row.createCell(1);

      if (isPackingReport) {
        strFormula = "SUM(H19:H" + TotalRowCount + ")";
        cell.setCellType(HSSFCell.CELL_TYPE_FORMULA);
        cell.setCellFormula(strFormula);
      }
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true, cell,
          null);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      if (isPackingReport) {
        cell = row.createCell(5);
        setCellBolderright(workbook, boldFont, row, cell);
      } else {
        cell = row.createCell(4);
        setCellBolderright(workbook, boldFont, row, cell);
      }

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(rowNum++);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
          "Total No. of Packages", true, row.createCell(0), false);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
          String.valueOf(boxList.size()), true, row.createCell(1), null);

      if (isPackingReport) {
        cell = row.createCell(5);
        setCellBolderright(workbook, boldFont, row, cell);
      } else {
        cell = row.createCell(4);
        setCellBolderright(workbook, boldFont, row, cell);
      }

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(rowNum++);

      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row,
          "Total No. of Pallets", true, row.createCell(0), false);
      setCellvalueWithAlignment(false, true, false, true, workbook, boldFont, row, "0", true,
          row.createCell(1), null);

      if (isPackingReport) {
        cell = row.createCell(5);
        setCellBolderright(workbook, boldFont, row, cell);
      } else {
        cell = row.createCell(4);
        setCellBolderright(workbook, boldFont, row, cell);
      }

      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(rowNum++);
      setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row, "Total Pieces",
          true, row.createCell(0), false);
      setCellvalueWithAlignment(false, true, true, true, workbook, boldFont, row,
          String.valueOf(totalQty), true, row.createCell(1), null);
      if (isPackingReport) {
        cell = row.createCell(5);
        setCellBolderright(workbook, boldFont, row, cell);
      } else {
        cell = row.createCell(4);
        setCellBolderright(workbook, boldFont, row, cell);
      }
      cell = row.createCell(MaxRowCount);

      setCellBolderleft(workbook, boldFont, row, cell);

      row = sheet.createRow(rowNum++);
      // setCellBolderleft(workbook, boldFont, row, 10);

      setCellBolderTop(workbook, boldFont, row, 2);
      setCellBolderTop(workbook, boldFont, row, 3);
      setCellBolderTop(workbook, boldFont, row, 4);
      setCellBolderTop(workbook, boldFont, row, 5);
      setCellBolderTop(workbook, boldFont, row, 6);
      setCellBolderTop(workbook, boldFont, row, 7);
      setCellBolderTop(workbook, boldFont, row, 8);
      if (isPackingReport) {
        setCellBolderTop(workbook, boldFont, row, 9);
      }
      row = sheet.createRow(13);

      cell = row.createCell(0);
      setCellvalueWithAlignment(true, false, false, false, workbook, boldFont, row, "", true, cell,
          false);

      cell = row.createCell(5);
      setCellvalueWithAlignment(false, false, false, false, workbook, boldFont, row,
          "Terms Of Payment", true, cell, false);
      if (isPackingReport) {
        cell = row.createCell(10);
      } else {
        cell = row.createCell(9);
      }
      setCellBolderleft(workbook, boldFont, row, cell);

      FILE_NAME = fileName + ".xls";

      return DownloadFile(workbook);
    } catch (Exception e) {
      throw new Exception("Error While Processing the Report for Srilanka and error is: " + e);
    }
  }

  public static void setProductDetailsHeader(HSSFWorkbook workbook, HSSFSheet sheet,
      HSSFFont boldFont, int rowCount, boolean isPackingReport) throws Exception {
    HSSFRow row;
    String[] cellData;
    try {
      if (isPackingReport) {
        cellData = new String[] { "Model code", "Item", "Nature of Product", "Size",
            "Criterion Code", "Country of Origin", "Net Weight", "Gross Weight", "Quantity",
            "Box Numbers" };
      } else {
        cellData = new String[] { "Model code", "Item", "Nature of Product", "Size", "HSN Code",
            "Country of Origin", "Quantity", "Unit Price USD", "TOT Amount USD" };
      }
      int colNum = 0;
      row = sheet.createRow(rowCount);
      // setCellBolderRight(workbook, boldFont, row, 9);

      for (Object field : cellData) {

        final HSSFCell cell = row.createCell(colNum);
        row.setHeightInPoints((short) 2 * sheet.getDefaultRowHeightInPoints());
        HSSFCellStyle cellStyle = workbook.createCellStyle();
        // int

        cellStyle.setFont(boldFont);
        cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        cellStyle.setAlignment(cellStyle.ALIGN_CENTER);
        cellStyle.setVerticalAlignment(CellStyle.ALIGN_FILL);
        cellStyle.setWrapText(true);
        // cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
        cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
        if (field.equals("Box Numbers") || field.equals("TOT Amount USD")) {
          cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
        }
        cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
        cell.setCellStyle(cellStyle);
        cell.setCellValue((String) field);
        sheet.autoSizeColumn(colNum);
        colNum++;
      }
    } catch (Exception e) {
      throw new Exception(
          "Error While Getting the product Detail's for Srilanka report and error is: " + e);
    }
  }

  private static String DownloadFile(HSSFWorkbook workbook) throws Exception {
    try {
      String attachpath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");

      String FILeName = attachpath.trim() + "/" + FILE_NAME;

      FileOutputStream outputStream = new FileOutputStream(FILeName);
      workbook.write(outputStream);
      String linkdocument = getDownloadUrl(FILE_NAME);

      return linkdocument;
    } catch (Exception e) {
      throw new Exception("Error While Downloading file for Srilanka report and error is: " + e);
    }
  }

  private static String getDownloadUrl(String fullFileName) throws Exception {
    try {
      String contextName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("context.name");
      String linkdocument = "/" + contextName
          + "/org.openbravo.warehouse.shipping.print/DownloadExcelFile.html?param=" + fullFileName;
      // // log.info("The final URL to download the Export Sheet: " + linkdocument);

      return "<a href='" + linkdocument + "'target='_blank' >here</a>";
    } catch (Exception e) {
      throw new Exception("Error While Downloading URL for Srilanka report and error is: " + e);
    }
  }

  public static List<Object[]> getInvoiceDetails(OBWSHIPShipping shippingObj) throws Exception {

    try {
      String sql = "select distinct mp.em_cl_modelcode as ModelCode,  "
          + " mp.name as ProductName,  "
          + " cln.name as natualofproduct,"
          + " mp.em_cl_size as size,"
          + " COALESCE(ml.em_obwship_hsncode,gst.value) as ProductHSNCode, "
          + " ml.movementqty as ShipmentQty, "
          + "  mi.documentno ,"
          + " ml.em_obwship_cessionprice as unitPrice, "
          + " ml.em_obwship_taxableamount as taxableAmt "

          + " from obwship_shipping os "
          + " left join obwship_shipping_details osd on os.obwship_shipping_id = osd.obwship_shipping_id  "
          + " left join m_inout mi on mi.m_inout_id = osd.m_inout_id  "
          + " left join m_inoutline ml on ml.m_inout_id = mi.m_inout_id "
          + " left join m_product mp on mp.m_product_id = ml.m_product_id "
          + " left join INGST_GSTProductCode gst on gst.INGST_GSTProductCode_id = mp.EM_Ingst_Gstproductcode_ID "
          + " left join m_productprice mpp on mpp.m_product_id = mp.m_product_id   "
          + " left join cl_model cl on cl.cl_model_id=mp.em_cl_model_id "
          + " left join cl_natureofproduct cln on cln.cl_natureofproduct_id=cl.cl_natureofproduct_id "
          + " where   os.obwship_shipping_id= '"
          + shippingObj.getId()
          + "' "
          + " group by   mp.em_cl_modelcode ,  mp.name , "
          + "  mp.em_cl_size ,"
          + " cln.name ,"
          + " COALESCE(ml.em_obwship_hsncode,gst.value) , "
          + " ml.movementqty ,  mi.documentno , ml.em_obwship_taxableamount,ml.em_obwship_cessionprice "
          + " order by  mi.documentno,mp.name ";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      @SuppressWarnings("unchecked")
      List<Object[]> queryList = query.list();
      return queryList;
    } catch (Exception e) {
      throw new Exception(
          "Error While Getting the Invoice Detail for Srilanka report and error is: " + e);
    }
  }

  public static void setCellvalueWithAlignment(Boolean withLeftBorder, Boolean withBorder,
      Boolean withBottomBorder, Boolean isRightBorder, HSSFWorkbook workbook, HSSFFont boldFont,
      Row row, String message, Boolean isBold, Cell cell, Boolean isCenterAllignment)
      throws Exception {

    try {
      if (message != null) {
        if (message.matches("[0-9]+")) {
          cell.setCellValue(Integer.parseInt(message));

        } else {
          cell.setCellValue(message);
        }
        // cell.setCellValue(message);
      } else {
        cell.setCellValue("");

      }
      cellStyle = workbook.createCellStyle();
      if (message.equals("Final Delivery Address")
          || message.equalsIgnoreCase("Total Net Weight (KG)")
          || message.equalsIgnoreCase("Total Gross Weight (KG)")
          || message.equalsIgnoreCase("Total No. of Packages")) {
        row.setHeightInPoints((short) 2 * sheet.getDefaultRowHeightInPoints());
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
    } catch (Exception e) {
      throw new Exception(
          "Error While setting the  Cell value With Alignment for Srilanka report and error is: "
              + e);
    }
  }

  public static void setCellBolder(HSSFWorkbook workbook, HSSFFont boldFont, Row row, int cellCount)
      throws Exception {
    try {
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
    } catch (Exception e) {
      throw new Exception("Error While setting the  Bolder for Srilanka report and error is: " + e);
    }
  }

  public static void setCellBolderTop(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      int cellCount) throws Exception {
    try {
      Cell cell = row.createCell(cellCount);
      HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();
      centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      cell.setCellStyle(centerAlignStyleWithBold);
    } catch (Exception e) {
      throw new Exception("Error While setting the  top Bolder for Srilanka report and error is: "
          + e);
    }
  }

  public static void setCellBolderTopAndBottom(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      Cell cell) throws Exception {
    try {

      HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

      centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      centerAlignStyleWithBold.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);

      cell.setCellStyle(centerAlignStyleWithBold);
    } catch (Exception e) {
      throw new Exception("Error While setting the left Bolder for Srilanka report and error is: "
          + e);
    }
  }

  public static void setCellBolderTopAndLeft(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      Cell cell) throws Exception {
    try {

      HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

      centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      centerAlignStyleWithBold.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

      cell.setCellStyle(centerAlignStyleWithBold);
    } catch (Exception e) {
      throw new Exception("Error While setting the left Bolder for Srilanka report and error is: "
          + e);
    }
  }

  public static void setCellBolderleft(HSSFWorkbook workbook, HSSFFont boldFont, Row row, Cell cell)
      throws Exception {
    try {

      HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

      centerAlignStyleWithBold.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

      cell.setCellStyle(centerAlignStyleWithBold);
    } catch (Exception e) {
      throw new Exception("Error While setting the left Bolder for Srilanka report and error is: "
          + e);
    }
  }

  public static void setCellBolderright(HSSFWorkbook workbook, HSSFFont boldFont, Row row, Cell cell)
      throws Exception {
    try {

      HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

      centerAlignStyleWithBold.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);

      cell.setCellStyle(centerAlignStyleWithBold);
    } catch (Exception e) {
      throw new Exception("Error While setting the left Bolder for Srilanka report and error is: "
          + e);
    }
  }

  public static HSSFCellStyle getAlignStyle(String Text, Boolean isRightBorder,
      HSSFWorkbook workbook, HSSFFont boldFont, Boolean isBoldText, Boolean isCenterAllignment)
      throws Exception {
    try {
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
      if (Text.equalsIgnoreCase("Signed by") || Text.equalsIgnoreCase("Commercial invoice")
          || Text.equalsIgnoreCase("Packing List") || Text.equalsIgnoreCase("Shipping Remarks:")) {
        cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
        cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
        cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
        cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
      } else {
        // cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
        // cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
        cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
        cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
      }
      if (isRightBorder) {

        // cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
      }
      return cellStyle;
    } catch (Exception e) {
      throw new Exception("Error While getting Align Style for Srilanka report and error is: " + e);
    }
  }
}
