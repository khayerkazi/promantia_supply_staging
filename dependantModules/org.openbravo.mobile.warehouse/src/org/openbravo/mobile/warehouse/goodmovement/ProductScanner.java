/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.goodmovement;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.mobile.warehouse.barcode.BarcodeScanner;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

public class ProductScanner implements BarcodeScanner {
  private static final Logger log = Logger.getLogger(ProductScanner.class);

  @Override
  public Boolean isValidEvent(String eventName) throws JSONException {
    if (eventName.equals("OBWH_goodsMovements")) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public JSONObject scan(String code, JSONObject line) throws JSONException {
    JSONObject response = new JSONObject();
    OBCriteria<Product> qProduct = OBDal.getInstance().createCriteria(Product.class);
    qProduct.add(Restrictions.eq(Product.PROPERTY_UPCEAN, code));
    List<Product> products = qProduct.list();
    if (products.size() == 1) {
      Product product = products.get(0);

      // Check product stock
      if (line.has("fromBin") && !line.isNull("fromBin")) {
        String fromBinId = line.getString("fromBin");
        OBCriteria<StorageDetail> qStock = OBDal.getInstance().createCriteria(StorageDetail.class);
        qStock.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
        qStock.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN + ".id", fromBinId));
        qStock.add(Restrictions.gt(StorageDetail.PROPERTY_QUANTITYONHAND, BigDecimal.ZERO));

        if (qStock.count() == 0) {
          response.put("status", 1);
          Locator bin = OBDal.getInstance().get(Locator.class, fromBinId);
          JSONObject jsonError = new JSONObject();
          String msg = OBMessageUtils.getI18NMessage("OBWH_ScannedProductWithoutStockInBin",
              new String[] { product.getIdentifier(), code, bin.getIdentifier() });
          jsonError.put("message", msg);
          response.put("error", jsonError);
          log.warn(msg);
          return response;
        }
      }

      // checks are ok, let's go forward
      response.put("status", 0);
      JSONObject jsonProduct = new JSONObject();
      jsonProduct.put("id", product.getId());
      jsonProduct.put("name", product.getIdentifier());
      jsonProduct.put("uom.name", product.getUOM().getIdentifier());
      jsonProduct.put("uom.id", product.getUOM().getId());
      jsonProduct.put("hasAttribute", product.getAttributeSet() != null);
      if (!line.has("quantity") || line.isNull("quantity") || line.getDouble("quantity") == 0) {
        jsonProduct.put("quantity", 1);
      }

      JSONObject data = new JSONObject();
      data.put("product", jsonProduct);
      response.put("data", data);
      log.debug("found product for code [" + code + "]:" + product);
    } else if (products.size() > 1) {
      response.put("status", 1);
      JSONObject jsonError = new JSONObject();
      jsonError.put("message",
          OBMessageUtils.getI18NMessage("OBWH_SeveralProductsWithSameCode", new String[] { code }));
      response.put("error", jsonError);
      log.warn("found several products for code [" + code + "]");
    }
    return response;
  }
}
