//package in.decathlon.supply.oba.orders;
//
//import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
//import in.decathlon.supply.oba.bean.POBean;
//import in.decathlon.supply.oba.bean.POLineBean;
//import in.decathlon.supply.oba.util.OBAUitility;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.log4j.Logger;
//import org.apache.xmlbeans.XmlObject;
//import org.hibernate.criterion.Restrictions;
//import org.openbravo.dal.core.OBContext;
//import org.openbravo.dal.service.OBCriteria;
//import org.openbravo.dal.service.OBDal;
//import org.openbravo.model.common.order.Order;
//import org.openbravo.model.common.order.OrderLine;
//import org.openbravo.service.web.WebService;
//
//import com.oxit.oagis.extended.OxDatedTermInformationType;
//import com.oxit.oagis.extended.PurchaseOrderLineType;
//import com.oxit.oagis.extended.UpdatePurchaseOrderDocument;
//
//public class ReadSUPUpdatePurchaseOrderXML implements WebService {
//	
//	private static final Logger LOG4J = Logger.getLogger(ReadSUPUpdatePurchaseOrderXML.class);
//	
//	@Override
//	public void doGet(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//
//		final Map<String, String> configDetails = new HashMap<String, String>();
//		// Fetching mail configurations from dsidef_module_config table
//		OBContext.setAdminMode();
//		OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
//		configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.supply.oba"));
//		if(configInfoObCriteria.count() > 0) {
//			for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
//				configDetails.put(config.getKey(), config.getSearchKey());
//			}
//		}
//		OBContext.restorePreviousMode();
//		
//		// downloading to local OBA/ORDER/ folder
//		LOG4J.info("downloading the ORDER Response files from server");
//		OBAUitility.downloadFromServerThroughFTPS(
//				configDetails.get("serverHost"),
//				configDetails.get("serverUserName"),
//				configDetails.get("serverPassword"),
//				configDetails.get("orderFolderLocationInLocal"),
//				configDetails.get("orderResponseLocation"));
//		LOG4J.info("download process is done!");
//		
//		// unzip to ORDER_UNZIP folder
//		LOG4J.info("unzip process is starts!");
//		OBAUitility.unzipFile(configDetails.get("orderFolderLocationInLocal"),
//				configDetails.get("orderUnzipFolderLocationInLocal"));
//		LOG4J.info("unzip process is finished!");
//		
//		// logic for reading the XML
//		final List<POBean> poBeanList = new ArrayList<POBean>();
//		for (final File fileEntry : new File(configDetails.get("orderUnzipFolderLocationInLocal")).listFiles()) {
//			final XmlObject obj = XmlObject.Factory.parse(fileEntry);
//			if (obj instanceof UpdatePurchaseOrderDocument) {
//				
//				final POBean poBean = new POBean();
//				LOG4J.info(((UpdatePurchaseOrderDocument) obj).getUpdatePurchaseOrder().getDataArea().getPurchaseOrderArray()[0].getPurchaseOrderHeader().getDocumentID().getID().getStringValue());
//				
//				poBean.setDocumentNo(((UpdatePurchaseOrderDocument) obj).getUpdatePurchaseOrder().getDataArea().getPurchaseOrderArray()[0].getPurchaseOrderHeader().getDocumentID().getID().getStringValue());
//				poBean.setStatus("CD");
//				final List<POLineBean> poLineBeanList = new ArrayList<POLineBean>();
//				
//				final PurchaseOrderLineType[] poLineTypes = ((UpdatePurchaseOrderDocument) obj).getUpdatePurchaseOrder().getDataArea().getPurchaseOrderArray()[0].getPurchaseOrderLineArray();
//				for (PurchaseOrderLineType purchaseOrderLineType : poLineTypes) {
//					
//					final POLineBean poLineBean = new POLineBean();
//					poLineBean.setItemCode(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue());
//					LOG4J.info(purchaseOrderLineType.getItem().getItemIDArray()[0].getID().getStringValue());
//					poLineBean.setOrderedQty(purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue());
//					LOG4J.info(purchaseOrderLineType.getTypedQuantityArray(0).getBigDecimalValue().longValue());
//					Long confirmedQty = 0L;
//					for (OxDatedTermInformationType oxDatedTermInformationType : purchaseOrderLineType.getOxDatedTermInformationArray()) {
//						confirmedQty = confirmedQty + oxDatedTermInformationType.getTypedQuantity().getBigDecimalValue().longValue();
//					}
//					LOG4J.info(confirmedQty);
//					poLineBean.setConfirmedQty(confirmedQty);
//					poLineBeanList.add(poLineBean);
//				}
//				
//				poBean.setPoLineList(poLineBeanList);
//				poBeanList.add(poBean);
//			}
//		}
//		
//		
//		// updating the order information in openbravo
//		for (POBean poBean : poBeanList) {
//			final OBCriteria<Order> purchaseOrder = OBDal.getInstance().createCriteria(Order.class);
//			purchaseOrder.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, poBean.getDocumentNo()));
//			if(purchaseOrder.count() > 0) {
//				final Order order = purchaseOrder.list().get(0);
//				order.setSWEMSwPostatus(poBean.getStatus());
//				
//				OBDal.getInstance().save(order);
//				OBDal.getInstance().flush();
////				OBDal.getInstance().commitAndClose();
//				int i = 0;
//				for (OrderLine orderLine : order.getOrderLineList()) {
//					
//					orderLine.setSwConfirmedqty(poBean.getPoLineList().get(i).getConfirmedQty());
//					OBDal.getInstance().save(orderLine);
//					OBDal.getInstance().flush();
//					i++;
//				}
//				OBDal.getInstance().commitAndClose();
//			}
//		}
//		
//		// moving the document archive drive
//		
//	}
//
//	@Override
//	public void doPost(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void doDelete(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public void doPut(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//		throw new UnsupportedOperationException();
//	}
//
//}
