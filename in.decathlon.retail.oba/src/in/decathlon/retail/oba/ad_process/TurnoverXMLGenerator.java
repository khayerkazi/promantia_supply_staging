package in.decathlon.retail.oba.ad_process;

import in.decathlon.retail.oba.reporting.ItemWisePriceBean;
import in.decathlon.retail.oba.util.RetailOBAUtility;

import java.util.List;
import java.util.Map;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class TurnoverXMLGenerator extends DalBaseProcess {

	private ProcessLogger LOGGER;
	
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		LOGGER = bundle.getLogger();
		LOGGER.logln("Starting background process for sending the CAN order XMLs! ");
		
		final RetailOBAUtility utility = new RetailOBAUtility();
		
		// getting all the configurations
		final Map<String, String> configDetails = utility.getOBAConfigurations();
		
		final List<String> uniqueOrgs = utility.fetchUniqueOrganizations(Integer.parseInt(configDetails.get("NnoOfDaysBack")));
		
		for (String orgId : uniqueOrgs) {
			
			final List<Invoice> invoicesOfYesterday = utility.fetchInvoicesOfYesterday(orgId, Integer.parseInt(configDetails.get("NnoOfDaysBack")));
			LOGGER.logln("Total no of invoices that are invoiced yesterday is : "+invoicesOfYesterday.size());
			
			final Map<String, ItemWisePriceBean> itemWiseInvoiceMap = utility.convertIntoItemwiseBean(invoicesOfYesterday);
			
			boolean status = utility.generatePurchaseOrderXMLInSAPServer(itemWiseInvoiceMap, configDetails, OBDal.getInstance().get(Organization.class, orgId));
			
			if(status) {
				LOGGER.logln("organization wise XMLs got placed in SAP server!");
			}
		}
		
		
		
		
		
		
	}

}
