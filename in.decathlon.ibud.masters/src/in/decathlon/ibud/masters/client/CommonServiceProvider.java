package in.decathlon.ibud.masters.client;

import in.decathlon.ibud.masters.data.IbudServerTime;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

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
      String erpUrl = configMap.get("");
      String basic_auth = configMap.get("");
      String grant_type = configMap.get("");
      String username = configMap.get("");
      String password = configMap.get("");
      String scope = configMap.get("");

      URL urlObj = new URL(erpUrl);
      HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
      HttpUrlConnection.setRequestMethod("POST");
      HttpUrlConnection.setDoOutput(true);
      HttpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      HttpUrlConnection.setRequestProperty("Authorization", "Basic " + basic_auth);

      String data = URLEncoder.encode("grant_type", "UTF-8") + "="
          + URLEncoder.encode(grant_type, "UTF-8");

      data += "&" + URLEncoder.encode("username", "UTF-8") + "="
          + URLEncoder.encode(username, "UTF-8");

      data += "&" + URLEncoder.encode("password", "UTF-8") + "="
          + URLEncoder.encode(password, "UTF-8");

      data += "&" + URLEncoder.encode("scope", "UTF-8") + "=" + URLEncoder.encode(scope, "UTF-8");
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
    // outPut.put("Bearer", tokenString);
    return tokenString;

  }

  public static Date getIbudUpdatedTime(String serviceKey) {

    OBContext.setAdminMode(true);
    OBCriteria<IbudServerTime> ibudServerTimeCriteria = OBDal.getInstance().createCriteria(
        IbudServerTime.class);
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, serviceKey));
    ibudServerTimeCriteria.setMaxResults(1);
    ibudServerTimeCriteria.setFilterOnReadableClients(false);
    ibudServerTimeCriteria.setFilterOnReadableOrganization(false);
    List<IbudServerTime> ibudServerTimeList = ibudServerTimeCriteria.list();

    Date lastUpdatedTime = null;

    if (ibudServerTimeList != null && ibudServerTimeList.size() > 0) {
      log.debug("Time taken from database ibudServerTimeList.get(0).getLastupdated() "
          + ibudServerTimeList.get(0).getLastupdated());
      lastUpdatedTime = ibudServerTimeList.get(0).getLastupdated();
      if (serviceKey.equalsIgnoreCase("FlexProcess")) {
        long t = lastUpdatedTime.getTime();
        lastUpdatedTime = new Date(t - (15 * 60000)); // Reducing 15 mins
      }
      return lastUpdatedTime;
    } else {
      log.debug("No record found for the Service " + serviceKey
          + ". New record need to insert !!! ");

      IbudServerTime newService = OBProvider.getInstance().get(IbudServerTime.class);
      newService.setActive(true);
      newService.setClient(OBContext.getOBContext().getCurrentClient());
      newService.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      newService.setCreatedBy(OBContext.getOBContext().getUser());
      newService.setCreationDate(new Date());
      newService.setUpdatedBy(OBContext.getOBContext().getUser());
      newService.setUpdated(new Date());
      newService.setNewOBObject(true);

      Date d = new Date();
      Date dateBefore = new Date(d.getTime() - 2 * 24 * 3600 * 1000);
      newService.setLastupdated(dateBefore);

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      String lastUpdatedTime1 = format.format(dateBefore);

      newService.setServiceKey(serviceKey); // OBDal.getInstance().save(newService);
      SessionHandler.getInstance().commitAndStart();
      return dateBefore;//
      // lastUpdatedTime1.toString().replaceAll(" ", "_");

    }

  }

}
