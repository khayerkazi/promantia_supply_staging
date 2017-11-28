/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse;

import org.openbravo.mobile.core.login.MobileCoreLoginUtilsServlet;

public class WarehouseLoginUtilsServlet extends MobileCoreLoginUtilsServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected String getModuleId() {
    return WarehouseConstants.MODULE_ID;
  }
}
