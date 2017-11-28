/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
OB.OBWPL = OB.OBWPL || {};
OB.OBWPL.MovementLine = {

  complete: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        movementlines = [],
        callback;

    callback = function (rpcResponse, data, rpcRequest) {
      var message = data.message;
      view.activeView.messageBar.setMessage(message.severity, message.title, message.text);
      // close process to refresh current view
      params.button.closeProcessPopup();
      isc.clearPrompt();
    };
    for (i = 0; i < selection.length; i++) {
      movementlines.push(selection[i].id);
    }
    isc.confirm(OB.I18N.getLabel('OBWPL_LineCompleteConfirm'), function (clickedOK) {
      if (clickedOK) {
        isc.showPrompt(OB.I18N.getLabel('OBUIAPP_PROCESSING') + isc.Canvas.imgHTML({
          src: OB.Styles.LoadingPrompt.loadingImage.src
        }));
        OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.MovementLineHandler', {
          movementlines: movementlines,
          action: 'confirm'
        }, {}, callback);
      }
    });
  },
  reject: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        movementlines = [],
        callback;

    callback = function (rpcResponse, data, rpcRequest) {
      var message = data.message;
      view.activeView.messageBar.setMessage(message.severity, message.title, message.text);
      // close process to refresh current view
      params.button.closeProcessPopup();
      isc.clearPrompt();
    };
    for (i = 0; i < selection.length; i++) {
      movementlines.push(selection[i].id);
    }
    isc.confirm(OB.I18N.getLabel('OBWPL_LineRejectConfirm'), function (clickedOK) {
      if (clickedOK) {
        isc.showPrompt(OB.I18N.getLabel('OBUIAPP_PROCESSING') + isc.Canvas.imgHTML({
          src: OB.Styles.LoadingPrompt.loadingImage.src
        }));
        OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.MovementLineHandler', {
          movementlines: movementlines,
          action: 'reject'
        }, {}, callback);
      }
    });
  }
};