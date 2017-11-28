/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
 
package org.openbravo.warehouse.packing.modulescript;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.modulescript.ModuleScript;
import org.openbravo.database.ConnectionProvider;

public class UOMasIsWeight extends ModuleScript {

  public void execute() {
    try {
    ConnectionProvider cp = getConnectionProvider();   
      if (UOMasIsWeightData.notMarkedAsWeight(cp)) {
        UOMasIsWeightData.updateUOM(cp);
      }
    } catch (Exception e) {
      handleError(e);
    }
  }
  
}
