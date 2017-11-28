/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.incidences;

import java.util.List;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.warehouse.pickinglist.OBWPL_Utils;
import org.openbravo.warehouse.pickinglist.PickingListProblem;

public class GetGeneratedMovementsByIncidence extends WarehouseJSONProcess {

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    List<InternalMovementLine> generatedMovs = null;
    PickingListProblem incidence = null;
    JSONObject result = new JSONObject();
    JSONObject jsonToReturn = new JSONObject();
    JSONArray jsonArrMovs = new JSONArray();

    String incidenceId = jsonsent.getString("incidenceId");

    OBContext.setAdminMode(true);
    try {

      if (incidenceId != null && !incidenceId.equals("")) {
        incidence = OBDal.getInstance().get(PickingListProblem.class, incidenceId);
        if (incidence.getObwplPickinglistincidence().getIncidencetype()
            .equals("OBWPL_AlternateLocationIncidence")
            || incidence.getObwplPickinglistincidence().getIncidencetype()
                .equals("OBWPL_BoxEmptyIncidence")) {
          generatedMovs = OBWPL_Utils.getGeneratedMovemtLinesByIncidence(incidence);
          for (InternalMovementLine movLine : generatedMovs) {
            JSONObject movLineItem = new JSONObject();

            movLineItem.put(InternalMovementLine.PROPERTY_MOVEMENTQUANTITY,
                movLine.getMovementQuantity());
            movLineItem.put(InternalMovementLine.PROPERTY_OBWPLPICKEDQTY,
                movLine.getOBWPLPickedqty());
            movLineItem.put(InternalMovementLine.PROPERTY_OBWPLITEMSTATUS,
                movLine.getOBWPLItemStatus());
            movLineItem.put("pendingQtyToPick",
                movLine.getMovementQuantity().subtract(movLine.getOBWPLPickedqty()));
            movLineItem.put(InternalMovementLine.PROPERTY_STORAGEBIN, movLine.getStorageBin()
                .getId());
            movLineItem.put(InternalMovementLine.PROPERTY_STORAGEBIN + "_identifier", movLine
                .getStorageBin().getIdentifier());
            movLineItem.put(InternalMovementLine.PROPERTY_STORAGEBIN + "_position", movLine
                .getStorageBin().getRowX()
                + "-"
                + movLine.getStorageBin().getStackY()
                + "-"
                + movLine.getStorageBin().getLevelZ());

            jsonArrMovs.put(movLineItem);
          }
        } else {
          throw new JSONException("This action is only available for alternate location incidences");
        }
      } else {
        throw new JSONException("Incidence id is needed to get movements");
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    jsonToReturn.put("generatedMovements", jsonArrMovs);
    result.put(JsonConstants.RESPONSE_DATA, jsonToReturn);
    result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    return result;
  }
}
