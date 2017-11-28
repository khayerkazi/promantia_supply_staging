package in.decathlon.supply.dc.ad_webservices;

import in.decathlon.supply.dc.ad_webservices.PicklistDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * This Web Service is used to provide the data required by the Picklist tool
 * application from ERP
 * 
 */

public class PicklistToolWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(PicklistToolWebService.class);

	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");

	private static final String EMPTY = "";

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
	 * the XML Schema related to picklist tool.
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
		String xml = EMPTY;
		String strRead = EMPTY;

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
			jsonDataObject = this.parsePicklistToolXML(document);
		} catch (Exception e) {
			LOGGER.error("Error occurred in doPost() of PicklistToolWebService: ", e);
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
	 * the product details
	 * 
	 * @param picklistToolXML
	 * @return jsonDataObject
	 */
	public JSONObject parsePicklistToolXML(Document picklistToolXML) throws Exception {
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		String statusMessage = EMPTY;
		String updateStatusMessage = EMPTY;
		String dcNumber = EMPTY;
		String strFromDate = EMPTY;
		String strToDate = EMPTY;
		Date fromDate = null;
		Date toDate = null;
		String stores = EMPTY;
		PicklistDTO queryParams = new PicklistDTO();

		try {
			if (null != picklistToolXML.getElementsByTagName("dcnumber").item(0)) {
				dcNumber = picklistToolXML.getElementsByTagName("dcnumber").item(0).getChildNodes().item(0)
						.getNodeValue();
				queryParams.setDocumentNumber(dcNumber);
				jsonDataObject = getDataFromDB(queryParams);
			} else {
				strFromDate = picklistToolXML.getElementsByTagName("fromdate").item(0).getChildNodes().item(0)
						.getNodeValue();
				strToDate = picklistToolXML.getElementsByTagName("todate").item(0).getChildNodes().item(0)
						.getNodeValue();
				stores = picklistToolXML.getElementsByTagName("stores").item(0).getChildNodes().item(0).getNodeValue();

				fromDate = dateFormatter.parse(strFromDate);
				toDate = dateFormatter.parse(strToDate);
				Calendar c = Calendar.getInstance();
				c.setTime(toDate);
				c.add(Calendar.DATE, +1);
				toDate = c.getTime();
				queryParams.setFromDate(fromDate);
				queryParams.setToDate(toDate);
				queryParams.setStores(stores);
				jsonDataObject = getDataFromDB(queryParams);
			}
		} catch (Exception exp) {
			LOGGER.error("Exception while fetching data picklist in parsePicklistToolXML(): ", exp);
			jsonDataObject.put("status", "dcfailure");
			jsonDataObject.put("message", exp.getMessage());
		}
		return jsonDataObject;
	}

	public JSONObject getDataFromDB(PicklistDTO queryParams) throws Exception {
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonDataObject = new JSONObject();
		try {
			if (null != queryParams.getDocumentNumber() && !queryParams.getDocumentNumber().isEmpty()) {
				String picklistStatusQuery = "select obwpl.pickliststatus as pickliststatus "
						+ "from m_inoutline  minoutl "
						+ "right outer join obwpl_pickinglist as obwpl on minoutl.em_obwpl_pickinglist_id= obwpl.obwpl_pickinglist_id "
						+ "join m_inout minout on minoutl.m_inout_id = minout.m_inout_id "
						+ "where minout.documentno = ? " + "group by obwpl.pickliststatus";

				SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(picklistStatusQuery);
				query.setString(0, queryParams.getDocumentNumber());
				List<String> statusList = query.list();
				if (null == statusList || statusList.isEmpty()) {
					jsonDataObject.put("status", "dcstatus");
					jsonDataObject.put("message", "nodata");
				} else if (statusList.get(0).equals("SH") || statusList.get(0).equals("CO")
						|| statusList.get(0).equals("CA")) {
					jsonDataObject.put("status", "dcstatus");
					jsonDataObject.put("message", statusList.get(0));
				} else {
					String picklistDCQuery = "select minout.documentno as documentnumber,mp.name as itemcode,coalesce(sd.name,'*') as brand, "
							+ "ml.value as location, COALESCE(mattr.lot,'NA') as boxnumber,minoutl.movementqty as quantityrequested,"
							+ "COALESCE(aorg.name,'NA') as store, case au.name when 'Openbravo' THEN 'AUTO' ELSE 'MANUAL' END as "
							+ "username, "
							+ "mpp.em_cl_mrpprice as mrp from "
							+ "m_inoutline  minoutl right outer join "
							+ "obwpl_pickinglist as obwpl on minoutl.em_obwpl_pickinglist_id= obwpl.obwpl_pickinglist_id "
							+ "left outer join m_attributesetinstance  mattr on minoutl.m_attributesetinstance_id = "
							+ "mattr.m_attributesetinstance_id "
							+ "left outer join m_locator ml on minoutl.m_locator_id = ml.m_locator_id "
							+ "join m_inout minout on minoutl.m_inout_id = minout.m_inout_id "
							+ "join ad_user  au on au.ad_user_id=minoutl.createdby "
							+ "left outer join ad_org aorg on minout.ad_orgtrx_id=aorg.ad_org_id "
							+ "join m_product mp on minoutl.m_product_id = mp.m_product_id "
							+ "full outer join c_order co on minout.c_order_id = co.c_order_id "
							+ "full outer join cl_storedept sd on sd.cl_storedept_id= co.em_cl_storedept_id "
							+ "full outer join m_productprice mpp on mpp.m_product_id= mp.m_product_id "
							+ "where minout.documentno = ? "
							+ "and obwpl.em_sw_status = ? "
							+ "and mpp.m_pricelist_version_id =(select m_pricelist_version_id from m_pricelist_version where name= ?) ";
							
					SQLQuery query2 = OBDal.getInstance().getSession().createSQLQuery(picklistDCQuery);
					query2.setString(0, queryParams.getDocumentNumber());
					query2.setString(1, "AP");
					query2.setString(2, "DMI CATALOGUE");
					List<Object[]> picklistDataList = query2.list();

					for (Object[] objects : picklistDataList) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("documentnumber", objects[0].toString());
						jsonObject.put("itemcode", objects[1].toString());
						jsonObject.put("brand", objects[2].toString());
						jsonObject.put("location", objects[3].toString());
						jsonObject.put("boxnumber", objects[4].toString());
						jsonObject.put("quantityrequested", objects[5].toString());
						jsonObject.put("store", objects[6].toString());
						jsonObject.put("username", objects[7].toString());
						jsonObject.put("mrp", objects[8].toString());
						jsonArray.put(jsonObject);
					}
					jsonDataObject.put("status", "dcsuccess");
					jsonDataObject.put("message", "success");
					jsonDataObject.put("data", jsonArray);

				}

			} else {

				StringBuffer picklistAddFilterQuery = new StringBuffer();
				picklistAddFilterQuery
						.append("select minout.documentno as documentnumber,mp.name as itemcode,coalesce(sd.name,'*') as brand, "
								+ "ml.value as location, COALESCE(mattr.lot,'NA') as boxnumber,minoutl.movementqty as quantityrequested, "
								+ "COALESCE(aorg.name,'NA') as store, case au.name when 'Openbravo' THEN 'AUTO' ELSE 'MANUAL' END as "
								+ "username,  "
								+ "mpp.em_cl_mrpprice as mrp from "
								+ "m_inoutline  minoutl right outer join "
								+ "obwpl_pickinglist as obwpl on minoutl.em_obwpl_pickinglist_id= obwpl.obwpl_pickinglist_id "
								+ "left outer join m_attributesetinstance  mattr on minoutl.m_attributesetinstance_id = "
								+ "mattr.m_attributesetinstance_id "
								+ "left outer join m_locator ml on minoutl.m_locator_id = ml.m_locator_id "
								+ "join m_inout minout on minoutl.m_inout_id = minout.m_inout_id "
								+ "join ad_user  au on au.ad_user_id=minoutl.createdby "
								+ "left outer join ad_org aorg on minout.ad_orgtrx_id=aorg.ad_org_id "
								+ "join m_product mp on minoutl.m_product_id = mp.m_product_id "
								+ "full outer join c_order co on minout.c_order_id = co.c_order_id "
								+ "full outer join cl_storedept sd on sd.cl_storedept_id= co.em_cl_storedept_id "
								+ "full outer join m_productprice mpp on mpp.m_product_id= mp.m_product_id "
								+ "where obwpl.em_sw_status = ? "
								+ "and obwpl.created >= ? "
								+ "and obwpl.created < ? "
								+ "and obwpl.pickliststatus not in (?,?,?) "
								+ "and mpp.m_pricelist_version_id =(select m_pricelist_version_id from m_pricelist_version where name= ?) " 
								+ "and aorg.name in (");

				String stores = queryParams.getStores();
				String store[] = stores.split(",");
				for (int i = 0; i < store.length; i++) {
					picklistAddFilterQuery.append("?" + (store.length == i + 1 ? "" : ","));
				}
				picklistAddFilterQuery.append(")");
				
				SQLQuery query3 = OBDal.getInstance().getSession().createSQLQuery(picklistAddFilterQuery.toString());
				query3.setString(0, "AP");
				query3.setDate(1, queryParams.getFromDate());
				query3.setDate(2, queryParams.getToDate());
				query3.setString(3, "CO");
				query3.setString(4, "SH");
				query3.setString(5, "CA");
				query3.setString(6, "DMI CATALOGUE");
				for (int i = 0; i < store.length; i++) {
					query3.setString(i+7, store[i].trim());
				}
				
				List<Object[]> picklistDataList2 = query3.list();
				if(null==picklistDataList2 || picklistDataList2.isEmpty()){
					jsonDataObject.put("status", "dcstatus");
					jsonDataObject.put("message", "nodata");
				}else{
				for (Object[] objects : picklistDataList2) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("documentnumber", objects[0].toString());
					jsonObject.put("itemcode", objects[1].toString());
					jsonObject.put("brand", objects[2].toString());
					jsonObject.put("location", objects[3].toString());
					jsonObject.put("boxnumber", objects[4].toString());
					jsonObject.put("quantityrequested", objects[5].toString());
					jsonObject.put("store", objects[6].toString());
					jsonObject.put("username", objects[7].toString());
					jsonObject.put("mrp", objects[8].toString());
					jsonArray.put(jsonObject);
				}
				jsonDataObject.put("status", "dcsuccess");
				jsonDataObject.put("message", "success");
				jsonDataObject.put("data", jsonArray);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in getDataFromDB() : ", e);
			jsonDataObject.put("status", "dcfailure");
			jsonDataObject.put("message", e.getMessage());
		}
		return jsonDataObject;
	}
}
