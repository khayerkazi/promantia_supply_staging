package org.openbravo.localization.india.ingst.master.business_event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.localization.india.ingst.master.data.ingstRateException;

public class GSTProductExceptionRateDelete extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ingstRateException.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    Boolean processedFlag = ((ingstRateException) event.getTargetInstance()).isProcessed();
    if (processedFlag) {

      throw new OBException(OBMessageUtils.messageBD("Ingst_Gst_Product_Processed"));
    }
  }

} // Delete Event End

