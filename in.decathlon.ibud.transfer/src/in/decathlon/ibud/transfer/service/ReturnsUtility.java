package in.decathlon.ibud.transfer.service;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.warehouse.pickinglist.actionhandler.RaiseIncidenceHandler;

public class ReturnsUtility {

  public Order createRFCOrder(JSONObject rtvsheaderjson) throws Exception {
    org.openbravo.model.common.enterprise.Organization org = BusinessEntityMapper
        .getOrgOfBP(rtvsheaderjson.getString(SOConstants.jsonBusinessPartner));

    BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(rtvsheaderjson
        .getString(SOConstants.jsonOrganization));

    Location location = bPartner.getBusinessPartnerLocationList().get(0);

    Order rfcOrd = OBProvider.getInstance().get(Order.class);

    rfcOrd.setCreatedBy(OBContext.getOBContext().getUser());
    rfcOrd.setOrganization(org);
    rfcOrd.setClient(OBContext.getOBContext().getCurrentClient());
    rfcOrd.setCreationDate(new Date());
    rfcOrd.setUpdatedBy(OBContext.getOBContext().getUser());
    rfcOrd.setUpdated(new Date());
    rfcOrd.setSalesTransaction(false);
    rfcOrd.setDocumentStatus(SOConstants.DraftDocumentStatus);
    FIN_PaymentMethod paymentMethod = getPaymentMethod(SOConstants.PaymentMethod);
    rfcOrd.setPaymentMethod(paymentMethod);
    PaymentTerm paymentTerm = getPaymentTerm(SOConstants.PaymentTerm);
    rfcOrd.setPaymentTerms(paymentTerm);
    rfcOrd.setBusinessPartner(bPartner);
    rfcOrd.setPartnerAddress(location);
    rfcOrd.setInvoiceAddress(location);
    String returnDocument = rtvsheaderjson.getString(SOConstants.jsonDocNo);
    int lastIndexOf = returnDocument.lastIndexOf("*");
    String documentNo = returnDocument.substring(0, lastIndexOf);
    rfcOrd.setWarehouse(SaveTransferRecord.getWarehouse(documentNo, org));
    rfcOrd.setOrderDate(new Date());
    rfcOrd.setScheduledDeliveryDate(new Date());
    rfcOrd.setProcessed(false);
    rfcOrd.setSwIsautoOrder(true);
    PriceList pricelist = BusinessEntityMapper.getPriceList(SOConstants.POPriceList);
    rfcOrd.setPriceList(pricelist);
    rfcOrd.setAccountingDate(new Date());
    rfcOrd.setSalesTransaction(true);
    rfcOrd.setCurrency(OBContext.getOBContext().getCurrentClient().getCurrency());
    rfcOrd.setDocumentType(BusinessEntityMapper.getDocType("SOO", true));
    rfcOrd.setTransactionDocument(BusinessEntityMapper.getDocType("SOO", true));
    rfcOrd.setDocumentNo(rtvsheaderjson.getString(SOConstants.jsonDocNo));
    rfcOrd.setIbodtrIsCreatedbyidl(rtvsheaderjson.getString("ibodtrIsCreatedbyidl")!=null && rtvsheaderjson.getString("ibodtrIsCreatedbyidl").equals("Y"));
    OBDal.getInstance().save(rfcOrd);
    return rfcOrd;
  }

