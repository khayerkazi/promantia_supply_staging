package in.decathlon.retail.rangeselection.ad_webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class RangeSelectionWebService extends HttpSecureAppServlet implements WebService {

	private static Logger log = Logger.getLogger(RangeSelectionWebService.class);

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String xml = "";
		String strRead = "";
		try {
			InputStreamReader isReader = new InputStreamReader(request.getInputStream());
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

			JSONObject jsonDataObject = new JSONObject();
			int records = parseUserXML(document);
			jsonDataObject.put("status", "success");
			jsonDataObject.put("recordsupdated", records);
			response.setContentType("text/json");
			response.setCharacterEncoding("utf-8");
			final Writer w = response.getWriter();
			w.write(jsonDataObject.toString());
			w.close();
		} catch (Exception e) {
			log.error("Exception occurred in RangeSelectionWebService: ",e);
		}

	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 * This method parses the xml tags and returns the json object containing
	 * the user details
	 * 
	 * @param userXML
	 * @return jsonDataObject
	 */
	public static int parseUserXML(Document userXML) {
		Session session = OBDal.getInstance().getSession();
		JSONObject jsonDataObject = new JSONObject();
		String orgId = userXML.getElementsByTagName("orgid").item(0).getChildNodes().item(0).getNodeValue();
		String models = userXML.getElementsByTagName("models").item(0).getChildNodes().item(0).getNodeValue();
		String isNewStore = userXML.getElementsByTagName("isnewstore").item(0).getChildNodes().item(0).getNodeValue();
		String itemQty = userXML.getElementsByTagName("qty").item(0).getChildNodes().item(0).getNodeValue();
		String keepPrevRange = userXML.getElementsByTagName("keepprevrange").item(0).getChildNodes().item(0).getNodeValue();
		int records = 0;
		Query query = null;
		try{
			OBContext context = OBContext.getOBContext();
			String userId = context.getUser().getId();
			
			if(isNewStore.equals("Y") && null!=itemQty && !itemQty.isEmpty()){
				if(keepPrevRange.equals("N")){
					String queryString = "update cl_minmax set isinrange='N',updated=now(),updatedby='"+userId+"',last_mm_update=now() where isinrange='Y' and ad_org_id='"+orgId+"'";
					query = session.createSQLQuery(queryString);
					records = query.executeUpdate();
				}
				log.info("Number of records updated with N in cl_minmax for new store--> "+records);
				records = updateIsinRange(session,models,orgId);
				List<String> itemQtyList = Arrays.asList(itemQty.split(", "));
				String displayMinQty = "";
				String implantationQty = "";
				StringBuffer displayminQuery = new StringBuffer();
				StringBuffer implantationQuery = new StringBuffer();
				String implantationSelectQuery = "";
				int recordsUpdated = 0;
				boolean isImpExist = false;
			
				displayminQuery.append("update cl_minmax as t set " +
						"displaymin = c.displaymin" +
						" from (values ");
				
				implantationQuery.append("insert into cl_implantation " +
						"(cl_implantation_id,ad_client_id,ad_org_id,isactive, createdby, updatedby, m_product_id, implantation_qty, " +
						"isimplanted, store_implanted, blocked_qty)	values ");
			

						for(String itemqty : itemQtyList){
							implantationSelectQuery = "select count(*) from cl_implantation where store_implanted='"+orgId+"' and m_product_id = '"+itemqty.split("=")[0]+"'";
							query = session.createSQLQuery(implantationSelectQuery.toString());
							List<BigInteger> count = query.list();
							if(count.get(0).intValue()==0){
								implantationQty += "(get_uuid(),'187D8FC945A5481CB41B3EE767F80DBB','0','Y','"+userId+"','"+userId+"','"+itemqty.split("=")[0]+"'," +
									"'"+Integer.parseInt(itemqty.split("=")[1])+"','N','"+orgId+"',0),";
								isImpExist = true;
							}
							itemqty = itemqty.replace("=", "', ");
							displayMinQty += "('"+itemqty+"),";
								
						}
						
						if(isImpExist){
							implantationQty = implantationQty.substring(0,implantationQty.length()-1);
							implantationQuery.append(implantationQty);
							query = session.createSQLQuery(implantationQuery.toString());
							recordsUpdated = query.executeUpdate();	
							log.info("Number of records inserted with qty in cl_implantation--> "+recordsUpdated);
						}
						
						displayMinQty = displayMinQty.substring(0,displayMinQty.length()-1);
						displayminQuery.append(displayMinQty).append(") as c(m_product_id,displaymin) " +
								"where c.m_product_id  = t.m_product_id and ad_org_id ='"+orgId+"'");
						
						query = session.createSQLQuery(displayminQuery.toString());
						recordsUpdated = query.executeUpdate();
						log.info("Number of records updated with qty in cl_minmax with new store--> "+recordsUpdated);
					
				
			}else if(null!=itemQty && !itemQty.isEmpty()){
				checkMinMaxToImplantation(session, models, orgId, itemQty, keepPrevRange, isNewStore);
			}
		}catch(Exception e){
			log.error("Exception occurred while updating cl_minmax: ", e);
			 OBDal.getInstance().rollbackAndClose();
		}
		
		return records;
	}
	
	/**
	 * This method updates the isinrange flag of cl_minmax table
	 * 
	 */
	private static void checkMinMaxToImplantation(Session session, String models, String orgId, String itemQty, String keepPrevRange, String isNewStore){
		int recordsUpdated = 0;
		Query hqlQuery = null;
		String implantationData = "";
				
		try{
			String userId = OBContext.getOBContext().getUser().getId();
			List<String> modelsList = Arrays.asList(models.split(", "));
			List<String> itemQtyList = Arrays.asList(itemQty.split(", "));
			StringBuffer selectMinMaxQuery = new StringBuffer();
			StringBuffer implantationQuery = new StringBuffer();
			StringBuffer updateMinmax = new StringBuffer();
			StringBuffer displayminQuery = new StringBuffer();
			String displayMinvalues = "";
			
			selectMinMaxQuery.append("select m_product_id from cl_minmax where isinrange='N' and ad_org_id=? and " +
					"m_product_id in (select m_product_id from m_product where producttype = 'I' and em_cl_lifestage in ('Active','New') and em_cl_modelcode in (");
			
			implantationQuery.append("insert into cl_implantation " +
					"(cl_implantation_id,ad_client_id,ad_org_id,isactive, createdby, updatedby, m_product_id, implantation_qty, " +
					"isimplanted, store_implanted, blocked_qty)	values ");
						
			updateMinmax.append("update cl_minmax set isinrange = 'Y' where ad_org_id ='"+orgId+"' and m_product_id in (");
			
			displayminQuery.append("update cl_minmax as t set displaymin = c.displaymin from (values ");
			
			for (int i = 0; i < modelsList.size(); i++) {
				selectMinMaxQuery.append("?" + (modelsList.size() == i + 1 ? ")) " : ","));
			}
			hqlQuery = session.createSQLQuery(selectMinMaxQuery.toString());
			hqlQuery.setString(0, orgId);
			
			for(int i = 0; i < modelsList.size(); i++){
				hqlQuery.setString(i+1, modelsList.get(i).trim());
			}
			
			List list  =  hqlQuery.list();

			if(list.size()>0){
				for(Object object : list)
		         {
		            String productId = (String)object;
		            updateMinmax.append("'"+productId+"',");
					for(String itemqty : itemQtyList){

						boolean flag = false;
						if(productId.equals(itemqty.split("=")[0])){
							implantationData += "(get_uuid(),'187D8FC945A5481CB41B3EE767F80DBB','0','Y','"+userId+"','"+userId+"','"+productId+"'," +
								"'"+Integer.parseInt(itemqty.split("=")[1])+"','N','"+orgId+"',0),";
								flag = true;
							}
						}
		         	}
				
					for(String itemqty : itemQtyList){
						if(!list.contains(itemqty.split("=")[0])){
							itemqty = itemqty.replace("=", "',");
							displayMinvalues += "('"+itemqty.trim()+"),";
						}
					
					}
				
					String updatedMinmax = updateMinmax.toString().substring(0,updateMinmax.toString().length()-1);
					updatedMinmax += ")";
					hqlQuery = session.createSQLQuery(updatedMinmax.toString());	
					recordsUpdated = hqlQuery.executeUpdate();
					log.info("Number of records updated with in cl_minmax which are isinrange as N to Y with old store--> "+recordsUpdated);
					

					implantationData = implantationData.substring(0,implantationData.length()-1);
					implantationQuery.append(implantationData);
					hqlQuery = session.createSQLQuery(implantationQuery.toString());
					recordsUpdated = hqlQuery.executeUpdate();	
					log.info("Number of records inserted with qty in cl_implantation which are isinrange as N in minmax with old store--> "+recordsUpdated);
					
				if(null!=displayMinvalues && !displayMinvalues.equals("")){
					displayMinvalues = displayMinvalues.substring(0,displayMinvalues.length()-1);
					displayminQuery.append(displayMinvalues).append(") as c(m_product_id,displaymin) " +
									"where c.m_product_id  = t.m_product_id and ad_org_id ='"+orgId+"'");
					hqlQuery = session.createSQLQuery(displayminQuery.toString());
					recordsUpdated = hqlQuery.executeUpdate();
					log.info("Number of records updated with displaymin in cl_minmax which are isinrange as N with old store --> "+recordsUpdated);
					}
					
					if(keepPrevRange.equals("N")){
						String queryString = "update cl_minmax set isinrange='N',updated=now(),updatedby='"+userId+"',last_mm_update=now() where isinrange='Y' and ad_org_id='"+orgId+"'";
						hqlQuery = session.createSQLQuery(queryString);
						recordsUpdated = hqlQuery.executeUpdate();
					}
					recordsUpdated = updateIsinRange(session,models,orgId);
					log.info("Number of records updated with displaymin in cl_minmax which are isinrange as Y with old store--> "+recordsUpdated);
					
				}else{
					if(keepPrevRange.equals("N")){
						String queryString = "update cl_minmax set isinrange='N',updated=now(),updatedby='"+userId+"',last_mm_update=now() where isinrange='Y' and ad_org_id='"+orgId+"'";
						hqlQuery = session.createSQLQuery(queryString);
						recordsUpdated = hqlQuery.executeUpdate();
					}
					recordsUpdated = updateIsinRange(session,models,orgId);
					StringBuffer displayminUpdateQuery = new StringBuffer();
					displayminUpdateQuery.append("update cl_minmax as t set " +
							"displaymin = c.displaymin" +
							" from (values ");
							for(String itemqty : itemQtyList){
								itemqty = itemqty.replace("=", "',");
								displayMinvalues += "('"+itemqty.trim()+"),";
									
							}
							if(null!=displayMinvalues && !displayMinvalues.equals("")){
								displayMinvalues = displayMinvalues.substring(0,displayMinvalues.length()-1);
								displayminUpdateQuery.append(displayMinvalues).append(") as c(m_product_id,displaymin) " +
										"where c.m_product_id  = t.m_product_id and ad_org_id ='"+orgId+"'");
								hqlQuery = session.createSQLQuery(displayminUpdateQuery.toString());
						
								recordsUpdated = hqlQuery.executeUpdate();
								log.info("Number of records updated with displaymin in cl_minmax with old store--> "+recordsUpdated);
							}
					
				}
			
				
		}catch(Exception e){
			log.error("Error occurred in updateIsinRange: ",e);
			 OBDal.getInstance().rollbackAndClose();
		}
		
	}
	
	

	/**
	 * This method updates the isinrange flag of cl_minmax table
	 * 
	 */
	private static int updateIsinRange(Session session, String models, String orgId){
		int recordsUpdated = 0;
		Query hqlQuery = null;
		String userId = null;
				
		try{
			
			
			userId = OBContext.getOBContext().getUser().getId();
			List<String> modelsList = Arrays.asList(models.split(", "));
			StringBuffer updateQuery = new StringBuffer();
			
			updateQuery.append("update cl_minmax set isinrange='Y',updated=now(),updatedby=?,last_mm_update=now() " +
								"where isinrange='N' and ad_org_id=? " +
								"and m_product_id in (select m_product_id from m_product where producttype = 'I' and em_cl_lifestage in ('Active','New') and em_cl_modelcode in (");


			for (int i = 0; i < modelsList.size(); i++) {
					updateQuery.append("?" + (modelsList.size() == i + 1 ? ")) " : ","));
			}
			
			hqlQuery = session.createSQLQuery(updateQuery.toString());
			hqlQuery.setString(0, userId);
			hqlQuery.setString(1, orgId);
			
			for(int i = 0; i < modelsList.size(); i++){
				hqlQuery.setString(i+2, modelsList.get(i).trim());
			}
			
			recordsUpdated =  hqlQuery.executeUpdate();
			log.info("Number of records updated with Y in cl_minmax from updateIsinRange--> "+recordsUpdated);
		}catch(Exception e){
			log.error("Error occurred in updateIsinRange: ",e);
			 OBDal.getInstance().rollbackAndClose();
		}
		
		return recordsUpdated;
	}

}

