/*
 ************************************************************************************
 * Copyright (C) 2016-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator.registeruser;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.FormAccess;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.ui.Form;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.retail.discounts.discountmatrixmanagement.data.RoleDiscounts;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.utils.FormatUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.decathlon.oagis942.AmountType;
import com.decathlon.oagis942.CodeType;
import com.decathlon.oagis942.CodesType;
import com.decathlon.oagis942.IdentifierType;
import com.decathlon.oagis942.ObjectFactory;
import com.decathlon.oagis942.OxAmountsList;
import com.decathlon.oagis942.PartyIDs;
import com.decathlon.oagis942.Personnel;
import com.decathlon.oagis942.SyncPersonnelType;
import com.decathlon.oagis942.TypedAmount;
import com.decathlon.oagis942.unqualifieddatatypes.v1_1.NameType;
import com.openbravo.decathlon.operator.DECOPEErrors;
import com.openbravo.decathlon.operator.utils.DECOPEUtils;
import com.openbravo.decathlon.operator.utils.JAXBContextSingleton;

/**
 * Class for processing the Operator Register XML for adding operators
 * 
 */
public class OBOperatorRegister {

  private JAXBContext jaxbContext;
  private static final Logger logger = LoggerFactory.getLogger(OBOperatorRegister.class);
  private static final BigDecimal ONEHUNDRED = new BigDecimal(100);

  /**
   * Method that processes the xml of the Operator Register
   * 
   * @param errorId
   *          Error Id if reprocessing a previous error, null otherwise.
   * @return {@link JSONObject} with the message
   */

