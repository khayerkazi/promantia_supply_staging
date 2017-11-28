package org.openbravo.localization.india.ingst.sales;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(GstProductCodeComponentProvider.CRLMT_VIEW_COMPONENT_TYPE)
public class GstProductCodeComponentProvider  extends BaseComponentProvider{
	public static final String CRLMT_VIEW_COMPONENT_TYPE="CRLMT_CreditLimitTYpe";
    @Override
	public List<ComponentResource> getGlobalComponentResources() {
	        final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
	      globalResources.add(createStaticResource(
	       "web/org.openbravo.localization.india.ingst.sales/js/SetGstProductCode.js", false));
	      
	      return globalResources;
	      }
	 @Override
	  public List<String> getTestResources() {
	    return Collections.emptyList();
	  }
	 
	@Override
	public Component getComponent(String componentId,
			Map<String, Object> parameters) {
		// TODO Auto-generated method stub
		throw new IllegalArgumentException("Component id " + componentId + " not supported.");
	}

	
}
