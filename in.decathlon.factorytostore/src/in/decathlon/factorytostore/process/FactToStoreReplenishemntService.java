package in.decathlon.factorytostore.process;

import in.decathlon.factorytostore.data.DirectDelOrderReturn;
import in.decathlon.factorytostore.data.StockCount;
import in.decathlon.factorytostore.data.WarehouseCount;
import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.replenishment.ReplenishmentDalUtils;
import in.decathlon.ibud.replenishment.bulk.MinMaxComputed;
import in.decathlon.ibud.replenishment.bulk.ReplenishmentTypeEnum;
import in.decathlon.ibud.replenishment.bulk.ReplenishmentUtils;
import in.decathlon.ibud.replenishment.bulk.SequenceDao;
import in.decathlon.ibud.replenishment.data.Replenishment;
import in.decathlon.supply.dc.util.AutoDCMails;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessLogger;

import au.com.bytecode.opencsv.CSVWriter;

import com.sysfore.catalog.CLBrand;
import com.sysfore.catalog.CLMinmax;

public class FactToStoreReplenishemntService {

  private ProcessLogger logger;
  static String userName = "";
  static String password = "";
  static String host = "";
  int port = 0;
  static String urlPO = "";
  static String context = "";
  String wsName = "in.decathlon.factorytostore.EmailListener";

  private static final Logger LOG = Logger.getLogger(FactToStoreReplenishemntService.class);
  private ReplenishmentDAODirect dao = new ReplenishmentDAODirect();
  private DirectDelSupplyWs directDelSupplyWs = new DirectDelSupplyWs();
  private final static Properties p = SuppyDCUtil.getInstance().getProperties(
      SOConstants.MODULE_NAME_FTS);
  Map<String, WarehouseCount> finalWarehouseStock = new HashMap<String, WarehouseCount>();

  public void executeReplenishment() {

    LOG.info("Starting autoreplenishment process");
    List<Organization> orgList = ReplenishmentDalUtils.getStoreOrganizations();

    if (orgList == null || orgList.size() == 0) {
      return;
    }

    Map<String, Long> qtyPerStores = new HashMap<String, Long>();
    for (Organization org : orgList) {
      try {
        planReplenishmentFor(org, qtyPerStores);
      } catch (Exception e) {
        LOG.error(e);
      }
    }
    try {
      send();
    } catch (Exception e) {
      LOG.error(e);
    }

    // sendFinalMail();
  }

  public void planReplenishmentFor(Organization org, Map<String, Long> qtyPerStores) {

    long startTime = System.currentTimeMillis();

    // Compute replenishment data (which and how much product we need to order)
    Map<String, MinMaxComputed> computed = computeReplenishment(org);

    long middleTime = System.currentTimeMillis();
    LOG.info("Computation finished for the store " + org.getName() + " in "
        + ((middleTime - startTime) / 1000) + " s");

    Map<CLBrand, List<Order>> orders = createOrder(org);

    // commit session and reattach object to get data.
    SessionHandler.getInstance().commitAndStart();

    // Reattach object
    for (List<Order> ods : orders.values()) {
      dao.refreshOrder(ods);
    }
    List<Order> pos = new ArrayList<Order>();
    for (List<Order> ods : orders.values()) {
      for (Order od : ods) {
        pos.add(od);
      }
    }

    // Send orders to supply
    try {
      DirectDelOrderReturn directDelOrderReturn = directDelSupplyWs.processRequest(pos, computed);
      setFinalStock(directDelOrderReturn);
      if (getEmailStore(org) != null && getEmailStore(org).size() > 0) {
        sendMailResultWarehouse(org, qtyPerStores, computed, directDelOrderReturn);
      }
    } catch (Exception e) {
      new OBException(e);
    }
    long endTime = System.currentTimeMillis();
    LOG.info("Replenishment finished for the store " + org.getName() + " [from the start ] in "
        + ((endTime - startTime) / 1000) + " s");

  }

  private void setFinalStock(DirectDelOrderReturn directDelOrderReturn) {
    Map<String, WarehouseCount> supplyWarehouseStock = directDelOrderReturn.getWarehouseStocks();
    Set<String> supplyPrdKeySet = supplyWarehouseStock.keySet();
    for (String prodKey : supplyPrdKeySet) {
      if (finalWarehouseStock.containsKey(prodKey)) {
        WarehouseCount warehouseCount = finalWarehouseStock.get(prodKey);
        HashMap<String, StockCount> supplyStockCounts = supplyWarehouseStock.get(prodKey)
            .getStockCounts();
        Set<String> warehouseKeySet = supplyStockCounts.keySet();
        for (String warehouse : warehouseKeySet) {
          warehouseCount.getStockCounts().put(warehouse, supplyStockCounts.get(warehouse));
        }
      } else {
        finalWarehouseStock.put(prodKey, supplyWarehouseStock.get(prodKey));
      }
    }
  }

