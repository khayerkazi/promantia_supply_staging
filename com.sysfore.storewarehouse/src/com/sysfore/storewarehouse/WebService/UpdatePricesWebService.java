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
public class UpdatePricesWebService implements WebService {
 
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(UpdatePricesWebService.class);
 
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // do some checking of parameters
    StringWriter sw = new StringWriter();
    Boolean updateAllVal = false;
     String hql = "";
     Query query = null;
     String message=null; 
     String userid = null; 
    String code = request.getParameter("code");
    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
     String email = request.getParameter("email");
    if (email == null) {
      throw new IllegalArgumentException("The email parameter is mandatory");
    }      
    String storename = request.getParameter("storename");
    if (storename == null) {
      throw new IllegalArgumentException("The storename parameter is mandatory");
    }
    String displaymin = request.getParameter("displaymin");
  
    if(displaymin == null)
    {
	    String updateAll = request.getParameter("updateAll");
	    if (updateAll == null) {
	      throw new IllegalArgumentException("The updateAll parameter is mandatory");
	    }
	    else
	    {
		 updateAllVal = Boolean.parseBoolean(updateAll);
	    }
	    String unitprice = request.getParameter("unitprice");
	    if (unitprice == null) {
	      throw new IllegalArgumentException("The unitprice parameter is mandatory");
	    }
	    String ueprice = request.getParameter("ueprice");
	    if (ueprice == null) {
	      throw new IllegalArgumentException("The ueprice parameter is mandatory");
	    }
	    String pcbprice = request.getParameter("pcbprice");
	    if (pcbprice == null) {
	      throw new IllegalArgumentException("The pcbprice parameter is mandatory");
	    } 
	    String strqty = request.getParameter("strqty");
	    if (strqty == null) {
	      throw new IllegalArgumentException("The strqty parameter is mandatory");
	    }  
	    String boxqty = request.getParameter("boxqty");
	    if (boxqty == null) {
	      throw new IllegalArgumentException("The boxqty parameter is mandatory");
	    } 
	    // Added for new Margin fields
	    String unitmargin = request.getParameter("unitmargin");
	    if (unitmargin == null) {
	      throw new IllegalArgumentException("The unit margin parameter is mandatory");
	    } 

 	    String uemargin = request.getParameter("uemargin");
	    if (uemargin == null) {
	      throw new IllegalArgumentException("The ue margin parameter is mandatory");
	    } 

  	    String pcbmargin = request.getParameter("pcbmargin");
	    if (pcbmargin == null) {
	      throw new IllegalArgumentException("The pcb margin parameter is mandatory");
	    } 
	    
	   
	    //System.out.println("clUnitmarginamount="+unitmargin);

	    try
	    {
	       hql="select COALESCE(id,'NA') from ADUser where email='"+email+"'";
	       query = OBDal.getInstance().getSession().createQuery(hql);
	       userid = query.list().get(0).toString(); 
	       if(userid.equals("NA"))
	       {
		  message  = "Error in getting the store stock: User has no email";
	       }
	       else
	       {
		   query = null;
		   if(updateAllVal)
		   {
		      hql="select name from Product where clModelcode=(select clModelcode from Product where name='"+code+"')  order by name desc";
		        query = OBDal.getInstance().getSession().createQuery(hql); 
		      List list = query.list();
		      for (Object rows : list) 
		     {
		        hql="select id from PricingProductPrice where product.name='"+rows.toString()+"' and organization.name='"+storename+"' order by creationDate desc";
		        query = OBDal.getInstance().getSession().createQuery(hql);
		        query.setMaxResults(1);
		        String productid = query.list().get(0).toString(); 
		        hql="update PricingProductPrice " +
			 "set clCcunitprice="+unitprice+", " +
			 "clCcueprice="+ueprice+", "+
			 "clCcpcbprice="+pcbprice+", "+
			 "cLEMClSuqty="+strqty+", "+
			 "cLEMClSboxqty="+boxqty+", "+
		         "updatedby='"+userid+"', "+
			"clUnitmarginpercentage="+unitmargin+", "+
			"clUemarginpercentage="+uemargin+", "+
			"clPcbmarginpercentage="+pcbmargin+", "+
		         "updated=now(),"+ 
			"clFollowcatalog='N'"+ 
			 "where id='"+productid+"'";
		        query = OBDal.getInstance().getSession().createQuery(hql);
		        int result = query.executeUpdate();
		        if(result == 0)
		        {
		           updateAllVal = false;
		        } 
		      }

		      if(updateAllVal)
		      {
		         sw.append("<message>Success in updating the prices</message>");
		      }
		      else
		      {
		        sw.append("<message>Error in updating the prices</message>");     
		      } 

		    }
		   if(query == null)
		   {
                      hql="select id from PricingProductPrice where product.name='"+code+"' and organization.name='"+storename+"' order by creationDate desc";
		        query = OBDal.getInstance().getSession().createQuery(hql);
		        query.setMaxResults(1);
		        String productid = query.list().get(0).toString(); 
		      hql="update PricingProductPrice " +
			 "set clCcunitprice="+unitprice+", " +
			 "clCcueprice="+ueprice+", "+
			 "clCcpcbprice="+pcbprice+", "+
			 "cLEMClSuqty="+strqty+", "+
			 "cLEMClSboxqty="+boxqty+", "+
		         "updatedby='"+userid+"', "+
			"clUnitmarginpercentage="+unitmargin+", "+
			"clUemarginpercentage="+uemargin+", "+
			"clPcbmarginpercentage="+pcbmargin+", "+
		         "updated=now(),"+ 
			"clFollowcatalog='N'"+ 
			 "where id='"+productid+"'";
		     query = OBDal.getInstance().getSession().createQuery(hql);
		     int result = query.executeUpdate();
		     if(result > 0)
		     {
		        sw.append("<message>Success in updating the prices</message>");
		     } 
		   }
	       }
	    }
	    catch(Exception e)
	    {
	      //e.printStackTrace();
	      log.error("Error occured due to: "+e.getMessage());
	      message = "Error in updating the prices";
	      sw.append("<message>"+message+"</message>"); 
	    }
   }
   else
   {
          try
	    {
	       hql="select COALESCE(id,'NA') from ADUser where email='"+email+"'";
	       query = OBDal.getInstance().getSession().createQuery(hql);
	       userid = query.list().get(0).toString(); 
	       if(userid.equals("NA"))
	       {
		  message  = "Error in getting the store stock: User has no email";
	       }
	       else
	       {
                     hql="select id from CL_Minmax where product.name='"+code+"' and organization.name = '"+storename+"' order by creationDate desc";
		     query = OBDal.getInstance().getSession().createQuery(hql);
		     query.setMaxResults(1);
		     String productid = query.list().get(0).toString(); 
		     hql="update CL_Minmax " +
			 "set displaymin="+displaymin+", " +
		         "updatedby='"+userid+"', "+
		         "updated=now()"+ 
			 "where id='"+productid+"'";
		     query = OBDal.getInstance().getSession().createQuery(hql);
		     int result = query.executeUpdate();
		     if(result > 0)
		     {
		        sw.append("<message>Success in updating the displaymin</message>");
		     }      
	       }
            }
	    catch(Exception e)
	    {
	      //e.printStackTrace();
	      log.error("Error occured due to: "+e.getMessage());
	      message = "Error in updating the prices";
	      sw.append("<message>"+message+"</message>"); 
	    }
 
   }
 
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
