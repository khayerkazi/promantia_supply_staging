package in.nous.creditnote.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
 * The Web Service generates the Credit Note Report
 * 
 */

public class CreditNoteReportWebService extends HttpSecureAppServlet implements WebService {

  private static Logger LOGGER = Logger.getLogger(CreditNoteReportWebService.class);

  private static SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");

  private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static SimpleDateFormat sdf3 = new SimpleDateFormat("dd/MM/yyyy");

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
   * to credit note report details.
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
      jsonDataObject = parseCreditNoteReportXML(document);
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
   * This method parses the xml tags and returns the json object containing the credit note report
   * details
   * 
   * @param crnReportXML
   * @return jsonDataObject
   */
  public JSONObject parseCreditNoteReportXML(Document crnReportXML) throws Exception {
    JSONObject jsonDataObject = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    String orgId = null;
    String fromDate = null;
    String toDate = null;

    try {
      orgId = crnReportXML.getElementsByTagName("orgId").item(0).getChildNodes().item(0)
          .getNodeValue();
      fromDate = crnReportXML.getElementsByTagName("fromDate").item(0).getChildNodes().item(0)
          .getNodeValue();
      toDate = crnReportXML.getElementsByTagName("toDate").item(0).getChildNodes().item(0)
          .getNodeValue();

      LOGGER.debug("Input received for Credit Note Report: orgId, fromDate, toDate : " + orgId
          + ", " + fromDate + ", " + toDate);

      // Increment the To Date by one day
      Calendar c = Calendar.getInstance();
      c.setTime(sdf3.parse(toDate));
      c.add(Calendar.DATE, 1);
      String toDatePlusOne = sdf3.format(c.getTime());

      String sqlQuery = "select coalesce(o.em_ncn_crtype,rr.name) as creditnotetype, inv.documentno as documentno,"
          + " inv.dateinvoiced as date, p.name as itemcode, abs(ol.qtyordered) as qty, ol.priceactual as amount,"
          + " abs(ol.line_gross_amount) as totalamount, coalesce(o.em_rc_oxylaneno,'') as decathlonid, coalesce(o.em_rc_mobileno,'') as mobileno,"
          + " coalesce(o.em_sync_email,'') as email, coalesce(o.em_sync_landline,'') as landline, ad.name as org, coalesce(o.em_ncn_printcount,'0') as printcount,ct.rate as taxrate"
          + " from c_invoice inv"
          + " join c_order o on o.c_order_id=inv.c_order_id"
          + " join c_orderline ol on ol.c_order_id=o.c_order_id"
          + " join m_inout io on io.c_order_id=o.c_order_id"
          + " join c_return_reason rr on rr.c_return_reason_id=o.c_return_reason_id"
          + " join m_product p on p.m_product_id=ol.m_product_id"
          + " join ad_org ad on ad.ad_org_id = o.ad_org_id"
          + " join c_tax ct on ct.c_tax_id = ol.c_tax_id"
          + " join c_doctype cd on cd.c_doctype_id = o.c_doctype_id"
          + " where inv.issotrx=? and o.issotrx=? and io.issotrx=? and o.docstatus!='VO'"
          + " and abs(ol.qtyordered) > ?"
          + " and inv.created >= ?"
          + " and inv.created < ?"
          + " and cd.name=?" + " and ad.ad_org_id =?" + " order by inv.dateinvoiced";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setString(0, "Y");
      query.setString(1, "Y");
      query.setString(2, "Y");
      query.setInteger(3, 0);
      query.setDate(4, new java.sql.Date(sdf3.parse(fromDate).getTime()));
      query.setDate(5, new java.sql.Date(sdf3.parse(toDatePlusOne).getTime()));
      query.setString(6, "Credit Note Order");
      query.setString(7, orgId);

      List<Object[]> reportList = query.list();
      // System.out.println("sqlQuery " + sqlQuery);

      jsonArray = getJsonFromList(reportList);
      jsonDataObject.put("data", jsonArray);
      jsonDataObject.put("status", "success");

    } catch (Exception exp) {
      LOGGER.error("Exception while fetching Credit Note Report. ", exp);
      jsonDataObject.put("status", "failure");
    }

    return jsonDataObject;
  }

  /**
   * This method converts list of data resuts from DB to json array
   * 
   * @param reportList
   * @return jsonArray
   */
  private JSONArray getJsonFromList(List<Object[]> reportList) throws Exception {
    JSONArray jsonArray = new JSONArray();
    for (Object[] objects : reportList) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("crntype", objects[0].toString());
      jsonObject.put("docno", objects[1].toString());
      jsonObject.put("date", sdf1.format(sdf2.parse(objects[2].toString())));
      jsonObject.put("itc", objects[3].toString());
      jsonObject.put("qty", objects[4].toString());
      jsonObject.put("amt", objects[5].toString());
      jsonObject.put("totalamt", objects[6].toString());
      jsonObject.put("decid", objects[7].toString());
      jsonObject.put("mobile", objects[8].toString());
      jsonObject.put("email", objects[9].toString());
      jsonObject.put("landline", objects[10].toString());
      jsonObject.put("org", objects[11].toString());
      jsonObject.put("printcount", objects[12].toString());
      jsonObject.put("taxrate", objects[13].toString());
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

}
