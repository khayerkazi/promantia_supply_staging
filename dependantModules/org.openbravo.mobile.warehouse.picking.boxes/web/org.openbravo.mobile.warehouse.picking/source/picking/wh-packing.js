/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, OBWH, Backbone, _*/

enyo.kind({
  name: 'OBWH.Picking.BoxSelector',
  kind: 'OB.UI.Modal',
  topPosition: '125px',
  i18nHeader: 'OBMWHP_BoxSelector_Header',
  body: {
    kind: 'OBWH.Picking.BoxSelector_Body',
    name: 'boxSelectorBody'
  },
  executeOnShow: function () {
    if (!this.model.get('picking').get('picking').get('usePickingBoxes')) {
      this.$.body.$.boxSelectorBody.$.boxButtonsContainer.$.addBoxButton.setDisabled(true);
      return false;
    }
    this.$.body.$.boxSelectorBody.$.boxListContainer.$.boxSelectorList.setCollection(this.model.get('picking').get('boxes'));
  },
  init: function (model) {
    this.model = model;
  }
});

enyo.kind({
  name: 'OBWH.Picking.BoxSelector_Body',
  components: [{
    kind: 'OBWH.Picking.BoxSelector_ListContainer',
    name: 'boxListContainer'
  }, {
    kind: 'OBWH.Picking.BoxSelector_ButtonsContainer',
    name: 'boxButtonsContainer'
  }]
});


enyo.kind({
  name: 'OBWH.Picking.BoxSelector_ListContainer',
  classes: 'row-fluid',
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;',
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          name: 'boxSelectorList',
          kind: 'OB.UI.ScrollableTable',
          scrollAreaMaxHeight: '400px',
          renderLine: 'OBWH.Picking.Box_RenderLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],
  init: function (model) {
    this.model = model;
  }
});

enyo.kind({
  name: 'OBWH.Picking.BoxSelector_AddBoxButton',
  kind: 'OB.UI.SmallButton',
  i18nContent: 'OBMWHP_addBox',
  classes: 'btnlink-orange',
  events: {
    onShowPopup: '',
    onHideThisPopup: ''
  },
  tap: function () {
    this.doHideThisPopup();
    this.doShowPopup({
      popup: 'modalAddBox'
    });
  }
});

enyo.kind({
  name: 'OBWH.Picking.BoxSelector_ButtonsContainer',
  components: [{
    classes: 'BoxSelector_ButtonsContainer',
    components: [{
      kind: 'OBWH.Picking.BoxSelector_AddBoxButton',
      name: 'addBoxButton'
    }, {
      style: 'clear: both;'
    }]
  }]
});

enyo.kind({
  name: 'OBWH.Picking.Box_RenderLine',
  kind: 'OB.UI.SelectButton',
  events: {
    onHideThisPopup: '',
    onBoxSelected: ''
  },
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      name: 'identifier'
    }, {
      name: 'barcode',
      classes: 'incidenceTypeIdentifier'
    }, {
      style: 'clear: both;'
    }]
  }],
  tap: function () {
    this.inherited(arguments);
    this.doBoxSelected({
      box: this.model
    });
    this.doHideThisPopup();
  },
  create: function () {
    this.inherited(arguments);
    this.$.identifier.setContent(OB.I18N.getLabel('OBMWHP_BoxNo') + ': ' + this.model.get('boxno'));
    this.$.barcode.setContent(OB.I18N.getLabel('OBMWHP_TrackingNo') + ': ' + this.model.get('searchKey'));
  }
});

/*Create boxes*/

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OBWH.Picking.AddBoxOk',
  isDefaultAction: true,
  events: {
    onApplyChanges: ''
  },
  tap: function () {
    this.doApplyChanges();
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMWHP_CreateBox'));
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OBWH.Picking.AddBoxCancel',
  tap: function () {
    this.doHideThisPopup();
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMOBC_LblCancel'));
  }
});

