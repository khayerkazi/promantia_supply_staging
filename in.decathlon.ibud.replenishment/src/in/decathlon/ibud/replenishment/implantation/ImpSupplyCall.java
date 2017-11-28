package in.decathlon.ibud.replenishment.implantation;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.orders.data.OrderLineState;
import in.decathlon.ibud.orders.data.OrderReturn;
import in.decathlon.ibud.orders.data.OrderState;
import in.decathlon.ibud.replenishment.bulk.ReplenishmentUtils;
import in.decathlon.ibud.replenishment.bulk.StatSupplyReturn;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

public class ImpSupplyCall {
	private static final Logger LOG = Logger.getLogger(ImpSupplyCall.class);
	private static final String wsName = "in.decathlon.ibud.orders.OrdersWS_bulk";

	public void processRequest(List<Order> ords, Map<String, String> computed) throws Exception {
		StatSupplyReturn ret = new StatSupplyReturn();

		// Create the JSON of orders
		JSONObject orders = ReplenishmentUtils.generateJsonPO(ords);

		// Send the order to supply and get the result
		LOG.error("Before sending order to supply");
		OrderReturn supplyRet = send(orders.toString());
		LOG.error("After sending order");

		Map<String, OrderState> retOrder = supplyRet.getOrders();
		for (Order o : ords) {
			OrderState os = retOrder.get(o.getId());
			if (os != null) {
				o.setSwPoReference(os.getPoRef());
				if (os.getNbSo() == 0) {
					o.setOrderReference(null);
					o.setDocumentStatus("VO");
					ret.incVoid();
				} else {
					for (OrderLine ol : o.getOrderLineList()) {
						OrderLineState ols = os.getLines().get(ol.getId());
						if (ols != null) {
							ol.setSwConfirmedqty((long) ols.getConfirmedQty());
							ImplantationProcessEnhanced.setBlockedQty(ol);
						}
					}
					OBDal.getInstance().flush();
					purchaseOrderBookProcess(o);
					o.setIbdoCreateso(true);
					ret.incCompleted();
				}
				OBDal.getInstance().flush();
			}else{
				ret.incDraft();
			}
		}
		
		OBDal.getInstance().flush();
	}

	private void purchaseOrderBookProcess(Order ord) throws Exception {
		String ordId = ord.getId();
		LOG.error("booking order " + ord.getDocumentNo() + " having action "
				+ ord.getDocumentAction());
		BusinessEntityMapper.executeProcess(ordId, "104", "SELECT * FROM c_order_post(?)");
		OBDal.getInstance().refresh(ord);
	}


	public OrderReturn send(String content) throws Exception {
		final String userName = IbudConfig.getSupplyUsername();
		final String password = IbudConfig.getSupplyPassword();

		String host = IbudConfig.getSupplyHost();
		int port = IbudConfig.getSupplyPort();

		String context = IbudConfig.getSupplyContext().concat("/ws/");

		String urlPO = "http://" + host + ":" + port + "/" + context + wsName;

		LOG.debug("Will send request to " + urlPO);

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password.toCharArray());
			}
		});

		DefaultHttpClient httpclient = new DefaultHttpClient();

		httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials(userName, password));

		HttpPost httpPost = new HttpPost(urlPO);

		ByteArrayEntity b = new ByteArrayEntity(content.getBytes());
		httpPost.setEntity(b);

		HttpResponse response = httpclient.execute(httpPost);

		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			LOG.error(response.getStatusLine());
			Header[] headrs = response.getAllHeaders();
			for (Header hd : headrs) {
				if (hd.getName().equals("Error")) {
					throw new OBException(hd.getValue());
				}
			}
			throw new OBException(response.getStatusLine().toString());
		}

		JSONObject obj = new JSONObject(EntityUtils.toString(response.getEntity()));

		JAXBContext jc = JAXBContext.newInstance(OrderReturn.class);

		MappedNamespaceConvention con = new MappedNamespaceConvention(new Configuration());
		XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj, con);

		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (OrderReturn) unmarshaller.unmarshal(xmlStreamReader);
	}
	
}
