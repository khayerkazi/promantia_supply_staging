/************************************************************************************
 * Copyright (C) 2012-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
OB.OBWPL = OB.OBWPL || {};
OB.OBWPL.QuantityValidate = function (item, validator, value, record) {
  var availableQty = isc.isA.Number(record.availableQty) ? new BigDecimal(String(record.availableQty)) : BigDecimal.prototype.ZERO,
      deliveredQuantity = isc.isA.Number(record.deliveredQuantity) ? new BigDecimal(String(record.deliveredQuantity)) : BigDecimal.prototype.ZERO,
      orderedQuantity = isc.isA.Number(record.orderedQuantity) ? new BigDecimal(String(record.orderedQuantity)) : BigDecimal.prototype.ZERO,
      reservedinothersQty = isc.isA.Number(record.reservedInOthers) ? new BigDecimal(String(record.reservedInOthers)) : BigDecimal.prototype.ZERO,
      quantity = null,
      totalQty = BigDecimal.prototype.ZERO,
      selectedRecords = item.grid.getSelectedRecords(),
      selectedRecordsLength = selectedRecords.length,
      editedRecord = null,
      i;
  if (!isc.isA.Number(value)) {
    return false;
  }
  if (value === null || value < 0) {
    return false;
  }
  quantity = new BigDecimal(String(value));
  if (quantity.compareTo(availableQty.subtract(reservedinothersQty)) > 0) {
    isc.warn(OB.I18N.getLabel('OBWPL_MoreQtyThanAvailable', [record.availableQty, record.reservedinothers]));
    return false;
  }
  for (i = 0; i < selectedRecordsLength; i++) {
    editedRecord = isc.addProperties({}, selectedRecords[i], item.grid.getEditedRecord(selectedRecords[i]));
    if (isc.isA.Number(editedRecord.quantity)) {
      totalQty = totalQty.add(new BigDecimal(String(editedRecord.quantity)));
    }
  }
  if (totalQty.compareTo(orderedQuantity.subtract(deliveredQuantity)) > 0) {
    isc.warn(OB.I18N.getLabel('OBWPL_Qty_MoreThanPendingOrdered', [orderedQuantity.subtract(deliveredQuantity).toString()]));
    return false;
  }
  if(quantity.compareTo(BigDecimal.prototype.ZERO) == 0){
	  return false;
  }
  return true;
};