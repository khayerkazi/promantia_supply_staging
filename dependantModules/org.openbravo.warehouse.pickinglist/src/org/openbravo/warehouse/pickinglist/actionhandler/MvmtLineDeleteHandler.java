/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.pickinglist.actionhandler;

import java.util.HashSet;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.OutboundPickingListProcess;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MvmtLineDeleteHandler extends BaseActionHandler {
  final private static Logger log = LoggerFactory.getLogger(MvmtLineDeleteHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      final JSONObject jsonData = new JSONObject(content);
      final JSONArray mvmtLineIds = jsonData.getJSONArray("mvmtLines");
      HashSet<PickingList> picklists = new HashSet<PickingList>();
      HashSet<PickingList> groupPicklists = new HashSet<PickingList>();
      HashSet<String> removedOrderIds = new HashSet<String>();
      for (int i = 0; i < mvmtLineIds.length(); i++) {
        final String strMvmtLineId = mvmtLineIds.getString(i);
        final InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class,
            strMvmtLineId);
        if (mvmtLine.getOBWPLWarehousePickingList() != null) {
          PickingList picklist = mvmtLine.getOBWPLWarehousePickingList();
          picklist.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList().remove(
              mvmtLine);
          picklists.add(picklist);
        }
        if (mvmtLine.getOBWPLGroupPickinglist() != null) {
          PickingList picklist = mvmtLine.getOBWPLWarehousePickingList();
          picklist.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList().remove(
              mvmtLine);
          groupPicklists.add(picklist);
        }
        removedOrderIds.add(mvmtLine.getStockReservation().getSalesOrderLine().getSalesOrder()
            .getId());

        if (!mvmtLine.getOBWPLItemStatus().equals("PE")) {
          throw new OBException(OBMessageUtils.messageBD("DocumentProcessed", false));
        }
        mvmtLine.setOBWPLAllowDelete(true);
        OBDal.getInstance().save(mvmtLine);
        OBDal.getInstance().flush();

        // delete it
        OBDal.getInstance().remove(mvmtLine.getMovement());
        OBDal.getInstance().flush();
      }
      for (PickingList picklist : picklists) {
        for (InternalMovementLine mvmtLine : picklist
            .getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList()) {
          removedOrderIds.remove(mvmtLine.getStockReservation().getSalesOrderLine().getSalesOrder()
              .getId());
        }
        String strStatus = OutboundPickingListProcess.checkStatus(
            picklist.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList(), true);
        if (!strStatus.equals(picklist.getPickliststatus())) {
          picklist.setPickliststatus(strStatus);
        }
      }
      for (PickingList picklist : groupPicklists) {
        for (InternalMovementLine mvmtLine : picklist
            .getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList()) {
          removedOrderIds.remove(mvmtLine.getStockReservation().getSalesOrderLine().getSalesOrder()
              .getId());
        }
        String strStatus = OutboundPickingListProcess.checkStatus(
            picklist.getMaterialMgmtInternalMovementLineEMOBWPLGroupPickinglistList(), false);
        if ("AS".equals(strStatus) && picklist.getUserContact() == null) {
          strStatus = "DR";
        }
        if (!strStatus.equals(picklist.getPickliststatus())) {
          picklist.setPickliststatus(strStatus);
        }
      }
      for (String strOrderId : removedOrderIds) {
        Order order = OBDal.getInstance().get(Order.class, strOrderId);
        order.setObwplIsinpickinglist(false);
        OBDal.getInstance().save(order);
      }
      // create the result
      jsonResponse = new JSONObject();
      JSONObject message = new JSONObject();
      message.put(
          "text",
          OBMessageUtils.messageBD("OBUIAPP_DeleteResult", false).replace("%0",
              Integer.toString(mvmtLineIds.length())));
      message.put("title", OBMessageUtils.messageBD("OBUIAPP_Success", false));
      message.put("severity", "success");
      jsonResponse.put("message", message);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OBDal.getInstance().rollbackAndClose();

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();

        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);

      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    }
    return jsonResponse;
  }

}
