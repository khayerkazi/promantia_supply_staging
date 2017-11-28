/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, _, $, GCNV */


(function () {
  DSIREF = DSIREF || {};
  DSIREF.global = DSIREF.global || {};

  DSIREF.global.listRefundsHeaderService = 'org.openbravo.decathlon.retail.refund.datasource.ListRefundHeaders';

  var creditNoteHeaderResponseForTest = [{
    "id": "998997F4FC6B4F37AA9A5326B8E55BE2",
    "creationDate": "2015-03-27 11:15:10.642887",
    "documentNo": "CRN-1008805",
    "grandTotalAmount": 1399,
    "returnReason": "Customer Exchange"
  }, {
    "id": "998997F4FC6B4F37AA9A5326B8E55BE2",
    "creationDate": "2015-03-27 11:15:10.642887",
    "documentNo": "CRN-1008806",
    "grandTotalAmount": 1658,
    "returnReason": "Customer Exchange"
  }, {
    "id": "998997F4FC6B4F37AA9A5326B8E55BE2",
    "creationDate": "2015-03-27 11:15:10.642887",
    "documentNo": "CRN-1008807",
    "grandTotalAmount": 1600,
    "returnReason": "Customer Exchange"
  }];

  var service = function (source, dataparams, callback, callbackError) {
      var ajaxRequest = new enyo.Ajax({
        url: '../../org.openbravo.retail.posterminal.service.jsonrest/' + source,
        cacheBust: false,
        method: 'POST',
        handleAs: 'json',
        contentType: 'application/json;charset=utf-8',
        data: JSON.stringify(dataparams),
        success: function (inSender, inResponse) {
          var response = inResponse.response;
          var status = response.status;
          if (status === 0) {
            callback(response.data);
          } else if (response.errors) {
            callbackError({
              exception: {
                message: response.errors.id
              }
            });
          } else {
            callbackError({
              exception: {
                message: response.error.message
              }
            });
          }
        },
        fail: function (inSender, inResponse) {
          callbackError({
            exception: {
              message: OB.I18N.getLabel('OBPOS_MsgApplicationServerNotAvailable'),
              status: inResponse
            }
          });
        }
      });
      if (!DSIREF.utils.inDevelopment) {
        ajaxRequest.go(ajaxRequest.data).response('success').error('fail');
      } else {
        setTimeout(function () {
          callback(creditNoteHeaderResponseForTest);
        }, 1000);
      }
      };

  DSIREF.utils = DSIREF.utils || {};
  DSIREF.utils = {
    service: service,
  };
  DSIREF.utils.inDevelopment = false;
}());