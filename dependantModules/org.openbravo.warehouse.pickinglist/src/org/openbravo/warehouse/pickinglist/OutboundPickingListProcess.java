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
 * All portions are Copyright (C) 2012-2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.warehouse.pickinglist;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallProcess;
import org.openbravo.warehouse.pickinglist.hooks.ClosePLOutbound_CreateShipmentsHook;
import org.openbravo.warehouse.pickinglist.hooks.ClosePLOutbound_ProcessOrdersHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboundPickingListProcess {
  final private static Logger log = LoggerFactory.getLogger(OutboundPickingListProcess.class);

  public static OBError close(PickingList picking) {
    OBError msg = new OBError();
    msg.setType("success");
    OBContext.setAdminMode(true);
    try {
      if ("CO".equals(picking.getPickliststatus())) {
    	  
    	try {
			addcontentinBox(picking);
		} catch (Exception e) {
		    msg = OBMessageUtils.translateError(e.getMessage());
	    }
    	
    	
        String strMsg = OBMessageUtils.messageBD("Success", false);
        int resultBoxesComparedToQtyInMovements = qtyInBoxesComparedToQtyInMovements(picking, true);
        if (resultBoxesComparedToQtyInMovements == -1) {
          throw new OBException(OBMessageUtils.parseTranslation("@OBWPL_LessQtyInBoxes@"));
        }
        if (resultBoxesComparedToQtyInMovements == 1) {
          throw new OBException(OBMessageUtils.parseTranslation("@OBWPL_MoreQtyInBoxes@"));
        }

        completeMovements(picking);

        if (picking.getDocumentType().isOBWPLGenerateShipment()) {
          strMsg = createShipments(picking);
        }
        releaseSalesOrders(picking);

        picking.setPickliststatus("CL");
        OBDal.getInstance().save(picking);
        msg.setMessage(strMsg);
      } else {
        msg.setType("error");
        String strPLStatus = getTranslatedListValueName(getPlstatusRef(),
            picking.getPickliststatus());
        String[] map = { strPLStatus };
        String message = OBMessageUtils.getI18NMessage("OBWPL_Close_StatusError", map);
        msg.setMessage(message);
      }
      // force errors in try catch
      OBDal.getInstance().flush();
    } catch (OBException e) {
      msg = OBMessageUtils.translateError(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return msg;
  }

private static void addcontentinBox(PickingList picking) throws SQLException {
	
	String boxQuery = "select box.id from OBWPL_plbox box where obwplPickinglist.id =:pickid)";
	  Query query = OBDal.getInstance().getSession().createQuery(boxQuery);
	  query.setParameter("pickid", picking.getId());
	  query.setMaxResults(1);
	  List<String> boxQueryList = query.list();
	  
	  if(boxQueryList.size() > 0){
		  
		  
		  String insertQuery = "insert into obwpl_plboxcontent (obwpl_plboxcontent_id, ad_client_id, ad_org_id, isactive, created, createdby, " +
		  					   "updated, updatedby, m_movementline_id, obwpl_plbox_id, quantity, c_orderline_id, m_product_id, " +
		  					   "m_attributesetinstance_id) select get_uuid(), '187D8FC945A5481CB41B3EE767F80DBB','603C6A266B4C40BCAD87C5C43DDF53EE'," +
		  					   "'Y', now(), '100', now(), '100',mil.m_movementline_id, ? , mil.movementqty, " +
		  					   "mil.c_orderline_id, mil.m_product_id, mil.m_attributesetinstance_id from obwpl_pickinglist pl " +
		  					   "left join m_movementline mil on pl.obwpl_pickinglist_id = mil.em_obwpl_pickinglist_id left join obwpl_plboxcontent plbc " +
		  					   "on plbc.m_movementline_id = mil.m_movementline_id where pl.obwpl_pickinglist_id = ? and plbc.quantity is null and mil.movementqty > 0";
		  
		  
		  PreparedStatement stmt = null;
		      			
			try {
				stmt = OBDal.getInstance().getConnection().prepareStatement(insertQuery);
				stmt.setString(1, boxQueryList.get(0));
				stmt.setString(2, picking.getId());
				stmt.executeUpdate();
				SessionHandler.getInstance().commitAndStart();
			} catch (SQLException e) {
				throw new OBException("Cannot insert box lines....", e);
			} finally {
				stmt.close();
			}
			
	  } else throw new OBException("No Box created.");
}

  private static void completeMovements(PickingList picking) {
    List<InternalMovementLine> mvmtLines = null;
    Process movementPost = null;
    movementPost = OBDal.getInstance().get(Process.class, "122");
    if (picking.getDocumentType().isOBWPLIsGroup()) {
      mvmtLines = picking.getMaterialMgmtInternalMovementLineEMOBWPLGroupPickinglistList();
    } else {
      mvmtLines = picking.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList();
    }
    for (InternalMovementLine mvmtLine : mvmtLines) {
      if ("CO".equals(mvmtLine.getOBWPLItemStatus()) || "CWI".equals(mvmtLine.getOBWPLItemStatus())) {
        continue;
      }
      if ("IC".equals(mvmtLine.getOBWPLItemStatus())) {
        BigDecimal originalMovQty = mvmtLine.getMovementQuantity();
        BigDecimal difference = originalMovQty.subtract(mvmtLine.getOBWPLPickedqty());
        if (mvmtLine.getStockReservation().isOBWPLGeneratedByPickingList()) {
          // TODO: what I should do here?
        } else {
          mvmtLine.setMovementQuantity(mvmtLine.getOBWPLPickedqty());
          if (mvmtLine.getMovementQuantity().equals(BigDecimal.ZERO)) {
            // delete mov
            mvmtLine.setOBWPLAllowDelete(true);
            OBDal.getInstance().save(mvmtLine);
            OBDal.getInstance().flush();
            OBDal.getInstance().remove(mvmtLine);
            OBDal.getInstance().flush();
          } else {
            OBDal.getInstance().save(mvmtLine);
          }
          StorageDetail sd = OBWPL_Utils.getStorageDetailFromMovLine(mvmtLine);

          ReservationUtils.reserveStockManual(mvmtLine.getStockReservation(), sd,
              difference.negate(), "Y");
        }
      }

      final ProcessInstance pinstance = CallProcess.getInstance().call(movementPost,
          (String) DalUtil.getId(mvmtLine.getMovement()), null);
      if (pinstance.getResult() != 1) {
        throw new OBException(OBMessageUtils.parseTranslation(pinstance.getErrorMsg()));
      }
    }
  }

  private static void releaseSalesOrders(PickingList picking) {
    HashSet<Order> orders = new HashSet<Order>();
    List<InternalMovementLine> mvmtLines = null;
    Set<Bean<?>> beansSet = WeldUtils.getStaticInstanceBeanManager().getBeans(
        ClosePLOutbound_ProcessOrdersHook.class);
    List<Bean<?>> beansList = new ArrayList<Bean<?>>();
    beansList.addAll(beansSet);
    if (picking.getDocumentType().isOBWPLIsGroup()) {
      mvmtLines = picking.getMaterialMgmtInternalMovementLineEMOBWPLGroupPickinglistList();
    } else {
      mvmtLines = picking.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList();
    }
    for (InternalMovementLine mvmtLine : mvmtLines) {
      orders.add(mvmtLine.getStockReservation().getSalesOrderLine().getSalesOrder());
    }
    for (Order order : orders) {
      // commented for DSI
      // order.setObwplIsinpickinglist(false);
      for (Bean<?> abstractBean : beansList) {
        ClosePLOutbound_ProcessOrdersHook hook = (ClosePLOutbound_ProcessOrdersHook) WeldUtils
            .getStaticInstanceBeanManager().getReference(abstractBean,
                ClosePLOutbound_ProcessOrdersHook.class,
                WeldUtils.getStaticInstanceBeanManager().createCreationalContext(abstractBean));
        try {
          hook.exec(order, picking);
        } catch (Exception e) {
          log.error("An error happened when ClosePLOutbound_ProcessOrdersHook was executed.",
              e.getMessage(), e.getStackTrace());
        }
      }

      OBDal.getInstance().save(order);
    }
  }

  private static String createShipments(PickingList picking) throws OBException {
    String strShipDocNos = "";
    // Map with all shipments created by Sales Order
    Map<String, ShipmentInOut> shipments = new HashMap<String, ShipmentInOut>();
    DocumentType shipDocType = picking.getDocumentType().getOBWPLShipmentDocumentType();
    Boolean groupShipments = picking.getDocumentType().isOBWPLGroupshipments();
    // Map with all shipment lines created by the concatenation of orderline, attribute set
    // instance and bin
    Map<String, ShipmentInOutLine> shipmentLines = new HashMap<String, ShipmentInOutLine>();
    List<InternalMovementLine> mvmtLines = null;
    if (picking.getDocumentType().isOBWPLIsGroup()) {
      mvmtLines = picking.getMaterialMgmtInternalMovementLineEMOBWPLGroupPickinglistList();
    } else {
      mvmtLines = picking.getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList();
    }
    if (picking.isUsePickingBoxes()) {
      int resultBoxesComparedToQtyInMovements = qtyInBoxesComparedToQtyInMovements(picking, false);
      if (resultBoxesComparedToQtyInMovements == -1) {
        throw new OBException(OBMessageUtils.parseTranslation("@OBWPL_LessQtyInBoxes@"));
      }
      if (resultBoxesComparedToQtyInMovements == 1) {
        throw new OBException(OBMessageUtils.parseTranslation("@OBWPL_MoreQtyInBoxes@"));
      }

      for (String boxID : getBoxIDsList(picking)) {
        PickingListBox box = OBDal.getInstance().get(PickingListBox.class, boxID);

        List<String> zeroQtyBoxContent = getZeroQtyBoxContent(box);
       //delete zero qty box contents
               if (zeroQtyBoxContent != null && zeroQtyBoxContent.size() > 0) {
                  for (String boxContentId : zeroQtyBoxContent) {
                    PickingListBoxContent boxContent = OBDal.getInstance().get(PickingListBoxContent.class,
                        boxContentId);
                    OBDal.getInstance().remove(boxContent);
                   OBDal.getInstance().flush();
                 }
               }
        
        // Create Shipment
        if (box.getOBWPLPlBoxContentList().size() == 0) {
          picking.getOBWPLPlboxList().remove(box);
          OBDal.getInstance().remove(box);
          OBDal.getInstance().save(picking);
          continue;	
        }

        PickingListBoxContent firstContent = box.getOBWPLPlBoxContentList().get(0);
        OrderLine orderLine = firstContent.getMovementLine().getStockReservation()
            .getSalesOrderLine();
        ShipmentInOut shipment = createShipment(orderLine.getSalesOrder(), shipDocType, picking,
            box);
        box.setGoodsShipment(shipment);
        OBDal.getInstance().save(box);
        if (StringUtils.isNotEmpty(strShipDocNos)) {
          strShipDocNos += ", ";
        }
        strShipDocNos += shipment.getDocumentNo();
        for (PickingListBoxContent content : box.getOBWPLPlBoxContentList()) {
        	
        	if((content.getQuantity().compareTo(BigDecimal.ZERO))==0){
        	log.info("box content is zeroso skipping the record " + content);
        	} else {       			
        		// Create Shipment Line with content qty
                ShipmentInOutLine shipmentLine = createShipmentLine(shipment, content.getMovementLine(),
                content.getSalesOrderLine(), content.getQuantity());
                // Add Shipment Line to Shipment
        	}
         
        }
      }
    } else {
      for (InternalMovementLine mvmtLine : mvmtLines) {
        String key = "";
        OrderLine orderLine = mvmtLine.getStockReservation().getSalesOrderLine();
        if (groupShipments) {
          key = orderLine.getSalesOrder().getClient().getId();
        } else {
          key = orderLine.getSalesOrder().getId();
        }
        // Possible Hook to allow custom group
        // this hook will allow to user to edit the key
        // key = callToHook
        ShipmentInOut shipment = shipments.get(key);

        // Just create shipments if the current sales order is present in lines
        boolean shipmentHasLines = false;
        if (OBWPL_Utils.getNumberOfItemsInPickingListRelatedToASalesOrder(picking,
            orderLine.getSalesOrder()).compareTo(BigDecimal.ZERO) > 0) {
          shipmentHasLines = true;
        }
        if (shipment == null && shipmentHasLines) {
          // order
          shipment = createShipment(orderLine.getSalesOrder(), shipDocType, picking, null);
          shipments.put(key, shipment);
          if (StringUtils.isNotEmpty(strShipDocNos)) {
            strShipDocNos += ", ";
          }
          strShipDocNos += shipment.getDocumentNo();
        }
        String strAsi = "0";
        if (mvmtLine.getAttributeSetValue() != null) {
          strAsi = (String) DalUtil.getId(mvmtLine.getAttributeSetValue());
        }
        ShipmentInOutLine shipmentLine = shipmentLines.get(orderLine.getId() + "-" + strAsi + "-"
            + mvmtLine.getNewStorageBin().getId());
        if (shipmentLine == null) {
          if (mvmtLine.getMovementQuantity().compareTo(BigDecimal.ZERO) > 0) {
            shipmentLine = createShipmentLine(shipment, mvmtLine, orderLine,
                mvmtLine.getMovementQuantity());
            shipmentLines.put(orderLine.getId() + "-" + strAsi + "-"
                + mvmtLine.getNewStorageBin().getId(), shipmentLine);
          }
        } else {
          shipmentLine.setMovementQuantity(shipmentLine.getMovementQuantity().add(
              mvmtLine.getMovementQuantity()));
        }
      }
    }
    // Process shipments
    // for (ShipmentInOut shipment : shipments.values()) {
    // processShipment(shipment.getId());
    // }
    Map<String, String> map = new HashMap<String, String>();
    map.put("shipments", strShipDocNos);
    return OBMessageUtils.parseTranslation(
        OBMessageUtils.messageBD("OBWPL_CreatedShipments", false), map);
  }

  private static List<String> getBoxIDsList(PickingList picking) {
    final StringBuilder hqlString = new StringBuilder();
    hqlString.append(" select e.id ");
    hqlString.append(" from OBWPL_plbox as e");
    hqlString.append(" where e.obwplPickinglist.id = :picking");
    Query query = OBDal.getInstance().getSession().createQuery(hqlString.toString());
    query.setParameter("picking", picking.getId());
    return query.list();
  }

  private static int qtyInBoxesComparedToQtyInMovements(PickingList picking,
      Boolean preProcessMovements) {
    int result = -1;
    OBContext.setAdminMode(true);
    try {
      BigDecimal totalQtyToPick = getTotalQtyToPick(picking, preProcessMovements);
      BigDecimal qtyInBoxes = getQtyInBoxes(picking);
      result = qtyInBoxes.compareTo(totalQtyToPick);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private static BigDecimal getTotalQtyToPick(PickingList picking, Boolean preProcessMovements) {
    BigDecimal result = BigDecimal.ZERO;
    BigDecimal result2 = BigDecimal.ZERO;
    OBContext.setAdminMode(true);
    try {
      StringBuffer qtyInBoxesWhere = new StringBuffer();
      qtyInBoxesWhere.append(" select sum(e." + InternalMovementLine.PROPERTY_MOVEMENTQUANTITY
          + ")");
      qtyInBoxesWhere.append(" from " + InternalMovementLine.ENTITY_NAME + " e");
      if (picking.getDocumentType().isOBWPLIsGroup()) {
        qtyInBoxesWhere.append(" where e." + InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST
            + " = :pickingList");
      } else {
        qtyInBoxesWhere.append(" where e."
            + InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST + " = :pickingList");
      }
      if (preProcessMovements) {
        qtyInBoxesWhere.append(" and e." + InternalMovementLine.PROPERTY_OBWPLITEMSTATUS
            + " = 'CF'");
      }
      Query qtyInBoxesQuery = OBDal.getInstance().getSession()
          .createQuery(qtyInBoxesWhere.toString());
      qtyInBoxesQuery.setParameter("pickingList", picking);
      result = (BigDecimal) qtyInBoxesQuery.uniqueResult();
      if (result == null) {
        result = BigDecimal.ZERO;
      }
      // 2nd
      if (preProcessMovements) {
        qtyInBoxesWhere.setLength(0);
        qtyInBoxesWhere.append(" select sum(e." + InternalMovementLine.PROPERTY_OBWPLPICKEDQTY
            + ")");
        qtyInBoxesWhere.append(" from " + InternalMovementLine.ENTITY_NAME + " e");
        if (picking.getDocumentType().isOBWPLIsGroup()) {
          qtyInBoxesWhere.append(" where e." + InternalMovementLine.PROPERTY_OBWPLGROUPPICKINGLIST
              + " = :pickingList");
        } else {
          qtyInBoxesWhere.append(" where e."
              + InternalMovementLine.PROPERTY_OBWPLWAREHOUSEPICKINGLIST + " = :pickingList");
        }
        qtyInBoxesWhere.append(" and e." + InternalMovementLine.PROPERTY_OBWPLITEMSTATUS
            + " = 'IC'");
        Query qtyInBoxesQuery2 = OBDal.getInstance().getSession()
            .createQuery(qtyInBoxesWhere.toString());
        qtyInBoxesQuery2.setParameter("pickingList", picking);
        result2 = (BigDecimal) qtyInBoxesQuery2.uniqueResult();
        if (result2 != null && result2.compareTo(BigDecimal.ZERO) > 0) {
          result = result.add(result2);
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private static BigDecimal getQtyInBoxes(PickingList picking) {
    BigDecimal result = BigDecimal.ZERO;
    OBContext.setAdminMode(true);
    try {
      StringBuffer qtyInBoxesWhere = new StringBuffer();
      qtyInBoxesWhere.append(" select coalesce(sum(e." + PickingListBoxContent.PROPERTY_QUANTITY + "),0)");
      qtyInBoxesWhere.append(" from " + PickingListBoxContent.ENTITY_NAME + " e");
      qtyInBoxesWhere.append(" left join e." + PickingListBoxContent.PROPERTY_OBWPLPLBOX + " b");
      qtyInBoxesWhere.append(" where b." + PickingListBox.PROPERTY_OBWPLPICKINGLIST
          + " = :pickingList");
      Query qtyInBoxesQuery = OBDal.getInstance().getSession()
          .createQuery(qtyInBoxesWhere.toString());
      qtyInBoxesQuery.setParameter("pickingList", picking);
      result = (BigDecimal) qtyInBoxesQuery.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
    
    if (result == null) {
    	throw new OBException("No Content in the boxes created");
      } 
    
    return result;
  }

  private static ShipmentInOut createShipment(Order order, DocumentType shipDocType,
      PickingList picking, PickingListBox box) {
    Set<Bean<?>> beansSet = WeldUtils.getStaticInstanceBeanManager().getBeans(
        ClosePLOutbound_CreateShipmentsHook.class);
    List<Bean<?>> beansList = new ArrayList<Bean<?>>();
    beansList.addAll(beansSet);

    ShipmentInOut shipment = OBProvider.getInstance().get(ShipmentInOut.class);
    shipment.setOrganization(order.getOrganization());
    shipment.setSalesTransaction(true);
    shipment.setMovementType("C-");
    shipment.setDocumentType(shipDocType);
    shipment.setDocumentNo(OBWPL_Utils.getDocumentNo(shipDocType, "M_InOut"));
    shipment.setWarehouse(order.getWarehouse());
    shipment.setBusinessPartner(order.getBusinessPartner());
    shipment.setPartnerAddress(order.getPartnerAddress());
    shipment.setDeliveryLocation(order.getDeliveryLocation());
    shipment.setDeliveryMethod(order.getDeliveryMethod());
    shipment.setDeliveryTerms(order.getDeliveryTerms());

    shipment.setMovementDate(new Date());
    shipment.setAccountingDate(new Date());
    shipment.setSalesOrder(order);
    shipment.setUserContact(order.getUserContact());
    shipment.setOrderReference(order.getOrderReference());
    shipment.setPriority(order.getPriority());

    shipment.setProject(order.getProject());
    shipment.setActivity(order.getActivity());
    shipment.setSalesCampaign(order.getSalesCampaign());
    shipment.setStDimension(order.getStDimension());
    shipment.setNdDimension(order.getNdDimension());

    if (box != null) {
      shipment.setObwplPlbox(box);
      shipment.setTrackingNo(box.getSearchKey());
    }

    for (Bean<?> abstractBean : beansList) {
      ClosePLOutbound_CreateShipmentsHook hook = (ClosePLOutbound_CreateShipmentsHook) WeldUtils
          .getStaticInstanceBeanManager().getReference(abstractBean,
              ClosePLOutbound_CreateShipmentsHook.class,
              WeldUtils.getStaticInstanceBeanManager().createCreationalContext(abstractBean));
      try {
        hook.exec(shipment, shipDocType, picking);
      } catch (Exception e) {
        log.error("An error happened when ClosePLOutbound_CreateShipmentsHook was executed.",
            e.getMessage(), e.getStackTrace());
      }
    }

    OBDal.getInstance().save(shipment);
    return shipment;
  }

  private static ShipmentInOutLine createShipmentLine(ShipmentInOut shipment,
      InternalMovementLine mvmtLine, OrderLine orderLine, BigDecimal movementQty) {
    ShipmentInOutLine shipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
    shipmentLine.setOrganization(shipment.getOrganization());
    shipmentLine.setShipmentReceipt(shipment);
    shipmentLine.setSalesOrderLine(orderLine);
    Long lineNo = (shipment.getMaterialMgmtShipmentInOutLineList().size() + 1) * 10L;
    shipmentLine.setLineNo(lineNo);
    shipmentLine.setProduct(orderLine.getProduct());
    shipmentLine.setUOM(orderLine.getUOM());
    shipmentLine.setAttributeSetValue(mvmtLine.getAttributeSetValue());
    shipmentLine.setStorageBin(mvmtLine.getNewStorageBin());
    shipmentLine.setMovementQuantity(movementQty);
    shipmentLine.setDescription(orderLine.getDescription());
    if (orderLine.getBOMParent() != null) {
      OBCriteria<ShipmentInOutLine> obc = OBDal.getInstance().createCriteria(
          ShipmentInOutLine.class);
      obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipment));
      obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine.getBOMParent()));
      obc.setMaxResults(1);
      shipmentLine.setBOMParent((ShipmentInOutLine) obc.uniqueResult());
    }

    OBDal.getInstance().save(shipmentLine);
    shipment.getMaterialMgmtShipmentInOutLineList().add(shipmentLine);
    OBDal.getInstance().save(shipment);

    return shipmentLine;
  }

  // private static void processShipment(String strShipmentId) {
  // final ProcessInstance pinstance = CallProcess.getInstance()
  // .call(inoutPost, strShipmentId, null);
  // if (pinstance.getResult() != 1) {
  // throw new OBException(OBMessageUtils.parseTranslation(pinstance.getErrorMsg()));
  // }
  // }

  private static String getTranslatedListValueName(Reference listRef, String value) {
    OBCriteria<org.openbravo.model.ad.domain.List> critList = OBDal.getInstance().createCriteria(
        org.openbravo.model.ad.domain.List.class);
    critList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, listRef));
    critList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, value));
    org.openbravo.model.ad.domain.List list = (org.openbravo.model.ad.domain.List) critList
        .uniqueResult();
    if (list == null) {
      return value;
    }
    // check if we have a translation
    OBCriteria<ListTrl> critListTrl = OBDal.getInstance().createCriteria(ListTrl.class);
    critListTrl.add(Restrictions.eq(ListTrl.PROPERTY_LISTREFERENCE, list));
    critListTrl.add(Restrictions.eq(ListTrl.PROPERTY_LANGUAGE, OBContext.getOBContext()
        .getLanguage()));
    ListTrl trl = (ListTrl) critListTrl.uniqueResult();
    if (trl != null) {
      return trl.getName();
    } else {
      return list.getName();
    }
  }

  private static Reference getPlstatusRef() {
    Reference plStatusRef = null;
    plStatusRef = OBDal.getInstance().get(Reference.class, "3F698D2435774CFAB5B850C7686E47F4");
    return plStatusRef;
  }

private static List<String> getZeroQtyBoxContent(PickingListBox box) {
		
		List<String> boxContentQryList = null;
	      try {
	        String qry = "select pbc.id from OBWPL_plbox pb left join pb.oBWPLPlBoxContentList pbc where pb.id='"
	            + box.getId() + "' and pbc.quantity=0";
	        Query boxContentQry = OBDal.getInstance().getSession().createQuery(qry);
	        boxContentQryList = boxContentQry.list();
	      } catch (Exception e) {
	        log.error("error in fetching content of box " + box + ". The error : " + e);
	      }
      return boxContentQryList;
}
  
  public static String checkStatus(List<InternalMovementLine> mvmtLines, boolean checkGroupStatus) {
    boolean hasComplete = false;
    boolean hasPending = false;
    boolean hasGrouped = false;
    boolean hasNotGrouped = false;
    String strStatus = "AS";
    // TODO:REVIEW move to scrollable
    for (InternalMovementLine mvmtLine : mvmtLines) {
      if (mvmtLine.isOBWPLRaiseIncidence()) {
        return "IN";
      }
      if (mvmtLine.getOBWPLItemStatus().equals("CF") || mvmtLine.getOBWPLItemStatus().equals("IC")) {
        hasComplete = true;
        strStatus = "CO";
      } else if (mvmtLine.getOBWPLItemStatus().equals("PE")) {
        hasPending = true;
      }
      if (mvmtLine.getOBWPLGroupPickinglist() != null) {
        hasGrouped = true;
      } else {
        hasNotGrouped = true;
      }
      if (hasPending && hasComplete) {
        strStatus = "IP";
        if (!checkGroupStatus) {
          return strStatus;
        }
      }
      if (checkGroupStatus && hasGrouped) {
        if (hasNotGrouped) {
          strStatus = "PG";
        } else {
          strStatus = "GR";
        }
      }
    }
    return strStatus;
  }

}
