/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2008-2009 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.sankalpcrm.ad_actionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class PreMail extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  // String status="";

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String fromEmail = vars.getStringParameter("inpFrom");
    String toEmail = vars.getStringParameter("inpTo");
    String ccEmail = vars.getStringParameter("inpCc");
    String bccEmail = vars.getStringParameter("inpBcc");
    String subject = vars.getStringParameter("inpSubject");
    String mobile = vars.getStringParameter("inpMobile");
    String name = vars.getStringParameter("inpName");
    String oxylane = vars.getStringParameter("inpOxylane");
    String status = vars.getStringParameter("inpStatus");
    String createdDate = vars.getStringParameter("inpCreated");
    String UpdatedDate = vars.getStringParameter("inpUpdate");
    String updatedStatus = vars.getStringParameter("inpUpdateStatus");
    String moduleId = vars.getStringParameter("inpcBpartnerId");
    String listOfSport = vars.getStringParameter("inpSports");
    String newsLetter = vars.getStringParameter("inpOpt");
    String mailBody = vars.getStringParameter("inpMailBody");    
   if (vars.commandIn("DEFAULT")) {
      printPage(response, vars, name);
    }
    if (vars.commandIn("EMAIL")) {
      printPage(response, vars, fromEmail, toEmail, ccEmail, bccEmail, subject, mobile, name,
          oxylane, status, createdDate, UpdatedDate, updatedStatus,moduleId,listOfSport,newsLetter,mailBody);

    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String name)
      throws IOException, ServletException {
    String moduleId = vars.getStringParameter("inpcBpartnerId");
    String sportsList = "";
    log4j.info("Mail Module Id " + moduleId);
    //System.out.println("BpartnerId:" + moduleId);
    log4j.info("BPartner: " + moduleId);
    StringBuffer listOfSports = new StringBuffer();
    
    PreMailData[] data = PreMailData.selectModule(this, moduleId);
    
   // int data1 = PreMailData.updateModule(this, moduleId);
   // System.out.println("moduleId got:"+moduleId);
    ResourceBundle rd = ResourceBundle.getBundle("com.sysfore.sankalpcrm.ad_actionButton.soap");
    String discard[] = { "", "" };
    String password = "";
    String newsLetter = "NO";
    String rEmail = "";
    try{
    	rEmail = PreMailData.selectRepEmail(this, data[0].companyid, data[0].rfirstname, data[0].rlastname);
    log4j.info("data.mobile " + data[0].mobile);
    log4j.info("text:" + vars.getStringParameter("inpMobile"));

    log4j.info("data[0].email " + data[0].email);
    log4j.info("text:" + vars.getStringParameter("inpTo"));
    /**  List of sports items       */
    
    
    if(data[0].emRcAikido.equalsIgnoreCase("Y"))
    	listOfSports.append(" Aikido,");
    if(data[0].emRcAlpinism.equalsIgnoreCase("Y"))
    	listOfSports.append(" Alpinism,");
    if(data[0].emRcArchery.equalsIgnoreCase("Y"))
    	listOfSports.append(" Archery,");
    if(data[0].emRcBadminton.equalsIgnoreCase("Y"))
    	listOfSports.append(" Badminton,");
    if(data[0].emRcBasketball.equalsIgnoreCase("Y"))
    	listOfSports.append(" Basketball,");
    if(data[0].emRcBoxing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Boxing,");
    if(data[0].emRcClimbing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Climbing,");
    if(data[0].emRcCricket.equalsIgnoreCase("Y"))
    	listOfSports.append(" Cricket,");
    if(data[0].emRcCycling.equalsIgnoreCase("Y"))
    	listOfSports.append(" Cycling,");
    if(data[0].emRcDance.equalsIgnoreCase("Y"))
    	listOfSports.append(" Dance,");
    if(data[0].emRcDiving.equalsIgnoreCase("Y"))
    	listOfSports.append(" Diving,");
    if(data[0].emRcFieldhockey.equalsIgnoreCase("Y"))
    	listOfSports.append(" Field Hockey,");
    if(data[0].emRcFishing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Fishing,");
    if(data[0].emRcFitness.equalsIgnoreCase("Y"))
    	listOfSports.append(" Fitness,");    
    if(data[0].emRcFootball.equalsIgnoreCase("Y"))
    	listOfSports.append(" Football,");
    if(data[0].emRcGolf.equalsIgnoreCase("Y"))
    	listOfSports.append(" Golf,");
    if(data[0].emRcGym.equalsIgnoreCase("Y"))
    	listOfSports.append(" Gym,");
    if(data[0].emRcHandball.equalsIgnoreCase("Y"))
    	listOfSports.append(" Handball,");
    if(data[0].emRcHiking.equalsIgnoreCase("Y"))
    	listOfSports.append(" Hiking,");
    if(data[0].emRcHorseriding.equalsIgnoreCase("Y"))
    	listOfSports.append(" Horse Riding,");
    if(data[0].emRcJudo.equalsIgnoreCase("Y"))
    	listOfSports.append(" Judo,");
    if(data[0].emRcKarate.equalsIgnoreCase("Y"))
    	listOfSports.append(" Karate,");
    if(data[0].emRcKitesurfing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Kite Surfing,");
    if(data[0].emRcPaddle.equalsIgnoreCase("Y"))
    	listOfSports.append(" Paddle,");
    if(data[0].emRcRollerskating.equalsIgnoreCase("Y"))
    	listOfSports.append(" Roller Skating,");
    if(data[0].emRcRugby.equalsIgnoreCase("Y"))
    	listOfSports.append(" Rugby,");
    if(data[0].emRcRunning.equalsIgnoreCase("Y"))
    	listOfSports.append(" Running,");
    if(data[0].emRcSailing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Sailing,");
    if(data[0].emRcSkiing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Skiing,");    
    if(data[0].emRcSnowboarding.equalsIgnoreCase("Y"))
    	listOfSports.append(" Snow Boarding,");
    if(data[0].emRcSquash.equalsIgnoreCase("Y"))
    	listOfSports.append(" Squash,");
    if(data[0].emRcSurfing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Surfing,");
    if(data[0].emRcSwimming.equalsIgnoreCase("Y"))
    	listOfSports.append(" Swimming,");
    if(data[0].emRcTabletennis.equalsIgnoreCase("Y"))
    	listOfSports.append(" Table Tennis,");
    if(data[0].emRcTennis.equalsIgnoreCase("Y"))
    	listOfSports.append(" Tennis,");
    if(data[0].emRcVolleyball.equalsIgnoreCase("Y"))
    	listOfSports.append(" VolleyBall,");
    if(data[0].emRcWindsurfing.equalsIgnoreCase("Y"))
    	listOfSports.append(" Wind Surfing,");
    if(data[0].emRcYoga.equalsIgnoreCase("Y"))
    	listOfSports.append(" Yoga,");
    }catch(Exception e){
    	//System.out.println("First Catch");
    	e.printStackTrace();
    }
    try{
	//System.out.println("2:"+listOfSports.indexOf(",", listOfSports.length()-2));
    if(listOfSports != null && listOfSports.length() >0){
    listOfSports.replace(listOfSports.length()-1, listOfSports.length(), ".");    
    sportsList = listOfSports.toString();
    }
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "com/sysfore/sankalpcrm/ad_actionButton/PreMail", discard).createXmlDocument();
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("help", PreMailData.getHelp(this, vars.getLanguage()));
    xmlDocument.setParameter("inpcBpartnerId", moduleId);
    if(data != null && data.length >0){ 
    xmlDocument.setParameter("mobile", data[0].mobile);
    xmlDocument.setParameter("email", data[0].email);
    xmlDocument.setParameter("inpName", data[0].firstname + " " + data[0].lastname);
    // if((data[0].oxylane) != null && (data[0].oxylane).length() != 13){
    xmlDocument.setParameter("inpOxylane", data[0].oxylane);
    xmlDocument.setParameter("inpStatus", data[0].status);
    xmlDocument.setParameter("inpCreated", data[0].created);
    xmlDocument.setParameter("inpUpdate", data[0].updated);
    if(data[0].opt.equalsIgnoreCase("Y"))
    	newsLetter = "YES";
    xmlDocument.setParameter("inpOpt", newsLetter);
    
    }
    xmlDocument.setParameter("inpSports", sportsList);
    // xmlDocument.setParameter("inpUpdateStatus", data[0].updatedstatus);
    //System.out.println("listOfSports.toString:"+listOfSports.toString());
    if (data[0].oxylane != null && data[0].oxylane.length() == 13)
    password = (data[0].oxylane).substring(7, 13);
    // }
    String mailBody = "";
    if(data != null && data.length >0){ 
    //System.out.println("date created:" + data[0].created + ":updated:" + data[0].updated);
    	log4j.info("date created: " + data[0].created);
    	log4j.info("date updated: " + data[0].updated);
    
    // System.out.println("updatedstatus:" + data[0].updatedstatus);
    if (data[0].status != null && data[0].status.equalsIgnoreCase("A")
        && data[0].emailstatus.equals("0")) {
      mailBody = SmsMailTemplate.sendMailCreateTemplate(data[0].firstname + " " + data[0].lastname,
    		  data[0].oxylane, data[0].email, password);
    } else if (data[0].status != null && data[0].status.equalsIgnoreCase("D")) {
      mailBody = SmsMailTemplate
          .sendMailDisableTemplate(data[0].firstname + " " + data[0].lastname);

    } else {
    	//String listOfSports = "";
      mailBody = SmsMailTemplate.sendMailUpdateTemplate(data[0].firstname + " " + data[0].lastname, data[0].oxylane, data[0].email,password,data[0].companyname,data[0].emRcLicenseno,data[0].rfirstname,data[0].rlastname, data[0].rdesignation, data[0].rmobile,rEmail, data[0].billinglocation,listOfSports.toString(),newsLetter);
    }
    }
    
    xmlDocument.setParameter("mailBody", mailBody);

    xmlDocument.setParameter("subject", rd.getString("mailsubject"));
    xmlDocument.setParameter("from", rd.getString("mailfrom"));
    
    {
      OBError myMessage = vars.getMessage("TemplateModule");
      vars.removeMessage("TemplateModule");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    PrintWriter out = response.getWriter();
    response.setContentType("text/html; charset=UTF-8");
    out.println(xmlDocument.print());
    out.close();
    }catch(Exception e){
    	//System.out.println("second catch");
    	e.printStackTrace();
    }
    
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String fromEmail,
      String toEmail, String ccEmail, String bccEmail, String subject, String mobile, String name,
      String oxylane, String status, String createdDate, String updatedDate, String updatedStatus,String moduleId,String listOfSports,String news,String mailBody)
      throws IOException, ServletException {
    //System.out.println("In email");
	  log4j.info("In Mail: " + "In mail");
    String password = "";
    ResourceBundle rd = ResourceBundle.getBundle("com.sysfore.sankalpcrm.ad_actionButton.soap");
    PreMailData[] data = PreMailData.selectModule(this, moduleId);
    // Sending SMS
    if (mobile != null && oxylane != null && data[0].emailstatus.equals("0") && status.equalsIgnoreCase("A")) {
      if (oxylane != null && oxylane.length() == 13)
        password = oxylane.substring(7, 13);
      // System.out.println("url:"+rd.getString("soapurl"));
      // System.out.println("mailhost:"+rd.getString("mailhost"));
      String SOAPUrl = rd.getString("soapurl");
      // String xmlFile2Send = "/WEB-INF/soap.properties";
      String SOAPAction = "";
      SOAPAction = rd.getString("soapaction");

      // Create the connection where we're going to send the file.
      URL url = new URL(SOAPUrl);
      URLConnection connection = url.openConnection();
      HttpURLConnection httpConn = (HttpURLConnection) connection;

      String sms = SmsMailTemplate.sendSMSTemplate(mobile, oxylane, password);
      byte[] b = sms.getBytes();
      httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
      httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
      httpConn.setRequestProperty("SOAPAction", SOAPAction);
      httpConn.setRequestMethod("POST");
      httpConn.setDoOutput(true);
      httpConn.setDoInput(true);

      // Everything's set up; send the XML that was read in to b.
      OutputStream out = httpConn.getOutputStream();
      out.write(b);
      out.close();

      // Read the response and write it to standard out.

      InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
      BufferedReader in = new BufferedReader(isr);

      String inputLine;

      while ((inputLine = in.readLine()) != null)
    	  log4j.info("Sucess: " + inputLine);
        //System.out.println(inputLine);
      in.close();     
      //log4j.info("Sucess: " + "Sucess");

    }
    if (toEmail != null) {
    	
      if (oxylane != null && oxylane.length() == 13)
        password = oxylane.substring(7, 13);      
      MailManager mailManager = new MailManager();
      boolean result = false; 
      int data1 = 0;
      if (status != null && status.equalsIgnoreCase("A") && data[0].emailstatus.equals("0")) {
        result = mailManager.sendMessage(fromEmail, toEmail, ccEmail, bccEmail, rd
            .getString("smtphost"), rd.getString("mailsubject"), mailBody);
        if (result)
        	data1 = PreMailData.updateModule(this, moduleId); 
      } else if (status != null && status.equalsIgnoreCase("D")) {
        result = mailManager.sendMessage(fromEmail, toEmail, ccEmail, bccEmail, rd
            .getString("smtphost"), rd.getString("mailsubject"), mailBody);
      } else {
    	  //String listOfSports="";
        result = mailManager.sendMessage(fromEmail, toEmail, ccEmail, bccEmail, rd
            .getString("smtphost"), rd.getString("mailsubject"), mailBody);
      }
      OBError myError = new OBError();
      if (result) {    	  
        myError.setType("Success");
        myError.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
        myError.setMessage(Utility.messageBD(this, "Mail sent sucessfully.", vars.getLanguage()));
      } else {
        myError.setType("Error");
        myError.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
        myError.setMessage(Utility.messageBD(this, "Mail sent failure.Please check Mail Id.", vars
            .getLanguage()));
      }
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "PreMail|paramTabId");
      strTabId = "ABD35AC7E47743F591FA36F957C52195";
      //System.out.println("strTabId:" + strTabId);
      String strTab = vars.getStringParameter("inpTabId");
      //System.out.println("strTab::" + strTab);
      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      //System.out.println("strWindowPath:" + strWindowPath);
      vars.setSessionValue(strTabId + "|MemberABD35AC7E47743F591FA36F957C52195.view", "EDIT");
      vars.setMessage(strTabId, myError);
      //System.out.println("Result is " + result);
      log4j.info("Result is: " + result);
      printPageClosePopUp(response, vars, strWindowPath);
    }

  }

  public static void copy(InputStream in, OutputStream out) throws IOException {

    // do not allow other threads to read from the
    // input or write to the output while copying is
    // taking place

    synchronized (in) {
      synchronized (out) {
        byte[] buffer = new byte[256];
        while (true) {
          int bytesRead = in.read(buffer);
          if (bytesRead == -1)
            break;
          out.write(buffer, 0, bytesRead);
        }
      }

    }
  }
}