  private Warehouse getWarehouse(Organization org) throws Exception {
    try {
      OBContext.setAdminMode(true);
      String qry = "id in (select id from Warehouse wh where wh.ibdoWarehousetype='CAR' and wh.organization.id='"
          + org.getId() + "')";
      OBQuery<Warehouse> strQry = OBDal.getInstance().createQuery(Warehouse.class, qry);
      strQry.setMaxResult(1);
      List<Warehouse> warehouseList = strQry.list();
      if (warehouseList == null || warehouseList.isEmpty()) {
        throw new OBException("There is no warehouse for org " + org);
      }
      Warehouse warehouse = warehouseList.get(0);
      Locator returnLoc = warehouse.getReturnlocator();
      if (returnLoc == null)
        throw new OBException("There is no return bin  for warehouse " + warehouse);

      return returnLoc.getWarehouse();
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public List<OrderLine> createRFCOrderLines(JSONArray rtvsLinesJson, Order rfc) throws Exception {
    Organization org = rfc.getOrganization();
    List<OrderLine> ollList = new ArrayList<OrderLine>();
    long lineNo = 0;
    for (int i = 0; i < rtvsLinesJson.length(); i++) {
      lineNo = lineNo + 10;
      JSONObject obj = rtvsLinesJson.getJSONObject(i);
      OrderLine line = OBProvider.getInstance().get(OrderLine.class);
      Product prd = OBDal.getInstance().get(Product.class, obj.getString(SOConstants.jsonProduct));

      line.setCreatedBy(rfc.getCreatedBy());
      line.setOrganization(org);
      line.setClient(rfc.getClient());
      line.setCreationDate(new Date());
      line.setUpdatedBy(rfc.getUpdatedBy());
      line.setUpdated(new Date());
      line.setActive(true);
      line.setOrderDate(new Date());
      line.setLineNo(lineNo);
      line.setUOM(prd.getUOM());
      line.setProduct(prd);
      line.setCurrency(prd.getClient().getCurrency());
      line.setSalesOrder(rfc);
      line.setPartnerAddress(null);
      line.setWarehouse(rfc.getWarehouse());
      line.setOrderedQuantity(new BigDecimal(obj.getString(SOConstants.jsonMovementQuantity)));
      line.setDeliveredQuantity(BigDecimal.ZERO);
      line.setReservedQuantity(BigDecimal.ZERO);
      line.setInvoicedQuantity(BigDecimal.ZERO);
      line.setListPrice(BigDecimal.ZERO);
      line.setUnitPrice(BigDecimal.ZERO);
      line.setTax(getTaxRate(prd));
      line.setDirectShipment(false);
      line.setFreightAmount(BigDecimal.ZERO);
      line.setLineNetAmount(BigDecimal.ZERO);
      line.setLineGrossAmount(BigDecimal.ZERO);
      line.setPriceLimit(BigDecimal.ZERO);
      line.setStandardPrice(BigDecimal.ZERO);
      line.setGrossListPrice(BigDecimal.ZERO);
      line.setDescriptionOnly(false);
      line.setGrossUnitPrice(BigDecimal.ZERO);
      line.setDSCessionPrice(BigDecimal.ZERO);

      ollList.add(line);
    }

    return ollList;
  }

  public ShipmentInOut createRMSShipment(JSONObject rtvsheaderjson, Order rfc) throws Exception {

    Location location = rfc.getBusinessPartner().getBusinessPartnerLocationList().get(0);
    DocumentType docType = BusinessEntityMapper.getDocType("MMS", true);

    ShipmentInOut shipmentHeader = OBProvider.getInstance().get(ShipmentInOut.class);
    shipmentHeader.setClient(rfc.getClient());
    shipmentHeader.setOrganization(rfc.getOrganization());
    shipmentHeader.setActive(true);
    shipmentHeader.setCreationDate(new Date());
    shipmentHeader.setCreatedBy(rfc.getCreatedBy());
    shipmentHeader.setUpdatedBy(rfc.getUpdatedBy());
    shipmentHeader.setUpdated(new Date());
    shipmentHeader.setBusinessPartner(rfc.getBusinessPartner());
    shipmentHeader.setPartnerAddress(rfc.getBusinessPartner().getBusinessPartnerLocationList()
        .get(0));
    shipmentHeader.setMovementDate(new Date());
    shipmentHeader.setDocumentStatus(SOConstants.DraftDocumentStatus);
    shipmentHeader.setProcessed(false);
    shipmentHeader.setPrint(false);

    shipmentHeader.setSWMovement(SOConstants.manualReturnDocType);
    shipmentHeader.setIbodtrVaidate(true);
    shipmentHeader.setMovementType("C-");
    shipmentHeader.setDocumentType(docType);
    shipmentHeader.setDocumentNo(rfc.getDocumentNo());
    shipmentHeader.setWarehouse(rfc.getWarehouse());
    shipmentHeader.setProcessGoodsJava(SOConstants.CompleteDocumentStatus);
    shipmentHeader.setAccountingDate(new Date());
    shipmentHeader.setIbodtrVaidate(true);
    shipmentHeader.setSalesTransaction(true);
    OBDal.getInstance().save(shipmentHeader);
    return shipmentHeader;
  }

  public List<ShipmentInOutLine> getRMSLines(List<OrderLine> rfcLines, ShipmentInOut rms,
      boolean flag) throws Exception {
    List<ShipmentInOutLine> shipLines = new ArrayList<ShipmentInOutLine>();
    long lineNo = 0;

    for (int i = 0; i < rfcLines.size(); i++) {
      OrderLine ol = rfcLines.get(i);
      lineNo = lineNo + 10;

      ShipmentInOutLine iol = OBProvider.getInstance().get(ShipmentInOutLine.class);
      iol.setLineNo(lineNo);
      iol.setClient(rms.getClient());
      iol.setOrganization(rms.getOrganization());
      iol.setActive(true);
      iol.setCreationDate(new Date());
      iol.setCreatedBy(rms.getCreatedBy());
      iol.setUpdatedBy(rms.getCreatedBy());
      iol.setStorageBin(rms.getWarehouse().getReturnlocator());
      iol.setUpdated(new Date());
      iol.setMovementQuantity(ol.getOrderedQuantity());
      iol.setShipmentReceipt(rms);
      iol.setProduct(ol.getProduct());
      iol.setUOM(ol.getProduct().getUOM());

      iol.setIbodtrActmovementqty(ol.getOrderedQuantity());
      SessionHandler.getInstance().commitAndStart();
      shipLines.add(iol);

    }
    return shipLines;
  }

  private FIN_PaymentMethod getPaymentMethod(String name) {
    OBCriteria<FIN_PaymentMethod> paymentCrit = OBDal.getInstance().createCriteria(
        FIN_PaymentMethod.class);
    paymentCrit.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_NAME, name));
    List<FIN_PaymentMethod> finPayCritList = paymentCrit.list();
    if (finPayCritList != null && finPayCritList.size() > 0) {
      return finPayCritList.get(0);
    } else {
      throw new OBException("payment method not found");
    }
  }

  private PaymentTerm getPaymentTerm(String name) {
    OBCriteria<PaymentTerm> paymentTermCrit = OBDal.getInstance().createCriteria(PaymentTerm.class);
    paymentTermCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
    List<PaymentTerm> finPayTermCritList = paymentTermCrit.list();
    if (finPayTermCritList != null && finPayTermCritList.size() > 0) {
      return finPayTermCritList.get(0);
    } else {
      throw new OBException("payment term not found");
    }
  }

  public TaxRate getTaxRate(Product product) {
    List<TaxRate> taxRateList = product.getTaxCategory().getFinancialMgmtTaxRateList();
    if (taxRateList == null || taxRateList.size() <= 0) {
      throw new OBException("specify tax rate to product " + product);
    } else
      return taxRateList.get(0);
  }
}
