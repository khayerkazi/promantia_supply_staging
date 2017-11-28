package in.decathlon.supply.stockreception.ad_webservice;

import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

public class GetItemDetails implements WebService {
  static Logger log4j = Logger.getLogger(GetItemDetails.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    StringWriter sw = new StringWriter();
    String getItemDetail_supply = "";
    String getdocumentstatus_retail = "";
    String strRead = "";
    Connection con_supply;
    Connection con_retail;
    String ItemDetailsFromWS = "";
    ResultSet getItemDetailResultSet_supply = null;
    ResultSet getItemDetailResultSet_retail = null;
    log4j.info("Entering Into Search by box Part");
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
      ItemDetailsFromWS = strBuilder.toString();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(ItemDetailsFromWS));
      Document document = builder.parse(is);
      String orgname = document.getElementsByTagName("orgname").item(0).getChildNodes().item(0)
          .getNodeValue().toString();
      String boxnumber = document.getElementsByTagName("boxnumber").item(0).getChildNodes().item(0)
          .getNodeValue().toString();

      con_supply = PassiveDB.getInstance().getSupplyDBConnection();
      PreparedStatement pst_supply = null;

      getItemDetail_supply = "select co.documentNo as documentno,mproduct.upc as upc,mproduct.name as itemcode,mproduct.em_cl_modelname as modelname,sum(prod.quantity) as quantity,minout.docstatus as docstatus from OBWPACK_Box box,OBWPACK_Movlinebox prod,obwpack_packingh header,c_bpartner partner,m_product mproduct, c_order co ,c_orderline col,m_inout minout where box.obwpack_packingh_id=header.obwpack_packingh_id  and  prod.obwpack_box_id=box.obwpack_box_id and partner.c_bpartner_id=header.c_bpartner_id and mproduct.m_product_id=prod.m_product_id and  col.c_order_id=co.c_order_id and col.c_orderline_id=prod.c_orderline_id and co.c_order_id=minout.c_order_id and box.trackingNo=? and partner.name=? group by mproduct.name,mproduct.em_cl_modelname,mproduct.upc, co.documentNo,minout.docstatus";
      pst_supply = con_supply.prepareStatement(getItemDetail_supply);
      pst_supply.setString(1, boxnumber);
      pst_supply.setString(2, orgname);
      getItemDetailResultSet_supply = pst_supply.executeQuery();
      JSONArray jsonArray = new JSONArray();
      int k = 0;
      int j = 0;
      while (getItemDetailResultSet_supply.next()) {
        String documentnumber = getItemDetailResultSet_supply.getString("documentno");
        String upc = getItemDetailResultSet_supply.getString("upc");
        String itemcode = getItemDetailResultSet_supply.getString("itemcode");
        String modelname = getItemDetailResultSet_supply.getString("modelname");
        String quantity = getItemDetailResultSet_supply.getString("quantity");
        String docstatus = getItemDetailResultSet_supply.getString("docstatus");
        k++;
        if (!docstatus.equalsIgnoreCase("CO")) {
          j++;
          log4j.info("One or more DC not Completed");
        }

        log4j.info("Insert data into Json");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("documentno", documentnumber);
        jsonObject.put("upc", upc);
        jsonObject.put("itemcode", itemcode);
        jsonObject.put("modelname", modelname);
        jsonObject.put("quantity", quantity);

        jsonArray.put(jsonObject);
      }
      jsonDataObject.put("data", jsonArray);
      jsonDataObject.put("status", "Success");
      log4j.info(jsonArray);

      if ((k <= 0) || (j != 0)) {
        jsonDataObject.put("data", "noresult");
        jsonDataObject.put("status", "Success");
        log4j.info("NoResult");
      }
    } catch (SQLException exception) {
      jsonDataObject.put("data", "noresult");
      jsonDataObject.put("status", "Failure");
      exception.printStackTrace();
    } catch (ClassNotFoundException e) {
      jsonDataObject.put("data", "noresult");
      jsonDataObject.put("status", "Failure");
      e.printStackTrace();
    } catch (Exception e) {
      jsonDataObject.put("data", "noresult");
      jsonDataObject.put("status", "Failure");
      e.printStackTrace();

    }
    final String xml = sw.toString();
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonDataObject.toString());

  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
