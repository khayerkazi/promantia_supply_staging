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
import java.util.Date;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.process.DataSynchronizationProcess.DataSynchronization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.retail.config.CashManagementEvents;
import org.openbravo.service.json.JsonConstants;

@DataSynchronization(entity = "FIN_Finacc_Transaction")
public class ProcessCashMgmt extends POSDataSynchronizationProcess {

  private static final Logger log = Logger.getLogger(ProcessCashMgmt.class);

  public JSONObject saveRecord(JSONObject jsonsent) throws Exception {

    OBPOSAppPayment paymentMethod = OBDal.getInstance().get(OBPOSAppPayment.class,
        jsonsent.getString("paymentMethodId"));
    OBContext.setOBContext(jsonsent.getString("userId"),
        OBContext.getOBContext().getRole().getId(), OBContext.getOBContext().getCurrentClient()
            .getId(), paymentMethod.getObposApplications().getOrganization().getId());
    String description = jsonsent.getString("description");
    BigDecimal amount = BigDecimal.valueOf(jsonsent.getDouble("amount"));
    BigDecimal origAmount = BigDecimal.valueOf(jsonsent.getDouble("origAmount"));
    String type = jsonsent.getString("type");
    String cashManagementReasonId = jsonsent.getString("reasonId");
    TerminalTypePaymentMethod terminalPaymentMethod = paymentMethod.getPaymentMethod();
    GLItem glItemMain;
    GLItem glItemSecondary;
    if (type.equals("drop")) {
      glItemMain = terminalPaymentMethod.getGLItemForDrops();
      glItemSecondary = terminalPaymentMethod.getGLItemForDeposits();
    } else {
      glItemMain = terminalPaymentMethod.getGLItemForDeposits();
      glItemSecondary = terminalPaymentMethod.getGLItemForDrops();
    }
    if (jsonsent.has("glItem")) {
      glItemMain = OBDal.getInstance().get(GLItem.class, jsonsent.getString("glItem"));
    }
    FIN_FinancialAccount account = paymentMethod.getFinancialAccount();

    FIN_FinaccTransaction transaction = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    transaction.setNewOBObject(true);
    transaction.setId(jsonsent.getString("id"));
    transaction.setCurrency(account.getCurrency());
    transaction.setAccount(account);
    transaction.setLineNo(TransactionsDao.getTransactionMaxLineNo(account) + 10);
    transaction.setGLItem(glItemMain);
    if (type.equals("drop")) {
      transaction.setPaymentAmount(amount);
      account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
    } else {
      transaction.setDepositAmount(amount);
      account.setCurrentBalance(account.getCurrentBalance().add(amount));
    }
    transaction.setProcessed(true);
    transaction.setTransactionType("BPW");
    transaction.setDescription(description);
    transaction.setDateAcct(new Date());
    transaction.setTransactionDate(new Date());
    transaction.setStatus("RDNC");

    OBDal.getInstance().save(transaction);

    CashManagementEvents event = OBDal.getInstance().get(CashManagementEvents.class,
        cashManagementReasonId);

    if (event != null) {
      FIN_FinancialAccount secondAccount = event.getFinancialAccount();

      FIN_FinaccTransaction secondTransaction = OBProvider.getInstance().get(
          FIN_FinaccTransaction.class);
      secondTransaction.setCurrency(secondAccount.getCurrency());
      secondTransaction.setAccount(secondAccount);
      secondTransaction.setLineNo(TransactionsDao.getTransactionMaxLineNo(event
          .getFinancialAccount()) + 10);
      secondTransaction.setGLItem(glItemSecondary);
      // The second transaction describes the opposite movement of the first transaction.
      // If the first is a deposit, the second is a drop
      if (type.equals("deposit")) {
        secondTransaction.setPaymentAmount(origAmount);
        secondAccount.setCurrentBalance(secondAccount.getCurrentBalance().subtract(origAmount));
      } else {
        secondTransaction.setDepositAmount(origAmount);
        secondAccount.setCurrentBalance(secondAccount.getCurrentBalance().add(origAmount));
      }
      secondTransaction.setProcessed(true);
      secondTransaction.setTransactionType("BPW");
      secondTransaction.setDescription(description);
      secondTransaction.setDateAcct(new Date());
      secondTransaction.setTransactionDate(new Date());
      secondTransaction.setStatus("RDNC");
      OBDal.getInstance().save(secondTransaction);
    }

    JSONObject result = new JSONObject();
    result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    return result;
  }

  @Override
  protected String getProperty() {
    return "";
  }
}
