/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.decathlon.retail.refund.datasource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.retail.posterminal.JSONProcessSimple;
import org.openbravo.service.json.JsonConstants;

public class ListRefundHeaders extends JSONProcessSimple {

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    // jsonent will accept the following fields
    // -> email (an error will be raised if not present)
    // -> mobile (an error will be raised if not present)
    // -> landlineno (an error will be raised if not present)
    // -> datefrom (if not provided today will be used)
    // -> dateto (if not provided today will be used)
    // vars
    Date dateFrom;
    Date dateTo;
    Boolean hasDocNoFilter = false;
    JSONObject jsonFilter;

    Calendar today = new GregorianCalendar();
    today.setTime(new Date());

    Calendar tomorrow = new GregorianCalendar();
    tomorrow.setTime(new Date());
    tomorrow.add(Calendar.DAY_OF_MONTH, 1);

    // validate inputs
    if (!jsonsent.has("filter")) {
      throw new OBException("Filter including email, mobile and landlineno should be provided");
    }

    jsonFilter = jsonsent.getJSONObject("filter");

    if (!jsonFilter.has("email") || !jsonFilter.has("mobile") || !jsonFilter.has("landlineno")) {
      throw new JSONException("At least email, mobile or landlineno should be provided");
    }

    if (jsonFilter.has("datefrom") && jsonFilter.has("dateto")) {
      DateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
      try {
        dateFrom = inputFormat.parse(jsonFilter.getString("datefrom"));
        dateTo = inputFormat.parse(jsonFilter.getString("dateto"));
        Calendar dateToCal = new GregorianCalendar();
        dateToCal.setTime(dateTo);
        dateToCal.add(Calendar.DAY_OF_MONTH, 1);
        dateTo = dateToCal.getTime();
      } catch (Exception e) {
        dateFrom = today.getTime();
        dateTo = tomorrow.getTime();
      }
    } else {
      dateFrom = today.getTime();
      dateTo = tomorrow.getTime();
    }

    JSONObject result = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      StringBuilder SQLCreditNotesQueryStr = new StringBuilder();

      SQLCreditNotesQueryStr
          .append("select o.c_order_id as id, inv.created as creationDate, inv.documentno as documentNo, SUM(ol.line_gross_amount) as grandTotalAmount, coalesce(o.em_ncn_crtype,rr.name) as returnReason");
      SQLCreditNotesQueryStr.append(" from c_invoice inv");
      SQLCreditNotesQueryStr.append(" join c_order o on o.c_order_id=inv.c_order_id");
      SQLCreditNotesQueryStr.append(" join c_orderline ol on ol.c_order_id=o.c_order_id");
      SQLCreditNotesQueryStr.append(" join m_inout io on io.c_order_id=o.c_order_id");
      SQLCreditNotesQueryStr
          .append(" join c_return_reason rr on rr.c_return_reason_id=o.c_return_reason_id");
      SQLCreditNotesQueryStr.append(" join c_doctype cd on cd.c_doctype_id = o.c_doctype_id");
      SQLCreditNotesQueryStr
          .append(" where inv.issotrx=? and o.issotrx=? and io.issotrx=? and o.docstatus!='VO'");
      SQLCreditNotesQueryStr.append(" and abs(ol.qtyordered) > ?");
      SQLCreditNotesQueryStr.append(" and inv.created >= TO_TIMESTAMP('"
          + dateFormat.format(dateFrom) + "', 'YYYY-MM-DD')");
      SQLCreditNotesQueryStr.append(" and inv.created <= TO_TIMESTAMP('"
          + dateFormat.format(dateTo) + "', 'YYYY-MM-DD')");
      SQLCreditNotesQueryStr
          .append(" and (o.em_rc_mobileno = ? or o.em_sync_email = ? or o.em_sync_landline = ?)");
      SQLCreditNotesQueryStr.append(" and cd.name=?");
      if (jsonFilter.has("documentno") && !jsonFilter.getString("documentno").equals("")) {
        hasDocNoFilter = true;
        SQLCreditNotesQueryStr.append(" and inv.documentNo like :documentno");
      }
      SQLCreditNotesQueryStr
          .append(" group by o.c_order_id, inv.created, inv.documentno, inv.dateinvoiced, rr.name");
      SQLCreditNotesQueryStr.append(" order by inv.dateinvoiced");

      SQLQuery SQLCreditNotesQuery = OBDal.getInstance().getSession()
          .createSQLQuery(SQLCreditNotesQueryStr.toString());
      SQLCreditNotesQuery.setString(0, "Y");
      SQLCreditNotesQuery.setString(1, "Y");
      SQLCreditNotesQuery.setString(2, "Y");
      SQLCreditNotesQuery.setInteger(3, 0);
      SQLCreditNotesQuery.setString(4, jsonFilter.getString("mobile"));
      SQLCreditNotesQuery.setString(5, jsonFilter.getString("email"));
      SQLCreditNotesQuery.setString(6, jsonFilter.getString("landlineno"));
      SQLCreditNotesQuery.setString(7, "Credit Note Order");
      if (hasDocNoFilter && !jsonFilter.getString("documentno").equals("")) {
        SQLCreditNotesQuery.setString(8, "%" + jsonFilter.getString("documentno") + "%");
      }

      JSONArray creditNotesFound = new JSONArray();

      // cycle through the lines of the selected order
      for (Object creditNoteLineResult : SQLCreditNotesQuery.list()) {
        Object[] creditNoteLineResultSet = (Object[]) creditNoteLineResult;
        JSONObject creditNote = new JSONObject();
        creditNote.put("id", creditNoteLineResultSet[0]);
        creditNote.put("creationDate", creditNoteLineResultSet[1]);
        creditNote.put("documentNo", creditNoteLineResultSet[2]);
        creditNote.put("grandTotalAmount", creditNoteLineResultSet[3]);
        creditNote.put("returnReason", creditNoteLineResultSet[4]);
        creditNotesFound.put(creditNote);
      }

      result.put(JsonConstants.RESPONSE_DATA, creditNotesFound);
      result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    } catch (Exception e) {
      throw new JSONException("Something went wrong retrieving credit notes: " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }
}