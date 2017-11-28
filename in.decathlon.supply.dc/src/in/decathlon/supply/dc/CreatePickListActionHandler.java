package in.decathlon.supply.dc;

import in.decathlon.supply.dc.util.SDC_Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.DbUtility;
import org.openbravo.warehouse.pickinglist.PickingList;

import com.sysfore.catalog.CLStoreDept;

public class CreatePickListActionHandler extends BaseActionHandler {

	final private static Logger log = Logger.getLogger(CreatePickListActionHandler.class);
	List<String> pickingLists = new ArrayList<String>();
	long lineNo;
	private String picklistId = "";
	
	@Override
	protected JSONObject execute(Map<String, Object> parameters, String content) {
		// TODO Auto-generated method stub
		JSONObject jsonRequest = null;
		try {
		      OBContext.setAdminMode(true);
		      
		      jsonRequest = new JSONObject(content);
		      final JSONArray orderIds = jsonRequest.getJSONArray("orders");
System.out.println("Order Ids from JS "+orderIds);
			  if(orderIds.length() > 1) {
				  return null;
			  }
		   // Get orders
		      boolean hasEnoughStock = true;
		      for (int i = 0; i < orderIds.length(); i++) {System.out.println("for loop starts");
		    	final Order so = OBDal.getInstance().get(Order.class, orderIds.getString(i));
		    	if(null != so) {
		    		if(so.isObwplIsinpickinglist() == true) {
		    			jsonRequest = new JSONObject();
				        JSONObject errorMessage = new JSONObject();
				        errorMessage.put("severity", "TYPE_ERROR");
				        errorMessage.put("text", "Picklist has been generated already for this document!");
				        jsonRequest.put("message", errorMessage);
				        return jsonRequest;
		    		}
		    		String soCLDeptId = "";
		    		if(null != so.getClStoredept()) {
		    			soCLDeptId = so.getClStoredept().getId();
		    			final String soOrgId = so.getOrganization().getId();
				    	final String soBPId = so.getBusinessPartner().getId();
				    	final String soWarehouseId = so.getWarehouse().getId();
				    	final String warehouse = so.getDocumentNo().substring(so.getDocumentNo().length()-4, so.getDocumentNo().length());
				    	System.out.println("warehouse "+warehouse);
						
				    	OBCriteria<Order> soCriteria = OBDal.getInstance().createCriteria(Order.class);
				    	if(!"".equals(soCLDeptId)) {
				    		soCriteria.add(Restrictions.eq(Order.PROPERTY_CLSTOREDEPT, OBDal.getInstance().get(CLStoreDept.class, soCLDeptId)));
				    	}
				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_ORGANIZATION, OBDal.getInstance().get(Organization.class, soOrgId)));
				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_BUSINESSPARTNER, OBDal.getInstance().get(BusinessPartner.class, soBPId)));
				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
//				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_SWISAUTOORDER, true));
				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_OBWPLISINPICKINGLIST, false));
				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_DOCUMENTSTATUS, "CO"));
//				    	soCriteria.add(Restrictions.eq(Order.PROPERTY_WAREHOUSE, OBDal.getInstance().get(Warehouse.class, soWarehouseId)));
				    	soCriteria.add(Restrictions.ilike(Order.PROPERTY_DOCUMENTNO, "%"+warehouse));
				    	soCriteria.setFilterOnReadableOrganization(false);
		
				    	final List<String> ordersPerStore = new ArrayList<String>();
				    	System.out.println(soCLDeptId);
				    	System.out.println(soOrgId);
				    	System.out.println(soBPId);
				    	if(soCriteria.count() > 0) {
				    		int count = 0;
				    		final ScrollableResults scroller = soCriteria.scroll(ScrollMode.FORWARD_ONLY);
				    		while (scroller.next()) {
				
				    			final Order oneOrder = (Order) scroller.get()[0];
				    			System.out.println(oneOrder.getId());
				    			// 	Adding into one ArrayList
				    			ordersPerStore.add(oneOrder.getId());
				    			// 	clear the session every 100 records
				    			if ((i % 100) == 0) {
				    				OBDal.getInstance().getSession().clear();
				    			}
				    			count++;
				    		}
						}
		System.out.println(ordersPerStore);
						// Performing Actual Logic
						if(ordersPerStore.size() > 0) {
							processOrdersNew(ordersPerStore, soBPId, soCLDeptId);
						}
		    		} else {
		    			processOrderOld(orderIds.getString(0));
		    		}
		    	}
			}
