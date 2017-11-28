package com.sysfore.storewarehouse.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The Web Service to run queries required by Cyclic Inventory application
 * 
 */

public class CyclicInventoryQueries extends HttpSecureAppServlet implements
		WebService {

	private static Logger log = Logger.getLogger(CyclicInventoryQueries.class);

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Performs the POST REST operation. This service handles the request for
	 * the XML Schema containing Cyclic Inventory scheduler information.
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

//		StringBuilder xmlBuilder = new StringBuilder();
//		xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
//		xmlBuilder.append("<cyclicInventory>");

		JSONObject jsonDataObject = parseCyclicInventoryXML(document);
//		if (null != instanceId && !instanceId.isEmpty()) {
//			xmlBuilder.append("<status>").append("Success").append("</status>");
//			xmlBuilder.append("<pInstanceId>").append(instanceId)
//					.append("</pInstanceId>");
//		} else {
//			xmlBuilder.append("<status>").append("Failure").append("</status>");
//		}

//		xmlBuilder.append("</cyclicInventory>");

		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
//		w.write(xmlBuilder.toString());
		w.write(jsonDataObject.toString());
		w.close();

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();

	}

	/**
	 * This method parses the xml tags and returns the parsed fields
	 * 
	 * @param cyclicXML
	 * @return instanceId
	 */
	public static JSONObject parseCyclicInventoryXML(Document cyclicXML) {
		String schedulerQuery = null;
		JSONObject jsonDataObject = new JSONObject();
		try {

			schedulerQuery = cyclicXML.getElementsByTagName("query").item(0)
					.getChildNodes().item(0).getNodeValue();

			// "select m_locator_id, value, x, y, z, created, updated, isactive from m_locator"

			if (null != schedulerQuery && schedulerQuery.equals("locator")) {
				log.debug("Query for Locator entity received");
				System.out.println("Query for Locator entity received");
				String hql = "from Locator where active='Y'";
				Query query = OBDal.getInstance().getSession().createQuery(hql);
				List<Locator> locatorList = query.list();
				JSONArray jsonArray = new JSONArray();
				for (Locator locator : locatorList) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("id", locator.getId());
					jsonObject.put("identifier", locator.getIdentifier());
					jsonObject.put("rowX", locator.getRowX());
					jsonObject.put("stackY", locator.getStackY());
					jsonObject.put("levelZ", locator.getLevelZ());
					jsonObject.put("creationDate", locator.getCreationDate());
					jsonObject.put("updated", locator.getUpdated());
					jsonArray.put(jsonObject);
				}
				jsonDataObject.put("data", jsonArray);
				jsonDataObject.put("status", "Success");
				
			}
		} catch (Exception exp) {
			log.error(
					"Exception while reading Locator data for Cyclic Inventory. ",
					exp);
//			jsonDataObject.put("status", "Failure: "+exp.getMessage());
			return jsonDataObject;
		}

		return jsonDataObject;

	}

}
