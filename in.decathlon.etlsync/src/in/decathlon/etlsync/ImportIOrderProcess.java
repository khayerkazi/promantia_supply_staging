package in.decathlon.etlsync;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.dataimport.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.json.JsonToDataConverter;

public class ImportIOrderProcess extends DalBaseProcess {

	private ProcessLogger logger;
	
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
		
		// this logger logs into the LOG column of the AD_PROCESS_RUN database table
		logger = bundle.getLogger(); 
		
		int i=0;//initialize header/c_order variable

		// This will fetch the records from i_order where isimported is 'Y' 
		final OBQuery<Order> importedOrders = OBDal.getInstance().createQuery(Order.class, "as o where o.importProcessComplete=true and o.processed=true");
		for(Order importedOrder : importedOrders.list()){
			// Deletes the record from i_order table
			OBDal.getInstance().remove(importedOrder);
		}
		OBDal.getInstance().commitAndClose();
		
		// Fetching the records from i_order table where the batch no is NULL with unique receipt numbers
		final OBQuery<Order> obQuery = OBDal.getInstance().createQuery(Order.class, "as o where o.imBatchno is null");
		obQuery.setSelectClause("DISTINCT o.iMEMImReceiptno");
		obQuery.setMaxResult(50);
		
		List batchNumbers = new ArrayList();
		// for-each to call the second webservice uniquely by fetching the specific records
		for (Object recNo : obQuery.list().toArray()) {
			logger.log("Import Process is started for the following receipnumbers \n");
			logger.log((String)recNo + "\n");
			final OBQuery<Order> obQuery1 = OBDal.getInstance().createQuery(Order.class, "as o where o.iMEMImReceiptno='"+(String)recNo+"' and o.imBatchno is null");
			
			// Generate random string for batch no
			String batch_no = UUID.randomUUID().toString().replace("-", "").toUpperCase();
			batchNumbers.add(batch_no);
			
			for(Order iorder : obQuery1.list()) {
				
				iorder.setImBatchno(batch_no);
				iorder.setProcessNow(true);
				
				OBDal.getInstance().save(iorder);
				OBDal.getInstance().flush();
			}
			OBDal.getInstance().commitAndClose();
			logger.log("Batch Numbers assigned for the above receipt Numbers as below \n");
			logger.log(batch_no+"\n");
			
		}
		// for-each for rotating based on batch number
		for(Object batchNo : batchNumbers) {
			logger.log("Import Process is started for the batch Number : "+(String)batchNo+"\n");
			String currentBatchno = (String)batchNo;
			
			final OBQuery<Order> obQuery2 = OBDal.getInstance().createQuery(Order.class, "as o where o.imBatchno='"+currentBatchno+"'");
			if(obQuery2.list().size() > 0) {

				Order orderHeader = obQuery2.list().get(i);
	
				// Default values declaration
				String doctype = "511A9371A0F74195AA3F6D66C722729D";
				String warehouse = "";
				String pricelist = "2205CDAF5996448484851F4524B25EA2";
				String bpartner_loc_id = "CF474260D60641439922F679EE439FD9";
				 
				if(orderHeader.getOrganization().getId().equals("13D67CE499E84F84BF95A66A4875BDB5")) {
					warehouse = "DD2AFCE3F329487A944E6BF2014A5063";
				} else if(orderHeader.getOrganization().getId().equals("07DB4B94385D4118808BE349EEEAB022")) {
					warehouse = "351502910F8B4F3795F1F45CCE39E0BD";
				} else if(orderHeader.getOrganization().getId().equals("603C6A266B4C40BCAD87C5C43DDF53EE")) {
					warehouse = "67951CEE618E42E99F4D97C24534CDC1";
				} else if(orderHeader.getOrganization().getId().equals("6C2C1AF9EE94432096C657FF4EE45990")) {
					warehouse = "64808B1D6D1F4AA7B11631BDB9E54976";
				} else if(orderHeader.getOrganization().getId().equals("77301C98A8124D4CB0E19404E9B6A26F")) {
					warehouse = "C77CED3C0C5A4CC494A8D1CED01A4C32";
				} else if(orderHeader.getOrganization().getId().equals("057FF7ABBAAA43ECA533ACA272264A1A")) {
					warehouse = "79D6C9755FF04C02A5F92C70E849EA4E";
				} else if(orderHeader.getOrganization().getId().equals("4BB328C91599449BB1FBE90E781097F2")) {
					warehouse = "93887BB917814E41A794D367F970C2CC";
				} else if(orderHeader.getOrganization().getId().equals("653A6FBA5DF4400C990AEF14FCC84CDE")) {
					warehouse = "2CEDC3AD8713406987E9C927E26F8E82";
				} else if(orderHeader.getOrganization().getId().equals("83ACE25CB7684EF0937B01BB6969C6DE")) {
					warehouse = "3B9648CEC63344C984D9F75E5DEE68C6";
				} else if(orderHeader.getOrganization().getId().equals("763822B8D9594A7BA0E8AC93006179FB")) {
					warehouse = "E2BBA43F68CE4FFDB693750690F312BE";
				} else if(orderHeader.getOrganization().getId().equals("5634F9BED7E94DB4A742DEF487DFCF3C")) {
					warehouse = "B267FD445B9447FB9BBC5484F0018660";
				}
				final String dateOrdered = orderHeader.getOrderDate().toString();
				String desc = orderHeader.getDescription();
				if(orderHeader.getDescription() !=null) 
					desc = orderHeader.getDescription().replace("\n", " ").trim();
				if(orderHeader.getBusinessPartner() != null) {
					try {
						JSONObject job = new JSONObject("{\"_entityName\":\"Order\",\"client\":\""+orderHeader.getClient().getId()+"\",\"client._identifier\":\"\",\"organization\":\""+orderHeader.getOrganization().getId()+"\",\"organization._identifier\":\"\",\"createdBy\":\""+orderHeader.getUserContact().getId()+"\",\"updatedBy\":\""+orderHeader.getUserContact().getId()+"\",\"salesTransaction\":\"true\",\"documentNo\":\""+orderHeader.getDocumentNo()+"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""+doctype+"\",\"documentType._identifier\":\"\",\"transactionDocument\":\""+doctype+"\",\"transactionDocument._identifier\":\"\",\"description\":\""+desc+"\",\"orderDate\":\""+orderHeader.getOrderDate().toString()+"\",\"scheduledDeliveryDate\":\""+orderHeader.getOrderDate().toString()+"\",\"accountingDate\":\""+orderHeader.getOrderDate().toString()+"\",\"businessPartner\":\""+orderHeader.getBusinessPartner().getId()+"\",\"businessPartner._identifier\":\"\",\"invoiceAddress\":\""+bpartner_loc_id+"\",\"invoiceAddress._identifier\":\"\",\"partnerAddress\":\""+bpartner_loc_id+"\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\"304\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\"A4B18FE74DF64897B71663B0E57A4EFE\",\"summedLineAmount\":0,\"paymentTerms._identifier\":\"\",\"invoiceTerms\":\"D\",\"deliveryTerms\":\"A\",\"freightCostRule\":\"I\",\"deliveryMethod\":\"P\",\"priority\":\"5\",\"warehouse\":\""+warehouse+"\",\"warehouse._identifier\":\"\",\"priceList\":\""+pricelist+"\",\"priceList._identifier\":\"\",\"userContact\":\""+orderHeader.getUserContact().getId()+"\",\"paymentMethod\":\"932DCBE7B0CA43E08BAEED3D456949C1\",\"paymentMethod._identifier\":\"\",\"dSReceiptno\":\""+orderHeader.getIMEMImReceiptno()+"\",\"dSTotalpriceadj\":"+orderHeader.getIMEMImTotalpriceadj()+",\"dSPosno\":\""+orderHeader.getIMEMImPosno()+"\",\"dSEMDsRatesatisfaction\":\""+orderHeader.getIMEMImCustomersatisfaction()+"\",\"dSChargeAmt\":"+orderHeader.getIMEMImCustomersatisfaction()+",\"generateTemplate\":\"\",\"copyFromPO\":\"\",\"pickFromShipment\":\"\",\"receiveMaterials\":\"\",\"createInvoice\":\"\",\"addOrphanLine\":\"\",\"calculatePromotions\":\"\",\"quotation\":\"\",\"reservationStatus\":\"NR\",\"swSendorder\":\"\",\"swModorder\":\"\",\"swDelorder\":\"\",\"obwplGeneratepicking\":\"\",\"sWEMSwPostatus\":\"\"}");
		
						final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(JsonToDataConverter.class);
						// Converting JSON Object to Openbravo Business object 
						BaseOBObject bob = toBaseOBObject.toBaseOBObject(job);
						
						// Database Connections and Database insertions 
						OBDal.getInstance().save(bob);
						OBDal.getInstance().flush();
						OBDal.getInstance().commitAndClose();
					
						// Inserted Sales Order Header id(c_order_id)
						final String olineID = (String) bob.getId();
						logger.log("C_order Header is created"+olineID+"\n");
						
						int lineno = 0;
						
						for (Order order : obQuery2.list()) {
							
							lineno++;
							BigDecimal qtyordered = order.getOrderedQuantity().add(new BigDecimal(order.getIMEMImPcbqty()).add(new BigDecimal(order.getIMEMImUeqty())));
							
							JSONObject jobi = new JSONObject("{\"_entityName\":\"OrderLine\",\"client\":\""+order.getClient().getId()+"\",\"client._identifier\":\"\",\"organization\":\""+order.getOrganization().getId()+"\",\"organization._identifier\":\"\",\"createdBy\":\""+order.getUserContact().getId()+"\",\"updatedBy\":\""+order.getUserContact().getId()+"\",\"active\":\"true\",\"salesOrder\":\""+olineID+"\",\"salesOrder._identifier\":\"\",\"lineNo\":"+lineno+",\"businessPartner\":\""+order.getBusinessPartner().getId()+"\",\"partnerAddress\":\""+bpartner_loc_id+"\",\"orderDate\":\""+order.getOrderDate().toString()+"\",\"product\":\""+order.getProduct()+"\",\"product._identifier\":\"\",\"warehouse\":\""+warehouse+"\",\"warehouse._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"orderedQuantity\":"+qtyordered+",\"reservedQuantity\":0,\"reservationStatus\":\"NR\",\"manageReservation\":\"\",\"managePrereservation\":\"\",\"currency\":\"304\",\"currency._identifier\":\"\",\"unitPrice\":"+order.getUnitPrice()+",\"attributeSetValue\":null,\"tax\":\""+order.getTax().getId()+"\",\"tax._identifier\":\"\",\"dSLotqty\":"+order.getIMEMImUeqty()+",\"dSLotprice\":"+order.getIMEMImUeprice()+",\"dSBoxqty\":"+order.getIMEMImPcbqty()+",\"dSBoxprice\":"+order.getIMEMImPcbprice()+",\"dSEMDSUnitQty\":"+order.getOrderedQuantity()+"}");
							BaseOBObject bobi = toBaseOBObject.toBaseOBObject(jobi);
						
							// Database Connections and Database insertions 
							OBDal.getInstance().save(bobi);
							OBDal.getInstance().flush();
						}
						OBDal.getInstance().commitAndClose();
						
						final List parameters = new ArrayList();
					    parameters.add(olineID);
					    parameters.add(orderHeader.getIMEMImReceiptno());
					    parameters.add(dateOrdered);
					    final String procedureName = "ds_order_post3";
					    String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null).toString();
					    logger.log("Procedure called for the C_Order ID "+olineID+"\n");
		
					    if(resp.equals("1")) {
					    	for (Order order : obQuery2.list()) {
					    		order.setSalesOrder((org.openbravo.model.common.order.Order)bob);
					    		order.setProcessed(true);
								order.setProcessNow(false);
								order.setImportErrorMessage("Import Success");
								order.setImportProcessComplete(true);
								OBDal.getInstance().save(order);
								OBDal.getInstance().flush();
					    	}
					    	OBDal.getInstance().commitAndClose();
					    }
					} catch (Exception e) {
						for (Order order : obQuery2.list()) {
							order.setImportErrorMessage("Error in Record");
							OBDal.getInstance().save(order);
							OBDal.getInstance().flush();
						}
						OBDal.getInstance().commitAndClose();
					    e.printStackTrace();
					}
				} else {
					for (Order order : obQuery2.list()) {
						// Setting the batch number
						order.setImportErrorMessage("BP Not Found");
						order.setProcessNow(false);
						// Updating the i_order records with batch numbers
						OBDal.getInstance().save(order);
						OBDal.getInstance().flush();
						logger.log("Import Process completed for the batch Number : "+(String)batchNo+"\n");
					}
					OBDal.getInstance().commitAndClose();
				}
			}
		}
	}
}