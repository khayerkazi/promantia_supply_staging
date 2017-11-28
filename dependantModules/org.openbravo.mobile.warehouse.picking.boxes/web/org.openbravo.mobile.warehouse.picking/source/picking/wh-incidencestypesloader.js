/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo, Backbone, _, console*/


(function () {


  OB.MobileApp.model.addPropertiesLoader({
    properties: ['incidencesTypes'],
    loadFunction: function (terminalModel) {
      OB.Model.IncidenceTypes = Backbone.Model.extend();
      OB.Model.IncidenceTypesList = Backbone.Collection.extend({
        model: OB.Model.IncidenceTypes
      });
      console.log('loading... ' + this.properties);
      var me = this;
      new OB.DS.Request('org.openbravo.mobile.warehouse.picking.terminal.IncidencesTypesLoader').exec({}, function (data) {
        // data is ready. Save it
        terminalModel.set(me.properties[0], new OB.Model.IncidenceTypesList(data));
        // this loader has finished
        terminalModel.propertiesReady(me.properties);
      });
    }
  });

  OB.MobileApp.model.addPropertiesLoader({
    properties: ['baseIncidencesTypes'],
    loadFunction: function (terminalModel) {
      console.log('loading... ' + this.properties);
      var me = this;
      new OB.DS.Request('org.openbravo.mobile.warehouse.picking.terminal.BaseIncidencesTypesLoader').exec({}, function (data) {
        // data is ready. Save it      
        terminalModel.set(me.properties[0], data);
        // this loader has finished
        terminalModel.propertiesReady(me.properties);
      });
    }
  });

  OB.OBMWHP = OB.OBMWHP || {};
  OB.OBMWHP.Utils = OB.OBMWHP.Utils || {};

  OB.OBMWHP.Utils.getBaseIncidenceTypeBySk = function (sk) {
    return _.find(OB.MobileApp.model.get('baseIncidencesTypes'), function (item) {
      return item.searchKey === sk;
    });
  };

  OB.OBMWHP.Utils.getIncidenceTypeById = function (id) {
    return _.find(OB.MobileApp.model.get('incidencesTypes').models, function (item) {
      if (item.id === id) {
        item.set('baseIncidenceType', OB.OBMWHP.Utils.getBaseIncidenceTypeBySk(item.get('incidencetype')));
        return true;
      }
      return false;
    });
  };

  OB.OBMWHP.Utils.IsStandardIncidencePresent = function (id) {
    var result = _.find(OB.MobileApp.model.get('incidencesTypes').models, function (item) {
      return item.searchKey === OB.OBMWHP.Utils.StandardIncidenceSk;
    });

    if (result) {
      return true;
    } else {
      return false;
    }
  };

  OB.OBMWHP.Utils.getIncidenceFromServer = function (id, callback, errorCallback) {
    var criteria = {
      _where: 'e.id = \'' + id + '\'',
      _limit: 1
    };
    OB.Dal.find(OB.Model.OBWPL_pickinglistproblem, criteria, function (response) {
      if (response.exception) {
        errorCallback(response);
      } else {
        if (response.length === 1) {
          response.at(0).set('incidenceType', OB.OBMWHP.Utils.getIncidenceTypeById(response.at(0).get('obwplPickinglistincidence')));
          callback(response.at(0));
        } else {
          errorCallback({
            exception: {
              message: 'Several incidences for id'
            }
          });
        }
      }
    }, function (response) {
      errorCallback(response);
    });
  };

  OB.OBMWHP.Utils.StandardIncidenceSk = 'OBWPL_StandardIncidence';

}());