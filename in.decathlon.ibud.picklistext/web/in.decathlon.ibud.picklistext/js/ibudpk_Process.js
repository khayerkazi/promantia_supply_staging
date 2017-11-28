
OB.IBUDPK = OB.IBUDPK || {};
OB.IBUDPK.Process = {

	completeProcess: function (params, view, doGroup) {

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

    isc.IBUDPK_CompletePopup.create({
    	pickings: pickings,
    	view: view,
    	params: params,
    	organization: org,
    	doGroup: doGroup
    }).show();
	},
	
	shipmentProcess: function (params, view, doGroup) {

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

	    isc.IBUDPK_ShipmentPopup.create({
	    	pickings: pickings,
	    	view: view,
	    	params: params,
	    	organization: org,
	    	doGroup: doGroup
	    }).show();
		},

  complete: function (params, view) {
    OB.IBUDPK.Process.completeProcess(params, view, true);
  },
	
  processShipment: function (params, view){
	  OB.IBUDPK.Process.shipmentProcess(params, view, true);
  }
};