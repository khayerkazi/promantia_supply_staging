/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse.picking;

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

@ApplicationScoped
@ComponentProvider.Qualifier(WHPickingConstants.COMPONENT_TYPE)
public class WarehousePickingComponentProvider extends MobileCoreComponentProvider {

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  private static final String PREFIX = "web/" + WHPickingConstants.MODULE_JAVAPACKAGE;

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> resources = new ArrayList<ComponentResource>();

    final String[] deps = { "picking/models/wh-picking-movementlinemodel.js",
        "picking/models/wh-picking-relatedordermodel.js", "picking/wh-picking-model.js",
        "picking/wh-picking-view.js", "picking/wh-picking-item.js",
        "picking/wh-picking-picking.js", "picking/wh-picking-search.js",
        "picking/wh-picking-selectincidence.js", "picking/wh-incidencesactions.js",
        "css/obmwhp-styles.css", "picking/wh-incidencestypesloader.js",
        "picking/wh-otherpreferencesloader.js", "picking/wh-packing.js" };

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
