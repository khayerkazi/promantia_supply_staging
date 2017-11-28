package com.sysfore.storewarehouse;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
 
import javax.enterprise.context.ApplicationScoped;
 
import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import com.sysfore.storewarehouse.handler.GRPickExecuteToComplete;

 
/**
 * @author guilleaer
 * 
 */
@ApplicationScoped
@ComponentProvider.Qualifier(StoreWarehouseComponentProvider.QUALIFIER)
public class StoreWarehouseComponentProvider extends BaseComponentProvider {
  public static final String QUALIFIER = "SW";
  public static final String MODULE_JAVA_PACKAGE = "com.sysfore.storewarehouse";
 
  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }
 
  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";
 
    String[] resourceList = { "picklistrefresh" };
 
    for (String resource : resourceList) {
      globalResources.add(createStaticResource(prefix + resource
          + ".js", true));
    }
 
    return globalResources;
  }
}