package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;
import in.decathlon.ibud.replenishment.data.Replenishment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLImplantation;
import com.sysfore.catalog.CLModel;

public class ImplantationProcess extends DalBaseProcess {
  static final Logger log = Logger.getLogger(ImplantationProcess.class);
  static ProcessLogger logger;
  static ReplenishmentDalUtils createBookPO = new ReplenishmentDalUtils();
  static ReplenishmentGenerator replGenerator = new ReplenishmentGenerator();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    String logmsg = "";
    try {
      List<Organization> orgList = createBookPO.getStoreOrganizations();

      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {

          if (spoonOrRegularCheckForOrg(org)) {
            OBCriteria<CLImplantation> implOrg = OBDal.getInstance().createCriteria(
                CLImplantation.class);
            implOrg.add(Restrictions.eq(CLImplantation.PROPERTY_STOREIMPLANTED, org));
            implOrg.add(Restrictions.eq(CLImplantation.PROPERTY_ISIMPLANTED, false));
            implOrg.setMaxResults(1);
            if (implOrg.count() >= 1) {

              Warehouse implWr = BusinessEntityMapper.getImplWarehouse(org).getWarehouse();
              Sequence docSeq = BusinessEntityMapper.getDocumentSequence(org);

              logmsg = logmsg + "\n" + implantationPO(org, implWr, docSeq, processid);

            }
          } else {
            logmsg = logmsg + "\n Failed to run implantation for " + org
                + " as replenishment process configured. \n";
          }
        }

      }
    }

    catch (Exception e) {
      log.error(e.getMessage(), e);
      OBDal.getInstance().rollbackAndClose();
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      // throw e;
    } finally {
      logger.log(logmsg);
    }

  }

  public boolean spoonOrRegularCheckForOrg(Organization org) {

    OBCriteria<Replenishment> replenishCrit = OBDal.getInstance().createCriteria(
        Replenishment.class);
    replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_ORGANIZATION, org));
    Criterion isSpoon = Restrictions.eq(Replenishment.PROPERTY_SPOON, true);
    Criterion isRegular = Restrictions.eq(Replenishment.PROPERTY_REGULAR, true);
    LogicalExpression orExp = Restrictions.or(isSpoon, isRegular);
    replenishCrit.add(orExp);
    replenishCrit.setMaxResults(1);
    /*
     * if (replenishCrit.count() >= 1) return false; else return true;
     */
    List<Replenishment> repList = replenishCrit.list();
    if (repList != null && repList.size() > 0) {
      return false;
    }
    return true;

  }

  @SuppressWarnings("unchecked")
  public String implantationPO(Organization org, Warehouse implWr, Sequence docSeq, String processid)
      throws Exception {
    String implantationPO = "";
    String result = "";
    try {
      // pOsToBeBooked = createImplantationOrder(org,implWr,docSeq);
      String qry = "select cli.product.id, cli.implantationQty, cli.bLOCKEDQTY ,strdept.name,m.name, m.clModel.modelName,m.clModel.id "
          + " from CL_Implantation cli  "
          + " join cli.product m "
          + " join m.clModel.brand strdept "
          + " join m.minmaxList mx with mx.organization.id = :orgId "
          + " where cli.storeImplanted = :orgId"
          + " and mx.isinrange = 'Y' and cli.implantationQty > cli.bLOCKEDQTY and cli.isimplanted = 'N' order by strdept.name";
      Query query = OBDal.getInstance().getSession().createQuery(qry);
      query.setParameter("orgId", org.getId());
      log.debug("Executing query " + qry);
      List<Object[]> qryList = query.list();

      if (qryList != null && qryList.size() > 0) {
        implantationPO = createImplantationOrder(org, implWr, docSeq, qryList, processid);
      }

      else {
        result = result + "\n Organization " + org.getName() + "'s warehouse " + implWr.getName()
            + " has no PO's to be created including and SO for them";
        log.info("Organization " + org.getName() + "'s warehouse " + implWr.getName()
            + " has no PO's to be created SO for them");
      }

      result = result + "\n" + implantationPO + " PO's created for " + "Organization "
          + org.getName() + "'s warehouse " + implWr.getName();
      log.info(implantationPO + " PO's created for " + "Organization " + org.getName()
          + "'s warehouse " + implWr.getName());

    } catch (Exception e) {
      log.error(e.getMessage(), e);

      result = result + e.toString();
      throw new Exception(result);
    }
    return result;
  }

  @SuppressWarnings("static-access")
  public String createImplantationOrder(Organization org, Warehouse implWr, Sequence docSeq,
      List<Object[]> qryList, String processid) throws Exception {
    JSONObject errorObject = new JSONObject();
    boolean isImplantation = true;
    ArrayList<String> pOids = new ArrayList<String>();
    String prevStoreDeptName = "";
    ImmediateSOonPO soOnPO = new ImmediateSOonPO();

    Long line = 0L;
    Order orderHeader = OBProvider.getInstance().get(Order.class);
    CLModel model = null;
    try {
      // Implantation booked and voided PO counts
      int booked = 0;
      int voided = 0;
      int draft = 0;
      String status = "";
      for (Object[] row : qryList) {
        String productId, storeDeptName, modelId;

        BigDecimal blockedQty = BigDecimal.ZERO;
        long implantedQty = 0;
        BigDecimal qtyToBeOrdered = BigDecimal.ZERO;

        productId = (String) row[0];
        implantedQty = (Long) row[1];
        blockedQty = (BigDecimal) row[2];
        storeDeptName = (String) row[3];
        modelId = (String) row[6];
        qtyToBeOrdered = BigDecimal.valueOf(implantedQty);
        qtyToBeOrdered = qtyToBeOrdered.subtract(blockedQty);
        if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) <= 0)
          continue;
        org.openbravo.model.common.plm.Product prd = OBDal.getInstance().get(
            org.openbravo.model.common.plm.Product.class, productId);
        model = OBDal.getInstance().get(CLModel.class, modelId);

        if (!prevStoreDeptName.equals(storeDeptName)
            || orderHeader.getOrderLineList().size() >= 100) {
          if (!prevStoreDeptName.equals("")) {
            if (orderHeader.getOrderLineList().size() > 0) {
              errorObject.put(SOConstants.recordIdentifier, orderHeader.getDocumentNo());
              try {
                status = replGenerator.processOrder(docSeq, soOnPO, pOids, orderHeader);
              } catch (Exception e) {
                status = "draft";
                BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
              }
              if (status.equals("booked")) {
                booked += 1;
              } else if (status.equals("voided")) {
                voided += 1;
              } else {
                draft += 1;
              }
            }
          }
          log.info("creation of order for dept " + storeDeptName);

          orderHeader = createBookPO.createPurchaseOrderHeader(org, implWr, model, docSeq,
              isImplantation);

          prevStoreDeptName = storeDeptName;

        }
        createBookPO.createOrderLines(prd, qtyToBeOrdered, orderHeader, line);
      }
      if (orderHeader != null) {
        if (orderHeader.getOrderLineList().size() > 0) {
          try {
            status = replGenerator.processOrder(docSeq, soOnPO, pOids, orderHeader);
          } catch (Exception e) {
            status = "draft";
            BusinessEntityMapper.rollBackNlogError(e, processid, null);
          }
          if (status.equals("booked")) {
            booked += 1;
          } else if (status.equals("voided")) {
            voided += 1;
          } else {
            draft += 1;
          }
        }
      }
      return booked + " booked and " + voided + " voided and " + draft + " draft ";
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      logger.log(e.toString());
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  public static void setBlockedQty(OrderLine ol) throws Exception {
    try {/*
          * Iterator<String> keys = ol2.keys(); while (keys.hasNext()) { String key = keys.next();
          * OrderLine ol = OBDal.getInstance().get(OrderLine.class, key); long blockedQty = (long)
          * ol2.getInt(key); CLImplantation implRecord =
          * BusinessEntityMapper.getImplantationOrg(ol.getOrganization(), ol.getProduct()); if
          * (implRecord != null) { BigDecimal oldBlockedQty = implRecord.getBLOCKEDQTY(); BigDecimal
          * newBlockedQty = oldBlockedQty.add(BigDecimal.valueOf(blockedQty));
          * implRecord.setBLOCKEDQTY(oldBlockedQty.add(BigDecimal.valueOf(blockedQty))); if
          * (newBlockedQty == BigDecimal.valueOf(implRecord.getImplantationQty())) {
          * implRecord.setImplanted(true); } OBDal.getInstance().save(implRecord); }
          * 
          * }
          */
      Long blockedQty = ol.getSwConfirmedqty();
      CLImplantation implRecord = BusinessEntityMapper.getImplantationOrg(ol.getOrganization()
          .getId(), ol.getProduct().getId());
      if (implRecord != null) {
        BigDecimal oldBlockedQty = implRecord.getBLOCKEDQTY();
        BigDecimal newBlockedQty = oldBlockedQty.add(BigDecimal.valueOf(blockedQty));
        implRecord.setBLOCKEDQTY(oldBlockedQty.add(BigDecimal.valueOf(blockedQty)));
        int comparedValue = newBlockedQty.compareTo(BigDecimal.valueOf(implRecord
            .getImplantationQty()));
        if (comparedValue >= 0) {
          implRecord.setImplanted(true);
        } else {
          implRecord.setImplanted(false);
        }
        OBDal.getInstance().save(implRecord);
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
      throw e;
    }

  }

}