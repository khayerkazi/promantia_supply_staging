/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.barcode;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Classes implementing this interface will be invoked when barcode is entered. They are in charge
 * of checking the code and returning a valid json object in case an item matching it is found.
 * 
 * @author alostale
 * 
 */
public interface BarcodeScanner {

  /**
   * Checks the code looking for an item matching it. It returns a {@link JSONObject} with the
   * following format:
   * 
   * <b>NOTE:</b> when no item is found, an empty JSONObject should be returned in order to continue
   * checking with the rest of possible scanners.
   * 
   * <code>
   * {
   *   status: (int) 0 [success] or 1 [error]. When an item is found 0 should be returned this 
   *                                           prevents other scanners to be invoked. 
   *                                           
   *   product: { (optional)
   *     id: (string) product id
   *     name: (string) product identifier
   *     uom.id: product uom id
   *     uom.name: product uom name
   *     quantity: (optional)
   *   },
   *   
   *   bin: { (optional)
   *     id: (string)
   *     name: (string)
   *   }
   *                                           
   * }
   * </code>
   * 
   * @param code
   *          barcode to look for
   * @param line
   *          current line, used to do checks
   * @return JSONObject with the item if found, or error in case it failed
   * @throws JSONException
   */
  JSONObject scan(String code, JSONObject line) throws JSONException;

  Boolean isValidEvent(String code) throws JSONException;
}
