package in.decathlon.supply.oba.webservice;

import java.io.Writer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.service.web.WebService;

import com.sysfore.catalog.CLCanstock;

public class CANStockWS implements WebService {
	
	private static final Logger LOG = Logger.getLogger(CANStockWS.class);
	
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		int records = 0;
		try {

			if (request == null) {
				LOG.info("request is null");
			} else {
				JSONArray array = new JSONArray(request.getParameter("parameter"));
				records = addtoDatabase(array);
			}
			response.setContentType("application/json");
			final Writer w = response.getWriter();
			w.write("Response: records updated =" + records);
			w.close();
		} catch (final Exception e) {
			LOG.error("Error in executing web service", e);
		}
	}

	private int addtoDatabase(JSONArray array) {
		
		int i = 0;
		int j = 0;
		try {
			JSONObject itemobj = null;
			JSONObject itemDetails = null;
			JSONObject job = null;
			
			OBContext.setAdminMode();
			for (i = 0; i < array.length(); i++) {
				
				itemobj = array.getJSONObject(i);
				
				
				final OBCriteria<CLCanstock> clCanStockObQuery = OBDal.getInstance().createCriteria(CLCanstock.class);
				clCanStockObQuery.add(Restrictions.eq(CLCanstock.PROPERTY_ITEMCODE, getAbsolute(itemobj.getString("itemCode"))));
				if(clCanStockObQuery.count() > 0) {
					
					final CLCanstock clCanstoc = clCanStockObQuery.list().get(0);
					clCanstoc.setItemCode(getAbsolute(itemobj.getString("itemCode")));
					clCanstoc.setCACQty(getAbsolute(itemobj.getString("cacPCEQty")));
					if(!"".equals(getNotNull(itemobj.getString("cacNextDeliveryDate")))) {
						long milliSeconds = Long.parseLong(getNotNull(itemobj.getString("cacNextDeliveryDate")));
						final Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis(milliSeconds);
						clCanstoc.setCacnextpromiseddeliverydate(calendar.getTime());
					}
					if(!"".equals(getAbsolute(itemobj.getString("cacNextDeliveryPCEQty")))) {
						clCanstoc.setCACNextAvailableQty(getAbsolute(itemobj.getString("cacNextDeliveryPCEQty")));
					}
					clCanstoc.setUpdated(new Timestamp(new Date().getTime()));
					
					OBDal.getInstance().save(clCanstoc);
					
				} else {
				
					final CLCanstock clCanstoc = new CLCanstock(); 
					clCanstoc.setItemCode(getAbsolute(itemobj.getString("itemCode")));
					clCanstoc.setCACQty(getAbsolute(itemobj.getString("cacPCEQty")));
					if(!"".equals(getNotNull(itemobj.getString("cacNextDeliveryDate")))) {
						long milliSeconds = Long.parseLong(getNotNull(itemobj.getString("cacNextDeliveryDate")));
						final Calendar calendar = Calendar.getInstance();
						calendar.setTimeInMillis(milliSeconds);
						clCanstoc.setCacnextpromiseddeliverydate(calendar.getTime());
					}
					if(!"".equals(getAbsolute(itemobj.getString("cacNextDeliveryPCEQty")))) {
						clCanstoc.setCACNextAvailableQty(getAbsolute(itemobj.getString("cacNextDeliveryPCEQty")));
					}
					clCanstoc.setUpdated(new Timestamp(new Date().getTime()));
					
					OBDal.getInstance().save(clCanstoc);
				}
				
			}
			
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
			OBContext.restorePreviousMode();
		} catch (JSONException e) {
			LOG.error("Error in updating JSOn data to database", e);
		}
		return j;
	}
	
	private String getAbsolute(String string) {
		if (string.equals("null")) {
			return "0";
		} else
			return string;
	}

	private String getNotNull(String string) {
		if (string.equals("null")) {
			return "";
		} else
			return string;
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

}
