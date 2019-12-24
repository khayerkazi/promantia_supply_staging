package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.masters.data.IbudServerTime;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;

public class CommonServiceProvider {
  private static Logger log = Logger.getLogger(CommonServiceProvider.class);

  public static String generateToken(HashMap<String, String> configMap) throws Exception {

    HttpURLConnection HttpUrlConnection = null;
    BufferedReader reader = null;
    String tokenString = null;
    String tokenType = null;
    OutputStreamWriter wr = null;
    InputStream is = null;
    try {
      String tokenUrl = configMap.get("tokenUrl");
      String basic_auth = configMap.get("token_authKey");
      String grant_type = configMap.get("tokenGrantType");

      URL urlObj = new URL(tokenUrl);
      HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
      HttpUrlConnection.setRequestMethod("POST");
      HttpUrlConnection.setDoOutput(true);
      HttpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      HttpUrlConnection.setRequestProperty("Authorization", "Basic " + basic_auth);

      String data = URLEncoder.encode("grant_type", "UTF-8") + "="
          + URLEncoder.encode(grant_type, "UTF-8");

      HttpUrlConnection.connect();
      wr = new OutputStreamWriter(HttpUrlConnection.getOutputStream());
      wr.write(data);
      wr.flush();

      if (HttpUrlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        tokenString = "Error_TokenGenerator in response from Token API Generator, Response:"
            + HttpUrlConnection.getResponseCode();
      } else {
        is = HttpUrlConnection.getInputStream();
        reader = new BufferedReader(new InputStreamReader((is)));
        String tmpStr = null;
        String result = null;
        while ((tmpStr = reader.readLine()) != null) {
          result = tmpStr;
        }
        JSONObject responseJson = new JSONObject(result);
        tokenString = responseJson.getString("access_token");
        tokenType = responseJson.getString("token_type");
        if (tokenType == null || (!tokenType.equals("Bearer"))) {
          tokenString = "Error_TokenGenerator TokenType null or invalid";
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error_TokenGenerator in fetching token from API ");
      tokenString = "Error_TokenGenerator in fetching token from API";
    } finally {
      if (is != null) {
        is.close();
      }
      if (wr != null) {
        wr.close();
      }
      if (HttpUrlConnection != null) {
        HttpUrlConnection.disconnect();
      }
    }
    return tokenString;

  }

  public static IbudServerTime getIbudUpdatedTime(String serviceKey) {

    OBContext.setAdminMode(true);
    OBCriteria<IbudServerTime> ibudServerTimeCriteria = OBDal.getInstance().createCriteria(
        IbudServerTime.class);
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, serviceKey));
    ibudServerTimeCriteria.setMaxResults(1);
    ibudServerTimeCriteria.setFilterOnReadableClients(false);
    ibudServerTimeCriteria.setFilterOnReadableOrganization(false);
    List<IbudServerTime> ibudServerTimeList = ibudServerTimeCriteria.list();

    if (ibudServerTimeList != null && ibudServerTimeList.size() > 0) {
      log.debug("Time taken from database ibudServerTimeList.get(0).getLastupdated() "
          + ibudServerTimeList.get(0).getLastupdated());
      return ibudServerTimeList.get(0);
    } else {
      log.debug("No record found for the Service " + serviceKey
          + ". New record need to insert !!! ");

      IbudServerTime newService = OBProvider.getInstance().get(IbudServerTime.class);
      newService.setActive(true);
      Client client = OBDal.getInstance().get(Client.class, "187D8FC945A5481CB41B3EE767F80DBB");
      newService.setClient(client);
      newService.setOrganization(OBContext.getOBContext().getCurrentOrganization());

      newService.setCreatedBy(OBContext.getOBContext().getUser());
      newService.setCreationDate(new Date());
      newService.setUpdatedBy(OBContext.getOBContext().getUser());
      newService.setUpdated(new Date());
      newService.setServiceKey(serviceKey);
      newService.setNewOBObject(true);

      Date d = new Date();
      Date dateBefore = new Date(d.getTime() - 2 * 24 * 3600 * 1000);
      newService.setLastupdated(dateBefore);
      newService.setServiceKey(serviceKey);
      OBDal.getInstance().save(newService);
      SessionHandler.getInstance().commitAndClose();
      return newService;
    }

  }

