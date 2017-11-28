package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

public class TaxToEcommerce implements WebService {

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		final String vendorID = request.getParameter("vid");
		final String userID = request.getParameter("ecomID");
		final String pwd = request.getParameter("ecomP");
		
		// Create SOAP Connection
		final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		final SOAPConnection soapConnection = soapConnectionFactory.createConnection();
		
		final Properties p = ECommerceUtil.getInstance().getProperties();
		final Connection conn = OBDal.getInstance().getConnection();
		PreparedStatement pst = null;
		ResultSet rs = null;
		
		int i=0;
		try {
			String date="";
			final String query="select * from ecom_ws_status where type_of_sync='wstax' and processed='Y' order by created desc limit 1";
			pst = conn.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				date = rs.getTimestamp("end_time").toString();
			}System.out.println(date);
			// Retrieving the products
			final OBQuery<Product> obQuery = OBDal.getInstance().createQuery(Product.class, "as p where  p.updated between '"+date+"' and '"+(new Timestamp((new java.util.Date()).getTime())).toString()+"'");
			final ScrollableResults scroller = obQuery.scroll(ScrollMode.FORWARD_ONLY);
			while (scroller.next()) {
				
				final Product prod = (Product) scroller.get()[0];
				// Getting the item id
				final SOAPMessage getItemIdResponse = soapConnection.call(getItemId(vendorID, userID, pwd, prod.getName()), p.getProperty("serverURL")+"itemservice");
				final SOAPBody getItemIdResponseBody = getItemIdResponse.getSOAPBody();
				final NodeList returnList = getItemIdResponseBody.getElementsByTagName("ns1:out");
				final String itemId = returnList.item(0).getChildNodes().item(10).getTextContent();
				
				// sending Request to Order Service findByItemStatus method
				final SOAPMessage soapResponse = soapConnection.call(attachProperty(vendorID, userID, pwd, itemId, prod.getTaxCategory().getName()), p.getProperty("serverURL")+"itemservice");
				// clear the session every 100 records
				if ((i % 100) == 0) {
					OBDal.getInstance().getSession().clear();
				}
				i++;
			}
			
			// Tax Category
			int j=0;
			// Retrieving the products
			final OBQuery<TaxRate> taxRates = OBDal.getInstance().createQuery(TaxRate.class, "as p where  p.updated between '"+date+"' and '"+(new Timestamp((new java.util.Date()).getTime())).toString()+"'");
			final ScrollableResults taxScroller = taxRates.scroll(ScrollMode.FORWARD_ONLY);
			while (taxScroller.next()) {
				
				final TaxRate taxRate = (TaxRate) taxScroller.get()[0];
				
				final SOAPMessage taxUpdateResponse = soapConnection.call(taxUpdate(vendorID, userID, pwd, taxRate.getTaxCategory().getName(), taxRate.getName(), taxRate.getRate()), p.getProperty("serverURL")+"taxservice");
				// clear the session every 100 records
				if ((j % 100) == 0) {
					OBDal.getInstance().getSession().clear();
				}
				j++;
			}
			
			// Insert a Record in ecom_ws_status
			final String insertQuery="INSERT INTO ecom_ws_status (ecom_ws_status_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, type_of_sync, start_time, end_time, processing, processed, remarks, no_of_records) VALUES (GET_UUID(), '187D8FC945A5481CB41B3EE767F80DBB', '057FF7ABBAAA43ECA533ACA272264A1A', 'Y', '100', '100', 'wstax', now(), now(), 'N', 'Y', '', '"+j+"');";
			pst = conn.prepareStatement(insertQuery);
			pst.executeUpdate();
			
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', updatedRecords,'"+j+"' }";
			response.setContentType("application/json");
			// Get the printwriter object from response to write the required json object to the output stream      
			PrintWriter out = response.getWriter();
			// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
			out.print(objectToReturn);
			out.flush();
		} catch (Exception e) {
			System.err.println("Error occurred while sending SOAP Request to Server");
            e.printStackTrace();
        }
		
	}

	private SOAPMessage getItemId(String vId, String uID, String pwd, String name) throws Exception {
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
        	el.setValue(vId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(uID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("find1", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(name);
        	
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

	private SOAPMessage attachProperty(String vId, String uID, String pwd, String itemcode, String taxCategory) throws Exception { 
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
        	el.setValue(vId);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(uID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("attachProperties", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("itemId", "web");
        	soapBodyElem3.addTextNode(itemcode);
        	SOAPElement soapBodyElem4 = soapBodyElem2.addChildElement("itemPropertiesInfoList", "web");
        	SOAPElement soapBodyElem5 = soapBodyElem4.addChildElement("ItemPropertiesInfo", "web");
        	SOAPElement soapBodyElem7 = soapBodyElem5.addChildElement("propnumber", "web");
        	soapBodyElem7.addTextNode("1");
        	SOAPElement soapBodyElem8 = soapBodyElem5.addChildElement("propvalue", "web");
        	soapBodyElem8.addTextNode(taxCategory);
        	
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
	
	private SOAPMessage taxUpdate(String vId, String uID, String pwd, String properType, String taxName, BigDecimal taxRate) throws Exception { 
		// TODO Auto-generated method stub
		
		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final  SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
		final SOAPEnvelope envelope = soapPart.getEnvelope();
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
        	SOAPElement soapBodyElem = soapBody.addChildElement("update", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("producttype", "web");
        	soapBodyElem3.addTextNode(properType);
        	SOAPElement soapBodyElem4 = soapBodyElem2.addChildElement("taxname", "web");
        	soapBodyElem4.addTextNode(taxName);
        	SOAPElement soapBodyElem5 = soapBodyElem2.addChildElement("taxrate", "web");
        	soapBodyElem5.addTextNode(taxRate.toString());
        	SOAPElement soapBodyElem6 = soapBodyElem2.addChildElement("vendorid", "web");
        	soapBodyElem6.addTextNode(vId);
        	
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
