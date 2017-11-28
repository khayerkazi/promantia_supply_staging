package in.decathlon.retail.inventorycorrectiontool.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Expression;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * This Web Service is used to upload the stock to ERP
 */

public class StockUploadWebService extends HttpSecureAppServlet implements WebService {

  private static Logger LOGGER = Logger.getLogger(StockUploadWebService.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Performs the POST REST operation. This service handles the request for the XML Schema related
   * to stock upload.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String xml = "";
    String strRead = "";

    InputStreamReader isReader = new InputStreamReader(request.getInputStream());
    BufferedReader bReader = new BufferedReader(isReader);
    StringBuilder strBuilder = new StringBuilder();
    JSONObject jsonDataObject = new JSONObject();
    strRead = bReader.readLine();
    while (strRead != null) {
      strBuilder.append(strRead);
      strRead = bReader.readLine();
    }
    xml = strBuilder.toString();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);

    try {
      jsonDataObject = uploadStockXML(document);
    } catch (Exception e) {
      LOGGER.error(e);
    }
    response.setContentType("text/json");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());
    w.close();

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

  /**
   * This method parses the xml tags and returns the json object containing the stock upload
   * 
   * @param stockUploadXML
   * @return jsonDataObject
   */
  public JSONObject uploadStockXML(Document stockUploadXML) throws Exception {
    JSONObject jsonDataObject = new JSONObject();
    String userId = null;
    String adOrgId = null;
    String inventoryType = null;
    String locatorId = null;
    InventoryCorrectionToolDTO ictHeaderDTO = new InventoryCorrectionToolDTO();
    List<InventoryCorrectionToolDTO> ictDTO = new ArrayList<InventoryCorrectionToolDTO>();

    try {
      adOrgId = stockUploadXML.getElementsByTagName("ad_org_id").item(0).getChildNodes().item(0)
          .getNodeValue();
      userId = stockUploadXML.getElementsByTagName("ad_user_id").item(0).getChildNodes().item(0)
          .getNodeValue();
      inventoryType = stockUploadXML.getElementsByTagName("inventoryType").item(0).getChildNodes()
          .item(0).getNodeValue();
      locatorId = stockUploadXML.getElementsByTagName("m_locator_id").item(0).getChildNodes()
              .item(0).getNodeValue();
      
      ictHeaderDTO.setAdOrgId(adOrgId);
      ictHeaderDTO.setAdUserId(userId);
      ictHeaderDTO.setInventoryType(inventoryType);

      LOGGER.debug("Receiving Stocks to upload: " + new Date());
      // handling multiple lines
      NodeList list = stockUploadXML.getElementsByTagName("line");
      for (int indexParent = 0; indexParent < list.getLength(); indexParent++) {
        Node fieldNode = list.item(indexParent);
        NodeList listChildren = fieldNode.getChildNodes();
        InventoryCorrectionToolDTO ictLinesDTO = new InventoryCorrectionToolDTO();
        for (int indexChild = 0; indexChild < listChildren.getLength(); indexChild++) {
          if (listChildren.item(indexChild).getNodeName().equals("m_product_id")) {
            ictLinesDTO.setmProductId(listChildren.item(indexChild).getChildNodes().item(0)
                .getNodeValue().toString());
          }
          if (listChildren.item(indexChild).getNodeName().equals("invqty")) {
            ictLinesDTO.setInvQty(new BigDecimal(listChildren.item(indexChild).getChildNodes()
                .item(0).getNodeValue()));
          }
          ictLinesDTO.setmLocatorId(locatorId);
        }
        ictDTO.add(ictLinesDTO);
      }
      // insert inventory header
      LOGGER.debug("Creating Inventory Header: " + new Date());
      InventoryCount inv = createInvtory(ictHeaderDTO);
      LOGGER.debug("Inventory Header created->" + inv.getName() + ":" + new Date());
      // insert inventory lines
      int line = 10;
      LOGGER.debug("Creating Inventory Lines:Size-> " + ictDTO.size() + new Date());
      for (InventoryCorrectionToolDTO ict : ictDTO) {
        createInvtoryLine(ict, inv, line);
        line += 10;
      }
      LOGGER.debug("Inventory Lines created: " + new Date());
      LOGGER.debug("Calling Inventory post: " + new Date());
      // posting inventory
      callInventoryPost(inv);
      jsonDataObject.put("message", "Stocks Uploaded");
      jsonDataObject.put("status", "success");
    } catch (Exception exp) {
      LOGGER.error("Exception while reading stocks to upload. ", exp);
      jsonDataObject.put("message", exp.getMessage());
      jsonDataObject.put("message", "Stocks failed to upload");
      jsonDataObject.put("status", "failure");
    }
    return jsonDataObject;

  }

  private void callInventoryPost(InventoryCount inv) {
    InventoryCountProcess icp = new InventoryCountProcess();
    OBError msg = icp.processInventory(inv);
    if (msg.getType().equals("Success")) {
      inv.setProcessed(false);
      OBDal.getInstance().save(inv);
      List<Object> param = new ArrayList<Object>();
      param.add(getPInstanceId(inv));
      CallStoredProcedure.getInstance().call("m_inventory_post", param, null, true, false);
      LOGGER.debug("Inventory posted successfully: " + new Date());
    } else {
      LOGGER.debug("Inventory process failed: " + new Date() + "->" + msg.getMessage().toString());
    }
  }

  // method to create Inventory header
  private InventoryCount createInvtory(InventoryCorrectionToolDTO ictHeaderDTO) {
    Organization org = OBDal.getInstance().get(Organization.class, ictHeaderDTO.getAdOrgId());
    User user = OBDal.getInstance().get(User.class, ictHeaderDTO.getAdUserId());
    InventoryCount inv = OBProvider.getInstance().get(InventoryCount.class);
    inv.setActive(true);
    inv.setClient(org.getClient());
    inv.setOrganization(org);
    inv.setUpdated(new Date());
    inv.setCreationDate(new Date());
    inv.setUpdatedBy(user);
    inv.setCreatedBy(user);
    inv.setMovementDate(new Date());
    inv.setWarehouse(getWarehouse(org));
    if (ictHeaderDTO.getInventoryType().equals("PI")) {
      inv.setName(getDocSequenceNo("PI"));
      inv.setSwMovementtype("PI");
    } else if (ictHeaderDTO.getInventoryType().equals("I")) {
      inv.setSwMovementtype("I");
      inv.setName(getDocSequenceNo("I"));
    }
    inv.setDescription("Inventory Correction Tool");
    inv.setGenerateList(true);
    OBDal.getInstance().save(inv);
    return inv;
  }

  // method to create Inventory line
  private void createInvtoryLine(InventoryCorrectionToolDTO ict, InventoryCount inv, int lineNo) {
    Product p = OBDal.getInstance().get(Product.class, ict.getmProductId());
    AttributeSetInstance asi = OBDal.getInstance().get(AttributeSetInstance.class, "0");
    org.openbravo.model.common.enterprise.Locator loc = OBDal.getInstance().get(
        org.openbravo.model.common.enterprise.Locator.class, ict.getmLocatorId());
    InventoryCountLine invLine = OBProvider.getInstance().get(InventoryCountLine.class);
    invLine.setActive(true);
    invLine.setClient(inv.getClient());
    invLine.setOrganization(inv.getOrganization());
    invLine.setUpdated(new Date());
    invLine.setCreationDate(new Date());
    invLine.setUpdatedBy(inv.getUpdatedBy());
    invLine.setCreatedBy(inv.getCreatedBy());
    invLine.setPhysInventory(inv);
    invLine.setProduct(p);
    invLine.setUOM(p.getUOM());
    invLine.setAttributeSetValue(asi);
    invLine.setLineNo((long) lineNo);
    invLine.setStorageBin(loc);
    invLine.setDescription("Inventory Correction Tool Line");
    invLine.setQuantityCount(ict.getInvQty());
    invLine.setBookQuantity(getQty(p, loc));

    OBDal.getInstance().save(invLine);
  }

  // getting pInstanceId for Inventory Post
  private String getPInstanceId(InventoryCount inv) {
    String pid = null;
    OBContext.setAdminMode(true);
    try {
      Process process = OBDal.getInstance().get(Process.class, "107");// M_Inventory Post
      ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
      pInstance.setActive(true);
      pInstance.setClient(inv.getClient());
      pInstance.setOrganization(inv.getOrganization());
      pInstance.setCreationDate(new Date());
      pInstance.setUpdated(new Date());
      pInstance.setCreatedBy(inv.getCreatedBy());
      pInstance.setUpdatedBy(inv.getUpdatedBy());
      pInstance.setUserContact(inv.getCreatedBy());
      pInstance.setProcess(process);
      pInstance.setRecordID(inv.getId());
      pInstance.setErrorMsg("");
      pInstance.setResult((long) 1);
      OBDal.getInstance().save(pInstance);
      pid = pInstance.getId();
    } finally {
      OBContext.restorePreviousMode();
    }
    return pid;
  }

  private BigDecimal getQty(Product p, Locator loc) {
    BigDecimal bookQty = new BigDecimal(0);
    OBCriteria<StorageDetail> obCriteria = OBDal.getInstance().createCriteria(StorageDetail.class);
    obCriteria.add(Expression.eq(StorageDetail.PROPERTY_PRODUCT, p));
    obCriteria.add(Expression.eq(StorageDetail.PROPERTY_STORAGEBIN, loc));
    // obCriteria.add(Expression.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE, asi));

    final List<StorageDetail> sdList = obCriteria.list();
    if (!sdList.isEmpty())
      bookQty = sdList.get(0).getQuantityOnHand();
    return bookQty;
  }

  // getting Warehouse using Organization
  private Warehouse getWarehouse(Organization org) {
    Warehouse w = null;
    OBCriteria<Warehouse> obCriteria = OBDal.getInstance().createCriteria(Warehouse.class);
    obCriteria.add(Expression.eq(Warehouse.PROPERTY_ORGANIZATION, org));
    if (org.getName().equals("BGT"))// Bannerghatta
      obCriteria.add(Expression.eq(Warehouse.PROPERTY_NAME, "Saleable" + " " + "Bannerghatta"));
    else
      obCriteria.add(Expression.eq(Warehouse.PROPERTY_NAME, "Saleable" + " "
          + org.getName().replaceAll("Store", "").trim()));
    final List<Warehouse> wList = obCriteria.list();
    if (!wList.isEmpty())
      w = wList.get(0);
    else
      throw new OBException("Warehouse not found for Organization->" + org.getName());
    return w;
  }

  // getting documentNo for Inventory
  private String getDocSequenceNo(String type) {
    Sequence seq = null;
    String strDocNo = "";
    OBCriteria<Sequence> obCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    if (type.equals("I"))
      obCriteria.add(Expression.eq(Sequence.PROPERTY_NAME, "Full Inventory"));
    else
      obCriteria.add(Expression.eq(Sequence.PROPERTY_NAME, "Partial Inventory"));

    final List<Sequence> seqList = obCriteria.list();
    if (!seqList.isEmpty()) {
      seq = seqList.get(0);
      strDocNo = seq.getPrefix().concat(seq.getNextAssignedNumber().toString());

      // to increment the next documentNo in the Document Sequence
      String dNo = strDocNo;
      if (type.equals("I"))
        seq.setNextAssignedNumber(Long.parseLong(dNo.replaceAll("I-", "").trim()) + 1);
      else
        seq.setNextAssignedNumber(Long.parseLong(dNo.replaceAll("PI-", "").trim()) + 1);
      OBDal.getInstance().save(seq);
    } else
      throw new OBException("Document Sequence not created in ERP for Inventory");

    return strDocNo;
  }
}
