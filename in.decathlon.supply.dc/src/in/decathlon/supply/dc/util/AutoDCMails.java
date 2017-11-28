package in.decathlon.supply.dc.util;

import in.decathlon.factorytostore.data.FacstWarehouseUsers;
import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

public class AutoDCMails implements Serializable, Cloneable {

  private static final Logger LOG = Logger.getLogger(AutoDCMails.class);
  private static final long serialVersionUID = 1L;
  private static volatile AutoDCMails instance;
  final Properties p = SuppyDCUtil.getInstance().getProperties();
  final Properties pForFTS = SuppyDCUtil.getInstance().getProperties(SOConstants.MODULE_NAME_FTS);

  private AutoDCMails() {
    // no-op
  }

  public static AutoDCMails getInstance() {
    if (instance == null) {
      synchronized (SuppyDCUtil.class) {
        if (instance == null) {
          instance = new AutoDCMails();
        }
      }
    }
    return instance;
  }

  public void simplySendWarehouseMail(Organization org, File filename, long qtyConfirmed) {
    LOG.info("send mail to warehouse for " + org.getName());

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);

    String storeName = org.getName();
    String emailTo = null;

    try {
      if (p.getProperty(org.getSearchKey() + "ToEmail") != null
          || p.getProperty(org.getSearchKey() + "ToEmail").equals("")) {
        emailTo = p.getProperty(org.getSearchKey() + "ToEmail");
      } else {
        emailTo = p.getProperty("elseToEmail");
      }
    } catch (Exception e) {
      LOG.warn("Cannot find store email...", e);
      emailTo = p.getProperty("elseToEmail");
    }

    final String emailFrom = p.getProperty("MailsFromEmail");

    final String username = p.getProperty("MailsFromEmail");
    final String password = p.getProperty("MailsFromPwd");

