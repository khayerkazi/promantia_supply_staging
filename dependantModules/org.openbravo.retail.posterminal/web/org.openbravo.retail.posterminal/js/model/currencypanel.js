/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone, _ */

(function () {

  var CurrencyPanel = OB.Data.ExtensibleModel.extend({
    modelName: 'CurrencyPanel',
    tableName: 'obpos_currency_panel',
    entityName: 'OBPOS_CurrencyPanel',
    source: 'org.openbravo.retail.posterminal.master.CurrencyPanel'
  });

  CurrencyPanel.addProperties([{
    name: 'id',
    column: 'obpos_currency_panel_id',
    primaryKey: true,
    type: 'TEXT'
  }, {
    name: 'currency',
    column: 'c_currency_id',
    type: 'TEXT'
  }, {
    name: 'amount',
    column: 'amount',
    type: 'NUMERIC'
  }, {
    name: 'backcolor',
    column: 'backcolor',
    type: 'TEXT'
  }, {
    name: 'bordercolor',
    column: 'bordercolor',
    type: 'TEXT'
  }, {
    name: 'lineNo',
    column: 'line',
    type: 'NUMERIC'
  }, {
    name: '_identifier',
    column: '_identifier',
    type: 'TEXT'
  }]);

  CurrencyPanel.addIndex([{
    name: 'currency_panel_idx',
    columns: [{
      name: 'c_currency_id',
      sort: 'desc'
    }]
  }]);

  OB.Data.Registry.registerModel(CurrencyPanel);
}());