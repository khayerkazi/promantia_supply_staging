/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo*/

enyo.kind({
  name: 'OBWH.Movement.LeftToolbar',
  kind: 'OB.UI.MultiColumn.Toolbar',
  handlers: {
    onDisableButton: 'disableButton'
  },

  showMenu: true,
  menuEntries: [{
    kind: 'OB.UI.MenuAction',
    i18nLabel: 'OBWH_DocProperties',
    eventName: 'onShowProperties',
    name: 'properties'
  }],
  buttons: [{
    kind: 'OBWH.Movement.ButtonNew',
    span: 3
  }, {
    kind: 'OBWH.Movement.ButtonDelete',
    span: 3
  }, {
    kind: 'OBWH.Movement.ButtonDone',
    name: 'btnDone',
    span: 6
  }],

  disableButton: function (inSender, inEvent) {
    this.waterfall('onDisableMenuEntry', {
      entryName: 'properties',
      disable: inEvent.buttons.indexOf('new') === -1
    });
  },

  initComponents: function () {
    this.inherited(arguments);
    this.waterfall('onDisableMenuEntry', {
      entryName: 'properties',
      disable: true
    });
  }
});

enyo.kind({
  name: 'OBWH.Movement.ButtonNew',
  kind: 'OB.UI.ToolbarButton',
  icon: 'btn-icon btn-icon-new',
  handlers: {
    onDisableButton: 'disableButton'
  },
  tap: function () {
    this.setDisabled(true);
    this.bubble('onStartDocument');
  },
  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('new') !== -1);
    //return true;
  }
});

enyo.kind({
  name: 'OBWH.Movement.ButtonDelete',
  kind: 'OB.UI.ToolbarButton',
  icon: 'btn-icon btn-icon-delete',
  disabled: true,
  handlers: {
    onDisableButton: 'disableButton'
  },
  tap: function () {
    var me = this;
    OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBWH_DeleteDocumentTitle'), OB.I18N.getLabel('OBWH_DeleteDocumentText'), [{
      label: OB.I18N.getLabel('OBWH_DeleteLbl'),
      action: function () {
        me.setDisabled(true);
        me.bubble('onDeleteDocument');
      }
    }, {
      label: OB.I18N.getLabel('OBMOBC_LblCancel')
    }]);
  },
  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('new') === -1);
  }
});

enyo.kind({
  name: 'OBWH.Movement.ButtonDone',
  kind: 'OB.UI.ToolbarButton',
  events: {
    onProcessDocument: ''
  },
  handlers: {
    onDisableButton: 'disableButton'
  },

  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('done') !== -1);
    return true;
  },

  tap: function () {
    this.doProcessDocument();
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBWH_BtnDone'));
    this.setDisabled(true);
  }
});

enyo.kind({
  name: 'OBWH.Movement.ReceiptView',
  classes: 'span12',
  published: {
    windowTitle: null,
    document: null
  },
  events: {
    onSelectedDocLine: ''
  },
  components: [{
    style: 'overflow:auto; margin: 5px',
    components: [{
      style: 'position: relative;background-color: #ffffff; color: black;',
      components: [{
        style: 'padding: 5px;',
        components: [{
          classes: 'row-fluid',
          components: [{
            classes: 'span12',
            components: [{
              style: 'padding: 5px 0px 10px 0px; border-bottom: 1px solid #cccccc;',
              components: [{
                style: 'float:left; padding: 15px 15px 5px 10px; font-weight: bold; color: #6CB33F;',
                name: 'title'
              }, {
                style: 'clear:both;'
              }]
            }, {
              name: 'document',
              showing: false,
              components: [{
                kind: 'OBWH.Movement.DocumentHeader'
              }, {
                kind: 'OBWH.Movement.ReceiptDetails',
                name: 'details'
              }, {
                name: 'totalLines'
              }]
            }, {
              name: 'startDocument',
              kind: 'OBWH.Movement.StartDocument'
            }]
          }]
        }]
      }]
    }]
  }],

  documentChanged: function () {
    this.$.details.setDocument(this.document);

    this.document.on('add remove reset', function () {
      if (this.document.length) {
        this.$.totalLines.show();
        this.$.totalLines.setContent(OB.I18N.getLabel('OBWH_NumLines', [this.document.length]));
      } else {
        this.$.totalLines.hide();
      }
    }, this);

    this.document.on('click', function (line) {
      this.doSelectedDocLine({
        line: line
      });
    }, this);

    this.document.properties.on('change:editing', function () {
      var editing = this.document.properties.get('editing');
      this.$.document.setShowing(editing);
      this.$.startDocument.setShowing(!editing);

      if (!editing) {
        this.$.startDocument.setCreatedDocName(this.document.properties.get('createdDocName'));
      }

    }, this);
  },

  windowTitleChanged: function () {
    this.$.title.setContent(this.windowTitle);
  }
});

