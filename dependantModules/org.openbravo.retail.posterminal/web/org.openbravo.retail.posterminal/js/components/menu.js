/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, _ */

enyo.kind({
  name: 'OB.UI.MenuReturn',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.return',
  events: {
    onShowDivText: ''
  },
  i18nLabel: 'OBPOS_LblReturn',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    this.doShowDivText({
      permission: this.permission,
      orderType: 1
    });
    if (OB.MobileApp.model.get('lastPaneShown') === 'payment') {
      this.model.get('order').trigger('scan');
    }
  },
  displayLogic: function () {
    var negativeLines = _.filter(this.model.get('order').get('lines').models, function (line) {
      return line.get('qty') < 0;
    }).length;
    if (!this.model.get('order').get('isQuotation')) {
      this.show();
    } else {
      this.hide();
      return;
    }
    if (negativeLines > 0) {
      this.hide();
      return;
    }
    if (this.model.get('order').get('isEditable') === false) {
      this.setShowing(false);
      return;
    }

    this.adjustVisibilityBasedOnPermissions();
  },
  init: function (model) {
    this.model = model;
    var receipt = model.get('order'),
        me = this;
    receipt.on('change:isEditable change:isQuotation change:gross', function (changedModel) {
      this.displayLogic();
    }, this);
    this.model.get('leftColumnViewManager').on('change:currentView', function (changedModel) {
      if (changedModel.isOrder()) {
        this.displayLogic();
        return;
      }
      if (changedModel.isMultiOrder()) {
        this.setShowing(false);
        return;
      }
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuVoidLayaway',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.voidLayaway',
  events: {
    onShowDivText: '',
    onTabChange: ''
  },
  i18nLabel: 'OBPOS_VoidLayaway',
  tap: function () {
    var voidAllowed = true,
        notValid = {};
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    // check if this order has been voided previously
    if (this.model.get('order').get('orderType') === 3) {
      return;
    }
    enyo.forEach(this.model.get('order').get('payments').models, function (curPayment) {
      if (_.isUndefined(curPayment.get('isPrePayment')) || _.isNull(curPayment.get('isPrePayment'))) {
        voidAllowed = false;
        notValid = curPayment;
        return;
      }
    }, this);

    if (voidAllowed) {
      this.doShowDivText({
        permission: this.permission,
        orderType: 3
      });
      this.doTabChange({
        tabPanel: 'payment',
        keyboard: 'toolbarpayment',
        edit: false
      });
    } else {
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBPOS_lblPaymentNotProcessedHeader'), OB.I18N.getLabel('OBPOS_lblPaymentNotProcessedMessage', [notValid.get('name'), notValid.get('origAmount'), OB.MobileApp.model.paymentnames[notValid.get('kind')].isocode]));
    }
  },
  displayLogic: function () {
    if (this.model.get('order').get('isLayaway')) {
      this.show();
      this.adjustVisibilityBasedOnPermissions();
    } else {
      this.hide();
    }
  },
  init: function (model) {
    this.model = model;
    var receipt = model.get('order'),
        me = this;
    this.setShowing(false);
    receipt.on('change:isLayaway', function (model) {
      this.displayLogic();
    }, this);

    this.model.get('leftColumnViewManager').on('change:currentView', function (changedModel) {
      if (changedModel.isOrder()) {
        this.displayLogic();
        return;
      }
      if (changedModel.isMultiOrder()) {
        this.setShowing(false);
        return;
      }
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuLayaway',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.layawayReceipt',
  events: {
    onShowDivText: ''
  },
  i18nLabel: 'OBPOS_LblLayawayReceipt',
  tap: function () {
    var negativeLines;
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    negativeLines = _.find(this.model.get('order').get('lines').models, function (line) {
      return line.get('qty') < 0;
    });
    if (negativeLines) {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBPOS_layawaysOrdersWithReturnsNotAllowed'));
      return true;
    }
    this.doShowDivText({
      permission: this.permission,
      orderType: 2
    });
  },
  updateVisibility: function (isVisible) {
    if (!OB.POS.modelterminal.hasPermission(this.permission)) {
      this.hide();
      return;
    }
    if (!isVisible) {
      this.hide();
      return;
    }
    this.show();
  },
  init: function (model) {
    this.model = model;
    var receipt = model.get('order'),
        me = this;
    receipt.on('change:isQuotation', function (model) {
      if (!model.get('isQuotation')) {
        me.updateVisibility(true);
      } else {
        me.updateVisibility(false);
      }
    }, this);
    receipt.on('change:isEditable', function (newValue) {
      if (newValue) {
        if (newValue.get('isEditable') === false) {
          me.updateVisibility(false);
          return;
        }
      }
      me.updateVisibility(true);
    }, this);

    this.model.get('leftColumnViewManager').on('change:currentView', function (changedModel) {
      if (changedModel.isOrder()) {
        if (model.get('order').get('isEditable') && !this.model.get('order').get('isQuotation')) {
          me.updateVisibility(true);
        } else {
          me.updateVisibility(false);
        }
        return;
      }
      if (changedModel.isMultiOrder()) {
        me.updateVisibility(false);
      }
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuProperties',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.properties',
  events: {
    onShowReceiptProperties: ''
  },
  i18nLabel: 'OBPOS_LblProperties',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    this.doShowReceiptProperties();
  },
  init: function (model) {
    this.model = model;
    this.model.get('leftColumnViewManager').on('change:currentView', function (changedModel) {
      if (changedModel.isOrder()) {
        if (model.get('order').get('isEditable')) {
          this.setDisabled(false);
          this.adjustVisibilityBasedOnPermissions();
        } else {
          this.setDisabled(true);
        }
        return;
      }
      if (changedModel.isMultiOrder()) {
        this.setDisabled(true);
      }
    }, this);
    this.model.get('order').on('change:isEditable', function (newValue) {
      if (newValue) {
        if (newValue.get('isEditable') === false) {
          this.setShowing(false);
          return;
        }
      }
      this.setShowing(true);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuInvoice',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.invoice',
  events: {
    onReceiptToInvoice: '',
    onCancelReceiptToInvoice: ''
  },
  i18nLabel: 'OBPOS_LblInvoice',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    this.taxIdValidation(this.model.get('order'));
  },
  taxIdValidation: function (model) {
    if (!OB.POS.modelterminal.hasPermission('OBPOS_receipt.invoice')) {
      this.doCancelReceiptToInvoice();
    } else if (OB.POS.modelterminal.hasPermission('OBPOS_retail.restricttaxidinvoice', true) && !model.get('bp').get('taxID')) {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBPOS_BP_No_Taxid'));
      this.doCancelReceiptToInvoice();
    } else {
      this.doReceiptToInvoice();
    }

  },
  init: function (model) {
    this.model = model;
    var receipt = model.get('order'),
        me = this;
    receipt.on('change:isQuotation change:isLayaway', function (model) {
      if (!model.get('isQuotation') || model.get('isLayaway')) {
        me.show();
      } else {
        me.hide();
      }
    }, this);
    receipt.on('change:bp', function (model) {
      // if the receip is cloning, then the called to taxIdValidation is not done because this function does a save
      if (model.get('generateInvoice') && !model.get('cloningReceipt')) {
        me.taxIdValidation(model);
      }
    }, this);
    receipt.on('change:isEditable', function (newValue) {
      if (newValue) {
        if (newValue.get('isEditable') === false && !newValue.get('isLayaway')) {
          this.setShowing(false);
          return;
        }
      }
      this.setShowing(true);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuOpenDrawer',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.opendrawerfrommenu',
  i18nLabel: 'OBPOS_LblOpenDrawer',
  init: function (model) {
    this.model = model;
  },
  tap: function () {
    var me = this;
    if (this.disabled) {
      return true;
    }
    OB.UTIL.Approval.requestApproval(
    me.model, 'OBPOS_approval.opendrawer.menu', function (approved, supervisor, approvalType) {
      if (approved) {
        OB.POS.hwserver.openDrawer({
          openFirst: true
        }, OB.MobileApp.model.get('permissions').OBPOS_timeAllowedDrawerSales);
      }
    });
    this.inherited(arguments);
  }
});

enyo.kind({
  name: 'OB.UI.MenuCustomers',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.customers',
  events: {
    onChangeSubWindow: ''
  },
  i18nLabel: 'OBPOS_LblCustomers',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    this.doChangeSubWindow({
      newWindow: {
        name: 'customerAdvancedSearch',
        params: {
          navigateOnClose: 'mainSubWindow'
        }
      }
    });
  },
  init: function (model) {
    this.model = model;
    model.get('leftColumnViewManager').on('order', function () {
      this.setDisabled(false);
      this.adjustVisibilityBasedOnPermissions();
    }, this);

    model.get('leftColumnViewManager').on('multiorder', function () {
      this.setDisabled(true);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuPrint',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_print.receipt',
  events: {
    onPrintReceipt: ''
  },
  i18nLabel: 'OBPOS_LblPrintReceipt',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doPrintReceipt();
    }
  },
  init: function (model) {
    var receipt = model.get('order'),
        me = this;
    receipt.on('change:isQuotation', function (model) {
      if (!model.get('isQuotation')) {
        me.show();
      } else {
        me.hide();
      }
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuQuotation',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.quotation',
  events: {
    onCreateQuotation: ''
  },
  i18nLabel: 'OBPOS_CreateQuotation',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (OB.POS.modelterminal.get('terminal').terminalType.documentTypeForQuotations) {
      if (OB.POS.modelterminal.hasPermission(this.permission)) {
        if (this.model.get('leftColumnViewManager').isMultiOrder()) {
          if (this.model.get('multiorders')) {
            this.model.get('multiorders').resetValues();
          }
          this.model.get('leftColumnViewManager').setOrderMode();
        }
        this.doCreateQuotation();
      }
    } else {
      OB.UTIL.showError(OB.I18N.getLabel('OBPOS_QuotationNoDocType'));
    }
  },
  updateVisibility: function (model) {
    if (!model.get('isQuotation')) {
      this.show();
      this.adjustVisibilityBasedOnPermissions();
    } else {
      this.hide();
    }
  },
  init: function (model) {
    var receipt = model.get('order'),
        me = this;
    this.model = model;
    receipt.on('change:isQuotation', function (model) {
      this.updateVisibility(model);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuDiscounts',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.advDiscounts',
  events: {
    onDiscountsMode: ''
  },
  //TODO
  i18nLabel: 'OBPOS_LblReceiptDiscounts',
  tap: function () {
    if (!this.disabled) {
      this.inherited(arguments); // Manual dropdown menu closure
      this.doDiscountsMode({
        tabPanel: 'edit',
        keyboard: 'toolbardiscounts',
        edit: false,
        options: {
          discounts: true
        }
      });
    }
  },
  updateVisibility: function () {
    var me = this;
    if (this.model.get('leftColumnViewManager').isMultiOrder()) {
      me.setDisabled(true);
      return;
    }

    me.setDisabled(OB.UTIL.isDisableDiscount(this.receipt));

    me.adjustVisibilityBasedOnPermissions();
  },
  init: function (model) {
    var me = this;
    this.model = model;
    this.receipt = model.get('order');
    //set disabled until ticket has lines
    me.setDisabled(true);
    if (!OB.POS.modelterminal.hasPermission(this.permission)) {
      //no permissions, never will be enabled
      return;
    }

    model.get('leftColumnViewManager').on('order', function () {
      this.updateVisibility();
    }, this);

    model.get('leftColumnViewManager').on('multiorder', function () {
      me.setDisabled(true);
    }, this);

    this.receipt.get('lines').on('all', function () {
      this.updateVisibility();
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuCreateOrderFromQuotation',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.createorderfromquotation',
  events: {
    onShowPopup: ''
  },
  i18nLabel: 'OBPOS_CreateOrderFromQuotation',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.inherited(arguments); // Manual dropdown menu closure
      this.doShowPopup({
        popup: 'modalCreateOrderFromQuotation'
      });
    }
  },
  updateVisibility: function (model) {
    if (OB.POS.modelterminal.hasPermission(this.permission) && model.get('isQuotation') && model.get('hasbeenpaid') === 'Y') {
      this.show();
    } else {
      this.hide();
    }
  },
  init: function (model) {
    var receipt = model.get('order'),
        me = this;
    me.hide();

    model.get('leftColumnViewManager').on('order', function () {
      this.updateVisibility(receipt);
      this.adjustVisibilityBasedOnPermissions();
    }, this);

    model.get('leftColumnViewManager').on('multiorder', function () {
      me.hide();
    }, this);

    receipt.on('change:isQuotation', function (model) {
      this.updateVisibility(model);
    }, this);
    receipt.on('change:hasbeenpaid', function (model) {
      this.updateVisibility(model);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuRejectQuotation',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.rejectquotation',
  events: {
    onRejectQuotation: ''
  },
  i18nLabel: 'OBPOS_RejectQuotation',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doRejectQuotation();
    }
  },
  updateVisibility: function (model) {
    if (model.get('isQuotation') && model.get('hasbeenpaid') === 'Y') {
      this.hide();
    } else {
      this.hide();
    }
  },
  init: function (model) {
    var receipt = model.get('order'),
        me = this;
    me.hide();
    receipt.on('change:isQuotation', function (model) {
      this.updateVisibility(model);
    }, this);
    receipt.on('change:hasbeenpaid', function (model) {
      this.updateVisibility(model);
    }, this);
  }
});
enyo.kind({
  name: 'OB.UI.MenuReactivateQuotation',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_receipt.reactivatequotation',
  events: {
    onShowReactivateQuotation: ''
  },
  i18nLabel: 'OBPOS_ReactivateQuotation',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doShowReactivateQuotation();
    }
  },
  updateVisibility: function (model) {
    if (OB.POS.modelterminal.hasPermission(this.permission) && model.get('isQuotation') && model.get('hasbeenpaid') === 'Y') {
      this.show();
    } else {
      this.hide();
    }
  },
  init: function (model) {
    var receipt = model.get('order'),
        me = this;
    me.hide();

    model.get('leftColumnViewManager').on('order', function () {
      this.updateVisibility(receipt);
      this.adjustVisibilityBasedOnPermissions();
    }, this);

    model.get('leftColumnViewManager').on('multiorder', function () {
      me.hide();
    }, this);

    receipt.on('change:isQuotation', function (model) {
      this.updateVisibility(model);
    }, this);
    receipt.on('change:hasbeenpaid', function (model) {
      this.updateVisibility(model);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.MenuPaidReceipts',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.paidReceipts',
  events: {
    onPaidReceipts: ''
  },
  i18nLabel: 'OBPOS_LblPaidReceipts',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (!OB.POS.modelterminal.get('connectedToERP')) {
      OB.UTIL.showError(OB.I18N.getLabel('OBPOS_OfflineWindowRequiresOnline'));
      return;
    }
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doPaidReceipts({
        isQuotation: false
      });
    }
  }
});

enyo.kind({
  name: 'OB.UI.MenuQuotations',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.quotations',
  events: {
    onQuotations: ''
  },
  i18nLabel: 'OBPOS_Quotations',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (!OB.POS.modelterminal.get('connectedToERP')) {
      OB.UTIL.showError(OB.I18N.getLabel('OBPOS_OfflineWindowRequiresOnline'));
      return;
    }
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doQuotations();
    }
  }
});

enyo.kind({
  name: 'OB.UI.MenuLayaways',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.layaways',
  events: {
    onLayaways: ''
  },
  i18nLabel: 'OBPOS_LblLayaways',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (!OB.POS.modelterminal.get('connectedToERP')) {
      OB.UTIL.showError(OB.I18N.getLabel('OBPOS_OfflineWindowRequiresOnline'));
      return;
    }
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doLayaways();
    }
  }
});

enyo.kind({
  name: 'OB.UI.MenuMultiOrders',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.multiorders',
  events: {
    onMultiOrders: ''
  },
  i18nLabel: 'OBPOS_LblPayOpenTickets',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (!OB.POS.modelterminal.get('connectedToERP')) {
      OB.UTIL.showError(OB.I18N.getLabel('OBPOS_OfflineWindowRequiresOnline'));
      return;
    }
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doMultiOrders();
    }
  }
});

enyo.kind({
  name: 'OB.UI.MenuBackOffice',
  kind: 'OB.UI.MenuAction',
  permission: 'OBPOS_retail.backoffice',
  url: '../..',
  events: {
    onBackOffice: ''
  },
  i18nLabel: 'OBPOS_LblOpenbravoWorkspace',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.inherited(arguments); // Manual dropdown menu closure
    if (OB.POS.modelterminal.hasPermission(this.permission)) {
      this.doBackOffice({
        url: this.url
      });
    }
  }
});