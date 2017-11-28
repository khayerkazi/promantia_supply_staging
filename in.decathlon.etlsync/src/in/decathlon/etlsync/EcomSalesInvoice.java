package in.decathlon.etlsync;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class EcomSalesInvoice extends DalBaseProcess {

  private ProcessLogger logger;
  static Logger log4j = Logger.getLogger(EcomSalesInvoice.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    logger = bundle.getLogger();
    JSONObject errorObject = new JSONObject();
    String processid = bundle.getProcessId();
    final OBQuery<Order> salesOrders = OBDal
        .getInstance()
        .createQuery(
            Order.class,
            "as o where "
                + "o.documentType.id = "
                + "case when o.documentNo like '%CAR%' then '808F8818F724497D94282AC83493F394' "
                + "else '1A9DF993701946CA96CE7AA97BDAB2C0' end and"
                // +
                // "o.organization.id in ('B2D0E3B212614BA6989ADCA3074FC423','076DD16AEA914588A919422D1C5FF037') and"

                /*
                 * B2B organization id removed from query : separation of B2B sales invoices from
                 * Ecom Sales Invoices
                 */

                + " o.organization.id in ('B2D0E3B212614BA6989ADCA3074FC423') and"
                + " o.businessPartner.id='35586321F375451389832DD198CA1DC7' and"
                + " o.salesTransaction =false  and reinvoice=false and o.dsBpartner!=null and o.documentStatus!='DR' order by o.orderDate asc");
    salesOrders.setMaxResult(100);

    log4j.debug("Started the process for the 100 records");
    logger.log("Started the process for the 100 records");

    for (Order order : salesOrders.list()) {
      String orderId = order.getId();
      errorObject.put(SOConstants.RECORD_ID, orderId);
      String documentNo = order.getDocumentNo();
      errorObject.put(SOConstants.recordIdentifier, documentNo);
      final List parameters = new ArrayList();
      parameters.add(orderId);
      final String procedureName = "sync_ecominvoice_post3";
      try {
        String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null)
            .toString();
        log4j.debug("....resp....." + resp);

        if (resp.equals("1")) {
          log4j.debug("Procedure called successfully for the C_Order ID = " + orderId);
          logger.log("Procedure called successfully for the C_Order ID = " + orderId);

          order.setReinvoice(true);

          // Updating the c_order records with batch numbers
          OBDal.getInstance().save(order);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        } else {
          log4j.debug("Procedure not called successfully for the C_Order ID = " + orderId);
          logger.log("Procedure not called successfully for the C_Order ID = " + orderId);
          BusinessEntityMapper.rollBackNlogError(null, processid, errorObject);
        }
      } catch (Exception e) {
        log4j
            .debug("@etlsync sync_ecominvoice_post3 Procedure not called successfully for the C_Order ID = "
                + orderId
                + "Document Number = "
                + documentNo
                + " Exception  for sync_ecominvoice_post3" + e);
        logger
            .log("@etlsync sync_ecominvoice_post3 Procedure not called successfully for the C_Order ID = "
                + orderId
                + "Document Number = "
                + documentNo
                + " Exception  for sync_ecominvoice_post3" + e);
        BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
        // throw e;
      }
    }

  }
}