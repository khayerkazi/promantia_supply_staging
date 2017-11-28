/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  dan "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.sysfore.storewarehouse.ad_process;

import java.util.Arrays;
import java.text.DecimalFormat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Timer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javassist.bytecode.Descriptor.Iterator;

import javax.servlet.ServletException;
import org.hibernate.Criteria;
import org.hibernate.transaction.JDBCTransactionFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.SessionHandler;
import org.joda.time.*;

/*
 * This class is used for calculating the fiscal stock
 * and the depriciation of stock
 */

public class FiscalStockValuationProcess extends DalBaseProcess {

	private final static String BATCH_SIZE = "50";
	private final static String SYSTEM_CLIENT_ID = "0";

	private boolean isDirect;

	private StringBuffer lastLog = new StringBuffer();
	private StringBuffer message = new StringBuffer();
	private Map<String, Integer> masterStoreMap = new LinkedHashMap<String, Integer>();

	private String[] TableIds = null;

	private ProcessLogger logger;
	private ConnectionProvider connection;
	private int totalProcessed = 0;

	static Logger log4j = Logger.getLogger(FiscalStockValuationProcess.class);

	public void doExecute(ProcessBundle bundle) throws Exception {

		logger = bundle.getLogger();
		connection = bundle.getConnection();

		VariablesSecureApp vars = bundle.getContext().toVars();
		if (vars.getClient().equals(SYSTEM_CLIENT_ID)) {
			OBCriteria<Client> obc = OBDal.getInstance().createCriteria(
					Client.class);
			obc.add(Expression.not(Expression.eq(Client.PROPERTY_ID,
					SYSTEM_CLIENT_ID)));

			for (Client c : obc.list()) {
				final VariablesSecureApp vars1 = new VariablesSecureApp(bundle
						.getContext().getUser(), c.getId(), bundle.getContext()
						.getOrganization());
				calculateFiscalStock(vars1, bundle);
			}
		} else {
			calculateFiscalStock(vars, bundle);
		}
	}

