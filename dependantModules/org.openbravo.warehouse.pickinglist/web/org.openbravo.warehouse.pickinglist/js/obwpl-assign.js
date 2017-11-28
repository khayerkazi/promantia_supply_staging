/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
isc.defineClass('OBWPL_AssignPopup', isc.OBPopup);

isc.OBWPL_AssignPopup.addProperties({

  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  mainform: null,
  okButton: null,
  cancelButton: null,
  pickings: null,
  organization: null,
  doGroup: null,
  view: null,
  params: null,

  getEmployeeList: function (form) {
    var send = {},
        employeeField, popup = this;
    send.pickings = this.pickings;
    send.action = 'getemployees';
    send.organization = this.organization;
    OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.AssignActionHandler', send, {}, function (response, data, request) {
      if (response) {
        employeeField = form.getField('Employee');
        if (response.data) {
          employeeField.setValueMap(response.data.valuecheck.valueMap);
          employeeField.setDefaultValue(response.data.valuecheck.defaultValue);
        }
      }
    });
  },

  initWidget: function () {

    var pickings = this.pickings,
        originalView = this.view,
        params = this.params;

    if (this.doGroup) {
      this.mainform = isc.DynamicForm.create({
        numCols: 2,
        colWidths: ['50%', '50%'],
        fields: [{
          name: 'Employee',
          height: 20,
          width: 255,
          required: true,
          type: '_id_17',
          defaultToFirstOption: true
        }, {
          name: 'Group',
          height: 20,
          width: 255,
          required: true,
          type: '_id_20'
        }]
      });
    } else {
      this.mainform = isc.DynamicForm.create({
        numCols: 2,
        colWidths: ['50%', '50%'],
        fields: [{
          name: 'Employee',
          height: 20,
          width: 255,
          required: true,
          type: '_id_17',
          defaultToFirstOption: true
        }]
      });
    }
    this.setTitle(OB.I18N.getLabel('OBWPL_CreatePL'));

    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE'),
      popup: this,
      action: function () {
        var callback, employee, group = false;

        callback = function (rpcResponse, data, rpcRequest) {
          var status = rpcResponse.status,
              context = rpcRequest.clientContext,
              view = context.originalView.getProcessOwnerView(context.popup.params.processId);
          if (data.message) {
            view.messageBar.setMessage(data.message.severity, data.message.title, data.message.text);

          }
          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
        };

        employee = this.popup.mainform.getItem('Employee').getValue();
        if (this.popup.doGroup) {
          group = this.popup.mainform.getItem('Group').getValue();
        }

        OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.AssignActionHandler', {
          pickings: pickings,
          action: 'assign',
          employee: employee,
          group: group
        }, {}, callback, {
          originalView: this.popup.view,
          popup: this.popup
        });
      }
    });

    this.cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.getEmployeeList(this.mainform);

    this.items = [
    isc.VLayout.create({
      defaultLayoutAlign: "center",
      align: "center",
      width: "100%",
      layoutMargin: 10,
      membersMargin: 6,
      members: [
      isc.HLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        layoutMargin: 30,
        membersMargin: 6,
        members: this.mainform
      }), isc.HLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        membersMargin: 10,
        members: [this.okButton, this.cancelButton]
      })]
    })];

    this.Super('initWidget', arguments);
  }

});