  /**
   * Create all order from replenishment.
   * 
   * @param org
   */
  private Map<CLBrand, List<Order>> createOrder(Organization org) {
    // Replenishment data OK, now get the data.
    Map<String, Integer> mapCatNbProd = dao.getNumberProductToOrder();

    Map<CLBrand, List<Order>> orders = createOrders(org, mapCatNbProd);
    for (List<Order> ods : orders.values()) {
      dao.saveOrder(ods);
    }

    dao.flush();

    dao.addOrderId(orders);
    dao.associateOrderWithLine();
    dao.flush();

    return orders;
  }

  /**
   * Create all the order object from the
   * 
   * @param org
   *          organisation
   * @param mapCatNbProd
   *          map of storedept / nb of product to order
   * @return map of store dept / list of order object (not persisted)
   */
  private Map<CLBrand, List<Order>> createOrders(Organization org, Map<String, Integer> mapCatNbProd) {
    String partnerId = BusinessEntityMapper.getSupplyBPartner(org);
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, partnerId);
    if (bp == null) {
      throw new OBException("No business Partner for the supply Organization");
    }

    User openBravoUser = OBDal.getInstance().get(User.class, SOConstants.UserId);
    Warehouse wr = BusinessEntityMapper.getOrgWarehouse(org.getId()).getWarehouse();
    Client dsi = ReplenishmentDalUtils.getClient(SOConstants.Client);
    DocumentType transactionDocTyp = BusinessEntityMapper.getTrasactionDocumentType(org);
    PriceList pricelist = BusinessEntityMapper.getPriceList(SOConstants.POPriceList);
    FIN_PaymentMethod paymentMethod = ReplenishmentDalUtils
        .getPaymentMethod(SOConstants.PaymentMethod);
    PaymentTerm paymentTerm = ReplenishmentDalUtils.getPaymentTerm(SOConstants.PaymentTerm);

    Map<CLBrand, List<Order>> orders = new HashMap<CLBrand, List<Order>>();

    Sequence sequence = transactionDocTyp.getDocumentSequence();
    int nextNumSeq = orderNumSeqBulk(mapCatNbProd, sequence);

    for (Entry<String, Integer> catNbProduct : mapCatNbProd.entrySet()) {
      CLBrand storeDept = OBDal.getInstance().get(CLBrand.class, catNbProduct.getKey());
      List<Order> oderForDept = new ArrayList<Order>();
      orders.put(storeDept, oderForDept);
      for (int i = 0; i <= (catNbProduct.getValue() / ReplenishmentUtils.NB_PRODUCT_PER_ORDER); i++) {
        String docNo = sequence.getPrefix() + nextNumSeq;
        nextNumSeq++;

        Order created = createOrder(docNo, openBravoUser, org, bp, wr, dsi, storeDept,
            transactionDocTyp, pricelist, paymentMethod, paymentTerm);

        oderForDept.add(created);
      }
    }

