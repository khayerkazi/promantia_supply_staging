/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.catalog.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class CL_Inventory_Locator extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strProduct = info.vars.getStringParameter("inpmProductId");
    String strLocator = info.vars.getStringParameter("inpmLocatorId");

    if (strProduct.startsWith("\"")) {
      strProduct = strProduct.substring(1, strProduct.length() - 1);
    }

    if (!strProduct.equals("")) {

       CLInventoryLocatorData[] data = CLInventoryLocatorData.select(this, strProduct, strLocator);
	         System.out.println(" data mai hu ");
      if (data == null || data.length == 0) {
        data = CLInventoryLocatorData.set();
        data[0].qty = "0";
        data[0].qtyorder = "0";
      }

      info.addResult("inpquantityorderbook",
          data[0].qtyorder == null || data[0].qtyorder.equals("") ? "\"\"" : data[0].qtyorder);
      info.addResult("inpqtycount", data[0].qty == null || data[0].qty.equals("") ? "\"\""
          : data[0].qty);
      info.addResult("inpqtybook", data[0].qty == null || data[0].qty.equals("") ? "\"\""
          : data[0].qty);

      info.addResult("EXECUTE", "displayLogic();");
	 CLInventoryLocatorMscData[] data1 = CLInventoryLocatorMscData.selectmsc(this, strProduct);	
	if (data1 == null || data1.length == 0) {
      //  data1 = CLInventoryLocatorData.set();
      }else{
	 StringBuffer resultado = new StringBuffer();
		 resultado.append("var calloutName='CL_Inventory_Product';\n\n");
		    resultado.append("var respuesta = new Array(");

      	  if (data1 != null && data1.length > 0)
      	  {
	info.addResult("inpemSwModelId", data1[0].emClModelId == null || data1[0].emClModelId.equals("") ? "\"\"" : data1[0].emClModelId);
	info.addResult("inpemSwSize", data1[0].emClSize == null || data1[0].emClSize.equals("") ? "\"\"" : data1[0].emClSize);
	info.addResult("inpemSwColorId", data1[0].emClColorId == null || data1[0].emClColorId.equals("") ? "\"\"" : data1[0].emClColorId);
	info.addResult("inpcUomId", data1[0].cUomId == null || data1[0].cUomId.equals("") ? "\"\"" : data1[0].cUomId);


      	  }
		}
    }

  }
  	
}
