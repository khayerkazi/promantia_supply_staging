/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.warehouse.pickinglist.datasource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.warehouse.pickinglist.OBWPL_Utils;
import org.openbravo.warehouse.pickinglist.PickingListProblem;
import org.openbravo.warehouse.pickinglist.PickingListProblemOrderLines;

public class SolOfIncidence extends ReadOnlyDataSourceService {
  private static final Logger log = Logger.getLogger(SolOfIncidence.class);
  private final SimpleDateFormat xmlDateTimeFormat = JsonUtils.createDateTimeFormat();
  private final String identifierSufix = "$" + JsonConstants.IDENTIFIER;
  // Table ID of the datasource based table defined in the application dictionary
  private static final String AD_TABLE_ID = "C708E07A64D4463D9D89F49A6287736B";

  @Override
  public Entity getEntity() {
    return ModelProvider.getInstance().getEntityByTableId(AD_TABLE_ID);
  }

  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, Integer.MAX_VALUE).size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {

    OBContext.setAdminMode(true);
    final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    try {
      final String incidenceId = parameters.get("@OBWPL_pickinglistproblem.id@");
      if (incidenceId == null || incidenceId.equals("null")) {
        return result;
      }
      final PickingListProblem incidence = OBDal.getInstance().get(PickingListProblem.class,
          incidenceId);

      ScrollableResults orderLinesScroll = OBWPL_Utils.getSOlinesRelatedToIncidence(incidence);
      while (orderLinesScroll.next()) {
        Map<String, Object> variantMap = new HashMap<String, Object>();
        PickingListProblemOrderLines plol = (PickingListProblemOrderLines) orderLinesScroll.get(0);
        OrderLine ol = plol.getOrderline();
        variantMap.put("id", ol.getId() + incidenceId);
        variantMap.put("client", ol.getClient());
        variantMap.put("client", ol.getClient().getIdentifier());
        variantMap.put("organization", ol.getOrganization());
        variantMap.put("organization" + identifierSufix, ol.getOrganization().getIdentifier());
        variantMap.put("active", true);
        variantMap.put("creationDate", xmlDateTimeFormat.format(plol.getCreationDate()));
        variantMap.put("createdBy", plol.getCreatedBy());
        variantMap.put("createdBy" + identifierSufix, plol.getCreatedBy().getIdentifier());
        variantMap.put("updated", xmlDateTimeFormat.format(plol.getUpdated()));
        variantMap.put("updatedBy", plol.getUpdatedBy());
        variantMap.put("updatedBy" + identifierSufix, plol.getUpdatedBy().getIdentifier());
        variantMap.put("pickingListProblem", incidenceId);
        variantMap.put("pickingListProblem" + identifierSufix, incidence.getIdentifier());
        variantMap.put("product", ol.getProduct().getId());
        variantMap.put("product" + identifierSufix, ol.getProduct().getIdentifier());
        variantMap.put("salesOrderLine", ol.getId());
        variantMap.put("salesOrderLine" + identifierSufix, ol.getIdentifier());
        variantMap.put("salesOrder", ol.getSalesOrder().getId());
        variantMap.put("salesOrder" + identifierSufix, ol.getSalesOrder().getIdentifier());
        variantMap.put("orderDate", ol.getOrderDate());
        variantMap.put("orderedQuantity", ol.getOrderedQuantity());
        result.add(variantMap);
      }

    } catch (Exception e) {
      log.error("Error in DS while getting Sales Order Lines: " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
