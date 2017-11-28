/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.pricing.scalablelevelpricing.hooks;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.hibernate.criterion.Restrictions;
import org.openbravo.common.hooks.OrderLineQtyChangedHook;
import org.openbravo.common.hooks.OrderLineQtyChangedHookObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.pricing.levelpricing.LevelProductPrice;
import org.openbravo.retail.posterminal.POSUtils;

@ApplicationScoped
public class OrderLineQtyChangedHookImplementation implements OrderLineQtyChangedHook {

  @Override
  public void exec(OrderLineQtyChangedHookObject hookObj) throws Exception {
    BigDecimal newPrice = null;
    if (hookObj.getChanged().equals("inpqtyordered")) {
      Order ord = OBDal.getInstance().get(Order.class, hookObj.getOrderId());
      Product prod = OBDal.getInstance().get(Product.class, hookObj.getProductId());
      PriceList plist = ord.getPriceList();
      PriceListVersion plistversion = POSUtils.getPriceListVersionForPriceList(plist.getId(),
          new Date());
      final OBCriteria<ProductPrice> prodPriceQuery = OBDal.getInstance().createCriteria(
          ProductPrice.class);
      prodPriceQuery.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, prod));
      prodPriceQuery.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION, plistversion));

      List<ProductPrice> lstProductPrice = prodPriceQuery.list();

      if (!lstProductPrice.isEmpty() && lstProductPrice.size() == 1) {
        if (lstProductPrice.get(0).getAlgorithm().equals("SLP_algorithm")) {
          ProductPrice origProductPrice = FinancialUtils.getProductPrice(prod, ord.getOrderDate(),
              true, plist);

          final OBCriteria<LevelProductPrice> levelPriceQuery = OBDal.getInstance().createCriteria(
              LevelProductPrice.class);
          levelPriceQuery.add(Restrictions.eq(LevelProductPrice.PROPERTY_PRODUCTPRICE,
              lstProductPrice.get(0)));
          levelPriceQuery
              .add(Restrictions.le(LevelProductPrice.PROPERTY_QUANTITY, hookObj.getQty()));
          levelPriceQuery.addOrderBy(LevelProductPrice.PROPERTY_QUANTITY, false);
          List<LevelProductPrice> lstLevelProductPrice = levelPriceQuery.list();
          for (LevelProductPrice levelProductPrice : lstLevelProductPrice) {
            if (hookObj.getQty().compareTo(levelProductPrice.getQuantity()) >= 0) {
              newPrice = levelProductPrice.getPrice();
              break;
            }
          }
          if (newPrice != null) {
            hookObj.setPrice(newPrice.equals("") ? BigDecimal.ZERO : newPrice.setScale(
                hookObj.getPricePrecision(), BigDecimal.ROUND_HALF_UP));
          } else {
            hookObj.setPrice(origProductPrice.getStandardPrice());
          }
        }
      }
    }
  }
}