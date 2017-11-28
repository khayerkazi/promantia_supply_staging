package in.decathlon.supply.oba.ad_process;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
import in.decathlon.supply.oba.data.OBA_ModelProduct;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import au.com.bytecode.opencsv.CSVWriter;

public class NewItemCodeMailToDMI extends DalBaseProcess {

	private ProcessLogger logger;
	final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
	
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		logger = bundle.getLogger();
		
		logger.logln("Mailing process about new itemcodes in OBA for DMI team has started!");
		final Map<String, String> mailDetails = new HashMap<String, String>();
		
		// Fetching mail configurations from dsidef_module_config table
		OBContext.setAdminMode();
		OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
		configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.supply.oba"));
		if(configInfoObCriteria.count() > 0) {
			for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
				mailDetails.put(config.getKey(), config.getSearchKey());
			}
		}
		logger.logln("Taken all the mailing configuraion details!");
		
		// CSV generation code
		String finename = mailDetails.get("csvPath")+"OBA_new_itemcodes-"+sdf.format(new Date())+".csv";
        final File file = new File(finename);
        // if file doesnt exists, then create it
        if (!file.exists()) {
          file.createNewFile();
        }
        final CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile()));

        // CSV Heading
        List<String[]> data = new ArrayList<String[]>();
        data.add(new String[] {"Modelcode","Item Code","Nature Of Product","Tax Category"});
		
		// Fetching new itemcodes from oba_modelproduct table, for which the mail is not sent
		OBCriteria<OBA_ModelProduct> obaModelProdObCriteria = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
		obaModelProdObCriteria.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_MAILSENT, false));
		if(obaModelProdObCriteria.count() > 0) {
			for (OBA_ModelProduct modelProductRecord : obaModelProdObCriteria.list()) {
				data.add(new String[] {modelProductRecord.getModelCode(),modelProductRecord.getItemCode(),modelProductRecord.getNatureOfProduct(),modelProductRecord.getTaxCategory()});
				
				// update the oba_modelproduct record with mailsent column as 'Y'
				modelProductRecord.setMailsent(true);
				
				OBDal.getInstance().save(modelProductRecord);
//				OBDal.getInstance().flush();
			}
		}
		writer.writeAll(data);
		writer.close();
		OBContext.restorePreviousMode();
		
		if(obaModelProdObCriteria.count() > 0) {
			logger.logln("Mail Process starts for "+obaModelProdObCriteria.count()+" itemcodes!");
			sendMailToDMI(mailDetails, logger, file);
		} else {
			logger.logln("No new modelcodes are there to send the mail!");
		}
		logger.logln("Mailing process about new itemcodes in OBA for DMI team has stopped!");
		OBDal.getInstance().commitAndClose();
		
	}
	
	private void sendMailToDMI(Map<String, String> mailDetails,
			ProcessLogger logger2, File filename) {

		// Mail Script starts from here
		String emailTo = mailDetails.get("ToEmail");
		final String emailFrom = mailDetails.get("fromEmail");
		final String emailFromPwd = mailDetails.get("fromEmailPwd");

		Properties props = new Properties();
		props.put("mail.smtp.auth", mailDetails.get("mail.smtp.auth"));
		props.put("mail.smtp.starttls.enable", mailDetails.get("mail.smtp.starttls.enable"));
		props.put("mail.smtp.host", mailDetails.get("mail.smtp.host"));
		props.put("mail.smtp.port", mailDetails.get("mail.smtp.port"));
		
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailFrom, emailFromPwd);
			}
		});
		
		try {
		      Message message = new MimeMessage(session);
		      message.setFrom(new InternetAddress(emailFrom));
		      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
		      message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(mailDetails.get("CCMail")));
		      message.setSubject("Alert for Tax Category Mapping for new Item Codes came from campus");
		      
		      final String body = "<br>Hello All,<br><br>" 
		    		  + "This is an alert mail to add the tax categories for the attached new item codes in Openbravo." + "<br><br>"
			          + "This new item codes are referenced in SAP and the same needs to be mapped with the corresponding Tax Category "
			          + "in the Openbravo System.<br><br>"
			          + "Thanks to send us the confirmation once done!<br>" + "</pre>" + "</font>" + "<br>" 
			          +	"<br>" + "Thanks,<br>" 
			          + "IT Service Team";

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
					e.printStackTrace();
					logger2.logln("Exception : Cannot attach the file to the email...");
				}

				message.setContent(multipart);
				Transport.send(message);
				logger2.logln("Mail sent!");
		      
		  } catch (MessagingException e) {
		      throw new RuntimeException(e);
		  }
	}

}
