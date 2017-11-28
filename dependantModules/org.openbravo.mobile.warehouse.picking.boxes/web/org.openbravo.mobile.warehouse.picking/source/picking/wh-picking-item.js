/*
 ************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, OBWH, _*/

enyo.kind({
  name: 'OBWH.Picking.LineProperty',
  style: 'display: table-row;',
  components: [{
    style: 'width: 45%; display: table-cell; vertical-align: middle; font-weight: bold;',
    name: 'propertyLabel'
  }, {
    style: 'width: 54%; display: table-cell; vertical-align: middle;',
    name: 'propertyValue'
  }],
  render: function (model) {
    if (model) {
      this.$.propertyValue.setContent(model.get(this.propertyToPrint));
    } else {
      this.$.propertyValue.setContent('');
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    if (this.I18NLabel) {
      this.$.propertyLabel.setContent(OB.I18N.getLabel(this.I18NLabel));
    } else if (this.label) {
      this.$.propertyLabel.setContent(this.label);
    }
    if (this.value) {
      this.$.propertyValue.setContent(this.value);
    }
  }
});



enyo.kind({
  name: 'OBWH.Picking.RightToolbar',
  kind: 'OB.UI.MultiColumn.Toolbar',

  showMenu: false,

  buttons: [{
    kind: 'OBWH.Picking.ToolbarButton',
    name: 'scan',
    enableScan: 'true',
    keepEnabledWhenActive: true,
    span: '6'
  }, {
    kind: 'OBWH.Picking.ToolbarButton',
    name: 'edit',
    span: '6'
  }]
});

enyo.kind({
  name: 'OBWH.Picking.ToolbarButton',
  kind: 'OB.UI.ToolbarButtonTab',
  events: {
    onActivateTab: ''
  },
  tap: function () {
    if (this.disabled) {
      //android
      if (this.enableScan) {
        OB.MobileApp.view.scanningFocus(true);
        setTimeout(function () {
          OB.MobileApp.view.scanningFocus(false);
        }, 500);
      }
      return true;
    }
    this.doActivateTab({
      tab: this.name
    });
    if (this.enableScan) {
      OB.MobileApp.view.scanningFocus(true);
      setTimeout(function () {
        OB.MobileApp.view.scanningFocus(false);
      }, 500);
    } else {
      //in android we have to quit the focus from focuskeeper manually
      if (document.activeElement.getAttribute("id") === '_focusKeeper') {
        OB.MobileApp.view.scanningFocus(false);
        document.activeElement.blur();
      }
    }
  },
  setTabActive: function (tab) {
    this.parent.parent.addRemoveClass('active', this.name === tab);
    this.setDisabled(tab === this.name);
  },
  initComponents: function () {
    this.inherited(arguments);
    if (this.name === 'scan') {
      this.setContent(OB.I18N.getLabel('OBWH_BtnScan'));
    } else {
      this.setContent(OB.I18N.getLabel('OBWH_BtnEdit'));
    }
  },
  init: function (model) {
    var me = this;
    this.setTabActive(model.get('activeTab'));
    model.on('change:activeTab', function () {
      me.setTabActive(model.get('activeTab'));
      if (model.get('activeTab') !== 'scan') {
        if (model.get('activeTab') === 'edit' && OB.MobileApp.model.hasPermission('OBMWHP_AlwaysScan', true)) {
          OB.MobileApp.view.scanningFocus(true);
        } else {
          OB.MobileApp.view.scanningFocus(false);
        }
      }
    });
  }
});

enyo.kind({
  name: 'OBWH.Picking.ItemView',
  handlers: {
    onBoxSelectedToPick: 'boxSelectedToPick',
    onMoveEditPropertiesPage: 'moveEditPropertiesPage'
  },
  propertiesToShow: [{
    kind: 'OBWH.Picking.LineProperty',
    position: 10,
    name: 'product',
    I18NLabel: 'OBMWHP_ProductProp',
    render: function (line) {
      if (line) {
        this.$.propertyValue.setContent(line.get('productName'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 20,
    name: 'productAtt',
    I18NLabel: 'OBMWHP_AttributeProp',
    render: function (line) {
      if (line) {
        this.$.propertyValue.setContent(line.get('attributeSetValueName'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 30,
    name: 'status',
    I18NLabel: 'OBMWHP_StatusProp',
    render: function (line) {
      if (line) {
        this.$.propertyValue.setContent(line.get('status') + ' (' + OBWH.Picking.Model.ItemStatus[line.get('status')] + ')');
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 40,
    name: 'qtyToPick',
    I18NLabel: 'OBMWHP_QtyToPickProp',
    render: function (line) {
      if (line) {
        this.$.propertyValue.setContent(line.get('neededQty'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 50,
    name: 'pickedQty',
    I18NLabel: 'OBMWHP_PickedQtyProp',
    render: function (line) {
      if (line) {
        this.$.propertyValue.setContent(line.get('pickedQty'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 60,
    name: 'boxContent',
    I18NLabel: 'OBMWHP_Box',
    render: function (line) {
      if (line && line.get('picking') && line.get('picking').get('currentBox') && line.get('picking').get('currentBox').id) {
        this.show();
        this.$.propertyLabel.setContent(OB.I18N.getLabel('OBMWHP_Box') + line.get('picking').get('currentBox').get('boxno'));
        if (line.get('boxes') && line.get('boxes')[line.get('picking').get('currentBox').id]) {
          this.$.propertyValue.setContent(line.get('boxes')[line.get('picking').get('currentBox').id]);
        } else {
          this.$.propertyValue.setContent('0');
        }
      } else {
        this.hide();
      }
    }
  }],
  components: [{
    name: 'tabsContainer',
    components: [{
      name: 'scanContainer',
      style: 'color: white; position: relative; background-color: #7da7d9; background-size: cover;  height: 200px; margin: 5px; padding: 5px',
      components: [{
        style: 'position: absolute; bottom: 0; right: 0; margin: 5px; padding: 5px;',
        kind: 'OB.UI.Clock'
      }]
    }, {
      name: 'editContainer',
      style: 'background-color: #ffffff; color: black; height: 200px; margin: 5px; padding: 5px',
      components: [{
        classes: 'row-fluid',
        components: [{
          name: 'linePropertiesHeaderContainer',
          components: [{
            components: [{
              name: 'linePropertiesHeaderContainer_title',
              classes: 'span8',
              //here should go buttons but now are not needed
              style: 'font-size: 1.5em; font-size: bold; margin: 15px 0px; padding-left: 5px;',
              //content set in initComponents
              content: ''
            }, {
              classes: 'span3',
              components: [{
                name: 'linePropertiesHeaderContainer_otherPropertiesButton',
                kind: 'OB.UI.Button',
                classes: 'btnlink btnlink-small btnlink-orange',
                //Dynamic content
                content: '1/2',
                ontap: 'moveEditPropertiesPage'
              }]
            }]
          }]
        }, {
          name: 'linePropertiesContentContainer',
          components: [{
            name: 'linePropertiesContent_page1',
            classes: 'span12',
            components: [{
              classes: 'span7',
              kind: 'Scroller',
              maxHeight: '139px',
              thumb: true,
              horizontal: 'hidden',
              style: 'padding: 6px 0px 4px 25px; line-height: 120%;',
              components: [{
                style: 'display: table; overflow: hidden; width: 100%; border-spacing: 0px 4px;',
                name: 'linePropertiesContainer'
              }]
            }, {
              classes: 'span4',
              sytle: 'text-align: right',
              components: [{
                style: 'padding: 2px 10px 10px 10px;',
                components: [{
                  classes: 'image-wrap image-editline',
                  contentType: 'image/png',
                  style: 'width: 128px; height: 128px',
                  components: [{
                    tag: 'img',
                    name: 'icon',
                    style: 'margin: auto; height: 100%; width: 100%; background-size: contain; background-repeat:no-repeat; background-position:center;'
                  }]
                }]
              }]
            }]
          }, {
            name: 'linePropertiesContent_page2',
            classes: 'span12',
            showing: false,
            components: [{
              kind: 'Scroller',
              maxHeight: '139px',
              thumb: true,
              horizontal: 'hidden',
              style: 'padding: 6px 0px 4px 25px; line-height: 120%;',
              components: [{
                style: 'display: table; overflow: hidden; width: 100%; border-spacing: 0px 4px;',
                name: 'relatedOrdersInformationContainer'
              }]
            }]
          }]
        }]
      }]
    }, {
      kind: 'OBWH.Picking.KeyboardScan',
      name: 'keyboardscan'
    }, {
      kind: 'OBWH.Picking.KeyboardEdit',
      name: 'keyboardedit'
    }]
  }],

  setTabActive: function (scanMode) {
    if (scanMode) {
      this.$.scanContainer.show();
      this.$.editContainer.hide();
      this.$.keyboardscan.show();
      this.$.keyboardscan.isEnabled = true;
      this.$.keyboardscan.defaultcommand = 'code';
      this.$.keyboardedit.hide();
      this.$.keyboardedit.isEnabled = false;
    } else {
      this.$.scanContainer.hide();
      this.$.editContainer.show();
      this.$.keyboardscan.hide();
      this.$.keyboardscan.isEnabled = false;
      this.$.keyboardedit.show();
      this.$.keyboardedit.isEnabled = true;
      if (OB.MobileApp.model.hasPermission('OBMWHP_AlwaysScan', true)) {
        this.$.keyboardedit.defaultcommand = 'code';
      } else {
        this.$.keyboardedit.defaultcommand = 'add';
      }
    }
  },
  boxSelectedToPick: function (inSender, inEvent) {
    this.func_render();
    return true;
  },
  moveEditPropertiesPage: function (inSender, inEvent) {
    if (inEvent.pageToSet) {
      if (inEvent.pageToSet !== this.currentEditPropertiesPage) {
        if (inEvent.pageToSet === 2) {
          this.$.linePropertiesContent_page1.hide();
          this.$.linePropertiesContent_page2.show();
          this.currentEditPropertiesPage = 2;
        } else if (inEvent.pageToSet === 1) {
          this.$.linePropertiesContent_page2.hide();
          this.$.linePropertiesContent_page1.show();
          this.currentEditPropertiesPage = 1;
        }
      }
    } else if (this.currentEditPropertiesPage === 1) {
      this.$.linePropertiesContent_page1.hide();
      this.$.linePropertiesContent_page2.show();
      this.currentEditPropertiesPage = 2;
    } else {
      this.$.linePropertiesContent_page2.hide();
      this.$.linePropertiesContent_page1.show();
      this.currentEditPropertiesPage = 1;
    }
    this.$.linePropertiesHeaderContainer_otherPropertiesButton.setContent(this.currentEditPropertiesPage.toString() + '/2');
  },
  init: function (model) {
    var me = this;
    //Default values for line properties
    this.defaultEditPropertiesPage = 1;
    this.currentEditPropertiesPage = 1;
    //read preference to set value for defaultEditPropertiesPage
    try {
      this.defaultEditPropertiesPage = Number(OB.MobileApp.model.get('permissions').OBMWHP_defaultLinePropertiesPage);
    } catch (e) {
      OB.Warn("Preference -OBMWHP_defaultLinePropertiesPage- cannot be read. The value shoud be a number (1 or 2)");
    }

    this.model = model;
    this.currentItem = null;
    this.func_render = function (args) {
      var position = 0,
          orderLine, orderName, orderLabel, orderValue, me = this;
      if (!(OB.UTIL.isNullOrUndefined(this.currentItem))) {
        enyo.forEach(this.$.linePropertiesContainer.getComponents(), function (compToRender) {
          if (compToRender.kindName.indexOf("enyo.") !== 0) {
            compToRender.render(this.currentItem, args);
          }
        }, this);

        this.$.relatedOrdersInformationContainer.destroyComponents();
        if (this && this.currentItem && this.currentItem.get('byOrders')) {
          _.each(this.currentItem.get('byOrders'), function (orderToDraw) {
            position += 10;
            orderName = 'salesOrderRelation_';
            if (orderToDraw && orderToDraw.order && orderToDraw.order.id) {
              orderName += orderToDraw.order.id;
              orderLabel = orderToDraw.order.documentNo;
              orderValue = orderToDraw.quantity;
            } else {
              orderName += 'unknown';
              orderLabel = OB.I18N.getLabel('OBMWHP_unknown');
              orderValue = orderToDraw.quantity;
            }
            orderLabel = OB.I18N.getLabel('OBMWHP_Order') + ' ' + orderLabel + ':';

            var relatedOrdersComponent = {
              kind: 'OBWH.Picking.LineProperty',
              position: 0,
              name: orderName,
              label: orderLabel,
              value: orderValue
            };

            OB.MobileApp.model.hookManager.executeHooks('OBMWHP_relatedOrdersInformation', {
              relatedOrdersComponent: relatedOrdersComponent,
              orderToDraw: orderToDraw
            }, function (args) {
              if (!args.cancellation) {
                me.$.relatedOrdersInformationContainer.createComponent(relatedOrdersComponent);
              }
            });
          }, this);
          this.$.relatedOrdersInformationContainer.render();
          this.moveEditPropertiesPage(null, {
            pageToSet: this.defaultEditPropertiesPage
          });
        }
      } else {
        model.set('activeTab', 'scan');
        me.setTabActive(true);
      }
    };
    model.on('change:currentItem', function (model) {
      this.currentItem = model.get('currentItem');
      this.func_render();
      if (this && this.currentItem) {
        this.currentItem.on('pickedQtyChanged', enyo.bind(this, this.func_render));
        this.currentItem.on('change:status', enyo.bind(this, this.func_render));
        //TODO: get product image
        this.$.icon.applyStyle('background-image', 'url(' + '../../org.openbravo.mobile.core.productimageprovider?id=' + this.currentItem.get('product') + '), url(' + '../org.openbravo.mobile.core/assets/img/box.png' + ')');
      }
    }, this);
    me.setTabActive(model.get('activeTab') === 'scan');
    model.on('change:activeTab', function () {
      me.setTabActive(model.get('activeTab') === 'scan');
    });
  },
  initComponents: function () {
    var sortedPropertiesByPosition;
    this.inherited(arguments);
    this.$.linePropertiesHeaderContainer_title.setContent(OB.I18N.getLabel('OBMWHP_linePropertiesTitle'));
    this.$.linePropertiesHeaderContainer_otherPropertiesButton.setContent('1/2');
    sortedPropertiesByPosition = _.sortBy(this.propertiesToShow, function (comp) {
      return (comp.position ? comp.position : (comp.position === 0 ? 0 : 999));
    });
    enyo.forEach(sortedPropertiesByPosition, function (compToCreate) {
      this.$.linePropertiesContainer.createComponent(compToCreate);
    }, this);
  }
});

enyo.kind({
  name: 'OBWH.Picking.KeyboardScan',
  kind: 'OB.UI.Keyboard',
  sideBarEnabled: false,
  events: {
    onScan: ''
  },
  keyMatcher: /^([0-9]|\.|,| |%|[a-z]|[A-Z])$/,

  initComponents: function () {
    var toolbar, me = this;

    this.addCommand('code', new OBWH.Picking.BarcodeHandler({
      keyboard: this
    }));

    // calling super after setting keyboard properties
    this.inherited(arguments);

    this.addKeypad('OBWH.Picking.MyKeypad');

    toolbar = {
      name: 'toolbarscan',
      buttons: [{
        command: 'code',
        label: OB.I18N.getLabel('OBMOBC_KbCode'),
        classButtonActive: 'btnactive-blue'
      }],
      shown: function () {
        var keyboard = this.owner.owner;
        keyboard.showKeypad('obmwhp-keypad');
        keyboard.defaultcommand = 'code';
      }
    };

    this.addToolbar(toolbar);
    this.showKeypad('obmwhp-keypad');
    this.showToolbar('toolbarscan');

    this.defaultcommand = 'code';
  },
  globalKeypressHandler: function (inSender, inEvent) {
    if (!this.isEnabled) {
      return;
    }
    this.inherited(arguments);
  }
});

enyo.kind({
  name: 'OBWH.Picking.BarcodeHandler',
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
  name: 'OBWH.Picking.MyKeypad',
  kind: 'OB.UI.KeypadBasic',
  padName: 'obmwhp-keypad',
  toolbarButtons: [{
    label: ' ',
    command: ''
  }, {
    label: ' ',
    command: ''
  }, {
    label: ' ',
    command: 'Add:'
  }]
});

enyo.kind({
  name: 'OBWH.Picking.KeyboardEdit',
  kind: 'OB.UI.Keyboard',
  sideBarEnabled: true,
  events: {
    onSetQuantity: '',
    onRaiseIncidence: '',
    onResetIncidence: '',
    onConfirmIncidence: '',
    onScan: ''
  },
  keyMatcher: /^([0-9]|\.|,)$/,

  buttonsDef: {
    sideBar: {
      qtyI18nLbl: '',
      priceI18nLbl: '',
      discountI18nLbl: ''
    }
  },

  initComponents: function () {
    var toolbar, me = this;

    this.addCommand('add', {
      action: function (keyboard, qty) {
        if (me.showing) {
          var value = OB.I18N.parseNumber(qty);
          if (value || value === 0) {
            me.doSetQuantity({
              quantity: value,
              incremental: false
            });
          }
        }
      }
    });

    this.addCommand('-', {
      stateless: true,
      action: function (keyboard, qty) {
        //remove
        if (me.showing) {
          var value = OB.I18N.parseNumber(qty);
          if (!value) {
            value = Number(1);
          }
          value = value * -1;
          me.doSetQuantity({
            quantity: value,
            incremental: true
          });
        }
      }
    });

    this.addCommand('+', {
      stateless: true,
      action: function (keyboard, qty) {
        //remove
        if (me.showing) {
          var value = OB.I18N.parseNumber(qty);
          if (!value) {
            value = Number(1);
          }
          me.doSetQuantity({
            quantity: value,
            incremental: true
          });
        }
      }
    });

    this.addCommand('incidence', {
      stateless: true,
      action: function (keyboard, qty) {
        me.doRaiseIncidence();
      }
    });

    this.addCommand('undoincidence', {
      stateless: true,
      action: function (keyboard, qty) {
        me.doResetIncidence();
      }
    });

    if (!(OB.MobileApp.model.attributes.permissions.OBWPL_autoConfirmIncidences && OB.MobileApp.model.attributes.permissions.OBWPL_autoConfirmIncidences === true)) {
      this.addCommand('confirmincidence', {
        stateless: true,
        action: function (keyboard, qty) {
          me.doConfirmIncidence();
        }
      });
    }

    // calling super after setting keyboard properties
    this.inherited(arguments);

    this.addKeypad('OBWH.Picking.MyKeypadBasic');
    toolbar = {
      name: 'toolbaredit',
      buttons: [{
        command: 'incidence',
        label: OB.I18N.getLabel('OBMWHP_Incidence'),
        classButtonActive: 'btnactive-blue'
      }, {
        command: 'undoincidence',
        label: OB.I18N.getLabel('OBMWHP_UndoIncidence'),
        classButtonActive: 'btnactive-blue'
      }],
      shown: function () {
        var keyboard = this.owner.owner;
        keyboard.showSidepad('sideenabled');
        keyboard.showKeypad('obmwhp-keypadbasic');
        keyboard.defaultcommand = 'add';
      }
    };

    if (!(OB.MobileApp.model.attributes.permissions.OBWPL_autoConfirmIncidences && OB.MobileApp.model.attributes.permissions.OBWPL_autoConfirmIncidences === true)) {
      toolbar.buttons.push({
        command: 'confirmincidence',
        label: OB.I18N.getLabel('OBMWHP_ConfirmIncidence'),
        classButtonActive: 'btnactive-blue'
      });
    }

    this.addToolbar(toolbar);
    this.showKeypad('obmwhp-keypadbasic');
    this.showToolbar('toolbaredit');
    this.defaultcommand = 'add';
  }

});

enyo.kind({
  name: 'OBWH.Picking.MyKeypadBasic',
  kind: 'OB.UI.KeypadBasic',
  padName: 'obmwhp-keypadbasic',
  toolbarButtons: [{
    label: ' ',
    command: ''
  }, {
    label: ' ',
    command: ''
  }, {
    label: ' ',
    command: ''
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.toolbarBtn1.disableButton(this.$.toolbarBtn1, {
      disabled: true
    });
  }
});