package in.nous.tools.mrpsticker.webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;

public class GetStickerDetailWebService extends HttpSecureAppServlet
		implements WebService {
	
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger
			.getLogger(GetStickerDetailWebService.class);

	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

	}

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

		JSONObject jsonDataObject = parseXML(document);

		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(jsonDataObject.toString());
		w.close();
	}

	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}
	
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	public static JSONObject parseXML(Document xml) {
		String itemCode = null;
		JSONObject jsonDataObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		
		try {

			itemCode = xml.getElementsByTagName("itemcode").item(0)
					.getChildNodes().item(0).getNodeValue();

			System.out.println("Item code is :" + itemCode);

			String hql3 = "select count(*) from Product where name='"+itemCode+"'";
			Query query3 = OBDal.getInstance().getSession().createQuery(hql3);
			
			String count = query3.list().get(0).toString(); 
	    		Integer countNo = Integer.parseInt(count);
			System.out.println("Count is :" + countNo);
			
			if(countNo > 0){
				jsonObject.put("count",	countNo);

				String hql = "from Product where name='"+itemCode+"'";
				Query query = OBDal.getInstance().getSession().createQuery(hql);
				List<Product> productList = query.list();
			
				if(productList.size() > 0){
					Product product = productList.get(0);
					jsonObject.put("itemCode",	itemCode);
					jsonObject.put("modelCode",	product.getClModelcode());
					jsonObject.put("modelName",	product.getClModelname());
					jsonObject.put("size",	product.getClSize());
					jsonObject.put("ean",	product.getUPCEAN());
				
				}	
			
				String hql2 = "from PricingProductPrice where product.name='"+itemCode+"'";
				Query query2 = OBDal.getInstance().getSession().createQuery(hql2);
			
				List<ProductPrice> productPriceList = query2.list();
				if(productList.size() > 0){
					ProductPrice productPrice = productPriceList.get(0);
					jsonObject.put("mrp",	productPrice.getClMrpprice());
				
				}
				
			}else{
				jsonObject.put("count",	"0");
			}


			jsonArray.put(jsonObject);	
			jsonDataObject.put("data", jsonArray);
			jsonDataObject.put("status", "Success");
			
		} catch (Exception exp) {
			log.error("Exception while reading Stikcer Data from Product and ProductPrice Table:", exp);
			exp.printStackTrace();
			return jsonDataObject;
		}

		return jsonDataObject;

	}

	
}
