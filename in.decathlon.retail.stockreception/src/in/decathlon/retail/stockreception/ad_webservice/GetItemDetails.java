package in.decathlon.retail.stockreception.ad_webservice;

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

import jxl.common.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class GetItemDetails implements WebService {
  final private static Logger log4j = Logger.getLogger(GetTruckDetails.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String truckDetails = "";
    String strRead = "";
    String orgName = "";
    String boxNumber = "";
    String getBox = "";
    int flag = 0;
    String docStatus = null;
    Connection con = null;
    ResultSet boxResult = null;
    ResultSet boxResultVerify = null;
    PreparedStatement pst = null;
    PreparedStatement pstVerity = null;
    JSONArray itemHolderArry = new JSONArray();
    JSONObject boxObject = new JSONObject();
    StringWriter sw = new StringWriter();
    String verifyBox;
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
      truckDetails = strBuilder.toString();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(truckDetails));
      Document document = builder.parse(is);

      boxNumber = document.getElementsByTagName("boxNumber").item(0).getChildNodes().item(0)
          .getNodeValue().toString();
      orgName = document.getElementsByTagName("orgName").item(0).getChildNodes().item(0)
          .getNodeValue().toString();
      con = OBDal.getInstance().getConnection();
      log4j.info("Search by Box Number");
      // String getItem =
      // "SELECT mp.name as itemCode,mp.upc as upc,clm.name as modelName,mp.em_cl_size as size,sum(minline.movementqty) as productSum FROM  M_INOUT min JOIN M_INOUTLINE minline ON min.M_INOUT_ID=minline.M_INOUT_ID JOIN DTR_TRUCK_DETAILS dtrline ON dtrline.M_INOUT_ID=min.M_INOUT_ID JOIN  DTR_TRUCK_RECEPTION dtr ON dtr.DTR_TRUCK_RECEPTION_ID=dtrline.DTR_TRUCK_RECEPTION_ID JOIN AD_ORG org ON org.AD_ORG_ID=dtr.AD_ORG_ID JOIN M_PRODUCT mp ON mp.M_PRODUCT_ID=minline.M_PRODUCT_ID JOIN CL_MODEL clm on clm.cl_model_id=mp.em_cl_model_id JOIN CL_BRAND clb on clb.cl_brand_id=clm.cl_brand_id WHERE min.DOCUMENTNO=? AND org.name=? AND min.docstatus='CO' group by clm.name,mp.em_cl_size,mp.upc,mp.name";
      String getItem = "SELECT col.c_orderline_id as orderLine,mp.name as itemCode,mp.upc as upc,clm.name as modelName, mp.em_cl_size as size,sum(minline.movementqty) as productSum FROM M_INOUT min JOIN M_INOUTLINE minline ON min.M_INOUT_ID=minline.M_INOUT_ID JOIN DTR_TRUCK_DETAILS dtrline ON dtrline.M_INOUT_ID=min.M_INOUT_ID JOIN DTR_TRUCK_RECEPTION dtr ON dtr.DTR_TRUCK_RECEPTION_ID=dtrline.DTR_TRUCK_RECEPTION_ID JOIN AD_ORG org ON org.AD_ORG_ID=dtr.AD_ORG_ID JOIN M_PRODUCT mp ON mp.M_PRODUCT_ID=minline.M_PRODUCT_ID JOIN CL_MODEL clm on clm.cl_model_id=mp.em_cl_model_id JOIN CL_BRAND clb on clb.cl_brand_id=clm.cl_brand_id JOIN c_orderline col on col.c_orderline_id=minline.c_orderline_id WHERE min.DOCUMENTNO=? AND org.name=? AND min.docstatus='CO' group by col.c_orderline_id,clm.name,mp.em_cl_size,mp.upc,mp.name";
      pst = con.prepareStatement(getItem);
      pst.setString(1, boxNumber);
      pst.setString(2, orgName);
      boxResult = pst.executeQuery();
      while (boxResult.next()) {
        JSONObject itemHolder = new JSONObject();
        itemHolder.put("orderLine", boxResult.getString("orderLine"));
        itemHolder.put("itemCode", boxResult.getString("itemCode"));
        itemHolder.put("upc", boxResult.getString("upc"));
        itemHolder.put("modelName", boxResult.getString("modelName"));
        itemHolder.put("size", boxResult.getString("size"));
        itemHolder.put("productSum", boxResult.getString("productSum"));
        itemHolderArry.put(itemHolder);
        flag++;
      }
      if (flag > 0) {
        log4j.info("Box Number" + itemHolderArry);
        boxObject.put("data", itemHolderArry);
        boxObject.put("status", "success");
      }

      else {
        log4j.debug("No results for Box" + boxNumber + "Org" + orgName);
        boxObject.put("data", "noresult");
        boxObject.put("status", "success");
      }

    } catch (Exception e) {
      log4j.debug("Error for Box" + boxNumber + "Org" + orgName);
      boxObject.put("data", "noresult");
      boxObject.put("status", "Failure");
      e.printStackTrace();
    } finally {
      con.close();
    }
    final String xml = sw.toString();
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(boxObject.toString());

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