	/**
	 * This calulates the fiscal stock and
	 * the depriciation of stock
	 * @param vars
	 * @param bundle
	 * @return
	 * @throws ServletException
	 */
	private OBError calculateFiscalStock(VariablesSecureApp vars,
			ProcessBundle bundle) throws ServletException {
		// final String processId = bundle.getProcessId();
		final String pinstanceId = bundle.getPinstanceId();
		boolean m_deleteOldImported = true;
		boolean m_processOrders = true;
		final ProcessContext ctx = bundle.getContext();
		isDirect = bundle.getChannel() == Channel.DIRECT;
		OBError myError = new OBError();
		String clientId = bundle.getContext().getClient();
		String orgId = bundle.getContext().getOrganization();
		String userId = bundle.getContext().getUser();
		ConnectionProvider conn = null;
		Connection con = null;
		try {
				int no = 0;
				int rowCount = 0;
				int totalRows = 0;
				String batchno = "";
				boolean isRowGreaterThanNumber = false;
				conn = bundle.getConnection();
				con = conn.getTransactionConnection();
	
				log4j.info("ConnectionProvider =" + conn + "|| Connection=" + con);
				
				//create the session 
				final Session session = OBDal.getInstance().getSession();

				//create transaction
				//tx = session.beginTransaction();
				
				//start timer
				long startTime = System.nanoTime();
				//check the date of the report whether it is 31st or not
				Calendar reportCalendar = checkReportDates(session);
				Date dateOfReport = reportCalendar.getTime();
				reportCalendar.add(Calendar.DAY_OF_MONTH, 1);
				Date comparisionDate = reportCalendar.getTime();
				log4j.info("actual date:"+comparisionDate);
				log4j.info("report date:"+dateOfReport);

				
				
				
				// Initializing masterStoreMap, whick contains all the name of the
				// store from ad_org table
				masterStoreMap = getMasterAllStoreMap(session,dateOfReport);
				
				log4j.info(masterStoreMap.size());
	
				
				// Selecting all the product ids from m_transaction table
				final Query queryForProductIds = session
						.createSQLQuery("select distinct m_product_id from m_product");
								       //" limit 100");
				
				List<String> productList = queryForProductIds.list();
	
				
				
				log4j.info("MaterialTransaction product size ="
						+ productList.size());

				totalRows = productList.size();
				int newNumber = (totalRows/1000)*1000;
				int left = totalRows - newNumber;
				// Fiscal stock value for other stores
				List<Object> storeNameList = getAllStoresFromAdOrg(session,comparisionDate);	
				for (Object mTransObj : productList) {
	
					//get the landed quantity and the unit price from the purchase table
					final Query queryForSWPurchase = session
							.createSQLQuery("select date_of_reception, coalesce(qty,0) as qty,coalesce(unit_price,0) as price from sw_purchase "
									+ " where m_product_id = :mProductId and date_of_reception < :dateOfReception " +
									  " order by date_of_reception desc");
					
					 queryForSWPurchase.setParameter("mProductId",mTransObj.toString());
					 queryForSWPurchase.setParameter("dateOfReception",comparisionDate);
					//queryForSWPurchase.setParameter("mProductId",
							//"9374E81B229943B1AF016A99E9004A0E");
					List swPurchaseList = queryForSWPurchase.list();
	
					log4j.info("Size of PURCHASE table is :" + swPurchaseList.size());
	
					double fiscalStockValueOfDSI = 0.0;
					double fiscalStockValueOfDMI = 0.0;
					double unitFiscalValueOfDSI = 0.0;
					int totalLandedQty = 0;
					if (swPurchaseList.size() != 0) {
						
						//query to get the total landed quantity from the purchase table
						final Query queryForTotalLandedStock = session
								.createSQLQuery("select coalesce(sum(qty),0) from sw_purchase where m_product_id= :mProductId " +
										        " and date_of_reception < :dateOfReception");
						
						 queryForTotalLandedStock.setParameter("mProductId",mTransObj.toString());
						 queryForTotalLandedStock.setParameter("dateOfReception",comparisionDate);
		
						List<Object> totalLandedStockList = queryForTotalLandedStock
								.list();
	
						totalLandedQty = Integer.parseInt(totalLandedStockList.get(0).toString());
	
						log4j.info("Total Landed Quatity from sw_purchase table is:" + totalLandedQty);
	
						//Stock in DSI
						int totalStockInDSI = getStoreStock("DSI",true,false,(String)mTransObj,comparisionDate);
						
						//Stock for the DMI
						int totalStockInDMI = getStoreStock("DMI",false,true,(String)mTransObj,comparisionDate);
						
						// Calculated the fiscal stock value of DSI
						fiscalStockValueOfDSI = getFiscalStockValue(swPurchaseList,totalLandedQty, totalStockInDSI);
						log4j.info("Fiscal Stock Value Of DSI =is :"+ fiscalStockValueOfDSI);
						if(totalStockInDSI != 0)
						{
							unitFiscalValueOfDSI = (double)fiscalStockValueOfDSI/(double)totalStockInDSI;
						}
	
						
						// Calculated the fiscal stock value of DMI
						fiscalStockValueOfDMI = unitFiscalValueOfDSI*totalStockInDMI;
						log4j.info("Fiscal Stock Value Of DMI is :" + fiscalStockValueOfDMI);
	
						
	
						// Keeping an list for the Fiscal Stock value of All the stores from ad_org, except 
						// DSI and DMI
						List<Double> fiscalStockValueOfStoreArr = new ArrayList<Double>();
						// Keeping an list for the total stock in All the stores from ad_org, except 
						// DSI and DMI
						List<Integer> stockInStoreArr = new ArrayList<Integer>();
	
						//Iterating over all the stores from ad_org and adding the values to stockInStoreArr
						// and fiscalStockValueOfStoreArr by calling getOtherStoreStock() and 
						// getFiscalStockValue(0 respectively
						for (Object rows : storeNameList) {
							
							int storeValue = getStoreStock(String
									.valueOf(rows),false,false,(String)mTransObj,comparisionDate);
	
							stockInStoreArr.add(storeValue);
	
							fiscalStockValueOfStoreArr.add(unitFiscalValueOfDSI*storeValue);
						}
	
						// Getting product details from m_product table by passing the m_product_id
						// from m_transaction table
						final Query queryToGetProductDetails = session
								.createSQLQuery("select name,em_cl_modelcode, em_cl_modelname from m_product where m_product_id= :mProductId");
	
						queryToGetProductDetails.setParameter("mProductId",mTransObj.toString());
						List<Object> productDetailsList = queryToGetProductDetails.list();
	
						String modelCode = null;
						String modelName = null;
						String itemCode = null;
	
						// Iterating List got from m_product table and getting the model_code and model_name
						for (Object rows : productDetailsList) {

							Object[] row = (Object[]) rows;
							itemCode = (String) row[0];
							log4j.info("Item Code is:" + itemCode);
	
							modelCode = (String) row[1];
							log4j.info("Model Code is:" + modelCode);
	
							modelName = (String) row[2];
							log4j.info("Model Name is :" + modelName);
	
						}
	
						// Updating Stock Depreciation Table
						updateStockDepreciationTable(session,itemCode, 
							    modelCode,
								modelName,totalStockInDSI,swPurchaseList,dateOfReport);
	
						// Updating Fiscal Stock Table
						updateFiscalStockTable(session,itemCode,modelCode, modelName,
								totalStockInDSI, fiscalStockValueOfDSI,
								totalStockInDMI, fiscalStockValueOfDMI,
								fiscalStockValueOfStoreArr, stockInStoreArr,dateOfReport);
						
						
						
	
					} else {
						// Keeping an list for the Fiscal Stock value of All the stores from ad_org, except 
						// DSI and DMI
						List<Double> fiscalStockValueOfStoreArr = new ArrayList<Double>();
						// Keeping an list for the total stock in All the stores from ad_org, except 
						// DSI and DMI
						List<Integer> stockInStoreArr = new ArrayList<Integer>();						
							final Query queryToGetProductDetails = session
								.createSQLQuery("select name,em_cl_modelcode, em_cl_modelname from m_product where m_product_id= :mProductId");
	
						queryToGetProductDetails.setParameter("mProductId",mTransObj.toString());
						List<Object> productDetailsList = queryToGetProductDetails.list();
	
						String modelCode = null;
						String modelName = null;
						String itemCode = null;
	
						// Iterating List got from m_product table and getting the model_code and model_name
						for (Object rows : productDetailsList) {

							Object[] row = (Object[]) rows;
							itemCode = (String) row[0];
							log4j.info("Item Code is:" + itemCode);
	
							modelCode = (String) row[1];
							log4j.info("Model Code is:" + modelCode);
	
							modelName = (String) row[2];
							log4j.info("Model Name is :" + modelName);
	
						}
						
						for (Object rows : storeNameList) {
							
							int storeValue = getStoreStock(String
									.valueOf(rows),false,false,(String)mTransObj,comparisionDate);
	
							stockInStoreArr.add(storeValue);
	
							fiscalStockValueOfStoreArr.add(unitFiscalValueOfDSI*storeValue);
						}
						
						
						//Stock in DSI
						int totalStockInDSI = getStoreStock("DSI",true,false,(String)mTransObj,comparisionDate);
						
						//Stock for the DMI
						int totalStockInDMI = getStoreStock("DMI",false,true,(String)mTransObj,comparisionDate);

						// Updating Stock Depreciation Table
						updateStockDepreciationTable(session,itemCode, 
							    modelCode,
								modelName,totalStockInDSI,swPurchaseList,dateOfReport);
	
						// Updating Fiscal Stock Table
						updateFiscalStockTable(session,itemCode,modelCode, modelName,
								totalStockInDSI, fiscalStockValueOfDSI,
								totalStockInDMI, fiscalStockValueOfDMI,
								fiscalStockValueOfStoreArr, stockInStoreArr,dateOfReport);
					}
					//commit after 1000 rows
					if(rowCount < newNumber && !isRowGreaterThanNumber)
						{
							if((rowCount%1000)==0)
							{
								SessionHandler.getInstance().commitAndStart();
							}	
						}
						else
						{
							isRowGreaterThanNumber = true;
							rowCount = 0;
							if(rowCount == (left-1))
							{
								SessionHandler.getInstance().commitAndStart();	
							}
						}
						
						rowCount++;
				}
				
				//log4j.info("final commit");
				SessionHandler.getInstance().commitAndClose();
				//end timer
				long endTime = System.nanoTime();
				log4j.info("it took: "+Long.toString(endTime-startTime)+" ns");
						
		} catch (NoConnectionAvailableException ex) {
			log4j.error(ex.getMessage());
			throw new ServletException("@CODE=NoConnectionAvailable");
		} catch (SQLException ex2) {
			try {
				conn.releaseRollbackConnection(con);
			} catch (Exception ignored) {
			}
			throw new ServletException("@CODE="
					+ Integer.toString(ex2.getErrorCode()) + "@"
					+ ex2.getMessage());
		} catch (Exception ex3) {
			try {
				//tx.rollback();
				conn.releaseRollbackConnection(con);
			} catch (Exception ignored) {
			}
			log4j.error(ex3.getMessage());
			throw new ServletException("@CODE=@" +ex3.getMessage());
		}
		
		

		return myError;

	}

