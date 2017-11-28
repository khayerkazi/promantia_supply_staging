package in.decathlon.retail.api.omniCommerce.ad_webservice;

/**
 * A Data Transfer class for Orderline
 * 
 */

public class OrderlineDTO {

  private int line;

  private int qtyOrdered;

  private double priceActual;

  private String taxId;

  private String supplierCode;

  private String itemCode;

  private int confirmedQty;

  public int getLine() {
    return line;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public int getQtyOrdered() {
    return qtyOrdered;
  }

  public void setQtyOrdered(int qtyOrdered) {
    this.qtyOrdered = qtyOrdered;
  }

  public double getPriceActual() {
    return priceActual;
  }

  public void setPriceActual(double priceActual) {
    this.priceActual = priceActual;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getSupplierCode() {
    return supplierCode;
  }

  public void setSupplierCode(String supplierCode) {
    this.supplierCode = supplierCode;
  }

  public String getItemCode() {
    return itemCode;
  }

  public void setItemCode(String itemCode) {
    this.itemCode = itemCode;
  }

  public int getConfirmedQty() {
    return confirmedQty;
  }

  public void setConfirmedQty(int confirmedQty) {
    this.confirmedQty = confirmedQty;
  }

}
