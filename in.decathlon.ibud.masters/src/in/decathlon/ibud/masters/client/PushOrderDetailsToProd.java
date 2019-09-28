package in.decathlon.ibud.masters.client;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;

import com.ibm.icu.text.SimpleDateFormat;

public class PushOrderDetailsToProd implements Process {

	private static Logger log = Logger.getLogger(PushOrderDetailsToProd.class);
	private ProcessLogger logger;

	@Override
	public void execute(ProcessBundle bundle) throws Exception {
		HttpURLConnection HttpUrlConnection = null;
		BufferedReader reader = null;
		String orderReference = null;
		String anomaly = null;
		OutputStreamWriter wr = null;
		InputStream is = null;
		TokenGenerator tokens = new TokenGenerator();
		String token = tokens.generateToken();
		System.out.println(token);

		String postUrl = OBPropertiesProvider.getInstance()
				.getOpenbravoProperties().getProperty("eas.posturl");
		String x_env = OBPropertiesProvider.getInstance()
				.getOpenbravoProperties().getProperty("eas.x_env");

		Date javaUtilDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ssZ");

		// get all purchase order details
		List<Order> orderList = getPurchaseOrder();
		for (Order order : orderList) {
			JSONObject orderObject = new JSONObject();
			orderObject.put("origin", "RETAIL");
			orderObject.put("externalNumber",
					Integer.parseInt(order.getDocumentNo()));
			orderObject.put("orderType", "Z");
			orderObject.put("orderStatus", "NV");
			orderObject.put("orderCreation",
					formatter.format(order.getCreationDate()).toString()
							.substring(0, 19)
							+ "Z");

			if (order.getSWEMSwExpdeldate() != null)
				orderObject.put("requestedDeliveryDate",
						formatter.format(order.getSWEMSwExpdeldate())
								.toString().substring(0, 19)
								+ "Z");
			else
				orderObject.put("requestedDeliveryDate",
						formatter.format(order.getCreationDate()).toString()
								.substring(0, 19)
								+ "Z");

			if (order.getSWEMSwExpdeldate() != null)
				orderObject.put("requestedShipmentDate",
						formatter.format(order.getSWEMSwEstshipdate())
								.toString().substring(0, 19)
								+ "Z");
			else
				orderObject.put("requestedShipmentDate",
						formatter.format(order.getCreationDate()).toString()
								.substring(0, 19)
								+ "Z");

			if (order.getScheduledDeliveryDate() != null)
				orderObject.put("scheduledDeliveryDate",
						formatter.format(order.getScheduledDeliveryDate())
								.toString().substring(0, 19)
								+ "Z");
			else
				orderObject.put("scheduleDeliveryDate",
						formatter.format(order.getCreationDate()).toString()
								.substring(0, 19)
								+ "Z");

			JSONArray customerJsonArray = new JSONArray();
			customerJsonArray.put("7");
			customerJsonArray.put("10006");
			customerJsonArray.put("10006");
			orderObject.put("customer", customerJsonArray);

			JSONArray supplierJsonArray = new JSONArray();
			supplierJsonArray.put("2");
			supplierJsonArray.put("31094");
			supplierJsonArray.put("31094");

			orderObject.put("supplier", supplierJsonArray);

			JSONArray deliveryJsonArray = new JSONArray();
			deliveryJsonArray.put("7");
			deliveryJsonArray.put("10006");
			deliveryJsonArray.put("10006");
			orderObject.put("delivery", deliveryJsonArray);

			JSONArray orderLineArray = new JSONArray();

			for (OrderLine lines : order.getOrderLineList()) {
				JSONObject orderlineJson = new JSONObject();

				orderlineJson.put("number", lines.getLineNo());
				orderlineJson.put("status", "A");
				orderlineJson.put("item",
						Long.parseLong(lines.getProduct().getName()));
				orderlineJson.put("quantity", lines.getOrderedQuantity());
				orderlineJson.put("price", lines.getGrossUnitPrice());
				orderlineJson.put("currency", lines.getCurrency().getISOCode());
				orderLineArray.put(orderlineJson);
			}
			orderObject.put("orderLines", orderLineArray);

			try {

				if (postUrl == null) {
					throw new Exception(
							"PushOrderDetailsToProd: eas.posturl configuration is missing");
				}
				if (x_env == null) {
					throw new Exception(
							"PushOrderDetailsToProd: eas.x_env configuration is missing");
				}

				URL urlObj = new URL(postUrl);
				HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
				HttpUrlConnection.setDoOutput(true);
				HttpUrlConnection.setRequestMethod("POST");
				HttpUrlConnection.setRequestProperty("Accept-Version", "1.0");
				HttpUrlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				HttpUrlConnection.setRequestProperty("x-env", x_env);
				HttpUrlConnection.setRequestProperty("Authorization", "Bearer "
						+ token);
				HttpUrlConnection.connect();
				
				try(OutputStream os = HttpUrlConnection.getOutputStream()) {
					byte[] input = orderObject.toString().getBytes(StandardCharsets.UTF_8);
					os.write(input, 0, input.length);
					os.flush();
				}

				if (HttpUrlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					// log.error("TokenGenerator: Error in response from Token API Generator, Response:"
					// + HttpUrlConnection.getResponseCode());

					is = HttpUrlConnection.getInputStream();
					reader = new BufferedReader(new InputStreamReader((is)));
					String tmpStr = null;
					String result = null;
					while ((tmpStr = reader.readLine()) != null) {
						result = tmpStr;
						JSONObject responseJson = new JSONObject(result);
						orderReference = responseJson.getString("orderNumber");
						anomaly = responseJson.getString("anomaly");
					}

					throw new Exception(anomaly);
					// throw new Exception(
					// "PushOrderDetailsToProd:Error in response from push order to prod API Generator, Response:"
					// + HttpUrlConnection.getResponseCode());
				}
				is = HttpUrlConnection.getInputStream();
				reader = new BufferedReader(new InputStreamReader((is)));
				String tmpStr1 = null;
				String result1 = null;
				while ((tmpStr1 = reader.readLine()) != null) {
					result1 = tmpStr1;
				}
				JSONObject responseJson = new JSONObject(result1);
				orderReference = responseJson.getString("orderNumber");
				anomaly = responseJson.getString("anomaly");
				if (orderReference == null || (!anomaly.equals("null"))) {
					// log.error("TokenGenerator:TokenType null or invalid");
					throw new Exception(anomaly);
				} else {
					order.setDescription(orderReference);
				}
			} catch (Exception e) {
				throw new Exception(
						"PushOrderToProd:error while creating connection with POST API",e);
			} finally {
				try {
					if (is != null) {
						is.close();
					}
					if (wr != null) {
						wr.close();
					}
					if (HttpUrlConnection != null) {
						HttpUrlConnection.disconnect();
					}
				} catch (Exception e) {
					throw new Exception(
							"Exception while closing outputstream or HttpUrlConnection",
							e);
				}
			}
			// logger.logln(orderObject.toString());
		}
	}

	private List<Order> getPurchaseOrder() {

		String strHql = "select co from Order co, OrderLine line , "
				+ "CL_DPP_SEQNO cd ,BusinessPartner bp "
				+ "where co.sWEMSwPostatus in ('SO') and co.id = line.salesOrder.id "
				+ "and bp.clSupplierno = line.sWEMSwSuppliercode "
				+ "and co.transactionDocument.id = 'C7CD4AC8AC414678A525AB7AE20D718C' "
				+ "and  co.imsapDuplicatesapPo != 'Y' and bp.rCSource = 'DPP'";

		// h.em_sw_postatus in ('SO','MO','VO') and
		Query query = OBDal.getInstance().getSession().createQuery(strHql);
		List<Order> orderlist = query.list();
		return orderlist;
	}
}
