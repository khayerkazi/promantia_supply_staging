package in.decathlon.supply.dc.util;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;

import com.sysfore.catalog.CLStoreDept;

public class SDC_Utils {

	/**
	   * Returns the DocumentType defined for the Organization (or parent organization tree) and
	   * document category.
	   * 
	   * @param org
	   *          the Organization for which the Document Type is defined. The Document Type can belong
	   *          to the parent organization tree of the specified Organization.
	   * @param docCategory
	   *          the document category of the Document Type.
	   * @return the Document Type
	   */
	  public static DocumentType getDocumentType(Organization org, String docCategory) {
	    DocumentType outDocType = null;
	    Client client = null;

	    OBCriteria<DocumentType> obcDoc = OBDal.getInstance().createCriteria(DocumentType.class);
	    obcDoc.setFilterOnReadableClients(false);
	    obcDoc.setFilterOnReadableOrganization(false);

	    if ("0".equals(org.getId())) {
	      client = OBContext.getOBContext().getCurrentClient();
	      if ("0".equals(client.getId())) {
	        return null;
	      }
	    } else {
	      client = org.getClient();
	    }
	    obcDoc.add(Restrictions.eq(DocumentType.PROPERTY_CLIENT, client));

	    obcDoc
	        .add(Restrictions.in("organization.id",
	            OBContext.getOBContext().getOrganizationStructureProvider(org.getClient().getId())
	                .getParentTree(org.getId(), true)));
	    obcDoc.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, docCategory));
	    obcDoc.addOrderBy(DocumentType.PROPERTY_DEFAULT, false);
	    obcDoc.addOrderBy(DocumentType.PROPERTY_ID, false);
	    List<DocumentType> docTypeList = obcDoc.list();
	    if (docTypeList != null && docTypeList.size() > 0) {
	      outDocType = docTypeList.get(0);
	    }
	    return outDocType;
	  }

	  /**
	   * Returns the next sequence number of the Document Type defined for the Organization and document
	   * category. The current number of the sequence is also updated.
	   * 
	   * @param docType
	   *          Document type of the document
	   * @return the next sequence number of the Document Type defined for the Organization and document
	   *         category. Null if no sequence is found.
	   */
	  public static String getDocumentNo(DocumentType docType, String tableName, String OrgId, String storeDeptId) {
	    String nextDocNumber = "";
	    String storeValue="";
	    Organization org = null;
	    CLStoreDept storeDept = null;
	    if(!"".equals(OrgId)) {
	    	org = OBDal.getInstance().get(Organization.class, OrgId);
	    }
	    if(!"".equals(storeDeptId)) {
	    	storeDept = OBDal.getInstance().get(CLStoreDept.class, storeDeptId);
	    }
	    if (docType != null) {
	      Sequence seq = docType.getDocumentSequence();
	      if (seq == null && tableName != null) {
	        OBCriteria<Sequence> obcSeq = OBDal.getInstance().createCriteria(Sequence.class);
	        obcSeq.add(Restrictions.eq(Sequence.PROPERTY_NAME, tableName));
	        if (obcSeq != null && obcSeq.list().size() > 0) {
	          seq = obcSeq.list().get(0);
	        }
	      }
	      if (seq != null) {
//	        if (seq.getPrefix() != null)
//	          nextDocNumber = seq.getPrefix();
	    	  if("1".equals(org.getSearchKey())) {
	    		  storeValue = "SJ";
	    	  } else if("5".equals(org.getSearchKey())) {
	    		  storeValue = "BGT";
	    	  } else {
	    		  storeValue = org.getSearchKey();
	    	  }
	    	  if(null != storeDept) {
	    		  if (seq.getPrefix() != null)
	    			  nextDocNumber = storeValue+"-"+storeDept.getName().substring(0, 3)+"-"+seq.getPrefix();
	    	  } else {
	    		  nextDocNumber = storeValue+"-"+seq.getPrefix();
	    	  }
	    	  
	        nextDocNumber += seq.getNextAssignedNumber().toString();
	        if (seq.getSuffix() != null)
	          nextDocNumber += seq.getSuffix();
	        seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
	        OBDal.getInstance().save(seq);
	        // OBDal.getInstance().flush();
	      }
	    }

	    return nextDocNumber;
	  }
	  public static String getDocumentNoOld(DocumentType docType, String tableName) {
		    String nextDocNumber = "";
		    if (docType != null) {
		      Sequence seq = docType.getDocumentSequence();
		      if (seq == null && tableName != null) {
		        OBCriteria<Sequence> obcSeq = OBDal.getInstance().createCriteria(Sequence.class);
		        obcSeq.add(Restrictions.eq(Sequence.PROPERTY_NAME, tableName));
		        if (obcSeq != null && obcSeq.list().size() > 0) {
		          seq = obcSeq.list().get(0);
		        }
		      }
		      if (seq != null) {
		        if (seq.getPrefix() != null)
		          nextDocNumber = seq.getPrefix();
		        nextDocNumber += seq.getNextAssignedNumber().toString();
		        if (seq.getSuffix() != null)
		          nextDocNumber += seq.getSuffix();
		        seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
		        OBDal.getInstance().save(seq);
		        // OBDal.getInstance().flush();
		      }
		    }

		    return nextDocNumber;
		  }
}
