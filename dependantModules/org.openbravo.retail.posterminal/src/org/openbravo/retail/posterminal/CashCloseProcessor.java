/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.posterminal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;

public class CashCloseProcessor {

  @Inject
  @Any
  private Instance<CashupHook> cashupHooks;

  public JSONObject processCashClose(OBPOSApplications posTerminal, JSONObject jsonCashup,
      JSONArray cashMgmtIds, Date cashUpDate) throws Exception {
    String cashUpId = jsonCashup.getString("id");
    JSONArray cashCloseInfo = jsonCashup.getJSONArray("cashCloseInfo");
    OBPOSAppCashup cashUp = createCashUp(posTerminal, cashUpId, cashUpDate);
    OBDal.getInstance().save(cashUp);

    for (int i = 0; i < cashCloseInfo.length(); i++) {
      JSONObject cashCloseObj = cashCloseInfo.getJSONObject(i);

      BigDecimal difference = new BigDecimal(cashCloseObj.getString("difference"));
      BigDecimal differenceToApply = difference;
      BigDecimal foreignDifference = new BigDecimal(0);

      if (cashCloseObj.has("foreignDifference")) {
        foreignDifference = new BigDecimal(cashCloseObj.getString("foreignDifference"));
        differenceToApply = foreignDifference;
      }
      String paymentTypeId = cashCloseObj.getString("paymentTypeId");
      OBPOSAppPayment paymentType = OBDal.getInstance().get(OBPOSAppPayment.class, paymentTypeId);

      FIN_Reconciliation reconciliation = createReconciliation(cashCloseObj, posTerminal,
          paymentType.getFinancialAccount(), cashUpDate);

      FIN_FinaccTransaction diffTransaction = null;
      if (!differenceToApply.equals(BigDecimal.ZERO)) {
        diffTransaction = createDifferenceTransaction(posTerminal, reconciliation, paymentType,
            differenceToApply, cashUpDate);
        OBDal.getInstance().save(diffTransaction);
      }
      OBDal.getInstance().save(reconciliation);

      OBPOSAppCashReconcil recon = createCashUpReconciliation(posTerminal, paymentType,
          reconciliation, cashUp);
      OBDal.getInstance().save(recon);

      BigDecimal reconciliationTotal = BigDecimal
          .valueOf(cashCloseObj.getDouble("foreignExpected")).add(foreignDifference);
      if (reconciliationTotal.compareTo(new BigDecimal(0)) != 0) {

        if (!cashCloseObj.getJSONObject("paymentMethod").isNull("amountToKeep")
            && BigDecimal.valueOf(
                cashCloseObj.getJSONObject("paymentMethod").getDouble("amountToKeep")).compareTo(
                new BigDecimal(0)) != 0) {

          BigDecimal amountToKeep = BigDecimal.valueOf(cashCloseObj.getJSONObject("paymentMethod")
              .getDouble("amountToKeep"));
          reconciliationTotal = reconciliationTotal.subtract(amountToKeep);
        }
        if (reconciliationTotal.compareTo(BigDecimal.ZERO) != 0) {
          FIN_FinaccTransaction paymentTransaction = createTotalTransferTransactionPayment(
              posTerminal, reconciliation, paymentType, reconciliationTotal, cashUpDate);
          OBDal.getInstance().save(paymentTransaction);

          FIN_FinaccTransaction depositTransaction = createTotalTransferTransactionDeposit(
              posTerminal, reconciliation, paymentType, reconciliationTotal, cashUpDate);
          OBDal.getInstance().save(depositTransaction);
        }
      }

      associateTransactions(paymentType, reconciliation, cashUpId, cashMgmtIds);

    }

    // Hook for procesing cashups..
    JSONArray messages = new JSONArray(); // all messages returned by hooks
    String next = null; // the first next action of all hooks wins
    for (CashupHook hook : cashupHooks) {
      CashupHookResult result = hook.exec(posTerminal, cashUp, jsonCashup);
      if (result != null) {
        if (result.getMessage() != null && !result.getMessage().equals("")) {
          messages.put(result.getMessage());
        }
        if (next == null && result.getNextAction() != null && !result.getNextAction().equals("")) {
          next = result.getNextAction();
        }
      }
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();

    JSONObject result = new JSONObject();
    result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    result.put("messages", messages);
    result.put("next", next);
    return result;
  }

  protected void associateTransactions(OBPOSAppPayment paymentType,
      FIN_Reconciliation reconciliation, String cashUpId, JSONArray cashMgmtIds) {
    String orderTransactionsQueryHQL = " as fintrans, FIN_Payment_ScheduleDetail as schedDetail"
        + " where schedDetail.paymentDetails.finPayment = fintrans.finPayment"
        + " and fintrans.account=:account and fintrans.reconciliation is null"
        + " and schedDetail.orderPaymentSchedule.order.obposAppCashup=:cashUpId";

    OBQuery<FIN_FinaccTransaction> orderTransactionsQuery = OBDal.getInstance().createQuery(
        FIN_FinaccTransaction.class, orderTransactionsQueryHQL);
    orderTransactionsQuery.setNamedParameter("account", paymentType.getFinancialAccount());
    orderTransactionsQuery.setNamedParameter("cashUpId", cashUpId);
    associateTransactionsFromQuery(orderTransactionsQuery, reconciliation);

    List<String> cashMgmtIdsList = new ArrayList<String>();
    for (int i = 0; i < cashMgmtIds.length(); i++) {
      try {
        cashMgmtIdsList.add(cashMgmtIds.getString(i));
      } catch (JSONException e) {
        // Won't happen
      }
    }
    if (cashMgmtIdsList.size() > 0) {
      OBQuery<FIN_FinaccTransaction> cashMgmtTransactionsQuery = OBDal.getInstance().createQuery(
          FIN_FinaccTransaction.class, "where id in (:transIds) and account.id=:account");
      cashMgmtTransactionsQuery.setNamedParameter("transIds", cashMgmtIdsList);
      cashMgmtTransactionsQuery.setNamedParameter("account", paymentType.getFinancialAccount()
          .getId());
      associateTransactionsFromQuery(cashMgmtTransactionsQuery, reconciliation);
    }
  }

  protected void associateTransactionsFromQuery(OBQuery<FIN_FinaccTransaction> transactionQuery,
      FIN_Reconciliation reconciliation) {
    ScrollableResults transactions = transactionQuery.scroll(ScrollMode.FORWARD_ONLY);
    try {
      while (transactions.next()) {
        FIN_FinaccTransaction transaction = (FIN_FinaccTransaction) transactions.get(0);
        transaction.setStatus("RPPC");
        transaction.setReconciliation(reconciliation);

        // not all transactions have payment (i.e. deposits don't have), if there is payment, set it
        // as cleared
        if (transaction.getFinPayment() != null) {
          transaction.getFinPayment().setStatus("RPPC");
        }
      }
    } finally {
      transactions.close();
    }
  }

  protected FIN_Reconciliation createReconciliation(JSONObject cashCloseObj,
      OBPOSApplications posTerminal, FIN_FinancialAccount account, Date cashUpDate)
      throws JSONException {

    BigDecimal startingBalance;
    OBCriteria<FIN_Reconciliation> reconciliationsForAccount = OBDal.getInstance().createCriteria(
        FIN_Reconciliation.class);
    reconciliationsForAccount.add(Restrictions.eq("account", account));
    reconciliationsForAccount.addOrderBy("creationDate", false);
    List<FIN_Reconciliation> reconciliations = reconciliationsForAccount.list();
    if (reconciliations.size() == 0) {
      startingBalance = account.getInitialBalance();
    } else {
      startingBalance = reconciliations.get(0).getEndingBalance();
    }

    FIN_Reconciliation reconciliation = OBProvider.getInstance().get(FIN_Reconciliation.class);
    reconciliation.setAccount(account);
    reconciliation.setOrganization(posTerminal.getOrganization());
    reconciliation.setDocumentType(posTerminal.getObposTerminaltype()
        .getDocumentTypeForReconciliations());
    reconciliation.setDocumentNo(getReconciliationDocumentNo(reconciliation.getDocumentType()));
    reconciliation.setEndingDate(cashUpDate);
    reconciliation.setTransactionDate(cashUpDate);
    if (!cashCloseObj.getJSONObject("paymentMethod").isNull("amountToKeep")) {
      reconciliation.setEndingBalance(BigDecimal.valueOf(cashCloseObj
          .getJSONObject("paymentMethod").getDouble("amountToKeep")));
    } else {
      reconciliation.setEndingBalance(new BigDecimal(0));
    }
    reconciliation.setStartingbalance(startingBalance);
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

  protected FIN_FinaccTransaction createDifferenceTransaction(OBPOSApplications terminal,
      FIN_Reconciliation reconciliation, OBPOSAppPayment payment, BigDecimal difference,
      Date cashUpDate) {
    FIN_FinancialAccount account = payment.getFinancialAccount();
    GLItem glItem = payment.getPaymentMethod().getCashDifferences();
    FIN_FinaccTransaction transaction = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    transaction.setCurrency(account.getCurrency());
    transaction.setAccount(account);
    transaction.setLineNo(TransactionsDao.getTransactionMaxLineNo(account) + 10);
    transaction.setGLItem(glItem);
    if (difference.compareTo(BigDecimal.ZERO) < 0) {
      transaction.setPaymentAmount(difference.abs());
      account.setCurrentBalance(account.getCurrentBalance().subtract(difference.abs()));
    } else {
      transaction.setDepositAmount(difference);
      account.setCurrentBalance(account.getCurrentBalance().add(difference));
    }
    transaction.setProcessed(true);
    transaction.setTransactionType("BPW");
    transaction.setStatus("RPPC");
    transaction.setDescription("GL Item: " + glItem.getName());
    transaction.setDateAcct(cashUpDate);
    transaction.setTransactionDate(cashUpDate);
    transaction.setReconciliation(reconciliation);

    return transaction;
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

  protected OBPOSAppCashup createCashUp(OBPOSApplications posTerminal, String cashUpId,
      Date cashUpDate) {
    OBPOSAppCashup cashup = OBProvider.getInstance().get(OBPOSAppCashup.class);
    cashup.setNewOBObject(true);
    cashup.setId(cashUpId);
    cashup.setOrganization(posTerminal.getOrganization());
    cashup.setCashUpDate(cashUpDate);
    cashup.setPOSTerminal(posTerminal);
    cashup.setUserContact(OBContext.getOBContext().getUser());
    return cashup;
  }

  protected OBPOSAppCashReconcil createCashUpReconciliation(OBPOSApplications posTerminal,
      OBPOSAppPayment paymentType, FIN_Reconciliation reconciliation, OBPOSAppCashup cashUp) {
    OBPOSAppCashReconcil recon = OBProvider.getInstance().get(OBPOSAppCashReconcil.class);
    recon.setOrganization(posTerminal.getOrganization());
    recon.setPaymentType(paymentType);
    recon.setReconciliation(reconciliation);
    recon.setCashUp(cashUp);
    return recon;
  }
}
