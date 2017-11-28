package org.openbravo.localization.india.ingst.sales.ad_callouts;

import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

 import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
 import org.openbravo.localization.india.ingst.master.data.GstIdentifierMaster;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.geography.Location;

public class SE_Order_BPartnerLocationGSTIN { /*extends SE_Order_BPartnerLocation {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    super.execute(info);

    String salesTransaction = info.getStringParameter("inpissotrx", null);

    String BpartnerLocationId = info.getStringParameter("inpcBpartnerLocationId", null);
    log4j.info("BpartnerLocationId" + BpartnerLocationId);

    log4j.info("info" + info);

    String supplyLocationID = info.getStringParameter("inpemIngststSupplylocationId", null);
    Location locationObj = null;

    log4j.info("salesTransaction" + salesTransaction);
    if (salesTransaction.equalsIgnoreCase("N")) {
      String organizationId = info.getStringParameter("inpadOrgId", null);
      String legalEntity = getLegalEntity(organizationId);
      // we don't need to check POS for legal org(this for selected org)
      // if (legalEntity.equalsIgnoreCase("true")) {
      locationObj = getLocationBasedOnOrganization(organizationId);
      // }

    } else {
      locationObj = getLocationId(BpartnerLocationId);

    }

    if (!(supplyLocationID.equalsIgnoreCase("")) && locationObj != null) {

      Location updateLocationId = getupdateLocationObj(supplyLocationID, locationObj);
      info.addResult("inpemIngststSupplylocationId", updateLocationId.getId());
    } else if (locationObj != null) {

      Location newLocationObj = newLocationObj = getNewLocationObj(locationObj);
      info.addResult("inpemIngststSupplylocationId", newLocationObj.getId());

    }

    GstIdentifierMaster businessPartnerGSTINObj = getBusinessPartnerGSTIN(BpartnerLocationId);
    if (businessPartnerGSTINObj != null) {
      info.addResult("inpemIngststBpGstmasterId", businessPartnerGSTINObj.getId());

    }

  }

  private GstIdentifierMaster getBusinessPartnerGSTIN(String bpartnerLocationId) {
    org.openbravo.model.common.businesspartner.Location bPartnerLocationObj = OBDal.getInstance()
        .get(org.openbravo.model.common.businesspartner.Location.class, bpartnerLocationId);
    GstIdentifierMaster ingstMasterObj = null;
    if (bPartnerLocationObj.getIngstGstidentifrmaster() != null)
      ingstMasterObj = bPartnerLocationObj.getIngstGstidentifrmaster();

    return ingstMasterObj;
  }

  private Location getLocationBasedOnOrganization(String organizationId) {
    // TODO Auto-generated method stub
    Organization orgObj = OBDal.getInstance().get(Organization.class, organizationId);

    OBCriteria<OrganizationInformation> OrgInfCriteria = OBDal.getInstance().createCriteria(
        OrganizationInformation.class);
    OrgInfCriteria.add(Restrictions.eq(OrganizationInformation.PROPERTY_ID, orgObj.getId()));
    List<OrganizationInformation> OrgInfoList = OrgInfCriteria.list();
    Location locationId = null;
    if (OrgInfoList.size() == 0) {
      log4j.info("OrganizationInformation is not available with this OrganizationId"
          + organizationId);
    } else {
      OrganizationInformation organizationInfo = OrgInfoList.get(0);
      locationId = organizationInfo.getLocationAddress();

    }
    return locationId;

  }

  private String getLegalEntity(String organizationId) {
    // TODO Auto-generated method stub
    // OBDal.getInstance().get(null, organizationId)

    String hql = "select organizationType.legalEntity from Organization where id='"
        + organizationId + "'";

    Query q = OBDal.getInstance().getSession().createQuery(hql);
    List result = q.list();
    String legalEntity = result.toString();
    legalEntity = legalEntity.substring(1, legalEntity.length() - 1).replaceAll(",", "");
    return legalEntity;

  }

  private Location getupdateLocationObj(String supplyLocationID, Location pocLocation) {
    // TODO Auto-generated method stub
    Location locationObj = OBDal.getInstance().get(Location.class, supplyLocationID);
    try {
      locationObj.setUpdated(new Date());
      locationObj.setUpdatedBy(OBContext.getOBContext().getUser());
      locationObj.setOrganization(pocLocation.getOrganization());
      locationObj.setClient(pocLocation.getClient());
      locationObj.setAddressLine1((pocLocation.getAddressLine1() == null) ? "" : pocLocation
          .getAddressLine1());
      locationObj.setAddressLine2((pocLocation.getAddressLine2() == null) ? "" : pocLocation
          .getAddressLine2());
      locationObj.setCityName((pocLocation.getCityName() == null) ? "" : pocLocation.getCityName());
      locationObj.setCity(pocLocation.getCity());
      locationObj.setPostalCode((pocLocation.getPostalCode() == null) ? "" : pocLocation
          .getPostalCode());
      locationObj.setPostalAdd((pocLocation.getPostalAdd() == null) ? "" : pocLocation
          .getPostalAdd());
      locationObj.setCountry(pocLocation.getCountry());
      locationObj.setRegion(pocLocation.getRegion());
      locationObj.setCity(pocLocation.getCity());
      locationObj.setRegionName((pocLocation.getRegionName() == null) ? "" : pocLocation
          .getRegionName());
      OBDal.getInstance().getSession().update(locationObj);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.debug(e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }
    return locationObj;

  }

  private Location getNewLocationObj(Location pocLocation) {
    // TODO Auto-generated method stub

    Location locationobj = OBProvider.getInstance().get(Location.class);
    try {
      locationobj.setCreationDate(new Date());
      locationobj.setUpdated(new Date());
      locationobj.setCreatedBy(OBContext.getOBContext().getUser());
      locationobj.setUpdatedBy(OBContext.getOBContext().getUser());
      locationobj.setOrganization(pocLocation.getOrganization());
      locationobj.setClient(pocLocation.getClient());
      locationobj.setAddressLine1((pocLocation.getAddressLine1() == null) ? "" : pocLocation
          .getAddressLine1());
      locationobj.setAddressLine2((pocLocation.getAddressLine2() == null) ? "" : pocLocation
          .getAddressLine2());
      locationobj.setCityName((pocLocation.getCityName() == null) ? "" : pocLocation.getCityName());

      locationobj.setCity(pocLocation.getCity());
      locationobj.setPostalCode((pocLocation.getPostalCode() == null) ? "" : pocLocation
          .getPostalCode());
      locationobj.setPostalAdd((pocLocation.getPostalAdd() == null) ? "" : pocLocation
          .getPostalAdd());
      locationobj.setCountry(pocLocation.getCountry());
      locationobj.setRegion(pocLocation.getRegion());
      locationobj.setCity(pocLocation.getCity());
      locationobj.setRegionName((pocLocation.getRegionName() == null) ? "" : pocLocation
          .getRegionName());

      OBDal.getInstance().save(locationobj);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.debug(e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }
    return locationobj;

  }

  private Location getLocationId(String bpartnerLocationId) {
    // TODO Auto-generated method stub

    org.openbravo.model.common.businesspartner.Location bpartnerlocation = OBDal.getInstance().get(
        org.openbravo.model.common.businesspartner.Location.class, bpartnerLocationId);
    String locationid = bpartnerlocation.getLocationAddress().getId();

    Location pocLocation = OBDal.getInstance().get(Location.class, locationid);
    return pocLocation;
  }
*/
}
