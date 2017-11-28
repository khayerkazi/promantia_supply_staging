package in.decathlon.factorytostore.process;

import in.decathlon.factorytostore.data.DirectDelOrderReturn;
import in.decathlon.factorytostore.data.StockCount;
import in.decathlon.factorytostore.data.WarehouseCount;
import in.decathlon.ibud.orders.client.SaveSupplyDataBulk;
import in.decathlon.ibud.orders.data.OrderReturn;
import in.decathlon.supply.dc.util.AutoDCMails;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.decathlon.warehouserules.FactoryToStoreRule;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;

public class SODirectDeliveryBulkListener implements WebService {

  private static final Logger log = Logger.getLogger(SODirectDeliveryBulkListener.class);
  private static final String rwhWarehouseId = "67951CEE618E42E99F4D97C24534CDC1";
  private static final String hubWarehouseId = "57E2E0F65EDE4344B4DDB1BEB54D0589";

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    DirectDelOrderReturn directDelOrderReturn = new DirectDelOrderReturn();
    OrderReturn orderReturn = new OrderReturn();
    SaveSupplyDataBulk directSaveSuppData = new SaveSupplyDataBulk();

    JSONObject orders = new JSONObject(getContentFromRequest(request));
    JSONArray ordersArray = orders.getJSONArray("data");

    Set<String> products = new HashSet<String>();
    for (int i = 0; i < ordersArray.length(); i++) {
      JSONObject ordObj = (JSONObject) ordersArray.get(i);
      JSONArray ordLines = ordObj.getJSONArray("Lines");
      for (int j = 0; j < ordLines.length(); j++) {
        JSONObject ordLine = ordLines.getJSONObject(j);
        products.add(ordLine.getString("product"));
      }
    }
   // setSupplierStock(directDelOrderReturn, products);

    log.info("Orders from Store to create sales orders: " + orders.length());

    Map<String, List<Order>> retailSupply = new HashMap<String, List<Order>>();
    List<Order> suporders = new ArrayList<Order>();

