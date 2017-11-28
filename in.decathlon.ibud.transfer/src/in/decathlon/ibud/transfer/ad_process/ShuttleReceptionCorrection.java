package in.decathlon.ibud.transfer.ad_process;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class ShuttleReceptionCorrection extends DalBaseProcess {

  private static Logger log = Logger.getLogger(ShuttleReceptionCorrection.class);
  private static ProcessLogger logger;
  OBError message = new OBError();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      logger = bundle.getLogger();
      String inOutid = (String) bundle.getParams().get("M_InOut_ID");
      ShipmentInOut shio = OBDal.getInstance().get(ShipmentInOut.class, inOutid);
      List<ShipmentInOutLine> shioLines = shio.getMaterialMgmtShipmentInOutLineList();
      InventoryCount ivCount = createInventoryHeader(shio);
      List<InventoryCountLine> inventoryLines = new ArrayList<InventoryCountLine>();
      for (ShipmentInOutLine shioL : shioLines) {
        boolean isAccepted = shioL.isIbodtrIsaccepted();
        BigDecimal quantity = shioL.getMovementQuantity();
        if (!isAccepted) {
          if (isShipment(shioL)) {
            // Inter Org MM Shipment
            quantity = quantity.negate();
            inventoryLines.add(createInventoryLines(ivCount, shioL, quantity, true));
            log.debug("Saved Inventory");
          } else {
            // Inter Org RFC Receipt
            log.debug("Since shortage removing excess quantity ");
            quantity = quantity.abs();
            inventoryLines.add(createInventoryLines(ivCount, shioL, quantity, false));
          }
        }

      }
      if (inventoryLines.size() > 0) {
        ivCount.getMaterialMgmtInventoryCountLineList().addAll(inventoryLines);
        OBDal.getInstance().save(ivCount);
        processInventory(ivCount);
      }
      shio.setIbodtrCorrection(true);
      OBDal.getInstance().save(shio);
      message.setType("Success");
      message.setMessage("Process Completed Successfully");

    } catch (Exception e) {
      log.error(e);
      logger.log(e.getMessage());
      message.setType("Error");
      message.setMessage("Error " + e.getMessage());
      e.printStackTrace();
      throw e;
    } finally {
      bundle.setResult(message);
    }

  }

  public InventoryCount createInventoryHeader(ShipmentInOut shio) {
    InventoryCount inventory = OBProvider.getInstance().get(InventoryCount.class);
    inventory.setClient(shio.getClient());
    inventory.setOrganization(shio.getOrganization());
    inventory.setActive(true);
    inventory.setCreatedBy(shio.getCreatedBy());
    inventory.setCreationDate(new Date());
    inventory.setUpdatedBy(shio.getUpdatedBy());
    inventory.setUpdated(new Date());
    inventory.setMovementDate(new Date());
    inventory.setProcessed(false);
    inventory.setName(new Date().toString());
    inventory.setWarehouse(getReturnBin(shio));
    inventory.setSwMovementtype("PI");
    log.debug("Inventory Header created");
    return inventory;
  }

  public InventoryCountLine createInventoryLines(InventoryCount ivCount, ShipmentInOutLine shioL,
      BigDecimal quantity, boolean subtract) throws Exception {
    try {
      BigDecimal bookQuantity = getBookQty(shioL);
      BigDecimal qtyCount = BigDecimal.ZERO;
      if (subtract) {
        qtyCount = bookQuantity.subtract(quantity).compareTo(BigDecimal.ZERO) >= 0 ? bookQuantity
            .subtract(quantity) : BigDecimal.ZERO;
      } else {
        qtyCount = bookQuantity.add(quantity).compareTo(BigDecimal.ZERO) >= 0 ? bookQuantity
            .subtract(quantity) : BigDecimal.ZERO;
      }
      InventoryCountLine inventoryLine = OBProvider.getInstance().get(InventoryCountLine.class);
      inventoryLine.setActive(true);
      inventoryLine.setClient(ivCount.getClient());
      inventoryLine.setOrganization(ivCount.getOrganization());
      inventoryLine.setCreatedBy(ivCount.getCreatedBy());
      inventoryLine.setCreationDate(new Date());
      inventoryLine.setUpdated(new Date());
      inventoryLine.setUpdatedBy(ivCount.getUpdatedBy());
      inventoryLine.setPhysInventory(ivCount);
      inventoryLine.setBookQuantity(bookQuantity);
      inventoryLine.setQuantityCount(qtyCount.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO
          : qtyCount);
      inventoryLine.setProduct(shioL.getProduct());
      inventoryLine.setUOM(shioL.getUOM());
      inventoryLine.setStorageBin(shioL.getStorageBin());
      log.debug("Inventory line created with qty: " + quantity);
      return inventoryLine;
    } catch (Exception e) {
      throw e;
    }
  }

  public boolean isShipment(ShipmentInOutLine shioL) {
    DocumentType docType = shioL.getShipmentReceipt().getDocumentType();
    if (docType.isSalesTransaction() && docType.isReturn() && docType.isIBODTRIsInterOrg()
        && docType.getDocumentCategory().equals("MMS")) {
      // Inter Org RFC Receipt
      log.debug("returning false since doctype is: " + docType.getName());
      return false;
    } else if (docType.isSalesTransaction() && docType.isIBODTRIsInterOrg()
        && docType.getDocumentCategory().equals("MMS") && !docType.isReturn()) {
      // Inter Org MM Shipment
      log.debug("returning true since doctype is: " + docType.getName());
      return true;
    } else {
      throw new OBException("Shipment has document type not related to returns flow");
    }
  }

  private Warehouse getReturnBin(ShipmentInOut shio) {

    if (shio.getMaterialMgmtShipmentInOutLineList() != null
        && shio.getMaterialMgmtShipmentInOutLineList().size() > 0)
      return shio.getMaterialMgmtShipmentInOutLineList().get(0).getStorageBin().getWarehouse();
    else {
      OBCriteria<Warehouse> warehouseCrit = OBDal.getInstance().createCriteria(Warehouse.class);
      warehouseCrit.add(Restrictions.eq(Warehouse.PROPERTY_NAME, "Return"));
      warehouseCrit.setMaxResults(1);
      if (warehouseCrit.count() > 0) {
        return warehouseCrit.list().get(0);
      } else
        return null;
    }
  }

  public void processInventory(InventoryCount ivCount) throws Exception {
    try {
      String ivId = ivCount.getId();
      log.debug("Running Process Inventory");
      BusinessEntityMapper.executeProcess(ivId, "107", "SELECT * FROM m_inventory_post(?)");
      OBDal.getInstance().refresh(ivCount);
      log.debug("Process Inventory completed");
    } catch (Exception e) {
      log.error("Error while executing Process Inventory count" + e.getMessage());
      throw e;
    }
  }

  private BigDecimal getBookQty(ShipmentInOutLine shioL) {
    BigDecimal qtyOnHand = BigDecimal.ZERO;
    try {
      String qry = "select sum(sd.quantityOnHand) from MaterialMgmtStorageDetail sd where sd.product.name=:product and sd.organization.name=:org "
          + " and sd.storageBin=:locator";
      Query query = OBDal.getInstance().getSession().createQuery(qry);
      query.setParameter("product", shioL.getProduct().getName());
      query.setParameter("org", shioL.getOrganization().getName());
      query.setParameter("locator", shioL.getStorageBin());
      List<BigDecimal> qtyOnHandList = query.list();
      if (qtyOnHandList != null && qtyOnHandList.size() > 0)
        qtyOnHand = qtyOnHandList.get(0);
      return qtyOnHand;
    } catch (HibernateException e) {
      throw e;
    }

  }
}
