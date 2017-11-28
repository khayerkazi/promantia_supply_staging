package com.sysfore.decathlonimport.ad_process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.service.db.CallStoredProcedure;

import com.sysfore.decathlonimport.IM_Movement;

public class GoodsMovementProcessActionHandler extends BaseActionHandler {

	static Logger log4j = Logger.getLogger(GoodsMovementProcessActionHandler.class);
	Integer updatederror = 0;
	long lineno = 0;
	String jseverity,jtext,jtitle = "";
	
	@Override
protected JSONObject execute(Map<String, Object> parameters, String content) {

		JSONObject data = null;
		JSONObject result = new  JSONObject();
		String imovementId;
		String createdby;
		
		try {
			OBContext.setAdminMode();
			data = new JSONObject(content);
			imovementId = data.get("recordId").toString();
			IM_Movement im_mvmnt = OBDal.getInstance().get(IM_Movement.class, imovementId);
			createdby = im_mvmnt.getCreatedBy().getId();
			result = validatemovement(createdby);
			
			log4j.error("Result :" + result);

		} catch (Exception e){
			e.printStackTrace();
		} finally{
			OBContext.restorePreviousMode();
		}
		
		return result;
	}

private JSONObject validatemovement(String createdby) throws Exception {

		User cuser = OBDal.getInstance().get(User.class, createdby);
		String imMovementId ;
		String warehouse;
		String product;
		String fromlocator;
		String tolocator;
		String frombox;
		long qty;
		String tobox;
		StringBuilder errormsg;
		String tbox = "";
		JSONObject JsonResult = new JSONObject();
		String headerId;
		
		try {
			
		OBCriteria<IM_Movement> criteriamovement = OBDal.getInstance().createCriteria(IM_Movement.class);
		criteriamovement.add(Restrictions.eq(IM_Movement.PROPERTY_VALIDATED, false));
		criteriamovement.add(Restrictions.eq(IM_Movement.PROPERTY_CREATEDBY, cuser));
		log4j.error(cuser);
		
		if(criteriamovement.count() > 0){
			
			populatevalues();
			
			for(IM_Movement imIterator : criteriamovement.list()){
				
				errormsg = new StringBuilder();
				
				imMovementId = imIterator.getId();
				warehouse = imIterator.getWarehouse();
				product = imIterator.getProduct();
				fromlocator = imIterator.getLocator();
				tolocator = imIterator.getLocatorto();
				frombox = imIterator.getAttributesetvalue();
				qty = imIterator.getMovementQuantity();
				tobox = imIterator.getBoxto();
				
				String adOrg = checkWarehouseExists(warehouse);
				StorageDetail storage = checkFromMovement(imMovementId,product,fromlocator,frombox,qty);
				String newStorage = checkNewStorageExists(tolocator);
				
				log4j.error(" newStorage : "+newStorage);
				
				
				// Hard coded only for Saleable Whitefield since doesnt have box numbers (Incase if this needs to eliminated remove the if block)
				
				if(null != fromlocator && null != tolocator){
					
				if((fromlocator.equals("Saleable Whitefield") || fromlocator.contains("InTransit") || fromlocator.contains("MRP")) || fromlocator.contains("Defective") || (tolocator.equals("Saleable Whitefield") || tolocator.contains("InTransit") || tolocator.contains("MRP") || tolocator.contains("Defective"))){
						
							if(tobox==null || tobox.equals("")){
							tbox = "0";
							}
				}
				
					if(!"0".equals(tbox)){
					tbox = checkNewBoxExists(tobox);	
					}
				}
				
				
				if(adOrg != null && storage != null && newStorage != null && tbox != null && (qty > 0)){
					
					// what to do ??
					
				} else {
					
					if(null == adOrg){
			    		errormsg.append("Warehouse Doesn't Exists,");
			    	}
			    	if(null == storage){
			    		errormsg.append("Storage Quantity not available for Movement,");
			    	}
			    	if(null == newStorage){
			    		errormsg.append("Locator To Doesn't Exists,");
			    	}
			    	if(null == tbox){
			    		errormsg.append("Box To Doesn't Exists or can't be null,");
			    	}
			    	if(qty <= 0){
			    		errormsg.append("Quantity cannot be negative or zero.");
			    	}
			    	if(fromlocator.equals(tolocator)){
						errormsg.append("Locator :From and To are same ");
					}
			    	
			    	updateErrorMsg(imMovementId,errormsg);
			    	
				}
			}
		
			if(updatederror != 0){
				jseverity= "TYPE_ERROR";
				jtext = "Please Check the Error Messages.";
				jtitle = OBMessageUtils.messageBD("ValidationError");
				JsonResult = updateJsonResponse(jseverity,jtext,jtitle);
				return JsonResult;
			} else {
				
				headerId = insertHeader(createdby);
				log4j.error("headerId : " + headerId);
				
				if(headerId != null){
					
					insertLinesbulk(headerId,createdby);
					log4j.error("All Lines inserted ");
					
					Boolean callProcedure = PinstanceAndProcess(headerId);
					
					if(callProcedure){
						
						log4j.error("Procedure Called...");
						
						InternalMovement headerObj = OBDal.getInstance().get(InternalMovement.class,headerId);
						String documentno = headerObj.getDocumentNo();
						
						jseverity= "TYPE_SUCCESS";
						jtext = documentno;
						jtitle = OBMessageUtils.messageBD("Success");
						JsonResult = updateJsonResponse(jseverity,jtext,jtitle);
					}  
					
					else {
						jseverity= "TYPE_ERROR";
						jtext = "Movement Dint Process";
						jtitle = "Movement Error";
						JsonResult = updateJsonResponse(jseverity,jtext,jtitle);
					}
					
				}
				else {
					jseverity= "TYPE_ERROR";
					jtext = "Either Header or Lines may be null";
					jtitle = "Insufficient Data";
					JsonResult = updateJsonResponse(jseverity,jtext,jtitle);
				}
				
			}
			
			// Delete the validated and processed records
			deleteProcessed(createdby);
			
		} else {
			jseverity= "TYPE_ERROR";
			jtext = "No Records to Validate";
			jtitle = "Data Error";
			JsonResult = updateJsonResponse(jseverity,jtext,jtitle);
		}
		
		} catch (Exception e) {
			e.printStackTrace();
			jseverity= "TYPE_ERROR";
			jtext = e.getMessage();
			jtitle = "Exception Occured";
			JsonResult = updateJsonResponse(jseverity,jtext,jtitle);
     } 
		
	return JsonResult;
}

private void insertLinesbulk(String headerId, String createdby) throws Exception {
	
	String clientId = "187D8FC945A5481CB41B3EE767F80DBB";
	String orgId = "603C6A266B4C40BCAD87C5C43DDF53EE";
	
	String query = "insert into m_movementline(m_movementline_id,ad_client_id,ad_org_id,isActive,created,createdby," +
				   "updated,updatedby,m_movement_id,m_locator_id,m_locatorto_id,m_product_id,movementqty,description," +
				   "m_attributesetinstance_id, m_product_uom_id,quantityorder,c_uom_id,em_sw_size,em_sw_color_id,em_sw_model_id," +
				   "em_sw_mod_id,em_sw_boxto) select get_uuid(),?,?,'Y',now(),?,now(),?,?,im.m_locator_id,im.m_locatorto_id," +
				   "im.m_product_id,movementqty,'INTER WAREHOUSE MOVEMENT',im.m_attributesetinstance_id,NULL,NULL,'100'," +
				   "mp.em_cl_size,mp.em_cl_color_id,mp.em_cl_model_id,NULL,im.boxto from im_movement im, m_product mp " +
				   "where mp.m_product_id = im.m_product_id and im.createdby= ? ";
	
	PreparedStatement stmt = null;
	
	try {
		stmt = OBDal.getInstance().getConnection().prepareStatement(query);
		stmt.setString(1, clientId);
		stmt.setString(2, orgId);
		stmt.setString(3, createdby);
		stmt.setString(4, createdby);
		stmt.setString(5, headerId);
		stmt.setString(6, createdby);
		stmt.executeUpdate();
	} catch (SQLException e) {
		throw new OBException("Cannot insert movementline", e);
	} finally {
		stmt.close();
	}
}

private void populatevalues() throws Exception {
	
	
	String updatePLB = "update im_movement im set m_product_id=temp.m_product_id, m_locator_id=temp.m_locator_id " +
					   "from (select im_movement_id, mp.m_product_id,mp.name,ml.m_locator_id,ml.value from im_movement im," +
					   " m_product mp,m_locator ml where im.product = mp.name and im.locator = ml.value) temp " +
					   "where im.im_movement_id = temp.im_movement_id";
	
	
	String updateBox = "update im_movement im set m_attributesetinstance_id=temp.m_attributesetinstance_id " +
					   "from (select im_movement_id,mt.m_attributesetinstance_id,mt.lot from im_movement im, " +
					   "m_attributesetinstance mt where im.attributesetvalue = mt.lot and mt.lot is not null) temp " +
					   "where im.im_movement_id = temp.im_movement_id";
	
	String updateNullBox = "update im_movement im set m_attributesetinstance_id='0' where attributesetvalue is null " +
						   "and m_attributesetinstance_id is null";

	String updateLT = "update im_movement imm set m_locatorto_id = temp.m_locator_id from " +
					  "(select distinct ml.value,ml.m_locator_id from m_locator ml, im_movement im " +
					  "where im.locatorto = ml.value) temp where imm.locatorto=temp.value and " +
					  "(imm.m_product_id is not null or imm.m_locator_id is not null or imm.m_attributesetinstance_id is not null)";

	 Statement stmt = null;
	 
	  try {
		  
	      stmt = OBDal.getInstance().getConnection().createStatement();
	      stmt.execute(updatePLB);
	      stmt.execute(updateBox);
	      stmt.execute(updateNullBox);
	      stmt.execute(updateLT);

	  } catch (SQLException e) {
	      throw new OBException("Cannot update PLB & LT...", e);
	  } finally {
		  stmt.close();
	  }
	
}

private JSONObject updateJsonResponse(String jseverity, String jtext, String jtitle) throws Exception {
	
	JSONObject result = new JSONObject();
	JSONObject JsonResult = new JSONObject();
	
	try {
		result.put("severity", jseverity);
		result.put("text", jtext);
		result.put("title", jtitle);
		JsonResult.put("message", result);
	} catch (Exception e){
		throw new OBException("Cannot update JSON Response", e);
	}
	return JsonResult;
}

private void deleteProcessed(String createdby) throws Exception {

	PreparedStatement stmt = null;
	String query = "delete from im_movement where createdby = ?";

	try {
		stmt = OBDal.getInstance().getConnection().prepareStatement(query);
		stmt.setString(1, createdby);
		stmt.executeUpdate();
		} catch (SQLException e) {
		throw new OBException("Cannot delete processed records", e);
		} finally {
			stmt.close();
		}
}

private Boolean PinstanceAndProcess(String headerId) throws Exception {

	String clientId = "187D8FC945A5481CB41B3EE767F80DBB";
	String orgId = "0";
	String processId = "122";
	Boolean callPro = false;
		
    String query = "insert into ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isprocessing, created, " +
    			   "ad_user_id, updated,result, ad_client_id, ad_org_id, createdby, updatedby, isactive) VALUES " +
    			   "(get_uuid(),?,?,'N', now(), '100', now(), '0',?," +
    			   " ?,'100','100','Y')";

    PreparedStatement stmt = null;
    ResultSet rs = null;
    
	try {
		
		stmt = OBDal.getInstance().getConnection().prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
		stmt.setString(1, processId);
		stmt.setString(2, headerId);
		stmt.setString(3, clientId);
		stmt.setString(4, orgId);
		stmt.executeUpdate();
		rs = stmt.getGeneratedKeys();
		
		if(rs != null && rs.next()){
	    	callPro = callProcedure(rs.getString(1));
		}
		
	} catch (SQLException e) {
		throw new OBException("Cannot insert Pinstance", e);
	} finally {
		stmt.close();
	}	
	
	return callPro;
}

private Boolean callProcedure(String pInstanceId) throws Exception {
		
	final List<Object> parameters = new ArrayList<Object>();
	final String procedureName = "M_MOVEMENT_POST";
	Boolean control = false;
	
	try {
		ProcessInstance pinstance = OBDal.getInstance().get(ProcessInstance.class,pInstanceId);
		parameters.add(pinstance);
		CallStoredProcedure.getInstance().call(procedureName, parameters, null);
		OBDal.getInstance().commitAndClose();
		control = true;
	} catch (Exception e){
		throw new OBException("Movement Dint Process", e);
	} 
		return control;
}



private String insertHeader(String createdby) throws Exception {
	
	Date formattedDate = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
    String currentDate = sdf.format(formattedDate);
	String clientId = "187D8FC945A5481CB41B3EE767F80DBB";
	String orgId = "603C6A266B4C40BCAD87C5C43DDF53EE";
	String name = currentDate;
	String documentno = "IWM-"+currentDate;
	String description = "INTER WAREHOUSE REPLENISHMENT";
	String typegm = "IWM";
	
    String query = "insert into m_movement (m_movement_id,ad_client_id,ad_org_id,isactive,createdby,updatedby," +
    			   "name,description,posted,processed,processing,ad_orgtrx_id,c_project_id,c_campaign_id,c_activity_id," +
    			   "user1_id,user2_id,move_fromto_locator,documentno,em_sw_movementtypegm,created,updated,movementdate) " +
    			   "values (get_uuid(),?,?,'Y',?,?,?,?,'N','N','N',NULL,NULL,NULL,NULL,NULL,NULL,'N',?,?,now(),now(),now())";

    PreparedStatement stmt = null;
	ResultSet rs = null;
	
	try {
		stmt = OBDal.getInstance().getConnection().prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
		stmt.setString(1, clientId);
		stmt.setString(2, orgId);
		stmt.setString(3, createdby);
		stmt.setString(4, createdby);
		stmt.setString(5, name);
		stmt.setString(6, description);
		stmt.setString(7, documentno);
		stmt.setString(8, typegm);
		stmt.executeUpdate();
		rs = stmt.getGeneratedKeys();
		
        if(rs != null && rs.next()){
        	return rs.getString(1);
        } else return null;
        
	} catch (SQLException e) {
		throw new OBException("Cannot insert Header", e);
	} finally {
		stmt.close();
	}
}

private void updateErrorMsg(String imMovementId, StringBuilder errormsg) throws Exception {
		
	updatederror += 1;
	String error = errormsg.toString();
	PreparedStatement stmt = null;
	
	String query = "update im_movement set errormsg= ? where im_movement_id = ? ";

	try {
		stmt = OBDal.getInstance().getConnection().prepareStatement(query);
		stmt.setString(1, error);
		stmt.setString(2, imMovementId);
		stmt.executeUpdate();
		} catch (SQLException e) {
		throw new OBException("Cannot Update ERROR", e);
		} finally {
			stmt.close();
	}
}

private String checkNewBoxExists(String tobox) throws Exception {
	
    String newbox;
    
	boolean isEmpty = (tobox == null || tobox.trim().length() == 0);
	
	if (isEmpty) {
		
		return null;
		
	} else {
		
		String boxQuery = "SELECT id FROM AttributeSetInstance WHERE lotName =:tobox and isactive='Y'";

		Query qry = OBDal.getInstance().getSession().createQuery(boxQuery);
		qry.setParameter("tobox", tobox);
		List<String> queryList = qry.list();

		Integer boxCount = queryList.size();
		
		if(boxCount > 0){
			return queryList.get(0);
		} else {
			if(tobox != null && tobox.startsWith("B")){
				newbox = insertNewBox(tobox);
				return newbox;
			} else return null;
		}
	}
}

private String insertNewBox(String tobox) throws Exception  {

	String clientId = "187D8FC945A5481CB41B3EE767F80DBB";
	String orgId = "603C6A266B4C40BCAD87C5C43DDF53EE";
	String attributesetId = "BD85293DD63E4AAA9E29FE8C2A4206F8";  
		
    String query = "INSERT INTO M_ATTRIBUTESETINSTANCE (M_ATTRIBUTESETINSTANCE_ID, AD_CLIENT_ID, AD_ORG_ID, " +
				   "ISACTIVE, CREATED, CREATEDBY, UPDATED, UPDATEDBY, M_ATTRIBUTESET_ID, SERNO, LOT, GUARANTEEDATE," +
				   "description, M_LOT_ID, ISLOCKED, LOCK_DESCRIPTION) VALUES (get_uuid(), ?, ?, 'Y', " +
				   "now(),'100', now(),'100', ?, NULL, ?, now(), ('L'||?) ,NULL, 'N',NULL)";

    PreparedStatement stmt = null;
	ResultSet rs = null;
	
	try {
		stmt = OBDal.getInstance().getConnection().prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
		stmt.setString(1, clientId);
		stmt.setString(2, orgId);
		stmt.setString(3, attributesetId);
		stmt.setString(4, tobox);
		stmt.setString(5, tobox);
		stmt.executeUpdate();
		rs = stmt.getGeneratedKeys();
		
        if(rs != null && rs.next()){
        	return rs.getString(1);
        } else return null;
        
	} catch (SQLException e) {
		throw new OBException("Cannot insert Box", e);
	} finally {
		stmt.close();
	}
}

private String checkNewStorageExists(String tolocator) {
		
	String storageQuery = "SELECT id FROM Locator WHERE searchKey=:tolocator AND active='Y'";

	Query qry = OBDal.getInstance().getSession().createQuery(storageQuery);
	qry.setParameter("tolocator", tolocator);
	List<String> queryList = qry.list();

	Integer storageCount = queryList.size();

	if(storageCount > 0){
		return queryList.get(0);
	} else {
		return null;
	}
}

private String checkWarehouseExists(String warehouse) {
		
		if(warehouse.equals("DSI Warehouse")){
	    	return warehouse;
	    }
	    else return null;
	}

private BigDecimal groupMovementCheck(String product, String fromlocator, String frombox) {
	
		BigDecimal quantity;
		Query qry ;
		
		if(frombox != null){
			String group_query = "SELECT sum(movementQuantity) as qty FROM IM_Movement WHERE product=:product " +
					  "AND locator=:fromlocator AND attributesetvalue=:frombox " +
					  "GROUP BY product,locator,attributesetvalue";

			qry = OBDal.getInstance().getSession().createQuery(group_query);
			qry.setParameter("product", product);
			qry.setParameter("fromlocator", fromlocator);
			qry.setParameter("frombox", frombox);

		} else {
			
			String group_query = "SELECT sum(movementQuantity) as qty FROM IM_Movement WHERE product=:product " +
					  "AND locator=:fromlocator AND attributesetvalue=null " +
					  "GROUP BY product,locator,attributesetvalue";

			qry = OBDal.getInstance().getSession().createQuery(group_query);
			qry.setParameter("product", product);
			qry.setParameter("fromlocator", fromlocator);

		}
				List<Long> queryList = qry.list();
		
		Integer groupcount = queryList.size();
		
		if(groupcount > 0){
			quantity = BigDecimal.valueOf(queryList.get(0));
			return quantity;
		} else {
			return null;
		}
}

private StorageDetail checkFromMovement(String imMovementId, String product, String fromlocator, String frombox, long qty) throws Exception {

		BigDecimal groupqty = groupMovementCheck(product,fromlocator,frombox);

		String stockQuery;
		String storageId;
		Query qry;
		
		BigDecimal quantity = BigDecimal.valueOf(qty);
		
		log4j.error("Inside from Movement method");
		log4j.error("product " + product);
		log4j.error("fromlocator " + fromlocator);
		log4j.error("frombox" + frombox);
		log4j.error("qty " + qty);
		log4j.error(" Group qty " + groupqty);


		if(frombox != null){
			stockQuery = "SELECT id FROM MaterialMgmtStorageDetail " +
					 "WHERE product.id=(select id from Product where name=:product) AND " +
					 "storageBin.id=(select id from Locator where searchKey=:locator) AND " +
					 "attributeSetValue.id=(select id from AttributeSetInstance where lotName=:box and organization.id='603C6A266B4C40BCAD87C5C43DDF53EE') " +
					 "GROUP by id HAVING (quantityOnHand-reservedQty) >= :qty AND (quantityOnHand-reservedQty) >= :groupqty";
			
			qry = OBDal.getInstance().getSession().createQuery(stockQuery);
			qry.setParameter("product", product);
			qry.setParameter("locator", fromlocator);
			qry.setParameter("box", frombox);
			qry.setParameter("qty", quantity);
			qry.setParameter("groupqty", groupqty);
			
			log4j.error("From Movement with Box number");

		} else {
			stockQuery = "SELECT id FROM MaterialMgmtStorageDetail " +
					 "WHERE product.id=(select id from Product where name=:product) AND " +
					 "storageBin.id=(select id from Locator where searchKey=:locator) AND " +
					 "attributeSetValue.id='0' " +
					 "GROUP by id HAVING (quantityOnHand-reservedQty) >= :qty AND (quantityOnHand-reservedQty) >= :groupqty";
			
			qry = OBDal.getInstance().getSession().createQuery(stockQuery);
			qry.setParameter("product", product);
			qry.setParameter("locator", fromlocator);
			qry.setParameter("qty", quantity);
			qry.setParameter("groupqty", groupqty);
			
			log4j.error("From Movement with Box null");
		}

		List<String> queryList = qry.list();
		
		Integer msdCount = queryList.size();
		
		if(msdCount > 0){
			
			storageId = queryList.get(0);
			StorageDetail msd = OBDal.getInstance().get(StorageDetail.class,storageId);
			log4j.error(" StorageDetail " + msd);
			return msd;
			
		} else {
			return null;
		}
}

}

