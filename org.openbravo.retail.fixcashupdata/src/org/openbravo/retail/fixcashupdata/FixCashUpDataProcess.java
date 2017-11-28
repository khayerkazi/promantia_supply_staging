package org.openbravo.retail.fixcashupdata;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetailV;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.retail.posterminal.OBPOSAppCashReconcil;
import org.openbravo.retail.posterminal.OBPOSAppCashup;
import org.openbravo.retail.posterminal.OBPOSAppPayment;
import org.openbravo.retail.posterminal.OBPOSApplications;
import org.openbravo.retail.posterminal.OrderGroupingProcessor;
import org.openbravo.retail.posterminal.TerminalTypePaymentMethod;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;

public class FixCashUpDataProcess extends BaseActionHandler {

  private static final Logger log = Logger.getLogger(FixCashUpDataProcess.class);

  private List<String> createdCashUps = new ArrayList<String>();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    JSONObject result = new JSONObject();
    JSONArray actions = new JSONArray();
    JSONObject msgTotal = new JSONObject();
    List<String> errors = new ArrayList<String>();
    List<String> warnings = new ArrayList<String>();
    OBContext.setAdminMode(true);
    TriggerHandler.getInstance().disable();
    try {

      JSONObject data = new JSONObject(content);
      JSONObject params = data.getJSONObject("_params");
      String currentUser = (String) RequestContext.get().getSession().getAttribute("#AD_USER_ID");
      String currentRole = (String) RequestContext.get().getSession().getAttribute("#AD_ROLE_ID");
      OBContext.setOBContext(currentUser, currentRole, OBContext.getOBContext().getCurrentClient()
          .getId(), params.getString("ad_org_id"));
      int maxDays = params.getInt("max_days");

      // Find affected orders
      String orderQueryHql = " as o "
          + " where o.organization.id=:orgId and not exists (select 1 from OBPOS_App_Cashup as cu where o.obposAppCashup = cu) "
          + " and exists (select 1 from OBPOS_App_Cashup as cup where o.obposApplications = cup.pOSTerminal and cup.cashUpDate >= o.orderDate)"
          + " and o.obposAppCashup is not null" + " order by o.obposAppCashup, o.orderDate desc";

      OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class, orderQueryHql);
      orderQuery.setNamedParameter("orgId", params.getString("ad_org_id"));
      ScrollableResults orders = orderQuery.scroll(ScrollMode.FORWARD_ONLY);
      String missingCashUpId = "";
      OBPOSAppCashup cashUp = null;
      int totalOrders = 0;
      long t0, t1, t2 = System.currentTimeMillis();
      while (orders.next()) {
        Order order = (Order) orders.get()[0];
        log.info("Processing order: " + order.getDocumentNo());
        if (!missingCashUpId.equals(order.getObposAppCashup())) {
          log.info("Reconcile payments: " + (System.currentTimeMillis() - t2));
          missingCashUpId = order.getObposAppCashup();
          t0 = System.currentTimeMillis();
          cashUp = findCorrespondingCashUpForCashUpId(order, maxDays);
          t1 = System.currentTimeMillis();
          new OrderGroupingProcessor().groupOrders(order.getObposApplications(), missingCashUpId,
              order.getOrderDate());
          t2 = System.currentTimeMillis();
          log.info("Find cashup: " + (t1 - t0) + ". Grouping: " + (t2 - t1));
        }
        if (cashUp == null) {
          log.info("no reconciliation for order: " + order.getDocumentNo());
          errors.add("Order " + order.getDocumentNo() + " has no valid cash up.");
          continue;
        }
        order.setObposAppCashup(cashUp.getId());
        OBDal.getInstance().flush();
        reconcileOrderPayments(order, cashUp, errors);
        totalOrders++;
      }
      log.info("Total orders: " + totalOrders);
      OBDal.getInstance().flush();
      log.info("Finalising created cashups");
      for (String cashUpId : createdCashUps) {
        cashUp = OBDal.getInstance().get(OBPOSAppCashup.class, cashUpId);
        closeCashUp(cashUp);
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
    } finally {
      TriggerHandler.getInstance().enable();
      OBContext.restorePreviousMode();
    }

