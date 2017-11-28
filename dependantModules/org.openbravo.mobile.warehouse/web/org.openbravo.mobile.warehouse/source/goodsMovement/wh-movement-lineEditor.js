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
  name: 'OBWH.Movement.RightToolbar',
  kind: 'OB.UI.MultiColumn.Toolbar',
  handlers: {
    onActivateTab: 'activateTab',
    onEditingLine: 'edit'
  },
  buttons: [{
    kind: 'OBWH.Movement.ButtonScan',
    name: 'scan',
    span: '6'
  }, {
    kind: 'OBWH.Movement.ButtonEdit',
    name: 'edit',
    span: '6'
  }],

  activateTab: function (inSender, inEvent) {
    this.waterfall('onSetTabActive', inEvent);
  },

  edit: function (inSender, inEvent) {
    this.activateTab(inSender, {
      tab: inEvent.edit ? 'edit' : 'scan'
    });
  },

  initComponents: function () {
    this.inherited(arguments);
    this.waterfall('onSetTabActive', {
      tab: 'scan'
    });
  }
});

enyo.kind({
  name: 'OBWH.Movement.ToolbarButton',
  kind: 'OB.UI.ToolbarButtonTab',
  events: {
    onActivateTab: ''
  },
  handlers: {
    onSetTabActive: 'setTabActive'
  },
  tap: function () {
    this.doActivateTab({
      tab: this.name
    });
  },
  setTabActive: function (inSender, inEvent) {
    this.parent.parent.addRemoveClass('active', this.name === inEvent.tab);
  }
});

enyo.kind({
  name: 'OBWH.Movement.ButtonScan',
  kind: 'OBWH.Movement.ToolbarButton',
  events: {
    onDoneLineEdit: ''
  },
  handlers: {
    onDisableButton: 'disableButton'
  },

  tap: function () {
    this.inherited(arguments);
    this.doDoneLineEdit();
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBWH_BtnScan'));
  },
  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('scan') !== -1);
    return true;
  }
});

enyo.kind({
  name: 'OBWH.Movement.ButtonEdit',
  kind: 'OBWH.Movement.ToolbarButton',
  events: {
    onSelectedDocLine: ''
  },

  handlers: {
    onDisableButton: 'disableButton'
  },

  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('edit') !== -1);
    return true;
  },

  tap: function () {
    this.inherited(arguments);
    this.doSelectedDocLine();
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setDisabled(true);
    this.setContent(OB.I18N.getLabel('OBWH_BtnEdit'));
  }
});

