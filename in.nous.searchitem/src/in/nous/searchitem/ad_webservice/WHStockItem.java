package in.nous.searchitem.ad_webservice;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;

public class WHStockItem extends HttpSecureAppServlet implements WebService{

	private static final long serialVersionUID = 1L;
	
	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception{
		// do some checking of parameters
	    StringWriter sw = new StringWriter();
	    String code = request.getParameter("code");
	    String storename = request.getParameter("storename");  
	    
	    if (code == null || code.equals("")) {
	      throw new IllegalArgumentException("The code parameter is mandatory");
	    }
	    if (storename == null || code.equals("")) {
	      throw new IllegalArgumentException("The storename parameter is mandatory");
	    }
	    
	    // for getting product info
	    Product product = null;
		final OBCriteria<Product> orgObCriteria = OBDal.getInstance().createCriteria(Product.class);
		orgObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, code));
		if(orgObCriteria.count() > 0) {
			product = orgObCriteria.list().get(0);
		}
	    
		//checking the north zone or south zone
	    Connection conn = null;
	    PreparedStatement pst = null;
 		ResultSet rs = null;
 		String zoneName = null;
 		try{
 			conn = OBDal.getInstance().getConnection();
 		}catch(Exception e){
 			e.printStackTrace();
 		}
 		
 		pst = conn.prepareStatement("select em_idsd_zone from ad_orginfo where ad_org_id in (select ad_org_id from ad_org where name = '"+storename+"')");
 		rs = pst.executeQuery();
 		while (rs.next()) {
 			zoneName = rs.getString("em_idsd_zone");
 		}
 		
 		rs.close();
 		pst.close();
 		pst = null;
 		rs = null;
 		
 		String cacActualQty = null;
 		String carActualQty = null;
 		String hubActualQty = null;
 		
 		if(null != zoneName){
 			if(zoneName.equalsIgnoreCase("SZ")){
 				
 		 		pst = conn.prepareStatement("SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
 		                        " FROM m_product mp" +
 		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
 		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
 		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
 		                        " WHERE mw.em_idsd_whgroup='RWH'" +
 		                        " and ml.isactive='Y'" +
 		                        " and ml.em_obwhs_type='ST'" +
 		                        " and mp.m_product_id='"+product.getId()+"'" +
 		                        " group by mp.m_product_id;");
 		 		rs = pst.executeQuery();
 		 		while (rs.next()) {
 		 			cacActualQty = ""+rs.getLong("msdqty");
 		 		}
 		 		rs.close();
 		 		pst.close();
 		 		if(null == cacActualQty || cacActualQty.equals("null"))
 		 			cacActualQty = "0";
 		 		pst = conn.prepareStatement("SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
 		                        " FROM m_product mp" +
 		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
 		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
 		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
 		                        " WHERE mw.em_idsd_whgroup='CWH'" +
 		                        " and ml.isactive='Y'" +
 		                        " and ml.em_obwhs_type='ST'" +
 		                        " and mp.m_product_id='"+product.getId()+"'" +
 		                        " group by mp.m_product_id;");
 		 		rs = pst.executeQuery();
 		 		while (rs.next()) {
 		 			carActualQty = ""+rs.getLong("msdqty");
 		 		}
 		 		if(null == carActualQty || carActualQty.equals("null"))
 		 			carActualQty = "0";
 			} else if(zoneName.equalsIgnoreCase("NZ")){
 				pst = conn.prepareStatement("SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
 		                        " FROM m_product mp" +
 		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
 		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
 		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
 		                        " WHERE mw.em_idsd_whgroup='HUB'" +
 		                        " and ml.isactive='Y'" +
 		                        " and ml.em_obwhs_type='ST'" +
 		                        " and mp.m_product_id='"+product.getId()+"'" +
 		                        " group by mp.m_product_id;");
 				rs = pst.executeQuery();
 		 		while (rs.next()) {
 		 			hubActualQty = ""+rs.getLong("msdqty");
 		 		}
 		 		rs.close();
 		 		pst.close();
 		 		if(null == hubActualQty || hubActualQty.equals("null"))
 		 			hubActualQty = "0";
 				
 				pst = conn.prepareStatement("SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
 		                        " FROM m_product mp" +
 		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
 		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
 		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
 		                        " WHERE mw.em_idsd_whgroup='RWH'" +
 		                        " and ml.isactive='Y'" +
 		                        " and ml.em_obwhs_type='ST'" +
 		                        " and mp.m_product_id='"+product.getId()+"'" +
 		                        " group by mp.m_product_id;");
 		 		rs = pst.executeQuery();
 		 		while (rs.next()) {
 		 			cacActualQty = ""+rs.getLong("msdqty");
 		 		}
 		 		if( null == cacActualQty || cacActualQty.equals("null"))
 		 			cacActualQty = "0";
 		 		rs.close();
 		 		pst.close();
 		 		
 		 		pst = conn.prepareStatement("SELECT mp.m_product_id as product_id,round(sum(msd.qtyonhand)-sum(msd.reservedqty)) as msdqty" +
 		                        " FROM m_product mp" +
 		                        " INNER JOIN m_storage_detail msd ON msd.m_product_id=mp.m_product_id" +
 		                        " INNER JOIN m_locator ml ON ml.m_locator_id=msd.m_locator_id" +
 		                        " INNER JOIN m_warehouse mw ON mw.m_warehouse_id=ml.m_warehouse_id" +
 		                        " WHERE mw.em_idsd_whgroup='CWH'" +
 		                        " and ml.isactive='Y'" +
 		                        " and ml.em_obwhs_type='ST'" +
 		                        " and mp.m_product_id='"+product.getId()+"'" +
 		                        " group by mp.m_product_id;");
 		 		rs = pst.executeQuery();
 		 		while (rs.next()) {
 		 			carActualQty = ""+rs.getLong("msdqty");
 		 		}
 		 		if( null == carActualQty || carActualQty.equals("null"))
 		 			carActualQty = "0";
 			}
 		} else {
 			// if zone value is null 
 			cacActualQty = "0";
 			carActualQty = "0";
 			hubActualQty = "0";
 			
 		}
 		
 		String EDD = "";
 	    try {
 	      pst = conn.prepareStatement("select coalesce(to_char(min(cc.em_sw_estshipdate),'dd/mm/yyyy'),'N/A') as innerEDD from C_Order as cc, M_Product as p,C_OrderLine as cc2 where cc2.M_Product_id=p.M_Product_id and cc2.C_Order_id in (select co.C_Order_id from C_Order co where not exists (select 1   from M_InOut sio where sio.C_Order_id=co.C_Order_ID) and co.em_sw_postatus in ('VD','SH','Underway','CD') and co.docstatus!='CO' and co.C_DocType_id in (select C_DocType_id from C_DocType where name like 'Purchase Order%')) and  cc.C_Order_id=cc2.C_Order_ID and p.isactive='Y' and p.name='"+ code + "'");
 	      rs = pst.executeQuery();
 	      while (rs.next()) {
 	        EDD = rs.getString("innerEDD");
 	      }
 	    } catch (Exception e) {
 	      e.printStackTrace();
 	    }

 		
 		
 	   sw.append("<items>");
 	   sw.append("<edd>"+EDD+"</edd>");
	   sw.append("<cacstock>"+cacActualQty+"</cacstock>");
	   sw.append("<carstock>"+carActualQty+"</carstock>");
	   sw.append("<hubstock>"+hubActualQty+"</hubstock>");
	   //sw.append("<itrqty>"++"</itrqty>");
	   //sw.append("<storeqty>"++"</storeqty>");
	   //sw.append("<edd>"++"</edd>");
	   sw.append("</items>");  
	    
	    // and get the result
	    final String xml = sw.toString();
	 
	    // write to the response
	    response.setContentType("text/xml");
	    response.setCharacterEncoding("utf-8");
	    final Writer w = response.getWriter();
	    w.write(xml);
	    w.close();
 		
 		
 		
 		//pst = conn.prepareStatement("select round(sum(sd.qtyonhand)-coalesce(round((select (sum(quantity)-sum(releasedqty)) from m_reservation_stock where m_locator_id in(select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and m_reservation_id in (select m_reservation_id from m_reservation where m_product_id = p.m_product_id)),0),'0'),0) as msdqty, p.m_product_id as product_id, p.name as itemcode from m_product p, m_storage_detail sd where sd.m_locator_id IN (select m_locator_id from m_locator where m_warehouse_id in (select m_warehouse_id from m_warehouse where name='Saleable Whitefield' and em_sw_iscac = 'Y') and isactive='Y' and em_obwhs_type='ST') and sd.m_product_id=p.m_product_id group by p.m_product_id, p.name having p.m_product_id='"+product.getId()+"';");
	   
	    
	    
	    
	    
	    
	    
	    
	    
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	}

}