    try {
      if (errors.size() == 0) {
        msgTotal.put("msgType", "success");
        // XXX: these two messages should be translatable, like OBEXAPP_MinGtMax above
        msgTotal.put("msgTitle", "Success");
        msgTotal.put("msgText", "Success");
      } else {

        msgTotal.put("msgType", "error");
        // XXX: these two messages should be translatable, like OBEXAPP_MinGtMax above
        msgTotal.put("msgTitle", "There were errors when executing the process:");
        String errorstr = "";
        for (String error : errors) {
          errorstr += error;
        }
        msgTotal.put("msgText", errorstr);
      }

      JSONObject msgTotalAction = new JSONObject();
      msgTotalAction.put("showMsgInProcessView", msgTotal);
      actions.put(msgTotalAction);
      result.put("responseActions", actions);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;

  }

  private void closeCashUp(OBPOSAppCashup cashUp) {
    for (OBPOSAppCashReconcil cashUpRec : cashUp.getOBPOSAppCashReconcilList()) {
      FIN_Reconciliation reconciliation = cashUpRec.getReconciliation();
      OBCriteria<FIN_FinaccTransaction> trxCriteria = OBDal.getInstance().createCriteria(
          FIN_FinaccTransaction.class);
      trxCriteria.add(Restrictions
          .eq(FIN_FinaccTransaction.PROPERTY_RECONCILIATION, reconciliation));
      ScrollableResults result = trxCriteria.scroll();
      BigDecimal totalDeposits = BigDecimal.ZERO;
      while (result.next()) {
        FIN_FinaccTransaction trx = (FIN_FinaccTransaction) result.get()[0];
        totalDeposits = totalDeposits.add(trx.getDepositAmount()).subtract(trx.getPaymentAmount());
      }

      FIN_FinaccTransaction paymentTransaction = createTotalTransferTransactionPayment(
          cashUp.getPOSTerminal(), reconciliation, cashUpRec.getPaymentType(), totalDeposits,
          cashUp.getCashUpDate());
      OBDal.getInstance().save(paymentTransaction);

      FIN_FinaccTransaction depositTransaction = createTotalTransferTransactionDeposit(
          cashUp.getPOSTerminal(), reconciliation, cashUpRec.getPaymentType(), totalDeposits,
          cashUp.getCashUpDate());
      OBDal.getInstance().save(depositTransaction);

    }
  }

  private OBPOSAppCashup findCorrespondingCashUpForCashUpId(Order order, int maxDays) {
    OBCriteria<OBPOSAppCashup> cashUpCriteria = OBDal.getInstance().createCriteria(
        OBPOSAppCashup.class);
    cashUpCriteria.add(Restrictions.eq(OBPOSAppCashup.PROPERTY_POSTERMINAL,
        order.getObposApplications()));

    cashUpCriteria.add(Restrictions.ge(OBPOSAppCashup.PROPERTY_CREATIONDATE,
        order.getCreationDate()));
    Calendar maxDate = Calendar.getInstance();
    maxDate.setTime(order.getCreationDate());
    maxDate.add(Calendar.DATE, maxDays);
    cashUpCriteria.add(Restrictions.le(OBPOSAppCashup.PROPERTY_CREATIONDATE, maxDate.getTime()));
    cashUpCriteria.addOrderBy(OBPOSAppCashup.PROPERTY_CREATIONDATE, true);
    ScrollableResults cashUpScroll = cashUpCriteria.scroll();
    if (!cashUpScroll.next()) {
      return null;
    }
    OBPOSAppCashup cashUp = (OBPOSAppCashup) cashUpScroll.get()[0];
    if (cashUp.getOBPOSAppCashReconcilList().size() > 0) {
      return cashUp;
    }
    // Cashup doesn't have reconciliation information. We will create it.
    log.info("Creating cashup reconciliation information for cashup " + cashUp);
    createdCashUps.add(cashUp.getId());
    for (org.openbravo.retail.posterminal.OBPOSAppPayment paymentMethod : cashUp.getPOSTerminal()
        .getOBPOSAppPaymentList()) {
      FIN_Reconciliation reconciliation = createReconciliation(cashUp.getPOSTerminal(),
          paymentMethod.getFinancialAccount(), cashUp);
      OBDal.getInstance().save(reconciliation);
      OBPOSAppCashReconcil cashUpRec = createCashUpReconciliation(cashUp.getPOSTerminal(),
          paymentMethod, reconciliation, cashUp);

      cashUp.getOBPOSAppCashReconcilList().add(cashUpRec);
      OBDal.getInstance().save(cashUpRec);
    }
    OBDal.getInstance().flush();
    return cashUp;

  }

