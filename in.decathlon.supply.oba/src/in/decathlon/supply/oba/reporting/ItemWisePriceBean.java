package in.decathlon.supply.oba.reporting;

import java.io.Serializable;

public class ItemWisePriceBean implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;
	
	// declarations
	private Long invoicedQuantity;
	private Double turnoverIncludingTax;
	private Double cashierMargin;
	private Double turnoverExcludingTax;
	private Double sellingPriceDiscrepancy;
	
	// getter and setter methods
	public Long getInvoicedQuantity() {
		return invoicedQuantity;
	}
	public void setInvoicedQuantity(Long invoicedQuantity) {
		this.invoicedQuantity = invoicedQuantity;
	}
	public Double getTurnoverIncludingTax() {
		return turnoverIncludingTax;
	}
	public void setTurnoverIncludingTax(Double turnoverIncludingTax) {
		this.turnoverIncludingTax = turnoverIncludingTax;
	}
	public Double getCashierMargin() {
		return cashierMargin;
	}
	public void setCashierMargin(Double cashierMargin) {
		this.cashierMargin = cashierMargin;
	}
	public Double getTurnoverExcludingTax() {
		return turnoverExcludingTax;
	}
	public void setTurnoverExcludingTax(Double turnoverExcludingTax) {
		this.turnoverExcludingTax = turnoverExcludingTax;
	}
	public Double getSellingPriceDiscrepancy() {
		return sellingPriceDiscrepancy;
	}
	public void setSellingPriceDiscrepancy(Double sellingPriceDiscrepancy) {
		this.sellingPriceDiscrepancy = sellingPriceDiscrepancy;
	}
	
}
