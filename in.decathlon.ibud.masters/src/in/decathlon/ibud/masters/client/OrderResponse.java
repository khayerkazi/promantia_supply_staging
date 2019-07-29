package in.decathlon.ibud.masters.client;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.web.WebService;

public class OrderResponse implements WebService {
	
	  private static final Logger logger = Logger.getLogger(OrderResponse.class);

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	      logger.info("Order Reference started ");
	      
	      JSONObject responseJson = new JSONObject();
	      String orderNo = request.getParameter("orderNumber");
	      String anomaly = request.getParameter("anomaly");
	      
	      if(orderNo != null && anomaly == null)
	      {
	    	  responseJson.put("Success", "Order has been successfully created");
	    	  writeResult(response, responseJson);
	      }
	      if(orderNo == null && anomaly != null)
	      {
	    	  responseJson.put("ErrorMsg", anomaly.toString());
	    	  writeResult(response, responseJson); 
	      }
	      if(orderNo != null && anomaly != null)
	      {
	    	  responseJson.put("ErrorMsg", anomaly.toString());
	    	  writeResult(response, responseJson); 
	      }
	      
	      
	      
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	
	 private void writeResult(HttpServletResponse response, JSONObject result) throws IOException,
     NumberFormatException, JSONException {
   int status = Integer.parseInt((String) result.get("Status"));
   response.setContentType("application/json;charset=UTF-8");
   response.addHeader("Content-Type", "application/json;charset=UTF-8");
   response.addHeader("Access-Control-Allow-Origin", "*");
   response.setStatus(status);
   final Writer w = response.getWriter();
   w.write(result.toString());
   w.close();
 }
	
	
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

}
