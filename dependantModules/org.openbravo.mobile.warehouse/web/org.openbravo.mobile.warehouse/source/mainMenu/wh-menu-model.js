/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, Backbone,  _, enyo, generateEntry*/

OBWH.Menu = OBWH.Menu || {};

OBWH.Menu.Model = OB.Model.WindowModel.extend({
  models: [],

  menuGeneratorClass: 'org.openbravo.mobile.warehouse.menu.Menu',

  composeMenu: function (response) {

    function generateMenu(menu, level) {
      var entry, menuModel = new Backbone.Collection(),
          i;
      for (i = 0; i < menu.length; i++) {
        entry = generateEntry(menu[i], level);
        menuModel.add(entry);
      }

      if (level > 0) {
        menuModel.add({
          title: OB.I18N.getLabel('OBWH_MenuBack'),
          back: true
        });
      }

      return menuModel;
    }

    function generateEntry(entry, level) {
      var entryModel = new Backbone.Model(),
          submenu;
      entryModel.set({
        title: entry.title,
        path: entry.path,
        parameters: entry.parameters
      });

      if (entry.submenu) {
        submenu = generateMenu(entry.submenu, level + 1);
        entryModel.set('submenu', submenu);
      }
      return entryModel;
    }

    var menu = generateMenu(response, 0);

    this.set('menu', menu);
    this.set('wholeMenu', menu);

    menu.on('click', this.menuHandler, this);
    this.trigger('menuChanged');
  },

  menuHandler: function (model) {
    var submenu, params;
    var me = this;
    if (model.get('submenu')) {
      submenu = model.get('submenu');
      me.get('menu').off();
      submenu.on('click', this.menuHandler, this);
      me.set('menu', submenu);
      me.trigger('menuChanged');
    } else if (model.get('back')) {
      me.set('menu', this.get('wholeMenu'));
      this.get('wholeMenu').on('click', this.menuHandler, this);
      me.trigger('menuChanged');
    } else if (model.get('path')) {
      params = _.extend({
        title: model.get('title')
      }, model.get('parameters'));
      OB.MobileApp.model.navigate(model.get('path') + "/" + encodeURI(JSON.stringify(params).replace("/", "@@")));
    }
  },

  init: function () {
    var me = this,
        proc = new OB.DS.Process(this.menuGeneratorClass);

    proc.exec({}, enyo.bind(this, this.composeMenu), function () {
      window.console.error('error');
    });
  }
});