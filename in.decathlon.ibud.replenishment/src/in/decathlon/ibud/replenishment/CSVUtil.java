package in.decathlon.ibud.replenishment;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.commons.IbudConfig;
import in.decathlon.supply.dc.util.SuppyDCUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;

import au.com.bytecode.opencsv.CSVWriter;

import com.sysfore.catalog.CLMinmax;

public class CSVUtil {
  Logger log = Logger.getLogger(CSVUtil.class);
  static IbudConfig config = new IbudConfig();
	static String csv = IbudConfig.getcsvFile();
  final static Properties p = SuppyDCUtil.getInstance().getProperties();
  Connection conn = null;
  String cwhWarehouseId = "962B547DC7D9431EABDE91E3A9BCFFF6";
  String rwhWarehouseId = "67951CEE618E42E99F4D97C24534CDC1";
  String hubWarehouseId = "57E2E0F65EDE4344B4DDB1BEB54D0589";

  // String wsName = "in.decathlon.ibud.orders.OrdersWS";

  public void writeDataToCsv(Organization org, List<String[]> repData) throws IOException {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date currentDate = new Date();
    final String date = format.format(currentDate);
    String csvFile = csv + org.getName() + "_" + date + ".csv";
    final File file = new File(csvFile);
    CSVWriter writer = new CSVWriter(new FileWriter(file.getAbsoluteFile(), true));

    // if file doesnt exists, then create it
    if (!file.exists()) {
      file.createNewFile();
    }
    writer.writeAll(repData);

		writer.close();
  }

  public List<String[]> getHeader() {
    List<String[]> data = new ArrayList<String[]>();

    data.add(new String[] { "Store Department", "Item Code", "Model Name", "Display Min", "Min",
        "Max", "UE", "PCB", "Logistic Recharge", "Store Stock", "CWH Stock", "RWH Stock",
        "Hub Stock", "Required Quantity", "Open Order", "Order Generated", "Pcb picking" });
    return data;

  }

  public List<String[]> createCsvData(Order orderHeader) {
    List<String[]> data = new ArrayList<String[]>();
    for (OrderLine ol : orderHeader.getOrderLineList()) {
      Product prd = ol.getProduct();
      List<String> carCacStocks = setCarCacStock(prd,true);
      List<String> minMaxDisp = getMinMaxDisplayQty(prd, orderHeader.getOrganization());
      String storeStock = getQtyOnStock(orderHeader.getOrganization(), prd);
      String minQty = minMaxDisp.get(0);
      String maxQty = minMaxDisp.get(1);
      String dispMinMax = minMaxDisp.get(2);
      String rwhStock = carCacStocks.get(0);
      String cwhStock = carCacStocks.get(1);
      String hubStock = carCacStocks.get(2);

      String isPcb = setisPcb(prd, orderHeader.getOrganization());
      String openOrder = getOpenOrder(orderHeader.getOrganization(), prd);
      data.add(new String[] { orderHeader.getClStoredept().getName(), prd.getName(),
          prd.getClModel().getModelName(), dispMinMax, minQty, maxQty,
          prd.getClUeQty() != null ? prd.getClUeQty().toString() : "0",
          prd.getClPcbQty() != null ? prd.getClPcbQty().toString() : "0",
          prd.getClLogRec() != null ? prd.getClLogRec() : "", storeStock, cwhStock, rwhStock,
          hubStock, ol.getOrderedQuantity().toString(),openOrder, ol.getSwConfirmedqty().toString(),
           isPcb });
    }
    return data;

  }

  public String getQtyOnStock(Organization organization, Product prd) {
    OrgWarehouse orgWarehouse = BusinessEntityMapper.getOrgWarehouse(organization.getId());
    String warehouseId = orgWarehouse.getWarehouse().getId();
    return BusinessEntityMapper.getStockOnWarehouse(prd.getId(), warehouseId).toString();
  }

