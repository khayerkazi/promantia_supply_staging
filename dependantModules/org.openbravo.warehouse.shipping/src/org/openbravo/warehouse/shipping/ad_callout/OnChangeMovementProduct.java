package org.openbravo.warehouse.shipping.ad_callout;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SL_Movement_Product;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.ProductPrice;

public class OnChangeMovementProduct extends SL_Movement_Product {
	static Logger log4j = Logger.getLogger(OnChangeMovementProduct.class);

	protected void execute(CalloutInfo info) throws ServletException{
	
		super.execute(info);
		
		log4j.debug("Core Callout SL_Movement_Product got executed ");
		String productId=info.getStringParameter("inpmProductId");
		String movementQty=info.getStringParameter("inpmovementqty");
	    Product productObj = OBDal.getInstance().get(Product.class,productId);
	    try{
	    BigDecimal rate=getRate( productObj);
	    if(rate==null)
	    {
	    	rate=BigDecimal.ZERO;
	    }
	    else{
	    	
			BigDecimal price=getCessionprice(productObj);
			price=price.multiply(new BigDecimal(movementQty));
			BigDecimal amt=price.multiply(new BigDecimal(100.0));
			BigDecimal divideFactor=rate.add(new BigDecimal(100.0));
			BigDecimal taxableAmount=amt.divide(divideFactor,2,BigDecimal .ROUND_HALF_UP);
			BigDecimal taxAmount=price.subtract(taxableAmount);
		    info.addResult("inpemObwshipCessionprice", price);
		    info.addResult("inpemObwshipTaxamount", taxAmount);
		    info.addResult("inpemObwshipTaxableamount", taxableAmount);
		    info.addResult("inpemObwshipTaxrate", rate);
	    	
	    }

		}catch (Exception e){
			log4j.error("Error in excecuting callout OnChangeMovementProduct "+e.getStackTrace());
			throw new OBException("There is no igst tax related to  Product "+productObj.getName());
			
		}
	}

	private BigDecimal getCessionprice(Product productObj) {
	    OBCriteria<ProductPrice> priceCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
		priceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, productObj));
		if(priceCriteria.list().size()>0){
	    return priceCriteria.list().get(0).getClCessionprice();
	}else{
		log4j.error("There is no Cession price related to  Product "+productObj.getName());

		return new BigDecimal(0.0);
	}
	}

	private BigDecimal getRate(Product productObj) {
		OBCriteria<TaxRate> criteritax = OBDal.getInstance().createCriteria(TaxRate.class);
		criteritax.add(Restrictions .eq(TaxRate.PROPERTY_TAXCATEGORY, productObj.getTaxCategory()));
		criteritax.add(Restrictions .eq(TaxRate.PROPERTY_CLIENT , OBContext .getOBContext().getCurrentClient()));
		criteritax.add(Restrictions .eq(TaxRate.PROPERTY_INTXINDIANTAXCATEGORY , "GS_IGST"));
		if(criteritax.list().size()>0){
	    return criteritax.list().get(0).getRate();
		}else{
			log4j.error("There is no igst tax related to  Product "+productObj.getName());
		    return new BigDecimal(0.0);


		}
	}
}
