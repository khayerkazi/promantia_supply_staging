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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/*
 * This Web Service is used to fetch the stock of risky products' details of a particular brand
 * for a set of rules
 */

public class BrandStockAlertsWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(BrandStockAlertsWebService.class);

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
			jsonDataObject = this.parseAlertsXML(document);
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
	 * the alerts details
	 * 
	 * @param alertsXML
	 * @return jsonDataObject
	 */
	public JSONObject parseAlertsXML(Document alertsXML) throws JSONException {
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		String locatorId = null;
		String orgId = null;
		String brandId = null;
		String productIds = null;
		List<String> whProdList = null;
		String strDate = null;
		PreparedStatement rule1PS1 = null, rule1PS2 = null, rule1PS3 = null, rule2PS1 = null, rule3PS1 = null, rule4PS1 = null, prodDetailsPS = null;
		ResultSet rule1RS1 = null, rule1RS2 = null, rule1RS3 = null, rule2RS1 = null, rule3RS1 = null, rule4RS1 = null, prodDetailsRS = null;
		HashSet<String> brandProducts = new HashSet<String>();
		HashSet<String> negativeProductsOfLocation = new HashSet<String>();
		HashSet<String> negativeProductsOfBrandLocation = new HashSet<String>();
		HashSet<String> fourWeeksProducts = new HashSet<String>();
		HashSet<String> eightWeeksProducts = new HashSet<String>();
		HashSet<String> posSaleProducts = new HashSet<String>();
		HashSet<String> finalProducts = new HashSet<String>();
		String brandProductsQuery = "select m_product_id from m_product mp join cl_model cm on cm.cl_model_id= mp.em_cl_model_id join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id where cd.cl_brand_id = ?";
		String locatorProductsQuery = "select m_product_id from m_storage_detail where m_locator_id = ? and qtyonhand < ?";
		StringBuffer modelProductsQuery = new StringBuffer();
		StringBuffer fourWeeksRuleQuery = new StringBuffer();
		StringBuffer eightWeeksRuleQuery = new StringBuffer();
		StringBuffer posSaleRuleQuery = new StringBuffer();
		StringBuffer prodDetailsQuery = new StringBuffer();
		Connection connection = null;

		fourWeeksRuleQuery
				.append("select msd.m_product_id from m_storage_detail msd ")
				.append("join m_product mp on mp.m_product_id = msd.m_product_id ")
				.append("where msd.m_locator_id = ? ")
				.append("and mp.em_cl_lifestage = ? ")
				.append("and msd.qtyonhand = 1 ")
				.append("and msd.m_product_id not in ")
				.append("(select m_product_id from m_transaction mt where mt.movementdate BETWEEN CURRENT_DATE - INTERVAL '28 days' AND CURRENT_DATE+1 ")
				.append("and mt.m_locator_id = ?) ");

		eightWeeksRuleQuery
				.append("select msd.m_product_id from m_storage_detail msd ")
				.append("join m_product mp on mp.m_product_id = msd.m_product_id ")
				.append("where msd.m_locator_id = ? ")
				.append("and mp.em_cl_lifestage = ? ")
				.append("and msd.qtyonhand > 0 ")
				.append("and msd.m_product_id not in ")
				.append("(select m_product_id from m_transaction mt where mt.movementdate BETWEEN CURRENT_DATE - INTERVAL '56 days' AND CURRENT_DATE+1 ")
				.append("and mt.m_locator_id = ?) ");

		prodDetailsQuery
				.append("select mp.m_product_id as m_product_id,mp.upc as ean,mp.name as itemcode,mp.em_cl_modelname as modelname,cd.name as brand,mp.em_cl_size as size, ")
				.append("clc.name as color,mpp.em_cl_cessionprice as cessionprice,sd.qtyonhand as erp ")
				.append("from m_product mp ").append("join cl_model cm on cm.cl_model_id = mp.em_cl_model_id ")
				.append("join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id ")
				.append("join cl_color clc on clc.cl_color_id = mp.em_cl_color_id ")
				.append("join m_productprice mpp on mpp.m_product_id = mp.m_product_id ")
				.append("join m_pricelist_version mpv on mpv.m_pricelist_version_id = mpp.m_pricelist_version_id ")
				.append("join m_storage_detail sd on sd.m_product_id = mp.m_product_id ").append("where mpv.name= ? ")
				.append("and sd.m_locator_id = ?").append("and mp.m_product_id in (");

		try {
			locatorId = alertsXML.getElementsByTagName("m_locator_id").item(0).getChildNodes().item(0).getNodeValue();
			brandId = alertsXML.getElementsByTagName("cl_brand_id").item(0).getChildNodes().item(0)
					.getNodeValue();
			orgId = alertsXML.getElementsByTagName("ad_org_id").item(0).getChildNodes().item(0).getNodeValue();
			if(alertsXML.getElementsByTagName("m_product_ids").item(0).getChildNodes().item(0) != null){
				productIds = alertsXML.getElementsByTagName("m_product_ids").item(0).getChildNodes().item(0).getNodeValue();
			}
			if (alertsXML.getElementsByTagName("date").item(0) != null) {
				strDate = alertsXML.getElementsByTagName("date").item(0).getChildNodes().item(0).getNodeValue();
			}
			
			whProdList = this.convertStringToList(productIds,",");
			LOGGER.debug("List size of Warehouse Stock > 0 : "+whProdList.size());
			
			posSaleRuleQuery
					.append("select col.m_product_id from c_order co ")
					.append("join c_orderline col on col.c_order_id = co.c_order_id ")
					.append("where co.created between ")
					.append("(timestamp '" + strDate + "') ")
					.append("and (timestamp '" + strDate + "' + interval '1 hour') ")
					.append("and co.ad_org_id = ? ")
					.append("and co.issotrx='Y' ")
					.append("and co.c_doctypetarget_id in (select c_doctype_id from c_doctype where name like 'POS Order%') ");

			connection = PassiveDB.getInstance().getConnection();

			// Rule 1 for negative products
			rule1PS1 = connection.prepareStatement(brandProductsQuery);
			rule1PS1.setString(1, brandId);
			rule1RS1 = rule1PS1.executeQuery();

			while (rule1RS1.next()) {
				brandProducts.add(rule1RS1.getString("m_product_id"));
			}

			rule1PS2 = connection.prepareStatement(locatorProductsQuery);
			rule1PS2.setString(1, locatorId);
			rule1PS2.setInt(2, 0);
			rule1RS2 = rule1PS2.executeQuery();

			while (rule1RS2.next()) {
				negativeProductsOfLocation.add(rule1RS2.getString("m_product_id"));
			}

			for (String product : negativeProductsOfLocation) {
				if (brandProducts.contains(product.trim())) {
					negativeProductsOfBrandLocation.add(product);
				}
			}

			// Get model codes of current products and get all product ids under
			// these model codes
			modelProductsQuery.append("select m_product_id from m_product where em_cl_model_id in ( ")
					.append("select cm.cl_model_id as cl_model_id from m_product mp ")
					.append("join cl_model cm on cm.cl_model_id= mp.em_cl_model_id ")
					.append("where mp.m_product_id in (");

			if (negativeProductsOfBrandLocation.size() > 0) {
				for (int i = 0; i < negativeProductsOfBrandLocation.size(); i++) {
					modelProductsQuery.append("?" + (negativeProductsOfBrandLocation.size() == i + 1 ? "" : ","));
				}
				modelProductsQuery.append("))");

				rule1PS3 = connection.prepareStatement(modelProductsQuery.toString());
				int index = 1;
				for (String product : negativeProductsOfBrandLocation) {
					rule1PS3.setString(index, product.trim());
					index++;
				}

				rule1RS3 = rule1PS3.executeQuery();

				while (rule1RS3.next()) {
					finalProducts.add(rule1RS3.getString("m_product_id"));
				}
			}

			// Rule 2 for 4 week products
			if(whProdList != null && !whProdList.isEmpty()){
				fourWeeksRuleQuery.append("and msd.m_product_id in (");
				for(int i = 0; i < whProdList.size(); i++){
					fourWeeksRuleQuery.append("?" + (whProdList.size() == i + 1 ? ")" : ","));
				}
			}
			
			rule2PS1 = connection.prepareStatement(fourWeeksRuleQuery.toString());
			rule2PS1.setString(1, locatorId);
			rule2PS1.setString(2, "Discontinued");
			rule2PS1.setString(3, locatorId);
			if(whProdList != null && !whProdList.isEmpty()){
				int index = 4;
				for(String productId : whProdList){
					rule2PS1.setString(index, productId.trim());
					index++;
				}
			}
			
			rule2RS1 = rule2PS1.executeQuery();

			while (rule2RS1.next()) {
				if (brandProducts.contains(rule2RS1.getString("m_product_id"))) {
					fourWeeksProducts.add(rule2RS1.getString("m_product_id"));
					finalProducts.add(rule2RS1.getString("m_product_id"));
				}
			}

			// Rule 3 for 8 week products
			if(whProdList != null && !whProdList.isEmpty()){
				eightWeeksRuleQuery.append("and msd.m_product_id in (");
				for(int i = 0; i < whProdList.size(); i++){
					eightWeeksRuleQuery.append("?" + (whProdList.size() == i + 1 ? ")" : ","));
				}
			}
			
			rule3PS1 = connection.prepareStatement(eightWeeksRuleQuery.toString());
			rule3PS1.setString(1, locatorId);
			rule3PS1.setString(2, "Discontinued");
			rule3PS1.setString(3, locatorId);
			if(whProdList != null && !whProdList.isEmpty()){
				int index = 4;
				for(String productId : whProdList){
					rule3PS1.setString(index, productId.trim());
					index++;
				}
			}
			
			rule3RS1 = rule3PS1.executeQuery();

			while (rule3RS1.next()) {
				if (brandProducts.contains(rule3RS1.getString("m_product_id"))) {
					eightWeeksProducts.add(rule3RS1.getString("m_product_id"));
					finalProducts.add(rule3RS1.getString("m_product_id"));
				}
			}

			// Rule 4
			// In case there is a sale recorded (POS sale) in the store up to 1
			// hour after the zone inventory is complete in the tool
			if (strDate != null && orgId != null && !strDate.isEmpty() && !orgId.isEmpty()) {
				rule4PS1 = connection.prepareStatement(posSaleRuleQuery.toString());
				rule4PS1.setString(1, orgId);

				rule4RS1 = rule4PS1.executeQuery();

				while (rule4RS1.next()) {
					if (brandProducts.contains(rule4RS1.getString("m_product_id"))) {
						posSaleProducts.add(rule4RS1.getString("m_product_id"));
						finalProducts.add(rule4RS1.getString("m_product_id"));
					}
				}

			}

			if (finalProducts.size() > 0) {
				for (int i = 0; i < finalProducts.size(); i++) {
					prodDetailsQuery.append("?" + (finalProducts.size() == i + 1 ? ")" : ","));
				}

				prodDetailsPS = connection.prepareStatement(prodDetailsQuery.toString());
				prodDetailsPS.setString(1, "DMI CATALOGUE");
				prodDetailsPS.setString(2, locatorId);

				int index = 3;
				for (String product : finalProducts) {
					prodDetailsPS.setString(index, product.trim());
					index++;
				}

				prodDetailsRS = prodDetailsPS.executeQuery();
				jsonArray = this.getJsonFromList(prodDetailsRS);
			}

			jsonDataObject.put("data", jsonArray);
			jsonDataObject.put("message", "Stock alerts found");

			jsonDataObject.put("status", "success");
		} catch (Exception exp) {
			LOGGER.error("Exception while reading stock details: ", exp);
			jsonDataObject.put("message", exp.getMessage() == null ? "" + exp : exp.getMessage());
			jsonDataObject.put("status", "failure");
		} finally {
			if (rule1PS1 != null) {
				try {
					rule1PS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule1PS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule1PS2 != null) {
				try {
					rule1PS2.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule1PS2 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule1PS3 != null) {
				try {
					rule1PS3.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule1PS3 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule2PS1 != null) {
				try {
					rule2PS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule2PS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule3PS1 != null) {
				try {
					rule3PS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule3PS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule4PS1 != null) {
				try {
					rule4PS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule4PS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule1RS1 != null) {
				try {
					rule1RS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule1RS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule1RS2 != null) {
				try {
					rule1RS2.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule1RS2 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule1RS3 != null) {
				try {
					rule1RS3.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule1RS3 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule2RS1 != null) {
				try {
					rule2RS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule2RS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule3RS1 != null) {
				try {
					rule3RS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule3RS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (rule4RS1 != null) {
				try {
					rule4RS1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rule4RS1 in BrandStockAlertsWebService", e2);
				}
			}
			if (prodDetailsPS != null) {
				try {
					prodDetailsPS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing prodDetailsPS in BrandStockAlertsWebService", e2);
				}
			}
			if (prodDetailsRS != null) {
				try {
					prodDetailsRS.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing prodDetailsRS in BrandStockAlertsWebService", e2);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing connection in BrandStockAlertsWebService", e2);
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
			jsonObject.put("pid", rs.getString("m_product_id"));
			jsonObject.put("ean", rs.getString("ean"));
			jsonObject.put("itc", rs.getString("itemcode"));
			jsonObject.put("mn", rs.getString("modelname"));
			jsonObject.put("sd", rs.getString("brand"));
			jsonObject.put("s", rs.getString("size"));
			jsonObject.put("c", rs.getString("color"));
			jsonObject.put("cp", rs.getString("cessionprice"));
			jsonObject.put("erp", rs.getString("erp"));
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}
	
	/**
	 * This method converts the String to List based on the delimiter
	 * 
	 * @param strData
	 * @param regex
	 * @return strList
	 */
	private List<String> convertStringToList(String strData, String regex) {
		List<String> strList = new ArrayList<String>();
		if (strData != null && !strData.isEmpty()) {
			String strArray[] = strData.split(regex);
			for (int i = 0; i < strArray.length; i++) {
				strList.add(strArray[i].trim());
			}
		}
		return strList;
	}

}
