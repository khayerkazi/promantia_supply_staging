package in.decathlon.ibud.replenishment.ad_callout;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SE_Order_BPartner;
import org.openbravo.model.common.enterprise.Organization;

public class IBDREP_Order_BPartner extends SE_Order_BPartner {

  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;
  Logger logger = Logger.getLogger(this.getClass());

  protected void execute(CalloutInfo info) throws ServletException {
    logger.debug("Callout - Setting warehouse by Business partner");
    super.execute(info);
    String windowId = info.getStringParameter("inpwindowId", null);
    String organization = info.getStringParameter("inpadOrgId", null);
    Organization org = OBDal.getInstance().get(Organization.class, organization);

    if (windowId.equals(SOConstants.StoreReqWindowId)) {
      info.addResult("inpmWarehouseId", BusinessEntityMapper.getOrgWarehouse(org.getId())
          .getWarehouse().getId());
    } else if (windowId.equals(SOConstants.ReturnToVendorWindowId)) {
      info.addResult("inpmWarehouseId", BusinessEntityMapper.getOrgWarehouse(org.getId())
          .getWarehouse().getId());
      info.addResult("inpmPricelistId", BusinessEntityMapper.getPriceList(SOConstants.SOPriceList)
          .getId());
    }

  }

}
