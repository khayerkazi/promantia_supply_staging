package in.decathlon.ibud.masters.client;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.masters.data.IbudServerTime;

public class FlexSyncMasterForCloseOrder extends DalBaseProcess {
  // private static Connection con = null;
  private static String sourceFolder = null;
  private static String movedFolder = null;
  public static String host = null;
  public static String username = null;
  public static String password = null;
  private static String sftpDestFolder = null;
  private static Logger logger = Logger.getRootLogger();
  private static String xmlid = null;
  private static String supplierCode = null;
  private static String orderActionCode = null;
  private static String orderStatus = null;
  private static final Logger log = Logger.getLogger(FlexSyncMasterForCloseOrder.class);
  static JSONWebServiceInvocationHelper masterHandler = new JSONWebServiceInvocationHelper();
  private static ProcessLogger monitorLogger;
  String tableDetails = "";
  String condIdOrTime = "id";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    monitorLogger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      log.debug("Inside MasterSyncClient class to GET master data");
      String updatedDate = getUpdatedTime("FlexProcess", 2);
      monitorLogger
          .log("Requesting Supply to get data for Flex Process ffrom Date: " + updatedDate);
      condIdOrTime = "time";
      sendCloseOrderTOSFTP(updatedDate);
      BusinessEntityMapper.setLastUpdatedTime(updated, "FlexProcess");
    }

    catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      monitorLogger.log(e.getMessage());
    }

    // processServerData(jsonObj);

  }

  private String getUpdatedTime(String serviceKey, int lastUpdatedDays) throws ParseException {
    OBContext.setAdminMode(true);
    OBCriteria<IbudServerTime> ibudServerTimeCriteria = OBDal.getInstance()
        .createCriteria(IbudServerTime.class);
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, serviceKey));
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_CLIENT,
        OBContext.getOBContext().getCurrentClient()));
    ibudServerTimeCriteria.setMaxResults(1);
    List<IbudServerTime> ibudServerTimeList = ibudServerTimeCriteria.list();

    Date lastUpdatedTime = null;

    if (ibudServerTimeList != null && ibudServerTimeList.size() > 0) {
      log.debug("Time taken from database ibudServerTimeList.get(0).getLastupdated() "
          + ibudServerTimeList.get(0).getLastupdated());
      lastUpdatedTime = ibudServerTimeList.get(0).getLastupdated();

      return lastUpdatedTime.toString();
    } else {
      log.debug(
          "No record found for the Service " + serviceKey + ". New record need to insert !!! ");

      IbudServerTime newService = OBProvider.getInstance().get(IbudServerTime.class);
      newService.setActive(true);
      newService.setClient(OBContext.getOBContext().getCurrentClient());
      newService.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      newService.setCreatedBy(OBContext.getOBContext().getUser());
      newService.setCreationDate(new Date());
      newService.setUpdatedBy(OBContext.getOBContext().getUser());
      newService.setUpdated(new Date());
      newService.setNewOBObject(true);

      Date d = new Date();
      Date dateBefore = new Date(d.getTime() - lastUpdatedDays * 24 * 3600 * 1000);
      newService.setLastupdated(dateBefore);

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      String lastUpdatedTime1 = format.format(dateBefore);

      newService.setServiceKey(serviceKey);
      OBDal.getInstance().save(newService);
      SessionHandler.getInstance().commitAndStart();
      return lastUpdatedTime1.toString().replaceAll(" ", "_");

    }
  }

  private void sendCloseOrderTOSFTP(String idOrTime) {
    logger.info("Flex Written Process is Running for Generate DC XML");

    host = "192.168.0.32";
    username = "Swathi";
    password = "Decathl0n";
    sftpDestFolder = "/home/rojar/sftpDestFolder";
    sourceFolder = "/home/swathi/temp/sourceFolder";
    movedFolder = "/home/swathi/temp/movedFolder";

    File checkSourceDir = new File(sourceFolder);
    if (!checkSourceDir.exists()) {
      checkSourceDir.mkdir();
    }
    File checkMovedDir = new File(movedFolder);
    if (!checkMovedDir.exists()) {
      checkMovedDir.mkdir();
    }
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setNamespaceAware(true);
    DocumentBuilder docBuilder = null;
    try {

      docBuilder = docFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e2) {
      logger.error("Could not instantiate docBuilder", e2);
    }
    // Variable Declarations
    // ResultSet rs = null;
    // ResultSet rs2 = null;
    // PreparedStatement pst = null;

    List<String> fileNames = new ArrayList<String>();

    Element rootElement = null;
    try {

      String sql = " SELECT distinct h.em_sw_postatus as postatus, h.documentno as documentNo,to_char(h.created,'YYYY-mm-dd') as created,to_char(h.updated,'YYYY-mm-dd') as updated,"
          + " to_char(h.dateordered,'YYYY-mm-dd') as dateordered,to_char(h.datepromised,'YYYY-mm-dd') as datepromised,to_char(h.em_sw_expdeldate,'YYYY-mm-dd') as em_sw_expdeldate,"
          + " l.em_sw_suppliercode as suppliercode "
          + " from c_order h,c_orderline l,c_bpartner bp ,cl_dpp_seqno cd where h.c_order_id=l.c_order_id "
          + " and bp.em_cl_supplierno=l.em_sw_suppliercode " + " and bp.em_rc_source='DPP' "
          + " and h.em_sw_postatus in ('OCL') and bp.em_cl_supplierno=cd.supplierno "
          + " and h.c_doctype_id ='C7CD4AC8AC414678A525AB7AE20D718C' and h.em_imsap_duplicatesap_po!='Y'";

      if (condIdOrTime.equalsIgnoreCase("time")) {
        // sql = sql + " and h.updated > '" + idOrTime + "'";
      } else {
        sql = sql + " and h.c_order_id = '" + idOrTime + "'";
      }
      // ConnectiontoDB connection = new ConnectiontoDB();
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      List<Object[]> recordList = query.list();

      if (recordList.size() <= 0) {
        logger.info("No Record Found for Flex Process and Order Count is: " + recordList.size());
      }

      for (Object[] rs : recordList) {
        String postatus = null;
        String documentNo = null;
        String created = null;
        String updated = null;
        String datepromised = null;
        String em_sw_expdeldate = null;
        String suppliercode = null;

        if (rs[0] != null) {
          postatus = rs[0].toString();
        }
        if (rs[1] != null) {
          documentNo = rs[1].toString();
        }
        if (rs[2] != null) {
          created = rs[2].toString();
        }
        if (rs[3] != null) {
          updated = rs[3].toString();
        }
        if (rs[5] != null) {
          datepromised = rs[5].toString();
        }
        if (rs[6] != null) {
          em_sw_expdeldate = rs[6].toString();
        }
        if (rs[7] != null) {
          suppliercode = rs[7].toString();
        }

        if (postatus.equals("SO")) {
          orderActionCode = "Add";
          orderStatus = "NV";
        } else if (postatus.equals("MO")) {
          orderActionCode = "Update";
          orderStatus = "";
        } else if (postatus.equals("OCL")) {
          orderActionCode = "Close";
          orderStatus = "C";
        } else {
          orderActionCode = "Delete";
          orderStatus = "";
        }
        logger.info("Process Running for Document No: " + documentNo);

        Document doc = docBuilder.newDocument();
        rootElement = doc.createElement("oxit:ProcessPurchaseOrder");
        doc.appendChild(rootElement);

        Attr releaseID = doc.createAttribute("releaseID");
        rootElement.setAttributeNode(releaseID);

        Attr schemaLocation = doc.createAttribute("xsi:schemaLocation");
        schemaLocation.setValue(
            "http://www.oxylane.com/oxit/oagis ../BODs/Developer/ProcessPurchaseOrder.xsd");
        rootElement.setAttributeNode(schemaLocation);

        Attr oag = doc.createAttribute("xmlns:oag");
        oag.setValue("http://www.openapplications.org/oagis/9");
        rootElement.setAttributeNode(oag);

        Attr oxit = doc.createAttribute("xmlns:oxit");
        oxit.setValue("http://www.oxylane.com/oxit/oagis");
        rootElement.setAttributeNode(oxit);

        Attr xsi = doc.createAttribute("xmlns:xsi");
        xsi.setValue("http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttributeNode(xsi);

        // shorten way
        Element oagApplicationArea = doc.createElement("oag:ApplicationArea");
        rootElement.appendChild(oagApplicationArea);

        Element oagSender = doc.createElement("oag:Sender");
        oagApplicationArea.appendChild(oagSender);

        Element oagLogicalID = doc.createElement("oag:LogicalID");
        oagLogicalID.appendChild(doc.createTextNode("PARTNER"));
        oagSender.appendChild(oagLogicalID);

        // Creation date of the xml
        Element oagCreationDateTime = doc.createElement("oag:CreationDateTime");
        oagCreationDateTime.appendChild(doc.createTextNode(created));
        oagApplicationArea.appendChild(oagCreationDateTime);

        /*
         * Setting an unique XML id for each supplier
         */
        // Retrieve and store supplier code
        supplierCode = suppliercode;
        logger.info("supplier Code is: " + supplierCode);
        // Call getXMLID method to retrieve XML ID
        xmlid = getXMLID(supplierCode);

        // Call getIncrementValue method and get incremented value
        xmlid = getIncrementValue(xmlid);

        // Pass the integer XML ID
        Element oagBODID = doc.createElement("oag:BODID");
        oagBODID.appendChild(doc.createTextNode(xmlid));
        oagApplicationArea.appendChild(oagBODID);

        Attr schemeName = doc.createAttribute("schemeName");
        schemeName.setValue("3020913899741");
        oagBODID.setAttributeNode(schemeName);

        Element oxitDataArea = doc.createElement("oxit:DataArea");
        rootElement.appendChild(oxitDataArea);

        Element oagProcess = doc.createElement("oag:Process");
        oxitDataArea.appendChild(oagProcess);

        Element oagActionCriteria = doc.createElement("oag:ActionCriteria");
        // oagProcess.appendChild(doc.createTextNode(""));
        oagProcess.appendChild(oagActionCriteria);

        Element oagActionExpression = doc.createElement("oag:ActionExpression");
        oagActionExpression.appendChild(doc.createTextNode(
            "/ProcessPurchaseOrder/DataArea/PurchaseOrder/PurchaseOrderHeader/DocumentID/ID"));
        oagActionCriteria.appendChild(oagActionExpression);

        Attr actionCode = doc.createAttribute("actionCode");
        actionCode.setValue(orderActionCode.toString());
        oagActionExpression.setAttributeNode(actionCode);

        Attr expressionLanguage = doc.createAttribute("expressionLanguage");
        expressionLanguage.setValue("XPath 2.0");
        oagActionExpression.setAttributeNode(expressionLanguage);

        Element oxitPurchaseOrder = doc.createElement("oxit:PurchaseOrder");
        oxitDataArea.appendChild(oxitPurchaseOrder);

        Element oxitPurchaseOrderHeader = doc.createElement("oxit:PurchaseOrderHeader");
        oxitPurchaseOrder.appendChild(oxitPurchaseOrderHeader);

        Element oagDocumentID = doc.createElement("oag:DocumentID");
        oxitPurchaseOrderHeader.appendChild(oagDocumentID);
        // Customer Order number
        Element oagID = doc.createElement("oag:ID");
        oagID.appendChild(doc.createTextNode(documentNo));
        oagDocumentID.appendChild(oagID);

        // order last update date
        Element oagLastModificationDateTime = doc.createElement("oag:LastModificationDateTime");
        oagLastModificationDateTime.appendChild(doc.createTextNode(updated));
        oxitPurchaseOrderHeader.appendChild(oagLastModificationDateTime);

        // order creation date
        Element oagDocumentDateTime = doc.createElement("oag:DocumentDateTime");
        oagDocumentDateTime.appendChild(doc.createTextNode(created));
        oxitPurchaseOrderHeader.appendChild(oagDocumentDateTime);

        // I for Implantation order (first one), R for replenishment
        Element oagDescription = doc.createElement("oag:Description");
        oagDescription.appendChild(doc.createTextNode("O"));
        oxitPurchaseOrderHeader.appendChild(oagDescription);

        Attr type = doc.createAttribute("type");
        type.setValue("PurchaseOrderType");
        oagDescription.setAttributeNode(type);

        Element oagStatus = doc.createElement("oag:Status");
        oxitPurchaseOrderHeader.appendChild(oagStatus);

        Element oagCode = doc.createElement("oag:Code");
        oagCode.appendChild(doc.createTextNode(orderStatus));
        oagStatus.appendChild(oagCode);

        // Customer Party should be decathlon
        Element oagCustomerParty = doc.createElement("oag:CustomerParty");
        oxitPurchaseOrderHeader.appendChild(oagCustomerParty);

        Element oagPartyIDs = doc.createElement("oag:PartyIDs");
        oagCustomerParty.appendChild(oagPartyIDs);

        // if EAN ok then the one taken ortherwise 3 next fields
        Element oagID2 = doc.createElement("oag:ID");
        oagID2.appendChild(doc.createTextNode("3020913899741"));// Decathlon EAN code
        oagPartyIDs.appendChild(oagID2);

        Attr schemeName2 = doc.createAttribute("schemeName");
        schemeName2.setValue("EAN");
        oagID2.setAttributeNode(schemeName2);

        // Supplier Party should be Supplier the Order is being
        // Procured.
        Element oxitSupplierParty = doc.createElement("oxit:SupplierParty");
        oxitPurchaseOrderHeader.appendChild(oxitSupplierParty);

        Element oagPartyIDs2 = doc.createElement("oag:PartyIDs");
        oxitSupplierParty.appendChild(oagPartyIDs2);

        // if EAN ok then the one tacken ortherwise 3 next fields
        Element oagID7 = doc.createElement("oag:ID");
        oagID7.appendChild(doc.createTextNode(suppliercode));
        oagPartyIDs2.appendChild(oagID7);

        Attr schemeName7 = doc.createAttribute("schemeName");
        schemeName7.setValue("EAN");
        oagID7.setAttributeNode(schemeName7);

        // Ship to party should be decathlon
        Element oagShipToParty = doc.createElement("oag:ShipToParty");
        oxitPurchaseOrderHeader.appendChild(oagShipToParty);

        Element oagPartyIDs21 = doc.createElement("oag:PartyIDs");
        oagShipToParty.appendChild(oagPartyIDs21);

        // if EAN ok then the one tacken ortherwise 3 next fields
        Element oagID71 = doc.createElement("oag:ID");
        oagID71.appendChild(doc.createTextNode("3020913899741"));
        oagPartyIDs21.appendChild(oagID71);

        Attr schemeName72 = doc.createAttribute("schemeName");
        schemeName72.setValue("EAN");
        oagID71.setAttributeNode(schemeName72);

        // Contractual Delivery Date
        Element oagRequiredDeliveryDateTime = doc.createElement("oag:RequiredDeliveryDateTime");
        oagRequiredDeliveryDateTime.appendChild(doc.createTextNode(em_sw_expdeldate));
        oxitPurchaseOrderHeader.appendChild(oagRequiredDeliveryDateTime);

        // Expected Delivery Date
        Element oagScheduledDeliveryDateTime = doc.createElement("oag:ScheduledDeliveryDateTime");
        oagScheduledDeliveryDateTime.appendChild(doc.createTextNode(datepromised));
        oxitPurchaseOrderHeader.appendChild(oagScheduledDeliveryDateTime);
        // createXML(doc, bodidIncrementer);

        String linesSql = "SELECT distinct m.name as  name, l.qtyordered as qtyordered,l.line,"
            + " (select iso_code from c_currency where c_currency_id=h.em_sw_currency) as iso_code,l.em_sw_fob as em_sw_fob,l.em_sw_suppliercode"
            + " from c_order h,c_orderline l,m_product m  where m.m_product_id=l.m_product_id"
            + " and h.c_order_id=l.c_order_id and h.documentno='" + documentNo
            + "' and h.c_doctype_id ='C7CD4AC8AC414678A525AB7AE20D718C' order by l.line";

        SQLQuery queryLine = OBDal.getInstance().getSession().createSQLQuery(linesSql);
        List<Object[]> recorLinedList = queryLine.list();

        /*
         * pst = con.prepareStatement(linesSql); rs2 = pst.executeQuery();
         */

        int LineID = 1;

        if (recorLinedList.size() <= 0) {
          logger.info(
              "No Line Record Found for Flex Process and Order Count is: " + recorLinedList.size());
        }

        for (Object[] rs2 : recordList) {
          String name = null;
          String qtyordered = null;
          String iso_code = null;
          String em_sw_fob = null;

          if (rs2[0] != null) {
            name = rs2[0].toString();
          }
          if (rs2[1] != null) {
            qtyordered = rs2[1].toString();
          }
          if (rs2[3] != null) {
            iso_code = rs2[3].toString();
          }
          if (rs2[4] != null) {
            em_sw_fob = rs2[4].toString();
          }

          Element oxitPurchaseOrderLine = doc.createElement("oxit:PurchaseOrderLine");
          oxitPurchaseOrder.appendChild(oxitPurchaseOrderLine);

          // line number of the order
          Element oagLineNumber = doc.createElement("oag:LineNumber");
          oagLineNumber.appendChild(doc.createTextNode(Integer.toString(LineID)));
          oxitPurchaseOrderLine.appendChild(oagLineNumber);
          LineID++;

          Element oagStatus1 = doc.createElement("oag:Status");
          oxitPurchaseOrderLine.appendChild(oagStatus1);

          // A : active, C : Closed, D : Deleted NV : New Order
          Element oagCode1 = doc.createElement("oag:Code");
          oagCode1.appendChild(doc.createTextNode("A"));
          oagStatus1.appendChild(oagCode1);

          Element oagItem = doc.createElement("oag:Item");
          oxitPurchaseOrderLine.appendChild(oagItem);

          Element oagItemID = doc.createElement("oag:ItemID");
          oagItem.appendChild(oagItemID);

          // Decathlon external item code
          Element oagID11 = doc.createElement("oag:ID");
          oagID11.appendChild(doc.createTextNode(name));
          oagItemID.appendChild(oagID11);

          Attr schemeName11 = doc.createAttribute("schemeName");
          schemeName11.setValue("itemCode");
          oagID11.setAttributeNode(schemeName11);

          Attr schemeAgencyName = doc.createAttribute("schemeAgencyName");
          schemeAgencyName.setValue("DECATHLON");
          oagID11.setAttributeNode(schemeAgencyName);

          Element oagQuantity = doc.createElement("oag:Quantity");
          oagQuantity.appendChild(doc.createTextNode(qtyordered));
          oxitPurchaseOrderLine.appendChild(oagQuantity);

          // Selling unit / Qty of item (selling unit)
          Attr unitCode = doc.createAttribute("unitCode");
          unitCode.setValue("PCE");
          oagQuantity.setAttributeNode(unitCode);

          Element oxitPurchasedUnitPrice = doc.createElement("oxit:PurchasedUnitPrice");
          oxitPurchaseOrderLine.appendChild(oxitPurchasedUnitPrice);

          Element oagAmount = doc.createElement("oag:Amount");
          oagAmount.appendChild(doc.createTextNode(em_sw_fob));
          oxitPurchasedUnitPrice.appendChild(oagAmount);

          Attr currencyID = doc.createAttribute("currencyID");
          currencyID.setValue(iso_code);
          oagAmount.setAttributeNode(currencyID);

          Element oagPerQuantity = doc.createElement("oag:PerQuantity");
          oagPerQuantity.appendChild(doc.createTextNode(em_sw_fob));
          oxitPurchasedUnitPrice.appendChild(oagPerQuantity);

          Attr unitCode1 = doc.createAttribute("unitCode");
          unitCode1.setValue("PCE");
          oagPerQuantity.setAttributeNode(unitCode1);

        } // inner while
        logger.info("Supply Code is: " + supplierCode);
        // Call createXML to write the XML to file
        String getFile = createXML(doc, xmlid);

        // add the file names to filenames List
        fileNames.add(getFile);

        // below method commented after issue with network which donot FTP file and update
        // status
        // Check FTP connection
        // checkFTPConnection(fileNames, supplierCode);

        // push files to FTP and Call updateStatus to update the status
        // if(pushFileToFTP(fileNames, supplierCode)){
        logger.info("Sending files to FTP");

        boolean flag = UploadRecursiveFolderToSFTPServer.pushedFileToNewSFTPLocaltion(sourceFolder,
            sftpDestFolder, fileNames);
        if (flag) {
          try {
            UpdateXMLID(xmlid, supplierCode);
            updateStatus(documentNo, orderActionCode);
          } catch (Exception e) {
          }
        }

        deleteFiles(fileNames);

        /*
         * if (pushFileToFTPModified(getFile, supplierCode)) {
         * updateStatus(rs.getString("documentno"), orderActionCode); }
         */
      } // outer while}

      // Call transferFolder method to FTPS the files to remote location
      // transferFolder(fileNames);

    } // end try
    catch (Exception e) {
      logger.error("Exception in Query " + e);
    } /*
       * finally { try { if (con != null) { con.close(); }
       * 
       * 
       * if (pst != null) pst.close(); if (rs != null) rs.close(); if (rs2 != null) rs2.close();
       * 
       * } catch (SQLException e) { logger.info("Could not close connections " + e);
       * e.printStackTrace(); } }
       */

    logger.info("********* Flex Process is Successfully Run Completed***************");

  }

  private static String getIncrementValue(String xmlidLocal) {
    logger.info("xmlidLocal: " + xmlidLocal);
    String xmlidnew = "";
    try {
      long test = Long.parseLong(xmlidLocal);
      test++;
      xmlidnew = Long.toString(test);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return xmlidnew;
  }

  private static void UpdateXMLID(String XMLID, String SupplierCode) {
    try {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      // get current date time with Date()
      Date date = new Date();

      String updateXMLSeqNo = "UPDATE cl_dpp_seqno set xmlseqno='" + XMLID + "' ,updated='"
          + dateFormat.format(date) + "' where supplierno='" + SupplierCode + "' ;";

      OBDal.getInstance().getSession().createSQLQuery(updateXMLSeqNo).executeUpdate();
      /*
       * PreparedStatement pstDSTracking = null; pstDSTracking =
       * con.prepareStatement(updateXMLSeqNo); pstDSTracking.executeUpdate();
       */

      logger.info("sequence updated for supplier : " + SupplierCode + " to " + XMLID);
    } catch (Exception e) {
      logger.info("Exception in updating synchtime  " + e);
    }

  }

  private static String createXML(Document doc, String uniqueID) {
    String filename = null;
    try {

      if (doc != null && uniqueID != null) {
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        // filename = "PPO_3020913899741_" + uniqueID + ".xml";
        filename = "PPO_" + supplierCode + "_" + uniqueID + ".xml";
        StreamResult xmlresult = new StreamResult(new File(sourceFolder + filename));

        transformer.transform(source, xmlresult);
        logger.info("Purchase Order Created  : " + filename);

      } else {
        logger.info("Critical Alert : No Document Found!!");
      }

    } // file writer try
    catch (Exception e2) {
      logger.info("XML Creation Failed for :");
      filename = null;
    }
    return filename;
  }

  public static String getXMLID(String SupplierCode) {
    String localxmlid = null;
    try {

      /*
       * ConnectiontoDB connection = new ConnectiontoDB(); PreparedStatement pst = null
       */;

      String getxmlid = "SELECT xmlseqno as xmlseqno, supplierno as supplierno From cl_dpp_seqno where supplierno='"
          + SupplierCode + "' ;";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(getxmlid);
      List<Object[]> recordList = query.list();

      /*
       * con = connection.getConnection(appConfigFilePath); pst = con.prepareStatement(getxmlid);
       * 
       * ResultSet docSeqResult = pst.executeQuery();
       */

      for (Object[] rs : recordList) {
        String xmlseqno = null;

        if (rs[0] != null) {
          xmlseqno = rs[0].toString();
        }
        localxmlid = xmlseqno;

      }
    } catch (Exception xmlSeqNo) {
      logger.info("Exception in getting seqno: " + xmlSeqNo);
    }

    return localxmlid;
  }

  private static void updateStatus(String documentNo, String documentStatus) {
    String doctStatus = null;
    try {

      if (documentStatus.equals("Add")) {
        doctStatus = "OS";
      }
      if (documentStatus.equals("Update")) {
        doctStatus = "OU";
      }
      if (documentStatus.equals("Delete")) {
        doctStatus = "OC";
      }
      if (documentStatus.equals("Close")) {
        doctStatus = "OCL";
      }
      String updateDocStatus = "update c_order set em_sw_postatus='" + doctStatus + "'"
          + " where documentno='" + documentNo + "' ;";

      OBDal.getInstance().getSession().createSQLQuery(updateDocStatus).executeUpdate();
      /*
       * PreparedStatement pstDSTracking = null; pstDSTracking =
       * con.prepareStatement(updateDocStatus);
       * 
       * // logger.info("STATUS " + pstDSTracking); // System.exit(0);
       * pstDSTracking.executeUpdate();
       */
      logger.info("Status updated for :" + documentNo + " to " + doctStatus);
    } catch (Exception updateDocStatusException) {
      // logger.info("EXCEPTION " + updateDocStatusException);
      logger.info("Exception in updating Document Status  " + updateDocStatusException
          + " DOCUMENTNO " + documentNo);

    }

  }

  private static void deleteFiles(List<String> fileNames) {
    try {
      for (int i = 0; i < fileNames.size(); i++) {
        File file = new File(sourceFolder + fileNames.get(i));
        // logger.info("FILES " + sourceFolder + fileNames.get(i));
        file.delete();
        logger.info("File deleted from:" + file.getAbsolutePath());
      }
    } catch (Exception e) {
      logger.info("Error in deleting files due to " + e.getMessage());
    }

  }

  @SuppressWarnings("unused")
  public void processServerData(JSONObject json) throws Exception {
    boolean organizationInfo = false;

    try {
      // Getting Organization details from supply's response
      final JSONArray organizationJsonArray = (JSONArray) JSONHelper
          .getContentAsJSON(json.toString());
      log.debug("Organizations in json  " + organizationJsonArray);
      monitorLogger.log(" Total Organizations " + organizationJsonArray.length());

      /*
       * final JSONArray orgTypeJsonArray = (JSONArray) getOtherMasters(json.toString(),
       * "OrganizationType"); log.debug("Organization type in json " + orgTypeJsonArray);
       * logger.log(" Total OrganizationType " + orgTypeJsonArray.length());
       */

      final JSONArray clientJsonArray = (JSONArray) getOtherMasters(json.toString(), "Client");
      log.debug("Client in json " + clientJsonArray);
      monitorLogger.log(" Total Clients " + clientJsonArray.length());

      final JSONArray generalLedgerJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "GeneralLedger");
      log.debug("general ledger in json " + generalLedgerJsonArray);
      monitorLogger.log(" Total general ledger " + generalLedgerJsonArray.length());

      final JSONArray organizationInfoJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "OrganizationInformation");
      log.debug("Organization Info in json " + organizationInfoJsonArray);
      monitorLogger.log(" Total Organization Info in json " + organizationInfoJsonArray.length());

      final JSONArray bpLocationJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "bpLocation");
      log.debug("Business partner location in json " + bpLocationJsonArray);
      monitorLogger.log("Total bpLocation in json " + bpLocationJsonArray.length());

      final JSONArray bPartnerCatJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "BPCategory");
      log.debug(" BPCategory in json " + bPartnerCatJsonArray);
      monitorLogger.log(" Total bpCategory in json " + bPartnerCatJsonArray);

      final JSONArray bPartnerJsonArray = (JSONArray) getOtherMasters(json.toString(), "BPartner");
      log.debug(" bPartner in json " + bPartnerJsonArray);
      monitorLogger.log(" Total bPartner in json" + bPartnerJsonArray.length());

      final JSONArray locationJsonArray = (JSONArray) getOtherMasters(json.toString(), "Location");
      log.debug(" Location in json " + locationJsonArray);
      monitorLogger.log(" Total location in json" + locationJsonArray.length());

      final JSONArray contactJsonArray = (JSONArray) getOtherMasters(json.toString(), "Contact");
      log.debug(" Contact in json " + contactJsonArray);
      monitorLogger.log(" Total no of contacts in json" + contactJsonArray);

      final JSONArray companyImageJsonArray = (JSONArray) getOtherMasters(json.toString(),
          "CompanyImage");
      log.debug(" Company Image in json " + companyImageJsonArray);
      monitorLogger.log(" Total no of companyImage in json" + companyImageJsonArray.length());

      JSONHelper.saveJSONObject(clientJsonArray, monitorLogger);
      JSONHelper.saveJSONObject(generalLedgerJsonArray, monitorLogger);
      OBContext.setAdminMode(true);
      JSONHelper.saveJSONObject(organizationJsonArray, monitorLogger);
      OBContext.restorePreviousMode();

      JSONHelper.saveJSONObject(locationJsonArray, monitorLogger);
      JSONHelper.saveJSONObject(contactJsonArray, monitorLogger);
      JSONHelper.saveJSONObject(bPartnerCatJsonArray, monitorLogger);
      JSONHelper.saveJSONObject(bPartnerJsonArray, monitorLogger);

      OBContext.setAdminMode(true);
      JSONHelper.saveJSONObject(organizationInfoJsonArray, monitorLogger);
      OBContext.restorePreviousMode();

      JSONObject lastOrganizationInfo = organizationInfoJsonArray
          .getJSONObject(organizationInfoJsonArray.length() - 1);

      String LastUpdatedTime = lastOrganizationInfo.getString("updatedTime");

      SimpleDateFormat readFormat = new SimpleDateFormat("EE MMM dd hh:mm:ss z yyyy");

      Date date = null;

      date = readFormat.parse(LastUpdatedTime);

      SimpleDateFormat writeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      String formattedDate = "";
      if (date != null) {
        formattedDate = writeFormat.format(date);
      }

      short updatedRow = (short) BusinessEntityMapper.setLastUpdatedTime(formattedDate,
          "Organization");

    } catch (Exception e) {
      log.error(e);
      if (e.getMessage().contains("JSONObject[\"data\"] not found"))
        monitorLogger.log("Supply failed to respond");
      monitorLogger.log(e.getMessage());
      e.printStackTrace();

    }
    log.info("Final result " + organizationInfo);
    monitorLogger.log("Final result" + organizationInfo);

  }

  private Object getOtherMasters(String content, String master) throws JSONException {
    Check.isNotNull(content, "Content must be set");
    Object jsonMasterList = null;
    JSONObject jsonObj = new JSONObject(content);
    jsonMasterList = jsonObj.get(master);
    return jsonMasterList;
  }

}
