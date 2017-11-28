package in.decathlon.supply.oba.ad_process;

import in.decathlon.supply.oba.data.OBA_ModelProduct;
import in.decathlon.supply.oba.util.OBACatalogueUtility;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

public class OBAValidateImportBGProcessOld extends DalBaseProcess {
	
	private ProcessLogger LOGGER;
	final OBACatalogueUtility OBACatalogueUtility = new OBACatalogueUtility();

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		try {
		LOGGER = bundle.getLogger();
		// Validating
		LOGGER.logln(" Validate process starts!");
		final List parameters = new ArrayList();
        parameters.add(OBContext.getOBContext().getCurrentClient().getId());
        parameters.add(OBContext.getOBContext().getCurrentOrganization().getId());
        parameters.add(OBContext.getOBContext().getUser().getId());
        final String procedureName = "oba_validatecatalog";
        final String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null).toString();
        if(resp.equals("SUCCESS")) {
        	LOGGER.logln(" validation is done successfully!");
        } else if(resp.equals("FAILURE")) {
        	LOGGER.logln(" validation is failed for some records!");
        }
        LOGGER.logln(" Validate process finishes!");
        
        
        final List<String> validatedItemcodes = new ArrayList<String>();
		final OBCriteria<OBA_ModelProduct> obaModelProductValidated = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
		obaModelProductValidated.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_VALIDATED, true));
		
		if(obaModelProductValidated.count() > 0) {
			int count=0;
			for (OBA_ModelProduct oba_ModelProduct : obaModelProductValidated.list()) {
				validatedItemcodes.add(oba_ModelProduct.getItemCode());
				
				oba_ModelProduct.setValidated(false);
				
				OBDal.getInstance().save(oba_ModelProduct);
				count++;
				if ((count % 100) == 0) {
					OBDal.getInstance().flush();
				    OBDal.getInstance().getSession().clear();
				}
			}
			SessionHandler.getInstance().commitAndStart();
		}
		System.out.println("Total items "+validatedItemcodes.size());
		final List<String> limitedValidatedItems = new ArrayList<String>();
		int countLimited=0;
		for (String string : validatedItemcodes) {
			countLimited++;
			limitedValidatedItems.add(string);
			if((countLimited % 500) == 0) {
				final OBCriteria<OBA_ModelProduct> obaModelProduct = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
				obaModelProduct.add(Restrictions.in(OBA_ModelProduct.PROPERTY_ITEMCODE, limitedValidatedItems));
				if(obaModelProduct.count() > 0) {
					int count=0;
					for (OBA_ModelProduct OBA_ModelProduct : obaModelProduct.list()) {
						count++;
						OBA_ModelProduct.setValidated(true);
						
						OBDal.getInstance().save(OBA_ModelProduct);
						if((count % 100) == 0)
							OBDal.getInstance().flush();
					}
					SessionHandler.getInstance().commitAndStart();
					final String procedureNameImport = "oba_importcatalog";
					final String respImport = CallStoredProcedure.getInstance().call(procedureNameImport, parameters, null).toString();
					LOGGER.logln(respImport);
				}
				limitedValidatedItems.clear();
				LOGGER.log(" "+countLimited);
			}
		}
		if(limitedValidatedItems.size() > 0) {
			final OBCriteria<OBA_ModelProduct> obaModelProduct = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
			obaModelProduct.add(Restrictions.in(OBA_ModelProduct.PROPERTY_ITEMCODE, limitedValidatedItems));
			if(obaModelProduct.count() > 0) {
				int count=0;
				for (OBA_ModelProduct OBA_ModelProduct : obaModelProduct.list()) {
					count++;
					OBA_ModelProduct.setValidated(true);
					
					OBDal.getInstance().save(OBA_ModelProduct);
					if((count % 100) == 0)
						OBDal.getInstance().flush();
				}
				SessionHandler.getInstance().commitAndStart();
				try {
					final String procedureNameImport = "oba_importcatalog";
					final String respImport = CallStoredProcedure.getInstance().call(procedureNameImport, parameters, null).toString();
					LOGGER.logln(respImport);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		LOGGER.logln(" Import process finishes!");
	}

}
