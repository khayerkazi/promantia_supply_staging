package com.sysfore.decathlonecom;

import java.io.StringReader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sysfore.decathlonecom.dao.EcomOrderServiceDAO;
import com.sysfore.decathlonecom.model.Stock;
import com.sysfore.decathlonecom.util.EcomStockSyncUtil;

/**
 * The Web Service in charge of integration between Decathlon Ecommerce and Openbravo ERP.
 * 
 * @author binesh michael
 */

public class EcomStockService extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(EcomStockService.class);

  // private static Map<String, PosSyncProcess> posProcesses = null;
  protected static ConnectionProvider pool;

  public EcomStockService() {
    // initPool();
  }

  /**
   * Performs the GET REST operation. This service handles the request for the XML Schema of list of
   * Business Objects.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // throw new UnsupportedOperationException();
    System.out.println("Inside the get");
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write("inside the get");
    w.close();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String xml = request.getParameter("ecomStock");
    String msg = "none";

    System.out.println("Inside the Stock");

    // Following code converting the String into XML format

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);

    // Validating the XML. Verify all the necessary tag and values are present;

    List<String> values = EcomStockSyncUtil.parseEcomStockXML(document);
   // EcomOrderServiceDAO ecomOrderServiceDAO = new EcomOrderServiceDAO();
    //List<Stock> list = new LinkedList<Stock>();
         List<Stock> list = null;
     EcomOrderServiceDAO ecomOrderServiceDAO = null;
     try {
     ecomOrderServiceDAO = new EcomOrderServiceDAO();
    synchronized (ecomOrderServiceDAO) {

      /**
       * if (values != null) {
       * 
       * System.out.println("Value 1   " + values[1]);
       * 
       * if (values[1].equals("0")) {
       * 
       * // System.out.println("Calling True");
       * 
       * list = ecomOrderServiceDAO.getStock(values, false);
       * 
       * } else { // System.out.println("Calling False"); values[1] =
       * ecomOrderServiceDAO.selectWareHouse(values[1]); list = ecomOrderServiceDAO.getStock(values,
       * true); }
       * 
       * ecomOrderServiceDAO.closeConnection();
       * 
       * }
       */
      //String warehouse = ecomOrderServiceDAO.selectWareHouse(values.get((values.size() - 1)));
      String warehouse ="Saleable Whitefield" ;	
      list = ecomOrderServiceDAO.getStock(values, warehouse);
    }
	} finally {
       if (ecomOrderServiceDAO != null) {
         ecomOrderServiceDAO.closeConnection();
       }
     }
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
	xmlBuilder.append("<stock>");
    if (list != null && list.size() > 0) {
      for (Stock val : list) {
        xmlBuilder.append("<items>");
        xmlBuilder.append("<item>").append(val.getProduct()).append("</item>");
        xmlBuilder.append("<qty>").append(val.getQty()).append("</qty>");
        xmlBuilder.append("<warehousename>").append(val.getWarehouseName()).append(
            "</warehousename>");
        xmlBuilder.append("</items>");

      }
    } else {
      xmlBuilder.append("<items>");
      xmlBuilder.append("<item>").append("NA").append("</item>");
      xmlBuilder.append("<qty>").append("0").append("</qty>");
      xmlBuilder.append("<warehousename>").append("NA").append("</warehousename>");
      xmlBuilder.append("</items>");
    }
	xmlBuilder.append("</stock>");
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(xmlBuilder.toString());
    w.close();

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

}
