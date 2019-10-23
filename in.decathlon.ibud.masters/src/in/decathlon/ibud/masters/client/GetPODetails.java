package in.decathlon.ibud.masters.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

import com.ibm.icu.text.SimpleDateFormat;

public class GetPODetails extends BaseProcessActionHandler {
  private static final Logger log = Logger.getLogger(GetPODetails.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String data) {

    JSONObject jsonData;
    try {
      OBContext.setAdminMode(true);

      jsonData = new JSONObject(data);
      String orderId = jsonData.getString("inpcOrderId");
      Order order = OBDal.getInstance().get(Order.class, orderId);
      List<Order> orderObjList = new ArrayList<Order>();
      orderObjList.add(order);
      HashMap<String, String> configMap = CommonServiceProvider.checkObConfig();

      if (!configMap.containsKey("Error")) {
        log.info("Getting OB List of Order for Getting Order details from Prod.com for Order No : "
            + order.getDocumentNo());
        int errorCount = updateOrderDetails(orderObjList, configMap);
        if (errorCount == 0) {
          String msg = "Sucessfully Fetched the data from Prod.com.for current order :"
              + order.getDocumentNo();
          log.info(msg);
          return getSuccessMessage(msg);
        } else {
          String msg = "Error while getting order details from Prod.com for orderNo :"
              + order.getDocumentNo();
          log.error(msg);
          order.setIbudProdMsgGet(msg);
          SessionHandler.getInstance().commitAndStart();
          return getErrorMessage(msg);
        }
      } else {
        String msg = "Missing Configuration in openbravo properties :" + configMap.get("Error");
        log.error(msg);
        order.setIbudProdMsgGet("Missing Configuration in openbravo properties :"
            + configMap.get("Error"));
        OBDal.getInstance().save(order);
        SessionHandler.getInstance().commitAndStart();
        return getErrorMessage(msg);

      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      return getErrorMessage(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public int updateOrderDetails(List<Order> orderList, HashMap<String, String> configMap)
      throws Exception {
    int errorOrderCount = 0;
    Map<String, HashMap<String, String>> orderMap;

    try {
      String token = CommonServiceProvider.generateToken(configMap);
      if (!token.contains("Error_TokenGenerator")) {
        for (Order order : orderList) {

          orderMap = updateOrderHeaderDetails(order, token, configMap);
          if (orderMap.containsKey("error")) {
            errorOrderCount++;
          }
        }
      } else {
        errorOrderCount++;
        log.error("GetOrderDetailsFromProd: Error in generating token " + token);
      }
    } catch (Exception e) {
      errorOrderCount++;
      e.printStackTrace();
      log.error("Error while Getting the Orders from Prod and Error is: " + e);
    }
    return errorOrderCount;

  }

  public Map<String, HashMap<String, String>> updateOrderHeaderDetails(Order orderObj,
      String token, HashMap<String, String> configMap) throws Exception {
    Map<String, HashMap<String, String>> orderMap = new HashMap<String, HashMap<String, String>>();

    String orderHeaderData = GetDataFromProd(orderObj.getOrderReference(),
        orderObj.getDocumentNo(), token, configMap, "Header", null, orderObj.getBusinessPartner()
            .getIbudDppNo());
    SaveProdHeaderData(orderObj, orderHeaderData, orderMap);
    String orderHeaderLineData = GetDataFromProd(orderObj.getOrderReference(),
        orderObj.getDocumentNo(), token, configMap, "Lines", null, orderObj.getBusinessPartner()
            .getIbudDppNo());
    List<String> errorItemCodeList = SaveProdOrderLineData(orderObj, orderHeaderLineData, token,
        configMap, orderObj.getBusinessPartner().getIbudDppNo(), orderMap);
    if (!orderMap.containsKey("error") && errorItemCodeList.size() == 0) {
      savePOData(orderMap, orderObj);
    } else {
      log.error("Order No: " + orderObj.getDocumentNo() + " and Error is: " + orderMap.get("error")
          + ", So Skipping the Order.");
      log.error("Order Map for Order no is: " + orderObj.getDocumentNo() + " and Map is: "
          + orderMap + " and Not Present Itemcode list: " + errorItemCodeList);

    }
    return orderMap;
  }

  private void savePOData(Map<String, HashMap<String, String>> orderMap, Order orderObj)
      throws Exception {
    String pStatus = null;
    String oStatus = null;
    String adate = null;
    String edate = null;
    if (orderMap.containsKey("header")) {
      HashMap<String, String> headerdata = orderMap.get("header");
      if (headerdata.containsKey("status")) {
        pStatus = headerdata.get("status");
      }
      if (headerdata.containsKey("edate")) {
        edate = headerdata.get("edate");
      }
      if (headerdata.containsKey("adate")) {
        adate = headerdata.get("adate");
      }
      // pStatus = "V";
      if (pStatus != null) {
        if (pStatus.equalsIgnoreCase("V")) {
          oStatus = "VD";
        } else if (pStatus.equalsIgnoreCase("S")) {
          oStatus = "SH";
        } else if (pStatus.equalsIgnoreCase("C")) {
          oStatus = "CL";
        } else {
          oStatus = orderObj.getSWEMSwPostatus();
        }
      } else {
        oStatus = "OS";
      }

      if (pStatus != null) {
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
              SessionHandler.getInstance().commitAndStart();
            }
          }
        }

        orderObj.setSWEMSwPostatus(oStatus);
        orderObj.setIbudProdStatus(pStatus);
        Date aDate = null;
        Date eDate = null;
        if (adate != null) {
          aDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(adate);
          orderObj.setSWEMSwActshipdate(aDate);
        }
        if (edate != null) {
          eDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(edate);
          orderObj.setSWEMSwEstshipdate(eDate);
        }

        if (orderMap.containsKey("error"))
          orderObj.setIbudProdMsgGet(orderMap.get("error").toString());
        else
          orderObj.setIbudProdMsgGet("Successfully fetched data from prod");
        orderObj.setIbudPoStatusdate(new Date());
      }
      OBDal.getInstance().save(orderObj);
      SessionHandler.getInstance().commitAndStart();
    } else {
      log.error("Order Map for Order no: " + orderObj.getDocumentNo() + " and Map is: " + orderMap);

    }
  }

  private void SaveProdHeaderData(Order orderObj, String orderHeaderData,
      Map<String, HashMap<String, String>> orderMap) throws JSONException {
    try {
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
          }
        } else {
          HashMap<String, String> headerMap = orderMap.get("error");
          if (headerMap == null) {
            headerMap = new HashMap<String, String>();
            headerMap.put("error", " Order not Found,");
            orderMap.put("error", headerMap);
          } else {
            headerMap.get("error").concat(" Order not Found,");
          }
          log.error("Order Data Not Found for Order: " + orderObj.getDocumentNo());
        }
      } else {
        log.error("Order API Error is:" + orderHeaderData + " for Order: "
            + orderObj.getDocumentNo());

        HashMap<String, String> headerMap = orderMap.get("error");
        if (headerMap == null) {
          headerMap = new HashMap<String, String>();
          headerMap.put("error", " order Prod.com API Failed,");
          orderMap.put("error", headerMap);
        } else {
          headerMap.get("error").concat(" order Prod.com API Failed,");
        }
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

      log.error("Error while Saving the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);
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
                  orderObj.getDocumentNo(), token, configMap, "ProductElp", productElpCode, dpp);
              if (!orderLineElpDetails.contains("Error_GETAPI:")) {
                JSONArray orderLineElpArray = new JSONArray(orderLineElpDetails.toString());
                if (orderLineElpArray != null && orderLineElpArray.length() > 0) {
                  for (int p = 0; p < orderLineElpArray.length(); p++) {
                    JSONObject jsO = orderLineElpArray.getJSONObject(p);
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
        HashMap<String, String> headerMap = orderMap.get("error");
        if (headerMap == null) {
          headerMap = new HashMap<String, String>();
          headerMap.put("error", " OrderLine API got failed not Found,");
          orderMap.put("error", headerMap);
        } else {
          headerMap.get("error").concat(" OrderLine API got failed not Found,");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error while Saving the the order Header data for order: "
          + orderObj.getDocumentNo() + " and error is: " + e);

      HashMap<String, String> headerMap = orderMap.get("error");
      if (headerMap == null) {
        headerMap = new HashMap<String, String>();
        headerMap.put("error", " Error while getting the OrderLine data,");
        orderMap.put("error", headerMap);
      } else {
        headerMap.get("error").concat(" Error while getting the OrderLine data,");
      }
    }
    return notFoundItemListObj;
  }

  private String GetDataFromProd(String orderReferenceNo, String orderNo, String tokenKey,
      HashMap<String, String> configMap, String orderType, String elpProduct, String dpp) {
    HttpURLConnection conn = null;

    URL url = null;
    try {
      if (orderType.equalsIgnoreCase("Header")) {
        url = new URL(configMap.get("getOrder_url") + dpp + "?search=ordOrderNumber="
            + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("Lines")) {
        url = new URL(configMap.get("getOrderLine_url") + dpp + "/order_lines/" + orderReferenceNo);
      } else if (orderType.equalsIgnoreCase("ProductElp") && elpProduct != null) {
        url = new URL(configMap.get("getElpProduct_url") + elpProduct);
      } else {
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
        return "Error_GETAPI: Error While Getting the Order details(Finally) for order No: "
            + orderNo + " and Type is: " + orderType;
      }
    }

  }

  JSONObject getSuccessMessage(String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONArray actions = new JSONArray();
      final JSONObject msgInBPTab = new JSONObject();
      msgInBPTab.put("msgType", "success");
      msgInBPTab.put("msgTitle", OBMessageUtils.messageBD("success"));
      msgInBPTab.put("msgText", msgText);
      final JSONObject msgInBPTabAction = new JSONObject();
      msgInBPTabAction.put("showMsgInProcessView", msgInBPTab);
      actions.put(msgInBPTabAction);
      result.put("responseActions", actions);
    } catch (Exception e) {
      log.error(e);
    }

    return result;
  }

  private static JSONObject getErrorMessage(String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONArray actions = new JSONArray();
      final JSONObject msgInBPTab = new JSONObject();
      msgInBPTab.put("msgType", "error");
      msgInBPTab.put("msgTitle", OBMessageUtils.messageBD("error"));
      msgInBPTab.put("msgText", msgText);
      final JSONObject msgInBPTabAction = new JSONObject();
      msgInBPTabAction.put("showMsgInProcessView", msgInBPTab);
      actions.put(msgInBPTabAction);
      result.put("responseActions", actions);
    } catch (Exception e) {
      log.error(e);
    }
    return result;
  }

}
