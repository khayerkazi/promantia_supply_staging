/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.nous.searchitem.ad_webservice;
 
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.Parameter;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
 
/**
 * Post the stock of the store
 * and create a PI
 * @author shreyas
 */
public class PostStoreStockWebService implements WebService {
 
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(PostStoreStockWebService.class);
 
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // do some checking of parameters
	  
    StringWriter sw = new StringWriter();
    String code = request.getParameter("code");
    String storeStock = request.getParameter("quantity");
    String storename = request.getParameter("storename");
    String email = request.getParameter("email");  
    String adClientId = "";


    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
    if (storename == null) {
      throw new IllegalArgumentException("The storename parameter is mandatory");
    }
    if (storeStock == null) {
      throw new IllegalArgumentException("The stock parameter is mandatory");
    } 
    if (email == null) {
      throw new IllegalArgumentException("The email parameter is mandatory");
    } 
    String[] itemCodes = code.split(",");
    String[] stock = storeStock.split(",");   
    String hql = "";
    String message="Success";
    //System.out.println(storename+code+email+storeStock);
    try
    {
    	// Current client
    	OBContext context = OBContext.getOBContext();
		Client client = context.getCurrentClient();
		adClientId = client.getId();
		
//		OBContext.setAdminMode(true);
		
		// Current Organization
		Organization currentOrg = null;
		final OBCriteria<Organization> orgObCriteria = OBDal.getInstance().createCriteria(Organization.class);
		orgObCriteria.add(Restrictions.eq(Organization.PROPERTY_NAME, storename));
		if(orgObCriteria.count() > 0) {
			currentOrg = orgObCriteria.list().get(0);
		}
		
		// fetching user information
		User currentUser = null;
		final OBCriteria<User> userObCriteria = OBDal.getInstance().createCriteria(User.class);
		userObCriteria.add(Restrictions.eq(User.PROPERTY_EMAIL, email));
		if(userObCriteria.count() > 0) {
			currentUser = userObCriteria.list().get(0);
		}
		
		// fetching warehouse information
		Warehouse currentWarehouse = null;
		final OBCriteria<Warehouse> warehouseObCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
		warehouseObCriteria.add(Restrictions.eq(Warehouse.PROPERTY_ORGANIZATION, currentOrg));
		warehouseObCriteria.add(Restrictions.ilike(Warehouse.PROPERTY_NAME, "Saleable%"));
		if(warehouseObCriteria.count() > 0) {
			currentWarehouse = warehouseObCriteria.list().get(0);
		}
		
		// fetching locator information
		Locator currentLocator = null;
		final OBCriteria<Locator> locatorObCriteria = OBDal.getInstance().createCriteria(Locator.class);
		locatorObCriteria.add(Restrictions.eq(Locator.PROPERTY_ORGANIZATION, currentOrg));
		locatorObCriteria.add(Restrictions.eq(Locator.PROPERTY_WAREHOUSE, currentWarehouse));
		if(locatorObCriteria.count() > 0) {
			currentLocator = locatorObCriteria.list().get(0);
		}
		
		final InventoryCount mInventoryEntry = OBProvider.getInstance().get(InventoryCount.class);
		mInventoryEntry.setClient(client);
		mInventoryEntry.setOrganization(currentOrg);
		mInventoryEntry.setCreatedBy(currentUser);
		mInventoryEntry.setUpdatedBy(currentUser);
		mInventoryEntry.setProcessNow(false);
		mInventoryEntry.setProcessed(false);
		mInventoryEntry.setName("STORE CORRECTION");
		mInventoryEntry.setDescription("STORE CORRECTION FOR STOCK");
		mInventoryEntry.setWarehouse(currentWarehouse);
		mInventoryEntry.setMovementDate(new Timestamp(new Date().getTime()));
		mInventoryEntry.setSwMovementtype("PI");
		
		OBDal.getInstance().save(mInventoryEntry);
		OBDal.getInstance().flush();
		Long lineNo = 0L;
		
		for(int i=0;i<stock.length;i++)
	    {
			if(!stock[i].equals("-")) {
				
				lineNo = lineNo + 10;
				// fetching product info
				Product currentProduct = null;
				final OBCriteria<Product> prodObCriteria = OBDal.getInstance().createCriteria(Product.class);
				prodObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, itemCodes[i]));
				if(prodObCriteria.count() > 0) {
					currentProduct = prodObCriteria.list().get(0);
				}
				
				// fetching booked stock information
				BigDecimal qtyBooked = BigDecimal.ZERO;
				final OBQuery qtyBookedObQuery = OBDal.getInstance().createQuery(StorageDetail.class, "as sd where sd.product.id='"+currentProduct.getId()+"' and sd.storageBin.id='"+currentLocator.getId()+"'");
				qtyBookedObQuery.setSelectClause("COALESCE(sd.quantityOnHand,0)");
				if(null != qtyBookedObQuery.list()) {
					log.info("Qtybook is not null from the query");
					try {
						if(!qtyBookedObQuery.list().get(0).toString().equals("0")) {
							qtyBooked = new BigDecimal(qtyBookedObQuery.list().get(0).toString());
							log.info(qtyBooked);
						}
					} catch (IndexOutOfBoundsException ioe) {
						qtyBooked = BigDecimal.ZERO;
					}
				}
				
				final InventoryCountLine mInventoryLineEntry = OBProvider.getInstance().get(InventoryCountLine.class);
				mInventoryLineEntry.setClient(client);
				mInventoryLineEntry.setOrganization(currentOrg);
				mInventoryLineEntry.setCreatedBy(currentUser);
				mInventoryLineEntry.setUpdatedBy(currentUser);
				mInventoryLineEntry.setLineNo(lineNo);
				mInventoryLineEntry.setPhysInventory(mInventoryEntry);
				mInventoryLineEntry.setStorageBin(currentLocator);
				mInventoryLineEntry.setProduct(currentProduct);
	            mInventoryLineEntry.setQuantityCount(new BigDecimal(stock[i]));
				mInventoryLineEntry.setBookQuantity(qtyBooked);
				mInventoryLineEntry.setDescription("STORE CORRECTION FOR STOCK");
				mInventoryLineEntry.setUOM(OBDal.getInstance().get(UOM.class, "100"));
				mInventoryLineEntry.setAttributeSetValue(OBDal.getInstance().get(AttributeSetInstance.class, "0"));
				
				OBDal.getInstance().save(mInventoryLineEntry);
				OBDal.getInstance().flush();
			} else {
				continue;
			}
			
	    }
		OBDal.getInstance().commitAndClose();
		
		OBContext.setAdminMode(true);
		
		// get the process, we know that 199 is the generate shipments from invoice sp
		final Process process = OBDal.getInstance().get(Process.class, "107");
		
	    // Create the pInstance
	    final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
	    
	    // sets its process
	    pInstance.setProcess(process);
	    
	    // must be set to true
	    pInstance.setActive(true);
	    pInstance.setRecordID(mInventoryEntry.getId());
	    
	    // get the user from the context
	    pInstance.setUserContact(currentUser);
	    
	    // now create a parameter and set its values
