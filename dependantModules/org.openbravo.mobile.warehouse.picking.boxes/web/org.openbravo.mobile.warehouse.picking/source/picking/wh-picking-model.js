/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo, Backbone, _*/

OBWH.Picking = OBWH.Picking || {};

//Enable logging
OB.Model.Terminal.prototype.supportLogClient = function () {
  return true;
};

OBWH.Picking.Model = OB.Model.WindowModel.extend({
  models: [],

  processPicking: function (options) {
    var proc = new OB.DS.Process(OBWH.Picking.Model.processPickingClass),
        me = this,
        readyItems = this.get('picking').get('itemsReady'),
        modifiedItems = this.get('picking').get('itemsToSave'),
        itemsToProcess = new Backbone.Collection();


    readyItems.forEach(function (item) {
      item.get('dalItems').forEach(function (dalItem) {
        dalItem.set('oBWPLPickedqty', dalItem.get('movementQuantity'));
        dalItem.set('oBWPLItemStatus', 'CF');
        itemsToProcess.add(dalItem);
      });
      if (item.get('picking').get('picking').get('usePickingBoxes')) {
        item.generateBoxDistribution();
      }
    });

    modifiedItems.forEach(function (item) {
      var tmpPickedQty = item.get('pickedQty');
      var dalItemsCol = item.get('dalItems');
      var counter = 0;
      for (counter = 0; counter < item.get('dalItems').length; counter++) {
        var dalItemToSave = dalItemsCol.at(counter);
        if (tmpPickedQty > 0) {
          if (tmpPickedQty - dalItemToSave.get('movementQuantity') >= 0) {
            dalItemToSave.set('oBWPLPickedqty', dalItemToSave.get('movementQuantity'));
            dalItemToSave.set('oBWPLItemStatus', 'CF');
            tmpPickedQty = tmpPickedQty - dalItemToSave.get('movementQuantity');
          } else {
            dalItemToSave.set('oBWPLPickedqty', tmpPickedQty);
            dalItemToSave.set('oBWPLItemStatus', 'PE');
            tmpPickedQty = tmpPickedQty - tmpPickedQty;
          }
        } else {
          dalItemToSave.set('oBWPLPickedqty', 0);
          dalItemToSave.set('oBWPLItemStatus', 'PE');
        }

        //no send items whithout changes
        if (dalItemToSave.get('_initialQty') !== dalItemToSave.get('oBWPLPickedqty')) {
          itemsToProcess.add(dalItemToSave);
          dalItemToSave.set('_initialQty', dalItemToSave.get('oBWPLPickedqty'));
        }
      }
      if (item.get('picking').get('picking').get('usePickingBoxes')) {
        item.generateBoxDistribution();
      }
    });

    if (itemsToProcess.length === 0) {
      proc.exec({
        action: 'confirm',
        pickingId: me.get('picking').get('picking.id')
      }, function (response, message) {
        if (response && response.exception) {
          OB.UTIL.showError(response.exception.message);
          me.get('picking').reset();
          return;
        } else {
          me.get('picking').get('picking').set('pickliststatus', 'CL');
          if (options && options.nextAction) {
            me.get('picking').reset(null, {
              callback: options.nextActionFunction
            });
            return;
          }
          me.get('picking').reset();
          OB.UTIL.showSuccess('success');
        }
        OB.UTIL.showLoading(false);
      }, function () {
        me.get('picking').reset();
        OB.UTIL.showLoading(false);
      });
    } else {
      proc.exec({
        action: 'process',
        items: JSON.parse(JSON.stringify(itemsToProcess.toJSON()))
      }, function (response, message) {
        if (response && response.exception) {
          OB.UTIL.showError(response.exception.message);
          me.get('picking').reset();
          return;
        } else {
          if (me.get('picking').get('ready')) {
            me.get('picking').get('picking').set('pickliststatus', 'CL');
          }
          if (options && options.nextAction) {
            me.get('picking').reset(null, {
              callback: options.nextActionFunction
            });
            return;
          }
          me.get('picking').reset();
          OB.UTIL.showSuccess('success');
        }
        OB.UTIL.showLoading(false);
      }, function () {
        me.get('picking').reset();
        OB.UTIL.showLoading(false);
      });
    }
  },

  raiseIncidence: function (incidence, options) {
    var obj = {
      nextAction: true,
      nextActionFunction: enyo.bind(this, function (uniqueAction) {
        var proc = new OB.DS.Process(OBWH.Picking.Model.processPickingClass),
            me = this,
            currentItem = this.get('currentItem'),
            items = currentItem.get('dalItems'),
            tmp = currentItem.get('pickedQty'),
            counter;

        if (uniqueAction) {
          if (currentItem.get('picking').get('picking').get('usePickingBoxes')) {
            for (counter = 0; counter < currentItem.get('dalItems').length; counter++) {
              var dalItemToSave = items.at(counter);
              if (tmp > 0) {
                dalItemToSave.set('oBWPLPickedqty', tmp);
                tmp = tmp - tmp;
              } else {
                dalItemToSave.set('oBWPLPickedqty', 0);
              }
            }
            currentItem.generateBoxDistribution();
          }
        }

        proc.exec({
          action: 'incidence',
          incidence: incidence,
          items: JSON.parse(JSON.stringify(items.toJSON())),
          pickedQty: currentItem.get('pickedQty'),
          neededQty: currentItem.get('neededQty')
        }, function (response, message) {
          if (response && response.exception) {
            OB.UTIL.showError(response.exception.message);
            me.get('picking').reset();
          } else {
            me.get('picking').reset();
          }
          if (options && options.callback) {
            options.callback(response);
          }
          OB.UTIL.showLoading(false);
        }, function () {
          me.get('picking').reset();
          OB.UTIL.showLoading(false);
        });
      })
    };

    if ((this.get('picking').get('itemsReady').length === 0 && this.get('picking').get('itemsToSave').length === 0) || (this.get('picking').get('itemsReady').length === 0 && this.get('picking').get('itemsToSave').length === 1 && this.get('picking').get('itemsToSave').at(0) === this.get('currentItem'))) {
      //raise incidence
      obj.nextActionFunction(true);
    } else {
      //save and then raise incidence
      this.processPicking(obj);
    }
  },

  resetIncidence: function () {
    var proc = new OB.DS.Process(OBWH.Picking.Model.processPickingClass),
        me = this,
        currentItem = this.get('currentItem'),
        items = currentItem.get('dalItems');

    proc.exec({
      action: 'resetincidence',
      items: JSON.parse(JSON.stringify(items.toJSON()))
    }, function (response, message) {
      if (response && response.exception) {
        OB.UTIL.showError(response.exception.message);
      } else {
        OB.UTIL.showSuccess('success');
        me.get('picking').reset();
      }
      OB.UTIL.showLoading(false);
    }, function () {
      me.get('picking').reset();
      OB.UTIL.showLoading(false);
    });
  },

  confirmIncidence: function () {
    var proc = new OB.DS.Process(OBWH.Picking.Model.processPickingClass),
        me = this,
        currentItem = this.get('currentItem'),
        items = currentItem.get('dalItems');

    proc.exec({
      action: 'confirmincidence',
      items: JSON.parse(JSON.stringify(items.toJSON()))
    }, function (response, message) {
      if (response && response.exception) {
        OB.UTIL.showError(response.exception.message);
      } else {
        OB.UTIL.showSuccess('success');
        me.get('picking').reset();
      }
      OB.UTIL.showLoading(false);
    }, function () {
      me.get('picking').reset();
      OB.UTIL.showLoading(false);
    });
  },

  createBox: function (boxToAdd, options) {
    var proc = new OB.DS.Process(OBWH.Picking.Model.processPickingClass),
        me = this;
    proc.exec({
      action: 'addBox',
      pickingId: this.get('picking').get('picking').get('id'),
      items: JSON.parse(JSON.stringify(boxToAdd))
    }, function (response, message) {
      if (response && response.exception) {
        OB.UTIL.showError(response.exception.message);
      } else {
        me.get('picking').get('boxes').add(response.boxes);
      }
      if (options && options.callback) {
        options.callback(response);
      }
    }, function () {
      OB.error('unknow error while adding boxes');
    });
  },

  scan: function (code) {
    var me = this,
        picking = this.get('picking'),
        currentItem, initialCurrentItem = null;
    OB.UTIL.showWarning(code + " scaned");
    var proc = new OB.DS.Process(OBWH.Movement.Model.scanHandlerClass);
    if (OB.MobileApp.model.get('permissions').scanusingselectedline) {
      if (this && this.get('currentItem') && this.get('currentItem').get('attributeSetValue')) {
        initialCurrentItem = {
          attributeSetValue: this.get('currentItem').get('attributeSetValue'),
          attributeSetValueName: this.get('currentItem').get('attributeSetValueName'),
          itemId: this.get('currentItem').get('id'),
          productId: this.get('currentItem').get('product')
        };
      } else {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_lineSelectWhenScan_header'), OB.I18N.getLabel('OBMWHP_lineSelectWhenScan_body'));
        return;
      }
    }
    proc.exec({
      code: code,
      line: {
        binids: picking.get('locators').getLocatorsIds().toString(),
        item: initialCurrentItem
      },
      eventName: OBWH.Picking.Model.scanEvent
    }, enyo.bind(this, function (response, message) {
      var items;
      if (response.exception) {
        OB.UTIL.showError(response.exception.message);
        return;
      }

      if (response.product) {
        if (response.product.itemId) {
          currentItem = picking.get('items').get(response.product.itemId);
        } else {
          items = picking.get('items');
          currentItem = _.find(items.models, function (item) {
            return item.get('product') === response.product.id && item.get('status') === 'PE';
          });
        }

        if (currentItem) {
          me.set('currentItem', currentItem);
          currentItem.setPickedQty(Number(1), true, true);
        }
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBWH_SacannedProduct', [response.product.name]));
      }
    }), function () {
      window.console.error('error');
    });
  },

  init: function () {
    var pickStatusCrit, boxes, relatedOrders, picking, items, itemsReady, itemsToSave, itemsDone, locators, pickingLinesPageManager, itemsToDraw, linesPerPage = 20;
    if (OB && OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('permissions') && OB.MobileApp.model.get('permissions').OBMWHP_PickingLinesPerPage) {
      var tmpLinesPerPage = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_PickingLinesPerPage, 10);
      if (_.isNumber(tmpLinesPerPage) && !_.isNaN(tmpLinesPerPage)) {
        linesPerPage = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_PickingLinesPerPage, 10);
      }
    }
    OB.Data.Registry.registerModel('OBWPL_pickinglist');
    OB.Data.Registry.registerModel('OBWPL_pickinglistproblem');
    OB.Data.Registry.registerModel('DocumentType');
    OB.Data.Registry.registerModel('OBWPL_plbox');
    OB.Data.Registry.registerModel('OBWPL_plBoxContent');

    items = new OBWH.Picking.Model.Items();
    boxes = new OB.Collection.OBWPL_plboxList();
    relatedOrders = new OB.Collection.OrderList();
    itemsReady = new OBWH.Picking.Model.Items();
    itemsToSave = new OBWH.Picking.Model.Items();
    itemsDone = new OBWH.Picking.Model.Items();
    locators = new OBWH.Picking.Model.Locators();
    picking = new OBWH.Picking.Model.Picking({
      windowModel: this,
      boxes: boxes,
      relatedOrders: relatedOrders,
      items: items,
      itemsReady: itemsReady,
      itemsToSave: itemsToSave,
      itemsDone: itemsDone,
      locators: locators
    });
    itemsToDraw = new OBWH.Picking.Model.Locators();
    pickingLinesPageManager = new OBWH.Picking.Model.PageManager(picking, locators, itemsToDraw, linesPerPage);
    this.set('picking', picking);
    this.set('pickingLinesPageManager', pickingLinesPageManager);
    this.set('activeTab', 'scan');

    picking.on('maybeReady', function () {
      var REItems, INItems;
      REItems = _.filter(items.models, function (itm) {
        return itm.get('status') === 'PE';
      }, this);

      INItems = _.filter(items.models, function (itm) {
        return itm.get('status') === 'IN';
      }, this);

      if ((_.isUndefined(REItems) || _.isNull(REItems) || REItems.length === 0) && (_.isUndefined(INItems) || _.isNull(INItems) || INItems.length === 0)) {

        if (picking.get('picking').get('pickliststatus') === 'CL') {
          picking.set('ready', false);
          picking.set('readyToSave', false);
        } else {
          picking.set('ready', true);
        }
      } else {
        picking.set('ready', false);
        picking.set('readyToSave', (itemsReady.length !== 0 || itemsToSave.length !== 0));
      }
    }, this);

    items.on('add remove', function () {
      picking.set('totalItems', items.length);
    });
    itemsReady.on('remove', function () {
      picking.set('ready', false);
      picking.set('readyToSave', (itemsReady.length !== 0 || itemsToSave.length !== 0));
    }, this);

    itemsReady.on('add', function () {
      var REItems, INItems;
      REItems = _.filter(items.models, function (itm) {
        return itm.get('status') === 'PE';
      }, this);

      INItems = _.filter(items.models, function (itm) {
        return itm.get('status') === 'IN';
      }, this);

      if ((_.isUndefined(REItems) || _.isNull(REItems) || REItems.length === 0) && (_.isUndefined(INItems) || _.isNull(INItems) || INItems.length === 0)) {
        picking.set('ready', true);
      } else {
        picking.set('ready', false);
        picking.set('readyToSave', (itemsReady.length !== 0 || itemsToSave.length !== 0));
      }
    });
    itemsToSave.on('add remove', function () {
      picking.set('readyToSave', (itemsReady.length !== 0 || itemsToSave.length !== 0));
    }, this);

    items.on('selected', function (item) {
      this.set('currentItem', item);
      this.set('activeTab', 'edit');
    }, this);

    // Load picking status names
    pickStatusCrit = {
      _where: 'e.reference.id = \'3F698D2435774CFAB5B850C7686E47F4\''
    };
    OB.Data.Registry.registerModel('ADList');
    OB.Dal.find(OB.Model.ADList, pickStatusCrit, enyo.bind(this, function (values) {
      enyo.forEach(values.models, function (model) {
        OBWH.Picking.Model.PickingStatus[model.get('searchKey')] = model.get('name');
      });
    }));

    // Load item status names
    pickStatusCrit = {
      _where: 'e.reference.id = \'87D064043A36446B8303A37C0EC929C3\''
    };
    OB.Dal.find(OB.Model.ADList, pickStatusCrit, enyo.bind(this, function (values) {
      enyo.forEach(values.models, function (model) {
        OBWH.Picking.Model.ItemStatus[model.get('searchKey')] = model.get('name');
      });
      OBWH.Picking.Model.ItemStatus.RE = OB.I18N.getLabel('OBMWHP_ItemStatusRE');
    }));
  }
});

