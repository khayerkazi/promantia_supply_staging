package in.decathlon.retail.rangeselection.ad_webservices;

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
import org.hibernate.Query;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.openbravo.model.common.enterprise.OrganizationInformation;

/**
 * This Web Service is used to fetch the data from ad_user table
 * 
 */

public class UserWebService extends HttpSecureAppServlet implements WebService {

	private static Logger LOGGER = Logger.getLogger(UserWebService.class);

	private static final String TABLE = "table";

	private static final String EMAIL = "email";

	private static final String AD_USER = "ad_user";

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
	 * the XML Schema related to user.
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

		JSONObject jsonDataObject = parseUserXML(document);

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
	 * the user details
	 * 
	 * @param userXML
	 * @return jsonDataObject
	 */
	public static JSONObject parseUserXML(Document userXML) {
		JSONObject jsonDataObject = new JSONObject();
		String tableName = null;
		String email = null;

		try {
			tableName = userXML.getElementsByTagName(TABLE).item(0).getChildNodes().item(0).getNodeValue();
			email = userXML.getElementsByTagName(EMAIL).item(0).getChildNodes().item(0).getNodeValue();

			if (tableName.equals(AD_USER)) {
				LOGGER.debug("Query for ad_user table received");
				String hql = "from ADUser where email='" + email + "'";
				
				Query query = OBDal.getInstance().getSession().createQuery(hql);
				List<User> userList = query.list();
				JSONArray jsonArray = new JSONArray();
				JSONObject jsonObject2 =null;
				for (User user : userList) {
					JSONObject jsonObject = new JSONObject();
					if(null != user.getId()){
						jsonObject.put("id", user.getId());
						
							jsonObject.put("organization$_id", user.getOrganization().getId());
							
							jsonObject.put("organization$_identifier", user.getOrganization().getDescription());
							if (null != user.getDefaultRole()) {
								
								jsonObject.put("defaultRole$_identifier", user.getDefaultRole().getName());
							}
							String hql2 = "from OrganizationInformation where organization.id='" + user.getOrganization().getId() + "' and idsdZone is not null";
							
							Query query2 = OBDal.getInstance().getSession().createQuery(hql2);
							List<OrganizationInformation> orgInfoList = query2.list();
							if(!orgInfoList.isEmpty()){
								OrganizationInformation orgInfo = orgInfoList.get(0);
										jsonObject.put("default_zone", orgInfo.getIdsdZone());
										
							}
							
							int stockRepListSize = user.getOrganization().getIbdrepReplenishList().size();
							if(stockRepListSize>0){ 
								 jsonObject.put("isnewstore","N");
							}else{ 
								jsonObject.put("isnewstore","Y"); 
							}
							
							jsonArray.put(jsonObject);
					}
				}
				jsonDataObject.put("data", jsonArray);
			
			}
		} catch (Exception exp) {
			exp.printStackTrace();
			LOGGER.error("Exception while reading data from ad_user table. ", exp);
		}

		return jsonDataObject;

	}

}

