package in.decathlon.ibud.masters.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.masters.data.IbudServerTime;

public class FlexSyncMasterForCloseOrder extends DalBaseProcess {
  private static ProcessLogger monitorLogger;
  private static final Logger log = Logger.getLogger(FlexSyncMasterForCloseOrder.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    monitorLogger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try {
      OBContext.setAdminMode(true);

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      log.debug("Inside MasterSyncClient class to GET master data");
      String updatedDate = getUpdatedTime("FlexProcess", 2);
      monitorLogger
          .log("Requesting Supply to get Closed Order data from Date: " + updatedDate + " \n");
      sendCloseOrderTOSFTP(updatedDate);
      setLastUpdatedTime("FlexProcess");
    } catch (Exception e) {
      BusinessEntityMapper.rollBackNlogError(e, processid, null);
      e.printStackTrace();
      log.error(e);
      monitorLogger.log("Error is: " + e.getMessage() + " \n");
    } finally {
      OBContext.restorePreviousMode();

    }

    // processServerData(jsonObj);

  }

  public static int setLastUpdatedTime(String serviceKey) {
    String qry = "update Ibud_ServerTime set lastupdated = now() where serviceKey= :serviceKey and client='"
        + OBContext.getOBContext().getCurrentClient().getId() + "'";
    log.info("executing " + qry);
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("serviceKey", serviceKey);
    int rowUpdated = query.executeUpdate();
    log.info("executed row=" + rowUpdated);
    return rowUpdated;
  }

