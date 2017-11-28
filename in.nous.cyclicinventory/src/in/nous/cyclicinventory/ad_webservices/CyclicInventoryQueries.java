package in.nous.cyclicinventory.ad_webservices;

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
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * The Web Service to run queries required by Cyclic Inventory application
 * 
 */

public class CyclicInventoryQueries extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(CyclicInventoryQueries.class);
  private static final String ROW_START = "rowstart";
  private static final String M_PRODUCT_COUNT = "m_product_count";
  private static final String LOCATOR_COUNT = "m_locator_count";
  private static final String ATTRIBUTESETINSTANCE_COUNT = "m_attributesetinstance_count";

  private static final String LIMIT = "limit";

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
   * Performs the POST REST operation. This service handles the request for the XML Schema
   * containing Cyclic Inventory scheduler information.
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

    // StringBuilder xmlBuilder = new StringBuilder();
    // xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    // xmlBuilder.append("<cyclicInventory>");

    JSONObject jsonDataObject = parseCyclicInventoryXML(document);
    // if (null != instanceId && !instanceId.isEmpty()) {
    // xmlBuilder.append("<status>").append("Success").append("</status>");
    // xmlBuilder.append("<pInstanceId>").append(instanceId)
    // .append("</pInstanceId>");
    // } else {
    // xmlBuilder.append("<status>").append("Failure").append("</status>");
    // }

    // xmlBuilder.append("</cyclicInventory>");

    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    // w.write(xmlBuilder.toString());
    w.write(jsonDataObject.toString());
    w.close();

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
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
    String rowStart = null;
    String limit = null;
    JSONObject jsonDataObject = new JSONObject();
    try {

      schedulerQuery = cyclicXML.getElementsByTagName("query").item(0).getChildNodes().item(0)
          .getNodeValue();      
      // "select m_locator_id, value, x, y, z, created, updated, isactive from m_locator"

      if (null != schedulerQuery && schedulerQuery.equals("m_locator")) {
        rowStart = cyclicXML.getElementsByTagName(ROW_START).item(0).getChildNodes().item(0)
            .getNodeValue();
        limit = cyclicXML.getElementsByTagName(LIMIT).item(0).getChildNodes().item(0).getNodeValue();
        log.debug("Query for Locator entity received");
        String hql = "from Locator order by id";
        Query query = OBDal.getInstance().getSession().createQuery(hql);
        @SuppressWarnings("unchecked")
        List<Locator> locatorList = query.setFirstResult(Integer.parseInt(rowStart))
            .setMaxResults(Integer.parseInt(limit)).list();

        JSONArray jsonArray = new JSONArray();
        for (Locator locator : locatorList) {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("id", locator.getId());
          //jsonObject.put("identifier", locator.getIdentifier());
          jsonObject.put("value", locator.getSearchKey());
          jsonObject.put("rowX", locator.getRowX());
          jsonObject.put("stackY", locator.getStackY());
          jsonObject.put("levelZ", locator.getLevelZ());
          jsonObject.put("creationDate", locator.getCreationDate());
          jsonObject.put("updated", locator.getUpdated());
          jsonObject.put("active",locator.isActive()); 
          jsonArray.put(jsonObject);
        }
        jsonDataObject.put("data", jsonArray);
      } else if (null != schedulerQuery && schedulerQuery.equals("m_product")) {
        log.debug("Query for Product entity received");
                
        rowStart = cyclicXML.getElementsByTagName(ROW_START).item(0).getChildNodes().item(0)
            .getNodeValue();
        limit = cyclicXML.getElementsByTagName(LIMIT).item(0).getChildNodes().item(0).getNodeValue();
        
        String hql = "from Product order by id";
        Query query = OBDal.getInstance().getSession().createQuery(hql);
        // List<Product> productList = query.list();
        @SuppressWarnings("unchecked")
        List<Product> productList = query.setFirstResult(Integer.parseInt(rowStart))
            .setMaxResults(Integer.parseInt(limit)).list();
        JSONArray jsonArray = new JSONArray();
        for (Product product : productList) {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("id", product.getId());
          jsonObject.put("name", product.getName());
          jsonObject.put("uPCEAN", product.getUPCEAN());
          jsonObject.put("creationDate", product.getCreationDate());
          jsonObject.put("updated", product.getUpdated());
          jsonObject.put("active",product.isActive());
          jsonArray.put(jsonObject);
        }
        jsonDataObject.put("data", jsonArray);
      } else if (null != schedulerQuery && schedulerQuery.equals("m_attributesetinstance")) {
        log.debug("Query for AttributeSetInstance entity received");
                
        rowStart = cyclicXML.getElementsByTagName(ROW_START).item(0).getChildNodes().item(0)
            .getNodeValue();
        limit = cyclicXML.getElementsByTagName(LIMIT).item(0).getChildNodes().item(0).getNodeValue();
        
        SQLQuery query2 = OBDal
            .getInstance()
            .getSession()
            .createSQLQuery(
                "select m_attributesetinstance_id,lot,created,updated,isactive from m_attributesetinstance order by m_attributesetinstance_id");
        // List<Object[]> attributeSetInstanceList = query2.list();
        @SuppressWarnings("unchecked")
        List<Object[]> attributeSetInstanceList = query2.setFirstResult(Integer.parseInt(rowStart))
            .setMaxResults(Integer.parseInt(limit)).list();
        JSONArray jsonArray = new JSONArray();
        for (Object[] objects : attributeSetInstanceList) {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("id", objects[0].toString());
          jsonObject.put("lot", objects[1]==null ? "" : objects[1].toString());
          jsonObject.put("creationDate", objects[2].toString());
          jsonObject.put("updated", objects[3].toString());
          jsonObject.put("isactive",objects[4].toString());
          jsonArray.put(jsonObject);
        }
        jsonDataObject.put("data", jsonArray);
      } else if (schedulerQuery.equals(M_PRODUCT_COUNT)) {
        log.debug("Query for m_product_count received");
        String hql2 = "select count(id) from Product";
        Query query2 = OBDal.getInstance().getSession().createQuery(hql2);
        List<String> listCount = query2.list();
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        if (null != listCount) {
          jsonObject.put("count", listCount.get(0));
          jsonArray.put(jsonObject);
        }

        jsonDataObject.put("data", jsonArray);
      } else if (schedulerQuery.equals(ATTRIBUTESETINSTANCE_COUNT)) {
        log.debug("Query for em_sw_attributesetinstance_count received");
        String hql2 = "select count(m_attributesetinstance_id) from m_attributesetinstance";
        Query query2 = OBDal.getInstance().getSession().createSQLQuery(hql2);
        List<String> listCount = query2.list();
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        if (null != listCount) {
          jsonObject.put("count", listCount.get(0));
          jsonArray.put(jsonObject);
        }

        jsonDataObject.put("data", jsonArray);
      } else if (schedulerQuery.equals(LOCATOR_COUNT)) {
        log.debug("Query for em_sw_locator_count received");
        String hql2 = "select count(id) from Locator";
        Query query2 = OBDal.getInstance().getSession().createQuery(hql2);
        List<String> listCount = query2.list();
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        if (null != listCount) {
          jsonObject.put("count", listCount.get(0));
          jsonArray.put(jsonObject);
        }

        jsonDataObject.put("data", jsonArray);
      }

      jsonDataObject.put("status", "Success");
    } catch (Exception exp) {
      log.error("Exception while reading Scheduler data for Cyclic Inventory. ", exp);
      return jsonDataObject;
    } 

    return jsonDataObject;

  }

}