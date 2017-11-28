package in.decathlon.ibud.replenishment.ad_callout;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.enterprise.Organization;

public class IBDREPGetBPartnerAndWarehouse extends SimpleCallout {

  private static final long serialVersionUID = 1L;
  Logger log = Logger.getLogger(this.getClass());

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    log.debug("Callout - Setting warehouse by Business partner");

    String windowId = info.getStringParameter("inpwindowId", null);
    String organization = info.getStringParameter("inpadOrgId", null);
    Organization org = OBDal.getInstance().get(Organization.class, organization);

    if (windowId.equals(SOConstants.StoreReqWindowId)) {
      info.addResult("inpcDoctypetargetId", BusinessEntityMapper.getTrasactionDocumentType(org)
          .getId());
      info.addResult("inpcDoctypeId", BusinessEntityMapper.getTrasactionDocumentType(org).getId());
      info.addResult("inpmWarehouseId", BusinessEntityMapper.getOrgWarehouse(org.getId())
          .getWarehouse().getId());
      info.addResult("inpcBpartnerId", BusinessEntityMapper.getSupplyBPartner(org));

    } else if (windowId.equals(SOConstants.ReturnToVendorWindowId)) {
      info.addResult("inpcDoctypetargetId", BusinessEntityMapper.getDocType("POO", true).getId());
      info.addResult("inpcDoctypeId", BusinessEntityMapper.getDocType("POO", true).getId());
      info.addResult("inpmWarehouseId", BusinessEntityMapper.getOrgWarehouse(org.getId())
          .getWarehouse().getId());
      info.addResult("inpcBpartnerId", BusinessEntityMapper.getSupplyBPartner(org));
      info.addResult("inpmPricelistId", BusinessEntityMapper.getPriceList(SOConstants.SOPriceList)
          .getId());
    }
  }
}
