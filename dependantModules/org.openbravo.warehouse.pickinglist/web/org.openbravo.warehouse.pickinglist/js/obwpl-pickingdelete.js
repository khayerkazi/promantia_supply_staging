/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
isc.OBToolbar.addClassProperties({
  OBWPL_Original_DELETE_BUTTON_PROPERTIES_action: isc.OBToolbar.DELETE_BUTTON_PROPERTIES.action
});

isc.addProperties(isc.OBToolbar.DELETE_BUTTON_PROPERTIES, isc.OBToolbar.DELETE_BUTTON_PROPERTIES, {
  action: function () {
    var a = isc.OBMessageBar.TYPE_SUCCESS;
    if (this.view.tabId === '2C7235D821114C619D8205C99F4ECCEA') {
      var msg, callback, mvmtLines = [],
          i, view = this.view,
          grid = view.viewGrid,
          selectedRecords = grid.getSelectedRecords();
      // collect the remittanceLines ids
      for (i = 0; i < selectedRecords.length; i++) {
        mvmtLines.push(selectedRecords[i].id);
      }

      // define the callback function which shows the result to the user
      callback = function (rpcResponse, data, rpcRequest) {
        var view = rpcRequest.clientContext.view;
        view.refresh();
        view.messageBar.setMessage(data.message.severity, data.message.title, data.message.text);
      };
      if (selectedRecords.length === 1) {
        msg = OB.I18N.getLabel('OBUIAPP_DeleteConfirmationSingle');
      } else {
        msg = OB.I18N.getLabel('OBUIAPP_DeleteConfirmationMultiple', [selectedRecords.length]);
      }
      // and call the server
      isc.confirm(msg, function (clickedOK) {
        if (clickedOK) {
          OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.actionhandler.MvmtLineDeleteHandler', {
            mvmtLines: mvmtLines
          }, {}, callback, {
            view: view
          });
        }
      });

    } else {
      isc.OBToolbar.OBWPL_Original_DELETE_BUTTON_PROPERTIES_action.call(this, arguments);
    }
  }
});