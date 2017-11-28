package in.decathlon.ibud.shipment.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.json.JsonToDataConverter;

import com.sysfore.catalog.CLImplantation;

public class CreateGRNService {
  public static final Logger log = Logger.getLogger(CreateGRNService.class);
  JsonToDataConverter JsonToData = OBProvider.getInstance().get(JsonToDataConverter.class);
  BusinessEntityMapper getDataforSupply = new BusinessEntityMapper();
  JSONObject responseInout = new JSONObject();
  JSONObject responseShipmentLine = new JSONObject();

  public ShipmentInOut addGRN(JSONObject shipmentJson, Organization org, BusinessPartner bPartner,
      Location location, DocumentType docType, Warehouse warehouse) throws Exception {
    log.debug(" Enter addGRN method");
    String shipmentDocNo = shipmentJson.getString("documentNo");

    // String orderDetails = (String) shipmentJson.get("salesOrder$_identifier");
    // int indexOfId = orderDetails.lastIndexOf("*");

    // String soOrderNo = orderDetails.substring(0, indexOfId);
    // Order poReference = getPoObj(soOrderNo);
    ShipmentInOut bob = null;
    try {
      bob = (ShipmentInOut) JsonToData.toBaseOBObject(shipmentJson);
      bob.set("organization", org);
      bob.set("createdBy", OBContext.getOBContext().getUser());
      bob.set("updatedBy", OBContext.getOBContext().getUser());
      bob.set("creationDate", new Date());
      bob.set("updated", new Date());
      bob.set("businessPartner", bPartner);
      bob.set("partnerAddress", location);
      bob.set("salesTransaction", false);
      bob.set("documentStatus", "DR");
      bob.set("documentType", docType);
      bob.set("warehouse", warehouse);
      bob.set("processed", false);
      bob.set("processGoodsJava", "CO");
      bob.set("documentAction", "CO");
      // ATP change by Amit ...
      bob.set("documentNo", shipmentDocNo);
      bob.set("salesOrder", null);
      bob.set("obwplPlbox", null);

      // Order purchaseOrder = getPurchaseOrder(shipmentDocNo, org);

      // bob.set("salesOrder", purchaseOrder);

      bob.set("id", null);
      bob.set("ibudsShipmentreference", shipmentDocNo);
      /*
       * if (purchaseOrder.isSwIsimplantation()) {
       * bob.setSWMovement(SOConstants.implantationMovementType); } else { bob.set("sWMovement",
       * "SRQ"); }
       */
      bob.setIbodtrIsautomatic(true);
      bob.setNewOBObject(true);
    } catch (Exception e) {
      throw e;
    }
    return bob;
  }

