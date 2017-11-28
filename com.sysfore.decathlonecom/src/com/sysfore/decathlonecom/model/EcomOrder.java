package com.sysfore.decathlonecom.model;

import java.util.List;

public class EcomOrder {
  private String orgName;
  private String customerId;
  private String firstName;
  private String lastName;
  private List<Product> itemOrdered;
  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private String billNo;
  private String orderDate;
  private String paymentMode;
  private String paymentIdentifier;
  private String paymentTotal;
  private String grantTotal;
  private String warehouseName;
  private String address1;
  private String address2;
  private String address3;
  private String address4;
  private String city;
  private String state;
  private String postal;
  private String country;
  private String chargeAmt;
  private String feedback;

  public String getChargeAmt() {
    return chargeAmt;
  }

  public void setChargeAmt(String chargeAmt) {
    this.chargeAmt = chargeAmt;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public List<Product> getItemOrdered() {
    return itemOrdered;
  }

  public void setItemOrdered(List<Product> itemOrdered) {
    this.itemOrdered = itemOrdered;
  }

  public String getBillNo() {
    return billNo;
  }

  public void setBillNo(String billNo) {
    this.billNo = billNo;
  }

  public String getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(String orderDate) {
    this.orderDate = orderDate;
  }

  public String getPaymentMode() {
    return paymentMode;
  }

  public void setPaymentMode(String paymentMode) {
    this.paymentMode = paymentMode;
  }

  public String getPaymentIdentifier() {
    return paymentIdentifier;
  }

  public void setPaymentIdentifier(String paymentIdentifier) {
    this.paymentIdentifier = paymentIdentifier;
  }

  public String getPaymentTotal() {
    return paymentTotal;
  }

  public void setPaymentTotal(String paymentTotal) {
    this.paymentTotal = paymentTotal;
  }

  public String getGrantTotal() {
    return grantTotal;
  }

  public void setGrantTotal(String grantTotal) {
    this.grantTotal = grantTotal;
  }

  public String getWarehouseName() {
    return warehouseName;
  }

  public void setWarehouseName(String warehouseName) {
    this.warehouseName = warehouseName;
  }

  public String getAddress1() {
    return address1;
  }

  public void setAddress1(String address1) {
    this.address1 = address1;
  }

  public String getAddress2() {
    return address2;
  }

  public void setAddress2(String address2) {
    this.address2 = address2;
  }

  public String getAddress3() {
    return address3;
  }

  public void setAddress3(String address3) {
    this.address3 = address3;
  }

  public String getAddress4() {
    return address4;
  }

  public void setAddress4(String address4) {
    this.address4 = address4;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getPostal() {
    return postal;
  }

  public void setPostal(String postal) {
    this.postal = postal;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

	public String getFeedback() {
    return feedback;
  }

  public void setFeedback(String feedback) {
    this.feedback = feedback;
  }

}
