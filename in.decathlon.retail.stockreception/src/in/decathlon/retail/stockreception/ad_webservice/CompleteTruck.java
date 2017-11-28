package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.decathlon.warehouse.truckreception.DTRTruckReception;
import org.decathlon.warehouse.truckreception.DTR_TruckDetails;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import au.com.bytecode.opencsv.CSVWriter;

public class CompleteTruck implements WebService {
  static Logger log4j = Logger.getLogger(CompleteTruck.class);
  final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
  final static Properties p = SuppyDCUtil.getInstance().getProperties();
  List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    StringBuilder strBuilder = new StringBuilder();
    String strRead = "";
    String truckDetails;
    String truckNumber;
    String isEmail;
    Boolean truckStatus = false;
    try {
      InputStreamReader isReader = new InputStreamReader(request.getInputStream());
      BufferedReader bReader = new BufferedReader(isReader);

      strRead = bReader.readLine();
      while (strRead != null) {
        strBuilder.append(strRead);
        strRead = bReader.readLine();
        log4j.info("Parsing Success");
      }
      truckDetails = strBuilder.toString();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(truckDetails));
      Document document = builder.parse(is);

      truckNumber = document.getElementsByTagName("truckNumber").item(0).getChildNodes().item(0)
          .getNodeValue().toString();

      isEmail = document.getElementsByTagName("email").item(0).getChildNodes().item(0)
          .getNodeValue().toString();

      List<DTRTruckReception> truckObj = new ArrayList<DTRTruckReception>();
      OBCriteria<DTRTruckReception> truckCriteria = OBDal.getInstance().createCriteria(
          DTRTruckReception.class);
      truckCriteria.add(Restrictions.eq(DTRTruckReception.PROPERTY_DOCUMENTNO, truckNumber));
      truckCriteria.add(Restrictions.eq(DTRTruckReception.PROPERTY_DOCUMENTSTATUS, "DR"));
      truckObj = truckCriteria.list();
      Integer truckObjectSize = truckObj.size();

