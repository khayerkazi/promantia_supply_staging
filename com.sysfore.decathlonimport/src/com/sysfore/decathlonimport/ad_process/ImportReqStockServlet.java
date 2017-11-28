/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2009 Openbravo S.L.U.
 ******************************************************************************
 */
package com.sysfore.decathlonimport.ad_process;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonDefaultData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ImportReqStockServlet extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private ConnectionProvider connection;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    String process = "ImportReqStock";
    // ImportData.processId(this, "ImportProduct");

    if (vars.commandIn("DEFAULT")) {
      System.out.println("Inside the Default function");
      String strTabId = vars.getGlobalVariable("inpTabId", "ImportReqStockServlet|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId", "ImportReqStockServlet|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
    }

    else if (vars.commandIn("SAVE")) {
      System.out.println("Inside the Inventory Save");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "ImportReqStockServlet|tabId");
      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

      OBError myError = new OBError();

      // vars.setMessage(strTabId, myError);
      // ConnectionProvider conn=vars.getConnection();

      String result = "";
      String client = vars.getClient();
      String organistation = vars.getOrg();
      String user = vars.getUser();
      String storetype = "", documentno = "", itemcode = "", mproductid = "", swsreqid = "", Strqtyintemp = "", docaction = "", processing = "", processed = "", docstatus = "", requisitionstatus = "", strlocatorefrom = "", strlocatoreto = "", strwhorgid = "", strshipqty = "", strmLocatorID = "", strBoxN0 = "", attributese = "", strShuttelheaderID = "", strBrandID = "", strSize = "", strColor = "", strModelname = "", storeImplanted = "";
      int updraft = 0, qtyrec = 0, qtyship = 0, upline = 0, mt1 = 0, mt2 = 0, mt3 = 0, mt4 = 0, movqty = 0, V_headerflag = 0, headerid = 0, shuline = 0, lineno = 10, corderline = 0;

      String strRTVheaderID = "";
      String strRFCheaderID = "";

      // c_order columns
      String adclientid = "", adorgid = "", isactive = "", created = "", createdby = "", updated = "", updatedby = "", issotrx = "", dcumentno = "", dcstatus = "", dcaction = "", cprocessing = "", cprocessed = "", description = "", isdelivered = "", isinvoiced = "", isprinted = "", isselected = "", salesrepid = "", cbpartnerid = "", billtoid = "", cbpartnerlocationid = "", poreference = "", isdiscountprinted = "", ccurrencyid = "", paymentrule = "", cpaymenttermid = "", invoicerule = "", deliveryrule = "", freightcostrule = "", freightamt = "", deliveryviarule = "", mshipperid = "", cchargeid = "", chargeamt = "", priorityrule = "", totallines = "", grandtotal = "", mwarehouseid = "", mpricelistid = "", istaxincluded = "", ccampaignid = "", cprojectid = "", cactivityid = "", posted = "", aduserid = "", copyfrom = "", dropshipbpartnerid = "", dropshiplocationid = "", dropshipuserid = "", isselfservice = "", adorgtrxid = "", user1id = "", user2id = "", deliverynotes = "", cincotermsid = "", incotermsdescription = "", generatetemplate = "", deliverylocationid = "", copyfrompo = "", finpaymentmethodid = "", finpaymentpriorityid = "", rmpickfromshipment = "", rmreceivematerials = "", rmcreateinvoice = "", creturnreasonid = "", rmaddorphanline = "", aassetid = "", calculatepromotions = "", ccostcenterid = "", convertquotation = "", crejectreasonid = "", validuntil = "", quotationid = "", soresstatus = "", emdstotalitemqty = "", emdstotalpriceadj = "", emdsposno = "", emdstime = "", emdsreceiptno = "", emdsratesatisfaction = "", emdsgrandtotalamt = "", emdschargeamt = "", emrcmobileno = "", emrcoxylaneno = "", emswhscode = "", emswmodelcode = "", emswmodelname = "", emswdeptid = "", emswbrandid = "", emswexpdeldate = "", emswestshipdate = "", emswactshipdate = "", emswpricelistver = "", emswcurrency = "", emswpostatus = "", emobwplgeneratepicking = "", emobwplisinpickinglist = "";

      // emobdiscaddpack="", emobposapplicationsid="", emobpossendemail="", emobposemailstatus="",

      // c_orderline columns
      String corderlineid = "", line = "", datedelivered = "", dateinvoiced = "", cmproductid = "", directship = "", cuomid = "", qtyordered = "", qtyreserved = "", qtydelivered = "0", qtyinvoiced = "", pricelist = "", priceactual = "", pricelimit = "", linenetamt = "", discount = "", ctaxid = "", sresourceassignmentid = "", reforderlineid = "", mattributesetinstanceid = "", isdescription = "", quantityorder = "", mproductuomid = "", mofferid = "", pricestd = "", cancelpricead = "", corderdiscountid = "", iseditlinenetamt = "", taxbaseamt = "", minoutlineid = "", linegrossamount = "", grosspricelist = "", grosspricestd = "", mwarehouseruleid = "", quotationlineid = "", createreservation = "", managereservation = "", manageprereservation = "", explode = "", bomparentid = "", emdssalesexcl = "", emdstaxamount = "", emdslotqty = "", emdslotprice = "", emdsboxqty = "", emdsboxprice = "", emdsunitqty = "", emdscessionprice = "", emdsccunitprice = "", emdsmrpprice = "", emdslinenetamt = "", emsworderqty = "", emswsuppliercode = "", emswvolpcb = "", emswntwtpcb = "", emswgrwtpcb = "", emswnoofparcel = "", emswitemcode = "", emswcolor = "", emswsize = "", emswrecqty = "", emswconfirmedqty = "";

      String dateordered = null;
      String datepromised = null;
      String dateprinted = null;
      String dateacct = null;

      float grossunitprice = 0.0f;
      float grossAmount = 0.0f;

      float qtyrecvd = 0.0f;

      // result = ImportInventoryData.importinventory(this, client, organistation, user);

      myError.setType("Success");
      myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      myError.setMessage(Utility.messageBD(this, result, vars.getLanguage()));
      vars.setMessage(strTabId, myError);

      storetype = ImportReqStockData.isstore(this, organistation);

      System.out.println("organistation " + organistation);
      System.out.println("storetype " + storetype);

      // SELECT distinct documentno from im_stockreplenishment where validatestock='Y' and createdby
      // = 'user'
      ImportReqStockData datadocument[] = ImportReqStockData.selectDcno(this, user);

      // System.out.println("datadocument length"+datadocument.length+" user"+user);

      if (datadocument.length > 0) {
        for (int i = 0; i < datadocument.length; i++) {
          // Get the first document Number
          documentno = datadocument[i].documentno;

          // set the ad_org_id as Organisation where storetype = 'y'
          if (storetype.equals("Y")) {
            strwhorgid = organistation;
          } else {
            // select ad_org_id from sw_srequisition where documentno='documentno'
            strwhorgid = ImportReqStockData.selectorg(this, documentno);
            System.out.println("documentno" + documentno + "   strwhorgid" + strwhorgid);
          }
          // System.out.print(strwhorgid);

          // SELECT locatorefrom, locatoreto from sw_movementlocatore where movementype='ITC' and
          // ad_org_id='strwhorgid'
          ImportReqStock2Data locatordata[] = ImportReqStock2Data.selectlocatores(this, strwhorgid);

          System.out.println("locatordata.length" + locatordata.length);
          if (locatordata.length > 0) {
            for (int l = 0; l < locatordata.length; l++) {
              // System.out.println("In side for loop");
              strlocatorefrom = locatordata[l].locatorefrom;
              // System.out.println("strlocatorefrom  " + strlocatorefrom);
              strlocatoreto = locatordata[l].locatoreto;
              // System.out.println("strlocatoreto " + strlocatoreto);
            }
          } // END if(locatordata.length>0)

          strShuttelheaderID = SequenceIdData.getUUID();

          strRTVheaderID = SequenceIdData.getUUID();

          strRFCheaderID = SequenceIdData.getUUID();

          // select itemcode, qty from im_stockreplenishment where documentno='documentno'
          ImportReqStock1Data data[] = ImportReqStock1Data.selectdocumentanditem(this, documentno);
          System.out.println("data.length" + data.length);
          if (data.length > 0) {
            for (int d = 0; d < data.length; d++) {

              System.out.println("documentno " + documentno);
              itemcode = data[d].itemcode;
              Strqtyintemp = data[d].qty;
              System.out.println("itemcode " + itemcode);
              mproductid = ImportReqStockData.selectmproductId(this, itemcode);
              System.out.println("mproductid " + mproductid);
              // Commented on 28 NOV
              // updraft=ImportReqStockData.updatesrequisitiontodraft(this,documentno);
              swsreqid = ImportReqStockData.swsrequisitionid(this, documentno);
              // strshipqty=ImportReqStockData.getshipqty(this,mproductid,swsreqid);
              System.out.println("  Strqtyintemp " + Strqtyintemp);
              movqty = Integer.parseInt(Strqtyintemp);
              // System.out.println("Ship qty "+strshipqty);
              if (storetype.equals("Y")) {
                docaction = "CL";
                processing = "N";

                processed = "Y";
                docstatus = "CL";
                requisitionstatus = "CD";
                strmLocatorID = "F0F02D9BAFD540B0896371A95F0B5A13";
                strBoxN0 = "";
                storeImplanted = organistation;
                qtyship = Integer.parseInt(Strqtyintemp);
                qtyrec = Integer.parseInt(Strqtyintemp);

                // strBrandID, strSize, strColor, strModelname
                ImportReqStock4Data Reqlinedata[] = ImportReqStock4Data.selectreqline(this,
                    mproductid, documentno);
                if (Reqlinedata.length > 0) {
                  strBrandID = Reqlinedata[0].clBrandId;
                  strSize = Reqlinedata[0].size;
                  strColor = Reqlinedata[0].color;
                  strModelname = Reqlinedata[0].modelname;
                }
                // System.out.println("movqty qty "+movqty +" for item code " + mproductid);
                // System.out.println("V_headerflag --->> " + V_headerflag);

                // movementqty of GR

                String grqty = ImportReqStock5Data.selectqtygr(this, documentno, itemcode);
                minoutlineid = ImportReqStock5Data.selectminoutid(this, documentno, itemcode);
                System.out.println(" grqty  " + grqty + "minout Id is " + minoutlineid);
                System.out.println("movqty is " + movqty);
                System.out.println("grqty is " + grqty);
                qtyrecvd = Float.parseFloat("" + movqty) - (Float.parseFloat(grqty));

                /*
                 * if(qtyrecvd >0.0){ qtyrecvd=-qtyrecvd; }
                 */

                // c_order and c_oderline
                ImportReqStock5Data Reqcorderlinedata[] = ImportReqStock5Data.selectcorderandline(
                    this, documentno, itemcode);

                if (Reqcorderlinedata.length > 0) {

                  // c_order columns
                  adclientid = Reqcorderlinedata[0].adClientId;
                  adorgid = Reqcorderlinedata[0].adOrgId;
                  isactive = Reqcorderlinedata[0].isactive;
                  created = Reqcorderlinedata[0].created;
                  createdby = Reqcorderlinedata[0].createdby;
                  updated = Reqcorderlinedata[0].updated;
                  updatedby = Reqcorderlinedata[0].updatedby;
                  issotrx = Reqcorderlinedata[0].issotrx;
                  dcumentno = Reqcorderlinedata[0].documentno;
                  dcstatus = Reqcorderlinedata[0].docstatus;
                  dcaction = Reqcorderlinedata[0].docaction;
                  cprocessing = Reqcorderlinedata[0].processing;
                  cprocessed = Reqcorderlinedata[0].processed;
                  description = Reqcorderlinedata[0].description;
                  isdelivered = Reqcorderlinedata[0].isdelivered;
                  isinvoiced = Reqcorderlinedata[0].isinvoiced;
                  isprinted = Reqcorderlinedata[0].isprinted;
                  isselected = Reqcorderlinedata[0].isselected;
                  salesrepid = Reqcorderlinedata[0].salesrepId;
                  dateordered = Reqcorderlinedata[0].dateordered;
                  datepromised = Reqcorderlinedata[0].datepromised;
                  dateprinted = Reqcorderlinedata[0].dateprinted;
                  dateacct = Reqcorderlinedata[0].dateacct;
                  cbpartnerid = Reqcorderlinedata[0].cBpartnerId;
                  billtoid = Reqcorderlinedata[0].billtoId;
                  cbpartnerlocationid = Reqcorderlinedata[0].cBpartnerLocationId;
                  poreference = Reqcorderlinedata[0].poreference;
                  isdiscountprinted = Reqcorderlinedata[0].isdiscountprinted;
                  ccurrencyid = Reqcorderlinedata[0].cCurrencyId;
                  paymentrule = Reqcorderlinedata[0].paymentrule;
                  cpaymenttermid = Reqcorderlinedata[0].cPaymenttermId;
                  invoicerule = Reqcorderlinedata[0].invoicerule;
                  deliveryrule = Reqcorderlinedata[0].deliveryrule;
                  freightcostrule = Reqcorderlinedata[0].freightcostrule;
                  freightamt = Reqcorderlinedata[0].freightamt;
                  deliveryviarule = Reqcorderlinedata[0].deliveryviarule;
                  mshipperid = Reqcorderlinedata[0].mShipperId;
                  cchargeid = Reqcorderlinedata[0].cChargeId;
                  chargeamt = Reqcorderlinedata[0].chargeamt;
                  priorityrule = Reqcorderlinedata[0].priorityrule;
                  totallines = Reqcorderlinedata[0].totallines;
                  grandtotal = Reqcorderlinedata[0].grandtotal;
                  mwarehouseid = Reqcorderlinedata[0].mWarehouseId;
                  mpricelistid = Reqcorderlinedata[0].mPricelistId;
                  istaxincluded = Reqcorderlinedata[0].istaxincluded;
                  ccampaignid = Reqcorderlinedata[0].cCampaignId;
                  cprojectid = Reqcorderlinedata[0].cProjectId;
                  cactivityid = Reqcorderlinedata[0].cActivityId;
                  posted = Reqcorderlinedata[0].posted;
                  aduserid = Reqcorderlinedata[0].adUserId;
                  copyfrom = Reqcorderlinedata[0].copyfrom;
                  dropshipbpartnerid = Reqcorderlinedata[0].dropshipBpartnerId;
                  dropshiplocationid = Reqcorderlinedata[0].dropshipLocationId;
                  dropshipuserid = Reqcorderlinedata[0].dropshipUserId;
                  isselfservice = Reqcorderlinedata[0].isselfservice;
                  adorgtrxid = Reqcorderlinedata[0].adOrgtrxId;
                  user1id = Reqcorderlinedata[0].user1Id;
                  user2id = Reqcorderlinedata[0].user2Id;
                  deliverynotes = Reqcorderlinedata[0].deliverynotes;
                  cincotermsid = Reqcorderlinedata[0].cIncotermsId;
                  incotermsdescription = Reqcorderlinedata[0].incotermsdescription;
                  generatetemplate = Reqcorderlinedata[0].generatetemplate;
                  deliverylocationid = Reqcorderlinedata[0].deliveryLocationId;
                  copyfrompo = Reqcorderlinedata[0].copyfrompo;
                  finpaymentmethodid = Reqcorderlinedata[0].finPaymentmethodId;
                  finpaymentpriorityid = Reqcorderlinedata[0].finPaymentPriorityId;
                  rmpickfromshipment = Reqcorderlinedata[0].rmPickfromshipment;
                  rmreceivematerials = Reqcorderlinedata[0].rmReceivematerials;
                  rmcreateinvoice = Reqcorderlinedata[0].rmCreateinvoice;

                  creturnreasonid = ImportReqStock5Data.selectreasonID(this);// return reason

                  rmaddorphanline = Reqcorderlinedata[0].rmAddorphanline;
                  aassetid = Reqcorderlinedata[0].aAssetId;
                  calculatepromotions = Reqcorderlinedata[0].calculatePromotions;
                  ccostcenterid = Reqcorderlinedata[0].cCostcenterId;
                  convertquotation = Reqcorderlinedata[0].convertquotation;
                  crejectreasonid = Reqcorderlinedata[0].cRejectReasonId;
                  validuntil = Reqcorderlinedata[0].validuntil;
                  quotationid = Reqcorderlinedata[0].quotationId;
                  soresstatus = Reqcorderlinedata[0].soResStatus;
                  emdstotalitemqty = Reqcorderlinedata[0].emDsTotalitemqty;
                  emdstotalpriceadj = Reqcorderlinedata[0].emDsTotalpriceadj;
                  emdsposno = Reqcorderlinedata[0].emDsPosno;
                  emdstime = Reqcorderlinedata[0].emDsTime;
                  emdsreceiptno = Reqcorderlinedata[0].emDsReceiptno;
                  emdsratesatisfaction = Reqcorderlinedata[0].emDsRatesatisfaction;
                  emdsgrandtotalamt = Reqcorderlinedata[0].emDsGrandtotalamt;
                  emdschargeamt = Reqcorderlinedata[0].emDsChargeamt;
                  emrcmobileno = Reqcorderlinedata[0].emRcMobileno;
                  emrcoxylaneno = Reqcorderlinedata[0].emRcOxylaneno;
                  emswhscode = Reqcorderlinedata[0].emSwHscode;
                  emswmodelcode = Reqcorderlinedata[0].emSwModelcode;
                  emswmodelname = Reqcorderlinedata[0].emSwModelname;
                  emswdeptid = Reqcorderlinedata[0].emSwDeptId;
                  emswbrandid = Reqcorderlinedata[0].emSwBrandId;
                  emswexpdeldate = Reqcorderlinedata[0].emSwExpdeldate;
                  emswestshipdate = Reqcorderlinedata[0].emSwEstshipdate;
                  emswactshipdate = Reqcorderlinedata[0].emSwActshipdate;
                  emswpricelistver = Reqcorderlinedata[0].emSwPricelistver;
                  emswcurrency = Reqcorderlinedata[0].emSwCurrency;
                  emswpostatus = Reqcorderlinedata[0].emSwPostatus;
                  // emobdiscaddpack =Reqcorderlinedata[0].emObdiscAddpack;
                  // emobposapplicationsid =Reqcorderlinedata[0].emObposApplicationsId;
                  // emobpossendemail =Reqcorderlinedata[0].emObposSendemail;
                  // emobposemailstatus =Reqcorderlinedata[0].emObposEmailStatus;
                  emobwplgeneratepicking = Reqcorderlinedata[0].emObwplGeneratepicking;
                  emobwplisinpickinglist = Reqcorderlinedata[0].emObwplIsinpickinglist;

                  // c_orderline columns
                  corderlineid = Reqcorderlinedata[0].cOrderlineId;
                  line = Reqcorderlinedata[0].line;
                  cmproductid = Reqcorderlinedata[0].mProductId;
                  directship = Reqcorderlinedata[0].directship;
                  cuomid = Reqcorderlinedata[0].cUomId;
                  qtyordered = Reqcorderlinedata[0].qtyordered;
                  qtyreserved = Reqcorderlinedata[0].qtyreserved;
                  qtydelivered = "0";
                  // qtydelivered = Reqcorderlinedata[0].qtydelivered;
                  qtyinvoiced = Reqcorderlinedata[0].qtyinvoiced;
                  pricelist = Reqcorderlinedata[0].pricelist;
                  priceactual = Reqcorderlinedata[0].priceactual;
                  pricelimit = Reqcorderlinedata[0].pricelimit;
                  linenetamt = Reqcorderlinedata[0].linenetamt;
                  discount = Reqcorderlinedata[0].discount;
                  ctaxid = Reqcorderlinedata[0].cTaxId;
                  sresourceassignmentid = Reqcorderlinedata[0].sResourceassignmentId;
                  reforderlineid = Reqcorderlinedata[0].refOrderlineId;
                  // mattributesetinstanceid = Reqcorderlinedata[0].mAttributesetinstanceId;
                  mattributesetinstanceid = "0";
                  isdescription = Reqcorderlinedata[0].isdescription;
                  quantityorder = Reqcorderlinedata[0].quantityorder;
                  mproductuomid = Reqcorderlinedata[0].mProductUomId;
                  mofferid = Reqcorderlinedata[0].mOfferId;
                  pricestd = Reqcorderlinedata[0].pricestd;
                  cancelpricead = Reqcorderlinedata[0].cancelpricead;
                  corderdiscountid = Reqcorderlinedata[0].cOrderDiscountId;
                  iseditlinenetamt = Reqcorderlinedata[0].iseditlinenetamt;
                  taxbaseamt = Reqcorderlinedata[0].taxbaseamt;
                  // minoutlineid =Reqcorderlinedata[0].mInoutlineId;
                  grossunitprice = Float.parseFloat(Reqcorderlinedata[0].grossUnitPrice);
                  linegrossamount = Reqcorderlinedata[0].lineGrossAmount;
                  grosspricelist = Reqcorderlinedata[0].grosspricelist;
                  grosspricestd = Reqcorderlinedata[0].grosspricestd;
                  mwarehouseruleid = Reqcorderlinedata[0].mWarehouseRuleId;
                  quotationlineid = Reqcorderlinedata[0].quotationlineId;
                  createreservation = Reqcorderlinedata[0].createReservation;
                  managereservation = Reqcorderlinedata[0].manageReservation;
                  manageprereservation = Reqcorderlinedata[0].managePrereservation;
                  explode = Reqcorderlinedata[0].explode;
                  bomparentid = Reqcorderlinedata[0].bomParentId;
                  emdssalesexcl = Reqcorderlinedata[0].emDsSalesexcl;
                  emdstaxamount = Reqcorderlinedata[0].emDsTaxamount;
                  emdslotqty = Reqcorderlinedata[0].emDsLotqty;
                  emdslotprice = Reqcorderlinedata[0].emDsLotprice;
                  emdsboxqty = Reqcorderlinedata[0].emDsBoxqty;
                  emdsboxprice = Reqcorderlinedata[0].emDsBoxprice;
                  emdsunitqty = Reqcorderlinedata[0].emDsUnitqty;
                  emdscessionprice = Reqcorderlinedata[0].emDsCessionprice;
                  emdsccunitprice = Reqcorderlinedata[0].emDsCcunitprice;
                  emdsmrpprice = Reqcorderlinedata[0].emDsMrpprice;
                  emdslinenetamt = Reqcorderlinedata[0].emDsLinenetamt;
                  emsworderqty = Reqcorderlinedata[0].emSwOrderqty;
                  emswsuppliercode = Reqcorderlinedata[0].emSwSuppliercode;
                  emswvolpcb = Reqcorderlinedata[0].emSwVolpcb;
                  emswntwtpcb = Reqcorderlinedata[0].emSwNtwtpcb;
                  emswgrwtpcb = Reqcorderlinedata[0].emSwGrwtpcb;
                  emswnoofparcel = Reqcorderlinedata[0].emSwNoofparcel;
                  emswitemcode = Reqcorderlinedata[0].emSwItemcode;
                  // emswcolor =Reqcorderlinedata[0].emClColor;
                  // emswsize =Reqcorderlinedata[0].emClSize;
                  emswcolor = "";
                  emswsize = "";
                  emswrecqty = Reqcorderlinedata[0].emSwRecqty;
                  emswconfirmedqty = Reqcorderlinedata[0].emSwConfirmedqty;

                  grossAmount = grossunitprice * movqty;
                  System.out.println("grossunitprice  " + grossunitprice);
                  System.out.println("qtydelivered " + qtydelivered);
                  System.out.println("qtyrecvd" + qtyrecvd);
                }
                if (qtyrecvd < 0.0) {
                  // insert into header and then line
                  if (V_headerflag == 0) {
                    // create header and line
                    // headerid=ImportReqStockData.insertshuttelheader(this, strShuttelheaderID,
                    // client,organistation, user, user,documentno);

                    /*
                     * // Dyuti Changes headerid = ImportReqStockData.insertshuttelheader(this,
                     * strShuttelheaderID, client, "603C6A266B4C40BCAD87C5C43DDF53EE",
                     * storeImplanted, user, user, documentno); // END
                     */
                    // Siddesh Changes insert into c_order as RTV
                    cprocessed = "N";
                    // Code change by Venki for inserting into COrder only if the qtyrecvd is
                    // Negative

                    String adBOrgID = ImportReqStock5Data.selectBOrgId(this, cbpartnerid);
                    log4j.info(cbpartnerid + " adBusinessOrgID = " + adBOrgID);
                    String bWHouseID = ImportReqStock5Data.selectBWHouseID(this, adBOrgID);
                    log4j.info("bWarehouseHouseID = " + bWHouseID);
                    String bpID = ImportReqStock5Data.selectbpID(this, adorgid);
                    log4j.info(adorgid + " businesspartnerID = " + bpID);
                    String bplocationID = ImportReqStock5Data.selectbplocationID(this, bpID);
                    log4j.info("businesspartnerLocationID = " + bplocationID);
                    headerid = ImportReqStock5Data.insertcorderheader(this, strRTVheaderID,
                        adclientid, adBOrgID, isactive, createdby, updatedby, "N", dcumentno,
                        cprocessing, cprocessed, "D2CE210969384179A54A964C7AA6CE0C",
                        "D2CE210969384179A54A964C7AA6CE0C", description, isdelivered, isinvoiced,
                        isprinted, isselected, salesrepid, dateordered, datepromised, dateprinted,
                        dateacct, bpID, billtoid, bplocationID, poreference, isdiscountprinted,
                        ccurrencyid, paymentrule, cpaymenttermid, invoicerule, deliveryrule,
                        freightcostrule, freightamt, deliveryviarule, mshipperid, cchargeid,
                        chargeamt, priorityrule, totallines, grandtotal, bWHouseID, mpricelistid,
                        istaxincluded, ccampaignid, cprojectid, cactivityid, posted, aduserid,
                        copyfrom, dropshipbpartnerid, dropshiplocationid, dropshipuserid,
                        isselfservice, adorgtrxid, user1id, user2id, deliverynotes, cincotermsid,
                        incotermsdescription, generatetemplate, deliverylocationid, copyfrompo,
                        finpaymentmethodid, finpaymentpriorityid, rmpickfromshipment,
                        rmreceivematerials, rmcreateinvoice, creturnreasonid, rmaddorphanline,
                        aassetid, calculatepromotions, ccostcenterid, convertquotation,
                        crejectreasonid, validuntil, quotationid, soresstatus, emdstotalitemqty,
                        emdstotalpriceadj, emdsposno, emdstime, emdsreceiptno,
                        emdsratesatisfaction, emdsgrandtotalamt, emdschargeamt, emrcmobileno,
                        emrcoxylaneno, emswhscode, emswmodelcode, emswmodelname, emswdeptid,
                        emswbrandid, emswexpdeldate, emswestshipdate, emswactshipdate,
                        emswpricelistver, emswcurrency, emswpostatus,
                        // emobdiscaddpack,
                        // emobposapplicationsid,
                        // emobpossendemail,
                        // emobposemailstatus,
                        emobwplgeneratepicking, emobwplisinpickinglist);
                    // END

                    // WarehouseId is retrieved from
                    // m_warehouse table for RFC C_order
                    // insert
                    mwarehouseid = ImportReqStock5Data.selectmwarehouseid(this);

                    // Siddesh Changes insert into c_order as RFC
                    cprocessed = "N";
                    headerid = ImportReqStock5Data.insertcorderheader(this, strRFCheaderID,
                        adclientid, adorgid, isactive, createdby, updatedby, "Y", dcumentno,
                        cprocessing, cprocessed, "B0745E66713C49199CE719BF5B88AF5C",
                        "B0745E66713C49199CE719BF5B88AF5C", description, isdelivered, isinvoiced,
                        isprinted, isselected, salesrepid, dateordered, datepromised, dateprinted,
                        dateacct, cbpartnerid, billtoid, cbpartnerlocationid, poreference,
                        isdiscountprinted, ccurrencyid, paymentrule, cpaymenttermid, invoicerule,
                        deliveryrule, freightcostrule, freightamt, deliveryviarule, mshipperid,
                        cchargeid, chargeamt, priorityrule, totallines, grandtotal, mwarehouseid,
                        mpricelistid, istaxincluded, ccampaignid, cprojectid, cactivityid, posted,
                        aduserid, copyfrom, dropshipbpartnerid, dropshiplocationid, dropshipuserid,
                        isselfservice, adorgtrxid, user1id, user2id, deliverynotes, cincotermsid,
                        incotermsdescription, generatetemplate, deliverylocationid, copyfrompo,
                        finpaymentmethodid, finpaymentpriorityid, rmpickfromshipment,
                        rmreceivematerials, rmcreateinvoice, creturnreasonid, rmaddorphanline,
                        aassetid, calculatepromotions, ccostcenterid, convertquotation,
                        crejectreasonid, validuntil, quotationid, soresstatus, emdstotalitemqty,
                        emdstotalpriceadj, emdsposno, emdstime, emdsreceiptno,
                        emdsratesatisfaction, emdsgrandtotalamt, emdschargeamt, emrcmobileno,
                        emrcoxylaneno, emswhscode, emswmodelcode, emswmodelname, emswdeptid,
                        emswbrandid, emswexpdeldate, emswestshipdate, emswactshipdate,
                        emswpricelistver, emswcurrency, emswpostatus,
                        // emobdiscaddpack,
                        // emobposapplicationsid,
                        // emobpossendemail,
                        // emobposemailstatus,
                        emobwplgeneratepicking, emobwplisinpickinglist);
                    // END

                    // System.out.println(" In header  : " +
                    // strShuttelheaderID
                    // +" document number is  :  " +
                    // documentno);
                    if (headerid > 0) {

                      // shuline=ImportReqStockData.insertshuttelline(this,
                      // client,organistation, user,
                      // user,strShuttelheaderID,
                      // mproductid, strBrandID,
                      // strModelname, strSize, strColor,
                      // (Integer.toString(movqty)),
                      // Integer.toString(lineno));
                      // Dyuti Changes
                      /*
                       * shuline = ImportReqStockData.insertshuttelline(this, client,
                       * "603C6A266B4C40BCAD87C5C43DDF53EE", user, user, strShuttelheaderID,
                       * mproductid, strBrandID, strModelname, strSize, strColor,
                       * (Integer.toString(movqty)), Integer.toString(lineno));
                       */
                      // END
                      System.out.println("About to Insert into COrder Line Table");
                      // corderline
                      // Siddesh Changes
                      // for RTV Flow
                      cbpartnerid = "";
                      cbpartnerlocationid = "";
                      datepromised = "";
                      soresstatus = "";
                      managereservation = "N";
                      manageprereservation = "N";
                      emswconfirmedqty = "0";
                      corderline = ImportReqStock5Data.insertcorderline(this, adclientid, adorgid,
                          isactive, createdby, updatedby, strRTVheaderID, Integer.toString(lineno),
                          cbpartnerid, cbpartnerlocationid, dateordered, datepromised,
                          datedelivered, dateinvoiced, description, mproductid, mwarehouseid,
                          directship, cuomid, Float.toString(qtyrecvd), qtyreserved, qtydelivered,
                          qtyinvoiced, mshipperid, ccurrencyid, pricelist, priceactual, pricelimit,
                          linenetamt, discount, freightamt, cchargeid, chargeamt, ctaxid,
                          sresourceassignmentid, reforderlineid, mattributesetinstanceid,
                          isdescription, quantityorder, mproductuomid, mofferid, pricestd,
                          cancelpricead, corderdiscountid, iseditlinenetamt, taxbaseamt,
                          minoutlineid, creturnreasonid, String.valueOf(grossunitprice),
                          String.valueOf(grossAmount), grosspricelist, ccostcenterid,
                          grosspricestd, aassetid, mwarehouseruleid, user1id, quotationlineid,
                          user2id, createreservation, cprojectid, soresstatus, managereservation,
                          manageprereservation, explode, bomparentid, emdssalesexcl, emdstaxamount,
                          emdslotqty, emdslotprice, emdsboxqty, emdsboxprice, emdsunitqty,
                          emdscessionprice, emdsccunitprice, emdsmrpprice, emdslinenetamt,
                          emsworderqty, emswsuppliercode, emswvolpcb, emswntwtpcb, emswgrwtpcb,
                          emswnoofparcel, emswitemcode, emswmodelname, emswcolor, emswsize,
                          emswrecqty, emswconfirmedqty);

                      // for RFC Flow
                      corderline = ImportReqStock5Data.insertcorderline(this, adclientid, adorgid,
                          isactive, createdby, updatedby, strRFCheaderID, Integer.toString(lineno),
                          cbpartnerid, cbpartnerlocationid, dateordered, datepromised,
                          datedelivered, dateinvoiced, description, mproductid, mwarehouseid,
                          directship, cuomid, Float.toString(qtyrecvd), qtyreserved, qtydelivered,
                          qtyinvoiced, mshipperid, ccurrencyid, pricelist, priceactual, pricelimit,
                          linenetamt, discount, freightamt, cchargeid, chargeamt, ctaxid,
                          sresourceassignmentid, reforderlineid, mattributesetinstanceid,
                          isdescription, quantityorder, mproductuomid, mofferid, pricestd,
                          cancelpricead, corderdiscountid, iseditlinenetamt, taxbaseamt,
                          minoutlineid, creturnreasonid, String.valueOf(grossunitprice),
                          String.valueOf(grossAmount), grosspricelist, ccostcenterid,
                          grosspricestd, aassetid, mwarehouseruleid, user1id, quotationlineid,
                          user2id, createreservation, cprojectid, soresstatus, managereservation,
                          manageprereservation, explode, bomparentid, emdssalesexcl, emdstaxamount,
                          emdslotqty, emdslotprice, emdsboxqty, emdsboxprice, emdsunitqty,
                          emdscessionprice, emdsccunitprice, emdsmrpprice, emdslinenetamt,
                          emsworderqty, emswsuppliercode, emswvolpcb, emswntwtpcb, emswgrwtpcb,
                          emswnoofparcel, emswitemcode, emswmodelname, emswcolor, emswsize,
                          emswrecqty, emswconfirmedqty);
                      // END

                      String storelocatorId = ImportReqStockData
                          .getStoreLocatorId(this, strwhorgid);
                      System.out.println("Store Locator Id is " + storelocatorId);
                      System.out.println("Input Parameter values are " + strlocatorefrom
                          + "product id " + mproductid);
                      System.out.println("minoutline id is " + minoutlineid);
                      System.out.println("strmLocatorID is " + strmLocatorID);
                      System.out.println("mattributesetinstanceid ------------ "
                          + mattributesetinstanceid);
                      mt1 = ImportReqStockData.insertMtransection(this, client, organistation,
                          user, user, "Shuttle Correction", storelocatorId,
                          // strlocatorefrom,
                          mproductid, "" + qtyrecvd, minoutlineid, strmLocatorID, "ITC",
                          documentno, mattributesetinstanceid);

                      // adding stock to return bin in case received less stock -changes by Hemant
                      String locatorId = ImportReqStockData.getLocatorId(this);
                      mt2 = ImportReqStockData.insertMtransection(this, client,
                          "603C6A266B4C40BCAD87C5C43DDF53EE", user, user, "Shuttle Correction",
                          locatorId,
                          // strlocatorefrom,
                          mproductid, "" + Math.abs(qtyrecvd), minoutlineid, strmLocatorID, "ITC",
                          documentno, mattributesetinstanceid);

                      // UPDATE RECQTY IN STORE REQUISITION
                      ImportReqStockData.updateCorderlineRecQty(this, "" + qtyrecvd, user,
                          documentno, mproductid);
                      System.out
                          .println("Successfully inserted into m_transaction Table ITC correction");

                      System.out.println("Successfully inserted into COrder Line Table 1");
                      V_headerflag = 1;
                    }
                  } else {

                    System.out.println("else if Siddesh Changes 2");
                    // create line only
                    // shuline=ImportReqStockData.insertshuttelline(this, client,organistation,
                    // user, user,strShuttelheaderID, mproductid, strBrandID, strModelname, strSize,
                    // strColor, (Integer.toString(movqty)), Integer.toString(lineno));
                    if (qtyrecvd < 0.0) {

                      // Dyuti Changes
                      // shuline=ImportReqStockData.insertshuttelline(this,
                      // client,"603C6A266B4C40BCAD87C5C43DDF53EE",
                      // user, user,strShuttelheaderID, mproductid, strBrandID, strModelname,
                      // strSize,
                      // strColor, (Integer.toString(movqty)), Integer.toString(lineno));
                      // END

                      // Siddesh Changes
                      cbpartnerid = "";
                      cbpartnerlocationid = "";
                      datepromised = "";
                      soresstatus = "";
                      managereservation = "N";
                      manageprereservation = "N";
                      emswconfirmedqty = "0";
                      corderline = ImportReqStock5Data.insertcorderline(this, adclientid, adorgid,
                          isactive, createdby, updatedby, strRTVheaderID, Integer.toString(lineno),
                          cbpartnerid, cbpartnerlocationid, dateordered, datepromised,
                          datedelivered, dateinvoiced, description, mproductid, mwarehouseid,
                          directship, cuomid, Float.toString(qtyrecvd), qtyreserved, qtydelivered,
                          qtyinvoiced, mshipperid, ccurrencyid, pricelist, priceactual, pricelimit,
                          linenetamt, discount, freightamt, cchargeid, chargeamt, ctaxid,
                          sresourceassignmentid, reforderlineid, mattributesetinstanceid,
                          isdescription, quantityorder, mproductuomid, mofferid, pricestd,
                          cancelpricead, corderdiscountid, iseditlinenetamt, taxbaseamt,
                          minoutlineid, creturnreasonid, String.valueOf(grossunitprice),
                          String.valueOf(grossAmount), grosspricelist, ccostcenterid,
                          grosspricestd, aassetid, mwarehouseruleid, user1id, quotationlineid,
                          user2id, createreservation, cprojectid, soresstatus, managereservation,
                          manageprereservation, explode, bomparentid, emdssalesexcl, emdstaxamount,
                          emdslotqty, emdslotprice, emdsboxqty, emdsboxprice, emdsunitqty,
                          emdscessionprice, emdsccunitprice, emdsmrpprice, emdslinenetamt,
                          emsworderqty, emswsuppliercode, emswvolpcb, emswntwtpcb, emswgrwtpcb,
                          emswnoofparcel, emswitemcode, emswmodelname, emswcolor, emswsize,
                          emswrecqty, emswconfirmedqty);

                      // for RFC Flow
                      corderline = ImportReqStock5Data.insertcorderline(this, adclientid, adorgid,
                          isactive, createdby, updatedby, strRFCheaderID, Integer.toString(lineno),
                          cbpartnerid, cbpartnerlocationid, dateordered, datepromised,
                          datedelivered, dateinvoiced, description, mproductid, mwarehouseid,
                          directship, cuomid, Float.toString(qtyrecvd), qtyreserved, qtydelivered,
                          qtyinvoiced, mshipperid, ccurrencyid, pricelist, priceactual, pricelimit,
                          linenetamt, discount, freightamt, cchargeid, chargeamt, ctaxid,
                          sresourceassignmentid, reforderlineid, mattributesetinstanceid,
                          isdescription, quantityorder, mproductuomid, mofferid, pricestd,
                          cancelpricead, corderdiscountid, iseditlinenetamt, taxbaseamt,
                          minoutlineid, creturnreasonid, String.valueOf(grossunitprice),
                          String.valueOf(grossAmount), grosspricelist, ccostcenterid,
                          grosspricestd, aassetid, mwarehouseruleid, user1id, quotationlineid,
                          user2id, createreservation, cprojectid, soresstatus, managereservation,
                          manageprereservation, explode, bomparentid, emdssalesexcl, emdstaxamount,
                          emdslotqty, emdslotprice, emdsboxqty, emdsboxprice, emdsunitqty,
                          emdscessionprice, emdsccunitprice, emdsmrpprice, emdslinenetamt,
                          emsworderqty, emswsuppliercode, emswvolpcb, emswntwtpcb, emswgrwtpcb,
                          emswnoofparcel, emswitemcode, emswmodelname, emswcolor, emswsize,
                          emswrecqty, emswconfirmedqty);
                      // END

                      String storelocatorId = ImportReqStockData
                          .getStoreLocatorId(this, strwhorgid);
                      System.out.println("Store Locator Id is " + storelocatorId);
                      System.out.println("Input Parameter values are " + strlocatorefrom
                          + "product id " + mproductid);
                      System.out.println("minoutline id is " + minoutlineid);
                      System.out.println("strmLocatorID is " + strmLocatorID);
                      System.out.println("mattributesetinstanceid ------------ "
                          + mattributesetinstanceid);
                      mt1 = ImportReqStockData.insertMtransection(this, client, organistation,
                          user, user, "Shuttle Correction", storelocatorId,
                          // strlocatorefrom,
                          mproductid, "" + qtyrecvd, minoutlineid, strmLocatorID, "ITC",
                          documentno, mattributesetinstanceid);
                      // UPDATE RECQTY IN STORE REQUISITION
                      ImportReqStockData.updateCorderlineRecQty(this, "" + qtyrecvd, user,
                          documentno, mproductid);
                      System.out
                          .println("Successfully inserted into m_transaction Table ITC correction lines only");

                    }

                  }
                }
                // Code change by Venki for inserting
                // into COrder only if the qtyrecvd is
                // Negative
                else {// Code change by Venki for
                  // inserting into COrder only if
                  // the qtyrecvd is positive and
                  // to insert the record into
                  // m_transaction
                  // mt1=ImportReqStockData.insertMtransection(this,
                  // client,organistation, user, user,
                  // "M-", strlocatorefrom,
                  // mproductid,
                  // Integer.toString(movqty),
                  // strmLocatorID,
                  // "ITC",documentno,"0"
                  // );
                  String locatorId = ImportReqStockData.getLocatorId(this);
                  // String returnlocatorId = ImportReqStockData.getLocatorId(this);
                  System.out.println("Locator Id is " + locatorId);
                  System.out.println("Input Parameter values are " + strlocatorefrom
                      + "product id " + mproductid);
                  System.out.println("minoutline id is " + minoutlineid);
                  System.out.println("strmLocatorID is " + strmLocatorID);
                  System.out.println("mattributesetinstanceid ------------ "
                      + mattributesetinstanceid);
                  // adding stock to store
                  String storelocatorId = ImportReqStockData.getStoreLocatorId(this, strwhorgid);
                  mt1 = ImportReqStockData.insertMtransection(this, client, organistation, user,
                      user, "Shuttle Correction", storelocatorId,
                      // strlocatorefrom,
                      mproductid, "" + qtyrecvd, minoutlineid, strmLocatorID, "ITC", documentno,
                      mattributesetinstanceid);
                  // adding negative stock to return bin in warehouse
                  mt2 = ImportReqStockData.insertMtransection(this, client,
                      "603C6A266B4C40BCAD87C5C43DDF53EE", user, user, "Shuttle Correction",
                      locatorId,
                      // strlocatorefrom,
                      mproductid, "-" + qtyrecvd, minoutlineid, strmLocatorID, "ITC", documentno,
                      mattributesetinstanceid);
                  // UPDATE RECQTY IN STORE REQUISITION
                  ImportReqStockData.updateCorderlineRecQty(this, "" + qtyrecvd, user, documentno,
                      mproductid);

                  System.out.println("M transaction insert return value is " + mt1);
                }
              } else {
                docaction = "CL";
                processing = "N";
                processed = "Y";
                docstatus = "CO";
                requisitionstatus = "SH";
                ImportReqStock3Data locatorbox[] = ImportReqStock3Data.selectdocumentanditem(this,
                    mproductid, documentno);
                if (locatorbox.length > 0) {
                  for (int B = 0; B < locatorbox.length; B++) {
                    strmLocatorID = locatorbox[B].mLocatorId;
                    strBoxN0 = locatorbox[B].boxno;
                    System.out.println("If locator " + strmLocatorID);
                  }
                  attributese = ImportReqStock3Data.selectattributeid(this, strBoxN0);
                  if ((attributese == null) || (attributese.equals(""))) {
                    attributese = "0";
                  }
                } else {
                  // System.out.println("Else locator "+strmLocatorID);
                  strmLocatorID = "F0F02D9BAFD540B0896371A95F0B5A13";
                  strBoxN0 = "";
                  attributese = "0";
                }

                // System.out.println("before mtra qty "+movqty);
                // mt1=ImportReqStockData.insertMtransection(this, client, organistation, user,
                // user, "M-", strmLocatorID, mproductid, Integer.toString(movqty*-1),
                // strlocatorefrom , "ITC", documentno, attributese );

                /*
                 * if(mt1>0) { mt2=ImportReqStockData.insertMtransection(this, client,organistation,
                 * user, user, "M+", "F0F02D9BAFD540B0896371A95F0B5A13", mproductid,
                 * Integer.toString(movqty*-1), strlocatorefrom , documentno ); }
                 */

                // Dyuti Code
                /*
                 * if(mt1>0) { mt2=ImportReqStockData.insertMtransection(this, client,
                 * organistation, user, user, "M+", "7962790FDD8843EF9B2E4CFB8364C12A", mproductid,
                 * (Integer.toString(movqty)), strlocatorefrom , "SIN", documentno, attributese); }
                 */
                // End Dyuti Code

                qtyship = Integer.parseInt(Strqtyintemp);
                qtyrec = 0;
              }
              upline = ImportReqStockData.updateline(this, Integer.toString(qtyship),
                  Integer.toString(qtyrec), mproductid, swsreqid);

              if (upline > 0) {
                updraft = ImportReqStockData.updatesrequisitiontostatus(this, docaction,
                    processing, processed, docstatus, requisitionstatus, documentno);
              }

            }
            lineno = lineno + 10;
            V_headerflag = 0;
          }// End if(data.length > 0)
        }
      }
      ImportReqStockData.deletefromtemp(this, user); // delete record from temp table
      printPageClosePopUp(response, vars, strWindowPath);
    } // END Of Else OF If(SAVE)

    else
      pageErrorPopUp(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strProcessId, String strWindowId, String strTabId, String strDeleteOld)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: process ImportInventoryServlet");
    ActionButtonDefaultData[] data = null;
    String strHelp = "", strDescription = "";
    /*
     * if (vars.getLanguage().equals("en_US")) { // data = ActionButtonDefaultData.select(this,
     * strProcessId); System.out.println("Inside English " + data.length); } else data =
     * ActionButtonDefaultData.selectLanguage(this, vars.getLanguage(), strProcessId); /* if (data
     * != null && data.length != 0) { strDescription = data[0].description; strHelp = data[0].help;
     * System.out.println(" StrDescription " + strDescription); System.out.println(" Strhelp " +
     * strHelp); }
     */
    String[] discard = { "" };
    if (strHelp.equals(""))
      discard[0] = new String("helpDiscard");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/decathlonimport/ad_process/ImportReqStock").createXmlDocument();
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("question",
        Utility.messageBD(this, "StartProcess?", vars.getLanguage()));
    xmlDocument.setParameter("description", strDescription);
    xmlDocument.setParameter("help", strHelp);
    xmlDocument.setParameter("windowId", strWindowId);
    xmlDocument.setParameter("tabId", strTabId);
    xmlDocument.setParameter("deleteOld", strDeleteOld);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet ImportReqStockServlet";
  } // end of getServletInfo() method
}
