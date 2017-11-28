package in.decathlon.ibud.picklistext.extendProperties;

import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.hooks.CreatePLHook;

public class SetReservationsAllocatedHook implements CreatePLHook {

  @Override
  public void exec(PickingList pickinglist, Order order) throws Exception {
    StringBuffer update = new StringBuffer();
    update.append(" update " + ReservationStock.ENTITY_NAME + " rs");
    update.append(" set " + ReservationStock.PROPERTY_ALLOCATED + " = true");
    update.append(" where exists (select 1");
    update.append("               from " + Reservation.ENTITY_NAME + " r");
    update.append("               where r." + Reservation.PROPERTY_ID + " = rs."
        + ReservationStock.PROPERTY_RESERVATION + ".id");
    update.append("               and exists (select 1");
    update.append("                     from " + OrderLine.ENTITY_NAME + " ol");
    update.append("                     where ol." + OrderLine.PROPERTY_ID + " = r."
        + Reservation.PROPERTY_SALESORDERLINE + ".id");
    update.append("                     and " + OrderLine.PROPERTY_SALESORDER + ".id = :orderId");
    update.append("                          )");
    update.append("              )");

    Query updateQry = OBDal.getInstance().getSession().createQuery(update.toString());
    updateQry.setParameter("orderId", order.getId());
    updateQry.executeUpdate();
    OBDal.getInstance().flush();

  }

}
