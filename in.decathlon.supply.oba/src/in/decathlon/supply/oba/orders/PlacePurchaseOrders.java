package in.decathlon.supply.oba.orders;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.web.WebService;

import com.oxit.oagis.extended.ApplicationAreaType;
import com.oxit.oagis.extended.CodeType;
import com.oxit.oagis.extended.DateTimeType;
import com.oxit.oagis.extended.IdentifierType;
import com.oxit.oagis.extended.ItemType;
import com.oxit.oagis.extended.PurchaseOrderHeaderType;
import com.oxit.oagis.extended.PurchaseOrderLineType;
import com.oxit.oagis.extended.PurchaseOrderType;
import com.oxit.oagis.extended.QuantityType;
import com.oxit.oagis.extended.SyncPurchaseOrderDataAreaType;
import com.oxit.oagis.extended.SyncPurchaseOrderDocument;
import com.oxit.oagis.extended.SyncPurchaseOrderType;

public class PlacePurchaseOrders implements WebService {

	private static final Logger log = Logger.getLogger(PlacePurchaseOrders.class);
	private static String NAMESPACE = "http://www.oxylane.com/oxit/oagis";
	private static String PREFIX = "oxit";
//	private static String filename = "/home/anvesh/OBA/ORDER/OBA_FLUX_ORDER_INDIA_";
	private static DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	private static DateFormat FORMATTERFILE = new SimpleDateFormat("yyyyMMdd");
	
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		try {
			
			final Map<String, String> configDetails = new HashMap<String, String>();
			// Fetching mail configurations from dsidef_module_config table
			OBContext.setAdminMode();
			OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
			configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.supply.oba"));
			if(configInfoObCriteria.count() > 0) {
				for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
					configDetails.put(config.getKey(), config.getSearchKey());
				}
			}
			OBContext.restorePreviousMode();
			
			final List<Order> poList = fetchPO();
			log.info(poList.size());
			for (Order order : poList) {
				// Declare document
				SyncPurchaseOrderDocument syncPurchaseOrderDocument = SyncPurchaseOrderDocument.Factory.newInstance();

				// SyncPurchaseOrder 
				final SyncPurchaseOrderType syncPurchaseOrder = syncPurchaseOrderDocument.addNewSyncPurchaseOrder();
				
				// Application Area
				final ApplicationAreaType applicationAreaType = syncPurchaseOrder.addNewApplicationArea();
				final DateTimeType creationDateTime = DateTimeType.Factory.newInstance();
				creationDateTime.setStringValue(FORMATTER.format(new Date()));
				applicationAreaType.xsetCreationDateTime(creationDateTime);
				
				// Build your document
				final SyncPurchaseOrderDataAreaType poDatatype = syncPurchaseOrder.addNewDataArea();
				
				final String filename = configDetails.get("orderXMLFileNamePrefix") + order.getDocumentNo()+"_"+FORMATTERFILE.format(new Date())+".xml";
				generatePO(poDatatype, order, configDetails.get("EAN_India"));
				// Then Write it to something...
				try {
					generateOutput(syncPurchaseOrderDocument, filename);
					if(moveToServerThroughFTPS(configDetails.get("serverHost"), configDetails.get("serverUserName"), configDetails.get("serverPassword"), filename)) {
						log.info("The "+filename+" is uploaded successfully!");
					}
				} catch (IOException e) {
					// LOG.error("Cannot write the file", e);
				}
				order.setSWEMSwPostatus("OS");
				
				OBDal.getInstance().save(order);
				OBDal.getInstance().flush();
			}
			OBDal.getInstance().commitAndClose();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private List<Order> fetchPO() {
		
		List<Order> poList = new ArrayList<Order>();
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append("as o WHERE o.sWEMSwPostatus IN ('SO', 'MO', 'VO') AND ");
		whereClause.append("o.documentType.id='C7CD4AC8AC414678A525AB7AE20D718C' AND ");
		whereClause.append("o.businessPartner.id in (select id from BusinessPartner where rCSource='CAN')");
		final OBQuery<Order> orderPO = OBDal.getInstance().createQuery(Order.class, whereClause.toString());
		if(orderPO.count() > 0) {
			poList = orderPO.list();
		}
		log.info("POs Size from server "+poList.size());
		return poList;
	}

