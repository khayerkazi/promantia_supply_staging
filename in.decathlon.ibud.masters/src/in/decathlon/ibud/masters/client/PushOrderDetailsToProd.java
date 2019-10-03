package in.decathlon.ibud.masters.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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

  final Logger log = Logger.getLogger(PushOrderDetailsToProd.class);
  private static ProcessLogger logger;

  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  HashMap<String, String> orderSentList = new HashMap<String, String>();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    try {
      OBContext.setAdminMode(true);
      List<Order> orderList = getPurchaseOrder();
      if (orderList != null) {
        sendOrdersToProd(orderList);
      } else {
        log.info("There is no order present at this time");
      }
    } catch (Exception e) {
      log.error(e);
      logger.logln(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void sendOrdersToProd(List<Order> orderList) throws Exception {
    // TODO Auto-generated method stub
    String token = generateToken();
    for (Order order : orderList) {
      JSONObject orderJson = generateJSON(order);
      sendOrder(order, orderJson, token);
    }
    updateOrderDetails();
  }

  private void updateOrderDetails() {
    log.info("PushOrderDetailsToProd:Updating sent order details");
    for (Entry<String, String> aa : orderSentList.entrySet()) {
      String orderId = aa.getKey();
      String reference = aa.getValue();
      Order order = OBDal.getInstance().get(Order.class, orderId);
      // Order order = getOrder(orderId);
      order.setIbudProdStatus("OS");
      order.setOrderReference(reference);
      order.setIbudProdMsgPost("Order Successfully sent");
      order.setProcessNow(true);
      OBDal.getInstance().save(order);
      OBDal.getInstance().flush();

    }

  }

  private void sendOrder(Order order, JSONObject orderJson, String token) {
    // TODO Auto-generated method stub
    String postUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("eas.posturl");
    String x_env = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("eas.x_env");
    HttpURLConnection HttpUrlConnection = null;
    BufferedReader reader = null;
    String orderReference = null;
    String anomaly = null;
    InputStream is = null;
    try {
      log.info("PushOrderDetailsToProd : Generating HttpConnection with Prod");
      URL urlObj = new URL(postUrl);
      HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
      HttpUrlConnection.setDoOutput(true);
      HttpUrlConnection.setRequestMethod("POST");
      HttpUrlConnection.setRequestProperty("Accept-Version", "1.0");
      HttpUrlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      HttpUrlConnection.setRequestProperty("x-env", x_env);
      HttpUrlConnection.setRequestProperty("Authorization", "Bearer " + token);
      HttpUrlConnection.connect();

      log.info("PushOrderDetailsToProd :Sending order to Prod with document number -:"
          + order.getDocumentNo());
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
        orderSentList.put(order.getId(), orderReference);
        if (is != null)
          is.close();
        if (HttpUrlConnection != null)
          HttpUrlConnection.disconnect();

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      logger.logln(e.getMessage());
      throw new OBException("Error while sending order to prod " + e.getMessage());
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
        log.error(e);
        logger.logln(e.getMessage());
      }
    }
  }

  private JSONObject generateJSON(Order order) throws JSONException {
    // TODO Auto-generated method stub
    log.info("PushOrderDetailsToProd : Generating JSON for order" + order.getDocumentNo());
    JSONObject orderObject = new JSONObject();
    orderObject.put("origin", "RETAIL");
    orderObject.put("externalNumber", Integer.parseInt(order.getDocumentNo()));
    orderObject.put("orderType", "Z");
    orderObject.put("orderStatus", "NV");
    orderObject.put("orderCreation", formatter.format(order.getCreationDate()).toString()
        .substring(0, 19)
        + "Z");
    orderObject.put("requestedDeliveryDate", formatter.format(order.getSWEMSwExpdeldate())
        .toString().substring(0, 19)
        + "Z");
    orderObject.put("requestedShipmentDate", formatter.format(order.getSWEMSwEstshipdate())
        .toString().substring(0, 19)
        + "Z");
    orderObject.put("scheduledDeliveryDate", formatter.format(order.getScheduledDeliveryDate())
        .toString().substring(0, 19)
        + "Z");

    JSONArray customerJsonArray = new JSONArray();
    customerJsonArray.put("7");
    customerJsonArray.put("10006");
    customerJsonArray.put("10006");
    orderObject.put("customer", customerJsonArray);

    JSONArray supplierJsonArray = new JSONArray();
    supplierJsonArray.put("2");
    supplierJsonArray.put("31094");
    supplierJsonArray.put("31094");

    orderObject.put("supplier", supplierJsonArray);

    JSONArray deliveryJsonArray = new JSONArray();
    deliveryJsonArray.put("7");
    deliveryJsonArray.put("10006");
    deliveryJsonArray.put("10006");
    orderObject.put("delivery", deliveryJsonArray);

    JSONArray orderLineArray = new JSONArray();

    // Get orderline information for every order

    for (OrderLine lines : order.getOrderLineList()) {
      JSONObject orderlineJson = new JSONObject();

      orderlineJson.put("number", lines.getLineNo());
      orderlineJson.put("status", "A");
      orderlineJson.put("item", Long.parseLong(lines.getProduct().getName()));
      orderlineJson.put("quantity", lines.getOrderedQuantity());
      orderlineJson.put("price", lines.getGrossUnitPrice());
      orderlineJson.put("currency", lines.getCurrency().getISOCode());
      orderLineArray.put(orderlineJson);
    }
    orderObject.put("orderLines", orderLineArray);
    log.info("Generated JSON for order  " + orderObject.toString());
    return orderObject;

  }

  private String generateToken() throws Exception {

    log.info("PushOrderDetailToProd : Started Generating token");
    TokenGenerator tokens = new TokenGenerator();
    String token = null;
    token = tokens.generateToken();
    return token;

  }

  /*
   * private Order getOrder(String orderId) { String hql =
   * "Select distinct ord from Order ord where ord.id = '" + orderId + "'"; Query qry =
   * OBDal.getInstance().getSession().createQuery(hql); return (Order) qry.uniqueResult(); }
   */

  private List<Order> getPurchaseOrder() {
    log.info("PushOrderDetailsToProd : Getting list of orders to be sent");

    String strHql = "select distinct co from Order co, OrderLine line ,"
        + "	CL_DPP_SEQNO cd ,BusinessPartner bp, Ibud_ServerTime ibud "
        + "where co.sWEMSwPostatus in ('SO') and co.id = line.salesOrder.id "
        + "and bp.clSupplierno = line.sWEMSwSuppliercode 	"
        + "and co.transactionDocument.id = 'C7CD4AC8AC414678A525AB7AE20D718C'  "
        + "and  co.imsapDuplicatesapPo != 'Y' and bp.rCSource = 'DPP'  "
        + "and  line.grossUnitPrice > 0 and line.orderedQuantity > 0 a"
        + "nd co.orderReference = null and  ibud.serviceKey = 'FlexProcess' "
        + "and co.updated > ibud.lastupdated";

    Query query = OBDal.getInstance().getSession().createQuery(strHql);
    if (query.list().size() > 0)
      return query.list();
    else
      return null;
  }
}
