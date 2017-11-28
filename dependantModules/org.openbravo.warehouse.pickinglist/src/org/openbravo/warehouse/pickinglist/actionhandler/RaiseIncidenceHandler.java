/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.pickinglist.actionhandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.OBWPL_Utils;
import org.openbravo.warehouse.pickinglist.OutboundPickingListProcess;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.PickingListIncidence;
import org.openbravo.warehouse.pickinglist.PickingListProblem;
import org.openbravo.warehouse.pickinglist.PickingListProblemOrderLines;
import org.openbravo.warehouse.pickinglist.hooks.CreateInventoryHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaiseIncidenceHandler extends BaseProcessActionHandler {
  final private static Logger log = LoggerFactory.getLogger(RaiseIncidenceHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    List<InternalMovementLine> lstMovLines = new ArrayList<InternalMovementLine>();
    JSONObject jsonResponse = new JSONObject();
    try {
      final JSONObject jsonRequest = new JSONObject(content);
      final JSONObject jsonProcessParams = jsonRequest.getJSONObject("_params");
      final JSONArray arrSelectedMovs = jsonProcessParams.getJSONArray("movLines");
      for (int i = 0; i < arrSelectedMovs.length(); i++) {
        String movId = arrSelectedMovs.getString(i);
        InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class, movId);
        // re validate
       if(mvmtLine.getOBWPLItemStatus().equals("PE")){
    	   lstMovLines.add(mvmtLine);   
       } else {
    	   throw new OBException("Incidence can be raised only for pending lines");
       }
        
      }
      
      raiseIncidence(lstMovLines, jsonProcessParams.getString("incidenceType"),
          jsonProcessParams.getString("IncidenceReason"));

      // create the result
      jsonResponse = new JSONObject();
      JSONObject message = new JSONObject();
      message.put("text", "");
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

  public static void raiseIncidence(List<InternalMovementLine> mvmtLines, String incidenceId,
      String strIncidenceReason) {
    PickingListIncidence incidence = OBDal.getInstance().get(PickingListIncidence.class,
        incidenceId);
    int counter = 0;
    PickingListProblem raisedProblem = null;
    for (InternalMovementLine mvmtLine : mvmtLines) {
      if (mvmtLine.getObwplPickinglistproblem() != null) {
        throw new OBException(mvmtLine.getOBWPLWarehousePickingList().getIdentifier()
            + ": Movement " + mvmtLine.getIdentifier() + "[" + mvmtLine.getId()
            + "] is already part of an incidence");
      }
      if (counter == 0) {
        raisedProblem = registerPickListIncidence(incidenceId, strIncidenceReason, mvmtLine, null);
      }
      if (!mvmtLine.getMovementQuantity().equals(mvmtLine.getOBWPLPickedqty())) {
        mvmtLine.setOBWPLIncidenceReason(strIncidenceReason);
        mvmtLine.setOBWPLRaiseIncidence(true);
        mvmtLine.setOBWPLItemStatus("IN");
        // mvmtLine.setOBWPLPickedqty(bdPickedQty);
        mvmtLine.getOBWPLWarehousePickingList().setPickliststatus("IN");
        mvmtLine.setObwplPickinglistproblem(raisedProblem);
        if (mvmtLine.getOBWPLGroupPickinglist() != null) {
          mvmtLine.getOBWPLGroupPickinglist().setPickliststatus("IN");
        } else {
          mvmtLine.getOBWPLWarehousePickingList().setPickliststatus("IN");
        }
        if (counter > 0) {
          raisedProblem = registerPickListIncidence(incidenceId, strIncidenceReason, mvmtLine,
              raisedProblem);
        }

        // TODO: possible hook
        if (incidence.getIncidencetype().equals("OBWPL_AlternateLocationIncidence")) {
          generateAlternateMovements(mvmtLine, raisedProblem);
        } else if (incidence.getIncidencetype().equals("OBWPL_BoxEmptyIncidence")) {
          generateDuplicateMovement(mvmtLine, raisedProblem);
        }
      }
      counter += 1;
    }
  }

  private static void generateDuplicateMovement(InternalMovementLine origMovementLine,
      PickingListProblem raisedProblem) {
    generateAlternateMovements(origMovementLine, raisedProblem);
  }

  public static void generateAlternateMovements(InternalMovementLine origMovement,
      PickingListProblem problem) {
    List<Map<String, Object>> nearestStockInfo = null;
    try {
      nearestStockInfo = OBWPL_Utils.getStockFromNearestBin(origMovement, origMovement
          .getMovementQuantity().subtract(origMovement.getOBWPLPickedqty()));
      for (Map<String, Object> map : nearestStockInfo) {
        StockProposed curStockProposed = (StockProposed) map.get("stockProposed");
        // create movement
        InternalMovement movementHeader = new InternalMovement();
        movementHeader.setClient(origMovement.getMovement().getClient());
        movementHeader.setOrganization(origMovement.getMovement().getOrganization());
        movementHeader.setActive(true);
        movementHeader.setName(origMovement.getMovement().getName());
        movementHeader.setMovementDate(new Date());

        OBDal.getInstance().save(movementHeader);

        InternalMovementLine movementLine = new InternalMovementLine();
        movementLine.setClient(origMovement.getClient());
        movementLine.setOrganization(origMovement.getOrganization());
        movementLine.setActive(true);
        movementLine.setMovement(movementHeader);
        movementLine.setSalesOrderLine(origMovement.getSalesOrderLine());
        movementLine.setStorageBin(curStockProposed.getStorageDetail().getStorageBin());
        movementLine.setNewStorageBin(origMovement.getNewStorageBin());
        movementLine.setLineNo(10L);
        movementLine.setMovementQuantity((BigDecimal) map.get("pickedQty"));
        movementLine.setProduct(curStockProposed.getStorageDetail().getProduct());
        movementLine.setAttributeSetValue(curStockProposed.getStorageDetail()
            .getAttributeSetValue());
        movementLine.setUOM(origMovement.getUOM());
        if (origMovement.getOBWPLGroupPickinglist() != null) {
          movementLine.setOBWPLGroupPickinglist(origMovement.getOBWPLGroupPickinglist());
        }
        movementLine.setOBWPLWarehousePickingList(origMovement.getOBWPLWarehousePickingList());

        movementLine.setStockReservation(origMovement.getStockReservation());
        movementLine.setOBWPLComplete(false);
        movementLine.setOBWPLItemStatus("PE");
        movementLine.setOBWPLEditItem(false);
        movementLine.setOBWPLAllowDelete(false);
        movementLine.setOBWPLRaiseIncidence(false);
        movementLine.setOBWPLIncidenceReason(null);
        movementLine.setOBWPLPickedqty(BigDecimal.ZERO);
        movementLine.setObwplPlproblemgenerator(problem);
        OBDal.getInstance().save(movementLine);
        OBDal.getInstance().flush();

        ReservationUtils.reserveStockManual(origMovement.getStockReservation(),
            ((BaseOBObject) curStockProposed.getStorageDetail()),
            ((BigDecimal) map.get("pickedQty")), "Y");

        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      if (!problem.getObwplPickinglistincidence().getIncidencetype()
          .equals("OBWPL_BoxEmptyIncidence")) {
        log.error("An error happened while movements for alternate location are created: "
            + e.getMessage());
        throw new OBException(e.getMessage());
      }
    }

  }

  public static void resetIncidence(InternalMovementLine mvmtLine) {
    PickingListProblem incidence = mvmtLine.getObwplPickinglistproblem();
    if (incidence.getObwplPickinglistincidence().getIncidencetype()
        .equals("OBWPL_BoxEmptyIncidence")
        & incidence.getStatus().equals("IC")) {
      throw new OBException("Confirmed Box Empty incidences cannot be undone");
    }
    mvmtLine.setOBWPLIncidenceReason(null);
    mvmtLine.setOBWPLRaiseIncidence(false);
    mvmtLine.setOBWPLItemStatus("PE");

    OBWPL_Utils.deleteSolFromPickingListIncidence(incidence, mvmtLine.getStockReservation()
        .getSalesOrderLine());

    // Restore picking previous status
    if (mvmtLine.getOBWPLGroupPickinglist() != null) {
      PickingList groupPL = mvmtLine.getOBWPLGroupPickinglist();
      String strStatus = OutboundPickingListProcess.checkStatus(
          groupPL.getMaterialMgmtInternalMovementLineEMOBWPLGroupPickinglistList(), false);
      groupPL.setPickliststatus(strStatus);
    }
    String strStatus = OutboundPickingListProcess.checkStatus(mvmtLine
        .getOBWPLWarehousePickingList()
        .getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList(), true);
    mvmtLine.getOBWPLWarehousePickingList().setPickliststatus(strStatus);
    // possible hook
    if (incidence.getObwplPickinglistincidence().getIncidencetype()
        .equals("OBWPL_AlternateLocationIncidence")
        || incidence.getObwplPickinglistincidence().getIncidencetype()
            .equals("OBWPL_BoxEmptyIncidence")) {
      // remove generated movs if them have not incidences. If incidences... -> exception
      removeGeneratedMovements(incidence);
    }

    mvmtLine.setObwplPickinglistproblem(null);
    OBDal.getInstance().flush();

    if (OBWPL_Utils.getNumberOfSOlinesRelatedToIncidence(incidence).compareTo(BigDecimal.ZERO) == 0) {
      OBWPL_Utils.deletePickingListIncidence(incidence);
    }
  }

  public static void confirmIncidence(InternalMovementLine mvmtLine) throws JSONException {
    if (mvmtLine.getObwplPickinglistproblem() != null) {
      mvmtLine.setOBWPLItemStatus("IC");
      mvmtLine.setOBWPLRaiseIncidence(false);
      mvmtLine.getObwplPickinglistproblem().setStatus("IC");
      OBDal.getInstance().save(mvmtLine);
      // Restore picking previous status
      if (mvmtLine.getOBWPLGroupPickinglist() != null) {
        PickingList groupPL = mvmtLine.getOBWPLGroupPickinglist();
        String strStatus = OutboundPickingListProcess.checkStatus(
            groupPL.getMaterialMgmtInternalMovementLineEMOBWPLGroupPickinglistList(), false);
        groupPL.setPickliststatus(strStatus);
      }
      String strStatus = OutboundPickingListProcess.checkStatus(mvmtLine
          .getOBWPLWarehousePickingList()
          .getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList(), true);
      mvmtLine.getOBWPLWarehousePickingList().setPickliststatus(strStatus);
      PickingListProblem problem = mvmtLine.getObwplPickinglistproblem();
      if (problem.getObwplPickinglistincidence().getIncidencetype()
          .equals("OBWPL_BoxEmptyIncidence")) {
        updateInventory(mvmtLine);
      }
    }
  }

  private static PickingListProblem registerPickListIncidence(String IncidenceId,
      String description, InternalMovementLine movementLine, PickingListProblem createdIncidence) {

    PickingListProblem plProblem = null;
    OrderLine sol = null;

    sol = movementLine.getStockReservation().getSalesOrderLine();
    // Possible improvement.
    // Detect when the incidence is the same. Instead of create a new incidence, just connect the
    // movement to that incidence.
    // Doing it we will need a new table to list N sales orders afected by this new
    // "grouped incidence", because each movement is connected to a different SO

    if (createdIncidence == null) {
      PickingListIncidence incidenceType = OBDal.getInstance().get(PickingListIncidence.class,
          IncidenceId);

      plProblem = new PickingListProblem();

      plProblem.setObwplPickinglistincidence(incidenceType);
      if (movementLine.getOBWPLGroupPickinglist() != null) {
        plProblem.setObwplGroupPickinglist(movementLine.getOBWPLGroupPickinglist());
        plProblem.setClient(movementLine.getOBWPLGroupPickinglist().getClient());
        plProblem.setOrganization(movementLine.getOBWPLGroupPickinglist().getOrganization());
      } else {
        plProblem.setObwplPickinglist(movementLine.getOBWPLWarehousePickingList());
        plProblem.setClient(movementLine.getOBWPLWarehousePickingList().getClient());
        plProblem.setOrganization(movementLine.getOBWPLWarehousePickingList().getOrganization());
      }
      plProblem.setDescription(description);
      plProblem.setOriginalqty(movementLine.getMovementQuantity());
      plProblem.setPickedqty(movementLine.getOBWPLPickedqty());
      plProblem.setStatus("IN");
      OBDal.getInstance().save(plProblem);
    } else {
      plProblem = createdIncidence;
      plProblem.setOriginalqty(plProblem.getOriginalqty().add(movementLine.getMovementQuantity()));
      plProblem.setPickedqty(plProblem.getPickedqty().add(movementLine.getOBWPLPickedqty()));
    }

    PickingListProblemOrderLines plProblemSol = new PickingListProblemOrderLines();
    plProblemSol.setClient(movementLine.getClient());
    plProblemSol.setOrganization(movementLine.getOrganization());
    plProblemSol.setPickingListProblem(plProblem);
    plProblemSol.setOrderline(sol);
    OBDal.getInstance().save(plProblemSol);

    // TODO: Remove? not needed because link is done through table plproblemsols
    // sol.setObwplPickinglistproblem(plProblem);
    // OBDal.getInstance().save(sol);

    movementLine.setObwplPickinglistproblem(plProblem);
    OBDal.getInstance().save(movementLine);

    return plProblem;
  }

  private static void removeGeneratedMovements(PickingListProblem incidence) {
    if (OBWPL_Utils.getNumberOfGeneratedMovementsNonDeleteables(incidence) > 0) {
      throw new OBException(
          "This incidence cannot be undo because generated movements are not in pending status");
    } else {
      OBWPL_Utils.deleteGeneratedMovements(incidence);
    }
  }

  public static void updateInventory(InternalMovementLine mvmtLine) throws JSONException {
    BigDecimal inventoryCount = BigDecimal.ZERO;
    undoReservationForMovementLine(mvmtLine);
    mvmtLine.setMovementQuantity(mvmtLine.getOBWPLPickedqty());
    List<ReservationStock> reservationStockList = getRelatedReserves(mvmtLine);
    for (ReservationStock reservationStock : reservationStockList) {
      InternalMovementLine movementLine = getRelatedPickingLine(reservationStock, mvmtLine);
      if (movementLine != null) {
        inventoryCount = inventoryCount.add(movementLine.getMovementQuantity());
      } else {
        Reservation reservation = reservationStock.getReservation();
        reservation.getMaterialMgmtReservationStockList().remove(reservationStock);
        OBDal.getInstance().remove(reservationStock);
        OBDal.getInstance().save(reservation);
      }
    }
    OBDal.getInstance().flush();
    InventoryCount inventory = createInventory(mvmtLine, inventoryCount);
    processInventory(inventory);
  }

  private static InternalMovementLine getRelatedPickingLine(ReservationStock reservationStock,
      InternalMovementLine mvmtLine) {

    StringBuffer where = new StringBuffer();
    where.append(" as ml");
    where.append(" where ml." + InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST
        + " is not null");
    where.append("   and ml." + InternalMovementLine.PROPERTY_PRODUCT + " = :product");
    where.append("   and ml." + InternalMovementLine.PROPERTY_STORAGEBIN + " = :storageBin");
    where.append("   and ml." + InternalMovementLine.PROPERTY_ATTRIBUTESETVALUE + " = :attribute");
    where.append("   and ml." + InternalMovementLine.PROPERTY_STOCKRESERVATION + " = :reservation");
    where.append("   and ml." + InternalMovementLine.PROPERTY_MOVEMENTQUANTITY + " <> 0");

    OBQuery<InternalMovementLine> qry = OBDal.getInstance().createQuery(InternalMovementLine.class,
        where.toString());
    qry.setNamedParameter("product", mvmtLine.getProduct());
    qry.setNamedParameter("storageBin", mvmtLine.getStorageBin());
    qry.setNamedParameter("attribute", mvmtLine.getAttributeSetValue());
    qry.setNamedParameter("reservation", reservationStock.getReservation());

    return (InternalMovementLine) qry.uniqueResult();
  }

  private static List<ReservationStock> getRelatedReserves(InternalMovementLine mvmtLine) {
    StringBuffer where = new StringBuffer();
    where.append(" as rs");
    where.append("   join rs." + ReservationStock.PROPERTY_RESERVATION + " as r");
    where.append(" where r." + Reservation.PROPERTY_PRODUCT + " = :product");
    where.append("   and rs." + ReservationStock.PROPERTY_STORAGEBIN + " = :storageBin");
    where.append("   and rs." + ReservationStock.PROPERTY_ATTRIBUTESETVALUE + " = :attribute");
    where.append("   and r." + Reservation.PROPERTY_RESSTATUS + " NOT IN ('DR','CL')");
    where.append("   and (rs." + ReservationStock.PROPERTY_QUANTITY + " - rs."
        + ReservationStock.PROPERTY_RELEASED + " <> 0)");

    OBQuery<ReservationStock> qry = OBDal.getInstance().createQuery(ReservationStock.class,
        where.toString());
    qry.setNamedParameter("product", mvmtLine.getProduct());
    qry.setNamedParameter("storageBin", mvmtLine.getStorageBin());
    qry.setNamedParameter("attribute", mvmtLine.getAttributeSetValue());

    return qry.list();
  }

  private static void undoReservationForMovementLine(InternalMovementLine mvmtLine) {
    StorageDetail sd = OBWPL_Utils.getStorageDetailFromMovLine(mvmtLine);
    ReservationUtils.reserveStockManual(mvmtLine.getStockReservation(), sd,
        (mvmtLine.getMovementQuantity().subtract(mvmtLine.getOBWPLPickedqty())).negate(), "Y");
  }

  private static void processInventory(InventoryCount inventory) throws JSONException {
    OBContext.setAdminMode(true);
    try {
      // lock inventory
      if (inventory.isProcessNow()) {
        throw new OBException(OBMessageUtils.parseTranslation("@OtherProcessActive@"));
      }
      inventory.setProcessNow(true);
      OBDal.getInstance().flush();

      log.warn("Inventory " + inventory.getIdentifier() + "[" + inventory.getId()
          + "] created. Processing it");

      OBError msg = new InventoryCountProcess().processInventory(inventory);
      inventory.setProcessNow(false);

      OBDal.getInstance().flush();

      log.warn("Inventory " + inventory.getIdentifier() + "[" + inventory.getId() + "] Processed");
    } catch (Exception e) {
      log.error("Error captured while processing a picking action: " + e.getMessage());
      throw new JSONException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private static InventoryCount createInventory(InternalMovementLine mvmtLine,
      BigDecimal inventoryCount) {
    InventoryCount inventory = OBProvider.getInstance().get(InventoryCount.class);
    inventory.setActive(true);
    inventory.setClient(mvmtLine.getClient());
    inventory.setOrganization(mvmtLine.getOrganization());
    inventory.setMovementDate(new Date());
    inventory.setName("Box Emtpy Incidence " + new Date().toString());
    inventory.setWarehouse(mvmtLine.getStorageBin().getWarehouse());
    inventory.setDescription("BE incidence. for mov " + mvmtLine.getId() + " from "
        + mvmtLine.getOBWPLWarehousePickingList().getIdentifier());

    try {
      executeCreateInventoryHook(mvmtLine, inventory);
    } catch (Exception e) {
      log.error("An error happened when CreateInventoryHook was executed.", e.getMessage(),
          e.getStackTrace());
    }

    OBDal.getInstance().save(inventory);

    InventoryCountLine inventoryLine = OBProvider.getInstance().get(InventoryCountLine.class);
    inventoryLine.setActive(true);
    inventoryLine.setClient(mvmtLine.getClient());
    inventoryLine.setOrganization(mvmtLine.getOrganization());
    inventoryLine.setAttributeSetValue(mvmtLine.getAttributeSetValue());
    OBDal.getInstance().flush();
    inventoryLine.setBookQuantity(getAcutalQuantity(mvmtLine.getProduct(),
        mvmtLine.getAttributeSetValue(), mvmtLine.getStorageBin(), mvmtLine.getUOM()));
    inventoryLine.setQuantityCount(inventoryCount);
    inventoryLine.setProduct(mvmtLine.getProduct());
    inventoryLine.setStorageBin(mvmtLine.getStorageBin());
    inventoryLine.setUOM(mvmtLine.getUOM());
    inventoryLine.setPhysInventory(inventory);
    OBDal.getInstance().save(inventoryLine);

    inventory.getMaterialMgmtInventoryCountLineList().add(inventoryLine);
    OBDal.getInstance().save(inventory);

    return inventory;
  }

  private static BigDecimal getAcutalQuantity(Product product,
      AttributeSetInstance attributeSetValue, Locator storageBin, UOM uom) {

    OBCriteria<StorageDetail> obc = OBDal.getInstance().createCriteria(StorageDetail.class);
    obc.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
    obc.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, storageBin));
    obc.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, attributeSetValue));
    obc.add(Restrictions.eq(StorageDetail.PROPERTY_UOM, uom));

    StorageDetail storageDetail = (StorageDetail) obc.uniqueResult();

    if (storageDetail != null) {
      OBDal.getInstance().getSession().refresh(storageDetail);
      return storageDetail.getQuantityOnHand();
    } else {
      return BigDecimal.ZERO;
    }
  }

  protected static void executeCreateInventoryHook(InternalMovementLine mov,
      InventoryCount inventory) throws Exception {
    Set<Bean<?>> beansSet = WeldUtils.getStaticInstanceBeanManager().getBeans(
        CreateInventoryHook.class);
    List<Bean<?>> beansList = new ArrayList<Bean<?>>();
    beansList.addAll(beansSet);
    for (Bean<?> abstractBean : beansList) {
      CreateInventoryHook hook = (CreateInventoryHook) WeldUtils.getStaticInstanceBeanManager()
          .getReference(abstractBean, CreateInventoryHook.class,
              WeldUtils.getStaticInstanceBeanManager().createCreationalContext(abstractBean));
      hook.exec(mov, inventory);
    }
  }
}
