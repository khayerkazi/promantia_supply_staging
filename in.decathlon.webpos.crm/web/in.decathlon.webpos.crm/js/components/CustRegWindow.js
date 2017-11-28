/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.

 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 *
 * Contributed by Promantia Global Consulting LLP
 *
 ************************************************************************************
 */
/*global enyo, Backbone, GCNV, $,DWCRM,console,data,result,custrCOptin */
OB.DWCRM = OB.DWCRM || {};
OB.DWCRM.CRMProperties = OB.DWCRM.CRMProperties || {};
OB.DWCRM.CRMCustomer = OB.DWCRM.CRMCustomer || {};
OB.DWCRM.CRMWindow = OB.DWCRM.CRMWindow || {};
var onlineStatusCounter = 0;
(function() {
  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'OB.UI.CustomerPropertiesDialogApply',
    isDefaultAction: true,
    buttonStates: {
      "Validate": 0,
      "Register": 1,
      "Go To Billing": 2
    },
    currentCustomer: null,
    events: {
      onApplyChanges: '',
      ShowPopup: ''
    },

    // function close
    initComponents: function() {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('DWCRM_CustValidate'));
    }
  });
  enyo.kind({
    kind: 'OB.UI.ModalDialogButton',
    name: 'OB.UI.CustomerPropertiesDialogCancel',
    initComponents: function() {
      this.inherited(arguments);
      this.setContent(OB.I18N.getLabel('DWCRM_CustClear'));
    }
  });
  /*
   * Main Window of CRM Search and Register
   */
  enyo.kind({
    name: 'DWCRM.UI.ModalCustomerPropertiesImpl',
    kind: 'OB.UI.ModalAction',
    i18nHeader: 'DWCRM_CustRegHeader',
    floating: true,
    autoDismiss: false,
    published: {
      focusblank: 'background-color: #779ECB;width:13.5%; position:absolute; left:40.5%; top:36%;height:06%;',
      focusMr: 'background-color: #779ECB;width:15%; position:absolute; left:53.5%; top:36%;height:06%;',
      focusMs: 'background-color: #779ECB;width:15%; position:absolute; left:68%; top:36%;height:06%;',
      focusMrs: 'background-color:#779ECB;width:15%; position:absolute; left:83%; top:36%; height:06%;'
    },
    handlers: {
      onkeypress: "inputChange",
      onShow: "refreshScreen",
      onApplyChanges: 'applyChanges',
      onclick: 'processButton',
      onfocus: "inputChange"

    },
    bodyContent: {
      kind: 'Scroller',
      maxHeight: '30%',
      style: 'background-color: #E8E8E8;',
      thumb: false,
      horizontal: 'hidden',
      vertical: 'hidden',
      components: [{
        name: 'attributes'
      }]
    },
    bodyButtons: {
      // todo: move this control to header
      components: [{
        kind: 'enyo.Control',
        tag: 'div',
        name: 'optin',
        components: [{
          kind: "Image",
          name: "logo",
          src: "../../web/in.decathlon.webpos.crm/assets/img/Decathlon.png",
          style: 'width:20%; position:absolute; left:05%; top:01%;'
        }, {
          name: "validationErrorMsg",
          classes: "enyo-inline",
          style: 'color:#FF0000;'
        }, {
          kind: 'enyo.Control',
          name: "unSubscribe",
          classes: "enyo-inline",
          style: 'font-weight:bold'
        }, {
          kind: "onyx.Input",
          type: "checkbox",
          name: "smsOpt",
          disabled: true,
          classes: "enyo-inline",
          style: "width:2em"
        }, {
          kind: 'enyo.Control',
          content: " Email ",
          classes: "enyo-inline",
          style: 'font-weight:bold'
        }, {
          kind: "onyx.Input",
          type: "checkbox",
          name: "emailOpt",
          disabled: true,
          style: "width:2em"
        }]
      }, {
        name: "applyButton",
        kind: 'OB.UI.CustomerPropertiesDialogApply',
        disabled: false
      }, {
        kind: "onyx.Button",
        content: " ",
        name: 'blank',
        disabled: true,
        style: 'background-color: #FFFFFF;width:13.5%; position:absolute; left:40.5%; top:36%;height:06%;'
      }, {
        kind: "onyx.Button",
        content: "Mr",
        name: 'Mr',
        disabled: true,
        style: 'background-color: #FFFFFF;width:15%; position:absolute; left:53.5%; top:36%;height:06%;'
      }, {
        kind: "onyx.Button",
        content: "Ms",
        name: "Ms",
        disabled: true,
        style: 'background-color: #FFFFFF;width:15%; position:absolute; left:68%; top:36%;height:06%;'
      }, {
        kind: "onyx.Button",
        content: "Mrs",
        name: "Mrs",
        disabled: true,
        style: 'background-color: #FFFFFF;width:15%; position:absolute; left:83%; top:36%; height:06%;'
      }, {
        name: "cancelButton",
        kind: 'OB.UI.CustomerPropertiesDialogCancel'
      }, {
        name: 'disclaimerHeading',
        style: 'margin-top:10px;margin-bottom:8px;font-size:16px,font-weight:bold,left:05%; top:01%;'
      }, {
        name: 'disclaimer',
        style: 'margin-top:10px;margin-bottom:10px;font-size:13px'
      }]
    },
    newAttributes: [{
      kind: 'OB.UI.renderTextProperty',
      name: 'customerPhone',
      modelProperty: 'phone',
      readOnly: false,
      placeholder: "Enter 10 Digit Mobile No ...",
      i18nLabel: 'DWCRM_CustPhoneNo',
      style: 'background-color: #E8E8E8;',
      attributes: {
        maxlength: 10
      }
    }, {
      kind: 'OB.UI.renderTextProperty',
      name: 'customerEmail',
      modelProperty: 'email',
      readOnly: false,
      placeholder: "Enter the Email address ...",
      i18nLabel: 'DWCRM_CustEmail',
      style: 'background-color: #E8E8E8;'
    }, {
      kind: 'OB.UI.renderTextProperty',
      name: 'landLineNo',
      modelProperty: 'land',
      readOnly: false,
      placeholder: "Enter 10 Digit Landline No ...",
      i18nLabel: 'DWCRM_LandLine',
      style: 'background-color: #E8E8E8;',
      attributes: {
        maxlength: 10
      }
    }, {
      kind: 'OB.UI.renderTextProperty',
      name: 'decId',
      modelProperty: 'decid',
      readOnly: true,
      i18nLabel: 'DWCRM_DecId',
      style: 'background-color: #E8E8E8;'
    }, {
      kind: 'OB.UI.renderTextProperty',
      align: "left",
      name: 'civility',
      modelProperty: 'civility',
      i18nLabel: 'DWCRM_Civility',
      style: 'background-color: #E8E8E8;padding-right:100%;border : 1px solid #CCC ;width:1.5%'
    }, {
      kind: 'OB.UI.renderTextProperty',
      name: 'customerFirstName',
      modelProperty: 'firstname',
      i18nLabel: 'DWCRM_CustFirstName',
      readOnly: true,
      align: "left",
      style: 'background-color: #E8E8E8;'
    }, {
      kind: 'OB.UI.renderTextProperty',
      name: 'customerLastName',
      modelProperty: 'lastName',
      i18nLabel: 'DWCRM_CustLastName',
      readOnly: true,
      style: 'background-color: #E8E8E8;'
    }, {
      kind: 'OB.UI.renderTextProperty',
      name: 'customerZipCode',
      modelProperty: 'ZipCode',
      i18nLabel: 'DWCRM_CustZipCode',
      readOnly: true,
      attributes: {
        maxlength: 6
      },
      style: 'background-color: #E8E8E8;'
    }],
    init: function(model) {
      OB.DWCRM.CRMWindow = this;
      this.$.header.applyStyle('font-size', '30px');
      this.$.header.applyStyle('margin-left', '140px');
      this.$.headerCloseButton.show(); // used to show the close button
      var json;
      var server = new OB.DS.Process('in.decathlon.webpos.crm.service.getproperties.PropertyFileWebservice');
      server.exec(null, function(data) {
        if (data && data.exception) {
          OB.UTIL.showError(OB.I18N.getLabel('OBPOS_MsgFinishCloseError'));
        } else {
          OB.DWCRM.CRMProperties = data;
        }
      });
      console.log('first executed');
    },
    applyChanges: function(inSender, inEvent) {
      this.waterfall('onApplyChange', {});
      return true;
    },
    initComponents: function() {
      OB.DWCRM.CustRegInstance = this;
      OB.DWCRM.CustRegInstance.CustOxyId = 'T';
      OB.DWCRM.CustRegInstance.CustId = 0;
      OB.DWCRM.CustRegInstance.orderObj = null;
      this.inherited(arguments);
      this.attributeContainer = this.$.bodyContent.$.attributes;
      enyo.forEach(this.newAttributes, function(natt) {
        this.$.bodyContent.$.attributes.createComponent({
          kind: 'OB.UI.PropertyEditLine',
          name: 'line_' + natt.name,
          newAttribute: natt
        });
      }, this);
      this.$.bodyButtons.$.disclaimer.setContent(OB.I18N.getLabel('DWCRM_CustDesclaimer'));
      this.$.bodyButtons.$.disclaimerHeading.setContent('Disclaimer');
      this.$.bodyButtons.$.unSubscribe.setContent(OB.I18N.getLabel('DWCRM_CustunSubscribe'));
    },
    setField: function(control, value) {
      control.controls[1].controls[0].controls[0].node.value = value;
    },
    loadValues: function(customer) {
      var line = this.$.bodyContent.$.attributes.$;
      var civility = customer.attributes.civility;
      var fName = customer.attributes.name.replace(customer.attributes.civility.concat('.'), '');
      //to to concatination of civility if requires
      //var fName=civility+'.'.concat(fName);
      var lName = customer.attributes.name2;
      var mobileNo = customer.attributes.rCMobile;
      var email = customer.attributes.rCEmail;
      var land = customer.attributes.rCLand;
      var zipCode = customer.attributes.rcZipcode;
      if (customer.attributes.rCDecid === '' || customer.attributes.rCDecid === null) {
        var decId = DWCRM.crmCustomerMob.order.attributes.rCOxylaneno;
      } else {
        decId = customer.attributes.rCDecid;
      }
      if (customer.attributes.rCSms === 'Y' || customer.attributes.rCSms === 'N' || customer.attributes.rCNotify === 'Y' || customer.attributes.rCNotify === 'N') {
        this.$.bodyButtons.$.smsOpt.node.checked = OB.DWCRM.CRMWindow.getSmsEmail(customer.attributes.rCSms);
        this.$.bodyButtons.$.emailOpt.node.checked = OB.DWCRM.CRMWindow.getSmsEmail(customer.attributes.rCNotify);
      } else {
        this.$.bodyButtons.$.smsOpt.node.checked = customer.attributes.rCSms;
        this.$.bodyButtons.$.emailOpt.node.checked = customer.attributes.rCNotify;
      }
      if (civility === "") {
        this.$.bodyButtons.$.blank.setStyle(this.focusblank);
      }
      if (civility === "Mr") {
        this.$.bodyButtons.$.Mr.setStyle(this.focusMr);
      }
      if (civility === "Mrs") {
        this.$.bodyButtons.$.Mrs.setStyle(this.focusMrs);
      }
      if (civility === "Ms") {
        this.$.bodyButtons.$.Ms.setStyle(this.focusMs);
      }
      this.setField(line.line_customerFirstName, fName);
      this.setField(line.line_customerLastName, lName);
      this.setField(line.line_customerPhone, mobileNo);
      this.setField(line.line_customerEmail, email);
      this.setField(line.line_landLineNo, land);
      this.setField(line.line_customerZipCode, zipCode);
      this.setField(line.line_decId, decId);
      this.$.bodyButtons.$.validationErrorMsg.setContent('');
      //      OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustGoToBilling'));
    },
    refreshScreen: function(inSender, inEvent) {
      //this.setDefaultBgColour(line.line_customerPhone, roStyle);
      if (inEvent.originator.name === 'picker') {
        return true;
      }
      if (typeof(this.args.customer) !== 'undefined' && Object.keys(this.args.customer).length > 0) {
        this.makeFormEditable();
        this.loadValues(this.args.customer);
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.blank.node.disabled = true;
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mr.node.disabled = true;
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mrs.node.disabled = true;
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.Ms.node.disabled = true;
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustGoToBilling'));
      } else {
        this.$.bodyButtons.$.validationErrorMsg.setContent('');
        this.clearFields();
        this.setDefaultBgForCivility();
      }
    },
    inputChange: function(inSender, inEvent) {
      // retrieve new input value
      var inputField = inEvent.originator.name;
      var input = inEvent.originator.node.value;
      var line = this.$.bodyContent.$.attributes.$;
      var roStyle = 'width: 100%;background-color: #E8E8E8;';
      var rwStyle = 'width: 100%;background-color: #FFFFF;';
      if (window.event.keyCode === 13 && this.$.bodyButtons.$.applyButton.content === 'Validate') {
        this.processButton(inSender, inEvent);
      }
      if ((inputField === 'customerFirstName') || (inEvent.originator.name === 'customerLastName')) {
        inEvent = (inEvent) ? inEvent : window.event;
        var charCode = (inEvent.which) ? inEvent.which : inEvent.keyCode;
        var keycode;
        keycode = event.keyCode ? event.keyCode : event.which;
        // space or number or captial alphabets or small alphabets
        if (((charCode >= 48 && charCode <= 57) || (charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122)) && ((charCode > 31 && (charCode < 48 || charCode > 57)))) {
          inEvent.returnValue = true;
        } else {
          inEvent.returnValue = false;
        }
      }
      if ((inputField === 'landLineNo') || (inputField === 'customerPhone') || (inputField === "customerZipCode")) {
        inEvent = (inEvent) ? inEvent : window.event;
        var charCode = (inEvent.which) ? inEvent.which : inEvent.keyCode;
        if (charCode < 48 || charCode > 57) {
          inEvent.returnValue = false;
        }
      }
      if (inEvent.originator.name === 'landLineNo') {
        if (input === '' && inEvent.keyCode === 48) {
          inEvent.returnValue = false;
        }
        if (this.$.bodyButtons.$.applyButton.content === 'Validate') {
          if (this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.value !== "" || this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.value !== "") {
            this.setReadOnly(line.line_landLineNo);
          } else {
            this.setDefaultBgColour(line.line_customerPhone, roStyle);
            this.setDefaultBgColour(line.line_customerEmail, roStyle);
            this.setDefaultBgColour(line.line_landLineNo, rwStyle);
            this.setEditable(line.line_landLineNo);
            this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
            this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
          }
        }
      }
      if (inEvent.originator.name === 'customerPhone') {
        // first char should not be anything but 7 8 and 9
        if (input === '' && inEvent.keyCode >= 48 && inEvent.keyCode <= 54) {
          inEvent.returnValue = false;
        }
        if (this.$.bodyButtons.$.applyButton.content === 'Validate') {
          if (this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.value !== "" || this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.value !== "") {
            this.setReadOnly(line.line_customerPhone);
          } else {
            this.setDefaultBgColour(line.line_customerPhone, rwStyle);
            this.setDefaultBgColour(line.line_customerEmail, roStyle);
            this.setDefaultBgColour(line.line_landLineNo, roStyle);
            this.setEditable(line.line_customerPhone);
            this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
            this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
          }
        }
      }
      if (inEvent.originator.name === 'customerEmail') {
        if (inEvent.keyCode === 32) {
          inEvent.returnValue = false;
        }
        if (this.$.bodyButtons.$.applyButton.content === 'Validate') {
          if (this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.value !== "" || this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.value !== "") {
            this.setReadOnly(line.line_customerEmail);
          } else {
            this.setDefaultBgColour(line.line_customerPhone, roStyle);
            this.setDefaultBgColour(line.line_customerEmail, rwStyle);
            this.setDefaultBgColour(line.line_landLineNo, roStyle);
            this.setEditable(line.line_customerEmail);
            this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
            this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
          }
        }
      }
    },
    getCurrentData: function(control) {
      return control.controls[1].controls[0].controls[0].node.value;
    },


    processButton: function(inSender, inEvent) {
      if (inEvent.originator.name === 'applyButton' || window.event.keyCode === 13) {
        var control = inSender.container.$.bodyContent.$.attributes.$;
        var no = this.getCurrentData(control.line_customerPhone);
        var email = this.getCurrentData(control.line_customerEmail);
        var landNo = this.getCurrentData(control.line_landLineNo);
        var zipCode = this.getCurrentData(control.line_customerZipCode);
        var buttonContent = inSender.container.$.bodyButtons.$.applyButton.content;
        var emailCheckBox = inSender.container.$.bodyButtons.$.emailOpt.node.checked;
        var smsCheckBox = inSender.container.$.bodyButtons.$.smsOpt.node.checked;
        var message = OB.DWCRM.CustRegInstance.validations(no, landNo, email, buttonContent, smsCheckBox, emailCheckBox, zipCode);
        inSender.container.$.bodyButtons.$.validationErrorMsg.setContent(message);
        if (message !== "") {
          return;
        }
        this.processCustomerFormData(no, email, landNo, buttonContent, inEvent);
      }

      if (inEvent.originator.name === 'cancelButton') {
        this.makeFormReadOnly();
        this.clearFields();
        var custRegOrder = DWCRM.crmCustomerMob.order;
        custRegOrder.set('syncEmail', '');
        custRegOrder.set('syncLandline', '');
        custRegOrder.set('rCMobileNo', '');
        custRegOrder.set('rCOxylaneno', '');
        var customer = {};
        OB.DWCRM.CRMCustomer = customer;
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.node.disabled = false;
      }
      if (inEvent.originator.name === 'blank') {
        this.setDefaultBgForCivility();
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.blank.setStyle(this.focusblank);
      }
      if (inEvent.originator.name === 'Mr') {
        this.setDefaultBgForCivility();
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mr.setStyle(this.focusMr);
      }
      if (inEvent.originator.name === 'Ms') {
        this.setDefaultBgForCivility();
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.Ms.setStyle(this.focusMs);
      }
      if (inEvent.originator.name === 'Mrs') {
        this.setDefaultBgForCivility();
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mrs.setStyle(this.focusMrs);
      }
    },
    processCustomerFormData: function(no, email, landNo, buttonContent, inEvent) {
      if (buttonContent === 'Go To Billing') {
        if (window.event.keyCode === 13) {
          return false;
        }


        if (OB.DWCRM.CRMWindow.isModified() != true) {
          OB.DWCRM.CustRegInstance.registerConfirmedCostomer();
          return;
        } else if (!(this.disabled)) {
          inEvent.originator.doShowPopup({
            popup: 'DWCRM_UI_ConfirmationToContinue',
            args: {
              customerWindow: OB.DWCRM.CRMWindow
            }
          });
        }


      } else if (buttonContent === 'Register') {
        if (window.event.keyCode === 13) {
          return false;
        }

        inEvent.originator.doShowPopup({
          popup: 'DWCRM_UI_ConfirmationToContinue',
          args: {
            customerWindow: OB.DWCRM.CRMWindow
          }
        });

      } else if (buttonContent == 'Validate') {
        var condition = navigator.onLine ? "online" : "offline";
        if (condition === "offline" && onlineStatusCounter === 0) {
          onlineStatusCounter++;
          inEvent.originator.doShowPopup({
            popup: 'DWCRM_UI_OfflineWindow',
            args: {
              customerWindow: OB.DWCRM.CRMWindow
            }
          });
        }
        OB.UTIL.CRMUtils.getCustomer(no, email, landNo,onlineStatusCounter);
        OB.DWCRM.CRMWindow.setDefaultBgForCivility();
      }

      if (condition === "online") {
        onlineStatusCounter = 0;
        OB.UTIL.CRMUtils.synchToCrm();
      }



    },
    clearFields: function() {
      // clear the content of all fields
      var roStyle = 'width: 100%;background-color: #E8E8E8;';
      var line = this.$.bodyContent.$.attributes.$;
      this.setField(line.line_customerFirstName, "");
      this.setField(line.line_customerLastName, "");
      this.setField(line.line_customerPhone, "");
      this.setField(line.line_landLineNo, "");
      this.setField(line.line_decId, "");
      this.setField(line.line_customerEmail, "");
      this.setField(line.line_customerZipCode, "");
      //line.line_civility.$.newAttribute.$.blank.setActive(true);
      this.setDefaultBgColour(line.line_customerPhone, roStyle);
      this.setDefaultBgColour(line.line_customerEmail, roStyle);
      this.setDefaultBgColour(line.line_landLineNo, roStyle);
      this.setDefaultBgColour(line.line_customerFirstName, roStyle);
      this.setDefaultBgColour(line.line_customerLastName, roStyle);
      this.setDefaultBgColour(line.line_decId, roStyle);
      this.setDefaultBgColour(line.line_customerZipCode, roStyle);
      this.$.bodyButtons.$.smsOpt.node.checked = false;
      this.$.bodyButtons.$.emailOpt.node.checked = false;
      this.$.bodyButtons.$.validationErrorMsg.setContent('');
      this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
      this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
      this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.attributes[0].value = roStyle;
      this.setDefaultBgForCivility();
      this.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustValidate'));
    },
    setDefaultBgForCivility: function() {
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.blank.setStyle('background-color: #FFFFFF;width:13.5%; position:absolute; left:40.5%; top:36%;height:06%;');
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mr.setStyle('background-color: #FFFFFF;width:15%; position:absolute; left:53.5%; top:36%;height:06%;');
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.Ms.setStyle('background-color: #FFFFFF;width:15%; position:absolute; left:68%; top:36%;height:06%;');
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mrs.setStyle('background-color:#FFFFFF;width:15%; position:absolute; left:83%; top:36%; height:06%;');
    },
    setChangedBgForCivility: function() {
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.blank.setStyle('background-color: #FFFFFF;width:13.5%; position:absolute; left:40.5%; top:35%;height:06%;');
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mr.setStyle('background-color: #FFFFFF;width:15%; position:absolute; left:53.5%; top:35%;height:06%;');
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.Ms.setStyle('background-color: #FFFFFF;width:15%; position:absolute; left:68%; top:35%;height:06%;');
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.Mrs.setStyle('background-color:#FFFFFF;width:15%; position:absolute; left:83%; top:35%; height:06%;');
    },
    setDefaultBgColour: function(control, value) {
      control.controls[1].controls[0].controls[0].setStyle(value);
    },
    setReadOnly: function(control) {
      control.controls[1].controls[0].controls[0].node.readOnly = true;
    },
    setEditable: function(control) {
      control.controls[1].controls[0].controls[0].node.readOnly = false;
    },
    makeFormReadOnly: function() {
      //todo: changing Background while GO-To-Billing
      // make all the fields readOnly
      var line = this.$.bodyContent.$.attributes.$;
      var roStyle = 'width: 100%;background-color: #E8E8E8;';
      this.setReadOnly(line.line_customerFirstName);
      this.setReadOnly(line.line_customerLastName);
      this.setReadOnly(line.line_decId);
      this.setReadOnly(line.line_customerZipCode);
      this.setDefaultBgColour(line.line_customerPhone, roStyle);
      this.setDefaultBgColour(line.line_customerEmail, roStyle);
      this.setDefaultBgColour(line.line_landLineNo, roStyle);
      this.setDefaultBgColour(line.line_customerFirstName, roStyle);
      this.setDefaultBgColour(line.line_customerLastName, roStyle);
      this.setDefaultBgColour(line.line_decId, roStyle);
      this.setDefaultBgColour(line.line_customerZipCode, roStyle);
      this.$.bodyButtons.$.smsOpt.node.disabled = true;
      this.$.bodyButtons.$.emailOpt.node.disabled = true;
      this.$.bodyButtons.$.blank.node.disabled = true;
      this.$.bodyButtons.$.Mrs.node.disabled = true;
      this.$.bodyButtons.$.Ms.node.disabled = true;
      this.$.bodyButtons.$.Mr.node.disabled = true;
    },
    makeFormEditable: function() {
      var line = this.$.bodyContent.$.attributes.$;
      var rwStyle = 'width: 100%;background-color: #FFFFF;';
      this.setEditable(line.line_customerFirstName);
      this.setEditable(line.line_customerLastName);
      this.setEditable(line.line_customerZipCode);
      this.setEditable(line.line_customerPhone);
      this.setEditable(line.line_customerEmail);
      this.setEditable(line.line_landLineNo);
      this.setDefaultBgColour(line.line_customerPhone, rwStyle);
      this.setDefaultBgColour(line.line_customerEmail, rwStyle);
      this.setDefaultBgColour(line.line_landLineNo, rwStyle);
      this.setDefaultBgColour(line.line_customerFirstName, rwStyle);
      this.setDefaultBgColour(line.line_customerLastName, rwStyle);
      this.setDefaultBgColour(line.line_customerZipCode, rwStyle);
      this.$.bodyButtons.$.smsOpt.node.disabled = false;
      this.$.bodyButtons.$.emailOpt.node.disabled = false;
      this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = rwStyle;
      this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = rwStyle;
      this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.attributes[0].value = rwStyle;
      this.$.bodyButtons.$.blank.node.disabled = false;
      this.$.bodyButtons.$.Mrs.node.disabled = false;
      this.$.bodyButtons.$.Ms.node.disabled = false;
      this.$.bodyButtons.$.Mr.node.disabled = false;
    },
    clearErrorMsgs: function() {
      this.$.bodyButtons.$.validationErrorMsg.setContent('');
    },
    setDefaultCivilityControl: function() {
      this.$.bodyButtons.$.blank.node.disabled = true;
      this.$.bodyButtons.$.Mr.node.disabled = true;
      this.$.bodyButtons.$.Mrs.node.disabled = true;
      this.$.bodyButtons.$.Ms.node.disabled = true;
    },
    validations: function(mobileNo, landLineNo, email, windowType, smsCheckBox, emailCheckBox, zipcode) {
      var returnString = "";
      var rrStyle = 'width: 100%;background-color: #ffe5e5;';
        if (mobileNo === "" && landLineNo === "" && email === "") {
          this.$.bodyButtons.$.smsOpt.node.disabled = true;
          this.$.bodyButtons.$.emailOpt.node.disabled = true;
          this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
          this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
          this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
          this.setChangedBgForCivility();
          return "Provide one of the identifiers"; // OB.I18N.getLabel('DWCRM_allSearchCriterablank')
        }
      if (windowType !== 'Validate') {
        if (smsCheckBox != "" && mobileNo === "") {
          this.setChangedBgForCivility();
          return "Please Provide the Mobile Number";
          this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
        }
        if (emailCheckBox != "" && email === "") {
          this.setChangedBgForCivility();
          return "Please Provide the Email Address";
          this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
        }
        if (zipcode !== "") {
          if (zipcode.length !== 6) {
            this.setChangedBgForCivility();
            return "Zip code should be 6 digits.";
          }
        }
      }
      if (mobileNo !== "" && mobileNo.length !== 10) {
        this.setChangedBgForCivility();
        returnString = returnString + OB.I18N.getLabel('DWCRM_custPhoneErrorMsg') + " ";
        this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
      }
      // validation
      if ((mobileNo !== "") && ((mobileNo.charAt(0) < "7") || (mobileNo === "7777777777") || (mobileNo === "8888888888") || (mobileNo === "9999999999"))) {
        //if (mobileNo.charAt(0) < "7" && mobileNo != "") {
        this.setChangedBgForCivility();
        return "Please Provide the Valid Mobile Number";
        this.$.bodyContent.$.attributes.$.line_customerPhone.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
      }
      if (landLineNo !== "" && landLineNo.length !== 10) {
        this.setChangedBgForCivility();
        returnString = returnString + OB.I18N.getLabel('DWCRM_custLandLineErrorMsg') + " ";
        this.$.bodyContent.$.attributes.$.line_landLineNo.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
      }
      if (email !== "") {
        // email should have one @ symbol and one "." after @ and
        // two char after .
        var atpos = email.indexOf("@");
        var latpos = email.lastIndexOf("@");
        var ldotpos = email.lastIndexOf(".");
        var subStringAfterat = email.substring(email.indexOf("@"), email.length - 1);
        var StringLengthAfteratanddot = subStringAfterat.substring(1, subStringAfterat.indexOf(".")).length;
        // no @ char or first @ not the same as last @ or no . char
        // or | last dot char after @ char | length < last . +
        // 2,atlease 1 char after first @ & first dot
        if (StringLengthAfteratanddot === 0 || atpos === -1 || atpos !== latpos || ldotpos === -1 || ldotpos < atpos | email.length <= ldotpos + 2) {
          this.setChangedBgForCivility();
          returnString = returnString + OB.I18N.getLabel('DWCRM_custEmailErrorMsg') + "";
          this.$.bodyContent.$.attributes.$.line_customerEmail.controls[1].controls[0].controls[0].node.attributes[0].value = rrStyle;
        }
      }
      return returnString;
    },
    getValues: function(line) {
      return line.controls[1].controls[0].controls[0].node.value;
    },
    getSmsEmail: function(value) {
      if (value === 'Y') {
        return true;
      } else {
        return false;
      }
    },
    getCivility: function(civility) {
      if (civility.blank.style === this.focusblank) {
        return '';
      }
      if (civility.Mr.style === this.focusMr) {
        return 'Mr';
      }
      if (civility.Ms.style === this.focusMs) {
        return 'Ms';
      }
      if (civility.Mrs.style === this.focusMrs) {
        return 'Mrs';
      } else {
        return '';
      }
    },
    getDecId: function(line) {
      var phone = this.getValues(line.line_customerPhone);
      var email = this.getValues(line.line_customerEmail);
      var land = this.getValues(line.line_landLineNo);
      if ((phone !== this.currentCustomer.attributes.rCMobile) || (email !== this.currentCustomer.attributes.rCEmail) || (land !== this.currentCustomer.attributes.rCLand)) {
        return true;
      } else {
        return false;
      }

    },
    isModified: function() {
      if (typeof(this.currentCustomer) === 'undefined' && Object.keys(this.args.customer).length > 0) {
        return true;
      }
      var line = this.$.bodyContent.$.attributes.$;
      var civilityControl = this.$.bodyButtons.$;
      //if (firstName != this.currentCustomer.firstName )
      var phone = this.getValues(line.line_customerPhone);
      var email = this.getValues(line.line_customerEmail);
      var land = this.getValues(line.line_landLineNo);
      var decId = this.getValues(line.line_decId);
      var civility = this.getCivility(civilityControl);
      var firstName = this.getValues(line.line_customerFirstName);
      var compareFname = this.currentCustomer.attributes.name.replace(civility.concat('.'), '');
      var lastName = this.getValues(line.line_customerLastName);
      var zipCode = this.getValues(line.line_customerZipCode);
      var smsOpt = this.$.bodyButtons.$.smsOpt.node.checked ? 'Y' : 'N';
      var emailOpt = this.$.bodyButtons.$.emailOpt.node.checked ? 'Y' : 'N';
      //to do
      if ((phone !== this.currentCustomer.attributes.rCMobile) || (email !== this.currentCustomer.attributes.rCEmail) || (land !== this.currentCustomer.attributes.rCLand) || (decId !== this.currentCustomer.attributes.rCDecid) || (civility !== this.currentCustomer.attributes.civility) || (firstName !== compareFname) || (lastName !== this.currentCustomer.attributes.name2) || (zipCode !== this.currentCustomer.attributes.rcZipcode) || (smsOpt !== this.currentCustomer.attributes.rCSms) || (emailOpt !== this.currentCustomer.attributes.rCNotify)) {
        return true;
      }
    },
    registerConfirmedCostomer: function() {
      var customerWindow = OB.DWCRM.CRMWindow;
      var butcontent = this.$.bodyButtons.$.applyButton.content;
      var line = this.$.bodyContent.$.attributes.$;
      var civilityControl = this.$.bodyButtons.$;
      //var civility = this.getCivility(line);
      var civility = this.getCivility(civilityControl);
      // line.line_civility.controls[1].controls[0].controls[0].node.value;
      var email = line.line_customerEmail.controls[1].controls[0].controls[0].node.value;
      // line.line_customerEmail.controls[1].controls[0].controls[0].node.value;
      var firstName = line.line_customerFirstName.controls[1].controls[0].controls[0].node.value;
      var lastName = line.line_customerLastName.controls[1].controls[0].controls[0].node.value;
      var phone = line.line_customerPhone.controls[1].controls[0].controls[0].node.value;
      var zipcode = line.line_customerZipCode.controls[1].controls[0].controls[0].node.value;
      var decId = line.line_decId.controls[1].controls[0].controls[0].node.value;
      if (butcontent !== "Register") {
        if (customerWindow.getDecId(line)) {
          decId = null;
        } else {
          decId = line.line_decId.controls[1].controls[0].controls[0].node.value;

        }
      }
      var land = line.line_landLineNo.controls[1].controls[0].controls[0].node.value;
      var smsOpt = this.$.bodyButtons.$.smsOpt.node.checked;
      var emailOpt = this.$.bodyButtons.$.emailOpt.node.checked;
      // customerWindow.$.bodyParent.container.$.bodyButtons.$.valueOf('sms-opt')
      // this.$.bodyButtons.$.smsOpt.node.checked
      var buttonContent = customerWindow.$.bodyButtons.$.applyButton.content;
      if (buttonContent === 'Go To Billing') {
        var custRegOrder = DWCRM.crmCustomerMob.order;
        custRegOrder.set('syncEmail', email);
        custRegOrder.set('syncLandline', land);
        custRegOrder.set('rCMobileNo', phone);
        custId = OB.DWCRM.CustRegInstance.CustId;
        OB.DWCRM.CustRegInstance.CustOxyId = null;
        OB.DWCRM.CustRegInstance.CustId = null;
/*if (land != "") DWCRM.crmCustomerMob.renderData(custRegOrder.attributes.documentNo, land);
        if (email != "") DWCRM.crmCustomerMob.renderData(custRegOrder.attributes.documentNo, email);
        if (phone != "") DWCRM.crmCustomerMob.renderData(custRegOrder.attributes.documentNo, phone);
       */
        customerWindow.reRegister(firstName, lastName, phone, land, email, decId, civility, zipcode, emailOpt, smsOpt, custId);
        customerWindow.clearFields();
        customerWindow.makeFormReadOnly();
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.node.disabled = false;
        OB.DWCRM.CRMWindow.hide();
      } else {
        //customerWindow.registerCustomer(firstName, lastName, phone, land, email, decId, civility, zipcode, emailOpt, smsOpt, '');
        var custRegOrder = DWCRM.crmCustomerMob.order;
        OB.DWCRM.CustRegInstance.CustOxyId = OB.DWCRM.CustRegInstance.CustOxyId + OB.POS.modelterminal.get('terminal').searchKey;
        custRegOrder.set('syncEmail', email);
        custRegOrder.set('syncLandline', land);
        custRegOrder.set('rCMobileNo', phone);
        custRegOrder.set('rCOxylaneno', decId);
        var customer = new window.OB.Model.CRMCustomer({});
        customer.load(phone, email, land, civility, firstName, lastName, decId, zipcode, smsOpt, emailOpt);
        OB.DWCRM.CRMCustomer = customer;
        customer.save();
/*   if (land != "") DWCRM.crmCustomerMob.renderData(custRegOrder.attributes.documentNo, land);
        if (email != "") DWCRM.crmCustomerMob.renderData(custRegOrder.attributes.documentNo, email);
        if (phone != "") DWCRM.crmCustomerMob.renderData(custRegOrder.attributes.documentNo, phone);*/
        customerWindow.clearFields();
        customerWindow.makeFormReadOnly();
        OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.node.disabled = false;
        OB.DWCRM.CRMWindow.hide();
      }
    },
    // Resave the customer on click of Go To Billing
    reRegister: function(firstName, lastName, phone, land, email, decid, civility, zipcode, emailCheckBox, smsCheckBox, Custid) {
      this.registerCustomer(firstName, lastName, phone, land, email, decid, civility, zipcode, emailCheckBox, smsCheckBox, Custid);
    },
    // Save the unregistered customer
    registerCustomer: function(firstName, lastName, mobileNo, landNo, email, decid, civility, zipcode, emailCheckBox, smsCheckBox, Custid) {
      custClientId = OB.POS.modelterminal.get('terminal').client;
      custOrganizationId = OB.POS.modelterminal.get('terminal').organization;
      custPartnerCategoryId = OB.POS.modelterminal.get('terminal').defaultbp_bpcategory;
      //ORG search key saved as store on customer record
      storeSearchKey = OB.POS.modelterminal.get('terminal').organization$_identifier;
      custUserId = OB.POS.modelterminal.usermodel.id; //updatedBy
      console.log('registering ' + civility + ' ' + firstName + ' ' + lastName + ' ' + mobileNo + ' ' + landNo + ' ' + decid + ' ' + email + ' ' + zipcode + ' ' + emailCheckBox + ' ' + smsCheckBox);
      custrCOptin = false;
      var buttonContent = "";
      var message = OB.DWCRM.CustRegInstance.validations(mobileNo, landNo, email, buttonContent, smsCheckBox, emailCheckBox, zipcode);
      if (message !== "") {
        this.$.bodyButtons.$.validationErrorMsg.setContent(message);
      }
      var emailCheckStatus;
      var smsCheckStatus;
      if (emailCheckBox) {
        emailCheckStatus = 'Y';
      } else {
        emailCheckStatus = "N";
      }
      if (smsCheckBox) {
        smsCheckStatus = 'Y';
      } else {
        smsCheckStatus = "N";
      }
      if (emailCheckBox || smsCheckBox) {
        custrCOptin = true;
      }
      result = {
        civility: civility,
        name: firstName,
        name2: lastName,
        rCMobile: mobileNo,
        rCLand: landNo,
        rCEmail: email,
        rCDecid: decid,
        rCOptin: custrCOptin,
        rCNotify: emailCheckBox,
        rcZipcode: zipcode,
        rCSms: smsCheckBox,
        customer: true,
        _identifier: mobileNo,
        _entityName: 'BusinessPartner',
        client: custClientId,
        organization: custOrganizationId,
        active: true,
        store: storeSearchKey,
        updatedBy: custUserId,
        profile_created_by: custUserId
      };
      if (Custid !== '') {
        result.id = Custid;
      }
      data = {
        data: result
      };
      console.log('after result');
      console.log(OB.context);
      // for offlinesynch object saving 29jan
      //var customer = new window.OB.Model.CRMCustomer(data.data);
      if (OB.DWCRM.CRMWindow.isModified() === true) {
        var customer = new window.OB.Model.CRMCustomer({});
        customer.load(mobileNo, email, landNo, civility, firstName, lastName, decid, zipcode, smsCheckBox, emailCheckBox);
        OB.DWCRM.CRMCustomer = customer;
        customer.save();
        return;
      }

      var custRegOrder = DWCRM.crmCustomerMob.order;
      custRegOrder.set('syncEmail', data.data.rCEmail);
      custRegOrder.set('syncLandline', data.data.rCLand);
      custRegOrder.set('rCMobileNo', data.data.rCMobile);
      custRegOrder.set('rCOxylaneno', data.data.rCDecid);
      custRegOrder.save();
      this.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustGoToBilling'));

    }
  });
  // Register customer with receipt properties...
  OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', {
    kind: 'DWCRM.UI.ModalCustomerPropertiesImpl',
    name: 'DWCRM_UI_CustomerPropertiesImpl'
  });


}());
