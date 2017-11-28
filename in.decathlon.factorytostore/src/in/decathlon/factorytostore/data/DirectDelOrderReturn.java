package in.decathlon.factorytostore.data;

import in.decathlon.ibud.orders.data.OrderState;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DirectDelOrderReturn {
  private Map<String, OrderState> orders = new HashMap<String, OrderState>();
  private Map<String, WarehouseCount> warehouseStocks = new HashMap<String, WarehouseCount>();

  public Map<String, OrderState> getOrders() {
    return orders;
  }

  public void setOrders(Map<String, OrderState> orders) {
    this.orders = orders;
  }

  public Map<String, WarehouseCount> getWarehouseStocks() {
    return warehouseStocks;
  }

  public void setWarehouseStocks(Map<String, WarehouseCount> warehouseStocks) {
    this.warehouseStocks = warehouseStocks;
  }

}
