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
import org.openbravo.model.common.order.OrderLine;

import com.sysfore.decathlonimport.ImportEDD;

public class ValidateImportEDD extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String process = "ValidateImportEDD";
    if (vars.commandIn("DEFAULT")) {
      // System.out.println("Inside the ValidateImportEDD");
      String strTabId = vars.getGlobalVariable("inpTabId", "ValidateImportEDD|tabId");
      String strWindowId = vars.getGlobalVariable("inpwindowId", "ValidateImportEDD|windowId");
      String strDeleteOld = vars.getStringParameter("inpDeleteOld", "Y");
      String strImportEDDID = vars.getStringParameter("inpimImportEddId");
      String strWindowPath = Utility.getTabURL(this, strTabId, "R");
      String strModelCode = vars.getStringParameter("inpmodelcode");
      String strOrderRefeNo = vars.getStringParameter("inporderRefno");
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;
      // coding starts
      boolean itemCodeExists = false;
      String errorMsg = "";
      final ImportEDD importEddObj = OBDal.getInstance().get(ImportEDD.class, strImportEDDID);
      String orderRefNo = importEddObj.getOrderRefno(), modelCode = importEddObj.getModelCode(), comments = importEddObj
          .getComments();
 final OBError msg = new OBError();
      if (strOrderRefeNo == null || strOrderRefeNo.isEmpty() || importEddObj.getOrderRefno() == null) {
        errorMsg = "Order Reference No should not be blank";
      }
      if (strModelCode == null || strModelCode.isEmpty() || importEddObj.getModelCode() == null) {
        errorMsg = "Model Code should not be blank";
      }
      if ((strModelCode == null || strModelCode.isEmpty() || importEddObj.getModelCode() == null)
          && (strOrderRefeNo == null || strOrderRefeNo.isEmpty() || importEddObj.getOrderRefno() == null)) {
        errorMsg = "Order Reference No. and Model Code should not be blank";
      } 
if (importEddObj.getEdd() == null) {
        errorMsg = "EDD should not be blank";
      }
if ((strModelCode == null || strModelCode.isEmpty() || importEddObj.getModelCode() == null)
          && (strOrderRefeNo == null || strOrderRefeNo.isEmpty() || importEddObj.getOrderRefno() == null) && importEddObj.getEdd() == null){
        errorMsg = "Order Reference No. , Model Code and EDD should not be blank";
      }

if(importEddObj.getModelCode() != null && importEddObj.getOrderRefno() != null && importEddObj.getEdd() != null){
      final OBCriteria<Order> obCriteria = OBDal.getInstance().createCriteria(Order.class);
      obCriteria.add(Expression.eq(Order.PROPERTY_ORDERREFERENCE, orderRefNo));
      obCriteria.add(Expression.eq(Order.PROPERTY_SWEMSWMODELCODE, modelCode));

      final List<Order> oList = obCriteria.list();
      if (oList.size() <= 0) {
        errorMsg = "Invalid Order Reference No or/and Model Code";
      } else if (importEddObj.getItemCode() != null) {// item code check if not blank
        Order po = oList.get(0);

        for (OrderLine ol : po.getOrderLineList()) {
          if (ol.getProduct().getName().equals(importEddObj.getItemCode())) {
            itemCodeExists = true;
          }
        }
        if (!itemCodeExists) {
          errorMsg = "Item Code is not present for Order with document No. - " + po.getDocumentNo();
        }
      }// end of item code check
}
      // OBError is also used for successful results
       if (errorMsg.equals("")) {
        importEddObj.setErrorMsg("");
        importEddObj.setValidated(true);
        OBDal.getInstance().save(importEddObj);
        msg.setType("Success");
        msg.setTitle("Success");
        msg.setMessage("EDD Validated Successfully");

      } else {
        importEddObj.setErrorMsg(errorMsg);
        OBDal.getInstance().save(importEddObj);
        msg.setType("Error");
        msg.setTitle("Error occurred");
        msg.setMessage("Validation Failed!");
      }
      vars.setMessage(strTabId, msg);
      printPageClosePopUp(response, vars, strWindowPath);
    } else
      pageErrorPopUp(response);
  }

  public String getServletInfo() {
    return "Servlet ValidateImportEDD";
  } // end of getServletInfo() method
}
