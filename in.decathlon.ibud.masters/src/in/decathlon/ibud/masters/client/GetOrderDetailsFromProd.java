package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.masters.data.IbudServerTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      String token = CommonServiceProvider.generateToken(configMap);
      Map<String, String> errorOrderMap = new HashMap<String, String>();
      if (!token.contains("Error_TokenGenerator")) {
        for (Order order : orderList) {

          updateOrderHeaderDetails(order, token, configMap, errorOrderMap);
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
      HashMap<String, String> configMap, Map<String, String> errorOrderMapObj) throws Exception {
    Map<String, HashMap<String, String>> orderMap = new HashMap<String, HashMap<String, String>>();

    String orderHeaderData = GetDataFromProd(orderObj.getOrderReference(),
        orderObj.getDocumentNo(), token, configMap, "Header", null, orderObj.getBusinessPartner()
            .getIbudDppNo());
    SaveProdHeaderData(orderObj, orderHeaderData, orderMap);
    if (!errorOrderMapObj.containsKey(orderObj.getDocumentNo())) {
      String orderHeaderLineData = GetDataFromProd(orderObj.getOrderReference(),
          orderObj.getDocumentNo(), token, configMap, "Lines", null, orderObj.getBusinessPartner()
              .getIbudDppNo());// ProductElp
      List<String> errorItemCodeList = SaveProdOrderLineData(orderObj, orderHeaderLineData, token,
          configMap, orderObj.getBusinessPartner().getIbudDppNo(), orderMap);
      if (!orderMap.containsKey("error") && errorItemCodeList.size() == 0) {
        savePOData(orderMap, orderObj);
      } else {
        logger.logln("Order No: " + orderObj.getDocumentNo() + " and Error is: "
            + orderMap.get("error") + ", So Skipping the Order.");
        log.error("Order No: " + orderObj.getDocumentNo() + " and Error is: "
            + orderMap.get("error") + ", So Skipping the Order.");
        log.error("Order Map for Order no is: " + orderObj.getDocumentNo() + " and Map is: "
            + orderMap + " and Not Present Itemcode list: " + errorItemCodeList);
      }
    }

  }

  private void savePOData(Map<String, HashMap<String, String>> orderMap, Order orderObj) {
    String status = null;
    String adate = null;
    String edate = null;
    if (orderMap.containsKey("header")) {
      HashMap<String, String> headerdata = orderMap.get("header");
      if (headerdata.containsKey("status")) {
        status = headerdata.get("status");
      }
      if (headerdata.containsKey("edate")) {
        edate = headerdata.get("edate");
      }
      if (headerdata.containsKey("adate")) {
        adate = headerdata.get("adate");
      }

      if (adate != null && edate != null && status != null) {
        for (OrderLine orderLineObj : orderObj.getOrderLineList()) {
          String cqty = null;
          String elp = null;
          if (orderMap.containsKey(orderLineObj.getProduct().getName())) {
            HashMap<String, String> olMap = orderMap.get(orderLineObj.getProduct().getName());
            if (olMap.containsKey("cqty")) {
              cqty = olMap.get("cqty");
            }
            if (olMap.containsKey("elp")) {
              elp = olMap.get("elp");
            }
            if (cqty != null) {
              orderLineObj.setSwConfirmedqty(Long.parseLong(cqty));
              OBDal.getInstance().save(orderLineObj);
            }
            if (elp != null) {

              orderLineObj.getProduct().setIbudElpelementprodnumelp(elp);
              OBDal.getInstance().save(orderLineObj.getProduct());
            }
          }
        }

        orderObj.setSwStatus(status);
        // Date new Simple
        orderObj.setSWEMSwActshipdate(new Date(adate));
        orderObj.setSWEMSwEstshipdate(new Date(edate));
        SessionHandler.getInstance().commitAndStart();
      }
    }
  }

  private void SaveProdHeaderData(Order orderObj, String orderHeaderData,
      Map<String, HashMap<String, String>> orderMap) throws JSONException {
    try {
      HashMap<String, String> headerMap1 = new HashMap<String, String>();
      headerMap1.put("error", " Order not Found,");
      orderMap.put("error", headerMap1);

      if (!orderHeaderData.contains("Error_GETAPI:")) {
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
          HashMap<String, String> headerMap = orderMap.get("header");
          if (headerMap == null) {
            headerMap = new HashMap<String, String>();
            headerMap.put("status", orderStatus);
            headerMap.put("edate", estShipDate);
            headerMap.put("adate", actShipDate);
            orderMap.put("header", headerMap);
          }/*
            * headerMap.put("status", orderStatus); headerMap.put("edate", estShipDate);
            * headerMap.put("adate", actShipDate); orderMap.put("header", headerMap);
            */
        } else {
          HashMap<String, String> headerMap = orderMap.get("error");
          if (headerMap == null) {
            headerMap = new HashMap<String, String>();
            headerMap.put("error", " Order not Found,");
            orderMap.put("error", headerMap);
          } else {
            headerMap.get("error").concat(" Order not Found,");
          }
          // errorOrderMapObj.put(orderObj.getDocumentNo(), " Order not Found, ");
          logger.logln("Order Data Not Found for Order: " + orderObj.getDocumentNo());
          log.error("Order Data Not Found for Order: " + orderObj.getDocumentNo());
        }
      } else {
        log.error("Order API Error is:" + orderHeaderData + " for Order: "
            + orderObj.getDocumentNo());
        logger.logln("Error is:" + orderHeaderData + " for Order: " + orderObj.getDocumentNo());
        HashMap<String, String> headerMap = orderMap.get("error");
        if (headerMap == null) {
          headerMap = new HashMap<String, String>();
          headerMap.put("error", " order Prod.com API Failed,");
          orderMap.put("error", headerMap);
        } else {
          headerMap.get("error").concat(" order Prod.com API Failed,");
        }
        // errorOrderMapObj.put(orderObj.getDocumentNo(), " Prod.com API Failed, ");
      }
    } catch (Exception e) {
      e.printStackTrace();
      HashMap<String, String> headerMap = orderMap.get("error");
      if (headerMap == null) {
        headerMap = new HashMap<String, String>();
        headerMap.put("error", " Error While process, ");
        orderMap.put("error", headerMap);
      } else {
        headerMap.get("error").concat(" Error While process, ");
      }
      logger.logln("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      log.error("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      // errorOrderMapObj.put(orderObj.getDocumentNo(), " Error While process, ");
    }
  }

  private List<String> SaveProdOrderLineData(Order orderObj, String orderHeaderLineData,
      String token, HashMap<String, String> configMap, String dpp,
      Map<String, HashMap<String, String>> orderMap) throws JSONException {
    List<String> notFoundItemListObj = new ArrayList<String>();
    try {
      Map<String, OrderLine> elpMap = new HashMap<String, OrderLine>();
      if (!orderHeaderLineData.contains("Error_GETAPI:")) {
        JSONArray orderLineArray = new JSONArray(orderHeaderLineData.toString());
        if (orderLineArray != null && orderLineArray.length() > 0) {
          for (OrderLine orderLineObj : orderObj.getOrderLineList()) {

            notFoundItemListObj.add(orderLineObj.getProduct().getName());
            if (orderLineObj.getProduct() != null
                && orderLineObj.getProduct().getIbudElpelementprodnumelp() != null) {
              elpMap.put(orderLineObj.getProduct().getIbudElpelementprodnumelp(), orderLineObj);

            }
          }

          for (int k = 0; k < orderLineArray.length(); k++) {
            String itemcode = null;
            String productElpCode = null;
            String itemConfirmQty = null;
            JSONObject js = orderLineArray.getJSONObject(k);
            if (js.has("olnOrderedQuantity") && !js.isNull("olnOrderedQuantity")) {
              itemConfirmQty = js.getString("olnOrderedQuantity");
            }
            productElpCode = js.getString("elpElementProdNumElp");
            if (elpMap.containsKey(productElpCode)) {
              OrderLine olObj = elpMap.get(productElpCode);
              if (notFoundItemListObj.contains(olObj.getProduct().getName())) {
                notFoundItemListObj.remove(olObj.getProduct().getName());
              }
              itemcode = olObj.getProduct().getName();

            } else {
              String orderLineElpDetails = GetDataFromProd(orderObj.getOrderReference(),
                  orderObj.getDocumentNo(), token, configMap, "ProductElp", null, dpp);
              if (!orderLineElpDetails.contains("Error_GETAPI:")) {
                JSONArray orderLineElpArray = new JSONArray(orderLineElpDetails.toString());
                if (orderLineElpArray != null && orderLineElpArray.length() > 0) {
                  for (int p = 0; p < orderLineElpArray.length(); p++) {
                    JSONObject jsO = orderLineArray.getJSONObject(p);
                    if (jsO.has("cofRefCodeRef") && !jsO.isNull("cofRefCodeRef")) {
                      itemcode = jsO.getString("cofRefCodeRef");
                      if (notFoundItemListObj.contains(itemcode)) {
                        notFoundItemListObj.remove(itemcode);
                      }
                    }
                  }
                } else {
                  HashMap<String, String> headerMap = orderMap.get("error");
                  if (headerMap == null) {
                    headerMap = new HashMap<String, String>();
                    headerMap.put("error", " OrderLine ELP data not Found for ELP:"
                        + orderLineElpDetails + ",");
                    orderMap.put("error", headerMap);
                  } else {
                    headerMap.get("error").concat(
                        " OrderLine ELP data not Found for ELP:" + orderLineElpDetails + ",");
                  }
                  log.error("ELP data is not found Error is:" + orderHeaderLineData
                      + " for Order: " + orderObj.getDocumentNo());
                  logger.logln("Error while Getting the line elp data Error is:"
                      + orderHeaderLineData + " for Order: " + orderObj.getDocumentNo());
                }
              } else {
                HashMap<String, String> headerMap = orderMap.get("error");
                if (headerMap == null) {
                  headerMap = new HashMap<String, String>();
                  headerMap.put("error", " OrderLine ELP API got failed not Found,");
                  orderMap.put("error", headerMap);
                } else {
                  headerMap.get("error").concat(" OrderLine ELP API got failed not Found,");
                }
                log.error("Error while Getting the line ELP data Error is:" + orderHeaderLineData
                    + " for Order: " + orderObj.getDocumentNo());
                logger.logln("Error while Getting the line elp data Error is:"
                    + orderHeaderLineData + " for Order: " + orderObj.getDocumentNo());

                // errorOrderMapObj.put(orderObj.getDocumentNo(), " ELP Api Failed, ");
              }
            }
            if (itemcode == null || itemConfirmQty == null || productElpCode == null) {
              HashMap<String, String> headerMap = orderMap.get("error");
              if (headerMap == null) {
                headerMap = new HashMap<String, String>();
                headerMap.put("error", " OrderLine data is null not Found and item code is: "
                    + itemcode + "itemConfirmQty: " + itemConfirmQty + "productElpCode: "
                    + productElpCode + ",");
                orderMap.put("error", headerMap);
              } else {
                headerMap.get("error").concat(
                    " OrderLine data is null not Found and item code is: " + itemcode
                        + "itemConfirmQty: " + itemConfirmQty + "productElpCode: " + productElpCode
                        + ",");
              }
            } else {
              HashMap<String, String> lineItemMap = orderMap.get(itemcode);
              if (lineItemMap == null) {
                lineItemMap = new HashMap<String, String>();
                lineItemMap.put("cqty", itemConfirmQty);
                lineItemMap.put("elp", productElpCode);
                orderMap.put(itemcode, lineItemMap);
              }
            }
          }
        } else {
          // errorOrderMapObj.put(orderObj.getDocumentNo(), " Line Data not found, ");
          logger.logln("Line data Not Found for Order: " + orderObj.getDocumentNo());
          log.error("Line Data Not Found for Order: " + orderObj.getDocumentNo());

          HashMap<String, String> headerMap = orderMap.get("error");
          if (headerMap == null) {
            headerMap = new HashMap<String, String>();
            headerMap.put("error", " OrderLine data not Found,");
            orderMap.put("error", headerMap);
          } else {
            headerMap.get("error").concat(" OrderLine data not Found,");
          }
        }
      } else {
        log.error("Error while Getting the line data Error is:" + orderHeaderLineData
            + " for Order: " + orderObj.getDocumentNo());
        logger.logln("Error while Getting the line data Error is:" + orderHeaderLineData
            + " for Order: " + orderObj.getDocumentNo());
        HashMap<String, String> headerMap = orderMap.get("error");
        if (headerMap == null) {
          headerMap = new HashMap<String, String>();
          headerMap.put("error", " OrderLine API got failed not Found,");
          orderMap.put("error", headerMap);
        } else {
          headerMap.get("error").concat(" OrderLine API got failed not Found,");
        }
        // errorOrderMapObj.put(orderObj.getDocumentNo(), " Line Api Failed, ");
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.logln("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
      log.error("Error while Savinf the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);

      HashMap<String, String> headerMap = orderMap.get("error");
      if (headerMap == null) {
        headerMap = new HashMap<String, String>();
        headerMap.put("error", " Error while getting the OrderLine data,");
        orderMap.put("error", headerMap);
      } else {
        headerMap.get("error").concat(" Error while getting the OrderLine data,");
      }
      // errorOrderMapObj.put(orderObj.getDocumentNo(), " Error while Process line Api, ");
    }
    return notFoundItemListObj;
  }

  private String GetDataFromProd(String orderReferenceNo, String orderNo, String tokenKey,
      HashMap<String, String> configMap, String orderType, String elpProduct, String dpp) {
    HttpURLConnection conn = null;

    // Map<String, String> resultMap = new HashMap<String, String>();
    URL url = null;
    try {
      /* try { */
      if (orderType.equalsIgnoreCase("Header")) {
        url = new URL(configMap.get("getOrder_url") + dpp + "?search=ordOrderNumber="
            + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("Lines")) {
        url = new URL(configMap.get("getOrderLine_url") + dpp + "/order_lines/=" + orderReferenceNo);
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
          + "    and bp.rCSource = 'DPP' and o.orderReference is not null "
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
