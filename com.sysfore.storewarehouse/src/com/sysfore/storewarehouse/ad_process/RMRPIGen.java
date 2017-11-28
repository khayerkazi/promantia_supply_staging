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
import org.openbravo.service.db.CallStoredProcedure;

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
//import com.sysfore.storewarehouse.SWEconomicValuation;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.SessionHandler;

/*
 * This class is used for calculating the Economical stock valuation
 */

public class RMRPIGen extends DalBaseProcess {

	// private final static String BATCH_SIZE = "50";
	private final static String SYSTEM_CLIENT_ID = "0";


	private ProcessLogger logger;
	private ConnectionProvider connection;
	// private int totalProcessed = 0;

	static Logger log4j = Logger.getLogger(RMRPIGen.class);

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
				// Calling the method to Update RFC Status
				UpdateRFCStatus(vars1, bundle);
			}
		} else {
			// Calling the method to calculate Economic stock
			// of the previous month.
			UpdateRFCStatus(vars, bundle);
		}
	}

	/**
	 * This calulates the Economic stock for the previous month.
	 * 
	 * @param vars
	 * @param bundle
	 * @return
	 * @throws ServletException
	 */
	private OBError UpdateRFCStatus(VariablesSecureApp vars,
			ProcessBundle bundle) throws ServletException {

		
		OBError myError = new OBError();
		// String clientId = bundle.getContext().getClient();
		// String orgId = bundle.getContext().getOrganization();
		// String userId = bundle.getContext().getUser();
		ConnectionProvider conn = null;
		Connection con = null;

		try {
		
			conn = bundle.getConnection();
			con = conn.getTransactionConnection();

			log4j.info("ConnectionProvider =" + conn + "|| Connection=" + con);

			// creating the session from OBDal
			final Session session = OBDal.getInstance().getSession();

			List<Object> cOrderIdLst = fetchCOrderIds (session);
		
			
			for (Object cOrderId : cOrderIdLst) {
			//	corrStoreValuesList.add(getTotalPurchaseAmount(session,String.valueOf(storeName)));
					log4j.info(String.valueOf(cOrderId));
					updateCOrderStatus(String.valueOf(cOrderId));
				
			}
			

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

	

	private List<Object> fetchCOrderIds (Session session) {

		//-interval '24hrs' 
		
		final SQLQuery queryToUpdateCOrderStatus = session
				.createSQLQuery("select c_order_id  from c_order where created < now() " +
						"and docstatus='DR' and c_doctypetarget_id =(select c_doctype_id from c_doctype " +
						"where name = 'RFC Order')"); 
	return	 queryToUpdateCOrderStatus.list();

	}

	private void updateCOrderStatus(String c_order_id ){

	 final List<Object> param = new ArrayList<Object>();
    param.add(null);
    param.add(c_order_id);
    CallStoredProcedure.getInstance().call("c_order_post1", param, null, true, false);
	}

}
