package in.nous.crm.ratingscreen.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import in.decathlon.integration.PassiveDB;
import in.nous.crm.ratingscreen.ad_webservices.StoreDTO;

/**
 * This Web Service is ued to run queries required by Rating Screen web
 * application
 * 
 */

public class RatingScreenWebService extends HttpSecureAppServlet implements
		WebService {

	private static Logger log = Logger.getLogger(RatingScreenWebService.class);

	private static final String comma = ",";

	private static final String noRate = "No rate yet";
	
	private static final String office = "Office";
	
	private static final String bestStore = "BestStore";

        private static final String zonalLevel = "ZonalLevel";    

        private static final String regex2 = "&";

	private static DecimalFormat df = new DecimalFormat("#.#");

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Performs the POST REST operation. This service handles the request for
	 * the XML Schema containing Rating Screen web application information.
	 * 
	 * @param path
	 *            the HttpRequest.getPathInfo(), the part of the url after the
	 *            context path
	 * @param request
	 *            the HttpServletRequest
	 * @param response
	 *            the HttpServletResponse
	 */
	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String xml = "";
		String strRead = "";

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

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		Document document = builder.parse(is);

		JSONObject jsonDataObject = parseRatingScreenXML(document);

		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(jsonDataObject.toString());
		w.close();

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();

	}

	/**
	 * This method parses the xml tags and returns the json object containing
	 * the ratings
	 * 
	 * @param rscreenXML
	 * @return ratings
	 */
	public static JSONObject parseRatingScreenXML(Document rscreenXML) {
		String storeName = null;
		JSONObject jsonDataObject = new JSONObject();
		String result = "";
		double currentRate = 0;
		double ytdRate = 0;
                double ytdRateNorthZone = 0;
                double ytdRateWestZone = 0;
                double ytdRateSouthZone = 0;
                double forPercentage=100;

		try {
			List<Object> param = new ArrayList<Object>();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date currentDate = new Date();
			
			currentDate.setHours(0);
			currentDate.setMinutes(0);
			currentDate.setSeconds(0);
			String cDate = sdf.format(currentDate);
						

			Calendar c = Calendar.getInstance();
			c.setTime(currentDate);
			c.add(Calendar.DATE, +1);
			c.set(Calendar.HOUR_OF_DAY,0);
			c.set(Calendar.MINUTE,0);
			c.set(Calendar.SECOND,0);
			String c1Date = sdf.format(c.getTime());

			Calendar c2 = Calendar.getInstance();
			c2.setTime(currentDate);
			int year = c2.get(Calendar.YEAR);
			c2.set(year, 0, 1);
			c2.set(Calendar.HOUR_OF_DAY,0);
			c2.set(Calendar.MINUTE,0);
			c2.set(Calendar.SECOND,0);
			String c2Date = sdf.format(c2.getTime());
			
			
			Connection conn = PassiveDB.getInstance().getConnection();
			String proc = "{call ncrs_rating_screen(?, ?, ?, ?, ?)}";
			CallableStatement callStatement = null;
			
			storeName = rscreenXML.getElementsByTagName("storeName").item(0)
					.getChildNodes().item(0).getNodeValue();
			if (null != storeName && !storeName.isEmpty()) {

				
				callStatement = conn.prepareCall(proc);
				callStatement.setString(1, storeName);
				callStatement.setTimestamp(2, Timestamp.valueOf(cDate));
				callStatement.setTimestamp(3, Timestamp.valueOf(c1Date));
				callStatement.setTimestamp(4, Timestamp.valueOf(c2Date));
				callStatement.registerOutParameter(5, java.sql.Types.VARCHAR);
				
				// execute getDBUSERByUserId store procedure
				callStatement.executeUpdate();
				
				result = callStatement.getString(5);
				
				if (null != result && result.contains(comma)) {
                                          
					String currentRateData[] = result.split(comma);
					if (Double.parseDouble(currentRateData[1]) == 0) {
						jsonDataObject.put("currentRate", noRate);
                                                   
					} else {
						currentRate = (Double.parseDouble(currentRateData[0])*forPercentage)
								/ Double.parseDouble(currentRateData[1]);
						jsonDataObject.put("currentRate",
								df.format(currentRate));
                                                
					}

					if (Double.parseDouble(currentRateData[3]) == 0) {
						jsonDataObject.put("ytdRate", noRate);
                                                     
					} else {
						
 
                                        ytdRate = (Double.parseDouble(currentRateData[2])*forPercentage)
								/ Double.parseDouble(currentRateData[3]);
						jsonDataObject.put("ytdRate", df.format(ytdRate));
                                                
					}
                                           
					if(storeName.equalsIgnoreCase(office)){
						
                                                List<Object> param2 = new ArrayList<Object>();
						String result2="";
						double maxCurrentRate=0;
						double maxYTDRate=0;
						StoreDTO bestCurrentStore=null;
						StoreDTO bestYTDStore=null;
                                                callStatement = conn.prepareCall(proc);
						callStatement.setString(1, bestStore);
						callStatement.setTimestamp(2, Timestamp.valueOf(cDate));
						callStatement.setTimestamp(3, Timestamp.valueOf(c1Date));
						callStatement.setTimestamp(4, Timestamp.valueOf(c2Date));
						callStatement.registerOutParameter(5, java.sql.Types.VARCHAR);
						
						// execute getDBUSERByUserId store procedure
						callStatement.executeUpdate();
						
						result2 = callStatement.getString(5);
						System.out.println("result2222 "+result2);
						
						if (null != result2 && result2.contains(regex2)){
                                                 
							String stores[] = result2.split(regex2);
                                                     
							List<StoreDTO> listStoreDTO = new ArrayList<StoreDTO>();
							
							for(String store : stores){
								
                                                                String storeData[] = store.split(comma);
								StoreDTO storeDTO = new StoreDTO();
								storeDTO.setStoreName(storeData[0]);
                                            
								storeDTO.setCurrentRate(Double.parseDouble(storeData[2]) == 0?noRate:df.format((Double.parseDouble(storeData[1])*forPercentage)
												/ Double.parseDouble(storeData[2])));


								storeDTO.setYtdRate(Double.parseDouble(storeData[4]) == 0?noRate:df.format((Double.parseDouble(storeData[3])*forPercentage)
										/ Double.parseDouble(storeData[4])));
        								listStoreDTO.add(storeDTO);
							}

						
							
							for(StoreDTO dto : listStoreDTO){
								
								if(!dto.getCurrentRate().equalsIgnoreCase(noRate) && maxCurrentRate < Double.parseDouble(dto.getCurrentRate())){
									maxCurrentRate = Double.parseDouble(dto.getCurrentRate());
									bestCurrentStore = dto;
								}
								
								if(!dto.getYtdRate().equalsIgnoreCase(noRate) && maxYTDRate < Double.parseDouble(dto.getYtdRate())){
									maxYTDRate = Double.parseDouble(dto.getYtdRate());
									bestYTDStore = dto;
								}
								
							}
							
							if(null==bestCurrentStore){
								jsonDataObject.put("bestCurrentRateStore", "");
								jsonDataObject.put("bestCurrentRate", noRate);
								
							}else{
								jsonDataObject.put("bestCurrentRateStore", bestCurrentStore.getStoreName());
								jsonDataObject.put("bestCurrentRate", bestCurrentStore.getCurrentRate());
							}
							
							if(null==bestYTDStore){
								jsonDataObject.put("bestYTDRateStore", "");
								jsonDataObject.put("bestYTDRate", noRate);
							}else{
								jsonDataObject.put("bestYTDRateStore", bestYTDStore.getStoreName());
								jsonDataObject.put("bestYTDRate", bestYTDStore.getYtdRate());
							}
							
							
						}
						

                                            //call procedure for zone
                                                String resultZone="";
						callStatement = conn.prepareCall(proc);
						callStatement.setString(1, zonalLevel);
						callStatement.setTimestamp(2, Timestamp.valueOf(cDate));
						callStatement.setTimestamp(3, Timestamp.valueOf(c1Date));
						callStatement.setTimestamp(4, Timestamp.valueOf(c2Date));
						callStatement.registerOutParameter(5, java.sql.Types.VARCHAR);
						
						// execute getDBUSERByUserId store procedure
						callStatement.executeUpdate();
						
						resultZone = callStatement.getString(5);
						System.out.println("resultZone "+resultZone);
						
						if (null != resultZone && resultZone.contains(comma)){
                                                 
							  String currentRateDataZone[] = resultZone.split(comma);
                
                                           if (Double.parseDouble(currentRateDataZone[1]) == 0) {
                                              jsonDataObject.put("ytdRateNorthZone", noRate);
                                                //System.out.println("ytdRateNorthZone 161616 " + noRate);

                                           } else {
                               ytdRateNorthZone = (Double.parseDouble(currentRateDataZone[0]) * forPercentage)
                                    / Double.parseDouble(currentRateDataZone[1]);
                                 jsonDataObject.put("ytdRateNorthZone", df.format(ytdRateNorthZone));
                       
                                    //System.out.println("ytdRateNorthZone 1717 " + df.format(ytdRateNorthZone));

                                   }

              // use this below block of code for west zone
              if (Double.parseDouble(currentRateDataZone[3]) == 0) {
                jsonDataObject.put("ytdRateWestZone", noRate);
                //System.out.println("ytdRateWestZone 1818 " + noRate);

              } else {

                ytdRateWestZone = (Double.parseDouble(currentRateDataZone[2]) * forPercentage)
                    / Double.parseDouble(currentRateDataZone[3]);
                jsonDataObject.put("ytdRateWestZone", df.format(ytdRateWestZone));
                
                System.out.println("ytdRatewestZone 1919 " + df.format(ytdRateWestZone));

              }

              // use this below block of code for South zone
              if (Double.parseDouble(currentRateDataZone[5]) == 0) {
                jsonDataObject.put("ytdRateSouthZone", noRate);
               // System.out.println("ytdRateSouthZone 2020 " + noRate);

              } else {
                 System.out.println("else loop south zone");
                ytdRateSouthZone = (Double.parseDouble(currentRateDataZone[4]) * forPercentage)
                    / Double.parseDouble(currentRateDataZone[5]);
                jsonDataObject.put("ytdRateSouthZone", df.format(ytdRateSouthZone));

                System.out.println("ytdRateSouthZone 212121 " + df.format(ytdRateSouthZone));

              }
							
	} // end of if resultZone condition
						
// end proc call for zone

                                }
					
					jsonDataObject.put("status", "Success");
                                          
				} else {
					jsonDataObject.put("status", "Failure");
                                           
				}

			}
		} catch (Exception exp) {
			log.error("Exception while reading data for Rating Screen. ", exp);

			exp.printStackTrace();
			return jsonDataObject;
		}

		return jsonDataObject;

	}

}
