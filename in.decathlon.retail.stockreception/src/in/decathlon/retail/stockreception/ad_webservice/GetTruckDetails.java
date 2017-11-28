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

public class GetTruckDetails implements WebService {
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
    String truckNumber = "";
    String cons_Query_getTruck = "";
    int flag = 0;
    Boolean isValid = false;
    String truckStatus = null;
    Connection con = null;
    ResultSet truckResult = null;
    PreparedStatement pst = null;
    PreparedStatement pstTruckStatus = null;
    ResultSet rsTruckStatus = null;
    JSONObject truckHolder = new JSONObject();
    JSONArray truckHolderArry = new JSONArray();
    JSONObject truckObject = new JSONObject();
    StringWriter sw = new StringWriter();
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

      truckNumber = document.getElementsByTagName("truckNumber").item(0).getChildNodes().item(0)
          .getNodeValue().toString();
      orgName = document.getElementsByTagName("orgName").item(0).getChildNodes().item(0)
          .getNodeValue().toString();
      con = OBDal.getInstance().getConnection();
      String status = "select docstatus from dtr_truck_reception where documentno=? limit 1";
      pstTruckStatus = con.prepareStatement(status);
      pstTruckStatus.setString(1, truckNumber);
      rsTruckStatus = pstTruckStatus.executeQuery();
      while (rsTruckStatus.next()) {
        isValid = true;
        truckStatus = rsTruckStatus.getString("docstatus");

        if (truckStatus.equalsIgnoreCase("DR")) {
          log4j.info("Search by Truck Number");
          cons_Query_getTruck = "select truck.documentno as truckName,count(trucklines.dtr_truck_details_id) as boxCount from dtr_truck_reception truck join dtr_truck_details trucklines on truck.dtr_truck_reception_id=trucklines.dtr_truck_reception_id join ad_org org on org.ad_org_id=truck.ad_org_id where truck.documentno=? and org.name=? and truck.docstatus='DR' group by truck.documentno";
          pst = con.prepareStatement(cons_Query_getTruck);
          pst.setString(1, truckNumber);
          pst.setString(2, orgName);
          truckResult = pst.executeQuery();
          while (truckResult.next()) {
            truckHolder.put("truckName", truckResult.getString("truckName"));
            truckHolder.put("boxCount", truckResult.getString("boxCount"));
            truckHolderArry.put(truckHolder);
            flag++;
          }
          if (flag > 0) {
            log4j.info("Truck Number" + truckHolderArry);
            truckObject.put("data", truckHolderArry);
            truckObject.put("status", "success");
          }

          else {
            log4j.debug("Truck is not exist" + truckNumber + "Org" + orgName);
            truckObject.put("data", "noresult");
            truckObject.put("status", "success");
          }
        } else if (truckStatus.equalsIgnoreCase("CO")) {
          log4j.debug("Truck is already completed" + truckNumber + "Org" + orgName);
          truckObject.put("data", "completed");
          truckObject.put("status", "completed");
        } else {
          log4j.debug("Truck is not valid" + truckNumber + "Org" + orgName);
          truckObject.put("data", "noresult");
          truckObject.put("status", "noresult");
        }
      }
      if (!isValid) {
        log4j.debug("Truck is not valid" + truckNumber + "Org" + orgName);
        truckObject.put("data", "noresult");
        truckObject.put("status", "noresult");
      }
    } catch (Exception e) {
      log4j.debug("Error for Truck" + truckNumber + "Org" + orgName);
      truckObject.put("data", "noresult");
      truckObject.put("status", "Failure");
      e.printStackTrace();

    } finally {
      con.close();
    }
    final String xml = sw.toString();
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(truckObject.toString());

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
