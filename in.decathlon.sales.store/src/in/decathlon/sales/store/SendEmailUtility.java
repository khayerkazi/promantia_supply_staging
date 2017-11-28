package in.decathlon.sales.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.sales.store.data.DcssPendingDocumentMail;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.utils.FormatUtilities;

public class SendEmailUtility {
  private static final Logger log4j = Logger.getLogger(SendEmailUtility.class);

  private class EmailStruct {
    String body;
    String subject;
  };

  JSONObject errorObject = new JSONObject();

  public void sendEmail(String orderId, String name, String emailId, String dmId,
      ProcessBundle bundle) throws ServletException, JSONException {

    boolean setFailed = false;
    Order order = OBDal.getInstance().get(Order.class, orderId);

    Client client = order.getClient();
    EmailStruct struct = getHtmlAsStringFromPOC(order, name, emailId);

    String host = null;
    boolean auth = true;
    String username = null;
    String password = null;
    String connSecurity = null;
    int port = 25;

    // new
    // GenerateReport().jasperReportCreate("/in/decathlon/sales/store/ad_reports/Receipt.jrxml","en",
    // "/home/chinmaya/src/Decathlon/app/3.0");
    List<EmailServerConfiguration> emailServersConfiguration = null;
    final String replyToName = "Decathlon Customer Support";
    String replyToEmail = null;
    String senderAddress = null;
    OBContext.setAdminMode(true);
    try {

      emailServersConfiguration = client.getEmailServerConfigurationList();

      final EmailServerConfiguration mailConfig = emailServersConfiguration.get(0);

      host = mailConfig.getSmtpServer();

      if (!mailConfig.isSMTPAuthentification()) {
        auth = false;
      }

      senderAddress = mailConfig.getSmtpServerSenderAddress();
      replyToEmail = mailConfig.getSmtpServerSenderAddress();
      username = mailConfig.getSmtpServerAccount();
      password = FormatUtilities.encryptDecrypt(mailConfig.getSmtpServerPassword(), false);
      connSecurity = mailConfig.getSmtpConnectionSecurity();
      port = mailConfig.getSmtpPort().intValue();
    } finally {
      OBContext.restorePreviousMode();
    }

    final String recipientTO = emailId;
    final String recipientCC = null;
    final String recipientBCC = null;
    final String replyTo = replyToEmail;
    final String contentType = "text/html; charset=UTF-8";

    if (log4j.isDebugEnabled()) {
      log4j.debug("From: " + senderAddress);
      log4j.debug("Recipient TO (contact email): " + recipientTO);
      log4j.debug("Recipient CC: " + recipientCC);
      log4j.debug("Recipient BCC (user email): " + recipientBCC);
      log4j.debug("Reply-to (sales rep email): " + replyTo);
    }

    if ((replyToEmail == null || replyToEmail.length() == 0)) {
      setFailed = true;
      // throw new ServletException(Utility.messageBD(this, 'NoSalesRepEmail', vars.getLanguage()));
      log4j.error("Cannot send email because there is no sales representative email address!");
    }

    if ((emailId == null || emailId.length() == 0)) {
      // throw new ServletException(Utility.messageBD(this, 'NoCustomerEmail', vars.getLanguage()));
      log4j.error("Cannot send email because there is no customer email address!");
      setFailed = true;
    }

    List<File> attachments = new ArrayList<File>();

    final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
    // File f = new File(url.getPath()+"/Receipt"+order.getDocumentNo());
    String docno = order.getDocumentNo();
    int pos = docno.indexOf("*", 1);
    String soDocNum = docno.substring(pos + 1, docno.length());
    // lt.setPartnerreference(soDocNum.split("\\*")[0]);
    String docNo = soDocNum.split("\\/")[1];
    OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("attach.path");
    File file = new File(OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path")
        + "/Receipt" + docNo + ".pdf");
    System.out.println("DOCNO" + order.getDocumentNo());
    attachments.add(file);

    try {
      OBContext.setAdminMode(false);
      EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
          recipientTO, recipientCC, recipientBCC, replyTo, struct.subject, struct.body,
          contentType, attachments, null, null);
      DcssPendingDocumentMail dm = OBDal.getInstance().get(DcssPendingDocumentMail.class, dmId);
      dm.setAlertStatus("Y");
      if (!file.isDirectory()) {
        file.delete();
      }
    } catch (Exception exception) {
      errorObject.put(SOConstants.RECORD_ID, dmId);
      errorObject.put(SOConstants.recordIdentifier, dmId);
      errorObject.put(SOConstants.TABLE_NAME, "");

      log4j.error(exception);
      final String exceptionClass = exception.getClass().toString().replace("class ", "");
      String exceptionString = "Problems while sending the email" + exception;
      exceptionString = exceptionString.replace(exceptionClass, "");
      BusinessEntityMapper.createErrorLogRecord(exception, bundle.getProcessId(), errorObject);

      // throw exception;
    } finally {
      OBContext.restorePreviousMode();

    }

  }

  private EmailStruct getHtmlAsStringFromPOC(Order order, String custName, String custEmail) {

    EmailStruct struct = new EmailStruct();

    struct.body = replaceTextWithData(getDefaultTemplate(), order, custName, custEmail);
    struct.subject = replaceTextWithData(
        "[Decathlon Sports India] - Your E-Bill dated @order_date@", order, custName, custEmail);
    return struct;
  }

  private String replaceTextWithData(String text, Order order, String custName, String custEmail) {
    String email;
    String custSatAltTxt = "Satisfaction";
    String bpId = order.getBusinessPartner().getId();
    String decId = order.getRCOxylaneno();
    IbudConfig config = new IbudConfig();

    String erpURL = config.getErpUrl();
    String erpUser = config.getErpuser();
    String erpPwd = config.geterppwd();

    String satisfaction = (order.getDSEMDsRatesatisfaction() == null) ? "2" : order
        .getDSEMDsRatesatisfaction();

    String satisfactionImageUrl = "http://ext-decathlon.in/decathlonimage/decathlon/satisfied.png";

    if (satisfaction.equals("3")) {
      satisfactionImageUrl = "http://ext-decathlon.in/decathlonimage/satisfied.png";
      custSatAltTxt = "'You rated your experience as VERY SATISFIED and we thank you for your positive feedback. Hope you return soon!!'";
    } else if (satisfaction.equals("2")) {
      satisfactionImageUrl = "http://ext-decathlon.in/decathlonimage/lesssat.png";
      custSatAltTxt = "'You rated your experience as SATISFIED and we thank you for your positive feedback. Hope you return soon!!'";

    } else if (satisfaction.equals("1")) {
      satisfactionImageUrl = "http://ext-decathlon.in/decathlonimage/unsat.png";
      custSatAltTxt = "'You rated your experience as UNSATISFIED. We at Decathlon care about all customers. You will receive a call within "
          + "24 hours to bring back a smile on your face.'";

    }

    HashMap<String, String> replacements = new HashMap<String, String>();
    replacements.put("customer_name", custName);
    replacements.put("store_name", order.getOrganization().getDescription());
    replacements.put("store_number", order.getOrganization().getDSTelephoneNo());
    if (order.getOrganization().getDsidefStoremanagermail() != null)

      // email = order.getOrganization().getOrganizationInformationList().get(0).getUserContact()
      // .getEmail();
      email = order.getOrganization().getDsidefStoremanagermail();
    else
      // email = "laure.suspene@decathlon.in";
      email = "";

    replacements.put("store_email", email);
    replacements.put("document_no", order.getDocumentNo());
    replacements.put("cust_satisfaction", satisfactionImageUrl);
    replacements.put("cust_alttext", custSatAltTxt);
    replacements.put("bpId", bpId);
    replacements.put("decId", decId);
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    replacements.put("erp_url", erpURL);
    replacements.put("erp_user", erpUser);
    replacements.put("erp_pwd", erpPwd);
    replacements.put("order_date",
        new SimpleDateFormat("dd-MMMM-yyyy").format(order.getOrderDate()));
    return parseTokens(text, replacements);
  }

  private String parseTokens(String text, HashMap<String, String> replacements) {
    Pattern pattern = Pattern.compile("@(.+?)@");
    Matcher matcher = pattern.matcher(text);
    StringBuilder builder = new StringBuilder();
    int i = 0;
    while (matcher.find()) {
      String replacement = replacements.get(matcher.group(1));
      builder.append(text.substring(i, matcher.start()));
      if (replacement == null)
        builder.append(matcher.group(0));
      else
        builder.append(replacement);
      i = matcher.end();
    }
    builder.append(text.substring(i, text.length()));
    return builder.toString();
  }

  private String getDefaultTemplate() {
    StringBuffer strBuffer = new StringBuffer();
    strBuffer
        .append("<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.01//EN' 'http://www.w3.org/TR/html4/strict.dtd'>");
    strBuffer
        .append("<html><head><title>Mail</title></head><body><div dir='ltr'><div><div><br><div class='gmail_quote'><br>");
    strBuffer
        .append("<div bgcolor='#FFFFFF' text='#000000'> <span><font color='#888888'> </font></span><span><font color='#888888'> </font></span><span><font color='#888888'> </font></span>");
    strBuffer
        .append("<table align='center' cellpadding='0' cellspacing='0' height='435' width='700'>");
    strBuffer
        .append("<tbody><tr><td bgcolor='#75767a' width='293'><table align='top' cellpadding='0' cellspacing='0'><tbody><tr>");
    strBuffer
        .append("<td align='center'><img style='width: 159px; height: 55px;' src='http://ext-decathlon.in/decathlonimage/decathlon_logo.png' alt='DECATHLON' title='DECATHLON'></td>");
    strBuffer
        .append("</tr><tr><td><br><h1 style='margin: 10px; color: rgb(255, 255, 255); font-size: 18px; font-weight: 400; font-family: Arial,Helvetica,sans-serif;'>Hello <strong>@customer_name@</strong>! </h1>");
    strBuffer
        .append("<h2 style='margin: 10px; color: rgb(255, 255, 255); font-size: 18px; font-family: Arial,Helvetica,sans-serif; font-weight: 400;'>Thank you for shopping at <br><strong>DECATHLON @store_name@.</strong></h2>");
    strBuffer
        .append("</td></tr><tr><td style='padding-left: 10px; color: rgb(255, 255, 255); font-family: Arial,Helvetica,sans-serif; font-size: 12px;'>");
    strBuffer
        .append("<p>Please find the attached soft copy of your bill.</p></td></tr><tr><td style='padding-top: 10px;'> <img style='width: 293px; height: 97px;' src='http://ext-decathlon.in/decathlonimage/play.png' title='Play' alt='Play'>");
    strBuffer
        .append("</td></tr><tr><td style='padding-left: 10px; color: rgb(255, 255, 255); line-height: 1.8em; font-family: Arial,Helvetica,sans-serif; font-size: 12px;'>");
    strBuffer
        .append("<p>If you have any queries or concerns,<br>please contact us at @store_number@<br>or email at @store_email@</p><br></td></tr><tr>");
    strBuffer
        .append("<td align='center'><img src='http://ext-decathlon.in/decathlonimage/oxylan.png' alt='oxylane' height='51' width='oxylane'> </td></tr></tbody></table></td>");
    strBuffer
        .append("<td width='407'> <span><font color='#888888'> </font></span><span><font color='#888888'> </font></span><span><font color='#888888'> </font></span>");
    strBuffer
        .append("<table style='border: 1px solid rgb(117, 118, 122); padding: 30px;' cellpadding='0' cellspacing='0' height='435' width='407'><tbody><tr cellpadding='0' cellspacing='0'>");

    strBuffer
        .append("<td cellpadding='0' cellspacing='0'><img src= @cust_satisfaction@ alt=@cust_alttext@ class=''></td></tr><tr>");

    strBuffer
        .append("<td style='padding: 10px; color: rgb(255, 255, 255); font-family: Arial,Helvetica,sans-serif;' align='center' bgcolor='#0075b9'><h2 style='font-weight: 400; font-size: 16px;'>CUSTOMER");
    strBuffer
        .append("HAPPINESS CENTER</h2><p>Open from <span class='aBn' data-term='goog_1982230579' tabindex='0'><span class='aQJ'>8:00 AM - 8:00 PM</span></span><br>");
    strBuffer
        .append("Tel : +91 76767 98989</p><p><a href='mailto: vijoy.nair@decathlon.in' style='color: rgb(255, 255, 255);' target='_blank'>vijoy.nair@decathlon.in</a></p></td></tr><tr><td style='font-family: Arial,Helvetica,sans-serif; font-size: 0.8em;'><br>");
    // strBuffer
    // .append("If you would like to update your information please follow the <a href='https://docs.google.com/a/decathlon.in/forms/d/1EtgON88Q-tiXw9fWS7b8o_iTe_6MUXeMJllPGpP8W58/viewform' style='color: rgb(3, 109, 194);' target='_blank'>link</a></td>");
    // strBuffer
    // .append("</tr><tr><td style='font-family: Arial,Helvetica,sans-serif; font-size: 0.8em;' align='center'><br><a href='@erp_url@?username=@erp_user@&pwd=@erp_pwd@&decathlonId=@decId@&em_email_alert=N' style='color: rgb(102, 102, 102);' target='_blank'>Unsubscribe</a>");
    strBuffer
        .append("<span class='HOEnZb'><font color='#888888'><span><font color='#888888'><span><font color='#888888'> </font></span></font></span></font></span></td></tr></tbody></table>");
    strBuffer
        .append("<span class='HOEnZb'><font color='#888888'><span><font color='#888888'><span><font color='#888888'> </font></span></font></span></font></span></td>");
    strBuffer
        .append("</tr></tbody></table><div class='yj6qo ajU'><div id=':1o1' class='ajR' role='button' tabindex='0' data-tooltip='Hide expanded content' aria-label='Hide expanded content'><img class='ajT' src='images/cleardot.gif'></div>");
    strBuffer
        .append("</div><span class='HOEnZb adL'><font color='#888888'><span><font color='#888888'><span><font color='#888888'> </font></span></font></span></font></span></div>");
    strBuffer
        .append("<span class='HOEnZb adL'><font color='#888888'><span><font color='#888888'><span></span></font></span></font></span></div><span class='HOEnZb adL'><font color='#888888'><span><font color='#888888'><span><font color='#888888'><br>");
    strBuffer.append("</font></span></font></span></font></span></div></div></div></body></html>");
    return strBuffer.toString();
  }

  public JSONObject getCustomerDetails(String wsName, String oxylaneId) throws Exception {
    JSONObject obj = new JSONObject();
    try {
      int maxRowCount = 1;
      int port = 0;

      final String userName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("crm.user");
      final String password = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("crm.password");
      final String url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("crm.url");

      // log4j.info("UserName=" + userName + " and Password=" + password);
      // log4j.info("Invoking Url is" + url);

      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, password.toCharArray());
        }
      });

      DefaultHttpClient httpclient = new DefaultHttpClient();

      httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(userName, password));

      HttpGet httpGet = new HttpGet(url);

      HttpResponse response = httpclient.execute(httpGet);

    } catch (Exception exc) {

      log4j.error(exc);
      BusinessEntityMapper.createErrorLogRecord(exc, null, errorObject);
    }

    return obj;

  }

}