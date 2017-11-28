package in.decathlon.factorytostore.data;

import java.util.HashMap;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class WarehouseCount {

  private HashMap<String, StockCount> stockCounts = new HashMap<String, StockCount>();

  public HashMap<String, StockCount> getStockCounts() {
    return stockCounts;
  }

  public void setStockCounts(HashMap<String, StockCount> stockCounts) {
    this.stockCounts = stockCounts;
  }

  public void set(String id, StockCount stockCount) {
    // HashMap<String, StockCount> stockCounts = this.getStockCounts();
    Set<String> stockCountsKeys = this.getStockCounts().keySet();
    if (stockCountsKeys.contains(id)) {
      this.getStockCounts().put(id, stockCount);
    }
  }

}
