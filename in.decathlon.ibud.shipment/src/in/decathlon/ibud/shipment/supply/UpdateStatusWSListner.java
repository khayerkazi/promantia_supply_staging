package in.decathlon.ibud.shipment.supply;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;
import org.openbravo.warehouse.pickinglist.PickingList;

public class UpdateStatusWSListner implements WebService {

  Logger log = Logger.getLogger(UpdateStatusWSListner.class);

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
    String duration = request.getParameter("duration");
    updated = updated.replace("_", " ");

    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    Date dateTo = null;

    try {
      newDate = formater.parse(updated);
    } catch (Exception e) {
      newDate = formater.parse("2000-03-19 00:00:00.0");
    }

    if (duration != null && !duration.equals("")) {
      dateTo = getDateTo(newDate, duration);
    }
    String qry = "";
    try {
      if (dateTo == null) {
        qry = "id in (select id from Order o where o.updated > '" + updated
            + "' and o.salesTransaction = 'Y' and o.businessPartner.id='" + bPartnerId
            + "' and o.documentType.return='N' )";
      } else {
        String strDateTo = dateTo.toString().replace("_", " ");
        strDateTo = formater.format(dateTo);
        qry = "id in (select id from Order o where o.updated > '" + updated
            + "' and o.updated <= '" + strDateTo
            + "' and o.salesTransaction = 'Y' and o.businessPartner.id='" + bPartnerId
            + "' and o.documentType.return='N' )";
      }
      log.debug("qry  " + qry);

      OBQuery<Order> ordQuery = OBDal.getInstance().createQuery(Order.class, qry);
      // ordQuery.setFilterOnActive(false);
      List<Order> orders = ordQuery.list();
      log.debug("There are :" + orders.size() + " orders picked having updated time greater than "
          + newDate);
      HashSet<String> pickedList = new HashSet<String>();
      HashSet<String> shippedList = new HashSet<String>();
      HashSet<String> partialPicked = new HashSet<String>();
      HashSet<String> partialShipped = new HashSet<String>();
      HashSet<String> partialClosed = new HashSet<String>();
      HashSet<String> closedList = new HashSet<String>();
      HashSet<String> voidedList = new HashSet<String>();

      for (Order ord : orders) {
        String docNo = ord.getDocumentNo();
        String ordDocStatus = ord.getDocumentStatus();
        String actuallDocNo = getActuallDocNo(docNo, cacWarehouseSk, carWarehouseSk);
        List<Order> contraDocs = getContraDocs(docNo, actuallDocNo);

        boolean isPicked = false, isShipped = false, isBooked = false, isClosed = false, isVoided = false;
        if (ordDocStatus.equals(SOConstants.DraftDocumentStatus)) {
          isBooked = true;
        } else if (ordDocStatus.equals(SOConstants.CompleteDocumentStatus)) {
          isBooked = true;
        } else if (ordDocStatus.equals(SOConstants.picked)) {
          isPicked = true;
        } else if (ordDocStatus.equals(SOConstants.shipped)) {
          isShipped = true;
        }

        else if (ordDocStatus.equals(SOConstants.closed)) {
          isClosed = true;
          if (!hasSoZeroPickListLine(ord)) {
            for (Order contraDoc : contraDocs) {
              if (!(contraDoc.getDocumentStatus().equals(SOConstants.closed))) {
                isClosed = false;
                break;
              }
            }
            if (isClosed) {
              isClosed = true;
              closedList.add(actuallDocNo);
            }
          }
        } else if (ordDocStatus.equals(SOConstants.voided)) {
          isVoided = true;
          for (Order contraDoc : contraDocs) {
            if (!(contraDoc.getDocumentStatus().equals(SOConstants.voided))) {
              isVoided = false;
              break;
            }
          }
          if (isVoided) {
            voidedList.add(actuallDocNo);
          }
        }

        for (Order order : contraDocs) {
          String contraDocStatus = order.getDocumentStatus();
          if (contraDocStatus.equals(SOConstants.DraftDocumentStatus)) {
            isBooked = true;
          } else if (contraDocStatus.equals(SOConstants.CompleteDocumentStatus)) {
            isBooked = true;
            continue;
          } else if (contraDocStatus.equals(SOConstants.picked)) {
            isPicked = true;
            continue;
          } else if (contraDocStatus.equals(SOConstants.shipped)) {
            isShipped = true;
          } else if (contraDocStatus.equals(SOConstants.closed)) {
            isClosed = true;
          }
        }

        if (isBooked == true && isPicked == true && isShipped == true) {
          partialShipped.add(actuallDocNo);
        }
        if (isBooked == true && isPicked == true && isShipped == false) {
          partialPicked.add(actuallDocNo);
        } else if (isBooked == true && isPicked == false && isShipped == true) {
          partialShipped.add(actuallDocNo);
        } else if (isBooked == false && isPicked == true && isShipped == false) {
          pickedList.add(actuallDocNo);
        } else if (isBooked == false && isPicked == false && isShipped == true) {
          shippedList.add(actuallDocNo);
        } else if (isBooked == false && isPicked == true && isShipped == true) {
          partialShipped.add(actuallDocNo);
        }

      }
      jsonResponse.put("closed", closedList);
      jsonResponse.put("shipped", shippedList);
      jsonResponse.put("partialShiped", partialShipped);
      jsonResponse.put("picked", pickedList);
      jsonResponse.put("partialPicked", partialPicked);
      jsonResponse.put("voided", voidedList);
    } catch (Exception e) {
      jsonResponse.put("error", e.getMessage());
    }

    return jsonResponse.toString();
  }

  private boolean hasSoZeroPickListLine(Order ord) {
    OBCriteria<PickingList> zeroPickList = OBDal.getInstance().createCriteria(PickingList.class);
    zeroPickList.add(Restrictions.ilike(PickingList.PROPERTY_DESCRIPTION, "%" + ord.getDocumentNo()
        + "%"));
    zeroPickList.setMaxResults(1);
    List<PickingList> pickLs = zeroPickList.list();
    if (pickLs != null && pickLs.size() > 0) {
      PickingList pl = pickLs.get(0);
      return BusinessEntityMapper.getPickListLines(ord, pl);
    }
    return true;
  }

  private Date getDateTo(Date newDate, String duration) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(newDate);
    calendar.add(Calendar.MINUTE, Integer.parseInt(duration));
    return calendar.getTime();
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

    }
    if (i > 0) {
      String actuallDoc = docNo.substring(0, i);
      return actuallDoc;
    } else {
      return docNo;
    }
  }

  private List<Order> getContraDocs(String docNo, String actuallDocNo) {
    String qry = "id in (select id from Order ord where ord.salesTransaction='Y' and ord.documentNo like '"
        + actuallDocNo + "%' and ord.documentNo not like '" + docNo + "')";
    OBQuery<Order> ordQuery = OBDal.getInstance().createQuery(Order.class, qry);
    List<Order> ordList = ordQuery.list();
    if (ordList.size() > 0)
      return ordList;
    else
      return ordList;
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

}
