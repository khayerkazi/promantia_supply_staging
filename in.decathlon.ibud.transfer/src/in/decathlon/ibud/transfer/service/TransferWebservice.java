package in.decathlon.ibud.transfer.service;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;

public class TransferWebservice implements WebService {

  SaveTransferRecord saveTrRecord = new SaveTransferRecord();
  public static final Logger log = Logger.getLogger(TransferWebservice.class);
  public JSONArray responseOrders = new JSONArray();
  public JSONArray responseShipMent = new JSONArray();
  public JSONArray responsepi = new JSONArray();

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      log.debug("Webservice invocation");

      String result = processServerResponse(request, response);
      JSONObject orders = new JSONObject(result);
      JSONArray shipmet = null, rmsShipment = null, ordersArray = null;
      if (orders.has("dataGRN"))
        shipmet = getReturnsData(orders, "dataGRN");
      if (orders.has("dataRMS"))
        rmsShipment = getReturnsData(orders, "dataRMS");
      if (orders.has("dataRTV"))
        ordersArray = getReturnsData(orders, "dataRTV");
      if (rmsShipment != null && ordersArray != null) {
        if (rmsShipment.length() > 0 && ordersArray.length() > 0) {
          for (short i = 0; i < ordersArray.length(); i++) {
            JSONObject ordObj = (JSONObject) ordersArray.get(i);
            JSONObject ordHeader = (JSONObject) ordObj.get("OrderHeader");
            JSONArray ordLines = ordObj.getJSONArray("OrderLines");

            JSONObject responseOrd = saveTrRecord.createAndSaveRFCOrder(ordHeader, ordLines);

            log.debug(responseOrd);
            responseOrders.put(i, responseOrd);

            response.setHeader("orders", responseOrders.toString());
          }
          log.debug("RFC Created and saved");
          OBDal.getInstance().flush();

          for (short i = 0; i < rmsShipment.length(); i++) {
            JSONObject shipMentObj = (JSONObject) rmsShipment.get(i);
            JSONObject shipMentHeader = (JSONObject) shipMentObj.get("ReceiptHeader");
            JSONArray shipMentLines = shipMentObj.getJSONArray("ReceiptLines");

            JSONObject responseShpMent = saveTrRecord.createAndSaveRMRShipment(shipMentHeader,
                shipMentLines);

            log.debug(responseShpMent);
            responseShipMent.put(i, responseShpMent);

            response.setHeader("shipment", responseShipMent.toString());
          }
        }
        log.debug("RMR Created and saved");
        OBDal.getInstance().flush();
      }
      if (shipmet != null && shipmet.length() > 0) {
        for (short i = 0; i < shipmet.length(); i++) {
          JSONObject grnMentObj = (JSONObject) shipmet.get(i);
          JSONObject grnHeader = (JSONObject) grnMentObj.get("ReceiptHeader");
          JSONArray grnLines = grnMentObj.getJSONArray("ReceiptLines");

          JSONObject responseShipment = saveTrRecord
              .createAndSaveGoodsShipment(grnHeader, grnLines);

          log.debug(responseShipment);
          responsepi.put(i, responseShipment);

          response.setHeader("Shipment", responsepi.toString());
        }
        log.debug("Shipment Created and saved");
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      throw e;
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

    // System.out.println(content);
    return content;
  }

  private String getContentFromRequest(HttpServletRequest request) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, "UTF-8");
    String Orders = writer.toString();
    return Orders;
  }

  private JSONArray getReturnsData(JSONObject orders, String delimeter) throws JSONException {
    try {
      JSONArray arr = new JSONArray();
      arr = orders.getJSONArray(delimeter);
      return arr;
    } catch (Exception e) {
      log.error("Error", e);
    }
    return null;
  }

}