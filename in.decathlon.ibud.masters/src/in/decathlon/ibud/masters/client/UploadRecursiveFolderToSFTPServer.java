package in.decathlon.ibud.masters.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class UploadRecursiveFolderToSFTPServer {

  static ChannelSftp channelSftp = null;
  static Session session = null;
  static Channel channel = null;
  static String PATHSEPARATOR = "/";
  static boolean flag = false;
  private static Logger logger = Logger.getRootLogger();

  /**
   * @param fileNames
   * @param args
   * @return
   */
  public static boolean pushedFileToNewSFTPLocaltion(String sourceDir, String SFTPDescDir,
      List<String> fileNames) {
    int SFTPPort = 22; // SFTP Port Number

    try {

      String SFTPHost = "192.168.0.128";
      String SFTPUser = "rojar";
      String SFTPPass = "1";
      String movedFolder = "/home/rojar/movedFolder";

      JSch jsch = new JSch();
      session = jsch.getSession(SFTPUser, SFTPHost, SFTPPort);

      session.setPassword(SFTPPass);
      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.connect(); // Create SFTP Session
      channel = session.openChannel("sftp"); // Open SFTP Channel
      channel.connect();
      channelSftp = (ChannelSftp) channel;
      channelSftp.cd(SFTPDescDir);
      // Change Directory on SFTP Server

      recursiveFolderUpload(sourceDir, SFTPDescDir, movedFolder, fileNames);

    } catch (SftpException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSchException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (channelSftp != null)
        channelSftp.disconnect();
      if (channel != null)
        channel.disconnect();
      if (session != null)
        session.disconnect();

    }
    return flag;

  }

  /**
   * This method is called recursively to Upload the local folder content to SFTP server
   * 
   * @param sourcePath
   * @param destinationPath
   * @param movedFolder
   * @param fileNames
   * @return
   * @throws SftpException
   * @throws FileNotFoundException
   */
  private static void recursiveFolderUpload(String sourcePath, String destinationPath,
      String movedFolder, List<String> fileNames) throws SftpException, FileNotFoundException {
    try {
      logger.info("source File Path: " + sourcePath);
      File sourceDir = new File(sourcePath);
      File movedDir = new File(movedFolder);

      // this is move file, source to move folder

      /*
       * if (fileNames.size() > 0) {
       * 
       * for (int i = 0; i < fileNames.size(); i++) { File file = new File(sourcePath +
       * fileNames.get(i)); logger.info("source File name: " + fileNames.get(i));
       * 
       * // Move file to new directory boolean success = file.renameTo(new File(sourceDir,
       * file.getName())); if (!success) { // File was not successfully moved
       * logger.error("File not moved " + file.getAbsolutePath()); } else {
       * logger.info("File moved " + file.getAbsolutePath()); } } }
       */

      // copy the all file move folder to sftp
      File[] files = sourceDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(@SuppressWarnings("hiding") File sourceDir, String name) {
          return name.endsWith(".xml");
        }
      });

      for (File sourceFile : files) {
        // copy if it is a file
        channelSftp.cd(destinationPath);
        if (sourceFile.getName().endsWith(".xml")) {
          channelSftp.put(new FileInputStream(sourceFile), sourceFile.getName(),
              ChannelSftp.OVERWRITE);
          flag = true;
          logger.info("File " + sourceFile + " moved to " + destinationPath);

          File file = new File(sourcePath + sourceFile.getName());

          // Move file to new directory
          boolean success = file.renameTo(new File(movedDir, file.getName()));
          if (!success) { // File was not successfully moved
            logger.error("File not moved " + file.getAbsolutePath());
          } else {
            logger.info("File moved " + file.getAbsolutePath());
          }

        } else {
          logger.error("XML file is not present on location " + sourceFile);
        }
      }
    } catch (Exception ex) {
      logger.error("Error while transfer File " + movedFolder + "   to " + destinationPath);

      ex.printStackTrace();

    }
  }
}
