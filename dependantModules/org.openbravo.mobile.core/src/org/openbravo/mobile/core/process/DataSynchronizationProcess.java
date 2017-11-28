/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.service.json.JsonConstants;

public abstract class DataSynchronizationProcess extends JSONProcessSimple {

  private static final Logger log = Logger.getLogger(DataSynchronizationProcess.class);

  @Inject
  @Any
  private Instance<DataSynchronizationErrorHandler> errorHandlers;

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    return exec(jsonsent, false);
  }

  /**
   * This method executes the process for all the records in the "data" array, inside the JSON
   * object. If the process fails for a record, it will automatically find an appropriate error
   * handler class, and execute it. A transaction will be used for every record, so every record is
   * considered independent
   * 
   * @param jsonsent
   *          The JSON object which should contain a "data" property, an array which contains a
   *          JSONObject for every record which needs to be saved
   * @param shouldFailWithError
   *          If this is set to true, the process will fail if an error was detected for at least
   *          one error
   * @return returns a JSONObject with the result of the execution
   * @throws JSONException
   * @throws ServletException
   */
  public JSONObject exec(JSONObject jsonsent, boolean shouldFailWithError) throws JSONException,
      ServletException {
    Object jsondata = jsonsent.get("data");

    JSONArray array = null;
    if (jsondata instanceof JSONObject) {
      array = new JSONArray();
      array.put(jsondata);
    } else if (jsondata instanceof String) {
      JSONObject obj = new JSONObject((String) jsondata);
      array = new JSONArray();
      array.put(obj);
    } else if (jsondata instanceof JSONArray) {
      array = (JSONArray) jsondata;
    }

    long t1 = System.currentTimeMillis();
    JSONObject result = this.saveRecord(array, shouldFailWithError);
    log.info("Final total time: " + (System.currentTimeMillis() - t1));
    return result;
  }

  public JSONObject saveRecord(JSONArray jsonarray, boolean shouldFailWithError)
      throws JSONException {
    boolean error = false;
    List<String> errorIds = new ArrayList<String>();
    String originalOrg = (String) RequestContext.get().getSession().getAttribute("#AD_ORG_ID");
    String originalUser = (String) RequestContext.get().getSession().getAttribute("#AD_USER_ID");
    String originalRole = (String) RequestContext.get().getSession().getAttribute("#AD_ROLE_ID");
    OBContext.setAdminMode(true);
    try {
      for (int i = 0; i < jsonarray.length(); i++) {
        String currentOrg = (String) RequestContext.get().getSession().getAttribute("#AD_ORG_ID");
        String currentUser = (String) RequestContext.get().getSession().getAttribute("#AD_USER_ID");
        String currentRole = (String) RequestContext.get().getSession().getAttribute("#AD_ROLE_ID");
        long t1 = System.currentTimeMillis();
        JSONObject jsonRecord = jsonarray.getJSONObject(i);
        String userId = jsonRecord.has("createdBy") ? jsonRecord.getString("createdBy")
            : jsonRecord.getString("userId");
        String orgId = jsonRecord.has("organization") ? jsonRecord.getString("organization")
            : currentOrg;
        if (!currentUser.equals(userId) || !currentOrg.equals(orgId)) {
          OBContext.setOBContext(userId, currentRole, OBContext.getOBContext().getCurrentClient()
              .getId(), orgId);
        }
        JSONObject result = null;
        try {
          result = saveRecord(jsonRecord);
          if (!result.get(JsonConstants.RESPONSE_STATUS).equals(
              JsonConstants.RPCREQUEST_STATUS_SUCCESS)) {
            log.error(this.getClass().getName() + ": There was an error importing record: "
                + jsonRecord.toString());
            error = true;
            errorIds.add(jsonRecord.getString("id"));
          }
          if (i % 1 == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getConnection(false).commit();
            OBDal.getInstance().getSession().clear();
          }
          log.info("Total process " + this.getClass().getName() + " time: "
              + (System.currentTimeMillis() - t1));
        } catch (Throwable t) {
          OBDal.getInstance().rollbackAndClose();
          if (TriggerHandler.getInstance().isDisabled()) {
            TriggerHandler.getInstance().enable();
          }
          if (shouldFailWithError) {
            error = true;
          }

          // check if there is a record with the same id. If it exists it means that it is a
          // duplicated record. Then, this error will not be stored
          List<Object> parameters = new ArrayList<Object>();
          parameters.add(jsonRecord.getString("id"));
          OBQuery<BaseOBObject> records = OBDal.getInstance().createQuery(getEntity().getName(),
              "id=?");
          records.setParameters(parameters);
          if (records.count() > 0 && additionalCheckForDuplicates(jsonRecord)) {
            log.warn("Record in process " + this.getClass().getName() + "duplicated with id: "
                + jsonRecord.getString("id") + "  Error not saved.");
          } else {
            handleError(t, this.getEntity(), result, jsonRecord);
          }
          try {
            OBDal.getInstance().getConnection().commit();
          } catch (SQLException e1) {
            error = true;
            errorIds.add(jsonRecord.getString("id"));
            log.error(
                this.getClass().getName()
                    + ": Critical Error: The process to save the record"
                    + jsonRecord.getString("id")
                    + " has failed and the record cannot be saved as an error. To avoid lose data, please don't remove the browser cache. \n Record: "
                    + jsonRecord.toString() + " \n", e1);
          }
        }
      }
    } finally {
      OBContext.restorePreviousMode();
      OBContext.setOBContext(originalUser, originalRole, OBContext.getOBContext()
          .getCurrentClient().getId(), originalOrg);
    }
    JSONObject jsonResponse = new JSONObject();
    if (!error) {
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      jsonResponse.put("result", "0");
    } else {
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
      jsonResponse.put("result", "0");
      JSONObject errors = new JSONObject();
      if (errorIds.size() > 0) {
        jsonResponse.put("errorids", errorIds);
        errors.put("message", "Records [" + errorIds.toString() + "] cannot be saved");
      } else {
        errors.put("message", "Some records cannot be saved");
      }
      jsonResponse.put("error", errors);
    }
    return jsonResponse;
  }

  private void handleError(Throwable t, Entity entity, JSONObject result, JSONObject jsonRecord) {
    DataSynchronizationErrorHandler errorHandler = getErrorHandler();
    if (errorHandler == null) {
      try {
        log.error(
            this.getClass().getName() + ": Synchronization of record " + jsonRecord.getString("id")
                + " failed (no error handler available)", t);
      } catch (JSONException e1) {
        // won't happen
      }
    } else {
      errorHandler.handleError(t, entity, result, jsonRecord);
    }
  }

  private DataSynchronizationErrorHandler getErrorHandler() {

    DataSynchronizationErrorHandler errorHandler = null;
    try {
      errorHandler = errorHandlers.select(new ComponentProvider.Selector(getAppName())).get();
    } catch (Exception e) {
      log.debug("Error retrieving error handler for " + getAppName(), e);
      // ignore it
    }
    return errorHandler;
  }

  /**
   * Optionally checks for additional information to better conclude that the record is indeed
   * duplicated
   * 
   * @param record
   * @return
   */
  protected boolean additionalCheckForDuplicates(JSONObject record) {
    return true;
  }

  public Entity getEntity() {
    return ModelProvider.getInstance().getEntity(
        this.getClass().getAnnotation(DataSynchronization.class).entity());
  }

  public abstract String getAppName();

  public abstract JSONObject saveRecord(JSONObject jsonRecord) throws Exception;

  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface DataSynchronization {
    String entity();
  }

  @SuppressWarnings("all")
  public static class Selector extends AnnotationLiteral<DataSynchronization> implements
      DataSynchronization {
    private static final long serialVersionUID = 1L;

    final String entity;

    public Selector(String entity) {
      this.entity = entity;
    }

    public String entity() {
      return entity;
    }
  }

}
