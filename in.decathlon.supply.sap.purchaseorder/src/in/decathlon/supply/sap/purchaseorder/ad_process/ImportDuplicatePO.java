package in.decathlon.supply.sap.purchaseorder.ad_process;

import in.decathlon.supply.sap.purchaseorder.data.IMSAPImportDuplicatePO;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Expression;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Region;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class ImportDuplicatePO extends DalBaseProcess {

  private static final Logger log4j = Logger.getLogger(ImportDuplicatePO.class);
  private OrderLine ol = null;
  private String itemcode = "";
  OBError msg = new OBError();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      System.out.println("Inside the Default ImportDuplicatePO");
      // coding starts
      String user_id = OBContext.getOBContext().getUser().getId();
      // System.out.println("user_id->" + user_id);
      String validateResult = ImportDuplicatePOData.imsap_validateduplicatepo(
          bundle.getConnection(), user_id);
      System.out.println("ValidateResult->" + validateResult);
      log4j.info("ValidateResult->" + validateResult);

      if (validateResult.equals("t")) {
        String importResult = importduplicatepo(user_id);
        System.out.println("importResult->" + importResult);
        log4j.info("importResult->" + importResult);
        if (importResult.equals("t")) {
          msg.setType("Success");
          msg.setTitle("Success");
          msg.setMessage("Duplicate PO Updated/Inserted Successfully");
        } else {
          msg.setType("Error");
          msg.setTitle("Error occurred");
          msg.setMessage("Import Failure: Please check Errormsg");
        }
      } else {
        msg.setType("Error");
        msg.setTitle("Error occurred");
        msg.setMessage("Validation Failure: Please check Errormsg");
      }
    } catch (Exception e) {
      log4j.error(e.getMessage(), e);
      msg.setType("Error");
      msg.setMessage("Error while importing SAP PO - " + e.getMessage());
    } finally {
      bundle.setResult(msg);

    }
  }

  private String importduplicatepo(String user_id) {
    String result = "t";
    boolean create = false;
    Order po = null;
    int line = 10;

    User u = OBDal.getInstance().get(User.class, user_id);
    // main loop to get PO list to be created/updated
    String sqlQuery = "SELECT distinct poreference FROM imsap_importduplicate_po "
        + " WHERE validated='Y' AND create_po='Y' AND processed='N' AND createdby=? "
        + " order by poreference";
    SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
    query.setString(0, user_id);
    List poList = query.list();
    System.out.println("Distinct POList->" + poList.size());
    log4j.info(("POList->" + poList.size()));
    // check for list of POs
    for (int p = 0; p < poList.size(); p++) {
      // get all po list to create po based on order reference
      OBCriteria<IMSAPImportDuplicatePO> obCriteria = OBDal.getInstance().createCriteria(
          IMSAPImportDuplicatePO.class);
      obCriteria.add(Expression.eq(IMSAPImportDuplicatePO.PROPERTY_CREATEDBY, u));
      obCriteria.add(Expression.eq(IMSAPImportDuplicatePO.PROPERTY_CREATEPO, true));
      obCriteria.add(Expression.eq(IMSAPImportDuplicatePO.PROPERTY_PROCESSED, false));
      obCriteria.add(Expression.eq(IMSAPImportDuplicatePO.PROPERTY_VALIDATED, true));
      obCriteria.add(Expression.eq(IMSAPImportDuplicatePO.PROPERTY_ORDERREFERENCE, poList.get(p)));
      obCriteria.addOrderBy(IMSAPImportDuplicatePO.PROPERTY_ORDERREFERENCE, true);
      obCriteria.addOrderBy(IMSAPImportDuplicatePO.PROPERTY_ITEMCODE, true);

      List<IMSAPImportDuplicatePO> oList = obCriteria.list();
      // check whether to insert/update PO
      OBCriteria<Order> orderCriteria = OBDal.getInstance().createCriteria(Order.class);
      orderCriteria.add(Expression.eq(Order.PROPERTY_SALESTRANSACTION, false));
      orderCriteria.add(Expression.eq(Order.PROPERTY_ORDERREFERENCE, poList.get(p)));
      // orderCriteria.add(Expression.eq(Order.PROPERTY_IMSAPDUPLICATESAPPO, false));
      List<Order> orderList = orderCriteria.list();
      if (orderList.isEmpty())
        create = true;
      else
        create = false;
      // System.out.println("OList->" + oList.size());
      for (IMSAPImportDuplicatePO sapPO : oList) {
        // System.out.println("Order->" + sapPO.getOrderReference() + ":Itemcode->"
        // + sapPO.getItemcode());
        if (create) {
          // create new PO
          // System.out.println("Create PO->" + sapPO.getOrderReference());
          if ((po == null) || (!po.getSwPoReference().equals(sapPO.getOrderReference()))) {
            line = 10;
            po = createPO(sapPO, po);
            ol = createLines(sapPO, po, line);
            itemcode = ol.getProduct().getName();
            // System.out.println("Product->" + itemcode);
          }
          // for lines
          else {
            // System.out.println("Inside else ");
            if ((!itemcode.equals(sapPO.getItemcode()))
                && (ol.getSalesOrder().getOrderReference().equals(sapPO.getOrderReference()))) {
              // System.out.println("Inside else Product->" + itemcode);

              line += 10;
              ol = createLines(sapPO, po, line);
            }
          }

          if (po != null) {
            po.setProcessed(true);
            OBDal.getInstance().save(po);
          }

          sapPO.setProcessed(true);
          OBDal.getInstance().save(sapPO);
        } else {
          // update to existing PO
          po = orderList.get(0);
		  if (!sapPO.getOrderstatus().equals("C")) {
          if (sapPO.getOrderstatus().equals("N")) {
            po.setDocumentStatus("CO");
            po.setDocumentAction("CL");
            po.setSWEMSwPostatus("OS");
          } else if (sapPO.getOrderstatus().equals("U")) {
            po.setDocumentStatus("CO");
            po.setDocumentAction("CL");
            po.setSWEMSwPostatus("OU");
          } else if (sapPO.getOrderstatus().equals("V")) {
            po.setDocumentStatus("CO");
            po.setDocumentAction("CL");
            po.setSWEMSwPostatus("VD");
          } else if (sapPO.getOrderstatus().equals("S")) {
            po.setDocumentStatus("CO");
            po.setDocumentAction("CL");
            po.setSWEMSwPostatus("SH");
          } else if (sapPO.getOrderstatus().equals("D")) {
            po.setDocumentStatus("VO");
            po.setDocumentAction("CL");
            po.setSWEMSwPostatus("OC");
          } /*
               * else if (sapPO.getOrderstatus().equals("C")) { po.setDocumentStatus("CL");
               * po.setDocumentAction("--"); po.setSWEMSwPostatus("CL"); }
               */
          DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
          Date edd;
          Date cdd;
          try {
            edd = dateFormat.parse(sapPO.getEDD());
            cdd = dateFormat.parse(sapPO.getCDD());
            po.setSWEMSwEstshipdate(edd);
            po.setSWEMSwExpdeldate(cdd);
          } catch (ParseException e) {
            e.printStackTrace();
          }
          po.setImsapDuplicatesapPo(true);
          OBDal.getInstance().save(po);
          po = orderList.get(0);
          System.out.println("Update PO->" + po.getOrderReference());
          List<OrderLine> olList = po.getOrderLineList();
          for (OrderLine odl : olList) {
            if (odl.getProduct().getName().equals(sapPO.getItemcode())) {
            odl.setOrderedQuantity(new BigDecimal(sapPO.getMatchedPOQty()));
            odl.setSwConfirmedqty(sapPO.getConfirmedqty());
            odl.setSwFob(sapPO.getNETOrderPrice().toString());
            odl.setCurrency(getCurrency(sapPO.getCurrency()));
            odl.setUpdatedBy(u);
            odl.setUpdated(sapPO.getUpdated());
            OBDal.getInstance().save(odl);
		    }
          }
          sapPO.setProcessed(true);
          OBDal.getInstance().save(sapPO);
        }else{
          sapPO.setProcessed(true);
          OBDal.getInstance().save(sapPO);
        }
       }// end of update
      }
      OBDal.getInstance().flush();
    }
    return result;
  }

  private Order createPO(IMSAPImportDuplicatePO sapPO, Order oldpo) {
    Order o = null;
    // for header
    o = OBProvider.getInstance().get(Order.class);
    Organization org = OBDal.getInstance().get(Organization.class,
        "603C6A266B4C40BCAD87C5C43DDF53EE");
    Client c = OBDal.getInstance().get(Client.class, "187D8FC945A5481CB41B3EE767F80DBB");
    OBCriteria<BusinessPartner> bpCriteria = OBDal.getInstance().createCriteria(
        BusinessPartner.class);
    bpCriteria.add(Expression.eq(BusinessPartner.PROPERTY_CLSUPPLIERNO, sapPO.getSuppliercode()));
    bpCriteria.add(Expression.eq(BusinessPartner.PROPERTY_VENDOR, true));
    final List<BusinessPartner> bpList = bpCriteria.list();
    BusinessPartner bp = bpList.get(0);
    try {
      OBContext.setAdminMode(true);
      o.setActive(true);
      o.setClient(c);
      o.setOrganization(org);
      DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
      Date createddate;
      Date updateddate;
      Date edd;
      Date cdd;

      try {
        createddate = dateFormat.parse(sapPO.getCreateddate());
        updateddate = dateFormat.parse(sapPO.getUpdateddate());
        updateddate = dateFormat.parse(sapPO.getUpdateddate());
        edd = dateFormat.parse(sapPO.getEDD());
        cdd = dateFormat.parse(sapPO.getCDD());

        o.setCreationDate(createddate);
        o.setUpdated(updateddate);
        o.setSWEMSwEstshipdate(edd);
        o.setSWEMSwExpdeldate(cdd);
        o.setOrderDate(createddate);
        o.setAccountingDate(createddate);
        o.setScheduledDeliveryDate(createddate);
      } catch (ParseException e) {
        e.printStackTrace();
      }
      o.setCreatedBy(sapPO.getCreatedBy());
      o.setUpdatedBy(sapPO.getUpdatedBy());
      o.setBusinessPartner(bp);
      if (!bp.getBusinessPartnerLocationList().isEmpty()) {
        o.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
        o.setInvoiceAddress(bp.getBusinessPartnerLocationList().get(0));
      } else {
        o.setPartnerAddress(getTempLocationForBP());
        o.setInvoiceAddress(getTempLocationForBP());
      }
      DocumentType docType = getDocumentType("Purchase Order (Decathlon)");
      o.setTransactionDocument(docType);
      o.setDocumentType(docType);
      String docNo = getDocSequenceNo("Purchase Order (Decathlon)");
      o.setDocumentNo(docNo);

      Currency cur = getCurrency(sapPO.getCurrency());
      o.setCurrency(cur);
      o.setSWEMSwCurrency(cur);
      o.setPaymentTerms(getPaymentTerms());// After Delivery
      o.setPaymentMethod(getPaymentMethod());// Cash
      o.setPriceList(getPriceList());// FOB
      if (sapPO.getOrderstatus().equals("N")) {
        o.setDocumentStatus("CO");
        o.setDocumentAction("CL");
        o.setSWEMSwPostatus("OS");
      } else if (sapPO.getOrderstatus().equals("U")) {
        o.setDocumentStatus("CO");
        o.setDocumentAction("CL");
        o.setSWEMSwPostatus("OU");
      } else if (sapPO.getOrderstatus().equals("V")) {
        o.setDocumentStatus("CO");
        o.setDocumentAction("CL");
        o.setSWEMSwPostatus("VD");
      } else if (sapPO.getOrderstatus().equals("S")) {
        o.setDocumentStatus("CO");
        o.setDocumentAction("CL");
        o.setSWEMSwPostatus("SH");
      } else if (sapPO.getOrderstatus().equals("D")) {
        o.setDocumentStatus("VO");
        o.setDocumentAction("CL");
        o.setSWEMSwPostatus("OC");
      } else if (sapPO.getOrderstatus().equals("C")) {
        o.setDocumentStatus("CL");
        o.setDocumentAction("--");
        o.setSWEMSwPostatus("CL");
      }
      o.setSwPoReference(sapPO.getOrderReference());
      o.setOrderReference(sapPO.getOrderReference());
      o.setSalesTransaction(false);
      o.setFreightAmount(new BigDecimal(0));
      o.setWarehouse(getWarehouse(org));
      o.setReceiveMaterials(true);
      o.setDescription("Created User-> " + sapPO.getCreatedBy25() + " : Updated User-> "
          + sapPO.getChangedBy() + " : Plant->" + sapPO.getComments());
      o.setImsapDuplicatesapPo(true);

      o.setImsapDuplicatesapPo(true);
      OBDal.getInstance().save(o);
    } catch (Exception e) {
      System.out.println("An error happened when creating PO->" + e.getMessage());
      log4j.error("An error happened when creating PO->" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    // OBDal.getInstance().flush();
    return o;
  }

  private OrderLine createLines(IMSAPImportDuplicatePO sapPO, Order o, int line) {
    OBCriteria<Product> pCriteria = OBDal.getInstance().createCriteria(Product.class);
    pCriteria.add(Expression.eq(Product.PROPERTY_NAME, sapPO.getItemcode()));
    final List<Product> pList = pCriteria.list();
    Product p = pList.get(0);
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
    ol.setOrderDate(o.getOrderDate());
    ol.setWarehouse(o.getWarehouse());
    ol.setOrderedQuantity(new BigDecimal(sapPO.getMatchedPOQty()));
    ol.setDeliveredQuantity(new BigDecimal(0));
    ol.setLineNetAmount(new BigDecimal(sapPO.getMatchedPOQty()).multiply(new BigDecimal(sapPO
        .getNETOrderPrice())));
    ol.setCurrency(o.getCurrency());
    ol.setUOM(p.getUOM());
    ol.setTax(taxRate);
    ol.setDescription(null);
    ol.setSWEMSwSuppliercode(o.getBusinessPartner().getClSupplierno());
    if (sapPO.getOrderstatus().equals("S") || sapPO.getOrderstatus().equals("C")) {
      ol.setSwConfirmedqty(sapPO.getMatchedPOQty());
    } else {
      ol.setSwConfirmedqty(sapPO.getConfirmedqty());
    }
    ol.setCLModelName(p.getClModelname());
    ol.setCLColor(p.getClColor());
    ol.setCLSize(p.getClSize());
    ol.setSwFob(sapPO.getNETOrderPrice().toString());
    OBDal.getInstance().save(ol);
    o.setSWEMSwModelcode(p.getClModelcode());
    o.setSWEMSwModelname(p.getClModelname());
    o.setSWEMSwBrand(p.getClModel().getBrand());
    o.setSwDept(p.getClModel().getDepartment());
    OBDal.getInstance().save(o);
    // OBDal.getInstance().flush();
    return ol;
  }

  private Location getTempLocationForBP() {
    Location loc = OBDal.getInstance().get(Location.class, "CF474260D60641439922F679EE439FD9");
    // System.out.println("Location->" + loc);
    return loc;
  }

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

  private String getDocSequenceNo(String docTypeName) {
    Sequence seq = null;
    String docNo = "";
    OBCriteria<Sequence> obCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    obCriteria.add(Expression.eq(Sequence.PROPERTY_NAME, docTypeName));
    List<Sequence> seqList = obCriteria.list();
    if (!seqList.isEmpty()) {
      seq = seqList.get(0);
      docNo = seq.getNextAssignedNumber().toString();
      // to increment the next documentNo in the Document Sequence
      String dNo = docNo;
      seq.setNextAssignedNumber(Long.parseLong(dNo) + 1);
      OBDal.getInstance().save(seq);
    } else
      throw new OBException("Document Sequence not created in ERP for Purchase Order (decathlon)");

    return docNo;
  }

  private Currency getCurrency(String code) {
    Currency c = null;
    // System.out.println("Code->" + code);

    OBCriteria<Currency> obCriteria = OBDal.getInstance().createCriteria(Currency.class);
    obCriteria.add(Expression.eq(Currency.PROPERTY_ISOCODE, code.trim()));
    List<Currency> currencyList = obCriteria.list();
    if (!currencyList.isEmpty())
      c = currencyList.get(0);
    else
      throw new OBException("Currency code does not exists.");
    // System.out.println("Currency->" + c);

    return c;
  }

  private PriceList getPriceList() {
    PriceList priceList = OBDal.getInstance().get(PriceList.class,
        "A6178825A4CD48A5BFF15471CA4AB823");
    return priceList;
  }

  private PaymentTerm getPaymentTerms() {
    PaymentTerm payTerms = null;
    OBCriteria<PaymentTerm> obCriteria = OBDal.getInstance().createCriteria(PaymentTerm.class);
    obCriteria.add(Expression.eq(PaymentTerm.PROPERTY_NAME, "Immediate"));
    final List<PaymentTerm> payTermsList = obCriteria.list();
    if (!payTermsList.isEmpty())
      payTerms = payTermsList.get(0);
    else
      throw new OBException("Payment Term not defined for Immediate");
    return payTerms;
  }

  private FIN_PaymentMethod getPaymentMethod() {
    FIN_PaymentMethod payMethod = null;
    OBCriteria<FIN_PaymentMethod> obCriteria = OBDal.getInstance().createCriteria(
        FIN_PaymentMethod.class);
    obCriteria.add(Expression.eq(FIN_PaymentMethod.PROPERTY_NAME, "Cash"));
    final List<FIN_PaymentMethod> payMethodList = obCriteria.list();
    if (!payMethodList.isEmpty())
      payMethod = payMethodList.get(0);
    else
      throw new OBException("Payment Method not defined for Cash");
    return payMethod;
  }

  private Warehouse getWarehouse(Organization org) {
    Warehouse w = null;
    OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
    obCriteria.add(Expression.eq(Warehouse.PROPERTY_NAME, "Saleable Whitefield"));
    final List<Warehouse> wList = obCriteria.list();
    if (!wList.isEmpty())
      w = wList.get(0);
    else
      throw new OBException("Warehouse not found for Saleable Whitefield");
    return w;
  }

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

}