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
 * This Web Service is used to fetch the data for calculating Stock Reliability
 */

public class StockReliabilityWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(StockReliabilityWebService.class);

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
			jsonDataObject = this.parseStockRelXML(document);
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
	 * @param stockRelXML
	 * @return jsonDataObject
	 */
	public JSONObject parseStockRelXML(Document stockRelXML) throws Exception {
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		String locatorId = null;
		String orgId = null;
		String brandId = null;
		String strFromDate = null;
		String strToDate = null;
		String invType = null;
		java.sql.Date firstDate = null;
		int openingStock = 0;
		int replenishmentStock = 0;
		int stockReliability = 0;
		PreparedStatement firstInvDatePS = null, openingStockPS = null, replenishmentPS = null;
		ResultSet firstInvDateRS = null, openingStockRS = null, replenishmentRS = null;
		Connection connection = null;
		StringBuffer firstInvDateQuery = new StringBuffer();
		StringBuffer openingStockQuery = new StringBuffer();
		StringBuffer replenishmentQuery = new StringBuffer();

		try {
			locatorId = stockRelXML.getElementsByTagName("m_locator_id").item(0).getChildNodes().item(0).getNodeValue();
			orgId = stockRelXML.getElementsByTagName("ad_org_id").item(0).getChildNodes().item(0).getNodeValue();
			strFromDate = stockRelXML.getElementsByTagName("fromdate").item(0).getChildNodes().item(0).getNodeValue();
			strToDate = stockRelXML.getElementsByTagName("todate").item(0).getChildNodes().item(0).getNodeValue();
			invType = stockRelXML.getElementsByTagName("inventorytype").item(0).getChildNodes().item(0).getNodeValue();

			if (stockRelXML.getElementsByTagName("cl_brand_id").item(0) != null) {
				brandId = stockRelXML.getElementsByTagName("cl_brand_id").item(0).getChildNodes().item(0)
						.getNodeValue();
			}
			firstInvDateQuery.append("select mi.movementdate from m_inventory mi ");
			if (brandId != null) {
				firstInvDateQuery.append("join m_inventoryline mil on mil.m_inventory_id = mi.m_inventory_id ")
						.append("join m_product mp on mp.m_product_id = mil.m_product_id ")
						.append("join cl_model cm on cm.cl_model_id= mp.em_cl_model_id ")
						.append("join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id ");

			}
			firstInvDateQuery.append("where mi.ad_org_id= ? and mi.movementdate >= ? and mi.movementdate < ? ");
			if (brandId != null) {
				firstInvDateQuery.append("and cd.cl_brand_id = ? ");
			}
			firstInvDateQuery.append("limit 1");

			connection = PassiveDB.getInstance().getConnection();

			firstInvDatePS = connection.prepareStatement(firstInvDateQuery.toString());
			firstInvDatePS.setString(1, orgId);
			firstInvDatePS.setDate(2, new java.sql.Date(dateFormatter.parse(strFromDate).getTime()));
			firstInvDatePS.setDate(3, new java.sql.Date(dateFormatter.parse(strToDate).getTime()));
			if (brandId != null) {
				firstInvDatePS.setString(4, brandId);
			}

			firstInvDateRS = firstInvDatePS.executeQuery();
			if (firstInvDateRS.next()) {
				firstDate = firstInvDateRS.getDate("movementdate");
			}

			if (firstDate != null) {
				openingStockQuery.append("select sum(mt.movementqty) as movementqty from m_transaction mt ");
				if (brandId != null) {
					openingStockQuery.append("join m_product mp on mp.m_product_id = mt.m_product_id ")
							.append("join cl_model cm on cm.cl_model_id= mp.em_cl_model_id ")
							.append("join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id ");
				}
				openingStockQuery.append("where mt.m_locator_id= ? and mt.movementdate < ? ");
				if (brandId != null) {
					openingStockQuery.append("and cd.cl_brand_id = ? ");
				}

				openingStockPS = connection.prepareStatement(openingStockQuery.toString());
				openingStockPS.setString(1, locatorId);
				openingStockPS.setDate(2, firstDate);
				if (brandId != null) {
					openingStockPS.setString(3, brandId);
				}

				openingStockRS = openingStockPS.executeQuery();
				if (openingStockRS.next()) {
					openingStock = openingStockRS.getInt("movementqty");
				}

				if (invType.contains("PI")) {
					replenishmentQuery.append("select sum(mt.movementqty) as movementqty from m_transaction mt ");
					if (brandId != null) {
						replenishmentQuery.append("join m_product mp on mp.m_product_id = mt.m_product_id ")
								.append("join cl_model cm on cm.cl_model_id= mp.em_cl_model_id ")
								.append("join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id ");
					}
					replenishmentQuery
							.append("where mt.m_locator_id= ? and mt.movementdate >= ? and mt.movementdate < ? ");
					if (brandId != null) {
						replenishmentQuery.append("and cd.cl_brand_id = ? ");
					}

					replenishmentPS = connection.prepareStatement(replenishmentQuery.toString());
					replenishmentPS.setString(1, locatorId);
					replenishmentPS.setDate(2, firstDate);
					replenishmentPS.setDate(3, new java.sql.Date(dateFormatter.parse(strToDate).getTime()));
					if (brandId != null) {
						replenishmentPS.setString(4, brandId);
					}

					replenishmentRS = replenishmentPS.executeQuery();
					if (replenishmentRS.next()) {
						replenishmentStock = replenishmentRS.getInt("movementqty");
					}
				}
				stockReliability = openingStock + replenishmentStock;
			}

			jsonDataObject.put("stockreliability", stockReliability);
			jsonDataObject.put("message", "Stock Reliability data fetched");
			jsonDataObject.put("status", "success");
		} catch (Exception exp) {
			LOGGER.error("Exception while reading Stock Reliability: ", exp);
			jsonDataObject.put("message", exp.getMessage());
			jsonDataObject.put("status", "failure");
		} finally {
			if (firstInvDatePS != null) {
				try {
					firstInvDatePS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing firstInvDatePS in StockReliabilityWebService", e2);
				}
			}
			if (openingStockPS != null) {
				try {
					openingStockPS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing openingStockPS in StockReliabilityWebService", e2);
				}
			}
			if (replenishmentPS != null) {
				try {
					replenishmentPS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing replenishmentPS in StockReliabilityWebService", e2);
				}
			}
			if (firstInvDateRS != null) {
				try {
					firstInvDateRS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing firstInvDateRS in StockReliabilityWebService", e2);
				}
			}
			if (openingStockRS != null) {
				try {
					openingStockRS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing openingStockRS in StockReliabilityWebService", e2);
				}
			}
			if (replenishmentRS != null) {
				try {
					replenishmentRS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing replenishmentRS in StockReliabilityWebService", e2);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing connection in StockReliabilityWebService", e2);
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
