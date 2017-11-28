package com.sysfore.decathlonecom.ad_webservices;

import java.util.List;

public class EcomCustomerRegisterDTO {

  private String greeting;
  private String firstName;
  private String lastName;
  private String email;
  private String mobile;
  private String company;
  private String oxylane;
  private String status;
  private String greetingId;
  private String companyId;
  private String licenseId;
  private String licenseNo;
  private String companyAddress;
  private String memberType = "Y";

  public String getMemberType() {
    return memberType;
  }

  public void setMemberType(String memberType) {
    this.memberType = memberType;
  }

  public String getGreetingId() {
    return greetingId;
  }

  public void setGreetingId(String greetingId) {
    this.greetingId = greetingId;
  }

  public String getCompanyId() {
    return companyId;
  }

  public void setCompanyId(String companyId) {
    this.companyId = companyId;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }

  public String getLicenseNo() {
    return licenseNo;
  }

  public void setLicenseNo(String licenseNo) {
    this.licenseNo = licenseNo;
  }

  public String getCompanyAddress() {
    return companyAddress;
  }

  public void setCompanyAddress(String companyAddress) {
    this.companyAddress = companyAddress;
  }

  private List<EcomAddressDTO> ecomAddress;

  public List<EcomAddressDTO> getEcomAddress() {
    return ecomAddress;
  }

  public void setEcomAddress(List<EcomAddressDTO> ecomAddress) {
    this.ecomAddress = ecomAddress;
  }

  public String getGreeting() {
    return greeting;
  }

  public void setGreeting(String greeting) {
    this.greeting = greeting;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getMobile() {
    return mobile;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getOxylane() {
    return oxylane;
  }

  public void setOxylane(String oxylane) {
    this.oxylane = oxylane;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSports() {
    return sports;
  }

  public void setSports(String sports) {
    this.sports = sports;
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

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
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

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getOptIn() {
    return optIn;
  }

  public void setOptIn(String optIn) {
    this.optIn = optIn;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  private String sports;
  private String address1;
  private String address2;
  private String address3;
  private String address4;
  private String postalCode;
  private String city;
  private String state;
  private String country;
  private String optIn;
  private String source;
  private String comments = "";

}
