package org.openbravo.decathlon.warehouserules;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.jfree.util.Log;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.enterprise.WarehouseRule;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

public class PCBWarehouseRule extends WarehouseRuleImplementation {

  String monitorMessage;

  @Override
  /**
   * Applies when the Product is PCB.
   */
  public boolean doesApply(JSONObject purchaseOrderHeader, JSONObject purcharOrderLine) {
    boolean isPCBProduct = false;

    try {
      Product product = OBDal.getInstance().get(Product.class,
          purcharOrderLine.getString("product"));
      if (product.getClPcbQty() != null) {
        isPCBProduct = product.getClPcbQty().compareTo(BigDecimal.ZERO) == 1;
      }
    } catch (JSONException e) {
      Log.error(e.getMessage(), e);
    }

    if (isPCBProduct) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  /**
   * Returns PCB Warehouse Rule
   */
  public WarehouseRule getWarehouseRule() {
    OBCriteria<WarehouseRule> obc = OBDal.getInstance().createCriteria(WarehouseRule.class);
    obc.add(Restrictions.eq(WarehouseRule.PROPERTY_DWHRJAVAPROCEDURE, this.getClass().getName()));
    return (WarehouseRule) obc.uniqueResult();
  }

  @Override
  /**
   * Distribution logic for PCB Warehouse Rule
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
    BigDecimal origQty = BigDecimal.ZERO;
    BigDecimal qty = BigDecimal.ZERO;
    BigDecimal remainingQty = BigDecimal.ZERO;
    BigDecimal pcbQty = BigDecimal.ZERO;
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

      // Set starting Qty for PCB logic
      if (remainigQtyToBook != null && remainigQtyToBook.get("remainingQty") != null) {
        origQty = remainigQtyToBook.get("remainingQty");
      } else {
        origQty = new BigDecimal(purcharOrderLine.getString("orderedQuantity"));
      }

      pcbQty = product.getClPcbQty();
      remainingQty = origQty.remainder(pcbQty);
      qty = origQty.subtract(remainingQty);
      
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

    // Start the Message for the Monitor
    this.monitorMessage = this.monitorMessage
        + "\n**************************************************";
    this.monitorMessage = this.monitorMessage
        + "\n*             PCB Warehouse Rule                 *";
    this.monitorMessage = this.monitorMessage
        + "\n**************************************************";
    this.monitorMessage = this.monitorMessage + "\nPCB Quantity of Product: " + pcbQty;
    this.monitorMessage = this.monitorMessage + "\nRemaining Ordered Quantity of Product: "
        + origQty;
    this.monitorMessage = this.monitorMessage + "\nQuantity elegible for PCB: " + qty;

    //My Changes
    // Query to retrieve OnHand Warehouses
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

      this.monitorMessage = this.monitorMessage + "\n\tWarehouse Name: " + warehouse.getName()
          + "\tWarehouse Id: " + warehouse.getId();

      BigDecimal stockAvailable = getStockAvailable(warehouse, product, pcbQty);
      this.monitorMessage = this.monitorMessage + "\n\tTotal Available Stock: " + stockAvailable;

      if (stockAvailable.compareTo(BigDecimal.ZERO) == 1) {
        List<SalesOrderLineInformation> salesOrderLineInfoList = new ArrayList<WarehouseRuleImplementation.SalesOrderLineInformation>();
        if (salesOrderMap.containsKey(warehouse.getId())) {
          salesOrderLineInfoList = salesOrderMap.get(warehouse.getId());
        }
        if (stockAvailable.compareTo(qty) == 1) {
          salesOrderLineInfoList.add(new SalesOrderLineInformation(product, qty, warehouseRule,
              purcharOrderLine));
          alreadyReservedStock.put(warehouse, qty);
          qty = BigDecimal.ZERO;
        } else {
          salesOrderLineInfoList.add(new SalesOrderLineInformation(product, stockAvailable,
              warehouseRule, purcharOrderLine));
          alreadyReservedStock.put(warehouse, stockAvailable);
          qty = qty.subtract(stockAvailable);
        }
        salesOrderMap.put(warehouse.getId(), salesOrderLineInfoList);
      }
    }

    remainingQty = remainingQty.add(qty);
    this.monitorMessage = this.monitorMessage + "\nEnd of Loop";
    this.monitorMessage = this.monitorMessage + "\nRemaining Stock to distribute: " + remainingQty;
    this.monitorMessage = this.monitorMessage + "\n";
    String previousMessage = monitor.getMonitor();
    if (previousMessage != null) {
      this.monitorMessage = monitor.getMonitor() + this.monitorMessage;
    }
    remainigQtyToBook.put("remainingQty",remainingQty);
    monitor.setMonitor(this.monitorMessage);
    OBDal.getInstance().save(monitor);
    paramMonitor.put("monitor",monitor);
    return salesOrderMap;

  }

  /**
   * @return The Total Stock for a Product in a Warehouse that is equal or greater than the PCB
   *         quantity.
   */
  private BigDecimal getStockAvailable(Warehouse warehouse, Product product, BigDecimal pcbQty) {
    BigDecimal availableQty = BigDecimal.ZERO;

    StringBuffer select = new StringBuffer();

    select.append(" select sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE + ".id, sum(sd."
        + StorageDetail.PROPERTY_QUANTITYONHAND + ")");
    select.append(" from " + StorageDetail.ENTITY_NAME + " as sd");
    select.append("  join sd." + StorageDetail.PROPERTY_STORAGEBIN + " as l");
    select.append(" where sd." + StorageDetail.PROPERTY_PRODUCT + " = :product");
    select.append("   and sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE + ".id <> '0'");
    select.append("   and l." + org.openbravo.model.common.enterprise.Locator.PROPERTY_WAREHOUSE
        + " = :warehouse");
    select.append(" and (l." + org.openbravo.model.common.enterprise.Locator.PROPERTY_OBWHSTYPE
        + " = null or l." + org.openbravo.model.common.enterprise.Locator.PROPERTY_OBWHSTYPE
        + "<>'OUT')");
    select.append(" group by sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE + ".id ,sd."
        + StorageDetail.PROPERTY_PRODUCT + ", l."
        + org.openbravo.model.common.enterprise.Locator.PROPERTY_WAREHOUSE);
    select.append(" having sum (sd." + StorageDetail.PROPERTY_QUANTITYONHAND + ") = :pcbQty");

    Query qry = OBDal.getInstance().getSession().createQuery(select.toString());
    qry.setParameter("product", product);
    qry.setParameter("warehouse", warehouse);
    qry.setParameter("pcbQty", pcbQty);
    @SuppressWarnings("unchecked")
    List<Object[]> stocks = qry.list();
    if (stocks.size() > 0) {
      for (Object[] resultSet : stocks) {
        AttributeSetInstance attributeSetInstance = OBDal.getInstance().get(
            AttributeSetInstance.class, resultSet[0]);
        BigDecimal onHandQty = (BigDecimal) resultSet[1];
        BigDecimal reservedQty = getReservedStock(product, warehouse, attributeSetInstance);
        BigDecimal totalAvailableQty = onHandQty.subtract(reservedQty);
        BigDecimal pcbAvailableQty = totalAvailableQty
            .subtract(totalAvailableQty.remainder(pcbQty));
        availableQty = availableQty.add(pcbAvailableQty);

        this.monitorMessage = this.monitorMessage + "\n\t\tAttribute: "
            + attributeSetInstance.getIdentifier() + "\tOn Hand Stock: " + onHandQty
            + "\tReserved Stock: " + reservedQty + "\tPCB Available Stock: " + pcbAvailableQty
            + "\tId: " + resultSet[0];
      }
    }
    return availableQty;
  }

  /**
   * @return The Reserved Stock for a Product in a Warehouse grouped by attributeSetInstance
   */
  private BigDecimal getReservedStock(Product product, Warehouse warehouse,
      AttributeSetInstance attributeSetInstance) {
    BigDecimal reservedStock = BigDecimal.ZERO;

    StringBuffer select = new StringBuffer();

    select.append(" select rs." + ReservationStock.PROPERTY_ATTRIBUTESETVALUE + ".id, sum(coalesce(rs."
        + ReservationStock.PROPERTY_QUANTITY + ",0) - coalesce(rs." + ReservationStock.PROPERTY_RELEASED + ",0))");
    select.append(" from " + ReservationStock.ENTITY_NAME + " as rs");
    select.append("  join rs." + ReservationStock.PROPERTY_RESERVATION + " as r");
    select.append("  join rs." + ReservationStock.PROPERTY_STORAGEBIN + " as l");
    select.append(" where r." + Reservation.PROPERTY_PRODUCT + " = :product");
    select.append("   and r." + Reservation.PROPERTY_RESSTATUS + " not in ('CL','DR')");
    select.append("   and rs." + ReservationStock.PROPERTY_ATTRIBUTESETVALUE
        + " = :attributeSetInstance");
    select.append("   and l." + org.openbravo.model.common.enterprise.Locator.PROPERTY_WAREHOUSE
        + " = :warehouse");
    select.append(" and (l." + org.openbravo.model.common.enterprise.Locator.PROPERTY_OBWHSTYPE
        + " = null or l." + org.openbravo.model.common.enterprise.Locator.PROPERTY_OBWHSTYPE
        + "<>'OUT')");
    select.append(" group by rs." + ReservationStock.PROPERTY_ATTRIBUTESETVALUE + ".id ,r."
        + Reservation.PROPERTY_PRODUCT + ", l."
        + org.openbravo.model.common.enterprise.Locator.PROPERTY_WAREHOUSE);

    Query qry = OBDal.getInstance().getSession().createQuery(select.toString());
    qry.setParameter("product", product);
    qry.setParameter("attributeSetInstance", attributeSetInstance);
    qry.setParameter("warehouse", warehouse);
    @SuppressWarnings("unchecked")
    List<Object[]> stocks = qry.list();
    if (stocks.size() > 0) {
      for (Object[] resultSet : stocks) {
        reservedStock = reservedStock.add((BigDecimal) resultSet[1]);
      }
    }
    return reservedStock;
  }
}
