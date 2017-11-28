package com.sysfore.decathlonecom.dao;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.tax.TaxRate;

import com.sysfore.decathlonecom.model.EcomOrder;
import com.sysfore.decathlonecom.model.Product;
import com.sysfore.decathlonecom.model.Stock;

public class EcomOrderServiceDAO {

  // Properties Singleton class instantiation
  Properties p = ECommerceUtil.getInstance().getProperties();
  static Connection conn = null;

  ResourceBundle properties = null;

  public EcomOrderServiceDAO() throws Exception {
    properties = ResourceBundle.getBundle("com.sysfore.decathlonecom.Openbravo");
    Class.forName(properties.getString("bbdd.driver"));
    conn = DriverManager.getConnection(
        properties.getString("bbdd.url") + "/" + properties.getString("bbdd.sid"),
        properties.getString("bbdd.user"), properties.getString("bbdd.password"));

    conn.setAutoCommit(false);

  }

  public String[] selectNames(String bPartnerId) {
    String sql = "Select name, name2 from c_bpartner where c_bpartner_id=?";
    String name[] = new String[2];
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, bPartnerId);
      rs = pst.executeQuery();

      while (rs.next()) {
        name[0] = rs.getString("name");
        name[1] = rs.getString("name2");
      }
      rs.close();
      pst.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return name;
  }

  public String selectOrgId(String value) {
    String sql = "select ad_org_id from ad_org where lower(name) = lower(?)";
    String ordId = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        ordId = rs.getString("ad_org_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return ordId;
  }

  public String selectBPId(String value) {
    String sql = "select c_bpartner_id from c_bpartner where em_rc_oxylane = ?";
    String clientId = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        clientId = rs.getString("c_bpartner_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return clientId;
  }

  public String selectLocation(String value) {
    String sql = "SELECT MIN(C_BPartner_Location_ID) as C_BPartner_Location_ID FROM C_BPartner_Location l WHERE l.C_BPartner_ID=?";
    String locationId = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        locationId = rs.getString("C_BPartner_Location_ID");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return locationId;
  }

  public String selectWareHouse(String value) {

    String sql = "select m_warehouse_id from m_warehouse where lower(name) = lower(?)";
    String warehouse = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        warehouse = rs.getString("m_warehouse_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return warehouse;
  }

  public String createEcomOrder(EcomOrder ecomOrder) {

    String sqlToCorder = "INSERT INTO C_Order (C_Order_ID, AD_Client_ID, AD_Org_ID, isActive, Created, CreatedBy, Updated, UpdatedBy,"
        + "C_Doctypetarget_ID, IsSOTrx, DocumentNo, C_BPartner_ID, BillTo_ID, C_BPartner_Location_ID, AD_User_ID,"
        + "Description, C_PaymentTerm_ID, M_PriceList_ID, M_Warehouse_ID, M_Shipper_ID, SalesRep_ID, AD_Orgtrx_ID,"
        + "C_Activity_ID, DocStatus, DocAction, C_Doctype_ID, DateOrdered, DatePromised, DateAcct, C_Currency_ID,"
        + "PaymentRule, InvoiceRule, DeliveryRule, FreightcostRule, DeliveryviaRule, PriorityRule, IsDiscountPrinted,"
        + "Processing,em_ds_receiptno,em_ds_ratesatisfaction,em_ds_totalpriceadj,em_ds_posno,em_ds_chargeamt,processed,grandtotal,em_ds_bpartner_id)"
        + "VALUES (?, ?, ?, 'Y', TO_DATE(now()), ?, TO_DATE(now()), ?, ?, 'N', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
        + "?, to_timestamp(?,'DD-MM-YYYY HH24:MI:SS:MS'), "
        + "TO_DATE(?), ?, ?, ?, ?,?,?, ?, ?, ?, ?,?,?,?,?,?,?,?)";

    String sqlToCorderLine = "INSERT INTO C_OrderLine (C_OrderLine_ID, AD_Client_ID, AD_Org_ID, isActive, Created, CreatedBy, Updated, "
        + "UpdatedBy,Line, M_Product_ID, QtyOrdered, PriceActual, C_Tax_ID, FreightAmt, C_Order_ID, PriceList, PriceStd, "
        + "PriceLimit,Discount, C_UOM_ID, DateOrdered, M_Warehouse_ID, C_Currency_ID, Description, C_BPartner_ID,"
        + " C_BPartner_Location_ID,em_ds_lotqty,em_ds_lotprice,em_ds_boxqty,em_ds_boxprice,em_ds_unitqty,Gross_Unit_Price,line_gross_amount,linenetamt) "
        + "VALUES (?, ?, ?, 'Y', now(), ?, now(), ?,?, ?, ?, ?, ?, COALESCE(?, 0), ?, ?,"
        + " ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        + "?,?, TO_NUMBER(?), TO_NUMBER(?), TO_NUMBER(?))";
    /*
     * String sqlToIorder =
     * "INSERT INTO i_order(i_order_id,ad_client_id,ad_org_id,m_warehouse_id,c_bpartner_id," +
     * "name,em_im_lastname,description,documentno,m_product_id,priceactual,qtyordered,em_im_pcbprice,"
     * +
     * "em_im_pcbqty,em_im_ueprice,em_im_ueqty,c_tax_id,em_im_receiptno,dateordered,ad_user_id,createdby,updatedby,doctypename,em_im_chargeamt,em_im_customersatisfaction"
     * + ") values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
     */

    String sqlToPayment = "INSERT INTO ds_paymentinfo(ds_paymentinfo_id,ad_client_id,ad_org_id,createdby,updatedby,paymentmode"
        + ",identifier,amount,receiptno) values (?,?,?,?,?,?,?,?,?)";

    String sqlToAddress = "INSERT INTO ds_location(ds_location_id,ad_client_id,ad_org_id,createdby,updatedby,address1"
        + ",address2,address3,address4,city,postal,state,country,receiptno) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    // String sqlTax = "Select c_tax_id from c_tax where c_taxcategory_id = ?";

    // Checking no of line items
    int totalCount = 0;
    PreparedStatement createCOrder = null;
    PreparedStatement createCOrderline = null;
    PreparedStatement createPaymentInfo = null;
    PreparedStatement addressInfo = null;
    PreparedStatement psTax = null;
    PreparedStatement postCOrder = null;
    ResultSet rsTax = null;
    String COrderId = SequenceIdData.getUUID();

    String uniqueId = SequenceIdData.getUUID();

    if (ecomOrder.getItemOrdered() != null) {
      totalCount = ecomOrder.getItemOrdered().size();
    }

    try {

      /************************************************************************************/
      double unitPrice = 0;
      double unitQty = 0;
      double uePrice = 0;
      double ueQty = 0;
      double pcbPrice = 0;
      double pcbQty = 0;
      double grantTotal = 0;
      double chargeAmt = 0;
      // Creating header corder
      try {
        createCOrder = conn.prepareStatement(sqlToCorder);
        createCOrder.setString(1, COrderId);
        createCOrder.setString(2, properties.getString("ws.client"));
        createCOrder.setString(3, ecomOrder.getOrgName());
        createCOrder.setString(4, properties.getString("ws.user"));
        createCOrder.setString(5, properties.getString("ws.user"));
        createCOrder.setString(8, "35586321F375451389832DD198CA1DC7");// "E678F32A20624D258AFD91CF83449A1F"
        createCOrder.setString(9, "D83677BA1F61434097590985224FD08A");
        createCOrder.setString(10, "D83677BA1F61434097590985224FD08A");// 0A0D2FB9A1ED4DFCAD69D392277EFCFE
        createCOrder.setString(11, properties.getString("ws.user"));
        createCOrder.setString(12, "Online Order");
        createCOrder.setString(13, "A4B18FE74DF64897B71663B0E57A4EFE");
        createCOrder.setString(14, "E0392FA3AF3B49DA876DFF40E65FC2E9");
        createCOrder.setString(15, "4336240A47C5454288B9754487EA3740");// warehouse
        createCOrder.setString(16, null);
        createCOrder.setString(17, properties.getString("ws.user"));
        createCOrder.setString(18, ecomOrder.getOrgName());
        createCOrder.setString(19, null);
        createCOrder.setString(20, "DR");
        createCOrder.setString(21, "CO");
        createCOrder.setString(26, "304");
        createCOrder.setString(27, "B");
        createCOrder.setString(28, "D");
        createCOrder.setString(29, "A");
        createCOrder.setString(30, "I");
        createCOrder.setString(31, "P");
        createCOrder.setString(32, "5");
        createCOrder.setString(33, "N");
        createCOrder.setString(34, "N");
        createCOrder.setString(35, ecomOrder.getBillNo());
        createCOrder.setString(36, ecomOrder.getFeedback());
        createCOrder.setInt(37, 0);
        createCOrder.setString(38, "0");

        try {
          chargeAmt = Double.parseDouble(ecomOrder.getChargeAmt());

        } catch (NumberFormatException e) {
          e.printStackTrace();
        }

        createCOrder.setDouble(39, chargeAmt);
        createCOrder.setString(40, "N");
        createCOrder.setDouble(41, grantTotal);
        createCOrder.setString(42, ecomOrder.getCustomerId());

        if (ecomOrder.getOrgName().equalsIgnoreCase("B2D0E3B212614BA6989ADCA3074FC423")) {
          createCOrder.setString(7, "*ECOM*" + ecomOrder.getBillNo());
          createCOrder.setString(6, p.getProperty("ecomsupplydocumentType"));
          createCOrder.setString(22, p.getProperty("ecomsupplydocumentType"));
          createCOrder.setString(15, "D490F9B57DE64C3ABAF953FEBB8C5F70");// warehouse

        } else {
          createCOrder.setString(7, "*B2B*" + ecomOrder.getBillNo());
          createCOrder.setString(6, p.getProperty("ecomsupplydocumentType"));
          createCOrder.setString(22, p.getProperty("ecomsupplydocumentType"));
          createCOrder.setString(15, "F55C4BA65ABE4382B65D4549319DC570");// warehouse
        }
        try {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          java.util.Date d = sdf.parse(ecomOrder.getOrderDate(), new ParsePosition(0));
          java.sql.Timestamp timeStampDate = new Timestamp(d.getTime());
          createCOrder.setTimestamp(23, timeStampDate);
          createCOrder.setTimestamp(24, timeStampDate);
          createCOrder.setTimestamp(25, timeStampDate);
        } catch (Exception e) {
          java.sql.Timestamp timeStampDate = new Timestamp(new java.util.Date().getTime());
          createCOrder.setTimestamp(23, timeStampDate);
          createCOrder.setTimestamp(24, timeStampDate);
          createCOrder.setTimestamp(25, timeStampDate);
        }

        createCOrder.executeUpdate();
        conn.commit();
      } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      final List<BaseOBObject> oLines = new ArrayList<BaseOBObject>();
      // Adding the lines to i_order
      try {
        int LineNo = 0;

        for (Product product : ecomOrder.getItemOrdered()) {
          String cOrderLineId = SequenceIdData.getUUID();
          LineNo = LineNo + 10;
          createCOrderline = conn.prepareStatement(sqlToCorderLine);
          createCOrderline.setString(1, cOrderLineId);
          createCOrderline.setString(2, properties.getString("ws.client"));
          createCOrderline.setString(3, ecomOrder.getOrgName());
          createCOrderline.setString(4, properties.getString("ws.user"));
          createCOrderline.setString(5, properties.getString("ws.user"));
          createCOrderline.setInt(6, LineNo);
          createCOrderline.setString(7, product.getProductId());
          createCOrderline.setDouble(8, Double.parseDouble(product.getQuantityOrdered()));
          try {

            unitPrice = Double.parseDouble(product.getUnitPrice());

          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          createCOrderline.setDouble(9, unitPrice);

          /*
           * createCOrderline.setString(4, ecomOrder.getWarehouseName());
           * createCOrderline.setString(5, "E678F32A20624D258AFD91CF83449A1F");
           * createCOrderline.setString(6, ecomOrder.getFirstName()); createCOrderline.setString(7,
           * ecomOrder.getLastName()); createCOrderline.setString(8, ecomOrder.getDescription());
           * 
           * if (ecomOrder.getOrgName().equalsIgnoreCase("B2D0E3B212614BA6989ADCA3074FC423")) {
           * createCOrderline.setString(9, "*ECOM*" + ecomOrder.getBillNo()); } else {
           * createCOrderline.setString(9, "*B2B*" + ecomOrder.getBillNo()); }
           */

          try {

            unitQty = Double.parseDouble(product.getUnitQty());

          } catch (NumberFormatException e) {

          }

          try {
            pcbPrice = Double.parseDouble(product.getPcbPrice());

          } catch (NumberFormatException e) {

          }

          try {

            pcbQty = Double.parseDouble(product.getPcbQty());

          } catch (NumberFormatException e) {

          }

          try {

            uePrice = Double.parseDouble(product.getUePrice());

          } catch (NumberFormatException e) {

          }

          try {

            ueQty = Double.parseDouble(product.getUeQty());

          } catch (NumberFormatException e) {

          }
          // createCOrderline.setDouble(11, unitPrice);
          createCOrderline.setDouble(28, unitQty);
          createCOrderline.setDouble(26, pcbPrice);
          createCOrderline.setDouble(27, pcbQty);
          createCOrderline.setDouble(25, uePrice);
          createCOrderline.setDouble(24, ueQty);
          createCOrderline.setString(29, product.getGrossUnitPrice());
          createCOrderline.setString(30, product.getLineGrossAmt());
          createCOrderline.setString(31, product.getLineNetAmt());

          /*
           * psTax = conn.prepareStatement(sqlTax); psTax.setString(1, product.getTaxId()); rsTax =
           * psTax.executeQuery(); String taxId = ""; if (rsTax.next()) { taxId =
           * rsTax.getString("c_tax_id"); }
           */

          // createCOrderline.setString(10, taxId);
          TaxRate rate = getTax(product.getTaxId(), ecomOrder.getState(), ecomOrder.getCustomerId());
          String tax = rate.getId();
          createCOrderline.setString(10, tax);
          createCOrderline.setInt(11, 0);
          createCOrderline.setString(12, COrderId);
          createCOrderline.setDouble(13, 0.00);
          createCOrderline.setInt(14, 0);
          createCOrderline.setInt(15, 0);
          createCOrderline.setInt(16, 0);
          createCOrderline.setString(17, "100");
          try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date d = sdf.parse(ecomOrder.getOrderDate(), new ParsePosition(0));
            java.sql.Timestamp timeStampDate = new Timestamp(d.getTime());
            createCOrderline.setTimestamp(18, timeStampDate);
          } catch (Exception e) {
            java.sql.Timestamp timeStampDate = new Timestamp(new java.util.Date().getTime());
            createCOrderline.setTimestamp(18, timeStampDate);
          }
          if (ecomOrder.getOrgName().equalsIgnoreCase("B2D0E3B212614BA6989ADCA3074FC423")) {
            createCOrderline.setString(19, "D490F9B57DE64C3ABAF953FEBB8C5F70");// warehouse

          } else {

            createCOrderline.setString(19, "F55C4BA65ABE4382B65D4549319DC570");// warehouse
          }

          createCOrderline.setString(20, "304");
          createCOrderline.setString(21, null);

          createCOrderline.setString(22, "35586321F375451389832DD198CA1DC7");
          createCOrderline.setString(23, "D83677BA1F61434097590985224FD08A");// "0A0D2FB9A1ED4DFCAD69D392277EFCFE"

          // createCOrderline.setString(20, properties.getString("ws.user"));
          // createCOrderline.setString(21, properties.getString("ws.user"));
          // createCOrderline.setString(22, properties.getString("ws.user"));
          // if (ecomOrder.getOrgName().equalsIgnoreCase("B2D0E3B212614BA6989ADCA3074FC423")) {
          // createCOrderline.setString(23, "Ecommerce Order");
          // } else {
          // createCOrderline.setString(23, "B2B Order");
          // }

          createCOrderline.executeUpdate();
          oLines.add(OBDal.getInstance().get(OrderLine.class, cOrderLineId));
        }
      } catch (Exception e1) {

        e1.printStackTrace();
      }

      try {
        // createCOrderline.executeBatch();
        conn.commit();

      } catch (Exception e1) {
        e1.printStackTrace();
      }

      try {
        // String sqlPostOrder = "select c_order_post1(null,?) from dual";
        String sqlPostOrder = "select IDSD_dc_post1(?) from dual";
        postCOrder = conn.prepareStatement(sqlPostOrder);
        postCOrder.setString(1, COrderId);
        postCOrder.executeQuery();

      } catch (Exception e) {
        e.printStackTrace();
      }

      ImmediateSOonPO soOnPO = new ImmediateSOonPO();
      ArrayList<String> pOids = new ArrayList<String>();
      Order orderHeader = OBDal.getInstance().get(Order.class, COrderId);
      if (orderHeader != null) {
        if (orderHeader.getOrderLineList().size() > 0) {
          processOrder(soOnPO, pOids, orderHeader);
        }
      }

      // Completing the i_order Process
      /************************************************************************************/

      // Adding PaymentInfo
      createPaymentInfo = conn.prepareStatement(sqlToPayment);
      createPaymentInfo.setString(1, SequenceIdData.getUUID());
      createPaymentInfo.setString(2, properties.getString("ws.client"));
      // createPaymentInfo.setString(3, properties.getString("ws.organisation"));
      createPaymentInfo.setString(3, ecomOrder.getOrgName());
      createPaymentInfo.setString(4, properties.getString("ws.user"));
      createPaymentInfo.setString(5, properties.getString("ws.user"));
      createPaymentInfo.setString(6, ecomOrder.getPaymentMode());
      createPaymentInfo.setString(7, ecomOrder.getPaymentIdentifier());
      try {

        grantTotal = Integer.parseInt(ecomOrder.getGrantTotal());

      } catch (NumberFormatException e) {

      }
      createPaymentInfo.setDouble(8, grantTotal);
      createPaymentInfo.setString(9, uniqueId);
      createPaymentInfo.executeUpdate();

      // Ending PaymentInfo

      /***********************************************************************************/

      // Adding Address
      addressInfo = conn.prepareStatement(sqlToAddress);
      addressInfo.setString(1, SequenceIdData.getUUID());
      addressInfo.setString(2, properties.getString("ws.client"));
      addressInfo.setString(3, ecomOrder.getOrgName());
      addressInfo.setString(4, properties.getString("ws.user"));
      addressInfo.setString(5, properties.getString("ws.user"));
      addressInfo.setString(6, ecomOrder.getAddress1());
      addressInfo.setString(7, ecomOrder.getAddress2());
      addressInfo.setString(8, ecomOrder.getAddress3());
      addressInfo.setString(9, ecomOrder.getAddress4());
      addressInfo.setString(10, ecomOrder.getCity());
      addressInfo.setString(11, ecomOrder.getPostal());
      addressInfo.setString(12, ecomOrder.getState());
      addressInfo.setString(13, ecomOrder.getCountry());
      addressInfo.setString(14, uniqueId);
      addressInfo.executeUpdate();
      conn.commit();
      // Ending Address

      /**********************************************************************************/

    } catch (Exception e) {
      e.printStackTrace();
      return "failure";

    }
    return "success";
  }

  private static void processOrder(ImmediateSOonPO soOnPO, ArrayList<String> pOids,
      Order orderHeader) throws Exception {
    // Long curDocNo = docSeq.getNextAssignedNumber();
    // docSeq.setNextAssignedNumber(curDocNo + docSeq.getIncrementBy());
    // OBDal.getInstance().save(docSeq);
    // orderHeader.setDocumentNo(docSeq.getPrefix() + curDocNo.toString());
    // OBDal.getInstance().save(orderHeader);
    soOnPO.processRequest(orderHeader);
    pOids.add(orderHeader.getId());
  }

  public List<Stock> getStock(List<String> values, String warehouse) {

    String content = "'";
    String strwarehouse = "Saleable Whitefield";
    int count = values.size() - 1;
    for (int i = 0; i < count; i++) {
      if (i == (count - 1)) {
        content += values.get(i) + "'";
      } else {
        content += values.get(i) + "','";
      }
    }

    List<Stock> l = new LinkedList();
    PreparedStatement ps = null;
    ResultSet rs = null;
    PreparedStatement psCac = null;
    ResultSet rsCac = null;
    PreparedStatement psCar = null;
    ResultSet rsCar = null;

    String sqlTrue = "SELECT mp.m_product_id as m_product_id,round((((case when round(sum(msd.qtyonhand))<0 Then 0  else round(sum(msd.qtyonhand)) End))-(case when sum((msd.reservedqty))<0 Then 0 else sum((msd.reservedqty)) End))) as msdqty FROM m_product mp INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id WHERE mw.value in ('RED','NS') and ml.isactive='Y' and ml.em_obwhs_type='ST' and mp.m_product_id in ("
        + content + ") group by mp.m_product_id";

    try {
      ps = conn.prepareStatement(sqlTrue);

      rs = ps.executeQuery();

      while (rs.next()) {

        Stock s = new Stock();

        final String carQuery = "SELECT mp.m_product_id as m_product_id,round((((case when round(sum(msd.qtyonhand))<0 Then 0  else round(sum(msd.qtyonhand)) End))-(case when sum((msd.reservedqty))<0 Then 0 else sum((msd.reservedqty)) End))) as msdqty FROM m_product mp INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id WHERE mw.value in ('RED','NS') and ml.isactive='Y' and ml.em_obwhs_type='ST' and mp.m_product_id  ='"
            + rs.getString("m_product_id") + "' group by mp.m_product_id";
        s.setProduct(rs.getString("m_product_id"));

        // int cacQty = 0;
        int carQty = 0;
        int qty = 0;

        // CAR Stock
        psCar = conn.prepareStatement(carQuery);

        rsCar = psCar.executeQuery();
        while (rsCar.next()) {
          carQty = Integer.parseInt(rsCar.getString("msdqty"));
          System.out.print(" carQty " + carQty);
        }
        qty = carQty;
        System.out.print(" Total Qty " + qty);
        String qtyonhand = Integer.toString(qty);
        s.setQty(qtyonhand);
        s.setWarehouseName("Saleable Whitefield");
        l.add(s);
      }
      ps = conn.prepareStatement(sqlTrue);

      rs = ps.executeQuery();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return l;
  }

  public List<String> getStock(String[] values, boolean flag) {

    String sqlFalse = "select m_storage_detail.qtyonhand, m_warehouse.name from m_storage_detail"
        + " inner join m_locator on m_locator.m_locator_id = m_storage_detail.m_locator_id and m_storage_detail.m_product_id = ? "
        + "inner join m_warehouse on m_locator.m_warehouse_id = m_warehouse.m_warehouse_id and lower(m_warehouse.name) like lower('%saleable%')";

    String sqlTrue = "select m_storage_detail.qtyonhand, m_warehouse.name from m_storage_detail"
        + " inner join m_locator on m_locator.m_locator_id = m_storage_detail.m_locator_id and m_storage_detail.m_product_id = ? "
        + "inner join m_warehouse on m_locator.m_warehouse_id = m_warehouse.m_warehouse_id where m_warehouse.m_warehouse_id= ?";

    PreparedStatement stockInfo = null;
    ResultSet stockData = null;
    String qtyOnHand = "";
    String warehouseName = "";

    List<String> arrayList = new ArrayList<String>();

    try {

      if (flag == false) {

        stockInfo = conn.prepareStatement(sqlFalse);
        stockInfo.setString(1, values[0]);

      } else {

        stockInfo = conn.prepareStatement(sqlTrue);
        stockInfo.setString(1, values[0]);
        stockInfo.setString(2, values[1]);

      }

      stockData = stockInfo.executeQuery();

      if (stockData != null) {

        while (stockData.next()) {

          qtyOnHand = "" + stockData.getInt("qtyonhand");
          warehouseName = "" + stockData.getString("name");
          arrayList.add(qtyOnHand + "," + warehouseName);

        }
      }

      conn.commit();
    } catch (Exception e) {

      e.printStackTrace();

    }

    return arrayList;
  }

  public String validateMandatoryData(String ad_org, String warehouse, String product,
      String bPartner, String taxid) {

    String message = "";
    String sqlOrg = "Select ad_org_id from ad_org where name = ?";
    String sqlWarehouse = "Select m_warehouse_id from m_warehouse where name = ?";
    String sqlProduct = "Select m_product_id from m_product where m_product_id = ?";
    String sqlPartner = "Select c_bpartner_id from c_bpartner where c_bpartner_id = ?";
    String sqlTax = "Select c_tax_id from c_tax where c_taxcategory_id = ?";

    PreparedStatement psOrg = null, psWarehouse = null, psProduct = null, psPartner = null, psTax = null;
    ResultSet rsOrg = null, rsWarehouse = null, rsProduct = null, rsPartner = null, rsTax = null;
    try {

      psOrg = conn.prepareStatement(sqlOrg);
      psOrg.setString(1, ad_org);
      rsOrg = psOrg.executeQuery();

      if (!rsOrg.next()) {
        message += " Organisation, ";
      }

      psWarehouse = conn.prepareStatement(sqlWarehouse);
      psWarehouse.setString(1, warehouse);
      rsWarehouse = psWarehouse.executeQuery();

      if (!rsWarehouse.next()) {
        message += " Warehouse, ";
      }

      psProduct = conn.prepareStatement(sqlProduct);
      psProduct.setString(1, product);
      rsProduct = psProduct.executeQuery();

      if (!rsProduct.next()) {
        message += " Product, ";
      }

      psPartner = conn.prepareStatement(sqlPartner);
      psPartner.setString(1, bPartner);
      rsPartner = psPartner.executeQuery();

      if (!rsPartner.next()) {
        message += " Customer, ";
      }

      psTax = conn.prepareStatement(sqlTax);
      psTax.setString(1, taxid);
      rsTax = psTax.executeQuery();

      if (!rsTax.next()) {
        message += " Tax, ";
      }

      if (!message.equals("")) {
        // message.replace(",", "");
        message += "not exists in ERP definition";
      }

      conn.commit();

    } catch (Exception e) {

      e.printStackTrace();

    }

    return message;
  }

  public void closeConnection() throws SQLException {
    if (conn == null) {
      return;
    }
    conn.close();
    conn = null;
  }

  public static TaxRate getTax(String taxCategoryId, String region, String customerId)
      throws SQLException {
    String sqlTax = "Select c_tax_id from c_tax where c_taxcategory_id = ?";
    PreparedStatement psTax = null;
    ResultSet rsTax = null;

    psTax = conn.prepareStatement(sqlTax);

    psTax.setString(1, taxCategoryId);
    rsTax = psTax.executeQuery();
    String taxId = "";
    if (rsTax.next()) {
      taxId = rsTax.getString("c_tax_id");
    }

    org.openbravo.model.common.businesspartner.BusinessPartner bpQuery = OBDal.getInstance().get(
        org.openbravo.model.common.businesspartner.BusinessPartner.class, customerId);
    String companyRegion = bpQuery.getRCCompany().getLocationAddress().getRegion().getName();
    if (companyRegion != null) {
      TaxRate currentTax = OBDal.getInstance().get(TaxRate.class, taxId);
      String taxName = currentTax.getName();
      if ((taxName.toUpperCase().contains("VAT"))
          && !(companyRegion.toUpperCase().equals("KARNATAKA"))) {
        OBCriteria<TaxRate> taxCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
        taxCriteria.add(Restrictions.eq(TaxRate.PROPERTY_RATE, currentTax.getRate()));
        taxCriteria.add(Restrictions.isNull(TaxRate.PROPERTY_DESTINATIONREGION));
        List<TaxRate> rateList = taxCriteria.list();
        if (rateList.size() != 0) {
          return rateList.get(0);
        }

      }
      return currentTax;
    } else {
      TaxRate currentTax = OBDal.getInstance().get(TaxRate.class, taxId);
      String taxName = currentTax.getName();
      if ((taxName.toUpperCase().contains("VAT")) && !(region.toUpperCase().equals("KARNATAKA"))) {
        OBCriteria<TaxRate> taxCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
        taxCriteria.add(Restrictions.eq(TaxRate.PROPERTY_RATE, currentTax.getRate()));
        taxCriteria.add(Restrictions.isNull(TaxRate.PROPERTY_DESTINATIONREGION));
        List<TaxRate> rateList = taxCriteria.list();
        if (rateList.size() != 0) {
          return rateList.get(0);
        }

      }

      return currentTax;
    }
  }

}
