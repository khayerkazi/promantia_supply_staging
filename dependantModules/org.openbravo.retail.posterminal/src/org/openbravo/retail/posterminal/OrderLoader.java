/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.posterminal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.mobile.core.process.DataSynchronizationProcess.DataSynchronization;
import org.openbravo.mobile.core.process.JSONPropertyToEntity;
import org.openbravo.mobile.core.process.PropertyByType;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.ad.access.OrderLineTax;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceLineOffer;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderLineOffer;
import org.openbravo.model.common.order.OrderTax;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_OrigPaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.Fin_OrigPaymentSchedule;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;

@DataSynchronization(entity = "Order")
public class OrderLoader extends POSDataSynchronizationProcess {

  HashMap<String, DocumentType> paymentDocTypes = new HashMap<String, DocumentType>();
  HashMap<String, DocumentType> invoiceDocTypes = new HashMap<String, DocumentType>();
  HashMap<String, DocumentType> shipmentDocTypes = new HashMap<String, DocumentType>();
  String paymentDescription = null;
  boolean isLayaway = false;
  boolean partialpayLayaway = false;
  boolean fullpayLayaway = false;
  boolean createShipment = true;
  Locator binForRetuns = null;
  private static final Logger log = Logger.getLogger(OrderLoader.class);

  private static final BigDecimal NEGATIVE_ONE = new BigDecimal(-1);

  @Inject
  @Any
  private Instance<OrderLoaderHook> orderProcesses;

  @Inject
  @Any
  private Instance<OrderLoaderPreProcessHook> orderPreProcesses;

  @Override
  public JSONObject saveRecord(JSONObject jsonorder) throws Exception {

    executeHooks(orderPreProcesses, jsonorder, null, null, null);
    boolean wasPaidOnCredit = false;
    boolean isQuotation = jsonorder.has("isQuotation") && jsonorder.getBoolean("isQuotation");
    if (jsonorder.getLong("orderType") != 2 && !jsonorder.getBoolean("isLayaway") && !isQuotation
        && verifyOrderExistance(jsonorder)
        && (!jsonorder.has("preserveId") || jsonorder.getBoolean("preserveId"))) {
      return successMessage(jsonorder);
    }
    long t0 = System.currentTimeMillis();
    long t1, t11, t2, t3;
    Order order = null;
    OrderLine orderLine = null;
    ShipmentInOut shipment = null;
    Invoice invoice = null;
    boolean sendEmail, createInvoice = false;
    TriggerHandler.getInstance().disable();
    isLayaway = jsonorder.has("orderType") && jsonorder.getLong("orderType") == 2
        && jsonorder.getDouble("payment") < jsonorder.getDouble("gross");
    partialpayLayaway = jsonorder.getBoolean("isLayaway")
        && jsonorder.getDouble("payment") < jsonorder.getDouble("gross");
    fullpayLayaway = jsonorder.getBoolean("isLayaway")
        && jsonorder.getDouble("payment") >= jsonorder.getDouble("gross");
    try {
      if (jsonorder.has("oldId") && !jsonorder.getString("oldId").equals("null")
          && jsonorder.has("isQuotation") && jsonorder.getBoolean("isQuotation")) {
        deleteOldDocument(jsonorder);
      }

      t1 = System.currentTimeMillis();
      // An invoice will be automatically created if:
      // - The order is not a layaway and is not completely paid (ie. it's paid on credit)
      // - Or, the order is a normal order or a fully paid layaway, and has the "generateInvoice"
      // flag
      wasPaidOnCredit = !isLayaway
          && !partialpayLayaway
          && !fullpayLayaway
          && !isQuotation
          && Math.abs(jsonorder.getDouble("payment")) < Math.abs(new Double(jsonorder
              .getDouble("gross")));
      createInvoice = wasPaidOnCredit
          || (!isQuotation && (!isLayaway && !partialpayLayaway || fullpayLayaway) && (jsonorder
              .has("generateInvoice") && jsonorder.getBoolean("generateInvoice")));
      createShipment = !isQuotation && (!isLayaway && !partialpayLayaway || fullpayLayaway);
      if (jsonorder.has("generateShipment")) {
        createShipment &= jsonorder.getBoolean("generateShipment");
        createInvoice &= jsonorder.getBoolean("generateShipment");
      }
      sendEmail = (jsonorder.has("sendEmail") && jsonorder.getBoolean("sendEmail"));
      // Order header
      long t111 = System.currentTimeMillis();
      ArrayList<OrderLine> lineReferences = new ArrayList<OrderLine>();
      JSONArray orderlines = jsonorder.getJSONArray("lines");
      if (fullpayLayaway) {
        order = OBDal.getInstance().get(Order.class, jsonorder.getString("id"));
        order.setObposAppCashup(jsonorder.getString("obposAppCashup"));
        order.setDelivered(true);
        for (int i = 0; i < order.getOrderLineList().size(); i++) {
          lineReferences.add(order.getOrderLineList().get(i));
          orderLine = order.getOrderLineList().get(i);
          orderLine.setDeliveredQuantity(orderLine.getOrderedQuantity());
        }
      } else if (partialpayLayaway) {
        order = OBDal.getInstance().get(Order.class, jsonorder.getString("id"));
        order.setObposAppCashup(jsonorder.getString("obposAppCashup"));
      } else {
        order = OBProvider.getInstance().get(Order.class);
        createOrder(order, jsonorder);
        OBDal.getInstance().save(order);
        lineReferences = new ArrayList<OrderLine>();
        createOrderLines(order, jsonorder, orderlines, lineReferences);
      }
      long t112 = System.currentTimeMillis();
      // Order lines

      if (jsonorder.has("oldId") && !jsonorder.getString("oldId").equals("null")
          && (!jsonorder.has("isQuotation") || !jsonorder.getBoolean("isQuotation"))) {
        // This order comes from a quotation, we need to associate both
        associateOrderToQuotation(jsonorder, order);
      }

      long t113 = System.currentTimeMillis();
      if (createShipment) {
        // Shipment header
        shipment = OBProvider.getInstance().get(ShipmentInOut.class);
        createShipment(shipment, order, jsonorder);
        if (shipment != null) {
          OBDal.getInstance().save(shipment);
        }
        // Shipment lines
        createShipmentLines(shipment, order, jsonorder, orderlines, lineReferences);

      }
      long t115 = System.currentTimeMillis();
      if (createInvoice) {
        // Invoice header
        invoice = OBProvider.getInstance().get(Invoice.class);
        createInvoice(invoice, order, jsonorder);
        OBDal.getInstance().save(invoice);

        // Invoice lines
        createInvoiceLines(invoice, order, jsonorder, orderlines, lineReferences);
      }

      long t116 = System.currentTimeMillis();
      createApprovals(order, jsonorder);

      t11 = System.currentTimeMillis();

      t2 = System.currentTimeMillis();
      updateAuditInfo(order, invoice, jsonorder);
      t3 = System.currentTimeMillis();

      if (!isQuotation) {
        if (!isLayaway && !partialpayLayaway && createShipment) {
          // Stock manipulation
          handleStock(shipment);
          // Send email
        }
      }

      long t4 = System.currentTimeMillis();

      log.debug("Creation of bobs. Order: " + (t112 - t111) + "; Orderlines: " + (t113 - t112)
          + "; Shipment: " + (t115 - t113) + "; Invoice: " + (t116 - t115) + "; Approvals"
          + (t11 - t116) + "; stock" + (t4 - t3));

    } finally {
      TriggerHandler.getInstance().enable();
    }

    long t4 = System.currentTimeMillis();

    if (!isQuotation) {
      // Payment
      JSONObject paymentResponse = handlePayments(jsonorder, order, invoice, wasPaidOnCredit);
      if (paymentResponse != null) {
        return paymentResponse;
      }
      if (sendEmail) {
        EmailSender emailSender = new EmailSender(order.getId(), jsonorder);
      }

      if (createInvoice && isMultipleShipmentLine(invoice)) {
        finishInvoice(invoice);
      }

      // Call all OrderProcess injected.
      executeHooks(orderProcesses, jsonorder, order, shipment, invoice);

    }
    long t5 = System.currentTimeMillis();
    OBDal.getInstance().flush();
    log.info("Order with docno: " + order.getDocumentNo() + " (uuid: " + order.getId()
        + ") saved correctly. Initial flush: " + (t1 - t0) + "; Generate bobs:" + (t11 - t1)
        + "; Save bobs:" + (t2 - t11) + "; First flush:" + (t3 - t2) + "; Second flush: "
        + (t4 - t3) + "; Process Payments:" + (t5 - t4) + " Final flush: "
        + (System.currentTimeMillis() - t5));

    return successMessage(jsonorder);
  }

  protected boolean additionalCheckForDuplicates(JSONObject record) {
    try {
      Order orderInDatabase = OBDal.getInstance().get(Order.class, record.getString("id"));
      String docNoInDatabase = orderInDatabase.getDocumentNo();
      String docNoInJSON = "";
      docNoInJSON = record.getString("documentNo");
      if (!docNoInDatabase.equals(docNoInJSON)) {
        return false;
      } else {
        return true;
      }
    } catch (JSONException e) {
      return true;
    }
  }

