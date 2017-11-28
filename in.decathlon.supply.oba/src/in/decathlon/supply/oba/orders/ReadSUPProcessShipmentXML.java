//package in.decathlon.supply.oba.orders;
//
//import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
//import in.decathlon.supply.oba.bean.ShipmentBean;
//import in.decathlon.supply.oba.data.OBAShipmentDetail;
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
//import org.openbravo.base.provider.OBProvider;
//import org.openbravo.dal.core.OBContext;
//import org.openbravo.dal.service.OBCriteria;
//import org.openbravo.dal.service.OBDal;
//import org.openbravo.model.common.order.Order;
//import org.openbravo.model.common.order.OrderLine;
//import org.openbravo.model.common.plm.Product;
//import org.openbravo.service.web.WebService;
//
//import com.oxit.oagis.extended.ProcessShipmentDocument;
//import com.oxit.oagis.extended.ShipmentItemType;
//
//public class ReadSUPProcessShipmentXML implements WebService {
//	
//	private static final Logger LOG4J = Logger.getLogger(ReadSUPProcessShipmentXML.class);
//	
//	@Override
//	public void doGet(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//
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
////		LOG4J.info("downloading the ORDER Response files from server");
////		OBAUitility.downloadFromServerThroughFTPS(
////				configDetails.get("serverHost"),
////				configDetails.get("serverUserName"),
////				configDetails.get("serverPassword"),
////				configDetails.get("orderFolderLocationInLocal"),
////				configDetails.get("orderResponseLocation"));
////		LOG4J.info("download process is done!");
//				
//		// unzip to ORDER_UNZIP folder
//		LOG4J.info("unzip process is starts!");
//		OBAUitility.unzipFile(configDetails.get("orderFolderLocationInLocal"),
//				configDetails.get("orderUnzipFolderLocationInLocal"));
//		LOG4J.info("unzip process is finished!");
//		
//		// logic for reading the XML
//		final List<ShipmentBean> shipmentBeanList = new ArrayList<ShipmentBean>();
//		for (final File fileEntry : new File(configDetails.get("orderUnzipFolderLocationInLocal")).listFiles()) {
//			final XmlObject obj = XmlObject.Factory.parse(fileEntry);
//			if (obj instanceof ProcessShipmentDocument) {
//				
//				final ShipmentItemType[] shipmentItemTypes = ((ProcessShipmentDocument) obj).getProcessShipment().getDataArea().getShipmentArray(0).getShipmentItemArray();
//				
//				for (ShipmentItemType shipmentItemType : shipmentItemTypes) {
//					
//					final ShipmentBean shipmentBean = new ShipmentBean();
//					shipmentBean.setShippedDate(((ProcessShipmentDocument) obj).getProcessShipment().getDataArea().getShipmentArray(0).getShipmentHeader().getActualShipDateTime().getTime());
//					shipmentBean.setDeliveryDate(((ProcessShipmentDocument) obj).getProcessShipment().getDataArea().getShipmentArray(0).getShipmentHeader().getActualDeliveryDateTime().getTime());
//					shipmentBean.setItemCode(shipmentItemType.getItemIDArray(0).getID().getStringValue().replaceFirst("^0+(?!$)", ""));
//					shipmentBean.setShippedQty(shipmentItemType.getShippedQuantity().getBigDecimalValue().longValue());
//					LOG4J.info("Doument No "+shipmentItemType.getPurchaseOrderReferenceArray(0).getDocumentID().getID().getStringValue());
//					shipmentBean.setDocumentNo(shipmentItemType.getPurchaseOrderReferenceArray(0).getDocumentID().getID().getStringValue());
//					shipmentBean.setPackagingInfo("Parcel ID:"
//							+ shipmentItemType.getPackagingArray(0).getID()
//									.getStringValue()
//							+ ", Width:"
//							+ shipmentItemType.getPackagingArray(2)
//									.getDimensions().getWidthMeasure()
//							+ " Meters, Length:"
//							+ shipmentItemType.getPackagingArray(2)
//									.getDimensions().getLengthMeasure()
//							+ " Meters, Height:"
//							+ shipmentItemType.getPackagingArray(2)
//									.getDimensions().getHeightMeasure()
//							+ "meters, Gross Weight:"
//							+ shipmentItemType.getPackagingArray(2)
//									.getDimensions().getGrossWeightMeasure());
//					
//					shipmentBeanList.add(shipmentBean);
//				}
//			}
//		}
//		LOG4J.info("Shipment list size "+shipmentBeanList.size());
//		// updating the order information in openbravo
//		for (ShipmentBean shipmentBean : shipmentBeanList) {
//			
//			Order po = null;
//			Product prod = null;
//			LOG4J.info("doc No "+shipmentBean.getDocumentNo());
//			final OBCriteria<Order> orderObCriteria = OBDal.getInstance().createCriteria(Order.class);
//			orderObCriteria.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, shipmentBean.getDocumentNo()));
//			if(orderObCriteria.count() > 0) {
//				LOG4J.info("order assigned");
//				po = orderObCriteria.list().get(0);
//			}
//			LOG4J.info("itemcode "+shipmentBean.getItemCode());
//			final OBCriteria<Product> productObCriteria = OBDal.getInstance().createCriteria(Product.class);
//			productObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, shipmentBean.getItemCode()));
//			if(productObCriteria.count() > 0) {
//				LOG4J.info("product assigned");
//				prod = productObCriteria.list().get(0);
//			}
//			
//			if(po != null && prod != null) {
//				final OBCriteria<OrderLine> orderLineObCriteria = OBDal.getInstance().createCriteria(OrderLine.class);
//				orderLineObCriteria.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, prod));
//				orderLineObCriteria.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, po));
//				LOG4J.info("product & order are not null");
//				if(orderLineObCriteria.count() > 0) {
//					final OrderLine poLine = orderLineObCriteria.list().get(0);
//					LOG4J.info("Order Line info "+poLine.getId());
//					LOG4J.info("Shipped Qty "+shipmentBean.getShippedQty());
//					final OBAShipmentDetail obaShipmentDetail = OBProvider.getInstance().get(OBAShipmentDetail.class);
//					obaShipmentDetail.setSalesOrderLine(poLine);
//					obaShipmentDetail.setShipmentDate(shipmentBean.getShippedDate());
//					obaShipmentDetail.setDeliveryDate(shipmentBean.getDeliveryDate());
//					obaShipmentDetail.setQtyshipped(shipmentBean.getShippedQty());
//					obaShipmentDetail.setPackagingDetails(shipmentBean.getPackagingInfo());
//					
//					
//					OBDal.getInstance().save(obaShipmentDetail);
//					OBDal.getInstance().flush();
//				}
//			}
//			
//			
//		}
//		OBDal.getInstance().commitAndClose();
//		
//		// moving the document archive drive
//		
//	}
//
//	@Override
//	public void doPost(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//		throw new UnsupportedOperationException();
//
//	}
//
//	@Override
//	public void doDelete(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//		throw new UnsupportedOperationException();
//
//	}
//
//	@Override
//	public void doPut(String path, HttpServletRequest request,
//			HttpServletResponse response) throws Exception {
//		throw new UnsupportedOperationException();
//
//	}
//	
//	
//
//}
