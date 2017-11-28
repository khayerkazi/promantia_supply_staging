/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.nous.dmi.orderclose.ad_process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.CallStoredProcedure;

public class MultiSendOrderHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(MultiSendOrderHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    try {
      final JSONObject jsonData = new JSONObject(content);
      final JSONArray orderIds = jsonData.getJSONArray("orders");
      final String action = jsonData.getString("action");
      JSONObject result = new JSONObject();

      for (int i = 0; i < orderIds.length(); i++) {
        final String orderId = orderIds.getString(i);

        /*
         * String orderQry =
         * "as o where (o.businessPartner.id='CA680031D81440839FBDB832101837B2' or o.businessPartner.id='28E76A2334C94682886C875B2CCD1CA0')  "
         * + "and o.salesTransaction = false and o.id= '" + orderId + "'"; OBQuery<Order>
         * ordLineQuery = OBDal.getInstance().createQuery(Order.class, orderQry); /*Order order =
         * OBDal.getInstance().get(Order.class,orderId); final List<Object> para = new
         * ArrayList<Object>(); para.add(0,order); ordLineQuery.setParameters(para);
         * ordLineQuery.setMaxResult(1);
         * 
         * List<Order> countOrder = ordLineQuery.list(); System.out.println("orderObj: " +
         * countOrder);
         * 
         * if(countOrder.size() > 0){ SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         * Date date1 = sdf.parse(countOrder.get(i).getSWEMSwExpdeldate().toString()); Date date2 =
         * sdf.parse("2016-01-18"); if
         * (action.contains("sendorder")||action.contains("updateorder")) {
         * 
         * final Date getSWEMSwExpdeldate = countOrder.get(0).getSWEMSwExpdeldate(); DateTime
         * currentExpdelDate = new DateTime(getSWEMSwExpdeldate); DateTime currentDate = new
         * DateTime(); java.util.Date input = getSWEMSwExpdeldate; DateTimeZone zone =
         * DateTimeZone.forID("UTC");
         * 
         * LocalDate userDate = new LocalDate(input, zone); boolean exclude = new
         * LocalDate(zone).plusDays(32).isAfter(userDate);
         * 
         * if (exclude) {
         * 
         * System.out.println(exclude+ " - Update error msg "); return
         * updateJsonResponse("TYPE_ERROR", "@CDDCannotBeLessThan32Days@",
         * OBMessageUtils.messageBD("ValidationError"));
         * 
         * } else if(date2.after(date1)) { result =
         * sendOrder(orderId,action);System.out.println(result); } }
         * 
         * } orderQry = "as o where  o.salesTransaction = false and o.id= '" + orderId + "'";
         * OBQuery<Order> ordLineQry = OBDal.getInstance().createQuery(Order.class, orderQry);
         * System.out.println("orderObj: " + ordLineQry.list()); if(ordLineQry.list().size()>0) { if
         * (action.contains("sendorder")||action.contains("updateorder")) {
         * System.out.println(" Its send order condition "
         * +ordLineQry.list().get(i).getDocumentNo()); SimpleDateFormat sdf = new
         * SimpleDateFormat("yyyy-MM-dd"); Date date1 =
         * sdf.parse(ordLineQry.list().get(i).getSWEMSwExpdeldate().toString()); Date date2 =
         * sdf.parse("2016-01-18");
         * 
         * if(date1.after(date2)) {
         * System.out.println(" - Update error msg "+"CDD"+date1+"last date"+date2); return
         * updateJsonResponse("TYPE_ERROR",
         * "Order with CDD greater than 18th Jan cannot be created in Openbravo.Please create the same in SAP. If you are not trained please contact Monitors(Joshua/Sunita/Saikat)"
         * , OBMessageUtils.messageBD("ValidationError")); } else{ result =
         * sendOrder(orderId,action);System.out.println("Not error"); } } else {
         * 
         * result = sendOrder(orderId,action);System.out.println("not send order"); }}
         */

        result = sendOrder(orderId, action);
        System.out.println(result);
      }

      result.put("updated", orderIds.length());
      return result;

    } catch (Exception e) {
      throw new OBException(e);
    }
  }

private JSONObject sendOrder(String orderId, String action) {
	    
    final List<Object> parameters = new ArrayList<Object>();
	final String procedureName = "ndoc_multi_send_order";
	Boolean control = false;
	JSONObject finalresult = new JSONObject();
	
	try {
		parameters.add(orderId);
		parameters.add(action);
		parameters.add(OBContext.getOBContext().getUser().getId());
	    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
		OBDal.getInstance().commitAndClose();
		control = true;
	} catch (Exception e){
		control = false;
		throw new OBException("Process dint complete successfully", e);
	} 

	if(control) {
		
		finalresult = updateJsonResponse("TYPE_SUCCESS", "Success", OBMessageUtils.messageBD("Success"));
		
	} else {
		
		finalresult = updateJsonResponse("TYPE_ERROR", "Process dint complete successfully", "Error");
	}
    
	return finalresult;
}

private JSONObject updateJsonResponse(String severity, String errormsg, String title) {

	JSONObject result = new JSONObject();
	JSONObject JsonResult = new JSONObject();
	
	try {
		
		result.put("severity", severity);
		result.put("text", errormsg);
		result.put("title", title);
		JsonResult.put("message", result);
		
	} catch (Exception e){
		throw new OBException("Cannot update JSON Response", e);
	}
	return JsonResult;
}

}
