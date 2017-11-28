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
 * All portions are Copyright (C) 2012-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.warehouse.pickinglist;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.hooks.ProcessPLHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickingListActionHandler extends BaseActionHandler {
  final private static Logger log = LoggerFactory.getLogger(PickingListActionHandler.class);

  @Inject
  @Any
  private Instance<ProcessPLHook> processPLHooks;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = null;

    try {
      JSONObject jsonRequest = new JSONObject(content);
      final String strAction = jsonRequest.getString("action");
      final JSONArray plIds = jsonRequest.getJSONArray("pickinglist");
      if ("cancel".equals(strAction)) {
        jsonResponse = doCancel(plIds);
      } else if ("process".equals(strAction)) {
        jsonResponse = doProcess(plIds);
      } else if ("close".equals(strAction)) {
        jsonResponse = doClose(plIds);
      }

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

  private JSONObject doProcess(JSONArray plIds) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    // Get pickinglist
    for (int i = 0; i < plIds.length(); i++) {
      PickingList pl = OBDal.getInstance().get(PickingList.class, plIds.getString(i));
      int pl_lines = 0;
      pl_lines = pl.getMaterialMgmtShipmentInOutLineEMObwplPickinglistIDList().size();
      if (pl_lines > 0) {
        processPL(plIds.getString(i));
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "success");
        errorMessage.put("text", OBMessageUtils.messageBD("Success", false));
        jsonResponse.put("message", errorMessage);
      } else {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWPL_NoLines", false));
        jsonResponse.put("message", errorMessage);
      }
    }
    return jsonResponse;
  }

  private void processPL(String plID) {
    PickingList pl = OBDal.getInstance().get(PickingList.class, plID);
    final Set<String> ships = new HashSet<String>();
    final Set<String> orders = new HashSet<String>();
    List<Order> completedOrders = new ArrayList<Order>();

    for (ShipmentInOutLine iol : pl.getMaterialMgmtShipmentInOutLineEMObwplPickinglistIDList()) {
      if (("DR").equals(iol.getShipmentReceipt().getDocumentStatus())) {
        ships.add(iol.getShipmentReceipt().getId());
      }
      orders.add(iol.getSalesOrderLine().getSalesOrder().getId());
    }
    for (String strShipId : ships) {
      final List<Object> param = new ArrayList<Object>();
      param.add(null);
      param.add(strShipId);
      CallStoredProcedure.getInstance().call("M_Inout_POST", param, null, true, false);
    }
    pl.setPickliststatus("CO");
    for (String strOrderId : orders) {
      // check there is no orderline in other draft pickinglist
      StringBuilder whereClause = new StringBuilder();
      List<Object> parameters = new ArrayList<Object>();
      whereClause.append(" as iol ");
      whereClause.append(" where iol.");
      whereClause.append(ShipmentInOutLine.PROPERTY_OBWPLPICKINGLIST);
      whereClause.append("." + PickingList.PROPERTY_PICKLISTSTATUS + " = 'DR'");
      whereClause.append(" and ");
      whereClause.append(ShipmentInOutLine.PROPERTY_OBWPLPICKINGLIST);
      whereClause.append(" is not null ");
      whereClause.append(" and ");
      whereClause.append(ShipmentInOutLine.PROPERTY_OBWPLPICKINGLIST);
      whereClause.append(" <> ? ");
      parameters.add(pl);
      whereClause.append(" and ");
      whereClause.append(ShipmentInOutLine.PROPERTY_SALESORDERLINE);
      whereClause.append("." + OrderLine.PROPERTY_ID + " IN (");
      whereClause.append(" SELECT ");
      whereClause.append(OrderLine.PROPERTY_ID);
      whereClause.append(" FROM OrderLine as ol");
      whereClause.append(" WHERE ol." + OrderLine.PROPERTY_SALESORDER + ".id = ?");
      parameters.add(strOrderId);
      whereClause.append(" )");
      OBQuery<ShipmentInOutLine> obData = OBDal.getInstance().createQuery(ShipmentInOutLine.class,
          whereClause.toString(), parameters);
      if (obData.count() == 0) {
        Order o = OBDal.getInstance().get(Order.class, strOrderId);
        o.setObwplIsinpickinglist(false);
        completedOrders.add(o);
      }
    }
    OBDal.getInstance().flush();

    try {
      executeProcessPLHooks(pl, orders, ships, completedOrders);
    } catch (Exception e) {
      log.error("An error happened when processPLHook was executed.", e.getMessage(),
          e.getStackTrace());
    }
  }

  private JSONObject doCancel(JSONArray plIds) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    boolean processedShip = false;
    // Get orders
    for (int i = 0; i < plIds.length(); i++) {
      PickingList pl = OBDal.getInstance().get(PickingList.class, plIds.getString(i));
      ShipmentInOut ship = null;
      for (ShipmentInOutLine iol : pl.getMaterialMgmtShipmentInOutLineEMObwplPickinglistIDList()) {
        OrderLine oline = iol.getSalesOrderLine();
        oline.getSalesOrder().setObwplIsinpickinglist(false);
        ship = iol.getShipmentReceipt();
        // if shipment is completed or voided,
        // then picklist status is set to cancel
        if ("CO".equals(ship.getDocumentStatus()) || "VO".equals(ship.getDocumentStatus())) {
          pl.setPickliststatus("CA");
          processedShip = true;
          OBDal.getInstance().save(pl);
        } else {
          Reservation res = ReservationUtils.getReservationFromOrder(oline);
          if (res.isOBWPLGeneratedByPickingList()) {
            // If the reservation was created using picking list it has to be deleted.
            removeReservation(iol);
          }
          OBDal.getInstance().remove(iol);
        }
      }
      OBDal.getInstance().flush();
      if (!processedShip) {
        // Only remove ship that has no lines.
        pl.getMaterialMgmtShipmentInOutLineEMObwplPickinglistIDList().clear();
        OBDal.getInstance().remove(pl);
        if (ship != null)
          OBDal.getInstance().remove(ship);
        OBDal.getInstance().flush();
      }
    }
    JSONObject errorMessage = new JSONObject();
    errorMessage.put("severity", "success");
    errorMessage.put("text", OBMessageUtils.messageBD("Success", false));
    jsonResponse.put("message", errorMessage);
    return jsonResponse;
  }

  private void removeReservation(ShipmentInOutLine iol) {
    Reservation res = ReservationUtils.getReservationFromOrder(iol.getSalesOrderLine());
    String iolLocator = iol.getStorageBin().getId();
    String iolAttr = iol.getAttributeSetValue() == null ? "0" : iol.getAttributeSetValue().getId();
    BigDecimal iolQty = iol.getMovementQuantity();
    ReservationStock resStock = null;
    for (ReservationStock rSAux : res.getMaterialMgmtReservationStockList()) {
      String rSLocator = rSAux.getStorageBin().getId();
      String rSAttr = rSAux.getAttributeSetValue() == null ? "0" : rSAux.getAttributeSetValue()
          .getId();
      BigDecimal rSQty = rSAux.getQuantity();
      if (rSLocator.equals(iolLocator) && iolAttr.equals(rSAttr) && iolQty.compareTo(rSQty) == 0) {
        resStock = rSAux;
        break;
      }
    }
    if (resStock != null) {
      resStock.setQuantity(resStock.getQuantity().subtract(iol.getMovementQuantity()));
      if (resStock.getQuantity().equals(BigDecimal.ZERO)) {
        res.getMaterialMgmtReservationStockList().remove(resStock);
        // Flush to persist changes in database and execute triggers.
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(res);
        if (res.getReservedQty().equals(BigDecimal.ZERO)
            && res.getMaterialMgmtReservationStockList().isEmpty()) {
          if (res.getRESStatus().equals("CO")) {
            // Unprocess reservation
            ReservationUtils.processReserve(res, "RE");
            OBDal.getInstance().save(res);
            OBDal.getInstance().flush();
          }
          OBDal.getInstance().remove(res);
        }
      }
    }
  }

  private JSONObject doClose(JSONArray plIds) throws JSONException {
    boolean hasErrors = false;
    StringBuffer finalMsg = new StringBuffer();
    for (int i = 0; i < plIds.length(); i++) {
      final String strPLId = plIds.getString(i);
      final PickingList pl = OBDal.getInstance().get(PickingList.class, strPLId);
      if (finalMsg.length() > 0) {
        finalMsg.append("\n");
      }
      finalMsg.append(pl.getDocumentNo() + ": ");
      OBError plMsg = OutboundPickingListProcess.close(pl);
      if (!"success".equals(plMsg.getType())) {
        hasErrors = true;
      }
      finalMsg.append(plMsg.getMessage());
    }
    JSONObject jsonResponse = new JSONObject();
    JSONObject jsonMsg = new JSONObject();
    jsonMsg.put("severity", hasErrors ? "error" : "success");
    jsonMsg.put("text", finalMsg.toString());
    jsonResponse.put("message", jsonMsg);

    return jsonResponse;
  }

  private void executeProcessPLHooks(PickingList pickingList, Set<String> orders,
      Set<String> shipments, List<Order> completedOrders) throws Exception {
    for (ProcessPLHook hook : processPLHooks) {
      hook.exec(pickingList, orders, shipments, completedOrders);
    }
  }
}
