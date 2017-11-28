/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.menu;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.warehouse.WarehouseMenu;

@Qualifier("wh-movement")
public class GoodMovementsParamHandler implements MenuNodeParameterHandler {

  @Override
  public JSONObject getParameters(WarehouseMenu menuNode) throws JSONException {
    JSONObject params = new JSONObject();
    if (menuNode.getFromBin() != null) {
      JSONObject fromBin = new JSONObject();
      fromBin.put("name", menuNode.getFromBin().getIdentifier());
      fromBin.put("id", menuNode.getFromBin().getId());
      fromBin.put("readonly", menuNode.isFromBinReadonly());
      params.put("fromBin", fromBin);
    }

    if (menuNode.getBin() != null) {
      JSONObject toBin = new JSONObject();
      toBin.put("name", menuNode.getBin().getIdentifier());
      toBin.put("id", menuNode.getBin().getId());
      toBin.put("readonly", menuNode.isBinReadonly());
      params.put("toBin", toBin);
    }
    return params;
  }

}
