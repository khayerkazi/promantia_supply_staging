/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone, _ */

OB.Model.WindowModel = Backbone.Model.extend({
  data: {},

  load: function () {
    var me = this,
        queue = {},
        initIfInit = function () {
        if (me.init) {
          me.init();
        }
        };
    if (!this.models) {
      this.models = [];
    }

    _.extend(this.models, Backbone.Events);

    this.models.on('ready', function () {
      if (!OB.MobileApp.model.get('loggedUsingCache')) {
        initIfInit();
        OB.MobileApp.model.set('isLoggingIn', false);
        this.trigger('ready');
      }
    }, this);
    if (!OB.MobileApp.model.get('loggedOffline')) {
      OB.Dal.loadModels(true, me.models, me.data);
      if (OB.MobileApp.model.get('loggedUsingCache')) {
        initIfInit();
        this.trigger('ready');
      }
    } else {
      initIfInit();
      this.trigger('ready');
    }
  },

  setAllOff: function (model) {
    var p;
    if (model.off) {
      model.off();
    }
    if (model.attributes) {
      for (p in model.attributes) {
        if (model.attributes.hasOwnProperty(p) && model.attributes[p]) {
          this.setAllOff(model);
        }
      }
    }
  },

  setOff: function () {
    if (!this.data) {
      return;
    }
    if (this.data) {
      _.forEach(this.data, function (model) {
        this.setAllOff(model);
      }, this);
    }
    this.data = null;

    if (this.models) {
      _.forEach(this.models, function (model) {
        if (model.off) {
          model.off();
        }
      }, this);
      if (this.models.off) {
        this.models.off();
      }
    }
    this.models = null;
  },

  getData: function (dsName) {
    return this.data[dsName];
  }
});