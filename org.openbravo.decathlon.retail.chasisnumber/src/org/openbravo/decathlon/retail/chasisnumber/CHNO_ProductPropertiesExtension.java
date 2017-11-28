package org.openbravo.decathlon.retail.chasisnumber;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.retail.posterminal.master.Product;
import org.openbravo.retail.posterminal.master.ProductProperties;

@Qualifier(Product.productPropertyExtension)
public class CHNO_ProductPropertiesExtension extends ProductProperties {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {

    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>();

    list.add(new HQLProperty("product.clIschasisnumberrequired", "clIschasisnumberrequired"));

    return list;
  }
}
