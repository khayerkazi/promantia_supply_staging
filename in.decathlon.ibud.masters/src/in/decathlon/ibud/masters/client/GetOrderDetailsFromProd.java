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
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.jboss.weld.util.collections.ArraySet;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

import com.ibm.icu.text.SimpleDateFormat;

public class GetOrderDetailsFromProd implements Process {

  final Logger log = LogManager.getLogger(GetOrderDetailsFromProd.class);

  private ProcessLogger logger;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    logger.logln("Get Order Details From prod Background process has started");
    StringBuilder logMsg = new StringBuilder(" ");

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
          int errorCount = updateOrderDetails(orderObjList, configMap, logMsg);
          if (!logMsg.toString().trim().equalsIgnoreCase("")) {
            logger.logln(logMsg.toString());

          }
          logMsg = new StringBuilder(" ");
          if (errorCount == 0) {
            newIbudServiceObj.setLastupdated(new Date());
            OBDal.getInstance().save(newIbudServiceObj);
            SessionHandler.getInstance().commitAndStart();
            logger.logln("Sucessfully Completed the GET Order Process From Prod.com.");
            log.info("Sucessfully Completed the GET Order Process From Prod.com.");
          } else {
            logger
                .logln("Error while Getting the order details from OB to Prod.com and Order size is "
                    + orderObjList.size()
                    + " and Error count is: "
                    + errorCount
                    + " from date: "
                    + ibud);
            log.info("Error while Getting the order details from OB to Prod.com and Order size is "
                + orderObjList.size() + " and Error count is: " + errorCount + " from date: "
                + ibud);
          }
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
      if (!logMsg.toString().trim().equalsIgnoreCase("")) {
        logger.logln(logMsg.toString());
      }
      logMsg = new StringBuilder(" ");

