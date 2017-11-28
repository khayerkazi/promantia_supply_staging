package in.decathlon.ibud.shipment;

import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.shipment.supply.GetSupplyShipmentData;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;
import org.openbravo.warehouse.shipping.OBWSHIPShippingDetails;

public class GetSupplyShipperData {
  public static final Logger log = Logger.getLogger(GetSupplyShipmentData.class);
  JsonToDataConverter fromJsonToData = OBProvider.getInstance().get(JsonToDataConverter.class);

  public void getJsonShipperDetails(BusinessPartner bPartner, String updatedTime,
      HttpServletResponse response, String docNo) throws Exception {

    JSONObject shipper = new JSONObject();
    JSONArray shipperArray = new JSONArray();
    try {
      PrintWriter wr = response.getWriter();
      log.debug(".......getJsonShipperDeatils..........");

      Date currentUpdatedTime = new Date();
      String qry = "";
      // select id from OBWSHIP_Shipping as shipper where shipper.documentStatus like '%SHIP%'
      if (bPartner != null) {
        qry = "id in (select id from OBWSHIP_Shipping as shipper" + " where shipper.updated   > '"
            + updatedTime
            + "' and shipper.documentStatus='SHIP' and shipper.businessPartner.id = '"
            + bPartner.getId() + "')";
      } else {
        qry = "id in (select id from OBWSHIP_Shipping as shipper" + " where shipper.updated   > '"
            + updatedTime + "' and shipper.documentStatus='SHIP')";
      }

      log.debug(".....qry..." + qry);
      OBQuery<OBWSHIPShipping> shipperQuery = OBDal.getInstance().createQuery(
          OBWSHIPShipping.class, qry);

      List<OBWSHIPShipping> shipperDetails = shipperQuery.list();

      if (shipperDetails != null && shipperDetails.size() > 0) {
        log.debug("............enter if condition of Shipper ");

        for (OBWSHIPShipping obwshipper : shipperDetails) {
          JSONObject shipperObj = new JSONObject();
          JSONObject shipperHeader = new JSONObject();
          JSONArray shipperlinedeatils = new JSONArray();

          shipperHeader = JSONHelper.convetBobToJson(obwshipper);
          shipperlinedeatils = getShippertHeaderLine(obwshipper);

          shipperObj.put("Header", shipperHeader);
          shipperObj.put("Lines", shipperlinedeatils);

          shipperArray.put(0, shipperObj);
          shipper.put("data", shipperArray);

          shipper.put("updatedTime", currentUpdatedTime);
          shipper.put("endOfObj", "&&&&");
          wr.write(shipper.toString());
          wr.flush();

        }

      } else {
        shipper.put("data", shipperArray);

        shipper.put("updatedTime", currentUpdatedTime);
        shipper.put("endOfObj", "&&&&");
        wr.write(shipper.toString());
        wr.flush();
      }

    }

    catch (Exception e) {
      e.printStackTrace();
      throw new OBException("error in retrieving shipping record with businesspartner " + bPartner);

    }

  }

  public JSONArray getShippertHeaderLine(OBWSHIPShipping shipper) throws JSONException {
    log.debug("......getShipperHeaderLine....");
    JSONArray shipperLines = new JSONArray();
    OBCriteria<OBWSHIPShippingDetails> shipperLineCriteria = OBDal.getInstance().createCriteria(
        OBWSHIPShippingDetails.class);
    shipperLineCriteria.add(Restrictions.eq(OBWSHIPShippingDetails.PROPERTY_OBWSHIPSHIPPING,
        shipper));

    List<OBWSHIPShippingDetails> shipperLineList = shipperLineCriteria.list();
    if (shipperLineList != null && shipperLineList.size() > 0) {
      for (OBWSHIPShippingDetails shipperDetail : shipperLineList) {

        shipperLines.put(JSONHelper.convetBobToJson(shipperDetail));

      }
    }
    return shipperLines;
  }

}
