/*
 ************************************************************************************
 * Copyright (C) 2012-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global $ Backbone enyo _ localStorage */

OB.OBPOSPointOfSale = OB.OBPOSPointOfSale || {};
OB.OBPOSPointOfSale.Model = OB.OBPOSPointOfSale.Model || {};
OB.OBPOSPointOfSale.UI = OB.OBPOSPointOfSale.UI || {};

//Window model
OB.OBPOSPointOfSale.Model.PointOfSale = OB.Model.TerminalWindowModel.extend({
  models: [{
    generatedModel: true,
    modelName: 'TaxRate'
  }, {
    generatedModel: true,
    modelName: 'TaxZone'
  },
  OB.Model.Product, OB.Model.ProductCategory, OB.Model.BusinessPartner, OB.Model.BPCategory, OB.Model.BPLocation, OB.Model.Order, OB.Model.DocumentSequence, OB.Model.ChangedBusinessPartners, OB.Model.ChangedBPlocation,
  {
    generatedModel: true,
    modelName: 'Discount'
  }, {
    generatedModel: true,
    modelName: 'DiscountFilterBusinessPartner'
  }, {
    generatedModel: true,
    modelName: 'DiscountFilterBusinessPartnerGroup'
  }, {
    generatedModel: true,
    modelName: 'DiscountFilterProduct'
  }, {
    generatedModel: true,
    modelName: 'DiscountFilterProductCategory'
  }, {
    generatedModel: true,
    modelName: 'DiscountFilterRole'
  },
  OB.Model.CurrencyPanel, OB.Model.SalesRepresentative, OB.Model.ProductCharacteristic, OB.Model.Brand, OB.Model.ProductChValue, OB.Model.ReturnReason, OB.Model.CashUp, OB.Model.OfflinePrinter, OB.Model.PaymentMethodCashUp, OB.Model.TaxCashUp],

  loadUnpaidOrders: function () {
    // Shows a modal window with the orders pending to be paid
    var orderlist = this.get('orderList'),
        criteria = {
        'hasbeenpaid': 'N',
        'session': OB.POS.modelterminal.get('session')
        };
    OB.Dal.find(OB.Model.Order, criteria, function (ordersNotPaid) { //OB.Dal.find success
      var currentOrder = {},
          loadOrderStr;
      if (!ordersNotPaid || ordersNotPaid.length === 0) {
        // If there are no pending orders,
        //  add an initial empty order
        orderlist.addFirstOrder();
      } else {
        // The order object is stored in the json property of the row fetched from the database
        orderlist.reset(ordersNotPaid.models);
        // At this point it is sure that there exists at least one order
        currentOrder = ordersNotPaid.models[0];
        orderlist.load(currentOrder);
        loadOrderStr = OB.I18N.getLabel('OBPOS_Order') + currentOrder.get('documentNo') + OB.I18N.getLabel('OBPOS_Loaded');
        OB.UTIL.showAlert.display(loadOrderStr, OB.I18N.getLabel('OBPOS_Info'));
      }
    }, function () { //OB.Dal.find error
      // If there is an error fetching the pending orders,
      // add an initial empty order
      orderlist.addFirstOrder();
    });
  },

  loadCheckedMultiorders: function () {
    // Shows a modal window with the orders pending to be paid
    var checkedMultiOrders, multiOrderList = this.get('multiOrders').get('multiOrdersList'),
        criteria = {
        'hasbeenpaid': 'N',
        'session': OB.POS.modelterminal.get('session')
        };
    OB.Dal.find(OB.Model.Order, criteria, function (possibleMultiOrder) { //OB.Dal.find success
      if (possibleMultiOrder && possibleMultiOrder.length > 0) {
        checkedMultiOrders = _.compact(possibleMultiOrder.map(function (e) {
          if (e.get('checked')) {
            return e;
          }
        }));
        //The order object is stored in the json property of the row fetched from the database
        multiOrderList.reset(checkedMultiOrders);
      }
    }, function () {
      // If there is an error fetching the checked orders of multiorders,
      //OB.Dal.find error
    });
  },
  isValidMultiOrderState: function () {
    if (this.get('leftColumnViewManager') && this.get('multiOrders')) {
      return this.get('leftColumnViewManager').isMultiOrder() && this.get('multiOrders').hasDataInList();
    }
    return false;
  },
  getPending: function () {
    if (this.get('leftColumnViewManager').isOrder()) {
      return this.get('order').getPending();
    } else {
      return this.get('multiOrders').getPending();
    }
  },
  getChange: function () {
    if (this.get('leftColumnViewManager').isOrder()) {
      return this.get('order').getChange();
    } else {
      return this.get('multiOrders').getChange();
    }
  },
  getTotal: function () {
    if (this.get('leftColumnViewManager').isOrder()) {
      return this.get('order').getTotal();
    } else {
      return this.get('multiOrders').getTotal();
    }
  },
  getPayment: function () {
    if (this.get('leftColumnViewManager').isOrder()) {
      return this.get('order').getPayment();
    } else {
      return this.get('multiOrders').getPayment();
    }
  },
  addPayment: function (payment) {
    var modelToIncludePayment;

    if (this.get('leftColumnViewManager').isOrder()) {
      modelToIncludePayment = this.get('order');
    } else {
      modelToIncludePayment = this.get('multiOrders');
    }

    modelToIncludePayment.addPayment(payment);
  },
  init: function () {
    var receipt = new OB.Model.Order(),
        auxReceipt = new OB.Model.Order(),
        i, j, k, amtAux, amountToPay, ordersLength, multiOrders = new OB.Model.MultiOrders(),
        me = this,
        iter, isNew = false,
        discounts, ordersave, customersave, customeraddrsave, taxes, orderList, hwManager, ViewManager, LeftColumnViewManager, LeftColumnCurrentView, SyncReadyToSendFunction, auxReceiptList = [];


    function success() {
      return true;
    }

    function error() {
      OB.UTIL.showError('Error removing');
    }
    this.set('filter', []);
    this.set('brandFilter', []);

    function searchCurrentBP() {
      var errorCallback = function (tx, error) {
          OB.UTIL.showError("OBDAL error while getting BP info: " + error);
          };

      function successCallbackBPs(dataBps) {
        var partnerAddressId = OB.MobileApp.model.get('terminal').partnerAddress,
            successCallbackBPLoc;

        if (dataBps) {
          if (partnerAddressId && dataBps.get('locId') !== partnerAddressId) {
            // Set default location
            successCallbackBPLoc = function (bpLoc) {
              dataBps.set('locId', bpLoc.get('id'));
              dataBps.set('locName', bpLoc.get('name'));
              OB.POS.modelterminal.set('businessPartner', dataBps);
              me.loadUnpaidOrders();
            };
            OB.Dal.get(OB.Model.BPLocation, partnerAddressId, successCallbackBPLoc, errorCallback);
          } else {
            OB.POS.modelterminal.set('businessPartner', dataBps);
            me.loadUnpaidOrders();
          }
        }
      }
      OB.Dal.get(OB.Model.BusinessPartner, OB.POS.modelterminal.get('businesspartner'), successCallbackBPs, errorCallback);
    }

    //Because in terminal we've the BP id and we want to have the BP model.
    //In this moment we can ensure data is already loaded in the local database
    searchCurrentBP();

    ViewManager = Backbone.Model.extend({
      defaults: {
        currentWindow: {
          name: 'mainSubWindow',
          params: []
        }
      },
      initialize: function () {}
    });
    LeftColumnViewManager = Backbone.Model.extend({
      defaults: {
        currentView: {}
      },
      initialize: function () {
        this.on('change:currentView', function (changedModel) {
          localStorage.setItem('leftColumnCurrentView', JSON.stringify(changedModel.get('currentView')));
          this.trigger(changedModel.get('currentView').name);
        }, this);
      },
      setOrderMode: function (parameters) {
        this.set('currentView', {
          name: 'order',
          params: parameters
        });
        localStorage.setItem('leftColumnCurrentView', JSON.stringify(this.get('currentView')));
      },
      isOrder: function () {
        if (this.get('currentView').name === 'order') {
          return true;
        }
        return false;
      },
      setMultiOrderMode: function (parameters) {
        this.set('currentView', {
          name: 'multiorder',
          params: parameters
        });
      },
      isMultiOrder: function () {
        if (this.get('currentView').name === 'multiorder') {
          return true;
        }
        return false;
      }
    });
    this.set('order', receipt);
    orderList = new OB.Collection.OrderList(receipt);
    OB.MobileApp.model.orderList = orderList;
    this.set('orderList', orderList);
    this.set('customer', new OB.Model.BusinessPartner());
    this.set('customerAddr', new OB.Model.BPLocation());
    this.set('multiOrders', multiOrders);

    this.get('multiOrders').on('paymentAccepted', function () {
      OB.UTIL.showLoading(true);
      ordersLength = this.get('multiOrders').get('multiOrdersList').length;

      function readyToSendFunction() {
        //this function is executed when all orders are ready to be sent
        me.get('leftColumnViewManager').setOrderMode();
        if (me.get('orderList').length > _.filter(me.get('multiOrders').get('multiOrdersList').models, function (order) {
          return !order.get('isLayaway');
        }).length) {
          me.get('orderList').addNewOrder();
        }
      }

      function prepareToSendCallback(order) {
        auxReceipt = new OB.Model.Order();
        auxReceipt.clearWith(order);

        if (order.get('orderType') !== 2 && order.get('orderType') !== 3) {
          var negativeLines = _.filter(order.get('lines').models, function (line) {
            return line.get('qty') < 0;
          }).length;
          if (negativeLines === order.get('lines').models.length) {
            order.setOrderType('OBPOS_receipt.return', OB.DEC.One, {
              applyPromotions: false,
              saveOrder: false
            });
          } else {
            order.setOrderType('', OB.DEC.Zero, {
              applyPromotions: false,
              saveOrder: false
            });
          }
        }
        me.get('multiOrders').trigger('closed', order);
        me.get('multiOrders').trigger('print', order, {
          offline: true
        }); // to guaranty execution order
        SyncReadyToSendFunction();

        auxReceiptList.push(auxReceipt);
        if (auxReceiptList.length === me.get('multiOrders').get('multiOrdersList').length) {
          OB.UTIL.cashUpReport(auxReceiptList);
        }
      }

      //this var is a function (copy of the above one) which is called by every items, but it is just executed once (when ALL items has called to it)
      SyncReadyToSendFunction = _.after(this.get('multiOrders').get('multiOrdersList').length, readyToSendFunction);

      for (j = 0; j < ordersLength; j++) {
        //Create the negative payment for change
        iter = this.get('multiOrders').get('multiOrdersList').at(j);
        amountToPay = !_.isUndefined(iter.get('amountToLayaway')) && !_.isNull(iter.get('amountToLayaway')) ? iter.get('amountToLayaway') : OB.DEC.sub(iter.get('gross'), iter.get('payment'));
        while (((_.isUndefined(iter.get('amountToLayaway')) || iter.get('amountToLayaway') > 0) && iter.get('gross') > iter.get('payment')) || (iter.get('amountToLayaway') > 0)) {
          for (i = 0; i < this.get('multiOrders').get('payments').length; i++) {
            var payment = this.get('multiOrders').get('payments').at(i),
                paymentMethod = OB.POS.terminal.terminal.paymentnames[payment.get('kind')];
            //FIXME:Change is always given back in store currency
            if (this.get('multiOrders').get('change') > 0 && paymentMethod.paymentMethod.iscash) {
              payment.set('origAmount', OB.DEC.sub(payment.get('origAmount'), this.get('multiOrders').get('change')));
              this.get('multiOrders').set('change', OB.DEC.Zero);
            }
            if (payment.get('origAmount') <= amountToPay) {
              var bigDecAmount = new BigDecimal(String(OB.DEC.mul(payment.get('origAmount'), paymentMethod.mulrate)));
              iter.addPayment(new OB.Model.PaymentLine({
                'kind': payment.get('kind'),
                'name': payment.get('name'),
                'amount': OB.DEC.toNumber(bigDecAmount),
                'rate': paymentMethod.rate,
                'mulrate': paymentMethod.mulrate,
                'isocode': paymentMethod.isocode,
                'allowOpenDrawer': payment.get('allowopendrawer'),
                'isCash': payment.get('iscash'),
                'openDrawer': payment.get('openDrawer'),
                'printtwice': payment.get('printtwice')
              }));
              if (!_.isUndefined(iter.get('amountToLayaway')) && !_.isNull(iter.get('amountToLayaway'))) {
                iter.set('amountToLayaway', OB.DEC.sub(iter.get('amountToLayaway'), payment.get('origAmount')));
              }
              this.get('multiOrders').get('payments').remove(this.get('multiOrders').get('payments').at(i));
              amountToPay = !_.isUndefined(iter.get('amountToLayaway')) && !_.isNull(iter.get('amountToLayaway')) ? iter.get('amountToLayaway') : OB.DEC.sub(iter.get('gross'), iter.get('payment'));
            } else {
              var bigDecAmountAux;
              if (j === this.get('multiOrders').get('multiOrdersList').length - 1 && !paymentMethod.paymentMethod.iscash) {
                bigDecAmountAux = new BigDecimal(String(payment.get('origAmount')));
                amtAux = OB.DEC.toNumber(bigDecAmountAux);
                this.get('multiOrders').get('payments').at(i).set('origAmount', OB.DEC.sub(this.get('multiOrders').get('payments').at(i).get('origAmount'), payment.get('origAmount')));
              } else {
                bigDecAmountAux = new BigDecimal(String(OB.DEC.mul(amountToPay, paymentMethod.mulrate)));
                amtAux = OB.DEC.toNumber(bigDecAmountAux);
                this.get('multiOrders').get('payments').at(i).set('origAmount', OB.DEC.sub(this.get('multiOrders').get('payments').at(i).get('origAmount'), amountToPay));
              }

              iter.addPayment(new OB.Model.PaymentLine({
                'kind': payment.get('kind'),
                'name': payment.get('name'),
                'amount': amtAux,
                'rate': paymentMethod.rate,
                'mulrate': paymentMethod.mulrate,
                'isocode': paymentMethod.isocode,
                'allowOpenDrawer': payment.get('allowopendrawer'),
                'isCash': payment.get('iscash'),
                'openDrawer': payment.get('openDrawer'),
                'printtwice': payment.get('printtwice')
              }));
              if (!_.isUndefined(iter.get('amountToLayaway')) && !_.isNull(iter.get('amountToLayaway'))) {
                iter.set('amountToLayaway', OB.DEC.sub(iter.get('amountToLayaway'), amtAux));
              }
              amountToPay = !_.isUndefined(iter.get('amountToLayaway')) && !_.isNull(iter.get('amountToLayaway')) ? iter.get('amountToLayaway') : OB.DEC.sub(iter.get('gross'), iter.get('payment'));
              break;
            }
          }
        }
        iter.prepareToSend(prepareToSendCallback);
      }

    }, this);

    customersave = new OB.DATA.CustomerSave(this);
    customeraddrsave = new OB.DATA.CustomerAddrSave(this);

    this.set('leftColumnViewManager', new LeftColumnViewManager());
    this.set('subWindowManager', new ViewManager());
    discounts = new OB.DATA.OrderDiscount(receipt);
    ordersave = new OB.DATA.OrderSave(this);
    taxes = new OB.DATA.OrderTaxes(receipt);

    OB.MobileApp.model.runSyncProcess(function () {
      me.loadCheckedMultiorders();
    }, function () {
      me.loadCheckedMultiorders();
    });

    receipt.on('paymentAccepted', function () {
      OB.UTIL.showLoading(true);
      receipt.prepareToSend(function () {
        //Create the negative payment for change
        var oldChange = receipt.get('change');
        var clonedCollection = new Backbone.Collection();
        if (receipt.get('orderType') !== 2 && receipt.get('orderType') !== 3) {
          var negativeLines = _.filter(receipt.get('lines').models, function (line) {
            return line.get('qty') < 0;
          }).length;
          if (negativeLines === receipt.get('lines').models.length) {
            receipt.setOrderType('OBPOS_receipt.return', OB.DEC.One, {
              applyPromotions: false,
              saveOrder: false
            });
          } else {
            receipt.setOrderType('', OB.DEC.Zero, {
              applyPromotions: false,
              saveOrder: false
            });
          }
        }
        if (!_.isUndefined(receipt.selectedPayment) && receipt.getChange() > 0) {
          var payment = OB.POS.terminal.terminal.paymentnames[receipt.selectedPayment];
          receipt.get('payments').each(function (model) {
            clonedCollection.add(new Backbone.Model(model.toJSON()));
          });
          if (!payment.paymentMethod.iscash) {
            payment = OB.POS.terminal.terminal.paymentnames[OB.POS.modelterminal.get('paymentcash')];
          }
          if (receipt.get('payment') >= receipt.get('gross')) {
            receipt.addPayment(new OB.Model.PaymentLine({
              'kind': payment.payment.searchKey,
              'name': payment.payment.commercialName,
              'amount': OB.DEC.sub(0, OB.DEC.mul(receipt.getChange(), payment.mulrate)),
              'rate': payment.rate,
              'mulrate': payment.mulrate,
              'isocode': payment.isocode,
              'allowOpenDrawer': payment.paymentMethod.allowopendrawer,
              'isCash': payment.paymentMethod.iscash,
              'openDrawer': payment.paymentMethod.openDrawer,
              'printtwice': payment.paymentMethod.printtwice
            }));
          }
          receipt.set('change', oldChange);
          receipt.trigger('closed', {
            callback: function () {
              receipt.get('payments').reset();
              clonedCollection.each(function (model) {
                receipt.get('payments').add(new Backbone.Model(model.toJSON()), {
                  silent: true
                });
              });
              // receipt is cloned because the receipt is deleted in the next sentence (orderList.deleteCurrent(true);)
              // so, if exists a method no synchronous (for example, hook OBPOS_PrePrint) the "receipt" has changed
              receipt.set('cloningReceipt', true);
              var orderToPrint = OB.UTIL.clone(receipt);
              receipt.set('cloningReceipt', false);
              orderToPrint.set('cloningReceipt', false);
              receipt.trigger('print', orderToPrint, {
                offline: true
              });
              orderList.deleteCurrent(true);
              OB.UTIL.showLoading(false);
            }
          });
        } else {
          receipt.trigger('closed', {
            callback: function () {
              // receipt is cloned because the receipt is deleted when event "closed" is triggered
              // so, if exists a method no synchronous, the "receipt" has changed
              receipt.set('cloningReceipt', true);
              var orderToPrint = OB.UTIL.clone(receipt);
              receipt.set('cloningReceipt', false);
              orderToPrint.set('cloningReceipt', false);
              receipt.trigger('print', orderToPrint, {
                offline: true
              });
              orderList.deleteCurrent(true);
              OB.UTIL.showLoading(false);
            }
          });
        }
      });
    }, this);

    receipt.on('paymentDone', function (openDrawer) {
      if (receipt.overpaymentExists()) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOS_OverpaymentWarningTitle'), OB.I18N.getLabel('OBPOS_OverpaymentWarningBody'), [{
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          isConfirmButton: true,
          action: function () {
            if (openDrawer) {
              OB.POS.hwserver.openDrawer({
                openFirst: false,
                receipt: receipt
              }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
            }
            receipt.trigger('paymentAccepted');
          }
        }, {
          label: OB.I18N.getLabel('OBMOBC_LblCancel')
        }]);
      } else if ((OB.DEC.abs(receipt.getPayment()) !== OB.DEC.abs(receipt.getGross())) && (!receipt.isLayaway() && !receipt.get('paidOnCredit'))) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOS_PaymentAmountDistinctThanReceiptAmountTitle'), OB.I18N.getLabel('OBPOS_PaymentAmountDistinctThanReceiptAmountBody'), [{
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          isConfirmButton: true,
          action: function () {
            receipt.trigger('paymentAccepted');
          }
        }, {
          label: OB.I18N.getLabel('OBMOBC_LblCancel')
        }]);
      } else {
        if (openDrawer) {
          OB.POS.hwserver.openDrawer({
            openFirst: true,
            receipt: receipt
          }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
        }
        receipt.trigger('paymentAccepted');
      }
    }, this);

    this.get('multiOrders').on('paymentDone', function (openDrawer) {
      var me = this,
          paymentstatus = this.get('multiOrders');
      if (OB.DEC.compare(OB.DEC.sub(paymentstatus.get('payment'), paymentstatus.get('total'))) > 0) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOS_OverpaymentWarningTitle'), OB.I18N.getLabel('OBPOS_OverpaymentWarningBody'), [{
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          isConfirmButton: true,
          action: function () {
            if (openDrawer) {
              OB.POS.hwserver.openDrawer({
                openFirst: false,
                receipt: me.get('multiOrders')
              }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
            }
            me.get('multiOrders').trigger('paymentAccepted');
          }
        }, {
          label: OB.I18N.getLabel('OBMOBC_LblCancel')
        }]);
      } else {
        if (openDrawer) {
          OB.POS.hwserver.openDrawer({
            openFirst: true,
            receipt: me.get('multiOrders')
          }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
        }
        this.get('multiOrders').trigger('paymentAccepted');
      }
    }, this);

    this.printReceipt = new OB.OBPOSPointOfSale.Print.Receipt(this);
    this.printLine = new OB.OBPOSPointOfSale.Print.ReceiptLine(receipt);

    // Listening events that cause a discount recalculation
    receipt.get('lines').on('add change:qty change:price', function (line) {
      if (!receipt.get('isEditable')) {
        return;
      }
      //When we do not want to launch promotions process (Not apply or remove discounts)
      if (receipt.get('skipApplyPromotions') || line.get('skipApplyPromotions')) {
        return;
      }
      OB.Model.Discounts.applyPromotions(receipt, line);
    }, this);

    receipt.get('lines').on('remove', function () {
      if (!receipt.get('isEditable')) {
        return;
      }
      OB.Model.Discounts.applyPromotions(receipt);
    });

    receipt.on('change:bp', function (line) {
      if (!receipt.get('isEditable') || receipt.get('lines').length === 0) {
        return;
      }
      OB.Model.Discounts.applyPromotions(receipt);
    }, this);
    receipt.on('voidLayaway', function () {
      var process = new OB.DS.Process('org.openbravo.retail.posterminal.ProcessVoidLayaway'),
          auxReceipt = new OB.Model.Order();
      auxReceipt.clearWith(receipt);
      process.exec({
        order: receipt
      }, function (data, message) {
        if (data && data.exception) {
          OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgErrorVoidLayaway'));
        } else {
          auxReceipt.calculateTaxes = receipt.calculateTaxes;
          auxReceipt.calculateTaxes(function () {
            auxReceipt.adjustPrices();
            OB.UTIL.cashUpReport(auxReceipt);
          });
          OB.Dal.remove(receipt, null, function (tx, err) {
            OB.UTIL.showError(err);
          });
          receipt.trigger('print');
          if (receipt.get('layawayGross')) {
            receipt.set('layawayGross', null);
          }
          orderList.deleteCurrent();
          receipt.trigger('change:gross', receipt);

          OB.UTIL.showSuccess(OB.I18N.getLabel('OBPOS_MsgSuccessVoidLayaway'));
        }
      }, function () {
        OB.UTIL.showError(OB.I18N.getLabel('OBPOS_OfflineWindowRequiresOnline'));
      });
    }, this);
  },

  /**
   * This method is invoked before paying a ticket, it is intended to do global
   * modifications in the ticket with OBPOS_PrePaymentHook hook, after this hook
   * execution checkPaymentApproval is invoked
   */
  completePayment: function () {
    var me = this;
    OB.MobileApp.model.hookManager.executeHooks('OBPOS_PrePaymentHook', {
      context: this
    }, function () {
      me.checkPaymentApproval();
    });
  },

  /**
   * Hooks for OBPOS_CheckPaymentApproval can modify args.approved to check if
   * payment is approved. In case value is true the process will continue, if not
   * it is aborted
   */
  checkPaymentApproval: function () {
    var me = this;
    OB.MobileApp.model.hookManager.executeHooks('OBPOS_CheckPaymentApproval', {
      approvals: [],
      context: this
    }, function (args) {
      var negativeLines = _.filter(me.get('order').get('lines').models, function (line) {
        return line.get('qty') < 0;
      }).length;
      if (negativeLines > 0 && !OB.POS.modelterminal.get('permissions')['OBPOS_approval.returns']) {
        args.approvals.push('OBPOS_approval.returns');
      }
      if (args.approvals.length > 0) {
        OB.UTIL.Approval.requestApproval(
        me, args.approvals, function (approved, supervisor, approvalType) {
          if (approved) {
            me.trigger('approvalChecked', {
              approved: args.approved
            });
          }
        });
      } else {
        me.trigger('approvalChecked', {
          approved: true
        });
      }
    });
  },

  /**
   * Approval final stage. Where approvalChecked event is triggered, with approved
   * property set to true or false regarding if approval was finally granted. In
   * case of granted approval, the approval is added to the order so it can be saved
   * in backend for audit purposes.
   */
  approvedRequest: function (approved, supervisor, approvalType, callback) {
    var order = this.get('order'),
        newApprovals = [],
        approvals, approval, i, date;


    approvals = order.get('approvals') || [];
    if (!Array.isArray(approvalType)) {
      approvalType = [approvalType];
    }

    _.each(approvals, function (appr) {
      var results;
      results = _.find(approvalType, function (apprType) {
        return apprType === appr.approvalType;
      });

      if (_.isUndefined(results)) {
        newApprovals.push(appr);
      }

    });

    if (approved) {
      date = new Date();
      date = date.getTime();
      for (i = 0; i < approvalType.length; i++) {
        approval = {
          approvalType: approvalType[i],
          userContact: supervisor.get('id'),
          created: (new Date()).getTime()
        };
        newApprovals.push(approval);
      }
      order.set('approvals', newApprovals);
    }


    this.trigger('approvalChecked', {
      approved: approved
    });
    if (enyo.isFunction(callback)) {
      callback(approved, supervisor, approvalType);
    }
  }
});