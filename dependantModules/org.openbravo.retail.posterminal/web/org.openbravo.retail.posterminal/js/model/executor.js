/*
 ************************************************************************************
 * Copyright (C) 2012-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
/*global Backbone,console,_*/

/**
 * OB.Model.Executor provides a mechanism to execute actions synchronously even each of
 * these actions are not synchronous. It is managed with two queues: one for events and
 * another one for actions. Each event has a series of actions to be executed synchronously,
 * when all actions in the event are finished, next event is started.
 */

OB.Model.Executor = Backbone.Model.extend({
  defaults: {
    executing: false
  },

  initialize: function () {
    var eventQueue = new Backbone.Collection();
    this.set('eventQueue', eventQueue);
    this.set('actionQueue', new Backbone.Collection());

    eventQueue.on('add', function () {
      if (!this.get('executing')) {
        // Adding an event to an empty queue, firing it
        this.nextEvent();
      }
    }, this);
  },

  addEvent: function (event, replaceExistent) {
    var evtQueue = this.get('eventQueue'),
        currentEvt, actionQueue, currentExecutionQueue;
    if (replaceExistent && evtQueue) {
      currentEvt = this.get('currentEvent');
      evtQueue.where({
        id: event.get('id')
      }).forEach(function (evt) {
        if (currentEvt === evt) {
          this.set('eventQueue');
          actionQueue.remove(actionQueue.models);
        }
        evtQueue.remove(evt);
        currentExecutionQueue = (this.get('exec') || 0) - 1;
        this.set('exec', currentExecutionQueue);
      }, this);
    }

    this.set('exec', (this.get('exec') || 0) + 1);

    event.on('finish', function () {
      var currentExecutionQueue = (this.get('exec') || 0) - 1;
      this.set('exec', currentExecutionQueue);
      OB.info('event execution time', (new Date().getTime()) - event.get('start'), currentExecutionQueue);
      if (currentExecutionQueue === 0 && event.get('receipt')) {
        event.get('receipt').trigger('eventExecutionDone');
      }
    }, this);

    evtQueue.add(event);
  },
  preEvent: function () {
    // Logic to implement before the event is created
  },
  postEvent: function () {
    // Logic to implement after the event is created
  },
  nextEvent: function () {
    var evt = this.get('eventQueue').shift(),
        previousEvt = this.get('currentEvent');
    if (previousEvt) {
      previousEvt.trigger('finish');
      this.postEvent();
    }
    if (evt) {
      this.preEvent();
      this.set('executing', true);
      this.set('currentEvent', evt);
      evt.set('start', new Date().getTime());
      evt.on('actionsCreated', function () {
        this.preAction(evt);
        this.nextAction(evt);
      }, this);
      this.createActions(evt);
    } else {
      this.set('executing', false);
      this.set('currentEvent', null);
    }
  },

  preAction: function (event) {
    // actions executed before the actions for the event
  },

  nextAction: function (event) {
    var action = this.get('actionQueue').shift();
    if (action) {
      action.get('action').call(this, action.get('args'), event);
    } else {
      // queue of action is empty
      this.postAction(event);
      this.nextEvent();
    }
  },

  postAction: function (event) {
    // actions executed after all actions for the event have been executed
  },

  createActions: function (event) {
    // To be implemented by subclasses. It should populate actionQueue with the
    // series of actions to be executed for this event. Note each of the actions
    // is in charge of synchronization by invoking nextAction method.
  }
});

