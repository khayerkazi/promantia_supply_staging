package in.decathlon.ibud.replenishment.process;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.replenishment.CSVUtil;
import in.decathlon.ibud.replenishment.EODReplenishment;
import in.decathlon.ibud.replenishment.ReplenishmentDalUtils;
import in.decathlon.ibud.replenishment.ReplenishmentGenerator;
import in.decathlon.ibud.replenishment.bulk.ReplenishmentService;
import in.decathlon.supply.dc.util.AutoDCMails;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class MenuForReplenishment extends DalBaseProcess {
	static ProcessLogger logger;
	static Logger log = Logger.getLogger(EODReplenishment.class);
	static SimpleDateFormat inputParser = new SimpleDateFormat("HH:mm");
	ReplenishmentDalUtils replDalUtils = new ReplenishmentDalUtils();
	final static Properties p = SuppyDCUtil.getInstance().getProperties();

  protected void doExecute(ProcessBundle bundle) throws Exception {
        ReplenishmentService svc = new ReplenishmentService();
        logger = bundle.getLogger();

		try {
			String OrgId = (String) bundle.getParams().get("adOrgId");
		    Organization org = OBDal.getInstance().get(Organization.class, OrgId);
		    String replenishmentType = (String) bundle.getParams().get("replenishmentType");
			svc.executeMenuForReplenishment(org,replenishmentType,logger);
			OBDal.getInstance().commitAndClose();
		} catch (Exception e) {
			log.error("Replenishment Menu error...", e);
			OBDal.getInstance().rollbackAndClose();
			BusinessEntityMapper.rollBackNlogError(e, bundle.getProcessId(), null);
		}

  }
}