	private void generateOutput(SyncPurchaseOrderDocument syncPurchaseOrderDocument, String filename)
			throws IOException {

		log.info("Hello");
		final Map<String, String> prefixes = new HashMap<String, String>();
		prefixes.put(NAMESPACE, PREFIX);
		final XmlOptions xmlOptions = new XmlOptions();
		xmlOptions.setCharacterEncoding("UTF-8");
		xmlOptions.setSaveAggressiveNamespaces();
		xmlOptions.setSaveSuggestedPrefixes(prefixes);

		// Can help to understand the XML but with big xml we should remove pretty print
		xmlOptions.setSavePrettyPrint();
		syncPurchaseOrderDocument.save(new File(filename), xmlOptions);
		
	}

	private void generatePO(SyncPurchaseOrderDataAreaType poDatatype, Order po, String eanIndia) {
		
		try {
			poDatatype.addNewSync();
			
			PurchaseOrderType poXML = poDatatype.addNewPurchaseOrder();
			poXML.setType("ZVAU");
			
			// Header
			final PurchaseOrderHeaderType purchaseOrderHeaderType = poXML.addNewPurchaseOrderHeader();
			
			purchaseOrderHeaderType.addNewDocumentID().addNewID().setStringValue(po.getDocumentNo());
			
			final IdentifierType partIdentifier = purchaseOrderHeaderType.addNewSupplierParty().addNewPartyIDs().addNewID();
			partIdentifier.setSchemeName("EAN13");
			partIdentifier.setStringValue(eanIndia);

			final DateTimeType deleveryDateTime = DateTimeType.Factory.newInstance();
			deleveryDateTime.setStringValue(FORMATTER.format(po.getScheduledDeliveryDate()));
			purchaseOrderHeaderType.xsetRequiredDeliveryDateTime(deleveryDateTime);
			
			final DateTimeType cdd = DateTimeType.Factory.newInstance();
			cdd.setStringValue(FORMATTER.format(po.getSWEMSwExpdeldate()));
			purchaseOrderHeaderType.xsetClosureDateTime(cdd);
			
			int lineNum = 0;
			// Lines
			for (OrderLine poLine : po.getOrderLineList()) {
				
				lineNum++;
				orderline(poXML, lineNum, poLine);
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	private void orderline(PurchaseOrderType poXML, int lineNum,
			OrderLine poLine) {
		final PurchaseOrderLineType purchaseOrderLineType = poXML.addNewPurchaseOrderLine();
		
		purchaseOrderLineType.addNewLineNumber().setStringValue(""+lineNum);
		
		CodeType codeType = purchaseOrderLineType.addNewStatus().addNewCode();
		codeType.setListAgencyName("SAPR3");
		codeType.setStringValue("C");
		
		final ItemType itemType = purchaseOrderLineType.addNewItem();
		
		final IdentifierType itemCodeIDType = itemType.addNewItemID().addNewID();
		itemCodeIDType.setStringValue(poLine.getProduct().getName());
		itemCodeIDType.setSchemeAgencyName("SAPR3");
		
		itemType.addNewPackaging().addNewDescription().setStringValue("CAS");
		
		final QuantityType quantityType = purchaseOrderLineType.addNewQuantity();
		quantityType.setUnitCode("PCE");
		quantityType.setStringValue(poLine.getOrderedQuantity().toString());
	}
	
	private boolean moveToServerThroughFTPS(String host, String userName,
			String password, String filename) throws NoSuchAlgorithmException {

		final FTPSClient ftpsClient = new FTPSClient();
		boolean completed = false;
		try {
			ftpsClient.connect(host);
			ftpsClient.login(userName, password);
			ftpsClient.enterLocalPassiveMode();
			ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);

			// APPROACH #2: uploads second file using an OutputStream
			final File secondLocalFile = new File(filename);
			final String secondRemoteFile = "/ORDER/"+filename.split("/")[filename.split("/").length-1];
			final InputStream inputStream = new FileInputStream(secondLocalFile);

			log.info("Start uploading file");
			final OutputStream outputStream = ftpsClient
					.storeFileStream(secondRemoteFile);
			byte[] bytesIn = new byte[4096];
			int read = 0;

			while ((read = inputStream.read(bytesIn)) != -1) {
				outputStream.write(bytesIn, 0, read);
			}
			outputStream.close();

			completed = ftpsClient.completePendingCommand();

		} catch (IOException e) {
			log.info("Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (ftpsClient.isConnected()) {
					ftpsClient.logout();
					ftpsClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return completed;
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		 throw new UnsupportedOperationException();

	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		 throw new UnsupportedOperationException();

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		 throw new UnsupportedOperationException();

	}
	
}
