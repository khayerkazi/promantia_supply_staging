/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2013-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.warehouse.pickinglist;

import java.util.HashSet;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectOrdersHandler extends BaseProcessActionHandler {
  final private static Logger log = LoggerFactory.getLogger(SelectOrdersHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject response = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      response = new JSONObject();
      final String strPickingId = jsonRequest.getString("Obwpl_Pickinglist_ID");
      final PickingList picking = OBDal.getInstance().get(PickingList.class, strPickingId);
      JSONArray selectedLines = jsonRequest.getJSONArray("_selection");
      log.debug("{}", jsonRequest);
      StringBuffer msg = new StringBuffer();
      HashSet<String> notCompletedPL = new HashSet<String>();

      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject row = selectedLines.getJSONObject(i);
        final String strOrderId = row.getString("order");
        Order order = OBDal.getInstance().get(Order.class, strOrderId);
        String strMessage = Utilities.processOrderOutbound(picking, order, notCompletedPL);
        if (msg.length() > 0) {
          msg.append("<br>");
        }
        msg.append("Order " + order.getDocumentNo() + ": " + strMessage);
      }
      Utilities.updatePickingListDescription(strPickingId);
      JSONObject jsonMsg = new JSONObject();
      jsonMsg.put("severity", "success");
      jsonMsg.put("text", msg);

      response.put("message", jsonMsg);

    } catch (OBException e) {
      log.error("Error in SelectOrdersHandler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        response = new JSONObject();
        String message = OBMessageUtils.parseTranslation(e.getMessage());
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        response.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error generating the error message", e2);
      }

    } catch (Exception e) {
      log.error("Error in SelectOrdersHandler", e);
      OBDal.getInstance().rollbackAndClose();
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
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return response;

  }
}