  public JSONObject processOperatorRegister(String errorId) {
    int errorCount = 0;
    String returnError = null;
    Map<String, String> errorMessages = new HashMap<String, String>();
    DECOPEErrors error = OBDal.getInstance().get(DECOPEErrors.class, errorId);
    if (error == null) {
      return createResponse("");
    }

    try {
      StringReader reader = new StringReader(error.getXMLContent());
      jaxbContext = JAXBContextSingleton.getContext();
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      Object syncPersonnelType = jaxbUnmarshaller.unmarshal(reader);

      SyncPersonnelType syncPersonnel = JAXBContextSingleton.getIntrospector(syncPersonnelType);

      List<PartyIDs> partyIdsListTemp = syncPersonnel.getDataArea().getPersonnels().get(0)
          .getPartyIDs();
      String sellingLocation = "0070";
      String partynumber = "";
      for (PartyIDs partyIds : partyIdsListTemp) {
        List<IdentifierType> orgIdTypeList = partyIds.getIDS();
        for (IdentifierType orgIdType : orgIdTypeList) {
          if (orgIdType.getSchemeName().equals("partyNumber")) {
            partynumber = String.format("%04d", Integer.parseInt(orgIdType.getValue()));
          }
        }
      }
      sellingLocation += partynumber;
      Organization organization = DECOPEUtils.obtainOrganization(sellingLocation);
      String orgId = organization.getId();

      // Obtain the default password
      String password = organization.getClient().getDECOPEDefaultPasswd();
      String clientId = organization.getClient().getId();

      error.setStatus("processing");
      error.setErrorMessage(OBMessageUtils.messageBD("DECOPE_Request_Processing"));
      OBDal.getInstance().commitAndClose();

      List<Personnel> iteratingList = new ArrayList<Personnel>();
      iteratingList.addAll(syncPersonnel.getDataArea().getPersonnels());
      String userName = "";
      for (Personnel personnel : iteratingList) {
        try {
          BigDecimal rptValue = BigDecimal.ZERO;
          String operatorID = "";
          BigDecimal typeRole = BigDecimal.ZERO;
          NameType firstNameType = personnel.getGivenName();
          String firstName = firstNameType.getValue();
          NameType lastNameType = personnel.getFamilyName();
          String lastName = lastNameType.getValue();

          List<IdentifierType> idTypeList = personnel.getIDS();
          for (IdentifierType idType : idTypeList) {
            if (idType.getSchemeName().equals("cashierAccount")) {
              userName = idType.getValue();
            } else if (idType.getSchemeName().equals("cashierNumber")) {
              operatorID = idType.getValue();
            }
          }
          if (userName.equals("")) {
            throw new OBException(
                OBMessageUtils.parseTranslation(OBMessageUtils.messageBD("DECOPE_Blank_Username")));
          }

          User user = searchUser(userName, clientId);

          // Operator extension
          String typeOfAction = personnel.getStatus().getCode().getValue();
          boolean changePassword = checkIfChangePassword(
              personnel.getPasswordProperty().isRenewPasswordIndicator().booleanValue(),
              typeOfAction);
          if (typeOfAction.equals("1") || typeOfAction.equals("2") || typeOfAction.equals("3")
              || typeOfAction.equals("6")) {
            // Create or Update
            OxAmountsList oxAmountsList = personnel.getOxAmountsList();
            List<TypedAmount> typedAmount = oxAmountsList.getTypedAmounts();
            AmountType amountType = typedAmount.get(0).getAmount();
            rptValue = amountType.getValue();

            CodesType jobRestrictionCodes = personnel.getJobRestrictionCodes();
            List<CodeType> codes = jobRestrictionCodes.getCodes();
            for (CodeType code : codes) {
              if (code.getListAgencyName().equals("tpnet")) {
                typeRole = new BigDecimal(code.getValue());
              }
            }
            Role role = null;
            if (validateUser(user, error)) {
              createOrUpdateUser(user, firstName, lastName, userName, operatorID, password,
                  changePassword, orgId);
              if (typeRole.equals(new BigDecimal(6))) {
                role = searchCashier(sellingLocation);
                if (role == null) {
                  role = OBProvider.getInstance().get(Role.class);
                }
                role = updateCashier(sellingLocation, role);
                createOrUpdateRoleDiscount(role, rptValue);
              } else if (typeRole.equals(new BigDecimal(4)) || typeRole.equals(new BigDecimal(5))) {
                role = searchAdmin(sellingLocation);
                if (role == null) {
                  role = OBProvider.getInstance().get(Role.class);
                  role = updateManager(sellingLocation, role);
                }
              } else {
                operatorError(errorMessages, userName, "DECOPE_WrongValue_Tpnet");
                errorCount++;
              }
              if (role != null) {
                if (user != null) {
                  logger.warn("[OperatorFlow] Before createformAccess for user " + user.getId());
                }
                createFormAcess(role);
                createUserRoles(role, user);
                createOrgAccess(role, orgId);
              }
            }
            user.setOBPOSDefaultPOSRole(role);
            user.setDefaultRole(role);
          } else if (typeOfAction.equals("4") || typeOfAction.equals("5")) {
            if (user.isNewOBObject()) {
              throw new OBException(OBMessageUtils
                  .parseTranslation(OBMessageUtils.messageBD("DECOPE_User_Not_Exist")));
            }
            // Deactivate or Delete
            user.setActive(false);
          }
          OBDal.getInstance().commitAndClose();
          OBDal.getInstance().getSession().clear();
          syncPersonnel.getDataArea().getPersonnels().remove(personnel);
        } catch (OBException obe) {
          OBDal.getInstance().rollbackAndClose();
          operatorError(errorMessages, userName, obe.getMessage());
          errorCount++;
        } catch (Exception e) {
          OBDal.getInstance().rollbackAndClose();
          Throwable ex = DbUtility.getUnderlyingSQLException(e);
          String strMessage = OBMessageUtils
              .translateError(new DalConnectionProvider(false),
                  RequestContext.get().getVariablesSecureApp(),
                  OBContext.getOBContext().getLanguage().getLanguage(), ex.getMessage())
              .getMessage();
          logger.error(strMessage, e);
          operatorError(errorMessages, userName, strMessage);
          errorCount++;
        }
      }

      if (errorCount > 0) {
        returnError = processErrors(error, errorCount, errorMessages, syncPersonnel, new Date());
      } else {
        if (errorId != null) {
          error.setStatus("success");
          OBDal.getInstance().remove(error);
        }
      }

    } catch (Exception e) {
      updateError(error, new Date(), error.getXMLContent(), e.getMessage());
      returnError = e.getMessage();
    }
    return createResponse(returnError);
  }

  /**
   * Search the warehouse
   * 
   * @param orgId
   * @return warehouse
   */
  private Warehouse searchWarehouse(Organization org) {
    OBCriteria<OrgWarehouse> warehouseCriteria = OBDal.getInstance()
        .createCriteria(OrgWarehouse.class);
    warehouseCriteria.add(Restrictions.eq(OrgWarehouse.PROPERTY_ORGANIZATION, org));
    warehouseCriteria.addOrderBy(OrgWarehouse.PROPERTY_PRIORITY, true);
    List<OrgWarehouse> lines = warehouseCriteria.list();
    Warehouse warehouse = lines.get(0).getWarehouse();
    return warehouse;
  }

