package in.decathlon.ibud.picklistext.extendProperties;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.hooks.ClosePLOutbound_CreateShipmentsHook;

public class ExtendShipmentObj implements ClosePLOutbound_CreateShipmentsHook {

  @Override
  public void exec(ShipmentInOut shipment, DocumentType docType, PickingList picking)
      throws Exception {
    System.out.println("Inside1 ExtendShipmentObj");
    String plDocNo = picking.getDocumentNo();
    shipment.setDocumentNo(shipment.getObwplPlbox().getIdentifier());
    shipment.setIbudpkDocumentno(plDocNo);
    OBDal.getInstance().save(shipment);
    System.out.println("finished1 saved");
  }

}
