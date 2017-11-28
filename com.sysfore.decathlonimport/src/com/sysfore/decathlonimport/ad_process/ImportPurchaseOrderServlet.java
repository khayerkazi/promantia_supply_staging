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
import java.sql.Connection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonDefaultData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.database.ConnectionProvider;

public class ImportPurchaseOrderServlet extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private ConnectionProvider connection;
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String process = "ImportPurchaseOrder";// ImportData.processId(this, "ImportProduct");
    //System.out.println(process);
    String message = "";
    //Connection connection;
    if (vars.commandIn("DEFAULT")) {
    //  System.out.println("Inside the Default Purchase Order");
      String strTabId = vars.getGlobalVariable("inpTabId", "ImportPurchaseOrderServlet|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId",
          "ImportPurchaseOrderServlet|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
    } else if (vars.commandIn("SAVE")) {
  //    System.out.println("Inside the Purchase Order Save");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
      String strTabId = vars.getRequestGlobalVariable("inpTabId",
          "ImportPurchaseOrderServlet|tabId");

      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;
     // ConnectionProvider conn=var.getConnection();
      OBError myError = new OBError();
      String result = "";
      String client = vars.getClient();
      String organistation = vars.getOrg();
      String user = vars.getUser();
      String coid = "", Itemcode = "", itemerpid = "", warehouse = "", warehouseid = "", documentnu = "", modelcode = "", modelname = "", modelcom = "", bpid = "", bplocid = "", deptid = "", brandid = "", suppcode = "", taxid = "", mcolor = "", msize = "", sswid="";
      int totallines = 0, updateline=0;
      Double qty = 0.0, pricelist = 0.0, mrp = 0.0, cashncarry = 0.0, lineamt=0.0, pricel = 0.0, prices = 0.0, pricelt = 0.0, taxrate = 0.0, cessionprice = 0.0, volpcb = 0.0, netw = 0.0, gwt = 0.0, pcb = 0.0, packlist = 0.0, taxamt=0.0, gtotal=0.0;
      String Importresult=null;
      
      Importresult=ImportPurchaseOrder1Data.importPO(this,user);
      
      System.out.println("Resutl from Import "+Importresult);
          if (Importresult.equals("t")) {
            //int tempdelete=ImportPurchaseOrder1Data.deleterecord(this,client,user);  
            myError.setType("Success");
            myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
            myError.setMessage(Utility.messageBD(this, "Processes Successful", vars.getLanguage()));
          } else {
            myError.setType("Error");
            myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            myError.setMessage(Utility.messageBD(this, "Process Failure ! " + message, vars
                .getLanguage()));

          }
      /*
      ImportPurchaseOrder1Data[] IPO = ImportPurchaseOrder1Data.select(this, client, user);

      if (IPO.length > 0) {
        for (int t = 0; t < IPO.length; t++) {

         /// documentnu = IPO[t].doc;
          warehouse = IPO[t].warehouse;
          warehouseid = ImportPurchaseOrder1Data.selectwarehouse(this, warehouse);
		  sswid="603C6A266B4C40BCAD87C5C43DDF53EE";
           //System.out.println("warehouse  --> " + warehouse + "   warehouseid --->> " + warehouseid);
		
          ImportPurchaseOrder2Data[] MCO = ImportPurchaseOrder2Data.selectmodelcode(this, user);
          if (MCO.length > 0) {
            for (int d = 0; d < MCO.length; d++) {
              int coh = 0, cl = 0;
              modelcode = MCO[d].mocode;
              coid = SequenceIdData.getUUID();
              ImportPurchaseOrder3Data[] MD = ImportPurchaseOrder3Data.modeldetail(this, modelcode);
              if (MD.length > 0) {
                for (int r = 0; r < MD.length; r++) {

                  modelname = MD[r].name;
                  bpid = MD[r].bpid;
                  bplocid = MD[r].locid;
                  deptid = MD[r].deptid;
                  brandid = MD[r].brandid;
                  suppcode = MD[r].supno;

                  //    
                }

              }
              //System.out.println(" code is here  " + "   ---   " + organistation);
              coh = ImportPurchaseOrderHeaderData.insertcorder(this, coid, client, sswid,
                  user, user, documentnu, bpid, bplocid, bplocid, warehouseid, modelcode,
                  modelname, deptid, brandid);
             // coh=1;
              //System.out.println("code inserted heer  " + coh);
              if (coh > 0) {
                ImportPurchaseOrderLineData[] linede = ImportPurchaseOrderLineData.selectitem(this,modelcode, user);
                if (linede.length > 0) {
                  int sline = 0;
                  String lno="";
                  for (int y = 0; y < linede.length; y++) {
                    sline = sline + 10;
                    lno=Integer.toString(sline);
                    qty = Double.parseDouble(linede[y].quantity);
                    //String qtty=qty.toString();
                    ImportPurchaseOrderLine1Data[] lldet = ImportPurchaseOrderLine1Data.itemdetail(
                        this, linede[y].itemcode);
					
                    if (lldet.length > 0) {
                      for (int Z = 0; Z < lldet.length; Z++) {
                        itemerpid = lldet[Z].productid;
                        pricelist = Double.parseDouble(lldet[Z].pricelist);
                        mrp = Double.parseDouble(lldet[Z].mrp);
                        cashncarry = Double.parseDouble(lldet[Z].cashandcarry);
                        pricel = Double.parseDouble(lldet[Z].pricelimit);
                        taxid = lldet[Z].taxid;
                        prices = Double.parseDouble(lldet[Z].pricestd);
                        pricelt = Double.parseDouble(lldet[Z].pricelimit);
                        taxrate = Double.parseDouble(lldet[Z].taxrate);
                        cessionprice = Double.parseDouble(lldet[Z].cession);
                        volpcb = Double.parseDouble(lldet[Z].volumepcb);
                        netw = Double.parseDouble(lldet[Z].pcbnetweight);
                        gwt = Double.parseDouble(lldet[Z].pcbgrossweight);
                        pcb = Double.parseDouble(lldet[Z].pcbqty);
                        Itemcode = linede[y].itemcode;
                        mcolor = lldet[Z].color;
                        msize = lldet[Z].size;
                        packlist = qty / pcb;
                        taxamt=(((mrp) / 100 * (taxrate))* qty);
                        lineamt=qty * mrp;
                        gtotal=gtotal+lineamt;
                        totallines=totallines+Z;
                      //System.out.println("insert into c_orderline(c_orderline_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, c_order_id, line, c_bpartner_id,c_bpartner_location_id, dateordered, datepromised, dateinvoiced, m_product_id, m_warehouse_id, directship, c_uom_id, qtyordered, qtyreserved, qtydelivered, qtyinvoiced, c_currency_id, pricelist, priceactual, pricelimit, linenetamt, freightamt, chargeamt, c_tax_id, isdescription, pricestd, cancelpricead, iseditlinenetamt, taxbaseamt, em_ds_taxamount, em_ds_unitqty, em_ds_cessionprice, em_sw_orderqty, em_sw_suppliercode, em_sw_volpcb, em_sw_ntwtpcb, em_sw_grwtpcb, em_sw_noofparcel, em_sw_itemcode, em_sw_modelname, em_sw_color, em_sw_size, em_ds_ccunitprice, em_ds_mrpprice  ) VALUES" +
                      //"(get_uuid(), '"+client+"', '"+organistation+"', 'Y' ,now(), '"+user+"',now(), '"+user+"', '"+coid+"','"+lno+"', '"+bpid+"', '"+bplocid+"', '2011-08-16 00:00:00', '2011-08-20 00:00:00', '2011-08-16 00:00:00', '"+itemerpid+"', '"+warehouseid+"', 'N', '100', '"+qty+"', 0, 0, '"+qty+"', '304', '"+pricelist+"', '"+mrp+"', '"+pricelt+"', '"+lineamt+"', 0.0, 0.0, '"+taxid+"', 'N', '"+prices+"','N', 'N', 0, '"+taxamt+"', 0, '"+cessionprice+"', '"+qty+"', '"+suppcode+"', '"+volpcb+"'), '"+netw+"','"+gwt+"', '"+packlist+"', '"+Itemcode+"', '"+modelname+"', '"+mcolor+"', '"+msize+"', '"+cashncarry+"', '"+cashncarry+"')");  
                        
						//int strlen=documentnu.length();
						//String datevalue=documentnu.substring(strlen-8, strlen);
						//String newdatein=datevalue.substring(0,2)+"-"+datevalue.substring(2,4)+"-"+datevalue.substring(4,8)+" 00:00:00";
						
						
                      //cl = ImportPurchaseOrderLine1Data.insertlines(this,client, organistation, user, user, coid, lno, bpid, bplocid, newdatein,newdatein,newdatein, itemerpid, warehouseid, qty.toString(), pricelist.toString(), mrp.toString(), pricelt.toString(), lineamt.toString(), taxid, prices.toString(), taxamt.toString() , cessionprice.toString(), qty.toString(), suppcode, volpcb.toString(), netw.toString(), gwt.toString(), packlist.toString(), Itemcode, modelname, mcolor, msize, cashncarry.toString(), mrp.toString());
                       //System.out.println("Order Line is  " + cl);
                      
                      }

                    }
                  }
                } 
                  updateline=ImportPurchaseOrderLine1Data.updatecorder(this, Integer.toString(totallines), gtotal.toString(), coid);
                  /// update grand total and item lines
              }
            }

          }
        }
      }
        if (updateline > 0) {
        int tempdelete=ImportPurchaseOrder1Data.deleterecord(this,client,user);  
        myError.setType("Success");
        myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
        myError.setMessage(Utility.messageBD(this, "Processes Successful", vars.getLanguage()));
      } else {
        myError.setType("Error");
        myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
        myError.setMessage(Utility.messageBD(this, "Process Failure ! " + message, vars
            .getLanguage()));

      }*/
      //vars.setSessionValue(strTabId + "|ImportPurchaseOrder37CA7AFF0DAC430A9681BF97937908DB.view", ""); // for local system tab id
        vars.setSessionValue(strTabId + "|ImportPurchaseOrder2922D457FAE34D59A30893C4DEA8B06B.view", ""); // for production tab id
      vars.setMessage(strTabId, myError);
      printPageClosePopUp(response, vars, strWindowPath);
    }
    else
      pageErrorPopUp(response);

  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strProcessId, String strWindowId, String strTabId, String strDeleteOld)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: process ImportPurchaseOrderServlet");
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
        "com/sysfore/decathlonimport/ad_process/ImportPurchaseOrderServlet").createXmlDocument();
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("question", Utility.messageBD(this, "StartProcess?", vars
        .getLanguage()));
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
    return "Servlet ImportPurchaseOrderServlet";
  } // end of getServletInfo() method
}
