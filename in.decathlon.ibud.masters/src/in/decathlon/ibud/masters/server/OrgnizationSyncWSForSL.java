package in.decathlon.ibud.masters.server;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;

import com.ibm.icu.text.SimpleDateFormat;

public class OrgnizationSyncWSForSL implements WebService {

  public static final Logger log = Logger.getLogger(OrgnizationSyncWSForSL.class);

  public JSONArray responseShipMent = new JSONArray();
  public static final String shuttleBin = "Shuttel Bin";
  public static String logger = "";

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      log.info("entered doPOst of completeShipmentWS");
      JSONObject respObj = new JSONObject();
      String result = processServerResponse(request, response);
      JSONObject orders = new JSONObject(result);

      HashMap<String, String> poStatusMap = new HashMap<String, String>();
      boolean flag = false;
      boolean actschemaFlag = true;
      boolean organizationFlag = true;
      if (orders.has("FinancialMgmtAcctSchema")) {
        actschemaFlag = saveJSONObject(getJsonData(orders, "FinancialMgmtAcctSchema"),
            "FinancialMgmtAcctSchema");
      }
      boolean OrganizationTypeFlag = true;
      if (orders.has("OrganizationType")) {
        OrganizationTypeFlag = saveJSONObject(getJsonData(orders, "OrganizationType"),
            "OrganizationType");
      }
      if (orders.has("Organization")) {
        organizationFlag = saveJSONObject(getJsonData(orders, "Organization"), "Organization");
      }
      if (OrganizationTypeFlag && organizationFlag && actschemaFlag) {
        flag = true;
      }
      respObj.put("errorMessage", logger);
      respObj.put("status", flag);
      logger = "";
      log.debug("response to update po " + poStatusMap);

