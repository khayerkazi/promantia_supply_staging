package org.openbravo.decathlon.warehouserules;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.jfree.util.Log;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.enterprise.WarehouseRule;
import org.openbravo.model.common.plm.Product;

public class StandardWarehouseRule extends WarehouseRuleImplementation {

  String monitorMessage;

  @Override
  /**
   * Applies when the Order is automatically generated, the Product is not PCB and the Product is Standard.
   */
  public boolean doesApply(JSONObject purchaseOrderHeader, JSONObject purcharOrderLine) {
    boolean isAutoSalesOrder = false;
    boolean isPCBProduct = false;
    boolean isStandard = false;

    try {
      isAutoSalesOrder = "true".equals(purchaseOrderHeader.getString("swIsautoOrder"));
      Product product = OBDal.getInstance().get(Product.class,
          purcharOrderLine.getString("product"));
      if (product.getClPcbQty() != null) {
        isPCBProduct = product.getClPcbQty().compareTo(BigDecimal.ZERO) == 1;
        isStandard = STANDARD_LOGISTIC_RECHARGE.equals(product.getClLogRec())
            || SPECIAL_CATEGORY_LOGISTIC_RECHARGE.equals(product.getClLogRec());
      }
    } catch (JSONException e) {
      Log.error(e.getMessage(), e);
    }

    if (isStandard) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  /**
   * Returns Standard Warehouse Rule
   */
  public WarehouseRule getWarehouseRule() {
    OBCriteria<WarehouseRule> obc = OBDal.getInstance().createCriteria(WarehouseRule.class);
    obc.add(Restrictions.eq(WarehouseRule.PROPERTY_DWHRJAVAPROCEDURE, this.getClass().getName()));
    return (WarehouseRule) obc.uniqueResult();
  }

  @Override
  /**
   * Distribution logic for Standard Warehouse Rule
   * 
   * Returns a HashMap<String, List<SalesOrderLineInformation>> with the distributed quantities data.
   */
  public HashMap<String, List<SalesOrderLineInformation>> distribute(
      JSONObject purchaseOrderHeader, JSONObject purcharOrderLine,
      HashMap<String, List<SalesOrderLineInformation>> salesOrdersMapParameter,DwhrWhruleConfig storeWarehouseRule,
      WarehouseRule warehouseRule, HashMap<String, BigDecimal> remainigQtyToBook,
      HashMap<Warehouse, BigDecimal> alreadyReservedStock, HashMap<String,DWHR_DistributeMonitor> paramMonitor) {

    // Initialize some variables
    HashMap<String, List<SalesOrderLineInformation>> salesOrderMap = salesOrdersMapParameter;
    Organization organization = null;
    Product product = null;
    BigDecimal qty = BigDecimal.ZERO;
    DWHR_DistributeMonitor monitor = null;
    this.monitorMessage = "";

    try {
      // Retrieve data from JSON Object
      organization = BusinessEntityMapper.getOrgOfBP(purchaseOrderHeader
          .getString("businessPartner"));
      product = OBDal.getInstance().get(Product.class, purcharOrderLine.getString("product"));
      String strPurchaseOrderId = purchaseOrderHeader.getString("id");
      String purchaseOrderIdentifier = purchaseOrderHeader.getString("documentNo");
      String strPurchaseOrderLineId = purcharOrderLine.getString("id");

      // Set starting Quantity
      if (remainigQtyToBook != null && remainigQtyToBook.get("remainingQty") != null) {
        qty = remainigQtyToBook.get("remainingQty");
      } else {
        qty = new BigDecimal(purcharOrderLine.getString("orderedQuantity"));
      }


      // Initialize Monitor
      if (paramMonitor.get("monitor") == null) {
        monitor = createNewMonitor(organization, product, strPurchaseOrderId,
            purchaseOrderIdentifier, strPurchaseOrderLineId);
      } else {
        monitor = paramMonitor.get("monitor");
      }
    } catch (JSONException e) {
      Log.error("DefaultWarehouseRule.java distribute method", e);
    } catch (Exception e) {
      Log.error("DefaultWarehouseRule.java distribute method", e);
    }

    // Starting Monitor Message
    this.monitorMessage = this.monitorMessage
        + "\n**************************************************";
    this.monitorMessage = this.monitorMessage
        + "\n*           Standard Warehouse Rule               *";
    this.monitorMessage = this.monitorMessage
        + "\n**************************************************";
    this.monitorMessage = this.monitorMessage + "\nLogicstic value for Product: "
        + product.getClLogRec();
    this.monitorMessage = this.monitorMessage + "\nRemaining Ordered Quantity of Product: " + qty;

    // Retrieve OnHand Warehouses
    OBCriteria<DwhrWarehouseConfig> obc = OBDal.getInstance().createCriteria(DwhrWarehouseConfig.class);
    obc.add(Restrictions.eq(DwhrWarehouseConfig.PROPERTY_WAREHOUSERULECONFIG, storeWarehouseRule));
    obc.addOrderBy(DwhrWarehouseConfig.PROPERTY_PRIORITY, true);

    this.monitorMessage = this.monitorMessage + "\nLoop On Hand Warehouses to retrieve Stock";

    // Distribution logic
    for (DwhrWarehouseConfig orgWarehouse : obc.list()) {
      if (qty.compareTo(BigDecimal.ZERO) == 0) {
        break;
      }

      Warehouse warehouse = orgWarehouse.getWarehouse();

      // Retrieve previously reserved Stock if any
      BigDecimal reservedStock = BigDecimal.ZERO;
      if (alreadyReservedStock != null && alreadyReservedStock.get(warehouse) != null) {
        reservedStock = alreadyReservedStock.get(warehouse);
      }

      this.monitorMessage = this.monitorMessage + "\n\tWarehouse Name: " + warehouse.getName()
          + "\tWarehouse Id: " + warehouse.getId();

      BigDecimal stockAvailable = getStockAvailable(warehouse, product, reservedStock);
      this.monitorMessage = this.monitorMessage + "\n\tTotal Available Stock: " + stockAvailable;

      if (stockAvailable.compareTo(BigDecimal.ZERO) == 1) {
        List<SalesOrderLineInformation> salesOrderLineInfoList = new ArrayList<WarehouseRuleImplementation.SalesOrderLineInformation>();
        if (salesOrderMap.containsKey(warehouse.getId())) {
          salesOrderLineInfoList = salesOrderMap.get(warehouse.getId());
        }
        if (stockAvailable.compareTo(qty) == 1) {
          salesOrderLineInfoList.add(new SalesOrderLineInformation(product, qty, warehouseRule,
              purcharOrderLine));
          qty = BigDecimal.ZERO;
        } else {
          salesOrderLineInfoList.add(new SalesOrderLineInformation(product, stockAvailable,
              warehouseRule, purcharOrderLine));
          qty = qty.subtract(stockAvailable);
        }
        salesOrderMap.put(warehouse.getId(), salesOrderLineInfoList);
      }
    }

    this.monitorMessage = this.monitorMessage + "\nEnd of Loop";
    this.monitorMessage = this.monitorMessage + "\nRemaining Stock to distribute: " + qty;
    this.monitorMessage = this.monitorMessage + "\n";
    String previousMessage = monitor.getMonitor();
    if (previousMessage != null) {
      this.monitorMessage = monitor.getMonitor() + this.monitorMessage;
    }
    remainigQtyToBook.put("remainingQty",qty);
    monitor.setMonitor(this.monitorMessage);
    OBDal.getInstance().save(monitor);
    paramMonitor.put("monitor",monitor);

    return salesOrderMap;

  }

  /**
   * @return The available stock for a Product in a Warehouse (Total Stock - Reserved Stock)
   */
  private BigDecimal getStockAvailable(Warehouse warehouse, Product product,
      BigDecimal alreadyReservedStock) {
    BigDecimal reservedQty = getReservedQty(warehouse, product).add(alreadyReservedStock);
    BigDecimal qtyInWarehouse = getActuallQty(warehouse, product);
    BigDecimal availableQty = qtyInWarehouse.subtract(reservedQty);

    this.monitorMessage = this.monitorMessage + "\n\t\tTotal Available Stock: " + availableQty
        + "\tOn Hand Stock: " + qtyInWarehouse + "\tReserved Stock: " + reservedQty;
    return availableQty;
  }

}
