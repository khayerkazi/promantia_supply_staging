package in.decathlon.ibud.replenishment.webservice;

import in.decathlon.ibud.orders.client.SOConstants;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.web.WebService;

public class IBUDGetStockWebService implements WebService {
  BigDecimal qtyInStock = BigDecimal.ZERO;
  BigDecimal releasedQty = BigDecimal.ZERO;
  BigDecimal availableStock = BigDecimal.ZERO;
  String stockDetails = "";

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    JSONArray responseStock = new JSONArray();
    String productId = request.getParameter("product");
    String warehouseId = request.getParameter("warehouse");
    responseStock = getStock(productId, warehouseId);
    writeResult(response, responseStock.toString());
  }

  private JSONArray getStock(String productId, String warehousesId) throws Exception {
    try {
      JSONArray warehouseProduct = new JSONArray();
      JSONObject stockInbin = new JSONObject();

      if (productId == null) {

        throw new Exception("The product parameter is mandatory");
      }

      if (warehousesId == null) {
        throw new Exception("The Organization name parameter is mandatory");
      }
      String[] warehouses = warehousesId.split(",");
      for (String warehouseId : warehouses) {
        Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
        if (warehouse != null) {
          if (isWarehouseOrg(warehouse.getOrganization())) {
            if (warehouse.getName().equals("Shuttle bin")) {
              availableStock = getStockFromShuttleBin(productId, warehouse);
              if (availableStock.signum() == -1) {
                availableStock = BigDecimal.ZERO;
              }
              stockDetails = warehouseId + ":" + productId;
              stockInbin.put(stockDetails, availableStock);
            }

            else if (warehouse.getIbdoWarehousetype().equals(SOConstants.ContinentalWarehouse)
                || warehouse.getIbdoWarehousetype().equals(SOConstants.RegionalWarehouse)) {
              BigDecimal stockfromShuttleBin = BigDecimal.ZERO;
              BigDecimal warehouseStock = BigDecimal.ZERO;
            
              warehouseStock = getStockFromWareHouse(productId, warehouseId);
              availableStock = warehouseStock.subtract(stockfromShuttleBin);
              // availableStock=availableStock.abs();
              if (availableStock.signum() == -1) {
                availableStock = BigDecimal.ZERO;
              }
              stockDetails = warehouseId + ":" + productId;
              stockInbin.put(stockDetails, availableStock);
            } else {
              availableStock = getStockFromWareHouse(productId, warehouseId);
              if (availableStock.signum() == -1) {
                availableStock = BigDecimal.ZERO;
              }
              stockDetails = warehouseId + ":" + productId;
              stockInbin.put(stockDetails, availableStock);
            }

          }

          else {
            availableStock = getStockFromWareHouse(productId, warehouseId);
            stockDetails = warehouseId + ":" + productId;
            stockInbin.put(stockDetails, availableStock);
          }
        }
      }
      warehouseProduct.put(stockInbin);
      return warehouseProduct;
    } catch (Exception e) {
      throw e;
    }

  }

  private boolean isWarehouseOrg(Organization warehouseOrg) {
    OBCriteria<Organization> orgCrit = OBDal.getInstance().createCriteria(Organization.class);
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_ID, warehouseOrg.getId()));
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_SWISWAREHOUSE, true));
    orgCrit.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, false));
    orgCrit.setMaxResults(1);
    if (orgCrit.count() > 0)
      return true;
    else
      return false;
  }

  private BigDecimal getStockFromWareHouse(String productId, String warehouseId) {
    String qry = " SELECT COALESCE(sum(quantityOnHand),0) as stockQty from MaterialMgmtStorageDetail where storageBin.warehouse.id = :warehouseId and product.id=:productId and storageBin.active=true and storageBin.oBWHSType='ST' ";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("warehouseId", warehouseId);
    query.setParameter("productId", productId);

    List<BigDecimal> queryList = query.list();
    if (queryList != null & queryList.size() > 0) {
      qtyInStock = queryList.get(0);
    }

    qry = "select COALESCE((sum(quantity)-sum(released)),0) as reserved from MaterialMgmtReservationStock where  storageBin.warehouse.id = :warehouseId and reservation.product.id = :productId and storageBin.active=true and storageBin.oBWHSType='ST'";
    query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("warehouseId", warehouseId);
    query.setParameter("productId", productId);

    queryList = query.list();
    if (queryList != null && queryList.size() > 0) {
      releasedQty = queryList.get(0);
    }

    availableStock = qtyInStock.subtract(releasedQty);
    return availableStock;
  }

  @SuppressWarnings("unchecked")
  private BigDecimal getStockFromShuttleBin(String productId, Warehouse warehouse) {
    String qry = " SELECT COALESCE(sum(quantityOnHand),0) as stockQty from MaterialMgmtStorageDetail where storageBin.warehouse.id in ("
        + "select id from Warehouse where  ibdoWarehousetype in ('"
        + SOConstants.ContinentalWarehouse
        + "','"
        + SOConstants.RegionalWarehouse
        + "'))  and storageBin.searchKey ilike '%shuttle bin%' and product.id=:productId";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("productId", productId);
    List<BigDecimal> qryList = query.list();
    if (qryList != null & qryList.size() > 0)
      qtyInStock = qryList.get(0);
    else
      qtyInStock = BigDecimal.ZERO;

    qry = "select COALESCE((sum(quantity)-sum(released)),0) as reserved from MaterialMgmtReservationStock where storageBin.warehouse.id in ("
        + "select id from Warehouse where  ibdoWarehousetype in ('"
        + SOConstants.ContinentalWarehouse
        + "','"
        + SOConstants.RegionalWarehouse
        + "'))  and storageBin.searchKey ilike '%shuttle bin%' and reservation.product.id=:productId";
    query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("productId", productId);
    qryList = query.list();
    if (qryList != null && qryList.size() > 0)

      releasedQty = qryList.get(0);

    else
      releasedQty = BigDecimal.ZERO;
    availableStock = qtyInStock.subtract(releasedQty);
    return availableStock;

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not implemented");

  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doDelete not implemented");

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPut not implemented");
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

}
