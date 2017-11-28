package in.decathlon.supply.oba.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.log4j.Logger;
import java.io.File;

public class OBAUitility {
	
	private static final Logger LOG4J = Logger.getLogger(OBAUitility.class);
	
	// to download to a local folder from server
	public static boolean downloadFromServerThroughFTPS(String host,
			String userName, String password, String localFolderLocation,
			String remoteFolderLocation) throws NoSuchAlgorithmException {
		
		boolean success = false; 
		final FTPSClient ftpsClient = new FTPSClient();
		
		try {
			ftpsClient.connect(host);
			ftpsClient.login(userName, password);
			ftpsClient.enterLocalPassiveMode();

			FTPFile[] files = ftpsClient.listFiles(remoteFolderLocation);
			
			if (files != null && files.length > 0) {
				for (FTPFile file : files) {
					if (!file.isFile()) {
                        continue;
                    }
					LOG4J.info("File is " + file.getName());
                    
                    //get output stream
					final OutputStream output = new BufferedOutputStream(new FileOutputStream(localFolderLocation + file.getName()));
                    ftpsClient.retrieveFile(remoteFolderLocation + file.getName(), output);
                    output.close();
                    
				}
			}
			
            if (success) {
                LOG4J.info("Files has been downloaded successfully.");
            }
			
		} catch (IOException e) {
			LOG4J.info("Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (ftpsClient.isConnected()) {
					ftpsClient.logout();
					ftpsClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return success;
	}
	

	
}
