package in.decathlon.scanner;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.jfree.util.Log;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;

public class ProcessCacStock {

  Logger log4j = Logger.getLogger(getClass());

  private class StockDetails {
    String sdId;
    String itemCode;
    String qty;
    String pcb;
    String logRec;
    ArrayList<BoxDetails> boxDetails;
  }

  private class BoxDetails {
    String boxNo;
    String bQty;
  }

  public boolean ProcessCacStockForShipment(JSONObject json, String returnWarehouse,
      String fromWarehouse) throws JSONException {

    boolean result = true;

    // Get the individual JSON objects corresponding to storage detail
    String shipName = getNextShipmentName(fromWarehouse);
    log4j.debug("Shipment Name: " + shipName);
    JSONArray stock = json.getJSONArray("Stock");
    // create 2 movements - one for return to from another for from to return
    for (int i = 0; i < stock.length(); i++) {
      JSONObject obj = stock.getJSONObject(i);

      StockDetails sd = getStockDetails(obj);

      Log.debug("Creating txns for product " + sd.itemCode + " Record No " + i);

      createReqdMovements(sd, returnWarehouse);// should only create lines - send two movements as a
                                               // parameter
      updateStockReservation(sd, shipName);
      updateProductDetails(sd);
    }
    // if movement line size = 0 then delete , else save
    return result;
  }

  private StockDetails getStockDetails(JSONObject obj) {
    StockDetails sd = new StockDetails();

    try {
      sd.sdId = (String) obj.getString("SDID");
      sd.itemCode = obj.getString("ItemCode");
      sd.qty = obj.getString("Qty");
      sd.pcb = obj.getString("PCB");
      if (obj.has("LogRec")) {
        sd.logRec = obj.getString("LogRec");

      } else {
        sd.logRec = "";
      }

      if (obj.has("Box")) {
        JSONArray boxDetails = obj.getJSONArray("Box");

        ArrayList<BoxDetails> bdList = null;
        if (boxDetails != null && boxDetails.length() > 0) {
          bdList = new ArrayList<ProcessCacStock.BoxDetails>();
        }
        for (int i = 0; i < boxDetails.length(); i++) {
          BoxDetails bd = new BoxDetails();
          JSONObject box = boxDetails.getJSONObject(i);
          bd.boxNo = box.getString("BoxNo");
          bd.bQty = box.getString("BQty");
          bdList.add(bd);
        }
        sd.boxDetails = bdList;
      } else {
        sd.boxDetails = null;
      }
    } catch (Exception e) {
      throw new OBException("Caught exception while reading the JSON data. Exception: "
          + e.getMessage());
    }

    log4j.debug("Stock Detail: " + sd.toString());

    return sd;
  }

  private void updateProductDetails(StockDetails sd) {
    Product prod = ScannerUtilities.getProduct(sd.itemCode);
    if (prod.getClPcbQty().compareTo(new BigDecimal(sd.pcb)) != 0) {
      prod.setClPcbQty(new BigDecimal(sd.pcb));
    }
    if (!sd.logRec.equals("")) {
      OBContext.setAdminMode();
      String logRecSkey = getLogisticsRechargeSKey(sd.logRec);
      OBContext.restorePreviousMode();
      if (!prod.getClLogRec().equals(logRecSkey)) {
        prod.setClLogRec(logRecSkey);
      }
    }
    OBDal.getInstance().save(prod);
  }

  private void updateStockReservation(StockDetails sd, String shipName) {
    log4j.debug("Product name " + sd.itemCode);
    Product prod = ScannerUtilities.getProduct(sd.itemCode);

    // Check if box details are present. Else, exit as there is no need
    // to create reservations
    if (sd.boxDetails == null) {
      // DO nothing
      log4j.debug("No reservation for SD" + sd.sdId);
      return;
    }
    // Create a reservation for each storage detail record
    Reservation resv = OBProvider.getInstance().get(Reservation.class);
    resv.setActive(true);
    resv.setProduct(prod);

    StorageDetail storDet = getStorageDetail(sd);
    String warehouseId = storDet.getStorageBin().getWarehouse().getId();
    Warehouse wh = OBDal.getInstance().get(Warehouse.class, warehouseId);
    resv.setWarehouse(wh);
    log4j.debug("the warehouse:" + resv.getWarehouse());
    resv.setStorageBin(storDet.getStorageBin());
    resv.setClient(storDet.getClient());
    resv.setOrganization(storDet.getOrganization());
    resv.setUOM(prod.getUOM());
    resv.setDscShipmentname(shipName);

    BigDecimal totalResvdQty = new BigDecimal(0);
    ArrayList<BoxDetails> bd = sd.boxDetails;
    List<ReservationStock> stockList = new ArrayList<ReservationStock>();
    for (BoxDetails box : bd) {
      // Create a stock line under Reservation
      ReservationStock stock = OBProvider.getInstance().get(ReservationStock.class);
      stock.setActive(true);
      stock.setQuantity(new BigDecimal(box.bQty));
      // Create an attribute set instance
      OBContext.setAdminMode();
      AttributeSetInstance attr = ScannerUtilities.createAttributeSetInstance(prod, box.boxNo);
      OBContext.restorePreviousMode();
      stock.setAttributeSetValue(attr);
      stock.setReservation(resv);
      stock.setStorageBin(storDet.getStorageBin());
      stock.setOrganization(storDet.getOrganization());
      stock.setClient(storDet.getClient());
      stockList.add(stock);
      totalResvdQty = totalResvdQty.add(new BigDecimal(box.bQty));
    }
    resv.setQuantity(totalResvdQty);
    resv.setMaterialMgmtReservationStockList(stockList);
    resv.setRESStatus("CO");
    resv.setRESProcess("CL");
    OBDal.getInstance().save(resv);
    log4j.debug("Created reservation for SD " + sd.sdId);
  }

