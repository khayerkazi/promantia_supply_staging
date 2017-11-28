/************************************************************************************ 
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.packing;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOMConversion;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class PackingActionHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(PackingActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    JSONObject response = null;
    try {
      jsonRequest = new JSONObject(content);
      response = new JSONObject();
      final String action = jsonRequest.getString("action");
      if ("open".equals(action)) {
        final String recordId = jsonRequest.getString("recordId");
        response = getGridData(recordId);
      } else if ("openHeader".equals(action)) {
        final String recordId = jsonRequest.getString("recordId");
        response = getGridDataHeader(recordId);
      } else if ("process".equals(action)) {
        final String shipmentId = jsonRequest.getString("shipmentId");
        final int boxNo = jsonRequest.getInt("boxNo");
        final JSONArray data = jsonRequest.getJSONArray("data");
        final boolean calculateWeight = jsonRequest.getBoolean("value");
        response = process(shipmentId, boxNo, data, calculateWeight);
      } else if ("processHeader".equals(action)) {
        final String packId = jsonRequest.getString("shipmentId");
        final int boxNo = jsonRequest.getInt("boxNo");
        final JSONArray data = jsonRequest.getJSONArray("data");
        final boolean calculateWeight = jsonRequest.getBoolean("value");
        response = processHeader(packId, boxNo, data, calculateWeight);
      }

    } catch (Exception e) {
      log.error("Error in ValidateActionHandler", e);
      try {
        response = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "TYPE_ERROR");
        errorMessage.put("text", message);
        response.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error generating the error message", e2);
        // do nothing, give up
      }
    }
    return response;
  }

  private JSONObject getGridData(String recordId) {
    ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, recordId);
    JSONObject response = new JSONObject();
    JSONArray data = new JSONArray();
    JSONObject item = null;
    OBContext.setAdminMode();
    try {
      OBCriteria<ShipmentInOutLine> critShipLines = OBDal.getInstance().createCriteria(
          ShipmentInOutLine.class);
      critShipLines.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipment));

      ProjectionList projections = Projections.projectionList();
      projections.add(Projections.sum(ShipmentInOutLine.PROPERTY_MOVEMENTQUANTITY));
      projections.add(Projections.groupProperty(ShipmentInOutLine.PROPERTY_PRODUCT));
      critShipLines.setProjection(projections);

      boolean hasBoxes = !shipment.getOBWPACKBoxList().isEmpty();
      @SuppressWarnings("rawtypes")
      List products = critShipLines.list();
      int queryCount = 0;
      for (Object o : products) {
        final Object[] qryRecord = (Object[]) o;
        Product product = (Product) qryRecord[1];
        if (!"I".equals(product.getProductType())) {
          continue;
        }
        // not show items that are non-stocked BOM
        if ("I".equals(product.getProductType()) && product.isBillOfMaterials()
            && !product.isStocked()) {
          continue;
        }
        queryCount++;

        item = new JSONObject();
        item.put("quantity", qryRecord[0]);
        item.put("qtyPending", qryRecord[0]);

        if (StringUtils.isNotEmpty(product.getUPCEAN())) {
          item.put("barcode", product.getUPCEAN());
        } else {
          item.put("barcode", "");
        }
        item.put("product", product.getName());
        item.put("productId", product.getId());
        if (hasBoxes) {
          int boxTotalNo = setProductBoxes(item, product, shipment);
          item.put("qtyPending", 0);
          item.put("boxed", qryRecord[0]);
          response.put("boxNo", boxTotalNo);
        }
        data.put(item);
      }
      response.put("startRow", 0);
      response.put("endRow", (queryCount == 0 ? 0 : queryCount - 1));
      response.put("totalRows", queryCount);
      response.put("data", data);
      response.put("windowId", "169");

      String val = "";
      try {
        val = Preferences.getPreferenceValue("OBWPACK_CalculateWeight", true, shipment.getClient(),
            shipment.getOrganization(), OBContext.getOBContext().getUser(), OBContext
                .getOBContext().getRole(), null);
      } catch (PropertyException e) {
        log.error("Error retrieving preference", e);
      }

      if ("Y".equals(val)) {
        response.put("valuecheck", true);
      } else {
        response.put("valuecheck", false);
      }

      if (shipment.getObwpackPackingh() == null) {
        response.put("headerStatus", "DR");
      } else {
        response.put("headerStatus", shipment.getObwpackPackingh().getProcessed());
      }

    } catch (JSONException e) {
      log.error("Error retrieving data", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return response;
  }

  private JSONObject getGridDataHeader(String recordId) {
    Packing pack = OBDal.getInstance().get(Packing.class, recordId);
    // ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, recordId);
    JSONObject response = new JSONObject();
    JSONArray data = new JSONArray();
    JSONObject item = null;
    OBContext.setAdminMode();
    try {

      OBCriteria<ShipmentInOutLine> critShipLines = OBDal.getInstance().createCriteria(
          ShipmentInOutLine.class);
      critShipLines.add(Restrictions.in(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT,
          pack.getMaterialMgmtShipmentInOutEMObwpackPackinghIDList()));
      ProjectionList projections = Projections.projectionList();
      projections.add(Projections.sum(ShipmentInOutLine.PROPERTY_MOVEMENTQUANTITY));
      projections.add(Projections.groupProperty(ShipmentInOutLine.PROPERTY_PRODUCT));
      critShipLines.setProjection(projections);

      boolean hasBoxes = !pack.getOBWPACKBoxList().isEmpty();
      @SuppressWarnings("rawtypes")
      List products = critShipLines.list();
      int queryCount = 0;
      for (Object o : products) {
        final Object[] qryRecord = (Object[]) o;
        Product product = (Product) qryRecord[1];
        if (!"I".equals(product.getProductType())) {
          continue;
        }
        // not show items that are non-stocked BOM
        if ("I".equals(product.getProductType()) && product.isBillOfMaterials()
            && !product.isStocked()) {
          continue;
        }
        queryCount++;
        item = new JSONObject();
        item.put("quantity", qryRecord[0]);
        item.put("qtyPending", qryRecord[0]);

        if (StringUtils.isNotEmpty(product.getUPCEAN())) {
          item.put("barcode", product.getUPCEAN());
        } else {
          item.put("barcode", "");
        }
        item.put("product", product.getName());
        item.put("productId", product.getId());
        if (hasBoxes) {

          int boxTotalNo = setProductBoxesHeader(item, product, pack, qryRecord[0]);

          response.put("boxNo", boxTotalNo);

        }
        data.put(item);
      }
      response.put("startRow", 0);
      response.put("endRow", (queryCount == 0 ? 0 : queryCount - 1));
      response.put("totalRows", queryCount);
      response.put("data", data);
      response.put("windowId", "9947FA6F69E64E659CA258E34576E6F4");

      // response.put("valuecheck", preferenceExist());
      String val = "";
      try {
        val = Preferences.getPreferenceValue("OBWPACK_CalculateWeight", true, pack.getClient(),
            pack.getOrganization(), OBContext.getOBContext().getUser(), OBContext.getOBContext()
                .getRole(), null);
      } catch (PropertyException e) {
        log.error("Error retrieving preference", e);
      }

      if ("Y".equals(val)) {
        response.put("valuecheck", true);
      } else {
        response.put("valuecheck", false);
      }

      response.put("headerStatus", pack.getProcessed());

    } catch (JSONException e) {
      log.error("Error retrieving data", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return response;
  }

  private int setProductBoxes(JSONObject item, Product product, ShipmentInOut shipment)
      throws JSONException {
    List<PackingBox> boxes = shipment.getOBWPACKBoxList();
    int boxTotalNo = boxes.size();
    for (int i = 1; i <= boxTotalNo; i++) {
      item.put("box" + i, 0);
    }
    OBCriteria<PackingBoxProduct> critBoxProd = OBDal.getInstance().createCriteria(
        PackingBoxProduct.class);
    critBoxProd.add(Restrictions.eq(PackingBoxProduct.PROPERTY_PRODUCT, product));
    critBoxProd.add(Restrictions.in(PackingBoxProduct.PROPERTY_PACKINGBOX, boxes));
    List<PackingBoxProduct> boxProds = critBoxProd.list();
    for (PackingBoxProduct boxProd : boxProds) {
      long boxNo = boxProd.getPackingBox().getBoxNumber();
      item.put("box" + boxNo, boxProd.getQuantity());
    }
    return boxTotalNo;
  }

  private int setProductBoxesHeader(JSONObject item, Product product, Packing pack, Object qty)
      throws JSONException {
    List<PackingBox> boxes = pack.getOBWPACKBoxList();
    int boxTotalNo = boxes.size();
    for (int i = 1; i <= boxTotalNo; i++) {
      item.put("box" + i, 0);
    }
    OBCriteria<PackingBoxProduct> critBoxProd = OBDal.getInstance().createCriteria(
        PackingBoxProduct.class);
    critBoxProd.add(Restrictions.eq(PackingBoxProduct.PROPERTY_PRODUCT, product));
    critBoxProd.add(Restrictions.in(PackingBoxProduct.PROPERTY_PACKINGBOX, boxes));
    List<PackingBoxProduct> boxProds = critBoxProd.list();
    if (boxProds.size() == 0) {
      item.put("box" + 1, BigDecimal.ZERO);
      item.put("qtyPending", qty);
      item.put("boxed", 0);
    } else {
      item.put("qtyPending", 0);
      item.put("boxed", qty);
    }
    for (PackingBoxProduct boxProd : boxProds) {
      long boxNo = boxProd.getPackingBox().getBoxNumber();
      item.put("box" + boxNo, boxProd.getQuantity());
    }
    return boxTotalNo;
  }

  private JSONObject process(String shipmentId, int boxNo, JSONArray data, boolean calculateWeight) {
    JSONObject response = new JSONObject();

    OBContext.setAdminMode();
    try {
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "TYPE_SUCCESS");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));

      ShipmentInOut shipment = OBDal.getInstance().get(ShipmentInOut.class, shipmentId);
      Organization orgProxy = (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME,
          DalUtil.getId(shipment.getOrganization()));
      List<PackingBox> boxes = shipment.getOBWPACKBoxList();
      HashMap<Integer, String> boxesIDs = new HashMap<Integer, String>();

      if (shipment.getObwpackPackingh() == null) {
        // create Packing header
        Packing pack = OBProvider.getInstance().get(Packing.class);
        pack.setOrganization(shipment.getOrganization());
        pack.setClient(shipment.getClient());
        pack.setPackdate(new Date());
        pack.setBusinessPartner(shipment.getBusinessPartner());
        pack.setPartnerAddress(shipment.getPartnerAddress());
        pack.setProcessed("DR");
        pack.setReactivated("DR");
        final String strDocNo = Utility.getDocumentNo(new DalConnectionProvider(false), shipment
            .getClient().getId(), "obwpack_packingh", true);
        pack.setDocumentNo(strDocNo);
        OBDal.getInstance().save(pack);

        shipment.setObwpackPackingh(pack);
        OBDal.getInstance().save(shipment);
      }

      for (PackingBox box : boxes) {
        boxesIDs.put(box.getBoxNumber().intValue(), box.getId());
        box.getOBWPACKBoxProductList().clear();
        OBDal.getInstance().save(box);
      }
      for (int i = boxes.size() + 1; i <= boxNo; i++) {
        PackingBox box = OBProvider.getInstance().get(PackingBox.class);
        box.setOrganization(orgProxy);
        box.setGoodsShipment(shipment);
        box.setBoxNumber((long) i);
        if (box.getObwpackPackingh() == null && shipment.getObwpackPackingh() != null) {
          box.setObwpackPackingh(shipment.getObwpackPackingh());
        }
        OBDal.getInstance().save(box);
        boxes.add(box);
        boxesIDs.put(i, box.getId());
        OBDal.getInstance().save(shipment);
      }

      for (int i = 0; i < data.length(); i++) {
        JSONObject row = (JSONObject) data.get(i);
        Product product = OBDal.getInstance().get(Product.class, row.getString("productId"));
        for (int j = 1; j <= boxNo; j++) {
          BigDecimal qty = new BigDecimal(row.getDouble("box" + j));
          if (qty.compareTo(BigDecimal.ZERO) != 0) {
            PackingBox box = OBDal.getInstance().get(PackingBox.class, boxesIDs.get(j));
            addRow(box, product, qty, orgProxy);
          }
        }
      }
      // Renumber and delete empty boxes
      // Calculate weight for each box
      OBDal.getInstance().flush();
      long i = 1L;
      for (PackingBox box : getOrderedBoxes(shipment)) {
        OBDal.getInstance().refresh(box);
        BigDecimal boxweight = BigDecimal.ZERO;
        if (box.getOBWPACKBoxProductList().isEmpty()) {
          shipment.getOBWPACKBoxList().remove(box);
        } else {
          box.setWeightcalculated(calculateWeight);
          box.setBoxNumber(i);
          if (calculateWeight) { // only if checkCalculateWeight is checked
            for (PackingBoxProduct p : box.getOBWPACKBoxProductList()) {
              HashMap<String, BigDecimal> list = calculateWeight(boxweight, p.getProduct(),
                  p.getQuantity(), shipment.getOrganization());
              if (list.get("Success") != null) {
                boxweight = list.get("Success");

              } else {
                boxweight = list.get("Warning");
                errorMessage.put("severity", "TYPE_WARNING");
                errorMessage.put("text", OBMessageUtils.messageBD("OBWPACK_roughWeight"));
              }
            }
            box.setWeight(boxweight);
            if (shipment.getOrganization().getObwpackUom() != null) {
              box.setUOM(shipment.getOrganization().getObwpackUom());
            }
          }

          i++;
        }
      }

      response.put("message", errorMessage);
    } catch (JSONException e) {
      log.error("Error retrieving data", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return response;
  }

  private JSONObject processHeader(String packId, int boxNo, JSONArray data, boolean calculateWeight) {
    JSONObject response = new JSONObject();

    OBContext.setAdminMode();
    try {
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "TYPE_SUCCESS");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));

      Packing pack = OBDal.getInstance().get(Packing.class, packId);
      List<PackingBox> boxes = pack.getOBWPACKBoxList();

      HashMap<Integer, String> boxesIDs = new HashMap<Integer, String>();
      for (PackingBox box : boxes) {
        boxesIDs.put(box.getBoxNumber().intValue(), box.getId());
        box.getOBWPACKBoxProductList().clear();
        OBDal.getInstance().save(box);
      }
      OBDal.getInstance().flush();
      for (int i = boxes.size() + 1; i <= boxNo; i++) {
        PackingBox box = OBProvider.getInstance().get(PackingBox.class);
        box.setOrganization(pack.getOrganization());
        box.setObwpackPackingh(pack);
        box.setBoxNumber((long) i);
        OBDal.getInstance().save(box);
        boxes.add(box);
        boxesIDs.put(i, box.getId());
        OBDal.getInstance().save(pack);
      }

      for (int i = 0; i < data.length(); i++) {
        JSONObject row = (JSONObject) data.get(i);
        Product product = OBDal.getInstance().get(Product.class, row.getString("productId"));
        for (int j = 1; j <= boxNo; j++) {

          BigDecimal qty = new BigDecimal(row.getLong("box" + j));
          if (qty.compareTo(BigDecimal.ZERO) != 0) {
            PackingBox box = OBDal.getInstance().get(PackingBox.class, boxesIDs.get(j));
            box.setWeightcalculated(calculateWeight);
            addRow(box, product, qty, pack.getOrganization());
          }
        }
      }
      // Renumber and delete empty boxes
      // Calculate weight for each box
      OBDal.getInstance().flush();
      long i = 1L;
      for (PackingBox box : getOrderedBoxesHeader(pack)) {
        OBDal.getInstance().refresh(box);
        BigDecimal boxweight = BigDecimal.ZERO;
        if (box.getOBWPACKBoxProductList().isEmpty()) {
          pack.getOBWPACKBoxList().remove(box);
        } else {
          box.setBoxNumber(i);
          if (calculateWeight) { // only if checkCalculateWeight is checked
            for (PackingBoxProduct p : box.getOBWPACKBoxProductList()) {
              HashMap<String, BigDecimal> list = calculateWeight(boxweight, p.getProduct(),
                  p.getQuantity(), pack.getOrganization());
              if (list.get("Success") != null) {
                boxweight = list.get("Success");

              } else {
                boxweight = list.get("Warning");
                errorMessage.put("severity", "TYPE_WARNING");
                errorMessage.put("text", OBMessageUtils.messageBD("OBWPACK_roughWeight"));
              }

            }
            box.setWeight(boxweight);
            if (pack.getOrganization().getObwpackUom() != null) {
              box.setUOM(pack.getOrganization().getObwpackUom());
            }
          }
          i++;
        }
      }

      response.put("message", errorMessage);
    } catch (JSONException e) {
      log.error("Error retrieving data", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return response;
  }

  private HashMap<String, BigDecimal> calculateWeight(BigDecimal weight, Product product,
      BigDecimal qty, Organization org) {
    HashMap<String, BigDecimal> list = new HashMap<String, BigDecimal>();
    BigDecimal we = product.getWeight().multiply(qty);
    if (!"I".equals(product.getProductType())) {
      list.put("Success", weight);
      return list;
    }
    if (product.getUOMForWeight() == null || org.getObwpackUom() == null) {
      list.put("Warning", weight);
      return list;
    }
    boolean existConv = false;
    if (org.getObwpackUom() != product.getUOMForWeight()) {
      for (UOMConversion uomConv : product.getUOMForWeight().getUOMConversionList()) {
        if (uomConv.getToUOM() == org.getObwpackUom()) {
          we = we.multiply(uomConv.getMultipleRateBy());
          existConv = true;
          break;
        }
      }
      if (!existConv) {
        list.put("Warning", weight);
        return list;
      }
    }

    weight = weight.add(we);
    list.put("Success", weight);
    return list;
  }

  private void addRow(PackingBox box, Product product, BigDecimal qty, Organization orgProxy) {
    PackingBoxProduct boxProduct = OBProvider.getInstance().get(PackingBoxProduct.class);
    boxProduct.setOrganization(orgProxy);
    boxProduct.setPackingBox(box);
    boxProduct.setProduct(product);
    boxProduct.setUOM(product.getUOM());
    boxProduct.setQuantity(qty);
    OBDal.getInstance().save(boxProduct);
  }

  private List<PackingBox> getOrderedBoxes(ShipmentInOut shipment) {
    OBCriteria<PackingBox> critBoxes = OBDal.getInstance().createCriteria(PackingBox.class);
    critBoxes.add(Restrictions.eq(PackingBox.PROPERTY_GOODSSHIPMENT, shipment));
    critBoxes.addOrderBy(PackingBox.PROPERTY_BOXNUMBER, true);
    return critBoxes.list();
  }

  private List<PackingBox> getOrderedBoxesHeader(Packing pack) {
    OBCriteria<PackingBox> critBoxes = OBDal.getInstance().createCriteria(PackingBox.class);
    critBoxes.add(Restrictions.eq(PackingBox.PROPERTY_OBWPACKPACKINGH, pack));
    critBoxes.addOrderBy(PackingBox.PROPERTY_BOXNUMBER, true);
    return critBoxes.list();
  }
}
