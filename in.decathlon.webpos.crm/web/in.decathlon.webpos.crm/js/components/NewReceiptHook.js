/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */


(function () {
	if (OB.MobileApp.model.hookManager) {

	OB.MobileApp.model.hookManager.registerHook('OBPOS_NewReceipt', function(args, c){
		OB.DWCRM.CRMCustomer={};
		args.context.doShowPopup({
	        popup: 'DWCRM_UI_CustomerPropertiesImpl',
	        args: args
	      });
		
		  OB.MobileApp.model.hookManager.callbackExecutor(args, c);
		});
	}
}());