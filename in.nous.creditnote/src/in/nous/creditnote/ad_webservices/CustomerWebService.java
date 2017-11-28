package in.nous.creditnote.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import in.decathlon.integration.PassiveDB;

/**
 * This Web Service is used to fetch the data from ad_user table
 * 
 */

public class CustomerWebService extends HttpSecureAppServlet implements WebService {

  private static Logger LOGGER = Logger.getLogger(UserWebService.class);

  private static final String TABLE = "table";

  private static final String INVOICENUMBER = "invoicenumber";
  private static final String ECOMM = "isecomm";

  private static final String C_ORDER = "c_order";

  private static final String YES = "Y";

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs the POST REST operation. This service handles the request for the XML Schema related
   * to user.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
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
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  /**
   * This method parses the xml tags and returns the json object containing the user details
   * 
   * @param userXML
   * @return jsonDataObject
   */
  public static JSONObject parseUserXML(Document userXML) {
    JSONObject jsonDataObject = new JSONObject();
    String tableName = null;
    String invoiceNumber = null;
    String ecom = null;
    String sqlQueryERP = null;
    Connection connection = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      tableName = userXML.getElementsByTagName(TABLE).item(0).getChildNodes().item(0)
          .getNodeValue();
      invoiceNumber = userXML.getElementsByTagName(INVOICENUMBER).item(0).getChildNodes().item(0)
          .getNodeValue();

	if(null!=userXML.getElementsByTagName(ECOMM).item(0)){
		ecom = userXML.getElementsByTagName(ECOMM).item(0).getChildNodes().item
			(0).getNodeValue();
	}

      if (tableName.equals(C_ORDER)) {
        LOGGER.debug("Query for c_order table received");
        Class.forName("org.postgresql.Driver");
        connection = PassiveDB.getInstance().getConnection();
        if(null!=ecom && !ecom.isEmpty() && ecom.equals(YES)){
		sqlQueryERP = "select em_ds_bpartner_id from c_order where em_ds_receiptno ilike ?";
	}else{
		sqlQueryERP = "select em_rc_oxylaneno from c_order where documentno ilike ?";
	}
		ps = connection.prepareStatement(sqlQueryERP);
		ps.setString(1, invoiceNumber);
		rs = ps.executeQuery();
		JSONArray jsonArray = new JSONArray();
		while (rs.next()) {
			JSONObject jsonObject = new JSONObject();
			if(null!=ecom && !ecom.isEmpty()){
			jsonObject.put("rCOxylane", rs.getString("em_ds_bpartner_id"));
			}else{
				jsonObject.put("rCOxylane", rs.getString("em_rc_oxylaneno"));
			}
				jsonArray.put(jsonObject);
		}
			jsonDataObject.put("data", jsonArray);
			
		}

	} catch (Exception exp) {
      LOGGER.error("Exception while reading data from c_order table. ", exp);
    } finally {

      try {
        rs.close();
        ps.close();
        connection.close();
      } catch (SQLException e) {
        LOGGER.error("Exception while reading data from c_order table. ", e);
      }
    }

    return jsonDataObject;

  }

}
