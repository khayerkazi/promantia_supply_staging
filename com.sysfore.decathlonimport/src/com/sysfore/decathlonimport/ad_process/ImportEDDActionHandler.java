package com.sysfore.decathlonimport.ad_process;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

import com.sysfore.decathlonimport.ImportEDD;

public class ImportEDDActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONArray eddIds = jsonData.getJSONArray("edds");
      final String action = jsonData.getString("action");
      int errStatus = 0;

      if ("validate".equals(action)) {
        final OBError msg = new OBError();
        for (int i = 0; i < eddIds.length(); i++) {
          final String orderId = eddIds.getString(i);

          String errorMsg = "";
          ImportEDD importedEDD = OBDal.getInstance().get(ImportEDD.class, orderId);
          String modelCode = importedEDD.getModelCode();
          String orderRefNumber = importedEDD.getOrderRefno();
          // String comments = importedEDD.getComments();

          if (orderRefNumber == null || orderRefNumber.isEmpty()
              || importedEDD.getOrderRefno() == null) {
            errorMsg = "Order Reference No should not be blank";
          }
          if (modelCode == null || modelCode.isEmpty() || importedEDD.getModelCode() == null) {
            errorMsg = "Model Code should not be blank";
          }
          if ((modelCode == null || modelCode.isEmpty() || importedEDD.getModelCode() == null)
              && (orderRefNumber == null || orderRefNumber.isEmpty() || importedEDD.getOrderRefno() == null)) {
            errorMsg = "Order Reference No. and Model Code should not be blank";
          }
          if (importedEDD.getEdd() == null) {
            errorMsg = "EDD should not be blank";
          }
          if ((modelCode == null || modelCode.isEmpty() || importedEDD.getModelCode() == null)
              && (orderRefNumber == null || orderRefNumber.isEmpty() || importedEDD.getOrderRefno() == null)
              && importedEDD.getEdd() == null) {
            errorMsg = "Order Reference No. , Model Code and EDD should not be blank";
          }

          if (importedEDD.getModelCode() != null && importedEDD.getOrderRefno() != null
              && importedEDD.getEdd() != null) {

            final OBCriteria<Order> obCriteria = OBDal.getInstance().createCriteria(Order.class);
            obCriteria.add(Restrictions.eq(Order.PROPERTY_ORDERREFERENCE, orderRefNumber));
            obCriteria.add(Restrictions.eq(Order.PROPERTY_SWEMSWMODELCODE, modelCode));

            Order order = (Order) obCriteria.uniqueResult();
            boolean itemCodeExists = false;
            if (order == null) {
              errorMsg = "Invalid Order Reference No or/and Model Code";
            } else if (importedEDD.getItemCode() != null) {// item code check if not blank

              for (OrderLine ol : order.getOrderLineList()) {
                if (ol.getProduct().getName().equals(importedEDD.getItemCode())) {
                  itemCodeExists = true;
                }
              }
              if (!itemCodeExists) {
                errorMsg = "Item Code is not present for Order with document No. - "
                    + order.getDocumentNo();
              }
            }// end of item code check
          }
          // OBError is also used for successful results
          if (errorMsg.equals("")) {
            importedEDD.setErrorMsg("");
            importedEDD.setValidated(true);
            OBDal.getInstance().save(importedEDD);
            msg.setType("Success");
            msg.setTitle("Success");
            msg.setMessage("EDD Validated Successfully");

          } else {
            errStatus = 1;
            importedEDD.setErrorMsg(errorMsg);
            OBDal.getInstance().save(importedEDD);
            msg.setType("Error");
            msg.setTitle("Error occurred");
            msg.setMessage("Validation Failed!");
          }
        }
        JSONObject result = new JSONObject();
        result.put("updated", eddIds.length());
        result.put("operation", action);
        result.put("errStatus", errStatus);

        return result;

      } else if ("process".equals(action)) {
        final OBError msg = new OBError();
        for (int i = 0; i < eddIds.length(); i++) {
          final String orderId = eddIds.getString(i);

          // String errorMsg = "";
          ImportEDD importedEDD = OBDal.getInstance().get(ImportEDD.class, orderId);
          String modelCode = importedEDD.getModelCode();
          String orderRefNumber = importedEDD.getOrderRefno();
          String comments = importedEDD.getComments();
          // check whether validated or not

          if (importedEDD.isValidated()) {

            final OBCriteria<Order> obCriteria = OBDal.getInstance().createCriteria(Order.class);
            obCriteria.add(Restrictions.eq(Order.PROPERTY_ORDERREFERENCE, orderRefNumber));
            obCriteria.add(Restrictions.eq(Order.PROPERTY_SWEMSWMODELCODE, modelCode));
            final List<Order> oList = obCriteria.list();

            // update PO
            Order po = oList.get(0);
            po.setSWEMSwEstshipdate(importedEDD.getEdd());
            po.setImComments(comments);
            OBDal.getInstance().save(po);

            // Update Import EDD
            importedEDD.setProcessed(true);
            OBDal.getInstance().save(importedEDD);

            // OBError is also used for successful results
            msg.setType("Success");
            msg.setTitle("Success");
            msg.setMessage("EDD Updated Successfully");
          } else {
            msg.setType("Error");
            msg.setTitle("Error occurred");
            msg.setMessage("Please Validate before Importing EDD");
          }

        }
        JSONObject result1 = new JSONObject();
        result1.put("updated", eddIds.length());
        result1.put("operation", action);
        result1.put("processed", msg.getType());
        return result1;

      } else {
        throw new IllegalStateException("Action not supported: " + action);
      }
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
