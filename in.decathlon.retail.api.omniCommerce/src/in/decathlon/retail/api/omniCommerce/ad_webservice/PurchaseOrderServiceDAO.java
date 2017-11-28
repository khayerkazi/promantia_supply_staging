package in.decathlon.retail.api.omniCommerce.ad_webservice;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;
import in.nous.searchitem.util.SearchItemRequisitionUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import org.hibernate.exception.ConstraintViolationException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.service.db.CallStoredProcedure;

import com.sysfore.catalog.CLBrand;

public class PurchaseOrderServiceDAO {

  // Properties Singleton class instantiation
  final Properties p = SearchItemRequisitionUtil.getInstance().getProperties();

  private static Logger log = Logger.getLogger(PurchaseOrderServiceDAO.class);

  private static Connection conn = null;

  private static ResourceBundle properties = null;

  private static String successStatus = "successStatus";

  private static String success = "success";

  private static String failure = "failure";

  private static String Y = "Y";

  private static String N = "N";

  private static String poDesc = "PO creation through Web Service";

  private static String poLineDesc = "PO line creation through Web Service";

  /**
   * Constructor used to fetch database properties
   * 
   * @throws Exception
   */
  public PurchaseOrderServiceDAO() throws Exception {
    OBDal obDal = new OBDal();
    conn = obDal.getConnection();
    conn.setAutoCommit(false);
  }

