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
import org.hibernate.criterion.Restrictions;
import org.openbravo.common.actionhandler.SRMOPickEditLines;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

public class HighVolumeReturnPickEditLines extends SRMOPickEditLines {
  private static final Logger log = Logger.getLogger(HighVolumeReturnPickEditLines.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();
    try {
      // Invoking process in super
      result = super.doExecute(parameters, content);
    } finally {
      // delete temporary info created for this process
      OBContext.setAdminMode(false);
      try {
        HttpServletRequest httpSession = (HttpServletRequest) parameters.get("_httpRequest");
        String dbSessionId = (String) httpSession.getSession().getAttribute("#AD_SESSION_ID");
        OBCriteria<ReturnBP> qTempBPs = OBDal.getInstance().createCriteria(ReturnBP.class);
        qTempBPs.add(Restrictions.eq(ReturnBP.PROPERTY_SESSION + ".id", dbSessionId));

        int i = 0;
        for (ReturnBP info : qTempBPs.list()) {
          OBDal.getInstance().remove(info);
          i++;
        }
        if (i == 0) {
          log.warn("Couldn't delete temp session info");
        }
      } catch (Exception e) {
        log.error("Error deleting temporary process info", e);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    return result;
  }
}
