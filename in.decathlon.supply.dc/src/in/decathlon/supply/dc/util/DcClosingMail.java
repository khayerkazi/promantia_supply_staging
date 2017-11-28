package in.decathlon.supply.dc.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;

public class DcClosingMail implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;
  private static volatile DcClosingMail instance;
  final Properties p = SuppyDCUtil.getInstance().getProperties();
  static Logger log4j = Logger.getLogger(DcClosingMail.class);

  private DcClosingMail() {
    // no-op
  }

  public static DcClosingMail getInstance() {
    if (instance == null) {
      synchronized (SuppyDCUtil.class) {
        if (instance == null) {
          instance = new DcClosingMail();
        }
      }
    }
    return instance;
  }

  public void sendmailtoall(String orgName, String filename) {
    
    final Connection conn = OBDal.getInstance().getConnection();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    String storeName = orgName;
    PreparedStatement pst = null;
    ResultSet rs = null;
    String emailTo = "";
    // Mail Script Strarted Here
        
    if (p.getProperty(orgName + "ClosingToEmail") != null || !p.getProperty(orgName + "ClosingToEmail").equals("")) {
		emailTo = p.getProperty(orgName + "ClosingToEmail");
	} else {
		emailTo = p.getProperty("ClosingElseEmail");
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
      
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
      message.setRecipients(Message.RecipientType.CC,
          InternetAddress.parse(p.getProperty("CloseCCEmail")));
      message.setSubject("Auto_Closing_DC_" + storeName + "_" + date + "");
      final String body = "<br>Hello All,<br><br>" + "The Auto Closing of DC's is run for " + storeName
          + " store. Please find the attachment for more information." + "<br><br>"
          + "Regards,<br>"
          + "ERP Team";

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
      log4j.info("Mail sent - " + storeName);

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
