package in.decathlon.ibud.shipment.supply;

import in.decathlon.ibud.commons.JSONHelper;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.json.JsonToDataConverter;

public class GetSupplyShipmentData {
  public static final Logger log = Logger.getLogger(GetSupplyShipmentData.class);
  JsonToDataConverter fromJsonToData = OBProvider.getInstance().get(JsonToDataConverter.class);

  public void getJsonShipmentOrders(BusinessPartner bPartner, String updatedTime,
      HttpServletResponse response, String docNo) throws Exception {

    JSONObject shipmentInout = new JSONObject();
    JSONArray shipmentArray = new JSONArray();
    try {
      PrintWriter wr = response.getWriter();
      log.debug(".......getJsonShipmentOrders..........");

      Date currentUpdatedTime = new Date();
      String qry = "";
      /*
       * select mi.id,s.documentStatus from MaterialMgmtShipmentInOut mi join
       * mi.oBWSHIPShippingDetailsList as sd join sd.obwshipShipping as s where
       * s.documentStatus='SHIP'
       */
      List<ShipmentInOut> inoutlist = getGSRecords(bPartner, updatedTime);

      if (inoutlist != null && inoutlist.size() > 0) {
        log.debug("............enter if condition of ShipmentInOut ");

        for (ShipmentInOut minout : inoutlist) {
          JSONObject shipmentObj = new JSONObject();
          JSONObject inoutHeader = new JSONObject();
          JSONArray inoutLines = new JSONArray();

          inoutHeader = JSONHelper.convetBobToJson(minout);
          inoutLines = getShipmentHeaderLine(minout);
          shipmentObj.put("Header", inoutHeader);
          shipmentObj.put("Lines", inoutLines);

          shipmentArray.put(0, shipmentObj);
          shipmentInout.put("data", shipmentArray);

          shipmentInout.put("updatedTime", currentUpdatedTime);
          shipmentInout.put("endOfObj", "&&&&");
          wr.write(shipmentInout.toString());
          wr.flush();

        }

      } else {
        shipmentInout.put("data", shipmentArray);

        shipmentInout.put("updatedTime", currentUpdatedTime);
        shipmentInout.put("endOfObj", "&&&&");
        wr.write(shipmentInout.toString());
        wr.flush();
      }

    }

    catch (Exception e) {
      e.printStackTrace();
      throw new OBException("error in retrieving goods shipment record with businesspartner " +bPartner);
    }

  }

  public List<ShipmentInOut> getGSRecords(BusinessPartner bPartner, String updatedTime) {
    String qry;
    if (bPartner != null) {
      qry = "id in (select shio.id from MaterialMgmtShipmentInOut shio"
          + " join shio.oBWSHIPShippingDetailsList as shippingdetail"
          + " join shippingdetail.obwshipShipping as shipping"
          + " where  shio.businessPartner.id ='" + bPartner.getId()
          + "'  and shipping.updated   > '" + updatedTime
          + "' and shipping.documentStatus='SHIP')";
    } else {
      qry = "id in (select shio.id from MaterialMgmtShipmentInOut shio "
          + " join shio.oBWSHIPShippingDetailsList as shippingdetail "
          + " join shippingdetail.obwshipShipping as shipping" + " where  shipping.updated  > '"
          + updatedTime + "' and shipping.documentStatus='SHIP')";
    }
    log.debug(".....qry..." + qry);
    OBQuery<ShipmentInOut> shipInoutQuery = OBDal.getInstance().createQuery(ShipmentInOut.class,
        qry);

    List<ShipmentInOut> inoutlist = shipInoutQuery.list();
    return inoutlist;
  }

  public JSONArray getShipmentHeaderLine(ShipmentInOut minout) throws HibernateException, Exception {
    log.debug("......getShipmentHeaderLine....");
    JSONArray inoutLines = new JSONArray();
    OBCriteria<ShipmentInOutLine> shipmentLineCrit = OBDal.getInstance().createCriteria(
        ShipmentInOutLine.class);
    shipmentLineCrit.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, minout));

    List<ShipmentInOutLine> inoutLineList = shipmentLineCrit.list();
    if (inoutLineList != null && inoutLineList.size() > 0) {
      for (ShipmentInOutLine inoutline : inoutLineList) {
        // JSONObject inoutLineObj = JSONHelper.convetBobToJson(inoutline);
        // inoutLineObj.put("", inoutLineList);
        inoutLines.put(JSONHelper.convetBobToJson(inoutline));
        // inoutLines.put(soDeails);
      }
    }
    return inoutLines;
  }

  private String getSoDocumentNo(String orderId) throws HibernateException, Exception {
    OBCriteria<Order> order = OBDal.getInstance().createCriteria(Order.class);
    order.add(Restrictions.eq(Order.PROPERTY_ID, orderId));
    order.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
    List<Order> orderList = order.list();
    if (orderList != null && orderList.size() > 0) {
      return order.list().get(0).getDocumentNo();
    } else {

      throw new Exception("No Sales Order with the document No"
          + order.list().get(0).getDocumentNo());
    }

  }

}
