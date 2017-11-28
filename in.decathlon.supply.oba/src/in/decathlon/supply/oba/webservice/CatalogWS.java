package in.decathlon.supply.oba.webservice;

import in.decathlon.supply.oba.data.OBA_ModelProduct;
import in.decathlon.supply.oba.util.OBAMail;

import java.io.Writer;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;

public class CatalogWS implements WebService {
	private static final Logger LOG = Logger.getLogger(CatalogWS.class);
    private static String actioncode=null;
    private static String pricelistversion="DMI CATALOGUE";
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			response.setContentType("text");
			final Writer w = response.getWriter();
			w.write("Web Service : CatalogWS - You are in GET method, to use this web service please use POST Method");
			w.close();
		} catch (final Exception e) {
			LOG.error(
					"Web Service : CatalogWS - You are in GET method  , Error receiving data",
					e);
		}
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		int records = 0;
		try {

			if (request == null) {
				LOG.debug("request is null");
			} else {
				JSONArray array = new JSONArray(request.getParameter("parameter"));
				if (array != null) {
					actioncode=request.getParameter("actioncode");
					LOG.debug("CatalogWS: action code ="+actioncode);
					records = addtoDatabase(array);
				}
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
		try {

			JSONObject itemobj = null;
			JSONObject modelobj = null;
			JSONObject job = null;
			String mailSentFlag = null;
			
			for (i = 0; i < array.length(); i++) {
				
				Double mrp=null;
				Double cessionprice=null;
				Double ueprice=null;
				Double pcbprice=null;
				Double unitprice=null;
				char spidflag='N';
				
				itemobj = array.getJSONObject(i);
				modelobj = new JSONObject(itemobj.getString("model"));
				mrp=Double.parseDouble(getAbsolute(itemobj.getString("mrp")));
				cessionprice=Double.parseDouble(getAbsolute(itemobj.getString("cessionprice")));
				unitprice=Double.parseDouble(getAbsolute(itemobj.getString("unitprice")));
				ueprice=Double.parseDouble(getAbsolute(itemobj.getString("ueprice")));
				pcbprice=Double.parseDouble(getAbsolute(itemobj.getString("pcbprice")));
				if(modelobj.getString("spidflag")=="true"){
					spidflag='Y';
				}
				
				// Fetching Tax Category
				String taxCategory = "";
				if(itemobj.getString("taxRate").equals("14.5")) {
					taxCategory = "Higher Rate";
					mailSentFlag = "null";
				} else if (itemobj.getString("taxRate").equals("5.5")) {
					taxCategory = "Lower Rate";
					mailSentFlag = "null";
				} else if (itemobj.getString("taxRate").equals("0")) {
					taxCategory = "Exempted";
					mailSentFlag = "null";
				} else {
					final OBCriteria<Product> prodObCriteria = OBDal.getInstance().createCriteria(Product.class);
					prodObCriteria.add(Restrictions.eq(Product.PROPERTY_NAME, getNotNull(itemobj.getString("itemcode"))));
					if(prodObCriteria.count() > 0) {
						taxCategory = prodObCriteria.list().get(0).getTaxCategory().getName();
						mailSentFlag = "null";
					} else {
						taxCategory = "TBA";
						mailSentFlag = "false";
					}
				}
				
				// if modelcode is existes and in Temp Table already
				final String modelCode = getNotNull(modelobj.getString("modelcode"));
				final String itemCode = getNotNull(itemobj.getString("itemcode"));
				String age = getNotNull(itemobj.getString("age"));
				if("".equals(age))
					age = "N/A";
				String color = getNotNull(itemobj.getString("color"));
				if("".equals(color))
					color = "N/A";
				String gender = getNotNull(itemobj.getString("gender"));
				if("".equals(gender))
					gender = "N/A";
				
				// Brand code
				String brand = getBrndNameUsingUniverse(getNotNull(modelobj.getString("universe")));

//				String brand = getNotNull(modelobj.getString("brandname"));
				if(brand.equals("KIPSTA")) {
					if(getNotNull(modelobj.getString("modelname")).contains("* IN FLX") || getNotNull(modelobj.getString("modelname")).contains("* IN DOMA")) {
						brand = "FLX";
					}
				}
				
				
				if(!"".equals(modelCode) && !"".equals(itemCode)) {
					final OBCriteria<OBA_ModelProduct> obaModelProductObCriteria = OBDal.getInstance().createCriteria(OBA_ModelProduct.class);
					obaModelProductObCriteria.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_MODELCODE, modelCode));
					obaModelProductObCriteria.add(Restrictions.eq(OBA_ModelProduct.PROPERTY_ITEMCODE, itemCode));
					
					if(obaModelProductObCriteria.count() > 0) {
						job = new JSONObject("{\"id\":\""+obaModelProductObCriteria.list().get(0).getId()+"\","
								+ "\"_entityName\":\"OBA_modelproduct\","
								+ "\"iMANCode\":\""+ getNotNull(modelobj.getString("imancode"))
								+ "\",\"actioncode\":\""+ getNotNull(actioncode)
								+ "\",\"modelCode\":\""+ getNotNull(modelobj.getString("modelcode"))
								+ "\",\"modelName\":\""+ getNotNull(modelobj.getString("modelname"))
								+ "\",\"brand\":\""	+ brand
								+ "\",\"merchandiseCategory\":\""+ getNotNull(modelobj.getString("familyname"))
								+ "\",\"typology\":\""+ getNotNull(modelobj.getString("typology"))
								+ "\",\"subdepartment\":\""+ getNotNull(modelobj.getString("dmisubdepatment"))
								+ "\",\"department\":\""+ getNotNull(modelobj.getString("dmidepartment"))
								+ "\",\"storeDepartment\":\""+ getNotNull(modelobj.getString("storedepartment"))
								+ "\",\"storeuniverse\":\""+ getNotNull(modelobj.getString("storeuniverse"))
								+ "\",\"branddepartment\":\""+ getNotNull(modelobj.getString("branddepartment"))
								+ "\",\"universe\":\""+ getNotNull(modelobj.getString("universe"))
								+ "\",\"lifeStage\":\""+ getNotNull(modelobj.getString("lifestage"))
								+ "\",\"spidurl\":\""+ getNotNull(modelobj.getString("spidurl"))
								+ "\",\"spidflag\":\""+ spidflag
								+ "\",\"natureOfProduct\":\""+ getNotNull(modelobj.getString("nature"))
								+ "\",\"uEQty\":"+ Long.parseLong(getAbsolute(itemobj.getString("ue")))
								+ ",\"pCBQty\":"+ Long.parseLong(getAbsolute(itemobj.getString("pcb")))
								+ ",\"eANCode\":\""+ getNotNull(itemobj.getString("eancode"))
								+ "\",\"itemCode\":\""+ getNotNull(itemobj.getString("itemcode"))
								+ "\",\"gender\":\""+ gender
								+ "\",\"age\":\""+ age
								+ "\",\"size\":\""+ getNotNull(itemobj.getString("size"))
								+ "\",\"color\":\""+ color
								+ "\",\"modelflag\":\""+ getNotNull(itemobj.getString("modelflag"))
								+ "\",\"fileName\":\""+ getNotNull(itemobj.getString("filename"))
								+ "\",\"pricelistVersion\":\""+ pricelistversion
								+ "\",\"mRPPrice\":"+ mrp
								+ ",\"cessionPrice\":"+ cessionprice
								+ ",\"unitPrice\":"+ unitprice		
								+ ",\"uEPrice\":"+ ueprice				
								+ ",\"pCBPrice\":"+ pcbprice 
								+ ",\"taxCategory\":"+ taxCategory
								+ ",\"mailsent\":\""+ getNotNull(mailSentFlag)
								+ "\",\"ismadeinindia\":\""+ getNotNull(modelobj.getString("isMadeInIndia"))
								+ "\",\"familycode\":\""+ getNotNull(modelobj.getString("familycode"))
								+ "\",\"isacode\":\""+ getNotNull(itemobj.getString("isACode"))
								+ "\",\"oBALogisticClass\":\""+ getNotNull(itemobj.getString("logisticClass"))
								+ "\"}"); 
						
					} else {
						job = new JSONObject("{\"_entityName\":\"OBA_modelproduct\","
								+ "\"iMANCode\":\""+ getNotNull(modelobj.getString("imancode"))
								+ "\",\"actioncode\":\""+ getNotNull(actioncode)
								+ "\",\"modelCode\":\""+ getNotNull(modelobj.getString("modelcode"))
								+ "\",\"modelName\":\""+ getNotNull(modelobj.getString("modelname"))
								+ "\",\"brand\":\""	+ brand
								+ "\",\"merchandiseCategory\":\""+ getNotNull(modelobj.getString("familyname"))
								+ "\",\"typology\":\""+ getNotNull(modelobj.getString("typology"))
								+ "\",\"subdepartment\":\""+ getNotNull(modelobj.getString("dmisubdepatment"))
								+ "\",\"department\":\""+ getNotNull(modelobj.getString("dmidepartment"))
								+ "\",\"storeDepartment\":\""+ getNotNull(modelobj.getString("storedepartment"))
								+ "\",\"storeuniverse\":\""+ getNotNull(modelobj.getString("storeuniverse"))
								+ "\",\"branddepartment\":\""+ getNotNull(modelobj.getString("branddepartment"))
								+ "\",\"universe\":\""+ getNotNull(modelobj.getString("universe"))
								+ "\",\"lifeStage\":\""+ getNotNull(modelobj.getString("lifestage"))
								+ "\",\"spidurl\":\""+ getNotNull(modelobj.getString("spidurl"))
								+ "\",\"spidflag\":\""+ spidflag
								+ "\",\"natureOfProduct\":\""+ getNotNull(modelobj.getString("nature"))
								+ "\",\"uEQty\":"+ Long.parseLong(getAbsolute(itemobj.getString("ue")))
								+ ",\"pCBQty\":"+ Long.parseLong(getAbsolute(itemobj.getString("pcb")))
								+ ",\"eANCode\":\""+ getNotNull(itemobj.getString("eancode"))
								+ "\",\"itemCode\":\""+ getNotNull(itemobj.getString("itemcode"))
								+ "\",\"gender\":\""+ gender
								+ "\",\"age\":\""+ age
								+ "\",\"size\":\""+ getNotNull(itemobj.getString("size"))
								+ "\",\"color\":\""+ color
								+ "\",\"modelflag\":\""+ getNotNull(itemobj.getString("modelflag"))
								+ "\",\"fileName\":\""+ getNotNull(itemobj.getString("filename"))
								+ "\",\"pricelistVersion\":\""+ pricelistversion
								+ "\",\"mRPPrice\":"+ mrp
								+ ",\"cessionPrice\":"+ cessionprice
								+ ",\"unitPrice\":"+ unitprice		
								+ ",\"uEPrice\":"+ ueprice				
								+ ",\"pCBPrice\":"+ pcbprice 
								+ ",\"taxCategory\":"+ taxCategory
								+ ",\"mailsent\":\""+ getNotNull(mailSentFlag)
								+ "\",\"ismadeinindia\":\""+ getNotNull(modelobj.getString("isMadeInIndia"))
								+ "\",\"familycode\":\""+ getNotNull(modelobj.getString("familycode"))
								+ "\",\"isacode\":\""+ getNotNull(itemobj.getString("isACode"))
								+ "\",\"oBALogisticClass\":\""+ getNotNull(itemobj.getString("logisticClass"))
								+ "\"}"); 
					}
					
				}
				
				try {
					
					LOG.debug("Json obj"+job.toString());
					final JsonToDataConverter toBaseOBObject = OBProvider
							.getInstance().get(JsonToDataConverter.class);
					// Converting JSON Object to openbravo Business object
				
					
					OBA_ModelProduct bob = (OBA_ModelProduct)toBaseOBObject.toBaseOBObject(job);

					if(null != bob.getModelName())
						bob.setModelName(bob.getModelName().replace(" amp ", "&"));
					if(null != bob.getMerchandiseCategory())
						bob.setMerchandiseCategory(bob.getMerchandiseCategory().replace(" amp ", "&"));
					if(null != bob.getSubdepartment())
						bob.setSubdepartment(bob.getSubdepartment().replace(" amp ", "&"));
					if(null != bob.getDepartment())
						bob.setDepartment(bob.getDepartment().replace(" amp ", "&"));
					if(null != bob.getStoreDepartment())
						bob.setStoreDepartment(bob.getStoreDepartment().replace(" amp ", "&"));
					if(null != bob.getStoreuniverse())
						bob.setStoreuniverse(bob.getStoreuniverse().replace(" amp ", "&"));
					if(null != bob.getBranddepartment())
						bob.setBranddepartment(bob.getBranddepartment().replace(" amp ", "&"));
					if(null != bob.getUniverse())
						bob.setUniverse(bob.getUniverse().replace(" amp ", "&"));
					if(null != bob.getNatureOfProduct())
						bob.setNatureOfProduct(bob.getNatureOfProduct().replace(" amp ", "&"));
					
					// Database Connections and Database insertions
					OBDal.getInstance().save(bob);
					OBDal.getInstance().flush();
					SessionHandler.getInstance().commitAndStart();

				} catch (Exception e) {
					LOG.error("Error in converting json to baseobject", e);
					SessionHandler.getInstance().rollback();
				}
			}
			
		    OBDal.getInstance().commitAndClose();
			LOG.debug("added to database");

		} catch (JSONException e) {
			LOG.error("Error in updating JSOn data to database", e);
		}
		return i;

	}
	
	private String getBrndNameUsingUniverse(String universeName) {
		
		String brandName = "";
		if(universeName.equalsIgnoreCase("Cycle")) {
			brandName = "B TWIN";
		} else if(universeName.equalsIgnoreCase("Water sports")) {
			brandName = "TRIBORD";
		} else if(universeName.equalsIgnoreCase("Running")) {
			brandName = "KALENJI";
		} else if(universeName.equalsIgnoreCase("Hiking")) {
			brandName = "QUECHUA";
		} else if(universeName.equalsIgnoreCase("Fitness")) {
			brandName = "DOMYOS";
		} else if(universeName.equalsIgnoreCase("Fishing")) {
			brandName = "CAPERLAN";
		} else if(universeName.equalsIgnoreCase("Rackets sports")) {
			brandName = "ARTENGO";
		} else if(universeName.equalsIgnoreCase("Teamsports")) {
			brandName = "KIPSTA";
		} else if(universeName.equalsIgnoreCase("WALKING")) {
			brandName = "NEWFEEL";
		} else if(universeName.equalsIgnoreCase("DIVERSIFICATIONS")) {
			brandName = "DIVERSIFICATIONS";
		} else if(universeName.equalsIgnoreCase("Roller and Skate")) {
			brandName = "OXELO";
		} else if(universeName.equalsIgnoreCase("Golf")) {
			brandName = "INESIS";
		} else if(universeName.equalsIgnoreCase("Winter sports")) {
			brandName = "WED'ZE";
		} else if(universeName.equalsIgnoreCase("Horse riding")) {
			brandName = "FOUGANZA";
		} else if(universeName.equalsIgnoreCase("Hunting")) {
			brandName = "SOLOGNAC";
		} else if(universeName.equalsIgnoreCase("Target sports")) {
			brandName = "GEOLOGIC";
		} else if(universeName.equalsIgnoreCase("Health")) {
			brandName = "APTONIA";
		} else if(universeName.equalsIgnoreCase("SWIMMING")) {
			brandName = "NABAIJI";
		} else if(universeName.equalsIgnoreCase("CHULLANKA")) {
			brandName = "CHULLANKA";
		} else if(universeName.equalsIgnoreCase("ONTARIO")) {
			brandName = "ONTARIO";
		} else if(universeName.equalsIgnoreCase("CLIMBING  amp  MOUNTAINEERING EQUIPMENT")) {
			brandName = "SIMOND";
		} else if(universeName.equalsIgnoreCase("Electronic")) {
			brandName = "GEONAUTE";
		} else if(universeName.equalsIgnoreCase("Optical")) {
			brandName = "ORAO";
		} else if(universeName.equalsIgnoreCase("OGEA")) {
			brandName = "OGEA";
		} else if(universeName.equalsIgnoreCase("Workshop")) {
			brandName = "WORKSHOP";
		} else if(universeName.equalsIgnoreCase("Secteur accessoires de ventes")) {
			brandName = "SECTEUR ACCESSOIRES DE VENTES";
		} else if(universeName.equalsIgnoreCase("Secteur pour les articles d'em")) {
			brandName = "SECTEUR POUR LES ARTICLES D'EM";
		} else if(universeName.equalsIgnoreCase("Secteur reserved to PRODUCT GM")) {
			brandName = "SECTEUR POUR GM PRODUCT.";
		}
		
		return brandName;
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
