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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

public class ImplantationMails implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;
  private static volatile ImplantationMails instance;
  final Properties p = SuppyDCUtil.getInstance().getProperties();

  private ImplantationMails() {
    // no-op
  }

  public static ImplantationMails getInstance() {
    if (instance == null) {
      synchronized (SuppyDCUtil.class) {
        if (instance == null) {
          instance = new ImplantationMails();
        }
      }
    }
    return instance;
  }


public void sendmailtoall(Organization org, File csvfile, String confirmedQty) {
	

    System.out.println("mail");
    
    final Connection conn = OBDal.getInstance().getConnection();
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    PreparedStatement pst = null;
    ResultSet rs = null;
    String emailTo = "";
    String filename = csvfile.toString();
    String orgName = org.getName();
    
    // Mail Script Strarted Here
        
    if (p.getProperty(orgName + "ImpToEmail") != null || !p.getProperty(orgName + "ImpToEmail").equals("")) {
		emailTo = p.getProperty(orgName + "ImpToEmail");
	} else {
		emailTo = p.getProperty("ImpElseEmail");
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
      System.out.println("emailFrom: " + emailFrom);
      System.out.println("emailTo: " + emailTo);
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
      message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(p.getProperty("ImpCCEmail")));
      message.setSubject("Implantation_" + orgName + "_" + date + "");
      final String body = "<br>Hello All,<br><br>"
          + "The Auto Implantation is run for "
          + orgName
          + " store. The quantity requested by Auto Implantation are as follows"
          + "<br><br>"
          + "<font size='3px' face='verdana'>"
          + "<pre>"
          + orgName
          + " Store    : "
          + confirmedQty
          + " qtys (auto)<br>"
          + "</pre>"
          + "</font>"
          + "<br><br><br>"
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
