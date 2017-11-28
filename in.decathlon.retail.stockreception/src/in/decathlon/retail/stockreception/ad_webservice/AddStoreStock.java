package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONHelper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class AddStoreStock {
	static Logger log = Logger.getLogger(AddStoreStock.class);
	public void add(ShipmentInOut shipmentInOut) throws Exception, JSONException
	{
		BigDecimal movementQty = null;
	    JSONObject wsObj = new JSONObject();
	    JSONArray wsArray = new JSONArray();
	   

	    List<ShipmentInOutLine> listShipmentInOutLine = shipmentInOut
	        .getMaterialMgmtShipmentInOutLineList();
	   
	    DocumentType returnGRN = BusinessEntityMapper.getDocType("MMR", false);

	    ShipmentInOut goodsReceipt = createRMShipmentHeader(shipmentInOut, returnGRN);

	    for (ShipmentInOutLine ioLine : listShipmentInOutLine) {
	      wsArray = new JSONArray();
	      movementQty = ioLine.getMovementQuantity();
	      double mQty = movementQty.doubleValue();
	      double returnqty = mQty;//movement quantity will add as stock to store
	        OBDal.getInstance().save(goodsReceipt); // Goods receipt header created
	        long line = (goodsReceipt.getMaterialMgmtShipmentInOutLineList().size() + 1) * 10;
	        List<ShipmentInOutLine> grnLineList = createGrnLine(goodsReceipt, ioLine, returnqty, true,
	            line);
	        goodsReceipt.getMaterialMgmtShipmentInOutLineList().addAll(grnLineList);
	        OBDal.getInstance().save(goodsReceipt);      

	     
	    }

	    shipmentInOut.setIbodtrVaidate(true);
	    OBDal.getInstance().save(shipmentInOut);
	    OBDal.getInstance().flush();
	    JSONObject shipmentObj = new JSONObject();
	    if (goodsReceipt.getMaterialMgmtShipmentInOutLineList().size() > 0) {
	      shipmentObj = createShipmentJSONObj(goodsReceipt);
	      wsArray.put(0, shipmentObj);
	      wsObj.put("dataGRN", wsArray);
	    }

	    wsArray = new JSONArray();
	   

	    SessionHandler.getInstance().commitAndStart();
	    String content = wsObj.toString();
	    log.error(content);
	    if (content.length() > 0) {
	      String wsName = "in.decathlon.ibud.transfer.TransferWS";
	      JSONWebServiceInvocationHelper.sendPostrequest(wsName, "", content);
	      wsObj = new JSONObject();
	      OBDal.getInstance().flush();
	      if (goodsReceipt.getMaterialMgmtShipmentInOutLineList().size() > 0) {
	        BusinessEntityMapper.executeProcess(goodsReceipt.getId(), "109",
	            "SELECT * FROM M_InOut_Post0(?)");
	        //BusinessEntityMapper.txnSWMovementType(goodsReceipt);
	      }

	   
	    }

	    SessionHandler.getInstance().commitAndStart();
	}
	private ShipmentInOut createRMShipmentHeader(ShipmentInOut io, DocumentType docType)
		      throws Exception {
		    ShipmentInOut shipmentHeader = OBProvider.getInstance().get(ShipmentInOut.class);
		    try {
		      shipmentHeader.setClient(io.getClient());
		      shipmentHeader.setOrganization(io.getOrganization());
		      shipmentHeader.setActive(true);
		      shipmentHeader.setCreationDate(new Date());
		      shipmentHeader.setCreatedBy(io.getCreatedBy());
		      shipmentHeader.setUpdatedBy(io.getUpdatedBy());
		      shipmentHeader.setUpdated(new Date());
		      shipmentHeader.setBusinessPartner(io.getBusinessPartner());
		      shipmentHeader.setPartnerAddress(io.getBusinessPartner().getBusinessPartnerLocationList()
		          .get(0));
		      shipmentHeader.setMovementDate(new Date());
		      shipmentHeader.setDocumentStatus(SOConstants.DraftDocumentStatus);
		      shipmentHeader.setProcessed(false);
		      shipmentHeader.setIbodtrVaidate(true);
		      shipmentHeader.setSWMovement(SOConstants.SWMovement);
		      shipmentHeader.setDocumentType(docType);
		      shipmentHeader.setDocumentNo(io.getDocumentNo() + "*EXC");
		      shipmentHeader.setWarehouse(io.getWarehouse());
		      shipmentHeader.setProcessGoodsJava(SOConstants.CompleteDocumentStatus);
		      shipmentHeader.setAccountingDate(new Date());
		      shipmentHeader.setIbodtrVaidate(true);
		      shipmentHeader.setSalesTransaction(false);
		      shipmentHeader.setNewOBObject(true);

		    } catch (Exception e) {
		      log.error(e);
		      e.printStackTrace();
		      throw e;
		    }
		    return shipmentHeader;
		  }
	private JSONObject createShipmentJSONObj(ShipmentInOut aShipment) throws Exception {

	    JSONObject shipmentObj = new JSONObject();
	    JSONObject shipmentHeader = new JSONObject();
	    JSONArray shipmentLines = new JSONArray();
	    try {

	      shipmentHeader = JSONHelper.convetBobToJson(aShipment);
	      for (ShipmentInOutLine sl : aShipment.getMaterialMgmtShipmentInOutLineList()) {
	        shipmentLines.put(JSONHelper.convetBobToJson(sl));
	      }

	      shipmentObj.put("ReceiptHeader", shipmentHeader);
	      shipmentObj.put("ReceiptLines", shipmentLines);
	    } catch (Exception e) {
	      log.error("Problem Creating Shipment Json Data ", e);
	      e.printStackTrace();
	      throw e;

	    }
	    return shipmentObj;
	  }
	private List<ShipmentInOutLine> createGrnLine(ShipmentInOut rMShipment, ShipmentInOutLine ioLine,
		      double qty, boolean flag, long line) throws Exception {
		    List<ShipmentInOutLine> shipmentInOutLine = new ArrayList<ShipmentInOutLine>();

		    try {
		      double quantity = qty;
		      if (!flag)
		        quantity = quantity * -1;

		      BigDecimal qtyToBeOrdered = BigDecimal.valueOf(quantity);

		      Product pr = ioLine.getProduct();
		      ShipmentInOutLine newShipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
		      newShipmentLine.setClient(rMShipment.getClient());
		      newShipmentLine.setOrganization(rMShipment.getOrganization());
		      newShipmentLine.setActive(true);
		      newShipmentLine.setCreationDate(new Date());
		      newShipmentLine.setCreatedBy(getUser(SOConstants.User));
		      newShipmentLine.setUpdatedBy(getUser(SOConstants.User));
		      newShipmentLine.setUpdated(new Date());
		      newShipmentLine.setMovementQuantity(qtyToBeOrdered);
		      newShipmentLine.setShipmentReceipt(rMShipment);
		      newShipmentLine.setProduct(pr);
		      newShipmentLine.setUOM(pr.getUOM());

		      newShipmentLine.setIbodtrActmovementqty(qtyToBeOrdered);
		      if (ioLine.getShipmentReceipt().getWarehouse().getReturnlocator() == null) {
		        throw new OBException("No Return Bin defined for warehouse");
		      } else
		        newShipmentLine
		            .setStorageBin(ioLine.getShipmentReceipt().getWarehouse().getReturnlocator());
		      newShipmentLine.setLineNo(line);
		      shipmentInOutLine.add(newShipmentLine);

		    } catch (Exception e) {
		      log.error(e);
		      e.printStackTrace();
		      throw e;
		    }
		    return shipmentInOutLine;
		  }
	 private User getUser(String name) {
		    OBCriteria<User> userCrit = OBDal.getInstance().createCriteria(User.class);
		    userCrit.add(Restrictions.eq(PaymentTerm.PROPERTY_NAME, name));
		    List<User> userCritList = userCrit.list();
		    if (userCritList != null && userCritList.size() > 0) {
		      return userCritList.get(0);
		    } else {
		      throw new OBException("user not found");
		    }
		  }

}
