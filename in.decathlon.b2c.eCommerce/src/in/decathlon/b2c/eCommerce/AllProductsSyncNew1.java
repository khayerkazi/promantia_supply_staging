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
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVWriter;

public class AllProductsSyncNew1 implements WebService {

	final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	final SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	final private static Logger log4j = Logger.getLogger(AllProductsSyncNew1.class);
	
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

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

		// Properties Singleton class instantiation
		Properties p = ECommerceUtil.getInstance().getProperties();
		
		try {
			
			// for csv
			List<String[]> data = new ArrayList<String[]>();
			CSVWriter writer = null;
			String finename = p.getProperty("b2cCsvPath")+"AllInventorySyncLog"+"-"+dt.format(new Date())+".csv";
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
			
			// Get the PassiveDB Database Connection
			Connection conn = null;
			if(p.getProperty("usePassiveDB").equals("Y")) {
				conn = PassiveDB.getInstance().getConnection();
			} else {
				conn = OBDal.getInstance().getConnection();
			}
			PreparedStatement pst = null;
			ResultSet rs = null;
			
			// Ecom Products
			final Map<String, Long> ecomProducts = new HashMap<String, Long>();
			final OBCriteria<Product> ecomProdObCriteria = OBDal.getInstance().createCriteria(Product.class);
			ecomProdObCriteria.add(Restrictions.eq(Product.PROPERTY_ECOMPRODUCT, true));
			if(ecomProdObCriteria.count() > 0) {
				log4j.info("Total Products : "+ecomProdObCriteria.count());
				final List<Product> prodList = ecomProdObCriteria.list();
				for (Product product : prodList) {
					ecomProducts.put(product.getName(), 0L);
				}
			}
			
			// CAR
			final String sqlQueryCAR = "SELECT mp.name as itemcode,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
	                        " FROM m_product mp" +
	                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
	                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
	                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
	                        " WHERE mw.em_idsd_whgroup='CWH' and mw.value in ('RED','NS')" +
	                        " and ml.isactive='Y'" +
	                        " and ml.em_obwhs_type='ST'" +
	                        " group by mp.name;";
			pst = conn.prepareStatement(sqlQueryCAR);
			rs = pst.executeQuery();
			while (rs.next()) {
				
				if(rs.getString("msdqty") != null) {
					if(Long.parseLong(rs.getString("msdqty")) > 0) {
						ecomProducts.put(rs.getString("itemcode"), Long.parseLong(rs.getString("msdqty")));
					}
				}
			}
			//pst = null;
			//rs = null;
			
			
			// CAC-Commented Due to RED-BLUE supply Project
			/*
			final String sqlQueryCAC = "SELECT mp.name as itemcode,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
                            " FROM m_product mp" +
                            " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
                            " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
                            " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
                            " WHERE mw.em_idsd_whgroup='RWH'" +
                            " and ml.isactive='Y'" +
                            " and ml.em_obwhs_type='ST'" +
                            " group by mp.name;";
			pst = conn.prepareStatement(sqlQueryCAC);
			rs = pst.executeQuery();
			while (rs.next()) {
				
				if(rs.getString("msdqty") != null) {
					if(Long.parseLong(rs.getString("msdqty")) > 0) {
						ecomProducts.put(rs.getString("itemcode"), ecomProducts.get(rs.getString("itemcode"))+Long.parseLong(rs.getString("msdqty")));
					}
				}
			}
			*/
			
			conn.close();
			pst.close();
			rs.close();
			
			int loopCounter=0;
			final Map<String, String> items = new HashMap<String, String>();
			for (Map.Entry<String, Long> entry : ecomProducts.entrySet()) {
				loopCounter++;
				items.put(entry.getKey(), entry.getValue().toString());
				if(p.getProperty("b2cCsvPath") != null) {
            		data.add(new String[] {entry.getKey(), entry.getValue().toString(), dt1.format(new Date().getTime())});
            	}
				// Sends the SOAP request for every 1000 products 
			  	if ((loopCounter % 1000) == 0) {
			  		try {
			  			soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
			  		} catch(Exception e) {
						e.printStackTrace();
					}
			  		items.clear();
			  	}
			}
			
			if(items.size() < 1000) {

				try {
					soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			
			/*final String sqlQuery = "select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where (em_sw_iscar = 'Y' or em_sw_iscac = 'Y') and em_idsd_whzone = 'SZ') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.name as itemcode from m_product p, m_storage_detail sd where p.em_ecom_product = 'Y' and sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where (em_sw_iscar = 'Y' or em_sw_iscac = 'Y') and em_idsd_whzone = 'SZ') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name";
			pst = conn.prepareStatement(sqlQuery);
			rs = pst.executeQuery();
			System.out.println("annu");
			int i = 0;
			final Map<String, String> items = new HashMap<String, String>();
			while (rs.next()) {
				
				if(rs.getString("msdqty") != null) {
					if(Long.parseLong(rs.getString("msdqty")) < 0) {
						items.put(rs.getString("itemcode"), "0");
					} else {
//						if(Long.parseLong(rs.getString("msdqty")) == 0) {
//							String EDD = "";
//							long diffDays = 0;
//							   try {
//									pst1 = conn.prepareStatement("select coalesce(to_char(min(cc.em_sw_estshipdate),'dd/mm/yyyy'),'0') as innerEDD from C_Order as cc, M_Product as p,C_OrderLine as cc2 where cc2.M_Product_id=p.M_Product_id and cc2.C_Order_id in (select co.C_Order_id from C_Order as co where co.C_DocType_id in (select C_DocType_id from C_DocType where name like 'Purchase Order%') and co.em_sw_postatus in ('VD','SH','Underway','CD') and co.docstatus!='CO' and not exists (select sio.C_Order_ID from M_InOut as sio where co.C_Order_id=sio.C_Order_ID)) and  cc.C_Order_id=cc2.C_Order_ID and p.isactive='Y' and p.name='"+rs.getString("itemcode")+"'");
//									rs1 = pst1.executeQuery();
//									while (rs1.next()) {
//										EDD = rs1.getString("innerEDD");
//										if(!EDD.equals("0")) {
//											Date date = dt.parse(EDD);
//											Date currentDate = new Date();
//											long diff = date.getTime() - currentDate.getTime();
//											
//											diffDays = diff / (24 * 60 * 60 * 1000);
//										}
//										// Get Inventory ID
//										String inventoryID = "";
//										try {
//											final SOAPMessage findByItemcodeResponse = soapConnection.call(findByItemcode(vendorID, userID, pwd, rs.getString("itemcode")), p.getProperty("serverURL")+"inventoryservice");
//											final SOAPBody findByItemcodeResponseBody = findByItemcodeResponse.getSOAPBody();
//											final NodeList findByItemcodeResponseList = findByItemcodeResponseBody.getElementsByTagName("ns1:Inventory");
//											inventoryID = getNodeValue("inventoryid", findByItemcodeResponseList.item(0).getChildNodes());
//											System.out.println(inventoryID);
//								  		} catch(Exception e) {
//											e.printStackTrace();
//										}
//										if(!"".equals(inventoryID)) {
//											try {
//									  			soapConnection.call(updateInventoryEDD(vendorID, userID, pwd, rs.getString("itemcode"), inventoryID, diffDays+""), p.getProperty("serverURL")+"inventoryservice");
//									  		} catch(Exception e) {
//												e.printStackTrace();
//											}
//										}
//										
//									}
//							   } catch (Exception e) {
//							   	e.printStackTrace();
//								}
//						} else {
							i++;
							items.put(rs.getString("itemcode"), rs.getString("msdqty"));
							if(p.getProperty("b2cCsvPath") != null) {
			            		data.add(new String[] {rs.getString("itemcode"), rs.getString("msdqty"), dt1.format(new Date().getTime())});
			            	}
//						}
					}
				} else {
					items.put(rs.getString("itemcode"), "0");
				}
				// Sends the SOAP request for every 1000 products 
			  	if ((i % 1000) == 0) {
			  		try {
			  			soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
			  		} catch(Exception e) {
						e.printStackTrace();
					}
			  		items.clear();
			  	}
			}
			if(items.size() < 1000) {

				try {
					soapConnection.call(updateInventory(vendorID, userID, pwd, items), p.getProperty("serverURL")+"inventoryservice");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}*/
			data.add(new String[] {"", "", dt1.format(new Date().getTime())});
			if(writer != null) {
				writer.writeAll(data);
			}
			writer.close();
			// Response
			final String objectToReturn = "{ staus: 'ok', code: '200', updatedRecords:'"+loopCounter+"' }";
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
	
	private SOAPMessage findByItemcode(String vendorID, String userID,
			String pwd, String itemCode) throws Exception {
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
        	SOAPElement soapBodyElem = soapBody.addChildElement("findByItemCode", "web");
        	SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("in0", "web");
        	soapBodyElem1.addTextNode(itemCode);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();

        soapMessage.writeTo(System.out);
        System.out.println();
		return soapMessage;
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

        soapMessage.writeTo(System.out);
        System.out.println();
		return soapMessage;
	}
	
	private SOAPMessage updateInventoryEDD(String vendorId, String userId, String pwd,	String itemCode, String inventoryID, String EDD) throws Exception {
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
        	System.out.println("Itemcode "+itemCode);
        	// SOAP Body
        	final SOAPBody soapBody = envelope.getBody();
        	SOAPElement soapBodyElem = soapBody.addChildElement("update", "web");
        	SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("in0", "web");
        	SOAPElement soapBodyElem2 = soapBodyElem1.addChildElement("code", "web");
        	soapBodyElem2.addTextNode(itemCode);
        	SOAPElement soapBodyElem7 = soapBodyElem1.addChildElement("defdelivery", "web");
			soapBodyElem7.addTextNode(EDD);
			SOAPElement soapBodyElem3 = soapBodyElem1.addChildElement("enableEditDeliveryDate", "web");
			soapBodyElem3.addTextNode("true");
			SOAPElement soapBodyElem4 = soapBodyElem1.addChildElement("instock", "web");
			soapBodyElem4.addTextNode("0");
			SOAPElement soapBodyElem5 = soapBodyElem1.addChildElement("inventoryid", "web");
			soapBodyElem5.addTextNode(inventoryID);
			SOAPElement soapBodyElem6 = soapBodyElem1.addChildElement("vendorid", "web");
			soapBodyElem6.addTextNode(vendorId);
			
        } catch (Exception e) {
        	e.printStackTrace();
        }
        soapMessage.saveChanges();
        
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
