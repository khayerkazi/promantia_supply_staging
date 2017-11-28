package org.openbravo.mobile.warehouse.picking.datasource;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;

@Qualifier(OBMWHP_materialMgmtInternalMovementLine_dataSource.movementLinesExtension)
public class OBMWHP_materialMgmtInternalMovementLine_dataSource_properties extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      private static final String alias = "movLine.";
      {
        add(new HQLProperty(alias + "id", "id"));
        add(new HQLProperty(alias + "client.id", "client"));
        add(new HQLProperty(alias + "client.name", "client$_identifier"));
        add(new HQLProperty(alias + "organization.id", "organization"));
        add(new HQLProperty(alias + "organization.name", "organization$_identifier"));
        add(new HQLProperty(alias + "creationDate", "creationDate"));
        add(new HQLProperty(alias + "movement.id", "movement"));
        add(new HQLProperty(alias + "movement.name", "movement$_identifier"));
        add(new HQLProperty(alias + "storageBin.id", "storageBin"));
        add(new HQLProperty(alias + "storageBin.searchKey", "storageBin$_identifier"));
        add(new HQLProperty(alias + "newStorageBin.id", "newStorageBin"));
        add(new HQLProperty(alias + "newStorageBin.searchKey", "newStorageBin$_identifier"));
        add(new HQLProperty("prd.id", "product"));
        add(new HQLProperty("prd.name", "product$_identifier"));
        add(new HQLProperty(alias + "lineNo", "lineNo"));
        add(new HQLProperty(alias + "movementQuantity", "movementQuantity"));
        add(new HQLProperty("(CASE WHEN " + alias
            + "salesOrderLine.id IS NOT NULL THEN sol.salesOrder.id ELSE null END)", "salesOrderId"));
        // add(new HQLProperty("(CASE WHEN " + alias
        // + "salesOrderLine.id IS NOT NULL THEN so.documentNo ELSE null END)",
        // "salesOrder$_identifier"));
        // add(new HQLProperty("(CASE WHEN " + alias
        // + "salesOrderLine.id IS NOT NULL THEN bp.id ELSE null END)",
        // "salesOrderBusinessPartnerId"));
        // add(new HQLProperty("(CASE WHEN " + alias
        // + "salesOrderLine.id IS NOT NULL THEN bp.name ELSE null END)",
        // "salesOrderBusinessPartner$_identifier"));
        add(new HQLProperty(alias + "description", "description"));
        add(new HQLProperty(alias + "attributeSetValue.id", "attributeSetValue"));
        add(new HQLProperty(alias + "attributeSetValue.lotName", "attributeSetValue$_identifier"));
        add(new HQLProperty(alias + "oBWPLWarehousePickingList.id", "oBWPLWarehousePickingList"));
        add(new HQLProperty(alias + "oBWPLWarehousePickingList.obmwhpMobileidentifier",
            "oBWPLWarehousePickingList$_identifier"));
        add(new HQLProperty(alias + "oBWPLGroupPickinglist.id", "oBWPLGroupPickinglist"));
        add(new HQLProperty(alias + "oBWPLComplete", "oBWPLComplete"));
        add(new HQLProperty(alias + "oBWPLItemStatus", "oBWPLItemStatus"));
        add(new HQLProperty(alias + "oBWPLEditItem", "oBWPLEditItem"));
        add(new HQLProperty(alias + "oBWPLAllowDelete", "oBWPLAllowDelete"));
        add(new HQLProperty(alias + "oBWPLRaiseIncidence", "oBWPLRaiseIncidence"));
        add(new HQLProperty(alias + "oBWPLIncidenceReason", "oBWPLIncidenceReason"));
        add(new HQLProperty(alias + "oBWPLReject", "oBWPLReject"));
        add(new HQLProperty(alias + "oBWPLPickedqty", "oBWPLPickedqty"));
        add(new HQLProperty(alias + "obwplPickinglistproblem.id", "obwplPickinglistproblem"));
        add(new HQLProperty(alias + "obwplPlproblemgenerator.id", "obwplPlproblemgenerator"));
      }
    };
    return list;
  }
}