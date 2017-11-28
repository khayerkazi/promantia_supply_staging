package com.sysfore.decathlonimport.ad_process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(ImportComponentProvider.QUALIFIER)
public class ImportComponentProvider extends BaseComponentProvider {
  public static final String QUALIFIER = "";

  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource("web/com.sysfore.decathlonimport/js/ImportEdd.js",
        false));

    return globalResources;
  }

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    // TODO Auto-generated method stub
    return null;
  }
}
