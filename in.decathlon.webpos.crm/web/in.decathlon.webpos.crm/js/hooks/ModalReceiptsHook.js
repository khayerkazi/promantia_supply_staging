/*global enyo */
OB.MobileApp.model.hookManager.registerHook('OBPOS_RenderListReceiptLine', function(args,c) {
  //Open the popup
	var identifier;
	    if (args.listReceiptLine.model.attributes.rCMobileNo !== '') {
	      identifier = args.listReceiptLine.model.attributes.rCMobileNo;
	    } else if (args.listReceiptLine.model.attributes.syncEmail !== '') {
	      identifier = args.listReceiptLine.model.attributes.syncEmail;
	    } else if (args.listReceiptLine.model.attributes.syncLandline !== '') {
	      identifier = args.listReceiptLine.model.attributes.syncLandline;
	    }
	    args.listReceiptLine.$.identifier.setContent(identifier);
  OB.MobileApp.model.hookManager.callbackExecutor(args,c);

  return;

});