/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B, Backbone, $, _, enyo */

//Modal pop up
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.cas',
  kind: 'OB.UI.Subwindow',
  events: {
    onSearchAction: ''
  },
  beforeSetShowing: function (params) {
    if (this.caller === 'mainSubWindow') {
      this.waterfall('onSearchAction', {
        cleanResults: true
      });
    } else {
      this.waterfall('onSearchAction', {
        executeLastCriteria: true
      });
    }
    this.$.subWindowBody.$.casbody.$.listcustomers.$.leftbar.$.newAction.putDisabled(!OB.MobileApp.model.hasPermission('OBPOS_retail.editCustomers'));
    return true;
  },
  beforeClose: function (dest) {
    if (dest === 'mainSubWindow') {
      this.waterfall('onSearchAction', {
        cleanResults: true
      });
    }
    return true;
  },
  defaultNavigateOnClose: 'mainSubWindow',
  header: {
    kind: 'OB.OBPOSPointOfSale.UI.customers.casheader'
  },
  body: {
    kind: 'OB.OBPOSPointOfSale.UI.customers.casbody',
    name: 'casbody'
  }
});

//Header of the body of cas (customer advanced search)
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.ModalCustomerScrollableHeader',
  kind: 'OB.UI.ScrollableTableHeader',
  style: 'border-bottom: 10px solid #ff0000;',
  events: {
    onSearchAction: '',
    onClearAction: ''
  },
  handlers: {
    onFiltered: 'searchAction'
  },
  components: [{
    style: 'padding: 10px; 10px; 0px; 10px;',
    components: [{
      style: 'display: table;',
      components: [{
        style: 'display: table-cell; width: 100%;',
        components: [{
          kind: 'OB.UI.SearchInputAutoFilter',
          name: 'filterText',
          style: 'width: 100%',
          isFirstFocus: true
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          classes: 'btnlink-gray btn-icon-small btn-icon-clear',
          style: 'width: 100px; margin: 0px 5px 8px 19px;',
          ontap: 'clearAction'
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          classes: 'btnlink-yellow btn-icon-small btn-icon-search',
          style: 'width: 100px; margin: 0px 0px 8px 5px;',
          ontap: 'searchAction'
        }]
      }]
    }]
  }],
  clearAction: function () {
    this.$.filterText.setValue('');
    this.doClearAction();
  },
  searchAction: function () {
    this.doSearchAction({
      bpName: this.$.filterText.getValue(),
      operator: OB.Dal.CONTAINS
    });
    return true;
  }
});

/*items of collection Customer*/
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.ListCustomersLine',
  kind: 'OB.UI.SelectButton',
  classes: 'btnselect-customer',
  components: [{
    name: 'line',
    style: 'line-height: 23px;',
    components: [{
      style: 'float: left; font-weight: bold;',
      name: 'identifier'
    }, {
      style: 'float: left;',
      name: 'address'
    }, {
      style: 'clear: both;'
    }]
  }],
  create: function () {
    this.inherited(arguments);
    this.$.identifier.setContent(this.model.get('_identifier') + ' / ');
    this.$.address.setContent(this.model.get('locName'));
  }
});

/*Search Customer Button*/
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
  kind: 'OB.UI.Button',
  events: {
    onSearchAction: ''
  },
  classes: 'btnlink-left-toolbar',
  searchAction: function (params) {
    this.doSearchAction({
      bpName: params.initial,
      operator: params.operator
    });
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(this.i18nLabel ? OB.I18N.getLabel(this.i18nLabel) : this.label);
  }
});

/*New Customer Button*/
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.NewCustomerButton',
  kind: 'OB.UI.Button',
  events: {
    onChangeSubWindow: ''
  },
  disabled: false,
  classes: 'btnlink-left-toolbar',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    var sw = this.subWindow;
    this.doChangeSubWindow({
      newWindow: {
        name: 'customerCreateAndEdit',
        params: {
          navigateOnClose: 'customerView'
        }
      }
    });
  },
  putDisabled: function (status) {
    if (status === false) {
      this.disabled = false;
      this.setDisabled(false);
      this.removeClass('disabled');
      return;
    } else {
      this.disabled = true;
      this.setDisabled();
      this.addClass('disabled');
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(this.i18nLabel ? OB.I18N.getLabel(this.i18nLabel) : this.label);
  }
});