System.out.println("partially reserved "+pickingLists+" size is "+pickingLists.size());

			String strPickingListDocNos = "";
			if (pickingLists.size() > 0) {
			    
			    for (String plDocNo : pickingLists) {
			    	strPickingListDocNos += plDocNo + ", ";
			    }
			    strPickingListDocNos = strPickingListDocNos.substring(0, strPickingListDocNos.lastIndexOf(","));
			    strPickingListDocNos = strPickingListDocNos + "</br>"
			          + OBMessageUtils.messageBD("OBWPL_PartiallyReserved");
			} else {
				strPickingListDocNos = picklistId;
			}
		    JSONObject errorMessage = new JSONObject();
		    errorMessage.put("severity", "TYPE_SUCCESS");
		    errorMessage.put("text", strPickingListDocNos);
			
		    errorMessage.put("title", OBMessageUtils.messageBD("OBWPL_PickingList_Created"));
		    jsonRequest.put("message", errorMessage);
				
			
		} catch (Exception e) {
		      log.error("Error in CreateActionHandler", e);
		      OBDal.getInstance().rollbackAndClose();

		      try {
		        jsonRequest = new JSONObject();
		        Throwable ex = DbUtility.getUnderlyingSQLException(e);
		        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
		        JSONObject errorMessage = new JSONObject();
		        errorMessage.put("severity", "TYPE_ERROR");
		        errorMessage.put("text", message);
		        jsonRequest.put("message", errorMessage);
		      } catch (Exception e2) {
		        log.error("Error generating the error message", e2);
		        // do nothing, give up
		      }
		}
		return jsonRequest;
	}
	
	private void processOrdersNew(List<String> ordersPerStore, String bpId, String storeDeptId) {
		
		Organization org = null;
		final BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, bpId);
		final OBQuery<Organization> orgObQuery = OBDal.getInstance().createQuery(Organization.class, "as o where o.name='"+bp.getName()+"'");
		String OrgId = orgObQuery.list().get(0).getId();
		String storeValue = "";
		if(!"".equals(OrgId)) {
		  	org = OBDal.getInstance().get(Organization.class, OrgId);
		}
System.out.println("SO1 "+ordersPerStore.get(0));
		Order order = OBDal.getInstance().get(Order.class, ordersPerStore.get(0));
		// Create Picking List
		PickingList pickingList = OBProvider.getInstance().get(PickingList.class);
	    pickingList.setOrganization(order.getOrganization());
	    pickingList.setDocumentdate(new Date());
	    DocumentType plDocType = SDC_Utils.getDocumentType(order.getOrganization(), "OBWPL_doctype");
	    if (plDocType == null) {
	      throw new OBException(OBMessageUtils.messageBD("OBWPL_DoctypeMissing"));
	    }
	    pickingList.setDocumentType(plDocType);
	    pickingList.setDocumentNo(SDC_Utils.getDocumentNo(plDocType, "OBWPL_PickingList", OrgId, storeDeptId));
	    pickingList.setPickliststatus("DR");
	    pickingList.setSwStatus("AP");	
	    
	    StringBuilder msg = new StringBuilder(); 
//	    msg.append(OBMessageUtils.messageBD("OBWPL_docNo"));
	    int i=1;
	    int sizeOfList = ordersPerStore.size();
	    if("1".equals(org.getSearchKey())) {
	    	storeValue = "SJ";
	  	} else if("5".equals(org.getSearchKey())) {
	  		storeValue = "BGT";
	  	} else {
	  		storeValue = org.getSearchKey();
	  	}
	    Order oneOrdr = OBDal.getInstance().get(Order.class, ordersPerStore.get(0));
	    msg.append(storeValue+"-"+oneOrdr.getDocumentNo().substring(oneOrdr.getDocumentNo().length()-3, oneOrdr.getDocumentNo().length())+"-"+sizeOfList+" DC's: ");
	    for (String orderId : ordersPerStore) {
	    	
			Order ordr = OBDal.getInstance().get(Order.class, orderId);
			ordr.setObwplIsinpickinglist(true);
			if(sizeOfList == i) {
				msg.append(ordr.getDocumentNo().substring(ordr.getDocumentNo().length()-8, ordr.getDocumentNo().length()-4));
			} else {
				msg.append(ordr.getDocumentNo().substring(ordr.getDocumentNo().length()-8, ordr.getDocumentNo().length()-4)+",");
			}
			i++;
		}
