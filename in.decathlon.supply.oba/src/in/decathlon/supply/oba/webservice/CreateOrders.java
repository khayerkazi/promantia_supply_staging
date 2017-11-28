package in.decathlon.supply.oba.webservice;

import java.io.Writer;
import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;

public class CreateOrders implements WebService {

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		log4j.debug("in do get");
	      response.setContentType("application/json");
	      final Writer w = response.getWriter();
	      w.write("Response from do get");
	      w.close();

	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	  String orderid=null;
	  log4j.debug("Web service call CreateOrders -->");
		    try {
		    	JSONObject obj= new JSONObject(request.getParameter("data"));
		        if (obj != null) {
		        	orderid=addtoDatabase(obj);
		      }
		    } catch (final Exception e) {
		      log4j.error("Error in executing web service",e);
		    }
		    finally{
			      response.setContentType("application/json");
			      final Writer w = response.getWriter();
			      w.write(orderid);
			      w.close();
		    }
	

	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}
	
	public static Logger log4j= Logger.getLogger(CreateOrders.class);	
	
    private String addtoDatabase(JSONObject obj) {
    	boolean status=false;
        String orderid=null;
	    try {
	        JSONArray linearray = null;        
	        JSONObject headerobj=obj.getJSONObject("header");
	        linearray =obj.getJSONArray("lines");
	        orderid=createHeader(headerobj);
	        if(null!=orderid){
	        	status=createLines(orderid,linearray,headerobj);
	        }
	        else{
	        	log4j.error("Error : order not created");
	        }
	        log4j.debug("order : "+orderid +"line creation status : "+status);

	    } catch (JSONException e) {
	    	log4j.error("Error in updating JSOn data to database",e);
	    }
	    return orderid;

	  }

	private boolean createLines(String orderid, JSONArray linearray, JSONObject headerobj) throws JSONException {
		boolean status=false;
		JSONObject job=null;
		
		String uom = "100";
		String tax = "7730F9AADA574981A8C84D884AD23FD2";
		int lineNo=0;
		BigDecimal zero=new BigDecimal(0);
		
		for(int i=0;i<linearray.length();i++){
			BusinessPartner businesspartner = OBDal.getInstance().get(BusinessPartner.class, headerobj.getString("businesspartner"));
			JSONObject lineobj= linearray.getJSONObject(i);
			
			job = new JSONObject("{\"_entityName\":\"OrderLine\"," 
					+ "\"organization\":\""+ headerobj.getString("organization") 
					+ "\",\"businessPartner\":\"" + headerobj.getString("businesspartner")
					+ "\",\"partnerAddress\":\"" + headerobj.getString("partnerAddress")
					+ "\",\"warehouse\":\"" + headerobj.getString("warehouse")
					+ "\",\"currency\":\"" + headerobj.getString("currency")
					+ "\",\"orderDate\":\"" + headerobj.getString("orderDate")
					+ "\",\"swFob\":\"" + lineobj.getString("costplus")
					+ "\",\"sWEMSwSuppliercode\":\"" + businesspartner.getClSupplierno()					
					+ "\",\"product\":\"" + lineobj.getString("product_id")
					+ "\",\"uOM\":\"" + uom
					+ "\",\"tax\":\"" + tax
					+ "\",\"salesOrder\":\"" + orderid
					+ "\",\"orderedQuantity\":" + Double.parseDouble(getAbsolute(lineobj.getString("qty")))
					+ ",\"lineNetAmount\":" + Double.parseDouble(getAbsolute(lineobj.getString("linenetamt")))
					+ ",\"lineNo\":" + lineNo
					+ ",\"priceLimit\":" + zero
					+ ",\"chargeAmount\":" + zero
					+ ",\"freightAmount\":" + zero
					+ "}");
				   		
				final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(
				    JsonToDataConverter.class);
				// Converting JSON Object to openbravo Business object BaseOBObject bob =
				try {
				  BaseOBObject bob = toBaseOBObject.toBaseOBObject(job);
				  // Database Connections and Database insertions
				  OBDal.getInstance().save(bob);
				  OBDal.getInstance().flush();       
				
				  lineNo+=10;
				} catch (Exception e) {
				  log4j.error("Error in converting json to baseobject",e);
				}
		     OBDal.getInstance().commitAndClose();
		     log4j.debug("added to database");
		}
		
		return status;
	}

	private String createHeader(JSONObject headerobj) throws JSONException {
		JSONObject job;
		String orderid=null;
		job = new JSONObject("{\"_entityName\":\"Order\"," 
			+ "\"organization\":\""+ headerobj.getString("organization") 
			+ "\",\"salesTransaction\":\""+ headerobj.getString("salesTransaction") 
			+ "\",\"documentStatus\":\""+ headerobj.getString("documentStatus") 
			+ "\",\"documentAction\":\""    + headerobj.getString("documentAction") 
			+ "\",\"documentType\":\"" + headerobj.getString("documentType")
			+ "\",\"transactionDocument\":\"" + headerobj.getString("transactionDocument") 
			+ "\",\"description\":\"" + headerobj.getString("description")
			+ "\",\"paymentTerms\":\"" + headerobj.getString("paymentterm")
			+ "\",\"warehouse\":\"" + headerobj.getString("warehouse")
			+ "\",\"priceList\":\"" + headerobj.getString("pricelist")
			+ "\",\"businessPartner\":\"" + headerobj.getString("businesspartner")
			+ "\",\"partnerAddress\":\"" + headerobj.getString("partnerAddress")
			+ "\",\"invoiceAddress\":\"" + headerobj.getString("invoiceAddress")
			+ "\",\"orderDate\":\"" + headerobj.getString("orderDate")
			+ "\",\"scheduledDeliveryDate\":\"" + headerobj.getString("scheduledDeliveryDate")
			+ "\",\"sWEMSwExpdeldate\":\"" + headerobj.getString("cdDate")
			+ "\",\"sWEMSwEstshipdate\":\"" + headerobj.getString("edDate")
			+ "\",\"accountingDate\":\"" + headerobj.getString("accountingDate")
			+ "\",\"currency\":\"" + headerobj.getString("currency")	
			+ "\",\"sWEMSwCurrency\":\"" + headerobj.getString("currency")
			+ "\",\"sWEMSwBrand\":\"" + headerobj.getString("brand")
			+ "\",\"sWEMSwModelcode\":\"" + headerobj.getString("modelcode")
			+ "\",\"sWEMSwModelname\":\"" + headerobj.getString("modelname")
			+ "\",\"swDept\":\"" + headerobj.getString("department")			
			+ "\",\"sWEMSwPostatus\":\"" + headerobj.getString("documentStatus")
			+ "\"" + "}");
		
		final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(
		    JsonToDataConverter.class);
		// Converting JSON Object to openbravo Business object BaseOBObject bob =
		try {
		  BaseOBObject bob = toBaseOBObject.toBaseOBObject(job);
		  // Database Connections and Database insertions
		  OBDal.getInstance().save(bob);
		  OBDal.getInstance().flush();       
		  orderid=(String) bob.getId();
		} catch (Exception e) {
     		  log4j.error("Error in converting json to baseobject",e);
		}
     OBDal.getInstance().commitAndClose();
     log4j.debug("added to database");
	return orderid;
	
	}
    
	private String getAbsolute(String string) {
    	if(string.equals("null")){
    		return "0";
    	}
    	else 
    		return string;
    }

}
