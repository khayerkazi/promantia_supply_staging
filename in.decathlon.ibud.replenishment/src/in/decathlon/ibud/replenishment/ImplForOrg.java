package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLImplantation;

public class ImplForOrg extends DalBaseProcess {
  ImplantationProcess implObject = new ImplantationProcess();
  static ProcessLogger logger;
  OBError message = new OBError();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    final String orgId = (String) bundle.getParams().get("adOrgId");
    String processid = bundle.getProcessId();
    logger = bundle.getLogger();
    String logmsg = "";
    try {
      Organization org = OBDal.getInstance().get(Organization.class, orgId);

      OBCriteria<CLImplantation> implOrg = OBDal.getInstance().createCriteria(CLImplantation.class);
      implOrg.add(Restrictions.eq(CLImplantation.PROPERTY_STOREIMPLANTED, org));
      implOrg.add(Restrictions.eq(CLImplantation.PROPERTY_ISIMPLANTED, false));
      implOrg.setMaxResults(1);
      if (implObject.spoonOrRegularCheckForOrg(org)) {
        if (implOrg.count() >= 1) {

          Warehouse implWr = BusinessEntityMapper.getImplWarehouse(org).getWarehouse();
          Sequence docSeq = ReplenishmentGenerator.getDocSequence(org);

          logmsg = logmsg + implObject.implantationPO(org, implWr, docSeq, processid);

        } else {
          logmsg = logmsg + "No records in implantation table to create PO for the org " + org;
        }
      } else {
        logmsg = logmsg + "Failed to run implantation for " + org
            + " as replenishment process configured .";
      }
      logger.log(logmsg);
      message.setType("Success");
      message.setTitle("Process completed");
      message.setMessage(logmsg);
    } catch (Exception e) {
      logger.log("Error: " + e.toString());
      message.setType("Error");
      message.setMessage(e.getMessage());
      message.setTitle("Implantation Process");
      bundle.setResult(e.toString());
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
    }

    finally {
      bundle.setResult(message);
    }
  }

}
