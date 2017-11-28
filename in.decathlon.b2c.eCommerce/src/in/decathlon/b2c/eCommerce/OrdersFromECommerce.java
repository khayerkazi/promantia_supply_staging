package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.orders.process.ImmediateSOonPO;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVWriter;

public class OrdersFromECommerce implements WebService {

	// Properties Singleton class instantiation
	Properties p = ECommerceUtil.getInstance().getProperties();
	private static final Logger log = Logger.getLogger(OrdersFromECommerce.class);
	
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
		String finename = p.getProperty("b2cCsvPath")+"OrderSyncLog"+"-"+dt.format(new Date())+".csv";
		// csv file
		final File file = new File(finename);
		List<String[]> data = new ArrayList<String[]>();
		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
			// CSV Headings
			data.add(new String[] {"Avetti Order ID", "ERP Order Doc Num 1", "ERP Order Doc Num 2", "Order Qty", "Confirmed Qty", "Time Stamp"});
			
		}
		final CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile(), true));
		if (!file.exists()) {
			writer.writeAll(data);
		}
		
		// Fetch the values from the URL(get)
		final String vendorID = request.getParameter("vid");
		final String userID = request.getParameter("ecomID");
		final String pwd = request.getParameter("ecomP");
		
		// SOAP Connection Initialisation
		final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		final SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		final DecimalFormat df = new DecimalFormat("#.##");
		
		// get the list of orders from findByItemsState with 1(Order State) and 3(Item State)
		try {
			// Sending Request to Order Service findByItemState method
			final SOAPMessage soapResponse = soapConnection.call(createfindByItem(vendorID, userID, pwd), p.getProperty("serverURL")+"orderservice");
			
			// Reading the findByItemResponse
			final SOAPBody body = soapResponse.getSOAPBody();
		    final NodeList returnList = body.getElementsByTagName("ns1:OrderData");
		    final List<OrdersDTO> orders = new ArrayList<OrdersDTO>(); 
		    if(returnList.getLength() > 0) {
			    
			    for (int k = 0; k < returnList.getLength(); k++) {

			    	// Store Order ID, OrderDataID, date of order(From created time) and invoicecustomer
			    	final OrdersDTO order =  new OrdersDTO();
			    	order.setCustSatisfaction("");
			    	// Calling the Order Properties SOAP webservice by Order ID
			    	final SOAPMessage findOrderProperties1Response = soapConnection.call(findOrderProperties1(vendorID, userID, pwd, getNodeValue("orderid", returnList.item(k).getChildNodes())), p.getProperty("serverURL")+"orderservice");
			    	final SOAPBody findOrderProperties1ResponseBody = findOrderProperties1Response.getSOAPBody();
					final NodeList orderPropertiesList = findOrderProperties1ResponseBody.getElementsByTagName("ns2:Orderproperties");
					for (int num = 0; num < orderPropertiesList.getLength(); num++) {
						if (getNodeValue("propname", orderPropertiesList.item(num).getChildNodes()).equals("Q-Feedback")) {
							if (getNodeValue("propvalue", orderPropertiesList.item(num).getChildNodes()).equals("Very Satisfied")) {
								order.setCustSatisfaction("3");
							} else if (getNodeValue("propvalue", orderPropertiesList.item(num).getChildNodes()).equals("Satisfied")) {
								order.setCustSatisfaction("2");
							} else if (getNodeValue("propvalue", orderPropertiesList.item(num).getChildNodes()).equals("Not Satisfied")) {
								order.setCustSatisfaction("1");
							}
						}
					}
			    	order.setOrderId(getNodeValue("orderid", returnList.item(k).getChildNodes()));
			    	order.setOrderDataId(getNodeValue("orderdataid", returnList.item(k).getChildNodes()));
			    	order.setOrderDate(getNodeValue("createtime", returnList.item(k).getChildNodes()));
			    	order.setInvoiceCustomer(getNodeValue("invoicecustomer", returnList.item(k).getChildNodes()));
			    	order.setLinesAmount(getNodeValue("paytot", returnList.item(k).getChildNodes()));
			    	order.setShipCost(getNodeValue("shiptot", returnList.item(k).getChildNodes()));
			    	// documentNumber to create PO
			    	order.setDocumentNumber("*ECOM*"+getNodeValue("orderid", returnList.item(k).getChildNodes()));
			    	// Adding the OrdersDTO object to the list
			    	orders.add(order);
			    	
			    }
		    }
		    int processed=0;
		    // Iterating the orders list in the for-each
			for (OrdersDTO order : orders) {
				
				// Call findOrderItems with OrderDataID
				// sending Request to Order Service findByItemStatus method
				final SOAPMessage findOrderItemsResponse = soapConnection.call(findOrderItems(vendorID, userID, pwd, order.getOrderDataId()), p.getProperty("serverURL")+"orderservice");
				final SOAPBody orderItemsResponseBody = findOrderItemsResponse.getSOAPBody();
				
				final NodeList returnList1 = orderItemsResponseBody.getElementsByTagName("ns2:OrderItem");
				
				final List<OrdersDataDTO> orderItmes = new ArrayList<OrdersDataDTO>();
				final Map<String, OrdersDataDTO> prodMap = new HashMap<String, OrdersDataDTO>();
				// Store the Items which has orderitemstate value 3
				for (int i = 0; i < returnList1.getLength(); i++) {
					
					final String orderItemState = getNodeValue("orderitemstate", returnList1.item(i).getChildNodes());
					if(orderItemState.equals("3")) {
						
						final OrdersDataDTO orderItem = new OrdersDataDTO();
						orderItem.setItemCode(getNodeValue("itemcode", returnList1.item(i).getChildNodes()));
						orderItem.setItemCost(getNodeValue("itemcost", returnList1.item(i).getChildNodes()));
						orderItem.setOrderItemId(getNodeValue("orderitemid", returnList1.item(i).getChildNodes()));
						orderItem.setPayTotal(getNodeValue("paytot", returnList1.item(i).getChildNodes()));
						orderItem.setQuantity(getNodeValue("qty", returnList1.item(i).getChildNodes()));
						orderItem.setShippedTotal(getNodeValue("shipcost", returnList1.item(i).getChildNodes()));
						orderItem.setShipTax(getNodeValue("taxrateGST", returnList1.item(i).getChildNodes()));
						double itemCost = Double.parseDouble(getNodeValue("itemcost", returnList1.item(i).getChildNodes()));
						double taxRate = Double.parseDouble(getNodeValue("taxrateGST", returnList1.item(i).getChildNodes()));
						double tax = (itemCost - ((itemCost * 100) / (100 + (taxRate*100))));
						orderItem.setTaxAmount(""+df.format(tax)+"");
						log.info("Amt"+df.format(tax));
						final OBQuery<Product> product = OBDal.getInstance().createQuery(Product.class, "as p where p.name='"+getNodeValue("itemcode", returnList1.item(i).getChildNodes())+"'");
						if(product.list().size() > 0) {
							// Get ERP ID's for those products and attach to above orderItem object
							final Product prodInfo = product.list().get(0);
							orderItem.setProductId(prodInfo.getId());
							prodMap.put(prodInfo.getId(), orderItem);
						}
						orderItmes.add(orderItem);
						
					}
				}
				String bPartnerSO = "";
				final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,	"as bp where bp.rCOxylane='"+order.getInvoiceCustomer()+"'");
				if(bpQuery.list().size() > 0 ){
					final BusinessPartner bPartner = bpQuery.list().get(0);
					bPartnerSO = bPartner.getId();
				} else {
					continue;
				}
				Double chargeAmtPO = 0.0;
				Double poGrandTotal = 0.0;
				Double poNetTotal = 0.0;
				for(OrdersDataDTO orderItem : orderItmes) {
					String taxId = p.getProperty("ecomsupplytax550");
				    if(orderItem.getShipTax().equals("0.055")) {
				    	taxId = p.getProperty("ecomsupplytax550");
				    } else if(orderItem.getShipTax().equals("0.145")) {
				    	taxId = p.getProperty("ecomsupplytax1450");
				    }
					chargeAmtPO = chargeAmtPO + Double.parseDouble(orderItem.getShippedTotal());
				}
				// create PO header based on OrderID
				final JSONObject job = new JSONObject("{\"_entityName\":\"Order\",\"client\":\""+p.getProperty("ecomsupplyClient")+"\",\"client._identifier\":\"\",\"organization\":\""+p.getProperty("ecomsupplyOrg")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"updatedBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"salesTransaction\":\"false\",\"documentNo\":\""+order.getDocumentNumber()+"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""+p.getProperty("ecomsupplydocumentType")+"\",\"documentType._identifier\":\"\",\"transactionDocument\":\""+p.getProperty("ecomsupplydocumentType")+"\",\"transactionDocument._identifier\":\"\",\"description\":\"Online Order\",\"orderDate\":\""+new Timestamp(new Date().getTime())+"\",\"scheduledDeliveryDate\":\"\",\"accountingDate\":\""+new Timestamp(new Date().getTime())+"\",\"businessPartner\":\""+p.getProperty("ecomsupplybusinessPartner")+"\",\"businessPartner._identifier\":\"\",\"invoiceAddress\":\""+p.getProperty("ecomsupplyinvoiceAddress")+"\",\"invoiceAddress._identifier\":\"\",\"partnerAddress\":\""+p.getProperty("ecomsupplyinvoiceAddress")+"\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\""+p.getProperty("ecomcurrency")+"\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\""+p.getProperty("ecomsupplypaymentTerms")+"\",\"summedLineAmount\":\"\",\"paymentTerms._identifier\":\"\",\"invoiceTerms\":\"D\",\"deliveryTerms\":\"A\",\"freightCostRule\":\"I\",\"deliveryMethod\":\"P\",\"priority\":\"5\",\"warehouse\":\""+p.getProperty("ecomsupplywarehouse")+"\",\"warehouse._identifier\":\"\",\"priceList\":\""+p.getProperty("ecomsupplypriceList")+"\",\"priceList._identifier\":\"\",\"userContact\":\""+OBContext.getOBContext().getUser().getId()+"\",\"paymentMethod\":\""+p.getProperty("ecomsupplypaymentMethod")+"\",\"paymentMethod._identifier\":\"\",\"dSTotalpriceadj\":0,\"dSPosno\":0,\"dSEMDsRatesatisfaction\":\""+order.getCustSatisfaction()+"\",\"dSChargeAmt\":"+chargeAmtPO+",\"generateTemplate\":\"\",\"copyFromPO\":\"\",\"pickFromShipment\":\"\",\"receiveMaterials\":\"\",\"createInvoice\":\"\",\"addOrphanLine\":\"\",\"calculatePromotions\":\"\",\"quotation\":\"\",\"reservationStatus\":\"NR\",\"dSReceiptno\":\""+order.getOrderId()+"\",\"swSendorder\":\"\",\"swModorder\":\"\",\"swDelorder\":\"\",\"obwplGeneratepicking\":\"\",\"sWEMSwPostatus\":\"\",\"grandTotalAmount\":\"\",\"summedLineAmount\":\"\",\"dSEMDsGrandTotalAmt\":\"\",\"dsBpartner\":\""+bPartnerSO+"\",\"salesRepresentative\":\""+OBContext.getOBContext().getUser().getId()+"\",\"ibdoCreateso\":\"true\"}");
				final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(JsonToDataConverter.class);
				// Converting JSON Object to Openbravo Business object 
				final BaseOBObject bob = toBaseOBObject.toBaseOBObject(job);
				
				// Database Connections and Database insertions 
				OBDal.getInstance().save(bob);
				OBDal.getInstance().flush();
				
				final String poHeaderId = (String) bob.getId();
				OBDal.getInstance().commitAndClose();
				int lineNumPO = 0;
				final List<BaseOBObject> oLines = new ArrayList<BaseOBObject>();
				// create PO lines based on orderItmes list
				for(OrdersDataDTO orderItem : orderItmes) {
					lineNumPO++;
					double taxRate = 0.0;
					String taxId = p.getProperty("ecomsupplytax550");
				    if(orderItem.getShipTax().equals("0.055")) {
				    	taxId = p.getProperty("ecomsupplytax550");
				    	taxRate = 5.5;
				    } else if(orderItem.getShipTax().equals("0.145")) {
				    	taxId = p.getProperty("ecomsupplytax1450");
				    	taxRate = 14.5;
				    }
				    double unitWoTax = (Double.parseDouble(orderItem.getItemCost()) * 100) / (100 + taxRate);
				    unitWoTax = Double.parseDouble(df.format(unitWoTax));
//				    Double lineNet = poNetTotal += Math.round(Double.parseDouble(orderItem.getItemCost()) * Double.parseDouble(orderItem.getQuantity()));
					final JSONObject jobi = new JSONObject("{\"_entityName\":\"OrderLine\",\"client\":\""+p.getProperty("ecomsupplyClient")+"\",\"client._identifier\":\"\",\"organization\":\""+p.getProperty("ecomsupplyOrg")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"updatedBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"active\":\"true\",\"salesOrder\":\""+poHeaderId+"\",\"salesOrder._identifier\":\"\",\"lineNo\":"+lineNumPO+",\"businessPartner\":\""+p.getProperty("ecomsupplybusinessPartner")+"\",\"partnerAddress\":\""+p.getProperty("ecomsupplyinvoiceAddress")+"\",\"orderDate\":\""+new Timestamp(new Date().getTime())+"\",\"product\":\""+orderItem.getProductId()+"\",\"product._identifier\":\"\",\"warehouse\":\""+p.getProperty("ecomsupplywarehouse")+"\",\"warehouse._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"orderedQuantity\":"+orderItem.getQuantity()+",\"reservedQuantity\":0,\"reservationStatus\":\"NR\",\"manageReservation\":\"\",\"managePrereservation\":\"\",\"currency\":\""+p.getProperty("ecomcurrency")+"\",\"currency._identifier\":\"\",\"unitPrice\":"+unitWoTax+",\"attributeSetValue\":null,\"tax\":\""+taxId+"\",\"tax._identifier\":\"\",\"dSLotqty\":0,\"dSLotprice\":0,\"dSBoxqty\":0,\"dSBoxprice\":0,\"dSEMDSUnitQty\":"+orderItem.getQuantity()+",\"dsLinenetamt\":"+(unitWoTax * Double.parseDouble(orderItem.getQuantity()))+",\"chargeAmount\":\"\",\"lineNetAmount\":"+(unitWoTax * Double.parseDouble(orderItem.getQuantity()))+",\"FreightAmt\":\"\",\"grossUnitPrice\":"+df.format((Double.parseDouble(orderItem.getItemCost())))+",\"PriceStd\":"+df.format((Double.parseDouble(orderItem.getItemCost())))+",\"priceList\":\""+p.getProperty("ecomstorepriceList")+"\",\"lineGrossAmount\":"+df.format(Double.parseDouble(orderItem.getItemCost()) * Double.parseDouble(orderItem.getQuantity()))+"}");
					final BaseOBObject bobi = toBaseOBObject.toBaseOBObject(jobi);
					oLines.add(bobi);
					// Database Connections and Database insertions 
					OBDal.getInstance().save(bobi);
					OBDal.getInstance().flush();
				}
				OBDal.getInstance().commitAndClose();
				// Calling Stored Procedure  ecom_order_post3
				final List parameters = new ArrayList();
			    parameters.add(null);
			    parameters.add(poHeaderId);
			    final String procedureName = "c_order_post1";
			   /* if((CallStoredProcedure.getInstance().call(procedureName, parameters, null)).equals("1")) {
			    	log.info("Procedure called successfully for the PO "+poHeaderId);
			    }*/
			    /*try {
			    final SendPOProcess sp = new SendPOProcess();
			    final DataToJsonConverter toJsonObject = OBProvider.getInstance().get(DataToJsonConverter.class);
			    final JSONObject poHeader = toJsonObject.toJsonObject(OBDal.getInstance().get(Order.class, poHeaderId), DataResolvingMode.FULL);
			    final List<JSONObject> poLines = toJsonObject.toJsonObjects(oLines);
			    final JSONArray jsonpoLines = new JSONArray(poLines);
log.info("Header = "+poHeader);			    
log.info("Lines = "+jsonpoLines);
				final StringBuilder jsonInput = new StringBuilder();
				jsonInput.append("{\"data\":[{\"Header\":");
				jsonInput.append(poHeader.toString()+",\"Lines\":"+jsonpoLines.toString());
				jsonInput.append("}]}");
log.info(jsonInput.toString());			    
			    JSONObject docNos = sp.send(jsonInput.toString());
			    log.info("response from supply" + docNos);
			    } catch (Exception e) {
				e.printStackTrace();
				}*/
			    ImmediateSOonPO soOnPO = new ImmediateSOonPO();
			    ArrayList<String> pOids = new ArrayList<String>();
			    Order orderHeader = OBDal.getInstance().get(Order.class, poHeaderId);
			    if (orderHeader != null) {
			    	if (orderHeader.getOrderLineList().size() > 0) {
			    		processOrder(soOnPO, pOids, orderHeader);
			    	}
			    }
					for (Map.Entry<String, OrdersDataDTO> entry : prodMap.entrySet()) {
						
						final OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class, "as ol where ol.salesOrder.id='"+poHeaderId+"' and ol.product='"+entry.getValue().getProductId()+"'");
						orderLine.setFilterOnActive(false);
						BigDecimal ordQty = BigDecimal.ZERO;
						BigDecimal cnfmQty = BigDecimal.ZERO;
						
						for (OrderLine ol : orderLine.list()) {
							
							ordQty = BigDecimal.ZERO;
							ordQty = ordQty.add(ol.getOrderedQuantity());
							cnfmQty = cnfmQty.add(new BigDecimal(ol.getSwConfirmedqty()));
							
						}
						log.info("product "+entry.getKey());
						log.info("or "+ordQty);
						log.info("cq "+cnfmQty);						
						int res = ordQty.compareTo(cnfmQty);
						if(res == 0) {
		    				final OrdersDataDTO orderData = prodMap.get(entry.getValue().getProductId());
		    				orderData.setConfirmedQty(cnfmQty.toString());
		    				log.info("Ordr data "+orderData);	    				
							log.info("soapConnection.call(setStatus6(vendorID, userID, pwd, "+order.getOrderDataId()+", "+orderData.getOrderItemId()+", "+cnfmQty+", '6'), url");
		    				soapConnection.call(setStatus6(vendorID, userID, pwd, order.getOrderDataId(), orderData.getOrderItemId(), cnfmQty, "6"), p.getProperty("serverURL")+"orderservice");
		    			} else if(res == 1) {
		    				final OrdersDataDTO orderData = prodMap.get(entry.getValue().getProductId());
		    				orderData.setConfirmedQty(cnfmQty.toString());
		    				soapConnection.call(setStatus6(vendorID, userID, pwd, order.getOrderDataId(), orderData.getOrderItemId(), cnfmQty, "6"), p.getProperty("serverURL")+"orderservice");
		    				soapConnection.call(setStatus6(vendorID, userID, pwd, order.getOrderDataId(), orderData.getOrderItemId(), ordQty.subtract(cnfmQty), "7"), p.getProperty("serverURL")+"orderservice");
		    			}
					}
			    final OBQuery<Order> obQuery = OBDal.getInstance().createQuery(Order.class,	"as p where p.id='"+poHeaderId+"'");
				final Order prod = obQuery.list().get(0);
				
				
				/***************************** SO Header Creation ***********************/
			    /*JSONObject job2 = new JSONObject("{\"_entityName\":\"Order\",\"client\":\""+p.getProperty("ecomstoreClient")+"\",\"client._identifier\":\"\",\"organization\":\""+p.getProperty("ecomstoreOrg")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"updatedBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"salesTransaction\":\"true\",\"documentNo\":\"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""+p.getProperty("ecomstoredocumentType")+"\",\"documentType._identifier\":\"\",\"transactionDocument\":\""+p.getProperty("ecomstoredocumentType")+"\",\"transactionDocument._identifier\":\"\",\"description\":\""+prod.getDocumentNo()+"\",\"orderDate\":\""+order.getOrderDate()+"\",\"scheduledDeliveryDate\":\"\",\"accountingDate\":\""+order.getOrderDate()+"\",\"businessPartner\":\""+bPartnerSO+"\",\"businessPartner._identifier\":\"\",\"invoiceAddress\":\""+p.getProperty("ecomstoreinvoiceAddress")+"\",\"invoiceAddress._identifier\":\"\",\"partnerAddress\":\""+p.getProperty("ecomstoreinvoiceAddress")+"\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\""+p.getProperty("ecomcurrency")+"\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\""+p.getProperty("ecomstorepaymentTerms")+"\",\"summedLineAmount\":\"\",\"paymentTerms._identifier\":\"\",\"invoiceTerms\":\"D\",\"deliveryTerms\":\"A\",\"freightCostRule\":\"I\",\"deliveryMethod\":\"P\",\"priority\":\"5\",\"warehouse\":\""+p.getProperty("ecomstorewarehouse")+"\",\"warehouse._identifier\":\"\",\"priceList\":\""+p.getProperty("ecomstorepriceList")+"\",\"priceList._identifier\":\"\",\"userContact\":\"\",\"paymentMethod\":\""+p.getProperty("ecomstorepaymentMethod")+"\",\"paymentMethod._identifier\":\"\",\"dSTotalpriceadj\":\"\",\"dSPosno\":\"\",\"dSEMDsRatesatisfaction\":\""+order.getCustSatisfaction()+"\",\"dSChargeAmt\":\"\",\"generateTemplate\":\"\",\"copyFromPO\":\"\",\"pickFromShipment\":\"\",\"receiveMaterials\":\"\",\"createInvoice\":\"\",\"addOrphanLine\":\"\",\"calculatePromotions\":\"\",\"quotation\":\"\",\"reservationStatus\":\"NR\",\"dSReceiptno\":\""+order.getOrderId()+"\",\"swSendorder\":\"\",\"swModorder\":\"\",\"swDelorder\":\"\",\"obwplGeneratepicking\":\"\",\"sWEMSwPostatus\":\"\",\"rCOxylaneno\":\""+order.getInvoiceCustomer()+"\",\"grandTotalAmount\":"+order.getLinesAmount()+"}");
				// Converting JSON Object to Openbravo Business object 
				BaseOBObject bob2 = toBaseOBObject.toBaseOBObject(job2);
				// Database Connections and Database insertions 
				OBDal.getInstance().save(bob2);
				OBDal.getInstance().flush();
				final String soHeaderID = (String) bob2.getId();
				OBDal.getInstance().commitAndClose();
				int lineNumSO = 0;
				for (Map.Entry<String, OrdersDataDTO> entry : prodMap.entrySet()) {
					lineNumSO++;
					final OrdersDataDTO orderDataInfo = entry.getValue();
					String taxId = p.getProperty("ecomstoretax550");
				    if(orderDataInfo.getShipTax().equals("0.055")) {
				    	taxId = p.getProperty("ecomstoretax550");
				    } else if(orderDataInfo.getShipTax().equals("0.145")) {
				    	taxId = p.getProperty("ecomstoretax1450");
				    }
				    Double lineAmount = Double.parseDouble(orderDataInfo.getItemCost()) * Double.parseDouble(orderDataInfo.getConfirmedQty());
				    
					JSONObject jobi2 = new JSONObject("{\"_entityName\":\"OrderLine\",\"client\":\""+p.getProperty("ecomstoreClient")+"\",\"client._identifier\":\"\",\"organization\":\""+p.getProperty("ecomstoreOrg")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"updatedBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"active\":\"true\",\"salesOrder\":\""+soHeaderID+"\",\"salesOrder._identifier\":\"\",\"lineNo\":"+lineNumSO+",\"businessPartner\":\""+bPartnerSO+"\",\"partnerAddress\":\""+p.getProperty("ecomstoreinvoiceAddress")+"\",\"orderDate\":\""+order.getOrderDate()+"\",\"product\":\""+orderDataInfo.getProductId()+"\",\"product._identifier\":\"\",\"warehouse\":\""+p.getProperty("ecomstorewarehouse")+"\",\"warehouse._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"orderedQuantity\":"+orderDataInfo.getConfirmedQty()+",\"reservedQuantity\":0,\"reservationStatus\":\"NR\",\"manageReservation\":\"\",\"managePrereservation\":\"\",\"currency\":\""+p.getProperty("ecomcurrency")+"\",\"currency._identifier\":\"\",\"unitPrice\":"+Double.parseDouble(orderDataInfo.getItemCost())+",\"attributeSetValue\":null,\"tax\":\""+taxId+"\",\"tax._identifier\":\"\",\"dSLotqty\":\"\",\"dSLotprice\":\"\",\"dSBoxqty\":\"\",\"dSBoxprice\":\"\",\"dSEMDSUnitQty\":"+orderDataInfo.getConfirmedQty()+",\"dsLinenetamt\":"+lineAmount+",\"chargeAmount\":"+Double.parseDouble(orderDataInfo.getShippedTotal())+",\"lineNetAmount\":"+lineAmount+",\"FreightAmt\":"+orderDataInfo.getShippedTotal()+",\"grossUnitPrice\":"+orderDataInfo.getItemCost()+",\"PriceStd\":"+orderDataInfo.getItemCost()+",\"priceList\":\""+p.getProperty("ecomstorepriceList")+"\"}");
					BaseOBObject bobi2 = toBaseOBObject.toBaseOBObject(jobi2);
					OBDal.getInstance().save(bobi2);
					OBDal.getInstance().flush();
				}
				OBDal.getInstance().commitAndClose();
				final String procedureName1 = "ecom_dc_post";
				final List parameterSO = new ArrayList();
				parameterSO.add(soHeaderID);
			    CallStoredProcedure.getInstance().call(procedureName1, parameterSO, null);*/
			    
			    
			    
			    
			    
			    /***************************** Sales Invoice Creation ***********************/
				Double chargeAmtSI = 0.0;
				for (Map.Entry<String, OrdersDataDTO> entry : prodMap.entrySet()) {
					final OrdersDataDTO orderDataInfo = entry.getValue();
					chargeAmtSI = chargeAmtSI + Double.parseDouble(orderDataInfo.getShippedTotal());
				}
				Double invoiceGrandTotal=0.0;
				Double invoiceNetTotal=0.0;
				Double totalorderedQty=0.0;
				Double totalconfirmedQty=0.0;
				for (Map.Entry<String, OrdersDataDTO> entry : prodMap.entrySet()) {
					final OrdersDataDTO orderDataInfo = entry.getValue();
					String taxId = p.getProperty("ecomstoretax550");
					double taxRate = 0.0;
				    if(orderDataInfo.getShipTax().equals("0.055")) {
				    	taxId = p.getProperty("ecomstoretax550");
				    	taxRate = 5.5;
				    } else if(orderDataInfo.getShipTax().equals("0.145")) {
				    	taxId = p.getProperty("ecomstoretax1450");
				    	taxRate = 14.5;
				    }
					if(Double.parseDouble(orderDataInfo.getConfirmedQty()) != 0.0 || Double.parseDouble(orderDataInfo.getConfirmedQty()) != 0 || orderDataInfo.getConfirmedQty().equals("0")) {
						totalorderedQty+=Double.parseDouble(orderDataInfo.getQuantity());
						totalconfirmedQty+=Double.parseDouble(orderDataInfo.getConfirmedQty());
						double unitWoTax = (Double.parseDouble(orderDataInfo.getItemCost()) * 100) / (100 + taxRate);
						invoiceGrandTotal += (Double.parseDouble(orderDataInfo.getItemCost()) * Double.parseDouble(orderDataInfo.getConfirmedQty()));
						invoiceNetTotal += Double.parseDouble(df.format(unitWoTax * Double.parseDouble(orderDataInfo.getConfirmedQty())));
					}
				}
				data.add(new String[] {order.getOrderId(), "ERP Order Doc Num 1", "ERP Order Doc Num 2", totalorderedQty.toString(), totalconfirmedQty.toString(), new Timestamp(new Date().getTime()).toString()});
				writer.writeAll(data);
			    JSONObject job2 = new JSONObject("{\"_entityName\":\"Invoice\",\"client\":\""+p.getProperty("ecomstoreClient")+"\",\"client._identifier\":\"\",\"organization\":\""+p.getProperty("ecomstoreOrg")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"updatedBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"salesTransaction\":\"true\",\"documentNo\":\"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""+p.getProperty("ecomstoredocumentType")+"\",\"documentType._identifier\":\"\",\"transactionDocument\":\""+p.getProperty("ecomstoredocumentType")+"\",\"transactionDocument._identifier\":\"\",\"description\":\""+order.getOrderId()+"\",\"orderDate\":\""+new Timestamp(new Date().getTime())+"\",\"accountingDate\":\""+new Timestamp(new Date().getTime())+"\",\"invoiceDate\":\""+new Timestamp(new Date().getTime())+"\",\"businessPartner\":\""+bPartnerSO+"\",\"businessPartner._identifier\":\"\",\"partnerAddress\":\""+p.getProperty("ecomstoreinvoiceAddress")+"\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\""+p.getProperty("ecomcurrency")+"\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\""+p.getProperty("ecomstorepaymentTerms")+"\",\"summedLineAmount\":\"\",\"priceList\":\""+p.getProperty("ecomstorepriceList")+"\",\"priceList._identifier\":\"\",\"userContact\":\"\",\"paymentMethod\":\""+p.getProperty("ecomstorepaymentMethod")+"\",\"paymentMethod._identifier\":\"\",\"calculatePromotions\":\"\",\"chargeAmount\":"+chargeAmtSI+",\"salesRepresentative\":\""+OBContext.getOBContext().getUser().getId()+"\",\"grandTotalAmount\":\"\",\"ecomOrderReceiptno\":\""+order.getOrderId()+"\"}");
				// Converting JSON Object to Openbravo Business object 
				BaseOBObject bob2 = toBaseOBObject.toBaseOBObject(job2);
				// Database Connections and Database insertions 
				OBDal.getInstance().save(bob2);
				OBDal.getInstance().flush();
				final String siHeaderID = (String) bob2.getId();
				OBDal.getInstance().commitAndClose();
				int lineNumSO = 0;
				for (Map.Entry<String, OrdersDataDTO> entry : prodMap.entrySet()) {
					lineNumSO++;
					final OrdersDataDTO orderDataInfo = entry.getValue();
					String taxId = p.getProperty("ecomstoretax550");
					double taxRate = 0.0;
				    if(orderDataInfo.getShipTax().equals("0.055")) {
				    	taxId = p.getProperty("ecomstoretax550");
				    	taxRate = 5.5;
				    } else if(orderDataInfo.getShipTax().equals("0.145")) {
				    	taxId = p.getProperty("ecomstoretax1450");
				    	taxRate = 14.5;
				    }
				    try {
					    if(Double.parseDouble(orderDataInfo.getConfirmedQty()) != 0.0 || Double.parseDouble(orderDataInfo.getConfirmedQty()) != 0 || orderDataInfo.getConfirmedQty().equals("0")) {
						    double unitWoTax = Double.parseDouble(df.format((Double.parseDouble(orderDataInfo.getItemCost()) * 100) / (100 + taxRate)));
						    Double lineAmount = Double.parseDouble(df.format(unitWoTax * Double.parseDouble(orderDataInfo.getConfirmedQty())));
						    double taxAmount = Double.parseDouble(orderDataInfo.getTaxAmount()) * Double.parseDouble(orderDataInfo.getConfirmedQty());
							JSONObject jobi2 = new JSONObject("{\"_entityName\":\"InvoiceLine\",\"client\":\""+p.getProperty("ecomstoreClient")+"\",\"client._identifier\":\"\",\"organization\":\""+p.getProperty("ecomstoreOrg")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"updatedBy\":\""+OBContext.getOBContext().getUser().getId()+"\",\"active\":\"true\",\"invoice\":\""+siHeaderID+"\",\"salesOrder._identifier\":\"\",\"lineNo\":"+lineNumSO+",\"product\":\""+orderDataInfo.getProductId()+"\",\"product._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"invoicedQuantity\":"+orderDataInfo.getConfirmedQty()+",\"unitPrice\":"+unitWoTax+",\"attributeSetValue\":null,\"tax\":\""+taxId+"\",\"tax._identifier\":\"\",\"chargeAmount\":"+Double.parseDouble(orderDataInfo.getShippedTotal())+",\"lineNetAmount\":"+lineAmount+",\"standardPrice\":"+unitWoTax+",\"listPrice\":\""+p.getProperty("ecomstorepriceList")+"\",\"taxAmount\":"+taxAmount+",\"grossAmount\":"+df.format(Double.parseDouble(orderDataInfo.getItemCost()) * Double.parseDouble(orderDataInfo.getConfirmedQty()))+",\"grossUnitPrice\":"+df.format(Double.parseDouble(orderDataInfo.getItemCost()))+"}");
							BaseOBObject bobi2 = toBaseOBObject.toBaseOBObject(jobi2);
							OBDal.getInstance().save(bobi2);
							OBDal.getInstance().flush();
					    }
				    } catch(Exception e1) {
				    	e1.printStackTrace();
				    }
				}
				OBDal.getInstance().commitAndClose();
