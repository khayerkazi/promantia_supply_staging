package in.decathlon.ibud.commons;

import in.decathlon.ibud.commons.data.IbdcomErrorLog;
import in.decathlon.ibud.masters.data.IbudServerTime;
import in.decathlon.ibud.orders.client.SOConstants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.warehouse.pickinglist.PickingList;

import com.sysfore.catalog.CLImplantation;

public class BusinessEntityMapper {
  private static final Logger log = Logger.getLogger(BusinessEntityMapper.class);

  public static Organization getOrgOfBP(String businesPartnerId) throws Exception {
    String qry = "id in (select organization from OrganizationInformation oinfo where oinfo.businessPartner.id = "
        + ":businesPartnerId )";

    OBQuery<Organization> orgQry = OBDal.getInstance().createQuery(Organization.class, qry);
    orgQry.setNamedParameter("businesPartnerId", businesPartnerId);
    orgQry.setFilterOnActive(false);
    orgQry.setFilterOnReadableOrganization(false);
    if (orgQry.uniqueResult() != null)
      return orgQry.uniqueResult();
    else
      throw new OBException("Organization does not exists for BPartner " + businesPartnerId);
  }

  public static BusinessPartner getBPOfOrg(String orgId) {
    String qry = "id in (select businessPartner from OrganizationInformation orgi where orgi.organization.id="
        + ":orgId )";
    OBQuery<BusinessPartner> bPQuery = OBDal.getInstance().createQuery(BusinessPartner.class, qry);
    bPQuery.setNamedParameter("orgId", orgId);
    bPQuery.setFilterOnActive(false);
    bPQuery.setFilterOnReadableOrganization(false);
    if (bPQuery.uniqueResult() != null)
      return bPQuery.uniqueResult();
    else
      throw new OBException("Business partner not present for org "
          + OBDal.getInstance().get(Organization.class, orgId).getName());
  }

  /**
   * @param BPartnerId
   * @return true if businesspartner belongs to any one orgInfo
   */
  public static boolean isSupplyBusinessPartnerConfigured(String BPartnerId) {
    String qry = "id in (select organization from OrganizationInformation oi where oi.businessPartner.id = "
        + ":BPartnerId )";
    OBQuery<Organization> bpQry = OBDal.getInstance().createQuery(Organization.class, qry);
    bpQry.setNamedParameter("BPartnerId", BPartnerId);
    bpQry.setFilterOnActive(false);
    bpQry.setMaxResult(1);
    if (bpQry.count() == 1)
      return true;

    return false;

  }

  public static DocumentType getContraDocumentType(DocumentType documentType) throws Exception {

    DocumentType docType = OBDal.getInstance().get(DocumentType.class, documentType.getId());
    if (docType != null) {
      return docType.getIbdoContradocument();
    } else {
      log.error("No contra Document type for " + documentType.getName());
      throw new OBException("There is no Contra Document type mentioned for "
          + documentType.getName());
    }
  }

  public static DocumentType getDocumentTypeOnName(String name) {

    OBCriteria<DocumentType> docTypeCrit = OBDal.getInstance().createCriteria(DocumentType.class);
    docTypeCrit.add(Restrictions.eq(DocumentType.PROPERTY_NAME, name));
    docTypeCrit.setMaxResults(1);

    if (docTypeCrit.count() == 1) {
      return docTypeCrit.list().get(0);
    } else {
      log.error("No Document type of name " + name);
      throw new OBException("No Document type of name " + name);
    }
  }

  public static String getSupplyBPartner(Organization org) {
    if (org.getOrganizationInformationList() != null) {
      OrganizationInformation orgInfo = org.getOrganizationInformationList().get(0);
      if (orgInfo.getIbdoAdOrgV() != null) {
        String warehouseOrgId = orgInfo.getIbdoAdOrgV().getId();
        OrganizationInformation warehouseOrgInfo = OBDal.getInstance().get(
            OrganizationInformation.class, warehouseOrgId);
        if (warehouseOrgInfo.getBusinessPartner() != null) {
          return warehouseOrgInfo.getBusinessPartner().getId();
        } else {
          throw new OBException("No Bpartner for org " + warehouseOrgInfo);
        }
      } else {
        log.error("No warehouse Org set for the org in OrgInfo" + org);
        throw new OBException("No warehouse Org set for the org in OrgInfo " + org);
      }

    } else {
      log.error("No OrgInfo for the org " + org);
      throw new OBException("No OrgInfo for the org " + org);
    }

  }

