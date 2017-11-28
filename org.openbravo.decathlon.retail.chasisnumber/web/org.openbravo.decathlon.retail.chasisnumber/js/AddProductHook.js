/*global enyo */
OB.MobileApp.model.hookManager.registerHook('OBPOS_PreAddProductToOrder', function(args, c) {
  //Open the popup
  if (args.productToAdd.get('clIschasisnumberrequired') && args.productToAdd.get('clIschasisnumberrequired') === true) {
    OB.MobileApp.view.$.containerWindow.showPopup('CHNO.ChasisNumber.GetChasisPopup', {
      product: args.productToAdd,
      callback: function (result) {
        if (!result) {
          args.cancelOperation = true;
        }
        OB.MobileApp.model.hookManager.callbackExecutor(args, c);
      }
    });
  } else {
    OB.MobileApp.model.hookManager.callbackExecutor(args, c);
  }
});