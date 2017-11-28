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
 * All portions are Copyright (C) 2013-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.warehouse.pickinglist;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPickingListItemHandler extends BaseProcessActionHandler {
  final private static Logger log = LoggerFactory.getLogger(EditPickingListItemHandler.class);
  private static final AttributeSetInstance asiZero = OBDal.getInstance().get(
      AttributeSetInstance.class, "0");
  private PickingList basePicking;
  private PickingList baseGroupPicking;
  private Reservation reservation;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject response = null;
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      log.debug("{}", jsonRequest);
      response = new JSONObject();
      StringBuffer msg = new StringBuffer();
      final String strMvmtLineId = jsonRequest.getString("M_MovementLine_ID");
      InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class,
          strMvmtLineId);
      basePicking = mvmtLine.getOBWPLWarehousePickingList();
      baseGroupPicking = mvmtLine.getOBWPLGroupPickinglist();
      reservation = mvmtLine.getStockReservation();
      Map<String, ReservationStock> curResStocks = getCurrentStockReservations();
      JSONArray selectedLines = jsonRequest.getJSONArray("_selection");
      for (int i = 0; i < selectedLines.length(); i++) {
        JSONObject selectedLine = selectedLines.getJSONObject(i);
        updateStockReservations(selectedLine, curResStocks);
      }
      if (!curResStocks.isEmpty()) {
        // Delete unselected lines.
        deleteStockReservations(curResStocks);
      }
      // commented for DSI
      // final String strPickingId = jsonRequest.getString("Obwpl_Pickinglist_ID");
      // Utilities.updatePickingListDescription(strPickingId);
      JSONObject jsonMsg = new JSONObject();
      jsonMsg.put("severity", "success");
      jsonMsg.put("text", msg.toString());

      response.put("message", jsonMsg);

    } catch (OBException e) {
      log.error("Error in SelectOrdersHandler", e);
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

  private void updateStockReservations(JSONObject selectedLine,
      Map<String, ReservationStock> curResStocks) throws JSONException {
    String strResStockId = selectedLine.get("reservationStock").equals(null) ? "" : selectedLine
        .getString("reservationStock");
    ReservationStock resStock = null;
    InternalMovementLine thisMvmtLine = null;
    List<InternalMovementLine> otherMvmtLines = Collections.emptyList();
    if (StringUtils.isNotEmpty(strResStockId)) {
      resStock = OBDal.getInstance().get(ReservationStock.class, strResStockId);
      if (resStock.getStorageBin() != null) {
        thisMvmtLine = getMvmtLines(resStock.getStorageBin(), resStock.getAttributeSetValue(),
            otherMvmtLines);
      }
      curResStocks.remove(getCurStockResKey(resStock));
    } else {
      resStock = OBProvider.getInstance().get(ReservationStock.class);

      resStock.setReservation(reservation);
      resStock.setOrganization(reservation.getOrganization());

      final String strLocator = selectedLine.get("storageBin").equals(null) ? "" : selectedLine
          .getString("storageBin");
      if (StringUtils.isNotBlank(strLocator)) {
        resStock.setStorageBin((Locator) OBDal.getInstance().getProxy(Locator.ENTITY_NAME,
            strLocator));
      }
      final String strASIId = selectedLine.get("attributeSetValue").equals(null) ? ""
          : selectedLine.getString("attributeSetValue");
      if (StringUtils.isNotBlank(strASIId)) {
        resStock.setAttributeSetValue((AttributeSetInstance) OBDal.getInstance().getProxy(
            AttributeSetInstance.ENTITY_NAME, strASIId));
      }
      final String strOrderLineId = selectedLine.get("purchaseOrderLine").equals(null) ? ""
          : selectedLine.getString("purchaseOrderLine");
      if (StringUtils.isNotBlank(strOrderLineId)) {
        resStock.setSalesOrderLine((OrderLine) OBDal.getInstance().getProxy(OrderLine.ENTITY_NAME,
            strOrderLineId));
      }

      reservation.getMaterialMgmtReservationStockList().add(resStock);
    }

    final Boolean isAllocated = selectedLine.getBoolean("allocated");
    resStock.setAllocated(isAllocated == true);
    final BigDecimal qty = new BigDecimal(selectedLine.getString("quantity"));
    resStock.setQuantity(qty);

    OBDal.getInstance().save(resStock);
    OBDal.getInstance().save(reservation);
    OBDal.getInstance().flush();

    BigDecimal pendingQty = qty;
    if (resStock.getReleased() != null) {
      pendingQty = pendingQty.subtract(resStock.getReleased());
    }
    // Update related movement lines.
    // First iterate movement lines that belong to other pickings. This movement lines cannot be
    // modified. If their quantity is lower than the pending quantity an exception is thrown.
    for (InternalMovementLine mvmtLine : otherMvmtLines) {
      pendingQty = pendingQty.subtract(mvmtLine.getMovementQuantity());
      if (pendingQty.signum() == -1) {
        throw new OBException("OBWPL_CannotUnreserveOtherPickings");
      }
    }
    if (thisMvmtLine != null
        && thisMvmtLine.getOBWPLItemStatus() != null
        && (thisMvmtLine.getOBWPLItemStatus().equals("CO") || thisMvmtLine.getOBWPLItemStatus()
            .equals("CF"))) {
      // Movement line processed, cannot be modified. Check pending quantity is higher.
      pendingQty = pendingQty.subtract(thisMvmtLine.getMovementQuantity());
      if (pendingQty.signum() == -1) {
        throw new OBException("OBWPL_CannotModifyProcessedMovement");
      }
      thisMvmtLine = null;
    }

    if (thisMvmtLine == null) {
      if (pendingQty.signum() == 1) {
        // Create movement line
        OBWPL_Utils.createGoodMovement(resStock, basePicking, baseGroupPicking, pendingQty, null);
      }
      return;
    }

    if (pendingQty.compareTo(thisMvmtLine.getMovementQuantity()) == 0) {
      // Nothing to change
      return;
    }
    if (pendingQty.signum() == 0) {
      // Remove movement line
      InternalMovement mvmt = thisMvmtLine.getMovement();
      thisMvmtLine.setOBWPLAllowDelete(true);
      OBDal.getInstance().save(thisMvmtLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().remove(mvmt);
      Order order = reservation.getSalesOrderLine().getSalesOrder();
      if (order.isObwplIsinpickinglist()) {
        // comented for dsi
        // order.setObwplIsinpickinglist(false);
        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();
      }
      return;
    } else {
      // Update movement line
      thisMvmtLine.setMovementQuantity(pendingQty);
      OBDal.getInstance().save(thisMvmtLine);
      return;
    }
  }

  private void deleteStockReservations(Map<String, ReservationStock> curResStocks) {
    for (String key : curResStocks.keySet()) {
      ReservationStock resStock = curResStocks.get(key);
      if (resStock.getStorageBin() != null) {
        List<InternalMovementLine> otherMvmtLines = Collections.emptyList();
        InternalMovementLine thisMvmtLine = getMvmtLines(resStock.getStorageBin(),
            resStock.getAttributeSetValue(), otherMvmtLines);
        if (!otherMvmtLines.isEmpty()) {
          throw new OBException("OBWPL_CannotUnreserveOtherPickings");
        }
        thisMvmtLine.setOBWPLAllowDelete(true);
        OBDal.getInstance().save(thisMvmtLine);
        OBDal.getInstance().flush();
        InternalMovement mvmt = thisMvmtLine.getMovement();
        OBDal.getInstance().remove(mvmt);
        Order order = reservation.getSalesOrderLine().getSalesOrder();
        if (order.isObwplIsinpickinglist()) {
          // commented for DSI
          // order.setObwplIsinpickinglist(false);
          OBDal.getInstance().save(order);
          OBDal.getInstance().flush();
        }
      }
      // we cannot remove this object without remove it first from reservation
      reservation.getMaterialMgmtReservationStockList().remove(resStock);
      OBDal.getInstance().remove(resStock);
    }
  }

  private Map<String, ReservationStock> getCurrentStockReservations() {
    Map<String, ReservationStock> curResStocks = new HashMap<String, ReservationStock>();
    for (ReservationStock resStock : reservation.getMaterialMgmtReservationStockList()) {
      String strKey = getCurStockResKey(resStock);
      curResStocks.put(strKey, resStock);
    }
    return curResStocks;
  }

  private String getCurStockResKey(ReservationStock resStock) {
    String strKey;
    if (resStock.getStorageBin() != null) {
      String strASIId = resStock.getAttributeSetValue() == null ? "0" : resStock
          .getAttributeSetValue().getId();
      strKey = "sd-" + resStock.getStorageBin().getId() + "-" + strASIId;
    } else {
      strKey = "pol-" + resStock.getSalesOrderLine();
    }
    return strKey;
  }

  private InternalMovementLine getMvmtLines(Locator storageBin,
      AttributeSetInstance attributeSetValue, List<InternalMovementLine> otherMvmtLines) {
    InternalMovementLine thisMvmtLine;
    OBCriteria<InternalMovementLine> critThisMvmtLine = OBDal.getInstance().createCriteria(
        InternalMovementLine.class);
    critThisMvmtLine.add(Restrictions.eq(InternalMovementLine.PROPERTY_STOCKRESERVATION,
        reservation));
    critThisMvmtLine.add(Restrictions.eq(InternalMovementLine.PROPERTY_STORAGEBIN, storageBin));
    if (attributeSetValue == null || attributeSetValue.getId().equals("0")) {
      critThisMvmtLine.add(Restrictions.or(
          Restrictions.isNull(StorageDetail.PROPERTY_ATTRIBUTESETVALUE),
          Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, asiZero)));
    } else {
      critThisMvmtLine.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE,
          attributeSetValue));
    }
    critThisMvmtLine.add(Restrictions.eq(InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST,
        basePicking));
    if (baseGroupPicking == null) {
      critThisMvmtLine
          .add(Restrictions.isNull(InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST));
    } else {
      critThisMvmtLine.add(Restrictions.eq(InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST,
          baseGroupPicking));
    }
    thisMvmtLine = (InternalMovementLine) critThisMvmtLine.uniqueResult();

    OBCriteria<InternalMovementLine> critOtherMvmtLine = OBDal.getInstance().createCriteria(
        InternalMovementLine.class);
    critOtherMvmtLine.add(Restrictions.eq(InternalMovementLine.PROPERTY_STOCKRESERVATION,
        reservation));
    critOtherMvmtLine.add(Restrictions.eq(InternalMovementLine.PROPERTY_STORAGEBIN, storageBin));
    if (attributeSetValue == null || attributeSetValue.getId().equals("0")) {
      critOtherMvmtLine.add(Restrictions.or(
          Restrictions.isNull(StorageDetail.PROPERTY_ATTRIBUTESETVALUE),
          Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, asiZero)));
    } else {
      critOtherMvmtLine.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE,
          attributeSetValue));
    }
    if (thisMvmtLine != null) {
      critOtherMvmtLine
          .add(Restrictions.ne(InternalMovementLine.PROPERTY_ID, thisMvmtLine.getId()));
      otherMvmtLines.addAll(critOtherMvmtLine.list());
    }
    return thisMvmtLine;
  }

}
