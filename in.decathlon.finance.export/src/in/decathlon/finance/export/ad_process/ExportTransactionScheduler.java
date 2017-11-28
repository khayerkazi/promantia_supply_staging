package in.decathlon.finance.export.ad_process;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class ExportTransactionScheduler extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    String processid = bundle.getProcessId();
    try {
      String companyxmlLocation = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("compxmldir");

      String ecomxmlLocation = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("ecomxmldir");
      String posxmlLocation = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("posxmldir");

      new GenericExportTransaction().GenericExportTransactionSchedule(bundle, true, "C",
          companyxmlLocation);
      new GenericExportTransaction().GenericExportTransactionSchedule(bundle, true, "S",
          ecomxmlLocation);
      new GenericExportTransaction().GenericExportTransactionSchedule(bundle, true, "SP",
          posxmlLocation);
    } catch (Exception e) {
      BusinessEntityMapper.createErrorLogRecord(e, processid, null);
      e.printStackTrace();
    }

  }

}
