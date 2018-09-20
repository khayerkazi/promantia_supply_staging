package org.openbravo.warehouse.shipping.adreports;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;
import org.openbravo.warehouse.shipping.OBWSHIPShippingDetails;

public class ShippingReport extends BaseProcessActionHandler {

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

          String fileName = "ShippingSalesReport_For-" + shippingInvoiceNo + "_on_"
              + dateFormat.format(date);
          String linkdocument = PackingListReport.extractForShipping(shippingObj, fileName,
              shippingInvoiceNo, false);

          JSONObject msgTotal = new JSONObject();
          msgTotal.put("msgType", "info");
          msgTotal.put("msgTitle", "Shipping Report Generated!!" + " Click " + linkdocument
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
      OBContext.restorePreviousMode();

    }

    return jsonRequest;

  }
}