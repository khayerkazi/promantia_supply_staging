package in.decathlon.retail.api.omniCommerce.ad_webservice;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;

public class OmniPrintInvoiceActionHandler extends BaseActionHandler {

  final DecimalFormat df = new DecimalFormat("#.##");
  private static final Logger log = Logger.getLogger(OmniPrintInvoiceActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    JSONObject jsonData;

    final JSONObject result = new JSONObject();

    try {

      jsonData = new JSONObject(content);
      final String recordId = jsonData.getString("recordId");
      System.out.println("recordId" + recordId);
      final Order omniOrder = OBDal.getInstance().get(Order.class, recordId);

      /*
       * final Location location2 = OBDal.getInstance().get(Location.class,
       * omniOrder.getInvoiceAddress());
       * 
       * // StringBuilder for preparing the Shipping Address html code final StringBuilder
       * billingAddress = new StringBuilder();
       * 
       * if (location2 != null) { billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;"
       * ); if (omniOrder.getBusinessPartner().getName() != null) {
       * billingAddress.append(omniOrder.getBusinessPartner().getName()); } if
       * (omniOrder.getBusinessPartner().getName2() != null) { billingAddress.append("&nbsp;" +
       * omniOrder.getBusinessPartner().getName2()); } billingAddress.append("</td></tr>");
       * billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (omniOrder.getBusinessPartner().getRCCompany().getCompanyName() != null) {
       * billingAddress.append(omniOrder.getBusinessPartner().getRCCompany().getCompanyName()); }
       * billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (location2.getRegion().getName() != null) {
       * billingAddress.append(location2.getRegion().getName()); }
       * billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (location2.getCountry().getName() != null) {
       * billingAddress.append(location2.getCountry().getName()); }
       * billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (location2.getAddressLine1() != null) {
       * billingAddress.append(location2.getAddressLine1()); } if (location2.getAddressLine2() !=
       * null) { billingAddress.append(",&nbsp;" + location2.getAddressLine2()); } if
       * (location2.getRCAddress3() != null) { billingAddress.append(",&nbsp;" +
       * location2.getRCAddress3()); } billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (location2.getCityName() != null) { billingAddress.append(location2.getCityName()); }
       * billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (location2.getPostalCode() != null) {
       * billingAddress.append(location2.getPostalCode()); } billingAddress.append("</td></tr>");
       * billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (omniOrder.getBusinessPartner().getRCMobile() != null) {
       * billingAddress.append(omniOrder.getBusinessPartner().getRCMobile()); }
       * billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;"
       * ); if (omniOrder.getBusinessPartner().getRCEmail() != null) {
       * billingAddress.append(omniOrder.getBusinessPartner().getRCEmail()); }
       * billingAddress.append("</td></tr>"); billingAddress.append(
       * "<tr><td style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;TIN NO&nbsp;:&nbsp;"
       * ); if (omniOrder.getBusinessPartner().getRCCompany().getLicenseNo() != null) {
       * billingAddress.append(omniOrder.getBusinessPartner().getRCCompany().getLicenseNo()); }
       * billingAddress.append("</td></tr>"); }
       */

      // Shipping Address
      final StringBuilder shippingAddress = new StringBuilder();
      HttpURLConnection hc;
      JSONObject dataJsonObject = null;

      // calling customer API
      try {
        hc = createConnection(omniOrder);
        hc.connect();
        // Getting the Response from the Web service
        BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
        String inputLine;
        StringBuffer resp = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          resp.append(inputLine);
        }

        String secondResponse = resp.toString();
        final JSONObject respJsonObject = new JSONObject(secondResponse);
        final JSONObject responseJsonObject = (JSONObject) respJsonObject.get("response");
        final JSONArray dataJsonArray = (JSONArray) responseJsonObject.get("data");
        if (dataJsonArray.length() > 0) {
          dataJsonObject = dataJsonArray.getJSONObject(0);
        }
        log.info("JSON Response : " + dataJsonObject);
        in.close();

      } catch (Exception e1) {
        e1.printStackTrace();
      }
      hc=null;
      JSONObject addressJsonObject = null;
      // calling address API
      try {
          hc = createConnectionForAddress(omniOrder);
          hc.connect();
          // Getting the Response from the Web service
          BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
          String inputLine;
          StringBuffer resp = new StringBuffer();

          while ((inputLine = in.readLine()) != null) {
            resp.append(inputLine);
          }

          String secondResponse = resp.toString();
          final JSONObject respJsonObject = new JSONObject(secondResponse);
          final JSONObject responseJsonObject = (JSONObject) respJsonObject.get("response");
          final JSONArray dataJsonArray = (JSONArray) responseJsonObject.get("data");
          if (dataJsonArray.length() > 0) {
        	  addressJsonObject = dataJsonArray.getJSONObject(0);
          }
          log.info("Address API JSON Response : " + addressJsonObject);
          in.close();

        } catch (Exception e1) {
          e1.printStackTrace();
        }

      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;");
      if (!dataJsonObject.get("name").equals("")) {
        shippingAddress.append(dataJsonObject.get("name"));
      }
      if (!dataJsonObject.get("name2").equals("")) {
        shippingAddress.append(dataJsonObject.get("name2"));
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      if (omniOrder.getBusinessPartner().getRCCompany().getCompanyName() != null) {
        shippingAddress.append(omniOrder.getBusinessPartner().getRCCompany().getCompanyName());
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      shippingAddress.append(addressJsonObject.get("state"));
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      shippingAddress.append(addressJsonObject.get("country"));
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      if (!addressJsonObject.get("Address1").equals("")) {
        shippingAddress.append(addressJsonObject.get("Address1"));
      }
      if (!addressJsonObject.get("Address2").equals("")) {
        shippingAddress.append(",&nbsp;" + addressJsonObject.get("Address2"));
      }
      if (!addressJsonObject.get("landmark").equals("")) {
        shippingAddress.append(",&nbsp; landmark : " + addressJsonObject.get("landmark"));
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      if (!addressJsonObject.get("city").equals("")) {
        shippingAddress.append(addressJsonObject.get("city"));
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      if (!addressJsonObject.get("zipcode").equals("")) {
        shippingAddress.append(addressJsonObject.get("zipcode"));
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      if (!addressJsonObject.get("mobile").equals("")) {
        shippingAddress.append(addressJsonObject.get("mobile"));
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;");
      if (!dataJsonObject.get("rCEmail").equals("")) {
        shippingAddress.append(dataJsonObject.get("rCEmail"));
      }
      shippingAddress.append("</th></tr>");
      shippingAddress
          .append("<tr><th style='width:100%;height:15px;font-size:11px;line-height:14px;text-align:left;'>&nbsp;&nbsp;&nbsp;&nbsp;TIN NO&nbsp;:&nbsp;");
      String tinNum = "";
      /*
       * if (findCustomerByEmailList.getLength() > 0)
       * {System.out.println(findCustomerByEmailList.getLength()); for (int k = 0; k <
       * findCustomerByEmailList.getLength(); k++) { if(getNodeValue("propname",
       * findCustomerByEmailList.item(k).getChildNodes()).equals("TAXID")) { tinNum =
       * getNodeValue("propvalue", findCustomerByEmailList.item(k).getChildNodes()); } } }
       */
      shippingAddress.append(tinNum);
      shippingAddress.append("</th></tr>");

      final StringBuilder itemsInfo = new StringBuilder();
      double subTotal = 0.0;
      double shipTotal = 0.0;
      double tax14 = 0.0;
      double tax5 = 0.0;
      int part = 0;
      int totalQty = 0;
      if (omniOrder.getOrderLineList().size() > 0) {
        for (OrderLine iLine : omniOrder.getOrderLineList()) {
          part++;
          BigDecimal qty = iLine.getInvoicedQuantity();
          // BigDecimal unitPrice = iLine.getUnitPrice();
          BigDecimal unitPrice = getUnitPriceFromRetail(omniOrder.getSwPoReference(),
              iLine.getProduct());
          String desc = "";
          Double delCharges = 0.0;
          totalQty = totalQty + iLine.getInvoicedQuantity().intValue();
          String Qty;
          if (iLine.getInvoicedQuantity().intValue() == 0) {
            Qty = "0*";
          } else {
            Qty = "" + iLine.getInvoicedQuantity().intValue();
          }
          itemsInfo.append("<tr>");
          itemsInfo
              .append("<td style='width:15%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
                  + iLine.getProduct().getName() + "</td>");
          itemsInfo
              .append("<td style='width:5%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
                  + part + "</td>");
          itemsInfo
              .append("<td style='width:20%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
                  + desc + "</td>");
          itemsInfo
              .append("<td style='width:3%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
                  + Qty + "</td>");
          itemsInfo
              .append("<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
                  + df.format(iLine.getUnitPrice()) + "</td>");
          /*
           * itemsInfo .append(
           * "<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
           * + df.format(unitPrice.multiply(qty)) + "</td>"); itemsInfo .append(
           * "<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
           * + iLine.getTax().getName() + "</td>"); itemsInfo .append(
           * "<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-right:1px solid #000;border-bottom:1px solid #000;'>"
           * + df.format(0.0) + "</td>"); itemsInfo .append(
           * "<td style='width:10%;height:24px;line-height:15px;text-align:center;font-size:11px;border-bottom:1px solid #000;'>"
           * + 0.0 + (unitPrice.multiply(qty)) + "</td>");
           */
          itemsInfo.append("</tr>");
          subTotal = subTotal + (0.0 + unitPrice.multiply(qty).doubleValue());
          shipTotal = shipTotal + Double.parseDouble(iLine.getChargeAmount().toString());
          if (iLine.getTax().getName().equals("VAT 5.50%")) {
            tax5 = Math.round(tax5 + (delCharges * Double.parseDouble(qty.toString())));
          } else if (iLine.getTax().getName().equals("VAT 14.50%")) {
            tax14 = Math.round(tax14 + (delCharges * Double.parseDouble(qty.toString())));
          }
        }
      }

      double invtotal = subTotal + tax14 + tax5;

      result.put("logourl", "http://www.northcheshirebowmen.freeserve.co.uk/decathlon_logo.gif");
      result.put("invoiceNum", omniOrder.getDocumentNo());
      result.put("orderDate", omniOrder.getOrderDate().toString().substring(0, 16));
      result.put("noOfItems", omniOrder.getOrderLineList().size());
      result.put("billAddress", shippingAddress.toString());
      result.put("shipAddress", shippingAddress.toString());
      result.put("itemInfo", itemsInfo.toString());
      result.put("subTotal", subTotal);
      result.put("tax14", tax14);
      result.put("tax5", tax5);
      result.put("shipTotal", Math.round(tax14 + tax5));
      result.put("invTotal", Math.round(invtotal));
      result.put("totalInvQty", totalQty);
      String dateOfIssue = omniOrder.getOrderDate().toString().substring(0, 10);
      result.put("dateOfIssue", dateOfIssue);
      result.put("place", dateOfIssue);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return result;
  }

  private BigDecimal getUnitPriceFromRetail(String docNo, Product prd) {
    long unitPrice = 0;
    BigDecimal uPrice = new BigDecimal(0);
    Connection conn = null;
    try {
      conn = PassiveDB.getInstance().getRetailDBConnection();
      if (conn == null) {
        throw new OBException("Retail DB connection failed");
      }
    } catch (ClassNotFoundException e) {
      log.error(e);
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
      log.error(e);
    }
    PreparedStatement pst = null;
    ResultSet rs = null;

    try {
      pst = conn
          .prepareStatement("select pricestd from c_orderline where c_order_id in (select c_order_id from c_order where documentno = '"
              + docNo + "') and m_product_id = '" + prd.getId() + "'");

      rs = pst.executeQuery();
      while (rs.next()) {
        unitPrice = rs.getLong("pricestd");
        System.out.println("unitPrice->" + unitPrice);
        System.out.println("unitPrice->" + uPrice);

        uPrice = new BigDecimal(unitPrice);
      }

    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (pst != null && rs != null) {
          pst.close();
          rs.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return uPrice;
  }

  protected HttpURLConnection createConnection(Order iOrder) throws Exception {

    String customerDBURL = "";
    String customerDBUName = "";
    String customerDBPwd = "";
    OBContext.setAdminMode();
    final Map<String, String> custmerDBConfig = new HashMap<String, String>();
    OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(
        DSIDEFModuleConfig.class);
    configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME,
        "in.decathlon.customerdb"));
    if (configInfoObCriteria.count() > 0) {
      for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
        custmerDBConfig.put(config.getKey(), config.getSearchKey());
      }
      customerDBURL = custmerDBConfig.get("customerdbWSURL");
      customerDBUName = custmerDBConfig.get("custSearchId");
      customerDBPwd = custmerDBConfig.get("custSearchPwd");
    }
    OBContext.restorePreviousMode();

    URL url = null;
    if (iOrder.getRCOxylaneno() != null) {
      url = new URL(customerDBURL + "/dsiCustomerdbGet?decathlonId=" + iOrder.getRCOxylaneno()
          + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
      log.info(customerDBURL + "/dsiCustomerdbGet?decathlonId=" + iOrder.getRCOxylaneno()
          + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
    }

    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    hc.setRequestMethod("GET");
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    return hc;
  }
  
  protected HttpURLConnection createConnectionForAddress(Order iOrder) throws Exception {

	    String customerDBURL = "";
	    String customerDBUName = "";
	    String customerDBPwd = "";
	    OBContext.setAdminMode();
	    final Map<String, String> custmerDBConfig = new HashMap<String, String>();
	    OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(
	        DSIDEFModuleConfig.class);
	    configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME,
	        "in.decathlon.customerdb"));
	    if (configInfoObCriteria.count() > 0) {
	      for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
	        custmerDBConfig.put(config.getKey(), config.getSearchKey());
	      }
	      customerDBURL = custmerDBConfig.get("customerdbWSURL");
	      customerDBUName = custmerDBConfig.get("custSearchId");
	      customerDBPwd = custmerDBConfig.get("custSearchPwd");
	    }
	    OBContext.restorePreviousMode();

	    URL url = null;
	    if (iOrder.getRCOxylaneno() != null) {
	      url = new URL(customerDBURL + "/dsiCustomerAddressGet?addressId=" + iOrder.getNsiShippingAddressid()
	          + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
	      log.info(customerDBURL + "/dsiCustomerAddressGet?addressId=" + iOrder.getNsiShippingAddressid()
	          + "&username=" + customerDBUName + "&pwd=" + customerDBPwd + "");
	    }

	    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
	    hc.setRequestMethod("GET");
	    hc.setAllowUserInteraction(false);
	    hc.setDefaultUseCaches(false);
	    hc.setDoInput(true);
	    hc.setInstanceFollowRedirects(true);
	    hc.setUseCaches(false);
	    return hc;
	  }

}
