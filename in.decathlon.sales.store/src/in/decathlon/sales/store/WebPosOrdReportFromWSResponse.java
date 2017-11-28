package in.decathlon.sales.store;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.sales.store.data.DcssPendingDocumentMail;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;

public class WebPosOrdReportFromWSResponse {

	  private static final Logger logger = Logger.getLogger(WebPosOrdReportFromWSResponse.class);
	  String reportLocation = null;
      String  searchParams = "";
	  JSONObject errorObject = new JSONObject();

	public void posOrderForEmailReport(String orderId) throws JSONException {

		Order ord = OBDal.getInstance().get(Order.class, orderId);
		 Boolean salesTxn = ord.isSalesTransaction();
	     Boolean orgStore = ord.getOrganization().isSWIsstore();
	     String mobileNo = "";
	     String email = "";
	     String landNO ="";
	     String decathlonId = "";
	     JSONArray response = new JSONArray();
         JSONObject obj = new JSONObject();
	     
	     if (salesTxn.equals(true) && orgStore.equals(true)) {
	    	 
	    	 decathlonId = ord.getRCOxylaneno();
	    	if(!decathlonId.equals("")) {
	    		 try {
	    			    obj = sendGetRequest(mobileNo,email,landNO,decathlonId);
						JSONArray jObj = obj.getJSONObject("response").getJSONArray("data");
				        JSONObject jObjData = new JSONObject(jObj.getString(0));
						String rcNotify = jObjData.get("rCNotify").toString();
					    String bpName = jObjData.get("name").toString();
			            email = ord.getSyncEmail();
                        CreateEmailReceipt(rcNotify,email,bpName,ord);
					   } catch (Exception e) {
						//e.printStackTrace();
				        BusinessEntityMapper.createErrorLogRecord(e, null, errorObject);

					}
	    	}
	    	else{
	         landNO = ord.getSyncLandline();
	         mobileNo = ord.getRCMobileNo();
             email = ord.getSyncEmail();
           
             if(!mobileNo.equals("") || !email.equals("") || !landNO.equals("") ){
            	 try {
                    obj = sendGetRequest(mobileNo,email,landNO,decathlonId);
					JSONArray jObj = obj.getJSONObject("response").getJSONArray("data");
					if(jObj.length() == 0){
						
					}
					else{
			        JSONObject jObjData = new JSONObject(jObj.getString(0));
					String rcNotify = jObjData.get("rCNotify").toString();
				    String bpName = jObjData.get("name").toString();
				    CreateEmailReceipt(rcNotify,email,bpName,ord);
					}
				   } catch (Exception e) {
					   errorObject.put(SOConstants.RECORD_ID, ord.getId());
					   errorObject.put(SOConstants.recordIdentifier, ord.getId());
					   errorObject.put(SOConstants.TABLE_NAME, "");
				      BusinessEntityMapper.createErrorLogRecord(e, null, errorObject);
				      //e.printStackTrace();
				}
             }
	    	}
             
	     }

	}
	
