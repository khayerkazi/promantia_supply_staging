package in.decathlon.ibud.masters.server;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.localization.india.ingst.master.data.GstIdentifierMaster;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Location;
import org.openbravo.model.common.geography.Region;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.gl.GLCategory;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;

public class OrgnizationSyncWS implements WebService {

  public static final Logger log = Logger.getLogger(OrgnizationSyncWS.class);

  public JSONArray responseShipMent = new JSONArray();
  public static final String shuttleBin = "Shuttel Bin";
  public static String logger = "";
  static Set<String> userSet = new HashSet<String>();

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

      boolean flag = false;
      boolean actschemaFlag = true;
      boolean organizationFlag = true;
      boolean OrganizationTypeFlag = true;
      boolean locationFlag = true;
      boolean bpCategoryFlag = true;
      boolean priceListFlag = true;
      boolean paymenttermFlag = true;
      boolean bpFlag = true;
      boolean costcenterFlag = true;
      boolean bplocationFlag = true;
      boolean orginfoFlag = true;
      boolean doctypeFlag = true;
      boolean glCategoryFlag = true;
      boolean docSeqFlag = true;
      boolean countryFlag = true;
      boolean regionFlag = true;
      boolean gstinFlag = true;
      boolean treeNodeFlag = true;
      log.error("Push Organization(INDIA)JSON is" + orders);
      if (orders.has("FinancialMgmtAcctSchema")) {
        actschemaFlag = saveJSONObject(getJsonData(orders, "FinancialMgmtAcctSchema"),
            "FinancialMgmtAcctSchema");
      }
      if (orders.has("OrganizationType")) {
        OrganizationTypeFlag = saveJSONObject(getJsonData(orders, "OrganizationType"),
            "OrganizationType");
      }
      if (orders.has("Organization")) {
        organizationFlag = saveJSONObject(getJsonData(orders, "Organization"), "Organization");
      }

      if (orders.has("Country")) {
        countryFlag = saveJSONObject(getJsonData(orders, "Country"), "Country");
      }
      if (orders.has("Region")) {
        regionFlag = saveJSONObject(getJsonData(orders, "Region"), "Region");
      }
      if (orders.has("Location")) {
        locationFlag = saveJSONObject(getJsonData(orders, "Location"), "Location");
      }

      if (orders.has("BusinessPartnerCategory")) {
        bpCategoryFlag = saveJSONObject(getJsonData(orders, "BusinessPartnerCategory"),
            "BusinessPartnerCategory");
      }

      if (orders.has("PricingPriceList")) {
        priceListFlag = saveJSONObject(getJsonData(orders, "PricingPriceList"), "PricingPriceList");
      }

      if (orders.has("FinancialMgmtPaymentTerm")) {
        paymenttermFlag = saveJSONObject(getJsonData(orders, "FinancialMgmtPaymentTerm"),
            "FinancialMgmtPaymentTerm");
      }
      if (orders.has("businessPartner")) {
        bpFlag = saveJSONObject(getJsonData(orders, "businessPartner"), "businessPartner");
      }
      if (orders.has("BusinessPartnerLocation")) {
        bplocationFlag = saveJSONObject(getJsonData(orders, "BusinessPartnerLocation"),
            "BusinessPartnerLocation");
      }

      if (orders.has("Costcenter")) {
        costcenterFlag = saveJSONObject(getJsonData(orders, "Costcenter"), "Costcenter");
      }
      if (orders.has("GstIdentifierMaster")) {
        gstinFlag = saveJSONObject(getJsonData(orders, "GstIdentifierMaster"),
            "INGST_GSTIdentifierMaster");
      }

      if (orders.has("OrganizationInformation")) {
        orginfoFlag = saveJSONObject(getJsonData(orders, "OrganizationInformation"),
            "OrganizationInformation");
      }
      if (orders.has("ADSequence")) {
        docSeqFlag = saveJSONObject(getJsonData(orders, "ADSequence"), "ADSequence");
      }

      if (orders.has("FinancialMgmtGLCategory")) {
        glCategoryFlag = saveJSONObject(getJsonData(orders, "FinancialMgmtGLCategory"),
            "FinancialMgmtGLCategory");
      }

      if (orders.has("DocumentType")) {
        doctypeFlag = saveJSONObject(getJsonData(orders, "DocumentType"), "DocumentType");
      }
      if (orders.has("ADTreeNode")) {
        treeNodeFlag = saveJSONObject(getJsonData(orders, "ADTreeNode"), "ADTreeNode");
      }

      if (actschemaFlag && organizationFlag && countryFlag && regionFlag && OrganizationTypeFlag
          && locationFlag && bpCategoryFlag && priceListFlag && paymenttermFlag && bpFlag
          && costcenterFlag && bplocationFlag && orginfoFlag && doctypeFlag && glCategoryFlag
          && docSeqFlag && gstinFlag && treeNodeFlag) {
        flag = true;
      }

