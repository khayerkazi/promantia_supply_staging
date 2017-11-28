package org.openbravo.retail.levelpricing.master;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;

@Qualifier(LevelProductPrices.levelProductPricesPropertyExtension)
public class LevelProductPricesProperties extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      {
        add(new HQLProperty("lvlpp.id", "id"));
        add(new HQLProperty("lvlpp.productPrice.product.id", "productId"));
        add(new HQLProperty("lvlpp.quantity", "quantity"));
        add(new HQLProperty("lvlpp.price", "price"));
        add(new HQLProperty("lvlpp.lvlprRange.name", "_identifier"));
      }
    };
    return list;
  }

}