  private void executeHooks(Instance<? extends Object> hooks, JSONObject jsonorder, Order order,
      ShipmentInOut shipment, Invoice invoice) throws Exception {
    for (Iterator<? extends Object> procIter = hooks.iterator(); procIter.hasNext();) {
      Object proc = procIter.next();
      if (proc instanceof OrderLoaderHook) {
        ((OrderLoaderHook) proc).exec(jsonorder, order, shipment, invoice);
      } else {
        ((OrderLoaderPreProcessHook) proc).exec(jsonorder);
      }
    }
  }

  private void updateAuditInfo(Order order, Invoice invoice, JSONObject jsonorder)
      throws JSONException {
    Long value = jsonorder.getLong("created");
    order.set("creationDate", new Date(value));
    if (invoice != null) {
      invoice.set("creationDate", new Date(value));
    }
  }

  protected void associateOrderToQuotation(JSONObject jsonorder, Order order) throws JSONException {
    String quotationId = jsonorder.getString("oldId");
    Order quotation = OBDal.getInstance().get(Order.class, quotationId);
    order.setQuotation(quotation);
    List<OrderLine> orderLines = order.getOrderLineList();
    List<OrderLine> quotationLines = quotation.getOrderLineList();
    for (int i = 0; (i < orderLines.size() && i < quotationLines.size()); i++) {
      orderLines.get(i).setQuotationLine(quotationLines.get(i));
    }
    quotation.setDocumentStatus("CA");

  }

  protected Locator getBinForReturns(String posTerminalId) {
    if (binForRetuns == null) {
      OBPOSApplications posTerminal = OBDal.getInstance().get(OBPOSApplications.class,
          posTerminalId);
      binForRetuns = POSUtils.getBinForReturns(posTerminal);
    }
    return binForRetuns;
  }

  protected JSONObject successMessage(JSONObject jsonorder) throws Exception {
    final JSONObject jsonResponse = new JSONObject();

    jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    jsonResponse.put("result", "0");
    return jsonResponse;
  }

  protected void deleteOldDocument(JSONObject jsonorder) throws JSONException {
    Order oldOrder = OBDal.getInstance().get(Order.class, jsonorder.getString("oldId"));
    OBDal.getInstance().remove(oldOrder);
    OBDal.getInstance().flush();
  }

