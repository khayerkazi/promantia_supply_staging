package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class CreatePOinDR extends DalBaseProcess {

  static ProcessLogger logger;
  static Logger log = Logger.getLogger(EODReplenishment.class);
  static SimpleDateFormat inputParser = new SimpleDateFormat("HH:mm");
  ReplenishmentDalUtils replDalUtils = new ReplenishmentDalUtils();
  final static Properties p = SuppyDCUtil.getInstance().getProperties();
  Connection conn = null;

  @SuppressWarnings("static-access")
  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    boolean isSpoon = false;
    Date dateToBeCompared = new Date();
    final Map<String, Long> orgs = new HashMap<String, Long>();
    ReplenishmentGenerator replenishmentGen = new ReplenishmentGenerator();
    boolean isHourly = false;
    CSVUtil csvUtil = new CSVUtil();
    IbudConfig config = new IbudConfig();

    try {
      List<Organization> orgList = replDalUtils.getStoreOrganizations();

      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {
          if (eligibleForSpoon(org, dateToBeCompared)) {
            logger.log("\n Running spoon process for Organization " + org.getName() + "   ");
            isSpoon = true;
          } else if (eligibleForRegular(org, dateToBeCompared)) {
            logger.log("\n Running regular process for Organization " + org.getName() + "   ");
            isSpoon = false;
          } else {
            logger.log("\n Spoon/Regular is not running for org  " + org.getName() + "   ");
            continue;
          }
          String result = replenishmentGen.replenishPurchaseOrders(org, isSpoon, isHourly, true,
              processid);

          logger.log("\n" + result);
        }

      }
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      logger.log(e.toString());
      OBDal.getInstance().rollbackAndClose();
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      // throw e;
    }
  }

  private boolean eligibleForSpoon(Organization org, Date dateToBeCompared) throws ParseException {

    return replDalUtils.timeCheck(org, true, dateToBeCompared);

  }

  private boolean eligibleForRegular(Organization org, Date dateToBeCompared) throws ParseException {

    return replDalUtils.timeCheck(org, false, dateToBeCompared);

  }

}
