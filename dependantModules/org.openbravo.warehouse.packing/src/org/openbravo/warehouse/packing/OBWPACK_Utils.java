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
package org.openbravo.warehouse.packing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.warehouse.packing.hooks.ValidateBoxSerialNumberHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OBWPACK_Utils {
  final private static Logger log = LoggerFactory.getLogger(OBWPACK_Utils.class);

  public static List<PackingBox> CreateBoxFromPicking(String packingId, JSONArray boxesToAdd) {
    Packing parentPacking = OBDal.getInstance().get(Packing.class, packingId);
    List<PackingBox> createdBoxes = new ArrayList<PackingBox>();
    Boolean trackingNumberValidated = true;
    String hookMessageResponse = "";
    Set<Bean<?>> beansSet = WeldUtils.getStaticInstanceBeanManager().getBeans(
        ValidateBoxSerialNumberHook.class);
    List<Bean<?>> beansList = new ArrayList<Bean<?>>();
    beansList.addAll(beansSet);
    try {
      for (int i = 0; i < boxesToAdd.length(); i++) {
        JSONObject dataForNewBox = boxesToAdd.getJSONObject(i);

        Long boxNo = 1L;
        if (dataForNewBox.has("boxNo")) {
          boxNo = dataForNewBox.getLong("boxNo");
        } else {
          // TODO: get next box No
          OBCriteria<PackingBox> boxesCrit = OBDal.getInstance().createCriteria(PackingBox.class);
          boxesCrit.add(Restrictions.eq("obwpackPackingh.id", packingId));

          List<PackingBox> packingBoxResult = boxesCrit.list();
          if (packingBoxResult != null & packingBoxResult.size() > 0) {
            boxNo = new Long(packingBoxResult.size() + 1);
          } else {
            boxNo = 1L;
          }
        }
        String trackingNo = dataForNewBox.getString("trackingNo");

        for (Bean<?> abstractBean : beansList) {
          ValidateBoxSerialNumberHook hook = (ValidateBoxSerialNumberHook) WeldUtils
              .getStaticInstanceBeanManager().getReference(abstractBean,
                  ValidateBoxSerialNumberHook.class,
                  WeldUtils.getStaticInstanceBeanManager().createCreationalContext(abstractBean));
          try {
            OBError result = hook.exec(trackingNo, parentPacking);
            if (result.getType().equals("error")) {
              trackingNumberValidated = false;
              hookMessageResponse = result.getMessage();
            }
          } catch (Exception e) {
            log.error("An error happened when ValidateBoxSerialNumberHook was executed.",
                e.getMessage(), e.getStackTrace());
          }
        }

        if (trackingNumberValidated == false) {
          throw new OBException("Box serial number is not valid: " + hookMessageResponse);
        }

        PackingBox newBox = new PackingBox();
        newBox.setClient(parentPacking.getClient());
        newBox.setOrganization(parentPacking.getOrganization());
        newBox.setBoxNumber(boxNo);
        newBox.setTrackingNo(trackingNo);
        newBox.setUOM(parentPacking.getUOM());
        newBox.setObwpackPackingh(parentPacking);

        newBox.setNewOBObject(true);

        OBDal.getInstance().save(newBox);
        createdBoxes.add(newBox);
        if (parentPacking.getTotalboxes() != null) {
          parentPacking.setTotalboxes(parentPacking.getTotalboxes() + 1);
        } else {
          parentPacking.setTotalboxes(0L);
        }
      }
    } catch (Exception e) {
      throw new OBException("An error happened while creating boxes. " + e.getMessage());
    }
    return createdBoxes;
  }

  public static void CreateOrUpdateBoxContent(String movId, String packinghId, JSONObject boxContent) {
    try {
      InternalMovementLine movLine = OBDal.getInstance().get(InternalMovementLine.class, movId);
      Packing packing = OBDal.getInstance().get(Packing.class, packinghId);
      @SuppressWarnings("unchecked")
      Iterator<String> boxesIds = boxContent.keys();
      while (boxesIds.hasNext()) {
        String boxId = (String) boxesIds.next();

        OBCriteria<MovLineBox> movLineCrit = OBDal.getInstance().createCriteria(MovLineBox.class);
        movLineCrit.add(Restrictions.eq(MovLineBox.PROPERTY_MOVEMENTLINE + ".id", movLine.getId()));
        movLineCrit.add(Restrictions.eq(MovLineBox.PROPERTY_PACKINGBOX + ".id", boxId));

        movLineCrit.setMaxResults(1);

        List<MovLineBox> lstMovLineBox = movLineCrit.list();
        MovLineBox mlb = null;

        if (lstMovLineBox.size() == 1) {
          mlb = lstMovLineBox.get(0);
          if (boxContent.getLong(boxId) == 0L) {
            OBDal.getInstance().remove(mlb);
          } else {
            mlb.setQuantity(new BigDecimal(boxContent.getLong(boxId)));
            OBDal.getInstance().save(mlb);
          }
        } else {
          mlb = new MovLineBox();
          mlb.setClient(packing.getClient());
          mlb.setOrganization(packing.getOrganization());
          mlb.setMovementLine(movLine);
          if (movLine.getStockReservation() != null) {
            if (movLine.getStockReservation().getSalesOrderLine() != null) {
              mlb.setSalesOrderLine(movLine.getStockReservation().getSalesOrderLine());
            }
          }
          mlb.setProduct(movLine.getProduct());
          if (movLine.getAttributeSetValue() != null) {
            mlb.setAttributeSetValue(movLine.getAttributeSetValue());
          }
          mlb.setPackingBox(OBDal.getInstance().get(PackingBox.class, boxId));
          mlb.setQuantity(new BigDecimal(boxContent.getLong(boxId)));
          mlb.setNewOBObject(true);
          OBDal.getInstance().save(mlb);
        }
      }
    } catch (Exception e) {
      throw new OBException("An error happened while adding content to pack: " + e.getMessage());
    }
  }

  public static void deletePickingBoxContent(PackingBox box) {
    OBContext.setAdminMode(true);
    try {
      StringBuffer removeBoxProductsStrQuery = new StringBuffer();
      removeBoxProductsStrQuery.append("delete FROM " + MovLineBox.ENTITY_NAME + " WHERE ");
      removeBoxProductsStrQuery.append(MovLineBox.PROPERTY_PACKINGBOX + " = '" + box.getId() + "'");
      Query removeBoxProductQuery = OBDal.getInstance().getSession()
          .createQuery(removeBoxProductsStrQuery.toString());
      removeBoxProductQuery.executeUpdate();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void deletePickingBox(PackingBox box) {
    OBContext.setAdminMode(true);
    try {
      deletePickingBoxContent(box);
      StringBuffer removeBoxStrQuery = new StringBuffer();
      removeBoxStrQuery.append("delete FROM " + PackingBox.ENTITY_NAME + " WHERE ");
      removeBoxStrQuery.append(PackingBox.PROPERTY_ID + " = '" + box.getId() + "'");
      Query removeBoxQuery = OBDal.getInstance().getSession()
          .createQuery(removeBoxStrQuery.toString());
      removeBoxQuery.executeUpdate();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static BigDecimal getNumberOfUnitsPackagedOnOtherBoxes(String boxIdToIgnore,
      String movLineId) {
    PackingBox boxToIgnore = OBDal.getInstance().get(PackingBox.class, boxIdToIgnore);
    Packing packingh = boxToIgnore.getObwpackPackingh();
    InternalMovementLine movLine = OBDal.getInstance().get(InternalMovementLine.class, movLineId);
    return getNumberOfUnitsPackagedOnOtherBoxes(packingh, movLine, boxToIgnore);
  }

  public static BigDecimal getNumberOfUnitsPackagedOnOtherBoxes(Packing packingh,
      InternalMovementLine movLine, PackingBox boxToIgnore) {
    BigDecimal result = null;
    List<PackingBox> boxes = packingh.getOBWPACKBoxList();
    StringBuilder boxexIds = new StringBuilder();
    int j = 0;
    for (int i = 0; i < boxes.size(); i++) {
      if (!boxes.get(i).getId().equals(boxToIgnore.getId())) {
        if (j > 0) {
          boxexIds.append(",");
        }
        boxexIds.append("'");
        boxexIds.append(boxes.get(i).getId());
        boxexIds.append("'");
        j += 1;
      }
    }
    StringBuffer countUntisInBoxesStrQuery = new StringBuffer();
    countUntisInBoxesStrQuery.append("select sum(" + MovLineBox.PROPERTY_QUANTITY + ") FROM "
        + MovLineBox.ENTITY_NAME + " WHERE ");
    if (boxexIds.toString().length() > 0) {
      countUntisInBoxesStrQuery.append(MovLineBox.PROPERTY_PACKINGBOX + ".id in ("
          + boxexIds.toString() + ") and ");
    }
    countUntisInBoxesStrQuery.append(MovLineBox.PROPERTY_MOVEMENTLINE + ".id = '" + movLine.getId()
        + "'");
    Query countUntisInBoxesQuery = OBDal.getInstance().getSession()
        .createQuery(countUntisInBoxesStrQuery.toString());
    result = (BigDecimal) countUntisInBoxesQuery.uniqueResult();
    if (result == null) {
      return BigDecimal.ZERO;
    } else {
      return result;
    }
  }
}