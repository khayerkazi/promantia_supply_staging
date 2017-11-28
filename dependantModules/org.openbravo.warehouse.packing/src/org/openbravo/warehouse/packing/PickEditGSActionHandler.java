/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.packing;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.DbUtility;

public class PickEditGSActionHandler extends BaseProcessActionHandler {
  final private static Logger log = Logger.getLogger(PickEditGSActionHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);

      final String strPACKId = jsonRequest.getString("inpobwpackPackinghId");
      final Packing pack = OBDal.getInstance().get(Packing.class, strPACKId);

      if (pack == null) {
        return jsonRequest;
      }

      List<String> idList = OBDao.getIDListFromOBObject(pack
          .getMaterialMgmtShipmentInOutEMObwpackPackinghIDList());

      JSONArray selectedLines = jsonRequest.getJSONArray("_selection");

      boolean removed = true;
      for (String strId : idList) {
        removed = true;
        for (int j = 0; j < selectedLines.length(); j++) {
          JSONObject selectedLine = selectedLines.getJSONObject((int) j);
          if (strId.equals(selectedLine.getString("goodsShipment"))) {
            removed = false;
            break;
          }
        }
        if (removed == true) {
          // delete packs boxes, and products
          removeBoxesAndProducts(pack);
          break;
        }
      }

      removeNonSelectedLines(idList);

      // if there are more than one shipment. SET null m_inout_id in obwpack_box
      if (idList.size() == 1 && selectedLines.length() > 1) {
        ShipmentInOut ship = OBDal.getInstance().get(ShipmentInOut.class, idList.get(0));
        for (PackingBox box : ship.getOBWPACKBoxList()) {
          box.setGoodsShipment(null);
          OBDal.getInstance().save(box);
        }
        OBDal.getInstance().flush();
      }

      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject((int) i);
        log.debug(selectedLine);

        ShipmentInOut ship = OBDal.getInstance().get(ShipmentInOut.class,
            selectedLine.getString("goodsShipment"));
        ship.setObwpackPackingh(pack);
        OBDal.getInstance().save(ship);

      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Error in PickEditGSActionHandler Action Handler", e);

      try {
        jsonRequest = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonRequest.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
        // do nothing, give up
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonRequest;
  }

  private void removeNonSelectedLines(List<String> idList) {
    if (idList.size() > 0) {
      for (String id : idList) {
        ShipmentInOut rl = OBDal.getInstance().get(ShipmentInOut.class, id);
        rl.setObwpackPackingh(null);

        OBDal.getInstance().save(rl);
      }
      OBDal.getInstance().flush();
    }
  }

  private void removeBoxesAndProducts(Packing pack) {
    List<PackingBox> idList = pack.getOBWPACKBoxList();

    for (PackingBox pb : idList) {
      pb.getOBWPACKBoxProductList().clear();
      OBDal.getInstance().save(pb);
    }
    pack.getOBWPACKBoxList().clear();
    OBDal.getInstance().save(pack);
    OBDal.getInstance().flush();

  }
}