  public String getOpenOrder(Organization organization, Product prd) {
    BigDecimal confirmedQty = BigDecimal.ZERO;
    try {
      // OrderLine is used because from clause is required 
      String query = "select coalesce(ibdrep_get_qty_on_order('"+prd.getId()+"','"+organization.getId()+"'),0)  from OrderLine ";
      org.hibernate.Query qryCrit = OBDal.getInstance().getSession().createQuery(query);
      qryCrit.setMaxResults(1);
      
      List<Object> qryResult = qryCrit.list();
      if (qryResult != null && qryResult.size() > 0) {
        for (Object row : qryResult) {
          confirmedQty = (BigDecimal) row;
        }
        //confirmedQty = confirmedQty.subtract(new BigDecimal(confmdQty));
      } 
    } catch (Exception e) {
     e.printStackTrace();
    }

    return confirmedQty.toString();
  }

  @SuppressWarnings("unchecked")
  public List<String> setCarCacStock(Product prd, boolean sendToSupply) {
    List<String> stocks = new ArrayList<String>();
    if (sendToSupply) {
		try {
			JSONArray responseObj = sendGetrequest("in.decathlon.ibud.replenishment.ibudStockWS?product="
					+ prd.getId()
					+ "&warehouse="
					+ rwhWarehouseId
					+ ","
					+ cwhWarehouseId + "," + hubWarehouseId);
			JSONObject obj = responseObj.getJSONObject(0);
			Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (key.contains(rwhWarehouseId))
					stocks.add(obj.getString(key));
				else if (key.contains(cwhWarehouseId))
					stocks.add(obj.getString(key));
				else if (key.contains(hubWarehouseId))
					stocks.add(obj.getString(key));
			}
		} catch (Exception exc) {
			stocks.add("0");
			stocks.add("0");
			stocks.add("0");
		}
	}
    else{
    	stocks.add("NA");
		stocks.add("NA");
		stocks.add("NA");
    }
	return stocks;
  }

  public List<String> getMinMaxDisplayQty(Product prd, Organization organization) {
    List<String> clMinMax = new ArrayList<String>();
    OBCriteria<CLMinmax> minMaxCrit = OBDal.getInstance().createCriteria(CLMinmax.class);
    minMaxCrit.add(Restrictions.eq(CLMinmax.PROPERTY_ORGANIZATION, organization));
    minMaxCrit.add(Restrictions.eq(CLMinmax.PROPERTY_PRODUCT, prd));
    minMaxCrit.setMaxResults(1);
    List<CLMinmax> minMaxList = minMaxCrit.list();
    if (minMaxList != null && minMaxList.size() > 0) {
      clMinMax.add(minMaxList.get(0).getMinQty().toString());
      clMinMax.add(minMaxList.get(0).getMaxQty().toString());
      clMinMax.add(minMaxList.get(0).getDisplaymin().toString());
    } else {
      clMinMax.add("notFOund");
      clMinMax.add("notFOund");
      clMinMax.add("notFOund");
    }
    return clMinMax;

  }

  @SuppressWarnings("static-access")
  public String setisPcb(Product prd, Organization organization) {
    String thresholdValue = "";
    try {
      OBContext.getOBContext().setAdminMode();
      thresholdValue = prd.getIdsdOxylaneProdcat() == null ? "" : prd.getIdsdOxylaneProdcat()
          .getSearchKey();
    } catch (Exception e) {
    } finally {
      OBContext.getOBContext().restorePreviousMode();
    }

    String qry = "id in (select id from CL_Minmax cl where cl.organization.id = '"
        + organization.getId() + "' and cl.product.id='" + prd.getId()
        + "' and cl.idsdPcbThreshold is not null)";
    OBQuery<CLMinmax> minmaxQry = OBDal.getInstance().createQuery(CLMinmax.class, qry);
    minmaxQry.setMaxResult(1);

    if (minmaxQry.count() > 0) {
      return "true";
    } else if (!thresholdValue.equals("")) {
      return "true";
    } else
      return "false";
  }

  @SuppressWarnings("static-access")
  public JSONArray sendGetrequest(String wsName) throws Exception {

    JSONArray obj = new JSONArray();
    String url = "";
    try {
      String host = "";
      int port = 0;

      final String userName = config.getSupplyUsername();
      final String password = config.getSupplyPassword();
      host = config.getSupplyHost();
      port = config.getSupplyPort();
      final String context = config.getSupplyContext().concat("/ws/");

      url = "http://" + host + ":" + port + "/" + context + wsName;

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
        obj = new JSONArray(output);

      } catch (Exception e) {
        throw e;
      }
    } catch (Exception exc) {
      throw exc;
    }

    return obj;

  }

}
