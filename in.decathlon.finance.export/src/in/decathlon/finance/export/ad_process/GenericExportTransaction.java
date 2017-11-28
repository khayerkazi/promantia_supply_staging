package in.decathlon.finance.export.ad_process;

import in.decathlon.finance.export.AttributeMap;
import in.decathlon.finance.export.DocumentSection;
import in.decathlon.finance.export.GLCategory;
import in.decathlon.finance.export.MapDefinitionHeader;
import in.decathlon.finance.export.NodeMap;
import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.journalentry.CompanyAccounting;
import in.decathlon.journalentry.LedgerTransaction;
import in.decathlon.journalentry.LedgerTransactionLeg;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.sysfore.sankalpcrm.RCCompany;

public class GenericExportTransaction extends DalBaseProcess {

  private static final String VOUCHER_NODE = "V";
  private static final String EXISTING_MASTERS_TXN_TYPE = "M";
  private static final String VENDOR_TXN_TYPE = VOUCHER_NODE;
  private static final String CUST_TXN_TYPE = "C";
  private static final String PURCHASE_RETURN_TXN_TYPE = "PR";
  private static final String PURCHASE_TXN_TYPE = "P";
  private static final String SALES_RETURN_TXN_TYPE = "SR";
  private static final String SALES_RETURN_TXN_TYPE_POS = "SRP";
  private static final String SALES_TXN_TYPE_ECOM = "S";
  private static final String SALES_TXN_TYPE_POS = "SP";
  private static final String PARAM_IS_FULL_DOWNLOAD = "fulldownload";
  private static final String PARAM_NO_OF_RECS = "numrecords";
  private static final String PARAM_END_DATE = "enddate";
  private static final String PARAM_START_DATE = "startdate";
  private static final String PARAM_TRANSACTION_TYPE = "transactionType";
  private static final String APPAREA_NODE = "H";

  Logger log4j = Logger.getLogger(getClass());

  private final String TITLE = "Eone Export";
  private boolean incremental = true;
  private Date startDate = null;
  private Date endDate = new Date();
  private String fullFileName;
  private String transactionType;
  private XPath xpath;
  private Node cloneVoucher, cloneLedger, cloneBill, cloneCost, cloneName, cloneAddr;
  private String parentVoucher, parentLedger, parentBill, parentCost, parentAddr, parentName;
  private DocumentSection ledger, name, address, appArea;
  private MapDefinitionHeader header;
  private Document doc = null;
  private int currentLegLineNo;
  private Node appAreaNode, costNode;
  private String shortFileName;

  JSONObject errorObject = new JSONObject();

  /*
   * This method is used for scheduling the generation and export of the xml files. The FTP host and
   * credentials to be predefined in the openbravo.properties file
   */

