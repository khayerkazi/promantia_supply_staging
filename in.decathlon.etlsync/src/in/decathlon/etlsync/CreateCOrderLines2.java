package in.decathlon.etlsync;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;

public class CreateCOrderLines2 implements WebService {

	
	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		// JSON Array static input for now
		JSONArray array = new JSONArray(request.getParameter("parameter"));
		String iOrderReceiptId = request.getParameter("iOrderReceiptId");System.err.println(iOrderReceiptId);
		double totalLinesAmount = 0.0;
		
		JSONObject obj = array.getJSONObject(0);
		
		final String res = "received";
		// write to the response
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(res);
		w.close();
		
		for(int i=0; i<array.length(); i++)
		{
			JSONObject obji = array.getJSONObject(i);
			totalLinesAmount = totalLinesAmount + obji.getDouble("priceactual") * obji.getInt("qtyordered");
		}
		
		// Default values declaration
		String doctype = "511A9371A0F74195AA3F6D66C722729D";
		String warehouse = "";
		String pricelist = "2205CDAF5996448484851F4524B25EA2";
		String bpartner_loc_id = "CF474260D60641439922F679EE439FD9";
		 
		if(obj.getString("ad_org_id").equals("13D67CE499E84F84BF95A66A4875BDB5")) {
			warehouse = "DD2AFCE3F329487A944E6BF2014A5063";
		} else if(obj.getString("ad_org_id").equals("07DB4B94385D4118808BE349EEEAB022")) {
			warehouse = "351502910F8B4F3795F1F45CCE39E0BD";
		} else if(obj.getString("ad_org_id").equals("603C6A266B4C40BCAD87C5C43DDF53EE")) {
			warehouse = "67951CEE618E42E99F4D97C24534CDC1";
		} else if(obj.getString("ad_org_id").equals("6C2C1AF9EE94432096C657FF4EE45990")) {
			warehouse = "64808B1D6D1F4AA7B11631BDB9E54976";
		} else if(obj.getString("ad_org_id").equals("77301C98A8124D4CB0E19404E9B6A26F")) {
			warehouse = "C77CED3C0C5A4CC494A8D1CED01A4C32";
		} else if(obj.getString("ad_org_id").equals("057FF7ABBAAA43ECA533ACA272264A1A")) {
			warehouse = "79D6C9755FF04C02A5F92C70E849EA4E";
		} else if(obj.getString("ad_org_id").equals("4BB328C91599449BB1FBE90E781097F2")) {
			warehouse = "93887BB917814E41A794D367F970C2CC";
		} else if(obj.getString("ad_org_id").equals("653A6FBA5DF4400C990AEF14FCC84CDE")) {
			warehouse = "2CEDC3AD8713406987E9C927E26F8E82";
		} else if(obj.getString("ad_org_id").equals("83ACE25CB7684EF0937B01BB6969C6DE")) {
			warehouse = "3B9648CEC63344C984D9F75E5DEE68C6";
		} else if(obj.getString("ad_org_id").equals("763822B8D9594A7BA0E8AC93006179FB")) {
			warehouse = "E2BBA43F68CE4FFDB693750690F312BE";
		} else if(obj.getString("ad_org_id").equals("5634F9BED7E94DB4A742DEF487DFCF3C")) {
			warehouse = "B267FD445B9447FB9BBC5484F0018660";
		}
		
