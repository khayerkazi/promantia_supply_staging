package com.sysfore.decathlonecom;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sysfore.decathlonecom.dao.EcomWebServiceDAO;
import com.sysfore.decathlonecom.model.EcomAddress;
import com.sysfore.decathlonecom.model.EcomCustomer;
import com.sysfore.decathlonecom.util.EcomSyncUtil;

/**
 * The Web Service in charge of integration between Decathlon Ecommerce and Openbravo ERP.
 * 
 * @author binesh michael
 */

public class EcomWebService extends HttpSecureAppServlet implements WebService {

  private static Logger log = Logger.getLogger(EcomWebService.class);

  // private static Map<String, PosSyncProcess> posProcesses = null;
  protected static ConnectionProvider pool;

  public EcomWebService() {
    // initPool();
  }

  /**
   * Performs the GET REST operation. This service handles the request for the XML Schema of list of
   * Business Objects.
   * 
   * @param path
   *          the HttpRequest.getPathInfo(), the part of the url after the context path
   * @param request
   *          the HttpServletRequest
   * @param response
   *          the HttpServletResponse
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // throw new UnsupportedOperationException();
    System.out.println("Inside the get");
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write("inside the get");
    w.close();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // throw new UnsupportedOperationException();

    //String xml = request.getParameter("ecomUser");
    
    String xml = "";
    String strRead = "";
    //String xml = request.getParameter("ecomuser");
    InputStreamReader isReader = new InputStreamReader(
			request.getInputStream());
	BufferedReader bReader = new BufferedReader(isReader);
	StringBuilder strBuilder = new StringBuilder();
	strRead = bReader.readLine();
	while (strRead != null) {
		strBuilder.append(strRead);
		strRead = bReader.readLine();
	}
	xml = strBuilder.toString();
  
    // Following code converting the String into XML format

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(xml));
    Document document = builder.parse(is);

    // Validating the XML. Verify all the necessary tag and values are present;

    EcomCustomer eComCustomer = EcomSyncUtil.parseEcomCustomerXML(document);

    String msg = "none";
    EcomWebServiceDAO ecomWebServiceDAO = null;
    try {

      //EcomWebServiceDAO ecomWebServiceDAO = new EcomWebServiceDAO();
      ecomWebServiceDAO = new EcomWebServiceDAO();
      synchronized (ecomWebServiceDAO) {
        TreeMap<String, String> rcSports = ecomWebServiceDAO.selectSports();
        StringTokenizer rcSportsToken = new StringTokenizer(eComCustomer.getSports(), "|");

        // Following section will transalate SportCode to ERP values
        System.out.println("Inside the Token");
        while (rcSportsToken != null && rcSportsToken.hasMoreTokens()) {
          String key = rcSportsToken.nextToken().trim();
          String value = rcSports.get(key);
	if(value == null) {
          
	}else if (value.equalsIgnoreCase("Aikido")) {
            eComCustomer.setAikido("Y");
            System.out.println("Aikido");
          } else if (value.equalsIgnoreCase("Alpinism")) {
            eComCustomer.setAlpinism("Y");
            System.out.println("Alpinism");
          } else if (value.equalsIgnoreCase("Archery")) {
            eComCustomer.setArchery("Y");
            System.out.println("Archery");
          } else if (value.equalsIgnoreCase("Badminton")) {
            eComCustomer.setBadminton("Y");
            System.out.println("Badminton");
          } else if (value.equalsIgnoreCase("Basket")) {
            eComCustomer.setBasket("Y");
            System.out.println("Basket");
          } else if (value.equalsIgnoreCase("Boxing")) {
            eComCustomer.setBoxing("Y");
            System.out.println("Boxing");
          } else if (value.equalsIgnoreCase("Climbing")) {
            eComCustomer.setClimbing("Y");
            System.out.println("Climbing");
          } else if (value.equalsIgnoreCase("Cricket")) {
            eComCustomer.setCricket("Y");
            System.out.println("Cricket");
          } else if (value.equalsIgnoreCase("Cycling")) {
            eComCustomer.setCycling("Y");
            System.out.println("Cycling");
          } else if (value.equalsIgnoreCase("Dance")) {
            eComCustomer.setDance("Y");
            System.out.println("Dance");
          } else if (value.equalsIgnoreCase("Diving")) {
            eComCustomer.setDiving("Y");
            System.out.println("Diving");
          } else if (value.equalsIgnoreCase("Field Hockey")) {
            eComCustomer.setFieldHockey("Y");
            System.out.println("Field Hockey");
          } else if (value.equalsIgnoreCase("Fitness")) {
            eComCustomer.setFitness("Y");
            System.out.println("Fitness");
          } else if (value.equalsIgnoreCase("Fishing")) {
            eComCustomer.setFishing("Y");
            System.out.println("Fishing");
          } else if (value.equalsIgnoreCase("Football")) {
            eComCustomer.setFootball("Y");
            System.out.println("Football");
          } else if (value.equalsIgnoreCase("Golf")) {
            eComCustomer.setGolf("Y");
            System.out.println("Golf");
          } else if (value.equalsIgnoreCase("Gym")) {
            eComCustomer.setGym("Y");
            System.out.println("Gym");
          } else if (value.equalsIgnoreCase("Handball")) {
            eComCustomer.setHandball("Y");
            System.out.println("Handball");
          } else if (value.equalsIgnoreCase("Hiking")) {
            eComCustomer.setHiking("Y");
            System.out.println("Hiking");
          } else if (value.equalsIgnoreCase("Horse riding")) {
            eComCustomer.setHorseRiding("Y");
            System.out.println("Horse riding");
          } else if (value.equalsIgnoreCase("Judo")) {
            eComCustomer.setJudo("Y");
            System.out.println("Judo");
          } else if (value.equalsIgnoreCase("Karate")) {
            eComCustomer.setKarate("Y");
          } else if (value.equalsIgnoreCase("Kite surfing")) {
            eComCustomer.setKiteSurfing("Y");
          } else if (value.equalsIgnoreCase("Paddle")) {
            eComCustomer.setPaddle("Y");
          } else if (value.equalsIgnoreCase("Rollerskating")) {
            eComCustomer.setRollerskating("Y");
          } else if (value.equalsIgnoreCase("Rugby")) {
            eComCustomer.setRugby("Y");
          } else if (value.equalsIgnoreCase("Running")) {
            eComCustomer.setRunning("Y");
          } else if (value.equalsIgnoreCase("Sailing")) {
            eComCustomer.setSailing("Y");
          } else if (value.equalsIgnoreCase("Skiing")) {
            eComCustomer.setSkiing("Y");
          } else if (value.equalsIgnoreCase("Snowboarding")) {
            eComCustomer.setSnowboarding("Y");
          } else if (value.equalsIgnoreCase("Squash")) {
            eComCustomer.setSquash("Y");
          } else if (value.equalsIgnoreCase("Surfing")) {
            eComCustomer.setSurfing("Y");
          } else if (value.equalsIgnoreCase("Swimming")) {
            eComCustomer.setSwimming("Y");
          } else if (value.equalsIgnoreCase("Table Tennis")) {
            eComCustomer.setTableTennis("Y");
          } else if (value.equalsIgnoreCase("Tennis")) {
            eComCustomer.setTennis("Y");
          } else if (value.equalsIgnoreCase("Volley ball")) {
            eComCustomer.setVolleyBall("Y");
          } else if (value.equalsIgnoreCase("Walking")) {
            eComCustomer.setWalking("Y");
          } else if (value.equalsIgnoreCase("Windsurfing")) {
            eComCustomer.setWindsurfing("Y");
          } else if (value.equalsIgnoreCase("Yoga")) {
            eComCustomer.setYoga("N");
          }

        }

        eComCustomer.setGrretingId(ecomWebServiceDAO.selectGreetings(eComCustomer.getGreeting()));

        LinkedList<String> companyDetails = ecomWebServiceDAO.selectCompanyDetails(eComCustomer
            .getCompany());
        if (companyDetails != null && companyDetails.size() > 0) {

          eComCustomer.setCompanyId(companyDetails.get(0));
          eComCustomer.setLicenseId(companyDetails.get(1));
          eComCustomer.setLicenseNo(companyDetails.get(2));
          eComCustomer.setCompanyAddress(companyDetails.get(3));
          // eComCustomer.setCountryID(ecomWebServiceDAO.selectCountry(eComCustomer.getCountry()));
          // eComCustomer.setStateID(ecomWebServiceDAO.selectState(eComCustomer.getState()));

          List<EcomAddress> address = eComCustomer.getEcomAddress();

          HttpURLConnection hc;
	  try {
	        hc = createCustomerConnection(eComCustomer, "old");
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
		final JSONObject respJsonObject = new JSONObject(secondResponse);
		final JSONObject responseJsonObject = (JSONObject)respJsonObject.get("response");
		final JSONArray dataJsonArray = (JSONArray)responseJsonObject.get("data");
		final JSONObject dataJsonObject = dataJsonArray.getJSONObject(0);
		log.info("JSON Response : "+dataJsonObject);
		in.close();
		if(null != dataJsonObject.get("rCOxylane") || dataJsonObject.has("rCOxylane") || !"".equals(dataJsonObject.has("rCOxylane"))) {
			//iorder.setSyncOxylane(dataJsonObject.get("rCOxylane").toString());
                        msg = (dataJsonObject.get("rCOxylane").toString());
		} else {
		     HttpURLConnection hc1;
		     try {
			hc1 = createCustomerConnection(eComCustomer, "new");
			hc1.connect();

			// Getting the Response from the Web service
			BufferedReader in1 = new BufferedReader(
			new InputStreamReader(hc1.getInputStream()));
			String inputLine1;
			StringBuffer resp1 = new StringBuffer();
			while ((inputLine1 = in1.readLine()) != null) {
				resp1.append(inputLine1);
			}
			String secondResponse1 = resp1.toString();
			final JSONObject respJsonObject1 = new JSONObject(secondResponse1);
			final JSONObject responseJsonObject1 = (JSONObject)respJsonObject1.get("response");
			final JSONArray dataJsonArray1 = (JSONArray)responseJsonObject1.get("data");
			final JSONObject dataJsonObject1 = dataJsonArray1.getJSONObject(0);
			log.info("JSON Response : "+dataJsonObject1);
			in1.close();
			//iorder.setSyncOxylane(dataJsonObject1.get("rCOxylane").toString());
                        msg = (dataJsonObject1.get("rCOxylane").toString());
		     } catch (Exception e1) {
			e1.printStackTrace();
		     }
                }
	  } catch (Exception e1) {
		e1.printStackTrace();
	  }

          /*if (eComCustomer.getOxylane().equalsIgnoreCase("0")) {
            msg = ecomWebServiceDAO.createEcomCustomer(eComCustomer);
            //ecomWebServiceDAO.closeConnection();
          } else {
            msg = ecomWebServiceDAO.updateEcomCustomer(eComCustomer);
            //ecomWebServiceDAO.closeConnection();
          }*/


        } else {

          msg = "Company not defined";
          //ecomWebServiceDAO.closeConnection();

        }

      }

    } catch (Exception e) {
      e.printStackTrace();
     } finally {
       if (ecomWebServiceDAO != null) {
         ecomWebServiceDAO.closeConnection();
       }
   }
    // doPost(request, response);
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    xmlBuilder.append("<root>");
    if (msg.length() == 13) {
      xmlBuilder.append("<oxylane>").append(msg).append("</oxylane>");
      xmlBuilder.append("<message>").append("0").append("</message>");
    } else {
      xmlBuilder.append("<oxylane>").append("0").append("</oxylane>");
      xmlBuilder.append("<message>").append(msg).append("</message>");
    }
    xmlBuilder.append("</root>");
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(xmlBuilder.toString());
    w.close();

  }


  protected HttpURLConnection createCustomerConnection(EcomCustomer ecomCustomer, String strKey) throws Exception {

	String customerDBURL = "";
	OBContext.setAdminMode();
	OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
	configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.etlsync"));
	configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_KEY, "customerdbWSURL"));
	if(configInfoObCriteria.count() > 0) {
	   customerDBURL = configInfoObCriteria.list().get(0).getSearchKey();
	}
	OBContext.restorePreviousMode();
	URL url = null; 
	if(strKey.equals("old")) {
	   if(ecomCustomer.getMobile() != null) {
	      url = new URL(customerDBURL+"/dsiCustomerdbGet?mobileno="+ecomCustomer.getMobile()+"&username=l&pwd=p");
	      log.info(customerDBURL+"/dsiCustomerdbGet?mobileno="+ecomCustomer.getMobile()+"&username=l&pwd=p");
	   } else if(ecomCustomer.getEmail() != null) {
	      url = new URL(customerDBURL+"/dsiCustomerdbGet?email="+ecomCustomer.getEmail()+"&username=l&pwd=p");
	      log.info(customerDBURL+"/dsiCustomerdbGet?email="+ecomCustomer.getEmail()+"&username=l&pwd=p");
	   } /*else if(ecomCustomer.getSyncLandline() != null) {
	      url = new URL(customerDBURL+"/dsiCustomerdbGet?landline="+ecomCustomer.getSyncLandline()+"&username=l&pwd=p");
	      log.info(customerDBURL+"/dsiCustomerdbGet?landline="+ecomCustomer.getSyncLandline()+"&username=l&pwd=p");
	   }*/
	} else {
	url = new URL(customerDBURL+"/dsiCustomerdbGet?mobileno="+ecomCustomer.getMobile()+"&email="+ecomCustomer.getEmail()+"&name="+ecomCustomer.getFirstName()+"&name2="+ecomCustomer.getLastName()+"&em_sms_alert=N&em_email_alert=Y &zipcode=null&civility=null&username=l&pwd=p");
	log.info(customerDBURL+"/dsiCustomerdbGet?mobileno="+ecomCustomer.getMobile()+"&email="+ecomCustomer.getEmail()+"&name="+ecomCustomer.getFirstName()+"&name2="+ecomCustomer.getLastName()+"&em_sms_alert=N&em_email_alert=Y &zipcode=null&civility=null&username=l&pwd=p");
	}

	final HttpURLConnection hc = (HttpURLConnection) url.openConnection();

	hc.setRequestMethod("GET");
	hc.setAllowUserInteraction(false);
	hc.setDefaultUseCaches(false);
	hc.setDoInput(true);
	hc.setInstanceFollowRedirects(true);
	hc.setUseCaches(false);
	return hc;
  }
  
  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();

  }

}