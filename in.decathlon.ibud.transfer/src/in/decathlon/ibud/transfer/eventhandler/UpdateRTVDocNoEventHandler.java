package in.decathlon.ibud.transfer.eventhandler;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class UpdateRTVDocNoEventHandler extends EntityPersistenceEventObserver {
  protected Logger logger = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final OrderLine ordLine = (OrderLine) event.getTargetInstance();
    if (ordLine != null) {
      Long lineNo = ordLine.getLineNo();
      final ShipmentInOutLine grLine = ordLine.getGoodsShipmentLine();
      if (grLine != null) {
        if (grLine.getShipmentReceipt() != null) {
          String docNo = grLine.getShipmentReceipt().getDocumentNo();
          final Order rtvOrder = ordLine.getSalesOrder();
          if (rtvOrder != null && !rtvOrder.isIbdoCreateso() && !rtvOrder.isIdsdIsautodc()
              && rtvOrder.getDocumentType().equals(BusinessEntityMapper.getDocType("POO", true))) {
            java.util.List<OrderLine> rtvlineList = rtvOrder.getOrderLineList();
            if (rtvlineList != null && rtvlineList.size() > 0) {
              for (OrderLine ol : rtvlineList) {
                if (ol.getLineNo().compareTo(lineNo) < 0) {
                  lineNo = ol.getLineNo();
                  docNo = ol.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo();
                }
              }
            }
            rtvOrder.setDocumentNo(docNo.concat("*SRN"));
          }
        }
      }
    }
  }

  public void onUpdate(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final OrderLine ordLine = (OrderLine) event.getTargetInstance();
    if (ordLine != null) {
      Long lineNo = ordLine.getLineNo();
      final ShipmentInOutLine grLine = ordLine.getGoodsShipmentLine();
      if (grLine != null) {
        if (grLine.getShipmentReceipt() != null) {
          String docNo = grLine.getShipmentReceipt().getDocumentNo();
          final Order rtvOrder = ordLine.getSalesOrder();
          if (rtvOrder != null && !rtvOrder.isIbdoCreateso() && !rtvOrder.isIdsdIsautodc()
              && rtvOrder.getDocumentType().equals(BusinessEntityMapper.getDocType("POO", true))) {
            java.util.List<OrderLine> rtvlineList = rtvOrder.getOrderLineList();
            if (rtvlineList != null && rtvlineList.size() > 0) {
              for (OrderLine ol : rtvlineList) {
                if (ol.getLineNo().compareTo(lineNo) < 0) {
                  lineNo = ol.getLineNo();
                  docNo = ol.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo();
                }
              }
            }
            rtvOrder.setDocumentNo(docNo.concat("*SRN"));
          }
        }
      }
    }
  }

}
