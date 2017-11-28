/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.terminal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.service.json.JsonConstants;

public class OtherPreferencesLoader extends WarehouseJSONProcess {
  private static final Logger log = Logger.getLogger(OtherPreferencesLoader.class);

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    JSONArray prefsToGet = jsonsent.getJSONObject("parameters").getJSONArray("prefs");
    JSONObject finalResult = new JSONObject();
    JSONArray arrPrefs = new JSONArray();

    for (int i = 0; i < prefsToGet.length(); i++) {
      JSONObject curPref = new JSONObject();
      String prefValue = "N";
      Boolean process = true;
      try {
        prefValue = Preferences.getPreferenceValue(prefsToGet.getString(i), true, OBContext
            .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
            OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
      } catch (Exception e) {
        log.warn("A problem happened while getting value for preference -"
            + prefsToGet.getString(i) + "-. This one will not be available in mobile app");
        process = false;
      }
      if (process) {
        curPref.put("searchKey", prefsToGet.getString(i));
        if ("Y".equals(prefValue)) {
          curPref.put("value", true);
        } else if ("N".equals(prefValue)) {
          curPref.put("value", false);
        } else {
          curPref.put("value", prefValue);
        }
        arrPrefs.put(curPref);
      }
    }
    finalResult.put(JsonConstants.RESPONSE_DATA, arrPrefs);
    finalResult.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    return finalResult;
  }
}
