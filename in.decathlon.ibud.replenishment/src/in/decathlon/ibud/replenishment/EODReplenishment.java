package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.replenishment.bulk.ReplenishmentService;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class EODReplenishment extends DalBaseProcess {
	static ProcessLogger logger;
	static Logger log = Logger.getLogger(EODReplenishment.class);
	static SimpleDateFormat inputParser = new SimpleDateFormat("HH:mm");
	ReplenishmentDalUtils replDalUtils = new ReplenishmentDalUtils();
	final static Properties p = SuppyDCUtil.getInstance().getProperties();

	protected void doExecute(ProcessBundle bundle) throws Exception {
		ReplenishmentService svc = new ReplenishmentService();
		logger = bundle.getLogger();

		try {
			svc.executeReplenishment(logger);
			OBDal.getInstance().commitAndClose();
		} catch (Exception e) {
			log.error("Replenishment auto error...", e);
			e.printStackTrace();
		    logger.log(e.toString());
			OBDal.getInstance().rollbackAndClose();
			BusinessEntityMapper.rollBackNlogError(e, bundle.getProcessId(), null);
		}
	}



}
