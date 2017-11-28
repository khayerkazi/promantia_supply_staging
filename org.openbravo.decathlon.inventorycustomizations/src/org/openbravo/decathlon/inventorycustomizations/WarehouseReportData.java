package org.openbravo.decathlon.inventorycustomizations;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

public class WarehouseReportData {

  public static String getOrganizationIdentifier(String ad_org_id) {
    if (ad_org_id == null) {
      return null;
    }
    return OBDal.getInstance().getProxy(Organization.ENTITY_NAME, ad_org_id).getIdentifier();
  }

  public static String getWarehouseIdentifier(String m_warehouse_id) {
    if (m_warehouse_id == null) {
      return null;
    }
    return OBDal.getInstance().getProxy(Warehouse.ENTITY_NAME, m_warehouse_id).getIdentifier();
  }

  public static String getCycleIdentifier(String dic_cycle_id) {
    if (dic_cycle_id == null) {
      return null;
    }
    return OBDal.getInstance().getProxy(Cycle.ENTITY_NAME, dic_cycle_id).getIdentifier();
  }

  public static String getAisleIdentifier(String dic_aisle_id) {
    if (dic_aisle_id == null) {
      return null;
    }
    return OBDal.getInstance().getProxy(Aisle.ENTITY_NAME, dic_aisle_id).getIdentifier();
  }
}
