package in.decathlon.ibud.picklistext.extendProperties;

import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;

import org.jfree.util.Log;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import com.sysfore.catalog.CLDepartment; 
import com.sysfore.catalog.CLStoreDept;

import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.warehouse.pickinglist.OBWPL_Utils;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.hooks.PLGenerationCompletedHook;

public class PLGenerationCompletedHookImplementation implements PLGenerationCompletedHook {

	@Override
	public void exec(HashMap<String, PickingList> createdPLs) throws Exception {
	    BusinessPartner bp = null;
	    for (PickingList curPL : createdPLs.values()) {
	      BusinessPartner curBp = null;
	      
	      final OBQuery<InternalMovementLine> firstMvtLineQuery = OBDal.getInstance().createQuery(
	          InternalMovementLine.class, "oBWPLWarehousePickingList.id = :plid");
	      firstMvtLineQuery.setNamedParameter("plid", curPL.getId());
	      firstMvtLineQuery.setMaxResult(1);
	      InternalMovementLine firstMvtLine = firstMvtLineQuery.list().get(0);
	      curBp = firstMvtLine.getStockReservation().getSalesOrderLine().getSalesOrder()
	          .getBusinessPartner();
	      if (bp == null) {
	        bp = curBp;
	      } else {
	        if (!curBp.getId().equals(bp)) {
	          Log.warn("Picking list have been generated using orders whih come from different BPs");
	        }
	      }
  
	      curPL.setObmwhpMobileidentifier(curPL.getIdentifier() + "-Q" 
	          + OBWPL_Utils.getNumberOfItemsInPickingList(curPL).toString() + "-L"
	          + OBWPL_Utils.getNumberOfRequiredBins(curPL));
	      OBDal.getInstance().save(curPL);
	    }
		
	}

}
