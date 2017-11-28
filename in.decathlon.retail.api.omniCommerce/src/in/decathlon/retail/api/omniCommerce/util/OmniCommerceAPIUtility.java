package in.decathlon.retail.api.omniCommerce.util;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

public class OmniCommerceAPIUtility {
	
	private static final Logger LOG = Logger.getLogger(OmniCommerceAPIUtility.class);

	public List<Organization> getRequestedStoresList(String requestedStores) {

		final List<Organization> returnList = new ArrayList<Organization>();
		final String[] storesArray = requestedStores.split(",");
		
		for (String string : storesArray) {
			final OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
			orgCriteria.add(Restrictions.eq(Organization.PROPERTY_NAME, string));
			if(orgCriteria.count() > 0) {
				returnList.add(orgCriteria.list().get(0));
			}
		}
		
		return returnList;
	}
	
	public List<Organization> getRequestedStoresListforInventory(String requestedStores) {

		final List<Organization> returnList = new ArrayList<Organization>();
		final String[] storesArray = requestedStores.split(",");
		
		for (String string : storesArray) {
			if(string.equals("WHStock")) {
				
			} else {
				final OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
				orgCriteria.add(Restrictions.eq(Organization.PROPERTY_NAME, string));
				if(orgCriteria.count() > 0) {
					returnList.add(orgCriteria.list().get(0));
				}
			}
		}
		
		return returnList;
	}
	
	// Fetches all the configuration details from dsidef_module_config table
	public Map<String, String> getConfigurations() {
		
		final Map<String, String> configDetails = new HashMap<String, String>();
		OBContext.setAdminMode();
		final OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
		configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.retail.api.omniCommerce"));
		if(configInfoObCriteria.count() > 0) {
			for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
				configDetails.put(config.getKey(), config.getSearchKey());
			}
		}
		OBContext.restorePreviousMode();
		return configDetails;
	}
	
}
