package in.decathlon.ibud.transfer.service;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.web.WebService;

public class ReturnsManuallWS implements WebService {

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doDelete method not implemented");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doGet method not implemented");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    JSONObject jsonResponse = new JSONObject();
    ReturnsUtility utility = new ReturnsUtility();
    String content = processResponse(request, response);
    JSONObject contentData = new JSONObject(content);
    JSONObject rtvs = contentData.getJSONObject(JsonConstants.DATA);
    JSONObject rtvsheaderjson = rtvs.getJSONObject("Header");
    JSONArray rtvsLinesJson = rtvs.getJSONArray("Lines");

    Order rfc = utility.createRFCOrder(rtvsheaderjson); // creates and returns RFC header
    // creates & returns RFC lines
    List<OrderLine> rfcLines = utility.createRFCOrderLines(rtvsLinesJson, rfc);

    rfc.getOrderLineList().addAll(rfcLines);// adds lines to header record
    OBDal.getInstance().save(rfc); // saves RFC record with lines.

    jsonResponse.put("rfc", rfc.getId());

    if (rfc.isIbodtrIsCreatedbyidl() != null && rfc.isIbodtrIsCreatedbyidl()) {
      OBDal.getInstance().flush();
      BusinessEntityMapper.executeProcess(rfc.getId(), "109", "SELECT * FROM C_Order_post(?)");
    }

    ShipmentInOut rms = utility.createRMSShipment(rtvsheaderjson, rfc);
    List<ShipmentInOutLine> rmsLines = utility.getRMSLines(rfcLines, rms, true);
    rms.getMaterialMgmtShipmentInOutLineList().addAll(rmsLines);
    OBDal.getInstance().save(rms);
    if (rfc.isIbodtrIsCreatedbyidl() != null && rfc.isIbodtrIsCreatedbyidl()) {
      OBDal.getInstance().flush();
      BusinessEntityMapper.executeProcess(rms.getId(), "109", "SELECT * FROM M_InOut_Post0(?)");
    }
    //BusinessEntityMapper.txnSWMovementType(rms);
    SessionHandler.getInstance().commitAndStart();
    jsonResponse.put("rms", rms.getId());
    response.setHeader("orders", jsonResponse.toString());

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPut method not Implemented");
  }

  private String processResponse(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String content = getContentFromRequest(request);
    return content;
  }

  private String getContentFromRequest(HttpServletRequest request) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, "UTF-8");
    String rtvs = writer.toString();
    return rtvs;
  }

}
