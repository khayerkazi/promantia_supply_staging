package in.decathlon.ibud.replenishment.implantation;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.supply.dc.util.ImplantationMails;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import au.com.bytecode.opencsv.CSVWriter;

import com.sysfore.catalog.CLImplantation;

public class ImplantationProcessEnhanced extends DalBaseProcess {
  static final Logger log = Logger.getLogger(ImplantationProcessEnhanced.class);
  static ProcessLogger logger;
  private ImplantationDAO dao = new ImplantationDAO();
  private ImpSupplyCall supplyWs = new ImpSupplyCall();
  final Properties p = SuppyDCUtil.getInstance().getProperties();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    String logmsg = "";
    try {
      List<Organization> orgList = implantationCheckForOrg();

      if (orgList != null && orgList.size() > 0) {
    	  
        for (Organization org : orgList) {

	              Warehouse implWr = BusinessEntityMapper.getImplWarehouse(org).getWarehouse();
	              Sequence docSeq = getDocumentSequence(org);
	
	              logmsg = logmsg + "\n" + implantationPO(org, implWr, docSeq, processid);
	
	       } 
        
	    } else {
	          logmsg = logmsg + "\n Failed to run implantation process  as it is not configured any organization. \n";
	        }
      
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      OBDal.getInstance().commitAndClose();
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
    } finally {
      logger.log(logmsg);
    }

  }

  public Sequence getDocumentSequence(Organization org) {
	  
	  	String sequence = org.getName()+" Store Requisition";
	  	
	  	OBCriteria<Sequence> seqCrit = OBDal.getInstance().createCriteria(Sequence.class);
	  	seqCrit.add(Restrictions.eq(Sequence.PROPERTY_NAME, sequence));
	  	seqCrit.setMaxResults(1);
	    
	    if (seqCrit.count() == 0) {
	    	log.error("There is no document Sequence for the organization " + org);
		    throw new OBException("There is no document Sequence for the organization " + org);
	    }
	    else return seqCrit.list().get(0);
	  }

private List<Organization> implantationCheckForOrg() {

	  OBCriteria<Organization> impcriteria = OBDal.getInstance().createCriteria(Organization.class);
	  impcriteria.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, true));
	  impcriteria.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
	  impcriteria.add(Restrictions.eq(Organization.PROPERTY_DSIDEFISIMPLANTATION, "Y"));
   
	    List<Organization> impList = impcriteria.list();
	    if (impList != null && impList.size() > 0) {
	      return impList;
	    }
	    else return null;
}

public String implantationPO(Organization org, Warehouse implWr, Sequence docSeq, String processid)
      throws Exception {

	  String result = "";
	  String confirmedQty = "0";
	  
    try {

    	dao.createTempTables();
		dao.insertValuesInTable(org);
		dao.updateRecords();
		dao.computerank();
		dao.computegb();
		dao.updateOrderId();
		dao.updateOrderLineId();
		
		Map<String, Integer> mapCatNbProd = dao.getNumberProductToOrder();
		Map<String, String> computed = dao.computeOrderQty(org);
		List<Order> orderList = dao.fetchvalues(org,mapCatNbProd);
		dao.associateOrderLines();
		setLineObjToOrder(orderList);
		
		SessionHandler.getInstance().commitAndStart();
		
		dao.refreshOrder(orderList);
		
		if (orderList != null && orderList.size() > 0) {

		// Send order to supply
 		
 			try {
 				supplyWs.processRequest(orderList, computed);
 			} catch (Exception e) {
 				new OBException(e);
 				e.printStackTrace();
 			    logger.log(org.getName()+" " +e.toString()+"\n");
 			}
 		
 		//dao.updateBlockedQty(org);
 		
 		//dao.insertIntoLogTable(org);
 		
 		confirmedQty = dao.fetchconfirmed(org);

 		File csvfile = generatecsv(org);
 		 		
		SessionHandler.getInstance().commitAndStart();
 				
 		ImplantationMails.getInstance().sendmailtoall(org,csvfile,confirmedQty);
     		     		
      }
      
      else {
        result = result + "\n Organization " + org.getName() + "'s warehouse " + implWr.getName()
            + " has no PO's to be created including and SO for them";
        log.info("Organization " + org.getName() + "'s warehouse " + implWr.getName()
            + " has no PO's to be created SO for them");
      }

      result = result + "\n" + orderList.size() + " PO's created for " + "Organization "
          + org.getName() + "'s warehouse " + implWr.getName();
      log.info(orderList.size() + " PO's created for " + "Organization " + org.getName()
          + "'s warehouse " + implWr.getName());

    } catch (Exception e) {
      log.error(e.getMessage(), e);

      result = result + e.toString();
      throw new Exception(result);
    }
    return result;
  }


public static void setBlockedQty(OrderLine ol) throws Exception {
    try {
    	
    	Long confirmQty = ol.getSwConfirmedqty();
    	
    	CLImplantation implRecord = BusinessEntityMapper.getImplantationOrg(ol.getOrganization().getId(), ol.getProduct().getId());
    	
    	if(implRecord != null){
    		
    		Long implantQty = implRecord.getImplantationQty();
    		BigDecimal blockedQty = implRecord.getBLOCKEDQTY();
    		BigDecimal newblockedQty = blockedQty.add(BigDecimal.valueOf(confirmQty));
    		
    		implRecord.setBLOCKEDQTY(newblockedQty);
    		
    		int compareValue = BigDecimal.valueOf(implantQty).compareTo(newblockedQty);
    		
    		if(compareValue <= 0){
    			implRecord.setImplanted(true);
    		} else if (compareValue > 0) {
    			implRecord.setImplanted(false);
    		} 
    		
    		OBDal.getInstance().save(implRecord);
    	}
    	    	      
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
      throw e;
    }

  }
  
  
  private void setLineObjToOrder(List<Order> orderList) {
	  
		 for(int i=0;i<orderList.size();i++){
			 
			Order ods = OBDal.getInstance().get(Order.class,orderList.get(i).getId());
			OBCriteria<OrderLine> impOrdLinecriteria = OBDal.getInstance().createCriteria(OrderLine.class);
			impOrdLinecriteria.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, ods));
			List<OrderLine> criteriaList = impOrdLinecriteria.list();
			
			if(impOrdLinecriteria.count() >= 0 && null != impOrdLinecriteria){
				ods.setOrderLineList(criteriaList);
			}     
		 }
	}
  
  public File generatecsv(Organization org) {
		
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_hh-mm");
		final String date = format.format(new Date());
		
		File f = new File(p.getProperty("ImpcsvPath") + org.getName() + "_" + date + ".csv");
		try {
			if (!f.exists()){
				f.createNewFile();
			}
			
			CSVWriter writer = new CSVWriter(new FileWriter(f, true));

			writer.writeNext(new String[] { "Brand", "Item Code", "Model Name", "Implantation Quantity", "Blocked Quantity", "Requested Quantity", "Confirmed Quantity" });
			
			dao.csvdata(writer,org);
			writer.close();
			
			return f;
						
		} catch (IOException e) {
			throw new OBException("Cannot write csv file...", e);
		}
		
		
	}


}