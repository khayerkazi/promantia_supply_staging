package org.openbravo.localization.india.ingst.sales;

import java.math.BigDecimal;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;

public class OnchangeProductHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    // TODO Auto-generated method stub
    BigDecimal costPrice = new BigDecimal(0);
    try {
      final JSONObject jsonData = new JSONObject(data);
      String gstProdCode = "";
      final String strmProductId = jsonData.getString("prodId");

      if (strmProductId != "null") {
        Product product = OBDal.getInstance().get(Product.class, strmProductId);
        gstProdCode = product.getIngstGstproductcode().getId();
      }

      JSONObject returnData = new JSONObject();
      returnData.put("gstProdCode", gstProdCode);
      return returnData;

    } catch (Exception e) {
      throw new OBException(e);
    }

  }
}