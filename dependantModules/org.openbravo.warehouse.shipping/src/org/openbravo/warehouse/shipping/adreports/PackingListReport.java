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
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.PrintSetup;
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
import org.openbravo.warehouse.shipping.OBWSHIPShipping;

public class PackingListReport extends BaseProcessActionHandler {

  private static String FILE_NAME = ""; // GSTIN_TAXPERIOD_DateTime.xls
  static SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);

    try {
      jsonRequest = new JSONObject(content);
      String strShippingId = jsonRequest.getString("Obwship_Shipping_ID");

      OBWSHIPShipping shippingObj = OBDal.getInstance().get(OBWSHIPShipping.class, strShippingId);
      /*
       * String packingInvoiceNo = ""; if (shippingObj.getPackinginvoiceno() != null) {
       * packingInvoiceNo = shippingObj.getPackinginvoiceno(); } else {
       */
      String shippingInvoiceNo = "";
      if (shippingObj.getGsUniqueno() != null)
        shippingInvoiceNo = shippingObj.getGsUniqueno();

      /* } */
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Date date = new Date();

      if (shippingObj != null) {
        if (parameters.containsKey("_action")) {

          String fileName = "PackingSalesReport_For-" + shippingInvoiceNo + "_on_"
              + dateFormat.format(date);
          String linkdocument = PackingListReport.extractForShipping(shippingObj, fileName,
              shippingInvoiceNo);
          JSONObject msgTotal = new JSONObject();
          msgTotal.put("msgType", "info");
          msgTotal
              .put("msgTitle", "Report Generated!!" + " Click here to download " + linkdocument);
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

    }

    return jsonRequest;

  }

  @SuppressWarnings("hiding")
  public static String extractForShipping(OBWSHIPShipping shippingObj, String fileName,
      String shippingInvoiceNo) throws FileNotFoundException, IOException, JSONException,
      ParseException {

    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet("Style example");

    PrintSetup ps = sheet.getPrintSetup();

    sheet.setAutobreaks(true);

    ps.setFitHeight((short) 1);
    ps.setFitWidth((short) 1);

    HSSFFont boldFont = workbook.createFont();
    boldFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
    boldFont.setFontName(HSSFFont.FONT_ARIAL);
    workbook.setSheetName(0, "Packing Report");

    Row row = null;
    Cell cell = null;

    setProductDetailsHeader(workbook, sheet, boldFont, 16);

    for (int rowCount = 0; rowCount <= 15; rowCount++) {
      row = sheet.createRow(rowCount);
      for (int cellCount = 0; cellCount <= 9; cellCount++) {
        cell = row.createCell(cellCount);

        if (rowCount == 15) {
          if (cellCount == 0) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Shipped by", true, cell,
                false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 2) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Equipment NB", true, cell,
                false);
            sheet.autoSizeColumn(cellCount);
            continue;

          } else if (cellCount == 6) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Notify Party", true, cell,
                false);
            sheet.autoSizeColumn(cellCount);
            continue;

          } else if (cellCount == 7) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Same as Consignee", false,
                cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;

          }/*
            * else { setCellBolder(workbook, boldFont, row, cellCount); continue;
            * 
            * }
            */

        }
        if (rowCount == 1) {

          if (cellCount == 0) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Invoice No", true, cell,
                false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 1) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, shippingInvoiceNo, false,
                cell, false);
            sheet.autoSizeColumn(cellCount);

            continue;
          } else if (cellCount == 6) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Shipper", true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 7) {

            setCellvalueWithAlignment(false, workbook, boldFont, row,
                "Decathlon Sports India Pvt Ltd", false, cell, false);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 14) {

          if (cellCount == 0) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Place Of Discharge", true,
                cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;

          } else if (cellCount == 1) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Sri Lanka", false, cell,
                false);
            continue;
          } else if (cellCount == 2) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "In.transport Ref.", true,
                cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else

        if (rowCount == 2) {

          if (cellCount == 7) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Survey No - 78/10", false,
                cell, false);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 3) {

          if (cellCount == 7) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "A2 - Chikkajala Village",
                false, cell, false);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 4) {

          if (cellCount == 0) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Date", true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 1) {

            setCellvalueWithAlignment(false, workbook, boldFont, row,
                formatter.format(shippingObj.getShipmentDate()), false, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 7) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "562157 Bangalore", false,
                cell, false);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 5) {

          if (cellCount == 7) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "India", false, cell, false);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else

        if (rowCount == 6) {

          if (cellCount == 0) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Final Delivery Address",
                true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 1) {

            setCellvalueWithAlignment(false, workbook, boldFont, row,
                "Decathlon Lanka Sport Access (Pvt) Ltd.", false, cell, false);
            continue;
          } else if (cellCount == 6) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Consignee", true, cell,
                false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 7) {

            setCellvalueWithAlignment(false, workbook, boldFont, row,
                "Decathlon Lanka Sport Access (Pvt) Ltd.", false, cell, false);
            continue;
          }/*
            * else { setCellBolder(workbook, boldFont, row, cellCount); }
            */
        } else if (rowCount == 7) {

          if (cellCount == 1) {
            setCellvalueWithAlignment(false, workbook, boldFont, row,
                "No. 249, Stanley Thilakarathne Mawatha,", false, cell, false);
            continue;
          } else if (cellCount == 7) {

            setCellvalueWithAlignment(false, workbook, boldFont, row,
                "No. 249, Stanley Thilakarathne Mawatha,", false, cell, false);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 8) {

          if (cellCount == 1) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Nugegoda, Sri Lanka.",
                false, cell, false);
            continue;

          } else if (cellCount == 7) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Nugegoda, Sri Lanka.",
                false, cell, false);
            continue;

          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 9) {

          if (cellCount == 1) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "TEL:  +94 112818345", false,
                cell, false);
            continue;

          } else if (cellCount == 7) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "TEL:  +94 112818345", false,
                cell, false);
            continue;

          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 10) {

          if (cellCount == 1) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Mobile:  +94 77 9446832",
                false, cell, false);
            continue;
          } else if (cellCount == 7) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Mobile:  +94 77 9446832",
                false, cell, false);
            continue;

          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 11) {

          if (cellCount == 0) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "ETD", true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 2) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "ATD", true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 6) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Cost Center", true, cell,
                false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 12) {

          if (cellCount == 0) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "ATD", true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 2) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Place Of Loading", true,
                cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } else if (cellCount == 6) {

            setCellvalueWithAlignment(false, workbook, boldFont, row, "Incoterm", true, cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 13) {

          if (cellCount == 6) {
            setCellvalueWithAlignment(false, workbook, boldFont, row, "Terms Of Payment", true,
                cell, false);
            sheet.autoSizeColumn(cellCount);
            continue;
          } /*
             * else { setCellBolder(workbook, boldFont, row, cellCount); }
             */
        } else if (rowCount == 0) {

          if (cellCount == 0) {
            cell = row.createCell(0);
            cell.setCellValue("PACKING LIST");
            cell.setCellStyle(getAlignStyle(true, workbook, boldFont, true, true));
            sheet.addMergedRegion(CellRangeAddress.valueOf("A1:J1"));
            CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

          }
        }
      }
    }

    int rowNum = 17;
    Set<String> boxList = new HashSet<String>();
    int totalQty = 0;
    List<Object[]> queryList = getPackingReportProductDetailList(shippingObj);

    for (Object[] queryListObj : queryList) {

      int colNum1 = 0;
      row = sheet.createRow(rowNum++);
      cell = row.createCell(0);

      if (queryListObj[0] != null) {
        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[0].toString(), false,
            cell, true);
      } else
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, row.createCell(0), true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      cell = row.createCell(1);
      if (queryListObj[1] != null) {
        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[1].toString(), false,
            cell, true);
      } else
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, row.createCell(1), true);

      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      cell = row.createCell(2);
      if (queryListObj[2] != null) {
        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[2].toString(), false,
            cell, true);
      } else
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, row.createCell(2), true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      cell = row.createCell(3);
      if (queryListObj[3] != null) {
        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[3].toString(), false,
            cell, true);
        sheet.autoSizeColumn(3);

      } else
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, row.createCell(3), true);

      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      cell = row.createCell(4);
      if (queryListObj[4] != null) {
        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[4].toString(), false,
            cell, true);
      } else
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, cell, true);

      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      cell = row.createCell(5);
      setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, cell, true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(6);

      setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, cell, true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
      cell = row.createCell(7);
      setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, cell, true);
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(8);
      if (queryListObj[5] != null) {
        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[5].toString(), false,
            cell, true);
        totalQty = totalQty + Integer.parseInt(queryListObj[5].toString());
      } else {
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, cell, true);
      }
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

      cell = row.createCell(9);

      if (queryListObj[6] != null) {

        setCellvalueWithAlignment(true, workbook, boldFont, row, queryListObj[6].toString(), false,
            cell, true);
        boxList.add(queryListObj[6].toString());
        sheet.autoSizeColumn(9);

      } else {
        setCellvalueWithAlignment(true, workbook, boldFont, row, "", false, cell, true);
      }
      CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    }

    row = sheet.createRow(rowNum++);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total", true, cell, true);
    sheet.addMergedRegion(CellRangeAddress.valueOf("A" + rowNum + ":E" + rowNum));
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_RIGHT);

    cell = row.createCell(5);

    setCellvalueWithAlignment(true, workbook, boldFont, row, "", true, cell, true);

    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
    cell = row.createCell(6);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "0", true, cell, true);

    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
    cell = row.createCell(7);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "0", true, cell, true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
    cell = row.createCell(8);

    setCellvalueWithAlignment(true, workbook, boldFont, row, String.valueOf(totalQty), true, cell,
        true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);
    cell = row.createCell(9);

    setCellvalueWithAlignment(true, workbook, boldFont, row, String.valueOf(boxList.size()), true,
        cell, true);
    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    row = sheet.createRow(rowNum++);
    cell = row.createCell(0);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "LUT ARN No", true, cell, true);
    setCellBolder(workbook, boldFont, row, 1);

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total Volume (CBM)", true,
        row.createCell(0), true);
    cell = row.createCell(2);
    cell.setCellValue("Shipping Remarks:");
    cell.setCellStyle(getAlignStyle(true, workbook, boldFont, true, false));
    int nextFour = rowNum + 5;
    sheet.addMergedRegion(CellRangeAddress.valueOf("C" + rowNum + ":D" + nextFour + ""));
    CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,
        CellStyle.VERTICAL_TOP);

    cell = row.createCell(4);
    cell.setCellValue("Signed by");
    cell.setCellStyle(getAlignStyle(true, workbook, boldFont, true, true));
    CellUtil.setCellStyleProperty(cell, workbook, CellUtil.VERTICAL_ALIGNMENT,
        CellStyle.VERTICAL_TOP);
    sheet.addMergedRegion(CellRangeAddress.valueOf("E" + rowNum + ":J" + nextFour + ""));
    setCellBolderRight(workbook, boldFont, row, 9);

    CellUtil.setAlignment(cell, workbook, CellStyle.ALIGN_CENTER);

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total Net Weight (KG)", true,
        row.createCell(0), true);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "0", true, row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total Gross Weight (KG)", true,
        row.createCell(0), true);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "0", true, row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total No. of Packages", true,
        row.createCell(0), true);
    setCellvalueWithAlignment(true, workbook, boldFont, row, String.valueOf(boxList.size()), true,
        row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total No. of Pallets", true,
        row.createCell(0), true);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "0", true, row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellvalueWithAlignment(true, workbook, boldFont, row, "Total Pieces", true,
        row.createCell(0), true);
    setCellvalueWithAlignment(true, workbook, boldFont, row, String.valueOf(totalQty), true,
        row.createCell(1), null);

    row = sheet.createRow(rowNum++);
    setCellBolderTop(workbook, boldFont, row, 2);
    setCellBolderTop(workbook, boldFont, row, 3);
    setCellBolderTop(workbook, boldFont, row, 4);
    setCellBolderTop(workbook, boldFont, row, 5);
    setCellBolderTop(workbook, boldFont, row, 6);
    setCellBolderTop(workbook, boldFont, row, 7);
    setCellBolderTop(workbook, boldFont, row, 8);
    setCellBolderTop(workbook, boldFont, row, 9);

    FILE_NAME = fileName + ".xlsx";

    return DownloadFile(workbook);

  }

  public static void setProductDetailsHeader(HSSFWorkbook workbook, HSSFSheet sheet,
      HSSFFont boldFont, int rowCount) {
    Row row;
    Cell cell;
    String[] cellData = { "Model code", "Item", "Nature of Product", "Size", "Criterion Code",
        "Country of Origin", "Net Weight (KG)", "Gross Weight", "Quantity", "Box Numbers" };

    int colNum = 0;
    row = sheet.createRow(rowCount);
    for (Object field : cellData) {
      cell = row.createCell(colNum);
      cell.setCellValue((String) field);
      HSSFCellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setFont(boldFont);
      cellStyle.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
      cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
      cellStyle.setAlignment(cellStyle.ALIGN_CENTER);
      cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
      cell.setCellStyle(cellStyle);

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

  public static void setCellvalueWithAlignment(Boolean isWithBorder, HSSFWorkbook workbook,
      HSSFFont boldFont, Row row, String message, Boolean isBold, Cell cell,
      Boolean isCenterAllignment) {
    cell.setCellValue(message);

    cell.setCellStyle(getAlignStyle(isWithBorder, workbook, boldFont, isBold, isCenterAllignment));

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

  public static void setCellBolderTop(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      int cellCount) {

    Cell cell;
    cell = row.createCell(cellCount);
    HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

    centerAlignStyleWithBold.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);

    cell.setCellStyle(centerAlignStyleWithBold);

  }

  public static void setCellBolderRight(HSSFWorkbook workbook, HSSFFont boldFont, Row row,
      int cellCount) {

    Cell cell;
    cell = row.createCell(cellCount);
    HSSFCellStyle centerAlignStyleWithBold = workbook.createCellStyle();

    centerAlignStyleWithBold.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);

    cell.setCellStyle(centerAlignStyleWithBold);

  }

  public static HSSFCellStyle getAlignStyle(Boolean isWithBorder, HSSFWorkbook workbook,
      HSSFFont boldFont, Boolean isBoldText, Boolean isCenterAllignment) {
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

    if (isWithBorder) {
      cellStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
      cellStyle.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);
    }
    return cellStyle;
  }
}