      OBContext.restorePreviousMode();
    }

  }

  public int updateOrderDetails(List<Order> orderList, HashMap<String, String> configMap,
      StringBuilder logMsg) throws Exception {
    int errorOrderCount = 0;
    try {

      String token = CommonServiceProvider.generateToken(configMap);
      if (!token.contains("Error_TokenGenerator")) {
        for (Order order : orderList) {
          Map<String, String> orderMap = updateOrderHeaderDetails(order, token, configMap, logMsg);
          if (orderMap.containsKey("error")) {
            errorOrderCount++;
          }
        }
      } else {
        errorOrderCount++;
        logMsg.append("GetOrderDetailsFromProd: Error in generating token " + token + " \n");
        log.error("GetOrderDetailsFromProd: Error in generating token " + token);

      }
    } catch (Exception e) {
      errorOrderCount++;

      e.printStackTrace();
      log.error("Error while Getting the Orders from Prod and Error is: " + e);
      logMsg.append("Error while Getting the Orders from Prod and Error is: " + e + " \n");
    }
    return errorOrderCount;
  }

  public Map<String, String> updateOrderHeaderDetails(Order orderObj, String token,
      HashMap<String, String> configMap, StringBuilder logMsg) throws Exception {
    Map<String, String> orderMap = new HashMap<String, String>();

    String orderHeaderData = GetDataFromProd(orderObj.getOrderReference(),
        orderObj.getDocumentNo(), token, configMap, "Header", null, orderObj.getBusinessPartner()
            .getIbudDppNo(), logMsg);
    getProdHeaderMapData(orderObj, orderHeaderData, orderMap, logMsg);
    String orderHeaderLineData = GetDataFromProd(orderObj.getOrderReference(),
        orderObj.getDocumentNo(), token, configMap, "Lines", null, orderObj.getBusinessPartner()
            .getIbudDppNo(), logMsg);
    GetOrderLineProdData(orderObj, orderHeaderLineData, token, configMap, orderObj
        .getBusinessPartner().getIbudDppNo(), orderMap, logMsg);

    savePOData(orderMap, orderObj, logMsg);

    return orderMap;
  }

  private void savePOData(Map<String, String> orderMap, Order orderObj, StringBuilder logMsg)
      throws Exception {
    String pStatus = null;
    String oStatus = null;
    String orderLastDate = null;
    String expectedDateShipment = null;
    String errorMsg = "Sucessfully Fetched the data from Prod.com.for current order";

    if (orderMap.containsKey("status")) {
      pStatus = orderMap.get("status");
    }
    if (orderMap.containsKey("edate")) {
      expectedDateShipment = orderMap.get("edate");
    }
    if (orderMap.containsKey("adate")) {
      orderLastDate = orderMap.get("adate");
    }
    if (pStatus != null) {
      if (pStatus.equalsIgnoreCase("V") || pStatus.equalsIgnoreCase("PK")
          || pStatus.equalsIgnoreCase("AS") || pStatus.equalsIgnoreCase("PS")) {
        oStatus = "VD";
      } else if (pStatus.equalsIgnoreCase("S")) {
        oStatus = "SH";
      } else if (pStatus.equalsIgnoreCase("C")) {
        oStatus = "CL";
      } else if (pStatus.equalsIgnoreCase("D")) {
        oStatus = "OC";
      } else {
        oStatus = orderObj.getSWEMSwPostatus();
      }
    } else {
      oStatus = "OS";
    }
    for (OrderLine orderLineObj : orderObj.getOrderLineList()) {
      String cqty = null;
      if (orderMap.containsKey(orderLineObj.getProduct().getName())) {
        cqty = orderMap.get(orderLineObj.getProduct().getName());

        if (cqty != null) {
          orderLineObj.setSwConfirmedqty(Long.parseLong(cqty));
          orderLineObj.setUpdated(new Date());

          OBDal.getInstance().save(orderLineObj);
        }
      }
      orderObj.setSWEMSwPostatus(oStatus);
      orderObj.setIbudProdStatus(pStatus);
      Date aDate = null;
      Date eDate = null;
      if (orderLastDate != null) {
        aDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(orderLastDate);
        orderObj.setSWEMSwActshipdate(aDate);
      }
      if (expectedDateShipment != null) {
        eDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(expectedDateShipment);
        orderObj.setSWEMSwEstshipdate(eDate);
      }
      logMsg.append("Getting the Order Details for Order No: " + orderObj.getDocumentNo()
          + " and PO Reference No: " + orderObj.getOrderReference()
          + " Process Completed Sucessfully. \n");

    }
    log.error("Order Map for Order no: " + orderObj.getDocumentNo() + " and Map is: " + orderMap);
    logMsg.append("Order Map for Order no: " + orderObj.getDocumentNo() + " and Map is: "
        + orderMap + " \n");

    if (orderMap.containsKey("error")) {

      errorMsg = orderMap.get("error");

      logMsg.append("Order No: " + orderObj.getDocumentNo() + " and Error is: " + errorMsg
          + ", So Skipping the Order. \n");
      log.error("Order No: " + orderObj.getDocumentNo() + " and Error is: " + errorMsg
          + ", So Skipping the Order.");

    }
    orderObj.setIbudProdMsgGet(errorMsg);
    orderObj.setIbudPoStatusdate(new Date());
    orderObj.setUpdated(new Date());
    OBDal.getInstance().save(orderObj);
    SessionHandler.getInstance().commitAndStart();

  }

  private void getProdHeaderMapData(Order orderObj, String orderHeaderData,
      Map<String, String> orderMap, StringBuilder logMsg) throws JSONException {
    try {

      if (!orderHeaderData.contains("Error_GETAPI:") && (!orderHeaderData.contains("[]"))) {
        JSONArray orderArray = new JSONArray(orderHeaderData.toString());
        if (orderArray != null && orderArray.length() > 0) {
          JSONObject js = orderArray.getJSONObject(0);
          String orderStatus = null;
          String estShipDate = null;
          String actShipDate = null;
          if (js.has("ostOrderStatusOst") && !js.isNull("ostOrderStatusOst")) {
            orderStatus = js.getString("ostOrderStatusOst");
          }
          if (js.has("ordExpectedShipmentDate") && !js.isNull("ordExpectedShipmentDate")) {
            estShipDate = js.getString("ordExpectedShipmentDate");
          }
          if (js.has("ordLastShipmentDate") && !js.isNull("ordLastShipmentDate")) {
            actShipDate = js.getString("ordLastShipmentDate");
          }

          orderMap.put("status", orderStatus);
          orderMap.put("edate", estShipDate);
          orderMap.put("adate", actShipDate);
        } else {
          if (orderMap.containsKey("error")) {
            orderMap.get("error").concat(" Order not Found, \n");
          } else {
            orderMap.put("error", " Order not Found, \n");
          }

          logMsg.append("Order Data Not Found for Order: " + orderObj.getDocumentNo() + " \n");
          log.error("Order Data Not Found for Order: " + orderObj.getDocumentNo());
        }
      } else {
        String prodMsg = "Order Not found with reference no " + orderObj.getOrderReference()
            + " Order API Error is :" + orderHeaderData + " for Order: " + orderObj.getDocumentNo();
        orderObj.setIbudProdMsgGet(prodMsg);
        orderObj.setUpdated(new Date());
        orderObj.setIbudPoStatusdate(new Date());

        OBDal.getInstance().save(orderObj);
        SessionHandler.getInstance().commitAndStart();
        log.error("Order API Error is:" + orderHeaderData + " for Order: "
            + orderObj.getDocumentNo());
        logMsg.append("Error is:" + orderHeaderData + " for Order: " + orderObj.getDocumentNo()
            + " \n");
        if (orderMap.containsKey("error")) {
          orderMap.get("error").concat(" Order Prod.com API Failed, \n");
        } else {
          orderMap.put("error", " Order Prod.com API Failed, \n");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (orderMap.containsKey("error")) {
        orderMap.get("error").concat(
            "Error while Saving the the order Header data for order: " + orderObj.getDocumentNo()
                + " \n");
      } else {
        orderMap.put("error",
            "Error while Saving the the order Header data for order: " + orderObj.getDocumentNo()
                + " \n");
      }
      logMsg.append("Error while Saving the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e + " \n");
      log.error("Error while Saving the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
    }
  }

  private Set<String> GetOrderLineProdData(Order orderObj, String orderHeaderLineData,
      String token, HashMap<String, String> configMap, String dpp, Map<String, String> orderMap,
      StringBuilder logMsg) throws JSONException {
    Set<String> itemNotFoundError = new ArraySet<String>();
    try {
      Map<String, String> OBelpMap = new HashMap<String, String>();
      if (!orderHeaderLineData.contains("Error_GETAPI:")) {
        JSONArray orderLineArray = new JSONArray(orderHeaderLineData.toString());
        if (orderLineArray != null && orderLineArray.length() > 0) {
          for (OrderLine orderLineObj : orderObj.getOrderLineList()) {
            if (orderLineObj.getProduct() != null) {
              itemNotFoundError.add(orderLineObj.getProduct().getName());
              if (orderLineObj.getProduct().getIbudElpelementprodnumelp() != null) {
                OBelpMap.put(orderLineObj.getProduct().getIbudElpelementprodnumelp(), orderLineObj
                    .getProduct().getName());
              }
            }
          }

          for (int k = 0; k < orderLineArray.length(); k++) {
            String itemcode = null;
            String itemConfirmQty = null;
            JSONObject js = orderLineArray.getJSONObject(k);
            if (js.has("olnOrderedQuantity") && !js.isNull("olnOrderedQuantity")) {
              itemConfirmQty = js.getString("olnOrderedQuantity");
            }
            String productElpCode = js.getString("elpElementProdNumElp");
            if (OBelpMap.containsKey(productElpCode)) {
              itemcode = OBelpMap.get(productElpCode);
              itemNotFoundError.remove(itemcode);
            } else {
              String orderLineElpDetails = GetDataFromProd(orderObj.getOrderReference(),
                  orderObj.getDocumentNo(), token, configMap, "ProductElp", productElpCode, dpp,
                  logMsg);
              if (!orderLineElpDetails.contains("Error_GETAPI:")) {
                JSONArray orderLineElpArray = new JSONArray(orderLineElpDetails.toString());
                if (orderLineElpArray != null && orderLineElpArray.length() > 0) {
                  for (int p = 0; p < orderLineElpArray.length(); p++) {
                    JSONObject jsO = orderLineElpArray.getJSONObject(p);
                    if (jsO.has("cofRefCodeRef") && !jsO.isNull("cofRefCodeRef")) {
                      itemcode = jsO.getString("cofRefCodeRef");
                      if (itemNotFoundError.contains(itemcode)) {
                        String hql = "select p from Product p where p.name = '" + itemcode + "'";
                        Query query = OBDal.getInstance().getSession().createQuery(hql);

                        if (query.list().size() > 0) {
                          Product product = (Product) query.uniqueResult();
                          product.setIbudElpelementprodnumelp(productElpCode);
                          product.setUpdated(new Date());

                          OBDal.getInstance().save(product);
                          SessionHandler.getInstance().commitAndStart();
                          itemNotFoundError.remove(itemcode);

                        } else {
                          itemNotFoundError.add(itemcode);
                        }

                      } else {
                        itemNotFoundError.add(itemcode);

                      }
                    }
                  }
                } else {

                  if (orderMap.containsKey("error")) {
                    orderMap.get("error").concat(
                        " OrderLine ELP data not Found for ELP:" + orderLineElpDetails + ", \n");
                  } else {
                    orderMap.put("error", " OrderLine ELP data not Found for ELP:"
                        + orderLineElpDetails + ", \n");
                  }
                  log.error("ELP data is not found Error is:" + orderHeaderLineData
                      + " for Order: " + orderObj.getDocumentNo());
                  logMsg.append("Error while Getting the line elp data Error is:"
                      + orderHeaderLineData + " for Order: " + orderObj.getDocumentNo() + " \n");
                }
              } else {

                if (orderMap.containsKey("error")) {
                  orderMap.get("error").concat(" OrderLine ELP API got failed not Found, \n");
                } else {
                  orderMap.put("error", " OrderLine ELP API got failed not Found, \n");
                }

                log.error("Error while Getting the line ELP data Error is:" + orderHeaderLineData
                    + " for Order: " + orderObj.getDocumentNo());
                logMsg.append("Error while Getting the line elp data Error is:"
                    + orderHeaderLineData + " for Order: " + orderObj.getDocumentNo() + " \n");

              }
            }
            if (itemcode == null || itemConfirmQty == null || productElpCode == null) {
              if (orderMap.containsKey("error")) {
                orderMap.get("error").concat(
                    " OrderLine data is null not Found and item code is: " + itemcode
                        + "itemConfirmQty: " + itemConfirmQty + "productElpCode: " + productElpCode
                        + ", \n");
              } else {
                orderMap.put("error", " OrderLine data is null not Found and item code is: "
                    + itemcode + "itemConfirmQty: " + itemConfirmQty + "productElpCode: "
                    + productElpCode + ", \n");
              }
            } else {
              orderMap.put(itemcode, itemConfirmQty);
            }
          }
          if (itemNotFoundError.size() > 0) {
            if (orderMap.containsKey("error")) {
              orderMap.get("error").concat(
                  "Item does not exist in OB or Prod.com with Item code: " + itemNotFoundError
                      + " \n");
            } else {
              orderMap.put("error", "Item does not exist in OB or Prod.com with Item code: "
                  + itemNotFoundError + " \n");
            }
          }
        } else {
          logMsg.append("Line data Not Found for Order: " + orderObj.getDocumentNo() + " \n");
          log.error("Line Data Not Found for Order: " + orderObj.getDocumentNo());

          if (orderMap.containsKey("error")) {
            orderMap.get("error").concat("OrderLine data not Found, \n");
          } else {
            orderMap.put("error", " OrderLine data not Found, \n");
          }
        }
      } else {
        log.error("Error while Getting the line data Error is:" + orderHeaderLineData
            + " for Order: " + orderObj.getDocumentNo());
        logMsg.append("Error while Getting the line data Error is:" + orderHeaderLineData
            + " for Order: " + orderObj.getDocumentNo() + " \n");

        if (orderMap.containsKey("error")) {
          orderMap.get("error").concat(" OrderLine API got failed not Found, \n");
        } else {
          orderMap.put("error", " OrderLine API got failed not Found, \n");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      logMsg.append("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e + " \n");
      log.error("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);

      if (orderMap.containsKey("error")) {
        orderMap.get("error").concat(" Error while getting the OrderLine data, \n");
      } else {
        orderMap.put("error", " Error while getting the OrderLine data, \n");
      }
    }
    return itemNotFoundError;
  }

  private String GetDataFromProd(String orderReferenceNo, String orderNo, String tokenKey,
      HashMap<String, String> configMap, String orderType, String elpProduct, String dpp,
      StringBuilder logMsg) {
    HttpURLConnection conn = null;

    URL url = null;
    try {
      if (orderType.equalsIgnoreCase("Header")) {
        url = new URL(configMap.get("getOrder_url") + dpp + "?search=ordOrderNumber="
            + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("Lines")) {
        url = new URL(configMap.get("getOrderLine_url") + dpp + "/order_lines/" + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("ProductElp") && elpProduct != null) {
        url = new URL(configMap.get("getElpProduct_url") + elpProduct
            + ",tptThiParNumTypRef=46,tprThirdParNumRef=1,tprSubThirdParNumRef=1");
      } else {

        logMsg.append("API is not present for " + orderType + " elp: " + elpProduct
            + " of Order no: " + orderNo + " \n");
        return "Error_GETAPI: API is not present for " + orderType + " elp: " + elpProduct
            + " of Order no: " + orderNo;
      }
      conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Accept-Version", configMap.get("order_acceptVersion"));
      if (!(configMap.get("order_XEnv") == null))
        conn.setRequestProperty("x-env", configMap.get("order_XEnv"));
      conn.setRequestProperty("Authorization", configMap.get("order_authorization") + " "
          + tokenKey);
      conn.connect();

      if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
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
      logMsg.append("GetOrderDetailsFromProd:Error while updating order header details " + e
          + " \n");
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
        logMsg.append("Error while disconnecting http connection " + e.getMessage() + " \n");
        return "Error_GETAPI: Error While Getting the Order details(Finally) for order No: "
            + orderNo + " and Type is: " + orderType;
      }
    }

  }

  private List<Order> getPurchaseOrder(HashMap<String, String> configMap, Date ibud) {

    List<Order> orderList = null;
    try {

      log.info("PushOrderDetailsToProd : Getting list of orders to be sent");

      String strHql = "  select distinct o from OrderLine ol join ol.salesOrder o join o.businessPartner bp "
          + "    where o.sWEMSwPostatus in ('OS') "
          + "    and  bp.clSupplierno = ol.sWEMSwSuppliercode  "
          + "    and  o.transactionDocument.id ='C7CD4AC8AC414678A525AB7AE20D718C'  "
          + "    and  o.imsapDuplicatesapPo != 'Y' "
          + "    and bp.rCSource = 'DPP' "
          + "    and (o.updated >= '" + ibud + "'  or ol.updated >= '" + ibud + "' )";

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
