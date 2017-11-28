package in.nous.creditnote.ad_process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.APRMPendingPaymentFromInvoice;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_ExecutePayment;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Region;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.ReturnReason;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.CallProcess;

import com.sysfore.sankalpcrm.RCCreditNote;
import com.sysfore.sankalpcrm.RC_CreditNoteline;

public class CreateCreditNote extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(CreateCreditNote.class);
  private static final String MINUS = "-";
  private static final String EMPTY = "";
  private static final String RMR_DOCTYPE = "Credit Note Receipt";
  private static final String RFC_DOCTYPE = "Credit Note Order";
  private static final String IMMEDIATE = "I";
  private static final String PAYMENT_METHOD = "Cash";
  private static final String CURRENCY = "INR";
  private static final String PAYMENT_TERMS = "Immediate";
  private static final String PRICELIST = "DMI CATALOGUE";
  private static final String FORM_OF_PAYMENTS = "B";
  private static final String MEDIUM = "5";
  private static final String ORDER_POST_PROCEDURE = "104";
  private static final String MOVEMENT_POST_PROCEDURE = "122";
  private static final String INVOICE_POST_PROCEDURE = "111";
  private static final String COMPLETE = "CO";
  private static final String DRAFT = "DR";
  private static final String M_MINUS = "M-";
  private static final String DASH = "--";
  private static final String CUSTOMER_EXCHANGE = "CE";
  private static final String CUSTOMER_DEFECTIVE = "CD";
  private static final String CUSTOMER_GESTURE = "CG";
  private static final String SALEABLE = "Saleable";
  private static final String DEFECTIVE = "Defective";
  private static final String WRITEDOWN = "Writedown";
  private static final String WRITE = "WT";
  private static final String CUSTOMER = "Customer";
  private static final String WHITEFIELD = "Whitefield";
  private static final String BGT = "Bannerghatta";
  private static final String CRN_DOCSEQ = "Credit Note";
  private static final String PREFIX = "CRN-";
  private static final String C_ORDER_ID = "C_Order_ID";
  private static final String M_INOUT_ID = "M_Inout_ID";
  private static final String AD_USER_ID = "AD_User_ID";
  private static final String NCN_CUSTOMER_EXCHANGE = "NCN_Customer Exchange";
  private static final String NCN_CUSTOMER_DEFECTIVE = "NCN_Customer Defective";
  private static final String NCN_CUSTOMER_GESTURE = "NCN_Customer Gesture";
  private static final String TEMP = "Temp";
  private static final String DEFAULT = "Default";

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  // method for performing credit note process
  public JSONObject processCreditNote(RCCreditNote rc) {
    JSONObject jsonDataObject = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObject = new JSONObject();
    log4j.info("Inside processCreditNote() of CreditNoteProcess: " + new Date());
    OBContext.setAdminMode();
    try{
      try {
      // create RFC header
      log.info("Creating RFC...");
      Order rfc = createRFCHeader(rc);
      log.info("Created RFC" + " with ID->" + rfc.getId());

      // create RMR header
      log.info("Creating RMR ...");
      ShipmentInOut rmr = createRMRHeader(rfc, rc);
      log.info("Created RMR" + " with ID->" + rmr.getId());

      // create GM header
      InternalMovement gm = createGMHeader(rmr, rc);

      // create RFC lines and RMR lines and GM lines
      log.info("Creating RFC, RMR Lines...");
      int line = 10;
      for (RC_CreditNoteline rcLine : rc.getRCCreditNoteLineList()) {
        OrderLine rfcLine = createRFCLine(rfc, rcLine, line);
        ShipmentInOutLine rmrLine = createRMRLine(rmr, rfcLine, rcLine, line);
        createGMLine(gm, rmrLine, rc, line);
        line += 10;
      }
      // Booking RFC
      log.info("Posting RFC...");
      log4j.info("Inside OrderPost() of CreditNoteProcess: " + new Date());
      ProcessInstance pInstanceIdForRFC = callProcessToPost(rfc.getCreatedBy().getId(),
          rfc.getId(), ORDER_POST_PROCEDURE, true);// C_Order_Post
      log.info("pInstanceIdForRFC: " + pInstanceIdForRFC);
      log.info("Posted RFC Successfully.");

      // Booking RMR
      log4j.info("Inside InoutPost() of CreditNoteProcess: " + new Date());
      log.info("Posting RMR...");
      rmr.setProcessed(true);
      rmr.setDocumentStatus(COMPLETE);
      rmr.setDocumentAction(DASH);
      OBDal.getInstance().save(rmr);
      ProcessInstance pInstanceIdForGM = callProcessToPost(gm.getCreatedBy().getId(), gm.getId(),
          MOVEMENT_POST_PROCEDURE, false);
      log.info("Posted RMR Successfully.");

      // Creating Credit Note Header
      log.info("Creating Credit Note Header...");
      Invoice inv = createInvoiceHeader(rfc);
      System.out.println("Invoice No->"+inv.getDocumentNo()+" created");	
      log.info("Created Credit Note Header->" + " with ID->" + inv.getId());

      // Creating Credit Note Lines
      log.info("Creating Credit Note Header...");
      int l = 10;
      for (RC_CreditNoteline rcLine : rc.getRCCreditNoteLineList()) {
        createInvoiceLines(inv, rcLine, l);
        l += 10;
      }
      log.info("Created Credit Note Lines.");

      // Booking Invoice
      log4j.info("Inside InvoicePost() of CreditNoteProcess: " + new Date());
      ProcessInstance pInstanceIdForInvoice = callProcessToPost(inv.getCreatedBy().getId(),
          inv.getId(), INVOICE_POST_PROCEDURE, true);

      if (pInstanceIdForInvoice.getResult() == 0L) {
        // Error
        jsonObject.put("status", "failure");
        log.error("Exception while creating credit note-> " + pInstanceIdForInvoice.getErrorMsg());
      } else {
        jsonObject.put("status", "success");
        jsonObject.put("InvoiceDocNos", inv.getDocumentNo());
        log.info("Created Credit Note Successfully.");
	rc.setNcnIsmigrate(true);
        OBDal.getInstance().save(rc);
        OBDal.getInstance().flush();
      }
      // Check if there is any payment pending execution for this invoice.
      try {
        log.info("Executing Payments for Credit Note");
        executePayments();
      } catch (Exception e) {
        jsonObject.put("status", "failure");
        log.error("Exception while executing payments for Credit Note-> " + e);
      }

      jsonArray.put(jsonObject);
      jsonDataObject.put("data", jsonArray);
      log.info("Webservice post completed.");
    } catch (Exception exp) {
      log.error("Exception thrown in webservices->", exp);
    }
   }finally {
      OBContext.restorePreviousMode();
    }
    return jsonDataObject;
  }

  // Method to create Invoice Lines
  private InvoiceLine createInvoiceLines(Invoice inv, RC_CreditNoteline rcLine, int line) {
    log4j.info("Inside InvoiceLine() of CreditNoteProcess: " + new Date());
    Product p = null;
    OBCriteria<Product> pCriteria = OBDal.getInstance().createCriteria(Product.class);
    pCriteria.add(Expression.eq(Product.PROPERTY_NAME, rcLine.getCommercialName()));
    final List<Product> pList = pCriteria.list();
    if (!pList.isEmpty())
      p = pList.get(0);
    else
      throw new OBException("Product does not found in ERP for " + rcLine.getCommercialName());
    OrderLine olLine = null;
    OBCriteria<OrderLine> obCriteria = OBDal.getInstance().createCriteria(OrderLine.class);
    obCriteria.add(Expression.eq(OrderLine.PROPERTY_SALESORDER, inv.getSalesOrder()));
    obCriteria.add(Expression.eq(OrderLine.PROPERTY_PRODUCT, p));
    final List<OrderLine> olList = obCriteria.list();
    if (!olList.isEmpty())
      olLine = olList.get(0);

    InvoiceLine invLine = OBProvider.getInstance().get(InvoiceLine.class);
    invLine.setActive(true);
    invLine.setClient(inv.getClient());
    invLine.setOrganization(inv.getOrganization());
    invLine.setCreationDate(new Date());
    invLine.setUpdated(new Date());
    invLine.setCreatedBy(inv.getCreatedBy());
    invLine.setUpdatedBy(inv.getUpdatedBy());
    invLine.setInvoice(inv);
    invLine.setBusinessPartner(inv.getBusinessPartner());
    BigDecimal exchangeQty = ((rcLine.getRefundexchangeqty() == null || rcLine
        .getRefundexchangeqty().toString().isEmpty()) ? new BigDecimal(0) : new BigDecimal(
        MINUS.concat(rcLine.getRefundexchangeqty().toString())));

    BigDecimal unitPrice = ((rcLine.getPricelevel3() == null || rcLine.getPricelevel3().toString()
        .isEmpty()) ? new BigDecimal(0) : new BigDecimal(rcLine.getPricelevel3()));
    BigDecimal exchangeAmt = ((rcLine.getEchangeredundprice() == null || rcLine
        .getEchangeredundprice().toString().isEmpty()) ? new BigDecimal(0) : new BigDecimal(
        rcLine.getEchangeredundprice()));
    invLine.setUnitPrice(unitPrice);
    invLine.setInvoicedQuantity(exchangeQty);
    invLine.setProduct(p);
    invLine.setAttributeSetValue(p.getAttributeSetValue());
    invLine.setLineNo((long) line);
    invLine.setUOM(p.getUOM());
    invLine.setTax(getTax(inv.getOrganization(), p));
    invLine.setSalesOrderLine(olLine);
    invLine.setGoodsShipmentLine(olLine.getGoodsShipmentLine());
    OBDal.getInstance().save(invLine);
   // OBDal.getInstance().flush();
    return invLine;
  }

  // Method to create Invoice Header
  private Invoice createInvoiceHeader(Order o) {
    log4j.info("Inside InvoiceHeader() of CreditNoteProcess: " + new Date());
    Invoice inv = OBProvider.getInstance().get(Invoice.class);
    inv.setActive(true);
    inv.setClient(o.getClient());
    inv.setOrganization(o.getOrganization());
    inv.setCreationDate(new Date());
    inv.setUpdated(new Date());
    inv.setCreatedBy(o.getCreatedBy());
    inv.setUpdatedBy(o.getUpdatedBy());
    inv.setSalesOrder(o);
    inv.setSalesTransaction(true);
    inv.setDocumentStatus(DRAFT);
    inv.setDocumentAction(COMPLETE);
    inv.setDocumentNo(o.getDocumentNo());
    inv.setDocumentType(o.getDocumentType().getDocumentTypeForInvoice());
    inv.setTransactionDocument(o.getDocumentType().getDocumentTypeForInvoice());
    inv.setProcessed(false);
    inv.setInvoiceDate(o.getOrderDate());
    inv.setBusinessPartner(o.getBusinessPartner());
    inv.setPartnerAddress(o.getPartnerAddress());
    inv.setPaymentTerms(o.getPaymentTerms());
    inv.setPaymentMethod(o.getPaymentMethod());
    inv.setPriceList(o.getPriceList());
    inv.setAccountingDate(o.getAccountingDate());
    inv.setOrderDate(o.getOrderDate());
    inv.setCurrency(o.getCurrency());
    OBDal.getInstance().save(inv);
   // OBDal.getInstance().flush();
    return inv;
  }

  // Method to create GM Lines
  private InternalMovementLine createGMLine(InternalMovement gm, ShipmentInOutLine iol,
      RCCreditNote rc, int line) {
    log4j.info("Inside GMLine() of CreditNoteProcess: " + new Date());
    InternalMovementLine gml = OBProvider.getInstance().get(InternalMovementLine.class);
    gml.setActive(true);
    gml.setClient(gm.getClient());
    gml.setOrganization(gm.getOrganization());
    gml.setCreationDate(new Date());
    gml.setUpdated(new Date());
    gml.setCreatedBy(gm.getCreatedBy());
    gml.setUpdatedBy(gm.getUpdatedBy());
    gml.setMovement(gm);
    gml.setLineNo((long) line);
    gml.setProduct(iol.getProduct());
    gml.setStorageBin(iol.getStorageBin());
    String buttonClicked = "CG";
    if (rc.getAcctype().equals("CD") || rc.getAcctype().equals("D"))
      buttonClicked = "CD";
    else if (rc.getAcctype().equals("CE") || rc.getAcctype().equals("ECR"))
      buttonClicked = "CE";
    else if ((!rc.getAcctype().equals("WD")) || (!rc.getAcctype().equals("CWD"))
        || (!rc.getAcctype().equals("WT")) || (!rc.getAcctype().equals("CWT")))
      buttonClicked = "CG";
    else if (rc.getAcctype().equals(EMPTY) || rc.getAcctype().isEmpty() || rc.getAcctype() == null)
      buttonClicked = "CG";

    if (buttonClicked.equals(CUSTOMER_EXCHANGE))
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), SALEABLE));
    else if (buttonClicked.equals(CUSTOMER_DEFECTIVE))
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), DEFECTIVE));
    else if (buttonClicked.equals(CUSTOMER_GESTURE))
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), WRITEDOWN));
    gml.setUOM(iol.getUOM());
    gml.setAttributeSetValue(iol.getAttributeSetValue());
    gml.setMovementQuantity(iol.getMovementQuantity().abs());
    OBDal.getInstance().save(gml);
   // OBDal.getInstance().flush();
    return gml;
  }

  // Method to create GM Header
  private InternalMovement createGMHeader(ShipmentInOut io, RCCreditNote rc) {
    log4j.info("Inside GMHeader() of CreditNoteProcess: " + new Date());
    InternalMovement gm = OBProvider.getInstance().get(InternalMovement.class);
    gm.setActive(true);
    gm.setClient(io.getClient());
    gm.setOrganization(io.getOrganization());
    gm.setCreationDate(new Date());
    gm.setUpdated(new Date());
    gm.setCreatedBy(io.getCreatedBy());
    gm.setUpdatedBy(io.getUpdatedBy());
    gm.setDocumentNo(io.getDocumentNo());
    gm.setMovementDate(io.getMovementDate());
    String buttonClicked = "CG";
    if (rc.getAcctype().equals("CD") || rc.getAcctype().equals("D"))
      buttonClicked = "CD";
    else if (rc.getAcctype().equals("CE") || rc.getAcctype().equals("ECR"))
      buttonClicked = "CE";
    else if ((!rc.getAcctype().equals("WD")) || (!rc.getAcctype().equals("CWD"))
        || (!rc.getAcctype().equals("WT")) || (!rc.getAcctype().equals("CWT")))
      buttonClicked = "CG";
    else if (rc.getAcctype().equals(EMPTY) || rc.getAcctype().isEmpty() || rc.getAcctype() == null)
      buttonClicked = "CG";

    if (buttonClicked.equals(CUSTOMER_EXCHANGE))
      gm.setSWMovementtypegm(CUSTOMER_EXCHANGE);
    else if (buttonClicked.equals(CUSTOMER_DEFECTIVE))
      gm.setSWMovementtypegm(CUSTOMER_DEFECTIVE);
    else if (buttonClicked.equals(CUSTOMER_GESTURE))
      gm.setSWMovementtypegm(WRITE);
    gm.setName(io.getMovementDate().toString());
    OBDal.getInstance().save(gm);
    //OBDal.getInstance().flush();
    return gm;
  }

  // Method to create RMR Lines
  private ShipmentInOutLine createRMRLine(ShipmentInOut io, OrderLine ol, RC_CreditNoteline rcLine,
      int line) {
    log4j.info("Inside RMRLine() of CreditNoteProcess: " + new Date());
    ShipmentInOutLine iol = OBProvider.getInstance().get(ShipmentInOutLine.class);
    iol.setActive(true);
    iol.setClient(io.getClient());
    iol.setOrganization(io.getOrganization());
    iol.setCreationDate(new Date());
    iol.setUpdated(new Date());
    iol.setCreatedBy(io.getCreatedBy());
    iol.setUpdatedBy(io.getUpdatedBy());
    iol.setSalesOrderLine(ol);
    iol.setShipmentReceipt(io);
    iol.setLineNo((long) line);
    iol.setProduct(ol.getProduct());
    iol.setAttributeSetValue(ol.getAttributeSetValue());
    iol.setUOM(ol.getUOM());
    iol.setStorageBin(getWarehouse(io.getOrganization()).getLocatorList().get(0));
    BigDecimal exchangeQty = ((rcLine.getRefundexchangeqty() == null || rcLine
        .getRefundexchangeqty().toString().isEmpty()) ? new BigDecimal(0) : new BigDecimal(
        MINUS.concat(rcLine.getRefundexchangeqty().toString())));
    iol.setMovementQuantity(exchangeQty);
    iol.setReinvoice(true);
    OBDal.getInstance().save(iol);
    ol.setDeliveredQuantity(iol.getMovementQuantity());
    ol.setGoodsShipmentLine(iol);
    OBDal.getInstance().save(ol);
    //OBDal.getInstance().flush();
    return iol;
  }

  // Method to create RMR Header
  private ShipmentInOut createRMRHeader(Order o, RCCreditNote rc) {
    log4j.info("Inside RMRHeader() of CreditNoteProcess: " + new Date());
    ShipmentInOut io = OBProvider.getInstance().get(ShipmentInOut.class);
    io.setActive(true);
    io.setClient(o.getClient());
    io.setOrganization(o.getOrganization());
    io.setCreationDate(new Date());
    io.setUpdated(new Date());
    io.setCreatedBy(o.getCreatedBy());
    io.setUpdatedBy(o.getUpdatedBy());
    io.setSalesOrder(o);
    io.setAccountingDate(new Date());
    io.setMovementDate(new Date());
    io.setMovementType(M_MINUS);// Movement From
    DocumentType docType = getDocumentType(RMR_DOCTYPE);
    io.setDocumentType(docType);
    io.setDocumentNo(o.getDocumentNo());
    io.setDocumentStatus(DRAFT);
    io.setDocumentAction(COMPLETE);
    io.setBusinessPartner(o.getBusinessPartner());
    io.setPartnerAddress(o.getPartnerAddress());
    io.setWarehouse(o.getWarehouse());
    io.setSalesTransaction(true);
    io.setReceiveMaterials(true);
    io.setSendMaterials(true);
    String buttonClicked = "CG";
    if (rc.getAcctype().equals("CD") || rc.getAcctype().equals("D"))
      buttonClicked = "CD";
    else if (rc.getAcctype().equals("CE") || rc.getAcctype().equals("ECR"))
      buttonClicked = "CE";
    else if ((!rc.getAcctype().equals("WD")) || (!rc.getAcctype().equals("CWD"))
        || (!rc.getAcctype().equals("WT")) || (!rc.getAcctype().equals("CWT")))
      buttonClicked = "CG";
    else if (rc.getAcctype().equals(EMPTY) || rc.getAcctype().isEmpty() || rc.getAcctype() == null)
      buttonClicked = "CG";

    if (buttonClicked.equals(CUSTOMER_EXCHANGE))
      io.setSWMovement(NCN_CUSTOMER_EXCHANGE);// Customer Exchange
    else if (buttonClicked.equals(CUSTOMER_DEFECTIVE))
      io.setSWMovement(NCN_CUSTOMER_DEFECTIVE);// Customer Defective
    else if (buttonClicked.equals(CUSTOMER_GESTURE))
      io.setSWMovement(NCN_CUSTOMER_GESTURE);// Customer Gesture
    OBDal.getInstance().save(io);
   // OBDal.getInstance().flush();
    return io;
  }

  // Method to create RFC Lines
  private OrderLine createRFCLine(Order o, RC_CreditNoteline rcLine, int line) {
    log4j.info("Inside RFCLine() of CreditNoteProcess: " + new Date());
    String originalOrder = EMPTY;
    OBCriteria<Order> obCriteria = OBDal.getInstance().createCriteria(Order.class);
    obCriteria.add(Expression.eq(Order.PROPERTY_DOCUMENTNO, rcLine.getDocno()));
    final List<Order> oList = obCriteria.list();
    if (!oList.isEmpty())
      originalOrder = oList.get(0).getId();

    Product p = null;
    OBCriteria<Product> pCriteria = OBDal.getInstance().createCriteria(Product.class);
    pCriteria.add(Expression.eq(Product.PROPERTY_NAME, rcLine.getCommercialName()));
    final List<Product> pList = pCriteria.list();
    if (!pList.isEmpty())
      p = pList.get(0);
    else
      throw new OBException("Product does not found in ERP for " + rcLine.getCommercialName());
    OrderLine ol = OBProvider.getInstance().get(OrderLine.class);
    ol.setActive(true);
    ol.setClient(o.getClient());
    ol.setOrganization(o.getOrganization());
    ol.setCreationDate(new Date());
    ol.setUpdated(new Date());
    ol.setCreatedBy(o.getCreatedBy());
    ol.setUpdatedBy(o.getUpdatedBy());
    ol.setSalesOrder(o);
    ol.setProduct(p);
    ol.setAttributeSetValue(p.getAttributeSetValue());
    ol.setLineNo((long) line);
    ol.setLineNetAmount(new BigDecimal(0));
    ol.setOrderDate(o.getOrderDate());
    ol.setWarehouse(o.getWarehouse());
    BigDecimal unitPrice = ((rcLine.getPricelevel3() == null || rcLine.getPricelevel3().toString()
        .isEmpty()) ? new BigDecimal(0) : new BigDecimal(rcLine.getPricelevel3()));
    BigDecimal exchangeQty = ((rcLine.getRefundexchangeqty() == null || rcLine
        .getRefundexchangeqty().toString().isEmpty()) ? new BigDecimal(0) : new BigDecimal(
        MINUS.concat(rcLine.getRefundexchangeqty().toString())));
    BigDecimal exchangeAmt = ((rcLine.getEchangeredundprice() == null || rcLine
        .getEchangeredundprice().toString().isEmpty()) ? new BigDecimal(0) : new BigDecimal(
        rcLine.getEchangeredundprice()));
    ol.setOrderedQuantity(exchangeQty);
    ol.setDeliveredQuantity(new BigDecimal(0));
    ol.setInvoicedQuantity(new BigDecimal(0));
    ol.setUnitPrice(unitPrice);
    ol.setDiscount((unitPrice.multiply(exchangeQty)).subtract(exchangeAmt));
    ol.setCurrency(o.getCurrency());
    ol.setUOM(p.getUOM());
    ol.setTax(getTax(o.getOrganization(), p));
    ol.setReturnReason(o.getReturnReason());// Customer Exchange
    ol.setNcnInvoice(originalOrder);// Original Order Id
    OBDal.getInstance().save(ol);
    //OBDal.getInstance().flush();
    return ol;
  }

  // Method to create RFC Header
  private Order createRFCHeader(RCCreditNote rc) {
    log4j.info("Inside RFCHeader() of CreditNoteProcess: " + new Date());
    Order o = OBProvider.getInstance().get(Order.class);
    o.setActive(true);
    o.setClient(rc.getClient());
    o.setOrganization(rc.getOrganization());
    o.setCreationDate(new Date());
    o.setUpdated(new Date());
    o.setCreatedBy(rc.getCreatedBy());
    o.setUpdatedBy(rc.getUpdatedBy());
    o.setDescription(rc.getDocumentNo().concat(",").concat(rc.getExchangedate().toString()));
    // check for blank BP
    BusinessPartner bp = ((rc.getBusinessPartner() == null || rc.getBusinessPartner().equals(EMPTY)) ? (OBDal
        .getInstance().get(BusinessPartner.class, "568FDF23329C4113A667AB3EF49630DF")) : rc
        .getBusinessPartner());
    o.setRCOxylaneno(bp.getRCOxylane());// decathlon-id
    o.setBusinessPartner(bp);
    if (!rc.getBusinessPartner().getBusinessPartnerLocationList().isEmpty()) {
      o.setPartnerAddress(rc.getBusinessPartner().getBusinessPartnerLocationList().get(0));
      o.setInvoiceAddress(rc.getBusinessPartner().getBusinessPartnerLocationList().get(0));
    } else {
      o.setPartnerAddress(getTempLocationForBP());
      o.setInvoiceAddress(getTempLocationForBP());
    }
    DocumentType docType = getDocumentType(RFC_DOCTYPE);
    o.setTransactionDocument(docType);
    o.setDocumentType(docType);
    String docNo = PREFIX.concat(rc.getDocumentNo());
    o.setDocumentNo(docNo);
    o.setOrderDate(rc.getExchangedate());
    o.setAccountingDate(new Date());
    o.setScheduledDeliveryDate(new Date());
    o.setCurrency(getCurrency());// INR
    o.setInvoiceTerms(IMMEDIATE);// Immediate
    o.setPaymentTerms(getPaymentTerms());// Immediate
    o.setPaymentMethod(getPaymentMethod());// Cash
    o.setFormOfPayment(FORM_OF_PAYMENTS);// Cash
    o.setPriority(MEDIUM);// Medium
    o.setPriceList(getPriceList());// DMI CATALOGUE
    o.setDocumentStatus(DRAFT);
    o.setDocumentAction(COMPLETE);
    o.setSalesTransaction(true);
    o.setFreightAmount(new BigDecimal(0));
    o.setFreightCostRule(IMMEDIATE);// Freight included
    o.setWarehouse(getWarehouse(rc.getOrganization()));
    o.setReinvoice(true);
    o.setReceiveMaterials(true);
    o.setReturnReason(getReturnReason((rc.getAcctype().equals(EMPTY) || rc.getAcctype() == null) ? EMPTY
        : rc.getAcctype()));
    o.setNcnCrtype((rc.getCrtype().equals(EMPTY) || rc.getCrtype() == null) ? DEFAULT : rc
        .getCrtype());
    OBDal.getInstance().save(o);
    //OBDal.getInstance().flush();
    return o;
  }

  /* Sub-functions to retrieve default values and required values */

  // getting ToStorageBin for Goods Movement
  private Locator getNewStorageBin(Locator loc, String name) {
    Locator newLoc = null;
    String storeName = loc.getSearchKey().replaceFirst(CUSTOMER, EMPTY).trim();
    if (storeName.equals(EMPTY))
      storeName = WHITEFIELD;
    storeName = name.concat(" ").concat(storeName);

    OBCriteria<Locator> obCriteria = OBDal.getInstance().createCriteria(Locator.class);
    obCriteria.add(Expression.eq(Locator.PROPERTY_SEARCHKEY, storeName));
    final List<Locator> locList = obCriteria.list();
    if (!locList.isEmpty())
      newLoc = locList.get(0);
    else
      throw new OBException("Locator not found for store->" + storeName);
    return newLoc;
  }

  // getting Warehouse using Organization
  private Warehouse getWarehouse(Organization org) {
    Warehouse w = null;
    OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
    obCriteria.add(Expression.eq(Warehouse.PROPERTY_ORGANIZATION, org));
    if (org.getName().equals("BGT"))// Bannerghatta
      obCriteria.add(Expression.eq(Warehouse.PROPERTY_NAME, CUSTOMER + " " + BGT));
    else
      obCriteria.add(Expression.eq(Warehouse.PROPERTY_NAME, CUSTOMER + " "
          + org.getName().replaceAll("Store", EMPTY).trim()));
    final List<Warehouse> wList = obCriteria.list();
    if (!wList.isEmpty())
      w = wList.get(0);
    else
      throw new OBException("Warehouse not found for Organization->" + org.getName());
    return w;
  }

  // getting Temp Business Partner Location
  private org.openbravo.model.common.businesspartner.Location getTempLocationForBP() {
    org.openbravo.model.common.businesspartner.Location tempBPLocation = null;
    OBCriteria<BusinessPartner> obCriteria = OBDal.getInstance().createCriteria(
        BusinessPartner.class);
    obCriteria.add(Expression.eq(BusinessPartner.PROPERTY_NAME, TEMP));
    final List<BusinessPartner> tempBPList = obCriteria.list();
    if (!tempBPList.isEmpty())
      tempBPLocation = tempBPList.get(0).getBusinessPartnerLocationList().get(0);
    else
      throw new OBException("Business Partner or its Location is not found for Temp");

    return tempBPLocation;
  }

  // getting Return Reason
  private ReturnReason getReturnReason(String returnReason) {
    ReturnReason reason = null;
    OBCriteria<ReturnReason> obCriteria = OBDal.getInstance().createCriteria(ReturnReason.class);
    obCriteria.add(Expression.eq(ReturnReason.PROPERTY_SEARCHKEY, returnReason));
    final List<ReturnReason> reasonList = obCriteria.list();
    if (!reasonList.isEmpty())
      reason = reasonList.get(0);
    else
      throw new OBException("Return Reason not created in ERP for->" + returnReason);

    return reason;
  }

  // getting documentNo for Credit Note
  private String getDocSequenceNo() {
    Sequence seq = null;
    String docNo = EMPTY;
    OBCriteria<Sequence> obCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    obCriteria.add(Expression.eq(Sequence.PROPERTY_NAME, CRN_DOCSEQ));
    final List<Sequence> seqList = obCriteria.list();
    if (!seqList.isEmpty()) {
      seq = seqList.get(0);
      docNo = seq.getPrefix().concat(seq.getNextAssignedNumber().toString());
      // to increment the next documentNo in the Document Sequence
      String dNo = docNo;
      // prefix is CRN-
      seq.setNextAssignedNumber(Long.parseLong(dNo.replaceAll(PREFIX, EMPTY).trim()) + 1);
      OBDal.getInstance().save(seq);
    } else
      throw new OBException("Document Sequence not created in ERP for Credit Note");

    return docNo;
  }

  // getting currency for INR
  private Currency getCurrency() {
    Currency c = null;
    OBCriteria<Currency> obCriteria = OBDal.getInstance().createCriteria(Currency.class);
    obCriteria.add(Expression.eq(Currency.PROPERTY_ISOCODE, CURRENCY));
    final List<Currency> currencyList = obCriteria.list();
    if (!currencyList.isEmpty())
      c = currencyList.get(0);
    else
      throw new OBException("Currency is different. It should be INR.");
    return c;
  }

  // getting Price List as DMI CATALOGUE
  private PriceList getPriceList() {
    PriceList priceList = null;
    OBCriteria<PriceList> obCriteria = OBDal.getInstance().createCriteria(PriceList.class);
    obCriteria.add(Expression.eq(PriceList.PROPERTY_NAME, PRICELIST));
    final List<PriceList> plList = obCriteria.list();
    if (!plList.isEmpty())
      priceList = plList.get(0);
    else
      throw new OBException("Price List not defined for->" + PRICELIST);
    return priceList;
  }

  // getting Payment terms as Immediate
  private PaymentTerm getPaymentTerms() {
    PaymentTerm payTerms = null;
    OBCriteria<PaymentTerm> obCriteria = OBDal.getInstance().createCriteria(PaymentTerm.class);
    obCriteria.add(Expression.eq(PaymentTerm.PROPERTY_NAME, PAYMENT_TERMS));
    final List<PaymentTerm> payTermsList = obCriteria.list();
    if (!payTermsList.isEmpty())
      payTerms = payTermsList.get(0);
    else
      throw new OBException("Payment Term not defined for->" + PAYMENT_TERMS);
    return payTerms;
  }

  // getting Payment method as Cash
  private FIN_PaymentMethod getPaymentMethod() {
    FIN_PaymentMethod payMethod = null;
    OBCriteria<FIN_PaymentMethod> obCriteria = OBDal.getInstance().createCriteria(
        FIN_PaymentMethod.class);
    obCriteria.add(Expression.eq(FIN_PaymentMethod.PROPERTY_NAME, PAYMENT_METHOD));
    final List<FIN_PaymentMethod> payMethodList = obCriteria.list();
    if (!payMethodList.isEmpty())
      payMethod = payMethodList.get(0);
    else
      throw new OBException("Payment Method not defined for->" + PAYMENT_METHOD);
    return payMethod;
  }

  // getting document Type for 'Credit Note Order' and 'Credit Note Receipt'
  private DocumentType getDocumentType(String docTypeName) {
    DocumentType docType = null;
    OBCriteria<DocumentType> obCriteria = OBDal.getInstance().createCriteria(DocumentType.class);
    obCriteria.add(Expression.eq(DocumentType.PROPERTY_NAME, docTypeName));
    final List<DocumentType> docTypeList = obCriteria.list();
    if (!docTypeList.isEmpty())
      docType = docTypeList.get(0);
    else
      throw new OBException("Document Type not created in ERP for->" + docTypeName);
    return docType;
  }

  // getting Tax
  private TaxRate getTax(Organization organization, Product p) {
    TaxRate tax = null;
    OBCriteria<TaxRate> obCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
    Region regionName = organization.getOrganizationInformationList().get(0).getLocationAddress()
        .getRegion();
    obCriteria.add(Expression.eq(TaxRate.PROPERTY_REGION, regionName));
    obCriteria.add(Expression.eq(TaxRate.PROPERTY_TAXCATEGORY, p.getTaxCategory()));
    final List<TaxRate> taxList = obCriteria.list();
    if (!taxList.isEmpty()) {
      tax = taxList.get(0);
    } else {
      OBCriteria<TaxRate> objCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
      objCriteria.add(Expression.eq(TaxRate.PROPERTY_TAXCATEGORY, p.getTaxCategory()));
      final List<TaxRate> taxesList = objCriteria.list();
      for (TaxRate t : taxesList) {
        for (int i = 0; i < t.getFinancialMgmtTaxZoneList().size(); i++) {
          if (t.getFinancialMgmtTaxZoneList().get(i).getFromRegion().equals(regionName)) {
            tax = t.getFinancialMgmtTaxZoneList().get(i).getTax();
          }
        }
      }

    }
    return tax;
  }

  // calling respective processes to post
  private ProcessInstance callProcessToPost(String userId, String id, String pName,
      Boolean isOrderPost) {
    OBContext.setAdminMode(true);
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, pName);// C_Order_Post or C_Invoice_Post or
                                                              // M_Inout_Post
    } finally {
      OBContext.restorePreviousMode();
    }
    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put(AD_USER_ID, userId);
    if (isOrderPost) {
      parameters.put(C_ORDER_ID, id);
    } else {
      parameters.put(M_INOUT_ID, id);
    }
    final ProcessInstance pInstanceId = CallProcess.getInstance().callProcess(process, id,
        parameters);
    return pInstanceId;
  }

  private void executePayments() {
    log4j.info("Inside executePayments() of CreditNoteProcess: " + new Date());
    OBContext.setAdminMode();
    try {
      if (getLeaveAsCreditProcesses().isEmpty()) {
        return;
      }
      List<APRMPendingPaymentFromInvoice> pendingPayments = getPendingPayments();
      if (pendingPayments.isEmpty()) {
        return;
      }
      List<FIN_Payment> payments = new ArrayList<FIN_Payment>();
      PaymentExecutionProcess executionProcess = null;
      Organization organization = null;
      AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
      for (APRMPendingPaymentFromInvoice pendingPayment : pendingPayments) {
        if (executionProcess != null
            && organization != null
            && (executionProcess != pendingPayment.getPaymentExecutionProcess() || organization != pendingPayment
                .getOrganization())) {
          if (dao.isAutomaticExecutionProcess(executionProcess)) {
            FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
            executePayment.init("OTHER", executionProcess, payments, null,
                pendingPayment.getOrganization());
            executePayment.execute();
          }
          payments.clear();
        }
        executionProcess = pendingPayment.getPaymentExecutionProcess();
        organization = pendingPayment.getOrganization();
        payments.add(pendingPayment.getPayment());

      }
      if (dao.isAutomaticExecutionProcess(executionProcess)) {
        FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
        executePayment.init("APP", executionProcess, payments, null, organization);
        executePayment.execute();
      }
    } finally {
      OBContext.restorePreviousMode();

    }
  }

  private List<APRMPendingPaymentFromInvoice> getPendingPayments() {
    OBCriteria<APRMPendingPaymentFromInvoice> ppfiCriteria = OBDal.getInstance().createCriteria(
        APRMPendingPaymentFromInvoice.class);
    ppfiCriteria.add(Restrictions.eq(APRMPendingPaymentFromInvoice.PROPERTY_PROCESSNOW, false));
    ppfiCriteria.add(Restrictions
        .in(APRMPendingPaymentFromInvoice.PROPERTY_PAYMENTEXECUTIONPROCESS,
            getLeaveAsCreditProcesses()));
    ppfiCriteria.addOrderBy(APRMPendingPaymentFromInvoice.PROPERTY_PAYMENTEXECUTIONPROCESS, false);
    ppfiCriteria.addOrderBy(APRMPendingPaymentFromInvoice.PROPERTY_ORGANIZATION, false);
    return ppfiCriteria.list();
  }

  private List<PaymentExecutionProcess> getLeaveAsCreditProcesses() {
    OBCriteria<PaymentExecutionProcess> payExecProcCrit = OBDal.getInstance().createCriteria(
        PaymentExecutionProcess.class);
    payExecProcCrit.add(Restrictions.eq(PaymentExecutionProcess.PROPERTY_JAVACLASSNAME,
        "org.openbravo.advpaymentmngt.executionprocess.LeaveAsCredit"));

    return payExecProcCrit.list();
  }

  public String getServletInfo() {
    return "Servlet CreateCreditNote";
  } // end of getServletInfo() method
}
