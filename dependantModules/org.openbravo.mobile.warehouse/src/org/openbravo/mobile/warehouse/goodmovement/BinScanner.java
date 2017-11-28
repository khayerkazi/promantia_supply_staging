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

public class BinScanner implements BarcodeScanner {
  private static final Logger log = Logger.getLogger(BinScanner.class);

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
    OBCriteria<Locator> qBin = OBDal.getInstance().createCriteria(Locator.class);
    qBin.add(Restrictions.eq(Locator.PROPERTY_BARCODE, code));
    List<Locator> bins = qBin.list();
    if (bins.size() == 1) {
      Locator bin = bins.get(0);

      // do checks
      if ((!line.has("fromBin") || line.isNull("fromBin")) && line.has("product")
          && !line.isNull("product")) {
        // setting from bin with a product already selected

        OBCriteria<StorageDetail> qStock = OBDal.getInstance().createCriteria(StorageDetail.class);
        qStock.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT + ".id",
            line.getString("product")));
        qStock.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, bin));
        if (line.has("quantity") && !line.isNull("quantity")) {
          qStock.add(Restrictions.ge(StorageDetail.PROPERTY_QUANTITYONHAND,
              BigDecimal.valueOf(line.getDouble("quantity"))));
        } else {
          qStock.add(Restrictions.gt(StorageDetail.PROPERTY_QUANTITYONHAND, BigDecimal.ZERO));
        }
        if (qStock.count() == 0) {
          response.put("status", 1);
          Product product = OBDal.getInstance().get(Product.class, line.getString("product"));
          JSONObject jsonError = new JSONObject();
          String msg = OBMessageUtils.getI18NMessage("OBWH_ScannedBinWithoutStockOfProduct",
              new String[] { bin.getIdentifier(), code, product.getIdentifier() });
          jsonError.put("message", msg);
          response.put("error", jsonError);
          log.warn(msg);
          return response;
        }
      }

      // checks are ok, let's go forward
      response.put("status", 0);
      JSONObject jsonBin = new JSONObject();
      jsonBin.put("id", bin.getId());
      jsonBin.put("name", bin.getIdentifier());

      JSONObject data = new JSONObject();
      data.put("bin", jsonBin);
      response.put("data", data);
      log.debug("found bin for code [" + code + "]:" + bin);
    } else if (bins.size() > 1) {
      response.put("status", 1);
      JSONObject jsonError = new JSONObject();
      jsonError.put("message",
          OBMessageUtils.getI18NMessage("OBWH_SeveralBinsWithSameCode", new String[] { code }));
      response.put("error", jsonError);
      log.warn("found several bins for code [" + code + "]");
    }
    return response;
  }

}
