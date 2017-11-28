package org.openbravo.pricing.scalablelevelpricing.eventhandlers;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.DalConnectionProvider;

public class CheckGroupedProduct extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ProductPrice.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes
  EntityUpdateEvent event) {
    if (isValidEvent(event)) {
      final Entity productEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);
      if (productEntity.hasProperty("obposGroupedproduct")) {
        final ProductPrice prodPrice = (ProductPrice) event.getTargetInstance();
        if (prodPrice.getAlgorithm().equals("SLP_algorithm")
            && prodPrice.getProduct().isObposGroupedproduct() == false) {
          String language = OBContext.getOBContext().getLanguage().getLanguage();
          ConnectionProvider conn = new DalConnectionProvider(false);
          throw new OBException(Utility.messageBD(conn, "SLP_NotGroupedProduct", language));
        }
      }
      return;
    }
  }

  public void onSave(@Observes
  EntityNewEvent event) {
    if (isValidEvent(event)) {
      final Entity productEntity = ModelProvider.getInstance().getEntity(Product.ENTITY_NAME);
      if (productEntity.hasProperty("obposGroupedproduct")) {
        final ProductPrice prodPrice = (ProductPrice) event.getTargetInstance();
        if (prodPrice.getAlgorithm().equals("SLP_algorithm")
            && prodPrice.getProduct().isObposGroupedproduct() == false) {
          String language = OBContext.getOBContext().getLanguage().getLanguage();
          ConnectionProvider conn = new DalConnectionProvider(false);
          throw new OBException(Utility.messageBD(conn, "SLP_NotGroupedProduct", language));
        }
      }
      return;
    }
  }
}