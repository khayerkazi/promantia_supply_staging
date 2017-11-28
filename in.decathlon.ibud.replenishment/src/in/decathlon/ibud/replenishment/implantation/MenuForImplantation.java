package in.decathlon.ibud.replenishment.implantation;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.replenishment.ReplenishmentGenerator;

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

public class MenuForImplantation extends DalBaseProcess {
  ImplantationProcessEnhanced implObject = new ImplantationProcessEnhanced();
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

          Warehouse implWr = BusinessEntityMapper.getImplWarehouse(org).getWarehouse();
          Sequence docSeq = implObject.getDocumentSequence(org);

          logmsg = logmsg + implObject.implantationPO(org, implWr, docSeq, processid);

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