	private void CreateEmailReceipt(String rCNotify, String  email, String bpName, Order ord) throws JRException, JSONException {
		
		 if (rCNotify.equals("Y") && !email.equals("")) {
            try{
    			

             final DcssPendingDocumentMail dpdmail = OBProvider.getInstance().get(
	              DcssPendingDocumentMail.class);
	          dpdmail.setNewOBObject(true);
	          dpdmail.setSalesOrder(ord);
	          dpdmail.setClient(ord.getClient());
	          dpdmail.setCreatedBy(ord.getCreatedBy());
	          dpdmail.setUpdatedBy(ord.getUpdatedBy());
	          dpdmail.setOrganization(ord.getOrganization());
	          dpdmail.setCreationDate(new Date());
	          dpdmail.setUpdated(new Date());
	          dpdmail.setActive(true);
	          //dpdmail.setFailureMsg("Sorry");
	          dpdmail.setDocumentType(ord.getTransactionDocument());
	          dpdmail.setAlertStatus("N");
	          dpdmail.setCustomerName(bpName == null || bpName.equals("") ? "Anonymous Customer"
	              : bpName);
	          dpdmail.setEmail(email);
	          OBDal.getInstance().save(dpdmail);
	         if (ord.getOrderLineList() != null && ord.getOrderLineList().size() > 0) {
	        	 String reportName = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
	        	          "source.path")
	        	          + "/modules/in.decathlon.sales.store/src/in/decathlon/sales/store/ad_reports/WebPosReceipt.jrxml";
	        	      
	        	      String baseDes = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
	        	              "source.path")
	        	              + "/modules/in.decathlon.sales.store/src";
	        	      String subReportLoc = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	        	          .getProperty("source.path")
	        	          + "/modules/in.decathlon.sales.store/src/in/decathlon/sales/store/ad_reports/";
	        	      JasperReport jasperReport = JasperCompileManager.compileReport(reportName);
	        	      Map<String, Object> params = new HashMap<String, Object>();
	        	      
	        	      String webBes = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
	        	          "source.path")
	        	          + "/modules/in.decathlon.sales.store/web";
	        	      Connection co = OBDal.getInstance().getConnection();
	        	      params.put("DOCUMENT_ID", ord.getId());
	        	      params.put("CUST_NAME", bpName);
	        	      params.put("outputtype", "pdf");
	        	      params.put("BASE_DESIGN", baseDes);
	        	      params.put("BASE_WEB", webBes);
	        	      params.put("SUBREPORT_DIR", subReportLoc);
	        	      params.put("REPORT_CONNECTION", co);
	        	      String docno = ord.getDocumentNo();
	        	      int pos = docno.indexOf("*", 1);
	        	      String soDocNum = docno.substring(pos + 1, docno.length());
	        	      String docNo = soDocNum.split("\\/")[1];
	        	      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, co);
	        	      JasperExportManager.exportReportToPdfFile(jasperPrint, OBPropertiesProvider.getInstance()
	        	          .getOpenbravoProperties().getProperty("attach.path")
	        	          + "/Receipt"+ docNo  + ".pdf");
	          }
		 
		 }catch(Exception e){
			  errorObject.put(SOConstants.RECORD_ID, ord.getId());
		      errorObject.put(SOConstants.recordIdentifier, ord.getId());
		      errorObject.put(SOConstants.TABLE_NAME, "");
		      BusinessEntityMapper.createErrorLogRecord(e, null, errorObject);

		 }
	        }
	}

	@SuppressWarnings("static-access")
	  private JSONObject sendGetRequest(String mobileNo,String email, String landlineNo,String decathlonId) throws Exception {
	    JSONObject obj = new JSONObject();
	    IbudConfig config = new IbudConfig();
	    String url = "";
	    try {
          
	      String webPosURL = config.getWebPosUrl();
	      final String userName = config.getPosUsername();
	      final String password = config.getPosPassword();
			
	      if (!mobileNo.equals("")) {
				url = webPosURL + "?username=" + userName + "&pwd=" + password + "&mobileno=" + mobileNo;
			} else if (!email.equals("")) {

				url = webPosURL + "?username=" + userName + "&pwd=" + password + "&email=" + email;

			} else if (!landlineNo.equals("")) {

				url = webPosURL + "?username=" + userName + "&pwd=" + password + "&landline=" + landlineNo;

			}
			else if(!decathlonId.equals("")){
			      url = webPosURL + "?username=" + userName + "&pwd=" + password + "&decathlonId=" + decathlonId;

			}
	      Authenticator.setDefault(new Authenticator() {
	        @Override
	        protected PasswordAuthentication getPasswordAuthentication() {
	          return new PasswordAuthentication(userName, password.toCharArray());
	        }
	      });

	      DefaultHttpClient httpclient = new DefaultHttpClient();

	      httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
	          new UsernamePasswordCredentials(userName, password));

	      HttpGet httpGet = new HttpGet(url);

	      HttpResponse response = httpclient.execute(httpGet);

	      try {
	        String output = EntityUtils.toString(response.getEntity());
	        obj = new JSONObject(output);

	      } catch (Exception e) {
	        throw e;
	      }
	    } catch (Exception e) {
	      throw new Exception(e.toString());
	    }
	    return obj;
	  }
	
	
	
}
