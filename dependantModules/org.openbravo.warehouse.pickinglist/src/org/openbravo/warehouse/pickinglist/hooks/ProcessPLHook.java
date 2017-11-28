/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.warehouse.pickinglist.hooks;

import java.util.List;
import java.util.Set;

import org.openbravo.model.common.order.Order;
import org.openbravo.warehouse.pickinglist.PickingList;

public interface ProcessPLHook {

  public void exec(PickingList pickingList, Set<String> processedOrders,
      Set<String> processedShipments, List<Order> completedOrders) throws Exception;
}