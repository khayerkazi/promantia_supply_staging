package in.decathlon.ibud.shipment.supply;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.shipment.GetSupplyShipperData;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.web.WebService;

public class CopyTruckWebService implements WebService {

  public static final Logger log = Logger.getLogger(ShipmentWebService.class);
  GetSupplyShipperData getSupplyShippertData = new GetSupplyShipperData();
  public JSONArray responseshipments = new JSONArray();

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    log.debug(".........doGet.....");

    getShipperDataInJson(request, response);

  }

  private void getShipperDataInJson(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    JSONObject jsonResponse = new JSONObject();
    BusinessPartner bPartnerOrg = null;
    String updated = request.getParameter("updated");
    updated = updated.replace("_", " ");
    log.debug(updated);
    log.debug("Enter getShipperDataInJson");
    String orgId = request.getParameter("orgId");
    log.debug(orgId);
    bPartnerOrg = BusinessEntityMapper.getBPOfOrg(orgId);
    log.debug(bPartnerOrg);
    getSupplyShippertData.getJsonShipperDetails(bPartnerOrg, updated, response, "");

    // jsonResponse = getSupplyShipmentData.getJsonShipmentOrders(bPartnerOrg, updated, response);

    log.debug(jsonResponse);
    // return jsonResponse.toString();
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

}