package org.openbravo.warehouse.shipping.ad_callout;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.ProductPrice;

import com.sysfore.decathlonimport.IM_Movement;


public class CalculateCessionPrice extends SimpleCallout {
	static Logger log4j = Logger.getLogger(CalculateCessionPrice.class);

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		String movementQty=info.getStringParameter("inpmovementqty");
		String productId=info.getStringParameter("inpmProductId");
	    Product productyObj = OBDal.getInstance().get(Product.class,productId);
	    //String taxRate=info.getStringParameter("inpemObwshipTaxrate");
		BigDecimal rate=getRate(productyObj);
		BigDecimal price=getCessionprice(productyObj);
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

	private BigDecimal getRate(Product productyObj) {
		
		OBCriteria<TaxRate> criteritax = OBDal.getInstance().createCriteria(TaxRate.class);
		criteritax.add(Restrictions .eq(TaxRate.PROPERTY_TAXCATEGORY, productyObj.getTaxCategory()));
		criteritax.add(Restrictions .eq(TaxRate.PROPERTY_CLIENT , OBContext .getOBContext().getCurrentClient()));
		criteritax.add(Restrictions .eq(TaxRate.PROPERTY_INTXINDIANTAXCATEGORY , "GS_IGST"));
        if(criteritax.list().size()>0){
	    return criteritax.list().get(0).getRate();
      }else{
	    throw new OBException("There is no igst tax related to  Product  "+productyObj.getName()); 
       }
		
	}

	private BigDecimal getCessionprice(Product productObj) {	    
	    OBCriteria<ProductPrice> priceCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
		priceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, productObj));
		if(priceCriteria.list().size()>0){
	    return priceCriteria.list().get(0).getClCessionprice();
	}else{
		return new BigDecimal(0.0);
	}

}
	}