    return orders;
  }

  /**
   * Create one order
   * 
   * @param org
   *          organisation
   * @param bp
   *          business partner
   * @param wr
   *          warehouse
   * @param dsi
   *          client
   * @param storeDept
   *          store department
   * @return a newly created order object (not persisted in db)
   */
  private Order createOrder(String docNo, User openBravoUser, Organization org, BusinessPartner bp,
      Warehouse wr, Client dsi, CLBrand storeDept, DocumentType transactionDocTyp,
      PriceList pricelist, FIN_PaymentMethod paymentMethod, PaymentTerm paymentTerm) {

    Order newOrder = OBProvider.getInstance().get(Order.class);
    newOrder.setNewOBObject(true);
    newOrder.setClient(OBContext.getOBContext().getCurrentClient());
    newOrder.setOrganization(org);
    newOrder.setActive(true);
    newOrder.setCreationDate(new Date());
    newOrder.setCreatedBy(openBravoUser);
    newOrder.setUpdatedBy(openBravoUser);
    newOrder.setUpdated(new Date());
    newOrder.setBusinessPartner(bp);
    newOrder.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
    newOrder.setPaymentMethod(paymentMethod);
    newOrder.setPaymentTerms(paymentTerm);
    newOrder.setOrderDate(new Date());
    newOrder.setScheduledDeliveryDate(new Date());
    newOrder.setDocumentStatus(SOConstants.DraftDocumentStatus);
    newOrder.setProcessed(false);
    newOrder.setClBrand(storeDept);
    newOrder.setTransactionDocument(transactionDocTyp);
    newOrder.setSwIsautoOrder(true);
    newOrder.setPriceList(pricelist);
    newOrder.setDocumentType(transactionDocTyp);
    newOrder.setAccountingDate(new Date());
    newOrder.setSalesTransaction(false);
    newOrder.setCurrency(dsi.getCurrency());
    newOrder.setWarehouse(wr);
    newOrder.setSwIsimplantation(false);
    newOrder.setDocumentNo(docNo);
    newOrder.setFacstIsDirectDelivery(true);

    return newOrder;
  }

  private int orderNumSeqBulk(Map<String, Integer> mapCatNbProd, Sequence sequence) {
    int nbOfOrder = 0;
    for (Integer nbProd : mapCatNbProd.values()) {
      nbOfOrder += (nbProd / ReplenishmentUtils.NB_PRODUCT_PER_ORDER) + 1;
    }

    SequenceDao seqDao = new SequenceDao();
    return seqDao.getSeqNumAndAddNumberInNewTransaction(sequence, nbOfOrder);
  }

  /**
   * Build the replenishment data
   * 
   * @param org
   * @param type
   */
  private Map<String, MinMaxComputed> computeReplenishment(Organization org) {
    ReplenishmentTypeEnum type = null;
    dao.createTemporaryTable();
    dao.insertIntoTableProductConfiguration(org);
    dao.computeStock(org);
    dao.computeNeededQuantity(type);
    dao.computInOrderQuantity(org);
    dao.computeToBeOrdered();
    dao.roundQtytoPCB();
    dao.roundQtytoUE();
    dao.computeRank();
    dao.computePrice(org);

    return dao.getComputed(type == ReplenishmentTypeEnum.SPOON);
  }

  private void sendMailResultWarehouse(Organization org, Map<String, Long> qtyPerStores,
      Map<String, MinMaxComputed> computed, DirectDelOrderReturn directDelOrderDetails)
      throws Exception {
    if (!p.getProperty("isMail").equals("TRUE")) {
      return;
    }

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
    final String date = format.format(new Date());
    long qtyConfirmed = 0;

    File f = new File(IbudConfig.getcsvFile() + org.getName() + "_" + date + ".csv");
    try {
      if (!f.exists()) {
        f.createNewFile();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(f, true));

      writer
          .writeNext(new String[] { "Brand", "Item Code", "Model Name", "Display Min", "Min",
              "Max", "UE", "PCB", "Logistic Recharge", "Store Stock", "Supplier Stock",
              "CWH Stock", "Required Quantity", "Open Order", "Order Generated",
              "Confirmed Quantity", "Pcb picking" });

      Set<MinMaxComputed> s = new TreeSet<MinMaxComputed>(new Comparator<MinMaxComputed>() {

        @Override
        public int compare(MinMaxComputed o1, MinMaxComputed o2) {
          int i = o1.getDeptName().compareTo(o2.getDeptName());
          if (i != 0)
            return i;
          i = o1.getModelName().compareTo(o2.getModelName());
          if (i != 0)
            return i;
          return o1.getProductId().compareTo(o2.getProductId());
        }

      });
      s.addAll(computed.values());

      for (MinMaxComputed mmc : s) {
        String supplierStock;
        String cacStock;
        supplierStock = cacStock = "NA";
        int qtyOrdered = 0;

        Map<String, WarehouseCount> supplyWarehouseStock = directDelOrderDetails
            .getWarehouseStocks();
        if (supplyWarehouseStock.containsKey(mmc.getProductId())) {
          WarehouseCount warehouseCCount = supplyWarehouseStock.get(mmc.getProductId());
          Map<String, StockCount> skt = warehouseCCount.getStockCounts();

          StockCount st1 = skt.get("CacStock");
          StockCount st4 = skt.get("SupplierStock");

          cacStock = st1.getAvailableStock().toString();
          supplierStock = st4.getAvailableStock().toString();
          qtyOrdered = mmc.getToBeOrderedQty();
        }

        writer.writeNext(new String[] { mmc.getDeptName(), mmc.getProductName(),
            mmc.getModelName(), "" + mmc.getDisplayMin(), "" + mmc.getMin(), "" + mmc.getMax(),
            "" + mmc.getUeQty(), "" + mmc.getPcbQty(), "" + mmc.getLogRec(),
            "" + mmc.getStoreStock(), "" + supplierStock, "" + cacStock,
            "" + "" + mmc.getRequiredQty(), "" + "" + mmc.getQtyalreadyOrdered(), "" + qtyOrdered,
            "" + mmc.getValidatedQty(), "" + "True" });
        qtyConfirmed += mmc.getValidatedQty();
      }

      writer.close();
    } catch (IOException e) {
      LOG.warn("Cannot write CSV report...", e);
      return;
    }

    qtyPerStores.put(org.getDescription(), qtyConfirmed);

    AutoDCMails.getInstance().simplySendWarehouseMail(org, f, qtyConfirmed);

  }

  /**
   * find the replenishment type
   * 
   * @param org
   *          stores
   * @param now
   *          the current date
   * @return replenishment type
   */
  public ReplenishmentTypeEnum findReplenishmentType(Organization org, Date now) {
    Calendar cal = Calendar.getInstance();
    OBCriteria<Replenishment> replenishCrit = OBDal.getInstance().createCriteria(
        Replenishment.class);
    replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_ORGANIZATION, org));

    // replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_SPOON, isSpoon ? true : false));
    // replenishCrit.add(Restrictions.eq(Replenishment.PROPERTY_REGULAR, isSpoon ? false : true));
    List<Replenishment> replenishList = replenishCrit.list();

    ReplenishmentTypeEnum type = ReplenishmentTypeEnum.NONE;

    for (Replenishment rep : replenishList) {
      if (rep.getDayofweek() != null) {
        String dayOfWeek = (String.valueOf(cal.get(Calendar.DAY_OF_WEEK) - 1));
        if (!dayOfWeek.equals(rep.getDayofweek())) {
          continue;
        }
      }

      if (ReplenishmentUtils.timeCompare(now, rep.getStartingTime(), rep.getEndingTime())) {
        if (rep.isSpoon()) {
          return ReplenishmentTypeEnum.SPOON;
        }
        if (rep.isRegular()) {
          type = ReplenishmentTypeEnum.REGULAR;
        }
      }
    }
    return type;

  }

  @SuppressWarnings("static-access")
  private void init() {
    IbudConfig config = new IbudConfig();
    userName = config.getSupplyUsername();
    password = config.getSupplyPassword();
    host = config.getSupplyHost();
    port = config.getSupplyPort();
    // rowCount = Integer.parseInt(config.getSupplyRowCount());
    context = config.getSupplyContext().concat("/ws/");
  }

  public void send() throws Exception {
    // TO DO - sendGetRequest method merge with JSONWebServiceInvocationHelper.java
    try {

      init();
      urlPO = "http://" + host + ":" + port + "/" + context + wsName;

      LOG.debug("User Name is *" + userName + "* and password is *" + password + "*");
      LOG.debug("Url is " + urlPO);
      LOG.debug("Host is " + host + " port is " + port);

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, password.toCharArray());
        }
      });

      DefaultHttpClient httpclient = new DefaultHttpClient();

      httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(userName, password));

      LOG.debug("final url =" + urlPO);
      HttpPost httpPost = new HttpPost(urlPO);
      LOG.debug("URL is " + urlPO);

      // ByteArrayEntity b = new ByteArrayEntity(content.getBytes());
      // httpPost.setEntity(b);

      HttpResponse response = httpclient.execute(httpPost);

      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        LOG.debug("Final Mail Sent");
      }
    } catch (Exception e) {
      // JSONObject order = new JSONObject(content);
      // String orderId =
      // order.getJSONArray("data").getJSONObject(0).getJSONObject("Header").getString("id");
      LOG.debug("Making it back to draft since supply is down");
      // BusinessEntityMapper.setPOBackToDraft(orderId);
      SessionHandler.getInstance().commitAndStart();
      LOG.error(e.getMessage(), e);
      throw new OBException(e.getMessage());
    }

  }

  public static List<CLMinmax> getEmailStore(Organization org) {
    OBCriteria<CLMinmax> minMaxList = OBDal.getInstance().createCriteria(CLMinmax.class);
    minMaxList.add(Restrictions.eq(CLMinmax.PROPERTY_ORGANIZATION, org));
    minMaxList.add(Restrictions.eq(CLMinmax.PROPERTY_FACSTISDIRECTDELIVERY, true));
    minMaxList.add(Restrictions.eq(CLMinmax.PROPERTY_ISINRANGE, true));
    minMaxList.setMaxResults(1);
    List<CLMinmax> orgMinMaxList = minMaxList.list();
    return orgMinMaxList;
  }

}
