package in.nous.tools.mrpsticker.webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import java.util.Date;

/**
 * 
 * @author administrator
 *
 */

public class PostBarcodeDetailsForLocation extends HttpSecureAppServlet implements
		WebService {

	private static final long serialVersionUID = 1L;

	static Logger log4j = Logger.getLogger(PostBarcodeDetailsForLocation.class);

	/**
	 * 
	 * @param path
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

	}

	/**
	 * 
	 * @param path
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String xml = "";
		String strRead = "";

		InputStreamReader isReader = new InputStreamReader(
				request.getInputStream());
		BufferedReader bReader = new BufferedReader(isReader);
		StringBuilder strBuilder = new StringBuilder();
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

		JSONObject jsonDataObject = parseXML(document);

		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(jsonDataObject.toString());
		w.close();
	}
	
	/**
	 * 
	 * @param path
	 * @param request
	 * @param response
	 * @throws Exception
	 */

	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	/**
	 * 
	 * @param path
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	/**
	 * 
	 * @param xml
	 * @return
	 */
	
	public static JSONObject parseXML(Document xml) {
		String locationNumbers = null;
		String barCodes = null;

		JSONObject jsonDataObject = new JSONObject();

		try {

			locationNumbers = xml.getElementsByTagName("Location").item(0)
					.getChildNodes().item(0).getNodeValue();
			//System.out.println("Box Numbers are :" + locationNumbers);
			List<String> locationList = new ArrayList<String>(
					Arrays.asList(locationNumbers.split(",")));
			//System.out.println("Box List :" + boxList);

			barCodes = xml.getElementsByTagName("Barcode").item(0)
					.getChildNodes().item(0).getNodeValue();
			//System.out.println("Bar codes are:" + barCodes);
			List<String> barCodeList = new ArrayList<String>(
					Arrays.asList(barCodes.split(",")));
			//System.out.println("Bar Code List :" + barCodeList);

			for (int index = 0; index < locationList.size(); index++) {

				insertToDSCWHLocationTable(barCodeList.get(index),
						locationList.get(index));

			}

			jsonDataObject.put("status", "Success");

		} catch (Exception exp) {
			log4j.error(
					"Exception while reading Stikcer Data from DSCWHLocation Table:",
					exp);
			exp.printStackTrace();
			return jsonDataObject;
		}

		return jsonDataObject;

	}
	
	/**
	 * 
	 * @param barCode
	 * @param boxNumber
	 */

	public static void insertToDSCWHLocationTable(String barCode, String locationNumber) {
		
		final Session session = OBDal.getInstance().getSession();
		
		String queryString = "insert into dsc_wh_location(dsc_wh_location_id, ad_client_id, ad_org_id, "
				+ " created, createdby, updated, updatedby, location_barcode, location_no) "
				+ " values(get_uuid(), :ad_client_id, :ad_org_id, "
				+ " :created, :createdby, :updated, :updatedby, :location_barcode, :location_no)";

		SQLQuery insertQuery = session.createSQLQuery(queryString);
		
		try {

			insertQuery.setString("ad_client_id","0");
			
			insertQuery.setString("ad_org_id", "0");

			insertQuery.setDate("created", new Date());
			
			insertQuery.setString("createdby", "0");
			
			insertQuery.setDate("updated", new Date());

			insertQuery.setString("updatedby", "0");

			insertQuery.setString("location_barcode",barCode);
			
			insertQuery.setString("location_no",locationNumber);

			int status = insertQuery.executeUpdate();

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.error("Exception happend while saving into dsc_wh_location table::"
					+ ex);
		}

	}

}