//	    final Parameter parameter = OBProvider.getInstance().get(Parameter.class);
//	    parameter.setSequenceNumber(SequenceIdData.getUUID());
//	    parameter.setParameterName("Selection");
//	    parameter.setString("Y");
	    
	    // set both sides of the bidirectional association
//	    pInstance.getADParameterList().add(parameter);
//	    parameter.setProcessInstance(pInstance);
	    
	    // persist to the db
	    OBDal.getInstance().save(pInstance);

	    // flush, this gives pInstance an ID
	    OBDal.getInstance().flush();
		
	    // call the SP
	    try {
	      // first get a connection
	      final Connection connection = OBDal.getInstance().getConnection();
	      // connection.createStatement().execute("CALL M_InOut_Create0(?)");
	      final PreparedStatement ps = connection.prepareStatement("SELECT * FROM m_inventory_post(?)");
	      ps.setString(1, pInstance.getId());
	      ps.execute();
	    } catch (Exception e) {
	      throw new IllegalStateException(e);
	    } finally {
	        OBContext.restorePreviousMode();
	    }
	    
	    // refresh the pInstance as the SP has changed it
	    OBDal.getInstance().getSession().refresh(pInstance);
	    OBDal.getInstance().commitAndClose();
	    
		// Calling Stored Procedure 
		/*final List parameters = new ArrayList();
	    parameters.add(poHeaderId);
	    final String procedureName = "ecom_dc_post";
	    if((CallStoredProcedure.getInstance().call(procedureName, parameters, null)).equals("1")) {
	    	System.out.println("Procedure called successfully for the PO "+mInventoryEntry.getId());
	    }
		
	    String pinstance = SequenceIdData.getUUID();
	    PInstanceProcessData.insertPInstance(con, conn, pinstance, "107", mInventoryEntry.getId(), "N",
	        vars.getUser(), vars.getClient(), vars.getOrg());
	    ImportOrderProcessData.cOrderPost0(con, conn, pinstance);

	    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(con, conn,
	        pinstance);
	    OBError myMessage = Utility.getProcessInstanceMessage(conn, vars, pinstanceData);

	    String messageResult = myMessage.getMessage();
	    if (myMessage.getMessage().equals("")) {
	      messageResult = order_documentno + " - "
	          + Utility.messageBD(conn, "Success", vars.getLanguage());
	    } else {
	      messageResult = order_documentno + " - " + myMessage.getMessage();
	    }

	    if (myMessage.getType().equals("Success") || myMessage.getType().equals("Warning")) {
	      totalProcessed = totalProcessed + 1;
	    }*/
		
	    /*for(int i=0;i<stock.length;i++)
	    {
	      List<Object> param = new ArrayList<Object>();
	      param.add(itemCodes[i]);
	      param.add(Integer.parseInt(stock[i]));
	      param.add(storename);
	      param.add(email);
	      param.add(adClientId);
	      
	      CallStoredProcedure.getInstance().call("nsi_post_inventory", param, null, true, false);
	    }*/
    }
    catch(Exception e)
    {
      e.printStackTrace();
      log.error("Error occured due to: "+e.getMessage());
      message = "Error in updating the store stock";
    }
    sw.append("<message>"+message+"</message>");
    // and get the result
    final String xml = sw.toString();
 
    // write to the response
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(xml);
    w.close();
  }
  
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
 
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
 
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
}
