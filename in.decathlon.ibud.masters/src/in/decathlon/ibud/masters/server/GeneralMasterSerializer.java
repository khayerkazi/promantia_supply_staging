package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.orders.client.SOConstants;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;

public class GeneralMasterSerializer {

  JSONTransmitter transmitter;

  GeneralMasterSerializer(HttpServletResponse response) throws IOException {
    transmitter = new JSONTransmitter(response);
  }

  public void generateJsonWS(Class<? extends BaseOBObject> bob, String updatedTime, int rowCount,
      String master, Organization org) throws Exception {

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }

    OBCriteria<? extends BaseOBObject> genClassCriteria = OBDal.getInstance().createCriteria(bob);
    if (!master.equals("DocSequence") && !master.equals("User")) {
      genClassCriteria.add(Restrictions.ge(Product.PROPERTY_UPDATED, newDate));
    } else {
      genClassCriteria.add(Restrictions.ge(Sequence.PROPERTY_CREATIONDATE, newDate));
    }

    /*
     * if (master.equals("Price")) {
     * genClassCriteria.add(Restrictions.eq(Product.PROPERTY_ORGANIZATION, org)); }
     */
    genClassCriteria.setFilterOnActive(false);
    genClassCriteria.setFilterOnReadableOrganization(false);
    genClassCriteria.setFetchSize(100);
    genClassCriteria.addOrderBy(Product.PROPERTY_UPDATED, true);
    ScrollableResults genMasterScrollar = genClassCriteria.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;