	/**
	 * This method calculates the fiscal stock vlue of each store
	 * by taking the total landed quatity from sw_prchase table
	 * and total number of stocks in each store.
	 * @param swPurchaseList : cotains all the information about 
	 * @param totalLandedQty
	 * @param totalStockInStores
	 * @return FiscalStockValue of a specific store
	 */
	
	public double getFiscalStockValue(List swPurchaseList, int totalLandedQty,
			int totalStockInStores) {

		double fiscalStockValue = 0.0;
		double fiscalValue = 0.0;
		long addedQty = 0;
		int stockDiff = totalStockInStores;
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		log4j.info("Difference in stock ==>" + stockDiff);
		//calculate the fiscal stock value using the formula
		for (Object rows : swPurchaseList) {

			Object[] row = (Object[]) rows;
			Date dateOfReception = (Date) row[0];

			BigDecimal qty = (BigDecimal) row[1];
			int quantity = Integer.parseInt(qty.toString());

			log4j.info("quantity ==>" + quantity);

			BigDecimal unitPrice = (BigDecimal) row[2];
			double uPrice = Double.parseDouble(unitPrice.toString());
			log4j.info("Unit Price ==>" + uPrice);

			addedQty = addedQty + quantity;
			if (addedQty <= stockDiff) {
				fiscalValue += quantity * uPrice;
			} 
			else {
				fiscalValue += (quantity - (addedQty - stockDiff)) * uPrice;
				break;
			}

		}
		log4j.info("fiscalValue is ==>" + fiscalValue);

		//fiscalStockValue = (fiscalValue)
		fiscalStockValue = Double.valueOf(decimalFormat.format(fiscalValue));
		return fiscalStockValue;

	}

