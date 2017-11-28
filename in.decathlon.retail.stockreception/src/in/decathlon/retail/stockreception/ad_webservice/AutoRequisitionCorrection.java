package in.decathlon.retail.stockreception.ad_webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.web.WebService;

public class AutoRequisitionCorrection implements WebService {
  // HashSet<ShipmentInOut> shipmentInOuts = new HashSet<ShipmentInOut>();
  HashSet<ShipmentInOut> shipmentInOuts = null;
  static Logger log4j = Logger.getLogger(AutoRequisitionCorrection.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String itemDetailsFromWS = request.getParameter("itemdetails");
    if (itemDetailsFromWS == null)
      throw new IllegalArgumentException("ItemDetails are Mandatory");
    log4j.info("Auto Requisition Correction Started");
    /*
     * String[] str_array = documentNo.split("::&"); String documentno = str_array[0]; int index =
     * documentno.length(); String Actual = documentNo.substring(index + 3);
     */

  }

  private BaseOBObject processImportRequisitionCorrection(String itemdetails, String email)
      throws Exception {
    ShipmentInOut goodsReceipt = null;
    String boxNumber = null;
    String itemCode = null;
    String strQtyReceived = null;
    String orderLine = null;
    Map<String, List<String[]>> mapValue = new HashMap<String, List<String[]>>();
    try {
      JSONArray documentData = new JSONArray(itemdetails);

      int n = documentData.length();
      for (int i = 0; i < n; i++) {

        final JSONObject itemDetails = documentData.getJSONObject(i);
        boxNumber = itemDetails.getString("boxNo");
        orderLine = itemDetails.getString("orderLine");
        itemCode = itemDetails.getString("itemCode");
        strQtyReceived = itemDetails.getString("qtyReceived");
        log4j.info("Document Number" + boxNumber);
        log4j.info("Itemcode" + itemCode);
        log4j.info("Qty Received" + strQtyReceived);
        log4j.debug("Orderline" + orderLine);
        List<ShipmentInOut> shipmentList = null;
        OBCriteria<ShipmentInOut> criteriaorder = OBDal.getInstance().createCriteria(
            ShipmentInOut.class);
        criteriaorder.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, boxNumber + "*IRC"));

        shipmentList = criteriaorder.list();

        Integer inoutCount1 = shipmentList.size();
        if (inoutCount1 > 0) {
          log4j.info("Duplicate IRC Avoided");
        } else {

          if (!mapValue.containsKey(boxNumber))
            mapValue.put(boxNumber, new ArrayList<String[]>());

          mapValue.get(boxNumber).add(new String[] { itemCode, strQtyReceived, orderLine });

        }
      }
      for (Entry<String, List<String[]>> entry : mapValue.entrySet()) {
        String documentno = entry.getKey();
        List<String[]> elements = new ArrayList<String[]>();
        elements.addAll(entry.getValue());
        String itemcode = null;
        String qtyreceived = null;
        for (String[] str : elements) {
          itemcode = str[0];
          qtyreceived = str[1];
          orderLine = str[2];
          goodsReceipt = checkGrnExists(documentno);
          ShipmentInOutLine goodsReceiptLine = checkGrnLineExistsForProduct(goodsReceipt, itemcode,
              orderLine);
          BigDecimal qtyReceived = new BigDecimal(qtyreceived);
          goodsReceiptLine.setIbodtrActmovementqty(qtyReceived);
          goodsReceiptLine.setUpdatedBy(getUser(email));
          OBDal.getInstance().save(goodsReceiptLine);
        }

        shipmentInOuts = new HashSet<ShipmentInOut>();
        shipmentInOuts.add(goodsReceipt);
        Iterator<ShipmentInOut> shipmentIterator = shipmentInOuts.iterator();

        while (shipmentIterator.hasNext()) {
          ShipmentInOut shipmentInOut = (ShipmentInOut) shipmentIterator.next();
          System.out.println(shipmentInOut.getDocumentNo());

          TransferReception transfer = new TransferReception();
          try {
            transfer.processValidation(shipmentInOut, email);
            shipmentInOuts.clear();
            // shipmentInOut = null;
          } catch (Exception e) {
            e.printStackTrace();
            throw e;
          }
        }

      }

    }
    // OBDal.getInstance().flush();

    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    return goodsReceipt;
  }

  private ShipmentInOutLine checkGrnLineExistsForProduct(ShipmentInOut goodsReceipt,
      String itemCode, String orderLine) {

    Product Product = getProduct(itemCode);
    OrderLine orderObj = getOrderLine(orderLine);

    OBCriteria<ShipmentInOutLine> criteriaOnGrn = OBDal.getInstance().createCriteria(
        ShipmentInOutLine.class);
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, goodsReceipt));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_PRODUCT, Product));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderObj));

    List<ShipmentInOutLine> inOutLines = criteriaOnGrn.list();
    if (inOutLines != null && inOutLines.size() > 0) {
      if (inOutLines.size() > 1)
        throw new OBException("More than one Line for same GRN " + goodsReceipt.getDocumentNo());
      return inOutLines.get(0);
    }
    throw new OBException("Grn line with Product " + Product
        + " in Goods receipt for docuement no " + goodsReceipt.getDocumentNo() + " does not exists");
  }

  private OrderLine getOrderLine(String orderLine) {
    OBCriteria<OrderLine> criteriaOnPrd = OBDal.getInstance().createCriteria(OrderLine.class);
    criteriaOnPrd.add(Restrictions.eq(OrderLine.PROPERTY_ID, orderLine));

    List<OrderLine> prdList = criteriaOnPrd.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    }
    throw new OBException(" Order line " + orderLine + "  does not exists");
  }

  private Product getProduct(String itemCode) {
    OBCriteria<Product> criteriaOnPrd = OBDal.getInstance().createCriteria(Product.class);
    criteriaOnPrd.add(Restrictions.eq(Product.PROPERTY_NAME, itemCode));

    List<Product> prdList = criteriaOnPrd.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    }
    throw new OBException(" Product with item code " + itemCode + "  does not exists");
  }

  private ShipmentInOut checkGrnExists(String documentNo) {
    OBCriteria<ShipmentInOut> criteriaOnGrn = OBDal.getInstance().createCriteria(
        ShipmentInOut.class);
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, documentNo.trim()));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "CO"));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_IBODTRISAUTOMATIC, true));
    List<ShipmentInOut> inOuts = criteriaOnGrn.list();
    if (inOuts != null && inOuts.size() > 0) {
      if (inOuts.size() > 1)
        throw new OBException("More than one Goods receipt for same document no " + documentNo);
      return inOuts.get(0);
    }
    throw new OBException("Goods receipt for document no " + documentNo
        + "Not exists/Not Completed/Not an automatic receipt");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String itemDetailsFromWS = "";
    String strRead = "";

    InputStreamReader isReader = new InputStreamReader(request.getInputStream());
    BufferedReader bReader = new BufferedReader(isReader);
    StringBuilder strBuilder = new StringBuilder();
    strRead = bReader.readLine();
    while (strRead != null) {
      strBuilder.append(strRead);
      strRead = bReader.readLine();
    }

    String[] valueFromWS = strBuilder.toString().split("@@&");
    itemDetailsFromWS = valueFromWS[0];
    String email = valueFromWS[1];
    JSONArray documentData = new JSONArray(itemDetailsFromWS);
    processImportRequisitionCorrection(itemDetailsFromWS, email);

  }

  private User getUser(String email) {

    OBCriteria<User> userCrit = OBDal.getInstance().createCriteria(User.class);
    userCrit.add(Restrictions.eq(User.PROPERTY_EMAIL, email));
    List<User> userCritList = userCrit.list();
    if (userCritList != null && userCritList.size() > 0) {
      return userCritList.get(0);
    } else {
      throw new OBException("user not found");
    }
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
