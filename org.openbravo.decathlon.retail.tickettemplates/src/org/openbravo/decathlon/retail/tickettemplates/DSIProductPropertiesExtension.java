package org.openbravo.decathlon.retail.tickettemplates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.retail.posterminal.master.Product;
import org.openbravo.retail.posterminal.master.ProductProperties;

@Qualifier(Product.productPropertyExtension)
public class DSIProductPropertiesExtension extends ProductProperties {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {

    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>();

    // Calculate POS Precision
    String localPosPrecision = "";
    try {
      if (params != null) {
        @SuppressWarnings("unchecked")
        HashMap<String, Object> localParams = (HashMap<String, Object>) params;
        localPosPrecision = (String) localParams.get("posPrecision");
      }
    } catch (Exception e) {
      log.error("Error getting posPrecision: " + e.getMessage(), e);
    }
    final String posPrecision = localPosPrecision;

    if (posPrecision != null && !"".equals(posPrecision)) {
      list.add(new HQLProperty("round(ppp.clMrpprice, " + posPrecision + ")", "dsitite_mrpPrice"));
    } else {
      list.add(new HQLProperty("ppp.clMrpprice", "dsitite_mrpPrice"));
    }
    if (posPrecision != null && !"".equals(posPrecision)) {
      list.add(new HQLProperty("round(ppp.clCessionprice, " + posPrecision + ")",
          "dsitite_clCessionprice"));
    } else {
      list.add(new HQLProperty("ppp.clCessionprice", "dsitite_clCessionprice"));
    }

    list.add(new HQLProperty("ppp.clLbtrate", "dsitite_clLbtrate"));

    list.add(new HQLProperty("product.clModelname", "dsitite_modelName"));
    list.add(new HQLProperty("product.clModel.brand.name", "dsitite_brandName"));

    return list;
  }
}
