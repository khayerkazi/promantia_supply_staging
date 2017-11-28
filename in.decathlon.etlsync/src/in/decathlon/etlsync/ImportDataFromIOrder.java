package in.decathlon.etlsync;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.dataimport.Order;

import org.openbravo.service.web.WebService;




public class ImportDataFromIOrder implements WebService {
	
	private static final String LOGIN = "Openbravo";
	private static final String PWD = "Op3N8r@v0";

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		// Fetching the records from i_order table where the batch no is NULL with unique receipt numbers
		final OBQuery<Order> obQuery = OBDal.getInstance().createQuery(Order.class, "as o where o.imBatchno is null");
		obQuery.setSelectClause("DISTINCT o.iMEMImReceiptno");
		obQuery.setMaxResult(50);
		
		// for-each to call the second webservice uniquely by fetching the specific records
		for (Object recNo : obQuery.list().toArray()) {
System.err.println((String)recNo);
			final OBQuery<Order> obQuery1 = OBDal.getInstance().createQuery(Order.class, "as o where o.iMEMImReceiptno='"+(String)recNo+"' and o.imBatchno is null");
			
			// Generate random string for batch no
			String batch_no = UUID.randomUUID().toString().replace("-", "").toUpperCase();
			
			StringBuffer sb = new StringBuffer();
 			sb.append("[");
 			
			for (Order order : obQuery1.list()) {
				
				if(order.getBusinessPartner() != null) {

					sb.append("{\"ad_client_id\":\""+order.getClient().getId()+"\",\"ad_org_id\":\""+order.getOrganization().getId()+"\",\"created\":\""+order.getCreationDate()+"\",\"createdby\":\""+order.getCreatedBy().getId()+"\",\"updated\":\""+order.getUpdated()+"\",\"updatedby\":\""+order.getUpdatedBy().getId()+"\",\"issotrx\":\""+order.isSalesTransaction()+"\",\"c_bpartner_id\":\""+order.getBusinessPartner().getId()+"\",\"ad_user_id\":\""+order.getUserContact().getId()+"\",\"phone\":\""+order.getPhone()+"\",\"c_paymentterm_id\":\""+order.getPaymentTerms().getId()+"\",\"documentno\":\""+order.getDocumentNo()+"\",\"description\":\""+order.getDescription()+"\",\"m_product_id\":\""+order.getProduct()+"\",\"c_tax_id\":\""+order.getTax().getId()+"\",\"taxamt\":"+order.getTaxAmount()+",\"linedescription\":\""+order.getLineDescription()+"\",\"qtyordered\":"+order.getOrderedQuantity()+",\"priceactual\":"+order.getUnitPrice()+",\"dateordered\":\""+order.getOrderDate().toString()+"\",\"paymentamount1\":"+order.getPaymentamount1()+",\"paymentamount2\":"+order.getPaymentamount2()+",\"synchronized\":\""+order.isSynchronized()+"\",\"em_im_receiptno\":\""+order.getIMEMImReceiptno()+"\",\"em_im_pcbprice\":"+order.getIMEMImPcbprice()+",\"em_im_pcbqty\":"+order.getIMEMImPcbqty()+",\"em_im_ueprice\":"+order.getIMEMImUeprice()+",\"em_im_ueqty\":"+order.getIMEMImUeqty()+",\"em_im_totalpriceadj\":"+order.getIMEMImTotalpriceadj()+",\"em_im_customersatisfaction\":\""+order.getIMEMImCustomersatisfaction()+"\",\"em_im_posno\":\""+order.getIMEMImPosno()+"\",\"em_im_chargeamt\":"+order.getIMChargeAmt()+"},");
					
				} else {
					// Setting the batch number
					order.setImBatchno(batch_no);
					order.setImportErrorMessage("Customer ID is missing");
					// Updating the i_order records with batch numbers
					OBDal.getInstance().save(order);
				}
			}
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
			
			sb.deleteCharAt((sb.length()-1));
			sb.append("]");
			final String jsonarray = sb.toString();
//			System.err.println(jsonarray);
			if(!jsonarray.equals("]")) {
				// HTTP POST request Parameters
				String urlParameter = "parameter="+jsonarray+"&iOrderReceiptId="+(String)recNo+"";
				
				// Sending HTTP POST request
				final HttpURLConnection hc = createConnection();
				hc.setDoOutput(true);
			    final OutputStream os = hc.getOutputStream();
			    os.write(urlParameter.getBytes("UTF-8"));
			    os.flush();
			    os.close();
				hc.connect();
				
				// Getting the Response from the Web service
				BufferedReader in = new BufferedReader(
				new InputStreamReader(hc.getInputStream()));
				String inputLine;
				StringBuffer resp = new StringBuffer();
		 
				while ((inputLine = in.readLine()) != null) {
					resp.append(inputLine);
				}
				String secondResponse = resp.toString();
				in.close();
			
System.err.println("received");
			
				if(secondResponse.equals("received")) {
					for (Order order : obQuery1.list()) {
						//Setting the batch number
						order.setImBatchno(batch_no);
						
						order.setProcessNow(true);
System.err.println("processing Y");
						// Updating the i_order records with batch numbers
						OBDal.getInstance().save(order);
					}
					OBDal.getInstance().flush();
					OBDal.getInstance().commitAndClose();
				}
			}
		}
System.err.println("The End");
 		
		// writing to the response i.e. 'created' 
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write("created");
		w.close();
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	  /**
	   * Creates a HTTP connection.
	   * 
	   * @param wsPart
	   * @param method
	   *          POST, PUT, GET or DELETE
	   * @return the created connection
	   * @throws Exception
	   */
	  protected HttpURLConnection createConnection() throws Exception {
	    Authenticator.setDefault(new Authenticator() {
	      @Override
	      protected PasswordAuthentication getPasswordAuthentication() {
	        return new PasswordAuthentication(LOGIN, PWD.toCharArray());
	      }
	    });
	    final URL url = new URL("https://erp.decathlon.in/atlas/ws/in.decathlon.etlsync.order");
	    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
	    
	    hc.setRequestMethod("POST");
	    hc.setAllowUserInteraction(false);
	    hc.setDefaultUseCaches(false);
	    hc.setDoInput(true);
	    hc.setInstanceFollowRedirects(true);
	    hc.setUseCaches(false);
		return hc;
	  }

}
