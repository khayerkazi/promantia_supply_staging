package in.decathlon.supply.dc;

public class AutoDCProductDTO {

	private String productId;
	private Long ueQty;
	private Long minQty;
	private Long maxQty;
	private Long pcbQty;
	private Long storeStock;
	private Long whStock;
	private Long displaymin;
	private String brandId;
	private String size;
	private String color;
	private String modelName;
	private String itemCode;
	private String pcbThresholdId;
	private String idsdProdCategoryId;
	private boolean isPcb;
	private String thresholdValue;
	private String logRecharge;
	
	public String getLogRecharge() {
		return logRecharge;
	}
	public void setLogRecharge(String logRecharge) {
		this.logRecharge = logRecharge;
	}
	public String getThresholdValue() {
		return thresholdValue;
	}
	public void setThresholdValue(String thresholdValue) {
		this.thresholdValue = thresholdValue;
	}
	public boolean isPcb() {
		return isPcb;
	}
	public void setPcb(boolean isPcb) {
		this.isPcb = isPcb;
	}
	public String getIdsdProdCategoryId() {
		return idsdProdCategoryId;
	}
	public void setIdsdProdCategoryId(String idsdProdCategoryId) {
		this.idsdProdCategoryId = idsdProdCategoryId;
	}
	public String getPcbThresholdId() {
		return pcbThresholdId;
	}
	public void setPcbThresholdId(String pcbThresholdId) {
		this.pcbThresholdId = pcbThresholdId;
	}
	public String getBrandId() {
		return brandId;
	}
	public void setBrandId(String brandId) {
		this.brandId = brandId;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	public String getItemCode() {
		return itemCode;
	}
	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	public Long getUeQty() {
		return ueQty;
	}
	public void setUeQty(Long ueQty) {
		this.ueQty = ueQty;
	}
	public Long getMinQty() {
		return minQty;
	}
	public void setMinQty(Long minQty) {
		this.minQty = minQty;
	}
	public Long getMaxQty() {
		return maxQty;
	}
	public void setMaxQty(Long maxQty) {
		this.maxQty = maxQty;
	}
	public Long getPcbQty() {
		return pcbQty;
	}
	public void setPcbQty(Long pcbQty) {
		this.pcbQty = pcbQty;
	}
	public Long getStoreStock() {
		return storeStock;
	}
	public void setStoreStock(Long storeStock) {
		this.storeStock = storeStock;
	}
	public Long getWhStock() {
		return whStock;
	}
	public void setWhStock(Long whStock) {
		this.whStock = whStock;
	}
	public Long getDisplaymin() {
		return displaymin;
	}
	public void setDisplaymin(Long displaymin) {
		this.displaymin = displaymin;
	}
}
