package in.decathlon.ibud.picklistext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(IBUDPKComponentProvider.PICKINGLISTEXT_COMPONENT_TYPE)
public class IBUDPKComponentProvider extends BaseComponentProvider{
	public static final String PICKINGLISTEXT_COMPONENT_TYPE = "IBUDPK_ComponentType";


	@Override
	public Component getComponent(String componentId,Map<String, Object> parameters) {
		return null;
	}
	
	@Override
	  public List<ComponentResource> getGlobalComponentResources() {
	    final List<ComponentResource> resources = new ArrayList<ComponentResource>();
	    resources.add(createStaticResource("web/in.decathlon.ibud.picklistext/js/ibudpk-completePk.js", false));
	    resources.add(createStaticResource("web/in.decathlon.ibud.picklistext/js/ibudpk_Process.js", false));
	    resources.add(createStaticResource("web/in.decathlon.ibud.picklistext/js/ibudpk-processShipmentPk.js", false));
	    return resources;
	    
	  }
	
}
