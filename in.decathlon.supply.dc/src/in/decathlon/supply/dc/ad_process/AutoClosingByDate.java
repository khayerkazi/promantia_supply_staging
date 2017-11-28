package in.decathlon.supply.dc.ad_process;

import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;
import in.decathlon.supply.dc.util.DcClosingMail;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import au.com.bytecode.opencsv.CSVWriter;

public class AutoClosingByDate extends DalBaseProcess {

	static Logger log4j = Logger.getLogger(AutoClosingByDate.class);
	final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	final static Properties p = SuppyDCUtil.getInstance().getProperties();
	List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
	ProcessLogger logger;
    
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		logger = bundle.getLogger();

		OBContext.setAdminMode();
		
		// Fetch all the Organizations 
				
		OBCriteria<Organization> orgcriteria = OBDal.getInstance().createCriteria(Organization.class);
		orgcriteria.add(Restrictions.isNotNull(Organization.PROPERTY_IDSDODRCLSTIME));
		
		if(orgcriteria.count() > 0){
			
			for(Organization orgIterator : orgcriteria.list()){
				
				receiptList.clear();
				
				try{
					logger.log("Organisation Running : " + orgIterator.getName());
					closetruck(orgIterator);
				} catch (Exception e){
					log4j.info("Error fetching organisation " + e.getMessage());logger.log("Error fetching organisation " + e.getMessage());
				}
			}
		}
		
		OBContext.restorePreviousMode();
	}		
		
	private void closetruck(Organization orgIterator) throws Exception {
		
		String query = "select truck.documentNo from DTR_Truck_Reception as truck where truck.documentStatus = 'DR' " +
					   "and organization.id = :orgId group by truck.documentNo, truck.id having (select count(distinct truckline.id) " +
					   "from DTR_Truck_Details as truckline join truckline.goodsShipment as ship where ship.documentStatus = 'CO' " +
					   "and truckline.dTRTruckReception.id = truck.id) > 0"; 
		
		try {
			
			Query fetchTruck = OBDal.getInstance().getSession().createQuery(query);
			fetchTruck.setParameter("orgId", orgIterator.getId());
			List<String> truckList = fetchTruck.list();
     
			  if (null!=truckList && truckList.size() > 0){
				  for(String truckno : truckList){
					  fetchReceipts(truckno, orgIterator);
				  }
			  }
			  
			  if(receiptList.size() > 0){
					sendmail(orgIterator);
				} else {
					log4j.info("No Mail for store - " + orgIterator.getName());logger.log("No Mail for store - " + orgIterator.getName());
				}
			  
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.log("Error fetching Truck " + e.getMessage());
				log4j.info("Error fetching Truck " + e.getMessage());
				throw new OBException("Error fetching Truck " + e.getMessage());
			}
		
	}

	private void fetchReceipts(String trucknumber, Organization orgIterator) {
		// TODO Auto-generated method stub
		
		int increment = 0;
		
		String query = "select ship from DTR_Truck_Details as truckline join truckline.goodsShipment as ship " +
					   "where ship.documentStatus = 'DR' and truckline.dTRTruckReception.documentNo = :truckno ";
		
			try {
			
			Query fetchBoxes = OBDal.getInstance().getSession().createQuery(query);
			fetchBoxes.setParameter("truckno", trucknumber);
			List<ShipmentInOut> boxesList = fetchBoxes.list();
     
			  if (null!=boxesList && boxesList.size() > 0){
				  
				  for(ShipmentInOut boxObj : boxesList){

					  increment++;
						
						try {
						
						receiptList.add(boxObj);
				    	EnhancedProcessGoods epg = new EnhancedProcessGoods();
						epg.processReceipt(boxObj);
						
						} catch (Exception e){
							logger.log("Error in closing receipts : " + e.getMessage());
							log4j.info("Error closing receipts " + e.getMessage());
							continue;
						}
						
						if(increment%10 == 0){
							SessionHandler.getInstance().commitAndStart();
						}
					  
				  }
			  }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				logger.log("Error fetching receipts " + e.getMessage());log4j.info("Error fetching receipts " + e.getMessage());
			}
	}


	private void sendmail(Organization orgIterator) {
		// TODO Auto-generated method stub
		
		String storename = orgIterator.getName();
		String filename = p.getProperty("ClosingcsvPath") + storename + "_" + dt.format(new Date()) + ".csv";
		final File file = new File(filename);
			
		try {
			
			if (!file.exists()) {
				file.createNewFile();
		      }
			
			final CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile()));
			csvwrite(receiptList,storename,file,writer);
			writer.close();
			DcClosingMail.getInstance().sendmailtoall(storename,filename);
			
		} catch(Exception e){
			logger.log("Error while creating CSV " + e.getMessage());log4j.info("Error while creating CSV " + e.getMessage());
		}
	    
		
	}	

	private void csvwrite(List<ShipmentInOut> shipmentInOutList, String stores, File file, CSVWriter writer) throws IOException {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "Store", "Movement Date", "Box Number", "Status" });
		
			for(ShipmentInOut InoutIter : shipmentInOutList){
	    		
    			String shipmentDoc = InoutIter.getDocumentNo().toString();
    			String docstatus = InoutIter.getDocumentStatus().toString();
    			String movementdate = InoutIter.getCreationDate().toString();
    			    			
	          if(docstatus.equals("CO")){
	        	  docstatus = "Completed";
	          } else if(docstatus.equals("DR")){
	        	  docstatus = "Draft";
	          }
	          
	          data.add(new String[] { stores,movementdate,shipmentDoc,docstatus });
	          
        }
			
		writer.writeAll(data);	
		
	}
}
