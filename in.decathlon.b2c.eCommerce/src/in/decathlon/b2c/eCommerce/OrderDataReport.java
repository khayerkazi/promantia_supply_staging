package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;
import in.decathlon.integration.PassiveDB;

import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

import com.sysfore.sankalpcrm.RCCompany;

import au.com.bytecode.opencsv.CSVWriter;

public class OrderDataReport implements WebService {

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		final String vendorId = request.getParameter("vid");
		final String userID = request.getParameter("ecomID");
		final String pwd = request.getParameter("ecomP");
		final String fromDate = request.getParameter("fromdate");
		final String toDate = request.getParameter("todate");
		
		// SOAP Connection Initialisation
		final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		final SOAPConnection soapConnection = soapConnectionFactory.createConnection();
		
		Properties p = ECommerceUtil.getInstance().getProperties();
		OBDal obDal = OBDal.getInstance();
		// Response specification
		response.setCharacterEncoding("utf-8");
	    response.setHeader("Content-Encoding", "UTF-8");
	    final String fileName = "OrderReport.csv";
	    response.setHeader("Content-disposition", "inline;filename="+fileName+"");
	    response.setContentType("text/csv");
	    response.setHeader("Content-Type", "text/csv");
	    
		final Writer w = response.getWriter();
		CSVWriter writer = new CSVWriter(w);
		
