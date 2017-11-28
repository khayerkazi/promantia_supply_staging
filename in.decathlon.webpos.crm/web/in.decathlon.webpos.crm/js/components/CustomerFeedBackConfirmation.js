/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 * 
 * 
 * Contributed by Promantia Global Consulting
 ************************************************************************************
 */
/*global B,enyo,DWCRM */


(function() {
    OB.DWCRM = OB.DWCRM || {};
    enyo.kind({
        kind: 'OB.UI.ModalAction',
        name: 'DWCRM_UI_FeedBack_Confirmation',
        showCloseButton: false,
        autoDismiss: false,
        style: 'color: #0D8CF2; height: 98%;width: 95%; background-color: #D9D9D9 ;',
        handlers: {
            ontap: "processModalAction"

        },
        bodyContent: {
            components: [{

                kind: "Image",
                name: "logo",
                src: "../../web/in.decathlon.webpos.crm/assets/img/not_satisfied.png",
                style: 'width:35%; position:absolute;height:55%; left:30%; top:15%;'
            }, {
                i18nContent: 'DWCRM_MsgAlertChanges'
            }]
        },
        bodyButtons: {
            components: [{
                kind: 'DWCRM.UI.btnConfirm',
                style: 'width:22%; position:absolute; left:15%; top:75%;'
            }, {
                kind: 'DWCRM.UI.btnBack',
                style: 'width:25%; position:absolute; left:55%; top:75%;'
            }]
        },
        executeOnShown: function() {
            //we are always setting the z-index greater than the z-index of the customer feedback window.
            this.applyStyle("z-index", this.args.customerFeedBackWindow.findZIndex() + 100);
        },
        initComponents: function() {
            this.inherited(arguments);
            this.header = OB.I18N.getLabel('DWCRM_ConfirmToContinue');

        },
        processModalAction: function(inEvent, inSender) {
            if (inSender.originator.name === "btnBack") {
                this.hide();
            } else if (inSender.originator.name === "btnConfirm") {
                this.hide();
                





            }
        }
    });
    enyo.kind({
        kind: 'OB.UI.ModalDialogButton',
        name: 'DWCRM.UI.btnConfirm',
        i18nContent: 'DWCRM_LblConfirm'

    });
    enyo.kind({
        kind: 'OB.UI.ModalDialogButton',
        name: 'DWCRM.UI.btnBack',
        i18nContent: 'DWCRM_LblBack'
    });
    OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
        kind: 'DWCRM_UI_FeedBack_Confirmation',
        name: 'DWCRM_UI_FeedBack_Confirmation'
    });
}());