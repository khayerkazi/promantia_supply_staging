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
 

package com.sysfore.storewarehouse.ad_process;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Timer;

import javassist.bytecode.Descriptor.Iterator;

import javax.servlet.ServletException;
import org.hibernate.Criteria;
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
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.joda.time.LocalDate;
import com.sysfore.storewarehouse.SWEconomicValuation;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.SessionHandler;
import java.math.RoundingMode;


 * This class is used for calculating the Economical stock valuation
 

public class EconomicalStockValuation extends DalBaseProcess {

	// private final static String BATCH_SIZE = "50";
	private final static String SYSTEM_CLIENT_ID = "0";

	// private boolean isDirect;

	// private StringBuffer lastLog = new StringBuffer();
	// private StringBuffer message = new StringBuffer();

	// private String[] TableIds = null;

	private ProcessLogger logger;
	private ConnectionProvider connection;
	// private int totalProcessed = 0;

	static Logger log4j = Logger.getLogger(EconomicalStockValuation.class);

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
				// Calling the method to calculate Economic stock
				// of the previous month.
				calculateEconomicalStock(vars1, bundle);
			}
		} else {
			// Calling the method to calculate Economic stock
			// of the previous month.
			calculateEconomicalStock(vars, bundle);
		}
	}

	*//**
	 * This calulates the Economic stock for the previous month.
	 * 
	 * @param vars
	 * @param bundle
	 * @return
	 * @throws ServletException
	 *//*
	private OBError calculateEconomicalStock(VariablesSecureApp vars,
			ProcessBundle bundle) throws ServletException {

		// final String pinstanceId = bundle.getPinstanceId();
		// boolean m_deleteOldImported = true;
		// boolean m_processOrders = true;
		// final ProcessContext ctx = bundle.getContext();
		// isDirect = bundle.getChannel() == Channel.DIRECT;
		OBError myError = new OBError();
		// String clientId = bundle.getContext().getClient();
		// String orgId = bundle.getContext().getOrganization();
		// String userId = bundle.getContext().getUser();
		ConnectionProvider conn = null;
		Connection con = null;

		try {
			// int no = 0;
			// String batchno = "";

			conn = bundle.getConnection();
			con = conn.getTransactionConnection();

			log4j.info("ConnectionProvider =" + conn + "|| Connection=" + con);

			// creating the session from OBDal
			final Session session = OBDal.getInstance().getSession();

			// DELETEING ALL THE RECORDS FROM TEMP TABLES
			deleteAllTempTable(session);

			// Delete the existing record from stock table for
			// the previous month
			deleteWASTableForSameMonth(session);

			// Delete the existing record from Margin table for the previous
			// month
			deleteMarginTableForSameMonth(session);

			// Deleting existing data from report table
			clearEconomicReportTable(session);

			// Deleting existing data from report table
			clearICWDTable(session);

			// Calculate WACP and storing it into sw_economic_wacp
			// table. This is done at DSI level.
			calculateWACPAndInsertToWACPTable();

			// Calculating for margin table for stores
			List<Object> storeNameList = getAllStoresFromAdOrg(session);
			// Adding DMI as to the store name list since values
			// are required to be calculated for this.
			storeNameList.add("DMI");
			// Removing Whitefield Warehouse from the store name list
			// since value for this is not requied to calculate
			storeNameList.remove("Whitefield Warehouse");

			// Need to comment out this for store wise testing
			// List<Object> storeNameList = new LinkedList<Object>();
			// storeNameList.add("BGT");

			for (Object storeName : storeNameList) {
				// This method is used to calculate various values
				// used for the calculation of Economic report. Basically,
				// it computes the values and insert into the sw_economic_margin
				// table, from where the actual Economic values
				// will be calculated later.
				calculateAndInsertToMarginTemp(session,
						String.valueOf(storeName));
			}

			// Calculating the values for each of the different
			// categories in this method and values are getting
			// inserted to Economic valuation table, where the
			// main Economic valuation report gets generated.
			calulateAndUpdateEconomicalValuationTable(session);

			// Calculating and inserting values to IC_WD_Shrinkage table
			calulateAndUpdateInternalConsumTable(session);

			log4j.info("PROCESS EXECUTION COMPLETED SUCCESSFULLY");
			conn.releaseCommitConnection(con);

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
				conn.releaseRollbackConnection(con);
			} catch (Exception ignored) {
			}
			log4j.error(ex3.getMessage());
			throw new ServletException("@CODE=@" + ex3.getMessage());
		}

		return myError;

	}

	*//**
	 * This method is used to delete all the record for the same month i.e. for
	 * the month for which Economic calculations are being done, from the
	 * sw_weighted_avg_stock table.
	 * 
	 * @param session
	 *//*
	private void deleteWASTableForSameMonth(Session session) {
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonth = new SimpleDateFormat("MMMMMMMMM").format(cale
				.getTime());// Previous month
		String year = new SimpleDateFormat("YYYY").format(cale.getTime());

		String deleteQuery = "delete from sw_weighted_avg_stock where "
				+ " month = :previousMonth  and year = :year";
		SQLQuery deleteQueryForWAStock = session.createSQLQuery(deleteQuery);

		deleteQueryForWAStock.setParameter("previousMonth", previousMonth);
		deleteQueryForWAStock.setParameter("year", year);

		int deleteStatus = deleteQueryForWAStock.executeUpdate();
		log4j.info("Deleted records for the same month and year from "
				+ " sw_weighted_avg_stock table");

	}

	*//**
	 * This method is used to delete all the records for the same month i.e. the
	 * calculating month from the sw_economic_margin table.
	 * 
	 * @param session
	 *//*

	private void deleteMarginTableForSameMonth(Session session) {
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonth = new SimpleDateFormat("MMMMMMMMM").format(cale
				.getTime());// Previous month
		String year = new SimpleDateFormat("YYYY").format(cale.getTime());

		String deleteQuery = "delete from sw_economic_margin where "
				+ " report_month = :previousMonth  "
				+ " and report_year = :year";
		SQLQuery deleteQueryForWAStock = session.createSQLQuery(deleteQuery);

		deleteQueryForWAStock.setParameter("previousMonth", previousMonth);
		deleteQueryForWAStock.setParameter("year", year);

		int deleteStatus = deleteQueryForWAStock.executeUpdate();
		log4j.info("Deleted records for the same month and year"
				+ " from sw_economic_margin table");

	}

	*//**
	 * This method returns all the stores to be considered in the report.
	 * 
	 * @return List of storenames
	 *//*

	private List<Object> getAllStoresFromAdOrg(Session session) {
		// query to get the names of organisation to be
		// considered for the report
		final Query queryToGetStoreNames = session
				.createQuery("select name from Organization "
						+ " where name <> '*' and name <> 'IN1006' order by name asc");  //modified to exclude IN1006
		return queryToGetStoreNames.list();

	}

	*//**
	 * This method returns all the locations to be considered in the report.
	 * 
	 * @return List of locations.
	 *//*

	private List<Object> getLocationsFromWarehouse(Session session) {
		// query to get the names of organisation to be considered for the
		// report
		final Query queryToGetLocationNames = session
				.createSQLQuery("select name from m_warehouse where em_sw_isdmi = 'Y'");
		return queryToGetLocationNames.list();

	}

	*//**
	 * This method returns Only the stores to be considered in the report. This
	 * will only be considered for shrinkage, write down and Internal
	 * consumption.
	 * 
	 * @return List of storenames
	 *//*

	private List<Object> getOnlyStoresFromAdOrg(Session session) {
		final Query queryToGetOnlyStoreNames = session
				.createQuery("select name from Organization "
						+ " where sWIsstore = 'Y' and name <> 'IN1006' order by name asc");//modified to exclude IN1006
		return queryToGetOnlyStoreNames.list();

	}

	*//**
	 * This method is used for calculation of WACP from taking the cession price
	 * from m_productprice table and the corresponding values will be inserted
	 * into WACP table.
	 * 
	 *//*

	private void calculateWACPAndInsertToWACPTable() {
		try {

			// Getting a Session object for WACP calculation
			final Session session = OBDal.getInstance().getSession();

			final Query queryWACP = session
					.createSQLQuery(" select distinct m_product_id from cl_pricehistory "
							+ " where ad_org_id =(select ad_org_id from ad_org where name = '*')");

			List<String> productPriceList = queryWACP.list();

			// For current stock month and year
			Calendar calender = Calendar.getInstance();
			calender.add(Calendar.MONTH, -1);// Putting -1 to take previous
												// month
			String currentStockMonth = new SimpleDateFormat("MMMMMMMMM")
					.format(calender.getTime());// Taking previous month
			String currentStockYear = new SimpleDateFormat("YYYY")
					.format(calender.getTime());// Taking year corresponding to
												// prevoius month
			// Iterating through all the PRODUCT IDs of cl_pricehistory
			for (Object mProductPriceObj : productPriceList) {

				// A map to pass the values to the insert method
				// where required values will be inserted into the
				// SW_ECONOMIC_WACP table.
				Map<String, String> insertMapForWACPTable = new LinkedHashMap<String, String>();
				// Declaring a variable to store first week qty
				BigDecimal fstWeekQty = new BigDecimal(0);
				// Declaring a variable to store first week CP
				BigDecimal fstWeekCP = new BigDecimal(0);
				// Declaring a variable to store second week qty
				BigDecimal secdWeekQty = new BigDecimal(0);
				// Declaring a variable to store second week CP
				BigDecimal secdWeekCP = new BigDecimal(0);
				// Declaring a variable to store third week qty
				BigDecimal thrdWeekQty = new BigDecimal(0);
				// Declaring a variable to store third week CP
				BigDecimal thrdWeekCP = new BigDecimal(0);
				// Declaring a variable to store fourth week qty
				BigDecimal frthWeekQty = new BigDecimal(0);
				// Declaring a variable to store fourth week CP
				BigDecimal frthWeekCP = new BigDecimal(0);
				// Declaring a variable to store fifth week qty
				BigDecimal fifthWeekQty = new BigDecimal(0);
				// Declaring a variable to store fifth week CP
				BigDecimal fifthWeekCP = new BigDecimal(0);
				// Declaring a variable to store sixth week qty
				BigDecimal sixthWeekQty = new BigDecimal(0);
				// Declaring a variable to store sixth week CP
				BigDecimal sixthWeekCP = new BigDecimal(0);
				// Declaring a variable to store Weekly Average Cession Price
				BigDecimal WACP = new BigDecimal(0);

				// Taking the product id
				String productId = mProductPriceObj.toString();
				insertMapForWACPTable.put("m_product_id", productId);
				log4j.info("PRODUCT ID FROM M_PRODUCTPRICE TABLE::" + productId);
				log4j.info("PRODUCT ID limited to 10"+ productId);

				// Declaring a variable to store total movement qty
				BigDecimal totalMovQtyForWACP = new BigDecimal(0);

				// For 1st week:
				// Calling the method to get the CP and movementqty for
				// the specified week number. The first element of the
				// fstWeekcpAndQtyList contains the movementqty and the
				// second element contains the cession price for that week.
				List<BigDecimal> fstWeekcpAndQtyList = getCPAndQtyForWeekNumber(
						session, productId, "1");
				if (fstWeekcpAndQtyList != null
						&& fstWeekcpAndQtyList.size() > 0) {
					// First week qty
					fstWeekQty = fstWeekcpAndQtyList.get(0);
					insertMapForWACPTable.put("first_week_qty", fstWeekQty.toString());
					// First week CP
					fstWeekCP = fstWeekcpAndQtyList.get(1);
					insertMapForWACPTable.put("first_week_price", fstWeekCP.toString());

				}

				// For 2nd week:
				// Calling the method to get the CP and movementqty for
				// the specified week number. The first element of the
				// fstWeekcpAndQtyList contains the movementqty and the
				// second element contains the cession price for that week.
				List<BigDecimal> secdWeekcpAndQtyList = getCPAndQtyForWeekNumber(
						session, productId, "2");
				if (secdWeekcpAndQtyList != null
						&& secdWeekcpAndQtyList.size() > 0) {
					// Second week qty
					secdWeekQty = secdWeekcpAndQtyList.get(0);
					insertMapForWACPTable.put("second_week_qty", secdWeekQty.toString());
					// Second week CP
					secdWeekCP = secdWeekcpAndQtyList.get(1);
					insertMapForWACPTable
							.put("second_week_price", secdWeekCP.toString());

				}

				// For 3rd week:
				// Calling the method to get the CP and movementqty for
				// the specified week number. The first element of the
				// fstWeekcpAndQtyList contains the movementqty and the
				// second element contains the cession price for that week.
				List<BigDecimal> thrdWeekcpAndQtyList = getCPAndQtyForWeekNumber(
						session, productId, "3");
				if (thrdWeekcpAndQtyList != null
						&& thrdWeekcpAndQtyList.size() > 0) {
					// Third Week Qty
					thrdWeekQty = thrdWeekcpAndQtyList.get(0);
					insertMapForWACPTable.put("third_week_qty", thrdWeekQty.toString());
					// Third week CP
					thrdWeekCP = thrdWeekcpAndQtyList.get(1);
					insertMapForWACPTable
							.put("third_week_price", thrdWeekCP.toString());

				}

				// For 4th week:
				// Calling the method to get the CP and movementqty for
				// the specified week number. The first element of the
				// fstWeekcpAndQtyList contains the movementqty and the
				// second element contains the cession price for that week.
				List<BigDecimal> frthWeekcpAndQtyList = getCPAndQtyForWeekNumber(
						session, productId, "4");
				if (frthWeekcpAndQtyList != null
						&& frthWeekcpAndQtyList.size() > 0) {
					// Fourth Week Qty
					frthWeekQty = frthWeekcpAndQtyList.get(0);
					insertMapForWACPTable.put("fourth_week_qty", frthWeekQty.toString());
					// Fourth Week CP
					frthWeekCP = frthWeekcpAndQtyList.get(1);
					insertMapForWACPTable
							.put("fourth_week_price", frthWeekCP.toString());

				}

				// For 5th week:
				// Calling the method to get the CP and movementqty for
				// the specified week number. The first element of the
				// fstWeekcpAndQtyList contains the movementqty and the
				// second element contains the cession price for that week.
				List<BigDecimal> fifthWeekcpAndQtyList = getCPAndQtyForWeekNumber(
						session, productId, "5");
				if (fifthWeekcpAndQtyList != null
						&& fifthWeekcpAndQtyList.size() > 0) {
					// Fifth week Qty
					fifthWeekQty = fifthWeekcpAndQtyList.get(0);
					insertMapForWACPTable
							.put("fifth_week_qty", fifthWeekQty.toString());
					// Fifth week CP
					fifthWeekCP = fifthWeekcpAndQtyList.get(1);
					insertMapForWACPTable.put("fifth_week_price",
							fifthWeekCP.toString());

				}

				// For 6th week:
				// Calling the method to get the CP and movementqty for
				// the specified week number. The first element of the
				// fstWeekcpAndQtyList contains the movementqty and the
				// second element contains the cession price for that week.
				List<BigDecimal> sixthWeekcpAndQtyList = getCPAndQtyForWeekNumber(
						session, productId, "6");
				if (sixthWeekcpAndQtyList != null
						&& sixthWeekcpAndQtyList.size() > 0) {
					// Sixth week Qty
					sixthWeekQty = fstWeekcpAndQtyList.get(0);
					insertMapForWACPTable
							.put("sixth_week_qty", sixthWeekQty.toString());
					// Sixth week CP
					sixthWeekCP = fstWeekcpAndQtyList.get(1);
					insertMapForWACPTable.put("sixth_week_price",
							sixthWeekCP.toString());

				}
				// CPQ1 represents product of first week CP and QTY
				BigDecimal CPQ1 = (fstWeekCP.multiply(fstWeekQty));
				// CPQ2 represents product of second week CP and QTY
				BigDecimal CPQ2 = (secdWeekCP.multiply(secdWeekQty));
				// CPQ3 represents product of third week CP and QTY
				BigDecimal CPQ3 = (thrdWeekCP.multiply(thrdWeekQty));
				// CPQ4 represents product of fourth week CP and QTY
				BigDecimal CPQ4 = (frthWeekCP.multiply(frthWeekQty));
				// CPQ5 represents product of fifth week CP and QTY
				BigDecimal CPQ5 = (fifthWeekCP.multiply(fifthWeekQty));
				// CPQ6 represents product of sixth week CP and QTY
				BigDecimal CPQ6 = (sixthWeekCP.multiply(sixthWeekQty));

				// For WACP:
				// Total movement qty is sum of first week qty, second week
				// qty, third week qty, fourth week qty, fifth week qty and
				// sith week qty.
				totalMovQtyForWACP = fstWeekQty.add(secdWeekQty)
						.add(thrdWeekQty).add(frthWeekQty).add(fifthWeekQty)
						.add(sixthWeekQty);

				log4j.info("Total Movement Qty For WACP for PRODUCT ID =="
						+ productId + ", is = " + totalMovQtyForWACP);
				insertMapForWACPTable.put("totalMovQtyForWACP",
						totalMovQtyForWACP.toString());
				// Checking if total movementqty for all the weeks is ZERO
				if (totalMovQtyForWACP.compareTo(BigDecimal.ZERO) == 0) {
					// Checking if Sixth week CP is ZERO
					if (sixthWeekCP.compareTo(BigDecimal.ZERO) == 0) {
						// Checking if Fifth week CP is ZERO
						if (fifthWeekCP.compareTo(BigDecimal.ZERO) == 0) {
							// Then, taking Fourth week Cession Price as WACP
							WACP = frthWeekCP;
						} else {
							// Taking Fifth week Cession Price as WACP
							WACP = fifthWeekCP;
						}

					} else {
						// Then, taking Sixth week Cession Price as WACP
						WACP = sixthWeekCP;
					}
					// Then, if total movementqty for all the week is not ZERO
				} else if (totalMovQtyForWACP.compareTo(BigDecimal.ZERO) > 0) {
					// WACP(Weekly Average Cession Price) is
					// (sum of all weeks product of CP and QTY )/(total
					// movementqty for all the weeks)
					WACP = (CPQ1.add(CPQ2).add(CPQ3).add(CPQ4).add(CPQ5)
							.add(CPQ6)).divide(totalMovQtyForWACP, 2,
							RoundingMode.HALF_UP);
				}
				// Inserting the WACP values to map which is to be
				// inserted into WACP table
				insertMapForWACPTable.put("wacp", WACP.toString());

				// Month
				insertMapForWACPTable.put("report_month", currentStockMonth);

				// Year
				insertMapForWACPTable.put("report_year", currentStockYear);

				// Inserting into WACP table
				insertToWACPTableForEachItemOfProductPrice(session,
						insertMapForWACPTable);

				log4j.info("###INSERTED SUCCESSFULLY TO WACP TABLE###");

			}

		} catch (Exception e) {
			e.printStackTrace();
			log4j.error("Exception occurred while calculting WACP and storing values "
					+ "into sw_economic_wacp table: " + e);
		}

	}

	*//**
	 * This method is used to calculation of different values like movementqty
	 * etc and insert those values at item level into a temporary table called
	 * sw_economic_margin. At the time of total value calculation of different
	 * categories like Iternal Purchase, Internal sales etc., these data will be
	 * used to get the consolidated value for each category.
	 * 
	 * @param session
	 * @param storeName
	 *//*
	private void calculateAndInsertToMarginTemp(Session session,
			String storeName) {

		try {

			log4j.info("STORE NAME:::::::::" + storeName);
			// Selecting all the product ids from m_transaction table
			final Query queryForProductIds = session
					.createSQLQuery("select m_product_id from m_product ");
			// .createSQLQuery("select distinct m_product_id from sw_aggr_transactions");
			// .createSQLQuery("select distinct m_product_id from sw_aggr_transactions "
			// +
			// "where m_product_id = (select m_product_id from m_product where name ='1369145')");

			List<String> productList = queryForProductIds.list();

			// For previous closing stock month and year
			Calendar cale = Calendar.getInstance();
			cale.add(Calendar.MONTH, -2);// Putting -2 to take 2 months from
											// current month
			String pClosingStockMonth = new SimpleDateFormat("MMMMMMMMM")
					.format(cale.getTime());// Taking 2 months back month
			String pClosingStockYear = new SimpleDateFormat("YYYY").format(cale
					.getTime());// Taking the year corresponding to that month

			// For current stock month and year
			Calendar calender = Calendar.getInstance();
			calender.add(Calendar.MONTH, -1); // Putting -1 to take previous
												// month
			String currentStockMonth = new SimpleDateFormat("MMMMMMMMM")
					.format(calender.getTime());
			String currentStockYear = new SimpleDateFormat("YYYY")
					.format(calender.getTime());// Taking the year corresponding
												// to that month

			// Iterating each product of M_PRODUCT table
			for (Object mTransObj : productList) {
				// A map to pass the values to the insert method
				// where required values will be inserted into the
				// SW_ECONOMIC_MARGIN table.
				Map<String, String> insertMapForMarginTable = new LinkedHashMap<String, String>();
				// Taking the product Id
				String productId = mTransObj.toString();
				log4j.info("PRODUCT ID::" + productId);
				insertMapForMarginTable.put("m_product_id", productId);

				// For opening stock value: current month
				// Calling the method to get the fiscal quantity and value for
				// each product for each store wise. The first element of
				// fiscalQtyValList will contain fisal qty and second element
				// will
				// contain fiscal value. These fiscal qty and values will be
				// only for positive fiscal values.
				List<BigDecimal> fiscalQtyValList = getFiscalQtyAndValueForPRMP(
						session, productId, storeName);

				// Getting the movement qty for each product for each store
				// by considering the returns. For B2B, considering the
				// movementtype of (B2B, RB2B), for Ecommerce, considering the
				// movementtype of (ECOM, RECOM) and for Stores, considering
				// the movementtype of (SRQ, ITC, SRN).
				BigDecimal movementQty = getMovementQtyForStoresB2BEcom(
						session, productId, storeName);
				insertMapForMarginTable.put("movementqty",
						movementQty.toString());

				// Taking the WACP values from SW_ECONOMIC_WACP table
				BigDecimal WACP = getWACPForProduct(session, productId);
				insertMapForMarginTable.put("wacp", WACP.toString());

				// Direct purchase value: at Item level
				// Calling the method to get the direct purchase value for
				// stores and DMI for each product for each store level.
				String directPurchaseValue = getPurchaseAmountForStoresAndDMI(
						session, storeName, productId);
				insertMapForMarginTable.put("dpurchase_value",
						directPurchaseValue);

				// Direct purchase qty: at item level
				// Calling the method to get the direct purchase qty for
				// stores and DMI for each product for each store level.
				String directPurchaseQty = getPurchaseQtyForStoresAndDMI(
						session, storeName, productId);
				insertMapForMarginTable.put("dpurchase_qty", directPurchaseQty);

				// Declaring gross Quantity which represents
				// Purchase DMI+Direct purchase
				BigDecimal grossQty = new BigDecimal(0);
				// For purchase qty + Direct purchase Qty:
				if ("DMI".equalsIgnoreCase(storeName)) {
					// For DMI, grossQty is same as Direct purchase qty
					grossQty = new BigDecimal(directPurchaseQty);
				} else {
					// For store, grossQty is addition of
					// movementQty and directPurchaseQty
					grossQty = movementQty
							.add(new BigDecimal(directPurchaseQty));
				}
				insertMapForMarginTable.put("gross_purchase_qty",
						grossQty.toString());

				// Gross purchase value: Internal purchase of store + direct
				// purchase value
				BigDecimal totalPurchaseValue = new BigDecimal(0);
				BigDecimal grossPurchaseval = new BigDecimal(0);

				if ("DMI".equalsIgnoreCase(storeName)) {
					// For DMI, Gross purchase value is Direct purchase value
					// i.e. values of purchases made by DMi for the month
					grossPurchaseval = new BigDecimal(directPurchaseValue);
				} else {

					// Taking WACP value and multiplying with movementqty
					// to get total purchase values from DMI
					totalPurchaseValue = movementQty.multiply(WACP);
					// Gross purchase values is the addition of purchases value
					// from DMI and Total purchases values from purchase DB i.e.
					// Direct purchase value
					grossPurchaseval = totalPurchaseValue.add(new BigDecimal(
							directPurchaseValue));
				}

				// Declaring a variable to store logistic recharge amount
				BigDecimal logRechargeRate = new BigDecimal(0);
				BigDecimal logRechargeAmount = new BigDecimal(0);
				if ("DMI".equalsIgnoreCase(storeName)) {
					// For DMI, Fetching the Logistic recharge from
					// the monthly aggregate table of m_transaction
					// at each product level.
					logRechargeRate = selectLogRechCostForDMI(productId);
					logRechargeAmount = logRechargeRate.multiply(grossQty);
					insertMapForMarginTable.put("logistic_recharge",
							logRechargeAmount.toString());
				} else {
					// For stores, the Logistic recharge value is ZERO.
					insertMapForMarginTable.put("logistic_recharge", "0");
				}

				// Declaring a variable to store Import recharge amount
				BigDecimal importRecharge = new BigDecimal(0);
				if ("DMI".equalsIgnoreCase(storeName)) {
					// For DMI, fetching the Import recharge value from
					// SW_PURCHASE table with type as 'IMPORTATION' for
					// each product.
					importRecharge = getIMRecharge(session, productId);
					insertMapForMarginTable.put("import_recharge",
							importRecharge.toString());
				} else {
					// For stores, the Import recharge value is ZERO.
					insertMapForMarginTable.put("import_recharge", "0");
				}

				// Declaring a variable to store Hedging Loss/Gain amount
				BigDecimal getHedgingLGAtItemLevel = new BigDecimal(0);
				if ("DMI".equalsIgnoreCase(storeName)) {
					// For DMI, fetching the Hedging Loss/Gain value from
					// SW_PURCHASE table with each product. This is being
					// calculated for both EURO and USD.
					getHedgingLGAtItemLevel = getHedgingLGAtItemLevel(session,
							productId);
					log4j.info("Hedging LG At Item Level is:"
							+ getHedgingLGAtItemLevel);
					insertMapForMarginTable.put("hedging_loss",
							getHedgingLGAtItemLevel.toString());
				} else {
					// For stores, the Hedging loss/gain value is ZERO.
					insertMapForMarginTable.put("hedging_loss", "0");
				}

				// Inserting current Month into the MARGIN table
				insertMapForMarginTable.put("report_month", currentStockMonth);

				// Inserting current year into the MARGIN table
				insertMapForMarginTable.put("report_year", currentStockYear);

				// For previous month closing stock qty: Passing 2 months before
				// month
				BigDecimal pCQty = getpCQty(session, productId, storeName,
						pClosingStockMonth, pClosingStockYear);
				
				
				//BigDecimal pCQty = new BigDecimal(5);

				// For previous month closing stock value: Passing 2 months
				// before month
				// Fetching the value from sw_weighted_monthly_avg table
				// for each product, for each store level
				BigDecimal pCSVal = getpCSVal(session, productId, storeName,
						pClosingStockMonth, pClosingStockYear);
				
				//BigDecimal pCSVal = new BigDecimal(10);

				// Declaring a variable to store PRMP unit value.
				BigDecimal PRMPUnitVal = new BigDecimal(0);

				if ("DMI".equalsIgnoreCase(storeName)) {

					// For DMI, if addition of previous month closing stock qty
					// and
					// gross qty is greater than ZERO.
					if ((pCQty.add(grossQty)).compareTo(BigDecimal.ZERO) > 0) {

						// Net Purchase value = Gross + LR+ IR + Hedging
						// Loss/Gain
						BigDecimal netPurchaseValue = grossPurchaseval
								.add(logRechargeAmount).add(importRecharge)
								.add(getHedgingLGAtItemLevel);

						// PRMP Unit Value = (Net Purchase value + Previous
						// month closing
						// stock PRMP value )/(Previous month closing stock qty
						// + Gross Qty)
						PRMPUnitVal = (netPurchaseValue.add(pCSVal)).divide(
								pCQty.add(grossQty), 2, RoundingMode.HALF_UP);
						insertMapForMarginTable.put("prmp_unit_value",
								PRMPUnitVal.toString());

					} else {
						// Assigning ZERO to PRMP Unit value
						insertMapForMarginTable.put("prmp_unit_value", "0");
					}

				} else {

					// For Stores, if addition of previous month closing stock
					// qty and
					// gross qty is greater than ZERO.
					if ((pCQty.add(grossQty)).compareTo(BigDecimal.ZERO) > 0) {
						// Net Purchase value = Gross purchase value
						BigDecimal netPurchaseValue = grossPurchaseval;

						// PRMP Unit Value = (Net Purchase value + Previous
						// month closing
						// stock PRMP value )/(Previous month closing stock qty
						// + Gross Qty)
						PRMPUnitVal = (netPurchaseValue.add(pCSVal)).divide(
								pCQty.add(grossQty), 2, RoundingMode.HALF_UP);
						insertMapForMarginTable.put("prmp_unit_value",
								PRMPUnitVal.toString());

					} else {
						// Assigning ZERO to PRMP Unit value
						insertMapForMarginTable.put("prmp_unit_value", "0");
					}

				}

				// Storing closing_stock_qty as sum of previous month
				// closing stock quantity and Gross quantity.
				insertMapForMarginTable.put("closing_stock_qty",
						pCQty.add(grossQty).toString());

				// Closing stock value is the product of PRMP Unit Value
				// and Fiscal quantity.
				BigDecimal closingStockValue = PRMPUnitVal
						.multiply(fiscalQtyValList.get(0));
				// If Closing stock value is ZERO, then taking the Fiscal
				// value as closing stock value.
				if (closingStockValue.compareTo(BigDecimal.ZERO) == 0) {
					closingStockValue = fiscalQtyValList.get(1);
				}
				insertMapForMarginTable.put("closing_stock_value",
						closingStockValue.toString());

				// Inserting Organization name into the MARGIN table
				insertMapForMarginTable.put("org_name", storeName);

				// Inserting into Weighted Average Sock table for future
				// calculation for the current month.
				insertIntoWAStockTable(session, pCQty.toString(), storeName,
						productId, closingStockValue);

				// Inserting the values into MARGIN table
				insertToMarginTableForEachItem(session, insertMapForMarginTable);

				log4j.info("###INSERTED SUCCESSFULLY TO MARGIN TABLE###");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.info("Exception occurred while calculation for  sw_economic_margin table::"
					+ ex.getMessage());
		}

	}

	*//**
	 * This method is used to get movement quantities for different stores.
	 * 
	 * @param session
	 * @param mProductId
	 * @param storeName
	 * @return BigDecimal qty
	 *//*

	private BigDecimal getMovementQtyForStoresB2BEcom(Session session,
			String mProductId, String storeName) {
		String query = null;
		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForMovementQty = null;

		if (storeName.equalsIgnoreCase("B2B")) {
			query = " select coalesce(sum(mt.movementqty*(-1)),0) as movementqty "
					+ " from sw_aggr_transactions mt where mt.movementtype "
					+ " in ('B2B') "
					+ " and mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
					+ " where name ='Saleable Omega' or name ='Saleable Whitefield')  "
					+ " and m_product_id = :mProductId ";

			queryForMovementQty = session.createSQLQuery(query);
			queryForMovementQty.setParameter("mProductId", mProductId);

			List<Object> tempMvtQtyList = queryForMovementQty.list();
			BigDecimal rB2BQty = new BigDecimal(0);
			if (tempMvtQtyList != null && tempMvtQtyList.size() > 0
					&& tempMvtQtyList.get(0) != null) {
				BigDecimal movementQty = new BigDecimal(tempMvtQtyList.get(0)
						.toString());
				rB2BQty = movementQty;
				return rB2BQty;
			} else {
				return rB2BQty;
			}

		} else if (storeName.equalsIgnoreCase("Ecommerce")) {

			query = " select coalesce(sum(mt.movementqty*(-1)),0) as movementqty "
					+ " from sw_aggr_transactions mt where mt.movementtype in ('ECOM') "
					+ " and mt.m_warehouse_id "
					+ " in (select m_warehouse_id from m_warehouse  where name ='Saleable Omega' "
					+ " or name ='Saleable Whitefield') and m_product_id = :mProductId";

			queryForMovementQty = session.createSQLQuery(query);
			queryForMovementQty.setParameter("mProductId", mProductId);

			List<Object> tempMvtQtyList = queryForMovementQty.list();
			BigDecimal rEcomQty = new BigDecimal(0);
			if (tempMvtQtyList != null && tempMvtQtyList.size() > 0
					&& tempMvtQtyList.get(0) != null) {
				BigDecimal movementQty = new BigDecimal(tempMvtQtyList.get(0)
						.toString());
				rEcomQty = movementQty;
				return rEcomQty;
			} else {
				return rEcomQty;
			}

		} else if (storeName.equalsIgnoreCase("DMI")) {
			return new BigDecimal(0);

		} else {
			query = " select sum(mt.movementqty) as movementqty "
					+ " from sw_aggr_transactions mt where mt.movementtype in ('SRQ', 'ITC', 'SRN') "
					+ " and mt.m_warehouse_id in( select m_warehouse_id from m_warehouse "
					+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
					+ " and name=:storeName) and name like '%Saleable%') and "
					+ " m_product_id = :mProductId";

			queryForMovementQty = session.createSQLQuery(query);

			queryForMovementQty.setParameter("storeName", storeName);
			queryForMovementQty.setParameter("mProductId", mProductId);

			List<Object> tempMvtQtyList = queryForMovementQty.list();
			BigDecimal storeQty = new BigDecimal(0);
			if (tempMvtQtyList != null && tempMvtQtyList.size() > 0
					&& tempMvtQtyList.get(0) != null) {
				BigDecimal movementQty = new BigDecimal(tempMvtQtyList.get(0)
						.toString());
				storeQty = movementQty;
				return storeQty;
			} else {
				return storeQty;
			}

		}

	}

	*//**
	 * This method is used to get the WACP value for the corresponding product.
	 * 
	 * @param session
	 * @param productId
	 * @return BigDecimal
	 *//*

	private BigDecimal getWACPForProduct(Session session, String productId) {

		BigDecimal WACP = new BigDecimal(0);
		String queryStr = null;
		 queryStr =
		 " select wacp from sw_economic_wacp  where m_product_id = :productId ";
		//queryStr = " select wacp from sw_economic_wacp_temp  where m_product_id = :productId ";

		final Query query = session.createSQLQuery(queryStr);
		query.setParameter("productId", productId);

		List<Object> WACPList = query.list();

		if (WACPList != null && WACPList.size() > 0) {
			if (WACPList.get(0) != null) {
				WACP = new BigDecimal(WACPList.get(0).toString());
			}
			return WACP;
		} else {
			return WACP;
		}

	}

	*//**
	 * This methos is used to get teh unit price from purchase table.
	 * 
	 * @param session
	 * @param productId
	 * @return BigDecimal unit price
	 *//*

	private BigDecimal getUnitPriceFromPurchase(Session session,
			String productId) {
		BigDecimal unitPrice = new BigDecimal(0);
		String queryStr = null;
		queryStr = " select unit_price from sw_purchase where m_product_id = :productId "
				+ " order by date_of_reception desc limit 1";

		final Query query = session.createSQLQuery(queryStr);
		query.setParameter("productId", productId);
		List<Object> unitPriceList = query.list();

		if (unitPriceList != null && unitPriceList.size() > 0) {
			if (unitPriceList.get(0) != null) {
				unitPrice = new BigDecimal(unitPriceList.get(0).toString());
				return unitPrice;
			} else {
				return unitPrice;
			}
		} else {
			return unitPrice;
		}

	}

	*//**
	 * This method returns list of fiscal quantity and value for the passed
	 * product id, for a particular store. It only returns the fiscal quantity
	 * and value if the fiscal value is positive.
	 * 
	 * @param session
	 * @param mProductId
	 * @param storeName
	 * @return List<BigDecimal> fiscal qty and value
	 *//*

	private List<BigDecimal> getFiscalQtyAndValueForPRMP(Session session,
			String mProductId, String storeName) {

		List<BigDecimal> fiscalQtyValList = new LinkedList<BigDecimal>();

		List<String> startAndEndDateArr = getStartAndEndDateArr();
		String query = null;

		switch (storeName) {
		case "IN1002":
			query = " select bgt_qty, bgt_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and  bgt_qty >= 0";
			break;
		case "IN1003":
			query = " select in1003_qty, in1003_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and in1003_qty >= 0";

			break;
		case "IN1004":
			query = " select in1004_qty, in1004_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and in1004_qty >= 0";
			break;
		case "IN1005":
			query = " select in1005_qty, in1005_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and in1005_qty >= 0";
			break;
		case "IN1001":
			query = " select sarjapur_store_qty, sarjapur_store_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and sarjapur_store_qty >= 0";
			break;
		case "DMI":
			query = " select dmi_qty, dmi_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and dmi_qty >= 0";
			break;

		default:
			query = " select dmi_qty, dmi_value from sw_fiscal_stock where item_code = "
					+ " (select name from m_product where m_product_id = :mProductId) "
					+ " and  report_date between :prevMnthStartDate and :prevMnthEndDate and dmi_qty >= 0";
		}

		final Query queryForFiscal = session.createSQLQuery(query);

		queryForFiscal.setParameter("mProductId", mProductId);
		queryForFiscal.setDate("prevMnthStartDate", new LocalDate(
				startAndEndDateArr.get(0)).toDate());
		queryForFiscal.setDate("prevMnthEndDate", new LocalDate(
				startAndEndDateArr.get(1)).toDate());

		if (queryForFiscal != null) {
			List<Object> fiscalList = queryForFiscal.list();
			if (fiscalList != null && fiscalList.size() > 0) {
				for (Object rows : fiscalList) {
					Object[] row = (Object[]) rows;
					BigDecimal qty = (BigDecimal) row[0];
					BigDecimal value = (BigDecimal) row[1];

					// Adding the qty and value into the list
					// to return it back
					fiscalQtyValList.add(qty);
					fiscalQtyValList.add(value);

				}
			} else {
				// Default assignment of qty and value to ZERO
				fiscalQtyValList.add(new BigDecimal(0));
				fiscalQtyValList.add(new BigDecimal(0));
			}
		}

		log4j.info("Fiscal Qty Val List :" + fiscalQtyValList + ", mProductId:"
				+ mProductId + ",storeName:" + storeName);
		return fiscalQtyValList;

	}

	*//**
	 * This is used to get the fiscal quatity and value from sw_fiscal_stock
	 * table.
	 * 
	 * @param session
	 * @param mProductId
	 * @param storeName
	 * @return List<BigDecimal>
	 *//*

	private List<BigDecimal> getFiscalQtyAndValue(Session session,
			String mProductId) {

		List<BigDecimal> fiscalQtyValList = new LinkedList<BigDecimal>();

		List<String> startAndEndDateArr = getStartAndEndDateArr();
		String query = " select dsi_qty, dsi_value from sw_fiscal_stock where item_code = "
				+ " (select name from m_product where m_product_id = :mProductId) "
				+ " and  report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate";

		final Query queryForFiscal = session.createSQLQuery(query);

		queryForFiscal.setParameter("mProductId", mProductId);
		queryForFiscal.setDate("prevMnthStartDate", new LocalDate(
				startAndEndDateArr.get(0)).toDate());
		queryForFiscal.setDate("prevMnthEndDate", new LocalDate(
				startAndEndDateArr.get(1)).toDate());

		if (queryForFiscal != null) {
			List<Object> fiscalList = queryForFiscal.list();
			if (fiscalList != null && fiscalList.size() > 0) {
				for (Object rows : fiscalList) {
					Object[] row = (Object[]) rows;
					BigDecimal qty = (BigDecimal) row[0];
					BigDecimal value = (BigDecimal) row[1];

					// Rounding up the values
					BigDecimal newTotalFiscalValue = value.setScale(2,
							value.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = qty.setScale(2,
							qty.ROUND_HALF_DOWN);

					// Adding the qty and value into the list
					// to return it back
					fiscalQtyValList.add(newTotalFiscalQty);
					fiscalQtyValList.add(newTotalFiscalValue);

				}
			} else {
				// Default assignment of qty and value to ZERO
				fiscalQtyValList.add(new BigDecimal(0));
				fiscalQtyValList.add(new BigDecimal(0));
			}
		}

		log4j.info("Fiscal Qty Val List :" + fiscalQtyValList + ", mProductId:"
				+ mProductId);
		return fiscalQtyValList;

	}

	*//**
	 * This method is used to get the log recharge cost for DMI.
	 * 
	 * @param mProductId
	 * @return BigDecimal log recharge value
	 *//*

	private BigDecimal selectLogRechCostForDMI(String mProductId) {
		final Session session = OBDal.getInstance().getSession();
		// get start and end date
		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		BigDecimal logRechargeAmount = new BigDecimal(0);

		final Query queryForLogRech = session
				.createSQLQuery(" select em_cl_log_rec from  m_product  " +
						" where m_product_id = :mProductId");
		queryForLogRech.setParameter("mProductId", mProductId);

		List<Object> storeLogRechList = queryForLogRech.list();

		if (storeLogRechList != null && storeLogRechList.size() > 0) {
			if (storeLogRechList.get(0) != null) {
				logRechargeAmount = new BigDecimal(storeLogRechList.get(0)
						.toString());
				return logRechargeAmount;
			} else {
				return logRechargeAmount;
			}
		} else {
			return logRechargeAmount;
		}

	}

	*//**
	 * This methos is used to insert the values into sw_weighted_avg_stock table
	 * for a particular month and year to keep those for future reference.
	 * 
	 * @param session
	 * @param pCQty
	 * @param storeName
	 * @param mProductId
	 * @param closingStockValue
	 *//*

	private void insertIntoWAStockTable(Session session, String pCQty,
			String storeName, String mProductId, BigDecimal closingStockValue) {

		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonth = new SimpleDateFormat("MMMMMMMMM").format(cale
				.getTime());
		String year = new SimpleDateFormat("YYYY").format(cale.getTime());

		String queryString = "insert into sw_weighted_avg_stock(item_code, qty, "
				+ " value, organization_name, month, year) values(:item_code, :qty, "
				+ " :value, :organization_name, :month, :year )";
		SQLQuery insertQueryForWAStock = session.createSQLQuery(queryString);
		try {

			if (storeName.equalsIgnoreCase("B2B")
					|| storeName.equalsIgnoreCase("Ecommerce")) {
				// No need to insert any values for B2B and Ecommerce
				log4j.info("NOT BEEN INSERTED FOR B2B AND ECOMMERCE!!!!!");

			} else {

				insertQueryForWAStock.setString("item_code", mProductId);
				insertQueryForWAStock.setBigDecimal("qty",
						new BigDecimal(pCQty));
				insertQueryForWAStock.setBigDecimal("value", closingStockValue);
				insertQueryForWAStock.setString("organization_name", storeName);
				insertQueryForWAStock.setString("month", previousMonth);
				insertQueryForWAStock.setString("year", year);

				int status = insertQueryForWAStock.executeUpdate();
				log4j.info("Inserted Record intO sw_weighted_average_stock table");
			}

		} catch (Exception ex) {
			log4j.error("Exception happend while saving into Weighted Average Stock table::"
					+ ex);
		}

	}

	*//**
	 * This method is used to get the purchase closing quantity from
	 * sw_weighted_avg_stock table.
	 * 
	 * @param session
	 * @param mProductId
	 * @param storeName
	 * @param prevMonth
	 * @param prevYear
	 * @return BigDecimal
	 *//*

	private BigDecimal getpCQty(Session session, String mProductId,
			String storeName, String prevMonth, String prevYear) {

		BigDecimal openStocQty = new BigDecimal(0);

		final Query query = session
				.createSQLQuery("select qty from sw_weighted_monthly_avg  "
						+ " where item_code = :mProductId and month = :prevMonth and year = :prevYear and "
						+ " organization_name = :storeName");

		query.setParameter("mProductId", mProductId);
		query.setParameter("prevMonth", prevMonth);
		query.setParameter("prevYear", prevYear);
		query.setParameter("storeName", storeName);

		List<Object> openingStockList = query.list();
		if (openingStockList != null && openingStockList.size() > 0) {
			if (openingStockList.get(0) != null) {
				openStocQty = new BigDecimal(openingStockList.get(0).toString());
				if (openStocQty.compareTo(BigDecimal.ZERO) < 0) {
					openStocQty = new BigDecimal(0);
				}
				return openStocQty;
			} else {
				return openStocQty;
			}

		} else {
			return openStocQty;
		}

	}

	*//**
	 * This method is used to get the purchase closing value from
	 * sw_weighted_avg_stock
	 * 
	 * @param session
	 * @param mProductId
	 * @param storeName
	 * @param prevMonth
	 * @param prevYear
	 * @return BigDecimal
	 *//*

	private BigDecimal getpCSVal(Session session, String mProductId,
			String storeName, String prevMonth, String prevYear) {

		BigDecimal openStocVal = new BigDecimal(0);

		final Query query = session
				.createSQLQuery("select value from sw_weighted_monthly_avg  "
						+ " where item_code = :mProductId and month = :prevMonth and year = :prevYear and "
						+ " organization_name = :storeName");

		query.setParameter("mProductId", mProductId);
		query.setParameter("prevMonth", prevMonth);
		query.setParameter("prevYear", prevYear);
		query.setParameter("storeName", storeName);

		List<Object> openingStockList = query.list();
		if (openingStockList != null && openingStockList.size() > 0) {
			if (openingStockList.get(0) != null) {
				openStocVal = new BigDecimal(openingStockList.get(0).toString());
				if (openStocVal.compareTo(BigDecimal.ZERO) < 0) {
					openStocVal = new BigDecimal(0);
				}
				return openStocVal;
			} else {
				return openStocVal;
			}
		} else {
			return openStocVal;
		}

	}

	*//**
	 * This method is used to insert the WACP and weekly CP and quantity into a
	 * temporary table called sw_economic_wacp for future reference.
	 * 
	 * @param session
	 * @param insertMapForWACPTable
	 *//*
	private void insertToWACPTableForEachItemOfProductPrice(Session session,
			Map<String, String> insertMapForWACPTable) {

		String queryString = "insert into sw_economic_wacp(m_product_id, first_week_price, first_week_qty,"
				+ " second_week_price, second_week_qty, third_week_price, third_week_qty, fourth_week_price, fourth_week_qty, fifth_week_price, fifth_week_qty, "
				+ " sixth_week_price, sixth_week_qty, total_movement_qty, wacp, report_month, report_year,"
				+ " created, updated) values(:m_product_id, :first_week_price, :first_week_qty, :second_week_price, "
				+ " :second_week_qty, :third_week_price, :third_week_qty, :fourth_week_price, :fourth_week_qty, :fifth_week_price, :fifth_week_qty, :sixth_week_price,"
				+ " :sixth_week_qty, :total_movement_qty, :wacp, :report_month, :report_year,"
				+ " :created, :updated)";
		SQLQuery insertQueryForEconomicWACP = session
				.createSQLQuery(queryString);
		try {

			insertQueryForEconomicWACP.setString("m_product_id",
					replaceNull(insertMapForWACPTable.get("m_product_id")));
			insertQueryForEconomicWACP.setBigDecimal(
					"first_week_price",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("first_week_price"))));
			insertQueryForEconomicWACP.setBigDecimal("first_week_qty", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("first_week_qty"))));
			insertQueryForEconomicWACP.setBigDecimal(
					"second_week_price",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("second_week_price"))));
			insertQueryForEconomicWACP.setBigDecimal("second_week_qty", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("second_week_qty"))));
			insertQueryForEconomicWACP.setBigDecimal(
					"third_week_price",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("third_week_price"))));
			insertQueryForEconomicWACP.setBigDecimal("third_week_qty", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("third_week_qty"))));
			insertQueryForEconomicWACP.setBigDecimal(
					"fourth_week_price",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("fourth_week_price"))));
			insertQueryForEconomicWACP.setBigDecimal("fourth_week_qty", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("fourth_week_qty"))));
			insertQueryForEconomicWACP.setBigDecimal(
					"fifth_week_price",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("fifth_week_price"))));
			insertQueryForEconomicWACP.setBigDecimal("fifth_week_qty", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("fifth_week_qty"))));
			insertQueryForEconomicWACP.setBigDecimal(
					"sixth_week_price",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("sixth_week_price"))));
			insertQueryForEconomicWACP.setBigDecimal("sixth_week_qty", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("sixth_week_qty"))));
			insertQueryForEconomicWACP.setBigDecimal(
					"total_movement_qty",
					new BigDecimal(replaceNull(insertMapForWACPTable
							.get("totalMovQtyForWACP"))));
			insertQueryForEconomicWACP.setBigDecimal("wacp", new BigDecimal(
					replaceNull(insertMapForWACPTable.get("wacp"))));

			insertQueryForEconomicWACP.setString("report_month",
					replaceNull(insertMapForWACPTable.get("report_month")));
			insertQueryForEconomicWACP.setString("report_year",
					replaceNull(insertMapForWACPTable.get("report_year")));
			insertQueryForEconomicWACP.setDate("created", new Date());
			insertQueryForEconomicWACP.setDate("updated", new Date());

			int status = insertQueryForEconomicWACP.executeUpdate();

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.error("Exception happend while saving into sw_economic_wacp table::"
					+ ex);
		}

	}

	*//**
	 * This method is used to insert the values into the sw_economic_margin
	 * table. The values are store in thios table for future reference.
	 * 
	 * @param session
	 * @param insertMapForMarginTable
	 *//*

	private void insertToMarginTableForEachItem(Session session,
			Map<String, String> insertMapForMarginTable) {

		String queryString = "insert into sw_economic_margin(m_product_id, movementqty, "
				+ " closing_stock_qty, closing_stock_value, "
				+ " gross_purchase_qty, logistic_recharge, import_recharge, "
				+ " hedging_loss, report_month, report_year, dpurchase_value, prmp_unit_value, org_name,"
				+ " created, updated,dpurchase_qty, wacp) values(:m_product_id, :movementqty, "
				+ " :closing_stock_qty,"
				+ " :closing_stock_value, :gross_purchase_qty, :logistic_recharge, "
				+ " :import_recharge, :hedging_loss, :report_month, :report_year, :dpurchase_value, "
				+ " :prmp_unit_value, :org_name, :created, :updated, :dpurchase_qty, :wacp)";

		SQLQuery insertQueryForEconomicMargin = session
				.createSQLQuery(queryString);
		try {

			insertQueryForEconomicMargin.setString("m_product_id",
					replaceNull(insertMapForMarginTable.get("m_product_id")));
			insertQueryForEconomicMargin.setBigDecimal(
					"movementqty",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("movementqty"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"closing_stock_qty",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("closing_stock_qty"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"closing_stock_value",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("closing_stock_value"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"gross_purchase_qty",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("gross_purchase_qty"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"logistic_recharge",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("logistic_recharge"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"import_recharge",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("import_recharge"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"hedging_loss",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("hedging_loss"))));
			insertQueryForEconomicMargin.setString("report_month",
					replaceNull(insertMapForMarginTable.get("report_month")));
			insertQueryForEconomicMargin.setString("report_year",
					replaceNull(insertMapForMarginTable.get("report_year")));
			insertQueryForEconomicMargin.setBigDecimal(
					"dpurchase_value",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("dpurchase_value"))));
			insertQueryForEconomicMargin.setBigDecimal(
					"prmp_unit_value",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("prmp_unit_value"))));

			insertQueryForEconomicMargin.setString("org_name",
					replaceNull(insertMapForMarginTable.get("org_name")));
			insertQueryForEconomicMargin.setDate("created", new Date());
			insertQueryForEconomicMargin.setDate("updated", new Date());
			insertQueryForEconomicMargin.setBigDecimal(
					"dpurchase_qty",
					new BigDecimal(replaceNull(insertMapForMarginTable
							.get("dpurchase_qty"))));

			insertQueryForEconomicMargin.setBigDecimal("wacp", new BigDecimal(
					replaceNull(insertMapForMarginTable.get("wacp"))));

			int status = insertQueryForEconomicMargin.executeUpdate();

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.error("Exception happend while saving into sw_economic_margin table::"
					+ ex);
		}

	}

	*//**
	 * This method is used to replace null by 0.
	 * 
	 * @param value
	 * @return String value
	 *//*
	private String replaceNull(String value) {
		if (value == null) {
			return new String("0");
		} else {
			return value;
		}

	}

	*//**
	 * This method is used to calculate the Hedging loss at item level.
	 * 
	 * @param session
	 * @param mProductId
	 * @return BigDecimal value
	 *//*
	private BigDecimal getHedgingLGAtItemLevel(Session session,
			String mProductId) {

		BigDecimal totalHedgingLoss = new BigDecimal(0);
		boolean isItem = true;

		try {
			BigDecimal hedgingInEuro = getHedgingInEUR(session, mProductId,
					isItem);
			BigDecimal hedgingInUSD = getHedgingInUSD(session, mProductId,
					isItem);
			BigDecimal decathRateInEur = getDRateInEur(session, mProductId,
					isItem);
			BigDecimal decathRateInUSD = getDRateInUSD(session, mProductId,
					isItem);

			totalHedgingLoss = (hedgingInEuro.add(hedgingInUSD))
					.subtract(decathRateInEur.add(decathRateInUSD));

		} catch (Exception e) {
			log4j.error("Error ocurred while calculting for Hedging Loss/Gain"
					+ " in getHedgingLGAtItemLevel() method :" + e);
		}

		return totalHedgingLoss;

	}

	*//**
	 * This method is used to get the import recharge value.
	 * 
	 * @param session
	 * @param mproductId
	 * @return BigDecimal value
	 *//*

	private BigDecimal getIMRecharge(Session session, String mproductId) {

		BigDecimal importRecharge = new BigDecimal(0);
		String cbmRate = getCBMRate();
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		final Query query = session
				.createSQLQuery("select coalesce(sum(volume),0) from sw_purchase "
						+ " where type='IMPORTATION' and m_product_id = :mProductId "
						+ " and date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate ");

		query.setParameter("mProductId", mproductId);
		query.setDate("prevMnthStartDate",
				new LocalDate(startAndEndDateArr.get(0)).toDate());
		query.setDate("prevMnthEndDate",
				new LocalDate(startAndEndDateArr.get(1)).toDate());
		List<Object> IMRechargeList = query.list();
		if (IMRechargeList != null && IMRechargeList.size() > 0) {

			BigDecimal irVol = new BigDecimal(IMRechargeList.get(0).toString());
			importRecharge = irVol.multiply(new BigDecimal(cbmRate));
		}

		return importRecharge;
	}

	*//**
	 * This method is used to get the Cesson price for each week.
	 * 
	 * @param session
	 * @param mproductId
	 * @param storeName
	 * @param weekNumber
	 * @return List<BigDecimal>
	 *//*
	private List<BigDecimal> getCPAndQtyForWeekNumber(Session session,
			String mproductId, String weekNumber) {

		List<BigDecimal> cpAndQtyList = new LinkedList<BigDecimal>();
		List<String> startAndEndDateArrForWeek = getStartAndDateListBasedOnWeekNumber(weekNumber);
		Query queryForStore = null;
		String queryStrStore = "";
		Query queryForB2BEcom = null;
		String queryStrB2BEcom = "";
		Query queryForCPAtDMI = null;
		String queryStrForCPAtDMI = "";
		BigDecimal totalQtyForStoreAndB2BEcom = new BigDecimal(0);

		log4j.info("Week Number===" + weekNumber);

		if (startAndEndDateArrForWeek != null
				&& startAndEndDateArrForWeek.size() > 0) {
			// Quantity: For stores
			queryStrStore = "select sum(movementqty) from sw_aggr_weekly_trx where ad_org_id in "
					+ " (select ad_org_id from ad_org where em_sw_isstore = 'Y' and name <> 'IN1006') and "
					+ " movementtype in('SRQ','ITC') and week =:weekStartDate and "
					+ " m_product_id = :mProductId ";

			log4j.info("weekStartDate==="
					+ new LocalDate(startAndEndDateArrForWeek.get(0)).toDate());
			log4j.info("weekEndDate==="
					+ new LocalDate(startAndEndDateArrForWeek.get(1)).toDate());

			queryForStore = session.createSQLQuery(queryStrStore);
			queryForStore.setDate("weekStartDate", new LocalDate(
					startAndEndDateArrForWeek.get(0)).toDate());

			queryForStore.setParameter("mProductId", mproductId);

			List<Object> mvtQtyStoreList = queryForStore.list();
			if (mvtQtyStoreList != null && mvtQtyStoreList.size() > 0
					&& mvtQtyStoreList.get(0) != null) {

				BigDecimal qty = new BigDecimal(mvtQtyStoreList.get(0)
						.toString());
				// Adding to sum the quantity of stores, B2B and Ecom
				totalQtyForStoreAndB2BEcom = totalQtyForStoreAndB2BEcom
						.add(qty);
			}

			// Quantity: For B2B and Ecommerce
			queryStrB2BEcom = " select coalesce(sum(movementqty)*(-1), 0) from "
					+ " sw_aggr_weekly_trx  where m_warehouse_id in (select m_warehouse_id "
					+ " from m_warehouse  where name ='Saleable Omega' or name ='Saleable Whitefield') "
					+ " and movementtype in('ECOM','B2B') and week =:weekStartDate and "
					+ " m_product_id = :mProductId";

			queryForB2BEcom = session.createSQLQuery(queryStrB2BEcom);

			queryForB2BEcom.setDate("weekStartDate", new LocalDate(
					startAndEndDateArrForWeek.get(0)).toDate());

			queryForB2BEcom.setParameter("mProductId", mproductId);

			List<Object> mvtQtyB2BECOMList = queryForB2BEcom.list();
			if (mvtQtyB2BECOMList != null && mvtQtyB2BECOMList.size() > 0
					&& mvtQtyB2BECOMList.get(0) != null) {

				BigDecimal qty = new BigDecimal(mvtQtyB2BECOMList.get(0)
						.toString());
				// Adding to sum the quantity of stores, B2B and Ecom
				totalQtyForStoreAndB2BEcom = totalQtyForStoreAndB2BEcom
						.add(qty);

			}

			// Considering CP at DMI level i.e. '*'
			queryStrForCPAtDMI = "SELECT cessionprice from cl_pricehistory  clph where"
					+ " clph.ad_org_id in (select ad_org_id from ad_org where name = '*') and "
					+ " clph.historydate < :weekEndDate  and  clph.m_product_id = :mProductId "
					+ " order by historydate desc limit 1	";

			queryForCPAtDMI = session.createSQLQuery(queryStrForCPAtDMI);
			queryForCPAtDMI.setDate("weekEndDate", new LocalDate(
					startAndEndDateArrForWeek.get(1)).toDate());
			queryForCPAtDMI.setParameter("mProductId", mproductId);

			List<Object> cpAtDMIList = queryForCPAtDMI.list();
			BigDecimal cessionPrice = new BigDecimal(0);
			if (cpAtDMIList != null && cpAtDMIList.size() > 0
					&& cpAtDMIList.get(0) != null) {

				cessionPrice = new BigDecimal(cpAtDMIList.get(0).toString());

			}
			log4j.info("weekNumber==" + weekNumber + ",cessionPrice=="
					+ cessionPrice + ", totalQtyForStoreAndB2BEcom=="
					+ totalQtyForStoreAndB2BEcom);

			cpAndQtyList.add(totalQtyForStoreAndB2BEcom);
			cpAndQtyList.add(cessionPrice);
		}

		return cpAndQtyList;

	}

	*//**
	 * This method is used to calculate and insert into the IC_DEF_Shrinkage
	 * table.
	 * 
	 * @param session
	 *//*

	private void calulateAndUpdateInternalConsumTable(Session session) {

		try {
			// get the storenames to be considered
			List<Object> onlyStoreNameList = getOnlyStoresFromAdOrg(session);
			List<Object> locationList = getLocationsFromWarehouse(session);

			log4j.info(" storeNameList:" + onlyStoreNameList.toString()
					+ "==============LocationList:" + locationList.toString());

			// For Shrinkage
			updateInternalConsumpTableForShrinkage(session, onlyStoreNameList,
					locationList);

			// For IC
			updateInternalConsumpTableForIC(session, onlyStoreNameList,
					locationList);

			// For Write down
			updateInternalConsumpTableForWD(session, onlyStoreNameList,
					locationList);

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.error("Error Occurred while updation of sw_economic_valuation table. Please try later."
					+ ex.getMessage());
		}

	}

	*//**
	 * This method is used to calculate and insert the values to economic report
	 * table.
	 * 
	 * @param session
	 *//*
	private void calulateAndUpdateEconomicalValuationTable(Session session) {

		log4j.info("In calulateAndUpdateEconomicalValuationTable method!!!");

		try {
			// Getting the storenames
			List<Object> storeNameList = getAllStoresFromAdOrg(session);

			// For External Sales: Calling the method to doing the calculation
			updateEconomicValTableForExternalSales(session, storeNameList);

			// For Sales return: Calling the method to doing the calculation
			updateEcomocValTableForSalesReturn(session, storeNameList);

			// Internal sales: Calling the method to doing the calculation
			updateEcomocValTableForInternalSales(session, storeNameList);

			// Internal purchase: Calling the method to doing the calculation
			updateEcomocValTableForInternalPurchase(session, storeNameList);

			// For Import Recharge: Calling the method to doing the calculation
			updateEcomocValTableForImportRecharge(session, storeNameList);

			// For Hegding Loss/Gain: Calling the method to doing the
			// calculation
			updateEcomocValTableForHedgingLG(session, storeNameList);

			// For Log Recharge cost: Calling the method to doing the
			// calculation
			updateEcomocValTableForLogRechCost(session, storeNameList);

			// For Purchase Amount: Calling the method to doing the calculation
			updateEcomocValTableForPurchaseAmount(session, storeNameList);

			// Initial stock: Calling the method to doing the calculation
			updateEcomocValTableForInitialStock(session, storeNameList);

			// Final Stock: Calling the method to doing the calculation
			updateEcomocValTableForFinalStock(session, storeNameList);

			// Opening fiscal stock: Calling the method to doing the calculation
			updateEcomocValTableForOpeningFiscalStock(session, storeNameList);

			// Closing fiscal stock: Calling the method to doing the calculation
			updateEcomocValTableForClosingFiscalStock(session, storeNameList);

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.error("Error Occurred while updation of sw_economic_valuation table. Please try later."
					+ ex.getMessage());
		}

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for External sales.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations list
	 *//*

	private void updateEconomicValTableForExternalSales(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of External sales
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForExternalSales = getCorrStoreValuesListForExternalSale(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding External sales values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForExtSales = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForExternalSales);

		// Calling this method to saving the External sales values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForExtSales);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Sales return.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*
	private void updateEcomocValTableForSalesReturn(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Sales return
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForSalesReturn = getCorrStoreValuesListForSalesReturn(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Sales return values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForSalesReturn = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForSalesReturn);

		// Calling this method to saving the Sales return values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForSalesReturn);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Internal Sales.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForInternalSales(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Internal Sales
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForIntSales = getCorrStoreValuesListForIntSales(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Internal Sales values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForIntSales = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForIntSales);

		// Calling this method to saving the Internal Sales values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForIntSales);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Import Recharge.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForImportRecharge(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Import Recharge
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForImpRecharge = getCorrStoreValuesListForImpRecharge(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Import Recharge values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForImpRecharge = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForImpRecharge);

		// Calling this method to saving the Import Recharge values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForImpRecharge);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Internal Purchase.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForInternalPurchase(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Internal Purchase
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForInternalPurchase = getCorrStoreValuesListForInternalPurchase(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Internal Purchase values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForInternalPurchase = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForInternalPurchase);

		// Calling this method to saving the Internal Purchase values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForInternalPurchase);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Purchase Amount.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForPurchaseAmount(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Purchase Amount
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForPurchaseAmount = getCorrStoreValuesListForPurchaseAmount(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Purchase Amount values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForPurchaseAmount = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForPurchaseAmount);

		// Calling this method to saving the Purchase Amount values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForPurchaseAmount);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Logistic Recharge Cost.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForLogRechCost(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Recharge Cost
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForLogRechCost = getCorrStoreValuesListForLogRechCost(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Recharge Cost values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForLogRechCost = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForLogRechCost);

		// Calling this method to saving the Recharge Cost values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForLogRechCost);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Hedging Loss/Gain.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForHedgingLG(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Hedging Loss/Gain
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForHedgingLG = getCorrStoreValuesListForHedgingLG(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Hedging Loss/Gain values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForHedgingLG = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForHedgingLG);

		// Calling this method to saving the Hedging Loss/Gain values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForHedgingLG);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Initial Stock.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForInitialStock(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Initial Stock
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForInitialStock = getCorrStoreValuesListForInitialStock(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Initial Stock values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForInitialStock = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForInitialStock);

		// Calling this method to saving the Initial Stock values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForInitialStock);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Final Stock.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForFinalStock(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of Final Stock
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForFinalStock = getCorrStoreValuesListForFinalStock(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding Final Stock values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForFinalStock = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForFinalStock);

		// Calling this method to saving the Final Stock values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForFinalStock);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for closing fiscal Stock.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForClosingFiscalStock(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of closing fiscal Stock
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForClosingFiscalStock = getCorrStoreValuesListForClosingFiscalStock(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding closing fiscal Stock values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForClosingFiscalStock = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForClosingFiscalStock);

		// Calling this method to saving the closing fiscal Stock values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForClosingFiscalStock);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for opening Fiscal Stock.
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateEcomocValTableForOpeningFiscalStock(Session session,
			List<Object> storeNameList) {

		// Calling the method to get a sequence of list of opening fiscal Stock
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		List<String> corrStoreValuesListForOpeningFiscalStock = getCorrStoreValuesListForOpeningFiscalStock(session);

		// Calling this method to get a LinkedHashMap which contains
		// the store name and corresponding opening fiscal Stock values
		// as the key value pair in the Map.
		Map<String, String> storeNameListAndValueMapForOpeningFiscalStock = createStoreAndValueMap(
				storeNameList, corrStoreValuesListForOpeningFiscalStock);

		// Calling this method to saving the opening fiscal Stock values
		// into the SW_ECONOMIC_VALUATION table.
		saveToEconomicValuationTable(session,
				storeNameListAndValueMapForOpeningFiscalStock);

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Shrinkage
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateInternalConsumpTableForShrinkage(Session session,
			List<Object> onlyStoreNameList, List<Object> locationList) {

		// Calling the method to get a sequence of list of Shrinkage
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		Map<String, List<BigDecimal>> insertValueMapForShrinkage = getCorrStoreValuesListForShrinkage(
				session, onlyStoreNameList, locationList);

		// Calling this method to saving the opening Shrinkage values
		// into the sw_economic_ic_def_wd table.
		saveToEconomicICDEFWDTable(session, insertValueMapForShrinkage,
				"Shrinkage");

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Internal Consumption
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateInternalConsumpTableForIC(Session session,
			List<Object> onlyStoreNameList, List<Object> locationList) {

		// Calling the method to get a sequence of list of Internal Consumption
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		Map<String, List<BigDecimal>> insertValueMapForIC = getCorrStoreValuesListForIC(
				session, onlyStoreNameList, locationList);

		// Calling this method to saving the Internal Consumption values
		// into the sw_economic_ic_def_wd table.
		saveToEconomicICDEFWDTable(session, insertValueMapForIC,
				"Internal Consumption");

	}

	*//**
	 * This method is used to do the calculations and insertion to Economic
	 * valuation table for Write down
	 * 
	 * @param session
	 * @param storeNameList
	 *            : represents all the organizations
	 *//*

	private void updateInternalConsumpTableForWD(Session session,
			List<Object> onlyStoreNameList, List<Object> locationList) {

		// Calling the method to get a sequence of list of Write Down
		// values calculated based on each organizations and put it inside
		// a LinkedList.
		Map<String, List<BigDecimal>> insertValueMapForWD = getCorrStoreValuesListForWD(
				session, onlyStoreNameList, locationList);

		// Calling this method to saving the Write Down values
		// into the sw_economic_ic_def_wd table.
		saveToEconomicICDEFWDTable(session, insertValueMapForWD, "Write Down");

	}

	*//**
	 * This method is used to create the header name and value map which will
	 * directly be used to insert the data into the Economic valuation table.
	 * 
	 * @param storeNameList
	 * @param corrStoreValues
	 * @return LinkedHashMap<String, String>
	 *//*

	private Map<String, String> createStoreAndValueMap(
			List<Object> storeNameList, List<String> corrStoreValues) {

		Map<String, String> storeNameListAndValueMap = new LinkedHashMap<String, String>();

		List<Object> headerList = new LinkedList<Object>();
		headerList.add("Description");
		headerList.add("DMI");
		for (Object storeName : storeNameList) {
			headerList.add(getFormattedHeaderName(String.valueOf(storeName)));
		}
		headerList.add("Category");

		int index = 0;
		for (Object headerName : headerList) {
			storeNameListAndValueMap.put(String.valueOf(headerName),
					corrStoreValues.get(index));
			index++;
		}

		return storeNameListAndValueMap;
	}

	*//**
	 * This method is used to use ¨_¨ between the stores names, since these
	 * headers will be used for cross tab reports.
	 * 
	 * @param storeName
	 * @return String
	 *//*

	private String getFormattedHeaderName(String storeName) {
		String formattedHeaderName = storeName.replace(" ", "_");
		return formattedHeaderName.toUpperCase();
	}

	*//**
	 * This method is used to get the stores values of External Sales into a
	 * list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForExternalSale(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);

		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("External Sales");
		corrStoreValuesList.add("0.00");
		for (Object storeName : storeNameList) {
			corrStoreValuesList
					.add(getExternalSales(String.valueOf(storeName)));
		}
		corrStoreValuesList.add("Sales");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Shrinkage into a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private Map<String, List<BigDecimal>> getCorrStoreValuesListForShrinkage(
			Session session, List<Object> onlyStoreNameList,
			List<Object> locationList) {

		checkDataAndInsertToICDEFTempTableForShrinkage(session,
				onlyStoreNameList, locationList);

		List<Object> mergedList = new LinkedList<Object>();
		mergedList.addAll(onlyStoreNameList);
		mergedList.addAll(locationList);

		Map<String, List<BigDecimal>> insertValueMapForShrinkage = new LinkedHashMap<String, List<BigDecimal>>();

		for (Object storeName : mergedList) {
			List<BigDecimal> corrStoreValuesList = new LinkedList<BigDecimal>();
			List<BigDecimal> shrinkageValueQtyList = getTotalQtyValueAndQty(
					String.valueOf(storeName), "Shrinkage");
			corrStoreValuesList.add(shrinkageValueQtyList.get(0));// Qty
			corrStoreValuesList.add(shrinkageValueQtyList.get(1));// Value

			// Putting key-value pain in the map. Key as store name
			insertValueMapForShrinkage.put(String.valueOf(storeName),
					corrStoreValuesList);

		}
		return insertValueMapForShrinkage;

	}

	*//**
	 * This method is used to get the stores values of Internal Consumption into
	 * a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private Map<String, List<BigDecimal>> getCorrStoreValuesListForIC(
			Session session, List<Object> onlyStoreNameList,
			List<Object> locationList) {

		checkDataAndInsertToICDEFTempTableForIC(session, onlyStoreNameList,
				locationList);

		List<Object> mergedList = new LinkedList<Object>();
		mergedList.addAll(onlyStoreNameList);
		mergedList.addAll(locationList);

		Map<String, List<BigDecimal>> 
			insertValueMapForIC = new LinkedHashMap<String, List<BigDecimal>>();

		for (Object storeName : mergedList) {
			List<BigDecimal> corrStoreValuesList = new LinkedList<BigDecimal>();
			List<BigDecimal> ICValueQtyList = getTotalQtyValueAndQty(
					String.valueOf(storeName), "Internal Consumption");
			corrStoreValuesList.add(ICValueQtyList.get(0));
			corrStoreValuesList.add(ICValueQtyList.get(1));

			// Putting key-value pain in the map. Key as store name
			insertValueMapForIC.put(String.valueOf(storeName),
					corrStoreValuesList);

		}
		return insertValueMapForIC;
	}

	*//**
	 * This method is used to get the stores values of Write Down into a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private Map<String, List<BigDecimal>> getCorrStoreValuesListForWD(
			Session session, List<Object> onlyStoreNameList,
			List<Object> locationList) {

		checkDataAndInsertToICDEFTempTableForWD(session, onlyStoreNameList,
				locationList);

		List<Object> mergedList = new LinkedList<Object>();
		mergedList.addAll(onlyStoreNameList);
		mergedList.addAll(locationList);

		Map<String, List<BigDecimal>> 
			insertValueMapForWD = new LinkedHashMap<String, List<BigDecimal>>();

		for (Object storeName : mergedList) {
			List<BigDecimal> corrStoreValuesList = new LinkedList<BigDecimal>();
			List<BigDecimal> WDValueQtyList = getTotalQtyValueAndQty(
					String.valueOf(storeName), "Write Down");
			corrStoreValuesList.add(WDValueQtyList.get(0));
			corrStoreValuesList.add(WDValueQtyList.get(1));

			// Putting key-value pain in the map. Key as store name
			insertValueMapForWD.put(String.valueOf(storeName),
					corrStoreValuesList);

		}
		return insertValueMapForWD;

	}

	*//**
	 * This method is used to fetch the shrinkage values from
	 * sw_shrinkage_ic_wd_temp table.
	 * 
	 * @param storeName
	 * @return String
	 *//*

	private List<BigDecimal> getTotalQtyValueAndQty(String storeName,
			String category) {

		final Session session = OBDal.getInstance().getSession();
		List<BigDecimal> valueQtyList = new LinkedList<BigDecimal>();
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String prevMonth = new SimpleDateFormat("MMMMMMMMM").format(cale
				.getTime());
		String year = new SimpleDateFormat("YYYY").format(cale.getTime());

		final Query query = session
				.createSQLQuery("select coalesce(sum(qty),0) as qty, coalesce(sum(total_value),0) as value"
						+ " from sw_shrinkage_ic_wd_temp where "
						+ " org_name = :storeName and report_month = :prevMonth and report_year = :year "
						+ " and category = :category ");

		query.setParameter("storeName", storeName);
		query.setParameter("prevMonth", prevMonth);
		query.setParameter("year", year);
		query.setParameter("category", category);

		List<Object> storeShrinkageList = query.list();

		if (storeShrinkageList != null && storeShrinkageList.size() > 0) {

			for (Object rows : storeShrinkageList) {
				Object[] row = (Object[]) rows;
				BigDecimal qty = new BigDecimal(0);
				BigDecimal value = new BigDecimal(0);
				if (row[0] != null) {
					qty = (BigDecimal) row[0];
				}
				if (row[1] != null) {
					value = (BigDecimal) row[1];
				}
				valueQtyList.add(qty);
				valueQtyList.add(value);
			}
		}

		log4j.info(storeName + category + " Qty  Value is :========="
				+ valueQtyList);
		return valueQtyList;
	}

	*//**
	 * This method is used to fetch and calculate the External Sales.
	 * 
	 * @param storeName
	 * @return String
	 *//*

	private String getExternalSales(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		String storeExtSalesValue = "0.00";
		List<String> startAndEndDateArr = getStartAndEndDateArr();

		final Query queryForSWPurchase = session
				.createSQLQuery("select round((sum(coalesce((col.em_ds_unitqty*col.priceactual),0))+"
						+ " sum(coalesce((col.em_ds_lotqty*col.em_ds_lotprice),0))+"
						+ " sum(coalesce((col.em_ds_boxqty*col.em_ds_boxprice),0))),2)as turnover,"
						+ " sum(em_ds_taxamount) as tax, sum(col.em_ds_unitqty+col.em_ds_lotqty+col.em_ds_boxqty)"
						+ " from c_orderline as col join m_product mp on col.m_product_id = mp.m_product_id "
						+ " inner join c_order co on col.c_order_id=co.c_order_id and co.IsSOTrx='Y' "
						+ " where col.ad_org_id=(select ad_org_id from ad_org where name = :storeName)"
						+ " and col.dateordered >= :prevMnthStartDate and col.dateordered < :prevMnthEndDate ");

		queryForSWPurchase.setParameter("storeName", storeName);

		queryForSWPurchase.setDate("prevMnthStartDate", new LocalDate(
				startAndEndDateArr.get(0)).toDate());
		queryForSWPurchase.setDate("prevMnthEndDate", new LocalDate(
				startAndEndDateArr.get(1)).toDate());

		List<Object> storeExtSalesList = queryForSWPurchase.list();

		for (Object rows : storeExtSalesList) {

			Object[] row = (Object[]) rows;
			BigDecimal turnOver = (BigDecimal) row[0];
			BigDecimal tax = (BigDecimal) row[1];

			if (turnOver != null) {
				if (turnOver.subtract(tax).compareTo(BigDecimal.ZERO) < 0) {
					storeExtSalesValue = "0.00";
				} else {
					storeExtSalesValue = turnOver.subtract(tax).toString();
				}
			}

		}
		log4j.info(storeName + " External Sales Value is :========="
				+ storeExtSalesValue + ",prevMnthStartDate="
				+ new LocalDate(startAndEndDateArr.get(0)).toDate()
				+ ",prevMnthEndDate=="
				+ new LocalDate(startAndEndDateArr.get(1)).toDate());
		return storeExtSalesValue.toString();

	}

	*//**
	 * This method is used to get the stores values of Sales Return into a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*
	private List<String> getCorrStoreValuesListForSalesReturn(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);

		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Sales Return");
		corrStoreValuesList.add("0.00");
		for (Object storeName : storeNameList) {

			selectAndInsertSRToTemp(String.valueOf(storeName));
			corrStoreValuesList.add(getSalesReturn(String.valueOf(storeName)));

		}
		corrStoreValuesList.add("Sales");

		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Sales Return into a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForLogRechCost(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		selectAndInsertLogRechCostToTemp();

		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Log Recharge Cost");
		corrStoreValuesList.add(getLogRechrCost("DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add("0.00");

		}
		corrStoreValuesList.add("Purchase");

		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of HedgingLG into a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForHedgingLG(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Hedging Loss/Gain");
		corrStoreValuesList.add(getHedgingLG("DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add("0.00");
		}
		corrStoreValuesList.add("Purchase");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Initial stock into a
	 * list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForInitialStock(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Initial Stock");
		corrStoreValuesList.add(getInitialStock(session, "DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add(getInitialStock(session,
					String.valueOf(storeName)));
		}
		corrStoreValuesList.add("Stock");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Final Stock into a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForFinalStock(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Final Stock");
		corrStoreValuesList.add(getFinalStock(session, "DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add(getFinalStock(session,
					String.valueOf(storeName)));
		}
		corrStoreValuesList.add("Stock");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Closing Fiscal Stock into
	 * a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForClosingFiscalStock(
			Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Closing Fiscal Stock");
		corrStoreValuesList.add(getClosingFiscalStock(session, "DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add(getClosingFiscalStock(session,
					String.valueOf(storeName)));
		}
		corrStoreValuesList.add("Stock");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Closing Fiscal Stock into
	 * a list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForOpeningFiscalStock(
			Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Opening Fiscal Stock");
		corrStoreValuesList.add(getOpeningFiscalStock(session, "DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add(getOpeningFiscalStock(session,
					String.valueOf(storeName)));
		}
		corrStoreValuesList.add("Stock");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Internal Sales into a
	 * list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*
	private List<String> getCorrStoreValuesListForIntSales(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);

		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Internal Sales");
		corrStoreValuesList.add(getInternalSalesForDMI(session));
		for (Object storeName : storeNameList) {

			calcIntSalesValueAndInsertToTemp(String.valueOf(storeName));
			corrStoreValuesList.add(getInternalSalesLogRecharge(String
					.valueOf(storeName)));

		}
		corrStoreValuesList.add("Sales");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Import Recharge into a
	 * list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForImpRecharge(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);

		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Import Recharge");
		corrStoreValuesList.add(getImportRecharge(session));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add("0.00");

		}
		corrStoreValuesList.add("Purchase");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Internal Purchase into a
	 * list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForInternalPurchase(
			Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);

		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Internal Purchase");
		corrStoreValuesList.add("0.00");
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add(getInternalPurchase(session,
					String.valueOf(storeName)));

		}
		corrStoreValuesList.add("Purchase");
		return corrStoreValuesList;

	}

	*//**
	 * This method is used to get the stores values of Purchase Amount into a
	 * list.
	 * 
	 * @param session
	 * @return List<String>
	 *//*

	private List<String> getCorrStoreValuesListForPurchaseAmount(Session session) {

		// get the storenames to be considered
		List<Object> storeNameList = getAllStoresFromAdOrg(session);
		List<String> corrStoreValuesList = new LinkedList<String>();
		corrStoreValuesList.add("Purchase Amount");
		corrStoreValuesList.add(getTotalPurchaseAmount(session, "DMI"));
		for (Object storeName : storeNameList) {
			corrStoreValuesList.add(getTotalPurchaseAmount(session,
					String.valueOf(storeName)));

		}
		corrStoreValuesList.add("Purchase");

		return corrStoreValuesList;

	}

	*//**
	 * This method is used to calculate and insert Sales return data to a
	 * temporary table for further calculations.
	 * 
	 * @param storeName
	 *//*

	private void selectAndInsertSRToTemp(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		List<String> karStoreList = getKarnatakaStoreList();
		Query queryForSalesReturn = null;

		try {
			if (karStoreList.contains(storeName)) {

				queryForSalesReturn = session
						.createSQLQuery("select rcc.name as ItemCode, coalesce(sum(linetotal),0) as Amount, "
								+ " coalesce(sum(refundexchangeqty),0) as Qty,ct.rate, "
								+ " ((sum(linetotal))*(ct.rate/(100+ct.rate))) as taxamount "
								+ " from rc_creditnoteline rcc join m_product mp on rcc.name= mp.name "
								+ " join c_tax ct on mp.c_taxcategory_id = ct.c_taxcategory_id "
								+ " where rcc.ad_org_id =(select ad_org_id from ad_org where name = :storeName) and "
								+ " rcc.updated >= :prevMnthStartDate and  rcc.updated < :prevMnthEndDate "
								+ " group by rcc.name ,ct.rate order by  rcc.name ");
			} else {

				queryForSalesReturn = session
						.createSQLQuery("select rcc.name as ItemCode, coalesce(sum(linetotal),0) as Amount, "
								+ " coalesce(sum(refundexchangeqty),0) as Qty,ct.rate, "
								+ " ((sum(linetotal))*(ct.rate/(100+ct.rate))) as taxamount "
								+ " from rc_creditnoteline rcc join m_product mp on rcc.name= mp.name "
								+ " join m_productprice mpp on mp.m_product_id= mpp.m_product_id join c_taxcategory ctxc on "
								+ " mpp.em_cl_taxcategory_id = ctxc.c_taxcategory_id join c_tax ct on "
								+ " ctxc.c_taxcategory_id = ct.c_taxcategory_id where  "
								+ " rcc.ad_org_id = (select ad_org_id from ad_org where name = :storeName) "
								+ " and mpp.ad_org_id=rcc.ad_org_id and rcc.updated >= :prevMnthStartDate "
								+ " and  rcc.updated < :prevMnthEndDate "
								+ " group by rcc.name ,ct.rate order by  rcc.name ");

			}

			queryForSalesReturn.setParameter("storeName", storeName);
			queryForSalesReturn.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForSalesReturn.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());

			if (queryForSalesReturn != null) {
				List<Object> storeSalesReturnList = queryForSalesReturn.list();

				if (storeSalesReturnList.size() > 0) {
					for (Object rows : storeSalesReturnList) {
						Object[] row = (Object[]) rows;
						String itemCode = (String) row[0];
						BigDecimal totalAmount = (BigDecimal) row[1];
						BigDecimal qty = (BigDecimal) row[2];
						BigDecimal vatRate = (BigDecimal) row[3];
						BigDecimal taxAmt = (BigDecimal) row[4];

						// Inserting the data into the temp table
						saveToSalesReturnTempTable(session, itemCode,
								totalAmount, qty, vatRate, taxAmt, storeName);

					}
				} else {
					// Inserting the data into the temp table if no records
					saveToSalesReturnTempTable(session, "0",
							new BigDecimal(0.0), new BigDecimal(0.0),
							new BigDecimal(0.0), new BigDecimal(0.0), storeName);
				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred while calculation for Sales return:" + e);
		}

		log4j.info("Inserted To Sales Return  Temp Table");
	}

	*//**
	 * This method is used to calculate the internal sales values and inserts
	 * into a temporary table for further calculations.
	 * 
	 * @param storeName
	 *//*
	private void calcIntSalesValueAndInsertToTemp(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		BigDecimal totalLogRechargeRate = new BigDecimal("0.00");
		Query queryForIntSales = null;

		try {
			// For B2B, getting the Logistic recharge rate at product level
			if ("B2B".equalsIgnoreCase(storeName)) {
				queryForIntSales = session
						.createSQLQuery("select mt.m_product_id, coalesce(sum(mt.movementqty),0),mp.em_cl_log_rec,"
								+ " (sum(mt.movementqty)* CAST(mp.em_cl_log_rec as numeric(10,2))) * (-1) as "
								+ " log_recharge_rate from m_transaction mt join m_product mp on"
								+ " mt.m_product_id=mp.m_product_id  where mt.em_sw_movementtype in ('B2B') "
								+ " and movementdate >= :prevMnthStartDate and movementdate < :prevMnthEndDate "
								+ " and mp.em_cl_log_rec is not null and mt.m_locator_id "
								+ " in (select ml.m_locator_id from m_locator ml where ml.m_warehouse_id "
								+ " in (select m_warehouse_id from m_warehouse  where name ='Saleable Omega' "
								+ " or name ='Saleable Whitefield')) group by mt.m_product_id, "
								+ " mp.em_cl_log_rec  order by mt.m_product_id");

				queryForIntSales.setDate("prevMnthStartDate", new LocalDate(
						startAndEndDateArr.get(0)).toDate());
				queryForIntSales.setDate("prevMnthEndDate", new LocalDate(
						startAndEndDateArr.get(1)).toDate());

			// For Ecommerce, getting the Logistic recharge rate at product level
			} else if ("Ecommerce".equalsIgnoreCase(storeName)) {
				queryForIntSales = session
						.createSQLQuery("select mt.m_product_id, coalesce(sum(mt.movementqty),0),mp.em_cl_log_rec,"
								+ " (sum(mt.movementqty)* CAST(mp.em_cl_log_rec as numeric(10,2))) * (-1) as "
								+ " log_recharge_rate from m_transaction mt join m_product mp on "
								+ " mt.m_product_id=mp.m_product_id  where mt.em_sw_movementtype in ('ECOM') "
								+ " and movementdate >= :prevMnthStartDate and movementdate < :prevMnthEndDate"
								+ " and mp.em_cl_log_rec is not null and mt.m_locator_id in "
								+ " (select ml.m_locator_id from m_locator ml where ml.m_warehouse_id"
								+ " in (select m_warehouse_id from m_warehouse  where name ='Saleable Omega' "
								+ " or name ='Saleable Whitefield')) group by mt.m_product_id, mp.em_cl_log_rec  "
								+ " order by mt.m_product_id");

				queryForIntSales.setDate("prevMnthStartDate", new LocalDate(
						startAndEndDateArr.get(0)).toDate());
				queryForIntSales.setDate("prevMnthEndDate", new LocalDate(
						startAndEndDateArr.get(1)).toDate());

			// For Stores, getting the Logistic recharge rate at product level
			} else {

				queryForIntSales = session
						.createSQLQuery("select mt.m_product_id, coalesce(sum(movementqty),0),mp.em_cl_log_rec," +
								" (sum(mt.movementqty)* CAST(mp.em_cl_log_rec as numeric(10,2))) as log_recharge_rate " +
								" from m_transaction mt join m_product mp on mt.m_product_id=mp.m_product_id " +
								" where mt.m_locator_id in (select m_locator_id from m_locator where ad_org_id " +
								" in (select ad_org_id from ad_org where name =:storeName)  and " +
								" value like 'Saleable%') and mp.em_cl_log_rec is not null and " +
								" mt.em_sw_movementtype in ('SRQ', 'ITC') and movementdate >= :prevMnthStartDate" +
								" and movementdate < :prevMnthEndDate group by mt.m_product_id,mp.em_cl_log_rec " +
								" order by mt.m_product_id");

				queryForIntSales.setParameter("storeName", storeName);
				queryForIntSales.setDate("prevMnthStartDate", new LocalDate(
						startAndEndDateArr.get(0)).toDate());
				queryForIntSales.setDate("prevMnthEndDate", new LocalDate(
						startAndEndDateArr.get(1)).toDate());

			}

			List<Object> storeIntenSalesList = queryForIntSales.list();
			log4j.info("store Intenal Sales List  size====="
					+ storeIntenSalesList.size());

			if (storeIntenSalesList != null && storeIntenSalesList.size() > 0) {
				for (Object rows : storeIntenSalesList) {

					Object[] row = (Object[]) rows;
					String itemCode = (String) row[0];
					BigDecimal totalQty = (BigDecimal) row[1];
					String logRecharge = (String) row[2];
					BigDecimal logRechargeRate = (BigDecimal) row[3];

					// Inserting the data into the temp table
					saveToInternalSalesTempTable(session, itemCode, totalQty,
							logRecharge, logRechargeRate, storeName);

				}
			} else {
				// Inserting the data into the temp table if no records
				saveToInternalSalesTempTable(session, "0", new BigDecimal(0.0),
						"0", new BigDecimal(0.0), storeName);
			}
		} catch (Exception e) {
			log4j.error("Error Ocurred in calcIntSalesValueAndInsertToTemp(), while "
					+ "calculation for Internal Sales:" + e);
		}

	}

	*//**
	 * This method is used to calculate the Logistic recharge values and inserts
	 * into a temporary table for further calculations.
	 * 
	 * @param storeName
	 *//*

	private void selectAndInsertLogRechCostToTemp() {
		final Session session = OBDal.getInstance().getSession();
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		BigDecimal totalLogRechCost = new BigDecimal("0.00");

		try {

			final Query queryForLogRech = session
					.createSQLQuery("select mt.m_product_id as item_code, coalesce(sum(movementqty),0) as qty,"
							+ " mp.em_cl_log_rec as logistic_recharge,"
							+ " (sum(movementqty) * CAST(mp.em_cl_log_rec as numeric(10,2))) as logistic_recharge_rate"
							+ " from m_transaction mt join m_product mp on mt.m_product_id=mp.m_product_id"
							+ " where mp.em_cl_log_rec is not null and mt.m_locator_id"
							+ " in( select m_locator_id from m_locator where m_warehouse_id"
							+ " in (select m_warehouse_id from m_warehouse  where name ='Saleable Omega'"
							+ " or name ='Saleable Whitefield')) and mt.em_sw_movementtype in ('DPP')"
							+ " and movementdate >= :prevMnthStartDate and movementdate < :prevMnthEndDate "
							+ " group by mt.m_product_id, mp.em_cl_log_rec order by mt.m_product_id ");

			queryForLogRech.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForLogRech.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());

			List<Object> storeLogRechList = queryForLogRech.list();

			if (storeLogRechList != null && storeLogRechList.size() > 0) {
				for (Object rows : storeLogRechList) {

					Object[] row = (Object[]) rows;
					String itemCode = (String) row[0];
					BigDecimal totalQty = (BigDecimal) row[1];
					String logRecharge = (String) row[2];
					BigDecimal logRechargeRate = (BigDecimal) row[3];

					// Inserting the data into the temp table
					saveToLogRechargeTempTable(session, itemCode, totalQty,
							logRecharge, logRechargeRate, "DMI");

				}
			} else {
				// Inserting the data into the temp table if no records
				saveToLogRechargeTempTable(session, "0", new BigDecimal(0.0),
						"0", new BigDecimal(0.0), "DMI");
			}
		} catch (Exception e) {
			log4j.error("Error Ocurred in selectAndInsertLogRechCostToTemp(), "
					+ " while calculation for Logistic Recharge cost :" + e);
		}

	}

	*//**
	 * This method is used to calculate the Shrinkage value and inserts the
	 * values at item level to a temporary table for further calculations for
	 * generating the report.
	 * 
	 * @param session
	 * @param onlyStoreNameList
	 * @param locationList
	 *//*

	private void checkDataAndInsertToICDEFTempTableForShrinkage(
			Session session, List<Object> onlyStoreNameList,
			List<Object> locationList) {
		// get start and end date
		// List<String> startAndEndDateArr = getStartAndEndDateArr();

		try {

			for (Object storeName : locationList) {
				final Query locationQuery = session
						.createSQLQuery("select m_product_id, sum(movementqty) as qty from sw_aggr_transactions mt "
								+ " where mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
								+ "  where name =:locationName) and movementtype "
								+ " in ('I', 'PI')  group by m_product_id");

				locationQuery.setParameter("locationName",
						String.valueOf(storeName));
				List<Object> outputList = locationQuery.list();

				if (outputList != null && outputList.size() > 0) {
					for (Object rows : outputList) {

						Object[] row = (Object[]) rows;
						String mProductId = (String) row[0];
						BigDecimal qty = (BigDecimal) row[1];

						selectFromInterConsTempTableLocationWiseForShrinkage(
								session, mProductId, String.valueOf(storeName));
					}

				}

			}

			for (Object storeName : onlyStoreNameList) {
				final Query storeQuery = session
						.createSQLQuery(" select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
								+ " where mt.m_warehouse_id in ( select m_warehouse_id from m_warehouse "
								+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
								+ " and name=:storeName) and name like '%Saleable%') and "
								+ " movementtype in ('I', 'PI') group by m_product_id");

				storeQuery.setParameter("storeName", String.valueOf(storeName));
				List<Object> outputList = storeQuery.list();

				if (outputList != null && outputList.size() > 0) {
					for (Object rows : outputList) {

						Object[] row = (Object[]) rows;
						String mProductId = (String) row[0];
						BigDecimal qty = (BigDecimal) row[1];

						selectFromInterConsTempTableStoreWiseForShrinkage(
								session, mProductId, String.valueOf(storeName));
					}

				}

			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in checkDateAndInsertToICDEFTempTable()::"
					+ e);
		}

	}
	
	*//**
	 * This method is used to fetch the data from monthly aggregate table
	 * and then it will call the method to insert the values into the 
	 * sw_shrinkage_ic_wd_temp table.
	 * @param session
	 * @param onlyStoreNameList
	 * @param locationList
	 *//*

	private void checkDataAndInsertToICDEFTempTableForIC(Session session,
			List<Object> onlyStoreNameList, List<Object> locationList) {

		try {

			// For all the Locations which are to be considered
			for (Object storeName : locationList) {
				final Query locationQuery = session
						.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt"
								+ "  where mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
								+ "  where name = :locationName) and movementtype in ('IC')  group by m_product_id");

				locationQuery.setParameter("locationName",
						String.valueOf(storeName));

				List<Object> outputList = locationQuery.list();

				if (outputList != null && outputList.size() > 0) {
					for (Object rows : outputList) {

						Object[] row = (Object[]) rows;
						String mProductId = (String) row[0];
						BigDecimal qty = (BigDecimal) row[1];

						selectFromInterConsTempTableLocationWiseForIC(session,
								mProductId, String.valueOf(storeName));
					}

				}

			}

			// For all the stored which are to be considered
			for (Object storeName : onlyStoreNameList) {
				final Query storeQuery = session
						.createSQLQuery(" select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
								+ " where mt.m_warehouse_id in( select m_warehouse_id from m_warehouse "
								+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
								+ " and name=:storeName) and name like '%Saleable%')  and movementtype "
								+ " in ('IC')  group by m_product_id");

				storeQuery.setParameter("storeName", String.valueOf(storeName));

				List<Object> outputList = storeQuery.list();

				if (outputList != null && outputList.size() > 0) {
					for (Object rows : outputList) {

						Object[] row = (Object[]) rows;
						String mProductId = (String) row[0];
						BigDecimal qty = (BigDecimal) row[1];

						selectFromInterConsTempTableStoreWiseForIC(session,
								mProductId, String.valueOf(storeName));
					}

				}

			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in checkDateAndInsertToICDEFTempTable()::"
					+ e);
		}

	}
	
	*//**
	 * 
	 * @param session
	 * @param onlyStoreNameList
	 * @param locationList
	 *//*

	private void checkDataAndInsertToICDEFTempTableForWD(Session session,
			List<Object> onlyStoreNameList, List<Object> locationList) {

		try {

			for (Object storeName : locationList) {
				final Query locationQuery = session
						.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
								+ " where mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
								+ " where name = :locationName) and movementtype in ('WT', 'CWT') group by m_product_id");

				locationQuery.setParameter("locationName",
						String.valueOf(storeName));
				List<Object> outputList = locationQuery.list();

				if (outputList != null && outputList.size() > 0) {
					for (Object rows : outputList) {

						Object[] row = (Object[]) rows;
						String mProductId = (String) row[0];
						BigDecimal qty = (BigDecimal) row[1];

						selectFromInterConsTempTableLocationWiseForWD(session,
								mProductId, String.valueOf(storeName));
					}

				}

			}

			for (Object storeName : onlyStoreNameList) {
				final Query storeQuery = session
						.createSQLQuery("  select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
								+ " where mt.m_warehouse_id in( select m_warehouse_id from m_warehouse "
								+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
								+ " and name=:storeName) and name like '%Saleable%' or  name like '%Customer%' ) "
								+ " and movementtype in ('WT', 'CWT') "
								+ " group by m_product_id");

				storeQuery.setParameter("storeName", String.valueOf(storeName));
				List<Object> outputList = storeQuery.list();

				if (outputList != null && outputList.size() > 0) {
					for (Object rows : outputList) {

						Object[] row = (Object[]) rows;
						String mProductId = (String) row[0];
						BigDecimal qty = (BigDecimal) row[1];

						selectFromInterConsTempTableStoreWiseForWD(session,
								mProductId, String.valueOf(storeName));
					}

				}

			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in checkDataAndInsertToICDEFTempTableForWD()::"
					+ e);
		}

	}

	private void selectFromInterConsTempTableLocationWiseForShrinkage(
			Session session, String mProductId, String storeName) {

		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		String category = "Shrinkage";
		BigDecimal totalLocationShrinkageValue = new BigDecimal(0);
		try {

			final Query query = session
					.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
							+ " where mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
							+ " where name =:locationName) and m_product_id = :mProductId and movementtype "
							+ " in ('I', 'PI') group by m_product_id");

			query.setParameter("locationName", storeName);
			query.setParameter("mProductId", mProductId);
			
			 * query.setDate("prevMnthStartDate", new
			 * LocalDate(startAndEndDateArr.get(0)).toDate());
			 * query.setDate("prevMnthEndDate", new
			 * LocalDate(startAndEndDateArr.get(1)).toDate());
			 

			List<Object> outputList = query.list();

			if (outputList != null && outputList.size() > 0) {

				for (Object rows : outputList) {

					Object[] row = (Object[]) rows;
					String productId = (String) row[0];
					BigDecimal qty = (BigDecimal) row[1];
					// Fiscal unit val from margin table
					BigDecimal totalFiscalValue = getFiscalQtyAndValue(session,
							productId).get(1);
					BigDecimal totalFiscalQty = getFiscalQtyAndValue(session,
							productId).get(0);

					// Newly
					BigDecimal newTotalFiscalValue = totalFiscalValue.setScale(
							2, totalFiscalValue.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = totalFiscalQty.setScale(2,
							totalFiscalQty.ROUND_HALF_DOWN);

					BigDecimal fiscalUnitVal = new BigDecimal(0);
					if (newTotalFiscalQty.compareTo(BigDecimal.ZERO) > 0) {
						fiscalUnitVal = newTotalFiscalValue.divide(
								newTotalFiscalQty, 2, RoundingMode.HALF_UP);
					}

					if (fiscalUnitVal.compareTo(BigDecimal.ZERO) == 0) {
						BigDecimal unitPrice = getUnitPriceFromPurchase(
								session, productId);
						if (unitPrice.compareTo(BigDecimal.ZERO) == 0) {
							BigDecimal WACP = getWACPForProduct(session,
									mProductId);
							totalLocationShrinkageValue = qty.multiply(WACP);
						} else {
							totalLocationShrinkageValue = qty
									.multiply(unitPrice);
						}
					} else {
						totalLocationShrinkageValue = qty
								.multiply(fiscalUnitVal);
					}

					insertToSWShrinkageICWDTemp(session, productId, qty,
							fiscalUnitVal, category, storeName,
							totalLocationShrinkageValue);

				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in selectFromInterConsTempTableLocationWiseForShrinkage()::"
					+ e);
		}

	}

	private void selectFromInterConsTempTableLocationWiseForIC(Session session,
			String mProductId, String storeName) {

		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		String category = "Internal Consumption";
		BigDecimal totalLocationICValue = new BigDecimal(0);
		try {

			final Query query = session
					.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt"
							+ "  where mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
							+ "  where name = :locationName) and m_product_id = :mProductId and "
							+ "  movementtype in ('IC')  group by m_product_id");

			query.setParameter("locationName", storeName);
			query.setParameter("mProductId", mProductId);
			
			 * query.setDate("prevMnthStartDate", new
			 * LocalDate(startAndEndDateArr.get(0)).toDate());
			 * query.setDate("prevMnthEndDate", new
			 * LocalDate(startAndEndDateArr.get(1)).toDate());
			 
			List<Object> outputList = query.list();

			if (outputList != null && outputList.size() > 0) {

				for (Object rows : outputList) {

					Object[] row = (Object[]) rows;
					String productId = (String) row[0];
					BigDecimal qty = (BigDecimal) row[1];
					// Fiscal unit val from margin table
					BigDecimal totalFiscalValue = getFiscalQtyAndValue(session,
							productId).get(1);
					BigDecimal totalFiscalQty = getFiscalQtyAndValue(session,
							productId).get(0);

					// Newly
					BigDecimal newTotalFiscalValue = totalFiscalValue.setScale(
							2, totalFiscalValue.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = totalFiscalQty.setScale(2,
							totalFiscalQty.ROUND_HALF_DOWN);

					BigDecimal fiscalUnitVal = new BigDecimal(0);
					if (newTotalFiscalQty.compareTo(BigDecimal.ZERO) > 0) {
						fiscalUnitVal = newTotalFiscalValue.divide(
								newTotalFiscalQty, 2, RoundingMode.HALF_UP);
					}

					if (fiscalUnitVal.compareTo(BigDecimal.ZERO) == 0) {
						BigDecimal unitPrice = getUnitPriceFromPurchase(
								session, productId);
						if (unitPrice.compareTo(BigDecimal.ZERO) == 0) {
							BigDecimal WACP = getWACPForProduct(session,
									mProductId);
							totalLocationICValue = qty.multiply(WACP);
						} else {
							totalLocationICValue = qty.multiply(unitPrice);
						}
					} else {
						totalLocationICValue = qty.multiply(fiscalUnitVal);
					}

					insertToSWShrinkageICWDTemp(session, productId, qty,
							fiscalUnitVal, category, storeName,
							totalLocationICValue);

				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in selectFromInterConsTempTableLocationWiseForIC()::"
					+ e);
		}

	}

	private void selectFromInterConsTempTableLocationWiseForWD(Session session,
			String mProductId, String storeName) {

		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		String category = "Write Down";
		BigDecimal totalLocationWDValue = new BigDecimal(0);
		try {

			final Query query = session
					.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
							+ " where mt.m_warehouse_id in (select m_warehouse_id from m_warehouse "
							+ " where name = :locationName)  and m_product_id =:mProductId and movementtype in ('WT', 'CWT') "
							+ " group by m_product_id");

			query.setParameter("locationName", storeName);
			query.setParameter("mProductId", mProductId);

			List<Object> outputList = query.list();

			if (outputList != null && outputList.size() > 0) {

				for (Object rows : outputList) {

					Object[] row = (Object[]) rows;
					String productId = (String) row[0];
					BigDecimal qty = (BigDecimal) row[1];
					// Fiscal unit val from margin table
					BigDecimal totalFiscalValue = getFiscalQtyAndValue(session,
							productId).get(1);
					BigDecimal totalFiscalQty = getFiscalQtyAndValue(session,
							productId).get(0);

					// Newly
					BigDecimal newTotalFiscalValue = totalFiscalValue.setScale(
							2, totalFiscalValue.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = totalFiscalQty.setScale(2,
							totalFiscalQty.ROUND_HALF_DOWN);

					BigDecimal fiscalUnitVal = new BigDecimal(0);
					if (newTotalFiscalQty.compareTo(BigDecimal.ZERO) > 0) {
						fiscalUnitVal = newTotalFiscalValue.divide(
								newTotalFiscalQty, 2, RoundingMode.HALF_UP);
					}

					if (fiscalUnitVal.compareTo(BigDecimal.ZERO) == 0) {
						BigDecimal unitPrice = getUnitPriceFromPurchase(
								session, productId);
						if (unitPrice.compareTo(BigDecimal.ZERO) == 0) {
							BigDecimal WACP = getWACPForProduct(session,
									mProductId);
							totalLocationWDValue = qty.multiply(WACP);
						} else {
							totalLocationWDValue = qty.multiply(unitPrice);
						}
					} else {
						totalLocationWDValue = qty.multiply(fiscalUnitVal);
					}

					insertToSWShrinkageICWDTemp(session, productId, qty,
							fiscalUnitVal, category, storeName,
							totalLocationWDValue);

				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in selectFromInterConsTempTableLocationWiseForIC()::"
					+ e);
		}

	}

	private void selectFromInterConsTempTableStoreWiseForShrinkage(
			Session session, String mProductId, String storeName) {
		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		String category = "Shrinkage";
		BigDecimal totalShrinkageValue = new BigDecimal(0);
		try {

			final Query query = session
					.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
							+ " where mt.m_warehouse_id in( select m_warehouse_id from m_warehouse "
							+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
							+ " and name=:storeName) and name like '%Saleable%') and m_product_id = :mProductId and "
							+ " movementtype in ('I', 'PI')  group by m_product_id");

			query.setParameter("storeName", storeName);
			query.setParameter("mProductId", mProductId);
			
			 * query.setDate("prevMnthStartDate", new
			 * LocalDate(startAndEndDateArr.get(0)).toDate());
			 * query.setDate("prevMnthEndDate", new
			 * LocalDate(startAndEndDateArr.get(1)).toDate());
			 

			List<Object> outputList = query.list();

			if (outputList != null && outputList.size() > 0) {

				for (Object rows : outputList) {
					Object[] row = (Object[]) rows;
					String productId = (String) row[0];
					BigDecimal qty = (BigDecimal) row[1];
					// Fiscal unit val from margin table
					BigDecimal totalFiscalValue = getFiscalQtyAndValue(session,
							productId).get(1);
					BigDecimal totalFiscalQty = getFiscalQtyAndValue(session,
							productId).get(0);

					// Newly
					BigDecimal newTotalFiscalValue = totalFiscalValue.setScale(
							2, totalFiscalValue.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = totalFiscalQty.setScale(2,
							totalFiscalQty.ROUND_HALF_DOWN);

					BigDecimal fiscalUnitVal = new BigDecimal(0);
					if (newTotalFiscalQty.compareTo(BigDecimal.ZERO) > 0) {
						fiscalUnitVal = newTotalFiscalValue.divide(
								newTotalFiscalQty, 2, RoundingMode.HALF_UP);
					}

					// Need to take the WACP instead of fiscal unit value

					BigDecimal WACP = getWACPForProduct(session, mProductId);
					if (WACP.compareTo(BigDecimal.ZERO) == 0) {
						if (fiscalUnitVal.compareTo(BigDecimal.ZERO) == 0) {
							BigDecimal unitPrice = getUnitPriceFromPurchase(
									session, productId);
							totalShrinkageValue = qty.multiply(unitPrice);
						} else {
							totalShrinkageValue = qty.multiply(fiscalUnitVal);
						}
					} else {
						totalShrinkageValue = qty.multiply(WACP);
					}

					insertToSWShrinkageICWDTemp(session, productId, qty,
							fiscalUnitVal, category, storeName,
							totalShrinkageValue);

				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in selectFromInterConsTempTableStoreWise()::"
					+ e);
		}

	}

	private void selectFromInterConsTempTableStoreWiseForIC(Session session,
			String mProductId, String storeName) {
		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		String category = "Internal Consumption";
		BigDecimal totalICValue = new BigDecimal(0);
		try {

			final Query query = session
					.createSQLQuery("select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
							+ " where mt.m_warehouse_id in( select m_warehouse_id from m_warehouse "
							+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
							+ " and name=:storeName) and name like '%Saleable%') and m_product_id = :mProductId "
							+ " and movementtype in ('IC')  group by m_product_id");

			query.setParameter("storeName", storeName);
			query.setParameter("mProductId", mProductId);
			
			 * query.setDate("prevMnthStartDate", new
			 * LocalDate(startAndEndDateArr.get(0)).toDate());
			 * query.setDate("prevMnthEndDate", new
			 * LocalDate(startAndEndDateArr.get(1)).toDate());
			 
			List<Object> outputList = query.list();

			if (outputList != null && outputList.size() > 0) {

				for (Object rows : outputList) {
					Object[] row = (Object[]) rows;
					String productId = (String) row[0];
					BigDecimal qty = (BigDecimal) row[1];
					// Fiscal unit val from margin table
					BigDecimal totalFiscalValue = getFiscalQtyAndValue(session,
							productId).get(1);
					BigDecimal totalFiscalQty = getFiscalQtyAndValue(session,
							productId).get(0);

					// Newly
					BigDecimal newTotalFiscalValue = totalFiscalValue.setScale(
							2, totalFiscalValue.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = totalFiscalQty.setScale(2,
							totalFiscalQty.ROUND_HALF_DOWN);

					BigDecimal fiscalUnitVal = new BigDecimal(0);
					if (newTotalFiscalQty.compareTo(BigDecimal.ZERO) > 0) {
						fiscalUnitVal = newTotalFiscalValue.divide(
								newTotalFiscalQty, 2, RoundingMode.HALF_UP);
					}

					BigDecimal WACP = getWACPForProduct(session, mProductId);
					if (WACP.compareTo(BigDecimal.ZERO) == 0) {
						if (fiscalUnitVal.compareTo(BigDecimal.ZERO) == 0) {
							BigDecimal unitPrice = getUnitPriceFromPurchase(
									session, productId);
							totalICValue = qty.multiply(unitPrice);
						} else {
							totalICValue = qty.multiply(fiscalUnitVal);
						}
					} else {
						totalICValue = qty.multiply(WACP);
					}

					insertToSWShrinkageICWDTemp(session, productId, qty,
							fiscalUnitVal, category, storeName, totalICValue);

				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in selectFromInterConsTempTableStoreWiseForIC()::"
					+ e);
		}

	}

	private void selectFromInterConsTempTableStoreWiseForWD(Session session,
			String mProductId, String storeName) {
		// List<String> startAndEndDateArr = getStartAndEndDateArr();
		String category = "Write Down";
		BigDecimal totalWDValue = new BigDecimal(0);
		try {

			final Query query = session
					.createSQLQuery(" select m_product_id, sum(movementqty) from sw_aggr_transactions mt "
							+ " where mt.m_warehouse_id in( select m_warehouse_id from m_warehouse "
							+ " where ad_org_id in (select ad_org_id from ad_org where em_sw_isstore='Y' "
							+ " and name=:storeName) and name like '%Saleable%' or  name like '%Customer%' ) "
							+ " and m_product_id =:mProductId  and "
							+ " movementtype in ('WT', 'CWT') group by m_product_id");

			query.setParameter("storeName", storeName);
			query.setParameter("mProductId", mProductId);
			
			 * query.setDate("prevMnthStartDate", new
			 * LocalDate(startAndEndDateArr.get(0)).toDate());
			 * query.setDate("prevMnthEndDate", new
			 * LocalDate(startAndEndDateArr.get(1)).toDate());
			 
			List<Object> outputList = query.list();

			if (outputList != null && outputList.size() > 0) {

				for (Object rows : outputList) {
					Object[] row = (Object[]) rows;
					String productId = (String) row[0];
					BigDecimal qty = (BigDecimal) row[1];
					// Fiscal unit val from margin table
					BigDecimal totalFiscalValue = getFiscalQtyAndValue(session,
							productId).get(1);
					BigDecimal totalFiscalQty = getFiscalQtyAndValue(session,
							productId).get(0);
					// Newly
					BigDecimal newTotalFiscalValue = totalFiscalValue.setScale(
							2, totalFiscalValue.ROUND_HALF_DOWN);
					BigDecimal newTotalFiscalQty = totalFiscalQty.setScale(2,
							totalFiscalQty.ROUND_HALF_DOWN);

					BigDecimal fiscalUnitVal = new BigDecimal(0);
					if (newTotalFiscalQty.compareTo(BigDecimal.ZERO) > 0) {
						fiscalUnitVal = newTotalFiscalValue.divide(
								newTotalFiscalQty, 2, RoundingMode.HALF_UP);
					}

					BigDecimal WACP = getWACPForProduct(session, mProductId);
					if (WACP.compareTo(BigDecimal.ZERO) == 0) {
						if (fiscalUnitVal.compareTo(BigDecimal.ZERO) == 0) {
							BigDecimal unitPrice = getUnitPriceFromPurchase(
									session, productId);
							totalWDValue = qty.multiply(unitPrice);
						} else {
							totalWDValue = qty.multiply(fiscalUnitVal);
						}
					} else {
						totalWDValue = qty.multiply(WACP);
					}

					insertToSWShrinkageICWDTemp(session, productId, qty,
							fiscalUnitVal, category, storeName, totalWDValue);

				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in selectFromInterConsTempTableStoreWiseForWD()::"
					+ e);
		}

	}

	private void insertToSWShrinkageICWDTemp(Session session,
			String mProductId, BigDecimal qty, BigDecimal fiscalUnitVal,
			String category, String orgName, BigDecimal totalValue) {

		BigDecimal newFiscalUnitVal = fiscalUnitVal.setScale(2,
				fiscalUnitVal.ROUND_HALF_DOWN);
		BigDecimal newQty = qty.setScale(2, qty.ROUND_HALF_DOWN);
		BigDecimal newTotalValue = totalValue.setScale(2,
				totalValue.ROUND_HALF_DOWN);

		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonth = new SimpleDateFormat("MMMMMMMMM").format(cale
				.getTime());
		String year = new SimpleDateFormat("YYYY").format(cale.getTime());

		String queryString = "insert into sw_shrinkage_ic_wd_temp(m_product_id, qty, "
				+ " fiscal_unit_value, category, org_name, report_month, report_year, total_value) values(:m_product_id, :qty, "
				+ " :fiscal_unit_value, :category, :org_name, :report_month, :report_year, :total_value)";
		SQLQuery insertToSWShrinkageICWD = session.createSQLQuery(queryString);
		try {

			insertToSWShrinkageICWD.setString("m_product_id", mProductId);
			insertToSWShrinkageICWD.setBigDecimal("qty", newQty);
			insertToSWShrinkageICWD.setBigDecimal("fiscal_unit_value",
					newFiscalUnitVal);
			insertToSWShrinkageICWD.setString("category", category);
			insertToSWShrinkageICWD.setString("org_name", orgName);
			insertToSWShrinkageICWD.setString("report_month", previousMonth);
			insertToSWShrinkageICWD.setString("report_year", year);
			insertToSWShrinkageICWD.setBigDecimal("total_value", newTotalValue);

			int status = insertToSWShrinkageICWD.executeUpdate();
			log4j.info("Inserted Record int sw_shrinkage_ic_wd_temp table");

		} catch (Exception ex) {
			log4j.error("Exception happend while saving into sw_shrinkage_ic_wd_temp table::"
					+ ex);
		}

	}

	private String getSalesReturn(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		String storeSalesReturnValue = null;

		try {
			final Query queryForSalesReturnValue = session
					.createSQLQuery("select (sum(total_amount)-sum(tax_amount)) as sales_value "
							+ "from sw_sales_return_temp where org_name= :storeName");
			queryForSalesReturnValue.setParameter("storeName", storeName);

			if (queryForSalesReturnValue != null) {
				List<Object> storeSalesReturnValueList = queryForSalesReturnValue
						.list();
				storeSalesReturnValue = storeSalesReturnValueList.get(0)
						.toString();
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred while fetching for Sales return from temp table. Please try Later:"
					+ e);
		}
		log4j.info(storeName + "  Sales Return Value is :========="
				+ storeSalesReturnValue);
		return storeSalesReturnValue;

	}

	private String getLogRechrCost(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		String storeLogRechrCostValue = null;

		try {
			final Query queryForLogRechrCost = session
					.createSQLQuery("select coalesce(sum(log_recharge_rate),0) as log_recharge_cost "
							+ "from sw_log_recharge_cost_temp where org_name= :storeName");
			queryForLogRechrCost.setParameter("storeName", storeName);

			if (queryForLogRechrCost != null) {
				List<Object> storeLogRechrCostList = queryForLogRechrCost
						.list();

				if (storeLogRechrCostList != null
						&& storeLogRechrCostList.size() > 0) {

					if (storeLogRechrCostList.get(0) == null) {
						storeLogRechrCostValue = "0.00";
					} else {
						storeLogRechrCostValue = storeLogRechrCostList.get(0)
								.toString();

					}
				}
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred while fetching for Log Recharge Cost from "
					+ " temp table :" + e);
		}
		log4j.info(storeName + "  Log Recharge Cost is :========="
				+ storeLogRechrCostValue);
		return storeLogRechrCostValue;

	}

	private String getHedgingLG(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		String storeHedgingLGValue = null;
		BigDecimal totalHedgingLoss = new BigDecimal(0);
		String mProductId = "";
		boolean isItem = false;

		try {
			BigDecimal hedgingInEuro = getHedgingInEUR(session, mProductId,
					isItem);
			BigDecimal hedgingInUSD = getHedgingInUSD(session, mProductId,
					isItem);
			BigDecimal decathRateInEur = getDRateInEur(session, mProductId,
					isItem);
			BigDecimal decathRateInUSD = getDRateInUSD(session, mProductId,
					isItem);

			totalHedgingLoss = (hedgingInEuro.subtract(decathRateInEur))
					.add(hedgingInUSD.subtract(decathRateInUSD));
			storeHedgingLGValue = totalHedgingLoss.toString();
		} catch (Exception e) {
			log4j.error("Error ocurred while calculting for Hedging Loss/Gain in getHedgingLG() method :"
					+ e);
		}
		log4j.info(storeName + "  Hedging loss/gain is :========="
				+ storeHedgingLGValue);
		return storeHedgingLGValue;

	}

	private String getInitialStock(Session session, String storeName) {

		String storeInitialStock = null;
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.YEAR, -1);
		String previousYear = new SimpleDateFormat("YYYY").format(cale
				.getTime());

		BigDecimal storeInitialStockValue = new BigDecimal(0);
		try {
			final Query query = session
					.createSQLQuery("select coalesce(sum(value),0) "
							+ "from sw_weighted_avg_stock where organization_name = :storeName "
							+ "and month = 'December' and year = :previousYear");
			query.setParameter("storeName", storeName);
			query.setParameter("previousYear", previousYear);

			if (query != null) {
				List<Object> storeInitialStockList = query.list();
				if (storeInitialStockList != null
						&& storeInitialStockList.size() > 0) {
					storeInitialStockValue = new BigDecimal(
							storeInitialStockList.get(0).toString());
					storeInitialStock = storeInitialStockValue.toString();
				}
			}

		} catch (Exception e) {
			log4j.error("Error ocurred while calculting for Initial Stock in getInitialStock() :"
					+ e);
		}
		log4j.info(storeName + "  store Initial Stock is :========="
				+ storeInitialStock);
		return storeInitialStock;

	}

	private String getFinalStock(Session session, String storeName) {

		String storeFinalStock = null;
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonthString = new SimpleDateFormat("MMMMMMMMM")
				.format(cale.getTime());
		String yearString = new SimpleDateFormat("YYYY").format(cale.getTime());
		BigDecimal storeFinalStockValue = new BigDecimal(0);
		String queryStr = null;
		Query query = null;

		try {

			if ("Whitefield Warehouse".equalsIgnoreCase(storeName)) {

				return "0";

			} else if ("B2B".equalsIgnoreCase(storeName)) {
				return "0";
			} else if ("Ecommerce".equalsIgnoreCase(storeName)) {
				return "0";
			} else {
				queryStr = "select coalesce(sum(closing_stock_value),0) "
						+ " as final_stock from sw_economic_margin where org_name = :storeName and "
						+ " report_month = :previousMonthString and report_year = :yearString";
			}

			query = session.createSQLQuery(queryStr);
			query.setParameter("storeName", storeName);
			query.setParameter("previousMonthString", previousMonthString);
			query.setParameter("yearString", yearString);

			if (query != null) {
				List<Object> storeFinalStockList = query.list();
				if (storeFinalStockList != null
						&& storeFinalStockList.size() > 0) {
					storeFinalStockValue = new BigDecimal(storeFinalStockList
							.get(0).toString());
					storeFinalStock = storeFinalStockValue.toString();
				}
			}

		} catch (Exception e) {
			log4j.error("Error ocurred while calculting for Final Stock  in getFinalStock() method :"
					+ e);
		}
		log4j.info(storeName + "  Final Stock is :=========" + storeFinalStock);
		return storeFinalStock;

	}

	*//**
	 * 
	 * @param session
	 * @param storeName
	 * @return
	 *//*

	private String getClosingFiscalStock(Session session, String storeName) {

		String storeCFStock = null;
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		String queryStr = null;
		Query query = null;
		BigDecimal storeCFStockval = new BigDecimal(0);
		try {

			switch (storeName) {
			case "BGT":
				queryStr = " select sum(bgt_value) from sw_fiscal_stock where "
						+ "report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate and bgt_qty > 0 ";
				break;
			case "IN1003":
				queryStr = " select sum(in1003_value) from sw_fiscal_stock where "
						+ "report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate and in1003_qty > 0 ";
				break;
			case "IN1004":
				queryStr = " select sum(in1004_value) from sw_fiscal_stock where "
						+ "report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate and in1004_qty > 0 ";
				break;
			case "IN1005":
				queryStr = " select sum(in1005_value) from sw_fiscal_stock where "
						+ "report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate and in1005_qty > 0 ";
				break;
			case "Sarjapur Store":
				queryStr = " select sum(sarjapur_store_value) from sw_fiscal_stock where "
						+ "report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate and sarjapur_store_qty > 0 ";
				break;
			case "DMI":
				queryStr = " select sum(dmi_value) from sw_fiscal_stock where "
						+ "report_date >= :prevMnthStartDate and report_date < :prevMnthEndDate and dmi_qty > 0 ";
				break;

			default:
				return "0";
			}

			query = session.createSQLQuery(queryStr);
			query.setDate("prevMnthStartDate",
					new LocalDate(startAndEndDateArr.get(0)).toDate());
			query.setDate("prevMnthEndDate",
					new LocalDate(startAndEndDateArr.get(1)).toDate());
			if (query != null) {
				List<Object> storeCFStockList = query.list();
				if (storeCFStockList != null && storeCFStockList.size() > 0) {

					storeCFStockval = new BigDecimal(storeCFStockList.get(0)
							.toString());
					storeCFStock = storeCFStockval.toString();
				}
			}

		} catch (Exception e) {
			log4j.error("Error ocurred while calculting for Final Stock  in getClosingFiscalStock() method :"
					+ e);
		}
		log4j.info(storeName + "  Closing Fiscal Stock is :========="
				+ storeCFStock);
		return storeCFStock;

	}

	*//**
	 * 
	 * @param session
	 * @param storeName
	 * @return
	 *//*

	private String getOpeningFiscalStock(Session session, String storeName) {

		Calendar cale = Calendar.getInstance();
		// cale.add(Calendar.MONTH, -1);
		// String previousMonth = new SimpleDateFormat("MMMMMMMMM")
		// .format(cale.getTime());
		String yearString = new SimpleDateFormat("YYYY").format(cale.getTime());
		String storeOFStock = "0.00";
		String queryStr = null;
		Query query = null;
		BigDecimal storeOFStockval = new BigDecimal(0);
		try {

			queryStr = " select value from sw_opening_fiscal where org_name= :storeName and "
					+ " year = :yearString ";

			query = session.createSQLQuery(queryStr);
			query.setString("storeName", storeName);
			// query.setString("previousMonth",previousMonth);
			query.setString("yearString", yearString);

			List<Object> storeOFStockList = query.list();
			if (storeOFStockList != null && storeOFStockList.size() > 0) {

				storeOFStockval = new BigDecimal(storeOFStockList.get(0)
						.toString());
				storeOFStock = storeOFStockval.toString();
			}

		} catch (Exception e) {
			log4j.error("Error ocurred while calculting OpeningFiscalStock() method :"
					+ e);
		}
		log4j.info(storeName + "  Opening Fiscal Stock is :========="
				+ storeOFStock);
		return storeOFStock;

	}

	private BigDecimal getHedgingInEUR(Session session, String mproductId,
			boolean isItem) {
		BigDecimal hedgingInEuro = new BigDecimal(0);
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForHedgingLGInEur = null;
		String euroRate = getEURORate();

		if (!isItem) {
			queryForHedgingLGInEur = session
					.createSQLQuery("select coalesce((sum(invoice_fob)),0) as eur_hedge_rate "
							+ " from sw_purchase where type= 'IMPORTATION' and currency = 'EUR' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate ");

			queryForHedgingLGInEur.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForHedgingLGInEur.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());
		} else {

			queryForHedgingLGInEur = session
					.createSQLQuery("select coalesce((sum(invoice_fob)),0) as eur_hedge_rate "
							+ " from sw_purchase where type= 'IMPORTATION' and currency = 'EUR' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate "
							+ " and m_product_id = :mproductId ");

			queryForHedgingLGInEur.setParameter("mproductId", mproductId);

			queryForHedgingLGInEur.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForHedgingLGInEur.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());
		}
		if (queryForHedgingLGInEur != null) {
			List<Object> storeHedgingLGList = queryForHedgingLGInEur.list();
			if (storeHedgingLGList != null && storeHedgingLGList.size() > 0) {
				hedgingInEuro = ((BigDecimal) storeHedgingLGList.get(0))
						.multiply(new BigDecimal(euroRate));
			}
		}
		return hedgingInEuro;
	}

	private BigDecimal getHedgingInUSD(Session session, String mproductId,
			boolean isItem) {
		BigDecimal hedgingInUSD = new BigDecimal(0);
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForHedgingLGInUSD = null;
		String usdRate = getUSDRate();

		if (!isItem) {
			queryForHedgingLGInUSD = session
					.createSQLQuery("select coalesce((sum(invoice_fob)),0) as usd_hedge_rate "
							+ " from sw_purchase where type= 'IMPORTATION' and currency = 'USD' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate ");

			queryForHedgingLGInUSD.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForHedgingLGInUSD.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());

		} else {

			queryForHedgingLGInUSD = session
					.createSQLQuery("select coalesce((sum(invoice_fob)),0) as usd_hedge_rate "
							+ " from sw_purchase where type= 'IMPORTATION' and currency = 'USD' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate "
							+ " and m_product_id = :mproductId ");

			queryForHedgingLGInUSD.setParameter("mproductId", mproductId);
			queryForHedgingLGInUSD.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForHedgingLGInUSD.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());
		}

		if (queryForHedgingLGInUSD != null) {
			List<Object> storeHedgingLGList = queryForHedgingLGInUSD.list();
			if (storeHedgingLGList != null && storeHedgingLGList.size() > 0) {
				hedgingInUSD = ((BigDecimal) storeHedgingLGList.get(0))
						.multiply(new BigDecimal(usdRate));

			}
		}
		return hedgingInUSD;
	}

	private BigDecimal getDRateInEur(Session session, String mproductId,
			boolean isItem) {
		BigDecimal decathRateInEur = new BigDecimal(0);
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForDecathLGInEUR = null;

		if (!isItem) {
			queryForDecathLGInEUR = session
					.createSQLQuery("select coalesce(sum(invoice_value),0) as eur_decathlon_rate  "
							+ " from sw_purchase where type= 'IMPORTATION' and currency ='EUR' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate ");

			queryForDecathLGInEUR.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForDecathLGInEUR.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());
		} else {
			queryForDecathLGInEUR = session
					.createSQLQuery("select coalesce(sum(invoice_value),0) as eur_decathlon_rate  "
							+ " from sw_purchase where type= 'IMPORTATION' and currency ='EUR' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate "
							+ " and m_product_id = :mproductId");
			queryForDecathLGInEUR.setParameter("mproductId", mproductId);

			queryForDecathLGInEUR.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForDecathLGInEUR.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());

		}
		if (queryForDecathLGInEUR != null) {
			List<Object> storeHedgingLGList = queryForDecathLGInEUR.list();
			if (storeHedgingLGList != null && storeHedgingLGList.size() > 0) {
				decathRateInEur = (BigDecimal) storeHedgingLGList.get(0);

			}
		}
		return decathRateInEur;
	}

	private BigDecimal getDRateInUSD(Session session, String mproductId,
			boolean isItem) {
		BigDecimal decathRateInUSD = new BigDecimal(0);
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForDecathLGInUSD = null;

		if (!isItem) {
			queryForDecathLGInUSD = session
					.createSQLQuery("select coalesce(sum(invoice_value),0) as usd_decathlon_rate "
							+ " from sw_purchase where type= 'IMPORTATION' and currency = 'USD' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate ");

			queryForDecathLGInUSD.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForDecathLGInUSD.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());
		} else {
			queryForDecathLGInUSD = session
					.createSQLQuery("select coalesce(sum(invoice_value),0) as usd_decathlon_rate "
							+ " from sw_purchase where type= 'IMPORTATION' and currency = 'USD' and "
							+ " date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate "
							+ " and m_product_id = :mproductId");

			queryForDecathLGInUSD.setParameter("mproductId", mproductId);

			queryForDecathLGInUSD.setDate("prevMnthStartDate", new LocalDate(
					startAndEndDateArr.get(0)).toDate());
			queryForDecathLGInUSD.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());
		}

		if (queryForDecathLGInUSD != null) {
			List<Object> storeHedgingLGList = queryForDecathLGInUSD.list();
			if (storeHedgingLGList != null && storeHedgingLGList.size() > 0) {
				decathRateInUSD = (BigDecimal) storeHedgingLGList.get(0);

			}
		}
		return decathRateInUSD;
	}

	private String getImportRecharge(Session session) {

		String storeImportRechargeValue = null;
		String cbmRate = getCBMRate();
		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		try {
			final Query queryForImportRechargeValue = session
					.createSQLQuery("select coalesce(sum(volume),0) from sw_purchase where type='IMPORTATION'"
							+ " and date_of_reception >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate ");

			queryForImportRechargeValue.setDate("prevMnthStartDate",
					new LocalDate(startAndEndDateArr.get(0)).toDate());
			queryForImportRechargeValue.setDate("prevMnthEndDate",
					new LocalDate(startAndEndDateArr.get(1)).toDate());

			List<Object> storeImportRechargeValueList = queryForImportRechargeValue
					.list();

			BigDecimal IMValue = new BigDecimal(storeImportRechargeValueList
					.get(0).toString()).multiply(new BigDecimal(cbmRate));
			storeImportRechargeValue = IMValue.toString();

		} catch (Exception e) {
			log4j.error("Error Ocurred in getImportRecharge():" + e);
		}
		log4j.info("DMI  Import Recharge Value is :========="
				+ storeImportRechargeValue);
		return storeImportRechargeValue;

	}

	private String getInternalSalesForDMI(Session session) {

		String intSalesDMIVal = null;
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonthString = new SimpleDateFormat("MMMMMMMMM")
				.format(cale.getTime());
		String yearString = new SimpleDateFormat("YYYY").format(cale.getTime());

		try {
			final Query queryForIntSales = session
					.createSQLQuery("select coalesce(sum(movementqty*wacp),0) as int_purchase "
							+ " from sw_economic_margin where org_name not in ('DMI') and "
							+ " report_month = :previousMonthString and report_year = :yearString");

			queryForIntSales.setString("previousMonthString",
					previousMonthString);
			queryForIntSales.setString("yearString", yearString);

			if (queryForIntSales != null) {
				List<Object> storeIntSalesValueList = queryForIntSales.list();
				BigDecimal IntSalesValue = new BigDecimal(
						storeIntSalesValueList.get(0).toString());
				intSalesDMIVal = IntSalesValue.toString();
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in getInternalSalesForDMI():" + e);
		}
		log4j.info("DMI  Internal sales Value is :=========" + intSalesDMIVal);
		return intSalesDMIVal;

	}

	private String getInternalPurchase(Session session, String storeName) {

		String storIntPurchaseValue = null;
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonthString = new SimpleDateFormat("MMMMMMMMM")
				.format(cale.getTime());
		String yearString = new SimpleDateFormat("YYYY").format(cale.getTime());

		try {
			final Query queryForIntPurchase = session
					.createSQLQuery("select coalesce(sum((movementqty*wacp)),0) as int_purchase "
							+ " from sw_economic_margin  where org_name = :storeName "
							+ " and report_month = :previousMonthString and report_year = :yearString ");

			queryForIntPurchase.setString("storeName", storeName);
			queryForIntPurchase.setString("previousMonthString",
					previousMonthString);
			queryForIntPurchase.setString("yearString", yearString);

			if (queryForIntPurchase != null) {
				List<Object> storequeryForIntPurchaseList = queryForIntPurchase
						.list();
				BigDecimal IntPurValue = new BigDecimal(
						storequeryForIntPurchaseList.get(0).toString());
				storIntPurchaseValue = IntPurValue.toString();
			}

		} catch (Exception e) {
			log4j.error("Error Ocurred in getInternalPurchase():" + e);
		}
		log4j.info(storeName + "-Import Recharge Value is :========="
				+ storIntPurchaseValue);
		return storIntPurchaseValue;

	}

	private String getTotalPurchaseAmount(Session session, String storeName) {

		// get start and end date
		List<String> startAndEndDateArr = getStartAndEndDateArr();

		String storTotalPurchaseAmount = null;
		Calendar cale = Calendar.getInstance();
		cale.add(Calendar.MONTH, -1);
		String previousMonthString = new SimpleDateFormat("MMMMMMMMM")
				.format(cale.getTime());
		String yearString = new SimpleDateFormat("YYYY").format(cale.getTime());

		Query queryForTotalPurchaseAmount = null;
		String queryStr = null;

		try {

			if ("Whitefield Warehouse".equalsIgnoreCase(storeName)) {
				return "0";

			} else if ("DMI".equalsIgnoreCase(storeName)) {

				queryStr = " select coalesce(sum(total_purchase),0) from sw_purchase where origin "
						+ " not like'%DIRECT PURCHASE%' and date_of_reception >= :prevMnthStartDate "
						+ " and  date_of_reception < :prevMnthEndDate ";
				queryForTotalPurchaseAmount = session.createSQLQuery(queryStr);
				queryForTotalPurchaseAmount.setDate("prevMnthStartDate",
						new LocalDate(startAndEndDateArr.get(0)).toDate());
				queryForTotalPurchaseAmount.setDate("prevMnthEndDate",
						new LocalDate(startAndEndDateArr.get(1)).toDate());

			} else {
				queryStr = "select sum(dpurchase_value) from sw_economic_margin   "
						+ " where org_name = :storeName and  report_month = :previousMonthString  and report_year = :yearString ";
				queryForTotalPurchaseAmount = session.createSQLQuery(queryStr);

				queryForTotalPurchaseAmount.setString("storeName", storeName);
				queryForTotalPurchaseAmount.setString("previousMonthString",
						previousMonthString);
				queryForTotalPurchaseAmount.setString("yearString", yearString);
			}

			if (queryForTotalPurchaseAmount != null) {
				List<Object> storeTotalPurchaseAmountList = queryForTotalPurchaseAmount
						.list();
				BigDecimal totalPurValue = new BigDecimal(
						storeTotalPurchaseAmountList.get(0).toString());
				storTotalPurchaseAmount = totalPurValue.toString();
			}

		} catch (Exception e) {
			e.printStackTrace();
			log4j.error("Error Ocurred in getTotalPurchaseAmount():" + e);
		}
		log4j.info(storeName + "-Total Purchase Amount is :========="
				+ storTotalPurchaseAmount);
		return storTotalPurchaseAmount;

	}

	private String getPurchaseAmountForStoresAndDMI(Session session,
			String storeName, String mProductId) {

		String storePurchaseAmt = "0";
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForPurchaseAmtStore = null;
		String query = null;

		try {

			switch (storeName) {
			case "Sarjapur Store":
				query = "select coalesce(sum(total_purchase),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASE' and "
						+ " date_of_reception >= :prevMnthStartDate and  date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "BGT":
				query = "select coalesce(sum(total_purchase),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASEBG' and "
						+ " date_of_reception >= :prevMnthStartDate and  date_of_reception  < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "IN1003":
				query = "select coalesce(sum(total_purchase),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASEWF' and "
						+ " date_of_reception  >= :prevMnthStartDate and date_of_reception  <  :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "IN1004":
				query = "select coalesce(sum(total_purchase),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASETH' and "
						+ " date_of_reception  >= :prevMnthStartDate and  date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "IN1005":
				query = "select coalesce(sum(total_purchase),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASEAM' and "
						+ " date_of_reception >= :prevMnthStartDate and  date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "DMI":
				query = " select coalesce(sum(total_purchase),0) from sw_purchase where origin "
						+ " not like'%DIRECT PURCHASE%' and date_of_reception >= :prevMnthStartDate "
						+ " and  date_of_reception < :prevMnthEndDate and m_product_id = :mProductId";
				break;

			default:
				return "0";
			}

			queryForPurchaseAmtStore = session.createSQLQuery(query);
			queryForPurchaseAmtStore.setParameter("mProductId", mProductId);
			queryForPurchaseAmtStore.setDate("prevMnthStartDate",
					new LocalDate(startAndEndDateArr.get(0)).toDate());
			queryForPurchaseAmtStore.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());

			if (queryForPurchaseAmtStore != null) {
				List<Object> storePurchaseAmtStoreValueList = queryForPurchaseAmtStore
						.list();
				if (storePurchaseAmtStoreValueList != null
						&& storePurchaseAmtStoreValueList.size() > 0) {
					if (storePurchaseAmtStoreValueList.get(0) == null) {
						storePurchaseAmt = "0.00";
					} else {
						storePurchaseAmt = storePurchaseAmtStoreValueList
								.get(0).toString();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log4j.info("Error Ocurred in getPurchaseAmountForStores():" + e);
		}
		log4j.info(storeName + "Purchase Amount Value is :========="
				+ storePurchaseAmt);
		return storePurchaseAmt;
	}

	private String getPurchaseQtyForStoresAndDMI(Session session,
			String storeName, String mProductId) {

		String storePurchaseQty = "0";
		List<String> startAndEndDateArr = getStartAndEndDateArr();
		Query queryForPurchaseQtyStore = null;
		String query = null;

		try {

			switch (storeName) {
			case "Sarjapur Store":
				query = "select coalesce(sum(qty),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASE' and "
						+ " date_of_reception  >= :prevMnthStartDate and  date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";

				break;
			case "BGT":
				query = "select coalesce(sum(qty),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASEBG' and "
						+ " date_of_reception  >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "IN1003":
				query = "select coalesce(sum(qty),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASEWF' and "
						+ " date_of_reception >= :prevMnthStartDate and  date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "IN1004":
				query = "select coalesce(sum(qty),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASETH' and "
						+ " date_of_reception  >= :prevMnthStartDate and  date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "IN1005":
				query = "select coalesce(sum(qty),0) "
						+ " from sw_purchase where origin = 'DIRECT PURCHASEAM' and "
						+ " date_of_reception  >= :prevMnthStartDate and date_of_reception < :prevMnthEndDate "
						+ " and m_product_id = :mProductId";
				break;
			case "DMI":
				query = " select coalesce(sum(qty),0) from sw_purchase where origin "
						+ " not like'%DIRECT PURCHASE%' and date_of_reception >= :prevMnthStartDate "
						+ " and  date_of_reception < :prevMnthEndDate and m_product_id = :mProductId";
				break;

			default:
				return "0";

			}

			queryForPurchaseQtyStore = session.createSQLQuery(query);
			queryForPurchaseQtyStore.setParameter("mProductId", mProductId);

			queryForPurchaseQtyStore.setDate("prevMnthStartDate",
					new LocalDate(startAndEndDateArr.get(0)).toDate());
			queryForPurchaseQtyStore.setDate("prevMnthEndDate", new LocalDate(
					startAndEndDateArr.get(1)).toDate());

			if (queryForPurchaseQtyStore != null) {
				List<Object> storePurchaseQtyStoreValueList = queryForPurchaseQtyStore
						.list();
				if (storePurchaseQtyStoreValueList != null
						&& storePurchaseQtyStoreValueList.size() > 0) {
					if (storePurchaseQtyStoreValueList.get(0) == null) {
						storePurchaseQty = "0.00";
					} else {
						storePurchaseQty = storePurchaseQtyStoreValueList
								.get(0).toString();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log4j.error("Error Ocurred in getPurchaseQtyForStoresAndDMI():" + e);
		}
		log4j.info(storeName + "Purchase Qty is :=========" + storePurchaseQty);
		return storePurchaseQty;
	}

	*//**
	 * This method is used to get the CBM value from reference list for further
	 * calculation.
	 * 
	 * @return String
	 *//*

	private String getCBMRate() {
		final Session session = OBDal.getInstance().getSession();
		String cbmRate = null;
		final Query queryForCBMValue = session
				.createSQLQuery("select value from ad_ref_list  where name = 'cbm_rate'");

		List<Object> storeCBMValueList = queryForCBMValue.list();
		for (Object rows : storeCBMValueList) {
			cbmRate = (String) rows;
			log4j.info(" CBM Rate :=========" + cbmRate);
		}
		return cbmRate;
	}

	*//**
	 * This method is used to get the USD value from reference list for further
	 * calculation.
	 * 
	 * @return String
	 *//*

	private String getUSDRate() {
		final Session session = OBDal.getInstance().getSession();
		String usdRate = null;
		final Query queryForUSDValue = session
				.createSQLQuery("select value from ad_ref_list  where name = 'usd_rate'");

		List<Object> storeUSDValueList = queryForUSDValue.list();
		for (Object rows : storeUSDValueList) {
			usdRate = (String) rows;
			log4j.info(" USD Rate :=========" + usdRate);
		}
		return usdRate;
	}

	*//**
	 * This method is used to get the EURO value from reference list for further
	 * calculation.
	 * 
	 * @return String
	 *//*

	private String getEURORate() {
		final Session session = OBDal.getInstance().getSession();
		String euroRate = null;
		final Query queryForEUROValue = session
				.createSQLQuery("select value from ad_ref_list  where name = 'euro_rate'");

		List<Object> storeEUROValueList = queryForEUROValue.list();
		for (Object rows : storeEUROValueList) {
			euroRate = (String) rows;
			log4j.info(" EURO Rate :=========" + euroRate);
		}
		return euroRate;
	}

	private String getInternalSalesLogRecharge(String storeName) {

		final Session session = OBDal.getInstance().getSession();
		String storeInternalSalesValue = null;
		Query queryForIntSales = null;

		if ("Sarjapur Store".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else if ("BGT".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else if ("IN1003".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else if ("IN1004".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else if ("IN1005".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else if ("B2B".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else if ("Ecommerce".equalsIgnoreCase(storeName)) {
			storeInternalSalesValue = "0.00";
		} else {

			List<Object> storeList = getAllStoresFromAdOrg(session);
			BigDecimal internalSalesVal = new BigDecimal(0.0);
			BigDecimal temp = new BigDecimal(0.0);
			for (Object store : storeList) {
				temp = getInternalSalesValueFromTemp(session,
						String.valueOf(store));
				internalSalesVal = internalSalesVal.add(temp);

			}
			storeInternalSalesValue = internalSalesVal.toString();
		}

		log4j.info(storeName + "  Internal Sales Value is :========="
				+ storeInternalSalesValue);
		return storeInternalSalesValue;

	}

	private BigDecimal getInternalSalesValueFromTemp(Session session,
			String store) {

		BigDecimal storeInternalSalesValue = new BigDecimal(0.0);

		try {
			final Query queryForInternalSalesValue = session
					.createSQLQuery("select coalesce((sum(log_recharge_rate)),0) as sales_value "
							+ " from sw_internal_sales_temp where org_name= :storeName");
			queryForInternalSalesValue.setParameter("storeName", store);

			if (queryForInternalSalesValue != null) {
				List<Object> storeInternalSalesValueList = queryForInternalSalesValue
						.list();
				storeInternalSalesValue = (BigDecimal) storeInternalSalesValueList
						.get(0);
			}

		} catch (Exception e) {
			log4j.info("Error Ocurred while fetching for Internal sales from temp table. Please try Later:"
					+ e);
		}
		return storeInternalSalesValue;

	}

	private void saveToEconomicValuationTable(Session session,
			Map<String, String> storeNameListAndValueMap) {

		long referenceNo = getReferenceNumber(session);

		java.util.Iterator<String> itr = storeNameListAndValueMap.keySet()
				.iterator();
		while (itr.hasNext()) {

			String name = (String) itr.next();
			String value = storeNameListAndValueMap.get(name);

			String queryString = "insert into sw_economic_valuation(name, value,"
					+ " reference_number, report_date, ad_client_id, ad_org_id, created, createdby, updated, updatedby,"
					+ " isactive) values(:name,:value,:reference_number, :report_date, :ad_client_id, "
					+ " :ad_org_id, :created, :createdby, :updated, :updatedby, :isactive)";

			SQLQuery insertQueryForEconomicVal = session
					.createSQLQuery(queryString);
			try {

				insertQueryForEconomicVal.setString("name", name);
				insertQueryForEconomicVal.setString("value", value);
				insertQueryForEconomicVal.setLong("reference_number",
						referenceNo);
				insertQueryForEconomicVal.setDate("report_date", new Date());
				insertQueryForEconomicVal.setString("ad_client_id", "0");
				insertQueryForEconomicVal.setString("ad_org_id", "0");
				insertQueryForEconomicVal.setDate("created", new Date());
				insertQueryForEconomicVal.setString("createdby", "0");
				insertQueryForEconomicVal.setDate("updated", new Date());
				insertQueryForEconomicVal.setString("updatedby", "0");
				insertQueryForEconomicVal.setString("isactive", "Y");

				int status = insertQueryForEconomicVal.executeUpdate();

			} catch (Exception ex) {
				log4j.error("Exception happend while saving into sw_economic_valuation table::"
						+ ex);
			}
		}

	}

	private long getReferenceNumber(Session session) {

		long referenceNo = 1;
		try {

			String hql = "select max(referenceNumber) from SW_EconomicValuation";
			Query query = session.createQuery(hql);
			List<Object> results = query.list();
			for (Object maxRef : results) {
				referenceNo = Long.valueOf(maxRef.toString()) + 1;
				log4j.info("Reference No For Valuation Table::" + referenceNo);
			}

		} catch (Exception e) {
			referenceNo = 1;
			log4j.error("Error: Reference No For Valuation Table is ::"
					+ referenceNo);
		}

		return referenceNo;

	}

	private void saveToEconomicICDEFWDTable(Session session,
			Map<String, List<BigDecimal>> insertValueMap, String category) {

		java.util.Iterator<String> itr = insertValueMap.keySet().iterator();
		while (itr.hasNext()) {

			BigDecimal referenceNumber = getReferenceNumberForICDEF(session);
			String storeName = (String) itr.next();
			List<BigDecimal> insertValuesList = (List<BigDecimal>) insertValueMap
					.get(storeName);

			String queryString = "insert into sw_economic_ic_def_wd(type, total_qty,"
					+ " total_value, reference_number, category, report_date, ad_client_id, ad_org_id, created, "
					+ " createdby, updated, updatedby, isactive) values(:type, :total_qty,"
					+ " :total_value, :reference_number, :category, :report_date, :ad_client_id, :ad_org_id, :created, "
					+ " :createdby, :updated, :updatedby, :isactive)";

			SQLQuery insertQueryForICWD = session.createSQLQuery(queryString);
			try {
				insertQueryForICWD.setString("type", storeName);
				insertQueryForICWD.setBigDecimal("total_qty",
						insertValuesList.get(0));
				insertQueryForICWD.setBigDecimal("total_value",
						insertValuesList.get(1));
				insertQueryForICWD.setBigDecimal("reference_number",
						referenceNumber);
				insertQueryForICWD.setString("category", category);
				insertQueryForICWD.setDate("report_date", new Date());
				insertQueryForICWD.setString("ad_client_id", "0");
				insertQueryForICWD.setString("ad_org_id", "0");
				insertQueryForICWD.setDate("created", new Date());
				insertQueryForICWD.setString("createdby", "0");
				insertQueryForICWD.setDate("updated", new Date());
				insertQueryForICWD.setString("updatedby", "0");
				insertQueryForICWD.setString("isactive", "Y");

				int status = insertQueryForICWD.executeUpdate();

			} catch (Exception ex) {
				log4j.error("Exception happend while saving into save To EconomicICDEFWDTable table::"
						+ ex);
			}
		}

	}

	*//**
	 * This method is used to get the reference number from
	 * sw_economic_ic_def_wd table. If reference number is not there then, it
	 * will start from 1. Else it will start from max reference+1.
	 * 
	 * @param session
	 * @return BigDecimal
	 *//*

	private BigDecimal getReferenceNumberForICDEF(Session session) {

		BigDecimal referenceNo = new BigDecimal(1);
		try {

			String hql = "select max(reference_number) from sw_economic_ic_def_wd";
			Query query = session.createSQLQuery(hql);
			List<Object> results = query.list();

			for (Object maxRef : results) {

				referenceNo = new BigDecimal(maxRef.toString())
						.add(new BigDecimal(1));
				log4j.info("Reference No For ICDEF Table ::" + referenceNo);
			}

		} catch (Exception e) {

			referenceNo = new BigDecimal(1);
			log4j.error("Error: Reference No For ICDEF Table is ::"
					+ referenceNo + e);
		}

		return referenceNo;

	}

	*//**
	 * This method is used to get the start and end date of a particluar month
	 * and year, by passing the week number.
	 * 
	 * @param weekNumber
	 * @return List<String> dates
	 *//*

	private List<String> getStartAndDateListBasedOnWeekNumber(String weekNumber) {
		List<String> strEndDateList = new LinkedList<String>();
		Calendar cal = Calendar.getInstance();
		int year = cal.get(cal.YEAR);
		int month = cal.get(cal.MONTH);

		log4j.info("year = " + year + "\nmonth = " + month);
		Map<String, List<List<String>>> monthWeekDateMap = new LinkedHashMap<String, List<List<String>>>();

		for (int index = 0; index < month; index++) {
			List<List<String>> weekDateList = getNumberOfWeeks(year, index);
			monthWeekDateMap.put(String.valueOf(index), weekDateList);
		}

		List<List<String>> weekAndDateList = monthWeekDateMap.get(String
				.valueOf(month - 1));
		Map<String, List<String>> weekNumberAndDateMap = new LinkedHashMap<String, List<String>>();

		for (List<String> dateList : weekAndDateList) {
			List<String> startEndDateList = new LinkedList<String>();
			startEndDateList.add(dateList.get(1));
			startEndDateList.add(dateList.get(2));
			weekNumberAndDateMap.put(dateList.get(0), startEndDateList);
		}

		log4j.info("Date List is :" + weekNumberAndDateMap.get(weekNumber));

		return weekNumberAndDateMap.get(weekNumber);

	}

	*//**
	 * This method is used to get all the weeks start date and end dates for a
	 * particular month and year.
	 * 
	 * @param year
	 * @param month
	 * @return List<List<String>>
	 *//*

	private List<List<String>> getNumberOfWeeks(int year, int month) {
		SimpleDateFormat sdateFormat = new SimpleDateFormat("yyyy-MM-dd");
		List<List<String>> weekDateList = new ArrayList<List<String>>();
		List<String> dates;
		int weekNumber = 1;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		while (cal.get(Calendar.MONTH) == month) {
			dates = new ArrayList<String>();
			while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				cal.add(Calendar.DAY_OF_MONTH, -1);
			}
			dates.add(String.valueOf(weekNumber));

			Date monthStartDate = getMonthStartDate(month, year, cal);

			if (compareDates(sdateFormat.format(cal.getTime()),
					sdateFormat.format(monthStartDate)) < 0) {
				dates.add(sdateFormat.format(monthStartDate));
			} else {
				dates.add(sdateFormat.format(cal.getTime()));
			}

			Date monthEndDate = getMonthEndDate(month, year, cal);

			cal.add(Calendar.DAY_OF_MONTH, 7);

			if (compareDates(sdateFormat.format(cal.getTime()),
					sdateFormat.format(monthEndDate)) > 0) {
				dates.add(sdateFormat.format(monthEndDate));
			} else {
				dates.add(sdateFormat.format(cal.getTime()));
			}

			weekDateList.add(dates);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			weekNumber++;
		}

		return weekDateList;
	}

	*//**
	 * This method is used to compare the dates provided.
	 * 
	 * @param dateString1
	 * @param dateString2
	 * @return int result
	 *//*

	private int compareDates(String dateString1, String dateString2) {
		int result = -1;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		Date date1;
		Date date2;
		try {
			date1 = format.parse(dateString1);
			date2 = format.parse(dateString2);
			result = date1.compareTo(date2);
		} catch (ParseException e) {
			log4j.error("Error occurred while comparing dates:date1:"
					+ dateString1 + ",date2:" + dateString2 + e);
		}

		return result;
	}

	*//**
	 * This method is used to get the month start date of a particular month and
	 * year.
	 * 
	 * @param month
	 * @param year
	 * @param cal
	 * @return Date Object
	 *//*

	private Date getMonthStartDate(int month, int year, Calendar cal) {
		GregorianCalendar gc1 = new GregorianCalendar(year, month,
				cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		return new java.util.Date(gc1.getTime().getTime());
	}

	*//**
	 * This method is used to get the month end date of a particular month and
	 * year.
	 * 
	 * @param month
	 * @param year
	 * @param cal
	 * @return Date Object
	 *//*
	private Date getMonthEndDate(int month, int year, Calendar cal) {
		GregorianCalendar gc2 = new GregorianCalendar(year, month,
				cal.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
		return new java.util.Date(gc2.getTime().getTime());
	}

	*//**
	 * This method is used to get date format in yyyy-MM-dd pattern.
	 * 
	 * @return SimpleDateFormat
	 *//*

	private SimpleDateFormat getDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}

	*//**
	 * This method is used to fetch the previous month.
	 * 
	 * @param cal
	 * @return int month
	 *//*
	private int getPreviuosMonth(Calendar cal) {
		int month = cal.get(cal.MONTH);
		return month - 1;// Make it to -1 when it is in production
	}

	*//**
	 * This method is used to fetch the year.
	 * 
	 * @param cal
	 * @return int year
	 *//*
	private int getPreviuosYear(Calendar cal) {
		return cal.get(cal.YEAR);
	}

	*//**
	 * This method is used to get the start and end date of previous month.
	 * 
	 * @return List<String>
	 *//*
	private List<String> getStartAndEndDateArr() {
		List<String> startAndEndDateArr = new LinkedList<String>();
		SimpleDateFormat sdateFormat = getDateFormat();
		Calendar cal = Calendar.getInstance();
		int year = getPreviuosYear(cal);
		int month = getPreviuosMonth(cal);

		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		startAndEndDateArr.add(sdateFormat.format(getMonthStartDate(month,
				year, cal)));
		startAndEndDateArr.add(sdateFormat.format(getMonthEndDate(month, year,
				cal)));

		log4j.info("Start and End Date list is:" + startAndEndDateArr);
		return startAndEndDateArr;

	}

	*//**
	 * This method is used to insert the sales return values into a temporary
	 * table for further calculations.
	 * 
	 * @param session
	 * @param itemCode
	 * @param totalAmount
	 * @param qty
	 * @param vatRate
	 * @param taxAmt
	 * @param storeName
	 *//*

	private void saveToSalesReturnTempTable(Session session, String itemCode,
			BigDecimal totalAmount, BigDecimal qty, BigDecimal vatRate,
			BigDecimal taxAmt, String storeName) {

		String queryString = "insert into sw_sales_return_temp(item_code, total_amount,"
				+ " qty, vat_rate, tax_amount, org_name) values(:item_code,:total_amount,:qty, :vat_rate, :tax_amount, :org_name )";

		SQLQuery insertQueryForSalesReturn = session
				.createSQLQuery(queryString);
		try {

			insertQueryForSalesReturn.setString("item_code", itemCode);
			insertQueryForSalesReturn
					.setBigDecimal("total_amount", totalAmount);
			insertQueryForSalesReturn.setBigDecimal("qty", qty);
			insertQueryForSalesReturn.setBigDecimal("vat_rate", vatRate);
			insertQueryForSalesReturn.setBigDecimal("tax_amount", taxAmt);
			insertQueryForSalesReturn.setString("org_name", storeName);

			int status = insertQueryForSalesReturn.executeUpdate();

		} catch (Exception ex) {
			log4j.error("Exception happend while saving into sw_economic_valuation table::"
					+ ex);
		}

	}

	*//**
	 * This method is used to insert the values required for the calculations of
	 * Internal sales.
	 * 
	 * @param session
	 * @param itemCode
	 * @param totalQty
	 * @param logRecharge
	 * @param logRechargeRate
	 * @param storeName
	 *//*

	private void saveToInternalSalesTempTable(Session session, String itemCode,
			BigDecimal totalQty, String logRecharge,
			BigDecimal logRechargeRate, String storeName) {

		String queryString = "insert into sw_internal_sales_temp(item_code,"
				+ " qty, log_recharge, log_recharge_rate, org_name) values(:item_code,:qty, "
				+ ":log_recharge, :log_recharge_rate, :org_name )";

		SQLQuery insertQueryForInternalSales = session
				.createSQLQuery(queryString);
		try {

			insertQueryForInternalSales.setString("item_code", itemCode);
			insertQueryForInternalSales.setBigDecimal("qty", totalQty);
			insertQueryForInternalSales.setBigDecimal("log_recharge",
					new BigDecimal(logRecharge));

			insertQueryForInternalSales.setBigDecimal("log_recharge_rate",
					logRechargeRate);
			insertQueryForInternalSales.setString("org_name", storeName);

			int status = insertQueryForInternalSales.executeUpdate();

		} catch (Exception ex) {

			log4j.info("Exception happend while saving into InternalSalesTemp table::"
					+ ex);
		}

	}

	*//**
	 * This method is used to insert the values for logistic recharge into a
	 * temporary table for further calculations.
	 * 
	 * @param session
	 * @param itemCode
	 * @param totalQty
	 * @param logRecharge
	 * @param logRechargeRate
	 * @param storeName
	 *//*

	private void saveToLogRechargeTempTable(Session session, String itemCode,
			BigDecimal totalQty, String logRecharge,
			BigDecimal logRechargeRate, String storeName) {

		String queryString = "insert into sw_log_recharge_cost_temp(item_code,"
				+ " qty, log_recharge, log_recharge_rate, org_name) values(:item_code,:qty, "
				+ ":log_recharge, :log_recharge_rate, :org_name )";

		SQLQuery insertQueryForLogRecharge = session
				.createSQLQuery(queryString);
		try {

			insertQueryForLogRecharge.setString("item_code", itemCode);
			insertQueryForLogRecharge.setBigDecimal("qty", totalQty);
			insertQueryForLogRecharge.setBigDecimal("log_recharge",
					new BigDecimal(logRecharge));

			insertQueryForLogRecharge.setBigDecimal("log_recharge_rate",
					logRechargeRate);
			insertQueryForLogRecharge.setString("org_name", storeName);

			int status = insertQueryForLogRecharge.executeUpdate();

		} catch (Exception ex) {
			log4j.info("Exception happend while saving into Logistic Recharge temp table::"
					+ ex);
		}

	}

	*//**
	 * This method is used to get only Karnataka store list.
	 * 
	 * @return
	 *//*

	private List<String> getKarnatakaStoreList() {

		List<String> karnatakaStoreList = new ArrayList<String>();
		karnatakaStoreList.add("BGT");
		karnatakaStoreList.add("Sarjapur Store");
		karnatakaStoreList.add("Whitefield Warehouse");
		karnatakaStoreList.add("IN1003");

		return karnatakaStoreList;

	}

	*//**
	 * This methos is used to get the current year.
	 * 
	 * @return int year : eg: 2013
	 *//*
	private int getCurrentYear() {
		Calendar cal = Calendar.getInstance();
		return cal.get(cal.YEAR);

	}

	*//**
	 * This method is used to delete all the records from sw_economic_valuation
	 * table.
	 * 
	 * @param session
	 *//*

	private void clearEconomicReportTable(Session session) {

		String deleteQuery = "delete from sw_economic_valuation";
		SQLQuery query = session.createSQLQuery(deleteQuery);

		int deleteStatus = query.executeUpdate();
		log4j.info("Deleted all the records from sw_economic_valuation table");

	}

	*//**
	 * This method is used to delete all the records from sw_economic_ic_def_wd
	 * table.
	 * 
	 * @param session
	 *//*

	private void clearICWDTable(Session session) {

		String deleteQuery = "delete from sw_economic_ic_def_wd";
		SQLQuery query = session.createSQLQuery(deleteQuery);
		int deleteStatus = query.executeUpdate();
		log4j.info("Deleted all records from sw_economic_ic_def_wd table");

	}

	*//**
	 * This method is used to delete all the records from the temporary tables.
	 * Getting deleted tables are : 1. sw_sales_return_temp 2.
	 * sw_internal_sales_temp 3. sw_log_recharge_cost_temp 4.
	 * sw_shrinkage_ic_wd_temp 5. sw_economic_wacp
	 * 
	 * @param session
	 *//*

	private void deleteAllTempTable(Session session) {

		SQLQuery query1 = session
				.createSQLQuery("delete from sw_sales_return_temp");
		int deleteStatus1 = query1.executeUpdate();

		log4j.info("DELETED RECORDS FROM sw_sales_return_temp TABLES");

		SQLQuery query2 = session
				.createSQLQuery("delete from sw_internal_sales_temp");
		int deleteStatus2 = query2.executeUpdate();

		log4j.info("DELETED RECORDS FROM sw_internal_sales_temp TABLES");

		SQLQuery query3 = session
				.createSQLQuery("delete from sw_log_recharge_cost_temp");
		int deleteStatus3 = query3.executeUpdate();

		log4j.info("DELETED RECORDS FROM sw_log_recharge_cost_temp TABLES");

		SQLQuery query4 = session
				.createSQLQuery("delete from sw_shrinkage_ic_wd_temp");
		int deleteStatus4 = query4.executeUpdate();

		log4j.info("DELETED RECORDS FROM sw_shrinkage_ic_wd_temp TABLES");

		
		SQLQuery query5 = session .createSQLQuery("delete from sw_economic_wacp");
		int deleteStatus5 = query5.executeUpdate();
		
		log4j.info("DELETED RECORDS FROM sw_economic_wacp TABLES");
		log4j.info("DELETED RECORDS FROM ALL TEMP TABLES");

	}

}
*/