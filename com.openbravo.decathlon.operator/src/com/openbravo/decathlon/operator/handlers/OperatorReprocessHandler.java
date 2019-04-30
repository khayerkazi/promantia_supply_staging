/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator.handlers;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openbravo.decathlon.operator.registeruser.OBOperatorRegister;

public class OperatorReprocessHandler extends BaseActionHandler {

  private static final Logger log = LoggerFactory.getLogger(OperatorReprocessHandler.class);

  @Inject
  OBOperatorRegister operatorRegister;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    String opErrorId = null;
    JSONObject output = null;
    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONArray errorIds = jsonData.getJSONArray("operatorErrors");

      int count = 0;
      for (int i = 0; i < errorIds.length(); i++) {
        opErrorId = errorIds.getString(i);
        JSONObject operatorprocess = operatorRegister.processOperatorRegister(opErrorId);
        output = operatorprocess;
        if (operatorprocess.getJSONObject("message").getString("severity").equals("error")) {
          output.getJSONObject("message").put("severity", "error");
          String strError = output.getJSONObject("message").getString("text");
          output.getJSONObject("message").put("text", strError);
          count++;
        }
      }

      if (count > 1) {
        String errorMessage = OBMessageUtils.messageBD("decope_errors_reprocessing");
        Map<String, String> errorParam = new HashMap<String, String>();
        errorParam.put("recordCount", String.valueOf(count));
        String returningError = OBMessageUtils.parseTranslation(errorMessage, errorParam);
        output.getJSONObject("message").put("text", returningError);
      }

    } catch (JSONException e) {
      log.error("Error in Operator Reprocess", e);
    }
    return output;
  }
}
