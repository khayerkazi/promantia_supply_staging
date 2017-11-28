package in.decathlon.supply.dc;

import in.decathlon.integration.PassiveDB;
import in.decathlon.supply.dc.util.AutoDCMails;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.web.WebService;

import au.com.bytecode.opencsv.CSVWriter;

import com.sysfore.catalog.CLStoreDept;

public class StoreReplenishmentWFS implements WebService {

  final static Properties p = SuppyDCUtil.getInstance().getProperties();
  Connection conn = null;
  private static final String LOGIN = p.getProperty("OBUsername");
  private static final String PWD = p.getProperty("OBpassword");

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

    final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

    if (p.size() < 0) {
      // Response
      final String objectToReturn = "{ staus: 'ko', response: 'Configuration are not found'}";
      response.setContentType("application/json");
      // Get the printwriter object from response to write the required json object to the output
      // stream
      PrintWriter out = response.getWriter();
      // Assuming your json object is **jsonObject**, perform the following, it will return your
      // json object
      out.print(objectToReturn);
      out.flush();
    } else {

      // Get the storedepartments
      final List<String> stores = new ArrayList<String>();
      final OBCriteria<CLStoreDept> activeStores = OBDal.getInstance().createCriteria(
          CLStoreDept.class);
      System.out.println(activeStores.count());
      if (activeStores.count() > 0) {
        int i = 0;
        final ScrollableResults scroller = activeStores.scroll(ScrollMode.FORWARD_ONLY);
        // loop through storedepartments
        while (scroller.next()) {
          final CLStoreDept oneStore = (CLStoreDept) scroller.get()[0];
          stores.add(oneStore.getId());
          // clear the session every 100 records
          if ((i % 100) == 0) {
            OBDal.getInstance().getSession().clear();
          }
          i++;
        }
      }

      final Map<String, Long> orgs = new HashMap<String, Long>();
      // Organiztion wise
      final OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(
          Organization.class);
      orgCriteria.add(Restrictions.eq(Organization.PROPERTY_DSIDEFISAUTODC, true));
      System.out.println("Orgs " + orgCriteria.count());
      if (orgCriteria.count() > 0) {
        int i = 0;
        final List<Organization> orgList = orgCriteria.list();
        for (Organization org : orgList) {
          String finename = p.getProperty("csvPath") + org.getSearchKey() + "-"
              + dt.format(new Date()) + ".csv";
          // csv file
          final File file = new File(finename);
          // if file doesnt exists, then create it
          if (!file.exists()) {
            file.createNewFile();
          }
          final CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile()));

          // CSV Headings
          List<String[]> data = new ArrayList<String[]>();
          data.add(new String[] { "Store Department", "Item Code", "Model Name",
              "Implantation Quantity", "Display Min", "Min", "Max", "UE", "PCB",
              "Logistic Recharge", "Store Stock", "WH Stock", "Required Quantity",
              "Already Blocked Quantity", "Order Generated", "PCB Picking", "Threshold Value" });

          writer.writeAll(data);
          // writer.close();

          System.out.println("Running for org " + org.getName());
          for (String clStoreDeptId : stores) {
            System.out.println("Running for store " + clStoreDeptId);
            if (p.getProperty("UsePassiveDB").equals("Y")) {
              conn = PassiveDB.getInstance().getConnection();
            } else {
              conn = OBDal.getInstance().getConnection();
            }
            final StringBuilder locatorWhere = new StringBuilder();
            locatorWhere.append("as l where l.searchKey like 'Saleable%' and organization='"
                + org.getId() + "'");
            final OBQuery<Locator> locObQuery = OBDal.getInstance().createQuery(Locator.class,
                locatorWhere.toString());
            final Locator loc = locObQuery.list().get(0);

            // Create PO DC according to minmax
            createPODC(clStoreDeptId, org.getId(), loc.getId(), loc.getWarehouse().getId(), org
                .getSearchKey(), p.getProperty("warehouseOrgId"), conn, writer);
          }
          writer.close();
          if (p.getProperty("isMail").equals("true")) {
            orgs.put(org.getDescription(), AutoDCMails.getInstance().sendMailToWarehouse(
org.getName(), org.getId(), file));
          }
          // clear the session every 100 records
          if ((i % 100) == 0) {
            OBDal.getInstance().getSession().clear();
          }
          i++;
        }
        /*
         * final ScrollableResults scroller = orgCriteria.scroll(ScrollMode.FORWARD_ONLY); while
         * (scroller.next()) { OBDal.getInstance().getSession(); final Organization org =
         * (Organization) scroller.get()[0];
         * 
         * }
         */
      }
      System.out.println("Map " + orgs);
      if (p.getProperty("isMail").equals("true")) {
        AutoDCMails.getInstance().mainMailToAll(orgs);
      }

      // Response
      final String objectToReturn = "{ staus: 'ok', code: '200'}";
      response.setContentType("application/json");
      // Get the printwriter object from response to write the required json object to the output
      // stream
      PrintWriter out = response.getWriter();
      // Assuming your json object is **jsonObject**, perform the following, it will return your
      // json object
      out.print(objectToReturn);
      out.flush();
    }
  }

  private void createPODC(String storeDeptId, String storeOrgId, String storeLocator,
      String storeWarehouse, String storeZone, String warehouse, Connection conn, CSVWriter writer)
      throws SQLException, ClassNotFoundException {
    // TODO Auto-generated method stub

    List<String[]> data = new ArrayList<String[]>();

    String product_id = "";
    Long line = 0L;

    final List<AutoDCProductDTO> products = getProductByDept(storeDeptId, storeOrgId, storeLocator,
        conn);
    System.out.println("No of products by query " + products.size());
    final List<SupplyDCOrderLinesDTO> linesData = new ArrayList<SupplyDCOrderLinesDTO>();

    for (AutoDCProductDTO pd : products) {

      Long impQty = 0L;
      Long blockedQty = 0L;

      Long minqty, maxqty, storestock, whstock, pcbqty, ue = 0L;
      Long alShipQty, reqQty, impQtylog, reqQtyBystock = 0L;

      product_id = pd.getProductId();
      ue = pd.getUeQty();
      minqty = pd.getMinQty();
      maxqty = pd.getMaxQty();
      storestock = pd.getStoreStock();
      whstock = pd.getWhStock();
      pcbqty = pd.getPcbQty();

      // check implantation
      Map<String, Long> implantation = implantationQty(product_id, storeOrgId, conn);
      if (implantation.size() > 0) {
        impQty = implantation.get("impqty");
        blockedQty = implantation.get("blockdqty");
        impQty = impQty - blockedQty;
      }
      if (impQty > 0) {
        reqQty = impQty;
        alShipQty = blockedQty;
        updateBlockedQty(product_id, storeOrgId, (reqQty + blockedQty), conn);
      } else {
        Long dispMin = pd.getDisplaymin();
        if ((dispMin > 0) && (dispMin >= maxqty)) {
          minqty = maxqty = dispMin;
        } else if ((dispMin > minqty) && (dispMin > 0)) {
          minqty = dispMin;
        }
        reqQty = claculateReqQty(product_id, ue, minqty, maxqty, storestock, storeZone);
        // reduce already shipped qty
        if (reqQty > 0) {
          alShipQty = qtyAlreadyShipped(product_id, storeOrgId, conn);
          if (alShipQty > 0) {
            reqQty = reqQty - alShipQty;
          }
        }

      }
      // check reservation
      if (reqQty > 0) {
        reqQtyBystock = reqQty;
        if (reqQtyBystock < 0) {
          reqQtyBystock = 0L;
        }
      }
      if (reqQty > 0) {
        if (pd.isPcb() == true) {
          System.out.println("isPCb " + pd.isPcb() + " reqQty before threshold" + reqQty);
          reqQty = calculateReqQtyByThresholdValue(pd.getPcbThresholdId(), reqQty, pd.getPcbQty(),
              pd.getIdsdProdCategoryId());
          System.out.println(" reqQty after threshold" + reqQty);
        }
        String cTaxId = getCtaxId(storeOrgId, product_id, conn);
        line = line + 10;
        final SupplyDCOrderLinesDTO suppyDCStoreLine = new SupplyDCOrderLinesDTO();
        suppyDCStoreLine.setStrdept_id(storeDeptId);
        suppyDCStoreLine.setProduct_id(product_id);
        suppyDCStoreLine.setReqqty(reqQty);
        suppyDCStoreLine.setBrand_id(pd.getBrandId());
        suppyDCStoreLine.setSize(pd.getSize());
        suppyDCStoreLine.setColor(pd.getColor());
        suppyDCStoreLine.setModelname(pd.getModelName());
        suppyDCStoreLine.setC_tax_id(cTaxId);
        suppyDCStoreLine.setOrgId(storeOrgId);
        suppyDCStoreLine.setLine(line);
        suppyDCStoreLine.setItemcode(pd.getItemCode());
        suppyDCStoreLine.setImpqty(impQty);
        linesData.add(suppyDCStoreLine);
      }
      final CLStoreDept clstore = OBDal.getInstance().get(CLStoreDept.class, storeDeptId);
      if (reqQty < 0) {
        reqQty = 0L;
      }
      String log_rech = "";
      if (pd.getLogRecharge().equals("145") || pd.getLogRecharge().equals("54")
          || pd.getLogRecharge().equals("null") || pd.getLogRecharge().equals("0")
          || pd.getLogRecharge().equals("") || (pd.getLogRecharge() == null)) {
        log_rech = "Non-Standard";
      } else if (pd.getLogRecharge().equals("9.7") || pd.getLogRecharge().equals("5")) {
        log_rech = "Standard";
      }
      data.add(new String[] { clstore.getName(), pd.getItemCode(), pd.getModelName(),
          impQty.toString(), pd.getDisplaymin().toString(), pd.getMinQty().toString(),
          pd.getMaxQty().toString(), pd.getUeQty().toString(), pd.getPcbQty().toString(), log_rech,
          pd.getStoreStock().toString(), pd.getWhStock().toString(), reqQtyBystock.toString(),
          blockedQty.toString(), reqQty.toString(), pd.isPcb() + "", pd.getThresholdValue() });
    }
    System.out.println("No of c_order lines " + linesData.size());
    writer.writeAll(data);
    if (!linesData.isEmpty()) {
      try {
        System.out.println("createPODC");
        createStoreReqHeader(storeDeptId, storeOrgId, false, storeWarehouse, linesData);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void updateBlockedQty(String productId, String storeOrgId, Long reqQty, Connection conn2) {

    Statement statement = null;
    String updateTableSQL = "UPDATE cl_implantation SET blocked_qty='" + reqQty.toString()
        + "' where " + "m_product_id='" + productId + "' and store_implanted='" + storeOrgId
        + "' and isimplanted = 'N'";
    try {
      statement = conn2.createStatement();
      System.out.println(updateTableSQL);
      // execute update SQL statement
      statement.execute(updateTableSQL);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
  }

  private Long calculateReqQtyByThresholdValue(String pcbThresholdId, Long reqQty, Long pcbQty,
      String productCategoryId) {
    // TODO Auto-generated method stub
    System.out.println("pcbQty " + pcbQty);
    PreparedStatement pst = null;
    ResultSet rs = null;
    Long thresholdValue = 0L;
    Long thresholdValueByProdCat = 0L;
    try {
      pst = conn.prepareStatement("select * from idsd_pcb_threshold where idsd_pcb_threshold_id='"
          + pcbThresholdId + "'");
      rs = pst.executeQuery();
      while (rs.next()) {
        thresholdValue = Long.parseLong(rs.getString("value"));
        System.out.println("thresholdValue " + thresholdValue);
      }
      if (thresholdValue == 0L) {
        try {
          pst = conn
              .prepareStatement("select * from idsd_oxylane_prodcategory where idsd_oxylane_prodcategory_id='"
                  + productCategoryId + "'");
          rs = pst.executeQuery();
          while (rs.next()) {
            thresholdValueByProdCat = Long.parseLong(rs.getString("value"));
            System.out.println("thresholdValueByProdCat " + thresholdValueByProdCat);
          }
        } catch (Exception e2) {
          e2.printStackTrace();
        }
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      try {
        if (pst != null && rs != null) {
          pst.close();
          rs.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (pcbQty > 0) {
      if (thresholdValue != 0L) {
        if (reqQty <= pcbQty) {
          if (thresholdValue > 0) {
            if ((((double) reqQty / pcbQty) * 100) > thresholdValue) {
              return pcbQty;
            } else {
              return reqQty;
            }
          } else {
            return reqQty;
          }
        } else {
          Long multiplier = (long) Math.floor(reqQty / pcbQty);
          System.out.println(multiplier);
          Long extraQty = reqQty - (pcbQty * multiplier);
          System.out.println(extraQty);
          if ((((double) extraQty / pcbQty) * 100) > thresholdValue) {
            multiplier++;
            System.out.println((pcbQty * multiplier));
            return (pcbQty * multiplier);
          } else {
            return reqQty;
          }
        }
      } else if (thresholdValueByProdCat != 0L) {
        if (reqQty <= pcbQty) {
          if (thresholdValueByProdCat > 0) {
            if ((((double) reqQty / pcbQty) * 100) > thresholdValueByProdCat) {
              return pcbQty;
            } else {
              return reqQty;
            }
          } else {
            return reqQty;
          }
        } else {
          Long multiplier = (long) Math.floor(reqQty / pcbQty);
          Long extraQty = reqQty - (pcbQty * multiplier);
          if ((((double) extraQty / pcbQty) * 100) > thresholdValueByProdCat) {
            multiplier++;
            return (pcbQty * multiplier);
          } else {
            return reqQty;
          }
        }
      } else if (thresholdValue == 0L) {
        Long multiplier = (long) Math.ceil(reqQty / pcbQty);
        return (multiplier * pcbQty);
      } else if (thresholdValueByProdCat == 0L) {
        Long multiplier = (long) Math.ceil(reqQty / pcbQty);
        return (multiplier * pcbQty);
      }
    }
    return reqQty;
  }

  private void createStoreReqLines(List<SupplyDCOrderLinesDTO> linesData, String storeWarehouseId,
      Order headerOrder) {

    int i = 0;
    // more elegant way
    for (SupplyDCOrderLinesDTO entry : linesData) {

      OrderLine newOrderLine = OBProvider.getInstance().get(OrderLine.class);
      newOrderLine.setClient(OBDal.getInstance().get(Client.class, p.getProperty("client")));
      newOrderLine.setOrganization(OBDal.getInstance().get(Organization.class, entry.getOrgId()));
      newOrderLine.setActive(true);
      newOrderLine.setCreatedBy(OBDal.getInstance().get(User.class, p.getProperty("createdBy")));
      newOrderLine.setUpdatedBy(OBDal.getInstance().get(User.class, p.getProperty("updatedBy")));
      newOrderLine.setLineNo(entry.getLine());
      newOrderLine.setProduct(OBDal.getInstance().get(Product.class, entry.getProduct_id()));
      newOrderLine.setOrderedQuantity(new BigDecimal(Long.parseLong(entry.getReqqty().toString())));
      newOrderLine.setUnitPrice(BigDecimal.ZERO);
      newOrderLine.setTax(OBDal.getInstance().get(TaxRate.class, entry.getC_tax_id()));
      newOrderLine.setFreightAmount(BigDecimal.ZERO);
      newOrderLine.setSalesOrder(headerOrder);
      newOrderLine.setListPrice(BigDecimal.ZERO);
      newOrderLine.setStandardPrice(BigDecimal.ZERO);
      newOrderLine.setPriceLimit(BigDecimal.ZERO);
      newOrderLine.setDiscount(BigDecimal.ZERO);
      newOrderLine.setUOM(OBDal.getInstance().get(UOM.class, "100"));
      newOrderLine.setOrderDate(new Timestamp(new Date().getTime()));
      newOrderLine.setWarehouse(OBDal.getInstance().get(Warehouse.class, storeWarehouseId));
      newOrderLine.setCurrency(OBDal.getInstance().get(Currency.class, p.getProperty("currency")));
      newOrderLine.setDescription("auto dc");
      newOrderLine.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
          p.getProperty("c_bpartner_id")));
      newOrderLine.setPartnerAddress(OBDal.getInstance().get(Location.class,
          p.getProperty("c_bpartner_location_id")));
      newOrderLine.setDSLotqty(null);
      newOrderLine.setDSLotprice(null);
      newOrderLine.setDSBoxqty(null);
      newOrderLine.setDSBoxprice(null);
      newOrderLine.setDSEMDSUnitQty(BigDecimal.ZERO);
      newOrderLine.setGrossListPrice(BigDecimal.ZERO);
      newOrderLine.setManageReservation(false);
      newOrderLine.setManagePrereservation(false);
      newOrderLine.setSWEMSwSuppliercode("IN000");
      try {
        OBDal.getInstance().save(newOrderLine);
        if (i % 100 == 0) {
          OBDal.getInstance().flush();
        }
      } catch (Exception e1) {
        e1.printStackTrace();
      }
      i++;
    }
  }

  private Order createStoreReqHeader(String storeDeptId, String orgId, boolean isImplantation,
      String storeWarehouseId, List<SupplyDCOrderLinesDTO> linesData) {

    Order newOrder = OBProvider.getInstance().get(Order.class);
    newOrder.setClient(OBDal.getInstance().get(Client.class, p.getProperty("client")));
    newOrder.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
    newOrder.setActive(true);
    newOrder.setCreatedBy(OBDal.getInstance().get(User.class, p.getProperty("createdBy")));
    newOrder.setUpdatedBy(OBDal.getInstance().get(User.class, p.getProperty("updatedBy")));
    newOrder.setTransactionDocument(OBDal.getInstance().get(DocumentType.class,
        p.getProperty("trxdoctype")));
    newOrder.setSalesTransaction(false);
    newOrder.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
        p.getProperty("c_bpartner_id")));
    newOrder.setInvoiceAddress(OBDal.getInstance().get(Location.class,
        p.getProperty("c_bpartner_location_id")));
    newOrder.setPartnerAddress(OBDal.getInstance().get(Location.class,
        p.getProperty("c_bpartner_location_id")));
    newOrder.setUserContact(OBDal.getInstance().get(User.class, p.getProperty("user")));
    newOrder.setDescription("AutoDC");
    newOrder.setPaymentTerms(OBDal.getInstance().get(PaymentTerm.class,
        p.getProperty("paymentterm")));
    newOrder.setPriceList(OBDal.getInstance().get(PriceList.class, p.getProperty("pricelist")));
    newOrder.setWarehouse(OBDal.getInstance().get(Warehouse.class, storeWarehouseId));
    newOrder.setClStoredept(OBDal.getInstance().get(CLStoreDept.class, storeDeptId));
    newOrder.setShippingCompany(null);
    newOrder.setSalesRepresentative(null);
    newOrder.setTrxOrganization(null);
    newOrder.setActivity(null);
    newOrder.setDocumentStatus("DR");
    newOrder.setDocumentAction("CO");
    newOrder.setDocumentType(OBDal.getInstance().get(DocumentType.class, p.getProperty("doctype")));
    newOrder.setOrderDate(new Timestamp(new Date().getTime()));
    newOrder.setScheduledDeliveryDate(new Timestamp(new Date().getTime()));
    newOrder.setAccountingDate(new Timestamp(new Date().getTime()));
    newOrder.setCurrency(OBDal.getInstance().get(Currency.class, p.getProperty("currency")));
    newOrder.setFormOfPayment("P");
    newOrder.setInvoiceTerms("I");
    newOrder.setDeliveryTerms("A");
    newOrder.setFreightCostRule("I");
    newOrder.setDeliveryMethod("P");
    newOrder.setPriority("5");
    newOrder.setPrintDiscount(false);
    newOrder.setProcessNow(false);
    newOrder.setDSReceiptno(null);
    newOrder.setDSEMDsRatesatisfaction(null);
    newOrder.setDSTotalpriceadj(null);
    newOrder.setDSPosno(null);
    newOrder.setChargeAmount(BigDecimal.ZERO);
    newOrder.setProcessed(false);
    newOrder.setSwIs2post(false);
    newOrder.setSwIsautoOrder(false);
    newOrder.setPriceIncludesTax(true);
    newOrder.setCopyFrom(false);
    newOrder.setGenerateTemplate(false);
    newOrder.setCopyFromPO(false);
    newOrder.setPickFromShipment(false);
    newOrder.setReceiveMaterials(false);
    newOrder.setCreateInvoice(false);
    newOrder.setAddOrphanLine(false);
    newOrder.setCalculatePromotions(false);
    newOrder.setSWEMSwPostatus("DR");
    newOrder.setSwSendorder(false);
    newOrder.setSwModorder(false);
    newOrder.setSwDelorder(false);
    newOrder.setObwplGeneratepicking(false);
    newOrder.setSwIsimplantation(isImplantation);
    newOrder.setIdsdIsautodc(true);
    try {
      OBDal.getInstance().save(newOrder);
      createStoreReqLines(linesData, storeWarehouseId, newOrder);
      OBDal.getInstance().commitAndClose();

    } catch (Exception e2) {
      e2.printStackTrace();
    }

    try {
      // HTTP POST request Parameters
      String urlParameter = "orderId=" + newOrder.getId() + "";

      // Sending HTTP POST request
      final HttpURLConnection hc = createConnection();
      hc.setDoOutput(true);
      final OutputStream os = hc.getOutputStream();
      os.write(urlParameter.getBytes("UTF-8"));
      os.flush();
      os.close();
      hc.connect();

      // Getting the Response from the Web service
      BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
      String inputLine;
      StringBuffer resp = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        resp.append(inputLine);
      }
      String secondResponse = resp.toString();
      in.close();

      System.err.println("received");

      if (secondResponse.equals("received")) {
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    /*
     * // Calling Stored Procedure ecom_order_post3 final List parameters = new ArrayList();
     * parameters.add(newOrder.getId()); final String procedureName =
     * "idsd_dc_post1";System.out.println("procedure "+newOrder.getId()); try {
     * CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
     * System.out.println("Procedure called successfully for the PO "+newOrder.getId()); } catch
     * (Exception e3) { e3.printStackTrace(); }
     */
    return newOrder;
  }

  // Get c_tax
  private String getCtaxId(String storeOrgId, String prodId, Connection conn) {
    PreparedStatement pst = null;
    ResultSet rs = null;
    String ctaxid = "";
    try {
      pst = conn
          .prepareStatement("select c_tax_id as c_tax from c_tax where c_region_id in (select l.c_region_id from ad_orginfo oi "
              + ", c_location l where oi.ad_org_id='"
              + storeOrgId
              + "' and oi.c_location_id=l.c_location_id) and c_tax.c_taxcategory_id = "
              + "(select c_taxcategory_id  from m_product where	m_product_id='"
              + prodId
              + "') union select c_tax.c_tax_id from "
              + "c_tax ,c_tax_zone where c_tax.c_tax_id = c_tax_zone.c_tax_id and c_tax_zone.from_region_id in "
              + "(select l.c_region_id from ad_orginfo oi, c_location l where oi.ad_org_id='"
              + storeOrgId
              + "' and oi.c_location_id=l.c_location_id) "
              + "and c_tax.c_taxcategory_id =(select c_taxcategory_id from m_product where m_product_id='"
              + prodId + "');");
      rs = pst.executeQuery();
      while (rs.next()) {
        ctaxid = rs.getString("c_tax");
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      try {
        if (pst != null && rs != null) {
          pst.close();
          rs.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return ctaxid;
    }
  }

  // Get c_tax
  /*
   * private Long getStock(String prodId, Connection conn) { PreparedStatement pst = null; ResultSet
   * rs = null; Long qty = 0L; try { pst =conn.prepareStatement(
   * "select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock "
   * +
   * "where m_locator_id in(select m_locator_id from  m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse "
   * +
   * "where name ilike 'Saleable O%' and ad_org_id='603C6A266B4C40BCAD87C5C43DDF53EE') and isactive='Y') and m_reservation_id in ("
   * +
   * "select	m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty from m_product p, "
   * +
   * "m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in "
   * +
   * "(select m_warehouse_id from m_warehouse where name ilike 'Saleable O%' and ad_org_id='603C6A266B4C40BCAD87C5C43DDF53EE')"
   * +
   * " and isactive='Y') and sd.qtyonhand >0 and	sd.m_product_id=p.m_product_id and p.m_product_id='"
   * +prodId+"' " + "group by p.m_product_id"); rs = pst.executeQuery(); while (rs.next()) { qty =
   * rs.getLong("msdqty"); } } catch(Exception e1) { e1.printStackTrace(); } finally { try { if(pst
   * != null && rs != null) { pst.close(); rs.close(); } } catch (Exception e) {
   * e.printStackTrace(); } return qty; } }
   */

  private Long qtyAlreadyShipped(String productId, String storeOrgId, Connection conn) {
    // TODO Auto-generated method stub
    PreparedStatement pst = null;
    ResultSet rs = null;
    Long alShipQtyp = 0L;
    try {
      pst = conn
          .prepareStatement("select sum(l.em_sw_confirmedqty) as alreadyconfirmed from c_orderline l, c_order h where "
              + "h.c_order_id=l.c_order_id and l.m_product_id='"
              + productId
              + "' and issotrx='N' and "
              + "l.ad_org_id='"
              + storeOrgId
              + "' and l.createdby='100' and h.DocStatus in ('CO','OBWPL_PK','OBWPL_SH')");
      rs = pst.executeQuery();
      while (rs.next()) {
        alShipQtyp = rs.getLong("alreadyconfirmed");
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      try {
        if (pst != null && rs != null) {
          pst.close();
          rs.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return alShipQtyp;
    }
  }

  private Long claculateReqQty(String productId, Long ue, Long minQty, Long maxQty,
      Long storeStock, String zone) {

    Long reqQty = 0L;
    Long calcReqQty = 0L;

    Calendar c = Calendar.getInstance();
    c.setTime(new Date());
    int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

    // Spoon functionality for karanataka stores, If dayOfWeek is Friday or Saturday, UE round up
    if (((dayOfWeek == 5) || (dayOfWeek == 6)) && (zone.equals("5"))) {
      if ((storeStock <= maxQty) && (storeStock >= 0)) {
        calcReqQty = maxQty - storeStock;
        // To block stock from going to warehouse UE is set to 0
        if (ue > 0) {
          calcReqQty = (long) Math.ceil(calcReqQty / ue);
          reqQty = calcReqQty * ue;
        }
      }
    }
    // Spoon functionality for Ahemdabad store, If dayOfWeek is Sunday, UE round up
    else if ((dayOfWeek == 0) && (zone.equals("IN1005"))) {
      if ((storeStock <= maxQty) && (storeStock >= 0)) {
        calcReqQty = maxQty - storeStock;
        // To block stock from going to warehouse UE is set to 0
        if (ue > 0) {
          calcReqQty = (long) Math.ceil(calcReqQty / ue);
          reqQty = calcReqQty * ue;
        }
      }
    }
    // Spoon functionality for thane,Hyd,Ahemdabad store, If dayOfWeek is Monday, UE round up
    else if (((dayOfWeek == 1))
        && (zone.equals("IN1006") || zone.equals("IN1004") || zone.equals("IN1005")
            || zone.equals("IN1008") || zone.equals("IN1010") || zone.equals("IN1009"))) {
      if ((storeStock <= maxQty) && (storeStock >= 0)) {
        calcReqQty = maxQty - storeStock;
        // To block stock from going to warehouse UE is set to 0
        if (ue > 0) {
          calcReqQty = (long) Math.ceil(calcReqQty / ue);
          reqQty = calcReqQty * ue;
        }
      }
    } else {
      // round up to ue
      if (storeStock <= minQty && storeStock >= 0) {
        calcReqQty = maxQty - storeStock;
        // if(ue<0) ue=1;
        // To block stock from going to warehouse UE is set to 0
        if (ue > 0) {
          calcReqQty = (long) Math.ceil(calcReqQty / ue);
          reqQty = calcReqQty * ue;
        }
      }
    }
    return reqQty;
  }

  private List<AutoDCProductDTO> getProductByDept(String storeDeptId, String storeOrgId,
      String storeLocator, Connection conn) {
    // TODO Auto-generated method stub
    PreparedStatement pst = null;
    ResultSet rs = null;
    PreparedStatement pst1 = null;
    ResultSet rs1 = null;
    PreparedStatement pst2 = null;
    ResultSet rs2 = null;
    final List<AutoDCProductDTO> products = new ArrayList<AutoDCProductDTO>();

    try {
      final String where = "em_cl_log_rec in ('54','145','9.7','5') ";
      final String query = "select distinct p.m_product_id as product_id, p.name as itemcode, round(p.em_cl_ue_qty,0) as ueqty, "
          + "round(p.em_cl_pcb_qty,0) as pcbqty, sum(sd.qtyonhand) as whstock, coalesce(round((select qtyonhand from m_storage_detail "
          + "where m_locator_id='"
          + storeLocator
          + "' and	m_product_id=p.m_product_id  limit 1),0),'0')	as storestock ,"
          + " m.name as modelname, m.cl_brand_id as brand_id, p.em_cl_size as size, c.name as color, dept.cl_storedept_id, "
          + "mm.displaymin, mm.minqty, mm.maxqty, coalesce(em_cl_log_rec,'0') as logrec from m_product p, m_storage_detail sd, cl_model m, cl_color c, cl_storedept dept,"
          + " cl_minmax mm where p.producttype = 'I' and sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in "
          + "(select m_warehouse_id from m_warehouse where name ilike 'Saleable%' and ad_org_id='603C6A266B4C40BCAD87C5C43DDF53EE')) "
          + "and sd.qtyonhand >0 and sd.m_product_id=p.m_product_id and m.cl_model_id=p.em_cl_model_id and p.em_cl_color_id=c.cl_color_id "
          + "and m.cl_storedept_id=dept.cl_storedept_id and mm.m_product_id=p.m_product_id and mm.ad_org_id='"
          + storeOrgId
          + "' and "
          + "mm.isinrange='Y' and dept.cl_storedept_id='"
          + storeDeptId
          + "' and "
          + where
          + " group by p.m_product_id, p.name, p.em_cl_ue_qty,"
          + " m.name, m.cl_brand_id, p.em_cl_size, c.name, dept.cl_storedept_id, mm.displaymin, mm.minqty, mm.maxqty,pcbqty,em_cl_log_rec";
      pst = conn.prepareStatement(query);
      rs = pst.executeQuery();
      while (rs.next()) {
        final AutoDCProductDTO newProd = new AutoDCProductDTO();
        newProd.setDisplaymin((long) rs.getInt("displaymin"));
        newProd.setMaxQty((long) rs.getInt("maxqty"));
        newProd.setMinQty((long) rs.getInt("minqty"));
        newProd.setPcbQty((long) rs.getInt("pcbqty"));
        newProd.setProductId(rs.getString("product_id"));
        newProd.setStoreStock((long) rs.getInt("storestock"));
        newProd.setUeQty((long) rs.getInt("ueqty"));
        newProd.setWhStock((long) rs.getInt("whstock"));
        newProd.setBrandId(rs.getString("brand_id"));
        newProd.setSize(rs.getString("size"));
        newProd.setColor(rs.getString("color"));
        newProd.setModelName(rs.getString("modelname"));
        newProd.setItemCode(rs.getString("itemcode"));
        newProd.setLogRecharge(rs.getString("logrec"));
        try {
          pst1 = conn
              .prepareStatement("select em_idsd_pcb_threshold_id from cl_minmax where m_product_id='"
                  + rs.getString("product_id") + "' and ad_org_id='" + storeOrgId + "'");
          rs1 = pst1.executeQuery();
          while (rs1.next()) {
            if (rs1.getString("em_idsd_pcb_threshold_id") != null) {
              newProd.setPcbThresholdId(rs1.getString("em_idsd_pcb_threshold_id"));
              newProd.setPcb(true);
              pst2 = conn
                  .prepareStatement("select * from idsd_pcb_threshold where idsd_pcb_threshold_id='"
                      + rs1.getString("em_idsd_pcb_threshold_id") + "'");
              rs2 = pst2.executeQuery();
              while (rs2.next()) {
                newProd.setThresholdValue(rs2.getString("value"));
              }
            } else {
              newProd.setPcb(false);
            }
          }
        } catch (Exception e1) {
          e1.printStackTrace();
        }
        try {
          pst1 = conn
              .prepareStatement("select em_idsd_oxylane_prodcat_id from m_product where m_product_id='"
                  + rs.getString("product_id") + "'");
          rs1 = pst1.executeQuery();
          while (rs1.next()) {
            if (newProd.isPcb() == false) {
              if (rs1.getString("em_idsd_oxylane_prodcat_id") != null) {
                newProd.setIdsdProdCategoryId(rs1.getString("em_idsd_oxylane_prodcat_id"));
                newProd.setPcb(true);
                pst2 = conn
                    .prepareStatement("select * from idsd_oxylane_prodcategory where idsd_oxylane_prodcategory_id='"
                        + rs1.getString("em_idsd_oxylane_prodcat_id") + "'");
                rs2 = pst2.executeQuery();
                while (rs2.next()) {
                  newProd.setThresholdValue(rs2.getString("value"));
                }
              }
            }
          }
        } catch (Exception e1) {
          e1.printStackTrace();
        }
        products.add(newProd);
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      try {
        if (pst != null && rs != null) {
          pst.close();
          rs.close();
        }
        if (pst1 != null && rs1 != null) {
          pst1.close();
          rs1.close();
        }
        if (pst2 != null && rs2 != null) {
          pst2.close();
          rs2.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return products;
    }
  }

  private Map<String, Long> implantationQty(String productId, String storeOrgId, Connection conn) {
    // TODO Auto-generated method stub

    Map<String, Long> impQty = new HashMap<String, Long>();
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {
      pst = conn
          .prepareStatement("select implantation_qty as impqty,blocked_qty as blockdqty from cl_implantation where "
              + "m_product_id='"
              + productId
              + "' and store_implanted='"
              + storeOrgId
              + "' and isimplanted = 'N'");
      rs = pst.executeQuery();
      while (rs.next()) {
        impQty.put("impqty", rs.getLong("impqty"));
        impQty.put("blockdqty", rs.getLong("blockdqty"));
      }
    } catch (Exception e1) {
      e1.printStackTrace();
    } finally {
      try {
        if (pst != null && rs != null) {
          pst.close();
          rs.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return impQty;
    }
  }

  protected HttpURLConnection createConnection() throws Exception {
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(LOGIN, PWD.toCharArray());
      }
    });
    final URL url = new URL(p.getProperty("ws2URL"));
    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();

    hc.setRequestMethod("POST");
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    return hc;
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

}
