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
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * The Web Service is used to run queries required by Cyclic Inventory application to fetch data
 * from StorageDetail
 * 
 */

public class CyclicInventoryMStorageDetail extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(CyclicInventoryMStorageDetail.class);

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
   * containing Cyclic Inventory StorageDetail information.
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

    JSONObject jsonDataObject = parseCyclicInventoryXML(document);

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
   * This method parses the xml tags and returns the parsed fields
   * 
   * @param cyclicXML
   * @return instanceId
   */
  public static JSONObject parseCyclicInventoryXML(Document cyclicXML) {
    String storageBin = null;
    String quantity = null;
    JSONObject jsonDataObject = new JSONObject();
    try {

      storageBin = cyclicXML.getElementsByTagName("storagebin").item(0).getChildNodes().item(0)
          .getNodeValue();

      if (null != cyclicXML.getElementsByTagName("quantity").item(0).getChildNodes().item(0)) {
        quantity = cyclicXML.getElementsByTagName("quantity").item(0).getChildNodes().item(0)
            .getNodeValue();
      }

      // "select * from StorageDetail where storageBin in ('x','y') and quantityOnHand<=qty"
      // Request in XML format:
      // <storagebin>'x','y','z'</storagebin>
      // <quantity>10</quantity>
      Locator l = null;
      if (null != storageBin) {
        storageBin = "(" + storageBin + ")";
        log.debug("Query for storageBin received");
        System.out.println("Query for storageBin received");
        String hql = "from MaterialMgmtStorageDetail where storageBin in " + storageBin;
        if (null != quantity) {
          hql = hql + " and quantityOnHand <= " + quantity;
        }
        hql = hql + " order by storageBin.searchKey, product.name";

        Query query = OBDal.getInstance().getSession().createQuery(hql);
        List<StorageDetail> storageList = query.list();
        JSONArray jsonArray = new JSONArray();
        for (StorageDetail storage : storageList) {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("_identifier", storage.getIdentifier());
          jsonObject.put("_entityName", storage.getEntityName());
          jsonObject.put("product", storage.getProduct().getId());
          jsonObject.put("product$_identifier", storage.getProduct().getIdentifier());
          jsonObject.put("storageBin", storage.getStorageBin().getId());
          jsonObject.put("storageBin$_identifier", storage.getStorageBin().getIdentifier());
          jsonObject.put("attributeSetValue", storage.getAttributeSetValue().getId());
          jsonObject.put("attributeSetValue$_identifier", storage.getAttributeSetValue()
              .getIdentifier());
          jsonObject.put("uOM", storage.getUOM().getId());
          jsonObject.put("uOM$_identifier", storage.getUOM().getIdentifier());
          jsonObject.put("orderUOM", storage.getOrderUOM());
          jsonObject.put("quantityOnHand", storage.getQuantityOnHand());
          jsonObject.put("onHandOrderQuanity", storage.getOnHandOrderQuanity());
          jsonObject.put("lastInventoryCountDate", storage.getLastInventoryCountDate());
          jsonObject.put("quantityInDraftTransactions", storage.getQuantityInDraftTransactions());
          jsonObject.put("quantityOrderInDraftTransactions",
              storage.getQuantityOrderInDraftTransactions());
          jsonObject.put("client", storage.getClient().getId());
          jsonObject.put("client$_identifier", storage.getClient().getIdentifier());
          jsonObject.put("organization", storage.getOrganization().getId());
          jsonObject.put("organization$_identifier", storage.getOrganization().getIdentifier());
          jsonObject.put("active", storage.isActive());
          jsonObject.put("creationDate", storage.getCreationDate());
          jsonObject.put("createdBy", storage.getCreatedBy().getId());
          jsonObject.put("createdBy$_identifier", storage.getCreatedBy().getIdentifier());
          jsonObject.put("updated", storage.getUpdated());
          jsonObject.put("updatedBy", storage.getUpdatedBy().getId());
          jsonObject.put("updatedBy$_identifier", storage.getUpdatedBy().getIdentifier());
          jsonObject.put("id", storage.getId());
          jsonObject.put("dsReceptiondate", storage.getDsReceptiondate());
          jsonObject.put("dsSequence", storage.getDsSequence());
          log.debug("Query for ReservedQty received");
          System.out.println("Query for ReservedQty received");
          String hql2 = "from MaterialMgmtReservationStock where reservation.product.id = '"
              + storage.getProduct().getId() + "' and storageBin.id in " + storageBin;

          Query query2 = OBDal.getInstance().getSession().createQuery(hql2);
          List<ReservationStock> reservationList = query2.list();
          // System.out.println("reservationList: " + reservationList);
          if (reservationList.size() > 0) {
            jsonObject
                .put(
                    "released",
                    reservationList.get(0).getQuantity()
                        .subtract(reservationList.get(0).getReleased()));
          } else {
            jsonObject.put("released", 0);
          }
          jsonArray.put(jsonObject);
        }
        jsonDataObject.put("data", jsonArray);
        jsonDataObject.put("status", "Success");

      }
    } catch (Exception exp) {
      log.error("Exception while reading StorageDetail data for Cyclic Inventory. ", exp);
      // jsonDataObject.put("status", "Failure: "+exp.getMessage());
      return jsonDataObject;
    }

    return jsonDataObject;

  }

}