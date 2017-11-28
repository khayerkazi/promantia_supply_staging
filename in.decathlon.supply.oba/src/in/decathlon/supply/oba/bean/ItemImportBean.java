package in.decathlon.supply.oba.bean;

import java.io.Serializable;

import org.openbravo.model.ad.domain.List;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;

import com.sysfore.catalog.CLColor;
import com.sysfore.catalog.CLModel;

public class ItemImportBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private CLModel clModel;
	private TaxCategory taxCategory;
	private CLColor clColor;
	private List clAge;
	private List clGender;
	private List clLifestage;
	private List productType;
	private ProductCategory productCategory;
	private UOM uOM;
	
	
	public CLModel getClModel() {
		return clModel;
	}
	public void setClModel(CLModel clModel) {
		this.clModel = clModel;
	}
	public TaxCategory getTaxCategory() {
		return taxCategory;
	}
	public void setTaxCategory(TaxCategory taxCategory) {
		this.taxCategory = taxCategory;
	}
	public CLColor getClColor() {
		return clColor;
	}
	public void setClColor(CLColor clColor) {
		this.clColor = clColor;
	}
	public List getClAge() {
		return clAge;
	}
	public void setClAge(List clAge) {
		this.clAge = clAge;
	}
	public List getClGender() {
		return clGender;
	}
	public void setClGender(List clGender) {
		this.clGender = clGender;
	}
	public List getClLifestage() {
		return clLifestage;
	}
	public void setClLifestage(List clLifestage) {
		this.clLifestage = clLifestage;
	}
	public List getProductType() {
		return productType;
	}
	public void setProductType(List productType) {
		this.productType = productType;
	}
	public ProductCategory getProductCategory() {
		return productCategory;
	}
	public void setProductCategory(ProductCategory productCategory) {
		this.productCategory = productCategory;
	}
	public UOM getuOM() {
		return uOM;
	}
	public void setuOM(UOM uOM) {
		this.uOM = uOM;
	}
	
	
}