	/**
	 * This method calculates the total quantity in the stores
	 * @param storeName
	 * @param isDSI
	 * @param isDMI
	 * @param productId
	 * @return stock quantity
	 */
	public int getStoreStock(String storeName,boolean isDSI,boolean isDMI,String productId,Date dateOfReport) {
		log4j.info("getStoreStock");
		final Session session = OBDal.getInstance().getSession();
		int totalStockInStore = 0;
		//prepare the query for each of the stores
		String subQuery = "";
		//check the flags isDSI or isDMI for generating the subquery
		//if isDSI get all stores values
		if(isDSI)
		{
			subQuery = "(select ml.m_locator_id from m_locator ml " +
					" inner join m_warehouse mw on ml.m_warehouse_id = mw.m_warehouse_id" +
					" where mw.em_sw_issvr = 'Y'"
					+ " group by ml.m_locator_id)";
			
		}
		//if isDMI then get only the ones selected for DMI
		else if(isDMI)
		{
			subQuery = "(select ml.m_locator_id from m_locator ml " +
					" inner join m_warehouse mw on ml.m_warehouse_id = mw.m_warehouse_id" +
					" where mw.em_sw_issvr = 'Y' and mw.em_sw_isdmi = 'Y'"
					+ " group by ml.m_locator_id)";
		}
		//else get by the storename
		else
		{
			subQuery = "(select ml.m_locator_id from m_locator ml " +
					" inner join m_warehouse mw on ml.m_warehouse_id = mw.m_warehouse_id" +
					" where mw.em_sw_issvr = 'Y' and mw.em_sw_isdmi = 'N' and ml.ad_org_id in" +
                                        " (select ad_org_id from ad_org where name = :storeName)"
					+ " group by ml.m_locator_id)";
			
		}

		//get the total stock value for the store
		String query = "select coalesce(sum(movementQty),0) from m_transaction "
				+ " where m_product_id = :mProductId and "
				+ " m_locator_id in "+subQuery+" and movementdate < :dateOfReception group by m_product_id";
		
		log4j.info("query for the stock:"+query);
		final Query queryForTotalStockInStore = session
				.createSQLQuery(query);
		
		//set the storename if not DSI or DMI
		if(!isDMI && !isDSI)
		{
			queryForTotalStockInStore.setParameter("storeName", storeName);
		}
		
		queryForTotalStockInStore.setParameter("mProductId",productId);
		queryForTotalStockInStore.setParameter("dateOfReception",dateOfReport);
		/*queryForTotalStockInStore.setParameter("mProductId",
				"9374E81B229943B1AF016A99E9004A0E");*/
		

		List<Object> queryForTotalStockInStoreList = queryForTotalStockInStore
				.list();
		
		if (queryForTotalStockInStoreList.size() != 0) {
				Double temp	= Double.parseDouble(queryForTotalStockInStoreList.get(0).toString());
				totalStockInStore = temp.intValue();
		}

		return totalStockInStore;

	}

