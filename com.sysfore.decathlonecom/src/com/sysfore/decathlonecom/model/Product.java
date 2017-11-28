package com.sysfore.decathlonecom.model;

public class Product {

  private String productId;
  private String quantityOrdered;
  private String unitQty;
  private String unitPrice;
  private String ueQty;
  private String uePrice;
  private String pcbQty;
  private String pcbPrice;
  private String taxId;
  private String lineGrossAmt;
  private String lineNetAmt;
  private String grossUnitPrice;

  public String getLineGrossAmt() {
    return lineGrossAmt;
  }

  public void setLineGrossAmt(String lineGrossAmt) {
    this.lineGrossAmt = lineGrossAmt;
  }

  public String getLineNetAmt() {
    return lineNetAmt;
  }

  public void setLineNetAmt(String lineNetAmt) {
    this.lineNetAmt = lineNetAmt;
  }

  public String getGrossUnitPrice() {
    return grossUnitPrice;
  }

  public void setGrossUnitPrice(String grossUnitPrice) {
    this.grossUnitPrice = grossUnitPrice;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getQuantityOrdered() {
    return quantityOrdered;
  }

  public void setQuantityOrdered(String quantityOrdered) {
    this.quantityOrdered = quantityOrdered;
  }

  public String getUnitQty() {
    return unitQty;
  }

  public void setUnitQty(String unitQty) {
    this.unitQty = unitQty;
  }

  public String getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(String unitPrice) {
    this.unitPrice = unitPrice;
  }

  public String getUeQty() {
    return ueQty;
  }

  public void setUeQty(String ueQty) {
    this.ueQty = ueQty;
  }

  public String getUePrice() {
    return uePrice;
  }

  public void setUePrice(String uePrice) {
    this.uePrice = uePrice;
  }

  public String getPcbQty() {
    return pcbQty;
  }

  public void setPcbQty(String pcbQty) {
    this.pcbQty = pcbQty;
  }

  public String getPcbPrice() {
    return pcbPrice;
  }

  public void setPcbPrice(String pcbPrice) {
    this.pcbPrice = pcbPrice;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

}
