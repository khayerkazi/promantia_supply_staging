package in.decathlon.retail.api.omniCommerce.ad_webservice;

import in.decathlon.retail.api.omniCommerce.data.BufferedStockAPI;
import in.decathlon.retail.api.omniCommerce.util.OmniCommerceAPIUtility;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.catalog.CLMinmax;
import com.sysfore.catalog.CLModel;


public class NWProductStock extends DalBaseProcess {
	
	private ProcessLogger LOGGER;
	private static final Logger LOG = Logger.getLogger(NWProductStock.class);
	private static final OmniCommerceAPIUtility OmniCommerceAPIUtility = new OmniCommerceAPIUtility();
	
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		
		
		try {
		LOGGER = bundle.getLogger();
		LOGGER.logln("NWProductStock starts!");
		
		final Map<String, String> configs = OmniCommerceAPIUtility.getConfigurations();
		OBContext.getOBContext().setAdminMode();
		BufferedStockAPI bufferedStockAPI = null;
		final OBCriteria<BufferedStockAPI> bufferedStockAPIObCriteria = OBDal.getInstance().createCriteria(BufferedStockAPI.class);
		if(bufferedStockAPIObCriteria.count() > 0) {
			bufferedStockAPI = bufferedStockAPIObCriteria.list().get(0);
		}
		OBContext.getOBContext().restorePreviousMode();
		
//		final OBCriteria<Organization> storeObCriteria = OBDal.getInstance().createCriteria(Organization.class);
//		storeObCriteria.add(Restrictions.eq(Organization.PROPERTY_SWISSTORE, true));
		
		Organization store = OBDal.getInstance().get(Organization.class, bundle.getContext().getOrganization());
//		if(storeObCriteria.count() > 0) {
			
//			for (Organization store : storeObCriteria.list()) {
				
				final StringBuilder queryModel = new StringBuilder();
				queryModel.append("as cl WHERE cl.organization.id='"+store.getId()+"' AND cl.isinrange=true");
				final OBQuery<CLMinmax> prodObQuery = OBDal.getInstance().createQuery(CLMinmax.class, queryModel.toString());
				LOG.info("No of CLMinmax records : "+ prodObQuery.count());
				final Set<String> modelIdSet = new HashSet<String>();
				if(prodObQuery.count() > 0) {
					for (CLMinmax clMinmax : prodObQuery.list()) {
						modelIdSet.add(clMinmax.getProduct().getClModel().getId());
					}
				}
				LOG.info("No of dist model ids : "+ modelIdSet.size());
				for (String distModelId : modelIdSet) {
					final StringBuilder modelStringBuilder = new StringBuilder();
					modelStringBuilder.append("{");
					modelStringBuilder.append("\"brandStoreId\":\""+store.getName()+"\"");
					modelStringBuilder.append(",\"brandProductId\":\""+OBDal.getInstance().get(CLModel.class, distModelId).getModelCode()+"\"");
					modelStringBuilder.append(",\"count\":1");
					final JSONArray itemsJsonArray = new JSONArray();
					final OBCriteria<Product> prodObCriteria = OBDal.getInstance().createCriteria(Product.class);
					prodObCriteria.add(Restrictions.eq(Product.PROPERTY_CLMODEL, OBDal.getInstance().get(CLModel.class, distModelId)));
					if(prodObCriteria.count() > 0) {
						modelStringBuilder.append(",\"attributes\":\"{");
						BigDecimal clUnitPrice = BigDecimal.ZERO;
						int count = 0;
						for (Product prod : prodObCriteria.list()) {
							
//							modelStringBuilder.append("{");
							final JSONObject itemsJsonObject = new JSONObject();
							final OBQuery<StorageDetail> storageDetObCriteria = OBDal.getInstance().createQuery(StorageDetail.class, "as sd WHERE sd.organization.id='"+store.getId()+"' AND sd.product.id='"+prod.getId()+"' AND sd.storageBin.id in (select id from Locator where searchKey='Saleable "+store.getName()+"')");
							LOG.info("total products "+storageDetObCriteria.count());
							final OBQuery<ProductPrice> productPriceObQuery = OBDal.getInstance().createQuery(ProductPrice.class, "as pp WHERE pp.organization.id='"+store.getId()+"' AND pp.product.id ='"+prod.getId()+"'");
							if(productPriceObQuery.count() > 0 && storageDetObCriteria.count() > 0) {
								clUnitPrice = productPriceObQuery.list().get(0).getClCcunitprice();
//								modelStringBuilder.append("\"price\":"+productPriceObQuery.list().get(0).getClCcunitprice());
								if(count==0) {
									if(bufferedStockAPI != null) {
										if(clUnitPrice.longValue() < bufferedStockAPI.getNetUnitPrice()) {
											Long diff = storageDetObCriteria.list().get(0).getQuantityOnHand().longValue()-bufferedStockAPI.getBufferedqty();
											if(diff > 0) 
												modelStringBuilder.append("\\\"var_"+prod.getName()+"_avbl\\\":1");
											else
												modelStringBuilder.append("\\\"var_"+prod.getName()+"_avbl\\\":0");
										}
									} else {
										Long diff = storageDetObCriteria.list().get(0).getQuantityOnHand().longValue();
										if(diff > 0) 
											modelStringBuilder.append("\\\"var_"+prod.getName()+"_avbl\\\":1");
										else
											modelStringBuilder.append("\\\"var_"+prod.getName()+"_avbl\\\":0");
									}
								} else {
									if(bufferedStockAPI != null) {
										if(clUnitPrice.longValue() < bufferedStockAPI.getNetUnitPrice()) {
											Long diff = storageDetObCriteria.list().get(0).getQuantityOnHand().longValue()-bufferedStockAPI.getBufferedqty();
											if(diff > 0) 
												modelStringBuilder.append(",\\\"var_"+prod.getName()+"_avbl\\\":1");
											else
												modelStringBuilder.append(",\\\"var_"+prod.getName()+"_avbl\\\":0");
										}
									} else {
										Long diff = storageDetObCriteria.list().get(0).getQuantityOnHand().longValue();
										if(diff > 0) 
											modelStringBuilder.append(",\\\"var_"+prod.getName()+"_avbl\\\":1");
										else
											modelStringBuilder.append(",\\\"var_"+prod.getName()+"_avbl\\\":0");
									}
								}
							} else {
								clUnitPrice = BigDecimal.ZERO;
//								modelStringBuilder.append("\"price\":0.0");
								if(count==0) {
									modelStringBuilder.append("\\\"var_"+prod.getName()+"_avbl\\\":0");
								} else {
									modelStringBuilder.append(",\\\"var_"+prod.getName()+"_avbl\\\":0");
								}
							}
//							modelStringBuilder.append("},");
							itemsJsonArray.put(itemsJsonObject);
							count++;
						}
						modelStringBuilder.append(",\\\"price\\\":"+clUnitPrice);
						modelStringBuilder.append("}\"");
					} else {
						LOG.info("No Products are there");
						LOGGER.logln("No Products are there");
					}
					modelStringBuilder.append(",\"brandId\":"+configs.get("brandId"));
					modelStringBuilder.append("}");
					try {
						// Sending HTTP POST request
						final HttpURLConnection hc = createConnection(configs);
						hc.setDoOutput(true);
						final OutputStream os = hc.getOutputStream();
						LOG.info(modelStringBuilder.toString());
						os.write(modelStringBuilder.toString().getBytes("UTF-8"));
						os.flush();
						os.close();
						hc.connect();

						// Getting the Response from the Web service
						BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
						String inputLine;
						StringBuffer resp = new StringBuffer();

						while ((inputLine = in.readLine()) != null) {
							resp.append(inputLine);
						}
						String secondResponse = resp.toString();
						in.close();

						modelStringBuilder.delete(0, modelStringBuilder.length()-1);
					} catch (Exception e) {
						LOGGER.log(e.getMessage());
						BusinessEntityMapper.createErrorLogRecord(e, bundle.getProcessId(), new JSONObject("{\"recordId\":\""+OBDal.getInstance().get(CLModel.class, distModelId).getModelCode()+"\"}"));
						e.printStackTrace();
					}
				}
//			}
//		}
		} catch(Exception e) {
			e.printStackTrace();
		}
		LOGGER.log("process ends here!");
	}
	
	private HttpURLConnection createConnection(Map<String, String> configs) throws Exception {
		
	    final URL url = new URL(configs.get("wsURL"));
	    final String headerString = configs.get("username")+":"+configs.get("password");
	    String encoded = Base64.encodeBase64String(headerString.getBytes());
	    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();

	    hc.setRequestMethod("POST");
	    hc.setAllowUserInteraction(false);
	    hc.setDefaultUseCaches(false);
	    hc.setDoInput(true);
	    hc.setInstanceFollowRedirects(true);
	    hc.setUseCaches(false);
	    hc.setRequestProperty("Authorization", "Basic "+encoded);
	    hc.setRequestProperty("content-type", "application/json");
	    return hc;
	  }

}
