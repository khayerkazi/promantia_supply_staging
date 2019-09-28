package in.decathlon.ibud.masters.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.session.OBPropertiesProvider;

public class TokenGenerator   {
	/*private static Logger log = Logger.getLogger(GenerateToken.class);
	  private ProcessLogger logger;
	  logger = bundle.getLogger();*/

public static String generateToken() throws Exception {	  
	    //logger.logln("Token generation has been called ");
    Map<String, String> easConfig = new HashMap<String, String>();

	 	String erpUrl = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	            .getProperty("eas.tokenUrl");
	    String basic_auth = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	            .getProperty("eas.basic_auth");
	    String grant_type = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	            .getProperty("eas.grant_type");
	    String username = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	            .getProperty("eas.username");
	    String password = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	            .getProperty("eas.password");
	    String scope = OBPropertiesProvider.getInstance().getOpenbravoProperties()
	            .getProperty("eas.scope");
	    
	    
	    easConfig.put("eas.tokenUrl", erpUrl);
	    easConfig.put("eas.basic_auth", basic_auth);
	    easConfig.put("eas.grant_type", grant_type);
	    easConfig.put("eas.username", username);
	    easConfig.put("eas.password", password);
	    easConfig.put("eas.scope", scope);
	
	    String token = getToken(easConfig);
	    
	    return token;		
}

private static String getToken(Map<String, String> easConfig) throws Exception {
	
    HttpURLConnection HttpUrlConnection = null;
    BufferedReader reader = null;
    String tokenString = null;
    String tokenType = null;
    OutputStreamWriter wr = null;
    InputStream is = null;
    try {

      if (easConfig.get("eas.tokenUrl") == null) {
        throw new Exception("TokenGenerator: eas.tokenUrl configuration is missing");
      }
      if (easConfig.get("eas.basic_auth") == null) {
        throw new Exception("TokenGenerator:eas.basic_auth configuration is missing");
      }
      if (easConfig.get("eas.grant_type") == null) {
        throw new Exception("TokenGenerator: eas.grant_type configuration is missing");
      }
      if (easConfig.get("eas.username") == null) {
        throw new Exception("TokenGenerator: eas.username configuration is missing");
      }
      if (easConfig.get("eas.password") == null) {
        throw new Exception("TokenGenerator:eas.password configuration is missing");
      }
      if (easConfig.get("eas.scope") == null) {
        throw new Exception("TokenGenerator:eas.scope configuration is missing");
      }
      URL urlObj = new URL(easConfig.get("eas.tokenUrl"));
      HttpUrlConnection = (HttpURLConnection) urlObj.openConnection();
      HttpUrlConnection.setRequestMethod("POST");
      HttpUrlConnection.setDoOutput(true);
      HttpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      HttpUrlConnection.setRequestProperty("Authorization",
          "Basic " + easConfig.get("eas.basic_auth"));

      String data = URLEncoder.encode("grant_type", "UTF-8") + "="
          + URLEncoder.encode(easConfig.get("eas.grant_type"), "UTF-8");

      data += "&" + URLEncoder.encode("username", "UTF-8") + "="
          + URLEncoder.encode(easConfig.get("eas.username"), "UTF-8");

      data += "&" + URLEncoder.encode("password", "UTF-8") + "="
          + URLEncoder.encode(easConfig.get("eas.password"), "UTF-8");

      data += "&" + URLEncoder.encode("scope", "UTF-8") + "="
          + URLEncoder.encode(easConfig.get("eas.scope"), "UTF-8");
      HttpUrlConnection.connect();
      wr = new OutputStreamWriter(HttpUrlConnection.getOutputStream());
      wr.write(data);
      wr.flush();

      if (HttpUrlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
    	//  log.error("TokenGenerator: Error in response from Token API Generator, Response:"
          //  + HttpUrlConnection.getResponseCode());
        throw new Exception("TokenGenerator:Error in response from Token API Generator, Response:"
            + HttpUrlConnection.getResponseCode());
      }
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
       // log.error("TokenGenerator:TokenType null or invalid");
       
        throw new Exception("TokenType null or invalid");
      }
    } catch (Exception e) {
      throw new Exception("TokenGenerator:Error in fetching token from API", e);
    } finally {
      try {
        if (is != null) {
          is.close();
        }
        if (wr != null) {
          wr.close();
        }
        if (HttpUrlConnection != null) {
          HttpUrlConnection.disconnect();
        }
      } catch (Exception e) {
        throw new Exception(
            "Exception while closing outputstream or HttpUrlConnection", e);
      }
    }
    HttpUrlConnection.disconnect();
    return tokenString;
    
  }


}




