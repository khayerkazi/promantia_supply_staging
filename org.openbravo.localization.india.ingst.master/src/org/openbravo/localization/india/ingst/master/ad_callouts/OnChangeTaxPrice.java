package org.openbravo.localization.india.ingst.master.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.localization.india.ingst.master.data.INGSTGSTProductCode;

public class OnChangeTaxPrice extends SimpleCallout {

  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    log4j.info("info" + info);

    String gstProductID = info.getStringParameter("inpemIngstGstproductcodeId", null);

    org.openbravo.model.financialmgmt.tax.TaxCategory taxCategoryObj = changeTaxPrice(gstProductID);

    if (taxCategoryObj != null) {
      info.addResult("inpcTaxcategoryId", taxCategoryObj.getId());
    } else {
      info.addResult("inpcTaxcategoryId", "");

    }
  }

  private org.openbravo.model.financialmgmt.tax.TaxCategory changeTaxPrice(String gstProductID) {
    // TODO Auto-generated method stub
    INGSTGSTProductCode ingstGSTProductCodeObj = OBDal.getInstance().get(INGSTGSTProductCode.class,
        gstProductID);
    org.openbravo.model.financialmgmt.tax.TaxCategory taxCategoryObj = null;
    if (ingstGSTProductCodeObj.getTaxCategory() != null) {
      taxCategoryObj = ingstGSTProductCodeObj.getTaxCategory();
    }
    return taxCategoryObj;

  }

}
