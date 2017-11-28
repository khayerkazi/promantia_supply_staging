package in.decathlon.ibud.orders.data;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderState {
	private String poRef = null;
	private int nbSo = 0;
	private Map<String, OrderLineState> lines = new HashMap<String, OrderLineState>();

	public String getPoRef() {
		return poRef;
	}

	public void setPoRef(String poRef) {
		this.poRef = poRef;
	}

	public int getNbSo() {
		return nbSo;
	}

	public void setNbSo(int nbSo) {
		this.nbSo = nbSo;
	}

	public Map<String, OrderLineState> getLines() {
		return lines;
	}

	public void setLines(Map<String, OrderLineState> lines) {
		this.lines = lines;
	}
}
