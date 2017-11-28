package in.nous.dmi.orderclose.ad_process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(MultiSendOrderComponentProvider.COMPONENT_TYPE)
public class MultiSendOrderComponentProvider extends BaseComponentProvider {

  public static final String COMPONENT_TYPE = "NDOC_Process";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final ArrayList<ComponentResource> resources = new ArrayList<ComponentResource>();
    resources.add(createStaticResource("web/in.nous.dmi.orderclose/js/multiSendOrder.js", false));
    return resources;
  }
}
