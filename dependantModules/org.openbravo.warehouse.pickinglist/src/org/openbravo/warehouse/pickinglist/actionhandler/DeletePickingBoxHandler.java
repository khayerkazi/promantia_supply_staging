/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.pickinglist.actionhandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.OBWPL_PickingBoxesUtils;
import org.openbravo.warehouse.pickinglist.PickingListBox;

public class DeletePickingBoxHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(DeletePickingBoxHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject response = new JSONObject();
    try {
      jsonRequest = new JSONObject(content);
      final String pickingListBoxId = jsonRequest.getString("inpobwplPlboxId");
      PickingListBox boxToDelete = OBDal.getInstance().get(PickingListBox.class, pickingListBoxId);
      OBWPL_PickingBoxesUtils.deletePickingBox(boxToDelete);
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
