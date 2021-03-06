package in.decathlon.ibud.commons;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;

public class JSONHelper {
  public static final Logger log = Logger.getLogger(JSONHelper.class);

  @SuppressWarnings("unchecked")
  public static boolean saveJSONObject(JSONArray jsonArrayContent, ProcessLogger logger)
      throws Exception {
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    boolean isSaved = true;
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
    Warehouse dwarehouse = null;
    Role dRole = null;
    BusinessPartner existingPosInvoiceBPartner = null;

    Organization dOrg = null;
    try {
      OBContext.setAdminMode(true);
      // OBDal.getInstance().getSession().beginTransaction();

      Date date = new Date();
      String dateFormat = formater.format(date);
      date = formater.parse(dateFormat);

      for (int i = 0; i < jsonArrayContent.length(); i++) {
        JSONObject entityJson = jsonArrayContent.getJSONObject(i);
        if (entityJson.has("client") && OBContext.getOBContext().getCurrentClient().getId() != null) {
          entityJson.remove("client");
          entityJson.put("client", OBContext.getOBContext().getCurrentClient().getId());
        }
        if (entityJson.has("client$_identifier")
            && OBContext.getOBContext().getCurrentClient().getName() != null) {
          entityJson.remove("client$_identifier");
          entityJson.put("client$_identifier", OBContext.getOBContext().getCurrentClient()
              .getName());
        }
        String id = entityJson.getString(JsonConstants.ID);
        String entityName = entityJson.getString(JsonConstants.ENTITYNAME);

        String query = "from " + entityName + " ent where  ent.id='" + id + "'";
        log.info("executing query " + query);

        Query qry = OBDal.getInstance().getSession().createQuery(query);
        List qryList = qry.list();
        if (qryList != null && qryList.size() > 0) {
          // object is present in store so update and save it
          logger.log("Updating the Record for " + entityName + " with id" + id + " \n");

          log.debug(entityName + " with this id exists");
          if (entityName.equals("Organization")) {
            // OBContext.setAdminMode(true);
            OBContext.getOBContext().addWritableOrganization(id);
            Organization existingOrg = OBDal.getInstance().get(Organization.class, id);
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
          if (entityName.equals("ADUser")) {
            User existingUser = OBDal.getInstance().get(User.class, id);

            // OBContext.setAdminMode(true);
            org = existingUser.getOrganization();
            dwarehouse = existingUser.getDefaultWarehouse();
            dOrg = existingUser.getDefaultOrganization();
            dRole = existingUser.getDefaultRole();

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

          if (entityName.equals("CL_Model")) {
            bob.setValue("prmiProcess", null);
            bob.setValue("prmiComponentlabel", null);
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
          if (entityName.equals("ADUser")) {
            // OBContext.setAdminMode(true);
            bob.setValue("organization", org);
            bob.setValue("defaultWarehouse", dwarehouse);
            bob.setValue("defaultOrganization", dOrg);
            bob.setValue("defaultRole", dRole);
            bob.setValue("businessPartner", null);
            bob.setValue("partnerAddress", null);

          }

          if (entityName.equals("BusinessPartner")) {
            bob.setValue("rCOxylane", null);
          }
          // OBContext.setAdminMode(true);
          OBDal.getInstance().save(bob);
          // OBDal.getInstance().flush();

          if (entityName.equals("FinancialMgmtTaxRate")) {
            deleteExtraTaxacct(id);
          }
          // OBContext.restorePreviousMode();
          isSaved = true;
          logger.log(" Updated the Record for " + entityName + " with id" + id + " \n");

        } else {
          // Object is not present in Store so create it
          logger.log("Inserting the Record for " + entityName + " with id" + id + " \n");

          log.info(entityName + " with id=" + id + " does not exist");
          log.debug(entityJson);

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
          if (entityName.equals("Organization")) {
            // if (entityName.equals("Organization")){
            // OBContext.setAdminMode(true);
            OBContext.getOBContext().addWritableOrganization((String) bob.getId());

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
          // bob.setValue("createdBy", OBContext.getOBContext().getUser());
          // bob.setValue("updatedBy", OBContext.getOBContext().getUser());

          // Deletion of old tree node in Org pull
          if (entityName.equals("ADTreeNode")) {
            // OBDal.getInstance().flush();
            // OBContext.setAdminMode(true);
            String treeNodeId = entityJson.getString("tree");
            String nodeId = entityJson.getString("node");
            deleteExistingTreeNode(treeNodeId, nodeId);

            // OBContext.restorePreviousMode();
          }
          // Delete document sequence created by trigger
          if (entityName.equals("ADSequence")) {
            String name = entityJson.getString("name");
            deleteExistingSequence(name);
            // OBDal.getInstance().flush();
          }
          if (entityName.equals("ADUser")) {
            // OBContext.setAdminMode(true);
            bob.setValue("defaultWarehouse", null);
            bob.setValue("defaultOrganization", null);
            bob.setValue("businessPartner", null);
            bob.setValue("partnerAddress", null);

          }
          if (entityName.equals("BusinessPartner")) {
            bob.setValue("rCOxylane", null);
          }
          OBDal.getInstance().save(bob);

          log.info(entityName + " with id=" + id + " saved/updated");
          // OBDal.getInstance().flush();

          if (entityName.equals("FinancialMgmtTaxRate")) {
            deleteExtraTaxacct(id);
          }

          isSaved = true;
          // OBContext.restorePreviousMode();
          logger.log("Inserted the Record for " + entityName + " with id" + id + " \n");

        }
      }
    } catch (Exception e) {
      log.error("Error while saving/updating - " + e);
      isSaved = false;
      logger.log("Error while Processing for Entity:[" + objectName + "]Identifier : [" + objectID
          + "] ," + e + " \n");
      throw new OBException("Entity:[" + objectName + "]Identifier : [" + objectID + "] ," + e);

    } finally {
      // OBContext.restorePreviousMode();
      try {
        // OBContext.setAdminMode(true);
        // OBDal.getInstance().commitAndClose();
        // OBDal.getInstance().flush();

      } catch (Exception e) {
        logger.log("Error while Saving the record for Entity:[" + objectName + "] Identifier : ["
            + objectID + "] ," + e);
        throw new Exception("Entity:[" + objectName + "] Identifier : [" + objectID + "] ," + e);
      } finally {
        OBContext.restorePreviousMode();
      }

    }
    return isSaved;

  }

  private static void deleteExistingSequence(String name) {
    String qry = "delete from ADSequence seq where seq.name='" + name + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    int rowUpdated = query.executeUpdate();
    log.info("Row Updated=" + rowUpdated);
  }

  private static void deleteExistingTreeNode(String treeNodeId, String nodeId) {
    Tree tree = OBDal.getInstance().get(Tree.class, treeNodeId);

    String qry = "id in (select id from ADTreeNode where tree.id='" + treeNodeId + "' and node='"
        + nodeId + "')";
    OBQuery<TreeNode> treeQuery = OBDal.getInstance().createQuery(TreeNode.class, qry);
    treeQuery.setFilterOnActive(false);
    treeQuery.setFilterOnReadableOrganization(false);
    List<TreeNode> treeList = treeQuery.list();
    log.info("adtreenodes size= " + treeList.size());
    if (treeList.size() > 0) {
      OBDal.getInstance().remove(treeList.get(0));
      log.debug("removed");
    }
    SessionHandler.getInstance().commitAndStart();
  }

  private static TreeNode getTreeNode(Organization organization) {
    OBCriteria<TreeNode> oldTreeNodeCrit = OBDal.getInstance().createCriteria(TreeNode.class);
    oldTreeNodeCrit.add(Restrictions.eq(TreeNode.PROPERTY_ORGANIZATION, organization));
    List<TreeNode> treeNodeList = oldTreeNodeCrit.list();
    if (treeNodeList != null && treeNodeList.size() > 0)
      return oldTreeNodeCrit.list().get(0);
    else
      return null;
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

  public static Object getContentAsJSON(String content) throws JSONException {
    Check.isNotNull(content, "Content must be set");
    final Object jsonRepresentation;
    if (content.trim().startsWith("[")) {
      jsonRepresentation = new JSONArray(content);
    } else {
      final JSONObject jsonObject = new JSONObject(content);
      jsonRepresentation = jsonObject.get(JsonConstants.DATA);
    }
    return jsonRepresentation;
  }

  public static JSONObject convetBobToJson(BaseOBObject bob) throws JSONException {
    DataToJsonConverter dataToJsonConverter = new DataToJsonConverter();

    JSONObject jsonObj = dataToJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
    jsonObj.put(JsonConstants.NEW_INDICATOR, true);
    jsonObj.put("updatedTime", new Date());
    jsonObj.put("_idGenerate", "generated");
    jsonObj.remove("updated");
    jsonObj.remove("recordTime");

    return jsonObj;

  }

  public static List<JSONObject> convertBobListToJsonList(List<? extends BaseOBObject> boblist)
      throws JSONException {
    DataToJsonConverter dataToJsonConverter = new DataToJsonConverter();

    List<JSONObject> jsonObjList = new ArrayList<JSONObject>();
    BaseOBObject bob;
    for (int i = 0; i < boblist.size(); i++) {
      bob = boblist.get(i);
      JSONObject jsonObj = dataToJsonConverter.toJsonObject(bob, DataResolvingMode.FULL);
      jsonObj.put(JsonConstants.NEW_INDICATOR, true);
      jsonObj.put("updatedTime", new Date());
      jsonObj.put("_idGenerate", "generated");
      jsonObj.remove("updated");
      jsonObj.remove("recordTime");
      jsonObjList.add(jsonObj);

    }

    return jsonObjList;
  }
}