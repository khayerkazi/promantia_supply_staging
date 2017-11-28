package in.nous.searchitem.ad_webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
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
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.openbravo.base.secureApp.HttpSecureAppServlet;

public class CustomTransactionWebService extends HttpSecureAppServlet implements
		WebService {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger
			.getLogger(CustomTransactionWebService.class);

	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

	}

	public static JSONObject parseXML(Document xml) {
		String searchQuery = null;
		String startRow = null;
		String endRow = null;
		String sortBy = null;
		JSONObject jsonDataObject = new JSONObject();
		try {

			searchQuery = xml.getElementsByTagName("query").item(0)
					.getChildNodes().item(0).getNodeValue();
			startRow = xml.getElementsByTagName("StartRow").item(0)
					.getChildNodes().item(0).getNodeValue();
			endRow = xml.getElementsByTagName("EndRow").item(0).getChildNodes()
					.item(0).getNodeValue();
			sortBy = xml.getElementsByTagName("SortBy").item(0).getChildNodes()
					.item(0).getNodeValue();			

			String hql = "from MaterialMgmtMaterialTransaction where "
					+ searchQuery +"order by updated desc";

			Query query = OBDal.getInstance().getSession().createQuery(hql);			
			query.setFirstResult(Integer.parseInt(startRow));
			query.setMaxResults(Integer.parseInt(endRow)
					- Integer.parseInt(startRow));
			//System.out.println("goods transaction query-->"+query.getQueryString());
			List<MaterialTransaction> mtransactionList = query.list();						
			//System.out.println("no of records-->"+mtransactionList.size());
			JSONArray jsonArray = new JSONArray();
			for (MaterialTransaction mtransaction : mtransactionList) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("storageBin$_identifier", mtransaction
							.getStorageBin().getSearchKey());
					jsonObject.put("updated", mtransaction.getUpdated());
					jsonObject.put("movementQuantity",
							mtransaction.getMovementQuantity());
					jsonObject.put("swAfterqty", mtransaction.getSwAfterqty());
					jsonObject.put("createdBy$_identifier", mtransaction
							.getCreatedBy().getName());
					jsonObject.put("swMovementtype",
							mtransaction.getSwMovementtype());
					jsonArray.put(jsonObject);			
				
			}
			jsonDataObject.put("data", jsonArray);
			jsonDataObject.put("status", "Success");
		} catch (Exception exp) {
			log.error(
					"Exception while reading MaterialMgmtMaterialTransaction data:",
					exp);
			exp.printStackTrace();
			return jsonDataObject;
		}

		return jsonDataObject;

	}

	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

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

	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}
}