		// Conversion from JSON String to JSON Object
		JSONObject job = new JSONObject("{\"_entityName\":\"Order\",\"client\":\""+obj.getString("ad_client_id")+"\",\"client._identifier\":\"\",\"organization\":\""+obj.getString("ad_org_id")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+obj.getString("ad_user_id")+"\",\"updatedBy\":\""+obj.getString("ad_user_id")+"\",\"salesTransaction\":\"true\",\"documentNo\":\""+obj.getString("documentno")+"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""+doctype+"\",\"documentType._identifier\":\"\",\"transactionDocument\":\""+doctype+"\",\"transactionDocument._identifier\":\"\",\"description\":\""+obj.getString("description")+"\",\"orderDate\":\""+obj.getString("dateordered")+"\",\"scheduledDeliveryDate\":\""+obj.getString("dateordered")+"\",\"accountingDate\":\""+obj.getString("dateordered")+"\",\"businessPartner\":\""+obj.getString("c_bpartner_id")+"\",\"businessPartner._identifier\":\"\",\"invoiceAddress\":\""+bpartner_loc_id+"\",\"invoiceAddress._identifier\":\"\",\"partnerAddress\":\""+bpartner_loc_id+"\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\"304\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\"A4B18FE74DF64897B71663B0E57A4EFE\",\"summedLineAmount\":"+totalLinesAmount+",\"paymentTerms._identifier\":\"\",\"invoiceTerms\":\"D\",\"deliveryTerms\":\"A\",\"freightCostRule\":\"I\",\"deliveryMethod\":\"P\",\"priority\":\"5\",\"warehouse\":\""+warehouse+"\",\"warehouse._identifier\":\"\",\"priceList\":\""+pricelist+"\",\"priceList._identifier\":\"\",\"userContact\":\""+obj.getString("ad_user_id")+"\",\"paymentMethod\":\"932DCBE7B0CA43E08BAEED3D456949C1\",\"paymentMethod._identifier\":\"\",\"dSReceiptno\":\""+obj.getString("em_im_receiptno")+"\",\"dSTotalpriceadj\":"+obj.getString("em_im_totalpriceadj")+",\"dSPosno\":\""+obj.getString("em_im_posno")+"\",\"dSEMDsRatesatisfaction\":\""+obj.getString("em_im_customersatisfaction")+"\",\"dSChargeAmt\":"+obj.getString("em_im_chargeamt")+",\"generateTemplate\":\"\",\"copyFromPO\":\"\",\"pickFromShipment\":\"\",\"receiveMaterials\":\"\",\"createInvoice\":\"\",\"addOrphanLine\":\"\",\"calculatePromotions\":\"\",\"quotation\":\"\",\"reservationStatus\":\"NR\",\"dSReceiptno\":\""+obj.getString("em_im_receiptno")+"\",\"swSendorder\":\"\",\"swModorder\":\"\",\"swDelorder\":\"\",\"obwplGeneratepicking\":\"\",\"sWEMSwPostatus\":\"\"}");
		System.err.println(job.toString());
		final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(JsonToDataConverter.class);
		// Converting JSON Object to Openbravo Business object 
		BaseOBObject bob = toBaseOBObject.toBaseOBObject(job);
		
		// Database Connections and Database insertions 
		OBDal.getInstance().save(bob);
		OBDal.getInstance().flush();
		
		// Inserted Sales Order Header id(c_order_id)
		final String olineID = (String) bob.getId();
		for(int i=0; i<array.length(); i++)
		{
			JSONObject obji = array.getJSONObject(i);
			// lines parameters 
			int lineno = i+1;
			
			long qtyordered = obji.getLong("qtyordered") + obji.getLong("em_im_pcbqty") + obji.getLong("em_im_ueqty");
			
			JSONObject jobi = new JSONObject("{\"_entityName\":\"OrderLine\",\"client\":\""+obji.getString("ad_client_id")+"\",\"client._identifier\":\"\",\"organization\":\""+obji.getString("ad_org_id")+"\",\"organization._identifier\":\"\",\"createdBy\":\""+obj.getString("ad_user_id")+"\",\"updatedBy\":\""+obj.getString("ad_user_id")+"\",\"active\":\"true\",\"salesOrder\":\""+olineID+"\",\"salesOrder._identifier\":\"\",\"lineNo\":"+lineno+",\"businessPartner\":\""+obj.getString("c_bpartner_id")+"\",\"partnerAddress\":\""+bpartner_loc_id+"\",\"orderDate\":\""+obji.getString("dateordered")+"\",\"product\":\""+obji.getString("m_product_id")+"\",\"product._identifier\":\"\",\"warehouse\":\""+warehouse+"\",\"warehouse._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"orderedQuantity\":"+qtyordered+",\"reservedQuantity\":0,\"reservationStatus\":\"NR\",\"manageReservation\":\"\",\"managePrereservation\":\"\",\"currency\":\"304\",\"currency._identifier\":\"\",\"unitPrice\":"+obji.getString("priceactual")+",\"attributeSetValue\":null,\"tax\":\""+obji.getString("c_tax_id")+"\",\"tax._identifier\":\"\",\"dSLotqty\":"+obji.getString("em_im_ueqty")+",\"dSLotprice\":"+obji.getString("em_im_ueprice")+",\"dSBoxqty\":"+obji.getString("em_im_pcbqty")+",\"dSBoxprice\":"+obji.getString("em_im_pcbprice")+",\"dSEMDSUnitQty\":"+obji.getString("qtyordered")+"}");
			BaseOBObject bobi = toBaseOBObject.toBaseOBObject(jobi);
			
			// Database Connections and Database insertions 
			OBDal.getInstance().save(bobi);
		}
		OBDal.getInstance().flush();
		
