/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

OB.MobileApp.model.hookManager.registerHook('OBMWHP_relatedOrdersInformation', function (args, callbacks) {
  if (args.orderToDraw && args.orderToDraw.order && args.orderToDraw.order.deoTrayNumber) {
    args.relatedOrdersComponent.value = args.relatedOrdersComponent.value.toString() + ' - Tray: ' + args.orderToDraw.order.deoTrayNumber.toString();
  }
  OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
});