  protected OBPOSAppCashReconcil createCashUpReconciliation(OBPOSApplications posTerminal,
      OBPOSAppPayment paymentType, FIN_Reconciliation reconciliation, OBPOSAppCashup cashUp) {
    OBPOSAppCashReconcil recon = OBProvider.getInstance().get(OBPOSAppCashReconcil.class);
    recon.setOrganization(posTerminal.getOrganization());
    recon.setPaymentType(paymentType);
    recon.setReconciliation(reconciliation);
    recon.setCashUp(cashUp);
    reconciliation.getOBPOSAppCashReconcilList().add(recon);
    return recon;
  }

  private FIN_Reconciliation createReconciliation(OBPOSApplications posTerminal,
      FIN_FinancialAccount account, OBPOSAppCashup cashUp) {
    Date cashUpDate = cashUp.getCashUpDate();
    FIN_Reconciliation reconciliation = OBProvider.getInstance().get(FIN_Reconciliation.class);
    reconciliation.setAccount(account);
    reconciliation.setOrganization(posTerminal.getOrganization());
    reconciliation.setDocumentType(posTerminal.getObposTerminaltype()
        .getDocumentTypeForReconciliations());
    reconciliation.setDocumentNo(getReconciliationDocumentNo(reconciliation.getDocumentType()));
    reconciliation.setEndingDate(cashUpDate);
    reconciliation.setTransactionDate(cashUpDate);
    reconciliation.setEndingBalance(new BigDecimal(0));
    reconciliation.setStartingbalance(BigDecimal.ZERO);
    reconciliation.setDocumentStatus("CO");
    reconciliation.setProcessNow(false);
    reconciliation.setProcessed(true);
    return reconciliation;
  }

