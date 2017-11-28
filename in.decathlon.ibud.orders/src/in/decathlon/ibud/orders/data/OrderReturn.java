package in.decathlon.ibud.orders.data;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class OrderReturn {
	private Map<String, OrderState> orders = new HashMap<String, OrderState>();
	private Map<String, StockState> stocks = new HashMap<String, StockState>();


	public Map<String, StockState> getStocks() {
		return stocks;
	}
	public void setStocks(Map<String, StockState> stoks) {
		this.stocks = stoks;
	}

	public Map<String, OrderState> getOrders() {
		return orders;
	}

	public void setOrders(Map<String, OrderState> orders) {
		this.orders = orders;
	}
}
