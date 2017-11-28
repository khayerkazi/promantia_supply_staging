/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B,_*/

(function () {

  OB = window.OB || {};
  OB.DATA = window.OB.DATA || {};

  OB.DATA.CustomerSave = function (model) {
    this.context = model;
    this.customer = model.get('customer');


    this.customer.on('customerSaved', function () {
      var me = this,
          customersList, customerId = this.customer.get('id'),
          isNew = false,
          bpToSave = new OB.Model.ChangedBusinessPartners(),
          bpLocToSave = new OB.Model.BPLocation(),
          customersListToChange;

      bpToSave.set('isbeingprocessed', 'N');
      this.customer.set('createdBy', OB.POS.modelterminal.get('orgUserId'));
      bpToSave.set('createdBy', OB.POS.modelterminal.get('orgUserId'));
      if (customerId) {
        this.customer.set('posTerminal', OB.POS.modelterminal.get('terminal').id);
        bpToSave.set('json', JSON.stringify(this.customer.serializeToJSON()));
        bpToSave.set('c_bpartner_id', this.customer.get('id'));
      } else {
        isNew = true;
      }

      //save that the customer is being processed by server
      OB.Dal.save(this.customer, function () {
        //OB.UTIL.showSuccess(OB.I18N.getLabel('OBPOS_customerSavedSuccessfullyLocally',[me.customer.get('_identifier')]));
        // Saving Customer Address locally
        bpLocToSave.set('id', me.customer.get('locId'));
        bpLocToSave.set('bpartner', me.customer.get('id'));
        bpLocToSave.set('name', me.customer.get('locName'));
        bpLocToSave.set('postalCode', me.customer.get('postalCode'));
        bpLocToSave.set('cityName', me.customer.get('cityName'));
        if (isNew) {
          bpLocToSave.set('countryName', OB.POS.modelterminal.get('terminal').defaultbp_bpcountry_name);
          bpLocToSave.set('countryId', OB.POS.modelterminal.get('terminal').defaultbp_bpcountry);
        } else {
          bpLocToSave.set('countryName', me.customer.get('countryName'));
          bpLocToSave.set('countryId', me.customer.get('country'));
        }
        bpLocToSave.set('_identifier', me.customer.get('locName'));
        OB.Dal.save(bpLocToSave, function () {}, function () {
          OB.error(arguments);
        }, isNew);

        if (isNew) {
          me.customer.set('posTerminal', OB.POS.modelterminal.get('terminal').id);
          bpToSave.set('json', JSON.stringify(me.customer.serializeToJSON()));
          bpToSave.set('c_bpartner_id', me.customer.get('id'));
        }
        bpToSave.set('isbeingprocessed', 'Y');
        OB.Dal.save(bpToSave, function () {
          bpToSave.set('json', me.customer.serializeToJSON());
          var successCallback, errorCallback, List;
          successCallback = function () {
            OB.UTIL.showSuccess(OB.I18N.getLabel('OBPOS_customerSaved', [me.customer.get('_identifier')]));
          };
          OB.MobileApp.model.runSyncProcess(successCallback);
        }, function () {
          //error saving BP changes with changes in changedbusinesspartners
          OB.UTIL.showError(OB.I18N.getLabel('OBPOS_errorSavingCustomerChanges', [me.customer.get('_identifier')]));
        });
      }, function () {
        //error saving BP with new values in c_bpartner
        OB.UTIL.showError(OB.I18N.getLabel('OBPOS_errorSavingCustomerLocally', [me.customer.get('_identifier')]));
      });
    }, this);
  };
}());