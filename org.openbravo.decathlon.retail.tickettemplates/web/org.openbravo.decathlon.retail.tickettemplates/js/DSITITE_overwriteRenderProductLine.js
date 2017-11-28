(function () {
    OB.UI.RenderProduct.prototype.setIdentifierContent = function () {
    return this.model.get('_identifier') + ' / ' + this.model.get('dsitite_brandName') + ' / ' + this.model.get('dsitite_modelName');
  }
}());