OBWH.Picking.Model.Item = Backbone.Model.extend({
  setItem: function (dalItem) {
    //iterate boxes. If no boxes we will not do nothing
    _.each(dalItem.get('boxes'), function (boxQty, key) {
      if (OB.UTIL.isNullOrUndefined(this.get('boxes')[key])) {
        this.get('boxes')[key] = boxQty;
      } else {
        this.get('boxes')[key] += boxQty;
      }
    }, this);
    //we don't need this data any more because we manage it in the base item
    dalItem.set('boxes', {});
    // orders distribution
    if (OB.UTIL.isNullOrUndefined(this.get('byOrders'))) {
      this.set('byOrders', {});
    }
    if (dalItem.get('salesOrderId')) {
      if (OB.UTIL.isNullOrUndefined(this.get('byOrders')[dalItem.get('salesOrderId')])) {
        this.get('byOrders')[dalItem.get('salesOrderId')] = {};
        this.get('byOrders')[dalItem.get('salesOrderId')].order = this.get('picking').get('relatedOrders').get(dalItem.get('salesOrderId')).toJSON();
        this.get('byOrders')[dalItem.get('salesOrderId')].quantity = Number(dalItem.get('movementQuantity'));
      } else {
        this.get('byOrders')[dalItem.get('salesOrderId')].quantity = Number(this.get('byOrders')[dalItem.get('salesOrderId')].quantity) + Number(dalItem.get('movementQuantity'));
      }
    } else {
      if (OB.UTIL.isNullOrUndefined(this.get('byOrders')._undefined)) {
        this.get('byOrders')._undefined = {};
        this.get('byOrders')._undefined.quantity = Number(dalItem.get('movementQuantity'));
      } else {
        this.get('byOrders')._undefined.quantity = Number(this.get('byOrders')._undefined.quantity) + Number(dalItem.get('movementQuantity'));
      }
    }
    //This mechanism will detect if the same movement line has been retrieved from the server twice
    if (!(OB.UTIL.isNullOrUndefined(this.get('dalItems').get(dalItem.id)))) {
      if (OB.UTIL.isNullOrUndefined(window.duplicatedDetected)) {
        window.duplicatedDetected = true;
        OB.UTIL.showConfirmation.display('Duplicated data detected!', 'The app must be refreshed. Please press -Accept- button to continue', null, {
          autoDismiss: false,
          onHideFunction: function () {
            window.duplicatedDetected = null;
            OB.MobileApp.model.navigate('wh');
          }
        });
      }
    }
    this.get('dalItems').add(dalItem);
    this.set('neededQty', Number(this.get('neededQty')) + Number(dalItem.get('movementQuantity')));
    this.set('pickedQty', Number(this.get('pickedQty')) + Number(dalItem.get('oBWPLPickedqty')));
  },
  setPickedQty: function (pickedQty, incremental, fromScan) {
    var maxQty = Number(this.get('neededQty')),
        curPickedQty = this.get('pickedQty'),
        diffPicked = 0,
        origScannedValue;

    if (fromScan && OB.MobileApp.model.get('permissions').OBMWHP_DisableLineUntillScan && this.get('scanned') === false) {
      origScannedValue = this.get('scanned');
      this.set('scanned', true);
    }

    if (OB.MobileApp.model.get('permissions').OBMWHP_DisableLineUntillScan) {
      if (this.get('scanned') === false) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_scanIsNeeded_Header'), OB.I18N.getLabel('OBMWHP_scanIsNeeded_Body'));
        return;
      }
    }

    if (incremental) {
      pickedQty = this.get('pickedQty') + Number(pickedQty);
    }
    if (Number(pickedQty) > maxQty) {
      pickedQty = maxQty;
    } else if (Number(pickedQty) < 0) {
      pickedQty = 0;
    }

    diffPicked = pickedQty - curPickedQty;

    if (this.get('picking').get('picking').get('usePickingBoxes')) {
      if (this.get('picking').get('currentBox')) {
        if (!OB.UTIL.isNullOrUndefined(this.get('boxes')[this.get('picking').get('currentBox').get('id')])) {
          if (diffPicked > 0) {
            this.get('boxes')[this.get('picking').get('currentBox').get('id')] += diffPicked;
          } else {
            if (this.get('boxes')[this.get('picking').get('currentBox').get('id')] + diffPicked >= 0) {
              this.get('boxes')[this.get('picking').get('currentBox').get('id')] += diffPicked;
            } else {
              OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_NotPossibleToUnpackHeader'), OB.I18N.getLabel('OBMWHP_NotPossibleToUnpackBody', [-(diffPicked), this.get('picking').get('currentBox').get('boxNumber'), (this.get('boxes')[this.get('picking').get('currentBox').get('id')] ? this.get('boxes')[this.get('picking').get('currentBox').get('id')] : '0')]));
              if (origScannedValue === true || origScannedValue === false) {
                this.set('scanned', origScannedValue);
              }
              return false;
            }
          }
        } else {
          if (diffPicked < 0) {
            this.get('boxes')[this.get('picking').get('currentBox').get('id')] = 0;
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_NotPossibleToUnpackHeader'), OB.I18N.getLabel('OBMWHP_NotPossibleToUnpackBody', [-(diffPicked), this.get('picking').get('currentBox').get('boxNumber'), (this.get('boxes')[this.get('picking').get('currentBox').get('id')] ? this.get('boxes')[this.get('picking').get('currentBox').get('id')] : '0')]));
            if (origScannedValue === true || origScannedValue === false) {
              this.set('scanned', origScannedValue);
            }
            return false;
          } else {
            this.get('boxes')[this.get('picking').get('currentBox').get('id')] = diffPicked;
          }
        }
      } else {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_BoxNotSelectedHeader'), OB.I18N.getLabel('OBMWHP_BoxNotSelectedBody'));
        if (origScannedValue === true || origScannedValue === false) {
          this.set('scanned', origScannedValue);
        }
        return false;
      }
    }

    this.set('pickedQty', pickedQty);
    if (pickedQty === maxQty) {
      this.set('status', 'RE');
      this.get('picking').get('itemsReady').add(this);
      this.get('picking').get('itemsToSave').remove(this);
    } else {
      this.set('status', 'PE');
      this.get('picking').get('itemsReady').remove(this);
      this.get('picking').get('itemsToSave').add(this);
    }
    this.trigger('pickedQtyChanged', this);

    //TODO: Preference, move to next collection, move to next page
    if (this.get('status') === 'RE' && OB.MobileApp.model.hasPermission('OBMWHP_autoMoveToNextLineWhenFinished', true)) {
      this.get('picking').get('windowModel').get('pickingLinesPageManager').moveToNextItem(this);
    }
  },
  generateBoxDistribution: function () {
    var lineCounter = 0,
        pendingToPack;
    _.each(this.get('boxes'), function (qty, id) {
      if (qty === 0) {
        _.each(this.get('dalItems').models, function (dalItem) {
          dalItem.get('boxes')[id] = 0;
        }, this);
      } else {
        pendingToPack = qty;
        while (pendingToPack > 0) {
          var curDalItem = this.get('dalItems').at(lineCounter);
          var pickedQty = OB.UTIL.isNullOrUndefined(curDalItem.get('_pendingToPack')) ? curDalItem.get('oBWPLPickedqty') : curDalItem.get('_pendingToPack');
          if (pickedQty <= pendingToPack) {
            //put total amount into box and continue to the next line until box amount is reached.
            if (OB.UTIL.isNullOrUndefined(curDalItem.get('boxes'))) {
              curDalItem.set('boxes', {});
            }
            if (OB.UTIL.isNullOrUndefined(curDalItem.get('boxes')[id])) {
              curDalItem.get('boxes')[id] = pickedQty;
            } else {
              curDalItem.get('boxes')[id] += pickedQty;
            }
            if (curDalItem.get('boxes')[id] > curDalItem.get('oBWPLPickedqty')) {
              OB.UTIL.showConfirmation.display('Error', 'More quantity (' + curDalItem.get('boxes')[id] + ') than picked quantity (' + curDalItem.get('oBWPLPickedqty') + ') is present in box [' + this.get('picking').get('boxes').get(id).get('_identifier') + ']. Applicattion will be reloaded', [{
                label: OB.I18N.getLabel('OBMOBC_LblOk'),
                action: function () {
                  OB.MobileApp.model.navigate('wh');
                }
              }], {
                onShowFunction: function (dialog) {
                  setTimeout(function () {
                    dialog.applyStyle('z-index', 1000);
                  }, 200);
                },
                onHideFunction: function () {
                  OB.MobileApp.model.navigate('wh');
                }
              });
              throw 'More quantity (' + curDalItem.get('boxes')[id] + ') than picked quantity (' + curDalItem.get('oBWPLPickedqty') + ') is present in box [' + this.get('picking').get('boxes').get(id).get('_identifier') + ']. Applicattion will be reloaded';
            }
            curDalItem.unset('_pendingToPack');
            pendingToPack -= pickedQty;
            lineCounter += 1;
          } else {
            //box empty but we need to process this line again for next box
            if (OB.UTIL.isNullOrUndefined(curDalItem.get('boxes')[id])) {
              curDalItem.get('boxes')[id] = pendingToPack;
            } else {
              curDalItem.get('boxes')[id] += pendingToPack;
            }
            if (curDalItem.get('boxes')[id] > curDalItem.get('oBWPLPickedqty')) {
              OB.UTIL.showConfirmation.display('Error', 'More quantity (' + curDalItem.get('boxes')[id] + ') than picked quantity (' + curDalItem.get('oBWPLPickedqty') + ') is present in box [' + this.get('picking').get('boxes').get(id).get('_identifier') + ']. Applicattion will be reloaded', [{
                label: OB.I18N.getLabel('OBMOBC_LblOk'),
                action: function () {
                  OB.MobileApp.model.navigate('wh');
                }
              }], {
                onShowFunction: function (dialog) {
                  setTimeout(function () {
                    dialog.applyStyle('z-index', 1000);
                  }, 200);
                },
                onHideFunction: function () {
                  OB.MobileApp.model.navigate('wh');
                }
              });
              throw 'More quantity (' + curDalItem.get('boxes')[id] + ') than picked quantity (' + curDalItem.get('oBWPLPickedqty') + ') is present in box [' + this.get('picking').get('boxes').get(id).get('_identifier') + ']. Applicattion will be reloaded';
            }
            curDalItem.set('_pendingToPack', pickedQty - pendingToPack);
            pendingToPack = 0;
          }
        }
      }
    }, this);
  },
  initialize: function () {
    //this.set('pickedQty', Number(0));
    this.set('neededQty', Number(0));
    this.set('dalItems', new Backbone.Collection());
    if (this.get('status') === 'CO' || this.get('status') === 'CF') {
      this.set('done', true);
    }
  }
});

