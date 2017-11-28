/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.decathlon.b2c.eCommerce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;

public class ExampleComponentProvider extends BaseComponentProvider {

  	@Override
	public Component getComponent(String componentId, Map<String, Object> parameters) {
		// TODO Auto-generated method stub
		return null;
	}

  	@Override
  	public List<ComponentResource> getGlobalComponentResources() {
  		final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
  			globalResources.add(createStaticResource("web/in.decathlon.b2c.eCommerce/js/print-invoice-actionhandler.js", false));
  			globalResources.add(createStaticResource("web/in.decathlon.b2c.eCommerce/js/generate-shipping-label.js", false));
  		return globalResources;
  	}

	@Override
	public List<String> getTestResources() {
		return Collections.emptyList();
	}
}
