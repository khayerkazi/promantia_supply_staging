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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.retail.discounts.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks in M_Offer table
 * 
 */
public class MOfferEventHandler extends EntityPersistenceEventObserver {
  private static Logger log = LoggerFactory.getLogger(MOfferEventHandler.class);
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      PriceAdjustment.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes
  EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final PriceAdjustment disc = (PriceAdjustment) event.getTargetInstance();
    if (!checkXYMandatory(disc)) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBDISC_DiscXYMandatoryCheck", OBContext.getOBContext().getLanguage().getLanguage()));
    }
  }

  public void onNew(@Observes
  EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final PriceAdjustment disc = (PriceAdjustment) event.getTargetInstance();
    if (!checkXYMandatory(disc)) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBDISC_DiscXYMandatoryCheck", OBContext.getOBContext().getLanguage().getLanguage()));
    }
  }

  public void onDelete(@Observes
  EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  private boolean checkXYMandatory(PriceAdjustment disc) {
    if (("312D41071ED34BA18B748607CA679F44".equals(disc.getDiscountType().getId()) || "E08EE3C23EBA49358A881EF06C139D63"
        .equals(disc.getDiscountType().getId()))
        && (disc.getOBDISCX() == null || disc.getOBDISCY() == null)) {
      return false;
    }
    return true;
  }
}
