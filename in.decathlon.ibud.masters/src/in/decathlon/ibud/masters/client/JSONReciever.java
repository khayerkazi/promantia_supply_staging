package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.orders.client.SOConstants;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.json.JsonConstants;

public class JSONReciever {
  public static final Logger log = Logger.getLogger(JSONReciever.class);

  public static void saveData(String output, String processid) throws Exception {

    JSONArray arr = new JSONArray();
    JSONObject obj = new JSONObject(output);
    arr.put(obj);
    JSONObject errorObject = new JSONObject();
    errorObject.put(SOConstants.RECORD_ID, obj.get(JsonConstants.ID));
    errorObject.put(SOConstants.recordIdentifier, obj.get(JsonConstants.IDENTIFIER));
    String entityName = obj.getString(JsonConstants.ENTITYNAME);
    errorObject.put(SOConstants.TABLE_NAME, entityName);

    try {
      JSONHelper.saveJSONObject(arr);
    } catch (Exception e) {
      errorObject.put(SOConstants.ERROR, e);
      BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
    }
  }

}
