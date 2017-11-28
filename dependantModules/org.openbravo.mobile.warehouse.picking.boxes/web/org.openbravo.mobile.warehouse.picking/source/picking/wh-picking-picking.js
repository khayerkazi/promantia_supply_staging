/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, OBWH, Backbone, _*/

/*pagination*/
enyo.kind({
  name: 'OBWH.Picking.PageSelector',
  pageManagerControler: 'pickingLinesPageManager',
  handlers: {
    onChangePage: 'pageChanged'
  },
  pageChanged: function (inSender, inEvent) {
    this.pageManager.movePage(inEvent.action, true);
    return true;
  },
  components: [{
    kind: 'OBWH.Picking.PageBefore',
    name: 'pageBefore'
  }, {
    name: 'pageNumber',
    style: 'line-height: 40px; font-size: 20px; text-align: center; width: 50%; display: inline-block;'
  }, {
    kind: 'OBWH.Picking.PageAfter',
    name: 'pageAfter'
  }],
  init: function (model) {
    this.pageManager = model.get(this.pageManagerControler);
    this.pageManager.on('change:currentPage', function () {
      this.$.pageNumber.setContent(OB.I18N.getLabel('OBMWHP_PageAdviser', [this.pageManager.getCurrentPageNumber(), this.pageManager.getPages()]));
      this.$.pageBefore.setDisabled(!this.pageManager.isPossibleBefore());
      this.$.pageAfter.setDisabled(!this.pageManager.isPossibleAfter());
    }, this);
    this.$.pageNumber.setContent('Loading...');
  }
});

enyo.kind({
  name: 'OBWH.Picking.PageBefore',
  kind: 'OB.UI.Button',
  classes: 'enyo-tool-decorator btnlink btnlink-small btnlink-orange pageSelector',
  content: '<',
  events: {
    onChangePage: ''
  },
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doChangePage({
      action: -1
    });
  }
});

enyo.kind({
  name: 'OBWH.Picking.PageAfter',
  kind: 'OB.UI.Button',
  classes: 'enyo-tool-decorator btnlink btnlink-small btnlink-orange pageSelector positionFixIPad',
  content: '>',
  events: {
    onChangePage: ''
  },
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doChangePage({
      action: 1
    });
  }
});

/*end pagination*/

/*header*/

enyo.kind({
  kind: 'OB.UI.SmallButton',
  name: 'OB.Picking.HideShowCompleted',
  content: '',
  classes: 'btnlink-orange',
  tap: function () {
    var me = this;
    me.removeClass('btnlink-orange');
    this.setTimeout(function () {
      me.addClass('btnlink-orange');
    }, 500);
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent('Hide completed');
  }
});

enyo.kind({
  name: 'OBWH.Picking.PickingTitle',
  published: {
    picking: null
  },
  components: [{
    name: 'identifier',
    attributes: {
      style: 'text-align: center; padding: 15px 0px 5px 0px; font-weight: bold; color: #6CB33F;'
    }
  }, {
    attributes: {
      style: 'clear: both;'
    }
  }],
  renderData: function (docNo) {
    if (OB.MobileApp.model.get('permissions').OBMWHP_showMobileIdentifier && _.isString(this.picking.get('obmwhpMobileidentifier')) && this.picking.get('obmwhpMobileidentifier').length > 0) {
      this.$.identifier.setContent(this.picking.get('obmwhpMobileidentifier'));
    } else {
      this.$.identifier.setContent(this.picking.get(OB.Constants.IDENTIFIER));
    }
  },
  pickingChanged: function (oldValue) {
    this.renderData();
  }
});

enyo.kind({
  name: 'OBWH.Picking.PickingInformation',
  published: {
    picking: null
  },
  components: [{
    attributes: {
      style: 'display: table; overflow: hidden; width: 100%;'
    },
    components: [{
      attributes: {
        style: 'display: table-cell; vertical-align: middle;'
      },
      components: [{
        //TODO: layout done. Implement functionallity.
        //        name: 'btnHideShowCompleted',
        //        kind: 'OB.Picking.HideShowCompleted',
        //        attributes: {
        //          style: 'float:left;'
        //        }
      }]
    }, {
      attributes: {
        style: 'display: table-cell; vertical-align: middle;'
      },
      components: [{
        name: 'outboundBinIdentifier',
        attributes: {
          //remove padding when button is ready
          style: 'float:right; padding: 7px 0 10px 0'
        }
      }]
    }]
  }],
  renderData: function () {
    this.$.outboundBinIdentifier.setContent(this.picking.get('outboundStorageBin$' + OB.Constants.IDENTIFIER));
  },
  pickingChanged: function (oldValue) {
    this.renderData();
  }
});

