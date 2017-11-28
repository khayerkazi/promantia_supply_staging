/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 *
 * Contributed by Promantia Global Consulting LLP.
 ************************************************************************************
 */
/*global enyo, Backbone,DWCRM */
DWCRM.crmCustomerMob = DWCRM.crmCustomerMob || {};
OB.UI.OrderDetails.extend({
  renderAdditionalData: function(model) {
    var customerIdentifier;
    var docNo = model.get('documentNo');
    var phone = model.get('rCMobileNo');
    var email = model.get('syncEmail');
    var land = model.get('syncLandline');
    var date = model.get('orderDate');
    if ((typeof(phone) !== 'undefined') && phone !== '' && phone !== null) {
      this.setContent(OB.I18N.formatHour(date) + ' - ' + docNo + ' - ' + phone);
    } else if ((typeof(email) !== 'undefined') && email !== '' && email !== null) {
      this.setContent(OB.I18N.formatHour(date) + ' - ' + docNo + ' - ' + email);
    } else if ((typeof(land) !== 'undefined') && land !== '' && land !== null) {
      this.setContent(OB.I18N.formatHour(date) + ' - ' + docNo + ' - ' + land);
    } else if (date instanceof Date) {
      this.setContent(OB.I18N.formatHour(date) + ' - ' + docNo);
    } else {
      this.setContent(date + ' - ' + docNo);
    }
    DWCRM.crmCustomerMob = this;
  },
  renderData: function(documentNo) {
    this.renderAdditionalData(this.order);
  },
  orderChanged: function(oldValue) {
    this.order.on('change', function(container) {
      this.renderAdditionalData(this.order);
    }, this);
  }
});