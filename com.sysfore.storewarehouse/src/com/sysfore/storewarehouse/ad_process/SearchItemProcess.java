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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Timer;
import java.util.Calendar;
import javassist.bytecode.Descriptor.Iterator;

import javax.servlet.ServletException;
import org.hibernate.Query;
import org.hibernate.Session;
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
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.SessionHandler;
import java.math.RoundingMode;

/*
 * This class is used for selecting the required items which are to be 
 * displayed in the Search Item Application in View Sales Data Tab.
 */

public class SearchItemProcess extends DalBaseProcess {

	// private final static String BATCH_SIZE = "50";
	private final static String SYSTEM_CLIENT_ID = "0";

	private ProcessLogger logger;
	private ConnectionProvider connection;
	// private int totalProcessed = 0;

	static Logger log4j = Logger.getLogger(SearchItemProcess.class);

	public void doExecute(ProcessBundle bundle) throws Exception {

		logger = bundle.getLogger();
		//connection = bundle.getConnection();

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
				processSearchItemForSalesData(vars1, bundle);
			}
		} else {
			processSearchItemForSalesData(vars, bundle);
		}
	}

	private OBError processSearchItemForSalesData(VariablesSecureApp vars,
			ProcessBundle bundle) throws ServletException {

		OBError myError = new OBError();
		ConnectionProvider conn = null;
		Connection con = null;

		try {

			conn = bundle.getConnection();
			con = conn.getTransactionConnection();

			log4j.info("ConnectionProvider =" + conn + "|| Connection=" + con);

			// creating the session from OBDal
			final Session session = OBDal.getInstance().getSession();

			final Query queryForStore = session
					.createSQLQuery("select name from ad_org where em_sw_isstore = 'Y'");

			List<String> storeList = queryForStore.list();

			for (Object storeObj : storeList) {
				log4j.info("Store Name::" + storeObj.toString());
				selectAndInsertToSearchItemTable(session, storeObj.toString());
			}

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

	private void selectAndInsertToSearchItemTable(Session session,
			String storeName) {

		try {

			// Selecting all the product ids from m_transaction table
			final Query queryForItemCode = session
					.createSQLQuery("select name from m_product ");

			List<String> itemCodeList = queryForItemCode.list();

			// Iterating each product of M_PRODUCT table
			for (Object mTransObj : itemCodeList) {

				// Taking the Item code
				String itemCode = mTransObj.toString();
				log4j.info("ITEM CODE::" + itemCode);
				selectAllItems(session, itemCode, storeName);
				log4j.info("#INSERTED#");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.info("Exception occurred while select and insert into Search Item Table::"
					+ ex.getMessage());
		}

	}

	private void selectAllItems(Session session, String itemCode,
			String storeName) {

		Map<String, String> insertMapForSearchItem = new LinkedHashMap<String, String>();

		String query = " select mp.name as ItemCode, coalesce(sum(col.priceactual),0) as PurchaseAmt, "
				+ " coalesce(sum(col.qtyordered),0) as TotalQty, coalesce(sum(col.em_ds_marginamt),0) as MarginAmt, "
				+ " coalesce(mp.em_cl_ue_qty,0) as UeQty, coalesce(mp.em_cl_pcb_qty,0) as PcbQty, coalesce(mpp.em_cl_ccueprice,0) as UePrice,"
				+ " coalesce(mpp.em_cl_ccpcbprice,0) as PcbPrice,  Date('2013-08-03') as reportdate from m_product mp"
				+ " left join c_orderline col on mp.m_product_id = col.m_product_id and "
				+ " col.dateordered >= '2013-07-29' and col.dateordered < '2013-08-05' "
				+ " and col.ad_org_id = (select ad_org_id from ad_org where name =:storeName) "
				+ " and col.c_order_id = ( select co.c_order_id from c_order co where co.c_order_id = col.c_order_id and co.IsSOTrx='Y'"
				+ " and co.c_doctype_id = (select c_doctype_id from c_doctype where name = 'POS Order (Decathlon)')) "
				+ " left join m_productprice mpp on mp.m_product_id = mpp.m_product_id and "
				+ " mpp.ad_org_id = col.ad_org_id where mp.name = :itemCode group by  mp.name, "
				+ " mp.em_cl_ue_qty, mp.em_cl_pcb_qty, mpp.em_cl_ccpcbprice, mpp.em_cl_ccueprice ";

		final Query queryToSelectForSITable = session.createSQLQuery(query);

		queryToSelectForSITable.setParameter("storeName", storeName);
		queryToSelectForSITable.setParameter("itemCode", itemCode);

		if (queryToSelectForSITable != null) {
			List<Object> searchItemList = queryToSelectForSITable.list();

			if (searchItemList != null && searchItemList.size() > 0) {

				for (Object rows : searchItemList) {
					Object[] row = (Object[]) rows;

					String itemcode = (String) row[0];
					insertMapForSearchItem.put("itemCode", itemcode);

					BigDecimal purchaseAmount = (BigDecimal) row[1];

					insertMapForSearchItem.put("purchaseAmount",
							purchaseAmount.toString());

					BigDecimal totalQuantity = (BigDecimal) row[2];
					insertMapForSearchItem.put("totalQuantity",
							totalQuantity.toString());

					BigDecimal marginAmount = (BigDecimal) row[3];
					insertMapForSearchItem.put("marginAmount",
							marginAmount.toString());

					BigDecimal ueQuantity = (BigDecimal) row[4];
					insertMapForSearchItem.put("ueQuantity",
							ueQuantity.toString());

					BigDecimal pcbQuantity = (BigDecimal) row[5];
					insertMapForSearchItem.put("pcbQuantity",
							pcbQuantity.toString());

					BigDecimal uePrice = (BigDecimal) row[6];
					insertMapForSearchItem.put("uePrice", uePrice.toString());

					BigDecimal pcbPrice = (BigDecimal) row[7];
					insertMapForSearchItem.put("pcbPrice", pcbPrice.toString());

					Date reportDate = (Date) row[8];

					// Inserting the values into the Search Item table
					insertIntoSearchItemTable(session, insertMapForSearchItem,
							reportDate, storeName);

				}
			}
		}

	}

	private void insertIntoSearchItemTable(Session session,
			Map<String, String> insertMapForSearchItem, Date reportDate,
			String storeName) {

		String queryString = "insert into sw_search_item(sw_search_item_id, itemcode, purchaseamount, "
				+ " totalquantity, marginamount, uequantity, pcbquantity, ueprice, pcbprice, totalamount, storename,"
				+ " reportdate, ad_client_id, ad_org_id, updated, updatedby, created, createdby, year) "
				+ " values(get_uuid(), :itemcode, :purchaseamount, :totalquantity, :marginamount, "
				+ " :uequantity, :pcbquantity, :ueprice, :pcbprice, :totalamount, :storename, :reportdate, "
				+ " :ad_client_id, :ad_org_id, :updated, :updatedby, :created, :createdby, :year)";

		SQLQuery insertQueryForSearchItem = session.createSQLQuery(queryString);
		try {

			insertQueryForSearchItem.setString("itemcode",
					insertMapForSearchItem.get("itemCode"));

			insertQueryForSearchItem
					.setBigDecimal("purchaseamount", new BigDecimal(
							insertMapForSearchItem.get("purchaseAmount")));

			insertQueryForSearchItem
					.setBigDecimal("totalquantity", new BigDecimal(
							insertMapForSearchItem.get("totalQuantity")));

			insertQueryForSearchItem.setBigDecimal("marginamount",
					new BigDecimal(insertMapForSearchItem.get("marginAmount")));

			insertQueryForSearchItem.setBigDecimal("uequantity",
					new BigDecimal(insertMapForSearchItem.get("ueQuantity")));

			insertQueryForSearchItem.setBigDecimal("pcbquantity",
					new BigDecimal(insertMapForSearchItem.get("pcbQuantity")));

			insertQueryForSearchItem.setBigDecimal("ueprice", new BigDecimal(
					insertMapForSearchItem.get("uePrice")));

			insertQueryForSearchItem.setBigDecimal("pcbprice", new BigDecimal(
					insertMapForSearchItem.get("pcbPrice")));

			BigDecimal totalAmount = (new BigDecimal(
					insertMapForSearchItem.get("purchaseAmount"))
					.multiply(new BigDecimal(insertMapForSearchItem
							.get("totalQuantity"))))
					.add((new BigDecimal(insertMapForSearchItem.get("uePrice"))
							.multiply(new BigDecimal(insertMapForSearchItem
									.get("ueQuantity")))))
					.add((new BigDecimal(insertMapForSearchItem.get("pcbPrice"))
							.multiply(new BigDecimal(insertMapForSearchItem
									.get("pcbQuantity")))))
					.subtract(
							new BigDecimal(insertMapForSearchItem
									.get("marginAmount")));

			insertQueryForSearchItem.setBigDecimal("totalamount", totalAmount);

			insertQueryForSearchItem.setString("storename", storeName);

			insertQueryForSearchItem.setDate("reportdate", reportDate);

			insertQueryForSearchItem.setString("ad_client_id", "0");

			insertQueryForSearchItem.setString("ad_org_id", "0");

			insertQueryForSearchItem.setDate("updated", new Date());

			insertQueryForSearchItem.setString("updatedby", "0");

			insertQueryForSearchItem.setDate("created", new Date());

			insertQueryForSearchItem.setString("createdby", "0");

			insertQueryForSearchItem.setString("year", getCurrentYear());

			int status = insertQueryForSearchItem.executeUpdate();

		} catch (Exception ex) {
			ex.printStackTrace();
			log4j.error("Exception happend while saving into Search Item Table::"
					+ ex);
		}

	}

	private String getCurrentYear() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		return String.valueOf(year);
	}

}
