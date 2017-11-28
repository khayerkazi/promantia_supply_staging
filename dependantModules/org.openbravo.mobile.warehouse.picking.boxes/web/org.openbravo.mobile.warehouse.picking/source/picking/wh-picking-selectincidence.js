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
  name: 'OBWH.Picking.IncidenceTypeSelector',
  kind: 'OB.UI.Modal',
  topPosition: '125px',
  i18nHeader: 'OBMWHP_IncidenceTypeSelector_Header',
  body: {
    kind: 'OBWH.Picking.IncidenceTypeSelector_ListContainer',
    name: 'incidenceTypeSelectorListContainer'
  }
});

enyo.kind({
  name: 'OBWH.Picking.IncidenceTypeSelector_ListContainer',
  classes: 'row-fluid',
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;',
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          name: 'incidenceTypeSelectorList',
          kind: 'OB.UI.ScrollableTable',
          scrollAreaMaxHeight: '400px',
          renderLine: 'OBWH.Picking.IncidenceTypeSelectorList_Line',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }],
  init: function () {
    this.$.incidenceTypeSelectorList.setCollection(OB.MobileApp.model.get('incidencesTypes'));
  }
});

enyo.kind({
  name: 'OBWH.Picking.IncidenceTypeSelectorList_Line',
  kind: 'OB.UI.SelectButton',
  events: {
    onHideThisPopup: '',
    onShowPopup: ''
  },
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      name: 'identifier'
    }, {
      name: 'type',
      classes: 'incidenceTypeIdentifier'

    }, {
      style: 'clear: both;'
    }]
  }],
  tap: function () {
    this.doHideThisPopup();
    this.inherited(arguments);
    this.doShowPopup({
      popup: 'OBWH.Picking.IncidencesActions.' + this.model.get('incidencetype'),
      args: this.model.toJSON()
    });
  },
  create: function () {
    this.inherited(arguments);
    this.$.identifier.setContent(this.model.get('_identifier'));
    this.$.type.setContent(OB.OBMWHP.Utils.getBaseIncidenceTypeBySk(this.model.get('incidencetype'))._identifier);
  }
});