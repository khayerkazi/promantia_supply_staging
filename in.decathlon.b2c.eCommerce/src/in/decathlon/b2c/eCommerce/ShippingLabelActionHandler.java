package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;

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
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.w3c.dom.NodeList;

public class ShippingLabelActionHandler extends BaseActionHandler {

	// Properties Singleton class instantiation
	final Properties p = ECommerceUtil.getInstance().getProperties();
	private static Logger log4j =  Logger.getLogger(ShippingLabelActionHandler.class);
	
	@Override
	protected JSONObject execute(Map<String, Object> parameters, String content) {
		// Declaring the result JSON Obj
		JSONObject result = new JSONObject();
		
		// Avetti Username and password properties
		final String vendorId = p.getProperty("avettivendor");
		final String userID = p.getProperty("avettiwsuser");
		final String pwd = p.getProperty("avettipwd");
		
		try {
			
			final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			final SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			OBContext.setAdminMode(true);
			final JSONObject jsonData = new JSONObject(content);
			final String recordId = jsonData.getString("recordId");
			final String action = jsonData.getString("action");
			log4j.info("the selected record id is "+ recordId);
			log4j.info("the selected record action is "+ action);
			
			final OBQuery<Invoice> categoryQuery = OBDal.getInstance().createQuery(Invoice.class, "id=:id");
			categoryQuery.setNamedParameter("id", recordId);
			final Invoice invoice = categoryQuery.list().get(0);
			
			final SOAPMessage findByIdResponse = soapConnection.call(findById(vendorId, userID, pwd, invoice.getDescription()), p.getProperty("serverURL")+"orderservice");
			final SOAPBody findByIdResponseeBody = findByIdResponse.getSOAPBody();
			final NodeList findByIdList = findByIdResponseeBody.getElementsByTagName("ns1:out");
			
			final SOAPMessage findOrderItemsResponse = soapConnection.call(findOrderItems(vendorId, userID, pwd, getNodeValue("orderdataid", findByIdList.item(0).getChildNodes())), p.getProperty("serverURL")+"orderservice");
			final SOAPBody findOrderItemsResponseBody = findOrderItemsResponse.getSOAPBody();
			final NodeList findOrderItemsList = findOrderItemsResponseBody.getElementsByTagName("ns2:OrderItem");
			
			final SOAPMessage findCustomerByEmailResponse = soapConnection.call(findCustomerByEmail(vendorId, userID, pwd, getNodeValue("loginname", findByIdList.item(0).getChildNodes())), p.getProperty("serverURL")+"customerservice");
			final SOAPBody findCustomerByEmailResponseBody = findCustomerByEmailResponse.getSOAPBody();
			final NodeList findCustomerByEmailList = findCustomerByEmailResponseBody.getElementsByTagName("Customerprops");
			
			// StringBuilder for preparing the Shipping Address html code
			final StringBuilder shippingAddress = new StringBuilder();
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			if (!getNodeValue("firstname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("firstname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			if (!getNodeValue("lastname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append("&nbsp;" + getNodeValue("lastname", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			if (invoice.getBusinessPartner().getRCCompany().getCompanyName() != null) {
				shippingAddress.append(invoice.getBusinessPartner().getRCCompany().getCompanyName());
			}
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			if (!getNodeValue("address1", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("address1", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			} 
			if (!getNodeValue("address2", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(",&nbsp;" + getNodeValue("address2", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			if (!getNodeValue("address3", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(",&nbsp;" + getNodeValue("address3", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			if (!getNodeValue("city", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("city", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			shippingAddress.append(getNodeValue("name", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes().item(31).getChildNodes()));
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			shippingAddress.append(getNodeValue("name", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes().item(10).getChildNodes()));
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			if (!getNodeValue("postal", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("postal", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("&nbsp;</div>");
			shippingAddress.append("<div style='width:100%;height:20px;font-size:11px;line-height:20px;text-align:right;'>&nbsp;");
			if (!getNodeValue("phone", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()).equals("")) {
				shippingAddress.append(getNodeValue("phone", findOrderItemsList.item(0).getChildNodes().item(46).getChildNodes()));
			}
			shippingAddress.append("&nbsp;</div>");
			result.put("invoiceNum", invoice.getDocumentNo());
			
			// Invoice Total calculation starts
			final OBQuery<InvoiceLine> invlinCriteria = OBDal.getInstance().createQuery(InvoiceLine.class, "as o where o.invoice='"+invoice.getId()+"'");
			
			double subTotal = 0.0;
			double shipTotal = 0.0;
			double tax14 = 0.0;
			double tax5 = 0.0;
			int part=0;
			if(invlinCriteria.list().size() > 0) {
				for (InvoiceLine iLine : invlinCriteria.list()) {
					part++;
					BigDecimal qty = iLine.getInvoicedQuantity();
					BigDecimal unitPrice = iLine.getUnitPrice();
					String desc = "";
					String qtyAvetti;
					Double delCharges = 0.0;
					for (int k = 0; k < findOrderItemsList.getLength(); k++) {
						if(getNodeValue("itemcode", findOrderItemsList.item(k).getChildNodes()).equals(iLine.getProduct().getName()))
						{
							desc = getNodeValue("title", findOrderItemsList.item(k).getChildNodes());
							qtyAvetti = getNodeValue("qty", findOrderItemsList.item(k).getChildNodes());
							delCharges = Double.parseDouble(iLine.getChargeAmount().toString()) / Double.parseDouble(qtyAvetti);
	 					}
					}
					subTotal = subTotal + Double.parseDouble(iLine.getTaxAmount().add(unitPrice.multiply(qty)).toString());
					shipTotal = shipTotal + Double.parseDouble(iLine.getChargeAmount().toString());
					if(iLine.getTax().getName().equals("VAT 5.50%")) {
						tax5 = Math.round(tax5 + (delCharges * Double.parseDouble(qty.toString())));
					} else if(iLine.getTax().getName().equals("VAT 14.50%")) {
						tax14 = Math.round(tax14 + (delCharges * Double.parseDouble(qty.toString())));
					}
				}
			}
			
			double invtotal = subTotal + tax14 + tax5;
			
			result.put("orderId", getNodeValue("orderid", findByIdList.item(0).getChildNodes()));
			result.put("shipAddress", shippingAddress.toString());
			result.put("annu_test", "Hello");
			result.put("invTotal", Math.round(invtotal));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		      throw new OBException(e);
	    } finally {
			OBContext.restorePreviousMode();
		}
	}
	 private SOAPMessage findCustomerByEmail(String vendorId, String userID,
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

		private SOAPMessage findOrderItems(String vendorId, String userID, String pwd,
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
			    	SOAPElement soapBodyElem = soapBody.addChildElement("findOrderItems", "web");
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

		  private SOAPMessage findById(String vendorId, String userID, String pwd,
				String description) throws Exception {
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
		    	SOAPElement soapBodyElem = soapBody.addChildElement("findById", "web");
		    	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
		    	soapBodyElem2.addTextNode(description);
		    	
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
}
