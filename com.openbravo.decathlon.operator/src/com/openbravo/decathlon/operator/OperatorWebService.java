/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.web.WebService;

import com.decathlon.oagis942.IdentifierType;
import com.decathlon.oagis942.PartyIDs;
import com.decathlon.oagis942.SyncPersonnelType;
import com.openbravo.decathlon.operator.utils.DECOPEUtils;
import com.openbravo.decathlon.operator.utils.JAXBContextSingleton;

/**
 * Implementation of webservice.
 */
public class OperatorWebService implements WebService {

  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    processRequest(request, response);
  }

  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    processRequest(request, response);
  }

  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    processRequest(request, response);
  }

  /**
   * Main method that processes the request
   * 
   * @param request
   *          {@link HttpServletRequest} the request incoming
   * @param response
   *          {@link HttpServletResponse} the response outgoing
   * @throws JSONException
   */
  private void processRequest(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // Gets the attribute
    StringBuffer jb = new StringBuffer();
    String line = null;
    BufferedReader reader = request.getReader();
    while ((line = reader.readLine()) != null) {
      jb.append(line);
    }

    // READ THE XML
    JAXBContext jaxbContext = JAXBContextSingleton.getContext();
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    Object syncPersonnelType = jaxbUnmarshaller.unmarshal(new StringReader(jb.toString()));

    SyncPersonnelType syncPersonnel = JAXBContextSingleton.getIntrospector(syncPersonnelType);

    List<PartyIDs> partyIdsList = syncPersonnel.getDataArea().getPersonnels().get(0).getPartyIDs();
    String organizationSellingLocation = "0070";
    String partynumber = "";
    for (PartyIDs partyIds : partyIdsList) {
      List<IdentifierType> orgIdTypeList = partyIds.getIDS();
      for (IdentifierType orgIdType : orgIdTypeList) {
        if (orgIdType.getSchemeName().equals("partyNumber")) {
          partynumber = String.format("%04d", Integer.parseInt(orgIdType.getValue()));
        }
      }
    }
    organizationSellingLocation += partynumber;
    Organization organization = DECOPEUtils.obtainOrganization(organizationSellingLocation);
    if (organization == null) {
      String errorMessage = OBMessageUtils.messageBD("decope_organization_not_found");
      Map<String, String> errorParam = new HashMap<String, String>();
      errorParam.put("OrganizationId", organizationSellingLocation);
      throw new OBException(OBMessageUtils.parseTranslation(errorMessage, errorParam));
    }

    // Set Context
    OBContext obContext = OBContext.getOBContext();
    obContext.setCurrentClient(organization.getClient());
    obContext.setCurrentOrganization(organization);
    obContext.addWritableOrganization(organization.getId());
    OBCriteria<Role> roleCriteria = OBDal.getInstance().createCriteria(Role.class);
    roleCriteria.setMaxResults(1);
    roleCriteria.setFilterOnReadableClients(false);
    roleCriteria.add(Restrictions.eq(Role.PROPERTY_CLIENT, organization.getClient()));
    roleCriteria.add(Restrictions.eq(Role.PROPERTY_MANUAL, false));
    roleCriteria.add(Restrictions.eq(Role.PROPERTY_CLIENTADMIN, true));
    Role role = (Role) roleCriteria.uniqueResult();
    if (role == null) {
      String errorMessage = OBMessageUtils.messageBD("decim_role_not_found");
      Map<String, String> errorParam = new HashMap<String, String>();
      errorParam.put("ClientName", organization.getClient().getName());
      throw new OBException(OBMessageUtils.parseTranslation(errorMessage, errorParam));
    }
    obContext.setReadableClients(role);
    obContext.setRole(role);
    OBContext.setOBContext(obContext);

    String xmlContent = jb.toString();
    // Create json for the ImportEntryManager
    if (StringUtils.isNotEmpty(xmlContent)) {
      DECOPEErrors error = OBProvider.getInstance().get(DECOPEErrors.class);
      error.setErrorMessage(OBMessageUtils.messageBD("DECOPE_Request_Scheduled"));
      error.setErrorDate(new Date());
      error.setXMLContent(xmlContent);
      OBDal.getInstance().save(error);
      JSONObject json = new JSONObject();
      JSONObject jsonData = new JSONObject();
      jsonData.put("requestId", error.getId());
      json.put("data", jsonData);
      ImportEntryManager importEntryManager = WeldUtils
          .getInstanceFromStaticBeanManager(ImportEntryManager.class);
      importEntryManager.createImportEntry(error.getId(), "DECOPE_Errors", json.toString());
    }
    // write to the response
    response.setStatus(HttpServletResponse.SC_OK);
  }
}