package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;
import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
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
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.geography.Location;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

public class CustomersFromERP implements WebService {

	// Properties Singleton class instantiation
	Properties p = ECommerceUtil.getInstance().getProperties();
	private static final Logger LOG = Logger.getLogger(CustomersFromERP.class);
	
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		// Fetch the values from the URL(get)
		final String vendorID = request.getParameter("vid");
		final String userID = request.getParameter("ecomID");
		final String pwd = request.getParameter("ecomP");
		
		// SOAP Connection Initialisation
		final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		final SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		final Connection conn = OBDal.getInstance().getConnection();
		PreparedStatement pst = null;
		ResultSet rs = null;
		
		try {
			
			String date="";
			final String query="select * from ecom_ws_status where type_of_sync='wscust' and processed='Y' order by created desc limit 1";
			pst = conn.prepareStatement(query);
			rs = pst.executeQuery();
			while (rs.next()) {
				date = rs.getTimestamp("end_time").toString();
			}
			final StringBuilder oldCusts = new StringBuilder();
			final StringBuilder newCusts = new StringBuilder();
			// for c_bpartner table
			/*final OBCriteria<BusinessPartner> bpQuery = OBDal.getInstance().createCriteria(BusinessPartner.class);
//			bpQuery.add(Restrictions.eq(BusinessPartner.PROPERTY_RCSOURCE, p.getProperty("ecomstoreOrg")));
			bpQuery.add(Restrictions.eq(BusinessPartner.PROPERTY_RCSTATUS, "A"));
//			bpQuery.add(Restrictions.ne(BusinessPartner.PROPERTY_RCEMAIL, null));
			bpQuery.add(Restrictions.between(BusinessPartner.PROPERTY_UPDATED, Timestamp.valueOf(date), new Timestamp(new java.util.Date().getTime())));
			// for c_bpartner table
			final OBCriteria<Location> locQuery = OBDal.getInstance().createCriteria(Location.class);
			locQuery.add(Restrictions.between(Location.PROPERTY_UPDATED, Timestamp.valueOf(date), new Timestamp(new java.util.Date().getTime())));
			// for c_bpartner table
			final OBCriteria<RCCompany> rcCompanyQuery = OBDal.getInstance().createCriteria(RCCompany.class);
			rcCompanyQuery.add(Restrictions.between(RCCompany.PROPERTY_UPDATED, Timestamp.valueOf(date), new Timestamp(new java.util.Date().getTime())));*/
			final StringBuilder whereOrderByClause = new StringBuilder();
			whereOrderByClause.append("as b where b.rCStatus='A' and ((b.updated between '"+date+"' and now()) or");
			whereOrderByClause.append("(b.rCCompany.updated between '"+date+"' and now()) or");
			whereOrderByClause.append("(b.rCCompany.locationAddress.updated between '"+date+"' and now()))");

			final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class, whereOrderByClause.toString());
			
//			final OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class, "as bp where bp.updated between '"+date+"' and NOW()");
	
