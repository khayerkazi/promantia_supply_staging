package in.decathlon.journalentry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.enterprise.Organization;

public class StoreClose  extends BaseProcessActionHandler {
	private static Logger log = Logger.getLogger(StoreClose.class);
	List<Organization> orgList = new ArrayList<Organization>();
	Organization org = null;
	String closeDateAsString;
	@Override
	protected JSONObject doExecute(Map<String, Object> parameters, String content) {
		JSONObject jsonRequest = new JSONObject();
	
		OBError result = new OBError();
		String msg = "";

		OBContext.setAdminMode();
		try {
			result.setType("Info");
			jsonRequest = new JSONObject(content);
			log.debug(jsonRequest);
			JSONArray selectedLines = jsonRequest.getJSONArray("_selection");
            JSONObject params = jsonRequest.getJSONObject("_params");
            closeDateAsString = params.getString("ClosedDate");

			selectedLines = jsonRequest.getJSONArray("_selection");

			if (selectedLines.length() == 0) {
				throw new OBException("Please select records ");
			}
			for (long i = 0; i < selectedLines.length(); i++) {
				JSONObject selectedLine = selectedLines.getJSONObject((int) i);
				final String freeIssueId = selectedLine.getString("id");
				org = OBDal.getInstance().get(Organization.class, freeIssueId);
				orgList.add(org);
			}
			msg = storeClose();
			result.setType("Success");
			result.setMessage(msg);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
			result.setType("Error");
			result.setMessage(e.getMessage());
			OBDal.getInstance().rollbackAndClose();
		} finally {
			OBContext.restorePreviousMode();
		}
		return jsonRequest;
	}
	private String storeClose() throws Exception {
	    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	    Date colseDate = format.parse(closeDateAsString);
		
		
	
		for (Organization org : orgList) {
			if(!checkIsOrgExists(org, colseDate)){
				StoreStatus storeStatus = OBProvider.getInstance().get(StoreStatus.class);
				storeStatus.setStoreStatus("Y");
				storeStatus.setOrganization(org);
				storeStatus.setClosedDate(colseDate);
				OBDal.getInstance().save(storeStatus);
			}
		
		}
		return null;
	}
	private  boolean checkIsOrgExists(Organization tempOrg,Date closedate) throws Exception{
		OBCriteria<StoreStatus> statusCriteria = OBDal.getInstance().createCriteria(StoreStatus.class);
		statusCriteria.add(Restrictions.eq(StoreStatus.PROPERTY_ORGANIZATION,tempOrg));
		statusCriteria.add(Restrictions.eq(StoreStatus.PROPERTY_CLOSEDDATE,closedate));
		List<StoreStatus> storeList = statusCriteria.list();
		if (storeList != null && storeList.size() > 0)
			return true;
		
		return false;
	}

}