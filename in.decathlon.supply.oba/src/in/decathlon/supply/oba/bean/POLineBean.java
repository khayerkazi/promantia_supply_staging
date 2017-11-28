package in.decathlon.supply.oba.bean;

import java.io.Serializable;

public class POLineBean implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	private String itemCode; 
	private Long orderedQty;
	private Long confirmedQty;
	private Boolean isCancelled;
	private Long cancelledQty;
	private String remarks;
	private String orderReferenceId;
	private Boolean toBeCancelled;
	
	
	
	public Boolean getToBeCancelled() {
		return toBeCancelled;
	}
	public void setToBeCancelled(Boolean toBeCancelled) {
		this.toBeCancelled = toBeCancelled;
	}
	public String getOrderReferenceId() {
		return orderReferenceId;
	}
	public void setOrderReferenceId(String orderReferenceId) {
		this.orderReferenceId = orderReferenceId;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	public Boolean getIsCancelled() {
		return isCancelled;
	}
	public void setIsCancelled(Boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	public Long getCancelledQty() {
		return cancelledQty;
	}
	public void setCancelledQty(Long cancelledQty) {
		this.cancelledQty = cancelledQty;
	}
	public String getItemCode() {
		return itemCode;
	}
	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}
	public Long getOrderedQty() {
		return orderedQty;
	}
	public void setOrderedQty(Long orderedQty) {
		this.orderedQty = orderedQty;
	}
	public Long getConfirmedQty() {
		return confirmedQty;
	}
	public void setConfirmedQty(Long confirmedQty) {
		this.confirmedQty = confirmedQty;
	}
	
	

}
