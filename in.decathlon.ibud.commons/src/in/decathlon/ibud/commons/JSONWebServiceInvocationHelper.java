package in.decathlon.ibud.commons;

import in.decathlon.ibud.masters.client.JSONReciever;
import in.decathlon.ibud.masters.data.IbudServerTime;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.shipment.store.CreateGRNService;
import in.decathlon.ibud.shipment.store.ReceiveDataToTruck;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

public class JSONWebServiceInvocationHelper {
  protected final static Logger log = Logger.getLogger(JSONWebServiceInvocationHelper.class);
  static final long MINUTE_IN_MILLIS = 60000;// millisecs

  private String getUpdatedTime(String serviceKey, int lastUpdatedDays) throws ParseException {
    OBContext.setAdminMode(true);
    OBCriteria<IbudServerTime> ibudServerTimeCriteria = OBDal.getInstance().createCriteria(
        IbudServerTime.class);
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_SERVICEKEY, serviceKey));
    ibudServerTimeCriteria.add(Restrictions.eq(IbudServerTime.PROPERTY_CLIENT, OBContext
        .getOBContext().getCurrentClient()));
    ibudServerTimeCriteria.setMaxResults(1);
    List<IbudServerTime> ibudServerTimeList = ibudServerTimeCriteria.list();

    Date lastUpdatedTime = null;

    if (ibudServerTimeList != null && ibudServerTimeList.size() > 0) {
      log.debug("Time taken from database ibudServerTimeList.get(0).getLastupdated() "
          + ibudServerTimeList.get(0).getLastupdated());
      lastUpdatedTime = ibudServerTimeList.get(0).getLastupdated();
      if (serviceKey.equals("Product")) {
        long t = lastUpdatedTime.getTime();
        lastUpdatedTime = new Date(t - (15 * MINUTE_IN_MILLIS)); // Reducing 15 mins
      }
      return lastUpdatedTime.toString().replaceAll(" ", "_");
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
      Date dateBefore = new Date(d.getTime() - lastUpdatedDays * 24 * 3600 * 1000);
      newService.setLastupdated(dateBefore);

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

      String lastUpdatedTime1 = format.format(dateBefore);

      newService.setServiceKey(serviceKey);
      OBDal.getInstance().save(newService);
      SessionHandler.getInstance().commitAndStart();
      return lastUpdatedTime1.toString().replaceAll(" ", "_");

    }

  }

  public JSONObject sendGetrequest(boolean incrementalData, String serviceKey, String wsName,
      String processid) throws Exception {
    return sendGetrequest(incrementalData, serviceKey, wsName, "", processid);
  }

  @SuppressWarnings("static-access")
  public JSONObject sendGetrequest(boolean incrementalData, String serviceKey, String wsName,
      String additionalQueryStringParamters, String processid) throws Exception {
    ReceiveDataToTruck receiveDataToTruck = new ReceiveDataToTruck();
    CreateGRNService createGRNService = new CreateGRNService();
    JSONObject obj = new JSONObject();
    try {
      boolean firstParam = true;
      int maxRowCount = 1;
      String host = "";
      int port = 0;
      String OBpassword = "";
      String url = "";
      int lastUpdatedDays = 0;
      String OBserName = "";
      String obContext = "";
      IbudConfig config = new IbudConfig();
      if (additionalQueryStringParamters.equals("ReqForSL")) {
        OBserName = config.getSupplySLUsername();
        OBpassword = config.getSupplySLPassword();
        host = config.getSupplySLHost();
        port = config.getSupplySLPort();
        url = config.getSupplySLServer();
        lastUpdatedDays = config.getLastUpdatedDays();
        obContext = config.getSupplySLContext().concat("/ws/");
      } else {
        OBserName = config.getSupplyUsername();
        OBpassword = config.getSupplyPassword();
        host = config.getSupplyHost();
        port = config.getSupplyPort();
        url = config.getSupplyServer();
        lastUpdatedDays = config.getLastUpdatedDays();
        obContext = config.getSupplyContext().concat("/ws/");
      }
      final String context = obContext;
      final String userName = OBserName;
      final String password = OBpassword;

      url = "http://" + host + ":" + port + "/" + context + wsName;

      log.info("UserName=" + userName + " and Password=" + password);
      log.info("Invoking Url is" + url);

      if (incrementalData) {
        String timestamp = getUpdatedTime(serviceKey, lastUpdatedDays);
        url = url.concat(firstParam ? "?" : "&").concat("updated=").concat(timestamp);
        firstParam = false;

      }
      if (config.getSupplyRowCount() != null) {
        maxRowCount = Integer.parseInt(config.getSupplyRowCount());
        url = url.concat(firstParam ? "?" : "&").concat("rowCount=" + maxRowCount);
        firstParam = false;
      }
      if (!additionalQueryStringParamters.equals("")
          && !additionalQueryStringParamters.equals("ReqForSL")) {
        url = url.concat(firstParam ? "?" : "&").concat(additionalQueryStringParamters);
        firstParam = false;
      }

      log.info("Final Url is " + url);
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

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date date = new Date();
      String updated = format.format(date);
      int i;
      char ch;

      StringBuffer sb = new StringBuffer();
      java.io.InputStream s = null;
      try {

        if (serviceKey.equals(SOConstants.ServiceKeyShipmentInOut)) {

          s = response.getEntity().getContent();
          log.debug("ShipmentInOut response processing starts");

          while ((i = s.read()) != -1) {
            ch = (char) i;
            sb.append(ch);

            if (sb.toString().contains(SOConstants.delimiterForGRN)) {
              log.debug("String being converted to shipment is " + sb.toString());

              obj = new JSONObject(sb.toString());
              createGRNService.saveShimpmentHeader(obj, processid);
              sb.setLength(0);
            }

          }
          log.info("Exiting GS processing with balance string **" + sb + "**");

        } else if (serviceKey.equals(SOConstants.ServiceKeyProduct)
            || serviceKey.equals(SOConstants.ServiceKeyPriceList)
            || serviceKey.equals(SOConstants.ServiceKeyOrganization)
            || serviceKey.equals(SOConstants.ServiceKeyUser)) {

          s = response.getEntity().getContent();

          while ((i = s.read()) != -1) {
            ch = (char) i;
            sb.append(ch);
            if ((ch == '}')) {
              if (sb.length() > 2) {
                JSONReciever.saveData(sb.toString(), processid);
                sb.setLength(0);

              }
            }
          }

        } else if (serviceKey.equals("obwship")) {

          s = response.getEntity().getContent();
          log.debug("Shipper response processing starts");

          while ((i = s.read()) != -1) {
            ch = (char) i;
            sb.append(ch);

            if (sb.toString().contains(SOConstants.delimiterForGRN)) {
              log.debug("String being converted to shipper is " + sb.toString());

              obj = new JSONObject(sb.toString());
              receiveDataToTruck.saveTruckHeader(obj, processid);
              sb.setLength(0);
            }

          }
          log.info("Exiting shipper processing with balance string **" + sb + "**");

        } else {
          String output = EntityUtils.toString(response.getEntity());
          log.info("Response from Supply " + output);
          obj = new JSONObject(output);

        }

      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      } finally {
        if (s != null)
          s.close();
      }
    } catch (Exception exc) {
      exc.printStackTrace();
      throw exc;
    }

    return obj;

  }

  public static void sendPostrequest(String wsName, String additionalQueryStringParamters,
      String content) throws Exception {

    JSONObject obj = new JSONObject();
    try {
      boolean firstParam = true;
      String host = "";
      int port = 0;
      String url = "";

      final String userName = IbudConfig.getSupplyUsername();
      final String password = IbudConfig.getSupplyPassword();
      host = IbudConfig.getSupplyHost();
      port = IbudConfig.getSupplyPort();
      String context = IbudConfig.getSupplyContext().concat("/ws/");

      url = "http://" + host + ":" + port + "/" + context + wsName;

      if (!additionalQueryStringParamters.equals("")) {
        url = url.concat(firstParam ? "?" : "&").concat(additionalQueryStringParamters);
        firstParam = false;
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

      HttpPost httpPost = new HttpPost(url);

      ByteArrayEntity b = new ByteArrayEntity(content.getBytes());
      httpPost.setEntity(b);

      HttpResponse response = httpclient.execute(httpPost);

      EntityUtils.toString(response.getEntity());

    } catch (Exception exc) {
      log.error(exc);
      exc.printStackTrace();
      throw exc;
    }

  }

  public static JSONObject sendPostrequestToClose(String wsName,
      String additionalQueryStringParamters, String content) throws Exception {

    JSONObject obj = new JSONObject();
    try {
      boolean firstParam = true;
      String host = "";
      int port = 0;
      String url = "";

      final String userName = IbudConfig.getSupplyUsername();
      final String password = IbudConfig.getSupplyPassword();
      host = IbudConfig.getSupplyHost();
      port = IbudConfig.getSupplyPort();
      String context = IbudConfig.getSupplyContext().concat("/ws/");

      url = "http://" + host + ":" + port + "/" + context + wsName;

      if (!additionalQueryStringParamters.equals("")) {
        url = url.concat(firstParam ? "?" : "&").concat(additionalQueryStringParamters);
        firstParam = false;
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

      HttpPost httpPost = new HttpPost(url);

      ByteArrayEntity b = new ByteArrayEntity(content.getBytes());
      httpPost.setEntity(b);

      HttpResponse response = httpclient.execute(httpPost);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        log.debug("response is " + response.getHeaders("Result"));
        Header[] headrs = response.getAllHeaders();
        for (Header hd : headrs) {
          if (hd.getName().equals("Result")) {
            obj = new JSONObject(hd.getValue());
          }
        }
        return obj;
      }

      else {
        log.error(response.getStatusLine());
        Header[] headrs = response.getAllHeaders();
        for (Header hd : headrs) {
          if (hd.getName().equals("Error")) {
            throw new Exception(hd.getValue());
          }
        }
        throw new OBException(response.getStatusLine().toString());

      }

    } catch (Exception exc) {
      log.error(exc);
      exc.printStackTrace();
      throw exc;
    }

  }
}
