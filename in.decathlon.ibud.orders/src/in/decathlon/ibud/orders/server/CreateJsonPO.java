package in.decathlon.ibud.orders.server;

import in.decathlon.ibud.commons.JSONHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

public class CreateJsonPO {
  public static final Logger log = Logger.getLogger(CreateJsonPO.class);

  public JSONObject generateJsonPO(boolean parameter, String ordId, int rowCount) throws Exception {

    JSONObject order = new JSONObject();
    JSONArray ordersArray = new JSONArray();

    List<Order> orderList = getPurchaseOrders(parameter, ordId, rowCount);
    if (orderList != null && orderList.size() > 0) {
      ordersArray = createJsonObjOfPO(orderList);
    }
    order.put("data", ordersArray);
    return order;

  }

  public List<Order> getPurchaseOrders(boolean parameter, String ordId, int rowCount) {
    List<BusinessPartner> bpList = getBusinessPatners();
    List<Order> orderlist = new ArrayList<Order>();
    log.debug("No of business Partners of supply are " + bpList.size());
    if (parameter) {
      Order order = OBDal.getInstance().get(Order.class, ordId);
      orderlist.add(order);
    } else {
      String qry1 = "id in (select o.id from Order o where o.swPoReference = null and o.salesTransaction=false and o.documentType.return=false and o.documentStatus='DR' and o.swIsautoOrder=true and o.businessPartner in (:bpList))  order by organization.name ";
      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class, qry1);
      orderQry.setNamedParameter("bpList", bpList);
      orderlist = orderQry.list();

    }
    return orderlist;
  }

  public JSONArray createJsonObjOfPO(List<Order> ords) throws Exception {
    JSONArray jsonOrds = new JSONArray();
    try {
      Set<String> orderDocs = new HashSet<String>();
      short i = 0;
      for (Order or : ords) {
        orderDocs.add(or.getDocumentNo());
        JSONObject orderObj = new JSONObject();
        JSONObject orderHeader = new JSONObject();
        JSONArray orderLines = new JSONArray();
        orderHeader = JSONHelper.convetBobToJson(or);
        orderLines = createOrderLineJson(or);
        orderObj.put("Header", orderHeader);
        orderObj.put("Lines", orderLines);

        jsonOrds.put(i, orderObj);
        i++;
      }
    } catch (Exception e) {
      throw e;
    }
    return jsonOrds;
  }

  private JSONArray createOrderLineJson(Order or) throws JSONException {

    JSONArray ordLines = new JSONArray();

    OBCriteria<OrderLine> orderLineCrit = OBDal.getInstance().createCriteria(OrderLine.class);
    orderLineCrit.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, or));

    List<OrderLine> orderLineList = orderLineCrit.list();
    for (OrderLine ol : orderLineList) {
      ordLines.put(JSONHelper.convetBobToJson(ol));

    }
    return ordLines;
  }

  public List<BusinessPartner> getBusinessPatners() {

    String qry = "id in (select businessPartner.id from OrganizationInformation orgInfo where orgInfo.organization.sWIsstore=false)";
    OBQuery<BusinessPartner> query = OBDal.getInstance().createQuery(BusinessPartner.class, qry);

    List<BusinessPartner> bPartnerList = query.list();
    return bPartnerList;
  }

}
