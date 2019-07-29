package in.decathlon.ibud.masters.client;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

import java.util.List;

import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;



public class PushOrderDetailsToProd implements Process {

	  private static Logger log = Logger.getLogger(PushOrderDetailsToProd.class);
	  private ProcessLogger logger;

	@Override
	public void execute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
	    logger = bundle.getLogger();
	    logger.logln("Push order details to prod.com started process starting...");
	    List<Order> orderList = getPurchaseOrder();  
	    for(Order order : orderList){
	    	JSONObject orderObject = new JSONObject();
	    	orderObject.put("origin", "Retail");
	    	orderObject.put("externalNumber", Integer.parseInt(order.getDocumentNo()));
	    	orderObject.put("orderType", "Z");
	    	orderObject.put("orderStatus", order.getSWEMSwPostatus());
	    	orderObject.put("orderCreation", order.getCreationDate());
	    	orderObject.put("requestedDeliveryDate", order.getSWEMSwExpdeldate());
	    	orderObject.put("requestedShipmentDate", "");
	    	orderObject.put("scheduleDeliveryDate", order.getDatePrinted());
	    	
	    	JSONArray customerJsonArray = new JSONArray();
	    	customerJsonArray.put("FirstValue");
	    	customerJsonArray.put("secondValue");
	    	customerJsonArray.put("thirdValue");  	
	    	orderObject.put("customer", customerJsonArray);
	    	
	    	JSONArray supplierJsonArray = new JSONArray();
	    	supplierJsonArray.put("FirstValue");
	    	supplierJsonArray.put("secondValue");
	    	supplierJsonArray.put("thirdValue");

	    	
	    	orderObject.put("supplier", supplierJsonArray);
	    	
	    	JSONArray deliveryJsonArray = new JSONArray();
	    	deliveryJsonArray.put("FirstValue");
	    	deliveryJsonArray.put("secondValue");
	    	deliveryJsonArray.put("thirdValue");
	    	orderObject.put("delivery", deliveryJsonArray);
	    	
	    	JSONArray orderLineArray = new JSONArray();
	    	 
	    	for (OrderLine lines :order.getOrderLineList())
	    	{
	    		JSONObject orderlineJson = new JSONObject();
	  
	    		orderlineJson.put("number", lines.getLineNo());
	    		orderlineJson.put("status", "A");
	    		orderlineJson.put("item", Long.parseLong(lines.getProduct().getName()));
	    		orderlineJson.put("qty", lines.getOrderedQuantity());
	    		orderlineJson.put("price", lines.getGrossUnitPrice());
	    		orderlineJson.put("currency", lines.getCurrency().getISOCode());
	    		orderLineArray.put(orderlineJson);
	    	}
	    	orderObject.put("orderLines", orderLineArray); 
	    	System.out.println(orderObject.toString());
	    	logger.logln(orderObject.toString());
	    }
	    
		
		
	}

	private List<Order> getPurchaseOrder() {
		String strSql = "select co from Order co where co.salesTransaction = 'N' and co.documentStatus = 'CO'" ;
		Query query = OBDal.getInstance().getSession().createQuery(strSql);
		List<Order> orderlist = query.list();
		return orderlist;		
	}

}
