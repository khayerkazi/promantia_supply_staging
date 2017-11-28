package in.decathlon.ibud.replenishment;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class HourlyReplenishment extends DalBaseProcess {

  static ProcessLogger logger;
  static Logger log = Logger.getLogger(EODReplenishment.class);
  ReplenishmentDalUtils replDalUtils = new ReplenishmentDalUtils();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    boolean isSpoon = false;
    Date dateToBeCompared = new Date();
    String processid = bundle.getProcessId();
    ReplenishmentGenerator replenishmentGen = new ReplenishmentGenerator();
    boolean isHourly = false;

    try {
      List<Organization> orgList = replDalUtils.getStoreOrganizations();
      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {
          if (eligibleForRegular(org, dateToBeCompared)) {
            logger.log("Running regular process for Organization " + org.getName() + "   ");
            isSpoon = false;
          } else {
            logger.log(" Regular is not running for org  " + org.getName() + "   ");
            continue;
          }

          String result = replenishmentGen.replenishPurchaseOrders(org, isSpoon, isHourly, false, processid);
          logger.log(result);
        }
      }
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      logger.log(e.toString());
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }
  }

  private boolean eligibleForRegular(Organization org, Date dateToBeCompared) throws ParseException {

    return replDalUtils.timeCheck(org, false, dateToBeCompared);

  }

}
