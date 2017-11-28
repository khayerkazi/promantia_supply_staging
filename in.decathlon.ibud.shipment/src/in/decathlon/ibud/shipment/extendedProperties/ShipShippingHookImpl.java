package in.decathlon.ibud.shipment.extendedProperties;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.warehouse.shipping.OBWSHIPShipping;
import org.openbravo.warehouse.shipping.ShipShippingHook;

@ApplicationScoped
public class ShipShippingHookImpl implements ShipShippingHook {
  private static Logger log = Logger.getLogger(ShipShippingHookImpl.class);

  @Override
  public void exec(OBWSHIPShipping shipping) throws Exception {

    try {
      log.info("hooks  called:");

      List<String> orderQryList1 = getAllOrdLineForTruck(shipping);
      log.info("order query list size is: " + orderQryList1.size());
      if (orderQryList1.size() > 0) {
        for (String orderId : orderQryList1) {
          log.info("order Id: " + orderId);
          List<String> qryList = getOrdLineHavingTruckEmptyOrDR(orderId);
          log.info("qryList size is: " + qryList.size());

          if (qryList.size() == 0) {
            changeSOToShip(orderId);
            log.info(" Run change SO To Ship");

          } else if (qryList != null && qryList.size() > 0) {
            log.info("else ");
            int zeroLineCount = 0;
            for (String colId : qryList) {
              List<String> mvmtList = BusinessEntityMapper.getMMLZeroRecForOrdLine(colId);
              log.info("mvm tList size is: " + mvmtList.size());

              if (mvmtList != null && mvmtList.size() > 0) {
                zeroLineCount++;
                log.info("zero Line Count: " + zeroLineCount);
              } else
                break;
            }
            if (qryList.size() == zeroLineCount) {
              changeSOToShip(orderId);
 

            } else {
              log.info("else qryList.size() " + qryList.size() + " == zeroLineCount="
                  + zeroLineCount);
            }
          }
        }

      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw e;
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private List<String> getOrdLineHavingTruckEmptyOrDR(String orderId) {
    String qryString = "select col.id from OBWSHIP_Shipping_Details shipDetails "
        + "join shipDetails.obwshipShipping ship " + "right join shipDetails.goodsShipment mi "
        + "right join mi.materialMgmtShipmentInOutLineList mil "
        + "right join mil.salesOrderLine col right join col.salesOrder co "
        + "where (ship.documentStatus='DR' or ship.documentStatus is null) and " + "co.id ='"
        + orderId + "'";
    final Query qry = OBDal.getInstance().getSession().createQuery(qryString);
    log.info("query-2: " + qryString);

    List<String> qryList = qry.list();
    return qryList;
  }

  private List<String> getAllOrdLineForTruck(OBWSHIPShipping shipping) {
    String orderQueryString1 = "select distinct co.id from OBWSHIP_Shipping_Details shipDetails "
        + "join shipDetails.obwshipShipping ship " + "right join shipDetails.goodsShipment mi "
        + "right join mi.materialMgmtShipmentInOutLineList mil "
        + "right join mil.salesOrderLine col right join col.salesOrder co " + " where "
        + " ship.id='" + shipping.getId() + "'";
    Query orderQuery1 = OBDal.getInstance().getSession().createQuery(orderQueryString1);
    log.info("query-1: " + orderQueryString1);
    List<String> orderQryList1 = orderQuery1.list();
    return orderQryList1;
  }

  private void changeSOToShip(String orderId) {
    Order salesOrdRecoder = OBDal.getInstance().get(Order.class, orderId);
    salesOrdRecoder.setDocumentStatus(SOConstants.shipped);
    salesOrdRecoder.setDocumentAction(SOConstants.closed);
    OBDal.getInstance().save(salesOrdRecoder);
    OBDal.getInstance().flush();
  }
}
