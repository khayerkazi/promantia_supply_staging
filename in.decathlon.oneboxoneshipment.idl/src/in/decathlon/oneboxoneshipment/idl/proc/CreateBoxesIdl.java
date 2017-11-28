package in.decathlon.oneboxoneshipment.idl.proc;

import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.PickingListBox;
import org.openbravo.warehouse.pickinglist.PickingListBoxContent;

public class CreateBoxesIdl extends IdlServiceJava {

  @Override
  protected String getEntityName() {

    return "CreateBoxesIdl";
  }

  @Override
  public Parameter[] getParameters() {

    return new Parameter[] { new Parameter("Picklist No", Parameter.STRING),
        new Parameter("Product", Parameter.STRING),
        new Parameter("Attribute Set Value", Parameter.STRING),
        new Parameter("Quantity", Parameter.STRING), new Parameter("Box Name", Parameter.STRING),
        new Parameter("Order Document No", Parameter.STRING)

    };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
	  Role role = OBContext.getOBContext().getRole();
	    Order salesOrder = getSoOnDocNo(values[5]);
	    if (role.getName().equals("Warehouse Manager") && salesOrder.isFacstIsDirectDelivery()) {
	      throw new OBException("Access Denied!");
	    }
    return values;
  }

  @Override
  protected BaseOBObject internalProcess(Object... values) throws Exception {
    return createBoxes((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5]);
  }

  private BaseOBObject createBoxes(String pickListNo, String product, String attributeSetValue,
      String quantity, String boxName, String orderDocumentNo) {

    PickingList pickList = getPickList(pickListNo);
    pickList.getPickliststatus();
    if (!(pickList.getPickliststatus().equalsIgnoreCase("AS"))) {
      throw new OBException("Pick List Is not in Assigned Status");
    }
    InternalMovementLine movementLine = getMovementLine(pickListNo, orderDocumentNo, product,
        attributeSetValue);
    validateBox(boxName, pickList);
    if (!(boxName.trim().matches("^((\\d)+)-((\\w)+)-((\\w)+)"))) {
      throw new OBException("Box Name Pattern mismatch");
    }
    BigDecimal numberOfUnitsPackagedOnOtherBoxes = getNumberOfUnitsPackagedOnOtherBoxes(movementLine);
    BigDecimal numberOfUnitsPackagedInCurrentBox = new BigDecimal(quantity);
    BigDecimal totalIncludingCurrentTransaction = numberOfUnitsPackagedOnOtherBoxes
        .add(numberOfUnitsPackagedInCurrentBox);
    if (!(movementLine.getMovementQuantity().compareTo(totalIncludingCurrentTransaction) >= 0)) {
      throw new OBException("The quantity that you want to add to the box is "
          + "greater than the movement line quantity");
    }

    return createBoxManageContents(pickList, movementLine, product, attributeSetValue, quantity,
        boxName, orderDocumentNo);
  }

  private BigDecimal getNumberOfUnitsPackagedOnOtherBoxes(InternalMovementLine movementLine) {

    String qry = " select coalesce(sum(quantity),0) as qtyinotherboxes from OBWPL_plBoxContent boxcontent "
        + " where boxcontent.movementLine.id=:movementLineId ";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    query.setParameter("movementLineId", movementLine.getId());
    List<BigDecimal> queryList = query.list();

    BigDecimal qtyInStock = BigDecimal.ZERO;
    if (queryList != null & queryList.size() > 0) {
      qtyInStock = queryList.get(0);
    }
    return qtyInStock;
  }

  private InternalMovementLine getMovementLine(String pickListNo, String orderDocumentNo,
      String product, String attributeSetValue) {
    String query = "id in (select mline from MaterialMgmtInternalMovementLine mline"
        + " join mline.salesOrderLine col  join col.salesOrder co where "
        + "co.documentNo =:orderDocNo and col.product.name =:productName and "
        + "mline.oBWPLWarehousePickingList.documentNo=:pickListNo ";

    Order salesOrder = getSoOnDocNo(orderDocumentNo);
    if (salesOrder.isFacstIsDirectDelivery()) {
      if (!(attributeSetValue.equals(""))) {
        query += " and mline.attributeSetValue.description='L" + attributeSetValue + "')";
      } else {
        query += " and mline.attributeSetValue.id='" + 0 + "')";

      }
    } else {
      query += " and mline.attributeSetValue.description='L" + attributeSetValue + "')";
    }

    OBQuery<InternalMovementLine> mlQuery = OBDal.getInstance().createQuery(
        InternalMovementLine.class, query);
    mlQuery.setNamedParameter("orderDocNo", orderDocumentNo);
    mlQuery.setNamedParameter("productName", product);
    mlQuery.setNamedParameter("pickListNo", pickListNo);

    List<InternalMovementLine> mlList = mlQuery.list();

    if (mlList != null && mlList.size() > 0) {
      return mlList.get(0);
    } else {
      throw new OBException("Data is incorrect in the CSV");
    }
  }

