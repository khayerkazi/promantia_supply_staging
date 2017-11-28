/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.posterminal.master;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.retail.config.OBRETCOProductList;
import org.openbravo.retail.posterminal.POSUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class Product extends ProcessHQLQuery {
  public static final String productPropertyExtension = "OBPOS_ProductExtension";
  public static final Logger log = Logger.getLogger(Product.class);

  @Inject
  @Any
  @Qualifier(productPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    String orgId = OBContext.getOBContext().getCurrentOrganization().getId();
    final OBRETCOProductList productList = POSUtils.getProductListByOrgId(orgId);

    final Date terminalDate = OBMOBCUtils.calculateServerDate(jsonsent.getJSONObject("parameters")
        .getString("terminalTime"),
        jsonsent.getJSONObject("parameters").getJSONObject("terminalTimeOffset").getLong("value"));

    final PriceList priceList = POSUtils.getPriceListByOrgId(orgId);
    final PriceListVersion priceListVersion = POSUtils.getPriceListVersionByOrgId(orgId,
        terminalDate);

    if (productList == null) {
      throw new JSONException("Product list not found");
    }

    SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
    Calendar now = Calendar.getInstance();

    List<String> products = new ArrayList<String>();
    String posPrecision = "";
    try {
      OBContext.setAdminMode();
      posPrecision = (priceList.getCurrency().getObposPosprecision() == null ? priceList
          .getCurrency().getPricePrecision() : priceList.getCurrency().getObposPosprecision())
          .toString();
    } catch (Exception e) {
      log.error("Error getting currency by id: " + e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }

    Map<String, Object> args = new HashMap<String, Object>();
    args.put("posPrecision", posPrecision);

    HQLPropertyList regularProductsHQLProperties = ModelExtensionUtils.getPropertyExtensions(
        extensions, args);

    // regular products
    products
        .add("select"
            + regularProductsHQLProperties.getHqlSelect()
            + "FROM OBRETCO_Prol_Product as pli left outer join pli.product.image img inner join pli.product as product, "
            + "PricingProductPrice ppp, "
            + "PricingPriceListVersion pplv "
            + "WHERE (pli.obretcoProductlist = '"
            + productList.getId()
            + "') "
            + "AND ("
            + "pplv.id='"
            + priceListVersion.getId()
            + "'"
            + ") AND ("
            + "ppp.priceListVersion.id = pplv.id"
            + ") AND ("
            + "pli.product.id = ppp.product.id"
            + ") AND ("
            + "pli.product.active = true"
            + ") AND "
            + "((pli.$incrementalUpdateCriteria) or (pli.product.$incrementalUpdateCriteria) or (ppp.$incrementalUpdateCriteria) ) order by pli.product.name");

    // discounts which type is defined as category
    String discountNameTrl;
    if (OBContext.hasTranslationInstalled()) {
      discountNameTrl = "coalesce ((select pt.name from PricingAdjustmentTrl pt where pt.promotionDiscount=p and pt.language='"
          + OBContext.getOBContext().getLanguage().getLanguage() + "'), p.name) ";
    } else {
      discountNameTrl = "p.name";
    }
    products
        .add("select p.id as id, "
            + discountNameTrl
            + " as searchkey, "
            + discountNameTrl
            + " as _identifier, p.discountType.id as productCategory, round(p.obdiscPrice, "
            + posPrecision
            + ") as listPrice, round(p.obdiscPrice, "
            + posPrecision
            + ") as standardPrice, p.obdiscUpc as uPCEAN, img.bindaryData as img, '[[null]]' as generic_product_id, 'false' as showchdesc, 'true' as ispack, 'false' as isGeneric , 'false' as stocked"//
            + "  from PricingAdjustment as p left outer join p.obdiscImage img" //
            + " where p.discountType.obposIsCategory = true "//
            + "   and p.discountType.active = true " //
            + "   and p.active = true"//
            + "   and p.$readableClientCriteria"//
            + "   and (p.endingDate is null or p.endingDate >= TO_DATE('"
            + format.format(now.getTime())
            + "', 'yyyy/MM/dd'))" //
            + "   and p.startingDate <= TO_DATE('"
            + format.format(now.getTime())
            + "', 'yyyy/MM/dd')"
            + "   and (p.$incrementalUpdateCriteria) "//
            // organization
            + "and ((p.includedOrganizations='Y' " + "  and not exists (select 1 "
            + "         from PricingAdjustmentOrganization o" + "        where active = true"
            + "          and o.priceAdjustment = p" + "          and o.organization.id ='" + orgId
            + "')) " + "   or (p.includedOrganizations='N' " + "  and  exists (select 1 "
            + "         from PricingAdjustmentOrganization o" + "        where active = true"
            + "          and o.priceAdjustment = p" + "          and o.organization.id ='" + orgId
            + "')) " + "    ) ");

    // generic products
    products
        .add("select "
            + regularProductsHQLProperties.getHqlSelect()
            + " from Product product left outer join product.image img left join product.oBRETCOProlProductList as pli left outer join product.pricingProductPriceList ppp where exists (select 1 from Product product2 left join product2.oBRETCOProlProductList as pli2, PricingProductPrice ppp2 where product.id = product2.genericProduct.id and product2 = ppp2.product and ppp2.priceListVersion.id = '"
            + priceListVersion.getId() + "' and pli2.obretcoProductlist.id = '"
            + productList.getId() + "')");

    return products;

  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }
}
