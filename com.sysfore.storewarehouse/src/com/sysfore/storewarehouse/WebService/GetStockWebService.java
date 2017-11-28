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
 
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

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
    String hql = " SELECT COALESCE(sum(quantityOnHand),0) as cacqty from MaterialMgmtStorageDetail where storageBin.warehouse.swIscac = true and product.name = '"+code+"' and attributeSetValue = '0'";
    Query query = OBDal.getInstance().getSession().createQuery(hql);
    String cacqty = query.list().get(0).toString();    
    hql = "select COALESCE((sum(quantity)-sum(released)),0) as reserved from MaterialMgmtReservationStock where  storageBin.warehouse.swIscac = true and reservation.product.name = '"+code+"'";
    query = OBDal.getInstance().getSession().createQuery(hql);
    String reservedcac = "0";
    List list = query.list();
    System.out.println(list);  
    if(list.size() > 0) 
    {
         reservedcac = list.get(0).toString();
    }

    Double cacActualQty = Double.parseDouble(cacqty)-Double.parseDouble(reservedcac);  


    hql = " SELECT COALESCE(sum(quantityOnHand),0) as cacqty from MaterialMgmtStorageDetail where storageBin.warehouse.swIscar = true and product.name = '"+code+"' and attributeSetValue <> '0'";
    query = OBDal.getInstance().getSession().createQuery(hql);
    String carQty = query.list().get(0).toString();    
    hql = "select COALESCE((sum(quantity)-sum(released)),0) as reserved from MaterialMgmtReservationStock where  storageBin.warehouse.swIscar = true and reservation.product.name = '"+code+"'";
    query = OBDal.getInstance().getSession().createQuery(hql);
    String reservedCar = "0";
    list = query.list(); 
    if(list.size() > 0) 
    {
         reservedCar = list.get(0).toString();
    }
    

    Double carActualQty = Double.parseDouble(carQty) - Double.parseDouble(reservedCar); 

    hql = " SELECT COALESCE(sum(quantityOnHand),0) as cacqty from MaterialMgmtStorageDetail where storageBin.warehouse.name like 'I%Transit%Warehouse' and product.name = '"+code+"'";
    query = OBDal.getInstance().getSession().createQuery(hql);
    String itrQty = query.list().get(0).toString();  


   hql =" SELECT COALESCE(sum(quantityOnHand),0) as storeqty from MaterialMgmtStorageDetail where storageBin.warehouse.name like 'Saleable%' and storageBin.organization.name = '"+storename+"'  and product.name = '"+code+"' and attributeSetValue = '0'";
    query = OBDal.getInstance().getSession().createQuery(hql);
    String storeQty = query.list().get(0).toString();  


   sw.append("<items>");
   sw.append("<cacstock>"+cacActualQty.intValue()+"</cacstock>");
   sw.append("<carstock>"+carActualQty.intValue()+"</carstock>");
   sw.append("<itrqty>"+itrQty+"</itrqty>");
   sw.append("<storeqty>"+storeQty+"</storeqty>");
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
