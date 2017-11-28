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

/*global enyo, Backbone, GCNV, $ */

(function() {
    OB.DWCRM = OB.DWCRM || {};


    enyo.kind({
        kind: 'OB.UI.ModalAction',
        name: 'DWCRM.UI.FeedBackWindow',
        style: 'color: #0D8CF2; height: 98%;width: 95%; background-color: #D9D9D9 ;',
        showCloseButton: false,
        autoDismiss: false,
        closeOnEscKey: true,
        published: {
            baseStyle: "background-color: #D9D9D9;background-size: contain; height: 300px; width: 30%;padding: 7px 10px;margin: 7px;background-repeat:no-repeat; background-position:center;",
            verySatisfiedStyle: "background-image: url(../../web/in.decathlon.webpos.crm/assets/img/very_satisfied.png);",
            satisfiedStyle: "background-image: url(../../web/in.decathlon.webpos.crm/assets/img/satisfied.png);",
            notSatisfiedStyle: "background-image: url(../../web/in.decathlon.webpos.crm/assets/img/not_satisfied.png);",
            enlargedStyle: "background-color: #D9D9D9;background-size: contain; height: 330px; width: 30%;padding: 7px 10px;margin: 7px;background-repeat:no-repeat; background-position:center;",
            onWait: false

        },
        handlers: {
            onShow: "initialize",
            ontap: "processFeedback"

        },
        bodyContent: {
            components: [

            {
                kind: "enyo.RichText",
                name: "feedbackTitleEnglish",
                align: "center",
                style: "color: #0D8CF2;height: 40px; font-size:200%; font-weight:bold;"
            },

            {
                kind: "enyo.RichText",
                name: "feedbackTitleLocal",
                align: "center",
                style: "color: #0D8CF2;height: 40px;font-size:200%; font-weight:bold;"
            }, {
                kind: 'DWCRM.UI.FeedBackSmielyButton',
                name: 'verySatisfied'
            }, {
                kind: 'DWCRM.UI.FeedBackSmielyButton',
                name: 'satisfied'
            }, {
                kind: 'DWCRM.UI.FeedBackSmielyButton',
                name: 'notSatisfied'
            }, {
                kind: 'enyo.Control',
                tag: 'div',
                components: [{
                    name: 'verySatisfiedText',
                    classes: 'enyo-inline',
                    style: 'color: #0D8CF2;width:30%; background-color: #D9D9D9; '
                }, {
                    name: 'satisfiedText',
                    classes: 'enyo-inline',
                    style: 'color: #0D8CF2;width:30%; background-color: #D9D9D9; '
                }, {
                    name: 'notSatisfiedText',
                    classes: 'enyo-inline',
                    style: 'color: #0D8CF2;width:30%; background-color: #D9D9D9;'
                }, {
                    name: 'verySatisfiedTextLoc',
                    classes: 'enyo-inline',
                    style: 'color: #0D8CF2;width:30%; background-color: #D9D9D9;'
                }, {
                    name: 'satisfiedTextLoc',
                    classes: 'enyo-inline',
                    style: 'color: #0D8CF2;width:30%; background-color: #D9D9D9; '
                }, {
                    name: 'notSatisfiedTextLoc',
                    classes: 'enyo-inline',
                    style: 'color: #0D8CF2;width:30%;height:26px; background-color: #D9D9D9;'
                }]
            }, {
                kind: "Image",
                align: "right",
                src: "../../web/in.decathlon.webpos.crm/assets/img/Decathlon.png",
                style: 'width:300px;height:80px; position:absolute; left:37%; top:87%;'

            }]
        },
        bodyButtons: {
            components: []
        },
        initComponents: function() {
            this.inherited(arguments);

            this.$.headerCloseButton.hide();

        },
        processFeedback: function(inSender, inEvent) {
            var feedback = inEvent.originator.name;
            if ((inEvent.originator.kind === "DWCRM.UI.FeedBackSmielyButton") && (!this.onWait)) {
                if (feedback === 'verySatisfied') {
                    this.args.model.set('dSEMDsRatesatisfaction', '3');
                    inEvent.originator.setStyle(this.enlargedStyle.concat(this.verySatisfiedStyle));
                    this.$.bodyContent.$.satisfied.setStyle(this.baseStyle);
                    this.$.bodyContent.$.notSatisfied.setStyle(this.baseStyle);
                    this.$.bodyContent.$.satisfiedText.setContent('');
                    this.$.bodyContent.$.satisfiedTextLoc.setContent('');
                    this.$.bodyContent.$.notSatisfiedText.setContent('');
                    this.$.bodyContent.$.notSatisfiedTextLoc.setContent('');
                    this.onWait = true;
                    window.setTimeout(function(thisObj) {
                        thisObj.hide();
                        OB.MobileApp.model.hookManager.callbackExecutor(thisObj.args.args, thisObj.args.c);
                    }, 2000, this);
                    return false;

                } else if (feedback === 'satisfied') {
                    this.args.model.set('dSEMDsRatesatisfaction', '2');
                    inEvent.originator.setStyle(this.enlargedStyle.concat(this.satisfiedStyle));
                    this.$.bodyContent.$.verySatisfied.setStyle(this.baseStyle);
                    this.$.bodyContent.$.notSatisfied.setStyle(this.baseStyle);
                    this.$.bodyContent.$.verySatisfiedText.setContent('');
                    this.$.bodyContent.$.verySatisfiedTextLoc.setContent('');
                    this.$.bodyContent.$.notSatisfiedText.setContent('');
                    this.$.bodyContent.$.notSatisfiedTextLoc.setContent('');
                    this.onWait = true;
                    window.setTimeout(function(thisObj) {
                        thisObj.hide();
                        OB.MobileApp.model.hookManager.callbackExecutor(thisObj.args.args, thisObj.args.c);
                    }, 2000, this);
                    return false;
                } else {
                    this.args.model.set('dSEMDsRatesatisfaction', '1');
                    inEvent.originator.setStyle(this.enlargedStyle.concat(this.notSatisfiedStyle));
                    this.$.bodyContent.$.satisfied.setStyle(this.baseStyle);
                    this.$.bodyContent.$.verySatisfied.setStyle(this.baseStyle);
                    this.$.bodyContent.$.satisfiedText.setContent('');
                    this.$.bodyContent.$.satisfiedTextLoc.setContent('');
                    this.$.bodyContent.$.verySatisfiedText.setContent('');
                    this.$.bodyContent.$.verySatisfiedTextLoc.setContent('');
                    this.onWait = true;
                    window.setTimeout(function(thisObj) {
                        thisObj.hide();
                        OB.MobileApp.model.hookManager.callbackExecutor(thisObj.args.args, thisObj.args.c);
                    }, 2000, this);

                    return false;
                }
            }

        },
        initialize: function(inSender, inEvent) {
            this.resetStyles(inEvent.originator.$.bodyContent);

        },
        resetStyles: function(bodyContent) {
            bodyContent.$.verySatisfied.setStyle(this.baseStyle.concat(this.verySatisfiedStyle));
            bodyContent.$.satisfied.setStyle(this.baseStyle.concat(this.satisfiedStyle));
            bodyContent.$.notSatisfied.setStyle(this.baseStyle.concat(this.notSatisfiedStyle));
            bodyContent.$.feedbackTitleEnglish.setContent(OB.I18N.getLabel('DWCRM_FeedBackHeaderEng'));
            bodyContent.$.feedbackTitleLocal.setContent(OB.I18N.getLabel('DWCRM_FeedBackHeaderLocal'));
            bodyContent.$.verySatisfiedText.setContent(OB.I18N.getLabel('DWCRM_custVerySatisfied'));
            bodyContent.$.verySatisfiedTextLoc.setContent(OB.I18N.getLabel('DWCRM_custVerySatisfiedLoc'));
            bodyContent.$.satisfiedText.setContent(OB.I18N.getLabel('DWCRM_custSatisfied'));
            bodyContent.$.satisfiedTextLoc.setContent(OB.I18N.getLabel('DWCRM_custSatisfiedLoc'));
            bodyContent.$.notSatisfiedText.setContent(OB.I18N.getLabel('DWCRM_custNotSatisfied'));
            bodyContent.$.notSatisfiedTextLoc.setContent(OB.I18N.getLabel('DWCRM_custNotSatisfiedLoc'));
            this.onWait = false;
            OB.UTIL.showLoading(false);
            return false;

        }

    });

    enyo.kind({
        name: 'DWCRM.UI.FeedBackSmielyButton',
        kind: 'OB.UI.ModalDialogButton',
        isDefaultAction: true,

        events: {
            onApplyChanges: ''
        }
    });

    OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
        kind: 'DWCRM.UI.FeedBackWindow',
        name: 'DWCRM_UI_FeedBackWindow'
    });


}());


OB.MobileApp.model.hookManager.registerHook('OBPOS_PreOrderSave', function(args, c) {
    var params = {};
    params.model = args.context.receipt;
    params.args = args;
    params.c = c;
    OB.OBPOSPointOfSale.currentWindow.doShowPopup({
        popup: 'DWCRM_UI_FeedBackWindow',
        args: params
    });
});