  private String getNextShipmentName(String fromWarehouse) {

    Warehouse shipmentWarehouse = ScannerUtilities.getWarehouse(fromWarehouse);

    OBCriteria<Reservation> resvCrit = OBDal.getInstance().createCriteria(Reservation.class);
    resvCrit.add(Restrictions.eq(Reservation.PROPERTY_WAREHOUSE, shipmentWarehouse));

    Long maxShipNo = (long) 0;
    for (Reservation resv : resvCrit.list()) {
      // Get the shipment name, extract the shipment no from the name
      // Shipment Name format: "Shipment n dd-mm-yyyy"
      if (resv.getDscShipmentname() != null) {
        String[] shipName = resv.getDscShipmentname().split(":");
        Long shipNo = Long.parseLong(shipName[1]);
        if (shipNo > maxShipNo) {
          maxShipNo = shipNo;
        }
      }
    }
    // Now form the shipment name
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    String date = dateFormat.format(new Date());
    String nextShipName = new String("Shipment:" + (maxShipNo + 1) + ":" + date);
    return nextShipName;
  }

  private StorageDetail getStorageDetail(StockDetails sd) {
    StorageDetail storageDetail = OBDal.getInstance().get(StorageDetail.class, sd.sdId);
    log4j.debug("Storage Detail: " + storageDetail);
    return storageDetail;
  }

  private String getLogisticsRechargeSKey(String logRec) {
    if (logRec == null) {
      return null;
    }
    OBCriteria<org.openbravo.model.ad.domain.List> adList = OBDal.getInstance().createCriteria(
        org.openbravo.model.ad.domain.List.class);
    adList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_NAME, logRec));
    adList.createAlias(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, "ref").add(
        Restrictions.eq("ref.name", "EM_Cl_Logistic_Recharge"));
    if (adList.list().size() == 0) {
      throw new OBException("Reference List " + logRec + " not found");
    }
    return adList.list().get(0).getSearchKey();
  }

  private void createReqdMovements(StockDetails sd, String returnWarehouse) {
    Locator cacBin, itrBin;
    StorageDetail storeDet = getStorageDetail(sd);
    BigDecimal resvdQty = ScannerUtilities.getTotalResvdQty(storeDet);
    BigDecimal initialQty = storeDet.getQuantityOnHand().subtract(resvdQty);
    BigDecimal finalQty = new BigDecimal(sd.qty);
    BigDecimal diffqty = finalQty.subtract(initialQty).abs();
    itrBin = storeDet.getStorageBin();
    cacBin = ScannerUtilities.getWarehouse(returnWarehouse).getLocatorList().get(0);

    InternalMovement mvmt = null;
    mvmt = ScannerUtilities.createMovement(storeDet.getOrganization(), storeDet.getClient());
    List<InternalMovementLine> mlineList = mvmt.getMaterialMgmtInternalMovementLineList();

    if (finalQty.compareTo(initialQty) > 0) {
      // Create movement from CAC bin to ITR bin
      log4j.debug("Create movement from CAC TO ITR");
      InternalMovementLine mline = ScannerUtilities.createMovementLine(storeDet.getOrganization(),
          storeDet.getClient(), storeDet.getProduct(), null, mvmt, cacBin, itrBin, diffqty);
      mlineList.add(mline);
    } else if (finalQty.compareTo(initialQty) < 0) {
      // Create movement from ITR bin to CAC bin
      log4j.debug("Create movement from ITR TO CAC");

      InternalMovementLine mline = ScannerUtilities.createMovementLine(storeDet.getOrganization(),
          storeDet.getClient(), storeDet.getProduct(), null, mvmt, itrBin, cacBin, diffqty);
      mlineList.add(mline);
    } else {
      return;
    }
    mvmt.setName("Stock Correction");
    mvmt.setSWMovementtypegm("IWM");
    OBDal.getInstance().save(mvmt);
    ScannerUtilities.processMovement(mvmt.getId());
  }
}
