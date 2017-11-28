package in.decathlon.retail.api.omniCommerce.ad_webservice;

import in.decathlon.retail.api.omniCommerce.util.OmniCommerceAPIUtility;

import java.io.PrintWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.web.WebService;

import com.sysfore.catalog.CLMinmax;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StoreWiseInRangeProducts implements WebService {
	
	private static final Logger LOG = Logger.getLogger(StoreWiseInRangeProducts.class);
	private static final OmniCommerceAPIUtility OmniCommerceAPIUtility = new OmniCommerceAPIUtility();

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();	
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		OBContext.setAdminMode();
		
		final String sampleRequest = request.getReader().readLine();
		
		final JSONObject requestObject = new JSONObject(sampleRequest);
		final String requestedStores = requestObject.getString("stores");
		
		final JSONObject storeWiseJsonObj = new JSONObject();
		final List<Organization> requestedStoreList = OmniCommerceAPIUtility.getRequestedStoresList(requestedStores);
		
		for (Organization store : requestedStoreList) {
			
			final JSONArray itemInfoJSONArray = new JSONArray(); 
			
			final OBCriteria<CLMinmax> clminmaxObCriteria = OBDal.getInstance().createCriteria(CLMinmax.class);
			clminmaxObCriteria.add(Restrictions.eq(CLMinmax.PROPERTY_ORGANIZATION, store));
			clminmaxObCriteria.add(Restrictions.eq(CLMinmax.PROPERTY_ISINRANGE, true));
			clminmaxObCriteria.setFilterOnReadableOrganization(false);
			
			if(clminmaxObCriteria.count() > 0) {
				for (CLMinmax clMinmax : clminmaxObCriteria.list()) {
					
					final JSONObject itemInfoJsonObj = new JSONObject();
					itemInfoJsonObj.put("itemcode", clMinmax.getProduct().getName());
					itemInfoJsonObj.put("modelcode", clMinmax.getProduct().getClModelcode());
					itemInfoJsonObj.put("modelname", clMinmax.getProduct().getClModelname());
					itemInfoJsonObj.put("size", clMinmax.getProduct().getClSize());
					itemInfoJSONArray.put(itemInfoJsonObj);
				}
				storeWiseJsonObj.put(store.getName(), itemInfoJSONArray);
			} else {
				storeWiseJsonObj.put(store.getName(), "");
			}
			
		}
		
		OBContext.restorePreviousMode();
		
		// Response
		response.setContentType("application/json");
		// Get the printwriter object from response to write the required json object to the output stream      
		PrintWriter out = response.getWriter();
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
		out.print(storeWiseJsonObj);
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
