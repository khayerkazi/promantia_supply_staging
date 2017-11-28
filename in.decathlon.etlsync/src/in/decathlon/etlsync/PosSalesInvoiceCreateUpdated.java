package in.decathlon.etlsync;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.journalentry.StoreStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLBrand;
import com.sysfore.catalog.CLStoreDept;

public class PosSalesInvoiceCreateUpdated extends DalBaseProcess {

  private ProcessLogger logger;

  static Logger log4j = Logger.getLogger(PosSalesInvoiceCreateUpdated.class);

  final String docType = "511A9371A0F74195AA3F6D66C722729D";
  final String arInvoiceDocType = "40EE9B1CD3B345FABEFDA62B407B407F";
  final Boolean isInvoiced = false;
  final String docStatus = "CO";
  final Boolean isSotrx = true;
  long linenum = 10;
  final String storeStatusClosed = "Y";

  private class InvoiceModel {

    public String orgId = "";
    public String orderDate = "1800-01-01";
    public String brandId = "";
    public String productId = "";
    public String orderedQuantity = "";
    public String lineNetAmout = "";
    public String linegrossAmount = "";
    public String tax = "";

    public boolean invoiceEquals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      InvoiceModel other = (InvoiceModel) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (orderDate == null) {
        if (other.orderDate != null)
          return false;
      } else if (!orderDate.equals(other.orderDate))
        return false;
      if (orgId == null) {
        if (other.orgId != null)
          return false;
      } else if (!orgId.equals(other.orgId))
        return false;
      if (brandId == null) {
        if (other.brandId != null)
          return false;
      } else if (!brandId.equals(other.brandId))
        return false;
      return true;
    }

