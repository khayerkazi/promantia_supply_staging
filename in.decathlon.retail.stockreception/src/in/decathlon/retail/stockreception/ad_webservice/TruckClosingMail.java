package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

import org.decathlon.warehouse.truckreception.DTRTruckReception;
import org.openbravo.dal.service.OBDal;

public class TruckClosingMail implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;
  private static volatile TruckClosingMail instance;
  final Properties p = SuppyDCUtil.getInstance().getProperties();

  private TruckClosingMail() {
    // no-op
  }

  public static TruckClosingMail getInstance() {
    if (instance == null) {
      synchronized (SuppyDCUtil.class) {
        if (instance == null) {
          instance = new TruckClosingMail();
        }
      }
    }
    return instance;
  }

  public void sendmailtoall(String orgName, String filename, List<DTRTruckReception> truckObj) {
    System.out.println("mail");

    final Connection conn = OBDal.getInstance().getConnection();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    String storeName = orgName;
    PreparedStatement pst = null;
    ResultSet rs = null;
    String emailTo = "";
    // Mail Script Strarted Here

    // String emailTo = "adarsh.balaji@decathlon.in";

    if (p.getProperty(orgName + "ClosingToEmail") != null
        || !p.getProperty(orgName + "ClosingToEmail").equals("")) {
      emailTo = p.getProperty(orgName + "ClosingToEmail");
    } else {
      emailTo = p.getProperty("ClosingElseEmail");
    }

    final String emailFrom = p.getProperty("MailsFromEmail");
    final String fullName = "ERP TEAM";

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
      System.out.println("emailFrom: " + emailFrom);
      System.out.println("emailTo: " + emailTo);
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
      message.setRecipients(Message.RecipientType.BCC,
          InternetAddress.parse(p.getProperty("CloseBCCEmail")));
      message.setSubject("Truck Closing_" + storeName + "_" + date + "");
      final String body = "<br>Hello All,<br><br>" + "The Truck no "
          + truckObj.get(0).getDocumentNo() + " has been closed for " + storeName
          + " store. Please find the attachment for more information." + "<br><br>"
          + "Regards,<br>" + "ERP Team";

      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/html; charset=utf-8");
      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(messageBodyPart);
      MimeBodyPart messageBodyPart1 = new MimeBodyPart();
      try {
        if (!filename.equals("")) {
          messageBodyPart1.attachFile(new File(filename));
          multipart.addBodyPart(messageBodyPart1);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }

      message.setContent(multipart);
      Transport.send(message);
      System.out.println("Done");

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (pst != null) {
          pst.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }
  }
}