	/**
	 * This method returns the global map for stores
	 * @return map
	 */
	
	public Map<String, Integer> getMasterAllStoreMap(Session session,Date dateOfReport) {
		Map<String, Integer> masterAllStoreMap = new LinkedHashMap<String, Integer>();
		List<Object> storeNameList = getAllStoresFromAdOrg(session,dateOfReport);
		int index = 0;
		//put all the store names in an global map
		for (Object rows : storeNameList) {
			masterAllStoreMap.put(String.valueOf(rows), index);
			index++;
		}
		return masterAllStoreMap;
	}

	/**
	 * This method inserts the values into fiscal stock table
	 * @param session
	 * @param modelCode
	 * @param modelName
	 * @param totalStockInDSI
	 * @param fiscalStockValueOfDSI
	 * @param totalStockInDMI
	 * @param fiscalStockValueOfDMI
	 * @param fiscalStockValueOfStoreArr
	 * @param stockInStoreArr
	 */
	
	// Put the parameter inside a Map and pass it
	public void updateFiscalStockTable(Session session,String itemCode, 
			String modelCode,
			String modelName, int totalStockInDSI,
			double fiscalStockValueOfDSI, int totalStockInDMI,
			double fiscalStockValueOfDMI,
			List<Double> fiscalStockValueOfStoreArr,
			List<Integer> stockInStoreArr,Date dateOfReport) {
		
		

		log4j.info(" In updateFiscalStockTable method!!!");

		String orgId = null;

		try {
			//get the storenames to be considered
			List<Object> storeNameList = getAllStoresFromAdOrg(session,dateOfReport);

			

			//add the strings to be present in all fiscal reports
			String queryString = "insert into sw_fiscal_stock(";
			queryString = queryString .concat("Item_Code").concat(",");
			queryString = queryString.concat("Model_Code").concat(",");
			queryString = queryString .concat("Model_Name").concat(",");
			queryString = queryString.concat("DSI_Qty").concat(",");
			queryString = queryString .concat("DSI_Value").concat(",");
			queryString = queryString .concat("DMI_Qty").concat(",");
			queryString = queryString .concat("DMI_Value").concat(",");
			queryString = queryString .concat("report_date").concat(",");

			//set the value string
			String valueString = " values(";
			valueString = valueString .concat(":Item_Code").concat(",");
			valueString = valueString.concat(":Model_Code").concat(",");
			valueString = valueString .concat(":Model_Name").concat(",");
			valueString = valueString.concat(":DSI_Qty").concat(",");
			valueString = valueString .concat(":DSI_Value").concat(",");
			valueString = valueString .concat(":DMI_Qty").concat(",");
			valueString = valueString .concat(":DMI_Value").concat(",");
			valueString = valueString .concat(":date_of_report").concat(",");

			

			int count = 0;
			for (Object rows : storeNameList) {
				String rowValue = String.valueOf(rows);
				if(rowValue.contains(" "))
					{
						rowValue = rowValue.replace(" ","_");
					}
				if(count<storeNameList.size()-1)
				{
					
					queryString = queryString.concat(rowValue + "_Qty").concat(",");
					queryString = queryString.concat(rowValue + "_Value").concat(",");
					valueString = valueString .concat(":"+rowValue + "_Qty").concat(",");
					valueString = valueString .concat(":"+rowValue + "_Value").concat(",");
					
				}
				else
				{
					queryString = queryString.concat(rowValue + "_Qty").concat(",");
					queryString = queryString.concat(rowValue + "_Value"+")");
					valueString = valueString .concat(":"+rowValue + "_Qty").concat(",");
					valueString = valueString .concat(":"+rowValue + "_Value"+")");
				}
				count++;
	
			}
			
			

			// Saving into sw_fiscal_stock table
				queryString = queryString.concat(valueString);
				//log4j.info("here is the query:"+queryString);
				SQLQuery insertQueryForFiscalStock = session.createSQLQuery(queryString);

			
			//set the values
			insertQueryForFiscalStock.setString("Item_Code",itemCode);
			insertQueryForFiscalStock.setString("Model_Code", modelCode);
			insertQueryForFiscalStock.setString("Model_Name", modelName);
			insertQueryForFiscalStock.setDouble("DSI_Qty",totalStockInDSI);
			insertQueryForFiscalStock.setDouble("DSI_Value",fiscalStockValueOfDSI);
			insertQueryForFiscalStock.setDouble("DMI_Qty",totalStockInDMI);
			insertQueryForFiscalStock.setDouble("DMI_Value",fiscalStockValueOfDMI);
			insertQueryForFiscalStock.setDate("date_of_report",dateOfReport);

			for (Object rows : storeNameList) {
			String rowValue = String.valueOf(rows);	
			if(rowValue.contains(" "))
					{
						rowValue = rowValue.replace(" ","_");
					}

			insertQueryForFiscalStock.setDouble(rowValue+"_Qty",stockInStoreArr.get(masterStoreMap.get(String.valueOf(rows))));
			insertQueryForFiscalStock.setDouble(rowValue+"_Value",fiscalStockValueOfStoreArr.get(masterStoreMap.get(String.valueOf(rows))));

			}



				int status = insertQueryForFiscalStock.executeUpdate();
				log4j.info(" FiscalStockTable:::Updated successfully");

			
		} catch (Exception ex) {
			log4j.info("Error Occurred while updation of sw_fiscal_stock table. Please try later."
					+ ex.getMessage());
		}

	}

