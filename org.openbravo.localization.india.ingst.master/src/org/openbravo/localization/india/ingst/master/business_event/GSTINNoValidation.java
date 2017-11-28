package org.openbravo.localization.india.ingst.master.business_event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.localization.india.ingst.master.data.GstIdentifierMaster;
import org.openbravo.model.common.geography.Region;

public class GSTINNoValidation extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      GstIdentifierMaster.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;

  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    String gstin = ((GstIdentifierMaster) event.getTargetInstance()).getUidno();
    String panNo = ((GstIdentifierMaster) event.getTargetInstance()).getPan();
    Region regionId = ((GstIdentifierMaster) event.getTargetInstance()).getRegion();
    if ((regionId != null || !(regionId.toString().equalsIgnoreCase(null)))
        && (panNo != null || !(panNo.equalsIgnoreCase(null)))) {

      String stateCode = getStateCode(regionId);
      if (gstin.length() == 15) {
        if (!(gstin.subSequence(0, 2).equals(stateCode))) {
          throw new OBException(OBMessageUtils.messageBD("INGST_WrongGSTINNOEntered"));

        }
      } else {
        throw new OBException(OBMessageUtils.messageBD("INGST_LengthGSTINNO"));
      }
      if (panNo.length() == 10) {
        if (!(gstin.subSequence(2, 12).equals(panNo))) {
          throw new OBException(OBMessageUtils.messageBD("INGST_WrongPANNOEntered"));
        }
      } else {
        throw new OBException(OBMessageUtils.messageBD("INGST_LengthPANNO"));
      }
    }
  }

  private String getStateCode(Region regionId) {
    Region regionObj = OBDal.getInstance().get(Region.class, regionId.getId());
    String stateCode = regionObj.getIngstStatecode();
    return stateCode;

  }

  // On save/insert event ends

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    String gstin = ((GstIdentifierMaster) event.getTargetInstance()).getUidno();
    String panNo = ((GstIdentifierMaster) event.getTargetInstance()).getPan();
    Region regionId = ((GstIdentifierMaster) event.getTargetInstance()).getRegion();
    if ((regionId != null || !(regionId.toString().equalsIgnoreCase(null)))
        && (panNo != null || !(panNo.equalsIgnoreCase(null)))) {
      String stateCode = getStateCode(regionId);

      if (!(gstin.subSequence(0, 2).equals(stateCode))) {
        throw new OBException(OBMessageUtils.messageBD("INGST_WrongGSTINNOEntered"));

      }

      if (!(gstin.subSequence(2, 12).equals(panNo))) {
        throw new OBException(OBMessageUtils.messageBD("INGST_WrongPANNOEntered"));
      }

    }

  } // On update event ends

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

  }

} // Delete Event End

