package in.decathlon.ibud.orders.client;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.decathlon.warehouserules.DWHR_DistributeMonitor;
import org.openbravo.decathlon.warehouserules.DwhrWhruleConfig;
import org.openbravo.decathlon.warehouserules.WarehouseRuleImplementation;
import org.openbravo.decathlon.warehouserules.WarehouseRuleImplementation.SalesOrderLineInformation;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.enterprise.WarehouseRule;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.json.JsonToDataConverter;

public class SaveSupplyData {

  BusinessEntityMapper getDataforSupply = new BusinessEntityMapper();
  public static final Logger log = Logger.getLogger(SaveSupplyData.class);

  public Order saveOrderHeader(JSONObject ordHeader, Warehouse warehouse, List<String> docNos,
      String type) throws Exception {
    Order ord = null;

    try {
      Organization org = BusinessEntityMapper.getOrgOfBP(ordHeader.getString("businessPartner"));
      BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(ordHeader
          .getString("organization"));
      Location location = bPartner.getBusinessPartnerLocationList().get(0);

      ord = createSalesOrder(ordHeader, org, bPartner, location, warehouse, type);
      docNos.add(ord.getDocumentNo());
      log.info("order created " + ord.getDocumentNo() + " having document action "
          + ord.getDocumentAction());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
    return ord;
  }

  public void saveOrderLines(JSONArray ordLines, HashMap<Product, BigDecimal> productsSet,
      Order ord, Warehouse warehouse) throws Exception {
    long lineNo = 0;
    boolean isStockPrefSet = isStockReservationPrfset();
    for (short i = 0; i < ordLines.length(); i++) {

      JSONObject obj = ordLines.getJSONObject(i);
      Organization org = ord.getOrganization();
      OrderLine ordLine = createSalesOrderLine(obj, org, ord, warehouse);
      Product pr = ordLine.getProduct();
      if (productsSet.containsKey(pr)) {
        if (isStockPrefSet) {
          lineNo = lineNo + 10;
          ordLine.setCreateReservation(SOConstants.StockReservationAutomatic);
          ordLine.setOrderedQuantity(productsSet.get(pr));
          ordLine.setLineNo(lineNo);

          // ordLine.setDescription(obj.getString("id"));
          ordLine.setIbdoPoid(obj.getString("id"));
          ord.getOrderLineList().add(ordLine);
          OBDal.getInstance().save(ord);

        } else {
          throw new OBException(
              "StockReservation preference not set Please set property to StockReservation and searchKey to Y  in preference window");
        }
      }
    }

    return;
  }

  private void salesOrderBookProcess(Order ord) throws Exception {
    SessionHandler.getInstance().commitAndStart();
    String ordId = ord.getId();
    log.debug(",," + SOConstants.performanceTest + ",supply side before booking order "
        + ord.getDocumentNo() + " at,, " + new Date());
    BusinessEntityMapper.executeProcess(ordId, "104", "SELECT * FROM c_order_post(?)");
    OBDal.getInstance().refresh(ord);
    log.debug(",," + SOConstants.performanceTest + ",supply side after booking order "
        + ord.getDocumentNo() + " at,, " + new Date());
  }

  private boolean isStockReservationPrfset() {
    OBContext.setAdminMode(true);
    try {
      String qry = "id in (select id from ADPreference adp where adp.property = 'StockReservations' and adp.searchKey='Y')";
      OBQuery<Preference> adprfQry = OBDal.getInstance().createQuery(Preference.class, qry);
      if (adprfQry.count() == 1)
        return true;
      else
        return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Order createSalesOrder(JSONObject ordJson, Organization org, BusinessPartner bPartner,
      Location location, Warehouse warehouse, String type) throws Exception {
    Order order = null;
    JsonToDataConverter fromJsonToData = new JsonToDataConverter();

    try {
      order = (Order) fromJsonToData.toBaseOBObject(ordJson);
      // order.setCreatedBy(OBContext.getOBContext().getUser());
      order.setCreationDate(new Date());
      // order.setUpdatedBy(OBContext.getOBContext().getUser());
      order.setUpdated(new Date());
      order.setSalesTransaction(true);
      order.setDocumentAction(SOConstants.CompleteDocumentStatus);
      order.setProcessed(false);
      order.setOrganization(org);
      order.setBusinessPartner(bPartner);
      order.setPartnerAddress(location);
      order.setInvoiceAddress(location);
      order.setWarehouse(warehouse);
      DocumentType docType = OBDal.getInstance().get(DocumentType.class,
          ordJson.getString(SOConstants.jsonTranxDoc));
      if (docType == null) {
        throw new OBException("There is no matching document Type");
      }
      DocumentType contraDoc = BusinessEntityMapper.getContraDocumentType(docType);
      order.setOrderDate(new Date());
      order.setScheduledDeliveryDate(new Date());
      order.setDocumentType(contraDoc);
      order.setTransactionDocument(contraDoc);
      order.setDocumentNo(ordJson.getString(SOConstants.jsonDocNo).concat("*" + type));
      order.setPriceList(BusinessEntityMapper.getPriceList(SOConstants.SOPriceList));
      order.setNewOBObject(true);
      OBDal.getInstance().save(order);

      return order;
    } catch (Exception e) {
      throw e;

    }

  }

  private OrderLine createSalesOrderLine(JSONObject obj, Organization org, Order ord,
      Warehouse warehouse) throws Exception {
    OrderLine orderLine = null;
    JsonToDataConverter fromJsonToData = new JsonToDataConverter();

    orderLine = (OrderLine) fromJsonToData.toBaseOBObject(obj);
    // orderLine.setCreatedBy(OBContext.getOBContext().getUser());
    orderLine.setCreationDate(new Date());
    // orderLine.setUpdatedBy(OBContext.getOBContext().getUser());
    orderLine.setUpdated(new Date());
    orderLine.setOrganization(org);
    orderLine.setSalesOrder(ord);
    orderLine.setPartnerAddress(null);
    orderLine.set(SOConstants.jsonWarehouse, warehouse);
    orderLine.setWarehouse(warehouse);
    orderLine.setSalesOrder(ord);
    orderLine.setNewOBObject(true);

    return orderLine;

  }

  public void getConfirmedQty(String orderId, Object object, JSONObject responseOrdLine,
      JSONObject responseOrd) throws Exception {
    try {
      Order ord = OBDal.getInstance().get(Order.class, orderId);
      List<OrderLine> ordLines = ord.getOrderLineList();
      for (OrderLine ordLine : ordLines) {
        if (responseOrdLine.has(ordLine.getIbdoPoid())) {
          BigDecimal prevQty = new BigDecimal(responseOrdLine.getString(ordLine.getIbdoPoid()));
          prevQty = prevQty.add(ordLine.getOrderedQuantity());
          responseOrdLine.put(ordLine.getIbdoPoid(), prevQty);
        } else {
          responseOrdLine.put(ordLine.getIbdoPoid(), ordLine.getOrderedQuantity());
        }
      }
      responseOrd.put("lines", responseOrdLine);
    } catch (Exception e) {
      responseOrd.put("Error", e.toString());
      throw e;
    }

  }

  public JSONObject distributeSalesOrder(JSONObject ordHeader, JSONArray ordLines,
      Map<String, List<Order>> retailSupply, boolean isSriLankaOrder) throws Exception {

    JSONObject responseOrd = new JSONObject();
    String orgId = ordHeader.getString("id");
    /*
     * In this method we are sending list of sales order id's, document no's and purchaser order id
     * to store. sales order id's helps store to send back to get confirmed qty.
     */
    String poDocNo = ordHeader.getString(SOConstants.jsonDocNo);

    // get SO's on dat document no.
    List<Order> ordList = BusinessEntityMapper.getSoOnDocNo(poDocNo);
    if (ordList != null && ordList.size() > 0) {
      responseOrd.put("PurchaseOrderID", ordHeader.getString("id"));
      String documentNumbers = "";
      for (Order ord : ordList) {
        if (ord.getDocumentStatus().equals("DR")) {
          purchaseOrderBookProcess(ord);
        }
        log.debug("Orders are already avalaible for this documentNo " + poDocNo);
        responseOrd.put(ord.getId(), ord.getDocumentNo());
        documentNumbers = documentNumbers + ord.getDocumentNo() + "/";
      }
      if (documentNumbers.length() > 1) {
        documentNumbers = documentNumbers.substring(0, documentNumbers.length() - 1);
      }
      retailSupply.put(orgId, ordList);
      responseOrd.put("DocumentNumbers", documentNumbers);
      return responseOrd;
    }

    try {
      responseOrd.put("PurchaseOrderID", ordHeader.getString("id"));
      List<Order> supplyOrder = new ArrayList<Order>();
      HashMap<String, List<SalesOrderLineInformation>> salesOrdersMap = new HashMap<String, List<SalesOrderLineInformation>>();
      String documentNumbers = "";

      for (int i = 0; i < ordLines.length(); i++) {
        JSONObject ordLine = ordLines.getJSONObject(i);

        if (isSriLankaOrder) {
          ordLine = updateOrderJsonWithCurrectIdForSL(ordLine);
        }

        salesOrdersMap = distributeSalesOrderLine(ordHeader, ordLine, salesOrdersMap);
      }

      for (Entry<String, List<SalesOrderLineInformation>> entry : salesOrdersMap.entrySet()) {
        String warehouseId = entry.getKey();
        List<SalesOrderLineInformation> salesOrderLineInformation = entry.getValue();
        Order order = createSalesOrder(ordHeader, ordLines, salesOrderLineInformation, warehouseId);
        Date startBook = new Date();
        salesOrderBookProcess(order);
        if (order.isFacstIsDirectDelivery())
          supplyOrder.add(order);

        Date endBook = new Date();
        log.debug(", " + SOConstants.perfOrmanceEnhanced + " SO Booking Time , "
            + order.getDocumentNo() + "," + startBook + ", " + endBook + ", "
            + (endBook.getTime() - startBook.getTime()) / 1000);
        responseOrd.put(order.getId(), order.getDocumentNo());
        documentNumbers = documentNumbers + order.getDocumentNo() + "/";
      }
      if (documentNumbers.length() > 1) {
        documentNumbers = documentNumbers.substring(0, documentNumbers.length() - 1);
      }

      retailSupply.put(orgId, supplyOrder);
      responseOrd.put("DocumentNumbers", documentNumbers);
    } catch (Exception e) {
      throw e;
    }
    return responseOrd;
  }

  private HashMap<String, List<SalesOrderLineInformation>> distributeSalesOrderLine(
      JSONObject ordHeader, JSONObject ordLine,
      HashMap<String, List<SalesOrderLineInformation>> salesOrdersMapParam) throws Exception {
    try {

      HashMap<String, List<SalesOrderLineInformation>> salesOrdersMap = salesOrdersMapParam;
      boolean warehouseRuleNotFound = true;
      HashMap<Warehouse, BigDecimal> alreadyReservedStock = new HashMap<Warehouse, BigDecimal>();
      HashMap<String, BigDecimal> remainigQtyToBook = new HashMap<String, BigDecimal>();
      HashMap<String, DWHR_DistributeMonitor> monitor = new HashMap<String, DWHR_DistributeMonitor>();

      Organization organization = null;
      organization = OBDal.getInstance().get(Organization.class,
          ordHeader.getString("organization"));
      OBCriteria<DwhrWhruleConfig> obc1 = OBDal.getInstance()
          .createCriteria(DwhrWhruleConfig.class);
      obc1.add(Restrictions.eq(DwhrWhruleConfig.PROPERTY_ORGANIZATION, organization));
      obc1.addOrderBy(DwhrWhruleConfig.PROPERTY_PRIORITY, true);

      for (DwhrWhruleConfig storeWarehouseRule : obc1.list()) {
        WarehouseRule warehouseRule = storeWarehouseRule.getWarehouseRule();

        if (warehouseRule.getDwhrJavaprocedure() == null
            || "".equals(warehouseRule.getDwhrJavaprocedure())) {
          continue;
        }

        Class<?> warehouseRuleJavaProcessClass;
        Object warehouseRuleJavaProcess = null;
        try {
          warehouseRuleJavaProcessClass = Class.forName(warehouseRule.getDwhrJavaprocedure());
          warehouseRuleJavaProcess = warehouseRuleJavaProcessClass.newInstance();
          WarehouseRuleImplementation warehouseRuleImplementator = (WarehouseRuleImplementation) warehouseRuleJavaProcess;
          if (warehouseRuleImplementator.doesApply(ordHeader, ordLine)) {
            salesOrdersMap = warehouseRuleImplementator.distribute(ordHeader, ordLine,
                salesOrdersMap, storeWarehouseRule, warehouseRule, remainigQtyToBook,
                alreadyReservedStock, monitor);
            warehouseRuleNotFound = false;

          }
        } catch (InstantiationException e) {
          log.error("SaveSupplyData.java distributeSalesOrderLine method", e);
        } catch (IllegalAccessException e) {
          log.error("SaveSupplyData.java distributeSalesOrderLine method", e);
        } catch (ClassNotFoundException e) {
          log.error("SaveSupplyData.java distributeSalesOrderLine method", e);
        }
      }

      if (warehouseRuleNotFound) {
        throw new OBException("WarehouseRule Not found");
      }

      return salesOrdersMap;
    } catch (Exception e) {
      throw new Exception(e.toString());
    }

  }

  private Order createSalesOrder(JSONObject ordHeader, JSONArray ordLines,
      List<SalesOrderLineInformation> salesOrderLineInformationList, String warehouseId)
      throws Exception {
    Order ord = null;
    try {
      Organization org = BusinessEntityMapper.getOrgOfBP(ordHeader.getString("businessPartner"));
      BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(ordHeader
          .getString("organization"));
      Location location = bPartner.getBusinessPartnerLocationList().get(0);
      Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
      ord = insertSalesOrder(ordHeader, org, bPartner, location, warehouse,
          warehouse.getSearchKey());
      long lineNo = 10;
      for (SalesOrderLineInformation salesOrderLineInformation : salesOrderLineInformationList) {
        OrderLine orderLine = insertOrderLine(ord, salesOrderLineInformation, org, warehouse,
            lineNo);
        ord.getOrderLineList().add(orderLine);
        lineNo = lineNo + 10;
      }
      OBDal.getInstance().save(ord);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new Exception(e.toString());
    }
    return ord;
  }

  private Order insertSalesOrder(JSONObject ordHeader, Organization org, BusinessPartner bPartner,
      Location location, Warehouse warehouse, String warehouseType) throws Exception {
    Order order = null;
    JsonToDataConverter fromJsonToData = new JsonToDataConverter();
    try {
      order = (Order) fromJsonToData.toBaseOBObject(ordHeader);
      order.setCreatedBy(order.getCreatedBy());
      order.setCreationDate(new Date());
      order.setUpdatedBy(order.getCreatedBy());
      order.setUpdated(new Date());
      order.setSalesTransaction(true);
      order.setDocumentAction(SOConstants.CompleteDocumentStatus);
      order.setProcessed(false);
      // set PO reference in SO record in sw_po_reference field
      order.setSwPoReference(order.getDocumentNo());
      order.setBusinessPartner(bPartner);
      order.setPartnerAddress(location);
      order.setInvoiceAddress(location);
      order.setWarehouse(warehouse);
      order.setFacstIsDirectDelivery(false);
      if (warehouse.getIbdoWarehousetype().equals("FACST_External")) {
        order.setFacstIsDirectDelivery(true);
        order.setOrganization(getExternalOrganization());
      } else {
        order.setOrganization(org);
      }
      order.setDsBpartner(null);
      DocumentType docType = OBDal.getInstance().get(DocumentType.class,
          ordHeader.getString(SOConstants.jsonTranxDoc));
      if (docType == null) {
        throw new OBException("There is no matching document Type");
      }
      DocumentType contraDoc = BusinessEntityMapper.getContraDocumentType(docType);
      order.setOrderDate(new Date());
      order.setScheduledDeliveryDate(new Date());
      order.setDocumentType(contraDoc);
      order.setTransactionDocument(contraDoc);
      order.setDocumentNo(ordHeader.getString(SOConstants.jsonDocNo).concat("*" + warehouseType));
      order.setPriceList(BusinessEntityMapper.getPriceList(SOConstants.SOPriceList));
      order.setNewOBObject(true);
      OBDal.getInstance().save(order);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new Exception(e.toString());
    }
    return order;
  }

  private OrderLine insertOrderLine(Order order,
      SalesOrderLineInformation salesOrderLineInformation, Organization org, Warehouse warehouse,
      long lineNo) throws Exception {
    // TODO check if preference check is needed
    OrderLine orderLine = null;
    JsonToDataConverter fromJsonToData = new JsonToDataConverter();
    try {
      orderLine = (OrderLine) fromJsonToData.toBaseOBObject(salesOrderLineInformation
          .getOrderLineJSON());
      orderLine.setCreatedBy(order.getCreatedBy());
      orderLine.setCreationDate(new Date());
      orderLine.setUpdatedBy(order.getCreatedBy());
      orderLine.setUpdated(new Date());
      orderLine.setOrganization(org);
      if (warehouse.getIbdoWarehousetype().equals("FACST_External")) {
        orderLine.setOrganization(order.getOrganization());
      }
      orderLine.setSalesOrder(order);
      orderLine.setPartnerAddress(null);
      orderLine.setWarehouse(warehouse);
      orderLine.setNewOBObject(true);
      orderLine.setProduct(salesOrderLineInformation.getProduct());
      orderLine.setCreateReservation(SOConstants.StockReservationAutomatic);
      orderLine.setOrderedQuantity(salesOrderLineInformation.getQuantity());
      orderLine.setWarehouseRule(salesOrderLineInformation.getWarehouseRule());
      orderLine.setLineNo(lineNo);
      orderLine.setIbdoPoid(salesOrderLineInformation.getOrderLineJSON().getString("id"));
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new Exception(e.toString());
    }
    OBDal.getInstance().save(orderLine);
    return orderLine;
  }

  private void purchaseOrderBookProcess(Order ord) throws Exception {
    try {
      String ordId = ord.getId();
      log.debug("booking order " + ord.getDocumentNo() + " having action "
          + ord.getDocumentAction());
      BusinessEntityMapper.executeProcess(ordId, "104", "SELECT * FROM c_order_post(?)");
      OBDal.getInstance().refresh(ord);
    } catch (Exception e) {
      log.error("Error while executing stored procedure c_order_post for order doc "
          + ord.getDocumentNo() + e.getMessage());
      throw e;
    }
  }

  private Organization getExternalOrganization() {
    OBCriteria<Organization> obCriteriaOrganization = OBDal.getInstance().createCriteria(
        Organization.class);
    obCriteriaOrganization.add(Restrictions.eq(Organization.PROPERTY_FACSTISEXTERNAL, true));
    List<Organization> organizationList = obCriteriaOrganization.list();
    if (organizationList.size() == 1) {
      return organizationList.get(0);
    } else {
      log.error("No Organization of type External/More than one Organization of type External ");
      throw new OBException(
          "No Organization of type External/More than one Organization of type External");
    }
  }

  public JSONObject updateOrderJsonWithCurrectIdForSL(JSONObject jsonObj) throws Exception {

    try {
      if (jsonObj.has("client") && OBContext.getOBContext().getCurrentClient().getId() != null) {
        jsonObj.remove("client");
        jsonObj.put("client", OBContext.getOBContext().getCurrentClient().getId());
      }
      if (jsonObj.has("client$_identifier")
          && OBContext.getOBContext().getCurrentClient().getName() != null) {
        jsonObj.remove("client$_identifier");
        jsonObj.put("client$_identifier", OBContext.getOBContext().getCurrentClient().getName());

      }/*
        * 
        * if (jsonObj.has("product") && jsonObj.has("product$_identifier")) { String productId =
        * (String) jsonObj.get("product"); String productName = (String)
        * jsonObj.get("product$_identifier");
        * 
        * OBCriteria<Product> obc = OBDal.getInstance().createCriteria(Product.class);
        * obc.add(Restrictions.or(Restrictions.eq(Product.PROPERTY_ID, productId),
        * Restrictions.eq(Product.PROPERTY_NAME, productName))); List<Product> productList =
        * obc.list(); if (productList.size() > 0) { jsonObj.remove("product");
        * jsonObj.put("product", productList.get(0).getId()); jsonObj.remove("product$_identifier");
        * jsonObj.put("product$_identifier", productList.get(0).getName()); } else { throw new
        * Exception("Product:" + productName + " Not Found!"); } }
        * 
        * if (jsonObj.has("cLColor") && jsonObj.has("cLColor$_identifier")) { String colorId =
        * (String) jsonObj.get("cLColor"); String colorName = (String)
        * jsonObj.get("cLColor$_identifier");
        * 
        * OBCriteria<CLColor> obc = OBDal.getInstance().createCriteria(CLColor.class);
        * obc.add(Restrictions.or(Restrictions.eq(CLColor.PROPERTY_ID, colorId),
        * Restrictions.eq(CLColor.PROPERTY_NAME, colorName))); List<CLColor> colorList = obc.list();
        * if (colorList.size() > 0) { jsonObj.remove("cLColor"); jsonObj.put("cLColor",
        * colorList.get(0).getId()); jsonObj.remove("cLColor$_identifier");
        * jsonObj.put("cLColor$_identifier", colorList.get(0).getName());
        * 
        * } else {
        * 
        * throw new Exception("color:" + colorName + " Not Found!");
        * 
        * } }
        * 
        * if (jsonObj.has("clBrand") && jsonObj.has("clBrand$_identifier")) { String brandId =
        * (String) jsonObj.get("clBrand"); String brandName = (String)
        * jsonObj.get("clBrand$_identifier");
        * 
        * OBCriteria<Brand> obc = OBDal.getInstance().createCriteria(Brand.class);
        * obc.add(Restrictions.or(Restrictions.eq(Brand.PROPERTY_ID, brandId),
        * Restrictions.eq(Brand.PROPERTY_NAME, brandName))); List<Brand> brandList = obc.list(); if
        * (brandList.size() > 0) { jsonObj.remove("clBrand"); jsonObj.put("clBrand",
        * brandList.get(0).getId()); jsonObj.remove("clBrand$_identifier");
        * jsonObj.put("clBrand$_identifier", brandList.get(0).getName()); } else {
        * 
        * throw new Exception("brand:" + brandName + " Not Found!");
        * 
        * } }
        */
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      throw new Exception("Error While Updating Id's For SL and Error is:" + e);
    }

    return jsonObj;
  }
}
