/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo */

enyo.kind({
  kind: 'OB.UI.ModalAction',
  name: 'OB.OBPOSPointOfSale.UI.Modals.modalEnoughCredit',
  bodyContent: {
    name: 'popupmessage',
    content: ''
  },
  bodyButtons: {
    components: [{
      kind: 'OB.OBPOSPointOfSale.UI.Modals.modalEnoughCredit.Components.apply_button'
    }, {
      kind: 'OB.OBPOSPointOfSale.UI.Modals.modalEnoughCredit.Components.cancel_button'
    }]
  },
  executeOnShow: function () {
    var pendingQty = this.args.order.getPending();
    var bpName = this.args.order.get('bp').get('_identifier');
    var selectedPaymentMethod = this.args.order.selectedPayment;
    var currSymbol;
    var rate = 1,
        i;
    var paymentList = OB.POS.modelterminal.get('payments');
    for (i = 0; i < paymentList.length; i++) {
      if (paymentList[i].payment.searchKey === selectedPaymentMethod) {
        rate = paymentList[i].mulrate;
        currSymbol = paymentList[i].symbol;
      }
    }
    this.setHeader(OB.I18N.getLabel('OBPOS_enoughCreditHeader'));
    if (this.args.message) {
      this.$.bodyContent.$.popupmessage.setContent(OB.I18N.getLabel(this.args.message));
    } else if (this.args.order.get('orderType') === 1) {
      this.$.bodyContent.$.popupmessage.setContent(OB.I18N.getLabel('OBPOS_enoughCreditReturnBody', [OB.I18N.formatCurrency(pendingQty * rate) + " " + currSymbol, bpName]));
    } else {
      this.$.bodyContent.$.popupmessage.setContent(OB.I18N.getLabel('OBPOS_enoughCreditBody', [OB.I18N.formatCurrency(pendingQty * rate) + " " + currSymbol, bpName]));
    }
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OB.OBPOSPointOfSale.UI.Modals.modalEnoughCredit.Components.apply_button',
  i18nContent: 'OBPOS_LblUseCredit',
  isDefaultAction: true,
  init: function (model) {
    this.model = model;
  },
  tap: function () {
    function error(tx) {
      OB.UTIL.showError("OBDAL error: " + tx);
    }

    this.doHideThisPopup();
    this.model.get('order').set('paidOnCredit', true);
    this.model.get('order').trigger('paymentDone');
    this.allowOpenDrawer = false;
    var payments = this.model.get('order').get('payments');
    var me = this;

    payments.each(function (payment) {
      if (payment.get('allowOpenDrawer') || payment.get('isCash')) {
        me.allowOpenDrawer = true;
      }
    });

    if (this.allowOpenDrawer) {
      OB.POS.hwserver.openDrawer({
        openFirst: false,
        receipt: this.model.get('order')
      }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
    }
    var bp = this.model.get('order').get('bp');
    var bpCreditUsed = this.model.get('order').get('bp').get('creditUsed');
    var totalPending = this.model.get('order').getPending();

    if (this.parent.parent.parent.parent.args.order.get('gross') < 0) {
      bp.set('creditUsed', bpCreditUsed - totalPending);
    } else {
      bp.set('creditUsed', bpCreditUsed + totalPending);
    }
    OB.Dal.save(bp, null, error);
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OB.OBPOSPointOfSale.UI.Modals.modalEnoughCredit.Components.cancel_button',
  i18nContent: 'OBMOBC_LblCancel',
  tap: function () {
    this.doHideThisPopup();
  }
});

enyo.kind({
  kind: 'OB.UI.ModalInfo',
  name: 'OB.OBPOSPointOfSale.UI.Modals.modalNotEnoughCredit',
  style: 'background-color: #EBA001;',
  i18nHeader: 'OBPOS_notEnoughCreditHeader',
  isDefaultAction: true,
  executeOnShow: function () {
    if (this.args) {
      this.$.bodyContent.$.popupmessage.setContent(OB.I18N.getLabel('OBPOS_notEnoughCreditBody', [this.args.bpName, this.args.actualCredit]));
    }
  },
  bodyContent: {
    name: 'popupmessage',
    content: ''
  }
});