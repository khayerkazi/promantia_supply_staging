/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package in.decathlon.retail.webpos.extendedhooks;

import javax.enterprise.context.ApplicationScoped;

import org.apache.axis.utils.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.retail.posterminal.OrderLoaderPreProcessHook;

@ApplicationScoped
public class PreOrderLoaderHookImplementation implements OrderLoaderPreProcessHook {
  private static final Logger log = Logger.getLogger(PreOrderLoaderHookImplementation.class);

  @Override
  public void exec(JSONObject jsonorder) throws Exception {
    JSONArray jsonOrderLines = jsonorder.getJSONArray("lines");
    for (int i = 0; i < jsonOrderLines.length(); i++) {
      JSONObject currentJsonOrderLine = (JSONObject) jsonOrderLines.get(i);
      JSONObject jsonProduct = currentJsonOrderLine.getJSONObject("product");
      if (jsonProduct.has("clChasisnumber")
          && !StringUtils.isEmpty(jsonProduct.getString("clChasisnumber"))) {
        currentJsonOrderLine.put("clChasisnumber", jsonProduct.getString("clChasisnumber"));
      }
    }
  }
}