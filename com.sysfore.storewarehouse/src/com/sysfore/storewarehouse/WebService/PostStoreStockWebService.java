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
public class PostStoreStockWebService implements WebService {
 
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(PostStoreStockWebService.class);
 
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // do some checking of parameters
    StringWriter sw = new StringWriter();
    String code = request.getParameter("code");
    String storeStock = request.getParameter("quantity");
    String storename = request.getParameter("storename");
    String email = request.getParameter("email");  



    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
    if (storename == null) {
      throw new IllegalArgumentException("The storename parameter is mandatory");
    }
    if (storeStock == null) {
      throw new IllegalArgumentException("The stock parameter is mandatory");
    } 
    if (email == null) {
      throw new IllegalArgumentException("The email parameter is mandatory");
    } 
    String[] itemCodes = code.split(",");
    String[] stock = storeStock.split(",");   
    String hql = "";
    String message="Success";
    //System.out.println(storename+code+email+storeStock);
    try
    {
	    for(int i=0;i<stock.length;i++)
	    {
	      List<Object> param = new ArrayList<Object>();
	      param.add(itemCodes[i]);
	      param.add(Integer.parseInt(stock[i]));
	      param.add(storename);
	      param.add(email);
	      CallStoredProcedure.getInstance().call("sw_post_inventory", param, null, true, false);
	    }
    }
    catch(Exception e)
    {
      //e.printStackTrace();
      log.error("Error occured due to: "+e.getMessage());
      message = "Error in updating the store stock";
    }
    sw.append("<message>"+message+"</message>");
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