    List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
    while (genMasterScrollar.next()) {

      bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
      transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

      if (i % 100 == 0) {
        bobList.clear();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  public void getProductPriceData(String updatedTime) throws Exception {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    String query = "pp where pp.priceListVersion.name like '" + SOConstants.POPriceList
        + "' and pp.updated > '" + newDate + "' order by pp.updated asc ";
    OBQuery<ProductPrice> prPriceCrit = OBDal.getInstance().createQuery(ProductPrice.class, query);
    prPriceCrit.setFilterOnActive(false);
    prPriceCrit.setFilterOnReadableOrganization(false);
    ScrollableResults genMasterScrollar = prPriceCrit.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;

    List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
    while (genMasterScrollar.next()) {

      bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
      transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

      if (i % 100 == 0) {
        bobList.clear();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  @SuppressWarnings("static-access")
  public void getTreeNode(String updatedTime) throws Exception {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    try {

      OBContext.getOBContext().setAdminMode(true);
      String query = "tn where tn.tree.typeArea = '" + SOConstants.TreeType
          + "' and tn.updated > '" + newDate + "' order by tn.updated asc ";
      OBQuery<TreeNode> treeCrit = OBDal.getInstance().createQuery(TreeNode.class, query);
      treeCrit.setFilterOnReadableOrganization(false);
      treeCrit.setFilterOnActive(false);
      ScrollableResults genMasterScrollar = treeCrit.scroll(ScrollMode.FORWARD_ONLY);
      int i = 0;

      List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
      while (genMasterScrollar.next()) {

        bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
        transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

        if (i % 100 == 0) {
          bobList.clear();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.getOBContext().restorePreviousMode();
    }
  }

  public void getUserAccess(Class<? extends BaseOBObject> userAccess, String updatedTime)
      throws Exception {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    String query = "orgAccess where orgAccess.role.manual = '" + SOConstants.Manual
        + "' and orgAccess.updated > '" + newDate + "' order by orgAccess.updated asc ";
    OBQuery<? extends BaseOBObject> orgCrit = OBDal.getInstance().createQuery(userAccess, query);
    orgCrit.setFilterOnActive(false);
    orgCrit.setFilterOnReadableOrganization(false);
    ScrollableResults genMasterScrollar = orgCrit.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;

    List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
    while (genMasterScrollar.next()) {

      bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
      transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

      if (i % 100 == 0) {
        bobList.clear();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  @SuppressWarnings("static-access")
  public void getBusinessPartner(String updatedTime, String master) throws Exception {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    OBQuery<? extends BaseOBObject> bpCrit = null;
    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    try {
      OBContext.getOBContext().setAdminMode(true);
      if (master.equals("businessPartner")) {
        String query1 = "bp where bp.updated > '" + newDate + "' and bp.id in (select m." + master
            + ".id from OrganizationInformation " + " as m )  order by bp.updated asc";
        bpCrit = OBDal.getInstance().createQuery(BusinessPartner.class, query1);
      } else if (master.equals("bpLocation")) {
        String query2 = "bp where bp.updated > '"
            + newDate
            + "' and bp.businessPartner.id in (select m.businessPartner.id from OrganizationInformation "
            + " as m ) order by bp.updated asc";
        bpCrit = OBDal.getInstance().createQuery(Location.class, query2);
      }
      bpCrit.setFilterOnReadableOrganization(false);
      bpCrit.setFilterOnActive(false);
      ScrollableResults genMasterScrollar = bpCrit.scroll(ScrollMode.FORWARD_ONLY);
      int i = 0;

      List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
      while (genMasterScrollar.next()) {

        bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
        transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

        if (i % 100 == 0) {
          bobList.clear();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.getOBContext().restorePreviousMode();
    }
  }

  public void getOrgInfo(Class<? extends BaseOBObject> bob, String updatedDate) throws Exception {

    String updated = updatedDate;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    String query = " oi  where updated > '" + newDate + "' order by oi.updated";

    OBQuery<? extends BaseOBObject> orgInfo = OBDal.getInstance().createQuery(bob, query);
    orgInfo.setFilterOnReadableOrganization(false);
    orgInfo.setFilterOnActive(false);
    ScrollableResults genMasterScrollar = orgInfo.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;
    List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
    while (genMasterScrollar.next()) {

      bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
      transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

      if (i % 100 == 0) {
        bobList.clear();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  public void getLocation(String updatedTime) throws Exception {

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    OBQuery<? extends BaseOBObject> bpCrit = null;
    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    try {

      OBContext.getOBContext().setAdminMode(true);
      String query1 = "bp where bp.updated > '"
          + newDate
          + "' and (bp.id in (select m.locationAddress"
          + ".id from OrganizationInformation as m) or "
          + " bp.id in (select b.locationAddress.id from BusinessPartnerLocation as b where b.businessPartner.id in"
          + " (select oi.businessPartner.id from OrganizationInformation " + " as oi)))"
          + "  order by bp.updated asc";
      bpCrit = OBDal.getInstance().createQuery(org.openbravo.model.common.geography.Location.class,
          query1);

      bpCrit.setFilterOnReadableOrganization(false);
      bpCrit.setFilterOnActive(false);
      ScrollableResults genMasterScrollar = bpCrit.scroll(ScrollMode.FORWARD_ONLY);
      int i = 0;

      List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
      while (genMasterScrollar.next()) {

        bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
        transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

        if (i % 100 == 0) {
          bobList.clear();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.getOBContext().restorePreviousMode();
    }

  }

  public void getUserDataJson(Class<? extends BaseOBObject> userAccess, String updatedTime,
      String master, boolean isIndiaRequest, boolean isRoleManual) throws Exception {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    StringBuilder stBuilder = new StringBuilder();
    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = new Date(new Date().getTime() - 2 * 24 * 3600 * 1000);
    }
    if (master.equalsIgnoreCase("Role")) {
      stBuilder
          .append("orgAccess where orgAccess.id in (select distinct e.role.id from ADUserRoles e "
              + " where (e.userContact.ibudIsslandinduser='Y' or e.userContact.ibudIssluser="
              + isIndiaRequest + "))");

    } else if (master.equalsIgnoreCase("Greeting")) {
      stBuilder
          .append("orgAccess where orgAccess.id in (select distinct e.greeting from ADUser e where e.ibudIsslandinduser='Y' or e.ibudIssluser="
              + isIndiaRequest + ")");

    } else if (master.equalsIgnoreCase("User")) {
      stBuilder
          .append("orgAccess where (orgAccess.ibudIsslandinduser='Y' or orgAccess.ibudIssluser="
              + isIndiaRequest + ")");

    } else if (master.equalsIgnoreCase("TabAccess")) {
      stBuilder
          .append("orgAccess where orgAccess.windowAccess.id in (select e.id from ADWindowAccess e where  e.role.id in (select distinct r.role.id from ADUserRoles r "
              + " where (r.userContact.ibudIsslandinduser='Y' or r.userContact.ibudIssluser="
              + isIndiaRequest + ")))");
    } else if (master.equalsIgnoreCase("FieldAccess")) {
      stBuilder
          .append("orgAccess where orgAccess.tabAccess.id in (select t.id from ADTabAccess t where    t.windowAccess.id in (select e.id from ADWindowAccess e where  e.role.id in (select distinct r.role.id from ADUserRoles r "
              + " where (r.userContact.ibudIsslandinduser='Y' or r.userContact.ibudIssluser="
              + isIndiaRequest + "))))");
    } else {
      stBuilder
          .append("orgAccess where orgAccess.role.id in (select distinct e.role.id from ADUserRoles e "
              + " where (e.userContact.ibudIsslandinduser='Y' or e.userContact.ibudIssluser="
              + isIndiaRequest + "))");

    }
    if (master.equalsIgnoreCase("User") || master.equalsIgnoreCase("Greeting")) {
      stBuilder.append(" and orgAccess.creationDate > '" + newDate + "'");

    } else {
      stBuilder.append(" and orgAccess.updated > '" + newDate + "'");

    }
    if (isRoleManual) {
      stBuilder.append(" and orgAccess.role.manual = '" + SOConstants.Manual + "' ");
    }
    stBuilder.append(" order by orgAccess.updated asc");
    OBQuery<? extends BaseOBObject> orgCrit = OBDal.getInstance().createQuery(userAccess,
        stBuilder.toString());
    orgCrit.setFilterOnActive(false);
    orgCrit.setFilterOnReadableOrganization(false);
    ScrollableResults genMasterScrollar = orgCrit.scroll(ScrollMode.FORWARD_ONLY);
    int i = 0;

    List<BaseOBObject> bobList = new ArrayList<BaseOBObject>();
    while (genMasterScrollar.next()) {

      bobList.add((BaseOBObject) genMasterScrollar.get()[0]);
      transmitter.sendData((BaseOBObject) genMasterScrollar.get()[0]);

      if (i % 100 == 0) {
        bobList.clear();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

}