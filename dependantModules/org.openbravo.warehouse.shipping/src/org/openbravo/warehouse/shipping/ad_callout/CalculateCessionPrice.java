package org.openbravo.warehouse.shipping.ad_callout;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.pricing.pricelist.ProductPrice;

import com.sysfore.decathlonimport.IM_Movement;


public class CalculateCessionPrice extends SimpleCallout {
	static Logger log4j = Logger.getLogger(CalculateCessionPrice.class);

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		String movementQty=info.getStringParameter("inpmovementqty");
		String productId=info.getStringParameter("inpmProductId");
		String taxRate=info.getStringParameter("inpemObwshipTaxrate");
		BigDecimal rate=new BigDecimal(taxRate);
		BigDecimal price=getCessionprice(productId);
		price=price.multiply(new BigDecimal(movementQty));
		BigDecimal amt=price.multiply(new BigDecimal(100.0));
		BigDecimal divideFactor=rate.add(new BigDecimal(100.0));
		BigDecimal taxableAmount=amt.divide(divideFactor,2,BigDecimal .ROUND_HALF_UP);
		BigDecimal taxAmount=price.subtract(taxableAmount);
	    info.addResult("inpemObwshipCessionprice", price);
	    info.addResult("inpemObwshipTaxamount", taxAmount);
	    info.addResult("inpemObwshipTaxableamount", taxableAmount);


		
	}

	private BigDecimal getCessionprice(String productId) {
	    Product productObj = OBDal.getInstance().get(Product.class,
	    		productId);
	    
	    OBCriteria<ProductPrice> priceCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
		priceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, productObj));
		if(priceCriteria.list().size()>0){
	    return priceCriteria.list().get(0).getClCessionprice();
	}else{
		return new BigDecimal(0.0);
	}

}
	}
