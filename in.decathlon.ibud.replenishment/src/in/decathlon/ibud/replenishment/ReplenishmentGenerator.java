package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.masters.data.IbudServerTime;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;
import in.decathlon.supply.dc.data.IDSDOxylaneProdCategory;
import in.decathlon.supply.dc.data.IDSDPCBThreshold;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;

import com.sysfore.catalog.CLMinmax;
import com.sysfore.catalog.CLModel;

public class ReplenishmentGenerator {

  static final Logger log = Logger.getLogger(ReplenishmentGenerator.class);
  private static final BigDecimal HUNDRED_BIG_DECIMAL = new BigDecimal(100);
  static ReplenishmentDalUtils createBookPO = new ReplenishmentDalUtils();
  static boolean header = false;

  /**
   * @param org
   *          - Store Organization for which this process is being executed
   * @param isSpoon
   *          - whether the process is running in Spoon Mode or not
   * @param isHourly
   *          - whether the process is getting incremental ( hourly ) replenishments or not ( end of
   *          day )
   * @param isDrCreateProc
   *          TODO
   * @param processId
   *          TODO
   * @return - returns the message for logging
   */
  @SuppressWarnings( { "unchecked" })
  public String replenishPurchaseOrders(Organization org, boolean isSpoon, boolean isHourly,
      boolean isDrCreateProc, String processId) throws Exception {
    Date lastRun = getLastRun();
    setLastRun();

    String result = "";

    try {
      Warehouse wr = getPriorityWarehouse(org);
      Sequence docSeq = getDocSequence(org);

      String replenishmentPOs = "";
      String orgId = org.getId();
      Query query = getReplenishmentQuery(isSpoon, lastRun, orgId, isHourly);

      log.info("executing query " + query);
      Date startQueryRun = new Date();

      List<Object[]> qryResult = query.list();
      Date endQueryRun = new Date();
      log.debug(", " + SOConstants.perfOrmanceEnhanced + " query Execution completed, "
          + org.getName() + "," + startQueryRun + "," + endQueryRun + " , "
          + (endQueryRun.getTime() - startQueryRun.getTime()) / 1000);
      log.info("Returned  " + qryResult.size() + " no of records");
      if (qryResult != null && qryResult.size() > 0) {
        long line = 0L;
        if (isDrCreateProc) {
          replenishmentPOs = createPurchaseOrderinDR(org, qryResult, wr, docSeq, line, isSpoon);
        } else {
          replenishmentPOs = createPurchaseOrder(org, qryResult, wr, docSeq, line, isSpoon,
              processId);
        }
      } else {
        result = result + "\n" + "Organization " + org.getName() + "'s warehouse " + wr.getName()
            + " has no PO's to be created";
        log.info("Organization " + org.getName() + "'s warehouse " + wr.getName()
            + " has no PO's to be created");
      }
      result = result + "\n" + replenishmentPOs + " PO's created for " + "Organization "
          + org.getName() + "'s warehouse " + wr.getName();
      log.info(replenishmentPOs + " PO's created for " + "Organization " + org.getName()
          + "'s warehouse " + wr.getName() + "and created SO for them");
    } catch (Exception e) {
      log.error(e);
      throw e;
    }
    return result;
  }

  private Query getReplenishmentQuery(boolean isSpoon, Date lastRun, String orgId, boolean isHourly) {
    String qry = "";

    if (!isHourly) {
      qry = "select mx.id as f0, m.id as f1, coalesce(mx.minQty,0) as f2,coalesce(mx.displaymin,0) as f3,  mx.maxQty as f4,strdept.name as f5,m.name as f6,  m.clModel.modelName as f7,  "
          + "ibdrep_get_stock(m.id, mx.organization.id) as f8, ibdrep_get_qty_on_order(m.id,mx.organization.id) as f9"
          + " ,m.clModel.id as f10, coalesce(m.clUeQty,0) as f11 "
          + " from CL_Minmax mx "
          + " join mx.product m  "
          + " join m.clModel.storeDepartment strdept  where mx.organization.id = '" + orgId + "'";
      if (isSpoon)
        qry = qry + " and mx.isinrange = 'Y' order by strdept.name ";
      else
        qry = qry + " and mx.isinrange = 'Y' order by strdept.name ";
    } else {

      qry = "select  distinct mx.id as f0, sd.product.id as f1, coalesce(mx.minQty,0) as f2, coalesce(mx.displaymin,0) as f3, mx.maxQty as f4, sd.product.clModel.storeDepartment.name as f5, "
          + " sd.product.name as f6, sd.product.clModel.modelName as f7,ibdrep_get_stock(sd.product.id, mx.organization.id) as f8, ibdrep_get_qty_on_order(sd.product.id,mx.organization.id) as f9,sd.product.clModel.id as f10, coalesce(m.clUeQty,0) as f11"
          + " from Organization o "
          + " join o.organizationWarehouseList w "
          + " join w.warehouse.locatorList l "
          + " join l.materialMgmtStorageDetailList sd "
          + " join sd.product.minmaxList mx "
          + " where  sd.organization.id='"
          + orgId
          + "' and mx.isinrange = 'Y' "
          + " and (mx.minQty > 0 or  coalesce(mx.displaymin,0) >0 ) "
          + " and mx.organization.id='"
          + orgId
          + "' and sd.updated > '"
          + lastRun
          + "' order by sd.product.clModel.storeDepartment.name ";
    }

    Query query = OBDal.getInstance().getSession().createQuery(qry);
    return query;
  }

