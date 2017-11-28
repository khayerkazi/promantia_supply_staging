package in.decathlon.supply.dc;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;

public class SubStoreReplenishmentWFS implements WebService {

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		String orderId = request.getParameter("orderId");
		System.out.println("new WS "+orderId);
		
		
		// Calling Stored Procedure  ecom_order_post3
		final List parameters = new ArrayList();
		parameters.add(orderId);
	    final String procedureName = "idsd_dc_post1";System.out.println("procedure "+orderId);
	    try {
		    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
		    	System.out.println("Procedure called successfully for the PO "+orderId);
	    } catch (Exception e3) {
			e3.printStackTrace();
		}
	    final String res = "received";
		// write to the response
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(res);
		w.close();
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

}
