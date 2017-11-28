/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone, _ */

(function () {

  var CashUp = OB.Data.ExtensibleModel.extend({
    modelName: 'CashUp',
    tableName: 'cashup',
    entityName: 'CashUp',
    source: '',
    local: true
  });

  CashUp.addProperties([{
    name: 'id',
    column: 'cashup_id',
    primaryKey: true,
    type: 'TEXT'
  }, {
    name: 'netSales',
    column: 'netSales',
    type: 'NUMERIC'
  }, {
    name: 'grossSales',
    column: 'grossSales',
    type: 'NUMERIC'
  }, {
    name: 'netReturns',
    column: 'netReturns',
    type: 'NUMERIC'
  }, {
    name: 'grossReturns',
    column: 'grossReturns',
    type: 'NUMERIC'
  }, {
    name: 'totalRetailTransactions',
    column: 'totalRetailTransactions',
    type: 'NUMERIC'
  }, {
    name: 'createdDate',
    column: 'createdDate',
    type: 'TEXT'
  }, {
    name: 'userId',
    column: 'userId',
    type: 'TEXT'
  }, {
    name: 'objToSend',
    column: 'objToSend',
    type: 'TEXT'
  }, {
    name: 'isbeingprocessed',
    column: 'isbeingprocessed',
    type: 'TEXT'
  }]);


  OB.Data.Registry.registerModel(CashUp);

}());