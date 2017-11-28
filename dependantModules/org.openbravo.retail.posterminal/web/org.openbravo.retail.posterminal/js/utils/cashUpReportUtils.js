/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B,_*/

(function () {

  OB = window.OB || {};
  OB.UTILS = window.OB.UTILS || {};

  function findAndSave(cashuptaxes, i, finishCallback) {

    if (i < cashuptaxes.length) {
      OB.Dal.find(OB.Model.TaxCashUp, {
        'cashup_id': cashuptaxes[i].cashupID,
        'name': cashuptaxes[i].taxName,
        'orderType': cashuptaxes[i].taxOrderType
      }, function (tax) {
        if (tax.length === 0) {
          OB.Dal.save(new OB.Model.TaxCashUp({
            name: cashuptaxes[i].taxName,
            amount: cashuptaxes[i].taxAmount,
            orderType: cashuptaxes[i].taxOrderType,
            cashup_id: cashuptaxes[i].cashupID
          }), function () {
            findAndSave(cashuptaxes, i + 1, finishCallback);
          }, null);
        } else {
          tax.at(0).set('amount', OB.DEC.add(tax.at(0).get('amount'), cashuptaxes[i].taxAmount));
          OB.Dal.save(tax.at(0), function () {
            findAndSave(cashuptaxes, i + 1, finishCallback);
          }, null);
        }
      });
    } else {
      if (finishCallback) {
        finishCallback();
      }
    }
  }

  function updateCashUpInfo(cashUp, receipt, j, callback) {
    var cashuptaxes, order, orderType, gross, i, taxOrderType, taxAmount, auxPay;
    if (j < receipt.length) {
      order = receipt[j];
      orderType = order.get('orderType');
      if (cashUp.length !== 0) {
        _.each(order.get('lines').models, function (line) {
          if (order.get('priceIncludesTax')) {
            gross = line.get('lineGrossAmount');
          } else {
            gross = line.get('discountedGross');
          }
          //Sales order: Positive line
          if (!(order.has('isQuotation') && order.get('isQuotation'))) {
            if (line.get('qty') > 0 && orderType !== 3 && !order.get('isLayaway')) {
              cashUp.at(0).set('netSales', OB.DEC.add(cashUp.at(0).get('netSales'), line.get('net')));
              cashUp.at(0).set('grossSales', OB.DEC.add(cashUp.at(0).get('grossSales'), gross));
              //Return from customer or Sales with return: Negative line
            } else if (line.get('qty') < 0 && orderType !== 3 && !order.get('isLayaway')) {
              cashUp.at(0).set('netReturns', OB.DEC.add(cashUp.at(0).get('netReturns'), -line.get('net')));
              cashUp.at(0).set('grossReturns', OB.DEC.add(cashUp.at(0).get('grossReturns'), -gross));
              //Void Layaway
            } else if (orderType === 3) {
              if (line.get('qty') > 0) {
                cashUp.at(0).set('netSales', OB.DEC.add(cashUp.at(0).get('netSales'), -line.get('net')));
                cashUp.at(0).set('grossSales', OB.DEC.add(cashUp.at(0).get('grossSales'), -gross));
              } else {
                cashUp.at(0).set('netReturns', OB.DEC.add(cashUp.at(0).get('netReturns'), line.get('net')));
                cashUp.at(0).set('grossReturns', OB.DEC.add(cashUp.at(0).get('grossReturns'), gross));
              }
            }
          }
        });
        cashUp.at(0).set('totalRetailTransactions', OB.DEC.sub(cashUp.at(0).get('grossSales'), cashUp.at(0).get('grossReturns')));
        OB.Dal.save(cashUp.at(0), null, null);

        // group and sum the taxes
        cashuptaxes = [];
        order.get('lines').each(function (line, taxIndex) {
          var taxLines, taxLine;
          taxLines = line.get('taxLines');
          if (orderType === 1 || line.get('qty') < 0) {
            taxOrderType = 1;
          } else {
            taxOrderType = 0;
          }

          _.each(taxLines, function (taxLine) {
            if (!(order.has('isQuotation') && order.get('isQuotation'))) {
              if (line.get('qty') > 0 && orderType !== 3 && !order.get('isLayaway')) {
                taxAmount = taxLine.amount;
              } else if (line.get('qty') < 0 && orderType !== 3 && !order.get('isLayaway')) {
                taxAmount = -taxLine.amount;
              } else if (orderType === 3) {
                if (line.get('qty') > 0) {
                  taxAmount = -taxLine.amount;
                } else {
                  taxAmount = taxLine.amount;
                }
              }
            }

            if (!OB.UTIL.isNullOrUndefined(taxAmount)) {
              cashuptaxes.push({
                taxName: taxLine.name,
                taxAmount: taxAmount,
                taxOrderType: taxOrderType.toString(),
                cashupID: cashUp.at(0).get('id')
              });
            }
          });
        });

        OB.Dal.find(OB.Model.PaymentMethodCashUp, {
          'cashup_id': cashUp.at(0).get('id')
        }, function (payMthds) { //OB.Dal.find success
          _.each(order.get('payments').models, function (payment) {
            auxPay = payMthds.filter(function (payMthd) {
              return payMthd.get('searchKey') === payment.get('kind') && !payment.get('isPrePayment');
            })[0];
            if (!auxPay) { //We cannot find this payment in local database, it must be a new payment method, we skip it.
              return;
            }
            if (order.getGross() > 0 && (orderType === 0 || orderType === 2)) {
              auxPay.set('totalSales', OB.DEC.add(auxPay.get('totalSales'), payment.get('amount')));
            } else if (order.getGross() < 0 || orderType === 1) {
              auxPay.set('totalReturns', OB.DEC.add(auxPay.get('totalReturns'), payment.get('amount')));
            } else if (orderType === 3) {
              auxPay.set('totalSales', OB.DEC.sub(auxPay.get('totalSales'), payment.get('amount')));
            }
            OB.Dal.save(auxPay, null, null);
          }, this);
          findAndSave(cashuptaxes, 0, function () {
            updateCashUpInfo(cashUp, receipt, j + 1, callback);
          });
        });
      }
    } else if (typeof callback === 'function') {
      callback();
    }
  }

  OB.UTIL.cashUpReport = function (receipt, callback) {
    var auxPay, orderType, taxOrderType, taxAmount, gross;
    if (!Array.isArray(receipt)) {
      receipt = [receipt];
    }
    OB.Dal.find(OB.Model.CashUp, {
      'isbeingprocessed': 'N'
    }, function (cashUp) {
      updateCashUpInfo(cashUp, receipt, 0, callback);
    });
  };

  OB.UTIL.deleteCashUps = function (cashUpModels) {
    var deleteCallback = function (models) {
        models.each(function (model) {
          OB.Dal.remove(model, null, function (tx, err) {
            OB.UTIL.showError(err);
          });
        });
        };
    _.each(cashUpModels, function (cashup) {
      var cashUpId = cashup.get('id');
      OB.Dal.find(OB.Model.TaxCashUp, {
        cashup_id: cashUpId
      }, deleteCallback, null);
      OB.Dal.find(OB.Model.CashManagement, {
        cashup_id: cashUpId
      }, deleteCallback, null);
      OB.Dal.find(OB.Model.PaymentMethodCashUp, {
        cashup_id: cashUpId
      }, deleteCallback, null);
      OB.Dal.remove(cashup, null, function (tx, err) {
        OB.UTIL.showError(err);
      });
    });
  };

  OB.UTIL.initCashUp = function (callback) {
    var criteria = {
      'isbeingprocessed': 'N'
    },
        lastCashUpPayments;
    OB.Dal.find(OB.Model.CashUp, criteria, function (cashUp) { //OB.Dal.find success
      var uuid;
      if (cashUp.length === 0) {
        criteria = {
          'isbeingprocessed': 'Y',
          '_orderByClause': 'createdDate desc'
        };
        OB.Dal.find(OB.Model.CashUp, criteria, function (lastCashUp) {
          if (lastCashUp.length !== 0) {
            lastCashUpPayments = JSON.parse(lastCashUp.at(0).get('objToSend')).cashCloseInfo;
          }
          uuid = OB.Dal.get_uuid();
          if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.model.get('terminal'))) {
            OB.MobileApp.model.get('terminal').cashUpId = uuid;
          }
          OB.Dal.save(new OB.Model.CashUp({
            id: uuid,
            netSales: OB.DEC.Zero,
            grossSales: OB.DEC.Zero,
            netReturns: OB.DEC.Zero,
            grossReturns: OB.DEC.Zero,
            totalRetailTransactions: OB.DEC.Zero,
            createdDate: new Date(),
            userId: null,
            objToSend: null,
            isbeingprocessed: 'N'
          }), function () {
            _.each(OB.POS.modelterminal.get('payments'), function (payment) {
              var startingCash = payment.currentBalance,
                  pAux;
              if (lastCashUpPayments) {
                pAux = lastCashUpPayments.filter(function (payMthd) {
                  return payMthd.paymentTypeId === payment.payment.id;
                })[0];
                if (!OB.UTIL.isNullOrUndefined(pAux)) {
                  startingCash = pAux.paymentMethod.amountToKeep;
                }
              }
              OB.Dal.save(new OB.Model.PaymentMethodCashUp({
                id: OB.Dal.get_uuid(),
                paymentmethod_id: payment.payment.id,
                searchKey: payment.payment.searchKey,
                name: payment.payment._identifier,
                startingCash: startingCash,
                totalSales: OB.DEC.Zero,
                totalReturns: OB.DEC.Zero,
                rate: payment.rate,
                isocode: payment.isocode,
                cashup_id: uuid
              }), null, null, true);
            }, this);
            if (callback) {
              callback();
            }
          }, null, true);
        }, null, this);
      } else {
        if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.model.get('terminal'))) {
          OB.MobileApp.model.get('terminal').cashUpId = cashUp.at(0).get('id');
        }
        if (callback) {
          callback();
        }
      }
    });
  };

  OB.UTIL.calculateCurrentCash = function (callback) {
    var me = this;
    OB.Dal.find(OB.Model.CashUp, {
      'isbeingprocessed': 'N'
    }, function (cashUp) {
      OB.Dal.find(OB.Model.PaymentMethodCashUp, {
        'cashup_id': cashUp.at(0).get('id')
      }, function (payMthds) { //OB.Dal.find success
        var payMthdsCash;
        _.each(OB.POS.modelterminal.get('payments'), function (paymentType, index) {
          var cash = 0,
              auxPay = payMthds.filter(function (payMthd) {
              return payMthd.get('paymentmethod_id') === paymentType.payment.id;
            })[0];
          if (!auxPay) { //We cannot find this payment in local database, it must be a new payment method, we skip it.
            return;
          }
          auxPay.set('_id', paymentType.payment.searchKey);
          auxPay.set('isocode', paymentType.isocode);
          auxPay.set('paymentMethod', paymentType.paymentMethod);
          auxPay.set('id', paymentType.payment.id);
          OB.Dal.find(OB.Model.CashManagement, {
            'cashup_id': cashUp.at(0).get('id'),
            'paymentMethodId': paymentType.payment.id
          }, function (cashMgmts, args) {
            var startingCash = auxPay.get('startingCash'),
                rate = auxPay.get('rate'),
                totalSales = auxPay.get('totalSales'),
                totalReturns = auxPay.get('totalReturns'),
                cashMgmt = _.reduce(cashMgmts.models, function (accum, trx) {
                if (trx.get('type') === 'deposit') {
                  return OB.DEC.add(accum, trx.get('origAmount'));
                } else {
                  return OB.DEC.sub(accum, trx.get('origAmount'));
                }
              }, 0);

            var payment = OB.POS.terminal.terminal.paymentnames[paymentType.payment.searchKey];
            cash = OB.DEC.add(OB.DEC.add(startingCash, OB.DEC.add(totalSales, totalReturns)), cashMgmt);
            payment.currentCash = OB.UTIL.currency.toDefaultCurrency(payment.paymentMethod.currency, cash);
            payment.foreignCash = OB.UTIL.currency.toForeignCurrency(payment.paymentMethod.currency, cash);

            if (typeof callback === 'function') {
              callback();
            }
          }, null, {
            me: me
          });
        }, this);
      });
    });
  };
}());
