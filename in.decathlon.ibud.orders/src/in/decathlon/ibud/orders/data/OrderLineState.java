package in.decathlon.ibud.orders.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderLineState {
	private int confirmedQty = 0;

	public int getConfirmedQty() {
		return confirmedQty;
	}

	public void setConfirmedQty(int confirmedQty) {
		this.confirmedQty = confirmedQty;
	}

	public void incConfirmedQty(int orderedQuantity) {
		confirmedQty += orderedQuantity;

	}
}
