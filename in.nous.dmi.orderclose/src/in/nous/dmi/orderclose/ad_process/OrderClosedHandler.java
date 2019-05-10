package in.nous.dmi.orderclose.ad_process;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;

public class OrderClosedHandler extends BaseProcessActionHandler {
	  private static final Logger log = Logger.getLogger(OrderClosedHandler.class);
	  
	@Override
	protected JSONObject doExecute(Map<String, Object> parameters, String content) {
		// TODO Auto-generated method stub
		
		try {
			final JSONObject jsonData = new JSONObject(content);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
