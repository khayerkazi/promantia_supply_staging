package in.decathlon.supply.oba.bean;

import java.io.Serializable;

public class ItemExistedBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String modelCode;
	private String dmiUniverse;
	private String dmiDept;
	private String storeUniverse;
	private String storeDept;
	private String brandName;
	private String subDept;
	
	
	public String getSubDept() {
		return subDept;
	}
	public void setSubDept(String subDept) {
		this.subDept = subDept;
	}
	public String getModelCode() {
		return modelCode;
	}
	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}
	public String getDmiUniverse() {
		return dmiUniverse;
	}
	public void setDmiUniverse(String dmiUniverse) {
		this.dmiUniverse = dmiUniverse;
	}
	public String getDmiDept() {
		return dmiDept;
	}
	public void setDmiDept(String dmiDept) {
		this.dmiDept = dmiDept;
	}
	public String getStoreUniverse() {
		return storeUniverse;
	}
	public void setStoreUniverse(String storeUniverse) {
		this.storeUniverse = storeUniverse;
	}
	public String getStoreDept() {
		return storeDept;
	}
	public void setStoreDept(String storeDept) {
		this.storeDept = storeDept;
	}
	public String getBrandName() {
		return brandName;
	}
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}
	
	

}