		// Headings
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] {"Decathlon ID", "Order Id", "Invoice Number", "Create Time", "Invoice Date/Time", "Dispatched Date", "Delivery date", "Mode of delivery", "Way Bill Number", "Status", "Customer Name", "Company Name", "TelephoneNumber", "CustomerEmail", "ShippingAddress", "ShippingCity", "ShippingPincode", "ShippingName", "Volume", "Ordered Quantity", "Total", "Tax Total", "Ship Total", "Pay Total", "Payment Method", "Amount Paid", "Amount Paid Via Gift Certs", "Gift Cert Codes", "Refund Total"});
		
		try {
			// sending a Request to Order Service findByItemStatus method
			final SOAPMessage find2OrderItemsResponse = soapConnection.call(find2OrderItems(vendorId, userID, pwd, fromDate, toDate), p.getProperty("serverURL")+"orderservice");
			final SOAPBody orderItemsResponseBody = find2OrderItemsResponse.getSOAPBody();
			final NodeList returnList1 = orderItemsResponseBody.getElementsByTagName("ns1:OrderData");
		    for (int k = 0; k < returnList1.getLength(); k++) {
		    	/// sending Request to Order Service findOrderPayments1 method
		    	final SOAPMessage findOrderPayments1Response = soapConnection.call(findOrderPayments1(vendorId, userID, pwd, returnList1.item(k).getChildNodes().item(13).getTextContent()), p.getProperty("serverURL")+"orderservice");
		    	final SOAPBody findOrderPayments1ResponseBody = findOrderPayments1Response.getSOAPBody();
		    	final NodeList returnList2 = findOrderPayments1ResponseBody.getElementsByTagName("ns2:OrderPayment");
		    	/// sending Request to Order Service findByItemStatus method
				final SOAPMessage findOrderItemsResponse = soapConnection.call(findOrderItems(vendorId, userID, pwd, returnList1.item(k).getChildNodes().item(11).getTextContent()), p.getProperty("serverURL")+"orderservice");
				final SOAPBody itemsResponseBody = findOrderItemsResponse.getSOAPBody();
				final NodeList returnList = itemsResponseBody.getElementsByTagName("ns2:OrderItem");
				String status = "";
				String name = "N/A";
				String phone = "N/A";
				String addr = "N/A";
				String city = "N/A";
				String pin = "N/A";
				if(returnList.item(0) != null) {
					if(returnList.item(0).getChildNodes().item(46) != null) {
						name = getNodeValue("firstname", returnList.item(0).getChildNodes().item(46).getChildNodes())+" "+getNodeValue("lastname", returnList.item(0).getChildNodes().item(46).getChildNodes());
						phone = getNodeValue("phone", returnList.item(0).getChildNodes().item(46).getChildNodes());
						addr = getNodeValue("address1", returnList.item(0).getChildNodes().item(46).getChildNodes())+", "+getNodeValue("city", returnList.item(0).getChildNodes().item(46).getChildNodes())+", "+getNodeValue("name", returnList.item(0).getChildNodes().item(46).getChildNodes().item(10).getChildNodes());
						city = getNodeValue("city", returnList.item(0).getChildNodes().item(46).getChildNodes());
						pin = getNodeValue("postal", returnList.item(0).getChildNodes().item(46).getChildNodes());
					}
					if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("1")) {
						status = "In Progress";
					} else if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("2")) {
						status = "Complete";
					} else if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("3")) {
						status = "Declined";
					} else if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("4")) {
						status = "Payment Failed";
					} else if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("5")) {
						status = "Wait Response";
					} else if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("6")) {
						status = "Processed";
					} else if(getNodeValue("orderstate", returnList1.item(k).getChildNodes()).equals("7")) {
						status = "On Hold";
					}
					String invoiceDate = "N/A";
					/*if(returnList.item(0).getChildNodes().item(14) != null) {
						if(!getNodeValue("ship1date", returnList.item(0).getChildNodes().item(14).getChildNodes()).replace("T", " ").equals("")) {
							invoiceDate = getNodeValue("ship1date", returnList.item(0).getChildNodes().item(14).getChildNodes()).replace("T", " ").substring(0, 19);
						}
					}*/
					Double volume = 0.0;
					Integer qty = 0;
					Double total = 0.0;
					Double taxTotal = 0.0;
					Double shipTotal = 0.0;
					Double payTotal = 0.0;
					String companyName = "N/A";
					for(int i = 0; i < returnList.getLength(); i++) {
						
						qty = qty + Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes()));
						shipTotal = shipTotal + Double.parseDouble(getNodeValue("shipcost", returnList.item(i).getChildNodes()));
						Double taxRate = Double.parseDouble(getNodeValue("taxrateGST", returnList.item(i).getChildNodes()));
						payTotal = payTotal + Double.parseDouble(getNodeValue("paytot", returnList.item(i).getChildNodes()));
						
						Double total1 = Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes())) * Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes()));
						total = total + total1;
						Double total2 = Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes())) * ((Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes()))) - ((Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes()))/(100 + (100 * taxRate))) * 100));
						taxTotal = taxTotal + total2;
						// fetching company name from customer service
						/*final SOAPMessage customerServiceFindByEmailResponse = soapConnection.call(customerServiceFindByEmail(vendorId, userID, pwd, getNodeValue("loginname", returnList1.item(k).getChildNodes())), p.getProperty("serverURL")+"customerservice");
				    	final SOAPBody customerServiceFindByEmailResponseBody = customerServiceFindByEmailResponse.getSOAPBody();
				    	final NodeList customerList = customerServiceFindByEmailResponseBody.getElementsByTagName("Address");
				    	for(int j = 0; j < customerList.getLength();j++) {
				    		if(getNodeValue("nickname", customerList.item(j).getChildNodes()).equals("Billing Address")) {
				    			companyName = getNodeValue("company", customerList.item(j).getChildNodes());
				    		}
				    	}*/
						System.out.println(getNodeValue("loginname", returnList1.item(k).getChildNodes()));
						OBQuery<BusinessPartner> bpQuery = obDal.createQuery(BusinessPartner.class," as bp where bp.rCEmail='"+getNodeValue("loginname", returnList1.item(k).getChildNodes())+"'");
						if(bpQuery.list() != null && bpQuery.list().size() > 0){
							RCCompany rcCompany = bpQuery.list().get(0).getRCCompany();
							if(rcCompany != null){
								companyName = rcCompany.getCompanyName();
							}
							System.out.println(companyName);
						}
						
						// fetching Volume from item service
						final SOAPMessage itemServiceFind1Response = soapConnection.call(itemServiceFind1(vendorId, userID, pwd, getNodeValue("itemcode", returnList.item(i).getChildNodes())), p.getProperty("serverURL")+"itemservice");
				    	final SOAPBody itemServiceFind1ResponseBody = itemServiceFind1Response.getSOAPBody();
				    	final NodeList itemList = itemServiceFind1ResponseBody.getElementsByTagName("ns1:out");
						if(itemList.getLength() > 0 && !getNodeValue("length", itemList.item(0).getChildNodes()).equals("")) {
							volume = volume + (Double.parseDouble(getNodeValue("length", itemList.item(0).getChildNodes())) * Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes())));
						}
					}
					Connection conn = null;
					if(p.getProperty("usePassiveDB").equals("Y")) {
						conn = PassiveDB.getInstance().getConnection();
					} else {
						conn = OBDal.getInstance().getConnection();
					}
					PreparedStatement pst = null;
					ResultSet rs = null;
					String invNo = "N/A";
					try {
//						pst = conn.prepareStatement("select * from c_invoice where em_ecom_order_receiptno='"+getNodeValue("orderid", returnList1.item(k).getChildNodes())+"'");
						pst = conn.prepareStatement("select documentno,dateinvoiced from c_invoice where em_ecom_order_receiptno='"+getNodeValue("orderid", returnList1.item(k).getChildNodes())+"' and ad_org_id ='"+p.getProperty("ecomstoreOrg")+"'");
						rs = pst.executeQuery();
						while (rs.next()) {
							invNo = rs.getString("documentno");
							invoiceDate = rs.getString("dateinvoiced");
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					} finally {
						conn.close();
						pst.close();
						rs.close();
					}
					if (getNodeValue("shiptime", returnList.item(0).getChildNodes()).equals("")) {
						data.add(new String[] {getNodeValue("invoicecustomer", returnList1.item(k).getChildNodes()), getNodeValue("orderid", returnList1.item(k).getChildNodes()), invNo, getNodeValue("createtime", returnList1.item(k).getChildNodes()).replace("T", " ").substring(0, 19), invoiceDate, invoiceDate, "", getNodeValue("shipvia", returnList.item(0).getChildNodes()), getNodeValue("courierno", returnList.item(0).getChildNodes()), status, name, companyName, phone, getNodeValue("loginname", returnList1.item(k).getChildNodes()), addr, city, pin, name, volume.toString(), qty.toString() , total.toString(), taxTotal.toString(), shipTotal.toString(), payTotal.toString(), getNodeValue("paytype", returnList2.item(0).getChildNodes()), getNodeValue("paytot", returnList2.item(0).getChildNodes()), "", "", getNodeValue("refundtot", returnList2.item(0).getChildNodes())});
					} else {
						data.add(new String[] {getNodeValue("invoicecustomer", returnList1.item(k).getChildNodes()), getNodeValue("orderid", returnList1.item(k).getChildNodes()), invNo, getNodeValue("createtime", returnList1.item(k).getChildNodes()).replace("T", " ").substring(0, 19), invoiceDate, invoiceDate, getNodeValue("shiptime", returnList.item(0).getChildNodes()).replace("T", " ").substring(0, 19), getNodeValue("shipvia", returnList.item(0).getChildNodes()), getNodeValue("courierno", returnList.item(0).getChildNodes()), status, name, companyName, phone, getNodeValue("loginname", returnList1.item(k).getChildNodes()), addr, city, pin, name, volume.toString(), qty.toString() , total.toString(), taxTotal.toString(), shipTotal.toString(), payTotal.toString(), getNodeValue("paytype", returnList2.item(0).getChildNodes()), getNodeValue("paytot", returnList2.item(0).getChildNodes()), "", "", getNodeValue("refundtot", returnList2.item(0).getChildNodes())});
					}
				}
		    }
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			soapConnection.close();
			writer.writeAll(data);
			writer.close();
		}
	}
	
	private SOAPMessage customerServiceFindByEmail(String vendorId, String userID, String pwd,
			String nodeValue) throws Exception {
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
        	el.setValue(vendorId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findByEmail", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(nodeValue);
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
		return soapMessage;
	}

	private SOAPMessage itemServiceFind1(String vendorId, String userID, String pwd, String nodeValue) throws Exception {
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
        	el.setValue(vendorId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("find1", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(nodeValue);
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
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

	private SOAPMessage findOrderPayments1(String vendorId, String userID, String pwd, String textContent) throws Exception {
		
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
        	el.setValue(vendorId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findOrderPayments1", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(textContent);
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
		return soapMessage;
	}

	private SOAPMessage findOrderItems(String vendorId, String userID, String pwd, String textContent) throws Exception {
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
        	el.setValue(vendorId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findOrderItems", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(textContent);
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
		return soapMessage;
	}

	private SOAPMessage find2OrderItems(String vendorId, String userID, String pwd, String fromDate, String toDate) throws Exception {
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
        	el.setValue(vendorId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("find2", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(fromDate);
        	SOAPElement soapBodyElem3 = soapBodyElem.addChildElement("in1", "web");
        	soapBodyElem3.addTextNode(toDate);
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();
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