		 final List parameters = new ArrayList();
		    parameters.add(olineID);
		    parameters.add(obj.getString("em_im_receiptno"));
		 final String procedureName = "ds_order_post3";
		    String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null).toString();
		    
		    if(resp.equals("1")) {
		    	
		    	System.err.println("1");System.err.println(" id "+olineID);
		    	final OBQuery<org.openbravo.model.dataimport.Order> i_order_query = OBDal.getInstance().createQuery(org.openbravo.model.dataimport.Order.class, "as o where o.iMEMImReceiptno='"+iOrderReceiptId+"'");
		    	
		    	for (org.openbravo.model.dataimport.Order orde : i_order_query.list()) {
	System.err.println("batch assigned"+orde.getImBatchno());
					if(orde.getImBatchno() != null) {
	System.err.println("delete");
						orde.setProcessed(true);
						orde.setProcessNow(false);
						orde.setImportErrorMessage("Import Success");
						orde.setImportProcessComplete(true);
						OBDal.getInstance().save(orde);
					}
					OBDal.getInstance().flush();
		    	}
		    }
		// STORED PROCEDURE
	    // get an AD_Process instance, 104 is the C_Order_Post process
	   /* final org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(
	        org.openbravo.model.ad.ui.Process.class, "104");
	    final ProcessInstance processInstance = CallProcess.getInstance().call(process, olineID,
	        new HashMap<String, String>());    // the processInstance now contains the result
	    final String errorMsg = processInstance.getErrorMsg();
	    final Object result = processInstance.getResult();
	    final String recordID = processInstance.getRecordID();

System.err.println(errorMsg);System.err.println(recordID);	    */
	    
	    /*final String resResult;
	    if (result.toString().equals("0")) {
	    	resResult = "KO";
	    } else {
	    	resResult = "OK";
	    }
		
System.err.println(resResult);
		
		
//	    if(resResult.equals("OK")) {
System.err.println("if OK");System.err.println(" id "+olineID);
	    	final OBQuery<org.openbravo.model.dataimport.Order> i_order_query = OBDal.getInstance().createQuery(org.openbravo.model.dataimport.Order.class, "as o where o.iMEMImReceiptno='"+iOrderReceiptId+"'");
	    	
	    	for (org.openbravo.model.dataimport.Order orde : i_order_query.list()) {
System.err.println("batch assigned"+orde.getImBatchno());
				if(orde.getImBatchno() != null) {
System.err.println("delete");
					OBDal.getInstance().remove(orde);
				}
	    	}
//	    }*/
		    
		    
		    
		/*final OBQuery<org.openbravo.model.common.order.Order> obQuery = OBDal.getInstance().createQuery(org.openbravo.model.common.order.Order.class, "as o where o.id='"+olineID+"'");
		org.openbravo.model.common.order.Order ordHeader = obQuery.list().get(0);
		ordHeader.setDSReceiptno(obj.getString("em_im_receiptno"));
		OBDal.getInstance().save(ordHeader);
		OBDal.getInstance().flush();*/
	    OBDal.getInstance().commitAndClose();
	}
	
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}
}