package in.decathlon.supply.oba.bean;

import java.io.Serializable;
import java.util.List;

public class POBean implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	private String documentNo;
	private String status;
	private List<POLineBean> poLineList;
	private String shippedDate;
	private String orderReference;
	
	
	
	public String getOrderReference() {
		return orderReference;
	}
	public void setOrderReference(String orderReference) {
		this.orderReference = orderReference;
	}
	public String getDocumentNo() {
		return documentNo;
	}
	public void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<POLineBean> getPoLineList() {
		return poLineList;
	}
	public void setPoLineList(List<POLineBean> poLineList) {
		this.poLineList = poLineList;
	}
	public String getShippedDate() {
		return shippedDate;
	}
	public void setShippedDate(String shippedDate) {
		this.shippedDate = shippedDate;
	}
	
	

}
