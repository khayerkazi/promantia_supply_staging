/************************************************************************************
 * Copyright (C) 2012-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/

OB.Styles = OB.Styles || {};

OB.Styles.OBWPL = OB.Styles.OBWPL || {};

OB.Styles.OBWPL.PickValidateProcess = {
  popupWidth: '900',
  popupHeight: '90%',

  quantityWidth: 75,
  barcodeWidth: 250,
  validateButtonTopMargin: 100,
  validateButtonLayoutTopMargin: 15,

  bottomComponentsSeparatorWidth: 120,

  bottomRightButtonsSeparatorWidth: 30,
  bottomRightButtonsLayoutTopMargin: 12,
  bottomRightButtonsLayoutHeight: 40,
  bottomRightButtonsLayoutAlign: 'center',

  errorSound: OB.Styles.skinsPath + 'Default/org.openbravo.warehouse.pickinglist/sounds/barcodeAlert'
};

OB.Styles.OBWPL.PickValidateProcessGrid = {
  barcodeColumnWidth: 150,
  productColumnWidth: '*',
  quantityColumnWidth: 85,
  qtyVerifiedColumnWidth: 85,
  qtyPendingColumnWidth: 85,
  iconStatusColumnWidth: 23,

  qtyVerifiedInputWidth: 75,

  iconStatusImgWidth: 20,
  iconStatusImgHeight: 20,
  iconStatusSuccessSrc: OB.Styles.skinsPath + 'Default/org.openbravo.warehouse.pickinglist/images/pick-validate-process/iconSuccess.png',
  iconStatusErrorSrc: OB.Styles.skinsPath + 'Default/org.openbravo.warehouse.pickinglist/images/pick-validate-process/iconError.png',
  iconStatusBlankSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/blank/blank.gif',

  cellSuccessStyle: 'OBPickValidateProcessGridCellSuccess',
  cellErrorStyle: 'OBPickValidateProcessGridCellError'
};