  private Order getPoObj(String soOrderNo) throws Exception {

    OBCriteria<Order> poOrderCriteria = OBDal.getInstance().createCriteria(Order.class);
    poOrderCriteria.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, soOrderNo));
    poOrderCriteria.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));

    List<Order> orderList = poOrderCriteria.list();
    if (orderList != null && orderList.size() > 0) {
      return poOrderCriteria.list().get(0);
    } else {
      throw new Exception("No PO found with document no" + soOrderNo + "");
    }

  }

  public void addGRNLines(JSONArray obj, Organization org, Locator sBin, ShipmentInOut bob)
      throws Exception {

    log.debug(" Enter addGRNLines method");
    HashSet<Product> lineProducts = new HashSet<Product>();
    for (int i = 0; i < obj.length(); i++) {

      ShipmentInOutLine bobline = null;

      JSONObject jsonobj = obj.getJSONObject(i);

      BigDecimal actualMovementQty = new BigDecimal(jsonobj.getString("movementQuantity")
          .toString());

      String orderLineDetails = (String) jsonobj.get("salesOrderLine$_identifier");
      int indexOfId = orderLineDetails.lastIndexOf("*");
      String soOrderNo = orderLineDetails.substring(0, indexOfId);
      // Order poReference = getPoObj(soOrderNo);
      bobline = (ShipmentInOutLine) JsonToData.toBaseOBObject(jsonobj);

      bobline.set("createdBy", OBContext.getOBContext().getUser());
      bobline.set("creationDate", new Date());
      bobline.set("updatedBy", OBContext.getOBContext().getUser());
      bobline.set("updated", new Date());
      bobline.set("organization", org);
      bobline.set("obwplPickinglist", null);
      String productId = jsonobj.getString("product");
      String orderLineId = getOrderLine(productId, soOrderNo);
      OrderLine orderLineRef = OBDal.getInstance().get(OrderLine.class, orderLineId);
      bobline.set("salesOrderLine", orderLineRef);

      bobline.set("storageBin", sBin);
      bobline.set("id", null);
      bobline.set("obwplPickinglist", null);
      bobline.set("ibodtrActmovementqty", actualMovementQty);
      bobline.setAttributeSetValue(null);
      bobline.setNewOBObject(true);
      bobline.setShipmentReceipt(bob);

      if (lineProducts.size() > 0) {
        if (lineProducts.contains(bobline.getProduct())) {
          SessionHandler.getInstance().commitAndStart();

          BigDecimal qty = bobline.getMovementQuantity();

          OBCriteria<ShipmentInOutLine> shplinCrit = OBDal.getInstance().createCriteria(
              ShipmentInOutLine.class);
          shplinCrit.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_PRODUCT, bobline.getProduct()));
          shplinCrit.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, bob));
          shplinCrit.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLineRef));
          shplinCrit.setMaxResults(1);
          List<ShipmentInOutLine> shpLinList = shplinCrit.list();
          if (shpLinList.size() > 0) {
            ShipmentInOutLine shpl = shpLinList.get(0);
            BigDecimal initQty = shpl.getMovementQuantity();
            shpl.setMovementQuantity(initQty.add(qty));
            shpl.setIbodtrActmovementqty(initQty.add(qty));
            OBDal.getInstance().save(shpl);
          } else {
            bob.getMaterialMgmtShipmentInOutLineList().add(bobline);
            lineProducts.add(bobline.getProduct());
            OBDal.getInstance().save(bob);
          }
        } else {
          bob.getMaterialMgmtShipmentInOutLineList().add(bobline);
          lineProducts.add(bobline.getProduct());
          OBDal.getInstance().save(bob);
        }
      } else {
        bob.getMaterialMgmtShipmentInOutLineList().add(bobline);
        lineProducts.add(bobline.getProduct());
        OBDal.getInstance().save(bob);
      }

    }

    return;
  }

  private String getOrderLine(String productId, String poReference) {
    String qry = "select ol.id from OrderLine ol join ol.salesOrder ord where ol.product.id = '"
        + productId + "' and ord.documentNo='" + poReference + "' and ord.salesTransaction='N'";
    final Query query = OBDal.getInstance().getSession().createQuery(qry);
    List qryList = query.list();
    if (qryList != null && qryList.size() > 0)
      return (String) qryList.get(0);
    else
      throw new OBException("no orderline exists for the order " + poReference + " with product "
          + productId);
  }

  public ShipmentInOut saveShimpmentHeader(JSONObject inoutHeader, String processid)
      throws Exception {
    log.debug("saveShimpmentHeader");
    ShipmentInOut inout = OBProvider.getInstance().get(ShipmentInOut.class);

    try {

      JSONArray shipmentsArray = inoutHeader.getJSONArray("data");
      for (int i = 0; i < shipmentsArray.length(); i++) {
        try {
          JSONObject shipmentObj = shipmentsArray.getJSONObject(i);
          JSONObject shipmentHeader = (JSONObject) shipmentObj.get("Header");
          JSONArray shipmentLines = shipmentObj.getJSONArray("Lines");

          String docNo = shipmentHeader.getString(SOConstants.jsonDocNo);
          boolean grnAvailable = getGrnOnDoc(docNo);
          if (!grnAvailable) {
            Organization org = BusinessEntityMapper.getOrgOfBP(shipmentHeader
                .getString(SOConstants.jsonBusinessPartner));
            BusinessPartner bPartner = BusinessEntityMapper.getBPOfOrg(shipmentHeader
                .getString(SOConstants.jsonOrganization));

            Location location = bPartner.getBusinessPartnerLocationList().get(0);

            DocumentType docType = OBDal.getInstance().get(DocumentType.class,
                shipmentHeader.getString(SOConstants.jsonDocumentType));

            log.debug(".......docType........." + docType);
            DocumentType contraDocumentType = getContraDocumentType(docType, docNo);
            log.debug("........contraDocumentType......" + contraDocumentType);

            String orgId = org.getId();
            OrgWarehouse orgWarehouse = BusinessEntityMapper.getOrgWarehouse(orgId);
            String warehouseId = orgWarehouse.getWarehouse().getId();
            Locator storageBin = BusinessEntityMapper.getFirstStorageBin(warehouseId);

            inout = addGRN(shipmentHeader, org, bPartner, location, contraDocumentType,
                orgWarehouse.getWarehouse());

            OBDal.getInstance().save(inout);

            log.debug(".............Shipment Header saved of docNo="
                + shipmentHeader.getString(SOConstants.jsonDocNo) + ".........");
            addGRNLines(shipmentLines, org, storageBin, inout);
            OBDal.getInstance().save(inout);
            OBDal.getInstance().flush();
            // OBDal.getInstance().refresh(inout);
            // SessionHandler.getInstance().commitAndStart();

            if (inout.getSalesOrder() != null) {
              if (inout.getSalesOrder().isSwIsimplantation()) {
                setImplantaionRecord(inout);
              }
            }
          } else
            continue;
          // poStatusChange(inout);
        } catch (Exception e) {
          BusinessEntityMapper.rollBackNlogError(e, processid, null);
          log.error("Exception while creating GRN");
        }

      }

    } catch (Exception e) {
      throw e;
    }
    return inout;
  }

  private void poStatusChange(ShipmentInOut inout) {
    try {
      for (ShipmentInOutLine inoutLine : inout.getMaterialMgmtShipmentInOutLineList()) {
        OrderLine orderLine = inoutLine.getSalesOrderLine();
        Order purchaseOrder = orderLine.getSalesOrder();
        String qry = "select col.id from MaterialMgmtShipmentInOutLine mil right join mil.salesOrderLine col "
            + "join col.salesOrder co where co.id='"
            + purchaseOrder.getId()
            + "' and mil.id is null "
            + "and col.id !='"
            + inoutLine.getSalesOrderLine().getId()
            + "'";
        final Query query = OBDal.getInstance().getSession().createQuery(qry);
        List<String> inoutLineList = query.list();
        if (inoutLineList != null && inoutLineList.size() > 0) {
          if (!(purchaseOrder.getDocumentStatus().equals(SOConstants.partialShipped))) {
            purchaseOrder.setDocumentStatus(SOConstants.partialShipped);
            purchaseOrder.setDocumentAction(SOConstants.closed);
            OBDal.getInstance().save(purchaseOrder);
            OBDal.getInstance().flush();
          } else
            log.debug("order " + purchaseOrder + " is already in partially recieved status");
        } else {
          if (!(purchaseOrder.getDocumentStatus().equals(SOConstants.shipped))) {
            purchaseOrder.setDocumentStatus(SOConstants.shipped);
            purchaseOrder.setDocumentAction(SOConstants.closed);
            OBDal.getInstance().save(purchaseOrder);
            OBDal.getInstance().flush();
          }
        }

      }
    } catch (Exception e) {
      log.error("error in query execution ");
      throw new OBException("error in query execution" + e);
    }
  }

  private DocumentType getContraDocumentType(DocumentType documentType, String docNo) {
    if (documentType == null) {
      throw new OBException("There is no document type assigned to GS for record " + docNo);
    }
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, documentType.getId());
    if (docType == null) {
      throw new OBException(" no doc type for the GS in retail to get contra doc type "
          + documentType);
    }
    if (docType.getIbdoContradocument() == null) {
      throw new OBException("There is no Contra Document type mentioned for "
          + documentType.getName() + " So cant create GR for the doc no " + docNo);
    }
    return docType.getIbdoContradocument();

  }

  @SuppressWarnings("null")
  public void setImplantaionRecord(ShipmentInOut inout) {
    Map<String, BigDecimal> inoutlineMap = new HashMap<String, BigDecimal>();
    Order purchaseOrder = inout.getSalesOrder();
    if (purchaseOrder != null) {
      String salesOrderRef = purchaseOrder.getSwPoReference();
      if (salesOrderRef != null) {
        String[] salesOrderList = salesOrderRef.split("/");
        int noOfDocuments = 0;
        if (salesOrderList.length == 1) {
          if (salesOrderList[0].equals(inout.getDocumentNo())) {
            List<ShipmentInOutLine> inoutline = inout.getMaterialMgmtShipmentInOutLineList();
            if (inoutline != null) {
              for (ShipmentInOutLine line : inoutline) {
                inoutlineMap.put(line.getProduct().getId(), line.getMovementQuantity());
              }
            }
            setBlockedQtyandImplFlag(purchaseOrder, inoutlineMap);
          }
        } else {
          int soDocCount = 0, closeSoCount = 0, grnCount = 0;
          soDocCount = salesOrderList.length;
          for (String salesOrder : salesOrderList) {
            if (CreateGRNService.getGrnOnDoc(salesOrder)) {
              noOfDocuments++;
            }
          }
          String closeSORef = purchaseOrder.getIbdoGrReference();
          String[] closeSOList = new String[100];
          if (closeSORef != null) {
            closeSOList = closeSORef.split("/");
            for (String closeSO : closeSOList) {
              closeSoCount++;
            }

          }
          grnCount = soDocCount - closeSoCount;
          String[] grnToBeCreatedList = new String[100];
          int i = 0;
          if (noOfDocuments == grnCount) {
            if (closeSOList != null) {
              for (String salesOrder : salesOrderList) {
                if (!(closeSOList.toString().contains(salesOrder))) {
                  grnToBeCreatedList[i] = salesOrder;
                  i++;
                }
              }
              for (String grn : grnToBeCreatedList) {
                getGRNDetailsForImpl(inoutlineMap, grn);

              }
            } else {
              for (String salesOrder : salesOrderList) {
                getGRNDetailsForImpl(inoutlineMap, salesOrder);

              }
            }
            setBlockedQtyandImplFlag(purchaseOrder, inoutlineMap);
          }

        }
      }
    }
  }

  private void getGRNDetailsForImpl(Map<String, BigDecimal> inoutlineMap, String salesOrder) {
    ShipmentInOut grn = getGRN(salesOrder);
    if (grn != null) {
      List<ShipmentInOutLine> inoutline = grn.getMaterialMgmtShipmentInOutLineList();
      if (inoutline != null) {
        for (ShipmentInOutLine line : inoutline) {
          String productId = line.getProduct().getId();
          BigDecimal resultantValue = line.getMovementQuantity();
          if (inoutlineMap.containsKey(productId)) {
            BigDecimal previousValue = inoutlineMap.get(productId);
            if (previousValue != null) {
              resultantValue = resultantValue.add(previousValue);
            }
          }
          inoutlineMap.put(productId, resultantValue);
        }
      }
    }
  }

  public static ShipmentInOut getGRN(String salesOrdDoc) {
    OBCriteria<ShipmentInOut> grnCrit = OBDal.getInstance().createCriteria(ShipmentInOut.class);
    grnCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, salesOrdDoc));
    grnCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, false));
    ShipmentInOut grn = (ShipmentInOut) grnCrit.uniqueResult();
    return grn;
  }
  

