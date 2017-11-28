/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.warehouse.pickinglist.hooks;

import org.openbravo.model.common.order.Order;
import org.openbravo.warehouse.pickinglist.PickingList;

public interface ClosePLOutbound_ProcessOrdersHook {

  public void exec(Order order, PickingList picking) throws Exception;
}