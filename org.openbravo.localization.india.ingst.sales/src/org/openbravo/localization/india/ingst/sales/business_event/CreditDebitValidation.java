package org.openbravo.localization.india.ingst.sales.business_event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;

public class CreditDebitValidation extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;

  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    Invoice invoiceObj = (Invoice) event.getTargetInstance();
    
    String uidno ="";
    if(invoiceObj.getIngststOrgGstmaster()!=null){
      uidno = invoiceObj.getIngststOrgGstmaster().getUidno();}
   
    
    String documentCategory = invoiceObj.getTransactionDocument().getDocumentCategory();
    if (!(uidno.equals(""))
        && (documentCategory.equalsIgnoreCase("APC") || documentCategory.equalsIgnoreCase("ARC"))) {

   /*   if (invoiceObj.getIngstrReason() == null) {
        throw new OBException(OBMessageUtils.messageBD("INGSTST_EmptyReason"));
      }
      if (invoiceObj.getIngstrInvoiceref() == null) {
        throw new OBException(OBMessageUtils.messageBD("INGSTST_EmptyCreditDebitNote"));
      }
      if (invoiceObj.getIngstrReferenceDate() == null) {
        throw new OBException(OBMessageUtils.messageBD("INGSTST_EmptyCreditDebitDate"));

      }*/

    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    
    Invoice invoiceObj = (Invoice) event.getTargetInstance();
    String uidno ="";
    if(invoiceObj.getIngststOrgGstmaster()!=null){
      uidno = invoiceObj.getIngststOrgGstmaster().getUidno();}
    String documentCategory = invoiceObj.getTransactionDocument().getDocumentCategory();

    if (!(uidno.equals(""))
        && (documentCategory.equalsIgnoreCase("APC") || documentCategory.equalsIgnoreCase("ARC"))) {

      /*if (invoiceObj.getIngstrReason() == null) {
        throw new OBException(OBMessageUtils.messageBD("INGSTST_EmptyReason"));
      }
      if (invoiceObj.getIngstrInvoiceref() == null) {
        throw new OBException(OBMessageUtils.messageBD("INGSTST_EmptyCreditDebitNote"));

      }

      if (invoiceObj.getIngstrReferenceDate() == null) {
        throw new OBException(OBMessageUtils.messageBD("INGSTST_EmptyCreditDebitDate"));

      }*/

    }

  } // On update event ends

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

  }

}