  protected String getReconciliationDocumentNo(DocumentType doctype) {
    return Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
        new DalConnectionProvider(false), RequestContext.get().getVariablesSecureApp(), "",
        "FIN_Reconciliation", "", doctype == null ? "" : doctype.getId(), false, true);
  }

  private void reconcileOrderPayments(Order order, OBPOSAppCashup cashUp, List<String> errors) {
    order = OBDal.getInstance().get(Order.class, order.getId());
    for (FIN_PaymentSchedule ps : order.getFINPaymentScheduleList()) {
      for (FIN_PaymentDetailV pd : ps.getFINPaymentDetailVPaymentPlanOrderList()) {
        log.info("payments found");
        FIN_Payment payment = pd.getPayment();
        FIN_FinancialAccount fa = pd.getFinFinancialAccount();
        OBCriteria<FIN_FinaccTransaction> criTrx = OBDal.getInstance().createCriteria(
            FIN_FinaccTransaction.class);
        criTrx.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
        criTrx.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_ACCOUNT, fa));
        criTrx.add(Restrictions.isNull(FIN_FinaccTransaction.PROPERTY_RECONCILIATION));
        List<FIN_FinaccTransaction> trxList = criTrx.list();
        if (!trxList.isEmpty()) {
          FIN_FinaccTransaction trx = trxList.get(0);
          // Search Reconciliation for transaction and update
          FIN_Reconciliation processedRecon = getReconciliation(trx, cashUp);
          if (processedRecon == null) {
            errors.add("Order " + order.getDocumentNo()
                + " doesn't have a reconciliation for its cash up");
          }
          if (processedRecon != null) {
            trx.setReconciliation(processedRecon);
            trx.setStatus("RPPC");
            payment.setStatus("RPPC");
            OBDal.getInstance().save(trx);
            OBDal.getInstance().save(payment);
          }
        }
      }
    }

  }

  private FIN_Reconciliation getReconciliation(FIN_FinaccTransaction trx, OBPOSAppCashup cashUp) {
    OBCriteria<OBPOSAppCashReconcil> cashUpRec = OBDal.getInstance().createCriteria(
        OBPOSAppCashReconcil.class);
    cashUpRec.add(Restrictions.eq(OBPOSAppCashReconcil.PROPERTY_CASHUP, cashUp));
    FIN_FinancialAccount account = trx.getAccount();
    for (OBPOSAppCashReconcil cashRec : cashUpRec.list()) {
      if (cashRec.getReconciliation().getAccount().equals(account)) {
        return cashRec.getReconciliation();
      }
    }
    return null;
  }

  protected FIN_FinaccTransaction createTotalTransferTransactionPayment(OBPOSApplications terminal,
      FIN_Reconciliation reconciliation, OBPOSAppPayment paymentType,
      BigDecimal reconciliationTotal, Date cashUpDate) {
    TerminalTypePaymentMethod paymentMethod = paymentType.getPaymentMethod();
    FIN_FinancialAccount account = paymentType.getFinancialAccount();
    GLItem glItem = paymentMethod.getGlitemDropdep();
    FIN_FinaccTransaction transaction = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    transaction.setCurrency(account.getCurrency());
    transaction.setAccount(account);
    transaction.setLineNo(TransactionsDao.getTransactionMaxLineNo(account) + 10);
    transaction.setGLItem(glItem);
    transaction.setPaymentAmount(reconciliationTotal);
    transaction.setProcessed(true);
    transaction.setTransactionType("BPW");
    transaction.setStatus("RPPC");
    transaction.setDescription("GL Item: " + glItem.getName());
    transaction.setDateAcct(cashUpDate);
    transaction.setTransactionDate(cashUpDate);
    transaction.setReconciliation(reconciliation);

    account.setCurrentBalance(account.getCurrentBalance().subtract(reconciliationTotal));

    return transaction;

  }

  protected FIN_FinaccTransaction createTotalTransferTransactionDeposit(OBPOSApplications terminal,
      FIN_Reconciliation reconciliation, OBPOSAppPayment paymentType,
      BigDecimal reconciliationTotal, Date cashUpDate) {
    GLItem glItem = paymentType.getPaymentMethod().getGlitemDropdep();
    if (paymentType.getObretcoCmevents() == null) {
      throw new OBException("There is no close event defined for the payment method");
    }
    FIN_FinancialAccount accountFrom = paymentType.getFinancialAccount();
    FIN_FinancialAccount accountTo = paymentType.getObretcoCmevents().getFinancialAccount();

    BigDecimal conversionRate = new BigDecimal(1);
    if (!accountFrom.getCurrency().getId().equals(accountTo.getCurrency().getId())) {
      List<Object> parameters = new ArrayList<Object>();
      parameters.add(accountFrom.getCurrency().getId());
      parameters.add(accountTo.getCurrency().getId());
      parameters.add(null);
      parameters.add(null);
      parameters.add(terminal.getClient().getId());
      parameters.add(terminal.getOrganization().getId());

      String procedureName = "C_CURRENCY_RATE";
      conversionRate = (BigDecimal) CallStoredProcedure.getInstance().call(procedureName,
          parameters, null);
    }

    FIN_FinaccTransaction transaction = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    transaction.setCurrency(accountTo.getCurrency());
    transaction.setAccount(accountTo);
    transaction.setLineNo(TransactionsDao.getTransactionMaxLineNo(accountTo) + 10);
    transaction.setGLItem(glItem);
    transaction.setDepositAmount(reconciliationTotal.multiply(conversionRate).setScale(2,
        BigDecimal.ROUND_HALF_EVEN));
    transaction.setProcessed(true);
    transaction.setTransactionType("BPW");
    transaction.setStatus("RDNC");
    transaction.setDescription("GL Item: " + glItem.getName());
    transaction.setDateAcct(cashUpDate);
    transaction.setTransactionDate(cashUpDate);

    accountTo.setCurrentBalance(accountTo.getCurrentBalance().add(reconciliationTotal));

    return transaction;

  }
}
