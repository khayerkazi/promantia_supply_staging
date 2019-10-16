package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.masters.data.IbudServerTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

public class GetOrderDetailsFromProd implements Process {

  final Logger log = LogManager.getLogger(GetOrderDetailsFromProd.class);
  private ProcessLogger logger;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    logger.logln("Get Order Details From prod Background process has started");

    try {
      OBContext.setAdminMode(true);
      IbudServerTime newIbudServiceObj = CommonServiceProvider
          .getIbudUpdatedTime("GetOrderProcess");
      Date ibud = newIbudServiceObj.getLastupdated();
      HashMap<String, String> configMap = CommonServiceProvider.checkObConfig();
      if (!configMap.containsKey("Error")) {
        List<Order> orderObjList = getPurchaseOrder(configMap, ibud);
        if (orderObjList != null & orderObjList.size() > 0) {
          logger
              .logln("Getting OB List of Order for Getting Order details from Prod.com from date: "
                  + ibud + " and count is: " + orderObjList.size());
          log.info("Getting OB List of Order for Getting Order details from Prod.com from date: "
              + ibud + " and count is: " + orderObjList.size());
          updateOrderDetails(orderObjList, configMap);
        } else {

          logger.logln("No Pending order for GET from OB to Prod.com from date: " + ibud);
          log.info("No Pending order for GET from OB to Prod.com from date: " + ibud);
          newIbudServiceObj.setLastupdated(new Date());
          OBDal.getInstance().save(newIbudServiceObj);
          SessionHandler.getInstance().commitAndStart();

        }
      } else {
        log.error("Missing Configuration in openbravo properties :" + configMap.get("Error"));
        logger.logln("Missing Configuration in openbravo properties :" + configMap.get("Error"));
        throw new OBException(
            "CheckOBConfig:Following configurations are mission in Openbravo properties : "
                + configMap.get("Error"));

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      logger.logln(e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void updateOrderDetails(List<Order> orderList, HashMap<String, String> configMap)
      throws Exception {
    try {
      String token;
      token = CommonServiceProvider.generateToken(configMap);
      Set<String> errorOrderList = new HashSet<String>();
      if (!token.contains("Error_TokenGenerator")) {
        for (Order order : orderList) {
          updateOrderHeaderDetails(order, token, configMap, errorOrderList);
        }
      } else {
        logger.logln("GetOrderDetailsFromProd: Error in generating token " + token);
        log.error("GetOrderDetailsFromProd: Error in generating token " + token);
        throw new OBException("Error in generating token " + token);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error while Getting the Orders from Prod and Error is: " + e);
      logger.logln("Error while Getting the Orders from Prod and Error is: " + e);
      throw new OBException(e.getMessage());
    }
  }

  private void updateOrderHeaderDetails(Order orderObj, String token,
      HashMap<String, String> configMap, Set<String> errorOrderList) throws Exception {

    String orderHeaderData = GetDataFromProd(orderObj.getOrderReference(),
        orderObj.getDocumentNo(), token, configMap, "Header", null);
    SaveProdHeaderData(orderObj, errorOrderList, orderHeaderData);
    if (!errorOrderList.contains(orderHeaderData)) {
      String orderHeaderLineData = GetDataFromProd(orderObj.getOrderReference(),
          orderObj.getDocumentNo(), token, configMap, "Lines", null);// ProductElp
      SaveProdOrderLineData(orderObj, errorOrderList, orderHeaderLineData, token, configMap);
    }
  }

  private void SaveProdHeaderData(Order orderObj, Set<String> errorOrderList, String orderHeaderData)
      throws JSONException {
    try {

      if (!orderHeaderData.contains("Error_GETAPI:")) {
        JSONArray orderArray = new JSONArray(orderHeaderData.toString());
        if (orderArray != null && orderArray.length() > 0) {
          JSONObject js = orderArray.getJSONObject(0);
          if (js.has("ostOrderStatusOst") && !js.isNull("ostOrderStatusOst")) {
            orderObj.setIbudProdStatus(js.getString("ostOrderStatusOst"));
          }
          String orderStatus = js.getString("ostOrderStatusOst");
          String estShipDate = js.getString("ordExpectedShipmentDate");
          String actShipDate = js.getString("ordLastShipmentDate");
          OBDal.getInstance().save(orderObj);
          SessionHandler.getInstance().commitAndStart();
        } else {
          errorOrderList.add(orderObj.getDocumentNo());
          logger.logln("Data Not Found for Order: " + orderObj.getDocumentNo());
          log.error("Data Not Found for Order: " + orderObj.getDocumentNo());
        }
      } else {
        log.error("Error is:" + orderHeaderData + " for Order: " + orderObj.getDocumentNo());
        logger.logln("Error is:" + orderHeaderData + " for Order: " + orderObj.getDocumentNo());

        errorOrderList.add(orderObj.getDocumentNo());
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.logln("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      log.error("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      errorOrderList.add(orderObj.getDocumentNo());
    }
  }

  private void SaveProdOrderLineData(Order orderObj, Set<String> errorOrderList,
      String orderHeaderData, String token, HashMap<String, String> configMap) throws JSONException {
    try {
      Map<String, OrderLine> elpMap = new HashMap<String, OrderLine>();
      if (!orderHeaderData.contains("Error_GETAPI:")) {
        JSONArray orderLineArray = new JSONArray(orderHeaderData.toString());
        if (orderLineArray != null && orderLineArray.length() > 0) {
          for (OrderLine orderLineObj : orderObj.getOrderLineList()) {
            if (orderLineObj.getProduct() != null
                && orderLineObj.getProduct().getIbudElpelementprodnumelp() != null) {
              elpMap.put(orderLineObj.getProduct().getIbudElpelementprodnumelp(), orderLineObj);
            }
          }

          for (int k = 0; k < orderLineArray.length(); k++) {
            JSONObject js = orderLineArray.getJSONObject(k);
            if (js.has("olnOrderedQuantity") && !js.isNull("olnOrderedQuantity")) {
              js.getString("olnOrderedQuantity");
            }
            String productElpCode = js.getString("elpElementProdNumElp");
            if (elpMap.containsKey(productElpCode)) {
              OrderLine olObj = elpMap.get(productElpCode);
              // olObj.setor
              OBDal.getInstance().save(olObj);
              SessionHandler.getInstance().commitAndStart();
            } else {
              String orderLineElpDetails = GetDataFromProd(orderObj.getOrderReference(),
                  orderObj.getDocumentNo(), token, configMap, "ProductElp", null);
            }
          }
        } else {
          errorOrderList.add(orderObj.getDocumentNo());
          logger.logln("Line data Not Found for Order: " + orderObj.getDocumentNo());
          log.error("Line Data Not Found for Order: " + orderObj.getDocumentNo());
        }
      } else {
        log.error("Error while Getting the line data Error is:" + orderHeaderData + " for Order: "
            + orderObj.getDocumentNo());
        logger.logln("Error while Getting the line data Error is:" + orderHeaderData
            + " for Order: " + orderObj.getDocumentNo());

        errorOrderList.add(orderObj.getDocumentNo());
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.logln("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      log.error("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      errorOrderList.add(orderObj.getDocumentNo());
    }
  }

  private String GetDataFromProd(String orderReferenceNo, String orderNo, String tokenKey,
      HashMap<String, String> configMap, String orderType, String elpProduct) {
    HttpURLConnection conn = null;

    // Map<String, String> resultMap = new HashMap<String, String>();
    URL url = null;
    try {
      /* try { */
      if (orderType.equalsIgnoreCase("Header")) {
        url = new URL(configMap.get("getOrder_url") + configMap.get("prodDPP")
            + "?search=ordOrderNumber=" + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("Lines")) {
        url = new URL(configMap.get("getOrderLine_url") + configMap.get("prodDPP")
            + "/order_lines/=" + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("ProductElp") && elpProduct != null) {
        url = new URL(configMap.get("getElpProduct_url") + elpProduct);
      } else {
        logger.logln("API is not present for " + orderType + " elp: " + elpProduct
            + " of Order no: " + orderNo);
        return "Error_GETAPI: API is not present for " + orderType + " elp: " + elpProduct
            + " of Order no: " + orderNo;
      }
      conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept-Version", configMap.get("order_acceptVersion"));
      conn.setRequestProperty("x-env", configMap.get("order_XEnv"));
      conn.setRequestProperty("Authorization", configMap.get("order_authorization") + " "
          + tokenKey);
      conn.connect();

      /*
       * } catch (Exception e) { e.printStackTrace(); log.error(
       * "updateOrderHeaderDetails:Error while Generating HTTP connection with prod, please check GET API credentials in Openbravo.properties and error is: "
       * + e); logger .logln(
       * "updateOrderHeaderDetails:Error while Generating HTTP connection with prod, please check GET API credentials in Openbravo.properties and error is: "
       * + e); throw new OBException(
       * "updateOrderHeaderDetails:Error while Generating HTTP connection with prod, please check GET API credentials in Openbravo.properties and error is: "
       * + e); }
       */

      if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        int responseCode = conn.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();
        return response.toString();

      } else {
        return "Error_GETAPI:" + orderType + "_" + conn.getResponseCode() + "_"
            + conn.getResponseMessage();

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("GetOrderDetailsFromProd:Error while updating order header details " + e);
      logger.logln("GetOrderDetailsFromProd:Error while updating order header details " + e);
      return "Error_GETAPI: Error While Getting the Order details for order No: " + orderNo
          + " and Type is: " + orderType;
    } finally {
      try {

        if (conn != null) {
          conn.disconnect();
        }
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Error while disconnecting http connection " + e.getMessage());
        logger.logln("Error while disconnecting http connection " + e.getMessage());
        return "Error_GETAPI: Error While Getting the Order details(Finally) for order No: "
            + orderNo + " and Type is: " + orderType;
      }
    }

  }

  private List<Order> getPurchaseOrder(HashMap<String, String> configMap, Date ibud) {

    List<Order> orderList = null;
    try {

      log.info("PushOrderDetailsToProd : Getting list of orders to be sent");
      /*
       * String strHql = "select distinct co from Order co, OrderLine line ," +
       * "   CL_DPP_SEQNO cd ,BusinessPartner bp " +
       * " where co.sWEMSwPostatus in ('OS') and co.id = line.salesOrder.id " +
       * " and bp.clSupplierno = line.sWEMSwSuppliercode     " +
       * " and co.transactionDocument.id = 'C7CD4AC8AC414678A525AB7AE20D718C'  " +
       * " and  co.imsapDuplicatesapPo != 'Y' and bp.rCSource = 'DPP'  " +
       * " and line.orderedQuantity > 0 " + " and co.orderReference is not null " +
       * " and co.updated > '" + ibud + "' ";
       */

      String strHql = "  select distinct o from OrderLine ol join ol.salesOrder o join o.businessPartner bp "
          + "    where o.sWEMSwPostatus in ('OS') "
          + "    and  bp.clSupplierno = ol.sWEMSwSuppliercode  "
          + "    and  o.transactionDocument.id ='C7CD4AC8AC414678A525AB7AE20D718C'  "
          + "    and  o.imsapDuplicatesapPo != 'Y' "
          + "    and bp.rCSource = 'DPP' and co.orderReference is not null "
          + "    and o.updated > '" + ibud + "' ";

      Query query = OBDal.getInstance().getSession().createQuery(strHql);
      orderList = query.list();

      return orderList;

    } catch (Exception e) {
      e.printStackTrace();
      log.error("GetOrderDetailsFromProd:Error while getting order details " + e);
      logger.logln("GetOrderDetailsFromProd:Error while getting order details " + e);

    }
    return null;

  }
}
