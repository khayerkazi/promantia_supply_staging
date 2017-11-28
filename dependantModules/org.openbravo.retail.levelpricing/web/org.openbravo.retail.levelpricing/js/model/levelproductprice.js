/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone */

(function () {

  var LvlProductPrice = OB.Data.ExtensibleModel.extend({
    modelName: 'LevelProductPrice',
    tableName: 'lvlpr_levelproductprice',
    entityName: 'LevelProductPrice',
    source: 'org.openbravo.retail.levelpricing.master.LevelProductPrices',
    includeTerminalDate: true,
    dataLimit: 300
  });

  LvlProductPrice.addProperties([{
    name: 'id',
    column: 'lvlpr_levelproductprice_id',
    primaryKey: true,
    type: 'TEXT'
  }, {
    name: 'productId',
    column: 'm_product_id',
    type: 'TEXT'
  }, {
    name: 'quantity',
    column: 'quantity',
    type: 'NUMERIC'
  }, {
    name: 'price',
    column: 'price',
    type: 'NUMERIC'
  }, {
    name: '_identifier',
    column: '_identifier',
    type: 'TEXT'
  }]);

  LvlProductPrice.addIndex([{
    name: 'lvlpr_in_prodId',
    columns: [{
      name: 'm_product_id',
      sort: 'desc'
    }]
  }]);

  OB.Data.Registry.registerModel(LvlProductPrice);

  // add the model to the window.
  OB.OBPOSPointOfSale.Model.PointOfSale.prototype.models.push(OB.Model.LevelProductPrice);
}());