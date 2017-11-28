package in.decathlon.ibud.picklistext.extendProperties;

import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.picklistext.CompleteActionHandler;


import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.hooks.ClosePLOutbound_ProcessOrdersHook;

public class ExtendOrderObj implements ClosePLOutbound_ProcessOrdersHook {

  @Override
  public void exec(Order order, PickingList picking) throws Exception {
    System.out.println("In ExtendeOrderObj hook");
    order.setDocumentStatus(SOConstants.picked);
    OBDal.getInstance().save(order);
    System.out.println("saved");

    List<InternalMovementLine> picklistLines = picking
        .getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList();

    if (picklistLines.size() >= 0) {
    	CompleteActionHandler shipActHanler = new CompleteActionHandler();
      shipActHanler.executeProcessForZeroLines(picking);
      if (picklistLines.size() == 0) {
        picking.setPickliststatus(SOConstants.pickListShippedStatus);
        OBDal.getInstance().save(picking);
      }

    }
  }

}
