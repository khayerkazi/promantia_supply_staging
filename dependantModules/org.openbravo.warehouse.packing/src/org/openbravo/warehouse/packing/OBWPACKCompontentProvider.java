/************************************************************************************ 
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************/
package org.openbravo.warehouse.packing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(OBWPACKCompontentProvider.PACKING_COMPONENT_TYPE)
public class OBWPACKCompontentProvider extends BaseComponentProvider {
  public static final String PACKING_COMPONENT_TYPE = "OBWPACK_ComponentType";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> resources = new ArrayList<ComponentResource>();
    resources.add(createStaticResource("web/org.openbravo.warehouse.packing/js/OBWPACK_Process.js",
        false));
    resources.add(createStaticResource(
        "web/org.openbravo.warehouse.packing/js/OBWPACK_PackingComponent.js", false));
    resources.add(createStaticResource(
        "web/org.openbravo.warehouse.packing/js/OBWPACK_ManagePickingBoxContent.js", false));

    resources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/"
            + "org.openbravo.warehouse.packing/ob-packing-process.js", false));

    resources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/"
            + "org.openbravo.warehouse.packing/ob-packing-process.css", false));
    return resources;
  }

}
