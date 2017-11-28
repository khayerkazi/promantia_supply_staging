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
  name: 'OBWH.Menu.View',
  kind: 'OB.UI.WindowView',
  windowmodel: OBWH.Menu.Model,
  published: {
    menu: null
  },

  components: [{
    kind: 'OB.UI.MultiColumn',
    name: 'multiColumn',
    leftToolbar: {
      kind: 'OB.UI.MultiColumn.Toolbar',
      showMenu: true,
      buttons: [{
        kind: 'OB.UI.MultiColumn.EmptyToolbar',
        i18nLabel: 'OBWH_MenuTitle'
      }]
    },
    leftPanel: {
      kind: 'OBWH.Menu.Panel',
      name: 'menuPanel'
    }
  }],

  menuChanged: function () {
    this.$.multiColumn.$.leftPanel.$.menuPanel.setMenu(this.menu);
  },

  init: function () {
    this.inherited(arguments);
    OB.MobileApp.view.scanningFocus(false);
    this.setMenu(this.model.get('menu'));
    this.model.on('menuChanged', function () {
      this.setMenu(this.model.get('menu'));
    }, this);
  },

  initComponents: function () {
    this.inherited(arguments);
    this.$.multiColumn.setRightShowing(false);
  }
});

enyo.kind({
  name: 'OBWH.Menu.Panel',
  kind: 'OB.UI.ScrollableTable',
  scrollAreaMaxHeight: '437px',
  classes: 'WHMenuContainer',
  renderLine: 'OBWH.Menu.Item',
  renderEmpty: 'OBWH.Menu.Empty',
  published: {
    menu: null
  },
  menuChanged: function () {
    this.setCollection(this.menu);
  }
});

enyo.kind({
  name: 'OBWH.Menu.Item',
  kind: 'OB.UI.SelectButton',
  classes: 'WHMenuText',
  initComponents: function () {
    this.setContent(this.model.get('title'));
  }
});

enyo.kind({
  name: 'OBWH.Menu.Empty',
  style: 'border-bottom: 1px solid #cccccc; padding: 20px; text-align: center; font-weight: bold; font-size: 30px; color: #cccccc',
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBWH_EmptyMenu'));
  }
});

OB.MobileApp.windowRegistry.registerWindow({
  windowClass: 'OBWH.Menu.View',
  route: 'wh',
  menuPosition: null
});