package in.decathlon.retail.api.omniCommerce.ad_webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The Web Service in charge of creating Purchase Order.
 * 
 */

public class PurchaseOrderService extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(PurchaseOrderService.class);

  private static String storeName = "storeName";

  private static String successStatus = "successStatus";

  private static String success = "success";

  private static String poLines = "poLines";

  private static String poLine = "poLine";

  private static String qtyOrdered = "qtyOrdered";

  private static String priceActual = "priceActual";

  private static String taxId = "taxId";

  private static String supplierCode = "supplierCode";

  private static String itemCode = "itemCode";

  private static String email = "email";

  private static String brand = "brand";

  private static String decathlonId = "decathlonId";
  private static String addressId = "addressId";
  private static String orderType = "orderType";
  private static String orderSubType = "orderSubType";
  private static String posOrderNumber = "posOrderNumber";

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
   * containing PO details to create Purchase Order.
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

    // Verify if all the necessary tag and values are present

    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    xmlBuilder.append("<purchaseOrder>");

    OrderDTO orderDTO = parsePurchaseOrderXML(document);

    if (orderDTO.getSuccessStatus().equals(success)) {

      PurchaseOrderServiceDAO poServiceDAO = new PurchaseOrderServiceDAO();
      synchronized (poServiceDAO) {
        orderDTO = poServiceDAO.createPONew(orderDTO);
        poServiceDAO.closeConnection();
      }

      if (orderDTO.getSuccessStatus().equals(success)) {

        final String linesInfoString = orderDTO.getLinesInfo();
        final String[] linesArray = linesInfoString.split(",");

        xmlBuilder.append("<status>").append(orderDTO.getSuccessStatus()).append("</status>");
        xmlBuilder.append("<documentNo>").append(orderDTO.getDocumentNo()).append("</documentNo>");
        xmlBuilder.append("<purchaseOrderId>").append(orderDTO.getCOrderId())
            .append("</purchaseOrderId>");
        xmlBuilder.append("<purchaseOrderLines>");
        for (int i = 0; i < linesArray.length; i++) {

          final String lineInfo = linesArray[i];
          final String[] lineInfoValues = lineInfo.split("-");
          xmlBuilder.append("<purchaseOrderLine>");
          xmlBuilder.append("<itemCode>" + lineInfoValues[0] + "</itemCode>");
          xmlBuilder.append("<orderedQty>" + lineInfoValues[1] + "</orderedQty>");
          xmlBuilder.append("<confirmedQty>" + lineInfoValues[2] + "</confirmedQty>");
          xmlBuilder.append("</purchaseOrderLine>");
        }
        xmlBuilder.append("</purchaseOrderLines>");
        xmlBuilder.append("<confirmedQty>").append(orderDTO.getConfirmedQty())
            .append("</confirmedQty>");
        log.debug("Purchase Order successfully created with c_order_id: " + orderDTO.getCOrderId()
            + " and documentno: " + orderDTO.getDocumentNo());
      } else {
        // System.out.println(orderDTO.getSuccessStatus());
        xmlBuilder.append("<status>").append(orderDTO.getSuccessStatus()).append("</status>");
      }

    } else {
      xmlBuilder.append("<status>").append(orderDTO.getSuccessStatus()).append("</status>");
    }

    xmlBuilder.append("</purchaseOrder>");

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

  /**
   * This method parses the xml tags and returns the parsed fields with status in OrderDTO object
   * 
   * @param orderXML
   * @return orderDTO
   */
  public static OrderDTO parsePurchaseOrderXML(Document orderXML) {
    OrderDTO orderDTO = new OrderDTO();
    List<OrderlineDTO> listOrderLineDTO = new ArrayList<OrderlineDTO>();
    int lineCount = 0;
    int fieldsCount = 0;
    String decathlon = "";
    String address = "";
    String ordertype = "";
    String ordersubtype = "";
    String posorderno = "";
    try {
      orderDTO.setStoreName(orderXML.getElementsByTagName(storeName).item(0).getChildNodes()
          .item(0).getNodeValue());
      orderDTO.setEmail(orderXML.getElementsByTagName(email).item(0).getChildNodes().item(0)
          .getNodeValue());
      orderDTO.setBrand(orderXML.getElementsByTagName(brand).item(0).getChildNodes().item(0)
          .getNodeValue());

      // for omni commerce
      decathlon = orderXML.getElementsByTagName(decathlonId).item(0).getChildNodes().item(0)
          .getNodeValue();
      address = orderXML.getElementsByTagName(addressId).item(0).getChildNodes().item(0)
          .getNodeValue();
      ordertype = orderXML.getElementsByTagName(orderType).item(0).getChildNodes().item(0)
          .getNodeValue();

      if (null != decathlon)
        orderDTO.setDecathlonId(decathlon);
      else
        orderDTO.setDecathlonId("");
      if (null != address)
        orderDTO.setAddressId(address);
      else
        orderDTO.setAddressId("");
      if (null != ordertype)
        orderDTO.setOrderType(ordertype);
      else
        orderDTO.setOrderType("");

      // for POS Order Number
      ordersubtype = orderXML.getElementsByTagName(orderSubType).item(0).getChildNodes().item(0)
          .getNodeValue();
      posorderno = orderXML.getElementsByTagName(posOrderNumber).item(0).getChildNodes().item(0)
          .getNodeValue();

      if (null != ordersubtype)
        orderDTO.setOrderSubType(ordersubtype);
      else
        orderDTO.setOrderSubType("");
      if (null != posorderno)
        orderDTO.setPosOrderNumber(posorderno);
      else
        orderDTO.setPosOrderNumber("");

      // Parsing of Purchase order lines

      NodeList listPOlines = orderXML.getElementsByTagName(poLines);
      NodeList listPOl = orderXML.getElementsByTagName(poLine);
      lineCount = listPOl.getLength();
      // System.out.println(lineCount+"linecount============");

      if (null != listPOlines && listPOlines.getLength() > 0 && null != listPOl
          && listPOl.getLength() > 0) {
        log.debug("PO Lines exist");

        lineCount = listPOl.getLength();

        for (int indexLine = 0; indexLine < lineCount; indexLine++) {
          Node poLineNode = listPOl.item(indexLine);
          fieldsCount = poLineNode.getChildNodes().getLength();
          NodeList listFields = poLineNode.getChildNodes();

          OrderlineDTO orderlineDTO = new OrderlineDTO();
          int tagCount = 0;

          for (int indexField = 0; indexField < fieldsCount; indexField++) {
            Node fieldNode = listFields.item(indexField);

            if (fieldNode.getNodeName().equals(qtyOrdered)) {
              orderlineDTO.setQtyOrdered(Integer.parseInt(fieldNode.getChildNodes().item(0)
                  .getNodeValue()));
              tagCount += 1;
            }
            if (fieldNode.getNodeName().equals(priceActual)) {
              orderlineDTO.setPriceActual(Double.parseDouble(fieldNode.getChildNodes().item(0)
                  .getNodeValue()));
              tagCount += 1;
            }

            if (fieldNode.getNodeName().equals(itemCode)) {
              orderlineDTO.setItemCode(fieldNode.getChildNodes().item(0).getNodeValue());
              tagCount += 1;
            }
          }

          if (tagCount != 3) {
            throw new Exception("Mandatory parameters missing in Lines.");
          }

          listOrderLineDTO.add(orderlineDTO);
        }

      }

      orderDTO.setListOrderlineDTOs(listOrderLineDTO);
      orderDTO.setSuccessStatus(success);

    } catch (Exception exp) {
      log.error("Exception while parsing the mandatory fields for creating Purchase Order. ", exp);
      // orderDTO.setSuccessStatus("failure");
      orderDTO.setSuccessStatus("Unable to create Order-Incomplete Data->" + exp);

      return orderDTO;
    }

    return orderDTO;

  }

}
