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
import org.hibernate.criterion.Restrictions;
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

public class AutoClosingByLead extends DalBaseProcess {

	static Logger log4j = Logger.getLogger(AutoClosingByLead.class);
	final static Properties p = SuppyDCUtil.getInstance().getProperties();
    final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
    List<ShipmentInOut> receiptList = new ArrayList<ShipmentInOut>();
	ProcessLogger logger;
	
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		
		logger = bundle.getLogger();
							
		OBContext.setAdminMode();
		
	     // Iterate through all Organizations
		
		OBCriteria<Organization> criteriaorg = OBDal.getInstance().createCriteria(Organization.class);
		criteriaorg.add(Restrictions.isNotNull(Organization.PROPERTY_IDSDODRCLSTIME));
						 
		if(criteriaorg.count() > 0){
			
			for(Organization orgIterator : criteriaorg.list()){
				
				receiptList.clear();
				
				try{
					
					logger.log("Organisation Running : " + orgIterator.getName());
					closebylead(orgIterator);
					
				} catch (Exception e){
					log4j.info("Error fetching organisation " + e.getMessage());logger.log("Error fetching organisation " + e.getMessage());
				}
			}
		} else {
			logger.log("No Organisations configured to close Receipts.");
		}
		
		OBContext.restorePreviousMode();
}

	private void closebylead(Organization orgIterator) {
		// TODO Auto-generated method stub
		
		long leadtime = orgIterator.getIdsdOdrclstime();
		long var = new Date().getTime() - (long)((leadtime)*1000*60*60*24);
		java.sql.Date dateordered = new java.sql.Date(var);
		int increment = 0;
		
		
		OBCriteria<ShipmentInOut> criteriainout = OBDal.getInstance().createCriteria(ShipmentInOut.class);
		criteriainout.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "DR"));
		criteriainout.add(Restrictions.eq(ShipmentInOut.PROPERTY_ORGANIZATION, orgIterator));
		criteriainout.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, false));
		criteriainout.add(Restrictions.eq(ShipmentInOut.PROPERTY_SWMOVEMENT, "SRQ"));
		criteriainout.add(Restrictions.le(ShipmentInOut.PROPERTY_CREATIONDATE, dateordered));
		
		logger.log(" Organisation  " + orgIterator.getName() + " has " + criteriainout.count() + " to close.");
		
		if(criteriainout.count() > 0){
			 
			for (ShipmentInOut InoutIterater : criteriainout.list()) {
				
				increment++;
				
				try {
					
				receiptList.add(InoutIterater);
		    	EnhancedProcessGoods epg = new EnhancedProcessGoods();
				epg.processReceipt(InoutIterater);
				
				} catch (Exception e){
					logger.log("Error in closing receipts : " + e.getMessage());
					log4j.info("Error in closing receipts " + e.getMessage());
					continue;
				}
				
				if(increment%10 == 0){
					SessionHandler.getInstance().commitAndStart();
				}
			}
		}
		else logger.log("No DCs' to close for " + orgIterator.getName() + "Store.");
		
		if(receiptList.size() > 0){
			sendmail(orgIterator);
		} else {
			log4j.info("No Mail for store - " + orgIterator.getName());logger.log("No Mail for store - " + orgIterator.getName());
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
