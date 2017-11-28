package in.nous.creditnote.ad_webservices;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
 * The Web Service reprints the old Credit Note Invoice
 * 
 */

public class ReprintInvoiceWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(ReprintInvoiceWebService.class);
	
	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Performs the POST REST operation. This service handles the request for
	 * the XML Schema related to credit note reprint invoice details.
	 * 
	 * @param path
	 *            the HttpRequest.getPathInfo(), the part of the url after the
	 *            context path
	 * @param request
	 *            the HttpServletRequest
	 * @param response
	 *            the HttpServletResponse
	 */
	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
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
			jsonDataObject = parseReprintXML(document);
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
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();

	}

	/**
	 * This method parses the xml tags and returns the json object containing
	 * the credit note report details
	 * 
	 * @param reprintXML
	 * @return jsonDataObject
	 */
	public JSONObject parseReprintXML(Document reprintXML) throws Exception {
		JSONObject jsonDataObject = new JSONObject();
		String docNo = null;
		
		try {
			docNo = reprintXML.getElementsByTagName("docNo").item(0).getChildNodes().item(0).getNodeValue();

			LOGGER.debug("Input received for Reprint Invoices: docNo : " + docNo );

			String custSQLQuery = "select coalesce(co.em_rc_oxylaneno,'') as decathlonid, coalesce(co.em_rc_mobileno,'') as mobileno, " 
			+ "coalesce(co.em_sync_landline,'') as landline, coalesce(co.em_sync_email,'') as email " 
			+ "from c_order co "
			+ "where co.documentno=?";

			SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(custSQLQuery);
			query.setString(0, docNo);
			
			List<Object[]> custList = query.list();

			
			String invoiceSQLQuery = "select co.documentno as invoiceno, to_char(col.dateordered,'DD-MM-YYYY') as creditnotedate , mp.name as itemcode, " 
			+ "abs(col.qtyordered) as qty, col.priceactual as price, col.line_gross_amount as total, "
			+ "(case rr.name when 'Customer Exchange' then 'Change of product' when 'Customer Defective' then 'Defective' else rr.name end) as reason "
			+ "from c_order co "
			+ "join c_orderline col on col.c_order_id=co.c_order_id "
			+ "join m_product mp on mp.m_product_id = col.m_product_id "
			+ "join c_return_reason rr on rr.c_return_reason_id=co.c_return_reason_id " 
			+ "where co.documentno=? order by col.line";

			SQLQuery query2 = OBDal.getInstance().getSession().createSQLQuery(invoiceSQLQuery);
			query2.setString(0, docNo);
			
			List<Object[]> invoiceList = query2.list();
			
			jsonDataObject.put("customer", getJsonFromList(custList));
			jsonDataObject.put("invoices", getJsonArrayFromList(invoiceList));
			jsonDataObject.put("status", "success");

		} catch (Exception exp) {
			LOGGER.error("Exception while fetching Reprint Invoices. ", exp);
			jsonDataObject.put("status", "failure");
		}

		return jsonDataObject;
	}

	/**
	 * This method converts list of customer data resuts from DB to json object
	 * 
	 * @param custList
	 * @return jsonObject
	 */
	private JSONObject getJsonFromList(List<Object[]> custList) throws Exception {
		JSONObject jsonObject = new JSONObject();
		for (Object[] objects : custList) {
			jsonObject.put("decathlonid", objects[0].toString());
			jsonObject.put("mobileno", objects[1].toString());
			jsonObject.put("landline", objects[2].toString());
			jsonObject.put("email", objects[3].toString());
		}
		return jsonObject;
	}

	/**
	 * This method converts list of invoice data resuts from DB to json array
	 * 
	 * @param invoiceList
	 * @return jsonArray
	 */
	private JSONArray getJsonArrayFromList(List<Object[]> invoiceList) throws Exception {
		JSONArray jsonArray = new JSONArray();
		for (Object[] objects : invoiceList) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("Invoice", objects[0].toString());
			jsonObject.put("Date", objects[1].toString());
			jsonObject.put("Item Code", objects[2].toString());
			jsonObject.put("Exchg Qty", objects[3].toString());
			jsonObject.put("CC Unit Price", objects[4].toString());
			jsonObject.put("Exchg Amt", objects[5].toString());
			jsonObject.put("Reason", objects[6].toString());
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}
}

