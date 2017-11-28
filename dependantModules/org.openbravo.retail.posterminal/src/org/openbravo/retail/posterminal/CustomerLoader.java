/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.posterminal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.process.DataSynchronizationProcess.DataSynchronization;
import org.openbravo.mobile.core.process.JSONPropertyToEntity;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.service.json.JsonConstants;

@DataSynchronization(entity = "BusinessPartner")
public class CustomerLoader extends POSDataSynchronizationProcess {

  private static final Logger log = Logger.getLogger(CustomerLoader.class);

  private static final BigDecimal NEGATIVE_ONE = new BigDecimal(-1);

  @Inject
  @Any
  private Instance<CustomerLoaderHook> customerCreations;

  public JSONObject saveRecord(JSONObject jsoncustomer) throws Exception {
    BusinessPartner customer = null;
    OBContext.setAdminMode(false);
    try {
      customer = getCustomer(jsoncustomer.getString("id"));
      if (customer.getId() == null) {
        customer = createBPartner(jsoncustomer);
      } else {
        customer = editBPartner(customer, jsoncustomer);
      }

      // Call all customerCreations injected.
      executeHooks(customerCreations, jsoncustomer, customer);

      editLocation(customer, jsoncustomer);
      editBPartnerContact(customer, jsoncustomer);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
    final JSONObject jsonResponse = new JSONObject();
    jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    jsonResponse.put("result", "0");

    return jsonResponse;
  }

  protected BusinessPartner getCustomer(String id) {
    BusinessPartner customer = OBDal.getInstance().get(BusinessPartner.class, id);
    if (customer != null) {
      return customer;
    }
    return new BusinessPartner();
  }

  protected BusinessPartner createBPartner(JSONObject jsonCustomer) throws JSONException {
    BusinessPartner customer = OBProvider.getInstance().get(BusinessPartner.class);
    Entity BusinessPartnerEntity = ModelProvider.getInstance().getEntity(BusinessPartner.class);
    JSONPropertyToEntity.fillBobFromJSON(BusinessPartnerEntity, customer, jsonCustomer);

    // customer.setClient(OBDal.getInstance().get(Client.class, jsonCustomer.getString("client")));
    // BP org (required)
    if (!jsonCustomer.has("organization") && jsonCustomer.getString("organization").equals("null")) {
      String errorMessage = "Business partner organization is a mandatory field to create a new customer from Web Pos";
      log.error(errorMessage);
      throw new OBException(errorMessage, null);
    }
    // BP id (required)
    if (jsonCustomer.has("id") && !jsonCustomer.getString("id").equals("null")) {
      customer.setId(jsonCustomer.getString("id"));
    } else {
      String errorMessage = "Business partner id is a mandatory field to create a new customer from Web Pos";
      log.error(errorMessage);
      throw new OBException(errorMessage, null);
    }
    // BP category (required)
    if (!jsonCustomer.has("businessPartnerCategory")
        && jsonCustomer.getString("businessPartnerCategory").equals("null")) {
      String errorMessage = "Business partner category is a mandatory field to create a new customer from Web Pos";
      log.error(errorMessage);
      throw new OBException(errorMessage, null);
    }
    // BP search key (required)
    if (!jsonCustomer.has("searchKey") && jsonCustomer.getString("searchKey").equals("null")) {
      String errorMessage = "Business partner search key is a mandatory field to create a new customer from Web Pos";
      log.error(errorMessage);
      throw new OBException(errorMessage, null);
    } else {
      String possibleSK = jsonCustomer.getString("searchKey");
      String finalSK = "";

      int bpsWithPossibleSK = 0;

      final OBCriteria<BusinessPartner> bpCriteria = OBDal.getInstance().createCriteria(
          BusinessPartner.class);
      bpCriteria.setFilterOnActive(false);
      bpCriteria.setFilterOnReadableOrganization(false);
      bpCriteria.add(Restrictions.eq("searchKey", possibleSK));
      bpCriteria.setMaxResults(1);
      bpsWithPossibleSK = bpCriteria.count();

      if (bpsWithPossibleSK > 0) {
        // SK exist -> make it unique
        finalSK = possibleSK + "_" + jsonCustomer.getString("id").substring(0, 4);
      } else {
        // we can use this SK
        finalSK = possibleSK;
      }
      customer.setSearchKey(finalSK);
    }
    // BP name (required)
    if (!jsonCustomer.has("name") && jsonCustomer.getString("name").equals("null")) {
      String errorMessage = "Business partner name is a mandatory field to create a new customer from Web Pos";
      log.error(errorMessage);
      throw new OBException(errorMessage, null);
    }

    // customer tab
    customer.setCustomer(true);
    customer.setCreditLimit(BigDecimal.ZERO);

    customer.setNewOBObject(true);
    OBDal.getInstance().save(customer);
    return customer;
  }

  protected BusinessPartner editBPartner(BusinessPartner customer, JSONObject jsonCustomer)
      throws JSONException {
    String previousSK = customer.getSearchKey();
    BigDecimal previousCL = customer.getCreditLimit();
    Entity BusinessPartnerEntity = ModelProvider.getInstance().getEntity(BusinessPartner.class);
    JSONPropertyToEntity.fillBobFromJSON(BusinessPartnerEntity, customer, jsonCustomer);

    // Don't change SK when BP is modified
    customer.setSearchKey(previousSK);
    // customer tab
    customer.setCustomer(true);
    // security
    customer.setCreditLimit(previousCL);

    OBDal.getInstance().save(customer);
    return customer;
  }

  protected void editBPartnerContact(BusinessPartner customer, JSONObject jsonCustomer)
      throws JSONException {
    Entity userEntity = ModelProvider.getInstance().getEntity(
        org.openbravo.model.ad.access.User.class);
    final org.openbravo.model.ad.access.User user = OBDal.getInstance().get(
        org.openbravo.model.ad.access.User.class, jsonCustomer.getString("contactId"));
    if (user != null) {

      JSONPropertyToEntity.fillBobFromJSON(userEntity, user, jsonCustomer);
      String name = jsonCustomer.getString("name");
      if (name.length() > 60) {
        name = name.substring(0, 60);
      }
      user.setFirstName(name);

      // Contact exist > modify it. The username is not modified
      OBDal.getInstance().save(user);
    } else {
      // Contact doesn't exists > create it - create user linked to BP

      // First: Check if the proposed username exists
      String name = jsonCustomer.getString("name");
      String possibleUsername = name.trim();
      String finalUsername = "";

      int usersWithPossibleUsername = 0;

      final OBCriteria<org.openbravo.model.ad.access.User> userCriteria = OBDal.getInstance()
          .createCriteria(org.openbravo.model.ad.access.User.class);
      userCriteria.add(Restrictions.eq("username", possibleUsername));
      userCriteria.setMaxResults(1);
      usersWithPossibleUsername = userCriteria.count();

      if (usersWithPossibleUsername > 0) {
        // username exist -> make it unique
        finalUsername = possibleUsername + "_"
            + jsonCustomer.getString("contactId").substring(0, 4);
      } else {
        // we can use this username
        finalUsername = possibleUsername;
      }

      // create the user

      final org.openbravo.model.ad.access.User usr = OBProvider.getInstance().get(
          org.openbravo.model.ad.access.User.class);

      JSONPropertyToEntity.fillBobFromJSON(userEntity, usr, jsonCustomer);

      if (jsonCustomer.has("contactId")) {
        usr.setId(jsonCustomer.getString("contactId"));
      } else {
        String errorMessage = "Business partner user ID is a mandatory field to create a new customer from Web Pos";
        log.error(errorMessage);
        throw new OBException(errorMessage, null);
      }

      usr.setUsername(finalUsername);
      if (name.length() > 60) {
        name = name.substring(0, 60);
      }
      usr.setFirstName(name);

      usr.setBusinessPartner(customer);

      usr.setNewOBObject(true);

      OBDal.getInstance().save(usr);
    }
  }

  protected void editLocation(BusinessPartner customer, JSONObject jsonCustomer)
      throws JSONException {
    Entity locationEntity = ModelProvider.getInstance().getEntity(Location.class);
    Entity baseLocationEntity = ModelProvider.getInstance().getEntity(
        org.openbravo.model.common.geography.Location.class);
    final Location location = OBDal.getInstance().get(Location.class,
        jsonCustomer.getString("locId"));
    if (location != null) {
      // location exist > modify it
      final org.openbravo.model.common.geography.Location rootLocation = location
          .getLocationAddress();

      JSONPropertyToEntity.fillBobFromJSON(baseLocationEntity, rootLocation, jsonCustomer);

      if (jsonCustomer.has("locName") && jsonCustomer.getString("locName") != null
          && !jsonCustomer.getString("locName").equals("")) {
        rootLocation.setAddressLine1(jsonCustomer.getString("locName"));
      }

      OBDal.getInstance().save(rootLocation);
    } else {
      // location not exists > create location and bplocation
      final org.openbravo.model.common.geography.Location rootLocation = OBProvider.getInstance()
          .get(org.openbravo.model.common.geography.Location.class);

      JSONPropertyToEntity.fillBobFromJSON(baseLocationEntity, rootLocation, jsonCustomer);

      if (jsonCustomer.has("locName") && jsonCustomer.getString("locName") != null
          && !jsonCustomer.getString("locName").equals("")) {
        rootLocation.setAddressLine1(jsonCustomer.getString("locName"));
      }

      OBDal.getInstance().save(rootLocation);

      Location newLocation = OBProvider.getInstance().get(Location.class);

      JSONPropertyToEntity.fillBobFromJSON(locationEntity, newLocation, jsonCustomer);

      if (jsonCustomer.has("locId")) {
        newLocation.setId(jsonCustomer.getString("locId"));
      } else {
        String errorMessage = "Business partner Location ID is a mandatory field to create a new customer from Web Pos";
        log.error(errorMessage);
        throw new OBException(errorMessage, null);
      }
      if (jsonCustomer.has("locName") && jsonCustomer.getString("locName") != null
          && !jsonCustomer.getString("locName").equals("")) {
        newLocation.setName(jsonCustomer.getString("locName"));
      } else {
        newLocation.setName(jsonCustomer.getString("searchKey"));
      }

      // don't set phone of location, the phone is set in contact
      newLocation.setPhone(null);

      newLocation.setBusinessPartner(customer);
      newLocation.setLocationAddress(rootLocation);
      newLocation.setNewOBObject(true);
      OBDal.getInstance().save(newLocation);
    }
  }

  public static String getErrorMessage(Exception e) {
    StringWriter sb = new StringWriter();
    e.printStackTrace(new PrintWriter(sb));
    return sb.toString();
  }

  @Override
  protected String getProperty() {
    return "OBPOS_receipt.customers";
  }

  private void executeHooks(Instance<CustomerLoaderHook> hooks, JSONObject jsonCustomer,
      BusinessPartner customer) throws Exception {
    for (Iterator<CustomerLoaderHook> procIter = hooks.iterator(); procIter.hasNext();) {
      CustomerLoaderHook proc = procIter.next();
      proc.exec(jsonCustomer, customer);
    }
  }
}