  public static PaymentTerm getPaymentTerm(String name) {
    OBCriteria<PaymentTerm> paymentTermCrit = OBDal.getInstance().createCriteria(PaymentTerm.class);
    paymentTermCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
    List<PaymentTerm> finPayTermCritList = paymentTermCrit.list();
    if (finPayTermCritList != null && finPayTermCritList.size() > 0) {
      return finPayTermCritList.get(0);
    } else {
      throw new OBException("payment term not found");
    }
  }

  public static FIN_PaymentMethod getPaymentMethod(String name) {
    OBCriteria<FIN_PaymentMethod> paymentCrit = OBDal.getInstance().createCriteria(
        FIN_PaymentMethod.class);
    paymentCrit.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_NAME, name));
    List<FIN_PaymentMethod> finPayCritList = paymentCrit.list();
    if (finPayCritList != null && finPayCritList.size() > 0) {
      return finPayCritList.get(0);
    } else {
      throw new OBException("payment method not found");
    }
  }

  public static Client getClient(String name) {
    OBCriteria<Client> clientCrit = OBDal.getInstance().createCriteria(Client.class);
    clientCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
    List<Client> clientCritList = clientCrit.list();
    if (clientCritList != null && clientCritList.size() > 0) {
      return clientCritList.get(0);
    } else {
      throw new OBException("client not found");
    }
  }

  public static int setLastUpdatedTime(String newDate, String serviceKey) {
    String qry = "update Ibud_ServerTime set lastupdated = '" + newDate
        + "' where serviceKey= :serviceKey and client='"
        + OBContext.getOBContext().getCurrentClient().getId() + "'";
    log.info("executing " + qry);
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("serviceKey", serviceKey);
    int rowUpdated = query.executeUpdate();
    log.info("executed row=" + rowUpdated);
    return rowUpdated;
  }

  public static OrgWarehouse getOrgWarehouse(String orgId) {

    String qry = " ow  where ow.organization.id=:orgId order by ow.priority asc     ";
    OBQuery<OrgWarehouse> wQuery = OBDal.getInstance().createQuery(OrgWarehouse.class, qry);
    wQuery.setNamedParameter("orgId", orgId);
    wQuery.setMaxResult(1);
    wQuery.setFilterOnReadableOrganization(false);
    List<OrgWarehouse> owlist = wQuery.list();
    if (owlist != null && owlist.size() > 0)
      return owlist.get(0);
    else
      throw new OBException("There are no priority warehouse in organization window "
          + OBDal.getInstance().get(Organization.class, orgId).getName());
  }

  public static OrgWarehouse getReturnOrgWarehouse(String orgId) {

    String qry = " ow  where ow.organization.id=:orgId and ow.warehouse.idsdWhzone='SZ' order by ow.priority asc     ";
    OBQuery<OrgWarehouse> wQuery = OBDal.getInstance().createQuery(OrgWarehouse.class, qry);
    wQuery.setNamedParameter("orgId", orgId);
    wQuery.setMaxResult(1);
    wQuery.setFilterOnReadableOrganization(false);
    List<OrgWarehouse> owlist = wQuery.list();
    if (owlist != null && owlist.size() > 0)
      return owlist.get(0);
    else
      throw new OBException(
          "There are no priority warehouse of south zone in organization window of"
              + OBDal.getInstance().get(Organization.class, orgId).getName());
  }

  public static Warehouse getWarehouse(Organization org, String type) throws Exception {

    String qry = "id in (select id from Warehouse where organization.id = :orgId "
        + " and ibdoWarehousetype=:type )";
    OBQuery<Warehouse> wareQuery = OBDal.getInstance().createQuery(Warehouse.class, qry);
    wareQuery.setNamedParameter("orgId", org.getId());
    wareQuery.setNamedParameter("type", type);
    if (wareQuery.uniqueResult() != null)
      return wareQuery.uniqueResult();
    else
      throw new OBException("there is no warehouse of type " + type + " for the org " + org);
  }

  public static Warehouse getWarehouse(Organization org) {

    OBContext.setAdminMode(true);
    String qry = "id in (select id from Warehouse wh where wh.ibdoWarehousetype='CAR' and wh.organization.id='"
        + org.getId() + "')";
    OBQuery<Warehouse> strQry = OBDal.getInstance().createQuery(Warehouse.class, qry);
    strQry.setMaxResult(1);
    List<Warehouse> WarehouseList = strQry.list();
    if (WarehouseList != null && WarehouseList.size() > 0)
      return WarehouseList.get(0);
    else
      throw new OBException("Warehouse not found for Org " + org.getName());

  }

  public static Locator getFirstStorageBin(String warehouse) {
    String qry = "sb where sb.warehouse.id ='" + warehouse + "'";
    OBQuery<Locator> storageqry = OBDal.getInstance().createQuery(Locator.class, qry);
    storageqry.setMaxResult(1);
    List<Locator> storageList = storageqry.list();
    if (storageList != null && storageList.size() > 0) {
      return storageList.get(0);
    } else {
      throw new OBException("There are no storage bin associated with warehouse " + warehouse);
    }

  }

  public static Sequence getDocumentSequence(Organization org) {

    OBCriteria<Sequence> seqCrit = OBDal.getInstance().createCriteria(Sequence.class);
    Criterion orgName = Restrictions.eq(Sequence.PROPERTY_ORGANIZATION, org);
    Criterion seqName = Restrictions.eq(Sequence.PROPERTY_NAME, org.getName());
    LogicalExpression orExp = Restrictions.or(orgName, seqName);
    seqCrit.add(orExp);
    // seqCrit.add(Restrictions.eq(Sequence.PROPERTY_NAME, org.getName()));
    seqCrit.setMaxResults(1);
    if (seqCrit.count() == 1) {
      return seqCrit.list().get(0);
    }

    else {
      log.error("There is no document Sequence for the organization " + org);
      throw new OBException("There is no document Sequence for the organization " + org);
    }

  }

  public static boolean executeProcess(String currentObjId, String processId, String strQry)
      throws Exception {
    boolean flag = false;
    OBContext.setAdminMode(true);
    final Process process = OBDal.getInstance().get(Process.class, processId);
    final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
    pInstance.setProcess(process);
    pInstance.setRecordID(currentObjId);
    pInstance.setUserContact(OBContext.getOBContext().getUser());
    OBDal.getInstance().save(pInstance);
    try {
      final Connection connection = OBDal.getInstance().getConnection();
      final PreparedStatement ps = connection.prepareStatement(strQry);
      ps.setString(1, pInstance.getId());

      flag = ps.execute();
      OBDal.getInstance().getSession().refresh(pInstance);
      if (pInstance.getResult() == 0) {
        throw new OBException(pInstance.getErrorMsg());
      }
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
    return flag;
  }

  public static int setLastUpdatedTimeMaster(Date lastUpdatedTime, String key) {
    IbudServerTime servertime = OBDal.getInstance().get(IbudServerTime.class, "1");
    servertime.setLastupdated(lastUpdatedTime);
    OBDal.getInstance().save(servertime);
    return 1;
  }

  public static PriceList getPriceList(String name) {
    OBCriteria<PriceList> priceListCrit = OBDal.getInstance().createCriteria(PriceList.class);
    priceListCrit.add(Restrictions.eq(PriceList.PROPERTY_NAME, name));
    List<PriceList> finPayTermCritList = priceListCrit.list();
    if (finPayTermCritList != null && finPayTermCritList.size() > 0) {
      return finPayTermCritList.get(0);
    } else {
      throw new OBException("PriceList not found");
    }
  }

  public static CLImplantation getImplantationOrg(String storeOrgId, String prodId) {
    String qry = "impl where impl.storeImplanted.id = '" + storeOrgId + "' and impl.product.id = '"
        + prodId + "'";
    OBQuery<CLImplantation> implCrit = OBDal.getInstance().createQuery(CLImplantation.class, qry);
    implCrit.setMaxResult(1);
    if (implCrit.count() == 1) {
      return implCrit.list().get(0);
    } else {
      log.error("There is no record for the organization " + storeOrgId + "with product " + prodId);
      return null;
    }
  }

  public static List<Organization> getStoreOrganizations() {
    OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(Organization.class);
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, true));
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
    List<Organization> orgList = orgCrit.list();
    if (orgList != null && orgList.size() > 0) {
      return orgList;
    } else {
      throw new OBException("No Stroe organization is active");
    }

  }

  public static OrgWarehouse getImplWarehouse(Organization org) {
    String qry = " ow  where ow.organization.id=:orgId order by ow.priority asc ";
    OBQuery<OrgWarehouse> wQuery = OBDal.getInstance().createQuery(OrgWarehouse.class, qry);
    wQuery.setNamedParameter("orgId", org.getId());
    wQuery.setMaxResult(1);
    List<OrgWarehouse> owlist = wQuery.list();
    if (owlist != null && owlist.size() > 0)
      return owlist.get(0);
    else
      throw new OBException("There are no priority warehouse in organization window "
          + OBDal.getInstance().get(Organization.class, org).getName());
  }

  public static void setPOBackToDraft(String orderId) throws Exception {
    try {
      Order currentOrder = OBDal.getInstance().get(Order.class, orderId);
      currentOrder.setDocumentAction("RE");
      // OBDal.getInstance().save(currentOrder);
      SessionHandler.getInstance().commitAndStart();
      BusinessEntityMapper.executeProcess(orderId, "104", "SELECT * FROM c_order_post(?)");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }

  }

  public static AttributeSetInstance getAttributeSetValue(ShipmentInOut bob) {
    AttributeSetInstance attributeSetIns = OBProvider.getInstance().get(AttributeSetInstance.class);
    attributeSetIns.setClient(bob.getClient());
    attributeSetIns.setOrganization(bob.getOrganization());
    attributeSetIns.setCreatedBy(bob.getCreatedBy());
    attributeSetIns.setUpdatedBy(bob.getUpdatedBy());
    attributeSetIns.setCreationDate(new Date());
    attributeSetIns.setUpdated(new Date());
    attributeSetIns.setLotName("0");
    attributeSetIns.setAttributeSet(getAttributeSet());
    attributeSetIns.setDescription("L0");
    OBDal.getInstance().save(attributeSetIns);
    OBDal.getInstance().flush();
    return attributeSetIns;
  }

  private static AttributeSet getAttributeSet() {
    OBCriteria<AttributeSet> attrCrit = OBDal.getInstance().createCriteria(AttributeSet.class);
    attrCrit.add(Restrictions.eq(AttributeSet.PROPERTY_NAME, "Lot"));
    attrCrit.setMaxResults(1);
    if (attrCrit.count() > 0)
      return attrCrit.list().get(0);
    else
      return OBDal.getInstance().get(AttributeSet.class, "0");
  }

  public static DocumentType getTrasactionDocumentType(Organization org) {
    OBCriteria<DocumentType> docTypeCriteria = OBDal.getInstance().createCriteria(
        DocumentType.class);
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_ORGANIZATION, org));
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, "POO"));
    List<DocumentType> docTypeCriteriaList = docTypeCriteria.list();
    if (docTypeCriteriaList != null && docTypeCriteriaList.size() > 0) {
      return docTypeCriteriaList.get(0);
    } else {
      throw new OBException("document type not found");
    }
  }

  public static DocumentType getDocType(String baseType, boolean returnType) {
    OBCriteria<DocumentType> returnDocType = OBDal.getInstance().createCriteria(DocumentType.class);
    returnDocType.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, baseType));
    returnDocType.add(Restrictions.eq(DocumentType.PROPERTY_RETURN, returnType));
    returnDocType.add(Restrictions.eq(DocumentType.PROPERTY_IBODTRISINTERORG, true));
    returnDocType.setMaxResults(1);
    List<DocumentType> documentType = returnDocType.list();
    if (documentType != null && documentType.size() > 0)
      return documentType.get(0);
    else
      throw new OBException("No document type found for return of base type" + baseType);

  }

  @SuppressWarnings("unchecked")
  public static BigDecimal getStockOnWarehouse(String productId, String warehouseId) {

    String qry = " SELECT COALESCE(sum(quantityOnHand),0) as stockQty from MaterialMgmtStorageDetail where storageBin.warehouse.id = :warehouseId and product.id=:productId ";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("warehouseId", warehouseId);
    query.setParameter("productId", productId);

    List<BigDecimal> queryList = query.list();
    BigDecimal qtyInStock = BigDecimal.ZERO;
    if (queryList != null & queryList.size() > 0) {
      qtyInStock = queryList.get(0);
    }

    return qtyInStock;

  }

  public static List<Order> getSoOnDocNo(String docNo) {
    // String docNoUp = docNo + "%";
    List<Order> ordList = new ArrayList<Order>();
    OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
    ordCrit.add(Restrictions.eq(Order.PROPERTY_SWPOREFERENCE, docNo));
    ordCrit.add(Restrictions.ne(Order.PROPERTY_DOCUMENTSTATUS, "VO"));
    ordCrit.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
    ordCrit.addOrderBy(Order.PROPERTY_CREATIONDATE, false);
    ordList = ordCrit.list();
    return ordList;
  }

  public static void txnSWMovementType(ShipmentInOut returns) {
    List<ShipmentInOutLine> grLineList = returns.getMaterialMgmtShipmentInOutLineList();
    for (ShipmentInOutLine grLine : grLineList) {
      String qry = "id in(from MaterialMgmtMaterialTransaction txn where txn.goodsShipmentLine.id = '"
          + grLine.getId() + "')";
      OBQuery<MaterialTransaction> obqry = OBDal.getInstance().createQuery(
          MaterialTransaction.class, qry);
      List<MaterialTransaction> txnList = obqry.list();
      for (MaterialTransaction txn : txnList) {
        txn.setSwMovementtype(returns.getSWMovement());
        OBDal.getInstance().save(txn);
      }
    }
  }

  public static void rollBackNlogError(Exception e, String processid, JSONObject recordInfo) {
    SessionHandler.getInstance().rollback();
    try {
      createErrorLogRecord(e, processid, recordInfo);
    } catch (Exception exc) {
      log.error(e);
    } finally {
      // OBDal.getInstance().flush();
      SessionHandler.getInstance().commitAndStart();
    }

  }

  public static void createErrorLogRecord(Exception e, String processid, JSONObject recordInfo)
      throws JSONException {
    IbdcomErrorLog ibdcomError = OBProvider.getInstance().get(IbdcomErrorLog.class);
    try {
      Writer writer = new StringWriter();
      PrintWriter printWriter = new PrintWriter(writer);

      ibdcomError.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      ibdcomError.setClient(OBContext.getOBContext().getCurrentClient());
      ibdcomError.setActive(true);
      ibdcomError.setCreatedBy(OBContext.getOBContext().getUser());
      ibdcomError.setCreationDate(new Date());
      ibdcomError.setUpdatedBy(OBContext.getOBContext().getUser());
      ibdcomError.setUpdated(new Date());
      if (processid != null) {
        Process proc = OBDal.getInstance().get(Process.class, processid);
        ibdcomError.setProcess(proc);
      }
      if (e != null) {
        e.printStackTrace(printWriter);
        String s = writer.toString();
        ibdcomError.setError(s);
      }
      if (recordInfo != null) {
        String recordId = recordInfo.getString(SOConstants.RECORD_ID);
        if (recordId != null) {
          ibdcomError.setRecord(recordId);
        }
        String recordIdentifier = recordInfo.getString(SOConstants.recordIdentifier);
        if (recordIdentifier != null) {
          ibdcomError.setRecordIdentifier(recordIdentifier);
        }
        String tableName = recordInfo.getString(SOConstants.TABLE_NAME);
        if (tableName != null) {
          ibdcomError.setTableName(tableName);
        }
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      OBDal.getInstance().save(ibdcomError);
    }
  }

  public static Organization getOrgOnName(String orgName) {
    OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(Organization.class);
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_NAME, orgName));
    orgCrit.setMaxResults(1);
    if (orgCrit.count() > 0) {
      return orgCrit.list().get(0);
    } else {
      throw new OBException("Organization by name " + orgName + " does not exist");
    }
  }

  public static DocumentType getRTVTrasactionDocumentType() {
    OBCriteria<DocumentType> docTypeCriteria = OBDal.getInstance().createCriteria(
        DocumentType.class);
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_ORGANIZATION,
        BusinessEntityMapper.getOrgOnName("*")));
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_RETURN, true));
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_IBODTRISINTERORG, true));
    docTypeCriteria.add(Restrictions.eq(DocumentType.PROPERTY_SALESTRANSACTION, false));
    List<DocumentType> docTypeCriteriaList = docTypeCriteria.list();
    if (docTypeCriteriaList != null && docTypeCriteriaList.size() > 0) {
      return docTypeCriteriaList.get(0);
    } else {
      throw new OBException("return trx document type not found");
    }
  }

  public static Product getproductOnName(String product) {
    OBCriteria<Product> prdCrit = OBDal.getInstance().createCriteria(Product.class);
    prdCrit.add(Restrictions.eq(Product.PROPERTY_NAME, product));
    prdCrit.setMaxResults(1);
    List<Product> prdList = prdCrit.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    } else
      throw new OBException("There is no product with the name: " + product);
  }

  public static boolean getdupIdNameRecord(String id, String entityName, String colNameValue) {
    List parameter = new ArrayList();
    parameter.add(entityName);
    String procedureName = "ibdcom_gettablename";
    String resp = CallStoredProcedure.getInstance().call(procedureName, parameter, null).toString();
    if (resp != null) {
      String qry = "select id from " + resp + " as tb where tb.name='" + colNameValue
          + "' and tb.id='" + id + "'";
      Query query = OBDal.getInstance().getSession().createQuery(qry);
      List qryList = query.list();
      if (qryList != null && qryList.size() > 0) {
        return false;
      }
    }
    return true;
  }

  public static boolean getPickListLines(Order ord, PickingList picking) {
    String qry = " id in (select id from MaterialMgmtInternalMovementLine ml where ml.oBWPLWarehousePickingList.id= '"
        + picking.getId()
        + "' and"
        + " ml.stockReservation.salesOrderLine.salesOrder.id= '"
        + ord.getId() + "' )";
    OBQuery<InternalMovementLine> query = OBDal.getInstance().createQuery(
        InternalMovementLine.class, qry);
    List<InternalMovementLine> mlList = query.list();
    if (mlList != null && mlList.size() > 0)
      return true;
    return false;
  }

  public static void closeSupplySO(ShipmentInOut goodShipment, HashMap<String, String> soDocNoObj)
      throws Exception {
    try {
      // JSONObject soDocNoObj = new JSONObject();
      for (ShipmentInOutLine inoutLine : goodShipment.getMaterialMgmtShipmentInOutLineList()) {
        OrderLine ordLine = inoutLine.getSalesOrderLine();
        if (ordLine != null) {

          Order ord = ordLine.getSalesOrder();
          if (ord != null) {
            List<String> queryList = getOrdLineHvngTruckCOOrEmpty(ord);
            if (queryList.size() == 0) {

              ord.setDocumentStatus(SOConstants.closed);
              ord.setDocumentAction("--");

            } else if (queryList != null && queryList.size() > 0) {
              int zeroLineCount = 0;
              for (String colId : queryList) {
                List<String> mvmtList = getMMLZeroRecForOrdLine(colId);
                if (mvmtList != null && mvmtList.size() > 0)
                  zeroLineCount++;
                else
                  break;
              }
              if (queryList.size() == zeroLineCount) {
                ord.setDocumentStatus(SOConstants.closed);
                ord.setDocumentAction("--");
              }
            }

            else {

              log.debug("All the shipments for the order are not yet completed for the order "
                  + ord);
            }
            ord.setProcessed(true);
            OBDal.getInstance().save(ord);
            OBDal.getInstance().flush();
            respForPOStatus(soDocNoObj, ord);

          } else
            throw new OBException(" no order reference for the orderline " + ordLine);

        } else
          throw new OBException(" no orderline reference for the inoutline " + inoutLine);
      }
      // return soDocNoObj;
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new OBException("error in close of order = " + e);
    }
  }

  public static List<String> getOrdLineHvngTruckCOOrEmpty(Order ord) {
    String qry = "select col.id from MaterialMgmtShipmentInOutLine mil "
        + "right join mil.salesOrderLine col right join col.salesOrder co left join mil.shipmentReceipt mi"
        + " where co.id='" + ord.getId() + "'  and (mi.documentStatus <> '"
        + SOConstants.CompleteDocumentStatus + "'  or mi.id is null)";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    log.debug("query to retrieve ol to check close status " + qry);
    List<String> queryList = query.list();
    return queryList;
  }

  public static List<String> getMMLZeroRecForOrdLine(String colId) {
    String mvmtQryStr = "select distinct col.id from MaterialMgmtInternalMovementLine ml "
        + "right join ml.salesOrderLine col join col.salesOrder co  where ml.id is null and  "
        + "  col.id='" + colId + "'";
    log.debug("qry to check zero line " + mvmtQryStr);
    Query mvmtQuery = OBDal.getInstance().getSession().createQuery(mvmtQryStr);
    List<String> mvmtList = mvmtQuery.list();
    return mvmtList;
  }

  public static void respForPOStatus(HashMap<String, String> soDocNoObj, Order ord) {
    try {
      String docNo = ord.getDocumentNo();
      String poDocNo = "";
      int totalSOLines = 0;
      int totalCompletedSOLines = 0;
      int totalsoLinesNotInTruck = 0;
      int totalmmlZeroSOLines = 0;

      if (docNo.length() > 0) {

        // get count of total SO Lines
        poDocNo = docNo.substring(0, docNo.lastIndexOf("*"));
        String qrySO = "select count(ol.id) from OrderLine ol join ol.salesOrder o where co.swPoReference = '"
            + poDocNo + "' and o.salesTransaction = 'Y'  and o.documentType.id = :docId ";
        log.debug("qry to retrieve SO " + qrySO);
        Query querySO = OBDal.getInstance().getSession().createQuery(qrySO);
        querySO.setParameter("docId", SOConstants.SODOCTYPEID);
        List<Long> totalSOLinesList = querySO.list();
        if (totalSOLinesList != null) {
          totalSOLines = totalSOLinesList.get(0).intValue();
        }

        // get count of SO Lines present in closed truck
        String shipCompleteQry = "select count(distinct col.id) from MaterialMgmtShipmentInOut mi "
            + " join mi.materialMgmtShipmentInOutLineList mil "
            + "right join mil.salesOrderLine col  join col.salesOrder co "
            + "where mi.documentStatus = 'CO'  and " + "co.swPoReference = '" + poDocNo
            + "' and co.salesTransaction = 'Y'  and co.documentType.id = :docId ";
        log.debug("retrive qry to get ol to check status " + shipCompleteQry);
        final Query soQry = OBDal.getInstance().getSession().createQuery(shipCompleteQry);
        soQry.setParameter("docId", SOConstants.SODOCTYPEID);
        List<Long> soQryList = soQry.list();
        if (soQryList != null) {
          totalCompletedSOLines = soQryList.get(0).intValue();
        }

        // get count of SO Lines present in not in truck
        String soLinesNotInTruckQry = "select count(distinct col.id) from MaterialMgmtShipmentInOut mi "
            + " join mi.materialMgmtShipmentInOutLineList mil "
            + "right join mil.salesOrderLine col join col.salesOrder co "
            + "where (mi.documentStatus != 'CO' or mi.documentStatus is null)  and "
            + "co.swPoReference = '"
            + poDocNo
            + "and co.salesTransaction = 'Y'  and co.documentType.id = :docId ";
        log.debug("retrive qry to get ol to check status " + shipCompleteQry);
        final Query soLinesNotInTruck = OBDal.getInstance().getSession()
            .createQuery(soLinesNotInTruckQry);
        soLinesNotInTruck.setParameter("docId", SOConstants.SODOCTYPEID);
        List<Long> soLinesNotInTruckList = soLinesNotInTruck.list();
        if (soLinesNotInTruck != null) {
          totalsoLinesNotInTruck = soLinesNotInTruckList.get(0).intValue();
        }

        // get count of so lines for which there is no movement line(incidence raised to 0 qty)
        String mmlZeroQry = "select count(distinct col.id) from MaterialMgmtInternalMovementLine ml "
            + " right join ml.salesOrderLine col join col.salesOrder co  where ml.id is null and  "
            + "co.documentNo like '"
            + poDocNo
            + "%' "
            + "and co.salesTransaction = 'Y'  and co.documentType.id = :docId";
        final Query mmlZero = OBDal.getInstance().getSession().createQuery(mmlZeroQry);
        mmlZero.setParameter("docId", SOConstants.SODOCTYPEID);
        List<Long> mmlZeroList = mmlZero.list();
        if (mmlZeroList != null) {
          totalmmlZeroSOLines = mmlZeroList.get(0).intValue();
        }

        if ((totalSOLines <= (totalCompletedSOLines + totalmmlZeroSOLines))
            && (totalsoLinesNotInTruck - totalmmlZeroSOLines) == 0) {
          soDocNoObj.put(poDocNo, SOConstants.closed);
        } else if ((totalSOLines >= (totalCompletedSOLines + totalmmlZeroSOLines) && (totalsoLinesNotInTruck - totalmmlZeroSOLines) > 0)
            && totalCompletedSOLines > 0) {
          soDocNoObj.put(poDocNo, SOConstants.partialRecieved);
        } else if (totalCompletedSOLines == 0) {
          soDocNoObj.put(poDocNo, "NA");
        } else {
          soDocNoObj.put(poDocNo, "NA");
        }
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("error in retrieving details for updating PO status " + e);
    }

  }

}