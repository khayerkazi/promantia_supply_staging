/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.model;

/**
 * Convenience class to store HQLProperties for model extensions
 * 
 * @author alostale, ral
 * 
 */
public class HQLProperty {
  String hqlProperty;
  private String jsonName;
  Boolean includeInGroupBy;

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   */
  public HQLProperty(String hqlProperty, String jsonName) {
    this(hqlProperty, jsonName, true);
  }

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   * @param includeInGroupBy
   *          include this field in the GROUP BY
   */
  public HQLProperty(String hqlProperty, String jsonName, Boolean includeInGroupBy) {
    this.hqlProperty = hqlProperty;
    this.jsonName = jsonName;
    this.includeInGroupBy = includeInGroupBy;
  }

  /**
   * @return the JSON Name
   */
  public String getJsonName() {
    return jsonName;
  }
}
