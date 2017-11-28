/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo */

(function () {

enyo.kind({
  name: 'OBWH.Terminal.UI',
  kind: 'OB.UI.Terminal'
});

OBWH.Terminal.Model = OB.Model.Terminal.extend({
  initialize: function () {
    this.set({
      appName: 'OBWH',
      appModuleId: '12B324560ABB478BAC79FFF6C9C75C69',
      appDisplayName: 'Openbravo Warehouse Mobile Operations',
      loginUtilsUrl: '../../org.openbravo.mobile.warehouse.loginutils',
      useBarcode: true,
      profileOptions: {
        showOrganization: true,
        showWarehouse: true
      },
      localDB: {
        size: 1024 * 1024,
        name: 'OBWHDB',
        displayName: 'Warehouse DB',
        version: '1'
      }
    });

    OB.Constants = OB.Constants || {};
    OB.Constants = {
      FIELDSEPARATOR: '$',
      IDENTIFIER: '_identifier'
    };
    
    OB.Model.Terminal.prototype.initialize.call(this);
  },

  renderMain: function () {
    this.navigate('wh');
  },

  postLoginActions: function () {
    this.set('isLoggingIn', false);
  }
});

// from this point, OB.MobileApp.model will be available
// the initialization is done to a dummy variable to allow the model to be extendable
var initializeOBModelTerminal = new OBWH.Terminal.Model();

OB.OBWH = {};

}());