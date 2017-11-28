package in.decathlon.ibud.shipment.supply;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

public class UpdateStatusWebservice implements WebService {

  public static final Logger log = Logger.getLogger(UpdateStatusWebservice.class);

  public JSONArray responseOrders = new JSONArray();

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    log.debug(".........doGet.....");
    String docNosinJson = getSODocNosAsJson(request);
    writeResult(response, docNosinJson);
    log.debug(".........writeResult.....");

  }

  // this method is used to generate 4 lists with so document numbers having different status
  private String getSODocNosAsJson(HttpServletRequest request) throws JSONException, ParseException {
    JSONObject jsonResponse = new JSONObject();
    log.debug("Enter getSODocNosAsJson");
    String bPartnerId = request.getParameter("bPartnerId");
    log.debug("BusinessPartner" + bPartnerId);

    // BusinessPartner bPartnerOrg = BusinessEntityMapper.getBPOfOrg(bPartnerId);
    BusinessPartner bPartnerOrg = OBDal.getInstance().get(BusinessPartner.class, bPartnerId);
    log.debug("Organization" + bPartnerOrg);

    String updated = request.getParameter("updated");
    updated = updated.replace("_", " ");
    log.debug(updated);

    List<String> orderlistforShipped = new ArrayList<String>();
    List<String> orderlistforPicked = new ArrayList<String>();
    List<String> orderlistforPartiallyShipped = new ArrayList<String>();
    List<String> orderlistforPartiallyPicked = new ArrayList<String>();

    getJsonOrders(bPartnerOrg, updated, "OBWPL_PK", "CO", orderlistforPicked);
    getJsonOrders(bPartnerOrg, updated, "OBWPL_SH", "OBWPL_PK", orderlistforShipped);

    chkForPartialPicked(orderlistforPicked, orderlistforPartiallyPicked, "OBWPL_PK");
    chkForPartialPicked(orderlistforShipped, orderlistforPartiallyShipped, "OBWPL_SH");

    jsonResponse.put("PartiallyPicked", orderlistforPartiallyPicked);
    jsonResponse.put("PartiallyShipped", orderlistforPartiallyShipped);
    jsonResponse.put("Picked", orderlistforPicked);
    jsonResponse.put("Shipped", orderlistforShipped);

    Date currentUpdatedTime = new Date();
    jsonResponse.put("updatedTime", currentUpdatedTime);
    // jsonResponse = getJsonOrders(bPartnerOrg, updated);
    log.debug(jsonResponse);
    return jsonResponse.toString();
  }

  private void chkForPartialPicked(List<String> orderList, List<String> orderlistforPartial,
      String myDocStatus) {
    for (int i = 0; i < orderList.size(); i++) {
      try {
        String newDocno = null;
        String oldDocNo = orderList.get(i).toString();
        if (oldDocNo.contains("CAC"))
          newDocno = oldDocNo.replace("CAC", "CAR");
        else if (oldDocNo.contains("CAR"))
          newDocno = oldDocNo.replace("CAR", "CAC");
        if (newDocno != null) {
          String qry = "id in (select id from Order ord where  ord.documentNo ='" + newDocno + "')";
          log.debug("partial.....qry..." + qry);
          OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class, qry);
          List<Order> neworderList = null;
          if (orderQuery.list().size() > 0)
            neworderList = orderQuery.list();

          if (neworderList != null && neworderList.size() > 0) {
            log.debug("............enter if condition of partial ShipmentInOut ");

            for (Order order : neworderList) {

              if (!order.getDocumentStatus().equals(myDocStatus)) {
                orderList.remove(i);
                orderlistforPartial.add(oldDocNo);
              }
            }
          }
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        log.error(e);
      }

    }
  }

  // this method adds document numbers to orderlistforPicked having doc status as picked
  private void getJsonOrders(BusinessPartner bPartner, String updatedTime,
      String docStatustoChanged, String docStatusTochk, List<String> orders) {

    try {
      String qry = "id in (select id from Order ord where  ord.businessPartner.id ='"
          + bPartner.getId() + "' and ord.updated  > '" + updatedTime
          + "' and ord.documentStatus ='" + docStatustoChanged + "')";
      log.debug(".....qry..." + qry);
      OBQuery<Order> orderQuery = OBDal.getInstance().createQuery(Order.class, qry);

      List<Order> orderList = orderQuery.list();

      if (orderList != null && orderList.size() > 0) {
        log.debug("............enter if condition of ShipmentInOut ");
        int i = 0;
        for (Order order : orderList) {

          // order.setDocumentStatus(docStatustoChanged);
          // OBDal.getInstance().save(order);
          orders.add(order.getDocumentNo());

          i++;

        }
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      log.error(e);
    }
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

}