package org.openbravo.localization.india.ingst.sales.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SE_Invoice_Organization;
import org.openbravo.localization.india.ingst.master.data.GstIdentifierMaster;
import org.openbravo.model.common.enterprise.OrganizationInformation;

public class SE_Invoice_OrganizationGSTIN extends SE_Invoice_Organization {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    super.execute(info);

    String orgId = info.getStringParameter("inpadOrgId", null);
    log4j.info("BpartnerLocationId" + orgId);

    GstIdentifierMaster ingststOrgGstmasterObj = changeOrganization(orgId);
    if (ingststOrgGstmasterObj != null) {
      log4j.info("ingststOrgGstmasterObj: " + ingststOrgGstmasterObj.getId());

      info.addResult("inpemIngststOrgGstmasterId", ingststOrgGstmasterObj.getId());
    } else {
      info.addResult("inpemIngststOrgGstmasterId", "");

    }
  }

  private GstIdentifierMaster changeOrganization(String orgId) {
    // TODO Auto-generated method stub
    OrganizationInformation orgInfoObj = OBDal.getInstance().get(OrganizationInformation.class,
        orgId);
    GstIdentifierMaster ingstMasterObj = null;
    if (orgInfoObj.getIngstGstidentifirmaster() != null) {
      ingstMasterObj = orgInfoObj.getIngstGstidentifirmaster();
    }

    return ingstMasterObj;

  }

}
