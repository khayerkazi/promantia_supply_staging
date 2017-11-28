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
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.warehouse.pickinglist;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

public class SelectOrdersDS extends ReadOnlyDataSourceService {

  private String distinct;
  private static final String AD_TABLE_ID = "F44AFD9E32554A1BBA3DA69548767F38";

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
    final String strPickingId = parameters.get("@OBWPL_pickinglist.id@");
    final String strClientId = parameters.get("@OBWPL_pickinglist.client@");
    PickingList picking = OBDal.getInstance().get(PickingList.class, strPickingId);
    distinct = parameters.get(JsonConstants.DISTINCT_PARAMETER);
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider(
        strClientId);
    Set<String> childOrgs = osp.getChildTree(picking.getOrganization().getId(), true);

    StringBuffer where = new StringBuffer();
    where.append(" as o");
    where.append(" where o." + Order.PROPERTY_ORGANIZATION + ".id in (:orgs)");
    where.append("   and o." + Order.PROPERTY_DOCUMENTSTATUS + " = 'CO'");
    where.append("   and o." + Order.PROPERTY_OBWPLISINPICKINGLIST + " = false");
    where.append("   and o." + Order.PROPERTY_SALESTRANSACTION + " = true");
    where.append("   and o." + Order.PROPERTY_WAREHOUSE + " = :warehouse");
    where.append("   and exists (select 1 from " + OrderLine.ENTITY_NAME + " as ol");
    where.append("                   join ol." + OrderLine.PROPERTY_PRODUCT + " as p");
    where.append("               where ol." + OrderLine.PROPERTY_SALESORDER + " = o");
    where.append("                 and ol." + OrderLine.PROPERTY_DELIVEREDQUANTITY + " != ol."
        + OrderLine.PROPERTY_ORDEREDQUANTITY);
    where.append("                 and p." + Product.PROPERTY_STOCKED + " = true");
    where.append("                 and p." + Product.PROPERTY_PRODUCTTYPE + " = 'I')");
    OBQuery<Order> qryOrder = OBDal.getInstance().createQuery(Order.class, where.toString());
    qryOrder.setNamedParameter("orgs", childOrgs);
    qryOrder.setNamedParameter("warehouse", picking.getOutboundStorageBin().getWarehouse());
    qryOrder.setFilterOnReadableOrganization(false);
    List<Order> orders = qryOrder.list();
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    try {
      result = buildResult(orders, parameters);
    } catch (JSONException e) {
      // Do nothing
    }
    if (result.isEmpty() || StringUtils.isNotEmpty(distinct)) {
      return result;
    }
    String sortBy = parameters.get(JsonConstants.SORTBY_PARAMETER);
    if (StringUtils.isEmpty(sortBy)) {
      sortBy = "documentNumber";
    }
    if (sortBy.endsWith("$_identifier")) {
      sortBy = sortBy.substring(0, sortBy.length() - 12);
    }

    Collections.sort(result, new ResultComparator(sortBy));

