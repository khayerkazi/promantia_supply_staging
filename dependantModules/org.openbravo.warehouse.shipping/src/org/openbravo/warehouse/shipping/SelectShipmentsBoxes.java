/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.warehouse.shipping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.json.JsonUtils;

public class SelectShipmentsBoxes extends BaseProcessActionHandler {
  private static Logger log = Logger.getLogger(SelectShipmentsBoxes.class);

  @Override
  /**
   * Main method of the SelectShipmentsBoxes Class It creates Shipping Details for the Selected
   * records or removes the ones that were created but are no longer selected.
   */
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      final String strShippingId = jsonRequest.getString("Obwship_Shipping_ID");
      OBWSHIPShipping shipping = OBDal.getInstance().get(OBWSHIPShipping.class, strShippingId);
      final String strBParnterID = jsonRequest.getString("inpcBpartnerId");
      BusinessPartner bpartner = null;
      if (!"".equals(strBParnterID) && strBParnterID != null) {
        bpartner = OBDal.getInstance().get(BusinessPartner.class, strBParnterID);
      }
      final String strWarehouseID = jsonRequest.getString("inpmWarehouseId");
      Warehouse warehouse = null;
      if (!"".equals(strWarehouseID) && strWarehouseID != null) {
        warehouse = OBDal.getInstance().get(Warehouse.class, strWarehouseID);
      }

      List<String> idList = OBDao.getIDListFromOBObject(shipping.getOBWSHIPShippingDetailsList());
      // The Selected Shipping Details are going to be created
      // If there is any error, it will be included in the returned HashMap

      boolean isSriLankaRecord = isSrilankaInvoice(bpartner);
      if (isSriLankaRecord) {
        shipping = setExchangeRate(shipping);
      } else {
        shipping.setExchangerate(BigDecimal.ONE);
        OBDal.getInstance().save(shipping);
      }

