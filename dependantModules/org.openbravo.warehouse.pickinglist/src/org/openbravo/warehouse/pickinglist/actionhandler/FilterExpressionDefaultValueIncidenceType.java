/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.warehouse.pickinglist.actionhandler;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;

public class FilterExpressionDefaultValueIncidenceType implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    // For the moment just standard incidences are supported in the ERP.
    return "2BD81F8D36664702B8B5E7C2FCD03F12";
  }

}
