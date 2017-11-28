/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo, Backbone, _*/

OB = OB || {};
OB.CHNO = OB.CHNO || {};

enyo.kind({
  name: 'CHNO.ChasisNumber.GetChasisPopup_body',
  classes: 'row-fluid',
  components: [{
    classes: 'span12',
    components: [{
      content: 'Chasis Number'
    }, {
      kind: 'enyo.TextArea',
      name: 'txtAreaChasisNumber'
    }]
  }]
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'CHNO.ChasisNumber.GetChasisPopup_OkButton',
  events: {
    onPutChasisNumber: ''
  },
  i18nContent: 'OBMOBC_LblOk',
  isDefaultAction: true,
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doPutChasisNumber();
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'CHNO.ChasisNumber.GetChasisPopup_CancelButton',
  i18nContent: 'OBMOBC_LblCancel',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doHideThisPopup();
  }
});

enyo.kind({
  name: 'CHNO.ChasisNumber.GetChasisPopup',
  kind: 'OB.UI.ModalAction',
  autoDismiss: false,
  classes: 'standardActionPopup_body actionPopup_body',
  handlers: {
    onPutChasisNumber: 'putChasisNumber'
  },
  events: {
    onHideThisPopup: ''
  },
  putChasisNumber: function (inSender, inEvent) {
    var chasisNo = this.$.bodyContent.$.getChasisPopup_body.$.txtAreaChasisNumber.getValue();
    if (chasisNo && chasisNo.length > 0) {
      this.chasisNo = chasisNo;
      this.doHideThisPopup();
    } else {
      this.chasisNo = null;
    }
  },
  executeOnShow: function () {
    this.$.header.setContent(this.args._identifier);
    this.$.bodyContent.$.getChasisPopup_body.$.txtAreaChasisNumber.setValue("");
    this.chasisNo = null;
  },
  executeBeforeHide: function () {
    if (!OB.UTIL.isNullOrUndefined(this.chasisNo) && this.chasisNo.length > 0) {
      this.args.product.set('clChasisnumber', this.chasisNo);
      this.args.callback(true);
    } else {
      this.args.callback(false);
    }
    return true;
  },
  topPosition: '125px',
  i18nHeader: 'CHNO_ChasisNumberPopup_Header',
  bodyContent: {
    kind: 'CHNO.ChasisNumber.GetChasisPopup_body'
  },
  bodyButtons: {
    components: [{
      kind: 'CHNO.ChasisNumber.GetChasisPopup_OkButton'
    }, {
      kind: 'CHNO.ChasisNumber.GetChasisPopup_CancelButton'
    }]
  },
  init: function (model) {
    this.model = model;
  }
});

OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
  kind: 'CHNO.ChasisNumber.GetChasisPopup',
  name: 'CHNO.ChasisNumber.GetChasisPopup'
});