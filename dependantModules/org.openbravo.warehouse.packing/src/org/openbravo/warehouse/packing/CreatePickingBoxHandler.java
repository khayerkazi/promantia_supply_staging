/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.packing;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

public class CreatePickingBoxHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(CreatePickingBoxHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject jsonParams = null;
    JSONObject response = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      jsonParams = jsonRequest.getJSONObject("_params");
      final String packingHeaderId = jsonRequest.getString("inpobwpackPackinghId");
      final String newBoxName = jsonParams.getString("OBWPACK_box_name");
      JSONObject newBox = new JSONObject();
      newBox.put("trackingNo", newBoxName);
      JSONArray boxesToAdd = new JSONArray();
      boxesToAdd.put(newBox);
      OBWPACK_Utils.CreateBoxFromPicking(packingHeaderId, boxesToAdd);
    } catch (Exception e) {
      log.error("Error while creating picking list box from ERP", e);
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
