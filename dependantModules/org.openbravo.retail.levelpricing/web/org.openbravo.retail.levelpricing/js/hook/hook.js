/*
 ************************************************************************************
 * Copyright (C) 2013-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone */

OB.MobileApp.model.hookManager.registerHook('OBPOS_AddProductToOrder', function (args, callbacks) {
  if (args.productToAdd.get('algorithm') && _.isString(args.productToAdd.get('algorithm')) && args.productToAdd.get('algorithm') !== 'undefined' && args.productToAdd.get('algorithm') !== 'S') {
    var criteria = {
      productId: args.productToAdd.id
    };
    OB.Dal.find(OB.Model.LevelProductPrice, criteria, function (data) {
      if (OB.MobileApp.model.get('pricingAlgorithms') && OB.MobileApp.model.get('pricingAlgorithms')[args.productToAdd.get('algorithm')]){
        OB.MobileApp.model.hookManager.executeHooks('LVLPR_addProduct', {
          originalArgs: args,
          receipt: args.receipt,
          possiblePrices: data,
          algorithm: args.productToAdd.get('algorithm')
        }, function (args) {
          // This is a MUST to properly manage callbacks
          OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
        });
      } else {
    	OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
      }
    }, function () {
      OB.error('Unexpected error while executing an extension of the hook "OBPOS_AddProductToOrder" by org.openbravo.retail.levelpricing');
      OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
    });
  } else {
    OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
  }
});