  private static String createPurchaseOrder(Organization org, List<Object[]> qryResult,
      Warehouse wr, Sequence docSeq, long line, boolean isSpoon, String processId) throws Exception {
    boolean isImplantation = false;
    ImmediateSOonPO soOnPO = new ImmediateSOonPO();
    CSVUtil csvUtil = new CSVUtil();
    HashMap<Product, CSVData> csvData = new HashMap<Product, CSVData>();
    JSONObject errorObject = new JSONObject();
    try {

      ArrayList<String> pOids = new ArrayList<String>();
      String prevStoreDeptName = "";
      Order orderHeader = null;
      CLModel model = null;
      // replenishment booked and voided PO counts
      int booked = 0;
      int voided = 0;
      int draft = 0;
      String status = "";
      boolean askSupplyForStck = true;

      for (Object[] row : qryResult) {
        try {
          String productId, storeDeptName, modelId;
          long minQty = 0;
          long displayMin = 0;
          long maxQty = 0;
          long csvMinQty, csvMaxQty;
          BigDecimal qtyInStock = BigDecimal.ZERO;
          BigDecimal qtyInOrder = BigDecimal.ZERO;
          BigDecimal qtyToBeOrdered = BigDecimal.ZERO;

          String minmaxId = (String) row[0];
          productId = (String) row[1];
          minQty = (Long) row[2];
          displayMin = (Long) row[3];
          maxQty = (Long) row[4];
          csvMinQty = minQty;
          csvMaxQty = maxQty;
          storeDeptName = (String) row[5];
          String isPcb = "false";
          if (row[8] != null) {
            qtyInStock = (BigDecimal) row[8];
            // check of negative qty in stock
          }

          if (row[9] != null) {
            qtyInOrder = (BigDecimal) row[9];
            if (qtyInOrder.compareTo(BigDecimal.ZERO) <= 0)
              qtyInOrder = BigDecimal.ZERO;
          }
          modelId = (String) row[10];
          BigDecimal clUeQty = (BigDecimal) row[11];
          /*
           * if (clUeQty.compareTo(BigDecimal.ZERO) == 0) continue;
           */

          // common logic
          if (displayMin >= maxQty && displayMin >= 0) {
            minQty = maxQty = displayMin;
          } else if (displayMin >= minQty && (displayMin >= 0)) {
            minQty = displayMin;
          }

          // for spoon
          if (isSpoon) {
            if (qtyInStock.compareTo(new BigDecimal(maxQty)) <= 0
                && qtyInStock.compareTo(BigDecimal.ZERO) >= 0) {
              qtyToBeOrdered = new BigDecimal(maxQty - qtyInStock.longValue());
            }
          }

          // for regular
          else {
            if (qtyInStock.compareTo(new BigDecimal(minQty)) <= 0
                && qtyInStock.compareTo(BigDecimal.ZERO) >= 0) {
              qtyToBeOrdered = new BigDecimal(maxQty - qtyInStock.longValue());
            }
          }

          if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) <= 0) {
            qtyToBeOrdered = BigDecimal.ZERO;
          }

          BigDecimal reqQty = qtyToBeOrdered;
          qtyToBeOrdered = reqQty.subtract(qtyInOrder);

          model = OBDal.getInstance().get(CLModel.class, modelId);

          Product prd = OBDal.getInstance().get(Product.class, productId);
          CLMinmax minmax = OBDal.getInstance().get(CLMinmax.class, minmaxId);

          qtyToBeOrdered = getPCBorderedQuantity(prd, minmax, qtyToBeOrdered);

          if (clUeQty.compareTo(BigDecimal.ZERO) == 0)
            qtyToBeOrdered = BigDecimal.ZERO;

          if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) <= 0) {
            askSupplyForStck = false;
          }
          List<String> carCacStocks = csvUtil.setCarCacStock(prd, askSupplyForStck);
          askSupplyForStck = true;
          String storeStock = qtyInStock.toString();

