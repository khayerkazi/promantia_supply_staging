package in.decathlon.supply.oba.ad_process;

import in.decathlon.supply.oba.util.OrdersProjectUtility;

import java.util.List;
import java.util.Map;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class CANOrderXMLSendingProcess extends DalBaseProcess {

	private ProcessLogger LOGGER;
	
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		LOGGER = bundle.getLogger();
		LOGGER.logln("Starting background process for sending the CAN order XMLs! ");
		
		final OrdersProjectUtility ordersUtility = new OrdersProjectUtility();
		
		// getting all the configurations
		final Map<String, String> configDetails = ordersUtility.getOBAConfigurations();
		
		final List<Order> poList = ordersUtility.getPurchaseOrdersforCAN();
		LOGGER.logln("Total " + poList.size() + " Purchase Order(s) for CAN Warehouse!");
		
		for (Order order : poList) {

			boolean xmlGenerationStatus = false;
			try {
				xmlGenerationStatus = ordersUtility.generatePurchaseOrderXMLInSAPServer(order, configDetails, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(xmlGenerationStatus) {
				LOGGER.logln("Order XML has placed in SAP server!");
				if(order.getSWEMSwPostatus().equals("SO")) {
					
					order.setSWEMSwPostatus("OS");
					
					OBDal.getInstance().save(order);
					OBDal.getInstance().flush();
					LOGGER.logln("Order status changed to Order Sent for Purchase Order of DocNum : " + order.getDocumentNo());
					
				} else if(order.getSWEMSwPostatus().equals("VO")) {

					order.setSWEMSwPostatus("OC");
					
					OBDal.getInstance().save(order);
					OBDal.getInstance().flush();
					LOGGER.logln("Order status changed to Order Cancelled for Purchase Order of DocNum : " + order.getDocumentNo());
				}
			}
		}
		OBDal.getInstance().commitAndClose();
		
	}

}
