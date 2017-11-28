package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ReceptionSearchItem implements WebService {
	 static Logger log4j = Logger.getLogger(ReceptionSearchItem.class);
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String itemWS = "";
	    String strRead = "";
	    String orgname = "";
	    String itemcode = "";
	    
	    StringWriter sw = new StringWriter();
	    String hql = "";
	    String getitem = "";
	    String getupc = "";
	    String productHql = "";
	    String departmentHql = "";
	    String itemCode="";
	    String upc="";
	    String modelname="";
	    String Status="";
	    int flag = 0;
	    Connection con;
	    ResultSet getitemstatus = null;
	    JSONArray jsonArray = new JSONArray();
	    JSONObject jsonObject = new JSONObject();
	    log4j.info("Entering Itemcode Search");
	    JSONObject jsonDataObject = new JSONObject();
	    try {
	      InputStreamReader isReader = new InputStreamReader(request.getInputStream());
	      BufferedReader bReader = new BufferedReader(isReader);
	      StringBuilder strBuilder = new StringBuilder();
	      strRead = bReader.readLine();
	      while (strRead != null) {
	        strBuilder.append(strRead);
	        strRead = bReader.readLine();
	        log4j.info("Parsing Success");
	      }
	      itemWS = strBuilder.toString();
	      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	      DocumentBuilder builder = factory.newDocumentBuilder();
	      InputSource is = new InputSource(new StringReader(itemWS));
	      Document document = builder.parse(is);
	      itemcode = document.getElementsByTagName("item").item(0).getChildNodes().item(0)
	          .getNodeValue().toString();
	      con = PassiveDB.getInstance().getRetailDBConnection();
	      PreparedStatement pst = null;
if(itemcode.length()>=13)
{
	      
	      log4j.info("Search by UPC");
	      getupc = "select name,upc from m_product where upc=?";
	      pst = con.prepareStatement(getupc);
	      pst.setString(1, itemcode);
	     
	      getitemstatus = pst.executeQuery();
	      if (getitemstatus.next()) {
	    	  flag=1;
	    	  itemCode=getitemstatus.getString("name");
	    	  upc=getitemstatus.getString("upc");
	    	  modelname=getitemstatus.getString("modelname");
	    	  jsonObject.put("itemcode", itemCode);
	    	  jsonObject.put("upc", upc);
	    	  jsonObject.put("modelname", modelname);
	      }
	    
	    }
	    else
	    {
	    	log4j.info("Search by Itemcode");
		      getitem = "select name from m_product where name=? ";
		      pst = con.prepareStatement(getitem);
		      pst.setString(1, itemcode);		     
		      getitemstatus = pst.executeQuery();
		      if (getitemstatus.next()) {
		    	  flag=1;
		    	  itemCode=getitemstatus.getString("name");
		    	  upc=getitemstatus.getString("upc");
		    	  modelname=getitemstatus.getString("modelname");
		    	  
		      }
		    
	    }
	  
	    if(flag!=1)
	    {
	    	jsonObject.put("status", "nil");
	    }
	    else
	    {
	    	jsonObject.put("itemcode", itemCode);
	    	  jsonObject.put("upc", upc);
	    	  jsonObject.put("modelname", modelname);
	    }
	    jsonArray.put(jsonObject);
	     response.setContentType("text/xml");
	    response.setCharacterEncoding("utf-8");
	    final Writer w = response.getWriter();
	    w.write(jsonArray.toString());
	    w.close();
	    }
	    catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
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
