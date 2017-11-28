package in.decathlon.ibud.replenishment.bulk;

public class MinMaxComputed {
	private String deptName;
	private String productId;
	private String productName;
	private String modelName;
	private String modelCode;
	private String lifeStage;
	private String size;
	private int displayMin = 0;	
	private int min = 0;
	private int max = 0;
	private int ueQty = 0;
	private int pcbQty = 0;
	private String logRec;
	private int storeStock;
	private int cacStock;
	private int carStock;
	private int hubStock;
	private int requiredQty;
	private int toBeOrderedQty;
	private int qtyAlreadyOrdered;
	private int validatedQty;
	private boolean pcb;
	private boolean isSpoon;       

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	public String getModelCode() {
		return modelCode;
	}

	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}
	
	public String getLifeStage() {
		return lifeStage;
	}

	public void setLifeStage(String lifeStage) {
		this.lifeStage = lifeStage;
	}

	public String getsize() {
		return size;
	}

	public void setsize(String size) {
		this.size = size;
	}
	
	public int getDisplayMin() {
		return displayMin;
	}

	public void setDisplayMin(int displayMin) {
		this.displayMin = displayMin;
	}
	
	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public int getUeQty() {
		return ueQty;
	}

	public void setUeQty(int ueQty) {
		this.ueQty = ueQty;
	}

	public int getPcbQty() {
		return pcbQty;
	}

	public void setPcbQty(int pcbQty) {
		this.pcbQty = pcbQty;
	}

	public String getLogRec() {
		return logRec;
	}

	public void setLogRec(String logRec) {
		this.logRec = logRec;
	}

	public int getStoreStock() {
		return storeStock;
	}

	public void setStoreStock(int storeStock) {
		this.storeStock = storeStock;
	}

	public int getCacStock() {
		return cacStock;
	}

	public void setCacStock(int cacStock) {
		this.cacStock = cacStock;
	}

	public int getCarStock() {
		return carStock;
	}

	public void setCarStock(int carStock) {
		this.carStock = carStock;
	}

	public int getHubStock() {
		return hubStock;
	}

	public void setHubStock(int hubStock) {
		this.hubStock = hubStock;
	}

	public int getRequiredQty() {
		return requiredQty;
	}

	public void setRequiredQty(int requiredQty) {
		this.requiredQty = requiredQty;
	}

	public int getToBeOrderedQty() {
		return toBeOrderedQty;
	}

	public void setToBeOrderedQty(int toBeOrderedQty) {
		this.toBeOrderedQty = toBeOrderedQty;
	}

	public int getValidatedQty() {
		return validatedQty;
	}

	public void setValidatedQty(int validatedQty) {
		this.validatedQty = validatedQty;
	}

	public boolean isPcb() {
		return pcb;
	}

	public void setPcb(boolean pcb) {
		this.pcb = pcb;
	}

	public boolean isSpoon() {
		return isSpoon;
	}

	public void setSpoon(boolean isSpoon) {
		this.isSpoon = isSpoon;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public void setQtyalreadyOrdered(int qtyAlreadyOrdered) {
		this.qtyAlreadyOrdered=qtyAlreadyOrdered;
		
	}
	
	public int getQtyalreadyOrdered() {
		return qtyAlreadyOrdered;
	}
}
