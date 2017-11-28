package in.decathlon.ecomorders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.hooks.PLGenerationCompletedHook;

public class UpdateTrayNoForEcomOrders implements PLGenerationCompletedHook {
  public static final String EcomOrder = "Ecommerce";

  @Override
  public void exec(HashMap<String, PickingList> createdPLs) throws Exception {
    // int i = 1;

    for (Map.Entry map : createdPLs.entrySet()) {
      int i = 1;
      PickingList picListId = (PickingList) map.getValue();
      String plId = picListId.getId();
      String qry = "select distinct co from MaterialMgmtInternalMovementLine  ml "
          + " join ml.salesOrderLine col join col.salesOrder co"
          + " where ml.oBWPLWarehousePickingList.id = '" + plId + "' order by co.documentNo asc  ";

      Query query = OBDal.getInstance().getSession().createQuery(qry);
      List<Order> orderList = query.list();
      for (Order order : orderList) {

        if (order.getBusinessPartner().getName().equals(EcomOrder)) {
          i = i;
          order.setDeoTrayNumber(new Long(i));
          OBDal.getInstance().save(order);
        }
        i++;
      }

    }
    OBDal.getInstance().flush();

  }

}
