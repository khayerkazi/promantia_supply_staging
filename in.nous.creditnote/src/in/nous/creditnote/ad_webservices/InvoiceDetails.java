package in.nous.creditnote.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * This Web Service is used to fetch the invoice details of a customer for an item purchased
 * 
 */

public class InvoiceDetails extends HttpSecureAppServlet implements WebService {

  private static Logger LOGGER = Logger.getLogger(InvoiceDetails.class);

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

  private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String SOTRX_N = "N";

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs the POST REST operation. This service handles the request for the XML Schema related
   * to invoice details.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String xml = "";
    String strRead = "";

    InputStreamReader isReader = new InputStreamReader(request.getInputStream());
    BufferedReader bReader = new BufferedReader(isReader);
    StringBuilder strBuilder = new StringBuilder();
    JSONObject jsonDataObject = new JSONObject();
    strRead = bReader.readLine();
    while (strRead != null) {
      strBuilder.append(strRead);
      strRead = bReader.readLine();
    }
    xml = strBuilder.toString();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);

    try {
      jsonDataObject = parseInvoiceXML(document);
    } catch (Exception e) {
      LOGGER.error(e);
    }
    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());
    w.close();

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  /**
   * This method parses the xml tags and returns the json object containing the invoice details
   * 
   * @param invoiceXML
   * @return jsonDataObject
   */
  public JSONObject parseInvoiceXML(Document invoiceXML) throws Exception {
    JSONObject jsonDataObject = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    String decathlonId = null;
    String itemCode = null;
    String invoiceNum = null;
    String docType = null;
    String orgId = null;
    String soTrx = null;
    String ean = null;
    String excludeInvoices = null;
    String excludeInvoice[] = null;

    try {
      decathlonId = invoiceXML.getElementsByTagName("decathlonId").item(0).getChildNodes().item(0)
          .getNodeValue();
      itemCode = invoiceXML.getElementsByTagName("itemCode").item(0).getChildNodes().item(0)
          .getNodeValue();
      docType = invoiceXML.getElementsByTagName("docType").item(0).getChildNodes().item(0)
          .getNodeValue();
      orgId = invoiceXML.getElementsByTagName("priceVersion").item(0).getChildNodes().item(0)
          .getNodeValue();
      soTrx = invoiceXML.getElementsByTagName("soTrx").item(0).getChildNodes().item(0)
          .getNodeValue();
      ean = invoiceXML.getElementsByTagName("ean").item(0).getChildNodes().item(0).getNodeValue();

      if (null != invoiceXML.getElementsByTagName("invoiceNum").item(0)) {
        invoiceNum = invoiceXML.getElementsByTagName("invoiceNum").item(0).getChildNodes().item(0)
            .getNodeValue();
      }
      if (null != invoiceXML.getElementsByTagName("excludeInvoices").item(0)) {
        excludeInvoices = invoiceXML.getElementsByTagName("excludeInvoices").item(0)
            .getChildNodes().item(0).getNodeValue();
      }
      if (null != excludeInvoices) {
        invoiceNum = null;
      }
      LOGGER.debug("Query for invoice details received: ");
      LOGGER.debug("soTrx, decathlonId, itemCode, docType, invoiceNum, excludeInvoices, ean "
          + soTrx + ", " + decathlonId + ", " + itemCode + ", " + docType + ", " + invoiceNum
          + ", " + excludeInvoices + ", " + ean);

      String sqlQuery = "select co.documentno as billnum,co.dateordered as billdate,col.qtyordered as purchaseqty,col.priceactual as unitprice,col.em_ds_linenetamt as totalamt,"
          + "mp.name as itemcode,mp.em_cl_modelname as modelname,mp.em_cl_size as size, clc.name as color,"
          + "col.c_orderline_id as c_orderline_id,(select coalesce(abs(sum(qtyordered)),0) from c_orderline where em_ncn_invoice = col.c_orderline_id) as returnqty, "
          + "case coalesce(ad.description,'') when '' then ad.name else ad.description end as store, mpp.em_cl_ccunitprice as ccunitprice, mp.m_product_id as m_product_id "
          + "from c_order co "
          + "full outer join c_orderline col on col.c_order_id = co.c_order_id "
          + "full outer join m_product mp on mp.m_product_id = col.m_product_id "
          + "full outer join cl_color clc on clc.cl_color_id = mp.em_cl_color_id "
          + "full outer join  c_doctype cd on cd.c_doctype_id = co.c_doctype_id "
          + "full outer join ad_org ad on ad.ad_org_id = co.ad_org_id "
          + "full outer join m_productprice mpp on mpp.m_product_id = mp.m_product_id "
          + "full outer join m_pricelist_version mpv on mpv.m_pricelist_version_id = mpp.m_pricelist_version_id "
          + "where co.issotrx=? "
          + "and cd.name like ? "
          + "and mp.name=? "
          + "and co.em_rc_oxylaneno=? "
          + "and mpv.ad_org_id=? "
          + "and co.documentno=? "
          + "and col.qtyordered > (select coalesce(abs(sum(qtyordered)),0) from c_orderline where em_ncn_invoice = col.c_orderline_id) "
          + "order by co.dateordered desc limit 1";

      if (null == invoiceNum) {
        sqlQuery = sqlQuery.replace("and co.documentno=? ", "");
      }
      if (soTrx.equals(SOTRX_N)) {
        sqlQuery = sqlQuery.replace("and co.em_rc_oxylaneno=? ", "and co.em_ds_bpartner_id=? ");
        sqlQuery = sqlQuery.replace("co.documentno as billnum", "co.em_ds_receiptno as billnum");
        sqlQuery = sqlQuery.replace("and co.documentno=? ", "and co.em_ds_receiptno=? ");
        sqlQuery = sqlQuery.replace("and mpv.ad_org_id=? ",
            "and mpv.ad_org_id=? and col.em_ds_linenetamt > 0 ");
      }
      if (ean.equals("true")) {
        sqlQuery = sqlQuery.replace("and mp.name=? ", "and mp.upc=? ");
      }
      if (null != excludeInvoices) {
        excludeInvoice = excludeInvoices.split(",");
        StringBuffer strBuf = new StringBuffer("(");
        for (int i = 0; i < excludeInvoice.length; i++) {
          strBuf.append("?" + (excludeInvoice.length == i + 1 ? ") " : ","));
        }
        sqlQuery = sqlQuery.replace("order by", "and col.c_orderline_id not in " + strBuf
            + " order by");
      }

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setString(0, soTrx);
      query.setString(1, docType);
      query.setString(2, itemCode);
      query.setString(3, decathlonId);
      query.setString(4, orgId);
      if (null != invoiceNum) {
        query.setString(5, invoiceNum);
      }
      if (null != excludeInvoices) {
        for (int i = 0; i < excludeInvoice.length; i++) {
          query.setString(i + 5, excludeInvoice[i].trim());
        }
      }

      List<Object[]> invoiceList = query.list();
      if (null == invoiceList || invoiceList.isEmpty()) {
        if (null != invoiceNum) {
          if (soTrx.equals(SOTRX_N)) {
            sqlQuery = sqlQuery.replace("and co.em_ds_receiptno=? ", "");
          } else {
            sqlQuery = sqlQuery.replace("and co.documentno=? ", "");
          }

          query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
          query.setString(0, soTrx);
          query.setString(1, docType);
          query.setString(2, itemCode);
          query.setString(3, decathlonId);
          query.setString(4, orgId);
          invoiceList = query.list();
          if (null == invoiceList || invoiceList.isEmpty()) {
            // Check if Credit Note was created earlier
            sqlQuery = sqlQuery
                .replace(
                    "and col.qtyordered > (select coalesce(abs(sum(qtyordered)),0) from c_orderline where em_ncn_invoice = col.c_orderline_id)",
                    "");
            query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
            query.setString(0, soTrx);
            query.setString(1, docType);
            query.setString(2, itemCode);
            query.setString(3, decathlonId);
            query.setString(4, orgId);
            invoiceList = query.list();
            if (null == invoiceList || invoiceList.isEmpty()) {
              jsonDataObject.put("message", "No results found");
            } else {
              jsonDataObject.put("message", "Credit Note exists");
            }

          } else {
            jsonArray = getJsonFromList(invoiceList);
            jsonDataObject.put("data", jsonArray);
            jsonDataObject.put("message", "Results found");
          }
        } else {
          // Check if Credit Note was created earlier
          sqlQuery = sqlQuery
              .replace(
                  "and col.qtyordered > (select coalesce(abs(sum(qtyordered)),0) from c_orderline where em_ncn_invoice = col.c_orderline_id)",
                  "");
          query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
          query.setString(0, soTrx);
          query.setString(1, docType);
          query.setString(2, itemCode);
          query.setString(3, decathlonId);
          query.setString(4, orgId);
          if (null != invoiceNum) {
            query.setString(5, invoiceNum);
          }
          if (null != excludeInvoices) {
            for (int i = 0; i < excludeInvoice.length; i++) {
              query.setString(i + 5, excludeInvoice[i].trim());
            }
          }
          invoiceList = query.list();
          if (null == invoiceList || invoiceList.isEmpty()) {
            jsonDataObject.put("message", "No results found");
          } else {
            jsonDataObject.put("message", "Credit Note exists");
          }
        }
      } else {
        jsonArray = getJsonFromList(invoiceList);
        jsonDataObject.put("data", jsonArray);
        jsonDataObject.put("message", "Results found");
      }
      jsonDataObject.put("status", "success");
    } catch (Exception exp) {
      LOGGER.error("Exception while reading invoice details. ", exp);
      jsonDataObject.put("message", exp.getMessage());
      jsonDataObject.put("status", "failure");
    }

    return jsonDataObject;

  }

  /**
   * This method converts list of data resuts from Db to json array
   * 
   * @param invoiceList
   * @return jsonArray
   */
  private JSONArray getJsonFromList(List<Object[]> invoiceList) throws Exception {
    JSONArray jsonArray = new JSONArray();
    for (Object[] objects : invoiceList) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("billnum", objects[0].toString());
      jsonObject.put("billdate", sdf.format(sdf2.parse(objects[1].toString())));
      jsonObject.put("purchaseqty", objects[2].toString());
      jsonObject.put("unitprice", objects[3].toString());
      jsonObject.put("totalamt", objects[4].toString());
      jsonObject.put("itemcode", objects[5].toString());
      jsonObject.put("modelname", objects[6].toString());
      jsonObject.put("size", objects[7].toString());
      jsonObject.put("color", objects[8].toString());
      jsonObject.put("c_orderline_id", objects[9].toString());
      jsonObject.put("returnqty", objects[10].toString());
      jsonObject.put("store", objects[11].toString());
      jsonObject.put("ccunitprice", objects[12].toString());
      jsonObject.put("m_product_id", objects[13].toString());
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

}
