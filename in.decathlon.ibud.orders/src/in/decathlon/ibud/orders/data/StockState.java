package in.decathlon.ibud.orders.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class StockState {
	private int carStock = 0;
	private int cacStock = 0;
	private int hubStock = 0;

	public int getCarStock() {
		return carStock;
	}

	public void setCarStock(int carStock) {
		this.carStock = carStock;
	}

	public int getCacStock() {
		return cacStock;
	}

	public void setCacStock(int cacStock) {
		this.cacStock = cacStock;
	}

	public int getHubStock() {
		return hubStock;
	}

	public void setHubStock(int hubStock) {
		this.hubStock = hubStock;
	}
}
