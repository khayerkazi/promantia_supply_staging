package in.decathlon.supply.stockreception.ad_webservice;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.web.WebService;

/*
 * 
 * This Completes shipment 
 * and Closes Sales Order linked with shipment
 * 
 */

public class CompleteShipmentInSupply implements WebService {

  public static final Logger log = Logger.getLogger(CompleteShipmentInSupply.class);

  public JSONArray responseShipMent = new JSONArray();
  public static final String shuttleBin = "Shuttel Bin";

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
      log.debug("Webservice invocation from store to complete goods shipment and close sales order");
      String shipmentDocumentno = request.getParameter("shipmentIdfromGRN");
      log.debug("Shipment document recieved from store is " + shipmentDocumentno);

      String query = "select documentNo from MaterialMgmtShipmentInOut where documentNo=:documentno and salesTransaction='Y'";

      Query resultset = OBDal.getInstance().getSession().createQuery(query);
      resultset.setParameter("documentno", shipmentDocumentno);

      ShipmentInOut shipment = getShipment(shipmentDocumentno);

      String shipmentId = shipment.getId();

      shipment.setDocumentStatus("DR");
      shipment.setDocumentAction("CO");

      OBDal.getInstance().save(shipment);
      OBDal.getInstance().flush();
      shipment.setProcessGoodsJava("CO");

      boolean flag = BusinessEntityMapper.executeProcess(shipmentId, "104",
          "SELECT * FROM M_InOut_Post0(?)");

      OBDal.getInstance().save(shipment);
      log.debug("completed GS" + shipment.getDocumentNo());

      if (flag) {
        boolean closeSO = closeSOinSupply(shipment);
        log.debug("Complete shipment webservice : Close SO: " + closeSO);
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
      throw e;
    }
  }

  private ShipmentInOut getShipment(String documentNo) {

    String query = "from MaterialMgmtShipmentInOut where documentNo='" + documentNo
        + "' and salesTransaction='Y'";
    Query resultset = OBDal.getInstance().getSession().createQuery(query);
    List<ShipmentInOut> resultlist = new ArrayList<ShipmentInOut>(resultset.list());
    if (resultset.list().size() > 0) {
      return resultlist.get(0);
    } else {
      throw new OBException("shipment " + documentNo + " not found");
    }

  }

  private boolean closeSOinSupply(ShipmentInOut io) {
    try {
      boolean flag = false;
      String shipmentDoc = io.getDocumentNo();

      Boolean isSalesTransaction = io.isSalesTransaction();
      String ordQry = "";
      ordQry = "from Order where documentNo='" + shipmentDoc + "' and salesTransaction='Y'";
      Query orderQuery = OBDal.getInstance().getSession().createQuery(ordQry);

      List<Order> orderList = new ArrayList<Order>(orderQuery.list());

      for (Order ord : orderList) {
        ord.setDocumentStatus("CL");
        ord.setDocumentAction("--");
        ord.setProcessed(true);
        OBDal.getInstance().save(ord);
        OBDal.getInstance().flush();
      }
      return flag;
    } catch (Exception e) {
      log.debug("Error closing PO/SO" + e);
    }
    return false;
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

}