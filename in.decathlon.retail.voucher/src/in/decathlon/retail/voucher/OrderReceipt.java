package in.decathlon.retail.voucher;

import in.decathlon.sales.store.WebPosOrdReportFromWSResponse;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.retail.posterminal.OrderLoaderHook;

public class OrderReceipt implements OrderLoaderHook {

	@Override
	public void exec(JSONObject jsonorder, Order order, ShipmentInOut shipment,
			Invoice invoice) throws Exception {
		
		WebPosOrdReportFromWSResponse posOrderRprt = new WebPosOrdReportFromWSResponse();
		posOrderRprt.posOrderForEmailReport(order.getId());
		
	}

}
