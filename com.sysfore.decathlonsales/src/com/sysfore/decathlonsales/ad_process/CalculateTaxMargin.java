package com.sysfore.decathlonsales.ad_process;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

public class CalculateTaxMargin extends DalBaseProcess {
  private static final String SerialNo = null;
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();

    try {
      callStoredProcedure();
      logger.log("Tax amount calculated");
    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    }
  }

  private void callStoredProcedure() throws Exception {
    final List parameters = new ArrayList();
    parameters.add("Tax");
    final String procedureName = "ds_updtax_priceadj_ord";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null);
  }
}