public static List<String> getGRNs(String poDocNo) {
	List<String> soQryList =null;
	  String shipCompleteQry = "select distinct mi.id from MaterialMgmtShipmentInOut mi "
	            + "right join mi.materialMgmtShipmentInOutLineList mil "
	            + "right join mil.salesOrderLine col right join col.salesOrder co "
	            + "where "
	            + "co.documentNo like '"
	            + poDocNo
	            + "%' "
	            + "and co.salesTransaction = 'N'  and co.documentType.return='N'";
	        log.debug("retrive qry to get ol to check status " + shipCompleteQry);
	        final Query soQry = OBDal.getInstance().getSession().createQuery(shipCompleteQry);
	         soQryList = soQry.list();
	        return soQryList;
}


  @SuppressWarnings("unchecked")
  public static void setBlockedQtyandImplFlag(Order purchaseOrder, Map inoutlineMap) {
    BigDecimal blockedQty = BigDecimal.ZERO;
    BigDecimal newBlockedQty = BigDecimal.ZERO;
    BigDecimal oldBlockedQty = BigDecimal.ZERO;
    List<OrderLine> poline = purchaseOrder.getOrderLineList();
    if (poline != null) {
      for (OrderLine ol : poline) {
        String orgId = ol.getOrganization().getId();
        String productId = ol.getProduct().getId();
        long actualConfirmedQty = ol.getSwConfirmedqty();
        BigDecimal confirmedQty = BigDecimal.valueOf(actualConfirmedQty);
        if (inoutlineMap.containsKey(productId)) {
          BigDecimal movementQty = (BigDecimal) inoutlineMap.get(productId);
          blockedQty = confirmedQty.subtract(movementQty);
        } else {
          blockedQty = confirmedQty;
        }
        CLImplantation implRecord = BusinessEntityMapper.getImplantationOrg(orgId, productId);
        if (implRecord != null) {
          log.debug("Setting blocked qty to the org " + implRecord.getStoreImplanted()
              + "with product" + implRecord.getProduct());
          oldBlockedQty = implRecord.getBLOCKEDQTY();
          newBlockedQty = oldBlockedQty.subtract(blockedQty);
          log.debug("old blocked qty= " + oldBlockedQty + "qty to be subtracted or added = "
              + blockedQty + "new blocked Qty = " + newBlockedQty);
          implRecord.setBLOCKEDQTY(newBlockedQty);
          if (newBlockedQty.compareTo(BigDecimal.valueOf(implRecord.getImplantationQty())) >= 0) {
            implRecord.setImplanted(true);
          } else {
            implRecord.setImplanted(false);
          }

          log.debug("blocked qty has been set to the received qty " + blockedQty);
          OBDal.getInstance().save(implRecord);
        }
      }
    }

  }

  public static boolean getGrnOnDoc(String docNo) {
    OBCriteria<ShipmentInOut> shipCrit = OBDal.getInstance().createCriteria(ShipmentInOut.class);
    shipCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, docNo));
    shipCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, false));
    shipCrit.setMaxResults(1);
    if (shipCrit.count() > 0)
      return true;
    else
      return false;
  }
}
