/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.warehouse.pickinglist.hooks;

import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.warehouse.pickinglist.PickingList;

public interface ClosePLOutbound_CreateShipmentsHook {

  public void exec(ShipmentInOut shipment, DocumentType docType, PickingList picking)
      throws Exception;
}