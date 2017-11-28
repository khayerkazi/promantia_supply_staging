/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, $ */

(function () {

  var RefundHeader = Backbone.Model.extend({
    dataLimit: 300
  });
  var RefundHeaderCollection = Backbone.Collection.extend({
    model: RefundHeader
  });



  window.DSIREF = window.DSIREF || {};
  window.DSIREF.Model = window.DSIREF.Model || {};
  window.DSIREF.Collection = window.DSIREF.Collection || {};

  window.DSIREF.Model.RefundHeader = RefundHeader;
  window.DSIREF.Collection.RefundHeaderCollection = RefundHeaderCollection;
}());