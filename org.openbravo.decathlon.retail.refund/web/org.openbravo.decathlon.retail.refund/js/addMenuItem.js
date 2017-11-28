/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo */

(function () {

  enyo.kind({
    name: 'DSIREF.UI.MenuRefunds',
    kind: 'OB.UI.MenuAction',
    i18nLabel: 'DSIREF_RefundsMenuEntry',
    events: {
      onShowPopup: ''
    },
    tap: function () {
      this.inherited(arguments);
      //check for customer
      if ((OB.UTIL.isNullOrUndefined(this.model.get('order').get('rCMobileNo')) || this.model.get('order').get('rCMobileNo') === '') && (OB.UTIL.isNullOrUndefined(this.model.get('order').get('syncEmail')) || this.model.get('order').get('syncEmail') === '') && (OB.UTIL.isNullOrUndefined(this.model.get('order').get('syncLandline')) || this.model.get('order').get('syncLandline') === '')) {
        this.doShowPopup({
          popup: 'DWCRM_UI_CustomerPropertiesImpl',
          args: {
            receipt: this.model
          }
        });
        return;
      }
      this.doShowPopup({
        popup: 'DSIREF_ModalSearchRefunds',
        args: {
          model: this.model
        }
      });
    },
    init: function (model) {
      this.model = model;
    },
  });

  // Register the menu...
  OB.OBPOSPointOfSale.UI.LeftToolbarImpl.prototype.menuEntries.push({
    kind: 'DSIREF.UI.MenuRefunds'
  });
}());