      HashMap<String, String> map = createShippingDetails(jsonRequest, bpartner, idList, shipping,
          warehouse, isSriLankaRecord);
      jsonRequest = new JSONObject();

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      if (map.get("DifferentBParner").equals("true")) {
        errorMessage.put("severity", "warning");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWSHIP_Different_BPartners_Selected"));
      }
      if (map.get("DifferentWarehouse").equals("true")) {
        errorMessage.put("severity", "warning");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWSHIP_Different_Warehouse_Selected"));
      }
      jsonRequest.put("message", errorMessage);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);

      try {
        jsonRequest = new JSONObject();

        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD(e.getMessage()));
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  /**
   * Shipping Details are going to be created for the selected records in the Window
   * 
   * @param jsonRequest
   * @param bpartner
   *          Selected Business Partner of the Shipping. Can be null
   * @param idList
   *          Already existing details for the current Shipping
   * @param shipping
   *          Current Shipping
   * @param isSriLankaRecord
   * @return HashMap with the number of affected records and a boolean to know if any Details belong
   *         to a different Business Partner than the one selected in the Shipping.
   * @throws JSONException
   * @throws OBException
   */
  private HashMap<String, String> createShippingDetails(JSONObject jsonRequest,
      BusinessPartner bpartner, List<String> idList, OBWSHIPShipping shipping, Warehouse warehouse,
      boolean isSriLankaRecord) throws JSONException, OBException {

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("DifferentBParner", "false");
    map.put("DifferentWarehouse", "false");
    map.put("Count", "0");

    JSONObject params = jsonRequest.getJSONObject("_params");
    JSONObject myGridItem = params.getJSONObject("grid");
    JSONArray selectedLines = myGridItem.getJSONArray("_selection");

    // if no lines selected remove existing Details records
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, shipping);
      return map;
    }

    int cont = 0;
    String differentBPartner = "false";
    String differentWarehouse = "false";
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject((int) i);
      log.debug(selectedLine);

      // Check if the Business Partner is the same as in the Shipping Header
      String strLineBpartnerID = selectedLine.getString("businessPartner");
      BusinessPartner lineBpartner = OBDal.getInstance().get(BusinessPartner.class,
          strLineBpartnerID);
      if (bpartner != null && !bpartner.equals(lineBpartner)) {
        differentBPartner = "true";
      }

      // Check if the Warehouse is the same as in the Shipping Header
      String strLineWarehouseID = selectedLine.getString("warehouse");
      Warehouse lineWarehouse = OBDal.getInstance().get(Warehouse.class, strLineWarehouseID);
      if (warehouse != null && !warehouse.equals(lineWarehouse)) {
        differentWarehouse = "true";
      }

      // Create the new Shipping Detail
      OBWSHIPShippingDetails newShippingDetail = null;
      String strShippingDetails = selectedLine.getString("obwshipShippingDetails");
      boolean notExistsDetails = idList.contains(strShippingDetails);
      if (notExistsDetails) {
        newShippingDetail = OBDal.getInstance().get(OBWSHIPShippingDetails.class,
            strShippingDetails);
        // Don't delete this Detail record, it is selected
        idList.remove(strShippingDetails);
      } else {
        newShippingDetail = OBProvider.getInstance().get(OBWSHIPShippingDetails.class);
      }
      newShippingDetail.setOrganization(shipping.getOrganization());
      newShippingDetail.setClient(shipping.getClient());
      newShippingDetail.setCreatedBy(shipping.getCreatedBy());
      newShippingDetail.setUpdatedBy(shipping.getUpdatedBy());
      newShippingDetail.setObwshipShipping(shipping);
      String strInoutID = selectedLine.getString("goodsShipment");
      ShipmentInOut inout = null;
      if (strInoutID != null && !"".equals(strInoutID)) {
        inout = OBDal.getInstance().get(ShipmentInOut.class, strInoutID);
      }
      newShippingDetail.setGoodsShipment(inout);
      OBDal.getInstance().save(newShippingDetail);
      OBDal.getInstance().save(shipping);
      OBDal.getInstance().flush();
      cont++;

      List<ShipmentInOutLine> inoutLineList = inout.getMaterialMgmtShipmentInOutLineList();
      /*
       * Map<Product, BigDecimal> mapObj = new HashMap<Product, BigDecimal>(); for
       * (ShipmentInOutLine inoutLine : inoutLineList) { mapObj.put(inoutLine.getProduct(),
       * getCessionPrice(inoutLine.getProduct())); }
       */
      for (ShipmentInOutLine inoutLine : inoutLineList) {
        // if (mapObj.containsKey(inoutLine.getProduct())) {

        BigDecimal movementQty = inoutLine.getMovementQuantity();

        // ----- Set Cession Price
        BigDecimal cessionPrice = getCessionPrice(inoutLine.getProduct()).divide(
            shipping.getExchangerate(), 2, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);

        inoutLine.setOBWSHIPCessionPrice(cessionPrice);
        // Set Cession Price completed -----
        if (inoutLine.getProduct() != null) {
          if (inoutLine.getProduct().getIngstGstproductcode() != null) {
            if (inoutLine.getProduct().getIngstGstproductcode().getValue() != null) {
              inoutLine.setObwshipHsncode(inoutLine.getProduct().getIngstGstproductcode()
                  .getValue());
            }
          }
        }
        // ----- Set Taxable Amount
        BigDecimal taxableAmt = cessionPrice.multiply(movementQty);
        inoutLine.setObwshipTaxableamount(taxableAmt);
        // Set Taxable Amount completed -----

        String bpName = "";
        String bpGSTIN = "";
        String ShipmentGSTIN = "";
        BigDecimal taxRate = BigDecimal.ZERO;

        if (!isSriLankaRecord) {
          bpName = newShippingDetail.getObwshipShipping().getBusinessPartner().getName();
          OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(
              Organization.class);
          orgCriteria.add(Restrictions.eq(Organization.PROPERTY_NAME, bpName));
          if (orgCriteria.list().size() <= 0) {
            throw new OBException("Organization is not found for " + bpName
                + " Businee Partner in Shipping Header");
          } else {
            if (orgCriteria.list().get(0).getOrganizationInformationList().size() > 0) {
              OrganizationInformation orgInfo = orgCriteria.list().get(0)
                  .getOrganizationInformationList().get(0);
              if (orgInfo.getIngstGstidentifirmaster() == null) {
                throw new OBException("GSTIN is not configured for " + bpName
                    + " Organization/BusinessPartner");
              } else {
                bpGSTIN = orgInfo.getIngstGstidentifirmaster().getUidno();

                ShipmentGSTIN = newShippingDetail.getGoodsShipment().getWarehouse().getGsGstin();

                // ----- Set Tax Rate
                if (!bpGSTIN.equalsIgnoreCase(ShipmentGSTIN)) {
                  if (inoutLine.getSalesOrderLine() != null) {
                    OrderLine orderlineObj = inoutLine.getSalesOrderLine();
                    if (orderlineObj.getOrderLineTaxList().size() > 0) {
                      if (orderlineObj.getOrderLineTaxList().get(0).getTax() != null) {
                        taxRate = orderlineObj.getOrderLineTaxList().get(0).getTax().getRate();
                      }
                    }
                  }
                }
              }
            }
          }

        }// Set Tax Rate Completed -----
        inoutLine.setObwshipTaxrate(taxRate);

        // ----- Set Tax Amount
        BigDecimal expression1 = taxRate.divide(new BigDecimal(100));
        BigDecimal taxAmount = taxableAmt.multiply(expression1);
        inoutLine.setObwshipTaxamount((taxAmount).setScale(2, RoundingMode.HALF_UP));
        // Set Tax Amount Completed -----

        // }
        OBDal.getInstance().save(inoutLine);
        // OBDal.getInstance().flush();
      }

    }

    // Remove non selected Details
    removeNonSelectedLines(idList, shipping);
    map.put("DifferentBParner", differentBPartner);
    map.put("DifferentWarehouse", differentWarehouse);
    map.put("Count", Integer.toString(cont));
    return map;
  }

  private BigDecimal getCessionPrice(Product product) {
    OBCriteria<PriceListVersion> priceListVersionCriteria = OBDal.getInstance().createCriteria(
        PriceListVersion.class);
    priceListVersionCriteria.add(Restrictions.eq(PriceList.PROPERTY_NAME, "DMI CATALOGUE"));
    priceListVersionCriteria.list();

    if (priceListVersionCriteria.list().size() < 0) {
      throw new OBException("PriceList DMI CATALOGUE is not available in erp");
    }

    OBCriteria<ProductPrice> productPriceCriteria = OBDal.getInstance().createCriteria(
        ProductPrice.class);
    productPriceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, product));
    productPriceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION,
        priceListVersionCriteria.list().get(0)));

    if (productPriceCriteria.list().size() < 0) {
      throw new OBException(
          "ProductPrice for priceListVersion as DMI CATALOGUE is not present for product: "
              + product.getName());
    } else
      return productPriceCriteria.list().get(0).getClCessionprice()
          .setScale(2, RoundingMode.HALF_UP);

  }

  /**
   * Removes the Details that were created previously but now are no longer selected.
   * 
   * @param idList
   *          List of Details to remove.
   * @param shipping
   *          Shipping from which the Details are going to be removed.
   */
  private void removeNonSelectedLines(List<String> idList, OBWSHIPShipping shipping) {
    if (idList.size() > 0) {
      for (String id : idList) {
        OBWSHIPShippingDetails shippingDetail = OBDal.getInstance().get(
            OBWSHIPShippingDetails.class, id);
        shipping.getOBWSHIPShippingDetailsList().remove(shippingDetail);
        OBDal.getInstance().remove(shippingDetail);
      }
      OBDal.getInstance().save(shipping);
      OBDal.getInstance().flush();
    }
  }

  private OBWSHIPShipping setExchangeRate(OBWSHIPShipping shipping) throws Exception {
    // Date date, Currency fromCurrency,
    // Currency toCurrency, Organization org, Client client
    Date shippingDate;
    try {
      shippingDate = JsonUtils.createDateFormat().parse(shipping.getShipmentDate().toString());
      Organization orgObj = OBDal.getInstance().get(Organization.class, "0");

      ConversionRate conversionRate = FinancialUtils.getConversionRate(shippingDate,
          getCurrency("INR"), getCurrency("USD"), orgObj, OBContext.getOBContext()
              .getCurrentClient());
      if (conversionRate != null) {
        shipping.setExchangerate(conversionRate.getDivideRateBy());
        shipping.setSlshippinginvoicereport(true);
        OBDal.getInstance().save(shipping);
      } else {
        throw new Exception("Conversion Rate is not Config for Sri Lanka of date: " + shippingDate);
      }
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      throw new Exception(e);
    }
    return shipping;
  }

  private Currency getCurrency(String currencyCode) {
    OBCriteria<Currency> orgCriteria = OBDal.getInstance().createCriteria(Currency.class);
    orgCriteria.add(Restrictions.eq(Currency.PROPERTY_ISOCODE, currencyCode));
    Currency Currency = null;
    if (orgCriteria.list().size() > 0) {
      Currency = orgCriteria.list().get(0);
    }
    return Currency;
  }

  public static boolean isSrilankaInvoice(BusinessPartner bpartner) {
    boolean isSriLankaRecord = false;
    if (bpartner != null) {
      if (bpartner.getBusinessPartnerLocationList().size() > 0) {
        if (bpartner.getBusinessPartnerLocationList().get(0).getLocationAddress() != null) {
          if (bpartner.getBusinessPartnerLocationList().get(0).getLocationAddress().getCountry() != null) {
            if (!bpartner.getBusinessPartnerLocationList().get(0).getLocationAddress().getCountry()
                .getName().equals("India")) {
              isSriLankaRecord = true;
            }
          }
        }
      }
    }
    return isSriLankaRecord;
  }

}
