/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo */

OB.MobileApp.model.hookManager.registerHook('OBPOS_cashManagementTransactionHook', function (args, callbacks) {
  var description = args.cashManagementTransactionToAdd.get('description');
  description  += '/' + args.newCashManagementTransaction.get('refund').get('documentNo');
  args.cashManagementTransactionToAdd.set('description', description);
  args.cashManagementTransactionToAdd.set('refundData', args.newCashManagementTransaction.get('refund').toJSON());
  // This is a MUST to properly manage callbacks
  OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
});