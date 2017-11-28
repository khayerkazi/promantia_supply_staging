/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.

 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B,_,moment,Backbone,localStorage, enyo, DWCRM, console, data, model, pendingCustomersMessage, orderSucessCallback  */
OB.UTIL = window.OB.UTIL || {};
OB.UTIL.CRMUtils = OB.UTIL.CRMUtils || {};
OB.UTIL.CRMUtils.getCustomer = function(mobile, email, landline, onlineStatusCounter) {
  var searchParams = "";
  if (landline !== "") {
    searchParams = searchParams + '&landline=' + landline;
  }
  if (mobile !== "") {
    searchParams = searchParams + '&mobileno=' + mobile;
  }
  if (email !== "") {
    searchParams = searchParams + '&email=' + email;
  }
  var ajaxRequest = new enyo.Ajax({
    url: OB.POS.hwserver.url.replace("printer", "httpproxy"),
    cacheBust: false,
    contentType: 'application/json;charset=utf-8',
    method: 'POST',
    handleAs: 'json',
    data: JSON.stringify({
      // Data to connect to the external service.
      url: OB.DWCRM.CRMProperties.geturl + '?username=' + OB.DWCRM.CRMProperties.username + '&pwd=' + OB.DWCRM.CRMProperties.password + searchParams,
      method: 'GET',
      contenttype: 'application/json;charset=utf-8',
      content: 'username=l&pwd=p' + searchParams
    }),
    success: function(inSender, inResponse) {
      if (inResponse.result !== 'success') {
        console.log(inResponse);
        onlineStatusCounter = 0;
        var array = JSON.parse(inResponse.content).response.data.length - 1;
        var returnData = JSON.parse(inResponse.content).response.data[array];
        console.log(returnData);
        console.log(JSON.parse(inResponse.content).response.data.length);
        if (JSON.parse(inResponse.content).response.data.length >= 1) {
          var customer = new window.OB.Model.CRMCustomer({});
          customer.load(returnData.rCMobile, returnData.rCEmail, returnData.rCLandline, returnData.civility, returnData.name, returnData.name2, returnData.rCOxylane, returnData.rCZipcode, returnData.rCSms, returnData.rCNotify);
          OB.DWCRM.CustRegInstance.currentCustomer = customer;
          var custRegOrder = DWCRM.crmCustomerMob.order;
          custRegOrder.set('syncEmail', returnData.rCEmail);
          custRegOrder.set('syncLandline', returnData.rCLandline);
          custRegOrder.set('rCMobileNo', returnData.rCMobile);
          OB.DWCRM.CustRegInstance.loadValues(customer);
          OB.DWCRM.CustRegInstance.makeFormEditable();
          OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustGoToBilling'));
        } else {
          OB.DWCRM.CustRegInstance.makeFormEditable();
          OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustRegister'));
        }
        OB.UTIL.showError(inResponse.result);
      }
      this.isActive = true;
    },
    fail: function(inSender, inResponse) {
      if (inSender === '500') {
        console.log('failed to request- status 500');
      }
      OB.DWCRM.CustRegInstance.makeFormEditable();
      onlineStatusCounter = onlineStatusCounter + 1;
      OB.UTIL.showWarning("Internet is offline, please continue with the billing");
      OB.DWCRM.CustRegInstance.$.bodyButtons.$.applyButton.setContent(OB.I18N.getLabel('DWCRM_CustRegister'));
      console.log('failed to request');
      OB.UTIL.showError(inResponse);
      this.isActive = true;
    }
  });
  // ajax function end
  ajaxRequest.go(ajaxRequest.data).response('success').error('fail');

};
OB.UTIL.CRMUtils.action = function() {
  OB.Dal.find(OB.Model.CRMCustomer, {}, function(customerNotRegistered) { // OB.Dal.find
    // success
    var successCallback, errorCallback, pendingOrdersMessage;
    if (!customerNotRegistered || customerNotRegistered.length === 0) {
      return;
    }
    customerNotRegistered.each(function(order) {});
    pendingCustomersMessage = OB.UTIL.showAlert.display(OB.I18N.getLabel('DWCRM_SendCustomerToERP'), OB.I18N.getLabel('OBPOS_Info'));
    successCallback = function() {
      pendingCustomersMessage.hide();
      if (orderSucessCallback) {
        orderSucessCallback(model);
        return;
      }
      OB.UTIL.showSuccess(OB.I18N.getLabel('DWCRM_SendCustomerSuccess'));
    };
    errorCallback = function() {
      pendingCustomersMessage.hide();
    };
    //setTimeout(processCustomers,3000, customerNotRegistered, successCallback, errorCallback );
    OB.UTIL.CRMUtils.processCustomers(customerNotRegistered, successCallback, errorCallback);
  });
};
OB.UTIL.CRMUtils.processCustomers = function(customers, successCallback, errorCallback) {
  console.log(customers);
  customers.each(function(customer) {
    data = JSON.parse(customer.attributes.json);
    var storeSearchKey = OB.POS.modelterminal.get('terminal').organization$_identifier;
    var custUserId = OB.POS.modelterminal.usermodel.id; //updatedBy
    // cusomerInJson.id='';
    //if (OB.MobileApp.model.get('connectedToERP')) {
    console.log(data);
    if (data.rCNotify) {
      var rCNotify = "Y";
    } else {
      var rCNotify = "N";
    }
    if (data.rCSms) {
      var rCSms = "Y";
    } else {
      var rCSms = "N";
    }
    // console.log(OBContext);
    var ajaxRequest = new enyo.Ajax({
      url: OB.POS.hwserver.url.replace("printer", "httpproxy"),
      // TODO url from
      // config
      cacheBust: false,
      contentType: 'application/json;charset=utf-8',
      method: 'POST',
      handleAs: 'json',
      data: JSON.stringify({
        url: OB.DWCRM.CRMProperties.posturl + '?username=' + OB.DWCRM.CRMProperties.username + '&pwd=' + OB.DWCRM.CRMProperties.password + '&mobileno=' + data.rCMobile + '&name2=' + data.name2 + '&decathlonId=' + data.rCDecid + '&civility=' + data.civility + '&email=' + data.rCEmail + '&landline=' + data.rCLand + '&zipcode=' + data.rcZipcode + '&name=' + data.name + '&em_email_alert=' + rCNotify + '&em_sms_alert=' + rCSms + '&store=' + storeSearchKey + '&updatedBy=' + custUserId + '&profile_created_by=' + custUserId,
        method: 'POST',
        contenttype: 'application/json;charset=utf-8',
        content: data
      }),
      success: function(inSender, inResponse) {
        if (inResponse.response = "success") {
          var jsonObject = JSON.parse(inSender.data);
          OB.Dal.remove(customer, null, function(tx, err) {
            OB.UTIL.showError(err);
            console.log(err);
          });
          console.log('Registered in ERP');
        }
      },
      fail: function(inSender, inResponse) {
        console.log('failed to request');
        OB.UTIL.showError(inResponse);
        //this.isActive = true;
        //window.location.reload();
      }
    }); // ajaxcall
    ajaxRequest.go(ajaxRequest.data).response('success').error('fail');
    //} // if connected erp
  }); // customereach
}; //process customers
OB.UTIL.CRMUtils.synchToCrm = function() {
  OB.UTIL.CRMUtils.action();

};