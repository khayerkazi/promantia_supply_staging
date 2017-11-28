/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;

public abstract class DataSynchronizationErrorHandler {

  public static String getErrorMessage(Throwable e) {
    StringWriter sb = new StringWriter();
    e.printStackTrace(new PrintWriter(sb));
    return sb.toString();
  }

  public abstract void handleError(Throwable e, Entity entity, JSONObject result,
      JSONObject jsonRecord);
}
