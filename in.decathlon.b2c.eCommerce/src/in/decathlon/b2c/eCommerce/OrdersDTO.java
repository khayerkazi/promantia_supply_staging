package in.decathlon.b2c.eCommerce;

public class OrdersDTO {
	
	private String orderId;
	private String orderDataId;
	private String orderDate;
	private String invoiceCustomer;
	private String linesAmount;
	private String custSatisfaction;
	private String shipCost;
	
	public String getShipCost() {
		return shipCost;
	}
	public void setShipCost(String shipCost) {
		this.shipCost = shipCost;
	}
	public String getCustSatisfaction() {
		return custSatisfaction;
	}
	public void setCustSatisfaction(String custSatisfaction) {
		this.custSatisfaction = custSatisfaction;
	}
	public String getLinesAmount() {
		return linesAmount;
	}
	public void setLinesAmount(String linesAmount) {
		this.linesAmount = linesAmount;
	}
	private String documentNumber;
	public String getDocumentNumber() {
		return documentNumber;
	}
	public void setDocumentNumber(String documentNumber) {
		this.documentNumber = documentNumber;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getOrderDataId() {
		return orderDataId;
	}
	public void setOrderDataId(String orderDataId) {
		this.orderDataId = orderDataId;
	}
	public String getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}
	public String getInvoiceCustomer() {
		return invoiceCustomer;
	}
	public void setInvoiceCustomer(String invoiceCustomer) {
		this.invoiceCustomer = invoiceCustomer;
	}
	

}