    private PosSalesInvoiceCreateUpdated getOuterType() {
      return PosSalesInvoiceCreateUpdated.this;
    }

  }

  /*
   * First get a list of ss objects where store status =y, call the procedure with organisation and
   * date as parameter
   */

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    String processid = bundle.getProcessId();
    try {
      logger = bundle.getLogger();
      // paramerers
      // org
      // date

      startPosInvoice();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
    }

  }

  public void startPosInvoice() {

    OBCriteria<StoreStatus> storeStatusRecords = OBDal.getInstance().createCriteria(
        StoreStatus.class);
    storeStatusRecords.add(Restrictions.eq(StoreStatus.PROPERTY_STORESTATUS, "Y"));

    for (StoreStatus st : storeStatusRecords.list()) {

      try {
        createPosInvoiceProcess(st.getOrganization(), st.getClosedDate());
        st.setStoreStatus("P");
        OBDal.getInstance().save(st);
        OBDal.getInstance().commitAndClose();

      } catch (ParseException e) {

        e.printStackTrace();
        logger.log(e.getMessage());
      } catch (Exception e) {

        e.printStackTrace();
        logger.log(e.getMessage());
      }
    }

  }

  public void createPosInvoiceProcess(Organization org, Date storeclosedate) throws ParseException {

    String stclosedate = new SimpleDateFormat("yyyy-MM-dd").format(storeclosedate);
    String orgid = org.getId();

    final String orderLineQuery = "select ol.salesOrder.organization.id,ol.orderDate,"
        + "ol.product.clModel.brand.id,ol.product.id,sum(ol.orderedQuantity),sum(ol.lineNetAmount),"
        + "sum(ol.lineGrossAmount),ol.tax.id from OrderLine ol"
        + " join ol.salesOrder join ol.product join ol.product.clModel "
        + "where  ol.salesOrder.documentType.id='" + docType + "' and ol.salesOrder.reinvoice="
        + isInvoiced + " and ol.salesOrder.documentStatus='" + docStatus
        + "' and ol.salesOrder.salesTransaction =" + isSotrx + " and ol.organization.id ='" + orgid
        + "' and trunc(ol.orderDate)='" + stclosedate + "' and "
        + "trunc(ol.salesOrder.orderDate)=trunc(ol.orderDate) and ol.active=true "
        + " group by ol.salesOrder.organization,ol.orderDate,ol.product.clModel.brand,"
        + " ol.product,ol.tax.id order by ol.orderDate,ol.salesOrder.organization";

    Query query = OBDal.getInstance().getSession().createQuery(orderLineQuery);

    log4j.debug("executing query " + query);
    List<Object[]> qryResult = query.list();
    List<InvoiceModel> inList = new ArrayList<InvoiceModel>();

    if (qryResult != null && qryResult.size() > 0) {

      for (Object[] obj : qryResult) {

        InvoiceModel im = new InvoiceModel();
        im.orgId = obj[0].toString();
        im.orderDate = obj[1].toString();
        im.brandId = obj[2].toString();
        im.productId = obj[3].toString();
        im.orderedQuantity = obj[4].toString();
        im.lineNetAmout = obj[5].toString();
        im.linegrossAmount = obj[6].toString();
        im.tax = obj[7].toString();
        inList.add(im);

      }

      Iterator<InvoiceModel> inListIterator = inList.iterator();
      InvoiceModel previousIM = new InvoiceModel();
      Invoice currentInvoice = null;

      // Start loop in the created models' list
      while (inListIterator.hasNext()) {

        InvoiceModel im = inListIterator.next();

        /*
         * while records { create header do { create lines
         * 
         * }while ( org is same and date is same and store department is same) update header and
         * call procedure for posting }
         */

        if (!im.invoiceEquals(previousIM)) {
          previousIM = im;
          log4j.debug("@etlSync:OrgID:" + im.orgId + "; Brand:" + im.brandId + ";product Id:"
              + im.productId + ";lineNetAmt:" + im.lineNetAmout);
          if (currentInvoice != null) {
            OBDal.getInstance().save(currentInvoice);
            OBDal.getInstance().commitAndClose();

            // Now posting the invoice using stored procedure which
            // takes p_instance id and invoice id as parameters

            completeInvoice(currentInvoice);
          }

         currentInvoice = createInvoiceHeader(im.orgId, im.orderDate, im.lineNetAmout,
            im.linegrossAmount, im.brandId);

          linenum = 10;
          OBDal.getInstance().save(currentInvoice);
        }
        InvoiceLine il = createInvoiceLine(im.orgId, currentInvoice, im.productId,
            im.orderedQuantity, im.lineNetAmout, im.linegrossAmount, im.tax);
        currentInvoice.getInvoiceLineList().add(il);
      }

      // This is done for the last record of the query list
      if (currentInvoice != null) {
        OBDal.getInstance().save(currentInvoice);
        completeInvoice(currentInvoice);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      }

    }
  }

  public Invoice createInvoiceHeader(String orgid, String orderDate, String totalLines,
      String grandTotalAmount, String brandId) {
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    log4j.debug("Entered into createInvoiceHeader function with orgid:" + orgid + "& order date:"
        + orderDate);

    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      Date invoicedDate;

      invoicedDate = formatter.parse(orderDate);

      Organization org = OBDal.getInstance().get(Organization.class, orgid);
      DocumentType posDocType = OBDal.getInstance().get(DocumentType.class, arInvoiceDocType);
      invoice.setOrganization(org);
      invoice.setClient(OBContext.getOBContext().getCurrentClient());
      invoice.setUpdatedBy(OBContext.getOBContext().getUser());
      invoice.setCreatedBy(OBContext.getOBContext().getUser());
      invoice.setUpdated(new Date());
      invoice.setCreationDate(new Date());
      invoice.setActive(true);
      invoice.setSalesTransaction(true);
      invoice.setDocumentStatus("DR");
      invoice.setDocumentAction("CO");
      invoice.setProcessed(false);
      invoice.setDocumentType(posDocType);
      invoice.setTransactionDocument(posDocType);
      invoice.setInvoiceDate(invoicedDate);
      invoice.setOrderDate(invoicedDate);
      BusinessPartner customer = getBusinessPartner(orgid);// /*OBDal.getInstance().get(BusinessPartner.class,
      // "D13C9E46A41A46EC9E1C04B6DE847E0D");*/
      invoice.setBusinessPartner(customer);
      invoice.setPartnerAddress(customer.getBusinessPartnerLocationList().get(0));
      invoice.setDocumentNo(getDocumentNumber(arInvoiceDocType));
      invoice.setPaymentTerms(customer.getPaymentTerms());
      invoice.setCurrency(OBContext.getOBContext().getCurrentClient().getCurrency());
      invoice.setAccountingDate(invoicedDate);
      invoice.setPriceList(customer.getPriceList()); // OBDal.getInstance().get(PriceList.class,"2205CDAF5996448484851F4524B25EA2")
      //invoice.setSyncStoredept(OBDal.getInstance().get(CLStoreDept.class, storeDeptId));
      invoice.setSyncBrand(OBDal.getInstance().get(CLBrand.class, brandId));
      invoice.setPaymentMethod(customer.getPaymentMethod());
    } catch (ParseException e) {
      log4j.debug("@etlsync:Invoice Header Date parsing issue. " + e.getMessage());
      logger.log("@etlsync:Invoice Header Date parsing issue. " + e.getMessage());
      e.printStackTrace();
    } catch (OBException oe) {
      log4j.debug("@etlsync:Problem with Invoice Header Generation " + oe.getMessage());
      logger.log("@etlsync:Problem with Invoice Header Generation " + oe.getMessage());
      oe.printStackTrace();
    }

    return invoice;

  }

  public InvoiceLine createInvoiceLine(String orgid, Invoice invoice, String prodid,
      String invoicedQuantity, String lineNetAmount, String grossAmount, String tax) {

    Organization org = OBDal.getInstance().get(Organization.class, orgid);

    InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);

    log4j.debug("Entered into Invoice lines Creation with product id:" + prodid
        + " & organization id:" + orgid);
    try {

      invoiceLine.setOrganization(org);
      invoiceLine.setClient(OBContext.getOBContext().getCurrentClient());
      invoiceLine.setUpdatedBy(OBContext.getOBContext().getUser());
      invoiceLine.setCreatedBy(OBContext.getOBContext().getUser());
      invoiceLine.setUpdated(new Date());
      invoiceLine.setCreationDate(new Date());
      invoiceLine.setActive(true);
      invoiceLine.setInvoice(invoice);
      invoiceLine.setProduct(OBDal.getInstance().get(Product.class, prodid));
      invoiceLine.setInvoicedQuantity(new BigDecimal(invoicedQuantity));
      System.out
          .println("...InvoiceQuantity......." + invoiceLine.getInvoicedQuantity().toString());
      invoiceLine.setLineNetAmount(new BigDecimal(lineNetAmount));
      invoiceLine.setGrossAmount(new BigDecimal(grossAmount));
      invoiceLine.setUnitPrice(new BigDecimal(lineNetAmount).divide(
          new BigDecimal(invoicedQuantity), 2, RoundingMode.HALF_UP));
      invoiceLine.setGrossUnitPrice(new BigDecimal(grossAmount).divide(new BigDecimal(
          invoicedQuantity), 2, RoundingMode.HALF_UP));
      invoiceLine.setTax(OBDal.getInstance().get(TaxRate.class, tax));
      invoiceLine.setLineNo(linenum);
      invoiceLine.setUOM(OBDal.getInstance().get(UOM.class, "100"));
      linenum += 10;

    } catch (OBException oe) {
      log4j.debug("@etlsync:Problem with Invoice line Generation " + oe.getMessage());
      logger.log("@etlsync:Problem with Invoice line Generation " + oe.getMessage());
      oe.printStackTrace();
    }

    return invoiceLine;

  }

  private BusinessPartner getBusinessPartner(String org) {

    Organization organization = OBDal.getInstance().get(Organization.class, org);
    BusinessPartner bp = organization.getOrganizationInformationList().get(0)
        .getDsidefPosinvoicebp();

    return bp;
  }

  private String getDocumentNumber(String doctypeid) {
    String posDocTypeSequenceNum = "";
    try {
      DocumentType posDocType = OBDal.getInstance().get(DocumentType.class, doctypeid);
      posDocTypeSequenceNum = posDocType.getDocumentSequence().getNextAssignedNumber().toString();
      posDocType.getDocumentSequence().setNextAssignedNumber(
          (Long.valueOf(posDocTypeSequenceNum).longValue() + 1));
      OBDal.getInstance().save(posDocType);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (NumberFormatException e) {
      log4j.debug("@etlsync:problem while generating the document number for doctypeid:"
          + doctypeid);
      logger
          .log("@etlsync:problem while generating the document number for doctypeid:" + doctypeid);
      e.printStackTrace();
    }
    return posDocTypeSequenceNum;

  }

  private String completeInvoice(Invoice currentInvoice) {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(null);
    parameters.add(currentInvoice.getId());

    String resp = "";
    try {
      resp = CallStoredProcedure.getInstance().call("c_invoice_post", parameters, null).toString();
      log4j.info("@etlsync:Procedure called successfully for the C_invoice ID = "
          + currentInvoice.getId() + "& Document No:" + currentInvoice.getDocumentNo());
    } catch (Exception e) {

      log4j.error("@etlsync: Posting could not be done for:" + currentInvoice.getId()
          + " & Document No:" + currentInvoice.getDocumentNo());
      throw new OBException(e.getMessage());

    }

    return "1";
  }

  @SuppressWarnings("deprecation")
  public void generatePOSInvoices(String orgid, String date) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    Date closedate = null;

    if (date != null) {
      try {
        closedate = sdf.parse(date);
      } catch (ParseException e) {
        String message = "The order date entered is not valid. Use format yyyy-MM-DD";
        log4j.error(message);
        throw new OBException(message);
      }
    }

    OBCriteria<StoreStatus> storestatus = OBDal.getInstance().createCriteria(StoreStatus.class);
    // It loads the items correctly - eagerly
    storestatus.setFetchMode("organization", FetchMode.EAGER);
    storestatus.add(Restrictions.eq(StoreStatus.PROPERTY_STORESTATUS, "Y"));

    Organization orgPrev;
    if (orgid != null) {
      orgPrev = OBDal.getInstance().get(Organization.class, orgid);
      if (orgPrev == null) {
        String message = "@etlSync:Invalid Organizaiton : " + orgid;
        log4j.error(message);
        throw new OBException(message);
      }
      storestatus.add(Restrictions.eq(StoreStatus.PROPERTY_ORGANIZATION, orgPrev));
    }
    if (date != null) {
      storestatus.add(Restrictions.eq(StoreStatus.PROPERTY_CLOSEDDATE, closedate));
    }

    if (storestatus.list().size() == 0) {
      String message = "@etlSync:No stores to process";
      log4j.info(message);
      throw new OBException(message);
    } else {
      for (StoreStatus st : storestatus.list()) {
        String message = "@etlSync:Setting the store processed for organization:"
            + st.getOrganization() + " & closed date:" + st.getClosedDate();
        createPosInvoiceProcess(st.getOrganization(), st.getClosedDate());
        log4j.info(message);
        st.setStoreStatus("P");
        OBDal.getInstance().save(st);
        OBDal.getInstance().commitAndClose();
      }
    }
  }
}