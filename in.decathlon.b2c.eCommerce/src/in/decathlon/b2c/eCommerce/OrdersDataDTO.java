package in.decathlon.b2c.eCommerce;

public class OrdersDataDTO {

	private String itemCode;
	private String itemCost;
	private String orderItemId;
	private String payTotal;
	private String quantity;
	private String shippedTotal;
	private String shipTax;
	private String productId;
	private String orderDate;
	private String confirmedQty;
	private String taxAmount;
	
	public String getTaxAmount() {
		return taxAmount;
	}
	public void setTaxAmount(String taxAmount) {
		this.taxAmount = taxAmount;
	}
	public String getConfirmedQty() {
		return confirmedQty;
	}
	public void setConfirmedQty(String confirmedQty) {
		this.confirmedQty = confirmedQty;
	}
	public String getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(String orderDate) {
		this.orderDate = orderDate;
	}
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public String getItemCode() {
		return itemCode;
	}
	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}
	public String getItemCost() {
		return itemCost;
	}
	public void setItemCost(String itemCost) {
		this.itemCost = itemCost;
	}
	public String getOrderItemId() {
		return orderItemId;
	}
	public void setOrderItemId(String orderItemId) {
		this.orderItemId = orderItemId;
	}
	public String getPayTotal() {
		return payTotal;
	}
	public void setPayTotal(String payTotal) {
		this.payTotal = payTotal;
	}
	public String getQuantity() {
		return quantity;
	}
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	public String getShippedTotal() {
		return shippedTotal;
	}
	public void setShippedTotal(String shippedTotal) {
		this.shippedTotal = shippedTotal;
	}
	public String getShipTax() {
		return shipTax;
	}
	public void setShipTax(String shipTax) {
		this.shipTax = shipTax;
	}
}
