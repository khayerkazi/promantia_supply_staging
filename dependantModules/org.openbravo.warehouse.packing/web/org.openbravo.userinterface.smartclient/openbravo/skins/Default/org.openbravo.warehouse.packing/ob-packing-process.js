/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.Styles = OB.Styles || {};

OB.Styles.OBWPACK = OB.Styles.OBWPACK || {};

OB.Styles.OBWPACK.PackingProcess = {
  popupWidth: '1000',
  popupHeight: '90%',

  boxWidth: 75,
  quantityWidth: 75,
  barcodeWidth: 250,
  validateButtonTopMargin: 100,
  validateButtonLayoutTopMargin: 15,
  addBoxButtonTopMargin: 100,
  addBoxButtonLayoutTopMargin: 15,

  bottomComponentsSeparatorWidth: 60,

  bottomRightButtonsSeparatorWidth: 30,
  bottomRightButtonsLayoutTopMargin: 12,
  bottomRightButtonsLayoutHeight: 40,
  bottomRightButtonsLayoutAlign: 'center',

  errorSound: OB.Styles.skinsPath + 'Default/org.openbravo.warehouse.packing/sounds/barcodeAlert'
};

OB.Styles.OBWPACK.PackingProcessGrid = {
  barcodeColumnWidth: 150,
  productColumnWidth: 300,
  quantityColumnWidth: 80,
  qtyBoxColumnWidth: 80,
  qtyPendingColumnWidth: 80,
  iconStatusColumnWidth: 23,

  boxInputWidth: 75,

  iconStatusImgWidth: 20,
  iconStatusImgHeight: 20,
  iconStatusSuccessSrc: OB.Styles.skinsPath + 'Default/org.openbravo.warehouse.packing/images/packing-process/iconSuccess.png',
  iconStatusErrorSrc: OB.Styles.skinsPath + 'Default/org.openbravo.warehouse.packing/images/packing-process/iconError.png',
  iconStatusBlankSrc: OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/blank/blank.gif',

  cellSuccessStyle: 'OBPackingProcessGridCellSuccess',
  cellErrorStyle: 'OBPackingProcessGridCellError'
};