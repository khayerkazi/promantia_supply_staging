/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.warehouse.pickinglist.hooks;

import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.warehouse.pickinglist.PickingList;

public interface ValidateBoxSerialNumberHook {

  public OBError exec(String proposedSerialNumber, PickingList pickingList) throws Exception;
}