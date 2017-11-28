package in.decathlon.retail.stockreception.ad_webservice;

public class GetTruckBean {
  private String truckNo;
  private String boxNo;
  private String movementDate;
  private String itemCode;
  private String brandName;
  private String modelName;

  public String getBrandName() {
    return brandName;
  }

  public void setBrandName(String brandName) {
    this.brandName = brandName;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  private String movementQuantity;
  private String boxStatus;

  public String getBoxStatus() {
    return boxStatus;
  }

  public void setBoxStatus(String boxStatus) {
    this.boxStatus = boxStatus;
  }

  public String getTruckNo() {
    return truckNo;
  }

  public void setTruckNo(String truckNo) {
    this.truckNo = truckNo;
  }

  public String getBoxNo() {
    return boxNo;
  }

  public void setBoxNo(String boxNo) {
    this.boxNo = boxNo;
  }

  public String getMovementDate() {
    return movementDate;
  }

  public void setMovementDate(String movementDate) {
    this.movementDate = movementDate;
  }

  public String getItemCode() {
    return itemCode;
  }

  public void setItemCode(String itemCode) {
    this.itemCode = itemCode;
  }

  public String getMovementQuantity() {
    return movementQuantity;
  }

  public void setMovementQuantity(String movementQuantity) {
    this.movementQuantity = movementQuantity;
  }

}