OBWH.Picking.Model.Items = Backbone.Collection.extend({
  model: OBWH.Picking.Model.Item
});

OBWH.Picking.Model.Locator = Backbone.Model.extend({
  initialize: function () {
    this.set('items', new OBWH.Picking.Model.Items());
  }
});

OBWH.Picking.Model.Locators = Backbone.Collection.extend({
  model: OBWH.Picking.Model.Locator,
  getLocatorsIds: function () {
    var result = [];
    _.each(this.models, function (lctr) {
      result.push(lctr.get('id'));
    }, this);
    return result;
  }
});

OBWH.Picking.Model.Picking = Backbone.Model.extend({
  reset: function (dalpicking, options) {
    var me = this,
        criteria;
    dalpicking = dalpicking || this.get('picking');
    this.set('picking', dalpicking);
    this.set('picking.id', dalpicking.get('id'));
    this.set('picking.name', dalpicking.get('_identifier'));
    this.set('ready', false);
    this.set('readyToSave', false);
    this.get('items').reset();
    this.get('itemsReady').reset();
    this.get('itemsToSave').reset();
    this.get('itemsDone').reset();
    this.get('locators').reset();
    this.set('totalItems', Number(0));
    this.set('done', Number(0));
    criteria = {
      _where: 'e.id = \'' + dalpicking.get('documentType') + '\''
    };
    OB.Dal.find(OB.Model.DocumentType, criteria, enyo.bind(this, function (values) {
      me.set('isGrouping', values.at(0).get('oBWPLIsGroup'));
      me.loadItems(options);
    }));
  },
  loadItems: function (options) {
    var items = this.get('items'),
        criteria = {},
        me = this,
        limit = 1000,
        movLineRequest = {},
        movLineRequestParams = {},
        relatedOrdersRequest = {},
        relatedOrdersRequestParams = {},
        tmpLimit;
    //Measure times
    var startGetLines, endGetLines;
    var startGetRelatedOrders, endGetRelatedOrders;
    var startGetBoxes, endGetBoxes;
    var startGetBoxesCont, endGetBoxesCont;
    var startClientSideProcess, endClientSideProcess; /*end times*/
    //end measure times
    if (OB && OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('permissions') && OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit) {
      tmpLimit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit, 10);
      if (_.isNumber(tmpLimit) && !_.isNaN(tmpLimit)) {
        limit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit, 10);
      }
    } else if (OB && OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('permissions') && OB.MobileApp.model.get('permissions').OBMWHP_LimitRecord) {
      tmpLimit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_LimitRecord, 10);
      if (_.isNumber(tmpLimit) && !_.isNaN(tmpLimit)) {
        limit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_LimitRecord, 10);
      }
      OB.warn("Preference -OBMWHP_LimitRecord- is deprecated. Please use -OBMWHP_MovementsLinesLimit- instead");
    }
    OB.UTIL.showLoading(true);
    items.reset();
    //Get mov lines
    if (this.get('isGrouping') === true) {
      movLineRequestParams.oBWPLGroupPickinglistId = this.get('picking.id');
    } else {
      movLineRequestParams.oBWPLWarehousePickingListId = this.get('picking.id');
    }
    movLineRequestParams._limit = limit;
    movLineRequest = new OB.DS.Request(OB.Model.MaterialMgmtInternalMovementLine);
    startGetLines = new Date().getTime();
    movLineRequest.exec(movLineRequestParams, enyo.bind(this, function (values) {
      endGetLines = new Date().getTime();
      OB.info("[" + me.get('picking.name') + "] " + "Time to retrieve Movement Lines: " + (endGetLines - startGetLines));
      if (values && values.exception) {
        var msg = values.exception.message || 'Unknown error';
        OB.UTIL.showConfirmation.display('Error', "Error retrieving picking lines: " + msg, [{
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          action: function () {
            OB.MobileApp.model.navigate('wh');
          }
        }]);
      }
      values = new OB.Collection.MaterialMgmtInternalMovementLineList(values);

      //get related orders info
      relatedOrdersRequestParams = movLineRequestParams;
      relatedOrdersRequest = new OB.DS.Request(OB.Model.Order);
      startGetRelatedOrders = new Date().getTime();
      relatedOrdersRequest.exec(relatedOrdersRequestParams, enyo.bind(this, function (valuesRelatedOrders) {
        endGetRelatedOrders = new Date().getTime();
        OB.info("[" + me.get('picking.name') + "] " + "Time to retrieve Related Orders: " + (endGetRelatedOrders - startGetRelatedOrders));
        if (valuesRelatedOrders && valuesRelatedOrders.exception) {
          var msg = valuesRelatedOrders.exception.message || 'Unknown error';
          OB.UTIL.showConfirmation.display('Error', "Error retrieving related orders: " + msg, [{
            label: OB.I18N.getLabel('OBMOBC_LblOk'),
            action: function () {
              OB.MobileApp.model.navigate('wh');
            }
          }]);
        }
        valuesRelatedOrders = new OB.Collection.OrderList(valuesRelatedOrders);
        this.get('relatedOrders').reset(valuesRelatedOrders.models);
        if (this.get('picking').get('usePickingBoxes')) {
          var boxesCrit = {},
              boxContentCrit = {};
          boxesCrit._where = 'e.obwplPickinglist.id =\'' + this.get('picking').get('id') + '\'';
          boxesCrit._limit = 100;
          boxesCrit._sortBy = 'boxno ASC';
          startGetBoxes = new Date().getTime();
          OB.Dal.find(OB.Model.OBWPL_plbox, boxesCrit, enyo.bind(this, function (boxesValues) {
            endGetBoxes = new Date().getTime();
            OB.info("[" + me.get('picking.name') + "] " + "Time to retrieve picking list Boxes: " + (endGetBoxes - startGetBoxes));
            this.get('boxes').reset(boxesValues.models);
            boxContentCrit._where = 'e.obwplPlbox.obwplPickinglist.id =\'' + this.get('picking').get('id') + '\'';
            boxContentCrit._limit = 1000;
            var tmpLimit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit, 10);
            if (_.isNumber(tmpLimit) && !_.isNaN(tmpLimit)) {
              boxContentCrit._limit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit, 10);
            }
            startGetBoxesCont = new Date().getTime();
            OB.Dal.find(OB.Model.OBWPL_plBoxContent, boxContentCrit, enyo.bind(this, function (boxContentValues) {
              endGetBoxesCont = new Date().getTime();
              OB.info("[" + me.get('picking.name') + "] " + "Time to retrieve box content: " + (endGetBoxesCont - startGetBoxesCont));
              startClientSideProcess = new Date().getTime();
              OB.times = OB.times || {};
              OB.times.addItem = 0;
              OB.times.addItemDone = 0;
              OB.times.addBox = 0;
              OB.times.executeNewItem = 0;
              OB.times.setItem = 0;
              OB.times.executeAddLocator = 0;
              OB.times.executeNewLocator = 0;
              OB.times.executeAddBaseItemToLocators = 0;
              enyo.forEach(values.models, function (model) {
                me.addItem(model, boxContentValues);
              });
              endClientSideProcess = new Date().getTime();
              OB.info("[" + me.get('picking.name') + "] " + "0.Total time grouping in the client side: " + (endClientSideProcess - startClientSideProcess));
              OB.info("[" + me.get('picking.name') + "] " + "1.Total Time to create instances of new baseItem: " + OB.times.executeNewItem);
              OB.info("[" + me.get('picking.name') + "] " + "2.Total Time to create instances of new Locator: " + OB.times.executeNewLocator);
              OB.info("[" + me.get('picking.name') + "] " + "3.Total Time to add Locator to locators collection: " + OB.times.executeAddLocator);
              OB.info("[" + me.get('picking.name') + "] " + "4.Total Time to add Base Item to sepecific Locator items collection: " + OB.times.executeAddBaseItemToLocators);
              OB.info("[" + me.get('picking.name') + "] " + "5.Total Time to add Base Item to the list of done items: " + OB.times.addItemDone);
              OB.info("[" + me.get('picking.name') + "] " + "6.Total Time to add Base Item to the list of items: " + OB.times.addItem);
              OB.info("[" + me.get('picking.name') + "] " + "7.Total Time to search the box content related to the current dalItem: " + OB.times.addBox);
              OB.info("[" + me.get('picking.name') + "] " + "8.Total Time to set dalItems into base Item: " + OB.times.setItem);
              OB.UTIL.showLoading(false);
              me.set('done', this.get('itemsDone').length);
              me.trigger('itemsLoaded');
              if (options && options.callback) {
                options.callback();
              }
              me.trigger('maybeReady');
              if (this.get('itemsDone').length === this.get('items').length) {
                OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_DoneTitle'), OB.I18N.getLabel('OBMWHP_DoneText'), [{
                  label: OB.I18N.getLabel('OBMOBC_LblOk'),
                  action: function () {
                    OB.MobileApp.model.navigate('wh');
                  }
                }, {
                  label: OB.I18N.getLabel('OBMOBC_LblCancel')
                }]);
              }
            }));
          }));
        } else {
          enyo.forEach(values.models, function (model) {
            me.addItem(model);
          });
          this.get('boxes').reset([]);
          OB.UTIL.showLoading(false);
          me.set('done', this.get('itemsDone').length);
          me.trigger('itemsLoaded');
          if (options && options.callback) {
            options.callback();
          }
          me.trigger('maybeReady');
          if (this.get('itemsDone').length === this.get('items').length) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMWHP_DoneTitle'), OB.I18N.getLabel('OBMWHP_DoneText'), [{
              label: OB.I18N.getLabel('OBMOBC_LblOk'),
              action: function () {
                OB.MobileApp.model.navigate('wh');
              }
            }, {
              label: OB.I18N.getLabel('OBMOBC_LblCancel')
            }]);
          }
        }
      }));
    }));
  },
  addItem: function (dalItem, boxContent) {
    var dalItemId, baseItem, locator, boxes;
    //Time measure
    var startItemNew, endItemNew;
    var startLocatorNew, endLocatorNew;
    var startAddLocator, endAddLocator;
    var startAddBaseItemToLocators, endAddBaseItemToLocators;
    var startAddDone, endAddDone;
    var startAdd, endAdd;
    var startCheckBox, endCheckBox;
    var startSetItem, endSetItem;
    OB.times = OB.times || {};
    OB.times.addItem = OB.times.addItem || 0;
    OB.times.addItemDone = OB.times.addItemDone || 0;
    OB.times.addBox = OB.times.addBox || 0;
    OB.times.executeNewItem = OB.times.executeNewItem || 0;
    OB.times.setItem = OB.times.setItem || 0;
    OB.times.executeAddLocator = OB.times.executeAddLocator || 0;
    OB.times.executeNewLocator = OB.times.executeNewLocator || 0;
    OB.times.executeAddBaseItemToLocators = OB.times.executeAddBaseItemToLocators || 0;
    //end time measure
    dalItemId = dalItem.get('product') + '$' + dalItem.get('storageBin') + '$' + dalItem.get('attributeSetValue') + '$' + dalItem.get('oBWPLItemStatus');
    if (dalItem.get('oBWPLItemStatus') === 'IN' || dalItem.get('oBWPLItemStatus') === 'IC') {
      dalItemId = dalItemId + '$' + dalItem.get('obwplPickinglistproblem');
    }
    baseItem = this.get('items').get(dalItemId);
    dalItem.set('_initialQty', dalItem.get('oBWPLPickedqty'));
    if (!baseItem) {
      startItemNew = new Date().getTime();
      baseItem = new OBWH.Picking.Model.Item({
        picking: this,
        product: dalItem.get('product'),
        productName: dalItem.get('product$_identifier'),
        storageBin: dalItem.get('storageBin'),
        storageBinName: dalItem.get('storageBin$_identifier'),
        attributeSetValue: dalItem.get('attributeSetValue'),
        attributeSetValueName: dalItem.get('attributeSetValue$_identifier'),
        uom: dalItem.get('uOM$_identifier'),
        status: dalItem.get('oBWPLItemStatus'),
        scanned: false,
        incidenceId: dalItem.get('obwplPickinglistproblem'),
        //calculated after
        pickedQty: 0,
        boxes: {},
        id: dalItemId
      });
      endItemNew = new Date().getTime();
      OB.times.executeNewItem += (endItemNew - startItemNew);
      locator = this.get('locators').get(dalItem.get('storageBin'));
      if (!locator) {
        startLocatorNew = new Date().getTime();
        locator = new OBWH.Picking.Model.Locator({
          picking: this,
          name: dalItem.get('storageBin$_identifier'),
          id: dalItem.get('storageBin')
        });
        endLocatorNew = new Date().getTime();
        OB.times.executeNewLocator += (endLocatorNew - startLocatorNew);
        startAddLocator = new Date().getTime();
        this.get('locators').add(locator);
        endAddLocator = new Date().getTime();
        OB.times.executeAddLocator += (endAddLocator - startAddLocator);
      }
      startAddBaseItemToLocators = new Date().getTime();
      locator.get('items').add(baseItem);
      endAddBaseItemToLocators = new Date().getTime();
      OB.times.executeAddBaseItemToLocators += (endAddBaseItemToLocators - startAddBaseItemToLocators);
      if (baseItem.get('status') === 'CO' || baseItem.get('status') === 'CF' || baseItem.get('status') === 'CWI') {
        startAddDone = new Date().getTime();
        this.get('itemsDone').add(baseItem);
        endAddDone = new Date().getTime();
        OB.times.addItemDone += (endAddDone - startAddDone);
      }
      startAdd = new Date().getTime();
      this.get('items').add(baseItem);
      endAdd = new Date().getTime();
      OB.times.addItem += (endAdd - startAdd);
    }

    startCheckBox = new Date().getTime();
    if (boxContent) {
      dalItem.set('boxes', {});
      _.each(boxContent.where({
        movementLine: dalItem.id
      }), function (box) {
        dalItem.get('boxes')[box.get('obwplPlbox')] = box.get('quantity');
      }, this);
    }
    endCheckBox = new Date().getTime();
    OB.times.addBox += (endCheckBox - startCheckBox);
    startSetItem = new Date().getTime();
    baseItem.setItem(dalItem);
    endSetItem = new Date().getTime();
    OB.times.setItem += (endSetItem - startSetItem);
  }
});

