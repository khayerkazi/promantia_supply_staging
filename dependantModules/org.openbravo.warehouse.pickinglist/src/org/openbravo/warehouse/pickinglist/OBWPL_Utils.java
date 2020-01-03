/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2012-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.warehouse.pickinglist;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StockProposed;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.CallProcess;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.PickingListProblem;
import org.openbravo.warehouse.pickinglist.PickingListProblemOrderLines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OBWPL_Utils {
  final private static String MOVEMENT_NAME = OBMessageUtils.messageBD("OBWPL_MovementName", false);
  final private static Logger log = LoggerFactory.getLogger(OBWPL_Utils.class);

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
    return getDocumentType(org, docCategory, false);
  }

  public static DocumentType getDocumentType(Organization org, String docCategory,
      boolean useOutbound) {
    Client client = null;

    if ("0".equals(org.getId())) {
      client = OBContext.getOBContext().getCurrentClient();
      if ("0".equals(client.getId())) {
        return null;
      }
    } else {
      client = org.getClient();
    }
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider(
        client.getId());

    OBCriteria<DocumentType> critDoc = OBDal.getInstance().createCriteria(DocumentType.class);
    critDoc.setFilterOnReadableClients(false);
    critDoc.setFilterOnReadableOrganization(false);
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_CLIENT, client));
    critDoc.add(Restrictions.in("organization.id", osp.getParentTree(org.getId(), true)));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, docCategory));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_OBWPLUSEOUTBOUND, useOutbound));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_OBWPLISGROUP, false));
    critDoc.addOrderBy(DocumentType.PROPERTY_DEFAULT, false);
    critDoc.addOrderBy(DocumentType.PROPERTY_ID, false);
    List<DocumentType> lstDocTypes = critDoc.list();

    if (lstDocTypes != null && lstDocTypes.size() == 1) {
      return (DocumentType) lstDocTypes.get(0);
    } else if (lstDocTypes == null || lstDocTypes.size() == 0) {
      throw new OBException(
          "A document type for picking list is needed to generate the picking list. Picking list cannot be created");
    } else {
      throw new OBException(
          "Several picking list documents types have been found. Picking list cannot be created");
    }
  }

  public static DocumentType getDocumentType(Organization org, String docCategory,
      boolean useOutbound, boolean isReturn) {
    Client client = null;

    if ("0".equals(org.getId())) {
      client = OBContext.getOBContext().getCurrentClient();
      if ("0".equals(client.getId())) {
        return null;
      }
    } else {
      client = org.getClient();
    }
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider(
        client.getId());

    OBCriteria<DocumentType> critDoc = OBDal.getInstance().createCriteria(DocumentType.class);
    critDoc.setFilterOnReadableClients(false);
    critDoc.setFilterOnReadableOrganization(false);
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_CLIENT, client));
    critDoc.add(Restrictions.in("organization.id", osp.getParentTree(org.getId(), true)));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, docCategory));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_OBWPLUSEOUTBOUND, useOutbound));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_RETURN, isReturn));
    critDoc.addOrderBy(DocumentType.PROPERTY_DEFAULT, false);
    critDoc.addOrderBy(DocumentType.PROPERTY_ID, false);
    critDoc.setMaxResults(1);
    return (DocumentType) critDoc.uniqueResult();
  }

  public static DocumentType getGroupPLDocumentType(Organization org) {
    Client client = null;

    if ("0".equals(org.getId())) {
      client = OBContext.getOBContext().getCurrentClient();
      if ("0".equals(client.getId())) {
        return null;
      }
    } else {
      client = org.getClient();
    }
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider(
        client.getId());

    OBCriteria<DocumentType> critDoc = OBDal.getInstance().createCriteria(DocumentType.class);
    critDoc.setFilterOnReadableClients(false);
    critDoc.setFilterOnReadableOrganization(false);
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_CLIENT, client));
    critDoc.add(Restrictions.in("organization.id", osp.getParentTree(org.getId(), true)));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, "OBWPL_doctype"));
    critDoc.add(Restrictions.eq(DocumentType.PROPERTY_OBWPLISGROUP, true));
    critDoc.addOrderBy(DocumentType.PROPERTY_DEFAULT, false);
    critDoc.addOrderBy(DocumentType.PROPERTY_ID, false);
    critDoc.setMaxResults(1);
    return (DocumentType) critDoc.uniqueResult();
  }

  /**
   * Returns the next sequence number of the Document Type defined for the Organization and document
   * category. The current number of the sequence is also updated.
   * 
   * @param docType
   *          Document type of the document
   * @return the next sequence number of the Document Type defined for the Organization and document
   *         category. Empty String if no sequence is found.
   */
  public static String getDocumentNo(DocumentType docType, String strTableName) {
    return getDocumentNo(docType, strTableName, true);
  }

  public static String getDocumentNo(DocumentType docType, String strTableName, boolean updateNext) {
    if (docType == null) {
      return "";
    }
    DocumentType docTypeObj = OBDal.getInstance().get(DocumentType.class, docType.getId());
    Sequence seq = docTypeObj.getDocumentSequence();
    if (seq == null && strTableName != null) {
      OBCriteria<Sequence> obcSeq = OBDal.getInstance().createCriteria(Sequence.class);
      obcSeq.add(Restrictions.eq(Sequence.PROPERTY_NAME, "DocumentNo_" + strTableName));
      if (obcSeq != null && obcSeq.list().size() > 0) {
        seq = obcSeq.list().get(0);
      }
    }
    if (seq == null) {
      return "";
    }

    if(docType.getName().trim().equals("Picking List outbound")){
     	return generateFromNative(docType.getName(),seq.getPrefix(),seq.getSuffix());
    }

    String nextDocNumber = "";
    if (seq.getPrefix() != null) {
      nextDocNumber = seq.getPrefix();
    }
    nextDocNumber += seq.getNextAssignedNumber().toString();
    if (seq.getSuffix() != null) {
      nextDocNumber += seq.getSuffix();
    }
    if (updateNext) {
      seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
      try {
        OBContext.setAdminMode(false);
        OBDal.getInstance().save(seq);
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    return nextDocNumber;
  }


private static String generateFromNative(String name, String prefix,
		String suffix) {
	  PreparedStatement stmt = null;
	  	ResultSet rs = null;
	  	String plnum = "";
	  	String sequence = "OBWPL_Picklist_sequence";
	  	
	  	String query = "select nextval(?)";
	  	
	  	try {
	  		stmt = OBDal.getInstance().getConnection().prepareStatement(query);
	  		stmt.setString(1, sequence);
	  		rs = stmt.executeQuery();
	  		
	  		if (rs.next()) { 
	  			plnum = (prefix!=null?prefix:"")+rs.getString(1)+(suffix!=null?suffix:"");
	  		}
	  		
	  		return plnum;
	  		
	  	} catch (SQLException e) {
	  		throw new OBException(" Cannot get sequence", e);
	  	} finally {
	  		try {
	  			rs.close();
	  			stmt.close();
	  		} catch (SQLException e) {
	  			// no operation
	  		}
	  	}
	  }

  public static void createGoodMovement(ReservationStock resStock, PickingList picking,
      PickingList groupPicking, BigDecimal quantity, OrderLine ordLine) {
    InternalMovement move = OBProvider.getInstance().get(InternalMovement.class);
    Map<String, String> map = new HashMap<String, String>();
    map.put("picking", "");

    move.setOrganization(picking.getOrganization());
    move.setMovementDate(new Date());
    move.setName(OBMessageUtils.parseTranslation(MOVEMENT_NAME, map));
    OBDal.getInstance().save(move);

    InternalMovementLine mvLine = OBProvider.getInstance().get(InternalMovementLine.class);
    mvLine.setOrganization(picking.getOrganization());
    mvLine.setMovement(move);
   // mvLine.setSalesOrderLine(ordLine);
    mvLine.setLineNo(10L);
    mvLine.setStockReservation(resStock.getReservation());
    mvLine.setOBWPLWarehousePickingList(picking);
    mvLine.setOBWPLGroupPickinglist(groupPicking);
    mvLine.setOBWPLAllowDelete(false);

    mvLine.setProduct(resStock.getReservation().getProduct());
    mvLine.setUOM(resStock.getReservation().getUOM());
    mvLine.setAttributeSetValue(resStock.getAttributeSetValue());
    mvLine.setStorageBin(resStock.getStorageBin());
    mvLine.setNewStorageBin(picking.getOutboundStorageBin());
    BigDecimal resStockReleasedQty = resStock.getReleased() == null ? BigDecimal.ZERO : resStock
        .getReleased();
    BigDecimal qty = quantity;
    if (qty == null) {
      qty = resStock.getQuantity().subtract(resStockReleasedQty);
    }
    mvLine.setMovementQuantity(qty);
    OBDal.getInstance().save(mvLine);
  }

  public static BigDecimal getNumberOfItemsInPickingList(PickingList pl) {
    StringBuffer countLinesStrQuery = new StringBuffer();
    countLinesStrQuery.append("select sum(" + InternalMovementLine.PROPERTY_MOVEMENTQUANTITY
        + ") FROM " + InternalMovementLine.ENTITY_NAME + " WHERE ");
    if (pl.getDocumentType().isOBWPLIsGroup()) {
      countLinesStrQuery.append(InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST + ".id = '"
          + pl.getId() + "'");
    } else {
      countLinesStrQuery.append(InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST + ".id = '"
          + pl.getId() + "'");
    }
    Query countLinesQuery = OBDal.getInstance().getSession()
        .createQuery(countLinesStrQuery.toString());
    return (BigDecimal) countLinesQuery.uniqueResult();
  }

  public static BigDecimal getNumberOfItemsInPickingListRelatedToASalesOrder(PickingList pl,
      Order ord) {
    StringBuffer countLinesStrQuery = new StringBuffer();
    countLinesStrQuery.append("select sum(" + InternalMovementLine.PROPERTY_MOVEMENTQUANTITY
        + ") FROM " + InternalMovementLine.ENTITY_NAME + "ml WHERE ");
    if (pl.getDocumentType().isOBWPLIsGroup()) {
      countLinesStrQuery.append("ml." + InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST
          + ".id = '" + pl.getId() + "'");
    } else {
      countLinesStrQuery.append("ml." + InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST
          + ".id = '" + pl.getId() + "'");
    }
    countLinesStrQuery.append("and ml.stockReservation.salesOrderLine.salesOrder.id = '"
        + ord.getId() + "'");

    Query countLinesQuery = OBDal.getInstance().getSession()
        .createQuery(countLinesStrQuery.toString());
    return (BigDecimal) countLinesQuery.uniqueResult();
  }

  public static BigDecimal getNumberOfMovLinesInPickingList(PickingList pl) {
    StringBuffer countLinesStrQuery = new StringBuffer();
    countLinesStrQuery.append("select count(" + InternalMovementLine.PROPERTY_ID + ") FROM "
        + InternalMovementLine.ENTITY_NAME + " WHERE ");
    if (pl.getDocumentType().isOBWPLIsGroup()) {
      countLinesStrQuery.append(InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST + ".id = '"
          + pl.getId() + "'");
    } else {
      countLinesStrQuery.append(InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST + ".id = '"
          + pl.getId() + "'");
    }

    Query countLinesQuery = OBDal.getInstance().getSession()
        .createQuery(countLinesStrQuery.toString());
    return (BigDecimal) countLinesQuery.uniqueResult();
  }

  public static BigDecimal getNumberOfSOlinesRelatedToIncidence(PickingListProblem incidence) {
    Long result = null;
    try {
      StringBuffer countLinesStrQuery = new StringBuffer();
      countLinesStrQuery.append("select count(" + PickingListProblemOrderLines.PROPERTY_ID
          + ") FROM " + PickingListProblemOrderLines.ENTITY_NAME + " WHERE ");
      countLinesStrQuery.append(PickingListProblemOrderLines.PROPERTY_PICKINGLISTPROBLEM
          + ".id = '" + incidence.getId() + "'");
      Query countLinesQuery = OBDal.getInstance().getSession()
          .createQuery(countLinesStrQuery.toString());
      result = (Long) countLinesQuery.uniqueResult();
      return new BigDecimal(result);
    } catch (Exception e) {
      throw new OBException(
          "A problem happened getting sales order lines related to pickinglistproblem_id ("
              + incidence.getId() + "). :" + e.getMessage());
    }

  }

  public static ScrollableResults getSOlinesRelatedToIncidence(PickingListProblem incidence) {
    try {
      StringBuffer getLinesStrQuery = new StringBuffer();
      getLinesStrQuery.append("FROM " + PickingListProblemOrderLines.ENTITY_NAME + " WHERE ");
      getLinesStrQuery.append(PickingListProblemOrderLines.PROPERTY_PICKINGLISTPROBLEM + ".id = '"
          + incidence.getId() + "'");
      Query getLinesQuery = OBDal.getInstance().getSession()
          .createQuery(getLinesStrQuery.toString());
      return getLinesQuery.scroll(ScrollMode.FORWARD_ONLY);
    } catch (Exception e) {
      throw new OBException(
          "A problem happened getting a scroll with sales order lines related to pickinglistproblem_id ("
              + incidence.getId() + "). :" + e.getMessage());
    }
  }

  public static void deletePickingListIncidence(PickingListProblem incidence) {
    StringBuffer removeIncidenceStrQuery = new StringBuffer();
    removeIncidenceStrQuery.append("delete FROM " + PickingListProblem.ENTITY_NAME + " WHERE ");
    removeIncidenceStrQuery.append(PickingListProblem.PROPERTY_ID + " = '" + incidence.getId()
        + "'");
    Query removeIncidenceQuery = OBDal.getInstance().getSession()
        .createQuery(removeIncidenceStrQuery.toString());
    removeIncidenceQuery.executeUpdate();
    OBDal.getInstance().flush();
  }

  public static void deleteSolFromPickingListIncidence(PickingListProblem incidence, OrderLine sol) {
    OBContext.setAdminMode(true);
    try {
      StringBuffer removeIncidenceStrQuery = new StringBuffer();
      removeIncidenceStrQuery.append("delete FROM " + PickingListProblemOrderLines.ENTITY_NAME
          + " WHERE ");
      removeIncidenceStrQuery.append(PickingListProblemOrderLines.PROPERTY_PICKINGLISTPROBLEM
          + " = '" + incidence.getId() + "'");
      removeIncidenceStrQuery.append(" and " + PickingListProblemOrderLines.PROPERTY_ORDERLINE
          + " = '" + sol.getId() + "'");
      Query removeIncidenceQuery = OBDal.getInstance().getSession()
          .createQuery(removeIncidenceStrQuery.toString());
      removeIncidenceQuery.executeUpdate();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static List<InternalMovementLine> getGeneratedMovemtLinesByIncidence(
      PickingListProblem incidence) {
    StringBuffer where = new StringBuffer();
    Map<String, Object> namedParameters = new HashMap<String, Object>();
    where.append(" as movline");
    where.append(" where movline." + InternalMovementLine.PROPERTY_OBWPLPLPROBLEMGENERATOR
        + ".id = :incidence");

    namedParameters.put("incidence", incidence.getId());

    OBQuery<InternalMovementLine> qryMovLines = OBDal.getInstance().createQuery(
        InternalMovementLine.class, where.toString());

    qryMovLines.setFilterOnReadableOrganization(false);
    qryMovLines.setNamedParameters(namedParameters);

    return qryMovLines.list();
  }

  public static void deleteGeneratedMovements(PickingListProblem incidence) {
    // get movements lines generated by a particular incidence
    StringBuffer where = new StringBuffer();
    Map<String, Object> namedParameters = new HashMap<String, Object>();
    where.append(" as movline");
    where.append(" where movline." + InternalMovementLine.PROPERTY_OBWPLPLPROBLEMGENERATOR
        + ".id = :incidence");
    where.append(" and movline." + InternalMovementLine.PROPERTY_OBWPLITEMSTATUS + " = 'PE'");

    namedParameters.put("incidence", incidence.getId());

    OBQuery<InternalMovementLine> qryMovLines = OBDal.getInstance().createQuery(
        InternalMovementLine.class, where.toString());

    qryMovLines.setFilterOnReadableOrganization(false);
    qryMovLines.setNamedParameters(namedParameters);

    ScrollableResults movlines = qryMovLines.scroll(ScrollMode.FORWARD_ONLY);

    while (movlines.next()) {
      InternalMovementLine movLine = (InternalMovementLine) movlines.get(0);

      StorageDetail sd = getStorageDetailFromMovLine(movLine);

      // delete reserved qty: reservedQty = reserved qty - mvmntQty
      ReservationUtils.reserveStockManual(movLine.getStockReservation(), sd, movLine
          .getMovementQuantity().negate(), "Y");

      // remove movement
      InternalMovement movHeader = movLine.getMovement();
      movLine.setOBWPLAllowDelete(true);
      OBDal.getInstance().save(movLine);
      OBDal.getInstance().flush();

      OBDal.getInstance().remove(movLine);
      OBDal.getInstance().remove(movHeader);
      OBDal.getInstance().flush();
    }

  }

  public static StorageDetail getStorageDetailFromMovLine(InternalMovementLine movLine) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<StorageDetail> sdCrit = OBDal.getInstance().createCriteria(StorageDetail.class);
      sdCrit.add(Restrictions.eq(StorageDetail.PROPERTY_PRODUCT, movLine.getProduct()));
      sdCrit.add(Restrictions.eq(StorageDetail.PROPERTY_ATTRIBUTESETVALUE,
          movLine.getAttributeSetValue()));
      sdCrit.add(Restrictions.eq(StorageDetail.PROPERTY_UOM, movLine.getUOM()));
      sdCrit.add(Restrictions.eq(StorageDetail.PROPERTY_STORAGEBIN, movLine.getStorageBin()));
      sdCrit.add(Restrictions.isNull(StorageDetail.PROPERTY_ORDERUOM));

      sdCrit.setFilterOnReadableClients(false);
      sdCrit.setFilterOnReadableOrganization(false);
      sdCrit.setMaxResults(1);

      List<StorageDetail> sdList = sdCrit.list();

      if (sdList.size() > 0 && sdList.size() <= 1) {
        return sdList.get(0);
      } else {
        throw new OBException("Something strange happened while getting storage detail: "
            + sdList.size() + "storage detail lines found for prod: "
            + movLine.getProduct().getId() + ", attSet: " + movLine.getAttributeSetValue().getId()
            + ", uom: " + movLine.getUOM().getId() + ", storageBin: "
            + movLine.getStorageBin().getId());
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static long getNumberOfRequiredBins(PickingList pl) {
    StringBuffer countLinesStrQuery = new StringBuffer();
    countLinesStrQuery.append("select count(distinct " + InternalMovementLine.PROPERTY_STORAGEBIN
        + ".id) FROM " + InternalMovementLine.ENTITY_NAME + " WHERE ");
    countLinesStrQuery.append(InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST + ".id = '"
        + pl.getId() + "'");
    Query countLinesQuery = OBDal.getInstance().getSession()
        .createQuery(countLinesStrQuery.toString());
    return (Long) countLinesQuery.uniqueResult();
  }

  public static long getNumberOfGeneratedMovementsNonDeleteables(PickingListProblem plProblem) {
    StringBuffer countLinesStrQuery = new StringBuffer();
    countLinesStrQuery.append("select count(id) FROM " + InternalMovementLine.ENTITY_NAME
        + " WHERE ");
    countLinesStrQuery
        .append(InternalMovementLine.PROPERTY_OBWPLPLPROBLEMGENERATOR + ".id = '"
            + plProblem.getId() + "' and " + InternalMovementLine.PROPERTY_OBWPLITEMSTATUS
            + " <> 'PE'");
    Query countLinesQuery = OBDal.getInstance().getSession()
        .createQuery(countLinesStrQuery.toString());
    return (Long) countLinesQuery.uniqueResult();
  }

  public static List<Map<String, Object>> getStockFromNearestBin(InternalMovementLine movLine,
      BigDecimal qtyToPick) throws OBException {
    List<Map<String, Object>> finalResult = new ArrayList<Map<String, Object>>();
    StringBuilder logBuilder = new StringBuilder();
    BigDecimal pendingQty = qtyToPick;
    String includeMySelfOperator = ">=";
    Boolean successLookingAhead = false;
    // if we are working with att we want to search in curren bin looking for differents atts
    if (movLine.getAttributeSetValue().getId().equals("0")) {
      // if not, we want to skip current bin
      includeMySelfOperator = ">";
    }

    OBContext.setAdminMode(true);
    try {
      logBuilder.append(System.getProperty("line.separator"));
      logBuilder.append("************* Alternate location call started ("
          + OBDateUtils.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss") + ")*****************");
      logBuilder.append(System.getProperty("line.separator"));
      logBuilder.append("Mov id: " + movLine.getId());
      logBuilder.append(System.getProperty("line.separator"));
      logBuilder.append("QtyToPick: " + qtyToPick);
      logBuilder.append(System.getProperty("line.separator"));
      logBuilder.append("Product: " + movLine.getProduct().getIdentifier() + " ("
          + movLine.getProduct().getId() + ")");
      logBuilder.append(System.getProperty("line.separator"));
      if (!movLine.getAttributeSetValue().getId().equals("0")) {
        logBuilder.append("Att:" + movLine.getAttributeSetValue().getIdentifier() + " ("
            + movLine.getAttributeSetValue().getId() + ")");
      } else {
        logBuilder.append("Att: N/A");
      }
      logBuilder.append(System.getProperty("line.separator"));
      logBuilder.append("Bin: " + movLine.getStorageBin().getIdentifier() + " ("
          + movLine.getStorageBin().getId() + ")");
      logBuilder.append(System.getProperty("line.separator"));

      // Call to m_get_stock process
      Process process = (Process) OBDal.getInstance().get(Process.class,
          "FF80818132C964E30132C9747257002E");
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("AD_Client_ID", movLine.getClient().getId());
      parameters.put("AD_Org_ID", movLine.getOrganization().getId());
      parameters.put("M_Product_ID", movLine.getProduct().getId());
      parameters.put("C_Uom_ID", movLine.getUOM().getId());
      parameters.put("M_Product_Uom_ID", null);
      parameters.put("M_Warehouse_ID", movLine.getStorageBin().getWarehouse().getId());

      ProcessInstance pInstance = CallProcess.getInstance().callProcess(process, null, parameters);

      // Get results sorting it by X,Y,Z and attsetInstance creation date excluding outbound bins

      StringBuffer where = new StringBuffer();
      Map<String, Object> namedParameters = new HashMap<String, Object>();
      where.append(" as sp");
      where.append("    join sp." + StockProposed.PROPERTY_STORAGEDETAIL + " as sd");
      where.append("    join sd." + StorageDetail.PROPERTY_STORAGEBIN + " as sb");
      where.append("    join sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE + " as asi");
      where.append(" where sp." + StockProposed.PROPERTY_PROCESSINSTANCE + " = :process");
      where.append(" and (");
      where.append("(sb.rowX = '" + movLine.getStorageBin().getRowX() + "' and sb.stackY = '"
          + movLine.getStorageBin().getStackY() + "' and sb.levelZ " + includeMySelfOperator + "'"
          + movLine.getStorageBin().getLevelZ() + "') or (sb.rowX = '"
          + movLine.getStorageBin().getRowX() + "' and sb.stackY > '"
          + movLine.getStorageBin().getStackY() + "') or (sb.rowX > '"
          + movLine.getStorageBin().getRowX() + "')");
      where.append(")");
      if (!movLine.getAttributeSetValue().getId().equals("0")) {
        where.append(" and sd.attributeSetValue.id <> '" + movLine.getAttributeSetValue().getId()
            + "'");
      }
      where.append(" and sb.oBWHSType <> 'OUT'");
      where.append("   order by sb." + Locator.PROPERTY_ROWX + ", sb." + Locator.PROPERTY_STACKY
          + ", sb." + Locator.PROPERTY_LEVELZ + ", asi."
          + AttributeSetInstance.PROPERTY_CREATIONDATE + " asc");

      namedParameters.put("process", pInstance.getId());

      OBQuery<StockProposed> qrySP = OBDal.getInstance().createQuery(StockProposed.class,
          where.toString());

      qrySP.setFilterOnReadableOrganization(false);
      qrySP.setNamedParameters(namedParameters);

      ScrollableResults bins = qrySP.scroll(ScrollMode.FORWARD_ONLY);

      logBuilder.append("Alternate locator results going ahead (until reach needed qty)");
      logBuilder.append(System.getProperty("line.separator"));
      logBuilder.append("-------");
      logBuilder.append(System.getProperty("line.separator"));
      while (bins.next() && pendingQty.compareTo(BigDecimal.ZERO) > 0) {
        StockProposed stock = (StockProposed) bins.get(0);
        Map<String, Object> finalResultItem = new HashMap<String, Object>();
        BigDecimal availableQty = stock.getQuantity();
        finalResultItem.put("availableQty", availableQty);
        finalResultItem.put("stockProposed", stock);
        if (availableQty.compareTo(pendingQty) >= 1) {
          finalResultItem.put("pickedQty", pendingQty);
          pendingQty = pendingQty.subtract(pendingQty);
        } else {
          finalResultItem.put("pickedQty", availableQty);
          pendingQty = pendingQty.subtract(availableQty);
        }
        finalResult.add(finalResultItem);
        logBuilder.append("Stock found going ahead! bin: "
            + stock.getStorageDetail().getStorageBin().getIdentifier() + " Available qty: "
            + availableQty.toString() + " Picked: " + finalResultItem.get("pickedQty")
            + " Pending to pick: " + pendingQty.toString());
        logBuilder.append(System.getProperty("line.separator"));
        successLookingAhead = true;
      }
      if (successLookingAhead == false) {
        logBuilder.append("No stock found going ahead");
        logBuilder.append(System.getProperty("line.separator"));
      }
      // going back just if I haven't been able to find the whole stock that I need
      if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
        logBuilder
            .append("Not enough alternate stock found going ahead. Trying backwards to find needed stock: "
                + pendingQty);
        logBuilder.append(System.getProperty("line.separator"));
        StringBuffer whereDown = new StringBuffer();
        Map<String, Object> namedParametersDown = new HashMap<String, Object>();
        whereDown.append(" as sp");
        whereDown.append("    join sp." + StockProposed.PROPERTY_STORAGEDETAIL + " as sd");
        whereDown.append("    join sd." + StorageDetail.PROPERTY_STORAGEBIN + " as sb");
        whereDown.append("    join sd." + StorageDetail.PROPERTY_ATTRIBUTESETVALUE + " as asi");
        whereDown.append(" where sp." + StockProposed.PROPERTY_PROCESSINSTANCE + " = :process");
        whereDown.append(" and (");
        whereDown.append("(sb.rowX = '" + movLine.getStorageBin().getRowX() + "' and sb.stackY = '"
            + movLine.getStorageBin().getStackY() + "' and sb.levelZ < '"
            + movLine.getStorageBin().getLevelZ() + "') or (sb.rowX = '"
            + movLine.getStorageBin().getRowX() + "' and sb.stackY < '"
            + movLine.getStorageBin().getStackY() + "') or (sb.rowX < '"
            + movLine.getStorageBin().getRowX() + "')");
        whereDown.append(")");
        if (!movLine.getAttributeSetValue().getId().equals("0")) {
          whereDown.append(" and sd.attributeSetValue.id <> '"
              + movLine.getAttributeSetValue().getId() + "'");
        }
        whereDown.append(" and sb.oBWHSType <> 'OUT'");
        whereDown.append("   order by sb." + Locator.PROPERTY_ROWX + ", sb."
            + Locator.PROPERTY_STACKY + ", sb." + Locator.PROPERTY_LEVELZ + " desc, asi."
            + AttributeSetInstance.PROPERTY_CREATIONDATE + " asc");

        namedParametersDown.put("process", pInstance.getId());

        OBQuery<StockProposed> qrySPDown = OBDal.getInstance().createQuery(StockProposed.class,
            whereDown.toString());

        qrySPDown.setFilterOnReadableOrganization(false);
        qrySPDown.setNamedParameters(namedParameters);

        ScrollableResults binsDown = qrySPDown.scroll(ScrollMode.FORWARD_ONLY);
        logBuilder.append("Alternate locator results going backwards (until reach needed qty)");
        logBuilder.append(System.getProperty("line.separator"));
        logBuilder.append("-------");
        logBuilder.append(System.getProperty("line.separator"));
        while (binsDown.next() && pendingQty.compareTo(BigDecimal.ZERO) > 0) {
          StockProposed stock = (StockProposed) binsDown.get(0);
          Map<String, Object> finalResultItem = new HashMap<String, Object>();
          BigDecimal availableQty = stock.getQuantity();
          finalResultItem.put("availableQty", availableQty);
          finalResultItem.put("stockProposed", stock);
          if (availableQty.compareTo(pendingQty) >= 1) {
            finalResultItem.put("pickedQty", pendingQty);
            pendingQty = pendingQty.subtract(pendingQty);
          } else {
            finalResultItem.put("pickedQty", availableQty);
            pendingQty = pendingQty.subtract(availableQty);
          }
          finalResult.add(finalResultItem);
          logBuilder.append("Stock found going backwards! bin: "
              + stock.getStorageDetail().getStorageBin().getIdentifier() + " Available qty: "
              + availableQty.toString() + " Picked: " + finalResultItem.get("pickedQty")
              + " Pending to pick: " + pendingQty.toString());
          logBuilder.append(System.getProperty("line.separator"));
        }
      } else {
        logBuilder.append(System.getProperty("line.separator"));
        logBuilder.append("Enough stock have been found going ahead, no backwards search required");
        logBuilder.append(System.getProperty("line.separator"));
      }
    } catch (Exception e) {
      throw new OBException("The following error happened while getting stock from nearest bin: "
          + e.getMessage());
    } finally {
      logBuilder.append("***********end of alternate location call**************");
      OBContext.restorePreviousMode();
      log.info(logBuilder.toString());
    }
    return finalResult;
  }

  public static void removeGeneratedMovementDueToEmpyBox(PickingListProblem incidence,
      InternalMovementLine mvmtLine) {

    if (OBWPL_Utils.getNumberOfGeneratedMovementsNonDeleteables(incidence) > 0) {
      throw new OBException(
          "This incidence cannot be undo because generated movements are not in pending status");
    }

    // get movements lines generated by a particular incidence
    StringBuffer where = new StringBuffer();
    BigDecimal origQty = BigDecimal.ZERO;
    Map<String, Object> namedParameters = new HashMap<String, Object>();
    where.append(" as movline");
    where.append(" where movline." + InternalMovementLine.PROPERTY_OBWPLPLPROBLEMGENERATOR
        + ".id = :incidence");
    where.append(" and movline." + InternalMovementLine.PROPERTY_OBWPLITEMSTATUS + " = 'PE'");

    namedParameters.put("incidence", incidence.getId());

    OBQuery<InternalMovementLine> qryMovLines = OBDal.getInstance().createQuery(
        InternalMovementLine.class, where.toString());

    qryMovLines.setFilterOnReadableOrganization(false);
    qryMovLines.setNamedParameters(namedParameters);

    ScrollableResults movlines = qryMovLines.scroll(ScrollMode.FORWARD_ONLY);

    while (movlines.next()) {
      InternalMovementLine movLine = (InternalMovementLine) movlines.get(0);

      // remove movement
      InternalMovement movHeader = movLine.getMovement();
      movLine.setOBWPLAllowDelete(true);
      OBDal.getInstance().save(movLine);
      OBDal.getInstance().flush();

      origQty = origQty.add(movLine.getMovementQuantity());

      OBDal.getInstance().remove(movLine);
      OBDal.getInstance().remove(movHeader);
      OBDal.getInstance().flush();
    }

    mvmtLine.setMovementQuantity(origQty);
    OBDal.getInstance().save(mvmtLine);

  }

}