  private String getUpdatedTime(String serviceKey, int lastUpdatedDays) throws ParseException {
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
      // SessionHandler.getInstance().commitAndStart();
      return lastUpdatedTime1.toString().replaceAll(" ", "_");

    }
  }

  private void sendCloseOrderTOSFTP(String idOrTime) {

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setNamespaceAware(true);
    List<String> fileNames = new ArrayList<String>();
    Element rootElement = null;
    Map<String, Map<String, String>> OrderMap = new HashMap<String, Map<String, String>>();
    try {

      String sql = " SELECT distinct h.em_sw_postatus as postatus, h.documentno as documentNo,to_char(h.created,'YYYY-mm-dd') as created,to_char(h.updated,'YYYY-mm-dd') as updated,"
          + " to_char(h.dateordered,'YYYY-mm-dd') as dateordered,to_char(h.datepromised,'YYYY-mm-dd') as datepromised,to_char(h.em_sw_expdeldate,'YYYY-mm-dd') as em_sw_expdeldate,"
          + " l.em_sw_suppliercode as suppliercode ,cd.xmlseqno as seqno"
          + " from c_order h,c_orderline l,c_bpartner bp ,cl_dpp_seqno cd where h.c_order_id=l.c_order_id "
          + " and bp.em_cl_supplierno=l.em_sw_suppliercode " + " and bp.em_rc_source='DPP' "
          + " and h.em_sw_postatus in ('OCL') and bp.em_cl_supplierno=cd.supplierno "
          + " and h.c_doctype_id ='C7CD4AC8AC414678A525AB7AE20D718C' and h.em_imsap_duplicatesap_po!='Y' and h.updated >= '"
          + idOrTime + "'";

      // ConnectiontoDB connection = new ConnectiontoDB();
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      List<Object[]> recordList = query.list();

      if (recordList.size() <= 0) {
        monitorLogger.log(
            "No Record Found for Flex Process and Order Count is: " + recordList.size() + " \n");
        return;
      } else {
        monitorLogger.log("Close Order getting and Count is: " + recordList.size() + " \n");
      }

      for (Object[] rs : recordList) {
        String postatus = null;
        String documentNo = null;
        String created = null;
        String updated = null;
        String datepromised = null;
        String em_sw_expdeldate = null;
        String supplierCode = null;
        String xmlid = null;
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
          supplierCode = rs[7].toString();
        }

        if (rs[8] != null) {
          xmlid = rs[8].toString();
        }

        /*
         * if (postatus.equals("SO")) { orderActionCode = "Add"; orderStatus = "NV"; } else if
         * (postatus.equals("MO")) { orderActionCode = "Update"; orderStatus = ""; } else if
         * (postatus.equals("OCL")) {
         */
        String orderActionCode = "Close";
        String orderStatus = "C";
        /*
         * } else { orderActionCode = "Delete"; orderStatus = ""; }
         */

        // monitorLogger.log(" Process Running for Document No: " + documentNo);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

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
        // monitorLogger.log(" supplier Code is: " + supplierCode);
        // Call getXMLID method to retrieve XML ID
        // xmlid = getXMLID(supplierCode);

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
        oagID7.appendChild(doc.createTextNode(supplierCode));
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
          monitorLogger
              .log("No Line Record Found for Flex Process and Order is: " + documentNo + " \n");
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
        // monitorLogger.log(" Supply Code is: " + supplierCode);
        // Call createXML to write the XML to file
        String getFile = createXML(doc, xmlid, supplierCode);

        // add the file names to filenames List
        fileNames.add(getFile);
        // OrderMap.get(documentNo);
        Map<String, String> orderMapObj = new HashMap<String, String>();
        orderMapObj.put("fileName", getFile);
        orderMapObj.put("supplierCode", supplierCode);
        orderMapObj.put("xmlid", xmlid);

        OrderMap.put(documentNo, orderMapObj);
        /*
         * if (pushFileToFTPModified(getFile, supplierCode)) {
         * updateStatus(rs.getString("documentno"), orderActionCode); }
         */
      }
      if (fileNames.size() > 0) {
        // monitorLogger.log(" Sending files to FTP");
        List<String> movedFileNames = pushedFileToNewSFTPLocaltion();

        if (movedFileNames.size() > 0) {
          try {
            monitorLogger.log("moved File Names list is: " + movedFileNames + " \n");

            monitorLogger.log("Order Map is: " + OrderMap + " \n");

            for (Entry<String, Map<String, String>> orderMaoObj : OrderMap.entrySet()) {
              String documentNo = orderMaoObj.getKey();
              Map<String, String> orderDetailsMap = orderMaoObj.getValue();
              if (orderDetailsMap.containsKey("supplierCode")
                  && orderDetailsMap.containsKey("xmlid")
                  && orderDetailsMap.containsKey("fileName")) {
                String supplierCode = orderDetailsMap.get("supplierCode");
                String xmlid = orderDetailsMap.get("xmlid");
                String fileName = orderDetailsMap.get("fileName");
                if (movedFileNames.contains(fileName)) {
                  UpdateXMLID(xmlid, supplierCode);
                  updateStatus(documentNo);
                } else {
                  monitorLogger.log("file not Moved  : " + fileName + " \n");

                }
              } else {
                monitorLogger.log("Key Not found for Document : " + documentNo + " \n");
              }
            }
          } catch (Exception e) {
          }
        }

      }

    } // end try
    catch (Exception e) {
      monitorLogger.log("Exception in Query " + e + " \n");
      e.printStackTrace();
    }

    monitorLogger.log("  ********* Flex Process is Successfully Run Completed*************** \n");

  }

  private static String getIncrementValue(String xmlidLocal) {
    // xmlidLocal: " + xmlidLocal);
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

      Date date = new Date();

      String updateXMLSeqNo = "UPDATE cl_dpp_seqno set xmlseqno='" + XMLID + "' ,updated='"
          + dateFormat.format(date) + "' where supplierno='" + SupplierCode + "' ;";

      OBDal.getInstance().getSession().createSQLQuery(updateXMLSeqNo).executeUpdate();

      monitorLogger.log(
          "  sequence updated for supplier : " + SupplierCode + " and seq No is: " + XMLID + " \n");
    } catch (Exception e) {
      e.printStackTrace();
      monitorLogger.log("  Exception in updating synchtime  " + e + " \n");
    }

  }

  private static String createXML(Document doc, String uniqueID, String supplierCode) {
    String filename = null;
    try {

      if (doc != null && uniqueID != null) {
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        // filename = "PPO_3020913899741_" + uniqueID + ".xml";
        filename = "PPO_" + supplierCode + "_" + uniqueID + ".xml";
        StreamResult xmlresult = new StreamResult(
            new File(IbudConfig.getSourceFolder() + filename));

        transformer.transform(source, xmlresult);
        monitorLogger.log("Purchase Order Created: " + filename + " \n");

      } else {
        monitorLogger.log("Critical Alert : No Document Found!! \n");
      }

    } // file writer try
    catch (Exception e2) {
      monitorLogger.log("Error is XML Creation Failed for :" + e2 + " \n");
      filename = null;
    }
    return filename;
  }

  private static void updateStatus(String documentNo) {
    try {

      String updateDocStatus = "update c_order set em_sw_postatus='CL'" + " where documentno='"
          + documentNo + "' ;";

      OBDal.getInstance().getSession().createSQLQuery(updateDocStatus).executeUpdate();

      monitorLogger.log("  Status updated for :" + documentNo + " as Closed order \n");
    } catch (Exception updateDocStatusException) {
      // monitorLogger.log(" EXCEPTION " + updateDocStatusException);
      monitorLogger.log("  Exception in updating Document Status  " + updateDocStatusException
          + " DOCUMENTNO " + documentNo + " \n");
    }

  }

  public static List<String> pushedFileToNewSFTPLocaltion() {
    int SFTPPort = 22; // SFTP Port Number
    List<String> fileNames = new ArrayList<String>();

    Session session = null;
    Channel channel = null;
    ChannelSftp channelSftp = null;
    try {
      String SFTPHost = IbudConfig.getSftpHost();
      String SFTPUser = IbudConfig.getSftpUserName();
      String SFTPPass = IbudConfig.getSftpPassword();
      String movedFolder = IbudConfig.getMoveFolder();
      String sourceDir = IbudConfig.getSourceFolder();
      String SFTPDescDir = IbudConfig.getSftpDestFolder();
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

      try {
        File sourceFileDir = new File(sourceDir);
        File movedDir = new File(movedFolder);

        // this is move file, source to move folder

        // copy the all file move folder to sftp
        File[] files = sourceFileDir.listFiles(new FilenameFilter() {
          @Override
          public boolean accept(@SuppressWarnings("hiding") File sourceDir, String name) {
            return name.endsWith(".xml");
          }
        });

        for (File sourceFile : files) {
          // copy if it is a file
          channelSftp.cd(SFTPDescDir);
          if (sourceFile.getName().endsWith(".xml")) {
            channelSftp.put(new FileInputStream(sourceFile), sourceFile.getName(),
                ChannelSftp.OVERWRITE);

            File file = new File(sourceDir + sourceFile.getName());
            fileNames.add(sourceFile.getName());
            // Move file to new directory
            boolean success = file.renameTo(new File(movedDir, file.getName()));
            if (!success) { // File was not successfully moved
              // logger.error("File not moved " + file.getAbsolutePath());
            } else {
              // logger.info("File moved " + file.getAbsolutePath());
            }

          } else {
            // logger.error("XML file is not present on location " + sourceFile);
          }
        }
      } catch (Exception ex) {
        // logger.error("Error while transfer File " + movedFolder + " to " + SFTPDescDir);

        ex.printStackTrace();

      }

    } catch (SftpException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSchException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
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
    return fileNames;

  }

}
