package in.decathlon.supply.oba.util;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
import in.decathlon.supply.oba.bean.POBean;
import in.decathlon.supply.oba.bean.POLineBean;
import in.decathlon.supply.oba.bean.ShipmentBean;
import in.decathlon.supply.oba.data.OBAShipmentDetail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

import com.oxit.oagis.extended.ApplicationAreaType;
import com.oxit.oagis.extended.CodeType;
import com.oxit.oagis.extended.DateTimeType;
import com.oxit.oagis.extended.IdentifierType;
import com.oxit.oagis.extended.ItemType;
import com.oxit.oagis.extended.OxDatedTermInformationType;
import com.oxit.oagis.extended.ProcessShipmentDocument;
import com.oxit.oagis.extended.PurchaseOrderHeaderType;
import com.oxit.oagis.extended.PurchaseOrderLineType;
import com.oxit.oagis.extended.PurchaseOrderType;
import com.oxit.oagis.extended.QuantityType;
import com.oxit.oagis.extended.ShipmentItemType;
import com.oxit.oagis.extended.SyncPurchaseOrderDataAreaType;
import com.oxit.oagis.extended.SyncPurchaseOrderDocument;
import com.oxit.oagis.extended.SyncPurchaseOrderType;
import com.oxit.oagis.extended.UpdatePurchaseOrderDocument;

public class OrdersProjectUtility {

	private static String NAMESPACE = "http://www.oxylane.com/oxit/oagis";
	private static String PREFIX = "oxit";
	private static final Logger LOG = Logger.getLogger(OrdersProjectUtility.class);
	private static DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	private static DateFormat FORMATTERFILE = new SimpleDateFormat("yyyyMMdd");
	
