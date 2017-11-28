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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.mobile.core.MobileCoreComponentProvider;
import org.openbravo.mobile.warehouse.WarehouseConstants;
import org.openbravo.mobile.warehouse.picking.WHPickingConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(WHPickingConstants.COMPONENT_TYPE)
public class PickingExtensionComponentProvider extends MobileCoreComponentProvider {

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  private static final String PREFIX = "web/org.openbravo.decathlon.picking.extension";

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> resources = new ArrayList<ComponentResource>();

    final String[] deps = { "relatedOrdersInformationHook.js" };

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
