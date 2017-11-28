package in.nous.creditnote.ad_webservices;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
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
import org.openbravo.model.ad.access.User;
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
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The Web Service processes the Credit Note
 * 
 */

public class CreditNoteEcomWebService extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(CreditNoteEcomWebService.class);
  private static final String ORG = "adOrgId";
  private static final String BPARTNER = "cBPartnerId";
  private static final String DECATHLONID = "decathlonId";
  private static final String USER = "adUserId";
  private static final String BUTTON = "buttonClicked";
  private static final String LINE = "line";
  private static final String PRODUCT = "mProductId";
  private static final String BILLNO = "billNo";
  private static final String ORDERLINEID = "orderlineId";
  private static final String QTY = "exchangeQty";
  private static final String PRICEACTUAL = "priceactual";
  private static final String PRICE = "unitPrice";
  private static final String AMT = "exchangeAmt";
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
  private static final String SHORT_SHIPMENT = "SS";
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
  private static final String EMPTY = "";
  private static final String NCN_CUSTOMER_EXCHANGE = "NCN_Customer Exchange";
  private static final String NCN_SHORT_SHIPMENT = "NCN_Customer EcomShortShipment";
  private static final String NCN_CUSTOMER_DEFECTIVE = "NCN_Customer Defective";
  private static final String NCN_CUSTOMER_GESTURE = "NCN_Customer Gesture";
  private static final String TEMP = "Temp";
  private static final String MOBILE = "mobile";
  private static final String EMAIL = "email";
  private static final String LANDLINE = "landline";
  private static final String ECOMMERCE = "Ecommerce";
  private static final String ECOM_RETURNBIN = "Ecommerce Return Bin";
  private static final String COMMENTBOX = "commentbox";
  private static final String SHORT_SHIPMENT_ECOM = "SE"; // from reference window SE= Shipment for
                                                          // Ecommerce

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs the POST REST operation.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String xml = "";
    String strRead = "";

    InputStreamReader isReader = new InputStreamReader(request.getInputStream());
    BufferedReader bReader = new BufferedReader(isReader);
    StringBuilder strBuilder = new StringBuilder();
    strRead = bReader.readLine();
    while (strRead != null) {
      strBuilder.append(strRead);
      strRead = bReader.readLine();
    }
    xml = strBuilder.toString();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);
    JSONObject jsonDataObject = new JSONObject();
    try {
      jsonDataObject = processCreditNote(document);
    } catch (JSONException e) {
      log.error("A JSON error occurred while processing Credit Note", e);
    }
    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());
    w.close();
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  /**
   * Method for performing credit note process
   * 
   * @param document
   * @return jsonDataObject
   */
  private JSONObject processCreditNote(Document document) throws JSONException {
    JSONObject jsonDataObject = new JSONObject();
    List<CreditNoteDTO> listCreditNoteDTO = new ArrayList<CreditNoteDTO>();
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObject = new JSONObject();
    String adOrgId = null;
    String bPartnerId = null;
    String userId = null;
    String decathlonId = null;
    String buttonClicked = null;
    String mobile = null;
    String email = null;
    String landline = null;
    String ecomCommentbox = null;
    try {
      adOrgId = document.getElementsByTagName(ORG).item(0).getChildNodes().item(0).getNodeValue();
      // bPartnerId = document.getElementsByTagName(BPARTNER).item(0).getChildNodes().item(0)
      // .getNodeValue();
      // bPartnerId = getDefaultBPartnerID();
      bPartnerId = "4F133B48A78E42B0BC7EBC01DB890EFA";
      userId = document.getElementsByTagName(USER).item(0).getChildNodes().item(0).getNodeValue();
      decathlonId = document.getElementsByTagName(DECATHLONID).item(0).getChildNodes().item(0)
          .getNodeValue();
      buttonClicked = document.getElementsByTagName(BUTTON).item(0).getChildNodes().item(0)
          .getNodeValue();
      if (null != document.getElementsByTagName(MOBILE).item(0).getChildNodes().item(0)) {
        mobile = document.getElementsByTagName(MOBILE).item(0).getChildNodes().item(0)
            .getNodeValue();
      }

      if (null != document.getElementsByTagName(EMAIL).item(0).getChildNodes().item(0)) {
        email = document.getElementsByTagName(EMAIL).item(0).getChildNodes().item(0).getNodeValue();
      }
      if (null != document.getElementsByTagName(LANDLINE).item(0).getChildNodes().item(0)) {
        landline = document.getElementsByTagName(LANDLINE).item(0).getChildNodes().item(0)
            .getNodeValue();
      }
      if (null != document.getElementsByTagName(COMMENTBOX).item(0).getChildNodes().item(0)) {
        ecomCommentbox = document.getElementsByTagName(COMMENTBOX).item(0).getChildNodes().item(0)
            .getNodeValue();
      }

      // for erp's header info
      CreditNoteDTO cnHeaderDTO = new CreditNoteDTO();
      cnHeaderDTO.setAdOrgId(adOrgId);
      cnHeaderDTO.setAdUserId(userId);
      cnHeaderDTO.setcBPartnerId(bPartnerId);
      cnHeaderDTO.setDecathlonId(decathlonId);
      cnHeaderDTO.setButtonClicked(buttonClicked);
      cnHeaderDTO.setMobile(mobile);
      cnHeaderDTO.setEmail(email);
      cnHeaderDTO.setLandline(landline);
      cnHeaderDTO.setEcomReturnComment(ecomCommentbox);

      log.debug("Credit Note Header Data received: " + cnHeaderDTO.toString());

      // for erp's lines info
      NodeList list = document.getElementsByTagName(LINE);
      for (int indexParent = 0; indexParent < list.getLength(); indexParent++) {
        Node fieldNode = list.item(indexParent);
        NodeList listChildren = fieldNode.getChildNodes();
        CreditNoteDTO cnLinesDTO = new CreditNoteDTO();
        for (int indexChild = 0; indexChild < listChildren.getLength(); indexChild++) {
          if (listChildren.item(indexChild).getNodeName().equals(PRODUCT)) {
            cnLinesDTO.setmProductId(listChildren.item(indexChild).getChildNodes().item(0)
                .getNodeValue().toString());
          }
          if (listChildren.item(indexChild).getNodeName().equals(ORDERLINEID)) {
            cnLinesDTO.setOrderId(listChildren.item(indexChild).getChildNodes().item(0)
                .getNodeValue().toString());
          }
          if (listChildren.item(indexChild).getNodeName().equals(BILLNO)) {
            cnLinesDTO.setBillNo(listChildren.item(indexChild).getChildNodes().item(0)
                .getNodeValue().toString());
          }
          if (listChildren.item(indexChild).getNodeName().equals(PRICE)) {
            cnLinesDTO.setUnitPrice(new BigDecimal(listChildren.item(indexChild).getChildNodes()
                .item(0).getNodeValue()));
          }
          if (listChildren.item(indexChild).getNodeName().equals(PRICEACTUAL)) {
            cnLinesDTO.setPriceActual(new BigDecimal(listChildren.item(indexChild).getChildNodes()
                .item(0).getNodeValue()));
          }
          if (listChildren.item(indexChild).getNodeName().equals(AMT)) {
            cnLinesDTO.setExchangeAmt(new BigDecimal(listChildren.item(indexChild).getChildNodes()
                .item(0).getNodeValue()));
          }
          if (listChildren.item(indexChild).getNodeName().equals(QTY)) {
            cnLinesDTO.setExchangeQty(new BigDecimal(listChildren.item(indexChild).getChildNodes()
                .item(0).getNodeValue()));
          }
        }
        listCreditNoteDTO.add(cnLinesDTO);
      }

      // create RFC header
      log.debug("Creating RFC...");
      Order rfc = createRFCHeader(cnHeaderDTO);
      log.debug("Created RFC" + " with ID->" + rfc.getId());

      // create RMR header
      log.debug("Creating RMR ...");
      ShipmentInOut rmr = createRMRHeader(rfc, cnHeaderDTO);
      log.debug("Created RMR" + " with ID->" + rmr.getId());

      // create GM header
      InternalMovement gm = createGMHeader(rmr, cnHeaderDTO);

      // create RFC lines and RMR lines and GM lines
      log.debug("Creating RFC, RMR Lines...");

      int line = 10;
      for (CreditNoteDTO cnLinesDTO : listCreditNoteDTO) {
        OrderLine rfcLine = createRFCLine(rfc, cnLinesDTO, line);
        ShipmentInOutLine rmrLine = createRMRLine(rmr, rfcLine, cnLinesDTO, line);
        createGMLine(gm, rmrLine, line, cnHeaderDTO);
        line = line + 10;
      }

      // Booking RFC
      log.debug("Posting RFC...");

      // C_Order_Post
      ProcessInstance pInstanceIdForRFC = callProcessToPost(null, rfc.getId(),
          ORDER_POST_PROCEDURE, true);

      log.debug("pInstanceIdForRFC: " + pInstanceIdForRFC);
      log.debug("Posted RFC Successfully.");

      // Booking RMR
      log.debug("Posting RMR...");

      rmr.setProcessed(true);
      rmr.setDocumentStatus(COMPLETE);
      rmr.setDocumentAction(DASH);
      OBDal.getInstance().save(rmr);
      // M_Movement_Post
      ProcessInstance pInstanceIdForGM = callProcessToPost(gm, gm.getId(), MOVEMENT_POST_PROCEDURE,
          false);
      log.debug("pInstanceIdForGM: " + pInstanceIdForGM);
      log.debug("Posted RMR(GM) Successfully.");

      // Creating Credit Note Header
      log.debug("Creating Credit Note Header...");

      Invoice inv = createInvoiceHeader(rfc);
      log.debug("Created Credit Note (Invoice) Header with ID->" + inv.getId());

      // Creating Credit Note Lines
      log.debug("Creating Credit Note (Invoice) Header...");

      int l = 10;
      for (CreditNoteDTO cnLinesDTO : listCreditNoteDTO) {
        createInvoiceLines(inv, cnLinesDTO, l);
        l = l + 10;
      }
      log.debug("Created Credit Note (Invoice) Lines.");

      // Booking Invoice C_Invoice_Post
      System.out.println("error bfor post111" + inv.getId());
      ProcessInstance pInstanceIdForInvoice = callProcessToPost(null, inv.getId(),
          INVOICE_POST_PROCEDURE, true);
      System.out.println("error while post222" + pInstanceIdForInvoice.getResult());
      System.out.println("error bfor post3333" + pInstanceIdForInvoice);

      if (pInstanceIdForInvoice == null || pInstanceIdForInvoice.getResult() == 0L) {
        // Error during Invoice post
        log.error("Error occurred while posting Invoice (Credit Note): "
            + (pInstanceIdForInvoice != null ? pInstanceIdForInvoice.getErrorMsg()
                : "pInstanceIdForInvoice: null"));
        throw new OBException("Error occurred whilewhile posting Invoice.");
      } else {
        jsonObject.put("status", "success");
        jsonObject.put("InvoiceDocNos", inv.getDocumentNo());
        log.debug("Created Credit Note Successfully.");
        // Check if there is any payment pending execution for this invoice.
        try {
          log.debug("Executing Payments for Credit Note");
          executePayments();
        } catch (Exception e) {
          log.error("Error occurred while executing payments for Credit Note-> ", e);
          log.debug("Rolling back the DB changes");
          jsonObject.put("status", "failure");
          jsonArray.put(jsonObject);
          jsonDataObject.put("data", jsonArray);
          OBDal.getInstance().rollbackAndClose();
          return jsonDataObject;
        }
      }
      jsonArray.put(jsonObject);
      jsonDataObject.put("data", jsonArray);
      log.debug("Webservice post completed.");
    } catch (Exception exp) {
      log.error("Error occurred while creating credit note in processCreditNote(): ", exp);
      log.debug("Rolling back the DB changes");
      OBDal.getInstance().rollbackAndClose();
      jsonObject.put("status", "failure");
      jsonArray.put(jsonObject);
      jsonDataObject.put("data", jsonArray);
    }
    return jsonDataObject;
  }

  /**
   * Method to create Invoice Lines
   */
  private InvoiceLine createInvoiceLines(Invoice inv, CreditNoteDTO cnLinesDTO, int line) {
    Product p = OBDal.getInstance().get(Product.class, cnLinesDTO.getmProductId());
    OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, cnLinesDTO.getOrderId());

    InvoiceLine invLine = OBProvider.getInstance().get(InvoiceLine.class);
    TaxRate taxRate = getTax(inv.getOrganization(), p);
    invLine.setActive(true);
    invLine.setClient(inv.getClient());
    invLine.setOrganization(inv.getOrganization());
    invLine.setCreationDate(new Date());
    invLine.setUpdated(new Date());
    invLine.setCreatedBy(inv.getCreatedBy());
    invLine.setUpdatedBy(inv.getUpdatedBy());
    invLine.setInvoice(inv);
    invLine.setBusinessPartner(inv.getBusinessPartner());
    invLine.setInvoicedQuantity(cnLinesDTO.getExchangeQty());
    invLine
        .setGrossUnitPrice(cnLinesDTO.getExchangeAmt().divide(cnLinesDTO.getExchangeQty()).abs());
    invLine.setUnitPrice((invLine.getGrossUnitPrice().subtract(calculateTaxAmount(
        taxRate.getRate(), invLine.getGrossUnitPrice()))).abs());
    invLine.setProduct(p);
    invLine.setDescription(cnLinesDTO.getEcomReturnComment());
    invLine.setAttributeSetValue(p.getAttributeSetValue());
    invLine.setLineNo((long) line);
    invLine.setUOM(p.getUOM());
    invLine.setTax(getTax(inv.getOrganization(), p));
    invLine.setSalesOrderLine(orderLine);
    invLine.setGoodsShipmentLine(orderLine.getGoodsShipmentLine());
    invLine.setLineNetAmount((invLine.getUnitPrice().multiply(cnLinesDTO.getExchangeQty())).abs());
    invLine.setGrossAmount(cnLinesDTO.getExchangeAmt());
    OBDal.getInstance().save(invLine);
    return invLine;
  }

  /**
   * Method to create Invoice Header
   */
  private Invoice createInvoiceHeader(Order o) {
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
    inv.setDescription(o.getDescription());
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
    inv.setNcnDecathlon(o.getRCOxylaneno());// Decathlon Id
    OBDal.getInstance().save(inv);
    return inv;
  }

  /**
   * Method to create GM Lines
   */
  private InternalMovementLine createGMLine(InternalMovement gm, ShipmentInOutLine iol, int line,
      CreditNoteDTO cnHeaderDTO) {
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
    gml.setDescription(cnHeaderDTO.getEcomReturnComment());
    gml.setStorageBin(iol.getStorageBin());
    System.out.println("iol.getStorageBin() " + iol.getStorageBin());
    System.out.println("gml.getStorageBinnn " + gml.getStorageBin());
    if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_EXCHANGE)) {
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), SALEABLE));
    } else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_DEFECTIVE)) {
      System.out.println("defectivebin bfor " + iol.getStorageBin());
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), DEFECTIVE));
      System.out.println("defectivebin after gml.getNewStorageBin() " + gml.getNewStorageBin());
    } else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_GESTURE)) {
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), WRITEDOWN));
    } else if (cnHeaderDTO.getButtonClicked().equals(SHORT_SHIPMENT)) {
      gml.setNewStorageBin(getNewStorageBin(iol.getStorageBin(), ECOM_RETURNBIN));
    }
    gml.setUOM(iol.getUOM());
    gml.setAttributeSetValue(iol.getAttributeSetValue());
    gml.setMovementQuantity(iol.getMovementQuantity().abs());
    OBDal.getInstance().save(gml);
    return gml;
  }

  /**
   * Method to create GM Header
   */
  private InternalMovement createGMHeader(ShipmentInOut io, CreditNoteDTO cnHeaderDTO) {
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
    gm.setDescription(cnHeaderDTO.getEcomReturnComment());
    if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_EXCHANGE))
      gm.setSWMovementtypegm(CUSTOMER_EXCHANGE);
    else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_DEFECTIVE))
      gm.setSWMovementtypegm(CUSTOMER_DEFECTIVE);
    else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_GESTURE))
      gm.setSWMovementtypegm(WRITE);
    else if (cnHeaderDTO.getButtonClicked().equals(SHORT_SHIPMENT))
      gm.setSWMovementtypegm(SHORT_SHIPMENT_ECOM);
    gm.setName(io.getMovementDate().toString());
    OBDal.getInstance().save(gm);
    return gm;
  }

  /**
   * Method to create RMR Lines
   */
  private ShipmentInOutLine createRMRLine(ShipmentInOut io, OrderLine ol, CreditNoteDTO cnLinesDTO,
      int line) {
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
    iol.setMovementQuantity(cnLinesDTO.getExchangeQty());
    iol.setReinvoice(true);
    iol.setDescription(cnLinesDTO.getEcomReturnComment());
    OBDal.getInstance().save(iol);
    ol.setDeliveredQuantity(iol.getMovementQuantity());
    ol.setGoodsShipmentLine(iol);
    OBDal.getInstance().save(ol);
    return iol;
  }

  /**
   * Method to create RMR Header
   */
  private ShipmentInOut createRMRHeader(Order o, CreditNoteDTO cnHeaderDTO) {
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
    io.setDescription(cnHeaderDTO.getEcomReturnComment());
    if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_EXCHANGE))
      io.setSWMovement(NCN_CUSTOMER_EXCHANGE);// Customer Exchange
    else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_DEFECTIVE))
      io.setSWMovement(NCN_CUSTOMER_DEFECTIVE);// Customer Defective
    else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_GESTURE))
      io.setSWMovement(NCN_CUSTOMER_GESTURE);// Customer Gesture
    else if (cnHeaderDTO.getButtonClicked().equals(SHORT_SHIPMENT))
      io.setSWMovement(NCN_SHORT_SHIPMENT);// Short Shipment
    OBDal.getInstance().save(io);
    return io;
  }

  /**
   * Method to create RFC Lines
   */
  private OrderLine createRFCLine(Order o, CreditNoteDTO cnLinesDTO, int line) {
    Product p = OBDal.getInstance().get(Product.class, cnLinesDTO.getmProductId());
    OrderLine ol = OBProvider.getInstance().get(OrderLine.class);
    TaxRate taxRate = getTax(o.getOrganization(), p);
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
    // ol.setLineNetAmount((cnLinesDTO.getUnitPrice().multiply(cnLinesDTO.getExchangeQty())).abs());
    // ol.setLineGrossAmount((cnLinesDTO.getUnitPrice().multiply(cnLinesDTO.getExchangeQty())).abs());
    ol.setLineGrossAmount(cnLinesDTO.getExchangeAmt());
    ol.setGrossUnitPrice(cnLinesDTO.getExchangeAmt().divide(cnLinesDTO.getExchangeQty()).abs());
    ol.setOrderDate(new Date());
    ol.setWarehouse(o.getWarehouse());
    ol.setOrderedQuantity(cnLinesDTO.getExchangeQty());// exchange qty
    ol.setDeliveredQuantity(new BigDecimal(0));
    ol.setInvoicedQuantity(new BigDecimal(0));
    ol.setDSEMDSUnitQty(cnLinesDTO.getExchangeQty().abs());
    ol.setUnitPrice((ol.getGrossUnitPrice().subtract(calculateTaxAmount(taxRate.getRate(),
        ol.getGrossUnitPrice()))).abs());
    // ol.setLineNetAmount((ol.getUnitPrice().multiply(cnLinesDTO.getExchangeQty())).abs());
    // ol.setDiscount((cnLinesDTO.getPriceActual().multiply(cnLinesDTO.getExchangeQty())).subtract(cnLinesDTO.getExchangeAmt()));
    ol.setCurrency(o.getCurrency());
    ol.setUOM(p.getUOM());
    ol.setTax(taxRate);
    ol.setReturnReason(o.getReturnReason());// Customer Exchange
    ol.setDescription(cnLinesDTO.getEcomReturnComment());
    ol.setNcnInvoice(cnLinesDTO.getOrderId());// Original Order Id
    ol.setDSTaxamount(cnLinesDTO
        .getExchangeAmt()
        .subtract(
            cnLinesDTO
                .getExchangeAmt()
                .multiply(
                    new BigDecimal(100.0)
                        .divide(new BigDecimal(100.0).add(taxRate.getRate()).abs(), 2,
                            RoundingMode.HALF_UP).abs()).abs()).abs());
    OBDal.getInstance().save(ol);
    return ol;
  }

  private BigDecimal calculateTaxAmount(BigDecimal rate, BigDecimal amount) {
    BigDecimal taxRate = amount.multiply(rate).divide(new BigDecimal(100.0));
    return taxRate;
  }

  /**
   * Method to create RFC Header
   */
  private Order createRFCHeader(CreditNoteDTO cnHeaderDTO) {
    Organization org = OBDal.getInstance().get(Organization.class, cnHeaderDTO.getAdOrgId());
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class,
        cnHeaderDTO.getcBPartnerId());
    User user = OBDal.getInstance().get(User.class, cnHeaderDTO.getAdUserId());

    Order o = OBProvider.getInstance().get(Order.class);
    o.setActive(true);
    o.setClient(org.getClient());
    o.setOrganization(org);
    o.setCreationDate(new Date());
    o.setUpdated(new Date());
    o.setCreatedBy(user);
    o.setUpdatedBy(user);
    o.setBusinessPartner(bp);
    if (!bp.getBusinessPartnerLocationList().isEmpty()) {
      o.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
      o.setInvoiceAddress(bp.getBusinessPartnerLocationList().get(0));
    } else {
      o.setPartnerAddress(getTempLocationForBP());
      o.setInvoiceAddress(getTempLocationForBP());
    }
    DocumentType docType = getDocumentType(RFC_DOCTYPE);
    o.setTransactionDocument(docType);
    o.setDocumentType(docType);
    String docNo = getDocSequenceNo();
    o.setDocumentNo(docNo);
    o.setOrderDate(new Date());
    o.setAccountingDate(new Date());
    o.setScheduledDeliveryDate(new Date());
    o.setCurrency(getCurrency());// INR
    o.setInvoiceTerms(IMMEDIATE);// Immediate
    o.setPaymentTerms(getPaymentTerms());// After Delivery
    o.setPaymentMethod(getPaymentMethod());// Cash
    o.setFormOfPayment(FORM_OF_PAYMENTS);// Cash
    o.setPriority(MEDIUM);// Medium
    o.setPriceList(getPriceList());// DMI CATALOGUE
    o.setDocumentStatus(DRAFT);
    o.setDocumentAction(COMPLETE);
    o.setSalesTransaction(true);
    o.setFreightAmount(new BigDecimal(0));
    o.setFreightCostRule(IMMEDIATE);// Freight included
    o.setWarehouse(getWarehouse(org));
    o.setReinvoice(true);
    o.setReceiveMaterials(true);
    o.setRCOxylaneno(cnHeaderDTO.getDecathlonId());// decathlon-id
    o.setRCMobileNo(cnHeaderDTO.getMobile());
    o.setSyncEmail(cnHeaderDTO.getEmail());
    o.setSyncLandline(cnHeaderDTO.getLandline());
    o.setDescription(cnHeaderDTO.getEcomReturnComment());

    if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_EXCHANGE))
      o.setReturnReason(getReturnReason(CUSTOMER_EXCHANGE));// Customer Exchange
    else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_DEFECTIVE))
      o.setReturnReason(getReturnReason(CUSTOMER_DEFECTIVE));// Customer Defective
    else if (cnHeaderDTO.getButtonClicked().equals(CUSTOMER_GESTURE))
      o.setReturnReason(getReturnReason(CUSTOMER_GESTURE));// Customer Gesture
    else if (cnHeaderDTO.getButtonClicked().equals(SHORT_SHIPMENT))
      o.setReturnReason(getReturnReason(SHORT_SHIPMENT));// Short Shipment
    OBDal.getInstance().save(o);
    return o;
  }

  // Below are methods to retrieve default values and required values

  /**
   * Method to get ToStorageBin for Goods Movement
   */
  private Locator getNewStorageBin(Locator loc, String name) {
    Locator newLoc = null;
    String storeName = loc.getSearchKey().replaceFirst(CUSTOMER, EMPTY).trim();
    if (storeName.equals(EMPTY))
      storeName = WHITEFIELD;
    // set ToStorage Bin for Ecomm
    if (storeName.equals("Saleable Ecommerce")) {
      storeName = "Ecommerce Return Bin";
    } else {
      storeName = name.concat(" ").concat(storeName);
    }

    OBCriteria<Locator> obCriteria = OBDal.getInstance().createCriteria(Locator.class);
    obCriteria.add(Expression.eq(Locator.PROPERTY_SEARCHKEY, storeName));
    final List<Locator> locList = obCriteria.list();
    if (!locList.isEmpty())
      newLoc = locList.get(0);
    else
      throw new OBException("Locator not found for store->" + storeName);
    return newLoc;
  }

  /**
   * Method to get Warehouse using Organization
   */
  private Warehouse getWarehouse(Organization org) {
    Warehouse w = null;
    OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
    obCriteria.add(Expression.eq(Warehouse.PROPERTY_ORGANIZATION, org));
    if (org.getName().equals("Ecommerce"))// Ecommerce
      obCriteria.add(Expression.eq(Warehouse.PROPERTY_NAME, SALEABLE + " " + ECOMMERCE));
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

  /**
   * Method to get Default Business Partner ID
   */
  private String getDefaultBPartnerID() {
    DSIDEFModuleConfig bp = null;
    OBContext.setAdminMode();
    OBCriteria<DSIDEFModuleConfig> obCriteria = OBDal.getInstance().createCriteria(
        DSIDEFModuleConfig.class);
    obCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_KEY, "defaultcustomer"));
    obCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.etlsync"));
    final List<DSIDEFModuleConfig> cBPList = obCriteria.list();
    if (!cBPList.isEmpty())
      bp = cBPList.get(0);
    else
      throw new OBException("Default C_BPartner_ID not found in dsidef_module_config table");
    OBContext.restorePreviousMode();
    return bp.getSearchKey();
  }

  /**
   * Method to get Temp Business Partner Location
   */
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

  /**
   * Method to get Return Reason
   */
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

  /**
   * Method to get documentNo for Credit Note
   */
  private String getDocSequenceNo() {
    Sequence seq = null;
    String docNo = "";
    OBCriteria<Sequence> obCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    obCriteria.add(Expression.eq(Sequence.PROPERTY_NAME, CRN_DOCSEQ));
    List<Sequence> seqList = obCriteria.list();
    if (!seqList.isEmpty()) {
      seq = seqList.get(0);
      docNo = seq.getPrefix().concat(seq.getNextAssignedNumber().toString());
      // to increment the next documentNo in the Document Sequence
      String dNo = docNo;
      // prefix is CRN-
      seq.setNextAssignedNumber(Long.parseLong(dNo.replaceAll(PREFIX, "").trim()) + 1);
      OBDal.getInstance().save(seq);
    } else
      throw new OBException("Document Sequence not created in ERP for Credit Note");

    return docNo;
  }

  /**
   * Method to get currency for INR
   */
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

  /**
   * Method to get Price List as DMI CATALOGUE
   */
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

  /**
   * Method to get Payment terms as Immediate
   */
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

  /**
   * Method to get Payment method as Cash
   */
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

  /**
   * Method to get document Type for 'Credit Note Order' and 'Credit Note Receipt'
   */
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

  /**
   * Method to get Tax
   */
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

  /**
   * Method to call respective processes to post
   */
  private ProcessInstance callProcessToPost(InternalMovement gm, String id, String pName,
      Boolean isOrderPost) {
    OBContext.setAdminMode(true);
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, pName);// C_Order_Post or C_Invoice_Post or
      System.out.println("check isOrderPost" + isOrderPost + " id " + id + " pName" + pName); // M_Inout_Post
      if (!isOrderPost) {
        List<Object> param = new ArrayList<Object>();
        ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
        pInstance.setActive(true);
        pInstance.setClient(gm.getClient());
        pInstance.setOrganization(gm.getOrganization());
        pInstance.setCreationDate(new Date());
        pInstance.setUpdated(new Date());
        pInstance.setCreatedBy(gm.getCreatedBy());
        pInstance.setUpdatedBy(gm.getUpdatedBy());
        pInstance.setUserContact(gm.getCreatedBy());
        pInstance.setProcess(process);
        pInstance.setRecordID(id);
        OBDal.getInstance().save(pInstance);
        // add param
        param.add(pInstance.getId());
        System.out.println("paramadd " + pInstance.getId());
        CallStoredProcedure.getInstance().call(process.getProcedure(), param, null, true, false);
        System.out.println("param id after ");
      }
    } catch (Exception exp) {
      log.error("Error ocurred while posting the process in 333 callProcessToPost(): ", exp);
      log.error("444Process ID , Process Name: "
          + pName
          + " , "
          + (pName == null ? "" : (pName.equals("104") ? " c_order_post()"
              : (pName.equals("122") ? " m_movement_post()" : " c_invoice_post()"))));
      throw new OBException("Error ocurred while posting ttthe process: " + exp);
    } finally {
      System.out.println("aaa isOrderPost" + isOrderPost);
      OBContext.restorePreviousMode();
      System.out.println("bbb isOrderPost" + isOrderPost);
    }
    final Map<String, Object> parameters = new HashMap<String, Object>();
    if (isOrderPost) {
      parameters.put(C_ORDER_ID, id);
      System.out.println("corderid " + C_ORDER_ID + "  id " + id + " process " + process);
      final ProcessInstance pInstanceId = CallProcess.getInstance().callProcess(process, id,
          parameters);
      System.out.println("ccc pInstanceId" + pInstanceId);
      System.out.println("dddd getResult" + pInstanceId.getResult());
      return pInstanceId;

    }
    return null;
  }

  /**
   * Method to execute payments
   */
  private void executePayments() {
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

  /**
   * Method to get pending payments
   */
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

  /**
   * Method to get leave as credit process
   */
  private List<PaymentExecutionProcess> getLeaveAsCreditProcesses() {
    OBCriteria<PaymentExecutionProcess> payExecProcCrit = OBDal.getInstance().createCriteria(
        PaymentExecutionProcess.class);
    payExecProcCrit.add(Restrictions.eq(PaymentExecutionProcess.PROPERTY_JAVACLASSNAME,
        "org.openbravo.advpaymentmngt.executionprocess.LeaveAsCredit"));

    return payExecProcCrit.list();
  }
}