  /**
   * Search a user using the userName
   * 
   * @param userName
   *          of the user
   * @param organization
   * @return the user which has the userName or a new user
   */
  private User searchUser(String userName, String clientId) {
    OBCriteria<User> userCriteria = OBDal.getInstance().createCriteria(User.class);
    userCriteria.setMaxResults(1);
    userCriteria.add(Restrictions.eq(User.PROPERTY_USERNAME, userName));
    userCriteria.setFilterOnReadableClients(false);
    userCriteria.setFilterOnActive(false);
    User user = (User) userCriteria.uniqueResult();
    if (user == null) {
      user = OBProvider.getInstance().get(User.class);
      user.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    } else {
      if (!user.getClient().getId().equals(clientId)) {
        throw new OBException(
            OBMessageUtils.parseTranslation(OBMessageUtils.messageBD("DECOPE_User_Exists")));
      }
    }
    return user;
  }

  /**
   * Check if a user should be created or updated
   * 
   * @param user
   *          to be checked
   * @param pendingItem
   *          to be checked
   * @return a boolean indicating if the user should be updated or created
   */
  private boolean validateUser(User user, DECOPEErrors pendingItem) {
    return (user.getUpdated() == null || user.getUpdated().before(pendingItem.getCreationDate()));
  }

  /**
   * Updates the information of the error
   * 
   * @param error
   *          to be updated
   * @param date
   *          of the update
   * @param xmlContent
   *          of the error
   * 
   * @param message
   *          error message
   */
  private void updateError(DECOPEErrors error, Date date, String xmlContent, String message) {
    error.setErrorMessage(message);
    error.setErrorDate(date);
    error.setXMLContent(xmlContent);
    error.setStatus("error");
    OBDal.getInstance().save(error);
  }

  private void operatorError(Map<String, String> errorMap, String itemId, String errorMessage) {
    String itemIdList = errorMap.get(errorMessage);
    if (itemIdList == null) {
      itemIdList = itemId;
    } else {
      itemIdList = itemIdList.concat(", ").concat(itemId);
    }
    errorMap.put(errorMessage, itemIdList);
  }

  /**
   * Creates or Updates a user
   * 
   * @param user
   *          to be created or updated
   * @param firstName
   *          of the user to be created or updated
   * @param lastName
   *          of the user to be created or updated
   * @param userName
   *          of the user to be created or updated
   * @param password
   *          of the user to be created or updated
   * @param changePassword
   *          flat to check if the password should be renewed
   * @return the created or updated user
   */
  private User createOrUpdateUser(User user, String firstName, String lastName, String userName,
      String operatorID, String password, boolean changePassword, String orgId) {
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setName(firstName + " " + lastName);
    user.setUsername(userName);
    user.setDecposlOperatorid(operatorID);
    user.setActive(true);
    Organization orga = OBDal.getInstance().getProxy(Organization.class, orgId);
    user.setDefaultOrganization(orga);
    user.setDefaultWarehouse(searchWarehouse(orga));
    user.setDefaultClient(orga.getClient());
    if (user.getCreationDate() == null || changePassword) {
      try {
        user.setPassword(FormatUtilities.sha1Base64(password));
      } catch (ServletException ignore) {
      }
      user.setPasswordExpired(true);
    }
    OBDal.getInstance().save(user);
    return user;
  }

  /**
   * Search a cashier role for an organization
   * 
   * @param org
   * @return The cashier role for that organization if exists
   */
  private Role searchCashier(String sellingLocation) {
    String roleName = sellingLocation + " - Cashier";
    OBCriteria<Role> roleCriteria = OBDal.getInstance().createCriteria(Role.class);
    roleCriteria.setMaxResults(1);
    roleCriteria.add(Restrictions.ilike(Role.PROPERTY_NAME, roleName));
    return (Role) roleCriteria.uniqueResult();
  }

