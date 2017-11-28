/*global enyo */
OB.MobileApp.model.hookManager.registerHook('OBPOS_PreAddProductToOrder', function(args, c) {
  //Open the popup
  if (((args.receipt.attributes.rCMobileNo === "" || args.receipt.attributes.rCMobileNo === null || args.receipt.attributes.rCMobileNo === undefined) && (args.receipt.attributes.syncEmail === "" || args.receipt.attributes.syncEmail === null || args.receipt.attributes.syncEmail === undefined)) && (args.receipt.attributes.syncLandline === "" || args.receipt.attributes.syncLandline === null || args.receipt.attributes.syncLandline === undefined)) {
    args.cancelOperation = true;

    //OB.UTIL.showConfirmation.display('Please assign the customer to this order');
    OB.MobileApp.view.$.containerWindow.showPopup('DWCRM_UI_CustomerPropertiesImpl', {
      //Parameters
      // Receipt model is send to the popup
      receipt: args.receipt,

      // Callback will be invoked by the popup when all is ready.
      // passing it through arguments
      callback: function(cancel) {
        if (((args.receipt.attributes.rCMobileNo === "" || args.receipt.attributes.rCMobileNo === null || args.receipt.attributes.rCMobileNo === undefined) && (args.receipt.attributes.syncEmail === "" || args.receipt.attributes.syncEmail === null || args.receipt.attributes.syncEmail === undefined)) && (args.receipt.attributes.syncLandline === "" || args.receipt.attributes.syncLandline === null || args.receipt.attributes.syncLandline === undefined)) {
          // This argument indicates that the operation will be cancelled.
          args.cancelOperation = true;
          //args.productToAdd.Set('');
        }
        // Custom logic has finished. The standard flow will continue.
        OB.MobileApp.model.hookManager.callbackExecutor(args, c);
      }
    });
  }

  OB.MobileApp.model.hookManager.callbackExecutor(args, c);

  return;

});