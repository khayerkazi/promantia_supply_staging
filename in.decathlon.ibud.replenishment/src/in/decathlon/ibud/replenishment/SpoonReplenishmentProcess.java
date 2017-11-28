package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.replenishment.data.Replenishment;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLMinmax;
import com.sysfore.catalog.CLModel;

public class SpoonReplenishmentProcess extends DalBaseProcess {

  private static Logger log = Logger.getLogger(SpoonReplenishmentProcess.class);
  private static ProcessLogger logger;
  Calendar cal = Calendar.getInstance();
  ReplenishmentDalUtils createBookPO = new ReplenishmentDalUtils();
  SimpleDateFormat inputParser = new SimpleDateFormat("HH:mm");

  @Override
  protected void doExecute(ProcessBundle bundle) {
    logger = bundle.getLogger();

    try {
      List<Organization> orgList = createBookPO.getStoreOrganizations();

      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {
          if (eligibleForSpoon(org)) {
            Warehouse wr = getPriorityWarehouse(org);
            replenishPurchaseOrders(org, wr);
          }
        }
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
      logger.log(e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }
  }

  private Warehouse getPriorityWarehouse(Organization org) {

    return BusinessEntityMapper.getOrgWarehouse(org.getId()).getWarehouse();

  }

  private boolean eligibleForSpoon(Organization org) {

    OBCriteria<Replenishment> replenishCrit = OBDal.getInstance().createCriteria(
        Replenishment.class);
    replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_ORGANIZATION, org));
    replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_SPOON, true));
    replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_DAYOFWEEK,
        String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1)));

    List<Replenishment> replenishList = replenishCrit.list();
    boolean flag = false;
    if (replenishList != null && replenishList.size() > 0) {
      for (Replenishment rep : replenishList) {

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);

        int startMin = rep.getEndingTime().getMinutes() - 5;
        int startHour = rep.getEndingTime().getHours();
        if (startMin < 0) {
          startMin = 55;
          startHour = startHour - 1;
        }
        String tempTime = startHour + ":" + startMin;
        int endMin = rep.getEndingTime().getMinutes() + 5;
        String tempTime2 = rep.getEndingTime().getHours() + ":" + endMin;
        Date endTime = parseDate(tempTime2);
        Date startTime = parseDate(tempTime);
        Date currentTime = parseDate(hour + ":" + minute + ":" + sec);
        if (currentTime.after(startTime) && currentTime.before(endTime))
          flag = true;
      }

    }
    return flag;

  }

  private Date parseDate(String date) {

    try {
      return inputParser.parse(date);
    } catch (java.text.ParseException e) {
      return new Date(0);
    }
  }

  private void replenishPurchaseOrders(Organization org, Warehouse wr) throws Exception {
    try {
      Set<String> pOsToBeBooked = new HashSet<String>();
      String orgId = org.getId();
      String saleablebin = BusinessEntityMapper.getOrgWarehouse(orgId).getWarehouse().getName();
      String qry = "select m.id,m.maxQty-(sum(mg.quantityOnHand) +  sum(ol.swConfirmedqty - cast(ol.sWEMSwRecqty as integer))),dept.name,m.displaymin,m.minQty,stdept.name  ,e.clModel.id from Product e "
          + " join e.clModel.department dept "
          + " join e.clModel.storeDepartment stdept "
          + "join e.minmaxList m with m.organization.id = '"
          + orgId
          + "' "
          + "left join e.materialMgmtStorageDetailList mg with mg.organization.id = '"
          + orgId
          + "'  "
          + "left join e.orderLineList ol with ol.organization.id = '"
          + orgId
          + "' and ol.swConfirmedqty > cast (ol.sWEMSwRecqty as integer) where mg.storageBin.warehouse.name like '"
          + saleablebin
          + "' m.minQty > 0 "
          + "group by m.id,m.minQty,m.maxQty,dept.name,stdept.name having m.maxQty >= sum(coalesce(mg.quantityOnHand,0)) +  sum(coalesce(ol.swConfirmedqty,0) - coalesce(cast(ol.sWEMSwRecqty as integer),0)) order by stdept.name";

      Query query = OBDal.getInstance().getSession().createQuery(qry);
      List<Object[]> qryResult = query.list();

      if (qryResult != null && qryResult.size() > 0) {
        pOsToBeBooked = createPurchaseOrder(org, qryResult, wr);
      } else {
        logger.log("Organization " + org.getName() + "'s warehouse " + wr.getName()
            + " has no PO's to be created");
        log.info("Organization " + org.getName() + "'s warehouse " + wr.getName()
            + " has no PO's to be created");
      }
      if (pOsToBeBooked.size() > 0) {
        createBookPO.bookPO(pOsToBeBooked);
        logger.log(pOsToBeBooked.size() + " PO's created for " + "Organization " + org.getName()
            + "'s warehouse " + wr.getName());
        log.info(pOsToBeBooked.size() + " PO's created for " + "Organization " + org.getName()
            + "'s warehouse " + wr.getName());
      } else
        logger.log("There no POs to be booked");

    } catch (Exception e) {
      throw e;
    }
  }

  private Set<String> createPurchaseOrder(Organization org, List<Object[]> qryResult, Warehouse wr)
      throws Exception {
	  boolean isImplanted = false;
    try {
      Set<String> pOids = new HashSet<String>();
      String productId = "";
      String Department = "";
      String modelId = "";
      Order orderHeader = OBProvider.getInstance().get(Order.class);
      for (Object[] row : qryResult) {
        long displayMin = 0;
        long minQty = 0;
        BigDecimal maxQty = BigDecimal.ZERO;
        BigDecimal qtyToBeOrdered = BigDecimal.ZERO;
        CLModel model = null;
        String minMaxId = (String) row[0];
        if (row[1] != null)
          maxQty = (BigDecimal) row[1];

        if (row[3] != null)
          displayMin = (Long) row[3];

        if (row[4] != null)
          minQty = (Long) row[4];

        if (row[6] != null)
          modelId = (String) row[6];

        CLMinmax clMinmax = OBDal.getInstance().get(CLMinmax.class, minMaxId);

        if (clMinmax.isInrange()) {

          if (displayMin > maxQty.longValue())
            qtyToBeOrdered = new BigDecimal(displayMin);
          else if (displayMin < maxQty.longValue())
            qtyToBeOrdered = maxQty;

          log.info("minMaxId= " + minMaxId + " quantityOrder=" + qtyToBeOrdered);
          if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) == 0)
            continue;
          org.openbravo.model.common.plm.Product prd = OBDal.getInstance().get(
              org.openbravo.model.common.plm.Product.class, productId);

          if (!Department.equals((String) row[5])) {
            model = OBDal.getInstance().get(CLModel.class, modelId);
            Department = (String) row[5];
            String storeDept = (String) row[5];
            orderHeader = createBookPO.createPurchaseOrderHeader(org, wr, model, null, isImplanted);
            createBookPO.createOrderLines(prd, qtyToBeOrdered, orderHeader, 0L);
            pOids.add(orderHeader.getId());
          } else {
            if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) == 0)
              continue;
            createBookPO.createOrderLines(prd, qtyToBeOrdered, orderHeader, 0L);
          }
        } else
          continue;
      }

      return pOids;

    } catch (Exception e) {
      throw e;
    }
  }
}