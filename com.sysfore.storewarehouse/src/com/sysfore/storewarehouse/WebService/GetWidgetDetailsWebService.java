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
package com.sysfore.storewarehouse.WebService;
 
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import java.util.Iterator;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.db.CallStoredProcedure;

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
		
		sw.append("<unitmargin>"+Double.parseDouble(row[9].toString())+"</unitmargin>");
		sw.append("<uemargin>"+Double.parseDouble(row[10].toString())+"</uemargin>");
		sw.append("<pcbmargin>"+Double.parseDouble(row[11].toString())+"</pcbmargin>");

		sw.append("<unitmarginamount>"+Double.parseDouble(row[12].toString())+"</unitmarginamount>");
		sw.append("<uemarginamount>"+Double.parseDouble(row[13].toString())+"</uemarginamount>");
		sw.append("<pcbmarginamount>"+Double.parseDouble(row[14].toString())+"</pcbmarginamount>");

                
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
        	log.info("taxCategoryId ="+taxCategoryId);
	        	
	      
	        hql = "select org.id from Organization org where org.name = '"+storename+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List storeIdList = query.list();
        	adOrgId = storeIdList.get(0).toString();
        	log.info("adOrgId ="+adOrgId);
	        	
	      
	        hql = 	" select l.region.id from OrganizationInformation oi, Location l where oi.id='"+adOrgId+"' " +
	        		" and oi.locationAddress=l.id";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List regionIdList = query.list();
        	regionId = regionIdList.get(0).toString();
        	log.info("regionId ="+regionId);
	        	
	        
	        hql = "select ct.rate from FinancialMgmtTaxRate ct where ct.region.id = '"+regionId+"' and ct.taxCategory.id ='"+taxCategoryId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List rateList1 = query.list();
	        if(rateList1.size() > 0){
	        	taxRate1 = Double.parseDouble(rateList1.get(0).toString());
	        }
	        log.info("taxRate1 ="+taxRate1);
	        	
	        
	        hql = 	" select ct.rate from FinancialMgmtTaxRate ct ,FinancialMgmtTaxZone ctz where ct.id = ctz.id " +
	        		" and ctz.fromRegion.id = '"+regionId+"' and ct.taxCategory.id ='"+taxCategoryId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List rateList2 = query.list();
	        if(rateList2.size() > 0){
	        	taxRate2 = Double.parseDouble(rateList2.get(0).toString());
	        }
	        log.info("taxRate2 ="+taxRate2);
	        
	        if(taxRate2 == 0.00){
	        	taxRate = taxRate1;
	        }else if(taxRate1 == 0.00){
	        	taxRate = taxRate2;
	        }else{
	        	taxRate = 0.00;
	        }
	        
	        log.info("Tax Rate="+taxRate);
	        sw.append("<taxrate>"+taxRate+"</taxrate>");
	        
	        
	        
	        hql = 	" select c.searchKey from Organization ad, OrganizationInformation ai, Location l, Region c " +
	        		" where ad.id = ai.id and ai.locationAddress.id = l.id " +
	        		" and l.region.id = c.id and ad.id = '"+adOrgId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List regionValueList = query.list();
	        regionName = regionValueList.get(0).toString();
	        log.info("regionName ="+regionName);
	        sw.append("<regionName>"+regionName+"</regionName>");
	        
	        
	        hql = 	" select p.clModel.id from Product p where p.name ='"+code+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List modelList = query.list();
        	modelId = modelList.get(0).toString();
        	log.info("modelId ="+modelId);
	        
	        hql = 	" select cm.octroi from CL_Model cm where cm.id ='"+modelId+"'";
	        query = OBDal.getInstance().getSession().createQuery(hql);
	        query.setMaxResults(1);
	        List octroiList = query.list();
		    octroi = Double.parseDouble(octroiList.get(0).toString());
		    
		    log.info("octroi ="+octroi);
	        sw.append("<octroi>"+octroi+"</octroi>");
	        
        
        
        
        
        
      hql = "select clModel.brand.name,clModel.department.name,clColor.name,clGender,clSize from Product where name='"+code+"' order by creationDate desc";
        query = OBDal.getInstance().getSession().createQuery(hql);
        query.setMaxResults(1);
        list = query.list();
        for (Object rows : list) 
       {


           Object[] row = (Object[]) rows;



           sw.append("<brand>"+row[0].toString()+"</brand>");
           sw.append("<family>"+row[1].toString()+"</family>");
           sw.append("<color>"+row[2].toString()+"</color>");
           hql = "select name from ADList where searchKey='"+row[3].toString()+"' and reference.name='GenderCat'";



           query = OBDal.getInstance().getSession().createQuery(hql);
           sw.append("<gender>"+query.list().get(0).toString()+"</gender>");
           sw.append("<size>"+row[4].toString()+"</size>");
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
     
        hql = "select sum(ol.swConfirmedqty) from Order o,OrderLine ol where o.createdBy <> '100' and ol.product.name='"+code+"'and o.organization.name='"+storename+"' and o.id = ol.salesOrder and (o.documentStatus = 'OBWPL_SH' or o.documentStatus= 'CO')";
        query = OBDal.getInstance().getSession().createQuery(hql);
        
        if(query != null && query.list().size() > 0 && query.list().get(0) != null){
        	
        	sw.append("<manualdc>"+query.list().get(0).toString()+"</manualdc>");  
        
        }else{
        	sw.append("<manualdc>0</manualdc>"); 
        }
        

        hql = "select sum(ol.swConfirmedqty) from Order o,OrderLine ol where o.createdBy='100' and ol.product.name='"+code+"'and o.organization.name='"+storename+"' and o.id = ol.salesOrder and (o.documentStatus = 'OBWPL_SH' or o.documentStatus= 'CO')";
        query = OBDal.getInstance().getSession().createQuery(hql);
        
		if(query != null && query.list().size() > 0 && query.list().get(0) != null){
		
	        	sw.append("<autodc>"+query.list().get(0).toString()+"</autodc>"); 
	
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

	/*try{

System.out.println("code=="+code);
		hql = "select ct.rate from FinancialMgmtTaxRate ct, Product mp where ct.taxCategory.id = mp.taxCategory.id and mp.name = '"+code+"'";
      		query = OBDal.getInstance().getSession().createQuery(hql);
		sw.append("<taxrate>"+query.list().get(0).toString()+"</taxrate>"); 
	System.out.println("Tax Rate==="+query.list().get(0).toString());

	}catch(Exception e){
		log.error("Error occured while getting Tax Rate: "+e.getMessage());
		String message = "Error in getting the Tax Rate";
      		sw.append("<message>"+message+"</message>"); 
	}	*/
           
    }
    catch(Exception e)
    {
      e.printStackTrace();
      log.error("Error occured due to: "+e.getMessage());
      String message = "Error in getting the store stock";
      sw.append("<message>"+message+"</message>"); 
    }
     sw.append("</items>");
    //sw.append("<message>"+message+"</message>");
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
