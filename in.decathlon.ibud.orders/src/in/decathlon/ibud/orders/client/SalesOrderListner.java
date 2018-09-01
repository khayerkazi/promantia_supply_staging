package in.decathlon.ibud.orders.client;

import in.decathlon.supply.dc.util.AutoDCMails;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

/*
 * Creates sales orders and books them based on store requests.
 * 
 */
public class SalesOrderListner implements WebService {
  public static boolean IsSriLankaOrder = false;

  public static final Logger log = Logger.getLogger(SalesOrderListner.class);
  public JSONArray responseOrders = new JSONArray();

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    SaveSupplyData saveSuppData = new SaveSupplyData();
    JSONObject responseOrdLine = new JSONObject();
    JSONObject responseOrd = new JSONObject();
    JSONArray responseArr = new JSONArray();
    try {
      String orderIDs = request.getParameter("OrderIDs");
      String[] orders = orderIDs.split(",");

      for (int i = 0; i < orders.length; i++) {
        saveSuppData.getConfirmedQty(orders[i], null, responseOrdLine, responseOrd);
      }
      responseArr.put(responseOrd);
      writeResult(response, responseArr.toString());
    } catch (Exception e) {
      responseArr.put(responseOrd);
      writeResult(response, responseArr.toString());
      throw e;
    }
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      SaveSupplyData saveSuppData = new SaveSupplyData();
      String result = processServerResponse(request, response);
      Date start = new Date();
      JSONObject orders = new JSONObject(result);
      String DocNo = "";
      log.info("Orders from Store to create sales orders" + orders.length());

      Map<String, List<Order>> retailSupply = new HashMap<String, List<Order>>();

      JSONArray ordersArray = getOrders(orders);
      for (short i = 0; i < ordersArray.length(); i++) {
        JSONObject ordObj = (JSONObject) ordersArray.get(i);
        JSONObject ordHeader = (JSONObject) ordObj.get("Header");
        DocNo = ordHeader.getString(SOConstants.jsonDocNo);
        JSONArray ordLines = ordObj.getJSONArray("Lines");

        log.error("Request json object " + ordObj);
        if (ordHeader.has("client")) {
          if (!(ordHeader.get("client").equals(OBContext.getOBContext().getCurrentClient().getId()))) {
            IsSriLankaOrder = true;
            ordHeader = saveSuppData.updateOrderJsonWithCurrectIdForSL(ordHeader);
          }
        }
        JSONObject responseOrd = saveSuppData.distributeSalesOrder(ordHeader, ordLines,
            retailSupply, IsSriLankaOrder);

        log.error("Response json object " + responseOrd);
        response.setHeader("orders", responseOrd.toString());
      }
      Date end = new Date();
      log.debug(", " + SOConstants.perfOrmanceEnhanced + " SO creation time ," + DocNo + ", "
          + start + ", " + end + ", " + (end.getTime() - start.getTime()) / 1000);

      // Send an Email for Direct Delivery to Supplier
      Map<String, StringBuffer> documentNosForWarehouse = new HashMap<String, StringBuffer>();
      for (Entry<String, List<Order>> e : retailSupply.entrySet()) {
        for (Order o : e.getValue()) {
          if (documentNosForWarehouse.containsKey(o.getWarehouse().getId())) {
            StringBuffer docNos = documentNosForWarehouse.get(o.getWarehouse().getId());
            documentNosForWarehouse.put(o.getWarehouse().getId(),
                docNos.append(", " + o.getDocumentNo()));
          } else {
            documentNosForWarehouse.put(o.getWarehouse().getId(), new StringBuffer(
                "Replenishment Document Numbers - " + o.getDocumentNo()));
          }
        }
      }
      sendMailToSupplier(documentNosForWarehouse);

    } catch (Exception e) {
      e.printStackTrace();
      response.setHeader("Error", e.toString());
      log.error(e);
      throw e;
    }

  }

  private void sendMailToSupplier(Map<String, StringBuffer> documentNosForWarehouse) {
    try {
      AutoDCMails autoDCMails = AutoDCMails.getInstance();
      Set<String> documentsKeySet = documentNosForWarehouse.keySet();
      if (documentsKeySet == null || documentsKeySet.size() < 1)
        return;
      for (String key : documentsKeySet) {
        autoDCMails.mailToSupplierFromSupplyErp(key, documentNosForWarehouse.get(key));
      }
    } catch (Exception e) {
      log.error(e);
    }

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method not implemented");
  }

  public String processServerResponse(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String content = getContentFromRequest(request);
    return content;
  }

  private String getContentFromRequest(HttpServletRequest request) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, "UTF-8");
    String Orders = writer.toString();
    return Orders;
  }

  private JSONArray getOrders(JSONObject orders) throws JSONException {
    JSONArray arr = new JSONArray();
    arr = orders.getJSONArray("data");
    return arr;
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

}
