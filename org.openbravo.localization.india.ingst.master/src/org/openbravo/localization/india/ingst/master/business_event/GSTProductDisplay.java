package org.openbravo.localization.india.ingst.master.business_event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.localization.india.ingst.master.data.INGSTGSTProductCode;

public class GSTProductDisplay extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      INGSTGSTProductCode.ENTITY_NAME) };
  Property p = entities[0].getProperty(INGSTGSTProductCode.PROPERTY_DISPLAY);

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;

  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    // String strIngstCode =((INGSTGSTProductCode)event.getTargetInstance()).getId();
    INGSTGSTProductCode INGSTGSTProductCode = (INGSTGSTProductCode) event.getTargetInstance();
    String name = INGSTGSTProductCode.getName();
    String value = INGSTGSTProductCode.getValue();
    String display = name + "-" + value;
    // INGSTGSTProductCode.set("display", name"-"value);
    event.setCurrentState(p, display);

    // OBDal.getInstance().save(INGSTGSTProductCode);
    // OBDal.getInstance().flush();

  }

  /*
   * isEstimationLine estLine=OBDal.getInstance().get(isEstimationLine.class, strEstLineId);
   * estLine.setSalesprice(quoteprice); estLine.setSalesamt(quoteamt);
   * OBDal.getInstance().save(estLine);
   */

  // On save/insert event ends

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    INGSTGSTProductCode INGSTGSTProductCode = (INGSTGSTProductCode) event.getTargetInstance();
    String name = INGSTGSTProductCode.getName();
    String value = INGSTGSTProductCode.getValue();
    // INGSTGSTProductCode.set("display", name"-"value);
    String display = name + "-" + value;

    event.setCurrentState(p, display);

  } // On update event ends

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

  }

  // Delete Event End

  public static String getHSNTypeValue(String type, String value) {
    if (type.equalsIgnoreCase("SAC")) {
      if (!value.substring(0, 1).matches("[0-9]"))
        value = value.replace(value.charAt(0), 'S');
      else
        value = "S" + value;
    } else {
      if (!value.substring(0, 1).matches("[0-9]"))
        value = value.replace(value.charAt(0), 'G');
      else
        value = "G" + value;
    }
    return value;
  }
}