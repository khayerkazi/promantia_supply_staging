/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, $, _*/

// Toolbar container
// ----------------------------------------------------------------------------
//enyo.kind({
//  name: 'OB.OBPOSPointOfSale.UI.RightToolbar',
//  handlers: {
//    onTabButtonTap: 'tabButtonTapHandler'
//  },
//  components: [{
//    tag: 'ul',
//    classes: 'unstyled nav-pos row-fluid',
//    name: 'toolbar'
//  }],
//  tabButtonTapHandler: function (inSender, inEvent) {
//    if (inEvent.tabPanel) {
//      this.setTabButtonActive(inEvent.tabPanel);
//    }
//  },
//  setTabButtonActive: function (tabName) {
//    var buttonContainerArray = this.$.toolbar.getComponents(),
//        i;
//
//    for (i = 0; i < buttonContainerArray.length; i++) {
//      buttonContainerArray[i].removeClass('active');
//      if (buttonContainerArray[i].getComponents()[0].getComponents()[0].name === tabName) {
//        buttonContainerArray[i].addClass('active');
//      }
//    }
//  },
//  manualTap: function (tabName, options) {
//    var tab;
//
//    function getButtonByName(name, me) {
//      var componentArray = me.$.toolbar.getComponents(),
//          i;
//      for (i = 0; i < componentArray.length; i++) {
//        if (componentArray[i].$.theButton.getComponents()[0].name === name) {
//          return componentArray[i].$.theButton.getComponents()[0];
//        }
//      }
//      return null;
//    }
//
//    tab = getButtonByName(tabName, this);
//    if (tab) {
//      tab.tap(options);
//    }
//  },
//  initComponents: function () {
//    this.inherited(arguments);
//    enyo.forEach(this.buttons, function (btn) {
//      this.$.toolbar.createComponent({
//        kind: 'OB.OBPOSPointOfSale.UI.RightToolbarButton',
//        button: btn
//      });
//    }, this);
//  }
//});
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.RightToolbarImpl',
  published: {
    receipt: null
  },
  handlers: {
    onTabButtonTap: 'tabButtonTapHandler'
  },
  tabButtonTapHandler: function (inSender, inEvent) {
    if (inEvent.tabPanel) {
      this.setTabButtonActive(inEvent.tabPanel);
    }
  },
  setTabButtonActive: function (tabName) {
    var buttonContainerArray = this.$.toolbar.getComponents(),
        i;

    for (i = 0; i < buttonContainerArray.length; i++) {
      buttonContainerArray[i].removeClass('active');
      if (buttonContainerArray[i].getComponents()[0].getComponents()[0].tabToOpen === tabName) {
        buttonContainerArray[i].addClass('active');
      }
    }
  },
  manualTap: function (tabName, options) {
    var tab;

    function getButtonByName(name, me) {
      var componentArray = me.$.toolbar.getComponents(),
          i;
      for (i = 0; i < componentArray.length; i++) {
        if (componentArray[i].$.theButton.getComponents()[0].tabToOpen === name) {
          return componentArray[i].$.theButton.getComponents()[0];
        }
      }
      return null;
    }

    tab = getButtonByName(tabName, this);
    if (tab) {
      tab.tap(options);
    }
  },
  kind: 'OB.UI.MultiColumn.Toolbar',
  buttons: [{
    kind: 'OB.OBPOSPointOfSale.UI.ButtonTabScan',
    name: 'dummy',
    span: 0,
    showing: false
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.ButtonTabScan',
    name: 'toolbarBtnScan',
    tabToOpen: 'scan',
    span: 3
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.ButtonTabBrowse',
    name: 'toolbarBtnCatalog',
    tabToOpen: 'catalog',
    span: 3
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.ButtonTabSearchCharacteristic',
    name: 'toolbarBtnSearchCharacteristic',
    tabToOpen: 'searchCharacteristic',
    span: 3
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.ButtonTabEditLine',
    name: 'toolbarBtnEdit',
    tabToOpen: 'edit',
    span: 3
  }],

  receiptChanged: function () {
    var totalPrinterComponent;

    this.receipt.on('clear', function () {
      this.waterfall('onChangeTotal', {
        newTotal: this.receipt.getTotal()
      });
      if (this.receipt.get('isEditable') === false) {
        this.manualTap('edit');
      } else {
        if (OB.POS.modelterminal.get('terminal').defaultwebpostab) {
          if (OB.POS.modelterminal.get('terminal').defaultwebpostab !== '') {
            this.manualTap(OB.POS.modelterminal.get('terminal').defaultwebpostab);
          } else {
            this.manualTap('scan');
          }
        } else {
          this.manualTap('scan');
        }

      }
    }, this);

    this.receipt.on('scan', function (params) {
      this.manualTap('scan', params);
    }, this);

    this.receipt.get('lines').on('click', function () {
      this.manualTap('edit');
    }, this);

    //some button will draw the total
    if (this.receipt.get('orderType') !== 3) { //Do not change voiding layaway
      this.bubble('onChangeTotal', {
        newTotal: this.receipt.getTotal()
      });
    }
    this.receipt.on('change:gross', function (model) {
      if (this.receipt.get('orderType') !== 3) { //Do not change voiding layaway
        this.bubble('onChangeTotal', {
          newTotal: this.receipt.getTotal()
        });
      }
    }, this);
  }
});


enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.RightToolbarButton',
  tag: 'li',
  components: [{
    name: 'theButton',
    attributes: {
      style: 'margin: 0px 5px 0px 5px;'
    }
  }],
  initComponents: function () {
    this.inherited(arguments);
    if (this.button.containerCssClass) {
      this.setClassAttribute(this.button.containerCssClass);
    }
    this.$.theButton.createComponent(this.button);
  }
});


// Toolbar buttons
// ----------------------------------------------------------------------------
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ButtonTabScan',
  kind: 'OB.UI.ToolbarButtonTab',
  tabPanel: 'scan',
  i18nLabel: 'OBMOBC_LblScan',
  events: {
    onTabChange: '',
    onRightToolbarDisabled: ''
  },
  handlers: {
    onRightToolbarDisabled: 'disabledButton'
  },
  init: function (model) {
    this.model = model;
    //    var me = this;
    //    this.model.get('multiOrders').on('change:isMultiOrders', function (model) {
    //      if (!model.get('isMultiOrders')) {
    //        this.doTabChange({
    //          tabPanel: this.tabPanel,
    //          keyboard: 'toolbarscan',
    //          edit: false,
    //          status: ''
    //        });
    //      }
    //      me.doRightToolbarDisabled({
    //        status: model.get('isMultiOrders')
    //      });
    //    }, this);
  },
  disabledButton: function (inSender, inEvent) {
    this.isEnabled = !inEvent.status;
    this.setDisabled(inEvent.status);
  },
  tap: function (options) {
    if (!this.disabled) {
      this.doTabChange({
        tabPanel: this.tabPanel,
        keyboard: 'toolbarscan',
        edit: false,
        options: options,
        status: ''
      });
    }
    OB.MobileApp.view.scanningFocus(true);

    return true;
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ButtonTabBrowse',
  kind: 'OB.UI.ToolbarButtonTab',
  events: {
    onTabChange: '',
    onRightToolbarDisabled: ''
  },
  handlers: {
    onRightToolbarDisabled: 'disabledButton'
  },
  init: function (model) {
    this.model = model;
    //    var me = this;
    //    this.model.get('multiOrders').on('change:isMultiOrders', function (model) {
    //      me.doRightToolbarDisabled({
    //        status: model.get('isMultiOrders')
    //      });
    //    }, this);
  },
  disabledButton: function (inSender, inEvent) {
    this.isEnabled = !inEvent.status;
    this.setDisabled(inEvent.status);
  },
  tabPanel: 'catalog',
  i18nLabel: 'OBMOBC_LblBrowse',
  tap: function () {
    OB.MobileApp.view.scanningFocus(false);
    if (!this.disabled) {
      this.doTabChange({
        tabPanel: this.tabPanel,
        keyboard: false,
        edit: false
      });
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ButtonTabSearchCharacteristic',
  kind: 'OB.UI.ToolbarButtonTab',
  tabPanel: 'searchCharacteristic',
  i18nLabel: 'OBPOS_LblSearch',
  events: {
    onTabChange: '',
    onRightToolbarDisabled: ''
  },
  handlers: {
    onRightToolbarDisabled: 'disabledButton'
  },
  init: function (model) {
    this.model = model;
    //    var me = this;
    //    this.model.get('multiOrders').on('change:isMultiOrders', function (model) {
    //      me.doRightToolbarDisabled({
    //        status: model.get('isMultiOrders')
    //      });
    //    }, this);
  },
  disabledButton: function (inSender, inEvent) {
    this.isEnabled = !inEvent.status;
    this.setDisabled(inEvent.status);
  },
  tap: function () {
    OB.MobileApp.view.scanningFocus(false);
    if (this.disabled === false) {
      this.doTabChange({
        tabPanel: this.tabPanel,
        keyboard: false,
        edit: false
      });
    }
  },
  initComponents: function () {
    this.inherited(arguments);
  }
});


enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.ButtonTabEditLine',
  published: {
    ticketLines: null
  },
  kind: 'OB.UI.ToolbarButtonTab',
  tabPanel: 'edit',
  i18nLabel: 'OBPOS_LblEdit',
  events: {
    onTabChange: '',
    onRightToolbarDisabled: ''
  },
  handlers: {
    onRightToolbarDisabled: 'disabledButton'
  },
  init: function (model) {
    this.model = model;
    //    var me = this;
    //    this.model.get('multiOrders').on('change:isMultiOrders', function (model) {
    //      me.doRightToolbarDisabled({
    //        status: model.get('isMultiOrders')
    //      });
    //    }, this);
  },
  disabledButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.status);
  },
  tap: function () {
    OB.MobileApp.view.scanningFocus(false);
    if (!this.disabled) {
      this.doTabChange({
        tabPanel: this.tabPanel,
        keyboard: 'toolbarscan',
        edit: true
      });
    }
  }
});


