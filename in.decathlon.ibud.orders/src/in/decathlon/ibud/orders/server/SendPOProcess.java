package in.decathlon.ibud.orders.server;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;
import in.decathlon.ibud.replenishment.CSVUtil;
import in.decathlon.ibud.replenishment.implantation.ImplantationProcessEnhanced;
import in.decathlon.ibud.replenishment.ReplenishmentDalUtils;

import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLMinmax;

public class SendPOProcess extends DalBaseProcess {

  private static final Logger log = Logger.getLogger(SendPOProcess.class);
  private ProcessLogger logger;
  static String userName = "";
  static String password = "";
  static String host = "";
  int port = 0;
  static String urlPO = "";
  static String context = "";
  int rowCount = 0;
  String carWarehouseId = "962B547DC7D9431EABDE91E3A9BCFFF6";
  String cacWarehouseId = "67951CEE618E42E99F4D97C24534CDC1";
  String hubWarehouseId = "57E2E0F65EDE4344B4DDB1BEB54D0589";
  String wsName = "in.decathlon.ibud.orders.OrdersWS";
  Map<OrderLine, BigDecimal> remainingProducts = new HashMap<OrderLine, BigDecimal>();
  ReplenishmentDalUtils remainingPO = new ReplenishmentDalUtils();
  CSVUtil csvUtil = new CSVUtil();
  List<String[]> repData = new ArrayList<String[]>();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    init();
    try {
      final CreateJsonPO createJsonPO = new CreateJsonPO();
      final ImmediateSOonPO immediateSOonPO = new ImmediateSOonPO();
      logger = bundle.getLogger();
      String orgName = "";
      boolean result = false;
      List<Order> purchasedOrders = createJsonPO.getPurchaseOrders(false, "", rowCount);
      for (Order order : purchasedOrders) {

        if (!orgName.equals(order.getOrganization().getName())) {
          if (!orgName.equals("")) {
            csvUtil.writeDataToCsv(order.getOrganization(), repData);
            repData.clear();
          }
          orgName = order.getOrganization().getName();
          repData.add(new String[] { "Store Department", "Item Code", "Model Name", "Display Min",
              "Min", "Max", "UE", "PCB", "Logistic Recharge", "Store Stock", "CWH Stock",
              "RWH Stock", "Hub Stock", "Required Quantity", "Order Generated", "Open Order",
              "Pcb picking" });
          result = immediateSOonPO.processRequest(order);
          if (result) {
            for (OrderLine ol : order.getOrderLineList()) {
              Product prd = ol.getProduct();
              List<String> carCacStocks = setCarCacStock(prd);
              List<String> minMaxDisp = getMinMaxDisplayQty(prd, order.getOrganization());
              String storeStock = getQtyOnStock(order.getOrganization(), prd);
              String minQty = minMaxDisp.get(0);
              String maxQty = minMaxDisp.get(1);
              String dispMinMax = minMaxDisp.get(2);
              String rwhStock = carCacStocks.get(0);
              String cwhStock = carCacStocks.get(1);
              String hubStock = carCacStocks.get(2);

              String isPcb = setisPcb(prd);
              String openOrder = getConfirmedQty(order.getOrganization(), prd);
              repData.add(new String[] { order.getClStoredept().getName(), prd.getName(),
                  prd.getClModel().getModelName(), dispMinMax, minQty, maxQty,
                  prd.getClUeQty() != null ? prd.getClUeQty().toString() : "0",
                  prd.getClPcbQty() != null ? prd.getClPcbQty().toString() : "0",
                  prd.getClLogRec() != null ? prd.getClLogRec() : "", storeStock, cwhStock,
                  rwhStock, hubStock, ol.getOrderedQuantity().toString(),
                  ol.getSwConfirmedqty().toString(), openOrder, isPcb });
            }
            logger.log("\n Success for: " + order.getDocumentNo() + "   ");
          }

        } else {
          result = immediateSOonPO.processRequest(order);
          orgName = order.getOrganization().getName();
          if (result) {
            for (OrderLine ol : order.getOrderLineList()) {
              Product prd = ol.getProduct();
              List<String> carCacStocks = setCarCacStock(prd);
              List<String> minMaxDisp = getMinMaxDisplayQty(prd, order.getOrganization());
              String storeStock = getQtyOnStock(order.getOrganization(), prd);
              String minQty = minMaxDisp.get(0);
              String maxQty = minMaxDisp.get(1);
              String dispMinMax = minMaxDisp.get(2);
              String rwhStock = carCacStocks.get(0);
              String cwhStock = carCacStocks.get(1);
              String hubStock = carCacStocks.get(2);

              String isPcb = setisPcb(prd);
              String openOrder = getConfirmedQty(order.getOrganization(), prd);
              repData.add(new String[] { order.getClStoredept().getName(), prd.getName(),
                  prd.getClModel().getModelName(), dispMinMax, minQty, maxQty,
                  prd.getClUeQty() != null ? prd.getClUeQty().toString() : "0",
                  prd.getClPcbQty() != null ? prd.getClPcbQty().toString() : "0",
                  prd.getClLogRec() != null ? prd.getClLogRec() : "", storeStock, cwhStock,
                  rwhStock, hubStock, ol.getOrderedQuantity().toString(),
                  ol.getSwConfirmedqty().toString(), openOrder, isPcb });
            }
            logger.log("\n Success for: " + order.getDocumentNo() + "   ");
          } else {
            logger.log("\n failed for: " + order.getDocumentNo() + "   ");
          }
        }

      }
      logger.log("\n \n***Process Completed***");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
      logger.log(e.getMessage());
      throw e;
    }

  }

  private String getQtyOnStock(Organization organization, Product prd) {
    OrgWarehouse orgWarehouse = BusinessEntityMapper.getOrgWarehouse(organization.getId());
    String warehouseId = orgWarehouse.getWarehouse().getId();
    return BusinessEntityMapper.getStockOnWarehouse(prd.getId(), warehouseId).toString();
  }

  private String getConfirmedQty(Organization organization, Product prd) {
    String confirmedQty = "";
    try {
      String qry = "select sum(ol.swConfirmedqty) from OrderLine ol "
          + "join ol.salesOrder o where ol.product.id = '" + prd.getId()
          + "' and ol.organization.id = '" + organization.getId()
          + "' and o.documentStatus not in ('DR','VO','CL')";
      org.hibernate.Query qryCrit = OBDal.getInstance().getSession().createQuery(qry);
      List<Object[]> qryResult = qryCrit.list();
      if (qryResult != null && qryResult.size() > 0) {
        for (Object[] row : qryResult) {
          confirmedQty = (String) row[0];
        }
      } else
        confirmedQty = "NA";
    } catch (Exception e) {
      confirmedQty = "NA";
    }

    return confirmedQty;
  }

  @SuppressWarnings("static-access")
  private String setisPcb(Product prd) {
    String thresholdValue = "";
    try {
      OBContext.getOBContext().setAdminMode();
      thresholdValue = prd.getIdsdOxylaneProdcat() == null ? "" : prd.getIdsdOxylaneProdcat()
          .getSearchKey();
    } catch (Exception e) {
    } finally {
      OBContext.getOBContext().restorePreviousMode();
    }
    if (!thresholdValue.equals("")) {
      return "true";
    } else {
      return "false";
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> setCarCacStock(Product prd) {
    List<String> stocks = new ArrayList<String>();
    try {
      JSONArray responseObj = csvUtil
          .sendGetrequest("in.decathlon.ibud.replenishment.ibudStockWS?product=" + prd.getId()
              + "&warehouse=" + cacWarehouseId + "," + carWarehouseId + "");
      JSONObject obj = responseObj.getJSONObject(0);
      Iterator<String> keys = obj.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (key.contains(cacWarehouseId))
          stocks.add(obj.getString(key));
        else
          stocks.add(obj.getString(key));
      }
    } catch (Exception exc) {
      stocks.add("NA");
      stocks.add("NA");
      log.error(exc.getMessage());
    }
    return stocks;
  }

  private List<String> getMinMaxDisplayQty(Product prd, Organization organization) {
    List<String> clMinMax = new ArrayList<String>();
    OBCriteria<CLMinmax> minMaxCrit = OBDal.getInstance().createCriteria(CLMinmax.class);
    minMaxCrit.add(Restrictions.eq(CLMinmax.PROPERTY_ORGANIZATION, organization));
    minMaxCrit.add(Restrictions.eq(CLMinmax.PROPERTY_PRODUCT, prd));
    minMaxCrit.setMaxResults(1);
    List<CLMinmax> minMaxList = minMaxCrit.list();
    if (minMaxList != null && minMaxList.size() > 0) {
      clMinMax.add(minMaxList.get(0).getMinQty().toString());
      clMinMax.add(minMaxList.get(0).getMaxQty().toString());
      clMinMax.add(minMaxList.get(0).getDisplaymin().toString());
    } else {
      clMinMax.add("notFOund");
      clMinMax.add("notFOund");
      clMinMax.add("notFOund");
    }
    return clMinMax;

  }

  @SuppressWarnings("unchecked")
  public boolean sendOrdersAndProcessRequest(JSONObject orders) throws Exception {
    boolean flag = true;
    try {
      log.debug(",, " + SOConstants.performanceTest + " before sending to supply,, " + new Date());
      // log.debug("*********** order converted into json object **********" + orders);
      JSONObject docNos = send(orders.toString());
      log.debug(",, " + SOConstants.performanceTest + " got response at ,, " + new Date());
      log.debug(", got respose at" + new Date() + " response object, " + docNos);

      Iterator<String> keys = docNos.keys();
      String orderIDs = "";
      Order purOrd = null;
      String documentNumbers = "";
      if (docNos.has("result")) {
        if ("success".equals(docNos.getString("result"))) {
          return true;
        }
      }
      while (keys.hasNext()) {
        String key = keys.next();
        if ("PurchaseOrderID".equals(key)) {
          purOrd = OBDal.getInstance().get(Order.class, docNos.getString(key));
        } else if ("DocumentNumbers".equals(key)) {
          documentNumbers = docNos.getString(key);
        } else {
          orderIDs = orderIDs + key + ",";
        }
      }
      purOrd.setSwPoReference(documentNumbers);

      int numberOfSO = 0;
      JSONArray jsonArr = new JSONArray();

      if (!orderIDs.equals("")) {
        orderIDs = orderIDs.substring(0, orderIDs.length() - 1);
        jsonArr = sendGetrequest(orderIDs);
        if (jsonArr.getJSONObject(0).has("Error")) {
          throw new Exception(jsonArr.getJSONObject(0).getString("Error"));
        }
        numberOfSO = jsonArr.getJSONObject(0).length();
      }

      if (numberOfSO == 0) {
        JSONObject ordObj = (JSONObject) orders.getJSONArray("data").get(0);
        JSONObject ordHeader = (JSONObject) ordObj.get("Header");
        String ordId = ordHeader.getString("id");
        Order purchaseOrder = OBDal.getInstance().get(Order.class, ordId);

        purchaseOrder.setOrderReference(null);
        purchaseOrder.setDocumentStatus("VO");
        OBDal.getInstance().save(purchaseOrder);
        return false;
      }
      flag = setConfirmedQty(jsonArr, flag);
    } catch (Exception e) {
      log.error(e.getMessage());
      flag = false;
      throw new OBException(e);
    }
    return flag;
  }

  @SuppressWarnings("static-access")
  private JSONArray sendGetrequest(String orderIDs) throws Exception {
    // TO DO - sendGetRequest method merge with JSONWebServiceInvocationHelper.java
    JSONArray obj = new JSONArray();
    try {

      String url = "";
      IbudConfig config = new IbudConfig();

      userName = config.getSupplyUsername();
      password = config.getSupplyPassword();
      host = config.getSupplyHost();
      port = config.getSupplyPort();

      url = config.getSupplyServer();
      context = config.getSupplyContext().concat("/ws/");

      url = "http://" + host + ":" + port + "/" + context + wsName;

      log.info("UserName=" + userName + " and Password=" + password);
      url = url + "?OrderIDs=" + orderIDs;
      log.info("Final Url is " + url);
      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, password.toCharArray());
        }
      });

      DefaultHttpClient httpclient = new DefaultHttpClient();

      httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(userName, password));

      HttpGet httpGet = new HttpGet(url);

      HttpResponse response = httpclient.execute(httpGet);

      java.io.InputStream s = null;
      try {

        {
          String output = EntityUtils.toString(response.getEntity());
          log.info("Response from Supply " + output);
          obj = new JSONArray(output);

        }

      } catch (Exception e) {
        throw e;
      } finally {
        if (s != null)
          s.close();
      }
    } catch (Exception exc) {
      throw exc;
    }

    return obj;

  }

  public JSONObject send(String content) throws Exception {
    // TO DO - sendGetRequest method merge with JSONWebServiceInvocationHelper.java
    try {

      init();
      urlPO = "http://" + host + ":" + port + "/" + context + wsName;

      log.debug("User Name is *" + userName + "* and password is *" + password + "*");
      log.debug("Url is " + urlPO);
      log.debug("Host is " + host + " post is " + port);

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, password.toCharArray());
        }
      });

      DefaultHttpClient httpclient = new DefaultHttpClient();

      httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(userName, password));

      log.debug("final url =" + urlPO);
      HttpPost httpPost = new HttpPost(urlPO);
      log.debug("URL is " + urlPO);

      ByteArrayEntity b = new ByteArrayEntity(content.getBytes());
      httpPost.setEntity(b);

      HttpResponse response = httpclient.execute(httpPost);

      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        log.debug("response is " + response.getHeaders("orders"));
        Header[] headrs = response.getAllHeaders();
        for (Header hd : headrs) {
          if (hd.getName().equals("orders")) {
            return new JSONObject(hd.getValue());
          }
        }
        return new JSONObject();
      }

      else {
        log.error(response.getStatusLine());
        Header[] headrs = response.getAllHeaders();
        for (Header hd : headrs) {
          if (hd.getName().equals("Error")) {
            throw new Exception(hd.getValue());
          }
        }
        throw new OBException(response.getStatusLine().toString());

      }
    } catch (Exception e) {
      JSONObject order = new JSONObject(content);
      String orderId = order.getJSONArray("data").getJSONObject(0).getJSONObject("Header")
          .getString("id");
      log.debug("Making it back to draft since supply is down");
      // BusinessEntityMapper.setPOBackToDraft(orderId);
      SessionHandler.getInstance().commitAndStart();
      log.error(e.getMessage(), e);
      throw new OBException(e.getMessage());
    }

  }

  @SuppressWarnings("static-access")
  private void init() {
    IbudConfig config = new IbudConfig();
    userName = config.getSupplyUsername();
    password = config.getSupplyPassword();
    host = config.getSupplyHost();
    port = config.getSupplyPort();
    rowCount = Integer.parseInt(config.getSupplyRowCount());
    context = config.getSupplyContext().concat("/ws/");
  }

  @SuppressWarnings("unchecked")
  private boolean setConfirmedQty(JSONArray docNos, boolean flag) throws Exception {
    for (short i = 0; i < docNos.length(); i++) {
      JSONObject orderObject = docNos.getJSONObject(i);
      Iterator<String> keys = orderObject.keys();
      if (keys.hasNext()) {
        JSONObject jsonObject = orderObject.getJSONObject("lines");
        setConfirmedQtyForLines(jsonObject);
        Iterator<String> keysObj = jsonObject.keys();

        while (keysObj.hasNext()) {
          String olkey = keysObj.next();
          OrderLine ol = OBDal.getInstance().get(OrderLine.class, olkey);
          if (ol.getSalesOrder().isSwIsimplantation()) {
        	  ImplantationProcessEnhanced.setBlockedQty(ol);
          }
        }
      }
    }
    return flag;
  }

  @SuppressWarnings("unchecked")
  private void setConfirmedQtyForLines(JSONObject jsonObject) throws Exception {
    try {
      Iterator<String> keys = jsonObject.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        OrderLine ol = OBDal.getInstance().get(OrderLine.class, key);
        long confirmedQty = (long) jsonObject.getInt(key);
        BigDecimal qtyOrdered = ol.getOrderedQuantity();
        BigDecimal remainingQty = new BigDecimal(confirmedQty - qtyOrdered.longValue()).abs();
        ol.setSwConfirmedqty(confirmedQty);
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
          remainingProducts.put(ol, remainingQty);
        }
        OBDal.getInstance().save(ol);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }
}