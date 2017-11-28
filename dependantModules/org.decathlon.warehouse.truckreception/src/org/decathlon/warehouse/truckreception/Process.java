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

package org.decathlon.warehouse.truckreception;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.JSONWebServiceInvocationHelper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;

public class Process extends BaseProcessActionHandler {
  private static Logger log = Logger.getLogger(Process.class);

  @Override
  /**
   * Main method of the Process class
   * It completes the Truck Reception Document
   */
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      log.debug(jsonRequest);
      jsonRequest = new JSONObject(content);
      JSONObject params = jsonRequest.getJSONObject("_params");

      boolean completeReceipts = (Boolean) params.get("CompleteReceipts");
      final String strTruckId = jsonRequest.getString("DTR_Truck_Reception_ID");
      DTRTruckReception truck = OBDal.getInstance().get(DTRTruckReception.class, strTruckId);

      // Call the process Method. If there is any error, there will be an entry on the HashMap to
      // detail it. If not the HashMap will be empty
      HashMap<String, List<String>> map = processTruck(truck, completeReceipts);
      jsonRequest = new JSONObject();

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      // Check possible errors
      if (map.containsKey("NoTruckDetails")) {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("DTR_No_Details"));
      } else if (map.containsKey("ErrorWhileProcessing")) {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("DTR_Processing_Error"));
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
   * Complete the Truck
   * 
   * @param truck
   * @param completeReceipts
   *          . If true, the Goods Receipts inside the Truck will be completed also
   * @return a HashMap with the description of the problems found if any
   */
  private HashMap<String, List<String>> processTruck(DTRTruckReception truck,
      boolean completeReceipts) {
    int count = 0;
    HashMap<String, List<String>> result = new HashMap<String, List<String>>();
    try {
      String truckDocNo = truck.getDocumentNo();
      if (truckDocNo.length() > 0) {
        JSONObject soDetails = new JSONObject();
        try {
          log.debug("Call to Web Service to close SO and complete Shipment");
          String wsName = "in.decathlon.ibud.shipment.CompleteShippingWS";
          soDetails = JSONWebServiceInvocationHelper.sendPostrequestToClose(wsName, "truckId="
              + truckDocNo, "{}");
          // JSONWebServiceInvocationHelper.sendPostrequest(wsName, "truckId=" + truckDocNo, "{}");
        } catch (Exception e) {
          e.printStackTrace();
          log.error("Error from Supply " + e);
          throw new OBException("Error from Supply " + e);
        }

        for (DTR_TruckDetails truckDetails : truck.getDTRTruckDetailsList()) {
          try {
            ShipmentInOut goodShipment = truckDetails.getGoodsShipment();
            if (goodShipment.getDocumentStatus().equals(SOConstants.CompleteDocumentStatus)) {
              log.debug("GRN is already closed so not proceeding to CL the PO ");
            } else {
            if ((count % 10) == 0) {
              SessionHandler.getInstance().commitAndStart();

            }
            BusinessEntityMapper.executeProcess(goodShipment.getId(), "104",
                "SELECT * FROM M_InOut_Post0(?)");
            OBDal.getInstance().refresh(goodShipment);
            goodShipment.setIbodtrCompletedTime(new Date());
            OBDal.getInstance().save(goodShipment);
            List<ShipmentInOutLine> grLineList = goodShipment
                .getMaterialMgmtShipmentInOutLineList();
            // Update custom movement type field that is set when M_Inout_Post is called

            List<MaterialTransaction> txnList = null;
            for (ShipmentInOutLine grnLine : grLineList) {
              txnList = grnLine.getMaterialMgmtMaterialTransactionList();
              for (MaterialTransaction txn : txnList) {
                txn.setSwMovementtype(goodShipment.getSWMovement());
                OBDal.getInstance().save(txn);
              }
            }
            EnhancedProcessGoods.updateRecQty(goodShipment, soDetails);
           } 
          } catch (Exception e) {
            e.printStackTrace();
            log.error("Error in retail " + e);

            // throw new OBException("Error in retail " + e);
          }
          count++;
        }
        SessionHandler.getInstance().commitAndStart();
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error from Supply " + e);
      throw new OBException("Error from Supply " + e);
    }
    try {

      // If the Document is already complete, revert it to Draft Status
      if (isCompleted(truck)) {
        truck.setDocumentStatus("DR");
        OBDal.getInstance().save(truck);
        return result;
      }

      // Raise an error if the Shipping has no details
      if (hasNoDetails(truck)) {
        result.put("NoTruckDetails", null);
        return result;
      }

      if (completeReceipts) {
        for (DTR_TruckDetails truckDetails : truck.getDTRTruckDetailsList()) {
          if ("DR".equals(truckDetails.getGoodsShipment().getDocumentStatus())) {
            if ((count % 10) == 0) {
              SessionHandler.getInstance().commitAndStart();
            }
            final List<Object> param = new ArrayList<Object>();
            param.add(null);
            param.add(truckDetails.getGoodsShipment().getId());

            try {
              CallStoredProcedure.getInstance().call("M_Inout_POST", param, null, true, false);
            } catch (Exception e) {
              e.printStackTrace();
              log.error("Error in retail " + e);

            }

          }
          count++;
        }
      }

      // If there has been no problems, complete the Document
      truck.setDocumentStatus("CO");
      OBDal.getInstance().save(truck);

    } catch (Exception e) {
      log.error("An error happened when processShipping was executed: " + e.getMessage(), e);
      result.put("ErrorWhileProcessing", null);
    }
    return result;
  }

  // Returns true if the Shipping is in Completed Status
  private boolean isCompleted(DTRTruckReception truck) {
    return truck.getDocumentStatus().equals("CO");
  }

  private boolean hasNoDetails(DTRTruckReception truck) {
    return truck.getDTRTruckDetailsList().size() == 0;
  }

}
