package in.decathlon.ibud.transfer.idl;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.replenishment.ReplenishmentDalUtils;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sysfore.catalog.CLModel;

public class ReturnsToVendorIdl extends IdlServiceJava{
	String prevOrgName="";
	Order order = null;
	long lineNo=0;
	ReplenishmentDalUtils ordUtils = new ReplenishmentDalUtils();
	private static Logger log = Logger.getLogger(ReturnsToVendorIdl.class);

	@Override
	protected String getEntityName() {
		return "Vendor";
	}

	@Override
	public Parameter[] getParameters() {
	    return new Parameter[] { new Parameter("Organization", Parameter.STRING),
	        new Parameter("Product", Parameter.STRING),
	        new Parameter("ReturnQty", Parameter.STRING)};
	  }
	
	protected Object[] validateProcess(Validator validator, String... values)
			throws Exception {
		return values;
	}

	@Override
	protected BaseOBObject internalProcess(Object... values) throws Exception {
		return createGs((String) values[0], (String) values[1], (String) values[2]);
	}

	private BaseOBObject createGs(String org, String product, String returnQty) throws Exception {
		Product prd = BusinessEntityMapper.getproductOnName(product);
		if(!prevOrgName.equals(org)){
			if(!prevOrgName.equals("")){
				lineNo = 0;
				OBDal.getInstance().save(order);
				log.debug("Created RTV for: "+order.getDocumentNo());
				prevOrgName = org;
				order = createRTVHeader(org,prd.getClModel());
			}
			else{
				order = createRTVHeader(org,prd.getClModel());
				prevOrgName = org;
			}
		}
		
		BigDecimal retQty = new BigDecimal(returnQty);
		BigDecimal negRetQty = retQty.negate();
		ordUtils.createOrderLines(prd, negRetQty, order, lineNo+=10);
		
		if(order != null){
			OBDal.getInstance().save(order);
		}
		
		return order;
	}


	private Order createRTVHeader(String orgName, CLModel clModel) throws Exception {
		Order rtvHeader = OBProvider.getInstance().get(Order.class);
		boolean isImplanted=false;
		Organization org = BusinessEntityMapper.getOrgOnName(orgName);
		Warehouse wr = BusinessEntityMapper.getOrgWarehouse(org.getId()).getWarehouse();
		DocumentType docType = BusinessEntityMapper.getDocType("POO", true);
		Sequence sequence =docType.getDocumentSequence();
		rtvHeader = ordUtils.createPurchaseOrderHeader(org, wr, clModel, sequence, isImplanted);
		if(sequence==null){
			throw new OBException("There is no document sequence for: "+docType.getName());
		}
	   
	    Long curDocNo = sequence.getNextAssignedNumber();
	    sequence.setNextAssignedNumber(curDocNo + sequence.getIncrementBy());
	    OBDal.getInstance().save(sequence);
	    OBDal.getInstance().flush();
	    OBDal.getInstance().refresh(sequence);
	    rtvHeader.setDocumentNo(orgName +"*"+ curDocNo.toString()+"*SRN");
		rtvHeader.setTransactionDocument(docType);
		rtvHeader.setDocumentType(docType);
		rtvHeader.setSwIsautoOrder(false);
		rtvHeader.setIbodtrReturnToVendor(false);
		rtvHeader.setIbodtrIsCreatedbyidl(true);
		
		
		return rtvHeader;
	}
	


}