enyo.kind({
  name: 'OBWH.Picking.PickingHeader',
  kind: 'OB.UI.ScrollableTableHeader',
  components: [{
    name: 'pickingListIdentifier',
    kind: 'OBWH.Picking.PickingTitle'
  }, {
    name: 'pickingListInfo',
    kind: 'OBWH.Picking.PickingInformation'
  }],
  init: function (model) {
    var me = this,
        picking = model.get('picking');
    picking.get('items').on('add remove', function () {
      me.$.pickingListIdentifier.setPicking(picking.get('picking'));
      me.$.pickingListInfo.setPicking(picking.get('picking'));
    });
  }
});

/*end header*/

enyo.kind({
  name: 'OBWH.Picking.LeftToolbar',
  kind: 'OB.UI.MultiColumn.Toolbar',
  handlers: {
    onDisableButton: 'disableButton'
  },

  showMenu: true,
  menuEntries: [{
    kind: 'OB.UI.MenuAction',
    i18nLabel: 'OBMOBC_LblBack',
    eventName: 'onBackMenuAction',
    name: 'properties'
  }],
  buttons: [{
    kind: 'OBWH.Picking.ButtonSelect',
    name: 'btnSelect',
    span: 4
  }, {
    kind: 'OBWH.Picking.ButtonDone',
    name: 'btnDone',
    span: 4
  }, {
    kind: 'OBWH.Picking.ButtonBox',
    name: 'btnBoxes',
    span: 4
  }],

  disableButton: function (inSender, inEvent) {
    this.waterfall('onDisableMenuEntry', {
      entryName: 'properties',
      disable: inEvent.buttons.indexOf('new') === -1
    });
  },

  initComponents: function () {
    var nrOfButtons = 3;
    if (OB.MobileApp.model.get('permissions').OBMWHP_ShowScanShortcut) {
      if (this.buttons.length === nrOfButtons) {
        this.buttons[0].span = this.buttons[0].span - 1;
        this.buttons[1].span = this.buttons[1].span - 1;
        this.buttons.push({
          kind: 'OBWH.Picking.ButtonScanShortcut',
          name: 'btnScanShortcut',
          span: 2
        });
      }
    }
    this.inherited(arguments);
    this.waterfall('onDisableMenuEntry', {
      entryName: 'properties',
      disable: true
    });
  }
});

enyo.kind({
  name: 'OBWH.Picking.ButtonSelect',
  kind: 'OB.UI.ToolbarButton',
  icon: 'btn-icon btn-icon-new',
  handlers: {
    onDisableButton: 'disableButton'
  },
  events: {
    onShowPopup: ''
  },
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doShowPopup({
      popup: 'modalPickingSearch'
    });
  },
  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('select') !== -1);
    //return true;
  }
});

enyo.kind({
  name: 'OBWH.Picking.ButtonDone',
  kind: 'OB.UI.ToolbarButton',
  events: {
    onProcessPicking: ''
  },
  handlers: {
    onDisableButton: 'disableButton'
  },

  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('done') !== -1);
    return true;
  },

  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doProcessPicking();
  },

  init: function (model) {
    var me = this,
        picking = model.get('picking');
    picking.on('change:ready', function (model) {
      //enable button
      if (model.get('ready')) {
        me.setContent(OB.I18N.getLabel('OBMWHP_Confirm_Upper'));
        me.setDisabled(!model.get('ready'));
      } else {
        me.setContent(OB.I18N.getLabel('OBMWHP_Save_Upper'));
        me.setDisabled(!model.get('readyToSave'));
      }
    });
    picking.on('change:readyToSave', function (model) {
      //enable button
      me.setDisabled(!model.get('readyToSave'));
      me.setContent(OB.I18N.getLabel('OBMWHP_Save_Upper'));
    }, this);
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBWH_BtnDone'));
    this.setDisabled(true);
  }
});

