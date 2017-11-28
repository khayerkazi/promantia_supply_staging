package com.sysfore.sankalpcrm.ad_actionButton;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.util.*;

import javax.naming.*;
import java.io.File;
import java.net.URI;

public class MailManager
{	
	//private String senders;
	private String emailFrom;
	private String emailTo;
	private String emailCc;
	private String emailBcc;
	private String smtphost;
	private String msgSubject;
	private String msgText;
	public MailManager(){
		
	}
	public MailManager(
			String emailFrom,
			String emailTo,
			String emailCc,
			String emailBcc,		
		String smtphost,
		String msgSubject, 
		String msgText)
	{
		//this.senders =  senders;
		this.emailFrom = emailFrom;
		this.emailTo = emailTo;
		this.emailCc = emailCc;
		this.emailBcc = emailBcc;
		
		this.smtphost = smtphost;		
		this.msgSubject = msgSubject;
		this.msgText = msgText;
		
	}

	public void run()
	{
		try
		{
			
			sendMessage(emailFrom, emailTo,emailCc,emailBcc, smtphost,msgSubject, msgText);
		}
		catch(Exception e)
		{
			
		}
	}
	public synchronized boolean sendMessage(String emailFrom, String emailTo,String emailCc,String emailBcc, String smtphost, String msgSubject, String msgText)
    {
		boolean debug = false;      
		
			try
			{   
				ResourceBundle rd = ResourceBundle.getBundle("com.sysfore.sankalpcrm.ad_actionButton.soap");
				
				Properties props = new Properties();
				props.put("mail.smtp.host", rd.getString("mailhost"));
				props.put("mail.smtp.port", rd.getString("mailport"));	
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable","true");
				props.put("mail.smtp.socketFactory.port",rd.getString("mailport"));
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				props.put("mail.smtp.socketFactory.fallback", "false");
				Authenticator auth = new SMTPAuthenticator();
				Session session = Session.getDefaultInstance(props, auth);
				//Session session = Session.getDefaultInstance(props, null);
				session.setDebug(debug);
				Message msg = new MimeMessage(session);
				InternetAddress from = new InternetAddress(emailFrom);
				msg.setFrom(from);
				StringTokenizer stTo = new StringTokenizer(emailTo, ";");
				while(stTo.hasMoreTokens()){
					emailTo = stTo.nextToken(); 
					InternetAddress[] address = {
							new InternetAddress(emailTo)
							};
					msg.setRecipients(Message.RecipientType.TO, address);
				}
				StringTokenizer stCc = new StringTokenizer(emailCc, ";");
				while(stCc.hasMoreTokens()){
					emailCc = stCc.nextToken();
					InternetAddress[] cc = {
							new InternetAddress(emailCc)
							};
					msg.addRecipients(Message.RecipientType.CC, cc);
				}
				StringTokenizer stBcc = new StringTokenizer(emailBcc, ";");
				while(stBcc.hasMoreTokens()){
					emailBcc = stBcc.nextToken();
					InternetAddress[] bcc = {
							new InternetAddress(emailBcc)
							};
					msg.addRecipients(Message.RecipientType.BCC, bcc);
				}				
				msg.setSubject(msgSubject);
				
				MimeBodyPart mbp1 = new MimeBodyPart();
				mbp1.setContent(msgText, "text/html");

				MimeMultipart mp = new MimeMultipart();
				mp.setSubType("related");
				mp.addBodyPart(mbp1);
				msg.setContent(mp);

				Transport.send(msg);
				
				//return "Email sent to " + emailto;
			}
			catch(Exception mex)
			{
				mex.printStackTrace();
				return false;
			}
		

		return true;
		
    }

	private class SMTPAuthenticator extends javax.mail.Authenticator
	{

		public PasswordAuthentication getPasswordAuthentication()
		{
			ResourceBundle rd = ResourceBundle.getBundle("com.sysfore.sankalpcrm.ad_actionButton.soap");
			String username = rd.getString("mailusername");
			String password = rd.getString("mailpassword");
			return new PasswordAuthentication(username, password);
		}
	}
}

