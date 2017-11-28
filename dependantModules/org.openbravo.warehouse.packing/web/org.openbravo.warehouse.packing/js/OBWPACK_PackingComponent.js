/************************************************************************************ 
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/


isc.defineClass('OBPackingProcess', isc.VLayout);

isc.OBPackingProcess.addProperties({
  width: '100%',
  height: '100%',
  overflow: 'auto',
  autoSize: false,
  checkCalculate: null,
  waitForFocus: [],
  setWaitForFocus: function (time) {
    var me = this;
    if (!time) {
      time = 2500;
    }
    this.waitForFocus.push(true);
    setTimeout(function () {
      me.waitForFocus.splice(0, 1);
    }, time);
  },
  barcodeForm: null,
  messageBar: null,
  packingGridData: null,
  shipmentId: null,
  valuecheck: false,
  windowId: null,
  forceFocusToBarcode: true,
  boxNo: 1,
  headerStatus: 'DR',
  initWidget: function () {
    var packingGrid, validateButton, validateButtonLayout, processButton, cancelButton, addBoxButton, addBoxButtonLayout, buttonLayout = [],
        me = this;

    this.focusCheckInterval = setInterval(function () {
      var activeTab;
      if (!me || !me.parentWindow || !me.parentWindow.parentElement) {
        return false;
      }
      activeTab = me.parentWindow.parentElement.parentElement.getSelectedTab();
      if (me.forceFocusToBarcode && activeTab && activeTab.windowId === me.parentWindow.windowId && me.waitForFocus.length === 0) {
        me.barcodeForm.focusInItem('Barcode');
      }
    }, 500);

    me.messageBar = isc.OBMessageBar.create({
      visibility: 'hidden',
      view: me
    });

    packingGrid = isc.OBPackingProcessGrid.create({
      ID: OB.Utilities.generateRandomString(10, true, false, false, false),
      //Random ID to avoid Smartclient error when loading the process popup the second time is invoked
      data: me.packingGridData,
      theLayout: me
    });

    me.barcodeForm = isc.DynamicForm.create({
      titleOrientation: 'top',
      numCols: 3,
      draw: function () {
        this.Super('draw', arguments);
        this.focusInItem('Barcode');
      },
      fields: [{
        name: 'Box',
        title: OB.I18N.getLabel('OBWPACK_Box'),
        type: 'OBSpinnerItem',
        width: OB.Styles.OBWPACK.PackingProcess.boxWidth,
        required: true,
        showErrorIcon: false,
        defaultValue: 1,
        keyPressFilter: '[0-9]',
        min: 1,
        max: me.boxNo,
        focus: function () {
          me.setWaitForFocus(2500);
          return this.Super('focus', arguments);
        },
        mouseDown: function () {
          var value = this.getValue(),
              thisItem = this;
          setTimeout(function () {
            if (value !== thisItem.getValue()) {
              me.barcodeForm.focusInItem('Barcode');
            }
          }, 10);
          return this.Super('mouseDown', arguments);
        },
        keyDown: function () {
          if (isc.EH.getKey() === 'Enter' && !isc.EH.ctrlKeyDown() && !isc.EH.altKeyDown() && !isc.EH.shiftKeyDown()) {
            me.barcodeForm.focusInItem('Barcode');
            return false;
          } else {
            me.setWaitForFocus(1500);
            return this.Super('keyDown', arguments);
          }
        }
      }, {
        name: 'Quantity',
        title: OB.I18N.getLabel('OBWPACK_Quantity'),
        type: 'OBSpinnerItem',
        width: OB.Styles.OBWPACK.PackingProcess.quantityWidth,
        required: true,
        showErrorIcon: false,
        defaultValue: 1,
        keyPressFilter: '[0-9.]',
        min: 0.0,
        max: 9999,
        focus: function () {
          me.setWaitForFocus(2500);
          return this.Super('focus', arguments);
        },
        mouseDown: function () {
          var value = this.getValue(),
              thisItem = this;
          setTimeout(function () {
            if (value !== thisItem.getValue()) {
              me.barcodeForm.focusInItem('Barcode');
            }
          }, 10);
          return this.Super('mouseDown', arguments);
        },
        keyDown: function () {
          if (isc.EH.getKey() === 'Enter' && !isc.EH.ctrlKeyDown() && !isc.EH.altKeyDown() && !isc.EH.shiftKeyDown()) {
            me.barcodeForm.focusInItem('Barcode');
            return false;
          } else {
            me.setWaitForFocus(1500);
            return this.Super('keyDown', arguments);
          }
        }
      }, {
        name: 'Barcode',
        title: OB.I18N.getLabel('OBWPACK_Barcode'),
        type: 'OBTextItem',
        width: OB.Styles.OBWPACK.PackingProcess.barcodeWidth,
        required: true,
        showErrorIcon: false,
        defaultValue: '',
        keyDown: function () {
          if (isc.EH.getKey() === 'Enter' && !isc.EH.ctrlKeyDown() && !isc.EH.altKeyDown() && !isc.EH.shiftKeyDown()) {
            validateButton.click();
            return false;
          }
        }
      }]
    });

    me.firstFocusedItem = me.barcodeForm;

    addBoxButton = isc.OBLinkButtonItem.create({
      layoutTopMargin: OB.Styles.OBWPACK.PackingProcess.addBoxButtonTopMargin,
      title: '[ ' + OB.I18N.getLabel('OBWPACK_AddBox') + ' ]',
      click: function () {
        me.boxNo = Number(me.boxNo) + 1;
        me.barcodeForm.getItem('Box').max = me.boxNo;
        me.barcodeForm.getItem('Box').setValue(me.boxNo);
        packingGrid.addBox(me.boxNo, true);
      },
      keyUp: function () {
        if (isc.EH.getKey() === 'Enter' && !isc.EH.ctrlKeyDown() && !isc.EH.altKeyDown() && !isc.EH.shiftKeyDown()) {
          me.barcodeForm.focusInItem('Barcode');
        } else if (isc.EH.getKey() === 'Tab') {
          me.setWaitForFocus(1200);
        }
        return this.Super('keyUp', arguments);
      }
    });

    addBoxButtonLayout = isc.Layout.create({
      layoutTopMargin: OB.Styles.OBWPACK.PackingProcess.addBoxButtonLayoutTopMargin,
      members: [addBoxButton]
    });

    validateButton = isc.OBLinkButtonItem.create({
      layoutTopMargin: OB.Styles.OBWPACK.PackingProcess.validateButtonTopMargin,
      title: '[ ' + OB.I18N.getLabel('OBWPACK_ValidateBarcode') + ' ]',
      click: function () {
        var box, code, qty, validated = true;
        if (me.barcodeForm.getItem('Box').getValue() !== null && me.barcodeForm.getItem('Box').getValue() !== '' && me.barcodeForm.getItem('Quantity').getValue() !== null && me.barcodeForm.getItem('Quantity').getValue() !== '' && me.barcodeForm.getItem('Barcode').getValue() !== null && me.barcodeForm.getItem('Barcode').getValue() !== '') {
          box = me.barcodeForm.getItem('Box').getValue();
          code = me.barcodeForm.getItem('Barcode').getValue();
          qty = me.barcodeForm.getItem('Quantity').getValue();
          me.barcodeForm.getItem('Quantity').setValue('1');
          me.barcodeForm.getItem('Barcode').setValue('');
          validated = packingGrid.validateCode(box, code, qty);
        }
        if (validated === false) {
          me.waitForFocus.push(true);
          me.addMembers(isc.HTMLFlow.create({
            width: 1,
            contents: '<audio controls="controls" autoplay="true" style="display: none;"><source src="' + OB.Styles.OBWPACK.PackingProcess.errorSound + '.mp3" type="audio/mpeg"><source src="' + OB.Styles.OBWPACK.PackingProcess.errorSound + '.ogg" type="audio/ogg"><embed src="' + OB.Styles.OBWPACK.PackingProcess.errorSound + '.mp3" hidden="true" autostart="true" loop="false" /></audio>'
          }));
          isc.warn(OB.I18N.getLabel('OBWPACK_Alert_WrongBarcode'), function () {
            me.waitForFocus.splice(0, 1);
            return true;
          }, {
            icon: '[SKINIMG]Dialog/error.png',
            title: OB.I18N.getLabel('OBUIAPP_Error')
          });
        }
      },
      keyUp: function () {
        if (isc.EH.getKey() === 'Enter' && !isc.EH.ctrlKeyDown() && !isc.EH.altKeyDown() && !isc.EH.shiftKeyDown()) {
          me.barcodeForm.focusInItem('Barcode');
        } else if (isc.EH.getKey() === 'Tab') {
          me.setWaitForFocus(1200);
        }
        return this.Super('keyUp', arguments);
      }
    });

    checkCalculate = isc.DynamicForm.create({
      width: 20,
      fields: [{
        name: 'checkCalculate',
        height: 16,
        width: 20,
        showTitle: true,
        title: OB.I18N.getLabel('OBWPACK_CalculateWeight'),
        //value: this.getvalue,
        value: me.valuecheck,
        type: '_id_20'
      }]
    });

    validateButtonLayout = isc.Layout.create({
      layoutTopMargin: OB.Styles.OBWPACK.PackingProcess.validateButtonLayoutTopMargin,
      members: [validateButton, checkCalculate]
    });

    processButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBWPACK_GeneratePack'),
      click: function () {
        var areErrors = false,
            i;

        for (i = 0; i < packingGrid.getTotalRows(); i++) {
          packingGrid.updateRecordStyle(null, i, true);
          if (packingGrid.getRecord(i).qtyPending !== 0) {
            areErrors = true;
          }
        }

        if (areErrors) {
          isc.warn(OB.I18N.getLabel('OBWPACK_Alert_PendingToPack'), function () {
            return true;
          }, {
            icon: '[SKINIMG]Dialog/error.png',
            title: OB.I18N.getLabel('OBUIAPP_Error')
          });
        } else {

          if (me.windowId === "169") {
            me.process();
          } else {
            me.processHeader();
          }

          me.closePopup();

          return true;
        }
      },
      keyUp: function () {
        if (isc.EH.getKey() === 'Tab') {
          me.setWaitForFocus(1200);
        }
        return this.Super('keyUp', arguments);
      }
    });

    cancelButton = isc.OBFormButton.create({
      title: OB.I18N.getLabel('OBUISC_Dialog.CANCEL_BUTTON_TITLE'),
      click: function () {
        me.closePopup();
      },
      keyUp: function () {
        if (isc.EH.getKey() === 'Tab') {
          me.setWaitForFocus(1200);
        }
        return this.Super('keyUp', arguments);
      }
    });

    buttonLayout.push(addBoxButtonLayout);
    buttonLayout.push(me.barcodeForm);
    buttonLayout.push(validateButtonLayout);

    buttonLayout.push(isc.HLayout.create({
      width: OB.Styles.OBWPACK.PackingProcess.bottomComponentsSeparatorWidth
    }));


    if (me.headerStatus === 'DR') {
      buttonLayout.push(processButton);
    }
    buttonLayout.push(isc.HLayout.create({
      width: OB.Styles.OBWPACK.PackingProcess.bottomRightButtonsSeparatorWidth
    }));
    buttonLayout.push(cancelButton);

    this.members = [this.messageBar, packingGrid, isc.HLayout.create({
      align: 'center',
      width: '100%',
      height: OB.Styles.Process.PickAndExecute.buttonLayoutHeight,
      members: [isc.HLayout.create({
        width: 1,
        layoutTopMargin: OB.Styles.OBWPACK.PackingProcess.bottomRightButtonsLayoutTopMargin,
        overflow: 'visible',
        styleName: this.buttonBarStyleName,
        height: OB.Styles.OBWPACK.PackingProcess.bottomRightButtonsLayoutHeight,
        defaultLayoutAlign: OB.Styles.OBWPACK.PackingProcess.bottomRightButtonsLayoutAlign,
        members: buttonLayout
      })]
    })];

    this.Super('initWidget', arguments);
  },
  destroy: function () {
    clearInterval(this.focusCheckInterval);
    return this.Super('destroy', arguments);
  },
  closePopup: function () {
    this.parentElement.parentElement.closeClick(); // Super call
  },
  closeClick: function () {
    return true;
  },
  process: function () {
    var callback, me = this;

    callback = function (rpcResponse, data, rpcRequest) {
      var processLayout, popupTitle;
      if (!data) {
        me.sourceView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
        return;
      }
      if (data.message || !data.data) {
        me.sourceView.messageBar.setMessage(isc.OBMessageBar[data.message.severity], null, data.message.text);
        me.sourceView.refresh();
        return;
      }
      
    };
    OB.RemoteCallManager.call('org.openbravo.warehouse.packing.PackingActionHandler', {
      data: me.packingGridData,
      boxNo: me.boxNo,
      shipmentId: me.shipmentId,
      action: 'process',
      value: checkCalculate.getItem('checkCalculate').getValue()
    }, {}, callback);
  },
  processHeader: function () {
    var callback, me = this;
    checkCalculate.getItem(0).getValue();
    callback = function (rpcResponse, data, rpcRequest) {
      var processLayout, popupTitle;
      if (!data) {
        me.sourceView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, null);
        return;
      }
      if (data.message || !data.data) {
        me.sourceView.messageBar.setMessage(isc.OBMessageBar[data.message.severity], null, data.message.text);
        me.sourceView.refresh();
        return;
      }
    };
    OB.RemoteCallManager.call('org.openbravo.warehouse.packing.PackingActionHandler', {
      data: me.packingGridData,
      boxNo: me.boxNo,
      shipmentId: me.shipmentId,
      action: 'processHeader',
      value: checkCalculate.getItem('checkCalculate').getValue()
    }, {}, callback);
  }

});

isc.defineClass('OBPackingProcessGrid', isc.OBGrid);

isc.OBPackingProcessGrid.addProperties({
  enforceVClipping: true,
  selectionType: 'single',
  fixedRecordHeights: true,
  cellHeight: OB.Styles.Process.PickAndExecute.gridCellHeight,
  width: '100%',
  height: '100%',
  canReorderFields: true,
  //showAllRecords: true,
  drawAheadRatio: 6,
  showRecordComponents: true,
  showRecordComponentsByCell: true,
  theLayout: null,
  initWidget: function () {
    var i;
    this.fields = [{
      name: 'barcode',
      title: OB.I18N.getLabel('OBWPACK_Barcode'),
      width: OB.Styles.OBWPACK.PackingProcessGrid.barcodeColumnWidth,
      frozen: true
    }, {
      name: 'product',
      title: OB.I18N.getLabel('OBWPACK_Product'),
      width: OB.Styles.OBWPACK.PackingProcessGrid.productColumnWidth,
      frozen: true
    }, {
      name: 'quantity',
      title: OB.I18N.getLabel('OBWPACK_Quantity'),
      type: '_id_29',
      width: OB.Styles.OBWPACK.PackingProcessGrid.quantityColumnWidth,
      frozen: true
    }, {
      name: 'qtyPending',
      title: OB.I18N.getLabel('OBWPACK_QtyPending'),
      type: '_id_29',
      width: OB.Styles.OBWPACK.PackingProcessGrid.qtyPendingColumnWidth,
      frozen: true
    }, {
      name: 'iconStatus',
      showTitle: false,
      canSort: false,
      canReorder: false,
      canHide: false,
      width: OB.Styles.OBWPACK.PackingProcessGrid.iconStatusColumnWidth,
      frozen: true
    }];
    for (i = 1; i <= this.theLayout.boxNo; i++) {
      this.addBox(i, false);
    }
    this.Super('initWidget', arguments);
  },
  createRecordComponent: function (record, colNum) {
    var theGrid = this,
        fieldName = this.getFieldName(colNum),
        item = null,
        boxItemType = (isc.Browser.isMoz ? 'OBTextItem' : 'OBSpinnerItem'); // To avoid UI problem with Smartclient + Firefox rendering the OBSpinnerItem as a record component in the grid
    if (isc.startsWith(fieldName, 'box')) {
      item = isc.DynamicForm.create({
        width: OB.Styles.OBWPACK.PackingProcessGrid.qtyVerifiedInputWidth,
        fields: [{
          name: 'fld' + fieldName,
          type: boxItemType,
          showTitle: false,
          changeOnKeypress: true,
          cellStyle: '',
          //To avoid use default OBFormItem that adds a padding
          width: OB.Styles.OBWPACK.PackingProcessGrid.boxInputWidth,
          required: true,
          showErrorIcon: false,
          keyPressFilter: '[0-9.]',
          min: 0.0,
          change: function (form, item, value, oldValue) {
            record[fieldName] = value;
            theGrid.updateQuantity(record, null, value, oldValue);
          },
          init: function () {
            this.Super('init', arguments);
            if (!record[fieldName]) {
              record[fieldName] = 0;
            }
            this.setValue(record[fieldName]);
            if (!record.boxed) {
              record.boxed = 0;
            }
            //record.boxed = record.boxed + Number(0);
            //record.qtyPending = record.quantity - record.qtyVerified;
          },
          focus: function () {
            theGrid.selectSingleRecord(record);
            theGrid.theLayout.setWaitForFocus(2500);
            return this.Super('focus', arguments);
          },
          mouseDown: function () {
            var value = this.getValue(),
                thisItem = this;
            setTimeout(function () {
              if (value !== thisItem.getValue()) {
                theGrid.theLayout.barcodeForm.focusInItem('Barcode');
              }
            }, 10);
            return this.Super('mouseDown', arguments);
          },
          keyDown: function () {
            if (isc.EH.getKey() === 'Enter' && !isc.EH.ctrlKeyDown() && !isc.EH.altKeyDown() && !isc.EH.shiftKeyDown()) {
              theGrid.theLayout.barcodeForm.focusInItem('Barcode');
              return false;
            } else {
              theGrid.theLayout.setWaitForFocus(1500);
              return this.Super('keyDown', arguments);
            }
          }
        }]
      });
    } else if (fieldName === 'iconStatus') {
      item = isc.Img.create({
        width: OB.Styles.OBWPACK.PackingProcessGrid.iconStatusImgWidth,
        height: OB.Styles.OBWPACK.PackingProcessGrid.iconStatusImgHeight,
        imageType: 'normal',
        setIcon: function (status) {
          if (status === 'success') {
            this.setSrc(OB.Styles.OBWPACK.PackingProcessGrid.iconStatusSuccessSrc);
          } else if (status === 'error') {
            this.setSrc(OB.Styles.OBWPACK.PackingProcessGrid.iconStatusErrorSrc);
          } else {
            this.setSrc(OB.Styles.OBWPACK.PackingProcessGrid.iconStatusBlankSrc);
          }
        },
        initWidget: function () {
          if (record.qtyPending === 0) {
            this.setIcon('success');
          } else if (record.qtyPending < 0 || (record._baseStyle && record._baseStyle.indexOf(OB.Styles.OBWPACK.PackingProcessGrid.cellErrorStyle) !== -1)) {
            this.setIcon('error');
          } else {
            this.setIcon('');
          }
          theGrid.updateRecordStyle(record);
          this.Super('initWidget', arguments);
        }
      });
    }
    return item;
  },
  getCellVAlign: function () {
    return 'center';
  },
  getRecordComponentsCustom: function (record) {
    if (this.getRecordComponents) {
      return this.getRecordComponents(record);
    }
    var ids = record['_recordComponents_' + this.ID],
        key, components = {};
    if (ids) {
      for (key in ids) {
        if (ids.hasOwnProperty(key)) {
          if (ids[key].isNullMarker) {
            components[key] = ids[key];
          } else {
            components[key] = isc.Canvas.getById(ids[key]);
          }
        }
      }
    }
    return components;
  },
  updateRecordStyle: function (record, rowNum, isStrict) {
    var iconStatus, oldBaseStyle, newBaseStyle, newIconType, i;

    if (!rowNum && rowNum !== 0) {
      for (i = 0; i < this.data.length; i++) {
        if (!record.barcode && record.barcode !== '' && this.data[i].barcode === record.barcode) {
          rowNum = i;
          break;
        }
        if (record.productId === this.data[i].productId) {
          rowNum = i;
          break;
        }
      }
    }
    if (!record) {
      record = this.data.get(rowNum);
    }
    iconStatus = this.getRecordComponentsCustom(record).iconStatus;
    oldBaseStyle = record._baseStyle;

    if (record.qtyPending === 0) {
      //this.deselectRecord(rowNum);
      newBaseStyle = OB.Styles.OBWPACK.PackingProcessGrid.cellSuccessStyle + ' ' + this.baseStyle;
      newIconType = 'success';
    } else if (record.qtyPending < 0 || isStrict) {
      //this.deselectRecord(rowNum);
      newBaseStyle = OB.Styles.OBWPACK.PackingProcessGrid.cellErrorStyle + ' ' + this.baseStyle;
      newIconType = 'error';
    } else {
      newBaseStyle = this.baseStyle;
      newIconType = '';
    }
    if (newBaseStyle !== oldBaseStyle) {
      record._baseStyle = newBaseStyle;
      if (iconStatus) {
        iconStatus.setIcon(newIconType);
      }
      for (i = 0; i < this.fields.length; i++) {
        this.refreshCellStyle(rowNum, i);
      }
    }
  },
  updateQuantity: function (record, rowNum, qty, oldQty) {
    var i;
    if (!rowNum && rowNum !== 0) {
      for (i = 0; i < this.data.length; i++) {
        if (!record.barcode && record.barcode !== '' && this.data[i].barcode === record.barcode) {
          rowNum = i;
          break;
        }
        if (record.productId === this.data[i].productId) {
          rowNum = i;
          break;
        }
      }
    }
    if (!record) {
      record = this.data.get(rowNum);
    }
    if (!record.boxed) {
      record.boxed = 0;
    }
    if (isc.isA.Number(Number(oldQty))) {
      record.boxed = record.boxed - oldQty;
    }
    record.boxed = record.boxed + Number(qty);
    if (isc.isA.Number(Number(record.quantity - record.boxed))) {
      record.qtyPending = record.quantity - record.boxed;
    } else {
      record.qtyPending = record.quantity;
    }
    this.refreshRow(rowNum);
    this.scrollCellIntoView(rowNum, null, true, true);
    this.selectSingleRecord(rowNum);
    this.updateRecordStyle(record, rowNum);
  },
  addBox: function (boxNo, doSetFields) {
    var fields = this.fields,
        newField = {
        name: 'box' + boxNo,
        title: OB.I18N.getLabel('OBWPACK_BoxNo', [boxNo]),
        width: OB.Styles.OBWPACK.PackingProcessGrid.qtyBoxColumnWidth
        };
    fields.splice(5, 0, newField);
    if (doSetFields) {
      this.setFields(fields);
    }
  },
  validateCode: function (box, code, qty) {
    var record, rowNum, i, oldQty;
    for (i = 0; i < this.data.length; i++) {
      if (this.data[i].barcode === code) {
        record = this.data[i];
        rowNum = i;
        break;
      }
    }
    if (record) {
      oldQty = record['box' + box];
      this.getRecordComponentsCustom(record)['box' + box].getItem('fldbox' + box).setValue(qty + oldQty);
      record['box' + box] = qty + oldQty;
      this.updateQuantity(record, rowNum, qty + oldQty, oldQty);
      return true;
    } else {
      return false;
    }
  }
});