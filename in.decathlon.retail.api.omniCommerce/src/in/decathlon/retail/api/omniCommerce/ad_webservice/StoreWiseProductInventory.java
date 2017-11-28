package in.decathlon.retail.api.omniCommerce.ad_webservice;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import in.decathlon.integration.PassiveDB;
import in.decathlon.retail.api.omniCommerce.util.OmniCommerceAPIUtility;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.service.web.WebService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StoreWiseProductInventory implements WebService {

	private static final Logger LOG = Logger.getLogger(StoreWiseProductInventory.class);
	private static final OmniCommerceAPIUtility OmniCommerceAPIUtility = new OmniCommerceAPIUtility();
	
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		OBContext.setAdminMode();
		
		final String sampleRequest = request.getReader().readLine();
		
		final JSONObject requestObject = new JSONObject(sampleRequest);
		final String requestedStores = requestObject.getString("stores");
		
		final JSONObject storeWiseJsonObj = new JSONObject();
		final List<Organization> requestedStoreList = OmniCommerceAPIUtility.getRequestedStoresListforInventory(requestedStores);
		LOG.info("stores "+requestedStoreList.size());
		for (Organization store : requestedStoreList) {
			
			final JSONArray itemInfoJSONArray = new JSONArray(); 
			
			final OBQuery<StorageDetail> storageDetObCriteria = OBDal.getInstance().createQuery(StorageDetail.class, "as sd WHERE sd.organization.id='"+store.getId()+"' AND sd.storageBin.id in (select id from Locator where searchKey='Saleable "+store.getName()+"')");
			LOG.info("total products "+storageDetObCriteria.count());
			if (storageDetObCriteria.count() > 0) {
				
				for (StorageDetail storageDetail : storageDetObCriteria.list()) {
					final JSONObject itemInventoryInfoJsonObj = new JSONObject();
					itemInventoryInfoJsonObj.put("itemcode", storageDetail.getProduct().getName());
					itemInventoryInfoJsonObj.put("stock", storageDetail.getQuantityOnHand());
					itemInfoJSONArray.put(itemInventoryInfoJsonObj);
				}
				storeWiseJsonObj.put(store.getName(), itemInfoJSONArray);
			} else {
				storeWiseJsonObj.put(store.getName(), "");
			}
		}
		
		// Passive Database
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = PassiveDB.getInstance().getSupplyDBConnection();
			LOG.info("Passive supply DB connected!");
			stmt = conn.createStatement();
			final String stockSQL = "SELECT mp.name as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty "
					+ "FROM m_product mp "
					+ "INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id "
					+ "INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id "
					+ "INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id "
					+ "and ml.isactive='Y' and ml.em_obwhs_type='ST' and msd.m_product_id = mp.m_product_id "
					+ "group by mp.name order by mp.name;";
			ResultSet rs = stmt.executeQuery(stockSQL);
			final JSONArray itemInfoJSONArray = new JSONArray();
			while(rs.next()){
				final JSONObject itemInventoryInfoJsonObj = new JSONObject();
				
				itemInventoryInfoJsonObj.put("itemcode", rs.getString("product_id"));
				itemInventoryInfoJsonObj.put("stock", rs.getDouble("msdqty"));
				itemInfoJSONArray.put(itemInventoryInfoJsonObj);
			}
			if(itemInfoJSONArray.length() > 0) {
				storeWiseJsonObj.put("WHStock", itemInfoJSONArray);
			} else {
				storeWiseJsonObj.put("WHStock", "");
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(conn != null)
					conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		OBContext.restorePreviousMode();
		
		// Response
		response.setContentType("application/json");
		// Get the printwriter object from response to write the required json object to the output stream      
		PrintWriter out = response.getWriter();
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object  
		out.print(storeWiseJsonObj);
		out.flush();
		out.close();		
	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();		
	}

}
