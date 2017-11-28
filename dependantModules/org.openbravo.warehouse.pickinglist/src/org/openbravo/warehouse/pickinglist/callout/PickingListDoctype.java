/************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.pickinglist.callout;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.warehouse.pickinglist.OBWPL_Utils;
import org.openbravo.warehouse.pickinglist.PickingList;

public class PickingListDoctype extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String strDocTypeId = info.getStringParameter("inpcDoctypeId", IsIDFilter.instance);
    final DocumentType docType = OBDal.getInstance().get(DocumentType.class, strDocTypeId);
    final String strDocumentNo = OBWPL_Utils.getDocumentNo(docType, PickingList.TABLE_NAME);
    info.addResult("inpdocumentno", "<" + strDocumentNo + ">");
    info.addResult("DOCTYPE_USEOUTBOUND", docType.isOBWPLUseOutbound() ? "Y" : "N");
  }

}
