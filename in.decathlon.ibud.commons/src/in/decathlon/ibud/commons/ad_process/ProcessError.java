package in.decathlon.ibud.commons.ad_process;

import in.decathlon.ibud.commons.data.IbdcomErrorLog;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class ProcessError extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      System.out.println("hiiii inside ad process");
      String errorId = (String) bundle.getParams().get("Ibdcom_Errors_ID");
      String processComments = (String) bundle.getParams().get("processsComment");
      IbdcomErrorLog errorRecord = OBDal.getInstance().get(IbdcomErrorLog.class, errorId);
      errorRecord.setProcessed(true);
      User curUser = OBContext.getOBContext().getUser();
      errorRecord.setProcessedby(curUser);
      errorRecord.setProcessComments(processComments);
      OBDal.getInstance().save(errorRecord);
      SessionHandler.getInstance().commitAndClose();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
