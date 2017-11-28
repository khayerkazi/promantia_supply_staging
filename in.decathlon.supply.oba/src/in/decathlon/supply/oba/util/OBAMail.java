package in.decathlon.supply.oba.util;

import java.io.Serializable;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class OBAMail implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  private OBAMail() {
    // no-op
  }

  	public static void sendMailToWarehouse(String itemCode) {
    
	  System.out.println("mail");
	  // Mail Script Strarted Here
	  String emailTo = "anvesh.k@in.exceloid.com";
	  final String emailFrom = "anvesh.kathuri@gmail.com";

	  final String username = "anvesh.kathuri@gmail.com";
	  final String password = "Annu_aaki";

	  Properties props = new Properties();
	  props.put("mail.smtp.auth", "true");
	  props.put("mail.smtp.starttls.enable", "true");
	  props.put("mail.smtp.host", "smtp.gmail.com");
	  props.put("mail.smtp.port", "587");
	  Session session = Session.getInstance(props, new javax.mail.Authenticator() {
	      protected PasswordAuthentication getPasswordAuthentication() {
	    	  return new PasswordAuthentication(username, password);
	      }
	  });
	  try {
	      Message message = new MimeMessage(session);
	      message.setFrom(new InternetAddress(emailFrom));
	      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
	      message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("anvesh.k2002@gmail.com"));
	      message.setSubject("Alert for Tax Category Mapping for the Item Code");
	      final String body = "<br>Hello All,<br><br>" + "This is an alert mail to add the tax category for the new item code "+itemCode+" in Openbravo." + "<br><br>"
	          + "This new item code is referenced in SAP and the same needs to be mapped with the corresponding Tax Category in the Openbravo System.<br><br>" +
	          		"Thanks to send us the confirmation once done!<br>" + "</pre>" + "</font>" + "<br>" +
	          				"<br>" + "Thanks,<br>" +
	          						"IT Service Team";
	      message.setContent(body, "text/html");
	      /*BodyPart messageBodyPart = new MimeBodyPart();
	      messageBodyPart.setContent(body, "text/html; charset=utf-8");
	      Multipart multipart = new MimeMultipart();
	      multipart.addBodyPart(messageBodyPart);*/
	      /*MimeBodyPart messageBodyPart1 = new MimeBodyPart();
	      try {
	        if (!filename.equals("")) {
	          messageBodyPart1.attachFile(new File(filename));
	          multipart.addBodyPart(messageBodyPart1);
	        }
	
	      } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	      }*/
//	      message.setContent(multipart);
	      Transport.send(message);
	      System.out.println("Done");
	
	  } catch (MessagingException e) {
	      throw new RuntimeException(e);
	  }
	}

}
