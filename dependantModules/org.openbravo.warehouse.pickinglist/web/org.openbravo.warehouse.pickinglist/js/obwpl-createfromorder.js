/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
isc.defineClass('OBWPL_CreateFromOrderPopup', isc.OBPopup);

isc.OBWPL_CreateFromOrderPopup.addProperties({

  width: '90%',
  height: '90%',
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  orders: [],
  warehouses: [],

  //Form
  mainform: null,
  //Buttons
  okButton: null,
  cancelButton: null,

  fields: [],
  origfields: [{
    name: 'PLType',
    title: OB.I18N.getLabel('OBWPL_PLType_Label'),
    //height: 20,
    width: '255',
    required: true,
    type: '_id_17',
    defaultToFirstOption: true,
    redrawOnChange: true,
    valueMap: {
      
      'OUT': OB.I18N.getLabel('OBWPL_PLType_Out_Label')
    }
  }, {
    name: 'Grouping',
    title: OB.I18N.getLabel('OBWPL_Grouping_Label'),
    height: 20,
    width: 255,
    required: true,
    type: '_id_17',
    defaultToFirstOption: true
  }],

  getGroupingCriteriaList: function (form) {
    var groupingField, popup = this,
        send = {
        orders: this.orders,
        action: 'getGroupingCriteria'
        };
    OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.CreateActionHandler', send, {}, function (response, data, request) {
      if (response) {
        groupingField = form.getField('Grouping');
        if (response.data) {
          groupingField.setValueMap(response.data.valueMap);
          groupingField.setDefaultValue(response.data.defaultValue);
        }
      }
    });
  },

  createLocatorField: function (warehouse) {
    var i, field, formFields = [];
    field = {
      name: 'Loc' + warehouse.id,
      title: warehouse.name,
      height: 20,
      width: 255,
      required: true,
      type: '_id_17',
      defaultToFirstOption: true,
      showIf: function (item, value, form, currentValues, context) {
        return form.getField('PLType').getValue() === 'OUT';
      }
    };
    this.fields.push(field);
    formFields = this.mainform.getFields();
    formFields.push(field);
    this.mainform.setFields(formFields);
  },

  getOutboundLocatorLists: function (form) {
    var i, warehouses = this.warehouses,
        locatorField, popup = this,
        send = {
        orders: this.orders,
        action: 'getOutboundLocatorLists'
        };
    OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.CreateActionHandler', send, {}, function (response, data, request) {
      var whs = [],
          warehouse;
      if (response && response.data) {
        whs = response.data.warehouses;
        for (i = 0; i < whs.length; i++) {
          warehouse = {
            id: whs[i].warehouseId,
            name: whs[i].warehouseName
          };
          // Avoid repeatation of adding same warehouse
          // again and again in warehouses[]
          if(popup.notExist(warehouses, warehouse)) {
            warehouses.push(warehouse);
          }
          popup.createLocatorField(warehouse);
          locatorField = form.getField('Loc' + whs[i].warehouseId);
          locatorField.setValueMap(whs[i].valueMap);
        }
      }
    });
  },

  notExist: function(warehouses, warehouse) {
    for (var j = 0; j < warehouses.length; j++) {
      if(warehouses[j].id === warehouse.id && warehouses[j].name === warehouse.name) {
		return false;
	  }
	}
    return true;
  },


  initWidget: function () {

    var orders = this.orders,
        originalView = this.view,
        params = this.params,
        me = this;
    this.fields = isc.shallowClone(this.origfields);

    this.mainform = isc.DynamicForm.create({
      numCols: 2,
      colWidths: ['*', '*'],
      titleOrientation: 'top',
      fields: this.fields
    });
    this.setTitle(OB.I18N.getLabel('OBWPL_CreatePL'));

    this.okButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.OK_BUTTON_TITLE'),
      popup: this,
      action: function () {
        var i, callback, groupingCrit, plType, locators = {},
            warehouse, me = this;

        callback = function (rpcResponse, data, rpcRequest) {
          var status = rpcResponse.status,
              context = rpcRequest.clientContext,
              view = context.originalView.getProcessOwnerView(context.popup.params.processId);
          me.enable();
          if (data.message) {
            view.messageBar.setMessage(data.message.severity, data.message.title, data.message.text);
          }
          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
        };
        me.disable();
        plType = this.popup.mainform.getItem('PLType').getValue();
        groupingCrit = this.popup.mainform.getItem('Grouping').getValue();
        if (plType === 'OUT') {
          for (i = 0; i < this.popup.warehouses.length; i++) {
            warehouse = this.popup.warehouses[i];
            if(this.popup.mainform.getItem('Loc' + warehouse.id) !== null) {
              locators[warehouse.id] = this.popup.mainform.getItem('Loc' + warehouse.id).getValue();
            }
          }
        }

        OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.CreateActionHandler', {
          orders: orders,
          action: 'create',
          plType: plType,
          groupingCrit: groupingCrit,
          locators: locators
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

    this.getGroupingCriteriaList(this.mainform);
    this.getOutboundLocatorLists(this.mainform);
    this.items = [
    isc.VLayout.create({
      defaultLayoutAlign: 'center',
      align: 'center',
      width: '100%',
      layoutMargin: 10,
      membersMargin: 6,
      members: [
      isc.HLayout.create({
        defaultLayoutAlign: 'center',
        align: 'center',
        layoutMargin: 30,
        membersMargin: 6,
        members: this.mainform
      }), isc.HLayout.create({
        defaultLayoutAlign: 'center',
        align: 'center',
        membersMargin: 10,
        members: [this.okButton, this.cancelButton]
      })]
    })];

    this.Super('initWidget', arguments);
  }

});
