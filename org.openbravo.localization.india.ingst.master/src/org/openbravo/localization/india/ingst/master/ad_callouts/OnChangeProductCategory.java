package org.openbravo.localization.india.ingst.master.ad_callouts;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.localization.india.ingst.master.data.INGSTGSTProductCode;
import org.openbravo.model.common.plm.ProductCategory;

public class OnChangeProductCategory extends SimpleCallout {

  /**
		 * 
		 */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) {
    // TODO Auto-generated method stub
    log4j.info("info" + info);

    String gstProductCategoryID = info.getStringParameter("inpmProductCategoryId", null);

    INGSTGSTProductCode taxCategoryObj = changeProductCategory(gstProductCategoryID);

    if (taxCategoryObj != null) {
      info.addResult("inpemIngstGstproductcodeId", taxCategoryObj.getId());
    } else {
      info.addResult("inpemIngstGstproductcodeId", "");

    }
  }

  private INGSTGSTProductCode changeProductCategory(String gstProductCategoryID) {
    // TODO Auto-generated method stub
    ProductCategory productCategoryObj = OBDal.getInstance().get(ProductCategory.class,
        gstProductCategoryID);
    INGSTGSTProductCode ingstGSTProductCodeObj = null;
    if (productCategoryObj.getIngstGstproductcode() != null) {
      ingstGSTProductCodeObj = productCategoryObj.getIngstGstproductcode();
    }

    return ingstGSTProductCodeObj;

  }

}
