/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.barcode;

import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.warehouse.barcode.BarcodeScanner;
import org.openbravo.mobile.warehouse.picking.WHPickingConstants;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;

public class PickingProductScanner implements BarcodeScanner {
  private static final Logger log = Logger.getLogger(PickingProductScanner.class);

  @Override
  public Boolean isValidEvent(String eventName) throws JSONException {
    if (eventName.equals(WHPickingConstants.PICKING_PROPERTY)) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public JSONObject scan(String code, JSONObject arguments) throws JSONException {
    Product productToReturn = null;
    String errorMessage = "";
    JSONObject response = new JSONObject();
    Boolean foundByLot = false;
    Boolean foundByProductUPC = false;
    String prefValue;
    log.warn("Scanned UPC/EAN code: " + code);
    try {
      prefValue = Preferences.getPreferenceValue("OBMWHP_scanusingselectedline", true, OBContext
          .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    } catch (PropertyException e) {
      prefValue = "N";
    }
    if ("Y".equals(prefValue)) {
      // search product by upc ean
      OBCriteria<Product> qProduct = OBDal.getInstance().createCriteria(Product.class);
      qProduct.add(Restrictions.eq(Product.PROPERTY_UPCEAN, code));
      List<Product> products = qProduct.list();
      if (products.size() == 1) {
        productToReturn = products.get(0);
        if (productToReturn.getId().equals(arguments.getJSONObject("item").get("productId"))) {
          foundByProductUPC = true;
        } else {
          errorMessage = "Product found using UPC/EAN doesn't match with selected line on picking list app";
          log.warn(errorMessage);
          productToReturn = null;
        }

      } else {
        if (products.size() == 0) {
          errorMessage = "No product found with the same upc/ean code";
        } else {
          errorMessage = "More than one product with the same upc/ean code";
        }
        log.warn(errorMessage);
      }
    } else {
      OBCriteria<AttributeSetInstance> qAttSetInstance = OBDal.getInstance().createCriteria(
          AttributeSetInstance.class);
      qAttSetInstance.add(Restrictions.eq(AttributeSetInstance.PROPERTY_LOTNAME, code));
      qAttSetInstance.setMaxResults(1);
      List<AttributeSetInstance> attSetInstances = qAttSetInstance.list();
      if (attSetInstances.size() > 0) {
        String[] strBins = arguments.getString("binids").split(",");
        AttributeSetInstance attSetInstance = attSetInstances.get(0);

        OBCriteria<StorageDetail> qStorageDetail = OBDal.getInstance().createCriteria(
            StorageDetail.class);
        qStorageDetail.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE + ".id",
            attSetInstance.getId()));
        qStorageDetail.add(Restrictions.in(StorageDetail.PROPERTY_STORAGEBIN + ".id", strBins));
        qStorageDetail.setMaxResults(1);
        List<StorageDetail> stDetailList = qStorageDetail.list();
        if (stDetailList.size() > 0) {
          productToReturn = stDetailList.get(0).getProduct();
          foundByLot = true;
        } else {
          errorMessage = "Products with lot " + code + " have not been found in storage detail";
          log.warn(errorMessage);
        }
      } else {
        OBCriteria<Product> qProduct = OBDal.getInstance().createCriteria(Product.class);
        qProduct.add(Restrictions.eq(Product.PROPERTY_UPCEAN, code));
        List<Product> products = qProduct.list();
        if (products.size() == 1) {
          productToReturn = products.get(0);
        } else {
          if (products.size() == 0) {
            errorMessage = "No product found with the same upc/ean code";
          } else {
            errorMessage = "More than one product with the same upc/ean code";
          }
          log.warn(errorMessage);
        }
      }
    }

    if (productToReturn != null) {
      response.put("status", 0);
      JSONObject jsonProduct = new JSONObject();
      jsonProduct.put("id", productToReturn.getId());
      jsonProduct.put("name", productToReturn.getIdentifier());
      jsonProduct.put("hasAttribute", productToReturn.getAttributeSet() != null);
      if (foundByLot) {
        jsonProduct.put("lot", code);
      } else if (foundByProductUPC) {
        jsonProduct.put("lot", arguments.getJSONObject("item").get("attributeSetValueName"));
        jsonProduct.put("itemId", arguments.getJSONObject("item").get("itemId"));
      }
      JSONObject data = new JSONObject();
      data.put("product", jsonProduct);
      response.put("data", data);
    } else {
      response.put("status", 1);
      JSONObject jsonError = new JSONObject();
      jsonError.put("message", errorMessage);
      response.put("error", jsonError);
    }

    return response;
  }
}