    return result;
  }

  private List<Map<String, Object>> buildResult(List<Order> orders, Map<String, String> parameters)
      throws JSONException {
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    List<String> includedIds = new ArrayList<String>();
    for (Order order : orders) {

      Map<String, Object> orderMap = createResultMap(order);

      if (StringUtils.isNotEmpty(parameters.get("criteria"))) {
        boolean addValue = new ResultMapCriteriaUtils(orderMap, parameters).applyFilter();
        if (!addValue) {
          continue;
        }
      }

      if (StringUtils.isNotEmpty(distinct)) {
        BaseOBObject obj = (BaseOBObject) orderMap.get(distinct);
        if (includedIds.contains((String) obj.getId())) {
          continue;
        } else {
          includedIds.add((String) obj.getId());
        }
        Map<String, Object> distinctMap = new HashMap<String, Object>();
        distinctMap.put("id", obj.getId());
        distinctMap.put("_identifier", obj.getIdentifier());
        result.add(distinctMap);

      } else {
        result.add(orderMap);
      }
    }
    return result;
  }

  private Map<String, Object> createResultMap(Order order) {
    Map<String, Object> resultMap = new HashMap<String, Object>();
    resultMap.put("id", order.getId());
    resultMap.put("Client", order.getClient());
    resultMap.put("Organization", order.getOrganization());
    resultMap.put("Active", true);
    resultMap.put("creationDate", new Date());
    resultMap.put("createdBy", OBContext.getOBContext().getUser());
    resultMap.put("updated", new Date());
    resultMap.put("updatedBy", OBContext.getOBContext().getUser());
    resultMap.put("documentNumber", order.getDocumentNo());
    resultMap.put("orderDate", JsonUtils.createDateFormat().format(order.getOrderDate()));
    resultMap.put("businessPartner", order.getBusinessPartner());
    resultMap.put("partnerAddress", order.getPartnerAddress());
    resultMap.put("scheduledDeliveryDate",
        JsonUtils.createDateFormat().format(order.getScheduledDeliveryDate()));
    resultMap.put("lines", createLinesField(order));
    resultMap.put("order", order);

    resultMap.put("hasError", false);
    resultMap.put("errorMsg", "");
    resultMap.put("obSelected", false);
    return resultMap;
  }

  private String createLinesField(Order order) {
    StringBuffer lines = new StringBuffer();
    boolean isFirst = true;
    for (OrderLine line : order.getOrderLineList()) {
      if (line.getProduct() == null) {
        continue;
      }
      if (!line.getProduct().isStocked() || !"I".equals(line.getProduct().getProductType())) {
        continue;
      }
      if (line.getOrderedQuantity().signum() <= 0) {
        continue;
      }
      if (line.getDeliveredQuantity().compareTo(line.getOrderedQuantity()) == 0) {
        continue;
      }
      if (isFirst) {
        isFirst = false;
      } else {
        lines.append(" \n");
      }
      lines.append(line.getLineNo());
      lines.append(" - " + line.getProduct().getIdentifier());
      lines.append(" - " + line.getOrderedQuantity().subtract(line.getDeliveredQuantity()));
      lines.append(" " + line.getUOM().getSymbol());
    }
    return lines.toString();
  }

  private static class ResultComparator implements Comparator<Map<String, Object>> {
    private String compareBy = "convertedFreightAmount";
    boolean desc = false;

    public ResultComparator(String _compareBy) {
      compareBy = _compareBy;
      if (compareBy.startsWith("-")) {
        desc = true;
        compareBy = compareBy.substring(1);
      }
    }

    @Override
    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
      Object obj1 = o1.get(compareBy);
      Object obj2 = o2.get(compareBy);
      if (obj2 == null) {
        return -1;
      } else if (obj1 == null) {
        return 1;
      }
      if (obj1 instanceof BigDecimal) {
        final BigDecimal v1 = (BigDecimal) o1.get(compareBy);
        final BigDecimal v2 = (BigDecimal) o2.get(compareBy);
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof String) {
        final String v1 = (String) o1.get(compareBy);
        final String v2 = (String) o2.get(compareBy);
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof Date) {
        final Date v1 = (Date) o1.get(compareBy);
        final Date v2 = (Date) o2.get(compareBy);
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof BaseOBObject) {
        final String v1 = ((BaseOBObject) o1.get(compareBy)).getIdentifier();
        final String v2 = ((BaseOBObject) o2.get(compareBy)).getIdentifier();
        return desc ? v2.compareTo(v1) : v1.compareTo(v2);
      } else if (obj1 instanceof Boolean) {
        final boolean v1 = (Boolean) o1.get(compareBy);
        final boolean v2 = (Boolean) o2.get(compareBy);
        if (v1 == v2) {
          return 0;
        }
        if (v1) {
          return desc ? -1 : 1;
        } else {
          return desc ? 1 : -1;
        }
      } else {
        // unable to compare
        return 0;
      }
    }
  }
}
