OB.IM = OB.IM || {};

OB.IM.Process = {
  execute: function (params, view) {
	 var recordId = params.button.contextView.viewGrid.getSelectedRecords()[0].id,
     processOwnerView = view.getProcessOwnerView(params.processId),
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

    isc.confirm(OB.I18N.getLabel('IM_Movement_Confirm'), function (clickedOK) {
    		if (clickedOK) {
    				this.disabled=true;
    				isc.warn("processing ... Please wait");
    				OB.RemoteCallManager.call('com.sysfore.decathlonimport.ad_process.GoodsMovementProcessActionHandler', {
    				recordId : recordId,
    				action: params.action
    				}, {}, callback);
    		}
    				this.disabled=false;
    	});
  },

  BulkGoodsMovement: function (params, view) {
    params.action = 'BulkGoodsMovement';
    OB.IM.Process.execute(params, view);
  }
};
