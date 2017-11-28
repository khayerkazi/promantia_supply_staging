package in.decathlon.etlsync;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class PosSalesInvoiceCreate extends DalBaseProcess {

  private ProcessLogger logger;

  static Logger log4j = Logger.getLogger(PosSalesInvoiceCreate.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    logger = bundle.getLogger();

    final String docType = "511A9371A0F74195AA3F6D66C722729D";

    final OBQuery<Order> salesOrders = OBDal
        .getInstance()
        .createQuery(
            Order.class,
            "as o where o.documentType.id='"
                + docType
                + "' and o.reinvoice=false and o.documentStatus='CO' and o.salesTransaction = 'Y' order by o.orderDate asc");
    salesOrders.setMaxResult(100);

    log4j.debug("Started the process for the 100 records");
    logger.log("Started the process for the 100 records");
    try {

      for (Order order : salesOrders.list()) {

        final List parameters = new ArrayList();
        parameters.add(order.getId());
        final String procedureName = "sync_invoice_create";
        String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null)
            .toString();
        if (resp.equals("1")) {
          log4j.debug("Procedure called successfully for the C_Order ID = " + order.getId());
          logger.log("Procedure called successfully for the C_Order ID = " + order.getId());

          order.setReinvoice(true);

          // Updating the c_order records with batch numbers
          OBDal.getInstance().save(order);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        } else {
          log4j.debug("Procedure not called successfully for the C_Order ID = " + order.getId());
          logger.log("Procedure not called successfully for the C_Order ID = " + order.getId());
        }

      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
