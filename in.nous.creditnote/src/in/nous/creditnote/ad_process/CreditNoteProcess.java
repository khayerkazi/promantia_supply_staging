package in.nous.creditnote.ad_process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Expression;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.sankalpcrm.RCCreditNote;

/*
 * This class is used to process Credit Note for existing CRNs.
 */

public class CreditNoteProcess extends DalBaseProcess {
  private ProcessLogger logger;
  static Logger log4j = Logger.getLogger(CreditNoteProcess.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    VariablesSecureApp vars = bundle.getContext().toVars();
    final String language = bundle.getContext().getLanguage();
    try {
      log4j.info("Inside doExecute() of CreditNoteProcess: " + new Date());
      List<Object> param = new ArrayList<Object>();
      // creating CRNs
      String resultMessage = createCRN();
      System.out.println(resultMessage);
      log4j.info(resultMessage);
      logger.log(resultMessage);
      System.out.println("Porting of CRN Process Completed.");
    } catch (Exception e) {
      logger
          .logln(Utility.parseTranslation(bundle.getConnection(), vars, language, e.getMessage()));
    }
  }

  private String createCRN() {
    int i = 0;
    int migrated = 0;
    int notMigrated = 0;
    String result = "";
    String logs = "\n";
    OBContext.setAdminMode();
    log4j.info("Inside createCRN() of CreditNoteProcess: " + new Date());
    try {
      // getting RC_CreditNotes
      OBCriteria<RCCreditNote> obCriteria = OBDal.getInstance().createCriteria(RCCreditNote.class);
      obCriteria.add(Expression.eq(RCCreditNote.PROPERTY_ACTIVE, true));
      obCriteria.add(Expression.eq(RCCreditNote.PROPERTY_PROCESSED, true));
      obCriteria.add(Expression.isNull(RCCreditNote.PROPERTY_NCNISMIGRATE));
      obCriteria.addOrderBy(RCCreditNote.PROPERTY_CREATEDBY, false);
      obCriteria.setMaxResults(2000);
      final List<RCCreditNote> crnList = obCriteria.list();
      System.out.println("crnList size-> " + crnList.size());
      for (RCCreditNote rc : crnList) {
        i++;
        log4j.info("********************Record " + i);
        if (!rc.getOrganization().getName().equals("*")) {
          if (!rc.getRCCreditNoteLineList().isEmpty()) {
            // calling webservice
            CreateCreditNote ws = new CreateCreditNote();
            JSONObject js = ws.processCreditNote(rc);
            logs.concat(logs).concat("\n");
            if (js.toString().contains("success")) {
              // to track the migrated credit notes
             // rc.setNcnIsmigrate(true);
             // OBDal.getInstance().save(rc);
              migrated++;
            } else {
              notMigrated++;
            }
          }// end of null lines check if
        }// end of org check if
        
      }// end of main for loop
      result = Integer.toString(migrated).concat(" RCs migrated successfully and ")
          .concat(Integer.toString(notMigrated).concat(" RCs failed to migrate."));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
