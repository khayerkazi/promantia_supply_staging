package in.decathlon.journalentry;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;

import com.sysfore.sankalpcrm.RCCompany;

public class ThirdNumForCompany extends EntityPersistenceEventObserver {

  protected Logger logger = Logger.getLogger(ThirdNumForCompany.class);
  public static Entity[] entities = { ModelProvider.getInstance().getEntity(RCCompany.class) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final RCCompany company = (RCCompany) event.getTargetInstance();

    final Entity companyEntity = ModelProvider.getInstance().getEntity(RCCompany.ENTITY_NAME);
    final Property companyProperty = companyEntity.getProperty(RCCompany.PROPERTY_DFEXTHIRDNUM);

    // Create Object for Document Sequence
    // Get NextAssigned Number
    // Populate it in Third NUm filed
    // Add 1 to NextAssigned Number and save it in Sequence again
    Sequence seq = null;
    OBCriteria<Sequence> seqCriteri = OBDal.getInstance().createCriteria(Sequence.class);
    seqCriteri.add(Restrictions.eq(Sequence.PROPERTY_NAME, "ThirdNumber"));
    seq = seqCriteri.list().get(0);
    Long newSeqNo = (seq.getNextAssignedNumber());
    event.setCurrentState(companyProperty, newSeqNo);

    // Update Sequence with the nextAssignedNumber
    seq.setNextAssignedNumber(newSeqNo + 1);
    // company.setDfexThirdNum(newSeqNo);
    OBDal.getInstance().save(seq);
    // OBDal.getInstance().save(company);

  }

}