package in.decathlon.finance.export.ad_process;

import org.apache.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;

import com.zehon.FileTransferStatus;
import com.zehon.exception.FileTransferException;
import com.zehon.ftps.FTPs;

public class FtpEone {

	Logger log4j = Logger.getLogger(getClass());

	/*
	 * This method is being used for transfer the generated xml files. The
	 * 'username', 'password', 'serveripaddr' and 'secureconnection' parameters
	 * has to be defined in the Openbravo.properties file. secureconnection is
	 * set as either 'ftp' or 'ftps'
	 */

	public void uploadEone(String fullFileName, String fileName,
			String xmlLocation) {
		String userName = OBPropertiesProvider.getInstance()
				.getOpenbravoProperties().getProperty("username");
		String password = OBPropertiesProvider.getInstance()
				.getOpenbravoProperties().getProperty("password");
		String serverAddr = OBPropertiesProvider.getInstance()
				.getOpenbravoProperties().getProperty("serveripaddr");

		log4j.info("Ftp User is:" + userName + " & Host is:" + serverAddr);

		boolean status = false;

		try {
			status = FTPs.folderExists(xmlLocation, serverAddr, userName,
					password);

			if (status) {

				try {

					int sendingstatus = FTPs.sendFile(fullFileName,
							xmlLocation, fileName, serverAddr, userName,
							password);

					if (FileTransferStatus.SUCCESS == sendingstatus) {
						log4j.info(fileName
								+ " got ftps-ed successfully to  folder "
								+ xmlLocation);
					}
				} catch (FileTransferException e) {
					log4j.error("File Transfer Exception " + e.getMessage());
				}
			} else {

				log4j.error("Folder Does Not Exist. Program will exit without transferring "
						+ fileName);

			}

		} catch (FileTransferException e) {
			System.out.println("Could not reach the remote FTP Server.");
			log4j.error("Could not reach the remote FTP Server."
					+ e.getMessage());

		}

	}

	
}
