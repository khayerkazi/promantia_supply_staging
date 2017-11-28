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
 * The Web Service mainatins the Credit Note Invoice print count
 * 
 */

public class PrintCountWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(PrintCountWebService.class);

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
	 * the XML Schema related to credit note invoice print count details.
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

			LOGGER.debug("Input received for Print count : docNo : " + docNo);

			String custSQLQuery = "update c_order set em_ncn_printcount=(1+ "
					+ "(select coalesce(em_ncn_printcount,0) from c_order where documentno=?)) where documentno=?";

			SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(custSQLQuery);
			query.setString(0, docNo);
			query.setString(1, docNo);

			query.executeUpdate();

			jsonDataObject.put("status", "success");
		} catch (Exception exp) {
			LOGGER.error("Exception while updating print count. ", exp);
			jsonDataObject.put("status", "failure");
		}

		return jsonDataObject;
	}
}
