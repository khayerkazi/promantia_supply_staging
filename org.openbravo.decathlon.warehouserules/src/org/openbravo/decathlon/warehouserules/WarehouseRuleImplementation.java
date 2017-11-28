package org.openbravo.decathlon.warehouserules;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.enterprise.WarehouseRule;
import org.openbravo.model.common.plm.Product;

/**
 * Abstract Class for implementing Java logic for Warehouse Rules.
 * 
 * Abstract Methods to implement by inheriting classes: doesApply, distribute and getWarehouseRule.
 * 
 * Inner class contained: SalesOrderLineInformation
 * 
 * public Methods: getActuallQty and getReservedQty
 */
public abstract class WarehouseRuleImplementation {

  // Global variables for Decathlon
  public static final String STANDARD_LOGISTIC_RECHARGE = "9.7";
  public static final String SPECIAL_CATEGORY_LOGISTIC_RECHARGE = "5";

  /**
   * Returns True if the Warehouse Rule applies, false if not.
   * 
   * @param purchaseOrderHeader
   *          . JSONObject with the Purchase Order Header Information
   * @param purcharOrderLine
   *          . JSONObject with the Purchase Order Lines Information
   * @return boolean. True if the Warehouse Rule applies, false if not.
   */
  public abstract boolean doesApply(JSONObject purchaseOrderHeader, JSONObject purcharOrderLine);

  /**
   * Distributes the Sales Order Line quantity base on the Warehouse Rule Logic
   * 
   * @param purchaseOrderHeader
   *          . JSONObject with the Purchase Order Header Information
   * @param purcharOrderLine
   *          . JSONObject with the Purchase Order Lines Information
   * @param salesOrdersMap
   *          . Data containing the previous distribution of the Sales Order Line
   * @param warehouseRule
   *          . The actual Warehouse Rule
   * @param qty
   *          . Optional. Used when not all the quantity of the Sales Order Line must be distributed
   * @param qty
   *          . Optional. Used when a Warehouse Rule calls another one, it can tell how much
   *          quantity has already been reserved in the Warehouse
   * @return A HashMap with the data containing the distribution of the Sales Order Line
   */
  public abstract HashMap<String, List<SalesOrderLineInformation>> distribute(
      JSONObject purchaseOrderHeader, JSONObject purcharOrderLine,
      HashMap<String, List<SalesOrderLineInformation>> salesOrdersMap,DwhrWhruleConfig storeWarehouseRule,WarehouseRule warehouseRule,
      HashMap<String, BigDecimal> remainigQtyToBook, HashMap<Warehouse, BigDecimal> alreadyReservedStock,
      HashMap<String,DWHR_DistributeMonitor> monitor);

  /**
   * @return The actual Warehouse Rule
   */
  public abstract WarehouseRule getWarehouseRule();

  /**
   * Inner class used to store the distribution data
   * 
   */
  public class SalesOrderLineInformation {

    Product product;
    BigDecimal quantity;
    WarehouseRule warehouseRule;
    JSONObject orderLineJSON;

    public SalesOrderLineInformation(Product product, BigDecimal quantity,
        WarehouseRule warehouseRule, JSONObject orderLineJSON) {
      super();
      this.product = product;
      this.quantity = quantity;
      this.warehouseRule = warehouseRule;
      this.orderLineJSON = orderLineJSON;
    }

    public Product getProduct() {
      return product;
    }

    public void setProduct(Product product) {
      this.product = product;
    }

    public BigDecimal getQuantity() {
      return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
      this.quantity = quantity;
    }

    public WarehouseRule getWarehouseRule() {
      return warehouseRule;
    }

    public void setWarehouseRule(WarehouseRule warehouseRule) {
      this.warehouseRule = warehouseRule;
    }

    public JSONObject getOrderLineJSON() {
      return orderLineJSON;
    }

    public void setOrderLineJSON(JSONObject orderLineJSON) {
      this.orderLineJSON = orderLineJSON;
    }
  }

  /**
   * @return The Total Stock for a Product in a Warehouse
   */
  public BigDecimal getActuallQty(Warehouse warehouse, Product product) {
    BigDecimal totalQty = BigDecimal.ZERO;
    String qry = "select sum(quantityOnHand) from MaterialMgmtStorageDetail sd where sd.storageBin.warehouse.id="
        + " :warehouseId and sd.product.id= :productId and (sd.storageBin.oBWHSType = null or sd.storageBin.oBWHSType<>'OUT' or sd.storageBin.oBWHSType<>'ARC') and sd.storageBin.active='Y'";
    Query storageQuery = OBDal.getInstance().getSession().createQuery(qry);
    storageQuery.setParameter("warehouseId", warehouse.getId());
    storageQuery.setParameter("productId", product.getId());
    @SuppressWarnings("unchecked")
    List<BigDecimal> qryResult = storageQuery.list();
    if (qryResult != null && qryResult.size() > 0) {
      totalQty = qryResult.get(0);
    }
    if (totalQty != null)
      return totalQty;
    else
      return BigDecimal.ZERO;
  }

  /**
   * @return The Reserved Stock for a Product in a Warehouse
   */
  public BigDecimal getReservedQty(Warehouse warehouse, Product product) {
    BigDecimal resrvdQty = BigDecimal.ZERO;
    String qry = "select sum(reservedQty) from MaterialMgmtStorageDetail sd where sd.storageBin.warehouse.id="
        + " :warehouseId and sd.product.id= :productId and (sd.storageBin.oBWHSType = null or sd.storageBin.oBWHSType<>'OUT' or sd.storageBin.oBWHSType<>'ARC') and sd.storageBin.active='Y'";
    Query storageQuery = OBDal.getInstance().getSession().createQuery(qry);
    storageQuery.setParameter("warehouseId", warehouse.getId());
    storageQuery.setParameter("productId", product.getId());
    @SuppressWarnings("unchecked")
    List<BigDecimal> qryResult = storageQuery.list();
    if (qryResult != null && qryResult.size() > 0) {
      resrvdQty = qryResult.get(0);
    }
    if (resrvdQty != null)
      return resrvdQty;
    else
      return BigDecimal.ZERO;
  }

  protected DWHR_DistributeMonitor createNewMonitor(Organization organization, Product product,
      String strPurchaseOrderId, String purchaseOrderIdentifier, String strPurchaseOrderLineId) {
    DWHR_DistributeMonitor monitor = OBProvider.getInstance().get(DWHR_DistributeMonitor.class);
    monitor.setClient(organization.getClient());
    monitor.setOrganization(organization);
    monitor.setProduct(product);
    monitor.setPurchaseOrd(strPurchaseOrderId);
    monitor.setPurchaseOrdIdentifier(purchaseOrderIdentifier);
    monitor.setLine(strPurchaseOrderLineId);
    OBDal.getInstance().save(monitor);
    return monitor;
  }

}