enyo.kind({
  name: 'OBWH.Movement.ProductBin',
  published: {
    currentLine: null
  },

  handlers: {
    onEditingLine: 'edit'
  },

  edit: function (inSender, inEvent) {
    // switching styles between edit and scan modes
    if (inEvent.edit) {
      // EDIT
      this.$.editor.applyStyle('background-color', 'white');

      this.$.productLbl.applyStyle('color', 'black');
      this.$.product.removeClass('btnlink-white');
      this.$.product.removeClass('btnlink-fontblue');
      this.$.product.addClass('btnlink-orange');

      this.$.qty.applyStyle('color', 'black');

      this.$.attributesLbl.applyStyle('color', 'black');
      this.$.attributeContent.removeClass('btnlink-white');
      this.$.attributeContent.removeClass('btnlink-fontblue');
      this.$.attributeContent.addClass('btnlink-orange');

      this.$.fromLbl.applyStyle('color', 'black');
      this.$.fromBin.removeClass('btnlink-white');
      this.$.fromBin.removeClass('btnlink-fontblue');
      this.$.fromBin.addClass('btnlink-orange');

      this.$.toLbl.applyStyle('color', 'black');
      this.$.toBin.removeClass('btnlink-white');
      this.$.toBin.removeClass('btnlink-fontblue');
      this.$.toBin.addClass('btnlink-orange');

    } else {
      // SCAN
      this.$.editor.applyStyle('background-color', '#7da7d9');

      this.$.productLbl.applyStyle('color', 'white');
      this.$.product.addClass('btnlink-white');
      this.$.product.addClass('btnlink-fontblue');
      this.$.product.removeClass('btnlink-orange');

      this.$.qty.applyStyle('color', 'white');

      this.$.attributesLbl.applyStyle('color', 'white');
      this.$.attributeContent.addClass('btnlink-white');
      this.$.attributeContent.addClass('btnlink-fontblue');
      this.$.attributeContent.removeClass('btnlink-orange');

      this.$.fromLbl.applyStyle('color', 'white');
      this.$.fromBin.addClass('btnlink-white');
      this.$.fromBin.addClass('btnlink-fontblue');
      this.$.fromBin.removeClass('btnlink-orange');

      this.$.toLbl.applyStyle('color', 'white');
      this.$.toBin.addClass('btnlink-white');
      this.$.toBin.addClass('btnlink-fontblue');
      this.$.toBin.removeClass('btnlink-orange');
    }
  },

  components: [{
    name: 'editor',
    style: 'position:relative; background-color: #7da7d9; background-size: cover; color: white; height: 200px; margin: 5px; padding: 5px',

    components: [{
      components: [{
        components: [{
          style: 'float: left; width: 40%;',
          components: [{
            name: 'productLbl',
            classes: 'WHLabel'
          }, {
            kind: 'OB.UI.SmallButton',
            name: 'product',
            classes: 'btnlink-white btnlink-fontblue',
            style: 'width: 95%',
            tap: function () {
              this.bubble('onShowPopup', {
                popup: 'modalProductSearch'
              });
            }
          }]
        }, {
          name: 'attribute',
          style: 'visibility: hidden; float: left; width: 40%;',
          components: [{
            name: 'attributesLbl',
            classes: 'WHLabel'
          }, {
            kind: 'OB.UI.SmallButton',
            name: 'attributeContent',
            classes: 'btnlink-white btnlink-fontblue',
            style: 'width: 95%',
            tap: function () {
              this.bubble('onShowPopup', {
                popup: 'modalAttributeSearch'
              });
            }
          }]
        }, {
          style: 'float: left; width: 20%;',
          components: [{
            style: 'visibility: hidden;',
            content: '.'
          }, {
            kind: 'OB.UI.SmallButton',
            name: 'deleteLineButton',
            style: 'float: right; width: 75%;',
            classes: 'btnlink-orange',
            tap: function () {
              var me = this;
              OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBWH_DeleteLineTitle'), OB.I18N.getLabel('OBWH_DeleteLineText'), [{
                label: OB.I18N.getLabel('OBWH_DeleteLbl'),
                action: function () {
                  me.bubble('onDeleteLine');
                }
              }, {
                label: OB.I18N.getLabel('OBMOBC_LblCancel')
              }]);
            },
            initComponents: function () {
              this.setContent(OB.I18N.getLabel('OBWH_DeleteLbl'));
            }
          }]
        }]
      }, {
        style: 'clear:both'
      }, {
        style: 'padding: 10px 0px 10px 0px; float:left;',
        components: [{
          name: 'qty',
          classes: 'WHLabel'
        }]
      }, {
        style: 'clear:both'
      }, {
        components: [{
          style: 'float: left; width: 40%;',
          components: [{
            name: 'fromLbl',
            classes: 'WHLabel'
          }, {
            kind: 'OB.UI.SmallButton',
            name: 'fromBin',
            classes: 'btnlink-white btnlink-fontblue',
            style: 'width: 95%',
            tap: function () {
              this.bubble('onShowPopup', {
                popup: 'modalBinSearch',
                args: {
                  type: 'from'
                }
              });
            }
          }]
        }, {
          style: 'float: left; width: 40%;',
          components: [{
            name: 'toLbl',
            classes: 'WHLabel'
          }, {
            kind: 'OB.UI.SmallButton',
            name: 'toBin',
            classes: 'btnlink-white btnlink-fontblue',
            style: 'width: 95%',
            tap: function () {
              this.bubble('onShowPopup', {
                popup: 'modalBinSearch',
                args: {
                  type: 'to'
                }
              });
            }
          }]
        }, {
          style: 'float: left; width: 20%;',
          components: [{
            style: 'visibility: hidden;',
            content: '.'
          }, {
            kind: 'OB.UI.SmallButton',
            name: 'doneLineEditButton',
            classes: 'btn-icon-adaptative btn-icon-check btnlink-white',
            style: 'width: 75%; visibility: hidden; float: right;',
            tap: function () {
              this.bubble('onDoneLineEdit');
            }
          }]
        }]
      }]
    }]
  }, {
    kind: 'OBWH.Movement.Keyboard',
    name: 'keyboard'
  }],

  currentLineChanged: function () {
    var params;
    this.$.keyboard.line = this.currentLine;

    this.currentLine.on('change', function (line) {
      // show done button only when all fields are filled
      var show;
      if (line.get('isNewLine') === undefined) {
        return;
      }
      if (!line.get('isNewLine')) {
        show = false;
      } else {
        show = line.get('product.id') && line.get('fromBin.id') && line.get('toBin.id') && line.get('quantity');
        show = show && (!line.get('product.attributeset.hasAttribute') || line.get('product.attributeset.instance.id'));
      }
      this.$.doneLineEditButton.applyStyle('visibility', show ? 'visible' : 'hidden');
    }, this);

    this.currentLine.on('change:product.name', function (line) {
      this.$.product.setContent(line.get('product.name'));
    }, this);

    this.currentLine.on('change:quantity product.uom.name', function (line) {
      if (line.get('product.uom.name')) {
        this.$.qty.setContent(OB.I18N.getLabel('OBWH_QtyInfo', [line.get('quantity'), line.get('product.uom.name')]));
      } else {
        this.$.qty.setContent('');
      }
    }, this);

    this.currentLine.on('change:fromBin.name', function (line) {
      this.$.fromBin.setContent(line.get('fromBin.name'));
    }, this);

    this.currentLine.on('change:toBin.name', function (line) {
      this.$.toBin.setContent(line.get('toBin.name'));
    }, this);

    this.currentLine.on('change:product.attributeset.hasAttribute', function (line) {
      this.$.attribute.applyStyle('visibility', line.get('product.attributeset.hasAttribute') ? 'visible' : 'hidden');
    }, this);

    this.currentLine.on('change:product.attributeset.instance.name', function (line) {
      this.$.attributeContent.setContent(line.get('product.attributeset.instance.name'));
    }, this);

    params = this.currentLine.get('windowModel').get('parameters');

    if (params.product) {
      this.$.product.setDisabled(params.product.readonly);
    }

    if (params.fromBin) {
      this.$.fromBin.setDisabled(params.fromBin.readonly);
    }

    if (params.toBin) {
      this.$.toBin.setDisabled(params.toBin.readonly);
    }
  },

  initComponents: function () {
    this.inherited(arguments);
    this.$.productLbl.setContent(OB.I18N.getLabel('OBWH_Product'));
    this.$.attributesLbl.setContent(OB.I18N.getLabel('OBWH_Attributes'));
    this.$.fromLbl.setContent(OB.I18N.getLabel('OBWH_fromBin'));
    this.$.toLbl.setContent(OB.I18N.getLabel('OBWH_toBin'));
  }
});

