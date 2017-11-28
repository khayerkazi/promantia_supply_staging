package in.decathlon.retail.inventorycorrectiontool.ad_webservices;

import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/*
 * This Web Service is used to fetch the product details of previously inventoried items
 */

public class PastInventoryWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(PastInventoryWebService.class);

	private static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

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
	 * the XML Schema related to stock details.
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
			jsonDataObject = this.parsePastInvXML(document);
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
	 * the past inventory details
	 * 
	 * @param pastInvXML
	 * @return jsonDataObject
	 */
	public JSONObject parsePastInvXML(Document pastInvXML) throws Exception {
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		String locatorId = null;
		String orgId = null;
		String brandId = null;
		String strFromDate = null;
		String strToDate = null;
		String invType = null;
		PreparedStatement pastInvPS1 = null;
		ResultSet pastInvRS1 = null;
		Connection connection = null;
		StringBuffer pastInvQuery = new StringBuffer();

		pastInvQuery
				.append("select au.name as createdby, mi.movementdate as date, cd.name as brand, mp.name as itemcode, ")
				.append("qtycount as inventoryqty, mpp.em_cl_cessionprice as cessionprice, mil.qtybook as erpqty, cd.cl_brand_id as brndid ")
				.append("from m_inventory mi ")
				.append("join m_inventoryline mil on mil.m_inventory_id = mi.m_inventory_id ")
				.append("join ad_user au on au.ad_user_id = mi.createdby ")
				.append("join m_product mp on mp.m_product_id = mil.m_product_id ")
				.append("join cl_model cm on cm.cl_model_id= mp.em_cl_model_id ")
				.append("join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id ")
				.append("join m_productprice mpp on mpp.m_product_id = mp.m_product_id ")
				.append("join m_pricelist_version mpv on mpv.m_pricelist_version_id = mpp.m_pricelist_version_id ")
				.append("where mi.movementdate >= ? and mi.movementdate < ? ").append("and mi.ad_org_id=? ")
				.append("and mpv.name=? ").append("and mi.em_sw_movementtype in (");

		try {
			locatorId = pastInvXML.getElementsByTagName("m_locator_id").item(0).getChildNodes().item(0).getNodeValue();
			orgId = pastInvXML.getElementsByTagName("ad_org_id").item(0).getChildNodes().item(0).getNodeValue();
			strFromDate = pastInvXML.getElementsByTagName("fromdate").item(0).getChildNodes().item(0).getNodeValue();
			strToDate = pastInvXML.getElementsByTagName("todate").item(0).getChildNodes().item(0).getNodeValue();
			invType = pastInvXML.getElementsByTagName("inventorytype").item(0).getChildNodes().item(0).getNodeValue();

			if (pastInvXML.getElementsByTagName("cl_brand_id").item(0) != null) {
				brandId = pastInvXML.getElementsByTagName("cl_brand_id").item(0).getChildNodes().item(0)
						.getNodeValue();
			}

			String invs[] = invType.split(",");
			for (int i = 0; i < invs.length; i++) {
				pastInvQuery.append("?" + (invs.length == i + 1 ? ")" : ","));
			}

			if (brandId != null) {
				pastInvQuery.append(" and cd.cl_brand_id = ? ");
			}

			pastInvQuery.append(" order by cd.name ");
			
			connection = PassiveDB.getInstance().getConnection();

			pastInvPS1 = connection.prepareStatement(pastInvQuery.toString());
			pastInvPS1.setDate(1, new java.sql.Date(dateFormatter.parse(strFromDate).getTime()));
			pastInvPS1.setDate(2, new java.sql.Date(dateFormatter.parse(strToDate).getTime()));
			pastInvPS1.setString(3, orgId);
			pastInvPS1.setString(4, "DMI CATALOGUE");

			int index = 5;
			for (String inv : invs) {
				pastInvPS1.setString(index, inv.trim());
				index++;
			}

			if (brandId != null) {
				pastInvPS1.setString(index, brandId);
			}

			pastInvRS1 = pastInvPS1.executeQuery();
			jsonArray = this.getJsonFromList(pastInvRS1);

			jsonDataObject.put("data", jsonArray);
			jsonDataObject.put("message", "Past inventory data fetched");
			jsonDataObject.put("status", "success");

		} catch (Exception exp) {
			LOGGER.error("Exception while reading past inventory details: ", exp);
			jsonDataObject.put("message", exp.getMessage());
			jsonDataObject.put("status", "failure");
		} finally {
			if (pastInvPS1 != null) {
				try {
					pastInvPS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing pastInvPS1 in PastInventoryWebService", e2);
				}
			}
			if (pastInvRS1 != null) {
				try {
					pastInvRS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing pastInvRS1 in PastInventoryWebService", e2);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing connection in PastInventoryWebService", e2);
				}
			}
		}
		return jsonDataObject;

	}

	/**
	 * This method converts list of data resuts from DB to json array
	 * 
	 * @param stockList
	 * @return jsonArray
	 */
	private JSONArray getJsonFromList(ResultSet rs) throws Exception {
		JSONArray jsonArray = new JSONArray();
		while (rs.next()) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("createdby", rs.getString("createdby"));
			jsonObject.put("date", rs.getString("date"));
			jsonObject.put("brnd", rs.getString("brand"));
			jsonObject.put("itc", rs.getString("itemcode"));
			jsonObject.put("invqty", rs.getString("inventoryqty"));
			jsonObject.put("cp", rs.getString("cessionprice"));
			jsonObject.put("erpqty", rs.getString("erpqty"));
			jsonObject.put("brndid", rs.getString("brndid"));
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}
}