OBWH.Picking.Model.PickingStatus = {};
OBWH.Picking.Model.ItemStatus = {};
OBWH.Picking.Model.scanEvent = 'OBMWHP_Picking';
OBWH.Picking.Model.processPickingClass = 'org.openbravo.mobile.warehouse.picking.ProcessPicking';
OBWH.Picking.Model.getPickingWhereClause = function (qry) {
  var limit = 1000;
  var identifier, criteria;

  if (OB && OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('permissions') && OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit) {
    var tmpLimit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit, 10);
    if (_.isNumber(tmpLimit) && !_.isNaN(tmpLimit)) {
      limit = parseInt(OB.MobileApp.model.get('permissions').OBMWHP_MovementsLinesLimit, 10);
    }
  }

  criteria = {
    _where: 'e.pickliststatus IN (\'AS\', \'IP\', \'IN\', \'CO\') AND e.userContact.id = \'' + OB.MobileApp.model.usermodel.id + '\' AND e.materialMgmtInternalMovementLineEMOBWPLWarehousePickingListList.size <= ' + limit
  };

  if (qry) {
    identifier = OB.Constants.FIELDSEPARATOR + OB.Constants.IDENTIFIER;

    criteria = _.extend(criteria, {
      _OrExpression: true,
      operator: 'or',
      _constructor: 'AdvancedCriteria',
      criteria: [{
        'fieldName': 'documentNo',
        'operator': 'iContains',
        'value': qry
      }]
    });
  }
  criteria._sortBy = 'updated DESC, pickliststatus DESC, documentdate';
  return criteria;
};