OB.Model.DiscountsExecutor = OB.Model.Executor.extend({
  // parameters that will be used in the SQL to get promotions, in case this SQL is extended, 
  // these parameters might be required to be extended too
  criteriaParams: ['bpId', 'bpId', 'bpId', 'bpId', 'productId', 'productId', 'productId', 'productId'],

  // defines the property each of the parameters in criteriaParams is translated to, in case of
  // different parameters than standard ones this should be extended
  paramsTranslation: {
    bpId: {
      model: 'receipt',
      property: 'bp'
    },
    productId: {
      model: 'line',
      property: 'product'
    }
  },

  convertParams: function (evt, line, receipt, pTrl) {
    var translatedParams = [];
    _.forEach(this.criteriaParams, function (param) {
      var paraTrl, model;

      paraTrl = pTrl[param];
      if (!paraTrl) {
        window.console.error('Not found param to calculate discounts', param);
        return;
      }

      if (paraTrl.model === 'receipt') {
        model = receipt;
      } else if (paraTrl.model === 'line') {
        model = line;
      } else {
        model = evt.get(paraTrl.model);
      }

      translatedParams.push(model.get(paraTrl.property).id);
    });
    return translatedParams;
  },

  createActions: function (evt) {
    var line = evt.get('line'),
        receipt = evt.get('receipt'),
        bpId = receipt.get('bp').id,
        productId = line.get('product').id,
        actionQueue = this.get('actionQueue'),
        me = this,
        criteria, t0 = new Date().getTime(),
        whereClause = OB.Model.Discounts.standardFilter + " AND M_OFFER_TYPE_ID NOT IN (" + OB.Model.Discounts.getManualPromotions() + ")";

    if (!receipt.shouldApplyPromotions() || line.get('product').get('ignorePromotions')) {
      // Cannot apply promotions, leave actions empty
      evt.trigger('actionsCreated');
      return;
    }

    criteria = {
      '_whereClause': whereClause,
      params: this.convertParams(evt, line, receipt, this.paramsTranslation)
    };

    OB.Dal.find(OB.Model.Discount, criteria, function (d) {
      d.forEach(function (disc) {
        actionQueue.add({
          action: me.applyRule,
          args: disc
        });
      });
      evt.trigger('actionsCreated');
    }, function () {
      OB.error('Error getting promotions', arguments);
    });
  },

  applyRule: function (disc, evt) {
    var receipt = evt.get('receipt'),
        line = evt.get('line'),
        rule = OB.Model.Discounts.discountRules[disc.get('discountType')],
        ds, ruleListener;
    if (line.stopApplyingPromotions()) {
      this.nextAction(evt);
      return;
    }

    if (rule && rule.implementation) {
      if (rule.async) {
        // waiting listener to trigger completed to move to next action
        ruleListener = new Backbone.Model();
        ruleListener.on('completed', function (obj) {
          if (obj && obj.alerts) {
            // in the new flow discount, the messages are stored in array, so only will be displayed the first time
            if (OB.POS.modelterminal.hasPermission('OBPOS_discount.newFlow', true)) {
              var localArrayMessages = line.get('promotionMessages') || [];
              localArrayMessages.push(obj.alerts);
              line.set('promotionMessages', localArrayMessages);
            } else {
              OB.UTIL.showAlert.display(obj.alerts);
            }
          }
          ruleListener.off('completed');
          this.nextAction(evt);
        }, this);
      }
      ds = rule.implementation(disc, receipt, line, ruleListener);
      if (ds && ds.alerts) {
        // in the new flow discount, the messages are stored in array, so only will be displayed the first time
        if (OB.POS.modelterminal.hasPermission('OBPOS_discount.newFlow', true)) {
          var localArrayMessages = line.get('promotionMessages') || [];
          localArrayMessages.push(ds.alerts);
          line.set('promotionMessages', localArrayMessages);
        } else {
          OB.UTIL.showAlert.display(ds.alerts);
        }
      }

      if (!rule.async) {
        // done, move to next action
        this.nextAction(evt);
      }
    } else {
      OB.warn('No POS implementation for discount ' + disc.get('discountType'));
      this.nextAction(evt);
    }
  },
  preEvent: function () {
    OB.MobileApp.view.waterfall('onApplyingDiscount');
  },
  postEvent: function () {
    OB.MobileApp.view.waterfall('onAppliedDiscount');
  },
  preAction: function (evt) {
    var line = evt.get('line'),
        order = evt.get('receipt'),
        manualPromotions = [],
        appliedPromotions, appliedPack;

    // Keep discretionary discounts at the beginning, recalculate them based on 
    // new info in line
    appliedPromotions = line.get('promotions');
    if (appliedPromotions) {
      if (line.lastAppliedPromotion() && !line.lastAppliedPromotion().applyNext) {
        manualPromotions.push(line.lastAppliedPromotion());
      } else {
        _.forEach(appliedPromotions, function (promotion) {
          if (promotion.manual) {
            manualPromotions.push(promotion);
          }
        });
      }
    }

    appliedPack = line.isAffectedByPack();
    if (appliedPack) {
      // we need to remove this pack from other lines in order to warranty consistency
      order.get('lines').forEach(function (l) {
        var promos = l.get('promotions'),
            newPromos = [];
        if (!promos) {
          return;
        }

        promos.forEach(function (p) {
          if (p.ruleId !== appliedPack.ruleId) {
            newPromos.push(p);
          }
        });

        l.set('promotions', newPromos);
      });
    }

    if (!line.get('originalOrderLineId')) {
      line.set({
        promotions: null,
        discountedLinePrice: null,
        promotionCandidates: null
      });
    }

    _.forEach(manualPromotions, function (promo) {
      var promotion = {
        rule: new Backbone.Model(promo),

        definition: {
          userAmt: promo.userAmt,
          applyNext: promo.applyNext,
          lastApplied: promo.lastApplied
        },
        alreadyCalculated: true // to prevent loops
      };
      OB.Model.Discounts.addManualPromotion(order, [line], promotion);
    });
  },

  postAction: function (evt) {
    // if new flow of discounts, then discountsApplied is triggered
    if (OB.POS.modelterminal.hasPermission('OBPOS_discount.newFlow', true)) {
      if (this.get('eventQueue').filter(function (p) {
        return p.get('receipt') === evt.get('receipt');
      }).length === 0) {
        evt.get('receipt').trigger('discountsApplied');
      }
    } else {
      evt.get('receipt').calculateGross();
    }

    // Forcing local db save. Rule implementations could (should!) do modifications
    // without persisting them improving performance in this manner.
    if (!evt.get('skipSave') && evt.get('receipt') && evt.get('receipt').get('lines') && evt.get('receipt').get('lines').length > 0) {
      evt.get('receipt').save();
    }
  }
});