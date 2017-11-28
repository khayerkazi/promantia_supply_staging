package in.decathlon.supply.oba.ad_process;

import in.decathlon.supply.oba.bean.POBean;
import in.decathlon.supply.oba.bean.POLineBean;
import in.decathlon.supply.oba.bean.ShipmentBean;
import in.decathlon.supply.oba.data.OBAShipmentDetail;
import in.decathlon.supply.oba.util.OrdersProjectUtility;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.oxit.oagis.extended.ProcessShipmentDocument;
import com.oxit.oagis.extended.UpdatePurchaseOrderDocument;

public class CANResponseXMLParsingProcess extends DalBaseProcess {
	
	private ProcessLogger LOGGER;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		LOGGER = bundle.getLogger();
		LOGGER.logln("Starting background process for parsing the CAN responses! ");
		
		final OrdersProjectUtility ordersUtility = new OrdersProjectUtility();
		
		// getting all the configurations
		final Map<String, String> configDetails = ordersUtility.getOBAConfigurations();
		
		// unzip to ORDER_UNZIP folder
		LOGGER.logln("unzip process starts!");
		if(ordersUtility.unzipFile(configDetails.get("orderFolder"),
				configDetails.get("orderUnzipFolder")))
			LOGGER.logln("unzip process is ends!");
		
		// Looping all the files
		final List<POBean> poBeanList = new ArrayList<POBean>();
		
