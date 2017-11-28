/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.datasource;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.mobile.warehouse.WarehouseProcessHQLQuery;

public class OBMWHP_relatedSalesOrders_dataSource extends WarehouseProcessHQLQuery {
  public static final String relatedSalesOrderExtension = "OBMWHP_relatedSalesOrdersExtension";

  @Inject
  @Any
  @Qualifier(relatedSalesOrderExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    final JSONObject params = jsonsent.getJSONObject("parameters");
    String idToSearch;
    Boolean grouped;

    if (params.has("oBWPLWarehousePickingListId")
        && !params.getJSONObject("oBWPLWarehousePickingListId").getString("value").equals("")) {
      grouped = false;
      idToSearch = params.getJSONObject("oBWPLWarehousePickingListId").getString("value");
    } else if (params.has("oBWPLGroupPickinglistId")
        && !params.getJSONObject("oBWPLGroupPickinglistId").getString("value").equals("")) {
      grouped = true;
      idToSearch = params.getJSONObject("oBWPLGroupPickinglistId").getString("value");
    } else {
      throw new JSONException(
          "OBMWHP_materialMgmtInternalMovementLine_dataSource has been executed without pickinglist id");
    }

    String limit = "1000";
    String prefLimit = null;
    if (params.has("limit")) {
      prefLimit = params.getString(prefLimit);
      if (StringUtils.isNumeric(prefLimit)) {
        limit = prefLimit;
      }
    } else {
      try {
        prefLimit = Preferences.getPreferenceValue("OBWPL_GroupPL", true, OBContext.getOBContext()
            .getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(), OBContext
            .getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
        if (StringUtils.isNumeric(prefLimit)) {
          limit = prefLimit;
        }
      } catch (PropertyNotFoundException e) {
        // nothing to do, use 1000
      } catch (PropertyException e) {
        // nothing to do, use 1000
      }
    }

    List<String> hqlQueries = new ArrayList<String>();

    HQLPropertyList regularRelatedSalesOrdesProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);

    StringBuilder strBuild = new StringBuilder();
    strBuild.append("select ");
    strBuild.append(regularRelatedSalesOrdesProperties.getHqlSelect());
    strBuild.append("from Order as ord where exists ");
    strBuild.append("(select 1 from MaterialMgmtInternalMovementLine as movLine ");
    strBuild.append("where ");
    if (grouped == false) {
      strBuild.append("movLine.oBWPLWarehousePickingList.id = '" + idToSearch + "' ");
    } else {
      strBuild.append("movLine.oBWPLGroupPickinglist.id = '" + idToSearch + "' ");
    }
    strBuild.append("and ord.id = movLine.salesOrderLine.salesOrder.id ");
    strBuild.append(") ");
    strBuild.append("order by ord.creationDate ASC ");
    strBuild.append("limit " + limit + ";");
    hqlQueries.add(strBuild.toString());

    return hqlQueries;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }
}