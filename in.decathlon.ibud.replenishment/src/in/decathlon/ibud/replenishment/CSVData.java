package in.decathlon.ibud.replenishment;

import java.math.BigDecimal;

public class CSVData {
	private String deptName;
	private String itemCode;
	private String modelName;
	private String logRec;
	private long displayMin;
	private long minQty;
	private long maxQty;
	private String openOrder;
	private long conFirmedQty;
	private String cacStock;
	private String carStock;
	private String hubStock;
	private BigDecimal ueQty;
	private BigDecimal pcbQty;
	private String storeStock;
	private BigDecimal reqQty;
	private BigDecimal ordQty;
	private String isPcb;
	private String isSpoon;
	
	
	public String getDeptName() {
		return deptName;
	}
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	public String getItemCode() {
		return itemCode;
	}
	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	public String getLogRec() {
		return logRec;
	}
	public void setLogRec(String logRec) {
		this.logRec = logRec;
	}
	public long getDisplayMin() {
		return displayMin;
	}
	public void setDisplayMin(long displayMin2) {
		this.displayMin = displayMin2;
	}
	public long getMinQty() {
		return minQty;
	}
	public void setMinQty(long minQty2) {
		this.minQty = minQty2;
	}
	public long getMaxQty() {
		return maxQty;
	}
	public void setMaxQty(long maxQty2) {
		this.maxQty = maxQty2;
	}
	public String getOpenOrder() {
		return openOrder;
	}
	public void setOpenOrder(String openOrder2) {
		this.openOrder = openOrder2;
	}
	public long getConFirmedQty() {
		return conFirmedQty;
	}
	public void setConFirmedQty(Long long1) {
		this.conFirmedQty = long1;
	}
	public String getCacStock() {
		return cacStock;
	}
	public void setCacStock(String rwhStock) {
		this.cacStock = rwhStock;
	}
	public String getCarStock() {
		return carStock;
	}
	public void setCarStock(String cwhStock) {
		this.carStock = cwhStock;
	}
	public String getHubStock() {
		return hubStock;
	}
	public void setHubStock(String hubStock2) {
		this.hubStock = hubStock2;
	}
	public BigDecimal getUeQty() {
		return ueQty;
	}
	public void setUeQty(BigDecimal ueQty) {
		this.ueQty = ueQty;
	}
	public BigDecimal getPcbQty() {
		return pcbQty;
	}
	public void setPcbQty(BigDecimal pcbQty) {
		this.pcbQty = pcbQty;
	}
	public String getStoreStock() {
		return storeStock;
	}
	public void setStoreStock(String storeStock2) {
		this.storeStock = storeStock2;
	}
	public BigDecimal getReqQty() {
		return reqQty;
	}
	public void setReqQty(BigDecimal reqQty) {
		this.reqQty = reqQty;
	}
	public BigDecimal getOrdQty() {
		return ordQty;
	}
	public void setOrdQty(BigDecimal ordQty) {
		this.ordQty = ordQty;
	}
	public String isPcb() {
		return isPcb;
	}
	public void setPcb(String isPcb2) {
		this.isPcb = isPcb2;
	}
	public String isSpoon() {
		return isSpoon;
	}
	public void setSpoon(String isSpoon) {
		this.isSpoon = isSpoon;
	}
	

}