  public static HashMap<String, String> checkObConfig() throws Exception {

    List<String> errorListObj = new ArrayList<String>();
    HashMap<String, String> outPut = new HashMap<String, String>();

    String tokenUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.url");
    String token_authKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.basic_auth");
    String tokenGrantType = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.token.grant_type");

    String postOrder_Url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.url");
    String order_XEnv = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.x_env");

    String postOrder_customerKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.customerKey");
    String postOrder_customerId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.customerId");
    String postOrder_orderType = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.orderType");
    String postOrder_orderStatus = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.orderStatus");
    String postOrder_origin = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.origin");

    String postOrder_deliveryKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.deliveryKey");
    String postOrder_deliveryId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.deliveryId");

    String postOrder_supplierKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.supplierKey");

    String postOrder_requestMethod = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.requestMethod");

    String postOrder_contentType = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.postorder.contentType");

    String order_acceptVersion = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.acceptVersion");

    String order_authorization = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.authorization");

    String getOrder_url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.getorder.url");

    String getOrderLine_url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.getorderLine.url");

    String getElpProduct_url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.getElpProduct.url");

    String deleteOrder_url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.deleteorder.url");

    String updateOrder_url = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("prod.updateorder.url");

    if (updateOrder_url == null) {
      errorListObj.add("prod.updateorder.url");
    } else {
      outPut.put("updateOrder_url", updateOrder_url);
    }

    if (deleteOrder_url == null) {
      errorListObj.add("prod.deleteorder.url");
    } else {
      outPut.put("deleteOrder_url", deleteOrder_url);
    }

    if (getOrder_url == null) {
      errorListObj.add("prod.getorder.url");
    } else {
      outPut.put("getOrder_url", getOrder_url);
    }

    if (getOrderLine_url == null) {
      errorListObj.add("prod.getorderLine.url");
    } else {
      outPut.put("getOrderLine_url", getOrderLine_url);
    }

    if (getElpProduct_url == null) {
      errorListObj.add("prod.getElpProduct.url");
    } else {
      outPut.put("getElpProduct_url", getElpProduct_url);
    }

    if (order_authorization == null) {
      errorListObj.add("prod.authorization");
    } else {
      outPut.put("order_authorization", order_authorization);
    }

    if (order_acceptVersion == null) {
      errorListObj.add("prod.acceptVersion");
    } else {
      outPut.put("order_acceptVersion", order_acceptVersion);
    }

    if (postOrder_contentType == null) {
      errorListObj.add("prod.postorder.contentType");
    } else {
      outPut.put("postOrder_contentType", postOrder_contentType);
    }

    if (postOrder_requestMethod == null) {
      errorListObj.add("prod.postorder.requestMethod");
    } else {
      outPut.put("postOrder_requestMethod", postOrder_requestMethod);
    }

    if (postOrder_supplierKey == null) {
      errorListObj.add("prod.postorder.supplierKey");
    } else {
      outPut.put("postOrder_supplierKey", postOrder_supplierKey);
    }

    if (postOrder_deliveryKey == null) {
      errorListObj.add("prod.postorder.deliveryKey");
    } else {
      outPut.put("postOrder_deliveryKey", postOrder_deliveryKey);
    }

    if (postOrder_deliveryId == null) {
      errorListObj.add("prod.postorder.deliveryId");
    } else {
      outPut.put("postOrder_deliveryId", postOrder_deliveryId);
    }

    if (postOrder_customerKey == null) {
      errorListObj.add("prod.postorder.customerKey");
    } else {
      outPut.put("postOrder_customerKey", postOrder_customerKey);
    }

    if (postOrder_customerId == null) {
      errorListObj.add("prod.postorder.customerId");
    } else {
      outPut.put("postOrder_customerId", postOrder_customerId);
    }

    if (postOrder_orderType == null) {
      errorListObj.add("prod.postorder.orderType");
    } else {
      outPut.put("postOrder_orderType", postOrder_orderType);
    }

    if (postOrder_orderStatus == null) {
      errorListObj.add("prod.postorder.orderStatus");
    } else {
      outPut.put("postOrder_orderStatus", postOrder_orderStatus);
    }

    if (postOrder_origin == null) {
      errorListObj.add("prod.postorder.origin");
    } else {
      outPut.put("postOrder_origin", postOrder_origin);
    }

    if (postOrder_Url == null) {
      errorListObj.add("prod.postorder.url");
    } else {
      outPut.put("postOrder_Url", postOrder_Url);
    }
    /*
     * if (order_XEnv == null) { errorListObj.add("prod.x_env"); } else {
     */
    outPut.put("order_XEnv", order_XEnv);
    // }
    if (tokenUrl == null) {
      errorListObj.add("prod.token.Url");
    } else {
      outPut.put("tokenUrl", tokenUrl);
    }
    if (token_authKey == null) {
      errorListObj.add("prod.token.basic_auth");
    } else {
      outPut.put("token_authKey", token_authKey);
    }
    if (tokenGrantType == null) {
      errorListObj.add("prod.token.grant_type");
    } else {
      outPut.put("tokenGrantType", tokenGrantType);
    }

    if (errorListObj.size() > 0) {
      String errorString = "Error_CommonServiceProvider: " + errorListObj
          + " configuration is missing in Openbravo.properties file";
      outPut.put("Error", errorString);
      // logger.logln(errorString);
      log.error(errorString);
    }
    return outPut;

  }

}
