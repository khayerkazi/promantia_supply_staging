OB.OBMULTISO = OB.OBMULTISO || {};
 
OB.OBMULTISO.Process = {
  execute: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
        orders = [],
        callback;
 
    callback = function (rpcResponse, data, rpcRequest) {
		
		 var processLayout, popupTitle;
    	 
    	 if (data == null) {
    	        view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
    	        return;
    	      } else {
    	    		var message = data.message;
    	    	      view.activeView.messageBar.setMessage(isc.OBMessageBar[message.severity], message.title, message.text);
    	    	      isc.say("Please click 'OK'");
    	    	      // close process to refresh current view
    	    	      params.button.closeProcessPopup();
    	    	}
    	 view.openPopupInTab(processLayout,'Movement Process','900', '90%', true, true, true, true);
    };
 
    for (i = 0; i < selection.length; i++) {
      orders.push(selection[i].id);
    };
 
    isc.confirm("Do you want to proceed ?", function (clickedOK) {
        if (clickedOK) {
			this.disabled=true;
    		isc.warn("processing ... Please wait");
		    OB.RemoteCallManager.call('in.nous.dmi.orderclose.ad_process.MultiSendOrderHandler', {
		      orders: orders,
		      action: params.action
		    }, {}, callback);
        } this.disabled=false;
    });
  },
 
  sendorder: function (params, view) {
    params.action = 'sendorder';
    OB.OBMULTISO.Process.execute(params, view);
  },
 
  updateorder: function (params, view) {
	    params.action = 'updateorder';
	    OB.OBMULTISO.Process.execute(params, view);
	  },
	  
  deleteorder: function (params, view) {
	    params.action = 'deleteorder';
	    OB.OBMULTISO.Process.execute(params, view);
	  },
	  
   orderclosed: function (params, view) {
	    params.action = 'orderclosed';
	    OB.OBMULTISO.Process.execute(params, view);
	  }
};
