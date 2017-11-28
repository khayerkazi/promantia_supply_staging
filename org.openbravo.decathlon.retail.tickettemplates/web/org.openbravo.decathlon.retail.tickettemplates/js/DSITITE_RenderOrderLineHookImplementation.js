OB.MobileApp.model.hookManager.registerHook('OBPOS_RenderOrderLine', function (args, callbacks) {
  // execute all your logic here
  args.orderline.$.product.setContent(args.orderline.model.get('product').get('_identifier') + ' / ' + args.orderline.model.get('product').get('dsitite_modelName'));
  // This is a MUST to properly manage callbacks
  OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
});