package in.nous.searchitem.ad_webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Location;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class RegionStockWebService extends HttpSecureAppServlet implements
								WebService{
	
	private static Logger log = Logger.getLogger(RegionStockWebService.class);
	
	//private static String storeName = "storeName";
	
	private static String successStatus = "successStatus";
	
	private static String success = "success";
	
	//private static String itemCode = "itemCode";

	//private static String email = "email";
	
	//private static String regionstock = "regionstock";
	
	@Override
	public void doGet(String path,HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void doDelete(String path,HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}
	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();

	}
	
	/**
	 * Performs the POST REST operation. This service handles the request for
	 * the XML Schema containing PO details to create Purchase Order.
	 * 
	 * @param path
	 *            the HttpRequest.getPathInfo(), the part of the url after the
	 *            context path
	 * @param request
	 *            the HttpServletRequest
	 * @param response
	 *            the HttpServletResponse
	 */
	
	public void doPost(String path,HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		String xml = "";
		String strRead = "";
		JSONObject jsonDataObject = new JSONObject();

try{

		
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
				
		String itemCode;
		String storeName;
		
		itemCode = document.getElementsByTagName("itemCode").item(0).getChildNodes().item(0).getNodeValue();
		
		storeName = document.getElementsByTagName("storeName").item(0).getChildNodes().item(0).getNodeValue();
		
		/*String sql = "from Organization where id in(select distinct(organization) from Location " +
					"where region=(select region from Location where id=" +
					"(select locationAddress from OrganizationInformation where organization=" +
					"(select id from Organization where name='"+storeName+"' ))))and sWIsstore='Y'";*/

		String regionQueryStr = "from Location where id=" +
					"(select locationAddress from OrganizationInformation where organization=" +
					"(select id from Organization where name='"+storeName+"'))";   
		
	
		Query regionQuery = OBDal.getInstance().getSession().createQuery(regionQueryStr);
		//System.out.println("query string-->"+regionQuery.getQueryString() );
		List<Location> regionList = regionQuery.list();
		//System.out.println("region list-->"+regionList.get(0)); 
		//System.out.println("region of current store-->"+regionList.get(0).getRegion());
//		String hql = "from Organization where id in" +
//			"(select id from OrganizationInformation where locationAddress in" +
//			"(select id from Location where id in" +
//			"(select locationAddress from OrganizationInformation where organization in"+
//			"(select distinct(organization) from Location)) "+
// 			 	"and region='"+regionList.get(0).getRegion().getId()+"')) " +
// 			 			"and id!=(select id from Organization where name='"+storeName+"')"; 
//			select name from ad_org where ad_org_id in (select ad_org_id from ad_orginfo where c_location_id in (select c_location_id from c_location where c_region_id = 'D05145EF70694D0CA11FE5943DEBAF44'))
			String hql = "from Organization where sWIsstore='Y' and id in (select organization.id from OrganizationInformation where idsdZone in (select idsdZone from OrganizationInformation where id in (select id from Organization where name ='"+storeName+"'))) and id!=(select id from Organization where name='"+storeName+"')";
		Query query = OBDal.getInstance().getSession().createQuery(hql);
		List<Organization> resultList = query.list();
		
		// m_productrice details
//		List<ProductPrice> productPriceList = null;
//		final OBQuery<ProductPrice> priceDeatailsQuery = OBDal.getInstance().createQuery(ProductPrice.class, "as p where p.product.id=(select id from Product where name='"+itemCode+"') ");
//		if(null != priceDeatailsQuery.list()) {
//			productPriceList = priceDeatailsQuery.list();
//		}
		
		JSONArray jsonArray = new JSONArray();
		for(Organization organization : resultList)
		{
			// m_productrice details
			List<ProductPrice> productPriceList = null;
			final OBQuery<ProductPrice> priceDeatailsQuery = OBDal.getInstance().createQuery(ProductPrice.class, "as p where p.product.id=(select id from Product where name='"+itemCode+"') and p.organization.id='"+organization.getId()+"'");
			if(null != priceDeatailsQuery.list()) {
				productPriceList = priceDeatailsQuery.list();
			}
			
			hql =" SELECT COALESCE(sum(quantityOnHand),0) as storeqty from MaterialMgmtStorageDetail where storageBin.warehouse.name like 'Saleable%' and storageBin.organization.name = '"+organization.getSearchKey()+"'  and product.name = '"+itemCode+"' and attributeSetValue = '0'";
		    query = OBDal.getInstance().getSession().createQuery(hql);
		    String storeQty = query.list().get(0).toString();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name",organization.getName());
			jsonObject.put("description",organization.getDescription());
			if(null != productPriceList) {
				jsonObject.put("unit_price",productPriceList.get(0).getClCcunitprice());
				jsonObject.put("lot_price",productPriceList.get(0).getClCcueprice());
				jsonObject.put("box_price",productPriceList.get(0).getClCcpcbprice());
			}
			jsonObject.put(" stock", storeQty);
			jsonArray.put(jsonObject);
		}
		jsonDataObject.put("data",jsonArray);
		jsonDataObject.put("status","Success");
			
		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(jsonDataObject.toString());
		w.close();
		for(int i=0;i<resultList.size();i++)
		{
		}
}catch(Exception e)
{
	log.error( "Exception while reading Stores:",e);
	e.printStackTrace();
}
				
	}
	
	

}