enyo.kind({
  name: 'OB.UI.ModalAddBox',
  kind: 'OB.UI.ModalAction',
  classes: 'ModalAddBox',
  i18nHeader: 'OBMWHP_CreateBox',
  executeOnShow: function () {
    this.$.bodyContent.$.BoxNoLabel.setContent(OB.I18N.getLabel('OBMWHP_BoxNo'));
    if (this.model.get('picking').get('boxes') && this.model.get('picking').get('boxes').length > 0) {
      this.$.bodyContent.$.BoxNoValue.setContent(this.model.get('picking').get('boxes').at(this.model.get('picking').get('boxes').length - 1).get('boxno') + 1);
    } else {
      this.$.bodyContent.$.BoxNoValue.setContent('1');
    }
    this.$.bodyContent.$.serialNoLabel.setContent(OB.I18N.getLabel('OBMWHP_TrackingNo'));
    this.$.bodyContent.$.serialNoValue.setValue('');
    this.$.bodyContent.$.errors.hide();
    this.$.bodyButtons.$.addBoxOk.setDisabled(false);
    this.$.bodyButtons.$.addBoxOk.setContent(OB.I18N.getLabel('OBMWHP_CreateBox'));
  },
  handlers: {
    onApplyChanges: 'applyChanges'
  },
  events: {
    onHideThisPopup: '',
    onBoxSelected: ''
  },
  applyChanges: function () {
    var boxNo = this.$.bodyContent.$.BoxNoValue.getContent();
    var serialNo = this.$.bodyContent.$.serialNoValue.getValue();
    this.$.bodyContent.$.errors.hide();
    this.$.bodyContent.$.errors.setContent('');
    this.$.bodyButtons.$.addBoxOk.setDisabled(true);
    this.$.bodyButtons.$.addBoxOk.setContent(OB.I18N.getLabel('OBMWHP_loading'));
    this.model.createBox([{
      boxNo: boxNo,
      trackingNo: serialNo
    }], {
      callback: enyo.bind(this, this.boxCreated)
    });
  },
  boxCreated: function (response) {
    if (response && response.exception) {
      this.$.bodyButtons.$.addBoxOk.setDisabled(false);
      this.$.bodyButtons.$.addBoxOk.setContent(OB.I18N.getLabel('OBMWHP_CreateBox'));
      this.$.bodyContent.$.errors.show();
      this.$.bodyContent.$.errors.setContent(response.exception.message);
    } else {
      if (this.model.get('picking').get('boxes') && this.model.get('picking').get('boxes').length > 0) {
        //last box of the collection is selected automatically
        this.doBoxSelected({
          box: this.model.get('picking').get('boxes').at(this.model.get('picking').get('boxes').length - 1)
        });
      }
      this.doHideThisPopup();
    }
  },
  bodyContent: {
    components: [{
      kind: 'Scroller',
      maxHeight: '225px',
      style: 'background-color: #ffffff; color: black;',
      thumb: true,
      horizontal: 'hidden',
      components: [{
        classes: 'addBoxLine',
        components: [{
          classes: 'span3 addBoxLabel',
          name: 'BoxNoLabel'
        }, {
          classes: 'span9 addBoxContent',
          name: 'BoxNoValue'
        }, {
          style: 'clear: both;'
        }]
      }, {
        classes: 'addBoxLine',
        components: [{
          classes: 'span3 addBoxLabel',
          name: 'serialNoLabel'
        }, {
          classes: 'span9',
          components: [{
            kind: 'enyo.Input',
            type: 'text',
            name: 'serialNoValue'
          }]
        }, {
          style: 'clear: both;'
        }]
      }]
    }, {
      name: 'errors',
      showing: false
    }]
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.AddBoxOk'
    }, {
      kind: 'OBWH.Picking.AddBoxCancel'
    }]
  },
  init: function (model) {
    this.model = model;
  }
});