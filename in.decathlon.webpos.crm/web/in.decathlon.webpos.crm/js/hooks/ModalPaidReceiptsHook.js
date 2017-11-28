/*global enyo */
OB.MobileApp.model.hookManager.registerHook('OBPOS_RenderPaidReceiptLine', function(args,c) {
  //Open the popup
	if ((args.paidReceiptLine.model.attributes.rCMobileNo !== null) && (args.paidReceiptLine.model.attributes.rCMobileNo !== "")) {
	      args.paidReceiptLine.$.topLine.setContent(args.paidReceiptLine.model.attributes.documentNo + ' - ' + args.paidReceiptLine.model.attributes.businessPartner + '-' + args.paidReceiptLine.model.attributes.rCMobileNo );
	      args.paidReceiptLine.$.bottonLine.setContent(args.paidReceiptLine.model.attributes.totalamount + ' (' + args.paidReceiptLine.model.attributes.orderDate.substring(0, 10) + ') ');
	    } else if ((args.paidReceiptLine.model.attributes.syncEmail !== null) && (args.paidReceiptLine.model.attributes.syncEmail !== "")) {
	      args.paidReceiptLine.$.topLine.setContent(args.paidReceiptLine.model.attributes.documentNo + ' - ' + args.paidReceiptLine.model.attributes.businessPartner + '-' + args.paidReceiptLine.model.attributes.syncEmail );
	      args.paidReceiptLine.$.bottonLine.setContent(args.paidReceiptLine.model.attributes.totalamount + ' (' + args.paidReceiptLine.model.attributes.orderDate.substring(0, 10) + ') ');
	    } else if ((args.paidReceiptLine.model.attributes.syncLandline !== null) && (args.paidReceiptLine.model.attributes.syncLandline !== "")) {
	      args.paidReceiptLine.$.topLine.setContent(args.paidReceiptLine.model.attributes.documentNo + ' - ' + args.paidReceiptLine.model.attributes.businessPartner + '-' + args.paidReceiptLine.model.attributes.syncLandline );
	      args.paidReceiptLine.$.bottonLine.setContent(args.paidReceiptLine.model.attributes.totalamount + ' (' + args.paidReceiptLine.model.attributes.orderDate.substring(0, 10) + ') ');
	    } else {
	      args.paidReceiptLine.$.topLine.setContent(args.paidReceiptLine.model.attributes.documentNo + '-' + args.paidReceiptLine.model.attributes.businessPartner);
	      args.paidReceiptLine.$.bottonLine.setContent(args.paidReceiptLine.model.attributes.totalamount + ' (' + args.paidReceiptLine.model.attributes.orderDate.substring(0, 10) + ') ');
	    }
  OB.MobileApp.model.hookManager.callbackExecutor(args,c);

  return;

});