package in.decathlon.ibud.shipment.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;

import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class ShipmentProcess extends DalBaseProcess {
  public static final Logger log = Logger.getLogger(ShipmentProcess.class);
  ProcessLogger logger;
  static JSONWebServiceInvocationHelper shipmentHandler = new JSONWebServiceInvocationHelper();

  // CreateGRNService createGRNService = new CreateGRNService();
  JSONObject jsonObj;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try {
      List<Organization> orgList = getStoreOrgnizations();
      log.debug("....doExecute..........");
      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {
          log.debug("Pulling GS for organization " + org.getName());
          log.debug("size of org list " + orgList.size());
          jsonObj = shipmentHandler.sendGetrequest(true, "ShipmentInOut",
              "in.decathlon.ibud.shipment.ShipmentWS", "orgId=" + org.getId(), processid, logger);
          log.debug(" json object is " + jsonObj);
          logger.log("finished pulling GS for organization " + org.getName());
        }

      }
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      log.error(e);
      logger.log(e.getMessage());
      throw e;
    }
  }

  private List<Organization> getStoreOrgnizations() throws JSONException {
    OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
    // orgCriteria.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, true));
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_CLIENT, OBContext.getOBContext()
        .getCurrentClient()));
    List<Organization> orgList = orgCriteria.list();
    return orgList;
  }
}