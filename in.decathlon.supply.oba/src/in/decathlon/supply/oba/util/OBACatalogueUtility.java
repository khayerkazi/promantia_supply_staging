package in.decathlon.supply.oba.util;

import in.decathlon.supply.oba.bean.ItemImportBean;
import in.decathlon.supply.oba.data.OBA_ModelProduct;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;

import com.sysfore.catalog.CLBrand;
import com.sysfore.catalog.CLBranddepartment;
import com.sysfore.catalog.CLCOMPONENTBRAND;
import com.sysfore.catalog.CLColor;
import com.sysfore.catalog.CLDepartment;
import com.sysfore.catalog.CLModel;
import com.sysfore.catalog.CLNatureOfProduct;
import com.sysfore.catalog.CLPurchasing;
import com.sysfore.catalog.CLSOURCING;
import com.sysfore.catalog.CLSport;
import com.sysfore.catalog.CLStoreDept;
import com.sysfore.catalog.CLSubdepartment;
import com.sysfore.catalog.CLUniverse;
import com.sysfore.catalog.ClStoreUniverse;

public class OBACatalogueUtility {

	private static final Logger LOG4J = Logger.getLogger(OBACatalogueUtility.class);
	
	public void doValidate(java.util.List<OBA_ModelProduct> list) {
		
		if(list.size() > 0) {
			
			final java.util.List<String> deptNames = new ArrayList<String>();
			final java.util.List<String> storeDeptNames = new ArrayList<String>();
			final java.util.List<String> universeNames = new ArrayList<String>();
			final java.util.List<String> storeUniverseNames = new ArrayList<String>();
			final java.util.List<String> natureOfProdNames = new ArrayList<String>();
			final java.util.List<String> brandNames = new ArrayList<String>();
			final java.util.List<String> subDeptNames = new ArrayList<String>();
			final java.util.List<String> typologytNames = new ArrayList<String>();
			final java.util.List<String> ageNames = new ArrayList<String>();
			final java.util.List<String> genderCateNames = new ArrayList<String>();
			final java.util.List<String> prodTypeNames = new ArrayList<String>();
			final java.util.List<String> lifestageNames = new ArrayList<String>();
			final java.util.List<String> taxCategoryNames = new ArrayList<String>();
			final java.util.List<String> uomNames = new ArrayList<String>();
			final java.util.List<String> prodCategoryNames = new ArrayList<String>();
			
			
			final OBCriteria<CLDepartment> clDeptObCriteria = OBDal
					.getInstance().createCriteria(CLDepartment.class);
			clDeptObCriteria.setFilterOnActive(false);
			if(clDeptObCriteria.count() > 0) {
				for (CLDepartment clDepartment : clDeptObCriteria.list()) {
					deptNames.add(clDepartment.getName());
				}
			}
			
			final OBCriteria<CLStoreDept> clStoreDeptObCriteria = OBDal.getInstance().createCriteria(CLStoreDept.class);
			clStoreDeptObCriteria.setFilterOnActive(false);
			if(clStoreDeptObCriteria.count() > 0) {
				for (CLStoreDept clStoreDept : clStoreDeptObCriteria.list()) {
					storeDeptNames.add(clStoreDept.getName());
				}
			}
			
			final OBCriteria<CLUniverse> clUniverseObCriteria = OBDal.getInstance().createCriteria(CLUniverse.class);
			clUniverseObCriteria.setFilterOnActive(false);
			if(clUniverseObCriteria.count() > 0) {
				for (CLUniverse clUniverse : clUniverseObCriteria.list()) {
					universeNames.add(clUniverse.getName());
				}
			}
			
			final OBCriteria<ClStoreUniverse> clStoreUniverseObCriteria = OBDal.getInstance().createCriteria(ClStoreUniverse.class);
			clStoreUniverseObCriteria.setFilterOnActive(false);
			if(clStoreUniverseObCriteria.count() > 0) {
				for (ClStoreUniverse clStoreUniverse : clStoreUniverseObCriteria.list()) {
					storeUniverseNames.add(clStoreUniverse.getCommercialName());
				}
			}
			
			final OBCriteria<CLNatureOfProduct> clNatureOfProdObCriteria = OBDal.getInstance().createCriteria(CLNatureOfProduct.class);
			clNatureOfProdObCriteria.setFilterOnActive(false);
			if(clNatureOfProdObCriteria.count() > 0) {
				for (CLNatureOfProduct clNatureOfProduct : clNatureOfProdObCriteria.list()) {
					natureOfProdNames.add(clNatureOfProduct.getName());
				}
			}
			
			final OBCriteria<CLBrand> clBrandObCriteria = OBDal.getInstance().createCriteria(CLBrand.class);
			clBrandObCriteria.setFilterOnActive(false);
			if(clBrandObCriteria.count() > 0) {
				for (CLBrand clBrand : clBrandObCriteria.list()) {
					brandNames.add(clBrand.getName());
				}
			}
			
			final OBCriteria<CLSubdepartment> clSubDeptObCriteria = OBDal.getInstance().createCriteria(CLSubdepartment.class);
			clSubDeptObCriteria.setFilterOnActive(false);
			if(clSubDeptObCriteria.count() > 0) {
				for (CLSubdepartment clSubdepartment : clSubDeptObCriteria.list()) {
					subDeptNames.add(clSubdepartment.getCommercialName());
				}
			}
			
			final OBQuery<List> typologyObQuery = OBDal.getInstance().createQuery(List.class, "as t where t.reference.id=(select id from ADReference where name='Typology')");
			typologyObQuery.setFilterOnActive(false);
			if(typologyObQuery.count() > 0) {
				for (List refList : typologyObQuery.list()) {
					typologytNames.add(refList.getName());
				}
			}
			
			final OBQuery<List> ageObQuery = OBDal.getInstance().createQuery(List.class, "as t where t.reference.id=(select id from ADReference where name='Age')");
			ageObQuery.setFilterOnActive(false);
			if(ageObQuery.count() > 0) {
				for (List refList : ageObQuery.list()) {
					ageNames.add(refList.getName());
				}
			}
			
			final OBQuery<List> genderCateObQuery = OBDal.getInstance().createQuery(List.class, "as t where t.reference.id=(select id from ADReference where name='GenderCat')");
			genderCateObQuery.setFilterOnActive(false);
			if(genderCateObQuery.count() > 0) {
				for (List refList : genderCateObQuery.list()) {
					genderCateNames.add(refList.getName());
				}
			}
			
			final OBQuery<List> prodTypeObQuery = OBDal.getInstance().createQuery(List.class, "as t where t.reference.id=(select id from ADReference where name='M_Product_ProductType')");
			prodTypeObQuery.setFilterOnActive(false);
			if(prodTypeObQuery.count() > 0) {
				for (List refList : prodTypeObQuery.list()) {
					prodTypeNames.add(refList.getName());
				}
			}
			
			final OBQuery<List> lifestageObQuery = OBDal.getInstance().createQuery(List.class, "as t where t.reference.id=(select id from ADReference where name='Lifestage')");
			lifestageObQuery.setFilterOnActive(false);
			if(lifestageObQuery.count() > 0) {
				for (List refList : lifestageObQuery.list()) {
					lifestageNames.add(refList.getName());
				}
			}
			
			final OBCriteria<TaxCategory> taxCategoryObCriteria = OBDal.getInstance().createCriteria(TaxCategory.class);
			taxCategoryObCriteria.setFilterOnActive(false);
			if(taxCategoryObCriteria.count() > 0) {
				for (TaxCategory taxCategory : taxCategoryObCriteria.list()) {
					taxCategoryNames.add(taxCategory.getName());
				}
			}
			
			final OBCriteria<UOM> uomObCriteria = OBDal.getInstance().createCriteria(UOM.class);
			uomObCriteria.setFilterOnActive(false);
			if(uomObCriteria.count() > 0) {
				for (UOM uom : uomObCriteria.list()) {
					uomNames.add(uom.getName());
				}
			}
			
			final OBCriteria<ProductCategory> prodCategoryObCriteria = OBDal.getInstance().createCriteria(ProductCategory.class);
			prodCategoryObCriteria.setFilterOnActive(false);
			if(prodCategoryObCriteria.count() > 0) {
				for (ProductCategory productCategory : prodCategoryObCriteria.list()) {
					prodCategoryNames.add(productCategory.getName());
				}
			}
			
			
			int count = 0;
			for (OBA_ModelProduct oba_ModelProduct : list) {
				
				final StringBuilder errorMessage = new StringBuilder();
				final StringBuilder notDefinedMessage = new StringBuilder();
				final OBCriteria<Product> prodObCriteria = OBDal.getInstance().createCriteria(Product.class);
				prodObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, oba_ModelProduct.getItemCode()));
				
				if(prodObCriteria.count() > 0) {
					
					// for existing item : missing case
					if(null == oba_ModelProduct.getModelName() || "".equals(oba_ModelProduct.getModelName())) {
						errorMessage.append("Model Name, ");
					}
					if(null == oba_ModelProduct.getModelCode() || "".equals(oba_ModelProduct.getModelCode())) {
						errorMessage.append("Model Code, ");
					}
					if(null == oba_ModelProduct.getIMANCode() || "".equals(oba_ModelProduct.getIMANCode())) {
						errorMessage.append("IMAN Code, ");
					}
					if(null == oba_ModelProduct.getItemCode() || "".equals(oba_ModelProduct.getItemCode())) {
						errorMessage.append("Item Code, ");
					}
					if(null == oba_ModelProduct.getDepartment() || "".equals(oba_ModelProduct.getDepartment())) {
					//	errorMessage.append("Dept, ");
					} else {
						if(!isDepartmentExists(deptNames, oba_ModelProduct.getDepartment())) {
							notDefinedMessage.append("Dept, ");
						}
					}
					if(null == oba_ModelProduct.getStoreDepartment() || "".equals(oba_ModelProduct.getStoreDepartment())) {
					//	errorMessage.append("Store Dept, ");
					} else {
						if(!isStoreDepartmentExists(storeDeptNames, oba_ModelProduct.getStoreDepartment())) {
							notDefinedMessage.append("Store Dept, ");
						}
					}
					if(null == oba_ModelProduct.getUniverse() || "".equals(oba_ModelProduct.getUniverse())) {
					//	errorMessage.append("Universe, ");
					} else {
						if(!isUniverseExists(universeNames, oba_ModelProduct.getUniverse())) {
							notDefinedMessage.append("Store Dept, ");
						}
					}
					if(null == oba_ModelProduct.getStoreuniverse() || "".equals(oba_ModelProduct.getStoreuniverse())) {
					//	errorMessage.append("Store Universe, ");
					} else {
						if(!isStoreUniverseExists(storeUniverseNames, oba_ModelProduct.getStoreuniverse())) {
							notDefinedMessage.append("Store Universe, ");
						}
					}
				/*	if(null == oba_ModelProduct.getMerchandiseCategory() || "".equals(oba_ModelProduct.getMerchandiseCategory())) {
						errorMessage.append("Merchandise Category, ");
					}*/
					if(null == oba_ModelProduct.getNatureOfProduct() || "".equals(oba_ModelProduct.getNatureOfProduct())) {
						errorMessage.append("Nature of Product, ");
					} else {
						if(!isNatureOfProdExists(natureOfProdNames, oba_ModelProduct.getNatureOfProduct())) {
							notDefinedMessage.append("Nature of Product, ");
						}
					}
					if(null == oba_ModelProduct.getBrand() || "".equals(oba_ModelProduct.getBrand())) {
						errorMessage.append("Brand, ");
					} else {
						if(!isBrandExists(brandNames, oba_ModelProduct.getBrand())) {
							notDefinedMessage.append("Brand, ");
						}
					}
				/*	if(oba_ModelProduct.getCessionPrice().doubleValue() > oba_ModelProduct.getMRPPrice().doubleValue()) {
						errorMessage.append("Cession Price is greater than MRP.");
					}
					if(oba_ModelProduct.getCessionPrice().doubleValue() > oba_ModelProduct.getMRPPrice().doubleValue()) {
						errorMessage.append("Cash and Carry Price is greater than MRP.");
					}*/
					/*				PriceListVersion priceListVersion = null;
		            final OBCriteria<PriceListVersion> priceListVersionObCriteria = OBDal.getInstance().createCriteria(PriceListVersion.class);
		            priceListVersionObCriteria.add(Restrictions.ilike(PriceListVersion.PROPERTY_NAME, oba_ModelProduct.getPricelistVersion()));
		            if(priceListVersionObCriteria.count() > 0) {
		                priceListVersion = priceListVersionObCriteria.list().get(0);
		           }
		            ProductPrice price = null;
					final OBCriteria<ProductPrice> productPriceObCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
	                productPriceObCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, prodObCriteria.list().get(0)));
	                productPriceObCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION, priceListVersion));
					
	                if(productPriceObCriteria.count() > 0) {
	                	price = productPriceObCriteria.list().get(0);
	                	if(oba_ModelProduct.getMRPPrice().doubleValue() < price.getClMrpprice().doubleValue())
	                		errorMessage.append("MRP price is less than the current value.");
	                }
					if(oba_ModelProduct.getCessionPrice().doubleValue() > oba_ModelProduct.getMRPPrice().doubleValue()) {
                    	errorMessage.append("Cession Price is greater than MRP.");
                   }
                    if(oba_ModelProduct.getCessionPrice().doubleValue() > oba_ModelProduct.getMRPPrice().doubleValue()) {
                    	errorMessage.append("Cash and Carry Price is greater than MRP.");
                   }*/
					if(errorMessage.length() > 0) {
						errorMessage.deleteCharAt(errorMessage.length() - 2);
						errorMessage.append("is mandatory. ");
					}
					if(notDefinedMessage.length() > 0) {
						notDefinedMessage.deleteCharAt(notDefinedMessage.length() - 2);
						notDefinedMessage.append("is not defined. ");
						errorMessage.append(notDefinedMessage.toString());
					}
					
					
				} else {
					
					// for non-existing item : missing case
					if(null == oba_ModelProduct.getModelName() || "".equals(oba_ModelProduct.getModelName())) {
						errorMessage.append("Model Name, ");
					}
					if(null == oba_ModelProduct.getModelCode() || "".equals(oba_ModelProduct.getModelCode())) {
						errorMessage.append("Model Code, ");
					}
					if(null == oba_ModelProduct.getIMANCode() || "".equals(oba_ModelProduct.getIMANCode())) {
						errorMessage.append("IMAN Code, ");
					}
					if(null == oba_ModelProduct.getItemCode() || "".equals(oba_ModelProduct.getItemCode())) {
						errorMessage.append("Item Code, ");
					}
					if(null == oba_ModelProduct.getEANCode() || "".equals(oba_ModelProduct.getEANCode())) {
						errorMessage.append("EAN Code, ");
					} else {
						if(oba_ModelProduct.getEANCode().length() < 8 ||oba_ModelProduct.getEANCode().length() > 13) {
							errorMessage.append("EAN Code must be 8 to 13 digits, ");
						}
						if(!oba_ModelProduct.getEANCode().matches("[0-9]+")) {
							errorMessage.append("EAN Code must be Numeric, ");
						}
					}
					if(null == oba_ModelProduct.getSize() || "".equals(oba_ModelProduct.getSize())) {
						errorMessage.append("Size, ");
					}
					if(null == oba_ModelProduct.getColor() || "".equals(oba_ModelProduct.getColor())) {
						errorMessage.append("Color, ");
					}
				/*	if(oba_ModelProduct.getCessionPrice().doubleValue() > oba_ModelProduct.getMRPPrice().doubleValue()) {
						errorMessage.append("Cession Price is greater than MRP.");
					}
					if(oba_ModelProduct.getCessionPrice().doubleValue() > oba_ModelProduct.getMRPPrice().doubleValue()) {
						errorMessage.append("Cash and Carry Price is greater than MRP.");
					}*/
					if(null == oba_ModelProduct.getDepartment() || "".equals(oba_ModelProduct.getDepartment())) {
					//	errorMessage.append("Dept, ");
					} else {
						if(!isDepartmentExists(deptNames, oba_ModelProduct.getDepartment())) {
							notDefinedMessage.append("Dept, ");
						}
					}
					if(null == oba_ModelProduct.getStoreDepartment() || "".equals(oba_ModelProduct.getStoreDepartment())) {
					//	errorMessage.append("Store Dept, ");
					} else {
						if(!isStoreDepartmentExists(storeDeptNames, oba_ModelProduct.getStoreDepartment())) {
							if(!isStoreDepartmentExists(storeDeptNames, oba_ModelProduct.getStoreDepartment())) {
								notDefinedMessage.append("Store Dept, ");
							}
						}
					}
					
					if(null == oba_ModelProduct.getUniverse() || "".equals(oba_ModelProduct.getUniverse())) {
					//	errorMessage.append("Universe, ");
					} else {
						if(!isUniverseExists(universeNames, oba_ModelProduct.getUniverse())) {
							notDefinedMessage.append("Universe, ");
						}
					}
					if(null == oba_ModelProduct.getStoreuniverse() || "".equals(oba_ModelProduct.getStoreuniverse())) {
					//	errorMessage.append("Store Universe, ");
					} else {
						if(!isStoreUniverseExists(storeUniverseNames, oba_ModelProduct.getStoreuniverse())) {
							notDefinedMessage.append("Store Universe, ");
						}
					}
				/*	if(null == oba_ModelProduct.getMerchandiseCategory() || "".equals(oba_ModelProduct.getMerchandiseCategory())) {
						errorMessage.append("Merchandise Category, ");
					}*/
					if(null == oba_ModelProduct.getNatureOfProduct() || "".equals(oba_ModelProduct.getNatureOfProduct())) {
						errorMessage.append("Nature of Product, ");
					} else {
						if(!isNatureOfProdExists(natureOfProdNames, oba_ModelProduct.getNatureOfProduct())) {
							notDefinedMessage.append("Nature of Product, ");
						}
					}
					if(null == oba_ModelProduct.getBrand() || "".equals(oba_ModelProduct.getBrand())) {
						errorMessage.append("Brand, ");
					} else {
						if(!isBrandExists(brandNames, oba_ModelProduct.getBrand())) {
							notDefinedMessage.append("Brand, ");
						}
					}
					if(null == oba_ModelProduct.getSubdepartment() || "".equals(oba_ModelProduct.getSubdepartment())) {
					//	errorMessage.append("Sub Department, ");
					} else {
						if(!isSubDeptExists(subDeptNames, oba_ModelProduct.getSubdepartment())) {
							notDefinedMessage.append("Sub Department, ");
						}
					}
					if(null == oba_ModelProduct.getTypology() || "".equals(oba_ModelProduct.getTypology())) {
						errorMessage.append("Typology, ");
					}
					if(errorMessage.length() > 0) {
						errorMessage.deleteCharAt(errorMessage.length() - 2);
						errorMessage.append("is mandatory. ");
					}
					if(notDefinedMessage.length() > 0) {
						notDefinedMessage.deleteCharAt(notDefinedMessage.length() - 2);
						notDefinedMessage.append("is not defined. ");
						errorMessage.append(notDefinedMessage.toString());
					}
					if(null != oba_ModelProduct.getTypology() && !"".equals(oba_ModelProduct.getTypology())) {
						if(!isTypologyExists(typologytNames, oba_ModelProduct.getTypology())) {
							errorMessage.append("Typology, ");
						}
					}
					if(null != oba_ModelProduct.getAge() && !"".equals(oba_ModelProduct.getAge())) {
						if(!isAgeExists(ageNames, oba_ModelProduct.getAge())) {
							errorMessage.append("Age, ");
						}
					}
					if(null != oba_ModelProduct.getGender() && !"".equals(oba_ModelProduct.getGender())) {
						if(!isGenderExists(genderCateNames, oba_ModelProduct.getGender())) {
							errorMessage.append("Gender, ");
						}
					}
					if(null != oba_ModelProduct.getProductType() && !"".equals(oba_ModelProduct.getProductType())) {
						if(!isProductTypeExists(prodTypeNames, oba_ModelProduct.getProductType())) {
							errorMessage.append("Product Type, ");
						}
					}
					if(null != oba_ModelProduct.getLifeStage() && !"".equals(oba_ModelProduct.getLifeStage())) {
						if(!isgetLifeStageExists(lifestageNames, oba_ModelProduct.getLifeStage())) {
							errorMessage.append("Life Stage, ");
						}
					}
					if(null != oba_ModelProduct.getTaxCategory() && !"".equals(oba_ModelProduct.getTaxCategory())) {
						if(!isgetLifeStageExists(taxCategoryNames, oba_ModelProduct.getTaxCategory())) {
							errorMessage.append("Tax Category, ");
						}
					}
					if(null != oba_ModelProduct.getUOM() && !"".equals(oba_ModelProduct.getUOM())) {
						if(!isUOMExists(uomNames, oba_ModelProduct.getUOM())) {
							errorMessage.append("UOM, ");
						}
					}
					if(null != oba_ModelProduct.getProductCategory() && !"".equals(oba_ModelProduct.getProductCategory())) {
						if(!isProdCateExists(prodCategoryNames, oba_ModelProduct.getProductCategory())) {
							errorMessage.append("Product Category, ");
						}
					}
					if(null != oba_ModelProduct.getProductType() && !"".equals(oba_ModelProduct.getProductType())) {
						// Business Partner
						if(!oba_ModelProduct.getProductType().equalsIgnoreCase("service")) {
							final OBCriteria<BusinessPartner> businessPartnerObCriteria = OBDal.getInstance().createCriteria(BusinessPartner.class);
							businessPartnerObCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_CLSUPPLIERNO, oba_ModelProduct.getSupplierCode()));
							businessPartnerObCriteria.setFilterOnReadableOrganization(false);
							businessPartnerObCriteria.setFilterOnActive(false);
							if(businessPartnerObCriteria.count() == 0) {
								errorMessage.append("Business Partner, ");
							}
						}
						
						// Sourcing
						if(!oba_ModelProduct.getProductType().equalsIgnoreCase("service")) {
							final OBCriteria<CLSOURCING> clSourcingObCriteria = OBDal.getInstance().createCriteria(CLSOURCING.class);
							clSourcingObCriteria.add(Restrictions.eq(CLSOURCING.PROPERTY_NAME, oba_ModelProduct.getSourcing()));
							clSourcingObCriteria.setFilterOnReadableOrganization(false);
							clSourcingObCriteria.setFilterOnActive(false);
							if(clSourcingObCriteria.count() == 0) {
								errorMessage.append("Sourcing, ");
							}
						}
						
						// Madein
						if(!oba_ModelProduct.getProductType().equalsIgnoreCase("service")) {
							final OBCriteria<Country> countryObCriteria = OBDal.getInstance().createCriteria(Country.class);
							countryObCriteria.add(Restrictions.eq(Country.PROPERTY_ISOCOUNTRYCODE, oba_ModelProduct.getCountry()));
							countryObCriteria.setFilterOnReadableOrganization(false);
							countryObCriteria.setFilterOnActive(false);
							if(countryObCriteria.count() == 0) {
								errorMessage.append("Country, ");
							}
						}
					}
					
