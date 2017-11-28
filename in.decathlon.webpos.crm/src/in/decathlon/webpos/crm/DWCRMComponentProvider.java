package in.decathlon.webpos.crm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.retail.posterminal.POSUtils;

/**
 * @author
 * 
 */
@ApplicationScoped
@ComponentProvider.Qualifier(DWCRMComponentProvider.QUALIFIER)
public class DWCRMComponentProvider extends BaseComponentProvider {

  public static final String QUALIFIER = "DWCRM_Main";
  public static final String MODULE_JAVA_PACKAGE = "in.decathlon.webpos.crm";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final GlobalResourcesHelper grhelper = new GlobalResourcesHelper();

    grhelper.add("components/CustRegWindow.js");
    grhelper.add("components/NewReceiptHook.js");
    grhelper.add("components/FeedBackWindow.js");
    grhelper.add("components/orderMobile.js");
    grhelper.add("model/crm.js");
    grhelper.add("hooks/CustRegHook.js");
    grhelper.add("components/CustRegConfirmation.js");
    grhelper.add("components/CustRegOffLineWindow.js");
    grhelper.add("utils/customerCRMCrud.js");
    grhelper.add("hooks/ModalReceiptsHook.js");
    grhelper.add("hooks/ModalPaidReceiptsHook.js");
    List<ComponentResource> resources = grhelper.getGlobalResources();
    resources.add(createStyleSheetResource(
        "web/in.decathlon.webpos.crm/web/assets/css/feedbackPopup.css", false));
    return resources;
  }

  private class GlobalResourcesHelper {
    private final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    private final String prefix = "web/" + MODULE_JAVA_PACKAGE + "/js/";

    public void add(String file) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + file,
          POSUtils.APP_NAME));

    }

    public List<ComponentResource> getGlobalResources() {
      return globalResources;
    }
  }
}
