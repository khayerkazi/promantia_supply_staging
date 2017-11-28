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
 * All portions are Copyright (C) 2011-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sysfore.catalog.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SE_Order_BPartner;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class CE_Order_BPartner extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    // first call the original callout
    info.executeCallout(new SE_Order_BPartner());

    // add the information here.

    CLOrderCurrencyData[] data = CLOrderCurrencyData.select(this,
        info.vars.getStringParameter("inpcBpartnerId"));
    String strPoCurrency = data[0].emClCurrency.equals("") ? info.vars
        .getStringParameter("inpemSwCurrency") : data[0].emClCurrency;

    info.addResult("inpemSwCurrency", strPoCurrency);
  }

}
