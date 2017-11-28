/************************************************************************************ 
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
OB.OBWPL = OB.OBWPL || {};
OB.OBWPL.ManagePickingBoxContent = OB.OBWPL.ManagePickingBoxContent || {};

OB.OBWPL.ManagePickingBoxContent.onLoad = function (view) {
  OB.Utilities.Action.remove('pickingBoxLineAdded');
  OB.Utilities.Action.set('pickingBoxLineAdded', function (paramObj) {
    var form = paramObj._processView.theForm;
    form.getItem("boxContent").canvas.viewGrid.invalidateCache();
    form.getItem("boxContent").canvas.viewGrid.refreshGrid();
    form.getItem('movLineToAdd').setValue(null);
    form.getItem('qtyToAdd').setValue(null);
  });
};

OB.OBWPL.ManagePickingBoxContent.onDelete = function (grid, arg1, deletedLine) {
  OB.RemoteCallManager.call('org.openbravo.warehouse.pickinglist.actionhandler.ManagePickingBoxDeleteHandler', {
    movLineBoxId: deletedLine.id
  }, {}, function () {
    grid.invalidateCache();
    grid.refreshGrid();
  }, {}, function () {
    isc.warn(OB.I18N.getLabel('OBWPL_errorDeletingBoxContent'));;
  });
  return false;
};