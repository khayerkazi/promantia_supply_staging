package in.decathlon.supply.oba.ad_process;

import in.decathlon.supply.oba.data.OBA_ModelProduct;
import in.decathlon.supply.oba.util.OBACatalogueUtility;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class OBAValidateImportBGProcess extends DalBaseProcess {
	
	private ProcessLogger LOGGER;
	final OBACatalogueUtility OBACatalogueUtility = new OBACatalogueUtility();

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		
		try {
		LOGGER = bundle.getLogger();

		// Validating
		LOGGER.logln(" Validate process starts!");
        
        // Validation Java Process
        final OBCriteria<OBA_ModelProduct> obaModelProductsToValidate = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
        obaModelProductsToValidate.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_VALIDATED, false));
        obaModelProductsToValidate.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_ISIMPORTED, false));
        obaModelProductsToValidate.setFilterOnActive(false);
        obaModelProductsToValidate.setFilterOnReadableOrganization(false);
        OBACatalogueUtility.doValidate(obaModelProductsToValidate.list());
        LOGGER.logln(" Validate process finishes!");
        
		// Importing
		// Importing Model
        LOGGER.logln(" Importing Model starts!");
		final OBQuery<OBA_ModelProduct> distModelCodesObQuery = OBDal.getInstance().createQuery(OBA_ModelProduct.class, "as oba WHERE oba.validated=true and oba.isimported=false");
		distModelCodesObQuery.setSelectClause("distinct oba.modelCode");
		if(distModelCodesObQuery.count() > 0) {
			final List<String> distModelCodes = new ArrayList<String>();
			for (Object modelCodeObj : distModelCodesObQuery.list()) {
				distModelCodes.add((String)modelCodeObj);
			}
			OBACatalogueUtility.importModel(distModelCodes);
			LOGGER.logln("No of modelcodes imported - "+distModelCodes.size());
		}
		LOGGER.logln(" Importing Model finishes!");

		// Importing Item and Price
		LOGGER.logln(" Importing Item and Price starts!");
		final OBQuery<OBA_ModelProduct> itemCodesObQuery = OBDal.getInstance().createQuery(OBA_ModelProduct.class, "as oba WHERE oba.validated=true and oba.isimported=false");
		if(itemCodesObQuery.count() > 0) {
			OBACatalogueUtility.importItem(itemCodesObQuery.list());
			OBACatalogueUtility.importPrice(itemCodesObQuery.list());
			LOGGER.logln("No of itemcodes to be imported - "+itemCodesObQuery.count());
			int count=0;
			for (OBA_ModelProduct obaModelProduct : itemCodesObQuery.list()) {
				
				OBDal.getInstance().remove(obaModelProduct);
				if ((count % 100) == 0) {
					OBDal.getInstance().flush();
				    OBDal.getInstance().getSession().clear();
				}
			}
			OBDal.getInstance().flush();
		    OBDal.getInstance().getSession().clear();
		}
		OBDal.getInstance().commitAndClose();
		
		LOGGER.logln(" Import process finishes!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
