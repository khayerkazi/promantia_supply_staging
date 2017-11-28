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
package org.openbravo.warehouse.pickinglist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.DbUtility;

public class AssignActionHandler extends BaseActionHandler {
  final private static Logger log = Logger.getLogger(AssignActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      final String strAction = jsonRequest.getString("action");
      if ("getemployees".equals(strAction)) {
        jsonResponse.put("valuecheck", getEmployeesComboBox(jsonRequest));
        return jsonResponse;
      } else if ("assign".equals(strAction)) {
        JSONObject jsonMsg = assign(jsonRequest);
        jsonResponse.put("message", jsonMsg);
        return jsonResponse;
      }

    } catch (Exception e) {
      log.error("Error in AssignActionHandler", e);
      OBDal.getInstance().rollbackAndClose();

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", "".equals(message) ? e : message);
        jsonResponse.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error generating the error message", e2);
      }
    }

    return jsonResponse;
  }

  private JSONObject assign(JSONObject jsonRequest) throws JSONException {
    final JSONObject jsonMsg = new JSONObject();
    jsonMsg.put("severity", "success");
    jsonMsg.put("text", OBMessageUtils.messageBD("Success", false));
    final JSONArray pickingIds = jsonRequest.getJSONArray("pickings");
    final String strEmployeeId = jsonRequest.getString("employee");
    final User employee = OBDal.getInstance().get(User.class, strEmployeeId);
    List<PickingList> lstPickingListToAssign = new ArrayList<PickingList>();
    boolean doGroup = jsonRequest.getBoolean("group");
    boolean valueForPickingBoxes = false;
    PickingList groupPL = null;
    for (int j = 0; j < pickingIds.length(); j++) {
      final String strPickingId = pickingIds.getString(j);
      final PickingList pickingDal = OBDal.getInstance().get(PickingList.class, strPickingId);
      if (j == 0) {
        valueForPickingBoxes = pickingDal.isUsePickingBoxes();
      } else {
        if (pickingDal.isUsePickingBoxes() != valueForPickingBoxes) {
          throw new OBException(OBMessageUtils.messageBD("OBWPL_GroupUsingBoxesAndNonUsingBoxes",
              false));
        }
      }
      lstPickingListToAssign.add(pickingDal);
    }

    for (PickingList picking : lstPickingListToAssign) {
      if (!"DR".equals(picking.getPickliststatus()) && !"PG".equals(picking.getPickliststatus())
          && !"AS".equals(picking.getPickliststatus())) {
        throw new OBException(OBMessageUtils.messageBD("OBWPL_AssignProcessStatusError", false));
      }
      if (doGroup) {
        if (groupPL == null) {
          groupPL = createGroupPicking(picking.getOrganization(), picking.getOutboundStorageBin(),
              valueForPickingBoxes);
          Map<String, String> map = new HashMap<String, String>();
          map.put("plNumber", groupPL.getDocumentNo());
          String strMsg = OBMessageUtils.parseTranslation(
              OBMessageUtils.messageBD("OBWPL_GroupPLCreated", false), map);
          jsonMsg.put("text", strMsg);
        }
        for (InternalMovementLine mvLine : picking
            .getMaterialMgmtInternalMovementLineEMOBWPLWarehousePickingListList()) {
          if (mvLine.getOBWPLGroupPickinglist() != null) {
            continue;
          }
          mvLine.setOBWPLGroupPickinglist(groupPL);
          OBDal.getInstance().save(mvLine);
        }
        picking.setPickliststatus("GR");
      } else {
        picking.setUserContact(employee);
        picking.setPickliststatus("AS");
      }
      OBDal.getInstance().save(picking);

    }
    if (doGroup) {
      groupPL.setUserContact(employee);
      groupPL.setPickliststatus("AS");
      OBDal.getInstance().save(groupPL);
    }

    return jsonMsg;
  }

  private PickingList createGroupPicking(Organization org, Locator locator, Boolean useBoxes) {
    PickingList groupPL = OBProvider.getInstance().get(PickingList.class);
    groupPL.setOrganization(org);
    groupPL.setDocumentdate(new Date());
    DocumentType docType = OBWPL_Utils.getGroupPLDocumentType(org);
    if (docType == null) {
      throw new OBException(OBMessageUtils.messageBD("OBWPL_DoctypeMissing", false));
    }
    groupPL.setDocumentType(docType);
    groupPL.setDocumentNo(OBWPL_Utils.getDocumentNo(docType, "OBWPL_PickingList"));
    groupPL.setPickliststatus("DR");
    groupPL.setUsePickingBoxes(useBoxes);
    groupPL.setOutboundStorageBin(locator);

    return groupPL;
  }

  private JSONObject getEmployeesComboBox(JSONObject jsonRequest) throws Exception {
    final String strOrgId = jsonRequest.getString("organization");
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider(
        OBContext.getOBContext().getCurrentClient().getId());
    Set<String> orgs = osp.getNaturalTree(strOrgId);
    OBCriteria<User> critEmployees = OBDal.getInstance().createCriteria(User.class);
    critEmployees.createAlias(User.PROPERTY_ORGANIZATION, "org");
    critEmployees.add(Restrictions.isNotNull(User.PROPERTY_USERNAME));
    critEmployees.add(Restrictions.in("org.id", orgs));
    JSONObject response = new JSONObject();
    JSONObject valueMap = new JSONObject();
    for (User employee : critEmployees.list()) {
      valueMap.put(employee.getId(), employee.getIdentifier());
    }
    response.put("valueMap", valueMap);
    response.put("defaultValue", "");
    return response;
  }
}