	/**
	 * This method calculates the stock depriciation table
	 * @param session
	 * @param modelCode
	 * @param modelName
	 * @param stockQuantityInDSI
	 * @param mProductId
	 */
	
	// To be added the Date range
	public void updateStockDepreciationTable(Session session, String itemCode,String modelCode,
			String modelName, int stockQuantityInDSI,List swPurchaseList,Date dateOfReport) {

		try {
				Integer[] stockList= new Integer[4];
				Arrays.fill(stockList, 0);
				//check if the dsi quantity is negative and make it 0
				Integer dsiQuantity = stockQuantityInDSI;
				if(Integer.signum(dsiQuantity) == -1)
				{
					stockQuantityInDSI = 0;
					dsiQuantity = 0;
				}
				//check the fifo rule for quantities
				if(stockQuantityInDSI > 0)
				{
					if(swPurchaseList.size() != 0)
					{
							for (Object rows : swPurchaseList) {

								Object[] row = (Object[]) rows;
								Date dateOfReception = (Date) row[0];

								BigDecimal qty = (BigDecimal) row[1];
								int quantity = Integer.parseInt(qty.toString());

								log4j.info("quantity ==>" + quantity);
						
								String differenceInDates = checkDateDifference(dateOfReception,dateOfReport);
								int index = differenceInDates.indexOf('&');
								Integer years = Integer.parseInt(differenceInDates.substring(0,index));
								Integer months = Integer.parseInt(differenceInDates.substring(index+1));
								if(stockQuantityInDSI > quantity)
								{
									stockQuantityInDSI = stockQuantityInDSI - quantity;
									stockList = insertForDsiStock(years,months,stockList,quantity);
								}
								else
								{
									stockList = insertForDsiStock(years,months,stockList,stockQuantityInDSI);
									break;
								}
							}

					 }
				}
				

				// Saving into sw_stock_depreciation table
				log4j.info("saving to stock table");
				String queryString = "insert into sw_stock_depreciation (item_code, model_code, model_name,"
						+ " dsi_stock_quantity, less_than_2_months, two_to_6_months, six_to_12_months, greater_than_12_months, report_date ) "
						+ " values(:itemCode,:modelCode,:modelName,:dsiQty,:less2mnth,:twoTo6mnth, :sixTo12mnths,:gr12Mnths, :report_date )";
				SQLQuery insertQueryForStockDepreciation = session
						.createSQLQuery(queryString);
	
				insertQueryForStockDepreciation.setString("itemCode",itemCode);
				insertQueryForStockDepreciation.setString("modelCode", modelCode);
				insertQueryForStockDepreciation.setString("modelName", modelName);
				insertQueryForStockDepreciation.setInteger("dsiQty",
						dsiQuantity);
	
				insertQueryForStockDepreciation.setInteger("less2mnth",
						(stockList[0] == null?0:stockList[0]));
				insertQueryForStockDepreciation.setInteger("twoTo6mnth",
						(stockList[1] == null?0:stockList[1]));
				insertQueryForStockDepreciation.setInteger("sixTo12mnths",
						(stockList[2] == null?0:stockList[2]));
				insertQueryForStockDepreciation.setInteger("gr12Mnths",
						(stockList[3] == null?0:stockList[3]));
				insertQueryForStockDepreciation.setDate("report_date", dateOfReport);
	
				//insert into the stock depriciation report
				int status = insertQueryForStockDepreciation.executeUpdate();
				log4j.info(" StockDepreciationTable:::Updated successfully");

		} catch (Exception ex) {
			log4j.info("Error Occurred while updation of sw_stock_depreciation table. Please try later."
					+ ex);
		}

	}

