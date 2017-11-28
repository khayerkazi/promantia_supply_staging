OB.RaiseIncidenceOnLoad = function (popup) {
  //validate popup
  var selectedRecords = popup.sourceView.viewGrid.getSelectedRecords();
  var reqStatus, reqBin, reqProduct, reqAttSet;
  var error = false;
  var selectedMovsIds = [];

  popup.theForm.getItem('movLines').hide();

  for (var int = 0; int < selectedRecords.length; int++) {
    if (int === 0) {
      reqStatus = selectedRecords[0].oBWPLItemStatus;
      reqBin = selectedRecords[0].storageBin;
      reqProduct = selectedRecords[0].product;
      reqAttSet = selectedRecords[0].attributeSetValue;
    } else {
      if (error === false && (selectedRecords[int].oBWPLItemStatus !== reqStatus || selectedRecords[int].storageBin !== reqBin || selectedRecords[int].product !== reqProduct || selectedRecords[int].attributeSetValue !== reqAttSet)) {
        error = true;
      }
    }
    selectedMovsIds.add(selectedRecords[int].id);
  }

  if (error) {
    popup.okButton.hide();
    popup.messageBar.setMessage('error', 'Selected movement lines cannot be grouped in the same incidence', 'To group lines in the same incidence, Status, Bin, Product and attribute should be the same.');
  }

  popup.theForm.getItem('movLines').setValue(selectedMovsIds);
};