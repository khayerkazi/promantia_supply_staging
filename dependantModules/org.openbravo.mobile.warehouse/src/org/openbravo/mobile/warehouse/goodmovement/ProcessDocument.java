/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.goodmovement;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.mobile.warehouse.WarehouseConstants;
import org.openbravo.mobile.warehouse.WarehouseJSONProcess;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.CallProcess;

public class ProcessDocument extends WarehouseJSONProcess {
  private static final Logger log = Logger.getLogger(ProcessDocument.class);

  @Override
  protected String getProperty() {
    return WarehouseConstants.MOVEMENT_PROPERTY;
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    long t0 = System.currentTimeMillis();

    OBContext.setAdminMode(true);

    try {
      JSONArray lines = jsonsent.getJSONArray("document");

      InternalMovement movement = OBProvider.getInstance().get(InternalMovement.class);
      movement.setName(jsonsent.getString("name"));
      movement.setMovementDate(new Date());

      for (int i = 0; i < lines.length(); i++) {
        JSONObject jsonLine = lines.getJSONObject(i);
        InternalMovementLine line = OBProvider.getInstance().get(InternalMovementLine.class);
        line.setProduct((Product) OBDal.getInstance().getProxy("Product",
            jsonLine.get("product.id")));
        line.setStorageBin((Locator) OBDal.getInstance().getProxy("Locator",
            jsonLine.get("fromBin.id")));
        line.setNewStorageBin((Locator) OBDal.getInstance().getProxy("Locator",
            jsonLine.get("toBin.id")));
        line.setUOM((UOM) OBDal.getInstance().getProxy("UOM", jsonLine.get("product.uom.id")));
        line.setMovementQuantity(BigDecimal.valueOf(jsonLine.getDouble("quantity")));

        if (jsonLine.getBoolean("product.attributeset.hasAttribute")) {
          // product.attributeset.instance.id
          line.setAttributeSetValue((AttributeSetInstance) OBDal.getInstance().getProxy(
              "AttributeSetInstance", jsonLine.get("product.attributeset.instance.id")));
        }

        line.setLineNo((long) ((i + 1) * 10));
        line.setMovement(movement);
        movement.getMaterialMgmtInternalMovementLineList().add(line);
      }
      OBDal.getInstance().save(movement);

      OBDal.getInstance().flush();

      long t1 = System.currentTimeMillis();
      log.debug("time to create the document: " + (t1 - t0));

      ProcessInstance pInstance = CallProcess.getInstance().call("M_Movement_Post",
          movement.getId(), new HashMap<String, String>());
      OBError msg = OBMessageUtils.getProcessInstanceMessage(pInstance);
      JSONObject result = new JSONObject();
      if (pInstance.getResult() == 1) {
        result.put("status", 0);
        result.put("message", msg.getMessage());
        JSONObject doc = new JSONObject();
        doc.put("docName", movement.getIdentifier());
        result.put("data", doc);
      } else {
        result.put("status", 1);
        JSONObject jsonError = new JSONObject();
        jsonError.put("message", msg.getMessage());
        result.put("error", jsonError);
        OBDal.getInstance().rollbackAndClose();
      }

      long t2 = System.currentTimeMillis();
      log.debug("time to process: " + (t2 - t1));
      log.debug(" TOTAL time: " + (t2 - t0));

      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
