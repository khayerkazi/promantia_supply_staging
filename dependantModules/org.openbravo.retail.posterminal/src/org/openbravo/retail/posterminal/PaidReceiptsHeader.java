/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.retail.posterminal;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class PaidReceiptsHeader extends ProcessHQLQuery {

  @Override
  protected boolean isAdminMode() {
    return true;
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {

    // OBContext.setAdminMode(true);
    JSONObject json = jsonsent.getJSONObject("filters");
    String strIsLayaway = "false";
    if (json.getBoolean("isLayaway")) {
      strIsLayaway = "true";
    }
    String hqlPaidReceipts = "select ord.id as id, ord.documentNo as documentNo, ord.orderDate as orderDate, "
        + "ord.businessPartner.name as businessPartner, ord.grandTotalAmount as totalamount, ord.documentType.id as documentTypeId, '"
        + strIsLayaway
        + "' as isLayaway from Order as ord "
        + "where ord.client='"
        + json.getString("client")
        + "' and ord.organization='"
        + json.getString("organization")
        + "' and ord.obposApplications is not null";

    if (!json.getString("filterText").isEmpty()) {
      hqlPaidReceipts += " and (ord.documentNo like '%" + json.getString("filterText")
          + "%' or REPLACE(ord.documentNo, '/', '') like '%"
          + json.getString("filterText").replace("-", "")
          + "%' or upper(ord.businessPartner.name) like upper('%" + json.getString("filterText")
          + "%')) ";
    }
    if (!json.isNull("documentType")) {
      JSONArray docTypes = json.getJSONArray("documentType");
      hqlPaidReceipts += " and ( ";
      for (int docType_i = 0; docType_i < docTypes.length(); docType_i++) {
        hqlPaidReceipts += "ord.documentType.id='" + docTypes.getString(docType_i) + "'";
        if (docType_i != docTypes.length() - 1) {
          hqlPaidReceipts += " or ";
        }
      }
      hqlPaidReceipts += " )";
    }
    if (!json.getString("docstatus").isEmpty() && !json.getString("docstatus").equals("null")) {
      hqlPaidReceipts += " and ord.documentStatus='" + json.getString("docstatus") + "'";
    }
    if (!json.getString("startDate").isEmpty()) {
      hqlPaidReceipts += " and ord.orderDate >='" + json.getString("startDate") + "'";
    }
    if (!json.getString("endDate").isEmpty()) {
      hqlPaidReceipts += " and ord.orderDate <='" + json.getString("endDate") + "'";
    }
    if (json.getBoolean("isLayaway")) {
      // (It is Layaway)
      hqlPaidReceipts += " and (select sum(deliveredQuantity) from ord.orderLineList where orderedQuantity > 0)=0 and ord.documentStatus = 'CO' ";
    } else if (json.getBoolean("isReturn")) {
      // (It is not Layaway and It is not a Return)
      hqlPaidReceipts += " and ((select count(deliveredQuantity) from ord.orderLineList where deliveredQuantity != 0) > 0 and (select count(orderedQuantity) from ord.orderLineList where orderedQuantity > 0) > 0) ";
    } else {
      // (It is not Layaway or it is a Return)
      hqlPaidReceipts += " and ((select sum(deliveredQuantity) from ord.orderLineList where deliveredQuantity > 0) > 0 or (select sum(deliveredQuantity) from ord.orderLineList) < 0) ";
    }
    hqlPaidReceipts += " order by ord.orderDate desc, ord.documentNo desc";
    return Arrays.asList(new String[] { hqlPaidReceipts });
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }
}