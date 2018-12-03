package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.shipment.supply.CompleteShpmnetWebservice;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.web.WebService;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;

public class SaveOrganizationSyncMasterData implements WebService {

  public static final Logger log = Logger.getLogger(CompleteShpmnetWebservice.class);

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
      log.info("entered doPOst of completeShipmentWS");
      JSONObject respObj = new JSONObject();
      HashMap<String, String> poStatusMap = new HashMap<String, String>();
      boolean flag = false;

      log.debug("Webservice invocation from store to complete goods shipment and close sales order");
      String shipmentDocumentno = request.getParameter("shipmentIdfromGRN");
      log.debug("Shipment document recieved from store is " + shipmentDocumentno);

      ShipmentInOut shipment = getShipment(shipmentDocumentno);

      String shipmentId = shipment.getId();

      shipment.setDocumentStatus("DR");
      shipment.setDocumentAction("CO");

      OBDal.getInstance().save(shipment);
      OBDal.getInstance().flush();
      shipment.setProcessGoodsJava("CO");

      if ("CO".equals(shipment.getDocumentStatus())) {

        flag = true;

      } else {

        shipment.setDocumentStatus("DR");
        shipment.setDocumentAction("CO");

        OBDal.getInstance().save(shipment);
        OBDal.getInstance().flush();
        shipment.setProcessGoodsJava("CO");

        try {
          flag = BusinessEntityMapper.executeProcess(shipmentId, "104",
              "SELECT * FROM M_InOut_Post0(?)");
        } catch (Exception e) {
          log.error("Error in documentno : " + shipment.getDocumentNo() + " and the error is : "
              + e.getMessage());
        }

      }

      completeShipping(shipment);

      OBDal.getInstance().save(shipment);
      log.debug("completed GS" + shipment.getDocumentNo());

      if (flag) {
        BusinessEntityMapper.closeSupplySO(shipment, poStatusMap);
        log.debug("Complete shipment webservice completed ");
      } else {
        for (ShipmentInOutLine inoutLine : shipment.getMaterialMgmtShipmentInOutLineList()) {
          OrderLine ordLine = inoutLine.getSalesOrderLine();
          if (ordLine != null) {

            Order ord = ordLine.getSalesOrder();
            if (ord != null) {
              BusinessEntityMapper.respForPOStatus(poStatusMap, ord);
            } else
              throw new OBException("no order reference for orderline " + ordLine);
          } else {
            throw new OBException("no orderline for GS line " + inoutLine);
          }
        }
      }

      respObj.put("data", poStatusMap);
      log.debug("response to update po " + poStatusMap);

      response.setHeader("Result", respObj.toString());
    } catch (Exception e) {
      e.printStackTrace();
      response.setHeader("Error", e.toString());

      log.error(e);
      throw e;
    }
  }

  private void completeShipping(ShipmentInOut shipmentInOut) {
    String qry = "select shipd.goodsShipment from OBWSHIP_Shipping ship "
        + " join ship.oBWSHIPShippingDetailsList shipd where ship.id = (select ship.id from OBWSHIP_Shipping ship "
        + " join ship.oBWSHIPShippingDetailsList shipd where shipd.goodsShipment.id ='"
        + shipmentInOut.getId() + "')" + " and shipd.goodsShipment.id <>'" + shipmentInOut.getId()
        + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    List<ShipmentInOut> queryList = query.list();
    Boolean allRecClosed = true;
    for (ShipmentInOut shipOut : queryList) {
      if (shipOut.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
        allRecClosed = true;
      } else {
        allRecClosed = false;
        break;
      }
    }
    if (allRecClosed.equals(true)) {
      OBWSHIPShipping shipping;
      String shippingQry = "select ship from OBWSHIP_Shipping ship join ship.oBWSHIPShippingDetailsList shipd "
          + "where shipd.goodsShipment.id =:shipmentId";
      Query shippingQuery = OBDal.getInstance().getSession().createQuery(shippingQry);
      shippingQuery.setParameter("shipmentId", shipmentInOut.getId());
      List<OBWSHIPShipping> shippingList = shippingQuery.list();
      if (shippingList != null & shippingList.size() > 0) {
        shipping = shippingList.get(0);
        shipping.setDocumentStatus(SOConstants.CompleteDocumentStatus);
        OBDal.getInstance().save(shipping);
      }
    }
  }

  private ShipmentInOut getShipment(String documentNo) {
    OBCriteria<ShipmentInOut> shpmntCrit = OBDal.getInstance().createCriteria(ShipmentInOut.class);
    shpmntCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, documentNo));
    shpmntCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, true));
    shpmntCrit.add(Restrictions.eq(ShipmentInOut.PROPERTY_CLIENT, OBContext.getOBContext()
        .getCurrentClient()));

    List<ShipmentInOut> shpmntCritList = shpmntCrit.list();
    if (shpmntCritList != null && shpmntCritList.size() > 0) {
      return shpmntCritList.get(0);
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
      ordQry = "id in (select ord.id from Order ord where ord.documentNo = '" + shipmentDoc
          + "' and ord.salesTransaction = :isSalesTransaction)";

      OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class, ordQry);
      orderQuery.setNamedParameter("isSalesTransaction", isSalesTransaction);

      List<Order> orderList = orderQuery.list();

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