      if (userSet.size() > 0) {
        logger = logger
            + "WARNING: User is not Present in DB with id: "
            + userSet
            + ",and Updated as Openbravo User on Same Record, Please Use the Pull User Process to Pull the this User Data.  \n";
      }
      userSet.clear();
      log.error("ERROR: Logged Error for Pull organization Process: " + logger);

      respObj.put("errorMessage", logger);
      respObj.put("status", flag);
      logger = "";
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
    JSONArray arr = new JSONArray();
    try {
      if (orders.has(delimeter)) {
        arr = orders.getJSONArray(delimeter);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error", e);
    }
    return arr;
  }

  @SuppressWarnings("unchecked")
  public static boolean saveJSONObject(JSONArray jsonArrayContent, String ProcessentityName)
      throws Exception {
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    User obUser = OBDal.getInstance().get(User.class, "100");

    boolean isSaved = true;
    try {
      OBContext.setAdminMode(true);
      Client currentClient = OBContext.getOBContext().getCurrentClient();

      Date date = new Date();
      String dateFormat = formater.format(date);
      date = formater.parse(dateFormat);
      int insertCount = 0;
      int updateCount = 0;
      for (int i = 0; i < jsonArrayContent.length(); i++) {
        BusinessPartner existingPosInvoiceBPartner = null;
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
        String gstinUniqueNo = null;
        try {
          User createdByUser = null;
          User updatedByUser = null;
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

          String id = entityJson.getString(JsonConstants.ID);
          String entityName = entityJson.getString(JsonConstants.ENTITYNAME);

          String query = "from " + entityName + " ent where  ent.id='" + id + "'";
          log.info("executing query is: " + query);

          Query qry = OBDal.getInstance().getSession().createQuery(query);
          List qryList = qry.list();

          if (qryList != null && qryList.size() > 0) {
            // object is present in store so update and save it
            try {
              loop: {
                Client processedClientObj = null;

                log.debug(entityName + " with this id exists");

                if (entityName.equals("FinancialMgmtAcctSchema")) {
                  AcctSchema existingUser = OBDal.getInstance().get(AcctSchema.class, id);
                  processedClientObj = existingUser.getClient();

                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      AcctSchema.class, entityJson.getString("name"), "",
                      existingUser.getOrganization(), existingUser.getClient());
                  List<AcctSchema> list = (List<AcctSchema>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (AcctSchema Obj : list) {
                      if (!Obj.getId().equals(id)) {
                        logger = logger
                            + "Acct Schema Name is Already Present in Supply DB with Id:"
                            + Obj.getId() + "  and Name is: " + entityJson.getString("name")
                            + " and Organization is:" + existingUser.getOrganization().getName()
                            + " and Client is: " + existingUser.getClient().getName()
                            + ", So Skip the Updation Action on Supply DB \n";
                        break loop;
                      }
                    }
                  }

                } else if (entityName.equals("Organization")) {
                  // OBContext.setAdminMode(true);
                  OBContext.getOBContext().addWritableOrganization(id);
                  Organization existingOrg = OBDal.getInstance().get(Organization.class, id);
                  processedClientObj = existingOrg.getClient();

                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      Organization.class, "", entityJson.getString("searchKey"), null,
                      existingOrg.getClient());
                  List<Organization> list = (List<Organization>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (Organization Obj : list) {
                      if (!Obj.getId().equals(id)) {
                        logger = logger
                            + "WARNING: Organization Search Key is Already Present in Supply DB with id:"
                            + Obj.getId() + "  and searchKey is: "
                            + entityJson.getString("searchKey") + " and Organization Name is:"
                            + existingOrg.getName() + " and Client is: "
                            + existingOrg.getClient().getName()
                            + ", So Skip the Updation Action on Supply DB \n";
                        break loop;
                      }
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
                } else if (entityName.equals("OrganizationType")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    OrganizationType existingobj = OBDal.getInstance().get(OrganizationType.class,
                        id);
                    processedClientObj = existingobj.getClient();
                  }
                } else if (entityName.equals("OrganizationInformation")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    OrganizationInformation existingOrginfo = OBDal.getInstance().get(
                        OrganizationInformation.class, id);
                    processedClientObj = existingOrginfo.getClient();

                    existingPosInvoiceBPartner = existingOrginfo.getDsidefPosinvoicebp();
                  }
                } else if (entityName.equals("Location")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    Location existingobj = OBDal.getInstance().get(Location.class, id);
                    processedClientObj = existingobj.getClient();
                  }
                } else if (entityName.equals("BusinessPartnerCategory")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    Category existingobj = OBDal.getInstance().get(Category.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        Category.class, "", entityJson.getString("searchKey"), null,
                        existingobj.getClient());
                    List<Category> list = (List<Category>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (Category Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger
                              + "WARNING: Business Category Search Key is Already Present in Supply DB with id:"
                              + Obj.getId() + "  and searchKey is: "
                              + entityJson.getString("searchKey") + " and Organization is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }
                  }
                } else if (entityName.equals("PricingPriceList")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    PriceList existingobj = OBDal.getInstance().get(PriceList.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        PriceList.class, entityJson.getString("name"), "", null,
                        existingobj.getClient());

                    List<PriceList> list = (List<PriceList>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (PriceList Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger
                              + "WARNING: Price List Name is Already Present in Supply DB with id:"
                              + Obj.getId() + "  and Name is: " + entityJson.getString("name")
                              + " and Organization is:" + existingobj.getOrganization().getName()
                              + " and Client is: " + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }

                  }
                } else if (entityName.equals("FinancialMgmtPaymentTerm")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    PaymentTerm existingobj = OBDal.getInstance().get(PaymentTerm.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        PaymentTerm.class, "", entityJson.getString("searchKey"),
                        existingobj.getOrganization(), existingobj.getClient());
                    List<PaymentTerm> list = (List<PaymentTerm>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (PaymentTerm Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger + "WARNING: " + entityName
                              + " Search Key is Already Present in Supply DB with id:"
                              + Obj.getId() + "  and searchKey is: "
                              + entityJson.getString("searchKey") + " and Organization Name is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }

                  }
                } else if (entityName.equals("businessPartner")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    BusinessPartner existingobj = OBDal.getInstance()
                        .get(BusinessPartner.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        BusinessPartner.class, "", entityJson.getString("searchKey"),
                        existingobj.getOrganization(), existingobj.getClient());
                    List<BusinessPartner> list = (List<BusinessPartner>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (BusinessPartner Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger + "WARNING: " + entityName
                              + " Search Key is Already Present in Supply DB with id:"
                              + Obj.getId() + "  and searchKey is: "
                              + entityJson.getString("searchKey") + " and Organization Name is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }

                  }
                } else if (entityName.equals("Region")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    Region existingobj = OBDal.getInstance().get(Region.class, id);
                    processedClientObj = existingobj.getClient();

                    Country countryObj = OBDal.getInstance().get(Country.class,
                        entityJson.getString("country"));
                    OBCriteria<Region> genClassCriteria = OBDal.getInstance().createCriteria(
                        Region.class);
                    genClassCriteria.add(Restrictions.eq(Region.PROPERTY_NAME,
                        entityJson.getString("name")));
                    genClassCriteria.add(Restrictions.eq(Region.PROPERTY_COUNTRY, countryObj));
                    genClassCriteria.setFilterOnActive(false);
                    genClassCriteria.setFilterOnReadableOrganization(false);

                    List<Region> list = genClassCriteria.list();
                    if (list != null && list.size() > 0) {
                      for (Region Obj : list) {
                        if (!existingobj.getId().equals(Obj.getId()))
                          logger = logger
                              + "WARNING: Region is Already Present in Supply DB with id:"
                              + Obj.getId() + "  and Name is: " + entityJson.getString("name")
                              + " and Country Name is:" + countryObj.getName() + " and Client is: "
                              + Obj.getClient().getName()
                              + ", So Skip the Insert Action on Supply DB \n";
                        break loop;
                      }

                    }

                  }
                } else if (entityName.equals("Country")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    Country existingobj = OBDal.getInstance().get(Country.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<Country> genClassCriteria = OBDal.getInstance().createCriteria(
                        Country.class);
                    genClassCriteria.add(Restrictions.eq(Country.PROPERTY_ISOCOUNTRYCODE,
                        entityJson.getString("iSOCountryCode")));
                    genClassCriteria.setFilterOnActive(false);
                    genClassCriteria.setFilterOnReadableOrganization(false);

                    List<Country> list = genClassCriteria.list();
                    if (list != null && list.size() > 0) {
                      for (Country Obj : list) {
                        if (!existingobj.getId().equals(Obj.getId()))
                          logger = logger + "WARNING: ISO Country Code:"
                              + entityJson.getString("iSOCountryCode")
                              + " is Already Present in Supply DB with id:" + Obj.getId()
                              + "  and Name is: " + entityJson.getString("name")
                              + " and Country Name is:" + Obj.getName() + " and Client is: "
                              + Obj.getClient().getName()
                              + ", So Skip the Insert Action on Supply DB \n";
                        break loop;
                      }

                    }
                  }
                } else if (entityName.equals("DocumentType")) {

                  // OBContext.setAdminMode(true);
                  if (id != null) {

                    OBContext.getOBContext().addWritableOrganization(id);
                    DocumentType existingobj = OBDal.getInstance().get(DocumentType.class, id);
                    processedClientObj = existingobj.getClient();
                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        DocumentType.class, entityJson.getString("name"), "",
                        existingobj.getOrganization(), existingobj.getClient());
                    List<DocumentType> list = (List<DocumentType>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (DocumentType Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger + "WARNING: " + entityName
                              + " name is Already Present in Supply DB with id:" + Obj.getId()
                              + "  and Name is: " + entityJson.getString("name")
                              + " and Organization Name is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }
                  }
                } else if (entityName.equals("FinancialMgmtGLCategory")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    GLCategory existingobj = OBDal.getInstance().get(GLCategory.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        GLCategory.class, entityJson.getString("name"), "",
                        existingobj.getOrganization(), existingobj.getClient());
                    List<GLCategory> list = (List<GLCategory>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (GLCategory Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger + "WARNING: " + entityName
                              + " name is Already Present in Supply DB with id:" + Obj.getId()
                              + "  and Name is: " + entityJson.getString("name")
                              + " and Organization Name is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }

                  }
                } else if (entityName.equals("ADSequence")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    Sequence existingobj = OBDal.getInstance().get(Sequence.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        Sequence.class, entityJson.getString("name"), "",
                        existingobj.getOrganization(), existingobj.getClient());
                    List<Sequence> list = (List<Sequence>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (Sequence Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger + "WARNING: " + entityName
                              + " name is Already Present in Supply DB with id:" + Obj.getId()
                              + "  and Name is: " + entityJson.getString("name")
                              + " and Organization Name is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }

                  }
                } else if (entityName.equals("INGST_GSTIdentifierMaster")) {
                  GstIdentifierMaster existingobj = OBDal.getInstance().get(
                      GstIdentifierMaster.class, id);

                  OBCriteria<GstIdentifierMaster> crit = OBDal.getInstance().createCriteria(
                      GstIdentifierMaster.class);
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_CLIENT, OBContext
                      .getOBContext().getCurrentClient()));
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_UIDNO,
                      entityJson.getString("uidno")));
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_ORGANIZATION,
                      entityJson.getString("organization")));
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_BUSINESS,
                      entityJson.getString("businessPartner")));
                  crit.setFilterOnReadableClients(false);

                  List<GstIdentifierMaster> list = crit.list();

                  if (list != null && list.size() > 0) {
                    GstIdentifierMaster gstinObj = list.get(0);
                    gstinUniqueNo = gstinObj.getId();
                    if (!gstinObj.getId().equals(id)) {
                      logger = logger
                          + "WARNING: INGST_GST Identifier Master already present in Supply DB with id:"
                          + gstinObj.getId() + "  and uidno is: " + entityJson.getString("uidno")
                          + " and client Name is: "
                          + OBContext.getOBContext().getCurrentClient().getName()
                          + " , So Skip the Update action on record \n";
                      break loop;

                    }
                  }

                } else if (entityName.equals("ADTreeNode")) {
                  TreeNode treeNodeObj = OBDal.getInstance().get(TreeNode.class, id);

                  BaseOBObject bob = fromJsonConverter.toBaseOBObject(entityJson);
                  bob.setValue("tree", treeNodeObj.getTree().getId());
                  bob.setValue("node", treeNodeObj.getNode());
                  bob.setValue("organization", treeNodeObj.getOrganization().getId());
                  bob.setValue("reportSet", treeNodeObj.getReportSet());
                } else if (entityName.equals("Costcenter")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    Costcenter existingobj = OBDal.getInstance().get(Costcenter.class, id);
                    processedClientObj = existingobj.getClient();

                    OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                        Costcenter.class, "", entityJson.getString("searchKey"), null,
                        existingobj.getClient());
                    List<Costcenter> list = (List<Costcenter>) BaseOBObject.list();
                    if (list != null && list.size() > 0) {
                      for (Costcenter Obj : list) {
                        if (!Obj.getId().equals(id)) {
                          logger = logger
                              + "WARNING: Costcenter Search Key is Already Present in Supply DB with id:"
                              + Obj.getId() + "  and searchKey is: "
                              + entityJson.getString("searchKey") + " and Organization is:"
                              + existingobj.getOrganization().getName() + " and Client is: "
                              + existingobj.getClient().getName()
                              + ", So Skip the Updation Action on Supply DB \n";
                          break loop;
                        }
                      }
                    }
                  }
                } else if (entityName.equals("BusinessPartnerLocation")) {
                  // OBContext.setAdminMode(true);
                  if (id != null) {
                    OBContext.getOBContext().addWritableOrganization(id);
                    org.openbravo.model.common.businesspartner.Location existingobj = OBDal
                        .getInstance().get(
                            org.openbravo.model.common.businesspartner.Location.class, id);
                    processedClientObj = existingobj.getClient();
                  }
                }
                entityJson.put(JsonConstants.NEW_INDICATOR, false);
                BaseOBObject bob = fromJsonConverter.toBaseOBObject(entityJson);
                objectName = bob.getEntityName();
                objectID = bob.getIdentifier();
                bob.setValue("updated", date);
                // bob.setValue("creationDate", date);

                if (entityName.equals("Organization")) {
                  // OBContext.setAdminMode(true);
                  OBContext.getOBContext().addWritableOrganization((String) bob.getId());
                  bob.setValue("calendar", null);

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
                } else if (entityName.equals("OrganizationInformation")) {
                  // OBContext.setAdminMode(true);
                  OBContext.getOBContext().addWritableOrganization((String) bob.getId());
                  bob.setValue("yourCompanyDocumentImage", null);
                  bob.setValue("dsidefPosinvoicebp", existingPosInvoiceBPartner);
                  // OBContext.restorePreviousMode();ingstGstidentifirmaster
                  // bob.setValue("ingstGstidentifirmaster", null);
                  // bob.setValue("ingstGstidentifirmaster", gstinUniqueNo);

                  if (!entityJson.isNull("userContact")
                      && entityJson.getString("userContact").equals("")) {
                    User UserObj = null;

                    User existingUser = OBDal.getInstance().get(User.class,
                        entityJson.getString("userContact"));
                    if (existingUser != null) {
                      UserObj = existingUser;
                    } else {
                      userSet.add(entityJson.getString("userContact"));
                      UserObj = obUser;
                    }
                    bob.setValue("userContact", UserObj);
                  }
                } else if (entityName.equals("BusinessPartner")) {
                  bob.setValue("rCOxylane", null);
                }
                if (processedClientObj != null) {
                  OBContext.getOBContext().setCurrentClient(processedClientObj);
                  bob.setValue("client", processedClientObj);

                } else {
                  bob.setValue("client", currentClient);

                }
                try {

                  User existingCreatedByUser = OBDal.getInstance().get(User.class,
                      entityJson.getString("createdBy"));
                  if (existingCreatedByUser != null) {
                    createdByUser = existingCreatedByUser;
                  } else {
                    userSet.add(entityJson.getString("createdBy"));
                    createdByUser = obUser;
                  }

                  User existingUpdatedByUser = OBDal.getInstance().get(User.class,
                      entityJson.getString("updatedBy"));
                  if (existingUpdatedByUser != null) {
                    updatedByUser = existingUpdatedByUser;
                  } else {
                    userSet.add(entityJson.getString("updatedBy"));
                    updatedByUser = obUser;
                  }
                  bob.setValue("createdBy", createdByUser);
                  bob.setValue("updatedBy", updatedByUser);

                  if (entityName.equals("DocumentType")) {
                    if (!entityJson.isNull("ibdoContradocument")
                        && !entityJson.getString("ibdoContradocument").equals("")) {

                      DocumentType docTypeObj = OBDal.getInstance().get(DocumentType.class,
                          entityJson.getString("ibdoContradocument"));
                      if (docTypeObj == null) {
                        logger = logger + "WARNING: " + entityName
                            + "  Having  Contra Document Type is :"
                            + "  Not present in Supply DB with id:"
                            + entityJson.getString("ibdoContradocument") + " and Name is: "
                            + entityJson.getString("ibdoContradocument$_identifier")
                            + ", So Skip the Update Contra Document Type in "
                            + entityJson.getString("name") + " Document Type record. \n";
                        bob.setValue("ibdoContradocument", null);

                      }
                    } else {
                      bob.setValue("ibdoContradocument", null);
                    }
                  }

                  OBDal.getInstance().save(bob);
                  OBDal.getInstance().flush();
                } catch (Exception e) {
                  logger = logger + "ERROR: While Updating the Record for " + entityName
                      + " with id: " + id + " and Error is: " + e + " \n";
                  isSaved = false;
                  e.printStackTrace();

                } finally {
                  OBContext.getOBContext().setCurrentClient(currentClient);
                }

                if (entityName.equals("FinancialMgmtTaxRate")) {
                  deleteExtraTaxacct(id);
                }
                updateCount++;
              }
            } catch (Exception e) {
              isSaved = false;
              logger = logger + "ERROR: While Updating the Record for Entity:[" + objectName
                  + "]Identifier : [" + objectID + "] ," + e + " \n";
              e.printStackTrace();

            } finally {
              OBContext.getOBContext().setCurrentClient(currentClient);

            }
          } else {

            // Object is not present in Store so create it
            try {
              log.info(entityName + " with id=" + id + " does not exist, so inserting in DB");
              insertLoop: {
                BaseOBObject bob = fromJsonConverter.toBaseOBObject(entityJson);
                objectName = bob.getEntityName();
                objectID = bob.getIdentifier();

                bob.setValue("id", id);
                if (entityName.equals("INGST_GSTIdentifierMaster")) {

                  OBCriteria<GstIdentifierMaster> crit = OBDal.getInstance().createCriteria(
                      GstIdentifierMaster.class);
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_CLIENT, OBContext
                      .getOBContext().getCurrentClient()));
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_UIDNO,
                      entityJson.getString("uidno")));
                  crit.add(Restrictions.eq(GstIdentifierMaster.PROPERTY_ORGANIZATION, OBDal
                      .getInstance().get(Organization.class, entityJson.getString("organization"))));

                  if (!entityJson.isNull("businessPartner")) {
                    crit.add(Restrictions.eq(
                        GstIdentifierMaster.PROPERTY_BUSINESS,
                        OBDal.getInstance().get(BusinessPartner.class,
                            entityJson.getString("businessPartner"))));
                  } else {
                    crit.add(Restrictions.isNull(GstIdentifierMaster.PROPERTY_BUSINESS));
                  }
                  crit.setFilterOnReadableClients(false);

                  List<GstIdentifierMaster> list = crit.list();

                  if (list != null && list.size() > 0) {
                    GstIdentifierMaster Obj = list.get(0);
                    logger = logger
                        + "WARNING: INGST_GST Identifier Master already present in Supply DB with id:"
                        + Obj.getId() + "  and uidno is: " + entityJson.getString("uidno")
                        + " and client Name is: "
                        + OBContext.getOBContext().getCurrentClient().getName()
                        + " , So Skip the Insert action on record \n";
                    break insertLoop;

                  }

                } else if (entityName.equals("FinancialMgmtAcctSchema")) {

                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      AcctSchema.class, entityJson.getString("name"), "", null, OBContext
                          .getOBContext().getCurrentClient());
                  List<AcctSchema> list = (List<AcctSchema>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    AcctSchema Obj = list.get(0);
                    logger = logger
                        + "WARNING: Acct Schema Name already present in Supply DB with id:"
                        + Obj.getId() + "  and Name is: " + entityJson.getString("name")
                        + " and client Name is: "
                        + OBContext.getOBContext().getCurrentClient().getName()
                        + " , So Skip the Insert action on record \n";
                    break insertLoop;

                  }

                } else if (entityName.equals("Organization")) {

                  OBContext.getOBContext().addWritableOrganization((String) bob.getId());
                  bob.setValue("calendar", null);

                  OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(
                      Organization.class);
                  orgCrit.add(Restrictions.eq(Organization.PROPERTY_CLIENT, OBContext
                      .getOBContext().getCurrentClient()));
                  orgCrit.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY,
                      entityJson.getString("searchKey")));
                  orgCrit.setFilterOnReadableClients(false);

                  List<Organization> orgList = orgCrit.list();
                  if (orgList != null && orgList.size() > 0) {
                    Organization orgObj = orgList.get(0);

                    logger = logger + "WARNING: Organization Search Key already present with id:"
                        + orgObj.getId() + " in Supply DB with Search Key is: "
                        + entityJson.getString("searchKey")
                        + " , So Skip the Insert action on record  \n";
                    break insertLoop;

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
                  // bob.setValue("ingstGstidentifirmaster", null);

                }
                bob.setValue("updated", date);
                bob.setValue("creationDate", date);

                if (entityName.equals("DocumentType")) {
                  if (!entityJson.isNull("ibdoContradocument")
                      && !entityJson.getString("ibdoContradocument").equals("")) {

                    DocumentType docTypeObj = OBDal.getInstance().get(DocumentType.class,
                        entityJson.getString("ibdoContradocument"));
                    if (docTypeObj == null) {
                      logger = logger + "WARNING: " + entityName
                          + "  Having  Contra Document Type is :"
                          + "  Not present in Supply DB with id:"
                          + entityJson.getString("ibdoContradocument") + " and Name is: "
                          + entityJson.getString("ibdoContradocument$_identifier")
                          + ", So Skip the Update Contra Document Type in "
                          + entityJson.getString("name") + " Document Type record. \n";
                      bob.setValue("ibdoContradocument", null);

                    }
                  } else {
                    bob.setValue("ibdoContradocument", null);
                  }
                }
                if (entityName.equals("ADSequence")) {
                  String name = entityJson.getString("name");

                  Organization orgObj = OBDal.getInstance().get(Organization.class,
                      entityJson.getString("organization"));
                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      Sequence.class, entityJson.getString("name"), "", orgObj, OBContext
                          .getOBContext().getCurrentClient());
                  List<Sequence> list = (List<Sequence>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    logger = logger + "WARNING: " + entityName
                        + "  Name is Already Present in Supply DB with id:" + list.get(0).getId()
                        + "  and Name is: " + entityJson.getString("name")
                        + " and client Name is: "
                        + OBContext.getOBContext().getCurrentClient().getName()
                        + " and Organization Name is: " + orgObj.getName()
                        + ", So Skip the Insert action on DB \n";
                    break insertLoop;

                  }
                  deleteExistingSequence(name);
                } else if (entityName.equals("DocumentType")) {

                  Organization orgObj = OBDal.getInstance().get(Organization.class,
                      entityJson.getString("organization"));
                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      DocumentType.class, entityJson.getString("name"), "", orgObj, OBContext
                          .getOBContext().getCurrentClient());
                  List<DocumentType> list = (List<DocumentType>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    logger = logger + "WARNING: " + entityName
                        + "  Name is Already Present in Supply DB with id:" + list.get(0).getId()
                        + "  and Name is: " + entityJson.getString("name")
                        + " and client Name is: "
                        + OBContext.getOBContext().getCurrentClient().getName()
                        + " and Organization Name is: " + orgObj.getName()
                        + ", So Skip the Insert action on DB \n";
                    break insertLoop;

                  }
                } else if (entityName.equals("FinancialMgmtGLCategory")) {

                  Organization orgObj = OBDal.getInstance().get(Organization.class,
                      entityJson.getString("organization"));
                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      GLCategory.class, entityJson.getString("name"), "", orgObj, OBContext
                          .getOBContext().getCurrentClient());
                  List<GLCategory> list = (List<GLCategory>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    logger = logger + "WARNING: " + entityName
                        + "  Name is Already Present in Supply DB with id:" + list.get(0).getId()
                        + "  and Name is: " + entityJson.getString("name")
                        + " and client Name is: "
                        + OBContext.getOBContext().getCurrentClient().getName()
                        + " and Organization Name is: " + orgObj.getName()
                        + ", So Skip the Insert action on DB \n";
                    break insertLoop;

                  }
                } else if (entityName.equals("BusinessPartner")) {
                  bob.setValue("rCOxylane", null);
                  Organization orgObj = OBDal.getInstance().get(Organization.class,
                      entityJson.getString("organization"));
                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      BusinessPartner.class, "", entityJson.getString("searchKey"), orgObj,
                      OBContext.getOBContext().getCurrentClient());
                  List<BusinessPartner> list = (List<BusinessPartner>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (BusinessPartner Obj : list) {
                      logger = logger + "WARNING: " + entityName
                          + " Search Key is Already Present in Supply DB with id:" + Obj.getId()
                          + "  and searchKey is: " + entityJson.getString("searchKey")
                          + " and Organization is:" + Obj.getOrganization().getName()
                          + " and Client is: "
                          + OBContext.getOBContext().getCurrentClient().getName()
                          + ", So Skip the Insert Action on Supply DB \n";
                      break insertLoop;

                    }
                  }

                } else if (entityName.equals("Costcenter")) {

                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      Costcenter.class, "", entityJson.getString("searchKey"), null, OBContext
                          .getOBContext().getCurrentClient());
                  List<Costcenter> list = (List<Costcenter>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (Costcenter Obj : list) {
                      logger = logger + "WARNING: " + entityName
                          + " Search Key is Already Present in Supply DB with id:" + Obj.getId()
                          + "  and searchKey is: " + entityJson.getString("searchKey")
                          + " and Organization is:" + Obj.getOrganization().getName()
                          + " and Client is: "
                          + OBContext.getOBContext().getCurrentClient().getName()
                          + ", So Skip the Insert Action on Supply DB \n";
                      break insertLoop;

                    }
                  }
                } else if (entityName.equals("FinancialMgmtPaymentTerm")) {
                  Organization orgObj = OBDal.getInstance().get(Organization.class,
                      entityJson.getString("organization"));
                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      PaymentTerm.class, "", entityJson.getString("searchKey"), orgObj, OBContext
                          .getOBContext().getCurrentClient());
                  List<PaymentTerm> list = (List<PaymentTerm>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (PaymentTerm Obj : list) {
                      logger = logger + "WARNING: " + entityName
                          + " Search Key is Already Present in Supply DB with id:" + Obj.getId()
                          + "  and searchKey is: " + entityJson.getString("searchKey")
                          + " and Organization is:" + Obj.getOrganization().getName()
                          + " and Client is: "
                          + OBContext.getOBContext().getCurrentClient().getName()
                          + ", So Skip the Insert Action on Supply DB \n";
                      break insertLoop;

                    }
                  }
                } else if (entityName.equals("BusinessPartnerCategory")) {

                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      Category.class, "", entityJson.getString("searchKey"), null, OBContext
                          .getOBContext().getCurrentClient());
                  List<Category> list = (List<Category>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (Category Obj : list) {
                      logger = logger + "WARNING: " + entityName
                          + " Search Key is Already Present in Supply DB with id:" + Obj.getId()
                          + "  and searchKey is: " + entityJson.getString("searchKey")
                          + " and Organization is:" + Obj.getOrganization().getName()
                          + " and Client is: "
                          + OBContext.getOBContext().getCurrentClient().getName()
                          + ", So Skip the Insert Action on Supply DB \n";
                      break insertLoop;

                    }
                  }
                } else if (entityName.equals("PricingPriceList")) {

                  OBCriteria<? extends BaseOBObject> BaseOBObject = getObCreteriaObject(
                      PriceList.class, entityJson.getString("name"), "", null, OBContext
                          .getOBContext().getCurrentClient());

                  List<PriceList> list = (List<PriceList>) BaseOBObject.list();
                  if (list != null && list.size() > 0) {
                    for (PriceList Obj : list) {
                      logger = logger
                          + "WARNING: Price List Name is Already Present in Supply DB with id:"
                          + Obj.getId() + "  and Name is: " + entityJson.getString("name")
                          + " and Organization is:" + Obj.getOrganization().getName()
                          + " and Client is: " + Obj.getClient().getName()
                          + ", So Skip the Updation Action on Supply DB \n";
                      break insertLoop;
                    }

                  }

                } else if (entityName.equals("ADTreeNode")) {
                  OBDal.getInstance().flush();
                  OBContext.setAdminMode(true);
                  String treeNodeId = entityJson.getString("tree");
                  String nodeId = entityJson.getString("node");
                  deleteExistingTreeNode(treeNodeId, nodeId);

                  OBContext.restorePreviousMode();
                } else if (entityName.equals("Region")) {
                  Country countryObj = OBDal.getInstance().get(Country.class,
                      entityJson.getString("country"));
                  OBCriteria<Region> genClassCriteria = OBDal.getInstance().createCriteria(
                      Region.class);

                  genClassCriteria.add(Restrictions.eq(Region.PROPERTY_NAME,
                      entityJson.getString("name")));

                  genClassCriteria.add(Restrictions.eq(Region.PROPERTY_COUNTRY, countryObj));

                  genClassCriteria.setFilterOnActive(false);
                  genClassCriteria.setFilterOnReadableOrganization(false);

                  List<Region> list = genClassCriteria.list();
                  if (list != null && list.size() > 0) {
                    for (Region Obj : list) {
                      logger = logger + "WARNING: Region is Already Present in Supply DB with id:"
                          + Obj.getId() + "  and Name is: " + entityJson.getString("name")
                          + " and Country Name is:" + countryObj.getName() + " and Client is: "
                          + Obj.getClient().getName()
                          + ", So Skip the Insert Action on Supply DB \n";
                      break insertLoop;
                    }

                  }

                }
                User existingCreatedByUser = OBDal.getInstance().get(User.class,
                    entityJson.getString("createdBy"));
                if (existingCreatedByUser != null) {
                  createdByUser = existingCreatedByUser;
                } else {
                  userSet.add(entityJson.getString("createdBy"));
                  createdByUser = obUser;
                }

                User existingUpdatedByUser = OBDal.getInstance().get(User.class,
                    entityJson.getString("updatedBy"));
                if (existingUpdatedByUser != null) {
                  updatedByUser = existingUpdatedByUser;
                } else {
                  userSet.add(entityJson.getString("updatedBy"));
                  updatedByUser = obUser;
                }

                bob.setValue("createdBy", createdByUser);
                bob.setValue("updatedBy", updatedByUser);

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
              logger = logger + "ERROR: while Inserting the Record for Entity:[" + objectName
                  + "]Identifier : [" + objectID + "] , and Error is: " + e + " \n";
              e.printStackTrace();

            }
          }
        } catch (Exception e) {
          isSaved = false;
          logger = logger + "ERROR: while Processing for Entity:[" + objectName + "]Identifier : ["
              + objectID + "] ,and Error is: " + e + " \n";
          e.printStackTrace();

        }

      }

      logger = logger + "INFO: Record " + ProcessentityName + " and Inserted the Record cound is: "
          + insertCount + " and Update Record Count id: " + updateCount + " \n";

    } catch (Exception e) {
      isSaved = false;
      e.printStackTrace();

    } finally {
      OBContext.restorePreviousMode();

    }
    return isSaved;

  }

  public static OBCriteria<? extends BaseOBObject> getObCreteriaObject(
      Class<? extends BaseOBObject> bob, String name, String value, Organization orgObj,
      Client clientObj) throws Exception {
    OBCriteria<? extends BaseOBObject> genClassCriteria = OBDal.getInstance().createCriteria(bob);
    if (name != null && !name.equalsIgnoreCase("")) {
      genClassCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_NAME, name));
    }
    if (value != null && !value.equalsIgnoreCase("")) {
      genClassCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, value));
    }
    if (clientObj != null) {
      genClassCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_CLIENT, clientObj));
    }
    if (orgObj != null) {
      genClassCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_ORGANIZATION, orgObj));
    }
    genClassCriteria.setFilterOnActive(false);
    genClassCriteria.setFilterOnReadableOrganization(false);
    return genClassCriteria;
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
}