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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.procurement.POInvoiceMatch;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.JsonConstants;

public class EditPickingListItemDS extends ReadOnlyDataSourceService {
  private String distinct;
  private static final String AD_TABLE_ID = "9C2BA220C5434E6FB074E20E9ACEB10A";
  private int count = 0;
  private static final AttributeSetInstance asiZero = OBDal.getInstance().get(
      AttributeSetInstance.class, "0");

  @Override
  public Entity getEntity() {
    return ModelProvider.getInstance().getEntityByTableId(AD_TABLE_ID);
  }

  @Override
  protected int getCount(Map<String, String> parameters) {
    return count;
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    OBContext.setAdminMode(true);
    try {
      final String strMovementLineId = parameters.get("@MaterialMgmtInternalMovementLine.id@");
      InternalMovementLine mvmtLine = OBDal.getInstance().get(InternalMovementLine.class,
          strMovementLineId);
      final Reservation reservation = mvmtLine.getStockReservation();

      StringBuffer where = new StringBuffer();
      Map<String, Object> namedParameters = new HashMap<String, Object>();
      where.append(" as sd");
      where.append("    join sd." + StorageDetail.PROPERTY_STORAGEBIN + " as sb");
      where.append(" where sd." + StorageDetail.PROPERTY_PRODUCT + " = :product");
      where.append("   and sd." + StorageDetail.PROPERTY_ORDERUOM + " is null");
      where.append("   and sd." + StorageDetail.PROPERTY_UOM + " = :uom");
      where.append("   and sd." + StorageDetail.PROPERTY_QUANTITYONHAND + " > 0");
      where.append("   and sb." + Locator.PROPERTY_WAREHOUSE + " = :pickwh");
      if (reservation.getWarehouse() != null) {
        where.append("   and sb." + Locator.PROPERTY_WAREHOUSE + " = :warehouse");
        namedParameters.put("warehouse", reservation.getWarehouse());
      }
      if (reservation.getStorageBin() != null) {
        where.append("   and sb = :locator");
        namedParameters.put("locator", reservation.getStorageBin());
      }
      if (reservation.getAttributeSetValue() != null) {
        where.append("   and sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE + " = :asi");
        namedParameters.put("asi", reservation.getAttributeSetValue());
      }

      OBQuery<StorageDetail> qrySD = OBDal.getInstance().createQuery(StorageDetail.class,
          where.toString());
      namedParameters.put("product", reservation.getProduct());
      namedParameters.put("uom", reservation.getUOM());
      namedParameters.put("pickwh", mvmtLine.getOBWPLWarehousePickingList().getOutboundStorageBin()
          .getWarehouse());
      qrySD.setFilterOnReadableOrganization(false);
      qrySD.setNamedParameters(namedParameters);
      List<StorageDetail> sds = qrySD.list();
      List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
      try {
        result = buildResult(sds, parameters, reservation);
      } catch (JSONException e) {
        // Do nothing
      }
      count = result.size();
      if (result.isEmpty() || StringUtils.isNotEmpty(distinct)) {
        return result;
      }
      String sortBy = parameters.get(JsonConstants.SORTBY_PARAMETER);
      if (StringUtils.isEmpty(sortBy)) {
        sortBy = "-availableQty";
      }
      if (sortBy.endsWith("$_identifier")) {
        sortBy = sortBy.substring(0, sortBy.length() - 12);
      }

      Collections.sort(result, new ResultComparator(sortBy));

      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<Map<String, Object>> buildResult(List<StorageDetail> sds,
      Map<String, String> parameters, Reservation reservation) throws JSONException {
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    List<String> includedIds = new ArrayList<String>();
    // Key of stockReservations is the concatenation of Locator and AttributeSetInstance ids.
    Map<String, ReservationStock> stockReservations = getCurrentStockReservations(reservation);
    for (StorageDetail sd : sds) {
      String strASIId = sd.getAttributeSetValue() == null ? "0" : sd.getAttributeSetValue().getId();
      String strKey = "sd-" + sd.getStorageBin().getId() + "-" + strASIId;

      ReservationStock resStock = stockReservations.remove(strKey);

      Map<String, Object> sdMap = createResultMap(reservation, sd, resStock);

      boolean forceValue = resStock != null;
      if (!forceValue && StringUtils.isNotEmpty(parameters.get("criteria"))) {
        boolean addValue = new ResultMapCriteriaUtils(sdMap, parameters).applyFilter();
        if (!addValue) {
          continue;
        }
      }

      if (StringUtils.isNotEmpty(distinct)) {
        BaseOBObject obj = (BaseOBObject) sdMap.get(distinct);
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
        result.add(sdMap);
      }
    }
    // Stock reservations from storage details that are not available within current reservation
    // definition. It can happen when the reservation is modified on a more restrictive way setting
    // a mandatory warehouse, storage bin or attributes already having stock reserved.
    for (ReservationStock resStock : stockReservations.values()) {
      StorageDetail sd = getStorageDetailFromResStock(resStock);
      Map<String, Object> sdMap = createResultMap(reservation, sd, resStock);
      if (StringUtils.isNotEmpty(distinct)) {
        BaseOBObject obj = (BaseOBObject) sdMap.get(distinct);
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
        result.add(sdMap);
      }
    }
    return result;
  }

  private Map<String, Object> createResultMap(Reservation reservation, StorageDetail sd,
      ReservationStock resStock) {
    // if the storage detail is null, the resStock is a pre-reservation
    OrderLine purchaseOL = (sd == null) ? resStock.getSalesOrderLine() : null;
    Locator locator = null;
    Warehouse wh = null;
    AttributeSetInstance asi = null;
    BigDecimal availableQty = BigDecimal.ZERO;
    BigDecimal reservedInOthers = BigDecimal.ZERO;
    BigDecimal reservedQty = null;
    BigDecimal releasedQty = null;
    BigDecimal reservationQty = reservation.getQuantity();
    boolean isAllocated = false;
    String strId = null;
    if (sd != null) {
      reservedInOthers = getQtyReservedFromSD(reservation, reservation.getProduct(),
          sd.getAttributeSetValue(), sd.getStorageBin());
    } else if (purchaseOL != null) {
      reservedInOthers = getQtyReservedFromPOL(reservation, purchaseOL);
    }
    if (resStock != null) {
      reservedQty = resStock.getQuantity();
      releasedQty = resStock.getReleased();
      isAllocated = resStock.isAllocated();
      locator = resStock.getStorageBin();
      if (locator != null) {
        wh = locator.getWarehouse();
      }
    }
    if (sd != null) {
      strId = sd.getId();
      locator = sd.getStorageBin();
      wh = locator.getWarehouse();
      asi = sd.getAttributeSetValue();
      availableQty = sd.getQuantityOnHand();
    } else if (purchaseOL != null) {
      strId = purchaseOL.getId();
      availableQty = purchaseOL.getOrderedQuantity();
      // decrease already received stock.
      for (POInvoiceMatch matchPO : purchaseOL.getProcurementPOInvoiceMatchList()) {
        if (matchPO.getGoodsShipmentLine() != null) {
          availableQty.subtract(matchPO.getQuantity());
        }
      }
    }
    if (asi == null) {
      asi = asiZero;
    }

    Map<String, Object> resultMap = new HashMap<String, Object>();

    resultMap.put("id", strId);
    resultMap.put("Client", OBContext.getOBContext().getCurrentClient());
    resultMap.put("Organization", OBContext.getOBContext().getCurrentOrganization());
    resultMap.put("Active", true);
    resultMap.put("creationDate", new Date());
    resultMap.put("createdBy", OBContext.getOBContext().getUser());
    resultMap.put("updated", new Date());
    resultMap.put("updatedBy", OBContext.getOBContext().getUser());

    resultMap.put("storageBin", locator);
    resultMap.put("warehouse", wh);
    resultMap.put("attributeSetValue", asi);
    resultMap.put("purchaseOrderLine", purchaseOL);

    resultMap.put("availableQty", availableQty);
    resultMap.put("reservedinothers", reservedInOthers);
    resultMap.put("quantity", reservedQty);
    resultMap.put("reservationQuantity", reservationQty);
    resultMap.put("released", releasedQty);
    resultMap.put("allocated", isAllocated);

    if (resStock != null) {
      resultMap.put("reservationStock", resStock.getId());
    }

    resultMap.put("hasError", false);
    resultMap.put("errorMsg", "");
    resultMap.put("obSelected", resStock != null);

    return resultMap;
  }

  private List<Warehouse> getOnHandWarehouses(Organization organization) {
    List<Warehouse> whs = new ArrayList<Warehouse>();
    List<OrgWarehouse> orgWhs = OBDao.getActiveOBObjectList(organization,
        Organization.PROPERTY_ORGANIZATIONWAREHOUSELIST);
    for (OrgWarehouse orgWh : orgWhs) {
      whs.add(orgWh.getWarehouse());
    }
    return whs;
  }

  private Map<String, ReservationStock> getCurrentStockReservations(Reservation reservation) {
    Map<String, ReservationStock> stockReservations = new HashMap<String, ReservationStock>();
    for (ReservationStock resStock : reservation.getMaterialMgmtReservationStockList()) {
      stockReservations.put(getCurStockResKey(resStock), resStock);
    }
    return stockReservations;
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

  private StorageDetail getStorageDetailFromResStock(ReservationStock resStock) {
    OBCriteria<StorageDetail> critSD = OBDal.getInstance().createCriteria(StorageDetail.class);
    critSD.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, resStock.getReservation()
        .getProduct()));
    critSD.add(Restrictions.eq(StorageDetail.PROPERTY_UOM, resStock.getReservation().getUOM()));
    critSD.add(Restrictions.isNull(StorageDetail.PROPERTY_ORDERUOM));
    critSD.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, resStock.getStorageBin()));
    if (resStock.getAttributeSetValue() == null
        || resStock.getAttributeSetValue().getId().equals("0")) {
      critSD.add(Restrictions.or(Restrictions.isNull(StorageDetail.PROPERTY_ATTRIBUTESETVALUE),
          Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, asiZero)));
    } else {
      critSD.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE,
          resStock.getAttributeSetValue()));
    }
    return null;
  }

  private BigDecimal getQtyReservedFromPOL(Reservation reservation, OrderLine orderLine) {
    final StringBuilder hqlString = new StringBuilder();
    hqlString
        .append("select coalesce(sum(rs.quantity - coalesce(rs.released,0)),0) from MaterialMgmtReservationStock rs ");
    hqlString.append("join rs.reservation as r ");
    hqlString.append(" where r.rESStatus not in ('CL', 'DR') ");
    hqlString.append(" and rs.salesOrderLine = :orderLine ");
    hqlString.append(" and r <> :reservation ");
    final Session session = OBDal.getInstance().getSession();
    Query query = session.createQuery(hqlString.toString());
    query.setParameter("orderLine", orderLine);
    query.setParameter("reservation", reservation);
    return (BigDecimal) query.uniqueResult();
  }

  private BigDecimal getQtyReservedFromSD(Reservation reservation, Product product,
      AttributeSetInstance attribute, Locator storageBin) {
    final StringBuilder hqlString = new StringBuilder();
    hqlString
        .append("select coalesce(sum(rs.quantity - coalesce(rs.released,0)),0) from MaterialMgmtReservationStock rs ");
    hqlString.append("join rs.reservation as r ");
    hqlString.append(" where r.rESStatus not in ('CL', 'DR') ");
    hqlString.append(" and rs.salesOrderLine is null ");
    hqlString.append(" and r.product = :product ");
    hqlString.append(" and r <> :reservation ");
    if (attribute != null) {
      hqlString.append(" and rs.attributeSetValue = :attributeSetValue ");
    }
    if (storageBin != null) {
      hqlString.append(" and rs.storageBin = :storageBin ");
    }
    final Session session = OBDal.getInstance().getSession();
    Query query = session.createQuery(hqlString.toString());
    query.setParameter("product", product);
    query.setParameter("reservation", reservation);
    if (attribute != null) {
      query.setParameter("attributeSetValue", attribute);
    }
    if (storageBin != null) {
      query.setParameter("storageBin", storageBin);
    }
    return (BigDecimal) query.uniqueResult();
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