		for (final File fileEntry : new File(configDetails.get("orderUnzipFolder")).listFiles()) {
			final XmlObject obj = XmlObject.Factory.parse(fileEntry);
			// Confirmation XML reading logic
			if (obj instanceof UpdatePurchaseOrderDocument) {
				
				poBeanList.add(ordersUtility.getPOBean((UpdatePurchaseOrderDocument) obj));
				LOGGER.logln("List Size " + poBeanList.size());
				// deleting successfully read XML file!
				if(fileEntry.getName().contains("UpdatePurchaseOrder")) {
					fileEntry.delete();
				}
			// ShipmentNotice XML reading logic
			} else if (obj instanceof ProcessShipmentDocument) {
			
				final List<ShipmentBean> shipmentBeanList = ordersUtility.getShipmentBeanList((ProcessShipmentDocument) obj);
				LOGGER.logln("Total "+shipmentBeanList.size()+" shipments are received in fileEntry.getName() file!");
				
				// updating the order information in openbravo
				for (ShipmentBean shipmentBean : shipmentBeanList) {
					
					Order po = null;
					Product prod = null;
					LOGGER.logln("doc No "+shipmentBean.getDocumentNo());
					final OBCriteria<Order> orderObCriteria = OBDal.getInstance().createCriteria(Order.class);
					orderObCriteria.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, shipmentBean.getDocumentNo()));
					if(orderObCriteria.count() > 0) {
						LOGGER.logln("order assigned");
						po = orderObCriteria.list().get(0);
					}
					LOGGER.logln("itemcode "+shipmentBean.getItemCode());
					final OBCriteria<Product> productObCriteria = OBDal.getInstance().createCriteria(Product.class);
					productObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, shipmentBean.getItemCode()));
					if(productObCriteria.count() > 0) {
						LOGGER.logln("product assigned");
						prod = productObCriteria.list().get(0);
					}
					
					if(po != null && prod != null) {
						
						po.setSWEMSwPostatus("SH");
						
						OBDal.getInstance().save(po);
						OBDal.getInstance().flush();
						LOGGER.logln("document No : " + po.getDocumentNo());
						
						final OBCriteria<OrderLine> orderLineObCriteria = OBDal.getInstance().createCriteria(OrderLine.class);
						orderLineObCriteria.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, prod));
						orderLineObCriteria.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, po));
						LOGGER.logln("product & order are not null");
						if(orderLineObCriteria.count() > 0) {
							final OrderLine poLine = orderLineObCriteria.list().get(0);
							LOGGER.logln("Order Line info "+poLine.getId());
							LOGGER.logln("Shipped Qty "+shipmentBean.getShippedQty());
							final OBAShipmentDetail obaShipmentDetail = OBProvider.getInstance().get(OBAShipmentDetail.class);
							obaShipmentDetail.setSalesOrderLine(poLine);
							obaShipmentDetail.setShipmentDate(shipmentBean.getShippedDate());
							obaShipmentDetail.setDeliveryDate(shipmentBean.getDeliveryDate());
							obaShipmentDetail.setQtyshipped(shipmentBean.getShippedQty());
							obaShipmentDetail.setPackagingDetails(shipmentBean.getPackagingInfo());
							
							
							OBDal.getInstance().save(obaShipmentDetail);
							OBDal.getInstance().flush();
						}
					}
				}
				OBDal.getInstance().commitAndClose();
				
				// deleting successfully read XML file!
				if(fileEntry.getName().contains("ProcessShipment")) {
					fileEntry.delete();
				}
			}
		}
		
		// updating the Confirmation XML response information in Openbravo
		if(poBeanList.size() > 0) {
			
			for (POBean poBean : poBeanList) {
				final OBCriteria<Order> purchaseOrder = OBDal.getInstance().createCriteria(Order.class);
				purchaseOrder.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, poBean.getDocumentNo()));
				if(purchaseOrder.count() > 0) {
					final Order order = purchaseOrder.list().get(0);
					if(null != poBean.getStatus()) {
						if (poBean.getStatus().equals("OC")
								&& (order.getSWEMSwPostatus().equals("CD") || order
										.getSWEMSwPostatus().equals("SH"))) {
							
							Set<String> currentCancelledICs = new HashSet<String>();
							for (OrderLine o : order.getOrderLineList()) {
								
								boolean iscancelled = false;
								for (OBAShipmentDetail OBAShipmentDetail : o.getObaPoShpmntDetailList()) {
									
									if(OBAShipmentDetail.isCancelled())
										iscancelled = OBAShipmentDetail.isCancelled();
									
								}
								if(iscancelled)
									currentCancelledICs.add(o.getProduct().getName());
							}
							for (POLineBean poLineBean : poBean.getPoLineList()) {
								if(poLineBean.getIsCancelled())
									currentCancelledICs.add(poLineBean.getItemCode());
							}
							int cancelledLinesCount = currentCancelledICs.size();
							if((cancelledLinesCount == order.getOrderLineList().size())) {
								order.setSWEMSwPostatus(poBean.getStatus());
							}
						} else if(poBean.getStatus() == "OC") {
							boolean toBeCancelled = true;
							for (OrderLine o : order.getOrderLineList()) {
								boolean iscancelled = false;
								for (OBAShipmentDetail OBAShipmentDetail : o.getObaPoShpmntDetailList()) {
									if(OBAShipmentDetail.isCancelled())
										iscancelled = OBAShipmentDetail.isCancelled();
								}
								if(!iscancelled)
									toBeCancelled = false;
							}
							if(toBeCancelled) {
								
								order.setSWEMSwPostatus(poBean.getStatus());
							}
						} else if(poBean.getStatus().equals("CD") && !order.getSWEMSwPostatus().equals("OC")) {
							order.setSWEMSwPostatus(poBean.getStatus());
						}
						
					}
					        
					if(null != poBean.getOrderReference())
						order.setOrderReference(poBean.getOrderReference());
					
					OBDal.getInstance().save(order);
					OBDal.getInstance().flush();
					LOGGER.logln("document No : " + poBean.getDocumentNo());
					for (POLineBean poLineBean : poBean.getPoLineList()) {
						Product prod = null;
						final OBCriteria<Product> productObCriteria = OBDal.getInstance().createCriteria(Product.class);
						productObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, poLineBean.getItemCode()));
						if(productObCriteria.count() > 0) {
							prod = productObCriteria.list().get(0);
						}
						if(null != prod) {
							final OBCriteria<OrderLine> orderLineObCriteria = OBDal.getInstance().createCriteria(OrderLine.class);
							orderLineObCriteria.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, prod));
							orderLineObCriteria.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order));
							if(orderLineObCriteria.count() > 0) {
								
								final OrderLine orderLine = orderLineObCriteria.list().get(0);
								
								if(poLineBean.getIsCancelled()) {
									Long cancelledQty = poLineBean.getCancelledQty();
									LOGGER.logln("Cancelled for " + prod.getName() + " of Quantity : " + poLineBean.getCancelledQty());
									if(poLineBean.getRemarks().equals("CancelledByWM")) {
										Long shippedQty = 0L;
										final OBCriteria<OBAShipmentDetail> shipmentObCriteria = OBDal.getInstance().createCriteria(OBAShipmentDetail.class);
										shipmentObCriteria.add(Restrictions.eq(OBAShipmentDetail.PROPERTY_SALESORDERLINE, orderLine));
										if(shipmentObCriteria.count() > 0) {
											cancelledQty = 0L;
											for (OBAShipmentDetail shipmentDetail : shipmentObCriteria.list()) {
												shippedQty += shipmentDetail.getQtyshipped();
											}
										}
										cancelledQty = orderLine.getOrderedQuantity().longValue() - shippedQty;
									}
									final OBAShipmentDetail obaShipmentDetail = OBProvider.getInstance().get(OBAShipmentDetail.class);
									obaShipmentDetail.setOrganization(orderLine.getOrganization());
									obaShipmentDetail.setClient(orderLine.getClient());
									obaShipmentDetail.setCancelledqty(cancelledQty);
									obaShipmentDetail.setCancelled(true);
									obaShipmentDetail.setSalesOrderLine(orderLine);
									obaShipmentDetail.setRemarks(poLineBean.getRemarks());
									
									OBDal.getInstance().save(obaShipmentDetail);
									OBDal.getInstance().flush();
									
								} else if (poLineBean.getToBeCancelled()) {
									LOGGER.logln("Confirmed logic for " + prod.getName() + " of Quantity : " + poLineBean.getConfirmedQty());
									orderLine.setSwConfirmedqty(poLineBean.getConfirmedQty());
									orderLine.setUpdated(new Timestamp(new Date().getTime()));
									LOGGER.logln("docuNo " + orderLine.getSalesOrder().getDocumentNo());
									
									OBDal.getInstance().save(orderLine);
									OBDal.getInstance().flush();
									
									final OBAShipmentDetail obaShipmentDetail = OBProvider.getInstance().get(OBAShipmentDetail.class);
									obaShipmentDetail.setOrganization(orderLine.getOrganization());
									obaShipmentDetail.setClient(orderLine.getClient());
									obaShipmentDetail.setCancelledqty(orderLine.getOrderedQuantity().longValue());
									obaShipmentDetail.setCancelled(true);
									obaShipmentDetail.setSalesOrderLine(orderLine);
									obaShipmentDetail.setRemarks(poLineBean.getRemarks());
									
									OBDal.getInstance().save(obaShipmentDetail);
									OBDal.getInstance().flush();
									
									// send the cancellation XML
									boolean xmlGenerationStatus = false;
									try {
										xmlGenerationStatus = ordersUtility.generatePurchaseOrderXMLInSAPServer(order, configDetails, poLineBean.getItemCode());
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else {
									LOGGER.logln("Confirmed logic for " + prod.getName() + " of Quantity : " + poLineBean.getConfirmedQty());
									orderLine.setSwConfirmedqty(poLineBean.getConfirmedQty());
									orderLine.setUpdated(new Timestamp(new Date().getTime()));
									LOGGER.logln("docuNo " + orderLine.getSalesOrder().getDocumentNo());
									
									OBDal.getInstance().save(orderLine);
									OBDal.getInstance().flush();
									
								}
							}
						}
					}
					OBDal.getInstance().commitAndClose();
				}
			}
		}
		
		// Moving Response XMLs to Archive folder
		for (final File fileEntry : new File(configDetails.get("orderFolder")).listFiles()) {
			if(fileEntry.getName().contains("UpdatePurchaseOrder") || fileEntry.getName().contains("ProcessShipment")) {
				fileEntry.renameTo(new File(configDetails.get("orderArchiveFolder") + fileEntry.getName()));
			}
		}
		
	}

	
}