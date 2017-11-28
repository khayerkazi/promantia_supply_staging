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
import org.hibernate.Session;
import java.util.Date;

/**
 * 
 * @author administrator
 *
 */

public class GetDscWHBoxIDList extends HttpSecureAppServlet
		implements WebService {
	
	private static final long serialVersionUID = 1L;
	
	static Logger log4j = Logger.getLogger(GetDscWHBoxIDList.class);

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
		String boxNumbers = null;
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		final Session session = OBDal.getInstance().getSession();
		
		try {

			boxNumbers = xml.getElementsByTagName("BoxNumber").item(0)
					.getChildNodes().item(0).getNodeValue();

			//System.out.println("box Numbers is :" + boxNumbers);

			//boxNumbers = getFormattedParameter(boxNumbers);

//System.out.println("boxNumbers:"+ boxNumbers);
			
			List<String> list = new ArrayList<String>(Arrays.asList(boxNumbers.split(",")));

//System.out.println("list:"+ list);

			final Query queryForDscBoxId = session
					.createSQLQuery("select dsc_wh_box_id from dsc_wh_box where boxno in (:boxInList)");
			
			queryForDscBoxId.setParameterList("boxInList", list);
			

			List<Object> idList = queryForDscBoxId.list();

//System.out.println("Size:"+ idList.size());


			/*if(idList.size() > 0){
				jsonObject.put("idList",idList.toString());
			}	
		
			jsonArray.put(jsonObject);	
			jsonDataObject.put("data", jsonArray);*/
			jsonDataObject.put("data", idList.toString());
			
		} catch (Exception exp) {
			log4j.error("Exception while reading teh DSC WH Box IDs from DSC_WH_BOX Table:", exp);
			exp.printStackTrace();
			return jsonDataObject;
		}

		return jsonDataObject;

	}

	public static String getFormattedParameter(List<String> dscWHBoxIdList){
		
		String boxIds = "";
		int count = 1;
		for(String boxId: dscWHBoxIdList){
			boxIds = boxIds.concat("'");
			boxIds = boxIds.concat(boxId);
			boxIds = boxIds.concat("'");
			if(count != dscWHBoxIdList.size()){
				boxIds = boxIds.concat(",");
			}
			count++;
		}
	
		//System.out.println("Box Ids are:"+boxIds);
		return boxIds;
	}

	
}

