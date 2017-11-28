/*global enyo,DWCRM */
DWCRM.crmCustomerMob = DWCRM.crmCustomerMob || {};
(function() {
  OB.DWCRM = OB.DWCRM || {};
  enyo.kind({
    kind: 'OB.UI.ModalAction',
    name: 'DWCRM_UI_OfflineWindow',
    showCloseButton: true,
    autoDismiss: false,
    handlers: {
      ontap: "processModalAction"
      // oninput:"inputChange",
    },
    bodyContent: {
      i18nContent: 'DWCRM_OfflineMsg'
    },
    bodyButtons: {
      components: [{
        kind: 'DWCRM.UI.btnContinue'
      }]
    },
    executeOnShown: function() {
      //we are always setting the z-index greater than the z-index of the customer registration window.
      this.applyStyle("z-index", this.args.customerWindow.findZIndex() + 10);
      this.onWait = true;
      window.setTimeout(function(thisObj) {
        thisObj.hide();
      }, 2000, this);
    },
    initComponents: function() {
      this.header = OB.I18N.getLabel('DWCRM_Confirm');
      // window.console.warn('Inside the Control');
      this.inherited(arguments);
    },
    processModalAction: function(inEvent, inSender) {
      if (inSender.originator.name === "btnContinue") {
        this.hide();
      }
    }
  });
  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'DWCRM.UI.btnContinue',
    i18nContent: 'DWCRM_Ok'

  });
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'DWCRM_UI_OfflineWindow',
    name: 'DWCRM_UI_OfflineWindow'
  });
}());