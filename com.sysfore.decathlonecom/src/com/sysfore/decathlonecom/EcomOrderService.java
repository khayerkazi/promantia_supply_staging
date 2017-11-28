package com.sysfore.decathlonecom;

import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sysfore.decathlonecom.dao.EcomOrderServiceDAO;
import com.sysfore.decathlonecom.model.EcomOrder;
import com.sysfore.decathlonecom.model.Product;
import com.sysfore.decathlonecom.util.EcomProductSyncUtil;

/**
 * The Web Service in charge of integration between Decathlon Ecommerce and Openbravo ERP.
 * 
 * @author binesh michael
 */

public class EcomOrderService extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(EcomOrderService.class);

  // private static Map<String, PosSyncProcess> posProcesses = null;
  protected static ConnectionProvider pool;

  public EcomOrderService() {
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
    String xml = request.getParameter("ecomOrder");
    String msg = "none";

    // Following code converting the String into XML format

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);

    // Validating the XML. Verify all the necessary tag and values are present;

    EcomOrder eComOrder = EcomProductSyncUtil.parseEcomOrderXML(document);

    if (eComOrder != null) {
      EcomOrderServiceDAO ecomOrderServiceDAO = null;
      try {

        System.out.println("Inside It");

        // EcomOrderServiceDAO ecomOrderServiceDAO = new EcomOrderServiceDAO();
        ecomOrderServiceDAO = new EcomOrderServiceDAO();
        if (eComOrder.getCustomerId() != null && !eComOrder.getCustomerId().equals(""))
          eComOrder.setCustomerId(ecomOrderServiceDAO.selectBPId(eComOrder.getCustomerId()));
        synchronized (ecomOrderServiceDAO) {
          // Create C_Order
          // msg = ecomOrderServiceDAO.createEcomOrder(eComOrder);

          List<Product> p = eComOrder.getItemOrdered();
          for (Product p1 : p) {
            msg = ecomOrderServiceDAO.validateMandatoryData(eComOrder.getOrgName(), eComOrder
                .getWarehouseName(), p1.getProductId(), eComOrder.getCustomerId(), p1.getTaxId());
            if (!msg.equals(""))
              System.out.println("HI");
            break;
          }
          if (msg.equals("")) {
            String[] names = ecomOrderServiceDAO.selectNames(eComOrder.getCustomerId());
            if (names != null) {
              eComOrder.setFirstName(names[0]);
              eComOrder.setLastName(names[1]);
            }

            eComOrder.setWarehouseName(ecomOrderServiceDAO.selectWareHouse(eComOrder
                .getWarehouseName()));

            eComOrder.setOrgName(ecomOrderServiceDAO.selectOrgId(eComOrder.getOrgName()));
            for (Product ol : p) {

              try {

                TaxRate taxRateObj = OBDal.getInstance().get(TaxRate.class, ol.getTaxId());

                BigDecimal priceUnitAmt = (new BigDecimal(ol.getUnitPrice())
                    .multiply(new BigDecimal(ol.getUnitQty())));

                BigDecimal lotPriceAmt = (new BigDecimal(ol.getPcbPrice()).multiply(new BigDecimal(
                    ol.getPcbQty())));

                BigDecimal boxPriceAmt = (new BigDecimal(ol.getUePrice()).multiply(new BigDecimal(
                    ol.getUeQty())));

                ol.setLineGrossAmt(((priceUnitAmt).add(lotPriceAmt).add(boxPriceAmt)).toString());
                System.out.println("........linegrosamt............." + ol.getLineGrossAmt());
                BigDecimal sumTotal = (new BigDecimal(ol.getUnitQty()).add(new BigDecimal(ol
                    .getUeQty())).add(new BigDecimal(ol.getPcbQty())));

                ol.setGrossUnitPrice((new BigDecimal(ol.getLineGrossAmt()).divide((sumTotal), 2,
                    RoundingMode.HALF_UP)).toString());
                System.out.println(".........grossunitprice............" + ol.getUnitPrice());

                TaxRate ss = EcomOrderServiceDAO.getTax(ol.getTaxId(), eComOrder.getState(),
                    eComOrder.getCustomerId());

                ol.setLineNetAmt((new BigDecimal(ol.getLineGrossAmt())
                    .multiply(new BigDecimal(100)).divide((new BigDecimal(100)).add(ss.getRate()),
                    2, RoundingMode.HALF_UP)).toString());

                System.out.println("...........linenetamt.........." + ol.getLineNetAmt());
                ol.setUnitPrice((new BigDecimal(ol.getLineNetAmt()).divide(new BigDecimal(ol
                    .getQuantityOrdered()), 2, RoundingMode.HALF_UP).toString()));

              } catch (Exception e) {
                log4j.debug("Exception" + e);
              }
            }
            msg = ecomOrderServiceDAO.createEcomOrder(eComOrder);
            // ecomOrderServiceDAO.closeConnection();
            // Create C_OrderLine
            // msg = ecomOrderServiceDAO.createEcomOrderLine(eComOrder);

            System.out.println("Complete It");

          }

          System.out.println("Validation It");
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (ecomOrderServiceDAO != null) {
          ecomOrderServiceDAO.closeConnection();
        }
      }
    } else {
      msg = "corrupt data";
    }

    // doPost(request, response);
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    xmlBuilder.append("<root>");
    xmlBuilder.append("<message>").append(msg).append("</message>");
    xmlBuilder.append("</root>");
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
