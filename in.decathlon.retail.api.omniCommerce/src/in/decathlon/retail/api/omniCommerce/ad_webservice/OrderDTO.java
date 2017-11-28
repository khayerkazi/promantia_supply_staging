package in.decathlon.retail.api.omniCommerce.ad_webservice;

import java.util.List;

/**
 * A Data Transfer class for Order
 * 
 */

public class OrderDTO {

  private String adClientId;

  private String adOrgId;

  private String createdBy;

  private String updatedBy;

  private String bpartnerId;

  private String billtoId;

  private String currencyId;

  private String pricelistId;

  private String modelCode;

  private String pricelistVer;

  private String currency;

  private String warehouseId;

  private List<OrderlineDTO> listOrderlineDTOs;

  private String successStatus;

  private String cOrderId;

  private String documentNo;

  private String storeName;

  private String poReference;

  private String email;

  private String brand;

  private String confirmedQty;

  private String linesInfo;

  private String decathlonId;

  private String addressId;

  private String orderType;

  private String orderSubType;
  private String posOrderNumber;

  public String getOrderSubType() {
    return orderSubType;
  }

  public void setOrderSubType(String orderSubType) {
    this.orderSubType = orderSubType;
  }

  public String getPosOrderNumber() {
    return posOrderNumber;
  }

  public void setPosOrderNumber(String posOrderNumber) {
    this.posOrderNumber = posOrderNumber;
  }

  public String getDecathlonId() {
    return decathlonId;
  }

  public void setDecathlonId(String decathlonId) {
    this.decathlonId = decathlonId;
  }

  public String getAddressId() {
    return addressId;
  }

  public void setAddressId(String addressId) {
    this.addressId = addressId;
  }

  public String getOrderType() {
    return orderType;
  }

  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  public String getLinesInfo() {
    return linesInfo;
  }

  public void setLinesInfo(String linesInfo) {
    this.linesInfo = linesInfo;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getAdClientId() {
    return adClientId;
  }

  public void setAdClientId(String adClientId) {
    this.adClientId = adClientId;
  }

  public String getAdOrgId() {
    return adOrgId;
  }

  public void setAdOrgId(String adOrgId) {
    this.adOrgId = adOrgId;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public String getBpartnerId() {
    return bpartnerId;
  }

  public void setBpartnerId(String bpartnerId) {
    this.bpartnerId = bpartnerId;
  }

  public String getBilltoId() {
    return billtoId;
  }

  public void setBilltoId(String billtoId) {
    this.billtoId = billtoId;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storename) {
    this.storeName = storename;
  }

  public String getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(String currencyId) {
    this.currencyId = currencyId;
  }

  public String getPricelistId() {
    return pricelistId;
  }

  public void setPricelistId(String pricelistId) {
    this.pricelistId = pricelistId;
  }

  public String getModelCode() {
    return modelCode;
  }

  public void setModelCode(String modelCode) {
    this.modelCode = modelCode;
  }

  public String getPricelistVer() {
    return pricelistVer;
  }

  public void setPricelistVer(String pricelistVer) {
    this.pricelistVer = pricelistVer;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getWarehouseId() {
    return warehouseId;
  }

  public void setWarehouseId(String warehouseId) {
    this.warehouseId = warehouseId;
  }

  public List<OrderlineDTO> getListOrderlineDTOs() {
    return listOrderlineDTOs;
  }

  public void setListOrderlineDTOs(List<OrderlineDTO> listOrderlineDTOs) {
    this.listOrderlineDTOs = listOrderlineDTOs;
  }

  public String getSuccessStatus() {
    return successStatus;
  }

  public void setSuccessStatus(String successStatus) {
    this.successStatus = successStatus;
  }

  public String getCOrderId() {
    return cOrderId;
  }

  public void setCOrderId(String cOrderId) {
    this.cOrderId = cOrderId;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public void setDocumentNo(String documentNo) {
    this.documentNo = documentNo;
  }

  public String getPoReference() {
    return poReference;
  }

  public void setPoReference(String poReference) {
    this.poReference = poReference;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getConfirmedQty() {
    return confirmedQty;
  }

  public void setConfirmedQty(String confirmedQty) {
    this.confirmedQty = confirmedQty;
  }
}
