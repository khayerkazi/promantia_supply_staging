/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global B, $, _, Backbone, window, confirm, OB, console, localStorage */

(function () {
  var modelterminal = OB.POS.modelterminal,
      executeWhenDOMReady;

  // alert all errors
  window.onerror = function (e, url, line) {
    var errorInfo;
    if (typeof (e) === 'string') {
      errorInfo = e + '. Line number: ' + line + '. File uuid: ' + url + '.';
      OB.UTIL.showError(errorInfo);
      OB.error(errorInfo);
    }
  };

  modelterminal.on('ready', function () {
    var webwindow, terminal = OB.POS.modelterminal.get('terminal');

    // We are Logged !!!
    // Set Hardware..
    OB.POS.hwserver = new OB.DS.HWServer(terminal.hardwareurl, terminal.scaleurl);

    // Set Arithmetic properties:
    OB.DEC.setContext(OB.UTIL.getFirstValidValue([OB.POS.modelterminal.get('currency').obposPosprecision, OB.POS.modelterminal.get('currency').pricePrecision]), BigDecimal.prototype.ROUND_HALF_UP);

    // Set disable promotion discount property
    OB.Dal.find(OB.Model.Discount, {
      _whereClause: "where m_offer_type_id in (" + OB.Model.Discounts.getManualPromotions() + ")"
    }, function (promos) {
      if (promos.length === 0) {
        OB.POS.modelterminal.set('isDisableDiscount', true);
      } else {
        OB.POS.modelterminal.set('isDisableDiscount', false);
      }
    }, function () {
      return true;
    });


    // TODO: check permissions: this has been removed because windows are not already loaded
    // at this point
    //    webwindow = OB.MobileApp.model.windows.where({
    //      route: OB.POS.paramWindow
    //    })[0].get('windowClass');
    //
    //    if (webwindow) {
    //      if (OB.POS.modelterminal.hasPermission(OB.POS.paramWindow)) {
    //        OB.POS.navigate('retail.pointofsale');
    //      } else {
    //        OB.UTIL.showLoading(false);
    //        alert(OB.I18N.getLabel('OBPOS_WindowNotPermissions', [OB.POS.paramWindow]));
    //      }
    //    } else {
    //      OB.UTIL.showLoading(false);
    //      alert(OB.I18N.getLabel('OBPOS_WindowNotFound', [OB.POS.paramWindow]));
    //    }
    OB.MobileApp.model.hookManager.executeHooks('OBPOS_LoadPOSWindow', {}, function () {
      OB.POS.navigate('retail.pointofsale');
    });

    if (OB.POS.modelterminal.get('loggedOffline') === true) {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBPOS_OfflineLogin'));
    }

    OB.POS.hwserver.print(new OB.DS.HWResource(OB.OBPOSPointOfSale.Print.WelcomeTemplate), {});
  });

  modelterminal.on('loginsuccess', function () {
    modelterminal.load();
  });

  modelterminal.on('logout', function () {

    // Logged out. go to login window
    modelterminal.off('loginfail');

    // Redirect to login window
    localStorage.setItem('target-window', window.location.href);
    //window.location = window.location.pathname + '?terminal=' + window.encodeURIComponent(OB.POS.paramTerminal);
  });


}());