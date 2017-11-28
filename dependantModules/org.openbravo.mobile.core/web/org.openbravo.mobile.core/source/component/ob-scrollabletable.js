/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global _, Backbone, $, enyo */

enyo.kind({
  name: 'OB.UI.ScrollableTableHeader',
  style: 'border-bottom: 1px solid #cccccc;',
  handlers: {
    onScrollableTableHeaderChanged: 'scrollableTableHeaderChanged_handler'
  }
});

enyo.kind({
  name: 'OB.UI.ScrollableTable',
  published: {
    collection: null,
    listStyle: null
  },
  getValue: function (arg, lineIndex) {
    //Automated test
    if (this.$.tbody.children[lineIndex].children[0].getValue) {
      return this.$.tbody.children[lineIndex].children[0].getValue(arg);
    } else {
      OB.warn('getValue is not present in line ' + lineIndex + ' component of ' + this.name);
      return null;
    }
  },
  getNumberOfLines: function () {
    //Automated test
    return this.$.tbody.children.length;
  },
  getLineByIndex: function (lineIndex) {
    //Automated test
    if (this.$.tbody.children[lineIndex].children[0]) {
      return this.$.tbody.children[lineIndex].children[0];
    } else {
      return undefined;
    }
  },
  getLineByIndexAndClick: function (lineIndex) {
    //Automated test
    var selectedLine = this.getLineByIndex(lineIndex);
    if (!_.isUndefined(selectedLine)) {
      selectedLine.tap();
    }
    return selectedLine;
  },
  getLineByValue: function (arg, value) {
    //Automated test
    var selectedLine;
    selectedLine = _.find(this.$.tbody.children, function (line) {
      if (line.children[0].getValue && line.children[0].getValue(arg) === value) {
        return true;
      }
    });
    if (!_.isUndefined(selectedLine)) {
      return selectedLine.children[0];
    } else {
      return undefined;
    }
  },
  getLineByValueAndClick: function (arg, value) {
    var selectedLine = this.getLineByValue(arg, value);
    if (!_.isUndefined(selectedLine)) {
      selectedLine.tap();
    }
    return selectedLine;
  },
  isSelectableLine: function (model) {
    return true;
  },
  components: [{
    name: 'theader'
  }, {
    components: [{
      kind: 'Scroller',
      name: 'scrollArea',
      thumb: true,
      horizontal: 'hidden',
      components: [{
        name: 'tbody',
        tag: 'ul',
        classes: 'unstyled',
        showing: false
      }]
    }]
  }, {
    name: 'tlimit',
    showing: false,
    style: 'border-bottom: 1px solid #cccccc; padding: 15px; text-align:center; font-weight: bold; color: #a1a328'
  }, {
    name: 'tinfo',
    showing: false,
    style: 'border-bottom: 1px solid #cccccc; padding: 15px; font-weight: bold; color: #cccccc'
  }, {
    name: 'tempty'
  }],
  create: function () {
    var tableName = this.name || '';

    this.inherited(arguments);

    // helping developers
    if (!this.renderLine) {
      throw enyo.format('Your list %s needs to define a renderLine kind', tableName);
    }

    if (!this.renderEmpty) {
      throw enyo.format('Your list %s needs to define a renderEmpty kind', tableName);
    }

    if (this.collection) {
      this.collectionChanged(null);
    }

    this.$.tlimit.setContent(OB.I18N.getLabel('OBMOBC_DataLimitReached'));
  },

  listStyleChanged: function (oldSTyle) {
    if (this.listStyle === 'checkboxlist') {
      this.collection.trigger('unSelectAll');
    }
  },

  collectionChanged: function (oldCollection) {
    this.selected = null;

    if (this.renderHeader && this.$.theader.getComponents().length === 0) {
      this.$.theader.createComponent({
        kind: this.renderHeader
      });
    }

    if (this.renderEmpty && this.$.tempty.getComponents().length === 0) {
      this.$.tempty.createComponent({
        kind: this.renderEmpty
      });
    }

    if (this.scrollAreaMaxHeight) {
      this.$.scrollArea.setMaxHeight(this.scrollAreaMaxHeight);
    }

    if (!this.collection) { // set to null?
      return;
    }

    /*selected*/
    this.func_selected = function (model) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('selected', this.func_selected);
        return true;
      }
      if (!model && this.listStyle && this.listStyle !== 'checkboxlist') {
        if (this.selected) {
          this.selected.addRemoveClass('selected', false);
        }
        this.selected = null;
      }
    };
    this.collection.on('selected', this.func_selected, this);

    /*unSelectAll*/
    this.func_unSelectAll = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('unSelectAll', this.func_unSelectAll);
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('unselected');
      });
    };
    this.collection.on('unSelectAll', this.func_unSelectAll, this);

    /*checkAll*/
    this.func_checkAll = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('checkAll', this.func_checkAll);
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('check');
      });
    };
    this.collection.on('checkAll', this.func_checkAll, this);

    /*unCheckAll*/
    this.func_unCheckAll = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('unCheckAll', this.func_unCheckAll);
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('uncheck');
      });
    };
    this.collection.on('unCheckAll', this.func_unCheckAll, this);

    /*add*/
    this.func_add = function (model, prop, options) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('add', this.func_add);
        return true;
      }

      this.$.tempty.hide();
      this.$.tbody.show();

      this._addModelToCollection(model, options.index);

      if (this.listStyle === 'list') {
        if (!this.selected) {
          model.trigger('selected', model);
        }
      } else if (this.listStyle === 'edit') {
        model.trigger('selected', model);
      }
      this.setScrollAfterAdd();
    };
    this.collection.on('add', this.func_add, this);

    /*remove*/
    this.func_remove = function (model, prop, options) {
      var index = options.index,
          indexToPoint = index - 1;

      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('remove', this.func_remove);
        return true;
      }

      this.$.tbody.getComponents()[index].destroy(); // controlAtIndex ?
      if (index >= this.collection.length) {
        if (this.collection.length === 0) {
          this.collection.trigger('selected');
        } else {
          this.collection.at(this.collection.length - 1).trigger('selected', this.collection.at(this.collection.length - 1));
        }
      } else {
        this.collection.at(index).trigger('selected', this.collection.at(index));
      }

      if (this.collection.length === 0) {
        this.$.tbody.hide();
        this.$.tempty.show();
      } else {
        //Put scroller in the previous item of deleted one
        //Issue 0021835 except when the deleted is the first one.
        if (indexToPoint < 0) {
          indexToPoint = 0;
        }
        this.getScrollArea().scrollToControl(this.$.tbody.getComponents()[indexToPoint]);
      }
    };
    this.collection.on('remove', this.func_remove, this);

    /*reset*/
    this.func_reset = function (a, b, c) {
      var modelsel, dataLimit;

      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('reset', this.func_reset);
        return true;
      }

      this.$.tlimit.hide();
      this.$.tbody.hide();
      this.$.tempty.show();

      this.$.tbody.destroyComponents();

      if (this.collection.size() === 0) {
        this.$.tbody.hide();
        this.$.tempty.show();
        this.collection.trigger('selected');
      } else {
        this.$.tempty.hide();
        this.$.tbody.show();
        this.collection.each(function (model) {
          this._addModelToCollection(model);
        }, this);
        // added to fix bug 25287
        this.getScrollArea().render();

        dataLimit = this.collection.at(0).dataLimit;
        if (dataLimit && dataLimit <= this.collection.length) {
          this.$.tlimit.show();
        }

        if (this.listStyle === 'list') {
          modelsel = this.collection.at(0);
          modelsel.trigger('selected', modelsel);
        } else if (this.listStyle === 'edit') {
          modelsel = this.collection.at(this.collection.size() - 1);
          if (this.autoSelectOnReset === false){
            //nothing
          } else {
            modelsel.trigger('selected', modelsel);  
          }
        }
      }
    };

    this.collection.on('reset', this.func_reset, this);

    /*info*/
    this.func_info = function (info) {
      //if the same collection is used by different components and one of them has been destroyed.
      if (this.destroyed) {
        this.collection.off('info', this.func_info);
        return true;
      }
      if (info) {
        this.$.tinfo.setContent(OB.I18N.getLabel(info));
        this.$.tinfo.show();
      } else {
        this.$.tinfo.hide();
      }
    };
    this.collection.on('info', this.func_info, this);

    // XXX: Reseting to show the collection if registered with data
    this.collection.trigger('reset');
  },
  getScrollArea: function () {
    return this.$.scrollArea;
  },
  setScrollAfterAdd: function(){
	//Put scroller in the position of new item
      this.getScrollArea().scrollToBottom();
  },
  setScrollAfterAdd: function () {
    //Put scroller in the position of new item
    this.getScrollArea().scrollToBottom();
  },
  getHeader: function () {
    var tableName = this.name || '';
    if (this.$.theader.getComponents()) {
      if (this.$.theader.getComponents().length > 0) {
        if (this.$.theader.getComponents().length === 1) {
          return this.$.theader.getComponents()[0];
        } else {
          //developers help
          throw enyo.format('Each scrolleable table ahould have only one component as header', tableName);
        }
      }
    }
    return null;
  },
  getHeaderValue: function () {
    var header = this.getHeader();
    if (header) {
      if (header.getValue) {
        return header.getValue();
      } else {
        return header.getContent();
      }
    }
    return '';
  },

  _addModelToCollection: function (model, index) {
    var i, models = [];

    var components = this.$.tbody.getComponents();
    if (!(_.isUndefined(index)) && !(_.isNull(index)) && index < components.length) {
      //refresh components collection, inserting new model...
      // get the models from current components
      for (i = 0; i < components.length; i++) {
        models[i] = {
          renderlinemodel: components[i].renderline.model,
          checked: components[i].checked
        };
      }
      this.$.tbody.destroyComponents();
      // rebuild component
      for (i = 0; i < models.length; i++) {
        if (i === index) {
          this._createComponentForModel(model);
        }
        this._createComponentForModel(models[i].renderlinemodel, models[i].checked);
      }
    } else {
      // add to the end...
      this._createComponentForModel(model);
    }
  },

  _createComponentForModel: function (model, checked) {
    var tr = this.$.tbody.createComponent({
      tag: 'li'
    }).render();
    //columns added for Automated test
    tr.renderline = tr.createComponent({
      kind: this.renderLine,
      model: model,
      columns: this.columns
    }).render();
    tr.checked = checked;

    model.on('change', function () {
      if (tr.destroyed) {
        return;
      }
      tr.destroyComponents();
      tr.renderline = tr.createComponent({
        kind: this.renderLine,
        model: model
      }).render();
    }, this);

    model.on('selected', function () {
      var selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
      if (this.scrollableTableGroup) {
        var elems = $('.' + this.scrollableTableGroup + '_activeScrollableTable');
        if (elems.length === 0) {
          $('#' + this.id).addClass(this.scrollableTableGroup + '_activeScrollableTable');
        } else if (elems && elems.length ===1 && elems[0].id != this.id) {
          elems.removeClass(this.scrollableTableGroup + '_activeScrollableTable');
          $('#' + this.id).addClass(this.scrollableTableGroup + '_activeScrollableTable');
        }
      }
      if (this.listStyle && this.listStyle === 'nonselectablelist') {
        //do nothing in this case, we don't want to select anything
        return;
      } else if (this.listStyle && this.listStyle !== 'checkboxlist') {
        if (tr.destroyed) {
          return;
        }
        if (this.selected) {
          this.selected.addRemoveClass(selectedCssClass, false);
        }
        this.selected = tr;
        this.selected.addRemoveClass(selectedCssClass, true);
        // FIXME: OB.UTIL.makeElemVisible(this.node, this.selected);
      } else if (this.listStyle === 'checkboxlist') {
        if (tr.destroyed) {
          return;
        }
        var components = tr.getComponents();
        if (components.length === 1) {
          if (components[0].$.checkBoxColumn.checked) {
            model.trigger('uncheck', model);
          } else {
            model.trigger('check', model);
          }
        }
      }
    }, this);

    model.on('unselected', function () {
      if (this.selected) {
        this.selected.removeClass('selected', false);
      }
      // FIXME: OB.UTIL.makeElemVisible(this.node, this.selected);
      this.selected = null;
    }, this);

    model.on('check', function () {
      var me = this;
      if (tr && tr.destroyed) {
        return;
      }

      // this line can not be selected
      if (!me.isSelectableLine(model)) {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_LineCanNotBeSelected'));
        return;
      }

      if (this.listStyle === 'checkboxlist') {
        var components = tr.getComponents(),
            allChecked = null,
            checkedLines = [];

        if (components.length === 1) {
          components[0].$.checkBoxColumn.check();
          tr.checked = true;

          _.each(tr.getParent().getComponents(), function (comp) {
            if (comp.checked) {
              checkedLines.push(comp.getComponents()[0].model);
              if (allChecked !== false) {
                allChecked = true;
              }
            } else {
              allChecked = false;
            }
          });

          components[0].doLineChecked({
            action: 'check',
            line: model,
            checkedLines: checkedLines,
            allChecked: allChecked
          });
        }
      }
    }, this);

    model.on('uncheck', function () {
      if (tr && tr.destroyed) {
        return;
      }
      if (this.listStyle === 'checkboxlist') {
        var components = tr.getComponents(),
            checkedLines = [];

        if (components.length === 1) {
          components[0].$.checkBoxColumn.unCheck();
          tr.checked = false;

          _.each(tr.getParent().getComponents(), function (comp) {
            if (comp.checked) {
              checkedLines.push(comp.getComponents()[0].model);
            }
          });

          components[0].doLineChecked({
            action: 'uncheck',
            line: model,
            checkedLines: checkedLines,
            allChecked: false
          });
        }
      }
    }, this);
    return tr;
  }
});