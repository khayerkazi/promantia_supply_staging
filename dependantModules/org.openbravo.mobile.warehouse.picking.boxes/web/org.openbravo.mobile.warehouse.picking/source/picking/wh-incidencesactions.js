/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global OBWH, enyo, Backbone, _*/

OB.OBMWHP = OB.OBMWHP || {};
OB.OBMWHP.IncidencesActions = OB.OBMWHP.IncidencesActions || {};

/*Standard action*/
enyo.kind({
  name: 'OBWH.Picking.StandardActionPopup_body',
  classes: 'row-fluid',
  components: [{
    classes: 'span12',
    components: [{
      content: 'Incidence description'
    }, {
      kind: 'enyo.TextArea',
      name: 'description'
    }]
  }]
});

enyo.kind({
  name: 'OBWH.Picking.StandardActionPopup_show_body',
  classes: 'row-fluid',
  components: [{
    classes: 'span12',
    components: [{
      classes: 'span1'
    }, {
      classes: 'span10',
      kind: 'Scroller',
      maxHeight: '300px',
      thumb: true,
      horizontal: 'hidden',
      style: 'padding: 8px 25px 4px 25px; line-height: 120%;',
      components: [{
        style: 'display: table; overflow: hidden; width: 100%; border-spacing: 0px 4px;',
        name: 'linePropertiesContainer'
      }]
    }, {
      classes: 'span1'
    }]
  }],
  propertiesToShow: [{
    kind: 'OBWH.Picking.LineProperty',
    position: 10,
    name: 'incType',
    I18NLabel: 'OBMWHP_Incidence',
    render: function (model) {
      if (model.get('incidenceObj').get('incidenceType').get('_identifier')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('incidenceType').get('_identifier'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 20,
    name: 'incBaseType',
    I18NLabel: 'OBMWHP_BaseIncidenceType',
    render: function (model) {
      if (model.get('incidenceObj').get('incidenceType').get('baseIncidenceType')._identifier) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('incidenceType').get('baseIncidenceType')._identifier);
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 30,
    name: 'createdBy',
    I18NLabel: 'OBMWHP_CreatedBy',
    render: function (model) {
      if (model.get('incidenceObj').get('createdBy$_identifier')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('createdBy$_identifier'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 40,
    name: 'description',
    I18NLabel: 'OBMWHP_Description',
    render: function (model) {
      if (model.get('incidenceObj').get('description')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('description'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }],
  initComponents: function () {
    var sortedPropertiesByPosition;
    this.inherited(arguments);
    sortedPropertiesByPosition = _.sortBy(this.propertiesToShow, function (comp) {
      return (comp.position ? comp.position : (comp.position === 0 ? 0 : 999));
    });
    enyo.forEach(sortedPropertiesByPosition, function (compToCreate) {
      this.$.linePropertiesContainer.createComponent(compToCreate);
    }, this);
  },
  render: function (model) {
    enyo.forEach(this.$.linePropertiesContainer.getComponents(), function (compToRender) {
      if (compToRender.kindName.indexOf("enyo.") !== 0) {
        compToRender.render(model);
      }
    }, this);
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OBWH.Picking.StandardActionPopup_OkButton',
  events: {
    onRaiseIncidenceFromPopup: ''
  },
  i18nContent: 'OBMOBC_LblOk',
  isDefaultAction: true,
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doRaiseIncidenceFromPopup();
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OBWH.Picking.StandardActionPopup_show_OkButton',
  events: {
    onHideThisPopup: ''
  },
  i18nContent: 'OBMOBC_LblOk',
  isDefaultAction: true,
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doHideThisPopup();
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OBWH.Picking.StandardActionPopup_CancelButton',
  i18nContent: 'OBMOBC_LblCancel',
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doHideThisPopup();
  }
});

enyo.kind({
  name: 'OBWH.Picking.IncidencesActions.OBWPL_StandardIncidence',
  kind: 'OB.UI.ModalAction',
  classes: 'standardActionPopup_body actionPopup_body',
  handlers: {
    onRaiseIncidenceFromPopup: 'raiseIncidence'
  },
  events: {
    onHideThisPopup: ''
  },
  raiseIncidence: function (inSender, inEvent) {
    var me = this;
    var incidence = {
      id: this.args.id,
      description: this.$.bodyContent.$.standardActionPopup_body.$.description.getValue()
    };
    this.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(true);
    this.$.bodyButtons.$.standardActionPopup_CancelButton.setDisabled(true);
    this.model.raiseIncidence(incidence, {
      callback: function (response) {
        me.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(false);
        me.$.bodyButtons.$.standardActionPopup_CancelButton.setDisabled(false);
        me.doHideThisPopup();
        if (response && response.exception) {
          OB.UTIL.showConfirmation.display('Error', 'Something has failed during the process to generate the incidence. Please reload the picking list and Make sure that you know what is happening before continue');
        }
      }
    });
    return true;
  },
  executeOnShow: function () {
    this.$.header.setContent(this.args._identifier);
    this.$.bodyContent.$.standardActionPopup_body.$.description.setValue("");
  },
  topPosition: '125px',
  i18nHeader: 'OBMWHP_IncidenceTypeSelector_Header',
  bodyContent: {
    kind: 'OBWH.Picking.StandardActionPopup_body'
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.StandardActionPopup_OkButton'
    }, {
      kind: 'OBWH.Picking.StandardActionPopup_CancelButton'
    }]
  },
  init: function (model) {
    this.model = model;
  }
});

enyo.kind({
  name: 'OBWH.Picking.IncidencesActions.OBWPL_StandardIncidence_show',
  kind: 'OB.UI.ModalAction',
  classes: 'standardActionPopup_body',
  i18nHeader: 'OBMWHP_IncidenceDetails_Header',
  events: {
    onHideThisPopup: ''
  },
  executeOnShow: function () {
    this.currentModel = this.model.get('currentItem');
    this.$.bodyContent.$.standardActionPopup_show_body.render(this.currentModel);
    //TODO: possible hook.
  },
  topPosition: '125px',
  bodyContent: {
    kind: 'OBWH.Picking.StandardActionPopup_show_body'
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.StandardActionPopup_show_OkButton'
    }]
  },
  init: function (model) {
    this.model = model;
  }
});

OB.UI.WindowView.registerPopup('OBWH.Picking.View', {
  kind: 'OBWH.Picking.IncidencesActions.OBWPL_StandardIncidence',
  name: 'OBWH.Picking.IncidencesActions.OBWPL_StandardIncidence'
});

OB.UI.WindowView.registerPopup('OBWH.Picking.View', {
  kind: 'OBWH.Picking.IncidencesActions.OBWPL_StandardIncidence_show',
  name: 'OBWH.Picking.IncidencesActions.OBWPL_StandardIncidence_show'
});

/*End standard action*/

/*Alternate Location action*/
enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OBWH.Picking.AlternateActionPopup_OkButton',
  events: {
    onRaiseIncidenceFromPopup: ''
  },
  i18nContent: 'OBMOBC_LblOk',
  isDefaultAction: true,
  tap: function () {
    if (this.disabled) {
      return true;
    }
    this.doRaiseIncidenceFromPopup();
  }
});

enyo.kind({
  name: 'OBWH.Picking.AlternateActionPopup_renderLine',
  components: [{
    name: 'line',
    classes: 'alternateStockLineContainer',
    components: [{
      classes: 'alternateStockLine left',
      components: [{
        classes: 'title',
        name: 'bin'
      }, {
        classes: 'subTitle',
        name: 'xyz'
      }]
    }, {
      classes: 'alternateStockLine right',
      components: [{
        classes: 'mainValue',
        name: 'qty'
      }]
    }, {
      style: 'clear: both;'
    }]
  }],
  create: function () {
    var xyzContent;
    this.inherited(arguments);
    this.$.bin.setContent(this.model.get('bin'));
    xyzContent = this.model.get('x') + "-" + this.model.get('y') + "-" + this.model.get('z');
    if (this.model.get('attSet')) {
      xyzContent += ' [' + this.model.get('attSet') + ']';
    }
    this.$.xyz.setContent(xyzContent);
    this.$.qty.setContent(this.model.get('pickedQty'));
  }
});
enyo.kind({
  name: 'OBWH.Picking.AlternateActionPopup_body',
  classes: 'row-fluid',
  published: {
    alternateStockCol: null
  },
  alternateStockColChanged: function () {
    this.$.result.setCollection(this.alternateStockCol);
  },
  components: [{
    classes: 'span12',
    components: [{
      name: 'alternateLocatorInfo',
      components: [{
        name: 'alternateLocatorStockLabel'
      }, {
        name: 'progress',
        classes: 'alternateLocator_progress'
      }, {
        name: 'result',
        scrollAreaMaxHeight: '150px',
        classes: 'alternateLocatorStockResult',
        kind: 'OB.UI.ScrollableTable',
        listStyle: 'nonselectablelist',
        renderLine: 'OBWH.Picking.AlternateActionPopup_renderLine',
        renderEmpty: 'OB.UI.RenderEmpty'
      }, {
        name: 'showError'
      }]
    }, {
      name: 'descriptionContainer',
      components: [{
        name: 'descriptionLabel'
      }, {
        kind: 'enyo.TextArea',
        name: 'description'
      }]
    }]
  }]
});

enyo.kind({
  name: 'OBWH.Picking.IncidencesActions.OBWPL_AlternateLocationIncidence',
  kind: 'OB.UI.ModalAction',
  classes: 'alternateActionPopup_body actionPopup_body',
  handlers: {
    onRaiseIncidenceFromPopup: 'raiseIncidence'
  },
  events: {
    onHideThisPopup: ''
  },
  stockHandler: 'org.openbravo.mobile.warehouse.picking.incidences.StockFromOtherLocator',
  raiseIncidence: function (inSender, inEvent) {
    var me = this;
    var incidence = {
      id: this.args.id,
      description: this.$.bodyContent.$.alternateActionPopup_body.$.description.getValue()
    };
    this.$.bodyButtons.$.alternateActionPopup_OkButton.setDisabled(true);
    this.$.bodyButtons.$.standardActionPopup_CancelButton.setDisabled(true);
    this.model.raiseIncidence(incidence, {
      callback: function (response) {
        me.$.bodyButtons.$.alternateActionPopup_OkButton.setDisabled(false);
        me.$.bodyButtons.$.standardActionPopup_CancelButton.setDisabled(false);
        me.doHideThisPopup();
        if (response && response.exception) {
          OB.UTIL.showConfirmation.display('Error', 'Something has failed during the process to generate the incidence. Please reload the picking list and Make sure that you know what is happening before continue');
        }
      }
    });
    return true;
  },
  executeOnShow: function () {
    this.currentModel = this.model.get('currentItem');
    this.$.bodyButtons.$.alternateActionPopup_OkButton.setDisabled(true);
    this.$.bodyContent.$.alternateActionPopup_body.setAlternateStockCol(this.alternateStockResultList);
    this.$.header.setContent(this.args._identifier);

    this.$.bodyContent.$.alternateActionPopup_body.$.progress.setContent(OB.I18N.getLabel('OBMWHP_loadingFromOtherLocators'));
    this.$.bodyContent.$.alternateActionPopup_body.$.progress.removeClass('error');
    this.$.bodyContent.$.alternateActionPopup_body.$.progress.show();

    this.$.bodyContent.$.alternateActionPopup_body.$.alternateLocatorStockLabel.setContent(OB.I18N.getLabel('OBMWHP_NearestBinsWithStock'));

    this.$.bodyContent.$.alternateActionPopup_body.$.result.hide();
    this.$.bodyContent.$.alternateActionPopup_body.$.descriptionLabel.setContent(OB.I18N.getLabel('OBMWHP_incidenceDescription'));
    this.$.bodyContent.$.alternateActionPopup_body.$.description.setValue("");
    this.getStockInformation();
  },
  getStockInformation: function () {
    var proc = new OB.DS.Process(this.stockHandler);
    proc.exec({
      incidence: this.args.id,
      items: JSON.parse(JSON.stringify(this.currentModel.get('dalItems').toJSON())),
      pickedQty: this.currentModel.get('pickedQty'),
      neededQty: this.currentModel.get('neededQty')
    }, enyo.bind(this, function (response, message) {
      if (response.exception) {
        //Error      
        this.$.bodyButtons.$.alternateActionPopup_OkButton.setDisabled(true);
        this.$.bodyContent.$.alternateActionPopup_body.$.result.hide();
        this.$.bodyContent.$.alternateActionPopup_body.$.progress.show();
        this.$.bodyContent.$.alternateActionPopup_body.$.progress.addClass('error');
        this.$.bodyContent.$.alternateActionPopup_body.$.progress.setContent(response.exception.message);
      } else {
        this.alternateStockResultList.reset(response.alternateLocators);
        this.$.bodyContent.$.alternateActionPopup_body.$.result.show();
        this.$.bodyContent.$.alternateActionPopup_body.$.progress.hide();
        this.$.bodyButtons.$.alternateActionPopup_OkButton.setDisabled(false);
      }
    }), enyo.bind(this, function (error) {
      this.$.bodyButtons.$.alternateActionPopup_OkButton.setDisabled(true);
      this.$.bodyContent.$.alternateActionPopup_body.$.result.hide();
      this.$.bodyContent.$.alternateActionPopup_body.$.progress.show();
      this.$.bodyContent.$.alternateActionPopup_body.$.progress.addClass('error');
      this.$.bodyContent.$.alternateActionPopup_body.$.progress.setContent(error.exception.message);
    }));
  },
  topPosition: '125px',
  bodyContent: {
    kind: 'OBWH.Picking.AlternateActionPopup_body'
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.AlternateActionPopup_OkButton'
    }, {
      kind: 'OBWH.Picking.StandardActionPopup_CancelButton'
    }]
  },
  init: function (model) {
    this.model = model;
    if (!this.alternateStockResultList) {
      OB.Model.AlternateStockResult = Backbone.Model.extend();
      OB.Model.AlternateStockResultList = Backbone.Collection.extend({
        model: OB.Model.AlternateStockResult
      });
      this.alternateStockResultList = new OB.Model.AlternateStockResultList([]);
    } else {
      this.alternateStockResultList.reset();
    }
  }
});

enyo.kind({
  name: 'OBWH.Picking.GenMovements_renderLine',
  components: [{
    name: 'line',
    classes: 'genMovementLineContainer',
    components: [{
      classes: 'genMovementLine',
      components: [{
        classes: 'title',
        name: 'bin'
      }, {
        classes: 'subTitle',
        name: 'details'
      }]
    }]
  }],
  create: function () {
    this.inherited(arguments);
    this.$.bin.setContent(this.model.get('storageBin_identifier') + ' [' + this.model.get('storageBin_position') + ']');
    this.$.details.setContent(OB.I18N.getLabel('OBMWHP_picked') + ': ' + this.model.get('oBWPLPickedqty') + ' - ' + OB.I18N.getLabel('OBMWHP_toPick') + ': ' + this.model.get('movementQuantity') + ' - ' + OB.I18N.getLabel('OBMWHP_pending') + ': ' + this.model.get('pendingQtyToPick'));
  }
});

enyo.kind({
  name: 'OBWH.Picking.AlternateActionPopup_show_body',
  classes: 'row-fluid',
  published: {
    genMovementsCollection: ''
  },
  components: [{
    classes: 'span12',
    name: 'genMovementsInfo',
    components: [{
      name: 'genMovementsInfoLabel'
    }, {
      name: 'progress',
      classes: 'genMovements_progress'
    }, {
      name: 'result',
      scrollAreaMaxHeight: '150px',
      classes: 'genMovementsResult',
      kind: 'OB.UI.ScrollableTable',
      listStyle: 'nonselectablelist',
      renderLine: 'OBWH.Picking.GenMovements_renderLine',
      renderEmpty: 'OB.UI.RenderEmpty'
    }, {
      name: 'showError'
    }]
  }, {
    name: 'detailsContainer',
    components: [{
      classes: 'span12',
      components: [{
        classes: 'span1'
      }, {
        classes: 'span10',
        kind: 'Scroller',
        maxHeight: '300px',
        thumb: true,
        horizontal: 'hidden',
        style: 'padding: 8px 25px 4px 25px; line-height: 120%;',
        components: [{
          style: 'display: table; overflow: hidden; width: 100%; border-spacing: 0px 4px;',
          name: 'linePropertiesContainer'
        }]
      }, {
        classes: 'span1'
      }]
    }]
  }],
  propertiesToShow: [{
    kind: 'OBWH.Picking.LineProperty',
    position: 10,
    name: 'incType',
    I18NLabel: 'OBMWHP_Incidence',
    render: function (model) {
      if (model.get('incidenceObj').get('incidenceType').get('_identifier')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('incidenceType').get('_identifier'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 20,
    name: 'incBaseType',
    I18NLabel: 'OBMWHP_BaseIncidenceType',
    render: function (model) {
      if (model.get('incidenceObj').get('incidenceType').get('baseIncidenceType')._identifier) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('incidenceType').get('baseIncidenceType')._identifier);
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 30,
    name: 'createdBy',
    I18NLabel: 'OBMWHP_CreatedBy',
    render: function (model) {
      if (model.get('incidenceObj').get('createdBy$_identifier')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('createdBy$_identifier'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 40,
    name: 'description',
    I18NLabel: 'OBMWHP_Description',
    render: function (model) {
      if (model.get('incidenceObj').get('description')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('description'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }],
  genMovementsCollectionChanged: function () {
    this.$.result.setCollection(this.genMovementsCollection);
  },
  initComponents: function () {
    var sortedPropertiesByPosition;
    this.inherited(arguments);
    sortedPropertiesByPosition = _.sortBy(this.propertiesToShow, function (comp) {
      return (comp.position ? comp.position : (comp.position === 0 ? 0 : 999));
    });
    enyo.forEach(sortedPropertiesByPosition, function (compToCreate) {
      this.$.linePropertiesContainer.createComponent(compToCreate);
    }, this);
  },
  render: function (model) {
    enyo.forEach(this.$.linePropertiesContainer.getComponents(), function (compToRender) {
      if (compToRender.kindName.indexOf("enyo.") !== 0) {
        compToRender.render(model);
      }
    }, this);
  }
});


enyo.kind({
  name: 'OBWH.Picking.IncidencesActions.OBWPL_AlternateLocationIncidence_show',
  kind: 'OB.UI.ModalAction',
  classes: 'alternateActionPopup_body actionPopup_body',
  i18nHeader: 'OBMWHP_IncidenceDetails_Header',
  handlers: {
    onRaiseIncidenceFromPopup: 'raiseIncidence'
  },
  events: {
    onHideThisPopup: ''
  },
  generatedMovementsHandler: 'org.openbravo.mobile.warehouse.picking.incidences.GetGeneratedMovementsByIncidence',
  executeOnShow: function () {
    this.currentModel = this.model.get('currentItem');
    this.$.bodyContent.$.modalBody.render(this.currentModel);
    this.$.bodyContent.$.modalBody.setGenMovementsCollection(this.generatedMovementsResultsList);
    this.$.bodyContent.$.modalBody.$.progress.setContent(OB.I18N.getLabel('OBMWHP_loadingGenMovs'));
    this.$.bodyContent.$.modalBody.$.progress.removeClass('error');
    this.$.bodyContent.$.modalBody.$.progress.show();

    this.$.bodyContent.$.modalBody.$.genMovementsInfoLabel.setContent(OB.I18N.getLabel('OBMWHP_genMovsByIncidence'));

    this.$.bodyContent.$.modalBody.$.result.hide();
    this.getMovementsInformation();
  },
  getMovementsInformation: function () {
    var proc = new OB.DS.Process(this.generatedMovementsHandler);
    proc.exec({
      incidenceId: this.currentModel.get('incidenceObj').get('id')
    }, enyo.bind(this, function (response, message) {
      if (response.exception) {
        //Error
        this.$.bodyContent.$.modalBody.$.result.hide();
        this.$.bodyContent.$.modalBody.$.progress.show();
        this.$.bodyContent.$.modalBody.$.progress.addClass('error');
        this.$.bodyContent.$.modalBody.$.progress.setContent(response.exception.message);
      } else {
        this.generatedMovementsResultsList.reset(response.generatedMovements);
        this.$.bodyContent.$.modalBody.$.result.show();
        this.$.bodyContent.$.modalBody.$.progress.hide();
      }
    }), enyo.bind(this, function (error) {
      this.$.bodyContent.$.modalBody.$.result.hide();
      this.$.bodyContent.$.modalBody.$.progress.show();
      this.$.bodyContent.$.modalBody.$.progress.addClass('error');
      this.$.bodyContent.$.modalBody.$.progress.setContent(error.exception.message);
    }));
  },
  topPosition: '125px',
  bodyContent: {
    kind: 'OBWH.Picking.AlternateActionPopup_show_body',
    name: 'modalBody'
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.StandardActionPopup_show_OkButton',
      name: 'okButton'
    }]
  },
  init: function (model) {
    this.model = model;
    if (!this.alternateStockResultList) {
      OB.Model.GeneratedMovementsResult = Backbone.Model.extend();
      OB.Model.GeneratedMovementsResultList = Backbone.Collection.extend({
        model: OB.Model.GeneratedMovementsResult
      });
      this.generatedMovementsResultsList = new OB.Model.GeneratedMovementsResultList([]);
    } else {
      this.generatedMovementsResultList.reset();
    }
  }
});

OB.UI.WindowView.registerPopup('OBWH.Picking.View', {
  kind: 'OBWH.Picking.IncidencesActions.OBWPL_AlternateLocationIncidence',
  name: 'OBWH.Picking.IncidencesActions.OBWPL_AlternateLocationIncidence'
});

OB.UI.WindowView.registerPopup('OBWH.Picking.View', {
  kind: 'OBWH.Picking.IncidencesActions.OBWPL_AlternateLocationIncidence_show',
  name: 'OBWH.Picking.IncidencesActions.OBWPL_AlternateLocationIncidence_show'
});

/*End alternate location action*/




/*Box Empty action*/

enyo.kind({
  name: 'OBWH.Picking.BoxEmptyActionPopup_body',
  classes: 'row-fluid',
  published: {
    alternateStockCol: null
  },
  alternateStockColChanged: function () {
    this.$.result.setCollection(this.alternateStockCol);
  },
  components: [{
    classes: 'span12',
    components: [{
      name: 'alternateLocatorInfo',
      components: [{
        name: 'alternateLocatorStockLabel'
      }, {
        name: 'progress',
        classes: 'alternateLocator_progress'
      }, {
        name: 'result',
        scrollAreaMaxHeight: '150px',
        classes: 'alternateLocatorStockResult',
        kind: 'OB.UI.ScrollableTable',
        listStyle: 'nonselectablelist',
        renderLine: 'OBWH.Picking.AlternateActionPopup_renderLine',
        renderEmpty: 'OB.UI.RenderEmpty'
      }, {
        name: 'showError'
      }]
    }, {
      name: 'descriptionContainer',
      components: [{
        name: 'descriptionLabel'
      }, {
        kind: 'enyo.TextArea',
        name: 'description'
      }]
    }]
  }]
});

enyo.kind({
  name: 'OBWH.Picking.IncidencesActions.OBWPL_BoxEmptyIncidence',
  kind: 'OB.UI.ModalAction',
  classes: 'alternateActionPopup_body actionPopup_body',
  handlers: {
    onRaiseIncidenceFromPopup: 'raiseIncidence'
  },
  events: {
    onHideThisPopup: ''
  },
  stockHandler: 'org.openbravo.mobile.warehouse.picking.incidences.StockFromOtherLocator',
  raiseIncidence: function (inSender, inEvent) {
    var me = this;
    var incidence = {
      id: this.args.id,
      description: this.$.bodyContent.$.boxEmptyActionPopup_body.$.description.getValue()
    };
    this.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(true);
    this.$.bodyButtons.$.standardActionPopup_CancelButton.setDisabled(true);
    this.model.raiseIncidence(incidence, {
      callback: function (response) {
        me.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(false);
        me.$.bodyButtons.$.standardActionPopup_CancelButton.setDisabled(false);
        me.doHideThisPopup();
        if (response && response.exception) {
          OB.UTIL.showConfirmation.display('Error', 'Something has failed during the process to generate the incidence. Please reload the picking list and Make sure that you know what is happening before continue');
        }
      }
    });
    return true;
  },
  executeOnShow: function () {
    this.currentModel = this.model.get('currentItem');
    this.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(true);
    this.$.bodyContent.$.boxEmptyActionPopup_body.setAlternateStockCol(this.alternateStockResultList);
    this.$.header.setContent(this.args._identifier);

    this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.setContent(OB.I18N.getLabel('OBMWHP_loadingFromOtherLocators'));
    this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.removeClass('error');
    this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.show();

    this.$.bodyContent.$.boxEmptyActionPopup_body.$.alternateLocatorStockLabel.setContent(OB.I18N.getLabel('OBMWHP_NearestBinsWithStock'));

    this.$.bodyContent.$.boxEmptyActionPopup_body.$.result.hide();
    this.$.bodyContent.$.boxEmptyActionPopup_body.$.descriptionLabel.setContent(OB.I18N.getLabel('OBMWHP_incidenceDescription'));
    this.$.bodyContent.$.boxEmptyActionPopup_body.$.description.setValue("");
    this.getStockInformation();
  },
  getStockInformation: function () {
    var proc = new OB.DS.Process(this.stockHandler);
    proc.exec({
      incidence: this.args.id,
      items: JSON.parse(JSON.stringify(this.currentModel.get('dalItems').toJSON())),
      pickedQty: this.currentModel.get('pickedQty'),
      neededQty: this.currentModel.get('neededQty')
    }, enyo.bind(this, function (response, message) {
      if (response.exception) {
        //Error      
        this.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(false);
        this.$.bodyContent.$.boxEmptyActionPopup_body.$.result.hide();
        this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.show();
        this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.addClass('error');
        this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.setContent(OB.I18N.getLabel('OBMWHP_NoAlternateStock'));
      } else {
        this.alternateStockResultList.reset(response.alternateLocators);
        this.$.bodyContent.$.boxEmptyActionPopup_body.$.result.show();
        this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.hide();
        this.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(false);
      }
    }), enyo.bind(this, function (error) {
      this.$.bodyButtons.$.standardActionPopup_OkButton.setDisabled(true);
      this.$.bodyContent.$.boxEmptyActionPopup_body.$.result.hide();
      this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.show();
      this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.addClass('error');
      this.$.bodyContent.$.boxEmptyActionPopup_body.$.progress.setContent(error.exception.message);
    }));
  },
  topPosition: '125px',
  bodyContent: {
    kind: 'OBWH.Picking.BoxEmptyActionPopup_body'
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.StandardActionPopup_OkButton'
    }, {
      kind: 'OBWH.Picking.StandardActionPopup_CancelButton'
    }]
  },
  init: function (model) {
    this.model = model;
    if (!this.alternateStockResultList) {
      OB.Model.AlternateStockResult = Backbone.Model.extend();
      OB.Model.AlternateStockResultList = Backbone.Collection.extend({
        model: OB.Model.AlternateStockResult
      });
      this.alternateStockResultList = new OB.Model.AlternateStockResultList([]);
    } else {
      this.alternateStockResultList.reset();
    }
  }
});

enyo.kind({
  name: 'OBWH.Picking.BoxEmptyActionPopup_show_body',
  classes: 'row-fluid',
  published: {
    genMovementsCollection: ''
  },
  components: [{
    classes: 'span12',
    name: 'genMovementsInfo',
    components: [{
      name: 'genMovementsInfoLabel'
    }, {
      name: 'progress',
      classes: 'genMovements_progress'
    }, {
      name: 'result',
      scrollAreaMaxHeight: '150px',
      classes: 'genMovementsResult',
      kind: 'OB.UI.ScrollableTable',
      listStyle: 'nonselectablelist',
      renderLine: 'OBWH.Picking.GenMovements_renderLine',
      renderEmpty: 'OB.UI.RenderEmpty'
    }, {
      name: 'showError'
    }]
  }, {
    name: 'detailsContainer',
    components: [{
      classes: 'span12',
      components: [{
        classes: 'span1'
      }, {
        classes: 'span10',
        kind: 'Scroller',
        maxHeight: '300px',
        thumb: true,
        horizontal: 'hidden',
        style: 'padding: 8px 25px 4px 25px; line-height: 120%;',
        components: [{
          style: 'display: table; overflow: hidden; width: 100%; border-spacing: 0px 4px;',
          name: 'linePropertiesContainer'
        }]
      }, {
        classes: 'span1'
      }]
    }]
  }],
  propertiesToShow: [{
    kind: 'OBWH.Picking.LineProperty',
    position: 10,
    name: 'incType',
    I18NLabel: 'OBMWHP_Incidence',
    render: function (model) {
      if (model.get('incidenceObj').get('incidenceType').get('_identifier')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('incidenceType').get('_identifier'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 20,
    name: 'incBaseType',
    I18NLabel: 'OBMWHP_BaseIncidenceType',
    render: function (model) {
      if (model.get('incidenceObj').get('incidenceType').get('baseIncidenceType')._identifier) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('incidenceType').get('baseIncidenceType')._identifier);
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 30,
    name: 'createdBy',
    I18NLabel: 'OBMWHP_CreatedBy',
    render: function (model) {
      if (model.get('incidenceObj').get('createdBy$_identifier')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('createdBy$_identifier'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }, {
    kind: 'OBWH.Picking.LineProperty',
    position: 40,
    name: 'description',
    I18NLabel: 'OBMWHP_Description',
    render: function (model) {
      if (model.get('incidenceObj').get('description')) {
        this.$.propertyValue.setContent(model.get('incidenceObj').get('description'));
      } else {
        this.$.propertyValue.setContent('');
      }
    }
  }],
  genMovementsCollectionChanged: function () {
    this.$.result.setCollection(this.genMovementsCollection);
  },
  initComponents: function () {
    var sortedPropertiesByPosition;
    this.inherited(arguments);
    sortedPropertiesByPosition = _.sortBy(this.propertiesToShow, function (comp) {
      return (comp.position ? comp.position : (comp.position === 0 ? 0 : 999));
    });
    enyo.forEach(sortedPropertiesByPosition, function (compToCreate) {
      this.$.linePropertiesContainer.createComponent(compToCreate);
    }, this);
  },
  render: function (model) {
    enyo.forEach(this.$.linePropertiesContainer.getComponents(), function (compToRender) {
      if (compToRender.kindName.indexOf("enyo.") !== 0) {
        compToRender.render(model);
      }
    }, this);
  }
});


enyo.kind({
  name: 'OBWH.Picking.IncidencesActions.OBWPL_BoxEmptyIncidence_show',
  kind: 'OB.UI.ModalAction',
  classes: 'alternateActionPopup_body actionPopup_body',
  i18nHeader: 'OBMWHP_IncidenceDetails_Header',
  handlers: {
    onRaiseIncidenceFromPopup: 'raiseIncidence'
  },
  events: {
    onHideThisPopup: ''
  },
  generatedMovementsHandler: 'org.openbravo.mobile.warehouse.picking.incidences.GetGeneratedMovementsByIncidence',
  executeOnShow: function () {
    this.currentModel = this.model.get('currentItem');
    this.$.bodyContent.$.modalBody.render(this.currentModel);
    this.$.bodyContent.$.modalBody.setGenMovementsCollection(this.generatedMovementsResultsList);
    this.$.bodyContent.$.modalBody.$.progress.setContent(OB.I18N.getLabel('OBMWHP_loadingGenMovs'));
    this.$.bodyContent.$.modalBody.$.progress.removeClass('error');
    this.$.bodyContent.$.modalBody.$.progress.show();

    this.$.bodyContent.$.modalBody.$.genMovementsInfoLabel.setContent(OB.I18N.getLabel('OBMWHP_genMovsByIncidence'));

    this.$.bodyContent.$.modalBody.$.result.hide();
    this.getMovementsInformation();
  },
  getMovementsInformation: function () {
    var proc = new OB.DS.Process(this.generatedMovementsHandler);
    proc.exec({
      incidenceId: this.currentModel.get('incidenceObj').get('id')
    }, enyo.bind(this, function (response, message) {
      if (response.exception) {
        //Error
        this.$.bodyContent.$.modalBody.$.result.hide();
        this.$.bodyContent.$.modalBody.$.progress.show();
        this.$.bodyContent.$.modalBody.$.progress.addClass('error');
        this.$.bodyContent.$.modalBody.$.progress.setContent(response.exception.message);
      } else {
        if (response.generatedMovements.length > 0) {
          this.generatedMovementsResultsList.reset(response.generatedMovements);
          this.$.bodyContent.$.modalBody.$.result.show();
          this.$.bodyContent.$.modalBody.$.progress.hide();
        } else {
          this.$.bodyContent.$.modalBody.$.result.hide();
          this.$.bodyContent.$.modalBody.$.genMovementsInfoLabel.hide();
          this.$.bodyContent.$.modalBody.$.progress.show();
          this.$.bodyContent.$.modalBody.$.progress.addClass('error');
          this.$.bodyContent.$.modalBody.$.progress.setContent(OB.I18N.getLabel('OBMWHP_NoAlternateMovements'));
        }
      }
    }), enyo.bind(this, function (error) {
      this.$.bodyContent.$.modalBody.$.result.hide();
      this.$.bodyContent.$.modalBody.$.progress.show();
      this.$.bodyContent.$.modalBody.$.progress.addClass('error');
      this.$.bodyContent.$.modalBody.$.progress.setContent(error.exception.message);
    }));
  },
  topPosition: '125px',
  bodyContent: {
    kind: 'OBWH.Picking.BoxEmptyActionPopup_show_body',
    name: 'modalBody'
  },
  bodyButtons: {
    components: [{
      kind: 'OBWH.Picking.StandardActionPopup_show_OkButton',
      name: 'okButton'
    }]
  },
  init: function (model) {
    this.model = model;
    if (!this.alternateStockResultList) {
      OB.Model.GeneratedMovementsResult = Backbone.Model.extend();
      OB.Model.GeneratedMovementsResultList = Backbone.Collection.extend({
        model: OB.Model.GeneratedMovementsResult
      });
      this.generatedMovementsResultsList = new OB.Model.GeneratedMovementsResultList([]);
    } else {
      this.generatedMovementsResultList.reset();
    }
  }
});

OB.UI.WindowView.registerPopup('OBWH.Picking.View', {
  kind: 'OBWH.Picking.IncidencesActions.OBWPL_BoxEmptyIncidence',
  name: 'OBWH.Picking.IncidencesActions.OBWPL_BoxEmptyIncidence'
});

OB.UI.WindowView.registerPopup('OBWH.Picking.View', {
  kind: 'OBWH.Picking.IncidencesActions.OBWPL_BoxEmptyIncidence_show',
  name: 'OBWH.Picking.IncidencesActions.OBWPL_BoxEmptyIncidence_show'
});

/*End Box Empty action*/