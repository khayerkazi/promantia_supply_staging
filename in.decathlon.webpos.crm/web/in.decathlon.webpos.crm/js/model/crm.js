/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.

 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B,_,moment,Backbone,localStorage, enyo,DWCRM */

DWCRM = DWCRM || {};
DWCRM.crmCustomer = DWCRM.crmCustomer || {};
(function() {
  var crmCustomer = Backbone.Model.extend({
    modelName: 'CRMCustomer',
    tableName: 'dwcrm_crm',
    entityName: 'CRMCustomer',
    source: '',
    dataLimit: 300,
    properties: ['id', 'json'],
    propertyMap: {
      'id': 'dwcrm_crm_id',
      'json': 'json'
    },


    createStatement: 'CREATE TABLE IF NOT EXISTS dwcrm_crm (dwcrm_crm_id TEXT PRIMARY KEY, json CLOB)',
    insertStatement: 'INSERT INTO dwcrm_crm(dwcrm_crm_id, json) VALUES (?,?)',
    dropStatement: 'DROP TABLE IF EXISTS dwcrm_crm',
    deletStatement: 'DELETE FROM dwcrm_crm WHERE dwcrm_crm_id in (?)',

    local: true,


    initialize: function(attributes) {


      var crmId;
      OB.Dal.initCache(OB.Model.CRMCustomer, [], null, null);
      this.clearCRMAttributes();

      //this.set('id',OB.Dal.get_uuid());

    },
    save: function() {
      var undoCopy;
      this.set('id', OB.Dal.get_uuid());
      if (this.attributes.json) {
        delete this.attributes.json; // Needed to avoid recursive inclusions of itself !!!
      }
      undoCopy = this.get('undo');
      this.unset('undo');
      this.set('json', JSON.stringify(this.toJSON()));
      OB.Dal.save(this, function() {}, function() {
        window.console.error(arguments);
      }, true);
      this.set('undo', undoCopy);
    },

    clear: function() {
      this.clearCRMAttributes();
      this.trigger('change');
      this.trigger('clear');
    },

    load: function(mobileNo, emailId, landlineNo, civility, firstName, lastName, decathlonId, zipcode, smsOpt, emailOpt) {
      this.set('name', firstName);
      this.set('name2', lastName);
      this.set('rCMobile', mobileNo);
      this.set('rCLand', landlineNo);
      this.set('rCEmail', emailId);
      this.set('rCNotify', emailOpt);
      this.set('rcZipcode', zipcode);
      this.set('rCSms', smsOpt);
      this.set('rCOptin', smsOpt || emailOpt);
      this.set('customer', true);
      this.set('civility', civility);
      this.set('rCDecid', decathlonId);
    },
    getIdentifier: function() {
      var identifier = "";
      if (this.get('rCMobile') !== '' && this.get('rCMobile') !== null) {
        identifier = this.get('rCMobile');
      } else if (this.get('rCEmail') !== '' && this.get('rCEmail') !== null) {
        identifier = this.get('rCEmail');
      } else if (this.get('rCLand') !== '' && this.get('rCLand') !== null) {
        identifier = this.get('rCLand');
      }
      return identifier;
    },
    clearCRMAttributes: function() {
      this.set('name', null);
      this.set('name2', null);
      this.set('rCMobile', '');
      this.set('rCEmail', '');
      this.set('rCLand', '');
      this.set('rCNotify', null);
      this.set('rcZipcode', null);
      this.set('rcSms', null);
      this.set('customer', null);
      this.set('civility', null);

    }


  });
  //Register customer
  window.OB.Model.CRMCustomer = crmCustomer;
}());