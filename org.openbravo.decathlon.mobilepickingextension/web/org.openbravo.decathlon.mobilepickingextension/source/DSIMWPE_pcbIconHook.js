/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, OBWH, _*/

OB.MobileApp.model.hookManager.registerHook('OBMWHP_renderMovementLine', function (args, callbacks) {
  var pcbFieldQtyIdentifier = 'clPcbQty',
      pcbValue;

  if (args && args.item && args.item.get('dalItems') && args.item.get('dalItems').length > 0 && args.item.get('dalItems').at(0).get(pcbFieldQtyIdentifier) && args.item.get('dalItems').at(0).get(pcbFieldQtyIdentifier) !== null) {
    pcbValue = args.item.get('dalItems').at(0).get(pcbFieldQtyIdentifier);
    if (args.item.get('neededQty') && args.item.get('neededQty') >= pcbValue && (args.item.get('neededQty') % pcbValue) === 0) {
      args.component.$.iconContainer.removeClass('hidden');
      //Custom icon
      //args.component.$.img.setAttribute('src', '../org.openbravo.decathlon.mobilepickingextension/assets/img/star-64.png');
    } else {
      if (!args.component.$.iconContainer.hasClass('hidden')) {
        args.component.$.iconContainer.addClass('hidden');
      }
    }
  } else {
    if (!args.component.$.iconContainer.hasClass('hidden')) {
      args.component.$.iconContainer.addClass('hidden');
    }
  }

  // This is a MUST to properly manage callbacks
  OB.MobileApp.model.hookManager.callbackExecutor(args, callbacks);
});