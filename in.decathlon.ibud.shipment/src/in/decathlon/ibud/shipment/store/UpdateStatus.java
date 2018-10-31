package in.decathlon.ibud.shipment.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.masters.data.IbudServerTime;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.client.SOConstants.Status;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class UpdateStatus extends DalBaseProcess {
  public static final Logger log = Logger.getLogger(ShipmentProcess.class);
  ProcessLogger logger;
  static JSONWebServiceInvocationHelper statusHandler = new JSONWebServiceInvocationHelper();
  ArrayList<String> failedDocs = null;
  JSONObject poJson = new JSONObject();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    String processid = bundle.getProcessId();
    try {
      failedDocs = new ArrayList<String>();
      logger = bundle.getLogger();
      JSONObject storeJson = new JSONObject();
      String duration = IbudConfig.getDuration();
      // Last updated time is set to start of the process
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      List<Organization> orgList = getNonWarehouseOrgnizations();
      log.debug("In doExecute() of UpdateStatus");
      logger.log("Start time of the process is : " + updated + "\n");
      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {
          BusinessPartner orgBp = null;
          try {
            orgBp = BusinessEntityMapper.getBPOfOrg(org.getId());
          } catch (Exception e) {
            logger.log("\n business partner not dfined for Organization hence skipping it "
                + org.getName());
            continue;
          }
          if (duration != null) {
            storeJson = statusHandler.sendGetrequest(true, "StatusUpdate",
                "in.decathlon.ibud.shipment.POStatusUpdateWS", "bPartnerId=" + orgBp.getId()
                    + "&duration=" + duration, processid, logger);
          } else {
            storeJson = statusHandler.sendGetrequest(true, "StatusUpdate",
                "in.decathlon.ibud.shipment.POStatusUpdateWS", "bPartnerId=" + orgBp.getId(),
                processid, logger);
          }
          logger.log("\n documents taken for org: " + org.getName() + " are " + storeJson);

          updatePOStatus(storeJson, processid);
          generateGRN(org.getId(), processid);
          SessionHandler.getInstance().commitAndClose();
          copyTruck(org.getId(), processid);
        }

        if (duration != null) {
          BusinessEntityMapper.setLastUpdatedTime(
              getDurationUpdatedTime("StatusUpdate", duration, date, updated), "StatusUpdate");
          BusinessEntityMapper.setLastUpdatedTime(
              getDurationUpdatedTime("StatusUpdate", duration, date, updated), "ShipmentInOut");
          BusinessEntityMapper.setLastUpdatedTime(
              getDurationUpdatedTime("StatusUpdate", duration, date, updated), "obwship");
        } else {
          BusinessEntityMapper.setLastUpdatedTime(updated, "StatusUpdate");
          BusinessEntityMapper.setLastUpdatedTime(updated, "ShipmentInOut");
          BusinessEntityMapper.setLastUpdatedTime(updated, "obwship");
        }
      }
      logger.log("Process Completed Successfully");
      if (!failedDocs.isEmpty()) {
        Iterator<String> itrFailedDocs = failedDocs.iterator();
        StringBuffer stringBuffer = new StringBuffer("\n Failed document numbers -");
        while (itrFailedDocs.hasNext()) {
          stringBuffer.append(" " + itrFailedDocs.next() + ",");
        }
        logger.log(stringBuffer.toString());
      }
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      log.error(e);
      logger.log(e.getMessage());
      throw e;
    }
  }

  private void copyTruck(String orgId, String processid) throws Exception {
    JSONObject errorObject = new JSONObject();
    errorObject.put(SOConstants.recordIdentifier, orgId);
    try {
      final JSONWebServiceInvocationHelper shippertHandler = new JSONWebServiceInvocationHelper();
      log.debug("Requesting supply to get shipment of document no starting with " + orgId);
      JSONObject jsonObj = shippertHandler.sendGetrequest(true, "obwship",
          "in.decathlon.ibud.shipment.CopyTruck", "orgId=" + orgId, processid, logger);
    }

    catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
      throw e;
    }
  }

  private String getDurationUpdatedTime(String serviceKey, String duration, Date date,
      String updated) throws ParseException {
    OBCriteria<IbudServerTime> ibudServerTimeCriteria = OBDal.getInstance().createCriteria(
        IbudServerTime.class);
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, serviceKey));
    ibudServerTimeCriteria.setMaxResults(1);
    List<IbudServerTime> ibudServerTimeList = ibudServerTimeCriteria.list();

    Date lastUpdatedTime = null;

    if (ibudServerTimeList != null && ibudServerTimeList.size() > 0) {
      log.debug("Time taken from database ibudServerTimeList.get(0).getLastupdated() "
          + ibudServerTimeList.get(0).getLastupdated());
      lastUpdatedTime = ibudServerTimeList.get(0).getLastupdated();
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(lastUpdatedTime);
      calendar.add(Calendar.MINUTE, Integer.parseInt(duration));
      Date intervalDate = calendar.getTime();
      if (date.after(intervalDate)) {
        return intervalDate.toString().replaceAll(" ", "_");
      } else {

        return updated;
      }
    }
    throw new OBException("Creted ibudServerTime manually for service " + serviceKey);
  }

  private void updatePOStatus(JSONObject storeJson, String processid) throws Exception {
    if (!storeJson.has("error")) {
      JSONArray closedArray = storeJson.getJSONArray("closed");
      JSONArray pickedArray = storeJson.getJSONArray("picked");
      JSONArray shippedArray = storeJson.getJSONArray("shipped");
      JSONArray parPickedArr = storeJson.getJSONArray("partialPicked");
      JSONArray parShippedArr = storeJson.getJSONArray("partialShiped");
      JSONArray voidedArray = storeJson.getJSONArray("voided");
      updateStorePOStatus(pickedArray, SOConstants.Status.IBDO_PK, processid);
      updateStorePOStatus(shippedArray, SOConstants.Status.IBDO_SH, processid);
      updateStorePOStatus(parPickedArr, SOConstants.Status.OBWPL_PPK, processid);
      updateStorePOStatus(parShippedArr, SOConstants.Status.IBDO_PSH, processid);
      updateVoidedStatus(voidedArray);
      updateCloseStatus(closedArray, processid);
      OBDal.getInstance().flush();
    } else {
      throw new OBException(storeJson.getString("error"));
    }

  }

  private void updateVoidedStatus(JSONArray voidedArray) throws Exception {
    try {
      for (int i = 0; i < voidedArray.length(); i++) {
        String poDoc = "";
        poDoc = voidedArray.getString(i);
        log.debug("getting PO document number of " + poDoc);
        OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
        ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, poDoc));
        ordCrit.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
        ordCrit.setMaxResults(1);
        List<Order> ordList = ordCrit.list();
        if (ordList.size() > 0) {
          Order ord = ordList.get(0);
          // if(ord.isSwIsimplantation()){
          ord.setDocumentStatus(SOConstants.voided);
          ord.setDocumentAction("--");
          OBDal.getInstance().save(ord);
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

  }

  private void updateCloseStatus(JSONArray closedArray, String processid) throws Exception {
    try {
      for (int i = 0; i < closedArray.length(); i++) {
        String poDoc = "";
        poDoc = closedArray.getString(i);
        log.debug("getting PO document number of " + poDoc);
        OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
        ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, poDoc));
        ordCrit.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
        ordCrit.setMaxResults(1);
        List<Order> ordList = ordCrit.list();
        if (ordList.size() > 0) {
          Order ord = ordList.get(0);
          if (ord.getDocumentStatus().equals(SOConstants.DraftDocumentStatus)) {
            ImmediateSOonPO poBook = new ImmediateSOonPO();
            poBook.processRequest(ord);
            if (ord.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
              ord.setIbdoCreateso(true);
              ord.setIbdoCreatingso(true);
              OBDal.getInstance().save(ord);
            }
          }
          SessionHandler.getInstance().commitAndStart();
          // if(ord.isSwIsimplantation()){
          Map<String, BigDecimal> inoutlineMap = new HashMap<String, BigDecimal>();
          ord.setDocumentStatus(SOConstants.closed);
          ord.setDocumentAction("--");
          OBDal.getInstance().save(ord);
          if (ord.isSwIsimplantation()) {
            setBlockedQtyForImpl(poDoc, ord, inoutlineMap);

          }

        }

      }
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

  }

  public static void setBlockedQtyForImpl(String poDoc, Order ord,
      Map<String, BigDecimal> inoutlineMap) {
    String productId;
    BigDecimal resultantValue;
    // ShipmentInOut grn = CreateGRNService.getGRN(salesOrder);
    List<String> grnList = CreateGRNService.getGRNs(poDoc);
    for (String grnId : grnList) {
      ShipmentInOut grn = OBDal.getInstance().get(ShipmentInOut.class, grnId);
      if (grn != null) {
        List<ShipmentInOutLine> inoutline = grn.getMaterialMgmtShipmentInOutLineList();
        if (inoutline != null) {
          for (ShipmentInOutLine line : inoutline) {
            productId = line.getProduct().getId();
            resultantValue = line.getMovementQuantity();
            if (inoutlineMap.containsKey(productId)) {
              BigDecimal previousValue = inoutlineMap.get(productId);
              if (previousValue != null) {
                resultantValue = resultantValue.add(previousValue);
              }
            }
            inoutlineMap.put(productId, resultantValue);
          }
        }
      }
    }

    CreateGRNService.setBlockedQtyandImplFlag(ord, inoutlineMap);
  }

  private void updateStorePOStatus(JSONArray docNoArray, Status ibdoPk, String processid)
      throws Exception {

    for (int i = 0; i < docNoArray.length(); i++) {
      String docNo = docNoArray.getString(i);
      try {
        OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
        ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, docNo));
        ordCrit.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
        // ordCrit.add(Restrictions.ne(Order.PROPERTY_DOCUMENTSTATUS, "DR"));
        ordCrit.add(Restrictions.ne(Order.PROPERTY_DOCUMENTSTATUS, "VO"));
        ordCrit.add(Restrictions.eq(Order.PROPERTY_ACTIVE, true));
        ordCrit.setMaxResults(1);
        List<Order> ordList = ordCrit.list();
        if (ordList.size() > 0) {
          Order ord = ordList.get(0);
          if (ord.getDocumentStatus().equals(SOConstants.DraftDocumentStatus)) {
            ImmediateSOonPO poBook = new ImmediateSOonPO();
            poBook.processRequest(ord);
            if (ord.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
              ord.setIbdoCreateso(true);
              ord.setIbdoCreatingso(true);
              OBDal.getInstance().save(ord);
            }
          }
          int statusInt = getCurrentStatusOrdinalValue(ord);
          if (statusInt < ibdoPk.ordinal()) {
            ord.setDocumentStatus(ibdoPk.toString());
          }
          OBDal.getInstance().save(ord);
        }

      } catch (Exception e) {
        log.error(e);
        failedDocs.add(docNo);
        throw e;
      }
    }
  }

  private int getCurrentStatusOrdinalValue(Order ord) {
    String docStatus = ord.getDocumentStatus();
    if (docStatus.equals(SOConstants.CompleteDocumentStatus))
      return 0;
    else if (docStatus.equals(SOConstants.partialPicked))
      return 1;
    else if (docStatus.equals(SOConstants.picked))
      return 2;
    else if (docStatus.equals(SOConstants.partialShipped))
      return 3;
    else if (docStatus.equals(SOConstants.shipped))
      return 4;
    else if (docStatus.equals(SOConstants.partialRecieved))
      return 5;
    return 6;
  }

  private void generateGRN(String orgId, String processid) throws Exception {
    JSONObject errorObject = new JSONObject();
    errorObject.put(SOConstants.recordIdentifier, orgId);
    try {
      final JSONWebServiceInvocationHelper shipmentHandler = new JSONWebServiceInvocationHelper();
      log.debug("Requesting supply to get shipment of document no starting with " + orgId);
      JSONObject jsonObj = shipmentHandler.sendGetrequest(true, "ShipmentInOut",
          "in.decathlon.ibud.shipment.ShipmentWS", "orgId=" + orgId, processid, logger);
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, errorObject);
      e.printStackTrace();
      log.error(e);
      logger.log(e.getMessage());
      throw e;
    }
  }

  private List<Organization> getNonWarehouseOrgnizations() throws JSONException {
    OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_SWISWAREHOUSE, false));
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
    orgCriteria.add(Restrictions.eq(Organization.PROPERTY_CLIENT, OBContext.getOBContext()
        .getCurrentClient()));
    orgCriteria.addOrderBy(Organization.PROPERTY_IBUDSPOSTATUSPRIORITY, true);
    List<Organization> orgList = orgCriteria.list();
    return orgList;
  }
}