    try {

      long start = System.currentTimeMillis();

      for (int i = 0; i < ordersArray.length(); i++) {
        JSONObject ordObj = (JSONObject) ordersArray.get(i);
        JSONObject ordHeader = (JSONObject) ordObj.get("Header");

        JSONArray ordLines = ordObj.getJSONArray("Lines");

        try {
          directSaveSuppData.distributeSalesOrder(orderReturn, retailSupply, ordHeader, ordLines,
              suporders);
          log.debug("Order finished");
        } catch (Exception e) {
          e.printStackTrace();
          log.warn("Order finished with error", e);
        }
      }
      setSupplierStock(directDelOrderReturn, products,retailSupply);
      directDelOrderReturn.setOrders(orderReturn.getOrders());
      log.debug("It take " + ((System.currentTimeMillis() - start) / 1000)
          + " to insert the orders");

      OBDal.getInstance().flush();

      for (Entry<String, List<Order>> e : retailSupply.entrySet()) {
        for (Order o : e.getValue()) {
          directSaveSuppData.getConfirmedQty(directDelOrderReturn.getOrders().get(e.getKey()), o);
          setWarehouseStock(directDelOrderReturn, o);
        }
      }

      Map<String, StringBuffer> documentNosForWarehouse = new HashMap<String, StringBuffer>();
      for (Order o : suporders) {
        if (documentNosForWarehouse.containsKey(o.getWarehouse().getId())) {
          StringBuffer docNos = documentNosForWarehouse.get(o.getWarehouse().getId());
          documentNosForWarehouse.put(o.getWarehouse().getId(), docNos.append(", "
              + o.getDocumentNo()));
        } else {
          documentNosForWarehouse.put(o.getWarehouse().getId(), new StringBuffer(
              "Replenishment Document Numbers - " + o.getDocumentNo()));
        }
      }
      sendMailToSupplier(documentNosForWarehouse);
      Map<String, WarehouseCount> warehouseStocksFromSupply = directDelOrderReturn
          .getWarehouseStocks();
      for (String prdKey : warehouseStocksFromSupply.keySet()) {
        WarehouseCount supplyWarehouseStock = warehouseStocksFromSupply.get(prdKey);
        Set<String> warehouseKeySet = supplyWarehouseStock.getStockCounts().keySet();
        for (String warehouseKey : warehouseKeySet) {
          StockCount stockCount = supplyWarehouseStock.getStockCounts().get(warehouseKey);
          // System.out.println("Available Stock - " + stockCount.getAvailableStock());
        }
      }
      writeResult(response, directDelOrderReturn);

    } catch (Exception e) {
      response.setHeader("Error", e.toString());
      log.error(e);
      throw e;
    }

  }

  private void setSupplierStock(DirectDelOrderReturn directDelOrderReturn, Set<String> products, Map<String, List<Order>> retailSupply)
      throws Exception {
    try {
    	
	
      EmailListener emailLis = new EmailListener();
      Map<String, WarehouseCount> warehouseStocksFromSupply = new HashMap<String, WarehouseCount>();
      
      BigDecimal cacQuantityReserved = BigDecimal.ZERO;
  	  BigDecimal supplierQuantityReserved = BigDecimal.ZERO;
  	  BigDecimal carQuantityReserved = BigDecimal.ZERO;
  	  BigDecimal hubQuantityReserved = BigDecimal.ZERO;
  
		if (!retailSupply.isEmpty()) {
			cacQuantityReserved = getQuantityReservedByWarehouseType(
					retailSupply, "CAC","");
			supplierQuantityReserved = getQuantityReservedByWarehouseType(
					retailSupply, "FACST_External","");
			carQuantityReserved = getQuantityReservedByWarehouseType(
					retailSupply, "CAR","NOT HUB");
			hubQuantityReserved = getQuantityReservedByWarehouseType(
					retailSupply, "CAR","HUB");
			
		}
      for (String s : products) {
        WarehouseCount wareCount = new WarehouseCount();
        HashMap<String, StockCount> stCount = new HashMap<String, StockCount>();
        StockCount cacStock = new StockCount();
        StockCount carStock = new StockCount();
        StockCount hubStock = new StockCount();
        StockCount supplierStock = new StockCount();
        
        StockCount cacStockReserved = new StockCount();
        StockCount carStockReserved = new StockCount();
        StockCount hubStockReserved = new StockCount();
        StockCount supplierStockReserved = new StockCount();
        
        cacStockReserved.setAvailableStock(BigDecimal.ZERO);
        carStockReserved.setAvailableStock(BigDecimal.ZERO);
        supplierStockReserved.setAvailableStock(BigDecimal.ZERO);
        

        cacStock.setAvailableStock(BigDecimal.ZERO);
        carStock.setAvailableStock(BigDecimal.ZERO);
        hubStock.setAvailableStock(BigDecimal.ZERO);
        supplierStock.setAvailableStock(BigDecimal.ZERO);

        BigDecimal availableQtyCac = BigDecimal.ZERO;
        BigDecimal availableQtyCar = BigDecimal.ZERO;
        BigDecimal availableQtyHub = BigDecimal.ZERO;
        BigDecimal availableQtySupplier = BigDecimal.ZERO;

        Product product = OBDal.getInstance().get(Product.class, s);
        for (Warehouse cwhHouse : cwhWarehouses()) {
        	availableQtyCac = availableQtyCac.add(emailLis.getWarehouseStock(cwhHouse,
                product).getAvailableStock());
          }
        Warehouse carWarehouse = OBDal.getInstance().get(Warehouse.class, rwhWarehouseId);
        availableQtyCar = emailLis.getWarehouseStock(carWarehouse, product).getAvailableStock();
        Warehouse hubWarehouse = OBDal.getInstance().get(Warehouse.class, hubWarehouseId);
        if (hubWarehouse != null) {
          availableQtyHub = emailLis.getWarehouseStock(hubWarehouse, product).getAvailableStock();
        }
        for (Warehouse wHouse : externalWarehouses()) {
          availableQtySupplier = availableQtySupplier.add(emailLis.getWarehouseStock(wHouse,
              product).getAvailableStock());
        }
        System.out.println("Available  Supplier Stock - " + availableQtySupplier);
        cacStock.setAvailableStock(availableQtyCac);
        carStock.setAvailableStock(availableQtyCar);
        hubStock.setAvailableStock(availableQtyHub);
        supplierStock.setAvailableStock(availableQtySupplier);
        
        supplierStockReserved.setAvailableStock(supplierQuantityReserved);
        cacStockReserved.setAvailableStock(cacQuantityReserved);
        carStockReserved.setAvailableStock(carQuantityReserved);
        hubStockReserved.setAvailableStock(hubQuantityReserved);
        
        stCount.put("SupplierStockReserved", supplierStockReserved);
        stCount.put("cacQuantityReserved", cacStockReserved);
        stCount.put("carQuantityReserved", carStockReserved);
        stCount.put("hubQuantityReserved", hubStockReserved);
        
        stCount.put("CacStock", cacStock);
        stCount.put("CarStock", carStock);
        stCount.put("hubStock", hubStock);
        stCount.put("SupplierStock", supplierStock);
        wareCount.setStockCounts(stCount);
        warehouseStocksFromSupply.put(s, wareCount);
        directDelOrderReturn.setWarehouseStocks(warehouseStocksFromSupply);
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

  }

  private void sendMailToSupplier(Map<String, StringBuffer> documentNosForWarehouse) {
    try {
      AutoDCMails autoDCMails = AutoDCMails.getInstance();
      Set<String> documentsKeySet = documentNosForWarehouse.keySet();
      if (documentsKeySet == null || documentsKeySet.size() < 1)
        return;
      for (String key : documentsKeySet) {
        autoDCMails.mailToSupplierFromSupplyErp(key, documentNosForWarehouse.get(key));
      }
    } catch (Exception e) {
      log.error(e);
    }

  }

  private void setWarehouseStock(DirectDelOrderReturn directDelOrderReturn, Order o) {
    for (OrderLine orderLine : o.getOrderLineList()) {
      Product product = orderLine.getProduct();
      WarehouseCount warehouseStock = new WarehouseCount();
      if (directDelOrderReturn.getWarehouseStocks().containsKey(product.getId())) {
        warehouseStock = directDelOrderReturn.getWarehouseStocks().get(product.getId());
      } else {
        warehouseStock.setStockCounts(new HashMap<String, StockCount>());
      }

      for (Warehouse warehouse : externalWarehouses()) {
        StockCount warehouseStock2 = getWarehouseStock(warehouse, product);
        warehouseStock.getStockCounts().put(warehouse.getName(), warehouseStock2);
        directDelOrderReturn.getWarehouseStocks().put(product.getId(), warehouseStock);
      }
    }
  }

  private List<Warehouse> externalWarehouses() {
    OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
    obCriteria.add(Restrictions.eq(Warehouse.PROPERTY_IBDOWAREHOUSETYPE, "FACST_External"));
    return obCriteria.list();
  }
  
  private List<Warehouse> cwhWarehouses() {
	    OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
	    obCriteria.add(Restrictions.eq(Warehouse.PROPERTY_IDSDWHGROUP, "CWH"));
	    return obCriteria.list();
	  }
  
  private StockCount getWarehouseStock(Warehouse warehouse, Product product) {
    StockCount stockCount = new StockCount();

    FactoryToStoreRule factoryToStoreRule = new FactoryToStoreRule();
    BigDecimal reservedQty = factoryToStoreRule.getReservedQty(warehouse, product);
    BigDecimal qtyInWarehouse = factoryToStoreRule.getActuallQty(warehouse, product);
    BigDecimal availableQty = qtyInWarehouse.subtract(reservedQty);
    stockCount.setPhysicalStock(qtyInWarehouse);
    stockCount.setReservedStock(reservedQty);
    stockCount.setAvailableStock(availableQty);

    return stockCount;
  }

  private String getContentFromRequest(HttpServletRequest request) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, "UTF-8");
    String orders = writer.toString();
    return orders;
  }

  private void writeResult(HttpServletResponse response, DirectDelOrderReturn directDelOrderReturn)
      throws IOException, JAXBException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    JAXBContext jc = JAXBContext.newInstance(DirectDelOrderReturn.class);

    MappedNamespaceConvention con = new MappedNamespaceConvention(new Configuration());
    XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, response.getWriter());

    Marshaller marshaller = jc.createMarshaller();
    marshaller.marshal(directDelOrderReturn, xmlStreamWriter);

  }
  
  
	public BigDecimal getQuantityReservedByWarehouseType(
			Map<String, List<Order>> retailSupply, String warehouseType, String searchKey) {
		String orderIdsConcat = "";
		for (Entry<String, List<Order>> ordersByWarehouse : retailSupply
				.entrySet()) {
			List<Order> orders = ordersByWarehouse.getValue();
			for (Order order : orders) {
				orderIdsConcat = orderIdsConcat.concat("'" + order.getId()
						+ "',");
			}
		}
		BigDecimal quantityReservedByWarehouseType = BigDecimal.ZERO;
		if (orderIdsConcat.length() > 0
				&& orderIdsConcat.charAt(orderIdsConcat.length() - 1) == ',')
			orderIdsConcat = orderIdsConcat.substring(0, orderIdsConcat
					.length() - 1);
		if (orderIdsConcat!=null && orderIdsConcat.length() > 0) {
			String qry = " select coalesce(sum(ordlist.orderedQuantity),0) from Order ord"
					+ " join ord.orderLineList ordlist"
					+ " join ordlist.warehouse ware"
					+ " where ord.id in ("
					+ orderIdsConcat
					+ ") "
					+ " and ware.ibdoWarehousetype= :warehouseType";
			if (!searchKey.equals("") && searchKey.equals("HUB")) {
				qry = qry + " and ware.searchKey='HUB'";
			} else if (!searchKey.equals("") && searchKey.equals("NOT HUB")) {
				qry = qry + " and ware.searchKey<>'HUB'";
			}
			Query query = OBDal.getInstance().getSession().createQuery(qry);
			query.setParameter("warehouseType", warehouseType);

			List<BigDecimal> queryList = query.list();
			// BigDecimal quantityReservedByWarehouseType = BigDecimal.ZERO;
			if (queryList != null & queryList.size() > 0) {
				quantityReservedByWarehouseType = queryList.get(0);
			}
		}
		return quantityReservedByWarehouseType;
	}

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("method Not Implemented");
  }

}
