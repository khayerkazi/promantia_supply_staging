package in.decathlon.factorytostore.process;

import in.decathlon.factorytostore.data.StockCount;
import in.decathlon.factorytostore.data.WarehouseCount;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.supply.dc.util.AutoDCMails;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;

import au.com.bytecode.opencsv.CSVWriter;

public class EmailListener implements WebService {
  Map<String, WarehouseCount> finalWarehouseStock = new HashMap<String, WarehouseCount>();
  private static final Logger LOG = Logger.getLogger(EmailListener.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    try {
      String qry = "id in (select id from Warehouse where ibdoWarehousetype='FACST_External')";
      OBQuery<Warehouse> wQuery = OBDal.getInstance().createQuery(Warehouse.class, qry);
      List<Warehouse> wareHouseList = wQuery.list();
      if (wareHouseList == null && wareHouseList.size() <= 0) {
        throw new OBException("No warehouse of type external found");
      }
      for (Warehouse wHouse : wareHouseList) {
        sendFinalMail(wHouse);
      }
    } catch (Exception e) {
      LOG.error(e);
      throw e;
    }
  }

  private void sendFinalMail(Warehouse wHouse) throws Exception {

    LOG.info("Inside supplier stock mail generation");

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
    final String date = format.format(new Date());
    Map<String, StockCount> warehouseStock = new HashMap<String, StockCount>();
    Map<String, Map<String, StockCount>> csvMap = new HashMap<String, Map<String, StockCount>>();
    File f = new File(IbudConfig.getcsvFile() + "Factory_TO_Store_" + wHouse.getName() + date
        + ".csv");
    try {
      if (!f.exists()) {
        f.createNewFile();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(f, true));

      writer.writeNext(new String[] { "Item Code", "Supplier Name", "Physical Stock",
          "Reserved Stock", "Available Stock" });

      String query = "id in (select product.id from MaterialMgmtStorageDetail msd where msd.storageBin.warehouse.id ='"
          + wHouse.getId()
          + "' and msd.storageBin.id not in (select returnlocator from Warehouse w where w.ibdoWarehousetype ='FACST_External'))";
      OBQuery<Product> prdQuery = OBDal.getInstance().createQuery(Product.class, query);
      List<Product> prodList = prdQuery.list();
      if (prodList == null && prodList.size() <= 0) {
        return;
      }
      Map<String, StockCount> stockDetail = new HashMap<String, StockCount>();
      for (Product prod : prodList) {
        StockCount prodWiseStock = getWarehouseStock(wHouse, prod);
        stockDetail.put(prod.getId(), prodWiseStock);
      }
      csvMap.put(wHouse.getId(), stockDetail);

      for (String warehouseId : csvMap.keySet()) {
        Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
        warehouseStock = csvMap.get(warehouseId);
        for (String productId : warehouseStock.keySet()) {
          Product product = OBDal.getInstance().get(Product.class, productId);
          StockCount stCount = warehouseStock.get(productId);
          writer.writeNext(new String[] { product.getIdentifier(), warehouse.getIdentifier(),
              "" + stCount.getPhysicalStock(), "" + stCount.getReservedStock(),
              "" + stCount.getAvailableStock() });
        }
      }
      LOG.info("Supplier mail been sent ");
      writer.close();
    } catch (IOException e) {
      LOG.warn("Cannot write CSV report...", e);
      LOG.error("Cannot write CSV report...", e);
      return;
    }

    AutoDCMails.getInstance().sendMailToWarehouseByDirectDelivery(f, wHouse.getId());
  }

  public StockCount getWarehouseStock(Warehouse warehouse, Product product) {
    StockCount stockCount = new StockCount();
    BigDecimal reservedQty = getReservedQty(warehouse, product);
    BigDecimal qtyInWarehouse = getActuallQty(warehouse, product);
    BigDecimal availableQty = qtyInWarehouse.subtract(reservedQty);
    stockCount.setPhysicalStock(qtyInWarehouse);
    stockCount.setReservedStock(reservedQty);
    stockCount.setAvailableStock(availableQty);

    return stockCount;
  }

  private BigDecimal getActuallQty(Warehouse warehouse, Product product) {

    BigDecimal totalQty = BigDecimal.ZERO;
    String qry = "select sum(quantityOnHand) from MaterialMgmtStorageDetail sd where sd.storageBin.warehouse.id="
        + " :warehouseId and sd.product.id= :productId and sd.storageBin.oBWHSType = 'ST' and sd.storageBin.id not in "
        + "(select returnlocator from Warehouse w where w.ibdoWarehousetype ='FACST_External')";
    Query storageQuery = OBDal.getInstance().getSession().createQuery(qry);
    storageQuery.setParameter("warehouseId", warehouse.getId());
    storageQuery.setParameter("productId", product.getId());
    @SuppressWarnings("unchecked")
    List<BigDecimal> qryResult = storageQuery.list();
    if (qryResult != null && qryResult.size() > 0) {
      totalQty = qryResult.get(0);
    }
    if (totalQty != null)
      return totalQty;
    else
      return BigDecimal.ZERO;

  }

  private BigDecimal getReservedQty(Warehouse warehouse, Product product) {

    BigDecimal resrvdQty = BigDecimal.ZERO;
    BigDecimal relsdQty = BigDecimal.ZERO;
    BigDecimal availableQty = BigDecimal.ZERO;
    String qry = "select sum(quantity),sum(released) from MaterialMgmtReservationStock mrs where mrs.reservation.product.id="
        + ":productId and mrs.storageBin.warehouse.id= :warehouseId and mrs.storageBin.oBWHSType = 'ST' and mrs.storageBin.id not in "
        + "(select returnlocator from Warehouse w where w.ibdoWarehousetype ='FACST_External')";
    Query storageQuery = OBDal.getInstance().getSession().createQuery(qry);
    storageQuery.setParameter("productId", product.getId());
    storageQuery.setParameter("warehouseId", warehouse.getId());
    @SuppressWarnings("unchecked")
    List<Object[]> qryResult = storageQuery.list();
    if (qryResult != null && qryResult.size() > 0) {
      for (Object[] row : qryResult) {
        resrvdQty = (BigDecimal) row[0];
        relsdQty = (BigDecimal) row[1];
      }
    }

    if (resrvdQty == null)
      resrvdQty = BigDecimal.ZERO;
    if (relsdQty == null)
      relsdQty = BigDecimal.ZERO;

    availableQty = resrvdQty.subtract(relsdQty);
    if (availableQty.compareTo(BigDecimal.ZERO) > 0)
      return availableQty;
    else
      return BigDecimal.ZERO;

  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
