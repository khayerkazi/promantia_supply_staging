/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, GCNV, $ */

(function () {

  enyo.kind({
    name: 'DSIREF.UI.ModalSearchRefund.SearchHeader',
    kind: 'OB.UI.ScrollableTableHeader',
    events: {
      onSearchAction: '',
      onClearAction: ''
    },
    components: [{
      style: 'padding: 10px;',
      components: [{
        style: 'display: table;',
        components: [{
          style: 'display: table-cell; width: 100%;',
          components: [{
            kind: 'OB.UI.SearchInputAutoFilter',
            minLengthToSearch: 3,
            name: 'filterText',
            style: 'width: 100%',
            onFiltered: 'searchAction'
          }]
        }, {
          style: 'display: table-cell;',
          components: [{
            kind: 'OB.UI.SmallButton',
            classes: 'btnlink-gray btn-icon-small btn-icon-clear',
            style: 'width: 100px; margin: 0px 5px 8px 19px;',
            ontap: 'clearAction'
          }]
        }, {
          style: 'display: table-cell;',
          components: [{
            kind: 'OB.UI.SmallButton',
            classes: 'btnlink-yellow btn-icon-small btn-icon-search',
            style: 'width: 100px; margin: 0px 0px 8px 5px;',
            ontap: 'searchAction'
          }]
        }]
      }]
    }],
    clearAction: function () {
      this.$.filterText.setValue('');
      this.doClearAction();
    },
    searchAction: function () {
      var filter = {};
      if (this.$.filterText.getValue() === '') {
        //
      } else {
        filter.documentno = this.$.filterText.getValue();
      }
      this.doSearchAction({
        filter: filter
      });
    }
  });


  enyo.kind({
    name: 'DSIREF.UI.ModalSearchRefund.RenderRefundHeader',
    kind: 'OB.UI.SelectButton',
    components: [{
      name: 'line',
      style: 'line-height: 23px;width: 100%;',
      components: [{
        components: [{
          style: 'float: left; text-align: left; width: 60%;',
          name: 'refundDocNo'
        }, {
          style: 'float: left; text-align:left; width: 29%;',
          name: 'grossAmount'
        }, {
          style: 'clear:both;'
        }]
      }, {
        style: 'color: #888888',
        name: 'date'
      }, {
        style: 'color: #888888',
        name: 'reason'
      }, {
        style: 'clear: both;'
      }]
    }],
    initComponents: function () {
      this.inherited(arguments);
      this.$.refundDocNo.setContent("Doc No: " + this.model.get('documentNo'));
      this.$.grossAmount.setContent("Gross: " + OB.I18N.formatCurrency(this.model.get('grandTotalAmount')));
      this.$.date.setContent("Date: " + OB.I18N.formatDate(new Date(this.model.get('creationDate'))));
      this.$.reason.setContent("Reason: " + this.model.get('returnReason'));
    }
  });

  enyo.kind({
    name: 'DSIREF.UI.ModalSearchRefunds',
    kind: 'OB.UI.Modal',
    topPosition: '125px',
    events: {
      onHideThisPopup: '',
      onShowPopup: '',
      onAddProduct: ''
    },
    handlers: {
      onSearchAction: 'searchAction',
      onClearAction: 'clearAction',
      onChangePaidReceipt: 'changePaidReceipt'
    },
    changedParams: function (value) {},
    body: {
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          style: 'border-bottom: 1px solid #cccccc;',
          classes: 'row-fluid',
          components: [{
            classes: 'span12',
            components: [{
              name: 'listRefunds',
              kind: 'OB.UI.ScrollableTable',
              scrollAreaMaxHeight: '400px',
              renderHeader: 'DSIREF.UI.ModalSearchRefund.SearchHeader',
              renderLine: 'DSIREF.UI.ModalSearchRefund.RenderRefundHeader',
              renderEmpty: 'OB.UI.RenderEmpty'
            }]
          }]
        }]
      }]
    },
    clearAction: function (inSender, inEvent) {
      this.refundsHeaderCollection.reset();
      return true;
    },
    searchAction: function (inSender, inEvent) {
      var me = this;
      var filter = inEvent && inEvent.filter ? inEvent.filter : {};
      filter.email = this.model.get('order').get('syncEmail');
      filter.mobile = this.model.get('order').get('rCMobileNo');
      filter.landlineno = this.model.get('order').get('syncLandline');
      OB.UTIL.showLoading(true);
      DSIREF.utils.service(DSIREF.global.listRefundsHeaderService, {
        filter: filter
      }, function (result) {
        me.refundsHeaderCollection.reset(result);
        OB.UTIL.showLoading(false);
      }, function (error) {
        OB.UTIL.showLoading(false);
        OB.UTIL.showConfirmation.display('Error', 'An error happened retrieving credit notes: ' + error.message);
      });
      return true;
    },
    changePaidReceipt: function (inSender, inEvent) {
      this.model.get('orderList').addPaidReceipt(inEvent.newPaidReceipt);
      return true;
    },
    executeOnShow: function () {
      //default filter
      //by date
      //by customer
      this.clearAction();
      this.searchAction();
      //serach using default filter
      return true;
    },
    processRefund: function (refund) {
      var me = this;
      var paymentMethodsForRefundArr;
      var cashManagementDropEventsArr;
      var cashManagementTransaction = new Backbone.Model();
      cashManagementTransaction.set('refund', refund);
      paymentMethodsForRefundArr = _.filter(OB.MobileApp.model.get('payments'), function (posPayments) {
        return posPayments.paymentMethod.dsirefIsusedforrefund;
      });
      if (paymentMethodsForRefundArr.length !== 1) {
        OB.UTIL.showConfirmation.display('Error', 'One payment method must exist in the terminal to manage refunds');
        return true;
      } else {
        if (paymentMethodsForRefundArr[0] && OB.UTIL.isNullOrUndefined(paymentMethodsForRefundArr[0].paymentMethod.dsirefObretcoCmevents)) {
          OB.UTIL.showConfirmation.display('Error', 'Cash Management event for refund must be defined.');
          return true;
        }
        cashManagementTransaction.set('paymentMethod', paymentMethodsForRefundArr[0]);
      }

      cashManagementDropEventsArr = _.filter(OB.MobileApp.model.get('cashMgmtDropEvents'), function (event) {
        //TODO: load from terminal
        return event.id === cashManagementTransaction.get('paymentMethod').paymentMethod.dsirefObretcoCmevents && event.type === 'drop';
      });
      if (cashManagementDropEventsArr.length !== 1) {
        OB.UTIL.showConfirmation.display('Error', 'One cash Management event should be selected for refunds');
        return true;
      } else {
        cashManagementTransaction.set('cashManagementEvent', cashManagementDropEventsArr[0]);
      }

      cashManagementTransaction.set('amount', refund.get('grandTotalAmount'));
      OB.UTILS.CashManagementUtils.addCashManagementTransaction(cashManagementTransaction, function (result) {
        debugger;
        if (result === null) {
          OB.UTIL.showConfirmation.display('Success!', 'DONE');
        } else {
          var components = [];
          var strWarnings = "";
          if (result && result.warnings && result.warnings.length > 0) {
            components.push({
              content: 'Refund processed succesfully with some warns',
              style: 'font-weight:bold; margin-bottom: 5px;'
            });
            _.each(result.warnings, function (warn, index) {
              if (warn && warn.msg) {
                components.push({
                  content: (index + 1) + ": " + warn.msg,
                  style: 'text-align: left; margin: 10px;'
                });
              }
            });
            OB.UTIL.showConfirmation.display('Success!', components);
          } else {
            OB.UTIL.showConfirmation.display('Success!', 'DONE');
          }
        }
        me.doHideThisPopup();
      }, function (e) {
        OB.UTIL.showConfirmation.display('Error', e);
        me.doHideThisPopup();
      }, {
        ticketTemplate: new DSIREF.Print.Refund()
      });
    },
    init: function (model) {
      this.model = model;

      this.refundsHeaderCollection = new DSIREF.Collection.RefundHeaderCollection();
      this.$.body.$.listRefunds.setCollection(this.refundsHeaderCollection);


      this.refundsHeaderCollection.on('click', function (model) {
        var me = this;
        OB.UTIL.showConfirmation.display('Proceed with refund', 'Do you want to proceed with the refund?', [{
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          isConfirmButton: true,
          action: function () {
            me.processRefund(model);
          }
        }, {
          label: OB.I18N.getLabel('OBMOBC_LblCancel')
        }], {
          onShowFunction: function (dialog) {
            setTimeout(function () {
              dialog.applyStyle('z-index', 1000);
            }, 200);
          }
        });
      }, this);
    },
    initComponents: function () {
      this.header = OB.I18N.getLabel('DSIREF_ModalSearchRefund_Header');
      this.inherited(arguments);
    }
  });
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'DSIREF.UI.ModalSearchRefunds',
    name: 'DSIREF_ModalSearchRefunds'
  });

}());