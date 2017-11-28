/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.barcode;

import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;

public class ScanHandler extends WarehouseJSONProcess {
  private final static Logger log = Logger.getLogger(ScanHandler.class);

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Inject
  @Any
  private Instance<BarcodeScanner> scanners;

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    OBContext.setAdminMode(true);
    try {
      JSONObject response = null;
      String eventName = null;
      String code = jsonsent.getString("code");
      JSONObject line = jsonsent.getJSONObject("line");

      if (jsonsent.has("eventName") && !jsonsent.isNull("eventName")) {
        eventName = jsonsent.getString("eventName");
      } else {
        log.error("not found eventName");
      }

      Iterator<BarcodeScanner> scannerIterator = scanners.iterator();
      while (scannerIterator.hasNext()) {
        BarcodeScanner scanner = scannerIterator.next();
        log.debug("scanning " + code + " with " + scanner.getClass().getName());
        if (scanner.isValidEvent(eventName)) {
          response = scanner.scan(code, line);
        }
        if (response != null && response.has("status")) {
          log.debug("found item with scanner " + scanner.getClass().getName());
          return response;
        }
      }

      response = new JSONObject();
      response.put("status", 1);
      JSONObject jsonError = new JSONObject();
      jsonError.put("message",
          OBMessageUtils.getI18NMessage("OBWH_NoItemWithCode", new String[] { code }));
      response.put("error", jsonError);
      log.warn("not found item for code [" + code + "]");

      return response;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
