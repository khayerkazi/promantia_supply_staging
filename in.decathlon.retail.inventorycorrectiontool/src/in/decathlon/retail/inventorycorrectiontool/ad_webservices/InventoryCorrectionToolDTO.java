package in.decathlon.retail.inventorycorrectiontool.ad_webservices;

import java.math.BigDecimal;

public class InventoryCorrectionToolDTO {
  private String adOrgId;

  private String adUserId;

  private String mProductId;

  private String mLocatorId;

  private String mAttributeSetInstanceId;

  private String inventoryType;

  public String getInventoryType() {
    return inventoryType;
  }

  public void setInventoryType(String inventoryType) {
    this.inventoryType = inventoryType;
  }

  public String getAdOrgId() {
    return adOrgId;
  }

  public void setAdOrgId(String adOrgId) {
    this.adOrgId = adOrgId;
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

  public String getmAttributeSetInstanceId() {
    return mAttributeSetInstanceId;
  }

  public void setmAttributeSetInstanceId(String mAttributeSetInstanceId) {
    this.mAttributeSetInstanceId = mAttributeSetInstanceId;
  }

  public BigDecimal getInvQty() {
    return invQty;
  }

  public void setInvQty(BigDecimal invQty) {
    this.invQty = invQty;
  }

  private BigDecimal invQty;

}