					if (errorMessage.toString().contains("Typology")
							|| errorMessage.toString().contains("Age")
							|| errorMessage.toString().contains("Gender")
							|| errorMessage.toString().contains("Product Type")
							|| errorMessage.toString().contains("Life Stage")
							|| errorMessage.toString().contains("Tax Category")
							|| errorMessage.toString().contains("UOM")
							|| errorMessage.toString().contains(
									"Product Category")
							|| errorMessage.toString().contains(
									"Business Partner")
							|| errorMessage.toString().contains("Sourcing")
							|| errorMessage.toString().contains("Country")) {
						errorMessage.deleteCharAt(errorMessage.length() - 2);
						errorMessage.append("is not defined. ");
					}
					
					if (null != oba_ModelProduct.getProductType()
							&& oba_ModelProduct.getProductType()
									.equalsIgnoreCase("service")
							&& (null != oba_ModelProduct.getSupplierCode()
									|| null != oba_ModelProduct.getSourcing() || null != oba_ModelProduct
									.getCountry())) {
						errorMessage.append("Service Item not allowed you to enter Supplier, Madein, Sourcing. ");
					}
				}
				if (null != oba_ModelProduct.getIMANCode()
						&& null != oba_ModelProduct.getBrand()
						&& null != oba_ModelProduct.getNatureOfProduct()
						&& null != oba_ModelProduct.getMerchandiseCategory()) {
					
					// for non-existing item : formataxtting error
					final StringBuilder formatValidationQueryMN = new StringBuilder();
					formatValidationQueryMN.append("as oba WHERE ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.modelCode!='"+oba_ModelProduct.getModelCode()+"') OR ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.iMANCode!='"+oba_ModelProduct.getIMANCode()+"') OR ");
//					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName()+"' AND oba.sports!='"+oba_ModelProduct.getSports()+"') OR ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.brand!='"+oba_ModelProduct.getBrand().replace("'", "''")+"') OR ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.natureOfProduct!='"+oba_ModelProduct.getNatureOfProduct().replace("'", "''")+"') OR ");
//					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName()+"' AND oba.componentBrand!='"+oba_ModelProduct.getComponentBrand()+"') OR ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.typology!='"+oba_ModelProduct.getTypology()+"') OR ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.merchandiseCategory!='"+oba_ModelProduct.getMerchandiseCategory().replace("'", "''")+"') OR ");
//					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName()+"' AND oba.componentMerchandiseCat!='"+oba_ModelProduct.getComponentMerchandiseCat()+"') OR ");
//					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName()+"' AND oba.sportCategory!='"+oba_ModelProduct.getSportCategory()+"') OR ");
					formatValidationQueryMN.append("(oba.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND oba.blueProduct!="+oba_ModelProduct.isBlueProduct()+")");
					
					final OBQuery<OBA_ModelProduct> obaModelProdObQueryMN = OBDal.getInstance().createQuery(OBA_ModelProduct.class, formatValidationQueryMN.toString());
					obaModelProdObQueryMN.setFilterOnActive(false);
					if(obaModelProdObQueryMN.count() > 0) {
					//	errorMessage.append("Same Model Name exists with duplicate Property. ");
					}
					
					final StringBuilder formatValidationQueryMC = new StringBuilder();
					formatValidationQueryMC.append("as oba WHERE ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.modelName!='"+oba_ModelProduct.getModelName().replace("'", "''")+"') OR ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.iMANCode!='"+oba_ModelProduct.getIMANCode()+"') OR ");
//					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.sports!='"+oba_ModelProduct.getSports()+"') OR ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.brand!='"+oba_ModelProduct.getBrand().replace("'", "''")+"') OR ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.natureOfProduct!='"+oba_ModelProduct.getNatureOfProduct().replace("'", "''")+"') OR ");
//					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.componentBrand!='"+oba_ModelProduct.getComponentBrand()+"') OR ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.typology!='"+oba_ModelProduct.getTypology()+"') OR ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.merchandiseCategory!='"+oba_ModelProduct.getMerchandiseCategory().replace("'", "''")+"') OR ");
//					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.componentMerchandiseCat!='"+oba_ModelProduct.getComponentMerchandiseCat()+"') OR ");
//					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.sportCategory!='"+oba_ModelProduct.getSportCategory()+"') OR ");
					formatValidationQueryMC.append("(oba.modelCode='"+oba_ModelProduct.getModelCode()+"' AND oba.blueProduct!="+oba_ModelProduct.isBlueProduct()+")");
					
					final OBQuery<OBA_ModelProduct> obaModelProdObQueryMC = OBDal.getInstance().createQuery(OBA_ModelProduct.class, formatValidationQueryMC.toString());
					obaModelProdObQueryMC.setFilterOnActive(false);
					if(obaModelProdObQueryMC.count() > 0) {
						errorMessage.append("Same Model Code exists with duplicate Property. ");
					}
					
					final StringBuilder duplicateMN = new StringBuilder();
					duplicateMN.append("as cl WHERE ");
					duplicateMN.append("cl.modelName='"+oba_ModelProduct.getModelName().replace("'", "''")+"' AND ");
					duplicateMN.append("cl.modelCode!='"+oba_ModelProduct.getModelCode()+"'");
					final OBQuery<CLModel> clModelObQueryMN = OBDal.getInstance().createQuery(CLModel.class, duplicateMN.toString());
					clModelObQueryMN.setFilterOnActive(false);
					if(clModelObQueryMN.count() > 0) {
						errorMessage.append("Same Model Name exists with different Model Code. ");
					}
					
					final StringBuilder duplicateIC = new StringBuilder();
					duplicateIC.append("as o WHERE ");
					duplicateIC.append("o.itemCode='"+oba_ModelProduct.getItemCode()+"'");
					final OBQuery<OBA_ModelProduct> duplicateItemCodeObQuery = OBDal.getInstance().createQuery(OBA_ModelProduct.class, duplicateIC.toString());
					duplicateItemCodeObQuery.setFilterOnActive(false);
					if(duplicateItemCodeObQuery.count()>1) {
						errorMessage.append("Duplicate Item. ");
					}
					
					final StringBuilder duplicateEAN = new StringBuilder();
					duplicateEAN.append("as o WHERE ");
					duplicateEAN.append("o.eANCode='"+oba_ModelProduct.getEANCode()+"' AND ");
					duplicateEAN.append("o.itemCode!='"+oba_ModelProduct.getItemCode()+"'");
					final OBQuery<OBA_ModelProduct> duplicateEANObQuery = OBDal.getInstance().createQuery(OBA_ModelProduct.class, duplicateEAN.toString());
					duplicateEANObQuery.setFilterOnActive(false);
					if(duplicateEANObQuery.count() > 0) {
						errorMessage.append("Duplicate EAN. ");
					}
					
					final StringBuilder alreadyEAN = new StringBuilder();
					alreadyEAN.append("as p WHERE ");
					alreadyEAN.append("p.uPCEAN='"+oba_ModelProduct.getEANCode()+"' AND ");
					alreadyEAN.append("p.name!='"+oba_ModelProduct.getItemCode()+"'");
					final OBQuery<Product> alreadyEANObQuery = OBDal.getInstance().createQuery(Product.class, alreadyEAN.toString());
					alreadyEANObQuery.setFilterOnActive(false);
					if(alreadyEANObQuery.count() > 0) {
						errorMessage.append("EAN Already assigned.");
					}
				}
				
				if(errorMessage.length()>0) {
					oba_ModelProduct.setErrorMsg(errorMessage.toString());
				} else {
					oba_ModelProduct.setValidated(true);
				}
				OBDal.getInstance().save(oba_ModelProduct);
				
				if(count % 100 ==0) {
					OBDal.getInstance().flush();
					OBDal.getInstance().getSession().clear();
				}
				count++;
			}
			SessionHandler.getInstance().commitAndStart();
		}
	}
	
	private boolean isProdCateExists(java.util.List<String> prodCategoryNames,
			String productCategory) {
		
		for (String string : prodCategoryNames) {
			if (string.equalsIgnoreCase(productCategory)) return true;
		}
		return false;
	}

	private boolean isUOMExists(java.util.List<String> uomNames, String uom) {
		
		for (String string : uomNames) {
			if (string.equalsIgnoreCase(uom)) return true;
		}
		return false;
	}

	private boolean isAgeExists(java.util.List<String> ageNames, String age) {
		
		for (String string : ageNames) {
			if (string.equalsIgnoreCase(age)) return true;
		}
		return false;
	}

	private boolean isgetLifeStageExists(java.util.List<String> lifestageNames,
			String lifeStage) {
		
		for (String string : lifestageNames) {
			if (string.equalsIgnoreCase(lifeStage)) return true;
		}
		return false;
	}

	private boolean isProductTypeExists(java.util.List<String> prodTypeNames,
			String productType) {
		
		for (String string : prodTypeNames) {
			if (string.equalsIgnoreCase(productType)) return true;
		}
		return false;
	}

	private boolean isGenderExists(java.util.List<String> genderCateNames,
			String gender) {
		
		for (String string : genderCateNames) {
			if (string.equalsIgnoreCase(gender)) return true;
		}
		return false;
	}

	private boolean isTypologyExists(java.util.List<String> typologytNames,
			String typology) {
		
		for (String string : typologytNames) {
			if (string.equalsIgnoreCase(typology)) return true;
		}
		return false;
	}

	private boolean isSubDeptExists(java.util.List<String> subDeptNames,
			String subdepartment) {
		
		for (String string : subDeptNames) {
			if (string.equalsIgnoreCase(subdepartment)) return true;
		}
		return false;
	}

	private boolean isBrandExists(java.util.List<String> brandNames,
			String brand) {
		
		for (String string : brandNames) {
			if (string.equalsIgnoreCase(brand)) return true;
		}
		return false;
	}

	private boolean isNatureOfProdExists(
			java.util.List<String> natureOfProdNames, String natureOfProduct) {
		
		for (String string : natureOfProdNames) {
			if (string.equalsIgnoreCase(natureOfProduct)) return true;
		}
		return false;
	}

	private boolean isStoreUniverseExists(
			java.util.List<String> storeUniverseNames, String storeuniverse) {
		
		for (String string : storeUniverseNames) {
			if (string.equalsIgnoreCase(storeuniverse)) return true;
		}
		return false;
	}

	private boolean isUniverseExists(java.util.List<String> universeNames,
			String universe) {
		
		for (String string : universeNames) {
			if (string.equalsIgnoreCase(universe)) return true;
		}
		return false;
	}

	private boolean isStoreDepartmentExists(
			java.util.List<String> storeDeptNames, String storeDepartment) {
		
		for (String string : storeDeptNames) {
			if (string.equalsIgnoreCase(storeDepartment)) return true;
		}
		return false;
	}

	private boolean isDepartmentExists(java.util.List<String> deptNames,
			String department) {
		
		for (String string : deptNames) {
			if (string.equalsIgnoreCase(department)) return true;
		}
		return false;
	}

	public void importModel(java.util.List<String> distModelCodes) {

		int count = 0;
		
		final java.util.Map<String, String> typologyMap = getTypologies();
		final java.util.Map<String, CLDepartment> clDeptMap = getCLDepts();
		final java.util.Map<String, CLStoreDept> clStoreDeptMap = getCLStoreDepts();
		final java.util.Map<String, CLUniverse> clUniverseMap = getCLUniverses();
		final java.util.Map<String, ClStoreUniverse> clStoreUniverseMap = getCLStoreUniverses();
		final java.util.Map<String, CLNatureOfProduct> clNatureOfProductMap = getCLNatureOfProducts();
		final java.util.Map<String, CLBrand> clBrandMap = getCLBrands();
		final java.util.Map<String, CLSubdepartment> clSubDeptMap = getCLSubDepts();
		final java.util.Map<String, CLSport> clSportMap = getCLSports();
		final java.util.Map<String, CLCOMPONENTBRAND> clCompBrandMap = getCLCompBrands();
		final java.util.Map<String, CLBranddepartment> clBrandDeptMap = getCLBrandDepts();
		
		for (String modelCode : distModelCodes) {
			
			count++;
			final OBCriteria<OBA_ModelProduct> modelProductObCriteria = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
			modelProductObCriteria.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_MODELCODE, modelCode));
			modelProductObCriteria.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_VALIDATED, true));
			final OBA_ModelProduct oba_ModelProduct = modelProductObCriteria.list().get(0);
			
			final OBCriteria<CLModel> clModelObCriteria = OBDal.getInstance().createCriteria(CLModel.class);
			clModelObCriteria.add(Restrictions.eq(CLModel.PROPERTY_MODELCODE, modelCode));
			clModelObCriteria.setFilterOnActive(false);
			if(clModelObCriteria.count() > 0) {
				LOG4J.info("update model " + modelCode);
				final CLModel clModel = clModelObCriteria.list().get(0);
					
				clModel.setUpdated(new Timestamp(new Date().getTime()));
				clModel.setUpdatedBy(oba_ModelProduct.getUpdatedBy());
				clModel.setModelName(oba_ModelProduct.getModelName());
				clModel.setModelCode(oba_ModelProduct.getModelCode());
				for (Map.Entry<String, CLBrand> entry : clBrandMap.entrySet()) {
					if(oba_ModelProduct.getBrand().equalsIgnoreCase(entry.getKey()))
						clModel.setBrand(clBrandMap.get(entry.getKey()));
				}
				for (Map.Entry<String, CLNatureOfProduct> entry : clNatureOfProductMap.entrySet()) {
					if(oba_ModelProduct.getNatureOfProduct().equalsIgnoreCase(entry.getKey()))
						clModel.setNatureOfProduct(clNatureOfProductMap.get(entry.getKey()));
				}
				clModel.setMerchandiseCategory(oba_ModelProduct.getMerchandiseCategory());
				for (Map.Entry<String, CLDepartment> entry : clDeptMap.entrySet()) {
					if(oba_ModelProduct.getDepartment().equalsIgnoreCase(entry.getKey()))
						clModel.setDepartment(clDeptMap.get(entry.getKey()));
				}
				for (Map.Entry<String, CLStoreDept> entry : clStoreDeptMap.entrySet()) {
					if(oba_ModelProduct.getStoreDepartment().equalsIgnoreCase(entry.getKey()))
						clModel.setStoreDepartment(clStoreDeptMap.get(entry.getKey()));
				}
				for (Map.Entry<String, CLUniverse> entry : clUniverseMap.entrySet()) {
					if(oba_ModelProduct.getUniverse().equalsIgnoreCase(entry.getKey()))
						clModel.setUniverse(clUniverseMap.get(entry.getKey()));
				}
				for (Map.Entry<String, ClStoreUniverse> entry : clStoreUniverseMap.entrySet()) {
					if(oba_ModelProduct.getStoreuniverse().equalsIgnoreCase(entry.getKey()))
						clModel.setStoreuniverse(clStoreUniverseMap.get(entry.getKey()));
				}
				if(null != oba_ModelProduct.getFamilycode())
					clModel.setSwFamilycode(oba_ModelProduct.getFamilycode());
				
				OBDal.getInstance().save(clModel);

			} else {
				
				LOG4J.info("insert model " + modelCode);
				final CLModel clModel = OBProvider.getInstance().get(CLModel.class);
				
				clModel.setClient(OBContext.getOBContext().getCurrentClient());
				clModel.setOrganization(OBContext.getOBContext().getCurrentOrganization());
				clModel.setCreatedBy(oba_ModelProduct.getCreatedBy());
				clModel.setUpdatedBy(oba_ModelProduct.getUpdatedBy());
				clModel.setCreationDate(new Timestamp(new Date().getTime()));
				clModel.setUpdated(new Timestamp(new Date().getTime()));
				clModel.setModelName(oba_ModelProduct.getModelName());
				clModel.setModelCode(oba_ModelProduct.getModelCode());
				clModel.setImanCode(oba_ModelProduct.getIMANCode());
				for (Map.Entry<String, CLDepartment> entry : clDeptMap.entrySet()) {
					if(oba_ModelProduct.getDepartment().equalsIgnoreCase(entry.getKey()))
						clModel.setDepartment(clDeptMap.get(entry.getKey()));
				}
				if(null != oba_ModelProduct.getSports() && !"".equals(oba_ModelProduct.getSports())) {
					for (Map.Entry<String, CLSport> entry : clSportMap.entrySet()) {
						if(oba_ModelProduct.getSports().equalsIgnoreCase(entry.getKey()))
							clModel.setSport(clSportMap.get(entry.getKey()));
					}
				}
				if(null != oba_ModelProduct.getSportCategory() && !"".equals(oba_ModelProduct.getSportCategory()))
					clModel.setSportCategory(oba_ModelProduct.getSportCategory());
				for (Map.Entry<String, CLBrand> entry : clBrandMap.entrySet()) {
					if(oba_ModelProduct.getBrand().equalsIgnoreCase(entry.getKey()))
						clModel.setBrand(clBrandMap.get(entry.getKey()));
				}
				for (Map.Entry<String, CLNatureOfProduct> entry : clNatureOfProductMap.entrySet()) {
					if(oba_ModelProduct.getNatureOfProduct().equalsIgnoreCase(entry.getKey()))
						clModel.setNatureOfProduct(clNatureOfProductMap.get(entry.getKey()));
				}
				for (Map.Entry<String, String> entry : typologyMap.entrySet()) {
					if(oba_ModelProduct.getTypology().equalsIgnoreCase(entry.getKey()))
						clModel.setTypology(typologyMap.get(entry.getKey()));
				}
				clModel.setMerchandiseCategory(oba_ModelProduct.getMerchandiseCategory());
				if(null != oba_ModelProduct.getComponentMerchandiseCat() && !"".equals(oba_ModelProduct.getComponentMerchandiseCat()))
					clModel.setComponentMerchandiseCategory(oba_ModelProduct.getComponentMerchandiseCat());
				if(null != oba_ModelProduct.getComponentBrand() && !"".equals(oba_ModelProduct.getComponentBrand())) {
					for (Map.Entry<String, CLCOMPONENTBRAND> entry : clCompBrandMap.entrySet()) {
						if(oba_ModelProduct.getComponentBrand().equalsIgnoreCase(entry.getKey()))
							clModel.setComponentBrand(clCompBrandMap.get(entry.getKey()));
					}
				}
				if(oba_ModelProduct.isBlueProduct()) {
					clModel.setBlueProduct("Y");
				} else {
					clModel.setBlueProduct("N");
				}
				for (Map.Entry<String, CLStoreDept> entry : clStoreDeptMap.entrySet()) {
					if(oba_ModelProduct.getStoreDepartment().equalsIgnoreCase(entry.getKey()))
						clModel.setStoreDepartment(clStoreDeptMap.get(entry.getKey()));
				}
				for (Map.Entry<String, CLUniverse> entry : clUniverseMap.entrySet()) {
					if(oba_ModelProduct.getUniverse().equalsIgnoreCase(entry.getKey()))
						clModel.setUniverse(clUniverseMap.get(entry.getKey()));
				}
				for (Map.Entry<String, ClStoreUniverse> entry : clStoreUniverseMap.entrySet()) {
					if(oba_ModelProduct.getStoreuniverse().equalsIgnoreCase(entry.getKey()))
						clModel.setStoreuniverse(clStoreUniverseMap.get(entry.getKey()));
				}
				for (Map.Entry<String, CLSubdepartment> entry : clSubDeptMap.entrySet()) {
					if(oba_ModelProduct.getSubdepartment().equalsIgnoreCase(entry.getKey()))
						clModel.setSubdepartment(clSubDeptMap.get(entry.getKey()));
				}
				if(null != oba_ModelProduct.getBranddepartment() && !"".equals(oba_ModelProduct.getBranddepartment())) {
					for (Map.Entry<String, CLBranddepartment> entry : clBrandDeptMap.entrySet()) {
						if(oba_ModelProduct.getBranddepartment().equalsIgnoreCase(entry.getKey()))
							clModel.setBranddepartment(clBrandDeptMap.get(entry.getKey()));
					}
				}
				if(null != oba_ModelProduct.getFamilycode())
					clModel.setSwFamilycode(oba_ModelProduct.getFamilycode());
				
				OBDal.getInstance().save(clModel);
			}
			if ((count % 100) == 0) {
				OBDal.getInstance().flush();
			    OBDal.getInstance().getSession().clear();
			}
		}
		OBDal.getInstance().flush();
	    OBDal.getInstance().getSession().clear();
		SessionHandler.getInstance().commitAndStart();
	}
	
	private Map<String, CLCOMPONENTBRAND> getCLCompBrands() {
		
		final Map<String, CLCOMPONENTBRAND> map = new HashMap<String, CLCOMPONENTBRAND>();
		final OBCriteria<CLCOMPONENTBRAND> obCriteria = OBDal.getInstance().createCriteria(CLCOMPONENTBRAND.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLCOMPONENTBRAND clcomponentbrand : obCriteria.list()) {
				map.put(clcomponentbrand.getName(), clcomponentbrand);
			}
		}
		return map;
	}

	private Map<String, CLBranddepartment> getCLBrandDepts() {
		
		final Map<String, CLBranddepartment> map = new HashMap<String, CLBranddepartment>();
		final OBCriteria<CLBranddepartment> obCriteria = OBDal.getInstance().createCriteria(CLBranddepartment.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLBranddepartment clBranddepartment : obCriteria.list()) {
				map.put(clBranddepartment.getCommercialName(), clBranddepartment);
			}
		}
		return map;
	}

	private Map<String, CLSport> getCLSports() {
		
		final Map<String, CLSport> map = new HashMap<String, CLSport>();
		final OBCriteria<CLSport> obCriteria = OBDal.getInstance().createCriteria(CLSport.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLSport clSport : obCriteria.list()) {
				map.put(clSport.getName(), clSport);
			}
		}
		return map;
	}

	private Map<String, CLSubdepartment> getCLSubDepts() {
		
		final Map<String, CLSubdepartment> map = new HashMap<String, CLSubdepartment>();
		final OBCriteria<CLSubdepartment> obCriteria = OBDal.getInstance().createCriteria(CLSubdepartment.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLSubdepartment clSubdepartment : obCriteria.list()) {
				map.put(clSubdepartment.getCommercialName(), clSubdepartment);
			}
		}
		return map;
	}

	private Map<String, CLNatureOfProduct> getCLNatureOfProducts() {
		
		final Map<String, CLNatureOfProduct> map = new HashMap<String, CLNatureOfProduct>();
		final OBCriteria<CLNatureOfProduct> obCriteria = OBDal.getInstance().createCriteria(CLNatureOfProduct.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLNatureOfProduct clNatureOfProduct : obCriteria.list()) {
				map.put(clNatureOfProduct.getName(), clNatureOfProduct);
			}
		}
		return map;
	}

	private Map<String, CLBrand> getCLBrands() {
		
		final Map<String, CLBrand> map = new HashMap<String, CLBrand>();
		final OBCriteria<CLBrand> obCriteria = OBDal.getInstance().createCriteria(CLBrand.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLBrand clBrand : obCriteria.list()) {
				map.put(clBrand.getName(), clBrand);
			}
		}
		return map;
	}

	private Map<String, ClStoreUniverse> getCLStoreUniverses() {
		
		final Map<String, ClStoreUniverse> map = new HashMap<String, ClStoreUniverse>();
		final OBCriteria<ClStoreUniverse> obCriteria = OBDal.getInstance().createCriteria(ClStoreUniverse.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (ClStoreUniverse clUniverse : obCriteria.list()) {
				map.put(clUniverse.getCommercialName(), clUniverse);
			}
		}
		return map;
	}

	private Map<String, CLUniverse> getCLUniverses() {
		
		final Map<String, CLUniverse> map = new HashMap<String, CLUniverse>();
		final OBCriteria<CLUniverse> obCriteria = OBDal.getInstance().createCriteria(CLUniverse.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLUniverse clUniverse : obCriteria.list()) {
				map.put(clUniverse.getName(), clUniverse);
			}
		}
		return map;
	}

	private Map<String, CLStoreDept> getCLStoreDepts() {
		
		final Map<String, CLStoreDept> map = new HashMap<String, CLStoreDept>();
		final OBCriteria<CLStoreDept> obCriteria = OBDal.getInstance().createCriteria(CLStoreDept.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLStoreDept clStoreDept : obCriteria.list()) {
				map.put(clStoreDept.getName(), clStoreDept);
			}
		}
		return map;
	}

	private Map<String, String> getTypologies() {
		
		final Map<String, String> map = new HashMap<String, String>();
		final OBCriteria<Reference> lifestageRefObCriteria  = OBDal.getInstance().createCriteria(Reference.class);
		lifestageRefObCriteria.add(Restrictions.eq(Reference.PROPERTY_NAME, "Typology"));
		
		final OBCriteria<List> lifestageRefListObCriteria  = OBDal.getInstance().createCriteria(List.class);
		lifestageRefListObCriteria.add(Restrictions.eq(List.PROPERTY_REFERENCE, lifestageRefObCriteria.list().get(0)));
		lifestageRefListObCriteria.setFilterOnActive(false);
		if(lifestageRefListObCriteria.count() > 0) {
			for (List lifeStage : lifestageRefListObCriteria.list()) {
				map.put(lifeStage.getName(), lifeStage.getSearchKey());
			}
		}
		return map;
	}

	private Map<String, CLDepartment> getCLDepts() {
		
		final Map<String, CLDepartment> map = new HashMap<String, CLDepartment>();
		final OBCriteria<CLDepartment> obCriteria = OBDal.getInstance().createCriteria(CLDepartment.class);
		obCriteria.setFilterOnActive(false);
		if(obCriteria.count() > 0) {
			for (CLDepartment clDepartment : obCriteria.list()) {
				map.put(clDepartment.getName(), clDepartment);
			}
		}
		return map;
	}

	public void importItem(java.util.List<OBA_ModelProduct> list) {

		final java.util.Map<String, CLModel> clModelsMap = getCLModels();
		final java.util.Map<String, TaxCategory> taxCategoriesMap = getTaxCategories();
		final java.util.Map<String, CLColor> colorsMap = getCLColors();
		final java.util.Map<String, ProductCategory> productCategorysMap = getProductCategories();
		final java.util.Map<String, UOM> uomMap = getUOMs();
		final java.util.Map<String, List> agesMap = getAges();
		final java.util.Map<String, List> gendersMap = getGenders();
		final java.util.Map<String, List> lifeStagesMap = getLifeStages();
		final java.util.Map<String, List> prodTypesMap = getProdTypes();
		
		final Map<String, ItemImportBean> itemImportBeanMap = getItemImportBeanMap(
				clModelsMap, taxCategoriesMap, colorsMap, productCategorysMap,
				uomMap, agesMap, gendersMap, lifeStagesMap, prodTypesMap, list);
		int count = 0;
		for (OBA_ModelProduct modelProduct : list) {
			
			count++;
			// Item
			final OBCriteria<Product> prodObCriteria = OBDal.getInstance().createCriteria(Product.class);
			prodObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, modelProduct.getItemCode()));
			prodObCriteria.setFilterOnActive(false);
			
			BusinessPartner supplierName = null;
			Location partnerAddress = null;
			CLSOURCING sourcing = null;
			Country country = null;
			if(null != modelProduct.getSupplierCode() && !"".equals(modelProduct.getSupplierCode()) && null != modelProduct.getSourcing() && !"".equals(modelProduct.getSourcing())) {
				
				final OBCriteria<BusinessPartner> bPartnerObCriteria = OBDal.getInstance().createCriteria(BusinessPartner.class);
				bPartnerObCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_CLSUPPLIERNO, modelProduct.getSupplierCode()));
				if(bPartnerObCriteria.count()>0) {
					supplierName = bPartnerObCriteria.list().get(0);
				}
				
				final OBCriteria<Location> locationObCriteria = OBDal.getInstance().createCriteria(Location.class);
				locationObCriteria.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, supplierName));
				if(locationObCriteria.count()>0) {
					partnerAddress = locationObCriteria.list().get(0);
				}
				
				final OBCriteria<CLSOURCING> clSourcingObCriteria = OBDal.getInstance().createCriteria(CLSOURCING.class);
				clSourcingObCriteria.add(Restrictions.eq(CLSOURCING.PROPERTY_NAME, modelProduct.getSourcing()));
				if(clSourcingObCriteria.count()>0) {
					sourcing = clSourcingObCriteria.list().get(0);
				}
				
				final OBCriteria<Country> countryObCriteria = OBDal.getInstance().createCriteria(Country.class);
				countryObCriteria.add(Restrictions.eq(Country.PROPERTY_ISOCOUNTRYCODE, modelProduct.getCountry()));
				if(countryObCriteria.count()>0) {
					country = countryObCriteria.list().get(0);
				}
				
			}			
		
			if(prodObCriteria.count() > 0) {
				final Product prod = prodObCriteria.list().get(0);
				LOG4J.info("update item " + modelProduct.getItemCode());
				prod.setUpdated(new Timestamp(new Date().getTime()));
				prod.setSearchKey(modelProduct.getItemCode());
				prod.setClModelcode(modelProduct.getModelCode());
				prod.setClModelname(modelProduct.getModelName());
				prod.setUpdatedBy(modelProduct.getUpdatedBy());
//				prod.setTaxCategory(itemImportBeanMap.get(modelProduct.getItemCode()).getTaxCategory());
				prod.setClModel(itemImportBeanMap.get(modelProduct.getItemCode()).getClModel());
				if(null != modelProduct.isAcode() && modelProduct.isAcode() ==  true) {
					prod.setClTypea(true);
				} else {
					prod.setClTypea(false);
				}
				if(null != modelProduct.isMadeinindia() && modelProduct.isMadeinindia() ==  true) {
					prod.setClIsmii(true);
				} else {
					prod.setClIsmii(false);
				}
				
				OBDal.getInstance().save(prod);
				
				if(null != modelProduct.getSupplierCode() && !"".equals(modelProduct.getSupplierCode()) && null != modelProduct.getSourcing() && !"".equals(modelProduct.getSourcing())) {
					final OBCriteria<CLPurchasing> clPurchasingObCriteria = OBDal.getInstance().createCriteria(CLPurchasing.class);
					clPurchasingObCriteria.add(Restrictions.eq(CLPurchasing.PROPERTY_MODEL, itemImportBeanMap.get(modelProduct.getItemCode()).getClModel()));
					
					if(clPurchasingObCriteria.count()>0) {
						final CLPurchasing clPurchasing = clPurchasingObCriteria.list().get(0);
						
						clPurchasing.setUpdated(new Timestamp(new Date().getTime()));
						clPurchasing.setUpdatedBy(modelProduct.getUpdatedBy());
						clPurchasing.setSupplierName(supplierName);
						clPurchasing.setPartnerAddress(partnerAddress);
						clPurchasing.setSourcing(sourcing);
						clPurchasing.setSupplierNo(modelProduct.getSupplierCode());
						clPurchasing.setCountry(country);
						clPurchasing.setPurchasingLeadTime(modelProduct.getPurchasingLeadtime());
						
						OBDal.getInstance().save(clPurchasing);
						
					}
				}
				
			} else {
				LOG4J.info("insert item " +  modelProduct.getItemCode());
				final Product prod = OBProvider.getInstance().get(Product.class);
				
				prod.setClient(OBContext.getOBContext().getCurrentClient());
				prod.setOrganization(OBContext.getOBContext().getCurrentOrganization());
				prod.setCreationDate(new Timestamp(new Date().getTime()));
				prod.setUpdated(new Timestamp(new Date().getTime()));
				prod.setCreatedBy(modelProduct.getCreatedBy());
				prod.setUpdatedBy(modelProduct.getUpdatedBy());
				prod.setName(modelProduct.getItemCode());
				prod.setSearchKey(modelProduct.getItemCode());
				prod.setClModelcode(modelProduct.getModelCode());
				prod.setClModelname(modelProduct.getModelName());
				prod.setUPCEAN(modelProduct.getEANCode());
				prod.setClColor(itemImportBeanMap.get(modelProduct.getItemCode()).getClColor());
				prod.setClAge(itemImportBeanMap.get(modelProduct.getItemCode()).getClAge().getSearchKey());
				prod.setClSize(modelProduct.getSize());
				prod.setClGender(itemImportBeanMap.get(modelProduct.getItemCode()).getClGender().getSearchKey());
				prod.setClUeQty(new BigDecimal(modelProduct.getUEQty()));
				prod.setClPcbQty(new BigDecimal(modelProduct.getPCBQty()));
				prod.setClGrosswtPcb(new BigDecimal(modelProduct.getGrossWeight()));
				prod.setWeight(new BigDecimal(modelProduct.getNetWeight()));
				prod.setClVolumePcb(modelProduct.getVolumeOfPCB());
				prod.setClLifestage(itemImportBeanMap.get(modelProduct.getItemCode()).getClLifestage().getSearchKey());
				prod.setProductType(itemImportBeanMap.get(modelProduct.getItemCode()).getProductType().getSearchKey());
				prod.setTaxCategory(itemImportBeanMap.get(modelProduct.getItemCode()).getTaxCategory());
				if(null != modelProduct.isStocked())
					prod.setStocked(true);
				else 
					prod.setStocked(modelProduct.isStocked());
				if(null != modelProduct.isPurchase())
					prod.setPurchase(true);
				else
					prod.setPurchase(modelProduct.isPurchase());
				if(null != modelProduct.isSales())
					prod.setSale(true);
				else 
					prod.setSale(modelProduct.isSales());
				prod.setProductCategory(itemImportBeanMap.get(modelProduct.getItemCode()).getProductCategory());
				prod.setUOM(itemImportBeanMap.get(modelProduct.getItemCode()).getuOM());
				prod.setClModel(itemImportBeanMap.get(modelProduct.getItemCode()).getClModel());
				if(null != modelProduct.isAcode() && modelProduct.isAcode() ==  true) {
					prod.setClTypea(true);
				} else {
					prod.setClTypea(false);
				}
				prod.setClTypeb(false);
				prod.setClTypec(false);
				if(null != modelProduct.isMadeinindia() && modelProduct.isMadeinindia() ==  true) {
					prod.setClIsmii(true);
				} else {
					prod.setClIsmii(false);
				}
				
				OBDal.getInstance().save(prod);
				
				if(null != modelProduct.getSupplierCode() && !"".equals(modelProduct.getSupplierCode()) && null != modelProduct.getSourcing() && !"".equals(modelProduct.getSourcing())) {
					final CLPurchasing clPurchasing = OBProvider.getInstance().get(CLPurchasing.class);
					
					clPurchasing.setOrganization(OBContext.getOBContext().getCurrentOrganization());
					clPurchasing.setClient(OBContext.getOBContext().getCurrentClient());
					clPurchasing.setCreatedBy(modelProduct.getCreatedBy());
					clPurchasing.setUpdatedBy(modelProduct.getUpdatedBy());
					clPurchasing.setCreationDate(new Timestamp(new Date().getTime()));
					clPurchasing.setUpdated(new Timestamp(new Date().getTime()));
					clPurchasing.setSupplierName(supplierName);
					clPurchasing.setPartnerAddress(partnerAddress);
					clPurchasing.setSourcing(sourcing);
					clPurchasing.setSupplierNo(modelProduct.getSupplierCode());
					clPurchasing.setCountry(country);
					clPurchasing.setPurchasingLeadTime(modelProduct.getPurchasingLeadtime());
					
					OBDal.getInstance().save(clPurchasing);
				}
				
			}
			
			if ((count % 100) == 0) {
				OBDal.getInstance().flush();
			    OBDal.getInstance().getSession().clear();
			}
		}
		OBDal.getInstance().flush();
	    OBDal.getInstance().getSession().clear();
		SessionHandler.getInstance().commitAndStart();
	}
	
	private Map<String, List> getLifeStages() {
		
		final Map<String, List> lifeStagesMap = new HashMap<String, List>();
		
		final OBCriteria<Reference> lifestageRefObCriteria  = OBDal.getInstance().createCriteria(Reference.class);
		lifestageRefObCriteria.add(Restrictions.eq(Reference.PROPERTY_NAME, "Lifestage"));
		
		final OBCriteria<List> lifestageRefListObCriteria  = OBDal.getInstance().createCriteria(List.class);
		lifestageRefListObCriteria.add(Restrictions.eq(List.PROPERTY_REFERENCE, lifestageRefObCriteria.list().get(0)));
		lifestageRefListObCriteria.setFilterOnActive(false);
		if(lifestageRefListObCriteria.count() > 0) {
			for (List lifeStage : lifestageRefListObCriteria.list()) {
				lifeStagesMap.put(lifeStage.getName(), lifeStage);
			}
		} 
		return lifeStagesMap;
	}

	private Map<String, List> getProdTypes() {
		
		final Map<String, List> prodTypeMap = new HashMap<String, List>();
		final OBCriteria<Reference> prodTypeRefObCriteria  = OBDal.getInstance().createCriteria(Reference.class);
		prodTypeRefObCriteria.add(Restrictions.eq(Reference.PROPERTY_NAME, "M_Product_ProductType"));
		
		final OBCriteria<List> prodTypeRefListObCriteria  = OBDal.getInstance().createCriteria(List.class);
		prodTypeRefListObCriteria.add(Restrictions.eq(List.PROPERTY_REFERENCE, prodTypeRefObCriteria.list().get(0)));
		prodTypeRefListObCriteria.setFilterOnActive(false);
		if(prodTypeRefListObCriteria.count() > 0) {
			for (List prodType : prodTypeRefListObCriteria.list()) {
				prodTypeMap.put(prodType.getName(), prodType);
			}
		}
		return prodTypeMap;
	}

	private Map<String, List> getGenders() {
		
		final Map<String, List> genderMap = new HashMap<String, List>();
		final OBCriteria<Reference> genderRefObCriteria  = OBDal.getInstance().createCriteria(Reference.class);
		genderRefObCriteria.add(Restrictions.eq(Reference.PROPERTY_NAME, "GenderCat"));
		
		final OBCriteria<List> genderRefListObCriteria  = OBDal.getInstance().createCriteria(List.class);
		genderRefListObCriteria.add(Restrictions.eq(List.PROPERTY_REFERENCE, genderRefObCriteria.list().get(0)));
		genderRefListObCriteria.setFilterOnActive(false);
		if(genderRefListObCriteria.count() > 0) {
			for (List gender : genderRefListObCriteria.list()) {
				genderMap.put(gender.getName(), gender);
			}
		}
		return genderMap;
	}

	private Map<String, List> getAges() {
		
		final Map<String, List> ageMap = new HashMap<String, List>();
		final OBCriteria<Reference> ageRefObCriteria  = OBDal.getInstance().createCriteria(Reference.class);
		ageRefObCriteria.add(Restrictions.eq(Reference.PROPERTY_NAME, "Age"));

		final OBCriteria<List> ageRefListObCriteria  = OBDal.getInstance().createCriteria(List.class);
		ageRefListObCriteria.add(Restrictions.eq(List.PROPERTY_REFERENCE, ageRefObCriteria.list().get(0)));
		ageRefListObCriteria.setFilterOnActive(false);
		if(ageRefListObCriteria.count() > 0) {
			for (List age : ageRefListObCriteria.list()) {
				ageMap.put(age.getName(), age);
			}
		}
		return ageMap;
	}

	private Map<String, ProductCategory> getProductCategories() {
		
		final Map<String, ProductCategory> prodCatMap = new HashMap<String, ProductCategory>();
		final OBCriteria<ProductCategory> prodCatObCriteria = OBDal.getInstance().createCriteria(ProductCategory.class);
		prodCatObCriteria.setFilterOnActive(false);
		if(prodCatObCriteria.count() > 0) {
			for (ProductCategory prodCate : prodCatObCriteria.list()) {
				prodCatMap.put(prodCate.getName(), prodCate);
			}
		}
		return prodCatMap;
	}

	private Map<String, UOM> getUOMs() {
		
		final Map<String, UOM> uomMap = new HashMap<String, UOM>();
		
		final OBCriteria<UOM> uomObCriteria = OBDal.getInstance().createCriteria(UOM.class);
		uomObCriteria.setFilterOnActive(false);
		if(uomObCriteria.count() > 0) {
			for (UOM uom : uomObCriteria.list()) {
				uomMap.put(uom.getName(), uom);
			}
		}
		return uomMap;
	}

	private Map<String, CLColor> getCLColors() {
		
		final Map<String, CLColor> colorMap = new HashMap<String, CLColor>();

		final OBCriteria<CLColor> clColorObCriteria = OBDal.getInstance().createCriteria(CLColor.class);
		clColorObCriteria.setFilterOnActive(false);
		if(clColorObCriteria.count() > 0) {
			for (CLColor color : clColorObCriteria.list()) {
				colorMap.put(color.getName(), color);
			}
		}
		return colorMap;
	}

	private Map<String, CLModel> getCLModels() {
		
		final Map<String, CLModel> clModelMap = new HashMap<String, CLModel>();
		final OBCriteria<CLModel> clModelObCriteria = OBDal.getInstance().createCriteria(CLModel.class);
		clModelObCriteria.setFilterOnActive(false);
		if(clModelObCriteria.count() > 0) {
			for (CLModel clModel : clModelObCriteria.list()) {
				clModelMap.put(clModel.getModelCode(), clModel);
			}
		}
		return clModelMap;
	}

	private Map<String, TaxCategory> getTaxCategories() {

		final Map<String, TaxCategory> getTaxCategoriesMap = new HashMap<String, TaxCategory>();
		final OBCriteria<TaxCategory> taxCategoriesObCriteria = OBDal.getInstance().createCriteria(TaxCategory.class);
		taxCategoriesObCriteria.setFilterOnActive(false);
		if(taxCategoriesObCriteria.count() > 0) {
			for (TaxCategory taxCategory : taxCategoriesObCriteria.list()) {
				getTaxCategoriesMap.put(taxCategory.getName(), taxCategory);
			}
		}
		return getTaxCategoriesMap;
	}

	private Map<String, ItemImportBean> getItemImportBeanMap(
			Map<String, CLModel> clModelsMap,
			Map<String, TaxCategory> taxCategoriesMap,
			Map<String, CLColor> colorsMap,
			Map<String, ProductCategory> productCategorysMap,
			Map<String, UOM> uomMap, Map<String, List> agesMap,
			Map<String, List> gendersMap, Map<String, List> lifeStagesMap,
			Map<String, List> prodTypesMap,
			java.util.List<OBA_ModelProduct> list) {
		
		final Map<String, ItemImportBean> itemImportBeanMap = new HashMap<String, ItemImportBean>();
		
		for (OBA_ModelProduct oba_ModelProduct : list) {
			
			final ItemImportBean itemImportBean = new ItemImportBean();
			
			// cl_model
			if(null != clModelsMap.get(oba_ModelProduct.getModelCode())) {
				itemImportBean.setClModel(clModelsMap.get(oba_ModelProduct.getModelCode()));
			}
			
			// c_taxcategory_id
			for (Map.Entry<String, TaxCategory> entry : taxCategoriesMap.entrySet())
			{
			    if(oba_ModelProduct.getTaxCategory().equalsIgnoreCase(entry.getKey()))
			    	itemImportBean.setTaxCategory(taxCategoriesMap.get(entry.getKey()));
			}
			
			// cl_color_id
			for (Map.Entry<String, CLColor> entry : colorsMap.entrySet()) {
				if(oba_ModelProduct.getColor().equalsIgnoreCase(entry.getKey()))
					itemImportBean.setClColor(colorsMap.get(entry.getKey()));
			}
			
			// product category
			if(null != oba_ModelProduct.getProductCategory() && !"".equals(oba_ModelProduct.getProductCategory())) {
				for (Map.Entry<String, ProductCategory> entry : productCategorysMap.entrySet()) {
					if(oba_ModelProduct.getProductCategory().equalsIgnoreCase(entry.getKey()))
						itemImportBean.setProductCategory(productCategorysMap.get(entry.getKey()));
				}
			} else {
				if(null != productCategorysMap.get("Standard")) {
					itemImportBean.setProductCategory(productCategorysMap.get("Standard"));
				}
			}
			
			// UOM
			if(null != oba_ModelProduct.getUOM() && !"".equals(oba_ModelProduct.getUOM())) {
				for (Map.Entry<String, UOM> entry : uomMap.entrySet()) {
					if(oba_ModelProduct.getUOM().equalsIgnoreCase(entry.getKey()))
						itemImportBean.setuOM(uomMap.get(entry.getKey()));
				}
			} else {
				if(null != uomMap.get("Unit")) {
					itemImportBean.setuOM(uomMap.get("Unit"));
				}
			}
			
			// age
			if(null != agesMap.get(oba_ModelProduct.getAge())) {
				itemImportBean.setClAge(agesMap.get(oba_ModelProduct.getAge()));
			}
			
			// gender
			if(null != gendersMap.get(oba_ModelProduct.getGender())) {
				for (Map.Entry<String, List> entry : gendersMap.entrySet()) {
					if(oba_ModelProduct.getGender().equalsIgnoreCase(entry.getKey()))
						itemImportBean.setClGender(gendersMap.get(entry.getKey()));
				}
			}
			
			// lifestage
			if(null != oba_ModelProduct.getLifeStage()) {
				for (Map.Entry<String, List> entry : lifeStagesMap.entrySet()) {
					if(oba_ModelProduct.getLifeStage().equalsIgnoreCase(entry.getKey()))
						itemImportBean.setClLifestage(lifeStagesMap.get(entry.getKey()));
				}
			}
			
			// prodType
			if(null != oba_ModelProduct.getProductType() && !"".equals(oba_ModelProduct.getProductType())) {
				for (Map.Entry<String, List> entry : prodTypesMap.entrySet()) {
					if(oba_ModelProduct.getProductType().equalsIgnoreCase(entry.getKey()))
						itemImportBean.setProductType(prodTypesMap.get(entry.getKey()));
				}
			} else {
				if(null != prodTypesMap.get("Item")) {
					itemImportBean.setProductType(prodTypesMap.get("Item"));
				}
			}
			itemImportBeanMap.put(oba_ModelProduct.getItemCode(), itemImportBean);
		}
		return itemImportBeanMap;
	}

	public boolean importPrice(java.util.List<OBA_ModelProduct> list) {

		boolean status = false;
int loopCount=0;
		        for (OBA_ModelProduct modelProduct : list) {
            Product prod = null;
            final OBCriteria<Product> prodObCriteria = OBDal.getInstance().createCriteria(Product.class);
            prodObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, modelProduct.getItemCode()));
            if(prodObCriteria.count() > 0) {
                prod = prodObCriteria.list().get(0);
            }
           
            PriceListVersion priceListVersion = null;
            final OBCriteria<PriceListVersion> priceListVersionObCriteria = OBDal.getInstance().createCriteria(PriceListVersion.class);
            priceListVersionObCriteria.add(Restrictions.ilike(PriceListVersion.PROPERTY_NAME, modelProduct.getPricelistVersion()));
            if(priceListVersionObCriteria.count() > 0) {
                priceListVersion = priceListVersionObCriteria.list().get(0);
           }
            if(null != prod && null != priceListVersion) {
                final OBCriteria<ProductPrice> productPriceObCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
                productPriceObCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, prod));
               productPriceObCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION, priceListVersion));
                if(productPriceObCriteria.count() > 0) {
					if (modelProduct.getMRPPrice().compareTo(BigDecimal.ZERO) != 0
							|| modelProduct.getCessionPrice().compareTo(
									BigDecimal.ZERO) != 0
							|| modelProduct.getUnitPrice().compareTo(
									BigDecimal.ZERO) != 0) {
                		
                		final ProductPrice productPrice = productPriceObCriteria.list().get(0);
                        LOG4J.info("price update "+prod.getName());
                        if(modelProduct.getMRPPrice().compareTo(BigDecimal.ZERO) != 0)
                        	productPrice.setClMrpprice(modelProduct.getMRPPrice());
                        if(modelProduct.getCessionPrice().compareTo(BigDecimal.ZERO) != 0)
                        	productPrice.setClCessionprice(modelProduct.getCessionPrice());
                        if(modelProduct.getUnitPrice().compareTo(BigDecimal.ZERO) != 0) {
                       	productPrice.setClCcunitprice(modelProduct.getUnitPrice());
                            productPrice.setClCcueprice(modelProduct.getUEPrice());
                            productPrice.setClCcpcbprice(modelProduct.getPCBPrice());
                        }
                        productPrice.setUpdated(new Timestamp(new Date().getTime()));
                        productPrice.setUpdatedBy(modelProduct.getUpdatedBy());
                       
                        OBDal.getInstance().save(productPrice);
                        OBDal.getInstance().flush();
                	}
                    
                   
                } else {
                   final ProductPrice productPrice = OBProvider.getInstance().get(ProductPrice.class);
                    LOG4J.info("price insert "+prod.getName());
                    productPrice.setPriceListVersion(priceListVersion);
                   productPrice.setProduct(prod);
                    productPrice.setClient(OBContext.getOBContext().getCurrentClient());
                   productPrice.setOrganization(OBContext.getOBContext().getCurrentOrganization());
                    productPrice.setCreatedBy(modelProduct.getCreatedBy());
                    productPrice.setClMrpprice(modelProduct.getMRPPrice());
                    productPrice.setClCessionprice(modelProduct.getCessionPrice());
                    productPrice.setClCcunitprice(modelProduct.getUnitPrice());
                    productPrice.setClCcueprice(modelProduct.getUEPrice());
                    productPrice.setClCcpcbprice(modelProduct.getPCBPrice());
                  
                    OBDal.getInstance().save(productPrice);
                    OBDal.getInstance().flush();
                   
                }
            }
            if(loopCount%100 == 0) {
                //OBDal.getInstance().flush();
            }
            loopCount++;
        }
        //OBDal.getInstance().flush();
        SessionHandler.getInstance().commitAndStart();
        return status;
    }
 	

	
}
