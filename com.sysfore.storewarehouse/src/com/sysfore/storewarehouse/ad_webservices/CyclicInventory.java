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
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The Web Service processes the partial inventory of Cyclic Inventory
 * 
 */

public class CyclicInventory extends HttpSecureAppServlet implements WebService {

	private static Logger log = Logger.getLogger(CyclicInventory.class);

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
	 * the XML Schema containing Cyclic Inventory to create Partial Inventory.
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

		StringBuilder xmlBuilder = new StringBuilder();
		xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		xmlBuilder.append("<cyclicInventory>");

		String instanceId = parseCyclicInventoryXML(document);
		if (null != instanceId && !instanceId.isEmpty()) {
			xmlBuilder.append("<status>").append("Success").append("</status>");
			xmlBuilder.append("<pInstanceId>").append(instanceId)
					.append("</pInstanceId>");
		} else {
			xmlBuilder.append("<status>").append("Failure").append("</status>");
		}

		xmlBuilder.append("</cyclicInventory>");

		response.setContentType("text/xml");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(xmlBuilder.toString());
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
	public static String parseCyclicInventoryXML(Document cyclicXML) {
		String instanceId = null;
		int lineCount = 0;
		int fieldsCount = 0;
		String locId = "";
		String itemDetails = "";
		int lineId = 10;
		Object pInstanceId = null;
		String userId="100";

		try {

			userId = cyclicXML.getElementsByTagName("userid")
			.item(0).getChildNodes().item(0).getNodeValue();
			NodeList listInv = cyclicXML.getElementsByTagName("items");
			lineCount = listInv.getLength();

			if (null != listInv && listInv.getLength() > 0) {

				List<Object> param = new ArrayList<Object>();
				param.add(userId);
				pInstanceId = CallStoredProcedure.getInstance().call(
						"sw_cyclic_inventory_header", param, null, true, true);
				System.out.println("pInstanceId: " + pInstanceId);
				log.debug("pInstanceId: " + pInstanceId);

				for (int indexField = 0; indexField < listInv.getLength(); indexField++) {

					Node fieldNode = listInv.item(indexField);

					itemDetails = fieldNode.getChildNodes().item(0)
							.getNodeValue().toString();
					if (null != itemDetails && itemDetails.contains(",")) {
						String[] item = itemDetails.split(",");
						List<Object> param2 = new ArrayList<Object>();
						param2.add(pInstanceId);
						param2.add(item[0]);
						param2.add(item[1]);
						param2.add(item[2]);
						param2.add(Integer.parseInt(item[3]));
						param2.add(lineId);
						param2.add(userId);
						CallStoredProcedure.getInstance().call(
								"sw_cyclic_inventory_line", param2, null, true,
								false);
					}
					lineId += 10;
				}
				List<Object> param3 = new ArrayList<Object>();
				param3.add(pInstanceId);
				CallStoredProcedure.getInstance().call("m_inventory_post",
						param3, null, true, false);
			}

			instanceId = (String) pInstanceId;
			System.out.println("instanceId: " + instanceId);
			log.debug("instanceId: " + instanceId);

		} catch (Exception exp) {
			log.error(
					"Exception while creating Cyclic Inventory. ",
					exp);
			return instanceId;
		}

		return instanceId;

	}

}
