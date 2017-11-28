package in.decathlon.webpos.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.retail.posterminal.POSUtils;

/**
 * @author
 * 
 */
@ApplicationScoped
@ComponentProvider.Qualifier(DWRComponentProvider.QUALIFIER)
public class DWRComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "DWR_Main";
  public static final String MODULE_JAVA_PACKAGE = "in.decathlon.webpos.receipt";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";

    String[] resourceList = { "changeDefaultTemplates" };

    for (String resource : resourceList) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + resource
          + ".js", POSUtils.APP_NAME));
    }

    return globalResources;
  }
}
