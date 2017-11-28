/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.decathlon.mobilepickingextension;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.mobile.core.MobileCoreComponentProvider;
import org.openbravo.mobile.warehouse.WarehouseConstants;

/**
 * @author guilleaer
 */
@ApplicationScoped
@ComponentProvider.Qualifier(DSIMWPEConstants.COMPONENT_TYPE)
public class DSIMWPEComponentProvider extends MobileCoreComponentProvider {

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  private static final String PREFIX = "web/" + DSIMWPEConstants.MODULE_JAVAPACKAGE;

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> resources = new ArrayList<ComponentResource>();

    final String[] deps = { "DSIMWPE_pcbIconHook.js" };

    for (final String dep : deps) {
      if (dep.endsWith(".js")) {
        resources.add(createComponentResource(ComponentResourceType.Static, PREFIX + "/source/"
            + dep, WarehouseConstants.APP_IDENTIFIER));
      } else if (dep.endsWith(".css")) {
        resources.add(createComponentResource(ComponentResourceType.Stylesheet, PREFIX + "/assets/"
            + dep, WarehouseConstants.APP_IDENTIFIER));
      }
    }

    return resources;
  }
}