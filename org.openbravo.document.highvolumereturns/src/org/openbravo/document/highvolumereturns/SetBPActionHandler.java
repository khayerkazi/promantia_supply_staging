/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.document.highvolumereturns;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Session;
import org.openbravo.model.common.businesspartner.BusinessPartner;

public class SetBPActionHandler extends BaseActionHandler {
  final static Logger log = Logger.getLogger(SetBPActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(false);
    try {
      // Remove old temporary BPs
      int i = 0;
      OBCriteria<ReturnBP> qOldTempBPs = OBDal.getInstance().createCriteria(ReturnBP.class);
      for (ReturnBP oldTmpBP : qOldTempBPs.list()) {
        if (!oldTmpBP.getSession().isSessionActive()) {
          OBDal.getInstance().remove(oldTmpBP);
          i++;
        }
      }
      if (i > 0) {
        log.info("Removed " + i + " old temporary BPs");
      }

      // set current BP in temporary table
      HttpServletRequest httpSession = (HttpServletRequest) parameters.get("_httpRequest");
      String dbSessionId = (String) httpSession.getSession().getAttribute("#AD_SESSION_ID");

      JSONObject params = new JSONObject(content);
      ReturnBP retBp = OBProvider.getInstance().get(ReturnBP.class);
      retBp.setBusinessPartner((BusinessPartner) OBDal.getInstance().getProxy("BusinessPartner",
          params.getString("bp")));
      retBp.setProcessRun(params.getString("processId"));
      retBp.setSession((Session) OBDal.getInstance().getProxy("ADSession", dbSessionId));
      OBDal.getInstance().save(retBp);
    } catch (Exception e) {
      log.error("error setting BP", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return new JSONObject();
  }
}
