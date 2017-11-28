/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B,_,moment,Backbone,localStorage, enyo */

(function () {

  // Sales.OrderLine Model
  var OrderLine = Backbone.Model.extend({
    modelName: 'OrderLine',
    defaults: {
      product: null,
      productidentifier: null,
      uOM: null,
      qty: OB.DEC.Zero,
      price: OB.DEC.Zero,
      priceList: OB.DEC.Zero,
      gross: OB.DEC.Zero,
      net: OB.DEC.Zero,
      description: ''
    },

    initialize: function (attributes) {
      if (attributes && attributes.product) {
        this.set('product', new OB.Model.Product(attributes.product));
        this.set('productidentifier', attributes.productidentifier);
        this.set('uOM', attributes.uOM);
        this.set('qty', attributes.qty);
        this.set('price', attributes.price);
        this.set('priceList', attributes.priceList);
        this.set('gross', attributes.gross);
        this.set('net', attributes.net);
        this.set('promotions', attributes.promotions);
        this.set('priceIncludesTax', attributes.priceIncludesTax);
        if (!attributes.grossListPrice && attributes.product && attributes.product.listPrice) {
          this.set('grossListPrice', attributes.product.listPrice);
        }
      }
    },

    getQty: function () {
      return this.get('qty');
    },

    printQty: function () {
      return this.get('qty').toString();
    },

    printPrice: function () {
      return OB.I18N.formatCurrency(this.get('_price') || this.get('nondiscountedprice') || this.get('price'));
    },

    printDiscount: function () {
      var disc = OB.DEC.sub(this.get('product').get('standardPrice'), this.get('price'));
      var prom = this.getTotalAmountOfPromotions();
      // if there is a discount no promotion then only discount no promotion is shown
      // if there is not a discount no promotion and there is a promotion then promotion is shown
      if (OB.DEC.compare(disc) === 0) {
        if (OB.DEC.compare(prom) === 0) {
          return '';
        } else {
          return OB.I18N.formatCurrency(prom);
        }
      } else {
        return OB.I18N.formatCurrency(disc);
      }
    },

    // returns the discount to substract in total
    discountInTotal: function () {
      var disc = OB.DEC.sub(this.get('product').get('standardPrice'), this.get('price'));
      // if there is a discount no promotion then total is price*qty
      // otherwise total is price*qty - discount
      if (OB.DEC.compare(disc) === 0) {
        return this.getTotalAmountOfPromotions();
      } else {
        return 0;
      }
    },

    calculateGross: function () {
      if (this.get('priceIncludesTax')) {
        this.set('gross', OB.DEC.mul(this.get('qty'), this.get('price')));
      } else {
        this.set('net', OB.DEC.mul(this.get('qty'), this.get('price')));
      }
    },

    getGross: function () {
      return this.get('gross');
    },

    getNet: function () {
      return this.get('net');
    },

    printGross: function () {
      return OB.I18N.formatCurrency(this.get('_gross') || this.getGross());
    },

    printNet: function () {
      return OB.I18N.formatCurrency(this.get('nondiscountednet') || this.getNet());
    },

    getTotalAmountOfPromotions: function () {
      var memo = 0;
      if (this.get('promotions') && this.get('promotions').length > 0) {
        return _.reduce(this.get('promotions'), function (memo, prom) {
          if (OB.UTIL.isNullOrUndefined(prom.amt)) {
            return memo;
          }
          return memo + prom.amt;
        }, memo, this);
      } else {
        return 0;
      }
    },
    isAffectedByPack: function () {
      return _.find(this.get('promotions'), function (promotion) {
        if (promotion.pack) {
          return true;
        }
      }, this);
    },

    stopApplyingPromotions: function () {
      var promotions = this.get('promotions'),
          i;
      if (promotions) {
        if (OB.POS.modelterminal.get('terminal').bestDealCase && promotions.length > 0) {
          // best deal case can only apply one promotion per line
          return true;
        }
        for (i = 0; i < promotions.length; i++) {
          if (!promotions[i].applyNext) {
            return true;
          }
        }
      }
      return false;
    },

    lastAppliedPromotion: function () {
      var promotions = this.get('promotions'),
          i;
      if (this.get('promotions')) {
        for (i = 0; i < promotions.length; i++) {
          if (promotions[i].lastApplied) {
            return promotions[i];
          }
        }
      }
      return null;
    }
  });

  // Sales.OrderLineCol Model.
  var OrderLineList = Backbone.Collection.extend({
    model: OrderLine,
    isProductPresent: function (product) {
      var result = null;
      if (this.length > 0) {
        result = _.find(this.models, function (line) {
          if (line.get('product').get('id') === product.get('id')) {
            return true;
          }
        }, this);
        if (_.isUndefined(result) || _.isNull(result)) {
          return false;
        } else {
          return true;
        }
      } else {
        return false;
      }
    }
  });

  // Sales.Payment Model
  var PaymentLine = Backbone.Model.extend({
    modelName: 'PaymentLine',
    defaults: {
      'amount': OB.DEC.Zero,
      'origAmount': OB.DEC.Zero,
      'paid': OB.DEC.Zero,
      // amount - change...
      'date': null
    },
    printAmount: function () {
      if (this.get('rate')) {
        return OB.I18N.formatCurrency(OB.DEC.mul(this.get('amount'), this.get('rate')));
      } else {
        return OB.I18N.formatCurrency(this.get('amount'));
      }
    },
    printForeignAmount: function () {
      return '(' + OB.I18N.formatCurrency(this.get('amount')) + ' ' + this.get('isocode') + ')';
    }
  });

  // Sales.OrderLineCol Model.
  var PaymentLineList = Backbone.Collection.extend({
    model: PaymentLine
  });

  // Sales.Order Model.
  var Order = Backbone.Model.extend({
    modelName: 'Order',
    tableName: 'c_order',
    entityName: 'Order',
    source: '',
    dataLimit: 300,
    properties: ['id', 'json', 'session', 'hasbeenpaid', 'isbeingprocessed'],
    propertyMap: {
      'id': 'c_order_id',
      'json': 'json',
      'session': 'ad_session_id',
      'hasbeenpaid': 'hasbeenpaid',
      'isbeingprocessed': 'isbeingprocessed'
    },

    defaults: {
      hasbeenpaid: 'N',
      isbeingprocessed: 'N'
    },

    createStatement: 'CREATE TABLE IF NOT EXISTS c_order (c_order_id TEXT PRIMARY KEY, json CLOB, ad_session_id TEXT, hasbeenpaid TEXT, isbeingprocessed TEXT)',
    dropStatement: 'DROP TABLE IF EXISTS c_order',
    insertStatement: 'INSERT INTO c_order(c_order_id, json, ad_session_id, hasbeenpaid, isbeingprocessed) VALUES (?,?,?,?,?)',
    local: true,
    _id: 'modelorder',
    initialize: function (attributes) {
      var orderId;
      if (attributes && attributes.id && attributes.json) {
        // The attributes of the order are stored in attributes.json
        // Makes sure that the id is copied
        orderId = attributes.id;
        attributes = JSON.parse(attributes.json);
        attributes.id = orderId;
      }

      if (attributes && attributes.documentNo) {
        this.set('id', attributes.id);
        this.set('client', attributes.client);
        this.set('organization', attributes.organization);
        this.set('documentType', attributes.documentType);
        this.set('createdBy', attributes.createdBy);
        this.set('updatedBy', attributes.updatedBy);
        this.set('orderType', attributes.orderType); // 0: Sales order, 1: Return order
        this.set('generateInvoice', attributes.generateInvoice);
        this.set('isQuotation', attributes.isQuotation);
        this.set('oldId', attributes.oldId);
        this.set('priceList', attributes.priceList);
        this.set('priceIncludesTax', attributes.priceIncludesTax);
        this.set('currency', attributes.currency);
        this.set('currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, attributes['currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER]);
        this.set('session', attributes.session);
        this.set('warehouse', attributes.warehouse);
        this.set('salesRepresentative', attributes.salesRepresentative);
        this.set('salesRepresentative' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, attributes['salesRepresentative' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER]);
        this.set('posTerminal', attributes.posTerminal);
        this.set('posTerminal' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, attributes['posTerminal' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER]);
        this.set('orderDate', new Date(attributes.orderDate));
        this.set('documentnoPrefix', attributes.documentnoPrefix);
        this.set('quotationnoPrefix', attributes.quotationnoPrefix);
        this.set('documentnoSuffix', attributes.documentnoSuffix);
        this.set('quotationnoSuffix', attributes.quotationnoSuffix);
        this.set('documentNo', attributes.documentNo);
        this.set('undo', attributes.undo);
        this.set('bp', new Backbone.Model(attributes.bp));
        this.set('lines', new OrderLineList().reset(attributes.lines));
        this.set('payments', new PaymentLineList().reset(attributes.payments));
        this.set('payment', attributes.payment);
        this.set('change', attributes.change);
        this.set('qty', attributes.qty);
        this.set('gross', attributes.gross);
        this.trigger('calculategross');
        this.set('net', attributes.net);
        this.set('taxes', attributes.taxes);
        this.set('hasbeenpaid', attributes.hasbeenpaid);
        this.set('isbeingprocessed', attributes.isbeingprocessed);
        this.set('description', attributes.description);
        this.set('print', attributes.print);
        this.set('sendEmail', attributes.sendEmail);
        this.set('isPaid', attributes.isPaid);
        this.set('isLayaway', attributes.isLayaway);
        this.set('isEditable', attributes.isEditable);
        this.set('openDrawer', attributes.openDrawer);
        _.each(_.keys(attributes), function (key) {
          if (!this.has(key)) {
            this.set(key, attributes[key]);
          }
        }, this);
      } else {
        this.clearOrderAttributes();
      }
    },

    save: function () {
      var undoCopy;

      if (this.attributes.json) {
        delete this.attributes.json; // Needed to avoid recursive inclusions of itself !!!
      }
      undoCopy = this.get('undo');
      this.unset('undo');
      this.set('json', JSON.stringify(this.toJSON()));
      if (!OB.POS.modelterminal.get('preventOrderSave')) {
        OB.Dal.save(this, function () {}, function () {
          OB.error(arguments);
        });
      }
      this.set('undo', undoCopy);
    },

    calculateTaxes: function (callback, doNotSave) {
      var tmp = new OB.DATA.OrderTaxes(this);
      this.calculateTaxes(callback);
    },

    prepareToSend: function (callback) {
      var me = this;
      this.calculateTaxes(function () {
        me.adjustPrices();
        callback(me);
      });
    },

    adjustPrices: function () {
      // Apply calculated discounts and promotions to price and gross prices
      // so ERP saves them in the proper place
      this.get('lines').each(function (line) {
        var price = line.get('price'),
            gross = line.get('gross'),
            totalDiscount = 0,
            grossListPrice = line.get('priceList'),
            grossUnitPrice, discountPercentage, base;

        // Calculate inline discount: discount applied before promotions
        if (line.get('product').get('standardPrice') !== price || (_.isNumber(line.get('discountedLinePrice')) && line.get('discountedLinePrice') !== line.get('product').get('standardPrice'))) {
          grossUnitPrice = new BigDecimal(price.toString());
          if (OB.DEC.compare(grossListPrice) === 0) {
            discountPercentage = OB.DEC.Zero;
          } else {
            discountPercentage = OB.DEC.toBigDecimal(grossListPrice).subtract(grossUnitPrice).multiply(new BigDecimal('100')).divide(OB.DEC.toBigDecimal(grossListPrice), 2, BigDecimal.prototype.ROUND_HALF_UP);
            discountPercentage = parseFloat(discountPercentage.setScale(2, BigDecimal.prototype.ROUND_HALF_UP).toString(), 10);
          }
        } else {
          discountPercentage = OB.DEC.Zero;
        }
        line.set({
          discountPercentage: discountPercentage
        }, {
          silent: true
        });

        // Calculate prices after promotions
        base = line.get('price');
        _.forEach(line.get('promotions') || [], function (discount) {
          var discountAmt = discount.actualAmt || discount.amt || 0;
          discount.basePrice = base;
          discount.unitDiscount = OB.DEC.div(discountAmt, line.get('qtyToApplyDisc') || line.get('qty'));
          totalDiscount = OB.DEC.add(totalDiscount, discountAmt);
          base = OB.DEC.sub(base, totalDiscount);
        }, this);

        gross = OB.DEC.sub(gross, totalDiscount);
        price = OB.DEC.div(gross, line.get('qty'));

        if (this.get('priceIncludesTax')) {
          line.set({
            net: OB.UTIL.getFirstValidValue([OB.DEC.toNumber(line.get('discountedNet')), line.get('net'), OB.DEC.div(gross, line.get('linerate'))]),
            pricenet: line.get('discountedNet') ? OB.DEC.div(line.get('discountedNet'), line.get('qty')) : OB.DEC.div(OB.DEC.div(gross, line.get('linerate')), line.get('qty')),
            listPrice: OB.DEC.div((grossListPrice || price), line.get('linerate')),
            standardPrice: OB.DEC.div((grossListPrice || price), line.get('linerate')),
            grossListPrice: grossListPrice || price,
            grossUnitPrice: price,
            lineGrossAmount: gross
          }, {
            silent: true
          });
        } else {
          line.set({
            nondiscountedprice: line.get('price'),
            nondiscountednet: line.get('net'),
            standardPrice: line.get('price'),
            net: line.get('discountedNet'),
            pricenet: OB.DEC.toNumber(line.get('discountedNetPrice')),
            listPrice: line.get('priceList'),
            grossListPrice: 0,
            lineGrossAmount: 0
          }, {
            silent: true
          });
          if (!this.get('isQuotation')) {
            line.set('price', 0, {
              silent: true
            });
          }
        }
      }, this);

      var totalnet = this.get('lines').reduce(function (memo, e) {
        var netLine = e.get('discountedNet');
        if (e.get('net')) {
          return memo.add(new BigDecimal(String(e.get('net'))));
        } else {
          return memo.add(new BigDecimal('0'));
        }
      }, new BigDecimal(String(OB.DEC.Zero)));
      totalnet = OB.DEC.toNumber(totalnet);

      this.set('net', totalnet);
    },
    getTotal: function () {
      return this.getGross();
    },
    getNet: function () {
      return this.get('net');
    },

    printTotal: function () {
      return OB.I18N.formatCurrency(this.getTotal());
    },

    getLinesByProduct: function (productId) {
      var affectedLines;
      if (this.get('lines') && this.get('lines').length > 0) {
        affectedLines = _.filter(this.get('lines').models, function (line) {
          return line.get('product').id === productId;
        });
      }
      return affectedLines ? affectedLines : null;
    },

    calculateGross: function () {
      var me = this;
      if (this.get('priceIncludesTax')) {
        this.calculateTaxes(function () {
          var gross = me.get('lines').reduce(function (memo, e) {
            var grossLine = e.getGross();
            if (e.get('promotions')) {
              grossLine = e.get('promotions').reduce(function (memo, e) {
                return OB.DEC.sub(memo, e.actualAmt || e.amt || 0);
              }, grossLine);
            }
            return OB.DEC.add(memo, grossLine);
          }, OB.DEC.Zero);
          me.set('gross', gross);
          me.adjustPayment();
          me.trigger('calculategross');
          me.trigger('saveCurrent');
        });
      } else {
        this.calculateTaxes(function () {
          //If the price doesn't include tax, the discounted gross has already been calculated
          var gross = me.get('lines').reduce(function (memo, e) {
            if (_.isUndefined(e.get('discountedGross'))) {
              return memo;
            }
            var grossLine = e.get('discountedGross');
            if (grossLine) {
              return OB.DEC.add(memo, grossLine);
            } else {
              return memo;
            }
          }, 0);
          me.set('gross', gross);
          var net = me.get('lines').reduce(function (memo, e) {
            var netLine = e.get('discountedNet');
            if (netLine) {
              return OB.DEC.add(memo, netLine);
            } else {
              return memo;
            }
          }, OB.DEC.Zero);
          me.set('net', net);
          me.adjustPayment();
          me.trigger('calculategross');
          me.trigger('saveCurrent');
        });
      }
      //total qty
      var qty = this.get('lines').reduce(function (memo, e) {
        var qtyLine = e.getQty();
        if (qtyLine > 0) {
          return OB.DEC.add(memo, qtyLine, OB.I18N.qtyScale());
        } else {
          return memo;
        }
      }, OB.DEC.Zero);
      this.set('qty', qty);
    },

    getQty: function () {
      return this.get('qty');
    },

    getGross: function () {
      return this.get('gross');
    },

    printGross: function () {
      return OB.I18N.formatCurrency(this.getGross());
    },

    getPayment: function () {
      return this.get('payment');
    },

    getChange: function () {
      return this.get('change');
    },

    getPending: function () {
      return OB.DEC.sub(OB.DEC.abs(this.getTotal()), this.getPayment());
    },
    printPending: function () {
      return OB.I18N.formatCurrency(this.getPending());
    },

    getPaymentStatus: function () {
      var total = OB.DEC.abs(this.getTotal()),
          pay = this.getPayment(),
          isReturn = true;

      _.each(this.get('lines').models, function (line) {
        if (line.get('gross') > 0) {
          isReturn = false;
        }
      }, this);
      return {
        'done': (this.get('lines').length > 0 && OB.DEC.compare(total) >= 0 && OB.DEC.compare(OB.DEC.sub(pay, total)) >= 0),
        'total': OB.I18N.formatCurrency(total),
        'pending': OB.DEC.compare(OB.DEC.sub(pay, total)) >= 0 ? OB.I18N.formatCurrency(OB.DEC.Zero) : OB.I18N.formatCurrency(OB.DEC.sub(total, pay)),
        'change': OB.DEC.compare(this.getChange()) > 0 ? OB.I18N.formatCurrency(this.getChange()) : null,
        'overpayment': OB.DEC.compare(OB.DEC.sub(pay, total)) > 0 ? OB.I18N.formatCurrency(OB.DEC.sub(pay, total)) : null,
        'isReturn': isReturn,
        'isNegative': this.get('gross') < 0 ? true : false,
        'changeAmt': this.getChange(),
        'pendingAmt': OB.DEC.compare(OB.DEC.sub(pay, total)) >= 0 ? OB.DEC.Zero : OB.DEC.sub(total, pay),
        'payments': this.get('payments')
      };
    },

    // returns true if the order is a Layaway, otherwise false
    isLayaway: function () {
      return this.getOrderType() === 2 || this.getOrderType() === 3 || this.get('isLayaway');
    },

    clear: function () {
      this.clearOrderAttributes();
      this.trigger('change');
      this.trigger('clear');
    },

    clearOrderAttributes: function () {
      this.set('id', null);
      this.set('client', null);
      this.set('organization', null);
      this.set('createdBy', null);
      this.set('updatedBy', null);
      this.set('documentType', null);
      this.set('orderType', 0); // 0: Sales order, 1: Return order
      this.set('generateInvoice', false);
      this.set('isQuotation', false);
      this.set('oldId', null);
      this.set('priceList', null);
      this.set('priceIncludesTax', null);
      this.set('currency', null);
      this.set('currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, null);
      this.set('session', null);
      this.set('warehouse', null);
      this.set('salesRepresentative', null);
      this.set('salesRepresentative' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, null);
      this.set('posTerminal', null);
      this.set('posTerminal' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, null);
      this.set('orderDate', new Date());
      this.set('documentnoPrefix', -1);
      this.set('quotationnoPrefix', -1);
      this.set('documentnoSuffix', -1);
      this.set('quotationnoSuffix', -1);
      this.set('documentNo', '');
      this.set('undo', null);
      this.set('bp', null);
      this.set('lines', this.get('lines') ? this.get('lines').reset() : new OrderLineList());
      this.set('payments', this.get('payments') ? this.get('payments').reset() : new PaymentLineList());
      this.set('payment', OB.DEC.Zero);
      this.set('change', OB.DEC.Zero);
      this.set('qty', OB.DEC.Zero);
      this.set('gross', OB.DEC.Zero);
      this.set('net', OB.DEC.Zero);
      this.set('taxes', null);
      this.trigger('calculategross');
      this.set('hasbeenpaid', 'N');
      this.set('isbeingprocessed', 'N');
      this.set('description', '');
      this.set('print', true);
      this.set('sendEmail', false);
      this.set('isPaid', false);
      this.set('paidOnCredit', false);
      this.set('isLayaway', false);
      this.set('isEditable', true);
      this.set('openDrawer', false);
      this.set('totalamount', null);
      this.set('approvals', []);
    },

    clearWith: function (_order) {
      var me = this,
          undf, localSkipApplyPromotions, idExecution;

      // we set first this property to avoid that the apply promotions is triggered
      this.set('isNewReceipt', _order.get('isNewReceipt'));
      //we need this data when IsPaid, IsLayaway changes are triggered
      this.set('documentType', _order.get('documentType'));

      this.set('isPaid', _order.get('isPaid'));
      this.set('paidOnCredit', _order.get('paidOnCredit'));
      this.set('isLayaway', _order.get('isLayaway'));
      if (!_order.get('isEditable')) {
        // keeping it no editable as much as possible, to prevent
        // modifications to trigger editable events incorrectly
        this.set('isEditable', _order.get('isEditable'));
      }

      // the idExecution is saved so only this execution of clearWith will check cloningReceipt to false
      if (OB.UTIL.isNullOrUndefined(this.get('idExecution')) && OB.UTIL.isNullOrUndefined(_order.get('idExecution'))) {
        idExecution = new Date().getTime();
        _order.set('idExecution', idExecution);
        _order.set('cloningReceipt', true);
        this.set('cloningReceipt', true);
        this.set('idExecution', idExecution);
      }

      OB.UTIL.clone(_order, this);

      if (!OB.UTIL.isNullOrUndefined(this.get('idExecution')) && this.get('idExecution') === idExecution) {
        _order.set('cloningReceipt', false);
        this.set('cloningReceipt', false);
        _order.unset('idExecution');
        this.unset('idExecution');
      }

      this.set('isEditable', _order.get('isEditable'));
      this.trigger('calculategross');
      this.trigger('change');
      this.trigger('clear');
    },

    removeUnit: function (line, qty) {
      if (!OB.DEC.isNumber(qty)) {
        qty = OB.DEC.One;
      }
      this.setUnit(line, OB.DEC.sub(line.get('qty'), qty, OB.I18N.qtyScale()), OB.I18N.getLabel('OBPOS_RemoveUnits', [qty, line.get('product').get('_identifier')]));
    },

    addUnit: function (line, qty) {
      if (!OB.DEC.isNumber(qty)) {
        qty = OB.DEC.One;
      }
      this.setUnit(line, OB.DEC.add(line.get('qty'), qty, OB.I18N.qtyScale()), OB.I18N.getLabel('OBPOS_AddUnits', [OB.DEC.toNumber(new BigDecimal((String)(qty.toString()))), line.get('product').get('_identifier')]));
    },

    setUnit: function (line, qty, text, doNotSave) {

      if (OB.DEC.isNumber(qty) && qty !== 0) {
        var oldqty = line.get('qty');
        if (line.get('product').get('groupProduct') === false) {
          this.addProduct(line.get('product'));
          return true;
        } else {
          var me = this;
          // sets the new quantity
          line.set('qty', qty);
          line.calculateGross();
          // sets the undo action
          this.set('undo', {
            text: text || OB.I18N.getLabel('OBPOS_SetUnits', [line.get('qty'), line.get('product').get('_identifier')]),
            oldqty: oldqty,
            line: line,
            undo: function () {
              line.set('qty', oldqty);
              line.calculateGross();
              me.set('undo', null);
            }
          });
        }
        this.adjustPayment();
        if (!doNotSave) {
          this.save();
        }
      } else {
        this.deleteLine(line);
      }
    },

    setPrice: function (line, price, options) {
      options = options || {};
      options.setUndo = (_.isUndefined(options.setUndo) || _.isNull(options.setUndo) || options.setUndo !== false) ? true : options.setUndo;

      if (!OB.UTIL.isNullOrUndefined(line.get('originalOrderLineId'))) {
        OB.UTIL.showError(OB.I18N.getLabel('OBPOS_CannotChangePrice'));
      } else if (OB.DEC.isNumber(price)) {
        var oldprice = line.get('price');
        if (OB.DEC.compare(price) >= 0) {
          var me = this;
          // sets the new price
          line.set('price', price);
          line.calculateGross();
          // sets the undo action
          if (options.setUndo) {
            this.set('undo', {
              text: OB.I18N.getLabel('OBPOS_SetPrice', [line.printPrice(), line.get('product').get('_identifier')]),
              oldprice: oldprice,
              line: line,
              undo: function () {
                line.set('price', oldprice);
                line.calculateGross();
                me.set('undo', null);
              }
            });
          }
        }
        this.adjustPayment();
      }
      this.save();
    },

    setLineProperty: function (line, property, value) {
      var me = this;
      var index = this.get('lines').indexOf(line);
      this.get('lines').at(index).set(property, value);
    },

    deleteLine: function (line, doNotSave) {
      var me = this;
      var index = this.get('lines').indexOf(line);
      var pack = line.isAffectedByPack(),
          productId = line.get('product').id;

      if (pack) {
        // When deleting a line, check lines with other product that are affected by
        // same pack than deleted one and merge splitted lines created for those
        this.get('lines').forEach(function (l) {
          var affected;
          if (productId === l.get('product').id) {
            return; //continue
          }
          affected = l.isAffectedByPack();
          if (affected && affected.ruleId === pack.ruleId) {
            this.mergeLines(l);
          }
        }, this);
      }

      // trigger
      line.trigger('removed', line);

      // remove the line
      this.get('lines').remove(line);
      // set the undo action
      this.set('undo', {
        text: OB.I18N.getLabel('OBPOS_DeleteLine', [line.get('qty'), line.get('product').get('_identifier')]),
        line: line,
        undo: function () {
          me.get('lines').add(line, {
            at: index
          });
          me.calculateGross();
          me.set('undo', null);
        }
      });
      this.adjustPayment();
      if (!doNotSave) {
        this.save();
        this.calculateGross();
      }
    },
    //Attrs is an object of attributes that will be set in order
    _addProduct: function (p, qty, options, attrs) {
      var me = this;
      if (enyo.Panels.isScreenNarrow()) {
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBPOS_AddLine', [qty ? qty : 1, p.get('_identifier')]));
      }
      if (p.get('ispack')) {
        OB.Model.Discounts.discountRules[p.get('productCategory')].addProductToOrder(this, p);
        return;
      }
      if (this.get('orderType') === 1) {
        qty = qty ? -qty : -1;
      } else {
        qty = qty || 1;
      }
      if (this.get('isQuotation') && this.get('hasbeenpaid') === 'Y') {
        OB.UTIL.showError(OB.I18N.getLabel('OBPOS_QuotationClosed'));
        return;
      }
      if (p.get('obposScale')) {
        OB.POS.hwserver.getWeight(function (data) {
          if (data.exception) {
            alert(data.exception.message);
          } else if (data.result === 0) {
            alert(OB.I18N.getLabel('OBPOS_WeightZero'));
          } else {
            me.createLine(p, data.result, options, attrs);
          }
        });
      } else {
        if (p.get('groupProduct') || (options && options.packId)) {
          var affectedByPack, line;
          if (options && options.line) {
            line = options.line;
          } else {
            line = this.get('lines').find(function (l) {
              if (l.get('product').id === p.id && l.get('qty') > 0) {
                affectedByPack = l.isAffectedByPack();
                if (!affectedByPack) {
                  return true;
                } else if ((options && options.packId === affectedByPack.ruleId) || !(options && options.packId)) {
                  return true;
                }
              }
            });
          }
          OB.MobileApp.model.hookManager.executeHooks('OBPOS_GroupedProductPreCreateLine', {
            receipt: this,
            line: line,
            allLines: this.get('lines'),
            p: p,
            qty: qty,
            options: options,
            attrs: attrs
          }, function (args) {
            if (args && args.cancelOperation) {
              return;
            }
            if (args.line) {
              args.receipt.addUnit(args.line, args.qty);
              args.line.trigger('selected', args.line);
            } else {
              args.receipt.createLine(args.p, args.qty, args.options, args.attrs);
            }
          });

        } else {
          //remove line even it is a grouped line
          if (options && options.line && qty === -1) {
            this.addUnit(options.line, qty);
          } else {
            this.createLine(p, qty, options, attrs);
          }
        }
      }
      this.save();
    },

    _drawLinesDistribution: function (data) {
      if (data && data.linesToModify && data.linesToModify.length > 0) {
        _.each(data.linesToModify, function (lineToChange) {
          var line = this.get('lines').getByCid(lineToChange.lineCid);
          var unitsToAdd = lineToChange.newQty - line.get('qty');
          if (unitsToAdd > 0) {
            this.addUnit(line, unitsToAdd);
          } else if (unitsToAdd < 0) {
            this.removeUnit(line, -unitsToAdd);
          }
          this.setPrice(line, lineToChange.newPrice, {
            setUndo: false
          });
          _.each(lineToChange.productProperties, function (propToSet) {
            line.get('product').set(propToSet.name, propToSet.value);
          });
          _.each(lineToChange.lineProperties, function (propToSet) {
            line.set(propToSet.name, propToSet.value);
          });
        }, this);
      }
      if (data && data.linesToRemove && data.linesToRemove.length > 0) {
        _.each(data.linesToRemove, function (lineCidToRemove) {
          var line = this.get('lines').getByCid(lineCidToRemove);
          this.deleteLine(line);
        }, this);
      }
      if (data && data.linesToAdd && data.linesToAdd.length > 0) {
        _.each(data.linesToAdd, function (lineToAdd) {
          this.createLine(lineToAdd.product, lineToAdd.qtyToAdd);
        }, this);
      }
    },
    //Attrs is an object of attributes that will be set in order
    addProduct: function (p, qty, options, attrs) {
      OB.debug('_addProduct');
      var me = this;
      OB.MobileApp.model.hookManager.executeHooks('OBPOS_AddProductToOrder', {
        receipt: this,
        productToAdd: p,
        qtyToAdd: qty,
        options: options
      }, function (args) {
        if (args && args.useLines) {
          me._drawLinesDistribution(args);
        } else {
          me._addProduct(p, qty, options, attrs);
        }
      });
    },

    /**
     * Splits a line from the ticket keeping in the line the qtyToKeep quantity,
     * the rest is moved to another line with the same product and no packs, or
     * to a new one if there's no other line. In case a new is created it is returned.
     */
    splitLine: function (line, qtyToKeep) {
      var originalQty = line.get('qty'),
          newLine, p, qtyToMove;

      if (originalQty === qtyToKeep) {
        return;
      }

      qtyToMove = originalQty - qtyToKeep;

      this.setUnit(line, qtyToKeep, null, true);

      p = line.get('product');

      newLine = this.get('lines').find(function (l) {
        return l !== line && l.get('product').id === p.id && !l.isAffectedByPack();
      });

      if (!newLine) {
        newLine = line.clone();
        newLine.set({
          promotions: null,
          addedBySplit: true
        });
        this.get('lines').add(newLine);
        this.setUnit(newLine, qtyToMove, null, true);
        return newLine;
      } else {
        this.setUnit(newLine, newLine.get('qty') + qtyToMove, null, true);
      }
    },

    /**
     * Checks other lines with the same product to be merged in a single one
     */
    mergeLines: function (line) {
      var p = line.get('product'),
          lines = this.get('lines'),
          merged = false;
      line.set('promotions', null);
      lines.forEach(function (l) {
        var promos = l.get('promotions');
        if (l === line) {
          return;
        }

        if (l.get('product').id === p.id && l.get('price') === line.get('price')) {
          line.set({
            qty: line.get('qty') + l.get('qty'),
            promotions: null
          });
          lines.remove(l);
          merged = true;
        }
      }, this);
      if (merged) {
        line.calculateGross();
      }
    },

    /**
     *  It looks for different lines for same product with exactly the same promotions
     *  to merge them in a single line
     */
    mergeLinesWithSamePromotions: function () {
      var lines = this.get('lines'),
          l, line, i, j, k, p, otherLine, toRemove = [],
          matches, otherPromos, found, compareRule;

      compareRule = function (p) {
        var basep = line.get('promotions')[k];
        return p.ruleId === basep.ruleId && ((!p.family && !basep.family) || (p.family && basep.family && p.family === basep.family));
      };

      for (i = 0; i < lines.length; i++) {
        line = lines.at(i);
        for (j = i + 1; j < lines.length; j++) {
          otherLine = lines.at(j);
          if (otherLine.get('product').id !== line.get('product').id) {
            continue;
          }

          if ((!line.get('promotions') || line.get('promotions').length === 0) && (!otherLine.get('promotions') || otherLine.get('promotions').length === 0)) {
            line.set('qty', line.get('qty') + otherLine.get('qty'));
            line.calculateGross();
            toRemove.push(otherLine);
          } else if (line.get('promotions') && otherLine.get('promotions') && line.get('promotions').length === otherLine.get('promotions').length && line.get('price') === otherLine.get('price')) {
            matches = true;
            otherPromos = otherLine.get('promotions');
            for (k = 0; k < line.get('promotions').length; k++) {
              found = _.find(otherPromos, compareRule);
              if (!found) {
                matches = false;
                break;
              }
            }
            if (matches) {
              line.set('qty', line.get('qty') + otherLine.get('qty'));
              for (k = 0; k < line.get('promotions').length; k++) {
                found = _.find(otherPromos, compareRule);
                line.get('promotions')[k].amt += found.amt;
                line.get('promotions')[k].displayedTotalAmount += found.displayedTotalAmount;
              }
              toRemove.push(otherLine);
              line.calculateGross();
            }
          }
        }
      }

      _.forEach(toRemove, function (l) {
        lines.remove(l);
      });
    },

    addPromotion: function (line, rule, discount) {
      var promotions = line.get('promotions') || [],
          disc = {},
          i, replaced = false;
      disc.name = discount.name || rule.get('printName') || rule.get('name');
      disc.ruleId = rule.id || rule.get('ruleId');
      disc.amt = discount.amt;
      disc.fullAmt = discount.amt ? discount.amt : 0;
      disc.actualAmt = discount.actualAmt;
      disc.pack = discount.pack;
      disc.discountType = rule.get('discountType');
      disc.priority = rule.get('priority');
      disc.manual = discount.manual;
      disc.userAmt = discount.userAmt;
      disc.lastApplied = discount.lastApplied;
      disc.obdiscQtyoffer = rule.get('qtyOffer') ? OB.DEC.toNumber(rule.get('qtyOffer')) : line.get('qty');
      disc.qtyOffer = disc.obdiscQtyoffer;
      disc.doNotMerge = discount.doNotMerge;

      disc.hidden = discount.hidden === true || (discount.actualAmt && !disc.amt);

      if (OB.UTIL.isNullOrUndefined(discount.actualAmt) && !disc.amt && disc.pack) {
        disc.hidden = true;
      }

      if (disc.hidden) {
        disc.displayedTotalAmount = 0;
      } else {
        disc.displayedTotalAmount = disc.amt || discount.actualAmt;
      }

      if (discount.percentage) {
        disc.percentage = discount.percentage;
      }

      if (discount.family) {
        disc.family = discount.family;
      }

      if (typeof discount.applyNext !== 'undefined') {
        disc.applyNext = discount.applyNext;
      } else {
        disc.applyNext = rule.get('applyNext');
      }
      if (!disc.applyNext) {
        disc.qtyOfferReserved = disc.obdiscQtyoffer;
      } else {
        disc.qtyOfferReserved = 0;
      }
      disc._idx = discount._idx || rule.get('_idx');

      for (i = 0; i < promotions.length; i++) {
        if (disc._idx !== -1 && disc._idx < promotions[i]._idx) {
          // Trying to apply promotions in incorrect order: recalculate whole line again
          if (OB.POS.modelterminal.hasPermission('OBPOS_discount.newFlow', true)) {
            OB.Model.Discounts.applyPromotionsImp(this, line, true);
          } else {
            OB.Model.Discounts.applyPromotionsImp(this, line, false);
          }
          return;
        }
      }

      for (i = 0; i < promotions.length; i++) {
        if (promotions[i].ruleId === rule.id) {
          promotions[i] = disc;
          replaced = true;
          break;
        }
      }

      if (!replaced) {
        promotions.push(disc);
      }

      line.set('promotions', promotions);
      line.trigger('change');
    },

    removePromotion: function (line, rule) {
      var promotions = line.get('promotions'),
          ruleId = rule.id,
          removed = false,
          res = [],
          i;
      if (!promotions) {
        return;
      }

      for (i = 0; i < promotions.length; i++) {
        if (promotions[i].ruleId === rule.id) {
          removed = true;
        } else {
          res.push(promotions[i]);
        }
      }

      if (removed) {
        line.set('promotions', res);
        line.trigger('change');
        this.save();

        // Recalculate promotions for all lines affected by this same rule,
        // because this rule could have prevented other ones to be applied
        this.get('lines').forEach(function (ln) {
          if (ln.get('promotionCandidates')) {
            ln.get('promotionCandidates').forEach(function (candidateRule) {
              if (candidateRule === ruleId) {
                OB.Model.Discounts.applyPromotions(this, line);
              }
            }, this);
          }
        }, this);
      }
    },
    //Attrs is an object of attributes that will be set in order line
    createLine: function (p, units, options, attrs) {
      var me = this;
      if (OB.POS.modelterminal.get('permissions').OBPOS_NotAllowSalesWithReturn) {
        var negativeLines = _.filter(this.get('lines').models, function (line) {
          return line.get('qty') < 0;
        }).length;
        if (this.get('lines').length > 0) {
          if (units > 0 && negativeLines > 0) {
            OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgCannotAddPositive'));
            return;
          } else if (units < 0 && negativeLines !== this.get('lines').length) {
            OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgCannotAddNegative'));
            return;
          }
        }
      }
      var newline = new OrderLine({
        product: p,
        uOM: p.get('uOM'),
        qty: OB.DEC.number(units),
        price: OB.DEC.number(p.get('standardPrice')),
        priceList: OB.DEC.number(p.get('listPrice')),
        priceIncludesTax: this.get('priceIncludesTax'),
        warehouse: {
          id: OB.POS.modelterminal.get('warehouses')[0].warehouseid,
          warehousename: OB.POS.modelterminal.get('warehouses')[0].warehousename
        }
      });
      if (!_.isUndefined(attrs)) {
        _.each(_.keys(attrs), function (key) {
          newline.set(key, attrs[key]);
        });
      }

      newline.calculateGross();

      //issue 25655: ungroup feature is just needed when the line is created. Then lines work as grouped lines.
      newline.get('product').set("groupProduct", true);

      //issue 25448: Show stock screen is just shown when a new line is created.
      if (newline.get('product').get("showstock") === true) {
        newline.get('product').set("showstock", false);
        newline.get('product').set("_showstock", true);
      }

      // add the created line
      this.get('lines').add(newline, options);
      newline.trigger('created', newline);
      // set the undo action
      this.set('undo', {
        text: OB.I18N.getLabel('OBPOS_AddLine', [newline.get('qty'), newline.get('product').get('_identifier')]),
        line: newline,
        undo: function () {
          me.get('lines').remove(newline);
          me.set('undo', null);
        }
      });
      this.adjustPayment();
      return newline;
    },
    returnLine: function (line, options, skipValidaton) {
      var me = this;
      if (OB.POS.modelterminal.get('permissions').OBPOS_NotAllowSalesWithReturn && !skipValidaton) {
        //The value of qty need to be negate because we want to change it
        var negativeLines = _.filter(this.get('lines').models, function (line) {
          return line.get('qty') < 0;
        }).length;
        if (this.get('lines').length > 0) {
          if (-line.get('qty') > 0 && negativeLines > 0) {
            OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgCannotAddPositive'));
            return;
          } else if (-line.get('qty') < 0 && negativeLines !== this.get('lines').length) {
            OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgCannotAddNegative'));
            return;
          }
        }
      }
      if (line.get('qty') > 0) {
        line.get('product').set('ignorePromotions', true);
      } else {
        line.get('product').set('ignorePromotions', false);
      }
      line.set('qty', -line.get('qty'));
      line.calculateGross();

      // set the undo action
      this.set('undo', {
        text: OB.I18N.getLabel('OBPOS_ReturnLine', [line.get('product').get('_identifier')]),
        line: line,
        undo: function () {
          line.set('qty', -line.get('qty'));
          me.set('undo', null);
        }
      });
      this.adjustPayment();
      if (line.get('promotions')) {
        line.unset('promotions');
      }
      me.calculateGross();
      this.save();

    },
    setBPandBPLoc: function (businessPartner, showNotif, saveChange) {
      var me = this,
          undef;
      var oldbp = this.get('bp');
      this.set('bp', businessPartner);
      // set the undo action
      if (showNotif === undef || showNotif === true) {
        this.set('undo', {
          text: businessPartner ? OB.I18N.getLabel('OBPOS_SetBP', [businessPartner.get('_identifier')]) : OB.I18N.getLabel('OBPOS_ResetBP'),
          bp: businessPartner,
          undo: function () {
            me.set('bp', oldbp);
            me.set('undo', null);
          }
        });
      }
      if (saveChange) {
        this.save();
      }
    },

    setOrderType: function (permission, orderType, options) {
      var me = this;
      if (orderType === OB.DEC.One) {
        this.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentTypeForReturns);
        _.each(this.get('lines').models, function (line) {
          if (line.get('qty') > 0) {
            me.returnLine(line, null, true);
          }
        }, this);
      } else {
        this.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentType);
      }
      this.set('orderType', orderType); // 0: Sales order, 1: Return order, 2: Layaway, 3: Void Layaway
      if (orderType !== 3) { //Void this Layaway, do not need to save
        if (!(options && !OB.UTIL.isNullOrUndefined(options.saveOrder) && options.saveOrder === false)) {
          this.save();
        }
      } else {
        this.set('layawayGross', this.getGross());
        this.set('gross', this.get('payment'));
        this.set('payment', OB.DEC.Zero);
        this.get('payments').reset();
      }
      // remove promotions
      if (!(options && !OB.UTIL.isNullOrUndefined(options.applyPromotions) && options.applyPromotions === false)) {
        OB.Model.Discounts.applyPromotions(this);
      }
    },

    // returns the ordertype: 0: Sales order, 1: Return order, 2: Layaway, 3: Void Layaway
    getOrderType: function () {
      return this.get('orderType');
    },

    shouldApplyPromotions: function () {
      // Do not apply promotions in return tickets
      return this.get('orderType') !== 1;
    },

    hasOneLineToIgnoreDiscounts: function () {
      return _.some(this.get('lines').models, function (line) {
        return line.get('product').get('ignorePromotions');
      });
    },

    setOrderInvoice: function () {
      if (OB.POS.modelterminal.hasPermission('OBPOS_receipt.invoice')) {
        this.set('generateInvoice', true);
        this.save();
      }
    },

    updatePrices: function (callback) {
      var order = this,
          newAllLinesCalculated;

      function allLinesCalculated() {
        callback(order);
      }

      newAllLinesCalculated = _.after(this.get('lines').length, allLinesCalculated);

      this.get('lines').each(function (line) {

        //remove promotions
        line.unset('promotions');

        var successCallbackPrices, criteria = {
          'id': line.get('product').get('id')
        };
        successCallbackPrices = function (dataPrices, line) {
          dataPrices.each(function (price) {
            order.setPrice(line, price.get('standardPrice', {
              setUndo: false
            }));
          });
          newAllLinesCalculated();
        };

        OB.Dal.find(OB.Model.Product, criteria, successCallbackPrices, function () {
          // TODO: Report error properly.
        }, line);
      });
    },

    createQuotation: function () {
      if (OB.POS.modelterminal.hasPermission('OBPOS_receipt.quotation')) {
        this.set('isQuotation', true);
        this.set('generateInvoice', false);
        this.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentTypeForQuotations);
        this.save();
      }
    },

    createOrderFromQuotation: function (updatePrices) {
      var documentseq, documentseqstr;

      this.get('lines').each(function (line) {
        //issue 25055 -> If we don't do the following prices and taxes are calculated
        //wrongly because the calculation starts with discountedNet instead of
        //the real net.
        //It only happens if the order is created from quotation just after save the quotation
        //(without load the quotation from quotations window)
        if (!this.get('priceIncludesTax')) {
          line.set('net', line.get('nondiscountednet'));
        }

        //issues 24994 & 24993
        //if the order is created from quotation just after save the quotation
        //(without load the quotation from quotations window). The order has the fields added
        //by adjust prices. We need to work without these values
        //price not including taxes
        line.unset('nondiscountedprice');
        line.unset('nondiscountednet');
        //price including taxes
        line.unset('netFull');
        line.unset('grossListPrice');
        line.unset('grossUnitPrice');
        line.unset('lineGrossAmount');
      }, this);

      this.set('id', null);
      this.set('isQuotation', false);
      this.set('generateInvoice', OB.POS.modelterminal.get('terminal').terminalType.generateInvoice);
      this.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentType);
      this.set('createdBy', OB.POS.modelterminal.get('orgUserId'));
      this.set('salesRepresentative', OB.POS.modelterminal.get('context').user.id);
      this.set('hasbeenpaid', 'N');
      this.set('isPaid', false);
      this.set('isEditable', true);
      this.set('orderDate', new Date());
      var nextDocumentno = OB.MobileApp.model.getNextDocumentno();
      this.set('documentnoPrefix', OB.MobileApp.model.get('terminal').docNoPrefix);
      this.set('documentnoSuffix', nextDocumentno.documentnoSuffix);
      this.set('documentNo', nextDocumentno.documentNo);
      this.set('posTerminal', OB.POS.modelterminal.get('terminal').id);
      this.save();
      if (updatePrices) {
        this.updatePrices(function (order) {
          OB.Model.Discounts.applyPromotions(order);
          OB.UTIL.showSuccess(OB.I18N.getLabel('OBPOS_QuotationCreatedOrder'));
          order.trigger('orderCreatedFromQuotation');
        });
      } else {
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBPOS_QuotationCreatedOrder'));
        this.calculateGross();
        this.trigger('orderCreatedFromQuotation');
      }
    },

    reactivateQuotation: function () {
      this.set('hasbeenpaid', 'N');
      this.set('isEditable', true);
      this.set('createdBy', OB.POS.modelterminal.get('orgUserId'));
      this.set('oldId', this.get('id'));
      this.set('id', null);
      this.save();
    },

    rejectQuotation: function () {
      alert('reject!!');
    },

    resetOrderInvoice: function () {
      if (OB.POS.modelterminal.hasPermission('OBPOS_receipt.invoice')) {
        this.set('generateInvoice', false);
        this.save();
      }
    },
    adjustPayment: function () {
      var i, max, p;
      var payments = this.get('payments');
      var total = OB.DEC.abs(this.getTotal());

      var nocash = OB.DEC.Zero;
      var cash = OB.DEC.Zero;
      var origCash = OB.DEC.Zero;
      var auxCash = OB.DEC.Zero;
      var prevCash = OB.DEC.Zero;
      var paidCash = OB.DEC.Zero;
      var pcash;

      for (i = 0, max = payments.length; i < max; i++) {
        p = payments.at(i);
        if (p.get('rate') && p.get('rate') !== '1') {
          p.set('origAmount', OB.DEC.mul(p.get('amount'), p.get('rate')));
        } else {
          p.set('origAmount', p.get('amount'));
        }
        p.set('paid', p.get('origAmount'));
        if (p.get('kind') === OB.POS.modelterminal.get('paymentcash')) {
          // The default cash method
          cash = OB.DEC.add(cash, p.get('origAmount'));
          pcash = p;
          paidCash = OB.DEC.add(paidCash, p.get('origAmount'));
        } else if (OB.POS.modelterminal.hasPayment(p.get('kind')) && OB.POS.modelterminal.hasPayment(p.get('kind')).paymentMethod.iscash) {
          // Another cash method
          origCash = OB.DEC.add(origCash, p.get('origAmount'));
          pcash = p;
          paidCash = OB.DEC.add(paidCash, p.get('origAmount'));
        } else {
          nocash = OB.DEC.add(nocash, p.get('origAmount'));
        }
      }

      // Calculation of the change....
      //FIXME
      if (pcash) {
        if (pcash.get('kind') !== OB.POS.modelterminal.get('paymentcash')) {
          auxCash = origCash;
          prevCash = cash;
        } else {
          auxCash = cash;
          prevCash = origCash;
        }
        if (OB.DEC.compare(nocash - total) > 0) {
          pcash.set('paid', OB.DEC.Zero);
          this.set('payment', nocash);
          this.set('change', OB.DEC.add(cash, origCash));
        } else if (OB.DEC.compare(OB.DEC.sub(OB.DEC.add(OB.DEC.add(nocash, cash), origCash), total)) > 0) {
          pcash.set('paid', OB.DEC.sub(total, OB.DEC.add(nocash, OB.DEC.sub(paidCash, pcash.get('origAmount')))));
          this.set('payment', total);
          //The change value will be computed through a rounded total value, to ensure that the total plus change
          //add up to the paid amount without any kind of precission loss
          this.set('change', OB.DEC.sub(OB.DEC.add(OB.DEC.add(nocash, cash), origCash), OB.Utilities.Number.roundJSNumber(total, 2)));
        } else {
          pcash.set('paid', auxCash);
          this.set('payment', OB.DEC.add(OB.DEC.add(nocash, cash), origCash));
          this.set('change', OB.DEC.Zero);
        }
      } else {
        if (payments.length > 0) {
          if (this.get('payment') === 0 || nocash > 0) {
            this.set('payment', nocash);
          }
        } else {
          this.set('payment', OB.DEC.Zero);
        }
        this.set('change', OB.DEC.Zero);
      }
    },

    addPayment: function (payment) {
      var payments, total;
      var i, max, p, order;

      if (!OB.DEC.isNumber(payment.get('amount'))) {
        alert(OB.I18N.getLabel('OBPOS_MsgPaymentAmountError'));
        return;
      }

      payments = this.get('payments');
      total = OB.DEC.abs(this.getTotal());
      order = this;
      OB.MobileApp.model.hookManager.executeHooks('OBPOS_preAddPayment', {
        paymentToAdd: payment,
        payments: payments,
        receipt: this
      }, function () {
        if (!payment.get('paymentData')) {
          // search for an existing payment only if there is not paymentData info.
          // this avoids to merge for example card payments of different cards.
          for (i = 0, max = payments.length; i < max; i++) {
            p = payments.at(i);
            if (p.get('kind') === payment.get('kind') && !p.get('isPrePayment')) {
              p.set('amount', OB.DEC.add(payment.get('amount'), p.get('amount')));
              if (p.get('rate') && p.get('rate') !== '1') {
                p.set('origAmount', OB.DEC.add(payment.get('origAmount'), OB.DEC.mul(p.get('origAmount'), p.get('rate'))));
              }
              order.adjustPayment();
              order.trigger('displayTotal');
              return;
            }
          }
        }
        if (payment.get('openDrawer') && (payment.get('allowOpenDrawer') || payment.get('isCash'))) {
          order.set('openDrawer', payment.get('openDrawer'));
        }
        payment.set('date', new Date());
        payments.add(payment);
        order.adjustPayment();
        order.trigger('displayTotal');
      }); // call with callback, no args
    },

    overpaymentExists: function () {
      return this.getPaymentStatus().overpayment ? true : false;
    },

    removePayment: function (payment) {
      var payments = this.get('payments');
      payments.remove(payment);
      if (payment.get('openDrawer')) {
        this.set('openDrawer', false);
      }
      this.adjustPayment();
      this.save();
    },

    serializeToJSON: function () {
      // this.toJSON() generates a collection instance for members like "lines"
      // We need a plain array object
      var jsonorder = JSON.parse(JSON.stringify(this.toJSON()));

      // remove not needed members
      delete jsonorder.undo;
      delete jsonorder.json;

      _.forEach(jsonorder.lines, function (item) {
        delete item.product.img;
      });

      return jsonorder;
    },

    changeSignToShowReturns: function () {
      this.set('change', OB.DEC.mul(this.get('change'), -1));
      this.set('gross', OB.DEC.mul(this.get('gross'), -1));
      this.set('net', OB.DEC.mul(this.get('net'), -1));
      this.set('qty', OB.DEC.mul(this.get('qty'), -1));
      //lines
      _.each(this.get('lines').models, function (line) {
        line.set('gross', OB.DEC.mul(line.get('gross'), -1));
        line.set('qty', OB.DEC.mul(line.get('qty'), -1));
      }, this);

      //payments
      _.each(this.get('payments').models, function (payment) {
        payment.set('amount', OB.DEC.mul(payment.get('amount'), -1));
        payment.set('origAmount', OB.DEC.mul(payment.get('origAmount'), -1));
      }, this);

      //taxes
      _.each(this.get('taxes'), function (tax) {
        tax.amount = OB.DEC.mul(tax.amount, -1);
        tax.gross = OB.DEC.mul(tax.gross, -1);
        tax.net = OB.DEC.mul(tax.net, -1);
      }, this);

    },

    setProperty: function (_property, _value) {
      this.set(_property, _value);
      this.save();
    },

    hasIntegrity: function () {
      // checks if the sum of the amount of every line is the same as the total gross
      var gross = this.attributes.gross;
      var grossOfSummedLines = 0;
      var countOfLines = 1;
      _.each(this.get('lines').models, function (line) {
        var lineGross = line.attributes.gross;
        grossOfSummedLines = OB.DEC.add(grossOfSummedLines, lineGross);
        countOfLines += 1;
      }, this);
      // allow up to 2 cents of deviation per line
      if (OB.DEC.abs(gross - grossOfSummedLines) <= (0.01 * countOfLines)) {
        return true;
      }
      OB.error("Receipt " + this.attributes.documentNo + " failed in the integrity test; gross: " + gross + " <> lineGross: " + grossOfSummedLines);
      return false;
    },

    groupLinesByProduct: function () {
      var me = this,
          lineToMerge, lines = this.get('lines'),
          auxLines = lines.models.slice(0),
          localSkipApplyPromotions;

      localSkipApplyPromotions = this.get('skipApplyPromotions');
      this.set('skipApplyPromotions', true);
      _.each(auxLines, function (l) {
        lineToMerge = _.find(lines.models, function (line) {
          if (l !== line && l.get('product').id === line.get('product').id && l.get('price') === line.get('price') && line.get('qty') > 0 && l.get('qty') > 0 && !_.find(line.get('promotions'), function (promo) {
            return promo.manual;
          }) && !_.find(l.get('promotions'), function (promo) {
            return promo.manual;
          })) {
            return line;
          }
        });
        if (lineToMerge) {
          lineToMerge.set({
            qty: lineToMerge.get('qty') + l.get('qty')
          }, {
            silent: true
          });
          lines.remove(l);
        }
      });
      this.set('skipApplyPromotions', localSkipApplyPromotions);
    },
    fillPromotionsWith: function (groupedOrder, isFirstTime) {
      var me = this,
          copiedPromo, pendingQtyOffer, undf, linesToMerge, auxPromo, idx, actProm, linesToCreate = [],
          qtyToReduce, lineToEdit, lineProm, linesToReduce, linesCreated = false,
          localSkipApplyPromotions;

      localSkipApplyPromotions = this.get('skipApplyPromotions');
      this.set('skipApplyPromotions', true);
      //reset pendingQtyOffer value of each promotion
      groupedOrder.get('lines').forEach(function (l) {
        _.each(l.get('promotions'), function (promo) {
          promo.pendingQtyOffer = promo.qtyOffer;
        });
        //copy lines from virtual ticket to original ticket when they have promotions which avoid us to merge lines
        if (_.find(l.get('promotions'), function (promo) {
          return promo.doNotMerge;
        })) {
          //First, try to find lines with the same qty
          lineToEdit = _.find(me.get('lines').models, function (line) {
            if (l !== line && l.get('product').id === line.get('product').id && l.get('price') === line.get('price') && line.get('qty') === l.get('qty') && !_.find(line.get('promotions'), function (promo) {
              return promo.manual;
            }) && !_.find(line.get('promotions'), function (promo) {
              return promo.doNotMerge;
            })) {
              return line;
            }
          });
          //if we cannot find lines with same qty, find lines with qty > 0
          if (!lineToEdit) {
            lineToEdit = _.find(me.get('lines').models, function (line) {
              if (l !== line && l.get('product').id === line.get('product').id && l.get('price') === line.get('price') && line.get('qty') > 0 && !_.find(line.get('promotions'), function (promo) {
                return promo.manual;
              })) {
                return line;
              }
            });
          }
          //if promotion affects only to few quantities of the line, create a new line with quantities not affected by the promotion
          if (lineToEdit.get('qty') > l.get('qty')) {
            linesToCreate.push({
              product: lineToEdit.get('product'),
              qty: l.get('qty'),
              attrs: {
                promotions: l.get('promotions'),
                promotionCandidates: l.get('promotionCandidates'),
                qtyToApplyDiscount: l.get('qtyToApplyDiscount')
              }
            });
            lineToEdit.set('qty', OB.DEC.sub(lineToEdit.get('qty'), l.get('qty')), {
              silent: true
            });
            //if promotion affects to several lines, edit first line with the promotion info and then remove the affected lines
          } else if (lineToEdit.get('qty') < l.get('qty')) {
            qtyToReduce = OB.DEC.sub(l.get('qty'), lineToEdit.get('qty'));
            linesToReduce = _.filter(me.get('lines').models, function (line) {
              if (l !== line && l.get('product').id === line.get('product').id && l.get('price') === line.get('price') && line.get('qty') > 0 && !_.find(line.get('promotions'), function (promo) {
                return promo.manual || promo.doNotMerge;
              })) {
                return line;
              }
            });
            lineProm = linesToReduce.shift();
            lineProm.set('qty', l.get('qty'));
            lineProm.set('promotions', l.get('promotions'));
            lineProm.set('promotionCandidates', l.get('promotionCandidates'));
            lineProm.set('qtyToApplyDiscount', l.get('qtyToApplyDiscount'));
            lineProm.trigger('change');
            _.each(linesToReduce, function (line) {
              if (line.get('qty') > qtyToReduce) {
                line.set({
                  qty: line.get('qty') - qtyToReduce
                }, {
                  silent: true
                });
                qtyToReduce = OB.DEC.Zero;
              } else if (line.get('qty') === qtyToReduce) {
                me.get('lines').remove(line);
                qtyToReduce = OB.DEC.Zero;
              } else {
                qtyToReduce = qtyToReduce - line.get('qty');
                me.get('lines').remove(line);
              }
            });
            //when qty of the promotion is equal to the line qty, we copy line info.
          } else {
            lineToEdit.set('qty', l.get('qty'));
            lineToEdit.set('promotions', l.get('promotions'));
            lineToEdit.set('promotionCandidates', l.get('promotionCandidates'));
            lineToEdit.set('qtyToApplyDiscount', l.get('qtyToApplyDiscount'));
            lineToEdit.trigger('change');
          }
        } else {
          //Filter lines which can be merged
          linesToMerge = _.filter(me.get('lines').models, function (line) {
            var qtyReserved = 0;
            var promotions = line.get('promotions') || [];
            if (promotions.length > 0) {
              promotions.forEach(function (p) {
                qtyReserved = OB.DEC.add(qtyReserved, p.qtyOfferReserved || 0);
              });
            }
            if (l !== line && l.get('product').id === line.get('product').id && l.get('price') === line.get('price') && OB.UTIL.Math.sign(line.get('qty')) === OB.UTIL.Math.sign(l.get('qty'))) {
              if (OB.DEC.sub(Math.abs(line.get('qty')), qtyReserved) > 0) {
                var isManualOrNotMerge = _.find(line.get('promotions'), function (promo) {
                  return promo.manual || promo.doNotMerge;
                });
                if (!isManualOrNotMerge) {
                  return line;
                }
              }
            }
          });
          // sort by qty asc to fix issue 28120
          // firstly the discount is applied to the lines with minus quantity, so the discount is applied to all quantity of the line
          // and if it is needed (promotion.qty > line.qty) the rest of promotion will be applied to the other line
          linesToMerge = _.sortBy(linesToMerge, function (lsb) {
            lsb.getQty();
          });
          if (linesToMerge.length > 0) {
            _.each(linesToMerge, function (line) {
              line.set('promotionCandidates', l.get('promotionCandidates'));
              line.set('promotionMessages', me.showMessagesPromotions(line.get('promotionMessages'), l.get('promotionMessages')));
              line.set('qtyToApplyDiscount', l.get('qtyToApplyDiscount'));
              _.each(l.get('promotions'), function (promo) {
                copiedPromo = JSON.parse(JSON.stringify(promo));
                //when ditributing the promotion between different lines, we save accumulated amount
                promo.distributedAmt = promo.distributedAmt ? promo.distributedAmt : OB.DEC.Zero;
                //pendingQtyOffer is the qty of the promotion which need to be apply (we decrease this qty in each loop)
                promo.pendingQtyOffer = !_.isUndefined(promo.pendingQtyOffer) ? promo.pendingQtyOffer : promo.qtyOffer;
                if (promo.pendingQtyOffer && promo.pendingQtyOffer >= line.get('qty')) {
                  //if _.isUndefined(promo.actualAmt) is true we do not distribute the discount
                  if (_.isUndefined(promo.actualAmt)) {
                    if (promo.pendingQtyOffer !== promo.qtyOffer) {
                      copiedPromo.hidden = true;
                      copiedPromo.amt = OB.DEC.Zero;
                    }
                  } else {
                    copiedPromo.actualAmt = (promo.fullAmt / promo.qtyOffer) * line.get('qty');
                    copiedPromo.amt = (promo.fullAmt / promo.qtyOffer) * line.get('qty');
                    copiedPromo.obdiscQtyoffer = line.get('qty');
                    promo.distributedAmt = OB.DEC.add(promo.distributedAmt, OB.DEC.toNumber(OB.DEC.toBigDecimal((promo.fullAmt / promo.qtyOffer) * line.get('qty'))));
                  }

                  if (promo.pendingQtyOffer === line.get('qty')) {

                    if (!_.isUndefined(promo.actualAmt) && promo.actualAmt && promo.actualAmt !== promo.distributedAmt) {
                      copiedPromo.actualAmt = OB.DEC.add(copiedPromo.actualAmt, OB.DEC.sub(promo.actualAmt, promo.distributedAmt));
                      copiedPromo.amt = promo.amt ? OB.DEC.add(copiedPromo.amt, OB.DEC.sub(promo.amt, promo.distributedAmt)) : promo.amt;
                    }
                    promo.pendingQtyOffer = null;
                  } else {
                    promo.pendingQtyOffer = promo.pendingQtyOffer - line.get('qty');
                  }
                  if (line.get('promotions')) {
                    auxPromo = _.find(line.get('promotions'), function (promo) {
                      return promo.ruleId === copiedPromo.ruleId;
                    });
                    if (auxPromo) {
                      idx = line.get('promotions').indexOf(auxPromo);
                      line.get('promotions').splice(idx, 1, copiedPromo);
                    } else {
                      line.get('promotions').push(copiedPromo);
                    }
                  } else {
                    line.set('promotions', [copiedPromo]);
                  }
                } else if (promo.pendingQtyOffer) {
                  if (_.isUndefined(promo.actualAmt)) {
                    if (promo.pendingQtyOffer !== promo.qtyOffer) {
                      copiedPromo.hidden = true;
                      copiedPromo.amt = OB.DEC.Zero;
                    }
                  } else {
                    copiedPromo.actualAmt = (promo.fullAmt / promo.qtyOffer) * promo.pendingQtyOffer;
                    copiedPromo.amt = (promo.fullAmt / promo.qtyOffer) * promo.pendingQtyOffer;
                    copiedPromo.obdiscQtyoffer = promo.pendingQtyOffer;
                    promo.distributedAmt = OB.DEC.add(promo.distributedAmt, OB.DEC.toNumber(OB.DEC.toBigDecimal((promo.fullAmt / promo.qtyOffer) * promo.pendingQtyOffer)));
                  }
                  if (!_.isUndefined(promo.actualAmt) && promo.actualAmt && promo.actualAmt !== promo.distributedAmt) {
                    copiedPromo.actualAmt = OB.DEC.add(copiedPromo.actualAmt, OB.DEC.sub(promo.actualAmt, promo.distributedAmt));
                    copiedPromo.amt = promo.amt ? OB.DEC.add(copiedPromo.amt, OB.DEC.sub(promo.amt, promo.distributedAmt)) : promo.amt;
                  }

                  if (line.get('promotions')) {
                    auxPromo = _.find(line.get('promotions'), function (promo) {
                      return promo.ruleId === copiedPromo.ruleId;
                    });
                    if (auxPromo) {
                      idx = line.get('promotions').indexOf(auxPromo);
                      line.get('promotions').splice(idx, 1, copiedPromo);
                    } else {
                      line.get('promotions').push(copiedPromo);
                    }
                  } else {
                    line.set('promotions', [copiedPromo]);
                  }
                  promo.pendingQtyOffer = null;
                  //if it is the first we enter in this method, promotions which are not in the virtual ticket are deleted.
                } else if (isFirstTime) {
                  actProm = _.find(line.get('promotions'), function (prom) {
                    return prom.ruleId === promo.ruleId;
                  });
                  if (actProm) {
                    idx = line.get('promotions').indexOf(actProm);
                    if (idx > -1) {
                      line.get('promotions').splice(idx, 1);
                    }
                  }
                }
              });
              line.trigger('change');
            });
          }
        }
      });
      if (!linesCreated) {
        _.each(linesToCreate, function (line) {
          me.createLine(line.product, line.qty, null, line.attrs);
        });
        linesCreated = true;
      }
      this.get('lines').forEach(function (l) {
        l.calculateGross();
      });
      this.calculateGross();
      this.trigger('promotionsUpdated');
      this.set('skipApplyPromotions', localSkipApplyPromotions);
    },


    // for each line, decrease the qtyOffer of promotions and remove the lines with qty 0
    removeQtyOffer: function () {
      var linesPending = new Backbone.Collection();
      this.get('lines').forEach(function (l) {
        var promotionsApplyNext = [],
            promotionsCascadeApplied = [],
            qtyReserved = 0,
            qtyPending;
        if (l.get('promotions')) {
          promotionsApplyNext = [];
          promotionsCascadeApplied = [];
          l.get('promotions').forEach(function (p) {
            if (p.qtyOfferReserved > 0) {
              qtyReserved = OB.DEC.add(qtyReserved, p.qtyOfferReserved);
            }
            // if it is a promotions with applyNext, the line is related to the promotion, so, when applyPromotions is called again,
            // if the promotion is similar to this promotion, then no changes have been done, then stop
            if (p.applyNext) {
              promotionsApplyNext.push(p);
              promotionsCascadeApplied.push(p);
            }
          });
        }
        qtyPending = OB.DEC.sub(l.get('qty'), qtyReserved);
        l.set('qty', qtyPending);
        l.set('promotions', promotionsApplyNext);
        l.set('promotionsCascadeApplied', promotionsCascadeApplied);
      });

      _.each(this.get('lines').models, function (line) {
        if (line.get('qty') > 0) {
          linesPending.add(line);
        }
      });
      this.get('lines').reset(linesPending.models);
    },

    removeLinesWithoutPromotions: function () {
      var linesPending = new Backbone.Collection();
      _.each(this.get('lines').models, function (l) {
        if (l.get('promotions') && l.get('promotions').length > 0) {
          linesPending.push(l);
        }
      });
      this.set('lines', linesPending);
    },

    hasPromotions: function () {
      var hasPromotions = false;
      this.get('lines').forEach(function (l) {
        if (l.get('promotions') && l.get('promotions').length > 0) {
          hasPromotions = true;
        }
      });
      return hasPromotions;
    },

    isSimilarLine: function (line1, line2) {
      var equalPromotions = function (x, y) {
          var isEqual = true;
          if (x.length !== y.length) {
            isEqual = false;
          } else {
            x.forEach(function (p1, ind) {
              if (p1.amt !== y[ind].amt || p1.displayedTotalAmount !== y[ind].displayedTotalAmount || p1.qtyOffer !== y[ind].qtyOffer || p1.qtyOfferReserved !== y[ind].qtyOfferReserved || p1.ruleId !== y[ind].ruleId || p1.obdiscQtyoffer !== y[ind].obdiscQtyoffer) {
                isEqual = false;
              }
            });
          }
          return isEqual;
          };
      if (line1.get('product').get('id') === line2.get('product').get('id') && line1.get('price') === line2.get('price') && line1.get('discountedLinePrice') === line2.get('discountedLinePrice') && line1.get('qty') === line2.get('qty')) {
        return equalPromotions(line1.get('promotions') || [], line2.get('promotions') || []);
      } else {
        return false;
      }
    },

    // if there is a promtion of type "applyNext" that it has been applied previously in the line, then It is replaced
    // by the first promotion applied. Ex:
    // Ex: prod1 - qty 5 - disc3x2 & discPriceAdj -> priceAdj is applied first to 5 units
    //     it is called to applyPromotions, with the 2 units frees, and priceAdj is applied again to this 2 units
    // it is wrong, only to 5 units should be applied priceAdj, no 5 + 2 units
    removePromotionsCascadeApplied: function () {
      this.get('lines').forEach(function (l) {
        if (!OB.UTIL.isNullOrUndefined(l.get('promotions')) && l.get('promotions').length > 0 && !OB.UTIL.isNullOrUndefined(l.get('promotionsCascadeApplied')) && l.get('promotionsCascadeApplied').length > 0) {
          l.get('promotions').forEach(function (p, ind) {
            l.get('promotionsCascadeApplied').forEach(function (pc) {
              if (p.ruleId === pc.ruleId) {
                l.get('promotions')[ind] = pc;
              }
            });
          });
        }
      });
    },

    showMessagesPromotions: function (arrayMessages1, arrayMessages2) {
      arrayMessages1 = arrayMessages1 || [];
      (arrayMessages2 || []).forEach(function (m2) {
        if (_.filter(arrayMessages1, function (m1) {
          return m1 === m2;
        }).length === 0) {
          arrayMessages1.push(m2);
          OB.UTIL.showAlert.display(m2);
        }
      });
      return arrayMessages1;
    },

    getOrderDescription: function () {
      var desc = 'Id: ' + this.get('id') + ". Docno: " + this.get('documentNo') + ". Total gross: " + this.get('gross') + ". Lines: [";
      var i = 0;
      this.get('lines').forEach(function (l) {
        if (i !== 0) {
          desc += ",";
        }
        desc += '{Product: ' + l.get('product').get('_identifier') + ', Quantity: ' + l.get('qty') + ' Gross: ' + l.get('gross') + '}';
        i++;
      });
      desc += '] Payments: [';
      i=0;
      this.get('payments').forEach(function (l) {
        if (i !== 0) {
          desc += ",";
        }
        desc += '{PaymentMethod: ' + l.get('kind') + ', Amount: ' + l.get('amount') + ' OrigAmount: ' + l.get('origAmount') + ' Date: '+l.get('date')+' isocode: '+l.get('isocode')+'}';
        i++;
      });
      desc += ']';
      return desc;
    }

  });

  var OrderList = Backbone.Collection.extend({
    model: Order,

    constructor: function (modelOrder) {
      if (modelOrder) {
        //this._id = 'modelorderlist';
        this.modelorder = modelOrder;
      }
      Backbone.Collection.prototype.constructor.call(this);
    },

    initialize: function () {
      var me = this;
      this.current = null;
      if (this.modelorder) {
        this.modelorder.on('saveCurrent', function () {
          me.saveCurrent();
        });
      }
    },

    newOrder: function () {
      var order = new Order(),
          me = this,
          documentseq, documentseqstr, receiptProperties, i, p;

      // reset in new order properties defined in Receipt Properties dialog
      if (OB.MobileApp.view.$.containerWindow && OB.MobileApp.view.$.containerWindow.getRoot() && OB.MobileApp.view.$.containerWindow.getRoot().$.receiptPropertiesDialog) {
        receiptProperties = OB.MobileApp.view.$.containerWindow.getRoot().$.receiptPropertiesDialog.newAttributes;
        for (i = 0; i < receiptProperties.length; i++) {
          if (receiptProperties[i].modelProperty) {
            order.set(receiptProperties[i].modelProperty, '');
          }
          if (receiptProperties[i].extraProperties) {
            for (p = 0; p < receiptProperties[i].extraProperties.length; p++) {
              order.set(receiptProperties[i].extraProperties[p], '');
            }
          }
        }
      }

      order.set('client', OB.POS.modelterminal.get('terminal').client);
      order.set('organization', OB.POS.modelterminal.get('terminal').organization);
      order.set('createdBy', OB.POS.modelterminal.get('orgUserId'));
      order.set('updatedBy', OB.POS.modelterminal.get('orgUserId'));
      order.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentType);
      order.set('orderType', 0); // 0: Sales order, 1: Return order, 2: Layaway, 3: Void Layaway
      order.set('generateInvoice', false);
      order.set('isQuotation', false);
      order.set('oldId', null);
      order.set('session', OB.POS.modelterminal.get('session'));
      order.set('priceList', OB.POS.modelterminal.get('terminal').priceList);
      order.set('priceIncludesTax', OB.POS.modelterminal.get('pricelist').priceIncludesTax);
      if (OB.POS.modelterminal.hasPermission('OBPOS_receipt.invoice')) {
        if (OB.POS.modelterminal.hasPermission('OBPOS_retail.restricttaxidinvoice', true) && !OB.POS.modelterminal.get('businessPartner').get('taxID')) {
          order.set('generateInvoice', false);
        } else {
          order.set('generateInvoice', OB.POS.modelterminal.get('terminal').terminalType.generateInvoice);
        }
      } else {
        order.set('generateInvoice', false);
      }
      order.set('currency', OB.POS.modelterminal.get('terminal').currency);
      order.set('currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, OB.POS.modelterminal.get('terminal')['currency' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER]);
      order.set('warehouse', OB.POS.modelterminal.get('terminal').warehouse);
      order.set('salesRepresentative', OB.POS.modelterminal.get('context').user.id);
      order.set('salesRepresentative' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, OB.POS.modelterminal.get('context').user._identifier);
      order.set('posTerminal', OB.POS.modelterminal.get('terminal').id);
      order.set('posTerminal' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER, OB.POS.modelterminal.get('terminal')._identifier);
      order.set('orderDate', new Date());
      order.set('isPaid', false);
      order.set('paidOnCredit', false);
      order.set('isLayaway', false);
      order.set('taxes', null);

      var nextDocumentno = OB.MobileApp.model.getNextDocumentno();
      order.set('documentnoPrefix', OB.MobileApp.model.get('terminal').docNoPrefix);
      order.set('documentnoSuffix', nextDocumentno.documentnoSuffix);
      order.set('documentNo', nextDocumentno.documentNo);

      order.set('bp', OB.POS.modelterminal.get('businessPartner'));
      order.set('print', true);
      order.set('sendEmail', false);
      order.set('openDrawer', false);
      return order;
    },

    newPaidReceipt: function (model, callback) {
      var order = new Order(),
          lines, me = this,
          documentseq, documentseqstr, bp, newline, prod, payments, curPayment, taxes, bpId, bpLocId, numberOfLines = model.receiptLines.length,
          orderQty = 0;

      // Call orderLoader plugings to adjust remote model to local model first
      // ej: sales on credit: Add a new payment if total payment < total receipt
      // ej: gift cards: Add a new payment for each gift card discount
      _.each(OB.Model.modelLoaders, function (f) {
        f(model);
      });

      //model.set('id', null);
      lines = new Backbone.Collection();

      // set all properties coming from the model
      order.set(model);

      // setting specific properties
      order.set('isbeingprocessed', 'N');
      order.set('hasbeenpaid', 'Y');
      order.set('isEditable', false);
      order.set('checked', model.checked); //TODO: what is this for, where it comes from?
      order.set('paidOnCredit', false);
      if (model.isQuotation) {
        order.set('isQuotation', true);
        order.set('oldId', model.orderid);
        order.set('id', null);
        order.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentTypeForQuotations);
      }
      if (model.isLayaway) {
        order.set('isLayaway', true);
        order.set('id', model.orderid);
        order.set('createdBy', OB.POS.terminal.terminal.usermodel.id);
        order.set('hasbeenpaid', 'N');
        order.set('session', OB.POS.modelterminal.get('session'));
      } else {
        order.set('isPaid', true);
        if (model.receiptPayments.length === 0 && model.totalamount > 0 && !model.isQuotation) {
          order.set('paidOnCredit', true);
        }
        order.set('id', model.orderid);
        if (order.get('documentType') === OB.POS.modelterminal.get('terminal').terminalType.documentTypeForReturns) {
          //return
          order.set('orderType', 1);
        }
      }

      bpLocId = model.bpLocId;
      bpId = model.bp;
      OB.Dal.get(OB.Model.BusinessPartner, bpId, function (bp) {
        OB.Dal.get(OB.Model.BPLocation, bpLocId, function (bpLoc) {
          bp.set('locName', bpLoc.get('name'));
          bp.set('locId', bpLoc.get('id'));
          order.set('bp', bp);
          order.set('gross', model.totalamount);
          order.set('net', model.totalNetAmount);
          order.trigger('calculategross');

          _.each(model.receiptLines, function (iter) {
            var price;
            if (order.get('priceIncludesTax')) {
              price = OB.DEC.number(iter.unitPrice);
            } else {
              price = OB.DEC.number(iter.baseNetUnitPrice);
            }

            OB.Dal.get(OB.Model.Product, iter.id, function (prod) {
              newline = new OrderLine({
                product: prod,
                uOM: iter.uOM,
                qty: OB.DEC.number(iter.quantity),
                price: price,
                priceList: prod.get('listPrice'),
                promotions: iter.promotions,
                priceIncludesTax: order.get('priceIncludesTax'),
                warehouse: {
                  id: iter.warehouse,
                  warehousename: iter.warehousename
                }
              });
              newline.calculateGross();
              // add the created line
              lines.add(newline);
              numberOfLines--;
              orderQty = OB.DEC.add(iter.quantity, orderQty);
              if (numberOfLines === 0) {
                order.set('lines', lines);
                order.set('qty', orderQty);
                if (order.get('orderType') === 1) {
                  order.changeSignToShowReturns();
                }
                order.set('json', JSON.stringify(order.toJSON()));
                callback(order);
              }
            });
          });
          order.set('orderDate', moment(model.orderDate.toString(), "YYYY-MM-DD").toDate());
          //order.set('payments', model.receiptPayments);
          payments = new PaymentLineList();
          _.each(model.receiptPayments, function (iter) {
            var paymentProp;
            curPayment = new PaymentLine();
            for (paymentProp in iter) {
              if (iter.hasOwnProperty(paymentProp)) {
                if (paymentProp === "paymentDate") {
                  if (!OB.UTIL.isNullOrUndefined(iter[paymentProp]) && moment(iter[paymentProp]).isValid()) {
                    curPayment.set(paymentProp, new Date(iter[paymentProp]));
                  } else {
                    curPayment.set(paymentProp, null);
                  }
                } else {
                  curPayment.set(paymentProp, iter[paymentProp]);
                }
              }
            }
            payments.add(curPayment);
          });
          order.set('payments', payments);
          order.adjustPayment();

          taxes = {};
          _.each(model.receiptTaxes, function (iter) {
            var taxProp;
            taxes[iter.taxid] = {};
            for (taxProp in iter) {
              if (iter.hasOwnProperty(taxProp)) {
                taxes[iter.taxid][taxProp] = iter[taxProp];
              }
            }
          });
          order.set('taxes', taxes);
        }, function () {
          // TODO: Report errors properly
        });

      }, function () {
        // TODO: Report errors properly
      });
    },
    newDynamicOrder: function (model, callback) {
      var order = new Backbone.Model(),
          undf;
      _.each(_.keys(model), function (key) {
        if (model[key] !== undf) {
          if (model[key] === null) {
            order.set(key, null);
          } else {
            order.set(key, model[key]);
          }
        }
      });
      callback(order);
    },
    addNewOrder: function () {
      this.saveCurrent();
      this.current = this.newOrder();
      this.add(this.current);
      this.loadCurrent(true);
    },

    addThisOrder: function (model) {
      this.saveCurrent();
      this.current = model;
      this.add(this.current);
      this.loadCurrent();
    },

    addFirstOrder: function () {
      this.addNewOrder();
    },

    addPaidReceipt: function (model) {
      this.saveCurrent();
      this.current = model;
      this.add(this.current);
      this.loadCurrent(true);
      // OB.Dal.save is done here because we want to force to save with the original od, only this time.
      OB.Dal.save(model, function () {}, function () {
        OB.error(arguments);
      }, model.get('isLayaway'));
    },
    addMultiReceipt: function (model) {
      OB.Dal.save(model, function () {}, function () {
        OB.error(arguments);
      }, model.get('isLayaway'));
    },

    addNewQuotation: function () {
      var documentseq, documentseqstr;
      this.saveCurrent();
      this.current = this.newOrder();
      this.current.set('isQuotation', true);
      this.current.set('generateInvoice', false);
      this.current.set('documentType', OB.POS.modelterminal.get('terminal').terminalType.documentTypeForQuotations);
      var nextQuotationno = OB.MobileApp.model.getNextQuotationno();
      this.current.set('quotationDocNoPrefix', OB.MobileApp.model.get('terminal').quotationDocNoPrefix);
      this.current.set('quotationnoSuffix', nextQuotationno.quotationnoSuffix);
      this.current.set('documentNo', nextQuotationno.documentNo);

      this.add(this.current);
      this.loadCurrent();
    },
    deleteCurrentFromDatabase: function (orderToDelete) {
      OB.Dal.remove(orderToDelete, function () {
        return true;
      }, function () {
        OB.UTIL.showError('Error removing');
      });
    },
    deleteCurrent: function (forceCreateNew) {
      var isNew = false;

      if (this.current) {
        this.remove(this.current);
        if (this.length > 0 && !forceCreateNew) {
          this.current = this.at(this.length - 1);
        } else {
          this.current = this.newOrder();
          this.add(this.current);
          isNew = true;
        }
        this.loadCurrent(isNew);
      }
    },

    load: function (model) {
      // Workaround to prevent the pending receipts moder window from remaining open
      // when the current receipt is selected from the list
      if (model && this.current && model.get('documentNo') === this.current.get('documentNo')) {
        return;
      }
      this.saveCurrent();
      this.current = model;
      this.loadCurrent();
    },
    saveCurrent: function () {
      if (this.current) {
        this.current.clearWith(this.modelorder);
      }
    },
    loadCurrent: function (isNew) {
      if (this.current) {
        if (isNew) {
          //set values of new attrs in current,
          //this values will be copied to modelOrder
          //in the next instruction
          this.modelorder.trigger('beforeChangeOrderForNewOne', this.current);
          this.current.set('isNewReceipt', true);
        }
        this.modelorder.clearWith(this.current);
        this.modelorder.set('isNewReceipt', false);
      }
    }

  });
  var MultiOrders = Backbone.Model.extend({
    modelName: 'MultiOrders',
    initialize: function () {
      //ISSUE 24487: Callbacks of this collection still exists if you come back from other page.
      //Force to remove callbacks
      this.get('multiOrdersList').off();
    },
    defaults: {
      //isMultiOrders: false,
      multiOrdersList: new Backbone.Collection(),
      total: OB.DEC.Zero,
      payment: OB.DEC.Zero,
      pending: OB.DEC.Zero,
      change: OB.DEC.Zero,
      payments: new Backbone.Collection(),
      openDrawer: false,
      additionalInfo: null
    },
    getPaymentStatus: function () {
      var total = OB.DEC.abs(this.getTotal());
      var pay = this.getPayment();
      return {
        'total': OB.I18N.formatCurrency(total),
        'pending': OB.DEC.compare(OB.DEC.sub(pay, total)) >= 0 ? OB.I18N.formatCurrency(OB.DEC.Zero) : OB.I18N.formatCurrency(OB.DEC.sub(total, pay)),
        'change': OB.DEC.compare(this.getChange()) > 0 ? OB.I18N.formatCurrency(this.getChange()) : null,
        'overpayment': OB.DEC.compare(OB.DEC.sub(pay, total)) > 0 ? OB.I18N.formatCurrency(OB.DEC.sub(pay, total)) : null,
        'isReturn': this.get('gross') < 0 ? true : false,
        'isNegative': this.get('gross') < 0 ? true : false,
        'changeAmt': this.getChange(),
        'pendingAmt': OB.DEC.compare(OB.DEC.sub(pay, total)) >= 0 ? OB.DEC.Zero : OB.DEC.sub(total, pay),
        'payments': this.get('payments')
      };
    },

    adjustPayment: function () {
      var i, max, p;
      var payments = this.get('payments');
      var total = OB.DEC.abs(this.getTotal());

      var nocash = OB.DEC.Zero;
      var cash = OB.DEC.Zero;
      var origCash = OB.DEC.Zero;
      var auxCash = OB.DEC.Zero;
      var prevCash = OB.DEC.Zero;
      var paidCash = OB.DEC.Zero;
      var pcash;

      for (i = 0, max = payments.length; i < max; i++) {
        p = payments.at(i);
        if (p.get('rate') && p.get('rate') !== '1') {
          p.set('origAmount', OB.DEC.mul(p.get('amount'), p.get('rate')));
        } else {
          p.set('origAmount', p.get('amount'));
        }
        p.set('paid', p.get('origAmount'));
        if (p.get('kind') === OB.POS.modelterminal.get('paymentcash')) {
          // The default cash method
          cash = OB.DEC.add(cash, p.get('origAmount'));
          pcash = p;
          paidCash = OB.DEC.add(paidCash, p.get('origAmount'));
        } else if (OB.POS.modelterminal.hasPayment(p.get('kind')) && OB.POS.modelterminal.hasPayment(p.get('kind')).paymentMethod.iscash) {
          // Another cash method
          origCash = OB.DEC.add(origCash, p.get('origAmount'));
          pcash = p;
          paidCash = OB.DEC.add(paidCash, p.get('origAmount'));
        } else {
          nocash = OB.DEC.add(nocash, p.get('origAmount'));
        }
      }

      // Calculation of the change....
      //FIXME
      if (pcash) {
        if (pcash.get('kind') !== OB.POS.modelterminal.get('paymentcash')) {
          auxCash = origCash;
          prevCash = cash;
        } else {
          auxCash = cash;
          prevCash = origCash;
        }
        if (OB.DEC.compare(nocash - total) > 0) {
          pcash.set('paid', OB.DEC.Zero);
          this.set('payment', nocash);
          this.set('change', OB.DEC.add(cash, origCash));
        } else if (OB.DEC.compare(OB.DEC.sub(OB.DEC.add(OB.DEC.add(nocash, cash), origCash), total)) > 0) {
          pcash.set('paid', OB.DEC.sub(total, OB.DEC.add(nocash, OB.DEC.sub(paidCash, pcash.get('origAmount')))));
          this.set('payment', total);
          //The change value will be computed through a rounded total value, to ensure that the total plus change
          //add up to the paid amount without any kind of precission loss
          this.set('change', OB.DEC.sub(OB.DEC.add(OB.DEC.add(nocash, cash), origCash), OB.Utilities.Number.roundJSNumber(total, 2)));
        } else {
          pcash.set('paid', auxCash);
          this.set('payment', OB.DEC.add(OB.DEC.add(nocash, cash), origCash));
          this.set('change', OB.DEC.Zero);
        }
      } else {
        if (payments.length > 0) {
          if (this.get('payment') === 0 || nocash > 0) {
            this.set('payment', nocash);
          }
        } else {
          this.set('payment', OB.DEC.Zero);
        }
        this.set('change', OB.DEC.Zero);
      }
    },
    addPayment: function (payment) {
      var payments, total;
      var i, max, p, order;

      if (!OB.DEC.isNumber(payment.get('amount'))) {
        alert(OB.I18N.getLabel('OBPOS_MsgPaymentAmountError'));
        return;
      }

      payments = this.get('payments');
      total = OB.DEC.abs(this.getTotal());
      order = this;
      OB.MobileApp.model.hookManager.executeHooks('OBPOS_preAddPayment', {
        paymentToAdd: payment,
        payments: payments,
        receipt: this
      }, function () {
        if (!payment.get('paymentData')) {
          // search for an existing payment only if there is not paymentData info.
          // this avoids to merge for example card payments of different cards.
          for (i = 0, max = payments.length; i < max; i++) {
            p = payments.at(i);
            if (p.get('kind') === payment.get('kind') && !p.get('isPrePayment')) {
              p.set('amount', OB.DEC.add(payment.get('amount'), p.get('amount')));
              if (p.get('rate') && p.get('rate') !== '1') {
                p.set('origAmount', OB.DEC.add(payment.get('origAmount'), OB.DEC.mul(p.get('origAmount'), p.get('rate'))));
              }
              order.adjustPayment();
              order.trigger('displayTotal');
              return;
            }
          }
        }
        if (payment.get('openDrawer') && (payment.get('allowOpenDrawer') || payment.get('isCash'))) {
          order.set('openDrawer', payment.get('openDrawer'));
        }
        payment.set('date', new Date());
        payments.add(payment);
        order.adjustPayment();
        order.trigger('displayTotal');
      });
    },
    removePayment: function (payment) {
      var payments = this.get('payments');
      payments.remove(payment);
      if (payment.get('openDrawer')) {
        this.set('openDrawer', false);
      }
      this.adjustPayment();
    },
    printGross: function () {
      return OB.I18N.formatCurrency(this.getTotal());
    },
    getTotal: function () {
      return this.get('total');
    },
    getChange: function () {
      return this.get('change');
    },
    getPayment: function () {
      return this.get('payment');
    },
    getPending: function () {
      return OB.DEC.sub(OB.DEC.abs(this.getTotal()), this.getPayment());
    },
    toInvoice: function (status) {
      if (status === false) {
        this.unset('additionalInfo');
        _.each(this.get('multiOrdersList').models, function (order) {
          order.unset('generateInvoice');
        }, this);
        return;
      }
      this.set('additionalInfo', 'I');
      _.each(this.get('multiOrdersList').models, function (order) {
        order.set('generateInvoice', true);
      }, this);
    },
    resetValues: function () {
      //this.set('isMultiOrders', false);
      this.get('multiOrdersList').reset();
      this.set('total', OB.DEC.Zero);
      this.set('payment', OB.DEC.Zero);
      this.set('pending', OB.DEC.Zero);
      this.set('change', OB.DEC.Zero);
      this.get('payments').reset();
      this.set('openDrawer', false);
      this.set('additionalInfo', null);
    },
    hasDataInList: function () {
      if (this.get('multiOrdersList') && this.get('multiOrdersList').length > 0) {
        return true;
      }
      return false;
    }
  });
  var TaxLine = Backbone.Model.extend();
  OB.Data.Registry.registerModel(OrderLine);
  OB.Data.Registry.registerModel(PaymentLine);

  // order model is not registered using standard Registry method becasue list is
  // becasue collection is specific
  window.OB.Model.Order = Order;
  window.OB.Collection.OrderList = OrderList;
  window.OB.Model.TaxLine = TaxLine;
  window.OB.Model.MultiOrders = MultiOrders;

  window.OB.Model.modelLoaders = [];
}());