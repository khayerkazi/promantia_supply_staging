/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.menu;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.core.MobileCoreConstants;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.mobile.warehouse.WarehouseMenu;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Warehouse;

public class Menu extends WarehouseJSONProcess {
  private static final Logger log = Logger.getLogger(Menu.class);

  @Inject
  @Any
  private Instance<MenuNodeParameterHandler> parameterHandlers;

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    OBContext.setAdminMode(true);
    try {
      JSONObject response = new JSONObject();
      response.put("status", 0);
      response.put("data", getBranch(null));
      return response;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private JSONArray getBranch(WarehouseMenu parentNode) throws JSONException {
    JSONArray branch = new JSONArray();
    OBCriteria<WarehouseMenu> qMenu = OBDal.getInstance().createCriteria(WarehouseMenu.class);
    qMenu.add(Restrictions.eq(WarehouseMenu.PROPERTY_ACTIVE, true));

    if (parentNode == null) {
      qMenu.add(Restrictions.isNull(WarehouseMenu.PROPERTY_PARENTMENU));
    } else {
      qMenu.add(Restrictions.eq(WarehouseMenu.PROPERTY_PARENTMENU, parentNode));
    }
    qMenu.addOrderBy(WarehouseMenu.PROPERTY_SEQUENCENUMBER, true);

    for (WarehouseMenu entry : qMenu.list()) {
      if (!hasWarehouseAccess(entry)) {
        continue;
      }
      boolean nodeHasData = false;
      JSONObject menuNode = new JSONObject();
      menuNode.put("title", entry.getIdentifier());
      if (!entry.isSummaryLevel()) {
        String windowType = entry.getWindowType();
        if (!hasPreferenceAccess(windowType)) {
          continue;
        }

        nodeHasData = true;
        menuNode.put("path", windowType);
        MenuNodeParameterHandler parameterHandler = null;
        try {
          parameterHandler = parameterHandlers.select(new ComponentProvider.Selector(windowType))
              .get();
        } catch (Exception e) {
          log.debug("Error retrieving parameter handler for " + windowType, e);
        }
        if (parameterHandler != null) {
          menuNode.put("parameters", parameterHandler.getParameters(entry));
        }
      } else {
        JSONArray submenu = getBranch(entry);
        if (submenu.length() > 0) {
          menuNode.put("submenu", getBranch(entry));
          nodeHasData = true;
        }
      }
      if (nodeHasData) {
        branch.put(menuNode);
      }
    }

    return branch;
  }

  private boolean hasWarehouseAccess(WarehouseMenu node) {
    Warehouse warehouse = getWarehouse(node);

    if (warehouse == null) {
      return true;
    }

    return isWarehouseInWritableOrgs(warehouse);
  }

  private boolean hasPreferenceAccess(String windowType) {
    if (!OBContext.getOBContext().getRole().isManual()) {
      return true;
    }

    OBCriteria<org.openbravo.model.ad.domain.List> qProp = OBDal.getInstance().createCriteria(
        org.openbravo.model.ad.domain.List.class);
    qProp.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE + ".id",
        MobileCoreConstants.PREFERENCE_LIST_ID));
    qProp.add(Restrictions.like(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, "%"
        + windowType));
    if (qProp.list().size() == 0) {
      log.warn("Not found preference for window type " + windowType);
      return false;
    }

    org.openbravo.model.ad.domain.List preference = qProp.list().get(0);
    System.out.println("Preference " + preference.getSearchKey());

    try {
      return "Y".equals(Preferences.getPreferenceValue(preference.getSearchKey(), true, OBContext
          .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null));
    } catch (PropertyException e) {
      return false;
    }
  }

  private boolean isWarehouseInWritableOrgs(Warehouse warehouse) {
    OBCriteria<OrgWarehouse> qOrgWarehouse = OBDal.getInstance().createCriteria(OrgWarehouse.class);
    qOrgWarehouse.add(Restrictions.in(OrgWarehouse.PROPERTY_ORGANIZATION + ".id", OBContext
        .getOBContext().getWritableOrganizations())); // XXX: Check this: should we use writable or
                                                      // readable orgs?
    qOrgWarehouse.add(Restrictions.eq(OrgWarehouse.PROPERTY_WAREHOUSE, warehouse));
    return qOrgWarehouse.count() != 0;
  }

  private Warehouse getWarehouse(WarehouseMenu node) {
    if (node.getWarehouse() != null) {
      return node.getWarehouse();
    }

    if (node.getParentMenu() != null) {
      return getWarehouse(node.getParentMenu());
    }

    return null;
  }
}