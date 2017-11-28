/*global B,enyo,DWCRM */
DWCRM.crmCustomerMob = DWCRM.crmCustomerMob || {};
(function() {
  OB.DWCRM = OB.DWCRM || {};
  enyo.kind({
    kind: 'OB.UI.ModalAction',
    name: 'DWCRM_UI_ConfirmationToContinue',
    showCloseButton: true,
    autoDismiss: false,
    handlers: {
      ontap: "processModalAction"
      // oninput:"inputChange",
    },
    bodyContent: {
      i18nContent: 'DWCRM_MsgAlertChanges'
    },
    bodyButtons: {
      components: [{
        kind: 'DWCRM.UI.btnContinue'
      }, {
        kind: 'DWCRM.UI.btnCancel'
      }]
    },
    executeOnShown: function() {
      //we are always setting the z-index greater than the z-index of the customer registration window.
      this.applyStyle("z-index", this.args.customerWindow.findZIndex() + 100);
    },
    initComponents: function() {
      this.header = OB.I18N.getLabel('DWCRM_ConfirmToContinue');
      //window.console.warn('Inside the Control');
      this.inherited(arguments);
    },
    processModalAction: function(inEvent, inSender) {
      if (inSender.originator.name === "btnCancel") {

        this.hide();

      } else if (inSender.originator.name === "btnContinue") {
        var customerWindow = this.args.customerWindow;
        this.hide();
        customerWindow.registerConfirmedCostomer();




      }
    }
  });
  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'DWCRM.UI.btnContinue',
    i18nContent: 'DWCRM_LblYes'

  });
  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'DWCRM.UI.btnCancel',
    i18nContent: 'DWCRM_LblNo'
  });
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'DWCRM_UI_ConfirmationToContinue',
    name: 'DWCRM_UI_ConfirmationToContinue'
  });
}());