  public static Order getSoOnDocNo(String docNo) {
    try {
      OBCriteria<Order> ordCrit = OBDal.getInstance().createCriteria(Order.class);
      ordCrit.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, docNo));
      ordCrit.add(Restrictions
          .eq(Order.PROPERTY_DOCUMENTSTATUS, SOConstants.CompleteDocumentStatus));
      ordCrit.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
      ordCrit.addOrderBy(Order.PROPERTY_CREATIONDATE, false);
      return (Order) ordCrit.uniqueResult();
    } catch (Exception e) {
      e.printStackTrace();
      throw new OBException(
          "there is either multilple sales order or no sales order for the doc no : " + docNo);
    }

  }

  private void validateBox(String boxName, PickingList pickList) {

    String qry = "select pBox.id from OBWPL_plbox pBox where pBox.name='" + boxName
        + "' and pBox.obwplPickinglist.id <>'" + pickList.getId() + "'";
    Query query = OBDal.getInstance().getSession().createQuery(qry);
    List<String> queryList = query.list();
    if (queryList != null && queryList.size() > 0) {
      throw new OBException("There is already a box with name " + boxName + " Exists");
    }
    return;
  }

  private InternalMovementLine getMovement(PickingList pickList) {
    OBCriteria<InternalMovementLine> movementList = OBDal.getInstance().createCriteria(
        InternalMovementLine.class);

    movementList.add(Restrictions.eq(InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST,
        pickList));
    if (movementList.list() == null || movementList.list().size() <= 0) {
      throw new OBException("No MovementLine records under picklist" + pickList.getDocumentNo());
    }
    return movementList.list().get(0);

  }

  private BaseOBObject createBoxManageContents(PickingList pickList,
      InternalMovementLine movementLine, String product, String attributeSetValue, String quantity,
      String boxName, String orderDocumentNo) {
    if (boxUnderGivenPicklistExists(pickList, boxName) != null) {
      PickingListBox boxObj = boxUnderGivenPicklistExists(pickList, boxName);
      if (ConentUnderGivenPicklistExists(boxObj, product, attributeSetValue, movementLine) != null) {

        PickingListBoxContent conentObj = ConentUnderGivenPicklistExists(boxObj, product,
            attributeSetValue, movementLine);
        updateContent(boxObj, conentObj, quantity);
      } else {
        addContents(boxObj, movementLine, quantity, product, attributeSetValue);

      }
    } else {
      createBoxAddTheContents(pickList, movementLine, product, attributeSetValue, quantity, boxName);
    }
    return pickList;

  }

  private PickingList createBoxAddTheContents(PickingList pickList,
      InternalMovementLine movementLine, String product, String attributeSetValue, String quantity,
      String boxName) {

    PickingListBox boxObj = OBProvider.getInstance().get(PickingListBox.class);
    boxObj.setOrganization(pickList.getOrganization());
    boxObj.setClient(OBContext.getOBContext().getCurrentClient());
    boxObj.setActive(true);
    boxObj.setCreationDate(new Date());
    boxObj.setCreatedBy(OBContext.getOBContext().getUser());
    boxObj.setUpdated(new Date());
    boxObj.setUpdatedBy(OBContext.getOBContext().getUser());
    boxObj.setDescription("Picking Box");
    boxObj.setBoxno(new Long((pickList.getOBWPLPlboxList().size() * 1 + 1)));
    boxObj.setName(boxName);
    boxObj.setSearchKey(boxName);
    boxObj.setObwplPickinglist(pickList);
    OBDal.getInstance().save(boxObj);
    pickList.getOBWPLPlboxList().add(boxObj);
    OBDal.getInstance().save(pickList);
    OBDal.getInstance().flush();
    addContents(boxObj, movementLine, quantity, product, attributeSetValue);
    OBDal.getInstance().flush();
    return pickList;

  }

  private void updateContent(PickingListBox boxObj, PickingListBoxContent conentObj, String quantity) {
    BigDecimal updatedQty = conentObj.getQuantity().add(new BigDecimal(quantity));
    conentObj.setQuantity(updatedQty);

    conentObj.setUpdated(new Date());
    OBDal.getInstance().save(conentObj);
    OBDal.getInstance().save(boxObj);
    OBDal.getInstance().flush();
  }

  private PickingListBoxContent ConentUnderGivenPicklistExists(
      PickingListBox boxUnderGivenPicklistExists, String product, String attributeSetValue,
      InternalMovementLine movementLine) {

    OBCriteria<PickingListBoxContent> contenList = OBDal.getInstance().createCriteria(
        PickingListBoxContent.class);
    contenList.add(Restrictions.eq(PickingListBoxContent.PROPERTY_OBWPLPLBOX,
        boxUnderGivenPicklistExists));
    contenList.add(Restrictions.eq(PickingListBoxContent.PROPERTY_PRODUCT, getProduct(product)));
    Order salesOrder = getSoOnDocNo(movementLine.getSalesOrderLine().getSalesOrder()
        .getDocumentNo());
    if (salesOrder.isFacstIsDirectDelivery()) {
      if (!(attributeSetValue.equals(""))) {
        contenList.add(Restrictions.eq(PickingListBoxContent.PROPERTY_ATTRIBUTESETVALUE,
            getAttributeSetValue(movementLine, attributeSetValue)));
      }
    } else {
      contenList.add(Restrictions.eq(PickingListBoxContent.PROPERTY_ATTRIBUTESETVALUE,
          getAttributeSetValue(movementLine, attributeSetValue)));
    }
    contenList.add(Restrictions.eq(PickingListBoxContent.PROPERTY_SALESORDERLINE, movementLine
        .getStockReservation().getSalesOrderLine()));
    if (contenList.list() != null && contenList.list().size() > 0) {
      return contenList.list().get(0);

    }
    return null;

  }

  private PickingListBox boxUnderGivenPicklistExists(PickingList pickList, String boxName) {

    OBCriteria<PickingListBox> boxObj = OBDal.getInstance().createCriteria(PickingListBox.class);
    boxObj.add(Restrictions.eq(PickingListBox.PROPERTY_NAME, boxName));
    if (boxObj.list() != null && boxObj.list().size() > 0) {
      return boxObj.list().get(0);

    }
    return null;

  }

  private void addContents(PickingListBox boxObj, InternalMovementLine movementLine,
      String quantity, String product, String attributeSetValue) {
    PickingListBoxContent boxContentObj = OBProvider.getInstance().get(PickingListBoxContent.class);
    boxContentObj.setOrganization(boxObj.getOrganization());
    boxContentObj.setClient(OBContext.getOBContext().getCurrentClient());
    boxContentObj.setActive(true);
    boxContentObj.setCreationDate(new Date());
    boxContentObj.setCreatedBy(OBContext.getOBContext().getUser());
    boxContentObj.setUpdated(new Date());
    boxContentObj.setUpdatedBy(OBContext.getOBContext().getUser());
    boxContentObj.setObwplPlbox(boxObj);
    boxContentObj.setMovementLine(movementLine);
    boxContentObj.setQuantity(new BigDecimal(quantity));
    boxContentObj.setProduct(getProduct(product));
    Order salesOrder = getSoOnDocNo(movementLine.getSalesOrderLine().getSalesOrder()
        .getDocumentNo());
    if (salesOrder.isFacstIsDirectDelivery()) {
      if (!(attributeSetValue.equals(""))) {
        boxContentObj.setAttributeSetValue(getAttributeSetValue(movementLine, attributeSetValue));
      }
    } else {
      boxContentObj.setAttributeSetValue(getAttributeSetValue(movementLine, attributeSetValue));
    }
    if (movementLine.getStockReservation() != null) {
      if (movementLine.getStockReservation().getSalesOrderLine() != null) {
        boxContentObj.setSalesOrderLine(movementLine.getStockReservation().getSalesOrderLine());
      } else {
        throw new OBException(
            "no orderline assigned to reservation, please check the reservation record "
                + movementLine);
      }
    } else {
      throw new OBException("No reservation assigned in movementLine " + movementLine);
    }
    boxObj.getOBWPLPlBoxContentList().add(boxContentObj);
    OBDal.getInstance().save(boxContentObj);
    OBDal.getInstance().save(boxObj);
    OBDal.getInstance().flush();
  }

  private AttributeSetInstance getAttributeSetValue(InternalMovementLine movement,
      String attributeSetValue) {

    
    String movementLineDesc = movement.getAttributeSetValue().getDescription() == null ? ""
        : movement.getAttributeSetValue().getDescription();
    if (!(("L" + attributeSetValue).equalsIgnoreCase(movementLineDesc))) {
      throw new OBException("AttributeSetInsatance in the CSV and MovementLine are not same");
    }
    return movement.getAttributeSetValue();

  

  }

  private Product getProduct(String product) {

    OBCriteria<Product> productObj = OBDal.getInstance().createCriteria(Product.class);
    productObj.add(Restrictions.eq(Product.PROPERTY_NAME, product));
    if (productObj.list() == null || productObj.list().size() <= 0) {
      throw new OBException("there is no product with name " + product);
    }
    return productObj.list().get(0);
  }

  private PickingList getPickList(String pickListNo) {
    OBCriteria<PickingList> pickList = OBDal.getInstance().createCriteria(PickingList.class);
    pickList.add(Restrictions.eq(PickingList.PROPERTY_DOCUMENTNO, pickListNo));
    if (pickList.list() == null || pickList.list().size() <= 0) {
      throw new OBException("PickList With DocumentNo " + " " + pickListNo + " "
          + " doesn't exists");
    }
    return pickList.list().get(0);
  }
}
