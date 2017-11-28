/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global Backbone */

(function () {

  var InternalMovementLine = OB.Data.ExtensibleModel.extend({
    modelName: 'MaterialMgmtInternalMovementLine',
    entityName: 'MaterialMgmtInternalMovementLine',
    source: 'org.openbravo.mobile.warehouse.picking.datasource.OBMWHP_materialMgmtInternalMovementLine_dataSource',
    dataLimit: 300,
    isdatasource: true,
    online: true
  });

  OB.Data.Registry.registerModel(InternalMovementLine);
}());