/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global enyo, OBWH*/

enyo.kind({
  name: 'OBWH.Picking.View',
  kind: 'OB.UI.WindowView',
  windowmodel: OBWH.Picking.Model,
  handlers: {
    onSelectPicking: 'selectPicking',
    onProcessPicking: 'processPicking',
    onSetQuantity: 'setPickedQtyHandler',
    onActivateTab: 'activateTabHandler',
    onRaiseIncidence: 'raiseIncidence',
    onResetIncidence: 'resetIncidence',
    onConfirmIncidence: 'confirmIncidence',
    onScan: 'scanHandler',
    onBackMenuAction: 'backMenuHandler',
    onBoxSelected: 'boxSelected'
  },
  events: {
    onShowPopup: ''
  },
  components: [{
    kind: 'OB.UI.MultiColumn',
    name: 'multiColumn',
    leftToolbar: {
      kind: 'OBWH.Picking.LeftToolbar'
    },
    leftPanel: {
      kind: 'OBWH.Picking.PickingView',
      name: 'pickingPanel'
    },
    rightToolbar: {
      kind: 'OBWH.Picking.RightToolbar'
    },
    rightPanel: {
      kind: 'OBWH.Picking.ItemView',
      name: 'rightPanel'
    }
  }, {
    kind: 'OBWH.Picking.PickingSearch',
    name: 'modalPickingSearch'
  }, {
    kind: 'OBWH.Picking.IncidenceTypeSelector',
    name: 'incidenceTypeSelector'
  }, {
    kind: 'OBWH.Picking.BoxSelector',
    name: 'boxSelector'
  }, {
    kind: 'OB.UI.ModalAddBox',
    name: 'modalAddBox'
  }],

  init: function () {
    var p, me = this;

    this.inherited(arguments);

    if (this.params) {
      p = JSON.parse(decodeURI(this.params));
      this.model.set('parameters', p);
    }

    OB.MobileApp.view.scanningFocus(false);
  },

  selectPicking: function (inSender, inEvent) {
    this.model.get('picking').reset(inEvent.picking);
  },

  setPickedQtyHandler: function (inSender, inEvent) {
    if (this.model.get('currentItem').get('status') === 'IN') {
      return;
    }
    this.model.get('currentItem').setPickedQty(inEvent.quantity, inEvent.incremental);
  },

  activateTabHandler: function (inSender, inEvent) {
    if (!this.model.get('currentItem')) {
      inEvent.tab = 'scan';
    }
    this.model.set('activeTab', inEvent.tab);
  },

  scanHandler: function (inSender, inEvent) {
    this.model.scan(inEvent.code);
  },

  processPicking: function () {
    var me = this;
    OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_ProcessTitle'), OB.I18N.getLabel('OBMWHP_ProcessText'), [{
      label: OB.I18N.getLabel('OBMOBC_LblOk'),
      action: function () {
        OB.UTIL.showLoading(true);
        me.model.processPicking();
      }
    }, {
      label: OB.I18N.getLabel('OBMOBC_LblCancel')
    }]);
  },

  raiseIncidence: function () {
    var me = this;
    if (this.model.get('currentItem').get('status') === 'RE') {
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_itemReady_header'), OB.I18N.getLabel('OBMWHP_itemReady_body'));
      return;
    }
    if (this.model.get('currentItem').get('status') === 'IN' || this.model.get('currentItem').get('status') === 'IC') {
      //ready in cache (2nd time)
      if (this.model.get('currentItem').get('incidenceObj')) {
        if (OBWH.Picking.IncidencesActions[this.model.get('currentItem').get('incidenceObj').get('incidenceType').get('baseIncidenceType').searchKey + '_show']) {
          var popupName = 'OBWH.Picking.IncidencesActions.' + this.model.get('currentItem').get('incidenceObj').get('incidenceType').get('baseIncidenceType').searchKey + '_show';
          this.doShowPopup({
            popup: popupName
          });
        } else {
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_alreadyHasIncidenceHeader'), OB.I18N.getLabel('OBMWHP_alreadyHasIncidenceBody'));
        }
      } else {
        OB.UTIL.showLoading(true);
        OB.OBMWHP.Utils.getIncidenceFromServer(this.model.get('currentItem').get('incidenceId'), function (incidence) {
          //save in cache, for the next times
          me.model.get('currentItem').set('incidenceObj', incidence);
          if (OBWH.Picking.IncidencesActions[incidence.get('incidenceType').get('baseIncidenceType').searchKey + '_show']) {
            var popupName = 'OBWH.Picking.IncidencesActions.' + incidence.get('incidenceType').get('baseIncidenceType').searchKey + '_show';
            me.doShowPopup({
              popup: popupName
            });
          } else {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_alreadyHasIncidenceHeader'), OB.I18N.getLabel('OBMWHP_alreadyHasIncidenceBody'));
          }
          OB.UTIL.showLoading(false);
          return;
        }, function (error) {
          OB.UTIL.showLoading(false);
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_alreadyHasIncidenceHeader'), OB.I18N.getLabel('OBMWHP_alreadyHasIncidenceBody'));
          return;
        });
      }
    } else {
      this.doShowPopup({
        popup: 'incidenceTypeSelector'
      });
    }
  },

  resetIncidence: function () {
    var me = this,
        process;
    if (this.model.get('currentItem').get('status') !== 'IN' && this.model.get('currentItem').get('status') !== 'IC') {
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_noIncidenceHeader'), OB.I18N.getLabel('OBMWHP_noIncidenceBody'));
      return;
    }
    process = new OB.DS.Process('org.openbravo.mobile.warehouse.picking.incidences.GetUndoIncidenceApproval');
    process.exec({
      incidenceId: this.model.get('currentItem').get('dalItems').at(0).get('obwplPickinglistproblem')
    }, enyo.bind(this, function (response, message) {
      if (response) {
        if (response.exception) {
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_ErrorGettingUndoApproval_header'), OB.I18N.getLabel('OBMWHP_ErrorGettingUndoApproval_body'));
        } else {
          if (response.allowedToUndo) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_UndoIncidence'), OB.I18N.getLabel('OBMWHP_ResetIncidenceText'), [{
              label: OB.I18N.getLabel('OBMOBC_LblUndo'),
              action: function () {
                OB.UTIL.showLoading(true);
                me.model.resetIncidence();
              }
            }, {
              label: OB.I18N.getLabel('OBMOBC_LblCancel')
            }]);
          } else {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_NotPossibleToUndoIncidence_header'), OB.I18N.getLabel('OBMWHP_NotPossibleToUndoIncidence_body'));
          }
        }
      }
    }), enyo.bind(this, function (error) {
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_ErrorGettingUndoApproval_header'), OB.I18N.getLabel('OBMWHP_ErrorGettingUndoApproval_body'));
    }));
  },

  confirmIncidence: function () {
    var me = this;
    if (this.model.get('currentItem').get('status') !== 'IN') {
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_noIncidenceHeader'), OB.I18N.getLabel('OBMWHP_noIncidenceBody'));
      return;
    }
    OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_ConfirmIncidenceHeader'), OB.I18N.getLabel('OBMWHP_ConfirmIncidenceBody'), [{
      label: OB.I18N.getLabel('OBMWHP_LblConfirmIncidence'),
      action: function () {
        OB.UTIL.showLoading(true);
        me.model.confirmIncidence();
      }
    }, {
      label: OB.I18N.getLabel('OBMOBC_LblCancel')
    }]);
  },

  backMenuHandler: function () {
    OB.MobileApp.model.navigate('wh');
    return true;
  },

  boxSelected: function (inSender, inEvent) {
    this.model.get('picking').set('currentBox', inEvent.box);
    this.waterfall('onBoxSelectedToPick', {
      box: inEvent.box
    });
    return true;
  }
});

OB.MobileApp.windowRegistry.registerWindow({
  windowClass: 'OBWH.Picking.View',
  route: 'OBMWHP_Picking',
  menuPosition: null
});