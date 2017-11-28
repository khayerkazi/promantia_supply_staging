/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.incidences;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.warehouse.pickinglist.PickingListProblem;

public class GetUndoIncidenceApproval extends WarehouseJSONProcess {

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    PickingListProblem incidence = null;
    Boolean result = true;
    JSONObject finalResult = new JSONObject();
    JSONObject jsonToReturn = new JSONObject();

    String incidenceId = jsonsent.getString("incidenceId");

    OBContext.setAdminMode(true);
    try {

      if (incidenceId != null && !incidenceId.equals("")) {
        incidence = OBDal.getInstance().get(PickingListProblem.class, incidenceId);
        if (incidence.getObwplPickinglistincidence().getIncidencetype()
            .equals("OBWPL_BoxEmptyIncidence")
            && incidence.getStatus().equals("IC")) {
          result = false;
        }
      } else {
        throw new JSONException("Incidence id is needed to get movements");
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    jsonToReturn.put("allowedToUndo", result);
    finalResult.put(JsonConstants.RESPONSE_DATA, jsonToReturn);
    finalResult.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    return finalResult;
  }
}
