package in.decathlon.ibud.transfer.service;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.transfer.ad_process.ShuttleReceptionCorrection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.json.JsonToDataConverter;

public class SaveTransferRecord {

  BusinessEntityMapper getDataforSupply = new BusinessEntityMapper();
  public static final Logger log = Logger.getLogger(SaveTransferRecord.class);
  JSONObject responseOrd = new JSONObject();
  JSONObject responseOrdLine = new JSONObject();

  JSONObject responseShipment = new JSONObject();
  JSONObject responseShipmentLine = new JSONObject();

  JSONObject responsePI = new JSONObject();
  JSONObject responsePILine = new JSONObject();
  public String movementType;

  public JSONObject createAndSaveGoodsShipment(JSONObject grnHeader, JSONArray grnLines)
      throws Exception {
    try {

      Organization org = BusinessEntityMapper.getOrgOfBP(grnHeader.getString("businessPartner"));
      String returnDocument = grnHeader.getString("documentNo").toString();
      int lastIndexOf = returnDocument.lastIndexOf("*");
      String documentNo = returnDocument.substring(0, lastIndexOf);
      Warehouse warehouse = getWarehouse(documentNo, null);
      DocumentType shipDoctype = BusinessEntityMapper.getDocType("MMS", false);
      ShipmentInOut shpMent = saveShipment(grnHeader, warehouse, shipDoctype);
      movementType = getMovementType(documentNo, org);

      saveRFCShipmentLines(grnLines, shpMent, warehouse);
      responseShipment.put("id", shpMent.getId());

      OBDal.getInstance().save(shpMent);
      OBDal.getInstance().flush();

      shpMent.setDocumentStatus(SOConstants.DraftDocumentStatus);
      shpMent.setProcessGoodsJava(SOConstants.CompleteDocumentStatus);
      shpMent.setDocumentAction(SOConstants.CompleteDocumentStatus);
      OBDal.getInstance().save(shpMent);
      if (movementType.equals("FACST_DD"))
        shuttleReceptionDirect(shpMent);

      OBDal.getInstance().flush();
      SessionHandler.getInstance().commitAndStart();
      BusinessEntityMapper.executeProcess(shpMent.getId(), "109", "SELECT * FROM M_InOut_Post0(?)");

      // BusinessEntityMapper.txnSWMovementType(shpMent);
      responsePI.put("id", shpMent.getId());
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }

    return responsePI;
  }

