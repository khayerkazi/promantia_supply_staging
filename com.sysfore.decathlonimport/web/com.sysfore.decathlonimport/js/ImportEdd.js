OB.IMPORTEDD = {};

OB.IMPORTEDD.Process = {
		execute: function (params, view) {
			var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
			edds = [],
			callback;

			callback = function (rpcResponse, data, rpcRequest) {
				// show result
				//isc.say(OB.I18N.getLabel('IM_Importedd_Validate', [data.updated]));
				if([data.operation]=='validate'){
					isc.say(OB.I18N.getLabel('IM_Importedd_Validate', [data.updated]));
					for(var j=0; j<[data.updated]; j++)
					{
						if([data.errStatus] == 1){
							view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR,'There are records with Errors');
							//params.button.closeProcessPopup();
						}
						else {
							view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, 'Validation Successful');
							// close process to refresh current view
							//params.button.closeProcessPopup();
						}//params.button.closeProcessPopup();
					}params.button.closeProcessPopup();
				}
				else if ([data.operation]=='process'){
					isc.say(OB.I18N.getLabel('IM_Importedd_Process', [data.updated]));
					if([data.processed]=='Success')
					{
						//isc.say(OB.I18N.getLabel('TS_Obexapp_Updateed', [data.msg]));
						view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS,'Records have been processed');
						params.button.closeProcessPopup();
						//throw new IllegalStateException("Chosen records have an error message");
					}
					else{
						view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR,' Chosen records have an error. Kindly check the error message');
						}
				}
				else {
					//alert('Incorrect Operation');
					throw new IllegalStateException("Action not supported: " + action);
				}

			};

			for (i = 0; i < selection.length; i++) {
				edds.push(selection[i].id);
			};

			OB.RemoteCallManager.call('com.sysfore.decathlonimport.ad_process.ImportEDDActionHandler', {
				edds: edds,
				action: params.action
			}, {}, callback);
		},

		validate: function (params, view) {
			params.action = 'validate';
			OB.IMPORTEDD.Process.execute(params, view);
		},

		process: function (params, view) {
			params.action = 'process';
			OB.IMPORTEDD.Process.execute(params, view);
		}
};
