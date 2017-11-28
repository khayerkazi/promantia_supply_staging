package org.openbravo.retail.levelpricing.master;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.core.model.ModelExtensionUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.retail.config.OBRETCOProductList;
import org.openbravo.retail.posterminal.POSUtils;
import org.openbravo.retail.posterminal.ProcessHQLQuery;

public class LevelProductPrices extends ProcessHQLQuery {
  public static final String levelProductPricesPropertyExtension = "POSLVPR_LevelProductPricesExtension";

  @Inject
  @Any
  @Qualifier(levelProductPricesPropertyExtension)
  private Instance<ModelExtension> extensions;

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    String orgId = OBContext.getOBContext().getCurrentOrganization().getId();
    final Date terminalDate = OBMOBCUtils.calculateServerDate(jsonsent.getJSONObject("parameters")
        .getString("terminalTime"),
        jsonsent.getJSONObject("parameters").getJSONObject("terminalTimeOffset").getLong("value"));
    final OBRETCOProductList productList = POSUtils.getProductListByOrgId(orgId);
    final PriceListVersion plv = POSUtils.getPriceListVersionByOrgId(orgId, terminalDate);

    HQLPropertyList regularLevelProductPriceHQLProperties = ModelExtensionUtils
        .getPropertyExtensions(extensions);

    return Arrays
        .asList(new String[] { "SELECT"
            + regularLevelProductPriceHQLProperties.getHqlSelect()
            + "from lvlpr_levelproductprice lvlpp where lvlpp.productPrice.product.id IN (select product.id from OBRETCO_Prol_Product where obretcoProductlist.id = '"
            + productList.getId() + "') and lvlpp.productPrice.priceListVersion.id = '"
            + plv.getId()
            + "' and lvlpp.$readableClientCriteria and (lvlpp.$incrementalUpdateCriteria)" });
  }
}
