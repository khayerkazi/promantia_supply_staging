package org.openbravo.decathlon.retail.tickettemplates;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.mobile.core.model.HQLProperty;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.retail.posterminal.term.Terminal;

@Qualifier(Terminal.terminalPropertyExtension)
public class DSITerminalPropertiesExtension extends
    org.openbravo.retail.posterminal.term.TerminalProperties {

  @Override
  public List<HQLProperty> getHQLProperties(Object params) {

    ArrayList<HQLProperty> list = new ArrayList<HQLProperty>();

    list.add(new HQLProperty("pos.organization." + Organization.PROPERTY_DSIDEFSTORETIMEDESC,
        "storeTiming"));
    list.add(new HQLProperty("pos.organization." + Organization.PROPERTY_DSIDEFSTOREPHONEDESC,
        "storePhone"));
    list.add(new HQLProperty("pos.organization." + Organization.PROPERTY_DSIDEFSTOREMANAGERMAIL,
        "storeManagerMail"));
    list.add(new HQLProperty("pos.organization." + Organization.PROPERTY_DESCRIPTION,
        "orgDescription"));
    list.add(new HQLProperty("pos.organization." + Organization.PROPERTY_DSTINNO,
            "tinNumber"));

    return list;
  }
}
