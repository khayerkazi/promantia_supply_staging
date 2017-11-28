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
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVWriter;

public class TransactionDataReport implements WebService {

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
		
		// Response specification
		response.setCharacterEncoding("utf-8");
	    response.setHeader("Content-Encoding", "UTF-8");
	    final String fileName = "TransactionReport.csv";
	    response.setHeader("Content-disposition", "inline;filename="+fileName+"");
	    response.setContentType("text/csv");
	    response.setHeader("Content-Type", "text/csv");
	    
		final Writer w = response.getWriter();
		CSVWriter writer = new CSVWriter(w);
		List<String[]> data = new ArrayList<String[]>();
		
		data.add(new String[] {"Decathlon ID", "Order ID", "Customer Name", "Company Name", "Invoice Number", "Date of Invoice", "Timestamp", "Total Turnover", "Delivery Charge", "Total Transaction", "Payment Amount", "Date of Payment", "Type of Payment", "Transaction Number"});
		
		try {
			// sending Request to Order Service findByItemStatus method
			final SOAPMessage find2OrderItemsResponse = soapConnection.call(find2OrderItems(vendorId, userID, pwd, fromDate, toDate), p.getProperty("serverURL")+"orderservice");
			final SOAPBody orderItemsResponseBody = find2OrderItemsResponse.getSOAPBody();
			final NodeList returnList1 = orderItemsResponseBody.getElementsByTagName("ns1:OrderData");
		    for (int k = 0; k < returnList1.getLength(); k++) {
		    	
		    	// sending Request to Order Service findOrderPayments1 method
		    	final SOAPMessage findOrderPayments1Response = soapConnection.call(findOrderPayments1(vendorId, userID, pwd, returnList1.item(k).getChildNodes().item(13).getTextContent()), p.getProperty("serverURL")+"orderservice");
		    	final SOAPBody findOrderPayments1ResponseBody = findOrderPayments1Response.getSOAPBody();
		    	final NodeList returnList2 = findOrderPayments1ResponseBody.getElementsByTagName("ns2:OrderPayment");
		    	
		    	// sending Request to Order Service findByItemStatus method
				final SOAPMessage findOrderItemsResponse = soapConnection.call(findOrderItems(vendorId, userID, pwd, returnList1.item(k).getChildNodes().item(11).getTextContent()), p.getProperty("serverURL")+"orderservice");
				final SOAPBody itemsResponseBody = findOrderItemsResponse.getSOAPBody();
				final NodeList returnList = itemsResponseBody.getElementsByTagName("ns2:OrderItem");
				
				String name = "N/A";
				if(returnList.item(0) != null) {
					Integer qty = 0;
					Double total = 0.0;
					Double taxTotal = 0.0;
					Double shipTotal = 0.0;
					Double payTotal = 0.0;
					Double totalTurnover = 0.0;
					String companyName = "N?A";
					for(int i = 0; i < returnList.getLength(); i++) {
						
						Double lineTotal = (Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes())) * Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes())));
						qty = qty + Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes()));
						total = total + lineTotal;
						taxTotal = taxTotal + (lineTotal * Double.parseDouble(getNodeValue("taxrateGST", returnList.item(i).getChildNodes())));
						shipTotal = shipTotal + Double.parseDouble(getNodeValue("shipcost", returnList.item(i).getChildNodes()));
						payTotal = payTotal + Double.parseDouble(getNodeValue("paytot", returnList.item(i).getChildNodes()));
						// fetching company name from customer service
						final SOAPMessage customerServiceFindByEmailResponse = soapConnection.call(customerServiceFindByEmail(vendorId, userID, pwd, getNodeValue("loginname", returnList1.item(k).getChildNodes())), p.getProperty("serverURL")+"customerservice");
				    	final SOAPBody customerServiceFindByEmailResponseBody = customerServiceFindByEmailResponse.getSOAPBody();
				    	final NodeList customerList = customerServiceFindByEmailResponseBody.getElementsByTagName("Address");
				    	for(int j = 0; j < customerList.getLength();j++) {
				    		if(getNodeValue("nickname", customerList.item(j).getChildNodes()).equals("Billing Address")) {
				    			companyName = getNodeValue("company", customerList.item(j).getChildNodes());
				    		}
				    	}
					}
					totalTurnover = payTotal - shipTotal;
					String invoiceDate = "";
					if(returnList.item(0).getChildNodes().item(14) != null) {
						if(!getNodeValue("ship1date", returnList.item(0).getChildNodes().item(14).getChildNodes()).replace("T", " ").equals("")) {
							invoiceDate = getNodeValue("ship1date", returnList.item(0).getChildNodes().item(14).getChildNodes()).replace("T", " ").substring(0, 19);
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
						pst = conn.prepareStatement("select * from c_invoice where em_ecom_order_receiptno='"+getNodeValue("orderid", returnList1.item(k).getChildNodes())+"'");
						rs = pst.executeQuery();
						while (rs.next()) {
							invNo = rs.getString("documentno");
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					} finally {
						conn.close();
						pst.close();
						rs.close();
					}
					if(returnList.item(0).getChildNodes().item(46) != null) {
						name = getNodeValue("firstname", returnList.item(0).getChildNodes().item(46).getChildNodes())+" "+getNodeValue("lastname", returnList.item(0).getChildNodes().item(46).getChildNodes());
					}
System.out.println(getNodeValue("transactionid", returnList2.item(0).getChildNodes()));				
					data.add(new String[] {getNodeValue("invoicecustomer", returnList1.item(k).getChildNodes()), getNodeValue("orderid", returnList1.item(k).getChildNodes()), name, companyName, invNo, invoiceDate, getNodeValue("createtime", returnList1.item(k).getChildNodes()).replace("T", " ").substring(0, 19), totalTurnover.toString(), shipTotal.toString(), payTotal.toString(), getNodeValue("paytot", returnList2.item(0).getChildNodes()), getNodeValue("createtime", returnList1.item(k).getChildNodes()).replace("T", " ").substring(0, 19), getNodeValue("paytype", returnList2.item(0).getChildNodes()), "T"+getNodeValue("transactionid", returnList2.item(0).getChildNodes())});
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
	
	private SOAPMessage findOrderPayments1(String vendorId, String userID, String pwd, String textContent) throws Exception {
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