	/**
	 * This method gets the organisation ids based on names
	 * @param orgName
	 * @return organisation id
	 */
	public String getOrgId(Session session,String orgName) {

		String orgID = null;
		//query to get the organisation id based on the name provided
		final Query queryToGetStoreNames = session
				.createSQLQuery("select ad_org_id from ad_org where name like :orgName");
		queryToGetStoreNames.setParameter("orgName", orgName + "%");
		List<Object> storeNameList = queryToGetStoreNames.list();

		if (storeNameList.size() != 0) {
			orgID = String.valueOf(storeNameList.get(0));
		}

		return orgID;

	}

	/**
	 * This method returns all the stores to be considered in the report
	 * @return List of storenames
	 */
	public List<Object> getAllStoresFromAdOrg(Session session,Date dateOfReport) {
		//query to get the names of organisation to be considered for the report
		final Query queryToGetStoreNames = session
				.createSQLQuery("select distinct ad.name from ad_org as ad"+
						        " left join m_warehouse as mw on ad.ad_org_id = mw.ad_org_id"+ 
						        " where ad.name <> '*' and mw.em_sw_issvr='Y' and ad.created <= :dateOfReport"+
						        " order by name ASC");
		queryToGetStoreNames.setParameter("dateOfReport", dateOfReport);
		return queryToGetStoreNames.list();

	}

	/**
	 * This method returns the storename without the Qty tag
	 * @param name
	 * @return storename
	 */
	public String getStoreNameWOQty(String name) {
		//return the storename substring
		return name.substring(0, name.indexOf("Qty")).trim();

	}

