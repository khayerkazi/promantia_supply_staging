/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.warehouse.pickinglist.MovementLineHandler;
import org.openbravo.warehouse.pickinglist.OBWPL_PickingBoxesUtils;
import org.openbravo.warehouse.pickinglist.OutboundPickingListProcess;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.PickingListBox;
import org.openbravo.warehouse.pickinglist.actionhandler.RaiseIncidenceHandler;

public class ProcessPicking extends WarehouseJSONProcess {
  private static final Logger log = Logger.getLogger(ProcessPicking.class);

  @Override
  protected String getProperty() {
    return WHPickingConstants.PICKING_PROPERTY;
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    long t0 = System.currentTimeMillis();
    Boolean responseData = false;
    JSONObject jsonResponseData = new JSONObject();
    OBContext.setAdminMode(true);

    try {
      final String action = jsonsent.getString("action");

      JSONArray items = null;
      if (jsonsent.has("items")) {
        items = jsonsent.getJSONArray("items");
      }
      if ("incidence".equals(action)) {
        final String incidenceId = jsonsent.getJSONObject("incidence").getString("id");
        final String incidenceDescription = jsonsent.getJSONObject("incidence").getString(
            "description");
        final int neededQty = jsonsent.getInt("neededQty");
        final int pickedQty = jsonsent.getInt("pickedQty");
        raiseIncidence(incidenceId, incidenceDescription, items, neededQty, pickedQty);
      } else if ("resetincidence".equals(action)) {
        resetIncidence(items);
      } else if ("confirmincidence".equals(action)) {
        confirmIncidence(items);
      } else if ("process".equals(action)) {
        processItems(items);
      } else if ("confirm".equals(action)) {
        String pickingId = jsonsent.getString("pickingId");
        confirmPicking(pickingId);
      } else if ("addBox".equals(action)) {
        String pickingId = jsonsent.getString("pickingId");
        // galboxes
        // if (Preferences.getPreferenceValue("OBWPACK_AutoPackingWhenPicking", true,
        // OBContext.getOBContext().getCurrentClient(),
        // OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        // OBContext.getOBContext().getRole(), null).equals("Y")) {
        if (true) {
          JSONArray jsonCreatedBoxes = new JSONArray();
          List<PickingListBox> createdBoxes = OBWPL_PickingBoxesUtils.CreatePickingListBox(
              pickingId, items);
          DataToJsonConverter jsonConverter = new DataToJsonConverter();
          for (PickingListBox packingBox : createdBoxes) {
            jsonCreatedBoxes.put(jsonConverter.toJsonObject(packingBox, DataResolvingMode.FULL));
          }
          responseData = true;
          jsonResponseData.put("boxes", jsonCreatedBoxes);
        }
      }

      OBDal.getInstance().flush();

      long t1 = System.currentTimeMillis();
      log.debug("time to create the document: " + (t1 - t0));

      JSONObject result = new JSONObject();
      if (responseData) {
        result.put("data", jsonResponseData);
      }
      result.put("status", 0);
      result.put("message", OBMessageUtils.messageBD("success"));

      long t2 = System.currentTimeMillis();
      log.debug("time to process: " + (t2 - t1));
      log.debug(" TOTAL time: " + (t2 - t0));

      return result;
    } catch (Exception e) {
      log.error("Error captured while processing a picking action: " + e.getMessage());
      throw new JSONException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void raiseIncidence(String incidenceId, String incidenceDescription, JSONArray items,
      int neededQty, int totalPickedQty) throws JSONException {
    PickingList pickList = null;
    BigDecimal tmpPickedQty = new BigDecimal(totalPickedQty);
    BigDecimal incidencePickedQty = BigDecimal.ZERO;
    if (items.length() > 1) {
      List<InternalMovementLine> lstMovsIntoIncidence = new ArrayList<InternalMovementLine>();
      StringBuilder mvmntsIdsStrBuilder = new StringBuilder();
      Map<String, Object> movLineIdJsonObjMap = new HashMap<String, Object>();
      for (int j = 0; j < items.length(); j++) {
        movLineIdJsonObjMap.put(items.getJSONObject(j).getString("id"), items.getJSONObject(j));
        mvmntsIdsStrBuilder.append("'" + items.getJSONObject(j).getString("id"));
        if (j == items.length() - 1) {
          mvmntsIdsStrBuilder.append("'");
        } else {
          mvmntsIdsStrBuilder.append("',");
        }
      }

      // sort by sorder date
      StringBuffer sortedMovsStrQuery = new StringBuffer();
      sortedMovsStrQuery.append("FROM " + InternalMovementLine.ENTITY_NAME + " e WHERE ");
      sortedMovsStrQuery.append("e.id IN (");
      sortedMovsStrQuery.append(mvmntsIdsStrBuilder.toString());
      sortedMovsStrQuery.append(") ORDER BY ");
      sortedMovsStrQuery.append("e.salesOrderLine.salesOrder.orderDate ASC");

      Query sortedMovsQuery = OBDal.getInstance().getSession()
          .createQuery(sortedMovsStrQuery.toString());
      ScrollableResults movsScroll = sortedMovsQuery.scroll();
      while (movsScroll.next()) {
        InternalMovementLine mvmtLine = (InternalMovementLine) movsScroll.get(0);

        if (mvmtLine.getOBWPLGroupPickinglist() != null) {
          pickList = mvmtLine.getOBWPLGroupPickinglist();
        } else {
          pickList = mvmtLine.getOBWPLWarehousePickingList();
        }
        if (mvmtLine.getMovementQuantity().compareTo(tmpPickedQty) <= 0) {
          mvmtLine.setOBWPLPickedqty(mvmtLine.getMovementQuantity());
          if (pickList.isUsePickingBoxes()) {
            // update box content
            if (((JSONObject) movLineIdJsonObjMap.get(mvmtLine.getId())).has("boxes")) {
              OBWPL_PickingBoxesUtils.CreateOrUpdateBoxContent(mvmtLine.getId(), pickList.getId(),
                  ((JSONObject) movLineIdJsonObjMap.get(mvmtLine.getId())).getJSONObject("boxes"));
            }
          }
          tmpPickedQty = tmpPickedQty.subtract(mvmtLine.getMovementQuantity());
          try {
            MovementLineHandler.processMovementLine(mvmtLine.getId(), "CF",
                mvmtLine.getMovementQuantity(), "process");
          } catch (PropertyException e) {
            String message = "Error executing MovementLineHandler.processMovementLine: "
                + e.getMessage();
            log.error(message);
            throw new JSONException(message);
          }
        } else {
          mvmtLine.setOBWPLPickedqty(tmpPickedQty);
          if (pickList.isUsePickingBoxes()) {
            // update box content
            if (((JSONObject) movLineIdJsonObjMap.get(mvmtLine.getId())).has("boxes")) {
              OBWPL_PickingBoxesUtils.CreateOrUpdateBoxContent(mvmtLine.getId(), pickList.getId(),
                  ((JSONObject) movLineIdJsonObjMap.get(mvmtLine.getId())).getJSONObject("boxes"));
            }
          }
          incidencePickedQty = incidencePickedQty.add(tmpPickedQty);
          tmpPickedQty = tmpPickedQty.subtract(mvmtLine.getOBWPLPickedqty());
          lstMovsIntoIncidence.add(mvmtLine);
        }
        OBDal.getInstance().save(mvmtLine);
      }
      RaiseIncidenceHandler.raiseIncidence(lstMovsIntoIncidence, incidenceId, incidenceDescription);
      String prefValue = "N";
      try {
        prefValue = Preferences.getPreferenceValue("OBWPL_autoConfirmIncidences", true, OBContext
            .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
            OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
      } catch (Exception E) {
        prefValue = "N";
      }

      if ("Y".equals(prefValue)) {
        log.warn("Incidence will be confirmed automatically because preference -OBWPL_autoConfirmIncidences- is enabled");
        OBDal.getInstance().flush();
        // raise incidence process has been created successfully
        this.confirmIncidence(items);
      }
    } else {
      List<InternalMovementLine> lstMovsIntoIncidence = new ArrayList<InternalMovementLine>();
      String mvtId = items.getJSONObject(0).getString("id");
      InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class, mvtId);
      if (mvmtLine.getOBWPLGroupPickinglist() != null) {
        pickList = mvmtLine.getOBWPLGroupPickinglist();
      } else {
        pickList = mvmtLine.getOBWPLWarehousePickingList();
      }
      mvmtLine.setOBWPLPickedqty(tmpPickedQty);
      if (pickList.isUsePickingBoxes()) {
        // update box content
        if (items.getJSONObject(0).has("boxes")) {
          OBWPL_PickingBoxesUtils.CreateOrUpdateBoxContent(mvtId, pickList.getId(), items
              .getJSONObject(0).getJSONObject("boxes"));
        }
      }
      lstMovsIntoIncidence.add(mvmtLine);
      RaiseIncidenceHandler.raiseIncidence(lstMovsIntoIncidence, incidenceId, incidenceDescription);
      OBDal.getInstance().save(mvmtLine);
      String prefValue = "N";
      try {
        prefValue = Preferences.getPreferenceValue("OBWPL_autoConfirmIncidences", true, OBContext
            .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
            OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
      } catch (Exception E) {
        prefValue = "N";
      }
      if ("Y".equals(prefValue)) {
        log.warn("Incidence will be confirmed automatically because preference -OBWPL_autoConfirmIncidences- is enabled");
        OBDal.getInstance().flush();
        // raise incidence process has been created successfully
        this.confirmIncidence(items);
      }
    }
  }

  private void resetIncidence(JSONArray items) throws JSONException {
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.getJSONObject(i);
      InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class,
          item.getString("id"));
      RaiseIncidenceHandler.resetIncidence(mvmtLine);
      OBDal.getInstance().save(mvmtLine);
    }
  }

  private void confirmIncidence(JSONArray items) throws JSONException {
    log.warn("The process to confirm the incidences [" + items.length() + " items] has started");
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.getJSONObject(i);
      InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class,
          item.getString("id"));
      RaiseIncidenceHandler.confirmIncidence(mvmtLine);
      
      if((mvmtLine.getMovementQuantity() == mvmtLine.getOBWPLPickedqty()) 
    		  && (mvmtLine.getMovementQuantity().compareTo(BigDecimal.ZERO) > 0) 
    		  && ("IC".equals(mvmtLine.getOBWPLItemStatus()))){
    	
    	  OBWPL_PickingBoxesUtils.CreateOrUpdateBoxContent(mvmtLine.getId(), mvmtLine.getOBWPLWarehousePickingList().getId(),item.getJSONObject("boxes"));
      
      }
      
      log.warn("The process to confirm the incidence for movement " + mvmtLine.getIdentifier()
          + "[" + mvmtLine.getId() + "] has finished");
      OBDal.getInstance().save(mvmtLine);
    }
  }

  private void processItems(JSONArray items) throws JSONException {

    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.getJSONObject(i);
      final String strMvmtLineId = item.getString("id");
      final InternalMovementLine movLine = OBDal.getInstance().get(InternalMovementLine.class,
          strMvmtLineId);
      final String strStatus = item.getString("oBWPLItemStatus");
      final BigDecimal pickedQty = new BigDecimal(item.getInt("oBWPLPickedqty"));
      if (item.has("boxes")) {
        PickingList pickingList = null;
        if (movLine.getOBWPLGroupPickinglist() != null) {
          pickingList = movLine.getOBWPLGroupPickinglist();
        } else {
          pickingList = movLine.getOBWPLWarehousePickingList();
        }
        if (pickingList.isUsePickingBoxes()) {
          String pickingListId = pickingList.getId();
          OBWPL_PickingBoxesUtils.CreateOrUpdateBoxContent(strMvmtLineId, pickingListId,
              item.getJSONObject("boxes"));
        }
      }
      try {
        MovementLineHandler.processMovementLine(strMvmtLineId, strStatus, pickedQty, "process");
      } catch (PropertyException e) {
        log.warn("Error happened processing movLine id " + strMvmtLineId + ". " + e.getMessage()
            + ". " + e.getStackTrace() + ".");
        throw new JSONException("Error happened processing movLine id " + strMvmtLineId + ". "
            + e.getMessage());
      }
    }
  }

  private void confirmPicking(String pickingId) throws JSONException {
    OBError closeResult = null;
    PickingList plToConfirm = OBDal.getInstance().get(PickingList.class, pickingId);
    String prefValue = "N";
    try {
      prefValue = Preferences.getPreferenceValue("OBWPL_AutoClose", true, OBContext.getOBContext()
          .getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(), OBContext
          .getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    } catch (Exception e) {
      prefValue = "N";
    }
    String strStatus = OutboundPickingListProcess.checkStatus(
        plToConfirm.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList(), true);
    if (prefValue.equals("Y") && strStatus.equals("CO")) {
      closeResult = OutboundPickingListProcess.close(plToConfirm);
      if (closeResult.getType().toLowerCase().equals("error")) {
        throw new OBException(closeResult.getMessage());
      }
    }
  }

}
