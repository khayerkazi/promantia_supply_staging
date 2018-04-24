package org.openbravo.decathlon.warehouserules;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.openbravo.service.db.DalConnectionProvider;

public class WarehousePriorityWarehouseRule extends WarehouseRuleImplementation {

  String monitorMessage;

  @Override
  /**
   * Applies in two scenarios:
   *   - When the Order is not automatically generated and the Product is not PCB.
   *   - When the Order is automatically generated, the Product is not PCB and the Product is not Standard.
   */
  public boolean doesApply(JSONObject purchaseOrderHeader, JSONObject purcharOrderLine) {
    boolean isAutoSalesOrder = false;
    boolean isPCBProduct = false;
    boolean isStandard = false;
    String orgId;
    Organization purchaseOrg = null;
	try {
		orgId = purchaseOrderHeader.getString("organization");
	    purchaseOrg=OBDal.getInstance().get(Organization.class, orgId);
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    try {
      isAutoSalesOrder = "true".equals(purchaseOrderHeader.getString("swIsautoOrder"));
      Product product = OBDal.getInstance().get(Product.class,
          purcharOrderLine.getString("product"));
      if (product.getClPcbQty() != null) {
        isPCBProduct = product.getClPcbQty().compareTo(BigDecimal.ZERO) == 1;
        //System.out.println(product.getClLogRec());
        isStandard = STANDARD_LOGISTIC_RECHARGE.equals(product.getClLogRec())
            || SPECIAL_CATEGORY_LOGISTIC_RECHARGE.equals(product.getClLogRec());
      }
    } catch (JSONException e) {
      Log.error(e.getMessage(), e);
    }
    //if(!isStandard)
    if (!isStandard) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  /**
   * Returns WarehouseByPriority Warehouse Rule
   */
  public WarehouseRule getWarehouseRule() {
    OBCriteria<WarehouseRule> obc = OBDal.getInstance().createCriteria(WarehouseRule.class);
    obc.add(Restrictions.eq(WarehouseRule.PROPERTY_DWHRJAVAPROCEDURE, this.getClass().getName()));
    return (WarehouseRule) obc.uniqueResult();
  }

  @Override
  /**
   * Distribution logic for WarehouseByPriority Warehouse Rule
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

      // Set starting Qty for Warehouse Rule logic
      if (remainigQtyToBook != null && remainigQtyToBook.get("remainingQty") != null) {
        qty = remainigQtyToBook.get("remainingQty");
      } else {
        qty = new BigDecimal(purcharOrderLine.getString("orderedQuantity"));
      }

      // Initialize monitor
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

    // Start Monitor message
    this.monitorMessage = this.monitorMessage
        + "\n**************************************************";
    this.monitorMessage = this.monitorMessage
        + "\n*     Warehouse By Priority Warehouse Rule       *";
    this.monitorMessage = this.monitorMessage
        + "\n**************************************************";
    this.monitorMessage = this.monitorMessage + "\nRemaining Ordered Quantity of Product: " + qty;

    // Retrieve OnHand Warehouses
    OBCriteria<DwhrWarehouseConfig> obc = OBDal.getInstance().createCriteria(DwhrWarehouseConfig.class);
    obc.add(Restrictions.eq(DwhrWarehouseConfig.PROPERTY_WAREHOUSERULECONFIG, storeWarehouseRule));
    obc.addOrderBy(DwhrWarehouseConfig.PROPERTY_PRIORITY, true);

    this.monitorMessage = this.monitorMessage + "\nLoop On Hand Warehouses to retrieve Stock";

    // Distribute logic
    for (DwhrWarehouseConfig orgWarehouse : obc.list()) {
      // For each Warehouse on Hand (Warehouse Tab in Organization Window)
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
          // If the Warehouse already exist in the salesOrderMap, do not create a new record in the
          // list
          salesOrderLineInfoList = salesOrderMap.get(warehouse.getId());
        }
        if (stockAvailable.compareTo(qty) == 1) {
          // If there is more stock than qty to consume, consume it all
          salesOrderLineInfoList.add(new SalesOrderLineInformation(product, qty, warehouseRule,
              purcharOrderLine));
          qty = BigDecimal.ZERO;
        } else {
          // If there is more qty to consume than acutal stock, consume all the stock
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
       getStockPerLocatior(warehouse, product);
    return availableQty;
  }

  public void getStockPerLocatior(Warehouse warehouse, Product product) {

    String sql = "select sum(qtyonhand) as qtyonhand , sum(reservedqty) as reservedqty ,ml.value as locatorName	  from m_storage_detail sd 	  join m_locator ml on sd.m_locator_id=ml.m_locator_id join m_warehouse mw on ml.m_warehouse_id=mw.m_warehouse_id	  join m_product mp on sd.m_product_id=mp.m_product_id	  "
        + " where mw.m_warehouse_id= ? and  mp.m_product_id= ?"
        + "    and  ml.em_obwhs_type ='ST'	         and ml.isactive='Y' group by ml.value having sum(qtyonhand) > sum(reservedqty);";

    PreparedStatement sqlQuery = null;
    ResultSet rs = null;

    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);

      sqlQuery.setString(1, warehouse.getId());
      sqlQuery.setString(2, product.getId());
      sqlQuery.execute();
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        BigDecimal qtyonhand = rs.getBigDecimal("qtyonhand");
        BigDecimal reservedqty = rs.getBigDecimal("reservedqty");
        String locatorName = rs.getString("locatorName");
        BigDecimal availableQty = qtyonhand.subtract(reservedqty);

        this.monitorMessage = this.monitorMessage + "\n\t\t warehouse name: " + warehouse.getName()
            + " locatorName: " + locatorName + " On Hand Stock on locator : " + qtyonhand
            + " Reserved Stock on locator: " + reservedqty + " Total Available Stock: "
            + availableQty;

      }

    } catch (Exception e) {
       e.printStackTrace();
    }

  }
}
