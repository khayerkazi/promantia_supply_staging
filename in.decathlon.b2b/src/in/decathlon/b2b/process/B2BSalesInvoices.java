package in.decathlon.b2b.process;

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
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class B2BSalesInvoices extends DalBaseProcess {
  private static final Logger LOG = Logger.getLogger(B2BSalesInvoices.class);

  @SuppressWarnings("unchecked")
  protected void doExecute(ProcessBundle bundle) throws Exception {
    JSONObject errorObject = new JSONObject();
    String processid = bundle.getProcessId();
    LOG.debug("B2B Sales Invoice Process -- >");
    final OBQuery<Order> salesOrders = OBDal.getInstance().createQuery(
        Order.class,
        "as o where o.organization.id in ('076DD16AEA914588A919422D1C5FF037') and "
            + "o.businessPartner.id='35586321F375451389832DD198CA1DC7' and "
            + "o.salesTransaction =false and o.documentNo like 'B2B%' and "
            + "reinvoice=false and " + "o.dsBpartner!=null and " + "o.documentStatus='CL' "
            + "order by o.orderDate asc");
    salesOrders.setMaxResult(20);
    LOG.debug("Started the process for the 20 records");
    try {

      for (Order order : salesOrders.list()) {
        String orderId = order.getId();
        errorObject.put(SOConstants.RECORD_ID, orderId);
        String documentNo = order.getDocumentNo();
        errorObject.put(SOConstants.recordIdentifier, documentNo);
        @SuppressWarnings("rawtypes")
        final List parameters = new ArrayList();
        parameters.add(orderId);
        LOG.debug("Creating invoice for order " + documentNo);
        final String procedureName = "b2b_salesinvoice_post";
        try {
          String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null)
              .toString();
          LOG.debug("Creating invoice for order " + orderId);

          if (resp.equals("1")) {
            LOG.debug("Sales invoice created for Order: " + documentNo + ", order id: " + orderId);
            LOG.debug("Response :" + resp);

            order.setReinvoice(true);

            // Updating the c_order records with batch numbers
            OBDal.getInstance().save(order);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          } else {
            LOG.error("Error in creating Invoice created for Order  = " + documentNo
                + ", order id= " + orderId);
            BusinessEntityMapper.rollBackNlogError(null, processid, errorObject);
          }
        } catch (Exception e) {
          LOG.error("Error in creating Invoice created for Order  = " + documentNo + ", order id= "
              + orderId);
          LOG.error("Error :", e);
          BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
        }
      }

    } catch (Exception e) {
      LOG.debug("Error in B2B Sales Invoice Process :", e);
      BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
    }
    LOG.debug("B2B Sales Invoice Process <-- ");
  }

}