  protected boolean verifyOrderExistance(JSONObject jsonorder) throws Exception {
    OBContext.setAdminMode(false);
    try {
      if (jsonorder.has("id") && jsonorder.getString("id") != null
          && !jsonorder.getString("id").equals("")) {
        Order order = OBDal.getInstance().get(Order.class, jsonorder.getString("id"));
        if (order != null) {
          // Additional check to verify that the order is indeed a duplicate
          if (!additionalCheckForDuplicates(jsonorder)) {
            throw new OBException(
                "An order has the same id, but it's not a duplicate. Existing order id:"
                    + order.getId() + ". Existing order documentNo:" + order.getDocumentNo()
                    + ". New documentNo:" + jsonorder.getString("documentNo"));
          } else {
            return true;
          }
        }
      }
      if ((!jsonorder.has("gross") || jsonorder.getString("gross").equals("0"))
          && (jsonorder.isNull("lines") || (jsonorder.getJSONArray("lines") != null && jsonorder
              .getJSONArray("lines").length() == 0))) {
        log.error("Detected order without lines and total amount zero. Document number "
            + jsonorder.getString("documentNo"));
        return true;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  protected String getPaymentDescription() {
    if (paymentDescription == null) {
      String language = RequestContext.get().getVariablesSecureApp().getLanguage();
      paymentDescription = Utility.messageBD(new DalConnectionProvider(false), "OrderDocumentno",
          language);
    }
    return paymentDescription;
  }

  protected DocumentType getPaymentDocumentType(Organization org) {
    if (paymentDocTypes.get(DalUtil.getId(org)) != null) {
      return paymentDocTypes.get(DalUtil.getId(org));
    }
    final DocumentType docType = FIN_Utility.getDocumentType(org, AcctServer.DOCTYPE_ARReceipt);
    paymentDocTypes.put((String) DalUtil.getId(org), docType);
    return docType;

  }

  protected DocumentType getInvoiceDocumentType(String documentTypeId) {
    if (invoiceDocTypes.get(documentTypeId) != null) {
      return invoiceDocTypes.get(documentTypeId);
    }
    DocumentType orderDocType = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    final DocumentType docType = orderDocType.getDocumentTypeForInvoice();
    invoiceDocTypes.put(documentTypeId, docType);
    if (docType == null) {
      throw new OBException(
          "There is no 'Document type for Invoice' defined for the specified Document Type. The document type for invoices can be configured in the Document Type window, and it should be configured for the document type: "
              + orderDocType.getName());
    }
    return docType;
  }

  protected DocumentType getShipmentDocumentType(String documentTypeId) {
    if (shipmentDocTypes.get(documentTypeId) != null) {
      return shipmentDocTypes.get(documentTypeId);
    }
    DocumentType orderDocType = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    final DocumentType docType = orderDocType.getDocumentTypeForShipment();
    shipmentDocTypes.put(documentTypeId, docType);
    if (docType == null) {
      throw new OBException(
          "There is no 'Document type for Shipment' defined for the specified Document Type. The document type for shipments can be configured in the Document Type window, and it should be configured for the document type: "
              + orderDocType.getName());
    }
    return docType;
  }

  protected void createInvoiceLine(Invoice invoice, Order order, JSONObject jsonorder,
      JSONArray orderlines, ArrayList<OrderLine> lineReferences, int numIter, int stdPrecision,
      ShipmentInOutLine inOutLine, int lineNo) throws JSONException {

    BigDecimal movQty = null;
    if (inOutLine != null && inOutLine.getMovementQuantity() != null) {
      movQty = inOutLine.getMovementQuantity();
    } else {
      movQty = lineReferences.get(numIter).getOrderedQuantity();
    }

    BigDecimal ratio = movQty.divide(lineReferences.get(numIter).getOrderedQuantity(), 32,
        RoundingMode.HALF_UP);

    Entity promotionLineEntity = ModelProvider.getInstance().getEntity(OrderLineOffer.class);
    InvoiceLine line = OBProvider.getInstance().get(InvoiceLine.class);
    Entity inlineEntity = ModelProvider.getInstance().getEntity(InvoiceLine.class);
    JSONPropertyToEntity.fillBobFromJSON(inlineEntity, line, orderlines.getJSONObject(numIter),
        jsonorder.getLong("timezoneOffset"));
    JSONPropertyToEntity.fillBobFromJSON(ModelProvider.getInstance().getEntity(InvoiceLine.class),
        line, jsonorder, jsonorder.getLong("timezoneOffset"));
    line.setLineNo((long) lineNo);
    line.setDescription(orderlines.getJSONObject(numIter).has("description") ? orderlines
        .getJSONObject(numIter).getString("description") : "");
    BigDecimal qty = movQty;

    // if ratio equals to one, then only one shipment line is related to orderline, then lineNetAmt
    // and gross is populated from JSON
    if (ratio.compareTo(BigDecimal.ONE) != 0) {
      line.setLineNetAmount(lineReferences.get(numIter).getUnitPrice().multiply(qty)
          .setScale(stdPrecision, RoundingMode.HALF_UP));
      line.setGrossAmount(lineReferences.get(numIter).getGrossUnitPrice().multiply(qty)
          .setScale(stdPrecision, RoundingMode.HALF_UP));
    } else {
      line.setLineNetAmount(BigDecimal.valueOf(orderlines.getJSONObject(numIter).getDouble("net"))
          .setScale(stdPrecision, RoundingMode.HALF_UP));
      line.setGrossAmount(lineReferences.get(numIter).getLineGrossAmount()
          .setScale(stdPrecision, RoundingMode.HALF_UP));
    }

    line.setInvoicedQuantity(qty);
    lineReferences.get(numIter).setInvoicedQuantity(
        (lineReferences.get(numIter).getInvoicedQuantity() != null ? lineReferences.get(numIter)
            .getInvoicedQuantity().add(qty) : qty));
    line.setInvoice(invoice);
    line.setSalesOrderLine(lineReferences.get(numIter));
    line.setGoodsShipmentLine(inOutLine);
    invoice.getInvoiceLineList().add(line);
    OBDal.getInstance().save(line);

    JSONObject taxes = orderlines.getJSONObject(numIter).getJSONObject("taxLines");
    @SuppressWarnings("unchecked")
    Iterator<String> itKeys = taxes.keys();
    BigDecimal totalTaxAmount = BigDecimal.ZERO;
    int ind = 0;
    while (itKeys.hasNext()) {
      String taxId = itKeys.next();
      JSONObject jsonOrderTax = taxes.getJSONObject(taxId);
      InvoiceLineTax invoicelinetax = OBProvider.getInstance().get(InvoiceLineTax.class);
      TaxRate tax = (TaxRate) OBDal.getInstance().getProxy(
          ModelProvider.getInstance().getEntity(TaxRate.class).getName(), taxId);
      invoicelinetax.setTax(tax);

      // if ratio equals to one, then only one shipment line is related to orderline, then
      // lineNetAmt and gross is populated from JSON
      if (ratio.compareTo(BigDecimal.ONE) != 0) {
        invoicelinetax.setTaxableAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("net"))
            .multiply(ratio).setScale(stdPrecision, RoundingMode.HALF_UP));
        invoicelinetax.setTaxAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("amount"))
            .multiply(ratio).setScale(stdPrecision, RoundingMode.HALF_UP));
        totalTaxAmount = totalTaxAmount.add(invoicelinetax.getTaxAmount());
      } else {
        invoicelinetax.setTaxableAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("net")).setScale(
            stdPrecision, RoundingMode.HALF_UP));
        invoicelinetax.setTaxAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("amount")).setScale(
            stdPrecision, RoundingMode.HALF_UP));
      }
      invoicelinetax.setInvoice(invoice);
      invoicelinetax.setInvoiceLine(line);
      invoicelinetax.setRecalculate(true);
      invoicelinetax.setLineNo((long) ((ind + 1) * 10));
      ind++;
      invoice.getInvoiceLineTaxList().add(invoicelinetax);
      line.getInvoiceLineTaxList().add(invoicelinetax);
      OBDal.getInstance().save(invoicelinetax);
    }

    // Discounts & Promotions
    if (orderlines.getJSONObject(numIter).has("promotions")
        && !orderlines.getJSONObject(numIter).isNull("promotions")
        && !orderlines.getJSONObject(numIter).getString("promotions").equals("null")) {
      JSONArray jsonPromotions = orderlines.getJSONObject(numIter).getJSONArray("promotions");
      for (int p = 0; p < jsonPromotions.length(); p++) {
        JSONObject jsonPromotion = jsonPromotions.getJSONObject(p);
        boolean hasActualAmt = jsonPromotion.has("actualAmt");
        if (hasActualAmt && jsonPromotion.getDouble("actualAmt") == 0) {
          continue;
        }

        InvoiceLineOffer promotion = OBProvider.getInstance().get(InvoiceLineOffer.class);
        JSONPropertyToEntity.fillBobFromJSON(promotionLineEntity, promotion, jsonPromotion,
            jsonorder.getLong("timezoneOffset"));

        if (hasActualAmt) {
          promotion.setTotalAmount(BigDecimal.valueOf(jsonPromotion.getDouble("actualAmt"))
              .multiply(ratio).setScale(stdPrecision, RoundingMode.HALF_UP));
        } else {
          promotion.setTotalAmount(BigDecimal.valueOf(jsonPromotion.getDouble("amt"))
              .multiply(ratio).setScale(stdPrecision, RoundingMode.HALF_UP));
        }
        promotion.setLineNo((long) ((p + 1) * 10));
        promotion.setInvoiceLine(line);
        line.getInvoiceLineOfferList().add(promotion);
      }
    }

  }

  protected void createInvoiceLines(Invoice invoice, Order order, JSONObject jsonorder,
      JSONArray orderlines, ArrayList<OrderLine> lineReferences) throws JSONException {
    int stdPrecision = order.getCurrency().getStandardPrecision().intValue();

    boolean multipleShipmentsLines = false;
    int lineNo = 0;
    for (int i = 0; i < orderlines.length(); i++) {
      List<ShipmentInOutLine> iolList = lineReferences.get(i)
          .getMaterialMgmtShipmentInOutLineList();
      if (iolList.size() > 1) {
        multipleShipmentsLines = true;
      }
      if (iolList.size() == 0) {
        lineNo = lineNo + 10;
        createInvoiceLine(invoice, order, jsonorder, orderlines, lineReferences, i, stdPrecision,
            null, lineNo);
      } else {
        for (ShipmentInOutLine iol : iolList) {
          lineNo = lineNo + 10;
          createInvoiceLine(invoice, order, jsonorder, orderlines, lineReferences, i, stdPrecision,
              iol, lineNo);
        }
      }
    }
    if (multipleShipmentsLines) {
      updateTaxes(invoice);
    }
  }

  protected void createInvoice(Invoice invoice, Order order, JSONObject jsonorder)
      throws JSONException {
    Entity invoiceEntity = ModelProvider.getInstance().getEntity(Invoice.class);
    JSONPropertyToEntity.fillBobFromJSON(invoiceEntity, invoice, jsonorder,
        jsonorder.getLong("timezoneOffset"));

    int stdPrecision = order.getCurrency().getStandardPrecision().intValue();

    String description;
    if (jsonorder.has("Invoice.description")) {
      // in case the description is directly set to Invoice entity, preserve it
      description = jsonorder.getString("Invoice.description");
    } else {
      // other case use generic description if present and add relationship to order
      if (jsonorder.has("description") && !jsonorder.getString("description").equals("")) {
        description = jsonorder.getString("description") + "\n";
      } else {
        description = "";
      }
      description += OBMessageUtils.getI18NMessage("OBPOS_InvoiceRelatedToOrder", null)
          + jsonorder.getString("documentNo");
    }

    invoice.setDescription(description);
    invoice
        .setDocumentType(getInvoiceDocumentType((String) DalUtil.getId(order.getDocumentType())));
    invoice.setTransactionDocument(getInvoiceDocumentType((String) DalUtil.getId(order
        .getDocumentType())));
    invoice.setDocumentNo(getDocumentNo(invoiceEntity, invoice.getTransactionDocument(),
        invoice.getDocumentType()));
    invoice.setAccountingDate(order.getOrderDate());
    invoice.setInvoiceDate(order.getOrderDate());
    invoice.setSalesTransaction(true);
    invoice.setDocumentStatus("CO");
    invoice.setDocumentAction("RE");
    invoice.setAPRMProcessinvoice("RE");
    invoice.setSalesOrder(order);
    invoice.setPartnerAddress(OBDal.getInstance().get(Location.class,
        jsonorder.getJSONObject("bp").getString("locId")));
    invoice.setProcessed(true);
    invoice.setPaymentMethod((FIN_PaymentMethod) OBDal.getInstance().getProxy("FIN_PaymentMethod",
        jsonorder.getJSONObject("bp").getString("paymentMethod")));
    invoice.setPaymentTerms((PaymentTerm) OBDal.getInstance().getProxy("FinancialMgmtPaymentTerm",
        jsonorder.getJSONObject("bp").getString("paymentTerms")));
    invoice.setGrandTotalAmount(BigDecimal.valueOf(jsonorder.getDouble("gross")).setScale(
        stdPrecision, RoundingMode.HALF_UP));
    invoice.setSummedLineAmount(BigDecimal.valueOf(jsonorder.getDouble("net")).setScale(
        stdPrecision, RoundingMode.HALF_UP));
    invoice.setTotalPaid(BigDecimal.ZERO);
    invoice.setOutstandingAmount((BigDecimal.valueOf(jsonorder.getDouble("gross"))).setScale(
        stdPrecision, RoundingMode.HALF_UP));
    invoice.setDueAmount((BigDecimal.valueOf(jsonorder.getDouble("gross"))).setScale(stdPrecision,
        RoundingMode.HALF_UP));

    // Create invoice tax lines
    JSONObject taxes = jsonorder.getJSONObject("taxes");
    @SuppressWarnings("unchecked")
    Iterator<String> itKeys = taxes.keys();
    int i = 0;
    while (itKeys.hasNext()) {
      String taxId = itKeys.next();
      JSONObject jsonOrderTax = taxes.getJSONObject(taxId);
      InvoiceTax invoiceTax = OBProvider.getInstance().get(InvoiceTax.class);
      TaxRate tax = (TaxRate) OBDal.getInstance().getProxy(
          ModelProvider.getInstance().getEntity(TaxRate.class).getName(), taxId);
      invoiceTax.setTax(tax);
      invoiceTax.setTaxableAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("net")).setScale(
          stdPrecision, RoundingMode.HALF_UP));
      invoiceTax.setTaxAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("amount")).setScale(
          stdPrecision, RoundingMode.HALF_UP));
      invoiceTax.setInvoice(invoice);
      invoiceTax.setLineNo((long) ((i + 1) * 10));
      invoiceTax.setRecalculate(true);
      i++;
      invoice.getInvoiceTaxList().add(invoiceTax);
    }

    // Update customer credit
    BigDecimal total = invoice.getGrandTotalAmount().setScale(stdPrecision, RoundingMode.HALF_UP);

    if (!invoice.getCurrency().equals(invoice.getBusinessPartner().getPriceList().getCurrency())) {
      total = convertCurrencyInvoice(invoice);
    }
    OBContext.setAdminMode(false);
    try {
      // Same currency, no conversion required
      invoice.getBusinessPartner().setCreditUsed(
          invoice.getBusinessPartner().getCreditUsed().add(total));
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  protected void updateTaxes(Invoice invoice) throws JSONException {
    int stdPrecision = invoice.getCurrency().getStandardPrecision().intValue();
    for (InvoiceTax taxInv : invoice.getInvoiceTaxList()) {
      BigDecimal taxAmt = BigDecimal.ZERO;
      BigDecimal taxableAmt = BigDecimal.ZERO;
      for (InvoiceLineTax taxLine : invoice.getInvoiceLineTaxList()) {
        if (taxLine.getTax() == taxInv.getTax()) {
          taxAmt = taxAmt.add(taxLine.getTaxAmount());
          taxableAmt = taxableAmt.add(taxLine.getTaxableAmount());
        }
      }
      taxInv.setTaxableAmount(taxableAmt.setScale(stdPrecision, RoundingMode.HALF_UP));
      taxInv.setTaxAmount(taxAmt.setScale(stdPrecision, RoundingMode.HALF_UP));
      OBDal.getInstance().save(taxInv);
    }
  }

  protected void finishInvoice(Invoice invoice) throws JSONException {
    int stdPrecision = invoice.getCurrency().getStandardPrecision().intValue();
    BigDecimal grossAmount = BigDecimal.ZERO;
    BigDecimal netAmount = BigDecimal.ZERO;
    BigDecimal totalPaid = BigDecimal.ZERO;

    for (InvoiceLine il : invoice.getInvoiceLineList()) {
      netAmount = netAmount.add(il.getLineNetAmount());
    }

    grossAmount = netAmount;
    for (InvoiceLineTax ilt : invoice.getInvoiceLineTaxList()) {
      grossAmount = grossAmount.add(ilt.getTaxAmount());
    }

    for (FIN_PaymentSchedule ps : invoice.getFINPaymentScheduleList()) {
      totalPaid = totalPaid.add(ps.getPaidAmount());
    }

    // if the total paid is distinct that grossamount, we should recalculate grandtotal and
    // lineNetAmt and create a new sched detail with the difference
    if (grossAmount.compareTo(totalPaid) != 0) {
      // update invoice header data
      invoice.setGrandTotalAmount(grossAmount.setScale(stdPrecision, RoundingMode.HALF_UP));
      invoice.setSummedLineAmount(netAmount.setScale(stdPrecision, RoundingMode.HALF_UP));
      invoice.setTotalPaid(totalPaid.setScale(stdPrecision, RoundingMode.HALF_UP));
      invoice.setOutstandingAmount(grossAmount.subtract(totalPaid));
      invoice.setPaymentComplete(grossAmount.compareTo(totalPaid) == 0);
      invoice.setFinalSettlementDate(grossAmount.compareTo(totalPaid) == 0 ? invoice
          .getFinalSettlementDate() : null);

      // update invoice payment data
      FIN_PaymentScheduleDetail newDetail = OBProvider.getInstance().get(
          FIN_PaymentScheduleDetail.class);
      newDetail.setAmount(grossAmount.subtract(totalPaid));
      if (invoice.getFINPaymentScheduleList().size() > 0) {
        newDetail.setInvoicePaymentSchedule(invoice.getFINPaymentScheduleList().get(0));
        invoice.getFINPaymentScheduleList().get(0)
            .getFINPaymentScheduleDetailInvoicePaymentScheduleList().add(newDetail);
        invoice.getFINPaymentScheduleList().get(0).setAmount(grossAmount);
        invoice.getFINPaymentScheduleList().get(0)
            .setOutstandingAmount(grossAmount.subtract(totalPaid));
      }
      OBDal.getInstance().save(newDetail);
      OBDal.getInstance().save(invoice.getFINPaymentScheduleList().get(0));
      OBDal.getInstance().save(invoice);
      // OBDal.getInstance().flush();
    }
  }

  protected boolean isMultipleShipmentLine(Invoice invoice) {
    OrderLine ol = null;
    for (InvoiceLine il : invoice.getInvoiceLineList()) {
      if (ol != null && ol.equals(il.getSalesOrderLine())) {
        return true;
      }
      ol = il.getSalesOrderLine();
    }
    return false;
  }

  public static BigDecimal convertCurrencyInvoice(Invoice invoice) {
    int stdPrecision = invoice.getCurrency().getStandardPrecision().intValue();
    List<Object> parameters = new ArrayList<Object>();
    List<Class<?>> types = new ArrayList<Class<?>>();
    parameters.add(invoice.getGrandTotalAmount().setScale(stdPrecision, RoundingMode.HALF_UP));
    types.add(BigDecimal.class);
    parameters.add(invoice.getCurrency());
    types.add(BaseOBObject.class);
    parameters.add(invoice.getBusinessPartner().getPriceList().getCurrency());
    types.add(BaseOBObject.class);
    parameters.add(invoice.getInvoiceDate());
    types.add(Timestamp.class);
    parameters.add("S");
    types.add(String.class);
    parameters.add(OBContext.getOBContext().getCurrentClient());
    types.add(BaseOBObject.class);
    parameters.add(OBContext.getOBContext().getCurrentOrganization());
    types.add(BaseOBObject.class);
    parameters.add('A');
    types.add(Character.class);

    return (BigDecimal) CallStoredProcedure.getInstance().call("c_currency_convert_precision",
        parameters, types);
  }

  protected void createShipmentLines(ShipmentInOut shipment, Order order, JSONObject jsonorder,
      JSONArray orderlines, ArrayList<OrderLine> lineReferences) throws JSONException {
    int lineNo = 0;
    Entity shplineentity = ModelProvider.getInstance().getEntity(ShipmentInOutLine.class);
    for (int i = 0; i < orderlines.length(); i++) {
      String hqlWhereClause;

      OrderLine orderLine = lineReferences.get(i);
      BigDecimal pendingQty = orderLine.getOrderedQuantity().abs();
      boolean negativeLine = orderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) < 0;

      AttributeSetInstance oldAttributeSetValues = null;
      if (negativeLine) {
        lineNo += 10;
        addShipemntline(shipment, shplineentity, orderlines.getJSONObject(i), orderLine, jsonorder,
            lineNo, pendingQty.negate(), getBinForReturns(jsonorder.getString("posTerminal")), null);

      } else {
        HashMap<String, ShipmentInOutLine> usedBins = new HashMap<String, ShipmentInOutLine>();
        if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
          // The M_GetStock function is used
          Process process = OBDal.getInstance().get(Process.class,
              "FF80818132C964E30132C9747257002E");
          Map<String, Object> parameters = new HashMap<String, Object>();
          parameters.put("AD_Client_ID", OBContext.getOBContext().getCurrentClient().getId());
          parameters.put("AD_Org_ID", OBContext.getOBContext().getCurrentOrganization().getId());
          parameters.put(
              "M_Product_ID",
              orderLine.getProduct() == null ? null
                  : (String) DalUtil.getId(orderLine.getProduct()));
          parameters.put("C_Uom_ID",
              orderLine.getUOM() == null ? null : (String) DalUtil.getId(orderLine.getUOM()));
          parameters.put("M_Product_Uom_ID", orderLine.getOrderUOM() == null ? null
              : (String) DalUtil.getId(orderLine.getOrderUOM()));
          parameters.put(
              "M_AttributesetInstance_ID",
              orderLine.getAttributeSetValue() == null ? null : (String) DalUtil.getId(orderLine
                  .getAttributeSetValue()));
          parameters.put("Quantity", pendingQty);
          parameters.put("ProcessID", "118");
          if (orderLine.getEntity().hasProperty("warehouseRule")
              && orderLine.get("warehouseRule") != null) {
            parameters.put("M_Warehouse_Rule_ID",
                (String) DalUtil.getId(orderLine.get("warehouseRule")));
          }
          if (orderLine.getEntity().hasProperty("warehouse") && orderLine.get("warehouse") != null) {
            parameters.put("Priority_Warehouse_ID",
                (String) DalUtil.getId(orderLine.get("warehouse")));
          }

          ProcessInstance pInstance = CallProcess.getInstance().callProcess(process, null,
              parameters, false);

          OBCriteria<StockProposed> stockProposed = OBDal.getInstance().createCriteria(
              StockProposed.class);
          stockProposed.add(Restrictions.eq(StockProposed.PROPERTY_PROCESSINSTANCE, pInstance));
          stockProposed.addOrderBy(StockProposed.PROPERTY_PRIORITY, true);

          ScrollableResults bins = stockProposed.scroll(ScrollMode.FORWARD_ONLY);

          boolean foundStockProposed = false;
          try {
            while (pendingQty.compareTo(BigDecimal.ZERO) > 0 && bins.next()) {
              foundStockProposed = true;
              // TODO: Can we safely clear session here?
              StockProposed stock = (StockProposed) bins.get(0);
              BigDecimal qty;

              Object stockQty = stock.get("quantity");
              if (stockQty instanceof Long) {
                stockQty = new BigDecimal((Long) stockQty);
              }
              if (pendingQty.compareTo((BigDecimal) stockQty) > 0) {
                qty = (BigDecimal) stockQty;
                pendingQty = pendingQty.subtract(qty);
              } else {
                qty = pendingQty;
                pendingQty = BigDecimal.ZERO;
              }
              lineNo += 10;
              if (negativeLine) {
                qty = qty.negate();
              }
              ShipmentInOutLine objShipmentLine = addShipemntline(shipment, shplineentity,
                  orderlines.getJSONObject(i), orderLine, jsonorder, lineNo, qty, stock
                      .getStorageDetail().getStorageBin(), stock.getStorageDetail()
                      .getAttributeSetValue());

              usedBins.put(stock.getStorageDetail().getStorageBin().getId(), objShipmentLine);

            }
          } finally {
            bins.close();
          }
          if (!foundStockProposed && orderLine.getProduct().getAttributeSet() != null) {
            // M_GetStock couldn't find any valid stock, and the product has an attribute set. We
            // will
            // attempt to find an old transaction for this product, and get the attribute values
            // from
            // there
            OBCriteria<ShipmentInOutLine> oldLines = OBDal.getInstance().createCriteria(
                ShipmentInOutLine.class);
            oldLines
                .add(Restrictions.eq(ShipmentInOutLine.PROPERTY_PRODUCT, orderLine.getProduct()));
            oldLines.setMaxResults(1);
            oldLines.addOrderBy(ShipmentInOutLine.PROPERTY_CREATIONDATE, false);
            List<ShipmentInOutLine> oldLine = oldLines.list();
            if (oldLine.size() > 0) {
              oldAttributeSetValues = oldLine.get(0).getAttributeSetValue();
            }

          }
        }

        if (pendingQty.compareTo(BigDecimal.ZERO) != 0) {
          // still qty to ship or return: let's use the bin with highest prio
          hqlWhereClause = " l where l.warehouse = :warehouse order by l.relativePriority, l.id";
          OBQuery<Locator> queryLoc = OBDal.getInstance()
              .createQuery(Locator.class, hqlWhereClause);
          queryLoc.setNamedParameter("warehouse", order.getWarehouse());
          queryLoc.setMaxResult(1);
          lineNo += 10;
          if (jsonorder.getLong("orderType") == 1) {
            pendingQty = pendingQty.negate();
          }
          ShipmentInOutLine objShipmentInOutLine = usedBins.get(queryLoc.list().get(0).getId());
          if (objShipmentInOutLine != null) {
            objShipmentInOutLine.setMovementQuantity(objShipmentInOutLine.getMovementQuantity()
                .add(pendingQty));
            OBDal.getInstance().save(objShipmentInOutLine);
          } else {
            addShipemntline(shipment, shplineentity, orderlines.getJSONObject(i), orderLine,
                jsonorder, lineNo, pendingQty, queryLoc.list().get(0), oldAttributeSetValues);
          }
        }
      }
    }
  }

  private ShipmentInOutLine addShipemntline(ShipmentInOut shipment, Entity shplineentity,
      JSONObject jsonOrderLine, OrderLine orderLine, JSONObject jsonorder, long lineNo,
      BigDecimal qty, Locator bin, AttributeSetInstance attributeSetInstance) throws JSONException {
    ShipmentInOutLine line = OBProvider.getInstance().get(ShipmentInOutLine.class);

    JSONPropertyToEntity.fillBobFromJSON(shplineentity, line, jsonOrderLine,
        jsonorder.getLong("timezoneOffset"));
    JSONPropertyToEntity.fillBobFromJSON(
        ModelProvider.getInstance().getEntity(ShipmentInOutLine.class), line, jsonorder,
        jsonorder.getLong("timezoneOffset"));
    line.setLineNo(lineNo);
    line.setShipmentReceipt(shipment);
    line.setSalesOrderLine(orderLine);

    orderLine.getMaterialMgmtShipmentInOutLineList().add(line);

    line.setMovementQuantity(qty);
    line.setStorageBin(bin);
    if (attributeSetInstance != null) {
      line.setAttributeSetValue(attributeSetInstance);
    }
    shipment.getMaterialMgmtShipmentInOutLineList().add(line);
    OBDal.getInstance().save(line);
    return line;
  }

  protected void createShipment(ShipmentInOut shipment, Order order, JSONObject jsonorder)
      throws JSONException {
    Entity shpEntity = ModelProvider.getInstance().getEntity(ShipmentInOut.class);
    JSONPropertyToEntity.fillBobFromJSON(shpEntity, shipment, jsonorder,
        jsonorder.getLong("timezoneOffset"));
    shipment.setDocumentNo(null);
    shipment
        .setDocumentType(getShipmentDocumentType((String) DalUtil.getId(order.getDocumentType())));
    shipment.setDocumentNo(getDocumentNo(shpEntity, null, shipment.getDocumentType()));
    shipment.setAccountingDate(order.getOrderDate());
    shipment.setMovementDate(order.getOrderDate());
    shipment.setPartnerAddress(OBDal.getInstance().get(Location.class,
        jsonorder.getJSONObject("bp").getString("locId")));
    shipment.setSalesTransaction(true);
    shipment.setDocumentStatus("CO");
    shipment.setDocumentAction("--");
    shipment.setMovementType("C-");
    shipment.setProcessNow(false);
    shipment.setProcessed(true);
    shipment.setSalesOrder(order);
    shipment.setProcessGoodsJava("--");

  }

  protected void createOrderLines(Order order, JSONObject jsonorder, JSONArray orderlines,
      ArrayList<OrderLine> lineReferences) throws JSONException {
    boolean isQuotation = false;
    try {
      isQuotation = jsonorder.has("isQuotation") && jsonorder.getBoolean("isQuotation");
    } catch (Exception ex) {
      isQuotation = false;
    }
    Entity orderLineEntity = ModelProvider.getInstance().getEntity(OrderLine.class);
    Entity promotionLineEntity = ModelProvider.getInstance().getEntity(OrderLineOffer.class);
    int stdPrecision = order.getCurrency().getStandardPrecision().intValue();

    for (int i = 0; i < orderlines.length(); i++) {

      OrderLine orderline = OBProvider.getInstance().get(OrderLine.class);
      JSONObject jsonOrderLine = orderlines.getJSONObject(i);

      JSONPropertyToEntity.fillBobFromJSON(ModelProvider.getInstance().getEntity(OrderLine.class),
          orderline, jsonorder, jsonorder.getLong("timezoneOffset"));
      JSONPropertyToEntity.fillBobFromJSON(orderLineEntity, orderline, jsonOrderLine,
          jsonorder.getLong("timezoneOffset"));

      orderline.setActive(true);
      orderline.setSalesOrder(order);
      orderline.setLineNetAmount(BigDecimal.valueOf(jsonOrderLine.getDouble("net")).setScale(
          stdPrecision, RoundingMode.HALF_UP));

      if (!isLayaway && !partialpayLayaway && (createShipment || isQuotation)) {
        // shipment is created, so all is delivered
        orderline.setDeliveredQuantity(orderline.getOrderedQuantity());
      }

      lineReferences.add(orderline);
      orderline.setLineNo((long) ((i + 1) * 10));
      order.getOrderLineList().add(orderline);
      OBDal.getInstance().save(orderline);

      JSONObject taxes = jsonOrderLine.getJSONObject("taxLines");
      @SuppressWarnings("unchecked")
      Iterator<String> itKeys = taxes.keys();
      int ind = 0;
      while (itKeys.hasNext()) {
        String taxId = itKeys.next();
        JSONObject jsonOrderTax = taxes.getJSONObject(taxId);
        OrderLineTax orderlinetax = OBProvider.getInstance().get(OrderLineTax.class);
        TaxRate tax = (TaxRate) OBDal.getInstance().getProxy(
            ModelProvider.getInstance().getEntity(TaxRate.class).getName(), taxId);
        orderlinetax.setTax(tax);
        orderlinetax.setTaxableAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("net")).setScale(
            stdPrecision, RoundingMode.HALF_UP));
        orderlinetax.setTaxAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("amount")).setScale(
            stdPrecision, RoundingMode.HALF_UP));
        orderlinetax.setSalesOrder(order);
        orderlinetax.setSalesOrderLine(orderline);
        orderlinetax.setLineNo((long) ((ind + 1) * 10));
        ind++;
        orderline.getOrderLineTaxList().add(orderlinetax);
        order.getOrderLineTaxList().add(orderlinetax);
        OBDal.getInstance().save(orderlinetax);
      }

      // Discounts & Promotions
      if (jsonOrderLine.has("promotions") && !jsonOrderLine.isNull("promotions")
          && !jsonOrderLine.getString("promotions").equals("null")) {
        JSONArray jsonPromotions = jsonOrderLine.getJSONArray("promotions");
        for (int p = 0; p < jsonPromotions.length(); p++) {
          JSONObject jsonPromotion = jsonPromotions.getJSONObject(p);
          boolean hasActualAmt = jsonPromotion.has("actualAmt");
          if ((hasActualAmt && jsonPromotion.getDouble("actualAmt") == 0)
              || (!hasActualAmt && jsonPromotion.getDouble("amt") == 0)) {
            continue;
          }

          OrderLineOffer promotion = OBProvider.getInstance().get(OrderLineOffer.class);
          JSONPropertyToEntity.fillBobFromJSON(promotionLineEntity, promotion, jsonPromotion,
              jsonorder.getLong("timezoneOffset"));

          if (hasActualAmt) {
            promotion.setTotalAmount(BigDecimal.valueOf(jsonPromotion.getDouble("actualAmt"))
                .setScale(stdPrecision, RoundingMode.HALF_UP));
          } else {
            promotion.setTotalAmount(BigDecimal.valueOf(jsonPromotion.getDouble("amt")).setScale(
                stdPrecision, RoundingMode.HALF_UP));
          }
          promotion.setLineNo((long) ((p + 1) * 10));
          promotion.setSalesOrderLine(orderline);
          orderline.getOrderLineOfferList().add(promotion);
        }
      }
    }
  }

  protected void createOrder(Order order, JSONObject jsonorder) throws JSONException {
    Entity orderEntity = ModelProvider.getInstance().getEntity(Order.class);
    JSONPropertyToEntity.fillBobFromJSON(orderEntity, order, jsonorder,
        jsonorder.getLong("timezoneOffset"));
    order.setNewOBObject(true);
    if (!jsonorder.has("preserveId") || jsonorder.getBoolean("preserveId")) {
      order.setId(jsonorder.getString("id"));
    }
    int stdPrecision = order.getCurrency().getStandardPrecision().intValue();
    BusinessPartner bp = order.getBusinessPartner();
    order.setTransactionDocument((DocumentType) OBDal.getInstance().getProxy("DocumentType",
        jsonorder.getString("documentType")));
    order.setAccountingDate(order.getOrderDate());
    order.setScheduledDeliveryDate(order.getOrderDate());
    order.setPartnerAddress(OBDal.getInstance().get(Location.class,
        jsonorder.getJSONObject("bp").getString("locId")));
    order.setInvoiceAddress(order.getPartnerAddress());
    order.setPaymentMethod((FIN_PaymentMethod) bp.getPaymentMethod());
    order.setPaymentTerms((PaymentTerm) bp.getPaymentTerms());
    order.setInvoiceTerms(bp.getInvoiceTerms());
    order.setGrandTotalAmount(BigDecimal.valueOf(jsonorder.getDouble("gross")).setScale(
        stdPrecision, RoundingMode.HALF_UP));
    order.setSummedLineAmount(BigDecimal.valueOf(jsonorder.getDouble("net")).setScale(stdPrecision,
        RoundingMode.HALF_UP));

    order.setSalesTransaction(true);
    if (jsonorder.getBoolean("isQuotation")) {
      order.setDocumentStatus("UE");
    } else {
      order.setDocumentStatus("CO");
    }
    order.setDocumentAction("--");
    order.setProcessed(true);
    order.setProcessNow(false);
    order.setObposSendemail((jsonorder.has("sendEmail") && jsonorder.getBoolean("sendEmail")));

    JSONObject taxes = jsonorder.getJSONObject("taxes");
    @SuppressWarnings("unchecked")
    Iterator<String> itKeys = taxes.keys();
    int i = 0;
    while (itKeys.hasNext()) {
      String taxId = itKeys.next();
      JSONObject jsonOrderTax = taxes.getJSONObject(taxId);
      OrderTax orderTax = OBProvider.getInstance().get(OrderTax.class);
      TaxRate tax = (TaxRate) OBDal.getInstance().getProxy(
          ModelProvider.getInstance().getEntity(TaxRate.class).getName(), taxId);
      orderTax.setTax(tax);
      orderTax.setTaxableAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("net")).setScale(
          stdPrecision, RoundingMode.HALF_UP));
      orderTax.setTaxAmount(BigDecimal.valueOf(jsonOrderTax.getDouble("amount")).setScale(
          stdPrecision, RoundingMode.HALF_UP));
      orderTax.setSalesOrder(order);
      orderTax.setLineNo((long) ((i + 1) * 10));
      i++;
      order.getOrderTaxList().add(orderTax);
    }
  }

  protected void handleStock(ShipmentInOut shipment) {
    for (ShipmentInOutLine line : shipment.getMaterialMgmtShipmentInOutLineList()) {
      if (line.getProduct().getProductType().equals("I") && line.getProduct().isStocked()) {
        // Stock is changed only for stocked products of type "Item"
        MaterialTransaction transaction = OBProvider.getInstance().get(MaterialTransaction.class);
        transaction.setOrganization(line.getOrganization());
        transaction.setMovementType(shipment.getMovementType());
        transaction.setProduct(line.getProduct());
        transaction.setStorageBin(line.getStorageBin());
        transaction.setOrderUOM(line.getOrderUOM());
        transaction.setUOM(line.getUOM());
        transaction.setOrderQuantity(line.getOrderQuantity());
        transaction.setMovementQuantity(line.getMovementQuantity().multiply(NEGATIVE_ONE));
        transaction.setMovementDate(shipment.getMovementDate());
        transaction.setGoodsShipmentLine(line);
        transaction.setAttributeSetValue(line.getAttributeSetValue());

        updateInventory(transaction);

        OBDal.getInstance().save(transaction);
      }
    }
  }

  protected void updateInventory(MaterialTransaction transaction) {
    try {
      org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
      CallableStatement cs = cp.getConnection().prepareCall(
          "{call M_UPDATE_INVENTORY (?,?,?,?,?,?,?,?,?,?,?,?,?)}");

      // client
      cs.setString(1, OBContext.getOBContext().getCurrentClient().getId());
      // org
      cs.setString(2, OBContext.getOBContext().getCurrentOrganization().getId());
      // user
      cs.setString(3, OBContext.getOBContext().getUser().getId());
      // product
      cs.setString(4, transaction.getProduct().getId());
      // locator
      cs.setString(5, transaction.getStorageBin().getId());
      // attributesetinstance
      cs.setString(6, transaction.getAttributeSetValue() != null ? transaction
          .getAttributeSetValue().getId() : null);
      // uom
      cs.setString(7, transaction.getUOM().getId());
      // product uom
      cs.setString(8, null);
      // p_qty
      cs.setBigDecimal(9,
          transaction.getMovementQuantity() != null ? transaction.getMovementQuantity() : null);
      // p_qtyorder
      cs.setBigDecimal(10, transaction.getOrderQuantity() != null ? transaction.getOrderQuantity()
          : null);
      // p_dateLastInventory --- **
      cs.setDate(11, null);
      // p_preqty
      cs.setBigDecimal(12, transaction.getMovementQuantity() != null ? transaction
          .getMovementQuantity().multiply(NEGATIVE_ONE) : null);
      // p_preqtyorder
      cs.setBigDecimal(13, transaction.getOrderQuantity() != null ? transaction.getOrderQuantity()
          .multiply(NEGATIVE_ONE) : null);

      cs.execute();
      cs.close();
    } catch (Exception e) {
      System.out.println("Error calling to M_UPDATE_INVENTORY");
      throw new OBException(e.getMessage());
    }
  }

  protected Date getCalculatedDueDateBasedOnPaymentTerms(Date startingDate, PaymentTerm paymentTerms) {
    // TODO Take into account the flag "Next business date"
    // TODO Take into account the flag "Fixed due date"
    long daysToAdd = paymentTerms.getOverduePaymentDaysRule();
    long MonthOffset = paymentTerms.getOffsetMonthDue();
    String dayToPay = paymentTerms.getOverduePaymentDayRule();
    Calendar calculatedDueDate = new GregorianCalendar();
    calculatedDueDate.setTime(startingDate);
    if (MonthOffset > 0) {
      calculatedDueDate.add(Calendar.MONTH, (int) MonthOffset);
    }
    if (daysToAdd > 0) {
      calculatedDueDate.add(Calendar.DATE, (int) daysToAdd);
    }
    if (dayToPay != null && !dayToPay.equals("")) {
      // for us: 1 -> Monday
      // for Calendar: 1 -> Sunday
      int dayOfTheWeekToPay = Integer.parseInt(dayToPay);
      dayOfTheWeekToPay += 1;
      if (dayOfTheWeekToPay == 8) {
        dayOfTheWeekToPay = 1;
      }
      if (calculatedDueDate.get(Calendar.DAY_OF_WEEK) == dayOfTheWeekToPay) {
        return calculatedDueDate.getTime();
      } else {
        Boolean dayFound = false;
        while (dayFound == false) {
          calculatedDueDate.add(Calendar.DATE, 1);
          if (calculatedDueDate.get(Calendar.DAY_OF_WEEK) == dayOfTheWeekToPay) {
            dayFound = true;
          }
        }
      }
    }
    return calculatedDueDate.getTime();
  }

  protected JSONObject handlePayments(JSONObject jsonorder, Order order, Invoice invoice,
      Boolean wasPaidOnCredit) throws Exception {
    String posTerminalId = jsonorder.getString("posTerminal");
    OBPOSApplications posTerminal = OBDal.getInstance().get(OBPOSApplications.class, posTerminalId);
    if (posTerminal == null) {
      final JSONObject jsonResponse = new JSONObject();
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
      jsonResponse.put(JsonConstants.RESPONSE_ERRORMESSAGE, "The POS terminal with id "
          + posTerminalId + " couldn't be found");
      return jsonResponse;
    } else {
      JSONArray payments = jsonorder.getJSONArray("payments");

      // Create a unique payment schedule for all payments
      BigDecimal amt = BigDecimal.valueOf(jsonorder.getDouble("payment"));
      FIN_PaymentSchedule paymentSchedule = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
      int stdPrecision = order.getCurrency().getStandardPrecision().intValue();
      if (fullpayLayaway || partialpayLayaway) {
        paymentSchedule = order.getFINPaymentScheduleList().get(0);
        stdPrecision = order.getCurrency().getStandardPrecision().intValue();
      } else {
        paymentSchedule = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
        paymentSchedule.setCurrency(order.getCurrency());
        paymentSchedule.setOrder(order);
        paymentSchedule.setFinPaymentmethod(order.getBusinessPartner().getPaymentMethod());
        // paymentSchedule.setPaidAmount(new BigDecimal(0));
        stdPrecision = order.getCurrency().getStandardPrecision().intValue();
        paymentSchedule.setAmount(BigDecimal.valueOf(jsonorder.getDouble("gross")).setScale(
            stdPrecision, RoundingMode.HALF_UP));
        // Sept 2012 -> gross because outstanding is not allowed in Openbravo Web POS
        paymentSchedule.setOutstandingAmount(BigDecimal.valueOf(jsonorder.getDouble("gross"))
            .setScale(stdPrecision, RoundingMode.HALF_UP));
        paymentSchedule.setDueDate(order.getOrderDate());
        paymentSchedule.setExpectedDate(order.getOrderDate());
        if (ModelProvider.getInstance().getEntity(FIN_PaymentSchedule.class)
            .hasProperty("origDueDate")) {
          // This property is checked and set this way to force compatibility with both MP13, MP14
          // and
          // later releases of Openbravo. This property is mandatory and must be set. Check issue
          paymentSchedule.set("origDueDate", paymentSchedule.getDueDate());
        }
        paymentSchedule.setFINPaymentPriority(order.getFINPaymentPriority());
        OBDal.getInstance().save(paymentSchedule);
      }

      FIN_PaymentSchedule paymentScheduleInvoice = null;
      if (invoice != null) {
        paymentScheduleInvoice = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
        paymentScheduleInvoice.setCurrency(order.getCurrency());
        paymentScheduleInvoice.setInvoice(invoice);
        paymentScheduleInvoice.setFinPaymentmethod(order.getBusinessPartner().getPaymentMethod());
        paymentScheduleInvoice.setAmount(BigDecimal.valueOf(jsonorder.getDouble("gross")).setScale(
            stdPrecision, RoundingMode.HALF_UP));
        paymentScheduleInvoice.setOutstandingAmount(BigDecimal
            .valueOf(jsonorder.getDouble("gross")).setScale(stdPrecision, RoundingMode.HALF_UP));
        // TODO: If the payment terms is configured to work with fractionated payments, we should
        // generate several payment schedules
        if (wasPaidOnCredit) {
          paymentScheduleInvoice.setDueDate(getCalculatedDueDateBasedOnPaymentTerms(
              order.getOrderDate(), order.getPaymentTerms()));
          paymentScheduleInvoice.setExpectedDate(paymentScheduleInvoice.getDueDate());
        } else {
          paymentScheduleInvoice.setDueDate(order.getOrderDate());
          paymentScheduleInvoice.setExpectedDate(order.getOrderDate());
        }

        if (ModelProvider.getInstance().getEntity(FIN_PaymentSchedule.class)
            .hasProperty("origDueDate")) {
          // This property is checked and set this way to force compatibility with both MP13, MP14
          // and
          // later releases of Openbravo. This property is mandatory and must be set. Check issue
          paymentScheduleInvoice.set("origDueDate", paymentScheduleInvoice.getDueDate());
        }
        paymentScheduleInvoice.setFINPaymentPriority(order.getFINPaymentPriority());
        invoice.getFINPaymentScheduleList().add(paymentScheduleInvoice);
        OBDal.getInstance().save(paymentScheduleInvoice);
        OBDal.getInstance().save(invoice);
      }

      BigDecimal gross = BigDecimal.valueOf(jsonorder.getDouble("gross"));
      BigDecimal writeoffAmt = amt.subtract(gross.abs());

      for (int i = 0; i < payments.length(); i++) {
        JSONObject payment = payments.getJSONObject(i);
        OBPOSAppPayment paymentType = null;
        if (payment.has("isPrePayment") && payment.getBoolean("isPrePayment")) {
          continue;
        }
        String paymentTypeName = payment.getString("kind");
        for (OBPOSAppPayment type : posTerminal.getOBPOSAppPaymentList()) {
          if (type.getSearchKey().equals(paymentTypeName)) {
            paymentType = type;
          }
        }
        if (paymentType == null) {
          @SuppressWarnings("unchecked")
          Class<PaymentProcessor> paymentclazz = (Class<PaymentProcessor>) Class
              .forName(paymentTypeName);
          PaymentProcessor paymentinst = paymentclazz.newInstance();
          paymentinst.process(payment, order, invoice, i == (payments.length() - 1) ? writeoffAmt
              : BigDecimal.ZERO);
        } else {
          processPayments(paymentSchedule, paymentScheduleInvoice, order, invoice, paymentType,
              payment, i == (payments.length() - 1) ? writeoffAmt : BigDecimal.ZERO, jsonorder);
        }
      }
      if (invoice != null && fullpayLayaway) {
        for (int j = 0; j < paymentSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList()
            .size(); j++) {
          if (paymentSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList().get(j)
              .getInvoicePaymentSchedule() == null) {
            paymentSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList().get(j)
                .setInvoicePaymentSchedule(paymentScheduleInvoice);
          }
        }
        invoice.setTotalPaid(invoice.getGrandTotalAmount());
        invoice.setOutstandingAmount(BigDecimal.ZERO);
        invoice.setDueAmount(BigDecimal.ZERO);
        invoice.setPaymentComplete(true);
        paymentScheduleInvoice.setOutstandingAmount(BigDecimal.ZERO);
        paymentScheduleInvoice.setPaidAmount(paymentScheduleInvoice.getAmount());
        invoice.getFINPaymentScheduleList().add(paymentScheduleInvoice);
        OBDal.getInstance().save(paymentScheduleInvoice);
        OBDal.getInstance().save(invoice);
      }

      BigDecimal diffPaid = BigDecimal.ZERO;
      if ((gross.compareTo(BigDecimal.ZERO) > 0) && (gross.compareTo(amt) > 0)) {
        diffPaid = gross.subtract(amt);
      } else if ((gross.compareTo(BigDecimal.ZERO) < 0)
          && (gross.compareTo(amt.multiply(new BigDecimal("-1"))) < 0)) {
        diffPaid = gross.subtract(amt.multiply(new BigDecimal("-1")));
      }
      // if (payments.length() == 0 ) or (writeoffAmt<0) means that use credit was used
      if ((payments.length() == 0 || diffPaid.compareTo(BigDecimal.ZERO) != 0) && invoice != null) {
        FIN_PaymentScheduleDetail paymentScheduleDetail = OBProvider.getInstance().get(
            FIN_PaymentScheduleDetail.class);
        paymentScheduleDetail.setOrderPaymentSchedule(paymentSchedule);
        paymentScheduleDetail.setAmount(diffPaid);
        if (paymentScheduleInvoice != null) {
          paymentScheduleDetail.setInvoicePaymentSchedule(paymentScheduleInvoice);
        }
        OBDal.getInstance().save(paymentScheduleDetail);
      }

      return null;
    }

  }

  protected void processPayments(FIN_PaymentSchedule paymentSchedule,
      FIN_PaymentSchedule paymentScheduleInvoice, Order order, Invoice invoice,
      OBPOSAppPayment paymentType, JSONObject payment, BigDecimal writeoffAmt, JSONObject jsonorder)
      throws Exception {
    long t1 = System.currentTimeMillis();
    OBContext.setAdminMode(true);
    try {
      boolean totalIsNegative = jsonorder.getDouble("gross") < 0;
      boolean checkPaidOnCreditChecked = (jsonorder.has("paidOnCredit") && jsonorder
          .getBoolean("paidOnCredit"));
      int stdPrecision = order.getCurrency().getStandardPrecision().intValue();
      BigDecimal amount = BigDecimal.valueOf(payment.getDouble("origAmount")).setScale(
          stdPrecision, RoundingMode.HALF_UP);
      BigDecimal origAmount = amount;
      BigDecimal mulrate = new BigDecimal(1);
      // FIXME: Coversion should be only in one direction: (USD-->EUR)
      if (payment.has("mulrate") && payment.getDouble("mulrate") != 1) {
        mulrate = BigDecimal.valueOf(payment.getDouble("mulrate"));
        if (payment.has("amount")) {
          origAmount = BigDecimal.valueOf(payment.getDouble("amount")).setScale(stdPrecision,
              RoundingMode.HALF_UP);
        } else {
          origAmount = amount.multiply(mulrate).setScale(stdPrecision, RoundingMode.HALF_UP);
        }
      }

      // writeoffAmt.divide(BigDecimal.valueOf(payment.getDouble("rate")));
      if (amount.signum() == 0) {
        return;
      }
      if (writeoffAmt.signum() == 1) {
        // there was an overpayment, we need to take into account the writeoffamt
        if (totalIsNegative) {
          amount = amount.subtract(writeoffAmt.negate()).setScale(stdPrecision,
              RoundingMode.HALF_UP);
        } else {
          amount = amount.subtract(writeoffAmt.abs()).setScale(stdPrecision, RoundingMode.HALF_UP);
        }
      } else if (writeoffAmt.signum() == -1
          && (!isLayaway && !partialpayLayaway && !fullpayLayaway && !checkPaidOnCreditChecked)) {
        if (totalIsNegative) {
          amount = amount.add(writeoffAmt).setScale(stdPrecision, RoundingMode.HALF_UP);
        } else {
          amount = amount.add(writeoffAmt.abs()).setScale(stdPrecision, RoundingMode.HALF_UP);
        }
        origAmount = amount;
        if (payment.has("mulrate") && payment.getDouble("mulrate") != 1) {
          mulrate = BigDecimal.valueOf(payment.getDouble("mulrate"));
          origAmount = amount.multiply(BigDecimal.valueOf(payment.getDouble("mulrate"))).setScale(
              stdPrecision, RoundingMode.HALF_UP);
        }
      }

      FIN_PaymentScheduleDetail paymentScheduleDetail = OBProvider.getInstance().get(
          FIN_PaymentScheduleDetail.class);
      paymentScheduleDetail.setOrderPaymentSchedule(paymentSchedule);
      paymentScheduleDetail.setAmount(amount);
      paymentSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList().add(
          paymentScheduleDetail);

      OBDal.getInstance().save(paymentScheduleDetail);
      if (paymentScheduleInvoice != null) {
        paymentScheduleInvoice.getFINPaymentScheduleDetailInvoicePaymentScheduleList().add(
            paymentScheduleDetail);
        paymentScheduleDetail.setInvoicePaymentSchedule(paymentScheduleInvoice);

        Fin_OrigPaymentSchedule origPaymentSchedule = OBProvider.getInstance().get(
            Fin_OrigPaymentSchedule.class);
        origPaymentSchedule.setCurrency(order.getCurrency());
        origPaymentSchedule.setInvoice(invoice);
        origPaymentSchedule.setPaymentMethod(paymentSchedule.getFinPaymentmethod());
        origPaymentSchedule.setAmount(amount);
        origPaymentSchedule.setDueDate(order.getOrderDate());
        origPaymentSchedule.setPaymentPriority(paymentScheduleInvoice.getFINPaymentPriority());

        OBDal.getInstance().save(origPaymentSchedule);

        FIN_OrigPaymentScheduleDetail origDetail = OBProvider.getInstance().get(
            FIN_OrigPaymentScheduleDetail.class);
        origDetail.setArchivedPaymentPlan(origPaymentSchedule);
        origDetail.setPaymentScheduleDetail(paymentScheduleDetail);
        origDetail.setAmount(amount);
        origDetail.setWriteoffAmount(paymentScheduleDetail.getWriteoffAmount().setScale(
            stdPrecision, RoundingMode.HALF_UP));

        OBDal.getInstance().save(origDetail);
      }

      HashMap<String, BigDecimal> paymentAmount = new HashMap<String, BigDecimal>();
      paymentAmount.put(paymentScheduleDetail.getId(), amount);

      FIN_FinancialAccount account = paymentType.getFinancialAccount();

      long t2 = System.currentTimeMillis();
      // Save Payment

      List<FIN_PaymentScheduleDetail> detail = new ArrayList<FIN_PaymentScheduleDetail>();
      detail.add(paymentScheduleDetail);

      DocumentType paymentDocType = getPaymentDocumentType(order.getOrganization());
      Entity paymentEntity = ModelProvider.getInstance().getEntity(FIN_Payment.class);
      String paymentDocNo = getDocumentNo(paymentEntity, null, paymentDocType);

      // get date
      Date calculatedDate = (payment.has("date") && !payment.isNull("date")) ? OBMOBCUtils
          .calculateServerDate((String) payment.get("date"), jsonorder.getLong("timezoneOffset"))
          : OBMOBCUtils.stripTime(new Date());

      FIN_Payment finPayment = FIN_AddPayment.savePayment(null, true, paymentDocType, paymentDocNo,
          order.getBusinessPartner(), paymentType.getPaymentMethod().getPaymentMethod(), account,
          amount.toString(), calculatedDate, order.getOrganization(), null, detail, paymentAmount,
          false, false, order.getCurrency(), mulrate, origAmount);
      if (writeoffAmt.signum() == 1) {
        if (totalIsNegative) {
          FIN_AddPayment.saveGLItem(finPayment, writeoffAmt.negate(), paymentType
              .getPaymentMethod().getGlitemWriteoff());
        } else {
          FIN_AddPayment.saveGLItem(finPayment, writeoffAmt, paymentType.getPaymentMethod()
              .getGlitemWriteoff());
        }
        // Update Payment In amount after adding GLItem
        finPayment.setAmount(origAmount.setScale(stdPrecision, RoundingMode.HALF_UP));
      }
      OBDal.getInstance().save(finPayment);

      String description = getPaymentDescription();
      description += ": " + order.getDocumentNo() + "\n";
      finPayment.setDescription(description);

      long t3 = System.currentTimeMillis();
      // Process Payment

      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      ProcessBundle pb = new ProcessBundle("6255BE488882480599C81284B70CD9B3", vars)
          .init(new DalConnectionProvider(false));
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("action", "D");
      parameters.put("Fin_Payment_ID", finPayment.getId());
      parameters.put("isPOSOrder", "Y");
      pb.setParams(parameters);
      FIN_PaymentProcess process = new FIN_PaymentProcess();
      process.execute(pb);
      OBError result = (OBError) pb.getResult();
      if (result.getType().equalsIgnoreCase("Error")) {
        throw new OBException(result.getMessage());
      }
      vars.setSessionValue("POSOrder", "Y");
      log.debug("Payment. Create entities: " + (t2 - t1) + "; Save payment: " + (t3 - t2)
          + "; Process payment: " + (System.currentTimeMillis() - t3));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  protected void createApprovals(Order order, JSONObject jsonorder) {
    if (!jsonorder.has("approvals")) {
      return;
    }
    Entity approvalEntity = ModelProvider.getInstance().getEntity(OrderApproval.class);
    try {
      JSONArray approvals = jsonorder.getJSONArray("approvals");
      for (int i = 0; i < approvals.length(); i++) {
        JSONObject jsonApproval = approvals.getJSONObject(i);

        OrderApproval approval = OBProvider.getInstance().get(OrderApproval.class);

        JSONPropertyToEntity.fillBobFromJSON(approvalEntity, approval, jsonApproval,
            jsonorder.getLong("timezoneOffset"));

        approval.setSalesOrder(order);

        Long value = jsonorder.getLong("created");
        Date creationDate = new Date(value);
        approval.setCreationDate(creationDate);
        approval.setUpdated(creationDate);

        OBDal.getInstance().save(approval);
      }

    } catch (JSONException e) {
      log.error("Error creating approvals for order" + order, e);
    }
  }

  protected void fillBobFromJSON(Entity entity, BaseOBObject bob, JSONObject json)
      throws JSONException {
    @SuppressWarnings("unchecked")
    Iterator<String> keys = json.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (key.equals("id")) {
        continue;
      }
      String oldKey = key;
      if (entity.hasProperty(key)) {
        log.debug("Found property: " + key + " in entity " + entity.getName());
      } else {
        key = getEquivalentKey(key);
        if (key == null) {
          log.debug("Did not find property: " + oldKey);
          continue;
        } else {
          if (entity.hasProperty(key)) {
            log.debug("Found equivalent key: " + key);
          } else {
            log.debug("Did not find property: " + oldKey);
            continue;
          }
        }
      }

      Property p = entity.getProperty(key);
      Object value = json.get(oldKey);
      if (p.isPrimitive()) {
        if (p.isDate()) {
          bob.set(p.getName(),
              (Date) JsonToDataConverter.convertJsonToPropertyValue(PropertyByType.DATE, value));
        } else if (p.isNumericType()) {
          value = json.getString(oldKey);
          bob.set(key, new BigDecimal((String) value));
        } else {
          bob.set(p.getName(), value);
        }
      } else {
        Property refProp = p.getReferencedProperty();
        Entity refEntity = refProp.getEntity();
        if (value instanceof JSONObject) {
          value = ((JSONObject) value).getString("id");
        }
        BaseOBObject refBob = OBDal.getInstance().getProxy(refEntity.getName(), value.toString());
        bob.set(p.getName(), refBob);
      }

    }
  }

  private static String getEquivalentKey(String key) {
    if (key.equals("bp")) {
      return "businessPartner";
    } else if (key.equals("bploc")) {
      return "partnerAddress";
    } else if (key.equals("qty")) {
      return "orderedQuantity";
    } else if (key.equals("price")) {
      return "grossUnitPrice";
    } else if (key.equals("posTerminal")) {
      return "obposApplications";
    } else if (key.equals("pricenet")) {
      return "unitPrice";
    }
    return null;
  }

  protected String getDocumentNo(Entity entity, DocumentType doctypeTarget, DocumentType doctype) {
    return Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
        new DalConnectionProvider(false), RequestContext.get().getVariablesSecureApp(), "", entity
            .getTableName(), doctypeTarget == null ? "" : doctypeTarget.getId(),
        doctype == null ? "" : doctype.getId(), false, true);
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

}
