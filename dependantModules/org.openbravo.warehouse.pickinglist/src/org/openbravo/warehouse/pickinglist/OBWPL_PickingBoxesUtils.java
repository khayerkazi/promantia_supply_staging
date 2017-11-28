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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.warehouse.pickinglist.hooks.ValidateBoxSerialNumberHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OBWPL_PickingBoxesUtils {
  final private static Logger log = LoggerFactory.getLogger(OBWPL_PickingBoxesUtils.class);

  public static List<PickingListBox> CreatePickingListBox(String pickingListId, JSONArray boxesToAdd) {
    PickingList parentPickingList = OBDal.getInstance().get(PickingList.class, pickingListId);
    List<PickingListBox> createdBoxes = new ArrayList<PickingListBox>();
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
          OBCriteria<PickingListBox> boxesCrit = OBDal.getInstance().createCriteria(
              PickingListBox.class);
          boxesCrit.add(Restrictions.eq(PickingListBox.PROPERTY_OBWPLPICKINGLIST + ".id",
              pickingListId));

          List<PickingListBox> pickingListBoxResult = boxesCrit.list();
          if (pickingListBoxResult != null & pickingListBoxResult.size() > 0) {
            boxNo = new Long(pickingListBoxResult.size() + 1);
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
            OBError result = hook.exec(trackingNo, parentPickingList);
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

        PickingListBox newBox = OBProvider.getInstance().get(PickingListBox.class);
        newBox.setClient(parentPickingList.getClient());
        newBox.setOrganization(parentPickingList.getOrganization());
        newBox.setBoxno(boxNo);
        newBox.setSearchKey(trackingNo);
        newBox.setName(trackingNo);
        newBox.setDescription("");
        newBox.setObwplPickinglist(parentPickingList);

        try {
          OBContext.setAdminMode(false);
          OBDal.getInstance().save(newBox);
        } finally {
          OBContext.restorePreviousMode();
        }

        createdBoxes.add(newBox);
      }
    } catch (Exception e) {
      throw new OBException("An error happened while creating boxes. " + e.getMessage());
    }
    return createdBoxes;
  }

  public static void CreateOrUpdateBoxContent(String movId, String pickingListId,
      JSONObject boxContent) {
    int counter = 0;
    try {
      InternalMovementLine movLine = OBDal.getInstance().get(InternalMovementLine.class, movId);
      PickingList picking = OBDal.getInstance().get(PickingList.class, pickingListId);
      @SuppressWarnings("unchecked")
      Iterator<String> boxesIds = boxContent.keys();
      while (boxesIds.hasNext()) {
        counter += 1;
        String boxId = (String) boxesIds.next();

        OBCriteria<PickingListBoxContent> movLineCrit = OBDal.getInstance().createCriteria(
            PickingListBoxContent.class);
        movLineCrit.add(Restrictions.eq(PickingListBoxContent.PROPERTY_MOVEMENTLINE + ".id",
            movLine.getId()));
        movLineCrit.add(Restrictions.eq(PickingListBoxContent.PROPERTY_OBWPLPLBOX + ".id", boxId));

        movLineCrit.setMaxResults(1);

        List<PickingListBoxContent> lstMovLineBox = movLineCrit.list();
        PickingListBoxContent plbcontent = null;

        if (lstMovLineBox.size() == 1) {
          plbcontent = lstMovLineBox.get(0);
          if (boxContent.getLong(boxId) == 0L) {
            OBDal.getInstance().remove(plbcontent);
          } else {
            plbcontent.setQuantity(new BigDecimal(boxContent.getLong(boxId)));
            OBDal.getInstance().save(plbcontent);
          }
        } else {
          plbcontent = OBProvider.getInstance().get(PickingListBoxContent.class);
          plbcontent.setClient(picking.getClient());
          plbcontent.setOrganization(picking.getOrganization());
          plbcontent.setMovementLine(movLine);
          if (movLine.getStockReservation() != null) {
            if (movLine.getStockReservation().getSalesOrderLine() != null) {
              plbcontent.setSalesOrderLine(movLine.getStockReservation().getSalesOrderLine());
            }
          }
          plbcontent.setProduct(movLine.getProduct());
          if (!movLine.getAttributeSetValue().equals("0")) {
            plbcontent.setAttributeSetValue(movLine.getAttributeSetValue());
          }
          plbcontent.setObwplPlbox(OBDal.getInstance().get(PickingListBox.class, boxId));
          plbcontent.setQuantity(new BigDecimal(boxContent.getLong(boxId)));

          OBDal.getInstance().save(plbcontent);
        }
      }
      if (counter == 0) {
        deleteMovLineFromEveryBoxes(movLine);
      }
    } catch (Exception e) {
      throw new OBException("An error happened while adding content to pack: " + e.getMessage());
    }
  }

  public static void deletePickingBoxContent(PickingListBox box) {
    OBContext.setAdminMode(true);
    try {
      StringBuffer removeBoxProductsStrQuery = new StringBuffer();
      removeBoxProductsStrQuery.append("delete FROM " + PickingListBoxContent.ENTITY_NAME
          + " WHERE ");
      removeBoxProductsStrQuery.append(PickingListBoxContent.PROPERTY_OBWPLPLBOX + ".id = '"
          + box.getId() + "'");
      Query removeBoxProductQuery = OBDal.getInstance().getSession()
          .createQuery(removeBoxProductsStrQuery.toString());
      removeBoxProductQuery.executeUpdate();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void deleteMovLineFromEveryBoxes(InternalMovementLine movLine) {
    OBContext.setAdminMode(true);
    try {
      StringBuffer removeMovFromEveryBoxesStrQuery = new StringBuffer();
      removeMovFromEveryBoxesStrQuery.append("delete FROM " + PickingListBoxContent.ENTITY_NAME
          + " WHERE ");
      removeMovFromEveryBoxesStrQuery.append(PickingListBoxContent.PROPERTY_MOVEMENTLINE
          + ".id = '" + movLine.getId() + "'");
      Query removeMovFromEveryBoxesQuery = OBDal.getInstance().getSession()
          .createQuery(removeMovFromEveryBoxesStrQuery.toString());
      removeMovFromEveryBoxesQuery.executeUpdate();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void deletePickingBox(PickingListBox box) {
    OBContext.setAdminMode(true);
    try {
      deletePickingBoxContent(box);
      StringBuffer removeBoxStrQuery = new StringBuffer();
      removeBoxStrQuery.append("delete FROM " + PickingListBox.ENTITY_NAME + " WHERE ");
      removeBoxStrQuery.append(PickingListBox.PROPERTY_ID + " = '" + box.getId() + "'");
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
    PickingListBox boxToIgnore = OBDal.getInstance().get(PickingListBox.class, boxIdToIgnore);
    PickingList picking = boxToIgnore.getObwplPickinglist();
    InternalMovementLine movLine = OBDal.getInstance().get(InternalMovementLine.class, movLineId);
    return getNumberOfUnitsPackagedOnOtherBoxes(picking, movLine, boxToIgnore);
  }

  public static BigDecimal getNumberOfUnitsPackagedOnOtherBoxes(PickingList picking,
      InternalMovementLine movLine, PickingListBox boxToIgnore) {
    BigDecimal result = null;
    List<PickingListBox> boxes = picking.getOBWPLPlboxList();
    StringBuilder boxexIds = new StringBuilder();
    boxexIds.append("'0'");
    for (int i = 0; i < boxes.size(); i++) {
      if (!boxes.get(i).getId().equals(boxToIgnore.getId())) {
        boxexIds.append(",'");
        boxexIds.append(boxes.get(i).getId());
        boxexIds.append("'");
      }
    }
    StringBuffer countUntisInBoxesStrQuery = new StringBuffer();
    countUntisInBoxesStrQuery.append("select sum(" + PickingListBoxContent.PROPERTY_QUANTITY
        + ") FROM " + PickingListBoxContent.ENTITY_NAME + " WHERE ");
    if (boxexIds.toString().length() > 0) {
      countUntisInBoxesStrQuery.append(PickingListBoxContent.PROPERTY_OBWPLPLBOX + ".id in ("
          + boxexIds.toString() + ") and ");
    }
    countUntisInBoxesStrQuery.append(PickingListBoxContent.PROPERTY_MOVEMENTLINE + ".id = '"
        + movLine.getId() + "'");
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