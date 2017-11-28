package in.decathlon.ibud.orders.client;

import in.decathlon.ibud.orders.data.OrderReturn;

import java.io.IOException;
import java.util.ArrayList;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

public class SalesOrderBulkListner implements WebService {
	private static final Logger LOG = Logger.getLogger(SalesOrderBulkListner.class);

	private static final String cwhWarehouseId = "CWH";
	private static final String rwhWarehouseId = "RWH";
	private static final String hubWarehouseId = "HUB";

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		throw new NotImplementedException("method Not Implemented");

	}

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		OrderReturn orderReturn = new OrderReturn();

		SaveSupplyDataBulk saveSuppData = new SaveSupplyDataBulk();

		JSONObject orders = new JSONObject(getContentFromRequest(request));
		JSONArray ordersArray = orders.getJSONArray("data");

		// Get different product
		Set<String> products = new HashSet<String>();
		for (int i = 0; i < ordersArray.length(); i++) {
			JSONObject ordObj = (JSONObject) ordersArray.get(i);
			JSONArray ordLines = ordObj.getJSONArray("Lines");
			for (int j = 0; j < ordLines.length(); j++) {
				JSONObject ordLine = ordLines.getJSONObject(j);
				products.add(ordLine.getString("product"));
			}
		}
		getStock(products, orderReturn);

		LOG.info("Orders from Store to create sales orders: " + orders.length());

		Map<String, List<Order>> retailSupply = new HashMap<String, List<Order>>();
                List<Order> suporders = new ArrayList<Order>();

		try {

			long start = System.currentTimeMillis();

			for (int i = 0; i < ordersArray.length(); i++) {
				JSONObject ordObj = (JSONObject) ordersArray.get(i);
				JSONObject ordHeader = (JSONObject) ordObj.get("Header");

				JSONArray ordLines = ordObj.getJSONArray("Lines");

				try {
					saveSuppData.distributeSalesOrder(orderReturn, retailSupply, ordHeader, ordLines, suporders);
					LOG.debug("Order finished");
				} catch (Exception e) {
					LOG.warn("Order finished with error", e);
				}
			}

			LOG.debug("It take " + ((System.currentTimeMillis() - start) / 1000) + " to insert the orders");

			OBDal.getInstance().flush();
			for (Entry<String, List<Order>> e : retailSupply.entrySet()) {
				for (Order o : e.getValue()) {
					saveSuppData.getConfirmedQty(orderReturn.getOrders().get(e.getKey()), o);
				}
			}

			writeResult(response, orderReturn);

		} catch (Exception e) {
			response.setHeader("Error", e.toString());
			LOG.error(e);
			throw e;
		}
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		throw new NotImplementedException("method Not Implemented");

	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		throw new NotImplementedException("method Not Implemented");

	}

	private String getContentFromRequest(HttpServletRequest request) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(request.getInputStream(), writer, "UTF-8");
		String orders = writer.toString();
		return orders;
	}

	private void writeResult(HttpServletResponse response, OrderReturn orderReturn) throws IOException, JAXBException {
		response.setContentType("application/json;charset=UTF-8");
		response.setHeader("Content-Type", "application/json;charset=UTF-8");

		JAXBContext jc = JAXBContext.newInstance(OrderReturn.class);

		MappedNamespaceConvention con = new MappedNamespaceConvention(new Configuration());
		XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, response.getWriter());

		Marshaller marshaller = jc.createMarshaller();
		marshaller.marshal(orderReturn, xmlStreamWriter);

	}

	private void getStock(Set<String> products, OrderReturn orderReturn) throws Exception {
	      if (products == null || products.size()==0) {
	    	  return;
	      }
	      
		ComputeStock cs = new ComputeStock();
		cs.createTemporaryTable();
		cs.insertIntoTableProduct(products);
		cs.computeStock("cacstock", "cacrelease", cwhWarehouseId);
		cs.computeStock("carstock", "carrelease", rwhWarehouseId);
		cs.computeStock("hubstock", "hubrelease", hubWarehouseId);

		cs.retriveStock(orderReturn);
	  }
}
