package in.decathlon.factorytostore.process;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class FactoryToStoreReplenishement extends DalBaseProcess {

  static ProcessLogger logger;
  static Logger log = Logger.getLogger(FactoryToStoreReplenishement.class);
  static SimpleDateFormat inputParser = new SimpleDateFormat("HH:mm");

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    FactToStoreReplenishemntService factToStoreReplenishemntService = new FactToStoreReplenishemntService();
    try {
      factToStoreReplenishemntService.executeReplenishment();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log.error("Replenishment auto error...", e);
      // OBDal.getInstance().rollbackAndClose();
      logger.logln("Error : " + e);
      BusinessEntityMapper.rollBackNlogError(e, bundle.getProcessId(), null);
    }
  }
}
