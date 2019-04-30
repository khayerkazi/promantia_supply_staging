/*
 ************************************************************************************
 * Copyright (C) 2017-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

OB.UTIL.HookManager.registerHook('OBPOS_preAddPayment', function (args, callbacks) {
  var gross = OB.MobileApp.model.receipt.get('gross'),
      maxReturnAmount = OB.MobileApp.model.get('context').user.dECOPEMaxReturnAmount,
      i, cashAmount = 0;
  if (gross < 0 && maxReturnAmount && maxReturnAmount > 0) {
    if (args.paymentToAdd.get('isCash')) {
      cashAmount = args.paymentToAdd.get('amount');
    }
    if (args.payments.length > 0) {
      for (i = 0; i < args.payments.length; i++) {
        if (args.payments.at(i).get('isCash')) {
          cashAmount = OB.DEC.add(cashAmount, args.payments.at(i).get('amount'));
        }
      }
    }
    if (OB.DEC.abs(cashAmount) > OB.DEC.abs(maxReturnAmount)) {
      OB.UTIL.Approval.requestApproval(
      args.context, 'DECOPE_maxreturnapproval', function (approved, supervisor, approvalType) {
        if (approved) {
          OB.UTIL.HookManager.callbackExecutor(args, callbacks);
        }
      });
    } else {
      OB.UTIL.HookManager.callbackExecutor(args, callbacks);
    }
  } else {
    OB.UTIL.HookManager.callbackExecutor(args, callbacks);
  }
});