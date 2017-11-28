package in.decathlon.journalentry;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.enterprise.Organization;

public class DefaultCostCenter extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    System.out.println("entered execute");
    // String ProductCc = "";
    // String productId = info.getStringParameter("inpmProductId", null);
    String orgCCId = "";
    String orgID = info.getStringParameter("inpadOrgId", null);

    Organization org = OBDal.getInstance().get(Organization.class, orgID);
    if (org.getOrganizationInformationList().get(0).getDjeCostcenter() != null) {
      orgCCId = org.getOrganizationInformationList().get(0).getDjeCostcenter().getId().toString();
      info.addResult("inpcCostcenterId", orgCCId);
    } else
      info.addResult("inpcCostcenterId", "");
    /*
     * String qry = "from Product where Product.id= '" + productId + "'"; query =
     * OBDal.getInstance().getSession().createQuery(qry); for (Object product : query.list()) {
     * final Object[] qryDataItem = (Object[]) product; ProductCc = (String) qryDataItem[96]; }
     * 
     * if (ProductCc.equalsIgnoreCase("")) {
     * 
     * } else { info.addResult("inpcCostcenterId", ProductCc); }
     */
  }
}