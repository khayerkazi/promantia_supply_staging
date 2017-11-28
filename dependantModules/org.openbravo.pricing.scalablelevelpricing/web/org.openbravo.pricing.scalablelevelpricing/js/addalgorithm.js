/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, console, _*/

OB.MobileApp.model.addPropertiesLoader({
  properties: ['SLP_algorithm'],
  loadFunction: function (terminalModel) {
    var algorithms = terminalModel.get('pricingAlgorithms');
    console.log('loading...', this.properties);
    if (!algorithms) {
      algorithms = {};
    }
    algorithms['SLP_algorithm'] = true;

    OB.MobileApp.model.hookManager.registerHook('LVLPR_addProduct', function (args, callbacks) {
      var finalResult;
      var linesToModify = [];
      var affectedLines = args.receipt.getLinesByProduct(args.originalArgs.productToAdd.id);

      if (args.algorithm === 'SLP_algorithm' && args.originalArgs.productToAdd.get('groupProduct') && affectedLines && affectedLines.length > 0) {
        var memo = 0;
        var totalQty = 0;
        totalQty = _.reduce(affectedLines, function (memo, line) {
          return memo + line.get('qty');
        }, memo);
        if (_.isNaN(args.originalArgs.qtyToAdd) || _.isUndefined(args.originalArgs.qtyToAdd) || _.isNull(args.originalArgs.qtyToAdd)) {
          totalQty += 1;
        } else {
          totalQty += args.originalArgs.qtyToAdd;
        }
        finalResult = OB.SLP.getLinesPricesAlgorithm(args.originalArgs.productToAdd, args.possiblePrices, totalQty);
        _.each(affectedLines, function (line) {
          if (line.get('product').get('standardPrice') !== finalResult.price || line.get('product').get('SLP_usedRange') !== finalResult.minQtyToUse) {
            linesToModify.push({
              lineCid: line.cid,
              newQty: totalQty,
              newPrice: finalResult.price,
              productProperties: [{
                name: 'SLP_usedRange',
                value: finalResult.minQtyToUse
              },{
                name: 'SLP_rangeIdentifier',
                value: finalResult.rangeIdentifier
              }],
              lineProperties: []
            });
          }
        });
        if (linesToModify.length > 0) {
          args.originalArgs.linesToModify = linesToModify;
          args.originalArgs.useLines = true;
        } else {
          args.originalArgs.useLines = false;
        }
      } else {
        //Use normal flow
        args.originalArgs.useLines = false;
      }
      OB.MobileApp.model.hookManager.callbackExecutor(args.originalArgs, callbacks);
    });

    terminalModel.set('pricingAlgorithms', algorithms);
    terminalModel.propertiesReady(this.properties);
  }
});

OB = OB || {};
OB.SLP = OB.SLP || {};
OB.SLP.logFinalResult = function (product, units, finalResult) {
  console.log('The final price for ' + units + ' units of product ' + product.get('_identifier') + ' is ' + finalResult.price + '(' + finalResult.rangeIdentifier + ')');
};
OB.SLP.getLinesPricesAlgorithm = function (product, possiblePrices, qty) {
  var sortedRanges = [];
  var i = 0;
  var finalResult = {
    minQtyToUse: 1,
    rangeIdentifier: 'Unit',
    price: OB.DEC.toNumber(new BigDecimal(String(product.get('originalStandardPrice'))))
  };

  sortedRanges = _.sortBy(possiblePrices.models, function (range) {
    return range.get('quantity') * -1;
  });

  for (i = 0; i < sortedRanges.length; i++) {
    if (sortedRanges[i].get('quantity') > 1 && qty >= sortedRanges[i].get('quantity')) {
      finalResult.minQtyToUse = sortedRanges[i].get('quantity');
      finalResult.rangeIdentifier = sortedRanges[i].get('_identifier');
      finalResult.price = OB.DEC.toNumber(new BigDecimal(String(sortedRanges[i].get('price'))));
      break;
    }
  }

  OB.SLP.logFinalResult(product, qty, finalResult);

  return finalResult;
};