  /**
   * @param bundle
   * @param incremental
   * @param transcationType
   * @param xmlLocation
   * @throws JSONException
   */
  public void GenericExportTransactionSchedule(ProcessBundle bundle, Boolean incremental,
      String transcationType, String xmlLocation) throws IOException, JSONException {

    String webDirName = getWebDirName();
    int nr = -1;
    this.transactionType = transcationType;
    this.incremental = incremental;
    String format = bundle.getContext().getJavaDateFormat();
    if ((format == null) || format.trim().equals("")) {
      format = "dd-MM-yyyy";
    }
    String fileType = getFileType();
    String fileName = new SimpleDateFormat("yyyyMMdd-hhmm").format(new Date()) + "-" + fileType
        + ".xml";
    fullFileName = webDirName + "/I-" + fileName;
    shortFileName = "/I-" + fileName;
    OBError result = null;
    try {
      result = exportTransactions(nr, bundle);
    } catch (Exception e) {
      System.out.println("Exception at exportTransaction in constructor");
      // OBDal.getInstance().rollbackAndClose();
      log4j.error(e);
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle(TITLE);
      bundle.setResult(msg);
      BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), null);
    }

    if (result.getType().equals("Success")) {
      result.setMessage(result.getMessage() + " Download from " + getDownloadUrl());
    }

    OBDal.getInstance().commitAndClose();
    if (result.getType().equals("Success")) {
    writeXmlFile(fullFileName);

    /*
     * Export the generated XML file through FTP
     */

    new FtpEone().uploadEone(fullFileName, fileName, xmlLocation);
    copyFile(fullFileName, shortFileName);
    }
    bundle.setResult(result);

  }

  /*
   * This is extra feature which helps us to generate the XML file manually i.e without scheduled
   * process. The FTP method has been commented out.
   */

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    try {
      log4j.info("Started Export Transaction with " + bundle.getParamsDeflated());
      String webDirName = getWebDirName();
      int nr = -1;

      if (bundle.getParams().get(PARAM_TRANSACTION_TYPE) != null) {
        this.transactionType = (String) bundle.getParams().get(PARAM_TRANSACTION_TYPE);
      }

      String fileType = getFileType();
      String fileName = new SimpleDateFormat("yyyyMMdd-hhmm").format(new Date()) + "-" + fileType
          + ".xml";

      String format = bundle.getContext().getJavaDateFormat();
      if ((format == null) || format.trim().equals("")) {
        format = "dd-MM-yyyy";
      }

      if ((bundle.getParams().get(PARAM_START_DATE) != null)
          && (!((String) bundle.getParams().get(PARAM_START_DATE)).trim().equals(""))) {
        startDate = new SimpleDateFormat(format).parse((String) bundle.getParams().get(
            PARAM_START_DATE));
      }

      if ((bundle.getParams().get(PARAM_END_DATE) != null)
          && (!((String) bundle.getParams().get(PARAM_END_DATE)).trim().equals(""))) {
        endDate = new SimpleDateFormat(format).parse((String) bundle.getParams()
            .get(PARAM_END_DATE));
      }

      if ((bundle.getParams().get(PARAM_NO_OF_RECS) != null)
          && (!((String) bundle.getParams().get(PARAM_NO_OF_RECS)).trim().equals(""))) {
        nr = Integer.parseInt((String) bundle.getParams().get(PARAM_NO_OF_RECS));
      }

      if ((bundle.getParams().get(PARAM_IS_FULL_DOWNLOAD) != null)
          && ((String) bundle.getParams().get(PARAM_IS_FULL_DOWNLOAD)).equals("Y")) {
        incremental = false;
        fullFileName = webDirName + "/F-" + fileName;
        shortFileName = "/F-" + fileName;

      } else {
        fullFileName = webDirName + "/I-" + fileName;
        shortFileName = "/I-" + fileName;
        incremental = true;
      }

      // nr == -1 indicates no limit on no. of transactions
      OBError result = exportTransactions(nr, bundle);
      if (result.getType().equals("Success")) {
        result.setMessage(result.getMessage() + " Download from " + getDownloadUrl());
      }

      OBDal.getInstance().commitAndClose();
      if (result.getType().equals("Success")) {
      writeXmlFile(fullFileName);
      copyFile(fullFileName, shortFileName);
      }
      // new FtpEone().uploadEone(fullFileName,fileName); /* This line
      // supports for FTP file sending*/
      bundle.setResult(result);

    } catch (Exception e) {
      // OBDal.getInstance().rollbackAndClose();
      log4j.error(e);
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle(TITLE);
      bundle.setResult(msg);
      BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), errorObject);

    }
  }

  private void copyFile(String filename, String sfilename) {
    try {

      String destFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
          "movedFolder");

      File file = new File(filename);
      // file.renameTo(new File(destFolder+sfilename));
      Files.copy(file.toPath(), new File(destFolder + sfilename).toPath());
    } catch (Exception e) {
      log4j.error("Exception while moving the file to destination folder in local server.");
      log4j.error(e.getMessage());
    }
  }

  /*
   * The xml creation process starts from here.
   */

  private OBError exportTransactions(int nr, ProcessBundle bundle) throws Exception {
    List<LedgerTransaction> txnList = null;
    List<RCCompany> compList;

    OBError result = new OBError();
    result.setTitle(TITLE);
    //result.setType("Success");
    result.setType("Info");
    result.setMessage("No transactions to export");
    
    header = getLatestHeader();

    /* For e-com XML files this condition */

    if (this.transactionType.equals(SALES_TXN_TYPE_ECOM)) {
      // Initialise the XML file - clone and delete all recurring nodes
      initializeECOMXML();
      ArrayList<String> glCategoryList = getGLCategory();
      txnList = getTransactionList(glCategoryList);
      log4j.info("Ledger transactions " + txnList);

      if (txnList.size() == 0) {
        addNoRecordsError(parentVoucher);
        result.setType("Info");
        result.setMessage("No transactions to export");
        return result;
      }
      processAppArea();
      LedgerTransaction txn;

      // Set nr to number of records if nr == -1
      if (nr == -1 || nr > txnList.size()) {
        nr = txnList.size();
      }
      for (int i = 0; i < nr; i++) {
        txn = txnList.get(i);

        if (txn.getTXNSource().equals("ECOM")) {
          // log4j.info("Processing ECOM txn " + txn.getDocumentNo());
          errorObject.put(SOConstants.RECORD_ID, txn.getId());

          if ((txn.getDfexThirdNum() == null) || (txn.getPartnerreference() == null)) {

            Exception e = new Exception("Third Num or Partnerrefernce  is not assigned " + txn);
            BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), errorObject);
            OBDal.getInstance().flush();
            continue;
          } else {
            boolean isLegOk = false;
            // isLegOk = getLedgerDocumentNum(txn);
            List<LedgerTransactionLeg> legLineList = txn.getDJELedgerTxnLegList();
            for (LedgerTransactionLeg leg : legLineList) {
              if ((leg.getCostcenter() == null)
                  || (!leg.getLTLType().equals("O") && leg.getLedgerName() == null)) {
                Exception e = new Exception("Costcenter or ledgername  is not assigned " + txn);
                BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), errorObject);
                OBDal.getInstance().flush();
                isLegOk = true;
                continue;
              }

            }
            if (isLegOk == false) {
              processTransaction(txn);
              result.setType("Success");
              result.setMessage("Successfully exported the transactions.");

            }
          }
        }
      }
    }
    /* For Masters XML file this condition */
    else if (this.transactionType.equals(CUST_TXN_TYPE)) {
      initializeMastersXML();

      log4j.info("Entered exportTransactions" + CUST_TXN_TYPE);
      compList = getCompanyList();
      // log4j.info("Companies to be exported: " + compList);
      if (compList.size() == 0) {
        addNoRecordsError(parentLedger);
        result.setType("Info");
        result.setMessage("No Companies to export");
        return result;
      }
      int countRecord = 0;
      RCCompany comp;

      // Set nr to number of records if nr == -1
      if (nr == -1 || nr > compList.size()) {
        nr = compList.size();
      }

      for (int i = 0; i < nr; i++) {
        comp = compList.get(i);

        if ((comp.getDfexThirdNum() == null) || (comp.getDfexThirdNum().equals(""))) {
          continue;
        } else {
          countRecord++;
        }
      }
      if (countRecord == 0) {
        addNoRecordsError(parentLedger);
        result.setType("Info");
        result.setMessage("No Companies to export");
        return result;
      }

      processAppArea();

      for (int i = 0; i < nr; i++) {
        comp = compList.get(i);
        errorObject.put(SOConstants.RECORD_ID, comp.getId());

        if ((comp.getDfexThirdNum() == null) || (comp.getDfexThirdNum().equals(""))) {
          continue;
        } else if ((comp.getCompanyName() == null) || (comp.getCompanyName().equals(""))) {
          Exception e = new Exception("company Name  is not assigned " + comp);
          BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), errorObject);
          OBDal.getInstance().flush();
          continue;
        } else {
          log4j.info("Processing customer " + comp);
          processCompany(comp);
          result.setType("Success");
          result.setMessage("Successfully exported the transactions.");
        }
      }
    }
    /* For POS XML file */
    else if (this.transactionType.equals(SALES_TXN_TYPE_POS)) {

      initializePOSXML();
      ArrayList<String> glCategoryList = getGLCategory();
      txnList = getTransactionList(glCategoryList);
      log4j.info("Ledger transactions " + txnList);

      if (txnList.size() == 0) {
        addNoRecordsError(parentVoucher);
        result.setType("Info");
        result.setMessage("No transactions to export");
        return result;
      }
      processAppArea();
      LedgerTransaction txn;

      // Set nr to number of records if nr == -1
      if (nr == -1 || nr > txnList.size()) {
        nr = txnList.size();
      }
      for (int i = 0; i < nr; i++) {
        txn = txnList.get(i);
        log4j.info("Processing txn " + txn);

        /*
         * This section processes the transactions for POS sales
         */
        if (txn.getTXNSource().equals("Store")) {
          errorObject.put(SOConstants.RECORD_ID, txn.getId());

          if (txn.getDocumentNo() == null) {
            continue;
          } else {
            boolean isLegPos = false;
            List<LedgerTransactionLeg> legLineList = txn.getDJELedgerTxnLegList();

            for (LedgerTransactionLeg leg : legLineList) {
              if ((leg.getCostcenter() == null) || (leg.getLedgerName() == null)) {
                Exception e = new Exception("Costcenter or Ledger Name  is not assigned " + leg);
                BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), errorObject);
                OBDal.getInstance().flush();
                isLegPos = true;

                continue;
              }
            }
            if (isLegPos == false) {
              processPosTransaction(txn);
              result.setType("Success");
              result.setMessage("Successfully exported the transactions.");
            }
          }
        }

      }

    }

    //result.setMessage("Successfully exported the transactions.");
    return result;
  }

  private void processAppArea() throws Exception {
    DocumentSection appAreaNode = getEoneHeaderNode(APPAREA_NODE);

    List<NodeMap> defList = appAreaNode.getDFEXNODEMAPList();
    for (NodeMap def : defList) {
      setAppAreaValue(def);

    }

  }

  private void setAppAreaValue(NodeMap def) throws Exception {
    Node element;

    /*
     * Get the nth node with the given tagname. If n = 0, return the last node
     */
    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    if (def.getObproperty() != null) {
      String result = computeValue(def.getObproperty());
      element.setTextContent(result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      if (def.getDFEXATTRIBUTEMAPList().size() == 0) {
        log4j.error("Both default value and OB Property not defined");
      }
    }

  }

  private String computeValue(String obProperty) {
    String result = null;

    if (obProperty.equals("CTS")) {
      result = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
    }

    return result;

  }

  private void initializePOSXML() throws Exception {
    openTemplateFile(header.getTemplate());
    doc.getDocumentElement().normalize();

    // Now read the recurring nodes
    OBCriteria<DocumentSection> nodeCrit = OBDal.getInstance()
        .createCriteria(DocumentSection.class);
    nodeCrit.add(Restrictions.eq(DocumentSection.PROPERTY_DFEXMAPDEFINITION, header));
    if (nodeCrit.list().size() == 0) {
      throw new OBException("Pls configure the sublists");
    }
    DocumentSection voucher, ledger, bill, cost, appArea;
    voucher = ledger = bill = cost = appArea = null;
    for (DocumentSection tnode : nodeCrit.list()) {
      if (tnode.getNodename().equals(VOUCHER_NODE)) {
        voucher = tnode;
      } else if (tnode.getNodename().equals("L")) {
        ledger = tnode;
      } else if (tnode.getNodename().equals("B")) {
        bill = tnode;
      } else if (tnode.getNodename().equals("C")) {
        cost = tnode;
      } else if (tnode.getNodename().equals("H")) {
        appArea = tnode;
      } else {
        throw new OBException("Incorrect sublist " + tnode.getNodename());
      }
    }
    if (appArea != null) {
      appAreaNode = getNode(voucher.getNodePath());
    }

    // TODO: Hardcoding the nodes here due to an issue in defining parent
    // node
    if (voucher == null) {
      throw new OBException("Mandatory Xpath node Voucher not defined in Setup");
    }
    Node voucherNode = getNode(voucher.getNodePath());

    if (ledger == null) {
      throw new OBException("Mandatory Xpath node Voucher not defined in Setup");
    }
    Node ledgerNode = null;
    ledgerNode = getNode(ledger.getNodePath());
    Node billNode = null;
    if (bill != null) {
      billNode = getNode(bill.getNodePath());
      parentBill = getXpath(billNode.getParentNode());

    }
    Node costNode = null;
    if (cost != null) {
      costNode = getNode(cost.getNodePath());
      parentCost = getXpath(costNode.getParentNode());

    }
    parentVoucher = getXpath(voucherNode.getParentNode());
    parentLedger = getXpath(ledgerNode.getParentNode());
    if (cost != null) {

      cloneCost = costNode.cloneNode(true);
      costNode.getParentNode().removeChild(costNode);
    }
    if (bill != null) {

      cloneBill = billNode.cloneNode(true);
      billNode.getParentNode().removeChild(billNode);
    }
    cloneLedger = ledgerNode.cloneNode(true);
    ledgerNode.getParentNode().removeChild(ledgerNode);
    cloneVoucher = voucherNode.cloneNode(true);
    voucherNode.getParentNode().removeChild(voucherNode);

    /*
     * cloneLedger = ledgerNode.cloneNode(true); ledgerNode.getParentNode().removeChild(ledgerNode);
     * cloneVoucher = voucherNode.cloneNode(true);
     * voucherNode.getParentNode().removeChild(voucherNode);
     */

    return;
  }

  /*
   * This section processes the transactions for POS sales
   */

  private OBError processPosTransaction(LedgerTransaction txn) throws Exception {

    OBError result = new OBError();

    DocumentSection voucher = getEoneHeaderNode(VOUCHER_NODE);
    log4j.info("Voucher node " + voucher);

    try {
      addNode(cloneVoucher, parentVoucher);
    } catch (Exception e) {
      System.out.println("There is a problem in POS process transactions");
      e.printStackTrace();
    }

    List<NodeMap> defList = voucher.getDFEXNODEMAPList();
    for (NodeMap def : defList) {
      log4j.info("Set the value for node " + def);
      try {
        setPosValue(def, txn);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        System.out.println("There is a problem in POS process transactions set value");
        e.printStackTrace();
      }
    }
    // Now process each transaction leg
    List<LedgerTransactionLeg> txnLegs = getLedgerList(txn);
    boolean firstRec = true;
    currentLegLineNo = 0;
    for (LedgerTransactionLeg leg : txnLegs) {
      currentLegLineNo++;
      processPosTxnLeg(leg, firstRec);
      firstRec = false;
    }
    txn.setExported(true);
    txn.setExport(new Date());
    OBDal.getInstance().save(txn);
    OBDal.getInstance().flush();
    return result;

  }

  private void setPosValue(NodeMap def, LedgerTransaction txn) throws Exception {

    Node element;

    /*
     * Get the nth node with the given tagname. If n = 0, return the last node
     */
    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    log4j.info("Tag " + def.getXpathName() + " Property " + def.getObproperty() + " Default value "
        + def.getDefaultValue());

    if (def.getObproperty() != null) {
      String result = computePosValue(txn, def.getObproperty());
      element.setTextContent(result);
      log4j.info("Set Computed Value: " + result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
      log4j.info("Set default Value: " + def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      if (def.getDFEXATTRIBUTEMAPList().size() == 0) {
        log4j.error("Both default value and OB Property not defined");
      }
    }
    if (def.getDFEXATTRIBUTEMAPList().size() > 0) {
      /*
       * If attributes are defined for a node in the defaults table, set them.
       */
      for (AttributeMap attribute : def.getDFEXATTRIBUTEMAPList()) {
        String attValue = null;
        if (attribute.getDefaultValue() != null) {
          attValue = attribute.getDefaultValue();
        } else {
          // Compute the attribute value based on the OB property
          attValue = getAttributeValue(attribute, txn);
        }
        ((Element) element).setAttribute(attribute.getAttribute(), attValue);
      }
    }

  }

  /*
   * This method computes the values which will be set in the xml file. The condition is checked
   * with the obProperty in the EONE setup window
   */

  private String computePosValue(LedgerTransaction txn, String obProperty) {
    String result = null;

    if (obProperty.equals("COMPANY")) {
      result = txn.getOrganization().getName();
    } else if (obProperty.equals("DATE")) {
      result = new SimpleDateFormat("yyyy-MM-dd").format(txn.getTransactionDate());
    } else if (obProperty.equals("GUID")) {
      result = txn.getId();
    } else if (obProperty.equals("VTYPE")) {
      result = getVoucherType(txn.getDocumentType().getGLCategory());
    } else if (obProperty.equals("DESC")) {
      if (getLedgerList(txn).get(0).getAccountingFact() != null) {
        result = getLedgerList(txn).get(0).getAccountingFact().getDescription();
      }
    } else if (obProperty.equals("VNUM")) {
      result = txn.getDocumentNo();
    } else if (obProperty.equals("USER")) {
      result = txn.getCreatedBy().getName();
    } else if (obProperty.equals("LEDGER")) {
      List<LedgerTransactionLeg> txnLegs = getLedgerList(txn);
      result = getLedgerName(txnLegs.get(0));
    } else if (obProperty.equals("REF")) {
      result = txn.getPartnerreference();
    } else if (obProperty.equals("CTS")) {
      result = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss").format(new Date());
    }

    log4j
        .info("Type: " + this.transactionType + " OB Property " + obProperty + " result " + result);
    return result;
  }

  /*
   * This method processes a POS transaction leg After POS transaction Header computation
   */
  private OBError processPosTxnLeg(LedgerTransactionLeg leg, boolean firstRec) throws Exception {
    OBError result = new OBError();

    DocumentSection ledger = getEoneHeaderNode("L");
    log4j.info("Ledger node " + ledger);
    addNode(cloneLedger, parentLedger);

    List<NodeMap> defList = ledger.getDFEXNODEMAPList();
    for (NodeMap def : defList) {
      setPosValue(def, leg);
    }

    return result;
  }

  private void setPosValue(NodeMap def, LedgerTransactionLeg leg) throws Exception {
    Node element;

    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    if (def.getObproperty() != null) {
      String result = computePosValue(leg, def.getObproperty());
      element.setTextContent(result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      log4j.error("Both default value and OB Property not defined");
    }
  }

  private String computePosValue(LedgerTransactionLeg leg, String obProperty) {
    String result = null;
    if (obProperty.equals("LEDGER") || obProperty.equals("NAME")) {
      result = getLedgerName(leg);
    } else if (obProperty.equals("ISPOS")) {
      result = "No";
      if (leg.getCredit().compareTo(BigDecimal.ZERO) == 0) {
        result = "Yes";
      }
    } else if (obProperty.equals("AMT")) {
      result = leg.getCredit().subtract(leg.getDebit()).toString();
    } else if (obProperty.equals("BTYPE")) {
      result = new String("New Ref");
    } else if (obProperty.equals("VNUM")) {
      result = leg.getDJELedgerTxn().getDocumentNo();
    } else if (obProperty.equals("COST")) {
      result = (leg.getCostcenter() == null ? "" : leg.getCostcenter().getName());
    } else if (obProperty.equals("REF")) {
      result = leg.getDJELedgerTxn().getPartnerreference();
    } else if (obProperty.equals("MAMT")) {

      result = leg.getCredit().subtract(leg.getDebit()).abs().toString();
    } else if (obProperty.equals("LINE")) {
      result = Integer.toString(this.currentLegLineNo);
    } else if (obProperty.equals("LEDGERALIAS")) {
      result = leg.getLedgerAlias();
    } else if (obProperty.equals("OBLEDGER")) {
      result = leg.getDescription();
    }

    return result;
  }

  /*
   * Clone the recurring nodes so that these nodes can be inserted when required
   */
  private void initializeECOMXML() throws Exception {
    // Parse the template file
    openTemplateFile(header.getTemplate());
    doc.getDocumentElement().normalize();

    // Now read the recurring nodes
    OBCriteria<DocumentSection> nodeCrit = OBDal.getInstance()
        .createCriteria(DocumentSection.class);
    nodeCrit.add(Restrictions.eq(DocumentSection.PROPERTY_DFEXMAPDEFINITION, header));
    if (nodeCrit.list().size() == 0) {
      throw new OBException("Pls configure the ecom sublists");
    }
    DocumentSection voucher, ledger, bill, cost, appArea;
    voucher = ledger = bill = cost = appArea = null;
    for (DocumentSection tnode : nodeCrit.list()) {
      if (tnode.getNodename().equals(VOUCHER_NODE)) {
        voucher = tnode;
      } else if (tnode.getNodename().equals("L")) {
        ledger = tnode;
      } else if (tnode.getNodename().equals("B")) {
        bill = tnode;
      } else if (tnode.getNodename().equals("C")) {
        cost = tnode;
      } else if (tnode.getNodename().equals("H")) {
        appArea = tnode;
      } else {
        throw new OBException("Incorrect sublist " + tnode.getNodename());
      }
    }
    if (appArea != null) {
      appAreaNode = getNode(voucher.getNodePath());
    }

    // node
    if (voucher == null) {
      throw new OBException("Mandatory Xpath node Voucher not defined in Setup");
    }
    Node voucherNode = getNode(voucher.getNodePath());

    if (ledger == null) {
      throw new OBException("Mandatory Xpath node Voucher not defined in Setup");
    }
    Node ledgerNode = null;
    ledgerNode = getNode(ledger.getNodePath());

    Node billNode = null;
    if (bill != null) {
      billNode = getNode(bill.getNodePath());
      parentBill = getXpath(billNode.getParentNode());

    }

    if (cost != null) {

      costNode = getNode(cost.getNodePath());
      parentCost = getXpath(costNode.getParentNode());

    }

    parentVoucher = getXpath(voucherNode.getParentNode());
    parentLedger = getXpath(ledgerNode.getParentNode());

    if (cost != null) {

      cloneCost = costNode.cloneNode(true);
      costNode.getParentNode().removeChild(costNode);
    }

    if (bill != null) {

      cloneBill = billNode.cloneNode(true);
      billNode.getParentNode().removeChild(billNode);
    }

    cloneLedger = ledgerNode.cloneNode(true);
    ledgerNode.getParentNode().removeChild(ledgerNode);
    cloneVoucher = voucherNode.cloneNode(true);
    voucherNode.getParentNode().removeChild(voucherNode);

    return;
  }

  /*
   * This method processes a Transaction of ECOM and sets values for the XML file
   */

  private OBError processTransaction(LedgerTransaction txn) throws Exception {
    log4j.info("Inside ECOM Transaction header" + txn.getDocumentNo());
    Map<BigDecimal, LedgerTransactionLeg> taxInfo = new HashMap<BigDecimal, LedgerTransactionLeg>();
    Map<BigDecimal, LedgerTransactionLeg> delvChrgInfo = new HashMap<BigDecimal, LedgerTransactionLeg>();

    OBError result = new OBError();

    DocumentSection voucher = getEoneHeaderNode(VOUCHER_NODE);
    log4j.info("Voucher node " + voucher);

    addNode(cloneVoucher, parentVoucher);

    List<NodeMap> defList = voucher.getDFEXNODEMAPList();
    for (NodeMap def : defList) {
      log4j.info("Set the value for node " + def.getEntityName());
      setTxnValue(def, txn);
    }
    /*
     * Now process each transaction leg
     */

    // This loop is for getting tax rate types into a hashmap. This map will
    // be used when creating detailed journal entry line
    List<LedgerTransactionLeg> txnLegs = getLedgerList(txn);
    for (LedgerTransactionLeg leg : txnLegs) {
      if (leg.getLTLType().equals("T")) {
        taxInfo.put(leg.getTax().getRate().stripTrailingZeros(), leg);

      }

    }
    // This loop is for getting delivery charges types into a hashmap. This
    // map will be used when creating detailed journal entry line

    for (LedgerTransactionLeg leg : txnLegs) {
      if (leg.getLTLType().equals("D")) {
        delvChrgInfo.put(leg.getTax().getRate().stripTrailingZeros(), leg);

      }

    }
    boolean firstRec = true;
    currentLegLineNo = 0;
    for (LedgerTransactionLeg leg : txnLegs) {
      if ((!leg.getLTLType().equals("T") && !leg.getLTLType().equals("O"))) {
        currentLegLineNo++;
        processTxnLeg(leg, firstRec, taxInfo, delvChrgInfo);
        firstRec = false;
      }
    }

    txn.setExported(true);
    txn.setExport(new Date());
    OBDal.getInstance().save(txn);
    OBDal.getInstance().flush();
    // SessionHandler.getInstance().commitAndStart();
    return result;
  }

  /*
   * For a given tag, compute the value based on the OB property, if one exists. Else, set the tag
   * value to the default value defined. Also, set the attributes for the nodes if defined
   */
  private void setTxnValue(NodeMap def, LedgerTransaction txn) throws Exception {
    Node element;

    /*
     * Get the nth node with the given tagname. If n = 0, return the last node
     */
    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    log4j.info("Tag " + def.getXpathName() + " Property " + def.getObproperty() + " Default value "
        + def.getDefaultValue());

    if (def.getObproperty() != null) {
      String result = computeValue(txn, def.getObproperty());
      element.setTextContent(result);
      log4j.info("Set Computed Value: " + result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
      log4j.info("Set default Value: " + def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      if (def.getDFEXATTRIBUTEMAPList().size() == 0) {
        log4j.error("Both default value and OB Property not defined");
      }
    }
    if (def.getDFEXATTRIBUTEMAPList().size() > 0) {
      /*
       * If attributes are defined for a node in the defaults table, set them.
       */
      for (AttributeMap attribute : def.getDFEXATTRIBUTEMAPList()) {
        String attValue = null;
        if (attribute.getDefaultValue() != null) {
          attValue = attribute.getDefaultValue();
        } else {
          // Compute the attribute value based on the OB property
          attValue = getAttributeValue(attribute, txn);
        }
        ((Element) element).setAttribute(attribute.getAttribute(), attValue);
      }
    }
  }

  /*
   * Compute the tag value based on the OB property defined
   */
  private String computeValue(LedgerTransaction txn, String obProperty) {
    String result = null;

    if (obProperty.equals("COMPANY")) {
      result = txn.getOrganization().getName();
    } else if (obProperty.equals("DATE")) {
      result = new SimpleDateFormat("yyyy-MM-dd").format(txn.getTransactionDate());
    } else if (obProperty.equals("GUID")) {
      result = txn.getId();
    } else if (obProperty.equals("VTYPE")) {
      result = getVoucherType(txn.getDocumentType().getGLCategory());
    } else if (obProperty.equals("DESC")) {
      if (getLedgerList(txn).get(0).getAccountingFact() != null) {
        result = getLedgerList(txn).get(0).getAccountingFact().getDescription();
      }
    } else if (obProperty.equals("VNUM")) {
      result = txn.getPartnerreference(); // getDocumentNo();
    } else if (obProperty.equals("TNUM")) {
      result = txn.getDfexThirdNum().toString();
    } else if (obProperty.equals("USER")) {
      result = txn.getCreatedBy().getName();
    } else if (obProperty.equals("LEDGER")) {
      List<LedgerTransactionLeg> txnLegs = getLedgerList(txn);
      result = getLedgerName(txnLegs.get(0));
    } else if (obProperty.equals("REF")) {
      result = txn.getPartnerreference();
    } else if (obProperty.equals("CTS")) {
      result = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss").format(new Date());
    }

    log4j
        .info("Type: " + this.transactionType + " OB Property " + obProperty + " result " + result);
    return result;
  }

  private OBError processTxnLeg(LedgerTransactionLeg leg, boolean firstLeg,
      Map<BigDecimal, LedgerTransactionLeg> taxInfo,
      Map<BigDecimal, LedgerTransactionLeg> delvChrgInfo) throws Exception {

    OBError result = new OBError();
    if (!leg.getLTLType().equals("T") && !leg.getLTLType().equals("O")) {
      DocumentSection ledger = getEoneHeaderNode("L");
      log4j.info("Ledger node " + ledger);
      addNode(cloneLedger, parentLedger);

      List<NodeMap> defList = ledger.getDFEXNODEMAPList();
      for (NodeMap def : defList) {
        setTxnLegValue(def, leg);
      }

      // if the transaction leg type is not a Delivery Charge then process
      if (!leg.getLTLType().equals("D")) {
        addNode(cloneCost, parentCost);
        processTxnDetails(leg, taxInfo, delvChrgInfo); // for Ecom
        // Details
      }

      return result;
    } else
      return result;

  }

  /*
   * For a given tag, compute the value based on the OB property, if one exists. Else, set the tag
   * value to the default value defined. Also, set the attributes for the nodes if defined
   */
  private void setTxnLegValue(NodeMap def, LedgerTransactionLeg leg) throws Exception {
    Node element;

    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    if (def.getObproperty() != null) {
      String result = computeValue(leg, def.getObproperty());
      element.setTextContent(result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      log4j.error("Both default value and OB Property not defined");
    }
  }

  private String computeValue(LedgerTransactionLeg leg, String obProperty) {
    log4j.info("The Ecom Leg obProperty" + obProperty);
    String result = null;
    if (obProperty.equals("LEDGER") || obProperty.equals("NAME")) {
      result = getLedgerName(leg) == null ? "" : getLedgerName(leg);
    } else if (obProperty.equals("ISPOS")) {
      result = "No";
      if (leg.getCredit().compareTo(BigDecimal.ZERO) == 0) {
        result = "Yes";
      }
    } else if (obProperty.equals("AMT")) {
      result = leg.getCredit().subtract(leg.getDebit()).toString();
    } else if (obProperty.equals("BTYPE")) {
      result = new String("New Ref");
    } else if (obProperty.equals("VNUM")) {
      result = leg.getDJELedgerTxn().getDocumentNo();
    } else if (obProperty.equals("COST")) {
      result = (leg.getCostcenter() == null ? "" : leg.getCostcenter().getName());
    } else if (obProperty.equals("REF")) {
      result = leg.getDJELedgerTxn().getPartnerreference();
    } else if (obProperty.equals("MAMT")) {

      result = leg.getCredit().subtract(leg.getDebit()).abs().toString();
    } else if (obProperty.equals("LINE")) {
      result = Integer.toString(this.currentLegLineNo);
    } else if (obProperty.equals("LEDGERALIAS")) {
      result = leg.getLedgerAlias() == null ? "" : leg.getLedgerAlias();
    } else if (obProperty.equals("OBLEDGER")) {
      // result = leg.getAccountingFact().getAccount().getName() ;
      result = leg.getDescription() == null ? "" : leg.getDescription();
    }

    return result;
  }

  private void processTxnDetails(LedgerTransactionLeg leg,
      Map<BigDecimal, LedgerTransactionLeg> taxInfo,
      Map<BigDecimal, LedgerTransactionLeg> delvChrgInfo) throws Exception {

    DocumentSection costNode = getEoneHeaderNode(CUST_TXN_TYPE);
    log4j.info("Entered into processTxnDetails");
    List<NodeMap> defList = costNode.getDFEXNODEMAPList();
    for (NodeMap def : defList) {
      setEcomDetailValue(def, leg, taxInfo, delvChrgInfo);

    }

  }

  private void setEcomDetailValue(NodeMap def, LedgerTransactionLeg leg,
      Map<BigDecimal, LedgerTransactionLeg> taxInfo,
      Map<BigDecimal, LedgerTransactionLeg> delvChrgInfo) throws Exception {
    Node element;

    // Get the nth node with the given tagname. If n = 0, return the last
    // node

    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    if (def.getObproperty() != null) {
      String result = computeEcomDetailValue(leg, def.getObproperty(), taxInfo, delvChrgInfo);
      element.setTextContent(result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      if (def.getDFEXATTRIBUTEMAPList().size() == 0) {
        log4j.error("Both default value and OB Property not defined");
      }
    }
  }

  private String computeEcomDetailValue(LedgerTransactionLeg leg, String obProperty,
      Map<BigDecimal, LedgerTransactionLeg> taxInfo,
      Map<BigDecimal, LedgerTransactionLeg> delvChrgInfo) {
    log4j.info("Entered into computeEcomDetailsValue");
    String result = null;

    try {
      LedgerTransactionLeg taxLeg = taxInfo.get(leg.getTax().getRate().stripTrailingZeros());
      LedgerTransactionLeg delvChrgLeg = delvChrgInfo.get(leg.getTax().getRate()
          .stripTrailingZeros());
      if (obProperty.equals("LEDGER") || obProperty.equals("NAME")) {
        result = taxLeg.getLedgerName();

      } else if (obProperty.equals("ISPOS")) {
        result = "No";
        if (leg.getCredit().compareTo(BigDecimal.ZERO) == 0) {
          result = "Yes";
        }
      } else if (obProperty.equals("AMT")) { // TOTAMT, TAXAMT
        result = leg.getCredit().subtract(leg.getDebit()).toString();
      } else if (obProperty.equals("BTYPE")) {
        result = new String("New Ref");
      } else if (obProperty.equals("VNUM")) {
        result = leg.getDJELedgerTxn().getDocumentNo();
      } else if (obProperty.equals("COST")) {
        result = (leg.getCostcenter() == null ? "" : leg.getCostcenter().getName());
      } else if (obProperty.equals("REF")) {
        result = leg.getDJELedgerTxn().getPartnerreference();
      } else if (obProperty.equals("MAMT")) {

        if (delvChrgLeg != null) {
          result = leg.getCredit().subtract(leg.getDebit()).abs().add(
              delvChrgLeg.getCredit().subtract(leg.getDebit()).abs()).toString();
        } else {
          result = leg.getCredit().subtract(leg.getDebit()).abs().toString();
        }
      } else if (obProperty.equals("LINE")) {
        result = Integer.toString(++this.currentLegLineNo);
      } else if (obProperty.equals("LEDGERALIAS")) {
        result = leg.getLedgerAlias();
      } else if (obProperty.equals("OBLEDGER")) {

        result = taxLeg.getDescription();

      } else if (obProperty.equals("TOTAMT")) {
        BigDecimal taxAmt = new BigDecimal(0.00);
        if (taxLeg.getCredit() != null) {
          taxAmt = taxLeg.getCredit().subtract(leg.getDebit()).abs();
        }
        if (delvChrgLeg != null) {
          result = leg.getCredit().subtract(leg.getDebit()).abs().add(taxAmt).add(
              delvChrgLeg.getCredit().subtract(leg.getDebit()).abs()).toString();
        } else {
          result = leg.getCredit().subtract(leg.getDebit()).abs().add(
              taxLeg.getCredit().subtract(leg.getDebit()).abs()).toString();
        }

      } else if (obProperty.equals("TAXAMT")) {

        result = taxLeg.getCredit() == null ? new BigDecimal(0.00).toString() : taxLeg.getCredit()
            .subtract(leg.getDebit()).abs().toString();
        log4j.info("TAXAMT is" + result);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /*
   * Clone the recurring nodes for Masters XML File so that these nodes can be inserted when
   * required
   */
  private void initializeMastersXML() throws Exception {

    log4j.info("Inside Masters XML initialization");
    // Parse the template file
    openTemplateFile(header.getTemplate());
    doc.getDocumentElement().normalize();

    // Now read the recurring nodes
    OBCriteria<DocumentSection> nodeCrit = OBDal.getInstance()
        .createCriteria(DocumentSection.class);
    nodeCrit.add(Restrictions.eq(DocumentSection.PROPERTY_DFEXMAPDEFINITION, header));
    if (nodeCrit.list().size() == 0) {
      throw new OBException("Pls configure the sublists");
    }

    ledger = name = address = appArea = null;
    for (DocumentSection tnode : nodeCrit.list()) {
      if (tnode.getNodename().equals(VOUCHER_NODE)) {
        ledger = tnode;
      } else if (tnode.getNodename().equals("A")) {
        address = tnode;
      } else if (tnode.getNodename().equals("N")) {
        name = tnode;
      } else if (tnode.getNodename().equals("H")) {
        appArea = tnode;
      } else {
        throw new OBException("Incorrect sublist " + tnode.getNodename());
      }
    }

    if (appArea != null) {
      appAreaNode = getNode(appArea.getNodePath());
    }

    Node ledgerNode = getNode(ledger.getNodePath());
    parentLedger = getXpath(ledgerNode.getParentNode());
    log4j.info("Parent node in Masters XML" + parentLedger);
    cloneLedger = ledgerNode.cloneNode(true);
    ledgerNode.getParentNode().removeChild(ledgerNode);

    return;
  }

  private OBError processCompany(RCCompany company) throws Exception {
    OBError result = new OBError();

    addCompanyNode(company, VOUCHER_NODE, cloneLedger, parentLedger);
    company.setDfexExported(true);
    company.setDfexExporttime(new Date());
    OBDal.getInstance().save(company);
    // OBDal.getInstance().flush();
    // SessionHandler.getInstance().commitAndStart();

    return result;

  }

  private void addCompanyNode(RCCompany company, String nodeType, Node clone, String parent)
      throws Exception {

    DocumentSection eoneNode = getEoneHeaderNode(nodeType);
    log4j.info("Eone node " + eoneNode);

    if (nodeType.equals(VOUCHER_NODE)) {
      addNode(clone, parent);
    }
    List<NodeMap> defList = eoneNode.getDFEXNODEMAPList();
    for (NodeMap def : defList) {
      log4j.info("Set the value for node " + def);

      setEoneCompValue(def, company, 0);

    }

    return;
  }

  /*
   * For a given tag, compute the value based on the OB property, if one exists. Else, set the tag
   * value to the default value defined. Also, set the attributes for the nodes if defined
   */
  private void setEoneCompValue(NodeMap def, RCCompany comp, int i) throws Exception {
    Node element;

    /*
     * Get the nth node with the given tagname. If n = 0, return the last node
     */
    element = getNthNode(def.getXpathName(), 0);
    if (element == null) {
      throw new OBException(def.getDefaultValue() + ": Node not found in template");
    }

    log4j.info("Tag " + def.getXpathName() + " Property " + def.getObproperty() + " Default value "
        + def.getDefaultValue());

    if (def.getObproperty() != null) {
      String result;
      if (def.getObproperty().equals("ADDR")) {
        result = computeValue(comp, def.getObproperty() + i);
      } else {
        result = computeValue(comp, def.getObproperty());
      }
      element.setTextContent(result);
      log4j.info("Set Computed Value: " + result);
    } else if (def.getDefaultValue() != null) {
      // If default value exists, use it.
      element.setTextContent(def.getDefaultValue());
      log4j.info("Set default Value: " + def.getDefaultValue());
    } else {
      // If both the default and OB property is not defined, check if any
      // attributes are defined.
      if (def.getDFEXATTRIBUTEMAPList().size() == 0) {
        log4j.error("Both default value and OB Property not defined");
      }
    }
    if (def.getDFEXATTRIBUTEMAPList().size() > 0) {
      /*
       * If attributes are defined for a node in the defaults table, set them.
       */
      for (AttributeMap attribute : def.getDFEXATTRIBUTEMAPList()) {
        String attValue = null;
        if (attribute.getDefaultValue() != null) {
          attValue = attribute.getDefaultValue();
        } else {
          // Compute the attribute value based on the OB property
          attValue = getAttributeValue(attribute, comp);
        }
        ((Element) element).setAttribute(attribute.getAttribute(), attValue);
      }
    }
  }

  /*
   * Compute the tag value based on the OB property defined
   */
  private String computeValue(RCCompany comp, String obProperty) {
    String result = null;

    if (obProperty.equals("COMPANY")) {
      result = comp.getOrganization().getName();
    } else if (obProperty.equals("CNAME")) {
      result = comp.getCompanyName() == null ? "" : comp.getCompanyName().length() > 40 ? comp
          .getCompanyName().substring(0, 40) : comp.getCompanyName();
    } else if (obProperty.equals("CALIAS")) {
      result = getLedgerAlias(comp);
    } else if (obProperty.equals("ADDR0")) {
      result = comp.getLocationAddress().getAddressLine1() == null ? "" : comp.getLocationAddress()
          .getAddressLine1().length() > 40 ? comp.getLocationAddress().getAddressLine1().substring(
          0, 40) : comp.getLocationAddress().getAddressLine1();
    } else if (obProperty.equals("ADDR1")) {
      result = comp.getLocationAddress().getAddressLine2() == null ? "" : comp.getLocationAddress()
          .getAddressLine2().length() > 40 ? comp.getLocationAddress().getAddressLine2().substring(
          0, 40) : comp.getLocationAddress().getAddressLine2();
    } else if (obProperty.equals("ADDR2")) {
      result = comp.getLocationAddress().getRCAddress3() == null ? "" : comp.getLocationAddress()
          .getRCAddress3().length() > 40 ? comp.getLocationAddress().getRCAddress3().substring(0,
          40) : comp.getLocationAddress().getRCAddress3();
    } else if (obProperty.equals("ADDR3")) {
      result = comp.getLocationAddress().getRCAddress4() == null ? "" : comp.getLocationAddress()
          .getRCAddress4().length() > 40 ? comp.getLocationAddress().getRCAddress4().substring(0,
          40) : comp.getLocationAddress().getRCAddress4();
    } else if (obProperty.equals("CITY")) {
      result = comp.getLocationAddress().getCityName() == null ? "" : comp.getLocationAddress()
          .getCityName();
    } else if (obProperty.equals("STATE")) {
      result = comp.getLocationAddress().getRegion().getSearchKey() == null ? "" : comp
          .getLocationAddress().getRegion().getSearchKey();
    } else if (obProperty.equals("CMOBILE")) {

      result = comp.getRCCompContactList().size() == 0 ? "" : (comp.getRCCompContactList().get(0)
          .getMobile() == null ? "" : comp.getRCCompContactList().get(0).getMobile());
      log4j.info("CMOBILE is:" + result);
    } else if (obProperty.equals("CMAIL")) {
      result = comp.getRCCompContactList().size() == 0 ? "" : (comp.getRCCompContactList().get(0)
          .getEmail() == null ? "" : comp.getRCCompContactList().get(0).getEmail());
      log4j.info("CMAIL is:" + result);
    } else if (obProperty.equals("PIN")) {
      result = comp.getLocationAddress().getPostalCode() == null ? "" : comp.getLocationAddress()
          .getPostalCode();
    } else if (obProperty.equals("LANG")) {
      if (comp.getLocationAddress().getCountry() != null
          && comp.getLocationAddress().getCountry().getLanguage() != null) {
        result = comp.getLocationAddress().getCountry().getLanguage().getLanguage();
      }
    } else if (obProperty.equals("CUR")) {

      result = new String("INR");
    } else if (obProperty.equals("TNUM")) {
      result = comp.getDfexThirdNum() == null ? "D5000000" : comp.getDfexThirdNum().toString();

    }

    log4j
        .info("Type: " + this.transactionType + " OB Property " + obProperty + " result " + result);
    return result;
  }

  private ArrayList<String> getGLCategory() {
    ArrayList<String> glCatList = new ArrayList<String>();
    for (GLCategory v : header.getDfexGlCategoryList()) {
      if (v.isActive()) {
        glCatList.add(v.getGLCategory().getName());
      }
    }
    return glCatList;
  }

  /*
   * Retrieve the accounting transaction list based on the transaction type, date range,
   * full/incremental download etc.
   */
  private List<LedgerTransaction> getTransactionList(ArrayList<String> glTypes) {
    boolean ret = false;
    OBCriteria<org.openbravo.model.financialmgmt.gl.GLCategory> glCatCriteria = OBDal.getInstance()
        .createCriteria(org.openbravo.model.financialmgmt.gl.GLCategory.class);
    glCatCriteria.add(Restrictions.in(
        org.openbravo.model.financialmgmt.gl.GLCategory.PROPERTY_NAME, glTypes));
    OBCriteria<DocumentType> docCriteria = OBDal.getInstance().createCriteria(DocumentType.class);

    docCriteria.add(Restrictions.in(DocumentType.PROPERTY_GLCATEGORY, glCatCriteria.list()));
    if (this.transactionType.equals(SALES_RETURN_TXN_TYPE)
        || this.transactionType.equals(PURCHASE_RETURN_TXN_TYPE)) {
      ret = true;
    }
    docCriteria.add(Restrictions.eq(DocumentType.PROPERTY_RETURN, ret));
    if (docCriteria.list().size() == 0) {
      throw new OBException("G/L Category " + glTypes.toString() + "does not exist");
    }

    // Retrieve all the ledger transaction of the given document types and
    // organization
    OBCriteria<LedgerTransaction> ledgCriteria = OBDal.getInstance().createCriteria(
        LedgerTransaction.class);
    ledgCriteria.add(Restrictions.in(LedgerTransaction.PROPERTY_DOCUMENTTYPE, docCriteria.list()
        .toArray()));
    if (startDate != null) {
      ledgCriteria.add(Restrictions.ge(LedgerTransaction.PROPERTY_TRANSACTIONDATE, startDate));
    }
    if (endDate != null) {
      ledgCriteria.add(Restrictions.le(LedgerTransaction.PROPERTY_TRANSACTIONDATE,
          getEndOfDay(endDate)));
    }

    if (incremental) {
      ledgCriteria.add(Restrictions.eq(LedgerTransaction.PROPERTY_EXPORTED, false));
    }
    ledgCriteria.addOrderBy(LedgerTransaction.PROPERTY_TRANSACTIONDATE, true);

    return ledgCriteria.list();
  }

  /*
   * This method returns a list of companies with restrictions of creation date and the export
   * timestamp if full download is not checked
   */

  private List<RCCompany> getCompanyList() {
    OBCriteria<RCCompany> compCriteria = OBDal.getInstance().createCriteria(RCCompany.class);
    compCriteria.setFilterOnActive(false);

    if (startDate != null) {
      compCriteria.add(Restrictions.ge(RCCompany.PROPERTY_CREATIONDATE, startDate));
    }
    if (endDate != null) {
      compCriteria.add(Restrictions.le(RCCompany.PROPERTY_CREATIONDATE, getEndOfDay(endDate)));
    }
    if (incremental) {
      compCriteria.add(Restrictions.eq(RCCompany.PROPERTY_DFEXEXPORTED, false));
    }
    compCriteria.addOrderBy(RCCompany.PROPERTY_CREATIONDATE, true);
    return compCriteria.list();
  }

  private String getFileType() {

    log4j.info("getFileType" + this.transactionType);
    if (this.transactionType.equals(CUST_TXN_TYPE)) {
      return new String("Customer");
    } else if (this.transactionType.equals(SALES_TXN_TYPE_ECOM)) {
      return new String("ECOM");
    } else if (this.transactionType.equals(SALES_TXN_TYPE_POS)) {
      return new String("POS");
    } else if (this.transactionType.equals(PURCHASE_TXN_TYPE)) {
      return new String("Purchase");
    } else if (this.transactionType.equals(SALES_RETURN_TXN_TYPE)) {
      return new String("Sales Return");
    } else if (this.transactionType.equals(PURCHASE_RETURN_TXN_TYPE)) {
      return new String("Purchase Return");
    } else if (this.transactionType.equals(VENDOR_TXN_TYPE)) {
      return new String("Vendor");
    } else if (this.transactionType.equals(EXISTING_MASTERS_TXN_TYPE)) {
      return new String("Master");
    }
    return null;
  }

  private String getDownloadUrl() {
    String relativeUrl = fullFileName.split("/web/")[1];
    return "<a href='../web/" + relativeUrl + "' target='_blank' >here</a> ";
  }

  private String getWebDirName() {
    final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
    File f = new File(url.getPath());

    do {
      f = f.getParentFile();
    } while (!(f.getName().equals("/")) && (!f.getName().equals("classes")));

    if (f.getName().equals("classes")) {
      if (f.getParentFile().getName().equals("build")) {
        return f.getParentFile().getParent() + "/WebContent/web";
      } else {
        return f.getParentFile().getParent() + "/web";
      }
    }
    throw new OBException("Cannot set path for download for " + url.getPath());
  }

  private void parseFile(String fileName) {
    log4j.info("Opening XML file... " + fileName);

    try {
      final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilderFactory.setNamespaceAware(true);
      final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      doc = docBuilder.parse(new File(fileName));

      XPathFactory factory = XPathFactory.newInstance();
      xpath = factory.newXPath();
      xpath.setNamespaceContext(new UniversalNamespaceCache(doc, true));
    } catch (Exception e) {
      log4j.error("Exception " + e.getMessage());
      log4j.error(e.getStackTrace());
      throw new OBException(e.getMessage());
    }
    log4j.info("Parsed file " + fileName + ". Document......." + doc);
    return;
  }

  private void openTemplateFile(String template) {
    // Get the web directory
    String webDir = getWebDirName();
    log4j.info("Web Directory is " + webDir);
    if (webDir == null) {
      throw new OBException("Cannot find the web directory");
    }

    String templateFile = webDir + "/" + template;
    log4j.info("Template File" + templateFile);
    parseFile(templateFile);

    return;
  }

  // This method writes a DOM document to a file
  public void writeXmlFile(String filename) {
    try {
      // Prepare the DOM document for writing
      Source source = new DOMSource(doc);

      // Prepare the output file
      File file = new File(fullFileName);
      Result result = new StreamResult(file);

      // Write the DOM document to the file
      Transformer xformer = TransformerFactory.newInstance().newTransformer();
      xformer.transform(source, result);
    } catch (TransformerConfigurationException e) {
      log4j.error("Caught configuration exception " + e.getMessage());
      log4j.error(e.getStackTrace());
      throw new OBException(e.getMessage());
    } catch (TransformerException e) {
      log4j.error("Caught transformer exception " + e.getMessage());
      log4j.error(e.getStackTrace());
      throw new OBException(e.getMessage());

    }
  }

  private Node getNode(String name) throws Exception {
    NodeList elemList;
    elemList = (NodeList) xpath.evaluate(name, doc, XPathConstants.NODESET);
    System.out.println("The number of child nodes: " + elemList.getLength());
    if (elemList.getLength() == 0) {
      throw new OBException("Could not find tag " + name);
    }
    Node element = elemList.item(0);

    return element;
  }

  private String getXpath(Node node) {
    StringBuffer path = new StringBuffer();
    while (node.getParentNode() != null) {
      path = path.insert(0, "/" + node.getNodeName());
      node = node.getParentNode();
    }

    log4j.info("Absolute path of node  is " + path.toString());
    return path.toString();
  }

  private Node getNthNode(String name, int n) throws Exception {
    NodeList elemList = (NodeList) xpath.evaluate(name, doc, XPathConstants.NODESET);
    Node element;

    if (elemList.getLength() == 0 || n > elemList.getLength()) {
      log4j.info("Could not find tag " + name + " at position " + n);
      return null;
    }
    if (n == 0) {
      // Return the last node
      element = elemList.item(elemList.getLength() - 1);
    } else {
      // Return the nth node
      element = elemList.item(n - 1);
    }

    return element;
  }

  private void addNode(Node node, String parent) throws Exception {
    log4j.info("Adding node to parent " + parent);
    Node clonedNode = node.cloneNode(true);
    Node parentNode = getNthNode(parent, 0);
    parentNode.appendChild(clonedNode);
  }

  private void addNoRecordsError(String rootNode) throws Exception {
    Element error = doc.createElement("ERROR");
    Text msg = doc.createTextNode("No records");
    error.appendChild(msg);

    Node parentNode = getNthNode(rootNode, 0);
    parentNode.appendChild(error);

  }

  /*
   * Retrieve the latest mapping record corresponding to the transaction
   */
  private MapDefinitionHeader getLatestHeader() {

    Organization adOrg = OBDal.getInstance().get(Organization.class, "0");
    OBCriteria<MapDefinitionHeader> headerCriteria = OBDal.getInstance().createCriteria(
        MapDefinitionHeader.class);
    headerCriteria.add(Restrictions.eq(MapDefinitionHeader.PROPERTY_TRANSACTIONTYPE,
        this.transactionType));
    headerCriteria.add(Restrictions.eq(MapDefinitionHeader.PROPERTY_ORGANIZATION, adOrg));

    MapDefinitionHeader latestVersion = null;
    Date latestVersionDate = null;
    if (headerCriteria.list().size() == 0) {
      throw new OBException("Setup the eone" + " mappings for transaction " + this.transactionType);
    }
    for (MapDefinitionHeader header : headerCriteria.list()) {
      if (latestVersionDate == null || header.getCreationDate().after(latestVersionDate)) {
        latestVersionDate = header.getCreationDate();
        latestVersion = header;
      }
    }
    return latestVersion;
  }

  DocumentSection getEoneHeaderNode(String nodetype) {
    OBCriteria<DocumentSection> nodeCriteria = OBDal.getInstance().createCriteria(
        DocumentSection.class);
    nodeCriteria.add(Restrictions.eq(DocumentSection.PROPERTY_NODENAME, nodetype));
    nodeCriteria.add(Restrictions.eq(DocumentSection.PROPERTY_DFEXMAPDEFINITION, header));

    log4j.info("Node list " + nodeCriteria.list());
    if (nodeCriteria.list().size() == 0) {
      throw new OBException("Node " + nodetype + " is not defined ");
    }
    return nodeCriteria.list().get(0);
  }

  /*
   * Compute the attribute value based on the OB property
   */
  private String getAttributeValue(AttributeMap attribute, LedgerTransaction txn) {

    String obProperty = attribute.getObproperty();
    String result = null;
    if (obProperty.equals("REMID")) {
      result = txn.getId();
    } else if (obProperty.equals("VKEY")) {
      result = txn.getId();
    } else if (obProperty.equals("VTYPE")) {
      result = getVoucherType(txn.getDocumentType().getGLCategory());
    } else if (obProperty.equals("ACT")) {
      result = new String("Create");
    }
    return result;
  }

  private String getAttributeValue(AttributeMap attribute, RCCompany comp) {
    String obProperty = attribute.getObproperty();
    String result = null;
    if (obProperty.equals("CNAME")) {
      result = comp.getCompanyName();
    } else if (obProperty.equals("CALIAS")) {
      result = getLedgerAlias(comp);
    }
    return result;
  }

  /*
   * Retrieve the voucher type from the vendor mapping defined in the voucher type table in the
   * setup
   */
  private String getVoucherType(org.openbravo.model.financialmgmt.gl.GLCategory glCategory) {

    OBCriteria<GLCategory> vTypeCriteria = OBDal.getInstance().createCriteria(GLCategory.class);

    vTypeCriteria.add(Restrictions.eq(GLCategory.PROPERTY_GLCATEGORY, glCategory));
    vTypeCriteria.add(Restrictions.eq(GLCategory.PROPERTY_DFEXMAPDEFINITION, header));
    String voucherType;
    if (vTypeCriteria.list().size() == 0) {
      voucherType = glCategory.getName();
    } else {
      voucherType = vTypeCriteria.list().get(0).getCommercialName();
    }
    log4j.info("Voucher type for " + glCategory.getName() + " : " + voucherType);
    return voucherType;
  }

  private List<LedgerTransactionLeg> getLedgerList(LedgerTransaction txn) {

    OBCriteria<LedgerTransactionLeg> legCriteria = OBDal.getInstance().createCriteria(
        LedgerTransactionLeg.class);
    legCriteria.add(Restrictions.eq(LedgerTransactionLeg.PROPERTY_DJELEDGERTXN, txn));
    if (txn.getDocumentType().getGLCategory().getName().equals("AR Invoice")) {
      if (txn.getDocumentType().isReturn()) {
        legCriteria.addOrderBy(LedgerTransactionLeg.PROPERTY_CREDIT, false);
      } else {
        legCriteria.addOrderBy(LedgerTransactionLeg.PROPERTY_DEBIT, false);
      }
    } else if (txn.getDocumentType().getGLCategory().getName().equals("AP Invoice")) {
      if (txn.getDocumentType().isReturn()) {
        legCriteria.addOrderBy(LedgerTransactionLeg.PROPERTY_DEBIT, false);
      } else {
        legCriteria.addOrderBy(LedgerTransactionLeg.PROPERTY_CREDIT, false);
      }
    }

    if (legCriteria.list().size() == 0) {
      throw new OBException(txn.getDocumentNo() + ": This transaction has no legs");
    }
    return legCriteria.list();
  }

  private String getLedgerName(LedgerTransactionLeg txnLeg) {
    if (txnLeg.getLedgerName() == null || txnLeg.getLedgerName().equals("")) {
      log4j.info("LEDGER: Alias" + txnLeg.getLedgerAlias());
      return txnLeg.getLedgerAlias();
    } else {
      log4j.info("LEDGER: Name" + txnLeg.getLedgerName());
      return txnLeg.getLedgerName();
    }
  }

  private String getLedgerAlias(RCCompany comp) {
    if (comp.getDJECompanyAccountingList().size() == 0) {
      throw new OBException("Company accounting not defined for company " + comp.getCompanyName());
    }
    CompanyAccounting acct = comp.getDJECompanyAccountingList().get(0);
    ElementValue acctElem = acct.getCompanyReceivables().getAccount();

    return acctElem.getSearchKey();
  }

  private Date getEndOfDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DATE);
    calendar.set(year, month, day, 23, 59, 59);
    return calendar.getTime();
  }

  // This is used only for debugging to print the DOM document
  @SuppressWarnings("unused")
  private void printDocument() {
    // this cast is checked on Apache implementation (Xerces):
    DocumentTraversal traversal = (DocumentTraversal) doc;

    NodeIterator iterator = traversal.createNodeIterator(doc.getDocumentElement(),
        NodeFilter.SHOW_ELEMENT, null, true);

    for (Node n = iterator.nextNode(); n != null; n = iterator.nextNode()) {
      log4j.info("Element: " + ((Element) n).getTagName());
    }
  }
}
