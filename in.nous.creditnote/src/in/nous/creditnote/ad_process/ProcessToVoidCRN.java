package in.nous.creditnote.ad_process;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class ProcessToVoidCRN extends DalBaseProcess {
  private static Logger log = Logger.getLogger(ProcessToVoidCRN.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    Invoice inv = null;
    try {
      // retrieve the parameters from the bundle
      System.out.println("Inside Void Credit Note App->" + bundle.getParams());
      final String cInvoiceNo = (String) bundle.getParams().get("cInvoiceId");
      if (!cInvoiceNo.startsWith("CRN-")) {
        throw new OBException("Credit Note No should start from CRN-");
      }
      OBCriteria<Invoice> obCriteria = OBDal.getInstance().createCriteria(Invoice.class);
      obCriteria.add(Expression.eq(Invoice.PROPERTY_DOCUMENTNO, cInvoiceNo));
      obCriteria.add(Expression.eq(Invoice.PROPERTY_SALESTRANSACTION, true));
      obCriteria.add(Expression.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CO"));
      obCriteria.add(Expression.eq(Invoice.PROPERTY_ORGANIZATION, OBContext.getOBContext()
          .getCurrentOrganization()));

      final List<Invoice> invList = obCriteria.list();
      if (!invList.isEmpty())
        inv = invList.get(0);
      else
        throw new OBException("Invoice DocumentNo [" + cInvoiceNo
            + "] does not exist for current Organization and Warehouse. Please re-enter.");

      // inv = OBDal.getInstance().get(Invoice.class, cInvoiceNo);
      final StringBuilder sb = new StringBuilder();
      String result = "";
      String rmrResult = "";
      String rfcResult = "";
      String invResult = "";

      // inactive and void RFC
      log.debug("Voiding RFC...");
      // System.out.println("Voiding RFC...");
      rfcResult = voidRFC(inv);
      log.info("rfcResult->" + rfcResult);

      if (rfcResult.equals("success")) {
        // Cancelling RMR
        // System.out.println("Cancelling RMR...");
        log.debug("Cancelling RMR...");
        rmrResult = voidRMR(inv.getSalesOrder());
        log.info("rmrResult->" + rmrResult);
      } else {
        result = result.concat(rfcResult);
        final OBError msg = new OBError();
        msg.setType("Error");
        msg.setMessage(result);
        msg.setTitle("Error occurred:");
        bundle.setResult(msg);
      }

      if (rmrResult.equals("success")) {
        // Cancelling Credit Note
        // System.out.println("Cancelling Credit Note...");
        log.debug("Cancelling Credit Note...");
        invResult = voidInvoice(inv);
        log.info("invResult->" + invResult);
      } else {
        result = result.concat(rmrResult);
        final OBError msg = new OBError();
        msg.setType("Error");
        msg.setMessage(result);
        msg.setTitle("Error occurred:");
        bundle.setResult(msg);
      }

      if (invResult.equals("success")) {
        final OBError msg = new OBError();
        result = "Sucessfully Voided Credit Note->";
        sb.append(result.concat(inv.getDocumentNo()));
        msg.setType("Success");
        msg.setTitle("Success Note:");
        msg.setMessage(sb.toString());
        bundle.setResult(msg);
        log.info("Voided Credit Note->" + inv.getDocumentNo());
      } else {
        result = result.concat(invResult);
        final OBError msg = new OBError();
        msg.setType("Error");
        msg.setTitle("Error occurred:");
        msg.setMessage(result);
        bundle.setResult(msg);
      }
    } catch (final Exception e) {
      e.printStackTrace(System.err);
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("Error occurred");
      bundle.setResult(msg);
    }
  }

  private String voidInvoice(Invoice inv) {
    String invResult = "success";
    try {
      // System.out.println("At Invoice : OrderID->" + inv.getSalesOrder().getId());
      List<Object> param = new ArrayList<Object>();
      param.add(null);
      param.add(inv.getSalesOrder().getId());
      CallStoredProcedure.getInstance().call("ncn_invoice_cancel", param, null, true, false);
      // System.out.println("Cancelled Credit Note.");
      log.debug("Cancelled Credit Note.");
    } catch (Exception e) {
      invResult = "";
      invResult = invResult.concat(e.toString());
    }
    return invResult;
  }

  private String voidRMR(Order o) {
    String rmrResult = "success";
    try {
      // ShipmentInOut io = getReceipt(o);
      // System.out.println("At RMR : OrderID->" + o.getId());
      List<Object> params = new ArrayList<Object>();
      params.add(null);
      params.add(o.getId());
      CallStoredProcedure.getInstance().call("ncn_inout_cancel", params, null, true, false);
      // System.out.println("Cancelled RMR.");
      log.debug("Cancelled RMR.");
    } catch (Exception e) {
      rmrResult = "";
      rmrResult = rmrResult.concat(e.toString());
    }
    return rmrResult;

  }

  private String voidRFC(Invoice inv) {
    String rfcResult = "success";
    Order o;
    try {
      o = inv.getSalesOrder();
      o.setActive(false);
      o.setDocumentStatus("VO");
      for (OrderLine ol : o.getOrderLineList()) {
        ol.setNcnInvoice(null);
        OBDal.getInstance().save(ol);
      }
      OBDal.getInstance().save(o);
      OBDal.getInstance().flush();
      // System.out.println("Voided RFC.");
      log.debug("Voided RFC.");
    } catch (Exception e) {
      rfcResult = rfcResult.concat(e.toString());
    }
    return rfcResult;
  }

  /*
   * private ShipmentInOut getReceipt(Order o) { ShipmentInOut io = null; OBCriteria<ShipmentInOut>
   * obCriteria = OBDal.getInstance().createCriteria(ShipmentInOut.class);
   * obCriteria.add(Expression.eq(ShipmentInOut.PROPERTY_SALESORDER, o)); final List<ShipmentInOut>
   * ioList = obCriteria.list(); if (!ioList.isEmpty()) io = ioList.get(0); else throw new
   * OBException("Goods Receipt is not foung for Order" + o.getDocumentNo()); return io; }
   */
}
