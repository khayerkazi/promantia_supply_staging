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
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.pricing.levelpricing.callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.pricing.levelpricing.LevelPricingRange;
import org.openbravo.pricing.levelpricing.LevelProductPrice;

/**
 * This class adds the default qty to the field qty based on the range qty.
 * 
 * @author guilleaer
 * 
 */
public class DefaultQtySetByRange extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String currentRecordId = info.getStringParameter("Lvlpr_Levelproductprice_ID", null);
    String rangeId = info.getStringParameter("inplvlprRangeId", null);
    LevelPricingRange currentRange = OBDal.getInstance().get(LevelPricingRange.class, rangeId);
    if (currentRecordId.equals("")) {
      info.addResult("inpqty", currentRange.getQuantity().toString());
    } else {
      LevelProductPrice levelPrice = OBDal.getInstance().get(LevelProductPrice.class,
          currentRecordId);
      if (rangeId != levelPrice.getLvlprRange().getId()) {
        currentRange = OBDal.getInstance().get(LevelPricingRange.class, rangeId);
        info.addResult("inpqty", currentRange.getQuantity().toString());
      }
    }
  }
}