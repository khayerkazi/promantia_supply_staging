/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo, Backbone, _*/

OBWH.Movement = OBWH.Movement || {};

OBWH.Movement.Model = OB.Model.WindowModel.extend({
  models: [],

  doneLineEdit: function () {
    var newLine, docLine, currentLine = this.get('currentLine'),
        document = this.get('document');
    if (currentLine.get('isNewLine')) {
      newLine = currentLine.clone();
      newLine.unset('isNewLine');
      document.add(newLine);
    } else {
      docLine = currentLine.get('lineInDocument');
      currentLine.unset('lineInDocument');
      currentLine.unset('isNewLine');
      docLine.set(currentLine);
    }
    currentLine.reset();
  },

  deleteLine: function () {
    var currentLine = this.get('currentLine');
    if (!currentLine.get('isNewLine') && currentLine.get('lineInDocument')) {
      this.get('document').remove(currentLine.get('lineInDocument'));
    }
    currentLine.reset();
  },

  processDocument: function () {
    var proc = new OB.DS.Process(OBWH.Movement.Model.processDocumentClass),
        me = this,
        document;

    document = this.get('document');

    document.forEach(function (line) {
      line.unset('windowModel');
    });

    proc.exec({
      document: JSON.parse(JSON.stringify(this.get('document').toJSON())),
      name: this.get('header').get('name')
    }, function (response, message) {
      if (response && response.exception) {
        me.get('document').forEach(function (line) {
          line.set('windowModel', me);
        });
        OB.UTIL.showError(response.exception.message);
      } else {
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBWH_CreatedGoodMovement', [response.docName]));
        me.get('document').properties.set('createdDocName', response.docName);
        me.resetDocument(false);
      }
      OB.UTIL.showLoading(false);
    }, function () {
      me.get('document').forEach(function (line) {
        line.set('windowModel', me);
      });
      OB.UTIL.showLoading(false);
    });
  },

  scan: function (code) {
    var proc = new OB.DS.Process(OBWH.Movement.Model.scanHandlerClass),
        currentLine = this.get('currentLine');
    proc.exec({
      code: code,
      line: {
        product: currentLine.get('product.id'),
        quantity: currentLine.get('quantity'),
        fromBin: currentLine.get('fromBin.id'),
        toBin: currentLine.get('toBin.id')
      },
      eventName: OBWH.Movement.Model.scanEvent
    }, enyo.bind(this, function (response, message) {
      var line = currentLine,
          fromTo,
          binProcessed = false;
      if (response.exception) {
        OB.UTIL.showError(response.exception.message);
        return;
      }

      if (response.product) {
        line.set({
          'product.id': response.product.id,
          'product.name': response.product.name,
          'product.uom.id': response.product['uom.id'],
          'product.uom.name': response.product['uom.name'],
          'product.attributeset.hasAttribute': response.product.hasAttribute
        });

        if (response.product.hasAttribute) {
          line.set('product.attributeset.instance.id', response.product.attributeset.instance.id);
          line.set('product.attributeset.instance.name', response.product.attributeset.instance.name);
        }

        if (!(_.isUndefined(response.product.quantity) && _.isNull(response.product.quantity)) && _.isNumber(response.product.quantity)) {
          line.set('quantity', response.product.quantity);
        }
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBWH_SacannedProduct', [response.product.name]));
      }

      if (response.bin) {
        if (response.bin.from) {
          if (_.isNull(line.get('fromBin.id')) || _.isUndefined(line.get('fromBin.id'))) {
            line.set('fromBin.id', response.bin.id);
            line.set('fromBin.name', response.bin.name);
          } else {
            return;
          }
          binProcessed = true;
        }
        if (response.bin.to) {
          if (_.isNull(line.get('toBin.id')) || _.isUndefined(line.get('toBin.id'))) {
            line.set('toBin.id', response.bin.id);
            line.set('toBin.name', response.bin.name);
          } else {
            return;
          }
          binProcessed = true;
        }

        if (!binProcessed) {
          if (_.isNull(line.get('fromBin.id')) || _.isUndefined(line.get('fromBin.id'))){
            fromTo = 'from';
          } else if (_.isNull(line.get('toBin.id')) || _.isUndefined(line.get('toBin.id'))) {
            fromTo = 'to';
          } else {
            return;
          }

          line.set(fromTo + 'Bin.id', response.bin.id);
          line.set(fromTo + 'Bin.name', response.bin.name);
        }
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBWH_SacannedBin', [response.bin.name]));
      }

      line.updateLineInDocument();
    }), function () {
      window.console.error('error');
    });
  },

  selectLine: function (line) {
    this.get('currentLine').reset(line);
  },

  resetDocument: function (startNew) {
    this.get('document').properties.set('editing', startNew);
    this.get('currentLine').reset();
    this.get('document').reset();
    this.get('header').set('name', startNew ? OB.I18N.formatDate(new Date()) : null);
  },

  init: function () {
    var currentLine = new OBWH.Movement.Model.Line({
      windowModel: this
    }),
        document = new OBWH.Movement.Model.Document(),
        header = new OBWH.Movement.Model.DocumentHeader();
    //TODO: move this to the model array
    OB.Data.Registry.registerModel('ProductStockView');
    OB.Data.Registry.registerModel('Locator');

    currentLine.reset();
    document.properties = new Backbone.Model();
    document.properties.set('editing', false);

    this.set('currentLine', currentLine);
    this.set('document', document);
    this.set('header', header);

    document.on('selected', function (model) {
      this.set('selectedLine', model);
    }, this);
  }
});


