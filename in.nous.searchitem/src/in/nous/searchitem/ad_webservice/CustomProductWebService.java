package in.nous.searchitem.ad_webservice;

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
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CustomProductWebService extends HttpSecureAppServlet implements WebService {
  private static final long serialVersionUID = 1L;

  private static Logger log = Logger.getLogger(CustomProductWebService.class);

  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  public static JSONObject parseXML(Document xml) {
    String searchQuery = null;
    String startRow = null;
    String endRow = null;
    String sortBy = null;
    String lifestage = null;
    JSONObject jsonDataObject = new JSONObject();
    try {

      searchQuery = xml.getElementsByTagName("query").item(0).getChildNodes().item(0)
          .getNodeValue();
      startRow = xml.getElementsByTagName("StartRow").item(0).getChildNodes().item(0)
          .getNodeValue();
      endRow = xml.getElementsByTagName("EndRow").item(0).getChildNodes().item(0).getNodeValue();
      sortBy = xml.getElementsByTagName("SortBy").item(0).getChildNodes().item(0).getNodeValue();
      lifestage = xml.getElementsByTagName("lifestage").item(0).getChildNodes().item(0)
          .getNodeValue();
      // System.out.println("searchQuery==" + searchQuery + ", startRow===" + startRow +
      // ", endRow=="
      // + endRow + ",sortBy==" + sortBy + ",lifestage==" + lifestage);

      // String hql = "from Product where "+ searchQuery+" order by name";
      String hql = "from Product where " + searchQuery + " and clLifestage = '" + lifestage
          + "' order by name";
      // System.out.println("query-->"+hql);
      Query query = OBDal.getInstance().getSession().createQuery(hql);
      query.setFirstResult(Integer.parseInt(startRow));
      query.setMaxResults(Integer.parseInt(endRow) - Integer.parseInt(startRow));

      List<Product> productList = query.list();
      // check for hql is null and lifestage is Active
      if ((productList.isEmpty()) && lifestage.equals("Active")) {
        lifestage = "New";
        hql = "from Product where " + searchQuery + " and clLifestage = '" + lifestage
            + "' order by name";
        query = OBDal.getInstance().getSession().createQuery(hql);
        query.setFirstResult(Integer.parseInt(startRow));
        query.setMaxResults(Integer.parseInt(endRow) - Integer.parseInt(startRow));
        productList = query.list();

        // check for hql is null and lifestage is New
        if ((productList.isEmpty()) && lifestage.equals("New")) {
          lifestage = "Discontinued";
          hql = "from Product where " + searchQuery + " and clLifestage = '" + lifestage
              + "' order by name";
          query = OBDal.getInstance().getSession().createQuery(hql);
          query.setFirstResult(Integer.parseInt(startRow));
          query.setMaxResults(Integer.parseInt(endRow) - Integer.parseInt(startRow));
          productList = query.list();

          // check for hql is null and lifestage is Discontinued
          if ((productList.isEmpty()) && lifestage.equals("Discontinued")) {
            lifestage = "";
            hql = "from Product where " + searchQuery + " and clLifestage = '" + lifestage
                + "' order by name";
            query = OBDal.getInstance().getSession().createQuery(hql);
            query.setFirstResult(Integer.parseInt(startRow));
            query.setMaxResults(Integer.parseInt(endRow) - Integer.parseInt(startRow));
            productList = query.list();
          }
        }
      }
      // System.out.println("productList=="+productList);

      JSONArray jsonArray = new JSONArray();
      for (Product product : productList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", product.getId());
        jsonObject.put("name", product.getName());
        jsonObject.put("clSize", product.getClSize());
        jsonObject.put("clModelname", product.getClModelname());
        jsonObject.put("clLifestage", product.getClLifestage());
        jsonArray.put(jsonObject);

      }
      jsonDataObject.put("data", jsonArray);
      // System.out.println("inwebservice-->"+jsonDataObject.toString());

      jsonDataObject.put("status", "Success");
    } catch (Exception exp) {
      log.error("Exception while reading Search ItemProduct data:", exp);
      exp.printStackTrace();
      return jsonDataObject;
    }

    return jsonDataObject;

  }

  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // System.out.println("In Custom Webservice!!!!!!!!!!!!!!!!!!1");

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

    JSONObject jsonDataObject = parseXML(document);
    // JSONObject jsonDataObject = new JSONObject();
    // jsonDataObject.put("data","hello");

    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    // w.write(xmlBuilder.toString());
    w.write(jsonDataObject.toString());
    w.close();

    /*
     * Query q = HibHelper.getSession().createQuery(sql); q.setFirstResult(10); q.setMaxResults(10);
     * List rc = q.list();
     */

  }

  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
}
