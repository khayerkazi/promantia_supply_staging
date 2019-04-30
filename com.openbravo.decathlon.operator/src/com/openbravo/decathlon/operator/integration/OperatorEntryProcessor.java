/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator.integration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.importprocess.ImportEntryManager.ImportEntryQualifier;
import org.openbravo.service.importprocess.ImportEntryProcessor;

import com.openbravo.decathlon.operator.registeruser.OBOperatorRegister;

/**
 * Class extending {@link ImportEntryProcessor} to process the registers in DECOPE_Errors entity.
 * 
 */
@ImportEntryQualifier(entity = "DECOPE_Errors")
@ApplicationScoped
public class OperatorEntryProcessor extends ImportEntryProcessor {

  @Override
  protected ImportEntryProcessRunnable createImportEntryProcessRunnable() {
    return WeldUtils.getInstanceFromStaticBeanManager(ErrorLoaderRunnable.class);
  }

  @Override
  protected boolean canHandleImportEntry(ImportEntry importEntryInformation) {
    return "DECOPE_Errors".equals(importEntryInformation.getTypeofdata());
  }

  @Override
  protected String getProcessSelectionKey(ImportEntry importEntry) {
    return (String) importEntry.getOrganization().getId();
  }

  /**
   * 
   * Inner class that processes the Operator Error entries.
   * 
   */
  private static class ErrorLoaderRunnable extends ImportEntryProcessRunnable {
    @Inject
    OBOperatorRegister operatorRegister;

    @Inject
    ImportEntryManager importEntryManager;

    /**
     * Method that processes each entry of DECOPE_Errors. It updates the user information with an
     * xml.
     */
    @Override
    protected void processEntry(ImportEntry importEntry) throws Exception {
      try {
        JSONObject jsonEntry = new JSONObject(importEntry.getJsonInfo());
        String requestId = jsonEntry.getJSONObject("data").getString("requestId");
        operatorRegister.processOperatorRegister(requestId);
        importEntryManager.setImportEntryProcessed(importEntry.getId());
        OBDal.getInstance().commitAndClose();
      } catch (Exception ignore) {
      }
    }
  }
}