/* Buttons Left bar*/
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.CustomerLeftBar',
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.customers.NewCustomerButton',
    i18nLabel: 'OBPOS_LblNew',
    name: 'newAction'
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblAC',
    tap: function () {
      this.searchAction({
        initial: 'A,B,C',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblDF',
    tap: function () {
      this.searchAction({
        initial: 'D,E,F',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblGI',
    tap: function () {
      this.searchAction({
        initial: 'G,H,I',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblJL',
    tap: function () {
      this.searchAction({
        initial: 'J,K,L',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblMO',
    tap: function () {
      this.searchAction({
        initial: 'M,N,O',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblPR',
    tap: function () {
      this.searchAction({
        initial: 'P,Q,R',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblSV',
    tap: function () {
      this.searchAction({
        initial: 'S,T,U,V',
        operator: OB.Dal.STARTSWITH
      });
    }
  }, {
    kind: 'OB.OBPOSPointOfSale.UI.customers.SearchCustomerButton',
    i18nLabel: 'OBPOS_LblWZ',
    tap: function () {
      this.searchAction({
        initial: 'W,X,Y,Z',
        operator: OB.Dal.STARTSWITH
      });
    }
  }]
});

/*scrollable table (body of customer)*/
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.ListCustomers',
  handlers: {
    onSearchAction: 'searchAction',
    onClearAction: 'clearAction'
  },
  events: {
    onChangeBusinessPartner: '',
    onChangeSubWindow: ''
  },
  components: [{
    style: 'text-align: center; padding-top: 10px; float: left; width: 19%;',
    components: [{
      kind: 'OB.OBPOSPointOfSale.UI.customers.CustomerLeftBar',
      name: 'leftbar'
    }]
  }, {
    style: 'float: left; width: 80%;',
    components: [{
      classes: 'row-fluid',
      components: [{
        classes: 'span12',
        components: [{
          name: 'bpslistitemprinter',
          kind: 'OB.UI.ScrollableTable',
          scrollAreaMaxHeight: '301px',
          renderHeader: 'OB.OBPOSPointOfSale.UI.customers.ModalCustomerScrollableHeader',
          renderLine: 'OB.OBPOSPointOfSale.UI.customers.ListCustomersLine',
          renderEmpty: 'OB.UI.RenderEmpty'
        }]
      }]
    }]
  }, {
    style: 'clear: both'
  }],
  clearAction: function (inSender, inEvent) {
    this.bpsList.reset();
    return true;
  },
  searchAction: function (inSender, inEvent) {
    var me = this,
        filter, splitFilter, splitFilterLength, _operator, i, criteria = {},
        lastCriteria = [];

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
    }

    function successCallbackBPs(dataBps, args) {
      if (args === 0) {
        me.bpsList.reset();
      }
      if (dataBps && dataBps.length > 0) {
        me.bpsList.add(dataBps.models);
      }
    }

    function reset(me) {
      me.bpsList.reset();
      me.lastCriteria = null;
      return true;
    }

    if (inEvent.executeLastCriteria) {
      if (this.lastCriteria) {
        for (i = 0; i < this.lastCriteria.length; i++) {
          OB.Dal.find(OB.Model.BusinessPartner, this.lastCriteria[i], successCallbackBPs, errorCallback, i);
          lastCriteria.push(this.lastCriteria[i]);
        }
        this.lastCriteria = lastCriteria;
        return true;
      } else {
        return reset(this);
      }
    }

    if (inEvent.cleanResults) {
      return reset(this);
    }

    filter = inEvent.bpName;
    splitFilter = filter.split(",");
    splitFilterLength = splitFilter.length;
    _operator = inEvent.operator;

    if (filter && filter !== '') {
      for (i = 0; i < splitFilter.length; i++) {
        criteria._filter = {
          operator: _operator,
          value: splitFilter[i]
        };
        OB.Dal.find(OB.Model.BusinessPartner, criteria, successCallbackBPs, errorCallback, i);
        lastCriteria.push(enyo.clone(criteria));
      }
      this.lastCriteria = lastCriteria;
    } else {
      OB.Dal.find(OB.Model.BusinessPartner, criteria, successCallbackBPs, errorCallback, 0);
    }


    return true;
  },
  bpsList: null,
  init: function () {
    this.bpsList = new Backbone.Collection();
    this.$.bpslistitemprinter.setCollection(this.bpsList);
    this.bpsList.on('click', function (model) {
      var sw = this.subWindow;
      this.doChangeSubWindow({
        newWindow: {
          name: 'customerView',
          params: {
            navigateOnClose: sw.getName(),
            businessPartner: model
          }
        }
      });
    }, this);
  }
});

//header
enyo.kind({
  kind: 'OB.UI.SubwindowHeader',
  name: 'OB.OBPOSPointOfSale.UI.customers.casheader',
  onTapCloseButton: function () {
    var subWindow = this.subWindow;
    subWindow.doChangeSubWindow({
      newWindow: {
        name: 'mainSubWindow'
      }
    });
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBPOS_TitleCustomerAdvancedSearch'));
  }
});


/*instance*/
enyo.kind({
  name: 'OB.OBPOSPointOfSale.UI.customers.casbody',
  components: [{
    kind: 'OB.OBPOSPointOfSale.UI.customers.ListCustomers',
    name: 'listcustomers'

  }]
});