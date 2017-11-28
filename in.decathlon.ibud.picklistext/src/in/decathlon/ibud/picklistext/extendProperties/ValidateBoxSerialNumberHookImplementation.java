/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package in.decathlon.ibud.picklistext.extendProperties;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.warehouse.pickinglist.PickingList;
import org.openbravo.warehouse.pickinglist.PickingListBox;
import org.openbravo.warehouse.pickinglist.hooks.ValidateBoxSerialNumberHook;

@ApplicationScoped
public class ValidateBoxSerialNumberHookImplementation implements ValidateBoxSerialNumberHook {

  @Override
  public OBError exec(String serialNo, PickingList pickingList) throws Exception {
    OBError result = new OBError();
    if (serialNo.trim().matches("^((\\d)+)-((\\w)+)-((\\w)+)")) {
      OBContext.setAdminMode(true);
      try {
        OBCriteria<PickingListBox> pickingBoxCrit = OBDal.getInstance().createCriteria(PickingListBox.class);
        pickingBoxCrit.add(Restrictions.eq(PickingListBox.PROPERTY_NAME, serialNo));
        List<PickingListBox> pbLst = pickingBoxCrit.list();

        if (pbLst.size() > 0) {
          result.setType("error");
          result.setMessage("Box serial number must be unique. There is another box in the system with the same serial number");
          return result;
        }
      } catch (Exception e) {
        result.setType("error");
        result.setMessage("An error happened while checking unique serial number. " + e.getMessage());
        return result;
      } finally {
        OBContext.restorePreviousMode();
      }
      result.setType("success");
    } else {
      result.setType("error");
      result.setMessage("Pattern not match");
    }
    return result;
  }
}
