/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.document.highvolumereturns;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(ReturnsHighVolumeConstants.COMPONENT_TYPE)
public class ReturnsHighVolumeComponentProvider extends BaseComponentProvider {

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final ArrayList<ComponentResource> resources = new ArrayList<ComponentResource>();
    resources.add(createStaticResource(
        "web/org.openbravo.document.highvolumereturns/js/obdhvr-return.js", false));
    return resources;
  }
}