enyo.kind({
  name: 'OBWH.Movement.Keyboard',
  kind: 'OB.UI.Keyboard',
  sideBarEnabled: true,
  events: {
    onSetQuantity: '',
    onScan: ''
  },
  keyMatcher: /^([0-9]|\.|,| |:|\/|-|[a-z]|[A-Z])$/,

  buttonsDef: {
    sideBar: {
      plusI18nLbl: '',
      minusI18nLbl: '',
      priceI18nLbl: '',
      discountI18nLbl: ''
    }
  },

  initComponents: function () {
    var toolbar, me = this,
        addQty;



    addQty = function (keyboard, value) {
      var total;
      if (!keyboard.line) {
        return;
      }
      total = keyboard.line.get('quantity') + value;
      if (total < 0) {
        total = 0;
      }
      me.doSetQuantity({
        quantity: total
      });
    };

    this.addCommand('code', new OBWH.Movement.BarcodeHandler({
      keyboard: this
    }));

    this.addCommand('line:qty', {
      action: function (keyboard, qty) {
        var value = OB.I18N.parseNumber(qty);
        if (!keyboard.line) {
          return true;
        }
        if (value || value === 0) {
          me.doSetQuantity({
            quantity: value
          });
        }
      }
    });

    this.addCommand(':', {
      stateless: true,
      action: function (keyboard, qty) {
        keyboard.writeCharacter(':');
      }
    });

    this.addCommand('/', {
      stateless: true,
      action: function (keyboard, qty) {
        keyboard.writeCharacter('/');
      }
    });

    this.addCommand('-', {
      stateless: true,
      action: function (keyboard, qty) {
        keyboard.writeCharacter('-');
      }
    });

    // calling super after setting keyboard properties
    this.inherited(arguments);

    this.addKeypad('OBWH.Movement.MyKeypad');

    toolbar = {
      name: 'toolbarscan',
      buttons: [{
        command: 'code',
        label: OB.I18N.getLabel('OBMOBC_KbCode'),
        classButtonActive: 'btnactive-blue'
      }],
      shown: function () {
        var keyboard = this.owner.owner;
        keyboard.showKeypad('obwh-keypad');
        keyboard.showSidepad('sideenabled');
        keyboard.defaultcommand = 'code';
      }
    };

    this.addToolbar(toolbar);
    this.showKeypad('obwh-keypad');
    this.showToolbar('toolbarscan');

    this.defaultcommand = 'code';
  }
});

enyo.kind({
  name: 'OBWH.Movement.BarcodeHandler',
  kind: 'OB.UI.AbstractBarcodeActionHandler',
  events: {
    onScan: ''
  },
  findProductByBarcode: function (code, callback, keyboard) {
    keyboard.doScan({
      code: code
    });
  }
});

enyo.kind({
  name: 'OBWH.Movement.MyKeypad',
  kind: 'OB.UI.KeypadBasic',
  padName: 'obwh-keypad',
  toolbarButtons: [{
    label: '/',
    command: '/'
  }, {
    label: '-',
    command: '-'
  }, {
    label: ':',
    command: ':'
  }]
});