enyo.kind({
  name: 'OBWH.Picking.ButtonBox',
  kind: 'OB.UI.ToolbarButton',
  events: {
    onShowPopup: ''
  },
  handlers: {
    onDisableButton: 'disableButton',
    onBoxSelectedToPick: 'boxSelectedToPick'
  },

  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('box') !== -1);
    return true;
  },

  boxSelectedToPick: function (inSender, inEvent) {
    if (inEvent.box) {
      this.setContent('Box id: ' + inEvent.box.get('_identifier'));
    } else {
      this.setContent(OB.I18N.getLabel('OBMWHP_Boxes'));
    }
  },

  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doShowPopup({
      popup: 'boxSelector'
    });
  },

  init: function (model) {
    this.model = model;
    this.model.get('picking').on('itemsLoaded', function () {
      if (this.model.get('picking').get('picking').get('usePickingBoxes')) {
        this.setDisabled(false);
      }
    }, this);
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMWHP_Boxes'));
    this.setDisabled(true);
  }
});

enyo.kind({
  name: 'OBWH.Picking.ButtonScanShortcut',
  kind: 'OB.UI.ToolbarButton',
  components: [{
    tag: 'img',
    attributes: {
      src: '../org.openbravo.mobile.warehouse.picking/assets/img/barcode_scanner.png'
    },
    style: 'width:45px;'
  }],
  events: {
    onActivateTab: ''
  },
  handlers: {
    onDisableButton: 'disableButton'
  },

  disableButton: function (inSender, inEvent) {
    this.setDisabled(inEvent.buttons.indexOf('scanShortcut') !== -1);
    return true;
  },

  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doActivateTab({
      tab: 'scan'
    });

    OB.MobileApp.view.scanningFocus(true);
    setTimeout(function () {
      OB.MobileApp.view.scanningFocus(false);
    }, 500);
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setContent('SC');
    this.setDisabled(true);
  },

  init: function (model) {
    this.model = model;
    //when this property loader is recovered from cache we've an array instead of a BBone collection,
    //but I want to have a Backbone collection, so I transform it
    if (OB.MobileApp.model.get('incidencesTypes') && OB.MobileApp.model.get('incidencesTypes').length > 0) {
      if (_.isArray(OB.MobileApp.model.get('incidencesTypes'))) {
        OB.Model.IncidenceTypes = Backbone.Model.extend();
        OB.Model.IncidenceTypesList = Backbone.Collection.extend({
          model: OB.Model.IncidenceTypes
        });
        OB.MobileApp.model.set('incidencesTypes', new OB.Model.IncidenceTypesList(OB.MobileApp.model.get('incidencesTypes')));
      }
    } else {
      OB.MobileApp.model.set('incidencesTypes', new OB.Model.IncidenceTypesList([]));
    }


    this.model.get('picking').on('itemsLoaded', function () {
      if (OB.MobileApp.model.get('permissions').OBMWHP_ShowScanShortcut) {
        this.setDisabled(false);
      }
    }, this);
  }
});

enyo.kind({
  name: 'OBWH.Picking.PickingView',
  classes: 'span12',

  components: [{
    style: 'overflow:auto; margin: 5px; position: relative; background-color: #ffffff; color: black; padding: 5px;',
    classes: 'row-fluid',
    components: [{
      name: 'picking',
      showing: false,
      components: [{
        kind: 'OBWH.Picking.PickingHeader',
        name: 'header'
      }, {
        kind: 'OBWH.Picking.PageSelector',
        name: 'pagination'
      }, {
        kind: 'OBWH.Picking.PickingDetails',
        name: 'details'
      }]
    }, {
      name: 'noPicking',
      kind: 'OBWH.Picking.BasePickingList'
    }]
  }],
  init: function (model) {
    var me = this;
    model.get('picking').on('change:picking.id', function () {
      me.$.noPicking.hide();
      me.$.picking.show();
    });
  }
});

