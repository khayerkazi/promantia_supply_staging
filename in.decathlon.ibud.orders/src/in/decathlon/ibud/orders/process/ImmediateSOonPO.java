package in.decathlon.ibud.orders.process;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.server.CreateJsonPO;
import in.decathlon.ibud.orders.server.SendPOProcess;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLMinmax;

/*
 * 
 * Books the store requisition(Purchase order) 
 * and requests supply to create sales order
 * 
 */
public class ImmediateSOonPO extends DalBaseProcess {

  private static final Logger log = Logger.getLogger(ImmediateSOonPO.class);

  OBError message = new OBError();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    String ordId = null;
    Order ord = null;
    try {

      log.debug("Started process to book store requisiton with params" + bundle.getParams());
      ordId = (String) bundle.getParams().get("C_Order_ID");
      ord = OBDal.getInstance().get(Order.class, ordId);
      if (!ord.isIbdoCreatingso()) {
        //ord.setIbdoCreatingso(true);
        OBDal.getInstance().save(ord);
        OBDal.getInstance().flush();
        String bpId = ord.getBusinessPartner().getId();

        if (BusinessEntityMapper.isSupplyBusinessPartnerConfigured(bpId)) {
          boolean successResult = processRequest(ord);

          if (successResult) {
            ord.setIbdoCreateso(true);
            ord.setIbdoCreatingso(true);
            OBDal.getInstance().save(ord);
            log.info("Success");
            message.setType("Success");
            message.setTitle("Process completed");
            message.setMessage("Successfully booked Purchase order and created Sales order");

          } else {
            ord.setIbdoCreatingso(false);
            OBDal.getInstance().save(ord);
            message.setType("Error");
            message.setTitle("Process Failed");
            message.setMessage("Insufficient Stock");
            log.info("failure on order doc no - " + ord.getDocumentNo());
          }
        }

        else {
          ord.setIbdoCreatingso(false);
          log.error("Business partner is not associated with the organisation"
              + ord.getOrganization().getName());
          message.setType("Error");
          message.setTitle("PO Creation failed");
          message.setMessage("Business partner is not associated with organisation"
              + ord.getOrganization().getName());

        }
        OBDal.getInstance().save(ord);
      } else {
        log.info("SO has been already created for this order: " + ord.getDocumentNo());
      }

    } catch (Exception e) {
      if (ord != null) {
        ord.setIbdoCreatingso(false);
        OBDal.getInstance().save(ord);
      }
      log.error(e.getMessage() + " Order id -" + ordId, e);
      message.setType("Error");
      message.setMessage("Error from supply for doc no - " + e.getMessage());
    } finally {
      bundle.setResult(message);

    }

  }

  public boolean processRequest(Order ord) throws Exception {
    log.debug("In Process Request method of ImmedeateSOonPO class");
    SendPOProcess poProcess = new SendPOProcess();
    final CreateJsonPO createJsonPO = new CreateJsonPO();
    String ordId = ord.getId();
    checkforDirectDelivery(ord);
    if (ord.getDocumentStatus().equals(SOConstants.DraftDocumentStatus)) {
      Date StartBook = new Date();
      // purchaseOrderBookProcess(ord);
      Date endBook = new Date();
      log.debug(", " + SOConstants.perfOrmanceEnhanced + " PO Booking completed ,"
          + ord.getDocumentNo() + ", " + StartBook + ", " + endBook + ", "
          + (endBook.getTime() - StartBook.getTime()) / 1000);

    }
    SessionHandler.getInstance().commitAndStart();
    JSONObject orders = createJsonPO.generateJsonPO(true, ordId, 1);
    Date sendSupply = new Date();
    boolean result = poProcess.sendOrdersAndProcessRequest(orders);
    if (result) {
      purchaseOrderBookProcess(ord);
    }
    Date recieveSupply = new Date();
    log.debug(", " + SOConstants.perfOrmanceEnhanced + " Client side SO Webservice time ,"
        + ord.getDocumentNo() + ", " + sendSupply + ", " + recieveSupply + ","
        + (recieveSupply.getTime() - sendSupply.getTime()) / 1000);
    return result;
  }

  private void purchaseOrderBookProcess(Order ord) throws Exception {
    try {
      String ordId = ord.getId();
      log.debug("booking order " + ord.getDocumentNo() + " having action "
          + ord.getDocumentAction());
      BusinessEntityMapper.executeProcess(ordId, "104", "SELECT * FROM c_order_post(?)");
      OBDal.getInstance().refresh(ord);
    } catch (Exception e) {
      log.error("Error while executing stored procedure c_order_post for order doc "
          + ord.getDocumentNo() + e.getMessage());
      throw e;
    }
  }

  private void checkforDirectDelivery(Order ord) {
    for (OrderLine line : ord.getOrderLineList()) {
      OBCriteria<CLMinmax> criteriaOnCLMinmax = OBDal.getInstance().createCriteria(CLMinmax.class);
      criteriaOnCLMinmax
          .add(Restrictions.eq(CLMinmax.PROPERTY_ORGANIZATION, line.getOrganization()));
      criteriaOnCLMinmax.add(Restrictions.eq(CLMinmax.PROPERTY_PRODUCT, line.getProduct()));
      criteriaOnCLMinmax.add(Restrictions.eq(CLMinmax.PROPERTY_FACSTISDIRECTDELIVERY, true));
      List<CLMinmax> clList = criteriaOnCLMinmax.list();
      if (clList != null && clList.size() == 1) {
        line.setFACSTIsDirectDelivery(true);
        ord.setFacstIsDirectDelivery(true);
        OBDal.getInstance().save(line);
        OBDal.getInstance().save(ord);
      }
    }

  }

}