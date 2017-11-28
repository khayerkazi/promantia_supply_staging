package in.decathlon.supply.dc;

public class SupplyDCOrderLinesDTO {
	
	private String strdept_id;
	private String product_id;
	private Long reqqty;
	private String brand_id;
	private String size;
	private String color;
	private String modelname;
	private String req_id;
	private String c_tax_id;
	private String orgId;
	private Long line;
	private String itemcode;
	private Long impqty;
	public String getStrdept_id() {
		return strdept_id;
	}
	public void setStrdept_id(String strdeptId) {
		strdept_id = strdeptId;
	}
	public String getProduct_id() {
		return product_id;
	}
	public void setProduct_id(String productId) {
		product_id = productId;
	}
	public Long getReqqty() {
		return reqqty;
	}
	public void setReqqty(Long reqqty) {
		this.reqqty = reqqty;
	}
	public String getBrand_id() {
		return brand_id;
	}
	public void setBrand_id(String brandId) {
		brand_id = brandId;
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
	public String getModelname() {
		return modelname;
	}
	public void setModelname(String modelname) {
		this.modelname = modelname;
	}
	public String getReq_id() {
		return req_id;
	}
	public void setReq_id(String reqId) {
		req_id = reqId;
	}
	public String getC_tax_id() {
		return c_tax_id;
	}
	public void setC_tax_id(String cTaxId) {
		c_tax_id = cTaxId;
	}
	public String getOrgId() {
		return orgId;
	}
	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}
	public Long getLine() {
		return line;
	}
	public void setLine(Long line) {
		this.line = line;
	}
	public String getItemcode() {
		return itemcode;
	}
	public void setItemcode(String itemcode) {
		this.itemcode = itemcode;
	}
	public Long getImpqty() {
		return impqty;
	}
	public void setImpqty(Long impqty) {
		this.impqty = impqty;
	}
}
