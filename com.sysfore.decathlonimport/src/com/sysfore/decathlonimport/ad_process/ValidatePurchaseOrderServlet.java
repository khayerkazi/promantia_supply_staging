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
import org.openbravo.erpCommon.ad_actionButton.ActionButtonDefaultData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ValidatePurchaseOrderServlet extends HttpSecureAppServlet {
private static final long serialVersionUID = 1L;
int errmsg=0;
public void init(ServletConfig config) {
super.init(config);
boolHist = false;
}

public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
ServletException {
VariablesSecureApp vars = new VariablesSecureApp(request);
String process = "ValidatePurchaseOrder";// ImportData.processId(this, "ImportProduct");
//  System.out.println(process);
String message = "";
if (vars.commandIn("DEFAULT")) {
// System.out.println("Inside the Default Product");
String strTabId = vars.getGlobalVariable("inpTabId", "ValidatePurchaseOrderServlet|tabId");
String strWindowId = vars.getGlobalVariable("inpwindowId",
        "ValidatePurchaseOrderServlet|windowId");
String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
printPage(response, vars, process, strWindowId, strTabId, strDeleteOld);
} else if (vars.commandIn("SAVE")) {
// System.out.println("Inside the Purchase Order Save");
String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
String strTabId = vars.getRequestGlobalVariable("inpTabId",
        "ValidatePurchaseOrderServlet|tabId");

String strWindowPath = Utility.getTabURL(this, strTabId, "R");

if (strWindowPath.equals(""))

strWindowPath = strDefaultServlet;

OBError myError = new OBError();
String result = "";
String client = vars.getClient();
String organistation = vars.getOrg();
String user = vars.getUser();
int recheck = 0;
String Validateresult=null;

Validateresult=ValidatePurchaseOrderData.validatePO(this,user);
	
	if (Validateresult.equals("t")) {
	myError.setType("Success");
	myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
	myError.setMessage(Utility.messageBD(this, "Validation Successful", vars.getLanguage()));
	} else {
		myError.setType("Error");
		myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
		myError.setMessage(Utility.messageBD(this, "Validation Failure ! " + message, vars
	        .getLanguage()));




/*

ValidatePurchaseOrderData[] VPO = ValidatePurchaseOrderData.select(this, client, user);

if (VPO.length > 0) {

for (int i = 0; i < VPO.length; i++) {
        String itemcode = VPO[i].itemcode;
        String documentno = VPO[i].documentno;
        String quantity = VPO[i].quantity;
        String warehouse=VPO[i].warehouse;
        String message1="";
        if (itemcode != null) {
        try {
        int item_code = Integer.parseInt(itemcode);
        recheck += 0;
        String itche=ValidatePurchaseOrderData.itcheck(this,itemcode);
        int itnum=Integer.parseInt(itche);
        // needs to check model code of item
        
        if(itnum>0)
        {
        recheck += 0;
			String productprice=ValidatePurchaseOrderData.productprice(this,itemcode);
			int IsPrice=Integer.parseInt(productprice);
        if(IsPrice>0)
				{
				recheck += 0;
				}
				else{
				recheck += 1;
				message1=message1+" Product Price not defined , ";
				}
			String bpid="", locationid="";
					
			ValidatePurchaseOrder1Data[] SS = ValidatePurchaseOrder1Data.selectsupplierdetail(this, itemcode);
			 if (SS.length > 0) {
							recheck += 0;
							 for (int dss = 0; dss < SS.length; dss++) {
								bpid = SS[dss].bpid;
								locationid = SS[dss].locid;
								if((bpid==null)|| (bpid.equals("")))
									{
									recheck += 1;
									message1=message1+" Supplier name not defined, ";	
									}
								else{
									recheck += 0;
									}
							  if((locationid==null) || (locationid.equals("")))
									{
									recheck += 1;
									message1=message1+" Supplier location not defined, ";	
									}
								else{
									recheck += 0;
									}
								}

				
							}
				else{
				recheck += 1;
				 message1=message1+" Supplier name or location not defined, ";	
						}			

				
        }else{
        recheck += 1;
        //  message = message + " Item code does not exits in ERP, ";
        message1=message1+" Item code does not exits in ERP, ";
        }
        } catch (Exception e) {
        recheck += 1;
        // message = message + " Item code is not Valid, ";
        message1 = message1 + " Item code is not Valid, ";
        }
        } else {
        //message = message + " Item code is Missing, ";
        message1 = message1 + " Item code is Missing, ";
        recheck += 1;
        }
        if ((documentno == null) || (documentno.equals(""))) {

        //message = message + " Document number is inccorect, ";
        message1 = message1 + " Document number is inccorect, ";
        recheck += 1;
        } else {
	  recheck += 0;
	  int strlen=documentno.length();
	  String datevalue=documentno.substring(strlen-8, strlen);
	  try{
	  int x = Integer.parseInt(datevalue);
	  }
	  catch (Exception e) {
        // message = message + " Invalid CDD date in document number, ";
        message1 = message1 + "Invalid CDD date in document number, ";
        recheck += 1;
        }
	              
        }
        if((warehouse==null)||(warehouse.equals("")))
        {
        //message = message + " invaild warehouse name, ";
        message1 = message1 + " invaild warehouse name, ";
        recheck += 1;
        }else{
        
        String whche=ValidatePurchaseOrderData.whcheck(this,warehouse);

        int whnum=Integer.parseInt(whche);
        if(whnum>0)
        {
        recheck += 0; 
        }else{
        recheck += 1;
        // message = message + " invaild WareHouse, ";
        message1 = message1 + " Invaild WareHouse, ";
        }
        recheck += 0;
        }
        if (quantity != null) {
        try {
        int qty = Integer.parseInt(quantity);
        recheck += 0;
        } catch (Exception e) {
        // message = message + " Invalid Quanity, ";
        message1 = message1 + " Invalid Quanity Only Numeric Values Are Allowed, ";
        recheck += 1;
        }
        } else {
        //message = message + " Quanity can't be null, ";
        message1 = message1 + " Quanity can't be null, ";
        recheck += 1;
        }
	//  System.out.println( "message1 " + message1);
	 // System.out.println( "message " + message);
        errmsg=ValidatePurchaseOrderData.updateerrormessage(this,message1,VPO[i].imcoid);

}
*/
}
//}
  
vars.setSessionValue(strTabId + "|ImportPurchaseOrder3273451C213B4D1BB7266346CCD6E4D7.view",
        "");
vars.setMessage(strTabId, myError);
// System.out.println("Result is " + result);
printPageClosePopUp(response, vars, strWindowPath);
} else
pageErrorPopUp(response);

}

private void printPage(HttpServletResponse response, VariablesSecureApp vars,
String strProcessId, String strWindowId, String strTabId, String strDeleteOld)
throws IOException, ServletException {
if (log4j.isDebugEnabled())
log4j.debug("Output: process ValidatePurchaseOrderServlet");
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
"com/sysfore/decathlonimport/ad_process/ValidatePurchaseOrderServlet").createXmlDocument();
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
return "Servlet ValidatePurchaseOrderServlet";
} // end of getServletInfo() method
}
