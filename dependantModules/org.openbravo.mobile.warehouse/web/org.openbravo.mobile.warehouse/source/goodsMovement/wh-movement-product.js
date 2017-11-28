/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, _, Backbone*/

enyo.kind({
  name: 'OBWH.Movement.ProductSearch',
  kind: 'OB.UI.Modal',
  published: {
    currentLine: null
  },
  topPosition: '125px',
  i18nHeader: 'OBWH_Product',
  body: {
    kind: 'OBWH.Movement.ProductSearchList',
    name: 'list'
  },
  currentLineChanged: function () {
    this.$.body.$.list.setCurrentLine(this.currentLine);
  }
});

enyo.kind({
  name: 'OBWH.Movement.ProductSearchList',
  classes: 'row-fluid',
  published: {
    productList: null,
    currentLine: null
  },
  handlers: {
    onSearchAction: 'searchAction',
    onClearAction: 'clearAction'
  },
  events: {
    onSetProduct: ''
  },
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;',
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          name: 'prodList',
          kind: 'OB.UI.ScrollableTable',
          scrollAreaMaxHeight: '400px',
          renderHeader: 'OBWH.Movement.ProductSearchHeader',
          renderLine: 'OBWH.Movement.ProductLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],

  currentLineChanged: function () {
    this.currentLine.on('change:fromBin.id', function () {
      this.productList.reset();
    }, this);
  },

  clearAction: function (inSender, inEvent) {
    this.productList.reset();
    return true;
  },

  searchAction: function (inSender, inEvent) {
    OB.Dal.find(OB.Model.ProductStockView, this.getWhereClause(inEvent.query), enyo.bind(this, function (prodCol) {
      if (prodCol && prodCol.length > 0) {
        this.productList.reset(prodCol.models);
      } else {
        this.productList.reset();
      }
    }));

    return true;
  },

  getWhereClause: function (qry) {
    var identifier, criteria = {
      _where: 'e.stocked = true and quantityOnHand > 0'
    };

    if (qry) {
      identifier = OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;

      criteria = _.extend(criteria, {
        _OrExpression: true,
        operator: 'or',
        _constructor: 'AdvancedCriteria',
        criteria: [{
          'fieldName': 'product' + identifier,
          'operator': 'iContains',
          'value': qry
        }, {
          'fieldName': 'storageBin' + identifier,
          'operator': 'iContains',
          'value': qry
        }, {
          'fieldName': 'attributeSetValue' + identifier,
          'operator': 'iContains',
          'value': qry
        }]
      });
    }
    if (this.currentLine.get('fromBin.id')) {
      criteria._where += ' and e.storageBin = \'' + this.currentLine.get('fromBin.id') + '\'';
    }
    return criteria;
  },

  init: function () {
    this.productList = new Backbone.Collection();
    this.$.prodList.setCollection(this.productList);
    this.productList.on('click', function (model) {
      this.doSetProduct({
        product: model
      });
    }, this);
  }
});

enyo.kind({
  name: 'OBWH.Movement.ProductSearchHeader',
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
  name: 'OBWH.Movement.ProductLine',
  kind: 'OB.UI.SelectButton',
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      name: 'identifier'
    }, {
      style: 'color: #888888',
      name: 'bin'
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
    this.doHideThisPopup();
    this.inherited(arguments);
  },
  create: function () {
    this.inherited(arguments);
    this.$.identifier.setContent(this.model.get(OB.Constants.IDENTIFIER));
    this.$.bin.setContent(this.model.get('storageBin' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER));
    this.$.attributes.setContent(this.model.get('attributeSetValue' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER));
  }
});