	// Fetches all the configuration details from dsidef_module_config table
	public Map<String, String> getOBAConfigurations() {
		
		final Map<String, String> configDetails = new HashMap<String, String>();
		OBContext.setAdminMode();
		OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
		configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.supply.oba"));
		if(configInfoObCriteria.count() > 0) {
			for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
				configDetails.put(config.getKey(), config.getSearchKey());
			}
		}
		OBContext.restorePreviousMode();
		return configDetails;
	}
	
	// PO placing XML generation
	
	public List<Order> getPurchaseOrdersforCAN() {
		
		List<Order> poList = new ArrayList<Order>();
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append("as o WHERE o.sWEMSwPostatus IN ('SO', 'VO') AND ");
		whereClause.append("o.documentType.id='C7CD4AC8AC414678A525AB7AE20D718C' AND ");
		whereClause.append("o.businessPartner.id in (select id from BusinessPartner where rCSource='CAN') and o.createdBy!='100' and o.imsapDuplicatesapPo!='Y'");
		final OBQuery<Order> orderPO = OBDal.getInstance().createQuery(Order.class, whereClause.toString());
		if(orderPO.count() > 0) {
			poList = orderPO.list();
		}
		LOG.info("Total " + poList.size() + "Purchase Orders for CAN Warehouse!");
		return poList;
		
	}

	public boolean generatePurchaseOrderXMLInSAPServer(Order order, Map<String, String> configDetails, String itemCode) throws NoSuchAlgorithmException {
		
		try {
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
//		if(order.getSWEMSwPostatus().equals("VO")) 
		LOG.info(order.getSWEMSwPostatus());
		generatePO(poDatatype, order, configDetails.get("EAN_India"), order.getSWEMSwPostatus(), itemCode);
		// Then Write it to something...
		
			generateOutput(syncPurchaseOrderDocument, filename);
			if(moveToServerThroughFTPS(configDetails.get("serverHost"), configDetails.get("serverUserName"), configDetails.get("serverPassword"), filename)) {
				LOG.info("The "+filename+" is uploaded successfully!");
			} else {
				LOG.info("The "+filename+" is not uploaded successfully!");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private void generatePO(SyncPurchaseOrderDataAreaType poDatatype, Order po,
			String eanIndia, String poStatus, String itemCode) {
		LOG.info(poStatus);
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
			LOG.info("@lines");
			
			if(null != itemCode) {
				for (OrderLine poLine : po.getOrderLineList()) {
					if (poLine.getProduct().getName().equals(itemCode)) {
						lineNum++;
						orderline(poXML, lineNum, poLine, poStatus, itemCode);
					}
				}
			} else {
				for (OrderLine poLine : po.getOrderLineList()) {
					LOG.info("@lines loop");
					lineNum++;
					orderline(poXML, lineNum, poLine, poStatus, null);
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void orderline(PurchaseOrderType poXML, int lineNum,
			OrderLine poLine, String poStatus, String itemCode) {
		LOG.info("@lines method");
		final PurchaseOrderLineType purchaseOrderLineType = poXML.addNewPurchaseOrderLine();
		
		purchaseOrderLineType.addNewLineNumber().setStringValue(""+lineNum);
		
		CodeType codeType = purchaseOrderLineType.addNewStatus().addNewCode();
		codeType.setListAgencyName("SAPR3");
		if (poStatus.equals("VO")) {
			LOG.info("@lines method VO");
			codeType.setStringValue("S");
			
			final ItemType itemType = purchaseOrderLineType.addNewItem();
			
			final IdentifierType itemCodeIDType = itemType.addNewItemID().addNewID();
			itemCodeIDType.setStringValue(poLine.getProduct().getName());
			itemCodeIDType.setSchemeAgencyName("SAPR3");
			
			Long cancelledQty = poLine.getOrderedQuantity().longValue();
			// fetching unshippedQty
			final OBCriteria<OBAShipmentDetail> shipmentDetailObCriteria = OBDal.getInstance().createCriteria(OBAShipmentDetail.class);
			shipmentDetailObCriteria.add(Restrictions.eq(OBAShipmentDetail.PROPERTY_SALESORDERLINE, poLine));
			shipmentDetailObCriteria.add(Restrictions.eq(OBAShipmentDetail.PROPERTY_ISCANCELLED, false));
			if(shipmentDetailObCriteria.count() > 0) {
				for (OBAShipmentDetail shipmentDetail : shipmentDetailObCriteria.list()) {
					
					cancelledQty -= shipmentDetail.getQtyshipped();
					
				}
				LOG.info("cancelledQty "+cancelledQty);
			}
			
			itemType.addNewPackaging().addNewDescription().setStringValue("CAS");
			
			final QuantityType quantityType = purchaseOrderLineType.addNewQuantity();
			quantityType.setUnitCode("PCE");
			quantityType.setStringValue(cancelledQty.toString());
			
		} else if (null != itemCode) {
			LOG.info("@lines method UserCancellation");
			codeType.setStringValue("S");
			
			final ItemType itemType = purchaseOrderLineType.addNewItem();
			
			final IdentifierType itemCodeIDType = itemType.addNewItemID().addNewID();
			itemCodeIDType.setStringValue(poLine.getProduct().getName());
			itemCodeIDType.setSchemeAgencyName("SAPR3");
			
			Long cancelledQty = poLine.getOrderedQuantity().longValue();
			
			itemType.addNewPackaging().addNewDescription().setStringValue("CAS");
			
			final QuantityType quantityType = purchaseOrderLineType.addNewQuantity();
			quantityType.setUnitCode("PCE");
			quantityType.setStringValue(cancelledQty.toString());
		} else if (poStatus.equals("SO")) {
			
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
		
	}
	
	private void generateOutput(SyncPurchaseOrderDocument syncPurchaseOrderDocument, String filename)
			throws IOException {

		LOG.info("Hello");
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

			LOG.info("Start uploading file");
			final OutputStream outputStream = ftpsClient
					.storeFileStream(secondRemoteFile);
			byte[] bytesIn = new byte[4096];
			int read = 0;

			while ((read = inputStream.read(bytesIn)) != -1) {
				outputStream.write(bytesIn, 0, read);
			}
			outputStream.close();

			completed = ftpsClient.completePendingCommand();

		} catch (Exception e) {
			LOG.info("Error: " + e.getMessage());
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
	
	// for un-zipping the files
	public boolean unzipFile(String sourcePath, String destinationPath){
        
		boolean unzipped = false;
        FileInputStream fis = null;
        ZipInputStream zipIs = null;
        ZipEntry zEntry = null;
        try {
        	
        	for (final File fileEntry : new File(sourcePath).listFiles()) {
        		fis = new FileInputStream(sourcePath + fileEntry.getName());
                zipIs = new ZipInputStream(new BufferedInputStream(fis));
                while((zEntry = zipIs.getNextEntry()) != null){
                    try{
                        byte[] tmp = new byte[4*1024];
                        FileOutputStream fos = null;
                        LOG.info("Extracting file to "+destinationPath);
                        fos = new FileOutputStream(destinationPath + fileEntry.getName().replace(".zip", ""));
                        int size = 0;
                        while((size = zipIs.read(tmp)) != -1){
                            fos.write(tmp, 0 , size);
                        }
                        fos.flush();
                        fos.close();
                    } catch(Exception ex){
                         
                    }
                }
                zipIs.close();
        	}
            
            unzipped = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return unzipped;
	}
	
	// Reading confirmation XML
	
	public POBean getPOBean(UpdatePurchaseOrderDocument obj) {
		
		final POBean poBean = new POBean();
		LOG.info(obj.getUpdatePurchaseOrder().getDataArea().getPurchaseOrderArray()[0].getPurchaseOrderHeader().getDocumentID().getID().getStringValue());
		
		poBean.setDocumentNo(obj.getUpdatePurchaseOrder().getDataArea().getPurchaseOrderArray()[0].getPurchaseOrderHeader().getDocumentID().getID().getStringValue());
		
		final List<POLineBean> poLineBeanList = new ArrayList<POLineBean>();
		
		final PurchaseOrderLineType[] poLineTypes = obj.getUpdatePurchaseOrder().getDataArea().getPurchaseOrderArray()[0].getPurchaseOrderLineArray();
		for (PurchaseOrderLineType purchaseOrderLineType : poLineTypes) {
			poBean.setOrderReference(purchaseOrderLineType.getDocumentReferenceArray()[0].getAlternateDocumentIDArray()[0].getID().getStringValue());
			if(null != purchaseOrderLineType.getStatusArray(0).getCode()) {
				// Cancellation logic
				if(purchaseOrderLineType.getStatusArray(0).getCode().getStringValue().equals("CancelledBySAP")) {
					
					final POLineBean poLineBean = new POLineBean();
					// Cancelled By SAP logic
					Long orderedQty = 0L;
					Long confirmedQty = 0L;
					if(purchaseOrderLineType.getTypedQuantityArray(0).getType().equals("ItemQuantity"))
						orderedQty = purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue();
					for (OxDatedTermInformationType oxDatedTermInformationType : purchaseOrderLineType.getOxDatedTermInformationArray()) {
						confirmedQty = confirmedQty + oxDatedTermInformationType.getTypedQuantity().getBigDecimalValue().longValue();
					}
					final Long cancelledQty = orderedQty - confirmedQty;
					if(cancelledQty == orderedQty) {
					  poBean.setStatus("OC");
					}
					poLineBean.setIsCancelled(true);
					poLineBean.setToBeCancelled(false);
					poLineBean.setCancelledQty(cancelledQty);
					poLineBean.setItemCode(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue().replaceFirst("^0+(?!$)", ""));
					poLineBean.setRemarks("CancelledBySAP");
					poLineBean.setOrderReferenceId(purchaseOrderLineType.getDocumentReferenceArray()[0].getAlternateDocumentIDArray()[0].getID().getStringValue());
					LOG.info("Cancelled Qty " + cancelledQty);
					poLineBeanList.add(poLineBean);
					
				} else if(purchaseOrderLineType.getStatusArray(0).getCode().getStringValue().equals("CancelledByWM")) {
					
					final POLineBean poLineBean = new POLineBean();
					// Cancelled By Warehouse logic
					Long orderedQty = 0L;
					Long confirmedQty = 0L;
					if(purchaseOrderLineType.getTypedQuantityArray(0).getType().equals("ItemQuantity"))
						orderedQty = purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue();
					for (OxDatedTermInformationType oxDatedTermInformationType : purchaseOrderLineType.getOxDatedTermInformationArray()) {
						confirmedQty = confirmedQty + oxDatedTermInformationType.getTypedQuantity().getBigDecimalValue().longValue();
					}
					final Long cancelledQty = orderedQty - confirmedQty;
					if(cancelledQty == orderedQty) {
                                          poBean.setStatus("OC");
                                        }
					poLineBean.setIsCancelled(true);
					poLineBean.setToBeCancelled(false);
					poLineBean.setCancelledQty(cancelledQty);
					poLineBean.setItemCode(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue().replaceFirst("^0+(?!$)", ""));
					poLineBean.setRemarks("CancelledByWM");
					poLineBean.setOrderReferenceId(purchaseOrderLineType.getDocumentReferenceArray()[0].getAlternateDocumentIDArray()[0].getID().getStringValue());
					LOG.info("Cancelled Qty " + cancelledQty);
					poLineBeanList.add(poLineBean);
					
				} else if(purchaseOrderLineType.getStatusArray(0).getCode().getStringValue().equals("Archived")) {
					
					// Archived order logic
					final POLineBean poLineBean = new POLineBean();
					// Cancelled By SAP logic
					Long orderedQty = 0L;
					Long confirmedQty = 0L;
					if(purchaseOrderLineType.getTypedQuantityArray(0).getType().equals("ItemQuantity"))
						orderedQty = purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue();
					for (OxDatedTermInformationType oxDatedTermInformationType : purchaseOrderLineType.getOxDatedTermInformationArray()) {
						confirmedQty = confirmedQty + oxDatedTermInformationType.getTypedQuantity().getBigDecimalValue().longValue();
					}
					final Long cancelledQty = orderedQty - confirmedQty;
					if(cancelledQty == orderedQty) {
                                          poBean.setStatus("OC");
                                        }
					poLineBean.setIsCancelled(true);
					poLineBean.setToBeCancelled(false);
					poLineBean.setCancelledQty(cancelledQty);
					poLineBean.setItemCode(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue().replaceFirst("^0+(?!$)", ""));
					poLineBean.setRemarks("Archived");
					poLineBean.setOrderReferenceId(purchaseOrderLineType.getDocumentReferenceArray()[0].getAlternateDocumentIDArray()[0].getID().getStringValue());
					LOG.info("Cancelled Qty " + cancelledQty);
					poLineBeanList.add(poLineBean);
				}
			} else {
			        poBean.setStatus("CD");
				final POLineBean poLineBean = new POLineBean();
				poLineBean.setItemCode(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue().replaceFirst("^0+(?!$)", ""));
				LOG.info(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue());
				poLineBean.setOrderedQty(purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue());
				LOG.info(purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue());
				Long confirmedQty = 0L;
				for (OxDatedTermInformationType oxDatedTermInformationType : purchaseOrderLineType.getOxDatedTermInformationArray()) {
					confirmedQty = confirmedQty + oxDatedTermInformationType.getTypedQuantity().getBigDecimalValue().longValue();
				}
				
				LOG.info("Confirmed Qty for "+purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue().replaceFirst("^0+(?!$)", "")+" is "+confirmedQty);
				if(confirmedQty == 0) {
					poLineBean.setConfirmedQty(confirmedQty);
					poLineBean.setIsCancelled(false);
					poLineBean.setToBeCancelled(true);
					poLineBean.setRemarks("CancelledByUser");
					poLineBean.setOrderReferenceId(purchaseOrderLineType.getDocumentReferenceArray()[0].getAlternateDocumentIDArray()[0].getID().getStringValue());
				} else {
					poLineBean.setConfirmedQty(confirmedQty);
					poLineBean.setIsCancelled(false);
					poLineBean.setToBeCancelled(false);
					poLineBean.setOrderReferenceId(purchaseOrderLineType.getDocumentReferenceArray()[0].getAlternateDocumentIDArray()[0].getID().getStringValue());
				}
				
				poLineBeanList.add(poLineBean);
			}
			
		}
		LOG.info("LineList size "+poLineBeanList.size());
		poBean.setPoLineList(poLineBeanList);
		
		return poBean;
	}
	
	// Reading Shipment XML
	
	public List<ShipmentBean> getShipmentBeanList(ProcessShipmentDocument obj) {
		
		final List<ShipmentBean> shipmentBeanList = new ArrayList<ShipmentBean>();
		
		final ShipmentItemType[] shipmentItemTypes = obj.getProcessShipment().getDataArea().getShipmentArray(0).getShipmentItemArray();
		
		for (ShipmentItemType shipmentItemType : shipmentItemTypes) {
			
			final ShipmentBean shipmentBean = new ShipmentBean();
			shipmentBean.setShippedDate(obj.getProcessShipment().getDataArea().getShipmentArray(0).getShipmentHeader().getActualShipDateTime().getTime());
			shipmentBean.setDeliveryDate(obj.getProcessShipment().getDataArea().getShipmentArray(0).getShipmentHeader().getActualDeliveryDateTime().getTime());
			shipmentBean.setItemCode(shipmentItemType.getItemIDArray(0).getID().getStringValue().replaceFirst("^0+(?!$)", ""));
			shipmentBean.setShippedQty(shipmentItemType.getShippedQuantity().getBigDecimalValue().longValue());
			LOG.info("Doument No "+shipmentItemType.getPurchaseOrderReferenceArray(0).getDocumentID().getID().getStringValue());
			shipmentBean.setDocumentNo(shipmentItemType.getPurchaseOrderReferenceArray(0).getDocumentID().getID().getStringValue());
			shipmentBean.setPackagingInfo("Parcel ID:"
					+ shipmentItemType.getPackagingArray(0).getID()
							.getStringValue());
			
			shipmentBeanList.add(shipmentBean);
		}
		return shipmentBeanList;
	}
	
	
	// Archiving responses
	
	
}
