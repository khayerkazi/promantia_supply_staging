package in.nous.creditnote.ad_process;

import java.math.BigDecimal;
import java.util.Date;

public class CreditNoteDTO {
  private String adOrgId;

  private String cBPartnerId;

  private String adUserId;

  private String mProductId;

  private String mLocatorId;

  private String decathlonId;

  private String OrderId;// orderLineId

  private String billNo;

  private Date billDate;

  private String buttonClicked;

  private BigDecimal unitPrice;

  private BigDecimal totalAmt;

  private BigDecimal exchangeQty;

  private BigDecimal exchangeAmt;

  private BigDecimal purchaseQty;

  public BigDecimal getExchangeAmt() {
    return exchangeAmt;
  }

  public void setExchangeAmt(BigDecimal exchangeAmt) {
    this.exchangeAmt = exchangeAmt;
  }

  public String getDecathlonId() {
    return decathlonId;
  }

  public void setDecathlonId(String decathlonId) {
    this.decathlonId = decathlonId;
  }

  public String getOrderId() {
    return OrderId;
  }

  public void setOrderId(String orderId) {
    OrderId = orderId;
  }

  public String getAdOrgId() {
    return adOrgId;
  }

  public void setAdOrgId(String adOrgId) {
    this.adOrgId = adOrgId;
  }

  public String getcBPartnerId() {
    return cBPartnerId;
  }

  public void setcBPartnerId(String cBPartnerId) {
    this.cBPartnerId = cBPartnerId;
  }

  public String getAdUserId() {
    return adUserId;
  }

  public void setAdUserId(String adUserId) {
    this.adUserId = adUserId;
  }

  public String getmProductId() {
    return mProductId;
  }

  public void setmProductId(String mProductId) {
    this.mProductId = mProductId;
  }

  public String getmLocatorId() {
    return mLocatorId;
  }

  public void setmLocatorId(String mLocatorId) {
    this.mLocatorId = mLocatorId;
  }

  public String getBillNo() {
    return billNo;
  }

  public void setBillNo(String billNo) {
    this.billNo = billNo;
  }

  public Date getBillDate() {
    return billDate;
  }

  public void setBillDate(Date billDate) {
    this.billDate = billDate;
  }

  public String getButtonClicked() {
    return buttonClicked;
  }

  public void setButtonClicked(String buttonClicked) {
    this.buttonClicked = buttonClicked;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getTotalAmt() {
    return totalAmt;
  }

  public void setTotalAmt(BigDecimal totalAmt) {
    this.totalAmt = totalAmt;
  }

  public BigDecimal getExchangeQty() {
    return exchangeQty;
  }

  public void setExchangeQty(BigDecimal exchangeQty) {
    this.exchangeQty = exchangeQty;
  }

  public BigDecimal getPurchaseQty() {
    return purchaseQty;
  }

  public void setPurchaseQty(BigDecimal purchaseQty) {
    this.purchaseQty = purchaseQty;
  }

}