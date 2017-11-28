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
  name: 'OBWH.Movement.AttributeSearch',
  kind: 'OB.UI.Modal',
  published: {
    currentLine: null
  },
  topPosition: '125px',
  i18nHeader: 'OBWH_Attributes',
  body: {
    kind: 'OBWH.Movement.AttributeSearchList',
    name: 'list'
  },
  currentLineChanged: function () {
    this.$.body.$.list.setCurrentLine(this.currentLine);
  }
});

enyo.kind({
  name: 'OBWH.Movement.AttributeSearchList',
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
    onSetAttribute: ''
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
          renderHeader: 'OBWH.Movement.AttributeSearchHeader',
          renderLine: 'OBWH.Movement.AttributeLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],

  currentLineChanged: function () {
    this.currentLine.on('change:product.id', function () {
      this.attributeList.reset();
    }, this);
  },

  clearAction: function (inSender, inEvent) {
    this.attributeList.reset();
    return true;
  },

  searchAction: function (inSender, inEvent) {
    OB.Dal.find(OB.Model.ProductStockView, this.getWhereClause(inEvent.query), enyo.bind(this, function (attrCol) {
      if (attrCol && attrCol.length > 0) {
        this.attributeList.reset(attrCol.models);
      } else {
        this.attributeList.reset();
      }
    }));

    return true;
  },

  getWhereClause: function (qry) {
    var identifier, criteria = {
      _where: 'e.stocked = true and e.product.id = \'' + this.currentLine.get('product.id') + '\''
    };

    if (qry) {
      identifier = OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;

      criteria = _.extend(criteria, {
        _OrExpression: true,
        operator: 'or',
        _constructor: 'AdvancedCriteria',
        criteria: [{
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
    this.attributeList = new Backbone.Collection();
    this.$.prodList.setCollection(this.attributeList);
    this.attributeList.on('click', function (model) {
      this.doSetAttribute({
        attribute: model
      });
    }, this);
  }
});

enyo.kind({
  name: 'OBWH.Movement.AttributeSearchHeader',
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
  name: 'OBWH.Movement.AttributeLine',
  kind: 'OB.UI.SelectButton',
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      name: 'identifier'
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
    this.$.identifier.setContent(this.model.get('attributeSetValue' + OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER));
  }
});