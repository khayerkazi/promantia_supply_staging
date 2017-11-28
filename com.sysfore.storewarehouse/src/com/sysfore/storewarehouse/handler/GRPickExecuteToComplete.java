package com.sysfore.storewarehouse.handler;

import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

/*
 * This class completes the multiple receipt 
 */
public class GRPickExecuteToComplete extends BaseProcessActionHandler {
  Logger logger = Logger.getLogger(this.getClass());
  EnhancedProcessGoods enhancedProcessGoods = new EnhancedProcessGoods();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = new JSONObject();
    ShipmentInOut receipt = null;
    OBError result = new OBError();
    System.out.println("inside pick and execute");
    String msg = "Process Completed Successfully";
    OBContext.setAdminMode();
    try {
      result.setType("info");
      jsonRequest = new JSONObject(content);
      logger.debug(jsonRequest);
      JSONArray selectedLines = jsonRequest.getJSONArray("_selection");

      if (selectedLines.length() == 0) {
        throw new OBException("Please select records to complete GRN");
      }
      ArrayList<ShipmentInOut> shipmentInOuts = new ArrayList<ShipmentInOut>();
      for (long i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject((int) i);
        final String receiptId = selectedLine.getString("id");
        receipt = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
        shipmentInOuts.add(receipt);
      }
      completeReceipts(shipmentInOuts);
      jsonRequest.put("retryExecution", true);
      JSONArray responseActions = new JSONArray();
      JSONObject action = new JSONObject();
      action.put("refreshTheGrid", new JSONObject());
      responseActions.put(action);
      jsonRequest.put("responseActions", responseActions);

      result.setType("Success");

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", msg);
      jsonRequest.put("message", errorMessage);
    } catch (Exception e) {
      try {
        logger.error(e.getMessage(), e);
        e.printStackTrace();
        result.setType("Error");
        result.setMessage(e.getMessage());
        OBDal.getInstance().rollbackAndClose();
        e.printStackTrace();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", e.getMessage());
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        logger.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
      try {
        jsonRequest.put("retryExecution", true);
      } catch (JSONException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    return jsonRequest;
  }

  /*
   * This method calls processReceipt method of EnhancedProcessGoods java class
   */
  private void completeReceipts(ArrayList<ShipmentInOut> shipmentInOuts) throws Exception {
    try {
      OBContext.setAdminMode();
      for (ShipmentInOut shipmentInOut : shipmentInOuts) {
        if (shipmentInOut.getDocumentStatus().endsWith("DR"))
          enhancedProcessGoods.processReceipt(shipmentInOut);
      }
    } catch (Exception e) {
      logger.error("Error", e);
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}