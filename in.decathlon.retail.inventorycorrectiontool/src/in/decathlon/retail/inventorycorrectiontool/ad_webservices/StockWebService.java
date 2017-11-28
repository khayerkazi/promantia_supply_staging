package in.decathlon.retail.inventorycorrectiontool.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
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

/*
 * This Web Service is used to fetch the stock of of a particular brand
 */

public class StockWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(StockWebService.class);

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
			jsonDataObject = parseStockXML(document);
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
	 * the stock details
	 * 
	 * @param stockXML
	 * @return jsonDataObject
	 */
	public JSONObject parseStockXML(Document stockXML) throws Exception {
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		String mLocatorId = null;
		String clBrandId = null;
		String productIds = null;
		boolean brndExists = false;
		boolean prodExists = false;
		String productId[] = null;

		String sqlQueryForStock = "select mp.m_product_id as m_product_id,sum(sd.qtyonhand) as qtyonhand,mp.name as itemcode,mpp.em_cl_cessionprice as cessionprice,cd.cl_brand_id as cl_brand_id "
				+ "from m_product mp "
				+ "join cl_model cm on cm.cl_model_id = mp.em_cl_model_id "
				+ "join cl_brand cd on cd.cl_brand_id = cm.cl_brand_id "
				+ "join m_productprice mpp on mpp.m_product_id = mp.m_product_id "
				+ "join m_pricelist_version mpv on mpv.m_pricelist_version_id = mpp.m_pricelist_version_id "
				+ "join m_storage_detail sd on sd.m_product_id = mp.m_product_id "
				+ "where sd.m_locator_id = ? "
				+ "and cd.cl_brand_id = ? "
				+ "and mpv.name = 'DMI CATALOGUE' "
				+ "group by mp.m_product_id,mp.name, mpp.em_cl_cessionprice, cd.cl_brand_id ";

		LOGGER.debug("Received query to get Stock Details");
		try {
			mLocatorId = stockXML.getElementsByTagName("m_locator_id").item(0).getChildNodes().item(0).getNodeValue();
			if (null != stockXML.getElementsByTagName("cl_brand_id").item(0)) {
				clBrandId = stockXML.getElementsByTagName("cl_brand_id").item(0).getChildNodes().item(0)
						.getNodeValue();
				brndExists = true;
			} else {
				sqlQueryForStock = sqlQueryForStock.replace("and cd.cl_brand_id = ?", "");
			}

			if (null != stockXML.getElementsByTagName("m_product_ids").item(0)) {
				productIds = stockXML.getElementsByTagName("m_product_ids").item(0).getChildNodes().item(0)
						.getNodeValue();
				prodExists = true;
			}

			if (prodExists) {
				StringBuffer stockQuery = new StringBuffer();
				stockQuery.append(" and mp.m_product_id in (");
				productId = productIds.split(",");
				for (int i = 0; i < productId.length; i++) {
					stockQuery.append("?" + (productId.length == i + 1 ? "" : ","));
				}
				stockQuery.append(") group by");
				sqlQueryForStock = sqlQueryForStock.replace("group by", stockQuery.toString());
			}

			LOGGER.debug("SQl Query: " + sqlQueryForStock);

			SQLQuery query1 = OBDal.getInstance().getSession().createSQLQuery(sqlQueryForStock);
			query1.setString(0, mLocatorId);
			if (brndExists) {
				query1.setString(1, clBrandId);
			}

			if (prodExists) {
				for (int i = 0; i < productId.length; i++) {
					query1.setString(i + 2, productId[i].trim());
				}
			}

			List<Object[]> stockList = query1.list();

			if (null == stockList || stockList.isEmpty()) {
				jsonDataObject.put("data", "");
				jsonDataObject.put("message", "No stock found");
			} else {
				jsonArray = getJsonFromList(stockList);
				jsonDataObject.put("data", jsonArray);
				jsonDataObject.put("message", "Stock found");
			}
			jsonDataObject.put("status", "success");
		} catch (Exception exp) {
			LOGGER.error("Exception while reading stock details. ", exp);
			jsonDataObject.put("message", exp.getMessage());
			jsonDataObject.put("status", "failure");
		}

		return jsonDataObject;

	}

	/**
	 * This method converts list of data resuts from Db to json array
	 * 
	 * @param stockList
	 * @return jsonArray
	 */
	private JSONArray getJsonFromList(List<Object[]> ruleList) throws Exception {
		JSONArray jsonArray = new JSONArray();
		for (Object[] objects : ruleList) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("m_product_id", objects[0].toString());
			jsonObject.put("qtyonhand", objects[1].toString());
			jsonObject.put("itemcode", objects[2].toString());
			jsonObject.put("cession_price", objects[3].toString());
			jsonObject.put("cl_brand_id", objects[4].toString());
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}
}
