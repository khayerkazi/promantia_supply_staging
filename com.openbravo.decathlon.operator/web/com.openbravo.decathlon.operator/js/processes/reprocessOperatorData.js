/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

OB = OB || {};

OB.DECOPE = OB.DECOPE || {};

OB.DECOPE.OperatorErrorsProcess = {
  execute: function (params, view) {
    var i, selectedRecords = params.button.contextView.viewGrid.getSelectedRecords(),
        operatorErrors = [],
        callback;

    callback = function (rpcResponse, data, rpcRequest) {
      var msg = data.message;

      params.button.contextView.messageBar.setMessage(msg.severity, msg.title, msg.text);
      isc.clearPrompt();
      if (msg.severity === 'success') {
        if (params.button.contextView.isShowingForm) {
          params.button.contextView.switchFormGridVisibility();
        }
        params.button.contextView.viewGrid.refreshGrid();
      } else {
        if (params.button.contextView.isShowingForm) {
          params.button.closeProcessPopup();
        } else {
          params.button.contextView.viewGrid.refreshGrid();
        }
      }
    };

    for (i = 0; i < selectedRecords.length; i++) {
      operatorErrors.push(selectedRecords[i].id);
    }
    isc.confirm(OB.I18N.getLabel('Decope_Error'), function (clickedOK) {
      if (clickedOK) {
        isc.showPrompt(OB.I18N.getLabel('OBUIAPP_PROCESSING') + isc.Canvas.imgHTML({
          src: OB.Styles.LoadingPrompt.loadingImage.src
        }));
        OB.RemoteCallManager.call('com.openbravo.decathlon.operator.handlers.OperatorReprocessHandler', {
          operatorErrors: operatorErrors
        }, {}, callback);
        params.button.contextView.viewGrid.refreshGrid();
      }
    });
  },

  reprocessOperatorErrors: function (params, view) {
    OB.DECOPE.OperatorErrorsProcess.execute(params, view);
  }
};