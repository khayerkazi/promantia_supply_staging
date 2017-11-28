package in.decathlon.ibud.transfer.ad_process;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class ProcessReturnFromVendor extends DalBaseProcess {
  Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger.debug("inside process to return vendor process");
    OBError message = new OBError();
    message.setTitle("Return to vendor");

    String returnToVendorId = (String) bundle.getParams().get("C_Order_ID");
    Order returnToVendor = OBDal.getInstance().get(Order.class, returnToVendorId);

    try {
      if (returnToVendor.getDocumentStatus().equals("DR")) {
        logger.debug("call the process to complete 'return to vendor order'");
        BusinessEntityMapper.executeProcess(returnToVendorId, "109",
            "SELECT * FROM c_order_post(?)");
      }

      logger.debug("Create And Process Return To Vendor Shipment Header");
      ShipmentInOut RTVS = CreateAndProcessReturnToVendorShipment(returnToVendor);

      ReturnsForManualRTVS returnsForManualRTVS = new ReturnsForManualRTVS();
      returnsForManualRTVS.processVendorShipment(RTVS.getId());
      returnToVendor.setIbodtrReturnToVendor(true);
      message.setType("Success");
      message.setMessage("Successfully processed");
    } catch (Exception e) {
      e.printStackTrace();
      message.setType("Error");
      message.setTitle("Connection failure - " + e.getMessage());
    } finally {
      bundle.setResult(message);
    }
  }

  private ShipmentInOut CreateAndProcessReturnToVendorShipment(Order returnToVendor) {
    ShipmentInOut returnToVendorShipment = createReturnToVendorShipmentHeader(returnToVendor);
    createReturnToVendorShipmentLines(returnToVendorShipment, returnToVendor);
    OBDal.getInstance().flush();
    return returnToVendorShipment;

  }

  private void createReturnToVendorShipmentLines(ShipmentInOut returnToVendorShipment,
      Order returnToVendor) {
    for (OrderLine returnToVendorLine : returnToVendor.getOrderLineList()) {
      ShipmentInOutLine returnToVendorShipmentLine = OBProvider.getInstance().get(
          ShipmentInOutLine.class);
      returnToVendorShipmentLine.setActive(true);
      returnToVendorShipmentLine.setCreatedBy(returnToVendor.getCreatedBy());
      returnToVendorShipmentLine.setUpdatedBy(returnToVendor.getUpdatedBy());
      returnToVendorShipmentLine.setClient(returnToVendor.getClient());
      returnToVendorShipmentLine.setOrganization(returnToVendor.getOrganization());
      returnToVendorShipmentLine.setCreationDate(new Date());
      returnToVendorShipmentLine.setUpdated(new Date());
      returnToVendorShipmentLine.setShipmentReceipt(returnToVendorShipment);
      returnToVendorShipmentLine.setSalesOrderLine(returnToVendorLine);
      returnToVendorShipmentLine.setLineNo(new Long(returnToVendorShipment
          .getMaterialMgmtShipmentInOutLineList().size() * 10 + 10));
      if (returnToVendorShipment.getWarehouse().getReturnlocator() == null)
        throw new OBException("Please specify return bin to warehouse");
      returnToVendorShipmentLine.setStorageBin(returnToVendorShipment.getWarehouse()
          .getReturnlocator());
      returnToVendorShipmentLine.setProduct(returnToVendorLine.getProduct());
      returnToVendorShipmentLine.setUOM(returnToVendorLine.getUOM());
      returnToVendorShipmentLine.setMovementQuantity(returnToVendorLine.getOrderedQuantity());
      OBDal.getInstance().save(returnToVendorShipmentLine);
      returnToVendorShipment.getMaterialMgmtShipmentInOutLineList().add(returnToVendorShipmentLine);
      OBDal.getInstance().save(returnToVendorShipment);
    }
  }

  private ShipmentInOut createReturnToVendorShipmentHeader(Order returnToVendor) {
    ShipmentInOut returnToVendorShipment = OBProvider.getInstance().get(ShipmentInOut.class);
    returnToVendorShipment.setActive(true);
    returnToVendorShipment.setCreatedBy(returnToVendor.getCreatedBy());
    returnToVendorShipment.setUpdatedBy(returnToVendor.getUpdatedBy());
    returnToVendorShipment.setClient(returnToVendor.getClient());
    returnToVendorShipment.setOrganization(returnToVendor.getOrganization());
    returnToVendorShipment.setCreationDate(new Date());
    returnToVendorShipment.setUpdated(new Date());
    returnToVendorShipment.setSalesTransaction(false);
    returnToVendorShipment.setDocumentAction("CO");
    returnToVendorShipment.setDocumentStatus("DR");
    returnToVendorShipment.setPosted("N");
    returnToVendorShipment.setProcessed(false);
    returnToVendorShipment.setDocumentType(BusinessEntityMapper.getDocType("MMR", true));
    returnToVendorShipment.setDocumentNo(returnToVendor.getDocumentNo());
    returnToVendorShipment.setSalesOrder(returnToVendor);
    returnToVendorShipment.setMovementType("V+");
    returnToVendorShipment.setMovementDate(new Date());
    returnToVendorShipment.setAccountingDate(new Date());
    returnToVendorShipment.setBusinessPartner(returnToVendor.getBusinessPartner());
    returnToVendorShipment.setPartnerAddress(returnToVendor.getPartnerAddress());
    returnToVendorShipment.setWarehouse(returnToVendor.getWarehouse());
    returnToVendorShipment.setDeliveryTerms("A");
    returnToVendorShipment.setFreightCostRule("I");
    returnToVendorShipment.setFreightAmount(new BigDecimal(0));
    returnToVendorShipment.setSWMovement(SOConstants.manualReturnDocType);
    returnToVendorShipment.setIbodtrVaidate(true);
    OBDal.getInstance().save(returnToVendorShipment);
    return returnToVendorShipment;
  }

  private String getDocumentNo(String seqName) {
    OBCriteria<Sequence> obCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    obCriteria.add(Restrictions.eq(Sequence.PROPERTY_NAME, seqName));
    List<Sequence> sequences = obCriteria.list();
    if (sequences != null && sequences.size() > 0) {
      Sequence inOutSequence = sequences.get(0);
      Long docNo = inOutSequence.getNextAssignedNumber();
      inOutSequence.setNextAssignedNumber(docNo + 1);
      OBDal.getInstance().save(inOutSequence);
      return docNo.toString();
    }
    throw new OBException("Create Sequence Type - DocumentNo/Value for Table M_InOut");
  }

  private DocumentType getDocumentType(String docTypeName) {
    OBCriteria<DocumentType> obCriteria = OBDal.getInstance().createCriteria(DocumentType.class);
    obCriteria.add(Restrictions.eq(DocumentType.PROPERTY_NAME, docTypeName));
    List<DocumentType> documentTypes = obCriteria.list();
    if (documentTypes != null && documentTypes.size() > 0)
      return documentTypes.get(0);

    throw new OBException("Create Document Type - Inter Org RTV Shipment");
  }
}
