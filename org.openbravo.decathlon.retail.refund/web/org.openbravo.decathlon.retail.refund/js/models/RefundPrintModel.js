/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global $ */

(function () {

  var PrintRefund = function () {
      this.printTemplateForRefunds = new OB.DS.HWResource(DSIREF.Print.RefundTemplate);
      };

      PrintRefund.prototype.print = function (arrRefunds) {

    OB.POS.hwserver.print(this.printTemplateForRefunds, {
      cashmgmt: arrRefunds
    });
  };

  DSIREF = DSIREF || {};
  DSIREF.Print = DSIREF.Print || {};
  DSIREF.Print.Refund = PrintRefund;
  DSIREF.Print.RefundTemplate = '../org.openbravo.decathlon.retail.refund/res/refundtemplate.xml';
}());