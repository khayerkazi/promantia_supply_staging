/*
 ************************************************************************************
 * Copyright (C) 2013-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo,_ */

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.Payment',
  published: {
    receipt: null
  },
  handlers: {
    onButtonStatusChanged: 'buttonStatusChanged'
  },
  getSelectedPayment: function () {
    if (this.receipt && this.receipt.selectedPayment) {
      return this.receipt.selectedPayment;
    }
    return null;
  },
  buttonStatusChanged: function (inSender, inEvent) {
    var payment, amt, change, pending, isMultiOrders, paymentstatus;
    payment = inEvent.value.payment || OB.POS.terminal.terminal.paymentnames[OB.POS.modelterminal.get('paymentcash')];
    if (_.isUndefined(payment)) {
      return true;
    }
    isMultiOrders = this.model.isValidMultiOrderState();
    change = this.model.getChange();
    pending = this.model.getPending();
    if (!isMultiOrders) {
      this.receipt.selectedPayment = payment.payment.searchKey;
      paymentstatus = this.receipt.getPaymentStatus();
    } else {
      this.model.get('multiOrders').set('selectedPayment', payment.payment.searchKey);
      paymentstatus = this.model.get('multiOrders').getPaymentStatus();
    }

    if (!_.isNull(change) && change) {
      this.$.change.setContent(OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(change, payment.mulrate), payment.symbol, payment.currencySymbolAtTheRight));
      OB.MobileApp.model.set('changeReceipt', OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(change, payment.mulrate), payment.symbol, payment.currencySymbolAtTheRight));
    } else if (!_.isNull(pending) && pending) {
      this.$.totalpending.setContent(OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(pending, payment.mulrate), payment.symbol, payment.currencySymbolAtTheRight));
    }
    this.checkEnoughCashAvailable(paymentstatus, payment);
  },
  components: [{
    style: 'background-color: #363636; color: white; height: 200px; margin: 5px; padding: 5px; position: relative;',
    components: [{
      classes: 'row-fluid',
      components: [{
        classes: 'span12'
      }]
    }, {
      classes: 'row-fluid',
      components: [{
        classes: 'span9',
        components: [{
          style: 'padding: 10px 0px 0px 10px; height: 28px;',
          components: [{
            tag: 'span',
            name: 'totalpending',
            style: 'font-size: 24px; font-weight: bold;'
          }, {
            tag: 'span',
            name: 'totalpendinglbl'
          }, {
            tag: 'span',
            name: 'change',
            style: 'font-size: 24px; font-weight: bold;'
          }, {
            tag: 'span',
            name: 'changelbl'
          }, {
            tag: 'span',
            name: 'overpayment',
            style: 'font-size: 24px; font-weight: bold;'
          }, {
            tag: 'span',
            name: 'overpaymentlbl'
          }, {
            tag: 'span',
            name: 'exactlbl'
          }, {
            tag: 'span',
            name: 'donezerolbl'
          }, {
            name: 'creditsalesaction',
            kind: 'OB.OBPOSPointOfSale.UI.CreditButton'
          }, {
            name: 'layawayaction',
            kind: 'OB.OBPOSPointOfSale.UI.LayawayButton',
            showing: false
          }]
        }, {
          style: 'overflow:auto; width: 100%;',
          components: [{
            style: 'padding: 5px',
            components: [{
              style: 'margin: 2px 0px 0px 0px; border-bottom: 1px solid #cccccc;'
            }, {
              kind: 'OB.UI.ScrollableTable',
              scrollAreaMaxHeight: '150px',
              name: 'payments',
              renderEmpty: enyo.kind({
                style: 'height: 36px'
              }),
              renderLine: 'OB.OBPOSPointOfSale.UI.RenderPaymentLine'
            }, {
              kind: 'OB.UI.ScrollableTable',
              scrollAreaMaxHeight: '150px',
              name: 'multiPayments',
              showing: false,
              renderEmpty: enyo.kind({
                style: 'height: 36px'
              }),
              renderLine: 'OB.OBPOSPointOfSale.UI.RenderPaymentLine'
            }, {
              style: 'position: absolute; bottom: 0px; height: 20px; color: #ff0000;',
              name: 'noenoughchangelbl',
              showing: false
            }]
          }]
        }]
      }, {
        classes: 'span3',
        components: [{
          style: 'float: right;',
          name: 'doneaction',
          components: [{
            kind: 'OB.OBPOSPointOfSale.UI.DoneButton'
          }]
        }, {
          style: 'float: right;',
          name: 'exactaction',
          components: [{
            kind: 'OB.OBPOSPointOfSale.UI.ExactButton'
          }]
        }]
      }]

    }]
  }],

  receiptChanged: function () {
    var me = this;
    this.$.payments.setCollection(this.receipt.get('payments'));
    this.$.multiPayments.setCollection(this.model.get('multiOrders').get('payments'));
    this.receipt.on('change:payment change:change calculategross change:bp change:gross', function () {
      this.updatePending();
    }, this);
    this.model.get('leftColumnViewManager').on('change:currentView', function () {
      if (!this.model.get('leftColumnViewManager').isMultiOrder()) {
        this.updatePending();
      } else {
        this.updatePendingMultiOrders();
      }
    }, this);
    this.updatePending();
    if (this.model.get('leftColumnViewManager').isMultiOrder()) {
      this.updatePendingMultiOrders();
    }
    this.receipt.on('change:orderType change:isLayaway change:payment', function (model) {
      if (this.model.get('leftColumnViewManager').isMultiOrder()) {
        this.$.creditsalesaction.hide();
        this.$.layawayaction.hide();
        return;
      }
      var payment = OB.POS.terminal.terminal.paymentnames[OB.POS.terminal.terminal.get('paymentcash')];
      if ((model.get('orderType') === 2 || (model.get('isLayaway'))) && model.get('orderType') !== 3 && !model.getPaymentStatus().done) {
        this.$.creditsalesaction.hide();
        this.$.layawayaction.setContent(OB.I18N.getLabel('OBPOS_LblLayaway'));
        this.$.layawayaction.show();
      } else if (model.get('orderType') === 3) {
        this.$.creditsalesaction.hide();
        this.$.layawayaction.hide();
      } else {
        this.$.layawayaction.hide();
      }
    }, this);
  },


  updatePending: function () {
    if (this.model.get('leftColumnViewManager').isMultiOrder()) {
      return true;
    }
    var paymentstatus = this.receipt.getPaymentStatus();
    var symbol = '',
        rate = OB.DEC.One,
        symbolAtRight = true,
        isCashType = true;

    if (_.isEmpty(OB.MobileApp.model.paymentnames)) {
      symbol = OB.MobileApp.model.get('terminal').symbol;
      symbolAtRight = OB.MobileApp.model.get('terminal').currencySymbolAtTheRight;
    }
    if (!_.isUndefined(this.receipt) && !_.isUndefined(OB.POS.terminal.terminal.paymentnames[this.receipt.selectedPayment])) {
      symbol = OB.POS.terminal.terminal.paymentnames[this.receipt.selectedPayment].symbol;
      rate = OB.POS.terminal.terminal.paymentnames[this.receipt.selectedPayment].mulrate;
      symbolAtRight = OB.POS.terminal.terminal.paymentnames[this.receipt.selectedPayment].currencySymbolAtTheRight;
      isCashType = OB.POS.terminal.terminal.paymentnames[this.receipt.selectedPayment].paymentMethod.iscash;
    }
    this.checkEnoughCashAvailable(paymentstatus, OB.POS.terminal.terminal.paymentnames[this.receipt.selectedPayment || OB.POS.modelterminal.get('paymentcash')]);
    if (paymentstatus.change) {
      this.$.change.setContent(OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(this.receipt.getChange(), rate), symbol, symbolAtRight));
      OB.MobileApp.model.set('changeReceipt', OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(this.receipt.getChange(), rate), symbol, symbolAtRight));
      this.$.change.show();
      this.$.changelbl.show();
    } else {
      this.$.change.hide();
      this.$.changelbl.hide();
    }
    if (paymentstatus.overpayment) {
      this.$.overpayment.setContent(paymentstatus.overpayment);
      this.$.overpayment.show();
      this.$.overpaymentlbl.show();
    } else {
      this.$.overpayment.hide();
      this.$.overpaymentlbl.hide();
    }

    if (paymentstatus.done) {
      this.$.totalpending.hide();
      this.$.totalpendinglbl.hide();
      if (!_.isEmpty(OB.MobileApp.model.paymentnames)) {
        this.$.doneaction.show();
      }
      this.$.creditsalesaction.hide();
      this.$.layawayaction.hide();
    } else {
      this.$.totalpending.setContent(OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(this.receipt.getPending(), rate), symbol, symbolAtRight));
      this.$.totalpending.show();
      //      if (this.receipt.get('orderType') === 1 || this.receipt.get('orderType') === 3) {
      if (paymentstatus.isNegative || this.receipt.get('orderType') === 3) {
        this.$.totalpendinglbl.setContent(OB.I18N.getLabel('OBPOS_ReturnRemaining'));
      } else {
        this.$.totalpendinglbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsRemaining'));
      }
      this.$.totalpendinglbl.show();
      this.$.doneaction.hide();
      if (this.$.doneButton.drawerpreference) {
        this.$.doneButton.setContent(OB.I18N.getLabel('OBPOS_LblOpen'));
        this.$.doneButton.drawerOpened = false;
      }
      if (OB.POS.modelterminal.get('terminal').allowpayoncredit && this.receipt.get('bp')) {
        if ((this.receipt.get('bp').get('creditLimit') > 0 || this.receipt.get('bp').get('creditUsed') < 0 || this.receipt.getGross() < 0) && !this.$.layawayaction.showing) {
          this.$.creditsalesaction.show();
        } else {
          this.$.creditsalesaction.hide();
        }
      }
    }

    if (paymentstatus.done || this.receipt.getGross() === 0) {
      this.$.exactaction.hide();
      this.$.creditsalesaction.hide();
      this.$.layawayaction.hide();
    } else {
      if (!_.isEmpty(OB.MobileApp.model.paymentnames)) {
        this.$.exactaction.show();
      }
      if (this.receipt.get('orderType') === 2 || (this.receipt.get('isLayaway') && this.receipt.get('orderType') !== 3)) {
        this.$.layawayaction.show();
        if (!this.receipt.get('isLayaway')) {
          this.$.exactaction.hide();
        }
      } else if (this.receipt.get('orderType') === 3) {
        this.$.layawayaction.hide();
      }
      if (OB.POS.modelterminal.get('terminal').allowpayoncredit && this.receipt.get('bp')) {
        if ((this.receipt.get('bp').get('creditLimit') > 0 || this.receipt.get('bp').get('creditUsed') < 0 || this.receipt.getGross() < 0) && !this.$.layawayaction.showing) {
          this.$.creditsalesaction.show();
        } else {
          this.$.creditsalesaction.hide();
        }
      }
    }
    if (paymentstatus.done && !paymentstatus.change && !paymentstatus.overpayment) {
      if (this.receipt.getGross() === 0) {
        this.$.exactlbl.hide();
        this.$.donezerolbl.show();
      } else {
        this.$.donezerolbl.hide();
        //        if (this.receipt.get('orderType') === 1 || this.receipt.get('orderType') === 3) {
        if (paymentstatus.isNegative || this.receipt.get('orderType') === 3) {
          this.$.exactlbl.setContent(OB.I18N.getLabel('OBPOS_ReturnExact'));
        } else {
          this.$.exactlbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsExact'));
        }
        this.$.exactlbl.show();
      }
    } else {
      this.$.exactlbl.hide();
      this.$.donezerolbl.hide();
    }
  },
  updatePendingMultiOrders: function () {
    var paymentstatus = this.model.get('multiOrders');
    var symbol = '',
        symbolAtRight = true,
        rate = OB.DEC.One,
        isCashType = true,
        selectedPayment;
    this.$.layawayaction.hide();
    if (_.isEmpty(OB.MobileApp.model.paymentnames)) {
      symbol = OB.MobileApp.model.get('terminal').symbol;
      symbolAtRight = OB.MobileApp.model.get('terminal').currencySymbolAtTheRight;
    }
    if (paymentstatus.get('selectedPayment')) {
      selectedPayment = OB.POS.terminal.terminal.paymentnames[paymentstatus.get('selectedPayment')];
    } else {
      selectedPayment = OB.POS.terminal.terminal.paymentnames[OB.POS.modelterminal.get('paymentcash')];
    }
    if (!_.isUndefined(selectedPayment)) {
      symbol = selectedPayment.symbol;
      rate = selectedPayment.mulrate;
      symbolAtRight = selectedPayment.currencySymbolAtTheRight;
      isCashType = selectedPayment.paymentMethod.iscash;
    }
    this.checkEnoughCashAvailable(paymentstatus.getPaymentStatus(), selectedPayment);
    if (paymentstatus.get('change')) {
      this.$.change.setContent(OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(paymentstatus.get('change'), rate), symbol, symbolAtRight));
      OB.MobileApp.model.set('changeReceipt', OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(paymentstatus.get('change'), rate), symbol, symbolAtRight));
      this.$.change.show();
      this.$.changelbl.show();
    } else {
      this.$.change.hide();
      this.$.changelbl.hide();
    }
    //overpayment
    if (OB.DEC.compare(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))) > 0) {
      this.$.overpayment.setContent(OB.I18N.formatCurrency(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))));
      this.$.overpayment.show();
      this.$.overpaymentlbl.show();
    } else {
      this.$.overpayment.hide();
      this.$.overpaymentlbl.hide();
    }

    if (paymentstatus.get('multiOrdersList').length > 0 && OB.DEC.compare(paymentstatus.get('total')) >= 0 && OB.DEC.compare(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))) >= 0) {
      this.$.totalpending.hide();
      this.$.totalpendinglbl.hide();
      if (!_.isEmpty(OB.MobileApp.model.paymentnames)) {
        this.$.doneaction.show();
      }
      this.$.creditsalesaction.hide();
      //            this.$.layawayaction.hide();
    } else {
      this.$.totalpending.setContent(OB.I18N.formatCurrency(OB.I18N.formatCurrencyWithSymbol(OB.DEC.mul(OB.DEC.sub(paymentstatus.get('total'), paymentstatus.get('payment')), rate), symbol, symbolAtRight)));
      this.$.totalpending.show();
      this.$.totalpendinglbl.show();
      this.$.doneaction.hide();
      if (this.$.doneButton.drawerpreference) {
        this.$.doneButton.setContent(OB.I18N.getLabel('OBPOS_LblOpen'));
        this.$.doneButton.drawerOpened = false;
      }
    }

    this.$.creditsalesaction.hide();
    this.$.layawayaction.hide();
    if (paymentstatus.get('multiOrdersList').length > 0 && OB.DEC.compare(paymentstatus.get('total')) >= 0 && (OB.DEC.compare(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))) >= 0 || paymentstatus.get('total') === 0)) {
      this.$.exactaction.hide();
    } else {
      if (!_.isEmpty(OB.MobileApp.model.paymentnames)) {
        this.$.exactaction.show();
      }
    }
    if (paymentstatus.get('multiOrdersList').length > 0 && OB.DEC.compare(paymentstatus.get('total')) >= 0 && OB.DEC.compare(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))) >= 0 && !paymentstatus.get('change') && OB.DEC.compare(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))) <= 0) {
      if (paymentstatus.get('total') === 0) {
        this.$.exactlbl.hide();
        this.$.donezerolbl.show();
      } else {
        this.$.donezerolbl.hide();
        this.$.exactlbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsExact'));
        this.$.exactlbl.show();
      }
    } else {
      this.$.exactlbl.hide();
      this.$.donezerolbl.hide();
    }
  },

  checkEnoughCashAvailable: function (paymentstatus, selectedPayment) {
    var currentCash = OB.DEC.Zero,
        requiredCash, hasEnoughCash, hasAllEnoughCash = true;
    if (selectedPayment && selectedPayment.paymentMethod.iscash) {
      currentCash = selectedPayment.currentCash || OB.DEC.Zero;
    }

    if (OB.UTIL.isNullOrUndefined(selectedPayment) || !selectedPayment.paymentMethod.iscash) {
      requiredCash = OB.DEC.Zero;
    } else if (paymentstatus.isNegative) {
      requiredCash = paymentstatus.pendingAmt;
      paymentstatus.payments.each(function (payment) {
        var paymentmethod;
        if (payment.get('kind') === selectedPayment.payment.searchKey) {
          requiredCash = OB.DEC.add(requiredCash, payment.get('amount'));
        } else {
          paymentmethod = OB.POS.terminal.terminal.paymentnames[payment.get('kind')];
          if (paymentmethod && payment.get('amount') > paymentmethod.currentCash) {
            hasAllEnoughCash = false;
          }
        }
      });
    } else {
      requiredCash = paymentstatus.changeAmt;
    }

    hasEnoughCash = OB.DEC.compare(OB.DEC.sub(currentCash, requiredCash)) >= 0;
    if (hasEnoughCash && hasAllEnoughCash) {
      this.$.noenoughchangelbl.hide();
      this.$.payments.scrollAreaMaxHeight = '150px';
      this.$.doneButton.setDisabled(false);
    } else {
      this.$.noenoughchangelbl.show();
      this.$.payments.scrollAreaMaxHeight = '130px';
      this.$.doneButton.setDisabled(true);
    }
  },

  initComponents: function () {
    this.inherited(arguments);
    this.$.totalpendinglbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsRemaining'));
    this.$.changelbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsChange'));
    this.$.overpaymentlbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsOverpayment'));
    this.$.exactlbl.setContent(OB.I18N.getLabel('OBPOS_PaymentsExact'));
    this.$.donezerolbl.setContent(OB.I18N.getLabel('OBPOS_MsgPaymentAmountZero'));
    this.$.noenoughchangelbl.setContent(OB.I18N.getLabel('OBPOS_NoEnoughCash'));
  },
  init: function (model) {
    var me = this;
    this.model = model;
    if (_.isEmpty(OB.MobileApp.model.paymentnames)) {
      this.$.doneaction.show();
      this.$.exactaction.hide();
    }
    this.model.get('multiOrders').get('multiOrdersList').on('all', function (event) {
      if (this.model.isValidMultiOrderState()) {
        this.updatePendingMultiOrders();
      }
    }, this);

    this.model.get('multiOrders').on('change:payment change:total change:change', function () {
      this.updatePendingMultiOrders();
    }, this);
    this.model.get('leftColumnViewManager').on('change:currentView', function (changedModel) {
      if (changedModel.isOrder()) {
        this.$.multiPayments.hide();
        this.$.payments.show();
        return;
      }
      if (changedModel.isMultiOrder()) {
        this.$.multiPayments.show();
        this.$.payments.hide();
        return;
      }
    }, this);
    //    this.model.get('multiOrders').on('change:isMultiOrders', function () {
    //      if (!this.model.get('multiOrders').get('isMultiOrders')) {
    //        this.$.multiPayments.hide();
    //        this.$.payments.show();
    //      } else {
    //        this.$.payments.hide();
    //        this.$.multiPayments.show();
    //      }
    //    }, this);
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.DoneButton',
  kind: 'OB.UI.RegularButton',
  drawerOpened: true,
  init: function (model) {
    this.model = model;
    this.setContent(OB.I18N.getLabel('OBPOS_LblDone'));
    this.model.get('order').on('change:openDrawer', function () {
      this.drawerpreference = this.model.get('order').get('openDrawer');
      var me = this;

      if (this.drawerpreference) {
        this.drawerOpened = false;
        this.setContent(OB.I18N.getLabel('OBPOS_LblOpen'));
      } else {
        this.drawerOpened = true;
        this.setContent(OB.I18N.getLabel('OBPOS_LblDone'));
      }
    }, this);
    this.model.get('multiOrders').on('change:openDrawer', function () {
      this.drawerpreference = this.model.get('multiOrders').get('openDrawer');
      if (this.drawerpreference) {
        this.drawerOpened = false;
        this.setContent(OB.I18N.getLabel('OBPOS_LblOpen'));
      } else {
        this.drawerOpened = true;
        this.setContent(OB.I18N.getLabel('OBPOS_LblDone'));
      }
    }, this);
  },
  tap: function () {
    var myModel = this.owner.model,
        me = this,
        payments;
    this.allowOpenDrawer = false;

    if (myModel.get('leftColumnViewManager').isOrder()) {
      payments = this.owner.receipt.get('payments');
    } else {
      payments = this.owner.model.get('multiOrders').get('payments');
    }

    payments.each(function (payment) {
      if (payment.get('allowOpenDrawer') || payment.get('isCash')) {
        me.allowOpenDrawer = true;
      }
    });
    //if (this.owner.model.get('multiOrders').get('multiOrdersList').length === 0 && !this.owner.model.get('multiOrders').get('isMultiOrders')) {
    if (myModel.get('leftColumnViewManager').isOrder()) {
      if (this.drawerpreference && this.allowOpenDrawer) {
        if (this.drawerOpened) {
          if (this.owner.receipt.get('orderType') === 3) {
            this.owner.receipt.trigger('voidLayaway');
          } else {
            this.setDisabled(true);
            this.owner.model.get('order').trigger('paymentDone', false);
          }
          this.drawerOpened = false;
          this.setContent(OB.I18N.getLabel('OBPOS_LblOpen'));
        } else {
          OB.POS.hwserver.openDrawer({
            openFirst: true,
            receipt: me.owner.receipt
          }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
          this.drawerOpened = true;
          this.setContent(OB.I18N.getLabel('OBPOS_LblDone'));
        }
      } else {
        //Void Layaway
        if (this.owner.receipt.get('orderType') === 3) {
          this.owner.receipt.trigger('voidLayaway');
        } else {
          this.setDisabled(true);
          this.owner.receipt.trigger('paymentDone', this.allowOpenDrawer);
        }
      }
    } else {
      if (this.drawerpreference && this.allowOpenDrawer) {
        if (this.drawerOpened) {
          this.owner.model.get('multiOrders').trigger('paymentDone', false);
          this.owner.model.get('multiOrders').set('openDrawer', false);
          this.drawerOpened = false;
          this.setContent(OB.I18N.getLabel('OBPOS_LblOpen'));
        } else {
          OB.POS.hwserver.openDrawer({
            openFirst: true,
            receipt: me.owner.model.get('multiOrders')
          }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
          this.drawerOpened = true;
          this.setContent(OB.I18N.getLabel('OBPOS_LblDone'));
        }
      } else {
        this.owner.model.get('multiOrders').trigger('paymentDone', this.allowOpenDrawer);
        this.owner.model.get('multiOrders').set('openDrawer', false);
      }
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ExactButton',
  events: {
    onExactPayment: ''
  },
  kind: 'OB.UI.RegularButton',
  classes: 'btn-icon-adaptative btn-icon-check btnlink-green',
  style: 'width: 73px; height: 43.37px;',
  tap: function () {
    this.doExactPayment();
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.RenderPaymentLine',
  classes: 'btnselect',
  components: [{
    style: 'color:white;',
    components: [{
      name: 'name',
      style: 'float: left; width: 20%; padding: 5px 0px 0px 0px;'
    }, {
      name: 'info',
      style: 'float: left; width: 15%; padding: 5px 0px 0px 0px;'
    }, {
      name: 'foreignAmount',
      style: 'float: left; width: 20%; padding: 5px 0px 0px 0px; text-align: right;'
    }, {
      name: 'amount',
      style: 'float: left; width: 25%; padding: 5px 0px 0px 0px; text-align: right;'
    }, {
      style: 'float: left; width: 20%; text-align: right;',
      components: [{
        kind: 'OB.OBPOSPointOfSale.UI.RemovePayment'
      }]
    }, {
      style: 'clear: both;'
    }]
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.name.setContent(OB.POS.modelterminal.getPaymentName(this.model.get('kind')) || this.model.get('name'));
    this.$.amount.setContent(this.model.printAmount());
    if (this.model.get('rate') && this.model.get('rate') !== '1') {
      this.$.foreignAmount.setContent(this.model.printForeignAmount());
    } else {
      this.$.foreignAmount.setContent('');
    }
    if (this.model.get('description')) {
      this.$.info.setContent(this.model.get('description'));
    } else {
      if (this.model.get('paymentData')) {
        this.$.info.setContent(this.model.get('paymentData').Name);
      } else {
        this.$.info.setContent('');
      }
    }
    if (this.model.get('isPrePayment')) {
      this.hide();
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.RemovePayment',
  events: {
    onRemovePayment: ''
  },
  kind: 'OB.UI.SmallButton',
  classes: 'btnlink-darkgray btnlink-payment-clear btn-icon-small btn-icon-clearPayment',
  tap: function () {
    var me = this;
    if ((_.isUndefined(this.deleting) || this.deleting === false)) {
      this.deleting = true;
      this.removeClass('btn-icon-clearPayment');
      this.addClass('btn-icon-loading');

      this.doRemovePayment({
        payment: this.owner.model,
        removeCallback: function () {
          me.deleting = false;
          me.removeClass('btn-icon-loading');
          me.addClass('btn-icon-clearPayment');
        }
      });
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.CreditButton',
  kind: 'OB.UI.SmallButton',
  i18nLabel: 'OBPOS_LblCreditSales',
  classes: 'btn-icon-small btnlink-green',
  style: 'width: 120px; float: right; margin: -5px 5px 0px 0px; height: 1.8em',
  permission: 'OBPOS_receipt.creditsales',
  events: {
    onShowPopup: ''
  },
  init: function (model) {
    this.model = model;
  },
  disabled: false,
  putDisabled: function (status) {
    if (status === false) {
      this.setDisabled(false);
      this.removeClass('disabled');
      this.disabled = false;
    } else {
      this.setDisabled(true);
      this.addClass('disabled');
      this.disabled = true;
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    this.putDisabled(!OB.MobileApp.model.hasPermission(this.permission));
  },
  tap: function () {
    if (this.disabled) {
      return true;
    }

    var process = new OB.DS.Process('org.openbravo.retail.posterminal.CheckBusinessPartnerCredit');
    var me = this;
    var paymentstatus = this.model.get('order').getPaymentStatus();
    if (!paymentstatus.isReturn) {
      //this.setContent(OB.I18N.getLabel('OBPOS_LblLoading'));
      process.exec({
        businessPartnerId: this.model.get('order').get('bp').get('id'),
        totalPending: this.model.get('order').getPending()
      }, function (data) {
        if (data) {
          if (data.enoughCredit) {
            me.doShowPopup({
              popup: 'modalEnoughCredit',
              args: {
                order: me.model.get('order')
              }
            });
            //this.setContent(OB.I18N.getLabel('OBPOS_LblCreditSales'));
          } else {
            var bpName = data.bpName;
            var actualCredit = data.actualCredit;
            me.doShowPopup({
              popup: 'modalNotEnoughCredit',
              args: {
                bpName: bpName,
                actualCredit: actualCredit
              }
            });
            //this.setContent(OB.I18N.getLabel('OBPOS_LblCreditSales'));
            //OB.UI.UTILS.domIdEnyoReference['modalNotEnoughCredit'].$.bodyContent.children[0].setContent();
          }
        } else {
          OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgErrorCreditSales'));
        }
      }, function () {
        me.doShowPopup({
          popup: 'modalEnoughCredit',
          args: {
            order: me.model.get('order'),
            message: 'OBPOS_Unabletocheckcredit'
          }
        });
      });
      //    } else if (this.model.get('order').get('orderType') === 1) {
    } else if (paymentstatus.isReturn) {
      var actualCredit;
      var creditLimit = this.model.get('order').get('bp').get('creditLimit');
      var creditUsed = this.model.get('order').get('bp').get('creditUsed');
      var totalPending = this.model.get('order').getPending();
      this.doShowPopup({
        popup: 'modalEnoughCredit',
        args: {
          order: this.model.get('order')
        }
      });
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.LayawayButton',
  kind: 'OB.UI.SmallButton',
  content: '',
  classes: 'btn-icon-small btnlink-green',
  style: 'width: 120px; float: right; margin: -5px 5px 0px 0px; height: 1.8em',
  permission: 'OBPOS_receipt.layaway',
  init: function (model) {
    this.model = model;
    this.setContent(OB.I18N.getLabel('OBPOS_LblLayaway'));
  },
  tap: function () {
    var receipt = this.owner.receipt,
        negativeLines, me = this,
        myModel = this.owner.model,
        payments;
    this.allowOpenDrawer = false;

    if (myModel.get('leftColumnViewManager').isOrder()) {
      payments = this.owner.receipt.get('payments');
    } else {
      payments = this.owner.model.get('multiOrders').get('payments');
    }

    payments.each(function (payment) {
      if (payment.get('allowOpenDrawer') || payment.get('isCash')) {
        me.allowOpenDrawer = true;
      }
    });
    if (receipt) {
      negativeLines = _.find(receipt.get('lines').models, function (line) {
        return line.get('qty') < 0;
      });
      if (negativeLines) {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBPOS_layawaysOrdersWithReturnsNotAllowed'));
        return true;
      }
      if (receipt.get('generateInvoice')) {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBPOS_noInvoiceIfLayaway'));
        receipt.set('generateInvoice', false);
      }
    }
    receipt.trigger('paymentDone', me.allowOpenDrawer);
  }
});