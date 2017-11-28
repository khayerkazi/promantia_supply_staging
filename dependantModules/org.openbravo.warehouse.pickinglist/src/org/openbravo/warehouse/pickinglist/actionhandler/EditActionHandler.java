/************************************************************************************
 * Copyright (C) 2012-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.pickinglist.actionhandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.PickinglistManualPickEdit;

public class EditActionHandler extends BaseProcessActionHandler {
  private static Logger log = Logger.getLogger(EditActionHandler.class);
  final String strInoutLineTableId = "320";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      final String strCOrderlineId = jsonRequest.getString("inpcOrderlineId");

      final String strPickingListId = jsonRequest.getString("Obwpl_Pickinglist_ID");
      PickingList picking = OBDal.getInstance().get(PickingList.class, strPickingListId);

      if (picking != null) {
        List<String> idList = new ArrayList<String>();
        for (ShipmentInOutLine inoutline : picking
            .getMaterialMgmtShipmentInOutLineEMObwplPickinglistIDList()) {
          if (strCOrderlineId.equals(inoutline.getSalesOrderLine().getId())) {
            idList.add(inoutline.getId());
          }
        }
        manageInOutLines(jsonRequest, picking, idList);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OBDal.getInstance().rollbackAndClose();

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

  private void manageInOutLines(JSONObject jsonRequest, PickingList picking, List<String> idList)
      throws JSONException {
    JSONArray selectedLines = jsonRequest.getJSONArray("_selection");
    // if no lines selected don't do anything.
    if (selectedLines.length() == 0) {
      setQuantityToZero(idList);
      return;
    }
    // All picking list lines belong to same shipment.
    ShipmentInOut mInOut = OBDal.getInstance().get(ShipmentInOutLine.class, idList.get(0))
        .getShipmentReceipt();
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject(i);
      log.debug(selectedLine);
      ShipmentInOutLine inoutLine = null;
      String strInOutLineId = selectedLine
          .get(PickinglistManualPickEdit.PROPERTY_GOODSSHIPMENTLINE).equals(null) ? ""
          : selectedLine.getString(PickinglistManualPickEdit.PROPERTY_GOODSSHIPMENTLINE);
      boolean existsShipmentLine = StringUtils.isNotBlank(strInOutLineId);

      final String strOrderLineId = selectedLine.getString(
          PickinglistManualPickEdit.PROPERTY_SALESORDERLINE).equals(null) ? "" : selectedLine
          .getString(PickinglistManualPickEdit.PROPERTY_SALESORDERLINE);
      OrderLine ol = OBDal.getInstance().get(OrderLine.class, strOrderLineId);

      if (existsShipmentLine) {
        inoutLine = OBDal.getInstance().get(ShipmentInOutLine.class, strInOutLineId);
        idList.remove(strInOutLineId);
      } else {
        inoutLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
        inoutLine.setObwplPickinglist(picking);
        inoutLine.setMovementQuantity(BigDecimal.ZERO);
        inoutLine.setOrganization(picking.getOrganization());
        inoutLine.setClient(picking.getClient());
        inoutLine.setShipmentReceipt(mInOut);
        inoutLine.setSalesOrderLine(ol);
        inoutLine.setProduct(ol.getProduct());
        inoutLine.setUOM(ol.getUOM());

        // get Max(LineNo)
        final OBCriteria<ShipmentInOutLine> iol = OBDal.getInstance().createCriteria(
            ShipmentInOutLine.class);
        iol.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_OBWPLPICKINGLIST, picking));
        iol.addOrderBy(ShipmentInOutLine.PROPERTY_LINENO, false);
        iol.setMaxResults(1);
        final List<ShipmentInOutLine> sp = iol.list();
        inoutLine.setLineNo(sp.get(0).getLineNo() + 10L);

        final String strLocator = selectedLine.get(PickinglistManualPickEdit.PROPERTY_STORAGEBIN)
            .equals(null) ? "" : selectedLine
            .getString(PickinglistManualPickEdit.PROPERTY_STORAGEBIN);
        inoutLine.setStorageBin((Locator) OBDal.getInstance().getProxy(Locator.ENTITY_NAME,
            strLocator));
        final String strASIId = selectedLine.get(
            PickinglistManualPickEdit.PROPERTY_ATTRIBUTESETVALUE).equals(null) ? "" : selectedLine
            .getString(PickinglistManualPickEdit.PROPERTY_ATTRIBUTESETVALUE);
        inoutLine.setAttributeSetValue((AttributeSetInstance) OBDal.getInstance().getProxy(
            AttributeSetInstance.ENTITY_NAME, StringUtils.isNotBlank(strASIId) ? strASIId : "0"));
      }

      final BigDecimal qty = new BigDecimal(
          selectedLine.getString(PickinglistManualPickEdit.PROPERTY_QUANTITY));

      // Update or create reservation
      StorageDetail sd = OBDal.getInstance().get(StorageDetail.class,
          selectedLine.getString(PickinglistManualPickEdit.PROPERTY_STORAGEDETAIL));
      Reservation reservation = ReservationUtils.getReservationFromOrder(ol);
      ReservationUtils.reserveStockManual(reservation, sd,
          qty.subtract(inoutLine.getMovementQuantity()), "Y");

      inoutLine.setMovementQuantity(qty);

      OBDal.getInstance().save(inoutLine);
      OBDal.getInstance().flush();
    }

    removeNonSelectedLines(idList, picking);
  }

  private void setQuantityToZero(List<String> idList) {
    Reservation reservation;
    StorageDetail sd;
    if (idList.size() > 0) {
      for (String id : idList) {
        ShipmentInOutLine inoutLine = OBDal.getInstance().get(ShipmentInOutLine.class, id);
        reservation = ReservationUtils.getReservationFromOrder(inoutLine.getSalesOrderLine());
        sd = getStorageDetail(inoutLine.getProduct(), inoutLine.getAttributeSetValue(),
            inoutLine.getStorageBin());
        ReservationUtils.reserveStockManual(reservation, sd, inoutLine.getMovementQuantity()
            .negate(), "Y");
        inoutLine.setMovementQuantity(BigDecimal.ZERO);
      }
      OBDal.getInstance().flush();
    }
  }

  private void removeNonSelectedLines(List<String> idList, PickingList picking) {
    if (idList.size() > 0) {
      StorageDetail sd;
      Reservation reservation;
      List<ReservationStock> listResStock = new ArrayList<ReservationStock>();
      for (String id : idList) {
        ShipmentInOutLine inoutLine = OBDal.getInstance().get(ShipmentInOutLine.class, id);
        sd = getStorageDetail(inoutLine.getProduct(), inoutLine.getAttributeSetValue(),
            inoutLine.getStorageBin());
        reservation = ReservationUtils.getReservationFromOrder(inoutLine.getSalesOrderLine());
        listResStock.add(ReservationUtils.reserveStockManual(reservation, sd, inoutLine
            .getMovementQuantity().negate(), "Y"));
        picking.getOBWPLPickinglistManualPickEditList().remove(inoutLine);
        OBDal.getInstance().remove(inoutLine);
      }
      if (!listResStock.isEmpty()) {
        for (ReservationStock resStock : listResStock) {
          if (resStock.getQuantity().equals(BigDecimal.ZERO)) {
            OBDal.getInstance().remove(resStock);
          }
        }
      }
      OBDal.getInstance().save(picking);
      OBDal.getInstance().flush();
    }
  }

  private StorageDetail getStorageDetail(Product product, AttributeSetInstance attributeSetInst,
      Locator storageBin) {
    OBCriteria<StorageDetail> sdCriteria = OBDal.getInstance().createCriteria(StorageDetail.class);
    sdCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
    sdCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, attributeSetInst));
    sdCriteria.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, storageBin));
    sdCriteria.add(Restrictions.isNull(StorageDetail.PROPERTY_ORDERUOM));
    sdCriteria.setMaxResults(1);
    return (StorageDetail) sdCriteria.uniqueResult();
  }
}