    Properties props = new Properties();
    props.put("mail.smtp.auth", p.getProperty("mail.smtp.auth"));
    props.put("mail.smtp.starttls.enable", p.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.host", p.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", p.getProperty("mail.smtp.port"));
    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });

    try {
      LOG.debug("Send mail from " + emailFrom + " to " + emailTo);

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
      message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(p
          .getProperty("storeMailsCc")));
      message.setSubject("Auto_Requisition_" + storeName + "_" + date + "");

      final String body = "<br>Hello All,<br><br>"
          + "The Auto Requisition is run for "
          + storeName
          + " store. The quantity requested by Auto Requisition are as follows"
          + "<br><br>"
          + "<font size='3px' face='verdana'>"
          + "<pre>"
          + storeName
          + " Store    : "
          + qtyConfirmed
          + " qtys (auto)<br>"
          + "</pre>"
          + "</font>"
          + "<br><font color='red'><b>NOTE: CWH, RWH, HUB values are displayed as NA , If Ordered quantity is zero as we dont check the Warehouse Stock.</b></font><br><br>"
          + "Regards,<br>" + "ERP Team";

      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/html; charset=utf-8");
      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(messageBodyPart);
      MimeBodyPart messageBodyPart1 = new MimeBodyPart();
      try {
        if (filename != null && filename.isFile()) {
          messageBodyPart1.attachFile(filename);
          multipart.addBodyPart(messageBodyPart1);
        }

      } catch (IOException e) {
        LOG.warn("Cannot attach the file to the email...", e);
      }

      message.setContent(multipart);
      Transport.send(message);
      LOG.debug("Done");

    } catch (MessagingException e) {
      LOG.error("Error sending mail to warehouse", e);
      throw new RuntimeException(e);
    }

  }

  public Long sendMailToWarehouse(String orgName, String orgId, File filename) {
    LOG.info("send mail to warehouse for " + orgName + " " + orgId);

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);

    // Store Qty Req
    String storeName = orgName;
    final Organization org = OBDal.getInstance().get(Organization.class, orgId);

    long sumQtyConfirm = getQtyConfirmed(orgId, currentDate);

    // Mail Script Strarted Here
    String emailTo = "";
    emailTo = p.getProperty(storeName + "ToEmail");
    if (emailTo == null) {
      emailTo = p.getProperty("elseToEmail");
    }

    // GWL WHAT IS THAT???????????? Emailto before don't needed?????? ///

    if (p.getProperty(org.getSearchKey() + "ToEmail") != null
        || p.getProperty(org.getSearchKey() + "ToEmail").equals("")) {
      emailTo = p.getProperty(org.getSearchKey() + "ToEmail");
    } else {
      emailTo = p.getProperty("elseToEmail");
    }

    final String emailFrom = p.getProperty("MailsFromEmail");

    final String username = p.getProperty("MailsFromEmail");
    final String password = p.getProperty("MailsFromPwd");

    Properties props = new Properties();
    props.put("mail.smtp.auth", p.getProperty("mail.smtp.auth"));
    props.put("mail.smtp.starttls.enable", p.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.host", p.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", p.getProperty("mail.smtp.port"));
    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });

    try {
      LOG.debug("Send mail from " + emailFrom + " to " + emailTo);

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
      message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(p
          .getProperty("storeMailsCc")));
      message.setSubject("Auto_Requisition_" + storeName + "_" + date + "");

      final String body = "<br>Hello All,<br><br>"
          + "The Auto Requisition is run for "
          + storeName
          + " store. The quantity requested by Auto Requisition are as follows"
          + "<br><br>"
          + "<font size='3px' face='verdana'>"
          + "<pre>"
          + storeName
          + " Store    : "
          + sumQtyConfirm
          + " qtys (auto)<br>"
          + "</pre>"
          + "</font>"
          + "<br><font color='red'><b>NOTE: CWH, RWH, HUB values are displayed as NA , If Ordered quantity is zero as we dont check the Warehouse Stock.</b></font><br><br>"
          + "Regards,<br>" + "ERP Team";

      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/html; charset=utf-8");
      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(messageBodyPart);
      MimeBodyPart messageBodyPart1 = new MimeBodyPart();
      try {
        if (filename != null && filename.isFile()) {
          messageBodyPart1.attachFile(filename);
          multipart.addBodyPart(messageBodyPart1);
        }

      } catch (IOException e) {
        LOG.warn("Cannot attach the file to the email...", e);
      }

      message.setContent(multipart);
      Transport.send(message);
      LOG.debug("Done");

    } catch (MessagingException e) {
      LOG.error("Error sending mail to warehouse", e);
      throw new RuntimeException(e);
    }
    return sumQtyConfirm;
  }

  private long getQtyConfirmed(String orgId, final Date currentDate) {
    final Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement pst = null;
    ResultSet rs = null;

    try {
      String query = "select round(sum(em_sw_confirmedqty),0) as totalconfirmqty from "
          + "c_orderline where created >= ?  and createdby='100' and ad_org_id=?";
      LOG.debug(query);

      pst = conn.prepareStatement(query);
      pst.setDate(1, new java.sql.Date(currentDate.getTime()));
      pst.setString(2, orgId);

      rs = pst.executeQuery();

      if (rs.next()) {
        return (long) rs.getInt("totalconfirmqty");
      }
    } catch (Exception e1) {
      LOG.error("Cannot get the qty to send mail", e1);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (pst != null)
          pst.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        // No operation - if crash then we don't care...
      }
    }
    return 0;
  }

  // Main Mail sent To Every One
  public void mainMailToAll(Map<String, Long> config) {
    LOG.debug("Allmail");
    Long totalQtyToPick = 0L;
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    final StringBuilder sb = new StringBuilder();

    sb
        .append("<br/>"
            + "Hello All,"
            + "<br/><br/>"
            + "The Auto Requisition is run for the stores. The quantity requested by Auto Requisition are as follows"
            + "<br/><br/>" + "<font size='3px' face='verdana'>" + "<pre>");
    for (Entry<String, Long> mapEntry : config.entrySet()) {
      sb.append(mapEntry.getKey() + " : " + mapEntry.getValue() + " qtys (auto)<br/><br/>");
      totalQtyToPick += mapEntry.getValue();
    }

    sb.append("</hr>" + "TOTAL QTY :" + totalQtyToPick + " qtys (auto)<br/><br/>");
    sb.append("</pre>" + "</font>" + "<br/><br/>" + "Regards,<br/>" + "ERP Team");

    final String toMail = p.getProperty("allToEmailIds");
    final String emailFrom = p.getProperty("MailsFromEmail");

    final String username = p.getProperty("MailsFromEmail");
    final String password = p.getProperty("MailsFromPwd");

    Properties props = new Properties();
    props.put("mail.smtp.auth", p.getProperty("mail.smtp.auth"));
    props.put("mail.smtp.starttls.enable", p.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.host", p.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", p.getProperty("mail.smtp.port"));
    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });
    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toMail));
      message.setSubject("Auto_Requisition_" + date);
      message.setContent(sb.toString(), "text/html; charset=utf-8");
      Transport.send(message);
      LOG.debug("Done");

    } catch (MessagingException e) {
      LOG.debug("Error sending allmail", e);
      throw new RuntimeException(e);
    }

  }

  public void mailToSupplierFromSupplyErp(String warehouseId, StringBuffer documents)
      throws JSONException {
    LOG.debug("mail To Supplier From Supply Erp");
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    final StringBuilder sb = new StringBuilder();

    sb.append("<br/>" + "Hello," + "<br/><br/>"
        + "The order had been generated for you. The Documents created is/are as follows:"
        + "<br/><br/>" + "<font size='3px' face='verdana'>" + "<pre>");
    sb.append(documents + "<br/><br/>");
    sb.append("</pre>" + "</font>" + "<br/><br/>" + "Regards,<br/>" + "DSI");

    final List<FacstWarehouseUsers> userList = getWarehousetUsers(warehouseId);
    String toMail = "";
    for (FacstWarehouseUsers warehouseUser : userList) {
      String email = warehouseUser.getUser().getEmail();
      if (email == null || email.equals("")) {
        LOG.error("Email is not defined for the user " + warehouseUser.getUser());
      } else {
        toMail += email + ",";
      }

    }
    if (toMail == "" && toMail.length() <= 0) {
      BusinessEntityMapper.createErrorLogRecord(new OBException("Email is not defined"),
          "91B9553DBE0D421EAF868590ED701524", null);
      return;
    }

    if (toMail.length() > 0 && toMail.charAt(toMail.length() - 1) == ',')
      toMail = toMail.substring(0, toMail.length() - 1);

    final String emailFrom = p.getProperty("MailsFromEmail");

    final String username = p.getProperty("MailsFromEmail");
    final String password = p.getProperty("MailsFromPwd");

    Properties props = new Properties();
    props.put("mail.smtp.auth", p.getProperty("mail.smtp.auth"));
    props.put("mail.smtp.starttls.enable", p.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.host", p.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", p.getProperty("mail.smtp.port"));
    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });
    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toMail));
      message.setSubject("Order_" + date);
      message.setContent(sb.toString(), "text/html; charset=utf-8");
      Transport.send(message);
      LOG.debug("Done");

    } catch (MessagingException e) {
      LOG.debug("Error sending allmail", e);
      throw new RuntimeException(e);
    }

  }

  private List<User> getUserByDefaultWarehouse(String warehouseId) {
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
    List<User> users = warehouse.getADUserDefaultWarehouseList();
    if (users == null || users.size() <= 0) {
      throw new OBException("No user for warehouse " + warehouse);
    }
    return users;
  }

  public void sendMailToWarehouseByDirectDelivery(File filename, String warehouseId)
      throws Exception {
    LOG.debug("mail To Supplier From Supply Erp");
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    final StringBuilder sb = new StringBuilder();

    String body = "<br>Hello All,<br><br>"
        + "The Auto Requisition ran for all the stores. "
        + " <br><br> Find the attachment( csv ) below to check for the stock positions in the external warehouses."
        + "<br><br><br>" + "Regards,<br>" + "ERP Team";

    final List<FacstWarehouseUsers> userList = getWarehousetUsers(warehouseId);
    String toMail = "";
    for (FacstWarehouseUsers warehouseUser : userList) {
      String email = warehouseUser.getUser().getEmail();
      if (email == null || email.equals("")) {
        LOG.error("Email is not defined for the user " + warehouseUser.getUser());
      } else {
        toMail += email + ",";
      }

    }
    if (toMail == "" && toMail.length() <= 0) {
      BusinessEntityMapper.createErrorLogRecord(new OBException("Email is not defined"),
          "91B9553DBE0D421EAF868590ED701524", null);
      return;
    }

    if (toMail.length() > 0 && toMail.charAt(toMail.length() - 1) == ',')
      toMail = toMail.substring(0, toMail.length() - 1);

    final String emailFrom = pForFTS.getProperty("MailsFromEmail");

    final String username = pForFTS.getProperty("MailsFromEmail");
    final String password = pForFTS.getProperty("MailsFromPwd");

    Properties props = new Properties();
    props.put("mail.smtp.auth", pForFTS.getProperty("mail.smtp.auth"));
    props.put("mail.smtp.starttls.enable", pForFTS.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.host", pForFTS.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", pForFTS.getProperty("mail.smtp.port"));
    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });
    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toMail));
      message.setSubject("Factory_To_Store - Stock Position" + date);

      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/html; charset=utf-8");
      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(messageBodyPart);
      MimeBodyPart messageBodyPart1 = new MimeBodyPart();
      try {
        if (filename != null && filename.isFile()) {
          messageBodyPart1.attachFile(filename);
          multipart.addBodyPart(messageBodyPart1);
        }

      } catch (IOException e) {
        LOG.warn("Cannot attach the file to the email...", e);
      }

      message.setContent(multipart);
      Transport.send(message);
      LOG.debug("Done");
    } catch (MessagingException e) {
      LOG.debug("Error sending allmail", e);
      throw new RuntimeException(e);
    }
  }

  private List<FacstWarehouseUsers> getWarehousetUsers(String warehouseId) {
    String qry = "id in (select id from facst_warehouse_users whuser where "
        + " whuser.warehouse.id='" + warehouseId + "')";
    OBQuery<FacstWarehouseUsers> strQry = OBDal.getInstance().createQuery(
        FacstWarehouseUsers.class, qry);
    List<FacstWarehouseUsers> WarehouseUsersList = strQry.list();
    return WarehouseUsersList;
  }
}
