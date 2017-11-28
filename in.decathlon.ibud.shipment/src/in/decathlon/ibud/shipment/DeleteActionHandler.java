package in.decathlon.ibud.shipment;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.warehouse.shipping.OBWSHIPShippingDetails;

public class DeleteActionHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(DeleteActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
	  log.info("executing delete action handler ");
    JSONObject jsonResponse = new JSONObject();
    JSONObject jsonRequest = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      jsonRequest = new JSONObject(content);
      final JSONArray shipDetailIds = jsonRequest.getJSONArray("orders");
     
      for (int i = 0; i < shipDetailIds.length(); i++) {

      StringBuffer removeBoxProductsStrQuery = new StringBuffer();
      removeBoxProductsStrQuery.append("delete FROM " + OBWSHIPShippingDetails.ENTITY_NAME
          + " WHERE ");
      removeBoxProductsStrQuery.append(OBWSHIPShippingDetails.PROPERTY_ID + " = '" + shipDetailIds.getString(i)
          + "'");
      Query removeBoxProductQuery = OBDal.getInstance().getSession().createQuery(
          removeBoxProductsStrQuery.toString());
      removeBoxProductQuery.executeUpdate();
      }
      OBDal.getInstance().flush();
      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "error");
      errorMessage.put("text", "deleted the shipping details successfully");
      jsonResponse.put("message", "Successfully deleted Shipping Details");
      log.info("Successfully deleted Shipping Details");

    } catch (Exception e) {
      log.error(e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonResponse;
  }

}