	/**
	 * This method returns the storename without the value tag
	 * @param name
	 * @return storename
	 */
	public String getStoreNameWOValue(String name) {
		//return the storename substring
		return name.substring(0, name.indexOf("Value")).trim();

	}
	
	
	/**
	 * This method checks whether 31st is 
	 * the date of report genreation else
	 * deletes the report generated on 31st of
	 * the previous month and return current date
	 * or previous month end date
	 * @param session
	 */
	public Calendar checkReportDates(Session session) {
		Calendar currentDate = Calendar.getInstance();
		Calendar lastDateOfMonth = Calendar.getInstance();
		setMidnightForCalendar(currentDate);
		setMidnightForCalendar(lastDateOfMonth);
		Calendar dateOfReport = currentDate;
		lastDateOfMonth.set(Calendar.DAY_OF_MONTH, lastDateOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
		//if the current date is less than the last date of the month delete the old records
		if(currentDate.getTime().before(lastDateOfMonth.getTime()))
		{
			lastDateOfMonth.add(Calendar.DAY_OF_MONTH,-(lastDateOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)));
			String queryForDepriciation = "delete from sw_stock_depreciation where " +
					                      "report_date in(select date_trunc('MONTH', now()) " +
					                      "- INTERVAL '1 day')";
			String queryForStockValuation = "delete from sw_fiscal_stock where " +
                    						"report_date in(select date_trunc('MONTH', now()) " +
                    						" - INTERVAL '1 day')";
			Query queryForMaxDate = session.createSQLQuery(queryForDepriciation);
			Query queryDate = session.createSQLQuery(queryForStockValuation);
			//query to delete last month records
			int returned = queryForMaxDate.executeUpdate();
			int returnedForFiscal = queryDate.executeUpdate();
			log4j.info("rows deleted in depriciation table "+returned);
			log4j.info("rows deleted in fiscal stock table"+returned);
			dateOfReport = lastDateOfMonth;
		}
		else
		{
			String queryForDepriciation = "delete from sw_stock_depreciation where " +
										  "report_date in(select date_trunc('MONTH', now()) + INTERVAL '1 month - 1 day') ";
			String queryForStockValuation = "delete from sw_fiscal_stock where " +
					                        "report_date in(select date_trunc('MONTH', now()) + INTERVAL '1 month - 1 day') ";
			Query queryForMaxDate = session.createSQLQuery(queryForDepriciation);
			Query queryDate = session.createSQLQuery(queryForStockValuation);
			//query to delete this month records
			int returned = queryForMaxDate.executeUpdate();
			int returnedForFiscal = queryDate.executeUpdate();
			log4j.info("rows deleted in depriciation table "+returned);
			log4j.info("rows deleted in fiscal stock table"+returned);
			//return last month date
		}
		
		return dateOfReport;
		
	}
	
	/**
	 * This method checks the
	 * difference between the dates
	 * in years and months
	 * @param session
	 */
	public String checkDateDifference(Date receptionDate,Date reportDate) {
		
		LocalDate lDate = new LocalDate(receptionDate.getTime());
		LocalDate localDate = new LocalDate(reportDate.getTime());	
		Period period = new Period(lDate,localDate);
		String years = Integer.toString(period.getYears());
		years = years.concat("&").concat(Integer.toString(period.getMonths()));
		//return the years and month seperated by &
		return years;
	}

	/**
	 * This method sets the 
	 * calendar dates to midnight
	 * @param calendar
	 */
	public Calendar setMidnightForCalendar(Calendar cal)
	{
		 cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
		  cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
		  cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
		  cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
		  return cal;
	}
	
	/**
	 * This method inserts values into the
	 * list to be used to store values for
	 * depriciation report
	 * @param session
	 */
	public Integer[] insertForDsiStock(int years,int months,Integer[] stockList,int dsiValue) {
		int value = 0;
		//check which value of array needs to be updated
		if(years>0)
		{
			value=3;
		}
		else
		{
			if(months <2)
			{
				value=0;
			}
			else if(months >=2 && months <6)
			{
				value=1;
			}
			else
			{
				value=2;
			}
		}
		//check if value is not zero add the value
		if(stockList[value] == 0)
		{
			stockList[value] = dsiValue;
		}
		else
		{
			stockList[value] = stockList[value]+dsiValue;
		}
		
		return stockList;
		
	}
	

}
