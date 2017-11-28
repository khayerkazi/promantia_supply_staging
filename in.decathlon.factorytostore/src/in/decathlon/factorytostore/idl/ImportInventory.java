package in.decathlon.factorytostore.idl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;

public class ImportInventory extends IdlServiceJava {

  private InventoryCount header;

  @Override
  protected String getEntityName() {
    return "Import Inventory";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Warehouse", Parameter.STRING),
        new Parameter("Location", Parameter.STRING), new Parameter("Item_code", Parameter.STRING),
        new Parameter("Quantity", Parameter.STRING), new Parameter("Lot", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    User user = OBContext.getOBContext().getUser();
    String strWarehouse = values[0];
    String location = values[1];
    String itemcode = values[2];
    String quantity = values[3];

    getProduct(itemcode);
    Warehouse warehouse = getWarehouse(strWarehouse);
    checkLocatorExists(location, warehouse);
    Integer.parseInt(quantity);

    if (user.getDefaultWarehouse() != null
        && !warehouse.getId().equals(user.getDefaultWarehouse().getId())) {
      throw new OBException(warehouse + " is wrong warehouse");
    }
    return values;
  }

  private Locator checkLocatorExists(String location, Warehouse wareHouse) {
    OBCriteria<Locator> criteriaOnlocator = OBDal.getInstance().createCriteria(Locator.class);
    criteriaOnlocator.add(Restrictions.eq(Locator.PROPERTY_SEARCHKEY, location.trim()));
    criteriaOnlocator.add(Restrictions.eq(Locator.PROPERTY_WAREHOUSE, wareHouse));
    List<Locator> locators = criteriaOnlocator.list();

    if (locators != null && locators.size() > 0) {
      return locators.get(0);
    }
    throw new OBException("Locator " + location + " does not exist for warehouse "
        + wareHouse.getIdentifier());
  }

  private Warehouse getWarehouse(String warehouse) {
    OBCriteria<Warehouse> criteriaOnWarehouse = OBDal.getInstance().createCriteria(Warehouse.class);
    criteriaOnWarehouse.add(Restrictions.eq(Warehouse.PROPERTY_NAME, warehouse.trim()));
    List<Warehouse> warehouses = criteriaOnWarehouse.list();

    if (warehouses != null && warehouses.size() > 0) {
      return warehouses.get(0);
    }
    throw new OBException("Warehouse " + warehouse + " does not exist");
  }

  protected void postProcess() throws Exception {
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(header);
    new InventoryCountProcess().processInventory(header);
  }

  private Product getProduct(String itemCode) {
    OBCriteria<Product> criteriaOnPrd = OBDal.getInstance().createCriteria(Product.class);
    criteriaOnPrd.add(Restrictions.eq(Product.PROPERTY_NAME, itemCode));

    List<Product> prdList = criteriaOnPrd.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    }
    throw new OBException(" Product with item code " + itemCode + "  Not exists");
  }

  @Override
  protected BaseOBObject internalProcess(Object... values) throws Exception {
    return processImportInventory((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4]);
  }

  private BaseOBObject processImportInventory(String strWarehouse, String location,
      String itemcode, String quantity, String lot) throws Exception {
    Product product = getProduct(itemcode);
    Warehouse warehouse = getWarehouse(strWarehouse);
    Locator locatorExists = checkLocatorExists(location, warehouse);
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    User user = OBContext.getOBContext().getUser();
    Client client = OBContext.getOBContext().getCurrentClient();

    if (user.getDefaultWarehouse() != null
        && !warehouse.getId().equals(user.getDefaultWarehouse().getId())) {
      throw new OBException("Warehouse " + warehouse + " does not Belong to User");
    }

    StorageDetail storageDetail = getStorageDetail(lot, product, locatorExists);

    if (this.getRecordsProcessed() == 0) {
      insertInventoryHeader(warehouse, locatorExists, product, quantity, client, org, user);
    }

    insertInventoryLine(storageDetail, product, locatorExists, quantity, client, org, user, lot);

    return storageDetail;

  }

  private StorageDetail getStorageDetail(String lot, Product product, Locator locatorExists) {
    AttributeSetInstance attr = null;
    if (lot != null && !lot.trim().equals("")) {
      try {
        attr = findDALInstance(false, AttributeSetInstance.class, new Value(
            AttributeSetInstance.PROPERTY_LOTNAME, lot));

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      final OBCriteria<StorageDetail> storageDetailCriteira = OBDal.getInstance().createCriteria(
          StorageDetail.class);
      storageDetailCriteira.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, product));
      storageDetailCriteira.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, locatorExists));
      if (attr != null)
        storageDetailCriteira.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, attr));
      List<StorageDetail> storageDetails = storageDetailCriteira.list();
      if (storageDetails != null && storageDetails.size() > 0) {
        return storageDetails.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private InventoryCountLine insertInventoryLine(StorageDetail storageDetail, Product product,
      Locator locator, String quantity, Client clientID, Organization org, User user, String lot) {
    BigDecimal qtyCount = new BigDecimal(0);
    BigDecimal bookQty = new BigDecimal(0);

    AttributeSetInstance attr = null;
    if (lot != null && !lot.trim().equals("")) {
      try {
        attr = findDALInstance(false, AttributeSetInstance.class, new Value(
            AttributeSetInstance.PROPERTY_LOTNAME, lot));

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      if (storageDetail != null) {
        bookQty = storageDetail.getQuantityOnHand();
      } else
        bookQty = BigDecimal.ZERO;
    } catch (Exception e) {
      e.printStackTrace();
    }

    BigDecimal qty = new BigDecimal(quantity);
    qtyCount = bookQty.add(qty);

    InventoryCountLine icl = OBProvider.getInstance().get(InventoryCountLine.class);
    icl.setClient(clientID);
    icl.setCreatedBy(user);
    icl.setCreationDate(new Date());
    icl.setUpdated(new Date());
    icl.setUpdatedBy(user);
    icl.setOrganization(org);

    icl.setClient(header.getClient());
    icl.setOrganization(header.getOrganization());
    icl.setPhysInventory(header);
    icl.setLineNo(new Long(header.getMaterialMgmtInventoryCountLineList().size() * 10 + 10));
    icl.setStorageBin(locator);
    icl.setProduct(product);
    icl.setAttributeSetValue(attr);
    icl.setQuantityCount(qtyCount);
    icl.setBookQuantity(bookQty);
    icl.setUOM(product.getUOM());

    OBDal.getInstance().save(icl);
    return icl;
  }

  private void insertInventoryHeader(Warehouse wareHouse, Locator location, Product itemcode,
      String quantity, Client clientId, Organization org, User user) throws Exception {

    try {
      header = OBProvider.getInstance().get(InventoryCount.class);
      header.setName(new Date().toString());
      header.setOrganization(org);
      header.setCreationDate(new Date());
      header.setCreatedBy(user);
      header.setUpdated(new Date());
      header.setUpdatedBy(user);
      header.setWarehouse(wareHouse);
      header.setMovementDate(new Date());
      header.setSwMovementtype("I");
      OBDal.getInstance().save(header);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
