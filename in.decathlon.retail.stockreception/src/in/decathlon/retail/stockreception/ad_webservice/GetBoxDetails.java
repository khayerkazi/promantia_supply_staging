package in.decathlon.retail.stockreception.ad_webservice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class GetBoxDetails implements WebService {
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
    JSONArray boxHolderArry = new JSONArray();
    JSONObject boxObject = new JSONObject();
    StringWriter sw = new StringWriter();
    PreparedStatement pstGetTruckNumber = null;
    ResultSet rsGetTruckNumber = null;
    String getTruck = null;
    String truckNumber = null;
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

      getTruck = "SELECT  dtr.documentno as trucknumber FROM  M_INOUT min JOIN M_INOUTLINE minline ON"
          + " min.M_INOUT_ID=minline.M_INOUT_ID  JOIN DTR_TRUCK_DETAILS dtrline ON"
          + " dtrline.M_INOUT_ID=min.M_INOUT_ID JOIN  DTR_TRUCK_RECEPTION dtr ON "
          + "dtr.DTR_TRUCK_RECEPTION_ID=dtrline.DTR_TRUCK_RECEPTION_ID JOIN AD_ORG "
          + "org ON org.AD_ORG_ID=dtr.AD_ORG_ID  WHERE min.DOCUMENTNO=?"
          + " AND org.name=? group by min.documentno,dtr.documentno";
      pstGetTruckNumber = con.prepareStatement(getTruck);
      pstGetTruckNumber.setString(1, boxNumber);
      pstGetTruckNumber.setString(2, orgName);
      rsGetTruckNumber = pstGetTruckNumber.executeQuery();
      while (rsGetTruckNumber.next()) {
        truckNumber = rsGetTruckNumber.getString("trucknumber");
      }
      if (truckNumber != null) {
        log4j.info("Search by Box Number");
        getBox = "SELECT min.documentno as boxNumber,dtr.documentno as truckNumber,count(minline.m_product_id) as productCount, "
            + "sum(minline.movementqty) as sumofqty FROM M_INOUT min JOIN M_INOUTLINE minline ON"
            + " min.M_INOUT_ID=minline.M_INOUT_ID JOIN DTR_TRUCK_DETAILS dtrline ON "
            + "dtrline.M_INOUT_ID=min.M_INOUT_ID JOIN  DTR_TRUCK_RECEPTION dtr ON "
            + "dtr.DTR_TRUCK_RECEPTION_ID=dtrline.DTR_TRUCK_RECEPTION_ID  WHERE dtr.documentno=? GROUP BY min.documentno,dtr.documentno";
        pst = con.prepareStatement(getBox);
        pst.setString(1, truckNumber);
        boxResult = pst.executeQuery();
        while (boxResult.next()) {
          JSONObject boxHolder = new JSONObject();
          String brandName = null;
          String boxLabel = boxResult.getString("boxNumber");
          Pattern pattern = Pattern.compile("-(.*?)-");
          Matcher matcher = pattern.matcher(boxLabel);
          while (matcher.find()) {
            brandName = matcher.group(1);
          }
          boxHolder.put("boxNumber", boxLabel);
          boxHolder.put("truckNumber", boxResult.getString("truckNumber"));
          boxHolder.put("productCount", boxResult.getString("productCount"));
          boxHolder.put("productSum", boxResult.getString("sumofqty"));
          boxHolder.put("brandName", brandName);
          boxHolderArry.put(boxHolder);
          flag++;
        }
        if (flag > 0) {
          log4j.info("Box Number" + boxHolderArry);
          boxObject.put("data", boxHolderArry);
          boxObject.put("status", "success");
        }

        else {
          log4j.debug("No results for Box" + boxNumber + "Org" + orgName);
          boxObject.put("data", "noresult");
          boxObject.put("status", "success");
        }
      } else {// data not available in db
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
