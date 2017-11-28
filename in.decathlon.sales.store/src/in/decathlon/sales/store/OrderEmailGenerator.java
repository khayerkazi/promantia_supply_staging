package in.decathlon.sales.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.sales.store.data.DcssPendingDocumentMail;

import java.util.List;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class OrderEmailGenerator extends DalBaseProcess {

  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    logger = bundle.getLogger();
    // logger.log("Starting background product revenue calculation. Loop ");

    processMailQueue(bundle);
    bundle.setResult(COMPLETE);
  }

  private void processMailQueue(ProcessBundle bundle) throws JSONException  {
	  JSONObject errorObject = new JSONObject();
    OBCriteria<DcssPendingDocumentMail> filter = OBDal.getInstance().createCriteria(
        DcssPendingDocumentMail.class);
    filter.add(Restrictions.eq(DcssPendingDocumentMail.PROPERTY_ALERTSTATUS, "N"));
    if (filter.list().size() == 0) {
      logger.log("Starting background product revenue calculation. Loop ");
    } else {
      List<DcssPendingDocumentMail> pendingList = filter.list();
      OBContext.setAdminMode(true);
      for (DcssPendingDocumentMail dm : pendingList) {
        String custName = dm.getCustomerName() == null || dm.getCustomerName().equals("") ? "Anonymous Customer"
            : dm.getCustomerName();

        String orderId = dm.getSalesOrder().getId();
        String custEmail = dm.getEmail();
       
        try {
          new SendEmailUtility().sendEmail(orderId, custName, custEmail,dm.getId(),bundle);
          //dm.setAlertStatus("Y");
        } catch (ServletException e) {
        	  errorObject.put(SOConstants.RECORD_ID, dm.getId());
        	  errorObject.put(SOConstants.recordIdentifier, dm.getId());
        	  errorObject.put(SOConstants.TABLE_NAME, "");
              logger.log("sendEmail not working." + e.getMessage());
              BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), null);

        } catch (Exception e) {
         
          errorObject.put(SOConstants.RECORD_ID, dm.getId());
      	  errorObject.put(SOConstants.recordIdentifier, dm.getId());
      	  errorObject.put(SOConstants.TABLE_NAME, "");
          e.printStackTrace();
          BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), errorObject);
          logger.log("sendEmail not working." + e.getMessage());

        }

      }
      OBContext.restorePreviousMode();
    }

  }

}