//	    msg.append(" "+OBMessageUtils.messageBD("OBWPL_BPartner") + order.getBusinessPartner().getName());
//	    String msg = OBMessageUtils.messageBD("OBWPL_docNo") + "" + order.getDocumentNo() + " "
//	        + OBMessageUtils.messageBD("OBWPL_BPartner") + order.getBusinessPartner().getName();
	    if(msg.length() > 255) {
	    	msg.substring(0, 255);
	    }
	    pickingList.setDescription(msg.toString());
	    OBDal.getInstance().save(pickingList);
	    
System.out.println("picklist "+pickingList.getId());
		picklistId = pickingList.getDocumentNo();
	    OBDal.getInstance().flush();
	    OBDal.getInstance().commitAndClose();
	    boolean hasEnoughStock = true;
	    for (String orderId : ordersPerStore) {
			Order ordr = OBDal.getInstance().get(Order.class, orderId);
			
			boolean hasEnoughStock2 = processOrder(ordr.getId(), pickingList);
			if(!hasEnoughStock2){
				pickingLists.add(ordr.getDocumentNo());
			}
System.out.println(hasEnoughStock2);				
	    }
	}

	private boolean processOrder(String strOrderId, PickingList pickingList) {
		// TODO Auto-generated method stub.
		
		Order order = OBDal.getInstance().get(Order.class, strOrderId);
//		if (order.isObwplIsinpickinglist()) {
//			throw new OBException(OBMessageUtils.messageBD("OBWPL_IsInPL") + order.getDocumentNo());
//	    }
//		order.setObwplIsinpickinglist(true);
		
		/*// Create Picking List
		PickingList pickingList = OBProvider.getInstance().get(PickingList.class);
	    pickingList.setOrganization(order.getOrganization());
	    pickingList.setDocumentdate(new Date());
	    DocumentType plDocType = ADC_Utils.getDocumentType(order.getOrganization(), "OBWPL_doctype");
	    if (plDocType == null) {
	      throw new OBException(OBMessageUtils.messageBD("OBWPL_DoctypeMissing"));
	    }
	    pickingList.setDocumentType(plDocType);
	    pickingList.setDocumentNo(ADC_Utils.getDocumentNo(plDocType, "OBWPL_PickingList"));
	    pickingList.setPickliststatus("DR");
	    pickingList.setSwStatus("AP");	
	    String msg = OBMessageUtils.messageBD("OBWPL_docNo") + "" + order.getDocumentNo() + " "
	        + OBMessageUtils.messageBD("OBWPL_BPartner") + order.getBusinessPartner().getName();
	    pickingList.setDescription(msg);
	    OBDal.getInstance().save(pickingList);
	    pickingLists.add(pickingList.getDocumentNo());*/
	    
	    // Create In Out
	    ShipmentInOut shipment = OBProvider.getInstance().get(ShipmentInOut.class);
	    shipment.setOrganization(order.getOrganization());
	    shipment.setSalesTransaction(true);
	    shipment.setMovementType("C-");
	    DocumentType shipDocType = SDC_Utils.getDocumentType(order.getOrganization(), "MMS");
	    shipment.setDocumentType(shipDocType);
	    shipment.setTrxOrganization(order.getTrxOrganization()); 
	    shipment.setDocumentNo(order.getDocumentNo());
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
	    shipment.setFreightCostRule(order.getFreightCostRule());
	    shipment.setFreightAmount(order.getFreightAmount());
	    shipment.setShippingCompany(order.getShippingCompany());
	    shipment.setPriority(order.getPriority());

	    shipment.setProject(order.getProject());
	    shipment.setActivity(order.getActivity());
	    shipment.setSalesCampaign(order.getSalesCampaign());
	    shipment.setStDimension(order.getStDimension());
	    shipment.setNdDimension(order.getNdDimension());
	    shipment.setTrxOrganization(order.getTrxOrganization());

	    shipment.setDocumentStatus("DR");
	    shipment.setDocumentAction("CO");
	    shipment.setProcessNow(false);
	    OBDal.getInstance().save(shipment);
	    lineNo = 10L;
	    boolean hasEnoughStock = true;
	    
	    for (OrderLine orderLine : order.getOrderLineList()) {
	        // Only consider pending to deliver lines of stocked item products.
	        if (orderLine.getProduct() != null && orderLine.getProduct().isStocked()
	            && orderLine.getProduct().getProductType().equals("I")
	            && orderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) != 0
	            && orderLine.getOrderedQuantity().compareTo(orderLine.getDeliveredQuantity()) != 0) {
	          boolean hasEnoughStock2 = processOrderLine(orderLine, shipment, pickingList);
	          if (!hasEnoughStock2) {
	            hasEnoughStock = false;
	          }
	        } else if (orderLine.getProduct() != null
	            && orderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) != 0
	            && orderLine.getOrderedQuantity().compareTo(orderLine.getDeliveredQuantity()) != 0) {
	          processRestOrderLine(orderLine, shipment, pickingList);
	        }
	      }
	      OBDal.getInstance().refresh(shipment);
	      if (shipment.getMaterialMgmtShipmentInOutLineList().size() == 0) {
	        throw new OBException(OBMessageUtils.messageBD("NotEnoughAvailableStock"));
	      }
	      return hasEnoughStock;
	}
	
	private boolean processOrderOld(String strOrderId) {
		// TODO Auto-generated method stub.
		
		Order order = OBDal.getInstance().get(Order.class, strOrderId);
		if (order.isObwplIsinpickinglist()) {
			throw new OBException(OBMessageUtils.messageBD("OBWPL_IsInPL") + order.getDocumentNo());
	    }
		order.setObwplIsinpickinglist(true);
		
		// Create Picking List
		PickingList pickingList = OBProvider.getInstance().get(PickingList.class);
	    pickingList.setOrganization(order.getOrganization());
	    pickingList.setDocumentdate(new Date());
	    DocumentType plDocType = SDC_Utils.getDocumentType(order.getOrganization(), "OBWPL_doctype");
	    if (plDocType == null) {
	      throw new OBException(OBMessageUtils.messageBD("OBWPL_DoctypeMissing"));
	    }
	    pickingList.setDocumentType(plDocType);
	    pickingList.setDocumentNo(SDC_Utils.getDocumentNoOld(plDocType, "OBWPL_PickingList"));
	    pickingList.setPickliststatus("DR");
	    pickingList.setSwStatus("AP");	
	    String msg = OBMessageUtils.messageBD("OBWPL_docNo") + "" + order.getDocumentNo() + " "
	        + OBMessageUtils.messageBD("OBWPL_BPartner") + order.getBusinessPartner().getName();
	    pickingList.setDescription(msg);
	    OBDal.getInstance().save(pickingList);
	    pickingLists.add(pickingList.getDocumentNo());
	    
	    // Create In Out
	    ShipmentInOut shipment = OBProvider.getInstance().get(ShipmentInOut.class);
	    shipment.setOrganization(order.getOrganization());
	    shipment.setSalesTransaction(true);
	    shipment.setMovementType("C-");
	    DocumentType shipDocType = SDC_Utils.getDocumentType(order.getOrganization(), "MMS");
	    shipment.setDocumentType(shipDocType);
	    shipment.setTrxOrganization(order.getTrxOrganization()); 
	    shipment.setDocumentNo(order.getDocumentNo());
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
	    shipment.setFreightCostRule(order.getFreightCostRule());
	    shipment.setFreightAmount(order.getFreightAmount());
	    shipment.setShippingCompany(order.getShippingCompany());
	    shipment.setPriority(order.getPriority());

	    shipment.setProject(order.getProject());
	    shipment.setActivity(order.getActivity());
	    shipment.setSalesCampaign(order.getSalesCampaign());
	    shipment.setStDimension(order.getStDimension());
	    shipment.setNdDimension(order.getNdDimension());
	    shipment.setTrxOrganization(order.getTrxOrganization());

	    shipment.setDocumentStatus("DR");
	    shipment.setDocumentAction("CO");
	    shipment.setProcessNow(false);
	    OBDal.getInstance().save(shipment);
	    lineNo = 10L;
	    boolean hasEnoughStock = true;
	    
	    for (OrderLine orderLine : order.getOrderLineList()) {
	        // Only consider pending to deliver lines of stocked item products.
	        if (orderLine.getProduct() != null && orderLine.getProduct().isStocked()
	            && orderLine.getProduct().getProductType().equals("I")
	            && orderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) != 0
	            && orderLine.getOrderedQuantity().compareTo(orderLine.getDeliveredQuantity()) != 0) {
	          boolean hasEnoughStock2 = processOrderLine(orderLine, shipment, pickingList);
	          if (!hasEnoughStock2) {
	            hasEnoughStock = false;
	          }
	        } else if (orderLine.getProduct() != null
	            && orderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) != 0
	            && orderLine.getOrderedQuantity().compareTo(orderLine.getDeliveredQuantity()) != 0) {
	          processRestOrderLine(orderLine, shipment, pickingList);
	        }
	      }
	      OBDal.getInstance().refresh(shipment);
	      if (shipment.getMaterialMgmtShipmentInOutLineList().size() == 0) {
	        throw new OBException(OBMessageUtils.messageBD("NotEnoughAvailableStock"));
	      }
	      return hasEnoughStock;
	}
	
	private boolean processOrderLine(OrderLine orderLine,
			ShipmentInOut shipment, PickingList pickingList) {
		// TODO Auto-generated method stub
		
		// Reserve Order Line
	    boolean hasEnoughStock = true;
	    boolean existsReservation = !orderLine.getMaterialMgmtReservationList().isEmpty();
	    Reservation res = ReservationUtils.getReservationFromOrder(orderLine);
	    if (res.getRESStatus().equals("DR")) {
	      ReservationUtils.processReserve(res, "PR");
	    } else if (res.getQuantity().compareTo(res.getReservedQty()) != 0) {
	      //call the reservation and try to book
	      OBDal.getInstance().flush();
	      //createReservation(orderline); 
	    }
	// Copying qty to reserved qty for ATP logic
	    res.setReservedQty(res.getQuantity());
	    OBDal.getInstance().save(res);
	//
	    for (ReservationStock a : res.getMaterialMgmtReservationStockList()) {
	      if (!a.isAllocated()) {
	        a.setAllocated(true);
	        OBDal.getInstance().save(a);
	        OBDal.getInstance().flush();
	      }
	    }

	    if (!existsReservation) {
	      res.setOBWPLGeneratedByPickingList(true);
	      OBDal.getInstance().save(res);
	      OBDal.getInstance().flush();
	    }
	    OBDal.getInstance().refresh(res);
	    if (res.getQuantity().compareTo(res.getReservedQty()) != 0) {
	      hasEnoughStock = false;
	    }

	    List<ShipmentInOutLine> shipmentLines = new ArrayList<ShipmentInOutLine>();
	    for (ReservationStock resStock : res.getMaterialMgmtReservationStockList()) {
	      if (resStock.getQuantity().subtract(resStock.getReleased()).compareTo(BigDecimal.ZERO) > 0) {
	        // Create InOut line.
	        ShipmentInOutLine line = OBProvider.getInstance().get(ShipmentInOutLine.class);
	        line.setOrganization(shipment.getOrganization());
	        line.setShipmentReceipt(shipment);
	        line.setSalesOrderLine(orderLine);
	        line.setObwplPickinglist(pickingList);
	        line.setLineNo(lineNo);
	        lineNo += 10L;
	        line.setProduct(orderLine.getProduct());
	        line.setUOM(orderLine.getUOM());
	        line.setAttributeSetValue(resStock.getAttributeSetValue());
	        line.setStorageBin(resStock.getStorageBin());
	        line.setMovementQuantity(resStock.getQuantity().subtract(resStock.getReleased()));
	        line.setSwStockReservation(resStock.getId());
	        OBDal.getInstance().save(line);
	        shipmentLines.add(line);
	      }
	    }
	    OBDal.getInstance().flush();
	    return hasEnoughStock;
	}

	private void processRestOrderLine(OrderLine orderLine,
			ShipmentInOut shipment, PickingList pickingList) {
		// TODO Auto-generated method stub
		
		ShipmentInOutLine line = OBProvider.getInstance().get(ShipmentInOutLine.class);
	    line.setOrganization(shipment.getOrganization());
	    line.setShipmentReceipt(shipment);
	    line.setSalesOrderLine(orderLine);
	    line.setObwplPickinglist(pickingList);
	    line.setLineNo(lineNo);
	    lineNo += 10L;
	    line.setProduct(orderLine.getProduct());
	    line.setUOM(orderLine.getUOM());
	    line.setAttributeSetValue(orderLine.getProduct().getAttributeSetValue());
	    line.setMovementQuantity(orderLine.getOrderedQuantity().subtract(
	        orderLine.getDeliveredQuantity()));

	    OBDal.getInstance().save(line);
	    OBDal.getInstance().flush();
	}

}
