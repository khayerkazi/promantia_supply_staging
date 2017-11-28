/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.packing;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.DbUtility;

public class ManagePickingBoxContentHandler extends BaseProcessActionHandler {
  final private static Logger log = Logger.getLogger(ManagePickingBoxContentHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject jsonParams = null;
    JSONObject response = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      jsonParams = jsonRequest.getJSONObject("_params");

      if (jsonRequest.getString("_buttonValue").equals(
          "OBWPACK_ManagePickingBoxContent_btnAddToBox")) {
        // confirm that we have
        // boxid
        // movlinetoadd
        // qtytoadd
        if (jsonParams.has("movLineToAdd") && !jsonParams.isNull("movLineToAdd")
            && !jsonParams.getString("movLineToAdd").equals("")
            && jsonRequest.has("inpobwpackBoxId") && !jsonRequest.isNull("inpobwpackBoxId")
            && !jsonRequest.getString("inpobwpackBoxId").equals("") && jsonParams.has("qtyToAdd")
            && !jsonParams.isNull("qtyToAdd") && jsonParams.getLong("qtyToAdd") > 0) {
          String movLineIdToAdd = jsonParams.getString("movLineToAdd");
          InternalMovementLine movementLine = OBDal.getInstance().get(InternalMovementLine.class,
              movLineIdToAdd);
          Long qtyInBox = jsonParams.getLong("qtyToAdd");
          String boxId = jsonRequest.getString("inpobwpackBoxId");
          String packinghId = OBDal.getInstance().get(PackingBox.class, boxId).getObwpackPackingh()
              .getId();
          BigDecimal qtyInBoxBD = new BigDecimal(qtyInBox);
          if (movementLine.getMovementQuantity().compareTo(qtyInBoxBD) >= 0
              && qtyInBoxBD.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal alreadyPackagedUnits = OBWPACK_Utils.getNumberOfUnitsPackagedOnOtherBoxes(
                boxId, movLineIdToAdd);
            BigDecimal totalIncludingCurrentTransaction = alreadyPackagedUnits.add(qtyInBoxBD);
            if (movementLine.getMovementQuantity().compareTo(totalIncludingCurrentTransaction) >= 0) {
              JSONObject boxIdJsonObj = new JSONObject();
              boxIdJsonObj.put(boxId, qtyInBox);
              OBWPACK_Utils.CreateOrUpdateBoxContent(movLineIdToAdd, packinghId, boxIdJsonObj);
              response = new JSONObject();
              JSONArray responseActions = new JSONArray();
              JSONObject myAction = new JSONObject();
              myAction.put("pickingBoxLineAdded", new JSONObject());
              responseActions.put(myAction);
              response.put("retryExecution", true);
              response.put("responseActions", responseActions);
            } else {
              response = new JSONObject();
              String[] arrParams = { movementLine.getMovementQuantity().toString(),
                  alreadyPackagedUnits.toString(), qtyInBoxBD.toString() };
              String message = OBMessageUtils.getI18NMessage(
                  "OBWPACK_qtyInOtherBoxesDoesntAllowThisQty", arrParams);
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", message);
              response.put("message", errorMessage);
              response.put("retryExecution", true);
            }
          } else {
            response = new JSONObject();
            String[] arrParams = { qtyInBoxBD.toString(),
                movementLine.getMovementQuantity().toString() };
            String message = OBMessageUtils.getI18NMessage(
                "OBWPACK_qtyToAddToBoxGreaterThanMovQty", arrParams);
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", message);
            response.put("message", errorMessage);
            response.put("retryExecution", true);
          }
        } else {
          response = new JSONObject();
          String message = OBMessageUtils.getI18NMessage("OBWPACK_movLineBoxFieldsRequired", null);
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", message);
          response.put("message", errorMessage);
          response.put("retryExecution", true);
        }
      } else if (jsonRequest.getString("_buttonValue").equals(
          "OBWPACK_ManagePickingBoxContent_btnDone")) {
        JSONArray gridLines = jsonParams.getJSONObject("boxContent").getJSONArray("_allRows");
        for (int j = 0; j < gridLines.length(); j++) {
          String movLineIdToAdd = gridLines.getJSONObject(j).getString("movementLine");
          Long qtyInBox = gridLines.getJSONObject(j).getLong("quantity");
          String boxId = jsonRequest.getString("inpobwpackBoxId");
          String packingId = OBDal.getInstance().get(PackingBox.class, boxId).getObwpackPackingh()
              .getId();
          JSONObject boxIdJsonObj = new JSONObject();
          boxIdJsonObj.put(boxId, qtyInBox);
          OBWPACK_Utils.CreateOrUpdateBoxContent(movLineIdToAdd, packingId, boxIdJsonObj);
        }
      }
    } catch (Exception e) {
      log.error("Error while managing picking list box from ERP", e);
      try {
        response = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        response.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error generating the error message", e2);
        // do nothing, give up
      }
    }
    return response;
  }
}
