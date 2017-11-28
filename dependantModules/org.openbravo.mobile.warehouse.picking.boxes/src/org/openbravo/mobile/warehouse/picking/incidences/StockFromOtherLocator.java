/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.incidences;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.warehouse.pickinglist.OBWPL_Utils;

public class StockFromOtherLocator extends WarehouseJSONProcess {

  @Override
  protected String getProperty() {
    return "OBMWHP_alternateLocator";
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    List<Map<String, Object>> nearestStockInfo = null;

    JSONObject result = new JSONObject();
    JSONObject jsonToReturn = new JSONObject();
    JSONArray nearestStockAvailable = new JSONArray();

    JSONArray items = jsonsent.getJSONArray("items");
    BigDecimal pickedQty = new BigDecimal(jsonsent.getInt("pickedQty"));
    BigDecimal neededQty = new BigDecimal(jsonsent.getInt("neededQty"));
    BigDecimal pendingQty = neededQty.subtract(pickedQty);

    if (items.length() >= 1) {
      OBContext.setAdminMode(true);
      try {
        InternalMovementLine movLine = OBDal.getInstance().get(InternalMovementLine.class,
            items.getJSONObject(0).getString("id"));
        try {
          nearestStockInfo = OBWPL_Utils.getStockFromNearestBin(movLine, pendingQty);
        } catch (Exception e) {
          throw new JSONException(e.getMessage());
        }

        for (Map<String, Object> map : nearestStockInfo) {
          JSONObject alternateLocatorStockItem = new JSONObject();
          StockProposed curStockProposed = (StockProposed) map.get("stockProposed");
          Locator curBin = OBDal.getInstance().get(Locator.class,
              curStockProposed.getStorageDetail().getStorageBin().getId());
          alternateLocatorStockItem.put("bin", curBin.getIdentifier());
          alternateLocatorStockItem.put("x", curBin.getRowX());
          alternateLocatorStockItem.put("y", curBin.getStackY());
          alternateLocatorStockItem.put("z", curBin.getLevelZ());
          alternateLocatorStockItem.put("pickedQty", map.get("pickedQty").toString());
          if (curStockProposed.getStorageDetail().getAttributeSetValue() != null) {
            alternateLocatorStockItem.put("attSet", curStockProposed.getStorageDetail()
                .getAttributeSetValue().getIdentifier());
          }
          nearestStockAvailable.put(alternateLocatorStockItem);
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    } else {
      throw new JSONException("One movement is needed to find nearest stock.");
    }
    jsonToReturn.put("alternateLocators", nearestStockAvailable);
    result.put(JsonConstants.RESPONSE_DATA, jsonToReturn);
    result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    return result;
  }
}
