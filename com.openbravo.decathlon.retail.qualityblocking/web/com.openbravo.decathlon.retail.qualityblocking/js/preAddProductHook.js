/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, moment, _ */

(function () {

  OB.MobileApp.model.hookManager.registerHook('OBPOS_PreAddProductToOrder', function (args, callbacks) {
    if (args.productToAdd.get('decqbIsblocked')) {
      args.cancelOperation = true;
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('DECQB_ProductNotAdded'));
    }
    OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
  });

}());