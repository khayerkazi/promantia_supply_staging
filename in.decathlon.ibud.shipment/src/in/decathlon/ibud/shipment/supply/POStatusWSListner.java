package in.decathlon.ibud.shipment.supply;

import in.decathlon.ibud.orders.client.SOConstants;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

public class POStatusWSListner implements WebService {

  Logger log = Logger.getLogger(POStatusWSListner.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    log.debug(".........doGet.....");
    String docNosinJson = getSODocNosAsJson(request);
    writeResult(response, docNosinJson);
    log.debug(".........writeResult.....");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not implemented");
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doDelete not implemented");
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPut not implemented");
  }

  private String getSODocNosAsJson(HttpServletRequest request) throws Exception {
    JSONObject jsonResponse = new JSONObject();
    String bPartnerId = request.getParameter("bPartnerId");
    String carWarehouseSk = "";
    String cacWarehouseSk = "";
    HashMap<String, String> warehouseSk = getWarehouseSearchKeys();
    List<String> priorityWares = getPriorityWarehouses();
    if (warehouseSk.size() > 2) {
      if (priorityWares.size() > 0) {
        carWarehouseSk = warehouseSk.get(priorityWares.get(0));
      }
      if (priorityWares.size() > 1) {
        cacWarehouseSk = warehouseSk.get(priorityWares.get(1));
      }
    }

    String updated = request.getParameter("updated");
    updated = updated.replace("_", " ");

    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = formater.parse("2000-03-19 00:00:00.0");
    }

    try {
      String qry = "id in (select id from Order o where o.updated > '" + updated
          + "' and o.salesTransaction = 'Y' and o.businessPartner.id='" + bPartnerId
          + "' and o.documentType.return='N')";
      OBQuery<Order> ordQuery = OBDal.getInstance().createQuery(Order.class, qry);
      List<Order> orders = ordQuery.list();
      log.debug("There are :" + orders.size() + " orders picked having updated time greater than "
          + newDate);
      List<String> pickedList = new ArrayList<String>();
      List<String> shippedList = new ArrayList<String>();
      List<String> partialPicked = new ArrayList<String>();
      List<String> partialShipped = new ArrayList<String>();

      for (Order ord : orders) {
        String docNo = ord.getDocumentNo();
        String actuallDocNo = getActuallDocNo(docNo, cacWarehouseSk, carWarehouseSk);
        Order contraDoc = getContraDoc(docNo, cacWarehouseSk, carWarehouseSk);
        String docStatus = ord.getDocumentStatus();
        String contraDocStatus = "";
        if (contraDoc != null)
          contraDocStatus = contraDoc.getDocumentStatus();
        if (docStatus.equals(SOConstants.picked)) {
          if (contraDocStatus.equals(SOConstants.picked))
            pickedList.add(actuallDocNo);
          else if (contraDocStatus.equals(SOConstants.shipped))
            partialShipped.add(actuallDocNo);
          else if (contraDocStatus.equals(SOConstants.CompleteDocumentStatus))
            partialPicked.add(actuallDocNo);
          else
            pickedList.add(actuallDocNo);

        } else if (docStatus.equals(SOConstants.shipped)) {
          if (contraDocStatus.equals(SOConstants.picked))
            partialShipped.add(actuallDocNo);
          else if (contraDocStatus.equals(SOConstants.shipped))
            shippedList.add(actuallDocNo);
          else if (contraDocStatus.equals(SOConstants.CompleteDocumentStatus))
            partialShipped.add(actuallDocNo);
          else
            shippedList.add(actuallDocNo);
        }
      }
      jsonResponse.put("picked", pickedList);
      jsonResponse.put("shipped", shippedList);
      jsonResponse.put("partialPicked", partialPicked);
      jsonResponse.put("partialShiped", partialShipped);
    } catch (Exception e) {
      jsonResponse.put("error", e.getMessage());
    }

    return jsonResponse.toString();
  }

  private HashMap<String, String> getWarehouseSearchKeys() {
    HashMap<String, String> wareSkMap = new HashMap<String, String>();
    String qry = "id in (select id from Warehouse mw where mw.organization.swIswarehouse='Y' and mw.organization.sWIsstore='N')";
    OBQuery<Warehouse> query = OBDal.getInstance().createQuery(Warehouse.class, qry);
    List<Warehouse> warehouse = query.list();
    for (Warehouse ware : warehouse) {
      wareSkMap.put(ware.getName(), ware.getSearchKey());
    }
    return wareSkMap;
  }

  private List<String> getPriorityWarehouses() {
    List<String> wareSkList = new ArrayList<String>();
    String qry = " ow  where ow.organization.swIswarehouse='Y' and ow.organization.sWIsstore='N' order by ow.priority asc     ";
    OBQuery<OrgWarehouse> wQuery = OBDal.getInstance().createQuery(OrgWarehouse.class, qry);
    List<OrgWarehouse> orgWarehouseList = wQuery.list();
    for (OrgWarehouse ow : orgWarehouseList) {
      wareSkList.add(ow.getWarehouse().getName());
    }
    return wareSkList;
  }

  private String getActuallDocNo(String docNo, String cacWarehouseSk, String carWarehouseSk) {
    log.debug("getting actuall document number of " + docNo);
    int i = docNo.lastIndexOf('*');
    String searchKey = docNo.substring(i + 1, docNo.length());
    if (!(searchKey.equals(carWarehouseSk) || searchKey.equals(cacWarehouseSk))) {
      throw new OBException("Document No " + docNo
          + " is not in correct format, Expected <orgname>*<sequence>*<warehouseSearchKey>");
    }
    if (i > 0) {
      String actuallDoc = docNo.substring(0, i);
      return actuallDoc;
    } else {
      return docNo;
    }
  }

  private Order getContraDoc(String docNo, String cacWarehouseSk, String carWarehouseSk) {
    String newDocno = "";
    if (docNo.contains(carWarehouseSk))
      newDocno = docNo.replace(carWarehouseSk, cacWarehouseSk);
    else if (docNo.contains(cacWarehouseSk))
      newDocno = docNo.replace(cacWarehouseSk, carWarehouseSk);

    OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
    ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, newDocno));
    ordCrit.setMaxResults(1);
    List<Order> ordList = ordCrit.list();
    if (ordList.size() > 0)
      return ordList.get(0);
    else
      return null;
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

}
