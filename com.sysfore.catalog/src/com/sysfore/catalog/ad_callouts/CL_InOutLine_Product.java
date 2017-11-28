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
 * All portions are Copyright (C) 2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.catalog.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SL_InOutLine_Product;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class CL_InOutLine_Product extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    // first call the original callout
    info.executeCallout(new CL_InOutLine_Product());

    // add the information here.
    String strMProductID = info.vars.getStringParameter("inpmProductId");

    CLInventoryLineCMNSData[] data = CLInventoryLineCMNSData.select(this, strMProductID);
    if (data != null && data.length > 0) {
      info.addResult("inpemSwModelId", data[0].emClModelId);
      info.addResult("inpemSwSize", data[0].emClSize);
      info.addResult("inpemSwColorId", data[0].emClColorId);
    }
  }
}
