/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, _ */

OB.OBPOSPointOfSale.UI.ToolbarScan = {
  name: 'toolbarscan',
  buttons: [{
    command: 'code',
    i18nLabel: 'OBMOBC_KbCode',
    classButtonActive: 'btnactive-blue',
    idSufix: 'upcean'
  }],
  shown: function () {
    var keyboard = this.owner.owner;
    keyboard.showKeypad('basic');
    keyboard.showSidepad('sideenabled');
    keyboard.defaultcommand = 'code';
    keyboard.disableCommandKey(this, {
      disabled: true,
      commands: ['%']
    });
  }
};

OB.OBPOSPointOfSale.UI.ToolbarDiscounts = {
  name: 'toolbardiscounts',
  buttons: [],
  shown: function () {
    var keyboard = this.owner.owner;
    keyboard.showKeypad('basic');
    keyboard.showSidepad('sideenabled');
    keyboard.defaultcommand = 'line:dto';
    keyboard.disableCommandKey(this, {
      disabled: true,
      commands: ['%']
    });
  }
};


enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ToolbarPayment',
  sideButtons: [],
  published: {
    receipt: null
  },
  toolbarName: 'toolbarpayment',
  events: {
    onShowPopup: ''
  },
  handlers: {
    onShowAllButtons: 'showAllButtons',
    onCloseAllPopups: 'closeAllPopups'
  },
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.PaymentMethods',
    name: 'OBPOS_UI_PaymentMethods'
  }],
  init: function (model) {
    this.model = model;
  },
  pay: function (amount, key, name, paymentMethod, rate, mulrate, isocode, options) {
    if (options && options.percentaje) {
      var pending = this.model.getPending();
      if (mulrate && mulrate !== '1') {
        pending = OB.DEC.mul(pending, mulrate);
      }
      amount = OB.DEC.div(OB.DEC.mul(pending, amount), 100);
    }

    if (OB.DEC.compare(amount) > 0) {
      var provider, receiptToPay, me = this;
      if (this.model.get('leftColumnViewManager').isOrder()) {
        receiptToPay = this.receipt;
        if (this.receipt.get('orderType') === 0 || this.receipt.get('orderType') === 2 || this.receipt.get('isLayaway')) {
          provider = paymentMethod.paymentProvider;
        } else if (this.receipt.get('orderType') === 1 || this.receipt.get('orderType') === 3) {
          provider = paymentMethod.refundProvider;
        } else {
          provider = null;
        }
      }
      //multiorders doesn't allow to return
      if (this.model.get('leftColumnViewManager').isMultiOrder()) {
        provider = paymentMethod.paymentProvider;
        receiptToPay = this.model.get('multiOrders');
      }

      if (provider) {
        this.doShowPopup({
          popup: 'modalpayment',
          args: {
            'receipt': receiptToPay,
            'provider': provider,
            'key': key,
            'name': name,
            'paymentMethod': paymentMethod,
            'amount': amount,
            'rate': rate,
            'mulrate': mulrate,
            'isocode': isocode
          }
        });
      } else {
        this.model.addPayment(new OB.Model.PaymentLine({
          'kind': key,
          'name': name,
          'amount': amount,
          'rate': rate,
          'mulrate': mulrate,
          'isocode': isocode,
          'allowOpenDrawer': paymentMethod.allowopendrawer,
          'isCash': paymentMethod.iscash,
          'openDrawer': paymentMethod.openDrawer,
          'printtwice': paymentMethod.printtwice
        }));
      }
    }
  },
  getButtonComponent: function (sidebutton) {
    if (sidebutton.i18nLabel) {
      sidebutton.label = OB.I18N.getLabel(sidebutton.i18nLabel);
    }
    return {
      kind: 'OB.UI.BtnSide',
      btn: {
        command: sidebutton.command,
        label: sidebutton.label,
        permission: sidebutton.permission,
        definition: {
          permission: sidebutton.permission,
          stateless: sidebutton.stateless,
          action: sidebutton.action
        }
      }
    };
  },

  initComponents: function () {
    //TODO: modal payments
    var i, max, payments, paymentsdialog, paymentsbuttons, countbuttons, btncomponent, Btn, inst, cont, exactdefault, cashdefault, allpayments = {},
        me = this,
        dialogbuttons = {};

    this.inherited(arguments);

    payments = OB.POS.modelterminal.get('payments');


    paymentsdialog = payments.length + this.sideButtons.length > 5;
    paymentsbuttons = paymentsdialog ? 4 : 5;
    countbuttons = 0;

    enyo.forEach(payments, function (payment) {
      if (payment.paymentMethod.id === OB.POS.modelterminal.get('terminal').terminalType.paymentMethod) {
        exactdefault = payment;
      }
      if (payment.payment.searchKey === OB.POS.modelterminal.get('paymentcash')) {
        cashdefault = payment;
      }
      allpayments[payment.payment.searchKey] = payment;

      btncomponent = this.getButtonComponent({
        command: payment.payment.searchKey,
        label: payment.payment._identifier,
        permission: payment.payment.searchKey,
        stateless: false,
        action: function (keyboard, txt) {
          var options = {};
          if (_.last(txt) === '%') {
            options.percentaje = true;
          }
          var amount = OB.DEC.number(OB.I18N.parseNumber(txt));
          if (_.isNaN(amount)) {
            OB.UTIL.showWarning(OB.I18N.getLabel('OBPOS_NotValidNumber', [txt]));
            return;
          }
          me.pay(amount, payment.payment.searchKey, payment.payment._identifier, payment.paymentMethod, payment.rate, payment.mulrate, payment.isocode, options);
        }
      });

      if (countbuttons++ < paymentsbuttons) {
        this.createComponent(btncomponent);
      } else {
        OB.OBPOSPointOfSale.UI.PaymentMethods.prototype.sideButtons.push(btncomponent);
        dialogbuttons[payment.payment.searchKey] = payment.payment._identifier;
      }
    }, this);

    // Fallback assign of the payment for the exact command.
    exactdefault = exactdefault || cashdefault || payments[0];

    enyo.forEach(this.sideButtons, function (sidebutton) {
      btncomponent = this.getButtonComponent(sidebutton);
      if (countbuttons++ < paymentsbuttons) {
        this.createComponent(btncomponent);
      } else {
        OB.OBPOSPointOfSale.UI.PaymentMethods.prototype.sideButtons.push(btncomponent);
        dialogbuttons[sidebutton.command] = sidebutton.label;
      }
    }, this);

    while (countbuttons++ < paymentsbuttons) {
      this.createComponent({
        kind: 'OB.UI.BtnSide',
        btn: {}
      });
    }

    if (paymentsdialog) {
      this.createComponent({
        name: 'btnMore',
        toolbar: this,
        dialogbuttons: dialogbuttons,
        kind: 'OB.OBPOSPointOfSale.UI.ButtonMore'
      });
    }

    this.createComponent({
      kind: 'OB.OBPOSPointOfSale.UI.ButtonSwitch',
      keyboard: this.keyboard
    });

    this.owner.owner.addCommand('cashexact', {
      action: function (keyboard, txt) {
        if (keyboard.status && !allpayments[keyboard.status]) {
          // Is not a payment, so continue with the default path...
          keyboard.execCommand(keyboard.status, null);
        } else {
          // It is a payment...
          var exactpayment = allpayments[keyboard.status] || exactdefault,
              amount = me.model.getPending();
          if (exactpayment.rate && exactpayment.rate !== '1') {
            amount = OB.DEC.div(me.model.getPending(), exactpayment.rate);
          }

          if (amount > 0 && exactpayment && OB.POS.modelterminal.hasPermission(exactpayment.payment.searchKey)) {
            me.pay(amount, exactpayment.payment.searchKey, exactpayment.payment._identifier, exactpayment.paymentMethod, exactpayment.rate, exactpayment.mulrate, exactpayment.isocode);
          }
        }
      }
    });
  },
  showAllButtons: function () {
    this.$.OBPOS_UI_PaymentMethods.show();
  },
  closeAllPopups: function () {
    this.$.OBPOS_UI_PaymentMethods.hide();
  },
  shown: function () {
    var me = this,
        i, max, p, keyboard = this.owner.owner;
    keyboard.showKeypad('Coins-' + OB.POS.modelterminal.get('currency').id); // shows the Coins/Notes panel for the terminal currency
    keyboard.showSidepad('sidedisabled');

    keyboard.disableCommandKey(this, {
      commands: ['%'],
      disabled: false
    });

    for (i = 0, max = OB.POS.modelterminal.get('payments').length; i < max; i++) {
      p = OB.POS.modelterminal.get('payments')[i];
      if (p.paymentMethod.id === OB.POS.modelterminal.get('terminal').terminalType.paymentMethod) {
        keyboard.defaultcommand = OB.POS.modelterminal.get('payments')[i].payment.searchKey;
        keyboard.setStatus(OB.POS.modelterminal.get('payments')[i].payment.searchKey);
        break;
      }
    }

    if (keyboard && OB.POS.modelterminal.get('paymentcash') && keyboard.status === '') {
      keyboard.defaultcommand = OB.POS.modelterminal.get('paymentcash');
      keyboard.setStatus(OB.POS.modelterminal.get('paymentcash'));
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.PaymentMethods',
  kind: 'OB.UI.Modal',
  topPosition: '125px',
  i18nHeader: 'OBPOS_MorePaymentsHeader',
  sideButtons: [],
  body: {
    classes: 'row-fluid',
    components: [{
      classes: 'span12',
      components: [{
        style: 'border-bottom: 1px solid #cccccc;',
        classes: 'row-fluid',
        components: [{
          name: 'buttonslist',
          classes: 'span12'
        }]
      }]
    }]
  },
  executeOnShow: function () {
    // build only the first time...
    if (this.$.body.$.buttonslist.children.length === 0) {
      enyo.forEach(this.sideButtons, function (sidebutton) {
        sidebutton.btn.definition.includedInPopUp = true;
        this.$.body.$.buttonslist.createComponent(sidebutton, {
          owner: this.args.toolbar
        });
      }, this);
    }
    return true;
  },
  init: function (model) {
    this.model = model;
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ButtonMore',
  style: 'display:table; width:100%;',
  events: {
    onShowAllButtons: ''
  },
  handlers: {
    onButtonStatusChanged: 'buttonStatusChanged'
  },
  components: [{
    style: 'margin: 5px;',
    components: [{
      kind: 'OB.UI.Button',
      classes: 'btnkeyboard',
      name: 'btn',
      label: ''
    }]
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.btn.setContent(OB.I18N.getLabel('OBPOS_MorePayments'));
    this.activegreen = false;
  },
  tap: function () {
    // this.toolbar.keyboard
    // this.dialogbuttons
    if (this.activegreen) {
      this.toolbar.keyboard.setStatus('');
    } else {
      this.doShowAllButtons();
    }
  },
  buttonStatusChanged: function (inSender, inEvent) {
    var status = inEvent.value.status;

    if (this.activegreen) {
      this.$.btn.setContent(OB.I18N.getLabel('OBPOS_MorePayments'));
      this.$.btn.removeClass('btnactive-green');
      this.activegreen = false;
    }

    if (this.dialogbuttons[status]) {
      this.$.btn.setContent(this.dialogbuttons[status]);
      this.$.btn.addClass('btnactive-green');
      this.activegreen = true;
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ButtonSwitch',

  style: 'display:table; width:100%;',
  components: [{
    style: 'margin: 5px;',
    components: [{
      kind: 'OB.UI.Button',
      classes: 'btnkeyboard',
      name: 'btn'
    }]
  }],
  setLabel: function (lbl) {
    this.$.btn.setContent(lbl);
  },
  tap: function () {
    this.keyboard.showNextKeypad();
  },

  create: function () {
    this.inherited(arguments);
    this.keyboard.state.on('change:keypadNextLabel', function () {
      this.setLabel(this.keyboard.state.get('keypadNextLabel'));
    }, this);
    this.setLabel(this.keyboard.state.get('keypadNextLabel'));
  }
});