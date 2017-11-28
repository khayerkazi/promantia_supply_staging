package in.decathlon.scanner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;

public class ProcessCarStock {
  Logger log4j = Logger.getLogger(getClass());

  private class Receipt {
    String resvId;
    String itemCode;
    String qty;
    String boxNo;
    String bin;
  }

  // List of reservations received. This is maintained to delete these
  // reservations at the end of the receipt.
  List<Reservation> rList;

  public void ProcessCarReceipt(JSONObject json) {

    try {
     // Get the individual JSON objects corresponding to storage detail
      log4j.debug("PUTCAR Input Received: " + json.toString());
      // Create a movement header against a shipment
     JSONArray stock = json.getJSONArray("Stock");

      boolean first = true;
      rList = new ArrayList<Reservation>();
      // Create movement lines for each transaction in the shipment
      InternalMovement mvmt = null;
      for (int i = 0; i < stock.length(); i++) {
        JSONObject obj;
        obj = stock.getJSONObject(i);
        // Process only records which have a destination Bin
        String bin = obj.getString("Bin");
        if (bin == null || bin == "") {
          continue;
        }
        mvmt = processReceipt(mvmt, obj, first);
        first = false;
      }
      OBDal.getInstance().save(mvmt);
      // Now process the movement
      ScannerUtilities.processMovement(mvmt.getId());

      // Now delete the corresponding reservations
      deleteReservation();
    } catch (JSONException e) {
      throw new OBException("ProcessCarReceipt: Caught exception "
          + e.getMessage());
    }
  }

  private InternalMovement processReceipt(InternalMovement mvmt,
      JSONObject obj, boolean first) {
    Receipt rp = getReceiptDetails(obj);

    ReservationStock resv = OBDal.getInstance().get(ReservationStock.class, rp.resvId);
    Product prod = ScannerUtilities.getProduct(rp.itemCode);

    if (first) {
      // Create movement for the first record
      mvmt = ScannerUtilities.createMovement(resv.getOrganization(),
          resv.getClient());
      mvmt.setSWMovementtypegm("IWM");
      mvmt.setName(resv.getReservation().getDscShipmentname());
    }

    // Reactivate the reservations for this shipment. Only then, we can process
    // the movements
    reactivateReservation(resv, rp);

    Locator itrBin = resv.getStorageBin();
    Locator toBin = ScannerUtilities.getLocatorFromBarcode(rp.bin);

    // Validate the receipt against reservation

    // Create movement for the stock received

    // TODO WORKAROUND:
    // Bug in openbravo where attribute is assocated with From
    // bin.
    // Hence, a workaround by reversing the direction of movement and using
    // negative qty
    /*
     * InternalMovementLine mline = ScannerUtilities.createMovementLine(
     * resv.getOrganization(), resv.getClient(), prod,
     * resv.getAttributeSetValue(), mvmt, itrBin, toBin, new
     * BigDecimal(rp.qty));
     */

    InternalMovementLine mline = ScannerUtilities.createMovementLine(
        resv.getOrganization(), resv.getClient(), prod,
        resv.getAttributeSetValue(), mvmt, toBin, itrBin, (new BigDecimal(
            rp.qty)).negate());

    List<InternalMovementLine> mLineList = mvmt
        .getMaterialMgmtInternalMovementLineList();
    if (mLineList == null) {
      mLineList = new ArrayList<InternalMovementLine>();
    }
    mLineList.add(mline);
    mvmt.setMaterialMgmtInternalMovementLineList(mLineList);

    return mvmt;
  }

  private void reactivateReservation(ReservationStock rstock, Receipt rp) {
    // Validate the reservation
    Reservation resv = rstock.getReservation();
    if (!resv.getProduct().getName().equals(rp.itemCode)) {
      throw new OBException("The product has changed after reservation");
    }
    if (rstock.getQuantity().compareTo(new BigDecimal(rp.qty)) != 0) {
      throw new OBException("The quantity has changed after reservation");
    }
    OBContext.setAdminMode();
    if (!rstock.getAttributeSetValue().getLotName().equals(rp.boxNo)) {
      throw new OBException("The box no has changed after reservation");
    }
    OBContext.restorePreviousMode();
    resv.setRESStatus("DR");
    if (!rList.contains(resv)) {
      rList.add(resv);
    }
    OBDal.getInstance().save(resv);
  }

  private Receipt getReceiptDetails(JSONObject obj) {
    Receipt rp = new Receipt();

    try {
      rp.resvId = (String) obj.getString("ResvID");
      rp.itemCode = obj.getString("ItemCode");
      rp.qty = obj.getString("Qty");
      rp.boxNo = obj.getString("BoxNo");
      rp.bin = obj.getString("Bin");

    } catch (Exception e) {
      throw new OBException("getReceiptDetails: Caught exception "
          + e.getMessage());
    }

    log4j.debug("Receipt Detail: " + rp);
    return rp;
  }

  private void deleteReservation() {
    for (Reservation resv : rList) {
      OBDal.getInstance().remove(resv);
    }
    OBDal.getInstance().flush();
  }

}
