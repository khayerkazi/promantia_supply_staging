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
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.DbUtility;

public class ProcessPackHActionHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(ProcessPackHActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    // JSONObject response = null;
    try {
      jsonRequest = new JSONObject(content);
      final String action = jsonRequest.getString("action");
      final JSONArray packHIds = jsonRequest.getJSONArray("packHeaders");
      for (int i = 0; i < packHIds.length(); i++) {
        processPackH(packHIds.getString(i), action);

      }

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "TYPE_SUCCESS");
      errorMessage.put("title", OBMessageUtils.messageBD("Success"));
      errorMessage.put("text", OBMessageUtils.messageBD("OBWPACK_DocumeCom"));
      jsonRequest.put("message", errorMessage);

    } catch (Exception e) {
      log.error("Error in CreateActionHandler", e);
      OBDal.getInstance().rollbackAndClose();

      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "TYPE_ERROR");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error generating the error message", e2);
        // do nothing, give up
      }
    }
    return jsonRequest;
  }

  private void processPackH(String strPackHId, String action) throws Exception {

    if ("processShip".equals(action)) {

      ShipmentInOut ship = OBDal.getInstance().get(ShipmentInOut.class, strPackHId);
      if ("CO".equals(ship.getObwpackProcessed())) {
        ship.setObwpackProcessed("DR");
        ship.setObwpackReactivated("DR");
        ship.getObwpackPackingh().setProcessed("DR");
        ship.getObwpackPackingh().setReactivated("DR");
        ship.getObwpackPackingh().setTotalboxes(null);
        ship.getObwpackPackingh().setTotalweight(null);
        ship.getObwpackPackingh().setUOM(null);
      } else {
        ship.setObwpackProcessed("CO");
        ship.setObwpackReactivated("CO");
        ship.getObwpackPackingh().setProcessed("CO");
        ship.getObwpackPackingh().setReactivated("CO");
        ship.getObwpackPackingh().setTotalboxes(
            0L + ship.getObwpackPackingh().getOBWPACKBoxList().size());
        BigDecimal w = BigDecimal.ZERO;
        for (PackingBox pb : ship.getObwpackPackingh().getOBWPACKBoxList()) {
          if (pb.getWeight() == null)
            throw new Exception(String.format(OBMessageUtils.messageBD("OBWPACK_noWeight"),
                pb.getIdentifier()));
          w = w.add(pb.getWeight());
        }
        ship.getObwpackPackingh().setTotalweight(w);
        ship.getObwpackPackingh().setUOM(null);
      }
      OBDal.getInstance().save(ship);

    } else if ("process".equals(action)) {
      Packing pack = OBDal.getInstance().get(Packing.class, strPackHId);
      if ("CO".equals(pack.getProcessed())) {
        pack.setProcessed("DR");
        pack.setReactivated("DR");
        if (pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().size() == 1
            && pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0)
                .getOBWPACKBoxList().size() != 0
            && pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0)
                .getOBWPACKBoxList().get(0).getGoodsShipment() != null) {

          pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0).getOBWPACKBoxList()
              .get(0).getGoodsShipment().setObwpackProcessed("DR");
          pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0).getOBWPACKBoxList()
              .get(0).getGoodsShipment().setObwpackReactivated("DR");
        }
        pack.setTotalboxes(null);
        pack.setTotalweight(null);
        pack.setUOM(null);
      } else {
        pack.setProcessed("CO");
        pack.setReactivated("CO");
        if (pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().size() == 1
            && pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0)
                .getOBWPACKBoxList().size() != 0
            && pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0)
                .getOBWPACKBoxList().get(0).getGoodsShipment() != null) {

          pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0).getOBWPACKBoxList()
              .get(0).getGoodsShipment().setObwpackProcessed("CO");
          pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList().get(0).getOBWPACKBoxList()
              .get(0).getGoodsShipment().setObwpackReactivated("CO");

        }
        pack.setTotalboxes(0L + pack.getOBWPACKBoxList().size());
        BigDecimal w = BigDecimal.ZERO;
        boolean weightCalculated = false;
        for (PackingBox pb : pack.getOBWPACKBoxList()) {
          if (pb.isWeightcalculated()) {
            weightCalculated = pb.isWeightcalculated();
            w = w.add(pb.getWeight());
          }
        }
        if (weightCalculated) {
          pack.setTotalweight(w);
          pack.setUOM(pack.getOBWPACKBoxList().get(0).getUOM());
        }

      }
      OBDal.getInstance().save(pack);
    } else {
      throw new Exception("Not valid option");
    }

    OBDal.getInstance().flush();
  }
}
