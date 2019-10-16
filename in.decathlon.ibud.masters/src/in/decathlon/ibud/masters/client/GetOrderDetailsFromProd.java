package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.masters.data.IbudServerTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
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
      HashMap<String, String> configMap = CommonServiceProvider.checkObConfig();
      if (!configMap.containsKey("Error")) {
        List<Order> ordList = getPurchaseOrder(configMap);
        if (ordList != null & ordList.size() > 0) {
          updateOrderDetails(ordList, configMap);
        } else {
          log.error("No pending orders to update ");
          logger.logln("No pending orders to update ");

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

  private void updateOrderDetails(List<Order> ordList, HashMap<String, String> configMap)
      throws Exception {
    try {
      String token;
      token = CommonServiceProvider.generateToken(configMap);

      if (!token.contains("Error_TokenGenerator")) {

        for (Order order : ordList) {
          updateOrderHeaderDetails(order, token, configMap);
        }
      } else {
        logger.logln("GetOrderDetailsFromProd: Error in generating token " + token);
        log.error("GetOrderDetailsFromProd: Error in generating token " + token);
        throw new OBException("Error in generating token " + token);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void updateOrderHeaderDetails(Order order, String token, HashMap<String, String> configMap)
      throws Exception {

    Map<String, String> orderDetailMap = GetOrderHeaderDetails(order, token, configMap, "Header",
        null);
    Map<String, String> orderLinesDetailMap = GetOrderHeaderDetails(order, token, configMap,
        "Lines", null);
    Map<String, String> productDetailMap = GetOrderHeaderDetails(order, token, configMap,
        "ProductElp", orderLinesDetailMap.get("elpProductNumber"));
  }

  private Map<String, String> GetOrderHeaderDetails(Order order, String token,
      HashMap<String, String> configMap, String orderType, String elpProduct) {
    HttpURLConnection HttpUrlConnection = null;

    Map<String, String> resultMap = new HashMap<String, String>();
    URL urlObj = null;
    try {
      try {
        if (orderType.equalsIgnoreCase("Header"))
          urlObj = new URL(configMap.get("getOrder_url") + configMap.get("prodDPP")
              + "?search=ordOrderNumber=" + order.getOrderReference());
        if (orderType.equalsIgnoreCase("Lines"))
          urlObj = new URL(configMap.get("getOrderLine_url") + configMap.get("prodDPP")
              + "/order_lines/=" + order.getOrderReference());
        if (orderType.equalsIgnoreCase("ProductElp"))
          urlObj = new URL(configMap.get("getElpProduct_url") + elpProduct);

        HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
        HttpUrlConnection.setDoOutput(true);
        HttpUrlConnection.setRequestMethod("GET");
        HttpUrlConnection
            .setRequestProperty("Accept-Version", configMap.get("order_acceptVersion"));
        HttpUrlConnection.setRequestProperty("x-env", configMap.get("order_XEnv"));
        HttpUrlConnection.setRequestProperty("Authorization", configMap.get("order_authorization")
            + " " + token);
        HttpUrlConnection.connect();

      } catch (Exception e) {
        e.printStackTrace();
        log.error("updateOrderHeaderDetails:Error while Generating HTTP connection with prod, please check GET API credentials in Openbravo.properties and error is: "
            + e);
        logger
            .logln("updateOrderHeaderDetails:Error while Generating HTTP connection with prod, please check GET API credentials in Openbravo.properties and error is: "
                + e);
        throw new OBException(
            "updateOrderHeaderDetails:Error while Generating HTTP connection with prod, please check GET API credentials in Openbravo.properties and error is: "
                + e);
      }

      if (HttpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        int responseCode = HttpUrlConnection.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(
            HttpUrlConnection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();
        JSONArray orderArray = new JSONArray(response.toString());

        if (orderType.equalsIgnoreCase("Header")) {
          JSONObject js = orderArray.getJSONObject(0);

          String orderStatus = js.getString("ostOrderStatusOst");
          String estShipDate = js.getString("ordExpectedShipmentDate");
          String actShipDate = js.getString("ordLastShipmentDate");
          resultMap.put("orderStatus", orderStatus);
          resultMap.put("estShipDate", estShipDate);
          resultMap.put("actShipDate", actShipDate);
        }
        if (orderType.equalsIgnoreCase("Lines")) {

        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("GetOrderDetailsFromProd:Error while updating order header details " + e);
      logger.logln("GetOrderDetailsFromProd:Error while updating order header details " + e);
    } finally {
      try {
        /*
         * if (is != null) { is.close(); }
         */
        if (HttpUrlConnection != null) {
          HttpUrlConnection.disconnect();
        }
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Error while disconnecting http connection " + e.getMessage());
        logger.logln("Error while disconnecting http connection " + e.getMessage());
      }
    }
    return resultMap;

  }

  private List<Order> getPurchaseOrder(HashMap<String, String> configMap) {

    List<Order> orderList = null;
    try {
      IbudServerTime newIbudServiceObj = CommonServiceProvider
          .getIbudUpdatedTime("GetOrderProcess");
      Date ibud = newIbudServiceObj.getLastupdated();
      log.info("PushOrderDetailsToProd : Getting list of orders to be sent");
      String strHql = "select distinct co from Order co, OrderLine line ,"
          + "   CL_DPP_SEQNO cd ,BusinessPartner bp "
          + " where co.sWEMSwPostatus in ('OS') and co.id = line.salesOrder.id "
          + " and bp.clSupplierno = line.sWEMSwSuppliercode     "
          + " and co.transactionDocument.id = 'C7CD4AC8AC414678A525AB7AE20D718C'  "
          + " and  co.imsapDuplicatesapPo != 'Y' and bp.rCSource = 'DPP'  "
          + " and line.orderedQuantity > 0 " + " and co.orderReference is not null "
          + " and co.updated > '" + ibud + "' ";

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
