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
import java.util.Calendar;
import java.sql.Date;
 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import java.net.URLDecoder;

import org.openbravo.service.web.WebService;
 
/**
 * Get the count of rows
 * and the type of search
 * @author shreyas
 */
public class GetCountOfProductsWebService implements WebService {
 
  private static final long serialVersionUID = 1L;
 
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // do some checking of parameters
    StringWriter sw = new StringWriter();
    String code = URLDecoder.decode(request.getParameter("code").toString());
    String storename = request.getParameter("storename");
    String selected = request.getParameter("selected");
    String hql = null; 
    Query query = null;
    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
    if(storename == null && selected == null)
    { 
	    hql = " SELECT count(*) as count from Product where clModelcode = (select clModelcode from Product where name = '"+code+"')";
	    query = OBDal.getInstance().getSession().createQuery(hql);  
	    String count = query.list().get(0).toString(); 
	    Integer countNo = Integer.parseInt(count);
	    sw.append("<?xml version='1.0'?>");
	    if(countNo > 0)
	    {
	       sw.append("<items>");
	       sw.append("<count>"+count+"</count>");
	       sw.append("<method>itemcode</method>");
	       sw.append("</items>");
	    }
	    else
	    {
	       hql = " SELECT count(*) as count from Product where clModelcode = '"+code+"'";
	       query = OBDal.getInstance().getSession().createQuery(hql);  
	       count = query.list().get(0).toString(); 
	       countNo = Integer.parseInt(count);
	       if(countNo > 0)
	       {
		 sw.append("<items>");
		 sw.append("<count>"+count+"</count>");
		 sw.append("<method>modelcode</method>");
		 sw.append("</items>");
	       }
	       else
	      {
		code = code.toLowerCase();

System.out.println(code);

		hql = " SELECT count(*) as count from Product where lower(clModelname) like '%"+code+"%'";
		query = OBDal.getInstance().getSession().createQuery(hql);  
		count = query.list().get(0).toString(); 
		countNo = Integer.parseInt(count);
		if(countNo > 0)
		{
		 sw.append("<items>");
		 sw.append("<count>"+count+"</count>");
		 sw.append("<method>modelname</method>");
		 sw.append("</items>");
		}
		else
		{
		     if(code.contains(" "))
		     { 
			     code = code.toLowerCase();
			     int index = code.lastIndexOf(" ");
			     String merchandise = code.substring(0,index);
			     String color = code.substring(index+1,code.length());
			     hql = " SELECT count(*) as count from Product where lower(clColor.name) like '"+color+"' and lower(clModel.merchandiseCategory) like '"+merchandise+"'";
			     query = OBDal.getInstance().getSession().createQuery(hql);  
			     count = query.list().get(0).toString(); 
			     countNo = Integer.parseInt(count);
			     if(countNo > 0)
			       {
				  sw.append("<items>");
				  sw.append("<count>"+count+"</count>");
				  sw.append("<method>nature</method>");
				  sw.append("</items>");
			       }
			     else
			     {
				  sw.append("<items>");
				  sw.append("<count>0</count>");
				  sw.append("<method>None</method>");
				  sw.append("</items>");
			     }
		     }
		     else
			     {
				  sw.append("<items>");
				  sw.append("<count>0</count>");
				  sw.append("<method>None</method>");
				  sw.append("</items>");
			     }
		}
	      }
	    }
      }
      else if(selected == null)
      {
           hql = " SELECT count(*) as count from CL_PriceHistory where product.name = '"+code+"' and organization.name='"+storename+"'";
	    query = OBDal.getInstance().getSession().createQuery(hql);  
	    String count = query.list().get(0).toString(); 
	    Integer countNo = Integer.parseInt(count);
            sw.append("<items>");
	    sw.append("<count>"+count+"</count>");
	    sw.append("</items>");
      }
      else
      {
         Calendar calendar = Calendar.getInstance();
	 calendar.add(Calendar.YEAR, -1);
	 Date date = new Date(calendar.getTime().getTime());  
         if(!selected.equals("all"))
         {
         	hql = " SELECT count(*) as count from MaterialMgmtMaterialTransaction where product.name = '"+code+"' and 	organization.name='"+storename+"' and swMovementtype='"+selected+"' and updated > '"+date+"'";
         }
         else
         {
            hql = " SELECT count(*) as count from MaterialMgmtMaterialTransaction where product.name = '"+code+"' and 	organization.name='"+storename+"' and updated > '"+date+"'";
         } 
	 query = OBDal.getInstance().getSession().createQuery(hql);  
	 String count = query.list().get(0).toString(); 
	    Integer countNo = Integer.parseInt(count);
            sw.append("<items>");
	    sw.append("<count>"+count+"</count>");
	    sw.append("</items>");
       

      }
     
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