//Assigned picking list scroll
enyo.kind({
  name: 'OBWH.Picking.BasePickingList',
  classes: 'row-fluid',
  published: {
    pickingList: null
  },
  events: {
    onSelectPicking: ''
  },
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;',
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          name: 'pickList',
          kind: 'OB.UI.ScrollableTable',
          scrollAreaMaxHeight: '595px',
          renderLine: 'OBWH.Picking.PickingLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],

  init: function () {
    this.pickingList = new Backbone.Collection();
    this.$.pickList.setCollection(this.pickingList);
    this.pickingList.on('click', function (model) {
      this.doSelectPicking({
        picking: model
      });
    }, this);
    var origEmpty = this.$.pickList.$.tempty.$.renderEmpty.getContent();
    this.$.pickList.$.tempty.$.renderEmpty.setContent('Loading...');
    OB.Dal.find(OB.Model.OBWPL_pickinglist, OBWH.Picking.Model.getPickingWhereClause(), enyo.bind(this, function (prodCol) {
      this.$.pickList.$.tempty.$.renderEmpty.setContent(origEmpty);
      if (prodCol && prodCol.length > 0) {
        this.pickingList.reset(prodCol.models);
      } else {
        this.pickingList.reset();
      }
    }));
  }
});

//scroll which contains bins and bins content
enyo.kind({
  name: 'OBWH.Picking.PickingDetails',
  kind: 'OB.UI.ScrollableTable',
  scrollAreaMaxHeight: '490px',
  renderLine: 'OBWH.Picking.Locator',
  renderEmpty: 'OB.UI.RenderEmpty',
  listStyle: 'edit',
  init: function (model) {
    var locators = model.get('pickingLinesPageManager').get('paginatedCollection');
    this.setCollection(locators);
  }
});

enyo.kind({
  name: 'OBWH.Picking.Locator',
  components: [{
    name: 'header',
    kind: 'OB.UI.ScrollableTableHeader',
    style: 'font-size: 24px; line-height: 32px;'
  }, {
    name: 'items',
    kind: 'OBWH.Picking.Items'
  }, {
    attributes: {
      style: 'float:right;'
    }
  }],
  initComponents: function () {
    this.inherited(arguments);
    var locator = this.model;
    this.$.header.setContent(locator.get('name'));
    this.$.items.setCollection(locator.get('items'));
  }
});

//bin content scroll
enyo.kind({
  name: 'OBWH.Picking.Items',
  kind: 'OB.UI.ScrollableTable',
  //scrollAreaMaxHeight: '460px',
  renderLine: 'OBWH.Picking.Item',
  renderEmpty: 'OB.UI.RenderEmpty',
  selectedCssClass: 'plLineSelected',
  listStyle: 'edit',
  autoSelectOnReset: false,
  scrollableTableGroup: 'pickingItems'
});

enyo.kind({
  name: 'OBWH.Picking.CheckItem',
  kind: 'OB.UI.CheckboxButton',
  tag: 'div',
  events: {
    onPickQtyTap: ''
  },
  tap: function () {
    if (!this.disabled && !this.checked) {
      if (this.scanned) {
        //using boxes. If not selected show error
        if (this.pickingListModel.get('picking').get('usePickingBoxes')) {
          if (OB.UTIL.isNullOrUndefined(this.pickingListModel.get('currentBox'))) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_BoxNotSelectedHeader'), OB.I18N.getLabel('OBMWHP_BoxNotSelectedBody'));
            return;
          }
        }
        this.inherited(arguments);
        this.doPickQtyTap();
      } else {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_scanIsNeeded_Header'), OB.I18N.getLabel('OBMWHP_scanIsNeeded_Body'));
      }
    }
  },

  initComponents: function () {
    var status = this.parent.model.get('status');
    this.pickingListModel = this.parent.model.get('picking');
    if (OB.MobileApp.model.get('permissions').OBMWHP_DisableLineUntillScan) {
      this.scanned = this.parent.model.get('scanned');
      this.parent.model.on('change:scanned', function (changedModel) {
        this.scanned = changedModel.get('scanned');
      }, this);
    } else {
      this.scanned = true;
    }
    this.inherited(arguments);
    this.checked = false;
    if (status === 'RE') {
      this.checked = true;
      this.setClassAttribute('btn-check active');
    } else if (status === 'IN' || status === 'IC') {
      this.setClassAttribute('btn-incidence');
      this.setDisabled(true);
    } else if (status === 'CO' || status === 'CF' || status === 'CWI') {
      this.setClassAttribute('btn-done');
      this.setDisabled(true);
    } else {
      this.setClassAttribute('btn-check');
    }
  }
});

