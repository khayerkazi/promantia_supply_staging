package  com.sysfore.storewarehouse.WebService;

import java.util.List;

import com.sysfore.storewarehouse.WebService.OrderlineDTO;

/**
 * A Data Transfer class for Order
 * 
 */

public class OrderDTO {

	private String adClientId;

	private String adOrgId;

	private String createdBy;

	private String updatedBy;

	private String bpartnerId;

	private String billtoId;

	private String currencyId;

	private String pricelistId;

	private String modelCode;

	private String pricelistVer;

	private String currency;

	private String warehouseId;

	private List<OrderlineDTO> listOrderlineDTOs;

	private String successStatus;

	private String cOrderId;

	private String documentNo;
	
	private String storeName;

	private String poReference;
	
	private String email;

	public String getAdClientId() {
		return adClientId;
	}

	public void setAdClientId(String adClientId) {
		this.adClientId = adClientId;
	}

	public String getAdOrgId() {
		return adOrgId;
	}

	public void setAdOrgId(String adOrgId) {
		this.adOrgId = adOrgId;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getBpartnerId() {
		return bpartnerId;
	}

	public void setBpartnerId(String bpartnerId) {
		this.bpartnerId = bpartnerId;
	}

	public String getBilltoId() {
		return billtoId;
	}

	public void setBilltoId(String billtoId) {
		this.billtoId = billtoId;
	}
	
	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storename) {
		this.storeName = storename;
	}

	public String getCurrencyId() {
		return currencyId;
	}

	public void setCurrencyId(String currencyId) {
		this.currencyId = currencyId;
	}

	public String getPricelistId() {
		return pricelistId;
	}

	public void setPricelistId(String pricelistId) {
		this.pricelistId = pricelistId;
	}

	public String getModelCode() {
		return modelCode;
	}

	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}

	public String getPricelistVer() {
		return pricelistVer;
	}

	public void setPricelistVer(String pricelistVer) {
		this.pricelistVer = pricelistVer;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(String warehouseId) {
		this.warehouseId = warehouseId;
	}

	public List<OrderlineDTO> getListOrderlineDTOs() {
		return listOrderlineDTOs;
	}

	public void setListOrderlineDTOs(List<OrderlineDTO> listOrderlineDTOs) {
		this.listOrderlineDTOs = listOrderlineDTOs;
	}

	public String getSuccessStatus() {
		return successStatus;
	}

	public void setSuccessStatus(String successStatus) {
		this.successStatus = successStatus;
	}

	public String getCOrderId() {
		return cOrderId;
	}

	public void setCOrderId(String cOrderId) {
		this.cOrderId = cOrderId;
	}

	public String getDocumentNo() {
		return documentNo;
	}

	public void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}

    public String getPoReference() {
		return poReference;
	}

	public void setPoReference(String poReference) {
		this.poReference = poReference;
	} 

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	} 

}
