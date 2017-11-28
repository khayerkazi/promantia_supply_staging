/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2009 Openbravo S.L.U.
 ******************************************************************************
 */
package com.sysfore.decathlonimport.ad_process;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Expression;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.order.Order;

import com.sysfore.decathlonimport.ImportEDD;

public class ImportEDDProcess extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String process = "ImportEDDProcess";
    if (vars.commandIn("DEFAULT")) {
      System.out.println("Inside the Default ImportEDDProcess");
      String strTabId = vars.getGlobalVariable("inpTabId", "ImportEDDProcess|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId", "ImportEDDProcess|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      String strImportEDDID = vars.getStringParameter("inpimImportEddId");
      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;
      // coding starts
      final OBError msg = new OBError();
      final ImportEDD importEddObj = OBDal.getInstance().get(ImportEDD.class, strImportEDDID);
      String orderRefNo = importEddObj.getOrderRefno(), itemCode = importEddObj.getItemCode(), modelCode = importEddObj
          .getModelCode(), comments = importEddObj.getComments();
      // check whether validated or not

      if (importEddObj.isValidated()) {

        // Order po = (Order) OBDal.getInstance().get(Order.PROPERTY_ORDERREFERENCE, orderRefNo);
        final OBCriteria<Order> obCriteria = OBDal.getInstance().createCriteria(Order.class);
        obCriteria.add(Expression.eq(Order.PROPERTY_ORDERREFERENCE, orderRefNo));
        obCriteria.add(Expression.eq(Order.PROPERTY_SWEMSWMODELCODE, modelCode));
        final List<Order> oList = obCriteria.list();

        // update PO
        Order po = oList.get(0);
        po.setSWEMSwEstshipdate(importEddObj.getEdd());
        po.setImComments(comments);
        OBDal.getInstance().save(po);

        // Update Import EDD
        importEddObj.setProcessed(true);
        OBDal.getInstance().save(importEddObj);

        // OBError is also used for successful results
        msg.setType("Success");
        msg.setTitle("Success");
        msg.setMessage("EDD Updated Successfully");
      } else {
        msg.setType("Error");
        msg.setTitle("Error occurred");
        msg.setMessage("Please Validate before Importing EDD");
      }
      vars.setMessage(strTabId, msg);
      printPageClosePopUp(response, vars, strWindowPath);
    } else
      pageErrorPopUp(response);
  }

  public String getServletInfo() {
    return "Servlet ImportEDDProcess";
  } // end of getServletInfo() method
}
