/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global define,_,console, Backbone, enyo */


(function () {
  OB = window.OB || {};
  OB.Cache = window.OB.Cache || {};

  function buildIdentifier(obj) {
    var identifier, p = '';
    for (p in obj) {
      if (obj.hasOwnProperty(p) && obj[p]) {
        identifier += obj[p] + ';';
      }
    }
    return identifier;
  }

  function saveObj(cacheName, paramObj, value) {
    OB.Cache.dataStructure[cacheName][buildIdentifier(paramObj)] = value;
  }

  OB.Cache.dataStructure = {};

  OB.Cache.initCache = function (cacheName) {
    if (!OB.Cache.dataStructure[cacheName]) {
      OB.Cache.dataStructure[cacheName] = {};
    }
  };

  OB.Cache.putItem = function (cacheName, paramObj, value) {

    OB.Cache.initCache(cacheName);
    saveObj(cacheName, paramObj, value);
  };

  OB.Cache.getItem = function (cacheName, paramObj) {
    OB.Cache.initCache(cacheName);
    return OB.Cache.dataStructure[cacheName][buildIdentifier(paramObj)];
  };

  OB.Cache.hasItem = function (cacheName, paramObj) {
    OB.Cache.initCache(cacheName);
    return !OB.UTIL.isNullOrUndefined(OB.Cache.getItem(cacheName, paramObj));
  };
}());