package com.sysfore.decathlonimport.proc;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.module.idljava.proc.IdlServiceJava;

public class CACReservationCreate extends IdlServiceJava {
  static Logger log4j = Logger.getLogger(CACReservationCreate.class);

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("WarehouseName", Parameter.STRING),
        new Parameter("ItemCode", Parameter.STRING),
        new Parameter("FromLocator", Parameter.STRING),
        new Parameter("ToLocator", Parameter.STRING), new Parameter("FromBoxNo", Parameter.STRING),
        new Parameter("ToBoxNo", Parameter.STRING), new Parameter("BoxQty", Parameter.STRING),
        new Parameter("ShipName", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkNotNull(validator.checkString(values[1], 40));
    validator.checkNotNull(validator.checkString(values[3], 40));
    validator.checkNotNull(validator.checkString(values[5], 40));
    validator.checkNotNull(validator.checkString(values[6], 40));

    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {
    return createReservation((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6],
        (String) values[7]);
  }

  public BaseOBObject createReservation(final String warehouseName, final String itemCode,
      final String fromLocator, final String toLocator, final String fromBoxNo,
      final String toBoxNo, final String boxQty, final String shipName) throws Exception {

    Product prod = getProduct(itemCode);
    String shipName1 = getNextShipmentName(toLocator);
    StorageDetail storDet = getStorageDetail(prod, getLocator(toLocator), boxQty);
    log4j.debug("The Storage Detail is :" + storDet);
    if (toBoxNo == null) {
      // log4j.debug("No reservation for SD");
      return null;
    }

    // Create a reservation for each storage detail record
    Reservation resv = OBProvider.getInstance().get(Reservation.class);
    resv.setActive(true);
    resv.setProduct(prod);
    resv.setStorageBin(storDet.getStorageBin());
    resv.setClient(storDet.getClient());
    resv.setOrganization(storDet.getOrganization());
    resv.setUOM(prod.getUOM());
    resv.setDscShipmentname(shipName1);

    BigDecimal totalResvdQty = new BigDecimal(0);
    List<ReservationStock> stockList = new ArrayList<ReservationStock>();
    // Create a stock line under Reservation
    ReservationStock stock = OBProvider.getInstance().get(ReservationStock.class);
    stock.setActive(true);
    stock.setQuantity(new BigDecimal(boxQty));
    // Create an attribute set instance
    OBContext.setAdminMode();
    AttributeSetInstance attr = createAttributeSetInstance(prod, toBoxNo);
    OBContext.restorePreviousMode();
    stock.setAttributeSetValue(attr);
    stock.setReservation(resv);
    stock.setStorageBin(storDet.getStorageBin());
    stock.setOrganization(storDet.getOrganization());
    stock.setClient(storDet.getClient());
    stockList.add(stock);
    totalResvdQty = totalResvdQty.add(new BigDecimal(boxQty));

    resv.setQuantity(totalResvdQty);
    resv.setMaterialMgmtReservationStockList(stockList);
    resv.setRESStatus("CO");
    resv.setRESProcess("CL");
    OBDal.getInstance().save(resv);

    return resv;

  }

  private StorageDetail getStorageDetail(Product item, Locator locator, String qty) {

    OBCriteria<StorageDetail> sdCrit = OBDal.getInstance().createCriteria(StorageDetail.class);
    sdCrit.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, item));
    sdCrit.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, locator));
    // sdCrit.add(Restrictions.ge(StorageDetail.PROPERTY_ONHANDORDERQUANITY, new BigDecimal(qty)));
    if (sdCrit.list().size() == 0) {
      throw new OBException("Stock Not available for Product " + item.getName());
    }
    return sdCrit.list().get(0);
  }

  private Locator getLocator(String locatorSearchKey) {
    OBCriteria<Locator> locatorCrit = OBDal.getInstance().createCriteria(Locator.class);
    locatorCrit.add(Restrictions.eq(Locator.PROPERTY_SEARCHKEY, locatorSearchKey));
    if (locatorCrit.list().size() == 0) {
      throw new OBException("Locator not found " + locatorSearchKey);
    }
    return locatorCrit.list().get(0);

  }

  private static Product getProduct(String name) {

    // Get the product from the product name
    OBCriteria<Product> prodCrit = OBDal.getInstance().createCriteria(Product.class);
    prodCrit.add(Restrictions.eq(Product.PROPERTY_NAME, name));
    if (prodCrit.list().size() == 0) {
      throw new OBException(name + ": Product not found");
    }
    return prodCrit.list().get(0);
  }

  private String getNextShipmentName(String fromWarehouse) {

    Warehouse shipmentWarehouse = getWarehouse(fromWarehouse);

    OBCriteria<Reservation> resvCrit = OBDal.getInstance().createCriteria(Reservation.class);
    resvCrit.add(Restrictions.eq(Reservation.PROPERTY_WAREHOUSE, shipmentWarehouse));

    Long maxShipNo = (long) 0;
    for (Reservation resv : resvCrit.list()) {
      // Get the shipment name, extract the shipment no from the name
      // Shipment Name format: "Shipment n dd-mm-yyyy"
      if (resv.getDscShipmentname() != null) {
        String[] shipName = resv.getDscShipmentname().split(":");
        Long shipNo = Long.getLong(shipName[1]);
        if (shipNo > maxShipNo) {
          maxShipNo = shipNo;
        }
      }
    }
    // Now form the shipment name
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    String date = dateFormat.format(new Date());
    String nextShipName = new String("Shipment:" + maxShipNo + 1 + ":" + date);
    return nextShipName;
  }

  public static AttributeSetInstance createAttributeSetInstance(Product prod, String boxNo) {
    // Create an attribute set instance
    AttributeSetInstance attr = OBProvider.getInstance().get(AttributeSetInstance.class);
    attr.setActive(true);
    attr.setAttributeSet(prod.getAttributeSet());
    attr.setLotName(boxNo);
    attr.setDescription("L" + boxNo);
    OBDal.getInstance().save(attr);
    return attr;
  }

  public static Warehouse getWarehouse(String searchKey) {
    OBCriteria<Warehouse> sdCrit = OBDal.getInstance().createCriteria(Warehouse.class);
    sdCrit.add(Restrictions.eq(Warehouse.PROPERTY_SEARCHKEY, searchKey));
    if (sdCrit.list().size() == 0) {
      return null;
    }
    return sdCrit.list().get(0);
  }

  @Override
  protected String getEntityName() {
    // TODO Auto-generated method stub
    return null;
  }

}
