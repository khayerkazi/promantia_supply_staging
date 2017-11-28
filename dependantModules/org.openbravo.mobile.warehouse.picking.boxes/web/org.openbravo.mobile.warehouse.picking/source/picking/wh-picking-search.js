/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, OBWH, Backbone, _*/

enyo.kind({
  name: 'OBWH.Picking.PickingSearch',
  kind: 'OB.UI.Modal',
  topPosition: '125px',
  i18nHeader: 'OBMWHP_PickListChooser',
  body: {
    kind: 'OBWH.Picking.PickingSearchList',
    name: 'list'
  }
});

enyo.kind({
  name: 'OBWH.Picking.PickingSearchList',
  classes: 'row-fluid',
  published: {
    pickingList: null
  },
  handlers: {
    onSearchAction: 'searchAction',
    onClearAction: 'clearAction'
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
          scrollAreaMaxHeight: '400px',
          renderHeader: 'OBWH.Picking.PickingSearchHeader',
          renderLine: 'OBWH.Picking.PickingLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],

  clearAction: function (inSender, inEvent) {
    this.pickingList.reset();
    return true;
  },

  searchAction: function (inSender, inEvent) {
    var origEmpty = this.$.pickList.$.tempty.$.renderEmpty.getContent();
    this.$.pickList.$.tempty.$.renderEmpty.setContent('Loading...');
    var query = inEvent ? inEvent.query : null;
    OB.Dal.find(OB.Model.OBWPL_pickinglist, OBWH.Picking.Model.getPickingWhereClause(query), enyo.bind(this, function (prodCol) {
      this.$.pickList.$.tempty.$.renderEmpty.setContent(origEmpty);
      if (prodCol && prodCol.length > 0) {
        this.pickingList.reset(prodCol.models);
      } else {
        this.pickingList.reset();
      }
    }));

    return true;
  },

  init: function () {
    this.pickingList = new Backbone.Collection();
    this.$.pickList.setCollection(this.pickingList);
    this.pickingList.on('click', function (model) {
      this.doSelectPicking({
        picking: model
      });
    }, this);
    this.searchAction();
  }

});

enyo.kind({
  name: 'OBWH.Picking.PickingSearchHeader',
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
  name: 'OBWH.Picking.PickingLine',
  kind: 'OB.UI.SelectButton',
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      name: 'identifier'
    }, {
      style: 'color: #888888',
      name: 'status'
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
    if (OB.MobileApp.model.get('permissions').OBMWHP_showMobileIdentifier && _.isString(this.model.get('obmwhpMobileidentifier')) && this.model.get('obmwhpMobileidentifier').length > 0) {
      this.$.identifier.setContent(this.model.get('obmwhpMobileidentifier'));
    } else {
      this.$.identifier.setContent(this.model.get(OB.Constants.IDENTIFIER));
    }
    this.$.status.setContent(OBWH.Picking.Model.PickingStatus[this.model.get('pickliststatus')]);
  }
});