package in.nous.creditnote.ad_webservices;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OxylaneIdMerger implements WebService {

	private static final Logger LOG = Logger.getLogger(OxylaneIdMerger.class);
	
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
//		final String givenRequest = "{\"data\":[{\"dids\":\"2090229827159,2090229827135,2090229860286,2090229853813,2090229851451,2090229850836,2090229846945,2090289024826,2090229836724\",\"mergedBy\":\"2090289073169\",\"updatedBy\":\"53451506CAA9402AA8F89C3F53F509BF\"}]}";
		final String givenRequest = request.getReader().readLine();
		final StringBuilder responseString = new StringBuilder();
		responseString.append("{\"data\":[");
		
		final JSONObject givenRequestJSON = new JSONObject(givenRequest);
		final JSONArray dataJSONArray = givenRequestJSON.getJSONArray("data");
		
		try {
		for (int i = 0; i < dataJSONArray.length(); i++) {
			
			final JSONObject jsonObject = (JSONObject) dataJSONArray.get(i);
			final String oldDecathlonIds = jsonObject.getString("dids");
			final String newDecathlonId = jsonObject.getString("mergedBy");
			final String updatedByUserId = jsonObject.getString("updatedBy");
			
			if(null != oldDecathlonIds && !"".equals(oldDecathlonIds)) {
				
				for (String oldDecathlonId : oldDecathlonIds.split(",")) {
					final OBCriteria<Order> orderObCriteria = OBDal.getInstance().createCriteria(Order.class);
					orderObCriteria.add(Restrictions.eq(Order.PROPERTY_RCOXYLANENO, oldDecathlonId));
					orderObCriteria.add(Restrictions.eq(Order.PROPERTY_PROCESSED, true));
					orderObCriteria.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
					LOG.info("total number of sales orders are "+orderObCriteria.count());
					
					if(orderObCriteria.count() > 0) {
						for (Order order : orderObCriteria.list()) {
							order.setRCOxylaneno(newDecathlonId);
							order.setDescription("Disabled Decathlon ID: "+oldDecathlonId);
							order.setUpdated(new Timestamp(new Date().getTime()));
							
							OBDal.getInstance().save(order);
							OBDal.getInstance().flush();
						}
					}
					
					// emptying the decathlon Id for this customer
					/*final OBCriteria<BusinessPartner> bpartnerObCriteria = OBDal.getInstance().createCriteria(BusinessPartner.class);
					bpartnerObCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_RCOXYLANE, oldDecathlonId));
					LOG.info("total number of bpartners are "+bpartnerObCriteria.count());
					
					if(bpartnerObCriteria.count() > 0) {
						for (BusinessPartner businessPartner : bpartnerObCriteria.list()) {
							businessPartner.setRCOxylane("");
							
							OBDal.getInstance().save(businessPartner);
							OBDal.getInstance().flush();
						}
					}*/
				}
				OBDal.getInstance().commitAndClose();
				
			}
			responseString.append("{\"custidentifier\":\""+newDecathlonId+"\",\"status\":\"200\"}");
			
			
		}
		responseString.append("]}");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Response
		response.setContentType("application/json");
		// Get the printwriter object from response to write the required json object to the output stream      
		PrintWriter out = response.getWriter();
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
		out.print(responseString.toString());
		out.flush();
		out.close();
		
		
	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();		
	}

}
