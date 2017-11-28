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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

public class SelectTrucks extends BaseProcessActionHandler {

  private static Logger log = Logger.getLogger(SelectTrucks.class);

  @Override
  /**
   * Main method of the SelectTrucks Class It creates Truck Details for the Selected
   * records or removes the ones that were created but are no longer selected.
   */
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonRequest = null;
    OBContext.setAdminMode();
    try {
      jsonRequest = new JSONObject(content);
      log.debug(jsonRequest);
      final String strTruckId = jsonRequest.getString("DTR_Truck_Reception_ID");
      DTRTruckReception truck = OBDal.getInstance().get(DTRTruckReception.class, strTruckId);
      final String strBParnterID = jsonRequest.getString("inpcBpartnerId");
      BusinessPartner bpartner = null;
      if (!"".equals(strBParnterID) && strBParnterID != null) {
        bpartner = OBDal.getInstance().get(BusinessPartner.class, strBParnterID);
      }
      final String strWarehouseID = jsonRequest.getString("inpmWarehouseId");
      Warehouse warehouse = null;
      if (!"".equals(strWarehouseID) && strWarehouseID != null) {
        warehouse = OBDal.getInstance().get(Warehouse.class, strWarehouseID);
      }

      List<String> idList = OBDao.getIDListFromOBObject(truck.getDTRTruckDetailsList());
      // The Selected Truck Details are going to be created
      // If there is any error, it will be included in the returned HashMap
      HashMap<String, String> map = createTruckDetails(jsonRequest, bpartner, idList, truck,
          warehouse);
      jsonRequest = new JSONObject();

      JSONObject errorMessage = new JSONObject();
      errorMessage.put("severity", "success");
      errorMessage.put("text", OBMessageUtils.messageBD("Success"));
      if (map.get("DifferentBParner").equals("true")) {
        errorMessage.put("severity", "warning");
        errorMessage.put("text", OBMessageUtils.messageBD("DTR_Different_BPartners_Selected"));
      }
      if (map.get("DifferentWarehouse").equals("true")) {
        errorMessage.put("severity", "warning");
        errorMessage.put("text", OBMessageUtils.messageBD("DTR_Different_Warehouse_Selected"));
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
   * Truck Details are going to be created for the selected records in the Window
   * 
   * @param jsonRequest
   * @param bpartner
   *          Selected Business Partner of the Truck. Can be null
   * @param idList
   *          Already existing details for the current Truck
   * @param truck
   *          Current Truck
   * @return HashMap with the number of affected records and a boolean to know if any Details belong
   *         to a different Business Partner than the one selected in the Truck.
   * @throws JSONException
   * @throws OBException
   */
  private HashMap<String, String> createTruckDetails(JSONObject jsonRequest,
      BusinessPartner bpartner, List<String> idList, DTRTruckReception truck, Warehouse warehouse)
      throws JSONException, OBException {

    HashMap<String, String> map = new HashMap<String, String>();
    map.put("DifferentBParner", "false");
    map.put("DifferentWarehouse", "false");
    map.put("Count", "0");

    JSONObject params = jsonRequest.getJSONObject("_params");
    JSONObject myGridItem = params.getJSONObject("grid");
    JSONArray selectedLines = myGridItem.getJSONArray("_selection");

    // if no lines selected remove existing Details records
    if (selectedLines.length() == 0) {
      removeNonSelectedLines(idList, truck);
      return map;
    }

    int cont = 0;
    String differentBPartner = "false";
    String differentWarehouse = "false";
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject selectedLine = selectedLines.getJSONObject((int) i);
      log.debug(selectedLine);

      // Check if the Business Partner is the same as in the Truck Header
      String strLineBpartnerID = selectedLine.getString("businessPartner");
      BusinessPartner lineBpartner = OBDal.getInstance().get(BusinessPartner.class,
          strLineBpartnerID);
      if (bpartner != null && !bpartner.equals(lineBpartner)) {
        differentBPartner = "true";
      }

      // Check if the Warehouse is the same as in the Truck Header
      String strLineWarehouseID = selectedLine.getString("warehouse");
      Warehouse lineWarehouse = OBDal.getInstance().get(Warehouse.class, strLineWarehouseID);
      if (warehouse != null && !warehouse.equals(lineWarehouse)) {
        differentWarehouse = "true";
      }

      // Create the new Truck Detail
      DTR_TruckDetails newTruckDetail = null;
      String strTruckDetails = selectedLine.getString("dTRTruckDetails");
      boolean notExistsDetails = idList.contains(strTruckDetails);
      if (notExistsDetails) {
        newTruckDetail = OBDal.getInstance().get(DTR_TruckDetails.class, strTruckDetails);
        // Don't delete this Detail record, it is selected
        idList.remove(strTruckDetails);
      } else {
        newTruckDetail = OBProvider.getInstance().get(DTR_TruckDetails.class);
      }
      newTruckDetail.setOrganization(truck.getOrganization());
      newTruckDetail.setClient(truck.getClient());
      newTruckDetail.setCreatedBy(truck.getCreatedBy());
      newTruckDetail.setUpdatedBy(truck.getUpdatedBy());
      newTruckDetail.setDTRTruckReception(truck);
      String strInoutID = selectedLine.getString("goodsShipment");
      ShipmentInOut inout = null;
      if (strInoutID != null && !"".equals(strInoutID)) {
        inout = OBDal.getInstance().get(ShipmentInOut.class, strInoutID);
      }
      newTruckDetail.setGoodsShipment(inout);
      OBDal.getInstance().save(newTruckDetail);
      OBDal.getInstance().save(truck);
      OBDal.getInstance().flush();
      cont++;
    }

    // Remove non selected Details
    removeNonSelectedLines(idList, truck);
    map.put("DifferentBParner", differentBPartner);
    map.put("DifferentWarehouse", differentWarehouse);
    map.put("Count", Integer.toString(cont));
    return map;
  }

  /**
   * Removes the Details that were created previously but now are no longer selected.
   * 
   * @param idList
   *          List of Details to remove.
   * @param truck
   *          Truck from which the Details are going to be removed.
   */
  private void removeNonSelectedLines(List<String> idList, DTRTruckReception truck) {
    if (idList.size() > 0) {
      for (String id : idList) {
        DTR_TruckDetails truckDetail = OBDal.getInstance().get(DTR_TruckDetails.class, id);
        truck.getDTRTruckDetailsList().remove(truckDetail);
        OBDal.getInstance().remove(truckDetail);
      }
      OBDal.getInstance().save(truck);
      OBDal.getInstance().flush();
    }
  }
}
