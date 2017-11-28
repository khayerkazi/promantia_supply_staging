/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.warehouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.mobile.core.MobileCoreComponentProvider;
import org.openbravo.mobile.core.MobileCoreConstants;

/**
 * @author alostale
 * 
 */
@ApplicationScoped
@ComponentProvider.Qualifier(WarehouseConstants.COMPONENT_TYPE)
public class WarehouseComponentProvider extends MobileCoreComponentProvider {
  static {
    // Set dependency on Mobile Core app
    BaseComponentProvider.setAppDependencies(WarehouseConstants.APP_IDENTIFIER,
        Arrays.asList(MobileCoreConstants.APP_IDENTIFIER));
  }

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  private static final String PREFIX = "web/" + WarehouseConstants.MODULE_JAVAPACKAGE;

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> resources = new ArrayList<ComponentResource>();

    final String[] deps = { "terminal/wh-terminal.js", "mainMenu/wh-menu-model.js",
        "mainMenu/wh-menu-view.js", "goodsMovement/wh-movement-model.js",
        "goodsMovement/wh-movement-view.js", "goodsMovement/wh-movement-document.js",
        "goodsMovement/wh-movement-lineEditor.js", "goodsMovement/wh-movement-product.js",
        "goodsMovement/wh-movement-bin.js", "goodsMovement/wh-movement-attribute.js",
        "css/wh-styles.css" };

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
