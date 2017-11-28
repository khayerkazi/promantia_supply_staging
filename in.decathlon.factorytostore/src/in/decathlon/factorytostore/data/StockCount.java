package in.decathlon.factorytostore.data;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class StockCount {

  private BigDecimal physicalStock;
  private BigDecimal reservedStock;
  private BigDecimal availableStock;

  public BigDecimal getPhysicalStock() {
    return physicalStock;
  }

  public void setPhysicalStock(BigDecimal physicalStock) {
    this.physicalStock = physicalStock;
  }

  public BigDecimal getReservedStock() {
    return reservedStock;
  }

  public void setReservedStock(BigDecimal reservedStock) {
    this.reservedStock = reservedStock;
  }

  public BigDecimal getAvailableStock() {
    return availableStock;
  }

  public void setAvailableStock(BigDecimal availableStock) {
    this.availableStock = availableStock;
  }

}