      if (truckObjectSize > 0) {
        String truckId = truckObj.get(0).getId();
        DTRTruckReception truckInfo = OBDal.getInstance().get(DTRTruckReception.class, truckId);
        HashMap<String, List<String>> map = processTruck(truckInfo, true);
        if (truckInfo.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus))
          truckStatus = true;
      }
      sendMail(truckStatus, truckObj, isEmail);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendMail(boolean ismail, List<DTRTruckReception> truckObj, String notify) {
    // TODO Auto-generated method stub
    List<GetTruckBean> list = new ArrayList<GetTruckBean>();
    String storename = truckObj.get(0).getOrganization().getName();
    list = getTruckDetails(truckObj.get(0).getDocumentNo());

    try {

      if (ismail == true && notify.equals("true")) {
        String filename = p.getProperty("ClosingcsvPath") + storename + "-" + dt.format(new Date())
            + ".csv";
        final File file = new File(filename);
        final CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile()));
        csvwrite(list, storename, file, writer);
        writer.close();
        TruckClosingMail.getInstance().sendmailtoall(storename, filename, truckObj);
      } else
        log4j.info("No mail - " + storename);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void csvwrite(List<GetTruckBean> truckObj, String storename, File file, CSVWriter writer)
      throws IOException {

    List<String[]> data = new ArrayList<String[]>();
    data.add(new String[] { "TruckId", "Box Number", "Itemcode", "BrandName", "Model Name",
        "MovementQty", "MovementDate", "BoxStatus" });

    if (!file.exists()) {
      file.createNewFile();
    }

    for (GetTruckBean truckInfo : truckObj) {

      String truckId = truckInfo.getTruckNo();
      String boxNo = truckInfo.getBoxNo();
      String itemCode = truckInfo.getItemCode();
      String brandName = truckInfo.getBrandName();
      String modelName = truckInfo.getModelName();
      String movementQty = truckInfo.getMovementQuantity();
      String movementDate = truckInfo.getMovementDate();
      String boxStatus = truckInfo.getBoxStatus();

      if (boxStatus.equals("CO")) {
        boxStatus = "Completed";
      } else if (boxStatus.equals("DR")) {
        boxStatus = "Draft";
      }

      data.add(new String[] { truckId, boxNo, itemCode, brandName, modelName, movementQty,
          movementDate, boxStatus });
    }

    writer.writeAll(data);
  }

  public List<GetTruckBean> getTruckDetails(String truckNo) {
    String qry = "select recep.documentno as truckNo,minout.documentno as boxNo,mp.name as itemCode, clm.name as modelName,clb.name as brandName,minoutline.movementQty,minout.movementDate,minout.docstatus as docStatus from m_inout minout INNER JOIN m_inoutline minoutline on minoutline.m_inout_id=minout.m_inout_id INNER JOIN dtr_truck_details receplines on receplines.m_inout_id=minout.m_inout_id join dtr_truck_reception recep on recep.dtr_truck_reception_id=receplines.dtr_truck_reception_id join m_product mp on mp.m_product_id=minoutline.m_product_id  join cl_model clm on clm.cl_model_id=mp.em_cl_model_id join cl_brand clb on clb.cl_brand_id=clm.cl_brand_id where recep.documentno='"
        + truckNo + "'";

    Statement stmt = null;
    ResultSet rs = null;
    List<GetTruckBean> list = new ArrayList<GetTruckBean>();
    try {
      stmt = OBDal.getInstance().getConnection().createStatement();
      rs = stmt.executeQuery(qry);

      while (rs.next()) {
        GetTruckBean obj = new GetTruckBean();
        String truckId = rs.getString("truckNo");
        obj.setTruckNo(truckId);
        obj.setBoxNo(rs.getString("boxNo"));
        obj.setItemCode(rs.getString("itemCode"));
        obj.setModelName(rs.getString("modelName"));
        obj.setBrandName(rs.getString("brandName"));
        obj.setMovementQuantity(rs.getString("movementQty"));
        obj.setMovementDate(rs.getString("movementDate"));
        obj.setBoxStatus(rs.getString("docStatus"));
        list.add(obj);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return list;
  }

  /**
   * Complete the Truck
   * 
   * @param truck
   * @param completeReceipts
   *          . If true, the Goods Receipts inside the Truck will be completed also
   * @return a HashMap with the description of the problems found if any
   */
  private HashMap<String, List<String>> processTruck(DTRTruckReception truck,
      boolean completeReceipts) {
    HashMap<String, List<String>> result = new HashMap<String, List<String>>();
    try {
      String truckDocNo = truck.getDocumentNo();
      if (truckDocNo.length() > 0) {
        JSONObject soDetails = new JSONObject();
        try {
          log4j.debug("Call to Web Service to close SO and complete Shipment");
          String wsName = "in.decathlon.ibud.shipment.CompleteShippingWS";
          soDetails = JSONWebServiceInvocationHelper.sendPostrequestToClose(wsName, "truckId="
              + truckDocNo, "{}");
          // JSONWebServiceInvocationHelper.sendPostrequest(wsName, "truckId=" + truckDocNo, "{}");
        } catch (Exception e) {
          e.printStackTrace();
          log4j.error("Error from Supply " + e);
          throw new OBException("Error from Supply " + e);
        }
        for (DTR_TruckDetails truckDetails : truck.getDTRTruckDetailsList()) {
          try {
            ShipmentInOut goodShipment = truckDetails.getGoodsShipment();
            if (goodShipment.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
              log4j.debug("GRN is already closed so not proceeding to CL the PO ");
            }
            BusinessEntityMapper.executeProcess(goodShipment.getId(), "104",
                "SELECT * FROM M_InOut_Post0(?)");
            OBDal.getInstance().refresh(goodShipment);
            goodShipment.setIbodtrCompletedTime(new Date());
            OBDal.getInstance().save(goodShipment);
            List<ShipmentInOutLine> grLineList = goodShipment
                .getMaterialMgmtShipmentInOutLineList();
            // Update custom movement type field that is set when M_Inout_Post is called

            List<MaterialTransaction> txnList = null;
            for (ShipmentInOutLine grnLine : grLineList) {
              txnList = grnLine.getMaterialMgmtMaterialTransactionList();
              for (MaterialTransaction txn : txnList) {
                txn.setSwMovementtype(goodShipment.getSWMovement());
                OBDal.getInstance().save(txn);
              }
            }
            EnhancedProcessGoods.updateRecQty(goodShipment, soDetails);
            System.out.println("TruckClosing:Processing" + goodShipment.getDocumentNo());
          } catch (Exception e) {
            e.printStackTrace();
            log4j.error("Error in retail " + e);

            // throw new OBException("Error in retail " + e);
          }

        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Error from Supply " + e);
      throw new OBException("Error from Supply " + e);
    }
    try {

      // If the Document is already complete, revert it to Draft Status
      if (isCompleted(truck)) {
        truck.setDocumentStatus("DR");
        OBDal.getInstance().save(truck);
        return result;
      }

      // Raise an error if the Shipping has no details
      if (hasNoDetails(truck)) {
        result.put("NoTruckDetails", null);
        return result;
      }

      if (completeReceipts) {
        for (DTR_TruckDetails truckDetails : truck.getDTRTruckDetailsList()) {
          if ("DR".equals(truckDetails.getGoodsShipment().getDocumentStatus())) {
            final List<Object> param = new ArrayList<Object>();
            param.add(null);
            param.add(truckDetails.getGoodsShipment().getId());
            CallStoredProcedure.getInstance().call("M_Inout_POST", param, null, true, false);
          }
        }
      }

      // If there has been no problems, complete the Document
      truck.setDocumentStatus("CO");
      OBDal.getInstance().save(truck);

    } catch (Exception e) {
      log4j.error("An error happened when processShipping was executed: " + e.getMessage(), e);
      result.put("ErrorWhileProcessing", null);
    }
    return result;
  }

  // Returns true if the Shipping is in Completed Status
  private boolean isCompleted(DTRTruckReception truck) {
    return truck.getDocumentStatus().equals("CO");
  }

  private boolean hasNoDetails(DTRTruckReception truck) {
    return truck.getDTRTruckDetailsList().size() == 0;
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
