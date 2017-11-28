/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, Backbone*/

enyo.kind({
  name: 'OBWH.Movement.BinSearch',
  kind: 'OB.UI.Modal',
  published: {
    currentLine: null
  },
  topPosition: '125px',
  body: {
    kind: 'OBWH.Movement.BinSearchList',
    name: 'list'
  },
  executeOnShow: function () {
    this.waterfall('onClearAction');
  },
  show: function (arg) {
    this.inherited(arguments);
    this.type = arg.type;
    this.$.body.$.list.setType(this.type);
    this.setHeader(OB.I18N.getLabel('OBWH_' + this.type + 'Bin'));
    },

    currentLineChanged: function () {
      this.$.body.$.list.setCurrentLine(this.currentLine);
    }
  });

enyo.kind({
  name: 'OBWH.Movement.BinSearchList',
  classes: 'row-fluid',
  published: {
    binListCollection: null,
    currentLine: null,
    type: null
  },
  handlers: {
    onSearchAction: 'searchAction',
    onClearAction: 'clearAction'
  },
  events: {
    onSetBin: '',
    onHideThisPopup: ''
  },
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;',
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          name: 'binList',
          kind: 'OB.UI.ScrollableTable',
          scrollAreaMaxHeight: '400px',
          renderHeader: 'OBWH.Movement.BinSearchHeader',
          renderLine: 'OBWH.Movement.BinLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],

  currentLineChanged: function () {
    this.currentLine.on('change:product.id', function () {
      this.binListCollection.reset();
    }, this);
  },

  clearAction: function (inSender, inEvent) {
    this.$.binList.$.theader.$.binSearchHeader.clearFilter();
    this.binListCollection.reset();
    return true;
  },

  searchAction: function (inSender, inEvent) {
    OB.Dal.find(OB.Model.Locator, this.getWhereClause(inEvent.query), enyo.bind(this, function (prodCol) {
      if (prodCol && prodCol.length > 0) {
        this.binListCollection.reset(prodCol.models);
        if (prodCol.length === 1) {
          this.doSetBin({
            bin: prodCol.at(0),
            type: this.parent.parent.type
          });

          this.doHideThisPopup();
        }
      } else {
        this.binListCollection.reset();
      }
    }));

    return true;
  },

  getWhereClause: function (qry) {
    var identifier, criteria = {};
    // TODO: include validations regarding selected product
    if (qry) {
      identifier = OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;

      criteria = {
        _OrExpression: true,
        operator: 'or',
        _constructor: 'AdvancedCriteria',
        criteria: [{
          'fieldName': 'warehouse' + identifier,
          'operator': 'iContains',
          'value': qry
        }, {
          'fieldName': OB.Constants.IDENTIFIER,
          'operator': 'iContains',
          'value': qry
        }, {
          'fieldName': 'barcode',
          'operator': 'iContains',
          'value': qry
        }]
      };
    }

    if (this.type === 'from' && this.currentLine.get('product.id')) {
      criteria._where = 'exists (select 1 from MaterialMgmtStorageDetail d ' + //
      ' where d.storageBin = e ' + // 
      '   and d.product.id = \'' + this.currentLine.get('product.id') + '\'' + // 
      '   and d.quantityOnHand > 0)';
    }

    return criteria;
  },

  init: function () {
    this.binListCollection = new Backbone.Collection();
    this.$.binList.setCollection(this.binListCollection);
    this.binListCollection.on('click', function (model) {
      this.doSetBin({
        bin: model,
        type: this.parent.parent.type
      });
    }, this);
  }
});

enyo.kind({
  name: 'OBWH.Movement.BinSearchHeader',
  kind: 'OB.UI.ScrollableTableHeader',
  events: {
    onSearchAction: '',
    onClearAction: ''
  },
  components: [{
    style: 'padding: 10px;',
    components: [{
      style: 'display: table;',
      components: [{
        style: 'display: table-cell; width: 100%;',
        components: [{
          kind: 'OB.UI.SearchInputAutoFilter',
          name: 'filterText',
          style: 'width: 100%'
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          classes: 'btnlink-gray btn-icon-small btn-icon-clear',
          style: 'width: 100px; margin: 0px 5px 8px 19px;',
          ontap: 'clearAction'
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          classes: 'btnlink-yellow btn-icon-small btn-icon-search',
          style: 'width: 100px; margin: 0px 0px 8px 5px;',
          ontap: 'searchAction'
        }]
      }]
    }]
  }],
  clearFilter: function () {
    this.$.filterText.setValue('');
  },
  clearAction: function () {
    this.$.filterText.setValue('');
    this.doClearAction();
  },
  searchAction: function () {
    this.doSearchAction({
      query: this.$.filterText.getValue()
    });
    return true;
  }
});

enyo.kind({
  name: 'OBWH.Movement.BinLine',
  kind: 'OB.UI.SelectButton',
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      name: 'warehouse'
    }, {
      style: 'color: #888888',
      name: 'name'
    }, {
      style: 'color: #888888',
      name: 'attributes'
    }, {
      style: 'clear: both;'
    }]
  }],
  events: {
    onHideThisPopup: ''
  },
  tap: function () {
    this.inherited(arguments);
    this.doHideThisPopup();
  },
  create: function () {
    this.inherited(arguments);
    this.$.warehouse.setContent(this.model.get('warehouse' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER));
    this.$.name.setContent(this.model.get(OB.Constants.IDENTIFIER));
  }
});