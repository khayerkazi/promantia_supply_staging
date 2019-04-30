/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.openbravo.decathlon.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.retail.posterminal.POSUtils;

@ApplicationScoped
@ComponentProvider.Qualifier(ComponentProvider.DECOPE_COMPONENT_PROVIDER_QUALIFIER)
public class ComponentProvider extends BaseComponentProvider {
  public static final String DECOPE_COMPONENT_PROVIDER_QUALIFIER = "DECOPE_COMPONENT_PROVIDER_QUALIFIER";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource(
        "web/com.openbravo.decathlon.operator/js/processes/reprocessOperatorData.js", false));
    globalResources.add(createComponentResource(ComponentResourceType.Static,
        "web/com.openbravo.decathlon.operator/js/maxReturnApproval.js", POSUtils.APP_NAME));
    return globalResources;
  }

  @Override
  public List<String> getTestResources() {
    return Collections.emptyList();
  }

}