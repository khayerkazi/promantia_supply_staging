package in.decathlon.ibud.picklistext.extendProperties;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.warehouse.pickinglist.hooks.GroupingPLHook;

public class GroupPickListByDept implements GroupingPLHook {

  @Override
  public String exec(String groupingOption, Order order) throws Exception {
    if (groupingOption.equals("IBUDPK_groupByDept")) {
      Organization org = BusinessEntityMapper.getOrgOfBP(order.getBusinessPartner().getId());
      if (order.getClBrand() != null) {
        return order.getClBrand().getId() + order.getWarehouse().getId() + org.getId();
      } else {
        return order.getWarehouse().getId() + org.getId();
      }
      // return order.getOrganization().getId();
    } else
      return "";
  }

}