// Toolbar panes
//----------------------------------------------------------------------------
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.RightToolbarPane',
  published: {
    model: null
  },
  classes: 'postab-content',

  handlers: {
    onTabButtonTap: 'tabButtonTapHandler'
  },
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.TabScan',
    name: 'scan'
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.TabBrowse',
    name: 'catalog'
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.TabSearchCharacteristic',
    name: 'searchCharacteristic',
    style: 'margin: 5px'
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.TabPayment',
    name: 'payment'
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.TabEditLine',
    name: 'edit'
  }],
  tabButtonTapHandler: function (inSender, inEvent) {
    if (inEvent.tabPanel) {
      this.showPane(inEvent.tabPanel, inEvent.options);
    }
  },
  showPane: function (tabName, options) {
    var paneArray = this.getComponents(),
        i;
    for (i = 0; i < paneArray.length; i++) {
      paneArray[i].removeClass('active');
      if (paneArray[i].name === tabName) {
        OB.MobileApp.model.set('lastPaneShown', tabName);
        if (paneArray[i].executeOnShow) {
          paneArray[i].executeOnShow(options);
        }
        paneArray[i].addClass('active');
      }
    }
  },
  modelChanged: function () {
    var receipt = this.model.get('order');
    this.$.scan.setReceipt(receipt);
    this.$.searchCharacteristic.setReceipt(receipt);
    this.$.payment.setReceipt(receipt);
    this.$.edit.setReceipt(receipt);
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.TabSearchCharacteristic',
  kind: 'OB.UI.TabPane',
  published: {
    receipt: null
  },
  components: [{
    kind: 'OB.UI.SearchProductCharacteristic',
    name: 'searchCharacteristicTabContent'
  }],
  receiptChanged: function () {
    this.$.searchCharacteristicTabContent.setReceipt(this.receipt);
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.TabBrowse',
  kind: 'OB.UI.TabPane',
  components: [{
    kind: 'OB.UI.ProductBrowser',
    name: 'catalogTabContent'
  }]
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.TabScan',
  kind: 'OB.UI.TabPane',
  published: {
    receipt: null
  },
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.Scan',
    name: 'scanTabContent'
  }],
  receiptChanged: function () {
    this.$.scanTabContent.setReceipt(this.receipt);
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.TabEditLine',
  kind: 'OB.UI.TabPane',
  published: {
    receipt: null
  },
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.EditLine',
    name: 'editTabContent'
  }],
  receiptChanged: function () {
    this.$.editTabContent.setReceipt(this.receipt);
  }
});

enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.TabPayment',
  kind: 'OB.UI.TabPane',
  published: {
    receipt: null
  },
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.Payment',
    name: 'paymentTabContent'
  }],
  receiptChanged: function () {
    this.$.paymentTabContent.setReceipt(this.receipt);
  },
  executeOnShow: function (options) {
    var me = this;
  }
});