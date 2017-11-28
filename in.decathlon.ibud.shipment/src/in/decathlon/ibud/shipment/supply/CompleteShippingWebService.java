package in.decathlon.ibud.shipment.supply;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.web.WebService;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;
import org.openbravo.warehouse.shipping.OBWSHIPShippingDetails;

public class CompleteShippingWebService implements WebService {
  public static final Logger log = Logger.getLogger(CompleteShippingWebService.class);

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      JSONObject respObj = new JSONObject();
      HashMap<String, String> poStatusMap = new HashMap<String, String>();

      boolean flag = false;

      log.debug("Webservice invocation from store to complete shipping, goods shipment and close sales order");
      String shippingDocNo = request.getParameter("truckId");
      log.debug("truck document recieved from store is " + shippingDocNo);

      String shippingId = getShippingOnDocNo(shippingDocNo);
      int count = 0;
      OBWSHIPShipping shipping = OBDal.getInstance().get(OBWSHIPShipping.class, shippingId);
      shipping.setDocumentStatus(SOConstants.CompleteDocumentStatus);
      for (OBWSHIPShippingDetails shipDetail : shipping.getOBWSHIPShippingDetailsList()) {
        ShipmentInOut goodShipment = shipDetail.getGoodsShipment();

        try {
          if ((count % 10) == 0) {
            SessionHandler.getInstance().commitAndStart();

          }

          flag = completeInOut(goodShipment);
        } catch (Exception e) {
          log.debug("Shipment Dint complete " + e.getMessage());

        }

        // close SO
        if (flag) {
          BusinessEntityMapper.closeSupplySO(goodShipment, poStatusMap);
          log.debug("Complete shipment webservice : Close SO: ");

        } else {
          for (ShipmentInOutLine inoutLine : goodShipment.getMaterialMgmtShipmentInOutLineList()) {
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

        log.debug("completed GS" + goodShipment.getDocumentNo());
        count++;
      }
      SessionHandler.getInstance().commitAndStart();
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

  private boolean completeInOut(ShipmentInOut goodShipment) throws Exception {
    boolean flag = false;
    goodShipment.setProcessGoodsJava("CO");

    OBDal.getInstance().save(goodShipment);
    OBDal.getInstance().flush();

    // complete the Goods shipment
    if (goodShipment.getDocumentStatus().equals(SOConstants.DraftDocumentStatus)) {
      flag = BusinessEntityMapper.executeProcess(goodShipment.getId(), "104",
          "SELECT * FROM M_InOut_Post0(?)");
      // OBDal.getInstance().refresh(goodShipment);
    }
    return flag;

  }

  private String getShippingOnDocNo(String shippingDocNo) {

    String qry = "select ship.id from  OBWSHIP_Shipping ship where documentNo='" + shippingDocNo
        + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    List<String> queryList = query.list();
    if (queryList != null && queryList.size() > 0) {
      return queryList.get(0);
    } else
      throw new OBException("No shipping record found with document no " + shippingDocNo);
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
