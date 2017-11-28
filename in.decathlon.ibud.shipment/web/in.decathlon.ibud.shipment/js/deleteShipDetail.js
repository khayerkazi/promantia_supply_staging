OB.UTIL = window.OB.UTIL || {};
 OB.UTIL.DeleteUtils = OB.UTIL.DeleteUtils || {};
OB.UTIL.DeleteUtils.getRecord =  function (params, view) {
//alert('hello   .....');
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        orders = [],
        callback;
 
    callback = function (rpcResponse, data, rpcRequest) {
// isc.say(OB.I18N.getLabel('Obexapp_Updated'));
          isc.say( data.message);
params.button.closeProcessPopup();
    };
 
    for (i = 0; i < selection.length; i++) {
      orders.push(selection[i].id);
    };
 
    OB.RemoteCallManager.call('in.decathlon.ibud.shipment.DeleteActionHandler', {
      orders: orders,
      action: params.action,
      refreshGrid: true

    }, {}, callback);
  };
