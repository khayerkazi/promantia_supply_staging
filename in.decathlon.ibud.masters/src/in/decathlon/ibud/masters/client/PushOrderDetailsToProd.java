package in.decathlon.ibud.masters.client;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import com.ibm.icu.text.SimpleDateFormat;

public class PushOrderDetailsToProd implements Process {

	// OBError msg = new OBError();
	final Logger log = Logger.getLogger(PushOrderDetailsToProd.class);

	@SuppressWarnings("null")
	@Override
	public void execute(ProcessBundle bundle) throws Exception {
		ProcessLogger logger = bundle.getLogger();
		HttpURLConnection HttpUrlConnection = null;
		BufferedReader reader = null;
		String orderReference = null;
		String anomaly = null;
		OutputStreamWriter wr = null;
		InputStream is = null;
		String postUrl = null;
		String x_env = null;
		HashMap<String, String> orderSentList = new HashMap<String, String>();
		List<Order> orderList = null;

		// Generate Token
		log.info("Started Generating token");
		TokenGenerator tokens = new TokenGenerator();
		String token = tokens.generateToken();
		System.out.println(token);

		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ssZ");

		// Parameters for Post API
		postUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
				.getProperty("eas.posturl");
		x_env = OBPropertiesProvider.getInstance().getOpenbravoProperties()
				.getProperty("eas.x_env");

		if (postUrl == null) {
			log.error("Error :Missing url");
			logger.logln("Please configure Post url as it is missing");

			throw new Exception(
					"PushOrderDetailsToProd: eas.posturl configuration is missing");
		}
		if (x_env == null) {
			log.info("x-env header configuration is missing");
			logger.logln("x-env header configuration is missing");
		}

	


		// get all purchase order details
		orderList = getPurchaseOrder();
		try {
			if (orderList != null) {
				for (Order order : orderList) {
					JSONObject orderObject = new JSONObject();
					orderObject.put("origin", "RETAIL");
					orderObject.put("externalNumber",
							Integer.parseInt(order.getDocumentNo()));
					orderObject.put("orderType", "Z");
					orderObject.put("orderStatus", "NV");
					orderObject.put("orderCreation",
							formatter.format(order.getCreationDate())
									.toString().substring(0, 19)
									+ "Z");
					orderObject.put("requestedDeliveryDate",
							formatter.format(order.getSWEMSwExpdeldate())
									.toString().substring(0, 19)
									+ "Z");
					orderObject.put("requestedShipmentDate",
							formatter.format(order.getSWEMSwEstshipdate())
									.toString().substring(0, 19)
									+ "Z");
					orderObject.put("scheduledDeliveryDate",
							formatter.format(order.getScheduledDeliveryDate())
									.toString().substring(0, 19)
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

					// Get orderline information for every order

					for (OrderLine lines : order.getOrderLineList()) {
						JSONObject orderlineJson = new JSONObject();

						orderlineJson.put("number", lines.getLineNo());
						orderlineJson.put("status", "A");
						orderlineJson.put("item",
								Long.parseLong(lines.getProduct().getName()));
						orderlineJson.put("quantity",
								lines.getOrderedQuantity());
						orderlineJson.put("price", lines.getGrossUnitPrice());
						orderlineJson.put("currency", lines.getCurrency()
								.getISOCode());
						orderLineArray.put(orderlineJson);
					}
					orderObject.put("orderLines", orderLineArray);
					
					// Connect with prod.com
					try {
						URL urlObj = new URL(postUrl);
						HttpUrlConnection = (HttpURLConnection) urlObj
								.openConnection();
						HttpUrlConnection.setDoOutput(true);
						HttpUrlConnection.setRequestMethod("POST");
						HttpUrlConnection.setRequestProperty("Accept-Version",
								"1.0");
						HttpUrlConnection.setRequestProperty("Content-Type",
								"application/json; charset=UTF-8");
						HttpUrlConnection.setRequestProperty("x-env", x_env);
						HttpUrlConnection.setRequestProperty("Authorization",
								"Bearer " + token);
						HttpUrlConnection.connect();
					} catch (Exception e1) {
						e1.printStackTrace();
						log.error("PushOrderDetailsToProd:Error while connecting with prod.com . Please check header details"
								+ e1);
						logger.logln(e1.getMessage());
					}

					
					try (OutputStream os = HttpUrlConnection.getOutputStream()) {
						byte[] input = orderObject.toString().getBytes(
								StandardCharsets.UTF_8);
						os.write(input, 0, input.length);
						os.flush();
					}
					
					

					if (HttpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
						log.info("Getting response from prod.com for created order ");
						is = HttpUrlConnection.getInputStream();
						reader = new BufferedReader(new InputStreamReader((is)));
						String tmpStr = null;
						String result = null;
						while ((tmpStr = reader.readLine()) != null) {
							result = tmpStr;
						}
						JSONObject responseJson = new JSONObject(result);
						orderReference = responseJson.getString("orderNumber");
						anomaly = responseJson.getString("anomaly");
						orderSentList.put(order.getId(), orderReference);
						if (is != null) 
							is.close();
						if (HttpUrlConnection != null) 
							HttpUrlConnection.disconnect();

					} else {
						anomaly = HttpUrlConnection.getResponseMessage();
						throw new OBException(
								anomaly
										+ " Order already exist or some data is missing");
					}
				}
			} else {
				log.info("No Order present at this moment ");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(
					"PushOrderToProd:error while pushing order details to prod.com please check data ",
					e);

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
				e.printStackTrace();
				throw new Exception(
						"Exception while closing HttpUrlConnection", e);
			}
		}

		// Set Orderreference number picked from prod.com and message in
		// purchase order
		Iterator iterator = orderSentList.entrySet().iterator();
		while (iterator.hasNext()) {
			OBContext.setAdminMode(false);
			try {
				Map.Entry mapElement = (Map.Entry) iterator.next();
				String orderId = (String) mapElement.getKey();
				Order order = getOrder(orderId);
				String reference = (String) mapElement.getValue();
				order.setIbudProdStatus("OS");
				order.setOrderReference(reference);
				order.setIbudProdMsgPost("Order Successfully sent");
				order.setProcessNow(true);
				OBDal.getInstance().save(order);
				OBDal.getInstance().flush();
			} finally {
				OBContext.restorePreviousMode();
			}
		}
	}

	private Order getOrder(String orderId) {
		String hql = "Select distinct ord from Order ord where ord.id = '"
				+ orderId + "'";
		Query qry = OBDal.getInstance().getSession().createQuery(hql);
		return (Order) qry.uniqueResult();
	}

	private List<Order> getPurchaseOrder() {
		String strHql = "select distinct co from Order co, OrderLine line ,"
				+ "	CL_DPP_SEQNO cd ,BusinessPartner bp, Ibud_ServerTime ibud "
				+ "where co.sWEMSwPostatus in ('SO') and co.id = line.salesOrder.id "
				+ "and bp.clSupplierno = line.sWEMSwSuppliercode 	"
				+ "and co.transactionDocument.id = 'C7CD4AC8AC414678A525AB7AE20D718C'  "
				+ "and  co.imsapDuplicatesapPo != 'Y' and bp.rCSource = 'DPP'  "
				+ "and  line.grossUnitPrice > 0 and line.orderedQuantity > 0 a"
				+ "nd co.orderReference = null and  ibud.serviceKey = 'FlexProcess' "
				+ "and co.updated > ibud.lastupdated";

		Query query = OBDal.getInstance().getSession().createQuery(strHql);
		if (query.list().size() > 0)
			return query.list();
		else
			return null;
	}
}
