package in.decathlon.supply.oba.bean;

import java.io.Serializable;
import java.util.Date;

public class ShipmentBean implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	private String documentNo;
	private String itemCode;
	private Long shippedQty;
	private String packagingInfo;
	private Date shippedDate;
	private Date deliveryDate;
	
	
	public Date getShippedDate() {
		return shippedDate;
	}
	public void setShippedDate(Date shippedDate) {
		this.shippedDate = shippedDate;
	}
	public Date getDeliveryDate() {
		return deliveryDate;
	}
	public void setDeliveryDate(Date deliveryDate) {
		this.deliveryDate = deliveryDate;
	}
	public String getDocumentNo() {
		return documentNo;
	}
	public void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}
	public String getItemCode() {
		return itemCode;
	}
	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}
	public Long getShippedQty() {
		return shippedQty;
	}
	public void setShippedQty(Long shippedQty) {
		this.shippedQty = shippedQty;
	}
	public String getPackagingInfo() {
		return packagingInfo;
	}
	public void setPackagingInfo(String packagingInfo) {
		this.packagingInfo = packagingInfo;
	}
	
	
}
