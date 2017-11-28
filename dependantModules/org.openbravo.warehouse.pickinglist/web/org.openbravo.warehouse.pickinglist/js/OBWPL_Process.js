/************************************************************************************
 * Copyright (C) 2012-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
OB.OBWPL = OB.OBWPL || {};
OB.OBWPL.Process = {
  create: function (params, view) {
    var i, j, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        orders = [];
    if (selection.length > 100){
        isc.showMessage(OB.I18N.getLabel('SW_SelectLimit'), OB.I18N.getLabel('OBUIAPP_Error'));
         return;
    }
    for (i = 0; i < selection.length; i++) {
      orders.push(selection[i].id);
    }

    isc.OBWPL_CreateFromOrderPopup.create({
      orders: orders,
      view: view,
      params: params
    }).show();
  },


  assignProcess: function (params, view, doGroup) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        pickings = [],
        org = selection[0].organization;

    for (i = 0; i < selection.length; i++) {
      if (org !== selection[i].organization) {
        isc.showMessage(OB.I18N.getLabel('OBWPL_MultipleOrgs'), OB.I18N.getLabel('OBUIAPP_Error'));
        return;
      }
      pickings.push(selection[i].id);
    }
    if (selection.length === 1) {
      doGroup = false;
    }

    isc.OBWPL_AssignPopup.create({
      pickings: pickings,
      view: view,
      params: params,
      organization: org,
      doGroup: doGroup
    }).show();
  },

  assign: function (params, view) {
    OB.OBWPL.Process.assignProcess(params, view, true);
  },
  reassign: function (params, view) {
    OB.OBWPL.Process.assignProcess(params, view, false);
  },

  validate: function (params, view) {
    var recordId = params.button.contextView.viewGrid.getSelectedRecords()[0].id,
        processOwnerView = view.getProcessOwnerView(params.processId),
        processAction, callback;

    processAction = function () {
      OB.OBWPL.Process.process(params, view);
    };
    callback = function (rpcResponse, data, rpcRequest) {
      var processLayout, popupTitle;
      if (!data) {
        view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
        return;
      }
      if (data.message || !data.data) {
        view.activeView.messageBar.setMessage(isc.OBMessageBar[data.message.severity], null, data.message.text);
        return;
      }
      popupTitle = OB.I18N.getLabel('OBWPL_PickingList') + ' - ' + params.button.contextView.viewGrid.getSelectedRecords()[0]._identifier;
      processLayout = isc.OBPickValidateProcess.create({
        parentWindow: view,
        sourceView: view.activeView,
        buttonOwnerView: processOwnerView,
        pickGridData: data.data,
        processAction: processAction
      });
      view.openPopupInTab(processLayout, popupTitle, OB.Styles.OBWPL.PickValidateProcess.popupWidth, OB.Styles.OBWPL.PickValidateProcess.popupHeight, true, true, true, true);
    };
    OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.ValidateActionHandler', {
      recordId: recordId,
      action: 'validate'
    }, {}, callback);
  },

  pickingHandlerCall: function (params, view, action, confirmMsg) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        pickinglist = [],
        callback;

    callback = function (rpcResponse, data, rpcRequest) {
      var message = data.message,
          processView = view.getProcessOwnerView(params.processId);
      processView.messageBar.setMessage(message.severity, message.title, message.text);
      // close process to refresh current view
      params.button.closeProcessPopup();
      isc.clearPrompt();
    };
    for (i = 0; i < selection.length; i++) {
      pickinglist.push(selection[i].id);
    }
    isc.confirm(OB.I18N.getLabel(confirmMsg), function (clickedOK) {
      if (clickedOK) {
        isc.showPrompt(OB.I18N.getLabel('OBUIAPP_PROCESSING') + isc.Canvas.imgHTML({
          src: OB.Styles.LoadingPrompt.loadingImage.src
        }));
        OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.PickingListActionHandler', {
          pickinglist: pickinglist,
          action: action
        }, {}, callback);
      }
    });
  },

  cancel: function (params, view) {
    OB.OBWPL.Process.pickingHandlerCall(params, view, 'cancel', 'OBWPL_CancelConfirm');
  },

  process: function (params, view) {
    OB.OBWPL.Process.pickingHandlerCall(params, view, 'process', 'OBWPL_ProcessConfirm');
  },

  close: function (params, view) {
    OB.OBWPL.Process.pickingHandlerCall(params, view, 'close', 'OBWPL_CloseConfirm');
  }

};