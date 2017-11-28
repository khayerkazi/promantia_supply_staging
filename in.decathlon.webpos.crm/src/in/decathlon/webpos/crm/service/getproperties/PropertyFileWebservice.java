package in.decathlon.webpos.crm.service.getproperties;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.mobile.core.process.JSONProcessSimple;
import org.openbravo.service.json.JsonConstants;

public class PropertyFileWebservice extends JSONProcessSimple {

  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    JSONObject result = new JSONObject();
    JSONObject json = new JSONObject();

    json.put(
        "geturl",
        OBPropertiesProvider.getInstance().getOpenbravoProperties()
            .getProperty("crm.webservice.geturl"));
    json.put(
        "posturl",
        OBPropertiesProvider.getInstance().getOpenbravoProperties()
            .getProperty("crm.webservice.posturl"));
    json.put("offlinesynchurl", OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("crm.webservice.offlinesynchurl"));
    json.put("username",
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("crm.username"));
    json.put("password",
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("crm.password"));

    result.put(JsonConstants.RESPONSE_DATA, json);
    result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);

    return result;
  }
}