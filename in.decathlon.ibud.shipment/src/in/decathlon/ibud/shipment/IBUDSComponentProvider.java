package in.decathlon.ibud.shipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;

public class IBUDSComponentProvider extends BaseComponentProvider{

	@Override
	public Component getComponent(String componentId,
			Map<String, Object> parameters) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	  public List<ComponentResource> getGlobalComponentResources() {
	    final List<ComponentResource> resources = new ArrayList<ComponentResource>();
	    resources.add(createStaticResource("web/in.decathlon.ibud.shipment/js/deleteShipDetail.js", false));
	     return resources;
	    
	  }
}
