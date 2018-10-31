package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class PriceListSyncClient extends DalBaseProcess {

  private final static Logger log = Logger.getLogger(PriceListSyncClient.class);
  private static ProcessLogger logger;
  static JSONWebServiceInvocationHelper jsonWSHandler = new JSONWebServiceInvocationHelper();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      String wsName = "in.decathlon.ibud.masters.PriceListWS";
      logger.log("Pulling prices \n");
      jsonWSHandler.sendGetrequest(true, "PriceList", wsName, processid, logger);
      logger.log("Pulling prices completed \n");
      BusinessEntityMapper.setLastUpdatedTime(updated, "PriceList");
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
    }

  }

  /**
   * @returns list of active organizations which are of type store
   * @throws JSONException
   */
  private List<Organization> getStoreOrgnizations() throws JSONException {
    OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, true));
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_CLIENT, OBContext.getOBContext()
        .getCurrentClient()));
    List<Organization> orgList = orgCriteria.list();
    return orgList;
  }

}