			if(bpQuery.count() > 0) {
				int i = 0;
				final ScrollableResults scroller = bpQuery.scroll(ScrollMode.FORWARD_ONLY);
				while (scroller.next()) {
			
					final BusinessPartner bp = (BusinessPartner) scroller.get()[0];
						
						// Send SOAP Message to SOAP Server
					final SOAPMessage findCustomerByDIdResp = soapConnection.call(findCustomerByDId(vendorID, userID, pwd, bp.getRCOxylane()), p.getProperty("serverURL")+"customerservice");
					final SOAPBody findCustomerByDIdRespBody = findCustomerByDIdResp.getSOAPBody();
					final NodeList customerList = findCustomerByDIdRespBody.getElementsByTagName("ns1:Customer");


					if(customerList.getLength() == 0) {
						try {
							
//							final OBQuery<org.openbravo.model.common.businesspartner.Location> locObQuery = OBDal.getInstance().createQuery(org.openbravo.model.common.businesspartner.Location.class, "as o where o.businessPartner='"+bp.getId()+"'");
//							final org.openbravo.model.common.businesspartner.Location bpLoc = locObQuery.list().get(0);
//							Location location2 = OBDal.getInstance().get(Location.class, bpLoc.getLocationAddress().getId());
							Location location2 = OBDal.getInstance().get(Location.class, bp.getRCCompany().getLocationAddress().getId());
							
							final Map<String, String> customerMap = new HashMap<String, String>();
							if(bp.getRCEmail() != null) {
								customerMap.put("loginname", bp.getRCEmail());
							} else {
								customerMap.put("loginname", "");
							}
							if(bp.getRCOxylane() != null) {
								customerMap.put("invoicecustomer", bp.getRCOxylane());
							} else {
								customerMap.put("invoicecustomer", "");
							}
							if(bp.getName() != null) {
								customerMap.put("firstname", bp.getName());
							} else {
								customerMap.put("firstname", "");
							}
							if(bp.getName2() != null) {
								customerMap.put("lastname", bp.getName2());
							} else {
								customerMap.put("lastname", "");
							}
							if(location2.getAddressLine1() != null) {
								customerMap.put("address1", location2.getAddressLine1());
							} else {
								customerMap.put("address1", "");
							}
							if(location2.getAddressLine2() != null) {
								customerMap.put("address2", location2.getAddressLine2());
							} else {
								customerMap.put("address2", "");
							}
							if(location2.getRCAddress3() != null) {
								String addr3 = "";
								if(location2.getRCAddress4() != null) {
									addr3 = (location2.getRCAddress3()+", "+location2.getRCAddress4());
								} else {
									addr3 = location2.getRCAddress3();
								}
								customerMap.put("address3", addr3);
							} else {
								customerMap.put("address3", "");
							}
							if(location2.getCityName() != null) {
								customerMap.put("city", location2.getCityName());
							} else {
								customerMap.put("city", "");
							}
							if(location2.getPostalCode() != null) {
								customerMap.put("postal", location2.getPostalCode());
							} else {
								customerMap.put("postal", "");
							}
							if(location2.getRegion().getName() != null) {
								customerMap.put("region", location2.getRegion().getName());
							} else {
								customerMap.put("region", "");
							}
							if(location2.getCountry().getName() != null) {
								customerMap.put("country", location2.getCountry().getName());
							} else {
								customerMap.put("country", "");
							}
							if(bp.isRCNotify()) {
								customerMap.put("newsletter", "Yes");
							} else {
								customerMap.put("newsletter", "No");
							}
							if(bp.getRCCompany().getLicenseNo() != null) {
								customerMap.put("taxid", bp.getRCCompany().getLicenseNo());
							} else {
								customerMap.put("taxid", "");
							}
							if(bp.getRCCompany().getId() != null) {
								customerMap.put("erp_company", bp.getRCCompany().getId());
							} else {
								customerMap.put("erp_company", "");
							}
							if(bp.getRCCompany().getCompanyName() != null) {
								customerMap.put("company", bp.getRCCompany().getCompanyName());
							} else {
								customerMap.put("company", "");
							}
							if(bp.getRCMobile() != null) {
								customerMap.put("mobile", bp.getRCMobile());
							} else {
								customerMap.put("mobile", "");
							}
							customerMap.put("countryId", "101");
							if(location2.getRegion().getSearchKey() != null) {

								customerMap.put("regionId", location2.getRegion().getSearchKey());
							} else {
								customerMap.put("regionId", "");
							}
							
							// Sending HTTP POST request
				    	  	HttpURLConnection hc;
							try {
								hc = createConnection(bp.getRCOxylane());
								hc.connect();
								
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							// Send SOAP Message to SOAP Server
							soapConnection.call(createCustomer(vendorID, userID, pwd, customerMap), p.getProperty("serverURL")+"customerservice");
							// Send SOAP Message to SOAP Server
							final SOAPMessage clientInfo = soapConnection.call(findByEmail(vendorID, userID, pwd, bp.getRCEmail()), p.getProperty("serverURL")+"customerservice");
							final SOAPBody clientInfoResponseBody = clientInfo.getSOAPBody();
							final NodeList clientInfoPropertiesList = clientInfoResponseBody.getElementsByTagName("ns1:Customer");
							final String clintId = getNodeValue("clientid", clientInfoPropertiesList.item(0).getChildNodes());
							
							// Send SOAP Message to SOAP Server
							soapConnection.call(sendEmail(vendorID, userID, pwd, bp.getRCEmail(), clintId, bp.getName(), bp.getName2()), p.getProperty("serverURL")+"customerservice");
							
							// clear the session every 100 records
							if ((i % 100) == 0) {
								OBDal.getInstance().getSession().clear();
							}
							i++;
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						newCusts.append(bp.getRCOxylane()+",");
					} else {
						try {
							Location location2 = OBDal.getInstance().get(Location.class, bp.getRCCompany().getLocationAddress().getId());
																			
							final Map<String, String> customerMap = new HashMap<String, String>();
							if(bp.getRCEmail() != null) {
								customerMap.put("loginname", bp.getRCEmail());
							} else {
								customerMap.put("loginname", "");
							}
							if(bp.getRCOxylane() != null) {
								customerMap.put("invoicecustomer", bp.getRCOxylane());
							} else {
								customerMap.put("invoicecustomer", "");
							}
							if(bp.getName() != null) {
								customerMap.put("firstname", bp.getName());
							} else {
								customerMap.put("firstname", "");
							}
							if(bp.getName2() != null) {
								customerMap.put("lastname", bp.getName2());
							} else {
								customerMap.put("lastname", "");
							}
							if(location2.getAddressLine1() != null) {
								customerMap.put("address1", location2.getAddressLine1());
							} else {
								customerMap.put("address1", "");
							}
							if(location2.getAddressLine2() != null) {
								customerMap.put("address2", location2.getAddressLine2());
							} else {
								customerMap.put("address2", "");
							}
							if(location2.getRCAddress3() != null) {
								String addr3 = "";
								if(location2.getRCAddress4() != null) {
									addr3 = (location2.getRCAddress3()+", "+location2.getRCAddress4());
								} else {
									addr3 = location2.getRCAddress3();
								}
								customerMap.put("address3", addr3);
							} else {
								customerMap.put("address3", "");
							}
							if(location2.getCityName() != null) {
								customerMap.put("city", location2.getCityName());
							} else {
								customerMap.put("city", "");
							}
							if(location2.getPostalCode() != null) {
								customerMap.put("postal", location2.getPostalCode());
							} else {
								customerMap.put("postal", "");
							}
							if(location2.getRegion().getName() != null) {
								customerMap.put("region", location2.getRegion().getName());
							} else {
								customerMap.put("region", "");
							}
							if(location2.getCountry().getName() != null) {
								customerMap.put("country", location2.getCountry().getName());
							} else {
								customerMap.put("country", "");
							}
							if(bp.isRCNotify()) {
								customerMap.put("newsletter", "Yes");
							} else {
								customerMap.put("newsletter", "No");
							}
							if(bp.getRCCompany().getLicenseNo() != null) {
								customerMap.put("taxid", bp.getRCCompany().getLicenseNo());
							} else {
								customerMap.put("taxid", "");
							}
							if(bp.getRCCompany().getId() != null) {
								customerMap.put("erp_company", bp.getRCCompany().getId());
							} else {
								customerMap.put("erp_company", "");
							}
							if(bp.getRCCompany().getCompanyName() != null) {
								customerMap.put("company", bp.getRCCompany().getCompanyName());
							} else {
								customerMap.put("company", "");
							}
							if(bp.getRCMobile() != null) {
								customerMap.put("mobile", bp.getRCMobile());
							} else {
								customerMap.put("mobile", "");
							}
							customerMap.put("countryId", "101");
							if(location2.getRegion().getSearchKey() != null) {
								customerMap.put("regionId", location2.getRegion().getSearchKey());
							} else {
								customerMap.put("regionId", "");
							}
							
							// Sending HTTP POST request
				    	  	HttpURLConnection hc;
							try {
								hc = createConnection(bp.getRCOxylane());
								hc.connect();
								
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							// Send SOAP Message to SOAP Server
							soapConnection.call(updateCustomer(vendorID, userID, pwd, customerMap), p.getProperty("serverURL")+"customerservice");
							// Send SOAP Message to SOAP Server
//							final SOAPMessage clientInfo = soapConnection.call(findByEmail(vendorID, userID, pwd, bp.getRCEmail()), p.getProperty("serverURL")+"customerservice");
//							final SOAPBody clientInfoResponseBody = clientInfo.getSOAPBody();
//							final NodeList clientInfoPropertiesList = clientInfoResponseBody.getElementsByTagName("ns1:Customer");
//							final String clintId = getNodeValue("clientid", clientInfoPropertiesList.item(0).getChildNodes());
							// Send SOAP Message to SOAP Server
//							soapConnection.call(sendEmail(vendorID, userID, pwd, bp.getRCEmail(), clintId), p.getProperty("serverURL")+"customerservice");
							
							// clear the session every 100 records
							if ((i % 100) == 0) {
								OBDal.getInstance().getSession().clear();
							}
							i++;
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						oldCusts.append(bp.getRCOxylane()+",");
					}
				}
			}
			String remarks = "";
			if(oldCusts.length() > 0) {
				remarks = " updated-"+oldCusts.deleteCharAt(oldCusts.length()-1).toString();
			}
			if(newCusts.length() > 0) {
				remarks = remarks + " new-"+newCusts.deleteCharAt(newCusts.length()-1).toString();
			}
			// Insert a Record in ecom_ws_status
			final String insertQuery="INSERT INTO ecom_ws_status (ecom_ws_status_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, type_of_sync, start_time, end_time, processing, processed, remarks, no_of_records) VALUES (GET_UUID(), '187D8FC945A5481CB41B3EE767F80DBB', '057FF7ABBAAA43ECA533ACA272264A1A', 'Y', '100', '100', 'wscust', now(), now(), 'N', 'Y', '"+remarks+"', '"+bpQuery.count()+"');";
			pst = conn.prepareStatement(insertQuery);
			pst.executeUpdate();
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', ordersDid:'"+bpQuery.count()+"' }";
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

	private SOAPMessage updateCustomer(String vendorID, String userID,
			String pwd, Map<String, String> customerMap) throws Exception {
		
		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
		final  SOAPEnvelope envelope = soapPart.getEnvelope();
        // add namespace
        envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
        envelope.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        final  SOAPHeader header = envelope.getHeader();
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
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("update", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("loginname", "web");
        	soapBodyElem3.addTextNode((String) customerMap.get("loginname"));
        	SOAPElement soapBodyElem4 = soapBodyElem2.addChildElement("vendorid", "web");
        	soapBodyElem4.addTextNode(vendorID);
        	SOAPElement soapBodyElem5 = soapBodyElem2.addChildElement("invoicecustomer", "web");
        	soapBodyElem5.addTextNode((String)customerMap.get("invoicecustomer"));
        	SOAPElement soapBodyElem6 = soapBodyElem2.addChildElement("email", "web");
        	soapBodyElem6.addTextNode((String)customerMap.get("loginname"));
        	SOAPElement soapBodyElem7 = soapBodyElem2.addChildElement("firstname", "web");
        	soapBodyElem7.addTextNode((String)customerMap.get("firstname"));
        	SOAPElement soapBodyElem8 = soapBodyElem2.addChildElement("lastname", "web");
        	soapBodyElem8.addTextNode((String)customerMap.get("lastname"));
        	SOAPElement soapBodyElem16 = soapBodyElem2.addChildElement("addresses", "web");
        	SOAPElement soapBodyElem9 = soapBodyElem16.addChildElement("Address", "web");
        	SOAPElement soapBodyElem10 = soapBodyElem9.addChildElement("address1", "web");
        	soapBodyElem10.addTextNode((String)customerMap.get("address1"));
        	SOAPElement soapBodyElem11 = soapBodyElem9.addChildElement("address2", "web");
        	soapBodyElem11.addTextNode((String)customerMap.get("address2"));
        	SOAPElement soapBodyElem12 = soapBodyElem9.addChildElement("address3", "web");
        	soapBodyElem12.addTextNode((String)customerMap.get("address3"));
        	SOAPElement soapBodyElem20 = soapBodyElem9.addChildElement("company", "web");
        	soapBodyElem20.addTextNode((String)customerMap.get("company"));
        	SOAPElement soapBodyElem13 = soapBodyElem9.addChildElement("city", "web");
        	soapBodyElem13.addTextNode((String)customerMap.get("city"));
        	SOAPElement soapBodyElem15 = soapBodyElem9.addChildElement("email", "web");
        	soapBodyElem15.addTextNode((String)customerMap.get("loginname"));
        	SOAPElement soapBodyElem17 = soapBodyElem9.addChildElement("firstname", "web");
        	soapBodyElem17.addTextNode((String)customerMap.get("firstname"));
        	SOAPElement soapBodyElem18 = soapBodyElem9.addChildElement("lastname", "web");
        	soapBodyElem18.addTextNode((String)customerMap.get("lastname"));
        	SOAPElement soapBodyElem19 = soapBodyElem9.addChildElement("nickname", "web");
        	soapBodyElem19.addTextNode("Billing Address");
        	SOAPElement soapBodyElem26 = soapBodyElem9.addChildElement("type", "web");
        	soapBodyElem26.addTextNode("3");
        	SOAPElement soapBodyElem45 = soapBodyElem9.addChildElement("phone", "web");
        	soapBodyElem45.addTextNode(customerMap.get("mobile"));
        	SOAPElement soapBodyElem46 = soapBodyElem9.addChildElement("country", "web");
        	SOAPElement soapBodyElem47 = soapBodyElem46.addChildElement("id", "ns1");
        	soapBodyElem47.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        	soapBodyElem47.addTextNode(customerMap.get("countryId"));
        	SOAPElement soapBodyElem48 = soapBodyElem9.addChildElement("province", "web");
        	SOAPElement soapBodyElem49 = soapBodyElem48.addChildElement("a2", "ns1");
        	soapBodyElem49.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        	soapBodyElem49.addTextNode(customerMap.get("regionId"));
        	
        	
        	SOAPElement soapBodyElem21 = soapBodyElem9.addChildElement("postal", "web");
        	soapBodyElem21.addTextNode((String)customerMap.get("postal"));
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        return soapMessage;
	}

	private SOAPMessage findCustomerByDId(String vendorID, String userID,
			String pwd, String rcOxylane) throws Exception {
		
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
        	el.setValue(vendorID);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findByInvCustomerCodes", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("string", "web");
        	soapBodyElem3.addTextNode(rcOxylane);
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        return soapMessage;
	}

	private SOAPMessage sendEmail(String vendorID, String userID, String pwd,
			String rcEmail, String clintId, String firstName, String lastName) throws Exception {
		
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
        	el.setValue(vendorID);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("sendRegistrationEmail", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(rcEmail);
        	SOAPElement soapBodyElem3 = soapBodyElem.addChildElement("in1", "web");
        	soapBodyElem3.addTextNode(clintId);
        	SOAPElement soapBodyElem4 = soapBodyElem.addChildElement("in2", "web");
        	soapBodyElem4.addTextNode(firstName);
        	SOAPElement soapBodyElem5 = soapBodyElem.addChildElement("in3", "web");
        	soapBodyElem5.addTextNode(lastName);
        	SOAPElement soapBodyElem6 = soapBodyElem.addChildElement("in4", "web");
        	soapBodyElem6.addTextNode(p.getProperty("skin_id"));
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        return soapMessage;
	}

	private SOAPMessage findByEmail(String vendorID, String userID, String pwd,
			String rcEmail) throws Exception {
		
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
        	el.setValue(vendorID);
        	SOAPElement el2 = header.addHeaderElement(envelope.createName("AuthenticationToken", "", "urn:commerce:authentication"));
        	SOAPElement el3 = el2.addChildElement(envelope.createName("Username", "", "urn:commerce:authentication"));
        	SOAPElement el4 = el2.addChildElement(envelope.createName("Password", "", "urn:commerce:authentication"));
        	el3.setValue(userID);
        	el4.setValue(pwd);
        	// SOAP Body
        	SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("findByEmail", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem2.addTextNode(rcEmail);
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

	private SOAPMessage createCustomer(String vId, String uID,
			String pwd, Map<String, String> custMap) throws Exception {

		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();
		final SOAPPart soapPart = soapMessage.getSOAPPart();
        // SOAP Envelope
		final  SOAPEnvelope envelope = soapPart.getEnvelope();
        // add namespace
        envelope.addNamespaceDeclaration("web", "http://webservices.commerce.avetti.com");
        envelope.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
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
        	SOAPElement soapBodyElem = soapBody.addChildElement("create", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("loginname", "web");
        	soapBodyElem3.addTextNode((String) custMap.get("loginname"));
        	SOAPElement soapBodyElem4 = soapBodyElem2.addChildElement("vendorid", "web");
        	soapBodyElem4.addTextNode(vId);
        	SOAPElement soapBodyElem5 = soapBodyElem2.addChildElement("invoicecustomer", "web");
        	soapBodyElem5.addTextNode((String)custMap.get("invoicecustomer"));
        	SOAPElement soapBodyElem6 = soapBodyElem2.addChildElement("email", "web");
        	soapBodyElem6.addTextNode((String)custMap.get("loginname"));
        	SOAPElement soapBodyElem7 = soapBodyElem2.addChildElement("firstname", "web");
        	soapBodyElem7.addTextNode((String)custMap.get("firstname"));
        	SOAPElement soapBodyElem8 = soapBodyElem2.addChildElement("lastname", "web");
        	soapBodyElem8.addTextNode((String)custMap.get("lastname"));
        	SOAPElement soapBodyElem16 = soapBodyElem2.addChildElement("addresses", "web");
        	SOAPElement soapBodyElem9 = soapBodyElem16.addChildElement("Address", "web");
        	SOAPElement soapBodyElem10 = soapBodyElem9.addChildElement("address1", "web");
        	soapBodyElem10.addTextNode((String)custMap.get("address1"));
        	SOAPElement soapBodyElem11 = soapBodyElem9.addChildElement("address2", "web");
        	soapBodyElem11.addTextNode((String)custMap.get("address2"));
        	SOAPElement soapBodyElem12 = soapBodyElem9.addChildElement("address3", "web");
        	soapBodyElem12.addTextNode((String)custMap.get("address3"));
        	SOAPElement soapBodyElem20 = soapBodyElem9.addChildElement("company", "web");
        	soapBodyElem20.addTextNode((String)custMap.get("company"));
        	SOAPElement soapBodyElem13 = soapBodyElem9.addChildElement("city", "web");
        	soapBodyElem13.addTextNode((String)custMap.get("city"));
        	SOAPElement soapBodyElem15 = soapBodyElem9.addChildElement("email", "web");
        	soapBodyElem15.addTextNode((String)custMap.get("loginname"));
        	SOAPElement soapBodyElem17 = soapBodyElem9.addChildElement("firstname", "web");
        	soapBodyElem17.addTextNode((String)custMap.get("firstname"));
        	SOAPElement soapBodyElem18 = soapBodyElem9.addChildElement("lastname", "web");
        	soapBodyElem18.addTextNode((String)custMap.get("lastname"));
        	SOAPElement soapBodyElem19 = soapBodyElem9.addChildElement("nickname", "web");
        	soapBodyElem19.addTextNode("Billing Address");
        	SOAPElement soapBodyElem101 = soapBodyElem9.addChildElement("def", "web");
        	soapBodyElem101.addTextNode("true");
        	SOAPElement soapBodyElem26 = soapBodyElem9.addChildElement("type", "web");
        	soapBodyElem26.addTextNode("3");
        	SOAPElement soapBodyElem45 = soapBodyElem9.addChildElement("phone", "web");
        	soapBodyElem45.addTextNode(custMap.get("mobile"));
        	SOAPElement soapBodyElem46 = soapBodyElem9.addChildElement("country", "web");
        	SOAPElement soapBodyElem47 = soapBodyElem46.addChildElement("id", "ns1");
        	soapBodyElem47.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        	soapBodyElem47.addTextNode(custMap.get("countryId"));
        	SOAPElement soapBodyElem48 = soapBodyElem9.addChildElement("province", "web");
        	SOAPElement soapBodyElem49 = soapBodyElem48.addChildElement("a2", "ns1");
        	soapBodyElem49.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        	soapBodyElem49.addTextNode(custMap.get("regionId"));
        	SOAPElement soapBodyElem21 = soapBodyElem9.addChildElement("postal", "web");
        	soapBodyElem21.addTextNode((String)custMap.get("postal"));
        	
        	SOAPElement soapBodyElem50 = soapBodyElem16.addChildElement("Address", "web");
        	SOAPElement soapBodyElem51 = soapBodyElem50.addChildElement("address1", "web");
        	soapBodyElem51.addTextNode((String)custMap.get("address1"));
        	SOAPElement soapBodyElem52 = soapBodyElem50.addChildElement("address2", "web");
        	soapBodyElem52.addTextNode((String)custMap.get("address2"));
        	SOAPElement soapBodyElem53 = soapBodyElem50.addChildElement("address3", "web");
        	soapBodyElem53.addTextNode((String)custMap.get("address3"));
        	SOAPElement soapBodyElem555 = soapBodyElem50.addChildElement("company", "web");
        	soapBodyElem555.addTextNode((String)custMap.get("company"));
        	SOAPElement soapBodyElem54 = soapBodyElem50.addChildElement("city", "web");
        	soapBodyElem54.addTextNode((String)custMap.get("city"));
        	SOAPElement soapBodyElem55 = soapBodyElem50.addChildElement("email", "web");
        	soapBodyElem55.addTextNode((String)custMap.get("loginname"));
        	SOAPElement soapBodyElem56 = soapBodyElem50.addChildElement("firstname", "web");
        	soapBodyElem56.addTextNode((String)custMap.get("firstname"));
        	SOAPElement soapBodyElem57 = soapBodyElem50.addChildElement("lastname", "web");
        	soapBodyElem57.addTextNode((String)custMap.get("lastname"));
        	SOAPElement soapBodyElem58 = soapBodyElem50.addChildElement("nickname", "web");
        	soapBodyElem58.addTextNode("SHIPPING ADDRESS");
        	SOAPElement soapBodyElem102 = soapBodyElem50.addChildElement("def", "web");
        	soapBodyElem102.addTextNode("true");
        	SOAPElement soapBodyElem59 = soapBodyElem50.addChildElement("type", "web");
        	soapBodyElem59.addTextNode("2");
        	SOAPElement soapBodyElem60 = soapBodyElem50.addChildElement("phone", "web");
        	soapBodyElem60.addTextNode(custMap.get("mobile"));
        	SOAPElement soapBodyElem61 = soapBodyElem50.addChildElement("country", "web");
        	SOAPElement soapBodyElem62 = soapBodyElem61.addChildElement("id", "ns1");
        	soapBodyElem62.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        	soapBodyElem62.addTextNode(custMap.get("countryId"));
        	SOAPElement soapBodyElem63 = soapBodyElem50.addChildElement("province", "web");
        	SOAPElement soapBodyElem64 = soapBodyElem63.addChildElement("a2", "ns1");
        	soapBodyElem64.addNamespaceDeclaration("ns1", "http://domainmodel.commerce.avetti.com");
        	soapBodyElem64.addTextNode(custMap.get("regionId"));
        	SOAPElement soapBodyElem65 = soapBodyElem50.addChildElement("postal", "web");
        	soapBodyElem65.addTextNode((String)custMap.get("postal"));
        	
        	
        	SOAPElement soapBodyElem22 = soapBodyElem2.addChildElement("properties", "web");
        	SOAPElement soapBodyElem23 = soapBodyElem22.addChildElement("Customerprops", "web");
        	SOAPElement soapBodyElem24 = soapBodyElem23.addChildElement("propname", "web");
        	soapBodyElem24.addTextNode(p.getProperty("propName"));
        	SOAPElement soapBodyElem25 = soapBodyElem23.addChildElement("propvalue", "web");
        	soapBodyElem25.addTextNode(p.getProperty("propValue"));
        	
        	if(!custMap.get("region").equals("") || custMap.get("region") != null) {
	        	SOAPElement soapBodyElem27 = soapBodyElem22.addChildElement("Customerprops", "web");
	        	SOAPElement soapBodyElem28 = soapBodyElem27.addChildElement("propname", "web");
	        	soapBodyElem28.addTextNode("Region");
	        	SOAPElement soapBodyElem29 = soapBodyElem27.addChildElement("propvalue", "web");
	        	soapBodyElem29.addTextNode(custMap.get("region"));
        	}
        	
        	if(!custMap.get("country").equals("") || custMap.get("country") != null) {
	        	SOAPElement soapBodyElem30 = soapBodyElem22.addChildElement("Customerprops", "web");
	        	SOAPElement soapBodyElem31 = soapBodyElem30.addChildElement("propname", "web");
	        	soapBodyElem31.addTextNode("Country");
	        	SOAPElement soapBodyElem32 = soapBodyElem30.addChildElement("propvalue", "web");
	        	soapBodyElem32.addTextNode(custMap.get("country"));
        	}
        	
        	SOAPElement soapBodyElem33 = soapBodyElem22.addChildElement("Customerprops", "web");
        	SOAPElement soapBodyElem34 = soapBodyElem33.addChildElement("propname", "web");
        	soapBodyElem34.addTextNode("Customer Type");
        	SOAPElement soapBodyElem35 = soapBodyElem33.addChildElement("propvalue", "web");
        	soapBodyElem35.addTextNode("Normal");
        	
        	if(!custMap.get("newsletter").equals("") || custMap.get("newsletter") != null) {
	        	SOAPElement soapBodyElem36 = soapBodyElem22.addChildElement("Customerprops", "web");
	        	SOAPElement soapBodyElem37 = soapBodyElem36.addChildElement("propname", "web");
	        	soapBodyElem37.addTextNode("Newsletter");
	        	SOAPElement soapBodyElem38 = soapBodyElem36.addChildElement("propvalue", "web");
	        	soapBodyElem38.addTextNode(custMap.get("newsletter"));
        	}
        	
        	if(!custMap.get("taxid").equals("") || custMap.get("taxid") != null) {
	        	SOAPElement soapBodyElem39 = soapBodyElem22.addChildElement("Customerprops", "web");
	        	SOAPElement soapBodyElem40 = soapBodyElem39.addChildElement("propname", "web");
	        	soapBodyElem40.addTextNode("TAXID");
	        	SOAPElement soapBodyElem41 = soapBodyElem39.addChildElement("propvalue", "web");
	        	soapBodyElem41.addTextNode(custMap.get("taxid"));
        	}
        	
        	if(!custMap.get("erp_company").equals("") || custMap.get("erp_company") != null) {
	        	SOAPElement soapBodyElem42 = soapBodyElem22.addChildElement("Customerprops", "web");
	        	SOAPElement soapBodyElem43 = soapBodyElem42.addChildElement("propname", "web");
	        	soapBodyElem43.addTextNode("erpcompany_id");
	        	SOAPElement soapBodyElem44 = soapBodyElem42.addChildElement("propvalue", "web");
	        	soapBodyElem44.addTextNode(custMap.get("erp_company"));
        	}
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        return soapMessage;
		
	}
	
	protected HttpURLConnection createConnection(String oxylaneId) throws Exception {
		
		String customerDBURL = "";
		String customerDBUName = "";
		String customerDBPwd = "";
		OBContext.setAdminMode();
		final Map<String, String> custmerDBConfig = new HashMap<String, String>();
		OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
		configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.customerdb"));
		if(configInfoObCriteria.count() > 0) {
			for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
				custmerDBConfig.put(config.getKey(), config.getSearchKey());
			}
			customerDBURL = custmerDBConfig.get("customerdbWSURL");
			customerDBUName = custmerDBConfig.get("custSearchId");
			customerDBPwd = custmerDBConfig.get("custSearchPwd");
		}
		OBContext.restorePreviousMode();
		
		final URL url = new URL(customerDBURL+"/updateCustomerStatus?decathlonId="+oxylaneId+"&status=A&username="+customerDBUName+"&pwd="+customerDBPwd+"");
		LOG.info(customerDBURL+"/updateCustomerStatus?decathlonId="+oxylaneId+"&status=A&username="+customerDBUName+"&pwd="+customerDBPwd+"");
		final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
	  
		hc.setRequestMethod("POST");
	  	hc.setAllowUserInteraction(false);
	  	hc.setDefaultUseCaches(false);
	  	hc.setDoInput(true);
	  	hc.setInstanceFollowRedirects(true);
	  	hc.setUseCaches(false);
		return hc;
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

}
