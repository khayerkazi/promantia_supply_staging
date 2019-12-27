package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.masters.data.IbudServerTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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

import com.ibm.icu.text.SimpleDateFormat;

public class PushOrderDetailsToProd implements Process {

  final Logger log = LogManager.getLogger(PushOrderDetailsToProd.class);
  private ProcessLogger logger;
  List<String> errorOrderList = new ArrayList<String>();
  HashMap<String, String> errorOrderMap = new HashMap<String, String>();

  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    logger.logln("Push Order Details To prod Background process has started");
    try {
      OBContext.setAdminMode(true);
      HashMap<String, String> configMap = CommonServiceProvider.checkObConfig();
      if (!configMap.containsKey("Error")) {
        getPurchaseOrder(configMap);
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
      logger.logln("Error while Excuting the Process for Post PO order and Error is: "
          + e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private int sendOrdersToProd(List<Order> orderList, HashMap<String, String> configMap)
      throws Exception {
    String token;
    try {
      token = CommonServiceProvider.generateToken(configMap);
      HashMap<String, HashMap<String, String>> orderSentMapObj = new HashMap<String, HashMap<String, String>>();
      ArrayList<String> orderNoListObj = new ArrayList<>();
      ArrayList<String> ErrorOrderListObj = new ArrayList<>();

      if (!token.contains("Error_TokenGenerator")) {
        JSONObject orderJson = null;
        for (Order order : orderList) {
          if (order.getSWEMSwPostatus().equalsIgnoreCase("SO")
              || order.getSWEMSwPostatus().equalsIgnoreCase("MO")) {
            orderJson = null;
            orderJson = generateJSON(order, configMap);

          }
          if (orderJson != null || order.getSWEMSwPostatus().equalsIgnoreCase("VO")) {
            orderNoListObj.add(order.getDocumentNo());
            sendOrder(order, orderJson, token, orderSentMapObj, configMap);
          } else {
            ErrorOrderListObj.add(order.getDocumentNo());
          }
        }
        if (orderSentMapObj.size() > 0) {
          return updateOrderDetails(orderSentMapObj);

        }
      } else {
        logger.logln("PushOrderDetailsToProd: Error in generating token " + token);
        log.error("PushOrderDetailsToProd: Error in generating token " + token);
        throw new OBException("Error in generating token " + token);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error while sending the Orders To Prod and Error is: " + e);
      logger.logln("Error while sending the Orders To Prod and Error is: " + e.getMessage());
      throw new OBException(e.getMessage());
    }
    return 1;
  }

  private int updateOrderDetails(HashMap<String, HashMap<String, String>> orderSentMapObjList) {
    log.info("PushOrderDetailsToProd:Updating sent order details");
    int count = 0;
    try {
      for (Entry<String, HashMap<String, String>> orderSentMapObj : orderSentMapObjList.entrySet()) {
        String orderId = orderSentMapObj.getKey();
        HashMap<String, String> orderDetailMapObj = orderSentMapObj.getValue();
        String reference = null;
        Order order = OBDal.getInstance().get(Order.class, orderId);
        String postMsg = "Order Successfully sent ";

        if (orderDetailMapObj.containsKey("orderNumber")) {
          reference = orderDetailMapObj.get("orderNumber");
          order.setOrderReference(reference);
          logger.logln(postMsg + " with document no " + order.getDocumentNo());
          log.info(postMsg + " with document no " + order.getDocumentNo());

        }
        if (orderDetailMapObj.containsKey("anomaly")) {
          postMsg = orderDetailMapObj.get("anomaly");
          count++;
          errorOrderList.add(order.getDocumentNo());
          errorOrderMap.put(order.getDocumentNo(), orderDetailMapObj.get("anomaly"));
        }
        if (orderDetailMapObj.containsKey("delete")) {
          postMsg = orderDetailMapObj.get("delete");
          order.setIbudProdStatus("D");

        }
        if (orderDetailMapObj.containsKey("update")) {
          postMsg = orderDetailMapObj.get("update");
        }

        String postatus = null;
        if (orderDetailMapObj.containsKey("postatus")) {
          postatus = orderDetailMapObj.get("postatus");
        }

        if (postatus != null) {
          if (orderDetailMapObj.containsKey("orderNumber"))
            order.setIbudProdStatus("NV");
          order.setSWEMSwPostatus(postatus);

        }
        order.setIbudPoStatusdate(new Date());
        order.setIbudProdMsgPost(postMsg);
        order.setProcessNow(true);
        OBDal.getInstance().commitAndClose();
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
      logger.logln("Error While update OrderDetails and Error is: " + e.getMessage());
      throw new OBException("PushOrderDetailsToProd:Updating:Error while updating data to order");
    }
    return count;
  }

  private void sendOrder(Order order, JSONObject orderJson, String token,
      HashMap<String, HashMap<String, String>> orderSentMapObj, HashMap<String, String> configMap)
      throws OBException, IOException, JSONException {
    StringBuffer sb = new StringBuffer();
    HttpURLConnection HttpUrlConnection = null;
    BufferedReader reader = null;
    String orderReference = null;
    String anomaly = null;
    InputStream is = null;
    if (order.getSWEMSwPostatus().equalsIgnoreCase("SO")) {
      try {

        log.info("PushOrderDetailsToProd : Generating HttpConnection with Prod for Order no: "
            + order.getDocumentNo());
        try {
          URL urlObj = new URL(configMap.get("postOrder_Url")
              + order.getBusinessPartner().getIbudDppNo());
          HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
          HttpUrlConnection.setDoOutput(true);
          HttpUrlConnection.setRequestMethod(configMap.get("postOrder_requestMethod"));
          HttpUrlConnection.setRequestProperty("Accept-Version",
              configMap.get("order_acceptVersion"));
          HttpUrlConnection.setRequestProperty("Content-Type",
              configMap.get("postOrder_contentType"));

          if (!(configMap.get("order_XEnv") == null)) {
            HttpUrlConnection.setRequestProperty("x-env", configMap.get("order_XEnv"));
          }
          HttpUrlConnection.setRequestProperty("Authorization",
              configMap.get("order_authorization") + " " + token);
          HttpUrlConnection.connect();

        } catch (Exception e) {
          e.printStackTrace();
          log.error("Error while Generating HTTP connection with prod, please check POST API credentials in Openbravo.properties and error is: "
              + e);
          logger
              .logln("Error while Generating HTTP connection with prod, please check POST API credentials in Openbravo.properties and error is: "
                  + e);
          throw new OBException(
              "Error while Generating HTTP connection with prod, please check POST API credentials in Openbravo.properties and error is: "
                  + e);

        }

        try (OutputStream os = HttpUrlConnection.getOutputStream()) {
          byte[] input = orderJson.toString().getBytes(StandardCharsets.UTF_8);
          os.write(input, 0, input.length);
          os.flush();
        }

        if (HttpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          log.info("PushOrderDetailsToProd :Generating response from prod for created order ");
          is = HttpUrlConnection.getInputStream();
          reader = new BufferedReader(new InputStreamReader((is)));
          String tmpStr = null;
          String result = null;
          while ((tmpStr = reader.readLine()) != null) {
            result = tmpStr;
          }
          JSONObject responseJson = new JSONObject(result);
          orderReference = responseJson.getString("orderNumber");
          anomaly = responseJson.getString("anomaly");

          if (!responseJson.isNull("orderNumber")) {
            HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
            if (orderDetailMapObj == null) {
              orderDetailMapObj = new HashMap<String, String>();
              orderDetailMapObj.put("orderNumber", orderReference);
              orderDetailMapObj.put("postatus", "OS");
              orderSentMapObj.put(order.getId(), orderDetailMapObj);
              log.info("Order with documentNo-" + order.getDocumentNo() + " sent to prod.com");
            } else {
              orderDetailMapObj.put("postatus", "OS");
              orderDetailMapObj.put("orderNumber", orderReference);
            }
          } else {
            HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
            if (orderDetailMapObj == null) {
              orderDetailMapObj = new HashMap<String, String>();
              orderDetailMapObj.put("anomaly", anomaly);
              orderSentMapObj.put(order.getId(), orderDetailMapObj);
            } else {
              orderDetailMapObj.put("anomaly", anomaly);
            }
          }
          if (is != null)
            is.close();
        } else {
          InputStreamReader isReader = new InputStreamReader(HttpUrlConnection.getErrorStream());
          BufferedReader bufferedReader = new BufferedReader(isReader);
          if (bufferedReader != null) {
            int cp;
            while ((cp = bufferedReader.read()) != -1) {
              sb.append((char) cp);
            }
            bufferedReader.close();
          }
          isReader.close();

          JSONObject responseJson = new JSONObject(sb.toString());
          anomaly = responseJson.getString("anomaly") + " - " + HttpUrlConnection.getResponseCode();
          HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
          if (orderDetailMapObj == null) {
            orderDetailMapObj = new HashMap<String, String>();
            orderDetailMapObj.put("anomaly", anomaly);
            orderSentMapObj.put(order.getId(), orderDetailMapObj);

          } else {
            orderDetailMapObj.put("anomaly", anomaly);
          }
          logger.logln("Error while sending order " + order.getDocumentNo() + " with Response "
              + HttpUrlConnection.getResponseCode() + " and Response Message: "
              + HttpUrlConnection.getResponseMessage() + " Response Error Is :"
              + responseJson.getString("anomaly") != null ? responseJson.getString("anomaly")
              : "NA");
          log.error("Error while sending order " + order.getDocumentNo() + " with Response "
              + HttpUrlConnection.getResponseCode() + " and Response Message: "
              + HttpUrlConnection.getResponseMessage() + " Response Error Is :"
              + responseJson.getString("anomaly") != null ? responseJson.getString("anomaly")
              : "NA");
        }
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Error while Sending the order to Prod.com for order no: "
            + order.getDocumentNo() + " and Error is: " + e);
        logger.logln("Error while Sending the order to Prod.com for order no: "
            + order.getDocumentNo() + " and Error is: " + e);
      } finally {
        try {
          if (is != null) {
            is.close();
          }
          if (HttpUrlConnection != null) {
            HttpUrlConnection.disconnect();
          }
        } catch (Exception e) {
          e.printStackTrace();
          log.error("Error while disconnecting http connection " + e.getMessage());
          logger.logln("Error while disconnecting http connection " + e.getMessage());
        }
      }
    } else if (order.getSWEMSwPostatus().equalsIgnoreCase("VO")) {
      try {
        // Order Deletion code
        log.info("PushOrderDetails:Delete API : Generating HttpConnection with Prod to delete Order no: "
            + order.getDocumentNo());

        URL urlObj;

        urlObj = new URL(configMap.get("deleteOrder_url")
            + order.getBusinessPartner().getIbudDppNo() + "/" + order.getOrderReference());

        try {
          HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
          HttpUrlConnection.setDoOutput(true);
          HttpUrlConnection.setRequestProperty("Accept-Version",
              configMap.get("order_acceptVersion"));
          HttpUrlConnection.setRequestProperty("Authorization",
              configMap.get("order_authorization") + " " + token);
          HttpUrlConnection.setRequestMethod("DELETE");
          HttpUrlConnection.connect();
        } catch (IOException e) {
          e.printStackTrace();
          log.error("PushOrderDetails:Delete API:Error while Generating HTTP connection with prod, please check POST API credentials in Openbravo.properties and error is: "
              + e);
          logger
              .logln("PushOrderDetails:Delete API:Error while Generating HTTP connection with prod, please check POST API credentials in Openbravo.properties and error is: "
                  + e);
          throw new OBException(
              "PushOrderDetails:Delete API:Error while Generating HTTP connection with prod, please check POST API credentials in Openbravo.properties and error is: "
                  + e);

        }

        if (HttpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          log.info("PushOrderDetailsToProd:Delete API:Generating response from prod for created order ");
          is = HttpUrlConnection.getInputStream();
          reader = new BufferedReader(new InputStreamReader((is)));
          String tmpStr = null;
          String result = null;
          while ((tmpStr = reader.readLine()) != null) {
            result = tmpStr;
          }
          log.info("Order successfully gets deleted from prod.com" + order.getDocumentNo());
          if (result == null) {
            HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
            if (orderDetailMapObj == null) {
              orderDetailMapObj = new HashMap<String, String>();
              orderDetailMapObj.put("delete", "order successfully deleted from prod.com");
              orderDetailMapObj.put("postatus", "OC");
              orderDetailMapObj.put("prodStatus", "D");
              orderSentMapObj.put(order.getId(), orderDetailMapObj);
              log.info("Order with documentNo-" + order.getDocumentNo() + " deleted from prod.com");
              logger.log("Order with documentNo-" + order.getDocumentNo()
                  + " deleted from prod.com");

            } else {
              orderDetailMapObj.put("postatus", "OC");
              orderDetailMapObj.put("prodStatus", "D");
              orderDetailMapObj.put("delete", "order successfully deleted from prod.com");
            }
          }

        } else {
          StringBuilder logMsg = new StringBuilder(" ");
          GetPODetails poDetails = new GetPODetails();

          InputStreamReader isReader = new InputStreamReader(HttpUrlConnection.getErrorStream());
          BufferedReader bufferedReader = new BufferedReader(isReader);
          if (bufferedReader != null) {
            int cp;
            while ((cp = bufferedReader.read()) != -1) {
              sb.append((char) cp);
            }
            bufferedReader.close();
          }
          isReader.close();

          JSONObject responseJson = new JSONObject(sb.toString());
          anomaly = responseJson.getString("error_description") + " - "
              + HttpUrlConnection.getResponseCode();
          if (!anomaly.equalsIgnoreCase(null)) {
            HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
            if (orderDetailMapObj == null) {
              orderDetailMapObj = new HashMap<String, String>();
              orderDetailMapObj.put("anomaly", anomaly);
              orderSentMapObj.put(order.getId(), orderDetailMapObj);
              log.info("Order with documentNo-" + order.getDocumentNo()
                  + " is not deleted from prod.com due to " + anomaly);
              logger.log("Order with documentNo-" + order.getDocumentNo()
                  + " is not deleted from prod.com due to " + anomaly);
            } else {
              orderDetailMapObj.put("anomaly", anomaly);
            }
          }
          logger.logln("Error while deleting order " + order.getDocumentNo() + " with Response "
              + HttpUrlConnection.getResponseCode() + " and Response Message: "
              + HttpUrlConnection.getResponseMessage() + " Response Error Is :"
              + responseJson.getString("error_description") != null ? responseJson
              .getString("error_description") : "NA");
          log.error("Error while deleting order " + order.getDocumentNo() + " with Response "
              + HttpUrlConnection.getResponseCode() + " and Response Message: "
              + HttpUrlConnection.getResponseMessage() + " Response Error Is :"
              + responseJson.getString("error_description") != null ? responseJson
              .getString("error_description") : "NA");
        }

      } catch (Exception e) {
        e.printStackTrace();
        logger.logln("Error while deleting order and error is " + e.getMessage());
        log.error("Error while deleting order and error is " + e.getMessage());

      } finally {
        try {
          if (is != null) {
            is.close();
          }
          if (HttpUrlConnection != null) {
            HttpUrlConnection.disconnect();
          }
        } catch (Exception e) {
          e.printStackTrace();
          log.error("Error while disconnecting http connection " + e.getMessage());
          logger.logln("Error while disconnecting http connection " + e.getMessage());
        }
      }
    }
    // code for order update
    else if (order.getSWEMSwPostatus().equalsIgnoreCase("MO")) {
      try {
        log.info("PushOrderDetailsToProd:Update API : Generating HttpConnection with Prod for Order no: "
            + order.getDocumentNo());
        try {
          URL urlObj = new URL(configMap.get("updateOrder_url") + "/"
              + order.getBusinessPartner().getIbudDppNo() + "/" + order.getOrderReference());
          HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
          HttpUrlConnection.setDoOutput(true);
          HttpUrlConnection.setRequestMethod("PUT");
          HttpUrlConnection.setRequestProperty("Accept-Version",
              configMap.get("order_acceptVersion"));
          HttpUrlConnection.setRequestProperty("Content-Type",
              configMap.get("postOrder_contentType"));
          HttpUrlConnection.setRequestProperty("Authorization",
              configMap.get("order_authorization") + " " + token);
          HttpUrlConnection.connect();

        } catch (Exception e) {
          e.printStackTrace();
          log.error("Error while Generating HTTP connection with prod, please check Update API credentials in Openbravo.properties and error is: "
              + e);
          logger
              .logln("Error while Generating HTTP connection with prod, please check Update API credentials in Openbravo.properties and error is: "
                  + e);
          throw new OBException(
              "Error while Generating HTTP connection with prod, please check Update API credentials in Openbravo.properties and error is: "
                  + e);

        }
        try (OutputStream os = HttpUrlConnection.getOutputStream()) {
          byte[] input = orderJson.toString().getBytes(StandardCharsets.UTF_8);
          os.write(input, 0, input.length);
          os.flush();
        }

        if (HttpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          log.info("PushOrderDetailsToProd:Update API:Generating response from prod for created order ");
          is = HttpUrlConnection.getInputStream();
          reader = new BufferedReader(new InputStreamReader((is)));
          String tmpStr = null;
          String result = null;
          while ((tmpStr = reader.readLine()) != null) {
            result = tmpStr;
          }
          log.info("Order successfully gets updated in prod.com" + order.getDocumentNo());
          if (result == null) {
            HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
            if (orderDetailMapObj == null) {
              orderDetailMapObj = new HashMap<String, String>();
              orderDetailMapObj.put("update", "Order successfully updated in prod.com");
              orderDetailMapObj.put("postatus", "OU");

              orderSentMapObj.put(order.getId(), orderDetailMapObj);
              log.info("Order with documentNo-" + order.getDocumentNo() + " updated in prod.com ");
              logger
                  .log("Order with documentNo-" + order.getDocumentNo() + " updated in prod.com ");

            } else {
              orderDetailMapObj.put("postatus", "OU");
              orderDetailMapObj.put("update", "order successfully updated in prod.com");
            }
          }

        } else {
          StringBuilder logMsg = new StringBuilder(" ");
          GetPODetails poDetails = new GetPODetails();

          InputStreamReader isReader = new InputStreamReader(HttpUrlConnection.getErrorStream());
          BufferedReader bufferedReader = new BufferedReader(isReader);
          if (bufferedReader != null) {
            int cp;
            while ((cp = bufferedReader.read()) != -1) {
              sb.append((char) cp);
            }
            bufferedReader.close();
          }
          isReader.close();

          JSONObject responseJson = new JSONObject(sb.toString());
          anomaly = responseJson.getString("error_description") + " - "
              + HttpUrlConnection.getResponseCode();
          if (!anomaly.equalsIgnoreCase(null)) {
            HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
            if (orderDetailMapObj == null) {
              orderDetailMapObj = new HashMap<String, String>();
              orderDetailMapObj.put("anomaly", anomaly);
              orderSentMapObj.put(order.getId(), orderDetailMapObj);
              log.info("Order with documentNo-" + order.getDocumentNo()
                  + " is not updated in prod.com due to " + anomaly);
              logger.log("Order with documentNo-" + order.getDocumentNo()
                  + " is not updated in prod.com due to " + anomaly);
            } else {
              orderDetailMapObj.put("anomaly", anomaly);
            }
          }

          logger.logln("Error while updating order " + order.getDocumentNo() + " with Response "
              + HttpUrlConnection.getResponseCode() + " and Response Message: "
              + HttpUrlConnection.getResponseMessage() + " Response Error Is :"
              + responseJson.getString("error_description") != null ? responseJson
              .getString("error_description") : "NA");
          log.error("Error while deleting order " + order.getDocumentNo() + " with Response "
              + HttpUrlConnection.getResponseCode() + " and Response Message: "
              + HttpUrlConnection.getResponseMessage() + " Response Error Is :"
              + responseJson.getString("error_description") != null ? responseJson
              .getString("error_description") : "NA");
        }
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Error while updating the order in Prod.com for order no: "
            + order.getDocumentNo() + " and Error is: " + e);
        logger.logln("Error while updating the order to Prod.com for order no: "
            + order.getDocumentNo() + " and Error is: " + e);
      } finally {
        try {
          if (is != null) {
            is.close();
          }
          if (HttpUrlConnection != null) {
            HttpUrlConnection.disconnect();
          }
        } catch (Exception e) {
          e.printStackTrace();
          log.error("Error while disconnecting http connection " + e.getMessage());
          logger.logln("Error while disconnecting http connection " + e.getMessage());
        }
      }

    }
  }

  private JSONObject generateJSON(Order order, HashMap<String, String> configMap) {
    JSONObject orderObject = new JSONObject();
    ArrayList<String> missingFields = new ArrayList<>();
    try {
      if (order.getSWEMSwPostatus().equalsIgnoreCase("SO")) {
        orderObject.put("origin", configMap.get("postOrder_origin"));
        orderObject.put("externalNumber", Integer.parseInt(order.getDocumentNo()));
        orderObject.put("orderType", configMap.get("postOrder_orderType"));
        orderObject.put("orderStatus", configMap.get("postOrder_orderStatus"));
        orderObject.put("orderCreation", formatter.format(order.getCreationDate()).toString()
            .substring(0, 19)
            + "Z");
        if (order.getSWEMSwExpdeldate() != null) {
          orderObject.put("requestedDeliveryDate", formatter.format(order.getSWEMSwExpdeldate())
              .toString().substring(0, 19)
              + "Z");
        } else {
          missingFields.add("Requested Delivery Date");
        }

        if (order.getScheduledDeliveryDate() != null) {
          orderObject.put("scheduledDeliveryDate",
              formatter.format(order.getScheduledDeliveryDate()).toString().substring(0, 19) + "Z");
        } else {
          missingFields.add("Schedule Delivery Date");
        }
        if (order.getBusinessPartner().getIbudDppNo() == null) {
          missingFields.add("Prod Supplier DPP for Business Partner: "
              + order.getBusinessPartner().getName());

        }
        if (order.getBusinessPartner().getClSupplierno() == null) {
          missingFields.add("Prod Supplier number for Business Partner: "
              + order.getBusinessPartner().getName());

        }
        if (missingFields.size() > 0) {
          log.error("For order " + missingFields + " is null with documentno"
              + order.getDocumentNo());
          logger.logln("Skipping Order due to " + missingFields + " is null for PO Document no: "
              + order.getDocumentNo());
          return null;
        }
        JSONArray customerJsonArray = new JSONArray();
        customerJsonArray.put(configMap.get("postOrder_customerKey"));
        customerJsonArray.put(configMap.get("postOrder_customerId"));
        customerJsonArray.put(configMap.get("postOrder_customerId"));
        orderObject.put("customer", customerJsonArray);

        JSONArray supplierJsonArray = new JSONArray();
        supplierJsonArray.put(configMap.get("postOrder_supplierKey"));
        supplierJsonArray.put(order.getBusinessPartner().getClSupplierno());
        supplierJsonArray.put(order.getBusinessPartner().getClSupplierno());
        orderObject.put("supplier", supplierJsonArray);

        JSONArray deliveryJsonArray = new JSONArray();
        deliveryJsonArray.put(configMap.get("postOrder_deliveryKey"));
        deliveryJsonArray.put(configMap.get("postOrder_deliveryId"));
        deliveryJsonArray.put(configMap.get("postOrder_deliveryId"));
        orderObject.put("delivery", deliveryJsonArray);

        JSONArray orderLineArray = new JSONArray();

        for (OrderLine lines : order.getOrderLineList()) {
          log.info("Getting lines for order " + order.getDocumentNo());
          JSONObject orderlineJson = new JSONObject();

          orderlineJson.put("number", lines.getLineNo());
          orderlineJson.put("status", "A");
          orderlineJson.put("item", Long.parseLong(lines.getProduct().getName()));
          orderlineJson.put("quantity", lines.getOrderedQuantity());
          orderlineJson.put("cessionPrice", lines.getSwFob());
          orderlineJson.put("currency", lines.getCurrency().getISOCode());
          orderLineArray.put(orderlineJson);
        }
        orderObject.put("orderLines", orderLineArray);
        log.info("Generated JSON for order  " + orderObject.toString());
      }
      // for update order JSON
      else if (order.getSWEMSwPostatus().equalsIgnoreCase("MO")) {

        if (order.getSWEMSwExpdeldate() != null) {
          orderObject.put("requestedDeliveryDate", formatter.format(order.getSWEMSwExpdeldate())
              .toString().substring(0, 19)
              + "Z");
        } else {
          missingFields.add("Requested Delivery Date");
        }
        if (order.getScheduledDeliveryDate() != null) {
          orderObject.put("scheduledDeliveryDate",
              formatter.format(order.getScheduledDeliveryDate()).toString().substring(0, 19) + "Z");
        } else {
          missingFields.add("Schedule Delivery Date");
        }
        if (order.getBusinessPartner().getIbudDppNo() == null) {
          missingFields.add("Prod Supplier DPP for Business Partner: "
              + order.getBusinessPartner().getName());
        }
        if (missingFields.size() > 0) {
          log.error("For order " + missingFields + " is null with documentno"
              + order.getDocumentNo());
          logger.logln("Skipping Order due to " + missingFields + " is null for PO Document no: "
              + order.getDocumentNo());
          return null;
        }

        JSONArray orderLineArray = new JSONArray();
        for (OrderLine lines : order.getOrderLineList()) {
          log.info("Getting lines for order " + order.getDocumentNo());
          JSONObject orderlineJson = new JSONObject();
          orderlineJson.put("number", lines.getLineNo());
          orderlineJson.put("quantity", lines.getOrderedQuantity());
          orderLineArray.put(orderlineJson);
        }
        orderObject.put("orderLines", orderLineArray);
        log.info("Update API:Generated JSON for updating order  " + orderObject.toString());

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("PushOrderDetailsToProd : Error while generating JSON object for order "
          + order.getDocumentNo() + " and error is: " + e);
      logger.logln("PushOrderDetailsToProd : Error while generating JSON object for order "
          + order.getDocumentNo() + " and error is: " + e);
      orderObject = null;
    }
    return orderObject;

  }

  private void getPurchaseOrder(HashMap<String, String> configMap) {

    try {
      IbudServerTime newIbudServiceObj = CommonServiceProvider.getIbudUpdatedTime("PostPOOrder",
          null);
      Date ibud = newIbudServiceObj.getLastupdated();
      log.info("PushOrderDetailsToProd : Getting list of orders to be sent");

      String strHql = "  select distinct o from OrderLine ol join ol.salesOrder o join o.businessPartner bp "
          + "    where o.sWEMSwPostatus in ('SO','VO','MO') "
          + "    and  bp.clSupplierno = ol.sWEMSwSuppliercode  "
          + "    and  o.transactionDocument.id ='C7CD4AC8AC414678A525AB7AE20D718C'  "
          + "    and  o.imsapDuplicatesapPo != 'Y' "
          + "    and bp.rCSource = 'DPP' "
          + "    and (o.updated >= '" + ibud + "' or ol.updated >= '" + ibud + "') ";

      Query query = OBDal.getInstance().getSession().createQuery(strHql);
      List<Order> orderList = query.list();

      if (orderList != null && orderList.size() > 0) {
        logger.logln("Pushing the Orders from OB to Prod.com from date: " + ibud
            + " and count is: " + orderList.size());
        log.info("Pushing the Orders from OB to Prod.com from date: " + ibud + " and count is: "
            + orderList.size());
        int errorCount = sendOrdersToProd(orderList, configMap);
        if (errorCount == 0) {
          newIbudServiceObj.setLastupdated(new Date());
          OBDal.getInstance().save(newIbudServiceObj);
          SessionHandler.getInstance().commitAndStart();
        } else {
          logger.logln("Order Error count is: " + errorCount + " and Order numbers is :"
              + errorOrderList + " and error is " + errorOrderMap);
        }
      } else {
        logger.logln("No Pending order for pushed from OB to Prod.com from date: " + ibud);
        log.info("No Pending order for pushed from OB to Prod.com from date: " + ibud);
        newIbudServiceObj.setLastupdated(new Date());
        OBDal.getInstance().save(newIbudServiceObj);
        SessionHandler.getInstance().commitAndStart();
      }

    } catch (Exception e) {
      e.printStackTrace();
      logger.logln("Error while Processing the order and error is: " + e);
      log.error("Error while Processing the order and error is: " + e);
      throw new OBException("Error while Processing the order and error is: " + e);
    }
  }
}
