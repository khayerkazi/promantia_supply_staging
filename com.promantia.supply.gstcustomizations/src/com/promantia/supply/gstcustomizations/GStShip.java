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

package com.promantia.supply.gstcustomizations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;
import org.openbravo.warehouse.shipping.OBWSHIPShippingDetails;
import org.openbravo.warehouse.shipping.ShipShippingHook;

public class GStShip extends BaseProcessActionHandler {

  private static Logger log = Logger.getLogger(GStShip.class);

  @Inject
  @Any
  private Instance<ShipShippingHook> shipShippingHooks;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      log.debug(jsonRequest);
      jsonRequest = new JSONObject(content);

      final String strShippingId = jsonRequest.getString("Obwship_Shipping_ID");
      OBWSHIPShipping shipping = OBDal.getInstance().get(OBWSHIPShipping.class, strShippingId);

      // Call the process Method. If there is any error, there will be an entry on the HashMap to
      // detail it. If not the HashMap will be empty
      log.info("Processed Ship Id: " + shipping);
      HashMap<String, List<String>> map = shipShipping(shipping);
      jsonRequest = new JSONObject();
      log.info("Processed Ship success ");
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      // Check possible errors
      if (map.containsKey("NoShippingDetails")) {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWSHIP_No_Details"));
      } else if (map.containsKey("ShippingAlreadyCompleted")) {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWSHIP_Shipping_Completed"));
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

  private HashMap<String, List<String>> shipShipping(OBWSHIPShipping shipping) throws Exception {
    HashMap<String, List<String>> result = new HashMap<String, List<String>>();

    try {

      // If the Document is already complete, revert it to Draft Status
      if (isCompleted(shipping)) {
        log.info("is Completed Ship:");
        result.put("ShippingAlreadyCompleted", null);
        return result;
      }

      // Raise an error if the Shipping has no details
      if (hasNoDetails(shipping)) {
        log.info("has No ship Details:");

        result.put("NoShippingDetails", null);
        return result;
      }

      // If there has been no problems, set the Status as Shipped
      generateGstInvoiceNumber(shipping);

      shipping.setDocumentStatus("SHIP");
      shipping.setShipped(true);
      OBDal.getInstance().save(shipping);
      OBDal.getInstance().flush();

      log.info("hook is executing");
      executeHooks(shipping);
      log.info("hook is execute Successfully");
    } catch (Exception e) {
      result.put("ErrorWhileProcessing", null);
      log.error("An error happened when shipShipping was executed: " + e.getMessage(), e);
      throw new Exception("Error happened when shipShipping was executed: " + e);
    }
    return result;
  }

  // Returns true if the Shipping is in Completed Status
  private boolean isCompleted(OBWSHIPShipping shipping) {
    return shipping.getDocumentStatus().equals("CO");
  }

  private boolean hasNoDetails(OBWSHIPShipping shipping) {
    return shipping.getOBWSHIPShippingDetailsList().size() == 0;
  }

  private void executeHooks(OBWSHIPShipping shipping) throws Exception {
    for (ShipShippingHook hook : shipShippingHooks) {
      log.info("hook calling:");

      hook.exec(shipping);
    }
  }

  private void generateGstInvoiceNumber(OBWSHIPShipping shipping) {
    try {
      if (shipping.getBusinessPartner().getBusinessPartnerLocationList().get(0)
          .getLocationAddress().getCountry().getName().equalsIgnoreCase("India")) {
        String bpGstinNumber = getBusinessPartnerGstinNumber(shipping);
        log.info("bp Gstin Number : " + bpGstinNumber);

        String warehouseGstinNumber = getWarehouseGstinNumber(shipping);
        log.info("warehouse Gstin Number : " + warehouseGstinNumber);

        if (warehouseGstinNumber.equals(bpGstinNumber)) {
          String intraUniqueInvoiceNumber = getIntraWarehouseDocumentSequence(shipping);
          shipping.setGsUniqueno(intraUniqueInvoiceNumber);
        } else {
          String uniqueInvoiceNumber = getWarehouseDocumentSequence(shipping);
          shipping.setGsUniqueno(uniqueInvoiceNumber);
        }

      } else {
        /*
         * String intraUniqueInvoiceNumber = getIntraWarehouseDocumentSequence(shipping);
         * shipping.setGsUniqueno(intraUniqueInvoiceNumber);
         */
        String packingInvoiceNumber = getPackingDocumentSequence(shipping);
        // shipping.setPackinginvoiceno(packingInvoiceNumber);
        shipping.setGsUniqueno(packingInvoiceNumber);

      }
    } catch (Exception e) {
      throw new OBException(" Generating Document Sequence Number and error is: " + e);
    }
  }

  private String getPackingDocumentSequence(OBWSHIPShipping shipping) {
    OBWSHIPShippingDetails shipWarehouse = getWarehouse(shipping);
    if (shipWarehouse.getGoodsShipment().getWarehouse().getObwshipPackingseqno() != null) {
      String warehousePrefix = shipWarehouse.getGoodsShipment().getWarehouse()
          .getObwshipPackingseqno().getPrefix();
      Long warehouseNumber = shipWarehouse.getGoodsShipment().getWarehouse()
          .getObwshipPackingseqno().getNextAssignedNumber();
      String uniqueNumber = warehousePrefix.concat(String.valueOf(warehouseNumber));
      Long newDocNumber = warehouseNumber + 1;
      shipWarehouse.getGoodsShipment().getWarehouse().getObwshipPackingseqno()
          .setNextAssignedNumber(newDocNumber);
      return uniqueNumber;
    } else {
      throw new OBException(" Packing Document Sequence is not config for "
          + shipWarehouse.getGoodsShipment().getWarehouse().getEntityName() + " Warehouse");
    }
  }

  private String getWarehouseDocumentSequence(OBWSHIPShipping shipping) {
    OBWSHIPShippingDetails shipWarehouse = getWarehouse(shipping);
    if (shipWarehouse.getGoodsShipment().getWarehouse().getGsSequence() != null) {
      String warehousePrefix = shipWarehouse.getGoodsShipment().getWarehouse().getGsSequence()
          .getPrefix();
      Long warehouseNumber = shipWarehouse.getGoodsShipment().getWarehouse().getGsSequence()
          .getNextAssignedNumber();
      Long newDocNumber = warehouseNumber + 1;
      String uniqueNumber = warehousePrefix.concat(String.valueOf(newDocNumber));
      shipWarehouse.getGoodsShipment().getWarehouse().getGsSequence()
          .setNextAssignedNumber(newDocNumber);
      return uniqueNumber;
    } else {
      throw new OBException(" Document Sequence is not config for "
          + shipWarehouse.getGoodsShipment().getWarehouse().getEntityName() + " Warehouse");
    }
  }

  private String getIntraWarehouseDocumentSequence(OBWSHIPShipping shipping) {
    OBWSHIPShippingDetails shipWarehouse = getWarehouse(shipping);
    if (shipWarehouse.getGoodsShipment().getWarehouse().getGsIntrasequence() != null) {
      String intraWarehousePrefix = shipWarehouse.getGoodsShipment().getWarehouse()
          .getGsIntrasequence().getPrefix();
      Long intraWarehouseNumber = shipWarehouse.getGoodsShipment().getWarehouse()
          .getGsIntrasequence().getNextAssignedNumber();
      Long intraNewDocNumber = intraWarehouseNumber + 1;
      String intraUniqueNumber = intraWarehousePrefix.concat(String.valueOf(intraNewDocNumber));
      shipWarehouse.getGoodsShipment().getWarehouse().getGsIntrasequence()
          .setNextAssignedNumber(intraNewDocNumber);
      return intraUniqueNumber;
    } else {
      throw new OBException(" Inter Warehouse Document Sequence is not config for "
          + shipWarehouse.getGoodsShipment().getWarehouse().getEntityName() + " Warehouse");
    }
  }

  private String getBusinessPartnerGstinNumber(OBWSHIPShipping shipping) {
    String bpName = shipping.getBusinessPartner().getName();
    Organization orgName = getBpInOrganization(bpName);
    OrganizationInformation orgGstin = getOrgGstinNumber(orgName);
    String gstNumber = orgGstin.getIngstGstidentifirmaster().getUidno();
    return gstNumber;
  }

  private String getWarehouseGstinNumber(OBWSHIPShipping shipping) {
    OBWSHIPShippingDetails shipmentWarehouse = getWarehouse(shipping);
    String warehouseGstin = shipmentWarehouse.getGoodsShipment().getWarehouse().getGsGstin();
    return warehouseGstin;
  }

  private Organization getBpInOrganization(String bpName) {
    String qry = " name ='" + bpName + "'";
    OBQuery<Organization> orgNameQry = OBDal.getInstance().createQuery(Organization.class, qry);
    orgNameQry.setFilterOnActive(false);
    orgNameQry.setFilterOnReadableOrganization(false);

    if (orgNameQry.uniqueResult() != null)
      return orgNameQry.uniqueResult();
    else
      throw new OBException("Organization Name Not Present for orgName " + bpName);
  }

  private OrganizationInformation getOrgGstinNumber(Organization orgName) {
    String qry = " id ='" + orgName.getId() + "'";
    OBQuery<OrganizationInformation> orgNameQry = OBDal.getInstance().createQuery(
        OrganizationInformation.class, qry);
    orgNameQry.setFilterOnActive(false);
    orgNameQry.setFilterOnReadableOrganization(false);

    if (orgNameQry.uniqueResult() != null)
      return orgNameQry.uniqueResult();
    else
      throw new OBException("OrganizationInfor Not Present for orgName " + orgName);
  }

  private OBWSHIPShippingDetails getWarehouse(OBWSHIPShipping shipping) {
    String qry = " obwshipShipping.id ='" + shipping.getId() + "'";
    OBQuery<OBWSHIPShippingDetails> shipmentsQry = OBDal.getInstance().createQuery(
        OBWSHIPShippingDetails.class, qry);
    shipmentsQry.setFilterOnActive(false);
    shipmentsQry.setFilterOnReadableOrganization(false);
    List shiplineList = shipmentsQry.list();
    if (shiplineList != null & shiplineList.size() > 0) {
      return shipmentsQry.list().get(0);
    } else {
      throw new OBException("No shipment lines associated");
    }
  }

}
