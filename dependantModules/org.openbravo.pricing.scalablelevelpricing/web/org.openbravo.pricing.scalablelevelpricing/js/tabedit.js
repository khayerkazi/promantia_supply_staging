/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, console, _*/


_.each(OB.OBPOSPointOfSale.UI.EditLine.prototype.propertiesToShow, function (item, index) {
  if (item && (item.name === 'qtyLine' || item.name === 'priceLine')) {
    OB.OBPOSPointOfSale.UI.EditLine.prototype.propertiesToShow.splice(index, 1);
  }
});
_.each(OB.OBPOSPointOfSale.UI.EditLine.prototype.propertiesToShow, function (item, index) {
  if (item && (item.name === 'qtyLine' || item.name === 'priceLine')) {
    OB.OBPOSPointOfSale.UI.EditLine.prototype.propertiesToShow.splice(index, 1);
  }
});

OB.OBPOSPointOfSale.UI.EditLine.prototype.propertiesToShow.push({
  kind: 'OB.OBPOSPointOfSale.UI.LinePropertyDiv',
  position: 15,
  name: 'lineRanges',
  I18NLabel: 'POSLVPR_ranges',
  display: false,
  render: function (line) {
    var me = this;
    var criteria;
    if (line && line.get('product').get('algorithm') === 'SLP_algorithm') {

      criteria = {
        productId: line.get('product').id,
        _orderByClause: 'quantity asc'
      };

      OB.Dal.find(OB.Model.LevelProductPrice, criteria, function (data) {
        var finalString = 'UNIT - ' + OB.DEC.toNumber(new BigDecimal(String(line.get('product').get('originalStandardPrice')))) + ' <br /> ';
        _.each(data.models, function (range, iter) {
          finalString += range.get('_identifier') + '(' + range.get('quantity') + ') - ' + OB.DEC.toNumber(new BigDecimal(String(range.get('price'))));
          if (iter < data.length - 1) {
            finalString += ' <br /> ';
          }
        });
        me.$.propertyValue.setAllowHtml(true);
        me.$.propertyValue.setContent(finalString);
        me.show();
      }, function () {
        me.hide();
      });
    } else {
      this.hide();
    }
  }
});