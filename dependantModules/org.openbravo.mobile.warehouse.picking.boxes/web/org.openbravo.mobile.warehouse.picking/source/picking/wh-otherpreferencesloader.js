/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo, Backbone, _, console*/


setTimeout(function () {
  OB.MobileApp.model.addPropertiesLoader({
    properties: ['otherPreferences'],
    loadFunction: function (terminalModel) {
      var me = this;
      var prefsToLoad = ['OBWPL_autoConfirmIncidences'];
      new OB.DS.Request('org.openbravo.mobile.warehouse.picking.terminal.OtherPreferencesLoader').exec({
        prefs: prefsToLoad
      }, function (data) {
        _.each(data, function (item) {
          terminalModel.get('permissions')[item.searchKey] = item.value;
        }, this);
        terminalModel.propertiesReady(me.properties);
      });
    }
  });
}, 10);