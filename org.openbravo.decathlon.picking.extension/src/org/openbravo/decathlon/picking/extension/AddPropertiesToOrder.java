/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.decathlon.picking.extension;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.mobile.core.model.ModelExtension;
import org.openbravo.mobile.warehouse.picking.datasource.OBMWHP_relatedSalesOrders_dataSource;

@Qualifier(OBMWHP_relatedSalesOrders_dataSource.relatedSalesOrderExtension)
public class AddPropertiesToOrder extends ModelExtension {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {
    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>() {
      private static final long serialVersionUID = 1L;
      private static final String alias = "ord.";
      {
        add(new HQLProperty(alias + "deoTrayNumber", "deoTrayNumber"));
      }
    };
    return list;
  }
}