OBWH.Movement.Model.Line = Backbone.Model.extend({
  reset: function (model) {
    var keepWindowModel;
    if (!model) {
      keepWindowModel = this.get('windowModel');
      this.clear();
      this.set({
        isNewLine: true,
        quantity: 0,
        windowModel: keepWindowModel
      });
      this.setDefaults();
      this.unset('lineInDocument');
    } else {
      this.set(model.attributes);
      this.set({
        isNewLine: false,
        lineInDocument: model
      });
    }
  },

  setDefaults: function () {
    var defaults = this.get('windowModel').get('parameters');
    if (!defaults) {
      return;
    }

    if (defaults.product) {
      this.set({
        'product.id': defaults.product.id,
        'product.name': defaults.product.name,
        'product.attributeset.hasAttribute': defaults.product.hasAttribute,
        'product.uom.id': defaults.product.uom.id,
        'product.uom.name': defaults.product.uom.name,
        'quantity': defaults.product.quantity
      });

      if (defaults.product.attributeSetInstance) {
        this.set({
          'product.attributeset.instance.id': defaults.product.attributeSetInstance.id,
          'product.attributeset.instance.name': defaults.product.attributeSetInstance.name
        });
      }
    }

    if (defaults.fromBin) {
      this.set({
        'fromBin.id': defaults.fromBin.id,
        'fromBin.name': defaults.fromBin.name
      });
    }

    if (defaults.toBin) {
      this.set({
        'toBin.id': defaults.toBin.id,
        'toBin.name': defaults.toBin.name
      });
    }
  },

  setProduct: function (productBin) {
    this.set('product.id', productBin.get('product'));
    this.set('product.name', productBin.get('product$_identifier'));
    this.set('product.uom.id', productBin.get('uOM'));
    this.set('product.uom.name', productBin.get('uOM$_identifier'));
    this.set('product.attributeset.hasAttribute', productBin.get('attributeSetValue') && productBin.get('attributeSetValue') !== '0' ? true : false);
    this.set('product.attributeset.instance.id', productBin.get('attributeSetValue'));
    this.set('product.attributeset.instance.name', productBin.get('attributeSetValue$_identifier'));
    this.set('quantity', productBin.get('quantityOnHand'));
    if (productBin.get('storageBin')) {
      this.set('fromBin.id', productBin.get('storageBin'));
      this.set('fromBin.name', productBin.get('storageBin$_identifier'));
    }
    this.updateLineInDocument();
  },

  setProductAttribute: function (attribute) {
    this.set({
      'product.attributeset.hasAttribute': true,
      'product.attributeset.instance.id': attribute.get('attributeSetValue'),
      'product.attributeset.instance.name': attribute.get('attributeSetValue$_identifier')
    });
    this.updateLineInDocument();
  },

  setBin: function (event) {

    var binType = event.type + "Bin";
    this.set(binType + ".id", event.bin.get('id'));
    this.set(binType + ".name", event.bin.get('_identifier'));
    this.updateLineInDocument();
  },

  setQuantity: function (qty) {
    this.set('quantity', qty);
    this.updateLineInDocument();
  },

  updateLineInDocument: function () {
    var lineInDocument;
    if (this.get('isNewLine') || !this.get('lineInDocument')) {
      return;
    }

    lineInDocument = this.get('lineInDocument');

    lineInDocument.set({
      'quantity': this.get('quantity'),
      'product.id': this.get('product.id'),
      'product.name': this.get('product.name'),
      'product.uom.id': this.get('product.uom.id'),
      'product.uom.name': this.get('product.uom.name'),
      'product.attributeset.hasAttribute': this.get('product.attributeset.hasAttribute'),
      'product.attributeset.instance.id': this.get('product.attributeset.instance.id'),
      'product.attributeset.instance.name': this.get('product.attributeset.instance.name'),
      'fromBin.id': this.get('fromBin.id'),
      'fromBin.name': this.get('fromBin.name'),
      'toBin.id': this.get('toBin.id'),
      'toBin.name': this.get('toBin.name')
    });
  }
});

OBWH.Movement.Model.Document = Backbone.Collection.extend({
  model: OBWH.Movement.Model.Line
});

OBWH.Movement.Model.DocumentHeader = Backbone.Model.extend({});

OBWH.Movement.Model.processDocumentClass = 'org.openbravo.mobile.warehouse.goodmovement.ProcessDocument';
OBWH.Movement.Model.scanHandlerClass = 'org.openbravo.mobile.warehouse.barcode.ScanHandler';
OBWH.Movement.Model.scanEvent = 'OBWH_goodsMovements';