  public void shuttleReceptionDirect(ShipmentInOut shpMent) throws Exception {
    try {
      ShuttleReceptionCorrection src = new ShuttleReceptionCorrection();
      InventoryCount ivCount = src.createInventoryHeader(shpMent);
      List<ShipmentInOutLine> shioLines = shpMent.getMaterialMgmtShipmentInOutLineList();
      List<InventoryCountLine> inventoryLines = new ArrayList<InventoryCountLine>();
      for (ShipmentInOutLine shioL : shioLines) {
        boolean isAccepted = shioL.isIbodtrIsaccepted();
        BigDecimal quantity = shioL.getMovementQuantity();
        if (!isAccepted) {
          if (src.isShipment(shioL)) {
            // Inter Org MM Shipment
            quantity = quantity.negate();
            inventoryLines.add(src.createInventoryLines(ivCount, shioL, quantity, true));
            log.debug("Saved Inventory");
          } else {
            // Inter Org RFC Receipt
            log.debug("Since shortage removing excess quantity ");
            quantity = quantity.abs();
            inventoryLines.add(src.createInventoryLines(ivCount, shioL, quantity, false));
          }
        }

      }
      if (inventoryLines.size() > 0) {
        ivCount.getMaterialMgmtInventoryCountLineList().addAll(inventoryLines);
        OBDal.getInstance().save(ivCount);
        src.processInventory(ivCount);
      }
      shpMent.setIbodtrCorrection(true);
      OBDal.getInstance().save(shpMent);
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
  }

  public static Warehouse getWarehouse(String documentNo, Organization org) throws Exception {
    try {
      OBContext.setAdminMode(true);

      String qry = "o where o.documentNo = '" + documentNo + "' and o.salesTransaction='Y'";
      OBQuery<ShipmentInOut> strQry = OBDal.getInstance().createQuery(ShipmentInOut.class, qry);
      strQry.setMaxResult(1);
      List<ShipmentInOut> m_inoutList = strQry.list();
      if (m_inoutList != null && m_inoutList.size() > 0) {
        ShipmentInOut gsRecord = m_inoutList.get(0);
        if (gsRecord != null) {
          return gsRecord.getWarehouse();
        } else {
          throw new Exception("No warehouse for the GS" + gsRecord);
        }
      } else {
        if (org != null) {
          return BusinessEntityMapper.getReturnOrgWarehouse(org.getId()).getWarehouse();
        } else {
          throw new OBException("No GS for the documentno: " + documentNo);
        }
      }

      /*
       * String qry =
       * "id in (select id from Warehouse wh where wh.ibdoWarehousetype='CAR' and wh.organization.id='"
       * + org.getId() + "')"; OBQuery<Warehouse> strQry =
       * OBDal.getInstance().createQuery(Warehouse.class, qry); strQry.setMaxResult(1);
       * List<Warehouse> warehouseList = strQry.list(); if (warehouseList == null ||
       * warehouseList.isEmpty()) { throw new OBException("There is no warehouse for org " + org); }
       * Warehouse warehouse = warehouseList.get(0); Locator returnLoc =
       * warehouse.getReturnlocator(); if (returnLoc == null) throw new
       * OBException("There is no return bin  for warehouse " + warehouse);
       * 
       * return returnLoc.getWarehouse();
       */
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static String getMovementType(String documentNo, Organization org) throws Exception {
    try {
      OBContext.setAdminMode(true);
      String qry = "o where o.documentNo = '" + documentNo + "' and o.salesTransaction='Y'";
      OBQuery<ShipmentInOut> strQry = OBDal.getInstance().createQuery(ShipmentInOut.class, qry);
      strQry.setMaxResult(1);
      List<ShipmentInOut> m_inoutList = strQry.list();
      ShipmentInOut gsRecord = m_inoutList.get(0);
      return gsRecord.getSWMovement();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public JSONObject createAndSaveRMRShipment(JSONObject shipMentHeader, JSONArray shipMentLines)
      throws Exception {
    try {

      Organization org = BusinessEntityMapper.getOrgOfBP(shipMentHeader
          .getString("businessPartner"));
      Warehouse warehouse = null;
      String docNo = shipMentHeader.getString("documentNo");
      int pos = docNo.lastIndexOf("*");
      String soDocNo = docNo.substring(0, pos);
      Order salesOrder = getSalesOrder(soDocNo);
      DocumentType rfcDoctype = BusinessEntityMapper.getDocType("MMS", true);

      movementType = getMovementType(soDocNo, org);
      if (salesOrder != null) {
        warehouse = salesOrder.getWarehouse();
      }
      ShipmentInOut shpMent = saveShipment(shipMentHeader, warehouse, rfcDoctype);

      saveRFCShipmentLines(shipMentLines, shpMent, warehouse);

      if (movementType.equals("FACST_DD"))
        shuttleReceptionDirect(shpMent);

      OBDal.getInstance().flush();
      SessionHandler.getInstance().commitAndStart();

      BusinessEntityMapper.executeProcess(shpMent.getId(), "109", "SELECT * FROM M_InOut_Post0(?)");

      // BusinessEntityMapper.txnSWMovementType(shpMent);

      responseShipment.put("id", shpMent.getId());
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
    return responseShipment;
  }

  private Order getSalesOrder(String docNo) {
    OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
    ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, docNo));
    ordCrit.setMaxResults(1);
    if (ordCrit.count() > 0)
      return ordCrit.list().get(0);
    log.info("Sales order does not exist on document no. " + docNo
        + " therefore default warehouse will be assigned as warehouse");
    return null;
  }

  public boolean saveRFCShipmentLines(JSONArray shipMentLines, ShipmentInOut shpMent,
      Warehouse warehouse) throws Exception {
    boolean isSaved = false;
    try {
      long line = 10;
      for (short i = 0; i < shipMentLines.length(); i++) {
        JSONObject obj = shipMentLines.getJSONObject(i);
        Organization org = shpMent.getOrganization();
        ShipmentInOutLine shpMentLine = createRFCShipmentLine(obj, org, shpMent, line, warehouse);
        OBDal.getInstance().save(shpMentLine);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(shpMent);
        responseShipmentLine.put(obj.getString("id"), 0.0);
        line = line + 10;
      }
    } catch (Exception e) {
      isSaved = false;
      e.printStackTrace();
      log.error(e);
      throw e;
    } finally {
      try {
        responseShipment.put("lines", responseOrdLine);
      } catch (JSONException e) {
        log.error(e);
        e.printStackTrace();
        throw e;
      }
    }
    return isSaved;
  }

  private ShipmentInOutLine createRFCShipmentLine(JSONObject obj, Organization org,
      ShipmentInOut shpMent, long line, Warehouse warehouse) {
    ShipmentInOutLine shpmntLine = null;
    try {
      JsonToDataConverter fromJsonToData = new JsonToDataConverter();
      String qty = obj.getString("movementQuantity");
      BigDecimal newQty = new BigDecimal(qty);
      shpmntLine = (ShipmentInOutLine) fromJsonToData.toBaseOBObject(obj);
      shpmntLine.set("shipmentReceipt", shpMent);
      shpmntLine.set("lineNo", line);
      shpmntLine.set("createdBy", OBContext.getOBContext().getUser());
      shpmntLine.set("creationDate", new Date());
      shpmntLine.set("updatedBy", OBContext.getOBContext().getUser());
      shpmntLine.set("updated", new Date());
      shpmntLine.set("organization", org);
      shpmntLine.setMovementQuantity(newQty);
      if (movementType.equals("FACST_DD"))
        shpmntLine.setIbodtrIsaccepted(false);
      shpmntLine.setStorageBin(warehouse.getReturnlocator());
      shpmntLine.setNewOBObject(true);
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
    }
    return (ShipmentInOutLine) shpmntLine;
  }

  public ShipmentInOut saveShipment(JSONObject ordHeader, Warehouse warehouse, DocumentType docType)
      throws Exception {
    ShipmentInOut shpMent = OBProvider.getInstance().get(ShipmentInOut.class);
    try {
      Organization org = BusinessEntityMapper.getOrgOfBP(ordHeader.getString("businessPartner"));
      BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(ordHeader
          .getString("organization"));
      Location location = bPartner.getBusinessPartnerLocationList().get(0);
      shpMent = createRFCShipment(ordHeader, org, bPartner, location, warehouse, docType);
      shpMent.setIbodtrVaidate(true);
      shpMent.setSWMovement(SOConstants.SWMovement);
      responseShipment.put(ordHeader.getString("id"), shpMent.getDocumentNo());
      OBDal.getInstance().save(shpMent);
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
    return shpMent;
  }

  private ShipmentInOut createRFCShipment(JSONObject shMentJson, Organization org,
      BusinessPartner bPartner, Location location, Warehouse warehouse, DocumentType docType)
      throws Exception {
    BaseOBObject bob = null;
    try {
      JsonToDataConverter fromJsonToData = new JsonToDataConverter();

      bob = fromJsonToData.toBaseOBObject(shMentJson);
      bob.set("createdBy", OBContext.getOBContext().getUser());
      bob.set("creationDate", new Date());
      bob.set("updatedBy", OBContext.getOBContext().getUser());
      bob.set("updated", new Date());
      bob.set("salesTransaction", true);
      bob.set("documentType", docType);
      bob.set("documentNo", shMentJson.getString("documentNo").toString());
      bob.set("organization", org);
      bob.set("businessPartner", bPartner);
      bob.set("partnerAddress", location);
      bob.set("warehouse", warehouse);
      bob.set("ibodtrIsautomatic", true);
      bob.setNewOBObject(true);
      bob.set("ibodtrAutotr", true);

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      throw e;
    }
    return (ShipmentInOut) bob;
  }

  public JSONObject createAndSaveRFCOrder(JSONObject ordHeader, JSONArray ordLines)
      throws Exception {
    try {
      Organization org = BusinessEntityMapper.getOrgOfBP(ordHeader.getString("businessPartner"));
      String returnDocument = ordHeader.getString("documentNo").toString();
      int lastIndexOf = returnDocument.lastIndexOf("*");
      String documentNo = returnDocument.substring(0, lastIndexOf);

      Warehouse warehouse = getWarehouse(documentNo, null);
      Order ord = saveRFCOrder(ordHeader);
      saveOrderLines(ordLines, ord, warehouse);
      OBDal.getInstance().save(ord);
      responseOrd.put("id", ord.getId());
      log.debug(JSONHelper.convetBobToJson(ord));
      OBDal.getInstance().flush();

      log.debug("Complete RFC");
      BusinessEntityMapper.executeProcess(ord.getId(), "109", "SELECT * FROM C_Order_post(?)");

    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
    return responseOrd;
  }

  public boolean saveOrderLines(JSONArray ordLines, Order ord, Warehouse warehouse)
      throws Exception {

    boolean isSaved = false;
    try {
      for (short i = 0; i < ordLines.length(); i++) {

        JSONObject obj = ordLines.getJSONObject(i);

        createSalesOrderLine(obj, ord.getOrganization(), ord, warehouse);
        isSaved = true;
        responseOrdLine.put(obj.getString("id"), 0.0);
      }
    } catch (Exception e) {
      isSaved = false;
      e.printStackTrace();
      log.error(e);
      throw e;
    } finally {
      try {
        responseOrd.put("lines", responseOrdLine);
      } catch (JSONException e) {
        log.error(e);
        e.printStackTrace();
        throw e;
      }
    }
    return isSaved;
  }

  public Order saveRFCOrder(JSONObject ordHeader) throws Exception {
    Order ord = OBProvider.getInstance().get(Order.class);
    try {
      Organization org = BusinessEntityMapper.getOrgOfBP(ordHeader.getString("businessPartner"));
      BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(ordHeader
          .getString("organization"));
      Location location = bPartner.getBusinessPartnerLocationList().get(0);

      ord = createSalesOrder(ordHeader, org, bPartner, location);
      responseOrd.put(ordHeader.getString("id"), ord.getDocumentNo());

    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }
    return ord;
  }

  private OrderLine createSalesOrderLine(JSONObject obj, Organization org, Order ord,
      Warehouse warehouse) throws Exception {
    OrderLine ordLine = null;
    try {
      JsonToDataConverter fromJsonToData = new JsonToDataConverter();
      ordLine = (OrderLine) fromJsonToData.toBaseOBObject(obj);
      ordLine.set("salesOrder", ord);
      ordLine.set("createdBy", OBContext.getOBContext().getUser());
      ordLine.set("creationDate", new Date());
      ordLine.set("updatedBy", OBContext.getOBContext().getUser());
      ordLine.set("updated", new Date());
      ordLine.set("organization", org);
      ordLine.setWarehouse(warehouse);
      ordLine.setNewOBObject(true);
      ord.getOrderLineList().add(ordLine);

    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      throw e;
    }

    return ordLine;
  }

  private Order createSalesOrder(JSONObject ordJson, Organization org, BusinessPartner bPartner,
      Location location) throws Exception {
    Order order = null;
    try {

      DocumentType docType = BusinessEntityMapper.getDocType("SOO", true);
      JsonToDataConverter fromJsonToData = new JsonToDataConverter();

      order = (Order) fromJsonToData.toBaseOBObject(ordJson);
      order.set("createdBy", OBContext.getOBContext().getUser());
      order.set("creationDate", new Date());
      order.set("updatedBy", OBContext.getOBContext().getUser());
      order.set("updated", new Date());
      order.set("salesTransaction", true);
      order.set("documentType", docType);
      order.set("documentNo", ordJson.getString("documentNo").toString());
      order.set("organization", org);
      order.set("businessPartner", bPartner);
      order.set("partnerAddress", location);
      String returnDocument = ordJson.getString("documentNo");
      int lastIndexOf = returnDocument.lastIndexOf("*");
      String documentNo = returnDocument.substring(0, lastIndexOf);
      order.set("warehouse", getWarehouse(documentNo, org));
      order.setTransactionDocument(docType);
      order.setDocumentStatus(SOConstants.DraftDocumentStatus);
      order.setNewOBObject(true);

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      throw e;
    }
    return order;
  }

  public DocumentType getDocumentType(String name) {
    OBCriteria<DocumentType> docCrit = OBDal.getInstance().createCriteria(DocumentType.class);
    docCrit.add(Restrictions.eq(DocumentType.PROPERTY_NAME, name));
    docCrit.add(Restrictions.eq(DocumentType.PROPERTY_CLIENT, OBContext.getOBContext()
        .getCurrentClient()));

    List<DocumentType> docCritList = docCrit.list();
    if (docCritList != null && docCritList.size() > 0) {
      return docCritList.get(0);
    } else {
      throw new OBException("client not found");
    }
  }

  private void txnSWMovementType(ShipmentInOut returns) {
    List<ShipmentInOutLine> grLineList = getShipmentLineList(returns);

    for (ShipmentInOutLine grLine : grLineList) {
      List<MaterialTransaction> txnList = getTxnList(grLine);

      for (MaterialTransaction txn : txnList) {
        txn.setSwMovementtype(returns.getSWMovement());
        OBDal.getInstance().save(txn);
      }
    }
  }

  private List<ShipmentInOutLine> getShipmentLineList(ShipmentInOut returns) {
    String qry = "id in(from MaterialMgmtShipmentInOutLine minoutLine where minoutLine.shipmentReceipt.id = '"
        + returns.getId() + "')";
    OBQuery<ShipmentInOutLine> obqry = OBDal.getInstance()
        .createQuery(ShipmentInOutLine.class, qry);
    List<ShipmentInOutLine> lineList = obqry.list();
    return lineList;

  }

  private List<MaterialTransaction> getTxnList(ShipmentInOutLine shipLine) {
    String qry = "id in(from MaterialMgmtMaterialTransaction txn where txn.goodsShipmentLine.id = '"
        + shipLine.getId() + "')";
    OBQuery<MaterialTransaction> obqry = OBDal.getInstance().createQuery(MaterialTransaction.class,
        qry);
    List<MaterialTransaction> txnList = obqry.list();
    return txnList;

  }

}