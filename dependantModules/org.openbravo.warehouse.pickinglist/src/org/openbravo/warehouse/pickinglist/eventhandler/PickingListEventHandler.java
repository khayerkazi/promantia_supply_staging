/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.pickinglist.eventhandler;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.warehouse.pickinglist.PickingList;

public class PickingListEventHandler extends EntityPersistenceEventObserver {
  protected Logger logger = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      PickingList.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes
  EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    PickingList picking = (PickingList) event.getTargetInstance();
    Entity entPickingList = entities[0];
    Property propDocType = entPickingList.getProperty(PickingList.PROPERTY_DOCUMENTTYPE);
    DocumentType oldDocType = (DocumentType) event.getPreviousState(propDocType);
    if (!oldDocType.getId().equals(picking.getDocumentType().getId())) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_CannotChangeDocumentType", false));
    }

  }

  public void onCreate(@Observes
  EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    PickingList picking = (PickingList) event.getTargetInstance();
    if (picking.getDocumentType().isOBWPLUseOutbound() && picking.getOutboundStorageBin() == null) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_OutboundLocatorNotDefined", false));
    }
  }

}
