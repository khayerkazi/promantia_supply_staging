package in.decathlon.ibud.masters.client;

import java.io.BufferedReader;
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
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
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

  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    logger.logln("Push Order Details To prod Background process has started");
    try {
      OBContext.setAdminMode(true);
      HashMap<String, String> configMap = checkObConfig();
      if (!configMap.containsKey("Error")) {
        List<Order> orderList = getPurchaseOrder();
        if (orderList != null && orderList.size() > 0) {
          sendOrdersToProd(orderList, configMap);
        } else {
          logger.logln("No orders pedning");
          log.info("No orders pedning");
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

  private HashMap<String, String> checkObConfig() throws Exception {

    List<String> errorListObj = new ArrayList<String>();
    HashMap<String, String> outPut = new HashMap<String, String>();

    String tokenUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.url");
    String token_authKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.basic_auth");
    String tokenGrantType = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.grant_type");
    String token_UserName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.username");
    String token_Password = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.password");
    String token_Scope = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.scope");

    String postOrder_Url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.url");
    String postOrder_XEnv = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.x_env");

    String postOrder_customerKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.customerKey");
    String postOrder_customerId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.customerId");
    String postOrder_orderType = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.orderType");
    String postOrder_orderStatus = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.orderStatus");
    String postOrder_origin = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.origin");

    String postOrder_deliveryKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.deliveryKey");
    String postOrder_deliveryId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.deliveryId");

    String postOrder_supplierKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.supplierKey");

    String postOrder_requestMethod = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.requestMethod");

    String postOrder_contentType = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.contentType");

    String postOrder_acceptVersion = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.acceptVersion");

    String postOrder_authorization = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.authorization");

    if (postOrder_authorization == null) {
      errorListObj.add("prod.postorder.authorization");
    } else {
      outPut.put("postOrder_authorization", postOrder_authorization);
    }

    if (postOrder_acceptVersion == null) {
      errorListObj.add("prod.postorder.acceptVersion");
    } else {
      outPut.put("postOrder_acceptVersion", postOrder_acceptVersion);
    }

    if (postOrder_contentType == null) {
      errorListObj.add("prod.postorder.contentType");
    } else {
      outPut.put("postOrder_contentType", postOrder_contentType);
    }

    if (postOrder_requestMethod == null) {
      errorListObj.add("prod.postorder.requestMethod");
    } else {
      outPut.put("postOrder_requestMethod", postOrder_requestMethod);
    }

    if (postOrder_supplierKey == null) {
      errorListObj.add("prod.postorder.supplierKey");
    } else {
      outPut.put("postOrder_supplierKey", postOrder_supplierKey);
    }

    if (postOrder_deliveryKey == null) {
      errorListObj.add("prod.postorder.deliveryKey");
    } else {
      outPut.put("postOrder_deliveryKey", postOrder_deliveryKey);
    }

    if (postOrder_deliveryId == null) {
      errorListObj.add("prod.postorder.deliveryId");
    } else {
      outPut.put("postOrder_deliveryId", postOrder_deliveryId);
    }

    if (postOrder_customerKey == null) {
      errorListObj.add("prod.postorder.customerKey");
    } else {
      outPut.put("postOrder_customerKey", postOrder_customerKey);
    }

    if (postOrder_customerId == null) {
      errorListObj.add("prod.postorder.customerId");
    } else {
      outPut.put("postOrder_customerId", postOrder_customerId);
    }

    if (postOrder_orderType == null) {
      errorListObj.add("prod.postorder.orderType");
    } else {
      outPut.put("postOrder_orderType", postOrder_orderType);
    }

    if (postOrder_orderStatus == null) {
      errorListObj.add("prod.postorder.orderStatus");
    } else {
      outPut.put("postOrder_orderStatus", postOrder_orderStatus);
    }

    if (postOrder_origin == null) {
      errorListObj.add("prod.postorder.origin");
    } else {
      outPut.put("postOrder_origin", postOrder_origin);
    }

    if (postOrder_Url == null) {
      errorListObj.add("prod.postorder.url");
    } else {
      outPut.put("postOrder_Url", postOrder_Url);
    }
    if (postOrder_XEnv == null) {
      errorListObj.add("prod.postorder.x_env");
    } else {
      outPut.put("postOrder_XEnv", postOrder_XEnv);
    }
    if (tokenUrl == null) {
      errorListObj.add("prod.token.Url");
    } else {
      outPut.put("tokenUrl", tokenUrl);
    }
    if (token_authKey == null) {
      errorListObj.add("prod.token.basic_auth");
    } else {
      outPut.put("token_authKey", token_authKey);
    }
    if (tokenGrantType == null) {
      errorListObj.add("prod.token.grant_type");
    } else {
      outPut.put("tokenGrantType", tokenGrantType);
    }
    if (token_UserName == null) {
      errorListObj.add("prod.token.username");
    } else {
      outPut.put("token_UserName", token_UserName);
    }
    if (token_Password == null) {
      errorListObj.add("prod.token.password");
    } else {
      outPut.put("token_Password", token_Password);
    }
    if (token_Scope == null) {
      errorListObj.add("prod.token.scope");
    } else {
      outPut.put("token_Scope", token_Scope);
    }

    if (errorListObj.size() > 0) {
      String errorString = "Error_CommonServiceProvider: " + errorListObj
          + " configuration is missing in Openbravo.properties file";
      outPut.put("Error", errorString);
      logger.logln(errorString);
      log.error(errorString);
    }
    return outPut;

  }

  private void sendOrdersToProd(List<Order> orderList, HashMap<String, String> configMap) {
    String token;
    try {
      token = CommonServiceProvider.generateToken(configMap);
      HashMap<String, HashMap<String, String>> orderSentMapObj = new HashMap<String, HashMap<String, String>>();
      if (!token.contains("Error_TokenGenerator")) {
        for (Order order : orderList) {
          JSONObject orderJson = generateJSON(order, configMap);
          if (orderJson != null)
            sendOrder(order, orderJson, token, orderSentMapObj, configMap);
        }
        if (orderSentMapObj.size() > 0) {
          updateOrderDetails(orderSentMapObj);
        }
      } else {
        logger.logln("PushOrderDetailsToProd: Error in generating token");
        log.error("PushOrderDetailsToProd: Error in generating token");
        throw new OBException("Error in generating token");
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
      logger.logln(e.getMessage());
      throw new OBException(e.getMessage());
    }
  }

  private void updateOrderDetails(HashMap<String, HashMap<String, String>> orderSentMapObjList) {
    log.info("PushOrderDetailsToProd:Updating sent order details");
    try {
      String postMsg = "Order Successfully sent";

      for (Entry<String, HashMap<String, String>> orderSentMapObj : orderSentMapObjList.entrySet()) {
        String orderId = orderSentMapObj.getKey();
        HashMap<String, String> orderDetailMapObj = orderSentMapObj.getValue();
        String reference = null;
        Order order = OBDal.getInstance().get(Order.class, orderId);

        if (orderDetailMapObj.containsKey("orderNumber")) {
          reference = orderDetailMapObj.get("orderNumber");
          order.setOrderReference(reference);
        }
        if (orderDetailMapObj.containsKey("anomaly")) {
          postMsg = orderDetailMapObj.get("anomaly");
        }
        String postatus = null;
        if (orderDetailMapObj.containsKey("postatus")) {
          postatus = orderDetailMapObj.get("postatus");
        }

        if (postatus != null) {
          order.setIbudProdStatus(postatus);
          order.setSWEMSwPostatus(postatus);
          order.setIbudPoStatusdate(new Date());

        }

        order.setIbudProdMsgPost(postMsg);
        order.setProcessNow(true);
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
      logger.logln(e.getMessage());
      throw new OBException("PushOrderDetailsToProd:Updating:Error while updating data to order");
    }
  }

  private void sendOrder(Order order, JSONObject orderJson, String token,
      HashMap<String, HashMap<String, String>> orderSentMapObj, HashMap<String, String> configMap) {
    StringBuffer sb = new StringBuffer();
    HttpURLConnection HttpUrlConnection = null;
    BufferedReader reader = null;
    String orderReference = null;
    String anomaly = null;
    InputStream is = null;

    try {

      log.info("PushOrderDetailsToProd : Generating HttpConnection with Prod");
      try {
        URL urlObj = new URL(configMap.get("postOrder_Url"));
        HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
        HttpUrlConnection.setDoOutput(true);
        HttpUrlConnection.setRequestMethod(configMap.get("postOrder_requestMethod"));
        HttpUrlConnection.setRequestProperty("Accept-Version",
            configMap.get("postOrder_acceptVersion"));
        HttpUrlConnection
            .setRequestProperty("Content-Type", configMap.get("postOrder_contentType"));
        HttpUrlConnection.setRequestProperty("x-env", configMap.get("postOrder_XEnv"));
        HttpUrlConnection.setRequestProperty("Authorization",
            configMap.get("postOrder_authorization") + " " + token);
        HttpUrlConnection.connect();

      } catch (Exception e) {
        e.printStackTrace();
        log.error("Error while Generating HTTP connection with prod please check POST API credentials in Openbravo.properties "
            + e.getMessage());
        logger
            .logln("Error while Generating HTTP connection with prod please check POST API credentials in Openbravo.properties "
                + e.getMessage());

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

        if (!orderReference.equals("null")) {
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
        anomaly = responseJson.getString("anomaly");
        HashMap<String, String> orderDetailMapObj = orderSentMapObj.get(order.getId());
        if (orderDetailMapObj == null) {
          orderDetailMapObj = new HashMap<String, String>();
          orderDetailMapObj.put("anomaly", anomaly);
          orderSentMapObj.put(order.getId(), orderDetailMapObj);

        } else {
          orderDetailMapObj.put("anomaly", anomaly);
        }
        logger.logln("" + anomaly);
        log.error("Error while sending order " + order.getDocumentNo() + " Response "
            + HttpUrlConnection.getResponseCode() + " " + HttpUrlConnection.getResponseMessage()
            + " :" + anomaly);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      logger.logln(e.getMessage());
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

  private JSONObject generateJSON(Order order, HashMap<String, String> configMap) {
    JSONObject orderObject = new JSONObject();
    ArrayList<String> missingFields = new ArrayList<>();
    try {
      log.info("PushOrderDetailsToProd : Generating JSON for order" + order.getDocumentNo());
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
        orderObject.put("scheduledDeliveryDate", formatter.format(order.getScheduledDeliveryDate())
            .toString().substring(0, 19)
            + "Z");
      } else {
        missingFields.add("Schedule Delivery Date");
      }
      if (order.getBusinessPartner().getIbudSupplierprodno() == null) {
        missingFields.add("Supplier Prod number");
        if (order.getBusinessPartner().getIbudSupplierprodno() == null) {
          missingFields.add("Supplier Prod number");
        }
      }
      if (missingFields.size() > 0) {
        log.error("For order " + missingFields + " is null with documentno" + order.getDocumentNo());
        logger
            .logln("Skipping Order due to " + missingFields + " is null " + order.getDocumentNo());
        // return null;
      }
      JSONArray customerJsonArray = new JSONArray();
      customerJsonArray.put(configMap.get("postOrder_customerKey"));
      customerJsonArray.put(configMap.get("postOrder_customerId"));
      customerJsonArray.put(configMap.get("postOrder_customerId"));
      orderObject.put("customer", customerJsonArray);

      JSONArray supplierJsonArray = new JSONArray();
      supplierJsonArray.put(configMap.get("postOrder_supplierKey"));
      supplierJsonArray.put(order.getBusinessPartner().getIbudSupplierprodno());
      supplierJsonArray.put(order.getBusinessPartner().getIbudSupplierprodno());
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
        orderlineJson.put("cessionPrice", lines.getGrossUnitPrice());
        orderlineJson.put("currency", lines.getCurrency().getISOCode());
        orderLineArray.put(orderlineJson);
      }
      orderObject.put("orderLines", orderLineArray);
      log.info("Generated JSON for order  " + orderObject.toString());
    } catch (Exception e) {
      e.printStackTrace();
      log.error("PushOrderDetailsToProd : Error while generating JSON object for order "
          + order.getDocumentNo() + " and error is: " + e);
      logger.logln("PushOrderDetailsToProd : Error while generating JSON object for order "
          + order.getDocumentNo() + " and error is: " + e);
      return null;
    }
    return orderObject;

  }

  private List<Order> getPurchaseOrder() {

    try {
      Date ibud = CommonServiceProvider.getIbudUpdatedTime("FlexProcess");

      log.info("PushOrderDetailsToProd : Getting list of orders to be sent");
      String strHql = "select distinct co from Order co, OrderLine line ,"
          + "	CL_DPP_SEQNO cd ,BusinessPartner bp "
          + " where co.sWEMSwPostatus in ('SO') and co.id = line.salesOrder.id "
          + " and bp.clSupplierno = line.sWEMSwSuppliercode 	"
          + " and co.transactionDocument.id = 'C7CD4AC8AC414678A525AB7AE20D718C'  "
          + " and  co.imsapDuplicatesapPo != 'Y' and bp.rCSource = 'DPP'  "
          + " and  line.grossUnitPrice > 0 and line.orderedQuantity > 0 "
          + " and co.orderReference is null " + " and co.updated > '" + ibud + "' ";

      Query query = OBDal.getInstance().getSession().createQuery(strHql);
      List<Order> orderList = query.list();
      if (orderList.size() > 0)
        return orderList;
      else
        return null;
    } catch (HibernateException e) {
      e.printStackTrace();
      logger.logln(e.getMessage());
      log.error("Error while retrieving order" + e.getMessage());
      throw new OBException("Error while getting orders" + e.getMessage());
    }
  }
}
