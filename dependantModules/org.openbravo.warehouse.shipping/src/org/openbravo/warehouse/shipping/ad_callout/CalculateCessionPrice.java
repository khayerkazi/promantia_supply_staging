package org.openbravo.warehouse.shipping.ad_callout;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.pricing.pricelist.ProductPrice;

public class CalculateCessionPrice extends SimpleCallout {
  static Logger log4j = Logger.getLogger(CalculateCessionPrice.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    BigDecimal existingCessionPrice = null;
    String movementid = info.getStringParameter("inpmMovementId");
    InternalMovement movementObj = OBDal.getInstance().get(InternalMovement.class, movementid);
    if (movementObj.getSWMovementtypegm().equalsIgnoreCase("Saleable Fixture WH-WH")) {

      String movementQty = info.getStringParameter("inpmovementqty", null);
      String productId = info.getStringParameter("inpmProductId", null);
      String cessionPrice = info.getStringParameter("inpemObwshipCessionprice", null);

      if (cessionPrice != "") {
        String CessionPriceNew = cessionPrice.replaceAll(",", "");
        existingCessionPrice = new BigDecimal(CessionPriceNew);
      }
      Product productyObj = OBDal.getInstance().get(Product.class, productId);
      BigDecimal rate = getRate(productyObj);

      if (rate == null) {
        rate = BigDecimal.ZERO;
      } else {
        if (cessionPrice == "") {
          String taxRate = info.getStringParameter("inpemObwshipTaxrate");
          BigDecimal price = getCessionprice(productyObj);
          BigDecimal unitPrice = price;
          price = price.multiply(new BigDecimal(movementQty));
          BigDecimal amt = price.multiply(new BigDecimal(100.0));
          BigDecimal divideFactor = rate.add(new BigDecimal(100.0));
          BigDecimal taxableAmount = amt.divide(divideFactor, 2, BigDecimal.ROUND_HALF_UP);
          BigDecimal taxAmount = price.subtract(taxableAmount);

          info.addResult("inpemObwshipCessionprice", unitPrice);
          info.addResult("inpemObwshipTaxamount", taxAmount);
          info.addResult("inpemObwshipTaxableamount", taxableAmount);
          info.addResult("inpemObwshipTaxrate", rate);

        }
        if (cessionPrice != "") {
          BigDecimal taxableAmount = existingCessionPrice.multiply(new BigDecimal(movementQty));

          BigDecimal taxAmount = taxableAmount.multiply(rate.divide(new BigDecimal(100.0), 2,
              BigDecimal.ROUND_HALF_UP));

          info.addResult("inpemObwshipCessionprice", existingCessionPrice);
          info.addResult("inpemObwshipTaxamount", taxAmount);
          info.addResult("inpemObwshipTaxableamount", taxableAmount);
          info.addResult("inpemObwshipTaxrate", rate);
        }
      }
    }

  }

  private BigDecimal getRate(Product productyObj) {

    Query qry;
    String getRate = "select rate from FinancialMgmtTaxRate e where e.taxCategory.id='"
        + productyObj.getTaxCategory().getId() + "' and e.intxIndianTaxcategory='GS_IGST'";
    qry = OBDal.getInstance().getSession().createQuery(getRate);
    List<BigDecimal> queryList = qry.list();
    if (queryList.size() > 0) {

      return queryList.get(0);

    } else {
      return null;
    }

  }

  private BigDecimal getCessionprice(Product productObj) {
    OBCriteria<ProductPrice> priceCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
    priceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, productObj));
    if (priceCriteria.list().size() > 0) {
      return priceCriteria.list().get(0).getClCessionprice();
    } else {
      return new BigDecimal(0.0);
    }

  }
}
