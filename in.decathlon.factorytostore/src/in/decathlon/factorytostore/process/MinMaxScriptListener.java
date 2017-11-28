package in.decathlon.factorytostore.process;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.web.WebService;

public class MinMaxScriptListener implements WebService {

	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		JSONObject responseAvailQty = new JSONObject();
		JSONArray responseArr = new JSONArray();
		try {
			String productIDs = request.getParameter("productIDs");
			String[] products = productIDs.split(",");

			for (int i = 0; i < products.length; i++) {
				getAvailableQty(products[i], responseAvailQty);
			}
			responseArr.put(responseAvailQty);
			writeResult(response, responseArr.toString());
		} catch (Exception e) {
			responseArr.put(responseAvailQty);
			writeResult(response, responseArr.toString());
			throw e;
		}
	}

	private void getAvailableQty(String productId, JSONObject responseAvailQty)
			throws JSONException {
		BigDecimal availableQtySupplier = BigDecimal.ZERO;
		for (Warehouse wHouse : externalWarehouses()) {
			availableQtySupplier = availableQtySupplier
					.add(getAvailableQtyByWarehouse(productId, wHouse));
		}
		responseAvailQty.put(productId, availableQtySupplier);
	}

	private BigDecimal getAvailableQtyByWarehouse(String productId,
			Warehouse wHouse) {
		BigDecimal reservedQty = getReservedQty(wHouse, productId);
		BigDecimal qtyInWarehouse = getActuallQty(wHouse, productId);
		BigDecimal availableQty = qtyInWarehouse.subtract(reservedQty);
		return availableQty;
	}

	private List<Warehouse> externalWarehouses() {
		OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(
				Warehouse.class);
		obCriteria.add(Restrictions.eq(Warehouse.PROPERTY_IBDOWAREHOUSETYPE,
				"FACST_External"));
		return obCriteria.list();
	}

	private BigDecimal getReservedQty(Warehouse warehouse, String productId) {
		BigDecimal resrvdQty = BigDecimal.ZERO;
		BigDecimal relsdQty = BigDecimal.ZERO;
		BigDecimal availableQty = BigDecimal.ZERO;
		String qry = "select sum(quantity),sum(released) from MaterialMgmtReservationStock mrs where mrs.reservation.product.id="
				+ ":productId and mrs.storageBin.warehouse.id= :warehouseId and mrs.storageBin.oBWHSType = 'ST' and mrs.storageBin.id not in "
				+ "(select returnlocator from Warehouse w where w.ibdoWarehousetype ='FACST_External')";
		Query storageQuery = OBDal.getInstance().getSession().createQuery(qry);
		storageQuery.setParameter("productId", productId);
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

	private BigDecimal getActuallQty(Warehouse warehouse, String productId) {
		BigDecimal totalQty = BigDecimal.ZERO;
		String qry = "select sum(quantityOnHand) from MaterialMgmtStorageDetail sd where sd.storageBin.warehouse.id="
				+ " :warehouseId and sd.product.id= :productId and sd.storageBin.oBWHSType = 'ST' and sd.storageBin.id not in "
				+ "(select returnlocator from Warehouse w where w.ibdoWarehousetype ='FACST_External')";
		Query storageQuery = OBDal.getInstance().getSession().createQuery(qry);
		storageQuery.setParameter("warehouseId", warehouse.getId());
		storageQuery.setParameter("productId", productId);
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

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	private void writeResult(HttpServletResponse response, String result)
			throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("Content-Type", "application/json;charset=UTF-8");

		final Writer w = response.getWriter();
		w.write(result);
		w.close();
	}

}