//				final String procedureName1 = "ecom_dc_post";
//				final List parameterSO = new ArrayList();
//				parameterSO.add(soHeaderID);
//			    CallStoredProcedure.getInstance().call(procedureName1, parameterSO, null);
				processed++;
			}
			writer.close();
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', ordersDid:'"+processed+"' }";
			response.setContentType("application/json");
			// Get the printwriter object from response to write the required json object to the output stream      
			PrintWriter out = response.getWriter();
			// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
			out.print(objectToReturn);
			out.flush();
		} catch (Exception e) {
            e.printStackTrace();
        } finally {
        	soapConnection.close();
        }
	}
	
	private static void processOrder(ImmediateSOonPO soOnPO,
		      ArrayList<String> pOids, Order orderHeader) throws Exception {
		    log.info(",, " + SOConstants.performanceTest + " before process ,," + new Date());
		    soOnPO.processRequest(orderHeader);
		    log.info(",, " + SOConstants.performanceTest + " after process ,," + new Date());
		    pOids.add(orderHeader.getId());
		  }
	
	private SOAPMessage findOrderProperties1(String vendorID, String userID,
			String pwd, String nodeValue) throws Exception {
		// TODO Auto-generated method stub
		
		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
		final SOAPEnvelope envelope = soapPart.getEnvelope();
        // add namespace
        envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
        final SOAPHeader header = envelope.getHeader();
        try {
        	// SOAP Headers
        	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
        	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
        	el.setValue(vendorID);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findOrderProperties1", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(nodeValue);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        return soapMessage;
	}

	private String getNodeValue(String tagName, NodeList nodes ) {
	    for ( int x = 0; x < nodes.getLength(); x++ ) {
	        Node node = (Node) nodes.item(x);
	        if (node.getNodeName().equalsIgnoreCase(tagName)) {
	            NodeList childNodes = node.getChildNodes();
	            for (int y = 0; y < childNodes.getLength(); y++ ) {
	                Node data = (Node) childNodes.item(y);
	                if ( data.getNodeType() == Node.TEXT_NODE )
	                    return data.getNodeValue();
	            }
	        }
	    }
	    return "";
	}

	private SOAPMessage createfindByItem(String vId, String uID, String pwd) throws Exception {
		// TODO Auto-generated method stub
		
		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
		final  SOAPEnvelope envelope = soapPart.getEnvelope();
        // add namespace
        envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
        final  SOAPHeader header = envelope.getHeader();
        try {
        	// SOAP Headers
        	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
        	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
        	el.setValue(vId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(uID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findByItemsState", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode("1");
        	SOAPElement soapBodyElem3 = soapBodyElem.addChildElement("in1", "web");
        	soapBodyElem3.addTextNode("3");
        	SOAPElement soapBodyElem4 = soapBodyElem.addChildElement("in2", "web");
        	soapBodyElem4.addTextNode("0");
        	SOAPElement soapBodyElem5 = soapBodyElem.addChildElement("in3", "web");
        	soapBodyElem5.addTextNode("24");
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        return soapMessage;
	}
	
	private SOAPMessage findOrderItems(String vId, String uID, String pwd, String orderId) throws Exception {
		// TODO Auto-generated method stub
		
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        // add namespace
        envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
        SOAPHeader header = envelope.getHeader();
        try {
        	// SOAP Headers
        	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
        	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
        	el.setValue(vId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(uID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findOrderItems", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(orderId);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
		return soapMessage;
	}
	
	private SOAPMessage setStatus6(String vId, String uID, String pwd, String orderDataId, String orderItemId, BigDecimal qty, String status) throws Exception {
		// TODO Auto-generated method stub
		
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        // add namespace
        envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
        SOAPHeader header = envelope.getHeader();
        try {
        	// SOAP Headers
        	SOAPElement el = header.addHeaderElement(envelope.createName("VendorToken", "", "urn:commerce:vendor"));
        	el = el.addChildElement(envelope.createName("VendorId", "", "urn:commerce:vendor"));
        	el.setValue(vId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(uID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("setStatus6", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(orderDataId);
        	SOAPElement soapBodyElem3 = soapBodyElem.addChildElement("in1", "web");
        	SOAPElement soapBodyElem4 = soapBodyElem3.addChildElement("long", "web");
        	soapBodyElem4.addTextNode(orderItemId);
        	SOAPElement soapBodyElem5 = soapBodyElem.addChildElement("in2", "web");
        	soapBodyElem5.addTextNode(status);
        	SOAPElement soapBodyElem6 = soapBodyElem.addChildElement("in3", "web");
        	SOAPElement soapBodyElem7 = soapBodyElem6.addChildElement("entry", "web");
        	SOAPElement soapBodyElem8 = soapBodyElem7.addChildElement("key", "web");
        	soapBodyElem8.addTextNode(orderItemId);
        	SOAPElement soapBodyElem9 = soapBodyElem7.addChildElement("value", "web");
        	soapBodyElem9.addTextNode(qty.toString());
        	SOAPElement soapBodyElem10 = soapBodyElem.addChildElement("in4", "web");
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
		return soapMessage;
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

}
