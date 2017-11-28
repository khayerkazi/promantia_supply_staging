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

import java.util.*;
import java.text.*;
import java.util.Calendar;

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

public class ValidateReqStockServlet extends HttpSecureAppServlet {
	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) {
		super.init(config);
		boolHist = false;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		VariablesSecureApp vars = new VariablesSecureApp(request);

		String process = "ValidateReqStockServlet";// ImportData.processId(this,
													// "ImportProduct");
		if (vars.commandIn("DEFAULT")) {
			//System.out.println("Inside the Default Inventory");
			String strTabId = vars.getGlobalVariable("inpTabId",
					"ValidateReqStockServlet|tabId");
			String strWindowId = vars.getGlobalVariable("inpwindowId",
					"ValidateInventoryServlet|windowId");
			String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
			printPage(response, vars, process, strWindowId, strTabId,
					strDeleteOld);
		} else if (vars.commandIn("SAVE")) {
			//System.out.println("Inside the Inventory Save");
			String strDeleteOld = vars.getStringParameter("inpDeleteOld", "N");
			String strTabId = vars.getRequestGlobalVariable("inpTabId",
					"ValidateInventoryServlet|tabId");

			String strWindowPath = Utility.getTabURL(this, strTabId, "R");
			if (strWindowPath.equals(""))
				strWindowPath = strDefaultServlet;
			//System.out.println("strWindowPath: " + strWindowPath);
			OBError myError = new OBError();

			// vars.setMessage(strTabId, myError);

			String client = vars.getClient();
			String organistation = vars.getOrg();
			String user = vars.getUser();
			String lineid="", document = "", uploadeditemcode = "", documentno = "", itemcodecoutn = "", message = "", message1 = "", strisstore="", strreqst="", strrecivedate="";
			int docno = 0, countofitemcode = 0, countofitemcodeinmaster = 0, mastercheck = 0, errmsg=0;
			
			strisstore=ValidateReqStockData.isstore(this, organistation);
                       //System.out.println(strisstore  + "store ");
			if(strisstore.equals("Y"))
			{
				strreqst="CD";
			}
			else
			{
				strreqst="AD";
			}
			

			ValidateReqStockData data[] = ValidateReqStockData.selectDcno(this,
					user); 

	
			//String docStatusValue="C0";
                        //System.out.println(data.length + " thats the start " + user);
			if (data.length > 0) { // if there are documents
				System.out.println("Status Completed for Goods Receipt"+document);
				for (int i = 0; i < data.length; i++) {
					document = data[i].documentno;
					System.out.println("Document no is "+document);
					String docStatusValue = ValidateReqStockData.docNoStatus(this,document);
					if(docStatusValue.equalsIgnoreCase("CO")){		
					//lineid=data[i].imStockreplenishmentId;
					docno = Integer.parseInt(ValidateReqStockData.documentnocheck(this,document)); 
					// check is dc number valid
					//System.out.println(docno + " number of document " + strreqst + " " + document);
					if (docno > 0) // dc number is valid
					{
						
						mastercheck += 0;
						message1="";
						ValidateReqStock1Data itemdata[] = ValidateReqStock1Data.selectitemcode(this, document);
						 // getting uploaded item codes
                                                 //System.out.println(itemdata.length + " itemdata length " );
						if (itemdata.length > 0) { // there are item codes for  document no
						
							if(strisstore.equals("Y"))
							{
								System.out.println( " IN Y loop ");
								strrecivedate=ValidateReqStockData.reqdate(this,document);
								System.out.println( " strrecivedate " + strrecivedate);
								try{
									System.out.println("inside try");
									 DateFormat formatter ; 
									 Date date ;
									 Date d1=new Date();
									 formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
									 date = (Date)formatter.parse(strrecivedate); 
									 Calendar cal=Calendar.getInstance();
									 cal.setTime(date);
									 Calendar cal1=Calendar.getInstance();
									 cal1.setTime(d1); 
									 
									System.out.println("str date is " +date );
									System.out.println("now date is " +d1 );
									int diff=(int)( (d1.getTime() - date.getTime()) / (60 * 60 * 1000));
									System.out.println("diff is " +diff + " Document no "+ document);
									if(diff <=24)
									{
										//System.out.println("if ");	
									}	
									else
									{
										//System.out.println("else ");
										mastercheck += 1;
										message1=message1 +" Requistion time has pass ";		
									}	
								}catch(ParseException e)
								{
									System.out.println(e);
								}
							}	
										
							
							mastercheck += 0;
						//	message1="";
							for (int ic = 0; ic < itemdata.length; ic++) {
								uploadeditemcode = itemdata[ic].itemcode; // uploaded item code
								lineid= itemdata[ic].imStockreplenishmentId; // Line ID  
								countofitemcode = Integer.parseInt(ValidateReqStockData.itemcodecheckintemp(this,uploadeditemcode,document)); // checking is item code uploaded twice for same dc
								System.out.println(countofitemcode + " countofitemcode");	
								if (countofitemcode > 1)// count of item code is one in temp table
								{
									mastercheck += 1; 
									message1=message1 + "Duplicate Item Code ,";
									
								} else { // Only One Item Code
									mastercheck += 0;
									
//									System.out.println("uploadeditemcode       "+uploadeditemcode);
									countofitemcodeinmaster = Integer.parseInt(ValidateReqStockData.itemcodecheckinmaster( this, document,uploadeditemcode)); // check item code twice in requisition line
									if (countofitemcodeinmaster == 0) 
// item code not in master									
									{
									mastercheck += 1;	
							 	        message1=message1 + "Uploaded Item Code Doesn't Exits In Requisition,";
									} /*else if (countofitemcodeinmaster > 1) 
// item code has more than one item in master										
									{
										mastercheck += 1;	
										message1=message1 + " Uploaded Item Code Is Twice In Requisition ,";	

									}*/ else // item code is ok
									{
										mastercheck += 0;		
									}
								}
								errmsg=ValidateReqStockData.updateerrormessage(this,message1,lineid);
								message1="";
							}
						} else { // no item code for documentno
							mastercheck += 1;
							message1=message1 + " No Item Code For Document Number ,";
							errmsg=ValidateReqStockData.updateerrormessage(this,message1,lineid);
							message1="";
						}
						
					} else // dc number is not valid
					{	
						System.out.println("here two");
						mastercheck += 1;
						message1="Uploaded document number does not exits in requisition ,";
						
						errmsg=ValidateReqStockData.updateerrormessage1(this,message1,document,user);
						message1="";
					}	
				}else{ // goods receipt is not completed (CO) for this documentno 
			      			System.out.println("goods receipt is not in completed status");
						mastercheck += 1;
						message1=" Please complete the Goods Receipt. ";
						errmsg=ValidateReqStockData.updateerrormessage1(this,message1,document,user);
						message1="";
			     }
			 }			
			} else { // no document to validate
				
				System.out.println("here one");
				mastercheck += 1;
				message="No Document Number For Login User";
			}
			
			
			if(mastercheck==0)
			{
				 errmsg=ValidateReqStockData.updatevalidate(this,user);				
				 myError.setType("Success");
			     myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
			     myError.setMessage(Utility.messageBD(this, "Validation Successful", vars.getLanguage()));
				
			}
			else
			{
				myError.setType("Error");
		        myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
		        myError.setMessage(Utility.messageBD(this, "Validation Failure ! " + message, vars.getLanguage()));	
				
			}	
			/*
			 * if (result != null && result.equalsIgnoreCase("SUCCESS")) {
			 * myError.setType("Success");
			 * myError.setTitle(Utility.messageBD(this, "Success",
			 * vars.getLanguage())); myError.setMessage(Utility.messageBD(this,
			 * "Validation Successful", vars.getLanguage())); } else {
			 * myError.setType("Error");
			 * myError.setTitle(Utility.messageBD(this, "Error",
			 * vars.getLanguage())); myError.setMessage(Utility.messageBD(this,
			 * "Validation Failure ! Please Check Error Messages.",
			 * vars.getLanguage()));
			 * 
			 * }
			 */
			vars.setSessionValue(strTabId
					+ "|InventoryAE08E1109BC94E0CB7442911C19F82D4.view", "");
			vars.setMessage(strTabId, myError);
			// System.out.println("Result is " + result);
			printPageClosePopUp(response, vars, strWindowPath);
		} else
			pageErrorPopUp(response);
	}

	private void printPage(HttpServletResponse response,
			VariablesSecureApp vars, String strProcessId, String strWindowId,
			String strTabId, String strDeleteOld) throws IOException,
			ServletException {
		if (log4j.isDebugEnabled())
			log4j.debug("Output: process ValidateInventoryServlet");
		ActionButtonDefaultData[] data = null;
		String strHelp = "", strDescription = "";
	//	System.out.println(" code here  ");
		/*
		 * if (vars.getLanguage().equals("en_US")) { // data =
		 * ActionButtonDefaultData.select(this, strProcessId);
		 * System.out.println("Inside English " + data.length); } else data =
		 * ActionButtonDefaultData.selectLanguage(this, vars.getLanguage(),
		 * strProcessId); /* if (data != null && data.length != 0) {
		 * strDescription = data[0].description; strHelp = data[0].help;
		 * System.out.println(" StrDescription " + strDescription);
		 * System.out.println(" Strhelp " + strHelp); }
		 */
		String[] discard = { "" };
		if (strHelp.equals(""))
			discard[0] = new String("helpDiscard");
		XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
				"com/sysfore/decathlonimport/ad_process/ValidateReqStock")
				.createXmlDocument();
		xmlDocument.setParameter("language",
				"defaultLang=\"" + vars.getLanguage() + "\";");
		xmlDocument.setParameter("directory", "var baseDirectory = \""
				+ strReplaceWith + "/\";\n");
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
		return "Servlet ValidateReqStockServlet";
	} // end of getServletInfo() method
}
