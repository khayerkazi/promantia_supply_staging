package in.decathlon.ibud.masters.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

public class GetPODetails extends BaseProcessActionHandler {
  private static final Logger log = Logger.getLogger(GetPODetails.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String data) {

    JSONObject jsonData;
    Order order = null;
    StringBuilder logMsg = new StringBuilder(" ");

    try {
      OBContext.setAdminMode(true);
      jsonData = new JSONObject(data);
      String orderId = jsonData.getString("inpcOrderId");
      order = OBDal.getInstance().get(Order.class, orderId);
      HashMap<String, String> configMap = CommonServiceProvider.checkObConfig();

      if (!configMap.containsKey("Error")) {
        log.info("Getting OB List of Order for Getting Order details from Prod.com for Order No : "
            + order.getDocumentNo());
        String returnMsg = updateOrderDetails(order, configMap, logMsg);
        if (!logMsg.toString().trim().equalsIgnoreCase("")) {
          log.error(logMsg.toString());

        }
        logMsg = new StringBuilder(" ");
        if (!returnMsg.contains("Error_")) {
          String msg = "Sucessfully Fetched the data from Prod.com.for current order :"
              + order.getDocumentNo();
          log.info(msg);
          order.setIbudProdMsgGet(msg);
          SessionHandler.getInstance().commitAndStart();
          return getSuccessMessage(msg);
        } else {
          String msg = returnMsg.replace("Error_", "");
          log.error(msg);
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
      order.setIbudProdMsgGet("Error while getting, Please try again after some time.");
      OBDal.getInstance().save(order);
      SessionHandler.getInstance().commitAndStart();
      log.error(e);
      return getErrorMessage(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
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

  public String updateOrderDetails(Order orderObj, HashMap<String, String> configMap,
      StringBuilder logMsg) throws Exception {
    String msg = "";
    try {
      GetOrderDetailsFromProd GetOrderDetailsFromProdobj = new GetOrderDetailsFromProd();

      String token = CommonServiceProvider.generateToken(configMap);
      if (!token.contains("Error_TokenGenerator")) {
        if (orderObj != null) {
          Map<String, String> orderMap = GetOrderDetailsFromProdobj.updateOrderHeaderDetails(
              orderObj, token, configMap, logMsg);
          if (orderMap.containsKey("error")) {
            msg = "Error_" + orderMap.get("error");

          } else {
            msg = "sucess";
          }
        }
      } else {
        logMsg.append("GetOrderDetailsFromProd: Error in generating token " + token + " \n");
        log.error("GetOrderDetailsFromProd: Error in generating token " + token);

      }
    } catch (Exception e) {

      e.printStackTrace();
      log.error("Error while Getting the Orders from Prod and Error is: " + e);
      logMsg.append("Error while Getting the Orders from Prod and Error is: " + e + " \n");
    }
    return msg;
  }
}
