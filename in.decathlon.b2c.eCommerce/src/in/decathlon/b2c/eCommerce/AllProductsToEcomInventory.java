package in.decathlon.b2c.eCommerce;

import in.decathlon.b2c.eCommerce.util.ECommerceUtil;
import in.decathlon.integration.PassiveDB;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;

import au.com.bytecode.opencsv.CSVWriter;

public class AllProductsToEcomInventory implements WebService {

	// Properties Singleton class instantiation
	Properties p = ECommerceUtil.getInstance().getProperties();
	final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	
	private static Logger logger=Logger.getLogger("AllProductsToEcomInventory");
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		// Fetch the values from the URL(get)
		final String vendorID = request.getParameter("vid");
		final String userID = request.getParameter("ecomID");
		final String pwd = request.getParameter("ecomP");
		
		// for csv
		List<String[]> data = new ArrayList<String[]>();
		CSVWriter writer = null;
		
		if(p.getProperty("b2cCsvPath") != null) {
			String finename = p.getProperty("b2cCsvPath")+"AllInventorySyncLog"+"-"+dt.format(new Date())+".csv";
			// csv file
			final File file = new File(finename);
			
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
				// CSV Headings
				data.add(new String[] {"Item Code", "Quantity", "Time Stamp"});
				
			}
			writer = new CSVWriter(new FileWriter(file.getAbsoluteFile(), true));
			if (!file.exists()) {
				writer.writeAll(data);
			}
		}
		
		// SOAP Connection Initialisation
		final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		final SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		try {
			// Get the PassiveDB Database Connection
			Connection conn = null;
			if(p.getProperty("usePassiveDB").equals("Y")) {
				conn = PassiveDB.getInstance().getConnection();
			} else {
				conn = OBDal.getInstance().getConnection();
			}
			
			final Map<String, Long> items = new HashMap<String, Long>();
			
			// New code
			logger.info("CAR Stock query has started at "+dt.format(new Date().getTime()));
			final Map<String, Long> carStock = carStockByOrg(conn);
			logger.info("CAR Stock query has ended at "+dt.format(new Date().getTime()));
			logger.info("CAR Size"+carStock.size());
			logger.info("CAC Stock query has started at "+dt.format(new Date().getTime()));
			final Map<String, Long> cacStock = cacStockByOrg(conn);
			logger.info("CAC Stock query has ended at "+dt.format(new Date().getTime()));
			logger.info("CAC Size"+cacStock.size());
			Long i = 0L;
			items.putAll(carStock);
			for(String key : cacStock.keySet()) {
				if(items.containsKey(key)) {
					items.put(key, cacStock.get(key) + items.get(key));
					if(p.getProperty("b2cCsvPath") != null) {
						data.add(new String[] {key, ""+items.get(key), dt.format(new Date().getTime())});
					}
	            } else {
	            	items.put(key,cacStock.get(key));
	            	if(p.getProperty("b2cCsvPath") != null) {
	            		data.add(new String[] {key, ""+items.get(key), dt.format(new Date().getTime())});
	            	}
	            }
			}
			logger.info("New Map Size "+items.size());
			for (Map.Entry<String, Long> entry : items.entrySet())
			{
			    // Sends the SOAP request for every 1000 products 
			  	if ((i % 1000) == 0) {
			  		try {
			  			soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
			  		} catch(Exception e) {
						e.printStackTrace();
					}
			  		items.clear();
			  	}
			  	i++;
			}
			if(items.size() < 1000) {
				logger.info("remaining elements "+items.size());
				try {
					soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			if(writer != null) {
				writer.writeAll(data);
			}
			
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', updatedRecords:'"+i+"' }";
			response.setContentType("application/json");
			// Get the printwriter object from response to write the required json object to the output stream      
			PrintWriter out = response.getWriter();
			// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
			out.print(objectToReturn);
			out.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private SOAPMessage updateInventory(String vendorId, String userId, String pwd,	Map items) throws Exception {
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
        	el3.setValue(userId);
        	el4.setValue(pwd);
        	
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("updateMultiplue", "web");
        	SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("in0", "web");
        	Iterator iterator = items.entrySet().iterator();
        	while (iterator.hasNext()) {
        		
        		Map.Entry mapEntry = (Map.Entry) iterator.next();
        		
        		// Inventory Update Multiple
				SOAPElement soapBodyElem2 = soapBodyElem1.addChildElement("ItemcodeInstock", "web");
				SOAPElement soapBodyElem3 = soapBodyElem2.addChildElement("instock", "web");
				SOAPElement soapBodyElem4 = soapBodyElem2.addChildElement("itemCode", "web");
				soapBodyElem3.addTextNode(mapEntry.getValue().toString());
				soapBodyElem4.addTextNode(mapEntry.getKey().toString());
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        logger.info("\n Request SOAP Message = ");
		return soapMessage;
	}
	
	private Map<String, Long> carStockByOrg(Connection conn) {
		// TODO Auto-generated method stub
		
		Map<String, Long> carStocMap = new HashMap<String, Long>();
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where em_sw_iscar = 'Y' and em_idsd_whzone = 'SZ') and isactive='Y') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where em_sw_iscar = 'Y' and em_idsd_whzone = 'SZ') and isactive='Y') and sd.qtyonhand >0 and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name");
			rs = pst.executeQuery();
			while (rs.next()) {
				if(rs.getLong("msdqty") < 0) {
					carStocMap.put(rs.getString("itemcode"), 0L);
				} else {
					carStocMap.put(rs.getString("itemcode"), rs.getLong("msdqty"));
				}
			}
		} catch(Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				if(pst != null && rs != null) {
					pst.close();
					rs.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return carStocMap;
		}
	}
	
	private Map<String, Long> cacStockByOrg(Connection conn) {
		// TODO Auto-generated method stub
		
		Map<String, Long> cacStocMap = new HashMap<String, Long>();
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where em_sw_iscac = 'Y' and em_idsd_whzone = 'SZ') and isactive='Y') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where em_sw_iscac = 'Y' and em_idsd_whzone = 'SZ') and isactive='Y') and sd.qtyonhand >0 and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name");
			rs = pst.executeQuery();
			while (rs.next()) {
				if(rs.getLong("msdqty") < 0) {
					cacStocMap.put(rs.getString("itemcode"), 0L);
				} else {
					cacStocMap.put(rs.getString("itemcode"), rs.getLong("msdqty"));
				}
			}
		} catch(Exception e1) {
			e1.printStackTrace();
		} finally {
			try {
				if(pst != null && rs != null) {
					pst.close();
					rs.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return cacStocMap;
		}
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
