package in.decathlon.ibud.picklistext.extendProperties;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.hooks.CreatePLHook;

public class PickListDocNoChange implements CreatePLHook {

  @Override
  public void exec(PickingList pickinglist, Order order) throws Exception {
    Order myLoadedOrder = OBDal.getInstance().get(Order.class, order.getId());
    BusinessPartner bp = myLoadedOrder.getBusinessPartner();
    Organization storeOrg = BusinessEntityMapper.getOrgOfBP(bp.getId());
    String org = storeOrg.getName();
    String picklistDocNo = pickinglist.getDocumentNo();
    String storeDept = ""; 
    if(null != myLoadedOrder.getClStoredept())
    	storeDept = myLoadedOrder.getClStoredept().getName();
    else 
    	storeDept = myLoadedOrder.getClBrand().getName();
    if (storeDept.length() > 3){
      storeDept = storeDept.substring(0, 3);
    }
    if(org.length() > 6){
        org = org.substring(0, 6);
    }      
    DocumentType docTypeObj = OBDal.getInstance().get(DocumentType.class, pickinglist.getDocumentType().getId());
    Sequence seq = docTypeObj.getDocumentSequence();
    String picklistPrefix = seq.getPrefix();
    String docNo = picklistDocNo.replace(picklistPrefix, "");
    String pickListDocNo = org + "-" + storeDept + "-" + docNo;
    pickinglist.setDocumentNo(pickListDocNo);
  }
}