  public OrderDTO createPONew(OrderDTO purchaseOrder) {
    /*
     * System.out.println(purchaseOrder.getStoreName());
     * System.out.println(purchaseOrder.getAddressId());
     * System.out.println(purchaseOrder.getDecathlonId());
     * System.out.println(purchaseOrder.getBrand()); System.out.println(purchaseOrder.getEmail());
     */

    try {
      PreparedStatement pstOrder = null;
      final String itemCode = purchaseOrder.getListOrderlineDTOs().get(0).getItemCode();
      long lineNo = 10L;
      // Organization based on purcahse order store name
      final OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(
          Organization.class);
      orgCriteria.add(Expression.eq(Organization.PROPERTY_NAME, purchaseOrder.getStoreName()));
      Organization storeOrganization = null;
      if (!orgCriteria.list().isEmpty()) {
        storeOrganization = orgCriteria.list().get(0);
      } else {
        throw new Exception("Organization with storename '" + purchaseOrder.getStoreName()
            + "' does not exists");
      }
      // User based on purcahse order email
      final OBCriteria<User> userCriteria = OBDal.getInstance().createCriteria(User.class);
      userCriteria.add(Expression.eq(User.PROPERTY_EMAIL, purchaseOrder.getEmail()));
      User adUser = null;
      if (!userCriteria.list().isEmpty()) {
        adUser = userCriteria.list().get(0);
      } else {
        throw new Exception("User with email '" + purchaseOrder.getEmail() + "' does not exists");
      }
      // PriceList based on purcahse order store name
      final OBCriteria<PriceList> priceCriteria = OBDal.getInstance().createCriteria(
          PriceList.class);
      priceCriteria.add(Expression.eq(PriceList.PROPERTY_ORGANIZATION, storeOrganization));
      PriceList orgPriceList = null;
      if (!priceCriteria.list().isEmpty()) {
        orgPriceList = priceCriteria.list().get(0);
      } else {
        throw new Exception("PriceList with StoreName '" + purchaseOrder.getStoreName()
            + "' does not exists");
      }

      // To create Manual Dc in Store Requisition on manual order creation
      OBQuery<CLBrand> clstoreQuery = null;
      CLBrand clstore = null;
      StringBuilder whereClause = new StringBuilder();
      whereClause.append("as cl where cl.name = '" + purchaseOrder.getBrand() + "'");
      clstoreQuery = OBDal.getInstance().createQuery(CLBrand.class, whereClause.toString());
      clstore = clstoreQuery.list().get(0);
      if (null == clstore) {
        whereClause = null;
        whereClause = new StringBuilder();
        whereClause
            .append("as cl where cl.id=(select storeDepartment from com.sysfore.catalog.CLModel where id=(select clModel from Product where name='"
                + itemCode + "'))");
        clstoreQuery = OBDal.getInstance().createQuery(CLBrand.class, whereClause.toString());
        clstore = clstoreQuery.list().get(0);
      }

      // Fetching the Organization specific information
      final OBQuery<Organization> obOrg = OBDal.getInstance().createQuery(Organization.class,
          "as org where org.id='" + storeOrganization.getId() + "'");
      final Organization orgSpecDets = obOrg.list().get(0);

      // Fetching document type by Organization
      DocumentType currDocumentType = null;
      /*
       * if (purchaseOrder.getOrderType().equals("OC")) { final OBCriteria<DocumentType>
       * docTypeByOrg = OBDal.getInstance().createCriteria( DocumentType.class);
       * docTypeByOrg.add(Expression.eq(DocumentType.PROPERTY_ORGANIZATION, storeOrganization));
       * docTypeByOrg.add(Expression.eq(DocumentType.PROPERTY_PRINTTEXT, "Omni Commerce - " +
       * purchaseOrder.getStoreName())); if (docTypeByOrg.count() > 0) { currDocumentType =
       * docTypeByOrg.list().get(0); } else {
       * purchaseOrder.setSuccessStatus("Configuration for document Type 'Omni Commerce - " +
       * purchaseOrder.getStoreName() + "' does not exist");
       * log.error("Configuration for document Type 'Omni Commerce - " +
       * purchaseOrder.getStoreName() + "' does not exist"); return purchaseOrder; } } else {
       */
      final OBCriteria<DocumentType> docTypeByOrg = OBDal.getInstance().createCriteria(
          DocumentType.class);
      docTypeByOrg.add(Expression.eq(DocumentType.PROPERTY_ORGANIZATION, storeOrganization));
      docTypeByOrg.add(Expression.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, "POO"));
      if (docTypeByOrg.count() > 0) {
        currDocumentType = docTypeByOrg.list().get(0);
      }
      // }
      // Fetching pricelist details by Organization
      PriceList currPriceList = null;
      final OBCriteria<PriceListVersion> priceListverByOrg = OBDal.getInstance().createCriteria(
          PriceListVersion.class);
      priceListverByOrg.add(Expression.eq(PriceList.PROPERTY_ORGANIZATION, storeOrganization));
      if (priceListverByOrg.count() > 0) {
        currPriceList = priceListverByOrg.list().get(0).getPriceList();
      }

      // Fetching Warehouse details by Organization
      Warehouse currWarehouse = null;
      final OBCriteria<Warehouse> warehouseByOrg = OBDal.getInstance().createCriteria(
          Warehouse.class);
      warehouseByOrg.add(Expression.eq(Warehouse.PROPERTY_ORGANIZATION, storeOrganization));
      warehouseByOrg.add(Expression.ilike(Warehouse.PROPERTY_NAME, "Saleable%"));
      if (warehouseByOrg.count() > 0) {
        currWarehouse = warehouseByOrg.list().get(0);
      }

      final Order newOrder = OBProvider.getInstance().get(Order.class);
      newOrder.setClient(OBDal.getInstance().get(Client.class,
          OBContext.getOBContext().getCurrentClient().getId()));
      newOrder.setOrganization(storeOrganization);
      newOrder.setActive(true);
      newOrder.setCreatedBy(adUser);
      newOrder.setUpdatedBy(adUser);
      newOrder.setSalesTransaction(false);
      // newOrder.setDocumentNo(p.getProperty("docNum"));
      newOrder.setDocumentStatus("DR");
      newOrder.setDocumentAction("CO");
      newOrder.setProcessNow(false);
      newOrder.setProcessed(false);
      newOrder.setDocumentType(currDocumentType);
      newOrder.setTransactionDocument(currDocumentType);
      newOrder.setDescription(poDesc);
      newOrder.setDelivered(false);
      newOrder.setReinvoice(false);
      newOrder.setPrint(false);
      newOrder.setSelected(false);
      newOrder.setOrderDate(new Timestamp(new Date().getTime()));
      newOrder.setScheduledDeliveryDate(new Timestamp(new Date().getTime()));
      newOrder.setAccountingDate(new Timestamp(new Date().getTime()));
      newOrder.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
          p.getProperty("businessPartner")));
      newOrder.setInvoiceAddress(OBDal.getInstance().get(Location.class,
          p.getProperty("businessPartnerLocation")));
      newOrder.setPartnerAddress(OBDal.getInstance().get(Location.class,
          p.getProperty("businessPartnerLocation")));
      newOrder.setPrintDiscount(false);
      newOrder.setFormOfPayment("P");
      newOrder.setPaymentTerms(OBDal.getInstance().get(PaymentTerm.class,
          p.getProperty("paymentTerm")));
      newOrder.setInvoiceTerms("D");
      newOrder.setDeliveryTerms("A");
      newOrder.setFreightCostRule("I");
      newOrder.setFreightAmount(BigDecimal.ZERO);
      newOrder.setDeliveryMethod("P");
      newOrder.setChargeAmount(BigDecimal.ZERO);
      newOrder.setPriority("5");
      newOrder.setSummedLineAmount(BigDecimal.ZERO);
      newOrder.setGrandTotalAmount(BigDecimal.ZERO);
      newOrder.setWarehouse(currWarehouse);
      newOrder.setPriceList(currPriceList);
      newOrder.setPriceIncludesTax(true);
      newOrder.setPosted("N");
      newOrder.setCopyFrom(false);
      newOrder.setSelfService(false);
      newOrder.setGenerateTemplate(false);
      newOrder.setCopyFromPO(false);
      newOrder.setPaymentMethod(OBDal.getInstance().get(FIN_PaymentMethod.class,
          p.getProperty("paymentMethod")));
      newOrder.setPickFromShipment(false);
      newOrder.setReceiveMaterials(false);
      newOrder.setCreateInvoice(false);
      newOrder.setAddOrphanLine(false);
      newOrder.setCalculatePromotions(false);
      newOrder.setDSTotalitemqty(0L);
      newOrder.setDSTotalpriceadj(0L);
      newOrder.setDSEMDsGrandTotalAmt(BigDecimal.ZERO);
      newOrder.setDSChargeAmt(BigDecimal.ZERO);
      // newOrder.setSwStatus("DR");
      newOrder.setCurrency(OBDal.getInstance().get(Currency.class, p.getProperty("currency")));
      newOrder.setSwIsautoOrder(false);
      newOrder.setClBrand(clstore);
      newOrder.setIbdoCreateso(true);
      // if OrderType is Omni Commerce
      if (purchaseOrder.getOrderType().equals("OC")) {
        newOrder.setNsiIsomniorder(true);
        newOrder.setRCOxylaneno(purchaseOrder.getDecathlonId());
        newOrder.setNsiShippingAddressid(purchaseOrder.getAddressId());
        // for document no
        Sequence omnidocSeq = getOmniCommerceSequence(storeOrganization);
        StringBuilder docNum = new StringBuilder();
        if (omnidocSeq.getPrefix() != null && omnidocSeq.getPrefix() != "") {
          docNum = docNum.append(omnidocSeq.getPrefix());
        }
        docNum = docNum.append(omnidocSeq.getNextAssignedNumber());
        if (omnidocSeq.getSuffix() != null && omnidocSeq.getSuffix() != "") {
          docNum = docNum.append(omnidocSeq.getSuffix());
        }
        // increment doc seq next no
        omnidocSeq.setNextAssignedNumber(omnidocSeq.getNextAssignedNumber() + 1);
        OBDal.getInstance().save(omnidocSeq);

        newOrder.setDocumentNo(docNum.toString());
      }
      // for POS Order No.
      if (!purchaseOrder.getPosOrderNumber().equals("")) {
        newOrder.setNsiOmniPoreference(purchaseOrder.getPosOrderNumber());
      }
      OBDal.getInstance().save(newOrder);
      OBDal.getInstance().flush();
      String poHeaderId = newOrder.getId();
      // OBDal.getInstance().commitAndClose();

      final List<BaseOBObject> oLines = new ArrayList<BaseOBObject>();
      // Insert Puchase Order Lines
      for (OrderlineDTO orderlineDTO : purchaseOrder.getListOrderlineDTOs()) {

        // Product based on purcahse order ItemCode
        final OBCriteria<Product> prodCriteria = OBDal.getInstance().createCriteria(Product.class);
        prodCriteria.add(Expression.eq(PriceList.PROPERTY_NAME, orderlineDTO.getItemCode()));
        final Product product = prodCriteria.list().get(0);

        String taxId = "";
        pstOrder = conn
            .prepareStatement("select c_tax.rate,c_tax.c_tax_id from c_tax,m_product where c_region_id in "
                + "(select l.c_region_id from ad_orginfo oi, c_location l where  oi.ad_org_id=? and oi.c_location_id=l.c_location_id) "
                + "and c_tax.c_taxcategory_id =m_product.c_taxcategory_id and m_product.m_product_id in (?) "
                + "union "
                + "select c_tax.rate,c_tax.c_tax_id from c_tax ,c_tax_zone,m_product where c_tax.c_tax_id = c_tax_zone.c_tax_id "
                + "and c_tax_zone.from_region_id in (select l.c_region_id from ad_orginfo oi, c_location l where oi.ad_org_id=? "
                + "and oi.c_location_id=l.c_location_id) and c_tax.c_taxcategory_id =m_product.c_taxcategory_id and m_product.m_product_id in (?)");
        pstOrder.setString(1, storeOrganization.getId());
        pstOrder.setString(2, product.getId());
        pstOrder.setString(3, storeOrganization.getId());
        pstOrder.setString(4, product.getId());

        ResultSet rsOrder = pstOrder.executeQuery();
        if (rsOrder.next()) {
          taxId = rsOrder.getString("c_tax_id");
        } else {
          return logError("Tax rate not available for itemCode: " + orderlineDTO.getItemCode(),
              purchaseOrder);
        }

        final OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
        newOrderLine.setClient(OBDal.getInstance().get(Client.class,
            OBContext.getOBContext().getCurrentClient().getId()));
        newOrderLine.setOrganization(storeOrganization);
        newOrderLine.setActive(true);
        newOrderLine.setCreatedBy(adUser);
        newOrderLine.setUpdatedBy(adUser);
        newOrderLine.setSalesOrder(newOrder);
        newOrderLine.setLineNo(lineNo);
        newOrderLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
            p.getProperty("businessPartner")));
        newOrderLine.setPartnerAddress(OBDal.getInstance().get(Location.class,
            p.getProperty("businessPartnerLocation")));
        newOrderLine.setOrderDate(new Timestamp(new Date().getTime()));
        newOrderLine.setDateDelivered(new Timestamp(new Date().getTime()));
        newOrderLine.setInvoiceDate(new Timestamp(new Date().getTime()));
        newOrderLine.setDescription(poLineDesc);
        newOrderLine.setProduct(product);
        newOrderLine.setWarehouse(currWarehouse);
        newOrderLine.setDirectShipment(false);
        newOrderLine.setUOM(OBDal.getInstance().get(UOM.class, p.getProperty("uom")));
        newOrderLine.setOrderedQuantity(new BigDecimal(orderlineDTO.getQtyOrdered()));
        newOrderLine.setReservedQuantity(BigDecimal.ZERO);
        newOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
        newOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
        newOrderLine.setListPrice(BigDecimal.ZERO);
        newOrderLine.setPriceLimit(BigDecimal.ZERO);
        newOrderLine.setUnitPrice(BigDecimal.ZERO);
        newOrderLine.setLineNetAmount(BigDecimal.ZERO);
        newOrderLine.setFreightAmount(BigDecimal.ZERO);
        newOrderLine.setChargeAmount(BigDecimal.ZERO);
        newOrderLine.setTax(OBDal.getInstance().get(TaxRate.class, taxId));
        newOrderLine.setDescriptionOnly(false);
        newOrderLine.setStandardPrice(BigDecimal.ZERO);
        newOrderLine.setCancelPriceAdjustment(false);
        newOrderLine.setEditLineAmount(false);
        newOrderLine.setTaxableAmount(BigDecimal.ZERO);
        newOrderLine.setGrossUnitPrice(BigDecimal.ZERO);
        newOrderLine.setLineGrossAmount(BigDecimal.ZERO);
        newOrderLine.setGrossListPrice(BigDecimal.ZERO);
        newOrderLine.setExplode(false);
        newOrderLine.setDSTaxamount(BigDecimal.ZERO);
        newOrderLine.setDSEMDSUnitQty(BigDecimal.ZERO);
        newOrderLine.setDSEMDsCcunitprice(new BigDecimal(orderlineDTO.getPriceActual()));
        newOrderLine.setDSEMDsMrpprice((long) orderlineDTO.getPriceActual());
        // newOrderLine.setDsLinenetamt(0L);
        newOrderLine.setSWEMSwOrderqty(new BigDecimal(orderlineDTO.getQtyOrdered()));
        newOrderLine.setSWEMSwVolpcb(new BigDecimal(1));
        newOrderLine.setSWEMSwNtwtpcb(BigDecimal.ZERO);
        newOrderLine.setSWEMSwGrwtpcb(BigDecimal.ZERO);
        newOrderLine.setSWEMSwNoofparcel(new BigDecimal(orderlineDTO.getQtyOrdered()));
        newOrderLine.setSWEMSwItemcode(orderlineDTO.getItemCode());
        newOrderLine.setCLModelName(product.getClModelname());
        newOrderLine.setCLColor(product.getClColor());
        newOrderLine.setCLSize(product.getClSize());
        newOrderLine.setSwConfirmedqty(0L);
        newOrderLine
            .setCurrency(OBDal.getInstance().get(Currency.class, p.getProperty("currency")));
        try {
          oLines.add(newOrderLine);
          OBDal.getInstance().save(newOrderLine);
          OBDal.getInstance().flush();
        } catch (ConstraintViolationException e1) {
          e1.printStackTrace();
        }
        lineNo = lineNo + 10;
      }
      OBDal.getInstance().commitAndClose();
      // Calling Stored Procedure ecom_order_post3
      final List parameters = new ArrayList();
      parameters.add(null);
      parameters.add(poHeaderId);
      // final String procedureName = "c_order_post1";
      // if((CallStoredProcedure.getInstance().call(procedureName, parameters, null)).equals("1")) {
      // log.info("Procedure called successfully for the PO "+poHeaderId);
      // }
      // CallStoredProcedure.getInstance().call(procedureName, parameters, null);
      // if((CallStoredProcedure.getInstance().call(procedureName, parameters, null)).equals("1")) {
      // System.out.println("Procedure called successfully for the PO "+poHeaderId);
      // }

      Order newPoHeader = OBDal.getInstance().get(Order.class, poHeaderId);
      /*
       * try { final SendPOProcess sp = new SendPOProcess(); final DataToJsonConverter toJsonObject
       * = OBProvider.getInstance().get(DataToJsonConverter.class); final JSONObject poHeader =
       * toJsonObject.toJsonObject(OBDal.getInstance().get(Order.class, poHeaderId),
       * DataResolvingMode.FULL); final List<JSONObject> poLines =
       * toJsonObject.toJsonObjects(oLines); final JSONArray jsonpoLines = new JSONArray(poLines);
       * System.out.println("Header = "+poHeader); System.out.println("Lines = "+jsonpoLines); final
       * StringBuilder jsonInput = new StringBuilder(); jsonInput.append("{\"data\":[{\"Header\":");
       * jsonInput.append(poHeader.toString()+",\"Lines\":"+jsonpoLines.toString());
       * jsonInput.append("}]}");
       * 
       * JSONObject docNos = sp.send(jsonInput.toString());
       * System.out.println("response from supply" + docNos);
       * 
       * } catch (Exception e) { e.printStackTrace(); }
       */
      // Sequence docSeq = getDocSequence(storeOrganization);
      Sequence docSeq = getStoreRequisitionSequence(storeOrganization);
      ImmediateSOonPO soOnPO = new ImmediateSOonPO();
      ArrayList<String> pOids = new ArrayList<String>();
      Order orderHeader = OBDal.getInstance().get(Order.class, poHeaderId);
      if (orderHeader != null) {
        if (orderHeader.getOrderLineList().size() > 0) {
          if (purchaseOrder.getOrderType().equals("OC")) {
            docSeq = getOmniCommerceSequence(storeOrganization);
            processOrder(docSeq, soOnPO, pOids, orderHeader);
          } else
            processOrder(docSeq, soOnPO, pOids, orderHeader);
        }
      }

      Long confirmedQty = 0L;
      final StringBuilder linesInfo = new StringBuilder();
      // Insert Puchase Order Lines
      for (OrderlineDTO orderlineDTO : purchaseOrder.getListOrderlineDTOs()) {
        // Product based on purcahse order ItemCode
        final OBCriteria<Product> prodCriteria = OBDal.getInstance().createCriteria(Product.class);
        prodCriteria.add(Expression.eq(PriceList.PROPERTY_NAME, orderlineDTO.getItemCode()));
        final Product product = prodCriteria.list().get(0);
        final OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(
            OrderLine.class,
            "as ol where ol.salesOrder.id='" + poHeaderId + "' and ol.product='" + product.getId()
                + "'");
        orderLine.setFilterOnActive(false);
        for (OrderLine ol : orderLine.list()) {
          linesInfo.append(product.getName() + "-" + orderlineDTO.getQtyOrdered() + "-"
              + ol.getSwConfirmedqty() + ",");
          confirmedQty += ol.getSwConfirmedqty();
          purchaseOrder.getListOrderlineDTOs().get(0)
              .setConfirmedQty(Integer.parseInt(ol.getSwConfirmedqty().toString()));
        }
      }
      linesInfo.deleteCharAt(linesInfo.length() - 1);
      purchaseOrder.setLinesInfo(linesInfo.toString());
      purchaseOrder.setConfirmedQty(confirmedQty.toString());
      purchaseOrder.setCOrderId(poHeaderId);
      purchaseOrder.setDocumentNo(newPoHeader.getDocumentNo());
      purchaseOrder.setSuccessStatus(success);
      return purchaseOrder;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private Sequence getStoreRequisitionSequence(Organization storeOrganization) {

    Sequence sequence = null;
    final OBCriteria<Sequence> sequenceCriteria = OBDal.getInstance()
        .createCriteria(Sequence.class);
    sequenceCriteria.add(Expression.eq(Sequence.PROPERTY_ORGANIZATION, storeOrganization));
    sequenceCriteria.add(Expression.like(Sequence.PROPERTY_NAME, "%Store Requisition"));
    sequenceCriteria.setMaxResults(1);
    if (sequenceCriteria.count() == 1) {
      sequence = sequenceCriteria.list().get(0);
    }
    return sequence;
  }

  private Sequence getOmniCommerceSequence(Organization storeOrganization) {

    Sequence sequence = null;
    final OBCriteria<Sequence> sequenceCriteria = OBDal.getInstance()
        .createCriteria(Sequence.class);
    sequenceCriteria.add(Expression.eq(Sequence.PROPERTY_ORGANIZATION, storeOrganization));
    sequenceCriteria.add(Expression.like(Sequence.PROPERTY_NAME, "Omni Commerce - %"));
    sequenceCriteria.setMaxResults(1);
    if (sequenceCriteria.count() == 1) {
      sequence = sequenceCriteria.list().get(0);
    }
    return sequence;
  }

  public static Sequence getDocSequence(Organization org) {
    return BusinessEntityMapper.getDocumentSequence(org);

  }

  private static void processOrder(Sequence docSeq, ImmediateSOonPO soOnPO,
      ArrayList<String> pOids, Order orderHeader) throws Exception {
    // Long curDocNo = docSeq.getNextAssignedNumber();
    // docSeq.setNextAssignedNumber(curDocNo + docSeq.getIncrementBy());
    // OBDal.getInstance().save(docSeq);
    // orderHeader.setDocumentNo(docSeq.getPrefix() + curDocNo.toString());
    // OBDal.getInstance().save(orderHeader);
    log.info(",, " + SOConstants.performanceTest + " before process ,," + new Date());
    soOnPO.processRequest(orderHeader);
    log.info(",, " + SOConstants.performanceTest + " after process ,," + new Date());
    pOids.add(orderHeader.getId());
  }

  /**
   * This method creates purchase order and purchase order lines by inserting the data into c_order
   * and c_orderline tables
   * 
   * @param purchaseOrder
   * @return orderDTO
   */
  public OrderDTO createPO(OrderDTO purchaseOrder) {
    String sqlCOrder = "INSERT INTO c_order("
        + "c_order_id, ad_client_id, ad_org_id, isactive, createdby,"
        + "updatedby, issotrx, documentno, docstatus, docaction,"
        + "processing, processed, c_doctype_id, c_doctypetarget_id, description, "
        + "isdelivered, isinvoiced, isprinted, isselected, "
        + "dateordered, datepromised,  dateacct, c_bpartner_id,"
        + "billto_id, c_bpartner_location_id, isdiscountprinted,"
        + "paymentrule, c_paymentterm_id, invoicerule, deliveryrule,"
        + "freightcostrule, freightamt, deliveryviarule,"
        + "chargeamt, priorityrule, totallines, grandtotal, m_warehouse_id,"
        + "m_pricelist_id, istaxincluded, " + "posted,  copyfrom," + "isselfservice,"
        + "generatetemplate, " + "copyfrompo, fin_paymentmethod_id,"
        + "rm_pickfromshipment, rm_receivematerials, rm_createinvoice,"
        + " rm_addorphanline,  calculate_promotions," + "convertquotation,"
        + " em_ds_totalitemqty, em_ds_totalpriceadj, " + " em_ds_grandtotalamt, "
        + "em_ds_chargeamt, "
        + "em_sw_postatus, c_currency_id,em_sw_isauto_order,em_cl_storedept_id)"

        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
        + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    String sqlCOrderLine = "INSERT INTO c_orderline("
        + "c_orderline_id, ad_client_id, ad_org_id, isactive, created, createdby,"
        + "updated, updatedby, c_order_id, line, c_bpartner_id, c_bpartner_location_id,"
        + "dateordered, datepromised,  dateinvoiced, description, m_product_id, m_warehouse_id, "
        + "directship, c_uom_id, qtyordered, qtyreserved, qtydelivered, qtyinvoiced, "
        + " pricelist, priceactual, pricelimit, linenetamt,  freightamt,"
        + "chargeamt, c_tax_id, isdescription, pricestd, cancelpricead, iseditlinenetamt,"
        + "taxbaseamt,  gross_unit_price, line_gross_amount, grosspricestd, explode, em_ds_taxamount, "
        + "em_ds_unitqty,  em_ds_ccunitprice, em_ds_mrpprice, em_ds_linenetamt, "
        + "em_sw_orderqty, em_sw_volpcb, em_sw_ntwtpcb, em_sw_grwtpcb, em_sw_noofparcel,"
        + "em_sw_itemcode, em_cl_modelname, em_cl_color_id, em_cl_size, em_sw_confirmedqty,"
        + "c_currency_id)"

        + "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    PreparedStatement pstOrder = null, pstOrderLine = null;
    ResultSet rsOrder = null;
    String bpartnerLocationId = "";
    String cOrderID = "";
    String documentNo = "";
    String uomId = "100";
    String docTypeId = "";
    String paymenttermId = "";
    String paymentMethodId = "";
    // this is hard coded after staging issue reported
    String adClientId = "";
    String adOrgId = "";
    String cbpartnerId = "";
    String pricelistId = "";
    String pricelistVersionId = "";
    String warehouseId = "";
    String adUserId = "";
    String currencyId = "";
    int line = 10;
    String lineId = "";
    // To create Manual DC in StoreRequisition Window
    String itemCode = "";
    String deptName = "";

    try {

      pstOrder = conn.prepareStatement("select c_doctype_id from c_doctype where name=?");
      pstOrder.setString(1, "Purchase Order");
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        docTypeId = rsOrder.getString("c_doctype_id");
      } else {
        return logError("c_doctype_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement("select c_paymentterm_id from c_paymentterm where name=?");
      pstOrder.setString(1, "Immediate");
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        paymenttermId = rsOrder.getString("c_paymentterm_id");
      } else {
        return logError("c_paymentterm_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select fin_paymentmethod_id from fin_paymentmethod where name=?");
      pstOrder.setString(1, "Cash");
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        paymentMethodId = rsOrder.getString("fin_paymentmethod_id");
      } else {
        return logError("fin_paymentmethod_id could not be found.", purchaseOrder);
      }

      OBContext context = OBContext.getOBContext();
      Client client = context.getCurrentClient();
      adClientId = client.getId();

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement("select ad_org_id from ad_org where name=?");
      pstOrder.setString(1, purchaseOrder.getStoreName());
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        adOrgId = rsOrder.getString("ad_org_id");
      } else {
        return logError("ad_org_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select c_bpartner_id from c_bpartner where name = 'Whitefield Warehouse'");
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        cbpartnerId = rsOrder.getString("c_bpartner_id");
      } else {
        return logError("c_bpartner_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select c_bpartner_location_id from c_bpartner_location where c_bpartner_id=?");
      pstOrder.setString(1, cbpartnerId);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        bpartnerLocationId = rsOrder.getString("c_bpartner_location_id");
      } else {
        return logError("c_bpartner_location_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select m_pricelist_id from m_pricelist where ad_org_id =? order by created desc limit 1");
      pstOrder.setString(1, adOrgId);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        pricelistId = rsOrder.getString("m_pricelist_id");
      } else {
        return logError("m_pricelist_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select m_pricelist_version_id from m_pricelist_version where ad_org_id =? order by created desc limit 1");
      pstOrder.setString(1, adOrgId);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        pricelistVersionId = rsOrder.getString("m_pricelist_version_id");
      } else {
        return logError("m_pricelist_version_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement("select ad_user_id from ad_user where email =?");

      pstOrder.setString(1, purchaseOrder.getEmail());
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        adUserId = rsOrder.getString("ad_user_id");
      } else {
        return logError("ad_user_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select m_warehouse_id from m_warehouse where ad_org_id = "
              + "(select ad_org_id from ad_org where name =(select name from c_bpartner where c_bpartner_id=? )) and em_sw_iscar='Y'");
      pstOrder.setString(1, cbpartnerId);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        warehouseId = rsOrder.getString("m_warehouse_id");
      } else {
        return logError("m_warehouse_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement("select c_currency_id from c_currency where iso_code=?");
      pstOrder.setString(1, "INR");
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        currencyId = rsOrder.getString("c_currency_id");
      } else {
        return logError("c_currency_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement("select *  from ad_sequence_next('C_Order', ?)");
      pstOrder.setString(1, adClientId);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        cOrderID = rsOrder.getString("p_nextno");
      } else {
        return logError("p_nextno could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement("select * from ad_sequence_doc(?,?,?)");
      pstOrder.setString(1, "Purchase Order");
      pstOrder.setString(2, adClientId);
      pstOrder.setString(3, Y);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        documentNo = rsOrder.getString("p_documentno");
      }

      pstOrder = null;
      rsOrder = null;
      // To create Manual Dc in Store Requisition on manual order creation
      itemCode = purchaseOrder.getListOrderlineDTOs().get(0).getItemCode();
      pstOrder = conn
          .prepareStatement("select cl_storedept_id from cl_storedept where cl_storedept_id="
              + "(select cl_storedept_id from cl_model where cl_model_id="
              + "(select em_cl_model_id from m_product where name=?))");

      pstOrder.setString(1, itemCode);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        deptName = rsOrder.getString("cl_storedept_id");
      } else {
        return logError("cl_storedept_id could not be found.", purchaseOrder);
      }

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn.prepareStatement(sqlCOrder);
      pstOrder.setString(1, cOrderID);
      pstOrder.setString(2, adClientId);
      pstOrder.setString(3, adOrgId);
      pstOrder.setString(4, Y);
      pstOrder.setString(5, adUserId);
      pstOrder.setString(6, adUserId);
      pstOrder.setString(7, N);
      pstOrder.setString(8, documentNo);
      pstOrder.setString(9, "DR");
      pstOrder.setString(10, "CO");
      pstOrder.setString(11, N);
      pstOrder.setString(12, N);
      pstOrder.setString(13, docTypeId);
      pstOrder.setString(14, docTypeId);
      pstOrder.setString(15, poDesc);
      pstOrder.setString(16, N);
      pstOrder.setString(17, N);
      pstOrder.setString(18, N);
      pstOrder.setString(19, N);
      Timestamp currentTimeStamp = new Timestamp(new Date().getTime());
      pstOrder.setTimestamp(20, currentTimeStamp);
      pstOrder.setTimestamp(21, currentTimeStamp);
      pstOrder.setTimestamp(22, currentTimeStamp);
      pstOrder.setString(23, cbpartnerId);
      // billToId is same as bpartnerLocationId
      pstOrder.setString(24, bpartnerLocationId);
      pstOrder.setString(25, bpartnerLocationId);
      pstOrder.setString(26, N);
      pstOrder.setString(27, "P");
      pstOrder.setString(28, paymenttermId);
      pstOrder.setString(29, "D");
      pstOrder.setString(30, "A");
      pstOrder.setString(31, "I");
      pstOrder.setInt(32, 0);
      pstOrder.setString(33, "P");
      pstOrder.setInt(34, 0);
      pstOrder.setInt(35, 5);
      pstOrder.setInt(36, 0);
      pstOrder.setInt(37, 0);
      pstOrder.setString(38, warehouseId);
      pstOrder.setString(39, pricelistId);
      pstOrder.setString(40, Y);
      pstOrder.setString(41, N);
      pstOrder.setString(42, N);
      pstOrder.setString(43, N);
      pstOrder.setString(44, N);
      pstOrder.setString(45, N);
      pstOrder.setString(46, paymentMethodId);
      pstOrder.setString(47, N);
      pstOrder.setString(48, N);
      pstOrder.setString(49, N);
      pstOrder.setString(50, N);
      pstOrder.setString(51, N);
      pstOrder.setString(52, N);
      pstOrder.setInt(53, 0);
      pstOrder.setInt(54, 0);
      pstOrder.setInt(55, 0);// grand total
      pstOrder.setInt(56, 0);
      pstOrder.setString(57, "DR");
      pstOrder.setString(58, currencyId);
      pstOrder.setString(59, N);
      pstOrder.setString(60, deptName);
      pstOrder.executeUpdate();

      // Insert Puchase Order Lines
      for (OrderlineDTO orderlineDTO : purchaseOrder.getListOrderlineDTOs()) {
        double lineNetAmt = 0.0;
        double taxAmt = 0.0;
        double taxRate = 0;
        String modelName = "";
        String colorName = "";
        String size = "";
        String productId = "";
        String regionName = "";
        String taxId = "";

        lineNetAmt = 0;

        // TODO:change to name for m_product
        pstOrder = conn
            .prepareStatement("select em_cl_modelname,(select name from cl_color where cl_color_id =em_cl_color_id) "
                + "as color_name, em_cl_size from m_product where name=?");
        pstOrder.setString(1, orderlineDTO.getItemCode());
        rsOrder = pstOrder.executeQuery();
        if (rsOrder.next()) {
          modelName = rsOrder.getString("em_cl_modelname");
          colorName = rsOrder.getString("color_name");
          size = rsOrder.getString("em_cl_size");
        } else {
          return logError(
              "em_cl_modelname, color_name, em_cl_size could not be found for itemCode: "
                  + orderlineDTO.getItemCode(), purchaseOrder);
        }

        pstOrder = null;
        rsOrder = null;

        // TODO:change to name for m_product
        pstOrder = conn.prepareStatement("select m_product_id from m_product where name=?");
        pstOrder.setString(1, orderlineDTO.getItemCode());
        rsOrder = pstOrder.executeQuery();
        if (rsOrder.next()) {
          productId = rsOrder.getString("m_product_id");
        } else {
          return logError(
              "m_product_id could not be found for itemCode: " + orderlineDTO.getItemCode(),
              purchaseOrder);
        }

        pstOrder = null;
        rsOrder = null;

        // TODO:change to name from value
        pstOrder = conn
            .prepareStatement("select c_tax.rate,c_tax.c_tax_id from c_tax,m_product where c_region_id in "
                + "(select l.c_region_id from ad_orginfo oi, c_location l where  oi.ad_org_id=? and oi.c_location_id=l.c_location_id) "
                + "and c_tax.c_taxcategory_id =m_product.c_taxcategory_id and m_product.m_product_id in (?) "
                + "union "
                + "select c_tax.rate,c_tax.c_tax_id from c_tax ,c_tax_zone,m_product where c_tax.c_tax_id = c_tax_zone.c_tax_id "
                + "and c_tax_zone.from_region_id in (select l.c_region_id from ad_orginfo oi, c_location l where oi.ad_org_id=? "
                + "and oi.c_location_id=l.c_location_id) and c_tax.c_taxcategory_id =m_product.c_taxcategory_id and m_product.m_product_id in (?)");
        pstOrder.setString(1, adOrgId);
        pstOrder.setString(2, productId);
        pstOrder.setString(3, adOrgId);
        pstOrder.setString(4, productId);

        rsOrder = pstOrder.executeQuery();
        if (rsOrder.next()) {
          taxId = rsOrder.getString("c_tax_id");
          taxRate = Double.parseDouble(rsOrder.getString("rate"));
        } else {
          return logError("Tax rate not available for itemCode: " + orderlineDTO.getItemCode(),
              purchaseOrder);
        }

        rsOrder = null;

        taxAmt = 0;

        lineId = SequenceIdData.getUUID();
        pstOrderLine = conn.prepareStatement(sqlCOrderLine);
        pstOrderLine.setString(1, lineId);
        pstOrderLine.setString(2, adClientId);
        pstOrderLine.setString(3, adOrgId);
        pstOrderLine.setString(4, Y);
        pstOrderLine.setTimestamp(5, currentTimeStamp);
        pstOrderLine.setString(6, adUserId);
        pstOrderLine.setTimestamp(7, currentTimeStamp);
        pstOrderLine.setString(8, adUserId);
        pstOrderLine.setString(9, cOrderID);
        pstOrderLine.setInt(10, line);
        pstOrderLine.setString(11, cbpartnerId);
        pstOrderLine.setString(12, bpartnerLocationId);
        pstOrderLine.setTimestamp(13, currentTimeStamp);
        pstOrderLine.setTimestamp(14, currentTimeStamp);
        pstOrderLine.setTimestamp(15, currentTimeStamp);
        pstOrderLine.setString(16, poLineDesc);
        pstOrderLine.setString(17, productId);
        pstOrderLine.setString(18, warehouseId);
        pstOrderLine.setString(19, N);
        pstOrderLine.setString(20, uomId);
        pstOrderLine.setInt(21, orderlineDTO.getQtyOrdered());
        pstOrderLine.setInt(22, 0);
        pstOrderLine.setInt(23, 0);
        pstOrderLine.setInt(24, 0);
        pstOrderLine.setInt(25, 0);
        pstOrderLine.setDouble(26, 0);
        pstOrderLine.setInt(27, 0);
        pstOrderLine.setDouble(28, lineNetAmt);
        pstOrderLine.setDouble(29, 0.00);
        pstOrderLine.setDouble(30, 0.00);
        pstOrderLine.setString(31, taxId);
        pstOrderLine.setString(32, N);
        pstOrderLine.setDouble(33, 0.00);
        pstOrderLine.setString(34, N);
        pstOrderLine.setString(35, N);
        pstOrderLine.setInt(36, 0);
        pstOrderLine.setInt(37, 0);
        pstOrderLine.setInt(38, 0);
        pstOrderLine.setInt(39, 0);
        pstOrderLine.setString(40, N);
        pstOrderLine.setDouble(41, taxAmt);
        pstOrderLine.setInt(42, 0);
        pstOrderLine.setDouble(43, orderlineDTO.getPriceActual());
        pstOrderLine.setDouble(44, orderlineDTO.getPriceActual());
        pstOrderLine.setInt(45, 0);
        pstOrderLine.setInt(46, orderlineDTO.getQtyOrdered());
        pstOrderLine.setInt(47, 1);
        pstOrderLine.setInt(48, 0);
        pstOrderLine.setInt(49, 0);
        pstOrderLine.setInt(50, orderlineDTO.getQtyOrdered());
        pstOrderLine.setString(51, orderlineDTO.getItemCode());
        pstOrderLine.setString(52, modelName);
        pstOrderLine.setString(53, colorName);
        pstOrderLine.setString(54, size);
        pstOrderLine.setInt(55, 0);
        pstOrderLine.setString(56, currencyId);
        pstOrderLine.executeUpdate();

        line += 10;
      }

      conn.commit();
      pstOrder = null;
      rsOrder = null;
      int confirmed = 0;

      // TODO:change to name for m_product
      pstOrder = conn
          .prepareStatement("select coalesce(em_sw_confirmedqty,0) as confirmed from c_orderline where c_orderline_id=?");
      pstOrder.setString(1, lineId);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        confirmed = Integer.parseInt(rsOrder.getString("confirmed"));
      }
      purchaseOrder.getListOrderlineDTOs().get(0).setConfirmedQty(confirmed);

      // call the post procedure
      final List<Object> param = new ArrayList<Object>();

      param.add(null);
      param.add(cOrderID);
      CallStoredProcedure.getInstance().call("C_ORDER_POST1", param, null, true, false);

      pstOrder = null;
      rsOrder = null;

      String confirmedQty = null;
      pstOrder = conn
          .prepareStatement("select coalesce(sum(col.em_sw_confirmedqty),0) from c_order co "
              + "join c_orderline col on col.c_order_id = co.c_order_id "
              + "where co.documentno like ( select replace(replace(documentno,'*CAR',''),'*CAC','')||'%' from c_order where c_order_id=? and issotrx='N') "
              + "and co.issotrx='N'");
      pstOrder.setString(1, cOrderID);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        confirmedQty = rsOrder.getString("coalesce");
      }
      purchaseOrder.setConfirmedQty(confirmedQty);
      log.debug("confirmed quantity-->" + purchaseOrder.getConfirmedQty());

      pstOrder = null;
      rsOrder = null;

      pstOrder = conn
          .prepareStatement("select replace(replace(documentno,'*CAR',''),'*CAC','') from c_order where c_order_id=? and issotrx='N'");
      pstOrder.setString(1, cOrderID);
      rsOrder = pstOrder.executeQuery();
      if (rsOrder.next()) {
        documentNo = rsOrder.getString("replace");
      }

    } catch (Exception e) {
      log.error("An error occurred while creating Purchase Order", e);
      purchaseOrder.setSuccessStatus(e.getMessage());
      return purchaseOrder;
    } finally {
      pstOrder = null;
      pstOrderLine = null;
      rsOrder = null;
      closeConnection();
    }

    purchaseOrder.setCOrderId(cOrderID);
    purchaseOrder.setDocumentNo(documentNo);
    purchaseOrder.setSuccessStatus(success);
    return purchaseOrder;

  }

  /**
   * This method is used to close the connection with database
   * 
   * @throws SQLException
   */
  public void closeConnection() {
    try {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    } catch (SQLException e) {
      log.error("An error occurred in closeConnection of PurchaseOrderServiceDAO: ", e);
    }
  }

  /**
   * This method logs the given error message and returns an error status
   * 
   * @param errorMessage
   * @param purchaseOrder
   * @return purchaseOrder
   */
  private static OrderDTO logError(String errorMessage, OrderDTO purchaseOrder) {
    log.error("An error occurred in PurchaseOrderServiceDAO: " + errorMessage);
    purchaseOrder.setSuccessStatus(errorMessage);
    return purchaseOrder;
  }
}