OBWH.Picking.Model.PageManager = Backbone.Model.extend({
  initialize: function (parentObject, baseCollection, paginatedCollection, linesPerPage) {
    this.set('parentObject', parentObject);
    this.set('originalCollection', baseCollection);
    this.set('paginatedCollection', paginatedCollection);
    this.set('linesPerPage', linesPerPage);
    this.set('currentPage', null);
    this.set('pages', 0);
    this.set('pagesMapping', []);

    this.get('parentObject').on('itemsLoaded', function () {
      var lines, pages, pend, memo = 0;
      var currentItem;
      lines = _.reduce(this.get('originalCollection').models, function (memo, locatorItems) {
        return memo + locatorItems.get('items').length;
      }, 0);
      this.set('lines', lines);
      pages = Math.floor(this.get('lines') / this.get('linesPerPage'));
      pend = this.get('lines') % this.get('linesPerPage');
      if (pend > 0) {
        pages += 1;
      }
      this.set('pages', pages);
      this.generatePaginatedCollection();
      if (this.getPage()) {
        this.setPage(this.getPage(), true);
      } else {
        this.setPage(0, true);
      }
      if (this.get('windowModel').get('currentItem')) {
        if (this.get('items').get(this.get('windowModel').get('currentItem').id)) {
          this.get('windowModel').set('currentItem', this.get('items').get(this.get('windowModel').get('currentItem').id));
          currentItem = this.get('windowModel').get('currentItem');
          currentItem.trigger('selected', currentItem);
          currentItem.trigger('click', currentItem);
        } else {
          this.get('windowModel').unset('currentItem');
        }
      }
    }, this);
  },
  movePage: function (action, render) {
    if (action > 0) {
      this.pageUp(render);
    } else {
      this.pageDown(render);
    }
  },
  isPossibleBefore: function () {
    if (this.getPage() > 0) {
      return true;
    } else {
      return false;
    }
  },
  isPossibleAfter: function () {
    if (this.getPage() < this.getPages() - 1) {
      return true;
    } else {
      return false;
    }
  },
  pageUp: function (render, options) {
    this.set('currentPage', this.get('currentPage') + 1);
    if (render === true) {
      this.render(options);
    }
  },
  pageDown: function (render) {
    this.set('currentPage', this.get('currentPage') - 1);
    if (render === true) {
      this.render();
    }
  },
  setPage: function (page, render) {
    this.set('currentPage', page);
    if (render === true) {
      this.render();
    }
  },
  getPage: function () {
    return (this.get('currentPage'));
  },
  getCurrentPageNumber: function () {
    return (this.get('currentPage') + 1);
  },
  getPages: function () {
    return (this.get('pages'));
  },
  moveToNextItem: function (currentItem) {
    var me = this;
    var currentItemIndexInLocatorCollection = currentItem.get('_itemIndexInCollection');
    var currentLocatorsCollectionIndexInPage = currentItem.get('_binIndexInPage');
    var currentPageIndex = currentItem.get('_pageIndex');
    var next = null;

    function click(item) {
      setTimeout(function () {
        item.trigger('selected', item);
        item.trigger('click', item);
        if (OB.MobileApp.model.hasPermission('OBMWHP_setScanAfterAutoMove', true)) {
          item.get('picking').get('windowModel').set('activeTab', 'scan');
          OB.MobileApp.view.scanningFocus(true);
          setTimeout(function () {
            OB.MobileApp.view.scanningFocus(false);
          }, 500);
        }
      }, 500);
    }

    if (this.get('pagesMapping')[currentPageIndex].at(currentLocatorsCollectionIndexInPage).get('items').at(currentItemIndexInLocatorCollection + 1)) {
      //same page, same collection
      next = this.get('paginatedCollection').at(currentLocatorsCollectionIndexInPage).get('items').at(currentItemIndexInLocatorCollection + 1);
      click(next);
    } else if (this.get('pagesMapping')[currentPageIndex].size() > currentLocatorsCollectionIndexInPage + 1) {
      //same page different collection
      next = this.get('paginatedCollection').at(currentLocatorsCollectionIndexInPage + 1).get('items').at(0);
      click(next);
    } else if (this.get('pagesMapping').length > currentPageIndex + 1) {
      //Different page and different collection.
      var objMovePage = {
        nextAction: true,
        nextActionFunction: enyo.bind(this, function () {
          next = this.get('paginatedCollection').at(0).get('items').at(0);
          click(next);
        })
      };
      this.pageUp(true, objMovePage);
    }
  },
  render: function (options) {
    if (this.get('itemsToSave').length > 0 || this.get('itemsReady').length > 0) {
      OB.UTIL.showLoading(true);
      if (options && options.nextAction) {
        this.get('windowModel').processPicking(options);
      } else {
        this.get('windowModel').processPicking();
      }
    } else {
      this.get('paginatedCollection').reset(this.get('pagesMapping')[this.getPage()].models);
    }
  },
  generatePaginatedCollection: function () {
    var pages = [];
    var itemIndex = 0;
    var locatorIndex = 0;
    var readedItemsCounter = 0;
    var generatedPages = 0;
    pages[generatedPages] = new OBWH.Picking.Model.Locators();
    for (locatorIndex = 0; locatorIndex < this.get('originalCollection').length; locatorIndex++) {
      var curLocator = this.get('originalCollection').at(locatorIndex);
      for (itemIndex = 0; itemIndex < curLocator.get('items').length; itemIndex++) {
        var curItem = curLocator.get('items').at(itemIndex);
        if (pages[generatedPages].length === 0 || _.isUndefined(pages[generatedPages].get(curItem.get('storageBin')))) {
          pages[generatedPages].add(new OBWH.Picking.Model.Locator({
            name: curItem.get('storageBinName'),
            id: curItem.get('storageBin')
          }));
        }
        curLocator.get('items').at(itemIndex).set('_pageIndex', generatedPages);
        curLocator.get('items').at(itemIndex).set('_binIndexInPage', pages[generatedPages].size() - 1);
        pages[generatedPages].get(curItem.get('storageBin')).get('items').add(curLocator.get('items').at(itemIndex));
        curLocator.get('items').at(itemIndex).set('_itemIndexInCollection', pages[generatedPages].get(curItem.get('storageBin')).get('items').size() - 1);

        readedItemsCounter += 1;
        if (readedItemsCounter === this.get('linesPerPage')) {
          readedItemsCounter = 0;
          generatedPages += 1;
          pages[generatedPages] = new OBWH.Picking.Model.Locators();
        }
      }
    }
    this.set('pagesMapping', pages);
  }
});