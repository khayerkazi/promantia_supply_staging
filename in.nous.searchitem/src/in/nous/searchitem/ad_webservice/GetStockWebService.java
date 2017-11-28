/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.nous.searchitem.ad_webservice;
 
import in.decathlon.integration.PassiveDB;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.service.web.WebService;
 
/**
 * Get the stock of CAC,CAR,ITR
 * and Store
 * @author shreyas
 */
public class GetStockWebService implements WebService {
 
  private static final long serialVersionUID = 1L;
 
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
	  
	Connection conn = null;
	conn = PassiveDB.getInstance().getSupplyDBConnection();
	PreparedStatement pst = null;
	ResultSet rs = null;
	
    // do some checking of parameters
    StringWriter sw = new StringWriter();
    String code = request.getParameter("code");
    String storename = request.getParameter("storename");  
    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
    if (storename == null) {
      throw new IllegalArgumentException("The storename parameter is mandatory");
    }

//    Long cacActualQty = 0L;  
//    try {
//		pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.qtyonhand >0 and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.name='"+code+"';");
//		rs = pst.executeQuery();
//		while (rs.next()) {
//			cacActualQty = rs.getLong("msdqty");
//		}
//    } catch (Exception e) {
//    	e.printStackTrace();
//	}
//
//    Long carActualQty = 0L; 
//    try {
//		pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Omega' and em_sw_iscar = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.qtyonhand >0 and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.name='"+code+"';");
//		rs = pst.executeQuery();
//		while (rs.next()) {
//			carActualQty = rs.getLong("msdqty");
//		}
//    } catch (Exception e) {
//    	e.printStackTrace();
//	}
    

//    String hql = " SELECT COALESCE(sum(quantityOnHand),0) as cacqty from MaterialMgmtStorageDetail where storageBin.warehouse.name like 'I%Transit%Warehouse' and product.name = '"+code+"'";
//    Query query = OBDal.getInstance().getSession().createQuery(hql);
//    String itrQty = query.list().get(0).toString();  


    String hql =" SELECT COALESCE(sum(quantityOnHand),0) as storeqty from MaterialMgmtStorageDetail where storageBin.warehouse.name like 'Saleable%' and storageBin.organization.name = '"+storename+"'  and product.name = '"+code+"' and attributeSetValue = '0'";
    Query query = OBDal.getInstance().getSession().createQuery(hql);
    String storeQty = query.list().get(0).toString();
    
//    String EDD = "";
//   try {
//		pst = conn.prepareStatement("select coalesce(to_char(min(cc.em_sw_estshipdate),'dd/mm/yyyy'),'N/A') as innerEDD from C_Order as cc, M_Product as p,C_OrderLine as cc2 where cc2.M_Product_id=p.M_Product_id and cc2.C_Order_id in (select co.C_Order_id from C_Order as co where co.C_DocType_id in (select C_DocType_id from C_DocType where name like 'Purchase Order%') and co.em_sw_postatus in ('VD','SH','Underway','CD') and co.docstatus!='CO' and not exists (select sio.C_Order_ID from M_InOut as sio where co.C_Order_id=sio.C_Order_ID)) and  cc.C_Order_id=cc2.C_Order_ID and p.isactive='Y' and p.name='"+code+"'");
//		rs = pst.executeQuery();
//		while (rs.next()) {
//			EDD = rs.getString("innerEDD");
//		}
//   } catch (Exception e) {
//   	e.printStackTrace();
//	}
   
	// store stock based on wh zone
	String whZone = "";
	final OBQuery<OrganizationInformation> orgInfoCriteria = OBDal.getInstance().createQuery(OrganizationInformation.class, "as o where o.organization.name='"+storename+"'");
	if(orgInfoCriteria.count() > 0) {
		whZone = orgInfoCriteria.list().get(0).getIdsdZone();
	}
//	Long hubActualQty = 0L; 
//	if(whZone.equals("NZ")) {
//	    try {
//			pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Hub') and isactive='Y' and em_obwhs_type='ST') and sd.qtyonhand >0 and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.name='"+code+"';");
//			rs = pst.executeQuery();
//			while (rs.next()) {
//				hubActualQty = rs.getLong("msdqty");
//			}
//	    } catch (Exception e) {
//	    	e.printStackTrace();
//		} finally {
//			try {
//				if(conn != null) {
//					conn.close();
//				}
//				if(pst != null && rs != null) {
//					pst.close();
//					rs.close();
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}
	
	
   sw.append("<items>");
//   sw.append("<cacstock>"+cacActualQty.intValue()+"</cacstock>");
//   sw.append("<carstock>"+carActualQty.intValue()+"</carstock>");
//   sw.append("<itrqty>"+itrQty+"</itrqty>");
//   if(whZone.equals("NZ")) {
//	   sw.append("<hubqty>"+hubActualQty.intValue()+"</hubqty>");
//   }
   sw.append("<storeqty>"+storeQty+"</storeqty>");
//   sw.append("<edd>"+EDD+"</edd>");
   sw.append("</items>");  
    
    // and get the result
    final String xml = sw.toString();
 
    // write to the response
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(xml);
    w.close();
  }
 
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
 
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
 
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
}