  /**
   * Search a administrator role for an organization
   * 
   * @param org
   * @return The administrator role for that organization if it exists
   */
  private Role searchAdmin(String sellingLocation) {
    String roleName = sellingLocation + " - Manager";
    OBCriteria<Role> roleCriteria = OBDal.getInstance().createCriteria(Role.class);
    roleCriteria.setMaxResults(1);
    roleCriteria.add(Restrictions.ilike(Role.PROPERTY_NAME, roleName));
    return (Role) roleCriteria.uniqueResult();
  }

  /**
   * Update cashier role
   * 
   * @param org
   * @param role
   * @return role
   */
  private Role updateCashier(String sellingLocation, Role role) {
    String roleName = sellingLocation + " - Cashier";
    role.setName(roleName);
    role.setUserLevel(" CO");
    role.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    role.setManual(true);
    role.setRestrictbackend(true);
    role.setClientAdmin(false);
    OBDal.getInstance().save(role);
    return role;
  }

  /**
   * Update cashier role
   * 
   * @param org
   * @param role
   * @return role
   */
  private Role updateManager(String sellingLocation, Role role) {
    String roleName = sellingLocation + " - Manager";
    role.setName(roleName);
    role.setUserLevel(" CO");
    role.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    role.setManual(true);
    role.setRestrictbackend(false);
    role.setClientAdmin(false);
    OBDal.getInstance().save(role);
    return role;
  }

  /**
   * Adds organization access to the role
   * 
   * @param role
   *          to give access
   * @param org
   *          organization which the access is given
   */
  private void createOrgAccess(Role role, String orgId) {
    Organization org = OBDal.getInstance().getProxy(Organization.class, orgId);
    OBCriteria<RoleOrganization> roleOrgCriteria = OBDal.getInstance()
        .createCriteria(RoleOrganization.class);
    roleOrgCriteria.setMaxResults(1);
    roleOrgCriteria.add(Restrictions.eq(RoleOrganization.PROPERTY_ROLE, role));
    roleOrgCriteria.add(Restrictions.eq(RoleOrganization.PROPERTY_ORGANIZATION, org));
    RoleOrganization roleOrganization = (RoleOrganization) roleOrgCriteria.uniqueResult();
    if (roleOrganization == null) {
      roleOrganization = OBProvider.getInstance().get(RoleOrganization.class);
      roleOrganization.setRole(role);
      roleOrganization.setOrganization(org);
      OBDal.getInstance().save(roleOrganization);
    }
  }

  /**
   * Creates the relation between user and role
   * 
   * @param role
   * @param user
   */
  private void createUserRoles(Role role, User user) {
    OBCriteria<UserRoles> userRolesCriteria = OBDal.getInstance().createCriteria(UserRoles.class);
    userRolesCriteria.setMaxResults(1);
    userRolesCriteria.add(Restrictions.eq(UserRoles.PROPERTY_USERCONTACT, user));
    UserRoles userRoles = (UserRoles) userRolesCriteria.uniqueResult();
    if (userRoles == null) {
      userRoles = OBProvider.getInstance().get(UserRoles.class);
      userRoles.setUserContact(user);
      userRoles.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
    }
    userRoles.setRole(role);
    OBDal.getInstance().save(userRoles);
  }

  /**
   * Creates Form Access record
   * 
   * @param role
   */
  private void createFormAcess(Role role) {
    Form form = OBDal.getInstance().get(Form.class, "B7B7675269CD4D44B628A2C6CF01244F");

    if (role.getADFormAccessList().isEmpty()) {
      logger.warn("[OperatorFlow] Create form empty- role: " + role.getId());
      FormAccess formAccess = OBProvider.getInstance().get(FormAccess.class);
      formAccess.setRole(role);
      formAccess.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      formAccess.setSpecialForm(form);
      OBDal.getInstance().save(formAccess);
      role.getADFormAccessList().add(formAccess);
    } else {
      logger.warn("[OperatorFlow] Create form not empty- role: " + role.getId());
      OBCriteria<FormAccess> formAcessCriteria = OBDal.getInstance()
          .createCriteria(FormAccess.class);
      formAcessCriteria.setMaxResults(1);
      formAcessCriteria.add(Restrictions.eq(FormAccess.PROPERTY_ROLE, role));
      formAcessCriteria.add(Restrictions.eq(FormAccess.PROPERTY_SPECIALFORM, form));
      FormAccess formAccessResult = (FormAccess) formAcessCriteria.uniqueResult();
      if (formAccessResult == null) {
        logger.warn("[OperatorFlow] Create form formaccessreuslt null - role: " + role.getId());
        FormAccess formAccess = OBProvider.getInstance().get(FormAccess.class);
        formAccess.setRole(role);
        formAccess.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        formAccess.setSpecialForm(form);
        OBDal.getInstance().save(formAccess);
        role.getADFormAccessList().add(formAccess);
      }
    }
  }

