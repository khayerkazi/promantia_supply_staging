package in.nous.tools.mrpsticker.webservice;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.service.web.WebService;

public class AttributeSetInstanceWs implements WebService{

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		final String requestString = request.getReader().readLine();
		final JSONObject requestjsJsonObject = new JSONObject(requestString);
		final String data = requestjsJsonObject.getString("data");
		final String[] boxBarcodes = data.split(",");
		
		OBContext.getOBContext().setAdminMode();
		boolean flag = false;
		Organization warehouse = null;
		OBCriteria<Organization> criteria = OBDal.getInstance().createCriteria(Organization.class);
		criteria.add(Restrictions.eq(Organization.PROPERTY_NAME, "DSI Warehouse"));
		if(criteria.count()>0){
			warehouse = criteria.list().get(0);
		}
		for (String barcode : boxBarcodes) {
			
			try{
				
				final AttributeSetInstance attributeSetInstance = OBProvider.getInstance().get(AttributeSetInstance.class);
				attributeSetInstance.setClient(OBContext.getOBContext().getCurrentClient());
				attributeSetInstance.setOrganization(warehouse);
				attributeSetInstance.setCreatedBy(OBContext.getOBContext().getUser());
				attributeSetInstance.setUpdatedBy(OBContext.getOBContext().getUser());
				attributeSetInstance.setAttributeSet(OBDal.getInstance().get(AttributeSet.class, "BD85293DD63E4AAA9E29FE8C2A4206F8"));
				attributeSetInstance.setExpirationDate(new Timestamp(new Date().getTime()));
				attributeSetInstance.setLotName(barcode.trim());
				attributeSetInstance.setDescription("L"+barcode.trim());
				attributeSetInstance.setLocked(false);
				
				OBDal.getInstance().save(attributeSetInstance);
				OBDal.getInstance().flush();
				flag = true;
			} catch(Exception e){
				flag = false;
				e.printStackTrace();
			}
		
		}
		OBDal.getInstance().commitAndClose();
		OBContext.getOBContext().restorePreviousMode();
		
		String responseString = null;
		if(flag){
			responseString = "{\"status\":\"200\"}";
		} else {
			responseString = "{\"status\":\"204\"}";
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
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

}
