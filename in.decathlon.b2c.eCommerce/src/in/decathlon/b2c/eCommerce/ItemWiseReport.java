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

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.info.InvoiceLine;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.dataimport.Order;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVWriter;

public class ItemWiseReport implements WebService {

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
	    final String fileName = "ItemWiseReport.csv";
	    response.setHeader("Content-disposition", "inline;filename="+fileName+"");
	    response.setContentType("text/csv");
	    response.setHeader("Content-Type", "text/csv");
	    
	    final Writer w = response.getWriter();
		CSVWriter writer = new CSVWriter(w);
		
		// Headings
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] {"Invoice Time", "Order Number", "Invoice Number", "Address", "Customer Name", "Company Name", "City", "Billing Postal Code", "Shipping Postal Code", "Item Code", "Qty", "VAT", "CST", "Tax Rate", "Cession Price", "Unit Price", "Tax per Item", "Total Turnover", "Shipping Cost", "Total Cost", "Margin Value", "Margin %"});
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
				
				for(int i = 0; i < returnList.getLength(); i++) {
					try {
					String name = "N/A";
					String addr = "N/A";
					String city = "N/A";
					String pin = "N/A";
					String companyName = "N/A";
					String billingPin = "N/A";
					// fetching company name from customer service
					final SOAPMessage customerServiceFindByEmailResponse = soapConnection.call(customerServiceFindByEmail(vendorId, userID, pwd, getNodeValue("loginname", returnList1.item(k).getChildNodes())), p.getProperty("serverURL")+"customerservice");
			    	final SOAPBody customerServiceFindByEmailResponseBody = customerServiceFindByEmailResponse.getSOAPBody();
			    	final NodeList customerList = customerServiceFindByEmailResponseBody.getElementsByTagName("Address");
			    	for(int j = 0; j < customerList.getLength();j++) {
			    		if(getNodeValue("nickname", customerList.item(j).getChildNodes()).equals("Billing Address")) {
			    			companyName = getNodeValue("company", customerList.item(j).getChildNodes());
			    			billingPin = getNodeValue("postal", customerList.item(j).getChildNodes());
			    		}
			    	}
					if(returnList.item(i).getChildNodes().item(46) != null) {
						name = getNodeValue("firstname", returnList.item(0).getChildNodes().item(46).getChildNodes())+" "+getNodeValue("lastname", returnList.item(0).getChildNodes().item(46).getChildNodes());
						addr = getNodeValue("address1", returnList.item(0).getChildNodes().item(46).getChildNodes())+", "+getNodeValue("city", returnList.item(0).getChildNodes().item(46).getChildNodes())+", "+getNodeValue("name", returnList.item(0).getChildNodes().item(46).getChildNodes().item(10).getChildNodes());
						city = getNodeValue("city", returnList.item(0).getChildNodes().item(46).getChildNodes());
						pin = getNodeValue("postal", returnList.item(0).getChildNodes().item(46).getChildNodes());
					}
					String cst = "";
					String vat = "";
					String taxRate = "";
					String[] taxArray = getNodeValue("taxGSTName", returnList.item(i).getChildNodes()).split(" ");
					if(taxArray[0].equals("VAT")) {
						vat = "VAT";
						taxRate = taxArray[1];
					} else if(taxArray[0].equals("CST")) {
						cst = "CST";
						taxRate = taxArray[1];
					}
					final OBQuery<ProductPrice> obQuery2 = OBDal.getInstance().createQuery(ProductPrice.class, "as o where o.product.name='"+getNodeValue("itemcode", returnList.item(i).getChildNodes())+"'");
					final ProductPrice pPrice = obQuery2.list().get(0);
					double unitWoTax = (Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes())) * 100) / (100 + (Double.parseDouble(getNodeValue("taxrateGST", returnList.item(i).getChildNodes())) * 100));
					Long turnover = Math.round((Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes())) * Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes()))));
					double marginValue = (unitWoTax - Double.parseDouble(pPrice.getClCessionprice().toString())) * Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes()));
					double marginPercent =  marginValue / turnover;
					String marginValue2 = String.format("%.2f", marginValue);
					String marginPercent2 = String.format("%.2f", marginPercent);
					double itemCost = Double.parseDouble(getNodeValue("itemcost", returnList.item(i).getChildNodes()));
					double tax = (itemCost - ((itemCost * 100) / (100 + ((Double.parseDouble(getNodeValue("taxrateGST", returnList.item(i).getChildNodes()))) * 100))));
					tax = tax * Integer.parseInt(getNodeValue("qty", returnList.item(i).getChildNodes()));
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
					double totalCost = Math.round(turnover + Double.parseDouble(getNodeValue("shipcost", returnList.item(i).getChildNodes())));
					data.add(new String[] {getNodeValue("createtime", returnList1.item(k).getChildNodes()).replace("T", " ").substring(0, 19), getNodeValue("orderid", returnList1.item(k).getChildNodes()), invNo, addr, name, companyName, city, billingPin, pin, getNodeValue("itemcode", returnList.item(i).getChildNodes()), getNodeValue("qty", returnList.item(i).getChildNodes()), vat, cst, taxRate, ""+Math.round(Double.parseDouble(pPrice.getClCessionprice().toString())), ""+Math.round(unitWoTax), ""+Math.round(tax), turnover.toString(), getNodeValue("shipcost", returnList.item(i).getChildNodes()), ""+totalCost, marginValue2, marginPercent2});
					} catch(Exception e) {
						e.printStackTrace();
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
