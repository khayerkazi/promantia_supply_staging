package in.decathlon.supply.inventorycorrectiontool.ad_webservices;

import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/*
 * This Web Service is used to fetch the products which have 
 * count > 0 for a particular brand in Warehouse
 */

public class WarehouseStockWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(WarehouseStockWebService.class);

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
	 * the XML Schema related to store stock details.
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
			jsonDataObject = this.parseWHStockXML(document);
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
	 * the product ids of items in wareouse which have stock > 0
	 * 
	 * @param whStockXML
	 * @return jsonDataObject
	 */
	public JSONObject parseWHStockXML(Document whStockXML) throws Exception {
		JSONObject jsonDataObject = new JSONObject();
		String brandId = null;
		List<String> productsList = new ArrayList<String>();
		Connection connection = null;
		PreparedStatement ps1 = null;
		ResultSet rs1 = null;
		String whStockQuery = "select msd.m_product_id from m_storage_detail msd "
				+ "join m_product mp on mp.m_product_id = msd.m_product_id "
				+ "join cl_model cm on cm.cl_model_id = mp.em_cl_model_id "
				+ "join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id " + "where msd.qtyonhand > 0 "
				+ "and msd.m_locator_id in " + "(select m_locator_id from m_locator where value like 'Saleable%' "
				+ "and ad_org_id in (select ad_org_id from ad_org where em_sw_iswarehouse='Y')) "
				+ "and cd.cl_brand_id = ? and mp.em_cl_lifestage = ?";

		try {
			brandId = whStockXML.getElementsByTagName("cl_brand_id").item(0).getChildNodes().item(0)
					.getNodeValue();

			connection = PassiveDB.getInstance().getConnection();

			ps1 = connection.prepareStatement(whStockQuery);
			ps1.setString(1, brandId);
			ps1.setString(2, "Discontinued");
			rs1 = ps1.executeQuery();

			while (rs1.next()) {
				productsList.add(rs1.getString("m_product_id"));
			}

			LOGGER.debug("List of m_product_id having WH stock > 0 for cl_brand_id: " + brandId + " List: "
					+ productsList);
			jsonDataObject.put("data", productsList.toString());
			jsonDataObject.put("message", "Warehouse Stock query executed successfully.");
			jsonDataObject.put("status", "success");
		} catch (Exception exp) {
			LOGGER.error("Exception while reading stock details: ", exp);
			jsonDataObject.put("message", exp.getMessage());
			jsonDataObject.put("status", "failure");
		} finally {
			if (ps1 != null) {
				try {
					ps1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing ps1 in WarehouseStockWebService", e2);
				}
			}
			if (rs1 != null) {
				try {
					rs1.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing rs1 in WarehouseStockWebService", e2);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e2) {
					LOGGER.error("An error occurred while closing connection in WarehouseStockWebService", e2);
				}
			}
		}
		return jsonDataObject;
	}
}
