/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, OBWH*/

enyo.kind({
  name: 'OBWH.Movement.View',
  kind: 'OB.UI.WindowView',
  windowmodel: OBWH.Movement.Model,
  handlers: {
    onSetProduct: 'setProduct',
    onSetAttribute: 'setProductAttribute',
    onSetBin: 'setBin',
    onSetQuantity: 'setQuantity',
    onScan: 'scan',
    onDoneLineEdit: 'doneLineEdit',
    onProcessDocument: 'processDocument',
    onSelectedDocLine: 'selectLine',
    onDeleteLine: 'deleteLine',
    onStartDocument: 'startDocument',
    onDeleteDocument: 'deleteDocument',
    onShowProperties: 'showPropertiesDialog',
    onSetProperty: 'setProperty'
  },
  events: {
    onShowPopup: ''
  },
  components: [{
    kind: 'OB.UI.MultiColumn',
    name: 'multiColumn',
    leftToolbar: {
      kind: 'OBWH.Movement.LeftToolbar'
    },
    leftPanel: {
      kind: 'OBWH.Movement.ReceiptView',
      name: 'receiptPanel'
    },
    rightToolbar: {
      kind: 'OBWH.Movement.RightToolbar'
    },
    rightPanel: {
      kind: 'OBWH.Movement.ProductBin',
      name: 'rightPanel'
    }
  }, {
    kind: 'OBWH.Movement.ProductSearch',
    name: 'modalProductSearch'
  }, {
    kind: 'OBWH.Movement.BinSearch',
    name: 'modalBinSearch'
  }, {
    kind: 'OBWH.Movement.AttributeSearch',
    name: 'modalAttributeSearch'
  }, {
    kind: 'OBWH.Movement.PropertiesDialog',
    name: 'propertiesDialog'
  }],

  setProduct: function (inSender, inEvent) {
    this.model.get('currentLine').setProduct(inEvent.product);
  },

  setProductAttribute: function (inSender, inEvent) {
    this.model.get('currentLine').setProductAttribute(inEvent.attribute);
  },

  setBin: function (inSender, inEvent) {
    this.model.get('currentLine').setBin(inEvent);
  },

  setQuantity: function (inSender, inEvent) {
    this.model.get('currentLine').setQuantity(inEvent.quantity);
  },

  scan: function (inSender, inEvent) {
    this.model.scan(inEvent.code);
  },

  doneLineEdit: function () {
    var wasNewLine = this.model.get('currentLine').get('isNewLine');
    this.model.doneLineEdit();
    this.waterfall('onEditingLine', {
      edit: false,
      line: null
    });

    if (OB.UI.MultiColumn.isSingleColumn() && wasNewLine) {
      OB.UTIL.showSuccess(OB.I18N.getLabel('OBWH_AddedLine'));
    }
  },

  processDocument: function () {
    OB.UTIL.showLoading(true);
    this.model.processDocument();
    return true;
  },

  selectLine: function (inSender, inEvent) {
    var line = inEvent.line || this.model.get('selectedLine');
    this.model.selectLine(line);
    this.waterfall('onEditingLine', {
      edit: true,
      line: line
    });
    this.waterfall('onShowColumn', {
      colNum: 1
    });
  },

  deleteLine: function (inSender, inEvent) {
    this.model.deleteLine();
    this.waterfall('onEditingLine', {
      edit: false
    });
  },

  startDocument: function (inSender, inEvent) {
    this.model.resetDocument(true);
  },

  deleteDocument: function (inSender, inEvent) {
    this.model.resetDocument(false);
  },

  showPropertiesDialog: function (inSender, inEvent) {
    this.doShowPopup({
      popup: 'propertiesDialog'
    });
    return true;
  },

  setProperty: function (inSender, inEvent) {
    this.model.get('header').set('name', inEvent.value);
    return true;
  },

  init: function () {
    var p, document;

    this.inherited(arguments);

    if (this.params) {
      p = JSON.parse(decodeURI(this.params).replace("@@", "/"));
      this.$.multiColumn.$.leftPanel.$.receiptPanel.setWindowTitle(p.title);
      this.model.set('parameters', p);
    }

    document = this.model.get('document');
    this.$.multiColumn.$.leftPanel.$.receiptPanel.setDocument(document);
    this.$.multiColumn.$.rightPanel.$.rightPanel.setCurrentLine(this.model.get('currentLine'));

    document.on('add remove', function () {
      if (document.length > 1) {
        // already enabled
        return;
      }
      this.waterfall('onDisableButton', {
        buttons: document.length === 0 ? ['done', 'edit', 'new'] : ['new']
      });
    }, this);

    document.properties.on('change:editing', function () {
      var editing = document.properties.get('editing');

      this.waterfall('onDisableButton', {
        buttons: editing ? ['new', 'done'] : ['done', 'scan', 'edit']
      });

      this.$.multiColumn.setRightShowing(editing);
    }, this);

    this.$.modalProductSearch.setCurrentLine(this.model.get('currentLine'));
    this.$.modalBinSearch.setCurrentLine(this.model.get('currentLine'));
    this.$.modalAttributeSearch.setCurrentLine(this.model.get('currentLine'));
    this.$.multiColumn.setRightShowing(false);

    OB.MobileApp.view.scanningFocus(true);
  }
});

OB.MobileApp.windowRegistry.registerWindow({
  windowClass: 'OBWH.Movement.View',
  route: 'wh-movement',
  menuPosition: null
});