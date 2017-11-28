package in.decathlon.ibud.transfer.ad_process;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;

public class ImportRequisitionCorrectionIDL extends IdlServiceJava {

  HashSet<ShipmentInOut> shipmentInOuts = new HashSet<ShipmentInOut>();

  @Override
  protected String getEntityName() {
    return "Import Requisition Correction";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Document No", Parameter.STRING),
        new Parameter("Item Code", Parameter.STRING),
        new Parameter("Qty received", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    String documentNo = values[0];
    String itemCode = values[1];
    String qtyReceived = values[2];

    ShipmentInOut goodsReceipt = checkGrnExists(documentNo);
    checkGrnLineExistsForProduct(goodsReceipt, itemCode);

    System.out.println("validate : document no - " + documentNo + " ,item code - " + itemCode
        + ",qtyReceived -" + qtyReceived);
    return values;
  }

  protected void postProcess() throws Exception {
    Iterator<ShipmentInOut> shipmentIterator = shipmentInOuts.iterator();
    while (shipmentIterator.hasNext()) {
      ShipmentInOut shipmentInOut = (ShipmentInOut) shipmentIterator.next();
      System.out.println(shipmentInOut.getDocumentNo());
      Transfer transfer = new Transfer();
      try {
        transfer.processValidation(shipmentInOut);
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

  }

  private ShipmentInOutLine checkGrnLineExistsForProduct(ShipmentInOut goodsReceipt, String itemCode) {

    Product Product = getProduct(itemCode);

    OBCriteria<ShipmentInOutLine> criteriaOnGrn = OBDal.getInstance().createCriteria(
        ShipmentInOutLine.class);
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, goodsReceipt));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_PRODUCT, Product));

    List<ShipmentInOutLine> inOutLines = criteriaOnGrn.list();
    if (inOutLines != null && inOutLines.size() > 0) {
      if (inOutLines.size() > 1)
        throw new OBException("More than one Line for same GRN " + goodsReceipt.getDocumentNo());
      return inOutLines.get(0);
    }
    throw new OBException("Grn line with Product " + Product
        + " in Goods receipt for docuement no " + goodsReceipt.getDocumentNo() + " does not exists");
  }

  private Product getProduct(String itemCode) {
    OBCriteria<Product> criteriaOnPrd = OBDal.getInstance().createCriteria(Product.class);
    criteriaOnPrd.add(Restrictions.eq(Product.PROPERTY_NAME, itemCode));

    List<Product> prdList = criteriaOnPrd.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    }
    throw new OBException(" Product with item code " + itemCode + "  does not exists");
  }

  private ShipmentInOut checkGrnExists(String documentNo) {
    OBCriteria<ShipmentInOut> criteriaOnGrn = OBDal.getInstance().createCriteria(
        ShipmentInOut.class);
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, documentNo.trim()));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "CO"));
    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_IBODTRISAUTOMATIC, true));
    List<ShipmentInOut> inOuts = criteriaOnGrn.list();
    if (inOuts != null && inOuts.size() > 0) {
      if (inOuts.size() > 1)
        throw new OBException("More than one Goods receipt for same document no " + documentNo);
      return inOuts.get(0);
    }
    throw new OBException("Goods receipt for document no " + documentNo
        + "Not exists/Not Completed/Not an automatic receipt");
  }

  @Override
  protected BaseOBObject internalProcess(Object... values) throws Exception {
    return processImportRequisitionCorrection((String) values[0], (String) values[1],
        (String) values[2]);
  }

  private BaseOBObject processImportRequisitionCorrection(String documentNo, String itemCode,
      String strQtyReceived) {
    ShipmentInOut goodsReceipt = checkGrnExists(documentNo);
    ShipmentInOutLine goodsReceiptLine = checkGrnLineExistsForProduct(goodsReceipt, itemCode);
    BigDecimal qtyReceived = new BigDecimal(strQtyReceived);
    goodsReceiptLine.setIbodtrActmovementqty(qtyReceived);
    OBDal.getInstance().save(goodsReceiptLine);
    // OBDal.getInstance().flush();
    shipmentInOuts.add(goodsReceipt);
    return goodsReceipt;
  }

}
