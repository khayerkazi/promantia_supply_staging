/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking.terminal;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.mobile.core.process.ProcessHQLQuery;
import org.openbravo.mobile.warehouse.WarehouseConstants;

public class IncidencesTypesLoader extends ProcessHQLQuery {

  @Override
  protected boolean isAdminMode() {
    return true;
  }

  @Override
  protected String getFormId() {
    return WarehouseConstants.FORM_ID;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    return Arrays
        .asList(new String[] { "from OBWPL_pickinglistincidence where 1=1 and ($readableCriteria) order by name" });
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }
}
