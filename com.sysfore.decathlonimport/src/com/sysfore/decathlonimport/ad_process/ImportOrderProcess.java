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

package com.sysfore.decathlonimport.ad_process;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;

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

public class ImportOrderProcess extends DalBaseProcess {

  private final static String BATCH_SIZE = "50";
  private final static String SYSTEM_CLIENT_ID = "0";

  private boolean isDirect;

  private StringBuffer lastLog = new StringBuffer();
  private StringBuffer message = new StringBuffer();

  private String[] TableIds = null;

  private ProcessLogger logger;
  private ConnectionProvider connection;
  private int totalProcessed = 0;

  static Logger log4j = Logger.getLogger(ImportOrderProcess.class);

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    connection = bundle.getConnection();

    VariablesSecureApp vars = bundle.getContext().toVars();
    if (vars.getClient().equals(SYSTEM_CLIENT_ID)) {
      OBCriteria<Client> obc = OBDal.getInstance().createCriteria(Client.class);
      obc.add(Expression.not(Expression.eq(Client.PROPERTY_ID, SYSTEM_CLIENT_ID)));

      for (Client c : obc.list()) {
        final VariablesSecureApp vars1 = new VariablesSecureApp(bundle.getContext().getUser(),
            c.getId(), bundle.getContext().getOrganization());
        processOrder(vars1, bundle);
      }
    } else {
      processOrder(vars, bundle);
    }
  }

  /**
   * 
   * @param vars
   * @param bundle
   * @return
   * @throws ServletException
   */
  @SuppressWarnings("static-access")
  private OBError processOrder(VariablesSecureApp vars, ProcessBundle bundle)
      throws ServletException {
    final String processId = bundle.getProcessId();
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
    // System.out.println("clientId:" + clientId);
    // System.out.println("o/:" + orgId);
    log4j.debug("Delete Old Imported = ");
    try {
      int no = 0;
			String batchno = "";
      conn = bundle.getConnection();
      con = conn.getTransactionConnection();
      if (m_deleteOldImported) {
        no = ImportOrderProcessData.deleteOld(con, conn, clientId);
        if (log4j.isDebugEnabled())
          log4j.debug("Delete Old Imported = " + no);
      }
			//Get a unique batch number
			batchno = SequenceIdData.getUUID();
			//System.out.println("BatchNum:" + batchno);
			//Update records with batch number
			no = ImportOrderProcessData.updateBatchNumber(con, conn, batchno, clientId);

      // Set Client, Org, IsActive, Created/Updated
      // changes to add client id and org id
      // System.out.println("clientId:"+clientId+":orgId:"+orgId);
      log4j.debug("ImportOrder Client Id = " + clientId);
      log4j.debug("ImportOrder Org Id = " + orgId);
      no = ImportOrderProcessData.updateClientRecords(con, conn, clientId, orgId);
      if (log4j.isDebugEnabled())
        log4j.debug("ImportOrder updated = " + no);

      no = ImportOrderProcessData.updateRecords(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("ImportOrder updated = " + no);
      no = ImportOrderProcessData.updateInvalidVendor(con, conn, orgId, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid vendor errors = " + no);
      // Invalid organization
      no = ImportOrderProcessData.updateRecordsError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid organization errors = " + no);
      // Document Type - PO - SO
      no = ImportOrderProcessData.updateDocTypePO(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated DocTypePO = " + no);
      no = ImportOrderProcessData.updateDocTypeSO(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated DocTypeSO = " + no);
      no = ImportOrderProcessData.updateDocType(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated DocType = " + no);
      no = ImportOrderProcessData.updateDocTypeError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid DocType errors = " + no);
      // DocType Default
      no = ImportOrderProcessData.updateDocTypePODefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated DocTypePO default = " + no);
      no = ImportOrderProcessData.updateDocTypeSODefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated DocTypeSO default = " + no);
      no = ImportOrderProcessData.updateDocTypeDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated DocType default = " + no);
      no = ImportOrderProcessData.updateDocTypeDefaultError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid DocType default errors = " + no);
      // Set IsSOTrx
      no = ImportOrderProcessData.updateIsSOTrxY(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated IsSOTrx=Y = " + no);
      no = ImportOrderProcessData.updateIsSOTrxN(con, conn, clientId);
      // update pricelist
      if (log4j.isDebugEnabled())
        log4j.debug("Update pricelist = " + no);
      no = ImportOrderProcessData.updatePriceList(con, conn, orgId, clientId);

      if (log4j.isDebugEnabled())
        log4j.debug("Updated IsSOTrx=N = " + no);
      // Price List
      no = ImportOrderProcessData.updatePriceListCurrencyDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PriceList by currency (default) = " + no);
      no = ImportOrderProcessData.updatePriceListNullCurrencyDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PriceList with null currency (default) = " + no);
      no = ImportOrderProcessData.updatePriceListCurrency(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PriceList by currency = " + no);
      no = ImportOrderProcessData.updatePriceListNullCurrency(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PriceList with null currency = " + no);
      no = ImportOrderProcessData.updatePriceListError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid PriceList errors = " + no);
      // Set Currency
      no = ImportOrderProcessData.updateCurrencyDefaultFromPriceList(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("ImportOrder Set Currency Default =" + no);
      no = ImportOrderProcessData.updateInvalidCurrency(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("ImportOrder Invalid Currency =" + no);
      // Payment Term
      no = ImportOrderProcessData.updatePaymentTerm(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PaymentTerm = " + no);
      no = ImportOrderProcessData.updatePaymentTermDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PaymentTerm default = " + no);
      no = ImportOrderProcessData.updatePaymentTermError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid PaymentTerm errors = " + no);
      // Warehouse
      no = ImportOrderProcessData.updateWarehouse(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Warehouse = " + no);
      no = ImportOrderProcessData.updateWarehouseOther(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Warehouse (other) = " + no);
      no = ImportOrderProcessData.updateWarehouseError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid Warehouse errors = " + no);
      // BusinessPartner
      // import depending on the external value
      no = ImportOrderProcessData.updateBPartnerFromValue(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BPartner from value = " + no);
      no = ImportOrderProcessData.updateBPartnerFromEmail(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BPartner from email = " + no);
      no = ImportOrderProcessData.updateBPartnerFromContact(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BPartner from contact = " + no);
      no = ImportOrderProcessData.updateBPartnerFromName(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BPartner from name = " + no);
      no = ImportOrderProcessData.updateBPartnerFromUPC(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BPartner from upc = " + no);
      no = ImportOrderProcessData.updateBPartnerDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BPartner (default) = " + no);

      // Member Type updates here
      no = ImportOrderProcessData.updateMemberType(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Member Type (default) = " + no);

      // Member Type proper updates here
      no = ImportOrderProcessData.updateProperMemberType(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Member Type (Proper) = " + no);
      // update PriceList and PaymentTerm according to BPartner
      no = ImportOrderProcessData.updatePriceListFromBPartner(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PriceList from BPartner = " + no);
      no = ImportOrderProcessData.updatePOPriceListFromBPartner(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated POPriceList from BPartner = " + no);
      no = ImportOrderProcessData.updatePaymentTermFromBPartner(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated PaymentTerm from BPartner = " + no);
      no = ImportOrderProcessData.updatePOPaymentTermFromBPartner(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated POPaymentTerm from BPartner = " + no);
      // Location
      no = ImportOrderProcessData.updateLocationByUPC(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Location by UPC = " + no);
      no = ImportOrderProcessData.updateLocationDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Location default = " + no);
      no = ImportOrderProcessData.updateBilltoByUPC(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated bill location by UPC = " + no);
      no = ImportOrderProcessData.updateLocationExisting(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Location (existing) = " + no);
      no = ImportOrderProcessData.updateBillLocation(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated BillLocation = " + no);
      no = ImportOrderProcessData.updateLocation(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Location = " + no);
      no = ImportOrderProcessData.updateLocationError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid Location errors = " + no);
      // Country
      no = ImportOrderProcessData.updateCountryDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Country (default) = " + no);
      no = ImportOrderProcessData.updateCountry(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Country = " + no);
      no = ImportOrderProcessData.updateCountryError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid Country errors = " + no);
      // Set Region
      no = ImportOrderProcessData.updateRegionDefault(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Region (default) = " + no);
      no = ImportOrderProcessData.updateRegion(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Region = " + no);
      no = ImportOrderProcessData.updateRegionError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid Region errors = " + no);
      // Product
      no = ImportOrderProcessData.updateProductFromValue(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Product from value = " + no);
      no = ImportOrderProcessData.updateProductFromUpc(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Product from UPC = " + no);
      no = ImportOrderProcessData.updateProductFromSku(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Product from SKU = " + no);
      no = ImportOrderProcessData.updateProductError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid Product errors = " + no);
      // Tax
      no = ImportOrderProcessData.updateTax(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Updated Tax = " + no);
      no = ImportOrderProcessData.updateTaxError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid Tax errors = " + no);
				
      conn.releaseCommitConnection(con);
			
System.out.println("Version with batch num Import Order Process");
      // till here, the edition of the I_Order table
      // now, the insertion from I_Order table in C_Order 
			//get only records relavant to batch number
      // New BPartner
      ImportOrderProcessData[] data = ImportOrderProcessData.selectWithoutBP(conn, clientId, batchno);

      if (log4j.isDebugEnabled())
        log4j.debug("Going through " + data.length + " records");
      for (int i = 0; i < data.length; i++) {
        con = conn.getTransactionConnection();
        String I_Order_ID = data[i].iOrderId;
	System.out.println("Import Order Process Inside selectwithoutBP");
        if (data[i].bpartnervalue == null || data[i].bpartnervalue.equals("")) {
          if (data[i].email != null && !data[i].email.equals(""))
            data[i].bpartnervalue = data[i].email;
          else if (data[i].name != null && !data[i].name.equals(""))
            data[i].bpartnervalue = data[i].name;
          else
            continue;
        }
        if (data[i].name == null || data[i].name.equals("")) {
          if (data[i].contactname != null && !data[i].contactname.equals(""))
            data[i].name = data[i].contactname;
          else
            data[i].name = data[i].bpartnervalue;
        }
        BPartnerData bp = new BPartnerData();
        bp.cBpartnerId = SequenceIdData.getUUID();
        bp.adClientId = data[i].adClientId;
        bp.adOrgId = data[i].adOrgId;
        bp.value = data[i].bpartnervalue;
        bp.name = data[i].name;
        try {
          bp.insert(con, conn);
        } catch (ServletException ex) {
          if (log4j.isDebugEnabled())
            log4j.debug("Insert Order - " + ex.toString());
          conn.releaseRollbackConnection(con);
          ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
          continue;
        }
        data[i].cBpartnerId = bp.cBpartnerId;
        LocationData loc = new LocationData();
        loc.cLocationId = SequenceIdData.getUUID();
        loc.adClientId = data[i].adClientId;
        loc.adOrgId = data[i].adOrgId;
        loc.address1 = data[i].address1;
        loc.address2 = data[i].address2;
        loc.city = data[i].city;
        loc.postal = data[i].postal;
        if (data[i].cRegionId != null)
          loc.cRegionId = data[i].cRegionId;
        loc.cCountryId = data[i].cCountryId;
        try {
          loc.insert(con, conn);
        } catch (ServletException ex) {
          if (log4j.isDebugEnabled())
            log4j.debug("Insert Order - " + ex.toString());
          conn.releaseRollbackConnection(con);
          ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
          continue;
        }
        data[i].cLocationId = loc.cLocationId;
        BPartnerLocationData bpl = new BPartnerLocationData();
        bpl.cBpartnerLocationId = SequenceIdData.getUUID();
        bpl.adClientId = data[i].adClientId;
        bpl.adOrgId = data[i].adOrgId;
        bpl.cBpartnerId = data[i].cBpartnerId;
        bpl.cLocationId = data[i].cLocationId;
        try {
          bpl.insert(con, conn);
        } catch (ServletException ex) {
          if (log4j.isDebugEnabled())
            log4j.debug("Insert Order - " + ex.toString());
          conn.releaseRollbackConnection(con);
          ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
          continue;
        }
        data[i].cBpartnerLocationId = bpl.cBpartnerLocationId;
        data[i].billtoId = bpl.cBpartnerLocationId;
        if (data[i].contactname != null || data[i].email != null || data[i].phone != null) {
          UserData user = new UserData();
          user.adUserId = SequenceIdData.getUUID();
          user.adClientId = data[i].adClientId;
          user.adOrgId = data[i].adOrgId;
          user.cBpartnerId = data[i].cBpartnerId;
          if (data[i].contactname != null && !data[i].contactname.equals(""))
            user.name = data[i].contactname;
          else
            user.name = data[i].name;
          user.email = data[i].email;
          user.phone = data[i].phone;
          try {
            user.insert(con, conn);
          } catch (ServletException ex) {
            if (log4j.isDebugEnabled())
              log4j.debug("Insert Order - " + ex.toString());
            conn.releaseRollbackConnection(con);
            ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
            continue;
          }
          data[i].adUserId = user.adUserId;
          data[i].updatedby = user.adUserId;
        }
	System.out.println("First Update");
        data[i].update(con, conn);
        conn.releaseCommitConnection(con);
      }
      con = conn.getTransactionConnection();
      no = ImportOrderProcessData.updateBPartnerError(con, conn, clientId);
      if (log4j.isDebugEnabled())
        log4j.debug("Invalid BPartner errors = " + no);
      conn.releaseCommitConnection(con);
System.out.println("Import Order Process After selectwithoutBP");
      // New Order belonging to same batch
      int noInsert = 0;
      int noInsertLine = 0;
      int noOrderError = 0;

      data = ImportOrderProcessData.selectNotImported(conn, clientId, batchno);
      if (log4j.isDebugEnabled())
        log4j.debug("Going through " + data.length + " records");
      COrderData corder = null;
      String order_documentno = "";
      String corder_corderid = "";
      String corder_mpricelistid = "";
      String corder_ccurrencyid = "";
      String corder_cbpartnerid = "";
      String corder_cbpartnerlocationid = "";
      String strPostMessage = "";
      int qtyUnit = 0;
      int qtyUe = 0;
      int qtyPcb = 0;
      int lineNo = 0;
      for (int i = 0; i < data.length; i++) {
        con = conn.getTransactionConnection();

        ImportOrder2Data.setTriggersImporting(con, conn);
	System.out.println("Import Order Process After setTriggersImporting");
        String I_Order_ID = data[i].iOrderId;

        if (!order_documentno.equals(data[i].documentno) || data[i].documentno.equals("")) {
          // if (!C_BPartner_ID.equals(data[i].cBpartnerId) ||
          // !BillTo_ID.equals(data[i].billtoId) ||
          // !C_BPartner_Location_ID.equals(data[i].cBpartnerLocationId))
          // {
	System.out.println("Import Order Process Inside Documentno Check"+data[i].documentno);
          corder = new COrderData();
          if (data[i].documentno != null && !data[i].documentno.equals("")) {
            corder.documentno = data[i].documentno;
          } else {
            String docTargetType = ImportOrderProcessData.cDoctypeTarget(con, conn,
                Utility.getContext(conn, vars, "#User_Client", "ImportOrder"),
                Utility.getContext(conn, vars, "#User_Org", "ImportOrder"));
            corder.documentno = Utility.getDocumentNo(conn, vars, "", "C_Order", docTargetType,
                docTargetType, false, true);
          }
          order_documentno = corder.documentno;
          if (data[i].dateordered == null || data[i].dateordered.equals("")) {
            data[i].dateordered = DateTimeData.today(conn);
          }
          if (data[i].datepromised == null || data[i].datepromised.equals("")) {
            data[i].datepromised = data[i].dateordered;
          }
	System.out.println("Import Order Process After DocumentNO");
          // Looking for same order yet inserted
          ImportOrderProcessData[] orderInserted = ImportOrderProcessData.selectOrderInserted(conn,
              clientId, data[i].adOrgId, data[i].cDoctypeId, data[i].documentno,
              data[i].dateordered, data[i].cBpartnerId);
          if (orderInserted != null && orderInserted.length == 0) {
            corder.cOrderId = SequenceIdData.getUUID();
            if (log4j.isDebugEnabled())
              log4j.debug("Creating new order with id = " + corder.cOrderId);
	System.out.println("Import Order Process Creating new order with id = " + corder.cOrderId);
            corder.adClientId = data[i].adClientId;
            corder.adOrgId = data[i].adOrgId;
            corder.cDoctypetargetId = data[i].cDoctypeId;
            corder.cDoctypeId = "0";
            if (log4j.isDebugEnabled())
              log4j.debug("data[i].cBpartnerLocationId: " + data[i].cBpartnerLocationId);
            corder.cBpartnerLocationId = data[i].cBpartnerLocationId;
            if (log4j.isDebugEnabled())
              log4j.debug("data[i].description: " + data[i].description);
            corder.description = data[i].description;
            if (data[i].description != null && !data[i].description.equals("")) {
              String location_name = ImportOrderProcessData.selectLocationName(con, conn,
                  data[i].description);
              if (log4j.isDebugEnabled())
                log4j.debug("location_name: " + location_name);
              if (location_name != null && !location_name.equals("")) {
                corder.description += " " + location_name;
                if (log4j.isDebugEnabled())
                  log4j.debug("corder.description: " + corder.description);
              }
            }
            corder.issotrx = data[i].issotrx;
            corder.docstatus = "DR";
            corder.docaction = "CO";
            corder.processing = "N";
            corder.cBpartnerId = data[i].cBpartnerId;
            corder.billtoId = data[i].billtoId;
            corder.cBpartnerLocationId = data[i].cBpartnerLocationId;
            corder.emDsReceiptno = data[i].emImReceiptno;
            corder.emDsRatesatisfaction = data[i].emImCustomersatisfaction;
            corder.emDsPosno = data[i].emImPosno;
            corder.emDsTotalpriceadj = data[i].emImTotalpriceadj;
            corder.emDsChargeamt = data[i].emImChargeamt;
	     System.out.println("cBpartnerId"+corder.cBpartnerId);
             System.out.println("data[i].emImPosno:"+data[i].emImPosno);

            // corder.emDsTotalpriceadj = data[i].emImTotalpriceadj;
            if (!data[i].adUserId.equals(""))
              corder.adUserId = data[i].adUserId;
            else
              corder.adUserId = vars.getUser();
            corder.cPaymenttermId = data[i].cPaymenttermId;
            corder.mPricelistId = data[i].mPricelistId;
            corder.mWarehouseId = data[i].mWarehouseId;
            if (!data[i].mShipperId.equals(""))
              corder.mShipperId = data[i].mShipperId;
            if (!data[i].salesrepId.equals(""))
              corder.salesrepId = data[i].salesrepId;
            if (!data[i].adOrgtrxId.equals(""))
              corder.adOrgtrxId = data[i].adOrgtrxId;
            if (!data[i].cActivityId.equals(""))
              corder.cActivityId = data[i].cActivityId;
            if (!data[i].cCampaignId.equals(""))
              corder.cCampaignId = data[i].cCampaignId;
            if (!data[i].cProjectId.equals(""))
              corder.cProjectId = data[i].cProjectId;
            if (data[i].dateordered != null && !data[i].dateordered.equals("")) {
              System.out.println("data[i].dateordered:" + data[i].dateordered);
              corder.dateordered = data[i].dateordered;
            }
            
	    if (data[i].datepromised != null && !data[i].datepromised.equals(""))
              corder.datepromised = data[i].datepromised;
            if (log4j.isDebugEnabled())
              log4j.debug("getting bp values as default");
            
	    BpartnerMiscData[] data1 = BpartnerMiscData.select(conn, data[i].cBpartnerId);
	    //BpartnerMiscData[] data1 = null;
	System.out.println("After BpartnerMiscData");
            String tmpCurrency = COrderData.selectCurrency(conn, vars.getUser(),
                data[i].cBpartnerId);
            corder.isdiscountprinted = "N";
            if (log4j.isDebugEnabled())
              log4j.debug("stablishing default values");
            // corder.cCurrencyId = (tmpCurrency == null ||
            // tmpCurrency.equals(""))?"102":tmpCurrency; // euro as
            // default

            if (tmpCurrency != null && !tmpCurrency.equals("")) {
              corder.cCurrencyId = tmpCurrency;
            } else {
              corder.cCurrencyId = data[i].cCurrencyId;
            }
	System.out.println("After Currency");
corder.paymentrule = "B";
corder.invoicerule = "D";
corder.deliveryrule = "A";
corder.deliveryviarule= "P";
corder.freightcostrule = "I";
corder.cPaymenttermId = "A4B18FE74DF64897B71663B0E57A4EFE";

/*            if (data1[0].paymentrule != null && !data1[0].paymentrule.equals("")) {
		System.out.println("Inside If Block Prev Payment Rule");
              corder.paymentrule = data1[0].paymentrule;
		System.out.println("Inside If Block  After Payment Rule");
            } else {
		System.out.println("Inside Else Block Prev Payment Rule");
              String defaultPaymentRule = ImportOrderProcessData.defaultValue(con, conn, "C_Order",
                  "PaymentRule");
              corder.paymentrule = (defaultPaymentRule == null || defaultPaymentRule.equals("")) ? "P"
                  : defaultPaymentRule; // P
              // =
              // on
              // credit
		System.out.println("Inside Payment Rule"+defaultPaymentRule);
            }
	System.out.println("After Payment Rule");
            if (log4j.isDebugEnabled())
              log4j.debug("corder.paymentrule = " + corder.paymentrule);
            if (data1[0].invoicerule != null && !data1[0].invoicerule.equals("")) {
              corder.invoicerule = data1[0].invoicerule;
            } else {
              String defaultInvoiceRule = ImportOrderProcessData.defaultValue(con, conn, "C_Order",
                  "InvoiceRule");
              corder.invoicerule = (defaultInvoiceRule == null || defaultInvoiceRule.equals("")) ? "I"
                  : defaultInvoiceRule; // I
              // =
              // immediate
            }
        System.out.println("After Invoice Rule");
	if (log4j.isDebugEnabled())
              log4j.debug("corder.invoicerule = " + corder.invoicerule);
            if (data1[0].deliveryrule != null && !data1[0].deliveryrule.equals("")) {
              corder.deliveryrule = data1[0].deliveryrule;
            } else {
              String defaultDeliveryRule = ImportOrderProcessData.defaultValue(con, conn,
                  "C_Order", "DeliveryRule");
              corder.deliveryrule = (defaultDeliveryRule == null || defaultDeliveryRule.equals("")) ? "A"
                  : defaultDeliveryRule; // A
              // =
              // availability
            }
            if (log4j.isDebugEnabled())
              log4j.debug("corder.deliveryrule = " + corder.deliveryrule);
            if (data1[0].deliveryviarule != null && !data1[0].deliveryviarule.equals("")) {
              corder.deliveryviarule = data1[0].deliveryviarule;
            } else {
              String defaultDeliveryViaRule = ImportOrderProcessData.defaultValue(con, conn,
                  "C_Order", "DeliveryViaRule");
              corder.deliveryviarule = (defaultDeliveryViaRule == null || defaultDeliveryViaRule
                  .equals("")) ? "S" : defaultDeliveryViaRule; // S
              // =
              // shipper
            }
            if (log4j.isDebugEnabled())
              log4j.debug("corder.deliveryviarule = " + corder.deliveryviarule);
            corder.freightcostrule = "I"; // I = included
            if (log4j.isDebugEnabled())
              log4j.debug("corder.freightcostrule = " + corder.freightcostrule);
            if (data1[0].cPaymenttermId != null && !data1[0].cPaymenttermId.equals("")) {
              corder.cPaymenttermId = data1[0].cPaymenttermId;
            }
            if (log4j.isDebugEnabled())
              log4j.debug("corder.cPaymenttermId = " + corder.cPaymenttermId);
            if (data1[0].salesrepId != null && !data1[0].salesrepId.equals("")) {
              String salesrep = ImportOrderProcessData.selectSalesRep(con, conn,
                  data[i].cBpartnerId);
              corder.salesrepId = salesrep;
            }

            if (log4j.isDebugEnabled())
              log4j.debug("corder.salesrepId = " + corder.salesrepId);
*/
            String user = ImportOrderProcessData.defaultUser(con, conn, data[i].cBpartnerId);
            if (data[i].adUserId != null && !data[i].adUserId.equals("")) {
              corder.adUserId = data[i].adUserId;
            }

            if (log4j.isDebugEnabled())																																																																																													
              log4j.debug("other default values");
            corder.priorityrule = "5"; // medium
            corder_corderid = corder.cOrderId;
            corder_mpricelistid = corder.mPricelistId;
            corder_ccurrencyid = corder.cCurrencyId;
            corder_cbpartnerid = corder.cBpartnerId;
            corder_cbpartnerlocationid = corder.cBpartnerLocationId;
            // corder.totallines = data[i].paymentamount1;
            // corder.grandtotal =
            // String.valueOf((Integer.parseInt(data[i].paymentamount1)+Integer.parseInt(data[i].taxamt)));
            // corder.emDsReceiptno = data[i].emImReceiptno;
            // corder.emDsRatesatisfaction = data[i].emImCustomersatisfaction;
	    System.out.println("Import Order Process Before Order Header Insertion");
            try {
              corder.insert(con, conn);
		System.out.println("Import Order Process After Order Header Insertion");
            } catch (ServletException ex) {
		ex.printStackTrace();
              if (log4j.isDebugEnabled())
                log4j.debug("Insert Order - " + ex.toString());
              ImportOrder2Data.resetTriggersImporting(con, conn);
              conn.releaseRollbackConnection(con);
              ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
              continue;
            }
            noInsert++;
            lineNo = 10;
          } else {
            // Order with same Org, Doctype, DocumentNo, DateOrdered, BPartner
            // already exists.

            corder_corderid = orderInserted[0].cOrderId;
            order_documentno = orderInserted[0].documentno;
            corder_mpricelistid = orderInserted[0].mPricelistId;
            corder_ccurrencyid = orderInserted[0].cCurrencyId;
            corder_cbpartnerid = orderInserted[0].cBpartnerId;
            corder_cbpartnerlocationid = orderInserted[0].cBpartnerLocationId;
            lineNo = Integer.parseInt(orderInserted[0].linedescription);
          }
        }
        PaymentInfoData paymentInfo = new PaymentInfoData();
        PaymentInfoData[] data2 = paymentInfo.selectpaymentinfo(conn, data[i].emImReceiptno);
        CPaymentInfoData cPaymentInfo = new CPaymentInfoData();
        for (int j = 0; j < data2.length; j++) {
          cPaymentInfo.dsPaymentinfoId = SequenceIdData.getUUID();
          cPaymentInfo.adClientId = clientId;
          cPaymentInfo.adOrgId = orgId;
          cPaymentInfo.createdby = userId;
          cPaymentInfo.updatedby = userId;
          cPaymentInfo.paymentmode = data2[j].paymentmode;
          cPaymentInfo.identifier = data2[j].identifier;
          if (data2[j].splitamount != null && !data2[j].splitamount.equals(""))
            cPaymentInfo.amount = data2[j].splitamount;
          else
            cPaymentInfo.amount = data2[j].amount;
          cPaymentInfo.receiptno = data2[j].receiptno;
          System.out.println("payment info:" + data2[j].amount);
          cPaymentInfo.insert(con, conn);
        }
        no = paymentInfo.deletepaymentinfo(con, conn, data[i].emImReceiptno);
        if (log4j.isDebugEnabled()) {
          log4j.debug("Delete Receipt ids = " + no);
        }

        data[i].cOrderId = corder_corderid;
        // New OrderLine
        COrderLineData line = new COrderLineData();
        line.cOrderlineId = SequenceIdData.getUUID();
        line.adClientId = data[i].adClientId;
        line.adOrgId = data[i].adOrgId;
        line.adUserId = vars.getUser();
        line.cOrderId = corder_corderid;
        line.line = Integer.toString(lineNo);
        if (log4j.isDebugEnabled())
          log4j.debug("reading order line number: " + line.line);
        line.description = data[i].linedescription;
        line.cBpartnerId = corder_cbpartnerid;
        line.cBpartnerLocationId = corder_cbpartnerlocationid;
        lineNo += 10;
        if (data[i].mProductId != null && !data[i].mProductId.equals(""))
          line.mProductId = data[i].mProductId;
        //
        if (data[i].qtyordered != null && !data[i].qtyordered.equals("")) {
          try {
            System.out.println("data[i].qtyordered:" + data[i].qtyordered);
            qtyUnit = Integer.parseInt(data[i].qtyordered);
            System.out.println("qtyUnit:" + qtyUnit);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        if (data[i].emImUeqty != null && !data[i].emImUeqty.equals("")) {
          try {
            System.out.println("data[i].emImUeqty:" + data[i].emImUeqty);
            qtyUe = Integer.parseInt(data[i].emImUeqty);
            System.out.println("qtyUnit:" + qtyUe);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        if (data[i].emImPcbqty != null && !data[i].emImPcbqty.equals("")) {
          try {
            System.out.println("data[i].emImPcbqty:" + data[i].emImPcbqty);
            qtyPcb = Integer.parseInt(data[i].emImPcbqty);
            System.out.println("qtyUnit:" + qtyPcb);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        // line.qtyordered = String.valueOf((Integer.parseInt(data[i].qtyordered) +
        // Integer.parseInt(data[i].emImUeqty) +Integer.parseInt(data[i].emImPcbqty)));
        // line.qtyordered = data[i].qtyordered;
        System.out.println("qtyUnit+qtyUe+qtyPcb:" + String.valueOf(qtyUnit + qtyUe + qtyPcb));
        line.qtyordered = String.valueOf(qtyUnit + qtyUe + qtyPcb);
        line.emDsUnitqty = data[i].qtyordered;
        line.emDsLotqty = data[i].emImUeqty;
        line.emDsBoxqty = data[i].emImPcbqty;
        line.emDsLotqty = (data[i].emImUeqty == null && data[i].emImUeqty.equals("")) ? "0"
            : data[i].emImUeqty;
        line.emDsBoxqty = (data[i].emImPcbqty == null && data[i].emImPcbqty.equals("")) ? "0"
            : data[i].emImPcbqty;
        // set price
        if (line.mProductId != null && !line.mProductId.equals("")) {
          ProductPriceData[] pprice = ProductPriceData.selectPL(conn, line.mProductId,
              corder_mpricelistid);
          line.emDsLotprice = data[i].emImUeprice;
          line.emDsBoxprice = data[i].emImPcbprice;
          line.emDsLotprice = (data[i].emImUeprice == null && data[i].emImUeprice.equals("")) ? "0"
              : data[i].emImUeprice;
          line.emDsBoxprice = (data[i].emImPcbprice == null && data[i].emImPcbprice.equals("")) ? "0"
              : data[i].emImPcbprice;
          if (pprice.length > 0) {
            //
            line.pricestd = pprice[0].pricestd;
            line.priceactual = pprice[0].pricestd;
            line.pricelist = (pprice[0].pricelist == null && pprice[0].pricelist.equals("")) ? "0"
                : pprice[0].pricelist;
		System.out.println("PriceList :"+line.pricelist+"Product :" +line.mProductId);
            line.pricelimit = (pprice[0].pricelimit == null && pprice[0].pricelimit.equals("")) ? "0"
                : pprice[0].pricelimit;
            line.discount = ((new BigDecimal(pprice[0].pricelist).compareTo(BigDecimal.ZERO) == 0) ? "0"
                : (((new BigDecimal(pprice[0].pricelist).subtract(new BigDecimal(line.priceactual)))
                    .divide(new BigDecimal(pprice[0].pricelist), 12, BigDecimal.ROUND_HALF_EVEN))
                    .multiply(new BigDecimal("100"))).toPlainString()); // ((PL-PA)/PL)*100
            line.cUomId = pprice[0].cUomId;
          } else {
            if (log4j.isDebugEnabled())
              log4j.debug("Could not establish prices");
          }
        } // set price
        if (data[i].priceactual != null && !data[i].priceactual.equals("")
            && new BigDecimal(data[i].priceactual).compareTo(BigDecimal.ZERO) != 0)
          line.priceactual = data[i].priceactual;
        if (data[i].cTaxId != null && !data[i].cTaxId.equals(""))
          line.cTaxId = data[i].cTaxId;
        else {
          try {
            line.cTaxId = Tax.get(conn, data[i].mProductId, DateTimeData.today(conn),
                data[i].adOrgId, data[i].mWarehouseId.equals("") ? vars.getWarehouse()
                    : data[i].mWarehouseId, ImportOrderProcessData.cBPartnerLocationId(conn,
                    data[i].cBpartnerId), ImportOrderProcessData.cBPartnerLocationId(conn,
                    data[i].cBpartnerId), data[i].cProjectId, true);
          } catch (IOException ioe) {
            if (log4j.isDebugEnabled())
              log4j.debug("IOException");
          }
        }
        if (line.cTaxId == null || line.cTaxId.equals(""))
          line.cTaxId = ProductPriceData.selectCTaxId(conn, vars.getClient());
        data[i].cTaxId = line.cTaxId;

        // line.dateordered = data[i].dateordered;
        line.dateordered = data[i].dateordered.equals("") ? DateTimeData.today(conn)
            : data[i].dateordered;
        line.mWarehouseId = (data[i].mWarehouseId == null || data[i].mWarehouseId.equals("")) ? vars
            .getWarehouse() : data[i].mWarehouseId;
        if (line.cUomId == null || line.cUomId.equals(""))
          line.cUomId = ProductPriceData.selectCUomIdByProduct(conn, line.mProductId);
        if (line.cUomId == null || line.cUomId.equals(""))
          line.cUomId = ProductPriceData.selectCUomIdDefault(conn);
        line.cCurrencyId = (data[i].cCurrencyId == null || data[i].cCurrencyId.equals("")) ? corder_ccurrencyid
            : data[i].cCurrencyId;
	System.out.println("Import Order Process Before Order Line Insertion");
        try {
          line.insert(con, conn);
	  System.out.println("Import Order Process After Order Line Insertion All is Well! :)");
        } catch (ServletException ex) {
          if (log4j.isDebugEnabled())
            log4j.debug("Insert Order - " + ex.toString());
          ImportOrder2Data.resetTriggersImporting(con, conn);
          conn.releaseRollbackConnection(con);
          ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
          noInsert--;
          continue;
        }

        String[] arrayPayment = { data[i].paymentrule1, data[i].paymentrule2 };
        String[] arrayAmount = { data[i].paymentamount1, data[i].paymentamount2 };
        for (int k = 0; k < arrayPayment.length; k++) {
          if (!arrayPayment[k].equals("")) {
            CDebtpaymentData cdebtpayment = new CDebtpaymentData();
            cdebtpayment.adClientId = data[i].adClientId;
            cdebtpayment.adOrgId = data[i].adOrgId;
            cdebtpayment.createdby = userId;
            cdebtpayment.updatedby = userId;
            cdebtpayment.cBpartnerId = corder_cbpartnerid;
            cdebtpayment.cCurrencyId = corder_ccurrencyid;
            /*
             * cdebtpayment.cBankaccountId = ; cdebtpayment.cCashbookId = ;
             */
            cdebtpayment.paymentrule = arrayPayment[k];
            cdebtpayment.amount = arrayAmount[k];
            cdebtpayment.ispaid = "N";
            cdebtpayment.dateplanned = data[i].dateordered.equals("") ? DateTimeData.today(conn)
                : data[i].dateordered;
            cdebtpayment.ismanual = "N";
            cdebtpayment.isvalid = "Y";
            cdebtpayment.changesettlementcancel = "N";
            cdebtpayment.cancelProcessed = "N";
            cdebtpayment.generateProcessed = "N";
            cdebtpayment.glitemamt = "0";
            cdebtpayment.isdirectposting = "N";
            cdebtpayment.status = "DE";
            cdebtpayment.statusInitial = "DE";
            cdebtpayment.cOrderId = data[i].cOrderId;
            // Looking for same payment yet inserted
            ImportOrderProcessData[] paymentInserted = ImportOrderProcessData
                .selectPaymentInserted(conn, cdebtpayment.adClientId, cdebtpayment.adOrgId,
                    cdebtpayment.createdby, cdebtpayment.updatedby, cdebtpayment.cBpartnerId,
                    cdebtpayment.cCurrencyId, cdebtpayment.paymentrule, cdebtpayment.amount,
                    cdebtpayment.ispaid, cdebtpayment.ismanual, cdebtpayment.isvalid,
                    cdebtpayment.changesettlementcancel, cdebtpayment.cancelProcessed,
                    cdebtpayment.generateProcessed, cdebtpayment.glitemamt,
                    cdebtpayment.isdirectposting, cdebtpayment.status, cdebtpayment.statusInitial,
                    cdebtpayment.cOrderId);
            if (paymentInserted != null && paymentInserted.length == 0) {
              try {
                cdebtpayment.cDebtPaymentId = SequenceIdData.getUUID();
                cdebtpayment.insert(con, conn);
              } catch (ServletException ex) {
                if (log4j.isDebugEnabled())
                  log4j.debug("Insert Order - " + ex.toString());
                ImportOrder2Data.resetTriggersImporting(con, conn);
                conn.releaseRollbackConnection(con);
                ImportOrderProcessData.importOrderError(conn, ex.toString(), I_Order_ID);
                noInsert--;
                continue;
              }
            }
          }
        }

        data[i].cOrderlineId = line.cOrderlineId;
        data[i].iIsimported = "Y";
        data[i].processed = "Y";
	System.out.println("Second update");
        data[i].update(con, conn);

        try {
          if (data[i].performPost.equals("Y") || m_processOrders) {
            if (i != data.length - 1) {
              if (!order_documentno.equals(data[i + 1].documentno))
                strPostMessage += cOrderPost(con, conn, vars, data[i].cOrderId, order_documentno)
                    + "<br>";
            } else {
              strPostMessage += cOrderPost(con, conn, vars, data[i].cOrderId, order_documentno)
                  + "<br>";
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
          log4j.debug("Post error");
          ImportOrderProcessData.updatePostError(con, conn, strPostMessage,
              data[i].orderReferenceno, clientId);
        }
        noInsertLine++;
        ImportOrder2Data.resetTriggersImporting(con, conn);
        conn.releaseCommitConnection(con);
      }
      con = conn.getTransactionConnection();
      noOrderError = ImportOrderProcessData.updateNotImported(con, conn, clientId);

      if (noOrderError > 0) {
        // addLog(Utility.messageBD(conn, "Order lines not imported", vars.getLanguage()) + ": "
        // + noOrderError + "; ");
      }
      // addLog("Orders inserted: " + noInsert + "; ");
      // addLog("Orders line inserted: " + noInsertLine + "; " + "<br>");
      if (noInsert > totalProcessed) {
        // addLog(Utility.messageBD(conn, "Orders not processed", vars.getLanguage()) + ": "
        // + (noInsert - totalProcessed) + "; ");
      }
      // addLog("Orders processed: " + Integer.toString(totalProcessed) + "; " + "<br>");
      if (strPostMessage != null && !strPostMessage.equals("")) {
        // addLog("Process result: " + "<br>" + strPostMessage);
      }

      if (noOrderError == 0 && noInsert == totalProcessed) {
        myError.setType("Success");
        myError.setTitle(Utility.messageBD(conn, "Success", vars.getLanguage()));
      } else if (noInsert > 0 || noInsertLine > 0) {
        myError.setType("Warning");
        myError.setTitle(Utility.messageBD(conn, "Some orders could not be imported or processed",
            vars.getLanguage()));
      } else {
        myError.setType("Error");
        myError
            .setTitle(Utility.messageBD(conn, " No orders could be imported", vars.getLanguage()));
      }
      // myError.setMessage(Utility.messageBD(conn, getLog(), vars.getLanguage()));

      conn.releaseCommitConnection(con);
    } catch (NoConnectionAvailableException ex) {
      throw new ServletException("@CODE=NoConnectionAvailable");
    } catch (SQLException ex2) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception ignored) {
      }
      throw new ServletException("@CODE=" + Integer.toString(ex2.getErrorCode()) + "@"
          + ex2.getMessage());
    } catch (Exception ex3) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception ignored) {
      }
      throw new ServletException("@CODE=@" + ex3.getMessage());
    }
    finally {
		try{
         con.close();
       }catch(Exception exx)
       {
		    throw new ServletException("Exception while closing connection" + exx.getMessage());
		   }
     }
    return myError;

  }

  String cOrderPost(Connection con, ConnectionProvider conn, VariablesSecureApp vars,
      String strcOrderId, String order_documentno) throws IOException, ServletException {
    String pinstance = SequenceIdData.getUUID();
    PInstanceProcessData.insertPInstance(con, conn, pinstance, "104", strcOrderId, "N",
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
    }
    return messageResult;
  }

}
