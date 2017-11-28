package org.openbravo.localization.india.utils.ad_Process; //ContraEntryProcess

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.localization.india.utils.data.ContraEntry;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class ContraEntryProcess extends DalBaseProcess {
  private static final String ACTION_PAYMENT_DEPOSIT = "D";
  private static final String FIN_PaymentProcess_ID = "6255BE488882480599C81284B70CD9B3";
  private static final String GLITM_NAME = "In Transit account (Contra Entry)";

  VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
  OBError message = new OBError();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    String recordId = (String) bundle.getParams().get("Obinutl_Conentry_ID");
    // System.out.println("I am here !");

    ContraEntry contraObj = OBDal.getInstance().get(ContraEntry.class, recordId);

    Client client = contraObj.getClient();
    Organization org = contraObj.getOrganization();

    DocumentType doctype = contraObj.getDocType();
    String docNo = contraObj.getDocumentNo();

    // Financial Account & Currency
    FIN_FinancialAccount fromAccount = contraObj.getFromAccount();
    FIN_FinancialAccount toAccount = contraObj.getToAccount();
    FIN_PaymentMethod paymentMethod = contraObj.getPaymentMethod();
    Currency currency = contraObj.getCurrency();
    Date paymentDate = contraObj.getTransactionDate();
    String amount = contraObj.getAmount().toString();
    String desc = contraObj.getDescription();

    if (contraObj.getFromAccount().getName().equals(contraObj.getToAccount().getName())) {
      throw new OBException("From Account and To account should not be same !");
    }

    // Getting GLItem
    String strGlitem = getGLItem(GLITM_NAME);

    FIN_Payment paymentOut = null;
    FIN_Payment paymentIn = null;

    try {

      paymentOut = createPaymentInOut(false, org, doctype, docNo, paymentDate, amount.toString(),
          currency, fromAccount, paymentMethod, desc);

      if (paymentOut != null) {
        OBContext.setAdminMode(true);
        FIN_PaymentDetail paymentDetailObj = createPaymentDetail(org, paymentOut, amount, strGlitem);
        BigDecimal bigAmount = new BigDecimal(amount);
        createPaymentScheduleDetail(client, org, paymentOut, bigAmount, paymentOut,
            paymentDetailObj);

        OBDal.getInstance().flush();
        DalConnectionProvider conn = new DalConnectionProvider();
        OBError processPaymentTransaction = processPayment(conn, ACTION_PAYMENT_DEPOSIT, paymentOut);
        if (processPaymentTransaction != null
            && "Error".equals(processPaymentTransaction.getType())) {
          throw new OBException(processPaymentTransaction.getMessage());
        }
        // Save in contra entry table
        contraObj.setPayout(paymentOut);
      }

      paymentIn = createPaymentInOut(true, org, doctype, docNo, paymentDate, amount.toString(),
          currency, toAccount, paymentMethod, desc);

      if (paymentIn != null) {
        OBContext.setAdminMode(true);
        FIN_PaymentDetail paymentDetailObj = createPaymentDetail(org, paymentIn, amount, strGlitem);
        BigDecimal bigAmount = new BigDecimal(amount);
        createPaymentScheduleDetail(client, org, paymentOut, bigAmount, paymentIn, paymentDetailObj);
        OBDal.getInstance().flush();
        DalConnectionProvider conn = new DalConnectionProvider();

        Hibernate.initialize(paymentIn.getFINPaymentDetailList().get(0));
        OBDal.getInstance().flush();
        DalConnectionProvider connc = new DalConnectionProvider();
        OBError processPaymentTransaction = processPayment(connc, ACTION_PAYMENT_DEPOSIT, paymentIn);
        if (processPaymentTransaction != null
            && "Error".equals(processPaymentTransaction.getType())) {
          throw new OBException(processPaymentTransaction.getMessage());
        }
        contraObj.setPayin(paymentIn);
      }

      contraObj.setDocstatus("CO");
      message = setSuccessMessage(contraObj);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      // log.error("Exception in ContraEntry Process: " + e.getMessage());
      message = setErrorMessage(e);
    } finally {
      bundle.setResult(message);
    }
  }

  private OBError processPayment(DalConnectionProvider connc, String strAction,
      FIN_Payment paymentInOut) throws Exception {
    ProcessBundle pb = null;
    OBError myMessage = null;
    try {
      pb = new ProcessBundle(FIN_PaymentProcess_ID, vars).init(connc);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("action", strAction);
      parameters.put("Fin_Payment_ID", paymentInOut.getId());
      pb.setParams(parameters);

      new FIN_PaymentProcess().execute(pb);
      myMessage = (OBError) pb.getResult();

    } catch (ServletException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return myMessage;

  }

  private FIN_PaymentScheduleDetail createPaymentScheduleDetail(Client client, Organization org,
      FIN_Payment paymentOut, BigDecimal amount, FIN_Payment paymentOut2,
      FIN_PaymentDetail paymentDetailObj) {
    OBContext.setAdminMode(true);
    // BigDecimal amount = new BigDecimal(amount);
    System.out.println("Big Decimal value:" + amount);
    FIN_PaymentScheduleDetail paymentScheduleDetailObj = OBProvider.getInstance().get(
        FIN_PaymentScheduleDetail.class);
    paymentScheduleDetailObj.setClient(client);
    paymentScheduleDetailObj.setOrganization(org);
    paymentScheduleDetailObj.setAmount(amount);

    paymentScheduleDetailObj.setInvoicePaid(true);
    paymentScheduleDetailObj.setPaymentDetails(paymentDetailObj);

    paymentScheduleDetailObj.setWriteoffAmount(new BigDecimal("0"));
    paymentScheduleDetailObj.setDoubtfulDebtAmount(new BigDecimal("0"));

    paymentDetailObj.getFINPaymentScheduleDetailList().add(paymentScheduleDetailObj);
    OBDal.getInstance().save(paymentScheduleDetailObj);
    OBContext.restorePreviousMode();
    return paymentScheduleDetailObj;
  }

  private String getGLItem(String glItemName) {
    String glItemQuery = "select e.id from FinancialMgmtGLItem e " + "where e.name='" + glItemName
        + "'";

    Query query = OBDal.getInstance().getSession().createQuery(glItemQuery);
    List<String> queryList = query.list();
    String glItem = queryList.get(0);
    return glItem;
  }

  private OBError setErrorMessage(Exception e) {
    OBError message = new OBError();
    Throwable ex = DbUtility.getUnderlyingSQLException(e);
    String errormsg = OBMessageUtils.translateError(ex.getMessage()).getMessage();
    message.setTitle("Error");
    message.setType("Error");
    message.setMessage(errormsg);
    return message;
  }

  private OBError setSuccessMessage(ContraEntry order) {
    OBError message = new OBError();
    message.setTitle("Success");
    message.setType("Success");
    message.setMessage("Process Completed Successfully.");
    return message;
  }

  private FIN_PaymentDetail createPaymentDetail(Organization org, FIN_Payment paymentOut,
      String amount, String glItem) {
    final FIN_PaymentDetail paymentDetailObj = OBProvider.getInstance()
        .get(FIN_PaymentDetail.class);

    try {
      GLItem glItemObj = OBDal.getInstance().get(GLItem.class, glItem);
      OBContext.setAdminMode(true);
      paymentDetailObj.setOrganization(org);
      paymentDetailObj.setActive(true);
      paymentDetailObj.setAmount(new BigDecimal(amount));
      paymentDetailObj.setFinPayment(paymentOut);
      paymentDetailObj.setRefund(false);
      paymentDetailObj.setWriteoffAmount(new BigDecimal("0"));
      paymentDetailObj.setPrepayment(false);
      paymentDetailObj.setGLItem(glItemObj);
      paymentOut.getFINPaymentDetailList().add(paymentDetailObj);

      OBDal.getInstance().save(paymentDetailObj);
    } finally {
      OBContext.restorePreviousMode();
    }
    return paymentDetailObj;
  }

  private FIN_Payment createPaymentInOut(boolean isreciept, Organization org, DocumentType doctype,
      String documentNo, Date paymentDate, String amount, Currency currency,
      FIN_FinancialAccount finAcct, FIN_PaymentMethod paymentMethod, String desc) {

    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    try {

      FIN_Payment paymentObj = OBProvider.getInstance().get(FIN_Payment.class);

      OBContext.setAdminMode(true);
      paymentObj.setOrganization(org);
      paymentObj.setClient(OBContext.getOBContext().getCurrentClient());
      paymentObj.setUpdated(new Date());
      paymentObj.setUpdatedBy(OBContext.getOBContext().getUser());
      paymentObj.setCreationDate(new Date());
      paymentObj.setCreatedBy(OBContext.getOBContext().getUser());
      paymentObj.setActive(true);
      paymentObj.setReceipt(isreciept);
      paymentObj.setCurrency(currency);
      paymentObj.setAmount(new BigDecimal(amount));
      paymentObj.setWriteoffAmount(new BigDecimal(0));
      paymentObj.setDocumentNo(documentNo);
      if (!isreciept) {
        paymentObj.setStatus("PWNC");
        DocumentType paymentOutDocType = getDocumentType("APP");
        paymentObj.setDocumentType(paymentOutDocType);
        paymentObj.setDocumentNo(getDocumentNumber(paymentOutDocType.getId()));

      } else {
        paymentObj.setStatus("RDNC");
        DocumentType paymentInDocType = getDocumentType("ARR");
        paymentObj.setDocumentType(paymentInDocType);
        paymentObj.setDocumentNo(getDocumentNumber(paymentInDocType.getId()));

      }
      paymentObj.setDescription(desc);
      paymentObj.setAccount(finAcct);
      paymentObj.setCreatedByAlgorithm(false);
      paymentObj.setFinancialTransactionConvertRate(new BigDecimal(1));
      paymentObj.setFinancialTransactionAmount(new BigDecimal(amount));
      paymentObj.setPaymentMethod(paymentMethod);
      paymentObj.setPaymentDate(paymentDate);

      OBDal.getInstance().save(paymentObj);

      return paymentObj;

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public static DocumentType getDocumentType(String documentCategory) {
    OBCriteria<DocumentType> docTypeCriteria = OBDal.getInstance().createCriteria(
        DocumentType.class);
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, documentCategory));
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_DEFAULT, true));
    List<DocumentType> docTypeCriteriaList = docTypeCriteria.list();
    if (docTypeCriteriaList != null && docTypeCriteriaList.size() > 0) {
      return docTypeCriteriaList.get(0);
    } else {
      throw new OBException("document type not found");
    }
  }

  private String getDocumentNumber(String doctypeid) {
    String docTypeSequenceNum = "";
    try {
      DocumentType posDocType = OBDal.getInstance().get(DocumentType.class, doctypeid);
      docTypeSequenceNum = posDocType.getDocumentSequence().getNextAssignedNumber().toString();
      posDocType.getDocumentSequence().setNextAssignedNumber(
          (Long.valueOf(docTypeSequenceNum).longValue() + 1));
      OBDal.getInstance().save(posDocType);
      OBDal.getInstance().flush();
      // OBDal.getInstance().commitAndClose();
      SessionHandler.getInstance().commitAndStart();

    } catch (NumberFormatException e) {

      e.printStackTrace();
    }
    return docTypeSequenceNum;

  }
}
