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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.service.web.WebService;
 
/**
 * Post the stock of the store
 * and create a PI
 * @author shreyas
 */
public class GetWidgetDetailsWebService implements WebService {
 
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(GetWidgetDetailsWebService.class);
 
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // do some checking of parameters
	  
	  // Supply DB Connections
	  Connection conn = null;
		//conn = PassiveDB.getInstance().getSupplyDBConnection();
	    conn = OBDal.getInstance().getConnection();
		PreparedStatement pst = null;
		ResultSet rs = null;
		
    StringWriter sw = new StringWriter();
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd");
    String code = request.getParameter("code");
    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
    String storename = request.getParameter("storename");
    if (storename == null) {
      throw new IllegalArgumentException("The storename parameter is mandatory");
    }
    String hql = "";
     Query query = null;
    //System.out.println(storename);
    try
    {
        hql = "select clMrpprice,clCcunitprice,clCcueprice,clCcpcbprice,COALESCE(cLEMClSuqty,0),COALESCE(cLEMClSboxqty,0),clCessionprice,updated,updatedBy.name, clUnitmarginpercentage, clUemarginpercentage, clPcbmarginpercentage,   clUnitmarginamount, clUemarginamount, clPcbmarginamount from PricingProductPrice where product.name='"+code+"' and organization.name='"+storename+"' order by creationDate desc";
        query = OBDal.getInstance().getSession().createQuery(hql);
        query.setMaxResults(1);
        List list = query.list();
        if(list.size()>0)
        {   	
        
        for (Object rows : list) 
       {
		Object[] row = (Object[]) rows;
		//System.out.println(row[1].toString());
	        sw.append("<items>");
		sw.append("<mrp>"+row[0].toString()+"</mrp>");
	        sw.append("<unitprice>"+row[1].toString()+"</unitprice>"); 
		sw.append("<ueprice>"+row[2].toString()+"</ueprice>"); 
		sw.append("<pcbprice>"+row[3].toString()+"</pcbprice>"); 
		sw.append("<strqty>"+row[4].toString()+"</strqty>"); 
		sw.append("<boxqty>"+row[5].toString()+"</boxqty>");
		sw.append("<cessionprice>"+row[6].toString()+"</cessionprice>"); 
                String date = sdf.format(myFormat.parse(row[7].toString().substring(0, 10)));
                String time = row[7].toString().substring(11, 16);
                String updatedPriceString = "Last Price Update done on "+date+" at "+time+" by "+row[8].toString();
                sw.append("<updateprice>"+updatedPriceString+"</updateprice>");
		
        if(!(row[9]==null))
        {
        	sw.append("<unitmargin>"+Double.parseDouble(row[9].toString())+"</unitmargin>");
        }else{
        	sw.append("<unitmargin>"+""+"</unitmargin>");
        }
		
        if(!(row[10]==null))
        {
        	sw.append("<uemargin>"+Double.parseDouble(row[10].toString())+"</uemargin>");
        }else{
        	sw.append("<uemargin>"+""+"</uemargin>");
        }
        
        if(!(row[11]==null))
        {
        	sw.append("<pcbmargin>"+Double.parseDouble(row[11].toString())+"</pcbmargin>");
        }else{
        	sw.append("<pcbmargin>"+""+"</pcbmargin>");
        }
		
		if(!(row[12]==null))
		{
			sw.append("<unitmarginamount>"+Double.parseDouble(row[12].toString())+"</unitmarginamount>");
			
		}else{
			
			sw.append("<unitmarginamount>"+""+"</unitmarginamount>");
		}
		
		if(!(row[13]==null))
		{
			sw.append("<uemarginamount>"+Double.parseDouble(row[13].toString())+"</uemarginamount>");
		}else{
			sw.append("<uemarginamount>"+""+"</uemarginamount>");
		}
		
		if(!(row[14]==null))
		{
			sw.append("<pcbmarginamount>"+Double.parseDouble(row[14].toString())+"</pcbmarginamount>");
		}else{
			sw.append("<pcbmarginamount>"+""+"</pcbmarginamount>");
		}
		

                
        }
    }else{
    	
    	sw.append("<items>");
		sw.append("<mrp>"+""+"</mrp>");
	        sw.append("<unitprice>"+""+"</unitprice>"); 
		sw.append("<ueprice>"+""+"</ueprice>"); 
		sw.append("<pcbprice>"+""+"</pcbprice>"); 
		sw.append("<strqty>"+""+"</strqty>"); 
		sw.append("<boxqty>"+""+"</boxqty>");
		sw.append("<cessionprice>"+""+"</cessionprice>"); 
                /*String date = sdf.format(myFormat.parse(row[7].toString().substring(0, 10)));
                String time = row[7].toString().substring(11, 16);
                String updatedPriceString = "Last Price Update done on "+date+" at "+time+" by "+row[8].toString();*/
                sw.append("<updateprice>"+""+"</updateprice>");
		
		sw.append("<unitmargin>"+""+"</unitmargin>");
		sw.append("<uemargin>"+""+"</uemargin>");
		sw.append("<pcbmargin>"+""+"</pcbmargin>");

		sw.append("<unitmarginamount>"+""+"</unitmarginamount>");
		sw.append("<uemarginamount>"+""+"</uemarginamount>");
		sw.append("<pcbmarginamount>"+""+"</pcbmarginamount>");
    	
    }
        
        
        
     // Implementation for Margin
        String taxCategoryId = null;
        String adOrgId = null;
        String regionId= null;
        String regionName = null;
        String modelId = null;
        Double taxRate = 0.0;
        Double taxRate1 = 0.0;
        Double taxRate2 = 0.0;
        Double octroi = 0.0;
        
	        hql = "select taxCategory.id from Product p where p.name='"+code+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List taxList = query.list();
        	taxCategoryId = taxList.get(0).toString();
        	//log.info("taxCategoryId ="+taxCategoryId);
	        	
	      
	        hql = "select org.id from Organization org where org.name = '"+storename+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List storeIdList = query.list();
	        adOrgId = storeIdList.get(0).toString();
        	//log.info("adOrgId ="+adOrgId);
	        	
	      
	        hql = 	" select l.region.id from OrganizationInformation oi, Location l where oi.id='"+adOrgId+"' " +
	        		" and oi.locationAddress=l.id";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List regionIdList = query.list();
        	regionId = regionIdList.get(0).toString();
        	//log.info("regionId ="+regionId);
	        	
	        
	        hql = "select ct.rate from FinancialMgmtTaxRate ct where ct.region.id = '"+regionId+"' and ct.taxCategory.id ='"+taxCategoryId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List rateList1 = query.list();
		//System.out.println("taxrate1 list-->"+rateList1.size());
	        if(rateList1.size() > 0){
	        	taxRate1 = Double.parseDouble(rateList1.get(0).toString());
	        }
	       // log.info("taxRate1 ="+taxRate1);
		//System.out.println("taxRate1-->"+taxRate1);
	        	
	        
	        hql = 	" select ct.rate from FinancialMgmtTaxRate ct ,FinancialMgmtTaxZone ctz where ct.id = ctz.tax " +
	        		" and ctz.fromRegion.id = '"+regionId+"' and ct.taxCategory.id ='"+taxCategoryId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List rateList2 = query.list();
		//System.out.println("taxrate2 List-->"+rateList2.size());
	        if(rateList2.size() > 0){
	        	taxRate2 = Double.parseDouble(rateList2.get(0).toString());
	        }
	       // log.info("taxRate2 ="+taxRate2);
		//System.out.println("taxRate2-->"+taxRate2);		
	        
	        if(taxRate1 != 0.00){
	        	taxRate = taxRate1;
			//System.out.println("first if");
	        }else if(taxRate2 != 0.00){
	        	taxRate = taxRate2;
			//System.out.println("second if");
	        }else{
	        	taxRate = 0.00;
			//System.out.println("else if");
	        }
	        
	       // log.info("Tax Rate="+taxRate);
	        sw.append("<taxrate>"+taxRate+"</taxrate>");
	        
	        
	        
	        hql = 	" select c.searchKey from Organization ad, OrganizationInformation ai, Location l, Region c " +
	        		" where ad.id = ai.id and ai.locationAddress.id = l.id " +
	        		" and l.region.id = c.id and ad.id = '"+adOrgId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List regionValueList = query.list();
	        regionName = regionValueList.get(0).toString();
	        //log.info("regionName ="+regionName);
	        sw.append("<regionName>"+regionName+"</regionName>");
	        
	        
	        hql = 	" select p.clModel.id from Product p where p.name ='"+code+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List modelList = query.list();
        	modelId = modelList.get(0).toString();
        	//log.info("modelId ="+modelId);
	        
	       
                // hql = 	" select cm.octroi from CL_Model cm where cm.id ='"+modelId+"'";
                //The above query is commented now we use octroi(lbt value) from m_productprice table
                hql = "select clLbtrate from PricingProductPrice  where product.name='"+code+"' and organization.name='"+storename+"' ";
                query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List octroiList = query.list();
		    		if(octroiList.get(0) != null){
		    octroi = Double.parseDouble(octroiList.get(0).toString());
		}

                // log.info("octroi ="+octroi);
	        sw.append("<octroi>"+octroi+"</octroi>");
	        
        
        
        
        
        
      hql = "select clModel.brand.name,clModel.merchandiseCategory,clColor.name,clGender,clSize,clModelcode,idsdOxylaneProdcat.id,clLogRec from Product where name='"+code+"' order by creationDate desc";
        query = OBDal.getInstance().getSession().createQuery(hql);
        query.setMaxResults(1);
        list = query.list();
        for (Object rows : list) 
       {
        	String log_rech = "";
			String prodCat = "";
           Object[] row = (Object[]) rows;



           sw.append("<brand>"+row[0].toString()+"</brand>");
           sw.append("<family>"+row[1].toString()+"</family>");
           sw.append("<color>"+row[2].toString()+"</color>");
           hql = "select name from ADList where searchKey='"+row[3].toString()+"' and reference.name='GenderCat'";



           query = OBDal.getInstance().getSession().createQuery(hql);
           sw.append("<gender>"+query.list().get(0).toString()+"</gender>");
           sw.append("<size>"+row[4].toString()+"</size>");
		   sw.append("<modelcode>"+row[5].toString()+"</modelcode>");
		   
		   if(row[6] == null){
			   sw.append("<prodcategory>N/A</prodcategory>");
		   } else {
			   // for product category
			   hql = "select commercialName from idsd_oxylane_prodcategory where id='"+row[6].toString()+"'";
		       query = OBDal.getInstance().getSession().createQuery(hql);
		       sw.append("<prodcategory>"+query.list().get(0).toString()+"</prodcategory>");
		   }
		   
		  
	       // for logistic recharge
		   if(null != row[7]) {
			   if (row[7].toString().equals("145") || row[7].toString().equals("54")
				          || row[7].toString().equals("null") || row[7].toString().equals("0")
				          || row[7].toString().equals("") || (row[7].toString() == null)) {
		    	   log_rech = "Non-Standard";
		       } else if (row[7].toString().equals("9.7") || row[7].toString().equals("5")) {
		    	   log_rech = "Standard";
		       }
		   }
	       
	       if(log_rech.equals("Standard")) {
	    	   sw.append("<cllogrec>"+log_rech+"</cllogrec>");
	       } else {
	    	   sw.append("<cllogrec>Non-Standard</cllogrec>");
	       }
	       
	   
       }

       hql = "select COALESCE(minQty,0),COALESCE(maxQty,0),COALESCE(displaymin,0),COALESCE(isinrange,'N'),updated,updatedBy.name from CL_Minmax where product.name='"+code+"' and organization.name = '"+storename+"' order by updated desc";
        query = OBDal.getInstance().getSession().createQuery(hql);
        query.setMaxResults(1);
        list = query.list();
         for (Object rows : list) 
       {
           Object[] row = (Object[]) rows;
           if(row[3].toString().equals("true"))
           {
             sw.append("<range>green</range>");
           }
           else
           {
             sw.append("<range>red</range>");
           }
           sw.append("<displayMin>"+row[2].toString()+"</displayMin>");
           sw.append("<min>"+row[0].toString()+"</min>");
           sw.append("<max>"+row[1].toString()+"</max>");
           String date = sdf.format(myFormat.parse(row[4].toString().substring(0, 10)));
           String time = row[4].toString().substring(11, 16);
           String updatedPriceString = "Last Displaymin Update done on "+date+" at "+time+" by "+row[5].toString();
           sw.append("<updatedispmin>"+updatedPriceString+"</updatedispmin>");
       }       
         
         // manualdc from Supply
         Long manualdc = 0L;
         try {
     		//pst = conn.prepareStatement("select coalesce(sum(qtyordered),0) - coalesce(sum(qtydelivered),0) as msdqty from c_orderline where m_product_id = (select m_product_id from m_product where name='"+code+"') and c_order_id in (select c_order_id from c_order where issotrx = 'Y' and createdby!='100' and c_bpartner_id = (select c_bpartner_id from c_bpartner where name = '"+storename+"') and docstatus IN ('OBWPC_PK','CO','IBDO_PK'))");
            //select coalesce(sum(ac),0) as msdqty from (select case when o.em_sw_po_reference is null then sum(ol.qtyordered-cast(coalesce(ol.em_sw_recqty,'0') as integer)) else sum(ol.em_sw_confirmedqty-cast(coalesce(ol.em_sw_recqty,'0') as integer)) end as ac from c_orderline ol join c_order o on o.c_order_id = ol.c_order_id join c_doctype cd on cd.c_doctype_id=o.c_doctype_id where  o.issotrx = 'N' and o.docstatus not in ('DR','VO','CL','OBWPL_SH','IBDO_SH') and o.createdby = '100' and o.em_sw_isauto_order='Y' and ol.ad_org_id = (select ad_org_id from ad_org where name = '"+storename+"') and ol.m_product_id = (select m_product_id from m_product where name='"+code+"') and cd.isreturn='N' group by  o.em_sw_po_reference) a
     		pst = conn.prepareStatement("select coalesce(sum(ac),0) as msdqty from (select case when o.em_sw_po_reference is null then sum(ol.qtyordered-cast(coalesce(ol.em_sw_recqty,'0') as integer)-cast(coalesce(mn.movementqty,'0') as integer)) else sum(ol.em_sw_confirmedqty-cast(coalesce(ol.em_sw_recqty,'0') as integer)-cast(coalesce(mn.movementqty,'0') as integer)) end as ac from c_orderline ol join c_order o on o.c_order_id = ol.c_order_id join c_doctype cd on cd.c_doctype_id=o.c_doctype_id full join m_inoutline mn on ol.c_orderline_id = mn.c_orderline_id  where  o.issotrx = 'N' and o.docstatus not in ('DR','VO','CL','OBWPL_SH','IBDO_SH')  and o.em_sw_isauto_order='N' and ol.ad_org_id = (select ad_org_id from ad_org where name = '"+storename+"') and ol.m_product_id = (select m_product_id from m_product where name='"+code+"') and cd.isreturn='N' group by  o.em_sw_po_reference) a;");
            rs = pst.executeQuery();
     		while (rs.next()) {
     			manualdc = rs.getLong("msdqty");
     		}
         } catch (Exception e) {
         	e.printStackTrace();
     	}
//        hql = "select sum(ol.swConfirmedqty) from Order o,OrderLine ol where o.createdBy <> '100' and ol.product.name='"+code+"'and o.organization.name='"+storename+"' and o.id = ol.salesOrder and (o.documentStatus = 'OBWPL_SH' or o.documentStatus= 'CO' or o.documentStatus='OBWPL_PK')";
//        query = OBDal.getInstance().getSession().createQuery(hql);
        
        //if(query != null && query.list().size() > 0 && query.list().get(0) != null){
        if(manualdc != null && manualdc > 0){	
        	sw.append("<manualdc>"+manualdc+"</manualdc>");  
        
        }else{
        	sw.append("<manualdc>0</manualdc>"); 
        } 
        pst = null;
        rs = null;
        
        // autodc from Supply
        Long autodc = 0L;
        try {
    		//pst = conn.prepareStatement("select coalesce(sum(qtyordered),0) - coalesce(sum(qtydelivered),0) as msdqty from c_orderline where m_product_id = (select m_product_id from m_product where name='"+code+"') and c_order_id in (select c_order_id from c_order where issotrx = 'Y' and createdby='100' and c_bpartner_id = (select c_bpartner_id from c_bpartner where name = '"+storename+"') and docstatus IN ('OBWPC_PK','CO','IBDO_PK'))");
        	//pst = conn.prepareStatement("select coalesce(sum(ac),0) as msdqty from (select case when o.em_sw_po_reference is null then sum(ol.qtyordered-cast(coalesce(ol.em_sw_recqty,'0') as integer)) else sum(ol.em_sw_confirmedqty-cast(coalesce(ol.em_sw_recqty,'0') as integer)) end as ac from c_orderline ol join c_order o on o.c_order_id = ol.c_order_id join c_doctype cd on cd.c_doctype_id=o.c_doctype_id where  o.issotrx = 'N' and o.docstatus not in ('DR','VO','CL','OBWPL_SH','IBDO_SH') and o.createdby != '100' and o.em_sw_isauto_order='Y' and ol.ad_org_id = (select ad_org_id from ad_org where name = '"+storename+"') and ol.m_product_id = (select m_product_id from m_product where name='"+code+"') and cd.isreturn='N' group by  o.em_sw_po_reference) a;");
            pst = conn.prepareStatement("select coalesce(sum(ac),0) as msdqty from (select case when o.em_sw_po_reference is null then sum(ol.qtyordered-cast(coalesce(ol.em_sw_recqty,'0') as integer)-cast(coalesce(mn.movementqty,'0') as integer)) else sum(ol.em_sw_confirmedqty-cast(coalesce(ol.em_sw_recqty,'0') as integer)-cast(coalesce(mn.movementqty,'0') as integer)) end as ac from c_orderline ol join c_order o on o.c_order_id = ol.c_order_id join c_doctype cd on cd.c_doctype_id=o.c_doctype_id full join m_inoutline mn on ol.c_orderline_id = mn.c_orderline_id  where  o.issotrx = 'N' and o.docstatus not in ('DR','VO','CL','OBWPL_SH','IBDO_SH')  and o.em_sw_isauto_order='Y' and ol.ad_org_id = (select ad_org_id from ad_org where name = '"+storename+"') and ol.m_product_id = (select m_product_id from m_product where name='"+code+"') and cd.isreturn='N' group by  o.em_sw_po_reference) a;");
    		rs = pst.executeQuery();
    		while (rs.next()) {
    			autodc = rs.getLong("msdqty");
    		}
        } catch (Exception e) {
        	e.printStackTrace();
    	}
//        hql = "select sum(ol.swConfirmedqty) from Order o,OrderLine ol where o.createdBy='100' and ol.product.name='"+code+"'and o.organization.name='"+storename+"' and o.id = ol.salesOrder and (o.documentStatus = 'OBWPL_SH' or o.documentStatus= 'CO' or o.documentStatus='OBWPL_PK')";
//        query = OBDal.getInstance().getSession().createQuery(hql);
        
		//if(query != null && query.list().size() > 0 && query.list().get(0) != null){
        if(autodc != null && autodc > 0){
		
	        	sw.append("<autodc>"+autodc+"</autodc>"); 
	
		}else{
			sw.append("<autodc>0</autodc>");  
		}

        hql = "select coalesce(sum(implantationQty),0) from CL_Implantation where storeImplanted.name='"+storename+"' and isimplanted = false and product.name='"+code+"'";
        query = OBDal.getInstance().getSession().createQuery(hql);
        sw.append("<stockallocated>"+query.list().get(0).toString()+"</stockallocated>"); 

         hql = "select mic.updated,mic.createdBy.name from MaterialMgmtInventoryCount mic,MaterialMgmtInventoryCountLine micl where mic.organization.name='"+storename+"' and mic.swMovementtype = 'PI' and micl.product.name='"+code+"' and mic.id = micl.physInventory order by mic.updated desc";
        query = OBDal.getInstance().getSession().createQuery(hql);
        query.setMaxResults(1);
        list = query.list(); 
          for (Object rows : list) 
          {
		Object[] row = (Object[]) rows; 
                //System.out.println(row[0].toString()+" "+row[1].toString());
		String date = sdf.format(myFormat.parse(row[0].toString().substring(0, 10)));
		String time = row[0].toString().substring(11, 16);
		String updatedPriceString = "Last Stock Update done on "+date+" at "+time+" by "+row[1].toString();
		sw.append("<updatestock>"+updatedPriceString+"</updatestock>"); 
          }
          
          // for on-road, 2 week sale and 4 week sale
          final Calendar cal = Calendar.getInstance();
      	
      	cal.setTime(new Date());
      	cal.add(Calendar.DAY_OF_MONTH, -28);
          long fromTimpstamp = cal.getTime().getTime();
          final long toTimpstamp = new Date().getTime();
          
          // On Road Quantity
          Object onRoadQty = "0";
         /* final OBQuery<ShipmentInOutLine> inoutLineObQuery = OBDal.getInstance().createQuery(ShipmentInOutLine.class, "as i where i.product.id=(select id from Product where name='"+code+"') and i.shipmentReceipt.id in (select id from ShipmentInOut where warehouse.id in (select id from Warehouse where name ilike 'In Transit%'))");
          inoutLineObQuery.setSelectClause("sum(i.movementQuantity)");
          if(null != inoutLineObQuery.list() && inoutLineObQuery.list().size() > 0) {
	          	onRoadQty = (Object)inoutLineObQuery.list().get(0);
	      		
	      	}*/
//          select COALESCE(sum(movementqty),0) from m_inoutline where m_product_id = '15501BEFE9A148C2939E1031F3E58258' 
//        	  and ad_org_id in (select ad_org_id from ad_org where name = 'IN1003') and 
//        	  m_locator_id in (select m_locator_id from m_locator where m_warehouse_id in 
//        			  (select m_warehouse_id from m_warehouse where name = 'In Transit IN1003'))
         /* final OBQuery<ShipmentInOutLine> inoutLineObQuery = OBDal.getInstance().createQuery(ShipmentInOutLine.class, "as i where i.product.id in (select id from Product where name='"+code+"') and i.organization.id='"+adOrgId+"' and i.storageBin.id in (select id form Locator where warehouse in (select id from Warehouse where name ilike 'In Transit "+storename+"')))");
          inoutLineObQuery.setSelectClause("sum(i.movementQuantity)");
          if(null != inoutLineObQuery.list() && inoutLineObQuery.list().size() > 0) {
	          	onRoadQty = (Object)inoutLineObQuery.list().get(0);
	      		
	      	}*/
          String onRoadQtySQL = "select COALESCE(sum(movementqty),0) from m_inoutline where m_product_id = (select m_product_id from m_product where name = '"+code+"') and ad_org_id ='"+adOrgId+"' and m_inout_id in (select m_inout_id from m_inout where docstatus = 'DR')";
          PreparedStatement statement = null;
          ResultSet result = null;
          try{
        	  conn = OBDal.getInstance().getConnection();
              statement = conn.prepareStatement(onRoadQtySQL);
              result = statement.executeQuery();
              if(result.next()){
            	  onRoadQty = result.getString(1);
              }
          }catch (Exception e) {
        	  e.printStackTrace();
          }finally{
        	  result.close();
              statement.close();
              conn.close();  
              pst.close();
              rs.close();
          }
         
          // Last 4 weeks sale
          Object invQty4weeks = "0";
          final OBQuery<InvoiceLine> invObQueryForlogs = OBDal.getInstance().createQuery(InvoiceLine.class, "as i where i.organization.id='"+adOrgId+"' and i.product.id=(select id from Product where name='"+code+"') and i.invoice.id in (select id from Invoice where documentStatus='CO' and salesTransaction=true and (invoiceDate between '"+new Timestamp(fromTimpstamp)+"' and '"+new Timestamp(toTimpstamp)+"'))");
          invObQueryForlogs.setSelectClause("sum(i.invoicedQuantity)");
          invObQueryForlogs.setFilterOnReadableOrganization(false);
	      	if(null != invObQueryForlogs.list() && invObQueryForlogs.list().size() > 0) {
	      		invQty4weeks = (Object)invObQueryForlogs.list().get(0);
	      		if(null == invQty4weeks){
	      			invQty4weeks =  "0";
	      		}
	      	}
          
          // Last 2 weeks sale
      	cal.setTime(new Date());
      	cal.add(Calendar.DAY_OF_MONTH, -14);
      	fromTimpstamp = cal.getTime().getTime();
      	
      	Object invQty2weeks = "0";
      	final OBQuery<InvoiceLine> invObQuery = OBDal.getInstance().createQuery(InvoiceLine.class, "as i where i.organization.id='"+adOrgId+"' and i.product.id=(select id from Product where name='"+code+"') and i.invoice.id in (select id from Invoice where documentStatus='CO' and salesTransaction=true and (invoiceDate between '"+new Timestamp(fromTimpstamp)+"' and '"+new Timestamp(toTimpstamp)+"'))");
      	invObQuery.setSelectClause("sum(i.invoicedQuantity)");
      	invObQuery.setFilterOnReadableOrganization(false);
      	if(null != invObQuery.list() && invObQuery.list().size() > 0) {
      		invQty2weeks = (Object)invObQuery.list().get(0);
      		if(null == invQty2weeks){
      			invQty2weeks = "0";
      		}
      	}
      	
      	//Recommended Price
      	String recommendedValue = "";
      	String recomLotValue = "";
      	String recomBoxValue = "";
      	String recomUnitValue = "";
      	String recommendedPriceSQL = "select em_cl_cessionprice, em_cl_ccueprice, em_cl_ccpcbprice, em_cl_ccunitprice from m_productprice where m_product_id = (select m_product_id from m_product where name = '"+code+"') and ad_org_id = '0'";
      	try{
      		 conn = OBDal.getInstance().getConnection();
             statement = conn.prepareStatement(recommendedPriceSQL);
             result = statement.executeQuery();
             if(result.next()){
            	 recommendedValue = result.getString(1);
            	 recomLotValue = result.getString(2);
            	 recomBoxValue = result.getString(3);
            	 recomUnitValue = result.getString(4);
             }
      	} catch(Exception e) {
      		e.printStackTrace();
      	}
      	
      	log.info("fourweeksale is "+invQty4weeks);
      	log.info("twoweeksale is "+invQty2weeks);
      	sw.append("<onroadqty>"+onRoadQty.toString()+"</onroadqty>");
        sw.append("<fourweeksale>"+invQty4weeks.toString()+"</fourweeksale>");
        sw.append("<twoweeksale>"+invQty2weeks.toString()+"</twoweeksale>"); 
        sw.append("<recommendedprice>"+recommendedValue.toString()+"</recommendedprice>");
        sw.append("<recom_lot_price>"+recomLotValue.toString()+"</recom_lot_price>");
        sw.append("<recom_box_price>"+recomBoxValue.toString()+"</recom_box_price>");
        sw.append("<recom_unit_price>"+recomUnitValue.toString()+"</recom_unit_price>");
     
}    catch(Exception e)
    {
      e.printStackTrace();
      log.error("Error occured due to: "+e.getMessage());
      String message = "Error in getting the store stock";
      sw.append("<message>"+message+"</message>"); 
    }
     sw.append("</items>");
    //sw.append("<message>"+message+"</message>");
    // and get the result
    final String xml = sw.toString().replaceAll("&", "&amp;");   
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
