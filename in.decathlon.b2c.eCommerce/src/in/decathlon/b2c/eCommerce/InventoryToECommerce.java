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

public class InventoryToECommerce implements WebService {
	
	private static Logger log = Logger.getLogger(InventoryToECommerce.class);
	final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	final SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
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
		final Connection conn1 = OBDal.getInstance().getConnection();
		Connection conn = null;
		if(p.getProperty("usePassiveDB").equals("Y")) {
			conn = PassiveDB.getInstance().getConnection();
		} else {
			conn = OBDal.getInstance().getConnection();
		}
		
		PreparedStatement pst = null;PreparedStatement pst2 = null;PreparedStatement pst3 = null;
		ResultSet rs = null;
		PreparedStatement pst1 = null;
		ResultSet rs1 = null;ResultSet rs2 = null;ResultSet rs3 = null;
		try {
			
			// for csv
			List<String[]> data = new ArrayList<String[]>();
			CSVWriter writer = null;
			String finename = p.getProperty("b2cCsvPath")+"InventorySyncLog"+"-"+dt.format(new Date())+".csv";
			// csv file
			final File file = new File(finename);
			if(p.getProperty("b2cCsvPath") != null) {
				
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
			
			String date="";
			final String query1="select * from ecom_ws_status where type_of_sync='wsinv' and processed='Y' order by created desc limit 1";
			pst1 = conn1.prepareStatement(query1);
			rs1 = pst1.executeQuery();
			while (rs1.next()) {
				date = rs1.getTimestamp("end_time").toString();
			}
			
			// Retrieving updated products
			final String query2="select distinct m_product_id from m_storage_detail where updated between '"+date+"' and now() and m_locator_id in (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name ilike 'Saleable%' and ad_org_id='603C6A266B4C40BCAD87C5C43DDF53EE') and isactive='Y') union select distinct m_product_id from m_reservation where m_reservation_id in (select m_reservation_id from m_reservation_stock where updated between '"+date+"' and now());";
			pst2 = conn.prepareStatement(query2);
			rs2 = pst2.executeQuery();
			int i = 0;
			
			final Map<String, String> items = new HashMap<String, String>();
			while (rs2.next()) {
				
				Long stock = 0L; 
				
				pst3 = conn.prepareStatement("select name from m_product where m_product_id='"+rs2.getString("m_product_id")+"'");
				rs3 = pst3.executeQuery();
				String itemcode="";
				while (rs3.next()) {
					itemcode = rs3.getString("name");
				}
				// quantity in hand calculation CAR
				final String sqlCAR = "SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
		                        " FROM m_product mp" +
		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
		                        " WHERE mw.em_idsd_whgroup='CWH' and mw.value in ('RED','NS')" +
		                        " and ml.isactive='Y'" +
		                        " and ml.em_obwhs_type='ST'" +
		                        " and mp.m_product_id='"+rs2.getString("m_product_id")+"'" +
		                        " group by mp.m_product_id;";
				
				// quantity in hand calculation CAR
				/*
				final String sqlCAC = "SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
		                        " FROM m_product mp" +
		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
		                        " WHERE mw.em_idsd_whgroup='RWH'" +
		                        " and ml.isactive='Y'" +
		                        " and ml.em_obwhs_type='ST'" +
		                        " and mp.name='"+rs2.getString("m_product_id")+"'" +
		                        " group by mp.name;";
				*/
				pst = conn.prepareStatement(sqlCAR);
				rs = pst.executeQuery();
				int carCounter=0;
				
				while (rs.next()) {
					carCounter++;
					if(rs.getString("msdqty") != null && !rs.getString("msdqty").equalsIgnoreCase("null")) {
						if(Long.parseLong(rs.getString("msdqty")) < 0) {
							stock = 0L;
//							items.put(itemcode, "0");
						} else {
							stock = Long.parseLong(rs.getString("msdqty"));
//							items.put(itemcode, rs.getString("msdqty"));
						}
					} else {
						stock = 0L;
//						items.put(itemcode, "0");
					}
				}
				if(carCounter == 0) {
					stock = 0L;
//					items.put(itemcode, "0");
				}

				pst = null;
				rs = null;
				
				//CAC-COMMENTED Due to RED-BLUE Supply Project
				/*
				pst = conn.prepareStatement(sqlCAC);
				rs = pst.executeQuery();
					
				while (rs.next()) {
						
					if(rs.getString("msdqty") != null && !rs.getString("msdqty").equalsIgnoreCase("null")) {
						if(Long.parseLong(rs.getString("msdqty")) < 0) {
							stock = stock + 0;
//							items.put(itemcode, ""+(Long.parseLong(items.get(rs.getString("itemcode"))) + 0));
						} else {
							stock = stock + Long.parseLong(rs.getString("msdqty"));
//							items.put(itemcode, ""+(Long.parseLong(items.get(rs.getString("itemcode"))) + Long.parseLong(rs.getString("msdqty"))));
						}
					} else {
						stock = stock;
//						items.put(itemcode, items.get(rs.getString("itemcode")));
					}
				}
				*/
				items.put(itemcode, stock.toString());
				if(p.getProperty("b2cCsvPath") != null) {
				    data.add(new String[] {itemcode, stock.toString(), dt1.format(new Date().getTime())});
	        	}
				
				// Sends the SOAP request for every 1000 products 
			  	if ((i % 1000) == 0) {
					try{
						soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
					}catch(Exception e){
						e.printStackTrace();
					}
			  		items.clear();
			  	}
			  	i++;
			}
        	if(items.size() < 1000) {
				try{
					soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
				}catch(Exception e){
						e.printStackTrace();
				}
        	}
        	
        	data.add(new String[] {"", "", dt1.format(new Date().getTime())});
			if(writer != null) {
				writer.writeAll(data);
			}
			writer.close();
			
        	// Insert a Record in ecom_ws_status
			final String insertQuery="INSERT INTO ecom_ws_status (ecom_ws_status_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, type_of_sync, start_time, end_time, processing, processed, remarks, no_of_records) VALUES (GET_UUID(), '187D8FC945A5481CB41B3EE767F80DBB', '057FF7ABBAAA43ECA533ACA272264A1A', 'Y', '100', '100', 'wsinv', now(), now(), 'N', 'Y', '', '"+i+"');";
			pst = conn1.prepareStatement(insertQuery);
			pst.executeUpdate();
			
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', updatedRecords:'"+i+"' }";
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
			conn1.close();
			if(pst != null && rs != null) {
				pst.close();
				rs.close();
			}
			if(pst3 != null && rs3 != null) {
				pst3.close();
				rs3.close();
			}
			pst1.close();
			rs1.close();
			pst2.close();
			rs2.close();
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
