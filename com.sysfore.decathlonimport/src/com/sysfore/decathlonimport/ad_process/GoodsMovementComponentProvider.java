package com.sysfore.decathlonimport.ad_process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;

public class GoodsMovementComponentProvider extends BaseComponentProvider {

  	@Override
	public Component getComponent(String componentId, Map<String, Object> parameters) {
		// TODO Auto-generated method stub
		return null;
	}

  	@Override
  	public List<ComponentResource> getGlobalComponentResources() {
  		final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
  			globalResources.add(createStaticResource("web/com.sysfore.decathlonimport/js/GoodsMovementHandler.js", false));
  		return globalResources;
  	}

	@Override
	public List<String> getTestResources() {
		return Collections.emptyList();
	}
}