  /**
   * Set or update the discount of a cashier role
   * 
   * @param role
   * @param rptValue
   */
  private void createOrUpdateRoleDiscount(Role role, BigDecimal rptValue) {
    OBCriteria<RoleDiscounts> roleDiscountCriteria = OBDal.getInstance()
        .createCriteria(RoleDiscounts.class);
    roleDiscountCriteria.add(Restrictions.eq(UserRoles.PROPERTY_ROLE, role));
    roleDiscountCriteria.setMaxResults(1);

    RoleDiscounts roleDiscount = (RoleDiscounts) roleDiscountCriteria.uniqueResult();

    if (rptValue.compareTo(ONEHUNDRED) == 0) {
      // When value is 100 no role discount is required. Remove in case it exists.
      if (roleDiscount != null) {
        OBDal.getInstance().remove(roleDiscount);
      }
    } else {
      if (roleDiscount == null) {
        roleDiscount = OBProvider.getInstance().get(RoleDiscounts.class);
        roleDiscount = new RoleDiscounts();
        roleDiscount.setDiscountTo(ONEHUNDRED);
        roleDiscount.setApprovalRequired(true);
      }
      roleDiscount.setRole(role);
      roleDiscount.setDiscountFrom(rptValue);
      OBDal.getInstance().save(roleDiscount);
    }
  }

  /**
   * 
   * @param error
   * @param errorCount
   * @param errorMessages
   * @param syncPersonnel
   * @param date
   * @return
   */
  private String processErrors(DECOPEErrors error, int errorCount,
      Map<String, String> errorMessages, SyncPersonnelType syncPersonnel, Date date) {
    String myError = null;

    try {
      StringWriter sw = new StringWriter();
      Marshaller marshaller = JAXBContextSingleton.getContext().createMarshaller();

      JAXBElement<?> jaxbElement = new ObjectFactory().createSyncPersonnel(syncPersonnel);

      marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
      marshaller.marshal(jaxbElement, sw);
      String errorMessage = OBMessageUtils.messageBD("DECOPE_Errors_Found_Xml");
      Map<String, String> errorParam = new HashMap<String, String>();
      errorParam.put("ErrorOperatorNumber", String.valueOf(errorCount));
      myError = OBMessageUtils.parseTranslation(errorMessage, errorParam);
      StringBuffer errorSb = new StringBuffer();
      errorSb.append(myError);
      for (Entry<String, String> errorEntry : errorMessages.entrySet()) {
        errorSb.append("\n ");
        errorSb.append(errorEntry.getKey());
        errorSb.append(": ");
        errorSb.append(errorEntry.getValue());
        errorSb.append(". ");
      }
      updateError(error, date, sw.toString(), errorSb.toString());
    } catch (JAXBException e) {
      myError = e.getMessage();
    }

    return myError;
  }

  /**
   * Generates the response of action handler
   * 
   * @param returnError
   * @return
   */
  private JSONObject createResponse(String returnError) {
    JSONObject result = new JSONObject();
    JSONObject finalResult = new JSONObject();
    try {
      result.put("message", finalResult);
      if (StringUtils.isEmpty(returnError)) {
        finalResult.put("severity", "success");
        finalResult.put("title", "");
        finalResult.put("text", OBMessageUtils.messageBD("Success"));
      } else {
        finalResult.put("severity", "error");
        finalResult.put("title", OBMessageUtils.messageBD("Error"));
        finalResult.put("text", returnError.toString());
      }
      JSONObject actions = new JSONObject();
      actions.put("refreshGrid", new JSONObject());
      result.put("responseActions", actions);
    } catch (JSONException e) {
      logger.error("Error returning error message", e);
    }
    return result;
  }

  /**
   * Returns if a user should have it's password reset based on the possible flags
   *
   * @param resetFlag
   * @param typeOfAction
   * @return
   */
  private boolean checkIfChangePassword(boolean resetFlag, String typeOfAction) {
    return (resetFlag || typeOfAction.equals("3") || typeOfAction.equals("6"));
  }
}