          String rwhStock = carCacStocks.get(0);
          String cwhStock = carCacStocks.get(1);
          String hubStock = carCacStocks.get(2);
          String openOrder = qtyInOrder.toString();
          // get CSVData object which includes all data for a row
          isPcb = csvUtil.setisPcb(prd, org);
          CSVData csvrow = getARow(storeDeptName, prd.getName(), prd.getClModel().getModelName(),
              displayMin, csvMinQty, csvMaxQty, prd.getClUeQty(), prd.getClPcbQty(), prd
                  .getClLogRec(), storeStock, cwhStock, rwhStock, hubStock, reqQty, qtyToBeOrdered,
              openOrder, isPcb, isSpoon);

          csvData.put(prd, csvrow); // add it to Map which will refered further

          if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) <= 0)
            continue;

          if (!prevStoreDeptName.equals(storeDeptName)
              || orderHeader.getOrderLineList().size() >= 1000) {
            if (!prevStoreDeptName.equals("")) {
              if (orderHeader.getOrderLineList().size() > 0) {
                errorObject.put(SOConstants.recordIdentifier, orderHeader.getDocumentNo());
                try {
                  Date startProcess = new Date();
                  status = processOrder(docSeq, soOnPO, pOids, orderHeader);
                  Date endProcess = new Date();
                  log.debug(", " + SOConstants.perfOrmanceEnhanced + " Process Completed for , "
                      + orderHeader.getDocumentNo() + " , " + startProcess + " ," + endProcess
                      + " , " + (endProcess.getTime() - startProcess.getTime()) / 1000);
                  addConfirmedQty(orderHeader, csvData);
                } catch (Exception e) {
                  log.error(e);
                  status = "draft";
                  BusinessEntityMapper.rollBackNlogError(e, processId, errorObject);
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

            orderHeader = createBookPO.createPurchaseOrderHeader(org, wr, model, docSeq,
                isImplantation);

            prevStoreDeptName = storeDeptName;

          }
          createBookPO.createOrderLines(prd, qtyToBeOrdered, orderHeader, line);
        } catch (Exception e) {
          BusinessEntityMapper.rollBackNlogError(e, processId, errorObject);
          e.printStackTrace();
        }
      }
      if (orderHeader != null) {
        if (orderHeader.getOrderLineList().size() > 0) {
          try {
            status = processOrder(docSeq, soOnPO, pOids, orderHeader);
          } catch (Exception e) {
            status = "draft";
            BusinessEntityMapper.rollBackNlogError(e, processId, null);
          }
          if (status.equals("booked")) {
            booked += 1;
            addConfirmedQty(orderHeader, csvData);
          } else if (status.equals("voided")) {
            voided += 1;
          } else {
            draft += 1;
          }
        }
      }
      Date befCSV = new Date();
      Set<Product> mapEntries = csvData.keySet();

      // header for csv file
      List<String[]> repData = new ArrayList<String[]>();
      repData.add(new String[] { "Store Department", "Item Code", "Model Name", "Display Min",
          "Min", "Max", "UE", "PCB", "Logistic Recharge", "Store Stock", "CWH Stock", "RWH Stock",
          "Hub Stock", "Required Quantity", "OpenOrder", "Order Qty", "COnfirmed Qty",
          "PCB Picking", "isSpoon" });
      // add rows for CSV file
      for (Product prdKey : mapEntries) {
        CSVData d = csvData.get(prdKey);
        String ordQty = d.getOrdQty().compareTo(BigDecimal.ZERO) <= 0 ? "0" : d.getOrdQty()
            .toString();
        String reqQty = d.getReqQty().compareTo(BigDecimal.ZERO) <= 0 ? "0" : d.getReqQty()
            .toString();
        repData.add(new String[] { d.getDeptName(), d.getItemCode(), d.getModelName(),
            String.valueOf(d.getDisplayMin()), String.valueOf(d.getMinQty()),
            String.valueOf(d.getMaxQty()), d.getUeQty().toString(), d.getPcbQty().toString(),
            d.getLogRec() == null ? "0" : d.getLogRec(), d.getStoreStock(), d.getCarStock(),
            d.getCacStock(), d.getHubStock(), reqQty, d.getOpenOrder(), ordQty,
            String.valueOf(d.getConFirmedQty()), d.isPcb(), d.isSpoon() });
      }

      // write all rows to CSV file
      csvUtil.writeDataToCsv(org, repData);
      Date aftCSV = new Date();

      log.debug(", " + SOConstants.perfOrmanceEnhanced + " writing to CSV completed , "
          + orderHeader.getDocumentNo() + " , " + befCSV + " ," + aftCSV + " , "
          + (aftCSV.getTime() - befCSV.getTime()) / 1000);
      return booked + " booked and " + voided + " voided and " + draft + " draft ";

    } catch (Exception e) {
      // BusinessEntityMapper.logErrorRecord(e, processId, null);
      log.error(e);
      throw e;
    }
  }

  // this method updates confirmed Qty of CSVData object which will be need for CSV file.
  private static void addConfirmedQty(Order orderHeader, HashMap<Product, CSVData> csvData) {

    for (OrderLine ol : orderHeader.getOrderLineList()) {
      CSVData tempCsv = csvData.get(ol.getProduct());
      if(tempCsv ==null){
    	  throw new OBException("csv map object returns null for product: "+ol.getProduct());
      }
      tempCsv.setConFirmedQty(ol.getSwConfirmedqty());
      csvData.put(ol.getProduct(), tempCsv);
    }

  }

  private static CSVData getARow(String storeDeptName, String name, String modelName,
      long displayMin, long minQty, long maxQty, BigDecimal clUeQty, BigDecimal clPcbQty,
      String clLogRec, String storeStock, String cwhStock, String rwhStock, String hubStock,
      BigDecimal reqQty, BigDecimal qtyToBeOrdered, String openOrder, String isPcb, boolean isSpoon) {
    CSVData csvd = new CSVData();
    csvd.setDeptName(storeDeptName);
    csvd.setItemCode(name);
    csvd.setModelName(modelName);
    csvd.setDisplayMin(displayMin);
    csvd.setMinQty(minQty);
    csvd.setMaxQty(maxQty);
    csvd.setUeQty(clUeQty);
    csvd.setPcbQty(clPcbQty);
    csvd.setLogRec(clLogRec);
    csvd.setStoreStock(storeStock);
    csvd.setCarStock(cwhStock);
    csvd.setCacStock(rwhStock);
    csvd.setHubStock(hubStock);
    csvd.setReqQty(reqQty);
    csvd.setOrdQty(qtyToBeOrdered);
    csvd.setOpenOrder(openOrder);
    csvd.setPcb(isPcb);
    if (isSpoon)
      csvd.setSpoon("true");
    else
      csvd.setSpoon("false");
    return csvd;
  }

  private String createPurchaseOrderinDR(Organization org, List<Object[]> qryResult, Warehouse wr,
      Sequence docSeq, long line, boolean isSpoon) throws Exception {

    boolean isImplantation = false;

    try {

      String prevStoreDeptName = "";
      Order orderHeader = null;
      CLModel model = null;

      // replenishment PO created counts
      int poCreate = 0;

      for (Object[] row : qryResult) {
        String productId, storeDeptName, modelId;
        long minQty = 0;
        long displayMin = 0;
        long maxQty = 0;
        BigDecimal qtyInStock = BigDecimal.ZERO;
        BigDecimal qtyInOrder = BigDecimal.ZERO;
        BigDecimal qtyToBeOrdered = BigDecimal.ZERO;

        String minmaxId = (String) row[0];
        productId = (String) row[1];
        minQty = (Long) row[2];
        displayMin = (Long) row[3];
        maxQty = (Long) row[4];
        storeDeptName = (String) row[5];
        if (row[8] != null) {
          qtyInStock = (BigDecimal) row[8];
          // check of negative qty in stock
          if ((qtyInStock.compareTo(BigDecimal.ZERO) <= 0))
            qtyInStock = BigDecimal.ZERO;

        }

        if (row[9] != null) {
          qtyInOrder = (BigDecimal) row[9];
          if (qtyInOrder.compareTo(BigDecimal.ZERO) <= 0)
            qtyInOrder = BigDecimal.ZERO;
        }
        modelId = (String) row[10];
        BigDecimal clUeQty = (BigDecimal) row[11];
        if (clUeQty.compareTo(BigDecimal.ZERO) == 0)
          continue;

        // for spoon
        if (isSpoon) {
          if (displayMin > maxQty) {
            qtyToBeOrdered = new BigDecimal(displayMin);
          } else {
            qtyToBeOrdered = new BigDecimal(maxQty);
          }
        }

        // for regular
        else {
          if (displayMin > minQty) {
            qtyToBeOrdered = new BigDecimal(displayMin);
          } else {
            qtyToBeOrdered = new BigDecimal(minQty);
          }
        }

        qtyToBeOrdered = qtyToBeOrdered.subtract(qtyInOrder).subtract(qtyInStock);
        model = OBDal.getInstance().get(CLModel.class, modelId);

        Product prd = OBDal.getInstance().get(Product.class, productId);
        CLMinmax minmax = OBDal.getInstance().get(CLMinmax.class, minmaxId);

        qtyToBeOrdered = getPCBorderedQuantity(prd, minmax, qtyToBeOrdered);

        if (qtyToBeOrdered.compareTo(BigDecimal.ZERO) <= 0)
          continue;

        if (!prevStoreDeptName.equals(storeDeptName)
            || orderHeader.getOrderLineList().size() >= 1000) {
          if (!prevStoreDeptName.equals("")) {
            if (orderHeader.getOrderLineList().size() > 0) {
              savePO(orderHeader);
              poCreate++;
            }
          }
          log.info("creation of order for dept " + storeDeptName);
          orderHeader = createBookPO.createPurchaseOrderHeader(org, wr, model, docSeq,
              isImplantation);
          prevStoreDeptName = storeDeptName;

        }
        createBookPO.createOrderLines(prd, qtyToBeOrdered, orderHeader, line);
      }
      if (orderHeader != null) {
        if (orderHeader.getOrderLineList().size() > 0) {
          savePO(orderHeader);
          poCreate++;
        }
      }

      return poCreate + " draft PO ";

    } catch (Exception e) {
      throw e;
    }

  }

  @SuppressWarnings("unused")
  private static void addDataToCsv(Order orderHeader, CSVUtil csvUtil, boolean headerFlag)
      throws IOException {
    List<String[]> repData = new ArrayList<String[]>();
    if (headerFlag) {
      repData.addAll(csvUtil.getHeader());
      // header = true;
    }
    repData.addAll(csvUtil.createCsvData(orderHeader));
    csvUtil.writeDataToCsv(orderHeader.getOrganization(), repData);
  }

  private void savePO(Order orderHeader) {
    Sequence sequence = orderHeader.getDocumentType().getDocumentSequence();
    OBDal.getInstance().refresh(sequence);
    Long curDocNo = sequence.getNextAssignedNumber();
    sequence.setNextAssignedNumber(curDocNo + sequence.getIncrementBy());
    OBDal.getInstance().save(sequence);
    orderHeader.setDocumentNo(sequence.getPrefix() + curDocNo.toString());
    OBDal.getInstance().save(orderHeader);
    SessionHandler.getInstance().commitAndStart();

  }

  @SuppressWarnings("static-access")
  private static BigDecimal getPCBorderedQuantity(Product prd, CLMinmax minmax,
      BigDecimal qtyOrdered) throws Exception {
    try {
      BigDecimal qtyToBeOrdered = qtyOrdered;
      OBContext.getOBContext().setAdminMode(true);
      BigDecimal pcbQty = prd.getClPcbQty();
      BigDecimal ueQty = prd.getClUeQty() != null ? (prd.getClUeQty()) : (BigDecimal.ONE);
      IDSDOxylaneProdCategory oxylaneProdCat = prd.getIdsdOxylaneProdcat();
      String searchKeyOxylane = "";
      IDSDPCBThreshold pcbThreschold = minmax.getIdsdPcbThreshold();
      String searchKeyOfThreschold = "";
      BigDecimal threscholdQty = BigDecimal.ZERO;

      BigDecimal calUEReqQty = BigDecimal.ZERO;

      BigDecimal threscholdLimit = BigDecimal.ZERO;
      if (prd.getIdsdOxylaneProdcat() != null || minmax.getIdsdPcbThreshold() != null) {
        if (oxylaneProdCat != null) {
          searchKeyOxylane = oxylaneProdCat.getSearchKey();
          threscholdLimit = new BigDecimal(searchKeyOxylane);
        } else if (pcbThreschold != null) {
          searchKeyOfThreschold = pcbThreschold.getSearchKey();
          threscholdLimit = new BigDecimal(searchKeyOfThreschold);
        } else
          threscholdLimit = BigDecimal.ZERO;

        threscholdQty = (pcbQty.multiply(threscholdLimit)).divide(HUNDRED_BIG_DECIMAL);

        if (pcbQty.compareTo(BigDecimal.ZERO) == 1) {
          BigDecimal bg[] = qtyToBeOrdered.divideAndRemainder(pcbQty);

          BigDecimal quotient = qtyToBeOrdered.divide(pcbQty, 0, RoundingMode.CEILING);
          BigDecimal remainder = bg[1];

          if (remainder.compareTo(threscholdQty) == 1) {
            qtyToBeOrdered = quotient.multiply(pcbQty);
          }
        }
      } else {
        if (ueQty.compareTo(BigDecimal.ZERO) == 1) {
          calUEReqQty = qtyToBeOrdered.divide(ueQty, 0, RoundingMode.CEILING);

          qtyToBeOrdered = ueQty.multiply(calUEReqQty);
        }
      }
      return qtyToBeOrdered;
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.getOBContext().restorePreviousMode();
    }
  }

  public static String processOrder(Sequence docSeq, ImmediateSOonPO soOnPO,
      ArrayList<String> pOids, Order orderHeader) throws Exception {
    SessionHandler.getInstance().commitAndStart();
    Sequence sequence = orderHeader.getDocumentType().getDocumentSequence();
    OBDal.getInstance().refresh(sequence);
    Long curDocNo = sequence.getNextAssignedNumber();
    sequence.setNextAssignedNumber(curDocNo + sequence.getIncrementBy());
    OBDal.getInstance().save(sequence);
    orderHeader.setDocumentNo(sequence.getPrefix() + curDocNo.toString());
    OBDal.getInstance().save(orderHeader);
    SessionHandler.getInstance().commitAndStart();
    soOnPO.processRequest(orderHeader);
    OBDal.getInstance().save(orderHeader);
    OBDal.getInstance().flush(); // flushing to save above changes, since new ord ill be created
    if (orderHeader.getDocumentStatus().equals("CO")) {
      orderHeader.setIbdoCreateso(true); // setting button to true so that it should b invisible
      return "booked";
    } else {
      return "voided";
    }
  }

  public Date getLastRun() throws Exception {
    try {
      OBContext.setAdminMode(true);
      OBCriteria<IbudServerTime> serCrit = OBDal.getInstance().createCriteria(IbudServerTime.class);
      serCrit.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, "Replenishment"));
      serCrit.setMaxResults(1);
      if (serCrit.count() > 0) {
        return serCrit.list().get(0).getLastupdated();
      } else {
        return new Date();
      }
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void setLastRun() throws Exception {
    // set last run time
    try {
      OBContext.setAdminMode(true);
      IbudServerTime newService = OBProvider.getInstance().get(IbudServerTime.class);
      OBCriteria<IbudServerTime> serCrit = OBDal.getInstance().createCriteria(IbudServerTime.class);
      serCrit.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, "Replenishment"));
      serCrit.setMaxResults(1);
      if (serCrit.count() > 0) {
        newService = serCrit.list().get(0);

        newService.setLastupdated(new Date());
        OBDal.getInstance().save(newService);
      } else {
        // if record does not exist insert new record
        newService.setActive(true);
        newService.setClient(OBContext.getOBContext().getCurrentClient());
        newService.setOrganization(OBContext.getOBContext().getCurrentOrganization());
        newService.setCreatedBy(OBContext.getOBContext().getUser());
        newService.setCreationDate(new Date());
        newService.setUpdatedBy(OBContext.getOBContext().getUser());
        newService.setUpdated(new Date());
        newService.setNewOBObject(true);
        newService.setLastupdated(new Date());
        newService.setServiceKey("Replenishment");
        OBDal.getInstance().save(newService);
      }
    } catch (Exception e) {
      throw e;
    } finally {
      SessionHandler.getInstance().commitAndStart();
      OBContext.restorePreviousMode();
    }
  }

  public static Warehouse getPriorityWarehouse(Organization org) {

    return BusinessEntityMapper.getOrgWarehouse(org.getId()).getWarehouse();

  }

  public static Sequence getDocSequence(Organization org) {
    return BusinessEntityMapper.getDocumentSequence(org);

  }

}