enyo.kind({
  name: 'OBWH.Picking.PickQty',
  style: 'position:relative;',
  published: {
    disabled: false
  },
  events: {
    onPickQtyTap: ''
  },
  components: [{
    style: 'float: right; line-height: 32px;',
    name: 'pickedqty'
  }],
  tap: function () {
    this.inherited(arguments);
    if (!this.getDisabled()) {
      this.doPickQtyTap({
        qty: Number(1)
      });
    }
  },
  initComponents: function () {
    var item = this.parent.model,
        status = item.get('status');
    this.inherited(arguments);
    this.$.pickedqty.setContent(item.get('pickedQty'));
    if (status === 'CO' || status === 'CF' || status === 'IN' || status === 'IC' || status === 'CWI') {
      this.setDisabled(true);
      this.$.pickedqty.addStyles('font-size: 16px;');
      if (status === 'IC') {
        this.$.pickedqty.setContent('IC - (' + item.get('pickedQty') + ')');
      } else if (status === 'IN') {
        this.$.pickedqty.setContent('IN - (' + item.get('pickedQty') + ')');
      } else if (status === 'CWI') {
        this.$.pickedqty.setContent('CWI - (' + item.get('pickedQty') + ')');
      } else {
        this.$.pickedqty.setContent(OBWH.Picking.Model.ItemStatus[status]);
      }
    } else {
      this.$.pickedqty.addStyles('font-size: 24px;');
    }
  }
});

enyo.kind({
  name: 'OBWH.Picking.Item',
  kind: 'OB.UI.SelectButton',
  classes: 'btnselect-orderline',
  events: {
    onStatusChange: ''
  },
  handlers: {
    onPickQtyTap: 'pickQtyTapHandler'
  },
  components: [{
    style: 'float: left; width: 10%;',
    kind: 'OBWH.Picking.CheckItem',
    name: 'setQty'
  }, {
    style: 'float: left; width: 50%;',
    components: [{
      name: 'attribute'
    }, {
      name: 'product',
      style: 'font-size: 16px;'
    }]
  }, {
    style: 'float: left; width: 10%;',
    name: 'iconContainer',
    classes: 'hidden',
    components: [{
      name: 'img',
      tag: 'img',
      attributes: {
        src: '../org.openbravo.mobile.warehouse.picking/assets/img/star-32.png'
      }
    }]
  }, {
    style: 'float: left; width: 15%; font-size: 24px; line-height: 32px;',
    name: 'quantity'
  }, {
    style: 'float: left; width: 15%; font-size: 24px; line-height: 32px;',
    kind: 'OBWH.Picking.PickQty',
    name: 'pickQty'
  }],
  pickQtyTapHandler: function (inSender, inEvent) {
    var qty;

    if (inEvent.qty) {
      qty = inEvent.qty + this.model.get('pickedQty');
    } else {
      qty = this.model.get('neededQty');
    }
    this.model.setPickedQty(qty);
    this.model.trigger('selected', this.model);
  },

  initComponents: function () {
    var item = this.model,
        me = this,
        status = item.get('status');
    this.inherited(arguments);
    this.$.product.setContent(item.get('productName'));
    this.$.attribute.setContent(item.get('attributeSetValueName'));
    this.$.quantity.setContent(item.get('neededQty'));
    if (status === 'IN') {
      this.addStyles('background-color: #FFE1C0;');
      this.setDisabled(true);
    } else if (status === 'IC') {
      this.addStyles('background-color: #F9FCAF;');
      this.setDisabled(true);
    } else if (status === 'CO' || status === 'CF' || status === 'CWI') {
      this.addStyles('background-color: #BBFFBB;');
      this.setDisabled(true);
    }
    OB.MobileApp.model.hookManager.executeHooks('OBMWHP_renderMovementLine', {
      component: this,
      item: item
    }, function () {
      //Nothing to do here
    });
  }
});