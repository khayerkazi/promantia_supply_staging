/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.warehouse.shipping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.CallStoredProcedure;

public class Process extends BaseProcessActionHandler {

  private static Logger log = Logger.getLogger(Process.class);

  @Override
  /**
   * Main method of the Process class
   * It completes the Shipping Document
   */
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      log.debug(jsonRequest);
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");

      boolean completeShipments = (Boolean) params.get("CompleteShipments");
      final String strShippingId = jsonRequest.getString("Obwship_Shipping_ID");
      OBWSHIPShipping shipping = OBDal.getInstance().get(OBWSHIPShipping.class, strShippingId);

      // Call the process Method. If there is any error, there will be an entry on the HashMap to
      // detail it. If not the HashMap will be empty
      HashMap<String, List<String>> map = processShipping(shipping, completeShipments);
      jsonRequest = new JSONObject();

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      // Check possible errors
      if (map.containsKey("NoShippingDetails")) {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWSHIP_No_Details"));
      } else if (map.containsKey("ErrorWhileProcessing")) {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("OBWSHIP_Processing_Error"));
      }
      jsonRequest.put("message", errorMessage);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage(), e);

      try {
        jsonRequest = new JSONObject();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD(e.getMessage()));
        jsonRequest.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonRequest;
  }

  /**
   * Complete the Shipping
   * 
   * @param shipping
   * @param completeShipments
   *          . If true, the Shipments inside the Shipping will be completed also
   * @return a HashMap with the description of the problems found if any
   */
  private HashMap<String, List<String>> processShipping(OBWSHIPShipping shipping,
      boolean completeShipments) {
    HashMap<String, List<String>> result = new HashMap<String, List<String>>();

    try {

      // If the Document is already complete, revert it to Draft Status
      if (isCompleted(shipping)) {
        if (shipping.isShipped()) {
          shipping.setDocumentStatus("SHIP");
        } else {
          shipping.setDocumentStatus("DR");
        }
        OBDal.getInstance().save(shipping);
        return result;
      }

      // Raise an error if the Shipping has no details
      if (hasNoDetails(shipping)) {
        result.put("NoShippingDetails", null);
        return result;
      }

      if (completeShipments) {
        for (OBWSHIPShippingDetails shippingDetails : shipping.getOBWSHIPShippingDetailsList()) {
          if ("DR".equals(shippingDetails.getGoodsShipment().getDocumentStatus())) {
            final List<Object> param = new ArrayList<Object>();
            param.add(null);
            param.add(shippingDetails.getGoodsShipment().getId());
            CallStoredProcedure.getInstance().call("M_Inout_POST", param, null, true, false);
          }
        }
      }

      // If there has been no problems, complete the Document
      shipping.setDocumentStatus("CO");
      OBDal.getInstance().save(shipping);

    } catch (Exception e) {
      log.error("An error happened when processShipping was executed: " + e.getMessage(), e);
      result.put("ErrorWhileProcessing", null);
    }
    return result;
  }

  // Returns true if the Shipping is in Completed Status
  private boolean isCompleted(OBWSHIPShipping shipping) {
    return shipping.getDocumentStatus().equals("CO");
  }

  private boolean hasNoDetails(OBWSHIPShipping shipping) {
    return shipping.getOBWSHIPShippingDetailsList().size() == 0;
  }
}