      response.setContentType("text/json");
      response.setCharacterEncoding("utf-8");
      final Writer w = response.getWriter();
      w.write(respObj.toString());
      w.close();
    } catch (Exception e) {
      e.printStackTrace();
      response.setHeader("Error", e.toString());

      log.error(e);
      throw e;
    }
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method not implemented");
  }

  public String processServerResponse(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String content = getContentFromRequest(request);
    return content;
  }

  private String getContentFromRequest(HttpServletRequest request) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, "UTF-8");
    String Orders = writer.toString();
    return Orders;
  }

  private static void deleteExistingSequence(String name) {
    String qry = "delete from ADSequence seq where seq.name='" + name + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    int rowUpdated = query.executeUpdate();
    log.info("Row Updated=" + rowUpdated);
  }

  private static void deleteExtraTaxacct(String taxRateId) {
    String taxAcctId = getTaxAcctRecord();
    String qry = "delete from FinancialMgmtTaxRateAccounts where tax.id='" + taxRateId
        + "' and accountingSchema.id='" + taxAcctId + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    int rowUpdated = query.executeUpdate();
    log.info("Row Updated=" + rowUpdated);

  }

  private static String getTaxAcctRecord() {
    OBCriteria<OrganizationAcctSchema> orgActSchemaCrit = OBDal.getInstance().createCriteria(
        OrganizationAcctSchema.class);
    orgActSchemaCrit.add(Restrictions.eq(OrganizationAcctSchema.PROPERTY_CLIENT, OBContext
        .getOBContext().getCurrentClient()));
    orgActSchemaCrit.setMaxResults(1);
    List<OrganizationAcctSchema> orgAcctList = orgActSchemaCrit.list();
    if (orgAcctList.size() > 0)
      return orgActSchemaCrit.list().get(0).getAccountingSchema().getId();
    else
      throw new OBException("There is no record in AD_org_AcctSchema with current client");
  }

  private JSONArray getJsonData(JSONObject orders, String delimeter) throws JSONException {
    try {
      JSONArray arr = new JSONArray();
      arr = orders.getJSONArray(delimeter);
      return arr;
    } catch (Exception e) {
      log.error("Error", e);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static boolean saveJSONObject(JSONArray jsonArrayContent, String ProcessentityName)
      throws Exception {
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    JsonToDataConverter fromJsonConverter = new JsonToDataConverter();
    String objectName = "", objectID = "";
    boolean existingOrgActive = false;
    Long repoPriority = new Long(0);
    Long poStatusPriority = new Long(0);
    boolean dSIDEFIslbtapply = false;
    String dsidefPosdoctype = null;
    String dsidefPostxdoc = null;
    String dsidefPosinvaddr = null;
    String dsidefPospartneraddr = null;
    String dsidefPospayterms = null;
    String dsidefPospricelist = null;
    String dsidefPospaymethod = null;
    String dsidefPoswarehouse = null;
    String dsidefShiptime = null;
    boolean dsidefIsautodc = false;
    String dsidefPowarehouse = null;
    String dsidefStoretimedesc = null;
    String dsidefStorephonedesc = null;
    String dsidefStoremanagermail = null;
    Boolean existingIsReady = false;
    Currency existingCurrency = null;

    Organization org = null;
    BusinessPartner existingPosInvoiceBPartner = null;

    Organization dOrg = null;
    boolean isSaved = true;
    try {
      OBContext.setAdminMode(true);
      // OBDal.getInstance().getSession().beginTransaction();

      Date date = new Date();
      String dateFormat = formater.format(date);
      date = formater.parse(dateFormat);
      int insertCount = 0;
      int updateCount = 0;
      for (int i = 0; i < jsonArrayContent.length(); i++) {
        try {
          JSONObject entityJson = jsonArrayContent.getJSONObject(i);
          if (entityJson.has("client")
              && OBContext.getOBContext().getCurrentClient().getId() != null) {
            entityJson.put("client", OBContext.getOBContext().getCurrentClient().getId());
          }
          if (entityJson.has("client$_identifier")
              && OBContext.getOBContext().getCurrentClient().getName() != null) {
            entityJson.put("client$_identifier", OBContext.getOBContext().getCurrentClient()
                .getName());
          }
          if (entityJson.has("createdby")
              && OBContext.getOBContext().getCurrentClient().getId() != null) {
            entityJson.put("createdby", getUserId(entityJson.getString("createdby")));
          }
          if (entityJson.has("updatedby")) {
            entityJson.put("updatedby", getUserId(entityJson.getString("updatedby")));
          }
          String id = entityJson.getString(JsonConstants.ID);
          String entityName = entityJson.getString(JsonConstants.ENTITYNAME);

          String query = "from " + entityName + " ent where  ent.id='" + id + "'";
          log.info("executing query " + query);

          Query qry = OBDal.getInstance().getSession().createQuery(query);
          List qryList = qry.list();

          if (qryList != null && qryList.size() > 0) {
            // object is present in store so update and save it

            log.debug(entityName + " with this id exists");

            if (entityName.equals("FinancialMgmtAcctSchema")) {
              OBCriteria<AcctSchema> Crit = OBDal.getInstance().createCriteria(AcctSchema.class);

              AcctSchema existingUser = OBDal.getInstance().get(AcctSchema.class, id);
              if (existingUser.getClient().getId() != OBContext.getOBContext().getCurrentClient()
                  .getId()) {
                Crit.add(Restrictions.eq(AcctSchema.PROPERTY_CLIENT, existingUser.getClient()));
                entityJson.put("client", existingUser.getClient().getId());
                entityJson.put("client$_identifier", existingUser.getClient().getName());

              } else {
                Crit.add(Restrictions.eq(AcctSchema.PROPERTY_CLIENT, OBContext.getOBContext()
                    .getCurrentClient()));
              }
              Crit.add(Restrictions.eq(AcctSchema.PROPERTY_NAME, entityJson.getString("name")));

              List<AcctSchema> list = Crit.list();
              if (list != null && list.size() > 0) {
                AcctSchema Obj = list.get(0);
                if (!Obj.getId().equals(id)) {
                  logger = logger + "Acct Schema Name already present in Supply DB with id:"
                      + Obj.getId() + "  and name is: " + entityJson.getString("name")
                      + ", So Skip the updation action on record \n";
                  continue;
                }
              }

            }
            if (entityName.equals("Organization")) {
              // OBContext.setAdminMode(true);
              OBContext.getOBContext().addWritableOrganization(id);
              Organization existingOrg = OBDal.getInstance().get(Organization.class, id);

              OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(
                  Organization.class);

              Organization existingUser = OBDal.getInstance().get(Organization.class, id);
              if (existingUser.getClient().getId() != OBContext.getOBContext().getCurrentClient()
                  .getId()) {
                orgCrit
                    .add(Restrictions.eq(Organization.PROPERTY_CLIENT, existingUser.getClient()));
                entityJson.put("client", existingUser.getClient().getId());
                entityJson.put("client$_identifier", existingUser.getClient().getName());

              } else {
                orgCrit.add(Restrictions.eq(Organization.PROPERTY_CLIENT, OBContext.getOBContext()
                    .getCurrentClient()));
              }

              orgCrit.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY,
                  entityJson.getString("searchKey")));

              List<Organization> orgList = orgCrit.list();
              if (orgList != null && orgList.size() > 0) {
                Organization orgObj = orgList.get(0);
                if (!orgObj.getId().equals(id)) {
                  logger = logger + "Organization Search Key already present with id:"
                      + orgObj.getId() + " in Supply DB with Search Key is: "
                      + entityJson.getString("searchKey")
                      + " , So Skip the updation action on record \n";
                  continue;
                }
              }

              existingOrgActive = existingOrg.isActive();
              repoPriority = existingOrg.getIbdrepOrgreppriority();
              poStatusPriority = existingOrg.getIbudsPostatusPriority();
              existingIsReady = existingOrg.isReady();
              existingCurrency = existingOrg.getCurrency();

              dSIDEFIslbtapply = existingOrg.isDSIDEFIslbtapply();
              dsidefPosdoctype = existingOrg.getDsidefPosdoctype();
              dsidefPostxdoc = existingOrg.getDsidefPostxdoc();
              dsidefPosinvaddr = existingOrg.getDsidefPosinvaddr();
              dsidefPospartneraddr = existingOrg.getDsidefPospartneraddr();
              dsidefPospayterms = existingOrg.getDsidefPospayterms();
              dsidefPospricelist = existingOrg.getDsidefPospricelist();
              dsidefPospaymethod = existingOrg.getDsidefPospaymethod();
              dsidefPoswarehouse = existingOrg.getDsidefPoswarehouse();
              dsidefShiptime = existingOrg.getDsidefShiptime();
              dsidefIsautodc = existingOrg.isDsidefIsautodc();
              dsidefPowarehouse = existingOrg.getDsidefPowarehouse();
              dsidefStoretimedesc = existingOrg.getDsidefStoretimedesc();
              dsidefStorephonedesc = existingOrg.getDsidefStorephonedesc();
              dsidefStoremanagermail = existingOrg.getDsidefStoremanagermail();

              // OBContext.restorePreviousMode();
            }
            if (entityName.equals("OrganizationInformation")) {
              // OBContext.setAdminMode(true);
              if (id != null) {
                OBContext.getOBContext().addWritableOrganization(id);
                OrganizationInformation existingOrginfo = OBDal.getInstance().get(
                    OrganizationInformation.class, id);
                existingPosInvoiceBPartner = existingOrginfo.getDsidefPosinvoicebp();
              }
              // OBContext.restorePreviousMode();

            }

            entityJson.put(JsonConstants.NEW_INDICATOR, false);
            BaseOBObject bob = fromJsonConverter.toBaseOBObject(entityJson);
            objectName = bob.getEntityName();
            objectID = bob.getIdentifier();
            bob.setValue("updated", date);
            // bob.setValue("creationDate", date);
            if (entityName.equals("Product")) {
              bob.setValue("storageBin", null);
            }
            if (entityName.equals("Organization")) {
              // OBContext.setAdminMode(true);
              OBContext.getOBContext().addWritableOrganization((String) bob.getId());
              bob.setValue("active", existingOrgActive);
              bob.setValue("ibdrepOrgreppriority", repoPriority);
              bob.setValue("ibudsPostatusPriority", poStatusPriority);
              bob.setValue("ready", existingIsReady);
              bob.setValue("currency", existingCurrency);

              bob.setValue("dSIDEFIslbtapply", dSIDEFIslbtapply);
              bob.setValue("dsidefPosdoctype", dsidefPosdoctype);
              bob.setValue("dsidefPostxdoc", dsidefPostxdoc);
              bob.setValue("dsidefPosinvaddr", dsidefPosinvaddr);
              bob.setValue("dsidefPospartneraddr", dsidefPospartneraddr);
              bob.setValue("dsidefPospayterms", dsidefPospayterms);
              bob.setValue("dsidefPospricelist", dsidefPospricelist);
              bob.setValue("dsidefPospaymethod", dsidefPospaymethod);
              bob.setValue("dsidefPoswarehouse", dsidefPoswarehouse);
              bob.setValue("dsidefShiptime", dsidefShiptime);
              bob.setValue("dsidefIsautodc", dsidefIsautodc);
              bob.setValue("dsidefPowarehouse", dsidefPowarehouse);
              bob.setValue("dsidefStoretimedesc", dsidefStoretimedesc);
              bob.setValue("dsidefStorephonedesc", dsidefStorephonedesc);
              bob.setValue("dsidefStoremanagermail", dsidefStoremanagermail);

              // OBContext.restorePreviousMode();
            }
            if (entityName.equals("OrganizationInformation")) {
              // OBContext.setAdminMode(true);
              OBContext.getOBContext().addWritableOrganization((String) bob.getId());
              bob.setValue("yourCompanyDocumentImage", null);
              bob.setValue("dsidefPosinvoicebp", existingPosInvoiceBPartner);
              // OBContext.restorePreviousMode();
            }

            if (entityName.equals("BusinessPartner")) {
              bob.setValue("rCOxylane", null);
            }
            // OBContext.setAdminMode(true);
            OBDal.getInstance().save(bob);
            OBDal.getInstance().flush();

            if (entityName.equals("FinancialMgmtTaxRate")) {
              deleteExtraTaxacct(id);
            }
            updateCount++;
          } else {
            // Object is not present in Store so create it

            log.info(entityName + " with id=" + id + " does not exist, so inserting in DB");

            BaseOBObject bob = fromJsonConverter.toBaseOBObject(entityJson);
            objectName = bob.getEntityName();
            objectID = bob.getIdentifier();

            if (entityName.equals("Product")) {
              bob.setValue("storageBin", null);
            }
            if (entityName.equals("CL_Model")) {
              bob.setValue("prmiProcess", null);
              bob.setValue("prmiComponentlabel", null);
            }

            bob.setValue("id", id);
            if (entityName.equals("FinancialMgmtAcctSchema")) {

              OBCriteria<AcctSchema> Crit = OBDal.getInstance().createCriteria(AcctSchema.class);
              Crit.add(Restrictions.eq(AcctSchema.PROPERTY_CLIENT, OBContext.getOBContext()
                  .getCurrentClient()));
              Crit.add(Restrictions.eq(AcctSchema.PROPERTY_NAME, entityJson.getString("name")));

              List<AcctSchema> list = Crit.list();
              if (list != null && list.size() > 0) {
                AcctSchema Obj = list.get(0);

                logger = logger + "Acct Schema Name already present in Supply DB with id:"
                    + Obj.getId() + "  and name is: " + entityJson.getString("name")
                    + " , So Skip the Insert action on record \n";
                continue;

              }

            }
            if (entityName.equals("Organization")) {

              OBContext.getOBContext().addWritableOrganization((String) bob.getId());

              OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(
                  Organization.class);
              orgCrit.add(Restrictions.eq(Organization.PROPERTY_CLIENT, OBContext.getOBContext()
                  .getCurrentClient()));
              orgCrit.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY,
                  entityJson.getString("searchKey")));

              List<Organization> orgList = orgCrit.list();
              if (orgList != null && orgList.size() > 0) {
                Organization orgObj = orgList.get(0);

                logger = logger + "Organization Search Key already present with id:"
                    + orgObj.getId() + " in Supply DB with Search Key is: "
                    + entityJson.getString("searchKey")
                    + " , So Skip the Insert action on record  \n";
                continue;

              }
              bob.setValue("dSIDEFIslbtapply", dSIDEFIslbtapply);
              bob.setValue("dsidefPosdoctype", dsidefPosdoctype);
              bob.setValue("dsidefPostxdoc", dsidefPostxdoc);
              bob.setValue("dsidefPospartneraddr", dsidefPospartneraddr);
              bob.setValue("dsidefPospayterms", dsidefPospayterms);
              bob.setValue("dsidefPospricelist", dsidefPospricelist);
              bob.setValue("dsidefPospaymethod", dsidefPospaymethod);
              bob.setValue("dsidefPoswarehouse", dsidefPoswarehouse);
              bob.setValue("dsidefShiptime", dsidefShiptime);
              bob.setValue("dsidefIsautodc", dsidefIsautodc);
              bob.setValue("dsidefPowarehouse", dsidefPowarehouse);
              bob.setValue("dsidefStoretimedesc", dsidefStoretimedesc);
              bob.setValue("dsidefStorephonedesc", dsidefStorephonedesc);
              bob.setValue("dsidefStoremanagermail", dsidefStoremanagermail);
            }
            if (entityName.equals("OrganizationInformation")) {
              bob.setValue("yourCompanyDocumentImage", null);
              bob.setValue("dsidefPosinvoicebp", null);
            }
            bob.setValue("updated", date);
            bob.setValue("creationDate", date);
            bob.setValue("createdBy", OBContext.getOBContext().getUser());
            bob.setValue("updatedBy", OBContext.getOBContext().getUser());

            // Deletion of old tree node in Org pull

            if (entityName.equals("ADSequence")) {
              String name = entityJson.getString("name");
              deleteExistingSequence(name);
            }

            if (entityName.equals("BusinessPartner")) {
              bob.setValue("rCOxylane", null);
            }
            OBDal.getInstance().save(bob);
            OBDal.getInstance().flush();

            log.info(entityName + " with id=" + id + " saved/updated");

            if (entityName.equals("FinancialMgmtTaxRate")) {
              deleteExtraTaxacct(id);
            }

            insertCount++;
          }
        } catch (Exception e) {
          isSaved = false;
          logger = logger + " Error while Processing for Entity:[" + objectName + "]Identifier : ["
              + objectID + "] ," + e + " \n";
        }

      }

      logger = logger + "Record " + ProcessentityName + " and Inserted the Record cound is: "
          + insertCount + " and update record count id: " + updateCount + " \n";

    } catch (Exception e) {
      isSaved = false;

    } finally {
      OBContext.restorePreviousMode();

    }
    return isSaved;

  }

  public static String getUserId(String id) throws Exception {
    String userid = "100";
    if (!id.equals(userid)) {
      User existingUser = OBDal.getInstance().get(User.class, id);
      if (existingUser != null) {
        userid = existingUser.getId();
      } else {
        logger = logger + "User is not Present in DB with id: " + id + " \n";
      }
    }
    return userid;

  }
}