/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
OB.OBWPACK = OB.OBWPACK || {};
OB.OBWPACK.Process = {
  create: function (params, view) {
    var recordId = params.button.contextView.viewGrid.getSelectedRecords()[0].id,
        processOwnerView = view.getProcessOwnerView(params.processId),
        callback;

    callback = function (rpcResponse, data, rpcRequest) {
      var processLayout, popupTitle;
      if (!data || !data.data) {
        view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
        return;
      }
      if (data.message) {
        view.activeView.messageBar.setMessage(isc.OBMessageBar[data.message.severity], null, data.message.text);
        return;
      }
      popupTitle = OB.I18N.getLabel('OBWPACK_Packing') + ' - ' + params.button.contextView.viewGrid.getSelectedRecords()[0]._identifier;
      processLayout = isc.OBPackingProcess.create({
        parentWindow: view,
        sourceView: view.activeView,
        buttonOwnerView: processOwnerView,
        shipmentId: recordId,
        packingGridData: data.data,
        boxNo: data.boxNo || 1,
        valuecheck: data.valuecheck,
        windowId: data.windowId,
        headerStatus: data.headerStatus
      });
      view.openPopupInTab(processLayout, popupTitle, OB.Styles.OBWPACK.PackingProcess.popupWidth, OB.Styles.OBWPACK.PackingProcess.popupHeight, true, true, true, true);
    };
    OB.RemoteCallManager.call('org.openbravo.warehouse.packing.PackingActionHandler', {
      recordId: recordId,
      action: 'open'
    }, {}, callback);
  },
  createHeader: function (params, view) {
    var recordId = params.button.contextView.viewGrid.getSelectedRecords()[0].id,
        processOwnerView = view.getProcessOwnerView(params.processId),
        callback;

    callback = function (rpcResponse, data, rpcRequest) {
      var processLayout, popupTitle;
      if (!data || !data.data) {
        view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
        return;
      }
      if (data.message) {
        view.activeView.messageBar.setMessage(isc.OBMessageBar[data.message.severity], null, data.message.text);
        return;
      }
      popupTitle = OB.I18N.getLabel('OBWPACK_Packing') + ' - ' + params.button.contextView.viewGrid.getSelectedRecords()[0]._identifier;
      processLayout = isc.OBPackingProcess.create({
        parentWindow: view,
        sourceView: view.activeView,
        buttonOwnerView: processOwnerView,
        shipmentId: recordId,
        packingGridData: data.data,
        boxNo: data.boxNo || 1,
        valuecheck: data.valuecheck,
        windowId: data.windowId,
        headerStatus: data.headerStatus
      });
      view.openPopupInTab(processLayout, popupTitle, OB.Styles.OBWPACK.PackingProcess.popupWidth, OB.Styles.OBWPACK.PackingProcess.popupHeight, true, true, true, true);
    };
    OB.RemoteCallManager.call('org.openbravo.warehouse.packing.PackingActionHandler', {
      recordId: recordId,
      action: 'openHeader'
    }, {}, callback);
  },
  process: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        packHeaders = [],
        callback;
    callback = function (rpcResponse, data, rpcRequest) {
      var message = data.message;
      view.activeView.messageBar.setMessage(isc.OBMessageBar[message.severity], message.title, message.text);
      // close process to refresh current view
      params.button.closeProcessPopup();
    };
    for (i = 0; i < selection.length; i++) {
      packHeaders.push(selection[i].id);
    }
    isc.confirm(OB.I18N.getLabel('OBWPACK_CreateConfirm'), function (clickedOK) {
      if (clickedOK) {
        OB.RemoteCallManager.call('org.openbravo.warehouse.packing.ProcessPackHActionHandler', {
          packHeaders: packHeaders,
          action: 'process'
        }, {}, callback);
      }
    });
  },
  processship: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        packHeaders = [],
        callback;
    callback = function (rpcResponse, data, rpcRequest) {
      var message = data.message;
      view.activeView.messageBar.setMessage(isc.OBMessageBar[message.severity], message.title, message.text);
      // close process to refresh current view
      params.button.closeProcessPopup();
    };
    for (i = 0; i < selection.length; i++) {
      packHeaders.push(selection[i].id);
    }
    isc.confirm(OB.I18N.getLabel('OBWPACK_CreateConfirm'), function (clickedOK) {
      if (clickedOK) {
        OB.RemoteCallManager.call('org.openbravo.warehouse.packing.ProcessPackHActionHandler', {
          packHeaders: packHeaders,
          action: 'processShip'
        }, {}, callback);
      }
    });
  }
};