enyo.kind({
  name: 'OBWH.Movement.ReceiptDetails',
  kind: 'OB.UI.ScrollableTable',
  scrollAreaMaxHeight: '437px',
  published: {
    document: null
  },
  renderLine: 'OBWH.Movement.ReceiptLine',
  renderEmpty: 'OBWH.Movement.NewDoc',
  listStyle: 'edit',

  documentChanged: function () {
    this.setCollection(this.document);
  }
});

enyo.kind({
  name: 'OBWH.Movement.NewDoc',
  kind: 'OB.UI.RenderEmpty',
  initComponents: function () {
    this.setContent(OB.I18N.getLabel('OBWH_NewDocument'));
  }
});

enyo.kind({
  name: 'OBWH.Movement.ReceiptLine',
  kind: 'OB.UI.SelectButton',
  classes: 'btnselect-orderline',
  handlers: {
    onEditingLine: 'edit'
  },
  components: [{
    style: 'float: left; width: 30%;',
    components: [{
      name: 'product'
    }, {
      name: 'attribute'
    }]
  }, {
    name: 'quantity',
    style: 'float: left; width: 10%; text-align: right;'
  }, {
    name: 'uom',
    style: 'float: left; width: 10%;'
  }, {
    name: 'fromBin',
    style: 'float: left; width: 25%;'
  }, {
    name: 'toBin',
    style: 'float: left; width: 25%;'
  }],

  edit: function (inSender, inEvent) {
    this.addRemoveClass('btnselect-orderline-edit', inEvent.edit && this.model === inEvent.line);
    return true;
  },

  initComponents: function () {
    this.inherited(arguments);
    this.$.product.setContent(this.model.get('product.name'));
    this.$.attribute.setContent(this.model.get('product.attributeset.instance.name'));
    this.$.quantity.setContent(this.model.get('quantity'));
    this.$.uom.setContent(this.model.get('product.uom.name'));
    this.$.fromBin.setContent(this.model.get('fromBin.name'));
    this.$.toBin.setContent(this.model.get('toBin.name'));
  }
});

enyo.kind({
  name: 'OBWH.Movement.StartDocument',
  classes: 'WHInfoText',
  published: {
    createdDocName: null
  },
  components: [{
    name: 'info'
  }, {
    kind: 'OB.UI.SmallButton',
    name: 'backBtn',
    classes: 'btnlink-gray btnbig',
    tap: function () {
      OB.MobileApp.model.navigate('wh');
    }
  }, {
    name: 'createdDoc',
    classes: 'WHDocCreatedText'
  }],

  createdDocNameChanged: function () {
    this.$.createdDoc.setShowing(this.createdDocName);
    this.$.createdDoc.setContent(OB.I18N.getLabel('OBWH_CreatedGoodMovement', [this.createdDocName]));
  },

  initComponents: function () {
    this.inherited(arguments);
    this.$.info.setContent(OB.I18N.getLabel('OBWH_NoDocInfo'));
    this.$.backBtn.setContent(OB.I18N.getLabel('OBWH_Back'));
  }
});
enyo.kind({
  name: 'OBWH.Movement.DocumentHeader',
  content: 'date',
  classes: 'WHDocHeader',
  //style: 'padding: 5px 0px 10px 0px; border-bottom: 1px solid #cccccc; height: 25px;',
  components: [{
    name: 'nameLbl',
    style: 'float: left'
  }, {
    name: 'dateLbl',
    style: 'float:right'
  }],
  init: function (model) {
    var header = model.get('header');
    header.on('change:name', function () {
      this.setDocName(header.get('name'));
    }, this);
    this.setDocName(header.get('name'));
  },
  setDocName: function (name) {
    this.$.nameLbl.setContent(OB.I18N.getLabel('OBWH_Name', [name]));
  },
  initComponents: function () {
    this.inherited(arguments);

    this.$.dateLbl.setContent(OB.I18N.getLabel('OBWH_MovementDate', [OB.I18N.formatDate(new Date())]));
  }
});

enyo.kind({
  name: 'OBWH.Movement.PropertiesDialog',
  kind: 'OB.UI.ModalReceiptProperties',
  newAttributes: [{
    kind: 'OB.UI.renderTextProperty',
    name: 'receiptDescription',
    modelProperty: 'name',
    i18nLabel: 'OBWH_DocName'
  }],
  init: function (model) {
    this.setHeader(OB.I18N.getLabel('OBWH_DocProperties'));

    this.model = model.get('header');
    this.model.bind('change', function () {
      var diff = this.model.changedAttributes(),
          att;
      for (att in diff) {
        if (diff.hasOwnProperty(att)) {
          this.loadValue(att);
        }
      }
    }, this);
  }
});