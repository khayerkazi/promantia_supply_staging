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
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

public class PriceToECommerce implements WebService {

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
		try {
			String date="";
			final String query="select * from ecom_ws_status where type_of_sync='wsprice' and processed='Y' order by created desc limit 1";
			pst = conn.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				date = rs.getTimestamp("end_time").toString();
			}
			
			final OBQuery<ProductPrice> productPrices = OBDal.getInstance().createQuery(ProductPrice.class, "as p where p.priceListVersion='C22B1923204C479DB075B0DC5B1C0532' and p.updated between '"+date+"' and NOW()");
			final ScrollableResults scroller = productPrices.scroll(ScrollMode.FORWARD_ONLY);
			int i=0;
			while (scroller.next()) {
				
				final ProductPrice bp = (ProductPrice) scroller.get()[0];
				BigDecimal session_price = bp.getClCcunitprice();
				BigDecimal mrp_price = bp.getClMrpprice();
				String itemcode = bp.getProduct().getName();
				
				// Send SOAP Message to SOAP Server
				final SOAPMessage find2Response = soapConnection.call(callFind2(vendorID, userID, pwd, itemcode), p.getProperty("serverURL")+"priceservice");
				
				 /* Print the request message */
		        System.out.print("Request SOAP Message = ");
		        find2Response.writeTo(System.out);
		        System.out.println();
				
				final SOAPBody find2ResponseBody = find2Response.getSOAPBody();

				final NodeList returnList = find2ResponseBody.getElementsByTagName("ns1:Offerprices");
				
				final String offerPriceId = returnList.item(returnList.getLength() - 1).getChildNodes().item(11).getTextContent();
				try {
					// Send SOAP Message to SOAP Server
					final SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(vendorID, userID, pwd, session_price, mrp_price, itemcode, offerPriceId), p.getProperty("serverURL")+"priceservice");
					// Process the SOAP Response
		            
System.out.println("after close");
		        } catch (Exception e) {
System.err.println("Error occurred while sending SOAP Request to Server");
		            e.printStackTrace();
		        }
				// clear the session every 100 records
				if ((i % 100) == 0) {
					OBDal.getInstance().getSession().clear();
				}
				i++;
			}
			
			// Insert a Record in ecom_ws_status
			final String insertQuery="INSERT INTO ecom_ws_status (ecom_ws_status_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, type_of_sync, start_time, end_time, processing, processed, remarks, no_of_records) VALUES (GET_UUID(), '187D8FC945A5481CB41B3EE767F80DBB', '057FF7ABBAAA43ECA533ACA272264A1A', 'Y', '100', '100', 'wsprice', now(), now(), 'N', 'Y', '', '"+i+"');";
			pst = conn.prepareStatement(insertQuery);
			pst.executeUpdate();
			
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', updatedRecords,'"+i+"' }";
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
			conn.close();
			pst.close();
			rs.close();
		}
	}

	private SOAPMessage callFind2(String vId, String uID, String pwd, String itemcode) throws Exception {
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
        	el.setValue(pwd);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(uID);
        	el4.setValue(pwd);
        	
	        // SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("find2", "web");
        	SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem1.addTextNode(itemcode);
        	
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

	private static SOAPMessage createSOAPRequest(String vId, String uID, String pwd, BigDecimal session_price, BigDecimal mrp_price, String itemcode, String id) throws Exception {
		
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
        	SOAPElement soapBodyElem = soapBody.addChildElement("update", "web");
        	SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem1.addChildElement("itemcode", "web");
        	soapBodyElem2.addTextNode(itemcode);
        	SOAPElement soapBodyElem3 = soapBodyElem1.addChildElement("listprice", "web");
        	soapBodyElem3.addTextNode(session_price.toString());
        	SOAPElement soapBodyElem4 = soapBodyElem1.addChildElement("id", "web");
        	soapBodyElem4.addTextNode(id);
        	SOAPElement soapBodyElem5 = soapBodyElem1.addChildElement("price_1", "web");
        	soapBodyElem5.addTextNode(mrp_price.toString());
        	SOAPElement soapBodyElem6 = soapBodyElem1.addChildElement("vendorid", "web");
        	soapBodyElem6.addTextNode(pwd);
        	
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
